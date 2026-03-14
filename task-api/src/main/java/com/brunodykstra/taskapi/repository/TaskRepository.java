package com.brunodykstra.taskapi.repository;

import com.brunodykstra.taskapi.model.Task;
import com.brunodykstra.taskapi.model.TaskPriority;
import com.brunodykstra.taskapi.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByUserId(Long userId, Pageable pageable);

    Page<Task> findByUserIdAndStatus(Long userId, TaskStatus status, Pageable pageable);

    Page<Task> findByUserIdAndPriority(Long userId, TaskPriority priority, Pageable pageable);

    Page<Task> findByUserIdAndStatusAndPriority(Long userId, TaskStatus status, TaskPriority priority, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Task> searchByUserIdAndKeyword(@Param("userId") Long userId,
                                        @Param("keyword") String keyword,
                                        Pageable pageable);

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndStatus(Long userId, TaskStatus status);
}
