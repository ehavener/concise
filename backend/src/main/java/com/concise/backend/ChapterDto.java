package com.concise.backend;

import com.concise.backend.model.ChapterEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChapterDto implements Serializable {
    private Integer id;
    private String summary;
    private int startTimeSeconds;
    private String title;
    private String transcript;

    public ChapterDto(ChapterEntity chapter) {
        this.id = chapter.getId();
        this.summary = chapter.getSummary();
        this.startTimeSeconds = chapter.getStartTimeSeconds();
        this.title = chapter.getTitle();
        this.transcript = chapter.getTranscript();
    }
}
