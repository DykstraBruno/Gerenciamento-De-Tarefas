package com.brunodykstra.taskapi.exception;

import com.brunodykstra.taskapi.controller.TaskController;
import com.brunodykstra.taskapi.service.TaskService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandlerTest.TestSecurityConfig.class)
@DisplayName("GlobalExceptionHandler — tests")
class GlobalExceptionHandlerTest {

    @Configuration
    static class TestSecurityConfig {
        @Bean SecurityFilterChain chain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired private MockMvc mockMvc;
    @MockBean  private TaskService taskService;

    @Test @DisplayName("404 response has success=false and error message")
    void notFoundResponse() throws Exception {
        when(taskService.findById(999L)).thenThrow(new ResourceNotFoundException("Task", 999L));
        mockMvc.perform(get("/api/tasks/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test @DisplayName("409 response for BusinessException")
    void businessExceptionResponse() throws Exception {
        when(taskService.create(any())).thenThrow(new BusinessException("Duplicate resource"));
        mockMvc.perform(post("/api/tasks")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Valid Title\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test @DisplayName("400 response for validation failure contains field errors")
    void validationFailureResponse() throws Exception {
        mockMvc.perform(post("/api/tasks")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"title\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.data").isMap());
    }
}
