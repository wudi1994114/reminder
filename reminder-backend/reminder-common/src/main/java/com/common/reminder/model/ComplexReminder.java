package com.common.reminder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "complex_reminder")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplexReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fromUserId; // 创建提醒的用户 ID

    @Column(nullable = false)
    private Long toUserId;   // 接收提醒的用户 ID

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;    // 提醒标题模板

    @Column(columnDefinition = "TEXT")
    private String description; // 提醒描述模板

    @Column(nullable = false, length = 255)
    private String cronExpression; // CRON 表达式

    @Enumerated(EnumType.STRING) // 将枚举映射为字符串存储
    @Column(nullable = false, length = 50)
    private ReminderType reminderType; // 提醒方式 (EMAIL, SMS)

    @Column
    private LocalDate validFrom;  // 提醒开始生效日期

    @Column
    private LocalDate validUntil; // 提醒失效日期

    @Column
    private Integer maxExecutions; // 最大执行次数限制

    @Column(name = "last_generated_ym")
    private Integer lastGeneratedYm; // 最后生成简单任务的年月(格式YYYYMM，如202405表示2024年5月)

    @CreationTimestamp
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;

    // 注意：这里没有直接映射到 AppUser 实体的 @ManyToOne 关系
    // 这是因为 schema.sql 中没有外键约束，保持实体与 schema 一致
    // 如果需要关联查询，可以在 Service 层处理或使用非约束的 @ManyToOne (不推荐)
} 