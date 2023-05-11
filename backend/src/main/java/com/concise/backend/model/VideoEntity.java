package com.concise.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "videos")
public class VideoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_id")
    private Long id;

    @OneToMany(mappedBy = "video")
    private List<ChapterEntity> chapters;

    @Column(name="title")
    private String title;

    @Column(name="transcript")
    private String transcript;

    @Column(name="summary")
    private String summary;

    @Column(name="language")
    private String language;

    @Column(name="youtube_id")
    private String youtubeId;

    @ManyToOne()
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    @JsonBackReference
    private UserEntity user;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getYoutubeId() {
        return youtubeId;
    }

    public void setYoutubeId(String youtubeId) {
        this.youtubeId = youtubeId;
    }

    public List<ChapterEntity> getChapters() {
        return chapters;
    }

    public void addChapter(ChapterEntity chapter) {
        this.chapters.add(chapter);
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }
}