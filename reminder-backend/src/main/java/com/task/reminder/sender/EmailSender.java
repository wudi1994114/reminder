package com.task.reminder.sender;

/**
 * 邮件发送器接口
 * 定义统一的邮件发送方法，支持多种邮件服务提供商
 */
public interface EmailSender {
    
    /**
     * 发送邮件
     *
     * @param to 收件人邮箱地址
     * @param subject 邮件主题
     * @param bodyText 邮件正文
     * @return 发送结果，成功返回true，失败返回false
     */
    boolean sendEmail(String to, String subject, String bodyText);
    
    /**
     * 发送HTML格式邮件
     *
     * @param to 收件人邮箱地址
     * @param subject 邮件主题
     * @param htmlBody HTML格式的邮件正文
     * @return 发送结果，成功返回true，失败返回false
     */
    boolean sendHtmlEmail(String to, String subject, String htmlBody);
    
    /**
     * 获取发送器类型名称
     *
     * @return 发送器类型名称
     */
    String getSenderType();
} 