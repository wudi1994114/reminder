package com.core.reminder.service;

import com.core.reminder.config.SaasStorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;
import java.util.Base64;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** Explicitly opted-in probe; creates one recognizable image-library original. */
@EnabledIfEnvironmentVariable(named = "RUN_BEAD_ARCHIVE_PROBE", matches = "true")
class BeadOriginalPlatformIntegrationTest {
    @Test
    void preservesOriginalBytesInTheBeiwangjiImageLibrary() throws Exception {
        // Never allow the HTTP debug logger to print the service client's login body.
        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(
                "org.springframework.web.client.RestTemplate")).setLevel(ch.qos.logback.classic.Level.WARN);
        SaasStorageProperties properties = new SaasStorageProperties();
        properties.setBaseUrl(System.getenv("SAAS_STORAGE_BASE_URL"));
        properties.setAppCode(System.getenv("SAAS_STORAGE_APP_CODE"));
        properties.setAppId(System.getenv("SAAS_STORAGE_APP_ID"));
        properties.setSecretCode(System.getenv("SAAS_STORAGE_SECRET_CODE"));
        assertEquals("beiwangji", properties.getAppCode());
        byte[] bytes = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9J4JwAAAAASUVORK5CYII=");
        StorageService service = new StorageService(new RestTemplate(), new ObjectMapper(), properties);
        Map<String, String> archived = service.storeBeadOriginal(new MockMultipartFile(
                "file", "bead-original-integration-probe.png", "image/png", bytes), 999999L);
        assertNotNull(archived.get("imageId"));
        assertTrue(archived.get("objectName").startsWith("app/beiwangji/beads/originals/999999/"));
        byte[] fetched = new RestTemplate().getForObject(archived.get("url"), byte[].class);
        assertArrayEquals(bytes, fetched, "原图字节必须与上传内容完全一致");
        // No credentials or access token are printed; the image ID identifies the audit record.
        System.out.println("BEAD_ARCHIVE_PROBE imageId=" + archived.get("imageId") + " originalBytes=" + bytes.length);
    }
}
