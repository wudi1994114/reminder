package com.core.reminder.dto;

import com.common.reminder.model.ReminderType;
import com.core.reminder.enums.UserPreferenceKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户偏好设置DTO - 键值对存储模式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceDto {

    private Long id;

    private Long userId;

    /**
     * 偏好设置键名
     */
    @NotBlank(message = "偏好设置键名不能为空")
    @Size(max = 100, message = "偏好设置键名长度不能超过100个字符")
    private String key;

    /**
     * 偏好设置值
     */
    private String value;

    /**
     * 偏好设置描述/属性
     */
    @Size(max = 500, message = "偏好设置描述长度不能超过500个字符")
    private String property;

    /**
     * 创建时间
     */
    private OffsetDateTime createAt;

    /**
     * 修改时间
     */
    private OffsetDateTime modifyAt;

    // 便利构造函数
    public UserPreferenceDto(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public UserPreferenceDto(String key, String value, String property) {
        this.key = key;
        this.value = value;
        this.property = property;
    }

    /**
     * 批量用户偏好设置DTO
     * 用于批量操作和返回用户的所有偏好设置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPreferencesDto {
        
        private Long userId;
        
        /**
         * 用户所有偏好设置的键值对
         */
        private Map<String, String> preferences;
        
        /**
         * 偏好设置详情列表（包含完整信息）
         */
        private List<UserPreferenceDto> details;
        
        /**
         * 总数量
         */
        private Integer total;
        
        /**
         * 最后更新时间
         */
        private OffsetDateTime lastModified;
        
        public UserPreferencesDto(Long userId, Map<String, String> preferences) {
            this.userId = userId;
            this.preferences = preferences;
            this.total = preferences != null ? preferences.size() : 0;
        }
    }

    /**
     * 批量更新偏好设置的请求DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchUpdateDto {
        
        /**
         * 要更新的偏好设置键值对
         */
        @NotNull(message = "偏好设置不能为空")
        private Map<String, String> preferences;
        
        /**
         * 是否覆盖现有设置
         * true: 覆盖，false: 只更新提供的键，保留其他现有设置
         */
        private Boolean override = false;
    }

    /**
     * 获取所有可用的配置项键名
     * @return 配置项键名数组
     */
    public static String[] getAllPreferenceKeys() {
        return UserPreferenceKey.getAllKeys();
    }

    /**
     * 检查键名是否有效
     * @param key 键名
     * @return 是否有效
     */
    public static boolean isValidKey(String key) {
        return UserPreferenceKey.isValidKey(key);
    }
} 