/**
 * 带缓存的API服务
 * 在现有API基础上添加智能缓存层，不破坏现有逻辑
 */

import ReminderCacheService from './reminderCache.js';
import * as originalApi from './api.js';

/**
 * 获取当前用户ID
 * @returns {string|null} 用户ID
 */
function getCurrentUserId() {
  try {
    const currentUser = ReminderCacheService.getCurrentUser();
    if (currentUser?.id) {
      return currentUser.id.toString();
    }

    // 备用方案：从缓存获取
    const cachedData = uni.getStorageSync('user_profile_cache');
    if (cachedData) {
      const parsed = JSON.parse(cachedData);
      if (parsed.user?.id) {
        return parsed.user.id.toString();
      }
    }

    return null;
  } catch (error) {
    console.warn('获取用户ID失败:', error);
    return null;
  }
}

/**
 * 缓存增强的API服务
 */
export const CachedApiService = {
  
  /**
   * 获取即将到来的提醒（带缓存）
   * @returns {Promise<Array>} 提醒列表
   */
  async getUpcomingReminders() {
    const userId = getCurrentUserId();
    if (!userId) {
      console.warn('无法获取用户ID，跳过缓存');
      return originalApi.getUpcomingReminders();
    }
    
    const cacheParams = { userId };
    
    // 尝试从缓存获取
    const cached = ReminderCacheService.getCache('upcoming', cacheParams);
    if (cached) {
      console.log('✅ 使用缓存的即将到来的提醒');
      return cached;
    }
    
    // 缓存未命中，从API获取
    console.log('📡 从API获取即将到来的提醒');
    try {
      const response = await originalApi.getUpcomingReminders();
      const data = Array.isArray(response) ? response : (response.data || []);
      
      // 缓存结果
      ReminderCacheService.setCache('upcoming', cacheParams, data);
      
      return data;
    } catch (error) {
      console.error('获取即将到来的提醒失败:', error);
      throw error;
    }
  },
  
  /**
   * 获取简单提醒（带缓存）
   * @param {number} year - 年份
   * @param {number} month - 月份
   * @returns {Promise<Array>} 提醒列表
   */
  async getSimpleReminders(year, month) {
    const userId = getCurrentUserId();
    if (!userId) {
      console.warn('无法获取用户ID，跳过缓存');
      return originalApi.getAllSimpleReminders(year, month);
    }
    
    const cacheParams = { userId, year, month };
    
    // 尝试从缓存获取
    const cached = ReminderCacheService.getCache('simple', cacheParams);
    if (cached) {
      console.log(`✅ 使用缓存的简单提醒 ${year}-${month}`);
      return cached;
    }
    
    // 缓存未命中，从API获取
    console.log(`📡 从API获取简单提醒 ${year}-${month}`);
    try {
      const response = await originalApi.getAllSimpleReminders(year, month);
      const data = Array.isArray(response) ? response : (response.data || []);
      
      // 缓存结果
      ReminderCacheService.setCache('simple', cacheParams, data);
      
      return data;
    } catch (error) {
      console.error('获取简单提醒失败:', error);
      throw error;
    }
  },
  
  /**
   * 获取复杂提醒（带缓存）
   * @returns {Promise<Array>} 提醒列表
   */
  async getComplexReminders() {
    const userId = getCurrentUserId();
    if (!userId) {
      console.warn('无法获取用户ID，跳过缓存');
      return originalApi.getAllComplexReminders();
    }
    
    const cacheParams = { userId };
    
    // 尝试从缓存获取
    const cached = ReminderCacheService.getCache('complex', cacheParams);
    if (cached) {
      console.log('✅ 使用缓存的复杂提醒');
      return cached;
    }
    
    // 缓存未命中，从API获取
    console.log('📡 从API获取复杂提醒');
    try {
      const response = await originalApi.getAllComplexReminders();
      const data = Array.isArray(response) ? response : (response.data || []);
      
      // 缓存结果
      ReminderCacheService.setCache('complex', cacheParams, data);
      
      return data;
    } catch (error) {
      console.error('获取复杂提醒失败:', error);
      throw error;
    }
  },
  
  /**
   * 创建简单提醒（自动清理相关缓存）
   * @param {Object} reminder - 提醒数据
   * @returns {Promise} API响应
   */
  async createSimpleReminder(reminder) {
    try {
      const response = await originalApi.createEvent(reminder);
      
      // 清理相关缓存
      this.invalidateSimpleReminderCaches(reminder);
      
      console.log('✅ 简单提醒创建成功，已清理相关缓存');
      return response;
    } catch (error) {
      console.error('创建简单提醒失败:', error);
      throw error;
    }
  },
  
  /**
   * 更新简单提醒（自动清理相关缓存）
   * @param {string|number} id - 提醒ID
   * @param {Object} reminder - 提醒数据
   * @returns {Promise} API响应
   */
  async updateSimpleReminder(id, reminder) {
    try {
      const response = await originalApi.updateEvent(id, reminder);
      
      // 清理相关缓存
      this.invalidateSimpleReminderCaches(reminder);
      
      console.log('✅ 简单提醒更新成功，已清理相关缓存');
      return response;
    } catch (error) {
      console.error('更新简单提醒失败:', error);
      throw error;
    }
  },
  
  /**
   * 删除简单提醒（自动清理相关缓存）
   * @param {string|number} id - 提醒ID
   * @returns {Promise} API响应
   */
  async deleteSimpleReminder(id) {
    try {
      const response = await originalApi.deleteEvent(id);
      
      // 清理所有简单提醒缓存（因为不知道具体的年月）
      this.invalidateAllSimpleReminderCaches();
      
      console.log('✅ 简单提醒删除成功，已清理相关缓存');
      return response;
    } catch (error) {
      console.error('删除简单提醒失败:', error);
      throw error;
    }
  },
  
  /**
   * 创建复杂提醒（自动清理相关缓存）
   * @param {Object} reminder - 提醒数据
   * @param {string} idempotencyKey - 幂等键
   * @returns {Promise} API响应
   */
  async createComplexReminder(reminder, idempotencyKey) {
    try {
      const response = await originalApi.createComplexReminder(reminder, idempotencyKey);
      
      // 清理复杂提醒缓存
      this.invalidateComplexReminderCaches();
      
      console.log('✅ 复杂提醒创建成功，已清理相关缓存');
      return response;
    } catch (error) {
      console.error('创建复杂提醒失败:', error);
      throw error;
    }
  },
  
  /**
   * 更新复杂提醒（自动清理相关缓存）
   * @param {string|number} id - 提醒ID
   * @param {Object} reminder - 提醒数据
   * @returns {Promise} API响应
   */
  async updateComplexReminder(id, reminder) {
    try {
      const response = await originalApi.updateComplexReminder(id, reminder);
      
      // 清理复杂提醒缓存
      this.invalidateComplexReminderCaches();
      
      console.log('✅ 复杂提醒更新成功，已清理相关缓存');
      return response;
    } catch (error) {
      console.error('更新复杂提醒失败:', error);
      throw error;
    }
  },
  
  /**
   * 删除复杂提醒（自动清理相关缓存）
   * @param {string|number} id - 提醒ID
   * @returns {Promise} API响应
   */
  async deleteComplexReminder(id) {
    try {
      const response = await originalApi.deleteComplexReminder(id);
      
      // 清理复杂提醒缓存
      this.invalidateComplexReminderCaches();
      
      console.log('✅ 复杂提醒删除成功，已清理相关缓存');
      return response;
    } catch (error) {
      console.error('删除复杂提醒失败:', error);
      throw error;
    }
  },
  
  /**
   * 清理简单提醒相关缓存
   * @param {Object} reminder - 提醒数据（可选）
   */
  invalidateSimpleReminderCaches(reminder = null) {
    const userId = getCurrentUserId();
    if (!userId) return;
    
    // 清理即将到来的提醒缓存
    ReminderCacheService.clearCache('upcoming', { userId });
    
    if (reminder && reminder.eventTime) {
      // 如果有具体的提醒数据，清理对应年月的缓存
      try {
        const eventDate = new Date(reminder.eventTime);
        const year = eventDate.getFullYear();
        const month = eventDate.getMonth() + 1;
        
        ReminderCacheService.clearCache('simple', { userId, year, month });
        console.log(`🧹 已清理 ${year}-${month} 的简单提醒缓存`);
      } catch (error) {
        console.warn('解析提醒时间失败，清理所有简单提醒缓存:', error);
        this.invalidateAllSimpleReminderCaches();
      }
    } else {
      // 清理所有简单提醒缓存
      this.invalidateAllSimpleReminderCaches();
    }
  },
  
  /**
   * 清理所有简单提醒缓存
   */
  invalidateAllSimpleReminderCaches() {
    const userId = getCurrentUserId();
    if (!userId) return;
    
    ReminderCacheService.clearCache('simple');
    ReminderCacheService.clearCache('upcoming', { userId });
    console.log('🧹 已清理所有简单提醒缓存');
  },
  
  /**
   * 清理复杂提醒相关缓存
   */
  invalidateComplexReminderCaches() {
    const userId = getCurrentUserId();
    if (!userId) return;
    
    ReminderCacheService.clearCache('complex', { userId });
    console.log('🧹 已清理复杂提醒缓存');
  },
  
  /**
   * 清理所有缓存
   */
  invalidateAllCaches() {
    ReminderCacheService.clearAllCache();
    console.log('🧹 已清理所有提醒缓存');
  },
  
  /**
   * 获取缓存统计信息
   * @returns {Object} 缓存统计
   */
  getCacheStats() {
    return ReminderCacheService.getCacheStats();
  }
};

// 导出兼容的API函数（保持现有代码兼容性）
export const getUpcomingReminders = () => CachedApiService.getUpcomingReminders();
export const getAllSimpleReminders = (year, month) => CachedApiService.getSimpleReminders(year, month);
export const getAllComplexReminders = () => CachedApiService.getComplexReminders();

export const createEvent = (reminder) => CachedApiService.createSimpleReminder(reminder);
export const updateEvent = (id, reminder) => CachedApiService.updateSimpleReminder(id, reminder);
export const deleteEvent = (id) => CachedApiService.deleteSimpleReminder(id);

export const createComplexReminder = (reminder, idempotencyKey) => 
  CachedApiService.createComplexReminder(reminder, idempotencyKey);
export const updateComplexReminder = (id, reminder) => 
  CachedApiService.updateComplexReminder(id, reminder);
export const deleteComplexReminder = (id) => 
  CachedApiService.deleteComplexReminder(id);

export default CachedApiService;
