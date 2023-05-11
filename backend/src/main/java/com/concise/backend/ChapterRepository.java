package com.concise.backend;

import com.concise.backend.model.ChapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterRepository extends JpaRepository<ChapterEntity, Integer> {
}
