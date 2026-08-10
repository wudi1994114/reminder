package com.core.reminder.service;

import com.common.reminder.model.ReminderExecutionHistory;
import com.core.reminder.repository.ReminderExecutionHistoryRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 提醒执行历史记录服务
 */
@Slf4j
@Service
public class ReminderExecutionHistoryService {

    @Autowired
    private ReminderExecutionHistoryRepository historyRepository;

    /**
     * 根据ID获取执行历史记录
     * @param id 历史记录ID
     * @return 执行历史记录
     */
    @Transactional(readOnly = true)
    public Optional<ReminderExecutionHistory> getHistoryById(Long id) {
        log.debug("获取执行历史记录, ID: {}", id);
        return historyRepository.findById(id);
    }

    /**
     * 获取用户的执行历史记录（分页）
     * @param userId 用户ID
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页的执行历史记录
     */
    @Transactional(readOnly = true)
    public Page<ReminderExecutionHistory> getUserHistories(Long userId, int page, int size) {
        log.debug("获取用户执行历史记录, userId: {}, page: {}, size: {}", userId, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "executedAt"));
        return historyRepository.findByToUserId(userId, pageable);
    }

    /**
     * 获取用户在指定时间范围内的执行历史记录
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页的执行历史记录
     */
    @Transactional(readOnly = true)
    public Page<ReminderExecutionHistory> getUserHistoriesInTimeRange(Long userId, OffsetDateTime startTime, 
                                                                      OffsetDateTime endTime, int page, int size) {
        log.debug("获取用户时间范围内的执行历史记录, userId: {}, startTime: {}, endTime: {}", userId, startTime, endTime);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "executedAt"));
        return historyRepository.findByToUserIdAndExecutedAtBetween(userId, startTime, endTime, pageable);
    }

    /**
     * 根据触发源获取执行历史记录
     * @param triggeringReminderType 触发源类型
     * @param triggeringReminderId 触发源ID
     * @return 执行历史记录列表
     */
    @Transactional(readOnly = true)
    public List<ReminderExecutionHistory> getHistoriesByTriggeringReminder(String triggeringReminderType, Long triggeringReminderId) {
        log.debug("根据触发源获取执行历史记录, type: {}, id: {}", triggeringReminderType, triggeringReminderId);
        return historyRepository.findByTriggeringReminderTypeAndTriggeringReminderId(triggeringReminderType, triggeringReminderId);
    }

    /**
     * 根据状态获取执行历史记录
     * @param status 执行状态
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页的执行历史记录
     */
    @Transactional(readOnly = true)
    public Page<ReminderExecutionHistory> getHistoriesByStatus(String status, int page, int size) {
        log.debug("根据状态获取执行历史记录, status: {}", status);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "executedAt"));
        return historyRepository.findByStatus(status, pageable);
    }

    /**
     * 删除指定ID的执行历史记录
     * @param id 历史记录ID
     * @return 是否删除成功
     */
    @Transactional
    public boolean deleteHistory(Long id) {
        log.info("删除执行历史记录, ID: {}", id);
        try {
            if (historyRepository.existsById(id)) {
                historyRepository.deleteById(id);
                log.info("成功删除执行历史记录, ID: {}", id);
                return true;
            } else {
                log.warn("执行历史记录不存在, ID: {}", id);
                return false;
            }
        } catch (Exception e) {
            log.error("删除执行历史记录失败, ID: {}", id, e);
            return false;
        }
    }

    /**
     * 批量删除用户的执行历史记录
     * @param userId 用户ID
     * @param beforeTime 删除此时间之前的记录
     * @return 删除的记录数量
     */
    @Transactional
    public int deleteUserHistoriesBefore(Long userId, OffsetDateTime beforeTime) {
        log.info("批量删除用户执行历史记录, userId: {}, beforeTime: {}", userId, beforeTime);
        try {
            int deletedCount = historyRepository.deleteByToUserIdAndExecutedAtBefore(userId, beforeTime);
            log.info("成功删除用户执行历史记录, userId: {}, 删除数量: {}", userId, deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("批量删除用户执行历史记录失败, userId: {}", userId, e);
            return 0;
        }
    }

    /**
     * 清理所有过期的执行历史记录
     * @param beforeTime 删除此时间之前的记录
     * @return 删除的记录数量
     */
    @Transactional
    public int cleanupOldHistories(OffsetDateTime beforeTime) {
        log.info("清理过期的执行历史记录, beforeTime: {}", beforeTime);
        try {
            int deletedCount = historyRepository.deleteByExecutedAtBefore(beforeTime);
            log.info("成功清理过期的执行历史记录, 删除数量: {}", deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("清理过期的执行历史记录失败", e);
            return 0;
        }
    }

    /**
     * 获取用户执行历史记录的统计信息
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计信息
     */
    @Transactional(readOnly = true)
    public HistoryStatistics getUserHistoryStatistics(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        log.debug("获取用户执行历史统计, userId: {}, startTime: {}, endTime: {}", userId, startTime, endTime);
        
        long totalCount = historyRepository.countByToUserIdAndExecutedAtBetween(userId, startTime, endTime);
        long successCount = historyRepository.countByToUserIdAndStatusAndExecutedAtBetween(userId, "SUCCESS", startTime, endTime);
        long failureCount = historyRepository.countByToUserIdAndStatusAndExecutedAtBetween(userId, "FAILURE", startTime, endTime);
        
        return HistoryStatistics.builder()
                .totalCount(totalCount)
                .successCount(successCount)
                .failureCount(failureCount)
                .successRate(totalCount > 0 ? (double) successCount / totalCount * 100 : 0.0)
                .build();
    }

    /**
     * 执行历史统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryStatistics {
        private long totalCount;
        private long successCount;
        private long failureCount;
        private double successRate;
    }
}
