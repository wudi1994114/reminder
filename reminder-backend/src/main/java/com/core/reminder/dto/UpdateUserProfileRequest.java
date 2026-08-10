package com.core.reminder.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

@Data
public class UpdateUserProfileRequest {
    
    @Size(max = 100, message = "昵称长度不能超过100个字符")
    private String nickname;
    
    private String avatarUrl;
    
    @Email(message = "邮箱格式不正确")
    @Size(max = 255, message = "邮箱长度不能超过255个字符")
    private String email;
    
    @Size(max = 20, message = "手机号长度不能超过20个字符")
    private String phoneNumber;
} 