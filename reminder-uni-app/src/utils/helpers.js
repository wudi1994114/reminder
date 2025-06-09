// 日期格式化 - 完整的年月日时分格式
export const formatDate = (date) => {
    if (!date) return '';
    const d = new Date(date);
    if (isNaN(d.getTime())) return '';
    
    const year = d.getFullYear();
    const month = d.getMonth() + 1;
    const day = d.getDate();
    const hour = d.getHours();
    const minute = d.getMinutes();
    
    return `${year}年${month}月${day}日 ${hour}时${String(minute).padStart(2, '0')}分`;
};

// 短日期格式化 (不含时间) - 年月日格式
export const formatShortDate = (date) => {
    if (!date) return '';
    const d = new Date(date);
    if (isNaN(d.getTime())) return '';
    
    const year = d.getFullYear();
    const month = d.getMonth() + 1;
    const day = d.getDate();
    
    return `${year}年${month}月${day}日`;
};

// 只显示时间 - 时分格式
export const formatTime = (date) => {
    if (!date) return '';
    const d = new Date(date);
    if (isNaN(d.getTime())) return '';
    
    const hour = d.getHours();
    const minute = d.getMinutes();
    
    return `${hour}时${String(minute).padStart(2, '0')}分`;
};

// 验证邮箱格式
export const isValidEmail = (email) => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
};

// 验证密码强度
export const isStrongPassword = (password) => {
    return password.length >= 8 && 
           /[a-zA-Z]/.test(password) && 
           /[0-9]/.test(password);
};

// 将简单提醒转换为日历事件格式
export const simpleReminderToEvent = (reminder) => {
    // 确保使用正确的本地时间解析事件时间
    const eventTime = new Date(reminder.eventTime);
    
    return {
        id: reminder.id,
        title: reminder.title,
        start: eventTime,
        end: new Date(eventTime.getTime() + 30 * 60000), // 默认30分钟
        description: reminder.description,
        email: reminder.email,
        reminderType: reminder.reminderType,
        type: 'simple'
    };
};

// 防抖函数
export const debounce = (func, wait) => {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
};

// 解析JWT token
export const parseJwt = (token) => {
    try {
        // uni-app环境使用Buffer解析
        const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(
            uni.arrayBufferToBase64(Array.from(atob(base64)).map(c => c.charCodeAt(0)))
            .split('')
            .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
            .join('')
        );
        return JSON.parse(jsonPayload);
    } catch (e) {
        console.error('解析token失败:', e);
        return null;
    }
};

// 检查token是否过期
export const isTokenExpired = (token) => {
    const decoded = parseJwt(token);
    if (!decoded) return true;
    return decoded.exp * 1000 < Date.now();
};

// 统一处理请求错误
export const handleApiError = (error) => {
    let message = '请求失败';
    
    if (error.errMsg) {
        message = error.errMsg;
    } else if (error.data && error.data.message) {
        message = error.data.message;
    }
    
    // 在uni-app中显示错误提示
    uni.showToast({
        title: message,
        icon: 'none',
        duration: 3000
    });
    
    return message;
}; 