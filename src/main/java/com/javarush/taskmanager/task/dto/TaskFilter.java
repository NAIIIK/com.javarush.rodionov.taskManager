package com.javarush.taskmanager.task.dto;

import com.javarush.taskmanager.task.TaskStatus;

import java.time.LocalDate;
import java.util.List;

public record TaskFilter(
        List<TaskStatus> statuses,
        LocalDate dueDateFrom,
        LocalDate dueDateTo,
        Boolean overdue
) {}