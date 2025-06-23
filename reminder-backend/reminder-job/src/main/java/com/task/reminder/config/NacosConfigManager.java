package com.task.reminder.config;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.common.reminder.utils.JacksonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Nacos配置管理器 - 启动时获取所有配置，通过key获取配置值
 */
@Slf4j
@Component
public class NacosConfigManager {

    @Autowired(required = false)
    private ConfigService configService;


    /**
     * 原始配置缓存，key为"dataId:group"格式
     */
    private final Map<String, String> configCache = new ConcurrentHashMap<>();


    /**
     * YAML解析器
     */
    private final Yaml yaml = new Yaml();

    /**
     * 异步执行器
     */
    private final Executor executor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "nacos-config-listener");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * 需要加载的配置列表
     */
    private final String[][] CONFIG_LIST = {
        {"secret", "DEFAULT_GROUP"}
    };

    @PostConstruct
    public void init() {
        if (configService != null) {
            log.info("NacosConfigManager initializing...");
            // 加载所有配置
            loadAllConfigs();
            log.info("NacosConfigManager initialized with {} flat configs", configCache.size());
            log.info("NacosConfigManager initialized with {} flat configs", JacksonUtils.toJson(configCache));
        }
    }

    /**
     * 手动初始化方法，用于在Spring容器外部使用
     */
    public void initWithConfigService(ConfigService configService) {
        this.configService = configService;
        log.info("NacosConfigManager manually initializing...");
        // 加载所有配置
        loadAllConfigs();
        log.info("NacosConfigManager manually initialized with {} flat configs", configCache.size());
        log.info("NacosConfigManager manually initialized with {} flat configs", JacksonUtils.toJson(configCache));
    }

    /**
     * 通过key获取配置值，提供默认值
     * @param key 配置key
     * @return 配置值或默认值
     */
    public String get(String key) {
        return configCache.get(key);
    }

    /**
     * 通过key获取配置值，提供默认值
     * @param key 配置key
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    public String get(String key, String defaultValue) {
        return configCache.getOrDefault(key, defaultValue);
    }

    /**
     * 获取所有配置
     */
    public Map<String, String> getAllConfigs() {
        return new HashMap<>(configCache);
    }


    /**
     * 加载所有配置
     */
    private void loadAllConfigs() {
        for (String[] config : CONFIG_LIST) {
            String dataId = config[0];
            String group = config[1];
            loadSingleConfig(dataId, group);
        }
    }

    /**
     * 加载单个配置文件
     */
    private void loadSingleConfig(String dataId, String group) {
        try {
            log.info("loadSingleConfig: {}, group: {}", dataId, group);
            String configInfo = configService.getConfig(dataId, group, 5000);
            if (StringUtils.hasText(configInfo)) {
                log.info("secret:{}", JacksonUtils.toJson(configInfo));
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(configInfo);
                Map<String, String> flattenedMap = flattenMap(data, null);
                configCache.putAll(flattenedMap);
                log.info("123" + JacksonUtils.toJson(configCache));
                // 添加监听器
                addNacosListener(dataId, group);
            } else {
                log.warn("Config not found or empty: {}:{}", dataId, group);
            }
        } catch (NacosException e) {
            log.error("Failed to load config: {}:{}", dataId, group, e);
        }
    }


    public void onChangeForDataMap(String newConfig) {

    }

    public static Map<String, String> flattenMap(Map<String, Object> map, String parentKey) {
        Map<String, String> flatMap = new HashMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = parentKey == null ? entry.getKey() : parentKey + "." + entry.getKey();

            if (entry.getValue() instanceof Map) {
                flatMap.putAll(flattenMap((Map<String, Object>) entry.getValue(), key));
            } else {
                flatMap.put(key, entry.getValue().toString());
            }
        }

        return flatMap;
    }

    /**
     * 添加Nacos监听器
     */
    private void addNacosListener(String dataId, String group) {

        Listener nacosListener = new Listener() {
            @Override
            public Executor getExecutor() {
                return executor;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                log.info("secret:{} changed", JacksonUtils.toJson(configInfo));
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(configInfo);
                Map<String, String> flattenedMap = flattenMap(data, null);
                configCache.putAll(flattenedMap);
            }
        };

        try {
            configService.addListener("secret", "DEFAULT_GROUP", nacosListener);
        } catch (NacosException e) {
            log.error("Failed to add nacos listener ", e);
        }
    }




}
