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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
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

    public List<VideoWithChaptersDto> getAllVideosWithChaptersByUserId(Long userId) {
        return videoRepository.findByUserId(userId).stream()
                .map(video -> new VideoWithChaptersDto(video, chapterService.getChaptersByVideoId(video.getId())))
                .toList();
    }

    public Optional<VideoDto> getVideoByIdAndUserId(long id, long userId) {
        Optional<VideoEntity> videoOptional = videoRepository.findByIdAndUserId(id, userId);
        if (videoOptional.isPresent()) {
            return Optional.of(new VideoDto(videoOptional.get()));
        }
        return Optional.empty();
    }

    public Optional<VideoWithChaptersDto> getVideoWithChaptersByYoutubeIdAndLanguageAndUserId(String youtubeId, String language, long userId) {
        Optional<VideoEntity> videoOptional = videoRepository.findByYoutubeIdAndLanguageAndUserId(youtubeId, language, userId);
        if (videoOptional.isPresent()) {
            VideoEntity video = videoOptional.get();
            List<ChapterEntity> chapters = chapterService.getChaptersByVideoId(video.getId());
            return Optional.of(new VideoWithChaptersDto(video, chapters));
        } else {
            return Optional.empty();
        }
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

        // summarize chapter-transcripts with HTTP request to Cohere API
        if (chapterTranscripts.size() > 0) {
            // TODO: only summarizing first chapter for now
            String cohereResponse = getChapterSummary(chapterTranscripts.get(0).getTranscript());
            chapterTranscripts.get(0).setSummary(cohereResponse);
        }

        String fullSummary = getFullSummary(fullTranscript);

        // TODO: Translate summaries if necessary with HTTP request to NLLB microservice

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

    public static String getChapterSummary(String chapterTranscript) {
        // Limit is 100,000 characters. 1 minute of audio is roughly 1000 characters.
        // Only first 1:40:00 of a chapter is summarized TODO: consider extending this
        chapterTranscript = chapterTranscript.substring(0, Math.min(chapterTranscript.length(), 10000));

        CohereApiService cohereApiService = new CohereApiService();
        String text = chapterTranscript;
        String length = "medium";
        String format = "paragraph";
        String model = "summarize-xlarge";
        String additionalCommand = "This is a transcript of a chapter from a video.";
        double temperature = 0.3;

        String jsonString = cohereApiService.summarize(text, length, format, model, additionalCommand, temperature);
                ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = null;
        try {
            rootNode = objectMapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String summary = rootNode.get("summary").asText();
        return summary;
    }

    public static String getFullSummary(String fullTranscript) {
        // Limit is 100,000 characters but in practice seems closer to 10,000.
        // 1 minute of audio is roughly 1000 characters.
        // Only first 1:40:00/10 minutes of video is summarized TODO: consider extending this
        fullTranscript = fullTranscript.substring(0, Math.min(fullTranscript.length(), 10000));

        CohereApiService cohereApiService = new CohereApiService();
        String text = fullTranscript;
        String length = "long";
        String format = "paragraph";
        String model = "summarize-xlarge";
        String additionalCommand = "This is a transcript of a video.";
        double temperature = 0.3;

        String jsonString = cohereApiService.summarize(text, length, format, model, additionalCommand, temperature);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = null;
        try {
            rootNode = objectMapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String summary = rootNode.get("summary").asText();
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
