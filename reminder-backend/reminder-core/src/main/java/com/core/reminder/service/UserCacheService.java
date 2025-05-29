package com.core.reminder.service;

import com.common.reminder.dto.UserProfileDto;
import com.common.reminder.model.AppUser;
import com.core.reminder.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class UserCacheService {
    private static final Logger log = LoggerFactory.getLogger(UserCacheService.class);
    private static final String USER_USERNAME_CACHE_KEY_PREFIX = "user:username:profile:"; // 修改原来的键前缀以区分
    private static final String USER_ID_CACHE_KEY_PREFIX = "user:id:profile:"; // 新增基于ID的键前缀
    private static final long USER_CACHE_TTL = 30; // 缓存有效期30分钟

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AppUserRepository appUserRepository;

    /**
     * 根据用户名获取用户信息，优先从缓存获取，缓存未命中则从数据库获取并缓存
     * @param username 用户名
     * @return 用户信息DTO
     */
    public UserProfileDto getUserProfileByUsername(String username) {
        String cacheKey = USER_USERNAME_CACHE_KEY_PREFIX + username;
        
        UserProfileDto cachedUser = (UserProfileDto) redisTemplate.opsForValue().get(cacheKey);
        if (cachedUser != null) {
            log.debug("缓存命中 (username): 用户[{}]的信息已从Redis获取", username);
            return cachedUser;
        }
        
        log.debug("缓存未命中 (username): 从数据库获取用户[{}]的信息", username);
        Optional<AppUser> userOpt = appUserRepository.findByUsername(username);
        
        if (!userOpt.isPresent()) {
            log.warn("用户[{}]在数据库中不存在 (username)", username);
            return null;
        }
        
        AppUser user = userOpt.get();
        UserProfileDto userProfileDto = convertToDtoAndCache(user, cacheKey); // 提取转换和缓存逻辑
        return userProfileDto;
    }

    /**
     * 根据用户ID获取用户信息，优先从缓存获取，缓存未命中则从数据库获取并缓存
     * @param userId 用户ID
     * @return 用户信息DTO
     */
    public UserProfileDto getUserProfileById(Long userId) {
        if (userId == null) {
            return null;
        }
        String cacheKey = USER_ID_CACHE_KEY_PREFIX + userId;

        UserProfileDto cachedUser = (UserProfileDto) redisTemplate.opsForValue().get(cacheKey);
        if (cachedUser != null) {
            log.debug("缓存命中 (ID): 用户ID[{}]的信息已从Redis获取", userId);
            return cachedUser;
        }

        log.debug("缓存未命中 (ID): 从数据库获取用户ID[{}]的信息", userId);
        Optional<AppUser> userOpt = appUserRepository.findById(userId);

        if (!userOpt.isPresent()) {
            log.warn("用户ID[{}]在数据库中不存在", userId);
            return null;
        }

        AppUser user = userOpt.get();
        // 注意：这里缓存时也需要考虑同时更新 username->profile 的缓存，或者让 refreshUserCache 处理
        UserProfileDto userProfileDto = convertToDtoAndCache(user, cacheKey); // 使用相同的转换和缓存逻辑
        return userProfileDto;
    }

    // 提取公共的转换和缓存逻辑到一个私有方法
    private UserProfileDto convertToDtoAndCache(AppUser user, String cacheKey) {
        UserProfileDto userProfileDto = new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getPhoneNumber()
        );
        redisTemplate.opsForValue().set(cacheKey, userProfileDto, USER_CACHE_TTL, TimeUnit.MINUTES);
        log.debug("用户信息已缓存到Redis (Key: {}), 有效期{}分钟", cacheKey, USER_CACHE_TTL);
        return userProfileDto;
    }
    
    /**
     * 更新用户信息时刷新缓存 (同时更新基于username和ID的缓存)
     * @param user 更新后的用户信息
     */
    public void refreshUserCache(AppUser user) {
        if (user == null || user.getUsername() == null || user.getId() == null) {
            return;
        }
        
        String usernameCacheKey = USER_USERNAME_CACHE_KEY_PREFIX + user.getUsername();
        String idCacheKey = USER_ID_CACHE_KEY_PREFIX + user.getId();

        UserProfileDto userProfileDto = new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getPhoneNumber()
        );
        
        redisTemplate.opsForValue().set(usernameCacheKey, userProfileDto, USER_CACHE_TTL, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(idCacheKey, userProfileDto, USER_CACHE_TTL, TimeUnit.MINUTES);
        log.debug("刷新用户[{}(ID:{})]的缓存信息 (username & ID based)", user.getUsername(), user.getId());
    }
    
    /**
     * 清除指定用户的缓存 (同时清除基于username和ID的缓存)
     * @param username 用户名 (ID的清除将基于从username获取到的ID)
     */
    public void invalidateUserCache(String username) {
        if (username == null) {
            return;
        }
        String usernameCacheKey = USER_USERNAME_CACHE_KEY_PREFIX + username;
        UserProfileDto cachedDto = (UserProfileDto) redisTemplate.opsForValue().get(usernameCacheKey); // 先获取，以便得到ID

        Boolean deletedUsernameCache = redisTemplate.delete(usernameCacheKey);
        if (Boolean.TRUE.equals(deletedUsernameCache)) {
            log.debug("已清除用户[{}]的username缓存", username);
        }

        if (cachedDto != null && cachedDto.getId() != null) {
            String idCacheKey = USER_ID_CACHE_KEY_PREFIX + cachedDto.getId();
            Boolean deletedIdCache = redisTemplate.delete(idCacheKey);
            if (Boolean.TRUE.equals(deletedIdCache)) {
                log.debug("已清除用户[{}(ID:{})]的ID缓存", username, cachedDto.getId());
            }
        }
    }
} 