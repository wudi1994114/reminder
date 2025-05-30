package com.core.reminder.utils;

import com.core.reminder.constant.CacheKeyEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 缓存工具类
 * 统一管理缓存操作，使用CacheKeyEnum管理缓存键和过期时间
 */
@Component
public class CacheUtils {
    
    private static final Logger log = LoggerFactory.getLogger(CacheUtils.class);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private final ObjectMapper objectMapper;
    
    public CacheUtils() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * 设置缓存
     * @param cacheKeyEnum 缓存键枚举
     * @param keySuffix 缓存键后缀
     * @param value 缓存值
     */
    public void set(CacheKeyEnum cacheKeyEnum, String keySuffix, Object value) {
        String cacheKey = cacheKeyEnum.buildKey(keySuffix);
        
        try {
            if (cacheKeyEnum.isNeverExpire()) {
                // 永不过期
                redisTemplate.opsForValue().set(cacheKey, value);
                log.debug("已设置永久缓存: {}", cacheKey);
            } else {
                // 设置过期时间
                redisTemplate.opsForValue().set(cacheKey, value, cacheKeyEnum.getExpireTime(), cacheKeyEnum.getTimeUnit());
                log.debug("已设置缓存: {}，过期时间: {} {}", cacheKey, cacheKeyEnum.getExpireTime(), cacheKeyEnum.getTimeUnit());
            }
        } catch (Exception e) {
            log.error("设置缓存失败: {}", cacheKey, e);
        }
    }
    
    /**
     * 获取缓存
     * @param cacheKeyEnum 缓存键枚举
     * @param keySuffix 缓存键后缀
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(CacheKeyEnum cacheKeyEnum, String keySuffix) {
        String cacheKey = cacheKeyEnum.buildKey(keySuffix);
        
        try {
            Object value = redisTemplate.opsForValue().get(cacheKey);
            log.debug("获取缓存: {}，结果: {}", cacheKey, value != null ? "命中" : "未命中");
            return (T) value;
        } catch (Exception e) {
            log.error("获取缓存失败: {}", cacheKey, e);
            return null;
        }
    }
    
    /**
     * 删除缓存
     * @param cacheKeyEnum 缓存键枚举
     * @param keySuffix 缓存键后缀
     * @return 是否删除成功
     */
    public boolean delete(CacheKeyEnum cacheKeyEnum, String keySuffix) {
        String cacheKey = cacheKeyEnum.buildKey(keySuffix);
        
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            boolean success = Boolean.TRUE.equals(deleted);
            log.debug("删除缓存: {}，结果: {}", cacheKey, success ? "成功" : "失败");
            return success;
        } catch (Exception e) {
            log.error("删除缓存失败: {}", cacheKey, e);
            return false;
        }
    }
    
    /**
     * 检查缓存是否存在
     * @param cacheKeyEnum 缓存键枚举
     * @param keySuffix 缓存键后缀
     * @return 是否存在
     */
    public boolean exists(CacheKeyEnum cacheKeyEnum, String keySuffix) {
        String cacheKey = cacheKeyEnum.buildKey(keySuffix);
        
        try {
            Boolean exists = redisTemplate.hasKey(cacheKey);
            boolean result = Boolean.TRUE.equals(exists);
            log.debug("检查缓存存在性: {}，结果: {}", cacheKey, result);
            return result;
        } catch (Exception e) {
            log.error("检查缓存存在性失败: {}", cacheKey, e);
            return false;
        }
    }
    
    /**
     * 设置缓存过期时间
     * @param cacheKeyEnum 缓存键枚举
     * @param keySuffix 缓存键后缀
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 是否设置成功
     */
    public boolean expire(CacheKeyEnum cacheKeyEnum, String keySuffix, long timeout, TimeUnit unit) {
        String cacheKey = cacheKeyEnum.buildKey(keySuffix);
        
        try {
            Boolean result = redisTemplate.expire(cacheKey, timeout, unit);
            boolean success = Boolean.TRUE.equals(result);
            log.debug("设置缓存过期时间: {}，过期时间: {} {}，结果: {}", cacheKey, timeout, unit, success ? "成功" : "失败");
            return success;
        } catch (Exception e) {
            log.error("设置缓存过期时间失败: {}", cacheKey, e);
            return false;
        }
    }
    
    /**
     * 获取缓存剩余过期时间（秒）
     * @param cacheKeyEnum 缓存键枚举
     * @param keySuffix 缓存键后缀
     * @return 剩余过期时间（秒），-1表示永不过期，-2表示键不存在
     */
    public long getExpire(CacheKeyEnum cacheKeyEnum, String keySuffix) {
        String cacheKey = cacheKeyEnum.buildKey(keySuffix);
        
        try {
            Long expire = redisTemplate.getExpire(cacheKey);
            long result = expire != null ? expire : -2;
            log.debug("获取缓存剩余过期时间: {}，结果: {} 秒", cacheKey, result);
            return result;
        } catch (Exception e) {
            log.error("获取缓存剩余过期时间失败: {}", cacheKey, e);
            return -2;
        }
    }
    
    // === 便捷方法 ===
    
    /**
     * 设置用户信息缓存
     */
    public void setUserInfo(Long userId, Object value) {
        set(CacheKeyEnum.USER_INFO, String.valueOf(userId), value);
    }
    
    /**
     * 获取用户信息缓存
     */
    @SuppressWarnings("unchecked")
    public <T> T getUserInfo(Long userId) {
        return get(CacheKeyEnum.USER_INFO, String.valueOf(userId));
    }
    
    /**
     * 删除用户信息缓存
     */
    public boolean deleteUserInfo(Long userId) {
        return delete(CacheKeyEnum.USER_INFO, String.valueOf(userId));
    }

    // === ZSet 相关方法 ===
    
    /**
     * 添加元素到ZSet
     * @param cacheKeyEnum 缓存键枚举
     * @param keySuffix 缓存键后缀
     * @param value 值
     * @param score 分数（用于排序）
     */
    public void zAdd(CacheKeyEnum cacheKeyEnum, String keySuffix, Object value, double score) {
        String cacheKey = cacheKeyEnum.buildKey(keySuffix);
        
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForZSet().add(cacheKey, jsonValue, score);
            
            // 设置过期时间
            if (!cacheKeyEnum.isNeverExpire()) {
                redisTemplate.expire(cacheKey, cacheKeyEnum.getExpireTime(), cacheKeyEnum.getTimeUnit());
            }
            
            log.debug("已添加到ZSet: {}，score: {}", cacheKey, score);
        } catch (Exception e) {
            log.error("添加到ZSet失败: {}", cacheKey, e);
        }
    }
    
    /**
     * 批量添加元素到ZSet
     * @param cacheKeyEnum 缓存键枚举
     * @param keySuffix 缓存键后缀
     * @param tuples 值和分数的集合
     */
    @SuppressWarnings("unchecked")
    public void zAddBatch(CacheKeyEnum cacheKeyEnum, String keySuffix, Set<ZSetOperations.TypedTuple<String>> tuples) {
        String cacheKey = cacheKeyEnum.buildKey(keySuffix);
        
        try {
            if (!tuples.isEmpty()) {
                // 转换为Object类型的TypedTuple
                Set<ZSetOperations.TypedTuple<Object>> objectTuples = tuples.stream()
                    .map(tuple -> ZSetOperations.TypedTuple.of((Object) tuple.getValue(), tuple.getScore()))
                    .collect(Collectors.toSet());
                
                redisTemplate.opsForZSet().add(cacheKey, objectTuples);
                
                // 设置过期时间
                if (!cacheKeyEnum.isNeverExpire()) {
                    redisTemplate.expire(cacheKey, cacheKeyEnum.getExpireTime(), cacheKeyEnum.getTimeUnit());
                }
                
                log.debug("已批量添加到ZSet: {}，数量: {}", cacheKey, tuples.size());
            }
        } catch (Exception e) {
            log.error("批量添加到ZSet失败: {}", cacheKey, e);
        }
    }
    
    /**
     * 按分数范围查询ZSet
     * @param cacheKeyEnum 缓存键枚举
     * @param keySuffix 缓存键后缀
     * @param minScore 最小分数
     * @param maxScore 最大分数
     * @param clazz 返回对象类型
     * @return 查询结果列表
     */
    public <T> List<T> zRangeByScore(CacheKeyEnum cacheKeyEnum, String keySuffix, double minScore, double maxScore, Class<T> clazz) {
        String cacheKey = cacheKeyEnum.buildKey(keySuffix);
        
        try {
            Set<Object> results = redisTemplate.opsForZSet().rangeByScore(cacheKey, minScore, maxScore);
            
            if (results == null || results.isEmpty()) {
                log.debug("ZSet查询无结果: {}，范围: {} - {}", cacheKey, minScore, maxScore);
                return java.util.Collections.emptyList();
            }
            
            List<T> objects = results.stream()
                .map(jsonStr -> {
                    try {
                        return objectMapper.readValue(jsonStr.toString(), clazz);
                    } catch (JsonProcessingException e) {
                        log.error("反序列化对象失败: {}", jsonStr, e);
                        return null;
                    }
                })
                .filter(obj -> obj != null)
                .collect(Collectors.toList());
            
            log.debug("ZSet查询结果: {}，范围: {} - {}，数量: {}", cacheKey, minScore, maxScore, objects.size());
            return objects;
            
        } catch (Exception e) {
            log.error("ZSet查询失败: {}", cacheKey, e);
            return java.util.Collections.emptyList();
        }
    }
    
    /**
     * 按分数范围查询ZSet（带限制）
     * @param cacheKeyEnum 缓存键枚举
     * @param keySuffix 缓存键后缀
     * @param minScore 最小分数
     * @param maxScore 最大分数
     * @param offset 偏移量
     * @param count 数量限制
     * @param clazz 返回对象类型
     * @return 查询结果列表
     */
    public <T> List<T> zRangeByScore(CacheKeyEnum cacheKeyEnum, String keySuffix, double minScore, double maxScore, 
                                     long offset, long count, Class<T> clazz) {
        String cacheKey = cacheKeyEnum.buildKey(keySuffix);
        
        try {
            Set<Object> results = redisTemplate.opsForZSet().rangeByScore(cacheKey, minScore, maxScore, offset, count);
            
            if (results == null || results.isEmpty()) {
                log.debug("ZSet查询无结果: {}，范围: {} - {}，限制: {} - {}", cacheKey, minScore, maxScore, offset, count);
                return java.util.Collections.emptyList();
            }
            
            List<T> objects = results.stream()
                .map(jsonStr -> {
                    try {
                        return objectMapper.readValue(jsonStr.toString(), clazz);
                    } catch (JsonProcessingException e) {
                        log.error("反序列化对象失败: {}", jsonStr, e);
                        return null;
                    }
                })
                .filter(obj -> obj != null)
                .collect(Collectors.toList());
            
            log.debug("ZSet查询结果: {}，范围: {} - {}，限制: {} - {}，数量: {}", 
                     cacheKey, minScore, maxScore, offset, count, objects.size());
            return objects;
            
        } catch (Exception e) {
            log.error("ZSet查询失败: {}", cacheKey, e);
            return java.util.Collections.emptyList();
        }
    }
    
    /**
     * 删除ZSet中的元素
     * @param cacheKeyEnum 缓存键枚举
     * @param keySuffix 缓存键后缀
     * @param value 要删除的值
     */
    public void zRemove(CacheKeyEnum cacheKeyEnum, String keySuffix, Object value) {
        String cacheKey = cacheKeyEnum.buildKey(keySuffix);
        
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            Long removed = redisTemplate.opsForZSet().remove(cacheKey, jsonValue);
            log.debug("从ZSet删除元素: {}，删除数量: {}", cacheKey, removed);
        } catch (Exception e) {
            log.error("从ZSet删除元素失败: {}", cacheKey, e);
        }
    }
    
    /**
     * 清空ZSet
     * @param cacheKeyEnum 缓存键枚举
     * @param keySuffix 缓存键后缀
     */
    public void zClear(CacheKeyEnum cacheKeyEnum, String keySuffix) {
        String cacheKey = cacheKeyEnum.buildKey(keySuffix);
        
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            log.debug("清空ZSet: {}，结果: {}", cacheKey, Boolean.TRUE.equals(deleted) ? "成功" : "失败");
        } catch (Exception e) {
            log.error("清空ZSet失败: {}", cacheKey, e);
        }
    }
    
    // === 提醒缓存专用方法 ===
    
    /**
     * 添加提醒到用户的ZSet缓存
     * @param userId 用户ID
     * @param reminder 提醒对象
     * @param eventTime 事件时间（用作score）
     */
    public void addReminderToCache(Long userId, Object reminder, OffsetDateTime eventTime) {
        double score = eventTime.toEpochSecond();
        zAdd(CacheKeyEnum.USER_REMINDERS_ZSET, String.valueOf(userId), reminder, score);
    }
    
    /**
     * 批量添加提醒到用户的ZSet缓存
     * @param userId 用户ID
     * @param reminders 提醒列表（包含时间戳）
     */
    public void addRemindersToCache(Long userId, List<?> reminders) {
        try {
            Set<ZSetOperations.TypedTuple<String>> tuples = reminders.stream()
                .map(reminder -> {
                    try {
                        // 假设提醒对象有getEventTime方法
                        OffsetDateTime eventTime = (OffsetDateTime) reminder.getClass().getMethod("getEventTime").invoke(reminder);
                        double score = eventTime.toEpochSecond();
                        String jsonValue = objectMapper.writeValueAsString(reminder);
                        return ZSetOperations.TypedTuple.of(jsonValue, score);
                    } catch (Exception e) {
                        log.error("处理提醒对象失败", e);
                        return null;
                    }
                })
                .filter(tuple -> tuple != null)
                .collect(Collectors.toSet());
            
            if (!tuples.isEmpty()) {
                zAddBatch(CacheKeyEnum.USER_REMINDERS_ZSET, String.valueOf(userId), tuples);
            }
        } catch (Exception e) {
            log.error("批量添加提醒到缓存失败", e);
        }
    }
    
    /**
     * 获取用户指定时间范围内的提醒
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param clazz 提醒对象类型
     * @return 提醒列表
     */
    public <T> List<T> getUserRemindersByTimeRange(Long userId, OffsetDateTime startTime, OffsetDateTime endTime, Class<T> clazz) {
        double minScore = startTime.toEpochSecond();
        double maxScore = endTime.toEpochSecond();
        return zRangeByScore(CacheKeyEnum.USER_REMINDERS_ZSET, String.valueOf(userId), minScore, maxScore, clazz);
    }
    
    /**
     * 获取用户即将到来的提醒（未来指定天数内）
     * @param userId 用户ID
     * @param days 未来天数
     * @param limit 限制数量
     * @param clazz 提醒对象类型
     * @return 提醒列表
     */
    public <T> List<T> getUserUpcomingReminders(Long userId, int days, int limit, Class<T> clazz) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime futureTime = now.plusDays(days);
        double minScore = now.toEpochSecond();
        double maxScore = futureTime.toEpochSecond();
        return zRangeByScore(CacheKeyEnum.USER_REMINDERS_ZSET, String.valueOf(userId), minScore, maxScore, 0, limit, clazz);
    }
    
    /**
     * 获取用户指定月份的提醒
     * @param userId 用户ID
     * @param year 年份
     * @param month 月份
     * @param clazz 提醒对象类型
     * @return 提醒列表
     */
    public <T> List<T> getUserMonthlyReminders(Long userId, int year, int month, Class<T> clazz) {
        // 计算月份的开始和结束时间
        OffsetDateTime startOfMonth = OffsetDateTime.of(year, month, 1, 0, 0, 0, 0, OffsetDateTime.now().getOffset());
        OffsetDateTime endOfMonth = startOfMonth.plusMonths(1).minusNanos(1);
        
        return getUserRemindersByTimeRange(userId, startOfMonth, endOfMonth, clazz);
    }
    
    /**
     * 删除用户的提醒缓存
     * @param userId 用户ID
     * @param reminder 要删除的提醒对象
     */
    public void removeReminderFromCache(Long userId, Object reminder) {
        zRemove(CacheKeyEnum.USER_REMINDERS_ZSET, String.valueOf(userId), reminder);
    }
    
    /**
     * 清空用户的所有提醒缓存
     * @param userId 用户ID
     */
    public void clearUserRemindersCache(Long userId) {
        zClear(CacheKeyEnum.USER_REMINDERS_ZSET, String.valueOf(userId));
    }
    
    /**
     * 清理用户ZSet中过期的提醒（已经过去的事件）
     * @param userId 用户ID
     * @return 清理的数量
     */
    public long cleanupExpiredReminders(Long userId) {
        String cacheKey = CacheKeyEnum.USER_REMINDERS_ZSET.buildKey(String.valueOf(userId));
        
        try {
            // 计算当前时间戳（清理已经过去的事件）
            OffsetDateTime now = OffsetDateTime.now();
            double maxScore = now.toEpochSecond();
            
            // 删除已经过去的事件
            Long removed = redisTemplate.opsForZSet().removeRangeByScore(cacheKey, 0, maxScore);
            long cleanedCount = removed != null ? removed : 0;
            
            if (cleanedCount > 0) {
                log.debug("清理用户[{}]的 {} 条过期提醒（已过去的事件）", userId, cleanedCount);
            }
            
            return cleanedCount;
            
        } catch (Exception e) {
            log.error("清理用户[{}]过期提醒失败", userId, e);
            return 0;
        }
    }
    
    /**
     * 清理ZSet中指定时间之前的数据
     * @param cacheKeyEnum 缓存键枚举
     * @param keySuffix 缓存键后缀
     * @param beforeTime 指定时间
     * @return 清理的数量
     */
    public long zRemoveByTimeBefore(CacheKeyEnum cacheKeyEnum, String keySuffix, OffsetDateTime beforeTime) {
        String cacheKey = cacheKeyEnum.buildKey(keySuffix);
        
        try {
            double maxScore = beforeTime.toEpochSecond();
            Long removed = redisTemplate.opsForZSet().removeRangeByScore(cacheKey, 0, maxScore);
            long cleanedCount = removed != null ? removed : 0;
            
            log.debug("清理ZSet {} 中 {} 条过期数据", cacheKey, cleanedCount);
            return cleanedCount;
            
        } catch (Exception e) {
            log.error("清理ZSet {} 过期数据失败", cacheKey, e);
            return 0;
        }
    }
    
    /**
     * 获取ZSet的大小
     * @param cacheKeyEnum 缓存键枚举
     * @param keySuffix 缓存键后缀
     * @return ZSet大小
     */
    public long zSize(CacheKeyEnum cacheKeyEnum, String keySuffix) {
        String cacheKey = cacheKeyEnum.buildKey(keySuffix);
        
        try {
            Long size = redisTemplate.opsForZSet().zCard(cacheKey);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("获取ZSet {} 大小失败", cacheKey, e);
            return 0;
        }
    }
} 