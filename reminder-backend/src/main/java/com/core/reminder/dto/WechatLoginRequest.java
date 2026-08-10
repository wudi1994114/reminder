package com.core.reminder.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 微信小程序登录请求DTO
 */
@Data
public class WechatLoginRequest {

    /**
     * 微信小程序登录凭证code
     */
    @NotBlank(message = "登录凭证不能为空")
    private String code;

    /**
     * 用户信息（可选，用于更新用户资料）
     */
    private WechatUserInfo userInfo;

    @Data
    public static class WechatUserInfo {
        /**
         * 微信用户昵称
         */
        private String nickName;

        /**
         * 微信用户头像URL
         */
        private String avatarUrl;

        /**
         * 微信用户性别：0-未知，1-男，2-女
         */
        private Integer gender;

        /**
         * 微信用户所在国家
         */
        private String country;

        /**
         * 微信用户所在省份
         */
        private String province;

        /**
         * 微信用户所在城市
         */
        private String city;

        /**
         * 微信用户语言
         */
        private String language;
    }
} 