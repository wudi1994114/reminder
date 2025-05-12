package com.example.reminder.job;

import com.example.reminder.model.ComplexReminder;
import com.example.reminder.model.SimpleReminder;
import com.example.reminder.repository.ComplexReminderRepository;
import com.example.reminder.repository.SimpleReminderRepository;
import com.example.reminder.service.ReminderEventServiceImpl; // 后续使用Service接口

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression; // 替换为CronExpression
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * 负责检查ComplexReminder模板并基于其CRON表达式
 * 在适当时间生成SimpleReminder实例的Quartz任务。
 */
@Component
@DisallowConcurrentExecution // 防止多个实例同时运行
public class ComplexReminderSchedulingJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(ComplexReminderSchedulingJob.class);

    @Autowired
    private ComplexReminderRepository complexReminderRepository;

    @Autowired
    private SimpleReminderRepository simpleReminderRepository; // 用于重复检查

    @Autowired
    private ReminderEventServiceImpl reminderService; // 注入服务以创建SimpleReminders

    @Override
    @Transactional // 包装在事务中确保原子性
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("正在运行ComplexReminderSchedulingJob...");

        List<ComplexReminder> templates = complexReminderRepository.findAll();
        if (templates.isEmpty()) {
            log.info("未找到复杂提醒模板。");
            return;
        }

        // 获取当前时间和下一分钟的开始时间（我们的检查窗口）
        // 使用Instant进行时间比较
        Instant now = Instant.now();
        Instant nextMinuteStart = now.plusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.MINUTES);


        for (ComplexReminder template : templates) {
            String cronExpression = template.getCronExpression();  // 移到try外部以便在catch块中也可以使用
            try {
                // 检查是否是缺少秒字段的5位CRON表达式
                if (cronExpression.split("\\s+").length == 5) {
                    // 添加秒字段（默认为0）
                    cronExpression = "0 " + cronExpression;
                }
                
                try {
                    // 使用CronExpression.parse验证表达式
                    CronExpression.parse(cronExpression);
                } catch (IllegalArgumentException e) {
                    log.warn("ComplexReminder ID: {}的CRON表达式'{}'无效。跳过。",
                             cronExpression, template.getId());
                    continue;
                }

                // 使用CronExpression替代CronSequenceGenerator
                CronExpression cron = CronExpression.parse(cronExpression);
                // 计算当前时间之后的下一次执行时间
                ZonedDateTime nowZoned = ZonedDateTime.ofInstant(now, ZoneId.systemDefault());
                ZonedDateTime nextExecutionTime = cron.next(nowZoned);
                Instant nextExecutionInstant = nextExecutionTime.toInstant();

                // 检查计算出的下一次执行时间是否在下一分钟内
                if (!nextExecutionInstant.isBefore(nextMinuteStart)) {
                   // log.debug("复杂提醒 {} ({}) 的下一次执行时间是 {}，不在下一分钟内。跳过。", template.getId(), template.getTitle(), nextExecutionInstant);
                    continue; // 不在下一分钟窗口内到期
                }

                log.info("复杂提醒 {} ('{}') 将在 {} 到期。检查重复并生成SimpleReminder。",
                         template.getId(), template.getTitle(), nextExecutionInstant);

                // 检查是否已经存在该模板在该精确时间的SimpleReminder
                // 这可以防止任务运行速度略快/慢于恰好1分钟而导致的重复
                OffsetDateTime nextExecutionOffsetTime = OffsetDateTime.ofInstant(nextExecutionInstant, ZoneId.systemDefault());

                // ** 精细的重复检查 **
                // 检查是否存在源自此复杂提醒且计划在计算出的下一执行时间的简单提醒。
                boolean exists = simpleReminderRepository.existsByOriginatingComplexReminderIdAndEventTime(template.getId(), nextExecutionOffsetTime);

                if (exists) {
                    log.warn("ComplexReminder ID {} 在 {} 时已存在SimpleReminder。跳过生成。", template.getId(), nextExecutionOffsetTime);
                    continue;
                }

                // 创建并保存SimpleReminder实例
                SimpleReminder instance = createSimpleReminderFromTemplate(template, nextExecutionOffsetTime);
                reminderService.createSimpleReminder(instance);
                log.info("成功从ComplexReminder ID: {} 生成并计划SimpleReminder (ID: {})", template.getId(), instance.getId());

            } catch (Exception e) {
                log.error("处理ComplexReminder ID: {} 时出错。Cron: '{}'",
                          template.getId(), cronExpression, e);
                // 继续处理其他模板
            }
        }
        log.info("ComplexReminderSchedulingJob完成。");
    }

    private SimpleReminder createSimpleReminderFromTemplate(ComplexReminder template, OffsetDateTime eventTime) {
        SimpleReminder instance = new SimpleReminder();
        instance.setFromUserId(template.getFromUserId());
        instance.setToUserId(template.getToUserId());
        instance.setTitle(template.getTitle()); // 直接使用模板标题
        instance.setDescription(template.getDescription()); // 使用模板描述
        instance.setEventTime(eventTime); // 计算出的下一次执行时间
        instance.setReminderType(template.getReminderType());
        instance.setOriginatingComplexReminderId(template.getId()); // 链接回模板
        // createdAt和updatedAt将由Hibernate自动设置
        return instance;
    }
} 