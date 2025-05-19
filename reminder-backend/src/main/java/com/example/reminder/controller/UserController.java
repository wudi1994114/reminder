package com.example.reminder.controller;

import com.example.reminder.dto.UpdateUserProfileRequest;
import com.example.reminder.dto.UserProfileDto;
import com.example.reminder.model.AppUser;
import com.example.reminder.repository.AppUserRepository;
import com.example.reminder.service.UserCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private AppUserRepository appUserRepository;
    
    @Autowired
    private UserCacheService userCacheService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401).body("Authentication required.");
        }

        String username;
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            username = (String) principal;
        } else {
            // Handle unexpected principal type if necessary
            return ResponseEntity.status(500).body("Could not determine username from principal.");
        }

        AppUser currentUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Map AppUser to UserProfileDto
        UserProfileDto userProfileDto = new UserProfileDto(
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getEmail(),
                currentUser.getNickname(),
                currentUser.getAvatarUrl()
        );

        return ResponseEntity.ok(userProfileDto);
    }
    
    @PutMapping("/me")
    public ResponseEntity<?> updateUserProfile(@RequestAttribute("currentUser") UserProfileDto currentUser, 
                                             @Valid @RequestBody UpdateUserProfileRequest request) {
        // 获取当前用户
        AppUser user = appUserRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + currentUser.getId()));
        
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
            UserProfileDto updatedUserDto = new UserProfileDto(
                    updatedUser.getId(),
                    updatedUser.getUsername(),
                    updatedUser.getEmail(),
                    updatedUser.getNickname(),
                    updatedUser.getAvatarUrl()
            );
            
            return ResponseEntity.ok(updatedUserDto);
        }
        
        // 如果没有更新，直接返回原有信息
        return ResponseEntity.ok(currentUser);
    }
} 