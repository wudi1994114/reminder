package com.core.reminder.dto;

import com.common.reminder.model.ReminderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 用于前端展示和接收的复杂提醒DTO对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplexReminderDTO {

    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String title;
    private String description;
    private String cronExpression;
    private ReminderType reminderType;
    private LocalDate validFrom;      // 提醒开始生效日期
    private LocalDate validUntil;     // 提醒失效日期
    private Integer maxExecutions;    // 最大执行次数限制
    private Integer lastGeneratedYm; // 年月格式：YYYYMM
    private String idempotencyKey;   // 幂等键，用于防止重复创建
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
} 