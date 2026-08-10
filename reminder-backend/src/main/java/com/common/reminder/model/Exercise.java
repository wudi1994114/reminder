package com.common.reminder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "exercise")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * 目标肌群（可选），如：胸、背、腿、肩、手臂、核心
     */
    private String muscleGroup;

    /**
     * 器械（可选），如：哑铃、杠铃、自重、弹力带
     */
    private String equipment;

    /** 默认次数（无显式指定时沿用） */
    @Column(nullable = false)
    private Integer defaultReps = 11;

    /** 默认组数 */
    @Column(nullable = false)
    private Integer defaultSets = 3;

    /** 默认组间休息（秒） */
    @Column(nullable = false)
    private Integer defaultRestSec = 30;

    /** 训练音频地址（可为 cloud:// 或 https://） */
    @Column(columnDefinition = "TEXT")
    private String audioUrl;

    /** 是否公开（公共动作） */
    @Column(nullable = false)
    private Boolean isPublic = true;

    /** 拥有者用户ID（非公开或个人动作使用） */
    private Long ownerUserId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;
}


