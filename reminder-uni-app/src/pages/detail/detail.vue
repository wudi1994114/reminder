<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="header-section">
      <view class="nav-container">
        <button class="back-btn" @click="goBack">
          <text class="back-icon">×</text>
        </button>
        <view class="title-container">
          <text class="page-title">提醒详情</text>
        </view>
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
        <view v-else-if="!reminder.id" class="empty-state">
          <view class="empty-content">
            <text class="empty-icon">❌</text>
            <text class="empty-title">提醒不存在</text>
            <text class="empty-desc">该提醒可能已被删除或ID无效</text>
            <button class="empty-action-btn" @click="goBack">
              <text class="btn-text">返回</text>
            </button>
          </view>
        </view>
        
        <!-- 提醒详情 -->
        <view v-else class="detail-container">
          <!-- 标题 -->
          <view class="title-section">
            <text class="reminder-title">{{ reminder.title }}</text>
          </view>
          
          <!-- 备注标题和内容 -->
          <view v-if="reminder.description" class="notes-section">
            <text class="notes-title">备注</text>
            <text class="notes-content">{{ reminder.description }}</text>
          </view>
          
          <!-- 提醒设置 -->
          <view class="remind-section">
            <text class="section-title">提醒我</text>
            
            <view class="remind-details">
              <view class="setting-item">
                <text class="setting-label">方式</text>
                <text class="value-text">{{ getReminderTypeText(reminder.reminderType) }}</text>
              </view>
              
              <view class="setting-item">
                <text class="setting-label">时间</text>
                <text class="value-text">{{ formatDisplayTime(reminder.eventTime) }}</text>
              </view>
              
              <view class="setting-item" v-if="reminder.cronExpression">
                <text class="setting-label">重复</text>
                <text class="value-text">{{ cronExpressionToText(reminder.cronExpression) }}</text>
              </view>
              
              <view class="setting-item">
                <text class="setting-label">下次提醒时间</text>
                <text class="value-text">{{ formatDisplayTime(reminder.eventTime) }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>
    
    <!-- 底部按钮 -->
    <view class="bottom-actions">
      <button class="action-button edit-btn" @click="editReminder">
        <text class="button-text">编辑</text>
      </button>
      <button class="action-button cancel-btn" @click="goBack">
        <text class="button-text">取消</text>
      </button>
    </view>
  </view>
</template>

<script>
import { ref, onMounted, getCurrentInstance } from 'vue';
import { getSimpleReminderById } from '../../services/api';
import { formatDate } from '../../utils/helpers';
import cronstrue from 'cronstrue/i18n';

export default {
  setup() {
    const reminder = ref({});
    const loading = ref(true);
    const reminderId = ref('');
    
    // 在setup中直接获取页面参数
    const getCurrentPageOptions = () => {
      const pages = getCurrentPages();
      const currentPage = pages[pages.length - 1];
      return currentPage.options || {};
    };
    
    onMounted(async () => {
      // 直接获取页面参数
      const options = getCurrentPageOptions();
      const id = options.id || '';
      
      console.log('=== Detail页面调试信息 ===');
      console.log('页面参数options:', options);
      console.log('获取到的ID:', id);
      console.log('ID类型:', typeof id);
      console.log('ID是否为空:', !id);
      
      if (id) {
        console.log('开始加载提醒详情，ID:', id);
        reminderId.value = id;
        await loadReminderDetail(id);
      } else {
        console.error('无效的提醒ID，停止加载');
        loading.value = false;
        uni.showToast({ title: '无效的提醒ID', icon: 'none' });
      }
    });
    
    const loadReminderDetail = async (id) => {
      try {
        loading.value = true;
        console.log('=== API调用调试信息 ===');
        console.log('调用getSimpleReminderById，参数ID:', id);
        
        const result = await getSimpleReminderById(id);
        
        console.log('API调用结果:', result);
        console.log('结果类型:', typeof result);
        console.log('结果是否为空:', !result);
        
        if (result) {
          reminder.value = result;
          console.log('提醒详情设置成功:', reminder.value);
        } else {
          console.warn('API返回空结果');
        }
      } catch (error) {
        console.error('获取提醒详情失败:', error);
        console.error('错误详情:', JSON.stringify(error));
        uni.showToast({ title: '获取提醒详情失败', icon: 'none' });
      } finally {
        loading.value = false;
      }
    };
    
    const goBack = () => {
      uni.navigateBack();
    };
    
    const editReminder = () => {
      if (reminder.value.id) {
        uni.navigateTo({
          url: `/pages/create/create?id=${reminder.value.id}&mode=edit`
        });
      }
    };
    
    const formatDisplayTime = (timeString) => {
      if (!timeString) return '-';
      return formatDate(timeString);
    };

    const cronExpressionToText = (cronExpression) => {
      if (!cronExpression) return '一次性提醒';
      try {
        return cronstrue.toString(cronExpression, { locale: "zh_CN" });
      } catch (e) {
        return cronExpression; // 解析失败则直接显示表达式
      }
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
    
    const getReminderTypeText = (type) => {
      switch (type) {
        case 'EMAIL': return '通知';
        case 'SMS': return '短信';
        case 'WECHAT_MINI': return '微信';
        default: return '通知';
      }
    };
    
    return {
      reminder,
      loading,
      goBack,
      editReminder,
      formatDisplayTime,
      cronExpressionToText,
      getStatusClass,
      getStatusText,
      getReminderTypeText
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

/* 顶部导航区域 */
.header-section {
  padding: 32rpx;
  background-color: #fcfbf8;
  border-bottom: none;
}

.nav-container {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.back-btn {
  width: 72rpx;
  height: 72rpx;
  background-color: transparent;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.back-icon {
  font-size: 36rpx;
  color: #1c170d;
  font-weight: 300;
  line-height: 1;
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

/* 内容区域 */
.content-scroll {
  flex: 1;
  background-color: #fcfbf8;
  padding-bottom: 120rpx; /* 为底部按钮留出空间 */
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

.btn-text {
  font-size: 32rpx;
  font-weight: 600;
}

/* 详情容器 */
.detail-container {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

/* 标题区域 */
.title-section {
  padding: 8 8rpx;
}

.reminder-title {
  font-size: 44rpx;
  font-weight: 700;
  color: #1c170d;
  line-height: 1.3;
  word-break: break-word;
}

/* 备注区域 */
.notes-section {
  padding: 0 8rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.notes-title {
  margin-top: 24rpx;
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
}

.notes-content {
  font-size: 30rpx;
  color: #666666;
  line-height: 1.6;
  word-break: break-word;
}

/* 提醒设置区域 */
.remind-section {
  padding: 32rpx 8rpx;
  margin-top: 24rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
  margin-bottom: 16rpx;
}

.remind-details {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
}

.setting-label {
  font-size: 30rpx;
  color: #1c170d;
  font-weight: 500;
}

.value-text {
  font-size: 30rpx;
  color: #666666;
  font-weight: 400;
}





/* 响应式调整 */
@media (max-width: 750rpx) {
  .content-container {
    padding: 0 24rpx 24rpx;
  }
  
  .notes-section {
    padding: 0 8rpx;
  }
  
  .remind-section {
    padding: 24rpx 8rpx;
  }
  
  .setting-item {
    padding: 12rpx 0;
  }
  
  .setting-label, .value-text {
    font-size: 28rpx;
  }
  
  .section-title {
    font-size: 30rpx;
  }
  
  .notes-content {
    font-size: 28rpx;
  }
  
  .bottom-actions {
    padding: 20rpx 24rpx;
    padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
    gap: 20rpx;
  }
  
  .action-button {
    height: 80rpx;
  }
  
  .button-text {
    font-size: 30rpx;
  }
}

/* 底部按钮区域 */
.bottom-actions {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: #fcfbf8;
  padding: 24rpx 32rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  display: flex;
  gap: 24rpx;
  border-top: 1rpx solid #f0ede4;
}

.action-button {
  flex: 1;
  height: 88rpx;
  border-radius: 16rpx;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.cancel-btn {
  background-color: #f5f5f5;
  color: #666666;
}

.edit-btn {
  background-color: #f7bd4a;
  color: #1c170d;
}

.button-text {
  font-size: 32rpx;
  font-weight: 600;
}
</style> 