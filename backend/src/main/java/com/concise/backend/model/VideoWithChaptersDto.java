package com.concise.backend.model;

import com.concise.backend.ChapterDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoWithChaptersDto implements Serializable {
    private Long id;
    private LocalDateTime createdAt;
    private String summary;
    private String summaryLanguage;
    private String thumbnailUrl;
    private String title;
    private String transcript;
    private List<ChapterDto> chapterDtos;

    public VideoWithChaptersDto(VideoEntity video, List<ChapterEntity> chapters) {
        this.id = video.getId();
        this.createdAt = video.getCreatedAt();
        this.summary = video.getSummary();
        this.summaryLanguage = video.getSummaryLanguage();
        this.thumbnailUrl = video.getThumbnailUrl();
        this.title = video.getTitle();
        this.transcript = video.getTranscript();

        this.chapterDtos = new ArrayList<>();
        for(ChapterEntity chapter : chapters) {
            ChapterDto chapterDto = new ChapterDto(chapter);
            this.chapterDtos.add(chapterDto);
        }
    }
}
