package com.brunodykstra.taskapi.repository;

import com.brunodykstra.taskapi.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("TaskRepository — JPA tests")
class TaskRepositoryTest {

    @Autowired private TaskRepository taskRepository;
    @Autowired private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
            .username("brunodykstra").email("brunodykstra@gmail.com").password("encoded").build());
    }

    private Task save(String title, TaskStatus status, TaskPriority priority) {
        return taskRepository.save(Task.builder()
            .title(title).description("desc").status(status).priority(priority).user(user).build());
    }

    @Nested @DisplayName("findByUserId()")
    class FindByUser {
        @Test @DisplayName("returns only tasks belonging to the given user")
        void onlyUserTasks() {
            save("Task A", TaskStatus.PENDING, TaskPriority.LOW);
            save("Task B", TaskStatus.COMPLETED, TaskPriority.HIGH);
            User other = userRepository.save(
                User.builder().username("other").email("other@test.com").password("pw").build());
            taskRepository.save(Task.builder().title("Other").status(TaskStatus.PENDING)
                .priority(TaskPriority.LOW).user(other).build());

            Page<Task> page = taskRepository.findByUserId(user.getId(), PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(2)
                .allMatch(t -> t.getUser().getId().equals(user.getId()));
        }
    }

    @Nested @DisplayName("findByUserIdAndStatus()")
    class FindByStatus {
        @Test @DisplayName("returns only tasks with matching status")
        void matchingStatus() {
            save("Pending 1",   TaskStatus.PENDING,     TaskPriority.LOW);
            save("Pending 2",   TaskStatus.PENDING,     TaskPriority.HIGH);
            save("Completed 1", TaskStatus.COMPLETED,   TaskPriority.MEDIUM);

            Page<Task> page = taskRepository.findByUserIdAndStatus(
                user.getId(), TaskStatus.PENDING, PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(2)
                .allMatch(t -> t.getStatus() == TaskStatus.PENDING);
        }

        @Test @DisplayName("returns empty when no tasks match status")
        void noMatch() {
            save("Task", TaskStatus.PENDING, TaskPriority.LOW);
            Page<Task> page = taskRepository.findByUserIdAndStatus(
                user.getId(), TaskStatus.COMPLETED, PageRequest.of(0, 10));
            assertThat(page.getContent()).isEmpty();
        }
    }

    @Nested @DisplayName("findByUserIdAndPriority()")
    class FindByPriority {
        @Test @DisplayName("returns only tasks with matching priority")
        void matchingPriority() {
            save("High 1", TaskStatus.PENDING,   TaskPriority.HIGH);
            save("High 2", TaskStatus.COMPLETED, TaskPriority.HIGH);
            save("Low 1",  TaskStatus.PENDING,   TaskPriority.LOW);

            Page<Task> page = taskRepository.findByUserIdAndPriority(
                user.getId(), TaskPriority.HIGH, PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(2)
                .allMatch(t -> t.getPriority() == TaskPriority.HIGH);
        }
    }

    @Nested @DisplayName("findByUserIdAndStatusAndPriority()")
    class FindByStatusAndPriority {
        @Test @DisplayName("returns tasks matching both status and priority")
        void exactMatch() {
            save("Match",     TaskStatus.PENDING, TaskPriority.HIGH);
            save("WrongPri",  TaskStatus.PENDING, TaskPriority.LOW);
            save("WrongStat", TaskStatus.COMPLETED, TaskPriority.HIGH);

            Page<Task> page = taskRepository.findByUserIdAndStatusAndPriority(
                user.getId(), TaskStatus.PENDING, TaskPriority.HIGH, PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1)
                .allMatch(t -> t.getStatus() == TaskStatus.PENDING && t.getPriority() == TaskPriority.HIGH);
        }
    }

    @Nested @DisplayName("searchByUserIdAndKeyword()")
    class KeywordSearch {
        @Test @DisplayName("finds task by title keyword (case-insensitive)")
        void byTitle() {
            save("Implement JWT login", TaskStatus.PENDING, TaskPriority.HIGH);
            save("Write unit tests",   TaskStatus.PENDING, TaskPriority.LOW);

            Page<Task> page = taskRepository.searchByUserIdAndKeyword(
                user.getId(), "jwt", PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getTitle()).containsIgnoringCase("jwt");
        }

        @Test @DisplayName("finds task by description keyword")
        void byDescription() {
            taskRepository.save(Task.builder().title("Auth feature")
                .description("Implement OAuth2 flow").status(TaskStatus.PENDING)
                .priority(TaskPriority.MEDIUM).user(user).build());

            Page<Task> page = taskRepository.searchByUserIdAndKeyword(
                user.getId(), "oauth2", PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
        }

        @Test @DisplayName("returns empty for non-matching keyword")
        void noMatch() {
            save("Task A", TaskStatus.PENDING, TaskPriority.LOW);
            Page<Task> page = taskRepository.searchByUserIdAndKeyword(
                user.getId(), "xyz_not_found", PageRequest.of(0, 10));
            assertThat(page.getContent()).isEmpty();
        }
    }

    @Nested @DisplayName("countByUserIdAndStatus()")
    class Count {
        @Test @DisplayName("returns accurate count per status")
        void accurateCount() {
            save("P1", TaskStatus.PENDING,   TaskPriority.LOW);
            save("P2", TaskStatus.PENDING,   TaskPriority.HIGH);
            save("C1", TaskStatus.COMPLETED, TaskPriority.MEDIUM);

            assertThat(taskRepository.countByUserIdAndStatus(user.getId(), TaskStatus.PENDING)).isEqualTo(2);
            assertThat(taskRepository.countByUserIdAndStatus(user.getId(), TaskStatus.COMPLETED)).isEqualTo(1);
            assertThat(taskRepository.countByUserIdAndStatus(user.getId(), TaskStatus.IN_PROGRESS)).isZero();
        }
    }

    @Nested @DisplayName("findByIdAndUserId()")
    class FindByIdAndUser {
        @Test @DisplayName("returns task when id and userId match")
        void found() {
            Task t = save("My Task", TaskStatus.PENDING, TaskPriority.LOW);
            assertThat(taskRepository.findByIdAndUserId(t.getId(), user.getId())).isPresent();
        }

        @Test @DisplayName("returns empty when task belongs to another user")
        void notFound() {
            Task t = save("My Task", TaskStatus.PENDING, TaskPriority.LOW);
            assertThat(taskRepository.findByIdAndUserId(t.getId(), 9999L)).isEmpty();
        }
    }
}
