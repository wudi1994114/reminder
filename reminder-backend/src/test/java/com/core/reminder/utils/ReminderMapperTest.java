package com.core.reminder.utils;

import com.core.reminder.dto.SimpleReminderDTO;
import com.common.reminder.model.SimpleReminder;
import com.common.reminder.model.ReminderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReminderMapper测试类
 * 验证DTO与实体对象之间的转换逻辑
 */
class ReminderMapperTest {

    private ReminderMapper reminderMapper;

    @BeforeEach
    void setUp() {
        reminderMapper = new ReminderMapper();
    }

    @Test
    void testUpdateEntityFromDTO_shouldOnlyUpdateNonNullFields() {
        // 准备现有实体
        SimpleReminder existingEntity = new SimpleReminder();
        existingEntity.setId(1L);
        existingEntity.setFromUserId(100L);
        existingEntity.setToUserId(200L);
        existingEntity.setTitle("原始标题");
        existingEntity.setDescription("原始描述");
        existingEntity.setEventTime(OffsetDateTime.now());
        existingEntity.setReminderType(ReminderType.EMAIL);

        // 准备DTO（只包含部分字段）
        SimpleReminderDTO dto = new SimpleReminderDTO();
        dto.setTitle("更新后的标题");
        dto.setDescription("更新后的描述");
        // 注意：fromUserId 和 toUserId 为 null

        // 执行更新
        reminderMapper.updateEntityFromDTO(dto, existingEntity);

        // 验证：只有非null字段被更新
        assertEquals("更新后的标题", existingEntity.getTitle());
        assertEquals("更新后的描述", existingEntity.getDescription());
        
        // 验证：null字段不应覆盖现有值
        assertEquals(Long.valueOf(100L), existingEntity.getFromUserId());
        assertEquals(Long.valueOf(200L), existingEntity.getToUserId());
        assertEquals(ReminderType.EMAIL, existingEntity.getReminderType());
        assertNotNull(existingEntity.getEventTime());
    }

    @Test
    void testUpdateEntityFromDTO_withNullDTO() {
        SimpleReminder existingEntity = new SimpleReminder();
        existingEntity.setId(1L);
        existingEntity.setFromUserId(100L);
        existingEntity.setToUserId(200L);

        // 使用null DTO调用方法
        reminderMapper.updateEntityFromDTO(null, existingEntity);

        // 验证实体没有被修改
        assertEquals(Long.valueOf(1L), existingEntity.getId());
        assertEquals(Long.valueOf(100L), existingEntity.getFromUserId());
        assertEquals(Long.valueOf(200L), existingEntity.getToUserId());
    }

    @Test
    void testUpdateEntityFromDTO_withNullEntity() {
        SimpleReminderDTO dto = new SimpleReminderDTO();
        dto.setTitle("测试标题");

        // 使用null实体调用方法（不应抛出异常）
        assertDoesNotThrow(() -> {
            reminderMapper.updateEntityFromDTO(dto, null);
        });
    }

    @Test
    void testUpdateEntityFromDTO_withAllNullFields() {
        // 准备现有实体
        SimpleReminder existingEntity = new SimpleReminder();
        existingEntity.setId(1L);
        existingEntity.setFromUserId(100L);
        existingEntity.setToUserId(200L);
        existingEntity.setTitle("原始标题");

        // 准备空DTO（所有字段都为null）
        SimpleReminderDTO dto = new SimpleReminderDTO();

        // 执行更新
        reminderMapper.updateEntityFromDTO(dto, existingEntity);

        // 验证：所有原始值都应保持不变
        assertEquals(Long.valueOf(1L), existingEntity.getId());
        assertEquals(Long.valueOf(100L), existingEntity.getFromUserId());
        assertEquals(Long.valueOf(200L), existingEntity.getToUserId());
        assertEquals("原始标题", existingEntity.getTitle());
    }
} 