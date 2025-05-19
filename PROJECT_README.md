# 提醒应用项目概述

## 项目简介
这是一个基于日历的提醒管理系统，支持一次性和重复性提醒，并通过邮件发送通知。

## 技术架构
### 后端 (Spring Boot)
- 核心框架：Spring Boot 2.7.18 + Java 8
- 认证：Spring Security + JWT
- 数据库：JPA + H2(开发)
- 调度：Quartz
- 通知：Spring Mail

### 前端 (Vue.js)
- 框架：Vue 3 (Composition API)
- 构建：Vite
- UI组件：FullCalendar 6.1.17
- HTTP：Axios

## 核心功能模块

### 1. 用户管理
- [x] 用户注册
- [x] 用户登录
- [ ] 用户资料管理
- [x] JWT认证

### 2. 提醒管理
- [x] 基础CRUD API
- [ ] 按用户隔离数据
- [ ] 提醒调度完善
- 支持类型：
  - 简单提醒（一次性）
  - 复杂提醒（重复性，基于CRON）

### 3. 通知系统
- [x] 邮件发送框架
- [ ] SMTP配置
- [ ] 短信通知（计划中）

## 当前状态

### 已完成
1. 基础架构搭建
2. 用户认证系统
3. 提醒CRUD API
4. 前端登录/注册界面
5. 日历视图基础组件

### 进行中
1. 用户数据隔离
2. Quartz调度逻辑完善
3. 前端事件管理适配
4. 用户资料管理功能

### 待开发
1. 邮件服务配置
2. 短信通知集成
3. 测试用例编写
4. 生产环境数据库迁移

## 数据模型

### AppUser
```java
class AppUser {
    Long id;
    String username;    // 唯一
    String email;       // 唯一
    String password;    // 加密存储
    String avatarUrl;   // 可选
}
```

### SimpleReminder（一次性提醒）
```java
class SimpleReminder {
    Long id;
    String title;
    String description;
    OffsetDateTime eventTime;
    String email;
    Long userId;
}
```

### ComplexReminder（重复性提醒）
```java
class ComplexReminder {
    Long id;
    String title;
    String description;
    String cronExpression;
    String email;
    Long userId;
}
```

## API概览

### 认证接口
- POST /api/auth/login
- POST /api/auth/register

### 提醒接口
- GET/POST/DELETE /api/reminders/simple
- GET/POST/DELETE /api/reminders/complex

## 快速启动

### 后端
```bash
cd reminder-backend
./mvnw spring-boot:run
```

### 前端
```bash
cd reminder-frontend
npm install
npm run dev
```

## 注意事项
1. 需要在application.yaml中配置邮件服务器信息
2. 前端默认运行在http://localhost:5173
3. 后端默认运行在http://localhost:8080
4. 已配置CORS支持前端开发服务器

## 近期更新
- 添加了用户头像URL支持
- 完善了用户认证响应
- 修复了事件CRUD操作 