package com.core.reminder.utils;

import com.common.reminder.utils.JacksonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis Stream事件发布器
 * 用于向Redis Stream发送事件消息
 */
@Slf4j
@Component
public class StreamEventPublisher {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${reminder.stream.key:complex-reminder-stream}")
    private String streamKey;

    /**
     * 发送复杂提醒生成事件
     * 
     * @param complexReminderId 复杂提醒ID
     * @param monthsAhead 要生成的月数
     * @param userId 用户ID
     */
    public void publishComplexReminderGenerationEvent(Long complexReminderId, int monthsAhead, Long userId) {
        try {
            Map<String, String> eventData = new HashMap<>();
            eventData.put("command", "GENERATE_COMPLEX_REMINDER");
            eventData.put("complexReminderId", complexReminderId.toString());
            eventData.put("monthsAhead", String.valueOf(monthsAhead));
            eventData.put("userId", userId.toString());
            eventData.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            // 发送到Redis Stream - 使用更直接的方式避免序列化问题
            String messageId = redisTemplate.opsForStream().add(streamKey, eventData).getValue();
            
            log.info("成功发送复杂提醒生成事件到Stream - 消息ID: {}, 复杂提醒ID: {}, 月数: {}", 
                    messageId, complexReminderId, monthsAhead);
                    
        } catch (Exception e) {
            log.error("发送复杂提醒生成事件失败 - 复杂提醒ID: {}, 月数: {}", 
                    complexReminderId, monthsAhead, e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 发送复杂提醒更新事件
     * 
     * @param complexReminderId 复杂提醒ID
     * @param monthsAhead 要生成的月数
     * @param userId 用户ID
     */
    public void publishComplexReminderUpdateEvent(Long complexReminderId, int monthsAhead, Long userId) {
        try {
            Map<String, String> eventData = new HashMap<>();
            eventData.put("command", "UPDATE_COMPLEX_REMINDER");
            eventData.put("complexReminderId", complexReminderId.toString());
            eventData.put("monthsAhead", String.valueOf(monthsAhead));
            eventData.put("userId", userId.toString());
            eventData.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            // 发送到Redis Stream - 使用更直接的方式避免序列化问题
            String messageId = redisTemplate.opsForStream().add(streamKey, eventData).getValue();
            
            log.info("成功发送复杂提醒更新事件到Stream - 消息ID: {}, 复杂提醒ID: {}, 月数: {}", 
                    messageId, complexReminderId, monthsAhead);
                    
        } catch (Exception e) {
            log.error("发送复杂提醒更新事件失败 - 复杂提醒ID: {}, 月数: {}", 
                    complexReminderId, monthsAhead, e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 发送通用事件
     * 
     * @param command 命令类型
     * @param eventData 事件数据
     */
    public void publishEvent(String command, Map<String, String> eventData) {
        try {
            eventData.put("command", command);
            eventData.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            // 发送到Redis Stream - 使用更直接的方式避免序列化问题
            String messageId = redisTemplate.opsForStream().add(streamKey, eventData).getValue();
            
            log.info("成功发送事件到Stream - 消息ID: {}, 命令: {}, 数据: {}", 
                    messageId, command, JacksonUtils.toJson(eventData));
                    
        } catch (Exception e) {
            log.error("发送事件失败 - 命令: {}, 数据: {}", command, JacksonUtils.toJson(eventData), e);
            // 不抛出异常，避免影响主流程
        }
    }
} 