/**
 * 认证守卫工具函数
 * 用于检查用户登录状态和权限
 */

import { userState } from '../services/store.js';

/**
 * 检查用户是否已登录
 * @returns {boolean} 是否已登录
 */
export function isAuthenticated() {
  const token = localStorage.getItem('accessToken');
  return !!(token && userState.isAuthenticated);
}

/**
 * 检查token是否存在
 * @returns {boolean} token是否存在
 */
export function hasValidToken() {
  const token = localStorage.getItem('accessToken');
  return !!(token && token.length > 0);
}

/**
 * 获取当前用户信息
 * @returns {Object|null} 用户信息或null
 */
export function getCurrentUser() {
  return userState.user;
}

/**
 * 检查用户是否有特定权限
 * @param {string} permission 权限名称
 * @returns {boolean} 是否有权限
 */
export function hasPermission(permission) {
  const user = getCurrentUser();
  if (!user || !user.permissions) {
    return false;
  }
  return user.permissions.includes(permission);
}

/**
 * 要求用户登录的守卫函数
 * @param {Function} callback 登录成功后的回调函数
 * @param {Function} onUnauthorized 未登录时的回调函数
 * @returns {boolean} 是否通过验证
 */
export function requireAuth(callback = null, onUnauthorized = null) {
  if (isAuthenticated()) {
    if (callback && typeof callback === 'function') {
      callback();
    }
    return true;
  } else {
    console.warn('用户未登录，需要先登录');
    if (onUnauthorized && typeof onUnauthorized === 'function') {
      onUnauthorized();
    } else {
      // 默认行为：显示提示信息
      if (window.showNotification) {
        window.showNotification('请先登录以使用此功能', 'warning');
      } else {
        alert('请先登录以使用此功能');
      }
    }
    return false;
  }
}

/**
 * 检查token是否即将过期
 * @param {number} thresholdMinutes 提前多少分钟提醒（默认5分钟）
 * @returns {boolean} 是否即将过期
 */
export function isTokenExpiringSoon(thresholdMinutes = 5) {
  const token = localStorage.getItem('accessToken');
  if (!token) return false;
  
  try {
    // 解析JWT token（简单实现）
    const payload = JSON.parse(atob(token.split('.')[1]));
    const expirationTime = payload.exp * 1000; // 转换为毫秒
    const currentTime = Date.now();
    const thresholdTime = thresholdMinutes * 60 * 1000; // 转换为毫秒
    
    return (expirationTime - currentTime) <= thresholdTime;
  } catch (error) {
    console.warn('无法解析token过期时间:', error);
    return false;
  }
}

/**
 * 清除认证信息
 */
export function clearAuthData() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('currentUser');
  userState.isAuthenticated = false;
  userState.user = null;
}

/**
 * 页面访问守卫
 * 用于保护需要登录才能访问的页面或功能
 */
export class AuthGuard {
  constructor(options = {}) {
    this.redirectToLogin = options.redirectToLogin || this.defaultRedirectToLogin;
    this.showUnauthorizedMessage = options.showUnauthorizedMessage || this.defaultShowUnauthorizedMessage;
  }
  
  /**
   * 检查页面访问权限
   * @param {string} requiredPermission 需要的权限
   * @returns {boolean} 是否允许访问
   */
  canAccess(requiredPermission = null) {
    if (!isAuthenticated()) {
      this.showUnauthorizedMessage('请先登录');
      this.redirectToLogin();
      return false;
    }
    
    if (requiredPermission && !hasPermission(requiredPermission)) {
      this.showUnauthorizedMessage('您没有访问此功能的权限');
      return false;
    }
    
    return true;
  }
  
  /**
   * 默认的重定向到登录页面方法
   */
  defaultRedirectToLogin() {
    // 在单页面应用中，这里可以触发显示登录模态框
    console.log('需要重定向到登录页面');
  }
  
  /**
   * 默认的显示未授权消息方法
   */
  defaultShowUnauthorizedMessage(message) {
    if (window.showNotification) {
      window.showNotification(message, 'warning');
    } else {
      console.warn(message);
    }
  }
}

// 创建默认的认证守卫实例
export const authGuard = new AuthGuard();

/**
 * 装饰器函数，用于保护需要登录的方法
 * @param {Function} target 目标函数
 * @returns {Function} 包装后的函数
 */
export function requireLogin(target) {
  return function(...args) {
    if (requireAuth()) {
      return target.apply(this, args);
    }
  };
}

/**
 * Vue组合式函数，用于在组件中使用认证守卫
 */
export function useAuthGuard() {
  return {
    isAuthenticated,
    hasValidToken,
    getCurrentUser,
    hasPermission,
    requireAuth,
    isTokenExpiringSoon,
    clearAuthData,
    authGuard
  };
} 