import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn'; // 引入中文语言包
import relativeTime from 'dayjs/plugin/relativeTime'; // 相对时间插件
import customParseFormat from 'dayjs/plugin/customParseFormat'; // 自定义解析格式插件
import isToday from 'dayjs/plugin/isToday'; // 判断是否为今天的插件
import isYesterday from 'dayjs/plugin/isYesterday'; // 判断是否为昨天的插件

// 配置Day.js
dayjs.locale('zh-cn'); // 设置中文语言
dayjs.extend(relativeTime);
dayjs.extend(customParseFormat);
dayjs.extend(isToday);
dayjs.extend(isYesterday);

/**
 * 统一的时间格式化工具类
 */
export class DateFormatter {
  
  /**
   * 标准日期时间格式 - 2025年6月10日 11:30
   * @param {string|Date|number} date - 日期
   * @returns {string} 格式化后的日期时间字符串
   */
  static formatDateTime(date) {
    if (!date) return '';
    const d = dayjs(date);
    if (!d.isValid()) return '';
    return d.format('YYYY年M月D日 HH:mm');
  }

  /**
   * 短日期格式 - 2025年6月10日
   * @param {string|Date|number} date - 日期
   * @returns {string} 格式化后的日期字符串
   */
  static formatDate(date) {
    if (!date) return '';
    const d = dayjs(date);
    if (!d.isValid()) return '';
    return d.format('YYYY年M月D日');
  }

  /**
   * 时间格式 - 11:30
   * @param {string|Date|number} date - 日期
   * @returns {string} 格式化后的时间字符串
   */
  static formatTime(date) {
    if (!date) return '';
    const d = dayjs(date);
    if (!d.isValid()) return '';
    return d.format('HH:mm');
  }

  /**
   * 智能日期格式 - 根据日期智能显示
   * 今天: 今天 11:30
   * 昨天: 昨天 11:30
   * 本年: 6月10日 11:30
   * 其他: 2024年6月10日 11:30
   * @param {string|Date|number} date - 日期
   * @returns {string} 智能格式化后的日期时间字符串
   */
  static formatSmart(date) {
    if (!date) return '';
    const d = dayjs(date);
    if (!d.isValid()) return '';

    if (d.isToday()) {
      return `今天 ${d.format('HH:mm')}`;
    }
    
    if (d.isYesterday()) {
      return `昨天 ${d.format('HH:mm')}`;
    }

    // 如果是今年
    if (d.year() === dayjs().year()) {
      return d.format('M月D日 HH:mm');
    }

    // 其他年份
    return d.format('YYYY年M月D日 HH:mm');
  }

  /**
   * 相对时间格式 - 3分钟前、1小时前、2天前等
   * @param {string|Date|number} date - 日期
   * @returns {string} 相对时间字符串
   */
  static formatRelative(date) {
    if (!date) return '';
    const d = dayjs(date);
    if (!d.isValid()) return '';
    return d.fromNow();
  }

  /**
   * 日历显示格式 - 用于日历组件
   * @param {string|Date|number} date - 日期
   * @returns {string} 日历格式的日期字符串
   */
  static formatCalendar(date) {
    if (!date) return '';
    const d = dayjs(date);
    if (!d.isValid()) return '';
    
    if (d.isToday()) {
      return '今天';
    }
    
    if (d.isYesterday()) {
      return '昨天';
    }

    // 如果是本周
    const startOfWeek = dayjs().startOf('week');
    const endOfWeek = dayjs().endOf('week');
    if (d.isAfter(startOfWeek) && d.isBefore(endOfWeek)) {
      return d.format('dddd'); // 星期几
    }

    // 如果是今年
    if (d.year() === dayjs().year()) {
      return d.format('M月D日');
    }

    // 其他年份
    return d.format('YYYY年M月D日');
  }

  /**
   * 提醒列表显示格式 - 专门用于提醒列表
   * 今天: 今天 11:30
   * 其他: 始终显示年份 (2025年6月10日 11:30)
   * @param {string|Date|number} date - 日期
   * @returns {string} 提醒列表格式的日期时间字符串
   */
  static formatReminder(date) {
    if (!date) return '';
    const d = dayjs(date);
    if (!d.isValid()) return '';

    // 只有今天显示特殊格式
    if (d.isToday()) {
      return `今天 ${d.format('HH:mm')}`;
    }
    
    // 其他所有时间都显示完整的年月日
    return d.format('YYYY年M月D日 HH:mm');
  }

  /**
   * 详情页显示格式 - 用于详情页面的完整时间显示
   * @param {string|Date|number} date - 日期
   * @returns {string} 详情页格式的日期时间字符串
   */
  static formatDetail(date) {
    if (!date) return '';
    const d = dayjs(date);
    if (!d.isValid()) return '';
    return d.format('YYYY年M月D日 dddd HH:mm');
  }

  /**
   * ISO格式转换 - 用于API传输
   * @param {string|Date|number} date - 日期
   * @returns {string} ISO格式的日期字符串
   */
  static toISO(date) {
    if (!date) return '';
    const d = dayjs(date);
    if (!d.isValid()) return '';
    return d.toISOString();
  }

  /**
   * 时间戳转换
   * @param {string|Date|number} date - 日期
   * @returns {number} 时间戳
   */
  static toTimestamp(date) {
    if (!date) return 0;
    const d = dayjs(date);
    if (!d.isValid()) return 0;
    return d.valueOf();
  }

  /**
   * 判断是否为有效日期
   * @param {string|Date|number} date - 日期
   * @returns {boolean} 是否为有效日期
   */
  static isValid(date) {
    if (!date) return false;
    return dayjs(date).isValid();
  }

  /**
   * 获取当前时间的各种格式
   * @returns {Object} 包含各种格式的当前时间对象
   */
  static now() {
    const now = dayjs();
    return {
      dateTime: now.format('YYYY年M月D日 HH:mm'),
      date: now.format('YYYY年M月D日'),
      time: now.format('HH:mm'),
      iso: now.toISOString(),
      timestamp: now.valueOf(),
      dayjs: now
    };
  }

  /**
   * 解析各种可能的日期格式
   * 包括: "Tue Jun 10 2025 11:30:00 GMT+0800 (CST)" 等格式
   * @param {string|Date|number} date - 日期
   * @returns {dayjs.Dayjs|null} Day.js对象或null
   */
  static parse(date) {
    if (!date) return null;
    
    // 如果已经是Day.js对象，直接返回
    if (dayjs.isDayjs(date)) return date;
    
    // 尝试直接解析
    let d = dayjs(date);
    if (d.isValid()) return d;
    
    // 尝试解析GMT格式的字符串
    if (typeof date === 'string' && date.includes('GMT')) {
      // 处理类似 "Tue Jun 10 2025 11:30:00 GMT+0800 (CST)" 的格式
      d = dayjs(new Date(date));
      if (d.isValid()) return d;
    }
    
    return null;
  }
}

// 为了向后兼容，导出原有的函数名
export const formatDate = DateFormatter.formatDateTime;
export const formatShortDate = DateFormatter.formatDate;
export const formatTime = DateFormatter.formatTime;

// 导出新的格式化函数
export const {
  formatDateTime,
  formatSmart,
  formatRelative,
  formatCalendar,
  formatReminder,
  formatDetail,
  toISO,
  toTimestamp,
  isValid,
  now,
  parse
} = DateFormatter;

// 默认导出
export default DateFormatter;