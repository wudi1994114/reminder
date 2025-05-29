# 提醒事项管理系统

一个功能强大的跨平台提醒事项管理系统，支持简单提醒和复杂定时任务管理。提供Web端、微信小程序等多端支持。

## 🌟 功能特点

### 📅 提醒管理
- **简单提醒**：创建一次性或基本重复性提醒
- **复杂提醒**：支持Cron表达式的高级定时任务
- **智能编辑**：支持简易模式和高级模式的Cron表达式编辑
- **时间范围**：可设置提醒的有效期和最大执行次数
- **实时预览**：直观展示下次触发时间和执行计划

### 🎯 用户体验
- **跨平台支持**：Web端、微信小程序、H5等多端适配
- **直观界面**：现代化的用户界面设计
- **实时预览**：修改设置时实时预览下次触发时间
- **人性化描述**：自动将复杂Cron表达式转换为易读文本
- **响应式设计**：适配各种屏幕尺寸

### 🔔 通知系统
- **邮件通知**：支持邮件提醒服务
- **短信通知**：预留短信提醒功能接口
- **微信通知**：小程序内消息推送（规划中）

### 🔐 认证系统
- **JWT认证**：安全的用户认证机制
- **微信登录**：支持微信小程序授权登录
- **用户隔离**：完善的多用户数据隔离

## 🛠️ 技术栈

### 后端架构
- **框架**：Spring Boot 2.7.18 + Java 8
- **认证**：Spring Security + JWT + 微信小程序授权
- **数据库**：JPA + H2(开发) / MySQL(生产)
- **调度**：Quartz Scheduler
- **通知**：Spring Mail
- **架构**：多模块Maven项目
  - `reminder-common`：公共模块
  - `reminder-core`：核心业务模块
  - `reminder-job`：任务调度模块

### 前端技术
#### Web端 (Vue.js)
- **框架**：Vue.js 3 (Composition API)
- **构建**：Vite 5.2.8
- **UI组件**：FullCalendar 6.1.17 + 自定义组件
- **HTTP**：Axios
- **Cron解析**：cron-parser、cronstrue

#### 移动端 (uni-app)
- **框架**：uni-app + Vue 3.4.21
- **构建**：Vite 5.2.8
- **样式**：SCSS + 设计系统
- **状态管理**：模块化状态管理
- **类型安全**：TypeScript支持

## 🚀 快速开始

### 环境要求
- JDK 8+
- Node.js 16+
- MySQL 5.7+ 或 H2数据库

### 后端启动
```bash
cd reminder-backend
./mvnw spring-boot:run
```

### Web前端启动
```bash
cd reminder-frontend
npm install
npm run dev
```

### uni-app启动
```bash
cd reminder-uni-app
npm install
npm run dev:h5          # H5版本
npm run dev:mp-weixin   # 微信小程序版本
```

### 访问应用
- 后端API：http://localhost:8080
- Web前端：http://localhost:5173
- uni-app H5：http://localhost:5174

## 📁 项目结构

```
reminder/
├── reminder-backend/           # Spring Boot后端
│   ├── reminder-common/        # 公共模块
│   │   └── src/main/java/com/common/reminder/
│   │       ├── constant/       # 常量定义
│   │       ├── dto/           # 数据传输对象
│   │       ├── model/         # 实体模型
│   │       └── utils/         # 工具类
│   ├── reminder-core/          # 核心业务模块
│   │   └── src/main/java/com/core/reminder/
│   │       ├── config/        # 配置类
│   │       ├── controller/    # 控制器
│   │       ├── dto/          # 业务DTO
│   │       ├── model/        # 业务实体
│   │       ├── repository/   # 数据访问层
│   │       ├── security/     # 安全配置
│   │       ├── service/      # 业务逻辑层
│   │       └── utils/        # 业务工具类
│   └── reminder-job/           # 任务调度模块
│       └── src/main/java/com/task/reminder/
│           ├── config/        # 调度配置
│           ├── job/          # 定时任务
│           ├── repository/   # 任务数据访问
│           ├── sender/       # 通知发送器
│           ├── service/      # 任务服务
│           └── utils/        # 任务工具类
├── reminder-frontend/          # Vue.js Web前端
│   ├── public/                # 静态资源
│   └── src/
│       ├── assets/           # 静态资源
│       ├── components/       # 组件
│       ├── pages/           # 页面
│       ├── router/          # 路由
│       ├── services/        # 服务
│       └── utils/           # 工具函数
└── reminder-uni-app/          # uni-app跨平台前端
    └── src/
        ├── api/             # API接口层
        ├── components/      # 公共组件
        ├── composables/     # Vue 3组合式函数
        ├── config/          # 配置文件
        ├── constants/       # 常量定义
        ├── pages/           # 页面
        ├── services/        # 服务层
        ├── store/           # 状态管理
        ├── styles/          # 样式文件
        ├── types/           # TypeScript类型
        └── utils/           # 工具函数
```

## 📊 数据模型

### 用户模型 (AppUser)
```java
class AppUser {
    Long id;
    String username;    // 用户名（唯一）
    String email;       // 邮箱（唯一）
    String password;    // 加密密码
    String avatarUrl;   // 头像URL
    String wechatOpenId; // 微信OpenID
}
```

### 简单提醒 (SimpleReminder)
```java
class SimpleReminder {
    Long id;
    Long fromUserId;                    // 创建者ID
    Long toUserId;                      // 接收者ID
    String title;                       // 标题
    String description;                 // 描述
    OffsetDateTime eventTime;           // 提醒时间
    ReminderType reminderType;          // 提醒方式
    Long originatingComplexReminderId;  // 关联的复杂提醒ID
    ReminderStatus status;              // 状态
}
```

### 复杂提醒 (ComplexReminder)
```java
class ComplexReminder {
    Long id;
    Long fromUserId;                // 创建者ID
    Long toUserId;                  // 接收者ID
    String title;                   // 标题模板
    String description;             // 描述模板
    String cronExpression;          // Cron表达式（5位格式：分 时 日 月 周）
    ReminderType reminderType;      // 提醒方式
    LocalDate validFrom;            // 生效开始日期
    LocalDate validUntil;           // 生效结束日期
    Integer maxExecutions;          // 最大执行次数
}
```

## 🔧 核心特性

### Cron表达式支持
- **统一格式**：采用5位标准格式（分 时 日 月 周）
- **智能解析**：自动将Cron表达式转换为人类可读的描述
- **可视化编辑**：提供图形化的Cron表达式编辑器
- **实时预览**：显示未来的执行时间计划

### 任务调度系统
- **Quartz集成**：基于Quartz的可靠任务调度
- **智能生成**：复杂提醒自动生成简单提醒实例
- **性能优化**：预生成策略 + 按需加载
- **防重复机制**：确保任务不会重复执行

### 跨平台支持
- **Web端**：完整的桌面端体验
- **微信小程序**：原生小程序体验
- **H5**：移动端浏览器适配
- **响应式设计**：自适应各种屏幕尺寸

## 📈 最近更新

### v2.0.0 (2024-12)
- ✅ 新增uni-app跨平台支持
- ✅ 重构Cron表达式解析系统，统一为5位格式
- ✅ 修复时间显示NaN和凌晨0点显示错误问题
- ✅ 优化复杂提醒编辑界面，支持简易/高级模式切换
- ✅ 新增微信小程序授权登录
- ✅ 完善用户数据隔离机制
- ✅ 编辑模式默认显示高级模式

### v1.0.0 (2024-11)
- ✅ 基础架构搭建完成
- ✅ 用户认证系统
- ✅ 简单提醒CRUD功能
- ✅ 复杂提醒Cron表达式支持
- ✅ 邮件通知系统
- ✅ Web端日历视图

## 🔮 开发计划

### 短期目标
- [ ] 微信小程序消息推送
- [ ] 短信通知集成
- [ ] 任务执行历史记录
- [ ] 数据导入导出功能

### 长期规划
- [ ] 任务依赖关系支持
- [ ] 智能推荐系统
- [ ] 团队协作功能
- [ ] 移动端App版本

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 Issue
- 发送邮件至项目维护者

---

**提醒事项管理系统** - 让时间管理变得简单高效 ⏰ 