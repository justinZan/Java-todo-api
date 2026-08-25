package com.zading.todoapi.service;

import com.zading.todoapi.dto.AdminStatisticsResponse;
import com.zading.todoapi.dto.AdminTodoResponse;
import com.zading.todoapi.dto.AdminUserResponse;
import com.zading.todoapi.model.AppUser;
import com.zading.todoapi.model.Todo;
import com.zading.todoapi.model.TodoPriority;
import com.zading.todoapi.model.UserRole;
import com.zading.todoapi.repository.TodoRepository;
import com.zading.todoapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
    @Mock
    private TodoRepository todoRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void shouldMapUsersToAdminResponses() {
        PageRequest pageable = PageRequest.of(0, 10);
        AppUser user = user(1L, "alice", UserRole.USER);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        Page<AdminUserResponse> result = adminService.getUsers(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("alice", result.getContent().getFirst().getUsername());
        assertEquals(UserRole.USER, result.getContent().getFirst().getRole());
        verify(userRepository).findAll(pageable);
    }

    @Test
    void shouldExcludeDeletedTodosByDefault() {
        PageRequest pageable = PageRequest.of(0, 10);
        Todo todo = todo(10L, user(1L, "alice", UserRole.USER));
        when(todoRepository.findByDeletedFalse(pageable)).thenReturn(new PageImpl<>(List.of(todo), pageable, 1));

        Page<AdminTodoResponse> result = adminService.getTodos(false, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("alice", result.getContent().getFirst().getUsername());
        verify(todoRepository).findByDeletedFalse(pageable);
    }

    @Test
    void shouldIncludeDeletedTodosWhenRequested() {
        PageRequest pageable = PageRequest.of(0, 10);
        Todo todo = todo(10L, user(1L, "alice", UserRole.USER));
        todo.setDeleted(true);
        when(todoRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(todo), pageable, 1));

        Page<AdminTodoResponse> result = adminService.getTodos(true, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(true, result.getContent().getFirst().isDeleted());
        verify(todoRepository).findAll(pageable);
    }

    @Test
    void shouldCollectTodoStatistics() {
        when(userRepository.count()).thenReturn(3L);
        when(todoRepository.count()).thenReturn(10L);
        when(todoRepository.countByDeletedFalse()).thenReturn(8L);
        when(todoRepository.countByDeletedFalseAndCompletedTrue()).thenReturn(5L);
        when(todoRepository.countByDeletedFalseAndCompletedFalse()).thenReturn(3L);
        when(todoRepository.countByDeletedTrue()).thenReturn(2L);

        AdminStatisticsResponse result = adminService.getStatistics();

        assertEquals(3L, result.getTotalUsers());
        assertEquals(10L, result.getTotalTodos());
        assertEquals(8L, result.getActiveTodos());
        assertEquals(5L, result.getCompletedTodos());
        assertEquals(3L, result.getPendingTodos());
        assertEquals(2L, result.getDeletedTodos());
    }

    private AppUser user(Long id, String username, UserRole role) {
        AppUser user = new AppUser(username, "encoded-password");
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private Todo todo(Long id, AppUser user) {
        Todo todo = new Todo(id, "管理员可见任务", false);
        todo.setUser(user);
        todo.setPriority(TodoPriority.MEDIUM);
        return todo;
    }
}
