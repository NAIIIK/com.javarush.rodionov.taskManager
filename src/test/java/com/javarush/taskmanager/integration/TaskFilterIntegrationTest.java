package com.javarush.taskmanager.integration;

import com.javarush.taskmanager.task.TaskPriority;
import com.javarush.taskmanager.task.TaskStatus;
import com.javarush.taskmanager.task.dto.CreateTaskRequest;
import com.javarush.taskmanager.task.dto.UpdateTaskStatusRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskFilterIntegrationTest extends AbstractProjectIntegrationTest {

    private static final String PROJECT_NAME = "Task Filter Test Project";

    private String createTaskWithDueDate(String token, String projectId, String title, LocalDate dueDate) throws Exception {
        CreateTaskRequest request = new CreateTaskRequest(title, null, TaskPriority.MEDIUM, null, dueDate);

        var result = mockMvc.perform(post(projectTasksPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private void setStatus(String token, String taskId, TaskStatus status) throws Exception {
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(status);

        mockMvc.perform(patch(taskStatusPath(taskId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void getTasks_withoutFilters_returnsAllTasks() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String projectId = createProject(ownerToken, PROJECT_NAME, null);
        createTask(ownerToken, projectId, "Task 1");
        createTask(ownerToken, projectId, "Task 2");

        mockMvc.perform(get(projectTasksPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getTasks_filteredByStatus_returnsOnlyMatchingTasks() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String projectId = createProject(ownerToken, PROJECT_NAME, null);
        String doneTaskId = createTask(ownerToken, projectId, "Done task");
        createTask(ownerToken, projectId, "Todo task");
        setStatus(ownerToken, doneTaskId, TaskStatus.DONE);

        mockMvc.perform(get(projectTasksPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(doneTaskId));
    }

    @Test
    void getTasks_filteredByMultipleStatuses_returnsUnionOfMatches() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String projectId = createProject(ownerToken, PROJECT_NAME, null);
        String doneTaskId = createTask(ownerToken, projectId, "Done task");
        String inProgressTaskId = createTask(ownerToken, projectId, "In progress task");
        createTask(ownerToken, projectId, "Todo task");
        setStatus(ownerToken, doneTaskId, TaskStatus.DONE);
        setStatus(ownerToken, inProgressTaskId, TaskStatus.IN_PROGRESS);

        mockMvc.perform(get(projectTasksPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("status", "DONE,IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getTasks_filteredByDueDateRange_returnsOnlyTasksInRange() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String projectId = createProject(ownerToken, PROJECT_NAME, null);
        LocalDate today = LocalDate.now();
        String inRangeId = createTaskWithDueDate(ownerToken, projectId, "In range", today.plusDays(2));
        createTaskWithDueDate(ownerToken, projectId, "Too early", today.minusDays(5));
        createTaskWithDueDate(ownerToken, projectId, "Too late", today.plusDays(10));

        mockMvc.perform(get(projectTasksPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("dueDateFrom", today.toString())
                        .param("dueDateTo", today.plusDays(5).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(inRangeId));
    }

    @Test
    void getTasks_filteredByOverdue_excludesDoneAndFutureTasks() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String projectId = createProject(ownerToken, PROJECT_NAME, null);
        LocalDate today = LocalDate.now();

        String overdueId = createTaskWithDueDate(ownerToken, projectId, "Overdue", today.minusDays(3));
        String overdueButDoneId = createTaskWithDueDate(ownerToken, projectId, "Overdue but done", today.minusDays(3));
        createTaskWithDueDate(ownerToken, projectId, "Future", today.plusDays(3));
        setStatus(ownerToken, overdueButDoneId, TaskStatus.DONE);

        mockMvc.perform(get(projectTasksPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("overdue", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(overdueId));
    }

    @Test
    void getTasks_combinedStatusAndDueDateFilters_appliesAndLogic() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String projectId = createProject(ownerToken, PROJECT_NAME, null);
        LocalDate today = LocalDate.now();

        String matchingId = createTaskWithDueDate(ownerToken, projectId, "Matches both", today.plusDays(2));
        setStatus(ownerToken, matchingId, TaskStatus.IN_PROGRESS);

        String wrongStatusId = createTaskWithDueDate(ownerToken, projectId, "Wrong status", today.plusDays(2));
        // stays TO_DO

        mockMvc.perform(get(projectTasksPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("status", "IN_PROGRESS")
                        .param("dueDateFrom", today.toString())
                        .param("dueDateTo", today.plusDays(5).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(matchingId));
    }
}