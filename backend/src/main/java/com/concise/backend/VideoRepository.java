package com.concise.backend;

import com.concise.backend.model.VideoEntity;
import com.concise.backend.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface VideoRepository extends JpaRepository<VideoEntity, Integer> {
}
