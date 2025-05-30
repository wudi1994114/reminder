package com.common.reminder.constant;

/**
 * 资源类型枚举
 * 定义系统中可操作的资源类型
 */
public enum ResourceType {
    
    USER("用户"),
    REMINDER("提醒"),
    COMPLEX_REMINDER("复杂提醒"),
    ORGANIZATION("组织"),
    ROLE("角色"),
    PERMISSION("权限"),
    PREFERENCE("偏好设置"),
    SOCIAL_ACCOUNT("第三方账户"),
    FILE("文件"),
    SYSTEM("系统"),
    API_KEY("API密钥"),
    SESSION("会话"),
    UNKNOWN("未知资源");
    
    private final String description;
    
    ResourceType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据字符串获取枚举值，不区分大小写
     */
    public static ResourceType fromString(String resourceType) {
        if (resourceType == null || resourceType.trim().isEmpty()) {
            return UNKNOWN;
        }
        
        try {
            return ResourceType.valueOf(resourceType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
} 