/**
 * 通用缓存服务
 * 负责：key-value缓存管理、过期管理、存储管理
 */

// 缓存配置
const DEFAULT_TTL = 5 * 60 * 1000; // 默认5分钟
const MAX_CACHE_SIZE = 100; // 最大缓存条目数

// 内存缓存
const memoryCache = new Map();

/**
 * 缓存项结构
 * {
 *   data: any,           // 缓存数据
 *   timestamp: number,   // 创建时间
 *   ttl: number,         // 过期时间（毫秒）
 *   version: number      // 数据版本
 * }
 */

/**
 * 通用缓存服务类
 */
class CacheService {
    
    /**
     * 生成缓存键
     * @param {string} namespace - 命名空间
     * @param {Object} params - 参数对象
     * @returns {string} 缓存键
     */
    static generateKey(namespace, params = {}) {
        if (!params || Object.keys(params).length === 0) {
            return namespace;
        }
        
        const paramStr = Object.keys(params)
            .sort()
            .map(key => `${key}=${params[key]}`)
            .join('&');
        
        return `${namespace}:${paramStr}`;
    }
    
    /**
     * 设置缓存
     * @param {string} key - 缓存键
     * @param {any} data - 缓存数据
     * @param {number} ttl - 过期时间（毫秒），默认5分钟
     * @param {number} version - 数据版本
     */
    static set(key, data, ttl = DEFAULT_TTL, version = Date.now()) {
        const cacheItem = {
            data,
            timestamp: Date.now(),
            ttl,
            version
        };
        
        // 内存缓存
        memoryCache.set(key, cacheItem);
        
        // 持久化缓存
        try {
            this.saveToStorage(key, cacheItem);
        } catch (error) {
            console.warn(`缓存存储失败: ${key}`, error);
        }
        
        // 清理过期缓存（异步）
        this.cleanExpiredAsync();
    }
    
    /**
     * 获取缓存
     * @param {string} key - 缓存键
     * @param {number} minVersion - 最小版本要求
     * @returns {any} 缓存数据，未找到或过期返回null
     */
    static get(key, minVersion = 0) {
        // 先从内存获取
        let cacheItem = memoryCache.get(key);
        
        // 内存中没有，尝试从存储中加载
        if (!cacheItem) {
            cacheItem = this.loadFromStorage(key);
            if (cacheItem) {
                memoryCache.set(key, cacheItem);
            }
        }
        
        // 检查缓存是否有效
        if (!cacheItem) {
            return null;
        }
        
        // 检查版本
        if (cacheItem.version < minVersion) {
            console.log(`缓存版本过低: ${key}`, {
                cached: cacheItem.version,
                required: minVersion
            });
            this.remove(key);
            return null;
        }
        
        // 检查过期
        if (this.isExpired(cacheItem)) {
            console.log(`缓存已过期: ${key}`);
            this.remove(key);
            return null;
        }
        
        return cacheItem.data;
    }
    
    /**
     * 检查缓存是否过期
     * @param {Object} cacheItem - 缓存项
     * @returns {boolean}
     */
    static isExpired(cacheItem) {
        if (!cacheItem) return true;
        const now = Date.now();
        return (now - cacheItem.timestamp) > cacheItem.ttl;
    }
    
    /**
     * 删除缓存
     * @param {string} key - 缓存键
     */
    static remove(key) {
        memoryCache.delete(key);
        
        try {
            uni.removeStorageSync(this.getStorageKey(key));
        } catch (error) {
            console.warn(`删除存储失败: ${key}`, error);
        }
    }
    
    /**
     * 清除指定命名空间的所有缓存
     * @param {string} namespace - 命名空间
     */
    static clearNamespace(namespace) {
        console.log(`清除命名空间缓存: ${namespace}`);
        
        // 清除内存缓存
        const keysToDelete = [];
        for (const key of memoryCache.keys()) {
            if (key.startsWith(namespace)) {
                keysToDelete.push(key);
            }
        }
        keysToDelete.forEach(key => memoryCache.delete(key));
        
        // 清除存储缓存
        try {
            const storageKeys = uni.getStorageInfoSync().keys || [];
            const storagePrefix = this.getStorageKey(namespace);
            storageKeys.forEach(storageKey => {
                if (storageKey.startsWith(storagePrefix)) {
                    uni.removeStorageSync(storageKey);
                }
            });
        } catch (error) {
            console.warn(`清除存储缓存失败: ${namespace}`, error);
        }
    }
    
    /**
     * 清除所有缓存
     */
    static clearAll() {
        console.log('清除所有缓存');
        memoryCache.clear();
        
        try {
            const storageKeys = uni.getStorageInfoSync().keys || [];
            const cachePrefix = 'cache_';
            storageKeys.forEach(key => {
                if (key.startsWith(cachePrefix)) {
                    uni.removeStorageSync(key);
                }
            });
        } catch (error) {
            console.warn('清除存储失败', error);
        }
    }
    
    /**
     * 清理过期缓存（异步执行）
     */
    static cleanExpiredAsync() {
        setTimeout(() => {
            this.cleanExpired();
        }, 0);
    }
    
    /**
     * 清理过期缓存
     */
    static cleanExpired() {
        const now = Date.now();
        
        // 清理内存缓存
        for (const [key, item] of memoryCache.entries()) {
            if (this.isExpired(item)) {
                memoryCache.delete(key);
            }
        }
        
        // 限制缓存大小
        if (memoryCache.size > MAX_CACHE_SIZE) {
            // 删除最旧的条目
            const entries = Array.from(memoryCache.entries());
            entries.sort((a, b) => a[1].timestamp - b[1].timestamp);
            
            const toDelete = entries.slice(0, memoryCache.size - MAX_CACHE_SIZE);
            toDelete.forEach(([key]) => memoryCache.delete(key));
        }
    }
    
    /**
     * 获取存储键
     * @param {string} key - 缓存键
     * @returns {string} 存储键
     */
    static getStorageKey(key) {
        return `cache_${key}`;
    }
    
    /**
     * 保存到存储
     * @param {string} key - 缓存键
     * @param {Object} cacheItem - 缓存项
     */
    static saveToStorage(key, cacheItem) {
        const storageKey = this.getStorageKey(key);
        uni.setStorageSync(storageKey, JSON.stringify(cacheItem));
    }
    
    /**
     * 从存储加载
     * @param {string} key - 缓存键
     * @returns {Object|null} 缓存项
     */
    static loadFromStorage(key) {
        try {
            const storageKey = this.getStorageKey(key);
            const data = uni.getStorageSync(storageKey);
            if (!data) return null;
            
            return JSON.parse(data);
        } catch (error) {
            console.warn(`从存储加载失败: ${key}`, error);
            return null;
        }
    }
    
    /**
     * 获取缓存统计信息
     * @returns {Object} 统计信息
     */
    static getStats() {
        const memorySize = memoryCache.size;
        let storageSize = 0;
        
        try {
            const storageKeys = uni.getStorageInfoSync().keys || [];
            storageSize = storageKeys.filter(k => k.startsWith('cache_')).length;
        } catch (error) {
            console.warn('获取存储统计失败', error);
        }
        
        return {
            memory: memorySize,
            storage: storageSize,
            total: memorySize + storageSize
        };
    }
}

export default CacheService;

// 导出常用方法
export const {
    generateKey,
    set: setCache,
    get: getCache,
    remove: removeCache,
    clearNamespace,
    clearAll: clearAllCache,
    getStats: getCacheStats
} = CacheService;

