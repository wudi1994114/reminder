package com.core.reminder.service;

import com.common.reminder.model.ComplexReminder;
import com.common.reminder.model.ReminderType;
import com.core.reminder.repository.ComplexReminderRepository;
import com.core.reminder.repository.SimpleReminderRepository;
import com.core.reminder.utils.CacheUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * ReminderEventServiceImpl 测试类
 * 主要测试用户复杂提醒数量限制功能
 */
@ExtendWith(MockitoExtension.class)
class ReminderEventServiceImplTest {

    @Mock
    private SimpleReminderRepository simpleReminderRepository;

    @Mock
    private ComplexReminderRepository complexReminderRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private CacheUtils cacheUtils;

    @InjectMocks
    private ReminderEventServiceImpl reminderEventService;

    private ComplexReminder testComplexReminder;

    @BeforeEach
    void setUp() {
        testComplexReminder = new ComplexReminder();
        testComplexReminder.setFromUserId(1L);
        testComplexReminder.setToUserId(1L);
        testComplexReminder.setTitle("测试复杂提醒");
        testComplexReminder.setDescription("测试描述");
        testComplexReminder.setCronExpression("0 0 9 * * ?");
        testComplexReminder.setReminderType(ReminderType.EMAIL);
    }

    @Test
    void testCreateComplexReminderWithSimpleReminders_Success() {
        // 模拟用户当前有19个复杂提醒（未达到限制）
        when(complexReminderRepository.countByFromUserId(1L)).thenReturn(19L);
        when(complexReminderRepository.save(testComplexReminder)).thenReturn(testComplexReminder);

        // 这应该成功执行，不抛出异常
        assertDoesNotThrow(() -> {
            reminderEventService.createComplexReminderWithSimpleReminders(testComplexReminder, 3);
        });
    }

    @Test
    void testCreateComplexReminderWithSimpleReminders_ExceedsLimit() {
        // 模拟用户当前有20个复杂提醒（已达到限制）
        when(complexReminderRepository.countByFromUserId(1L)).thenReturn(20L);

        // 这应该抛出IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            reminderEventService.createComplexReminderWithSimpleReminders(testComplexReminder, 3);
        });

        assertTrue(exception.getMessage().contains("用户复杂提醒数量已达到最大限制"));
        assertTrue(exception.getMessage().contains("20"));
    }

    @Test
    void testCreateComplexReminderWithSimpleReminders_ExactlyAtLimit() {
        // 模拟用户当前有20个复杂提醒（正好达到限制）
        when(complexReminderRepository.countByFromUserId(1L)).thenReturn(20L);

        // 这应该抛出IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            reminderEventService.createComplexReminderWithSimpleReminders(testComplexReminder, 3);
        });

        assertTrue(exception.getMessage().contains("用户复杂提醒数量已达到最大限制"));
    }

    @Test
    void testCreateComplexReminderWithSimpleReminders_NullUserId() {
        // 设置fromUserId为null
        testComplexReminder.setFromUserId(null);

        // 这应该抛出IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            reminderEventService.createComplexReminderWithSimpleReminders(testComplexReminder, 3);
        });

        assertTrue(exception.getMessage().contains("用户ID不能为空"));
    }

    @Test
    void testCreateComplexReminderWithSimpleReminders_ZeroReminders() {
        // 模拟用户当前有0个复杂提醒
        when(complexReminderRepository.countByFromUserId(1L)).thenReturn(0L);
        when(complexReminderRepository.save(testComplexReminder)).thenReturn(testComplexReminder);

        // 这应该成功执行，不抛出异常
        assertDoesNotThrow(() -> {
            reminderEventService.createComplexReminderWithSimpleReminders(testComplexReminder, 3);
        });
    }
}
