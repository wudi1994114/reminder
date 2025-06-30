/**
 * 倒计时工具函数
 * 用于计算和格式化复杂提醒的下次触发倒计时
 */

import { parseCronExpression } from './timeManager.js';

/**
 * 计算下次触发时间
 * @param {string} cronExpression - Cron表达式
 * @returns {Date|null} 下次触发时间，如果无法计算则返回null
 */
export function getNextTriggerTime(cronExpression) {
  if (!cronExpression) return null;
  
  try {
    // 解析Cron表达式
    const parsed = parseCronExpression(cronExpression);
    if (!parsed) return null;
    
    const now = new Date();
    const { hour, minute, weekdays, monthDays, months } = parsed;
    
    // 创建候选时间，从今天开始找
    let candidateDate = new Date(now);
    candidateDate.setHours(hour, minute, 0, 0);
    
    // 如果今天的时间已过，从明天开始
    if (candidateDate <= now) {
      candidateDate.setDate(candidateDate.getDate() + 1);
    }
    
    // 最多搜索365天
    for (let i = 0; i < 365; i++) {
      if (matchesCronSchedule(candidateDate, weekdays, monthDays, months)) {
        return candidateDate;
      }
      candidateDate.setDate(candidateDate.getDate() + 1);
    }
    
    return null;
  } catch (error) {
    console.error('计算下次触发时间失败:', error);
    return null;
  }
}

/**
 * 检查日期是否匹配Cron调度
 * @param {Date} date - 要检查的日期
 * @param {Array} weekdays - 星期数组 (0-6)
 * @param {Array} monthDays - 月份日期数组 (1-31)
 * @param {Array} months - 月份数组 (1-12)
 * @returns {boolean} 是否匹配
 */
function matchesCronSchedule(date, weekdays, monthDays, months) {
  const dayOfWeek = date.getDay();
  const dayOfMonth = date.getDate();
  const month = date.getMonth() + 1;
  
  // 检查月份
  if (months && months.length > 0 && !months.includes(month)) {
    return false;
  }
  
  // 检查星期
  if (weekdays && weekdays.length > 0 && !weekdays.includes(dayOfWeek)) {
    return false;
  }
  
  // 检查日期
  if (monthDays && monthDays.length > 0 && !monthDays.includes(dayOfMonth)) {
    return false;
  }
  
  return true;
}

/**
 * 计算倒计时
 * @param {Date} targetTime - 目标时间
 * @returns {Object} 倒计时对象 {days, hours, minutes, seconds, totalSeconds}
 */
export function calculateCountdown(targetTime) {
  if (!targetTime) return null;
  
  const now = new Date();
  const diffMs = targetTime.getTime() - now.getTime();
  
  if (diffMs <= 0) return null; // 时间已过
  
  const totalSeconds = Math.floor(diffMs / 1000);
  const days = Math.floor(totalSeconds / (24 * 60 * 60));
  const hours = Math.floor((totalSeconds % (24 * 60 * 60)) / (60 * 60));
  const minutes = Math.floor((totalSeconds % (60 * 60)) / 60);
  const seconds = totalSeconds % 60;
  
  return {
    days,
    hours,
    minutes,
    seconds,
    totalSeconds
  };
}

/**
 * 格式化倒计时文本
 * @param {string} title - 提醒标题
 * @param {Object} countdown - 倒计时对象
 * @returns {string} 格式化的倒计时文本
 */
export function formatCountdownText(title, countdown) {
  if (!countdown) return '';
  
  const { days, hours, minutes } = countdown;
  
  if (days > 0) {
    // 大于一天：显示天、小时、分钟
    return `距离${title}还有${days}天 ${hours}:${String(minutes).padStart(2, '0')}`;
  } else {
    // 不足一天：只显示小时和分钟
    return `距离${title}还有${hours}小时${minutes}分`;
  }
}

/**
 * 获取复杂提醒的倒计时文本
 * @param {Object} reminder - 复杂提醒对象
 * @returns {string} 倒计时文本
 */
export function getComplexReminderCountdown(reminder) {
  if (!reminder || !reminder.cronExpression) return '';
  
  try {
    const nextTriggerTime = getNextTriggerTime(reminder.cronExpression);
    if (!nextTriggerTime) return '';
    
    const countdown = calculateCountdown(nextTriggerTime);
    if (!countdown) return '';
    
    return formatCountdownText(reminder.title, countdown);
  } catch (error) {
    console.error('获取复杂提醒倒计时失败:', error);
    return '';
  }
}

/**
 * 创建动态倒计时更新器
 * @param {Function} updateCallback - 更新回调函数
 * @param {Object} reminder - 复杂提醒对象
 * @returns {Object} 包含start和stop方法的对象
 */
export function createCountdownUpdater(updateCallback, reminder) {
  let intervalId = null;
  
  const update = () => {
    const countdownText = getComplexReminderCountdown(reminder);
    updateCallback(countdownText);
  };
  
  const start = () => {
    // 立即更新一次
    update();
    // 每秒更新一次
    intervalId = setInterval(update, 1000);
  };
  
  const stop = () => {
    if (intervalId) {
      clearInterval(intervalId);
      intervalId = null;
    }
  };
  
  return {
    start,
    stop,
    update
  };
} 