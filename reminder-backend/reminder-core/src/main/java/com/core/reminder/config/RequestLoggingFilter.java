package com.core.reminder.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            // 继续处理请求
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            try {
                // 确保响应已提交后再进行日志记录
                if (!response.isCommitted()) {
                    // 简化的日志记录
                    StringBuilder logMessage = new StringBuilder();
                    
                    // 请求方法和URL
                    logMessage.append("接口：[")
                              .append(wrappedRequest.getMethod()).append("] ")
                              .append(wrappedRequest.getRequestURI());
                    
                    String queryString = wrappedRequest.getQueryString();
                    if (queryString != null) {
                        logMessage.append("?").append(queryString);
                    }
                    
                    // 请求体
                    String requestBody = getRequestBody(wrappedRequest);
                    if (!requestBody.isEmpty() && isReadable(requestBody)) { 
                        logMessage.append("\n入参：").append(requestBody);
                    }

                    // 响应体
                    String responseBody = getResponseBody(wrappedResponse);
                    if (!responseBody.isEmpty() && isReadable(responseBody)) {
                        logMessage.append("\n返回：").append(responseBody);
                    }

                    logger.info(logMessage.toString());
                }
            } catch (Exception ex) {
                // 日志记录失败不应该影响正常响应
                logger.warn("记录请求日志时发生异常", ex);
            }
            
            try {
                // 将响应内容复制回原始响应
                wrappedResponse.copyBodyToResponse();
            } catch (Exception ex) {
                // 如果复制响应失败，记录错误但不抛出异常
                logger.error("复制响应体时发生异常", ex);
            }
        }
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        if (buf.length > 0) {
            try {
                // 固定使用UTF-8编码，解决中文乱码问题
                return new String(buf, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                logger.error("解析请求体失败", ex);
                return "";
            }
        }
        return "";
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] buf = response.getContentAsByteArray();
        if (buf.length > 0) {
            try {
                // 固定使用UTF-8编码，解决中文乱码问题
                return new String(buf, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                logger.error("解析响应体失败", ex);
                return "";
            }
        }
        return "";
    }

    private boolean isReadable(String content) {
        return content != null && !content.contains("\uFFFD");
    }
} 