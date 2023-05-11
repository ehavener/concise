package com.concise.backend;

import com.concise.backend.model.UserEntity;
import com.concise.backend.model.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto addUser(UserDto userDto) {
        UserEntity user = new UserEntity(userDto);
        UserEntity savedUser = userRepository.saveAndFlush(user);
        UserDto responseUserDto = new UserDto(savedUser);
        responseUserDto.setPassword(null);
        return responseUserDto;
    }

    public List<String> userLogin(UserDto userDto) {
        List<String> response = new ArrayList<>();
        Optional<UserEntity> userOptional = userRepository.findByEmail(userDto.getEmail());
        if (userOptional.isPresent()) {
            if (passwordEncoder.matches(userDto.getPassword(), userOptional.get().getPassword())) {
                response.add(userOptional.get().getId().toString());
            } else {
                response.add("Username or password incorrect");
            }
        } else {
            response.add("Username or password incorrect");
        }
        return response;
    }
}
