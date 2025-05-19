package com.example.reminder.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.nio.charset.StandardCharsets;

/**
 * Thymeleaf配置类
 * 用于配置Thymeleaf模板引擎，主要用于邮件模板渲染
 */
@Configuration
public class ThymeleafConfig {

    /**
     * 配置Thymeleaf的模板解析器
     * 设置模板位置、模式和编码方式
     */
    @Bean
    @Description("Thymeleaf Template Resolver")
    public ITemplateResolver templateResolver() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // 开发环境可以设置为false以便实时看到更改，生产环境设置为true提高性能
        templateResolver.setCacheable(false);
        return templateResolver;
    }

    /**
     * 配置Thymeleaf模板引擎
     * 使用上面定义的模板解析器
     */
    @Bean
    @Description("Thymeleaf Template Engine")
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }
} 