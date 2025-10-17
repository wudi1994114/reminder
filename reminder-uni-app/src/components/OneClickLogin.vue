<template>
  <view class="login-modal" v-show="visible" @click="handleMaskClick">
    <view class="modal-content" @click.stop>
      <view class="modal-header">
        <text class="modal-title">登录提醒应用</text>
        <view class="close-btn" @click="close">
          <text class="close-icon">×</text>
        </view>
      </view>
      
      <view class="modal-body">
        <view class="login-info">
          <text class="info-text">登录后可以创建和管理您的提醒事项</text>
        </view>
        
        <view class="login-buttons">
          <!-- 微信一键登录按钮 -->
          <button 
            class="wechat-login-btn" 
            :disabled="wechatLoading"
            :class="{ 'loading': wechatLoading }"
            @click="handleWechatLogin"
            v-if="isWeChatMiniProgram"
          >
            <text class="wechat-icon">💬</text>
            <text class="btn-text">{{ wechatLoading ? '登录中...' : '微信一键登录' }}</text>
          </button>
          
          <!-- 非微信环境提示 -->
          <view class="no-wechat-tip" v-else>
            <text class="tip-text">请在微信小程序中使用一键登录功能</text>
          </view>
        </view>
        
        <!-- 错误信息 -->
        <view class="error-msg" v-if="errorMsg">
          <text class="error-text">{{ errorMsg }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { ref, watch } from 'vue';
import { isWeChatMiniProgram, smartWechatLogin } from '../services/api';
import { UserService } from '../services/userService';

export default {
  name: 'OneClickLogin',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    maskClosable: {
      type: Boolean,
      default: true
    }
  },
  emits: ['close', 'login-success'],
  setup(props, { emit }) {
    const wechatLoading = ref(false);
    const errorMsg = ref('');
    
    // 监听 visible 属性变化
    watch(() => props.visible, (newVal) => {
      console.log('👁️ OneClickLogin: visible 属性变化:', newVal);
      if (newVal) {
        console.log('🎯 OneClickLogin: 模态框应该显示');
      } else {
        console.log('🔒 OneClickLogin: 模态框应该隐藏');
      }
    });
    
    // 检查是否为微信小程序环境
    const isWeChatEnv = isWeChatMiniProgram();
    
    // 关闭模态框
    const close = () => {
      console.log('❌ OneClickLogin: 触发关闭事件');
      emit('close');
    };
    
    // 点击遮罩层处理
    const handleMaskClick = () => {
      console.log('🔍 OneClickLogin: 点击遮罩层，maskClosable:', props.maskClosable);
      if (props.maskClosable) {
        close();
      }
    };
    
    // 微信一键登录
    const handleWechatLogin = async () => {
      try {
        wechatLoading.value = true;
        errorMsg.value = '';
        
        console.log('🚀 OneClickLogin: 开始微信一键登录...');
        
        // 使用智能微信登录流程
        const response = await smartWechatLogin();
        
        console.log('✅ OneClickLogin: 微信登录完成，响应:', response);
        
        if (response && response.accessToken) {
          // 使用用户服务处理登录成功
          const userInfo = await UserService.onLoginSuccess(response, 'wechat');
          
          console.log('✅ OneClickLogin: 微信登录处理完成，用户信息:', userInfo);
          
          // 显示登录成功提示
          uni.showToast({
            title: response.message || '登录成功',
            icon: 'success',
            duration: 2000
          });
          
          // 通知登录成功
          emit('login-success', userInfo);
          
          // 关闭模态框
          close();
          
        } else {
          throw new Error('微信登录响应格式错误');
        }
        
      } catch (error) {
        console.error('微信一键登录失败:', error);
        
        let errorMessage = '微信登录失败';
        if (error.message) {
          if (error.message.includes('用户拒绝')) {
            errorMessage = '用户取消了微信授权';
          } else if (error.message.includes('网络')) {
            errorMessage = '网络连接失败，请检查网络';
          } else {
            errorMessage = error.message;
          }
        }
        
        errorMsg.value = errorMessage;
        
        uni.showToast({
          title: errorMessage,
          icon: 'none',
          duration: 3000
        });
      } finally {
        wechatLoading.value = false;
      }
    };
    
    return {
      wechatLoading,
      errorMsg,
      isWeChatMiniProgram: isWeChatEnv,
      close,
      handleMaskClick,
      handleWechatLogin
    };
  }
};
</script>

<style scoped>
.login-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 40rpx;
}

.modal-content {
  background-color: #ffffff;
  border-radius: 24rpx;
  width: 100%;
  max-width: 640rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.2);
  animation: modalFadeIn 0.3s ease-out;
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(-20rpx);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 40rpx 40rpx 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.modal-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #1c170d;
}

.close-btn {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: #f5f5f5;
  transition: background-color 0.2s;
}

.close-btn:active {
  background-color: #e8e8e8;
}

.close-icon {
  font-size: 32rpx;
  color: #666666;
}

.modal-body {
  padding: 40rpx;
}

.login-info {
  text-align: center;
  margin-bottom: 40rpx;
}

.info-text {
  font-size: 28rpx;
  color: #666666;
  line-height: 1.5;
}

.login-buttons {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.wechat-login-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  height: 96rpx;
  background-color: #07c160;
  border-radius: 16rpx;
  border: none;
  color: #ffffff;
  font-size: 32rpx;
  font-weight: 600;
  transition: background-color 0.2s;
}

.wechat-login-btn:active {
  background-color: #06ad56;
}

.wechat-login-btn:disabled,
.wechat-login-btn.loading {
  background-color: #cccccc;
  color: #ffffff;
}

.wechat-icon {
  font-size: 36rpx;
}

.btn-text {
  font-size: 32rpx;
  font-weight: 600;
}

.no-wechat-tip {
  text-align: center;
  padding: 40rpx 20rpx;
  background-color: #f9f9f9;
  border-radius: 16rpx;
}

.tip-text {
  font-size: 28rpx;
  color: #999999;
  line-height: 1.5;
}

.error-msg {
  margin-top: 24rpx;
  text-align: center;
  padding: 20rpx;
  background-color: #fff2f0;
  border-radius: 12rpx;
  border: 1rpx solid #ffccc7;
}

.error-text {
  font-size: 26rpx;
  color: #ff4d4f;
  line-height: 1.4;
}

/* 响应式适配 */
@media (max-width: 750rpx) {
  .login-modal {
    padding: 32rpx;
  }
  
  .modal-header {
    padding: 32rpx 32rpx 16rpx;
  }
  
  .modal-title {
    font-size: 32rpx;
  }
  
  .modal-body {
    padding: 32rpx;
  }
  
  .wechat-login-btn {
    height: 88rpx;
    font-size: 30rpx;
  }
  
  .btn-text {
    font-size: 30rpx;
  }
}
</style> 