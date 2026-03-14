package com.brunodykstra.taskapi.service;

import com.brunodykstra.taskapi.dto.TaskDTO;
import com.brunodykstra.taskapi.exception.ResourceNotFoundException;
import com.brunodykstra.taskapi.model.*;
import com.brunodykstra.taskapi.repository.TaskRepository;
import com.brunodykstra.taskapi.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService — unit tests")
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private TaskService taskService;

    private User mockUser;
    private Task mockTask;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).username("brunodykstra")
                .email("brunodykstra@gmail.com").password("encoded").build();

        mockTask = Task.builder().id(1L).title("Implement login feature")
                .description("JWT + Spring Security").status(TaskStatus.PENDING)
                .priority(TaskPriority.HIGH).user(mockUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("brunodykstra", null, Collections.emptyList()));
    }

    // ── create ─────────────────────────────────────────────────────────────

    @Nested @DisplayName("create()")
    class Create {

        @Test @DisplayName("creates task with default PENDING status and MEDIUM priority when not provided")
        void defaultStatusAndPriority() {
            when(userRepository.findByUsername("brunodykstra")).thenReturn(Optional.of(mockUser));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
                Task t = inv.getArgument(0);
                return Task.builder().id(10L).title(t.getTitle()).status(t.getStatus())
                        .priority(t.getPriority()).user(mockUser)
                        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            });

            TaskDTO.Response res = taskService.create(TaskDTO.Request.builder().title("New Task").build());

            assertThat(res.getStatus()).isEqualTo(TaskStatus.PENDING);
            assertThat(res.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        }

        @Test @DisplayName("respects explicit status and priority from request")
        void explicitValues() {
            when(userRepository.findByUsername("brunodykstra")).thenReturn(Optional.of(mockUser));
            when(taskRepository.save(any())).thenAnswer(inv -> {
                Task t = inv.getArgument(0);
                return Task.builder().id(11L).title(t.getTitle()).status(t.getStatus())
                        .priority(t.getPriority()).user(mockUser)
                        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            });

            TaskDTO.Response res = taskService.create(TaskDTO.Request.builder()
                    .title("High Priority").status(TaskStatus.IN_PROGRESS).priority(TaskPriority.HIGH).build());

            assertThat(res.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(res.getPriority()).isEqualTo(TaskPriority.HIGH);
        }

        @Test @DisplayName("persists task via repository")
        void persistsTask() {
            when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
            when(taskRepository.save(any())).thenReturn(mockTask);
            taskService.create(TaskDTO.Request.builder().title("Any").build());
            verify(taskRepository, times(1)).save(any(Task.class));
        }

        @Test @DisplayName("all three statuses can be set on creation")
        void allStatuses() {
            for (TaskStatus status : TaskStatus.values()) {
                when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
                when(taskRepository.save(any())).thenAnswer(inv -> {
                    Task t = inv.getArgument(0);
                    return Task.builder().id(1L).title(t.getTitle()).status(t.getStatus())
                            .priority(TaskPriority.MEDIUM).user(mockUser)
                            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
                });
                TaskDTO.Response res = taskService.create(
                    TaskDTO.Request.builder().title("T").status(status).build());
                assertThat(res.getStatus()).isEqualTo(status);
            }
        }

        @Test @DisplayName("all three priorities can be set on creation")
        void allPriorities() {
            for (TaskPriority priority : TaskPriority.values()) {
                when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
                when(taskRepository.save(any())).thenAnswer(inv -> {
                    Task t = inv.getArgument(0);
                    return Task.builder().id(1L).title(t.getTitle()).status(TaskStatus.PENDING)
                            .priority(t.getPriority()).user(mockUser)
                            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
                });
                TaskDTO.Response res = taskService.create(
                    TaskDTO.Request.builder().title("T").priority(priority).build());
                assertThat(res.getPriority()).isEqualTo(priority);
            }
        }
    }

    // ── findAll ────────────────────────────────────────────────────────────

    @Nested @DisplayName("findAll()")
    class FindAll {

        @Test @DisplayName("uses keyword search when keyword is provided")
        void withKeyword() {
            when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
            when(taskRepository.searchByUserIdAndKeyword(eq(1L), eq("login"), any()))
                .thenReturn(new PageImpl<>(List.of(mockTask)));

            Page<TaskDTO.Response> page = taskService.findAll(null, null, "login", 0, 10, "createdAt", "desc");
            assertThat(page.getContent()).hasSize(1);
            verify(taskRepository).searchByUserIdAndKeyword(eq(1L), eq("login"), any());
        }

        @Test @DisplayName("filters by status only when only status is provided")
        void withStatusOnly() {
            when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
            when(taskRepository.findByUserIdAndStatus(eq(1L), eq(TaskStatus.PENDING), any()))
                .thenReturn(new PageImpl<>(List.of(mockTask)));

            taskService.findAll(TaskStatus.PENDING, null, null, 0, 10, "createdAt", "desc");
            verify(taskRepository).findByUserIdAndStatus(eq(1L), eq(TaskStatus.PENDING), any());
        }

        @Test @DisplayName("filters by priority only when only priority is provided")
        void withPriorityOnly() {
            when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
            when(taskRepository.findByUserIdAndPriority(eq(1L), eq(TaskPriority.HIGH), any()))
                .thenReturn(new PageImpl<>(List.of(mockTask)));

            taskService.findAll(null, TaskPriority.HIGH, null, 0, 10, "createdAt", "desc");
            verify(taskRepository).findByUserIdAndPriority(eq(1L), eq(TaskPriority.HIGH), any());
        }

        @Test @DisplayName("filters by status AND priority when both are provided")
        void withStatusAndPriority() {
            when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
            when(taskRepository.findByUserIdAndStatusAndPriority(eq(1L), eq(TaskStatus.PENDING), eq(TaskPriority.HIGH), any()))
                .thenReturn(new PageImpl<>(List.of(mockTask)));

            taskService.findAll(TaskStatus.PENDING, TaskPriority.HIGH, null, 0, 10, "createdAt", "desc");
            verify(taskRepository).findByUserIdAndStatusAndPriority(eq(1L), eq(TaskStatus.PENDING), eq(TaskPriority.HIGH), any());
        }

        @Test @DisplayName("lists all tasks with no filters")
        void noFilters() {
            when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
            when(taskRepository.findByUserId(eq(1L), any())).thenReturn(new PageImpl<>(List.of(mockTask)));

            taskService.findAll(null, null, null, 0, 10, "createdAt", "desc");
            verify(taskRepository).findByUserId(eq(1L), any());
        }

        @Test @DisplayName("respects ascending sort direction")
        void ascSort() {
            when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
            when(taskRepository.findByUserId(eq(1L), any())).thenReturn(Page.empty());
            taskService.findAll(null, null, null, 0, 10, "title", "asc");
            ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
            verify(taskRepository).findByUserId(eq(1L), cap.capture());
            assertThat(cap.getValue().getSort().getOrderFor("title").getDirection())
                .isEqualTo(Sort.Direction.ASC);
        }
    }

    // ── findById ───────────────────────────────────────────────────────────

    @Nested @DisplayName("findById()")
    class FindById {

        @Test @DisplayName("returns task when it belongs to the authenticated user")
        void found() {
            when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
            when(taskRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockTask));
            TaskDTO.Response res = taskService.findById(1L);
            assertThat(res.getId()).isEqualTo(1L);
            assertThat(res.getTitle()).isEqualTo("Implement login feature");
        }

        @Test @DisplayName("throws ResourceNotFoundException when task not found")
        void notFound() {
            when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
            when(taskRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> taskService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("99");
        }

        @Test @DisplayName("throws ResourceNotFoundException when task belongs to another user")
        void otherUserTask() {
            when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
            when(taskRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> taskService.findById(5L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── update ─────────────────────────────────────────────────────────────

    @Nested @DisplayName("update()")
    class Update {

        @Test @DisplayName("updates all mutable fields")
        void updatesAllFields() {
            when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
            when(taskRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockTask));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TaskDTO.Response res = taskService.update(1L, TaskDTO.Request.builder()
                .title("Updated").description("New desc").status(TaskStatus.COMPLETED).priority(TaskPriority.LOW).build());

            assertThat(res.getTitle()).isEqualTo("Updated");
            assertThat(res.getDescription()).isEqualTo("New desc");
            assertThat(res.getStatus()).isEqualTo(TaskStatus.COMPLETED);
            assertThat(res.getPriority()).isEqualTo(TaskPriority.LOW);
        }

        @Test @DisplayName("keeps existing status when request status is null")
        void keepStatusIfNull() {
            when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
            when(taskRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockTask));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TaskDTO.Response res = taskService.update(1L,
                TaskDTO.Request.builder().title("T").status(null).build());
            assertThat(res.getStatus()).isEqualTo(TaskStatus.PENDING);
        }

        @Test @DisplayName("throws when task not found on update")
        void notFound() {
            when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
            when(taskRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> taskService.update(99L,
                TaskDTO.Request.builder().title("T").build()))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── patchStatus ────────────────────────────────────────────────────────

    @Nested @DisplayName("patchStatus()")
    class PatchStatus {

        @Test @DisplayName("updates status to each possible value")
        void allStatuses() {
            for (TaskStatus status : TaskStatus.values()) {
                when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
                when(taskRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockTask));
                when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                assertThat(taskService.patchStatus(1L, status).getStatus()).isEqualTo(status);
            }
        }

        @Test @DisplayName("does not modify title or priority on patch")
        void onlyStatus() {
            when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
            when(taskRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockTask));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TaskDTO.Response res = taskService.patchStatus(1L, TaskStatus.COMPLETED);
            assertThat(res.getTitle()).isEqualTo("Implement login feature");
            assertThat(res.getPriority()).isEqualTo(TaskPriority.HIGH);
        }
    }

    // ── delete ─────────────────────────────────────────────────────────────

    @Nested @DisplayName("delete()")
    class Delete {

        @Test @DisplayName("deletes task successfully")
        void deletesTask() {
            when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
            when(taskRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(mockTask));
            assertThatCode(() -> taskService.delete(1L)).doesNotThrowAnyException();
            verify(taskRepository).delete(mockTask);
        }

        @Test @DisplayName("never calls delete when task not found")
        void noDeleteWhenNotFound() {
            when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
            when(taskRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> taskService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
            verify(taskRepository, never()).delete(any());
        }
    }

    // ── getSummary ─────────────────────────────────────────────────────────

    @Nested @DisplayName("getSummary()")
    class Summary {

        @Test @DisplayName("returns correct counts for all statuses")
        void correctCounts() {
            when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
            when(taskRepository.countByUserIdAndStatus(1L, TaskStatus.PENDING)).thenReturn(3L);
            when(taskRepository.countByUserIdAndStatus(1L, TaskStatus.IN_PROGRESS)).thenReturn(2L);
            when(taskRepository.countByUserIdAndStatus(1L, TaskStatus.COMPLETED)).thenReturn(5L);

            Map<String, Long> summary = taskService.getSummary();
            assertThat(summary.get("pending")).isEqualTo(3L);
            assertThat(summary.get("in_progress")).isEqualTo(2L);
            assertThat(summary.get("completed")).isEqualTo(5L);
            assertThat(summary.get("total")).isEqualTo(10L);
        }

        @Test @DisplayName("returns all zeros when user has no tasks")
        void allZeros() {
            when(userRepository.findByUsername(any())).thenReturn(Optional.of(mockUser));
            when(taskRepository.countByUserIdAndStatus(anyLong(), any())).thenReturn(0L);
            Map<String, Long> s = taskService.getSummary();
            assertThat(s.get("total")).isZero();
        }
    }
}
