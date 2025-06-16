<template>
  <view class="register-page">
    <view class="header">
      <text class="title">创建新账号</text>
      <text class="subtitle">填写以下信息完成注册</text>
    </view>
    
    <view class="form-container">
      <view class="form-group">
        <text class="label">用户名</text>
        <input 
          class="input" 
          type="text" 
          v-model="form.username" 
          placeholder="请输入用户名（只能包含英文字母和数字）"
          @input="handleUsernameInput"
        />
        <text class="error-msg" v-if="errors.username">{{ errors.username }}</text>
      </view>
      
      <view class="form-group">
        <text class="label">昵称</text>
        <input 
          class="input" 
          type="text" 
          v-model="form.nickname" 
          placeholder="请输入昵称"
          @input="validateForm"
        />
        <text class="error-msg" v-if="errors.nickname">{{ errors.nickname }}</text>
      </view>
      
      <view class="form-group">
        <text class="label">邮箱</text>
        <input 
          class="input" 
          type="text" 
          v-model="form.email" 
          placeholder="请输入邮箱"
          @input="validateForm"
        />
        <text class="error-msg" v-if="errors.email">{{ errors.email }}</text>
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
        <text class="error-msg" v-if="errors.password">{{ errors.password }}</text>
      </view>
      
      <view class="form-group">
        <text class="label">确认密码</text>
        <input 
          class="input" 
          type="password" 
          password="true"
          v-model="form.confirmPassword" 
          placeholder="请再次输入密码"
          @input="validateForm"
        />
        <text class="error-msg" v-if="errors.confirmPassword">{{ errors.confirmPassword }}</text>
      </view>
      
      <button 
        class="register-btn" 
        :disabled="!isValid || loading" 
        :class="{'loading': loading}"
        @click="handleRegister"
      >
        {{ loading ? '注册中...' : '注册' }}
      </button>
      
      <view class="back-to-login">
        <text>已有账号？</text>
        <text class="login-link" @click="goToLogin">返回登录</text>
      </view>
    </view>
  </view>
</template>

<script>
import { ref, reactive, computed } from 'vue';
import { register } from '../../services/api';
import { isValidEmail, isStrongPassword } from '../../utils/helpers';

export default {
  setup() {
    const form = reactive({
      username: '',
      nickname: '',
      email: '',
      password: '',
      confirmPassword: ''
    });
    
    const errors = reactive({
      username: '',
      nickname: '',
      email: '',
      password: '',
      confirmPassword: ''
    });
    
    const loading = ref(false);
    
    const isValid = computed(() => {
      return form.username.length >= 3 && 
             form.nickname.length >= 1 &&
             isValidEmail(form.email) && 
             form.password.length >= 8 && 
             form.password === form.confirmPassword &&
             !errors.username && 
             !errors.nickname &&
             !errors.email && 
             !errors.password && 
             !errors.confirmPassword;
    });
    
    const handleUsernameInput = (event) => {
      // 只保留英文字母和数字
      const value = event.detail.value;
      const filteredValue = value.replace(/[^a-zA-Z0-9]/g, '');
      
      // 如果输入了非法字符，更新输入框的值
      if (value !== filteredValue) {
        form.username = filteredValue;
        // 显示提示
        uni.showToast({
          title: '只能输入英文字母和数字',
          icon: 'none',
          duration: 1500
        });
      } else {
        form.username = value;
      }
      
      // 触发验证
      validateForm();
    };
    
    const validateForm = () => {
      // 用户名验证
      if (!form.username) {
        errors.username = '用户名不能为空';
      } else if (form.username.length < 3) {
        errors.username = '用户名不能少于3个字符';
      } else if (!/^[a-zA-Z0-9]+$/.test(form.username)) {
        errors.username = '用户名只能包含英文字母和数字';
      } else {
        errors.username = '';
      }
      
      // 昵称验证
      if (!form.nickname || form.nickname.length < 1) {
        errors.nickname = '昵称不能为空';
      } else if (form.nickname.length > 10) {
        errors.nickname = '昵称不能超过10个字符';
      } else {
        errors.nickname = '';
      }
      
      // 邮箱验证
      if (!form.email) {
        errors.email = '请输入邮箱';
      } else if (!isValidEmail(form.email)) {
        errors.email = '请输入有效的邮箱地址';
      } else {
        errors.email = '';
      }
      
      // 密码验证
      if (!form.password) {
        errors.password = '请输入密码';
      } else if (form.password.length < 8) {
        errors.password = '密码不能少于8个字符';
      } else if (!isStrongPassword(form.password)) {
        errors.password = '密码需包含字母和数字';
      } else {
        errors.password = '';
      }
      
      // 确认密码验证
      if (form.password !== form.confirmPassword) {
        errors.confirmPassword = '两次输入的密码不一致';
      } else {
        errors.confirmPassword = '';
      }
    };
    
    const handleRegister = async () => {
      if (!isValid.value) return;
      
      try {
        loading.value = true;
        
        // 构建注册请求数据
        const registerData = {
          username: form.username,
          nickname: form.nickname,
          email: form.email,
          password: form.password
        };
        
        console.log('发送注册请求:', registerData);
        
        // 调用注册API
        const result = await register(registerData);
        
        console.log('注册成功，响应数据:', result);
        
        // 显示成功提示
        uni.showToast({
          title: '注册成功',
          icon: 'success',
          duration: 2000
        });
        
        // 延迟跳转到登录页面
        setTimeout(() => {
          uni.navigateTo({
            url: '/pages/login/login'
          });
        }, 2000);
        
      } catch (error) {
        console.error('注册失败详情:', error);
        
        // 重置所有错误信息
        Object.keys(errors).forEach(key => {
          errors[key] = '';
        });
        
        let errorMessage = '注册失败，请稍后重试';
        
        // 处理不同类型的错误
        if (error.data) {
          const errorData = error.data;
          
          // 处理字段验证错误
          if (errorData.fieldErrors) {
            const fieldErrors = errorData.fieldErrors;
            if (fieldErrors.username) errors.username = fieldErrors.username;
            if (fieldErrors.nickname) errors.nickname = fieldErrors.nickname;
            if (fieldErrors.email) errors.email = fieldErrors.email;
            if (fieldErrors.password) errors.password = fieldErrors.password;
          }
          
          // 处理业务逻辑错误
          if (errorData.message) {
            errorMessage = errorData.message;
          }
          
          // 处理特定错误码
          if (errorData.code) {
            switch(errorData.code) {
              case 'USER_EXISTS':
                errorMessage = '用户名已存在';
                break;
              case 'VALIDATION_ERROR':
                errorMessage = '输入信息格式不正确';
                break;
              default:
                errorMessage = errorData.message || errorMessage;
            }
          }
        }
        
        // 显示错误提示
        uni.showToast({
          title: errorMessage,
          icon: 'none',
          duration: 3000
        });
      } finally {
        loading.value = false;
      }
    };
    
    const goToLogin = () => {
      uni.redirectTo({
        url: '/pages/login/login'
      });
    };
    
    return {
      form,
      errors,
      loading,
      isValid,
      handleUsernameInput,
      validateForm,
      handleRegister,
      goToLogin
    };
  }
};
</script>

<style>
.register-page {
  padding: 40rpx;
}

.header {
  margin-bottom: 60rpx;
}

.title {
  font-size: 48rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 20rpx;
}

.subtitle {
  font-size: 28rpx;
  color: #666;
}

.form-container {
  
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
  font-size: 24rpx;
  color: #ff4d4f;
  margin-top: 10rpx;
}

.register-btn {
  width: 100%;
  height: 90rpx;
  line-height: 90rpx;
  text-align: center;
  background-color: #3cc51f;
  color: #fff;
  font-size: 32rpx;
  border-radius: 8rpx;
  margin-top: 20rpx;
  margin-bottom: 40rpx;
}

.register-btn:disabled {
  background-color: #cccccc;
}

.register-btn.loading {
  opacity: 0.8;
}

.back-to-login {
  text-align: center;
  font-size: 28rpx;
  color: #666;
}

.login-link {
  color: #3cc51f;
  margin-left: 10rpx;
}
</style> 