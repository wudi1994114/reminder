# API修复总结 - 提醒设置页面

## 问题描述
提醒设置页面（`notification.vue`）中的标签管理和授权次数无法正常显示。

## 根本原因
重构后的 `api/user.js` 与原有的API调用格式不兼容，导致：
1. 请求参数格式错误
2. 响应数据格式处理不正确
3. API URL格式不匹配

## 修复内容

### 1. 标签管理启用状态（`setUserTagManagementEnabled`）

**问题**：
- 旧版：发送 `{ key: 'userTagManagementEnabled', value: '1'/'0', property: '' }`
- 新版（错误）：发送 `{ value: true/false }`

**修复**：
```javascript
setUserTagManagementEnabled: (enabled) => {
    const key = 'userTagManagementEnabled';
    const value = enabled ? '1' : '0'; // 转换为字符串
    const property = '';
    return request({
        url: '/user/preferences/userTagManagementEnabled',
        method: 'PUT',
        data: { key, value, property }
    });
}
```

### 2. 用户标签列表（`getUserTagList`）

**问题**：
- 旧版：返回 `{ value: "tag1|-|tag2" }` 格式，并过滤反斜杠
- 新版（错误）：尝试将响应转换为数组

**修复**：
```javascript
getUserTagList: () => {
    return request({
        url: '/user/preferences/userTagList',
        method: 'GET'
    }).then(response => {
        // 在API层面过滤掉所有反斜杠字符
        if (response && response.value && typeof response.value === 'string') {
            response.value = response.value.replace(/\\/g, '');
            console.log('🏷️ API层面 - 过滤反斜杠后的标签字符串:', response.value);
        }
        return response;
    });
}
```

### 3. 设置标签列表（`setUserTagList`）

**问题**：
- 旧版：发送 `{ key: 'userTagList', value: 'tag1|-|tag2', property: '' }`
- 新版（错误）：发送 `{ value: 'tag1|-|tag2' }`

**修复**：
```javascript
setUserTagList: (tagList) => {
    const key = 'userTagList';
    const value = tagList;
    const property = '';
    return request({
        url: '/user/preferences/userTagList',
        method: 'PUT',
        data: { key, value, property }
    });
}
```

### 4. 微信授权次数API（`getWechatAuthCount` 和 `increaseWechatAuthCount`）

**问题**：
- 旧版URL：`/user/preferences/wechat-auth-count`（带连字符）
- 新版URL（错误）：`/user/preferences/wechatAuthCount`（驼峰命名）

**修复**：
```javascript
getWechatAuthCount: () => {
    return request({
        url: '/user/preferences/wechat-auth-count',  // 修正URL
        method: 'GET'
    });
},

increaseWechatAuthCount: (count = 1) => {
    return request({
        url: '/user/preferences/wechat-auth-count/increase',  // 修正URL
        method: 'POST',
        data: { count }
    });
}
```

## 相关文件
- ✅ `/src/api/user.js` - 已修复
- ✅ `/src/services/cachedApi.js` - 已添加缺失的导入
- ✅ `/src/pages/settings/notification.vue` - 无需修改（使用正确的API格式）

## 测试建议
1. 打开提醒设置页面（`/pages/settings/notification`）
2. 验证标签管理开关能否正常开启/关闭
3. 验证标签列表能否正常显示
4. 验证能否添加、删除、拖拽标签
5. 验证微信授权次数是否正确显示
6. 点击"增加授权"按钮，验证授权次数是否正确增加

## 注意事项
在重构API时，必须保持与后端接口的兼容性：
- URL格式必须与后端一致（注意连字符vs驼峰命名）
- 请求参数格式必须与后端期望的格式一致
- 响应数据的处理必须与UI组件期望的格式一致

---
*修复时间：2025-10-17*
*修复人：AI Assistant*

