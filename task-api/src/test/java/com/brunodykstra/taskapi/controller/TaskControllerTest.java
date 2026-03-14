package com.brunodykstra.taskapi.controller;

import com.brunodykstra.taskapi.dto.TaskDTO;
import com.brunodykstra.taskapi.exception.ResourceNotFoundException;
import com.brunodykstra.taskapi.model.TaskPriority;
import com.brunodykstra.taskapi.model.TaskStatus;
import com.brunodykstra.taskapi.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import(TaskControllerTest.TestSecurityConfig.class)
@DisplayName("TaskController — integration tests")
class TaskControllerTest {

    @Configuration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private TaskService taskService;

    private TaskDTO.Response sample;

    @BeforeEach
    void setUp() {
        sample = TaskDTO.Response.builder().id(1L).title("Implement login feature")
                .description("JWT + Spring Security").status(TaskStatus.PENDING)
                .priority(TaskPriority.HIGH).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    // ── POST /api/tasks ────────────────────────────────────────────────────

    @Nested @DisplayName("POST /api/tasks")
    class Create {

        @Test @WithMockUser @DisplayName("201 with valid request")
        void valid() throws Exception {
            when(taskService.create(any())).thenReturn(sample);
            mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        TaskDTO.Request.builder().title("Implement login feature").priority(TaskPriority.HIGH).build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Implement login feature"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"));
        }

        @Test @WithMockUser @DisplayName("400 when title is blank")
        void blankTitle() throws Exception {
            mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(TaskDTO.Request.builder().title("").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.title").exists());
        }

        @Test @WithMockUser @DisplayName("400 when title is too short (< 3 chars)")
        void tooShortTitle() throws Exception {
            mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(TaskDTO.Request.builder().title("AB").build())))
                .andExpect(status().isBadRequest());
        }

        @Test @WithMockUser @DisplayName("400 when title is null")
        void nullTitle() throws Exception {
            mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\": null}"))
                .andExpect(status().isBadRequest());
        }

        @Test @WithMockUser @DisplayName("400 when description exceeds 500 chars")
        void descriptionTooLong() throws Exception {
            mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(TaskDTO.Request.builder()
                        .title("Valid Title").description("x".repeat(501)).build())))
                .andExpect(status().isBadRequest());
        }
    }

    // ── GET /api/tasks ─────────────────────────────────────────────────────

    @Nested @DisplayName("GET /api/tasks")
    class ListTasks {

        @Test @WithMockUser @DisplayName("200 with paginated results")
        void paginated() throws Exception {
            when(taskService.findAll(any(), any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(new PageImpl<>(List.of(sample), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test @WithMockUser @DisplayName("passes status filter to service")
        void withStatusFilter() throws Exception {
            when(taskService.findAll(eq(TaskStatus.PENDING), any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(new PageImpl<>(List.of(sample)));

            mockMvc.perform(get("/api/tasks").param("status", "PENDING"))
                .andExpect(status().isOk());
            verify(taskService).findAll(eq(TaskStatus.PENDING), any(), any(), anyInt(), anyInt(), anyString(), anyString());
        }

        @Test @WithMockUser @DisplayName("passes priority filter to service")
        void withPriorityFilter() throws Exception {
            when(taskService.findAll(any(), eq(TaskPriority.HIGH), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(new PageImpl<>(List.of(sample)));

            mockMvc.perform(get("/api/tasks").param("priority", "HIGH"))
                .andExpect(status().isOk());
            verify(taskService).findAll(any(), eq(TaskPriority.HIGH), any(), anyInt(), anyInt(), anyString(), anyString());
        }

        @Test @WithMockUser @DisplayName("passes keyword search to service")
        void withKeyword() throws Exception {
            when(taskService.findAll(any(), any(), eq("login"), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(new PageImpl<>(List.of(sample)));

            mockMvc.perform(get("/api/tasks").param("keyword", "login"))
                .andExpect(status().isOk());
            verify(taskService).findAll(any(), any(), eq("login"), anyInt(), anyInt(), anyString(), anyString());
        }
    }

    // ── GET /api/tasks/{id} ────────────────────────────────────────────────

    @Nested @DisplayName("GET /api/tasks/{id}")
    class GetById {

        @Test @WithMockUser @DisplayName("200 when task exists")
        void found() throws Exception {
            when(taskService.findById(1L)).thenReturn(sample);
            mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
        }

        @Test @WithMockUser @DisplayName("404 when task not found")
        void notFound() throws Exception {
            when(taskService.findById(99L)).thenThrow(new ResourceNotFoundException("Task", 99L));
            mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ── PUT /api/tasks/{id} ────────────────────────────────────────────────

    @Nested @DisplayName("PUT /api/tasks/{id}")
    class UpdateTask {

        @Test @WithMockUser @DisplayName("200 with full update")
        void updated() throws Exception {
            when(taskService.update(eq(1L), any())).thenReturn(
                sample.toBuilder().title("Updated").status(TaskStatus.COMPLETED).build());

            mockMvc.perform(put("/api/tasks/1").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(TaskDTO.Request.builder()
                        .title("Updated").status(TaskStatus.COMPLETED).priority(TaskPriority.LOW).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }

        @Test @WithMockUser @DisplayName("404 when updating non-existent task")
        void notFound() throws Exception {
            when(taskService.update(eq(99L), any())).thenThrow(new ResourceNotFoundException("Task", 99L));
            mockMvc.perform(put("/api/tasks/99").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        TaskDTO.Request.builder().title("Valid").build())))
                .andExpect(status().isNotFound());
        }
    }

    // ── PATCH /api/tasks/{id}/status ───────────────────────────────────────

    @Nested @DisplayName("PATCH /api/tasks/{id}/status")
    class PatchStatus {

        @Test @WithMockUser @DisplayName("updates to COMPLETED")
        void toCompleted() throws Exception {
            when(taskService.patchStatus(1L, TaskStatus.COMPLETED))
                .thenReturn(sample.toBuilder().status(TaskStatus.COMPLETED).build());

            mockMvc.perform(patch("/api/tasks/1/status").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }

        @Test @WithMockUser @DisplayName("updates to IN_PROGRESS")
        void toInProgress() throws Exception {
            when(taskService.patchStatus(1L, TaskStatus.IN_PROGRESS))
                .thenReturn(sample.toBuilder().status(TaskStatus.IN_PROGRESS).build());

            mockMvc.perform(patch("/api/tasks/1/status").param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        }

        @Test @WithMockUser @DisplayName("updates to PENDING")
        void toPending() throws Exception {
            when(taskService.patchStatus(1L, TaskStatus.PENDING))
                .thenReturn(sample.toBuilder().status(TaskStatus.PENDING).build());

            mockMvc.perform(patch("/api/tasks/1/status").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
        }
    }

    // ── DELETE /api/tasks/{id} ─────────────────────────────────────────────

    @Nested @DisplayName("DELETE /api/tasks/{id}")
    class DeleteTask {

        @Test @WithMockUser @DisplayName("200 when deleted")
        void deleted() throws Exception {
            doNothing().when(taskService).delete(1L);
            mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test @WithMockUser @DisplayName("404 when deleting non-existent task")
        void notFound() throws Exception {
            doThrow(new ResourceNotFoundException("Task", 99L)).when(taskService).delete(99L);
            mockMvc.perform(delete("/api/tasks/99"))
                .andExpect(status().isNotFound());
        }
    }

    // ── GET /api/tasks/summary ─────────────────────────────────────────────

    @Nested @DisplayName("GET /api/tasks/summary")
    class Summary {

        @Test @WithMockUser @DisplayName("returns counts per status")
        void returnsCounts() throws Exception {
            when(taskService.getSummary()).thenReturn(
                Map.of("pending", 3L, "in_progress", 1L, "completed", 5L, "total", 9L));

            mockMvc.perform(get("/api/tasks/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pending").value(3))
                .andExpect(jsonPath("$.data.in_progress").value(1))
                .andExpect(jsonPath("$.data.completed").value(5))
                .andExpect(jsonPath("$.data.total").value(9));
        }
    }
}
