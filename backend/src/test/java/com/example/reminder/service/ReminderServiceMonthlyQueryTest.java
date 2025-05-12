package com.example.reminder.service;

import com.example.reminder.model.ComplexReminder;
import com.example.reminder.model.ReminderType;
import com.example.reminder.model.SimpleReminder;
import com.example.reminder.repository.ComplexReminderRepository;
import com.example.reminder.repository.SimpleReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReminderServiceMonthlyQueryTest {
    
    @Mock
    private SimpleReminderRepository simpleReminderRepository;
    
    @Mock
    private ComplexReminderRepository complexReminderRepository;
    
    @Mock
    private Scheduler scheduler;
    
    @InjectMocks
    private ReminderEventServiceImpl reminderService;
    
    @Captor
    private ArgumentCaptor<ComplexReminder> complexReminderCaptor;
    
    private ComplexReminder testReminder;
    
    @BeforeEach
    void setup() {
        // 创建测试用的复杂提醒
        testReminder = new ComplexReminder();
        testReminder.setId(1L);
        testReminder.setFromUserId(101L);
        testReminder.setToUserId(102L);
        testReminder.setTitle("测试提醒");
        testReminder.setDescription("测试描述");
        testReminder.setCronExpression("0 0 10 1 * ?"); // 每月1日上午10点
        testReminder.setReminderType(ReminderType.EMAIL);
        testReminder.setValidFrom(LocalDate.now().minusMonths(1));
        testReminder.setValidUntil(LocalDate.now().plusYears(1));
        
        // 模拟复杂任务生成方法的行为
        doReturn(new ArrayList<SimpleReminder>())
            .when(reminderService).generateSimpleRemindersForMonths(any(ComplexReminder.class), anyInt());
    }
    
    @Test
    void testGetSimpleRemindersByYearMonth_ShouldEnsureTasksGenerated() {
        // 设置当前年月
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        
        // 模拟仓库的行为
        when(complexReminderRepository.findByLastGeneratedYmLessThanOrLastGeneratedYmIsNull(anyInt()))
            .thenReturn(Arrays.asList(testReminder));
        
        when(simpleReminderRepository.findByYearMonth(currentYear, currentMonth))
            .thenReturn(new ArrayList<>());
        
        // 调用被测试的方法
        List<SimpleReminder> result = reminderService.getSimpleRemindersByYearMonth(currentYear, currentMonth);
        
        // 验证调用生成方法
        verify(reminderService).generateSimpleRemindersForMonths(eq(testReminder), anyInt());
        
        // 验证查询简单任务
        verify(simpleReminderRepository).findByYearMonth(currentYear, currentMonth);
    }
    
    @Test
    void testGetSimpleRemindersByYearMonthAndUser_ShouldEnsureTasksGenerated() {
        // 设置当前年月
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        Long userId = 102L;
        
        // 模拟仓库的行为
        when(complexReminderRepository.findByLastGeneratedYmLessThanOrLastGeneratedYmIsNull(anyInt()))
            .thenReturn(Arrays.asList(testReminder));
        
        when(simpleReminderRepository.findByYearMonthAndUserId(currentYear, currentMonth, userId))
            .thenReturn(new ArrayList<>());
        
        // 调用被测试的方法
        List<SimpleReminder> result = reminderService.getSimpleRemindersByYearMonthAndUser(currentYear, currentMonth, userId);
        
        // 验证调用生成方法
        verify(reminderService).generateSimpleRemindersForMonths(eq(testReminder), anyInt());
        
        // 验证查询简单任务
        verify(simpleReminderRepository).findByYearMonthAndUserId(currentYear, currentMonth, userId);
    }
    
    @Test
    void testEnsureComplexRemindersGenerated_HistoricalMonth_ShouldUpdateLastGeneratedYmOnly() {
        // 设置历史年月（一年前）
        int historicalYear = LocalDate.now().getYear() - 1;
        int historicalMonth = LocalDate.now().getMonthValue();
        int historicalYm = historicalYear * 100 + historicalMonth;
        
        // 复杂任务没有lastGeneratedYm
        testReminder.setLastGeneratedYm(null);
        
        // 模拟仓库的行为
        when(complexReminderRepository.findByLastGeneratedYmLessThanOrLastGeneratedYmIsNull(historicalYm + 1))
            .thenReturn(Arrays.asList(testReminder));
        
        // 调用私有方法
        ReflectionTestUtils.invokeMethod(
            reminderService,
            "ensureComplexRemindersGenerated",
            historicalYear,
            historicalMonth
        );
        
        // 验证：对于历史月份应只更新lastGeneratedYm，而不调用生成方法
        verify(complexReminderRepository).save(complexReminderCaptor.capture());
        verify(reminderService, never()).generateSimpleRemindersForMonths(any(), anyInt());
        
        // 验证lastGeneratedYm被正确更新
        assertEquals(historicalYm, complexReminderCaptor.getValue().getLastGeneratedYm());
    }
    
    @Test
    void testEnsureComplexRemindersGenerated_FutureMonth_ShouldGenerateTasks() {
        // 设置未来年月（三个月后）
        LocalDate futureDate = LocalDate.now().plusMonths(3);
        int futureYear = futureDate.getYear();
        int futureMonth = futureDate.getMonthValue();
        
        // 模拟仓库的行为
        when(complexReminderRepository.findByLastGeneratedYmLessThanOrLastGeneratedYmIsNull(anyInt()))
            .thenReturn(Arrays.asList(testReminder));
        
        // 调用私有方法
        ReflectionTestUtils.invokeMethod(
            reminderService,
            "ensureComplexRemindersGenerated",
            futureYear,
            futureMonth
        );
        
        // 验证：对于未来月份应调用生成方法
        verify(reminderService).generateSimpleRemindersForMonths(eq(testReminder), anyInt());
    }
} 