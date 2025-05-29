# 提醒助手 - uni-app版本

一个基于uni-app的跨平台提醒应用，支持多端部署（H5、微信小程序、App等）。

## 📁 项目结构（已优化）

```
src/
├── api/                    # API接口层
│   ├── auth.js            # 认证相关接口
│   ├── reminder.js        # 提醒相关接口
│   ├── calendar.js        # 日历相关接口
│   ├── request.js         # 请求封装
│   └── index.js           # 统一导出
├── store/                  # 状态管理
│   ├── modules/           # 状态模块
│   │   ├── user.js        # 用户状态
│   │   ├── reminder.js    # 提醒状态
│   │   └── ui.js          # UI状态
│   └── index.js           # 统一导出
├── utils/                  # 工具函数
│   ├── date.js            # 日期工具
│   ├── validation.js      # 表单验证
│   ├── helpers.js         # 原有工具函数
│   └── index.js           # 统一导出
├── types/                  # TypeScript类型定义
│   └── index.d.ts         # 主要类型定义
├── composables/           # Vue 3组合式函数
│   └── useAuth.js         # 认证相关composable
├── config/                # 配置文件
│   └── env.js             # 环境配置
├── styles/                # 样式文件
│   └── variables.scss     # SCSS变量
├── components/            # 公共组件
├── pages/                 # 页面
├── static/               # 静态资源
└── constants/            # 常量定义
```

## 🚀 主要改进

### 1. **模块化API层**
- 按功能拆分API（auth、reminder、calendar）
- 统一的请求封装和错误处理
- 支持环境配置

### 2. **状态管理优化**
- 模块化状态管理（user、reminder、ui）
- 清晰的action和getter
- 兼容旧版本API

### 3. **类型安全**
- 完整的TypeScript类型定义
- 接口类型、状态类型、组件Props类型

### 4. **工具函数分类**
- 日期工具函数独立模块
- 表单验证工具函数
- 通用工具函数扩展

### 5. **开发体验提升**
- 路径别名配置（@、@api、@store等）
- 环境配置分离
- SCSS变量系统

### 6. **Vue 3特性**
- Composables组合式函数
- 响应式状态管理
- 现代化的代码组织

## 🛠️ 使用方式

### API调用
```javascript
// 新的模块化API
import { authApi, reminderApi } from '@/api';

// 或使用具体模块
import { authApi } from '@/api/auth';
```

### 状态管理
```javascript
// 导入状态和操作
import { userState, userActions } from '@/store';

// 或使用composables
import { useAuth } from '@/composables/useAuth';
```

### 工具函数
```javascript
// 导入分类的工具函数
import { formatDate, isToday } from '@/utils/date';
import { isValidEmail } from '@/utils/validation';
```

## 📝 开发指南

### 1. **新增API接口**
在对应的API模块中添加方法，并在`api/index.js`中导出。

### 2. **状态管理**
在对应的store模块中添加状态和操作，保持模块化。

### 3. **组件开发**
使用TypeScript类型定义，利用设计系统变量。

### 4. **样式开发**
使用SCSS变量系统，保持设计一致性。

## 🔧 配置说明

### 路径别名
- `@` - src目录
- `@api` - api目录
- `@store` - store目录
- `@utils` - utils目录
- `@components` - components目录

### 环境配置
在`config/env.js`中配置不同环境的API地址和其他参数。

## 兼容性

项目保持了与原有代码的兼容性，旧的导入方式仍然可用：

```javascript
// 旧的导入方式仍然支持
import { login, getAllSimpleReminders } from '@/services/api';
import store from '@/services/store';
```

## 依赖

主要依赖包括：
- Vue 3.4.21
- @dcloudio/uni-app 3.0+
- Vite 5.2.8
- sass 1.89.0

所有依赖都是最新版本，确保项目的现代化和稳定性。 