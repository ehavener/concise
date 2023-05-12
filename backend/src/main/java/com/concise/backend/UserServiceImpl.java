package com.concise.backend;

import com.concise.backend.model.UpdateUserDto;
import com.concise.backend.model.UserEntity;
import com.concise.backend.model.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class UserServiceImpl {
    @Autowired
    private UserRepository userRepository;

    @Transactional
    public UserDto addUser(UserDto userDto) {
        UserEntity user = new UserEntity(userDto);
        UserEntity savedUser = userRepository.saveAndFlush(user);
        UserDto responseUserDto = new UserDto(savedUser);
        responseUserDto.setPassword(null);
        return responseUserDto;
    }

    @Transactional
    public UpdateUserDto updateUser(UpdateUserDto updateUserDto) {
        UserEntity user = userRepository.getById(updateUserDto.getId());
        user.setLanguage(updateUserDto.getLanguage());
        UserEntity savedUser = userRepository.saveAndFlush(user);
        UpdateUserDto responseUserDto = new UpdateUserDto(savedUser);
        return responseUserDto;
    }
}
