package com.core.reminder.service;

import com.common.reminder.model.ReminderType;
import com.common.reminder.model.UserPreference;
import com.core.reminder.dto.UserPreferenceDto;
import com.core.reminder.enums.UserPreferenceKey;
import com.core.reminder.repository.UserPreferenceRepository;
import com.core.reminder.utils.UserTagValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户偏好设置服务 - 键值对存储模式
 */
@Slf4j
@Service
public class UserPreferenceService {

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    /**
     * 获取用户的所有偏好设置
     * @param userId 用户ID
     * @return 用户偏好设置批量DTO
     */
    public UserPreferenceDto.UserPreferencesDto getUserAllPreferences(Long userId) {
        log.info("获取用户所有偏好设置, userId: {}", userId);
        
        List<UserPreference> preferences = userPreferenceRepository.findByUserId(userId);
        
        Map<String, String> preferenceMap = preferences.stream()
                .collect(Collectors.toMap(
                    UserPreference::getKey, 
                    pref -> pref.getValue() != null ? pref.getValue() : "",
                    (existing, replacement) -> replacement
                ));
        
        List<UserPreferenceDto> details = preferences.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        OffsetDateTime lastModified = preferences.stream()
                .map(UserPreference::getModifyAt)
                .max(OffsetDateTime::compareTo)
                .orElse(null);
        
        UserPreferenceDto.UserPreferencesDto result = new UserPreferenceDto.UserPreferencesDto();
        result.setUserId(userId);
        result.setPreferences(preferenceMap);
        result.setDetails(details);
        result.setTotal(preferences.size());
        result.setLastModified(lastModified);
        
        log.info("获取用户偏好设置成功, userId: {}, 共{}项设置", userId, preferences.size());
        return result;
    }

    /**
     * 获取用户指定键的偏好设置
     * @param userId 用户ID
     * @param key 偏好设置键名
     * @return 偏好设置DTO
     */
    public Optional<UserPreferenceDto> getUserPreference(Long userId, String key) {
        log.info("获取用户偏好设置, userId: {}, key: {}", userId, key);
        
        Optional<UserPreference> preference = userPreferenceRepository.findByUserIdAndKey(userId, key);
        
        if (preference.isPresent()) {
            return Optional.of(convertToDto(preference.get()));
        } else {
            log.debug("用户偏好设置不存在, userId: {}, key: {}", userId, key);
            return Optional.empty();
        }
    }

    /**
     * 获取用户指定键的偏好设置值
     * @param userId 用户ID
     * @param key 偏好设置键名
     * @param defaultValue 默认值
     * @return 偏好设置值
     */
    public String getUserPreferenceValue(Long userId, String key, String defaultValue) {
        Optional<UserPreference> preference = userPreferenceRepository.findByUserIdAndKey(userId, key);
        return preference.map(UserPreference::getValue).orElse(defaultValue);
    }

    /**
     * 设置用户偏好设置
     * @param userId 用户ID
     * @param key 偏好设置键名
     * @param value 偏好设置值
     * @param property 偏好设置描述（可选）
     * @return 保存后的偏好设置DTO
     */
    @Transactional
    public UserPreferenceDto setUserPreference(Long userId, String key, String value, String property) {
        log.info("设置用户偏好设置, userId: {}, key: {}, value: {}", userId, key, value);

        // 特殊处理用户标签列表的验证
        if (UserPreferenceKey.USER_TAG_LIST.getKey().equals(key)) {
            UserTagValidator.ValidationResult validationResult = UserTagValidator.validateTagList(value);
            if (!validationResult.isValid()) {
                throw new IllegalArgumentException("用户标签验证失败: " + validationResult.getMessage());
            }
            // 格式化标签列表
            value = UserTagValidator.formatTagList(value);
            log.info("用户标签列表验证通过并格式化, userId: {}, 格式化后的标签: {}", userId, value);
        }

        UserPreference preference = userPreferenceRepository.findByUserIdAndKey(userId, key)
                .orElse(new UserPreference(userId, key, null));

        preference.setValue(value);
        if (StringUtils.hasText(property)) {
            preference.setProperty(property);
        }

        UserPreference savedPreference = userPreferenceRepository.save(preference);

        log.info("用户偏好设置保存成功, userId: {}, key: {}", userId, key);
        return convertToDto(savedPreference);
    }



    public static void main(String[] args) {
        System.out.println(UserTagValidator.validateTagList("123|312|-|1234|321"));
    }

    /**
     * 设置用户偏好设置（重载方法）
     * @param userId 用户ID
     * @param key 偏好设置键名
     * @param value 偏好设置值
     * @return 保存后的偏好设置DTO
     */
    @Transactional
    public UserPreferenceDto setUserPreference(Long userId, String key, String value) {
        return setUserPreference(userId, key, value, null);
    }

    /**
     * 批量更新用户偏好设置
     * @param userId 用户ID
     * @param batchUpdateDto 批量更新DTO
     * @return 更新后的用户偏好设置批量DTO
     */
    @Transactional
    public UserPreferenceDto.UserPreferencesDto batchUpdateUserPreferences(Long userId, UserPreferenceDto.BatchUpdateDto batchUpdateDto) {
        log.info("批量更新用户偏好设置, userId: {}, 设置数量: {}, 覆盖模式: {}", 
                userId, batchUpdateDto.getPreferences().size(), batchUpdateDto.getOverride());
        
        if (Boolean.TRUE.equals(batchUpdateDto.getOverride())) {
            // 覆盖模式：先删除所有现有设置
            userPreferenceRepository.deleteByUserId(userId);
            log.info("覆盖模式：已删除用户所有现有偏好设置, userId: {}", userId);
        }
        
        // 批量保存新的偏好设置
        for (Map.Entry<String, String> entry : batchUpdateDto.getPreferences().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            UserPreference preference = userPreferenceRepository.findByUserIdAndKey(userId, key)
                    .orElse(new UserPreference(userId, key, null));
            
            preference.setValue(value);
            userPreferenceRepository.save(preference);
        }
        
        log.info("批量更新用户偏好设置完成, userId: {}", userId);
        return getUserAllPreferences(userId);
    }

    /**
     * 删除用户指定键的偏好设置
     * @param userId 用户ID
     * @param key 偏好设置键名
     */
    @Transactional
    public void deleteUserPreference(Long userId, String key) {
        log.info("删除用户偏好设置, userId: {}, key: {}", userId, key);
        userPreferenceRepository.deleteByUserIdAndKey(userId, key);
        log.info("删除用户偏好设置成功, userId: {}, key: {}", userId, key);
    }

    /**
     * 删除用户所有偏好设置
     * @param userId 用户ID
     */
    @Transactional
    public void deleteAllUserPreferences(Long userId) {
        log.info("删除用户所有偏好设置, userId: {}", userId);
        userPreferenceRepository.deleteByUserId(userId);
        log.info("删除用户所有偏好设置成功, userId: {}", userId);
    }

    /**
     * 检查用户是否启用了某种通知类型
     * @param userId 用户ID
     * @param reminderType 通知类型
     * @return 是否启用
     */
    public boolean isNotificationEnabled(Long userId, ReminderType reminderType) {
        String key;
        switch (reminderType) {
            case EMAIL:
                key = UserPreferenceKey.EMAIL_NOTIFICATION_ENABLED.getKey();
                break;
            case SMS:
                key = UserPreferenceKey.SMS_NOTIFICATION_ENABLED.getKey();
                break;
            case WECHAT_MINI:
                key = UserPreferenceKey.WECHAT_NOTIFICATION_ENABLED.getKey();
                break;
            default:
                return false;
        }

        String value = getUserPreferenceValue(userId, key, "true");
        return Boolean.parseBoolean(value);
    }

    /**
     * 获取用户的默认通知类型
     * @param userId 用户ID
     * @return 默认通知类型
     */
    public ReminderType getDefaultReminderType(Long userId) {
        String value = getUserPreferenceValue(userId,
                UserPreferenceKey.DEFAULT_REMINDER_TYPE.getKey(),
                UserPreferenceKey.DEFAULT_REMINDER_TYPE.getDefaultValue());

        try {
            return ReminderType.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("无效的默认通知类型: {}, 使用EMAIL作为默认值", value);
            return ReminderType.EMAIL;
        }
    }

    /**
     * 获取用户的默认提前提醒时间（分钟）
     * @param userId 用户ID
     * @return 默认提前提醒时间
     */
    public Integer getDefaultAdvanceMinutes(Long userId) {
        String value = getUserPreferenceValue(userId,
                UserPreferenceKey.DEFAULT_ADVANCE_MINUTES.getKey(),
                UserPreferenceKey.DEFAULT_ADVANCE_MINUTES.getDefaultValue());

        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            log.warn("无效的默认提前提醒时间: {}, 使用15分钟作为默认值", value);
            return 15;
        }
    }

    /**
     * 初始化用户默认偏好设置
     * @param userId 用户ID
     * @return 初始化的偏好设置批量DTO
     */
    @Transactional
    public UserPreferenceDto.UserPreferencesDto initializeDefaultPreferences(Long userId) {
        log.info("初始化用户默认偏好设置, userId: {}", userId);
        
        Map<String, String> defaultPreferences = getDefaultPreferences();
        
        UserPreferenceDto.BatchUpdateDto batchUpdateDto = new UserPreferenceDto.BatchUpdateDto();
        batchUpdateDto.setPreferences(defaultPreferences);
        batchUpdateDto.setOverride(false); // 不覆盖已有设置
        
        return batchUpdateUserPreferences(userId, batchUpdateDto);
    }

    /**
     * 获取默认偏好设置
     * @return 默认偏好设置Map
     */
    private Map<String, String> getDefaultPreferences() {
        Map<String, String> defaults = new HashMap<>();

        // 使用枚举自动生成所有默认配置
        for (UserPreferenceKey preferenceKey : UserPreferenceKey.values()) {
            defaults.put(preferenceKey.getKey(), preferenceKey.getDefaultValue());
        }

        return defaults;
    }

    /**
     * 检查用户是否启用了标签管理功能
     * @param userId 用户ID
     * @return 是否启用
     */
    public boolean isUserTagManagementEnabled(Long userId) {
        String value = getUserPreferenceValue(userId,
                UserPreferenceKey.USER_TAG_MANAGEMENT_ENABLED.getKey(),
                UserPreferenceKey.USER_TAG_MANAGEMENT_ENABLED.getDefaultValue());
        return "1".equals(value);
    }

    /**
     * 获取用户标签列表
     * @param userId 用户ID
     * @return 标签列表，如果为空则返回空字符串
     */
    public String getUserTagList(Long userId) {
        return getUserPreferenceValue(userId,
                UserPreferenceKey.USER_TAG_LIST.getKey(),
                UserPreferenceKey.USER_TAG_LIST.getDefaultValue());
    }

    /**
     * 设置用户标签管理开关
     * @param userId 用户ID
     * @param enabled 是否启用（true=1, false=0）
     * @return 保存后的偏好设置DTO
     */
    @Transactional
    public UserPreferenceDto setUserTagManagementEnabled(Long userId, boolean enabled) {
        String value = enabled ? "1" : "0";
        return setUserPreference(userId, UserPreferenceKey.USER_TAG_MANAGEMENT_ENABLED.getKey(), value);
    }

    /**
     * 设置用户标签列表
     * @param userId 用户ID
     * @param tagList 标签列表（逗号分隔）
     * @return 保存后的偏好设置DTO
     */
    @Transactional
    public UserPreferenceDto setUserTagList(Long userId, String tagList) {
        return setUserPreference(userId, UserPreferenceKey.USER_TAG_LIST.getKey(), tagList);
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
} 