package com.common.reminder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.OffsetDateTime;

/**
 * 微信用户信息实体类
 * 用于存储微信小程序用户的相关信息
 */
@Entity
@Table(name = "wechat_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WechatUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的系统用户ID
     */
    @Column(name = "app_user_id", nullable = false)
    private Long appUserId;

    /**
     * 微信小程序的openid，唯一标识用户
     */
    @Column(name = "openid", unique = true, nullable = false, length = 100)
    private String openid;

    /**
     * 微信unionid，用于关联同一微信开放平台下的应用
     */
    @Column(name = "unionid", length = 100)
    private String unionid;

    /**
     * 微信用户昵称
     */
    @Column(name = "nickname", length = 100)
    private String nickname;

    /**
     * 微信用户头像URL
     */
    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    /**
     * 微信用户性别：0-未知，1-男，2-女
     */
    @Column(name = "gender")
    private Integer gender;

    /**
     * 微信用户所在国家
     */
    @Column(name = "country", length = 50)
    private String country;

    /**
     * 微信用户所在省份
     */
    @Column(name = "province", length = 50)
    private String province;

    /**
     * 微信用户所在城市
     */
    @Column(name = "city", length = 50)
    private String city;

    /**
     * 微信用户语言
     */
    @Column(name = "language", length = 20)
    private String language;

    /**
     * 会话密钥
     */
    @Column(name = "session_key", length = 100)
    private String sessionKey;

    /**
     * 最后登录时间
     */
    @Column(name = "last_login_time", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastLoginTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;
} 