package com.brunodykstra.taskapi.controller;

import com.brunodykstra.taskapi.dto.ApiResponse;
import com.brunodykstra.taskapi.dto.TaskDTO;
import com.brunodykstra.taskapi.model.TaskPriority;
import com.brunodykstra.taskapi.model.TaskStatus;
import com.brunodykstra.taskapi.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Create a new task")
    public ResponseEntity<ApiResponse<TaskDTO.Response>> create(
            @Valid @RequestBody TaskDTO.Request request) {
        TaskDTO.Response task = taskService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Task created successfully", task));
    }

    @GetMapping
    @Operation(summary = "List tasks with optional filters, search and pagination")
    public ResponseEntity<ApiResponse<Page<TaskDTO.Response>>> findAll(
            @Parameter(description = "Filter by status: PENDING, IN_PROGRESS, COMPLETED")
            @RequestParam(required = false) TaskStatus status,
            @Parameter(description = "Filter by priority: LOW, MEDIUM, HIGH")
            @RequestParam(required = false) TaskPriority priority,
            @Parameter(description = "Search keyword in title or description")
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Page<TaskDTO.Response> tasks = taskService.findAll(status, priority, keyword, page, size, sortBy, direction);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a task by ID")
    public ResponseEntity<ApiResponse<TaskDTO.Response>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(taskService.findById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Fully update a task")
    public ResponseEntity<ApiResponse<TaskDTO.Response>> update(
            @PathVariable Long id,
            @Valid @RequestBody TaskDTO.Request request) {
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", taskService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update only the status of a task")
    public ResponseEntity<ApiResponse<TaskDTO.Response>> patchStatus(
            @PathVariable Long id,
            @RequestParam TaskStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", taskService.patchStatus(id, status)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully", null));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get task count summary by status")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(taskService.getSummary()));
    }
}
