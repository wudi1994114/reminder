package com.wwmty.stream.consumer.handler;

import com.common.reminder.model.ComplexReminder;
import com.common.reminder.model.SimpleReminder;
import com.wwmty.stream.consumer.repository.ComplexReminderRepository;
import com.wwmty.stream.consumer.repository.SimpleReminderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.scheduling.support.CronExpression;

/**
 * 复杂提醒更新事件处理器
 * 处理通过Stream发送的复杂提醒更新请求
 */
@Slf4j
@Component
public class ComplexReminderUpdateHandler implements StreamEventHandler {

    @Autowired
    private ComplexReminderRepository complexReminderRepository;

    @Autowired
    private SimpleReminderRepository simpleReminderRepository;

    @Override
    public String getCommand() {
        return "UPDATE_COMPLEX_REMINDER";
    }

    @Override
    @Transactional
    public void handle(MapRecord<String, String, String> record) {
        log.info("开始处理复杂提醒更新事件 - 消息ID: {}", record.getId());
        
        try {
            Map<String, String> data = record.getValue();
            
            // 提取事件数据
            Long complexReminderId = Long.parseLong(data.get("complexReminderId"));
            int monthsAhead = Integer.parseInt(data.get("monthsAhead"));
            Long userId = Long.parseLong(data.get("userId"));
            
            log.info("处理复杂提醒更新 - ID: {}, 月数: {}, 用户: {}", complexReminderId, monthsAhead, userId);
            
            // 查询复杂提醒
            Optional<ComplexReminder> complexReminderOpt = complexReminderRepository.findById(complexReminderId);
            if (!complexReminderOpt.isPresent()) {
                log.error("复杂提醒不存在 - ID: {}", complexReminderId);
                return;
            }
            
            ComplexReminder complexReminder = complexReminderOpt.get();
            
            // 验证用户权限
            if (!complexReminder.getFromUserId().equals(userId)) {
                log.error("用户无权限操作此复杂提醒 - 用户ID: {}, 复杂提醒ID: {}", userId, complexReminderId);
                return;
            }
            
            // 先删除与该复杂提醒相关的所有简单任务
            int deletedCount = simpleReminderRepository.deleteByOriginatingComplexReminderId(complexReminderId);
            log.info("已删除与复杂提醒ID: {} 相关的 {} 个简单任务", complexReminderId, deletedCount);
            
            // 重新生成简单提醒
            List<SimpleReminder> generatedReminders = generateSimpleRemindersForMonths(complexReminder, monthsAhead);
            
            log.info("成功重新生成 {} 个简单提醒 - 复杂提醒ID: {}", generatedReminders.size(), complexReminderId);
            
        } catch (Exception e) {
            log.error("处理复杂提醒更新事件失败 - 消息ID: {}", record.getId(), e);
            throw e; // 重新抛出异常以触发重试机制
        }
    }

    /**
     * 根据复杂提醒生成指定月数内的简单任务
     * 
     * @param complexReminder 复杂提醒对象
     * @param monthsAhead 要生成的月数
     * @return 生成的简单任务列表
     */
    private List<SimpleReminder> generateSimpleRemindersForMonths(ComplexReminder complexReminder, int monthsAhead) {
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
            
            // 使用中国时区(Asia/Shanghai)确保时间一致性
            ZoneId chinaZone = ZoneId.of("Asia/Shanghai");
            
            // 设置起始时间（当前时间或validFrom，取较晚者）
            ZonedDateTime now = ZonedDateTime.now(chinaZone);
            ZonedDateTime startTime = now;
            
            if (complexReminder.getValidFrom() != null) {
                ZonedDateTime validFromTime = complexReminder.getValidFrom().atStartOfDay(chinaZone);
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
            ZonedDateTime endTime = lastDayOfTargetMonth.atTime(23, 59, 59).atZone(chinaZone);
            
            // 如果有validUntil且在目标月份结束之前，则使用validUntil
            if (complexReminder.getValidUntil() != null) {
                ZonedDateTime validUntilTime = complexReminder.getValidUntil().atTime(23, 59, 59).atZone(chinaZone);
                if (validUntilTime.isBefore(endTime)) {
                    endTime = validUntilTime;
                }
            }
            
            log.info("为复杂提醒ID: {} 生成简单任务的时间范围: {} 至 {} (使用中国时区)", 
                     complexReminder.getId(), startTime, endTime);
            
            // 开始计算执行时间并生成简单任务
            ZonedDateTime nextTime = startTime;
            int count = 0;
            Integer maxExecutions = complexReminder.getMaxExecutions();

            // 用于批量插入的列表
            List<SimpleReminder> batchToSave = new ArrayList<>();
            final int BATCH_SIZE = 100;

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

                // 转换为OffsetDateTime，确保使用中国时区的偏移量
                OffsetDateTime nextExecutionTime = nextTime.toOffsetDateTime();

                // 创建简单任务（先不保存，加入批量列表）
                SimpleReminder simpleReminder = createSimpleReminderFromTemplate(complexReminder, nextExecutionTime);
                batchToSave.add(simpleReminder);
                count++;

                log.debug("准备批量保存SimpleReminder，执行时间: {} (中国时区)", nextExecutionTime);

                // 当批量列表达到指定大小时，执行批量保存
                if (batchToSave.size() >= BATCH_SIZE) {
                    List<SimpleReminder> savedBatch = simpleReminderRepository.saveAll(batchToSave);
                    generatedReminders.addAll(savedBatch);
                    log.info("批量保存了 {} 个SimpleReminder", savedBatch.size());
                    batchToSave.clear();
                }
            }

            // 保存剩余的记录
            if (!batchToSave.isEmpty()) {
                List<SimpleReminder> savedBatch = simpleReminderRepository.saveAll(batchToSave);
                generatedReminders.addAll(savedBatch);
                log.info("批量保存了剩余的 {} 个SimpleReminder", savedBatch.size());
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
            
            return generatedReminders;
            
        } catch (Exception e) {
            log.error("为复杂提醒ID: {} 生成简单任务时出错: {}", complexReminder.getId(), e.getMessage());
            return generatedReminders;
        }
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
} 