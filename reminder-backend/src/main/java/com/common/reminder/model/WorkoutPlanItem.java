package com.common.reminder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "workout_plan_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属计划ID（不使用JPA关系，避免耦合，保持与既有风格一致） */
    @Column(nullable = false)
    private Long planId;

    /** 执行顺序（从1开始） */
    @Column(nullable = false)
    private Integer orderIndex;

    /** 动作ID */
    @Column(nullable = false)
    private Long exerciseId;

    /** 次数、组数、组间休息（秒） */
    @Column(nullable = false)
    private Integer reps = 11;

    @Column(nullable = false)
    private Integer sets = 3;

    @Column(nullable = false)
    private Integer restSec = 30;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;
}


