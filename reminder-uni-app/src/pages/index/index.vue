<template>
  <view class="page-container">
    <!-- 顶部标题区域 -->
    <view class="header-section">
      <view class="title-container">
        <text class="page-title">我的提醒</text>
      </view>
      <view class="action-buttons">
        <button class="action-btn primary-btn" @click="navigateToCreate">
          <text class="btn-text">新建提醒</text>
        </button>
        <button class="action-btn secondary-btn" @click="navigateToComplexCreate">
          <text class="btn-text">复杂提醒</text>
        </button>
      </view>
    </view>
    
    <!-- 内容区域 -->
    <scroll-view class="content-scroll" scroll-y>
      <view class="content-container">
        <!-- 加载状态 -->
        <view v-if="loading" class="loading-state">
          <view class="loading-content">
            <text class="loading-text">加载中...</text>
          </view>
        </view>
        
        <!-- 空状态 -->
        <view v-else-if="reminders && reminders.length === 0" class="empty-state">
          <view class="empty-content">
            <text class="empty-icon">📝</text>
            <text class="empty-title">暂无提醒</text>
            <text class="empty-desc">点击"新建提醒"开始添加你的提醒</text>
            <button class="empty-action-btn" @click="navigateToCreate">
              <text class="btn-text">立即创建</text>
            </button>
          </view>
        </view>
        
        <!-- 提醒列表 -->
        <view v-else class="reminder-list">
          <view 
            v-for="(item, index) in reminders" 
            :key="index" 
            class="reminder-card" 
            @click="goToDetail(item.id)"
          >
            <view class="card-content">
              <view class="reminder-main">
                <text class="reminder-title">{{ item.title }}</text>
                <text class="reminder-time">{{ formatTime(item.eventTime) }}</text>
              </view>
              <view class="reminder-status" :class="getStatusClass(item.status)">
                <view class="status-dot"></view>
                <text class="status-text">{{ getStatusText(item.status) }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { ref, onMounted, computed } from 'vue';
import { formatDate } from '../../utils/helpers';
import { getUpcomingReminders } from '../../services/api';
import { reminderState } from '../../services/store';

export default {
  onTabItemTap() {
    this.loadReminders();
  },
  
  setup() {
    const loading = ref(false);
    
    // 使用共享状态管理的即将到来的提醒数据
    const reminders = computed(() => {
      return reminderState.upcomingReminders || [];
    });
    
    // 加载即将到来的提醒列表
    const loadReminders = async () => {
      try {
        loading.value = true;
        reminderState.loading = true;
        
        const result = await getUpcomingReminders();
        console.log('=== Index页面提醒数据调试 ===');
        console.log('getUpcomingReminders结果:', result);
        console.log('结果类型:', typeof result);
        console.log('是否为数组:', Array.isArray(result));
        if (Array.isArray(result) && result.length > 0) {
          console.log('第一个提醒项:', result[0]);
          console.log('第一个提醒项的ID:', result[0].id);
        }
        
        if (result) {
          reminderState.upcomingReminders = result;
        } else {
          // 确保upcomingReminders始终是数组
          reminderState.upcomingReminders = [];
        }
      } catch (error) {
        console.error('获取即将到来的提醒列表失败:', error);
        
        // 确保发生错误时也赋值为空数组
        reminderState.upcomingReminders = [];
        
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
      console.log('=== Index页面跳转调试信息 ===');
      console.log('点击的提醒ID:', id);
      console.log('ID类型:', typeof id);
      console.log('跳转URL:', `/pages/detail/detail?id=${id}`);
      
      uni.navigateTo({
        url: `/pages/detail/detail?id=${id}`
      });
    };
    
    const formatTime = (timeString) => {
      return formatDate(timeString);
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
      loadReminders,
      getStatusClass,
      getStatusText
    };
  }
};
</script>

<style scoped>
.page-container {
  height: 100vh;
  background-color: #fcfbf8;
  display: flex;
  flex-direction: column;
  font-family: 'PingFang SC', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 顶部标题区域 */
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx;
  background-color: #fcfbf8;
  border-bottom: none;
}

.title-container {
  flex: 1;
}

.page-title {
  font-size: 48rpx;
  font-weight: 700;
  color: #1c170d;
  line-height: 1.2;
}

.reminder-count {
  font-size: 26rpx;
  color: #9d8148;
  font-weight: 400;
  margin-top: 8rpx;
}

.action-buttons {
  display: flex;
  gap: 24rpx;
}

.action-btn {
  height: 72rpx;
  padding: 0 32rpx;
  border-radius: 16rpx;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  transition: all 0.2s ease;
}

.primary-btn {
  background-color: #f7bd4a;
  color: #1c170d;
}

.secondary-btn {
  background-color: #ffffff;
  color: #1c170d;
  border: 2rpx solid #e9e0ce;
}

.btn-text {
  font-size: 28rpx;
  font-weight: 600;
}

/* 内容区域 */
.content-scroll {
  flex: 1;
  background-color: #fcfbf8;
}

.content-container {
  padding: 0 32rpx 32rpx;
  max-width: 960rpx;
  margin: 0 auto;
}

/* 加载状态 */
.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 120rpx 0;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24rpx;
}

.loading-text {
  font-size: 28rpx;
  color: #9d8148;
  font-weight: 500;
}

/* 空状态 */
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 120rpx 32rpx;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  max-width: 480rpx;
}

.empty-icon {
  font-size: 96rpx;
  margin-bottom: 32rpx;
  opacity: 0.6;
}

.empty-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #1c170d;
  margin-bottom: 16rpx;
}

.empty-desc {
  font-size: 28rpx;
  color: #9d8148;
  line-height: 1.4;
  margin-bottom: 48rpx;
}

.empty-action-btn {
  height: 88rpx;
  padding: 0 48rpx;
  background-color: #f7bd4a;
  color: #1c170d;
  border-radius: 16rpx;
  border: none;
  font-size: 32rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 提醒列表 */
.reminder-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

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

/* 状态标签 */
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
  .header-section {
    flex-direction: column;
    gap: 24rpx;
    align-items: stretch;
  }
  
  .title-container {
    text-align: center;
  }
  
  .action-buttons {
    justify-content: center;
  }
  
  .content-container {
    padding: 0 24rpx 24rpx;
  }
  
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
