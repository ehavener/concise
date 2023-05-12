package com.concise.backend;

import com.concise.backend.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@RestController
@RequestMapping("videos")
public class VideoController {

    @Autowired
    private VideoServiceImpl videoService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping(path="/", produces = "application/json")
    public @ResponseBody List<VideoWithChaptersDto> getAllVideos() {
        Long userId = getAuthenticatedUserId();
        return videoService.getAllVideosWithChaptersByUserId(userId);
    }

    @GetMapping(path="/{id}", produces = "application/json")
    public @ResponseBody Optional<VideoDto> getVideo(@PathVariable int id) {
        Long userId = getAuthenticatedUserId();
        return videoService.getVideoByIdAndUserId(id, userId);
    }

    @GetMapping(path="/search", produces = "application/json")
    public @ResponseBody Optional<VideoWithChaptersDto> getVideo(@RequestParam String youtubeId, @RequestParam String language) {
        Long userId = getAuthenticatedUserId();
        return videoService.getVideoWithChaptersByYoutubeIdAndLanguageAndUserId(youtubeId, language, userId);
    }

    @PostMapping(path="/create", consumes = "application/json", produces = "application/json")
    public @ResponseBody VideoWithChaptersDto createVideo(@RequestBody CreateVideoDto createVideoDto) throws GeneralSecurityException, IOException {
        Long userId = getAuthenticatedUserId();
        return videoService.createVideoFromYoutubeId(createVideoDto, userId);
    }

    public Long getAuthenticatedUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            Optional<UserEntity> userEntity = userRepository.findByEmail(username);
            return userEntity.get().getId();
        } else {
            throw new RuntimeException();
        }
    }
}
