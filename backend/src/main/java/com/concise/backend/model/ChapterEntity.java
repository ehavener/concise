package com.concise.backend.model;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "chapters")
public class ChapterEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chapter_id")
    private Integer id;

    // TODO: video_id
    // TODO: user_id

    @Column(name="title")
    private String title;

    @Column(name="transcript")
    private String transcript;

    @Column(name="summary")
    private String summary;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}