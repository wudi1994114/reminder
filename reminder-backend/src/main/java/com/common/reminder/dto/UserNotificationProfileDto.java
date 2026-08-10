package com.common.reminder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户通知配置信息DTO
 * 包含用户基本信息和通知相关的配置（邮箱、微信openid等）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationProfileDto {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱地址
     */
    private String email;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 头像URL
     */
    private String avatarUrl;
    
    /**
     * 手机号
     */
    private String phoneNumber;
    
    /**
     * 微信openid（用于发送微信通知）
     */
    private String wechatOpenid;
    
    /**
     * 微信unionid
     */
    private String wechatUnionid;
    
    /**
     * 是否启用邮件通知
     */
    private Boolean emailNotificationEnabled;
    
    /**
     * 是否启用微信通知
     */
    private Boolean wechatNotificationEnabled;
    
    /**
     * 从基础UserProfileDto构造
     */
    public UserNotificationProfileDto(UserProfileDto userProfile) {
        if (userProfile != null) {
            this.id = userProfile.getId();
            this.username = userProfile.getUsername();
            this.email = userProfile.getEmail();
            this.nickname = userProfile.getNickname();
            this.avatarUrl = userProfile.getAvatarUrl();
            this.phoneNumber = userProfile.getPhoneNumber();
        }
        // 默认启用所有通知方式
        this.emailNotificationEnabled = true;
        this.wechatNotificationEnabled = true;
    }
    
    /**
     * 检查是否有有效的邮箱地址
     */
    public boolean hasValidEmail() {
        return email != null && !email.trim().isEmpty() && email.contains("@");
    }
    
    /**
     * 检查是否有有效的微信openid
     */
    public boolean hasValidWechatOpenid() {
        return wechatOpenid != null && !wechatOpenid.trim().isEmpty() && wechatOpenid.startsWith("o");
    }
    
    /**
     * 检查邮件通知是否可用
     */
    public boolean isEmailNotificationAvailable() {
        return Boolean.TRUE.equals(emailNotificationEnabled) && hasValidEmail();
    }
    
    /**
     * 检查微信通知是否可用
     */
    public boolean isWechatNotificationAvailable() {
        return Boolean.TRUE.equals(wechatNotificationEnabled) && hasValidWechatOpenid();
    }
}