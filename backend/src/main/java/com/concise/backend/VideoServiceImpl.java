package com.concise.backend;

import com.concise.backend.model.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class VideoServiceImpl {
    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private ChapterServiceImpl chapterService;

    @Autowired
    private UserRepository userRepository;

    @Value("${transcript.api.url}")
    private String transcriptApiUrl;

    @Value("${transcript.api.key}")
    private String transcriptApiKey;

    @Value("${summarization.api.url}")
    private String summarizationApiUrl;

    @Value("${summarization.api.key}")
    private String summarizationApiKey;

    public List<VideoWithChaptersDto> getAllVideosWithChaptersByUserId(Long userId) {
        return videoRepository.findAllByUserIdOrderByIdDesc(userId).stream()
                .map(video -> new VideoWithChaptersDto(video, chapterService.getChaptersByVideoId(video.getId())))
                .toList();
    }

    public Optional<VideoWithChaptersDto> getVideoByIdAndUserId(long id, long userId) {
        Optional<VideoEntity> videoOptional = videoRepository.findByIdAndUserId(id, userId);
        if (videoOptional.isPresent()) {
            VideoEntity videoEntity = videoOptional.get();
            VideoWithChaptersDto videoWithChaptersDto = new VideoWithChaptersDto(videoEntity, chapterService.getChaptersByVideoId(videoEntity.getId()));
            return Optional.of(videoWithChaptersDto);
        }
        return Optional.empty();
    }

    public List<VideoWithChaptersDto> getAllVideosWithChaptersByYoutubeIdAndLanguageAndUserId(String youtubeId, String language, long userId) {
        return videoRepository.findAllByYoutubeIdAndLanguageAndUserId(youtubeId, language, userId).stream()
                .map(video -> new VideoWithChaptersDto(video, chapterService.getChaptersByVideoId(video.getId())))
                .toList();
    }

    @Transactional
    public VideoEntity addVideo(VideoEntity video) {
        return videoRepository.saveAndFlush(video);
    }

    public VideoWithChaptersDto createVideoFromYoutubeId(CreateVideoDto createVideoDto, UserEntity user) throws GeneralSecurityException, IOException {
        // get chapters with YouTubeChapterExtractorService.getVideoTimelineById
        Map.Entry<String, List<YouTubeChapterExtractorService.Chapter>> videoInfo = YouTubeChapterExtractorService.getVideoTimelineById(createVideoDto.getYoutubeId());
        String videoTitle = videoInfo.getKey();
        List<YouTubeChapterExtractorService.Chapter> chapters = videoInfo.getValue();

        // get transcript with HTTP request to youtube-transcript-api microservice
        String jsonString = getTranscript(createVideoDto.getYoutubeId(), user.getLanguage());

        // Transcript is a string of JSON in format {"language": "en", "transcript":[ {"text":"- Welcome to the Huberman Lab Podcast,","start":0.36,"duration":1.56} ]}
        // Parse the JSON transcript to a java file
        TranscriptContainer transcriptContainer = readTranscriptFromJson(jsonString);

        String fullTranscript = transcriptContainer.getTranscript().stream()
                .map(TranscriptEntry::getText)  // Get the text property of each TranscriptEntry
                .collect(Collectors.joining(" "))
                .replaceAll("\\r?\\n|\\r", " ");

        // Need to map each TranscriptEntry to a chapter
        List<ChapterTranscript> chapterTranscripts = createChapterTranscripts(chapters, transcriptContainer);

        // TODO: Implement recursive summarization
        // TODO: Return summaries as separate requests using Celery tasks
        for (int i = 0; i < chapterTranscripts.size(); i++) {
            String summary = getSummary(chapterTranscripts.get(i).getTranscript());
            chapterTranscripts.get(i).setSummary(summary);
        }

        String fullSummary = getSummary(fullTranscript);

        // TODO: Translate summaries if necessary with HTTP request to NLLB translation-api microservice

        // Return video with chapters
        VideoWithChaptersDto createdVideoWithChaptersDto = storeVideoAndChapters(
                createVideoDto.getYoutubeId(),
                videoTitle, fullTranscript, fullSummary,
                "en", user.getId(), chapters, chapterTranscripts);

        return createdVideoWithChaptersDto;
    }

    public String getTranscript(String youtubeId, String language) {
        RestTemplate restTemplate = new RestTemplate();
        String url = transcriptApiUrl + youtubeId + "/" + language;
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", transcriptApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        String responseObject = response.getBody();
        return responseObject;
    }

    public String getSummary(String transcript) throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        String url = summarizationApiUrl;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", summarizationApiKey);
        Map<String, String> map = new HashMap<>();
        String prompt = """
        Summarize the following video transcript text into 5-10 sentence paragraph.
        """ + transcript;
        map.put("text", prompt);
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(map);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        System.out.println("url" + url);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        String responseBody = response.getBody();
        ObjectMapper responseObjectMapper = new ObjectMapper();
        JsonNode jsonNode = responseObjectMapper.readTree(responseBody);
        String summary = jsonNode.get("summary").asText();
        return summary;
    }

    public static TranscriptContainer readTranscriptFromJson(String jsonString) {
        TranscriptContainer transcriptContainer = new TranscriptContainer();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            transcriptContainer = objectMapper.readValue(jsonString, TranscriptContainer.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return transcriptContainer;
    }

    public VideoWithChaptersDto storeVideoAndChapters(String youtubeId, String videoTitle, String fullTranscript, String fullSummary, String language, long userId, List<YouTubeChapterExtractorService.Chapter> chapters, List<ChapterTranscript> chapterTranscripts) {
        // Store video in database
        VideoEntity videoEntity = new VideoEntity();
        videoEntity.setYoutubeId(youtubeId);
        videoEntity.setTitle(videoTitle);
        videoEntity.setTranscript(fullTranscript);
        videoEntity.setSummary(fullSummary);
        videoEntity.setLanguage(language);
        videoEntity.setUser(userRepository.findById(userId).get());
        VideoEntity createdVideo = addVideo(videoEntity);

        // Store chapters in database
        List<ChapterEntity> createdChapters = new ArrayList<>();
        for (int i = 0; i < chapters.size(); i++) {
            ChapterEntity chapterEntity = new ChapterEntity();
            chapterEntity.setTitle(chapters.get(i).getTitle());
            chapterEntity.setTranscript(chapterTranscripts.get(i).getTranscript());
            chapterEntity.setSummary(chapterTranscripts.get(i).getSummary());
            chapterEntity.setStartTimeSeconds(chapters.get(i).getStartTimeSeconds());
            chapterEntity.setVideo(createdVideo);
            createdChapters.add(chapterService.addChapter(chapterEntity));
        }
        return new VideoWithChaptersDto(createdVideo, createdChapters);
    }

    public static class TranscriptContainer {
        @JsonProperty("language")
        private String language;

        @JsonProperty("transcript")
        private List<TranscriptEntry> transcript;

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public List<TranscriptEntry> getTranscript() {
            return transcript;
        }

        public void setTranscript(List<TranscriptEntry> transcript) {
            this.transcript = transcript;
        }
    }

    public static class TranscriptEntry {
        @JsonProperty("text")
        private String text;

        @JsonProperty("start")
        private double start;

        @JsonProperty("duration")
        private double duration;

        private double endTimeSeconds;

        @JsonCreator()
        public TranscriptEntry(@JsonProperty("text") String text,
                               @JsonProperty("start") double start,
                               @JsonProperty("duration") double duration) {
            this.text = text;
            this.start = start;
            this.duration = duration;
            updateEndTimeSeconds();
        }

        // Getters and setters
        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public double getStart() {
            return start;
        }

        public void setStart(double start) {
            this.start = start;
        }

        public double getDuration() {
            return duration;
        }

        public void setDuration(double duration) {
            this.duration = duration;
        }

        public double getEndTimeSeconds() {
            return endTimeSeconds;
        }

        private void updateEndTimeSeconds() {
            endTimeSeconds = start + duration;
        }

        @Override
        public String toString() {
            return "TranscriptEntry{" +
                    "text='" + text + '\'' +
                    ", start=" + start +
                    ", duration=" + duration +
                    ", endTimeSeconds=" + endTimeSeconds +
                    '}';
        }
    }

    public static List<ChapterTranscript> createChapterTranscripts(List<YouTubeChapterExtractorService.Chapter> chapters, TranscriptContainer transcriptContainer) {
        List<ChapterTranscript> chapterTranscripts = new ArrayList<>();
        List<TranscriptEntry> transcriptEntries = transcriptContainer.getTranscript();
        int transcriptIndex = 0;

        for (int i = 0; i < chapters.size(); i++) {
            YouTubeChapterExtractorService.Chapter chapter = chapters.get(i);
            double chapterStartTimeSeconds = (double) chapter.getStartTimeSeconds();
            double chapterEndTimeSeconds;

            if (i < chapters.size() - 1) {
                chapterEndTimeSeconds = (double) chapters.get(i + 1).getStartTimeSeconds();
            } else {
                chapterEndTimeSeconds = Double.MAX_VALUE;
            }

            StringBuilder chapterTranscriptBuilder = new StringBuilder();

            for (int j = 0; j < transcriptEntries.size(); j++) {
                // if transcript entry is within chapter time range, add to chapter transcript
                TranscriptEntry transcriptEntry = transcriptEntries.get(j);
                double transcriptEntryStartTimeSeconds = transcriptEntry.getStart();
                double transcriptEntryEndTimeSeconds = transcriptEntry.getEndTimeSeconds();
                // This will ignore transcript entries that start in one chapter and end in the following chapter
                // (i.e. the final transcript entry of each chapter)
                // This should be improved.
                if (transcriptEntryStartTimeSeconds >= chapterStartTimeSeconds &&
                        transcriptEntryEndTimeSeconds <= chapterEndTimeSeconds) {
                    chapterTranscriptBuilder.append(transcriptEntry.getText().replaceAll("\\r?\\n|\\r", " ") + " ");
                }
            }

            chapterTranscripts.add(new ChapterTranscript(chapter, chapterTranscriptBuilder.toString().trim()));
        }

        return chapterTranscripts;
    }


    public static class ChapterTranscript {
        private final YouTubeChapterExtractorService.Chapter chapter;
        private final String transcript;

        private String summary;

        public ChapterTranscript(YouTubeChapterExtractorService.Chapter chapter, String transcript) {
            this.chapter = chapter;
            this.transcript = transcript;
        }

        public YouTubeChapterExtractorService.Chapter getChapter() {
            return chapter;
        }

        public String getTranscript() {
            return transcript;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        @Override
        public String toString() {
            return "ChapterTranscript{" +
                    "chapter=" + chapter +
                    ", transcript='" + transcript + '\'' +
                    '}';
        }
    }
}
