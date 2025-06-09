// 日期格式化
export const formatDate = (date) => {
    if (!date) return '';
    return new Date(date).toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
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

// 将复杂提醒转换为日历事件格式
export const complexReminderToEvent = (reminder) => {
    return {
        id: reminder.id,
        title: reminder.title,
        description: reminder.description,
        email: reminder.email,
        cronExpression: reminder.cronExpression,
        type: 'complex'
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
        return JSON.parse(atob(token.split('.')[1]));
    } catch (e) {
        return null;
    }
};

// 检查token是否过期
export const isTokenExpired = (token) => {
    const decoded = parseJwt(token);
    if (!decoded) return true;
    return decoded.exp * 1000 < Date.now();
}; 