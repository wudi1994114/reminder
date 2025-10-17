package com.core.reminder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文本转语音请求DTO
 * 
 * @author wudi
 * @since 2025-01-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsRequest {
    
    /**
     * 需要转换的文本内容
     */
    private String text;
    
    /**
     * 音色ID，默认0（亲和女声）
     * 可选值：0-亲和女声，1-亲和男声，2-成熟男声，4-温暖女声，5-情感女声，6-情感男声等
     */
    @Builder.Default
    private Integer voiceType = 0;
    
    /**
     * 语速，范围[-2, 2]，默认0
     * -2: 0.6倍速，-1: 0.8倍速，0: 1.0倍速，1: 1.2倍速，2: 1.5倍速
     */
    @Builder.Default
    private Integer speed = 0;
    
    /**
     * 音量大小，范围[0, 10]，默认5
     */
    @Builder.Default
    private Integer volume = 5;
    
    /**
     * 音频采样率，默认16000
     * 可选值：8000, 16000
     */
    @Builder.Default
    private Integer sampleRate = 16000;
    
    /**
     * 返回音频格式，默认mp3
     * 可选值：mp3, wav, pcm
     */
    @Builder.Default
    private String codec = "mp3";
    
    /**
     * 是否开启时间戳功能，默认false
     */
    @Builder.Default
    private Boolean enableSubtitle = false;
    
    /**
     * 语言类型，默认中文
     * 1: 中文（默认），2: 英文
     */
    @Builder.Default
    private Integer primaryLanguage = 1;
}

