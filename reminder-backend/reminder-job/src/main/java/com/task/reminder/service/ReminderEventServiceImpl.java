package com.task.reminder.service;

import com.common.reminder.model.ComplexReminder;
import com.common.reminder.model.SimpleReminder;
import com.task.reminder.repository.ComplexReminderRepository;
import com.task.reminder.repository.SimpleReminderRepository;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.quartz.Trigger;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.time.LocalDate;

@Service
public class ReminderEventServiceImpl /* implements ReminderService */ {

    private static final Logger log = LoggerFactory.getLogger(ReminderEventServiceImpl.class);

    /**
     * 用户最大复杂提醒数量限制
     */
    private static final int MAX_COMPLEX_REMINDERS_PER_USER = 20;

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
        return simpleReminderRepository.save(simpleReminder);
    }

    public Optional<SimpleReminder> getSimpleReminderById(Long id) {
        return simpleReminderRepository.findById(id);
    }

    public List<SimpleReminder> getAllSimpleReminders() {
        return simpleReminderRepository.findAll();
    }

    /**
     * 按年月查询简单提醒
     * 在查询前确保复杂任务已生成该月份的简单任务
     * 
     * @param year 年份
     * @param month 月份(1-12)
     * @return 指定月份的简单提醒列表
     */
    @Transactional
    public List<SimpleReminder> getSimpleRemindersByYearMonth(int year, int month) {
        log.info("查询 {}-{} 月份的所有简单提醒", year, month);
        
        // 验证月份有效性
        if (month < 1 || month > 12) {
            log.error("无效的月份: {}", month);
            throw new IllegalArgumentException("月份必须在1-12之间");
        }
        
        // 首先确保所有复杂任务都已生成该月份的简单任务
        ensureComplexRemindersGenerated(year, month);
        
        return simpleReminderRepository.findByYearMonth(year, month);
    }
    
    /**
     * 确保复杂任务已经生成了指定月份的简单任务
     * 
     * @param year 年份
     * @param month 月份(1-12)
     */
    private void ensureComplexRemindersGenerated(int year, int month) {
        log.info("确保 {}-{} 月份的复杂任务已生成简单任务", year, month);
        
        // 计算当前查询月份的YYYYMM格式
        int queryYearMonth = year * 100 + month;
        
        // 查询所有lastGeneratedYm小于等于查询月份的复杂任务
        List<ComplexReminder> templatesNeedUpdate = complexReminderRepository
                .findByLastGeneratedYmLessThanOrLastGeneratedYmIsNull(queryYearMonth + 1);
        
        if (templatesNeedUpdate.isEmpty()) {
            log.info("没有需要生成 {}-{} 月份提醒的复杂任务模板", year, month);
            return;
        }
        
        log.info("找到 {} 个需要生成 {}-{} 月份提醒的复杂任务模板", 
                templatesNeedUpdate.size(), year, month);
        
        // 为每个需要更新的复杂任务生成简单任务
        for (ComplexReminder template : templatesNeedUpdate) {
            try {
                // 计算需要生成到的月份数
                int monthsAhead;
                LocalDate now = LocalDate.now();
                int currentYear = now.getYear();
                int currentMonth = now.getMonthValue();
                
                // 如果查询的月份是当前月份或未来月份
                if (year > currentYear || (year == currentYear && month >= currentMonth)) {
                    // 计算从当前月到查询月的月数差
                    int monthDiff = (year - currentYear) * 12 + (month - currentMonth);
                    // 确保至少生成到查询月份
                    monthsAhead = Math.max(monthDiff + 1, 1);
                } else {
                    // 如果是查询历史月份，只需确保该任务的lastGeneratedYm已经覆盖了查询月份
                    if (template.getLastGeneratedYm() == null || template.getLastGeneratedYm() < queryYearMonth) {
                        // 如果没有覆盖，则更新lastGeneratedYm
                        template.setLastGeneratedYm(queryYearMonth);
                        complexReminderRepository.save(template);
                        log.info("已更新复杂任务ID: {} 的lastGeneratedYm为: {}", template.getId(), queryYearMonth);
                    }
                    continue; // 跳过历史月份的生成
                }
                
                // 使用服务生成简单任务
                generateSimpleRemindersForMonths(template, monthsAhead);
                log.info("为复杂任务ID: {} 生成了 {}-{} 月份的简单任务", 
                        template.getId(), year, month);
                
            } catch (Exception e) {
                log.error("为复杂任务ID: {} 生成 {}-{} 月份简单任务时出错", 
                        template.getId(), year, month, e);
                // 继续处理其他模板
            }
        }
    }

    /**
     * 按年月和用户查询简单提醒
     * 在查询前确保复杂任务已生成该月份的简单任务
     * 
     * @param year 年份
     * @param month 月份(1-12)
     * @param userId 用户ID
     * @return 指定用户在指定月份的简单提醒列表
     */
    @Transactional
    public List<SimpleReminder> getSimpleRemindersByYearMonthAndUser(int year, int month, Long userId) {
        log.info("查询用户ID: {} 在 {}-{} 月份的所有简单提醒", userId, year, month);
        
        // 验证月份有效性
        if (month < 1 || month > 12) {
            log.error("无效的月份: {}", month);
            throw new IllegalArgumentException("月份必须在1-12之间");
        }
        
        if (userId == null) {
            log.error("用户ID不能为空");
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        // 首先确保所有复杂任务都已生成该月份的简单任务
        ensureComplexRemindersGenerated(year, month);
        
        return simpleReminderRepository.findByYearMonthAndUserId(year, month, userId);
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
            // 从Quartz中删除任务和触发器
            scheduler.deleteJob(jobKey);
            log.info("Successfully deleted Quartz job for reminder ID: {}", id);
        } catch (SchedulerException e) {
            log.error("Failed to delete Quartz job for reminder ID: {}", id, e);
            // 继续删除数据库记录，即使Quartz失败
        }
        // 从数据库中删除提醒
        simpleReminderRepository.deleteById(id);
    }

    private JobDetail buildJobDetail(SimpleReminder simpleReminder, Class<? extends Job> jobClass) {
        // 使用ID作为唯一标识创建JobKey
        JobKey jobKey = buildJobKey(simpleReminder.getId());

        return JobBuilder.newJob(jobClass)
                .withIdentity(jobKey)
                .withDescription("Reminder Job for: " + simpleReminder.getTitle())
                .usingJobData("reminderId", simpleReminder.getId())
                .usingJobData("reminderType", simpleReminder.getReminderType().toString())
                .storeDurably(false)  // 任务执行完成后不需要持久化
                .requestRecovery(true)  // 如果执行过程中系统崩溃，恢复后尝试再次执行
                .build();
    }

    private JobKey buildJobKey(Long simpleReminderId) {
        return new JobKey("simpleReminder_" + simpleReminderId, REMINDER_JOB_GROUP);
    }

    private TriggerKey buildTriggerKey(Long simpleReminderId) {
        return new TriggerKey("simpleReminderTrigger_" + simpleReminderId, REMINDER_TRIGGER_GROUP);
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

    // === 验证方法 ===

    /**
     * 验证用户复杂提醒数量限制
     * @param userId 用户ID
     * @throws IllegalStateException 当用户复杂提醒数量超过限制时抛出
     */
    private void validateUserComplexReminderLimit(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        long currentCount = complexReminderRepository.countByFromUserId(userId);

        if (currentCount >= MAX_COMPLEX_REMINDERS_PER_USER) {
            log.warn("用户[{}]尝试创建复杂提醒，但已达到最大限制。当前数量: {}, 最大限制: {}",
                    userId, currentCount, MAX_COMPLEX_REMINDERS_PER_USER);
            throw new IllegalStateException(
                String.format("用户复杂提醒数量已达到最大限制(%d条)，无法创建新的复杂提醒", MAX_COMPLEX_REMINDERS_PER_USER)
            );
        }

        log.debug("用户[{}]复杂提醒数量验证通过。当前数量: {}, 最大限制: {}",
                userId, currentCount, MAX_COMPLEX_REMINDERS_PER_USER);
    }

    @Transactional
    public ComplexReminder createComplexReminder(ComplexReminder complexReminder) {
        log.info("Creating complex reminder template: {}", complexReminder.getTitle());
        return complexReminderRepository.save(complexReminder);
    }

    /**
     * 创建复杂提醒并生成指定月数内的简单任务
     * 整个过程在一个事务中完成，保证数据一致性
     * 
     * @param complexReminder 要创建的复杂提醒
     * @param monthsAhead 要生成的简单任务的月数
     * @return 创建后的复杂提醒对象
     */
    @Transactional
    public ComplexReminder createComplexReminderWithSimpleReminders(ComplexReminder complexReminder, int monthsAhead) {
        log.info("创建复杂提醒并生成{}个月内的简单任务", monthsAhead);
        
        // 保存复杂提醒
        ComplexReminder savedReminder = createComplexReminder(complexReminder);
        
        // 生成简单任务
        List<SimpleReminder> generatedReminders = generateSimpleRemindersForMonths(savedReminder, monthsAhead);
        log.info("为复杂提醒ID: {} 成功生成 {} 个简单任务", savedReminder.getId(), generatedReminders.size());
        
        return savedReminder;
    }
    
    /**
     * 更新复杂提醒并重新生成指定月数内的简单任务
     * 整个过程在一个事务中完成，保证数据一致性
     * 
     * @param complexReminder 要更新的复杂提醒
     * @param monthsAhead 要生成的简单任务的月数
     * @return 更新后的复杂提醒对象
     */
    @Transactional
    public ComplexReminder updateComplexReminderWithSimpleReminders(ComplexReminder complexReminder, int monthsAhead) {
        log.info("更新复杂提醒并生成{}个月内的简单任务", monthsAhead);
        
        // 先删除与该复杂提醒相关的所有简单任务
        int deletedCount = simpleReminderRepository.deleteByOriginatingComplexReminderId(complexReminder.getId());
        log.info("已删除与复杂提醒ID: {} 相关的 {} 个简单任务", complexReminder.getId(), deletedCount);
        
        // 保存更新后的复杂提醒
        ComplexReminder updatedReminder = createComplexReminder(complexReminder);
        
        // 生成简单任务
        List<SimpleReminder> generatedReminders = generateSimpleRemindersForMonths(updatedReminder, monthsAhead);
        log.info("为更新的复杂提醒ID: {} 成功生成 {} 个简单任务", updatedReminder.getId(), generatedReminders.size());
        
        return updatedReminder;
    }

    /**
     * 根据复杂提醒生成指定月数内的简单任务
     * 生成的任务需要在validFrom到validUntil这个时间范围内
     * 并且最多生成maxExecutions条任务
     * 生成到指定月份的月底
     * 
     * @param complexReminder 复杂提醒对象
     * @param monthsAhead 要生成的月数
     * @return 生成的简单任务列表
     */
    @Transactional
    public List<SimpleReminder> generateSimpleRemindersForMonths(ComplexReminder complexReminder, int monthsAhead) {
        log.info("为复杂提醒ID: {} 生成{}个月内的简单任务", complexReminder.getId(), monthsAhead);
        
        List<SimpleReminder> generatedReminders = new ArrayList<>();
        String cronExpression = complexReminder.getCronExpression();
        
        // 检查cron表达式是否有效
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            log.error("复杂提醒ID: {} 的CRON表达式为空", complexReminder.getId());
            return generatedReminders;
        }
        
        // 标准化cron表达式（添加秒字段如果没有）
        if (cronExpression.split("\\s+").length == 5) {
            cronExpression = "0 " + cronExpression;
        }
        
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            
            // 设置起始时间（当前时间或validFrom，取较晚者）
            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime startTime = now;
            
            if (complexReminder.getValidFrom() != null) {
                ZonedDateTime validFromTime = complexReminder.getValidFrom().atStartOfDay(ZoneId.systemDefault());
                if (validFromTime.isAfter(now)) {
                    startTime = validFromTime;
                }
            }
            
            // 计算目标月份（当前月份+monthsAhead）
            int targetYear = now.getYear() + (now.getMonthValue() + monthsAhead - 1) / 12;
            int targetMonth = (now.getMonthValue() + monthsAhead - 1) % 12 + 1;
            
            // 计算目标月份的最后一天
            LocalDate lastDayOfTargetMonth = LocalDate.of(targetYear, targetMonth, 1)
                .plusMonths(1).minusDays(1);
            
            // 设置结束时间为目标月份的最后一天的23:59:59
            ZonedDateTime endTime = lastDayOfTargetMonth.atTime(23, 59, 59).atZone(ZoneId.systemDefault());
            
            // 如果有validUntil且在目标月份结束之前，则使用validUntil
            if (complexReminder.getValidUntil() != null) {
                ZonedDateTime validUntilTime = complexReminder.getValidUntil().atTime(23, 59, 59).atZone(ZoneId.systemDefault());
                if (validUntilTime.isBefore(endTime)) {
                    endTime = validUntilTime;
                }
            }
            
            log.info("为复杂提醒ID: {} 生成简单任务的时间范围: {} 至 {}", 
                     complexReminder.getId(), startTime, endTime);
            
            // 开始计算执行时间并生成简单任务
            ZonedDateTime nextTime = startTime;
            int count = 0;
            Integer maxExecutions = complexReminder.getMaxExecutions();
            
            while (true) {
                // 计算下一个执行时间
                nextTime = cron.next(nextTime);
                
                // 如果超出了指定范围或validUntil，则停止
                if (nextTime == null || nextTime.isAfter(endTime)) {
                    break;
                }
                
                // 如果已经达到最大执行次数限制，则停止
                if (maxExecutions != null && count >= maxExecutions) {
                    break;
                }
                
                // 转换为OffsetDateTime
                OffsetDateTime nextExecutionTime = nextTime.toOffsetDateTime();
                
                // 检查是否已经存在相同时间的简单任务
                boolean exists = simpleReminderRepository.existsByOriginatingComplexReminderIdAndEventTime(
                        complexReminder.getId(), nextExecutionTime);
                
                if (!exists) {
                    // 创建简单任务
                    SimpleReminder simpleReminder = createSimpleReminderFromTemplate(complexReminder, nextExecutionTime);
                    SimpleReminder savedReminder = createSimpleReminder(simpleReminder);
                    generatedReminders.add(savedReminder);
                    count++;
                    
                    log.info("已生成SimpleReminder (ID: {}) 执行时间: {}", 
                             savedReminder.getId(), nextExecutionTime);
                }
            }
            
            // 更新lastGeneratedYm字段 - 使用目标月份
            if (!generatedReminders.isEmpty()) {
                // 计算目标年月 (格式 YYYYMM)
                int targetYearMonth = targetYear * 100 + targetMonth;
                
                // 如果有validUntil且在目标月份之前，则使用validUntil的年月
                if (complexReminder.getValidUntil() != null) {
                    LocalDate validUntil = complexReminder.getValidUntil();
                    int validUntilYm = validUntil.getYear() * 100 + validUntil.getMonthValue();
                    if (validUntilYm < targetYearMonth) {
                        targetYearMonth = validUntilYm;
                    }
                }
                
                // 更新lastGeneratedYm字段
                complexReminder.setLastGeneratedYm(targetYearMonth);
                complexReminderRepository.save(complexReminder);
                log.info("更新复杂提醒ID: {} 的lastGeneratedYm为: {}", complexReminder.getId(), targetYearMonth);
            }
            
            log.info("为复杂提醒ID: {} 成功生成 {} 个简单任务", complexReminder.getId(), generatedReminders.size());
            return generatedReminders;
            
        } catch (Exception e) {
            log.error("为复杂提醒ID: {} 生成简单任务时出错: {}", complexReminder.getId(), e.getMessage());
            return generatedReminders;
        }
    }

    /**
     * 根据复杂提醒生成三个月内的简单任务
     * 生成的任务需要在validFrom到validUntil这个时间范围内
     * 并且最多生成maxExecutions条任务
     * 
     * @param complexReminder 复杂提醒对象
     * @return 生成的简单任务列表
     */
    @Transactional
    public List<SimpleReminder> generateSimpleRemindersForThreeMonths(ComplexReminder complexReminder) {
        // 调用新的方法，默认生成三个月的简单任务
        return generateSimpleRemindersForMonths(complexReminder, 3);
    }

    /**
     * 从复杂提醒模板创建简单提醒实例
     */
    private SimpleReminder createSimpleReminderFromTemplate(ComplexReminder template, OffsetDateTime eventTime) {
        SimpleReminder instance = new SimpleReminder();
        instance.setFromUserId(template.getFromUserId());
        instance.setToUserId(template.getToUserId());
        instance.setTitle(template.getTitle()); // 直接使用模板标题
        instance.setDescription(template.getDescription()); // 使用模板描述
        instance.setEventTime(eventTime); // 计算出的执行时间
        instance.setReminderType(template.getReminderType());
        instance.setOriginatingComplexReminderId(template.getId()); // 链接回模板
        // createdAt和updatedAt将由Hibernate自动设置
        return instance;
    }

    /**
     * 删除与指定复杂提醒ID相关的所有简单任务
     * 
     * @param complexReminderId 复杂提醒ID
     * @return 删除的记录数
     */
    @Transactional
    public int deleteSimpleRemindersByComplexReminderId(Long complexReminderId) {
        log.info("删除与复杂提醒ID: {} 相关的所有简单任务", complexReminderId);
        return simpleReminderRepository.deleteByOriginatingComplexReminderId(complexReminderId);
    }

    /**
     * 删除复杂提醒及其关联的所有简单提醒
     * 整个操作在一个事务中完成，确保原子性
     * 
     * @param complexReminderId 要删除的复杂提醒ID
     * @return 删除的简单提醒数量
     */
    @Transactional
    public int deleteComplexReminderWithRelatedSimpleReminders(Long complexReminderId) {
        log.info("删除复杂提醒ID: {} 及其关联的所有简单任务", complexReminderId);
        
        // 先删除关联的简单提醒
        int deletedCount = simpleReminderRepository.deleteByOriginatingComplexReminderId(complexReminderId);
        log.info("已删除与复杂提醒ID: {} 相关的 {} 个简单任务", complexReminderId, deletedCount);
        
        // 检查复杂提醒是否存在
        boolean exists = complexReminderRepository.existsById(complexReminderId);
        if (exists) {
            // 删除复杂提醒
            complexReminderRepository.deleteById(complexReminderId);
            log.info("已删除复杂提醒ID: {}", complexReminderId);
        } else {
            log.warn("无法删除不存在的复杂提醒ID: {}", complexReminderId);
        }
        
        return deletedCount;
    }

    /**
     * 获取未来1分钟内需要触发的提醒事项
     * 
     * @return 未来1分钟内的提醒事项列表
     */
    public List<SimpleReminder> getNextMinuteReminders() {
        log.info("获取未来1分钟内的提醒事项");
        
        // 获取当前时间
        OffsetDateTime now = OffsetDateTime.now();
        // 计算1分钟后的时间
        OffsetDateTime oneMinuteLater = now.plusMinutes(1);
        
        // 查询在这个时间范围内的提醒事项
        List<SimpleReminder> reminders = simpleReminderRepository.findByEventTimeBetween(now, oneMinuteLater);
        
        log.info("找到 {} 个未来1分钟内的提醒事项", reminders.size());
        return reminders;
    }
} 