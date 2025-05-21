package com.task.reminder.job;

import com.common.reminder.constant.CacheKeyEnum;
import com.common.reminder.dto.UserProfileDto;
import com.common.reminder.model.ReminderExecutionHistory;
import com.common.reminder.model.SimpleReminder;
import com.task.reminder.repository.ReminderExecutionHistoryRepository;
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
    
    @Autowired
    private ReminderExecutionHistoryRepository historyRepository;
    
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
                String reminderIdStr = entry.getKey().toString();
                String reminderJson = (String) entry.getValue();
                
                if (reminderJson != null) {
                    // 为每个提醒创建异步任务，并提交到线程池执行
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        SimpleReminder reminder = null;
                        UserProfileDto userProfile = null;
                        String status = "FAILURE"; // 默认状态为失败
                        String details = "";
                        Long parsedReminderId = null; // 用于在 reminder 对象解析失败时，从字符串尝试获取的 ID

                        try {
                            // 尝试从字符串解析ID，以备后用
                            try { parsedReminderId = Long.parseLong(reminderIdStr); } catch (NumberFormatException nfe) { /* ignore, will log later if needed */ }

                            reminder = JacksonUtils.fromJson(reminderJson, SimpleReminder.class);
                            if (reminder == null || reminder.getId() == null) {
                                String errorMsg = "提醒数据解析失败或ID为空: " + reminderJson;
                                log.error(errorMsg);
                                details = errorMsg;
                                saveHistory(null, null, "SIMPLE", parsedReminderId, null, null, "EMAIL", null, "FAILURE", details, reminderJson, parsedReminderId, OffsetDateTime.now());
                                return;
                            }
                            // 如果 reminder 解析成功，优先使用 reminder.getId()
                            parsedReminderId = reminder.getId(); 

                            log.info("正在处理提醒 - ID:{}, 标题:{}, 目标用户ID:{}", 
                                reminder.getId(), reminder.getTitle(), reminder.getToUserId());

                            if (gmailSender != null && userCacheService != null && reminder.getToUserId() != null) {
                                try {
                                    userProfile = userCacheService.getUserProfileById(reminder.getToUserId()); 
                                } catch (Exception e) {
                                    String errorMsg = "获取用户信息失败 (ID: " + reminder.getToUserId() + ") - 提醒ID: " + reminder.getId() + ", 错误: " + e.getMessage();
                                    log.error(errorMsg);
                                    details = errorMsg;
                                }

                                if (userProfile != null && userProfile.getEmail() != null && !userProfile.getEmail().isEmpty()) {
                                    try {
                                        String emailSubject = reminder.getTitle();
                                        String emailBody = "尊敬的" + (userProfile.getNickname() != null ? userProfile.getNickname() : "用户") + "，<br/>您的提醒事项：" + reminder.getTitle(); 
                                        
                                        gmailSender.sendEmail(userProfile.getEmail(), emailSubject, emailBody);
                                        status = "SUCCESS";
                                        details = "邮件提醒已成功发送至 " + userProfile.getEmail();
                                        log.info("邮件提醒已成功发送至 {} (用户ID: {}) - 提醒ID: {}", userProfile.getEmail(), reminder.getToUserId(), reminder.getId());
                                    } catch (Exception mailEx) {
                                        String errorMsg = "发送邮件提醒失败 - 提醒ID: " + reminder.getId() + ", 用户ID: " + reminder.getToUserId() + ", 邮箱: " + userProfile.getEmail() + ", 错误: " + mailEx.getMessage();
                                        log.error(errorMsg);
                                        details = errorMsg;
                                    }
                                } else {
                                    if (userProfile == null && reminder.getToUserId() != null) { // 检查 toUserId 是否存在以避免 NPE
                                        details = "未能获取到用户信息 (ID: " + reminder.getToUserId() + ")，无法发送邮件提醒";
                                    } else if (reminder.getToUserId() == null) {
                                        details = "提醒的接收用户ID (toUserId) 为空，无法发送邮件";
                                    } else { // userProfile is not null, but email is null or empty
                                        details = "用户邮箱为空 (ID: " + reminder.getToUserId() + ")，无法发送邮件提醒";
                                    }
                                    log.warn("{}, 提醒ID: {}",details, reminder.getId());
                                }
                            } else {
                                String missingComponent = "";
                                if (gmailSender == null) missingComponent += "GmailSender未注入; ";
                                if (userCacheService == null) missingComponent += "UserCacheService未注入; ";
                                if (reminder.getToUserId() == null) missingComponent += "接收用户ID为空; ";
                                details = "无法发送邮件提醒，前置条件不足: " + missingComponent.trim();
                                log.warn("{}, 提醒ID: {}, 标题: {}", details, reminder.getId(), reminder.getTitle());
                            }
                        } catch (Exception e) { // Catch-all for other unexpected errors during processing
                            String errorMsg = "发送提醒处理时发生未知错误 - 原始提醒ID字符串:" + reminderIdStr + ", 解析后ID(如成功):" + (parsedReminderId != null ? parsedReminderId : "N/A") + ", 错误: " + e.getMessage();
                            log.error(errorMsg, e);
                            details = errorMsg;
                            // status 默认为 FAILURE
                        } finally {
                            // 确保 parsedReminderId 被正确传递，即使 reminder 对象是 null
                            Long finalIdForHistory = (reminder != null && reminder.getId() != null) ? reminder.getId() : parsedReminderId;
                            saveHistory(reminder, userProfile, "SIMPLE", finalIdForHistory, status, details, OffsetDateTime.now());
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

    // 抽取保存历史记录的逻辑为一个私有方法
    private void saveHistory(SimpleReminder reminder, UserProfileDto userProfile, String triggerType, Long triggerId, String status, String details, OffsetDateTime executedAt) {
        if (historyRepository == null) {
            log.error("ReminderExecutionHistoryRepository 未注入，无法保存执行历史！提醒ID (如存在): {}", triggerId);
            return;
        }
        try {
            ReminderExecutionHistory history = new ReminderExecutionHistory();
            history.setExecutedAt(executedAt);
            history.setTriggeringReminderType(triggerType);
            history.setStatus(status);
            history.setDetails(details);
            history.setActualReminderMethod("EMAIL"); // Default for this job

            if (reminder != null) {
                history.setTriggeringReminderId(reminder.getId());
                history.setFromUserId(reminder.getFromUserId()); 
                history.setToUserId(reminder.getToUserId());
                history.setTitle(reminder.getTitle());
                history.setDescription(reminder.getDescription());
                history.setScheduledEventTime(reminder.getEventTime());
            } else if (triggerId != null) { 
                 history.setTriggeringReminderId(triggerId);
                 // Set defaults for other fields if reminder is null
                 history.setFromUserId(-1L); 
                 history.setToUserId(-1L); 
                 // title, description, scheduledEventTime will be null by default if not set
            } else {
                // Case where both reminder and triggerId are null (should be rare after parsing logic improvement)
                history.setTriggeringReminderId(-1L); 
                history.setFromUserId(-1L);
                history.setToUserId(-1L);
            }
            
            historyRepository.save(history);
            log.info("提醒执行历史已保存 - 提醒ID: {}, 状态: {}", history.getTriggeringReminderId(), status);
        } catch (Exception e) {
            log.error("保存提醒执行历史失败 - 提醒ID: {}, 错误: {}", triggerId, e.getMessage(), e);
        }
    }

    // Overloaded method for cases where SimpleReminder object could not be parsed
    // Ensures all relevant info from ReminderExecutionHistory is considered.
    private void saveHistory(Long fromUserId, Long toUserId, String triggerType, Long triggerId, 
                             String title, String description, String actualMethod, 
                             OffsetDateTime scheduledTime, String status, String details, 
                             String originalReminderJson, Long parsedReminderId, // parsedReminderId is the ID from string, might be same as triggerId
                             OffsetDateTime executedAt) {
         if (historyRepository == null) {
            log.error("ReminderExecutionHistoryRepository 未注入，无法保存执行历史！触发ID: {}", (triggerId != null ? triggerId : parsedReminderId));
            return;
        }
        try {
            ReminderExecutionHistory history = new ReminderExecutionHistory();
            history.setExecutedAt(executedAt);
            history.setTriggeringReminderType(triggerType);
            
            Long finalTriggerId = triggerId; // Primary ID source for this overload
            if (finalTriggerId == null && parsedReminderId != null) { 
                finalTriggerId = parsedReminderId;
            }
            if (finalTriggerId == null) { // Fallback if all ID sources are null
                finalTriggerId = -1L;
            }
            history.setTriggeringReminderId(finalTriggerId);

            history.setFromUserId(fromUserId != null ? fromUserId : -1L);
            history.setToUserId(toUserId != null ? toUserId : -1L);
            history.setTitle(title); 
            history.setDescription(description);
            history.setActualReminderMethod(actualMethod != null ? actualMethod : "EMAIL");
            history.setScheduledEventTime(scheduledTime);
            history.setStatus(status);
            
            String fullDetails = details;
            if (originalReminderJson != null && !originalReminderJson.isEmpty()) {
                String separator = (fullDetails != null && !fullDetails.isEmpty()) ? " | " : "";
                fullDetails = (fullDetails != null ? fullDetails : "") + separator + "Original JSON: " + originalReminderJson;
            }
            history.setDetails(fullDetails);

            historyRepository.save(history);
            log.info("提醒执行历史(特定情况)已保存 - 触发ID: {}, 状态: {}", finalTriggerId, status);
        } catch (Exception e) {
            log.error("保存提醒执行历史(特定情况)失败 - 触发ID: {}, 错误: {}", (triggerId != null ? triggerId : parsedReminderId), e.getMessage(), e);
        }
    }
} 