package com.core.reminder.repository;

import com.common.reminder.model.WorkoutPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkoutPlanItemRepository extends JpaRepository<WorkoutPlanItem, Long> {

    List<WorkoutPlanItem> findByPlanIdOrderByOrderIndexAsc(Long planId);

    void deleteByPlanId(Long planId);
}


