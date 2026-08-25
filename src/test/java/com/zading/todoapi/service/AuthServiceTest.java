package com.zading.todoapi.service;

import com.zading.todoapi.dto.LoginResponse;
import com.zading.todoapi.dto.UserResponse;
import com.zading.todoapi.exception.BusinessException;
import com.zading.todoapi.exception.UnauthorizedException;
import com.zading.todoapi.model.AppUser;
import com.zading.todoapi.model.UserRole;
import com.zading.todoapi.repository.UserRepository;
import com.zading.todoapi.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterUserWithDefaultUserRole() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(" alice ", "secret");

        assertEquals("alice", response.getUsername());
        assertEquals(UserRole.USER, response.getRole());
        verify(passwordEncoder).encode("secret");
        verify(userRepository).save(any(AppUser.class));
    }

    @Test
    void shouldRejectDuplicateUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.register("alice", "secret")
        );

        assertEquals("DUPLICATE_USERNAME", exception.getErrorCode().name());
        verify(passwordEncoder, never()).encode(any(String.class));
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void shouldLoginAndGenerateJwtWithCurrentRole() {
        AppUser user = user(1L, "alice", UserRole.ADMIN);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "encoded-secret")).thenReturn(true);
        when(jwtService.generateToken("alice", UserRole.ADMIN)).thenReturn("admin-token");

        LoginResponse response = authService.login(" alice ", "secret");

        assertEquals("admin-token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        verify(jwtService).generateToken("alice", UserRole.ADMIN);
    }

    @Test
    void shouldRejectLoginWhenPasswordIsWrong() {
        AppUser user = user(1L, "alice", UserRole.USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-secret")).thenReturn(false);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login("alice", "wrong")
        );

        assertEquals("INVALID_CREDENTIALS", exception.getErrorCode().name());
        verify(jwtService, never()).generateToken(any(String.class), any(UserRole.class));
    }

    @Test
    void shouldRejectBlankUsernameBeforeCallingRepository() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login("  ", "secret")
        );

        assertTrue(exception.getMessage().contains("用户名不能为空"));
        verify(userRepository, never()).findByUsername(any(String.class));
    }

    private AppUser user(Long id, String username, UserRole role) {
        AppUser user = new AppUser(username, "encoded-secret");
        user.setId(id);
        user.setRole(role);
        return user;
    }
}
