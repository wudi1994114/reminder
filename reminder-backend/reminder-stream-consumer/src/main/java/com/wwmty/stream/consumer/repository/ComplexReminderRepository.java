package com.wwmty.stream.consumer.repository;

import com.common.reminder.model.ComplexReminder;
import com.common.reminder.model.ReminderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplexReminderRepository extends JpaRepository<ComplexReminder, Long> {

    // 可能需要的查询方法示例
    List<ComplexReminder> findByFromUserId(Long fromUserId);

    List<ComplexReminder> findByToUserId(Long toUserId);
    
    // 查询lastGeneratedYm小于指定值的所有记录
    List<ComplexReminder> findByLastGeneratedYmLessThanOrLastGeneratedYmIsNull(Integer targetYearMonth);

    // 可以根据需要添加更多查询方法，例如按 cronExpression 查询等

    /**
     * 根据幂等键查找复杂提醒
     * @param idempotencyKey 幂等键
     * @return 复杂提醒对象
     */
    Optional<ComplexReminder> findByIdempotencyKey(String idempotencyKey);

    /**
     * 检查幂等键是否已存在
     * @param idempotencyKey 幂等键
     * @return 是否存在
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * 根据业务字段查找是否存在相同的复杂提醒
     * @param fromUserId 创建用户ID
     * @param toUserId 接收用户ID
     * @param title 标题
     * @param cronExpression CRON表达式
     * @param reminderType 提醒类型
     * @param validFrom 生效开始日期
     * @param validUntil 生效结束日期
     * @param maxExecutions 最大执行次数
     * @return 是否存在相同的复杂提醒
     */
    @Query("SELECT COUNT(cr) > 0 FROM ComplexReminder cr WHERE " +
           "cr.fromUserId = :fromUserId AND cr.toUserId = :toUserId AND " +
           "cr.title = :title AND cr.cronExpression = :cronExpression AND " +
           "cr.reminderType = :reminderType AND " +
           "COALESCE(cr.validFrom, :defaultValidFrom) = COALESCE(:validFrom, :defaultValidFrom) AND " +
           "COALESCE(cr.validUntil, :defaultValidUntil) = COALESCE(:validUntil, :defaultValidUntil) AND " +
           "COALESCE(cr.maxExecutions, :defaultMaxExecutions) = COALESCE(:maxExecutions, :defaultMaxExecutions)")
    boolean existsByBusinessFields(
            @Param("fromUserId") Long fromUserId,
            @Param("toUserId") Long toUserId,
            @Param("title") String title,
            @Param("cronExpression") String cronExpression,
            @Param("reminderType") ReminderType reminderType,
            @Param("validFrom") LocalDate validFrom,
            @Param("validUntil") LocalDate validUntil,
            @Param("maxExecutions") Integer maxExecutions,
            @Param("defaultValidFrom") LocalDate defaultValidFrom,
            @Param("defaultValidUntil") LocalDate defaultValidUntil,
            @Param("defaultMaxExecutions") Integer defaultMaxExecutions
    );

    /**
     * 根据业务字段查找相同的复杂提醒
     * @param fromUserId 创建用户ID
     * @param toUserId 接收用户ID
     * @param title 标题
     * @param cronExpression CRON表达式
     * @param reminderType 提醒类型
     * @param validFrom 生效开始日期
     * @param validUntil 生效结束日期
     * @param maxExecutions 最大执行次数
     * @return 相同的复杂提醒
     */
    @Query("SELECT cr FROM ComplexReminder cr WHERE " +
           "cr.fromUserId = :fromUserId AND cr.toUserId = :toUserId AND " +
           "cr.title = :title AND cr.cronExpression = :cronExpression AND " +
           "cr.reminderType = :reminderType AND " +
           "COALESCE(cr.validFrom, :defaultValidFrom) = COALESCE(:validFrom, :defaultValidFrom) AND " +
           "COALESCE(cr.validUntil, :defaultValidUntil) = COALESCE(:validUntil, :defaultValidUntil) AND " +
           "COALESCE(cr.maxExecutions, :defaultMaxExecutions) = COALESCE(:maxExecutions, :defaultMaxExecutions)")
    Optional<ComplexReminder> findByBusinessFields(
            @Param("fromUserId") Long fromUserId,
            @Param("toUserId") Long toUserId,
            @Param("title") String title,
            @Param("cronExpression") String cronExpression,
            @Param("reminderType") ReminderType reminderType,
            @Param("validFrom") LocalDate validFrom,
            @Param("validUntil") LocalDate validUntil,
            @Param("maxExecutions") Integer maxExecutions,
            @Param("defaultValidFrom") LocalDate defaultValidFrom,
            @Param("defaultValidUntil") LocalDate defaultValidUntil,
            @Param("defaultMaxExecutions") Integer defaultMaxExecutions
    );

    /**
     * 统计用户创建的复杂提醒数量
     * @param fromUserId 创建用户ID
     * @return 复杂提醒数量
     */
    @Query("SELECT COUNT(cr) FROM ComplexReminder cr WHERE cr.fromUserId = :fromUserId")
    long countByFromUserId(@Param("fromUserId") Long fromUserId);
}