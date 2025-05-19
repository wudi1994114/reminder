package com.common.reminder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "simple_reminder")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long fromUserId; // 创建提醒的用户 ID

    @Column(nullable = false)
    private Long toUserId;   // 接收提醒的用户 ID

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;    // 提醒标题

    @Column(columnDefinition = "TEXT")
    private String description; // 提醒描述

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime eventTime; // 提醒触发的精确时间点

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReminderType reminderType; // 提醒方式 (EMAIL, SMS)

    // 关联的复杂提醒模板 ID (可以为 null)
    @Column(name = "originating_complex_reminder_id") // 明确列名以匹配 schema
    private Long originatingComplexReminderId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;

    // 同样，没有直接映射到 AppUser 或 ComplexReminder 的关系，保持与 schema 一致
} 