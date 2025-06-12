<template>
  <view v-if="visible" class="modal-overlay" @tap="onOverlayTap">
    <view class="modal-container" @tap.stop>
      <view class="modal-header">
        <view v-if="!required" class="close-button" @tap="closeModal">
          <text class="close-icon">×</text>
        </view>
      </view>
      
      <view class="modal-content">
        <UserInfoEditor
          :initialUserInfo="initialUserInfo"
          :required="required"
          @success="onSuccess"
          @cancel="onCancel"
        />
      </view>
    </view>
  </view>
</template>

<script>
import UserInfoEditor from './UserInfoEditor.vue';

export default {
  name: 'UserInfoModal',
  
  components: {
    UserInfoEditor
  },
  
  props: {
    // 是否显示弹窗
    visible: {
      type: Boolean,
      default: false
    },
    // 初始用户信息
    initialUserInfo: {
      type: Object,
      default: () => ({})
    },
    // 是否必须完成（新用户为true，编辑时为false）
    required: {
      type: Boolean,
      default: false
    },
    // 是否允许点击遮罩关闭
    maskClosable: {
      type: Boolean,
      default: true
    }
  },
  
  emits: ['close', 'success', 'cancel'],
  
  methods: {
    // 点击遮罩层
    onOverlayTap() {
      if (this.maskClosable && !this.required) {
        this.closeModal();
      }
    },
    
    // 关闭弹窗
    closeModal() {
      this.$emit('close');
    },
    
    // 保存成功
    onSuccess(data) {
      console.log('用户信息保存成功:', data);
      this.$emit('success', data);
      this.closeModal();
    },
    
    // 取消/跳过
    onCancel() {
      console.log('用户取消完善资料');
      this.$emit('cancel');
      if (!this.required) {
        this.closeModal();
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
  right: 0;
  bottom: 0;
  background-color: rgba(28, 23, 13, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(4rpx);
}

.modal-container {
  width: 90%;
  max-width: 600rpx;
  max-height: 85vh;
  background-color: #fcfbf8;
  border-radius: 32rpx;
  overflow: hidden;
  position: relative;
  box-shadow: 0 12rpx 48rpx rgba(28, 23, 13, 0.12);
  animation: modalSlideIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: translateY(60rpx) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  position: relative;
  padding: 32rpx 32rpx 24rpx;
  text-align: center;
  background-color: transparent;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1c170d;
  display: block;
  margin-bottom: 6rpx;
  line-height: 1.2;
}

.modal-subtitle {
  font-size: 24rpx;
  color: #9d8148;
  display: block;
  line-height: 1.4;
}

.close-button {
  position: absolute;
  top: 24rpx;
  right: 24rpx;
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: rgba(157, 129, 72, 0.1);
  transition: all 0.3s ease;
  backdrop-filter: blur(10rpx);
}

.close-button:active {
  background-color: rgba(247, 189, 74, 0.2);
  transform: scale(0.9);
}

.close-icon {
  font-size: 32rpx;
  color: #9d8148;
  line-height: 1;
  font-weight: 400;
}

.close-button:active .close-icon {
  color: #1c170d;
}

.modal-content {
  max-height: calc(85vh - 120rpx);
  overflow-y: auto;
  background-color: #fcfbf8;
}

/* 滚动条样式 */
.modal-content::-webkit-scrollbar {
  width: 4rpx;
}

.modal-content::-webkit-scrollbar-track {
  background: transparent;
}

.modal-content::-webkit-scrollbar-thumb {
  background: #e9e0ce;
  border-radius: 2rpx;
}

.modal-content::-webkit-scrollbar-thumb:hover {
  background: #9d8148;
}

/* 响应式调整 */
@media (max-width: 750rpx) {
  .modal-container {
    width: 95%;
    max-width: none;
    border-radius: 20rpx;
  }
  
  .modal-header {
    padding: 24rpx 24rpx 20rpx;
  }
  
  .modal-title {
    font-size: 32rpx;
  }
  
  .modal-subtitle {
    font-size: 24rpx;
  }
  
  .close-button {
    width: 48rpx;
    height: 48rpx;
    top: 20rpx;
    right: 20rpx;
  }
  
  .close-icon {
    font-size: 28rpx;
  }
}
</style> 