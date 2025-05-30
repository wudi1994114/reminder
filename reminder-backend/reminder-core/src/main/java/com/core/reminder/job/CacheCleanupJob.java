package com.core.reminder.job;

import com.core.reminder.constant.CacheKeyEnum;
import com.core.reminder.utils.CacheUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * 缓存清理定时任务
 * 定期清理Redis中过期的提醒数据
 */
@Component
public class CacheCleanupJob {
    
    private static final Logger log = LoggerFactory.getLogger(CacheCleanupJob.class);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private CacheUtils cacheUtils;
    
    /**
     * 清理过期的提醒缓存（已经过去的事件）
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredReminders() {
        log.info("开始清理过期的提醒缓存（已过去的事件）...");
        
        try {
            long startTime = System.currentTimeMillis();
            int cleanedCount = 0;
            
            // 计算当前时间戳作为清理阈值（清理过去的事件）
            OffsetDateTime now = OffsetDateTime.now();
            double maxScore = now.toEpochSecond();
            
            // 获取所有用户提醒缓存的键
            String pattern = CacheKeyEnum.USER_REMINDERS_ZSET.getKeyPrefix() + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    try {
                        // 删除已经过去的提醒数据（score < 当前时间戳）
                        Long removed = redisTemplate.opsForZSet().removeRangeByScore(key, 0, maxScore);
                        if (removed != null && removed > 0) {
                            cleanedCount += removed.intValue();
                            log.debug("清理缓存键 {} 中的 {} 条过期提醒", key, removed);
                        }
                        
                        // 检查ZSet是否为空，如果为空则删除整个键
                        Long size = redisTemplate.opsForZSet().zCard(key);
                        if (size != null && size == 0) {
                            redisTemplate.delete(key);
                            log.debug("删除空的缓存键: {}", key);
                        }
                        
                    } catch (Exception e) {
                        log.error("清理缓存键 {} 时出错", key, e);
                    }
                }
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            log.info("过期提醒缓存清理完成，共处理 {} 个缓存键，清理 {} 条过期提醒，耗时 {}ms", 
                    keys != null ? keys.size() : 0, cleanedCount, duration);
            
        } catch (Exception e) {
            log.error("清理过期提醒缓存时发生错误", e);
        }
    }
    
    /**
     * 清理空的缓存键
     * 每周日凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void cleanupEmptyCacheKeys() {
        log.info("开始清理空的缓存键...");
        
        try {
            long startTime = System.currentTimeMillis();
            int deletedCount = 0;
            
            // 获取所有用户提醒缓存的键
            String pattern = CacheKeyEnum.USER_REMINDERS_ZSET.getKeyPrefix() + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    try {
                        // 检查ZSet大小
                        Long size = redisTemplate.opsForZSet().zCard(key);
                        if (size != null && size == 0) {
                            Boolean deleted = redisTemplate.delete(key);
                            if (Boolean.TRUE.equals(deleted)) {
                                deletedCount++;
                                log.debug("删除空的缓存键: {}", key);
                            }
                        }
                    } catch (Exception e) {
                        log.error("检查缓存键 {} 时出错", key, e);
                    }
                }
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            log.info("空缓存键清理完成，共检查 {} 个缓存键，删除 {} 个空键，耗时 {}ms", 
                    keys != null ? keys.size() : 0, deletedCount, duration);
            
        } catch (Exception e) {
            log.error("清理空缓存键时发生错误", e);
        }
    }
    
    /**
     * 缓存统计信息
     * 每天上午9点执行
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void cacheStatistics() {
        log.info("开始统计缓存信息...");
        
        try {
            // 获取所有用户提醒缓存的键
            String pattern = CacheKeyEnum.USER_REMINDERS_ZSET.getKeyPrefix() + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                int totalKeys = keys.size();
                long totalReminders = 0;
                int emptyKeys = 0;
                
                for (String key : keys) {
                    try {
                        Long size = redisTemplate.opsForZSet().zCard(key);
                        if (size != null) {
                            if (size == 0) {
                                emptyKeys++;
                            } else {
                                totalReminders += size;
                            }
                        }
                    } catch (Exception e) {
                        log.error("统计缓存键 {} 时出错", key, e);
                    }
                }
                
                log.info("缓存统计信息 - 总缓存键数: {}，总提醒数: {}，空键数: {}，平均每个用户提醒数: {}", 
                        totalKeys, totalReminders, emptyKeys, 
                        totalKeys > 0 ? String.format("%.2f", (double) totalReminders / totalKeys) : "0");
            } else {
                log.info("缓存统计信息 - 当前没有提醒缓存数据");
            }
            
        } catch (Exception e) {
            log.error("统计缓存信息时发生错误", e);
        }
    }
} 