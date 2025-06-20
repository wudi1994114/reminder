package com.core.reminder.controller;

import com.common.reminder.dto.UserProfileDto;
import com.common.reminder.model.UserFeedback;
import com.core.reminder.dto.UserFeedbackRequest;
import com.core.reminder.service.UserFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户反馈控制器
 */
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@Slf4j
public class UserFeedbackController {

    private final UserFeedbackService userFeedbackService;

    /**
     * 提交用户反馈
     *
     * @param request 反馈请求
     * @param currentUser 当前用户信息（可能为空，支持匿名反馈）
     * @return 响应结果
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitFeedback(
            @Valid @RequestBody UserFeedbackRequest request,
            @RequestAttribute(value = "currentUser", required = false) UserProfileDto currentUser) {
        try {
            log.info("收到用户反馈提交请求");

            // 从currentUser中获取用户ID（可能为空，支持匿名反馈）
            Long userId = currentUser != null ? currentUser.getId() : null;
            log.info("当前用户ID: {}, 邮箱: {}", userId, request.getEmail());

            UserFeedback feedback = userFeedbackService.submitFeedback(request, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "反馈提交成功，感谢您的宝贵意见！");
            response.put("feedbackId", feedback.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("提交反馈失败", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "提交失败，请稍后重试");

            return ResponseEntity.status(500).body(response);
        }
    }
}
