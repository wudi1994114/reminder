// 按需导入Vue组合式API
import { ref, reactive } from '../utils/imports.js';

// Token 格式验证和处理
const validateAndFormatToken = (token) => {
    if (!token) return null;
    return token.startsWith('Bearer ') ? token : `Bearer ${token}`;
};

// 初始化时确保 token 格式正确
const initToken = localStorage.getItem('accessToken');
if (initToken) {
    const formattedToken = validateAndFormatToken(initToken);
    if (formattedToken !== initToken) {
        localStorage.setItem('accessToken', formattedToken);
    }
}

// 用户状态
export const userState = reactive({
    isAuthenticated: !!localStorage.getItem('accessToken'),
    user: null,
    loading: false,
    error: null,
});

// 提醒事项状态
export const reminderState = reactive({
    simpleReminders: [],
    complexReminders: [],
    upcomingReminders: [],
    loading: false,
    error: null,
});

// 全局UI状态
export const uiState = reactive({
    isEventModalOpen: false,
    isProfileModalOpen: false,
    currentEditingEvent: null,
    notifications: [],
});

// 通知方法
export const showNotification = (message, type = 'info') => {
    const id = Date.now();
    uiState.notifications.push({ id, message, type });
    setTimeout(() => {
        const index = uiState.notifications.findIndex(n => n.id === id);
        if (index !== -1) {
            uiState.notifications.splice(index, 1);
        }
    }, 3000);
};

export default {
    user: userState,
    reminder: reminderState,
    ui: uiState,
    showNotification,
    validateAndFormatToken,
}; 