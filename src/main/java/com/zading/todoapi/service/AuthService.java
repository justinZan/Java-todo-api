package com.zading.todoapi.service;

import com.zading.todoapi.dto.LoginResponse;
import com.zading.todoapi.dto.UserResponse;
import com.zading.todoapi.exception.BusinessException;
import com.zading.todoapi.exception.ErrorCode;
import com.zading.todoapi.exception.UnauthorizedException;
import com.zading.todoapi.model.AppUser;
import com.zading.todoapi.repository.UserRepository;
import com.zading.todoapi.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse register(String username, String password) {
        String normalizedUsername = normalizeUsername(username);

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME, "用户名已存在");
        }

        AppUser user = new AppUser(normalizedUsername, passwordEncoder.encode(password));
        AppUser savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    public LoginResponse login(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        AppUser user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new UnauthorizedException("用户名或密码错误"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        return new LoginResponse(jwtService.generateToken(user.getUsername()));
    }

    private String normalizeUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        return username.trim();
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getCreatedAt());
    }
}
