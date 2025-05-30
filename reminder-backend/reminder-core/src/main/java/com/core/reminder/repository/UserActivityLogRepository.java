package com.core.reminder.repository;

import com.common.reminder.constant.ActivityAction;
import com.common.reminder.constant.ActivityStatus;
import com.common.reminder.constant.ResourceType;
import com.common.reminder.model.UserActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 用户活动日志数据访问层接口
 */
@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {

    /**
     * 根据用户ID查询活动日志
     */
    Page<UserActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 根据用户ID和时间范围查询活动日志
     */
    Page<UserActivityLog> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId, OffsetDateTime startTime, OffsetDateTime endTime, Pageable pageable);

    /**
     * 根据操作类型查询活动日志
     */
    Page<UserActivityLog> findByActionOrderByCreatedAtDesc(ActivityAction action, Pageable pageable);

    /**
     * 根据资源类型和资源ID查询活动日志
     */
    List<UserActivityLog> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
            ResourceType resourceType, Long resourceId);

    /**
     * 根据会话ID查询活动日志
     */
    List<UserActivityLog> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    /**
     * 根据状态查询活动日志
     */
    Page<UserActivityLog> findByStatusOrderByCreatedAtDesc(ActivityStatus status, Pageable pageable);

    /**
     * 查询指定时间范围内的活动日志
     */
    Page<UserActivityLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            OffsetDateTime startTime, OffsetDateTime endTime, Pageable pageable);

    /**
     * 统计用户在指定时间范围内的操作次数
     */
    @Query("SELECT COUNT(l) FROM UserActivityLog l WHERE l.userId = :userId " +
           "AND l.createdAt BETWEEN :startTime AND :endTime")
    Long countByUserIdAndTimeRange(@Param("userId") Long userId,
                                   @Param("startTime") OffsetDateTime startTime,
                                   @Param("endTime") OffsetDateTime endTime);

    /**
     * 统计指定操作类型在时间范围内的次数
     */
    @Query("SELECT COUNT(l) FROM UserActivityLog l WHERE l.action = :action " +
           "AND l.createdAt BETWEEN :startTime AND :endTime")
    Long countByActionAndTimeRange(@Param("action") ActivityAction action,
                                   @Param("startTime") OffsetDateTime startTime,
                                   @Param("endTime") OffsetDateTime endTime);

    /**
     * 查询用户最近的登录记录
     */
    @Query("SELECT l FROM UserActivityLog l WHERE l.userId = :userId " +
           "AND l.action IN ('LOGIN', 'WECHAT_LOGIN') " +
           "ORDER BY l.createdAt DESC")
    List<UserActivityLog> findRecentLoginsByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 查询失败的登录尝试
     */
    @Query("SELECT l FROM UserActivityLog l WHERE l.action = 'LOGIN_FAILED' " +
           "AND l.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY l.createdAt DESC")
    List<UserActivityLog> findFailedLoginAttempts(@Param("startTime") OffsetDateTime startTime,
                                                  @Param("endTime") OffsetDateTime endTime);

    /**
     * 根据IP地址查询活动日志
     */
    @Query("SELECT l FROM UserActivityLog l WHERE CAST(l.ipAddress AS string) = :ipAddress " +
           "ORDER BY l.createdAt DESC")
    List<UserActivityLog> findByIpAddress(@Param("ipAddress") String ipAddress, Pageable pageable);

    /**
     * 查询高风险操作日志
     */
    @Query("SELECT l FROM UserActivityLog l WHERE l.action IN " +
           "('PASSWORD_CHANGE', 'PERMISSION_GRANT', 'ROLE_ASSIGN', 'ACCOUNT_LOCKED') " +
           "ORDER BY l.createdAt DESC")
    Page<UserActivityLog> findHighRiskActivities(Pageable pageable);

    /**
     * 删除指定时间之前的日志记录（用于数据清理）
     */
    void deleteByCreatedAtBefore(OffsetDateTime cutoffTime);

    /**
     * 统计各操作类型的数量
     */
    @Query("SELECT l.action, COUNT(l) FROM UserActivityLog l " +
           "WHERE l.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY l.action ORDER BY COUNT(l) DESC")
    List<Object[]> countByActionInTimeRange(@Param("startTime") OffsetDateTime startTime,
                                            @Param("endTime") OffsetDateTime endTime);

    /**
     * 统计各用户的活动数量
     */
    @Query("SELECT l.userId, COUNT(l) FROM UserActivityLog l " +
           "WHERE l.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY l.userId ORDER BY COUNT(l) DESC")
    List<Object[]> countByUserInTimeRange(@Param("startTime") OffsetDateTime startTime,
                                          @Param("endTime") OffsetDateTime endTime);
} 