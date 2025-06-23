package com.core.reminder.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.common.reminder.utils.JacksonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Nacos配置覆盖器
 * 在应用初始化时使用Nacos配置覆盖本地配置
 * 使用BeanFactoryPostProcessor确保在所有Bean初始化之前执行
 */
@Slf4j
@Component
public class NacosConfigOverrider implements BeanFactoryPostProcessor, PriorityOrdered, EnvironmentAware {

    private ConfigurableEnvironment environment;

    private ConfigService configService;
    private NacosConfigManager nacosConfigManager;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        log.info("开始使用Nacos配置覆盖本地配置...");

        try {
            // 创建ConfigService来获取配置
            ConfigService configService = createConfigService();
            this.configService = configService;
            // 创建临时的NacosConfigManager来获取配置
            NacosConfigManager tempManager = new NacosConfigManager();
            tempManager.setConfigService(configService);
            tempManager.loadAllConfigs();
            this.nacosConfigManager = tempManager;

            // 获取所有Nacos配置
            Map<String, String> nacosConfigs = tempManager.getAllConfigs();
            log.info(JacksonUtils.toJson(nacosConfigs));
            if (nacosConfigs.isEmpty()) {
                log.warn("未从Nacos获取到任何配置，跳过配置覆盖");
                return;
            }

            // 创建覆盖配置的Map
            Map<String, Object> overrideProperties = new HashMap<>(nacosConfigs);

            // 将覆盖配置添加到Spring Environment中，优先级最高
            if (!overrideProperties.isEmpty()) {
                MapPropertySource nacosPropertySource = new MapPropertySource("nacosOverride", overrideProperties);
                environment.getPropertySources().addFirst(nacosPropertySource);

                log.info("成功使用Nacos配置覆盖了 {} 个配置项", overrideProperties.size());
                log.debug("覆盖的配置项: {}", overrideProperties.keySet());
            } else {
                log.info("没有需要覆盖的配置项");
            }

        } catch (Exception e) {
            log.error("配置覆盖过程中发生错误", e);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * 创建NacosConfigManager Bean
     */
    @Bean
    public NacosConfigManager createNacosConfigManager() {
        return this.nacosConfigManager;
    }

    /**
     * 创建ConfigService Bean
     */
    @Bean
    public ConfigService configService() throws Exception {
        return this.configService;
    }

    /**
     * 创建ConfigService Bean
     */
    public ConfigService createConfigService() throws Exception {
        // 从Environment获取Nacos配置
        String serverAddr = environment.getProperty("nacos.config.server-addr", "127.0.0.1:8848");
        String namespace = environment.getProperty("nacos.config.namespace", "");

        java.util.Properties properties = new java.util.Properties();
        properties.setProperty("serverAddr", serverAddr);
        if (!namespace.isEmpty()) {
            properties.setProperty("namespace", namespace);
        }

        ConfigService configService = NacosFactory.createConfigService(properties);
        log.info("ConfigService Bean 创建成功");
        return configService;
    }

}
