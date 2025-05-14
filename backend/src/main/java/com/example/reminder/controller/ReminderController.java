package com.example.reminder.controller;

import com.example.reminder.job.MonthlyComplexReminderJob;
import com.example.reminder.service.ReminderEventServiceImpl;
import com.example.reminder.utils.ReminderMapper;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private static final Logger log = LoggerFactory.getLogger(ReminderController.class);

    private final MonthlyComplexReminderJob monthlyJob;

    @Autowired
    public ReminderController(MonthlyComplexReminderJob monthlyJob) {
        this.monthlyJob = monthlyJob;
    }

    // ... 现有的API方法 ...

    /**
     * 手动触发月度复杂提醒任务，生成未来3个月的提醒
     * 
     * @return 响应消息
     */
    @PostMapping("/generate-future-reminders")
    public ResponseEntity<Map<String, String>> triggerMonthlyComplexReminderJob() {
        log.info("手动触发月度复杂提醒任务");
        
        try {
            // 创建临时的JobExecutionContext对象
            JobExecutionContext dummyContext = null;
            
            // 直接调用月度任务的execute方法
            monthlyJob.execute(dummyContext);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "成功触发月度任务，已生成未来3个月的提醒");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("触发月度任务时出错", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "触发月度任务失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
} 