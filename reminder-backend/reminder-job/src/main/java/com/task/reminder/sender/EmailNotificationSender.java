package com.task.reminder.sender;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 邮件通知发送器适配器
 * 将现有的EmailSender适配为NotificationSender接口
 */
@Component("emailNotificationSender")
public class EmailNotificationSender implements NotificationSender {

    @Autowired
    @Qualifier("tencentCloudEmailSender")
    private EmailSender emailSender;

    @Override
    public boolean sendNotification(String recipient, String title, String content, Object extraData) {
        try {
            if (!isValidRecipient(recipient)) {
                return false;
            }

            // 如果extraData是HTML内容，使用HTML邮件发送
            if (extraData instanceof String && ((String) extraData).contains("<html>")) {
                return emailSender.sendHtmlEmail(recipient, title, (String) extraData);
            } else {
                // 创建HTML格式的邮件内容
                String htmlContent = createHtmlContent(title, content);
                return emailSender.sendHtmlEmail(recipient, title, htmlContent);
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getSenderType() {
        return "EMAIL";
    }

    @Override
    public boolean isValidRecipient(String recipient) {
        // 简单的邮箱格式验证
        return recipient != null && recipient.contains("@") && recipient.contains(".");
    }

    /**
     * 创建HTML格式的邮件内容
     */
    private String createHtmlContent(String title, String content) {
        String template = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'><title>%s</title></head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #1890ff;'>%s</h2>" +
                "<div style='background-color: #f0f8ff; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<p>%s</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
        
        return String.format(template, title, title, content != null ? content : "");
    }
}