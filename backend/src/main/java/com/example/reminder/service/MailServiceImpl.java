package com.example.reminder.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailServiceImpl implements MailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}") // 从配置文件读取发件人邮箱，如果未配置则为空字符串
    private String fromEmail;

    @Autowired
    public MailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendReminderEmail(String to, String subject, String body) {
        if (fromEmail == null || fromEmail.isEmpty()) {
            log.warn("Sender email address (spring.mail.username) is not configured. Skipping email sending.");
            return;
        }
        if (to == null || to.isEmpty()) {
             log.warn("Recipient email address is null or empty for subject: '{}'. Skipping email sending.", subject);
             return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Reminder email sent successfully to {}", to);
        } catch (MailException e) {
            log.error("Failed to send reminder email to {}: {}", to, e.getMessage());
            // Consider adding more robust error handling, e.g., retries or notifications
        }
    }
} 