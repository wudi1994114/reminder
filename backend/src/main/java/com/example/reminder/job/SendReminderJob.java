package com.example.reminder.job;

import com.example.reminder.constant.CacheKeyEnum;
import com.example.reminder.model.SimpleReminder;
import com.example.reminder.utils.JacksonUtils;
import com.example.reminder.utils.RedisUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

/**
 * 发送备忘录的任务
 * 用于在指定时间发送备忘录给用户
 */
@Slf4j
@Component
public class SendReminderJob implements Job {
    
    private static final DateTimeFormatter REMINDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final ExecutorService reminderExecutor = Executors.newFixedThreadPool(5);
    
    @Autowired
    private RedisUtils redisUtils;
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行提醒发送任务");
        
        try {
            // 获取当前时间，用于构造Redis key
            String currentTime = OffsetDateTime.now().format(REMINDER_TIME_FORMATTER);
            String redisKey = CacheKeyEnum.PENDING_REMINDER.getKey(currentTime);
            
            // 获取当前时间需要发送的所有提醒
            Map<Object, Object> reminderMap = redisUtils.hmget(redisKey);
            
            if (reminderMap == null || reminderMap.isEmpty()) {
                log.info("当前时间 {} 没有需要发送的提醒", currentTime);
                return;
            }
            
            log.info("获取到 {} 个需要发送的提醒", reminderMap.size());
            
            // 创建异步任务列表
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            // 遍历并异步处理每个提醒
            for (Map.Entry<Object, Object> entry : reminderMap.entrySet()) {
                String reminderId = entry.getKey().toString();
                String reminderJson = (String) entry.getValue();
                
                if (reminderJson != null) {
                    // 创建异步任务
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            SimpleReminder reminder = JacksonUtils.fromJson(reminderJson, SimpleReminder.class);
                            
                            // TODO: 实现具体的推送逻辑，可以根据不同的提醒类型使用不同的推送方式
                            // 1. 可以通过WebSocket推送到前端
                            // 2. 可以发送邮件
                            // 3. 可以发送手机通知
                            // 4. 可以调用其他消息服务
                            // 5. 记录日志以及入库历史消息表  
                            
                            log.info("正在发送提醒 - ID:{}, 标题:{}, 接收用户:{}", 
                                reminder.getId(), reminder.getTitle(), reminder.getToUserId());
                            
                        } catch (Exception e) {
                            log.error("发送提醒失败 - ID:{}, 错误:{}", reminderId, e.getMessage());
                        }
                    }, reminderExecutor);
                    
                    futures.add(future);
                }
            }
            
            // 等待所有推送任务完成，最多等待30秒
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(30, TimeUnit.SECONDS);
            
            // 处理完成后删除Redis中的数据
            redisUtils.del(redisKey);
            log.info("提醒发送任务完成，已清理Redis缓存 - 键:{}", redisKey);
            
        } catch (Exception e) {
            log.error("执行提醒发送任务时发生错误", e);
            throw new JobExecutionException(e);
        }
    }
    
    /**
     * 关闭线程池的方法，应在应用关闭时调用
     * 可以添加@PreDestroy注解或在其他合适的地方调用
     */
    public void shutdown() {
        reminderExecutor.shutdown();
        try {
            if (!reminderExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                reminderExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            reminderExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void preDestroy() {
        shutdown();
    }
} 