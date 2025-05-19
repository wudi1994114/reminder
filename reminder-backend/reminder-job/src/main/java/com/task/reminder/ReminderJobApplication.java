package com.task.reminder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Ensure this is present if you rely on Spring's @Scheduled in addition to Quartz
@ComponentScan(basePackages = {"com.task.reminder", "com.common.reminder"})
public class ReminderJobApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReminderJobApplication.class, args);
    }

} 