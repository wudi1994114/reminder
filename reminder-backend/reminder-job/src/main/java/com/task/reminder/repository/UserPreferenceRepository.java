package com.task.reminder.repository;

import com.common.reminder.model.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户偏好设置数据访问层 - Job模块专用
 */
@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    
    /**
     * 根据用户ID和key查找偏好设置
     * @param userId 用户ID
     * @param key 偏好设置键名
     * @return 偏好设置
     */
    Optional<UserPreference> findByUserIdAndKey(Long userId, String key);
} 