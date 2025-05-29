package com.core.reminder.utils;

import com.core.reminder.dto.ComplexReminderDTO;
import com.core.reminder.dto.SimpleReminderDTO;
import com.common.reminder.model.ComplexReminder;
import com.common.reminder.model.SimpleReminder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 提醒事项实体对象和DTO对象之间的转换工具类
 */
@Component
public class ReminderMapper {

    /**
     * 将数据库实体对象ComplexReminder转换为DTO对象ComplexReminderDTO
     */
    public ComplexReminderDTO toDTO(ComplexReminder entity) {
        if (entity == null) {
            return null;
        }
        
        return ComplexReminderDTO.builder()
                .id(entity.getId())
                .fromUserId(entity.getFromUserId())
                .toUserId(entity.getToUserId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .cronExpression(entity.getCronExpression())
                .reminderType(entity.getReminderType())
                .validFrom(entity.getValidFrom())
                .validUntil(entity.getValidUntil())
                .maxExecutions(entity.getMaxExecutions())
                .lastGeneratedYm(entity.getLastGeneratedYm())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    /**
     * 将DTO对象ComplexReminderDTO转换为数据库实体对象ComplexReminder
     * 注意：如果是更新操作，应先从数据库中获取原实体，再用DTO中的值更新
     */
    public ComplexReminder toEntity(ComplexReminderDTO dto) {
        if (dto == null) {
            return null;
        }
        
        ComplexReminder entity = new ComplexReminder();
        entity.setId(dto.getId());
        entity.setFromUserId(dto.getFromUserId());
        entity.setToUserId(dto.getToUserId());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setCronExpression(dto.getCronExpression());
        entity.setReminderType(dto.getReminderType());
        
        // 处理可选的日期字段：空字符串转换为null
        entity.setValidFrom(dto.getValidFrom());
        entity.setValidUntil(dto.getValidUntil());
        
        // 处理可选的数字字段：确保null值正确传递
        entity.setMaxExecutions(dto.getMaxExecutions());
        
        entity.setLastGeneratedYm(dto.getLastGeneratedYm());
        // 注意：创建时间和更新时间通常由JPA自动管理，但在特殊情况下也可以设置
        // entity.setCreatedAt(dto.getCreatedAt());
        // entity.setUpdatedAt(dto.getUpdatedAt());
        
        return entity;
    }
    
    /**
     * 将数据库实体对象SimpleReminder转换为DTO对象SimpleReminderDTO
     */
    public SimpleReminderDTO toDTO(SimpleReminder entity) {
        if (entity == null) {
            return null;
        }
        
        return SimpleReminderDTO.builder()
                .id(entity.getId())
                .fromUserId(entity.getFromUserId())
                .toUserId(entity.getToUserId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .eventTime(entity.getEventTime())
                .reminderType(entity.getReminderType())
                .originatingComplexReminderId(entity.getOriginatingComplexReminderId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    /**
     * 将DTO对象SimpleReminderDTO转换为数据库实体对象SimpleReminder
     * 注意：如果是更新操作，应先从数据库中获取原实体，再用DTO中的值更新
     */
    public SimpleReminder toEntity(SimpleReminderDTO dto) {
        if (dto == null) {
            return null;
        }
        
        SimpleReminder entity = new SimpleReminder();
        entity.setId(dto.getId());
        entity.setFromUserId(dto.getFromUserId());
        entity.setToUserId(dto.getToUserId());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setEventTime(dto.getEventTime());
        entity.setReminderType(dto.getReminderType());
        entity.setOriginatingComplexReminderId(dto.getOriginatingComplexReminderId());
        // 注意：创建时间和更新时间通常由JPA自动管理，但在特殊情况下也可以设置
        // entity.setCreatedAt(dto.getCreatedAt());
        // entity.setUpdatedAt(dto.getUpdatedAt());
        
        return entity;
    }
    
    /**
     * 批量转换复杂提醒实体对象列表为DTO对象列表
     */
    public List<ComplexReminderDTO> toComplexReminderDTOList(List<ComplexReminder> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 批量转换简单提醒实体对象列表为DTO对象列表
     */
    public List<SimpleReminderDTO> toSimpleReminderDTOList(List<SimpleReminder> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 更新已有的实体对象，使用DTO中的值
     * 用于编辑操作，保留数据库实体的创建时间等不应被修改的属性
     */
    public void updateEntityFromDTO(ComplexReminderDTO dto, ComplexReminder existingEntity) {
        if (dto == null || existingEntity == null) {
            return;
        }
        
        // 只更新业务上允许修改的字段
        existingEntity.setFromUserId(dto.getFromUserId());
        existingEntity.setToUserId(dto.getToUserId());
        existingEntity.setTitle(dto.getTitle());
        existingEntity.setDescription(dto.getDescription());
        existingEntity.setCronExpression(dto.getCronExpression());
        existingEntity.setReminderType(dto.getReminderType());
        
        // 处理可选的日期字段：允许null值
        existingEntity.setValidFrom(dto.getValidFrom());
        existingEntity.setValidUntil(dto.getValidUntil());
        
        // 处理可选的数字字段：允许null值
        existingEntity.setMaxExecutions(dto.getMaxExecutions());
        
        // 不更新lastGeneratedYm，这是系统内部管理的字段
        // 不更新创建时间和更新时间，这些由JPA自动管理
    }
    
    /**
     * 更新已有的实体对象，使用DTO中的值
     * 用于编辑操作，保留数据库实体的创建时间等不应被修改的属性
     */
    public void updateEntityFromDTO(SimpleReminderDTO dto, SimpleReminder existingEntity) {
        if (dto == null || existingEntity == null) {
            return;
        }
        
        // 只更新业务上允许修改的字段
        existingEntity.setFromUserId(dto.getFromUserId());
        existingEntity.setToUserId(dto.getToUserId());
        existingEntity.setTitle(dto.getTitle());
        existingEntity.setDescription(dto.getDescription());
        existingEntity.setEventTime(dto.getEventTime());
        existingEntity.setReminderType(dto.getReminderType());
        // 通常不应修改originatingComplexReminderId，因为这是系统关联关系
        // 不更新创建时间和更新时间，这些由JPA自动管理
    }
} 