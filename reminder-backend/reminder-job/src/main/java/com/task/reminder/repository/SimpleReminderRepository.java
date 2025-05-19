package com.task.reminder.repository;

import com.common.reminder.model.SimpleReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    // 查询最近的10个提醒(按eventTime从近到远排序)
    List<SimpleReminder> findTop10ByEventTimeAfterOrderByEventTimeAsc(OffsetDateTime now);

    /**
     * 查询未来1分钟内需要触发的提醒事项
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 在指定时间范围内的提醒事项列表
     */
    @Query("SELECT sr FROM SimpleReminder sr WHERE sr.eventTime BETWEEN :startTime AND :endTime ORDER BY sr.eventTime ASC")
    List<SimpleReminder> findByEventTimeBetween(@Param("startTime") OffsetDateTime startTime, @Param("endTime") OffsetDateTime endTime);

    /**
     * 删除指定复杂提醒ID相关的所有简单提醒
     * @param originatingComplexReminderId 来源复杂提醒ID
     * @return 删除的记录数
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM SimpleReminder sr WHERE sr.originatingComplexReminderId = :originatingComplexReminderId")
    int deleteByOriginatingComplexReminderId(@Param("originatingComplexReminderId") Long originatingComplexReminderId);

    /**
     * 按年月和用户ID查询简单提醒
     * 查询指定月份内触发的提醒
     * 
     * @param year 年份
     * @param month 月份(1-12)
     * @param userId 用户ID
     * @return 符合条件的简单提醒列表
     */
    @Query("SELECT sr FROM SimpleReminder sr WHERE " +
           "YEAR(sr.eventTime) = :year AND MONTH(sr.eventTime) = :month AND " +
           "sr.toUserId = :userId ORDER BY sr.eventTime ASC")
    List<SimpleReminder> findByYearMonthAndUserId(
            @Param("year") int year, 
            @Param("month") int month, 
            @Param("userId") Long userId);
    
    /**
     * 按年月查询简单提醒（不限用户）
     * 查询指定月份内触发的提醒
     * 
     * @param year 年份
     * @param month 月份(1-12)
     * @return 符合条件的简单提醒列表
     */
    @Query("SELECT sr FROM SimpleReminder sr WHERE " +
           "YEAR(sr.eventTime) = :year AND MONTH(sr.eventTime) = :month " +
           "ORDER BY sr.eventTime ASC")
    List<SimpleReminder> findByYearMonth(@Param("year") int year, @Param("month") int month);

    // 可以根据需要添加更多查询方法
} 