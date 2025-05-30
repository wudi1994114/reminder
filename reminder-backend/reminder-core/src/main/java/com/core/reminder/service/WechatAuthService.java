package com.core.reminder.service;

import com.common.reminder.constant.ActivityAction;
import com.common.reminder.constant.ResourceType;
import com.common.reminder.model.AppUser;
import com.common.reminder.model.WechatUser;
import com.core.reminder.aspect.ActivityLogAspect.LogActivity;
import com.core.reminder.dto.WechatApiResponse;
import com.core.reminder.dto.WechatLoginRequest;
import com.core.reminder.dto.WechatLoginResponse;
import com.core.reminder.repository.AppUserRepository;
import com.core.reminder.repository.WechatUserRepository;
import com.core.reminder.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 微信登录服务类
 */
@Slf4j
@Service
public class WechatAuthService {

    @Autowired
    private WechatApiService wechatApiService;

    @Autowired
    private WechatUserRepository wechatUserRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserCacheService userCacheService;

    /**
     * 微信小程序登录
     * @param request 登录请求
     * @return 登录响应
     */
    @Transactional
    @LogActivity(action = ActivityAction.WECHAT_LOGIN, resourceType = ResourceType.USER, 
                description = "微信小程序登录", async = true, logParams = false, logResult = false)
    public WechatLoginResponse wechatLogin(WechatLoginRequest request) {
        try {
            // 1. 调用微信API获取openid和session_key
            WechatApiResponse apiResponse = wechatApiService.jscode2session(request.getCode());
            
            if (apiResponse.getOpenid() == null || apiResponse.getOpenid().isEmpty()) {
                throw new RuntimeException("微信登录失败：未获取到有效的openid");
            }

            // 2. 查找或创建微信用户
            Optional<WechatUser> existingWechatUser = wechatUserRepository.findByOpenid(apiResponse.getOpenid());
            
            WechatUser wechatUser;
            AppUser appUser;
            boolean isNewUser = false;

            if (existingWechatUser.isPresent()) {
                // 已存在的微信用户，更新登录信息
                wechatUser = existingWechatUser.get();
                appUser = appUserRepository.findById(wechatUser.getAppUserId())
                        .orElseThrow(() -> new RuntimeException("关联的系统用户不存在"));
                
                // 更新微信用户信息
                updateWechatUser(wechatUser, apiResponse, request);
                
                log.info("微信用户登录成功，openid: {}, 用户ID: {}", apiResponse.getOpenid(), appUser.getId());
            } else {
                // 新用户，创建系统用户和微信用户
                isNewUser = true;
                appUser = createAppUser(request);
                wechatUser = createWechatUser(appUser.getId(), apiResponse, request);
                
                log.info("新微信用户注册成功，openid: {}, 用户ID: {}", apiResponse.getOpenid(), appUser.getId());
            }

            // 3. 生成JWT token
            String accessToken = jwtTokenProvider.generateTokenFromUserDetails(appUser);

            // 4. 更新缓存
            userCacheService.refreshUserCache(appUser);

            // 5. 构建响应
            return new WechatLoginResponse(
                    accessToken,
                    appUser.getId(),
                    appUser.getNickname(),
                    appUser.getAvatarUrl(),
                    isNewUser,
                    apiResponse.getOpenid()
            );

        } catch (Exception e) {
            log.error("微信登录失败", e);
            throw new RuntimeException("微信登录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建系统用户
     */
    @LogActivity(action = ActivityAction.REGISTER, resourceType = ResourceType.USER, 
                description = "创建微信关联的系统用户", async = true, logParams = false, logResult = true)
    private AppUser createAppUser(WechatLoginRequest request) {
        AppUser appUser = new AppUser();
        
        // 生成唯一的用户名
        String username = "wx_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        appUser.setUsername(username);
        
        // 设置默认密码（微信用户不需要密码）
        appUser.setPassword("WECHAT_USER_NO_PASSWORD");
        
        // 设置用户信息
        if (request.getUserInfo() != null) {
            appUser.setNickname(request.getUserInfo().getNickName() != null ? 
                    request.getUserInfo().getNickName() : "微信用户");
            appUser.setAvatarUrl(request.getUserInfo().getAvatarUrl());
        } else {
            appUser.setNickname("微信用户");
        }
        
        // 设置默认邮箱（微信用户可能没有邮箱）
        appUser.setEmail(username + "@wechat.local");
        
        return appUserRepository.save(appUser);
    }

    /**
     * 创建微信用户
     */
    @LogActivity(action = ActivityAction.SOCIAL_ACCOUNT_BIND, resourceType = ResourceType.SOCIAL_ACCOUNT, 
                description = "创建微信用户关联", async = true, logParams = false, logResult = true)
    private WechatUser createWechatUser(Long appUserId, WechatApiResponse apiResponse, WechatLoginRequest request) {
        WechatUser wechatUser = new WechatUser();
        wechatUser.setAppUserId(appUserId);
        wechatUser.setOpenid(apiResponse.getOpenid());
        wechatUser.setUnionid(apiResponse.getUnionid());
        wechatUser.setSessionKey(apiResponse.getSessionKey());
        wechatUser.setLastLoginTime(OffsetDateTime.now());
        
        // 设置用户信息
        if (request.getUserInfo() != null) {
            WechatLoginRequest.WechatUserInfo userInfo = request.getUserInfo();
            wechatUser.setNickname(userInfo.getNickName());
            wechatUser.setAvatarUrl(userInfo.getAvatarUrl());
            wechatUser.setGender(userInfo.getGender());
            wechatUser.setCountry(userInfo.getCountry());
            wechatUser.setProvince(userInfo.getProvince());
            wechatUser.setCity(userInfo.getCity());
            wechatUser.setLanguage(userInfo.getLanguage());
        }
        
        return wechatUserRepository.save(wechatUser);
    }

    /**
     * 更新微信用户信息
     */
    @LogActivity(action = ActivityAction.PROFILE_UPDATE, resourceType = ResourceType.SOCIAL_ACCOUNT, 
                description = "更新微信用户信息", async = true, logParams = false)
    private void updateWechatUser(WechatUser wechatUser, WechatApiResponse apiResponse, WechatLoginRequest request) {
        // 更新session_key和登录时间
        wechatUser.setSessionKey(apiResponse.getSessionKey());
        wechatUser.setLastLoginTime(OffsetDateTime.now());
        
        // 更新unionid（如果有）
        if (apiResponse.getUnionid() != null) {
            wechatUser.setUnionid(apiResponse.getUnionid());
        }
        
        // 更新用户信息（如果提供）
        if (request.getUserInfo() != null) {
            WechatLoginRequest.WechatUserInfo userInfo = request.getUserInfo();
            if (userInfo.getNickName() != null) {
                wechatUser.setNickname(userInfo.getNickName());
            }
            if (userInfo.getAvatarUrl() != null) {
                wechatUser.setAvatarUrl(userInfo.getAvatarUrl());
            }
            if (userInfo.getGender() != null) {
                wechatUser.setGender(userInfo.getGender());
            }
            if (userInfo.getCountry() != null) {
                wechatUser.setCountry(userInfo.getCountry());
            }
            if (userInfo.getProvince() != null) {
                wechatUser.setProvince(userInfo.getProvince());
            }
            if (userInfo.getCity() != null) {
                wechatUser.setCity(userInfo.getCity());
            }
            if (userInfo.getLanguage() != null) {
                wechatUser.setLanguage(userInfo.getLanguage());
            }
        }
        
        wechatUserRepository.save(wechatUser);
        
        // 同时更新系统用户的头像和昵称
        if (request.getUserInfo() != null) {
            AppUser appUser = appUserRepository.findById(wechatUser.getAppUserId()).orElse(null);
            if (appUser != null) {
                boolean needUpdate = false;
                
                if (request.getUserInfo().getNickName() != null && 
                    !request.getUserInfo().getNickName().equals(appUser.getNickname())) {
                    appUser.setNickname(request.getUserInfo().getNickName());
                    needUpdate = true;
                }
                
                if (request.getUserInfo().getAvatarUrl() != null && 
                    !request.getUserInfo().getAvatarUrl().equals(appUser.getAvatarUrl())) {
                    appUser.setAvatarUrl(request.getUserInfo().getAvatarUrl());
                    needUpdate = true;
                }
                
                if (needUpdate) {
                    appUser.setUpdatedAt(OffsetDateTime.now());
                    appUserRepository.save(appUser);
                }
            }
        }
    }
} 