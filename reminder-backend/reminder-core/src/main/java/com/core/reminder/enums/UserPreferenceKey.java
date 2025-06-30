package com.core.reminder.enums;

import lombok.Getter;

/**
 * 用户偏好设置键值枚举
 * 统一管理所有用户配置项的键名、默认值和描述
 */
@Getter
public enum UserPreferenceKey {
    
    // ==================== 通知相关配置 ====================
    DEFAULT_REMINDER_TYPE("defaultReminderType", "EMAIL", "默认提醒方式"),
    EMAIL_NOTIFICATION_ENABLED("emailNotificationEnabled", "true", "邮件通知开关"),
    SMS_NOTIFICATION_ENABLED("smsNotificationEnabled", "false", "短信通知开关"),
    WECHAT_NOTIFICATION_ENABLED("wechatNotificationEnabled", "false", "微信通知开关"),
    
    // ==================== 提醒设置 ====================
    DEFAULT_ADVANCE_MINUTES("defaultAdvanceMinutes", "15", "默认提前提醒分钟数"),
    SOUND_ENABLED("soundEnabled", "true", "声音提醒开关"),
    VIBRATION_ENABLED("vibrationEnabled", "true", "震动提醒开关"),
    
    // ==================== 界面设置 ====================
    THEME("theme", "light", "主题设置"),
    LANGUAGE("language", "zh-CN", "语言设置"),
    TIMEZONE("timezone", "Asia/Shanghai", "时区设置"),
    
    // ==================== 时间设置 ====================
    DAILY_SUMMARY_TIME("dailySummaryTime", "08:00", "每日总结时间"),
    DAILY_SUMMARY_ENABLED("dailySummaryEnabled", "false", "每日总结开关"),
    WEEKEND_NOTIFICATION_ENABLED("weekendNotificationEnabled", "true", "周末通知开关"),
    
    // ==================== 免打扰设置 ====================
    QUIET_HOURS_START("quietHoursStart", "22:00", "免打扰开始时间"),
    QUIET_HOURS_END("quietHoursEnd", "07:00", "免打扰结束时间"),
    QUIET_HOURS_ENABLED("quietHoursEnabled", "false", "免打扰模式开关"),
    
    // ==================== 用户标签管理 ====================
    USER_TAG_MANAGEMENT_ENABLED("userTagManagementEnabled", "0", "用户标签管理功能开关，0关闭，1开启"),
    USER_TAG_LIST("userTagList", "", "用户标签列表，|||分隔不同标签，|分隔标题和内容，总长度不超过100个字符");
    
    /**
     * 配置项键名
     */
    private final String key;
    
    /**
     * 默认值
     */
    private final String defaultValue;
    
    /**
     * 配置项描述
     */
    private final String description;
    
    UserPreferenceKey(String key, String defaultValue, String description) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.description = description;
    }
    
    /**
     * 根据键名查找枚举
     * @param key 键名
     * @return 对应的枚举值，如果不存在则返回null
     */
    public static UserPreferenceKey fromKey(String key) {
        if (key == null) {
            return null;
        }
        
        for (UserPreferenceKey preferenceKey : values()) {
            if (preferenceKey.getKey().equals(key)) {
                return preferenceKey;
            }
        }
        return null;
    }
    
    /**
     * 检查键名是否为有效的配置项
     * @param key 键名
     * @return 是否有效
     */
    public static boolean isValidKey(String key) {
        return fromKey(key) != null;
    }
    
    /**
     * 获取所有配置项的键名数组
     * @return 所有键名
     */
    public static String[] getAllKeys() {
        UserPreferenceKey[] values = values();
        String[] keys = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            keys[i] = values[i].getKey();
        }
        return keys;
    }
}
