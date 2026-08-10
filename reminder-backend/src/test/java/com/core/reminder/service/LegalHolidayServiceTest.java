package com.core.reminder.service;

import com.core.reminder.model.LegalHoliday;
import com.core.reminder.repository.LegalHolidayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LegalHolidayServiceTest {

    @Mock
    private LegalHolidayRepository legalHolidayRepository;

    @InjectMocks
    private LegalHolidayService legalHolidayService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCacheHit() {
        // 准备测试数据
        LegalHoliday holiday1 = new LegalHoliday();
        holiday1.setYear(2024);
        holiday1.setMonth(1);
        holiday1.setDay(1);
        holiday1.setName("元旦");
        holiday1.setHoliday(true);

        LegalHoliday holiday2 = new LegalHoliday();
        holiday2.setYear(2024);
        holiday2.setMonth(10);
        holiday2.setDay(1);
        holiday2.setName("国庆节");
        holiday2.setHoliday(true);

        List<LegalHoliday> mockHolidays = Arrays.asList(holiday1, holiday2);

        // 模拟数据库返回
        when(legalHolidayRepository.findByYear(2024)).thenReturn(mockHolidays);

        // 第一次调用 - 应该查询数据库
        List<LegalHoliday> result1 = legalHolidayService.getHolidaysByYearRange(2024, 2024);
        assertEquals(2, result1.size());
        verify(legalHolidayRepository, times(1)).findByYear(2024);

        // 第二次调用 - 应该从缓存获取，不再查询数据库
        List<LegalHoliday> result2 = legalHolidayService.getHolidaysByYearRange(2024, 2024);
        assertEquals(2, result2.size());
        // 验证数据库只被调用了一次
        verify(legalHolidayRepository, times(1)).findByYear(2024);

        // 验证缓存统计信息
        String cacheStats = legalHolidayService.getCacheStats();
        assertTrue(cacheStats.contains("已缓存年份数量: 1"));
        assertTrue(cacheStats.contains("缓存的节假日总数: 2"));
    }

    @Test
    void testClearCache() {
        // 准备测试数据
        LegalHoliday holiday = new LegalHoliday();
        holiday.setYear(2024);
        holiday.setMonth(1);
        holiday.setDay(1);
        holiday.setName("元旦");
        holiday.setHoliday(true);

        when(legalHolidayRepository.findByYear(2024)).thenReturn(Arrays.asList(holiday));

        // 第一次调用 - 加载到缓存
        legalHolidayService.getHolidaysByYearRange(2024, 2024);
        verify(legalHolidayRepository, times(1)).findByYear(2024);

        // 清除缓存
        legalHolidayService.clearCacheForYear(2024);

        // 再次调用 - 应该重新查询数据库
        legalHolidayService.getHolidaysByYearRange(2024, 2024);
        verify(legalHolidayRepository, times(2)).findByYear(2024);
    }

    @Test
    void testPreloadHolidays() {
        // 准备测试数据
        when(legalHolidayRepository.findByYear(2023)).thenReturn(Arrays.asList(new LegalHoliday()));
        when(legalHolidayRepository.findByYear(2024)).thenReturn(Arrays.asList(new LegalHoliday()));

        // 预加载数据
        legalHolidayService.preloadHolidays(2023, 2024);

        // 验证数据库被调用
        verify(legalHolidayRepository, times(1)).findByYear(2023);
        verify(legalHolidayRepository, times(1)).findByYear(2024);

        // 再次获取数据，应该从缓存获取
        legalHolidayService.getHolidaysByYearRange(2023, 2024);
        
        // 验证数据库没有再次被调用
        verify(legalHolidayRepository, times(1)).findByYear(2023);
        verify(legalHolidayRepository, times(1)).findByYear(2024);
    }
} 