# 复杂提醒幂等性API文档

## 概述

为了防止复杂提醒的重复创建，系统实现了幂等性控制机制。支持两种幂等控制方式：

1. **客户端提供幂等键**：客户端在请求中提供唯一的幂等键
2. **系统自动生成幂等键**：系统根据业务字段自动生成幂等键

## API接口

### 创建复杂提醒（支持幂等控制）

**接口地址：** `POST /api/reminders/complex`

**请求头：**
```
Content-Type: application/json
Authorization: Bearer <token>
Idempotency-Key: <optional-idempotency-key>  // 可选的幂等键
```

**请求体：**
```json
{
  "title": "每日站会提醒",
  "description": "记得参加每日站会",
  "cronExpression": "0 0 9 * * MON-FRI",
  "reminderType": "EMAIL",
  "validFrom": "2024-01-01",
  "validUntil": "2024-12-31",
  "maxExecutions": 100,
  "idempotencyKey": "client-generated-key-123"  // 可选，也可以通过请求头提供
}
```

**响应：**

成功创建新提醒（HTTP 201）：
```json
{
  "id": 123,
  "title": "每日站会提醒",
  "description": "记得参加每日站会",
  "cronExpression": "0 0 9 * * MON-FRI",
  "reminderType": "EMAIL",
  "validFrom": "2024-01-01",
  "validUntil": "2024-12-31",
  "maxExecutions": 100,
  "idempotencyKey": "client-generated-key-123",
  "fromUserId": 1,
  "toUserId": 1,
  "lastGeneratedYm": 202406,
  "createdAt": "2024-06-21T10:00:00Z",
  "updatedAt": "2024-06-21T10:00:00Z"
}
```

返回已存在的提醒（HTTP 200）：
```json
{
  "id": 123,
  "title": "每日站会提醒",
  // ... 其他字段
  "idempotencyKey": "client-generated-key-123",
  "createdAt": "2024-06-21T09:00:00Z"  // 注意：这是原始创建时间
}
```

## 幂等键规则

### 1. 幂等键来源优先级

1. **请求头中的 `Idempotency-Key`**（最高优先级）
2. **请求体中的 `idempotencyKey` 字段**
3. **系统自动生成的业务幂等键**（如果以上都未提供）

### 2. 幂等键格式要求

- 长度：10-100个字符
- 字符集：字母、数字、下划线、连字符
- 正则表达式：`^[a-zA-Z0-9_-]+$`

### 3. 幂等键类型

#### 客户端生成的幂等键
```
示例：user-123-reminder-20240621-001
```

#### 系统生成的业务幂等键
```
格式：business_<SHA256哈希值>
示例：business_a1b2c3d4e5f6...
```

#### 随机UUID幂等键
```
格式：xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
示例：550e8400-e29b-41d4-a716-446655440000
```

## 幂等性行为

### 1. 重复检查机制

系统会按以下顺序进行重复检查：

1. **幂等键检查**：如果提供了幂等键，先检查是否已存在相同幂等键的记录
2. **业务字段检查**：检查是否存在相同业务字段组合的记录

### 2. 业务字段组合

以下字段组合被认为是相同的业务记录：
- `fromUserId`
- `toUserId`
- `title`
- `cronExpression`
- `reminderType`
- `validFrom`（NULL值统一处理为1900-01-01）
- `validUntil`（NULL值统一处理为9999-12-31）
- `maxExecutions`（NULL值统一处理为-1）

### 3. 并发处理

系统使用数据库唯一约束来处理并发情况：
- 如果发生唯一约束冲突，系统会重新查询已存在的记录并返回
- 确保在高并发情况下不会创建重复记录

## 错误处理

### 400 Bad Request
```json
{
  "error": "Bad Request",
  "message": "无效的幂等键格式",
  "timestamp": "2024-06-21T10:00:00Z"
}
```

### 500 Internal Server Error
```json
{
  "error": "Internal Server Error",
  "message": "创建复杂提醒事项失败",
  "timestamp": "2024-06-21T10:00:00Z"
}
```

## 最佳实践

### 1. 客户端实现建议

```javascript
// 生成客户端幂等键
function generateIdempotencyKey(userId, timestamp) {
  return `user-${userId}-${timestamp}-${Math.random().toString(36).substr(2, 9)}`;
}

// 创建复杂提醒
async function createComplexReminder(reminderData) {
  const idempotencyKey = generateIdempotencyKey(userId, Date.now());
  
  const response = await fetch('/api/reminders/complex', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'Idempotency-Key': idempotencyKey
    },
    body: JSON.stringify(reminderData)
  });
  
  if (response.status === 201) {
    console.log('新提醒创建成功');
  } else if (response.status === 200) {
    console.log('返回已存在的提醒');
  }
  
  return response.json();
}
```

### 2. 重试策略

- 对于网络错误，可以使用相同的幂等键重试
- 对于4xx错误，不应该重试
- 对于5xx错误，可以重试，但建议使用指数退避

### 3. 幂等键管理

- 客户端应该为每个独特的业务操作生成唯一的幂等键
- 建议在幂等键中包含用户ID、时间戳等信息以确保唯一性
- 可以将幂等键存储在本地，用于重试和状态跟踪

## 监控和日志

系统会记录以下关键信息：
- 幂等键的使用情况
- 重复请求的检测
- 并发冲突的处理
- 业务字段重复的检测

可以通过日志监控系统的幂等性工作情况。
