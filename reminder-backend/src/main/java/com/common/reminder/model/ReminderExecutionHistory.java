package com.common.reminder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "reminder_execution_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReminderExecutionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 使用 @Column(insertable=false, updatable=false) 配合 columnDefinition
    // 是因为 executed_at 有 DEFAULT CURRENT_TIMESTAMP，让数据库处理默认值
    @Column(nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime executedAt; // 任务实际执行时间戳

    @Column(nullable = false, length = 50)
    private String triggeringReminderType; // 触发源类型 ('SIMPLE', 'COMPLEX')

    @Column(nullable = false)
    private Long triggeringReminderId; // 触发源提醒的 ID

    @Column(nullable = false)
    private Long fromUserId; // 创建原始提醒/模板的用户 ID (冗余)

    @Column(nullable = false)
    private Long toUserId; // 接收提醒的目标用户 ID (冗余)

    @Column(columnDefinition = "TEXT")
    private String title; // 执行时的提醒标题 (冗余)

    @Column(columnDefinition = "TEXT")
    private String description; // 执行时的提醒描述 (冗余)

    @Column(nullable = false, length = 50)
    private String actualReminderMethod; // 实际使用的提醒方式 (例如: 'EMAIL', 'SMS') (冗余)

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime scheduledEventTime; // SIMPLE 触发源的原定计划时间 (冗余)

    @Column(nullable = false, length = 50)
    private String status; // 执行结果状态 ('SUCCESS', 'FAILURE')

    @Column(columnDefinition = "TEXT")
    private String details; // 执行详情或错误信息

    // 这个实体通常是只写的 (记录日志)，所以不需要 @CreationTimestamp/@UpdateTimestamp
} 