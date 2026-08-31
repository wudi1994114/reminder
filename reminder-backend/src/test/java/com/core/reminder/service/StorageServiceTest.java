package com.core.reminder.service;

import com.core.reminder.config.SaasStorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StorageServiceTest {

    private MockRestServiceServer server;
    private StorageService storageService;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();

        SaasStorageProperties properties = new SaasStorageProperties();
        properties.setBaseUrl("http://saas-admin-backend:8080");
        properties.setAppCode("beiwangji");
        properties.setAppId("audit-app-id");
        properties.setSecretCode("audit-secret");
        properties.setBizDir("avatars");
        properties.setStorageType("MINIO");
        properties.setMaxSizeBytes(1024L);
        properties.setAllowedContentTypes(Arrays.asList("image/jpeg", "image/png"));

        storageService = new StorageService(restTemplate, new ObjectMapper(), properties);
    }

    @Test
    void authenticatesAsBeiwangjiAppAndUploadsThroughSaasStorageGateway() {
        expectLogin("access-token-1");
        server.expect(requestTo("http://saas-admin-backend:8080/sys/storage/upload"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token-1"))
                .andExpect(header("X-Project-Code", "beiwangji"))
                .andExpect(content().string(containsString("avatars")))
                .andExpect(content().string(containsString("MINIO")))
                .andRespond(withSuccess("{\"storageType\":\"MINIO\",\"key\":\"app/beiwangji/avatars/20260810/avatar.png\",\"url\":\"https://image.example.com/beiwangji/avatar.png\"}", MediaType.APPLICATION_JSON));

        StoredFile stored = storageService.store(image("avatar.png", "image/png", 3));

        assertEquals("app/beiwangji/avatars/20260810/avatar.png", stored.getObjectName());
        assertEquals("https://image.example.com/beiwangji/avatar.png", stored.getUrl());
        server.verify();
    }

    @Test
    void defaultsGatewayCallsAndObjectScopeToBeiwangjiTenant() {
        SaasStorageProperties defaultProperties = new SaasStorageProperties();
        defaultProperties.setBaseUrl("http://saas-admin-backend:8080");
        defaultProperties.setAppId("audit-app-id");
        defaultProperties.setSecretCode("audit-secret");
        defaultProperties.setBizDir("avatars");
        defaultProperties.setStorageType("MINIO");
        defaultProperties.setMaxSizeBytes(1024L);
        defaultProperties.setAllowedContentTypes(Arrays.asList("image/jpeg", "image/png"));
        RestTemplate defaultRestTemplate = new RestTemplate();
        StorageService defaultStorageService = new StorageService(
                defaultRestTemplate, new ObjectMapper(), defaultProperties);
        MockRestServiceServer defaultServer = MockRestServiceServer.bindTo(defaultRestTemplate).build();

        defaultServer.expect(requestTo("http://saas-admin-backend:8080/auth/app-login"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Project-Code", "beiwangji"))
                .andExpect(jsonPath("$.appCode").value("beiwangji"))
                .andRespond(withSuccess("{\"accessToken\":\"beiwangji-token\"}", MediaType.APPLICATION_JSON));
        defaultServer.expect(requestTo("http://saas-admin-backend:8080/sys/storage/upload"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer beiwangji-token"))
                .andExpect(header("X-Project-Code", "beiwangji"))
                .andRespond(withSuccess("{\"key\":\"app/beiwangji/avatars/default.png\",\"url\":\"https://image.example.com/beiwangji/default.png\"}", MediaType.APPLICATION_JSON));

        StoredFile stored = defaultStorageService.store(image("avatar.png", "image/png", 3));

        assertEquals("app/beiwangji/avatars/default.png", stored.getObjectName());
        defaultServer.verify();
    }

    @Test
    void reauthenticatesOnceWhenCachedAppTokenIsRejected() {
        expectLogin("expired-token");
        server.expect(requestTo("http://saas-admin-backend:8080/sys/storage/upload"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer expired-token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        expectLogin("fresh-token");
        server.expect(requestTo("http://saas-admin-backend:8080/sys/storage/upload"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer fresh-token"))
                .andRespond(withSuccess("{\"key\":\"app/beiwangji/avatars/new.png\",\"url\":\"https://image.example.com/new.png\"}", MediaType.APPLICATION_JSON));

        StoredFile stored = storageService.store(image("avatar.png", "image/png", 3));

        assertEquals("app/beiwangji/avatars/new.png", stored.getObjectName());
        server.verify();
    }

    @Test
    void rejectsUnsupportedContentTypeBeforeCallingSaas() {
        assertThrows(IllegalArgumentException.class,
                () -> storageService.store(image("avatar.svg", "image/svg+xml", 3)));
        server.verify();
    }

    @Test
    void rejectsOversizedFileBeforeCallingSaas() {
        assertThrows(FileTooLargeException.class,
                () -> storageService.store(image("avatar.jpg", "image/jpeg", 1025)));
        server.verify();
    }

    @Test
    void doesNotReturnAProfileUrlWhenSaasStorageFails() {
        server.expect(requestTo("http://saas-admin-backend:8080/auth/app-login"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThrows(StorageException.class,
                () -> storageService.store(image("avatar.jpg", "image/jpeg", 3)));
        server.verify();
    }

    private void expectLogin(String token) {
        server.expect(requestTo("http://saas-admin-backend:8080/auth/app-login"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Project-Code", "beiwangji"))
                .andExpect(jsonPath("$.appCode").value("beiwangji"))
                .andExpect(jsonPath("$.appId").value("audit-app-id"))
                .andExpect(jsonPath("$.secretCode").value("audit-secret"))
                .andRespond(withSuccess("{\"accessToken\":\"" + token + "\"}", MediaType.APPLICATION_JSON));
    }

    private MockMultipartFile image(String filename, String contentType, int size) {
        return new MockMultipartFile("file", filename, contentType, new byte[size]);
    }
}
