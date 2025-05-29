package com.core.reminder.config;

import com.core.reminder.service.LegalHolidayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

/**
 * 应用初始化配置类
 * 在应用启动时执行初始化操作
 */
@Component
public class ApplicationInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationInitializer.class);

    @Autowired
    private LegalHolidayService legalHolidayService;

    @Autowired
    private HolidayCacheConfig holidayCacheConfig;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("开始执行应用初始化操作...");
        
        try {
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
            
            logger.info("应用初始化操作完成");
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
} 