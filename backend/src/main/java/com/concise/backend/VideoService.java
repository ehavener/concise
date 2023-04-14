package com.concise.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class VideoService {
    @Autowired
    public VideoService(@Qualifier("video") Video video) {
        this.video = video;
    }

    private Video video;

    public Video getVideo() {

        return video;
    }
}