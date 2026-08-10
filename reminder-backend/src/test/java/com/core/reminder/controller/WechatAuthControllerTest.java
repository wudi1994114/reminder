package com.core.reminder.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WechatAuthControllerTest {

    @Test
    void exposesCodeLoginButNoCloudGatewayLogin() {
        String[] postMappings = Arrays.stream(WechatAuthController.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(PostMapping.class))
                .filter(annotation -> annotation != null)
                .flatMap(annotation -> Arrays.stream(annotation.value()))
                .toArray(String[]::new);

        assertTrue(Arrays.asList(postMappings).contains("/login"));
        assertFalse(Arrays.asList(postMappings).contains("/cloud-login"));
    }
}
