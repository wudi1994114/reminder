package com.core.reminder.dto;

import com.common.reminder.model.ReminderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 用户偏好设置DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceDto {

    private Long id;

    private Long userId;

    /**
     * 默认通知类型
     */
    @NotNull(message = "默认通知类型不能为空")
    private ReminderType defaultReminderType;

    /**
     * 是否启用邮件通知
     */
    @NotNull(message = "邮件通知设置不能为空")
    private Boolean emailNotificationEnabled;

    /**
     * 是否启用短信通知
     */
    @NotNull(message = "短信通知设置不能为空")
    private Boolean smsNotificationEnabled;

    /**
     * 是否启用微信小程序通知
     */
    @NotNull(message = "微信通知设置不能为空")
    private Boolean wechatNotificationEnabled;

    /**
     * 默认提前提醒时间（分钟）
     */
    @NotNull(message = "默认提前提醒时间不能为空")
    @Min(value = 0, message = "提前提醒时间不能小于0分钟")
    @Max(value = 1440, message = "提前提醒时间不能超过24小时")
    private Integer defaultAdvanceMinutes;

    /**
     * 是否启用声音提醒
     */
    @NotNull(message = "声音提醒设置不能为空")
    private Boolean soundEnabled;

    /**
     * 是否启用震动提醒
     */
    @NotNull(message = "震动提醒设置不能为空")
    private Boolean vibrationEnabled;

    /**
     * 时区设置
     */
    private String timezone;

    /**
     * 语言设置
     */
    @Pattern(regexp = "^(zh-CN|en-US)$", message = "语言设置只支持zh-CN和en-US")
    private String language;

    /**
     * 主题设置
     */
    @Pattern(regexp = "^(light|dark)$", message = "主题设置只支持light和dark")
    private String theme;

    /**
     * 每日汇总邮件时间
     */
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "时间格式不正确，应为HH:mm")
    private String dailySummaryTime;

    /**
     * 是否启用每日汇总邮件
     */
    @NotNull(message = "每日汇总邮件设置不能为空")
    private Boolean dailySummaryEnabled;

    /**
     * 周末是否接收提醒
     */
    @NotNull(message = "周末提醒设置不能为空")
    private Boolean weekendNotificationEnabled;

    /**
     * 免打扰开始时间
     */
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "时间格式不正确，应为HH:mm")
    private String quietHoursStart;

    /**
     * 免打扰结束时间
     */
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "时间格式不正确，应为HH:mm")
    private String quietHoursEnd;

    /**
     * 是否启用免打扰模式
     */
    @NotNull(message = "免打扰模式设置不能为空")
    private Boolean quietHoursEnabled;
} 