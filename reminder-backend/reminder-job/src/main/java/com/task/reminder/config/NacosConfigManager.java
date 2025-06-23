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
public class NacosConfigManager {

    @Autowired
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
     * 设置ConfigService（用于前置处理）
     */
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * 加载所有配置
     */
    public void loadAllConfigs() {
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
            String configInfo = configService.getConfig(dataId, group, 5000);
            if (StringUtils.hasText(configInfo)) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(configInfo);
                Map<String, String> flattenedMap = flattenMap(data, null);
                configCache.putAll(flattenedMap);
                log.info("成功加载配置 {}:{}, 配置项数量: {}", dataId, group, flattenedMap.size());
                // 添加监听器
                addNacosListener(dataId, group);
            } else {
                log.warn("配置不存在或为空: {}:{}", dataId, group);
            }
        } catch (NacosException e) {
            log.error("加载配置失败: {}:{}", dataId, group, e);
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
                log.info("配置发生变更 {}:{}", dataId, group);
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(configInfo);
                Map<String, String> flattenedMap = flattenMap(data, null);
                configCache.putAll(flattenedMap);
                log.info("配置更新完成，当前配置项数量: {}", configCache.size());
            }
        };

        try {
            configService.addListener(dataId, group, nacosListener);
            log.info("成功添加配置监听器: {}:{}", dataId, group);
        } catch (NacosException e) {
            log.error("添加配置监听器失败: {}:{}", dataId, group, e);
        }
    }




}
