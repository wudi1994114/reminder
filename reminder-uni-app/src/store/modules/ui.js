import { reactive } from 'vue';

// 全局UI状态
export const uiState = reactive({
    isEventModalOpen: false,
    isProfileModalOpen: false,
    isLoadingOverlayVisible: false,
    currentEditingEvent: null,
    notifications: [],
    theme: 'light', // light | dark
    language: 'zh-CN'
});

// UI相关操作
export const uiActions = {
    // 打开事件弹窗
    openEventModal(event = null) {
        uiState.isEventModalOpen = true;
        uiState.currentEditingEvent = event;
    },

    // 关闭事件弹窗
    closeEventModal() {
        uiState.isEventModalOpen = false;
        uiState.currentEditingEvent = null;
    },

    // 打开用户资料弹窗
    openProfileModal() {
        uiState.isProfileModalOpen = true;
    },

    // 关闭用户资料弹窗
    closeProfileModal() {
        uiState.isProfileModalOpen = false;
    },

    // 显示加载遮罩
    showLoadingOverlay() {
        uiState.isLoadingOverlayVisible = true;
    },

    // 隐藏加载遮罩
    hideLoadingOverlay() {
        uiState.isLoadingOverlayVisible = false;
    },

    // 设置当前编辑的事件
    setCurrentEditingEvent(event) {
        uiState.currentEditingEvent = event;
    },

    // 添加通知
    addNotification(notification) {
        const id = Date.now();
        const newNotification = {
            id,
            message: '',
            type: 'info', // info | success | warning | error
            duration: 3000,
            ...notification
        };
        
        uiState.notifications.push(newNotification);
        
        // 自动移除通知
        if (newNotification.duration > 0) {
            setTimeout(() => {
                uiActions.removeNotification(id);
            }, newNotification.duration);
        }
        
        return id;
    },

    // 移除通知
    removeNotification(id) {
        const index = uiState.notifications.findIndex(n => n.id === id);
        if (index !== -1) {
            uiState.notifications.splice(index, 1);
        }
    },

    // 清空所有通知
    clearNotifications() {
        uiState.notifications = [];
    },

    // 设置主题
    setTheme(theme) {
        uiState.theme = theme;
        uni.setStorageSync('theme', theme);
    },

    // 设置语言
    setLanguage(language) {
        uiState.language = language;
        uni.setStorageSync('language', language);
    }
};

// 通知方法（兼容旧版本）
export const showNotification = (message, type = 'info') => {
    // 在uni-app中直接使用uni.showToast
    const icon = type === 'success' ? 'success' : (type === 'error' ? 'error' : 'none');
    
    uni.showToast({
        title: message,
        icon: icon,
        duration: 3000
    });
    
    // 同时添加到通知列表
    return uiActions.addNotification({ message, type });
};

// 初始化主题和语言设置
const initTheme = uni.getStorageSync('theme') || 'light';
const initLanguage = uni.getStorageSync('language') || 'zh-CN';
uiState.theme = initTheme;
uiState.language = initLanguage;

export default {
    state: uiState,
    actions: uiActions,
    showNotification
}; 