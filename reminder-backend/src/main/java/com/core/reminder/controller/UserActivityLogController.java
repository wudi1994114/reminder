package com.core.reminder.controller;

import com.common.reminder.constant.ActivityAction;
import com.common.reminder.constant.ResourceType;
import com.common.reminder.dto.UserProfileDto;
import com.common.reminder.model.UserActivityLog;
import com.core.reminder.aspect.ActivityLogAspect.LogActivity;
import com.core.reminder.service.UserActivityLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 用户活动日志控制器
 * 提供活动日志的查询和管理功能
 */
@Slf4j
@RestController
@RequestMapping("/api/activity-logs")
public class UserActivityLogController {

    @Autowired
    private UserActivityLogService activityLogService;

    /**
     * 获取当前用户的活动日志
     */
    @GetMapping("/my")
    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.USER, description = "查看个人活动日志")
    public ResponseEntity<Page<UserActivityLog>> getMyActivityLogs(
            @RequestAttribute("currentUser") UserProfileDto currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {

        Pageable pageable = PageRequest.of(page, size);
        Page<UserActivityLog> logs;

        if (startTime != null && endTime != null) {
            logs = activityLogService.getUserActivityLogs(currentUser.getId(), startTime, endTime, pageable);
        } else {
            logs = activityLogService.getUserActivityLogs(currentUser.getId(), pageable);
        }

        return ResponseEntity.ok(logs);
    }

    /**
     * 获取当前用户的活动摘要
     */
    @GetMapping("/my/summary")
    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.USER, description = "查看个人活动摘要")
    public ResponseEntity<Map<String, Object>> getMyActivitySummary(
            @RequestAttribute("currentUser") UserProfileDto currentUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {

        // 默认查询最近30天
        if (startTime == null) {
            startTime = OffsetDateTime.now().minusDays(30);
        }
        if (endTime == null) {
            endTime = OffsetDateTime.now();
        }

        Map<String, Object> summary = activityLogService.getUserActivitySummary(
                currentUser.getId(), startTime, endTime);

        return ResponseEntity.ok(summary);
    }

    /**
     * 获取当前用户的最近登录记录
     */
    @GetMapping("/my/recent-logins")
    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.USER, description = "查看最近登录记录")
    public ResponseEntity<List<UserActivityLog>> getMyRecentLogins(
            @RequestAttribute("currentUser") UserProfileDto currentUser,
            @RequestParam(defaultValue = "10") int limit) {

        List<UserActivityLog> recentLogins = activityLogService.getRecentLogins(currentUser.getId(), limit);
        return ResponseEntity.ok(recentLogins);
    }

    /**
     * 管理员：获取所有用户的活动日志
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.SYSTEM, description = "管理员查看所有活动日志")
    public ResponseEntity<Page<UserActivityLog>> getAllActivityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {

        // 默认查询最近7天
        if (startTime == null) {
            startTime = OffsetDateTime.now().minusDays(7);
        }
        if (endTime == null) {
            endTime = OffsetDateTime.now();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<UserActivityLog> logs = activityLogService.getUserActivityLogs(null, startTime, endTime, pageable);

        return ResponseEntity.ok(logs);
    }

    /**
     * 管理员：获取指定用户的活动日志
     */
    @GetMapping("/admin/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.USER, description = "管理员查看指定用户活动日志")
    public ResponseEntity<Page<UserActivityLog>> getUserActivityLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {

        Pageable pageable = PageRequest.of(page, size);
        Page<UserActivityLog> logs;

        if (startTime != null && endTime != null) {
            logs = activityLogService.getUserActivityLogs(userId, startTime, endTime, pageable);
        } else {
            logs = activityLogService.getUserActivityLogs(userId, pageable);
        }

        return ResponseEntity.ok(logs);
    }

    /**
     * 管理员：获取活动统计报告
     */
    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.SYSTEM, description = "管理员查看活动统计")
    public ResponseEntity<Map<String, Object>> getActivityStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {

        // 默认查询最近30天
        if (startTime == null) {
            startTime = OffsetDateTime.now().minusDays(30);
        }
        if (endTime == null) {
            endTime = OffsetDateTime.now();
        }

        Map<String, Object> statistics = activityLogService.getActivityStatistics(startTime, endTime);
        return ResponseEntity.ok(statistics);
    }

    /**
     * 管理员：获取高风险操作日志
     */
    @GetMapping("/admin/high-risk")
    @PreAuthorize("hasRole('ADMIN')")
    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.SYSTEM, description = "管理员查看高风险操作")
    public ResponseEntity<Page<UserActivityLog>> getHighRiskActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<UserActivityLog> logs = activityLogService.getHighRiskActivities(pageable);

        return ResponseEntity.ok(logs);
    }

    /**
     * 管理员：获取失败的登录尝试
     */
    @GetMapping("/admin/failed-logins")
    @PreAuthorize("hasRole('ADMIN')")
    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.SYSTEM, description = "管理员查看登录失败记录")
    public ResponseEntity<List<UserActivityLog>> getFailedLoginAttempts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {

        // 默认查询最近24小时
        if (startTime == null) {
            startTime = OffsetDateTime.now().minusHours(24);
        }
        if (endTime == null) {
            endTime = OffsetDateTime.now();
        }

        List<UserActivityLog> failedLogins = activityLogService.getFailedLoginAttempts(startTime, endTime);
        return ResponseEntity.ok(failedLogins);
    }

    /**
     * 管理员：根据操作类型查询日志
     */
    @GetMapping("/admin/by-action/{action}")
    @PreAuthorize("hasRole('ADMIN')")
    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.SYSTEM, description = "管理员按操作类型查看日志")
    public ResponseEntity<Page<UserActivityLog>> getActivityLogsByAction(
            @PathVariable ActivityAction action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<UserActivityLog> logs = activityLogService.getActivityLogsByAction(action, pageable);

        return ResponseEntity.ok(logs);
    }

    /**
     * 管理员：根据资源查询日志
     */
    @GetMapping("/admin/by-resource")
    @PreAuthorize("hasRole('ADMIN')")
    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.SYSTEM, description = "管理员按资源查看日志")
    public ResponseEntity<List<UserActivityLog>> getActivityLogsByResource(
            @RequestParam ResourceType resourceType,
            @RequestParam Long resourceId) {

        List<UserActivityLog> logs = activityLogService.getActivityLogsByResource(resourceType, resourceId);
        return ResponseEntity.ok(logs);
    }

    /**
     * 管理员：清理过期日志
     */
    @DeleteMapping("/admin/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    @LogActivity(action = ActivityAction.DATA_EXPORT, resourceType = ResourceType.SYSTEM, description = "管理员清理过期日志")
    public ResponseEntity<String> cleanupOldLogs(
            @RequestParam(defaultValue = "90") int daysToKeep) {

        try {
            activityLogService.cleanupOldLogs(daysToKeep);
            return ResponseEntity.ok("成功清理了" + daysToKeep + "天前的日志记录");
        } catch (Exception e) {
            log.error("清理日志失败", e);
            return ResponseEntity.internalServerError().body("清理日志失败: " + e.getMessage());
        }
    }

    /**
     * 管理员：检查用户异常活动
     */
    @GetMapping("/admin/check-abnormal/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.USER, description = "管理员检查用户异常活动")
    public ResponseEntity<Map<String, Object>> checkAbnormalActivity(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {

        // 默认检查最近24小时
        if (startTime == null) {
            startTime = OffsetDateTime.now().minusHours(24);
        }
        if (endTime == null) {
            endTime = OffsetDateTime.now();
        }

        boolean hasAbnormal = activityLogService.hasAbnormalActivity(userId, startTime, endTime);
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("hasAbnormalActivity", hasAbnormal);
        
        Map<String, Object> checkPeriod = new HashMap<>();
        checkPeriod.put("startTime", startTime);
        checkPeriod.put("endTime", endTime);
        result.put("checkPeriod", checkPeriod);

        return ResponseEntity.ok(result);
    }
} 