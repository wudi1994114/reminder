package com.wwmty.stream.consumer.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wwmty.stream.consumer.model.StreamEvent;
import com.wwmty.stream.consumer.repository.StreamEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class PersistenceEventHandler implements StreamEventHandler {

    private final StreamEventRepository streamEventRepository;
    private final ObjectMapper objectMapper;


    @Autowired
    public PersistenceEventHandler(StreamEventRepository streamEventRepository, ObjectMapper objectMapper) {
        this.streamEventRepository = streamEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void handle(MapRecord<String, String, String> record) {
        log.info("为消息 ID {} 执行 PersistenceEventHandler", record.getId());
        try {
            String payload;
            try {
                // 将 map 类型的负载序列化为 JSON 字符串以便存储
                payload = objectMapper.writeValueAsString(record.getValue());
            } catch (JsonProcessingException e) {
                log.warn("无法将消息 ID {} 的负载序列化为 JSON。回退到使用 toString()", record.getId());
                payload = record.getValue().toString();
            }


            StreamEvent event = new StreamEvent();
            event.setStreamKey(record.getStream());
            event.setMessageId(record.getId().toString());
            event.setPayload(payload);

            streamEventRepository.save(event);
            log.info("成功持久化消息 ID {}", record.getId());
        } catch (Exception e) {
            log.error("持久化消息 ID {} 时出错: {}", record.getId(), e.getMessage(), e);
            // 重新抛出异常以确保事务回滚。
            throw new RuntimeException("持久化事件失败", e);
        }
    }

    @Override
    public String getCommand() {
        // 当消息的 "command" 字段为 "PERSIST_EVENT" 时，将调用此处理器
        return "PERSIST_EVENT";
    }
} 