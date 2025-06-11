package com.core.reminder.controller;

import com.common.reminder.dto.UserProfileDto;
import com.core.reminder.dto.UserPreferenceDto;
import com.core.reminder.service.UserPreferenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 用户偏好设置控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user/preferences")
public class UserPreferenceController {

    @Autowired
    private UserPreferenceService userPreferenceService;

    /**
     * 获取用户偏好设置
     * @param currentUser 当前用户信息
     * @return 用户偏好设置
     */
    @GetMapping
    public ResponseEntity<UserPreferenceDto> getUserPreference(
            @RequestAttribute("currentUser") UserProfileDto currentUser) {
        try {
            Long userId = currentUser.getId();
            log.info("获取用户偏好设置请求, userId: {}", userId);
            
            UserPreferenceDto preference = userPreferenceService.getUserPreference(userId);
            return ResponseEntity.ok(preference);
            
        } catch (Exception e) {
            log.error("获取用户偏好设置失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 更新用户偏好设置
     * @param currentUser 当前用户信息
     * @param preferenceDto 偏好设置数据
     * @return 更新后的偏好设置
     */
    @PutMapping
    public ResponseEntity<UserPreferenceDto> updateUserPreference(
            @RequestAttribute("currentUser") UserProfileDto currentUser,
            @Valid @RequestBody UserPreferenceDto preferenceDto) {
        try {
            Long userId = currentUser.getId();
            log.info("更新用户偏好设置请求, userId: {}", userId);
            
            UserPreferenceDto updatedPreference = userPreferenceService.updateUserPreference(userId, preferenceDto);
            return ResponseEntity.ok(updatedPreference);
            
        } catch (Exception e) {
            log.error("更新用户偏好设置失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 重置用户偏好设置为默认值
     * @param currentUser 当前用户信息
     * @return 重置后的偏好设置
     */
    @PostMapping("/reset")
    public ResponseEntity<UserPreferenceDto> resetUserPreference(
            @RequestAttribute("currentUser") UserProfileDto currentUser) {
        try {
            Long userId = currentUser.getId();
            log.info("重置用户偏好设置请求, userId: {}", userId);
            
            // 使用一个空的DTO来重置为默认值
            UserPreferenceDto defaultDto = new UserPreferenceDto();
            UserPreferenceDto resetPreference = userPreferenceService.updateUserPreference(userId, defaultDto);
            return ResponseEntity.ok(resetPreference);
            
        } catch (Exception e) {
            log.error("重置用户偏好设置失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 