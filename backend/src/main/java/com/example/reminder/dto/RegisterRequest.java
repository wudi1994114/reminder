package com.example.reminder.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, message = "Password must be at least 6 characters long") // 密码长度要求
    private String password;

    @NotBlank(message = "Nickname cannot be blank")
    @Size(max = 100)
    private String nickname;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    @Size(max = 255)
    private String email;

    // Optional fields can be added here later if needed
    // private String phoneNumber;
    // private String avatarUrl;
    // private String gender;
    // private String birthDate; // Consider using LocalDate if added
} 