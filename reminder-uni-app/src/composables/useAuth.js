import { computed } from 'vue';
import { userState, userActions } from '@/store';
import { authApi } from '@/api';
import { showNotification } from '@/store';

/**
 * 认证相关的组合式函数
 */
export function useAuth() {
    // 状态
    const isAuthenticated = computed(() => userState.isAuthenticated);
    const user = computed(() => userState.user);
    const loading = computed(() => userState.loading);
    const error = computed(() => userState.error);

    // 登录
    const login = async (credentials) => {
        try {
            userActions.setLoading(true);
            userActions.setError(null);
            
            const response = await authApi.login(credentials);
            
            if (response.token) {
                // 保存token
                uni.setStorageSync('accessToken', response.token);
                
                // 保存用户信息
                userActions.saveUserInfo(response.user || {});
                
                showNotification('登录成功', 'success');
                return { success: true, data: response };
            } else {
                throw new Error('登录失败：未收到token');
            }
        } catch (error) {
            const message = error.message || '登录失败';
            userActions.setError(message);
            showNotification(message, 'error');
            return { success: false, error: message };
        } finally {
            userActions.setLoading(false);
        }
    };

    // 注册
    const register = async (userData) => {
        try {
            userActions.setLoading(true);
            userActions.setError(null);
            
            const response = await authApi.register(userData);
            
            showNotification('注册成功，请登录', 'success');
            return { success: true, data: response };
        } catch (error) {
            const message = error.message || '注册失败';
            userActions.setError(message);
            showNotification(message, 'error');
            return { success: false, error: message };
        } finally {
            userActions.setLoading(false);
        }
    };

    // 登出
    const logout = async () => {
        try {
            await authApi.logout();
        } catch (error) {
            console.warn('登出API调用失败:', error);
        } finally {
            // 无论API调用是否成功，都清除本地状态
            userActions.clearUserInfo();
            showNotification('已退出登录', 'info');
            
            // 如果没有上一页，直接跳转到登录页
            uni.switchTab({
                url: '/pages/index/index'
            });
        }
    };

    // 获取用户信息
    const fetchUserProfile = async () => {
        try {
            userActions.setLoading(true);
            const profile = await authApi.getUserProfile();
            userActions.saveUserInfo(profile);
            return { success: true, data: profile };
        } catch (error) {
            const message = error.message || '获取用户信息失败';
            userActions.setError(message);
            return { success: false, error: message };
        } finally {
            userActions.setLoading(false);
        }
    };

    // 更新用户信息
    const updateProfile = async (profileData) => {
        try {
            userActions.setLoading(true);
            const updatedProfile = await authApi.updateProfile(profileData);
            userActions.saveUserInfo(updatedProfile);
            showNotification('更新成功', 'success');
            return { success: true, data: updatedProfile };
        } catch (error) {
            const message = error.message || '更新失败';
            userActions.setError(message);
            showNotification(message, 'error');
            return { success: false, error: message };
        } finally {
            userActions.setLoading(false);
        }
    };

    // 检查认证状态
    const checkAuth = () => {
        const token = uni.getStorageSync('accessToken');
        if (token) {
            // 如果有token但用户状态未认证，重新设置状态
            if (!userState.isAuthenticated) {
                userActions.saveUserInfo({});
            }
            return true;
        } else {
            userActions.clearUserInfo();
            return false;
        }
    };

    // 需要认证的页面检查
    const requireAuth = () => {
        if (!checkAuth()) {
            uni.showModal({
                title: '提示',
                content: '请先登录',
                showCancel: false,
                success: () => {
                    // 跳转到登录页面
                    uni.switchTab({
                        url: '/pages/index/index'
                    });
                }
            });
            return false;
        }
        return true;
    };

    return {
        // 状态
        isAuthenticated,
        user,
        loading,
        error,
        
        // 方法
        login,
        register,
        logout,
        fetchUserProfile,
        updateProfile,
        checkAuth,
        requireAuth
    };
} 