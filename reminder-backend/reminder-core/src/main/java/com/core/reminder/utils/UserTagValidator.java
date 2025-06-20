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
     * 最大标签数量
     */
    public static final int MAX_TAG_COUNT = 10;
    
    /**
     * 每个标签最大汉字数量
     */
    public static final int MAX_CHINESE_CHARS = 4;
    
    /**
     * 每个标签最大字母数量
     */
    public static final int MAX_ENGLISH_CHARS = 8;
    
    /**
     * 汉字正则表达式
     */
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\u4e00-\u9fa5]");
    
    /**
     * 英文字母正则表达式
     */
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]");
    
    /**
     * 验证标签列表字符串
     * @param tagListStr 逗号分隔的标签字符串
     * @return 验证结果
     */
    public static ValidationResult validateTagList(String tagListStr) {
        if (!StringUtils.hasText(tagListStr)) {
            return ValidationResult.success("标签列表为空，验证通过");
        }
        
        // 分割标签
        List<String> tags = Arrays.stream(tagListStr.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        
        // 检查标签数量
        if (tags.size() > MAX_TAG_COUNT) {
            return ValidationResult.error(String.format("标签数量超过限制，最多允许%d个标签，当前有%d个", 
                    MAX_TAG_COUNT, tags.size()));
        }
        
        // 检查每个标签的格式
        for (int i = 0; i < tags.size(); i++) {
            String tag = tags.get(i);
            ValidationResult tagResult = validateSingleTag(tag);
            if (!tagResult.isValid()) {
                return ValidationResult.error(String.format("第%d个标签验证失败：%s", i + 1, tagResult.getMessage()));
            }
        }
        
        return ValidationResult.success(String.format("标签列表验证通过，共%d个标签", tags.size()));
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
        
        // 统计汉字和英文字母数量
        int chineseCount = 0;
        int englishCount = 0;
        
        for (char c : tag.toCharArray()) {
            if (CHINESE_PATTERN.matcher(String.valueOf(c)).matches()) {
                chineseCount++;
            } else if (ENGLISH_PATTERN.matcher(String.valueOf(c)).matches()) {
                englishCount++;
            }
        }
        
        // 检查汉字数量
        if (chineseCount > MAX_CHINESE_CHARS) {
            return ValidationResult.error(String.format("标签'%s'汉字数量超过限制，最多允许%d个汉字，当前有%d个", 
                    tag, MAX_CHINESE_CHARS, chineseCount));
        }
        
        // 检查英文字母数量
        if (englishCount > MAX_ENGLISH_CHARS) {
            return ValidationResult.error(String.format("标签'%s'英文字母数量超过限制，最多允许%d个字母，当前有%d个", 
                    tag, MAX_ENGLISH_CHARS, englishCount));
        }
        
        // 检查是否只包含汉字和英文字母
        if (chineseCount == 0 && englishCount == 0) {
            return ValidationResult.error(String.format("标签'%s'必须包含汉字或英文字母", tag));
        }
        
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
        
        List<String> tags = Arrays.stream(tagListStr.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct() // 去重
                .collect(Collectors.toList());
        
        return String.join(",", tags);
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
