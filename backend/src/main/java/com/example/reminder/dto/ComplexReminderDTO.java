package com.example.reminder.dto;

import com.example.reminder.model.ReminderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Integer lastGeneratedYm; // 年月格式：YYYYMM
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
} 