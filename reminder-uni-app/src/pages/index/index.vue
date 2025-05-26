<template>
  <view class="container">
    <view class="header">
      <text class="title">我的提醒</text>
      <view class="header-buttons">
        <button class="add-btn" @click="navigateToCreate">新建提醒</button>
        <button class="complex-btn" @click="navigateToComplexCreate">复杂提醒</button>
      </view>
    </view>
    
    <view class="reminder-list">
      <view v-if="loading" class="loading-container">
        <text>加载中...</text>
      </view>
      <view v-else-if="reminders && reminders.length === 0" class="empty-tip">
        <text>暂无提醒，点击"新建提醒"添加</text>
      </view>
      <view v-else>
        <view v-for="(item, index) in reminders" :key="index" class="reminder-item" @click="goToDetail(item.id)">
          <view class="reminder-info">
            <text class="reminder-title">{{item.title}}</text>
            <text class="reminder-time">{{formatTime(item.eventTime)}}</text>
          </view>
          <view class="reminder-status" :class="[item.status === 'PENDING' ? 'pending' : 'completed']">
            <text>{{item.status === 'PENDING' ? '待提醒' : '已完成'}}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { ref, onMounted, computed } from 'vue';
import { formatDate } from '../../utils/helpers';
import { getAllSimpleReminders } from '../../services/api';
import { reminderState } from '../../services/store';

export default {
  onTabItemTap() {
    this.loadReminders();
  },
  
  setup() {
    const loading = ref(false);
    
    // 使用共享状态管理的提醒数据
    const reminders = computed(() => {
      return reminderState.simpleReminders || [];
    });
    
    // 加载提醒列表
    const loadReminders = async () => {
      try {
        loading.value = true;
        reminderState.loading = true;
        
        const currentDate = new Date();
        const year = currentDate.getFullYear();
        const month = currentDate.getMonth() + 1;
        
        const result = await getAllSimpleReminders(year, month);
        if (result) {
          reminderState.simpleReminders = result;
        } else {
          // 确保simpleReminders始终是数组
          reminderState.simpleReminders = [];
        }
      } catch (error) {
        console.error('获取提醒列表失败:', error);
        
        // 确保发生错误时也赋值为空数组
        reminderState.simpleReminders = [];
        
        uni.showToast({
          title: '获取提醒列表失败',
          icon: 'none',
          duration: 2000
        });
      } finally {
        loading.value = false;
        reminderState.loading = false;
      }
    };
    
    const navigateToCreate = () => {
      uni.navigateTo({
        url: '/pages/create/create'
      });
    };
    
    const navigateToComplexCreate = () => {
      uni.navigateTo({
        url: '/pages/create-complex/create-complex'
      });
    };
    
    const goToDetail = (id) => {
      uni.navigateTo({
        url: `/pages/detail/detail?id=${id}`
      });
    };
    
    const formatTime = (timeString) => {
      return formatDate(timeString);
    };
    
    // 页面加载时获取数据
    onMounted(() => {
      loadReminders();
    });
    
    return {
      reminders,
      loading,
      navigateToCreate,
      navigateToComplexCreate,
      goToDetail,
      formatTime,
      loadReminders
    };
  }
};
</script>

<style>
.container {
  padding: 20rpx;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
}

.header-buttons {
  display: flex;
  gap: 16rpx;
}

.title {
  font-size: 36rpx;
  font-weight: bold;
}

.add-btn {
  background-color: #3cc51f;
  color: #ffffff;
  font-size: 28rpx;
  padding: 10rpx 30rpx;
  border-radius: 8rpx;
  border: none;
}

.complex-btn {
  background-color: #007aff;
  color: #ffffff;
  font-size: 28rpx;
  padding: 10rpx 30rpx;
  border-radius: 8rpx;
  border: none;
}

.reminder-list {
  margin-top: 20rpx;
}

.loading-container {
  text-align: center;
  padding: 40rpx 0;
  color: #999999;
}

.empty-tip {
  text-align: center;
  margin-top: 100rpx;
  color: #999999;
}

.reminder-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx;
  margin-bottom: 20rpx;
  background-color: #ffffff;
  border-radius: 10rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}

.reminder-title {
  font-size: 32rpx;
  margin-bottom: 10rpx;
}

.reminder-time {
  font-size: 24rpx;
  color: #666666;
}

.reminder-status {
  padding: 6rpx 16rpx;
  border-radius: 6rpx;
  font-size: 24rpx;
}

.pending {
  background-color: #fef0f0;
  color: #f56c6c;
}

.completed {
  background-color: #f0f9eb;
  color: #67c23a;
}
</style>
