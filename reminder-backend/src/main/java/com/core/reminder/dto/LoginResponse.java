package com.core.reminder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private Long userId;
    private String email;
    private String nickname;

    public LoginResponse(String accessToken, Long userId, String email, String nickname) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
    }
} 