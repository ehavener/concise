package com.concise.backend;

import com.concise.backend.model.UpdateUserDto;
import com.concise.backend.model.UserDto;
import com.concise.backend.model.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("users")
public class UserController {
    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/sign-up")
    public UserDto addUser(@RequestBody UserDto userDto) {
        String passHash = passwordEncoder.encode(userDto.getPassword());
        userDto.setPassword(passHash);
        return userService.addUser(userDto);
    }

    @PostMapping("/update_self")
    public UpdateUserDto updateUser(@RequestBody UpdateUserDto updateUserDto) {
        Long userId = getAuthenticatedUserId();
        if (!userId.equals(updateUserDto.getId())) {
            throw new RuntimeException();
        }
        return userService.updateUser(updateUserDto);
    }

    public Long getAuthenticatedUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            Optional<UserEntity> userEntity = userRepository.findByEmail(username);
            return userEntity.get().getId();
        } else {
            throw new RuntimeException();
        }
    }
}
