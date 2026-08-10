package com.core.reminder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private List<String> allowedOrigins = Arrays.asList(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "https://wwmty.cn",
            "https://www.wwmty.cn");
}
