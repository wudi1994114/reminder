package com.example.reminder.utils;

import com.example.reminder.model.AppUser;
import com.example.reminder.model.SimpleReminder;
import com.example.reminder.model.ReminderType;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RedisUtilsTest {

    @Autowired
    private RedisUtils redisUtils;

    private final String testKey = "test:key";
    private final String testListKey = "test:list";
    private final String testMapKey = "test:map";
    private final String testObjectKey = "test:object";
    private final String testComplexKey = "test:complex";

    @BeforeEach
    void setUp() {
        // 清理测试前可能存在的键
        cleanup();
    }

    @AfterEach
    void cleanup() {
        // 测试后清理所有测试键
        redisUtils.del(testKey, testListKey, testMapKey, testObjectKey, testComplexKey);
    }

    @Test
    void testSetAndGet() {
        // 测试简单的字符串存取
        String value = "Hello Redis";
        boolean setResult = redisUtils.set(testKey, value);
        assertTrue(setResult, "设置值应该成功");

        Object getValue = redisUtils.get(testKey);
        assertEquals(value, getValue, "获取的值应该与设置的值相同");
    }

    @Test
    void testSetWithExpire() {
        // 测试设置值并设置过期时间
        String value = "临时数据";
        boolean setResult = redisUtils.set(testKey, value, 2); // 2秒过期
        assertTrue(setResult, "设置带过期时间的值应该成功");

        Object getValue = redisUtils.get(testKey);
        assertEquals(value, getValue, "获取的值应该与设置的值相同");

        // 验证过期时间
        long expireTime = redisUtils.getExpire(testKey);
        assertTrue(expireTime > 0 && expireTime <= 2, "过期时间应该在合理范围内");
    }

    @Test
    void testSetJsonAndGetObj() {
        // 创建一个用户对象
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername("testUser");
        user.setNickname("测试用户");
        user.setEmail("test@example.com");
        user.setPassword("password123");
        user.setPhoneNumber("13800138000");
        user.setBirthDate(LocalDate.of(1990, 1, 1));

        // 使用JSON序列化存储
        boolean setResult = redisUtils.setJson(testObjectKey, user);
        assertTrue(setResult, "设置JSON对象应该成功");

        // 使用类型转换获取
        AppUser retrievedUser = redisUtils.getObj(testObjectKey, AppUser.class);
        assertNotNull(retrievedUser, "获取的对象不应为空");
        assertEquals(user.getId(), retrievedUser.getId(), "ID应该相同");
        assertEquals(user.getUsername(), retrievedUser.getUsername(), "用户名应该相同");
        assertEquals(user.getEmail(), retrievedUser.getEmail(), "邮箱应该相同");
    }

    @Test
    void testComplexObjectWithTypeReference() {
        // 创建一个复杂结构：包含用户列表的Map
        Map<String, List<SimpleReminder>> reminderMap = new HashMap<>();
        
        List<SimpleReminder> importantReminders = new ArrayList<>();
        List<SimpleReminder> normalReminders = new ArrayList<>();
        
        SimpleReminder reminder1 = new SimpleReminder();
        reminder1.setId(1L);
        reminder1.setTitle("重要会议");
        reminder1.setDescription("项目启动会议");
        reminder1.setEventTime(OffsetDateTime.now().plusDays(1));
        reminder1.setReminderType(ReminderType.EMAIL);
        reminder1.setToUserId(1001L);
        importantReminders.add(reminder1);
        
        SimpleReminder reminder2 = new SimpleReminder();
        reminder2.setId(2L);
        reminder2.setTitle("常规任务");
        reminder2.setDescription("日常代码审查");
        reminder2.setEventTime(OffsetDateTime.now().plusDays(2));
        reminder2.setReminderType(ReminderType.SMS);
        reminder2.setToUserId(1002L);
        normalReminders.add(reminder2);
        
        reminderMap.put("important", importantReminders);
        reminderMap.put("normal", normalReminders);
        
        // 存储复杂对象
        boolean setResult = redisUtils.setJson(testComplexKey, reminderMap);
        assertTrue(setResult, "设置复杂对象应该成功");
        
        // 使用TypeReference获取
        TypeReference<Map<String, List<SimpleReminder>>> typeRef = 
            new TypeReference<Map<String, List<SimpleReminder>>>() {};
        Map<String, List<SimpleReminder>> retrievedMap = redisUtils.getObj(testComplexKey, typeRef);
        
        assertNotNull(retrievedMap, "获取的复杂对象不应为空");
        assertEquals(2, retrievedMap.size(), "Map应该有两个键");
        
        List<SimpleReminder> retrievedImportant = retrievedMap.get("important");
        assertNotNull(retrievedImportant, "important列表不应为空");
        assertEquals(1, retrievedImportant.size(), "important列表应该有一个元素");
        assertEquals("重要会议", retrievedImportant.get(0).getTitle(), "标题应该匹配");
        
        List<SimpleReminder> retrievedNormal = retrievedMap.get("normal");
        assertNotNull(retrievedNormal, "normal列表不应为空");
        assertEquals("常规任务", retrievedNormal.get(0).getTitle(), "标题应该匹配");
    }

    @Test
    void testHashOperations() {
        // 测试Hash操作
        String hashKey = "user:info";
        
        // 设置单个字段
        redisUtils.hset(testMapKey, "username", "zhang3");
        redisUtils.hset(testMapKey, "age", 25);
        
        // 获取单个字段
        Object username = redisUtils.hget(testMapKey, "username");
        assertEquals("zhang3", username, "Hash字段值应该匹配");
        
        // 批量设置多个字段
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("email", "zhang3@example.com");
        userMap.put("phone", "13900001111");
        redisUtils.hmset(testMapKey, userMap);
        
        // 获取整个Hash
        Map<Object, Object> retrievedMap = redisUtils.hmget(testMapKey);
        assertEquals(4, retrievedMap.size(), "Hash应该有4个字段");
        assertEquals("zhang3", retrievedMap.get("username"), "username字段应该匹配");
        assertEquals("13900001111", retrievedMap.get("phone"), "phone字段应该匹配");
        
        // 检查字段是否存在
        assertTrue(redisUtils.hHasKey(testMapKey, "email"), "email字段应该存在");
        assertFalse(redisUtils.hHasKey(testMapKey, "address"), "address字段不应该存在");
        
        // 删除字段
        redisUtils.hdel(testMapKey, "age");
        assertFalse(redisUtils.hHasKey(testMapKey, "age"), "被删除的字段不应该存在");
    }
} 