package com.core.reminder.controller;

import com.core.reminder.service.LegalHolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 节假日缓存管理控制器
 */
@RestController
@RequestMapping("/api/holiday-cache")
public class HolidayCacheController {

    @Autowired
    private LegalHolidayService legalHolidayService;

    /**
     * 获取缓存统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "缓存统计信息获取成功");
        response.put("data", legalHolidayService.getCacheStats());
        return ResponseEntity.ok(response);
    }

    /**
     * 清除指定年份的缓存
     */
    @DeleteMapping("/year/{year}")
    public ResponseEntity<Map<String, Object>> clearCacheForYear(@PathVariable Integer year) {
        Map<String, Object> response = new HashMap<>();
        try {
            legalHolidayService.clearCacheForYear(year);
            response.put("success", true);
            response.put("message", "年份 " + year + " 的缓存已清除");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "清除缓存失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 清除所有缓存
     */
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Object>> clearAllCache() {
        Map<String, Object> response = new HashMap<>();
        try {
            legalHolidayService.clearAllCache();
            response.put("success", true);
            response.put("message", "所有缓存已清除");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "清除缓存失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 预加载指定年份范围的缓存
     */
    @PostMapping("/preload")
    public ResponseEntity<Map<String, Object>> preloadCache(
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            // 如果没有指定年份，默认加载当前年份前后各一年
            if (startYear == null || endYear == null) {
                int currentYear = LocalDate.now().getYear();
                startYear = currentYear - 1;
                endYear = currentYear + 1;
            }
            
            long startTime = System.currentTimeMillis();
            legalHolidayService.preloadHolidays(startYear, endYear);
            long duration = System.currentTimeMillis() - startTime;
            
            response.put("success", true);
            response.put("message", String.format("年份范围 %d-%d 的缓存预加载完成，耗时: %dms", 
                startYear, endYear, duration));
            response.put("stats", legalHolidayService.getCacheStats());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "预加载缓存失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 