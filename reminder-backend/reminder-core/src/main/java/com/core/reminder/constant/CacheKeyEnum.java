package com.core.reminder.constant;

import java.util.concurrent.TimeUnit;

/**
 * 缓存键枚举类
 * 统一管理所有缓存键的前缀和过期时间
 */
public enum CacheKeyEnum {
    
    /**
     * 用户提醒ZSet缓存（按时间戳排序）
     * 格式: user:reminders:zset:{userId}
     * 过期时间: 永不过期（长期缓存，与数据库同步）
     * 使用ZSet存储，score为时间戳，value为提醒JSON
     */
    USER_REMINDERS_ZSET("user:reminders:zset:", -1, null),
    
    /**
     * 用户信息缓存
     * 格式: user:info:{userId}
     * 过期时间: 30分钟
     */
    USER_INFO("user:info:", 30, TimeUnit.MINUTES),
    
    /**
     * 用户权限缓存
     * 格式: user:permissions:{userId}
     * 过期时间: 15分钟
     */
    USER_PERMISSIONS("user:permissions:", 15, TimeUnit.MINUTES),
    
    /**
     * 节假日缓存
     * 格式: holiday:{year}
     * 过期时间: 永不过期
     */
    HOLIDAY_CACHE("holiday:", -1, null),
    
    /**
     * 系统配置缓存
     * 格式: system:config:{configKey}
     * 过期时间: 1小时
     */
    SYSTEM_CONFIG("system:config:", 1, TimeUnit.HOURS),
    
    /**
     * 用户会话缓存
     * 格式: user:session:{sessionId}
     * 过期时间: 2小时
     */
    USER_SESSION("user:session:", 2, TimeUnit.HOURS),
    
    /**
     * 验证码缓存
     * 格式: verification:code:{phone/email}
     * 过期时间: 5分钟
     */
    VERIFICATION_CODE("verification:code:", 5, TimeUnit.MINUTES),
    
    /**
     * API限流缓存
     * 格式: rate:limit:{userId}:{api}
     * 过期时间: 1分钟
     */
    RATE_LIMIT("rate:limit:", 1, TimeUnit.MINUTES);
    
    /**
     * 缓存键前缀
     */
    private final String keyPrefix;
    
    /**
     * 过期时间值
     * -1 表示永不过期
     */
    private final long expireTime;
    
    /**
     * 时间单位
     * 当expireTime为-1时，此字段为null
     */
    private final TimeUnit timeUnit;
    
    CacheKeyEnum(String keyPrefix, long expireTime, TimeUnit timeUnit) {
        this.keyPrefix = keyPrefix;
        this.expireTime = expireTime;
        this.timeUnit = timeUnit;
    }
    
    /**
     * 获取缓存键前缀
     */
    public String getKeyPrefix() {
        return keyPrefix;
    }
    
    /**
     * 获取过期时间
     * @return 过期时间，-1表示永不过期
     */
    public long getExpireTime() {
        return expireTime;
    }
    
    /**
     * 获取时间单位
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
    
    /**
     * 是否永不过期
     */
    public boolean isNeverExpire() {
        return expireTime == -1;
    }
    
    /**
     * 构建完整的缓存键
     * @param suffix 缓存键后缀
     * @return 完整的缓存键
     */
    public String buildKey(String suffix) {
        return keyPrefix + suffix;
    }
    
    /**
     * 构建用户提醒ZSet缓存键
     * @param userId 用户ID
     * @return 缓存键
     */
    public static String buildUserRemindersZSetKey(Long userId) {
        return USER_REMINDERS_ZSET.buildKey(String.valueOf(userId));
    }
    
    /**
     * 构建用户信息缓存键
     * @param userId 用户ID
     * @return 缓存键
     */
    public static String buildUserInfoKey(Long userId) {
        return USER_INFO.buildKey(String.valueOf(userId));
    }
    
    /**
     * 构建用户权限缓存键
     * @param userId 用户ID
     * @return 缓存键
     */
    public static String buildUserPermissionsKey(Long userId) {
        return USER_PERMISSIONS.buildKey(String.valueOf(userId));
    }
    
    /**
     * 构建节假日缓存键
     * @param year 年份
     * @return 缓存键
     */
    public static String buildHolidayKey(int year) {
        return HOLIDAY_CACHE.buildKey(String.valueOf(year));
    }
    
    /**
     * 构建系统配置缓存键
     * @param configKey 配置键
     * @return 缓存键
     */
    public static String buildSystemConfigKey(String configKey) {
        return SYSTEM_CONFIG.buildKey(configKey);
    }
    
    /**
     * 构建用户会话缓存键
     * @param sessionId 会话ID
     * @return 缓存键
     */
    public static String buildUserSessionKey(String sessionId) {
        return USER_SESSION.buildKey(sessionId);
    }
    
    /**
     * 构建验证码缓存键
     * @param contact 联系方式（手机号或邮箱）
     * @return 缓存键
     */
    public static String buildVerificationCodeKey(String contact) {
        return VERIFICATION_CODE.buildKey(contact);
    }
    
    /**
     * 构建API限流缓存键
     * @param userId 用户ID
     * @param api API标识
     * @return 缓存键
     */
    public static String buildRateLimitKey(Long userId, String api) {
        return RATE_LIMIT.buildKey(userId + ":" + api);
    }
} 