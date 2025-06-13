package com.core.reminder.service;

import com.core.reminder.config.WechatConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
@Slf4j
public class WechatAccessTokenService {

    @Autowired
    private WechatConfig wechatConfig;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private LocalDateTime tokenExpiryTime;

    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";

    public String getAccessToken() {
        if (accessToken == null || LocalDateTime.now().isAfter(tokenExpiryTime)) {
            fetchNewAccessToken();
        }
        return accessToken;
    }

    private synchronized void fetchNewAccessToken() {
        // Double-check locking to prevent multiple threads from fetching at the same time
        if (accessToken != null && !LocalDateTime.now().isAfter(tokenExpiryTime)) {
            return;
        }

        log.info("Fetching new WeChat access token...");
        RestTemplate restTemplate = new RestTemplate();
        String url = String.format(TOKEN_URL, wechatConfig.getAppid(), wechatConfig.getSecret());

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.has("access_token")) {
                this.accessToken = root.get("access_token").asText();
                long expiresIn = root.get("expires_in").asLong();
                // Expire 5 minutes early to be safe
                this.tokenExpiryTime = LocalDateTime.now().plusSeconds(expiresIn - 300);
                log.info("Successfully fetched new WeChat access token.");
            } else {
                String errMsg = root.has("errmsg") ? root.get("errmsg").asText() : "Unknown error";
                log.error("Failed to fetch WeChat access token: {}", errMsg);
                throw new RuntimeException("Failed to fetch WeChat access token: " + errMsg);
            }
        } catch (Exception e) {
            log.error("Error fetching WeChat access token", e);
            throw new RuntimeException("Error fetching WeChat access token", e);
        }
    }
} 