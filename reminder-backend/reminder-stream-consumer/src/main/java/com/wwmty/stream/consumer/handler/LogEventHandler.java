package com.wwmty.stream.consumer.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogEventHandler implements StreamEventHandler {

    @Override
    public void handle(MapRecord<String, String, String> record) {
        log.info("为消息 ID {} 执行 LogEventHandler。", record.getId());
        log.info("Stream: {}", record.getStream());
        log.info("消息 ID: {}", record.getId());
        log.info("负载: {}", record.getValue());
        log.info("消息 ID {} 的 LogEventHandler 执行完毕。", record.getId());
    }

    @Override
    public String getCommand() {
        // 当消息的 "command" 字段为 "LOG_EVENT" 时，将调用此处理器
        return "LOG_EVENT";
    }
} 