package com.example.reminder.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.nio.charset.StandardCharsets;

/**
 * 邮件模板配置类
 * 专门为邮件服务提供模板引擎
 */
@Configuration
public class MailTemplateConfig {

    /**
     * 创建一个邮件专用的模板解析器
     */
    @Bean
    public ITemplateResolver emailTemplateResolver() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(false);
        resolver.setCheckExistence(true);
        resolver.setName("emailTemplateResolver");
        return resolver;
    }

    /**
     * 创建一个邮件专用的模板引擎，并标记为主要Bean
     * 确保邮件服务可以正确注入
     */
    @Bean
    @Primary
    public TemplateEngine emailTemplateEngine() {
        final SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.addTemplateResolver(emailTemplateResolver());
        templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }
} 