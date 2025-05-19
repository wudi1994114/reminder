package com.task.reminder.job;

import com.common.reminder.model.SimpleReminder;
import com.task.reminder.repository.SimpleReminderRepository;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 用于发送邮件提醒的Quartz任务实现类
 */
@Component
public class SimpleEmailReminderJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(SimpleEmailReminderJob.class);

    @Autowired
    private SimpleReminderRepository simpleReminderRepository;

    // 可以注入邮件发送服务
    // @Autowired
    // private EmailService emailService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        
        // 从JobDataMap中获取提醒ID
        Long reminderId = dataMap.getLong("reminderId");
        String reminderType = dataMap.getString("reminderType");
        
        log.info("执行提醒任务，ID: {}, 类型: {}", reminderId, reminderType);
        
        try {
            // 从数据库获取提醒详情
            Optional<SimpleReminder> reminderOpt = simpleReminderRepository.findById(reminderId);
            
            if (reminderOpt.isPresent()) {
                SimpleReminder reminder = reminderOpt.get();
                log.info("正在发送提醒: '{}' 到用户ID: {}", reminder.getTitle(), reminder.getToUserId());
                
                // 在这里实现发送邮件的逻辑，例如：
                // emailService.sendReminderEmail(reminder);
                
                // 如果需要，可以在这里更新提醒状态
                // reminder.setStatus("SENT");
                // simpleReminderRepository.save(reminder);
                
                log.info("成功发送提醒，ID: {}", reminderId);
            } else {
                log.warn("无法找到提醒，ID: {}", reminderId);
            }
        } catch (Exception e) {
            log.error("发送提醒时出错，ID: {}", reminderId, e);
            throw new JobExecutionException("发送提醒时出错", e);
        }
    }
} 