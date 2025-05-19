package com.task.reminder.job;

import com.common.reminder.constant.CacheKeyEnum;
import com.common.reminder.dto.UserProfileDto;
import com.common.reminder.model.SimpleReminder;
import com.task.reminder.sender.GmailSender;
import com.task.reminder.service.UserCacheService;
import com.common.reminder.utils.JacksonUtils;
import com.task.reminder.utils.RedisUtils;
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
 * 提醒事项发送任务
 * 
 * 该任务负责实际发送提醒给用户。它从Redis中获取由PrepareReminderJob预先缓存的提醒事项，
 * 并使用线程池并行处理多个提醒，以提高处理效率并避免阻塞Quartz的调度线程。
 * 
 * 工作流程：
 * 1. 从Redis获取当前时间需要发送的所有提醒事项
 * 2. 使用线程池并行处理每个提醒，实现异步发送
 * 3. 等待所有提醒处理完成（设置超时机制）
 * 4. 发送完成后从Redis删除相关数据，避免重复处理
 * 
 * 该任务通常由Quartz调度器按固定频率（如每分钟）执行，紧随PrepareReminderJob之后。
 * 使用线程池可以提高系统处理大量提醒的能力，并且通过超时机制确保不会因少数提醒处理缓慢而影响整体任务。
 */
@Slf4j // 使用Lombok自动生成日志记录器
@Component // 将类标记为Spring组件，使其可以被自动检测和注入
public class SendReminderJob implements Job {
    
    /**
     * Redis中存储的时间格式：年-月-日 时:分
     * 用于构造Redis键名，与PrepareReminderJob使用的格式保持一致
     */
    private static final DateTimeFormatter REMINDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    /**
     * 固定大小的线程池，用于并行处理提醒发送任务
     * 线程池大小为5，表示最多同时处理5个提醒发送任务
     * 使用固定大小线程池可以限制系统资源使用，避免过多线程导致性能下降
     */
    private static final ExecutorService reminderExecutor = Executors.newFixedThreadPool(5);
    
    /**
     * Redis工具类，提供对Redis的操作功能
     */
    @Autowired
    private RedisUtils redisUtils;
    
    @Autowired
    private GmailSender gmailSender;
    
    @Autowired
    private UserCacheService userCacheService;
    
    /**
     * 任务执行方法，由Quartz调度器在指定时间调用
     * 
     * @param context 作业执行上下文，包含作业的相关信息
     * @throws JobExecutionException 当任务执行过程中出现错误时抛出
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行提醒发送任务");
        
        try {
            // 获取当前时间，用于构造Redis key
            String currentTime = OffsetDateTime.now().format(REMINDER_TIME_FORMATTER);
            String redisKey = CacheKeyEnum.PENDING_REMINDER.getKey(currentTime);
            
            // 获取当前时间需要发送的所有提醒
            // Redis中的数据由PrepareReminderJob提前准备好
            Map<Object, Object> reminderMap = redisUtils.hmget(redisKey);
            
            // 检查是否有需要发送的提醒
            if (reminderMap == null || reminderMap.isEmpty()) {
                log.info("当前时间 {} 没有需要发送的提醒", currentTime);
                return;
            }
            
            log.info("获取到 {} 个需要发送的提醒", reminderMap.size());
            
            // 创建异步任务列表，用于跟踪所有的提醒发送任务
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            // 遍历并异步处理每个提醒
            for (Map.Entry<Object, Object> entry : reminderMap.entrySet()) {
                String reminderId = entry.getKey().toString();
                String reminderJson = (String) entry.getValue();
                
                if (reminderJson != null) {
                    // 为每个提醒创建异步任务，并提交到线程池执行
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            // 将JSON字符串反序列化为SimpleReminder对象
                            SimpleReminder reminder = JacksonUtils.fromJson(reminderJson, SimpleReminder.class);
                            
                            log.info("正在处理提醒 - ID:{}, 标题:{}, 目标用户ID:{}", 
                                reminder.getId(), reminder.getTitle(), reminder.getToUserId());

                            // 发送邮件逻辑
                            if (gmailSender != null && userCacheService != null && reminder.getToUserId() != null) {
                                UserProfileDto userProfile = null;
                                try {
                                    userProfile = userCacheService.getUserProfileById(reminder.getToUserId()); 
                                } catch (Exception e) {
                                    log.error("获取用户信息失败 (ID: {}) - 提醒ID: {}, 错误: {}", reminder.getToUserId(), reminder.getId(), e.getMessage());
                                }

                                if (userProfile != null && userProfile.getEmail() != null && !userProfile.getEmail().isEmpty()) {
                                    try {
                                        String emailSubject = reminder.getTitle();
                                        // 邮件正文可根据需求调整，例如加入用户昵称 userProfile.getNickname()
                                        String emailBody = "尊敬的" + (userProfile.getNickname() != null ? userProfile.getNickname() : "用户") + "，<br/>您的提醒事项：" + reminder.getTitle(); 
                                        
                                        gmailSender.sendEmail(userProfile.getEmail(), emailSubject, emailBody);
                                        log.info("邮件提醒已成功发送至 {} (用户ID: {}) - 提醒ID: {}", userProfile.getEmail(), reminder.getToUserId(), reminder.getId());
                                    } catch (Exception mailEx) {
                                        log.error("发送邮件提醒失败 - 提醒ID: {}, 用户ID: {}, 邮箱: {}, 错误: {}", 
                                            reminder.getId(), reminder.getToUserId(), userProfile.getEmail(), mailEx.getMessage());
                                    }
                                } else {
                                    if (userProfile == null) {
                                        log.warn("未能获取到用户信息 (ID: {})，无法发送邮件提醒 - 提醒ID: {}", reminder.getToUserId(), reminder.getId());
                                    } else {
                                        log.warn("用户邮箱为空 (ID: {})，无法发送邮件提醒 - 提醒ID: {}", reminder.getToUserId(), reminder.getId());
                                    }
                                }
                            } else {
                                if (gmailSender == null) {
                                    log.warn("GmailSender 未注入，无法发送邮件提醒 - 提醒ID: {}", reminder.getId());
                                }
                                if (userCacheService == null) {
                                    log.warn("UserCacheService 未注入，无法获取用户信息 - 提醒ID: {}", reminder.getId());
                                }
                                if (reminder.getToUserId() == null){
                                    log.warn("接收用户ID为空，无法发送邮件提醒 - 提醒ID: {}, 标题: {}", reminder.getId(), reminder.getTitle());
                                }
                            }
                            
                        } catch (Exception e) {
                            // 捕获并记录单个提醒处理过程中的异常，但不影响其他提醒的处理
                            log.error("发送提醒失败 - ID:{}, 错误:{}", reminderId, e.getMessage());
                        }
                    }, reminderExecutor);
                    
                    // 将异步任务添加到列表中，用于后续等待所有任务完成
                    futures.add(future);
                }
            }
            
            // 等待所有推送任务完成，最多等待30秒
            // 这种方式可以并行处理所有提醒，提高效率，同时设置超时防止个别提醒处理过慢影响整体任务
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(30, TimeUnit.SECONDS);
            
            // 处理完成后删除Redis中的数据，避免重复处理
            redisUtils.del(redisKey);
            log.info("提醒发送任务完成，已清理Redis缓存 - 键:{}", redisKey);
            
        } catch (Exception e) {
            // 捕获并记录任何异常，确保日志中包含错误信息
            log.error("执行提醒发送任务时发生错误", e);
            // 将异常包装为JobExecutionException并抛出，通知调度器任务执行失败
            throw new JobExecutionException(e);
        }
    }
    
    /**
     * 关闭线程池的方法
     * 
     * 该方法尝试优雅地关闭线程池：
     * 1. 首先尝试正常关闭，不接受新任务，但允许已提交任务完成
     * 2. 等待最多60秒让任务完成
     * 3. 如果任务未能在超时时间内完成，则强制关闭线程池
     * 4. 如果等待过程中被中断，也会强制关闭线程池
     * 
     * 这种优雅关闭可以确保尽可能多的提醒能够正常发送完成，同时不会因为个别任务卡住而导致应用无法正常关闭。
     */
    public void shutdown() {
        reminderExecutor.shutdown();
        try {
            // 等待任务完成，最多等待60秒
            if (!reminderExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                // 如果等待超时，则强制关闭线程池
                reminderExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            // 如果等待过程中被中断，也强制关闭线程池
            reminderExecutor.shutdownNow();
            // 恢复中断状态
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 在Spring容器销毁Bean时自动调用shutdown方法，确保线程池正确关闭
     * 这是Spring生命周期回调机制的一部分，用于资源清理
     */
    @PreDestroy
    public void preDestroy() {
        shutdown();
    }
} 