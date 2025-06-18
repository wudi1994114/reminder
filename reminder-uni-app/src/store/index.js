// Store 统一导出
import * as user from './modules/user.js';
import * as reminder from './modules/reminder.js';
import * as ui from './modules/ui.js';

// 导出各模块的状态和操作
export const { userState, userActions } = user;
export const { reminderState, reminderActions, reminderGetters } = reminder;
export const { uiState, uiActions, showNotification } = ui;

// 兼容旧版本的导出方式
export const saveUserInfo = user.userActions.saveUserInfo;
export const clearUserInfo = user.userActions.clearUserInfo;
export const validateAndFormatToken = user.userActions.validateAndFormatToken;

// 默认导出所有store模块
export default {
    user: user.default,
    reminder: reminder.default,
    ui: ui.default
}; 