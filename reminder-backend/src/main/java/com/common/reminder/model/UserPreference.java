package com.common.reminder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.OffsetDateTime;

/**
 * 用户偏好设置实体 - 键值对存储模式
 */
@Entity
@Table(name = "user_preference")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(nullable = false, name = "user_id")
    private Long userId;

    /**
     * 偏好设置键名
     * 例如：defaultReminderType, emailNotificationEnabled, theme 等
     */
    @Column(nullable = false, length = 100, name = "key")
    private String key;

    /**
     * 偏好设置值
     * 存储为字符串，可根据需要进行类型转换
     */
    @Column(columnDefinition = "TEXT", name = "value")
    private String value;

    /**
     * 偏好设置属性/描述
     * 可选字段，用于存储配置项的额外描述信息
     */
    @Column(length = 500, name = "property")
    private String property;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE", name = "create_at")
    private OffsetDateTime createAt;

    /**
     * 修改时间
     */
    @UpdateTimestamp
    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE", name = "modify_at")
    private OffsetDateTime modifyAt;

    // 构造函数
    public UserPreference(Long userId, String key, String value) {
        this.userId = userId;
        this.key = key;
        this.value = value;
    }

    public UserPreference(Long userId, String key, String value, String property) {
        this.userId = userId;
        this.key = key;
        this.value = value;
        this.property = property;
    }
} 