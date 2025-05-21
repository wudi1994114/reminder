import { ref, reactive } from 'vue';

// Token 格式验证和处理
const validateAndFormatToken = (token) => {
    if (!token) return null;
    return token.startsWith('Bearer ') ? token : `Bearer ${token}`;
};

// 初始化时确保 token 格式正确
const initToken = uni.getStorageSync('accessToken');
if (initToken) {
    const formattedToken = validateAndFormatToken(initToken);
    if (formattedToken !== initToken) {
        uni.setStorageSync('accessToken', formattedToken);
    }
}

// 用户状态
export const userState = reactive({
    isAuthenticated: !!uni.getStorageSync('accessToken'),
    user: null,
    loading: false,
    error: null,
});

// 提醒事项状态
export const reminderState = reactive({
    simpleReminders: [],
    complexReminders: [],
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
    // 在uni-app中直接使用uni.showToast
    const icon = type === 'success' ? 'success' : (type === 'error' ? 'error' : 'none');
    
    uni.showToast({
        title: message,
        icon: icon,
        duration: 3000
    });
};

// 保存用户信息
export const saveUserInfo = (userInfo) => {
    userState.user = userInfo;
    userState.isAuthenticated = true;
};

// 清除用户信息
export const clearUserInfo = () => {
    userState.user = null;
    userState.isAuthenticated = false;
    uni.removeStorageSync('accessToken');
};

export default {
    user: userState,
    reminder: reminderState,
    ui: uiState,
    showNotification,
    validateAndFormatToken,
    saveUserInfo,
    clearUserInfo
}; 