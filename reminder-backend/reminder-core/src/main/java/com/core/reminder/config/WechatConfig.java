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
    private String appid = "wx597f8aaca581205f";

    /**
     * 小程序secret
     */
    private String secret = "b70c1fd582a6f6d66e8dc1e2e436d023";

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