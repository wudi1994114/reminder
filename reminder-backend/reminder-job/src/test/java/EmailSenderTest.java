import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.task.reminder.ReminderJobApplication;
import com.task.reminder.sender.EmailSenderFactory;
import com.task.reminder.sender.TencentCloudEmailSender;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * 简单的邮件发送测试类
 * 用于测试腾讯云邮件发送功能
 */
@Slf4j
@SpringBootTest(classes = ReminderJobApplication.class)
@ActiveProfiles("local")  // 使用local配置文件
public class EmailSenderTest {
    
    @Resource
    private EmailSenderFactory emailSenderFactory;

    @Resource
    private TencentCloudEmailSender tencentCloudEmailSender;
    
    @Test
    public void testEmailSenderFactory() {
        System.out.println("=== 测试邮件发送器工厂 ===");
        
        if (emailSenderFactory != null) {
            System.out.println("✅ EmailSenderFactory 注入成功");
            System.out.println("可用发送器: " + emailSenderFactory.getAvailableSenderTypes());
            
            if (emailSenderFactory.getDefaultSender() != null) {
                System.out.println("默认发送器: " + emailSenderFactory.getDefaultSender().getSenderType());
            }
        } else {
            System.out.println("❌ EmailSenderFactory 注入失败");
        }
    }
    
    @Test
    public void testTencentCloudEmailSender() {
        System.out.println("=== 测试腾讯云邮件发送器 ===");
        
        if (tencentCloudEmailSender != null) {
            System.out.println("✅ TencentCloudEmailSender 注入成功");
            System.out.println("发送器类型: " + tencentCloudEmailSender.getSenderType());
        } else {
            System.out.println("❌ TencentCloudEmailSender 注入失败");
            System.out.println("请检查配置: email.provider=tencent");
        }
    }
    
    /**
     * 实际发送测试邮件（使用配置文件中的真实配置）
     * 取消注释@Test来运行真实的邮件发送测试
     */
    @Test
    public void testSendRealEmail() {
        System.out.println("=== 测试真实邮件发送 ===");
        
        if (tencentCloudEmailSender == null) {
            System.out.println("❌ TencentCloudEmailSender 未注入，请检查配置");
            return;
        }
        
        try {
            // 使用配置文件中的设置发送测试邮件
            String testEmail = "609679329@qq.com"; // 发送给你自己的Gmail
            String subject = "腾讯云邮件测试 - " + java.time.LocalDateTime.now();
            String content = "这是通过Spring Boot测试发送的腾讯云邮件\n\n" +
                    "发送时间: " + java.time.LocalDateTime.now() + "\n" +
                    "发送器: TencentCloudEmailSender\n" +
                    "配置来源: application-local.yaml\n\n" +
                    "如果您收到这封邮件，说明腾讯云邮件配置正确！";
            
            boolean success = tencentCloudEmailSender.sendEmail(testEmail, subject, content);
            
            if (success) {
                System.out.println("✅ 邮件发送成功！");
                System.out.println("请检查收件箱: " + testEmail);
            } else {
                System.out.println("❌ 邮件发送失败！");
            }
            
        } catch (Exception e) {
            System.out.println("❌ 邮件发送过程中出现错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试HTML邮件发送
     */
    @Test
    public void testSendHtmlEmail() {
        System.out.println("=== 测试HTML邮件发送 ===");
        
        if (tencentCloudEmailSender == null) {
            System.out.println("❌ TencentCloudEmailSender 未注入");
            return;
        }
        
        String testEmail = "609679329@qq.com";
        String subject = "备忘鸡测试";
        String htmlContent = createTestHtmlContent(subject, "备忘鸡测试内容", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        
        boolean success = tencentCloudEmailSender.sendHtmlEmail(testEmail, subject, htmlContent);
        
        if (success) {
            System.out.println("✅ HTML邮件发送成功！");
        } else {
            System.out.println("❌ HTML邮件发送失败！");
        }
    }
    
    private String createTestHtmlContent(String subject, String content, String time) {
         String detail = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'><title>%s</title></head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #1890ff;'>%s</h2>" +
                "<p><strong>时间:</strong> %s </p>" +
                "<div style='background-color: #f0f8ff; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<h3 style='color: #1890ff; margin-top: 0;'>%s</h3>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
        return String.format(detail, subject, subject, time, content);
    }
} 