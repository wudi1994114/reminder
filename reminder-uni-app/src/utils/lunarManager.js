/**
 * 农历管理器 - 统一的农历功能接口
 * 优先使用 lunar-calendar-zh 库，提供稳定的阳历转换和节气功能
 */

// 导入轻量级实现（主要方案）
import LunarAlternative from './lunarAlternative.js';

// 配置选项
const CONFIG = {
  // 优先使用 alternative (lunar-calendar-zh)，更稳定
  preferredMethod: 'alternative', // 'alternative' | 'fallback'
  enableFallback: true, // 是否启用降级方案
  enableCache: true, // 是否启用缓存
  cacheTimeout: 10 * 60 * 1000 // 缓存超时时间（10分钟）
};

// 缓存存储
const cache = new Map();

// 当前使用的实现
let currentImplementation = null;
let isInitialized = false;

/**
 * 初始化农历管理器
 */
async function initialize() {
  if (isInitialized) {
    return currentImplementation;
  }

  try {
    // 优先使用 LunarAlternative (lunar-calendar-zh)
    if (CONFIG.preferredMethod === 'alternative') {
      currentImplementation = 'alternative';
      console.log('[LunarManager] 使用 lunar-calendar-zh 实现');
      isInitialized = true;
      return currentImplementation;
    }

    // 降级方案
    if (CONFIG.enableFallback) {
      currentImplementation = 'fallback';
      console.warn('[LunarManager] 使用降级方案');
      isInitialized = true;
      return currentImplementation;
    }

  } catch (error) {
    console.error('[LunarManager] 初始化失败:', error);
    currentImplementation = 'fallback';
    isInitialized = true;
    return currentImplementation;
  }
}

/**
 * 缓存管理
 */
function getCacheKey(method, ...args) {
  return `${method}_${JSON.stringify(args)}`;
}

function getFromCache(key) {
  if (!CONFIG.enableCache) return null;
  
  const cached = cache.get(key);
  if (cached && Date.now() - cached.timestamp < CONFIG.cacheTimeout) {
    return cached.data;
  }
  
  if (cached) {
    cache.delete(key);
  }
  
  return null;
}

function setToCache(key, data) {
  if (!CONFIG.enableCache) return;
  
  cache.set(key, {
    data,
    timestamp: Date.now()
  });
}

/**
 * 降级方案 - 基本的节气和农历功能
 */
const fallbackImplementation = {
  getSolarTermForDate(date) {
    // 简化的节气判断
    const dateStr = date instanceof Date 
      ? date.toISOString().split('T')[0] 
      : date;
    
    // 2024年的主要节气日期
    const terms2024 = {
      '2024-01-06': '小寒',
      '2024-01-20': '大寒',
      '2024-02-04': '立春',
      '2024-02-19': '雨水',
      '2024-03-05': '惊蛰',
      '2024-03-20': '春分',
      '2024-04-04': '清明',
      '2024-04-20': '谷雨',
      '2024-05-05': '立夏',
      '2024-05-21': '小满',
      '2024-06-05': '芒种',
      '2024-06-21': '夏至',
      '2024-07-07': '小暑',
      '2024-07-22': '大暑',
      '2024-08-07': '立秋',
      '2024-08-23': '处暑',
      '2024-09-07': '白露',
      '2024-09-23': '秋分',
      '2024-10-08': '寒露',
      '2024-10-23': '霜降',
      '2024-11-07': '立冬',
      '2024-11-22': '小雪',
      '2024-12-07': '大雪',
      '2024-12-21': '冬至'
    };
    
    const termName = terms2024[dateStr];
    if (termName) {
      const [year, month, day] = dateStr.split('-').map(Number);
      return {
        name: termName,
        year,
        month,
        day,
        date: dateStr
      };
    }
    
    return null;
  },

  getLunarInfo(date) {
    // 简化的农历信息
    const targetDate = date instanceof Date ? date : new Date(date);
    return {
      lunarYear: targetDate.getFullYear(),
      lunarMonth: targetDate.getMonth() + 1,
      lunarDay: targetDate.getDate(),
      lunarMonthName: `${targetDate.getMonth() + 1}月`,
      lunarDayName: `${targetDate.getDate()}日`,
      gzYear: '',
      gzMonth: '',
      gzDay: '',
      animal: '',
      lunarFestival: '',
      jieQi: '',
      isLeapMonth: false,
      fullString: '农历信息不可用'
    };
  }
};

/**
 * 统一的API接口
 */

/**
 * 获取指定日期的节气信息
 */
export function getSolarTermForDate(date) {
  const cacheKey = getCacheKey('getSolarTermForDate', date);
  const cached = getFromCache(cacheKey);
  if (cached) return cached;

  initialize();
  
  let result = null;
  
  try {
    switch (currentImplementation) {
      case 'alternative':
        result = LunarAlternative.getSolarTermForDate(date);
        break;
      case 'fallback':
      default:
        result = fallbackImplementation.getSolarTermForDate(date);
        break;
    }
  } catch (error) {
    console.warn('[LunarManager] getSolarTermForDate 失败:', error);
    result = fallbackImplementation.getSolarTermForDate(date);
  }
  
  setToCache(cacheKey, result);
  return result;
}

/**
 * 获取农历信息
 */
export function getLunarInfo(date) {
  const cacheKey = getCacheKey('getLunarInfo', date);
  const cached = getFromCache(cacheKey);
  if (cached) return cached;

  initialize();
  
  let result = null;
  
  try {
    switch (currentImplementation) {
      case 'alternative':
        result = LunarAlternative.getLunarInfo(date);
        break;
      case 'fallback':
      default:
        result = fallbackImplementation.getLunarInfo(date);
        break;
    }
  } catch (error) {
    console.warn('[LunarManager] getLunarInfo 失败:', error);
    result = fallbackImplementation.getLunarInfo(date);
  }
  
  setToCache(cacheKey, result);
  return result;
}

/**
 * 获取指定年份的所有节气
 */
export function getAllSolarTermsInYear(year) {
  const cacheKey = getCacheKey('getAllSolarTermsInYear', year);
  const cached = getFromCache(cacheKey);
  if (cached) return cached;

  initialize();
  
  let result = [];
  
  try {
    switch (currentImplementation) {
      case 'alternative':
        result = LunarAlternative.getAllSolarTermsInYear(year);
        break;
      case 'fallback':
      default:
        // 简化实现
        result = [];
        break;
    }
  } catch (error) {
    console.warn('[LunarManager] getAllSolarTermsInYear 失败:', error);
    result = [];
  }
  
  setToCache(cacheKey, result);
  return result;
}

/**
 * 获取指定月份的节气
 */
export function getSolarTermsInMonth(year, month) {
  const cacheKey = getCacheKey('getSolarTermsInMonth', year, month);
  const cached = getFromCache(cacheKey);
  if (cached) return cached;

  initialize();
  
  let result = [];
  
  try {
    switch (currentImplementation) {
      case 'alternative':
        result = LunarAlternative.getSolarTermsInMonth(year, month);
        break;
      case 'fallback':
      default:
        // 简化实现
        result = [];
        break;
    }
  } catch (error) {
    console.warn('[LunarManager] getSolarTermsInMonth 失败:', error);
    result = [];
  }
  
  setToCache(cacheKey, result);
  return result;
}

/**
 * 获取当前日期的节气信息
 */
export function getCurrentSolarTerm() {
  return getSolarTermForDate(new Date());
}

/**
 * 检查指定日期是否是节气
 */
export function isSolarTerm(date) {
  return getSolarTermForDate(date) !== null;
}

/**
 * 获取下一个节气
 */
export function getNextSolarTerm(date) {
  initialize();
  
  try {
    if (currentImplementation === 'alternative') {
      return LunarAlternative.getNextSolarTerm(date);
    }
  } catch (error) {
    console.warn('[LunarManager] getNextSolarTerm 失败:', error);
  }
  
  return null;
}

/**
 * 管理器状态和配置
 */
export const LunarManager = {
  // API 方法
  getSolarTermForDate,
  getLunarInfo,
  getAllSolarTermsInYear,
  getSolarTermsInMonth,
  getCurrentSolarTerm,
  isSolarTerm,
  getNextSolarTerm,
  
  // 状态查询
  isInitialized: () => isInitialized,
  getCurrentImplementation: () => currentImplementation,
  
  // 配置管理
  setConfig: (newConfig) => {
    Object.assign(CONFIG, newConfig);
  },
  getConfig: () => ({ ...CONFIG }),
  
  // 缓存管理
  clearCache: () => {
    cache.clear();
  },
  getCacheSize: () => cache.size,
  
  // 手动初始化
  initialize
};

export default LunarManager; 