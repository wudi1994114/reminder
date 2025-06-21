/**
 * 数据同步工具
 * 处理登出后的数据清理和状态同步问题
 */

import { isAuthenticated } from './auth';

/**
 * 页面数据同步管理器
 * 确保页面数据与用户认证状态保持同步
 */
export class PageDataSyncManager {
  constructor() {
    this.registeredPages = new Map();
    this.lastAuthState = isAuthenticated();
    
    // 定期检查认证状态变化
    this.startAuthStateMonitoring();
  }

  /**
   * 注册页面数据清理函数
   * @param {string} pageId - 页面标识
   * @param {Function} clearDataFn - 清理数据的函数
   */
  registerPage(pageId, clearDataFn) {
    this.registeredPages.set(pageId, clearDataFn);
    console.log(`📝 [数据同步] 注册页面: ${pageId}`);
  }

  /**
   * 注销页面
   * @param {string} pageId - 页面标识
   */
  unregisterPage(pageId) {
    this.registeredPages.delete(pageId);
    console.log(`🗑️ [数据同步] 注销页面: ${pageId}`);
  }

  /**
   * 开始监控认证状态变化
   */
  startAuthStateMonitoring() {
    // 每秒检查一次认证状态
    setInterval(() => {
      const currentAuthState = isAuthenticated();
      
      // 如果从已认证变为未认证，清理所有页面数据
      if (this.lastAuthState && !currentAuthState) {
        console.log('🚨 [数据同步] 检测到用户登出，清理所有页面数据');
        this.clearAllPagesData();
      }
      
      this.lastAuthState = currentAuthState;
    }, 1000);
  }

  /**
   * 清理所有注册页面的数据
   */
  clearAllPagesData() {
    this.registeredPages.forEach((clearDataFn, pageId) => {
      try {
        console.log(`🧹 [数据同步] 清理页面数据: ${pageId}`);
        clearDataFn();
      } catch (error) {
        console.error(`❌ [数据同步] 清理页面数据失败: ${pageId}`, error);
      }
    });
  }

  /**
   * 手动触发数据同步检查
   */
  syncNow() {
    const currentAuthState = isAuthenticated();
    if (!currentAuthState) {
      console.log('🔄 [数据同步] 手动同步：用户未认证，清理数据');
      this.clearAllPagesData();
    }
  }
}

// 全局数据同步管理器实例
export const globalDataSyncManager = new PageDataSyncManager();

/**
 * 页面数据同步混入
 * 为页面提供自动数据同步功能
 */
export function usePageDataSync(pageId, clearDataFn) {
  // 注册页面
  globalDataSyncManager.registerPage(pageId, clearDataFn);
  
  // 返回清理函数，用于页面销毁时注销
  return () => {
    globalDataSyncManager.unregisterPage(pageId);
  };
}

/**
 * 智能数据加载器
 * 在加载数据前检查认证状态，避免无效请求
 */
export function createSmartDataLoader(loadDataFn, clearDataFn) {
  return async (...args) => {
    // 检查认证状态
    if (!isAuthenticated()) {
      console.log('🔒 [智能加载] 用户未认证，清理数据而非加载');
      if (clearDataFn) {
        clearDataFn();
      }
      return;
    }
    
    // 用户已认证，执行数据加载
    try {
      console.log('📡 [智能加载] 用户已认证，开始加载数据');
      return await loadDataFn(...args);
    } catch (error) {
      console.error('❌ [智能加载] 数据加载失败:', error);
      
      // 如果是认证错误，清理数据
      if (error.message && (
        error.message.includes('认证') || 
        error.message.includes('401') || 
        error.message.includes('unauthorized')
      )) {
        console.log('🔒 [智能加载] 认证错误，清理数据');
        if (clearDataFn) {
          clearDataFn();
        }
      }
      
      throw error;
    }
  };
}

/**
 * 页面显示时的数据同步检查
 * 用于页面的 onShow 生命周期
 */
export function checkDataSyncOnShow(pageId, loadDataFn, clearDataFn) {
  console.log(`👁️ [数据同步] ${pageId} 页面显示，检查数据同步`);
  
  if (!isAuthenticated()) {
    console.log(`🔒 [数据同步] ${pageId} 用户未认证，清理数据`);
    if (clearDataFn) {
      clearDataFn();
    }
  } else {
    console.log(`✅ [数据同步] ${pageId} 用户已认证，加载数据`);
    if (loadDataFn) {
      loadDataFn();
    }
  }
}

/**
 * 创建响应式的认证状态监听器
 * 当认证状态变化时自动执行回调
 */
export function createAuthStateWatcher(onAuthChange) {
  let lastAuthState = isAuthenticated();
  
  const checkAuthState = () => {
    const currentAuthState = isAuthenticated();
    if (lastAuthState !== currentAuthState) {
      console.log(`🔄 [认证监听] 状态变化: ${lastAuthState} -> ${currentAuthState}`);
      onAuthChange(currentAuthState, lastAuthState);
      lastAuthState = currentAuthState;
    }
  };
  
  // 立即检查一次
  checkAuthState();
  
  // 定期检查
  const intervalId = setInterval(checkAuthState, 1000);
  
  // 返回清理函数
  return () => {
    clearInterval(intervalId);
  };
}

/**
 * 页面数据管理器类
 * 为单个页面提供完整的数据同步管理
 */
export class PageDataManager {
  constructor(pageId) {
    this.pageId = pageId;
    this.data = new Map();
    this.loadingStates = new Map();
    this.cleanupFunctions = [];
    
    // 注册到全局管理器
    this.unregisterFromGlobal = usePageDataSync(pageId, () => this.clearAllData());
    
    console.log(`📋 [页面管理] 创建页面数据管理器: ${pageId}`);
  }

  /**
   * 设置数据
   */
  setData(key, value) {
    this.data.set(key, value);
  }

  /**
   * 获取数据
   */
  getData(key) {
    return this.data.get(key);
  }

  /**
   * 设置加载状态
   */
  setLoading(key, loading) {
    this.loadingStates.set(key, loading);
  }

  /**
   * 获取加载状态
   */
  getLoading(key) {
    return this.loadingStates.get(key) || false;
  }

  /**
   * 清理所有数据
   */
  clearAllData() {
    console.log(`🧹 [页面管理] 清理 ${this.pageId} 的所有数据`);
    this.data.clear();
    this.loadingStates.clear();
  }

  /**
   * 智能加载数据
   */
  async smartLoad(key, loadFn) {
    if (!isAuthenticated()) {
      console.log(`🔒 [页面管理] ${this.pageId} 用户未认证，清理 ${key} 数据`);
      this.setData(key, null);
      this.setLoading(key, false);
      return;
    }

    try {
      this.setLoading(key, true);
      const result = await loadFn();
      this.setData(key, result);
      return result;
    } catch (error) {
      console.error(`❌ [页面管理] ${this.pageId} 加载 ${key} 失败:`, error);
      this.setData(key, null);
      throw error;
    } finally {
      this.setLoading(key, false);
    }
  }

  /**
   * 销毁管理器
   */
  destroy() {
    console.log(`🗑️ [页面管理] 销毁页面数据管理器: ${this.pageId}`);
    
    // 清理数据
    this.clearAllData();
    
    // 从全局管理器注销
    if (this.unregisterFromGlobal) {
      this.unregisterFromGlobal();
    }
    
    // 执行所有清理函数
    this.cleanupFunctions.forEach(cleanup => {
      try {
        cleanup();
      } catch (error) {
        console.error('清理函数执行失败:', error);
      }
    });
    
    this.cleanupFunctions = [];
  }

  /**
   * 添加清理函数
   */
  addCleanup(cleanupFn) {
    this.cleanupFunctions.push(cleanupFn);
  }
}
