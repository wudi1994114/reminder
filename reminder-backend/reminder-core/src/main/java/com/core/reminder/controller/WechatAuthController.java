package com.core.reminder.controller;

import com.core.reminder.dto.WechatLoginRequest;
import com.core.reminder.dto.WechatLoginResponse;
import com.core.reminder.service.WechatAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信登录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/wechat")
public class WechatAuthController {

    @Autowired
    private WechatAuthService wechatAuthService;

    /**
     * 微信小程序登录
     * @param request 登录请求
     * @return 登录响应
     */
    @PostMapping("/login")
    public ResponseEntity<?> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        try {
            log.info("收到微信小程序登录请求，code: {}", request.getCode());
            
            WechatLoginResponse response = wechatAuthService.wechatLogin(request);
            
            log.info("微信小程序登录成功，用户ID: {}, 是否新用户: {}", 
                    response.getUserId(), response.getIsNewUser());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("微信小程序登录失败", e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "微信登录失败：" + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    /**
     * 获取微信登录状态
     * @return 登录状态信息
     */
    @GetMapping("/status")
    public ResponseEntity<?> getWechatLoginStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("wechatLoginEnabled", true);
        status.put("message", "微信小程序登录功能已启用");
        
        return ResponseEntity.ok(status);
    }
} 