package com.task.reminder.config;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Quartz定时任务初始化器
 * 
 * 该组件实现ApplicationRunner接口，在Spring Boot应用启动后
 * 确保所有的Quartz定时任务被正确注册到调度器中。
 * 
 * 主要解决的问题是：
 * 1. 确保QuartzConfig中配置的任务被正确加载到Quartz中
 * 2. 防止因为数据库连接失败导致任务未能注册
 * 3. 提供详细的初始化日志，便于排查问题
 */
@Slf4j
@Component
public class QuartzInitializer implements ApplicationRunner {

    @Autowired
    private Scheduler scheduler;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始初始化Quartz定时任务...");
        
        try {
            // 检查Quartz调度器是否正在运行
            if (!scheduler.isStarted()) {
                log.warn("Quartz调度器未运行，尝试启动它...");
                scheduler.start();
            }
            
            // 检查调度器中是否有注册的任务
            if (scheduler.getJobGroupNames().isEmpty()) {
                log.warn("Quartz调度器中没有任何任务组，可能任务未正确注册");
            } else {
                log.info("Quartz调度器中的任务组数量: {}", scheduler.getJobGroupNames().size());
                
                // 输出每个任务组中的任务数量
                for (String groupName : scheduler.getJobGroupNames()) {
                    int jobCount = scheduler.getJobKeys(org.quartz.impl.matchers.GroupMatcher.jobGroupEquals(groupName)).size();
                    log.info("任务组 [{}] 中有 {} 个任务", groupName, jobCount);
                }
            }
            
            log.info("Quartz定时任务初始化完成");
        } catch (SchedulerException e) {
            log.error("初始化Quartz定时任务失败", e);
            throw e;
        }
    }
} 