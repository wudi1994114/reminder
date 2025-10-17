/**
 * reminderCache.js - 兼容层
 * 
 * 此文件作为过渡期兼容层，将旧的API调用转发到新的服务
 * 未来版本将移除此文件，请直接使用新的服务：
 * - UserService (用户相关)
 * - ReminderService (提醒相关)
 * - CacheService (缓存相关)
 */

// 导入新的服务
import { UserService, userState } from './userService.js';
import ReminderService, { updateDataVersion, globalDataVersion } from './reminderService.js';
import CacheService from './cacheService.js';

// 导出用户状态（保持向后兼容）
export { userState, globalDataVersion, updateDataVersion };

/**
 * ReminderCacheService - 兼容类
 * 所有方法调用都转发到新的服务
 */
class ReminderCacheService {

  // ==================== 用户服务方法 ====================

  static async init() {
    return await UserService.init();
  }
  
  static async fetchUserProfile() {
    return await UserService.fetchUserProfile();
  }
  
  static async updateUserProfile(profileData) {
    return await UserService.updateUserProfile(profileData);
  }
  
  static async onLoginSuccess(loginResponse, loginType = 'unknown') {
    return await UserService.onLoginSuccess(loginResponse, loginType);
  }
  
  static logout() {
    return UserService.logout();
  }

  static getCurrentUser() {
    return UserService.getCurrentUser();
  }

  static getUserState() {
    return UserService.getUserState();
  }

  static setUserInfo(userInfo) {
    return UserService.setUserInfo(userInfo);
  }

  static clearUserState() {
    console.warn('[Deprecated] clearUserState is deprecated, use UserService.clearUserInfo()');
    return UserService.clearUserInfo();
  }
  
  static clearUserInfo() {
    return UserService.clearUserInfo();
  }
  
  static async refreshUserProfile() {
    return await UserService.refreshUserProfile();
  }
  
  // ==================== 缓存服务方法 ====================
  
  static generateCacheKey(type, params = {}) {
    return CacheService.generateKey(type, params);
  }
  
  static getCache(type, params = {}) {
    const key = CacheService.generateKey(type, params);
    return CacheService.get(key, globalDataVersion.value);
  }
  
  static setCache(type, params = {}, data) {
    const key = CacheService.generateKey(type, params);
    const ttl = this.getTTLForType(type);
    return CacheService.set(key, data, ttl, globalDataVersion.value);
  }
  
  static clearCache(type, params = null) {
    if (params) {
      const key = CacheService.generateKey(type, params);
      return CacheService.remove(key);
    } else {
      return CacheService.clearNamespace(type);
    }
  }
  
  static clearAllCache() {
    return ReminderService.clearAllCache();
  }
  
  static getCacheStats() {
    return ReminderService.getCacheStats();
  }
  
  // ==================== 提醒服务方法 ====================
  
  static async getUpcomingReminders(apiCall) {
    return await ReminderService.getUpcomingReminders(apiCall);
  }
  
  static async getSimpleReminders(year, month, apiCall) {
    return await ReminderService.getSimpleReminders(year, month, apiCall);
  }
  
  static async getComplexReminders(apiCall) {
    return await ReminderService.getComplexReminders(apiCall);
  }
  
  static clearUpcomingCache() {
    return ReminderService.clearUpcomingCache();
  }
  
  static clearSimpleRemindersCache(year = null, month = null) {
    return ReminderService.clearSimpleRemindersCache(year, month);
  }
  
  static clearComplexRemindersCache() {
    return ReminderService.clearComplexRemindersCache();
  }
  
  // ==================== 辅助方法 ====================
  
  static getTTLForType(type) {
    const TTL_MAP = {
      'upcoming': 2 * 60 * 1000,    // 2分钟
      'simple': 5 * 60 * 1000,      // 5分钟
      'complex': 10 * 60 * 1000,    // 10分钟
      'user_profile': 30 * 60 * 1000, // 30分钟
      'user_tags': 15 * 60 * 1000   // 15分钟
    };
    return TTL_MAP[type] || 5 * 60 * 1000;
  }
  
  // ==================== 兼容旧的缓存键名 ====================
  
  static setUserProfileCache(userId, userInfo) {
    const key = `user_profile:${userId}`;
    const ttl = 30 * 60 * 1000;
    return CacheService.set(key, userInfo, ttl);
  }
  
  static getUserProfileCache(userId) {
    const key = `user_profile:${userId}`;
    return CacheService.get(key);
  }
  
  static clearUserCache(userId = null) {
    if (userId) {
      CacheService.clearNamespace(`user_profile:${userId}`);
      CacheService.clearNamespace(`user_tags:${userId}`);
    } else {
      CacheService.clearNamespace('user_profile');
      CacheService.clearNamespace('user_tags');
    }
  }
  
  static setUserTagsCache(userId, tags) {
    const key = `user_tags:${userId}`;
    const ttl = 15 * 60 * 1000;
    return CacheService.set(key, tags, ttl);
  }
  
  static getUserTagsCache(userId) {
    const key = `user_tags:${userId}`;
    return CacheService.get(key);
  }
  
  static loadUserFromCache() {
    console.warn('[Deprecated] loadUserFromCache is deprecated, use UserService.loadFromCache()');
    return UserService.loadFromCache();
  }
  
  static isValidCache(cacheItem) {
    return !CacheService.isExpired(cacheItem);
  }
  
  static cleanExpiredStorageCache(storedData) {
    console.warn('[Deprecated] cleanExpiredStorageCache is deprecated');
    return storedData;
  }
}

// 默认导出（保持向后兼容）
export default ReminderCacheService;

// 命名导出（保持向后兼容）
export { ReminderCacheService };

console.log('%c[Compat] reminderCache.js 兼容层已加载', 'color: #FF9800; font-weight: bold;');
console.log('%c[Compat] 请尽快迁移到新的服务: UserService, ReminderService, CacheService', 'color: #FF9800;');
