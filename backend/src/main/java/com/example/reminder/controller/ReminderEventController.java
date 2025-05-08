package com.example.reminder.controller;

// 导入所需的实体类和重构后的服务（或其接口）
import com.example.reminder.dto.ComplexReminderDTO;
import com.example.reminder.dto.SimpleReminderDTO;
import com.example.reminder.model.ComplexReminder;
import com.example.reminder.model.SimpleReminder;
import com.example.reminder.service.ReminderEventServiceImpl; // 暂时使用具体类，后续最好使用接口
import com.example.reminder.utils.ReminderMapper;
// import com.example.reminder.service.ReminderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reminders") // 更新后的基础路径
@CrossOrigin(origins = "http://localhost:5173") // 保持CORS配置
// 考虑重命名为ReminderController
public class ReminderEventController {

    // 注入重构后的服务实现（后续替换为接口）
    private final ReminderEventServiceImpl reminderService;
    private final ReminderMapper reminderMapper;

    @Autowired
    public ReminderEventController(ReminderEventServiceImpl reminderService, ReminderMapper reminderMapper) {
        this.reminderService = reminderService;
        this.reminderMapper = reminderMapper;
    }

    // --- 简单提醒事项的接口 ---

    /**
     * 创建新的简单提醒事项
     * POST /api/reminders/simple
     */
    @PostMapping("/simple")
    public ResponseEntity<SimpleReminderDTO> createSimpleReminder(@RequestBody SimpleReminderDTO reminderDTO) {
        // 验证必要参数
        if (reminderDTO.getToUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "接收提醒的用户ID不能为空");
        }

        try {
            // 获取当前登录用户（创建者）的ID
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户未登录");
            }

            // 确保toUserId已设置（如果前端没有设置，默认设置为当前用户）
            if (reminderDTO.getToUserId() == null) {
                // 假设我们可以从Authentication中获取用户ID
                // 具体实现取决于你的用户认证系统
                Long currentUserId = getCurrentUserId(auth);
                reminderDTO.setToUserId(currentUserId);
            }

            // 转换DTO为实体
            SimpleReminder simpleReminder = reminderMapper.toEntity(reminderDTO);
            
            // 保存实体
            SimpleReminder created = reminderService.createSimpleReminder(simpleReminder);
            
            // 转换实体为DTO并返回
            return new ResponseEntity<>(reminderMapper.toDTO(created), HttpStatus.CREATED);
        } catch (Exception e) {
            // 处理创建/调度过程中的潜在异常
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "创建简单提醒事项失败", e);
        }
    }

    // 获取当前用户ID的辅助方法
    private Long getCurrentUserId(Authentication auth) {
        // 这里的具体实现取决于你的认证系统
        // 例如，如果你使用自定义的UserDetails实现：
        // CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        // return userDetails.getId();
        
        // 这里仅作示例，需要根据实际的认证实现来修改
        throw new UnsupportedOperationException("需要实现获取当前用户ID的方法");
    }

    /**
     * 通过ID获取简单提醒事项
     * GET /api/reminders/simple/{id}
     */
    @GetMapping("/simple/{id}")
    public ResponseEntity<SimpleReminderDTO> getSimpleReminderById(@PathVariable Long id) {
        Optional<SimpleReminder> reminderOpt = reminderService.getSimpleReminderById(id);
        
        return reminderOpt
                .map(reminderMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到简单提醒事项"));
    }

    /**
     * 获取所有简单提醒事项
     * GET /api/reminders/simple
     */
    @GetMapping("/simple")
    public ResponseEntity<List<SimpleReminderDTO>> getAllSimpleReminders() {
        List<SimpleReminder> reminders = reminderService.getAllSimpleReminders();
        List<SimpleReminderDTO> reminderDTOs = reminderMapper.toSimpleReminderDTOList(reminders);
        return ResponseEntity.ok(reminderDTOs);
    }

    /**
     * 获取即将到来的提醒事项(最近10个)
     * GET /api/reminders/upcoming
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<SimpleReminderDTO>> getUpcomingReminders() {
        List<SimpleReminder> reminders = reminderService.getUpcomingReminders();
        List<SimpleReminderDTO> reminderDTOs = reminderMapper.toSimpleReminderDTOList(reminders);
        return ResponseEntity.ok(reminderDTOs);
    }

    /**
     * 通过ID删除简单提醒事项
     * DELETE /api/reminders/simple/{id}
     */
    @DeleteMapping("/simple/{id}")
    public ResponseEntity<Void> deleteSimpleReminder(@PathVariable Long id) {
         try {
            // 可选：在尝试删除前检查提醒事项是否存在
            reminderService.deleteSimpleReminder(id);
            return ResponseEntity.noContent().build(); // HTTP 204
        } catch (Exception e) {
             // 处理删除过程中的潜在异常（例如，Quartz相关问题）
             throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "删除简单提醒事项失败", e);
        }
    }

    /**
     * 通过ID更新简单提醒事项
     * PUT /api/reminders/simple/{id}
     */
    @PutMapping("/simple/{id}")
    public ResponseEntity<SimpleReminderDTO> updateSimpleReminder(@PathVariable Long id, @RequestBody SimpleReminderDTO reminderDTO) {
        try {
            // 确保设置正确的ID
            reminderDTO.setId(id);
            
            // 获取当前登录用户ID
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户未登录");
            }
            
            // 验证是否存在此提醒事项
            Optional<SimpleReminder> existingReminderOpt = reminderService.getSimpleReminderById(id);
            if (!existingReminderOpt.isPresent()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到简单提醒事项");
            }
            
            // 获取现有实体
            SimpleReminder existingReminder = existingReminderOpt.get();
            
            // 用DTO中的值更新实体
            reminderMapper.updateEntityFromDTO(reminderDTO, existingReminder);
            
            // 更新提醒事项
            SimpleReminder updated = reminderService.updateSimpleReminder(existingReminder);
            
            // 转换为DTO并返回
            return ResponseEntity.ok(reminderMapper.toDTO(updated));
        } catch (ResponseStatusException e) {
            // 重新抛出已经格式化的异常
            throw e;
        } catch (Exception e) {
            // 处理更新过程中的潜在异常
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "更新简单提醒事项失败", e);
        }
    }

    // --- 复杂提醒事项的接口 ---

    /**
     * 创建新的复杂提醒事项模板
     * POST /api/reminders/complex
     */
    @PostMapping("/complex")
    public ResponseEntity<ComplexReminderDTO> createComplexReminder(@RequestBody ComplexReminderDTO reminderDTO) {
        // TODO: 添加输入对象的验证（例如，CRON语法）
         try {
            // 转换DTO为实体
            ComplexReminder complexReminder = reminderMapper.toEntity(reminderDTO);
            
            // 保存实体
            ComplexReminder created = reminderService.createComplexReminder(complexReminder);
            
            // 转换实体为DTO并返回
            return new ResponseEntity<>(reminderMapper.toDTO(created), HttpStatus.CREATED);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "创建复杂提醒事项失败", e);
        }
    }

    /**
     * 通过ID获取复杂提醒事项模板
     * GET /api/reminders/complex/{id}
     */
    @GetMapping("/complex/{id}")
    public ResponseEntity<ComplexReminderDTO> getComplexReminderById(@PathVariable Long id) {
        Optional<ComplexReminder> reminderOpt = reminderService.getComplexReminderById(id);
        
        return reminderOpt
                .map(reminderMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到复杂提醒事项模板"));
    }

    /**
     * 获取所有复杂提醒事项模板
     * GET /api/reminders/complex
     */
    @GetMapping("/complex")
    public ResponseEntity<List<ComplexReminderDTO>> getAllComplexReminders() {
        List<ComplexReminder> reminders = reminderService.getAllComplexReminders();
        List<ComplexReminderDTO> reminderDTOs = reminderMapper.toComplexReminderDTOList(reminders);
        return ResponseEntity.ok(reminderDTOs);
    }

    /**
     * 通过ID删除复杂提醒事项模板
     * DELETE /api/reminders/complex/{id}
     */
    @DeleteMapping("/complex/{id}")
    public ResponseEntity<Void> deleteComplexReminder(@PathVariable Long id) {
        try {
             // 可选：检查模板是否存在
             reminderService.deleteComplexReminder(id);
             return ResponseEntity.noContent().build(); // HTTP 204
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "删除复杂提醒事项失败", e);
        }
    }

    /**
     * 通过ID更新复杂提醒事项模板
     * PUT /api/reminders/complex/{id}
     */
    @PutMapping("/complex/{id}")
    public ResponseEntity<ComplexReminderDTO> updateComplexReminder(@PathVariable Long id, @RequestBody ComplexReminderDTO reminderDTO) {
        try {
            // 确保设置正确的ID
            reminderDTO.setId(id);
            
            // 验证是否存在此提醒事项模板
            Optional<ComplexReminder> existingReminderOpt = reminderService.getComplexReminderById(id);
            if (!existingReminderOpt.isPresent()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到复杂提醒事项模板");
            }
            
            // 获取现有实体
            ComplexReminder existingReminder = existingReminderOpt.get();
            
            // 用DTO中的值更新实体
            reminderMapper.updateEntityFromDTO(reminderDTO, existingReminder);
            
            // 保存更新后的实体
            ComplexReminder updated = reminderService.createComplexReminder(existingReminder);
            
            // 转换为DTO并返回
            return ResponseEntity.ok(reminderMapper.toDTO(updated));
        } catch (ResponseStatusException e) {
            // 重新抛出已经格式化的异常
            throw e;
        } catch (Exception e) {
            // 处理更新过程中的潜在异常
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "更新复杂提醒事项模板失败", e);
        }
    }
} 