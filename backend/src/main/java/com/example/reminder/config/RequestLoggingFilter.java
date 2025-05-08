package com.example.reminder.config;

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
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;

@Component // 将过滤器注册为 Spring Bean，使其自动应用
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 使用 Wrapper 包装 request 和 response 以便多次读取内容 (例如读取请求体)
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        // 继续处理请求
        filterChain.doFilter(wrappedRequest, wrappedResponse);

        long duration = System.currentTimeMillis() - startTime;

        // --- 开始记录日志 ---
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("HTTP Request: [")
                  .append(wrappedRequest.getMethod()).append("] ")
                  .append(wrappedRequest.getRequestURI());

        String queryString = wrappedRequest.getQueryString();
        if (queryString != null) {
            logMessage.append("?").append(queryString);
        }

        logMessage.append("; Status: ").append(wrappedResponse.getStatus());
        logMessage.append("; Duration: ").append(duration).append("ms");
        
        // --- (可选) 记录请求头 ---
        // Enumeration<String> headerNames = wrappedRequest.getHeaderNames();
        // logMessage.append("; Headers: [");
        // while (headerNames.hasMoreElements()) {
        //     String headerName = headerNames.nextElement();
        //     // 过滤敏感头部，例如 Authorization
        //     if (!headerName.equalsIgnoreCase("Authorization")) { 
        //          logMessage.append(headerName).append("=").append(wrappedRequest.getHeader(headerName)).append(", ");
        //     }
        // }
        // if (logMessage.toString().endsWith(", ")) { // Remove trailing comma and space
        //     logMessage.setLength(logMessage.length() - 2);
        // }
        // logMessage.append("]");
        
        // --- (可选) 记录请求体 (注意：可能包含敏感信息，并且对性能有影响) ---
        // String requestBody = getRequestBody(wrappedRequest);
        // if (!requestBody.isEmpty()) {
        //     // 避免记录过大的请求体或二进制内容
        //     if (requestBody.length() < 1024 && isReadable(requestBody)) { 
        //         logMessage.append("; RequestBody: ").append(requestBody);
        //     } else {
        //         logMessage.append("; RequestBody: [hidden or too large]");
        //     }
        // }

        // --- (可选) 记录响应体 (同样注意敏感信息和性能) ---
        // String responseBody = getResponseBody(wrappedResponse);
        // if (!responseBody.isEmpty()) {
        //      if (responseBody.length() < 1024 && isReadable(responseBody)) {
        //         logMessage.append("; ResponseBody: ").append(responseBody);
        //      } else {
        //          logMessage.append("; ResponseBody: [hidden or too large]");
        //      }
        // }

        logger.info(logMessage.toString());
        
        // --- 重要：将响应内容复制回原始响应 --- 
        // 必须在日志记录之后调用，否则客户端可能收不到响应体
        wrappedResponse.copyBodyToResponse(); 
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        if (buf.length > 0) {
            try {
                return new String(buf, 0, buf.length, request.getCharacterEncoding());
            } catch (UnsupportedEncodingException ex) {
                logger.error("Failed to parse request body due to encoding issue", ex);
                return "[Unsupported Encoding]";
            }
        }
        return "";
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] buf = response.getContentAsByteArray();
        if (buf.length > 0) {
            try {
                return new String(buf, 0, buf.length, response.getCharacterEncoding());
            } catch (UnsupportedEncodingException ex) {
                 logger.error("Failed to parse response body due to encoding issue", ex);
                 return "[Unsupported Encoding]";
            }
        }
        return "";
    }

    // Helper to check if content is likely readable text (simple check)
    private boolean isReadable(String content) {
        // Avoid logging binary data or excessively long strings
        return content != null && !content.contains("\uFFFD"); // Check for replacement character (often indicates binary)
    }
} 