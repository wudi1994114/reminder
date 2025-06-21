/**
 * 提醒数据缓存服务
 * 提供简单和复杂提醒的智能缓存管理
 * 现在也包含用户状态管理和认证功能
 */

import { reactive } from 'vue';
import { request } from './api';

// 缓存配置
const CACHE_CONFIG = {
  // 缓存有效期（毫秒）
  SIMPLE_REMINDERS_TTL: 5 * 60 * 1000, // 5分钟
  COMPLEX_REMINDERS_TTL: 10 * 60 * 1000, // 10分钟
  UPCOMING_REMINDERS_TTL: 2 * 60 * 1000, // 2分钟
  USER_PROFILE_TTL: 30 * 60 * 1000, // 30分钟
  USER_TAGS_TTL: 15 * 60 * 1000, // 15分钟

  // 存储键名
  STORAGE_KEYS: {
    SIMPLE_REMINDERS: 'reminder_cache_simple',
    COMPLEX_REMINDERS: 'reminder_cache_complex',
    UPCOMING_REMINDERS: 'reminder_cache_upcoming',
    USER_PROFILE: 'user_profile_cache',
    USER_TAGS: 'user_tags_cache'
  },

  // 最大缓存条目数
  MAX_CACHE_ITEMS: 1000
};

// 用户状态管理（从 userService 迁移过来）
export const userState = reactive({
  user: null,
  isAuthenticated: false,
  loading: false,
  error: null
});

// 内存缓存
const memoryCache = {
  simpleReminders: new Map(),
  complexReminders: new Map(),
  upcomingReminders: new Map(),
  userProfile: new Map(),
  userTags: new Map()
};

/**
 * 缓存工具类
 * 现在也包含用户服务功能
 */
class ReminderCacheService {

  // ==================== 用户服务方法（从 userService 迁移） ====================

  /**
   * 初始化用户服务
   */
  static async init() {
    console.log('🚀 ReminderCacheService: 初始化用户服务');

    try {
      // 检查是否有token
      const token = uni.getStorageSync('accessToken');
      if (!token) {
        console.log('📝 ReminderCacheService: 没有token，清除用户状态');
        this.clearUserState(); // 只清除用户状态，不清除token
        return;
      }

      // 尝试从缓存加载用户信息
      const cachedUser = this.loadUserFromCache();
      if (cachedUser) {
        console.log('✅ ReminderCacheService: 从有效缓存中恢复用户信息成功');
        this.setUserInfo(cachedUser);
      } else {
        console.log('📝 ReminderCacheService: 无有效缓存，但存在Token，尝试从服务器恢复会话...');
        await this.fetchUserProfile();
      }
    } catch (error) {
      console.error('❌ ReminderCacheService: 初始化失败:', error);
      // 初始化失败时只清除用户状态，保留token以便下次重试
      this.clearUserState();
    }
  }

  /**
   * 从服务器获取用户信息
   */
  static async fetchUserProfile() {
    if (userState.loading) {
      console.log('⏳ ReminderCacheService: 正在获取用户信息，跳过重复请求');
      return false;
    }

    try {
      userState.loading = true;
      userState.error = null;
      console.log('📡 ReminderCacheService: 开始从服务器获取用户信息');

      const response = await request({
        url: '/auth/profile',
        method: 'GET'
      });

      if (response && response.id) {
        console.log('✅ ReminderCacheService: 获取用户信息成功:', {
          id: response.id,
          username: response.username,
          nickname: response.nickname
        });

        this.setUserInfo(response);
        this.setUserProfileCache(response.id.toString(), response);
        return true;
      } else {
        throw new Error('服务器返回的用户数据无效');
      }

    } catch (error) {
      console.error('❌ ReminderCacheService: 获取用户信息失败:', error);
      userState.error = error.message || '获取用户信息失败';

      // 如果是认证错误，只清除用户状态，保留Token
      if (error.statusCode === 401 || error.statusCode === 403) {
        console.log('🔐 ReminderCacheService: 认证失败，清除用户状态但保留Token');
        this.clearUserState();
      }
      return false;
    } finally {
      userState.loading = false;
    }
  }

  /**
   * 更新用户信息
   */
  static async updateUserProfile(profileData) {
    try {
      userState.loading = true;
      userState.error = null;
      console.log('📝 ReminderCacheService: 更新用户信息:', profileData);

      const response = await request({
        url: '/auth/profile',
        method: 'PUT',
        data: profileData
      });

      if (response && response.id) {
        console.log('✅ ReminderCacheService: 更新用户信息成功');
        this.setUserInfo(response);
        this.setUserProfileCache(response.id.toString(), response);
        return { success: true, data: response };
      } else {
        throw new Error('服务器返回的更新后数据无效');
      }

    } catch (error) {
      console.error('❌ ReminderCacheService: 更新用户信息失败:', error);
      userState.error = error.message || '更新用户信息失败';
      return { success: false, error: userState.error };
    } finally {
      userState.loading = false;
    }
  }

  /**
   * 登录成功后的处理
   */
  static async onLoginSuccess(loginResponse, loginType = 'unknown') {
    console.log('🎉 ReminderCacheService: 处理登录成功, 登录类型:', loginType);

    // 1. 保存token
    if (loginResponse.accessToken) {
      const token = loginResponse.accessToken.startsWith('Bearer ')
        ? loginResponse.accessToken
        : `Bearer ${loginResponse.accessToken}`;
      uni.setStorageSync('accessToken', token);
      console.log('✅ ReminderCacheService: Token已保存');
    }

    // 保存登录类型
    uni.setStorageSync('loginType', loginType);
    console.log('✅ ReminderCacheService: 登录类型已保存:', loginType);

    // 2. 从服务器获取最新的用户信息
    console.log('📡 ReminderCacheService: 登录成功，立即获取用户信息');
    const success = await this.fetchUserProfile();

    if (success) {
      console.log('✅ ReminderCacheService: 登录流程完成，用户信息已更新');

      // 发送登录成功事件
      uni.$emit('userLoginSuccess', userState.user);
      console.log('🎉 ReminderCacheService: 已发送用户登录成功事件');

      return userState.user;
    } else {
      console.error('❌ ReminderCacheService: 登录成功但获取用户信息失败');
      throw new Error('登录成功但获取用户信息失败');
    }
  }

  /**
   * 登出处理
   */
  static logout() {
    console.log('👋 ReminderCacheService: 处理登出');
    this.clearUserInfo();
  }

  /**
   * 获取当前用户信息
   */
  static getCurrentUser() {
    return userState.user;
  }

  /**
   * 获取用户状态（响应式）
   */
  static getUserState() {
    return userState;
  }

  /**
   * 设置用户信息
   */
  static setUserInfo(userInfo) {
    userState.user = userInfo;
    userState.isAuthenticated = true;
    userState.error = null;
    console.log('💾 ReminderCacheService: 用户信息已更新到状态');
  }

  /**
   * 清除用户状态（不清除Token）
   * 用于网络错误、临时认证失败等情况
   */
  static clearUserState() {
    const userId = userState.user?.id?.toString();

    userState.user = null;
    userState.isAuthenticated = false;
    userState.error = null;

    // 清除统一缓存中的用户信息
    if (userId) {
      this.clearUserCache(userId);
    } else {
      // 如果没有用户ID，清除所有用户缓存
      this.clearUserCache();
    }

    // 只清除用户信息缓存，保留Token
    uni.removeStorageSync(CACHE_CONFIG.STORAGE_KEYS.USER_PROFILE);

    console.log('🧹 ReminderCacheService: 用户状态已清除，Token保留');
  }

  /**
   * 清除用户信息（包括Token）
   * 仅用于用户主动登出
   */
  static clearUserInfo() {
    const userId = userState.user?.id?.toString();

    userState.user = null;
    userState.isAuthenticated = false;
    userState.error = null;

    // 清除统一缓存中的用户信息
    if (userId) {
      this.clearUserCache(userId);
    } else {
      // 如果没有用户ID，清除所有用户缓存
      this.clearUserCache();
    }

    // 清除所有存储
    uni.removeStorageSync('accessToken');
    uni.removeStorageSync(CACHE_CONFIG.STORAGE_KEYS.USER_PROFILE);
    uni.removeStorageSync('loginType');

    console.log('🗑️ ReminderCacheService: 所有用户信息、Token和登录类型已清除');
  }

  /**
   * 从缓存加载用户信息
   */
  static loadUserFromCache() {
    try {
      // 尝试获取当前用户ID
      let userId = null;
      if (userState.user?.id) {
        userId = userState.user.id.toString();
      } else {
        // 如果没有用户状态，尝试从token中获取（这里简化处理）
        const token = uni.getStorageSync('accessToken');
        if (token) {
          // 这里可以解析token获取用户ID，暂时使用通用缓存
          userId = 'current';
        }
      }

      if (!userId) {
        console.log('📝 ReminderCacheService: 无法确定用户ID，跳过缓存加载');
        return null;
      }

      // 使用统一缓存服务
      const cachedUser = this.getUserProfileCache(userId);
      if (cachedUser) {
        console.log(`✅ ReminderCacheService: 从统一缓存加载用户信息成功`);
        return cachedUser;
      }

      // 兼容旧缓存格式
      const cachedData = uni.getStorageSync(CACHE_CONFIG.STORAGE_KEYS.USER_PROFILE);
      if (!cachedData) {
        return null;
      }

      const parsed = JSON.parse(cachedData);
      const now = Date.now();

      // 检查缓存是否过期
      if (now - parsed.timestamp > CACHE_CONFIG.USER_PROFILE_TTL) {
        console.log('⏰ ReminderCacheService: 缓存已过期');
        uni.removeStorageSync(CACHE_CONFIG.STORAGE_KEYS.USER_PROFILE);
        return null;
      }

      console.log(`✅ ReminderCacheService: 从旧缓存加载用户信息成功`);

      // 迁移到新缓存格式
      if (parsed.user && parsed.user.id) {
        this.setUserProfileCache(parsed.user.id.toString(), parsed.user);
        console.log('🔄 ReminderCacheService: 已迁移到新缓存格式');
      }

      return parsed.user;

    } catch (error) {
      console.error('❌ ReminderCacheService: 加载缓存失败:', error);
      uni.removeStorageSync(CACHE_CONFIG.STORAGE_KEYS.USER_PROFILE);
      return null;
    }
  }

  /**
   * 强制刷新用户信息
   */
  static async refreshUserProfile() {
    console.log('🔄 ReminderCacheService: 手动强制刷新用户信息');
    return await this.fetchUserProfile();
  }

  // ==================== 原有的缓存方法 ====================
  
  /**
   * 生成缓存键
   * @param {string} type - 缓存类型 (simple|complex|upcoming)
   * @param {Object} params - 参数对象
   * @returns {string} 缓存键
   */
  static generateCacheKey(type, params = {}) {
    const { userId, year, month, ...otherParams } = params;
    
    let key = `${type}`;
    if (userId) key += `_user_${userId}`;
    if (year && month) key += `_${year}_${month}`;
    
    // 添加其他参数
    const sortedParams = Object.keys(otherParams).sort();
    if (sortedParams.length > 0) {
      const paramStr = sortedParams.map(k => `${k}_${otherParams[k]}`).join('_');
      key += `_${paramStr}`;
    }
    
    return key;
  }
  
  /**
   * 获取缓存数据
   * @param {string} type - 缓存类型
   * @param {Object} params - 参数
   * @returns {Object|null} 缓存的数据或null
   */
  static getCache(type, params = {}) {
    const cacheKey = this.generateCacheKey(type, params);
    
    // 1. 先检查内存缓存
    const memCache = memoryCache[`${type}Reminders`];
    if (memCache && memCache.has(cacheKey)) {
      const cached = memCache.get(cacheKey);
      if (this.isValidCache(cached)) {
        console.log(`✅ 内存缓存命中: ${cacheKey}`);
        return cached.data;
      } else {
        // 内存缓存过期，删除
        memCache.delete(cacheKey);
      }
    }
    
    // 2. 检查存储缓存
    try {
      const storageKey = CACHE_CONFIG.STORAGE_KEYS[`${type.toUpperCase()}_REMINDERS`];
      const storedData = uni.getStorageSync(storageKey);
      
      if (storedData) {
        const parsed = JSON.parse(storedData);
        if (parsed[cacheKey] && this.isValidCache(parsed[cacheKey])) {
          console.log(`✅ 存储缓存命中: ${cacheKey}`);
          
          // 恢复到内存缓存
          memCache.set(cacheKey, parsed[cacheKey]);
          return parsed[cacheKey].data;
        }
      }
    } catch (error) {
      console.warn('读取存储缓存失败:', error);
    }
    
    console.log(`❌ 缓存未命中: ${cacheKey}`);
    return null;
  }
  
  /**
   * 设置缓存数据
   * @param {string} type - 缓存类型
   * @param {Object} params - 参数
   * @param {Array} data - 要缓存的数据
   */
  static setCache(type, params = {}, data) {
    const cacheKey = this.generateCacheKey(type, params);
    const ttl = CACHE_CONFIG[`${type.toUpperCase()}_REMINDERS_TTL`];
    
    const cacheItem = {
      data: data,
      timestamp: Date.now(),
      ttl: ttl,
      params: params
    };
    
    // 1. 设置内存缓存
    const memCache = memoryCache[`${type}Reminders`];
    memCache.set(cacheKey, cacheItem);
    
    // 限制内存缓存大小
    if (memCache.size > CACHE_CONFIG.MAX_CACHE_ITEMS) {
      const firstKey = memCache.keys().next().value;
      memCache.delete(firstKey);
    }
    
    // 2. 设置存储缓存
    try {
      const storageKey = CACHE_CONFIG.STORAGE_KEYS[`${type.toUpperCase()}_REMINDERS`];
      let storedData = {};
      
      try {
        const existing = uni.getStorageSync(storageKey);
        if (existing) {
          storedData = JSON.parse(existing);
        }
      } catch (e) {
        console.warn('解析存储缓存失败，重新创建:', e);
      }
      
      storedData[cacheKey] = cacheItem;
      
      // 清理过期的存储缓存
      this.cleanExpiredStorageCache(storedData);
      
      uni.setStorageSync(storageKey, JSON.stringify(storedData));
      console.log(`💾 缓存已保存: ${cacheKey}`);
    } catch (error) {
      console.warn('保存存储缓存失败:', error);
    }
  }
  
  /**
   * 检查缓存是否有效
   * @param {Object} cacheItem - 缓存项
   * @returns {boolean} 是否有效
   */
  static isValidCache(cacheItem) {
    if (!cacheItem || !cacheItem.timestamp || !cacheItem.ttl) {
      return false;
    }
    
    const now = Date.now();
    const isValid = (now - cacheItem.timestamp) < cacheItem.ttl;
    
    if (!isValid) {
      console.log(`⏰ 缓存已过期: ${now - cacheItem.timestamp}ms > ${cacheItem.ttl}ms`);
    }
    
    return isValid;
  }
  
  /**
   * 清除特定类型的缓存
   * @param {string} type - 缓存类型
   * @param {Object} params - 参数（可选，如果不提供则清除该类型的所有缓存）
   */
  static clearCache(type, params = null) {
    if (params) {
      // 清除特定缓存
      const cacheKey = this.generateCacheKey(type, params);
      
      // 清除内存缓存
      const memCache = memoryCache[`${type}Reminders`];
      if (memCache) {
        memCache.delete(cacheKey);
      }
      
      // 清除存储缓存
      try {
        const storageKey = CACHE_CONFIG.STORAGE_KEYS[`${type.toUpperCase()}_REMINDERS`];
        const storedData = uni.getStorageSync(storageKey);
        if (storedData) {
          const parsed = JSON.parse(storedData);
          delete parsed[cacheKey];
          uni.setStorageSync(storageKey, JSON.stringify(parsed));
        }
      } catch (error) {
        console.warn('清除存储缓存失败:', error);
      }
      
      console.log(`🧹 已清除缓存: ${cacheKey}`);
    } else {
      // 清除该类型的所有缓存
      const memCache = memoryCache[`${type}Reminders`];
      if (memCache) {
        memCache.clear();
      }
      
      try {
        const storageKey = CACHE_CONFIG.STORAGE_KEYS[`${type.toUpperCase()}_REMINDERS`];
        uni.removeStorageSync(storageKey);
      } catch (error) {
        console.warn('清除存储缓存失败:', error);
      }
      
      console.log(`🧹 已清除所有${type}缓存`);
    }
  }
  
  /**
   * 清除所有缓存（包括用户缓存和提醒缓存）
   */
  static clearAllCache() {
    // 清除内存缓存
    Object.values(memoryCache).forEach(cache => cache.clear());

    // 清除存储缓存
    Object.values(CACHE_CONFIG.STORAGE_KEYS).forEach(key => {
      try {
        uni.removeStorageSync(key);
      } catch (error) {
        console.warn(`清除存储缓存失败: ${key}`, error);
      }
    });

    console.log('🧹 已清除所有缓存（包括用户缓存和提醒缓存）');
  }
  
  /**
   * 清理过期的存储缓存
   * @param {Object} storedData - 存储的缓存数据
   */
  static cleanExpiredStorageCache(storedData) {
    const now = Date.now();
    let cleanedCount = 0;
    
    Object.keys(storedData).forEach(key => {
      const item = storedData[key];
      if (!this.isValidCache(item)) {
        delete storedData[key];
        cleanedCount++;
      }
    });
    
    if (cleanedCount > 0) {
      console.log(`🧹 清理了 ${cleanedCount} 个过期的存储缓存项`);
    }
  }
  
  /**
   * 设置用户信息缓存
   * @param {string} userId - 用户ID
   * @param {Object} userInfo - 用户信息
   */
  static setUserProfileCache(userId, userInfo) {
    const cacheKey = `user_profile_${userId}`;
    const cacheItem = {
      user: userInfo,
      timestamp: Date.now(),
      ttl: CACHE_CONFIG.USER_PROFILE_TTL
    };

    // 设置内存缓存
    memoryCache.userProfile.set(cacheKey, cacheItem);

    // 设置存储缓存（兼容现有的user_profile_cache格式）
    try {
      uni.setStorageSync(CACHE_CONFIG.STORAGE_KEYS.USER_PROFILE, JSON.stringify(cacheItem));
      console.log(`💾 用户信息缓存已保存: ${userId}`);
    } catch (error) {
      console.warn('保存用户信息缓存失败:', error);
    }
  }

  /**
   * 获取用户信息缓存
   * @param {string} userId - 用户ID
   * @returns {Object|null} 用户信息或null
   */
  static getUserProfileCache(userId) {
    const cacheKey = `user_profile_${userId}`;

    // 先检查内存缓存
    if (memoryCache.userProfile.has(cacheKey)) {
      const cached = memoryCache.userProfile.get(cacheKey);
      if (this.isValidCache(cached)) {
        console.log(`✅ 用户信息内存缓存命中: ${userId}`);
        return cached.user;
      } else {
        memoryCache.userProfile.delete(cacheKey);
      }
    }

    // 检查存储缓存
    try {
      const storedData = uni.getStorageSync(CACHE_CONFIG.STORAGE_KEYS.USER_PROFILE);
      if (storedData) {
        const parsed = JSON.parse(storedData);
        if (this.isValidCache(parsed)) {
          console.log(`✅ 用户信息存储缓存命中: ${userId}`);
          // 恢复到内存缓存
          memoryCache.userProfile.set(cacheKey, parsed);
          return parsed.user;
        }
      }
    } catch (error) {
      console.warn('读取用户信息缓存失败:', error);
    }

    console.log(`❌ 用户信息缓存未命中: ${userId}`);
    return null;
  }

  /**
   * 设置用户标签缓存
   * @param {string} userId - 用户ID
   * @param {Array} tags - 标签列表
   */
  static setUserTagsCache(userId, tags) {
    const cacheKey = `user_tags_${userId}`;
    const cacheItem = {
      data: tags,
      timestamp: Date.now(),
      ttl: CACHE_CONFIG.USER_TAGS_TTL
    };

    // 设置内存缓存
    memoryCache.userTags.set(cacheKey, cacheItem);

    // 设置存储缓存
    try {
      const storageKey = CACHE_CONFIG.STORAGE_KEYS.USER_TAGS;
      let storedData = {};

      try {
        const existing = uni.getStorageSync(storageKey);
        if (existing) {
          storedData = JSON.parse(existing);
        }
      } catch (e) {
        console.warn('解析用户标签缓存失败，重新创建:', e);
      }

      storedData[cacheKey] = cacheItem;
      uni.setStorageSync(storageKey, JSON.stringify(storedData));
      console.log(`💾 用户标签缓存已保存: ${userId}`);
    } catch (error) {
      console.warn('保存用户标签缓存失败:', error);
    }
  }

  /**
   * 获取用户标签缓存
   * @param {string} userId - 用户ID
   * @returns {Array|null} 标签列表或null
   */
  static getUserTagsCache(userId) {
    const cacheKey = `user_tags_${userId}`;

    // 先检查内存缓存
    if (memoryCache.userTags.has(cacheKey)) {
      const cached = memoryCache.userTags.get(cacheKey);
      if (this.isValidCache(cached)) {
        console.log(`✅ 用户标签内存缓存命中: ${userId}`);
        return cached.data;
      } else {
        memoryCache.userTags.delete(cacheKey);
      }
    }

    // 检查存储缓存
    try {
      const storageKey = CACHE_CONFIG.STORAGE_KEYS.USER_TAGS;
      const storedData = uni.getStorageSync(storageKey);
      if (storedData) {
        const parsed = JSON.parse(storedData);
        if (parsed[cacheKey] && this.isValidCache(parsed[cacheKey])) {
          console.log(`✅ 用户标签存储缓存命中: ${userId}`);
          // 恢复到内存缓存
          memoryCache.userTags.set(cacheKey, parsed[cacheKey]);
          return parsed[cacheKey].data;
        }
      }
    } catch (error) {
      console.warn('读取用户标签缓存失败:', error);
    }

    console.log(`❌ 用户标签缓存未命中: ${userId}`);
    return null;
  }

  /**
   * 清除用户相关缓存
   * @param {string} userId - 用户ID（可选，如果不提供则清除所有用户缓存）
   */
  static clearUserCache(userId = null) {
    if (userId) {
      // 清除特定用户的缓存
      const profileKey = `user_profile_${userId}`;
      const tagsKey = `user_tags_${userId}`;

      // 清除内存缓存
      memoryCache.userProfile.delete(profileKey);
      memoryCache.userTags.delete(tagsKey);

      // 清除存储缓存
      try {
        // 用户信息缓存（直接删除，因为是单个文件）
        uni.removeStorageSync(CACHE_CONFIG.STORAGE_KEYS.USER_PROFILE);

        // 用户标签缓存（从集合中删除特定项）
        const storageKey = CACHE_CONFIG.STORAGE_KEYS.USER_TAGS;
        const storedData = uni.getStorageSync(storageKey);
        if (storedData) {
          const parsed = JSON.parse(storedData);
          delete parsed[tagsKey];
          uni.setStorageSync(storageKey, JSON.stringify(parsed));
        }
      } catch (error) {
        console.warn('清除用户存储缓存失败:', error);
      }

      console.log(`🧹 已清除用户缓存: ${userId}`);
    } else {
      // 清除所有用户缓存
      memoryCache.userProfile.clear();
      memoryCache.userTags.clear();

      try {
        uni.removeStorageSync(CACHE_CONFIG.STORAGE_KEYS.USER_PROFILE);
        uni.removeStorageSync(CACHE_CONFIG.STORAGE_KEYS.USER_TAGS);
      } catch (error) {
        console.warn('清除用户存储缓存失败:', error);
      }

      console.log('🧹 已清除所有用户缓存');
    }
  }

  /**
   * 获取缓存统计信息
   * @returns {Object} 缓存统计
   */
  static getCacheStats() {
    const stats = {
      memory: {},
      storage: {},
      total: { items: 0, size: 0 }
    };

    // 内存缓存统计
    Object.entries(memoryCache).forEach(([type, cache]) => {
      stats.memory[type] = {
        items: cache.size,
        validItems: 0
      };

      cache.forEach(item => {
        if (this.isValidCache(item)) {
          stats.memory[type].validItems++;
        }
      });

      stats.total.items += stats.memory[type].validItems;
    });

    // 存储缓存统计
    Object.entries(CACHE_CONFIG.STORAGE_KEYS).forEach(([type, key]) => {
      try {
        const data = uni.getStorageSync(key);
        if (data) {
          if (type === 'USER_PROFILE') {
            // 用户信息缓存是单个对象
            const parsed = JSON.parse(data);
            const isValid = this.isValidCache(parsed) ? 1 : 0;
            stats.storage[type] = {
              items: 1,
              validItems: isValid,
              size: JSON.stringify(data).length
            };
          } else {
            // 其他缓存是对象集合
            const parsed = JSON.parse(data);
            const validItems = Object.values(parsed).filter(item => this.isValidCache(item)).length;

            stats.storage[type] = {
              items: Object.keys(parsed).length,
              validItems: validItems,
              size: JSON.stringify(data).length
            };
          }

          stats.total.size += stats.storage[type].size;
        }
      } catch (error) {
        stats.storage[type] = { items: 0, validItems: 0, size: 0 };
      }
    });

    return stats;
  }
}

export default ReminderCacheService;
