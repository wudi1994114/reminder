package com.core.reminder.service;

import com.core.reminder.config.TencentCloudConfig;
import com.core.reminder.dto.TtsRequest;
import com.core.reminder.dto.TtsResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.tts.v20190823.TtsClient;
import com.tencentcloudapi.tts.v20190823.models.TextToVoiceRequest;
import com.tencentcloudapi.tts.v20190823.models.TextToVoiceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * 腾讯云文本转语音服务
 * 
 * @author wudi
 * @since 2025-01-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TencentTtsService {
    
    private final TencentCloudConfig tencentCloudConfig;
    
    /**
     * 文本转语音
     * 
     * @param request TTS请求参数
     * @return TTS响应结果
     * @throws TencentCloudSDKException SDK异常
     */
    public TtsResponse textToVoice(TtsRequest request) throws TencentCloudSDKException {
        log.info("开始文本转语音，文本长度: {}, 音色: {}, 语速: {}", 
                request.getText().length(), request.getVoiceType(), request.getSpeed());
        
        try {
            // 创建认证对象
            Credential cred = new Credential(
                    tencentCloudConfig.getSecretId(), 
                    tencentCloudConfig.getSecretKey()
            );
            
            // 创建HTTP配置
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("tts.tencentcloudapi.com");
            
            // 创建客户端配置
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            
            // 创建TTS客户端
            TtsClient client = new TtsClient(
                    cred, 
                    tencentCloudConfig.getTts().getRegion(), 
                    clientProfile
            );
            
            // 构建请求
            TextToVoiceRequest ttsRequest = new TextToVoiceRequest();
            ttsRequest.setText(request.getText());
            ttsRequest.setSessionId(java.util.UUID.randomUUID().toString()); // 设置SessionId
            ttsRequest.setVoiceType(request.getVoiceType().longValue());
            ttsRequest.setSpeed(request.getSpeed().floatValue());
            ttsRequest.setVolume(request.getVolume().floatValue());
            ttsRequest.setSampleRate(request.getSampleRate().longValue());
            ttsRequest.setCodec(request.getCodec());
            ttsRequest.setEnableSubtitle(request.getEnableSubtitle());
            ttsRequest.setPrimaryLanguage(request.getPrimaryLanguage().longValue());
            
            // 调用API
            TextToVoiceResponse response = client.TextToVoice(ttsRequest);
            
            // 构建响应
            TtsResponse ttsResponse = TtsResponse.builder()
                    .audioData(response.getAudio())
                    .codec(request.getCodec())
                    .requestId(response.getRequestId())
                    .subtitle(response.getSubtitles() != null && response.getSubtitles().length > 0 
                            ? response.getSubtitles()[0].getText() : null)
                    .success(true)
                    .build();
            
            // 计算音频大小
            if (response.getAudio() != null) {
                byte[] audioBytes = Base64.getDecoder().decode(response.getAudio());
                ttsResponse.setAudioSize((long) audioBytes.length);
            }
            
            log.info("文本转语音成功，RequestId: {}, 音频大小: {} bytes", 
                    response.getRequestId(), ttsResponse.getAudioSize());
            
            return ttsResponse;
            
        } catch (TencentCloudSDKException e) {
            log.error("文本转语音失败: {}", e.getMessage(), e);
            
            return TtsResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
    
    /**
     * 文本转语音（使用默认参数）
     * 
     * @param text 要转换的文本
     * @return TTS响应结果
     * @throws TencentCloudSDKException SDK异常
     */
    public TtsResponse textToVoice(String text) throws TencentCloudSDKException {
        TtsRequest request = TtsRequest.builder()
                .text(text)
                .build();
        return textToVoice(request);
    }
    
    /**
     * 文本转语音并保存到文件
     * 
     * @param request TTS请求参数
     * @param filePath 保存路径
     * @return 是否成功
     * @throws TencentCloudSDKException SDK异常
     */
    public boolean textToVoiceAndSave(TtsRequest request, String filePath) throws TencentCloudSDKException {
        TtsResponse response = textToVoice(request);
        
        if (!response.getSuccess()) {
            log.error("文本转语音失败: {}", response.getErrorMessage());
            return false;
        }
        
        try {
            // 解码Base64音频数据
            byte[] audioBytes = Base64.getDecoder().decode(response.getAudioData());
            
            // 保存到文件
            java.nio.file.Files.write(
                    java.nio.file.Paths.get(filePath), 
                    audioBytes
            );
            
            log.info("音频文件已保存到: {}", filePath);
            return true;
            
        } catch (Exception e) {
            log.error("保存音频文件失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 验证文本长度是否合法
     * 腾讯云TTS限制：单次请求文本长度不超过150字符
     * 
     * @param text 文本内容
     * @return 是否合法
     */
    public boolean validateTextLength(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.length() <= 150;
    }
    
    /**
     * 分段处理长文本
     * 
     * @param text 长文本
     * @param maxLength 每段最大长度
     * @return 文本段列表
     */
    public java.util.List<String> splitText(String text, int maxLength) {
        java.util.List<String> segments = new java.util.ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return segments;
        }
        
        int length = text.length();
        for (int i = 0; i < length; i += maxLength) {
            int end = Math.min(i + maxLength, length);
            segments.add(text.substring(i, end));
        }
        
        return segments;
    }
}

