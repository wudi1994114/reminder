package com.core.reminder.controller;

import com.core.reminder.dto.ChangePasswordRequest;
import com.core.reminder.dto.LoginRequest;
import com.core.reminder.dto.LoginResponse;
import com.core.reminder.dto.RegisterRequest;
import com.common.reminder.dto.UserProfileDto;
import com.common.reminder.model.AppUser;
import com.core.reminder.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse loginResponse = authService.loginUser(loginRequest);
            return ResponseEntity.ok(loginResponse);
        } catch (BadCredentialsException e) {
            // 密码错误的情况
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "用户名或密码错误");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (UsernameNotFoundException e) {
            // 用户不存在的情况
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "用户不存在：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (Exception e) {
            // 其他未预期的错误
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "登录失败：" + e.getMessage());
            // 记录错误日志
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        AppUser registeredUser = authService.registerUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully!");
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestAttribute("currentUser") UserProfileDto currentUser,
                                          @Valid @RequestBody ChangePasswordRequest request) {
        try {
            // 验证确认密码
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest().body("新密码与确认密码不匹配");
            }
            
            // 执行密码修改
            authService.changePassword(currentUser.getId(), request);
            
            return ResponseEntity.ok("密码修改成功");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("修改密码失败：服务器内部错误");
        }
    }

    // Optional: Add endpoint for registration later
    // @PostMapping("/register")
    // public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) { ... }
} 