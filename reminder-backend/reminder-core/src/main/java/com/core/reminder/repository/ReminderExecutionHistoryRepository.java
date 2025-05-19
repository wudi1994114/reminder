package com.core.reminder.repository;

import com.core.reminder.model.ReminderExecutionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReminderExecutionHistoryRepository extends JpaRepository<ReminderExecutionHistory, Long> {

    // 根据触发源查询历史记录
    List<ReminderExecutionHistory> findByTriggeringReminderTypeAndTriggeringReminderId(String type, Long id);

    // 根据用户查询历史记录
    List<ReminderExecutionHistory> findByFromUserId(Long fromUserId);
    List<ReminderExecutionHistory> findByToUserId(Long toUserId);

    // 根据状态查询历史记录
    List<ReminderExecutionHistory> findByStatus(String status);

    // 可以添加基于时间范围等的查询
} 