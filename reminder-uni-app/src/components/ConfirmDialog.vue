<template>
  <view v-if="show" class="modal-overlay" @click.stop="onOverlayClick">
    <view class="modal-content">
      <text class="modal-title">{{ title }}</text>
      <text class="modal-message">{{ message }}</text>
      <view class="modal-actions">
        <button @click="onConfirm" class="button primary confirm-btn">{{ confirmText }}</button>
        <button @click="onCancel" class="button cancel-btn">{{ cancelText }}</button>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  props: {
    show: {
      type: Boolean,
      required: true
    },
    title: {
      type: String,
      default: '确认'
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
    },
    // 是否允许点击遮罩层关闭
    overlayClosable: {
      type: Boolean,
      default: true
    }
  },
  emits: ['confirm', 'cancel'],
  methods: {
    onConfirm() {
      this.$emit('confirm');
    },
    onCancel() {
      this.$emit('cancel');
    },
    onOverlayClick() {
      if (this.overlayClosable) {
        this.onCancel();
      }
    }
  }
};
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999; /* uni-app中需要合适的z-index */
}

.modal-content {
  background-color: white;
  padding: 40rpx; /* 使用rpx单位 */
  border-radius: 16rpx;
  box-shadow: 0 6rpx 30rpx rgba(0, 0, 0, 0.2);
  width: 80%; /* 调整宽度 */
  max-width: 600rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.modal-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333333;
  margin-bottom: 24rpx;
  text-align: center;
}

.modal-message {
  font-size: 30rpx;
  color: #666666;
  line-height: 1.6;
  text-align: center;
  margin-bottom: 40rpx;
}

.modal-actions {
  display: flex;
  justify-content: space-around; /* 按钮间距 */
  width: 100%;
}

.button {
  flex: 1; /* 让按钮平分空间 */
  margin: 0 10rpx; /* 按钮间距 */
  padding: 20rpx 0;
  border-radius: 8rpx;
  font-size: 30rpx;
  font-weight: 500;
  text-align: center;
}

.primary {
  background-color: #3cc51f; /* uni-app 主题色 */
  color: white;
}

.cancel-btn {
  background-color: #f0f0f0;
  color: #666666;
}

/* 可以考虑通过js切换暗色模式的class，或者使用uni-app的暗黑模式适配 */
</style> 