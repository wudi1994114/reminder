<template>
  <view 
    class="reminder-card complex-card" 
    @click="handleCardClick"
  >
    <view class="card-content">
      <view class="reminder-main">
        <text class="reminder-title">{{ reminder.title }}</text>
        
        <!-- 倒计时显示 -->
        <view v-if="countdownText" class="countdown-container">
          <text class="countdown-text">{{ countdownText }}</text>
        </view>
        
        <view class="reminder-cron-container">
          <text class="reminder-cron" :class="{ 'marquee': isLongText(formatCronDescription(reminder.cronExpression)) }">
            {{ formatCronDescription(reminder.cronExpression) }}
          </text>
        </view>
        <text v-if="reminder.description" class="reminder-desc">{{ reminder.description }}</text>
        <view class="reminder-meta">
          <text class="meta-item">{{ getReminderTypeText(reminder.reminderType) }}</text>
          <text v-if="reminder.validFrom || reminder.validUntil" class="meta-item">
            {{ formatDateRange(reminder.validFrom, reminder.validUntil) }}
          </text>
        </view>
      </view>
      <view class="reminder-actions">
        <button class="action-icon-btn" @click.stop="handleEdit">
          <text class="action-icon">✏️</text>
        </button>
        <button class="action-icon-btn delete-btn" @click.stop="handleDelete">
          <text class="action-icon">🗑️</text>
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue';
import { createCountdownUpdater } from '@/utils/countdown';

export default {
  name: 'ComplexReminderCard',
  props: {
    reminder: {
      type: Object,
      required: true
    }
  },
  emits: ['click', 'edit', 'delete'],
  setup(props, { emit }) {
    // 倒计时相关状态
    const countdownText = ref('');
    let countdownUpdater = null;
    
    const handleCardClick = () => {
      // 只提供视觉反馈，不触发任何实际功能
      // 点击动画效果由CSS的:active伪类处理
      console.log('复杂提醒卡片被点击:', props.reminder.title);
    };
    
    const handleEdit = () => {
      emit('edit', props.reminder.id);
    };
    
    const handleDelete = () => {
      emit('delete', props.reminder.id);
    };
    
    // 格式化Cron表达式描述
    const formatCronDescription = (cronExpression) => {
      console.log('解析Cron表达式:', cronExpression);
      
      if (!cronExpression || cronExpression.trim() === '') {
        console.log('Cron表达式为空');
        return '请设置重复规则';
      }
      
      try {
        // 解析cron表达式 (统一使用5位格式: 分 时 日 月 周)
        const parts = cronExpression.trim().split(/\s+/);
        console.log('Cron表达式分割结果:', parts);
        
        if (parts.length < 5) {
          console.log('Cron表达式位数不足');
          return '无效的Cron表达式';
        }
        
        let minute, hour, day, month, weekday;
        
        if (parts.length === 5) {
          // 5位格式: 分 时 日 月 周
          [minute, hour, day, month, weekday] = parts;
          console.log('解析为5位格式');
        } else if (parts.length === 6) {
          // 6位格式: 秒 分 时 日 月 周 - 忽略秒
          [, minute, hour, day, month, weekday] = parts;
          console.log('解析为6位格式，忽略秒');
        } else {
          // 7位格式: 秒 分 时 日 月 周 年 - 忽略秒和年
          [, minute, hour, day, month, weekday] = parts;
          console.log('解析为7位格式，忽略秒和年');
        }
        
        console.log('解析结果:', { minute, hour, day, month, weekday });
        
        let description = '';
        
        // 解析时间
        const timeStr = formatTime(hour, minute);
        
        // 解析重复模式
        if (weekday !== '*' && weekday !== '?') {
          // 按星期重复
          const weekdayStr = parseWeekdays(weekday);
          if (month !== '*' && month !== '?') {
            // 特定月份的特定星期
            const monthStr = parseMonths(month);
            description = `${monthStr}的${weekdayStr} ${timeStr}`;
          } else {
            // 每周的特定星期
            description = `每${weekdayStr} ${timeStr}`;
          }
        } else if (day !== '*' && day !== '?') {
          // 按日期重复
          if (month !== '*' && month !== '?') {
            // 特定月份的特定日期（每年）
            const monthStr = parseMonths(month);
            description = `每年${monthStr}${day}日 ${timeStr}`;
          } else {
            // 每月的特定日期
            description = `每月${day}日 ${timeStr}`;
          }
        } else if (month !== '*' && month !== '?') {
          // 特定月份（每年）
          const monthStr = parseMonths(month);
          description = `每年${monthStr} ${timeStr}`;
        } else {
          // 每天
          description = `每天 ${timeStr}`;
        }
        
        console.log('生成的描述:', description);
        return description;
        
      } catch (e) {
        console.error('解析Cron表达式失败:', e);
        return '无效的Cron表达式';
      }
    };
    
    // 初始化倒计时
    const initCountdown = () => {
      if (countdownUpdater) {
        countdownUpdater.stop();
      }
      
      countdownUpdater = createCountdownUpdater((text) => {
        countdownText.value = text;
      }, props.reminder);
      
      countdownUpdater.start();
    };
    
    // 生命周期钩子
    onMounted(() => {
      initCountdown();
    });
    
    onUnmounted(() => {
      if (countdownUpdater) {
        countdownUpdater.stop();
      }
    });
    
    // 格式化时间
    const formatTime = (hour, minute) => {
      const h = hour === '*' ? '0' : hour;
      const m = minute === '*' ? '0' : minute;
      const hourNum = parseInt(h) || 0;  // 使用 || 0 处理NaN
      const minuteNum = parseInt(m) || 0; // 使用 || 0 处理NaN
      
      // 处理凌晨0点的情况
      if (hourNum === 0) {
        return `上午12:${String(minuteNum).padStart(2, '0')}`;
      } else if (hourNum < 12) {
        return `上午${hourNum}:${String(minuteNum).padStart(2, '0')}`;
      } else if (hourNum === 12) {
        return `中午${hourNum}:${String(minuteNum).padStart(2, '0')}`;
      } else {
        return `下午${hourNum - 12}:${String(minuteNum).padStart(2, '0')}`;
      }
    };
    
    // 解析星期
    const parseWeekdays = (weekday) => {
      const weekMap = {
        '0': '周日', '7': '周日',
        '1': '周一', '2': '周二', '3': '周三', 
        '4': '周四', '5': '周五', '6': '周六',
        'SUN': '周日', 'MON': '周一', 'TUE': '周二', 
        'WED': '周三', 'THU': '周四', 'FRI': '周五', 'SAT': '周六'
      };
      
      if (weekday.includes(',')) {
        return weekday.split(',').map(w => weekMap[w.trim()] || w).join(',');
      } else {
        return weekMap[weekday] || weekday;
      }
    };
    
    // 解析月份
    const parseMonths = (month) => {
      const monthMap = {
        '1': '1月', '2': '2月', '3': '3月', '4': '4月',
        '5': '5月', '6': '6月', '7': '7月', '8': '8月',
        '9': '9月', '10': '10月', '11': '11月', '12': '12月'
      };
      
      if (month.includes(',')) {
        return month.split(',').map(m => monthMap[m.trim()] || m).join(',');
      } else {
        return monthMap[month] || month;
      }
    };
    
    // 获取提醒方式文本
    const getReminderTypeText = (type) => {
      const typeMap = {
        'EMAIL': '邮件提醒',
        'SMS': '短信提醒'
      };
      return typeMap[type] || type;
    };
    
    // 格式化日期范围
    const formatDateRange = (validFrom, validUntil) => {
      if (validFrom && validUntil) {
        return `${validFrom} 至 ${validUntil}`;
      } else if (validFrom) {
        return `从 ${validFrom} 开始`;
      } else if (validUntil) {
        return `至 ${validUntil} 结束`;
      }
      return '';
    };
    
    // 判断文本是否过长需要跑马灯效果
    const isLongText = (text) => {
      return text && text.length > 12; // 超过12个字符就使用跑马灯
    };
    
    return {
      handleCardClick,
      handleEdit,
      handleDelete,
      formatCronDescription,
      getReminderTypeText,
      formatDateRange,
      isLongText,
      countdownText
    };
  }
};
</script>

<style scoped>
.reminder-card {
  background-color: #ffffff;
  border-radius: 24rpx;
  border: 2rpx solid #e9e0ce;
  padding: 32rpx;
  transition: all 0.2s ease;
  cursor: pointer;
}

.reminder-card:active {
  transform: scale(0.98);
  border-color: #f7bd4a;
  background-color: #fefefe;
}

.complex-card {
  border-left: 6rpx solid #f7bd4a;
}

.card-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24rpx;
}

.reminder-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.reminder-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
  line-height: 1.3;
  word-break: break-word;
}

/* 倒计时样式 */
.countdown-container {
  margin-top: 8rpx;
  padding: 8rpx 16rpx;
  background-color: #fff3e0;
  border-radius: 12rpx;
  border-left: 4rpx solid #ff9800;
}

.countdown-text {
  font-size: 24rpx;
  color: #e65100;
  font-weight: 500;
  line-height: 1.2;
}

.reminder-cron-container {
  width: 100%;
  overflow: hidden;
  position: relative;
  height: 48rpx;
  line-height: 48rpx;
}

.reminder-cron {
  font-size: 26rpx;
  color: #f7bd4a;
  font-weight: 500;
  display: inline-block;
  white-space: nowrap;
}

.reminder-cron.marquee {
  animation: marquee 8s linear infinite;
}

@keyframes marquee {
  0% {
    transform: translateX(100%);
  }
  100% {
    transform: translateX(-100%);
  }
}

.reminder-desc {
  font-size: 24rpx;
  color: #666666;
  line-height: 1.4;
  margin-top: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.reminder-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 12rpx;
}

.meta-item {
  font-size: 22rpx;
  color: #9d8148;
  background-color: #f4efe7;
  padding: 8rpx 16rpx;
  border-radius: 8rpx;
  line-height: 1;
}

.reminder-actions {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.action-icon-btn {
  width: 64rpx;
  height: 64rpx;
  background-color: #f4efe7;
  border: none;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  position: relative;
}

.action-icon-btn:active {
  background-color: #e9e0ce;
  transform: scale(0.95);
}

.action-icon-btn.delete-btn {
  background-color: #ffe6e6;
}

.action-icon-btn.delete-btn:active {
  background-color: #ffcccc;
  transform: scale(0.95);
}

.action-icon {
  font-size: 32rpx;
}

/* 响应式调整 */
@media (max-width: 750rpx) {
  .reminder-card {
    padding: 24rpx;
  }
  
  .card-content {
    flex-direction: column;
    align-items: stretch;
    gap: 20rpx;
  }
  
  .reminder-actions {
    align-self: flex-end;
  }
}
</style> 