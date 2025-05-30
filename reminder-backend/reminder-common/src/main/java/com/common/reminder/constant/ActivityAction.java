package com.common.reminder.constant;

/**
 * 用户活动操作类型枚举
 * 定义系统中所有可记录的用户操作类型
 */
public enum ActivityAction {
    
    // 认证相关操作
    LOGIN("用户登录"),
    LOGOUT("用户登出"),
    REGISTER("用户注册"),
    PASSWORD_CHANGE("修改密码"),
    PASSWORD_RESET("重置密码"),
    LOGIN_FAILED("登录失败"),
    ACCOUNT_LOCKED("账户锁定"),
    ACCOUNT_UNLOCKED("账户解锁"),
    
    // 用户资料操作
    PROFILE_UPDATE("更新个人资料"),
    AVATAR_UPLOAD("上传头像"),
    EMAIL_VERIFY("邮箱验证"),
    PHONE_VERIFY("手机验证"),
    PREFERENCE_UPDATE("更新偏好设置"),
    
    // 提醒相关操作
    REMINDER_CREATE("创建提醒"),
    REMINDER_UPDATE("更新提醒"),
    REMINDER_DELETE("删除提醒"),
    REMINDER_VIEW("查看提醒"),
    REMINDER_EXECUTE("执行提醒"),
    COMPLEX_REMINDER_CREATE("创建复杂提醒"),
    COMPLEX_REMINDER_UPDATE("更新复杂提醒"),
    COMPLEX_REMINDER_DELETE("删除复杂提醒"),
    
    // 系统操作
    SYSTEM_CONFIG_UPDATE("更新系统配置"),
    DATA_EXPORT("数据导出"),
    DATA_IMPORT("数据导入"),
    
    // 安全相关操作
    PERMISSION_GRANT("授予权限"),
    PERMISSION_REVOKE("撤销权限"),
    ROLE_ASSIGN("分配角色"),
    ROLE_REMOVE("移除角色"),
    
    // 组织相关操作
    ORGANIZATION_CREATE("创建组织"),
    ORGANIZATION_UPDATE("更新组织"),
    ORGANIZATION_DELETE("删除组织"),
    ORGANIZATION_JOIN("加入组织"),
    ORGANIZATION_LEAVE("离开组织"),
    
    // 第三方集成操作
    WECHAT_LOGIN("微信登录"),
    SOCIAL_ACCOUNT_BIND("绑定第三方账户"),
    SOCIAL_ACCOUNT_UNBIND("解绑第三方账户"),
    
    // API相关操作
    API_ACCESS("API访问"),
    API_KEY_CREATE("创建API密钥"),
    API_KEY_DELETE("删除API密钥"),
    
    // 其他操作
    FILE_UPLOAD("文件上传"),
    FILE_DELETE("文件删除"),
    SEARCH("搜索操作"),
    UNKNOWN("未知操作");
    
    private final String description;
    
    ActivityAction(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据字符串获取枚举值，不区分大小写
     */
    public static ActivityAction fromString(String action) {
        if (action == null || action.trim().isEmpty()) {
            return UNKNOWN;
        }
        
        try {
            return ActivityAction.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
} 