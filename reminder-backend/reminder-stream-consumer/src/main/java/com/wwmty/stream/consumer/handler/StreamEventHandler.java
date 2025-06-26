package com.wwmty.stream.consumer.handler;

import org.springframework.data.redis.connection.stream.MapRecord;

/**
 * 用于处理 Redis Stream 事件的接口。
 * 每个处理器负责一个特定的命令。
 */
public interface StreamEventHandler {
    /**
     * 处理流事件。
     *
     * @param record 流消息记录
     */
    void handle(MapRecord<String, String, String> record);

    /**
     * 此处理器支持的命令字符串。
     * 工厂类使用此方法将消息路由到正确的处理器。
     *
     * @return 命令字符串
     */
    String getCommand();
} 