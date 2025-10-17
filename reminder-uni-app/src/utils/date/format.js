/**
 * 日期格式化工具
 * 负责：日期格式化、时间格式化、相对时间等
 */

/**
 * 格式化日期时间
 * @param {Date|string|number} date - 日期
 * @returns {string} 格式化后的日期时间字符串 (YYYY-MM-DD HH:mm:ss)
 */
export function formatDateTime(date) {
    const d = parseDate(date);
    if (!d) return '';
    
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    const seconds = String(d.getSeconds()).padStart(2, '0');
    
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

/**
 * 格式化日期
 * @param {Date|string|number} date - 日期
 * @returns {string} 格式化后的日期字符串 (YYYY-MM-DD)
 */
export function formatDate(date) {
    const d = parseDate(date);
    if (!d) return '';
    
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    
    return `${year}-${month}-${day}`;
}

/**
 * 格式化时间
 * @param {Date|string|number} date - 日期
 * @returns {string} 格式化后的时间字符串 (HH:mm:ss)
 */
export function formatTime(date) {
    const d = parseDate(date);
    if (!d) return '';
    
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    const seconds = String(d.getSeconds()).padStart(2, '0');
    
    return `${hours}:${minutes}:${seconds}`;
}

/**
 * 智能格式化日期（今天、昨天、明天等）
 * @param {Date|string|number} date - 日期
 * @returns {string} 格式化后的智能日期字符串
 */
export function formatSmart(date) {
    const d = parseDate(date);
    if (!d) return '';
    
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const targetDate = new Date(d.getFullYear(), d.getMonth(), d.getDate());
    
    const diffDays = Math.floor((targetDate - today) / (1000 * 60 * 60 * 24));
    const timeStr = formatTime(d).substring(0, 5); // HH:mm
    
    if (diffDays === 0) {
        return `今天 ${timeStr}`;
    } else if (diffDays === 1) {
        return `明天 ${timeStr}`;
    } else if (diffDays === -1) {
        return `昨天 ${timeStr}`;
    } else if (diffDays > 1 && diffDays <= 7) {
        return `${diffDays}天后 ${timeStr}`;
    } else if (diffDays < -1 && diffDays >= -7) {
        return `${-diffDays}天前 ${timeStr}`;
    } else {
        return formatDateTime(d).substring(0, 16); // YYYY-MM-DD HH:mm
    }
}

/**
 * 格式化相对时间（刚刚、几分钟前等）
 * @param {Date|string|number} date - 日期
 * @returns {string} 相对时间字符串
 */
export function formatRelative(date) {
    const d = parseDate(date);
    if (!d) return '';
    
    const now = new Date();
    const diffMs = now - d;
    const diffSeconds = Math.floor(diffMs / 1000);
    const diffMinutes = Math.floor(diffSeconds / 60);
    const diffHours = Math.floor(diffMinutes / 60);
    const diffDays = Math.floor(diffHours / 24);
    
    if (diffSeconds < 60) {
        return '刚刚';
    } else if (diffMinutes < 60) {
        return `${diffMinutes}分钟前`;
    } else if (diffHours < 24) {
        return `${diffHours}小时前`;
    } else if (diffDays < 7) {
        return `${diffDays}天前`;
    } else {
        return formatDate(d);
    }
}

/**
 * 格式化提醒时间（用于提醒卡片显示）
 * @param {Date|string|number} date - 日期
 * @returns {string} 格式化后的提醒时间
 */
export function formatReminder(date) {
    const d = parseDate(date);
    if (!d) return '';
    
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const targetDate = new Date(d.getFullYear(), d.getMonth(), d.getDate());
    
    const diffDays = Math.floor((targetDate - today) / (1000 * 60 * 60 * 24));
    const timeStr = formatTime(d).substring(0, 5); // HH:mm
    
    if (diffDays === 0) {
        return `今天 ${timeStr}`;
    } else if (diffDays === 1) {
        return `明天 ${timeStr}`;
    } else if (diffDays === -1) {
        return `昨天 ${timeStr}`;
    } else {
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${month}-${day} ${timeStr}`;
    }
}

/**
 * 转换为ISO格式
 * @param {Date|string|number} date - 日期
 * @returns {string} ISO格式字符串
 */
export function toISO(date) {
    const d = parseDate(date);
    if (!d) return '';
    return d.toISOString();
}

/**
 * 转换为时间戳
 * @param {Date|string|number} date - 日期
 * @returns {number} 时间戳
 */
export function toTimestamp(date) {
    const d = parseDate(date);
    if (!d) return 0;
    return d.getTime();
}

/**
 * 解析日期
 * @param {Date|string|number} date - 日期
 * @returns {Date|null} Date对象
 */
export function parseDate(date) {
    if (!date) return null;
    
    if (date instanceof Date) {
        return isNaN(date.getTime()) ? null : date;
    }
    
    if (typeof date === 'number') {
        return new Date(date);
    }
    
    if (typeof date === 'string') {
        const d = new Date(date);
        return isNaN(d.getTime()) ? null : d;
    }
    
    return null;
}

/**
 * 检查日期是否有效
 * @param {Date|string|number} date - 日期
 * @returns {boolean}
 */
export function isValid(date) {
    return parseDate(date) !== null;
}

/**
 * 获取当前时间
 * @returns {Date}
 */
export function now() {
    return new Date();
}

export default {
    formatDateTime,
    formatDate,
    formatTime,
    formatSmart,
    formatRelative,
    formatReminder,
    toISO,
    toTimestamp,
    parseDate,
    isValid,
    now
};

