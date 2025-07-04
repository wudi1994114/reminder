package com.core.reminder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信小程序登录响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WechatLoginResponse {

    /**
     * JWT访问令牌
     */
    private String accessToken;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像
     */
    private String avatarUrl;

    /**
     * 是否为新用户
     */
    private Boolean isNewUser;

    /**
     * 微信openid（可选返回）
     */
    private String openid;

    /**
     * 微信unionid（可选返回）
     */
    private String unionid;

    /**
     * 构造函数（不包含unionid，向后兼容）
     */
    public WechatLoginResponse(String accessToken, Long userId, String nickname, 
                              String avatarUrl, Boolean isNewUser, String openid) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.isNewUser = isNewUser;
        this.openid = openid;
        this.unionid = null;
    }
} 