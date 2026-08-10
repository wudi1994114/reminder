package com.core.reminder.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // 根据你的API路径前缀调整
                .allowedOrigins(
                        "http://localhost:5173",      // 本地Vite开发服务器
                        "http://127.0.0.1:5173",    // 本地Vite开发服务器 (备选)
                        "http://wwmty.cn:5173",       // 通过 wwmty.cn 访问 Vite 开发服务器
                        "http://wwmty.cn",            // 生产环境前端 (标准HTTP/HTTPS端口)
                        "http://123.57.175.66",       // 指定的IP地址 (可能用于前端，标准端口)
                        "http://123.57.175.66:5173"   // 指定的IP地址 (可能用于前端Vite开发服务器)
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // pre-flight请求的缓存时间 (秒)
    }
} 