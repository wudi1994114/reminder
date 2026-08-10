package com.core.reminder.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WechatConfigTest {

    @Test
    void containsNoBuiltInCredentialsOrCloudEnvironment() {
        WechatConfig config = new WechatConfig();

        assertEquals("", config.getAppid());
        assertEquals("", config.getSecret());
        assertFalse(Arrays.stream(WechatConfig.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("cloudEnv")));
    }
}
