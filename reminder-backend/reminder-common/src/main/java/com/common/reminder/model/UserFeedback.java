package com.common.reminder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.OffsetDateTime;

/**
 * 用户反馈实体类
 * 用于存储用户提交的反馈信息
 */
@Entity
@Table(name = "user_feedback")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID，可为空（支持匿名反馈）
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 用户邮箱，可为空
     */
    @Column(length = 255)
    private String email;

    /**
     * 用户反馈内容
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * 反馈创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createTime;
}
