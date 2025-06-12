package com.task.reminder.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信小程序订阅消息发送器
 * 用于发送微信小程序订阅消息通知
 */
@Slf4j
@Component("wechatNotificationSender")
public class WechatNotificationSender implements NotificationSender {

    @Value("${wechat.miniprogram.appid:wx597f8aaca581205f}")
    private String appId;

    @Value("${wechat.miniprogram.secret:b70c1fd582a6f6d66e8dc1e2e436d023}")
    private String appSecret;

    @Value("${wechat.notification.template-id:VXTd9P8DUPpM0aM-nLSHSkDC9KTUolBNhkAHV7UkqxQ}")
    private String templateId;

    @Value("${wechat.notification.page:index}")
    private String defaultPage;

    private ObjectMapper objectMapper = new ObjectMapper();
    
    // 微信API相关常量
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
    private static final String SUBSCRIBE_MESSAGE_URL = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=%s";
    
    // 访问令牌缓存（简单实现，生产环境建议使用Redis）
    private String cachedAccessToken;
    private long tokenExpireTime;

    @PostConstruct
    public void init() {
        log.info("微信通知发送器初始化 - AppID: {}, Secret: {}, 模板ID: {}", 
                appId != null && !appId.isEmpty() ? appId.substring(0, 6) + "***" : "未配置", 
                appSecret != null && !appSecret.isEmpty() ? "已配置" : "未配置",
                templateId != null && !templateId.isEmpty() ? templateId.substring(0, 10) + "***" : "未配置");
        
        // 验证必要的配置
        if (appId == null || appId.trim().isEmpty()) {
            log.error("微信小程序AppID未配置！请检查配置文件中的 wechat.miniprogram.appid");
        }
        if (appSecret == null || appSecret.trim().isEmpty()) {
            log.error("微信小程序AppSecret未配置！请检查配置文件中的 wechat.miniprogram.secret");
        }
        if (templateId == null || templateId.trim().isEmpty()) {
            log.error("微信订阅消息模板ID未配置！请检查配置文件中的 wechat.notification.template-id");
        }
    }

    @Override
    public boolean sendNotification(String recipient, String title, String content, Object extraData) {
        try {
            if (!isValidRecipient(recipient)) {
                log.error("无效的微信openid: {}", recipient);
                return false;
            }

            // 获取访问令牌
            String accessToken = getAccessToken();
            if (accessToken == null) {
                log.error("获取微信访问令牌失败");
                return false;
            }

            // 构建订阅消息数据
            Map<String, Object> messageData = buildMessageData(recipient, title, content, extraData);
            
            // 发送订阅消息
            return sendSubscribeMessage(accessToken, messageData);

        } catch (Exception e) {
            log.error("发送微信通知失败 - openid: {}, 标题: {}, 错误: {}", recipient, title, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getSenderType() {
        return "WECHAT";
    }

    @Override
    public boolean isValidRecipient(String recipient) {
        // 微信openid通常以'o'开头，长度约28个字符
        return recipient != null && recipient.length() > 20 && recipient.startsWith("o");
    }

    /**
     * 获取微信访问令牌
     */
    private String getAccessToken() {
        try {
            // 验证配置
            if (appId == null || appId.trim().isEmpty()) {
                log.error("微信AppID未配置，无法获取访问令牌");
                return null;
            }
            if (appSecret == null || appSecret.trim().isEmpty()) {
                log.error("微信AppSecret未配置，无法获取访问令牌");
                return null;
            }

            // 检查缓存的令牌是否有效
            if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpireTime) {
                return cachedAccessToken;
            }

            // 请求新的访问令牌
            String url = String.format(ACCESS_TOKEN_URL, appId, appSecret);
            log.debug("请求微信访问令牌 - URL: {}", url.replaceAll("secret=[^&]*", "secret=***"));
            
            try (CloseableHttpClient httpClient = createHttpClient()) {
                HttpPost httpPost = new HttpPost(url);
                
                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);
                    
                    if (result.containsKey("access_token")) {
                        cachedAccessToken = (String) result.get("access_token");
                        Integer expiresIn = (Integer) result.get("expires_in");
                        // 提前5分钟过期，避免边界情况
                        tokenExpireTime = System.currentTimeMillis() + (expiresIn - 300) * 1000L;
                        
                        log.info("获取微信访问令牌成功，有效期: {} 秒", expiresIn);
                        return cachedAccessToken;
                    } else {
                        log.error("获取微信访问令牌失败: {}", responseBody);
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取微信访问令牌异常", e);
            return null;
        }
    }

    /**
     * 构建订阅消息数据
     */
    private Map<String, Object> buildMessageData(String openid, String title, String content, Object extraData) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("touser", openid);
        messageData.put("template_id", templateId);
        messageData.put("page", defaultPage);

        // 构建消息内容
        Map<String, Object> data = new HashMap<>();
        
        if (extraData instanceof Map) {
            // 如果传入了自定义数据，直接使用
            @SuppressWarnings("unchecked")
            Map<String, Object> customData = (Map<String, Object>) extraData;
            data.putAll(customData);
        } else {
            // 默认数据格式
            data.put("thing2", createDataItem(title));
            data.put("thing11", createDataItem(content));
            data.put("date4", createDataItem(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        }

        messageData.put("data", data);
        return messageData;
    }

    /**
     * 创建消息数据项
     */
    private Map<String, String> createDataItem(String value) {
        Map<String, String> item = new HashMap<>();
        item.put("value", value != null ? value : "");
        return item;
    }

    /**
     * 发送订阅消息
     */
    private boolean sendSubscribeMessage(String accessToken, Map<String, Object> messageData) {
        try {
            String url = String.format(SUBSCRIBE_MESSAGE_URL, accessToken);
            String jsonData = objectMapper.writeValueAsString(messageData);
            
            log.info("发送微信订阅消息 - URL: {}, 数据: {}", url, jsonData);
            
            try (CloseableHttpClient httpClient = createHttpClient()) {
                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeader("Content-Type", "application/json; charset=utf-8");
                httpPost.setEntity(new StringEntity(jsonData, StandardCharsets.UTF_8));
                
                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);
                    
                    Integer errcode = (Integer) result.get("errcode");
                    String errmsg = (String) result.get("errmsg");
                    
                    if (errcode != null && errcode == 0) {
                        log.info("微信订阅消息发送成功 - openid: {}", messageData.get("touser"));
                        return true;
                    } else {
                        log.error("微信订阅消息发送失败 - errcode: {}, errmsg: {}, openid: {}", 
                                errcode, errmsg, messageData.get("touser"));
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            log.error("发送微信订阅消息异常", e);
            return false;
        }
    }

    /**
     * 创建HTTP客户端
     */
    private CloseableHttpClient createHttpClient() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setSocketTimeout(10000)
                .build();
        
        return HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();
    }
}