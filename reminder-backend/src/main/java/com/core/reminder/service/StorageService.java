package com.core.reminder.service;

import com.core.reminder.config.SaasStorageProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class StorageService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SaasStorageProperties properties;
    private volatile String cachedAccessToken;

    public StorageService(
            @Qualifier("saasStorageRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            SaasStorageProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public StoredFile store(MultipartFile file) {
        validateFile(file);
        validateClientConfiguration();
        return upload(file, properties.getBizDir(), true);
    }

    /** Store the unmodified original and register it in the tenant's image library. */
    public Map<String, String> storeBeadOriginal(MultipartFile file, Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("需要登录后上传原图");
        }
        validateFile(file);
        validateClientConfiguration();
        StoredFile stored = upload(file, "beads/originals/" + userId, true);
        Map<String, Object> image = new LinkedHashMap<>();
        image.put("originKey", stored.getObjectName());
        // The library may use the original as its thumbnail; no recompression of the original.
        image.put("thumbKey", stored.getObjectName());
        String filename = file.getOriginalFilename() == null ? "original.png" : file.getOriginalFilename();
        filename = filename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        image.put("filename", "拼豆-" + userId + "-" + filename);
        image.put("fileSize", file.getSize());
        image.put("contentType", file.getContentType());
        image.put("storageType", properties.getStorageType());
        image.put("folderId", 0L);
        image.put("referenceCount", 0);
        image.put("assetType", "image");
        image.put("assetOriginType", "upload");
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("purpose", "bead_original");
        metadata.put("reminderUserId", userId);
        image.put("metadata", metadata);
        String imageId = registerImage(image, true);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("url", stored.getUrl());
        result.put("objectName", stored.getObjectName());
        result.put("imageId", imageId);
        return result;
    }

    private String registerImage(Map<String, Object> image, boolean retryOnUnauthorized) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAccessToken());
        headers.set("X-Project-Code", properties.getAppCode());
        try {
            ResponseEntity<String> response = restTemplate.exchange(endpoint("/biz/image"),
                    HttpMethod.POST, new HttpEntity<>(image, headers), String.class);
            JsonNode payload = responsePayload(response.getBody());
            String id = text(payload, "id");
            if (!StringUtils.hasText(id) || !id.matches("[1-9][0-9]*")) {
                throw new StorageException("Image library returned no image ID", null);
            }
            return id;
        } catch (HttpClientErrorException.Unauthorized e) {
            if (retryOnUnauthorized) {
                clearCachedAccessToken();
                return registerImage(image, false);
            }
            throw storageFailure(e);
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw storageFailure(e);
        }
    }

    private StoredFile upload(MultipartFile file, String bizDir, boolean retryOnUnauthorized) {
        try {
            String token = getAccessToken();
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            requestHeaders.setBearerAuth(token);
            requestHeaders.set("X-Project-Code", properties.getAppCode());

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", createFilePart(file));
            body.add("bizDir", bizDir);
            body.add("storageType", properties.getStorageType());

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint("/sys/storage/upload"),
                    HttpMethod.POST,
                    new HttpEntity<>(body, requestHeaders),
                    String.class);

            JsonNode payload = responsePayload(response.getBody());
            String objectName = text(payload, "key");
            String url = text(payload, "url");
            String expectedPrefix = "app/" + properties.getAppCode() + "/";
            if (!StringUtils.hasText(objectName) || !objectName.startsWith(expectedPrefix)) {
                throw new StorageException("Storage gateway returned an invalid object scope", null);
            }
            if (!StringUtils.hasText(url)) {
                throw new StorageException("Storage gateway returned no public URL", null);
            }
            return new StoredFile(url, objectName);
        } catch (HttpClientErrorException.Unauthorized e) {
            if (retryOnUnauthorized) {
                clearCachedAccessToken();
                return upload(file, bizDir, false);
            }
            throw storageFailure(e);
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw storageFailure(e);
        }
    }

    private HttpEntity<ByteArrayResource> createFilePart(MultipartFile file) throws Exception {
        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename());
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return originalFilename;
            }
        };

        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.parseMediaType(file.getContentType()));
        return new HttpEntity<>(resource, partHeaders);
    }

    private String getAccessToken() {
        String token = cachedAccessToken;
        if (StringUtils.hasText(token)) {
            return token;
        }
        synchronized (this) {
            if (!StringUtils.hasText(cachedAccessToken)) {
                cachedAccessToken = login();
            }
            return cachedAccessToken;
        }
    }

    private String login() {
        Map<String, String> request = new LinkedHashMap<>();
        request.put("appCode", properties.getAppCode());
        request.put("appId", properties.getAppId());
        request.put("secretCode", properties.getSecretCode());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Project-Code", properties.getAppCode());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint("/auth/app-login"),
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    String.class);
            JsonNode payload = responsePayload(response.getBody());
            String token = text(payload, "accessToken");
            if (!StringUtils.hasText(token)) {
                token = text(payload, "token");
            }
            if (!StringUtils.hasText(token)) {
                throw new StorageException("Storage gateway login returned no access token", null);
            }
            return token;
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw storageFailure(e);
        }
    }

    private JsonNode responsePayload(String body) throws Exception {
        if (!StringUtils.hasText(body)) {
            throw new StorageException("Storage gateway returned an empty response", null);
        }
        JsonNode root = objectMapper.readTree(body);
        JsonNode data = root.get("data");
        return data != null && data.isObject() ? data : root;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (file.getSize() > properties.getMaxSizeBytes()) {
            throw new FileTooLargeException("File exceeds the configured size limit");
        }

        String contentType = file.getContentType();
        boolean allowed = contentType != null && properties.getAllowedContentTypes().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals(contentType.toLowerCase(Locale.ROOT)));
        if (!allowed) {
            throw new IllegalArgumentException("Unsupported file content type");
        }
    }

    private void validateClientConfiguration() {
        if (!StringUtils.hasText(properties.getBaseUrl())
                || !StringUtils.hasText(properties.getAppCode())
                || !StringUtils.hasText(properties.getAppId())
                || !StringUtils.hasText(properties.getSecretCode())) {
            throw new StorageException("SaaS storage client is not configured", null);
        }
    }

    private String endpoint(String path) {
        return properties.getBaseUrl().replaceAll("/+$", "") + path;
    }

    private void clearCachedAccessToken() {
        cachedAccessToken = null;
    }

    private StorageException storageFailure(Exception cause) {
        log.error("SaaS storage gateway request failed: appCode={}", properties.getAppCode(), cause);
        return new StorageException("File storage service is unavailable", cause);
    }
}
