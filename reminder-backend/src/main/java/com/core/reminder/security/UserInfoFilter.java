package com.core.reminder.security;

import com.common.reminder.dto.UserProfileDto;
import com.core.reminder.service.UserCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(2) // 确保在JWT验证之后执行
public class UserInfoFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(UserInfoFilter.class);

    @Autowired
    private UserCacheService userCacheService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            try {
                Object principal = authentication.getPrincipal();
                String username;
                
                if (principal instanceof UserDetails) {
                    username = ((UserDetails) principal).getUsername();
                } else if (principal instanceof String) {
                    username = (String) principal;
                } else {
                    log.warn("无法识别的Principal类型: {}", principal.getClass());
                    filterChain.doFilter(request, response);
                    return;
                }
                
                // 使用缓存服务获取用户信息
                UserProfileDto userProfileDto = userCacheService.getUserProfileByUsername(username);
                
                if (userProfileDto == null) {
                    log.warn("找不到用户: {}", username);
                } else {
                    // 设置到请求属性中，控制器可以通过@RequestAttribute("currentUser")获取
                    request.setAttribute("currentUser", userProfileDto);
                    log.debug("已设置用户信息到请求属性中: {}", userProfileDto);
                }
            } catch (Exception e) {
                log.error("处理用户信息时出错", e);
            }
        }
        
        filterChain.doFilter(request, response);
    }
} 