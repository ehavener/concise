package com.concise.backend;

import com.concise.backend.model.VideoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.util.List;

public interface VideoRepository extends JpaRepository<VideoEntity, Integer> {
    List<VideoEntity> findAllByYoutubeIdAndSummaryLanguageAndUserId(String youtubeId, String language, long userId);
    List<VideoEntity> findAllByUserIdOrderByIdDesc(Long userId);
    Optional<VideoEntity> findByIdAndUserId(Long id, Long userId);
}
