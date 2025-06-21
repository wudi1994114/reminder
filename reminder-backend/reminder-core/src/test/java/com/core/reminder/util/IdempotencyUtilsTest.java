package com.core.reminder.util;

import com.common.reminder.model.ComplexReminder;
import com.common.reminder.model.ReminderType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 幂等性工具类测试
 */
class IdempotencyUtilsTest {

    @Test
    void testGenerateRandomIdempotencyKey() {
        String key1 = IdempotencyUtils.generateRandomIdempotencyKey();
        String key2 = IdempotencyUtils.generateRandomIdempotencyKey();
        
        assertNotNull(key1);
        assertNotNull(key2);
        assertNotEquals(key1, key2);
        assertTrue(IdempotencyUtils.isValidIdempotencyKey(key1));
        assertTrue(IdempotencyUtils.isValidIdempotencyKey(key2));
        assertTrue(IdempotencyUtils.isRandomKey(key1));
        assertTrue(IdempotencyUtils.isRandomKey(key2));
    }

    @Test
    void testGenerateBusinessBasedIdempotencyKey() {
        // 测试相同业务数据生成相同的幂等键
        String key1 = IdempotencyUtils.generateBusinessBasedIdempotencyKey(
                1L, 2L, "测试提醒", "0 0 9 * * ?", ReminderType.EMAIL,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), 10
        );
        
        String key2 = IdempotencyUtils.generateBusinessBasedIdempotencyKey(
                1L, 2L, "测试提醒", "0 0 9 * * ?", ReminderType.EMAIL,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), 10
        );
        
        assertEquals(key1, key2);
        assertTrue(IdempotencyUtils.isValidIdempotencyKey(key1));
        assertTrue(IdempotencyUtils.isBusinessBasedKey(key1));
        assertFalse(IdempotencyUtils.isRandomKey(key1));
    }

    @Test
    void testGenerateBusinessBasedIdempotencyKeyWithDifferentData() {
        // 测试不同业务数据生成不同的幂等键
        String key1 = IdempotencyUtils.generateBusinessBasedIdempotencyKey(
                1L, 2L, "测试提醒1", "0 0 9 * * ?", ReminderType.EMAIL,
                null, null, null
        );
        
        String key2 = IdempotencyUtils.generateBusinessBasedIdempotencyKey(
                1L, 2L, "测试提醒2", "0 0 9 * * ?", ReminderType.EMAIL,
                null, null, null
        );
        
        assertNotEquals(key1, key2);
        assertTrue(IdempotencyUtils.isValidIdempotencyKey(key1));
        assertTrue(IdempotencyUtils.isValidIdempotencyKey(key2));
    }

    @Test
    void testGenerateBusinessBasedIdempotencyKeyFromComplexReminder() {
        ComplexReminder reminder = new ComplexReminder();
        reminder.setFromUserId(1L);
        reminder.setToUserId(2L);
        reminder.setTitle("测试提醒");
        reminder.setCronExpression("0 0 9 * * ?");
        reminder.setReminderType(ReminderType.EMAIL);
        reminder.setValidFrom(LocalDate.of(2024, 1, 1));
        reminder.setValidUntil(LocalDate.of(2024, 12, 31));
        reminder.setMaxExecutions(10);
        
        String key = IdempotencyUtils.generateBusinessBasedIdempotencyKey(reminder);
        
        assertNotNull(key);
        assertTrue(IdempotencyUtils.isValidIdempotencyKey(key));
        assertTrue(IdempotencyUtils.isBusinessBasedKey(key));
    }

    @Test
    void testGenerateBusinessBasedIdempotencyKeyWithNullReminder() {
        assertThrows(IllegalArgumentException.class, () -> {
            IdempotencyUtils.generateBusinessBasedIdempotencyKey(null);
        });
    }

    @Test
    void testIsValidIdempotencyKey() {
        // 有效的幂等键
        assertTrue(IdempotencyUtils.isValidIdempotencyKey("business_abc123"));
        assertTrue(IdempotencyUtils.isValidIdempotencyKey("12345678-1234-1234-1234-123456789012"));
        assertTrue(IdempotencyUtils.isValidIdempotencyKey("custom_key_123"));
        
        // 无效的幂等键
        assertFalse(IdempotencyUtils.isValidIdempotencyKey(null));
        assertFalse(IdempotencyUtils.isValidIdempotencyKey(""));
        assertFalse(IdempotencyUtils.isValidIdempotencyKey("   "));
        assertFalse(IdempotencyUtils.isValidIdempotencyKey("key with spaces"));
        assertFalse(IdempotencyUtils.isValidIdempotencyKey("key@with#special$chars"));
        assertFalse(IdempotencyUtils.isValidIdempotencyKey("a")); // 太短
    }

    @Test
    void testIsBusinessBasedKey() {
        assertTrue(IdempotencyUtils.isBusinessBasedKey("business_abc123"));
        assertFalse(IdempotencyUtils.isBusinessBasedKey("custom_key"));
        assertFalse(IdempotencyUtils.isBusinessBasedKey("12345678-1234-1234-1234-123456789012"));
        assertFalse(IdempotencyUtils.isBusinessBasedKey(null));
    }

    @Test
    void testIsRandomKey() {
        assertTrue(IdempotencyUtils.isRandomKey("12345678-1234-1234-1234-123456789012"));
        assertTrue(IdempotencyUtils.isRandomKey("abcdefgh-1234-5678-9abc-def123456789"));
        assertFalse(IdempotencyUtils.isRandomKey("business_abc123"));
        assertFalse(IdempotencyUtils.isRandomKey("custom_key"));
        assertFalse(IdempotencyUtils.isRandomKey("invalid-uuid-format"));
        assertFalse(IdempotencyUtils.isRandomKey(null));
    }

    @Test
    void testConsistencyWithNullValues() {
        // 测试NULL值的一致性处理
        String key1 = IdempotencyUtils.generateBusinessBasedIdempotencyKey(
                1L, 2L, "测试", "0 0 9 * * ?", ReminderType.EMAIL,
                null, null, null
        );
        
        String key2 = IdempotencyUtils.generateBusinessBasedIdempotencyKey(
                1L, 2L, "测试", "0 0 9 * * ?", ReminderType.EMAIL,
                null, null, null
        );
        
        assertEquals(key1, key2);
    }

    @Test
    void testWhitespaceHandling() {
        // 测试空白字符的处理
        String key1 = IdempotencyUtils.generateBusinessBasedIdempotencyKey(
                1L, 2L, "  测试提醒  ", "0 0 9 * * ?", ReminderType.EMAIL,
                null, null, null
        );
        
        String key2 = IdempotencyUtils.generateBusinessBasedIdempotencyKey(
                1L, 2L, "测试提醒", "0 0 9 * * ?", ReminderType.EMAIL,
                null, null, null
        );
        
        assertEquals(key1, key2);
    }
}
