package com.wwmty.stream.consumer.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class LogEventHandler implements StreamEventHandler {

    @Override
    public void handle(Map<String, String> eventData, String messageId) {
        log.info("为消息 ID {} 执行 LogEventHandler。", messageId);
        log.info("消息 ID: {}", messageId);
        log.info("负载: {}", eventData);
        log.info("消息 ID {} 的 LogEventHandler 执行完毕。", messageId);
    }

    @Override
    public String getCommand() {
        // 当消息的 "command" 字段为 "LOG_EVENT" 时，将调用此处理器
        return "LOG_EVENT";
    }
} 