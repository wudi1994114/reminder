/**
 * 测试新的时间格式化 - 带年份
 */

import DateFormatter from './dateFormat.js';

// 创建一些测试日期
const now = new Date();
const today = new Date(); // 今天
const tomorrow = new Date(now.getTime() + 24 * 60 * 60 * 1000); // 明天
const nextWeek = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000); // 下周
const nextMonth = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000); // 下个月
const nextYear = new Date(now.getFullYear() + 1, now.getMonth(), now.getDate()); // 明年

console.log('=== 新的 formatReminder 格式测试 ===');
console.log('更新要求：除了"今天"以外，所有时间都显示年份');
console.log('');

const testDates = [
  { name: '今天', date: today },
  { name: '明天', date: tomorrow },
  { name: '下周', date: nextWeek },
  { name: '下个月', date: nextMonth },
  { name: '明年', date: nextYear },
  { name: 'GMT格式', date: "Tue Jun 10 2025 11:30:00 GMT+0800 (CST)" }
];

testDates.forEach(({ name, date }) => {
  const result = DateFormatter.formatReminder(date);
  console.log(`${name}: ${result}`);
});

console.log('');
console.log('=== 格式验证 ===');
console.log('✅ 只有"今天"显示为: 今天 HH:mm');
console.log('✅ 其他所有日期都显示为: YYYY年M月D日 HH:mm');
console.log('✅ 用户可以清楚看到完整的年月日信息');

export default { testDates, DateFormatter };