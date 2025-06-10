/**
 * 测试时间格式化功能
 * 验证 formatReminder 方法的新逻辑
 */

import DateFormatter from './dateFormat.js';

// 创建测试用的日期
const now = new Date();
const today = new Date();
const tomorrow = new Date(now.getTime() + 24 * 60 * 60 * 1000);
const nextWeek = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
const nextMonth = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000);
const nextYear = new Date(now.getFullYear() + 1, now.getMonth(), now.getDate());

// 测试用例
const testCases = [
  {
    name: '今天',
    date: today,
    expected: '今天 + 时间'
  },
  {
    name: '明天',
    date: tomorrow,
    expected: '具体日期 + 时间'
  },
  {
    name: '下周',
    date: nextWeek,
    expected: '具体日期 + 时间'
  },
  {
    name: '下个月',
    date: nextMonth,
    expected: '具体日期 + 时间'
  },
  {
    name: '明年',
    date: nextYear,
    expected: '年月日 + 时间'
  },
  {
    name: 'GMT格式字符串',
    date: "Tue Jun 10 2025 11:30:00 GMT+0800 (CST)",
    expected: '2025年6月10日 11:30'
  }
];

console.log('=== 测试 formatReminder 方法 ===');
console.log('需求：只有"今天"显示特殊格式，其他时间都始终显示年份');
console.log('这样用户可以一眼看到完整的日期信息，包括年份');
console.log('');

testCases.forEach(testCase => {
  const result = DateFormatter.formatReminder(testCase.date);
  console.log(`${testCase.name}:`);
  console.log(`  输入: ${testCase.date}`);
  console.log(`  结果: ${result}`);
  console.log(`  期望: ${testCase.expected}`);
  console.log('');
});

// 额外测试：比较新旧格式的差异
console.log('=== 格式对比 ===');
const sampleDates = [
  today,
  tomorrow,
  nextWeek,
  "Tue Jun 10 2025 11:30:00 GMT+0800 (CST)"
];

sampleDates.forEach(date => {
  console.log(`日期: ${date}`);
  console.log(`  formatReminder: ${DateFormatter.formatReminder(date)}`);
  console.log(`  formatSmart:    ${DateFormatter.formatSmart(date)}`);
  console.log(`  formatDateTime: ${DateFormatter.formatDateTime(date)}`);
  console.log('');
});

export { testCases, DateFormatter };