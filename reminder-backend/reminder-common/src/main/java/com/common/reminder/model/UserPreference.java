package com.common.reminder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.OffsetDateTime;

/**
 * 用户偏好设置实体
 */
@Entity
@Table(name = "user_preference")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(nullable = false, unique = true)
    private Long userId;

    /**
     * 默认通知类型
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReminderType defaultReminderType = ReminderType.EMAIL;

    /**
     * 是否启用邮件通知
     */
    @Column(nullable = false)
    private Boolean emailNotificationEnabled = true;

    /**
     * 是否启用短信通知
     */
    @Column(nullable = false)
    private Boolean smsNotificationEnabled = false;

    /**
     * 是否启用微信小程序通知
     */
    @Column(nullable = false)
    private Boolean wechatNotificationEnabled = false;

    /**
     * 默认提前提醒时间（分钟）
     */
    @Column(nullable = false)
    private Integer defaultAdvanceMinutes = 15;

    /**
     * 是否启用声音提醒
     */
    @Column(nullable = false)
    private Boolean soundEnabled = true;

    /**
     * 是否启用震动提醒
     */
    @Column(nullable = false)
    private Boolean vibrationEnabled = true;

    /**
     * 时区设置
     */
    @Column(length = 50)
    private String timezone = "Asia/Shanghai";

    /**
     * 语言设置
     */
    @Column(length = 10)
    private String language = "zh-CN";

    /**
     * 主题设置
     */
    @Column(length = 20)
    private String theme = "light";

    /**
     * 每日汇总邮件时间（24小时制，如 "08:00"）
     */
    @Column(length = 5)
    private String dailySummaryTime = "08:00";

    /**
     * 是否启用每日汇总邮件
     */
    @Column(nullable = false)
    private Boolean dailySummaryEnabled = false;

    /**
     * 周末是否接收提醒
     */
    @Column(nullable = false)
    private Boolean weekendNotificationEnabled = true;

    /**
     * 免打扰开始时间（24小时制，如 "22:00"）
     */
    @Column(length = 5)
    private String quietHoursStart = "22:00";

    /**
     * 免打扰结束时间（24小时制，如 "07:00"）
     */
    @Column(length = 5)
    private String quietHoursEnd = "07:00";

    /**
     * 是否启用免打扰模式
     */
    @Column(nullable = false)
    private Boolean quietHoursEnabled = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;

    // 与用户表建立关系（可选）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    private AppUser user;
} 