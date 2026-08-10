package com.core.reminder.repository;

import com.common.reminder.model.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {

    List<WorkoutPlan> findByOwnerUserId(Long ownerUserId);

    @Query("SELECT w FROM WorkoutPlan w WHERE (w.isPublic = true OR w.ownerUserId = :userId) " +
           "AND (:keyword IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY w.createdAt DESC")
    List<WorkoutPlan> searchAccessible(@Param("userId") Long userId,
                                       @Param("keyword") String keyword);
}


