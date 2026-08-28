package com.zading.todoapi.controller;

import com.zading.todoapi.dto.ApiResponse;
import com.zading.todoapi.dto.CreateTodoRequest;
import com.zading.todoapi.dto.PageResponse;
import com.zading.todoapi.dto.TodoActionLogResponse;
import com.zading.todoapi.dto.TodoResponse;
import com.zading.todoapi.dto.UpdateTodoRequest;
import com.zading.todoapi.config.properties.RedisProtectionProperties;
import com.zading.todoapi.exception.BusinessException;
import com.zading.todoapi.exception.ErrorCode;
import com.zading.todoapi.mapper.TodoActionLogMapper;
import com.zading.todoapi.mapper.TodoMapper;
import com.zading.todoapi.model.Todo;
import com.zading.todoapi.redis.RateLimitDecision;
import com.zading.todoapi.redis.RateLimiter;
import com.zading.todoapi.security.AuthenticatedUser;
import com.zading.todoapi.service.TodoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/todos")
@Validated
public class TodoController {
    private final TodoService todoService;
    private final TodoMapper todoMapper;
    private final TodoActionLogMapper todoActionLogMapper;
    private final TodoSortParser todoSortParser;
    private final RateLimiter rateLimiter;
    private final RedisProtectionProperties redisProperties;

    public TodoController(
            TodoService todoService,
            TodoMapper todoMapper,
            TodoActionLogMapper todoActionLogMapper,
            TodoSortParser todoSortParser,
            RateLimiter rateLimiter,
            RedisProtectionProperties redisProperties
    ) {
        this.todoService = todoService;
        this.todoMapper = todoMapper;
        this.todoActionLogMapper = todoActionLogMapper;
        this.todoSortParser = todoSortParser;
        this.rateLimiter = rateLimiter;
        this.redisProperties = redisProperties;
    }

    @GetMapping
    public ApiResponse<PageResponse<TodoResponse>> getTodos(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page 不能小于 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size 不能小于 1") @Max(value = 100, message = "size 不能大于 100") int size,
            @RequestParam(defaultValue = "id,asc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, todoSortParser.parse(sort));
        Page<Todo> todos = todoService.getTodos(currentUser.getId(), completed, keyword, pageable);
        List<TodoResponse> items = todoMapper.toResponseList(todos);

        return ApiResponse.success(PageResponse.from(todos, items));
    }

    @GetMapping("/{id}")
    public ApiResponse<TodoResponse> getTodo(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return ApiResponse.success(todoMapper.toResponse(todoService.getTodo(currentUser.getId(), id)));
    }

    @GetMapping("/{id}/logs")
    public ApiResponse<List<TodoActionLogResponse>> getTodoLogs(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id
    ) {
        List<TodoActionLogResponse> logs = todoActionLogMapper.toResponseList(
                todoService.getTodoLogs(currentUser.getId(), id)
        );

        return ApiResponse.success(logs);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TodoResponse>> createTodo(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CreateTodoRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        RateLimitDecision decision = rateLimiter.tryAcquire(
                "rate-limit:todo-create:user:" + currentUser.getId(),
                redisProperties.todoCreateLimit(),
                redisProperties.rateLimitWindow()
        );

        if (!decision.allowed()) {
            throw new BusinessException(
                    ErrorCode.RATE_LIMIT_EXCEEDED,
                    "Todo 创建请求过于频繁，请在 " + decision.retryAfterSeconds() + " 秒后重试"
            );
        }

        Todo createdTodo = todoService.addTodo(
                currentUser.getId(),
                request.getTitle(),
                request.getPriority(),
                request.getDueDate(),
                idempotencyKey
        );
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdTodo.getId())
                .toUri();

        return ResponseEntity.created(location).body(ApiResponse.created(todoMapper.toResponse(createdTodo)));
    }

    @PatchMapping("/{id}")
    public ApiResponse<TodoResponse> updateTodo(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTodoRequest request
    ) {
        return ApiResponse.success(todoMapper.toResponse(todoService.updateTodo(
                currentUser.getId(),
                id,
                request.getTitle(),
                request.getCompleted(),
                request.getPriority(),
                request.getDueDate()
        )));
    }

    @PatchMapping("/{id}/toggle")
    public ApiResponse<TodoResponse> toggleTodo(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return ApiResponse.success(todoMapper.toResponse(todoService.toggleTodo(currentUser.getId(), id)));
    }

    @PatchMapping("/{id}/restore")
    public ApiResponse<TodoResponse> restoreTodo(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return ApiResponse.success("恢复成功", todoMapper.toResponse(todoService.restoreTodo(currentUser.getId(), id)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTodo(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        todoService.deleteTodo(currentUser.getId(), id);
        return ApiResponse.success("删除成功", null);
    }

}
