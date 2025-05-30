package com.common.reminder.constant;

/**
 * 活动状态枚举
 * 定义操作执行的结果状态
 */
public enum ActivityStatus {
    
    SUCCESS("成功"),
    FAILED("失败"),
    PARTIAL("部分成功"),
    PENDING("待处理"),
    CANCELLED("已取消");
    
    private final String description;
    
    ActivityStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据字符串获取枚举值，不区分大小写
     */
    public static ActivityStatus fromString(String status) {
        if (status == null || status.trim().isEmpty()) {
            return SUCCESS; // 默认为成功
        }
        
        try {
            return ActivityStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SUCCESS;
        }
    }
} 