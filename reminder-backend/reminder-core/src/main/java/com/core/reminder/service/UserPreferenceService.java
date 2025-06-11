package com.core.reminder.service;

import com.common.reminder.model.ReminderType;
import com.common.reminder.model.UserPreference;
import com.core.reminder.dto.UserPreferenceDto;
import com.core.reminder.repository.UserPreferenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 用户偏好设置服务
 */
@Slf4j
@Service
public class UserPreferenceService {

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    /**
     * 获取用户偏好设置
     * @param userId 用户ID
     * @return 用户偏好设置DTO
     */
    public UserPreferenceDto getUserPreference(Long userId) {
        log.info("获取用户偏好设置, userId: {}", userId);
        
        UserPreference userPreference = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(userId));
        
        return convertToDto(userPreference);
    }

    /**
     * 更新用户偏好设置
     * @param userId 用户ID
     * @param preferenceDto 偏好设置DTO
     * @return 更新后的偏好设置DTO
     */
    @Transactional
    public UserPreferenceDto updateUserPreference(Long userId, UserPreferenceDto preferenceDto) {
        log.info("更新用户偏好设置, userId: {}", userId);
        
        UserPreference userPreference = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(userId));
        
        // 更新字段
        if (preferenceDto.getDefaultReminderType() != null) {
            userPreference.setDefaultReminderType(preferenceDto.getDefaultReminderType());
        }
        if (preferenceDto.getEmailNotificationEnabled() != null) {
            userPreference.setEmailNotificationEnabled(preferenceDto.getEmailNotificationEnabled());
        }
        if (preferenceDto.getSmsNotificationEnabled() != null) {
            userPreference.setSmsNotificationEnabled(preferenceDto.getSmsNotificationEnabled());
        }
        if (preferenceDto.getWechatNotificationEnabled() != null) {
            userPreference.setWechatNotificationEnabled(preferenceDto.getWechatNotificationEnabled());
        }
        if (preferenceDto.getDefaultAdvanceMinutes() != null) {
            userPreference.setDefaultAdvanceMinutes(preferenceDto.getDefaultAdvanceMinutes());
        }
        if (preferenceDto.getSoundEnabled() != null) {
            userPreference.setSoundEnabled(preferenceDto.getSoundEnabled());
        }
        if (preferenceDto.getVibrationEnabled() != null) {
            userPreference.setVibrationEnabled(preferenceDto.getVibrationEnabled());
        }
        if (preferenceDto.getTimezone() != null) {
            userPreference.setTimezone(preferenceDto.getTimezone());
        }
        if (preferenceDto.getLanguage() != null) {
            userPreference.setLanguage(preferenceDto.getLanguage());
        }
        if (preferenceDto.getTheme() != null) {
            userPreference.setTheme(preferenceDto.getTheme());
        }
        if (preferenceDto.getDailySummaryTime() != null) {
            userPreference.setDailySummaryTime(preferenceDto.getDailySummaryTime());
        }
        if (preferenceDto.getDailySummaryEnabled() != null) {
            userPreference.setDailySummaryEnabled(preferenceDto.getDailySummaryEnabled());
        }
        if (preferenceDto.getWeekendNotificationEnabled() != null) {
            userPreference.setWeekendNotificationEnabled(preferenceDto.getWeekendNotificationEnabled());
        }
        if (preferenceDto.getQuietHoursStart() != null) {
            userPreference.setQuietHoursStart(preferenceDto.getQuietHoursStart());
        }
        if (preferenceDto.getQuietHoursEnd() != null) {
            userPreference.setQuietHoursEnd(preferenceDto.getQuietHoursEnd());
        }
        if (preferenceDto.getQuietHoursEnabled() != null) {
            userPreference.setQuietHoursEnabled(preferenceDto.getQuietHoursEnabled());
        }
        
        userPreference.setUpdatedAt(OffsetDateTime.now());
        UserPreference savedPreference = userPreferenceRepository.save(userPreference);
        
        log.info("用户偏好设置更新成功, userId: {}", userId);
        return convertToDto(savedPreference);
    }

    /**
     * 创建默认偏好设置
     * @param userId 用户ID
     * @return 默认偏好设置
     */
    private UserPreference createDefaultPreference(Long userId) {
        log.info("为用户创建默认偏好设置, userId: {}", userId);
        
        UserPreference preference = new UserPreference();
        preference.setUserId(userId);
        preference.setDefaultReminderType(ReminderType.EMAIL);
        preference.setEmailNotificationEnabled(true);
        preference.setSmsNotificationEnabled(false);
        preference.setWechatNotificationEnabled(false);
        preference.setDefaultAdvanceMinutes(15);
        preference.setSoundEnabled(true);
        preference.setVibrationEnabled(true);
        preference.setTimezone("Asia/Shanghai");
        preference.setLanguage("zh-CN");
        preference.setTheme("light");
        preference.setDailySummaryTime("08:00");
        preference.setDailySummaryEnabled(false);
        preference.setWeekendNotificationEnabled(true);
        preference.setQuietHoursStart("22:00");
        preference.setQuietHoursEnd("07:00");
        preference.setQuietHoursEnabled(false);
        preference.setCreatedAt(OffsetDateTime.now());
        preference.setUpdatedAt(OffsetDateTime.now());
        
        return userPreferenceRepository.save(preference);
    }

    /**
     * 转换为DTO
     * @param userPreference 用户偏好设置实体
     * @return DTO
     */
    private UserPreferenceDto convertToDto(UserPreference userPreference) {
        UserPreferenceDto dto = new UserPreferenceDto();
        BeanUtils.copyProperties(userPreference, dto);
        return dto;
    }

    /**
     * 检查用户是否启用了某种通知类型
     * @param userId 用户ID
     * @param reminderType 通知类型
     * @return 是否启用
     */
    public boolean isNotificationEnabled(Long userId, ReminderType reminderType) {
        UserPreference preference = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(userId));
        
        switch (reminderType) {
            case EMAIL:
                return preference.getEmailNotificationEnabled();
            case SMS:
                return preference.getSmsNotificationEnabled();
            case WECHAT_MINI:
                return preference.getWechatNotificationEnabled();
            default:
                return false;
        }
    }

    /**
     * 获取用户的默认通知类型
     * @param userId 用户ID
     * @return 默认通知类型
     */
    public ReminderType getDefaultReminderType(Long userId) {
        UserPreference preference = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(userId));
        
        return preference.getDefaultReminderType();
    }
} 