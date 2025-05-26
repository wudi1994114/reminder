<template>
  <view v-if="show" class="modal-overlay" @click.self="handleCancel">
    <view class="modal-container">
      <view class="modal-header">
        <text class="modal-title">{{ title }}</text>
      </view>
      
      <view class="modal-content">
        <text class="modal-message">{{ message }}</text>
      </view>
      
      <view class="modal-actions">
        <button class="modal-btn cancel-btn" @click="handleCancel">
          {{ cancelText }}
        </button>
        <button class="modal-btn confirm-btn" @click="handleConfirm">
          {{ confirmText }}
        </button>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'ConfirmModal',
  props: {
    show: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: '提示'
    },
    message: {
      type: String,
      default: '确定要执行此操作吗？'
    },
    confirmText: {
      type: String,
      default: '确定'
    },
    cancelText: {
      type: String,
      default: '取消'
    }
  },
  
  emits: ['confirm', 'cancel'],
  
  methods: {
    handleConfirm() {
      this.$emit('confirm');
    },
    
    handleCancel() {
      this.$emit('cancel');
    }
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.modal-container {
  width: 600rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  overflow: hidden;
  margin: 0 40rpx;
}

.modal-header {
  padding: 32rpx 32rpx 16rpx;
  text-align: center;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333333;
}

.modal-content {
  padding: 16rpx 32rpx 32rpx;
  text-align: center;
}

.modal-message {
  font-size: 28rpx;
  color: #666666;
  line-height: 1.5;
}

.modal-actions {
  display: flex;
  border-top: 1rpx solid #e5e5e5;
}

.modal-btn {
  flex: 1;
  height: 88rpx;
  border: none;
  background-color: transparent;
  font-size: 32rpx;
  color: #333333;
}

.modal-btn:active {
  background-color: #f5f5f5;
}

.cancel-btn {
  border-right: 1rpx solid #e5e5e5;
  color: #666666;
}

.confirm-btn {
  color: #007aff;
  font-weight: 600;
}
</style>