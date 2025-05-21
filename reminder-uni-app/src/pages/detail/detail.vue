<template>
  <view class="container">
    <view v-if="loading" class="loading-container">
      <text>加载中...</text>
    </view>
    <view v-else-if="!reminder.id" class="empty-tip">
      <text>提醒信息不存在</text>
    </view>
    <view v-else class="detail-card">
      <view class="detail-header">
        <text class="detail-title">{{reminder.title}}</text>
        <view class="detail-status" :class="[reminder.status === 'PENDING' ? 'pending' : 'completed']">
          <text>{{reminder.status === 'PENDING' ? '待提醒' : '已完成'}}</text>
        </view>
      </view>
      
      <view class="detail-info">
        <view class="info-item">
          <text class="label">提醒时间：</text>
          <text class="value">{{formatDisplayTime(reminder.eventTime)}}</text>
        </view>
        <view class="info-item" v-if="reminder.cronExpression">
          <text class="label">提醒频率：</text>
          <text class="value">{{ cronExpressionToText(reminder.cronExpression) }}</text>
        </view>
        <view class="info-item">
          <text class="label">创建时间：</text>
          <text class="value">{{formatDisplayTime(reminder.createTime)}}</text>
        </view>
        <view class="info-item desc" v-if="reminder.description">
          <text class="label">提醒内容：</text>
          <text class="value">{{reminder.description}}</text>
        </view>
      </view>
      
      <view class="detail-actions">
        <button class="action-btn edit" @click="editReminder">编辑</button>
        <button class="action-btn delete" @click="showDeleteConfirm">删除</button>
        <button 
          class="action-btn complete" 
          v-if="reminder.status === 'PENDING'" 
          @click="completeReminder"
          :disabled="actionLoading"
        >
          {{ actionLoading && currentAction === 'complete' ? '处理中...' : '完成' }}
        </button>
      </view>
    </view>

    <ConfirmDialog
      :show="showConfirmDialog"
      title="确认删除"
      message="确定要删除这条提醒吗？"
      @confirm="handleDeleteConfirm"
      @cancel="handleDeleteCancel"
    />
  </view>
</template>

<script>
import { ref, onMounted } from 'vue';
import { getSimpleReminderById, deleteEvent, updateEvent } from '../../services/api';
import { formatDate } from '../../utils/helpers';
import ConfirmDialog from '../../components/ConfirmDialog.vue';
import cronstrue from 'cronstrue/i18n';

export default {
  components: {
    ConfirmDialog
  },
  setup() {
    const reminder = ref({});
    const loading = ref(true);
    const actionLoading = ref(false); // 用于标记完成/删除操作的加载状态
    const currentAction = ref(''); // 当前执行的操作类型
    const showConfirmDialog = ref(false);
    let reminderId = null;
    
    onMounted(async () => {
      const pages = getCurrentPages();
      const page = pages[pages.length - 1];
      reminderId = page.$page?.options?.id || '';
      
      if (reminderId) {
        await loadReminderDetail(reminderId);
      } else {
        loading.value = false;
        uni.showToast({ title: '无效的提醒ID', icon: 'none' });
      }
    });
    
    const loadReminderDetail = async (id) => {
      try {
        loading.value = true;
        const result = await getSimpleReminderById(id);
        if (result) {
          reminder.value = result;
        }
      } catch (error) {
        console.error('获取提醒详情失败:', error);
        uni.showToast({ title: '获取提醒详情失败', icon: 'none' });
      } finally {
        loading.value = false;
      }
    };
    
    const editReminder = () => {
      uni.navigateTo({
        url: `/pages/create/create?id=${reminder.value.id}&mode=edit`
      });
    };
    
    const showDeleteConfirm = () => {
      showConfirmDialog.value = true;
    };

    const handleDeleteConfirm = async () => {
      showConfirmDialog.value = false;
      if (actionLoading.value) return;
      try {
        actionLoading.value = true;
        currentAction.value = 'delete';
        await deleteEvent(reminder.value.id);
        uni.showToast({
          title: '删除成功',
          icon: 'success',
          duration: 1500
        });
        setTimeout(() => {
          uni.navigateBack();
        }, 1500);
      } catch (error) {
        console.error('删除提醒失败:', error);
        uni.showToast({ title: '删除失败', icon: 'none' });
      } finally {
        actionLoading.value = false;
        currentAction.value = '';
      }
    };

    const handleDeleteCancel = () => {
      showConfirmDialog.value = false;
    };
    
    const completeReminder = async () => {
      if (actionLoading.value) return;
      try {
        actionLoading.value = true;
        currentAction.value = 'complete';
        const updatedReminder = { ...reminder.value, status: 'COMPLETED' };
        const result = await updateEvent(reminder.value.id, updatedReminder);
        if (result) {
          reminder.value = result;
          uni.showToast({
            title: '已标记为完成',
            icon: 'success',
            duration: 1500
          });
        }
      } catch (error) {
        console.error('标记完成失败:', error);
        uni.showToast({ title: '操作失败', icon: 'none' });
      } finally {
        actionLoading.value = false;
        currentAction.value = '';
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
    
    return {
      reminder,
      loading,
      actionLoading,
      currentAction,
      showConfirmDialog,
      editReminder,
      showDeleteConfirm,
      handleDeleteConfirm,
      handleDeleteCancel,
      completeReminder,
      formatDisplayTime,
      cronExpressionToText
    };
  }
};
</script>

<style>
.container {
  padding: 30rpx;
}

.loading-container, .empty-tip {
  text-align: center;
  padding: 60rpx 0;
  color: #999999;
  font-size: 30rpx;
}

.detail-card {
  background-color: #ffffff;
  border-radius: 10rpx;
  padding: 30rpx;
  box-shadow: 0 2rpx 20rpx rgba(0, 0, 0, 0.1);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
  padding-bottom: 20rpx;
  border-bottom: 1px solid #f0f0f0;
}

.detail-title {
  font-size: 36rpx;
  font-weight: bold;
}

.detail-status {
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

.detail-info {
  margin-bottom: 30rpx;
}

.info-item {
  display: flex;
  margin-bottom: 20rpx;
}

.info-item.desc {
  flex-direction: column;
}

.label {
  font-size: 28rpx;
  color: #666666;
  min-width: 160rpx;
}

.value {
  font-size: 28rpx;
  color: #333333;
}

.detail-actions {
  display: flex;
  justify-content: space-around;
  margin-top: 40rpx;
}

.action-btn {
  width: 200rpx;
  font-size: 28rpx;
  padding: 12rpx 0;
  text-align: center;
  border-radius: 8rpx;
}

.edit {
  background-color: #e6a23c;
  color: #ffffff;
}

.delete {
  background-color: #f56c6c;
  color: #ffffff;
}

.complete {
  background-color: #67c23a;
  color: #ffffff;
}

.complete:disabled {
  background-color: #a0cfff;
}
</style> 