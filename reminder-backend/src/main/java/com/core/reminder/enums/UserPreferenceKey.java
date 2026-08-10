package com.core.reminder.enums;

import lombok.Getter;

/**
 * Stable keys, defaults and descriptions for user preferences.
 */
@Getter
public enum UserPreferenceKey {
    DEFAULT_REMINDER_TYPE("defaultReminderType", "EMAIL", "默认提醒方式"),
    EMAIL_NOTIFICATION_ENABLED("emailNotificationEnabled", "true", "邮件通知开关"),
    SMS_NOTIFICATION_ENABLED("smsNotificationEnabled", "false", "短信通知开关"),
    WECHAT_NOTIFICATION_ENABLED("wechatNotificationEnabled", "false", "微信通知开关"),
    DEFAULT_ADVANCE_MINUTES("defaultAdvanceMinutes", "15", "默认提前提醒分钟数"),
    SOUND_ENABLED("soundEnabled", "true", "声音提醒开关"),
    VIBRATION_ENABLED("vibrationEnabled", "true", "震动提醒开关"),
    THEME("theme", "light", "主题设置"),
    LANGUAGE("language", "zh-CN", "语言设置"),
    TIMEZONE("timezone", "Asia/Shanghai", "时区设置"),
    DAILY_SUMMARY_TIME("dailySummaryTime", "08:00", "每日总结时间"),
    DAILY_SUMMARY_ENABLED("dailySummaryEnabled", "false", "每日总结开关"),
    WEEKEND_NOTIFICATION_ENABLED("weekendNotificationEnabled", "true", "周末通知开关"),
    QUIET_HOURS_START("quietHoursStart", "22:00", "免打扰开始时间"),
    QUIET_HOURS_END("quietHoursEnd", "07:00", "免打扰结束时间"),
    QUIET_HOURS_ENABLED("quietHoursEnabled", "false", "免打扰模式开关"),
    USER_TAG_MANAGEMENT_ENABLED("userTagManagementEnabled", "0", "用户标签管理功能开关"),
    USER_TAG_LIST("userTagList", "", "用户标签列表"),
    WECHAT_AUTH_COUNT("wechatAuthCount", "0", "微信通知授权剩余次数统计");

    private final String key;
    private final String defaultValue;
    private final String description;

    UserPreferenceKey(String key, String defaultValue, String description) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public static UserPreferenceKey fromKey(String key) {
        if (key == null) {
            return null;
        }
        for (UserPreferenceKey preferenceKey : values()) {
            if (preferenceKey.key.equals(key)) {
                return preferenceKey;
            }
        }
        return null;
    }

    public static boolean isValidKey(String key) {
        return fromKey(key) != null;
    }

    public static String[] getAllKeys() {
        UserPreferenceKey[] values = values();
        String[] keys = new String[values.length];
        for (int index = 0; index < values.length; index++) {
            keys[index] = values[index].key;
        }
        return keys;
    }
}
