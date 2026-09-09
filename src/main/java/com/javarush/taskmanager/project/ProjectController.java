package com.javarush.taskmanager.project;

import com.javarush.taskmanager.project.dto.CreateProjectRequest;
import com.javarush.taskmanager.project.dto.ProjectResponse;
import com.javarush.taskmanager.security.CurrentUserId;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @CurrentUserId UUID userId,
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.createProject(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getMyProjects(@CurrentUserId UUID userId) {
        return ResponseEntity.ok(projectService.getProjectsForUser(userId));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable UUID projectId,
            @CurrentUserId UUID userId) {
        return ResponseEntity.ok(projectService.getProject(projectId, userId));
    }
}