package com.example.reminder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling; // 确保 Quartz 等调度任务能运行

@SpringBootApplication
@EntityScan("com.example.reminder.model")
@EnableJpaRepositories("com.example.reminder.repository")
@EnableScheduling // 如果使用 Spring 自身的 @Scheduled, 也需要这个注解
@ComponentScan(basePackages = {"com.example.reminder"}) // 确保扫描所有包含配置类的包
public class ReminderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReminderApplication.class, args);
    }

} 