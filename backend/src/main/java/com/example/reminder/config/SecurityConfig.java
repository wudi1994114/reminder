package com.example.reminder.config;

import com.example.reminder.security.JwtAuthenticationFilter;
import com.example.reminder.security.UserDetailsServiceImpl;
import com.example.reminder.security.UserInfoFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity // Enables Spring Security's web security support
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private UserInfoFilter userInfoFilter;

    // Define the JWT Authentication Filter as a Bean
    // This allows Spring to manage its lifecycle and dependency injection
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    // Define the PasswordEncoder Bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Define the AuthenticationManager Bean
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
            http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
            .userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }
    
    // Define CORS configuration Bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow requests from typical frontend development server origin
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:8081", "http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        configuration.setAllowCredentials(true); // Important for cookies, authorization headers
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply CORS to all paths
        return source;
    }

    // Configure the main SecurityFilterChain
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS using the Bean defined above
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Disable CSRF protection as we are using stateless JWT
            .csrf(csrf -> csrf.disable())
            // Configure session management to be stateless
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                .antMatchers("/api/auth/**").permitAll() // Allow access to authentication endpoints
                .antMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll() // Allow Swagger UI if used
                .antMatchers("/actuator/**").permitAll() // Allow Actuator endpoints if used (consider securing them in production)
                .antMatchers("/api/users/me").authenticated() 
                .anyRequest().authenticated() // All other requests require authentication
            )
            // Add the JWT filter before the standard UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            // 添加用户信息过滤器，在JWT验证之后执行
            .addFilterAfter(userInfoFilter, JwtAuthenticationFilter.class);
            // Optional: Configure exception handling (e.g., for authentication errors)
            // .exceptionHandling(exceptions -> exceptions
            //     .authenticationEntryPoint(...) // Handle unauthenticated access
            //     .accessDeniedHandler(...)      // Handle forbidden access
            // );

        return http.build();
    }
} 