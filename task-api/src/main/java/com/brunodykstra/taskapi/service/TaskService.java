package com.brunodykstra.taskapi.service;

import com.brunodykstra.taskapi.dto.TaskDTO;
import com.brunodykstra.taskapi.exception.ResourceNotFoundException;
import com.brunodykstra.taskapi.model.*;
import com.brunodykstra.taskapi.repository.TaskRepository;
import com.brunodykstra.taskapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional
    public TaskDTO.Response create(TaskDTO.Request request) {
        User user = getAuthenticatedUser();

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.PENDING)
                .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM)
                .user(user)
                .build();

        Task saved = taskRepository.save(task);
        log.info("Task created [id={}] by user [{}]", saved.getId(), user.getUsername());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<TaskDTO.Response> findAll(TaskStatus status, TaskPriority priority,
                                          String keyword, int page, int size, String sortBy, String direction) {
        User user = getAuthenticatedUser();
        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Task> tasks;

        if (StringUtils.hasText(keyword)) {
            tasks = taskRepository.searchByUserIdAndKeyword(user.getId(), keyword, pageable);
        } else if (status != null && priority != null) {
            tasks = taskRepository.findByUserIdAndStatusAndPriority(user.getId(), status, priority, pageable);
        } else if (status != null) {
            tasks = taskRepository.findByUserIdAndStatus(user.getId(), status, pageable);
        } else if (priority != null) {
            tasks = taskRepository.findByUserIdAndPriority(user.getId(), priority, pageable);
        } else {
            tasks = taskRepository.findByUserId(user.getId(), pageable);
        }

        return tasks.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TaskDTO.Response findById(Long id) {
        User user = getAuthenticatedUser();
        Task task = taskRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        return toResponse(task);
    }

    @Transactional
    public TaskDTO.Response update(Long id, TaskDTO.Request request) {
        User user = getAuthenticatedUser();
        Task task = taskRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        if (request.getPriority() != null) task.setPriority(request.getPriority());

        Task updated = taskRepository.save(task);
        log.info("Task updated [id={}] by user [{}]", id, user.getUsername());
        return toResponse(updated);
    }

    @Transactional
    public TaskDTO.Response patchStatus(Long id, TaskStatus status) {
        User user = getAuthenticatedUser();
        Task task = taskRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        task.setStatus(status);
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        User user = getAuthenticatedUser();
        Task task = taskRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        taskRepository.delete(task);
        log.info("Task deleted [id={}] by user [{}]", id, user.getUsername());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getSummary() {
        User user = getAuthenticatedUser();
        long pending   = taskRepository.countByUserIdAndStatus(user.getId(), TaskStatus.PENDING);
        long inProgress = taskRepository.countByUserIdAndStatus(user.getId(), TaskStatus.IN_PROGRESS);
        long completed  = taskRepository.countByUserIdAndStatus(user.getId(), TaskStatus.COMPLETED);
        return Map.of(
                "pending", pending,
                "in_progress", inProgress,
                "completed", completed,
                "total", pending + inProgress + completed
        );
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private TaskDTO.Response toResponse(Task task) {
        return TaskDTO.Response.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
