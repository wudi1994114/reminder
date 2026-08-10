package com.core.reminder.repository;

import com.common.reminder.model.WechatUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 微信用户数据访问层接口
 */
@Repository
public interface WechatUserRepository extends JpaRepository<WechatUser, Long> {

    /**
     * 根据openid查找微信用户
     * @param openid 微信openid
     * @return 微信用户信息
     */
    Optional<WechatUser> findByOpenid(String openid);

    /**
     * 根据系统用户ID查找微信用户
     * @param appUserId 系统用户ID
     * @return 微信用户信息
     */
    Optional<WechatUser> findByAppUserId(Long appUserId);

    /**
     * 根据unionid查找微信用户
     * @param unionid 微信unionid
     * @return 微信用户信息
     */
    Optional<WechatUser> findByUnionid(String unionid);

    /**
     * 检查openid是否存在
     * @param openid 微信openid
     * @return 是否存在
     */
    boolean existsByOpenid(String openid);
} 