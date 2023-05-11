package com.concise.backend;

import com.concise.backend.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

@RestController
@RequestMapping("videos")
public class VideoController {

    @Autowired
    private VideoServiceImpl videoService;

    @GetMapping(path="/{id}", produces = "application/json")
    public @ResponseBody Optional<VideoDto> getVideo(@PathVariable int id) {
        return videoService.getVideoById(id);
    }

    @PostMapping(path="/create", consumes = "application/json", produces = "application/json")
    public @ResponseBody VideoWithChaptersDto createVideo(@RequestBody CreateVideoDto createVideoDto) throws GeneralSecurityException, IOException {
        return videoService.createVideoFromYoutubeId(createVideoDto);
    }
}
