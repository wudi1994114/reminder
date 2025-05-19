// 测试cron-parser库的使用方式
import cronParser from 'cron-parser';

console.log('cronParser类型:', typeof cronParser);
console.log('cronParser内容:', Object.keys(cronParser));

try {
  // 测试解析一个标准的cron表达式
  const interval = cronParser.parseExpression('0 8 * * *');
  console.log('下一次触发时间:', interval.next().toDate());
  console.log('下5次触发时间:');
  
  // 查看未来5次的触发时间
  for (let i = 0; i < 5; i++) {
    const next = interval.next();
    console.log(`第${i+1}次:`, next.toDate());
  }
} catch (e) {
  console.error('解析cron表达式出错:', e);
} 