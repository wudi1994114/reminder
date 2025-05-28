package com.core.reminder.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 微信API响应DTO
 */
@Data
public class WechatApiResponse {

    /**
     * 用户唯一标识
     */
    @JsonProperty("openid")
    private String openid;

    /**
     * 会话密钥
     */
    @JsonProperty("session_key")
    private String sessionKey;

    /**
     * 用户在开放平台的唯一标识符
     */
    @JsonProperty("unionid")
    private String unionid;

    /**
     * 错误码
     */
    @JsonProperty("errcode")
    private Integer errcode;

    /**
     * 错误信息
     */
    @JsonProperty("errmsg")
    private String errmsg;
} 