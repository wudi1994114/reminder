package com.core.reminder.enums;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPreferenceKeyTest {

    @Test
    void exposesStableKeysUsedByApiAndJobs() {
        assertEquals("wechatAuthCount", UserPreferenceKey.WECHAT_AUTH_COUNT.getKey());
        assertEquals("0", UserPreferenceKey.WECHAT_AUTH_COUNT.getDefaultValue());
        assertTrue(Arrays.asList(UserPreferenceKey.getAllKeys()).contains("userTagList"));
        assertTrue(UserPreferenceKey.isValidKey("defaultReminderType"));
        assertFalse(UserPreferenceKey.isValidKey("unknownPreference"));
    }
}
