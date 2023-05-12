package com.concise.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;

import java.util.List;

@Entity
@AllArgsConstructor
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name="email", unique = true)
    private String email;

    @Column(name="password")
    private String password;

    @OneToMany()
    @JoinColumn(name="user_id")
    private List<VideoEntity> videos;

    public UserEntity() {}

    public UserEntity(UserDto userDto) {
        if (userDto.getEmail() != null) {
            this.email = userDto.getEmail();
        }
        if (userDto.getPassword() != null) {
            this.password = userDto.getPassword();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<VideoEntity> getVideos() {
        return videos;
    }

    public void setVotes(List<VideoEntity> videos) {
        this.videos = videos;
    }

    public void addVideo(VideoEntity video) {
        this.videos.add(video);
    }
}