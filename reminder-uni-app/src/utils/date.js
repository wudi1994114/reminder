/**
 * 日期相关工具函数
 */

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

// 短日期格式化 (不含时间)
export const formatShortDate = (date) => {
    if (!date) return '';
    return new Date(date).toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    });
};

// 只显示时间
export const formatTime = (date) => {
    if (!date) return '';
    return new Date(date).toLocaleTimeString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit'
    });
};

// 格式化为ISO字符串
export const formatToISO = (date) => {
    if (!date) return '';
    return new Date(date).toISOString();
};

// 相对时间格式化
export const formatRelativeTime = (date) => {
    if (!date) return '';
    
    const now = new Date();
    const targetDate = new Date(date);
    const diffMs = targetDate.getTime() - now.getTime();
    const diffMins = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    
    if (diffMins < 1) return '刚刚';
    if (diffMins < 60) return `${diffMins}分钟后`;
    if (diffHours < 24) return `${diffHours}小时后`;
    if (diffDays < 7) return `${diffDays}天后`;
    
    return formatShortDate(date);
};

// 获取今天的开始时间
export const getStartOfDay = (date = new Date()) => {
    const result = new Date(date);
    result.setHours(0, 0, 0, 0);
    return result;
};

// 获取今天的结束时间
export const getEndOfDay = (date = new Date()) => {
    const result = new Date(date);
    result.setHours(23, 59, 59, 999);
    return result;
};

// 获取本周的开始时间
export const getStartOfWeek = (date = new Date()) => {
    const result = new Date(date);
    const day = result.getDay();
    const diff = result.getDate() - day;
    result.setDate(diff);
    return getStartOfDay(result);
};

// 获取本周的结束时间
export const getEndOfWeek = (date = new Date()) => {
    const result = new Date(date);
    const day = result.getDay();
    const diff = result.getDate() - day + 6;
    result.setDate(diff);
    return getEndOfDay(result);
};

// 获取本月的开始时间
export const getStartOfMonth = (date = new Date()) => {
    const result = new Date(date);
    result.setDate(1);
    return getStartOfDay(result);
};

// 获取本月的结束时间
export const getEndOfMonth = (date = new Date()) => {
    const result = new Date(date);
    result.setMonth(result.getMonth() + 1, 0);
    return getEndOfDay(result);
};

// 检查是否是今天
export const isToday = (date) => {
    const today = new Date();
    const targetDate = new Date(date);
    return targetDate.toDateString() === today.toDateString();
};

// 检查是否是本周
export const isThisWeek = (date) => {
    const startOfWeek = getStartOfWeek();
    const endOfWeek = getEndOfWeek();
    const targetDate = new Date(date);
    return targetDate >= startOfWeek && targetDate <= endOfWeek;
};

// 检查是否是本月
export const isThisMonth = (date) => {
    const today = new Date();
    const targetDate = new Date(date);
    return targetDate.getFullYear() === today.getFullYear() && 
           targetDate.getMonth() === today.getMonth();
};

// 添加天数
export const addDays = (date, days) => {
    const result = new Date(date);
    result.setDate(result.getDate() + days);
    return result;
};

// 添加月份
export const addMonths = (date, months) => {
    const result = new Date(date);
    result.setMonth(result.getMonth() + months);
    return result;
};

// 计算两个日期之间的天数差
export const getDaysDiff = (date1, date2) => {
    const firstDate = new Date(date1);
    const secondDate = new Date(date2);
    const diffTime = Math.abs(secondDate - firstDate);
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
};

// 将简单提醒转换为日历事件格式
export const simpleReminderToEvent = (reminder) => {
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