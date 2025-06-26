package com.wwmty.stream.consumer.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class StreamEventHandlerFactory {

    private final Map<String, StreamEventHandler> handlerMap;

    /**
     * 注入所有实现 StreamEventHandler 的 bean，并根据它们的命令进行映射。
     * @param handlers 由 Spring 提供的处理器 bean 列表。
     */
    @Autowired
    public StreamEventHandlerFactory(List<StreamEventHandler> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(StreamEventHandler::getCommand, Function.identity()));
    }

    /**
     * 根据给定的命令检索处理器。
     *
     * @param command 用于查找处理器的命令。
     * @return 一个包含处理器的 Optional (如果找到)，否则为空的 Optional。
     */
    public Optional<StreamEventHandler> getHandler(String command) {
        return Optional.ofNullable(handlerMap.get(command));
    }
} 