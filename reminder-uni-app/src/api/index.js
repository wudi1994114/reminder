/**
 * API统一导出
 * 重构后的API模块化结构
 */

// 导出HTTP配置和request函数
export { request, HTTP_CONFIG } from './http.js';

// 导出认证相关API
export { authApi } from './auth.js';

// 导出提醒相关API
export { reminderApi, createEvent, updateEvent, deleteEvent } from './reminder.js';

// 导出日历相关API
export { calendarApi } from './calendar.js';

// 导出用户相关API（新增）
export { 
    userPreferencesApi, 
    submitUserFeedback,
    userTagsApi,
    wechatAuthApi
} from './user.js';

// 导出微信相关API（新增）
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
} from './wechat.js';

// 导出上传相关API（新增）
export {
    uploadAvatarWithFile,
    uploadFile
} from './upload.js';

// 力量训练相关API（新增）
export { strengthApi } from './strength.js';

// =================================
// 兼容旧的导入方式（保持向后兼容）
// =================================

import { authApi } from './auth.js';
import { reminderApi } from './reminder.js';
import { calendarApi } from './calendar.js';
import {
    getUserPreferences,
    getUserPreference,
    setUserPreference,
    batchUpdateUserPreferences,
    deleteUserPreference,
    initializeUserPreferences,
    resetUserPreferences,
    submitUserFeedback,
    getUserTagManagementEnabled,
    setUserTagManagementEnabled,
    getUserTagList,
    setUserTagList,
    deleteUserTagList,
    getWechatAuthCount,
    increaseWechatAuthCount
} from './user.js';

// 认证相关（兼容导出）
export const {
    login,
    register,
    getUserProfile,
    updateProfile,
    refreshToken,
    logout
} = authApi;

// 提醒相关（兼容导出）
export const {
    getAllSimpleReminders,
    getSimpleReminderById,
    createSimpleReminder,
    updateSimpleReminder,
    deleteSimpleReminder,
    getUpcomingReminders,
    getAllComplexReminders,
    getComplexReminderById,
    createComplexReminder,
    updateComplexReminder,
    deleteComplexReminder
} = reminderApi;

// 日历相关（兼容导出）
export const {
    getHolidaysByYearRange,
    getCalendarData,
    getMonthEvents,
    getLunarInfo
} = calendarApi;

// 用户偏好相关（兼容导出）
export {
    getUserPreferences,
    getUserPreference,
    setUserPreference,
    batchUpdateUserPreferences,
    deleteUserPreference,
    initializeUserPreferences,
    resetUserPreferences,
    submitUserFeedback,
    getUserTagManagementEnabled,
    setUserTagManagementEnabled,
    getUserTagList,
    setUserTagList,
    deleteUserTagList,
    getWechatAuthCount,
    increaseWechatAuthCount
};

// 默认导出所有API
export default {
    // HTTP
    request,
    // 模块化API
    auth: authApi,
    reminder: reminderApi,
    calendar: calendarApi,
    strength: () => import('./strength.js').then(m => m.strengthApi),
    user: {
        preferences: import('./user.js').then(m => m.userPreferencesApi),
        tags: import('./user.js').then(m => m.userTagsApi),
        wechatAuth: import('./user.js').then(m => m.wechatAuthApi),
        submitFeedback: submitUserFeedback
    }
};
