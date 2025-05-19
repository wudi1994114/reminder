package com.core.reminder.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 100, message = "用户名长度必须在3到100个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少为6个字符") // 密码长度要求
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 100)
    private String nickname;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式无效")
    @Size(max = 255)
    private String email;

    // 以后如有需要可以在此添加可选字段
    // private String phoneNumber;
    // private String avatarUrl;
    // private String gender;
    // private String birthDate; // 考虑使用LocalDate类型
} 