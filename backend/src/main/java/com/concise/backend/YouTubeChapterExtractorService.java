package com.concise.backend;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ThumbnailDetails;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.YouTubeRequestInitializer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class YouTubeChapterExtractorService {

    private static final String API_SERVICE_NAME = "youtube";
    private static final String API_VERSION = "v3";

    @Value("${youtube.api.key}")
    private String API_KEY;

    private static String apiKeyStatic;

    @PostConstruct
    public void init() {
        apiKeyStatic = API_KEY;
    }

    private static YouTube getService() throws GeneralSecurityException, IOException {
        return new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                new GsonFactory(),
                null)
                .setApplicationName(API_SERVICE_NAME)
                .setYouTubeRequestInitializer(new YouTubeRequestInitializer(apiKeyStatic))
                .build();
    }

    public static class VideoData {
        private final String videoTitle;
        private final List<Chapter> chapters;
        private final ThumbnailDetails thumbnails;

        public VideoData(String videoTitle, List<Chapter> chapters, ThumbnailDetails thumbnails) {
            this.videoTitle = videoTitle;
            this.chapters = chapters;
            this.thumbnails = thumbnails;
        }

        public String getVideoTitle() {
            return videoTitle;
        }

        public List<Chapter> getChapters() {
            return chapters;
        }

        public ThumbnailDetails getThumbnails() {
            return thumbnails;
        }
    }


    public static class Chapter {
        private final int startTimeSeconds;
        private final String timestamp;
        private final String title;

        public Chapter(String timestamp, String title) {
            this.timestamp = timestamp;
            this.title = title;
            this.startTimeSeconds = convertToSeconds(timestamp);
        }

        public int getStartTimeSeconds() {
            return startTimeSeconds;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getTitle() {
            return title;
        }

        @Override
        public String toString() {
            return "Chapter{" +
                    "timestamp='" + timestamp + '\'' +
                    ", title='" + title + '\'' +
                    ", startTimeSeconds=" + startTimeSeconds +
                    '}';
        }

        private int convertToSeconds(String timestamp) {
            String[] parts = timestamp.split(":");
            int seconds = 0;
            for (int i = 0; i < parts.length; i++) {
                seconds = seconds * 60 + Integer.parseInt(parts[i]);
            }
            return seconds;
        }
    }

    public static VideoData getVideoTimelineById(String videoId) throws GeneralSecurityException, IOException {
        YouTube youtubeService = getService();
        YouTube.Videos.List request = youtubeService.videos().list("id,snippet,contentDetails");
        request.setId(videoId);

        VideoListResponse response = request.execute();
        Video video = response.getItems().get(0);
        String description = video.getSnippet().getDescription();
        String videoTitle = video.getSnippet().getTitle();
        ThumbnailDetails thumbnails = video.getSnippet().getThumbnails();

        Pattern pattern = Pattern.compile("((?:(?:[01]?\\d|2[0-3]):)?(?:[0-5]?\\d):(?:[0-5]?\\d))(.+)");
        Matcher matcher = pattern.matcher(description);

        List<Chapter> timeline = new ArrayList<>();
        while (matcher.find()) {
            timeline.add(new Chapter(matcher.group(1), matcher.group(2)));
        }

        return new VideoData(videoTitle, timeline, thumbnails);
    }
}
