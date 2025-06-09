<template>
  <view 
    class="reminder-card complex-card" 
    @click="handleClick"
  >
    <view class="card-content">
      <view class="reminder-main">
        <text class="reminder-title">{{ reminder.title }}</text>
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
    const handleClick = () => {
      emit('click', props.reminder.id);
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
        console.log('格式化时间结果:', timeStr);
        
        // 解析重复模式 - 修复逻辑顺序
        if (month !== '*' && month !== '?' && month.trim() !== '') {
          // 按年重复 - 优先检查年重复
          const months = parseMonths(month);
          
          if (weekday !== '?' && weekday !== '*' && weekday.trim() !== '') {
            // 年重复 + 星期模式
            const weekdays = parseWeekdays(weekday);
            description = `每年${months}的${weekdays}${timeStr}`;
          } else if (day !== '?' && day !== '*' && day.trim() !== '') {
            // 年重复 + 日期模式
            description = `每年${months}${day}日${timeStr}`;
          } else {
            // 年重复但日期和星期都为通配符
            description = `每年${months}${timeStr}`;
          }
          console.log('识别为年重复:', description);
        } else if (weekday !== '?' && weekday !== '*' && weekday.trim() !== '') {
          // 按周重复
          const weekdays = parseWeekdays(weekday);
          description = `每${weekdays}${timeStr}`;
          console.log('识别为周重复:', description);
        } else if (day !== '?' && day !== '*' && day.trim() !== '') {
          // 按月重复
          if (day.includes(',')) {
            const days = day.split(',').join('日、');
            description = `每月${days}日${timeStr}`;
          } else {
            description = `每月${day}日${timeStr}`;
          }
          console.log('识别为月重复:', description);
        } else {
          // 每天重复
          description = `每天${timeStr}`;
          console.log('识别为每天重复:', description);
        }
        
        console.log('最终描述:', description);
        return description;
      } catch (error) {
        console.error('解析Cron表达式失败:', error);
        return '无效的Cron表达式';
      }
    };
    
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
      handleClick,
      handleEdit,
      handleDelete,
      formatCronDescription,
      getReminderTypeText,
      formatDateRange,
      isLongText
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