package com.example.reminder.service;

import com.example.reminder.dto.UserProfileDto;
import com.example.reminder.model.AppUser;
import com.example.reminder.repository.AppUserRepository;
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
    private static final String USER_CACHE_KEY_PREFIX = "user:profile:";
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
        String cacheKey = USER_CACHE_KEY_PREFIX + username;
        
        // 尝试从缓存获取
        UserProfileDto cachedUser = (UserProfileDto) redisTemplate.opsForValue().get(cacheKey);
        if (cachedUser != null) {
            log.debug("缓存命中: 用户[{}]的信息已从Redis获取", username);
            return cachedUser;
        }
        
        log.debug("缓存未命中: 从数据库获取用户[{}]的信息", username);
        // 缓存未命中，从数据库获取
        Optional<AppUser> userOpt = appUserRepository.findByUsername(username);
        
        if (!userOpt.isPresent()) {
            log.warn("用户[{}]在数据库中不存在", username);
            return null;
        }
        
        AppUser user = userOpt.get();
        UserProfileDto userProfileDto = new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl()
        );
        
        // 缓存用户信息
        redisTemplate.opsForValue().set(cacheKey, userProfileDto, USER_CACHE_TTL, TimeUnit.MINUTES);
        log.debug("用户[{}]的信息已缓存到Redis, 有效期{}分钟", username, USER_CACHE_TTL);
        
        return userProfileDto;
    }
    
    /**
     * 更新用户信息时刷新缓存
     * @param user 更新后的用户信息
     */
    public void refreshUserCache(AppUser user) {
        if (user == null || user.getUsername() == null) {
            return;
        }
        
        String cacheKey = USER_CACHE_KEY_PREFIX + user.getUsername();
        UserProfileDto userProfileDto = new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl()
        );
        
        // 更新缓存
        redisTemplate.opsForValue().set(cacheKey, userProfileDto, USER_CACHE_TTL, TimeUnit.MINUTES);
        log.debug("刷新用户[{}]的缓存信息", user.getUsername());
    }
    
    /**
     * 清除指定用户的缓存
     * @param username 用户名
     */
    public void invalidateUserCache(String username) {
        if (username == null) {
            return;
        }
        
        String cacheKey = USER_CACHE_KEY_PREFIX + username;
        Boolean deleted = redisTemplate.delete(cacheKey);
        if (Boolean.TRUE.equals(deleted)) {
            log.debug("已清除用户[{}]的缓存", username);
        }
    }
} 