package com.concise.backend.security;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthenticationResource {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @PostMapping("/authenticate")
    public AuthenticationResponse authenticate(@RequestBody @Valid final AuthenticationRequest authenticationRequest) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    authenticationRequest.getLogin(), authenticationRequest.getPassword()));
        } catch (final BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        final UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(authenticationRequest.getLogin());
        final AuthenticationResponse authenticationResponse = new AuthenticationResponse();
        authenticationResponse.setAccessToken(jwtTokenService.generateToken(userDetails));
        return authenticationResponse;
    }
}
