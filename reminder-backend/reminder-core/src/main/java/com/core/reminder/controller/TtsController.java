package com.core.reminder.controller;

import com.core.reminder.dto.TtsRequest;
import com.core.reminder.dto.TtsResponse;
import com.core.reminder.service.TencentTtsService;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文本转语音测试控制器
 * 
 * @author wudi
 * @since 2025-01-17
 */
@Slf4j
@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
public class TtsController {
    
    private final TencentTtsService tencentTtsService;
    
    /**
     * 测试接口 - 简单文本转语音
     * 
     * POST /api/tts/test
     * {
     *   "text": "你好，这是测试"
     * }
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testTts(@RequestBody Map<String, String> request) {
        log.info("收到TTS测试请求");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String text = request.get("text");
            if (text == null || text.isEmpty()) {
                response.put("success", false);
                response.put("message", "文本不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("文本内容: {}", text);
            
            // 调用TTS服务
            TtsResponse ttsResponse = tencentTtsService.textToVoice(text);
            
            if (ttsResponse.getSuccess()) {
                response.put("success", true);
                response.put("message", "转换成功");
                response.put("requestId", ttsResponse.getRequestId());
                response.put("audioSize", ttsResponse.getAudioSize());
                response.put("codec", ttsResponse.getCodec());
                // 注意：这里返回Base64编码的音频数据，前端需要解码后播放
                response.put("audioData", ttsResponse.getAudioData());
                
                log.info("TTS转换成功 - RequestId: {}, 音频大小: {} bytes", 
                        ttsResponse.getRequestId(), ttsResponse.getAudioSize());
            } else {
                response.put("success", false);
                response.put("message", "转换失败: " + ttsResponse.getErrorMessage());
                
                log.error("TTS转换失败: {}", ttsResponse.getErrorMessage());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (TencentCloudSDKException e) {
            log.error("TTS服务异常", e);
            response.put("success", false);
            response.put("message", "服务异常: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 高级测试接口 - 自定义参数
     * 
     * POST /api/tts/convert
     * {
     *   "text": "测试文本",
     *   "voiceType": 0,
     *   "speed": 0,
     *   "volume": 5,
     *   "codec": "mp3"
     * }
     */
    @PostMapping("/convert")
    public ResponseEntity<Map<String, Object>> convertTts(@RequestBody TtsRequest request) {
        log.info("收到TTS转换请求 - 文本长度: {}, 音色: {}, 语速: {}", 
                request.getText().length(), request.getVoiceType(), request.getSpeed());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证文本长度
            if (!tencentTtsService.validateTextLength(request.getText())) {
                response.put("success", false);
                response.put("message", "文本长度不合法（应在1-150字符之间）");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 调用TTS服务
            TtsResponse ttsResponse = tencentTtsService.textToVoice(request);
            
            if (ttsResponse.getSuccess()) {
                response.put("success", true);
                response.put("message", "转换成功");
                response.put("requestId", ttsResponse.getRequestId());
                response.put("audioSize", ttsResponse.getAudioSize());
                response.put("codec", ttsResponse.getCodec());
                response.put("audioData", ttsResponse.getAudioData());
                
                log.info("TTS转换成功 - RequestId: {}, 音频大小: {} bytes", 
                        ttsResponse.getRequestId(), ttsResponse.getAudioSize());
            } else {
                response.put("success", false);
                response.put("message", "转换失败: " + ttsResponse.getErrorMessage());
                
                log.error("TTS转换失败: {}", ttsResponse.getErrorMessage());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (TencentCloudSDKException e) {
            log.error("TTS服务异常", e);
            response.put("success", false);
            response.put("message", "服务异常: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 下载音频文件
     * 
     * POST /api/tts/download
     * {
     *   "text": "测试文本",
     *   "voiceType": 0
     * }
     * 
     * 直接返回音频文件流，可以直接下载或播放
     */
    @PostMapping("/download")
    public ResponseEntity<byte[]> downloadAudio(@RequestBody TtsRequest request) {
        log.info("收到音频下载请求 - 文本: {}", request.getText());
        
        try {
            // 调用TTS服务
            TtsResponse ttsResponse = tencentTtsService.textToVoice(request);
            
            if (!ttsResponse.getSuccess()) {
                log.error("TTS转换失败: {}", ttsResponse.getErrorMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            // 解码Base64音频数据
            byte[] audioBytes = Base64.getDecoder().decode(ttsResponse.getAudioData());
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentDispositionFormData("attachment", "tts-audio.mp3");
            headers.setContentLength(audioBytes.length);
            
            log.info("音频下载成功 - RequestId: {}, 大小: {} bytes", 
                    ttsResponse.getRequestId(), audioBytes.length);
            
            return new ResponseEntity<>(audioBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("音频下载失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取音频流（用于在线播放）- 增强版，支持停顿控制
     * 
     * GET /api/tts/stream?text=测试文本&voiceType=0&speed=0&autoPause=true
     * 
     * 返回音频流，可以直接在浏览器播放
     * 
     * 停顿控制方式：
     * 1. 使用标点符号（逗号、句号）自动产生停顿
     * 2. 在文本中使用 <break time="500ms"/> 精确控制停顿时长
     * 3. 使用 <break strength="medium"/> 控制停顿强度
     * 
     * 示例：
     * - 标点停顿: text=第一句。第二句，停顿。第三句
     * - 时间停顿: text=第一句<break time="1s"/>停1秒<break time="500ms"/>停0.5秒
     * - 强度停顿: text=第一句<break strength="medium"/>中等停顿
     * - 组合使用: text=提醒：<break time="800ms"/>您有会议<break strength="strong"/>请准时参加
     */
    @GetMapping("/stream")
    public ResponseEntity<byte[]> streamAudio(
            @RequestParam String text,
            @RequestParam(defaultValue = "0") Integer voiceType,
            @RequestParam(defaultValue = "0") Integer speed,
            @RequestParam(defaultValue = "false") Boolean autoPause,
            @RequestParam(required = false) Integer pauseMs) {
        
        log.info("收到音频流请求 - 文本: {}, 音色: {}, 自动停顿: {}", text, voiceType, autoPause);
        
        try {
            // 如果启用自动停顿，智能添加停顿标记
            String processedText = text;
            if (autoPause) {
                processedText = addAutoPause(text, pauseMs != null ? pauseMs : 500);
                log.info("自动添加停顿后的文本: {}", processedText);
            }
            
            TtsRequest request = TtsRequest.builder()
                    .text(processedText)
                    .voiceType(voiceType)
                    .speed(speed)
                    .build();
            
            TtsResponse ttsResponse = tencentTtsService.textToVoice(request);
            
            if (!ttsResponse.getSuccess()) {
                log.error("TTS转换失败: {}", ttsResponse.getErrorMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            byte[] audioBytes = Base64.getDecoder().decode(ttsResponse.getAudioData());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentLength(audioBytes.length);
            headers.setCacheControl("no-cache");
            
            log.info("音频流返回成功 - RequestId: {}, 大小: {} bytes", 
                    ttsResponse.getRequestId(), audioBytes.length);
            
            return new ResponseEntity<>(audioBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("音频流处理失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 智能添加停顿
     * 在句号、逗号、问号、感叹号后自动添加停顿
     */
    private String addAutoPause(String text, int pauseMs) {
        String result = text;
        
        // 在句号后添加停顿（如果没有已有的break标签）
        if (!result.contains("<break")) {
            result = result.replaceAll("。", String.format("。<break time=\"%dms\"/>", pauseMs));
            result = result.replaceAll("，", String.format("，<break time=\"%dms\"/>", pauseMs / 2));
            result = result.replaceAll("？", String.format("？<break time=\"%dms\"/>", pauseMs));
            result = result.replaceAll("！", String.format("！<break time=\"%dms\"/>", pauseMs));
            result = result.replaceAll("；", String.format("；<break time=\"%dms\"/>", pauseMs));
        }
        
        return result;
    }
    
    /**
     * 验证文本长度
     * 
     * GET /api/tts/validate?text=测试文本
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateText(@RequestParam String text) {
        Map<String, Object> response = new HashMap<>();
        
        boolean valid = tencentTtsService.validateTextLength(text);
        
        response.put("valid", valid);
        response.put("length", text.length());
        response.put("maxLength", 150);
        response.put("message", valid ? "文本长度合法" : "文本长度超出限制（最大150字符）");
        
        log.info("文本验证 - 长度: {}, 结果: {}", text.length(), valid);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 分段处理长文本
     * 
     * POST /api/tts/split
     * {
     *   "text": "很长的文本...",
     *   "maxLength": 150
     * }
     */
    @PostMapping("/split")
    public ResponseEntity<Map<String, Object>> splitText(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        String text = (String) request.get("text");
        Integer maxLength = request.get("maxLength") != null 
                ? (Integer) request.get("maxLength") 
                : 150;
        
        List<String> segments = tencentTtsService.splitText(text, maxLength);
        
        response.put("success", true);
        response.put("totalLength", text.length());
        response.put("segmentCount", segments.size());
        response.put("segments", segments);
        
        log.info("文本分段 - 原长度: {}, 分段数: {}", text.length(), segments.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 测试停顿效果
     * 
     * GET /api/tts/pause-demo
     * 
     * 演示不同的停顿方式
     */
    @GetMapping("/pause-demo")
    public ResponseEntity<Map<String, Object>> pauseDemo() {
        Map<String, Object> response = new HashMap<>();
        
        // 提供多种停顿示例
        Map<String, String> examples = new HashMap<>();
        
        // 1. 使用标点符号（最简单）
        examples.put("punctuation", "这是第一句，停顿一下。这是第二句，再停顿一下。这是第三句。");
        
        // 2. 使用SSML的break标签 - 按时间停顿
        examples.put("ssml_time_500ms", "这是第一句<break time=\"500ms\"/>停顿500毫秒<break time=\"1s\"/>停顿1秒");
        examples.put("ssml_time_2s", "开始<break time=\"2s\"/>停顿2秒<break time=\"3s\"/>停顿3秒");
        
        // 3. 使用SSML的break标签 - 按强度停顿
        examples.put("ssml_strength_weak", "第一句<break strength=\"weak\"/>弱停顿，相当于逗号");
        examples.put("ssml_strength_medium", "第一句<break strength=\"medium\"/>中等停顿，相当于句号");
        examples.put("ssml_strength_strong", "第一句<break strength=\"strong\"/>强停顿，相当于段落");
        
        // 4. 组合使用
        examples.put("combined", "欢迎使用提醒助手。<break time=\"1s\"/>今天是个好日子<break strength=\"medium\"/>祝你工作顺利。");
        
        // 5. 实际应用场景
        examples.put("reminder_example", "提醒：<break time=\"500ms\"/>您有一个会议<break time=\"1s\"/>时间是下午3点<break strength=\"medium\"/>请准时参加。");
        
        response.put("success", true);
        response.put("message", "停顿控制示例");
        response.put("examples", examples);
        response.put("tips", Map.of(
            "1", "最简单：使用标点符号（逗号、句号）自动产生停顿",
            "2", "精确控制：使用SSML的<break>标签",
            "3", "时间停顿：<break time=\"500ms\"/> 或 <break time=\"1s\"/>",
            "4", "强度停顿：<break strength=\"weak/medium/strong\"/>",
            "5", "测试方法：将examples中的文本复制到 /api/tts/stream 接口测试"
        ));
        
        log.info("返回停顿控制示例");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 快速测试停顿
     * 
     * GET /api/tts/test-pause?pauseMs=1000
     * 
     * 快速测试指定时长的停顿效果
     */
    @GetMapping("/test-pause")
    public ResponseEntity<byte[]> testPause(
            @RequestParam(defaultValue = "1000") Integer pauseMs) {
        
        log.info("测试停顿效果 - 停顿时长: {}ms", pauseMs);
        
        try {
            // 构建带停顿的文本
            String text = String.format(
                "这是第一句话<break time=\"%dms\"/>停顿了%d毫秒<break time=\"%dms\"/>这是最后一句话",
                pauseMs, pauseMs, pauseMs
            );
            
            TtsRequest request = TtsRequest.builder()
                    .text(text)
                    .voiceType(0)
                    .speed(0)
                    .build();
            
            TtsResponse ttsResponse = tencentTtsService.textToVoice(request);
            
            if (!ttsResponse.getSuccess()) {
                log.error("TTS转换失败: {}", ttsResponse.getErrorMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            byte[] audioBytes = Base64.getDecoder().decode(ttsResponse.getAudioData());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentLength(audioBytes.length);
            
            log.info("停顿测试成功 - 停顿: {}ms, 音频大小: {} bytes", pauseMs, audioBytes.length);
            
            return new ResponseEntity<>(audioBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("停顿测试失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 提醒通知模板测试
     * 
     * GET /api/tts/reminder-template?title=会议&time=下午3点
     * 
     * 测试提醒通知的停顿效果
     */
    @GetMapping("/reminder-template")
    public ResponseEntity<byte[]> reminderTemplate(
            @RequestParam String title,
            @RequestParam String time) {
        
        log.info("生成提醒通知 - 标题: {}, 时间: {}", title, time);
        
        try {
            // 构建提醒模板（带合理停顿）
            String text = String.format(
                "提醒通知<break time=\"800ms\"/>您有一个%s<break time=\"1s\"/>时间是%s<break time=\"500ms\"/>请准时参加",
                title, time
            );
            
            TtsRequest request = TtsRequest.builder()
                    .text(text)
                    .voiceType(0) // 使用女声
                    .speed(0)
                    .volume(7) // 音量稍大
                    .build();
            
            TtsResponse ttsResponse = tencentTtsService.textToVoice(request);
            
            if (!ttsResponse.getSuccess()) {
                log.error("提醒通知生成失败: {}", ttsResponse.getErrorMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            byte[] audioBytes = Base64.getDecoder().decode(ttsResponse.getAudioData());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentLength(audioBytes.length);
            
            log.info("提醒通知生成成功 - 音频大小: {} bytes", audioBytes.length);
            
            return new ResponseEntity<>(audioBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("提醒通知生成失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 健康检查
     * 
     * GET /api/tts/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "TTS文本转语音服务");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}

