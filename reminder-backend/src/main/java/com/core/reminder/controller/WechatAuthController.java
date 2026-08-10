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
     * 新增：微信云托管登录接口
     * @param openId 从微信云托管网关获取的 openid
     * @param request 包含可选的用户信息
     * @return 登录响应
     */
    @PostMapping("/cloud-login")
    public ResponseEntity<?> wechatCloudLogin(
            @RequestHeader("X-WX-OPENID") String openId,
            @RequestBody(required = false) WechatLoginRequest request) {
        try {
            log.info("收到微信云托管登录请求，X-WX-OPENID: {}", openId);
            
            // 如果request为空，创建一个新的实例以传递openid
            WechatLoginRequest loginRequest = (request != null) ? request : new WechatLoginRequest();
            
            WechatLoginResponse response = wechatAuthService.cloudLogin(openId, loginRequest.getUserInfo());
            
            log.info("微信云托管登录成功，用户ID: {}, 是否新用户: {}", 
                    response.getUserId(), response.getIsNewUser());

            if (response == null || response.getAccessToken() == null || response.getAccessToken().trim().isEmpty() || response.getUserId() == null) {
                log.error("微信云托管登录服务返回无效响应");
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "云托管登录服务内部错误");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("微信云托管登录失败", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "微信云托管登录失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

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
            
            // 确保响应对象不为null且包含必要字段
            if (response == null) {
                log.error("微信登录服务返回null响应");
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "登录服务内部错误");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
            
            // 验证响应的关键字段
            if (response.getAccessToken() == null || response.getAccessToken().trim().isEmpty()) {
                log.error("微信登录响应缺少访问令牌");
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "登录失败：无法生成访问令牌");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
            
            if (response.getUserId() == null) {
                log.error("微信登录响应缺少用户ID");
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "登录失败：无法获取用户信息");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("微信小程序登录失败", e);
            
            // 安全地处理异常消息
            String errorMessage = "微信登录失败";
            if (e.getMessage() != null && !e.getMessage().trim().isEmpty()) {
                errorMessage = "微信登录失败：" + e.getMessage();
            }
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", errorMessage);
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    /**
     * 获取微信登录状态
     * @return 登录状态信息
     */
    @GetMapping("/status")
    public ResponseEntity<?> getWechatLoginStatus() {
        try {
            // 返回微信登录配置状态
            Map<String, Object> status = new HashMap<>();
            status.put("service", "微信小程序登录服务");
            status.put("status", "运行中");
            status.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("获取微信登录状态失败", e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "获取状态失败：" + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 调试接口：查看微信用户unionid存储状况
     * 注意：此接口仅用于开发调试，生产环境应移除或添加权限控制
     */
    @GetMapping("/debug/unionid-status")
    public ResponseEntity<?> getUnionidStatus() {
        try {
            // 获取所有微信用户的unionid状态统计
            Map<String, Object> stats = wechatAuthService.getUnionidStats();
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("获取unionid状态失败", e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "获取unionid状态失败：" + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 手动更新微信用户信息到AppUser表
     * @param request 包含用户信息的请求
     * @return 更新结果
     */
    @PostMapping("/update-profile")
    public ResponseEntity<?> updateWechatProfile(@Valid @RequestBody WechatLoginRequest request) {
        try {
            log.info("收到手动更新微信用户信息请求");
            
            if (request.getUserInfo() == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "用户信息不能为空");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 这里需要从JWT token中获取当前用户ID
            // 为简化，先返回操作指导
            Map<String, String> response = new HashMap<>();
            response.put("message", "接口已就绪，需要集成JWT认证以获取当前用户ID");
            response.put("note", "请在前端调用 /auth/wechat/login 进行完整的登录流程");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("手动更新微信用户信息失败", e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "更新失败：" + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
} 