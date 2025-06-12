package com.task.reminder.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通知发送器工厂类
 * 根据指定类型获取对应的通知发送器
 */
@Slf4j
@Component
public class NotificationSenderFactory {

    @Autowired
    private List<NotificationSender> notificationSenders;

    private Map<String, NotificationSender> senderMap;

    @PostConstruct
    public void init() {
        // 创建发送器映射
        senderMap = notificationSenders.stream()
                .collect(Collectors.toMap(
                        sender -> sender.getSenderType().toUpperCase(),
                        Function.identity()
                ));

        log.info("通知发送器工厂初始化完成");
        log.info("可用发送器: {}", senderMap.keySet());
    }



    /**
     * 获取指定类型的发送器
     *
     * @param senderType 发送器类型
     * @return 发送器实例，如果不存在则返回null
     */
    public NotificationSender getSender(String senderType) {
        return senderMap.get(senderType.toUpperCase());
    }

    /**
     * 获取所有可用的发送器类型
     *
     * @return 发送器类型列表
     */
    public List<String> getAvailableSenderTypes() {
        return notificationSenders.stream()
                .map(NotificationSender::getSenderType)
                .collect(Collectors.toList());
    }




}