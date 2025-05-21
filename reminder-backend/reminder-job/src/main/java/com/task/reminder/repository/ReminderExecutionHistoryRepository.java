package com.task.reminder.repository;

import com.common.reminder.model.ReminderExecutionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for {@link ReminderExecutionHistory} entities.
 * Provides CRUD operations and potentially custom query methods for reminder execution history.
 */
@Repository
public interface ReminderExecutionHistoryRepository extends JpaRepository<ReminderExecutionHistory, Long> {
    // 你可以在这里添加自定义的查询方法，如果需要的话
    // 例如: List<ReminderExecutionHistory> findByStatus(String status);
    // List<ReminderExecutionHistory> findByTriggeringReminderIdAndTriggeringReminderType(Long reminderId, String type);
} 