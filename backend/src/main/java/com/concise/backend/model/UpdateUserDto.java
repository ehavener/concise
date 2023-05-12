package com.concise.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserDto implements Serializable {
    private Long id;
    private String language;

    public UpdateUserDto(UserEntity user) {
        if (user.getId() != null) {
            this.language = user.getLanguage();
        }
    }
}
