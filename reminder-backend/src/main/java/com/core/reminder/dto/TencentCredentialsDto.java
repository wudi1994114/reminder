package com.core.reminder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 腾讯云临时凭证DTO
 * 
 * @author wudi
 * @since 2025-06-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TencentCredentialsDto {
    
    /**
     * 临时密钥ID
     */
    private String tmpSecretId;
    
    /**
     * 临时密钥Key
     */
    private String tmpSecretKey;
    
    /**
     * 会话令牌
     */
    private String sessionToken;
    
    /**
     * 过期时间（Unix时间戳）
     */
    private Long expiredTime;

    /**
     * 服务地域
     */
    private String region;
}
