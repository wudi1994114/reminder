package com.core.reminder.job;

import com.core.reminder.service.UserActivityLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 活动日志清理定时任务
 * 定期清理过期的用户活动日志，避免数据库存储过多历史数据
 */
@Slf4j
@Component
public class ActivityLogCleanupJob {

    @Autowired
    private UserActivityLogService activityLogService;

    /**
     * 日志保留天数，默认90天
     */
    @Value("${app.activity-log.retention-days:90}")
    private int retentionDays;

    /**
     * 每天凌晨2点执行日志清理任务
     * cron表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldActivityLogs() {
        log.info("开始执行活动日志清理任务，保留最近{}天的日志", retentionDays);
        
        try {
            activityLogService.cleanupOldLogs(retentionDays);
            log.info("活动日志清理任务执行完成");
        } catch (Exception e) {
            log.error("活动日志清理任务执行失败", e);
        }
    }

    /**
     * 手动触发清理任务（用于测试）
     */
    public void manualCleanup() {
        log.info("手动触发活动日志清理任务");
        cleanupOldActivityLogs();
    }
} 