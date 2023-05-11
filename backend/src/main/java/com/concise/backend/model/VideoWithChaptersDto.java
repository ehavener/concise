package com.concise.backend.model;

import com.concise.backend.ChapterDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoWithChaptersDto implements Serializable {
    private Integer id;
    private String language;
    private String summary;
    private String title;
    private String transcript;
    private List<ChapterDto> chapterDtos;

    public VideoWithChaptersDto(VideoEntity video, List<ChapterEntity> chapters) {
        this.id = video.getId();
        this.language = video.getLanguage();
        this.summary = video.getSummary();
        this.title = video.getTitle();
        this.transcript = video.getTranscript();

        this.chapterDtos = new ArrayList<>();
        for(ChapterEntity chapter : chapters) {
            ChapterDto chapterDto = new ChapterDto(chapter);
            this.chapterDtos.add(chapterDto);
        }
    }
}
