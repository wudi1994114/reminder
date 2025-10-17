<template>
  <view v-if="visible" class="global-login-modal-overlay" @click="handleClose">
    <view class="global-login-modal" @click.stop>
      <view class="global-login-modal-header">
        <text class="global-login-modal-title">需要登录</text>
        <view class="global-login-modal-close" @click="handleClose">
          <text class="close-icon">×</text>
        </view>
      </view>
      <view class="global-login-modal-content">
        <text class="login-desc">请先登录以继续操作</text>
        <button 
          class="wechat-login-button" 
          open-type="getUserInfo"
          @getuserinfo="handleWechatLogin"
        >
          <text class="wechat-login-text">微信一键登录</text>
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import { computed } from 'vue';
import { globalAuthState, hideOneClickLogin } from '../utils/auth';
import { loginWithBackend } from '../api/wechat';
import { UserService } from '../services/userService';

export default {
  name: 'GlobalLoginModal',
  
  setup() {
    const visible = computed(() => globalAuthState.showLoginModal);
    
    const handleClose = () => {
      console.log('关闭全局登录弹窗');
      hideOneClickLogin(false);
    };
    
    const handleWechatLogin = async (e) => {
      console.log('全局登录弹窗：微信一键登录触发', e);
      
      try {
        // 显示加载提示
        uni.showLoading({
          title: '登录中...',
          mask: true
        });
        
        // 获取用户信息
        const userInfo = e.detail.userInfo;
        if (!userInfo) {
          uni.hideLoading();
          uni.showToast({
            title: '登录已取消',
            icon: 'none'
          });
          hideOneClickLogin(false);
          return;
        }
        
        console.log('全局登录：获取到用户信息:', userInfo);
        
        // 调用微信登录获取code
        const loginRes = await new Promise((resolve, reject) => {
          uni.login({
            provider: 'weixin',
            success: resolve,
            fail: reject
          });
        });
        
        console.log('全局登录：微信登录成功:', loginRes);
        
        // 构建登录请求数据
        const loginData = {
          code: loginRes.code
        };
        
        console.log('🔐 全局登录：发送登录数据到后端:', loginData);
        
        // 调用后端登录接口
        const response = await loginWithBackend(loginData);
        
        console.log('✅ 全局登录：后端登录API响应:', response);
        
        if (response && response.accessToken) {
          // 使用UserService处理登录成功
          await UserService.onLoginSuccess(response, 'wechat');
          
          console.log('✅ 全局登录：登录处理完成，用户状态已更新');
          
          uni.hideLoading();
          uni.showToast({
            title: '登录成功',
            icon: 'success'
          });
          
          // 关闭全局登录弹窗，并返回成功状态
          hideOneClickLogin(true);
          
        } else {
          throw new Error('登录响应格式错误');
        }
        
      } catch (error) {
        console.error('全局登录：微信登录失败:', error);
        uni.hideLoading();
        
        let errorMessage = '登录失败';
        if (error.message) {
          if (error.message.includes('用户拒绝')) {
            errorMessage = '用户取消了授权';
          } else if (error.message.includes('网络')) {
            errorMessage = '网络连接失败';
          } else {
            errorMessage = error.message;
          }
        }
        
        uni.showToast({
          title: errorMessage,
          icon: 'none'
        });
        
        // 关闭全局登录弹窗，返回失败状态
        hideOneClickLogin(false);
      }
    };
    
    return {
      visible,
      handleClose,
      handleWechatLogin
    };
  }
};
</script>

<style scoped>
.global-login-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(28, 23, 13, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999; /* 确保在最顶层 */
}

.global-login-modal {
  background-color: #fcfbf8;
  border-radius: 24rpx;
  width: 600rpx;
  max-width: 90vw;
  overflow: hidden;
  border: 2rpx solid #e9e0ce;
}

.global-login-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx;
  border-bottom: 2rpx solid #e9e0ce;
  background-color: #fcfbf8;
}

.global-login-modal-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #1c170d;
}

.global-login-modal-close {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 8rpx;
  transition: all 0.2s ease;
}

.global-login-modal-close:hover {
  background-color: #e9e0ce;
}

.close-icon {
  font-size: 40rpx;
  color: #9d8148;
  line-height: 1;
  font-weight: 600;
}

.global-login-modal-content {
  padding: 48rpx 32rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32rpx;
  background-color: #fcfbf8;
}

.login-desc {
  font-size: 28rpx;
  color: #9d8148;
  text-align: center;
  font-weight: 500;
}

.wechat-login-button {
  width: 100%;
  height: 88rpx;
  background-color: #f7bd4a;
  color: #1c170d;
  border-radius: 16rpx;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  transition: all 0.2s ease;
  box-shadow: 0 4rpx 12rpx rgba(247, 189, 74, 0.3);
}

.wechat-login-button:active {
  transform: translateY(2rpx);
  box-shadow: 0 2rpx 8rpx rgba(247, 189, 74, 0.4);
}

.wechat-login-text {
  font-size: 32rpx;
  color: #1c170d;
  font-weight: 600;
}
</style> 