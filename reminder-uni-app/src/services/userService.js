/**
 * 统一用户服务
 * 负责用户信息的获取、缓存和管理
 * 简化版：直接管理状态，避免循环依赖
 */
import { reactive } from 'vue';
import { request } from '../api/http.js';

// 直接定义用户状态，不依赖store
export const userState = reactive({
  user: null,
  isAuthenticated: false,
  loading: false,
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
   */
  static async init() {
    console.log('🚀 UserService: 初始化用户服务');

    try {
      // 检查是否有token
      const token = uni.getStorageSync('accessToken');
      if (!token) {
        console.log('📝 UserService: 没有token，无需初始化');
        this.clearUserInfo();
        return;
      }

      // 尝试从缓存加载用户信息
      const cachedUser = this.loadFromCache();
      if (cachedUser) {
        console.log('✅ UserService: 从有效缓存中恢复用户信息成功');
        this.setUserInfo(cachedUser);
      } else {
        console.log('📝 UserService: 无有效缓存，但存在Token，尝试从服务器恢复会话...');
        await this.fetchUserProfile();
      }
    } catch (error) {
      console.error('❌ UserService: 初始化失败:', error);
      this.clearUserInfo();
    }
  }

  /**
   * 从服务器获取用户信息
   */
  static async fetchUserProfile() {
    if (userState.loading) {
      console.log('⏳ UserService: 正在获取用户信息，跳过重复请求');
      return false;
    }

    try {
      userState.loading = true;
      userState.error = null;
      console.log('📡 UserService: 开始从服务器获取用户信息');

      const response = await request({
        url: '/auth/profile',
        method: 'GET'
      });

      if (response && response.id) {
        console.log('✅ UserService: 获取用户信息成功:', {
          id: response.id,
          username: response.username,
          nickname: response.nickname
        });
        
        this.setUserInfo(response);
        this.saveToCache(response);
        return true;
      } else {
        throw new Error('服务器返回的用户数据无效');
      }

    } catch (error) {
      console.error('❌ UserService: 获取用户信息失败:', error);
      userState.error = error.message || '获取用户信息失败';
      
      // 如果是认证错误，清除所有信息
      if (error.statusCode === 401 || error.statusCode === 403) {
        console.log('🔐 UserService: 认证失败，清除所有用户信息');
        this.clearUserInfo();
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
      console.log('📝 UserService: 更新用户信息:', profileData);

      const response = await request({
        url: '/auth/profile',
        method: 'PUT',
        data: profileData
      });

      if (response && response.id) {
        console.log('✅ UserService: 更新用户信息成功');
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
      userState.loading = false;
    }
  }

  /**
   * 登录成功后的处理
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

    // 保存登录类型
    uni.setStorageSync('loginType', loginType);
    console.log('✅ UserService: 登录类型已保存:', loginType);

    // 2. 从服务器获取最新的用户信息
    console.log('📡 UserService: 登录成功，立即获取用户信息');
    const success = await this.fetchUserProfile();

    if (success) {
      console.log('✅ UserService: 登录流程完成，用户信息已更新');
      
      // 发送登录成功事件
      uni.$emit('userLoginSuccess', userState.user);
      console.log('🎉 UserService: 已发送用户登录成功事件');
      
      return userState.user;
    } else {
      console.error('❌ UserService: 登录成功但获取用户信息失败');
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
    console.log('💾 UserService: 用户信息已更新到状态');
  }

  /**
   * 清除用户信息
   */
  static clearUserInfo() {
    userState.user = null;
    userState.isAuthenticated = false;
    userState.error = null;

    // 清除存储
    uni.removeStorageSync('accessToken');
    uni.removeStorageSync(CACHE_CONFIG.STORAGE_KEY);
    uni.removeStorageSync('loginType');

    console.log('🗑️ UserService: 所有用户信息、Token和登录类型已清除');
  }

  /**
   * 清除用户状态但保留Token（用于网络错误等临时问题）
   */
  static clearUserState() {
    userState.user = null;
    userState.isAuthenticated = false;
    userState.error = null;

    // 只清除用户缓存数据，保留Token
    uni.removeStorageSync(CACHE_CONFIG.STORAGE_KEY);

    console.log('🔄 UserService: 用户状态已清除，Token已保留');
  }

  /**
   * 保存到缓存
   */
  static saveToCache(userInfo) {
    try {
      const cacheData = {
        user: userInfo,
        timestamp: Date.now()
      };
      uni.setStorageSync(CACHE_CONFIG.STORAGE_KEY, JSON.stringify(cacheData));
      console.log('💾 UserService: 用户信息已保存到缓存');
    } catch (error) {
      console.error('❌ UserService: 保存缓存失败:', error);
    }
  }

  /**
   * 从缓存加载
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
        uni.removeStorageSync(CACHE_CONFIG.STORAGE_KEY);
        return null;
      }

      return parsed.user;

    } catch (error) {
      console.error('❌ UserService: 加载缓存失败:', error);
      uni.removeStorageSync(CACHE_CONFIG.STORAGE_KEY);
      return null;
    }
  }

  /**
   * 强制刷新用户信息
   */
  static async refreshUserProfile() {
    console.log('🔄 UserService: 手动强制刷新用户信息');
    return await this.fetchUserProfile();
  }
}

// 导出用户服务和状态
export { UserService };