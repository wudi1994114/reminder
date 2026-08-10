package com.core.reminder.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.core.reminder.config.WechatConfig;
import com.core.reminder.dto.WechatApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WechatApiServiceSecurityTest {

    @Test
    void loginLogsContainStatusButNoCredentialsOrSessionData() throws Exception {
        String appId = "audit-app-id";
        String appSecret = "audit-app-secret";
        String loginCode = "one-time-login-code";
        String openId = "sensitive-openid";
        String sessionKey = "sensitive-session-key";

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/sns/jscode2session", exchange -> {
            byte[] body = ("{\"openid\":\"" + openId + "\",\"session_key\":\"" + sessionKey + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        Logger logger = (Logger) LoggerFactory.getLogger(WechatApiService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            WechatConfig config = new WechatConfig();
            config.setAppid(appId);
            config.setSecret(appSecret);
            config.setApiBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());

            WechatApiService service = new WechatApiService();
            ReflectionTestUtils.setField(service, "wechatConfig", config);
            ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());

            WechatApiResponse response = service.jscode2session(loginCode);
            assertEquals(openId, response.getOpenid());

            String messages = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));

            assertTrue(messages.contains("200"));
            assertFalse(messages.contains(appId));
            assertFalse(messages.contains(appSecret));
            assertFalse(messages.contains(loginCode));
            assertFalse(messages.contains(openId));
            assertFalse(messages.contains(sessionKey));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            server.stop(0);
        }
    }

    @Test
    void sourceDoesNotDisableCertificateOrHostnameVerification() throws IOException {
        Path source = Path.of(System.getProperty("user.dir"),
                "src/main/java/com/core/reminder/service/WechatApiService.java");
        String code = Files.readString(source);

        assertFalse(code.contains("NoopHostnameVerifier"));
        assertFalse(code.contains("TrustStrategy"));
        assertFalse(code.contains("loadTrustMaterial"));
        assertFalse(code.contains("X509Certificate"));
    }
}
