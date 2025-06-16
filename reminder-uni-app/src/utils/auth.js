/**
 * 认证工具函数
 * 提供统一的登录检查和一键登录弹窗功能
 */
import { reactive } from 'vue';
import { userState } from '../services/userService';

// 响应式的全局认证状态
export const globalAuthState = reactive({
  showLoginModal: false,
  loginModalResolve: null
});

/**
 * 全局用户状态管理器
 * 统一管理用户登录状态变化和数据清理
 */
export const globalUserManager = {
  // 用户登录成功处理
  onUserLogin(userInfo) {
    console.log('🎉 [全局用户管理] 用户登录成功，通知所有页面');
    
    // 发送登录成功事件
    uni.$emit('userLoginSuccess', userInfo);
    
    // 可以在这里添加其他全局登录后的处理逻辑
  },
  
  // 用户登出处理
  onUserLogout() {
    console.log('🚪 [全局用户管理] 用户登出，开始清理所有数据');
    
    // 1. 清空所有用户相关数据
    clearAllUserData();
    
    // 2. 发送全局登出事件，通知所有页面清理数据
    uni.$emit('userLogout');
    
    console.log('✅ [全局用户管理] 登出处理完成，已通知所有页面');
  },
  
  // 强制刷新所有页面数据
  refreshAllPages() {
    console.log('🔄 [全局用户管理] 强制刷新所有页面数据');
    uni.$emit('refreshAllPages');
  }
};

/**
 * 检查用户是否已登录
 * @returns {boolean} 是否已登录
 */
export function isAuthenticated() {
  const token = uni.getStorageSync('accessToken');
  return !!(token && userState.isAuthenticated);
}

/**
 * 获取当前用户信息
 * @returns {Object|null} 用户信息或null
 */
export function getCurrentUser() {
  return userState.user;
}

/**
 * 显示一键登录模态框
 * @returns {Promise<boolean>} 登录是否成功
 */
export function showOneClickLogin() {
  return new Promise((resolve) => {
    if (globalAuthState.showLoginModal) {
      // 如果模态框已经显示，直接返回
      console.log('⚠️ Auth: 登录模态框已经显示，跳过重复显示');
      resolve(false);
      return;
    }
    
    console.log('🔓 Auth: 准备显示一键登录模态框');
    globalAuthState.showLoginModal = true;
    globalAuthState.loginModalResolve = resolve;
    
    console.log('✅ Auth: 响应式状态已更新，模态框应该显示');
  });
}

/**
 * 隐藏一键登录模态框
 * @param {boolean} success 登录是否成功
 */
export function hideOneClickLogin(success = false) {
  console.log('🔒 Auth: 隐藏一键登录模态框，成功状态:', success);
  globalAuthState.showLoginModal = false;
  
  if (globalAuthState.loginModalResolve) {
    console.log('✅ Auth: 解析登录Promise，结果:', success);
    globalAuthState.loginModalResolve(success);
    globalAuthState.loginModalResolve = null;
  }
}

/**
 * 检查登录状态，如果未登录则弹出一键登录
 * @param {Object} options 选项
 * @param {string} options.title 提示标题（已废弃，保留兼容性）
 * @param {string} options.content 提示内容（已废弃，保留兼容性）
 * @param {boolean} options.showModal 是否显示确认模态框（已废弃，保留兼容性）
 * @returns {Promise<boolean>} 是否已登录或登录成功
 */
export async function requireAuth(options = {}) {
  // 如果已经登录，直接返回 true
  if (isAuthenticated()) {
    return true;
  }
  
  // 未登录时直接弹出一键登录模态框
  console.log('🔐 用户未登录，直接弹出一键登录模态框');
  return await showOneClickLogin();
}

/**
 * 登出处理（统一管理）
 */
export function logout() {
  console.log('🚪 [统一登出] 开始执行登出流程');
  
  // 使用全局用户管理器处理登出
  globalUserManager.onUserLogout();
  
  // 显示提示
  uni.showToast({
    title: '已退出登录',
    icon: 'success'
  });
}

/**
 * 获取登录模态框状态
 * @returns {boolean} 模态框是否可见
 */
export function getLoginModalVisible() {
  return globalAuthState.showLoginModal;
}

/**
 * 清空所有用户相关数据（全局工具函数）
 * 可在各个页面的初始化时调用，确保登出后数据清理干净
 */
export function clearAllUserData() {
  console.log('🧹 开始清理所有用户数据...');
  
  try {
    // 清理存储的用户数据
    uni.removeStorageSync('user');
    uni.removeStorageSync('accessToken');
    uni.removeStorageSync('refreshToken');
    
    // 清理用户状态
    userActions.clearUserInfo();
    
    // 清理提醒数据状态
    reminderState.upcomingReminders = [];
    reminderState.complexReminders = [];
    reminderState.loading = false;
    
    console.log('✅ 所有用户数据清理完成');
    
    // 发送全局事件，通知其他页面清理数据
    uni.$emit('userLogout');
    
    return true;
  } catch (error) {
    console.error('❌ 清理用户数据时出错:', error);
    return false;
  }
}

/**
 * 检查登录状态并清空数据（页面初始化专用）
 * @param {string} pageName 页面名称，用于日志
 * @returns {boolean} 是否已登录
 */
export function checkAuthAndClearData(pageName = '未知页面') {
  console.log(`🔍 [${pageName}] 检查登录状态`);
  
  if (!isAuthenticated()) {
    console.log(`❌ [${pageName}] 用户未登录，清空所有数据`);
    clearAllUserData();
    return false;
  }
  
  console.log(`✅ [${pageName}] 用户已登录，可以加载数据`);
  return true;
}

/**
 * Vue组合函数：页面级别的登录检查
 * 在页面的setup函数中使用，提供响应式的登录状态检查
 * @param {string} pageName 页面名称
 * @returns {Object} 包含登录检查相关的响应式状态和方法
 */
export function usePageAuth(pageName = '未知页面') {
  const checkPageAuth = () => {
    return checkAuthAndClearData(pageName);
  };
  
  const clearPageData = () => {
    return clearAllUserData();
  };
  
  return {
    checkPageAuth,
    clearPageData,
    isAuthenticated,
    requireAuth
  };
} 