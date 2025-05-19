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
 * 提醒事项预处理定时任务
 * 
 * 该任务负责提前获取下一分钟需要执行的所有提醒事项，并将它们缓存到Redis中，以便SendReminderJob能够快速获取并处理。
 * 这种预处理机制可以减轻发送任务的压力，提高系统响应速度，并且通过Redis的持久化确保即使在服务重启的情况下也不会丢失提醒。
 * 
 * 工作流程：
 * 1. 从数据库查询下一分钟需要触发的所有提醒事项
 * 2. 将提醒信息以哈希结构存入Redis，键名包含执行时间
 * 3. 设置Redis数据的过期时间，防止内存泄漏
 * 
 * 该任务通常由Quartz调度器按固定频率（如每分钟）执行。
 */
@Slf4j // 使用Lombok自动生成日志记录器
@Component // 将类标记为Spring组件，使其可以被自动检测和注入
public class PrepareReminderJob implements Job {
    
    /**
     * Redis中存储的时间格式：年-月-日 时:分
     * 用于格式化提醒时间，构造Redis键名
     */
    private static final DateTimeFormatter REMINDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    /**
     * 提醒事项服务，提供获取下一分钟需要触发的提醒等功能
     */
    @Autowired
    private ReminderEventServiceImpl reminderService;
    
    /**
     * Redis工具类，提供对Redis的操作功能
     */
    @Autowired
    private RedisUtils redisUtils;
    
    /**
     * 任务执行方法，由Quartz调度器在指定时间调用
     * 
     * @param context 作业执行上下文，包含作业的相关信息
     * @throws JobExecutionException 当任务执行过程中出现错误时抛出
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始预处理下一分钟的提醒任务");
        
        try {
            // 获取下一分钟需要触发的提醒
            List<SimpleReminder> nextMinuteReminders = reminderService.getNextMinuteReminders();
            
            // 如果没有需要触发的提醒，则直接返回
            if (nextMinuteReminders.isEmpty()) {
                log.info("下一分钟没有需要触发的提醒");
                return;
            }
            
            log.info("找到 {} 个需要触发的提醒", nextMinuteReminders.size());
            
            // 获取下一分钟的时间作为key，确保所有的提醒使用同一个时间标记
            String formattedTime = nextMinuteReminders.get(0).getEventTime().format(REMINDER_TIME_FORMATTER);
            // 构造Redis键名，包含时间信息
            String redisKey = CacheKeyEnum.PENDING_REMINDER.getKey(formattedTime);
            
            // 构建哈希结构的map，将提醒ID作为字段名，提醒对象作为值
            // 使用哈希结构可以在同一个键下存储多个提醒，便于批量操作
            Map<String, Object> reminderMap = nextMinuteReminders.stream()
                .collect(Collectors.toMap(
                    reminder -> reminder.getId().toString(), // 使用提醒ID作为哈希字段
                    reminder -> reminder                     // 值为提醒对象本身
                ));
            
            // 批量设置提醒到Redis，使用JSON序列化存储对象
            // 同时设置过期时间，防止占用过多内存
            redisUtils.hmsetJson(redisKey, reminderMap, CacheKeyEnum.PENDING_REMINDER.getExpireSeconds());
            
            log.info("已缓存提醒事项到Redis - 执行时间:{}, 键:{}, 数量:{}, 过期时间:{}秒", 
                formattedTime, redisKey, reminderMap.size(), 
                CacheKeyEnum.PENDING_REMINDER.getExpireSeconds());
            
        } catch (Exception e) {
            // 捕获并记录任何异常，确保日志中包含错误信息
            log.error("预处理提醒任务时发生错误", e);
            // 将异常包装为JobExecutionException并抛出，通知调度器任务执行失败
            throw new JobExecutionException(e);
        }
    }
} 