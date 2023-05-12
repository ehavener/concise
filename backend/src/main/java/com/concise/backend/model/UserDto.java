package com.concise.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto implements Serializable {
    private Long id;
    private String email;
    private String password;

    public UserDto(UserEntity user) {
        if (user.getId() != null) {
            this.id = user.getId();
        }
        if (user.getEmail() != null) {
            this.email = user.getEmail();
        }
        if (user.getPassword() != null) {
            this.password = user.getPassword();
        }
    }
}