package com.task.reminder.service;

import com.common.reminder.model.UserPreference;
import com.task.reminder.repository.UserPreferenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 用户偏好设置服务 - Job模块专用
 * 用于在job执行过程中处理用户偏好设置，特别是微信授权次数统计
 */
@Slf4j
@Service
public class UserPreferenceJobService {

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;
    
    private static final String WECHAT_AUTH_COUNT_KEY = "wechatAuthCount";

    /**
     * 获取用户微信授权剩余次数
     * @param userId 用户ID
     * @return 剩余次数
     */
    public Integer getWechatAuthCount(Long userId) {
        String value = getUserPreferenceValue(userId, WECHAT_AUTH_COUNT_KEY, "0");
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            log.warn("无效的微信授权次数: {}, 使用0作为默认值", value);
            return 0;
        }
    }

    /**
     * 减少用户微信授权次数
     * @param userId 用户ID
     * @param count 减少的次数
     * @return 是否成功减少
     */
    @Transactional
    public boolean decreaseWechatAuthCount(Long userId, Integer count) {
        Integer currentCount = getWechatAuthCount(userId);
        if (currentCount < count) {
            log.warn("用户ID[{}]微信授权次数不足，当前:{}, 尝试减少:{}", userId, currentCount, count);
            return false;
        }
        Integer newCount = currentCount - count;
        setUserPreference(userId, WECHAT_AUTH_COUNT_KEY, newCount.toString());
        log.info("用户ID[{}]微信授权次数已减少{}，剩余:{}", userId, count, newCount);
        return true;
    }

    /**
     * 获取用户指定键的偏好设置值
     * @param userId 用户ID
     * @param key 偏好设置键名
     * @param defaultValue 默认值
     * @return 偏好设置值
     */
    private String getUserPreferenceValue(Long userId, String key, String defaultValue) {
        Optional<UserPreference> preference = userPreferenceRepository.findByUserIdAndKey(userId, key);
        return preference.map(UserPreference::getValue).orElse(defaultValue);
    }

    /**
     * 设置用户偏好设置
     * @param userId 用户ID
     * @param key 偏好设置键名
     * @param value 偏好设置值
     */
    @Transactional
    private void setUserPreference(Long userId, String key, String value) {
        UserPreference preference = userPreferenceRepository.findByUserIdAndKey(userId, key)
                .orElse(new UserPreference(userId, key, null));
        preference.setValue(value);
        userPreferenceRepository.save(preference);
    }
} 