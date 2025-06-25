package com.wwmty.stream.consumer.service;

import com.wwmty.stream.consumer.handler.StreamEventHandlerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Slf4j
@Service
public class StreamConsumerService {

    private final StreamEventHandlerFactory handlerFactory;

    @Autowired
    public StreamConsumerService(StreamEventHandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
    }

    /**
     * Receives a stream message, extracts the command, and delegates to the appropriate handler.
     * The @Transactional annotation has been moved to the specific handlers that need it.
     * @param record The message record from Redis Stream.
     */
    public void handleStreamEvent(MapRecord<String, String, String> record) {
        log.debug("Received message from stream [{}]: {}", record.getStream(), record.getValue());

        // The command is expected to be in a field named "command" in the stream message payload.
        final String command = record.getValue().get("command");

        if (!StringUtils.hasText(command)) {
            log.error("消息 id {} 中的 command 字段缺失或为空。无法处理。 负载: {}", record.getId(), record.getValue());
            // 在这里你可能需要添加处理格式错误消息的逻辑，
            // 例如，发送到死信队列。
            return;
        }

        Optional<StreamEventHandler> handlerOptional = handlerFactory.getHandler(command);
        if (handlerOptional.isPresent()) {
            StreamEventHandler handler = handlerOptional.get();
            try {
                log.info("发现命令 [{}] 的处理器。正在处理消息 id {}", command, record.getId());
                handler.handle(record);
                log.info("使用命令 [{}] 的处理器成功处理了消息 id {}", record.getId(), command);
            } catch (Exception e) {
                log.error("命令 [{}] 的处理器在处理消息 id {} 时失败: {}", command, record.getId(), e.getMessage(), e);
                // 处理失败消息的逻辑，例如，发送到死信队列。
            }
        } else {
            log.warn("未找到命令 [{}] 的处理器，消息 id {}。已忽略。", command, record.getId());
            // 处理未知命令的逻辑。
        }
    }
} 