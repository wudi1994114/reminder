package com.core.reminder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 提醒执行历史记录DTO
 * 用于前端展示和接收的执行历史数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderExecutionHistoryDTO {

    /**
     * 历史记录ID
     */
    private Long id;

    /**
     * 任务实际执行时间戳
     */
    private OffsetDateTime executedAt;

    /**
     * 触发源类型 ('SIMPLE', 'COMPLEX')
     */
    private String triggeringReminderType;

    /**
     * 触发源提醒的 ID
     */
    private Long triggeringReminderId;

    /**
     * 创建原始提醒/模板的用户 ID
     */
    private Long fromUserId;

    /**
     * 接收提醒的目标用户 ID
     */
    private Long toUserId;

    /**
     * 执行时的提醒标题
     */
    private String title;

    /**
     * 执行时的提醒描述
     */
    private String description;

    /**
     * 实际使用的提醒方式 (例如: 'EMAIL', 'SMS')
     */
    private String actualReminderMethod;

    /**
     * 对于 SIMPLE 触发源，记录原定计划的事件时间
     */
    private OffsetDateTime scheduledEventTime;

    /**
     * 执行结果状态 ('SUCCESS', 'FAILURE')
     */
    private String status;

    /**
     * 执行详情或错误信息
     */
    private String details;
}
