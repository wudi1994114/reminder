/**
 * 节气辅助工具 - 使用lunar-typescript库计算和获取节气信息
 * 
 * 该模块提供了与二十四节气相关的计算和转换功能，用于日历显示和提醒功能。
 */

import { Solar, SolarTerm } from 'lunar-typescript';

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
  
  // 遍历24个节气
  for (let i = 0; i < 24; i++) {
    // 使用lunar-typescript库计算节气日期
    const solarDate = SolarTerm.fromIndex(year, i).getSolar();
    
    result.push({
      name: SOLAR_TERMS[i],
      year: solarDate.getYear(),
      month: solarDate.getMonth(),
      day: solarDate.getDay(),
      date: solarDate.toYmd(), // 格式化为YYYY-MM-DD
      index: i
    });
  }
  
  return result;
}

/**
 * 获取指定日期的节气信息
 * @param {Date|string} date - 日期对象或YYYY-MM-DD格式的日期字符串
 * @returns {Object|null} - 返回节气信息对象，如果当天不是节气则返回null
 */
export function getSolarTermForDate(date) {
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
}

/**
 * 获取指定月份的所有节气
 * @param {number} year - 年份
 * @param {number} month - 月份(1-12)
 * @returns {Array} - 返回指定月份的节气数组
 */
export function getSolarTermsInMonth(year, month) {
  // 获取全年节气
  const allTerms = getAllSolarTermsInYear(year);
  
  // 筛选指定月份的节气
  return allTerms.filter(term => term.month === month);
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
    isLeapMonth: lunar.isLeapMonth(), // 是否闰月
    fullString: lunar.toFullString() // 完整农历字符串
  };
}

export default {
  SOLAR_TERMS,
  getAllSolarTermsInYear,
  getSolarTermForDate,
  getSolarTermsInMonth,
  getCurrentSolarTerm,
  getLunarInfo
}; 