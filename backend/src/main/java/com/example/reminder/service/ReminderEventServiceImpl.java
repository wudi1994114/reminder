package com.example.reminder.service;

import com.example.reminder.job.ReminderJob;
import com.example.reminder.model.ComplexReminder;
import com.example.reminder.model.SimpleReminder;
import com.example.reminder.repository.ComplexReminderRepository;
import com.example.reminder.repository.SimpleReminderRepository;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class ReminderEventServiceImpl /* implements ReminderService */ {

    private static final Logger log = LoggerFactory.getLogger(ReminderEventServiceImpl.class);

    private final SimpleReminderRepository simpleReminderRepository;
    private final ComplexReminderRepository complexReminderRepository;
    private final Scheduler scheduler;

    private static final String REMINDER_JOB_GROUP = "reminder-jobs";
    private static final String REMINDER_TRIGGER_GROUP = "reminder-triggers";

    @Autowired
    public ReminderEventServiceImpl(SimpleReminderRepository simpleReminderRepository,
                                    ComplexReminderRepository complexReminderRepository,
                                    Scheduler scheduler) {
        this.simpleReminderRepository = simpleReminderRepository;
        this.complexReminderRepository = complexReminderRepository;
        this.scheduler = scheduler;
    }

    @Transactional
    public SimpleReminder createSimpleReminder(SimpleReminder simpleReminder) {
        log.info("Creating simple reminder: {}", simpleReminder.getTitle());
        SimpleReminder savedReminder = simpleReminderRepository.save(simpleReminder);
        scheduleSimpleReminderJob(savedReminder);
        return savedReminder;
    }

    public Optional<SimpleReminder> getSimpleReminderById(Long id) {
        return simpleReminderRepository.findById(id);
    }

    public List<SimpleReminder> getAllSimpleReminders() {
        return simpleReminderRepository.findAll();
    }

    public List<SimpleReminder> getSimpleRemindersByFromUser(Long userId) {
        return simpleReminderRepository.findByFromUserId(userId);
    }

    public List<SimpleReminder> getSimpleRemindersByToUser(Long userId) {
        return simpleReminderRepository.findByToUserId(userId);
    }

    /**
     * 获取最近10个即将到来的提醒
     */
    public List<SimpleReminder> getUpcomingReminders() {
        // 获取当前时间
        OffsetDateTime now = OffsetDateTime.now();
        // 查询即将到来的10个提醒
        return simpleReminderRepository.findTop10ByEventTimeAfterOrderByEventTimeAsc(now);
    }

    @Transactional
    public void deleteSimpleReminder(Long id) {
        log.info("Deleting simple reminder with ID: {}", id);
        JobKey jobKey = buildJobKey(id);
        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                log.info("Deleted associated Quartz job for simple reminder ID: {}", id);
            } else {
                log.warn("No Quartz job found for simple reminder ID: {}. It might have already executed or failed scheduling.", id);
            }
        } catch (SchedulerException e) {
            log.error("Error deleting Quartz job for simple reminder ID: {}", id, e);
        }
        simpleReminderRepository.deleteById(id);
    }

    /**
     * 更新简单提醒事项并重新调度任务
     */
    @Transactional
    public SimpleReminder updateSimpleReminder(SimpleReminder simpleReminder) {
        log.info("Updating simple reminder with ID: {}", simpleReminder.getId());
        
        // 首先删除现有的调度任务
        JobKey jobKey = buildJobKey(simpleReminder.getId());
        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                log.info("Deleted existing Quartz job for simple reminder ID: {}", simpleReminder.getId());
            }
        } catch (SchedulerException e) {
            log.error("Error deleting existing Quartz job for simple reminder ID: {}", simpleReminder.getId(), e);
            // 继续处理，不要因为调度删除错误阻止更新
        }
        
        // 保存更新后的提醒事项
        SimpleReminder updatedReminder = simpleReminderRepository.save(simpleReminder);
        
        // 为更新后的提醒创建新的调度
        scheduleSimpleReminderJob(updatedReminder);
        
        return updatedReminder;
    }

    @Transactional
    public ComplexReminder createComplexReminder(ComplexReminder complexReminder) {
        log.info("Creating complex reminder template: {}", complexReminder.getTitle());
        return complexReminderRepository.save(complexReminder);
    }

    public Optional<ComplexReminder> getComplexReminderById(Long id) {
        return complexReminderRepository.findById(id);
    }

    public List<ComplexReminder> getAllComplexReminders() {
        return complexReminderRepository.findAll();
    }

    public List<ComplexReminder> getComplexRemindersByFromUser(Long userId) {
        return complexReminderRepository.findByFromUserId(userId);
    }

    public List<ComplexReminder> getComplexRemindersByToUser(Long userId) {
        return complexReminderRepository.findByToUserId(userId);
    }

    @Transactional
    public void deleteComplexReminder(Long id) {
        log.info("Deleting complex reminder template with ID: {}", id);
        complexReminderRepository.deleteById(id);
    }

    private void scheduleSimpleReminderJob(SimpleReminder simpleReminder) {
        try {
            JobDetail jobDetail = buildJobDetail(simpleReminder);
            Trigger trigger = buildTrigger(simpleReminder, jobDetail);

            if (scheduler.checkExists(jobDetail.getKey())) {
                 log.warn("Job with key {} already exists. Skipping schedule.", jobDetail.getKey());
            } else {
                scheduler.scheduleJob(jobDetail, trigger);
                log.info("Scheduled job for simple reminder ID: {} with key {}", simpleReminder.getId(), jobDetail.getKey());
            }

        } catch (SchedulerException e) {
            log.error("Error scheduling job for simple reminder ID: {}", simpleReminder.getId(), e);
            throw new RuntimeException("Failed to schedule reminder job for SimpleReminder ID: " + simpleReminder.getId(), e);
        }
    }

    private JobKey buildJobKey(Long simpleReminderId) {
         return new JobKey("simpleReminder_" + simpleReminderId, REMINDER_JOB_GROUP);
    }

    private TriggerKey buildTriggerKey(Long simpleReminderId) {
        return new TriggerKey("simpleReminderTrigger_" + simpleReminderId, REMINDER_TRIGGER_GROUP);
    }

    private JobDetail buildJobDetail(SimpleReminder simpleReminder) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("simpleReminderId", simpleReminder.getId());
        jobDataMap.put("eventTitle", simpleReminder.getTitle());

        return JobBuilder.newJob(ReminderJob.class)
                .withIdentity(buildJobKey(simpleReminder.getId()))
                .withDescription("Reminder Job for SimpleReminder: " + simpleReminder.getTitle())
                .usingJobData(jobDataMap)
                .storeDurably(false)
                .build();
    }

    private Trigger buildTrigger(SimpleReminder simpleReminder, JobDetail jobDetail) {
         // Simple Triggers for SimpleReminder - run once at eventTime
         // Convert OffsetDateTime to Instant, then to Date for Quartz
         Instant eventInstant = simpleReminder.getEventTime().toInstant();
         Date startTime = Date.from(eventInstant);

        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(buildTriggerKey(simpleReminder.getId())) // Use consistent key
                .withDescription("Reminder Trigger for SimpleReminder: " + simpleReminder.getTitle())
                .startAt(startTime) // Trigger exactly once at the specified time
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withMisfireHandlingInstructionFireNow()) // If missed, fire immediately
                .build();
    }
} 