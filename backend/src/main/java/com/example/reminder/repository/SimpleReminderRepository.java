package com.example.reminder.repository;

import com.example.reminder.model.SimpleReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface SimpleReminderRepository extends JpaRepository<SimpleReminder, Long> {

    // 可能需要的查询方法示例
    List<SimpleReminder> findByFromUserId(Long fromUserId);

    List<SimpleReminder> findByToUserId(Long toUserId);

    // 查询特定时间点之前的提醒 (常用于定时任务扫描)
    List<SimpleReminder> findByEventTimeBefore(OffsetDateTime time);

    // 根据来源的复杂提醒 ID 查询
    List<SimpleReminder> findByOriginatingComplexReminderId(Long complexReminderId);

    // Add method for duplicate check in ComplexReminderSchedulingJob
    boolean existsByOriginatingComplexReminderIdAndEventTime(Long complexReminderId, OffsetDateTime eventTime);

    // 可以根据需要添加更多查询方法
} 