package com.core.reminder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信小程序配置类
 */
@Configuration
@ConfigurationProperties(prefix = "wechat.miniprogram")
@Data
public class WechatConfig {

    /**
     * 小程序appid
     */
    private String appid;

    /**
     * 小程序secret
     */
    private String secret;

    /**
     * 微信API基础URL
     */
    private String apiBaseUrl = "https://api.weixin.qq.com";

    /**
     * 获取session_key的URL
     */
    private String jscode2sessionUrl = "/sns/jscode2session";

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 5000;

    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 10000;
} 