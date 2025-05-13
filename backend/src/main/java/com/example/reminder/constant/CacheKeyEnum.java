package com.example.reminder.constant;

/**
 * Redis缓存键枚举
 * 统一管理所有Redis的键名
 * 命名规范：
 * 1. 使用冒号(:)分隔不同级别的键名
 * 2. 键名使用小写字母
 * 3. 多个单词使用下划线(_)分隔
 */
public enum CacheKeyEnum {
    
    /**
     * 待执行的提醒事项
     */
    PENDING_REMINDER(
        "reminder:pending:%s",
        "按时间存储待执行的提醒事项，格式：reminder:pending:yyyy-MM-dd HH:mm",
        60 * 60 * 24 * 7 // 5分钟过期
    ),
    
    /**
     * 用户信息缓存
     * 完整键名格式: user:info:userId
     */
    Bottom("Bottom", "垫底专用", -1L);
    
    private final String keyPattern;
    private final String comment;
    private final long expireSeconds;
    
    CacheKeyEnum(String keyPattern, String comment, long expireSeconds) {
        this.keyPattern = keyPattern;
        this.comment = comment;
        this.expireSeconds = expireSeconds;
    }
    
    CacheKeyEnum(String keyPattern) {
        this.keyPattern = keyPattern;
        this.comment = "";
        this.expireSeconds = 0L;
    }
    
    /**
     * 获取格式化后的缓存键
     * @param args 格式化参数
     * @return 格式化后的缓存键
     */
    public String getKey(Object... args) {
        return String.format(keyPattern, args);
    }
    
    /**
     * 获取键的说明信息
     * @return 说明信息
     */
    public String getComment() {
        return comment;
    }
    
    /**
     * 获取过期时间（秒）
     * @return 过期时间
     */
    public long getExpireSeconds() {
        return expireSeconds;
    }
} 