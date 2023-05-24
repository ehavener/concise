package com.concise.backend;

import com.concise.backend.model.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.youtube.model.ThumbnailDetails;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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

    private final WebClient webClient;

    public VideoServiceImpl(WebClient webClient) {
        this.webClient = webClient;
    }

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

    public VideoPreviewDto getVideoPreviewByYoutubeId(String youtubeId) throws GeneralSecurityException, IOException {
        YouTubeChapterExtractorService.VideoData videoData = YouTubeChapterExtractorService.getVideoTimelineById(youtubeId);
        String videoTitle = videoData.getVideoTitle();
        ThumbnailDetails thumbnailDetails = videoData.getThumbnails();
        return new VideoPreviewDto(videoTitle, thumbnailDetails.getHigh().getUrl());
    }

    public VideoWithChaptersDto createVideoFromYoutubeId(CreateVideoDto createVideoDto, UserEntity user) throws GeneralSecurityException, IOException {
        // Get chapter names and timestamps with from YouTube Data API using YouTubeChapterExtractorService.getVideoTimelineById
        YouTubeChapterExtractorService.VideoData videoData = YouTubeChapterExtractorService.getVideoTimelineById(createVideoDto.getYoutubeId());
        String videoTitle = videoData.getVideoTitle();
        ThumbnailDetails thumbnailDetails = videoData.getThumbnails();
        List<YouTubeChapterExtractorService.Chapter> chapters = videoData.getChapters();

        // Get transcript with HTTP request to youtube-transcript-api microservice
        String jsonString = getTranscript(createVideoDto.getYoutubeId(), user.getLanguage());

        // Transcript is a string of JSON in format {"language": "en", "transcript":[ {"text":"- Welcome to the Huberman Lab Podcast,","start":0.36,"duration":1.56} ]}
        // Parse the JSON transcript to a java class
        TranscriptContainer transcriptContainer = readTranscriptFromJson(jsonString);

        // Parse the java transcript class to a string
        String fullTranscript = transcriptContainer.getTranscript().stream()
                .map(TranscriptEntry::getText)  // Get the text property of each TranscriptEntry
                .collect(Collectors.joining(" "))
                .replaceAll("\\r?\\n|\\r", " ");

        // Get summary of full transcript with HTTP request to summarization-api microservice
        String fullSummary = getSummaryAsync(fullTranscript).block();

        // Store video in database
        VideoEntity videoEntity = new VideoEntity();
        videoEntity.setYoutubeId(createVideoDto.getYoutubeId());
        videoEntity.setThumbnailUrl(thumbnailDetails.getHigh().getUrl());
        videoEntity.setTitle(videoTitle);
        videoEntity.setTranscript(fullTranscript);
        videoEntity.setSummary(fullSummary);
        videoEntity.setLanguage("en");
        videoEntity.setUser(userRepository.findById(user.getId()).get());
        VideoEntity createdVideo = addVideo(videoEntity);

        // TODO: Application may hang when chapters=[]; application only fetches chapters that are defined in a description (youtube now auto-generates chapters from video)
        // TODO: ...need to test this with a video that has no chapters
        // Map Transcript object to chapters using chapters from YouTube Data API and transcript from youtube-transcript-api microservice
        List<ChapterTranscript> chapterTranscripts = createChapterTranscripts(chapters, transcriptContainer);

        // Get summaries of chapter transcripts with HTTP requests to summarization-api microservice. Note: performing this synchronously takes 2.5 minutes for a 1 hr video with 16 chapters.
        // Async brings the time down to 1.5 minutes, but still too slow. Maybe try a card with more FLOPS?
        List<Mono<String>> summaries = new ArrayList<>();
        for (int i = 0; i < chapterTranscripts.size(); i++) {
            summaries.add(getSummaryAsync(chapterTranscripts.get(i).getTranscript()));
        }
        Flux.fromIterable(summaries)
                .flatMap(Function.identity())
                .collectList()
                .doOnNext(collectedSummaries -> {
                    for (int i = 0; i < collectedSummaries.size(); i++) {
                        chapterTranscripts.get(i).setSummary(collectedSummaries.get(i));
                    }
                })
                .block();


        // TODO: Translate summaries if necessary with HTTP request to NLLB translation-api microservice

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
        VideoWithChaptersDto createdVideoWithChaptersDto = new VideoWithChaptersDto(createdVideo, createdChapters);

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

    public Mono<String> getSummaryAsync(String transcript) {
        String prompt = "Summarize the following video transcript text into a 5-10 sentence paragraph.\n\n" + transcript;

        return webClient.post()
                .uri(summarizationApiUrl)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("X-API-Key", summarizationApiKey)
                .bodyValue(Map.of("text", prompt))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(jsonNode -> jsonNode.get("summary").asText());
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
