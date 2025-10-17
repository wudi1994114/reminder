/**
 * 农历和节气工具
 * 负责：农历转换、节气计算等
 * 
 * 注意：这是一个简化实现，完整功能使用lunarManager.js
 */

import { parseDate } from './format.js';

// 导入完整的农历管理器（如果存在）
let lunarManager = null;
try {
    lunarManager = require('../lunarManager.js');
} catch (e) {
    console.warn('lunarManager未加载，使用简化实现');
}

/**
 * 获取农历信息
 * @param {Date|string|number} date - 日期
 * @returns {Object|null} 农历信息
 */
export function getLunarInfo(date) {
    const d = parseDate(date);
    if (!d) return null;
    
    // 如果有完整的农历管理器，使用它
    if (lunarManager && lunarManager.getLunarInfo) {
        return lunarManager.getLunarInfo(d);
    }
    
    // 简化实现 - 返回基本信息
    return {
        year: d.getFullYear(),
        month: d.getMonth() + 1,
        day: d.getDate(),
        lunarYear: '',
        lunarMonth: '',
        lunarDay: '',
        isLeapMonth: false,
        solarTerm: '',
        ganzhi: ''
    };
}

/**
 * 获取节气信息
 * @param {Date|string|number} date - 日期
 * @returns {string} 节气名称
 */
export function getSolarTermForDate(date) {
    const d = parseDate(date);
    if (!d) return '';
    
    // 如果有完整的农历管理器，使用它
    if (lunarManager && lunarManager.getSolarTermForDate) {
        return lunarManager.getSolarTermForDate(d);
    }
    
    return '';
}

/**
 * 获取一年的所有节气
 * @param {number} year - 年份
 * @returns {Array} 节气列表
 */
export function getAllSolarTermsInYear(year) {
    // 如果有完整的农历管理器，使用它
    if (lunarManager && lunarManager.getAllSolarTermsInYear) {
        return lunarManager.getAllSolarTermsInYear(year);
    }
    
    return [];
}

/**
 * 获取一个月的节气
 * @param {number} year - 年份
 * @param {number} month - 月份 (1-12)
 * @returns {Array} 节气列表
 */
export function getSolarTermsInMonth(year, month) {
    // 如果有完整的农历管理器，使用它
    if (lunarManager && lunarManager.getSolarTermsInMonth) {
        return lunarManager.getSolarTermsInMonth(year, month);
    }
    
    return [];
}

/**
 * 获取当前节气
 * @returns {string} 当前节气名称
 */
export function getCurrentSolarTerm() {
    return getSolarTermForDate(new Date());
}

/**
 * 判断是否为节气
 * @param {Date|string|number} date - 日期
 * @returns {boolean}
 */
export function isSolarTerm(date) {
    const term = getSolarTermForDate(date);
    return term && term !== '';
}

/**
 * 获取下一个节气
 * @param {Date|string|number} date - 日期
 * @returns {Object|null} 节气信息 {name, date}
 */
export function getNextSolarTerm(date) {
    const d = parseDate(date);
    if (!d) return null;
    
    const year = d.getFullYear();
    const allTerms = getAllSolarTermsInYear(year);
    
    for (const term of allTerms) {
        if (new Date(term.date) > d) {
            return term;
        }
    }
    
    // 如果今年没有下一个节气，返回明年的第一个
    const nextYearTerms = getAllSolarTermsInYear(year + 1);
    return nextYearTerms.length > 0 ? nextYearTerms[0] : null;
}

export default {
    getLunarInfo,
    getSolarTermForDate,
    getAllSolarTermsInYear,
    getSolarTermsInMonth,
    getCurrentSolarTerm,
    isSolarTerm,
    getNextSolarTerm
};

