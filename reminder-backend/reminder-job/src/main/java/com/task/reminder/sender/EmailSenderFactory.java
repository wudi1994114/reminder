package com.task.reminder.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 邮件发送器工厂类
 * 根据配置选择合适的邮件发送器
 */
@Component
public class EmailSenderFactory {

    private static final Logger logger = LoggerFactory.getLogger(EmailSenderFactory.class);

    @Value("${email.provider:gmail}")
    private String emailProvider;

    @Autowired
    private List<EmailSender> emailSenders;

    private Map<String, EmailSender> senderMap;
    private EmailSender defaultSender;

    @PostConstruct
    public void init() {
        // 创建发送器映射
        senderMap = emailSenders.stream()
                .collect(Collectors.toMap(
                        sender -> sender.getSenderType().toLowerCase(),
                        Function.identity()
                ));

        // 设置默认发送器
        defaultSender = senderMap.get(emailProvider.toLowerCase());
        
        if (defaultSender == null) {
            logger.warn("未找到配置的邮件提供商: {}，可用的提供商: {}", 
                    emailProvider, senderMap.keySet());
            // 使用第一个可用的发送器作为默认值
            defaultSender = emailSenders.isEmpty() ? null : emailSenders.get(0);
        }

        if (defaultSender != null) {
            logger.info("邮件发送器初始化完成，当前使用: {} ({})", 
                    defaultSender.getSenderType(), emailProvider);
        } else {
            logger.error("没有可用的邮件发送器！");
        }
    }

    /**
     * 获取默认邮件发送器
     *
     * @return 默认邮件发送器
     */
    public EmailSender getDefaultSender() {
        return defaultSender;
    }

    /**
     * 根据类型获取邮件发送器
     *
     * @param senderType 发送器类型
     * @return 邮件发送器，如果不存在则返回null
     */
    public EmailSender getSender(String senderType) {
        return senderMap.get(senderType.toLowerCase());
    }

    /**
     * 获取所有可用的邮件发送器类型
     *
     * @return 发送器类型列表
     */
    public List<String> getAvailableSenderTypes() {
        return emailSenders.stream()
                .map(EmailSender::getSenderType)
                .collect(Collectors.toList());
    }

    /**
     * 发送邮件（使用默认发送器）
     *
     * @param to 收件人邮箱地址
     * @param subject 邮件主题
     * @param bodyText 邮件正文
     * @return 发送结果
     */
    public boolean sendEmail(String to, String subject, String bodyText) {
        if (defaultSender == null) {
            logger.error("没有可用的邮件发送器");
            return false;
        }
        return defaultSender.sendEmail(to, subject, bodyText);
    }

    /**
     * 发送HTML邮件（使用默认发送器）
     *
     * @param to 收件人邮箱地址
     * @param subject 邮件主题
     * @param htmlBody HTML格式的邮件正文
     * @return 发送结果
     */
    public boolean sendHtmlEmail(String to, String subject, String htmlBody) {
        if (defaultSender == null) {
            logger.error("没有可用的邮件发送器");
            return false;
        }
        return defaultSender.sendHtmlEmail(to, subject, htmlBody);
    }

    /**
     * 使用指定发送器发送邮件
     *
     * @param senderType 发送器类型
     * @param to 收件人邮箱地址
     * @param subject 邮件主题
     * @param bodyText 邮件正文
     * @return 发送结果
     */
    public boolean sendEmailWithSender(String senderType, String to, String subject, String bodyText) {
        EmailSender sender = getSender(senderType);
        if (sender == null) {
            logger.error("未找到邮件发送器: {}", senderType);
            return false;
        }
        return sender.sendEmail(to, subject, bodyText);
    }
} 