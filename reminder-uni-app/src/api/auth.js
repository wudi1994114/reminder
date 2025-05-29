import { request } from './request.js';
import { handleApiError } from '@/utils/helpers.js';

// 认证相关API
export const authApi = {
    // 用户登录
    login: (credentials) => request({
        url: '/auth/login',
        method: 'POST',
        data: credentials
    }),

    // 用户注册
    register: (userData) => request({
        url: '/auth/register',
        method: 'POST',
        data: userData
    }),

    // 获取用户信息
    getUserProfile: () => request({
        url: '/auth/profile',
        method: 'GET'
    }).catch(handleApiError),

    // 更新用户信息
    updateProfile: (profileData) => request({
        url: '/auth/profile',
        method: 'PUT',
        data: profileData
    }).catch(handleApiError),

    // 刷新token
    refreshToken: () => request({
        url: '/auth/refresh',
        method: 'POST'
    }).catch(handleApiError),

    // 登出
    logout: () => request({
        url: '/auth/logout',
        method: 'POST'
    }).catch(handleApiError)
}; 