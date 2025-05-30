package com.core.reminder.service;

import com.common.reminder.constant.ActivityAction;
import com.common.reminder.constant.ActivityStatus;
import com.common.reminder.constant.ResourceType;
import com.common.reminder.model.UserActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户活动日志服务接口
 */
public interface UserActivityLogService {

    /**
     * 记录用户活动日志
     */
    UserActivityLog logActivity(Long userId, ActivityAction action, ActivityStatus status,
                               ResourceType resourceType, Long resourceId, String resourceName,
                               String errorMessage, Map<String, Object> details,
                               HttpServletRequest request);

    /**
     * 记录用户活动日志（简化版本）
     */
    UserActivityLog logActivity(Long userId, ActivityAction action, ResourceType resourceType,
                               Long resourceId, String resourceName, HttpServletRequest request);

    /**
     * 记录用户活动日志（最简版本）
     */
    UserActivityLog logActivity(Long userId, ActivityAction action, HttpServletRequest request);

    /**
     * 异步记录用户活动日志
     */
    void logActivityAsync(Long userId, ActivityAction action, ActivityStatus status,
                         ResourceType resourceType, Long resourceId, String resourceName,
                         String errorMessage, Map<String, Object> details,
                         HttpServletRequest request);

    /**
     * 根据用户ID查询活动日志
     */
    Page<UserActivityLog> getUserActivityLogs(Long userId, Pageable pageable);

    /**
     * 根据用户ID和时间范围查询活动日志
     */
    Page<UserActivityLog> getUserActivityLogs(Long userId, OffsetDateTime startTime,
                                             OffsetDateTime endTime, Pageable pageable);

    /**
     * 根据操作类型查询活动日志
     */
    Page<UserActivityLog> getActivityLogsByAction(ActivityAction action, Pageable pageable);

    /**
     * 根据资源查询活动日志
     */
    List<UserActivityLog> getActivityLogsByResource(ResourceType resourceType, Long resourceId);

    /**
     * 根据会话ID查询活动日志
     */
    List<UserActivityLog> getActivityLogsBySession(String sessionId);

    /**
     * 查询用户最近的登录记录
     */
    List<UserActivityLog> getRecentLogins(Long userId, int limit);

    /**
     * 查询失败的登录尝试
     */
    List<UserActivityLog> getFailedLoginAttempts(OffsetDateTime startTime, OffsetDateTime endTime);

    /**
     * 查询高风险操作日志
     */
    Page<UserActivityLog> getHighRiskActivities(Pageable pageable);

    /**
     * 统计用户活动数量
     */
    Long countUserActivities(Long userId, OffsetDateTime startTime, OffsetDateTime endTime);

    /**
     * 统计操作类型数量
     */
    Long countActivitiesByAction(ActivityAction action, OffsetDateTime startTime, OffsetDateTime endTime);

    /**
     * 获取活动统计报告
     */
    Map<String, Object> getActivityStatistics(OffsetDateTime startTime, OffsetDateTime endTime);

    /**
     * 清理过期的日志记录
     */
    void cleanupOldLogs(int daysToKeep);

    /**
     * 检查用户是否存在异常活动
     */
    boolean hasAbnormalActivity(Long userId, OffsetDateTime startTime, OffsetDateTime endTime);

    /**
     * 获取用户活动摘要
     */
    Map<String, Object> getUserActivitySummary(Long userId, OffsetDateTime startTime, OffsetDateTime endTime);
} 