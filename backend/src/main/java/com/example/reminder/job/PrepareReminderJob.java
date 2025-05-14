package com.example.reminder.job;

import com.example.reminder.constant.CacheKeyEnum;
import com.example.reminder.model.SimpleReminder;
import com.example.reminder.service.ReminderEventServiceImpl;
import com.example.reminder.utils.RedisUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预处理下一分钟需要执行的提醒事项的定时任务
 * 1. 查询下一分钟需要触发的提醒
 * 2. 将提醒信息存入Redis，便于执行任务时快速获取
 */
@Slf4j
@Component
public class PrepareReminderJob implements Job {
    
    /**
     * Redis中存储的时间格式
     */
    private static final DateTimeFormatter REMINDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    @Autowired
    private ReminderEventServiceImpl reminderService;
    
    @Autowired
    private RedisUtils redisUtils;
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始预处理下一分钟的提醒任务");
        
        try {
            // 获取下一分钟需要触发的提醒
            List<SimpleReminder> nextMinuteReminders = reminderService.getNextMinuteReminders();
            
            if (nextMinuteReminders.isEmpty()) {
                log.info("下一分钟没有需要触发的提醒");
                return;
            }
            
            log.info("找到 {} 个需要触发的提醒", nextMinuteReminders.size());
            
            // 获取下一分钟的时间作为key
            String formattedTime = nextMinuteReminders.get(0).getEventTime().format(REMINDER_TIME_FORMATTER);
            String redisKey = CacheKeyEnum.PENDING_REMINDER.getKey(formattedTime);
            
            // 构建hash结构的map
            Map<String, Object> reminderMap = nextMinuteReminders.stream()
                .collect(Collectors.toMap(
                    reminder -> reminder.getId().toString(),
                    reminder -> reminder
                ));
            
            // 批量设置提醒到Redis
            redisUtils.hmsetJson(redisKey, reminderMap, CacheKeyEnum.PENDING_REMINDER.getExpireSeconds());
            
            log.info("已缓存提醒事项到Redis - 执行时间:{}, 键:{}, 数量:{}, 过期时间:{}秒", 
                formattedTime, redisKey, reminderMap.size(), 
                CacheKeyEnum.PENDING_REMINDER.getExpireSeconds());
            
        } catch (Exception e) {
            log.error("预处理提醒任务时发生错误", e);
            throw new JobExecutionException(e);
        }
    }
} 