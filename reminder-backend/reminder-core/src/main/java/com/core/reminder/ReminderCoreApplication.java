package com.core.reminder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling; // 确保 Quartz 等调度任务能运行

@SpringBootApplication
@EntityScan(basePackages = {"com.common.reminder.model", "com.core.reminder.model"})
@EnableScheduling // 如果使用 Spring 自身的 @Scheduled, 也需要这个注解
@EnableAspectJAutoProxy // 启用AOP支持
@ComponentScan(basePackages = {"com.core.reminder", "com.common.reminder"}) // 确保扫描所有包含配置类的包
public class ReminderCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReminderCoreApplication.class, args);
    }

} 