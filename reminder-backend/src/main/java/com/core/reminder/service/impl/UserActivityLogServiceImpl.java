package com.core.reminder.service.impl;

import com.common.reminder.constant.ActivityAction;
import com.common.reminder.constant.ActivityStatus;
import com.common.reminder.constant.ResourceType;
import com.common.reminder.model.UserActivityLog;
import com.core.reminder.repository.UserActivityLogRepository;
import com.core.reminder.service.UserActivityLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户活动日志服务实现类
 */
@Slf4j
@Service
@Transactional
public class UserActivityLogServiceImpl implements UserActivityLogService {

    @Autowired
    private UserActivityLogRepository activityLogRepository;

    @Override
    public UserActivityLog logActivity(Long userId, ActivityAction action, ActivityStatus status,
                                      ResourceType resourceType, Long resourceId, String resourceName,
                                      String errorMessage, Map<String, Object> details,
                                      HttpServletRequest request) {
        try {
            UserActivityLog.UserActivityLogBuilder builder = UserActivityLog.builder()
                    .userId(userId)
                    .action(action)
                    .status(status)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .resourceName(resourceName)
                    .errorMessage(errorMessage);

            // 从请求中提取信息
            if (request != null) {
                builder.sessionId(request.getSession().getId())
                       .requestMethod(request.getMethod())
                       .requestUrl(request.getRequestURI())
                       .userAgent(request.getHeader("User-Agent"));

                // 获取真实IP地址
                String ipAddress = getClientIpAddress(request);
                if (ipAddress != null) {
                    UserActivityLog log = builder.build();
                    log.setIpAddressString(ipAddress);
                    // 设置详细信息
                    if (details != null) {
                        log.setDetailsMap(details);
                    }
                    return activityLogRepository.save(log);
                }
            }

            UserActivityLog log = builder.build();
            // 设置详细信息
            if (details != null) {
                log.setDetailsMap(details);
            }
            return activityLogRepository.save(log);
        } catch (Exception e) {
            log.error("记录用户活动日志失败: userId={}, action={}", userId, action, e);
            return null;
        }
    }

    @Override
    public UserActivityLog logActivity(Long userId, ActivityAction action, ResourceType resourceType,
                                      Long resourceId, String resourceName, HttpServletRequest request) {
        return logActivity(userId, action, ActivityStatus.SUCCESS, resourceType, resourceId,
                          resourceName, null, null, request);
    }

    @Override
    public UserActivityLog logActivity(Long userId, ActivityAction action, HttpServletRequest request) {
        return logActivity(userId, action, ActivityStatus.SUCCESS, null, null,
                          null, null, null, request);
    }

    @Override
    @Async
    public void logActivityAsync(Long userId, ActivityAction action, ActivityStatus status,
                                ResourceType resourceType, Long resourceId, String resourceName,
                                String errorMessage, Map<String, Object> details,
                                HttpServletRequest request) {
        logActivity(userId, action, status, resourceType, resourceId, resourceName,
                   errorMessage, details, request);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserActivityLog> getUserActivityLogs(Long userId, Pageable pageable) {
        return activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserActivityLog> getUserActivityLogs(Long userId, OffsetDateTime startTime,
                                                    OffsetDateTime endTime, Pageable pageable) {
        return activityLogRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                userId, startTime, endTime, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserActivityLog> getActivityLogsByAction(ActivityAction action, Pageable pageable) {
        return activityLogRepository.findByActionOrderByCreatedAtDesc(action, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserActivityLog> getActivityLogsByResource(ResourceType resourceType, Long resourceId) {
        return activityLogRepository.findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
                resourceType, resourceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserActivityLog> getActivityLogsBySession(String sessionId) {
        return activityLogRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserActivityLog> getRecentLogins(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return activityLogRepository.findRecentLoginsByUserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserActivityLog> getFailedLoginAttempts(OffsetDateTime startTime, OffsetDateTime endTime) {
        return activityLogRepository.findFailedLoginAttempts(startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserActivityLog> getHighRiskActivities(Pageable pageable) {
        return activityLogRepository.findHighRiskActivities(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countUserActivities(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        return activityLogRepository.countByUserIdAndTimeRange(userId, startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countActivitiesByAction(ActivityAction action, OffsetDateTime startTime, OffsetDateTime endTime) {
        return activityLogRepository.countByActionAndTimeRange(action, startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getActivityStatistics(OffsetDateTime startTime, OffsetDateTime endTime) {
        Map<String, Object> statistics = new HashMap<>();

        // 统计各操作类型的数量
        List<Object[]> actionCounts = activityLogRepository.countByActionInTimeRange(startTime, endTime);
        Map<String, Long> actionStatistics = new HashMap<>();
        for (Object[] row : actionCounts) {
            actionStatistics.put(((ActivityAction) row[0]).name(), (Long) row[1]);
        }
        statistics.put("actionStatistics", actionStatistics);

        // 统计各用户的活动数量
        List<Object[]> userCounts = activityLogRepository.countByUserInTimeRange(startTime, endTime);
        Map<Long, Long> userStatistics = new HashMap<>();
        for (Object[] row : userCounts) {
            userStatistics.put((Long) row[0], (Long) row[1]);
        }
        statistics.put("userStatistics", userStatistics);

        // 统计总数
        Page<UserActivityLog> totalLogs = activityLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                startTime, endTime, PageRequest.of(0, 1));
        statistics.put("totalCount", totalLogs.getTotalElements());

        return statistics;
    }

    @Override
    @Transactional
    public void cleanupOldLogs(int daysToKeep) {
        OffsetDateTime cutoffTime = OffsetDateTime.now().minusDays(daysToKeep);
        try {
            activityLogRepository.deleteByCreatedAtBefore(cutoffTime);
            log.info("清理了{}天前的活动日志", daysToKeep);
        } catch (Exception e) {
            log.error("清理活动日志失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAbnormalActivity(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        // 检查登录失败次数
        Long failedLogins = countActivitiesByAction(ActivityAction.LOGIN_FAILED, startTime, endTime);
        if (failedLogins > 5) { // 超过5次登录失败
            return true;
        }

        // 检查总活动次数是否异常
        Long totalActivities = countUserActivities(userId, startTime, endTime);
        if (totalActivities > 1000) { // 单日超过1000次操作
            return true;
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getUserActivitySummary(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        Map<String, Object> summary = new HashMap<>();

        // 总活动次数
        Long totalActivities = countUserActivities(userId, startTime, endTime);
        summary.put("totalActivities", totalActivities);

        // 最近登录记录
        List<UserActivityLog> recentLogins = getRecentLogins(userId, 5);
        summary.put("recentLogins", recentLogins);

        // 是否有异常活动
        boolean hasAbnormal = hasAbnormalActivity(userId, startTime, endTime);
        summary.put("hasAbnormalActivity", hasAbnormal);

        // 活动时间范围
        summary.put("startTime", startTime);
        summary.put("endTime", endTime);

        return summary;
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 多级代理的情况，取第一个IP
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}