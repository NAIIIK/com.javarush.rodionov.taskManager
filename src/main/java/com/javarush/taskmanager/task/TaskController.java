package com.javarush.taskmanager.task;

import com.javarush.taskmanager.security.CurrentUserId;
import com.javarush.taskmanager.task.dto.CreateTaskRequest;
import com.javarush.taskmanager.task.dto.TaskResponse;
import com.javarush.taskmanager.task.dto.UpdateTaskStatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable UUID projectId,
            @CurrentUserId UUID userId,
            @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = taskService.createTask(projectId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasks(
            @PathVariable UUID projectId,
            @CurrentUserId UUID userId) {
        return ResponseEntity.ok(taskService.getTasksForProject(projectId, userId));
    }

    @PatchMapping("/api/tasks/{taskId}/assign-self")
    public ResponseEntity<TaskResponse> assignSelf(
            @PathVariable UUID taskId,
            @CurrentUserId UUID userId) {
        return ResponseEntity.ok(taskService.assignSelf(taskId, userId));
    }

    @PatchMapping("/api/tasks/{taskId}/status")
    public ResponseEntity<TaskResponse> updateStatus(
            @PathVariable UUID taskId,
            @CurrentUserId UUID userId,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        return ResponseEntity.ok(taskService.updateStatus(taskId, userId, request));
    }
}