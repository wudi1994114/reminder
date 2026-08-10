package com.core.reminder.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户标签验证工具类
 * 用于验证用户标签管理功能中的标签格式和数量
 */
@Slf4j
public class UserTagValidator {
    
    /**
     * 标签列表最大总长度
     */
    public static final int MAX_TOTAL_LENGTH = 100;
    
    /**
     * 标签分隔符
     */
    public static final String TAG_SEPARATOR = "\\|-\\|";
    
    /**
     * 标题和内容分隔符
     */
    public static final String TITLE_CONTENT_SEPARATOR = "\\|";
    
    /**
     * 验证标签列表字符串
     * @param tagListStr 逗号分隔的标签字符串
     * @return 验证结果
     */
    public static ValidationResult validateTagList(String tagListStr) {
        if (!StringUtils.hasText(tagListStr)) {
            return ValidationResult.success("标签列表为空，验证通过");
        }
        
        // 检查总长度
        if (tagListStr.length() > MAX_TOTAL_LENGTH) {
            return ValidationResult.error(String.format("标签列表总长度超过限制，最多允许%d个字符，当前有%d个", 
                    MAX_TOTAL_LENGTH, tagListStr.length()));
        }
        
        // 分割标签
        List<String> tags = Arrays.stream(tagListStr.split(TAG_SEPARATOR))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        
        // 检查每个标签的格式
        for (int i = 0; i < tags.size(); i++) {
            String tag = tags.get(i);
            ValidationResult tagResult = validateSingleTag(tag);
            if (!tagResult.isValid()) {
                return ValidationResult.error(String.format("第%d个标签验证失败：%s", i + 1, tagResult.getMessage()));
            }
        }
        
        return ValidationResult.success(String.format("标签列表验证通过，共%d个标签，总长度%d个字符", tags.size(), tagListStr.length()));
    }
    
    /**
     * 验证单个标签
     * @param tag 标签内容
     * @return 验证结果
     */
    public static ValidationResult validateSingleTag(String tag) {
        if (!StringUtils.hasText(tag)) {
            return ValidationResult.error("标签不能为空");
        }
        
        // 检查标签是否包含标签分隔符（不允许在单个标签中使用）
        if (tag.contains(TAG_SEPARATOR)) {
            return ValidationResult.error(String.format("标签'%s'不能包含标签分隔符'%s'", tag, TAG_SEPARATOR));
        }
        
        // 检查标签是否包含分隔符（标题|内容格式）
        if (tag.contains(TITLE_CONTENT_SEPARATOR)) {
            String[] parts = tag.split(TITLE_CONTENT_SEPARATOR, 2);
            if (parts.length == 2) {
                String title = parts[0].trim();
                String content = parts[1].trim();
                
                if (!StringUtils.hasText(title)) {
                    return ValidationResult.error(String.format("标签'%s'的标题部分不能为空", tag));
                }
                
                if (!StringUtils.hasText(content)) {
                    return ValidationResult.error(String.format("标签'%s'的内容部分不能为空", tag));
                }
                
                return ValidationResult.success(String.format("标签'%s'验证通过（标题|内容格式）", tag));
            } else {
                return ValidationResult.error(String.format("标签'%s'格式错误，|分隔符使用不正确", tag));
            }
        }
        
        // 普通标签（没有|分隔符）
        return ValidationResult.success(String.format("标签'%s'验证通过", tag));
    }
    
    /**
     * 格式化标签列表字符串
     * 去除空白标签，去除重复标签
     * @param tagListStr 原始标签字符串
     * @return 格式化后的标签字符串
     */
    public static String formatTagList(String tagListStr) {
        if (!StringUtils.hasText(tagListStr)) {
            return "";
        }
        
        List<String> tags = Arrays.stream(tagListStr.split(TAG_SEPARATOR))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct() // 去重
                .collect(Collectors.toList());
        
        return String.join(TAG_SEPARATOR, tags);
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public static ValidationResult success(String message) {
            return new ValidationResult(true, message);
        }
        
        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
        
        @Override
        public String toString() {
            return String.format("ValidationResult{valid=%s, message='%s'}", valid, message);
        }
    }
}
