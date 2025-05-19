package com.core.reminder.service;

import com.core.reminder.dto.LoginRequest;
import com.core.reminder.dto.LoginResponse;
import com.core.reminder.dto.ChangePasswordRequest;
import com.common.reminder.model.AppUser;
import com.core.reminder.repository.AppUserRepository;
import com.core.reminder.security.JwtTokenProvider;
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

import java.time.OffsetDateTime;

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

        // 2. 检查邮箱是否已存在
        if (appUserRepository.existsByEmail(registerRequest.getEmail())) {
            throw new UserAlreadyExistsException("Email Address already in use: " + registerRequest.getEmail());
        }

        // 3. 创建新用户对象
        AppUser newUser = new AppUser();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setEmail(registerRequest.getEmail());
        newUser.setNickname(registerRequest.getNickname());
        
        // 4. 加密密码
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        // 5. 设置其他可选字段 (如果 DTO 中有)
        // newUser.setPhoneNumber(registerRequest.getPhoneNumber());
        // newUser.setAvatarUrl(registerRequest.getAvatarUrl());
        // newUser.setGender(registerRequest.getGender());
        // newUser.setBirthDate(registerRequest.getBirthDate()); 

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
        
        // 设置新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(OffsetDateTime.now());
        
        // 保存更新
        appUserRepository.save(user);
        
        // 密码更改后，清除该用户的缓存，强制下次请求重新加载
        userCacheService.invalidateUserCache(user.getUsername());
    }
} 