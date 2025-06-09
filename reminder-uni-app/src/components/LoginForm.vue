<template>
  <view class="login-container">
    <view class="form-group">
      <text class="label">用户名/邮箱</text>
      <input 
        class="input" 
        type="text" 
        v-model="form.username" 
        placeholder="请输入用户名或邮箱"
        @input="validateForm"
      />
    </view>
    
    <view class="form-group">
      <text class="label">密码</text>
      <input 
        class="input" 
        type="password" 
        password="true"
        v-model="form.password" 
        placeholder="请输入密码"
        @input="validateForm"
      />
    </view>
    
    <view class="error-msg" v-if="errorMsg">
      <text>{{ errorMsg }}</text>
    </view>
    
    <button 
      class="login-btn" 
      :disabled="!isValid || loading" 
      :class="{'loading': loading}"
      @click="handleLogin"
    >
      {{ loading ? '登录中...' : '登录' }}
    </button>
    
    <view class="links">
      <text class="link" @click="onForgotPassword">忘记密码?</text>
      <text class="link" @click="onRegister">注册账号</text>
    </view>
  </view>
</template>

<script>
import { ref, reactive, computed } from 'vue';
import { login } from '../services/api';
import { isValidEmail } from '../utils/helpers';
import { userState, saveUserInfo } from '../services/store';

export default {
  emits: ['register', 'forgot-password', 'login-success'],
  
  setup(props, { emit }) {
    const form = reactive({
      username: '',
      password: ''
    });
    
    const loading = ref(false);
    const errorMsg = ref('');
    
    const isValid = computed(() => {
      return form.username.length > 0 && form.password.length >= 8;
    });
    
    const validateForm = () => {
      errorMsg.value = '';
    };
    
    const handleLogin = async () => {
      try {
        loading.value = true;
        errorMsg.value = '';
        
        // 构建登录请求数据
        const loginData = {
          username: form.username,
          password: form.password
        };
        
        // 调用登录API
        const response = await login(loginData);
        
        let token = null;
        let user = null;

        if (typeof response === 'string' && response.length > 0) {
          token = response;
          // 用户信息可能需要单独获取，或store能处理user为null的情况
        } else if (response && typeof response === 'object') {
          token = response.token || response.accessToken || response.jwt || response.id_token;
          user = response.user || response.userDetails || response.principal;
        }
        
        // 保存Token
        if (token) {
          uni.setStorageSync('accessToken', `Bearer ${token}`);
          
          // 保存用户信息
          // saveUserInfo 应该能处理 user 为 null 或 undefined 的情况
          saveUserInfo(user); 
          
          // 通知登录成功
          emit('login-success', user); // 传递获取到的用户信息
          
          // 跳转到首页
          uni.switchTab({
            url: '/pages/index/index'
          });
        } else {
          // 2xx 响应，但 token 未能成功提取，或者后端在2xx响应中返回了业务错误
          if (response && response.message) {
            errorMsg.value = response.message;
          } else if (response && response.error && typeof response.error === 'string') { // e.g. { error: "some_error_code", error_description: "details" }
            errorMsg.value = response.error_description || response.error;
          } else if (response && response.error && response.error.message) {
             errorMsg.value = response.error.message;
          } else {
            errorMsg.value = '登录失败，响应数据格式不正确';
          }
        }
      } catch (error) {
        console.error('登录失败:', error);
        if (error && error.data && error.data.message) {
          errorMsg.value = error.data.message;
        } else if (error && error.statusCode) {
          errorMsg.value = `登录失败，状态码: ${error.statusCode}`;
        } else {
          errorMsg.value = '登录失败，请稍后重试';
        }
      } finally {
        loading.value = false;
      }
    };
    
    const onRegister = () => {
      emit('register');
    };
    
    const onForgotPassword = () => {
      emit('forgot-password');
    };
    
    return {
      form,
      loading,
      errorMsg,
      isValid,
      validateForm,
      handleLogin,
      onRegister,
      onForgotPassword
    };
  }
};
</script>

<style>
.login-container {
  padding: 20rpx;
}

.form-group {
  margin-bottom: 30rpx;
}

.label {
  display: block;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 10rpx;
}

.input {
  width: 100%;
  height: 80rpx;
  background-color: #f5f5f5;
  border-radius: 8rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
}

.error-msg {
  color: #ff4d4f;
  font-size: 24rpx;
  margin-bottom: 20rpx;
}

.login-btn {
  width: 100%;
  height: 90rpx;
  line-height: 90rpx;
  text-align: center;
  background-color: #3cc51f;
  color: #fff;
  font-size: 32rpx;
  border-radius: 8rpx;
  margin-bottom: 30rpx;
}

.login-btn:disabled {
  background-color: #cccccc;
  color: #ffffff;
}

.login-btn.loading {
  opacity: 0.8;
}

.links {
  display: flex;
  justify-content: space-between;
  font-size: 28rpx;
}

.link {
  color: #3cc51f;
}
</style> 