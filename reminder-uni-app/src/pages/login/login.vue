<template>
  <view class="login-page">
    <view class="logo-container">
      <image class="logo" src="/static/images/logo.png"></image>
    </view>
    
    <view class="content">
      <LoginForm 
        @register="goToRegister"
        @forgot-password="showForgotPasswordTip"
      />
    </view>
  </view>
</template>

<script>
import { ref } from 'vue';
import LoginForm from '../../components/LoginForm.vue';

export default {
  components: {
    LoginForm
  },
  
  setup() {
    const goToRegister = () => {
      uni.navigateTo({
        url: '/pages/register/register'
      });
    };
    
    const showForgotPasswordTip = () => {
      uni.showModal({
        title: '忘记密码',
        content: '请联系管理员重置密码',
        showCancel: false,
        confirmText: '我知道了'
      });
    };
    
    return {
      goToRegister,
      showForgotPasswordTip,
    };
  }
};
</script>

<style>
.login-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #E6EFEC;
}

.logo-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0 60rpx;
}

.logo {
  width: 400rpx;
  height: 120rpx;
  margin-bottom: 30rpx;
}

.app-name {
  font-size: 48rpx;
  font-weight: bold;
  color: #333333;
}

.content {
  flex: 1;
  padding: 0 40rpx;
}
</style> 