package com.core.reminder.repository;

import com.common.reminder.model.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户偏好设置数据访问层 - 键值对存储模式
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
    
    /**
     * 根据用户ID查找所有偏好设置
     * @param userId 用户ID
     * @return 用户所有偏好设置列表
     */
    List<UserPreference> findByUserId(Long userId);
    
    /**
     * 根据用户ID和多个key查找偏好设置
     * @param userId 用户ID
     * @param keys 偏好设置键名列表
     * @return 匹配的偏好设置列表
     */
    List<UserPreference> findByUserIdAndKeyIn(Long userId, List<String> keys);
    
    /**
     * 检查用户指定key的偏好设置是否存在
     * @param userId 用户ID
     * @param key 偏好设置键名
     * @return 是否存在
     */
    boolean existsByUserIdAndKey(Long userId, String key);
    
    /**
     * 根据用户ID删除所有偏好设置
     * @param userId 用户ID
     */
    @Modifying
    @Query("DELETE FROM UserPreference up WHERE up.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户ID和key删除偏好设置
     * @param userId 用户ID
     * @param key 偏好设置键名
     */
    @Modifying
    @Query("DELETE FROM UserPreference up WHERE up.userId = :userId AND up.key = :key")
    void deleteByUserIdAndKey(@Param("userId") Long userId, @Param("key") String key);
    
    /**
     * 根据用户ID和多个key删除偏好设置
     * @param userId 用户ID
     * @param keys 偏好设置键名列表
     */
    @Modifying
    @Query("DELETE FROM UserPreference up WHERE up.userId = :userId AND up.key IN :keys")
    void deleteByUserIdAndKeyIn(@Param("userId") Long userId, @Param("keys") List<String> keys);
    
    /**
     * 统计用户的偏好设置数量
     * @param userId 用户ID
     * @return 偏好设置数量
     */
    @Query("SELECT COUNT(up) FROM UserPreference up WHERE up.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    /**
     * 根据key查找所有用户的该项偏好设置
     * @param key 偏好设置键名
     * @return 所有用户的该项偏好设置
     */
    List<UserPreference> findByKey(String key);
} 