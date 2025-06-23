package com.core.reminder.service;

import com.core.reminder.config.TencentCloudConfig;
import com.core.reminder.dto.TencentCredentialsDto;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.sts.v20180813.StsClient;
import com.tencentcloudapi.sts.v20180813.models.GetFederationTokenRequest;
import com.tencentcloudapi.sts.v20180813.models.GetFederationTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 腾讯云STS临时密钥服务
 * 
 * @author wudi
 * @since 2025-06-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TencentStsService {
    
    private final TencentCloudConfig tencentCloudConfig;
    
    /**
     * 获取语音识别临时凭证
     * 
     * @param userId 用户ID（用于日志记录）
     * @return 临时凭证信息
     * @throws TencentCloudSDKException 腾讯云SDK异常
     */
    public TencentCredentialsDto getSpeechRecognitionCredentials(Long userId) throws TencentCloudSDKException {
        log.info("为用户 {} 获取语音识别临时凭证", userId);
        
        try {
            // 1. 初始化客户端
            Credential cred = new Credential(
                tencentCloudConfig.getSecretId(), 
                tencentCloudConfig.getSecretKey()
            );
            StsClient client = new StsClient(cred, tencentCloudConfig.getSts().getRegion());
            
            // 2. 构造请求
            GetFederationTokenRequest req = new GetFederationTokenRequest();
            req.setName("speech-recognition-user-" + userId); // 自定义名称，包含用户ID
            req.setDurationSeconds(tencentCloudConfig.getSts().getDurationSeconds().longValue());
            
            // 3. 设置策略（Policy）- 只授予语音识别的权限
            String policy = buildSpeechRecognitionPolicy();
            req.setPolicy(policy);
            
            // 4. 调用接口获取临时凭证
            GetFederationTokenResponse resp = client.GetFederationToken(req);
            
            // 5. 构造返回数据
            TencentCredentialsDto credentialsDto = TencentCredentialsDto.builder()
                .tmpSecretId(resp.getCredentials().getTmpSecretId())
                .tmpSecretKey(resp.getCredentials().getTmpSecretKey())
                .sessionToken(resp.getCredentials().getToken())
                .expiredTime(resp.getExpiredTime())
                .region(tencentCloudConfig.getSts().getRegion())
                .build();
            
            log.info("成功为用户 {} 获取临时凭证，过期时间: {}", userId, resp.getExpiredTime());
            return credentialsDto;
            
        } catch (TencentCloudSDKException e) {
            log.error("为用户 {} 获取临时凭证失败: {}", userId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 构建语音识别权限策略
     * 
     * @return JSON格式的权限策略
     */
    private String buildSpeechRecognitionPolicy() {
        // 构建权限策略，只允许语音识别相关操作
        return "{\n" +
               "  \"version\": \"2.0\",\n" +
               "  \"statement\": [\n" +
               "    {\n" +
               "      \"effect\": \"allow\",\n" +
               "      \"action\": [\n" +
               "        \"asr:CreateRecTask\",\n" +
               "        \"asr:DescribeTaskStatus\",\n" +
               "        \"asr:SentenceRecognition\",\n" +
               "        \"asr:FlashRecognition\"\n" +
               "      ],\n" +
               "      \"resource\": \"*\"\n" +
               "    }\n" +
               "  ]\n" +
               "}";
    }
    
    /**
     * 获取通用临时凭证（可用于其他腾讯云服务）
     * 
     * @param userId 用户ID
     * @param serviceName 服务名称
     * @param customPolicy 自定义权限策略
     * @return 临时凭证信息
     * @throws TencentCloudSDKException 腾讯云SDK异常
     */
    public TencentCredentialsDto getCustomCredentials(Long userId, String serviceName, String customPolicy) 
            throws TencentCloudSDKException {
        log.info("为用户 {} 获取 {} 服务临时凭证", userId, serviceName);
        
        try {
            // 1. 初始化客户端
            Credential cred = new Credential(
                tencentCloudConfig.getSecretId(), 
                tencentCloudConfig.getSecretKey()
            );
            StsClient client = new StsClient(cred, tencentCloudConfig.getSts().getRegion());
            
            // 2. 构造请求
            GetFederationTokenRequest req = new GetFederationTokenRequest();
            req.setName(serviceName + "-user-" + userId);
            req.setDurationSeconds(tencentCloudConfig.getSts().getDurationSeconds().longValue());
            req.setPolicy(customPolicy);
            
            // 3. 调用接口获取临时凭证
            GetFederationTokenResponse resp = client.GetFederationToken(req);
            
            // 4. 构造返回数据
            TencentCredentialsDto credentialsDto = TencentCredentialsDto.builder()
                .tmpSecretId(resp.getCredentials().getTmpSecretId())
                .tmpSecretKey(resp.getCredentials().getTmpSecretKey())
                .sessionToken(resp.getCredentials().getToken())
                .expiredTime(resp.getExpiredTime())
                .region(tencentCloudConfig.getSts().getRegion())
                .build();
            
            log.info("成功为用户 {} 获取 {} 服务临时凭证，过期时间: {}", userId, serviceName, resp.getExpiredTime());
            return credentialsDto;
            
        } catch (TencentCloudSDKException e) {
            log.error("为用户 {} 获取 {} 服务临时凭证失败: {}", userId, serviceName, e.getMessage(), e);
            throw e;
        }
    }
}
