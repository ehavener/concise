package com.concise.backend;

import com.concise.backend.model.ChapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterRepository extends JpaRepository<ChapterEntity, Integer> {
    List<ChapterEntity> findByVideoId(Long videoId);
}
