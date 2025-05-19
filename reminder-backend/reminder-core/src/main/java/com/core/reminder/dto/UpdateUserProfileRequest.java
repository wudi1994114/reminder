package com.core.reminder.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class UpdateUserProfileRequest {
    
    @Size(max = 100, message = "昵称长度不能超过100个字符")
    private String nickname;
    
    private String avatarUrl;
    
    @Size(max = 20, message = "手机号长度不能超过20个字符")
    private String phoneNumber;
} 