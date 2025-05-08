package com.example.reminder.controller;

import com.example.reminder.dto.LoginRequest;
import com.example.reminder.dto.LoginResponse;
import com.example.reminder.dto.RegisterRequest;
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
        try {
            AppUser registeredUser = authService.registerUser(registerRequest);
            // 注册成功返回 201 Created，可以包含用户信息或成功消息
            // 为了安全，通常不直接返回密码
            // 可以只返回一个成功消息，或是不包含密码的用户信息 DTO
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully!");
            // 或者返回用户信息 (需要创建 User DTO 或从 registeredUser 构造)
            // return ResponseEntity.status(HttpStatus.CREATED).body(createUserDtoFromAppUser(registeredUser)); 
        } catch (UserAlreadyExistsException e) {
            // 返回 409 Conflict，包含错误信息
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            // 其他未知错误返回 500
            System.err.println("Registration failed: " + e.getMessage()); // 替换为日志
            e.printStackTrace(); // 打印堆栈跟踪以供调试
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Registration failed due to an internal error.");
        }
    }

    // Optional: Add endpoint for registration later
    // @PostMapping("/register")
    // public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) { ... }
} 