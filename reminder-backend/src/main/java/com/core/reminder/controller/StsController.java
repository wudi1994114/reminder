package com.core.reminder.controller;

import com.common.reminder.dto.UserProfileDto;
import com.core.reminder.dto.TencentCredentialsDto;
import com.core.reminder.service.TencentStsService;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * STS临时密钥控制器
 * 
 * @author wudi
 * @since 2025-06-22
 */
@Slf4j
@RestController
@RequestMapping("/api/sts")
@RequiredArgsConstructor
public class StsController {
    
    private final TencentStsService tencentStsService;
    
    /**
     * 获取语音识别临时凭证
     * 
     * @param currentUser 当前用户信息
     * @return 临时凭证信息
     */
    @GetMapping("/credentials")
    public ResponseEntity<Map<String, Object>> getCredentials(
            @RequestAttribute("currentUser") UserProfileDto currentUser) {
        
        Map<String, Object> responseMap = new HashMap<>();
        
        try {
            log.info("用户 {} 请求语音识别临时凭证", currentUser.getId());
            
            // 获取临时凭证
            TencentCredentialsDto credentials = tencentStsService.getSpeechRecognitionCredentials(currentUser.getId());
            
            // 构造返回数据
            responseMap.put("success", true);
            responseMap.put("message", "获取临时凭证成功");
            responseMap.put("data", credentials);
            
            return ResponseEntity.ok(responseMap);
            
        } catch (TencentCloudSDKException e) {
            log.error("获取临时凭证失败: {}", e.getMessage(), e);
            responseMap.put("success", false);
            responseMap.put("message", "获取临时凭证失败: " + e.getMessage());
            responseMap.put("error", e.toString());
            
            return ResponseEntity.status(500).body(responseMap);
        } catch (Exception e) {
            log.error("获取临时凭证时发生未知错误: {}", e.getMessage(), e);
            responseMap.put("success", false);
            responseMap.put("message", "服务器内部错误");
            
            return ResponseEntity.status(500).body(responseMap);
        }
    }
    
    /**
     * 获取语音识别临时凭证（简化版本，直接返回凭证信息）
     * 
     * @param currentUser 当前用户信息
     * @return 临时凭证信息
     */
    @GetMapping("/speech-credentials")
    public ResponseEntity<TencentCredentialsDto> getSpeechCredentials(
            @RequestAttribute("currentUser") UserProfileDto currentUser) {
        
        try {
            log.info("用户 {} 请求语音识别临时凭证（简化版本）", currentUser.getId());
            
            // 获取临时凭证
            TencentCredentialsDto credentials = tencentStsService.getSpeechRecognitionCredentials(currentUser.getId());
            
            return ResponseEntity.ok(credentials);
            
        } catch (TencentCloudSDKException e) {
            log.error("获取临时凭证失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        } catch (Exception e) {
            log.error("获取临时凭证时发生未知错误: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 健康检查接口
     * 
     * @return 服务状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "STS临时密钥服务");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}
