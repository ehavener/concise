package com.concise.backend.security;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public class AuthenticationRequest {
    @NotNull
    @Size(max = 255)
    private String login;

    @NotNull
    @Size(max = 255)
    private String password;

    public String getLogin() {
        return login;
    }

    public Object getPassword() {
        return password;
    }
}
