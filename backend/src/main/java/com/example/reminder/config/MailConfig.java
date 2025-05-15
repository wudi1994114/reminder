package com.example.reminder.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${mail.host:smtp.gmail.com}")
    private String host;

    @Value("${mail.port:587}")
    private int port;

    @Value("${mail.username}")
    private String username;

    @Value("${mail.password}")
    private String password;

    @Value("${mail.properties.mail.smtp.auth:true}")
    private String auth;

    @Value("${mail.properties.mail.smtp.starttls.enable:true}")
    private String starttlsEnable;

    @Value("${mail.properties.mail.smtp.starttls.required:true}")
    private String starttlsRequired;

    @Value("${mail.properties.mail.transport.protocol:smtp}")
    private String protocol;

    @Value("${mail.properties.mail.debug:false}")
    private String debug;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        
        // 设置用户名密码
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        
        // 配置邮件属性
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.starttls.enable", starttlsEnable);
        props.put("mail.smtp.starttls.required", starttlsRequired);
        props.put("mail.transport.protocol", protocol);
        props.put("mail.debug", debug);
        
        return mailSender;
    }
} 