package com.core.reminder.service;

import com.core.reminder.config.WechatConfig;
import com.core.reminder.dto.WechatApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;

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
     * 
     * [安全警告] 当前实现为了解决 "PKIX path building failed" 错误，配置为信任所有SSL证书。
     * 这会绕过证书验证，使HTTPS连接容易受到中间人攻击。
     * 此配置仅建议用于开发、调试或在确认网络环境绝对安全的情况下使用。
     * 生产环境的最佳实践是：
     * 1. 确保部署环境的Java拥有最新的根证书。
     * 2. 如果必须使用自定义证书，请创建一个包含特定受信任证书的TrustStore，而不是完全禁用验证。
     */
    private CloseableHttpClient createHttpClient() {
        try {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(wechatConfig.getConnectTimeout())
                    .setSocketTimeout(wechatConfig.getReadTimeout())
                    .build();

            // 创建一个信任所有证书的SSL上下文
            TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, acceptingTrustStrategy)
                    .build();
            
            // 使用NoopHostnameVerifier来禁用主机名验证
            SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

            return HttpClients.custom()
                    .setSSLSocketFactory(csf)
                    .setDefaultRequestConfig(requestConfig)
                    .build();
        } catch (Exception e) {
            log.error("创建信任所有证书的HttpClient时出错", e);
            // 如果自定义SSL上下文失败，回退到默认的HttpClient
            return HttpClients.createDefault();
        }
    }
} 