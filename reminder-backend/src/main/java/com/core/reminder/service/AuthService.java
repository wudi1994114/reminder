package com.core.reminder.service;

import com.core.reminder.dto.LoginRequest;
import com.core.reminder.dto.LoginResponse;
import com.core.reminder.dto.ChangePasswordRequest;
import com.core.reminder.dto.UpdateUserProfileRequest;
import com.common.reminder.dto.UserProfileDto;
import com.common.reminder.model.AppUser;
import com.core.reminder.repository.AppUserRepository;
import com.core.reminder.security.JwtTokenProvider;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.core.reminder.exception.UserAlreadyExistsException;
import com.core.reminder.dto.RegisterRequest;
import com.core.reminder.utils.PasswordValidator;

import java.time.OffsetDateTime;
@Slf4j
@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private AppUserRepository appUserRepository; // Inject repository to get full user details

    @Autowired
    private PasswordEncoder passwordEncoder; // 注入密码编码器
    
    @Autowired
    private UserCacheService userCacheService;

    public LoginResponse loginUser(LoginRequest loginRequest) {
        // 1. Authenticate using username
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(), // 使用 username
                        loginRequest.getPassword()
                )
        );

        // 2. If authentication is successful, set the authentication in the SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Generate JWT token
        // We need the full AppUser object to include details like ID and nickname in the response/token
        AppUser appUser = appUserRepository.findByUsername(loginRequest.getUsername())
             .orElseThrow(() -> new UsernameNotFoundException("User not found after authentication - unexpected state")); 

        // Generate token using the AppUser details for richer claims
        String jwt = tokenProvider.generateTokenFromUserDetails(appUser);

        // 4. Create and return the LoginResponse DTO
        return new LoginResponse(jwt, appUser.getId(), appUser.getEmail(), appUser.getNickname());
    }
    
    // --- 新增注册方法 ---
    public AppUser registerUser(RegisterRequest registerRequest) {
        // 1. 检查用户名是否已存在
        if (appUserRepository.existsByUsername(registerRequest.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken: " + registerRequest.getUsername());
        }

        // 2. 验证密码强度
        if (!PasswordValidator.isValidPassword(registerRequest.getPassword())) {
            throw new IllegalArgumentException(PasswordValidator.getPasswordRequirement());
        }

        // 3. 创建新用户对象
        AppUser newUser = new AppUser();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setEmail(registerRequest.getEmail());
        newUser.setNickname(registerRequest.getNickname());
        
        // 4. 加密密码
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        // Log the avatarUrl from DTO
        log.info("AuthService - Registering user. Avatar URL from DTO: '{}'", registerRequest.getAvatarUrl());

        // 5. 设置其他可选字段 (如果 DTO 中有)
        // newUser.setPhoneNumber(registerRequest.getPhoneNumber());
        newUser.setAvatarUrl(registerRequest.getAvatarUrl());
        
        // Log the avatarUrl set on entity
        log.info("AuthService - Avatar URL set on AppUser entity: '{}'", newUser.getAvatarUrl());

        // createdAt 和 updatedAt 会由 @CreationTimestamp 和 @UpdateTimestamp 自动处理

        // 6. 保存用户到数据库
        AppUser savedUser = appUserRepository.save(newUser);
        
        // 用户注册时不需要刷新缓存，因为还没有缓存

        return savedUser;
    }
    
    // 修改密码
    public void changePassword(Long userId, ChangePasswordRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        
        // 验证当前密码
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("当前密码不正确");
        }
        
        // 验证新密码强度
        if (!PasswordValidator.isValidPassword(request.getNewPassword())) {
            throw new IllegalArgumentException(PasswordValidator.getPasswordRequirement());
        }
        
        // 设置新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(OffsetDateTime.now());
        
        // 保存更新
        appUserRepository.save(user);
        
        // 密码更改后，清除该用户的缓存，强制下次请求重新加载
        userCacheService.invalidateUserCache(user.getUsername());
    }
    
    // 更新用户个人信息
    public UserProfileDto updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + userId));
        
        // 更新用户信息
        boolean needUpdate = false;
        
        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            user.setNickname(request.getNickname());
            needUpdate = true;
        }
        
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().equals(user.getAvatarUrl())) {
            user.setAvatarUrl(request.getAvatarUrl());
            needUpdate = true;
        }
        
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            user.setEmail(request.getEmail());
            needUpdate = true;
        }
        
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
            user.setPhoneNumber(request.getPhoneNumber());
            needUpdate = true;
        }
        
        // 如果有更新，保存到数据库
        if (needUpdate) {
            // 更新时间戳
            user.setUpdatedAt(OffsetDateTime.now());
            
            // 保存到数据库
            AppUser updatedUser = appUserRepository.save(user);
            
            // 更新缓存
            userCacheService.refreshUserCache(updatedUser);
            
            // 构建返回DTO
            return new UserProfileDto(
                    updatedUser.getId(),
                    updatedUser.getUsername(),
                    updatedUser.getEmail(),
                    updatedUser.getNickname(),
                    updatedUser.getAvatarUrl(),
                    updatedUser.getPhoneNumber()
            );
        } else {
            // 没有更新，返回当前用户信息
            return new UserProfileDto(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getNickname(),
                    user.getAvatarUrl(),
                    user.getPhoneNumber()
            );
        }
    }
} 