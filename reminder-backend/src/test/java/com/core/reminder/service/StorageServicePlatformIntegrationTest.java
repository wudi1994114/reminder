package com.core.reminder.service;

import com.core.reminder.config.SaasStorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An opt-in probe of the deployed SaaS Admin storage gateway.
 *
 * <p>The test deliberately creates a tiny auditable object. It is disabled
 * unless Jenkins explicitly provides the app-client credentials and sets
 * {@code RUN_SAAS_STORAGE_INTEGRATION_TEST=true}.</p>
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "RUN_SAAS_STORAGE_INTEGRATION_TEST", matches = "(?i)true")
class StorageServicePlatformIntegrationTest {

    private static final byte[] ONE_PIXEL_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9J4JwAAAAASUVORK5CYII=");

    @Test
    void uploadsPngToSaasMinioUnderBeiwangjiAuditScope() {
        SaasStorageProperties properties = new SaasStorageProperties();
        properties.setBaseUrl(requiredEnvironment("SAAS_STORAGE_BASE_URL"));
        properties.setAppCode(requiredEnvironment("SAAS_STORAGE_APP_CODE"));
        properties.setAppId(requiredEnvironment("SAAS_STORAGE_APP_ID"));
        properties.setSecretCode(requiredEnvironment("SAAS_STORAGE_SECRET_CODE"));
        properties.setBizDir("avatars/probe");
        properties.setStorageType("MINIO");

        assertEquals("beiwangji", properties.getAppCode(),
                "The integration probe must use the audited BeiWangJi app client");

        StorageService storageService = new StorageService(
                new RestTemplate(), new ObjectMapper(), properties);
        MockMultipartFile image = new MockMultipartFile(
                "file", "saas-storage-probe.png", "image/png", ONE_PIXEL_PNG);

        StoredFile stored = storageService.store(image);

        assertNotNull(stored);
        assertTrue(stored.getObjectName().startsWith("app/beiwangji/avatars/probe/"),
                "The platform must isolate this app under its BeiWangJi object prefix");
        assertNotNull(stored.getUrl());
        assertFalse(stored.getUrl().isBlank());

        URI publicUri = URI.create(stored.getUrl());
        assertTrue(publicUri.isAbsolute(), "The storage gateway must return a public object URL");
        assertTrue(publicUri.getScheme().equals("https") || publicUri.getScheme().equals("http"),
                "The storage gateway must return an HTTP(S) object URL");
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }
}
