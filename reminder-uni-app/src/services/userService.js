/**
 * 统一用户服务
 * 负责用户信息的获取、缓存和管理
 *
 * 修改版：严格遵循缓存策略，仅在登录和更新时请求新数据。
 */
import { reactive } from 'vue';
import { request } from './api';

// 用户状态管理
const userState = reactive({
  user: null,
  isAuthenticated: false,
  isLoading: false,
  error: null
});

// 缓存配置
const CACHE_CONFIG = {
  // 缓存有效期（毫秒）- 30分钟
  CACHE_DURATION: 30 * 60 * 1000,
  // 用户信息存储键名
  STORAGE_KEY: 'user_profile_cache'
};

/**
 * 用户服务类
 */
class UserService {

  /**
   * 初始化用户服务
   * 应用启动时调用。此方法只会尝试从有效缓存中恢复用户状态，不会发起网络请求。
   */
  static init() {
    console.log('🚀 UserService: 初始化用户服务');

    try {
      // 检查是否有token，没有token则无需继续
      const token = uni.getStorageSync('accessToken');
      if (!token) {
        console.log('📝 UserService: 没有token，无需初始化');
        // 为确保状态一致，主动清理一次
        this.clearUserInfo();
        return;
      }

      // 尝试从缓存加载用户信息
      // loadFromCache 内部会处理过期逻辑
      const cachedUser = this.loadFromCache();
      if (cachedUser) {
        console.log('✅ UserService: 从有效缓存中恢复用户信息成功');
        this.setUserInfo(cachedUser);
      } else {
        console.log('📝 UserService: 无有效缓存，等待用户操作（如登录）');
        // 缓存无效或过期，不清空token，但保持未登录状态
        // 让路由守卫或页面逻辑判断 isAuthenticated 来决定是否跳转到登录页
      }
    } catch (error) {
      console.error('❌ UserService: 初始化失败:', error);
      this.clearUserInfo();
    }
  }

  /**
   * 从服务器获取用户信息 (内部调用)
   * 此方法是获取数据的核心，只应该在登录或手动刷新时被调用。
   */
  static async fetchUserProfile() {
    if (userState.isLoading) {
      console.log('⏳ UserService: 正在获取用户信息，跳过重复请求');
      return false;
    }

    try {
      userState.isLoading = true;
      userState.error = null;
      console.log('📡 UserService: 开始从服务器获取用户信息');

      const response = await request({
        url: '/auth/profile',
        method: 'GET'
      });

      if (response && response.id) { // 确保返回了有效用户数据
        console.log('✅ UserService: 获取用户信息成功:', {
          id: response.id,
          username: response.username,
          nickname: response.nickname
        });
        // 保存到状态和缓存
        this.setUserInfo(response);
        this.saveToCache(response);
        return true;
      } else {
        throw new Error('服务器返回的用户数据无效');
      }

    } catch (error) {
      console.error('❌ UserService: 获取用户信息失败:', error);
      userState.error = error.message || '获取用户信息失败';
      // 如果是认证错误 (例如 token 失效)，则清除所有信息，强制重新登录
      if (error.statusCode === 401 || error.statusCode === 403) {
        console.log('🔐 UserService: 认证失败，清除所有用户信息');
        this.clearUserInfo();
      }
      return false;
    } finally {
      userState.isLoading = false;
    }
  }

  /**
   * 更新用户信息
   * 更新成功后，用服务器返回的数据刷新本地状态和缓存。
   */
  static async updateUserProfile(profileData) {
    try {
      userState.isLoading = true;
      userState.error = null;
      console.log('📝 UserService: 更新用户信息:', profileData);

      const response = await request({
        url: '/auth/profile',
        method: 'PUT',
        data: profileData
      });

      if (response && response.id) {
        console.log('✅ UserService: 更新用户信息成功');
        // 更新状态和缓存
        this.setUserInfo(response);
        this.saveToCache(response);
        return { success: true, data: response };
      } else {
        throw new Error('服务器返回的更新后数据无效');
      }

    } catch (error) {
      console.error('❌ UserService: 更新用户信息失败:', error);
      userState.error = error.message || '更新用户信息失败';
      return { success: false, error: userState.error };
    } finally {
      userState.isLoading = false;
    }
  }

  /**
   * 登录成功后的处理
   * 这是触发用户信息网络请求的主要入口之一。
   */
  static async onLoginSuccess(loginResponse, loginType = 'unknown') {
    console.log('🎉 UserService: 处理登录成功, 登录类型:', loginType);

    // 1. 保存token
    if (loginResponse.accessToken) {
      const token = loginResponse.accessToken.startsWith('Bearer ')
        ? loginResponse.accessToken
        : `Bearer ${loginResponse.accessToken}`;
      uni.setStorageSync('accessToken', token);
      console.log('✅ UserService: Token已保存');
    }

    // 2. 保存登录类型，用于后续判断是否需要微信订阅权限
    uni.setStorageSync('loginType', loginType);
    console.log('✅ UserService: 登录类型已保存:', loginType);

    // 2. 强制从服务器获取最新的用户信息
    console.log('📡 UserService: 登录成功，立即获取用户信息');
    const success = await this.fetchUserProfile();

    if (success) {
      console.log('✅ UserService: 登录流程完成，用户信息已更新');
      return userState.user;
    } else {
      console.error('❌ UserService: 登录成功但获取用户信息失败');
      // 即使获取失败，token也存了，但 isAuthenticated 仍为 false
      // 这种情况需要业务侧决定如何处理，例如提示用户刷新或重新登录
      throw new Error('登录成功但获取用户信息失败');
    }
  }

  /**
   * 登出处理
   */
  static logout() {
    console.log('👋 UserService: 处理登出');
    this.clearUserInfo();
  }

  /**
   * 获取当前用户信息 (从内存状态)
   * 这是在应用各处获取用户信息的推荐方法，它不会触发网络请求。
   */
  static getCurrentUser() {
    return userState.user;
  }

  /**
   * 获取用户状态（响应式）
   * 用于 Vue 组件，可以自动响应用户状态变化。
   */
  static getUserState() {
    return userState;
  }

  // --- 内部辅助方法 ---

  /**
   * 设置用户信息到状态 (内部)
   */
  static setUserInfo(userInfo) {
    userState.user = userInfo;
    userState.isAuthenticated = true;
    userState.error = null;
    console.log('💾 UserService: 用户信息已更新到状态');
  }

  /**
   * 清除用户信息 (内部)
   */
  static clearUserInfo() {
    userState.user = null;
    userState.isAuthenticated = false;
    userState.error = null;

    // 清除缓存、token和登录类型
    uni.removeStorageSync(CACHE_CONFIG.STORAGE_KEY);
    uni.removeStorageSync('accessToken');
    uni.removeStorageSync('loginType');

    console.log('🗑️ UserService: 所有用户信息、Token和登录类型已清除');
  }

  /**
   * 保存到缓存 (内部)
   */
  static saveToCache(userInfo) {
    try {
      const cacheData = {
        user: userInfo,
        timestamp: Date.now() // 记录保存时间戳
      };
      uni.setStorageSync(CACHE_CONFIG.STORAGE_KEY, JSON.stringify(cacheData));
      console.log('💾 UserService: 用户信息已保存到缓存');
    } catch (error) {
      console.error('❌ UserService: 保存缓存失败:', error);
    }
  }

  /**
   * 从缓存加载 (内部)
   */
  static loadFromCache() {
    try {
      const cachedData = uni.getStorageSync(CACHE_CONFIG.STORAGE_KEY);
      if (!cachedData) {
        return null;
      }

      const parsed = JSON.parse(cachedData);
      const now = Date.now();

      // 检查缓存是否过期
      if (now - parsed.timestamp > CACHE_CONFIG.CACHE_DURATION) {
        console.log('⏰ UserService: 缓存已过期');
        // 清理过期的缓存
        uni.removeStorageSync(CACHE_CONFIG.STORAGE_KEY);
        return null;
      }

      return parsed.user;

    } catch (error) {
      console.error('❌ UserService: 加载缓存失败:', error);
      // 加载失败也清理掉损坏的缓存
      uni.removeStorageSync(CACHE_CONFIG.STORAGE_KEY);
      return null;
    }
  }

  /**
   * 强制刷新用户信息 (可选)
   * 提供一个手动触发刷新的方法，供下拉刷新等场景使用。
   */
  static async refreshUserProfile() {
    console.log('🔄 UserService: 手动强制刷新用户信息');
    return await this.fetchUserProfile();
  }
}

// 导出用户服务和状态
export { UserService, userState };