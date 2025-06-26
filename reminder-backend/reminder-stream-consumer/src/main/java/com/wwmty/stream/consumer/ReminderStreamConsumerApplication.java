package com.wwmty.stream.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"com.common.reminder.model", "com.wwmty.stream.consumer.model"})
@EnableJpaRepositories(basePackages = {"com.wwmty.stream.consumer.repository"})
public class ReminderStreamConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReminderStreamConsumerApplication.class, args);
    }

} 