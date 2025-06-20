package com.core.reminder.repository;

import com.common.reminder.model.UserFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户反馈数据访问层接口
 */
@Repository
public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {
    
    // 基础的CRUD操作由JpaRepository提供
    // 如果需要自定义查询方法，可以在这里添加
}
