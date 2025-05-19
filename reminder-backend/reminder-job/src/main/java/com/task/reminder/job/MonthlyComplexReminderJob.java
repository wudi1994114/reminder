package com.task.reminder.job;

import com.common.reminder.model.ComplexReminder;
import com.task.reminder.repository.ComplexReminderRepository;
import com.task.reminder.service.ReminderEventServiceImpl;

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
 * 复杂提醒月度生成任务
 * 
 * 该任务负责定期（通常每月执行一次）检查并生成未来3个月的简单提醒实例。
 * 复杂提醒基于Cron表达式定义重复模式，但为了提高系统性能和查询效率，
 * 系统会提前将复杂提醒转换为具体的简单提醒实例，并存储在数据库中。
 * 
 * 工作流程：
 * 1. 计算当前日期和目标日期（当前月+3个月）
 * 2. 查询所有需要更新的复杂提醒模板（lastGeneratedYm小于目标日期或未设置）
 * 3. 对每个模板，计算需要生成的月数，并调用服务生成相应的简单提醒实例
 * 4. 更新复杂提醒的lastGeneratedYm字段，记录生成状态
 * 
 * 该任务通常由Quartz调度器按照每月执行一次的频率调用，确保系统中始终有足够的预生成提醒。
 * 使用@DisallowConcurrentExecution注解确保任务不会并发执行，避免生成重复提醒。
 */
@Slf4j  // 使用Lombok自动生成日志记录器
@Component  // 将类标记为Spring组件，使其可以被自动检测和注入
@DisallowConcurrentExecution  // 防止多个实例同时运行，避免生成重复数据
public class MonthlyComplexReminderJob implements Job {

    /**
     * 复杂提醒仓库，提供对复杂提醒的数据访问功能
     */
    @Autowired
    private ComplexReminderRepository complexReminderRepository;

    /**
     * 提醒事项服务，提供生成简单提醒实例的功能
     */
    @Autowired
    private ReminderEventServiceImpl reminderService;

    /**
     * 任务执行方法，由Quartz调度器在指定时间调用
     * 
     * 使用@Transactional注解确保整个执行过程是事务性的，
     * 要么全部成功，要么全部失败并回滚，保持数据一致性。
     * 
     * @param context 作业执行上下文，包含作业的相关信息
     * @throws JobExecutionException 当任务执行过程中出现错误时抛出
     */
    @Override
    @Transactional  // 确保整个执行过程在一个事务中，保持数据一致性
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("正在运行MonthlyComplexReminderJob...");

        // 计算当前年月和目标年月（当前月+3）
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();
        
        // 计算目标年月（当前月+3）
        // 这里需要处理跨年的情况，例如当前是10月，目标月就是明年1月
        int targetYear = currentYear + (currentMonth + 3 - 1) / 12;
        int targetMonth = (currentMonth + 3 - 1) % 12 + 1;
        
        // 格式化为YYYYMM形式，例如202312表示2023年12月
        int targetYearMonth = targetYear * 100 + targetMonth;
        
        log.info("当前年月: {}, 目标年月: {}", currentYear * 100 + currentMonth, targetYearMonth);

        // 查询所有需要更新的复杂提醒模板
        // 包括从未生成过简单提醒的模板（lastGeneratedYm为null）
        // 或者最后生成的年月小于目标年月的模板
        List<ComplexReminder> templatesNeedUpdate = complexReminderRepository
                .findByLastGeneratedYmLessThanOrLastGeneratedYmIsNull(targetYearMonth);
        
        if (templatesNeedUpdate.isEmpty()) {
            log.info("没有需要生成未来提醒的复杂任务模板。");
            return;
        }
        
        log.info("找到{}个需要生成未来提醒的复杂任务模板。", templatesNeedUpdate.size());
        
        // 为每个需要更新的复杂任务模板生成简单提醒实例
        for (ComplexReminder template : templatesNeedUpdate) {
            try {
                // 确定需要生成多少个月的提醒
                int monthsAhead;
                
                if (template.getLastGeneratedYm() == null) {
                    // 如果之前从未生成过提醒，则生成完整的3个月
                    monthsAhead = 3;
                } else {
                    // 计算当前lastGeneratedYm所对应的年和月
                    int lastGenYear = template.getLastGeneratedYm() / 100;
                    int lastGenMonth = template.getLastGeneratedYm() % 100;
                    
                    // 计算目标年月与上次生成年月之间的月份差距
                    // 例如：从202301到202304的差距是3个月
                    int monthDiff = (targetYear - lastGenYear) * 12 + (targetMonth - lastGenMonth);
                    // 至少生成1个月的提醒，即使计算结果小于1
                    monthsAhead = Math.max(1, monthDiff);
                }
                
                // 调用服务生成指定月数的简单提醒实例
                // 该方法会同时更新lastGeneratedYm字段
                reminderService.generateSimpleRemindersForMonths(template, monthsAhead);
                log.info("为复杂任务ID: {} 生成了未来{}个月的简单任务", template.getId(), monthsAhead);
                
            } catch (Exception e) {
                // 捕获并记录单个模板处理过程中的异常，但不影响其他模板的处理
                log.error("为复杂任务ID: {} 生成简单任务时出错", template.getId(), e);
                // 继续处理其他模板，不抛出异常中断整个任务
            }
        }
        
        log.info("MonthlyComplexReminderJob执行完成。");
    }
} 