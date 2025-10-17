/**
 * services/api.js - 兼容层
 * 
 * 此文件作为过渡期兼容层，将旧的API调用转发到新的模块化API
 * 未来版本将移除此文件，请直接使用新的API模块：
 * - api/auth.js (认证相关)
 * - api/reminder.js (提醒相关)
 * - api/calendar.js (日历相关)
 * - api/user.js (用户偏好相关)
 * - api/wechat.js (微信相关)
 * - api/upload.js (上传相关)
 * - api/http.js (HTTP请求封装)
 */

// 导入新的模块化API
import { request, callContainer, HTTP_CONFIG } from '../api/http.js';
import { authApi } from '../api/auth.js';
import { reminderApi } from '../api/reminder.js';
import { calendarApi } from '../api/calendar.js';
import { 
    userPreferencesApi, 
    submitUserFeedback,
    userTagsApi,
    wechatAuthApi 
} from '../api/user.js';
import {
    isWeChatMiniProgram,
    wechatLogin,
    getLoginCode,
    smartWechatLogin,
    loginWithBackend,
    updateUserInfoFromComponent,
    getSystemInfo,
    requestSubscribeMessage,
    smartRequestSubscribe,
    shareToWeChat,
    scanCode
} from '../api/wechat.js';
import {
    uploadAvatarWithFile,
    uploadFile,
    deleteCloudFile,
    deleteOldAvatarAsync
} from '../api/upload.js';

// ==================== HTTP 请求相关 ====================
export { request, callContainer, HTTP_CONFIG };

// ==================== 认证相关 API ====================
export const login = authApi.login;
export const register = authApi.register;
export const getUserProfile = authApi.getUserProfile;
export const updateProfile = authApi.updateProfile;
export const refreshToken = authApi.refreshToken;
export const logout = authApi.logout;

// ==================== 提醒相关 API ====================
export const getAllSimpleReminders = reminderApi.getAllSimpleReminders;
export const getSimpleReminderById = reminderApi.getSimpleReminderById;
export const createSimpleReminder = reminderApi.createSimpleReminder;
export const updateSimpleReminder = reminderApi.updateSimpleReminder;
export const deleteSimpleReminder = reminderApi.deleteSimpleReminder;
export const getUpcomingReminders = reminderApi.getUpcomingReminders;

export const getAllComplexReminders = reminderApi.getAllComplexReminders;
export const getComplexReminderById = reminderApi.getComplexReminderById;
export const createComplexReminder = reminderApi.createComplexReminder;
export const updateComplexReminder = reminderApi.updateComplexReminder;
export const deleteComplexReminder = reminderApi.deleteComplexReminder;

// 兼容旧的事件API名称
export const createEvent = reminderApi.createSimpleReminder;
export const updateEvent = reminderApi.updateSimpleReminder;
export const deleteEvent = reminderApi.deleteSimpleReminder;

// ==================== 日历相关 API ====================
export const getHolidaysByYearRange = calendarApi.getHolidaysByYearRange;
export const getCalendarData = calendarApi.getCalendarData;
export const getMonthEvents = calendarApi.getMonthEvents;
export const getLunarInfo = calendarApi.getLunarInfo;

// ==================== 用户偏好相关 API ====================
export const getUserPreferences = userPreferencesApi.getUserPreferences;
export const getUserPreference = userPreferencesApi.getUserPreference;
export const setUserPreference = userPreferencesApi.setUserPreference;
export const batchUpdateUserPreferences = userPreferencesApi.batchUpdateUserPreferences;
export const deleteUserPreference = userPreferencesApi.deleteUserPreference;
export const initializeUserPreferences = userPreferencesApi.initializeUserPreferences;
export const resetUserPreferences = userPreferencesApi.resetUserPreferences;

// ==================== 用户反馈 API ====================
export { submitUserFeedback };

// ==================== 用户标签管理 API ====================
export const getUserTagManagementEnabled = userTagsApi.getUserTagManagementEnabled;
export const setUserTagManagementEnabled = userTagsApi.setUserTagManagementEnabled;
export const getUserTagList = userTagsApi.getUserTagList;
export const setUserTagList = userTagsApi.setUserTagList;
export const deleteUserTagList = userTagsApi.deleteUserTagList;

// ==================== 微信授权次数 API ====================
export const getWechatAuthCount = wechatAuthApi.getWechatAuthCount;
export const increaseWechatAuthCount = wechatAuthApi.increaseWechatAuthCount;

// ==================== 微信小程序相关 API ====================
export {
    isWeChatMiniProgram,
    wechatLogin,
    getLoginCode,
    smartWechatLogin,
    loginWithBackend,
    updateUserInfoFromComponent,
    getSystemInfo,
    requestSubscribeMessage,
    smartRequestSubscribe,
    shareToWeChat,
    scanCode
};

// ==================== 文件上传相关 API ====================
export {
    uploadAvatarWithFile,
    uploadFile,
    deleteCloudFile,
    deleteOldAvatarAsync
};

// ==================== 导出API模块（供组织化使用） ====================
export const API = {
    // HTTP
    request,
    callContainer,
    HTTP_CONFIG,
    
    // 模块化API
    auth: authApi,
    reminder: reminderApi,
    calendar: calendarApi,
    user: {
        preferences: userPreferencesApi,
        tags: userTagsApi,
        wechatAuth: wechatAuthApi,
        submitFeedback: submitUserFeedback
    },
    wechat: {
        isWeChatMiniProgram,
        wechatLogin,
        getLoginCode,
        smartWechatLogin,
        loginWithBackend,
        updateUserInfoFromComponent,
        getSystemInfo,
        requestSubscribeMessage,
        smartRequestSubscribe,
        shareToWeChat,
        scanCode
    },
    upload: {
        uploadAvatarWithFile,
        uploadFile,
        deleteCloudFile,
        deleteOldAvatarAsync
    }
};

// 默认导出
export default API;

