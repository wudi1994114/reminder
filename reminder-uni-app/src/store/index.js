// Store 统一导出
import userStore from './modules/user.js';
import reminderStore from './modules/reminder.js';
import uiStore from './modules/ui.js';

// 导出各模块的状态和操作
export const { userState, userActions } = userStore;
export const { reminderState, reminderActions, reminderGetters } = reminderStore;
export const { uiState, uiActions, showNotification } = uiStore;

// 兼容旧版本的导出方式
export const saveUserInfo = userActions.saveUserInfo;
export const clearUserInfo = userActions.clearUserInfo;
export const validateAndFormatToken = userActions.validateAndFormatToken;

// 默认导出所有store模块
export default {
    user: userStore,
    reminder: reminderStore,
    ui: uiStore
}; 