package com.example.reminder.dto;

import com.example.reminder.model.ReminderType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 用于前端展示和接收的简单提醒DTO对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimpleReminderDTO {

    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String title;
    private String description;
    private OffsetDateTime eventTime;
    private ReminderType reminderType;
    private Long originatingComplexReminderId; // 来源复杂提醒ID，可能为null
    @JsonIgnore
    private OffsetDateTime createdAt;
    @JsonIgnore
    private OffsetDateTime updatedAt;
} 