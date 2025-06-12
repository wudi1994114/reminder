<template>
  <view class="login-container">
    <view class="form-group">
      <text class="label">用户名</text>
      <input 
        class="input" 
        type="text" 
        v-model="form.username" 
        placeholder="请输入用户名"
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
    
    <!-- 微信登录按钮 -->
    <button 
      class="wechat-login-btn" 
      :disabled="wechatLoading"
      :class="{'loading': wechatLoading}"
      @click="handleWechatLogin"
      v-if="isWeChatMiniProgram"
    >
      <text class="wechat-icon">🏮</text>
      <text class="wechat-text">{{ wechatLoading ? '微信登录中...' : '微信快捷登录' }}</text>
    </button>
    
    <view class="links">
      <text class="link" @click="onForgotPassword">忘记密码?</text>
      <text class="link" @click="onRegister">注册账号</text>
    </view>
    

  </view>
</template>

<script>
import { ref, reactive, computed } from 'vue';
import { login, isWeChatMiniProgram, smartWechatLogin } from '../services/api';
import { isValidEmail } from '../utils/helpers';
import { UserService, userState } from '../services/userService';

export default {
  emits: ['register', 'forgot-password', 'login-success'],
  
  setup(props, { emit }) {
    const form = reactive({
      username: '',
      password: ''
    });
    
    const loading = ref(false);
    const wechatLoading = ref(false);
    const errorMsg = ref('');
    
    const isValid = computed(() => {
      return form.username.length > 0 && form.password.length > 0;
    });
    
    const validateForm = () => {
      errorMsg.value = '';
    };
    
    const handleLogin = async () => {
      try {
        loading.value = true;
        errorMsg.value = '';
        
        // 添加调试日志
        console.log('📝 LoginForm - form数据:', JSON.stringify(form, null, 2));
        
        // 构建登录请求数据
        const loginData = {
          username: form.username,
          password: form.password
        };
        
        // 添加调试日志
        console.log('📦 LoginForm - 构建的loginData:', JSON.stringify(loginData, null, 2));
        
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
        
        // 保存Token和用户信息
        if (token) {
          // 构造登录响应对象
          const loginResponse = {
            accessToken: token,
            user: user
          };
          
          // 使用用户服务处理登录成功，标记为普通登录
          const userInfo = await UserService.onLoginSuccess(loginResponse, 'normal');
          
          console.log('✅ LoginForm: 普通登录处理完成，用户信息:', userInfo);
          
          // 通知登录成功
          emit('login-success', userInfo);
          
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

    // 检查是否为微信小程序环境
    const isWeChatEnv = isWeChatMiniProgram();

    // 微信登录处理
    const handleWechatLogin = async () => {
      try {
        wechatLoading.value = true;
        errorMsg.value = '';

        console.log('🚀 LoginForm: 开始微信登录...');
        console.log('🔍 LoginForm: 环境检查:', {
          isWeChatEnv: isWeChatEnv,
          hasSmartWechatLogin: typeof smartWechatLogin === 'function'
        });

        // 使用智能微信登录流程（自动判断是否需要获取用户信息）
        const response = await smartWechatLogin();

        console.log('✅ LoginForm: 微信登录完成，响应:', JSON.stringify(response, null, 2));

        if (response && response.accessToken) {
          // 使用用户服务处理登录成功，标记为微信登录
          const userInfo = await UserService.onLoginSuccess(response, 'wechat');
          
          console.log('✅ LoginForm: 微信登录处理完成，用户信息:', userInfo);

          // 如果是新用户且需要完善资料，标记状态
          if (response.isNewUser && response.needCompleteProfile) {
            console.log('🆕 新用户需要完善资料，标记状态');
            
            // 在本地存储中标记需要完善资料
            uni.setStorageSync('needCompleteProfile', {
              isNewUser: true,
              userInfo: {
                nickname: response.nickname || '',
                avatarUrl: response.avatarUrl || '',
                email: response.email || '',
                phone: response.phone || response.phoneNumber || ''
              }
            });
          } else if (response.isNewUser) {
            console.log('🎉 新用户信息已完整，无需额外完善');
          }

          // 显示登录成功提示
          uni.showToast({
            title: response.message || (response.needCompleteProfile ? '注册成功' : '登录成功'),
            icon: 'success',
            duration: 2000
          });

          // 通知登录成功
          emit('login-success', userInfo);

          // 跳转到首页
          setTimeout(() => {
            uni.switchTab({
              url: '/pages/index/index'
            });
          }, 2000);

        } else {
          throw new Error('微信登录响应格式错误');
        }

      } catch (error) {
        console.error('微信登录失败:', error);
        
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
      form,
      loading,
      wechatLoading,
      errorMsg,
      isValid,
      isWeChatMiniProgram: isWeChatEnv,
      validateForm,
      handleLogin,
      handleWechatLogin,
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

.wechat-login-btn {
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

.wechat-login-btn:disabled {
  background-color: #cccccc;
  color: #ffffff;
}

.wechat-login-btn.loading {
  opacity: 0.8;
}

.wechat-icon {
  margin-right: 10rpx;
}

.wechat-text {
  font-size: 32rpx;
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