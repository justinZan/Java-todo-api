package com.zading.todoapi.controller;

import com.zading.todoapi.dto.AdminStatisticsResponse;
import com.zading.todoapi.dto.AdminTodoResponse;
import com.zading.todoapi.dto.AdminUserResponse;
import com.zading.todoapi.dto.ApiResponse;
import com.zading.todoapi.dto.PageResponse;
import com.zading.todoapi.exception.BusinessException;
import com.zading.todoapi.exception.ErrorCode;
import com.zading.todoapi.service.AdminService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private static final Set<String> USER_SORT_FIELDS = Set.of("id", "username", "role", "createdAt", "updatedAt");
    private static final Set<String> TODO_SORT_FIELDS = Set.of(
            "id", "title", "completed", "deleted", "priority", "dueDate", "completedAt", "deletedAt", "createdAt", "updatedAt"
    );

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<AdminUserResponse>> getUsers(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page 不能小于 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size 不能小于 1") @Max(value = 100, message = "size 不能大于 100") int size,
            @RequestParam(defaultValue = "id,asc") String sort
    ) {
        Page<AdminUserResponse> users = adminService.getUsers(PageRequest.of(page, size, parseSort(sort, USER_SORT_FIELDS)));
        return ApiResponse.success(PageResponse.from(users, users.getContent()));
    }

    @GetMapping("/todos")
    public ApiResponse<PageResponse<AdminTodoResponse>> getTodos(
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page 不能小于 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size 不能小于 1") @Max(value = 100, message = "size 不能大于 100") int size,
            @RequestParam(defaultValue = "id,asc") String sort
    ) {
        Page<AdminTodoResponse> todos = adminService.getTodos(
                includeDeleted,
                PageRequest.of(page, size, parseSort(sort, TODO_SORT_FIELDS))
        );
        return ApiResponse.success(PageResponse.from(todos, todos.getContent()));
    }

    @GetMapping("/statistics")
    public ApiResponse<AdminStatisticsResponse> getStatistics() {
        return ApiResponse.success(adminService.getStatistics());
    }

    private Sort parseSort(String sort, Set<String> allowedFields) {
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim() : "asc";

        if (!allowedFields.contains(field)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的排序字段: " + field);
        }

        if (Arrays.stream(Sort.Direction.values())
                .map(Enum::name)
                .noneMatch(name -> name.equalsIgnoreCase(direction))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的排序方向: " + direction);
        }

        return Sort.by(Sort.Direction.fromString(direction), field);
    }
}
