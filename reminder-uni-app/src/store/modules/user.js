import { reactive } from 'vue';

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

// 用户相关操作
export const userActions = {
    // 保存用户信息
    saveUserInfo(userInfo) {
        userState.user = userInfo;
        userState.isAuthenticated = true;
    },

    // 清除用户信息
    clearUserInfo() {
        userState.user = null;
        userState.isAuthenticated = false;
        uni.removeStorageSync('accessToken');
    },

    // 设置加载状态
    setLoading(loading) {
        userState.loading = loading;
    },

    // 设置错误信息
    setError(error) {
        userState.error = error;
    },

    // 验证和格式化Token
    validateAndFormatToken
};

export default {
    state: userState,
    actions: userActions
}; 