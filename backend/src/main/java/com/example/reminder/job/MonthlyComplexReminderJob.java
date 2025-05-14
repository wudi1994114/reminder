package com.example.reminder.job;

import com.example.reminder.model.ComplexReminder;
import com.example.reminder.repository.ComplexReminderRepository;
import com.example.reminder.service.ReminderEventServiceImpl;

import lombok.extern.slf4j.Slf4j;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 月度定时任务，用于检查和生成未来3个月的提醒
 * 查询所有lastGeneratedYm小于当前月份+3的复杂任务，并补充生成简单任务
 */
@Slf4j
@Component
@DisallowConcurrentExecution // 防止多个实例同时运行
public class MonthlyComplexReminderJob implements Job {

    @Autowired
    private ComplexReminderRepository complexReminderRepository;

    @Autowired
    private ReminderEventServiceImpl reminderService;

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("正在运行MonthlyComplexReminderJob...");

        // 计算当前年月和目标年月（当前月+3）
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();
        
        // 计算目标年月（当前月+3）
        int targetYear = currentYear + (currentMonth + 3 - 1) / 12;
        int targetMonth = (currentMonth + 3 - 1) % 12 + 1;
        
        // 格式化为YYYYMM
        int targetYearMonth = targetYear * 100 + targetMonth;
        
        log.info("当前年月: {}, 目标年月: {}", currentYear * 100 + currentMonth, targetYearMonth);

        // 使用新的查询方法查询需要更新的复杂任务
        List<ComplexReminder> templatesNeedUpdate = complexReminderRepository
                .findByLastGeneratedYmLessThanOrLastGeneratedYmIsNull(targetYearMonth);
        
        if (templatesNeedUpdate.isEmpty()) {
            log.info("没有需要生成未来提醒的复杂任务模板。");
            return;
        }
        
        log.info("找到{}个需要生成未来提醒的复杂任务模板。", templatesNeedUpdate.size());
        
        // 为每个需要更新的复杂任务生成简单任务
        for (ComplexReminder template : templatesNeedUpdate) {
            try {
                int monthsAhead;
                
                if (template.getLastGeneratedYm() == null) {
                    // 如果之前从未生成过，则生成完整的3个月
                    monthsAhead = 3;
                } else {
                    // 计算当前lastGeneratedYm所对应的年和月
                    int lastGenYear = template.getLastGeneratedYm() / 100;
                    int lastGenMonth = template.getLastGeneratedYm() % 100;
                    
                    // 计算目标年月与上次生成年月之间的月份差
                    int monthDiff = (targetYear - lastGenYear) * 12 + (targetMonth - lastGenMonth);
                    monthsAhead = Math.max(1, monthDiff);
                }
                
                // 使用服务生成简单任务
                reminderService.generateSimpleRemindersForMonths(template, monthsAhead);
                log.info("为复杂任务ID: {} 生成了未来{}个月的简单任务", template.getId(), monthsAhead);
                
            } catch (Exception e) {
                log.error("为复杂任务ID: {} 生成简单任务时出错", template.getId(), e);
                // 继续处理其他模板
            }
        }
        
        log.info("MonthlyComplexReminderJob执行完成。");
    }
} 