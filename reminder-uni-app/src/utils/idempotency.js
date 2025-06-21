/**
 * 幂等性工具函数
 * 用于生成和管理API请求的幂等键
 */

/**
 * 生成幂等键
 * 格式: user-{userId}-{timestamp}-{randomSuffix}
 * 
 * @param {string|number} userId - 用户ID
 * @param {string} action - 操作类型，如 'create-complex'
 * @returns {string} 幂等键
 */
export function generateIdempotencyKey(userId, action = 'action') {
    const timestamp = Date.now();
    const randomSuffix = Math.random().toString(36).substring(2, 8);
    const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '');
    
    return `user-${userId}-${dateStr}-${action}-${randomSuffix}`;
}

/**
 * 生成基于内容的幂等键
 * 用于防止相同内容的重复提交
 * 
 * @param {string|number} userId - 用户ID
 * @param {Object} content - 内容对象
 * @param {string} action - 操作类型
 * @returns {string} 幂等键
 */
export function generateContentBasedIdempotencyKey(userId, content, action = 'action') {
    // 创建内容的简化哈希
    const contentStr = JSON.stringify(content);
    const hash = simpleHash(contentStr);
    const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '');
    
    return `user-${userId}-${dateStr}-${action}-${hash}`;
}

/**
 * 简单哈希函数
 * 用于生成内容的哈希值
 * 
 * @param {string} str - 要哈希的字符串
 * @returns {string} 哈希值
 */
function simpleHash(str) {
    let hash = 0;
    if (str.length === 0) return hash.toString(36);
    
    for (let i = 0; i < str.length; i++) {
        const char = str.charCodeAt(i);
        hash = ((hash << 5) - hash) + char;
        hash = hash & hash; // 转换为32位整数
    }
    
    return Math.abs(hash).toString(36);
}

/**
 * 验证幂等键格式
 * 
 * @param {string} key - 幂等键
 * @returns {boolean} 是否有效
 */
export function isValidIdempotencyKey(key) {
    if (!key || typeof key !== 'string') {
        return false;
    }
    
    // 基本长度检查
    if (key.length < 10 || key.length > 255) {
        return false;
    }
    
    // 检查是否包含非法字符（只允许字母、数字、连字符、下划线）
    const validPattern = /^[a-zA-Z0-9\-_]+$/;
    return validPattern.test(key);
}

/**
 * 为复杂任务创建生成幂等键
 * 
 * @param {string|number} userId - 用户ID
 * @param {Object} reminderData - 任务数据
 * @returns {string} 幂等键
 */
export function generateComplexReminderIdempotencyKey(userId, reminderData) {
    // 使用任务的关键字段生成内容哈希
    const keyFields = {
        title: reminderData.title,
        cronExpression: reminderData.cronExpression,
        reminderType: reminderData.reminderType,
        validFrom: reminderData.validFrom,
        validUntil: reminderData.validUntil
    };
    
    return generateContentBasedIdempotencyKey(userId, keyFields, 'complex');
}

/**
 * 存储幂等键到本地缓存
 * 用于避免短时间内的重复请求
 * 
 * @param {string} key - 幂等键
 * @param {number} ttl - 生存时间（毫秒），默认5分钟
 */
export function cacheIdempotencyKey(key, ttl = 5 * 60 * 1000) {
    const expireTime = Date.now() + ttl;
    const cacheData = {
        key: key,
        expireTime: expireTime
    };
    
    try {
        uni.setStorageSync(`idempotency_${key}`, JSON.stringify(cacheData));
    } catch (e) {
        console.warn('缓存幂等键失败:', e);
    }
}

/**
 * 检查幂等键是否已在本地缓存中
 * 
 * @param {string} key - 幂等键
 * @returns {boolean} 是否已缓存且未过期
 */
export function isIdempotencyKeyCached(key) {
    try {
        const cacheDataStr = uni.getStorageSync(`idempotency_${key}`);
        if (!cacheDataStr) {
            return false;
        }
        
        const cacheData = JSON.parse(cacheDataStr);
        const now = Date.now();
        
        if (now > cacheData.expireTime) {
            // 已过期，清除缓存
            uni.removeStorageSync(`idempotency_${key}`);
            return false;
        }
        
        return true;
    } catch (e) {
        console.warn('检查幂等键缓存失败:', e);
        return false;
    }
}

/**
 * 清理过期的幂等键缓存
 */
export function cleanExpiredIdempotencyKeys() {
    try {
        const info = uni.getStorageInfoSync();
        const now = Date.now();
        
        info.keys.forEach(key => {
            if (key.startsWith('idempotency_')) {
                try {
                    const cacheDataStr = uni.getStorageSync(key);
                    if (cacheDataStr) {
                        const cacheData = JSON.parse(cacheDataStr);
                        if (now > cacheData.expireTime) {
                            uni.removeStorageSync(key);
                        }
                    }
                } catch (e) {
                    // 如果解析失败，直接删除
                    uni.removeStorageSync(key);
                }
            }
        });
    } catch (e) {
        console.warn('清理过期幂等键缓存失败:', e);
    }
}
