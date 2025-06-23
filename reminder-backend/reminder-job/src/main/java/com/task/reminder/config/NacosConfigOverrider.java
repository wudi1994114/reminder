package com.task.reminder.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
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

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        log.info("开始使用Nacos配置覆盖本地配置...");

        try {

            // 创建NacosConfigManager实例来获取配置
            NacosConfigManager nacosConfigManager = createNacosConfigManager();

            // 获取所有Nacos配置
            Map<String, String> nacosConfigs = nacosConfigManager.getAllConfigs();

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
     * 创建NacosConfigManager实例
     */
    private NacosConfigManager createNacosConfigManager() {
        // 这里需要手动创建NacosConfigManager，因为此时Spring容器还未完全初始化
        // 可以通过Environment获取Nacos配置信息
        try {
            // 创建ConfigService
            ConfigService configService = createConfigService();

            // 创建NacosConfigManager实例
            NacosConfigManager manager = new NacosConfigManager();

            // 调用手动初始化方法
            manager.initWithConfigService(configService);

            return manager;
        } catch (Exception e) {
            log.error("创建NacosConfigManager失败", e);
            throw new RuntimeException("创建NacosConfigManager失败", e);
        }
    }

    /**
     * 创建ConfigService
     */
    private ConfigService createConfigService() throws Exception {
        // 从Environment获取Nacos配置
        String serverAddr = environment.getProperty("nacos.config.server-addr", "127.0.0.1:8848");
        String namespace = environment.getProperty("nacos.config.namespace", "");

        java.util.Properties properties = new java.util.Properties();
        properties.setProperty("serverAddr", serverAddr);
        if (!namespace.isEmpty()) {
            properties.setProperty("namespace", namespace);
        }

        return NacosFactory.createConfigService(properties);
    }

    /**
     * 覆盖Gmail配置
     */
    private void overrideGmailConfig(Map<String, String> nacosConfigs, Map<String, Object> overrideProperties) {
        String clientId = nacosConfigs.get("gmail.client.id");
        String clientSecret = nacosConfigs.get("gmail.client.secret");
        String refreshToken = nacosConfigs.get("gmail.refresh.token");
        String senderAddress = nacosConfigs.get("gmail.sender.email.address");

        if (clientId != null && !clientId.isEmpty()) {
            overrideProperties.put("gmail.client.id", clientId);
            log.debug("覆盖Gmail客户端ID: {}", maskSensitive(clientId));
        }
        if (clientSecret != null && !clientSecret.isEmpty()) {
            overrideProperties.put("gmail.client.secret", clientSecret);
            log.debug("覆盖Gmail客户端密钥");
        }
        if (refreshToken != null && !refreshToken.isEmpty()) {
            overrideProperties.put("gmail.refresh.token", refreshToken);
            log.debug("覆盖Gmail刷新令牌");
        }
        if (senderAddress != null && !senderAddress.isEmpty()) {
            overrideProperties.put("gmail.sender.email.address", senderAddress);
            log.debug("覆盖Gmail发送者地址: {}", senderAddress);
        }
    }

    /**
     * 覆盖腾讯邮箱配置
     */
    private void overrideTencentEmailConfig(Map<String, String> nacosConfigs, Map<String, Object> overrideProperties) {
        String username = nacosConfigs.get("tencent.email.username");
        String password = nacosConfigs.get("tencent.email.password");
        String host = nacosConfigs.get("tencent.email.host");
        String port = nacosConfigs.get("tencent.email.port");
        String fromName = nacosConfigs.get("tencent.email.fromName");
        String senderType = nacosConfigs.get("tencent.email.senderType");
        String ssl = nacosConfigs.get("tencent.email.ssl");
        String sslEnabled = nacosConfigs.get("tencent.email.sslEnabled");
        
        if (username != null) {
            overrideProperties.put("tencent.email.username", username);
            log.debug("覆盖腾讯邮箱用户名");
        }
        if (password != null) {
            overrideProperties.put("tencent.email.password", password);
            log.debug("覆盖腾讯邮箱密码");
        }
        if (host != null) {
            overrideProperties.put("tencent.email.host", host);
            log.debug("覆盖腾讯邮箱主机");
        }
        if (port != null) {
            overrideProperties.put("tencent.email.port", port);
            log.debug("覆盖腾讯邮箱端口");
        }
        if (fromName != null) {
            overrideProperties.put("tencent.email.fromName", fromName);
            log.debug("覆盖腾讯邮箱发送者名称");
        }
        if (senderType != null) {
            overrideProperties.put("tencent.email.senderType", senderType);
            log.debug("覆盖腾讯邮箱发送者类型");
        }
        if (ssl != null) {
            overrideProperties.put("tencent.email.ssl", ssl);
            log.debug("覆盖腾讯邮箱SSL设置");
        }
        if (sslEnabled != null) {
            overrideProperties.put("tencent.email.sslEnabled", sslEnabled);
            log.debug("覆盖腾讯邮箱SSL启用设置");
        }
    }

    /**
     * 覆盖腾讯云配置
     */
    private void overrideTencentCloudConfig(Map<String, String> nacosConfigs, Map<String, Object> overrideProperties) {
        String secretId = nacosConfigs.get("tencent.cloud.secret-id");
        String secretKey = nacosConfigs.get("tencent.cloud.secret-key");
        String stsRegion = nacosConfigs.get("tencent.cloud.sts.region");
        String stsDurationSeconds = nacosConfigs.get("tencent.cloud.sts.duration-seconds");

        if (secretId != null && !secretId.isEmpty()) {
            overrideProperties.put("tencent.cloud.secret-id", secretId);
            log.debug("覆盖腾讯云SecretId: {}", maskSensitive(secretId));
        }
        if (secretKey != null && !secretKey.isEmpty()) {
            overrideProperties.put("tencent.cloud.secret-key", secretKey);
            log.debug("覆盖腾讯云SecretKey");
        }
        if (stsRegion != null && !stsRegion.isEmpty()) {
            overrideProperties.put("tencent.cloud.sts.region", stsRegion);
            log.debug("覆盖腾讯云STS区域: {}", stsRegion);
        }
        if (stsDurationSeconds != null && !stsDurationSeconds.isEmpty()) {
            overrideProperties.put("tencent.cloud.sts.duration-seconds", stsDurationSeconds);
            log.debug("覆盖腾讯云STS持续时间: {}", stsDurationSeconds);
        }
    }

    /**
     * 覆盖微信小程序配置
     */
    private void overrideWechatConfig(Map<String, String> nacosConfigs, Map<String, Object> overrideProperties) {
        String appid = nacosConfigs.get("wechat.miniprogram.appid");
        String secret = nacosConfigs.get("wechat.miniprogram.secret");
        String apiBaseUrl = nacosConfigs.get("wechat.miniprogram.api-base-url");
        String jscode2sessionUrl = nacosConfigs.get("wechat.miniprogram.jscode2session-url");
        String connectTimeout = nacosConfigs.get("wechat.miniprogram.connect-timeout");
        String readTimeout = nacosConfigs.get("wechat.miniprogram.read-timeout");
        String templateId = nacosConfigs.get("wechat.notification.template-id");
        String page = nacosConfigs.get("wechat.notification.page");
        
        if (appid != null) {
            overrideProperties.put("wechat.miniprogram.appid", appid);
            log.debug("覆盖微信小程序AppID");
        }
        if (secret != null) {
            overrideProperties.put("wechat.miniprogram.secret", secret);
            log.debug("覆盖微信小程序Secret");
        }
        if (apiBaseUrl != null) {
            overrideProperties.put("wechat.miniprogram.api-base-url", apiBaseUrl);
            log.debug("覆盖微信API基础URL");
        }
        if (jscode2sessionUrl != null) {
            overrideProperties.put("wechat.miniprogram.jscode2session-url", jscode2sessionUrl);
            log.debug("覆盖微信jscode2session URL");
        }
        if (connectTimeout != null) {
            overrideProperties.put("wechat.miniprogram.connect-timeout", connectTimeout);
            log.debug("覆盖微信连接超时时间");
        }
        if (readTimeout != null) {
            overrideProperties.put("wechat.miniprogram.read-timeout", readTimeout);
            log.debug("覆盖微信读取超时时间");
        }
        if (templateId != null) {
            overrideProperties.put("wechat.notification.template-id", templateId);
            log.debug("覆盖微信通知模板ID");
        }
        if (page != null) {
            overrideProperties.put("wechat.notification.page", page);
            log.debug("覆盖微信通知页面");
        }
    }

    /**
     * 屏蔽敏感信息用于日志输出
     */
    private String maskSensitive(String value) {
        if (value == null || value.length() <= 8) {
            return "***";
        }
        return value.substring(0, 4) + "***" + value.substring(value.length() - 4);
    }
}
