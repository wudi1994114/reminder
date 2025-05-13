package com.example.reminder.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 准备备忘录的任务
 * 空类，便于后续扩展
 */
@Slf4j
@Component
public class PrepareReminderJob implements Job {
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // 空实现，待扩展
        log.info("准备备忘录的任务");
    }
} 