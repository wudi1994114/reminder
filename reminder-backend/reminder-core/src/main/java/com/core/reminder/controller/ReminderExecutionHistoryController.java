package com.core.reminder.controller;

import com.common.reminder.dto.UserProfileDto;
import com.common.reminder.model.ReminderExecutionHistory;
import com.core.reminder.dto.ReminderExecutionHistoryDTO;
import com.core.reminder.service.ReminderExecutionHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 提醒执行历史记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/reminder-histories")
public class ReminderExecutionHistoryController {

    @Autowired
    private ReminderExecutionHistoryService historyService;

    /**
     * 获取当前用户的执行历史记录（分页）
     * GET /api/reminder-histories?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserHistories(
            @RequestAttribute("currentUser") UserProfileDto currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Long userId = currentUser.getId();
            log.info("获取用户执行历史记录, userId: {}, page: {}, size: {}", userId, page, size);
            
            Page<ReminderExecutionHistory> historyPage = historyService.getUserHistories(userId, page, size);
            
            // 转换为DTO
            Page<ReminderExecutionHistoryDTO> dtoPage = historyPage.map(this::convertToDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", dtoPage.getContent());
            response.put("totalElements", dtoPage.getTotalElements());
            response.put("totalPages", dtoPage.getTotalPages());
            response.put("currentPage", dtoPage.getNumber());
            response.put("size", dtoPage.getSize());
            response.put("hasNext", dtoPage.hasNext());
            response.put("hasPrevious", dtoPage.hasPrevious());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取用户执行历史记录失败", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "获取执行历史记录失败", e);
        }
    }

    /**
     * 根据时间范围获取当前用户的执行历史记录
     * GET /api/reminder-histories/range?startTime=2023-01-01T00:00:00Z&endTime=2023-12-31T23:59:59Z&page=0&size=20
     */
    @GetMapping("/range")
    public ResponseEntity<Map<String, Object>> getUserHistoriesInTimeRange(
            @RequestAttribute("currentUser") UserProfileDto currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Long userId = currentUser.getId();
            log.info("获取用户时间范围内的执行历史记录, userId: {}, startTime: {}, endTime: {}", userId, startTime, endTime);
            
            Page<ReminderExecutionHistory> historyPage = historyService.getUserHistoriesInTimeRange(userId, startTime, endTime, page, size);
            
            // 转换为DTO
            Page<ReminderExecutionHistoryDTO> dtoPage = historyPage.map(this::convertToDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", dtoPage.getContent());
            response.put("totalElements", dtoPage.getTotalElements());
            response.put("totalPages", dtoPage.getTotalPages());
            response.put("currentPage", dtoPage.getNumber());
            response.put("size", dtoPage.getSize());
            response.put("hasNext", dtoPage.hasNext());
            response.put("hasPrevious", dtoPage.hasPrevious());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取用户时间范围内的执行历史记录失败", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "获取执行历史记录失败", e);
        }
    }

    /**
     * 根据ID获取执行历史记录详情
     * GET /api/reminder-histories/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReminderExecutionHistoryDTO> getHistoryById(
            @PathVariable Long id,
            @RequestAttribute("currentUser") UserProfileDto currentUser) {
        
        try {
            log.info("获取执行历史记录详情, id: {}", id);
            
            Optional<ReminderExecutionHistory> historyOpt = historyService.getHistoryById(id);
            
            if (!historyOpt.isPresent()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "执行历史记录不存在");
            }
            
            ReminderExecutionHistory history = historyOpt.get();
            
            // 验证用户权限：只能查看自己相关的历史记录
            Long userId = currentUser.getId();
            if (!history.getToUserId().equals(userId) && !history.getFromUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限查看此执行历史记录");
            }
            
            return ResponseEntity.ok(convertToDTO(history));
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取执行历史记录详情失败", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "获取执行历史记录详情失败", e);
        }
    }

    /**
     * 删除执行历史记录
     * DELETE /api/reminder-histories/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHistory(
            @PathVariable Long id,
            @RequestAttribute("currentUser") UserProfileDto currentUser) {
        
        try {
            log.info("删除执行历史记录, id: {}", id);
            
            // 验证记录是否存在以及用户权限
            Optional<ReminderExecutionHistory> historyOpt = historyService.getHistoryById(id);
            
            if (!historyOpt.isPresent()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "执行历史记录不存在");
            }
            
            ReminderExecutionHistory history = historyOpt.get();
            Long userId = currentUser.getId();
            
            // 只允许删除自己接收的历史记录
            if (!history.getToUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限删除此执行历史记录");
            }
            
            boolean deleted = historyService.deleteHistory(id);
            
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "删除执行历史记录失败");
            }
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除执行历史记录失败", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "删除执行历史记录失败", e);
        }
    }

    /**
     * 批量删除用户的历史记录（删除指定时间之前的记录）
     * DELETE /api/reminder-histories/cleanup?beforeTime=2023-01-01T00:00:00Z
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupUserHistories(
            @RequestAttribute("currentUser") UserProfileDto currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime beforeTime) {
        
        try {
            Long userId = currentUser.getId();
            log.info("批量删除用户执行历史记录, userId: {}, beforeTime: {}", userId, beforeTime);
            
            int deletedCount = historyService.deleteUserHistoriesBefore(userId, beforeTime);
            
            Map<String, Object> response = new HashMap<>();
            response.put("deletedCount", deletedCount);
            response.put("message", "成功删除 " + deletedCount + " 条历史记录");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("批量删除用户执行历史记录失败", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "批量删除执行历史记录失败", e);
        }
    }

    /**
     * 获取用户执行历史统计信息
     * GET /api/reminder-histories/statistics?startTime=2023-01-01T00:00:00Z&endTime=2023-12-31T23:59:59Z
     */
    @GetMapping("/statistics")
    public ResponseEntity<ReminderExecutionHistoryService.HistoryStatistics> getUserHistoryStatistics(
            @RequestAttribute("currentUser") UserProfileDto currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        
        try {
            Long userId = currentUser.getId();
            log.info("获取用户执行历史统计, userId: {}, startTime: {}, endTime: {}", userId, startTime, endTime);
            
            ReminderExecutionHistoryService.HistoryStatistics statistics = 
                    historyService.getUserHistoryStatistics(userId, startTime, endTime);
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            log.error("获取用户执行历史统计失败", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "获取执行历史统计失败", e);
        }
    }

    /**
     * 转换实体为DTO
     */
    private ReminderExecutionHistoryDTO convertToDTO(ReminderExecutionHistory history) {
        ReminderExecutionHistoryDTO dto = new ReminderExecutionHistoryDTO();
        BeanUtils.copyProperties(history, dto);
        return dto;
    }
}
