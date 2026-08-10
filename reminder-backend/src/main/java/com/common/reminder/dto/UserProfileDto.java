package com.common.reminder.dto;

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
    private String phoneNumber;
    // Add other fields you might want to expose, e.g., birthDate
    // but EXCLUDE password
} 