package com.core.reminder.service;

import com.core.reminder.config.TencentCloudConfig;
import com.core.reminder.dto.TtsRequest;
import com.core.reminder.dto.TtsResponse;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 腾讯云TTS服务测试
 * 
 * @author wudi
 * @since 2025-01-17
 */
@SpringBootTest
class TencentTtsServiceTest {

    @Autowired
    private TencentTtsService tencentTtsService;

    @Autowired
    private TencentCloudConfig tencentCloudConfig;

    @BeforeEach
    void setUp() {
        assertNotNull(tencentTtsService);
        assertNotNull(tencentCloudConfig);
    }

    /**
     * 测试基本文本转语音功能（需要真实的API密钥）
     */
    @Test
    @Disabled("需要真实的腾讯云API密钥才能运行")
    void testTextToVoice_Basic() throws TencentCloudSDKException {
        // 准备测试数据
        String text = "你好，这是一个测试文本转语音的例子。";
        
        // 执行转换
        TtsResponse response = tencentTtsService.textToVoice(text);
        
        // 验证结果
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertNotNull(response.getAudioData());
        assertNotNull(response.getRequestId());
        assertTrue(response.getAudioSize() > 0);
        assertEquals("mp3", response.getCodec());
        
        System.out.println("RequestId: " + response.getRequestId());
        System.out.println("音频大小: " + response.getAudioSize() + " bytes");
    }

    /**
     * 测试使用自定义参数的文本转语音
     */
    @Test
    @Disabled("需要真实的腾讯云API密钥才能运行")
    void testTextToVoice_WithCustomParams() throws TencentCloudSDKException {
        // 准备测试数据
        TtsRequest request = TtsRequest.builder()
                .text("春眠不觉晓，处处闻啼鸟。")
                .voiceType(1)  // 使用男声
                .speed(1)      // 1.2倍速
                .volume(8)     // 音量8
                .codec("mp3")
                .build();
        
        // 执行转换
        TtsResponse response = tencentTtsService.textToVoice(request);
        
        // 验证结果
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertNotNull(response.getAudioData());
        assertTrue(response.getAudioSize() > 0);
        
        System.out.println("RequestId: " + response.getRequestId());
        System.out.println("音频大小: " + response.getAudioSize() + " bytes");
    }

    /**
     * 测试保存音频到文件
     */
    @Test
    @Disabled("需要真实的腾讯云API密钥才能运行")
    void testTextToVoiceAndSave() throws TencentCloudSDKException {
        // 准备测试数据
        TtsRequest request = TtsRequest.builder()
                .text("这是一个保存到文件的测试。")
                .voiceType(0)
                .codec("mp3")
                .build();
        
        String filePath = "target/test-tts-output.mp3";
        
        // 执行转换并保存
        boolean success = tencentTtsService.textToVoiceAndSave(request, filePath);
        
        // 验证结果
        assertTrue(success);
        assertTrue(java.nio.file.Files.exists(java.nio.file.Paths.get(filePath)));
        
        System.out.println("音频已保存到: " + filePath);
    }

    /**
     * 测试文本长度验证
     */
    @Test
    void testValidateTextLength() {
        // 测试正常长度
        assertTrue(tencentTtsService.validateTextLength("正常长度的文本"));
        
        // 测试空文本
        assertFalse(tencentTtsService.validateTextLength(""));
        assertFalse(tencentTtsService.validateTextLength(null));
        
        // 测试超长文本
        String longText = "a".repeat(200);
        assertFalse(tencentTtsService.validateTextLength(longText));
        
        // 测试边界值
        String boundaryText = "a".repeat(150);
        assertTrue(tencentTtsService.validateTextLength(boundaryText));
    }

    /**
     * 测试文本分段功能
     */
    @Test
    void testSplitText() {
        // 测试正常分段
        String text = "这是一个很长的文本，需要分成多段来处理。腾讯云TTS单次请求限制150字符。";
        List<String> segments = tencentTtsService.splitText(text, 20);
        
        assertNotNull(segments);
        assertTrue(segments.size() > 1);
        segments.forEach(segment -> assertTrue(segment.length() <= 20));
        
        System.out.println("分段数量: " + segments.size());
        segments.forEach(segment -> System.out.println("段落: " + segment));
        
        // 测试空文本
        List<String> emptySegments = tencentTtsService.splitText("", 20);
        assertTrue(emptySegments.isEmpty());
        
        // 测试null
        List<String> nullSegments = tencentTtsService.splitText(null, 20);
        assertTrue(nullSegments.isEmpty());
    }

    /**
     * 测试不同音色
     */
    @Test
    @Disabled("需要真实的腾讯云API密钥才能运行")
    void testDifferentVoiceTypes() throws TencentCloudSDKException {
        String text = "测试不同的音色效果。";
        
        // 测试多个音色
        int[] voiceTypes = {0, 1, 2, 4, 5, 6};
        
        for (int voiceType : voiceTypes) {
            TtsRequest request = TtsRequest.builder()
                    .text(text)
                    .voiceType(voiceType)
                    .build();
            
            TtsResponse response = tencentTtsService.textToVoice(request);
            
            assertTrue(response.getSuccess());
            System.out.println("音色 " + voiceType + " 转换成功，音频大小: " + response.getAudioSize());
        }
    }

    /**
     * 测试不同语速
     */
    @Test
    @Disabled("需要真实的腾讯云API密钥才能运行")
    void testDifferentSpeeds() throws TencentCloudSDKException {
        String text = "测试不同的语速效果。";
        
        // 测试多个语速
        int[] speeds = {-2, -1, 0, 1, 2};
        
        for (int speed : speeds) {
            TtsRequest request = TtsRequest.builder()
                    .text(text)
                    .speed(speed)
                    .build();
            
            TtsResponse response = tencentTtsService.textToVoice(request);
            
            assertTrue(response.getSuccess());
            System.out.println("语速 " + speed + " 转换成功，音频大小: " + response.getAudioSize());
        }
    }

    /**
     * 测试不同音频格式
     */
    @Test
    @Disabled("需要真实的腾讯云API密钥才能运行")
    void testDifferentCodecs() throws TencentCloudSDKException {
        String text = "测试不同的音频格式。";
        
        // 测试多种格式
        String[] codecs = {"mp3", "wav", "pcm"};
        
        for (String codec : codecs) {
            TtsRequest request = TtsRequest.builder()
                    .text(text)
                    .codec(codec)
                    .build();
            
            TtsResponse response = tencentTtsService.textToVoice(request);
            
            assertTrue(response.getSuccess());
            assertEquals(codec, response.getCodec());
            System.out.println("格式 " + codec + " 转换成功，音频大小: " + response.getAudioSize());
        }
    }

    /**
     * 测试长文本分段转换
     */
    @Test
    @Disabled("需要真实的腾讯云API密钥才能运行")
    void testLongTextConversion() throws TencentCloudSDKException {
        String longText = "这是一段很长的文本。" + "需要分段处理。".repeat(30);
        
        // 分段
        List<String> segments = tencentTtsService.splitText(longText, 150);
        
        System.out.println("长文本分为 " + segments.size() + " 段");
        
        // 逐段转换
        for (int i = 0; i < segments.size(); i++) {
            TtsResponse response = tencentTtsService.textToVoice(segments.get(i));
            assertTrue(response.getSuccess());
            System.out.println("第 " + (i + 1) + " 段转换成功，音频大小: " + response.getAudioSize());
        }
    }
}

