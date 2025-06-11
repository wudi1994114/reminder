package com.core.reminder.repository;

import com.common.reminder.model.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户偏好设置数据访问层
 */
@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    
    /**
     * 根据用户ID查找用户偏好设置
     * @param userId 用户ID
     * @return 用户偏好设置
     */
    Optional<UserPreference> findByUserId(Long userId);
    
    /**
     * 检查用户是否已有偏好设置
     * @param userId 用户ID
     * @return 是否存在
     */
    boolean existsByUserId(Long userId);
    
    /**
     * 根据用户ID删除偏好设置
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);
} 