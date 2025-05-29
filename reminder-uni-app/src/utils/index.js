// 工具函数统一导出
export * from './date.js';
export * from './validation.js';

// 原有的工具函数保留
export const debounce = (func, wait) => {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
};

// 节流函数
export const throttle = (func, limit) => {
    let inThrottle;
    return function() {
        const args = arguments;
        const context = this;
        if (!inThrottle) {
            func.apply(context, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
};

// 解析JWT token
export const parseJwt = (token) => {
    try {
        const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(
            uni.arrayBufferToBase64(Array.from(atob(base64)).map(c => c.charCodeAt(0)))
            .split('')
            .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
            .join('')
        );
        return JSON.parse(jsonPayload);
    } catch (e) {
        console.error('解析token失败:', e);
        return null;
    }
};

// 检查token是否过期
export const isTokenExpired = (token) => {
    const decoded = parseJwt(token);
    if (!decoded) return true;
    return decoded.exp * 1000 < Date.now();
};

// 统一处理请求错误
export const handleApiError = (error) => {
    let message = '请求失败';
    
    if (error.errMsg) {
        message = error.errMsg;
    } else if (error.data && error.data.message) {
        message = error.data.message;
    }
    
    // 在uni-app中显示错误提示
    uni.showToast({
        title: message,
        icon: 'none',
        duration: 3000
    });
    
    return message;
};

// 深拷贝
export const deepClone = (obj) => {
    if (obj === null || typeof obj !== 'object') return obj;
    if (obj instanceof Date) return new Date(obj.getTime());
    if (obj instanceof Array) return obj.map(item => deepClone(item));
    if (typeof obj === 'object') {
        const clonedObj = {};
        for (const key in obj) {
            if (obj.hasOwnProperty(key)) {
                clonedObj[key] = deepClone(obj[key]);
            }
        }
        return clonedObj;
    }
};

// 生成唯一ID
export const generateId = () => {
    return Date.now().toString(36) + Math.random().toString(36).substr(2);
};

// 存储操作封装
export const storage = {
    set(key, value) {
        try {
            uni.setStorageSync(key, JSON.stringify(value));
            return true;
        } catch (e) {
            console.error('存储失败:', e);
            return false;
        }
    },
    
    get(key, defaultValue = null) {
        try {
            const value = uni.getStorageSync(key);
            return value ? JSON.parse(value) : defaultValue;
        } catch (e) {
            console.error('读取失败:', e);
            return defaultValue;
        }
    },
    
    remove(key) {
        try {
            uni.removeStorageSync(key);
            return true;
        } catch (e) {
            console.error('删除失败:', e);
            return false;
        }
    },
    
    clear() {
        try {
            uni.clearStorageSync();
            return true;
        } catch (e) {
            console.error('清空失败:', e);
            return false;
        }
    }
};

// 数组工具函数
export const arrayUtils = {
    // 数组去重
    unique: (arr, key = null) => {
        if (key) {
            const seen = new Set();
            return arr.filter(item => {
                const keyValue = item[key];
                if (seen.has(keyValue)) {
                    return false;
                }
                seen.add(keyValue);
                return true;
            });
        }
        return [...new Set(arr)];
    },
    
    // 数组分组
    groupBy: (arr, key) => {
        return arr.reduce((groups, item) => {
            const groupKey = typeof key === 'function' ? key(item) : item[key];
            if (!groups[groupKey]) {
                groups[groupKey] = [];
            }
            groups[groupKey].push(item);
            return groups;
        }, {});
    },
    
    // 数组排序
    sortBy: (arr, key, order = 'asc') => {
        return [...arr].sort((a, b) => {
            const aVal = typeof key === 'function' ? key(a) : a[key];
            const bVal = typeof key === 'function' ? key(b) : b[key];
            
            if (order === 'desc') {
                return bVal > aVal ? 1 : -1;
            }
            return aVal > bVal ? 1 : -1;
        });
    }
};

// 对象工具函数
export const objectUtils = {
    // 深度合并对象
    deepMerge: (target, source) => {
        const result = { ...target };
        
        for (const key in source) {
            if (source.hasOwnProperty(key)) {
                if (typeof source[key] === 'object' && !Array.isArray(source[key]) && source[key] !== null) {
                    result[key] = objectUtils.deepMerge(result[key] || {}, source[key]);
                } else {
                    result[key] = source[key];
                }
            }
        }
        
        return result;
    },
    
    // 选择对象的部分属性
    pick: (obj, keys) => {
        const result = {};
        keys.forEach(key => {
            if (obj.hasOwnProperty(key)) {
                result[key] = obj[key];
            }
        });
        return result;
    },
    
    // 排除对象的某些属性
    omit: (obj, keys) => {
        const result = { ...obj };
        keys.forEach(key => {
            delete result[key];
        });
        return result;
    }
};