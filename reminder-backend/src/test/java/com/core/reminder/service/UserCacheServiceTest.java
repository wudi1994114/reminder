package com.core.reminder.service;

import com.common.reminder.dto.UserNotificationProfileDto;
import com.common.reminder.model.AppUser;
import com.common.reminder.model.WechatUser;
import com.core.reminder.repository.AppUserRepository;
import com.core.reminder.repository.WechatUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private WechatUserRepository wechatUserRepository;

    @InjectMocks
    private UserCacheService userCacheService;

    @BeforeEach
    void setUpRedisOperations() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void combinesApplicationAndWechatProfilesForNotificationDelivery() {
        AppUser user = new AppUser();
        user.setId(7L);
        user.setUsername("notify-user");
        user.setNickname("Notify User");
        user.setEmail("notify@example.com");

        WechatUser wechatUser = new WechatUser();
        wechatUser.setAppUserId(7L);
        wechatUser.setOpenid("openid-for-notification");
        wechatUser.setUnionid("unionid-for-notification");

        when(valueOperations.get("user:id:profile:7")).thenReturn(null);
        when(appUserRepository.findById(7L)).thenReturn(Optional.of(user));
        when(wechatUserRepository.findByAppUserId(7L)).thenReturn(Optional.of(wechatUser));

        UserNotificationProfileDto result = userCacheService.getUserNotificationProfileById(7L);

        assertNotNull(result);
        assertEquals("notify@example.com", result.getEmail());
        assertEquals("openid-for-notification", result.getWechatOpenid());
        assertEquals("unionid-for-notification", result.getWechatUnionid());
    }
}
