package com.core.reminder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文本转语音响应DTO
 * 
 * @author wudi
 * @since 2025-01-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsResponse {
    
    /**
     * 音频数据（Base64编码）
     */
    private String audioData;
    
    /**
     * 音频格式
     */
    private String codec;
    
    /**
     * 音频大小（字节）
     */
    private Long audioSize;
    
    /**
     * 请求ID
     */
    private String requestId;
    
    /**
     * 时间戳信息（如果开启）
     */
    private String subtitle;
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;
}

