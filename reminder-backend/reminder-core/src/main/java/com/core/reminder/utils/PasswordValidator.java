package com.core.reminder.utils;

import java.util.regex.Pattern;

/**
 * 密码验证工具类
 */
public class PasswordValidator {
    
    // 密码必须包含字母和数字，长度至少8位
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-zA-Z])(?=.*[0-9]).{8,}$");
    
    /**
     * 验证密码强度
     * @param password 密码
     * @return 是否符合要求
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }
    
    /**
     * 获取密码要求说明
     * @return 密码要求说明
     */
    public static String getPasswordRequirement() {
        return "密码必须包含字母和数字，且长度至少为8个字符";
    }
} 