package com.common.reminder.model;

import com.common.reminder.constant.ActivityAction;
import com.common.reminder.constant.ActivityStatus;
import com.common.reminder.constant.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.OffsetDateTime;

/**
 * 用户活动日志实体类
 * 记录用户在系统中的各种操作行为
 */
@Entity
@Table(name = "user_activity_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 操作用户ID，用户删除后设为NULL
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 会话ID，用于关联同一次登录的操作
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * 操作类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 100)
    private ActivityAction action;

    /**
     * 资源类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", length = 50)
    private ResourceType resourceType;

    /**
     * 资源ID
     */
    @Column(name = "resource_id")
    private Long resourceId;

    /**
     * 资源名称（冗余存储，便于查询）
     */
    @Column(name = "resource_name", length = 200)
    private String resourceName;

    /**
     * 客户端IP地址
     */
    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    /**
     * 用户代理字符串
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * HTTP请求方法
     */
    @Column(name = "request_method", length = 10)
    private String requestMethod;

    /**
     * 请求URL
     */
    @Column(name = "request_url", length = 500)
    private String requestUrl;

    /**
     * 操作状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private ActivityStatus status = ActivityStatus.SUCCESS;

    /**
     * 错误信息（如果操作失败）
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 执行时间（毫秒）
     */
    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    /**
     * 详细信息（JSON格式存储为字符串）
     */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    /**
     * 便捷方法：设置IP地址（字符串格式）
     */
    public void setIpAddressString(String ipAddressString) {
        if (ipAddressString != null && !ipAddressString.trim().isEmpty()) {
            // 简单的IP地址格式验证
            if (isValidIpAddress(ipAddressString)) {
                this.ipAddress = ipAddressString;
            } else {
                this.ipAddress = null;
            }
        } else {
            this.ipAddress = null;
        }
    }

    /**
     * 便捷方法：获取IP地址字符串
     */
    public String getIpAddressString() {
        return ipAddress;
    }

    /**
     * 简单的IP地址格式验证
     */
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        
        // IPv4 格式验证
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            try {
                for (String part : parts) {
                    int num = Integer.parseInt(part);
                    if (num < 0 || num > 255) {
                        return false;
                    }
                }
                return true;
            } catch (NumberFormatException e) {
                // 可能是IPv6，继续检查
            }
        }
        
        // IPv6 格式简单验证（包含冒号）
        if (ip.contains(":")) {
            return true; // 简化的IPv6验证
        }
        
        return false;
    }

    /**
     * 便捷方法：设置详细信息（将Map转换为JSON字符串）
     */
    public void setDetailsMap(java.util.Map<String, Object> detailsMap) {
        if (detailsMap != null && !detailsMap.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                this.details = mapper.writeValueAsString(detailsMap);
            } catch (Exception e) {
                this.details = "{}";
            }
        } else {
            this.details = null;
        }
    }

    /**
     * 便捷方法：获取详细信息（将JSON字符串转换为Map）
     */
    @SuppressWarnings("unchecked")
    public java.util.Map<String, Object> getDetailsMap() {
        if (this.details != null && !this.details.trim().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(this.details, java.util.Map.class);
            } catch (Exception e) {
                return new java.util.HashMap<>();
            }
        }
        return new java.util.HashMap<>();
    }

    /**
     * 便捷方法：添加详细信息
     */
    public void addDetail(String key, Object value) {
        java.util.Map<String, Object> detailsMap = getDetailsMap();
        detailsMap.put(key, value);
        setDetailsMap(detailsMap);
    }

    /**
     * 便捷方法：获取详细信息
     */
    public Object getDetail(String key) {
        java.util.Map<String, Object> detailsMap = getDetailsMap();
        return detailsMap.get(key);
    }
} 