package com.core.reminder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云配置类
 * 
 * @author wudi
 * @since 2025-06-22
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "tencent.cloud")
public class TencentCloudConfig {
    
    /**
     * 腾讯云永久密钥ID
     */
    private String secretId;
    
    /**
     * 腾讯云永久密钥Key
     */
    private String secretKey;
    
    /**
     * STS临时密钥配置
     */
    private StsConfig sts = new StsConfig();
    
    /**
     * STS临时密钥配置
     */
    @Data
    public static class StsConfig {
        /**
         * 临时密钥有效期（秒）
         */
        private Integer durationSeconds = 1800;
        
        /**
         * 服务地域
         */
        private String region = "ap-beijing";
    }
    

}
