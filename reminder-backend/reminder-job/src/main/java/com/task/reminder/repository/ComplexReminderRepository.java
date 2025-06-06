package com.task.reminder.repository;

import com.common.reminder.model.ComplexReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplexReminderRepository extends JpaRepository<ComplexReminder, Long> {

    // 可能需要的查询方法示例
    List<ComplexReminder> findByFromUserId(Long fromUserId);

    List<ComplexReminder> findByToUserId(Long toUserId);
    
    // 查询lastGeneratedYm小于指定值的所有记录
    List<ComplexReminder> findByLastGeneratedYmLessThanOrLastGeneratedYmIsNull(Integer targetYearMonth);

    // 可以根据需要添加更多查询方法，例如按 cronExpression 查询等
} 