package com.task.reminder.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;

/**
 * 腾讯云邮件发送器
 * 使用腾讯云SES（Simple Email Service）发送邮件
 */
@Service
@ConditionalOnProperty(name = "email.provider", havingValue = "tencent", matchIfMissing = false)
public class TencentCloudEmailSender implements EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(TencentCloudEmailSender.class);

    // 腾讯云SES SMTP服务器配置
    private static final String SMTP_HOST = "smtp.exmail.qq.com";  // 腾讯企业邮箱
    private static final String SMTP_PORT = "587";
    private static final String SMTP_SSL_PORT = "465";

    @Value("${tencent.email.username}")
    private String username;

    @Value("${tencent.email.password}")
    private String password;

    @Value("${tencent.email.username}")  // 使用username作为发件人邮箱
    private String fromEmail;

    @Value("${tencent.email.fromName:提醒助手}")
    private String fromName;

    @Value("${tencent.email.sslEnabled:true}")
    private boolean sslEnabled;

    private Session session;

    @PostConstruct
    public void initSession() {
        try {
            Properties props = new Properties();
            
            if (sslEnabled) {
                // 使用SSL连接
                props.put("mail.smtp.host", SMTP_HOST);
                props.put("mail.smtp.port", SMTP_SSL_PORT);
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            } else {
                // 使用STARTTLS连接
                props.put("mail.smtp.host", SMTP_HOST);
                props.put("mail.smtp.port", SMTP_PORT);
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "true");
            }

            // 创建认证器
            Authenticator authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };

            this.session = Session.getInstance(props, authenticator);
            
            logger.info("腾讯云邮件服务初始化成功，发送邮箱: {}", fromEmail);
        } catch (Exception e) {
            logger.error("腾讯云邮件服务初始化失败", e);
            throw new RuntimeException("腾讯云邮件服务初始化失败", e);
        }
    }

    @Override
    public boolean sendEmail(String to, String subject, String bodyText) {
        // 将纯文本转换为HTML格式
        String htmlBody = "<div style='font-family: Arial, sans-serif; line-height: 1.6;'>" +
                "<h3 style='color: #333;'>" + subject + "</h3>" +
                "<p>" + bodyText.replace("\n", "<br/>") + "</p>" +
                "</div>";
        return sendHtmlEmail(to, subject, htmlBody);
    }

    @Override
    public boolean sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = new MimeMessage(session);
            
            // 设置发件人
            message.setFrom(new InternetAddress(fromEmail, fromName, "UTF-8"));
            
            // 设置收件人
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            
            // 设置邮件主题
            message.setSubject(subject, "UTF-8");
            
            // 设置邮件内容
            message.setContent(htmlBody, "text/html; charset=UTF-8");
            
            // 发送邮件
            Transport.send(message);
            
            logger.info("邮件发送成功 - 收件人: {}, 主题: {}", to, subject);
            return true;
            
        } catch (Exception e) {
            logger.error("邮件发送失败 - 收件人: {}, 主题: {}, 错误: {}", to, subject, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getSenderType() {
        return "TencentCloud";
    }

    /**
     * 发送带附件的邮件（扩展功能）
     *
     * @param to 收件人邮箱地址
     * @param subject 邮件主题
     * @param htmlBody HTML格式的邮件正文
     * @param attachmentPath 附件路径
     * @param attachmentName 附件名称
     * @return 发送结果
     */
    public boolean sendEmailWithAttachment(String to, String subject, String htmlBody, 
                                         String attachmentPath, String attachmentName) {
        try {
            MimeMessage message = new MimeMessage(session);
            
            // 设置发件人
            message.setFrom(new InternetAddress(fromEmail, fromName, "UTF-8"));
            
            // 设置收件人
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            
            // 设置邮件主题
            message.setSubject(subject, "UTF-8");
            
            // 创建多部分消息
            Multipart multipart = new MimeMultipart();
            
            // 添加文本部分
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(htmlBody, "text/html; charset=UTF-8");
            multipart.addBodyPart(messageBodyPart);
            
            // 添加附件部分
            if (attachmentPath != null && !attachmentPath.isEmpty()) {
                messageBodyPart = new MimeBodyPart();
                javax.activation.DataSource source = new javax.activation.FileDataSource(attachmentPath);
                messageBodyPart.setDataHandler(new javax.activation.DataHandler(source));
                messageBodyPart.setFileName(attachmentName != null ? attachmentName : "attachment");
                multipart.addBodyPart(messageBodyPart);
            }
            
            // 设置邮件内容
            message.setContent(multipart);
            
            // 发送邮件
            Transport.send(message);
            
            logger.info("带附件邮件发送成功 - 收件人: {}, 主题: {}, 附件: {}", to, subject, attachmentName);
            return true;
            
        } catch (Exception e) {
            logger.error("带附件邮件发送失败 - 收件人: {}, 主题: {}, 错误: {}", to, subject, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 批量发送邮件
     *
     * @param recipients 收件人列表
     * @param subject 邮件主题
     * @param htmlBody HTML格式的邮件正文
     * @return 发送成功的数量
     */
    public int sendBatchEmails(String[] recipients, String subject, String htmlBody) {
        int successCount = 0;
        
        for (String recipient : recipients) {
            if (sendHtmlEmail(recipient, subject, htmlBody)) {
                successCount++;
            }
            
            // 添加延迟以避免发送过快
            try {
                Thread.sleep(100); // 100ms延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("批量发送邮件时被中断");
                break;
            }
        }
        
        logger.info("批量邮件发送完成 - 总数: {}, 成功: {}", recipients.length, successCount);
        return successCount;
    }

    /**
     * 测试方法 - 可以直接运行测试腾讯云邮件发送功能
     * 使用前请先配置好相关参数
     */
    public static void main(String[] args) {
        System.out.println("=== 腾讯云邮件发送器测试 ===");
        
        // 创建测试实例
        TencentCloudEmailSender sender = new TencentCloudEmailSender();
        
        // 手动设置测试参数 - 请替换为你的实际配置
        sender.username = "your-smtp-username@your-domain.com";  // 替换为你的SMTP用户名
        sender.password = "your-smtp-password";                   // 替换为你的SMTP密码
        sender.fromEmail = "your-from-email@your-domain.com";     // 替换为你的发件人邮箱
        sender.fromName = "提醒助手测试";
        sender.sslEnabled = true;
        
        try {
            // 初始化邮件会话
            sender.initSession();
            System.out.println("✅ 邮件服务初始化成功");
            
            // 测试收件人邮箱 - 请替换为你的测试邮箱
            String testEmail = "test-recipient@example.com";  // 替换为你的测试邮箱
            
            System.out.println("📧 开始发送测试邮件到: " + testEmail);
            
            // 测试1: 发送纯文本邮件
            System.out.println("\n--- 测试1: 纯文本邮件 ---");
            boolean result1 = sender.sendEmail(
                testEmail,
                "腾讯云邮件测试 - 纯文本",
                "这是一封测试邮件。\n\n发送时间: " + java.time.LocalDateTime.now() + "\n测试内容: 纯文本格式邮件发送测试"
            );
            System.out.println("纯文本邮件发送结果: " + (result1 ? "✅ 成功" : "❌ 失败"));
            
            // 等待一秒
            Thread.sleep(1000);
            
            // 测试2: 发送HTML邮件
            System.out.println("\n--- 测试2: HTML邮件 ---");
            String htmlContent = createTestHtmlContent();
            boolean result2 = sender.sendHtmlEmail(
                testEmail,
                "腾讯云邮件测试 - HTML格式",
                htmlContent
            );
            System.out.println("HTML邮件发送结果: " + (result2 ? "✅ 成功" : "❌ 失败"));
            
            // 等待一秒
            Thread.sleep(1000);
            
            // 测试3: 批量发送测试（如果有多个测试邮箱）
            System.out.println("\n--- 测试3: 批量发送 ---");
            String[] recipients = {testEmail}; // 可以添加更多测试邮箱
            int successCount = sender.sendBatchEmails(
                recipients,
                "腾讯云邮件测试 - 批量发送",
                "<h2>批量发送测试</h2><p>这是批量发送的测试邮件</p><p>发送时间: " + 
                java.time.LocalDateTime.now() + "</p>"
            );
            System.out.println("批量发送结果: " + successCount + "/" + recipients.length + " 成功");
            
            // 总结
            System.out.println("\n=== 测试完成 ===");
            System.out.println("总体结果: " + 
                (result1 && result2 && successCount > 0 ? "✅ 所有测试通过" : "⚠️ 部分测试失败"));
            
        } catch (Exception e) {
            System.err.println("❌ 测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            System.out.println("\n💡 常见问题排查:");
            System.out.println("1. 检查SMTP用户名和密码是否正确");
            System.out.println("2. 检查发件人邮箱是否已在腾讯云SES中验证");
            System.out.println("3. 检查网络连接是否正常");
            System.out.println("4. 检查腾讯云SES服务是否已开通");
        }
    }
    
    /**
     * 创建测试用的HTML邮件内容
     */
    private static String createTestHtmlContent() {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<title>腾讯云邮件测试</title>" +
                "</head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<div style='border: 2px solid #1890ff; border-radius: 8px; padding: 20px;'>" +
                "<h1 style='color: #1890ff; text-align: center; margin-bottom: 30px;'>🚀 腾讯云邮件服务测试</h1>" +
                
                "<div style='background-color: #f0f8ff; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<h3 style='color: #1890ff; margin-top: 0;'>📋 测试信息</h3>" +
                "<p><strong>发送时间:</strong> " + java.time.LocalDateTime.now() + "</p>" +
                "<p><strong>发送器:</strong> 腾讯云SES (Simple Email Service)</p>" +
                "<p><strong>邮件格式:</strong> HTML</p>" +
                "<p><strong>字符编码:</strong> UTF-8</p>" +
                "</div>" +
                
                "<div style='background-color: #f6ffed; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #52c41a;'>" +
                "<h3 style='color: #52c41a; margin-top: 0;'>✅ 功能验证项目</h3>" +
                "<ul style='margin: 10px 0;'>" +
                "<li>✓ SMTP连接建立</li>" +
                "<li>✓ 身份认证通过</li>" +
                "<li>✓ HTML内容渲染</li>" +
                "<li>✓ 中文字符显示</li>" +
                "<li>✓ CSS样式应用</li>" +
                "<li>✓ 邮件成功投递</li>" +
                "</ul>" +
                "</div>" +
                
                "<div style='background-color: #fff7e6; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #fa8c16;'>" +
                "<h3 style='color: #fa8c16; margin-top: 0;'>📊 性能指标</h3>" +
                "<p>如果您收到这封邮件，说明以下功能正常:</p>" +
                "<ul>" +
                "<li>腾讯云SES服务配置正确</li>" +
                "<li>SMTP认证信息有效</li>" +
                "<li>发件人域名验证通过</li>" +
                "<li>邮件内容格式正确</li>" +
                "</ul>" +
                "</div>" +
                
                "<div style='text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #d9d9d9;'>" +
                "<p style='color: #666; font-size: 14px;'>此邮件由提醒事项管理系统自动发送</p>" +
                "<p style='color: #666; font-size: 12px;'>测试时间: " + java.time.LocalDateTime.now() + "</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
} 