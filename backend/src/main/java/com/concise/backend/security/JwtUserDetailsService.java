package com.concise.backend.security;

import com.concise.backend.UserRepository;
import com.concise.backend.model.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;


@Service
public class JwtUserDetailsService implements UserDetailsService {

    public static final String USER = "USER";
    public static final String ROLE_USER = "ROLE_" + USER;

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(final String username) {
        final UserEntity user = userRepository.findByEmail(username).orElseThrow(
                () -> new UsernameNotFoundException("User " + username + " not found"));
        return new JwtUserDetails(user.getId(), username, user.getPassword(), Collections.singletonList(new SimpleGrantedAuthority(ROLE_USER)));
    }

}
