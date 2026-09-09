package com.javarush.taskmanager.task;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findAllByProjectId(UUID projectId);

    List<Task> findAllByAssigneeId(UUID assigneeId);

    List<Task> findAllByProjectIdAndStatus(UUID projectId, TaskStatus status);
}