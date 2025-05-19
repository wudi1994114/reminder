package com.task.reminder.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA配置类
 * 
 * 这个配置类负责设置JPA相关的配置，包括：
 * 1. 实体类扫描路径设置 - 确保能扫描到common包中的实体
 * 2. JPA仓库接口扫描路径设置
 */
@Configuration
@EntityScan(basePackages = {"com.common.reminder.model", "com.task.reminder.model"})
@EnableJpaRepositories(basePackages = {"com.task.reminder.repository"})
public class JpaConfig {
    // 配置类不需要任何方法，只需要通过注解配置扫描路径
} 