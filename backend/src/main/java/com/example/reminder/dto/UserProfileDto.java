package com.example.reminder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatarUrl;
    // Add other fields you might want to expose, e.g., phoneNumber, birthDate
    // but EXCLUDE password
} 