# 提醒事项管理系统

一个功能强大的提醒事项管理系统，支持简单提醒和复杂定时任务管理。

## 功能特点

### 提醒管理
- **简单提醒**：创建一次性或重复性基本提醒
- **复杂提醒**：支持Cron表达式的高级定时任务
- **灵活设置**：支持简易模式和高级模式的Cron表达式编辑
- **时间范围**：可设置提醒的有效期和最大执行次数
- **预览功能**：直观展示下次触发时间

### 用户体验
- **直观界面**：清晰简洁的用户界面
- **实时预览**：修改设置时实时预览下次触发时间
- **人性化描述**：自动将复杂Cron表达式转换为易读文本

### 通知系统
- **邮件通知**：支持邮件提醒服务
- **短信通知**：预留短信提醒功能接口（后期实现）

## 技术栈

### 后端
- **框架**：Spring Boot 2.7.18 + Java 8
- **认证**：Spring Security + JWT
- **数据库**：JPA + H2(开发)
- **调度**：Spring Scheduler
- **通知**：Spring Mail

### 前端
- **框架**：Vue.js 3 (Composition API)
- **构建**：Vite
- **UI组件**：FullCalendar 6.1.17 + 自定义组件
- **HTTP**：Axios
- **Cron解析**：cron-parser、cronstrue

## 安装说明

### 环境要求
- JDK 8+
- Node.js 14+
- MySQL 5.7+ 或 H2数据库

### 后端启动
```bash
cd backend
./mvnw spring-boot:run
```

### 前端启动
```bash
cd frontend
npm install
npm run dev
```

### 访问应用
应用默认运行在：
- 后端API：http://localhost:8080
- 前端页面：http://localhost:5173

## 项目结构
```
reminder
├── backend                 # Spring Boot后端
│   └── src/main
│       ├── java            # Java源代码
│       │   └── com/example/reminder
│       │       ├── config      # 配置类
│       │       ├── controller  # 控制器
│       │       ├── dto         # 数据传输对象
│       │       ├── model       # 实体模型
│       │       ├── repository  # 数据访问层
│       │       ├── service     # 业务逻辑层
│       │       └── utils       # 工具类
│       └── resources       # 配置文件
└── frontend                # Vue.js前端
    ├── public              # 静态资源
    └── src                 # 源代码
        ├── assets          # 静态资源
        ├── components      # 组件
        ├── pages           # 页面
        ├── router          # 路由
        ├── services        # 服务
        └── utils           # 工具函数
```

## 数据模型

### 简单提醒 (SimpleReminder)
一次性提醒事项，包含标题、描述、提醒时间等基本信息。

### 复杂提醒 (ComplexReminder)
基于Cron表达式的重复性提醒，支持高级定时任务管理，可设置有效期和最大执行次数。

## 近期更新
- 增强了复杂提醒功能，添加有效期和最大执行次数限制
- 新增复杂提醒编辑界面，支持简易/高级模式切换
- 添加了Cron表达式预览和人性化描述功能
- 优化用户界面和交互体验

---

# Reminder Management System

A powerful reminder management system supporting simple reminders and complex scheduled tasks.

## Features

### Reminder Management
- **Simple Reminders**: Create one-time or repeating basic reminders
- **Complex Reminders**: Advanced scheduled tasks with Cron expressions
- **Flexible Settings**: Support for both simple and advanced Cron expression editing
- **Time Range**: Set validity period and maximum execution count
- **Preview Function**: Intuitive display of next trigger times

### User Experience
- **Intuitive Interface**: Clear and concise user interface
- **Real-time Preview**: Preview next trigger times as settings are modified
- **Human-readable Descriptions**: Automatically convert complex Cron expressions to readable text

### Notification System
- **Email Notifications**: Support for email reminder services
- **SMS Notifications**: Reserved SMS reminder function interface (to be implemented later)

## Technology Stack

### Backend
- **Framework**: Spring Boot 2.7.18 + Java 8
- **Authentication**: Spring Security + JWT
- **Database**: JPA + H2 (development)
- **Scheduling**: Spring Scheduler
- **Notification**: Spring Mail

### Frontend
- **Framework**: Vue.js 3 (Composition API)
- **Build Tool**: Vite
- **UI Components**: FullCalendar 6.1.17 + Custom Components
- **HTTP**: Axios
- **Cron Parsing**: cron-parser, cronstrue

## Installation Guide

### Requirements
- JDK 8+
- Node.js 14+
- MySQL 5.7+ or H2 Database

### Start Backend
```bash
cd backend
./mvnw spring-boot:run
```

### Start Frontend
```bash
cd frontend
npm install
npm run dev
```

### Access Application
Application runs at:
- Backend API: http://localhost:8080
- Frontend: http://localhost:5173

## Project Structure
```
reminder
├── backend                 # Spring Boot backend
│   └── src/main
│       ├── java            # Java source code
│       │   └── com/example/reminder
│       │       ├── config      # Configurations
│       │       ├── controller  # Controllers
│       │       ├── dto         # Data Transfer Objects
│       │       ├── model       # Entity Models
│       │       ├── repository  # Data Access Layer
│       │       ├── service     # Business Logic Layer
│       │       └── utils       # Utilities
│       └── resources       # Configuration files
└── frontend                # Vue.js frontend
    ├── public              # Static resources
    └── src                 # Source code
        ├── assets          # Static assets
        ├── components      # Components
        ├── pages           # Pages
        ├── router          # Router
        ├── services        # Services
        └── utils           # Utility functions
```

## Data Models

### Simple Reminder
One-time reminder containing title, description, reminder time, and other basic information.

### Complex Reminder
Repeating reminders based on Cron expressions, supporting advanced scheduled task management with validity period and maximum execution count.

## Recent Updates
- Enhanced complex reminder functionality with validity period and maximum execution count
- Added complex reminder editing interface with simple/advanced mode switching
- Added Cron expression preview and human-readable description
- Optimized user interface and interaction experience 