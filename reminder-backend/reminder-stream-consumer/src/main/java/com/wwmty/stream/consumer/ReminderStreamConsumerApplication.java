package com.wwmty.stream.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.wwmty.stream.consumer.repository")
public class ReminderStreamConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReminderStreamConsumerApplication.class, args);
    }

} 