package com.core.reminder.config;

import com.core.reminder.service.LegalHolidayService;
import com.core.reminder.repository.SimpleReminderRepository;
import com.core.reminder.utils.CacheUtils;
import com.common.reminder.model.SimpleReminder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 应用初始化配置类
 * 在应用启动时执行初始化操作
 */
@Component
public class ApplicationInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationInitializer.class);

    /**
     * 初始化时间戳Redis key前缀
     */
    private static final String INIT_TIMESTAMP_KEY = "init:data:";

    /**
     * 日期格式化器
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private LegalHolidayService legalHolidayService;

    @Autowired
    private HolidayCacheConfig holidayCacheConfig;

    @Autowired
    private SimpleReminderRepository simpleReminderRepository;

    @Autowired
    private CacheUtils cacheUtils;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("开始执行应用初始化操作...");

        try {
            String today = LocalDate.now().format(DATE_FORMATTER);

            // 检查今天是否已经初始化过
            if (isAlreadyInitializedToday(today)) {
                logger.info("今天({})已经初始化过，跳过初始化操作", today);
                return;
            }

            // 预加载节假日缓存
            if (holidayCacheConfig.isEnabled()) {
                if (holidayCacheConfig.isAsyncPreload()) {
                    // 异步预加载
                    preloadHolidayCacheAsync();
                    logger.info("节假日缓存异步预加载已启动");
                } else {
                    // 同步预加载
                    preloadHolidayCache();
                }
            } else {
                logger.info("节假日缓存预加载已禁用");
            }

            // 同步数据库和Redis ZSet
            syncDatabaseToRedisAsync();
            logger.info("数据库与Redis ZSet异步同步已启动");

            // 标记今天已初始化
            markInitializedToday(today);

            logger.info("应用初始化操作完成，已标记今天({})为已初始化", today);
        } catch (Exception e) {
            logger.error("应用初始化过程中发生错误", e);
            // 不抛出异常，避免影响应用启动
        }
    }

    /**
     * 预加载节假日缓存
     */
    private void preloadHolidayCache() {
        try {
            int currentYear = LocalDate.now().getYear();
            int startYear = currentYear - holidayCacheConfig.getYearsBefore();
            int endYear = currentYear + holidayCacheConfig.getYearsAfter();
            
            logger.info("开始预加载节假日缓存，年份范围: {} - {}", startYear, endYear);
            
            long startTime = System.currentTimeMillis();
            
            // 预加载指定年份范围的节假日数据
            legalHolidayService.preloadHolidays(startYear, endYear);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // 获取缓存统计信息
            String cacheStats = legalHolidayService.getCacheStats();
            
            logger.info("节假日缓存预加载完成，耗时: {}ms，{}", duration, cacheStats);
            
        } catch (Exception e) {
            logger.error("预加载节假日缓存失败", e);
        }
    }

    /**
     * 异步预加载节假日缓存
     */
    @Async
    public CompletableFuture<Void> preloadHolidayCacheAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                int currentYear = LocalDate.now().getYear();
                int startYear = currentYear - holidayCacheConfig.getYearsBefore();
                int endYear = currentYear + holidayCacheConfig.getYearsAfter();
                
                logger.info("开始异步预加载节假日缓存，年份范围: {} - {}", startYear, endYear);
                
                long startTime = System.currentTimeMillis();
                
                // 预加载指定年份范围的节假日数据
                legalHolidayService.preloadHolidays(startYear, endYear);
                
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                
                // 获取缓存统计信息
                String cacheStats = legalHolidayService.getCacheStats();
                
                logger.info("节假日缓存异步预加载完成，耗时: {}ms，{}", duration, cacheStats);
                
            } catch (Exception e) {
                logger.error("异步预加载节假日缓存失败", e);
            }
        });
    }

    /**
     * 同步数据库和Redis ZSet
     */
    @Async
    public CompletableFuture<Void> syncDatabaseToRedisAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("开始同步数据库到Redis ZSet...");
                
                long startTime = System.currentTimeMillis();
                
                // 获取所有未来的提醒（不包括已过期的）
                OffsetDateTime now = OffsetDateTime.now();
                List<SimpleReminder> futureReminders = simpleReminderRepository.findByEventTimeAfter(now);
                
                if (futureReminders.isEmpty()) {
                    logger.info("没有需要同步的未来提醒数据");
                    return;
                }
                
                // 按用户分组并同步到缓存
                futureReminders.stream()
                    .collect(Collectors.groupingBy(SimpleReminder::getToUserId))
                    .forEach((userId, reminders) -> {
                        if (userId != null && !reminders.isEmpty()) {
                            try {
                                // 先清空用户的现有缓存
                                cacheUtils.clearUserRemindersCache(userId);
                                
                                // 批量添加提醒到缓存
                                cacheUtils.addRemindersToCache(userId, reminders);
                                
                                logger.debug("已同步用户[{}]的{}条提醒到缓存", userId, reminders.size());
                            } catch (Exception e) {
                                logger.error("同步用户[{}]提醒到缓存失败", userId, e);
                            }
                        }
                    });
                
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                
                // 统计同步结果
                long totalUsers = futureReminders.stream()
                    .map(SimpleReminder::getToUserId)
                    .filter(userId -> userId != null)
                    .distinct()
                    .count();
                
                logger.info("数据库与Redis ZSet同步完成，共同步{}个用户的{}条提醒，耗时{}ms",
                           totalUsers, futureReminders.size(), duration);

            } catch (Exception e) {
                logger.error("同步数据库和Redis ZSet失败", e);
            }
        });
    }

    /**
     * 检查今天是否已经初始化过
     * @param today 今天的日期字符串 (yyyy-MM-dd)
     * @return true表示已初始化，false表示未初始化
     */
    private boolean isAlreadyInitializedToday(String today) {
        try {
            String key = INIT_TIMESTAMP_KEY + today;
            String value = stringRedisTemplate.opsForValue().get(key);
            boolean initialized = value != null;

            if (initialized) {
                logger.info("发现初始化标记: {} = {}", key, value);
            } else {
                logger.info("未发现今天的初始化标记: {}", key);
            }

            return initialized;
        } catch (Exception e) {
            logger.error("检查初始化状态失败", e);
            // 出错时返回false，允许重新初始化
            return false;
        }
    }

    /**
     * 标记今天已初始化
     * @param today 今天的日期字符串 (yyyy-MM-dd)
     */
    private void markInitializedToday(String today) {
        try {
            String key = INIT_TIMESTAMP_KEY + today;
            String value = OffsetDateTime.now().toString();

            // 设置过期时间为2天，避免Redis中积累过多的标记
            stringRedisTemplate.opsForValue().set(key, value, java.time.Duration.ofDays(2));

            logger.info("已标记今天初始化完成: {} = {}", key, value);
        } catch (Exception e) {
            logger.error("标记初始化状态失败", e);
        }
    }
}