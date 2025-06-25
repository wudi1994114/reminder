package com.core.reminder.utils;

import com.common.reminder.model.ComplexReminder;
import com.common.reminder.model.ReminderType;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 幂等性工具类
 * 用于生成和验证幂等键
 */
@Slf4j
public class IdempotencyUtils {

    /**
     * 生成随机幂等键
     * @return 随机UUID字符串
     */
    public static String generateRandomIdempotencyKey() {
        return UUID.randomUUID().toString();
    }

    /**
     * 根据复杂提醒的业务字段生成幂等键
     * 使用SHA-256哈希算法确保相同的业务数据生成相同的幂等键
     * 
     * @param fromUserId 创建用户ID
     * @param toUserId 接收用户ID
     * @param title 标题
     * @param cronExpression CRON表达式
     * @param reminderType 提醒类型
     * @param validFrom 生效开始日期
     * @param validUntil 生效结束日期
     * @param maxExecutions 最大执行次数
     * @return 基于业务字段的幂等键
     */
    public static String generateBusinessBasedIdempotencyKey(
            Long fromUserId,
            Long toUserId,
            String title,
            String cronExpression,
            ReminderType reminderType,
            LocalDate validFrom,
            LocalDate validUntil,
            Integer maxExecutions) {
        
        try {
            // 构建用于哈希的字符串
            StringBuilder sb = new StringBuilder();
            sb.append("fromUserId:").append(fromUserId != null ? fromUserId : "null").append("|");
            sb.append("toUserId:").append(toUserId != null ? toUserId : "null").append("|");
            sb.append("title:").append(title != null ? title.trim() : "null").append("|");
            sb.append("cronExpression:").append(cronExpression != null ? cronExpression.trim() : "null").append("|");
            sb.append("reminderType:").append(reminderType != null ? reminderType.name() : "null").append("|");
            sb.append("validFrom:").append(validFrom != null ? validFrom.toString() : "null").append("|");
            sb.append("validUntil:").append(validUntil != null ? validUntil.toString() : "null").append("|");
            sb.append("maxExecutions:").append(maxExecutions != null ? maxExecutions : "null");
            
            String input = sb.toString();
            log.debug("生成幂等键的输入字符串: {}", input);
            
            // 使用SHA-256生成哈希
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            String idempotencyKey = "business_" + hexString.toString();
            log.debug("生成的业务幂等键: {}", idempotencyKey);
            
            return idempotencyKey;
            
        } catch (NoSuchAlgorithmException e) {
            log.error("生成幂等键时发生错误，使用随机UUID作为备选方案", e);
            return "fallback_" + generateRandomIdempotencyKey();
        }
    }

    /**
     * 根据ComplexReminder对象生成业务幂等键
     * 
     * @param complexReminder 复杂提醒对象
     * @return 业务幂等键
     */
    public static String generateBusinessBasedIdempotencyKey(ComplexReminder complexReminder) {
        if (complexReminder == null) {
            throw new IllegalArgumentException("ComplexReminder对象不能为null");
        }
        
        return generateBusinessBasedIdempotencyKey(
                complexReminder.getFromUserId(),
                complexReminder.getToUserId(),
                complexReminder.getTitle(),
                complexReminder.getCronExpression(),
                complexReminder.getReminderType(),
                complexReminder.getValidFrom(),
                complexReminder.getValidUntil(),
                complexReminder.getMaxExecutions()
        );
    }

    /**
     * 验证幂等键格式是否有效
     * 
     * @param idempotencyKey 幂等键
     * @return 是否有效
     */
    public static boolean isValidIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return false;
        }
        
        // 检查长度（UUID长度36，业务哈希长度约72，加上前缀）
        if (idempotencyKey.length() < 10 || idempotencyKey.length() > 100) {
            return false;
        }
        
        // 检查是否包含非法字符（只允许字母、数字、下划线、连字符）
        return idempotencyKey.matches("^[a-zA-Z0-9_-]+$");
    }

    /**
     * 判断是否为业务生成的幂等键
     * 
     * @param idempotencyKey 幂等键
     * @return 是否为业务生成的幂等键
     */
    public static boolean isBusinessBasedKey(String idempotencyKey) {
        return idempotencyKey != null && idempotencyKey.startsWith("business_");
    }

    /**
     * 判断是否为随机生成的幂等键
     * 
     * @param idempotencyKey 幂等键
     * @return 是否为随机生成的幂等键
     */
    public static boolean isRandomKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return false;
        }
        
        // UUID格式：xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
        return idempotencyKey.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }
}
