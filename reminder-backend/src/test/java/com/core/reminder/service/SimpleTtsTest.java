package com.core.reminder.service;

import com.core.reminder.config.TencentCloudConfig;
import com.core.reminder.dto.TtsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TTS音频生成测试类
 * 
 * 使用步骤：
 * 1. 在 application-wechat.yml 中配置腾讯云密钥
 *    tencent.cloud.secret-id 和 tencent.cloud.secret-key
 * 2. 运行测试方法生成测试音频
 * 3. 音频文件会保存到 target/tts-output/ 目录
 * 
 * 注意：如果密钥未配置，测试会自动跳过
 * 
 * @author wudi
 * @since 2025-01-17
 */
@SpringBootTest
@ActiveProfiles("test")
class SimpleTtsTest {

    @Autowired
    private TencentTtsService tencentTtsService;

    @Autowired
    private TencentCloudConfig tencentCloudConfig;

    private static final String OUTPUT_DIR = "target/tts-output/";

    @BeforeEach
    void setUp() {
        assertNotNull(tencentTtsService, "TencentTtsService应该被注入");
        assertNotNull(tencentCloudConfig, "TencentCloudConfig应该被注入");
    }

    /**
     * 检查密钥是否已配置
     */
    private boolean isConfigured() {
        String secretId = tencentCloudConfig.getSecretId();
        return secretId != null && !secretId.isEmpty() && !secretId.contains("你的");
    }

    /**
     * 测试1：生成基本的测试音频
     * 这是最简单的测试，生成一个"你好"的音频文件
     */
    @Test
    void test01_GenerateBasicAudio() throws Exception {
        if (!isConfigured()) {
            System.out.println("⚠️  跳过测试：腾讯云密钥未配置");
            System.out.println("   请在 application-wechat.yml 中配置 tencent.cloud.secret-id 和 secret-key");
            return;
        }

        System.out.println("\n========== 测试1：生成基本音频 ==========");
        
        // 创建输出目录
        Files.createDirectories(Paths.get(OUTPUT_DIR));
        
        String text = "你好，这是一个文本转语音测试。";
        String outputFile = OUTPUT_DIR + "01-basic.mp3";
        
        TtsRequest request = TtsRequest.builder()
                .text(text)
                .voiceType(0)  // 女声
                .build();
        
        System.out.println("文本：" + text);
        System.out.println("输出文件：" + outputFile);
        
        boolean success = tencentTtsService.textToVoiceAndSave(request, outputFile);
        
        assertTrue(success, "音频生成应该成功");
        assertTrue(new File(outputFile).exists(), "音频文件应该存在");
        
        System.out.println("✅ 音频生成成功：" + new File(outputFile).getAbsolutePath());
    }

    /**
     * 测试2：生成不同音色的音频
     * 测试女声、男声等不同音色
     */
    @Test
    void test02_GenerateDifferentVoices() throws Exception {
        if (!isConfigured()) {
            System.out.println("⚠️  跳过测试：腾讯云密钥未配置");
            return;
        }

        System.out.println("\n========== 测试2：不同音色对比 ==========");
        
        Files.createDirectories(Paths.get(OUTPUT_DIR));
        
        String text = "这是音色测试，听听效果如何。";
        
        // 测试几种常见音色
        int[] voiceTypes = {0, 1, 2};
        String[] voiceNames = {"亲和女声", "亲和男声", "成熟男声"};
        
        for (int i = 0; i < voiceTypes.length; i++) {
            String outputFile = OUTPUT_DIR + String.format("02-voice-%d-%s.mp3", voiceTypes[i], voiceNames[i]);
            
            TtsRequest request = TtsRequest.builder()
                    .text(text)
                    .voiceType(voiceTypes[i])
                    .build();
            
            System.out.println("\n生成音色：" + voiceNames[i]);
            
            boolean success = tencentTtsService.textToVoiceAndSave(request, outputFile);
            
            assertTrue(success, voiceNames[i] + " 应该生成成功");
            System.out.println("✅ " + voiceNames[i] + " 生成成功：" + outputFile);
        }
    }

    /**
     * 测试3：生成不同语速的音频
     * 测试慢速、正常、快速
     */
    @Test
    void test03_GenerateDifferentSpeeds() throws Exception {
        if (!isConfigured()) {
            System.out.println("⚠️  跳过测试：腾讯云密钥未配置");
            return;
        }

        System.out.println("\n========== 测试3：不同语速对比 ==========");
        
        Files.createDirectories(Paths.get(OUTPUT_DIR));
        
        String text = "这是语速测试，听听快慢的区别。";
        
        int[] speeds = {-1, 0, 1};
        String[] speedNames = {"慢速0.8倍", "正常1.0倍", "快速1.2倍"};
        
        for (int i = 0; i < speeds.length; i++) {
            String outputFile = OUTPUT_DIR + String.format("03-speed-%s.mp3", speedNames[i]);
            
            TtsRequest request = TtsRequest.builder()
                    .text(text)
                    .speed(speeds[i])
                    .build();
            
            System.out.println("\n生成语速：" + speedNames[i]);
            
            boolean success = tencentTtsService.textToVoiceAndSave(request, outputFile);
            
            assertTrue(success, speedNames[i] + " 应该生成成功");
            System.out.println("✅ " + speedNames[i] + " 生成成功：" + outputFile);
        }
    }

    /**
     * 测试4：生成一首诗的音频
     * 测试较长文本和标点符号的处理
     */
    @Test
    void test04_GeneratePoem() throws Exception {
        if (!isConfigured()) {
            System.out.println("⚠️  跳过测试：腾讯云密钥未配置");
            return;
        }

        System.out.println("\n========== 测试4：生成诗歌朗诵 ==========");
        
        Files.createDirectories(Paths.get(OUTPUT_DIR));
        
        String text = "春眠不觉晓，处处闻啼鸟。夜来风雨声，花落知多少。";
        String outputFile = OUTPUT_DIR + "04-poem.mp3";
        
        TtsRequest request = TtsRequest.builder()
                .text(text)
                .voiceType(1)  // 使用男声朗诵
                .speed(0)
                .volume(7)
                .build();
        
        System.out.println("文本：" + text);
        System.out.println("输出文件：" + outputFile);
        
        boolean success = tencentTtsService.textToVoiceAndSave(request, outputFile);
        
        assertTrue(success, "诗歌音频应该生成成功");
        System.out.println("✅ 诗歌朗诵音频生成成功：" + new File(outputFile).getAbsolutePath());
    }

    /**
     * 测试5：生成提醒通知音频
     * 模拟真实的提醒场景
     */
    @Test
    void test05_GenerateReminderNotification() throws Exception {
        if (!isConfigured()) {
            System.out.println("⚠️  跳过测试：腾讯云密钥未配置");
            return;
        }

        System.out.println("\n========== 测试5：生成提醒通知 ==========");
        
        Files.createDirectories(Paths.get(OUTPUT_DIR));
        
        String[] reminders = {
            "您有一个会议即将开始，请及时参加。",
            "今天是您朋友的生日，记得送上祝福。",
            "明天有重要考试，请提前准备。"
        };
        
        for (int i = 0; i < reminders.length; i++) {
            String outputFile = OUTPUT_DIR + String.format("05-reminder-%d.mp3", i + 1);
            
            TtsRequest request = TtsRequest.builder()
                    .text(reminders[i])
                    .voiceType(0)  // 女声更适合提醒
                    .speed(0)
                    .volume(8)     // 音量稍大
                    .build();
            
            System.out.println("\n提醒 " + (i + 1) + "：" + reminders[i]);
            
            boolean success = tencentTtsService.textToVoiceAndSave(request, outputFile);
            
            assertTrue(success, "提醒" + (i + 1) + " 应该生成成功");
            System.out.println("✅ 生成成功：" + outputFile);
        }
    }

    /**
     * 测试文本长度验证
     */
    @Test
    void test06_ValidateTextLength() {
        System.out.println("\n========== 测试6：文本长度验证 ==========");
        
        // 正常长度
        assertTrue(tencentTtsService.validateTextLength("正常长度的文本"));
        System.out.println("✅ 正常长度验证通过");
        
        // 空文本
        assertFalse(tencentTtsService.validateTextLength(""));
        assertFalse(tencentTtsService.validateTextLength(null));
        System.out.println("✅ 空文本验证通过");
        
        // 超长文本
        String longText = "a".repeat(200);
        assertFalse(tencentTtsService.validateTextLength(longText));
        System.out.println("✅ 超长文本验证通过");
        
        // 边界值
        String boundaryText = "a".repeat(150);
        assertTrue(tencentTtsService.validateTextLength(boundaryText));
        System.out.println("✅ 边界值验证通过");
    }

    /**
     * 测试完成后显示输出目录
     */
    @Test
    void test99_ShowOutputDirectory() {
        System.out.println("\n========================================");
        System.out.println("📁 所有测试音频文件保存在：");
        System.out.println("   " + new File(OUTPUT_DIR).getAbsolutePath());
        System.out.println("========================================");
    }
}
