/**
 * 用户相关API
 * 负责：用户偏好、反馈、标签管理等
 */

import { request } from './http.js';

/**
 * 用户偏好API
 */
export const userPreferencesApi = {
    // 获取所有用户偏好
    getUserPreferences: () => request({ 
        url: '/user/preferences', 
        method: 'GET' 
    }),

    // 获取单个用户偏好
    getUserPreference: (key) => request({ 
        url: `/user/preferences/${key}`, 
        method: 'GET' 
    }),

    // 设置用户偏好
    setUserPreference: (key, value, property = null) => request({ 
        url: `/user/preferences/${key}`, 
        method: 'PUT', 
        data: { key, value, property } 
    }),

    // 批量更新用户偏好
    batchUpdateUserPreferences: (preferences, override = false) => request({ 
        url: '/user/preferences/batch', 
        method: 'PUT', 
        data: { preferences, override } 
    }),

    // 删除用户偏好
    deleteUserPreference: (key) => request({ 
        url: `/user/preferences/${key}`, 
        method: 'DELETE' 
    }),

    // 初始化用户偏好
    initializeUserPreferences: () => request({ 
        url: '/user/preferences/initialize', 
        method: 'POST' 
    }),

    // 重置用户偏好
    resetUserPreferences: () => request({ 
        url: '/user/preferences/reset', 
        method: 'POST' 
    })
};

/**
 * 用户反馈API
 */
export const submitUserFeedback = (feedbackData) => {
    return request({
        url: '/user/feedback',
        method: 'POST',
        data: feedbackData
    });
};

/**
 * 用户标签管理API
 */
export const userTagsApi = {
    // 获取标签管理是否启用
    getUserTagManagementEnabled: () => request({ 
        url: '/user/preferences/userTagManagementEnabled', 
        method: 'GET' 
    }),

    // 设置标签管理启用状态
    setUserTagManagementEnabled: (enabled) => {
        const key = 'userTagManagementEnabled';
        const value = enabled ? '1' : '0'; // 转换为字符串
        const property = '';
        return request({
            url: '/user/preferences/userTagManagementEnabled',
            method: 'PUT',
            data: { key, value, property }
        });
    },

    // 获取用户标签列表
    getUserTagList: () => {
        return request({
            url: '/user/preferences/userTagList',
            method: 'GET'
        }).then(response => {
            // 在API层面过滤掉所有反斜杠字符
            if (response && response.value && typeof response.value === 'string') {
                response.value = response.value.replace(/\\/g, '');
                console.log('🏷️ API层面 - 过滤反斜杠后的标签字符串:', response.value);
            }
            return response;
        });
    },

    // 设置用户标签列表
    setUserTagList: (tagList) => {
        const key = 'userTagList';
        const value = tagList;
        const property = '';
        return request({
            url: '/user/preferences/userTagList',
            method: 'PUT',
            data: { key, value, property }
        });
    },

    // 删除用户标签列表
    deleteUserTagList: () => request({ 
        url: '/user/preferences/userTagList', 
        method: 'DELETE' 
    })
};

/**
 * 微信授权次数API
 */
export const wechatAuthApi = {
    // 获取微信授权剩余次数
    getWechatAuthCount: () => {
        return request({
            url: '/user/preferences/wechat-auth-count',
            method: 'GET'
        });
    },

    // 增加微信授权次数
    increaseWechatAuthCount: (count = 1) => {
        return request({
            url: '/user/preferences/wechat-auth-count/increase',
            method: 'POST',
            data: { count }
        });
    }
};

// 统一导出
export default {
    userPreferencesApi,
    submitUserFeedback,
    userTagsApi,
    wechatAuthApi
};

// 兼容旧的导入方式
export const getUserPreferences = userPreferencesApi.getUserPreferences;
export const getUserPreference = userPreferencesApi.getUserPreference;
export const setUserPreference = userPreferencesApi.setUserPreference;
export const batchUpdateUserPreferences = userPreferencesApi.batchUpdateUserPreferences;
export const deleteUserPreference = userPreferencesApi.deleteUserPreference;
export const initializeUserPreferences = userPreferencesApi.initializeUserPreferences;
export const resetUserPreferences = userPreferencesApi.resetUserPreferences;

export const getUserTagManagementEnabled = userTagsApi.getUserTagManagementEnabled;
export const setUserTagManagementEnabled = userTagsApi.setUserTagManagementEnabled;
export const getUserTagList = userTagsApi.getUserTagList;
export const setUserTagList = userTagsApi.setUserTagList;
export const deleteUserTagList = userTagsApi.deleteUserTagList;

export const getWechatAuthCount = wechatAuthApi.getWechatAuthCount;
export const increaseWechatAuthCount = wechatAuthApi.increaseWechatAuthCount;

