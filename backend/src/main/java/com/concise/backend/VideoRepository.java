package com.concise.backend;

import com.concise.backend.model.VideoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<VideoEntity, Integer> {
}
