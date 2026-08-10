package com.core.reminder.repository;

import com.common.reminder.model.ReminderExecutionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ReminderExecutionHistoryRepository extends JpaRepository<ReminderExecutionHistory, Long> {

    // 根据触发源查询历史记录
    List<ReminderExecutionHistory> findByTriggeringReminderTypeAndTriggeringReminderId(String type, Long id);

    // 根据用户查询历史记录
    List<ReminderExecutionHistory> findByFromUserId(Long fromUserId);
    List<ReminderExecutionHistory> findByToUserId(Long toUserId);

    // 分页查询用户历史记录
    Page<ReminderExecutionHistory> findByToUserId(Long toUserId, Pageable pageable);
    Page<ReminderExecutionHistory> findByFromUserId(Long fromUserId, Pageable pageable);

    // 根据状态查询历史记录
    List<ReminderExecutionHistory> findByStatus(String status);
    Page<ReminderExecutionHistory> findByStatus(String status, Pageable pageable);

    // 时间范围查询
    Page<ReminderExecutionHistory> findByToUserIdAndExecutedAtBetween(Long toUserId, OffsetDateTime startTime, OffsetDateTime endTime, Pageable pageable);
    Page<ReminderExecutionHistory> findByExecutedAtBetween(OffsetDateTime startTime, OffsetDateTime endTime, Pageable pageable);

    // 删除操作
    @Modifying
    @Query("DELETE FROM ReminderExecutionHistory h WHERE h.toUserId = :userId AND h.executedAt < :beforeTime")
    int deleteByToUserIdAndExecutedAtBefore(@Param("userId") Long userId, @Param("beforeTime") OffsetDateTime beforeTime);

    @Modifying
    @Query("DELETE FROM ReminderExecutionHistory h WHERE h.executedAt < :beforeTime")
    int deleteByExecutedAtBefore(@Param("beforeTime") OffsetDateTime beforeTime);

    // 统计查询
    long countByToUserIdAndExecutedAtBetween(Long toUserId, OffsetDateTime startTime, OffsetDateTime endTime);
    long countByToUserIdAndStatusAndExecutedAtBetween(Long toUserId, String status, OffsetDateTime startTime, OffsetDateTime endTime);
}