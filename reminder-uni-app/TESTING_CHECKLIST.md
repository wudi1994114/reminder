# 重构后测试清单

## 测试环境准备
- [ ] 安装依赖：`npm install`
- [ ] 检查编译：`npm run dev:mp-weixin`
- [ ] 清除缓存：清理 `node_modules/.cache` 和 `dist`

## 核心功能测试

### 1. 登录/认证功能 ✅
**测试点**：
- [ ] 微信一键登录
- [ ] Token存储和刷新
- [ ] 登出功能
- [ ] 认证状态保持

**验证方式**：
```javascript
// 在小程序开发工具中测试
// 1. 首次打开应用，应显示登录提示
// 2. 点击微信登录，应成功获取token
// 3. 重新打开应用，应保持登录状态
// 4. 点击登出，应清除token
```

**关键文件**：
- `services/userService.js` - 用户服务
- `api/wechat.js` - 微信登录API
- `api/auth.js` - 认证API
- `components/OneClickLogin.vue` - 登录组件

### 2. 提醒CRUD功能 ✅
**测试点**：
- [ ] 创建简单提醒
- [ ] 更新简单提醒
- [ ] 删除简单提醒
- [ ] 获取提醒列表
- [ ] 创建复杂提醒（Cron表达式）
- [ ] 更新复杂提醒
- [ ] 删除复杂提醒

**验证方式**：
```javascript
// 测试简单提醒
import { reminderApi } from '@/api/reminder.js';

// 创建
const reminder = await reminderApi.createSimpleReminder({
  title: '测试提醒',
  eventTime: '2025-10-18 10:00:00'
});

// 更新
await reminderApi.updateSimpleReminder(reminder.id, {
  title: '更新后的提醒'
});

// 删除
await reminderApi.deleteSimpleReminder(reminder.id);
```

**关键文件**：
- `api/reminder.js` - 提醒API
- `services/reminderService.js` - 提醒服务
- `services/cachedApi.js` - 带缓存的API
- `pages/create/create.vue` - 创建页面
- `pages/index/index.vue` - 列表页面

### 3. 缓存机制 ✅
**测试点**：
- [ ] 数据缓存存储
- [ ] 缓存过期检查
- [ ] 缓存清除
- [ ] 数据版本管理
- [ ] 缓存统计

**验证方式**：
```javascript
import CacheService from '@/services/cacheService.js';
import ReminderService from '@/services/reminderService.js';

// 测试缓存设置和获取
CacheService.set('test-key', { data: 'test' }, 5000);
const cached = CacheService.get('test-key');
console.log('缓存数据:', cached);

// 测试提醒缓存
const reminders = await ReminderService.getSimpleReminders(2025, 10);
console.log('提醒列表:', reminders);

// 检查缓存统计
const stats = CacheService.getStats();
console.log('缓存统计:', stats);
```

**关键文件**：
- `services/cacheService.js` - 通用缓存服务
- `services/reminderService.js` - 提醒缓存逻辑
- `services/cachedApi.js` - 带缓存的API封装

### 4. 日期处理功能 ✅
**测试点**：
- [ ] 日期格式化
- [ ] 时间格式化
- [ ] 智能格式化（今天、昨天等）
- [ ] 相对时间显示
- [ ] 农历转换
- [ ] 节气计算

**验证方式**：
```javascript
import { 
  formatDateTime, 
  formatDate, 
  formatTime,
  formatSmart,
  formatRelative 
} from '@/utils/date/format.js';

import { 
  getLunarInfo, 
  getSolarTermForDate 
} from '@/utils/date/lunar.js';

// 测试格式化
console.log('日期时间:', formatDateTime(new Date()));
console.log('日期:', formatDate(new Date()));
console.log('时间:', formatTime(new Date()));
console.log('智能:', formatSmart(new Date()));

// 测试农历
const lunar = getLunarInfo(new Date());
console.log('农历信息:', lunar);

// 测试节气
const term = getSolarTermForDate(new Date());
console.log('节气:', term);
```

**关键文件**：
- `utils/date/format.js` - 格式化功能
- `utils/date/lunar.js` - 农历和节气
- `utils/date/index.js` - 统一导出
- `components/SimpleReminderCard.vue` - 使用日期格式化

## 兼容性测试

### 5. 兼容层测试 ✅
**测试点**：
- [ ] 旧的API导入方式仍然可用
- [ ] ReminderCacheService转发工作正常
- [ ] 数据结构保持一致

**验证方式**：
```javascript
// 测试旧的导入方式（应该仍然工作）
import { 
  login, 
  getAllSimpleReminders,
  createEvent 
} from '@/services/api.js';

import ReminderCacheService from '@/services/reminderCache.js';

// 测试兼容层
const reminders = await getAllSimpleReminders();
console.log('通过兼容层获取的提醒:', reminders);

const userState = ReminderCacheService.getUserState();
console.log('用户状态:', userState);
```

**关键文件**：
- `services/api.js` - API兼容层
- `services/reminderCache.js` - 缓存兼容层

## 集成测试

### 6. 完整流程测试
**流程**：
1. [ ] 启动应用
2. [ ] 微信登录
3. [ ] 创建提醒
4. [ ] 查看提醒列表
5. [ ] 编辑提醒
6. [ ] 删除提醒
7. [ ] 登出

### 7. 页面测试
- [ ] 首页（index）- 提醒列表
- [ ] 创建页（create）- 简单提醒
- [ ] 创建复杂提醒页（create-complex）
- [ ] 详情页（detail）
- [ ] 日历页（calendar）
- [ ] 个人中心（mine）
- [ ] 个人资料编辑（profile/edit）
- [ ] 设置页（settings）

## 性能测试

### 8. 性能指标
- [ ] 首次加载时间 < 3秒
- [ ] 页面切换响应 < 500ms
- [ ] API响应时间 < 1秒
- [ ] 缓存命中率 > 80%
- [ ] 内存占用 < 50MB

### 9. 缓存效率测试
```javascript
// 测试缓存性能
console.time('首次加载');
await reminderApi.getAllSimpleReminders();
console.timeEnd('首次加载');

console.time('缓存加载');
await reminderApi.getAllSimpleReminders();
console.timeEnd('缓存加载');

// 应该看到第二次明显更快
```

## 错误处理测试

### 10. 异常场景
- [ ] 网络错误处理
- [ ] Token过期处理
- [ ] 服务器错误处理
- [ ] 数据格式错误处理
- [ ] 缓存失效处理

## 兼容性测试

### 11. 微信小程序环境
- [ ] 微信开发者工具
- [ ] 真机调试
- [ ] iOS设备
- [ ] Android设备

### 12. 功能兼容性
- [ ] 云托管模式
- [ ] HTTP模式
- [ ] 混合模式

## 代码质量检查

### 13. Lint检查
```bash
# 如果有配置eslint
npm run lint

# 或手动检查
cd src && find . -name "*.js" -o -name "*.vue" | xargs grep -l "console.log"
```

### 14. 类型检查
- [ ] 检查导入路径
- [ ] 检查函数签名
- [ ] 检查变量命名

## 测试结果

### ✅ 通过的测试
- [x] 新文件创建成功（无语法错误）
- [x] 兼容层正确导出API
- [x] 导入路径修复完成
- [x] 模块结构清晰

### ⚠️ 需要注意的点
1. 某些页面仍在使用兼容层（预期行为）
2. 建议逐步迁移到新的模块化导入
3. 监控缓存命中率和性能指标

### 📝 待测试项目（需要在实际环境中运行）
1. 微信登录流程
2. 提醒CRUD完整流程
3. 缓存机制实际效果
4. 农历和节气计算准确性
5. 页面性能指标

## 建议

1. **逐步迁移**：建议在未来1-2个月内，逐步将页面迁移到新的模块化导入
2. **性能监控**：添加性能监控，跟踪缓存命中率和API响应时间
3. **单元测试**：为核心服务添加单元测试（userService, cacheService, reminderService）
4. **文档更新**：更新开发文档，说明新的架构和使用方式

## 下一步

1. 在小程序开发工具中进行完整的功能测试
2. 真机测试各个核心流程
3. 监控线上性能指标
4. 收集用户反馈
5. 根据反馈进行优化

