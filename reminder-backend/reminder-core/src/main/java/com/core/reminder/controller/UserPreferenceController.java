package com.core.reminder.controller;

import com.common.reminder.dto.UserProfileDto;
import com.core.reminder.dto.UserPreferenceDto;
import com.core.reminder.service.UserPreferenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;

/**
 * 用户偏好设置控制器 - 键值对存储模式
 */
@Slf4j
@RestController
@RequestMapping("/api/user/preferences")
public class UserPreferenceController {

    @Autowired
    private UserPreferenceService userPreferenceService;

    /**
     * 获取用户所有偏好设置
     * @param currentUser 当前用户信息
     * @return 用户所有偏好设置
     */
    @GetMapping
    public ResponseEntity<UserPreferenceDto.UserPreferencesDto> getAllUserPreferences(
            @RequestAttribute("currentUser") UserProfileDto currentUser) {
        try {
            Long userId = currentUser.getId();
            log.info("获取用户所有偏好设置请求, userId: {}", userId);
            
            UserPreferenceDto.UserPreferencesDto preferences = userPreferenceService.getUserAllPreferences(userId);
            return ResponseEntity.ok(preferences);
            
        } catch (Exception e) {
            log.error("获取用户所有偏好设置失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取用户指定键的偏好设置
     * @param currentUser 当前用户信息
     * @param key 偏好设置键名
     * @return 偏好设置
     */
    @GetMapping("/{key}")
    public ResponseEntity<UserPreferenceDto> getUserPreference(
            @RequestAttribute("currentUser") UserProfileDto currentUser,
            @PathVariable String key) {
        try {
            Long userId = currentUser.getId();
            log.info("获取用户偏好设置请求, userId: {}, key: {}", userId, key);
            
            Optional<UserPreferenceDto> preference = userPreferenceService.getUserPreference(userId, key);
            
            if (preference.isPresent()) {
                return ResponseEntity.ok(preference.get());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("获取用户偏好设置失败, key: {}", key, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 设置用户偏好设置
     * @param currentUser 当前用户信息
     * @param key 偏好设置键名
     * @param preferenceDto 偏好设置数据
     * @return 设置后的偏好设置
     */
    @PutMapping("/{key}")
    public ResponseEntity<UserPreferenceDto> setUserPreference(
            @RequestAttribute("currentUser") UserProfileDto currentUser,
            @PathVariable String key,
            @Valid @RequestBody UserPreferenceDto preferenceDto) {
        try {
            Long userId = currentUser.getId();
            log.info("设置用户偏好设置请求, userId: {}, key: {}", userId, key);
            
            // 从路径中获取key，覆盖请求体中的key（防止不一致）
            UserPreferenceDto updatedPreference = userPreferenceService.setUserPreference(
                    userId, key, preferenceDto.getValue(), preferenceDto.getProperty());
            
            return ResponseEntity.ok(updatedPreference);
            
        } catch (IllegalArgumentException e) {
            log.error("用户偏好设置验证失败, key: {}, error: {}", key, e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            log.error("设置用户偏好设置失败, key: {}", key, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 批量更新用户偏好设置
     * @param currentUser 当前用户信息
     * @param batchUpdateDto 批量更新数据
     * @return 更新后的所有偏好设置
     */
    @PutMapping("/batch")
    public ResponseEntity<UserPreferenceDto.UserPreferencesDto> batchUpdateUserPreferences(
            @RequestAttribute("currentUser") UserProfileDto currentUser,
            @Valid @RequestBody UserPreferenceDto.BatchUpdateDto batchUpdateDto) {
        try {
            Long userId = currentUser.getId();
            log.info("批量更新用户偏好设置请求, userId: {}", userId);
            
            UserPreferenceDto.UserPreferencesDto updatedPreferences = 
                    userPreferenceService.batchUpdateUserPreferences(userId, batchUpdateDto);
            
            return ResponseEntity.ok(updatedPreferences);
            
        } catch (Exception e) {
            log.error("批量更新用户偏好设置失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除用户指定键的偏好设置
     * @param currentUser 当前用户信息
     * @param key 偏好设置键名
     * @return 删除结果
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteUserPreference(
            @RequestAttribute("currentUser") UserProfileDto currentUser,
            @PathVariable String key) {
        try {
            Long userId = currentUser.getId();
            log.info("删除用户偏好设置请求, userId: {}, key: {}", userId, key);
            
            userPreferenceService.deleteUserPreference(userId, key);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            log.error("删除用户偏好设置失败, key: {}", key, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除用户所有偏好设置
     * @param currentUser 当前用户信息
     * @return 删除结果
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAllUserPreferences(
            @RequestAttribute("currentUser") UserProfileDto currentUser) {
        try {
            Long userId = currentUser.getId();
            log.info("删除用户所有偏好设置请求, userId: {}", userId);
            
            userPreferenceService.deleteAllUserPreferences(userId);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            log.error("删除用户所有偏好设置失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 初始化用户默认偏好设置
     * @param currentUser 当前用户信息
     * @return 初始化后的偏好设置
     */
    @PostMapping("/initialize")
    public ResponseEntity<UserPreferenceDto.UserPreferencesDto> initializeDefaultPreferences(
            @RequestAttribute("currentUser") UserProfileDto currentUser) {
        try {
            Long userId = currentUser.getId();
            log.info("初始化用户默认偏好设置请求, userId: {}", userId);
            
            UserPreferenceDto.UserPreferencesDto initializedPreferences = 
                    userPreferenceService.initializeDefaultPreferences(userId);
            
            return ResponseEntity.ok(initializedPreferences);
            
        } catch (Exception e) {
            log.error("初始化用户默认偏好设置失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 重置用户偏好设置为默认值（删除所有现有设置后重新初始化）
     * @param currentUser 当前用户信息
     * @return 重置后的偏好设置
     */
    @PostMapping("/reset")
    public ResponseEntity<UserPreferenceDto.UserPreferencesDto> resetUserPreferences(
            @RequestAttribute("currentUser") UserProfileDto currentUser) {
        try {
            Long userId = currentUser.getId();
            log.info("重置用户偏好设置请求, userId: {}", userId);
            
            // 先删除所有现有设置
            userPreferenceService.deleteAllUserPreferences(userId);
            
            // 然后初始化默认设置
            UserPreferenceDto.UserPreferencesDto resetPreferences = 
                    userPreferenceService.initializeDefaultPreferences(userId);
            
            return ResponseEntity.ok(resetPreferences);
            
        } catch (Exception e) {
            log.error("重置用户偏好设置失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 