package com.core.reminder.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理表单验证错误
     * @param ex 验证异常
     * @return 包含验证错误信息的响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        try {
            Map<String, Object> response = new HashMap<>();
            Map<String, String> errors = new HashMap<>();
            
            ex.getBindingResult().getAllErrors().forEach((error) -> {
                String fieldName = "";
                if (error instanceof FieldError) {
                    fieldName = ((FieldError) error).getField();
                } else {
                    fieldName = error.getObjectName();
                }
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage != null ? errorMessage : "验证失败");
            });
            
            response.put("status", "error");
            response.put("message", "表单验证失败");
            response.put("errors", errors);
            
            // 如果只有一个错误，直接将错误信息作为主消息返回
            if (errors.size() == 1) {
                response.put("message", errors.values().iterator().next());
            }
            
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("处理表单验证错误时发生异常", e);
            return createSafeErrorResponse("表单验证失败", HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * 处理用户已存在异常
     * @param ex 用户已存在异常
     * @return 错误响应
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", ex.getMessage() != null ? ex.getMessage() : "用户已存在");
            
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            log.error("处理用户已存在异常时发生异常", e);
            return createSafeErrorResponse("用户已存在", HttpStatus.CONFLICT);
        }
    }
    
    /**
     * 处理其他所有未捕获的异常
     * @param ex 异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        try {
            log.error("全局异常处理器捕获到未处理的异常", ex);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "服务器内部错误");
            
            // 安全地处理异常消息
            String errorMessage = ex.getMessage();
            if (errorMessage != null && !errorMessage.trim().isEmpty()) {
                response.put("error", errorMessage);
            } else {
                response.put("error", "未知错误");
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            log.error("全局异常处理器自身发生异常", e);
            return createSafeErrorResponse("服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 创建安全的错误响应，避免进一步的异常
     */
    private ResponseEntity<Map<String, Object>> createSafeErrorResponse(String message, HttpStatus status) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", message != null ? message : "处理请求时发生错误");
            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            log.error("创建安全错误响应时发生异常", e);
            // 最后的兜底措施
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 