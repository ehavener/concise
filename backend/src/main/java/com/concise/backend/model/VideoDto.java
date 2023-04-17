package com.concise.backend.model;

import com.concise.backend.model.VideoEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoDto implements Serializable {
    private Integer id;
    private String body;
    private UserDto userDto;

    public VideoDto(VideoEntity video) {
        if (video.getId() != null) {
            this.id = video.getId();
        }
    }
}
