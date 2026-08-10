package com.core.reminder.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigurationTest {

    @Test
    void productionHttpsOriginIsAllowedByDefault() {
        CorsProperties properties = new CorsProperties();
        SecurityConfig securityConfig = new SecurityConfig();
        ReflectionTestUtils.setField(securityConfig, "corsProperties", properties);

        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();
        CorsConfiguration configuration = source.getCorsConfigurations().get("/**");

        assertTrue(configuration.getAllowedOrigins().contains("https://wwmty.cn"));
    }
}
