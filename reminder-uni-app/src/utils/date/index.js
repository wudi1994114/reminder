/**
 * 日期工具统一导出
 * 整合所有日期相关功能
 */

// 导出格式化功能
export {
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
} from './format.js';

// 导出农历和节气功能
export {
    getLunarInfo,
    getSolarTermForDate,
    getAllSolarTermsInYear,
    getSolarTermsInMonth,
    getCurrentSolarTerm,
    isSolarTerm,
    getNextSolarTerm
} from './lunar.js';

// 导入子模块
import * as format from './format.js';
import * as lunar from './lunar.js';

// 默认导出所有功能
export default {
    ...format,
    ...lunar
};

// 兼容原有的导出方式（从原date.js保留）
import { parseDate as _parseDate } from './format.js';

/**
 * 获取一天的开始时间
 * @param {Date} date - 日期
 * @returns {Date}
 */
export const getStartOfDay = (date = new Date()) => {
    const d = _parseDate(date) || new Date();
    return new Date(d.getFullYear(), d.getMonth(), d.getDate(), 0, 0, 0, 0);
};

/**
 * 获取一天的结束时间
 * @param {Date} date - 日期
 * @returns {Date}
 */
export const getEndOfDay = (date = new Date()) => {
    const d = _parseDate(date) || new Date();
    return new Date(d.getFullYear(), d.getMonth(), d.getDate(), 23, 59, 59, 999);
};

/**
 * 获取一周的开始时间（周一）
 * @param {Date} date - 日期
 * @returns {Date}
 */
export const getStartOfWeek = (date = new Date()) => {
    const d = _parseDate(date) || new Date();
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1);
    return new Date(d.getFullYear(), d.getMonth(), diff, 0, 0, 0, 0);
};

/**
 * 获取一周的结束时间（周日）
 * @param {Date} date - 日期
 * @returns {Date}
 */
export const getEndOfWeek = (date = new Date()) => {
    const d = _parseDate(date) || new Date();
    const day = d.getDay();
    const diff = d.getDate() + (day === 0 ? 0 : 7 - day);
    return new Date(d.getFullYear(), d.getMonth(), diff, 23, 59, 59, 999);
};

/**
 * 获取一个月的开始时间
 * @param {Date} date - 日期
 * @returns {Date}
 */
export const getStartOfMonth = (date = new Date()) => {
    const d = _parseDate(date) || new Date();
    return new Date(d.getFullYear(), d.getMonth(), 1, 0, 0, 0, 0);
};

/**
 * 获取一个月的结束时间
 * @param {Date} date - 日期
 * @returns {Date}
 */
export const getEndOfMonth = (date = new Date()) => {
    const d = _parseDate(date) || new Date();
    return new Date(d.getFullYear(), d.getMonth() + 1, 0, 23, 59, 59, 999);
};

/**
 * 判断是否为今天
 * @param {Date} date - 日期
 * @returns {boolean}
 */
export const isToday = (date) => {
    const d = _parseDate(date);
    if (!d) return false;
    
    const today = new Date();
    return d.getDate() === today.getDate() &&
           d.getMonth() === today.getMonth() &&
           d.getFullYear() === today.getFullYear();
};

/**
 * 判断是否为本周
 * @param {Date} date - 日期
 * @returns {boolean}
 */
export const isThisWeek = (date) => {
    const d = _parseDate(date);
    if (!d) return false;
    
    const today = new Date();
    const weekStart = getStartOfWeek(today);
    const weekEnd = getEndOfWeek(today);
    
    return d >= weekStart && d <= weekEnd;
};

/**
 * 判断是否为本月
 * @param {Date} date - 日期
 * @returns {boolean}
 */
export const isThisMonth = (date) => {
    const d = _parseDate(date);
    if (!d) return false;
    
    const today = new Date();
    return d.getMonth() === today.getMonth() &&
           d.getFullYear() === today.getFullYear();
};

/**
 * 添加天数
 * @param {Date} date - 日期
 * @param {number} days - 天数
 * @returns {Date}
 */
export const addDays = (date, days) => {
    const d = _parseDate(date) || new Date();
    const result = new Date(d);
    result.setDate(result.getDate() + days);
    return result;
};

/**
 * 添加月数
 * @param {Date} date - 日期
 * @param {number} months - 月数
 * @returns {Date}
 */
export const addMonths = (date, months) => {
    const d = _parseDate(date) || new Date();
    const result = new Date(d);
    result.setMonth(result.getMonth() + months);
    return result;
};

/**
 * 计算两个日期之间的天数差
 * @param {Date} date1 - 日期1
 * @param {Date} date2 - 日期2
 * @returns {number}
 */
export const getDaysDiff = (date1, date2) => {
    const d1 = _parseDate(date1);
    const d2 = _parseDate(date2);
    
    if (!d1 || !d2) return 0;
    
    const diffMs = Math.abs(d2 - d1);
    return Math.floor(diffMs / (1000 * 60 * 60 * 24));
};

/**
 * 简单提醒转换为事件对象（兼容原有功能）
 * @param {Object} reminder - 提醒对象
 * @returns {Object} 事件对象
 */
export const simpleReminderToEvent = (reminder) => {
    return {
        id: reminder.id,
        title: reminder.title,
        start: _parseDate(reminder.eventTime),
        end: _parseDate(reminder.eventTime),
        description: reminder.description,
        type: 'simple',
        allDay: false
    };
};

