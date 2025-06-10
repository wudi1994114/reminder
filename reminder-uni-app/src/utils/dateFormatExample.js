/**
 * 时间格式化工具使用示例
 * 
 * 这个文件展示了如何使用新的 DateFormatter 工具类
 * 来处理各种时间格式化需求，特别是解决 GMT+0800 (CST) 格式的问题
 */

import DateFormatter from './dateFormat.js';

// 示例：处理各种时间格式
const examples = {
  // 原始的GMT格式字符串（问题格式）
  gmtString: "Tue Jun 10 2025 11:30:00 GMT+0800 (CST)",
  
  // ISO格式字符串
  isoString: "2025-06-10T11:30:00.000Z",
  
  // 普通日期字符串
  dateString: "2025-06-10 11:30:00",
  
  // 时间戳
  timestamp: 1749513000000,
  
  // Date对象
  dateObject: new Date("2025-06-10T11:30:00")
};

console.log('=== 时间格式化工具使用示例 ===');

// 测试各种格式化方法
Object.entries(examples).forEach(([key, value]) => {
  console.log(`\n--- 处理 ${key}: ${value} ---`);
  
  // 基础格式化
  console.log('标准日期时间:', DateFormatter.formatDateTime(value));
  console.log('短日期格式:', DateFormatter.formatDate(value));
  console.log('时间格式:', DateFormatter.formatTime(value));
  
  // 智能格式化
  console.log('智能格式:', DateFormatter.formatSmart(value));
  console.log('相对时间:', DateFormatter.formatRelative(value));
  console.log('日历格式:', DateFormatter.formatCalendar(value));
  console.log('提醒格式:', DateFormatter.formatReminder(value));
  console.log('详情格式:', DateFormatter.formatDetail(value));
  
  // 工具方法
  console.log('是否有效:', DateFormatter.isValid(value));
  console.log('ISO格式:', DateFormatter.toISO(value));
  console.log('时间戳:', DateFormatter.toTimestamp(value));
});

// 特殊测试：GMT格式解析
console.log('\n=== GMT格式特殊测试 ===');
const gmtDate = "Tue Jun 10 2025 11:30:00 GMT+0800 (CST)";
const parsed = DateFormatter.parse(gmtDate);
console.log('原始GMT字符串:', gmtDate);
console.log('解析结果:', parsed ? parsed.format('YYYY-MM-DD HH:mm:ss') : '解析失败');
console.log('格式化结果:', DateFormatter.formatDateTime(gmtDate));

// 当前时间示例
console.log('\n=== 当前时间示例 ===');
const now = DateFormatter.now();
console.log('当前时间对象:', now);

export default {
  examples,
  DateFormatter
};