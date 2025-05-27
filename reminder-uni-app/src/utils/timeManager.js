/**
 * 统一时间管理器
 * 处理简单模式（一次性提醒）和高级模式（重复提醒）的时间数据
 */

// 时间类型枚举
export const TimeType = {
  ONCE: 'ONCE',           // 一次性
  DAILY: 'DAILY',         // 每天
  WEEKLY: 'WEEKLY',       // 每周
  MONTHLY: 'MONTHLY',     // 每月
  YEARLY: 'YEARLY',       // 每年
  CUSTOM: 'CUSTOM'        // 自定义
};

// 统一的时间数据结构
export class UnifiedTimeData {
  constructor() {
    // 基础数据
    this.type = TimeType.ONCE;           // 时间类型
    this.eventTime = '';                 // 事件时间（用于一次性提醒）
    this.cronExpression = '';            // Cron表达式（用于重复提醒）
    
    // 时间组件数据
    this.date = '';                      // 日期 YYYY-MM-DD
    this.time = '';                      // 时间 HH:mm
    this.hour = 9;                       // 小时
    this.minute = 0;                     // 分钟
    
    // 重复设置数据
    this.weekdays = [];                  // 星期选择 [0-6]
    this.monthDays = [];                 // 月份中的日期 [1-31]
    this.months = [];                    // 月份选择 [1-12]
    
    // 描述信息
    this.description = '';               // 人类可读的描述
    this.previewTimes = [];              // 预览时间列表
  }
  
  // 从简单模式数据创建
  static fromSimpleMode(date, time) {
    const data = new UnifiedTimeData();
    data.type = TimeType.ONCE;
    data.date = date;
    data.time = time;
    data.eventTime = `${date} ${time}:00`;
    
    // 解析时间
    const [hour, minute] = time.split(':');
    data.hour = parseInt(hour);
    data.minute = parseInt(minute);
    
    data.description = `${date} ${time}`;
    return data;
  }
  
  // 从Cron表达式创建
  static fromCronExpression(cronExpression, type = TimeType.CUSTOM) {
    const data = new UnifiedTimeData();
    data.type = type;
    data.cronExpression = cronExpression;
    
    // 解析Cron表达式
    const parsed = parseCronExpression(cronExpression);
    if (parsed) {
      data.hour = parsed.hour;
      data.minute = parsed.minute;
      data.weekdays = parsed.weekdays || [];
      data.monthDays = parsed.monthDays || [];
      data.months = parsed.months || [];
    }
    
    return data;
  }
}

// 解析Cron表达式
function parseCronExpression(cronExpression) {
  if (!cronExpression) return null;
  
  try {
    const parts = cronExpression.trim().split(/\s+/);
    let minute, hour, day, month, weekday;
    
    if (parts.length === 5) {
      // 5位格式: 分 时 日 月 周
      [minute, hour, day, month, weekday] = parts;
    } else if (parts.length >= 6) {
      // 6位或7位格式: 秒 分 时 日 月 周 [年]
      [, minute, hour, day, month, weekday] = parts;
    } else {
      return null;
    }
    
    const result = {
      minute: parseInt(minute) || 0,
      hour: parseInt(hour) || 0
    };
    
    // 解析星期
    if (weekday && weekday !== '*' && weekday !== '?') {
      result.weekdays = weekday.split(',').map(w => parseInt(w));
    }
    
    // 解析日期
    if (day && day !== '*' && day !== '?') {
      result.monthDays = day.split(',').map(d => parseInt(d));
    }
    
    // 解析月份
    if (month && month !== '*' && month !== '?') {
      result.months = month.split(',').map(m => parseInt(m));
    }
    
    return result;
  } catch (error) {
    console.error('解析Cron表达式失败:', error);
    return null;
  }
}

// 生成Cron表达式
export function generateCronExpression(timeData) {
  const { type, hour, minute, weekdays, monthDays, months } = timeData;
  
  // 确保时间格式正确
  const h = String(hour).padStart(2, '0');
  const m = String(minute).padStart(2, '0');
  
  switch (type) {
    case TimeType.ONCE:
      // 一次性任务不需要Cron表达式
      return '';
      
    case TimeType.DAILY:
      // 每天: 分 时 * * *
      return `${m} ${h} * * *`;
      
    case TimeType.WEEKLY:
      // 每周: 分 时 * * 周几
      const weekStr = weekdays.length > 0 ? weekdays.join(',') : '1';
      return `${m} ${h} * * ${weekStr}`;
      
    case TimeType.MONTHLY:
      // 每月: 分 时 日 * *
      const dayStr = monthDays.length > 0 ? monthDays.join(',') : '1';
      return `${m} ${h} ${dayStr} * *`;
      
    case TimeType.YEARLY:
      // 每年: 分 时 日 月 *
      const yearDayStr = monthDays.length > 0 ? monthDays.join(',') : '1';
      const monthStr = months.length > 0 ? months.join(',') : '1';
      return `${m} ${h} ${yearDayStr} ${monthStr} *`;
      
    case TimeType.CUSTOM:
      // 自定义：直接返回已有的Cron表达式
      return timeData.cronExpression || '';
      
    default:
      return '';
  }
}

// 生成人类可读的描述
export function generateDescription(timeData) {
  const { type, date, time, hour, minute, weekdays, monthDays, months } = timeData;
  
  // 格式化时间
  const timeStr = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
  
  switch (type) {
    case TimeType.ONCE:
      // 一次性：2024年1月15日 09:00
      if (date && time) {
        const [year, month, day] = date.split('-');
        return `${year}年${parseInt(month)}月${parseInt(day)}日 ${time}`;
      }
      return '一次性提醒';
      
    case TimeType.DAILY:
      return `每天 ${timeStr}`;
      
    case TimeType.WEEKLY:
      if (weekdays.length === 0) {
        return `每周一 ${timeStr}`;
      } else if (weekdays.length === 7) {
        return `每天 ${timeStr}`;
      } else {
        const weekNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
        const days = weekdays.map(d => weekNames[d]).join('、');
        return `每${days} ${timeStr}`;
      }
      
    case TimeType.MONTHLY:
      if (monthDays.length === 0) {
        return `每月1日 ${timeStr}`;
      } else {
        const days = monthDays.join('、');
        return `每月${days}日 ${timeStr}`;
      }
      
    case TimeType.YEARLY:
      if (months.length === 0 && monthDays.length === 0) {
        return `每年1月1日 ${timeStr}`;
      } else {
        const monthStr = months.length > 0 ? months.join('、') + '月' : '每月';
        const dayStr = monthDays.length > 0 ? monthDays.join('、') + '日' : '1日';
        return `每年${monthStr}${dayStr} ${timeStr}`;
      }
      
    case TimeType.CUSTOM:
      return `自定义规则 ${timeStr}`;
      
    default:
      return '未知规则';
  }
}

// 判断时间类型
export function detectTimeType(cronExpression) {
  if (!cronExpression) return TimeType.ONCE;
  
  const parts = cronExpression.trim().split(/\s+/);
  let day, month, weekday;
  
  if (parts.length === 5) {
    [, , day, month, weekday] = parts;
  } else if (parts.length >= 6) {
    [, , , day, month, weekday] = parts;
  } else {
    return TimeType.CUSTOM;
  }
  
  // 每天
  if (day === '*' && month === '*' && weekday === '*') {
    return TimeType.DAILY;
  }
  
  // 每周
  if (day === '*' && month === '*' && weekday !== '*' && weekday !== '?') {
    return TimeType.WEEKLY;
  }
  
  // 每月
  if (day !== '*' && day !== '?' && month === '*' && (weekday === '*' || weekday === '?')) {
    return TimeType.MONTHLY;
  }
  
  // 每年
  if (day !== '*' && day !== '?' && month !== '*' && month !== '?') {
    return TimeType.YEARLY;
  }
  
  return TimeType.CUSTOM;
}

// 生成预览时间列表
export function generatePreviewTimes(timeData, count = 5) {
  const { type, eventTime, cronExpression, hour, minute, weekdays, monthDays, months } = timeData;
  const previewTimes = [];
  const now = new Date();
  
  // 一次性提醒
  if (type === TimeType.ONCE && eventTime) {
    const eventDate = new Date(eventTime.replace(' ', 'T'));
    if (eventDate > now) {
      previewTimes.push(formatPreviewTime(eventDate));
    }
    return previewTimes;
  }
  
  // 重复提醒
  let currentDate = new Date(now);
  currentDate.setHours(hour, minute, 0, 0);
  
  // 如果今天的时间已过，从明天开始
  if (currentDate <= now) {
    currentDate.setDate(currentDate.getDate() + 1);
  }
  
  let attempts = 0;
  const maxAttempts = 365; // 最多尝试一年
  
  while (previewTimes.length < count && attempts < maxAttempts) {
    attempts++;
    
    if (matchesSchedule(currentDate, type, weekdays, monthDays, months)) {
      previewTimes.push(formatPreviewTime(currentDate));
    }
    
    // 移动到下一天
    currentDate = new Date(currentDate);
    currentDate.setDate(currentDate.getDate() + 1);
  }
  
  return previewTimes;
}

// 检查日期是否匹配计划
function matchesSchedule(date, type, weekdays, monthDays, months) {
  const dayOfWeek = date.getDay();
  const dayOfMonth = date.getDate();
  const month = date.getMonth() + 1;
  
  switch (type) {
    case TimeType.DAILY:
      return true;
      
    case TimeType.WEEKLY:
      return weekdays.length === 0 ? dayOfWeek === 1 : weekdays.includes(dayOfWeek);
      
    case TimeType.MONTHLY:
      return monthDays.length === 0 ? dayOfMonth === 1 : monthDays.includes(dayOfMonth);
      
    case TimeType.YEARLY:
      const monthMatch = months.length === 0 ? month === 1 : months.includes(month);
      const dayMatch = monthDays.length === 0 ? dayOfMonth === 1 : monthDays.includes(dayOfMonth);
      return monthMatch && dayMatch;
      
    default:
      return false;
  }
}

// 格式化预览时间
function formatPreviewTime(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  
  // 获取星期
  const weekNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
  const weekday = weekNames[date.getDay()];
  
  // 判断是否是今天或明天
  const today = new Date();
  const tomorrow = new Date(today);
  tomorrow.setDate(tomorrow.getDate() + 1);
  
  let datePrefix = '';
  if (date.toDateString() === today.toDateString()) {
    datePrefix = '今天';
  } else if (date.toDateString() === tomorrow.toDateString()) {
    datePrefix = '明天';
  } else {
    datePrefix = `${month}月${day}日`;
  }
  
  return `${datePrefix} ${weekday} ${hours}:${minutes}`;
}

// 转换数据格式：从后端数据到UnifiedTimeData
export function convertFromBackend(reminderData) {
  const { eventTime, cronExpression, reminderType } = reminderData;
  
  // 一次性提醒
  if (eventTime && !cronExpression) {
    const [date, timeWithSeconds] = eventTime.split(' ');
    const time = timeWithSeconds.substring(0, 5); // HH:mm
    return UnifiedTimeData.fromSimpleMode(date, time);
  }
  
  // 重复提醒
  if (cronExpression) {
    const type = detectTimeType(cronExpression);
    const data = UnifiedTimeData.fromCronExpression(cronExpression, type);
    
    // 生成预览时间
    data.previewTimes = generatePreviewTimes(data);
    data.description = generateDescription(data);
    
    return data;
  }
  
  // 默认返回空数据
  return new UnifiedTimeData();
}

// 转换数据格式：从UnifiedTimeData到后端数据
export function convertToBackend(timeData) {
  const result = {};
  
  if (timeData.type === TimeType.ONCE) {
    // 一次性提醒
    result.eventTime = timeData.eventTime;
    result.cronExpression = null;
  } else {
    // 重复提醒
    result.eventTime = null;
    result.cronExpression = generateCronExpression(timeData);
  }
  
  return result;
}

// 更新导出
export default {
  TimeType,
  UnifiedTimeData,
  parseCronExpression,
  generateCronExpression,
  generateDescription,
  detectTimeType,
  generatePreviewTimes,
  convertFromBackend,
  convertToBackend
}; 