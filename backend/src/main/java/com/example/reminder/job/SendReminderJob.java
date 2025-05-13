package com.example.reminder.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 发送备忘录的任务
 * 用于在指定时间发送备忘录给用户
 */
@Slf4j
@Component
public class SendReminderJob implements Job {
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // 空实现，后续可以添加发送备忘录的逻辑
        log.info("发送备忘录的任务");
    }
} 