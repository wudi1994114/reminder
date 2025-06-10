package com.core.reminder.config;

import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

/**
 * 时区配置类
 * 确保整个应用程序使用中国时区(Asia/Shanghai)
 */
@Configuration
public class TimeZoneConfig {

    @PostConstruct
    public void init() {
        // 设置JVM默认时区为中国时区
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        System.setProperty("user.timezone", "Asia/Shanghai");
        
        System.out.println("应用程序时区已设置为: " + TimeZone.getDefault().getID());
    }
} 