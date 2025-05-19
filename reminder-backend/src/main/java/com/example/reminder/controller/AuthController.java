package com.example.reminder.controller;

import com.example.reminder.dto.ChangePasswordRequest;
import com.example.reminder.dto.LoginRequest;
import com.example.reminder.dto.LoginResponse;
import com.example.reminder.dto.RegisterRequest;
import com.example.reminder.dto.UserProfileDto;
import com.example.reminder.model.AppUser;
import com.example.reminder.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.reminder.exception.UserAlreadyExistsException;

import javax.validation.Valid;

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
        } catch (Exception e) {
            // Catch specific exceptions like BadCredentialsException for better error handling
            // For now, returning a generic 401 Unauthorized or 400 Bad Request
            // Log the exception details
            System.err.println("Login failed: " + e.getMessage()); // Replace with proper logging
            return ResponseEntity.status(401).body("Login failed: Invalid credentials or server error."); 
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