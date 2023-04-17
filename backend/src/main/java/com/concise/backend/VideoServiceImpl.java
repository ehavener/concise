package com.concise.backend;

import com.concise.backend.model.VideoDto;
import com.concise.backend.model.VideoEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class VideoServiceImpl {
    @Autowired
    private VideoRepository videoRepository;

    public Optional<VideoDto> getVideoById(int videoId) {
        Optional<VideoEntity> noteOptional = videoRepository.findById(videoId);
        if (noteOptional.isPresent()) {
            return Optional.of(new VideoDto(noteOptional.get()));
        }
        return Optional.empty();
    }
}
