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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            
            log.info("微信API响应详情 - openid: {}, unionid: {}, session_key: {}", 
                    apiResponse.getOpenid(), 
                    apiResponse.getUnionid(), 
                    apiResponse.getSessionKey() != null ? "***已获取***" : "未获取");
            
            if (apiResponse.getOpenid() == null || apiResponse.getOpenid().isEmpty()) {
                throw new RuntimeException("微信登录失败：未获取到有效的openid");
            }
            
            // 检查unionid状态
            if (apiResponse.getUnionid() != null && !apiResponse.getUnionid().isEmpty()) {
                log.info("✅ 获取到unionid: {}", apiResponse.getUnionid());
            } else {
                log.warn("⚠️ 未获取到unionid，这是正常情况。只有绑定微信开放平台或满足特定条件才会有unionid");
            }

            // 2. 查找或创建微信用户
            Optional<WechatUser> existingWechatUser = wechatUserRepository.findByOpenid(apiResponse.getOpenid());
            
            WechatUser wechatUser;
            AppUser appUser;
            boolean isNewUser = false;

            if (existingWechatUser.isPresent()) {
                // 已存在的微信用户 - 纯登录，不更新用户资料
                wechatUser = existingWechatUser.get();
                appUser = appUserRepository.findById(wechatUser.getAppUserId())
                        .orElseThrow(() -> new RuntimeException("关联的系统用户不存在"));
                
                // 只更新登录相关信息，不更新用户资料
                updateWechatUserLoginInfo(wechatUser, apiResponse);
                
                log.info("微信用户登录成功，openid: {}, 用户ID: {} - 保护现有用户资料", apiResponse.getOpenid(), appUser.getId());
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
            WechatLoginResponse response = new WechatLoginResponse(
                    accessToken,
                    appUser.getId(),
                    appUser.getNickname(),
                    appUser.getAvatarUrl(),
                    isNewUser,
                    apiResponse.getOpenid()
            );
            // 设置unionid
            response.setUnionid(wechatUser.getUnionid());
            
            log.info("微信登录响应构建完成 - 用户ID: {}, openid: {}, unionid: {}", 
                    response.getUserId(), response.getOpenid(), response.getUnionid());
            
            return response;

        } catch (Exception e) {
            log.error("微信登录失败", e);
            
            // 安全地处理异常消息，避免空指针
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                errorMessage = "微信登录过程中发生未知错误";
            }
            
            // 抛出包装后的异常，确保消息不为null
            throw new RuntimeException("微信登录失败: " + errorMessage, e);
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
        
        // 使用默认的用户信息，不依赖前端传递
        appUser.setNickname("微信用户");
        appUser.setAvatarUrl("https://thirdwx.qlogo.cn/mmopen/vi_32/POgEwh4mIHO4nibH0KlMECNjjGxQUq24ZEaGT4poC6icRiccVGKSyXwibcPq4BWmiaIGuG1icwxaQX6grC9VemZoJ8rg/132");
        
        log.info("👤 [AppUser存储] 新用户使用默认昵称: 微信用户");
        log.info("🖼️ [AppUser存储] 新用户使用默认头像");
        
        // 设置默认邮箱（微信用户可能没有邮箱）
        appUser.setEmail("");
        
        AppUser savedUser = appUserRepository.save(appUser);
        log.info("✅ [AppUser存储] 用户创建成功 - ID: {}, 昵称: {}, 头像: {}", 
                savedUser.getId(), 
                savedUser.getNickname(), 
                savedUser.getAvatarUrl() != null ? "已设置" : "未设置");
        
        return savedUser;
    }
    
    /**
     * 转换性别数值为字符串
     */
    private String convertGenderToString(Integer gender) {
        if (gender == null) return null;
        switch (gender) {
            case 1: return "男";
            case 2: return "女";
            default: return "未知";
        }
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
        
        // 记录unionid存储状态
        if (apiResponse.getUnionid() != null && !apiResponse.getUnionid().isEmpty()) {
            log.info("💾 新用户创建：将unionid [{}] 存储到数据库", apiResponse.getUnionid());
        } else {
            log.info("💾 新用户创建：unionid为空，存储null值到数据库");
        }
        
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
     * 只更新登录相关信息，不更新用户资料
     */
    @LogActivity(action = ActivityAction.LOGIN, resourceType = ResourceType.SOCIAL_ACCOUNT, 
                description = "更新微信用户登录信息", async = true, logParams = false)
    private void updateWechatUserLoginInfo(WechatUser wechatUser, WechatApiResponse apiResponse) {
        // 更新session_key和登录时间
        wechatUser.setSessionKey(apiResponse.getSessionKey());
        wechatUser.setLastLoginTime(OffsetDateTime.now());
        
        // 更新unionid（如果有）
        if (apiResponse.getUnionid() != null && !apiResponse.getUnionid().isEmpty()) {
            if (!apiResponse.getUnionid().equals(wechatUser.getUnionid())) {
                log.info("🔄 更新unionid：从 [{}] 更新为 [{}]", wechatUser.getUnionid(), apiResponse.getUnionid());
                wechatUser.setUnionid(apiResponse.getUnionid());
            } else {
                log.debug("✅ unionid无变化：{}", apiResponse.getUnionid());
            }
        } else {
            if (wechatUser.getUnionid() != null) {
                log.warn("⚠️ 注意：之前有unionid [{}]，但本次登录未获取到unionid", wechatUser.getUnionid());
            } else {
                log.debug("ℹ️ unionid保持为空");
            }
        }
        
        wechatUserRepository.save(wechatUser);
        log.info("✅ 登录信息更新完成，不修改用户资料");
    }

    /**
     * 更新微信用户信息（已废弃 - 仅用于注册时）
     */
    @LogActivity(action = ActivityAction.PROFILE_UPDATE, resourceType = ResourceType.SOCIAL_ACCOUNT, 
                description = "更新微信用户信息", async = true, logParams = false)
    private void updateWechatUser(WechatUser wechatUser, WechatApiResponse apiResponse, WechatLoginRequest request) {
        // 更新session_key和登录时间
        wechatUser.setSessionKey(apiResponse.getSessionKey());
        wechatUser.setLastLoginTime(OffsetDateTime.now());
        
        // 更新unionid（如果有）
        if (apiResponse.getUnionid() != null && !apiResponse.getUnionid().isEmpty()) {
            if (!apiResponse.getUnionid().equals(wechatUser.getUnionid())) {
                log.info("🔄 更新unionid：从 [{}] 更新为 [{}]", wechatUser.getUnionid(), apiResponse.getUnionid());
                wechatUser.setUnionid(apiResponse.getUnionid());
            } else {
                log.debug("✅ unionid无变化：{}", apiResponse.getUnionid());
            }
        } else {
            if (wechatUser.getUnionid() != null) {
                log.warn("⚠️ 注意：之前有unionid [{}]，但本次登录未获取到unionid", wechatUser.getUnionid());
            } else {
                log.debug("ℹ️ unionid保持为空");
            }
        }
        
        // 更新微信用户表的用户信息（辅助存储）
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
        
        // 🎯 重点：同时更新系统用户的头像和昵称（AppUser表为主要存储）
        if (request.getUserInfo() != null) {
            AppUser appUser = appUserRepository.findById(wechatUser.getAppUserId()).orElse(null);
            if (appUser != null) {
                boolean needUpdate = false;
                WechatLoginRequest.WechatUserInfo userInfo = request.getUserInfo();
                
                // 更新昵称
                if (userInfo.getNickName() != null && !userInfo.getNickName().trim().isEmpty()) {
                    if (!userInfo.getNickName().equals(appUser.getNickname())) {
                        log.info("🔄 [AppUser更新] 昵称从 [{}] 更新为 [{}]", 
                                appUser.getNickname(), userInfo.getNickName());
                        appUser.setNickname(userInfo.getNickName());
                        needUpdate = true;
                    } else {
                        log.debug("👤 [AppUser更新] 昵称无变化: {}", userInfo.getNickName());
                    }
                }
                
                // 更新头像
                if (userInfo.getAvatarUrl() != null && !userInfo.getAvatarUrl().trim().isEmpty()) {
                    if (!userInfo.getAvatarUrl().equals(appUser.getAvatarUrl())) {
                        log.info("🔄 [AppUser更新] 头像从 [{}] 更新为 [{}]", 
                                appUser.getAvatarUrl() != null ? "已设置" : "未设置", 
                                "新头像链接");
                        appUser.setAvatarUrl(userInfo.getAvatarUrl());
                        needUpdate = true;
                    } else {
                        log.debug("🖼️ [AppUser更新] 头像无变化");
                    }
                }
                
                // 更新性别
                if (userInfo.getGender() != null && !userInfo.getGender().equals(wechatUser.getGender())) {
                    log.info("🔄 [AppUser更新] 性别从 [{}] 更新为 [{}]", 
                            wechatUser.getGender(), userInfo.getGender());
                    wechatUser.setGender(userInfo.getGender());
                    needUpdate = true;
                }
                
                if (needUpdate) {
                    appUser.setUpdatedAt(OffsetDateTime.now());
                    AppUser updatedUser = appUserRepository.save(appUser);
                    log.info("✅ [AppUser更新] 用户信息更新成功 - ID: {}, 昵称: {}, 头像: {}, 性别: {}", 
                            updatedUser.getId(), 
                            updatedUser.getNickname(), 
                            updatedUser.getAvatarUrl() != null ? "已设置" : "未设置",
                            updatedUser.getGender());
                } else {
                    log.debug("ℹ️ [AppUser更新] 用户信息无变化，跳过更新");
                }
            } else {
                log.error("❌ [AppUser更新] 未找到关联的系统用户，appUserId: {}", wechatUser.getAppUserId());
            }
        } else {
            log.debug("ℹ️ [AppUser更新] 请求中无用户信息，跳过AppUser更新");
        }
    }

    /**
     * 获取unionid统计信息（调试用）
     * @return unionid统计数据
     */
    public Map<String, Object> getUnionidStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 获取所有微信用户
            List<WechatUser> allWechatUsers = wechatUserRepository.findAll();
            
            int totalUsers = allWechatUsers.size();
            int usersWithUnionid = 0;
            int usersWithoutUnionid = 0;
            
            List<Map<String, Object>> userDetails = new ArrayList<>();
            
            for (WechatUser user : allWechatUsers) {
                if (user.getUnionid() != null && !user.getUnionid().isEmpty()) {
                    usersWithUnionid++;
                } else {
                    usersWithoutUnionid++;
                }
                
                // 详细信息（隐藏敏感数据）
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("appUserId", user.getAppUserId());
                userInfo.put("openid", maskSensitiveData(user.getOpenid()));
                userInfo.put("unionid", user.getUnionid() != null ? maskSensitiveData(user.getUnionid()) : null);
                userInfo.put("hasUnionid", user.getUnionid() != null && !user.getUnionid().isEmpty());
                userInfo.put("nickname", user.getNickname());
                userInfo.put("lastLoginTime", user.getLastLoginTime());
                userInfo.put("createdAt", user.getCreatedAt());
                
                userDetails.add(userInfo);
            }
            
            stats.put("totalUsers", totalUsers);
            stats.put("usersWithUnionid", usersWithUnionid);
            stats.put("usersWithoutUnionid", usersWithoutUnionid);
            stats.put("unionidCoverageRate", totalUsers > 0 ? 
                    String.format("%.2f%%", (double) usersWithUnionid / totalUsers * 100) : "0%");
            stats.put("userDetails", userDetails);
            stats.put("timestamp", OffsetDateTime.now());
            
            // 解释说明
            stats.put("explanation", 
                    "微信小程序默认不返回unionid。只有当小程序绑定了微信开放平台账号且用户满足特定条件时才会有unionid。");
            
        } catch (Exception e) {
            log.error("获取unionid统计失败", e);
            stats.put("error", "获取统计失败：" + e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 掩码敏感数据
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.length() <= 8) {
            return data;
        }
        return data.substring(0, 4) + "****" + data.substring(data.length() - 4);
    }

    /**
     * 更新微信用户信息到AppUser表（主要存储）
     * @param appUserId 系统用户ID
     * @param userInfo 微信用户信息
     * @return 是否更新成功
     */
    @LogActivity(action = ActivityAction.PROFILE_UPDATE, resourceType = ResourceType.USER, 
                description = "更新微信用户信息到AppUser表", async = true, logParams = false)
    public boolean updateWechatUserInfoToAppUser(Long appUserId, WechatLoginRequest.WechatUserInfo userInfo) {
        try {
            AppUser appUser = appUserRepository.findById(appUserId).orElse(null);
            if (appUser == null) {
                log.error("❌ [AppUser更新] 未找到用户，ID: {}", appUserId);
                return false;
            }
            
            boolean hasUpdates = false;
            
            // 更新昵称
            if (userInfo.getNickName() != null && !userInfo.getNickName().trim().isEmpty()) {
                if (!userInfo.getNickName().equals(appUser.getNickname())) {
                    log.info("🔄 [手动更新] 昵称从 [{}] 更新为 [{}]", 
                            appUser.getNickname(), userInfo.getNickName());
                    appUser.setNickname(userInfo.getNickName());
                    hasUpdates = true;
                }
            }
            
            // 更新头像
            if (userInfo.getAvatarUrl() != null && !userInfo.getAvatarUrl().trim().isEmpty()) {
                if (!userInfo.getAvatarUrl().equals(appUser.getAvatarUrl())) {
                    log.info("🔄 [手动更新] 头像更新: {}", userInfo.getAvatarUrl());
                    appUser.setAvatarUrl(userInfo.getAvatarUrl());
                    hasUpdates = true;
                }
            }
            
            // 更新性别
            if (userInfo.getGender() != null) {
                String genderStr = convertGenderToString(userInfo.getGender());
                if (appUser.getGender() == null || !appUser.getGender().equals(genderStr)) {
                    log.info("🔄 [手动更新] 性别从 [{}] 更新为 [{}]",
                            appUser.getGender(), genderStr);
                    appUser.setGender(genderStr);
                    hasUpdates = true;
                }
            }
            
            if (hasUpdates) {
                appUser.setUpdatedAt(OffsetDateTime.now());
                AppUser updatedUser = appUserRepository.save(appUser);
                log.info("✅ [手动更新] 用户信息更新成功 - ID: {}, 昵称: {}, 头像: {}, 性别: {}", 
                        updatedUser.getId(), 
                        updatedUser.getNickname(), 
                        updatedUser.getAvatarUrl() != null ? "已设置" : "未设置",
                        updatedUser.getGender());
                return true;
            } else {
                log.debug("ℹ️ [手动更新] 用户信息无变化");
                return true;
            }
            
        } catch (Exception e) {
            log.error("❌ [手动更新] 更新用户信息失败", e);
            return false;
        }
    }

}
