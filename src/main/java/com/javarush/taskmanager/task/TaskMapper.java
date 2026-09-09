package com.javarush.taskmanager.task;

import com.javarush.taskmanager.task.dto.TaskResponse;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getAssignee() != null ? task.getAssignee().getId() : null,
                task.getDueDate(),
                task.getCreatedAt()
        );
    }
}