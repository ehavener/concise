package com.concise.backend;

import com.concise.backend.model.VideoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<VideoEntity, Integer> {
    Optional<VideoEntity> findByYoutubeIdAndLanguageAndUserId(String youtubeId, String language, long userId);
    Optional<VideoEntity> findByUserId(Long userId);
    Optional<VideoEntity> findByIdAndUserId(Long id, Long userId);
}
