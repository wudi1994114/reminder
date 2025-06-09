/**
 * 节气辅助工具 - 使用lunar-typescript库计算和获取节气信息
 * 
 * 该模块提供了与二十四节气相关的计算和转换功能，用于日历显示和提醒功能。
 */

import { Solar } from 'lunar-typescript';

/**
 * 二十四节气名称数组
 */
export const SOLAR_TERMS = [
  '小寒', '大寒', 
  '立春', '雨水', '惊蛰', '春分', '清明', '谷雨', 
  '立夏', '小满', '芒种', '夏至', '小暑', '大暑', 
  '立秋', '处暑', '白露', '秋分', '寒露', '霜降', 
  '立冬', '小雪', '大雪', '冬至'
];

/**
 * 获取指定年份的所有节气日期
 * @param {number} year - 年份
 * @returns {Array} - 返回包含节气信息的数组，每个元素包含节气名称、日期等信息
 */
export function getAllSolarTermsInYear(year) {
  const result = [];
  
  // 遍历12个月，每月有两个节气
  for (let month = 1; month <= 12; month++) {
    // 获取当月的节气日期
    const monthTerms = getMonthSolarTerms(year, month);
    result.push(...monthTerms);
  }
  
  return result;
}

/**
 * 获取指定月份的节气
 * @param {number} year - 年份
 * @param {number} month - 月份(1-12)
 * @returns {Array} - 返回指定月份的节气数组
 */
function getMonthSolarTerms(year, month) {
  const result = [];
  
  // 每个月大约有两个节气，我们遍历整个月来查找
  const daysInMonth = new Date(year, month, 0).getDate();
  
  for (let day = 1; day <= daysInMonth; day++) {
    try {
      const solar = Solar.fromYmd(year, month, day);
      const lunar = solar.getLunar();
      const jieQi = lunar.getJieQi();
      
      if (jieQi && SOLAR_TERMS.includes(jieQi)) {
        const termIndex = SOLAR_TERMS.indexOf(jieQi);
        result.push({
          name: jieQi,
          year: year,
          month: month,
          day: day,
          date: `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`,
          index: termIndex
        });
      }
    } catch (error) {
      // 忽略无效日期
      continue;
    }
  }
  
  return result;
}

/**
 * 获取指定日期的节气信息
 * @param {Date|string} date - 日期对象或YYYY-MM-DD格式的日期字符串
 * @returns {Object|null} - 返回节气信息对象，如果当天不是节气则返回null
 */
export function getSolarTermForDate(date) {
  try {
    // 转换为Solar对象
    let solar;
    if (date instanceof Date) {
      solar = Solar.fromDate(date);
    } else if (typeof date === 'string') {
      const [year, month, day] = date.split('-').map(Number);
      solar = Solar.fromYmd(year, month, day);
    } else {
      throw new Error('日期格式不正确');
    }
    
    // 获取对应的农历对象
    const lunar = solar.getLunar();
    
    // 获取节气名称，如果不是节气日则返回null
    const termName = lunar.getJieQi();
    if (!termName || !SOLAR_TERMS.includes(termName)) {
      return null;
    }
    
    // 获取节气索引
    const termIndex = SOLAR_TERMS.indexOf(termName);
    
    return {
      name: termName,
      index: termIndex,
      year: solar.getYear(),
      month: solar.getMonth(),
      day: solar.getDay(),
      date: solar.toYmd()
    };
  } catch (error) {
    console.warn('获取节气信息失败:', error);
    return null;
  }
}

/**
 * 获取指定月份的所有节气
 * @param {number} year - 年份
 * @param {number} month - 月份(1-12)
 * @returns {Array} - 返回指定月份的节气数组
 */
export function getSolarTermsInMonth(year, month) {
  return getMonthSolarTerms(year, month);
}

/**
 * 获取当前日期的节气信息
 * @returns {Object|null} - 返回当天的节气信息，如果当天不是节气则返回null
 */
export function getCurrentSolarTerm() {
  return getSolarTermForDate(new Date());
}

/**
 * 获取农历信息
 * @param {Date|string} date - 日期对象或YYYY-MM-DD格式的日期字符串
 * @returns {Object} - 返回农历信息对象
 */
export function getLunarInfo(date) {
  try {
    // 转换为Solar对象
    let solar;
    if (date instanceof Date) {
      solar = Solar.fromDate(date);
    } else if (typeof date === 'string') {
      const [year, month, day] = date.split('-').map(Number);
      solar = Solar.fromYmd(year, month, day);
    } else {
      throw new Error('日期格式不正确');
    }
    
    // 获取农历信息
    const lunar = solar.getLunar();
    
    return {
      lunarYear: lunar.getYear(),
      lunarMonth: lunar.getMonth(),
      lunarDay: lunar.getDay(),
      lunarMonthName: lunar.getMonthInChinese(),
      lunarDayName: lunar.getDayInChinese(),
      gzYear: lunar.getYearInGanZhi(), // 干支纪年
      gzMonth: lunar.getMonthInGanZhi(), // 干支纪月
      gzDay: lunar.getDayInGanZhi(), // 干支纪日
      animal: lunar.getYearShengXiao(), // 生肖
      lunarFestival: lunar.getFestivals().join(','), // 农历节日
      jieQi: lunar.getJieQi(), // 节气
      isLeapMonth: lunar.isLeapMonth, // 是否闰月
      fullString: lunar.toFullString() // 完整农历字符串
    };
  } catch (error) {
    console.warn('获取农历信息失败:', error);
    return {
      lunarYear: 0,
      lunarMonth: 0,
      lunarDay: 0,
      lunarMonthName: '',
      lunarDayName: '',
      gzYear: '',
      gzMonth: '',
      gzDay: '',
      animal: '',
      lunarFestival: '',
      jieQi: '',
      isLeapMonth: false,
      fullString: ''
    };
  }
}

export default {
  SOLAR_TERMS,
  getAllSolarTermsInYear,
  getSolarTermForDate,
  getSolarTermsInMonth,
  getCurrentSolarTerm,
  getLunarInfo
}; 