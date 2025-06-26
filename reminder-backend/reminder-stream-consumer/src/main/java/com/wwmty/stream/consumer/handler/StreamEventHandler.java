package com.wwmty.stream.consumer.handler;

import java.util.Map;

/**
 * 用于处理 Redis Stream 事件的接口。
 * 每个处理器负责一个特定的命令。
 */
public interface StreamEventHandler {
    /**
     * 处理流事件。
     *
     * @param eventData 清理后的事件数据
     * @param messageId 消息ID
     */
    void handle(Map<String, String> eventData, String messageId);

    /**
     * 此处理器支持的命令字符串。
     * 工厂类使用此方法将消息路由到正确的处理器。
     *
     * @return 命令字符串
     */
    String getCommand();
} 