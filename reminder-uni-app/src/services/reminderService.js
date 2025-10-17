/**
 * 提醒业务服务
 * 负责：提醒数据管理、缓存、版本控制
 */

import { ref } from 'vue';
import CacheService from './cacheService.js';

// 全局数据版本管理
export const globalDataVersion = ref(Date.now());

/**
 * 更新数据版本并清除缓存
 */
export const updateDataVersion = () => {
  const oldVersion = globalDataVersion.value;
  globalDataVersion.value = Date.now();
  
  // 获取调用栈信息
  const stack = new Error().stack;
  const caller = stack.split('\n')[2]?.trim() || 'unknown';
  
  console.log(`%c[Data Version] 数据版本更新，将清除提醒缓存`, 'color: #4CAF50; font-weight: bold;');
  console.log(`%c[Data Version] 旧版本: ${oldVersion}`, 'color: #666;');
  console.log(`%c[Data Version] 新版本: ${globalDataVersion.value}`, 'color: #4CAF50; font-weight: bold;');
  console.log(`%c[Data Version] 调用者: ${caller}`, 'color: #2196F3;');
  console.log(`%c[Data Version] 版本差异: ${globalDataVersion.value - oldVersion}ms`, 'color: #FF9800;');

  // 清除所有提醒相关的缓存
  try {
    CacheService.clearNamespace('reminder:upcoming');
    CacheService.clearNamespace('reminder:simple');
    CacheService.clearNamespace('reminder:complex');
    console.log('%c[Data Version] 所有提醒相关的缓存已清除', 'color: #4CAF50; font-weight: bold;');
  } catch(e) {
    console.warn('[Data Version] 清除缓存时出错，可能是初始化时序问题，可暂时忽略', e);
  }
};

/**
 * 提醒服务类
 * 专注于提醒数据的业务逻辑
 */
class ReminderService {
  
  /**
   * 获取即将到来的提醒（带缓存）
   */
  static async getUpcomingReminders(apiCall) {
    const cacheKey = 'reminder:upcoming';
    const ttl = 2 * 60 * 1000; // 2分钟
    
    // 尝试从缓存获取
    const cached = CacheService.get(cacheKey, globalDataVersion.value);
    if (cached) {
      console.log('✅ 从缓存获取即将到来的提醒');
      return cached;
    }
    
    // 从API获取
    console.log('📡 从API获取即将到来的提醒');
    const data = await apiCall();
    
    // 保存到缓存
    CacheService.set(cacheKey, data, ttl, globalDataVersion.value);
    
    return data;
  }
  
  /**
   * 获取简单提醒列表（带缓存）
   */
  static async getSimpleReminders(year, month, apiCall) {
    const cacheKey = CacheService.generateKey('reminder:simple', { year, month });
    const ttl = 5 * 60 * 1000; // 5分钟
    
    // 尝试从缓存获取
    const cached = CacheService.get(cacheKey, globalDataVersion.value);
    if (cached) {
      console.log('✅ 从缓存获取简单提醒列表');
      return cached;
    }
    
    // 从API获取
    console.log('📡 从API获取简单提醒列表');
    const data = await apiCall();
    
    // 保存到缓存
    CacheService.set(cacheKey, data, ttl, globalDataVersion.value);
    
    return data;
  }
  
  /**
   * 获取复杂提醒列表（带缓存）
   */
  static async getComplexReminders(apiCall) {
    const cacheKey = 'reminder:complex';
    const ttl = 10 * 60 * 1000; // 10分钟
    
    // 尝试从缓存获取
    const cached = CacheService.get(cacheKey, globalDataVersion.value);
    if (cached) {
      console.log('✅ 从缓存获取复杂提醒列表');
      return cached;
    }
    
    // 从API获取
    console.log('📡 从API获取复杂提醒列表');
    const data = await apiCall();
    
    // 保存到缓存
    CacheService.set(cacheKey, data, ttl, globalDataVersion.value);
    
    return data;
  }
  
  /**
   * 清除即将到来的提醒缓存
   */
  static clearUpcomingCache() {
    CacheService.remove('reminder:upcoming');
    console.log('🗑️ 已清除即将到来的提醒缓存');
  }
  
  /**
   * 清除简单提醒缓存
   */
  static clearSimpleRemindersCache(year = null, month = null) {
    if (year && month) {
      const cacheKey = CacheService.generateKey('reminder:simple', { year, month });
      CacheService.remove(cacheKey);
      console.log(`🗑️ 已清除简单提醒缓存 (${year}-${month})`);
    } else {
      CacheService.clearNamespace('reminder:simple');
      console.log('🗑️ 已清除所有简单提醒缓存');
    }
  }
  
  /**
   * 清除复杂提醒缓存
   */
  static clearComplexRemindersCache() {
    CacheService.remove('reminder:complex');
    console.log('🗑️ 已清除复杂提醒缓存');
  }
  
  /**
   * 清除所有提醒缓存
   */
  static clearAllCache() {
    this.clearUpcomingCache();
    this.clearSimpleRemindersCache();
    this.clearComplexRemindersCache();
    console.log('🧹 已清除所有提醒缓存');
  }
  
  /**
   * 创建简单提醒后的缓存处理
   */
  static onSimpleReminderCreated(reminder) {
    // 更新数据版本，触发全局缓存清除
    updateDataVersion();
    
    console.log('✅ 简单提醒创建成功，缓存已更新');
  }
  
  /**
   * 更新简单提醒后的缓存处理
   */
  static onSimpleReminderUpdated(reminder) {
    // 更新数据版本，触发全局缓存清除
    updateDataVersion();
    
    console.log('✅ 简单提醒更新成功，缓存已更新');
  }
  
  /**
   * 删除简单提醒后的缓存处理
   */
  static onSimpleReminderDeleted(reminderId) {
    // 更新数据版本，触发全局缓存清除
    updateDataVersion();
    
    console.log('✅ 简单提醒删除成功，缓存已更新');
  }
  
  /**
   * 创建复杂提醒后的缓存处理
   */
  static onComplexReminderCreated(reminder) {
    // 更新数据版本，触发全局缓存清除
    updateDataVersion();
    
    console.log('✅ 复杂提醒创建成功，缓存已更新');
  }
  
  /**
   * 更新复杂提醒后的缓存处理
   */
  static onComplexReminderUpdated(reminder) {
    // 更新数据版本，触发全局缓存清除
    updateDataVersion();
    
    console.log('✅ 复杂提醒更新成功，缓存已更新');
  }
  
  /**
   * 删除复杂提醒后的缓存处理
   */
  static onComplexReminderDeleted(reminderId) {
    // 更新数据版本，触发全局缓存清除
    updateDataVersion();
    
    console.log('✅ 复杂提醒删除成功，缓存已更新');
  }
  
  /**
   * 获取缓存统计信息
   */
  static getCacheStats() {
    const stats = CacheService.getStats();
    
    // 只返回提醒相关的统计
    return {
      total: stats.total,
      reminderCaches: stats.keys.filter(key => key.startsWith('reminder:')).length,
      version: globalDataVersion.value
    };
  }
}

// 导出服务类
export default ReminderService;

// 兼容旧的导出方式
export const ReminderCacheService = ReminderService;

