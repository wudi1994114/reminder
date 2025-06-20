package com.core.reminder.controller;

// 导入所需的实体类和重构后的服务（或其接口）
import com.core.reminder.dto.ComplexReminderDTO;
import com.core.reminder.dto.SimpleReminderDTO;
import com.common.reminder.dto.UserProfileDto;
import com.common.reminder.model.ComplexReminder;
import com.common.reminder.model.SimpleReminder;
import com.core.reminder.service.ReminderEventServiceImpl; // 暂时使用具体类，后续最好使用接口
import com.core.reminder.utils.ReminderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reminders") // 更新后的基础路径
// @CrossOrigin(origins = "http://wwmty.cn:5173") // 全局配置CORS，移除此行
// 考虑重命名为ReminderController
public class ReminderEventController {

    private static final Logger log = LoggerFactory.getLogger(ReminderEventController.class);

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
    public ResponseEntity<SimpleReminderDTO> createSimpleReminder(
            @RequestBody SimpleReminderDTO reminderDTO,
            @RequestAttribute("currentUser") UserProfileDto userProfileDto) {
        
        try {
            log.info("接收到创建提醒请求，原始DTO: {}", reminderDTO);
            log.info("当前用户信息: {}", userProfileDto);
            
            // 自动设置用户ID
            Long currentUserId = userProfileDto.getId();
            reminderDTO.setFromUserId(currentUserId);
            reminderDTO.setToUserId(currentUserId);
            
            log.info("设置用户ID后的DTO: {}", reminderDTO);

            // 转换DTO为实体
            SimpleReminder simpleReminder = reminderMapper.toEntity(reminderDTO);
            log.info("转换后的实体: fromUserId={}, toUserId={}, title={}", 
                    simpleReminder.getFromUserId(), simpleReminder.getToUserId(), simpleReminder.getTitle());

            // 保存实体
            SimpleReminder created = reminderService.createSimpleReminder(simpleReminder);

            // 转换实体为DTO并返回
            return new ResponseEntity<>(reminderMapper.toDTO(created), HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("创建简单提醒事项失败", e);
            // 处理创建/调度过程中的潜在异常
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "创建简单提醒事项失败", e);
        }
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
     * 支持可选的年月参数，例如：/api/reminders/simple?year=2023&month=12
     */
    @GetMapping("/simple")
    public ResponseEntity<List<SimpleReminderDTO>> getAllSimpleReminders(
            @RequestAttribute("currentUser") UserProfileDto userProfileDto,
            @RequestParam(required = true) Integer year,
            @RequestParam(required = true) Integer month,
            HttpServletRequest request) {

        log.info("当前登录用户信息: {}", userProfileDto);
        Long userId = userProfileDto.getId(); // 获取用户ID

        // 年和月现在是必需的，直接按月查询
        List<SimpleReminder> reminders = reminderService.getSimpleRemindersByYearMonthAndUser(year, month, userId);
        log.info("用户 {} 查询 {}-{} 月份的所有简单提醒，共 {} 条", userId, year, month, reminders.size());

        List<SimpleReminderDTO> reminderDTOs = reminderMapper.toSimpleReminderDTOList(reminders);
        return ResponseEntity.ok(reminderDTOs);
    }

    /**
     * 获取即将到来的提醒事项(最近10个)
     * GET /api/reminders/upcoming
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<SimpleReminderDTO>> getUpcomingReminders(@RequestAttribute("currentUser") UserProfileDto userProfileDto) {
        Long userId = userProfileDto.getId(); // 获取用户ID
        log.info("用户 {} 查询即将到来的提醒事项", userId);
        List<SimpleReminder> reminders = reminderService.getUpcomingReminders(userId); // 传递userId
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
    public ResponseEntity<SimpleReminderDTO> updateSimpleReminder(
            @PathVariable Long id,
            @RequestBody SimpleReminderDTO reminderDTO,
            @RequestAttribute("currentUser") UserProfileDto userProfileDto) {
        try {
            // 确保设置正确的ID
            reminderDTO.setId(id);

            // 验证是否存在此提醒事项
            Optional<SimpleReminder> existingReminderOpt = reminderService.getSimpleReminderById(id);
            if (!existingReminderOpt.isPresent()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到简单提醒事项");
            }

            // 获取现有实体
            SimpleReminder existingReminder = existingReminderOpt.get();
            
            // 验证用户权限：只有创建者或接收者可以修改
            Long userId = userProfileDto.getId();
            if (!existingReminder.getFromUserId().equals(userId) && !existingReminder.getToUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限修改此提醒");
            }

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
    public ResponseEntity<ComplexReminderDTO> createComplexReminder(
            @RequestBody ComplexReminderDTO reminderDTO,
            @RequestAttribute("currentUser") UserProfileDto userProfileDto) {
        // TODO: 添加输入对象的验证（例如，CRON语法）
        try {
            // 转换DTO为实体
            ComplexReminder complexReminder = reminderMapper.toEntity(reminderDTO);
            complexReminder.setToUserId(userProfileDto.getId());

            // 设置用户信息
            Long userId = userProfileDto.getId();
            complexReminder.setFromUserId(userId);
            // 如果DTO中没有指定toUserId，则默认为当前用户
            if (complexReminder.getToUserId() == null) {
                complexReminder.setToUserId(userId);
            }

            // 使用事务方法创建复杂提醒并生成简单任务(默认三个月)
            ComplexReminder created = reminderService.createComplexReminderWithSimpleReminders(complexReminder, 3);

            // 转换实体为DTO并返回
            return new ResponseEntity<>(reminderMapper.toDTO(created), HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("创建复杂提醒事项失败", e);
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
    public ResponseEntity<List<ComplexReminderDTO>> getAllComplexReminders(
            @RequestAttribute("currentUser") UserProfileDto userProfileDto) {
        Long userId = userProfileDto.getId();
        List<ComplexReminder> reminders = reminderService.getComplexRemindersByFromUser(userId);
        List<ComplexReminderDTO> reminderDTOs = reminderMapper.toComplexReminderDTOList(reminders);
        return ResponseEntity.ok(reminderDTOs);
    }

    /**
     * 通过ID删除复杂提醒事项模板
     * DELETE /api/reminders/complex/{id}
     */
    @DeleteMapping("/complex/{id}")
    public ResponseEntity<Void> deleteComplexReminder(
            @PathVariable Long id,
            @RequestAttribute("currentUser") UserProfileDto userProfileDto) {
        try {
            // 验证提醒是否存在
            Optional<ComplexReminder> existingReminderOpt = reminderService.getComplexReminderById(id);
            if (!existingReminderOpt.isPresent()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到复杂提醒事项模板");
            }
            
            // 验证用户权限：只有创建者可以删除
            ComplexReminder existingReminder = existingReminderOpt.get();
            Long userId = userProfileDto.getId();
            if (!existingReminder.getFromUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限删除此提醒");
            }

            // 使用集成方法删除复杂提醒及其关联的所有简单任务
            int deletedCount = reminderService.deleteComplexReminderWithRelatedSimpleReminders(id);
            log.info("已删除复杂提醒ID: {} 及其关联的 {} 个简单任务", id, deletedCount);

            return ResponseEntity.noContent().build(); // HTTP 204
        } catch (ResponseStatusException e) {
            // 重新抛出已经格式化的异常
            throw e;
        } catch (Exception e) {
            log.error("删除复杂提醒事项失败", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "删除复杂提醒事项失败", e);
        }
    }

    /**
     * 通过ID更新复杂提醒事项模板
     * PUT /api/reminders/complex/{id}
     */
    @PutMapping("/complex/{id}")
    public ResponseEntity<ComplexReminderDTO> updateComplexReminder(
            @PathVariable Long id,
            @RequestBody ComplexReminderDTO reminderDTO,
            @RequestAttribute("currentUser") UserProfileDto userProfileDto) {
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
            
            // 验证用户权限：只有创建者可以修改
            Long userId = userProfileDto.getId();
            if (!existingReminder.getFromUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限修改此提醒");
            }

            // 用DTO中的值更新实体
            reminderMapper.updateEntityFromDTO(reminderDTO, existingReminder);

            // 使用事务方法更新复杂提醒并生成简单任务(默认三个月)
            // 此方法会先删除已有的简单任务，保证数据一致性
            ComplexReminder updated = reminderService.updateComplexReminderWithSimpleReminders(existingReminder, 3);

            // 转换为DTO并返回
            return ResponseEntity.ok(reminderMapper.toDTO(updated));
        } catch (ResponseStatusException e) {
            // 重新抛出已经格式化的异常
            throw e;
        } catch (Exception e) {
            log.error("更新复杂提醒事项模板失败", e);
            // 处理更新过程中的潜在异常
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "更新复杂提醒事项模板失败", e);
        }
    }

}