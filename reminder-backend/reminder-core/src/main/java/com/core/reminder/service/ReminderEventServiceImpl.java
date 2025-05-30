package com.core.reminder.service;

import com.common.reminder.constant.ActivityAction;
import com.common.reminder.constant.ResourceType;
import com.common.reminder.model.ComplexReminder;
import com.common.reminder.model.SimpleReminder;
import com.core.reminder.aspect.ActivityLogAspect.LogActivity;
import com.core.reminder.repository.ComplexReminderRepository;
import com.core.reminder.repository.SimpleReminderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Service
public class ReminderEventServiceImpl /* implements ReminderService */ {

    private static final Logger log = LoggerFactory.getLogger(ReminderEventServiceImpl.class);

    private final SimpleReminderRepository simpleReminderRepository;
    private final ComplexReminderRepository complexReminderRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 缓存键前缀
    private static final String USER_MONTHLY_REMINDERS_KEY_PREFIX = "user:reminders:monthly:";
    private static final String USER_UPCOMING_REMINDERS_KEY_PREFIX = "user:reminders:upcoming:";
    
    // 缓存过期时间：7天
    private static final long CACHE_TTL_DAYS = 7;

    private static final String REMINDER_JOB_GROUP = "reminder-jobs";
    private static final String REMINDER_TRIGGER_GROUP = "reminder-triggers";

    @Autowired
    public ReminderEventServiceImpl(SimpleReminderRepository simpleReminderRepository,
                                    ComplexReminderRepository complexReminderRepository) {
        this.simpleReminderRepository = simpleReminderRepository;
        this.complexReminderRepository = complexReminderRepository;
    }

    // === 缓存相关方法 ===
    
    /**
     * 构建月度提醒缓存键
     */
    private String buildMonthlyRemindersKey(Long userId, int year, int month) {
        return USER_MONTHLY_REMINDERS_KEY_PREFIX + userId + ":" + year + ":" + String.format("%02d", month);
    }

    /**
     * 构建即将到来的提醒缓存键
     */
    private String buildUpcomingRemindersKey(Long userId) {
        return USER_UPCOMING_REMINDERS_KEY_PREFIX + userId;
    }

    /**
     * 缓存用户当月提醒数据
     */
    private void cacheUserMonthlyReminders(Long userId, int year, int month, List<SimpleReminder> reminders) {
        if (userId == null || reminders == null) {
            return;
        }
        
        String cacheKey = buildMonthlyRemindersKey(userId, year, month);
        
        try {
            redisTemplate.opsForValue().set(cacheKey, reminders, CACHE_TTL_DAYS, TimeUnit.DAYS);
            log.debug("已缓存用户[{}] {}-{} 月提醒数据，共{}条，过期时间{}天", 
                     userId, year, month, reminders.size(), CACHE_TTL_DAYS);
        } catch (Exception e) {
            log.error("缓存用户[{}] {}-{} 月提醒数据失败", userId, year, month, e);
        }
    }

    /**
     * 缓存用户即将到来的提醒数据
     */
    private void cacheUserUpcomingReminders(Long userId, List<SimpleReminder> reminders) {
        if (userId == null || reminders == null) {
            return;
        }
        
        String cacheKey = buildUpcomingRemindersKey(userId);
        
        try {
            // 即将到来的提醒使用较短的缓存时间（1小时）
            redisTemplate.opsForValue().set(cacheKey, reminders, 1, TimeUnit.HOURS);
            log.debug("已缓存用户[{}] 即将到来的提醒数据，共{}条，过期时间1小时", userId, reminders.size());
        } catch (Exception e) {
            log.error("缓存用户[{}] 即将到来的提醒数据失败", userId, e);
        }
    }

    /**
     * 清除用户指定月份的提醒缓存
     */
    private void invalidateUserMonthlyReminders(Long userId, int year, int month) {
        if (userId == null) {
            return;
        }
        
        String cacheKey = buildMonthlyRemindersKey(userId, year, month);
        
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("已清除用户[{}] {}-{} 月提醒缓存", userId, year, month);
            }
        } catch (Exception e) {
            log.error("清除用户[{}] {}-{} 月提醒缓存失败", userId, year, month, e);
        }
    }

    /**
     * 清除用户即将到来的提醒缓存
     */
    private void invalidateUserUpcomingReminders(Long userId) {
        if (userId == null) {
            return;
        }
        
        String cacheKey = buildUpcomingRemindersKey(userId);
        
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("已清除用户[{}] 即将到来的提醒缓存", userId);
            }
        } catch (Exception e) {
            log.error("清除用户[{}] 即将到来的提醒缓存失败", userId, e);
        }
    }

    /**
     * 清除用户当前月份的提醒缓存（当提醒发生变化时调用）
     */
    private void invalidateUserCurrentMonthReminders(Long userId) {
        if (userId == null) {
            return;
        }
        
        LocalDate now = LocalDate.now();
        invalidateUserMonthlyReminders(userId, now.getYear(), now.getMonthValue());
        invalidateUserUpcomingReminders(userId);
    }

    /**
     * 清除用户所有提醒相关缓存
     */
    private void invalidateAllUserReminderCaches(Long userId) {
        if (userId == null) {
            return;
        }
        
        try {
            // 清除当前月份和前后几个月的缓存
            LocalDate now = LocalDate.now();
            for (int i = -2; i <= 2; i++) {
                LocalDate targetDate = now.plusMonths(i);
                invalidateUserMonthlyReminders(userId, targetDate.getYear(), targetDate.getMonthValue());
            }
            
            // 清除即将到来的提醒缓存
            invalidateUserUpcomingReminders(userId);
            
            log.info("已清除用户[{}] 所有提醒相关缓存", userId);
        } catch (Exception e) {
            log.error("清除用户[{}] 所有提醒缓存失败", userId, e);
        }
    }

    // === 业务方法（集成缓存功能） ===

    @Transactional
    @LogActivity(action = ActivityAction.REMINDER_CREATE, resourceType = ResourceType.REMINDER, 
                description = "创建简单提醒", async = true, logParams = false, logResult = true)
    public SimpleReminder createSimpleReminder(SimpleReminder simpleReminder) {
        log.info("Creating simple reminder: {}", simpleReminder.getTitle());
        SimpleReminder savedReminder = simpleReminderRepository.save(simpleReminder);
        
        // 清除相关用户的缓存
        if (savedReminder.getToUserId() != null) {
            invalidateUserCurrentMonthReminders(savedReminder.getToUserId());
        }
        if (savedReminder.getFromUserId() != null && !savedReminder.getFromUserId().equals(savedReminder.getToUserId())) {
            invalidateUserCurrentMonthReminders(savedReminder.getFromUserId());
        }
        
        return savedReminder;
    }

    @LogActivity(action = ActivityAction.REMINDER_VIEW, resourceType = ResourceType.REMINDER, 
                description = "查看简单提醒详情", async = true)
    public Optional<SimpleReminder> getSimpleReminderById(Long id) {
        return simpleReminderRepository.findById(id);
    }

    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.REMINDER, 
                description = "获取用户所有简单提醒", async = true)
    public List<SimpleReminder> getAllSimpleReminders(Long userId) {
        return simpleReminderRepository.findByToUserId(userId);
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
    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.REMINDER, 
                description = "按年月查询简单提醒", async = true, logParams = true)
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
     * 按年月和用户查询简单提醒（优先从缓存获取）
     * 在查询前确保复杂任务已生成该月份的简单任务
     * 
     * @param year 年份
     * @param month 月份(1-12)
     * @param userId 用户ID
     * @return 指定用户在指定月份的简单提醒列表
     */
    @Transactional
    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.REMINDER, 
                description = "按年月和用户查询简单提醒", async = true, logParams = true)
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
        
        // 优先从缓存获取
        String cacheKey = buildMonthlyRemindersKey(userId, year, month);
        
        try {
            // 尝试从缓存获取
            @SuppressWarnings("unchecked")
            List<SimpleReminder> cachedReminders = (List<SimpleReminder>) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedReminders != null) {
                log.debug("缓存命中：用户[{}] {}-{} 月提醒数据，共{}条", userId, year, month, cachedReminders.size());
                return cachedReminders;
            }
            
            // 缓存未命中，从数据库获取
            log.debug("缓存未命中：从数据库获取用户[{}] {}-{} 月提醒数据", userId, year, month);
            List<SimpleReminder> reminders = simpleReminderRepository.findByYearMonthAndUserId(year, month, userId);
            
            // 缓存到Redis
            cacheUserMonthlyReminders(userId, year, month, reminders);
            
            return reminders;
            
        } catch (Exception e) {
            log.error("获取用户[{}] {}-{} 月提醒缓存时出错，降级到数据库查询", userId, year, month, e);
            // 降级到数据库查询
            return simpleReminderRepository.findByYearMonthAndUserId(year, month, userId);
        }
    }

    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.REMINDER, 
                description = "获取用户创建的简单提醒", async = true)
    public List<SimpleReminder> getSimpleRemindersByFromUser(Long userId) {
        return simpleReminderRepository.findByFromUserId(userId);
    }

    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.REMINDER, 
                description = "获取用户接收的简单提醒", async = true)
    public List<SimpleReminder> getSimpleRemindersByToUser(Long userId) {
        return simpleReminderRepository.findByToUserId(userId);
    }

    /**
     * 获取最近10个即将到来的提醒（优先从缓存获取）
     */
    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.REMINDER, 
                description = "获取即将到来的提醒", async = true)
    public List<SimpleReminder> getUpcomingReminders(Long userId) {
        String cacheKey = buildUpcomingRemindersKey(userId);
        
        try {
            // 尝试从缓存获取
            @SuppressWarnings("unchecked")
            List<SimpleReminder> cachedReminders = (List<SimpleReminder>) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedReminders != null) {
                log.debug("缓存命中：用户[{}] 即将到来的提醒数据，共{}条", userId, cachedReminders.size());
                return cachedReminders;
            }
            
            // 缓存未命中，从数据库获取
            log.debug("缓存未命中：从数据库获取用户[{}] 即将到来的提醒数据", userId);
            OffsetDateTime now = OffsetDateTime.now();
            List<SimpleReminder> reminders = simpleReminderRepository.findTop10ByToUserIdAndEventTimeAfterOrderByEventTimeAsc(userId, now);
            
            // 缓存到Redis（较短的过期时间，因为即将到来的提醒变化较快）
            cacheUserUpcomingReminders(userId, reminders);
            
            return reminders;
            
        } catch (Exception e) {
            log.error("获取用户[{}] 即将到来的提醒缓存时出错，降级到数据库查询", userId, e);
            // 降级到数据库查询
            OffsetDateTime now = OffsetDateTime.now();
            return simpleReminderRepository.findTop10ByToUserIdAndEventTimeAfterOrderByEventTimeAsc(userId, now);
        }
    }

    @Transactional
    @LogActivity(action = ActivityAction.REMINDER_DELETE, resourceType = ResourceType.REMINDER, 
                description = "删除简单提醒", async = true, logParams = true)
    public void deleteSimpleReminder(Long id) {
        log.info("Deleting simple reminder with ID: {}", id);
        
        // 先获取要删除的提醒信息，用于清除缓存
        Optional<SimpleReminder> reminderOpt = simpleReminderRepository.findById(id);
        
        simpleReminderRepository.deleteById(id);
        
        // 清除相关用户的缓存
        if (reminderOpt.isPresent()) {
            SimpleReminder reminder = reminderOpt.get();
            if (reminder.getToUserId() != null) {
                invalidateUserCurrentMonthReminders(reminder.getToUserId());
            }
            if (reminder.getFromUserId() != null && !reminder.getFromUserId().equals(reminder.getToUserId())) {
                invalidateUserCurrentMonthReminders(reminder.getFromUserId());
            }
        }
    }

    /**
     * 更新简单提醒事项并重新调度任务
     */
    @Transactional
    @LogActivity(action = ActivityAction.REMINDER_UPDATE, resourceType = ResourceType.REMINDER, 
                description = "更新简单提醒", async = true, logParams = false, logResult = true)
    public SimpleReminder updateSimpleReminder(SimpleReminder simpleReminder) {
        log.info("Updating simple reminder with ID: {}", simpleReminder.getId());

        SimpleReminder updatedReminder = simpleReminderRepository.save(simpleReminder);
        
        // 清除相关用户的缓存
        if (updatedReminder.getToUserId() != null) {
            invalidateUserCurrentMonthReminders(updatedReminder.getToUserId());
        }
        if (updatedReminder.getFromUserId() != null && !updatedReminder.getFromUserId().equals(updatedReminder.getToUserId())) {
            invalidateUserCurrentMonthReminders(updatedReminder.getFromUserId());
        }
        
        return updatedReminder;
    }

    @Transactional
    @LogActivity(action = ActivityAction.COMPLEX_REMINDER_CREATE, resourceType = ResourceType.COMPLEX_REMINDER, 
                description = "创建复杂提醒", async = true, logParams = false, logResult = true)
    public ComplexReminder createComplexReminder(ComplexReminder complexReminder) {
        log.info("Creating complex reminder template: {}", complexReminder.getTitle());
        ComplexReminder savedReminder = complexReminderRepository.save(complexReminder);
        
        // 清除相关用户的缓存
        if (savedReminder.getToUserId() != null) {
            invalidateUserCurrentMonthReminders(savedReminder.getToUserId());
        }
        if (savedReminder.getFromUserId() != null && !savedReminder.getFromUserId().equals(savedReminder.getToUserId())) {
            invalidateUserCurrentMonthReminders(savedReminder.getFromUserId());
        }
        
        return savedReminder;
    }

    @LogActivity(action = ActivityAction.REMINDER_VIEW, resourceType = ResourceType.COMPLEX_REMINDER, 
                description = "查看复杂提醒详情", async = true)
    public Optional<ComplexReminder> getComplexReminderById(Long id) {
        return complexReminderRepository.findById(id);
    }

    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.COMPLEX_REMINDER, 
                description = "获取所有复杂提醒", async = true)
    public List<ComplexReminder> getAllComplexReminders() {
        return complexReminderRepository.findAll();
    }

    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.COMPLEX_REMINDER, 
                description = "获取用户创建的复杂提醒", async = true)
    public List<ComplexReminder> getComplexRemindersByFromUser(Long userId) {
        return complexReminderRepository.findByFromUserId(userId);
    }

    @LogActivity(action = ActivityAction.API_ACCESS, resourceType = ResourceType.COMPLEX_REMINDER, 
                description = "获取用户接收的复杂提醒", async = true)
    public List<ComplexReminder> getComplexRemindersByToUser(Long userId) {
        return complexReminderRepository.findByToUserId(userId);
    }

    @Transactional
    @LogActivity(action = ActivityAction.COMPLEX_REMINDER_DELETE, resourceType = ResourceType.COMPLEX_REMINDER, 
                description = "删除复杂提醒", async = true, logParams = true)
    public void deleteComplexReminder(Long id) {
        log.info("Deleting complex reminder template with ID: {}", id);
        
        // 先获取要删除的复杂提醒信息，用于清除缓存
        Optional<ComplexReminder> reminderOpt = complexReminderRepository.findById(id);
        
        complexReminderRepository.deleteById(id);
        
        // 清除相关用户的缓存
        if (reminderOpt.isPresent()) {
            ComplexReminder reminder = reminderOpt.get();
            if (reminder.getToUserId() != null) {
                invalidateAllUserReminderCaches(reminder.getToUserId());
            }
            if (reminder.getFromUserId() != null && !reminder.getFromUserId().equals(reminder.getToUserId())) {
                invalidateAllUserReminderCaches(reminder.getFromUserId());
            }
        }
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
    @LogActivity(action = ActivityAction.COMPLEX_REMINDER_CREATE, resourceType = ResourceType.COMPLEX_REMINDER, 
                description = "创建复杂提醒并生成简单任务", async = true, logParams = true, logResult = true)
    public ComplexReminder createComplexReminderWithSimpleReminders(ComplexReminder complexReminder, int monthsAhead) {
        log.info("创建复杂提醒并生成{}个月内的简单任务", monthsAhead);
        
        // 保存复杂提醒
        ComplexReminder savedReminder = createComplexReminder(complexReminder);
        
        // 生成简单任务
        List<SimpleReminder> generatedReminders = generateSimpleRemindersForMonths(savedReminder, monthsAhead);
        log.info("为复杂提醒ID: {} 成功生成 {} 个简单任务", savedReminder.getId(), generatedReminders.size());
        
        // 清除相关用户的缓存（因为生成了新的简单任务）
        if (savedReminder.getToUserId() != null) {
            invalidateAllUserReminderCaches(savedReminder.getToUserId());
        }
        if (savedReminder.getFromUserId() != null && !savedReminder.getFromUserId().equals(savedReminder.getToUserId())) {
            invalidateAllUserReminderCaches(savedReminder.getFromUserId());
        }
        
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
    @LogActivity(action = ActivityAction.COMPLEX_REMINDER_UPDATE, resourceType = ResourceType.COMPLEX_REMINDER, 
                description = "更新复杂提醒并重新生成简单任务", async = true, logParams = true, logResult = true)
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
        
        // 清除相关用户的缓存（因为重新生成了简单任务）
        if (updatedReminder.getToUserId() != null) {
            invalidateAllUserReminderCaches(updatedReminder.getToUserId());
        }
        if (updatedReminder.getFromUserId() != null && !updatedReminder.getFromUserId().equals(updatedReminder.getToUserId())) {
            invalidateAllUserReminderCaches(updatedReminder.getFromUserId());
        }
        
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
    @LogActivity(action = ActivityAction.REMINDER_DELETE, resourceType = ResourceType.REMINDER, 
                description = "删除复杂提醒关联的简单任务", async = true, logParams = true)
    public int deleteSimpleRemindersByComplexReminderId(Long complexReminderId) {
        log.info("删除与复杂提醒ID: {} 相关的所有简单任务", complexReminderId);
        
        // 先获取复杂提醒信息，用于清除缓存
        Optional<ComplexReminder> complexReminderOpt = complexReminderRepository.findById(complexReminderId);
        
        int deletedCount = simpleReminderRepository.deleteByOriginatingComplexReminderId(complexReminderId);
        
        // 清除相关用户的缓存
        if (complexReminderOpt.isPresent()) {
            ComplexReminder complexReminder = complexReminderOpt.get();
            if (complexReminder.getToUserId() != null) {
                invalidateAllUserReminderCaches(complexReminder.getToUserId());
            }
            if (complexReminder.getFromUserId() != null && !complexReminder.getFromUserId().equals(complexReminder.getToUserId())) {
                invalidateAllUserReminderCaches(complexReminder.getFromUserId());
            }
        }
        
        return deletedCount;
    }

    /**
     * 删除复杂提醒及其关联的所有简单提醒
     * 整个操作在一个事务中完成，确保原子性
     * 
     * @param complexReminderId 要删除的复杂提醒ID
     * @return 删除的简单提醒数量
     */
    @Transactional
    @LogActivity(action = ActivityAction.COMPLEX_REMINDER_DELETE, resourceType = ResourceType.COMPLEX_REMINDER, 
                description = "删除复杂提醒及其关联的简单任务", async = true, logParams = true, logResult = true)
    public int deleteComplexReminderWithRelatedSimpleReminders(Long complexReminderId) {
        log.info("删除复杂提醒ID: {} 及其关联的所有简单任务", complexReminderId);
        
        // 先获取复杂提醒信息，用于清除缓存
        Optional<ComplexReminder> complexReminderOpt = complexReminderRepository.findById(complexReminderId);
        
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
        
        // 清除相关用户的缓存
        if (complexReminderOpt.isPresent()) {
            ComplexReminder complexReminder = complexReminderOpt.get();
            if (complexReminder.getToUserId() != null) {
                invalidateAllUserReminderCaches(complexReminder.getToUserId());
            }
            if (complexReminder.getFromUserId() != null && !complexReminder.getFromUserId().equals(complexReminder.getToUserId())) {
                invalidateAllUserReminderCaches(complexReminder.getFromUserId());
            }
        }
        
        return deletedCount;
    }

    /**
     * 获取未来1分钟内需要触发的提醒事项
     * 
     * @return 未来1分钟内的提醒事项列表
     */
    @LogActivity(action = ActivityAction.REMINDER_EXECUTE, resourceType = ResourceType.REMINDER, 
                description = "获取即将触发的提醒", async = true)
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