/**
 * 轻量级农历工具 - 使用 lunar-calendar-zh 替代 lunar-typescript
 * 解决构建兼容性问题
 */

import LunarCalendar from 'lunar-calendar-zh';

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
 * 节气日期数据（2024-2025年）
 */
const SOLAR_TERMS_DATA = {
  2024: {
    '小寒': '2024-01-06',
    '大寒': '2024-01-20',
    '立春': '2024-02-04',
    '雨水': '2024-02-19',
    '惊蛰': '2024-03-05',
    '春分': '2024-03-20',
    '清明': '2024-04-04',
    '谷雨': '2024-04-20',
    '立夏': '2024-05-05',
    '小满': '2024-05-21',
    '芒种': '2024-06-05',
    '夏至': '2024-06-21',
    '小暑': '2024-07-07',
    '大暑': '2024-07-22',
    '立秋': '2024-08-07',
    '处暑': '2024-08-23',
    '白露': '2024-09-07',
    '秋分': '2024-09-23',
    '寒露': '2024-10-08',
    '霜降': '2024-10-23',
    '立冬': '2024-11-07',
    '小雪': '2024-11-22',
    '大雪': '2024-12-07',
    '冬至': '2024-12-21'
  },
  2025: {
    '小寒': '2025-01-05',
    '大寒': '2025-01-20',
    '立春': '2025-02-03',
    '雨水': '2025-02-18',
    '惊蛰': '2025-03-05',
    '春分': '2025-03-20',
    '清明': '2025-04-04',
    '谷雨': '2025-04-20',
    '立夏': '2025-05-05',
    '小满': '2025-05-21',
    '芒种': '2025-06-05',
    '夏至': '2025-06-21',
    '小暑': '2025-07-07',
    '大暑': '2025-07-22',
    '立秋': '2025-08-07',
    '处暑': '2025-08-23',
    '白露': '2025-09-07',
    '秋分': '2025-09-23',
    '寒露': '2025-10-08',
    '霜降': '2025-10-23',
    '立冬': '2025-11-07',
    '小雪': '2025-11-22',
    '大雪': '2025-12-07',
    '冬至': '2025-12-21'
  }
};

// 保持向后兼容
const SOLAR_TERMS_2024 = SOLAR_TERMS_DATA[2024];

/**
 * 获取指定日期的节气信息
 * @param {Date|string} date - 日期对象或YYYY-MM-DD格式的日期字符串
 * @returns {Object|null} - 返回节气信息对象，如果当天不是节气则返回null
 */
export function getSolarTermForDate(date) {
  try {
    let dateStr;
    let year;
    
    if (date instanceof Date) {
      year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      dateStr = `${year}-${month}-${day}`;
    } else if (typeof date === 'string') {
      dateStr = date;
      year = parseInt(dateStr.split('-')[0]);
    } else {
      return null;
    }

    // 检查是否有该年份的节气数据
    const yearData = SOLAR_TERMS_DATA[year];
    if (!yearData) {
      console.warn(`暂不支持${year}年的节气数据`);
      return null;
    }

    // 查找是否是节气日
    for (const [termName, termDate] of Object.entries(yearData)) {
      if (termDate === dateStr) {
        const termIndex = SOLAR_TERMS.indexOf(termName);
        const [y, m, d] = dateStr.split('-').map(Number);
        
        return {
          name: termName,
          index: termIndex,
          year: y,
          month: m,
          day: d,
          date: dateStr
        };
      }
    }

    return null;
  } catch (error) {
    console.warn('获取节气信息失败:', error);
    return null;
  }
}

/**
 * 获取农历信息
 * @param {Date|string} date - 日期对象或YYYY-MM-DD格式的日期字符串
 * @returns {Object} - 返回农历信息对象
 */
export function getLunarInfo(date) {
  try {
    let targetDate;
    if (date instanceof Date) {
      targetDate = date;
    } else if (typeof date === 'string') {
      targetDate = new Date(date);
    } else {
      throw new Error('日期格式不正确');
    }

    // 使用 lunar-calendar-zh 获取农历信息
    const lunarData = LunarCalendar.solarToLunar(
      targetDate.getFullYear(),
      targetDate.getMonth() + 1,
      targetDate.getDate()
    );

    // 获取节气信息
    const solarTerm = getSolarTermForDate(date);

    return {
      lunarYear: lunarData.lunarYear,
      lunarMonth: lunarData.lunarMonth,
      lunarDay: lunarData.lunarDay,
      lunarMonthName: lunarData.lunarMonthName || `${lunarData.lunarMonth}月`,
      lunarDayName: lunarData.lunarDayName || `${lunarData.lunarDay}日`,
      gzYear: lunarData.gzYear || '',
      gzMonth: lunarData.gzMonth || '',
      gzDay: lunarData.gzDay || '',
      animal: lunarData.zodiac || '',
      lunarFestival: lunarData.lunarFestival || '',
      jieQi: solarTerm ? solarTerm.name : '',
      isLeapMonth: lunarData.isLeapMonth || false,
      fullString: `${lunarData.lunarYear}年${lunarData.lunarMonthName}${lunarData.lunarDayName}`
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

/**
 * 获取指定年份的所有节气日期
 * @param {number} year - 年份
 * @returns {Array} - 返回包含节气信息的数组
 */
export function getAllSolarTermsInYear(year) {
  const result = [];
  
  // 检查是否有该年份的节气数据
  const yearData = SOLAR_TERMS_DATA[year];
  if (!yearData) {
    console.warn(`暂不支持${year}年的节气数据`);
    return result;
  }

  for (const [termName, termDate] of Object.entries(yearData)) {
    const [y, m, d] = termDate.split('-').map(Number);
    const termIndex = SOLAR_TERMS.indexOf(termName);
    
    result.push({
      name: termName,
      year: y,
      month: m,
      day: d,
      date: termDate,
      index: termIndex
    });
  }

  return result.sort((a, b) => a.index - b.index);
}

/**
 * 获取指定月份的所有节气
 * @param {number} year - 年份
 * @param {number} month - 月份(1-12)
 * @returns {Array} - 返回指定月份的节气数组
 */
export function getSolarTermsInMonth(year, month) {
  const allTerms = getAllSolarTermsInYear(year);
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
 * 检查指定日期是否是节气
 * @param {Date|string} date - 日期
 * @returns {boolean} - 是否是节气
 */
export function isSolarTerm(date) {
  return getSolarTermForDate(date) !== null;
}

/**
 * 获取下一个节气
 * @param {Date|string} date - 当前日期
 * @returns {Object|null} - 下一个节气信息
 */
export function getNextSolarTerm(date) {
  try {
    const currentDate = date instanceof Date ? date : new Date(date);
    const year = currentDate.getFullYear();
    
    // 目前只支持2024年
    if (year !== 2024) {
      return null;
    }

    const allTerms = getAllSolarTermsInYear(year);
    const currentDateStr = currentDate.toISOString().split('T')[0];

    for (const term of allTerms) {
      if (term.date > currentDateStr) {
        return term;
      }
    }

    return null;
  } catch (error) {
    console.warn('获取下一个节气失败:', error);
    return null;
  }
}

/**
 * 导出的API
 */
export const LunarAlternative = {
  SOLAR_TERMS,
  getSolarTermForDate,
  getLunarInfo,
  getAllSolarTermsInYear,
  getSolarTermsInMonth,
  getCurrentSolarTerm,
  isSolarTerm,
  getNextSolarTerm
};

export default LunarAlternative; 