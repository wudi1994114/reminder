package com.task.reminder.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * Nacos配置管理类
 * 用于管理 Nacos 连接配置信息
 */
@Slf4j
@Configuration
public class NacosConfig {

    @Value("${nacos.config.server-addr}")
    private String nacosServer;

    @Value("${nacos.config.namespace}")
    private String namespace;

    @Bean
    public ConfigService getConfigService() {
        try {
            Properties properties = new Properties();
            properties.put("serverAddr", nacosServer);
            properties.put("namespace", namespace);
            ConfigService configService = NacosFactory.createConfigService(properties);
            log.info("nacos configService init success");
            return configService;
        } catch (NacosException e) {
            log.error("nacos configService init error", e);
            return null;
        }
    }
}
