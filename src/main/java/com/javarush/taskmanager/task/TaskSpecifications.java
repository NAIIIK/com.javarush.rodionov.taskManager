package com.javarush.taskmanager.task;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class TaskSpecifications {

    private static final String STATUS_FIELD = "status";
    private static final String DUE_DATE_FIELD = "dueDate";

    private TaskSpecifications() {
    }

    public static Specification<Task> hasProjectId(UUID projectId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Task> hasStatusIn(List<TaskStatus> statuses) {
        return (root, query, criteriaBuilder) -> root.get(STATUS_FIELD).in(statuses);
    }

    public static Specification<Task> dueDateFrom(LocalDate from) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get(DUE_DATE_FIELD), from);
    }

    public static Specification<Task> dueDateTo(LocalDate to) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get(DUE_DATE_FIELD), to);
    }

    public static Specification<Task> isOverdue() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.lessThan(root.get(DUE_DATE_FIELD), LocalDate.now()),
                criteriaBuilder.notEqual(root.get(STATUS_FIELD), TaskStatus.DONE)
        );
    }
}