package com.core.reminder.service;

import com.core.reminder.config.WechatConfig;
import com.core.reminder.dto.WechatApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 微信API服务类
 */
@Slf4j
@Service
public class WechatApiService {

    @Autowired
    private WechatConfig wechatConfig;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 通过code获取微信用户的openid和session_key
     * @param code 微信小程序登录凭证
     * @return 微信API响应
     * @throws Exception 调用异常
     */
    public WechatApiResponse jscode2session(String code) throws Exception {
        String url = buildJscode2sessionUrl(code);
        
        log.info("调用微信jscode2session接口，URL: {}", url);
        
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet httpGet = new HttpGet(url);
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                log.info("微信API响应: {}", responseBody);
                
                WechatApiResponse apiResponse = objectMapper.readValue(responseBody, WechatApiResponse.class);
                
                // 检查是否有错误
                if (apiResponse.getErrcode() != null && apiResponse.getErrcode() != 0) {
                    log.error("微信API调用失败，错误码: {}, 错误信息: {}", 
                            apiResponse.getErrcode(), apiResponse.getErrmsg());
                    throw new RuntimeException("微信登录失败: " + apiResponse.getErrmsg());
                }
                
                return apiResponse;
            }
        }
    }

    /**
     * 构建jscode2session请求URL
     */
    private String buildJscode2sessionUrl(String code) {
        try {
            return String.format("%s%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    wechatConfig.getApiBaseUrl(),
                    wechatConfig.getJscode2sessionUrl(),
                    URLEncoder.encode(wechatConfig.getAppid(), StandardCharsets.UTF_8.name()),
                    URLEncoder.encode(wechatConfig.getSecret(), StandardCharsets.UTF_8.name()),
                    URLEncoder.encode(code, StandardCharsets.UTF_8.name()));
        } catch (Exception e) {
            throw new RuntimeException("构建微信API URL失败", e);
        }
    }

    /**
     * 创建HTTP客户端
     */
    private CloseableHttpClient createHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(wechatConfig.getConnectTimeout())
                .setSocketTimeout(wechatConfig.getReadTimeout())
                .build();

        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
} 