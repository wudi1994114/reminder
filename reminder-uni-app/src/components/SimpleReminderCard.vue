<template>
  <view 
    class="reminder-card" 
    @click="handleClick"
  >
    <view class="card-content">
      <view class="reminder-main">
        <text class="reminder-title">{{ reminder.title }}</text>
        <text class="reminder-time">{{ formatTimeHelper(reminder.eventTime) }}</text>
        <text v-if="reminder.description" class="reminder-desc">{{ reminder.description }}</text>
      </view>
      <view class="reminder-status" :class="getStatusClass(reminder.status)">
        <view class="status-dot"></view>
        <text class="status-text">{{ getStatusText(reminder.status) }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { formatReminder } from '../utils/dateFormat';

export default {
  name: 'SimpleReminderCard',
  props: {
    reminder: {
      type: Object,
      required: true
    }
  },
  emits: ['click'],
  setup(props, { emit }) {
    const handleClick = () => {
      emit('click', props.reminder.id);
    };
    
    const getStatusClass = (status) => {
      if (status === 'PENDING') {
        return 'pending';
      } else if (status === 'COMPLETED') {
        return 'completed';
      }
      return '';
    };
    
    const getStatusText = (status) => {
      if (status === 'PENDING') {
        return '待提醒';
      } else if (status === 'COMPLETED') {
        return '已完成';
      }
      return '';
    };
    
    const formatTimeHelper = (timeString) => {
      return formatReminder(timeString);
    };
    
    return {
      handleClick,
      getStatusClass,
      getStatusText,
      formatTimeHelper
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

.reminder-time {
  font-size: 26rpx;
  color: #9d8148;
  font-weight: 400;
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

.reminder-status {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 24rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
}

.reminder-status.pending {
  background-color: #fff3e0;
  border: 1rpx solid #ffcc80;
}

.reminder-status.pending .status-dot {
  background-color: #ff9800;
}

.reminder-status.pending .status-text {
  color: #e65100;
}

.reminder-status.completed {
  background-color: #e8f5e8;
  border: 1rpx solid #a5d6a7;
}

.reminder-status.completed .status-dot {
  background-color: #4caf50;
}

.reminder-status.completed .status-text {
  color: #2e7d32;
}

.status-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-text {
  font-size: 24rpx;
  font-weight: 500;
  white-space: nowrap;
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
  
  .reminder-status {
    align-self: flex-start;
  }
}
</style> 