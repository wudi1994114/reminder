package com.core.reminder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "storage.saas")
public class SaasStorageProperties {

    private String baseUrl = "http://saas-admin-backend:8080";
    private String appCode = "beiwangji";
    private String appId = "";
    private String secretCode = "";
    private String bizDir = "avatars";
    private String storageType = "MINIO";
    private long maxSizeBytes = 5 * 1024 * 1024L;
    private List<String> allowedContentTypes = Arrays.asList(
            "image/jpeg", "image/png", "image/webp", "image/gif");
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 30000;
}
