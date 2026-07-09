package com.zading.todoapi.controller;

import com.zading.todoapi.dto.CreateTodoRequest;
import com.zading.todoapi.dto.PageResponse;
import com.zading.todoapi.dto.TodoResponse;
import com.zading.todoapi.dto.UpdateTodoRequest;
import com.zading.todoapi.mapper.TodoMapper;
import com.zading.todoapi.model.Todo;
import com.zading.todoapi.service.TodoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/todos")
@Validated
public class TodoController {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "title",
            "completed",
            "priority",
            "dueDate",
            "createdAt",
            "updatedAt"
    );

    private final TodoService todoService;
    private final TodoMapper todoMapper;

    public TodoController(TodoService todoService, TodoMapper todoMapper) {
        this.todoService = todoService;
        this.todoMapper = todoMapper;
    }

    @GetMapping
    public PageResponse<TodoResponse> getTodos(
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page 不能小于 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size 不能小于 1") @Max(value = 100, message = "size 不能大于 100") int size,
            @RequestParam(defaultValue = "id,asc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<Todo> todos = todoService.getTodos(completed, keyword, pageable);
        List<TodoResponse> items = todoMapper.toResponseList(todos);

        return PageResponse.from(todos, items);
    }

    @GetMapping("/{id}")
    public TodoResponse getTodo(@PathVariable Long id) {
        return todoMapper.toResponse(todoService.getTodo(id));
    }

    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(@Valid @RequestBody CreateTodoRequest request) {
        Todo createdTodo = todoService.addTodo(request.getTitle(), request.getPriority(), request.getDueDate());
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdTodo.getId())
                .toUri();

        return ResponseEntity.created(location).body(todoMapper.toResponse(createdTodo));
    }

    @PatchMapping("/{id}")
    public TodoResponse updateTodo(@PathVariable Long id, @Valid @RequestBody UpdateTodoRequest request) {
        return todoMapper.toResponse(todoService.updateTodo(
                id,
                request.getTitle(),
                request.getCompleted(),
                request.getPriority(),
                request.getDueDate()
        ));
    }

    @PatchMapping("/{id}/toggle")
    public TodoResponse toggleTodo(@PathVariable Long id) {
        return todoMapper.toResponse(todoService.toggleTodo(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(@PathVariable Long id) {
        todoService.deleteTodo(id);
        return ResponseEntity.noContent().build();
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim() : "asc";

        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new IllegalArgumentException("不支持的排序字段: " + field);
        }

        Sort.Direction sortDirection = parseDirection(direction);
        return Sort.by(sortDirection, field);
    }

    private Sort.Direction parseDirection(String direction) {
        if (Arrays.stream(Sort.Direction.values()).map(Enum::name).anyMatch(name -> name.equalsIgnoreCase(direction))) {
            return Sort.Direction.fromString(direction);
        }

        throw new IllegalArgumentException("不支持的排序方向: " + direction);
    }
}
