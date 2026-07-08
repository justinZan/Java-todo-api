package com.zading.todoapi.controller;

import com.zading.todoapi.dto.CreateTodoRequest;
import com.zading.todoapi.dto.TodoResponse;
import com.zading.todoapi.dto.UpdateTodoRequest;
import com.zading.todoapi.mapper.TodoMapper;
import com.zading.todoapi.model.Todo;
import com.zading.todoapi.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
import java.util.List;

@RestController
@RequestMapping("/api/todos")
public class TodoController {
    private final TodoService todoService;
    private final TodoMapper todoMapper;

    public TodoController(TodoService todoService, TodoMapper todoMapper) {
        this.todoService = todoService;
        this.todoMapper = todoMapper;
    }

    @GetMapping
    public List<TodoResponse> getTodos(
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) String keyword
    ) {
        return todoMapper.toResponseList(todoService.getTodos(completed, keyword));
    }

    @GetMapping("/{id}")
    public TodoResponse getTodo(@PathVariable Long id) {
        return todoMapper.toResponse(todoService.getTodo(id));
    }

    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(@Valid @RequestBody CreateTodoRequest request) {
        Todo createdTodo = todoService.addTodo(request.getTitle());
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdTodo.getId())
                .toUri();

        return ResponseEntity.created(location).body(todoMapper.toResponse(createdTodo));
    }

    @PatchMapping("/{id}")
    public TodoResponse updateTodo(@PathVariable Long id, @RequestBody UpdateTodoRequest request) {
        return todoMapper.toResponse(todoService.updateTodo(id, request.getTitle(), request.getCompleted()));
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
}
