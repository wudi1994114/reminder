<script setup>
import { ref } from '../utils/imports.js';
import { isValidEmail, isStrongPassword } from '../utils/helpers';
import { DEFAULT_AVATARS } from '../constants/avatars';

// Define emits to communicate with the parent component
const emit = defineEmits(['register-attempt', 'show-login']);

// Local state for the form inputs
const username = ref('');
const password = ref('');
const email = ref('');
const nickname = ref('');
const phone = ref('');
const selectedAvatar = ref(DEFAULT_AVATARS[0]); // 默认选择第一个头像
const formErrors = ref({});

// Prop to receive error messages from the parent
defineProps({
  registerError: {
    type: String,
    default: null
  }
});

// 单个字段验证函数
function validateField(fieldName) {
  const fieldValue = {
    username: username.value,
    password: password.value,
    email: email.value,
    nickname: nickname.value,
    phone: phone.value
  }[fieldName];

  if (!fieldValue || (typeof fieldValue === 'string' && !fieldValue.trim()) ) { // 确保对 password 也适用
    // 对于密码字段，即使为空字符串也需要触发后续的 isStrongPassword 验证，因此这里不做提前返回
    if (fieldName !== 'password') return;
  }

  switch(fieldName) {
    case 'username':
      if (fieldValue.length < 3) {
        formErrors.value.username = '用户名长度必须至少为3个字符';
      } else {
        delete formErrors.value.username;
      }
      break;
    case 'password':
      if (!isStrongPassword(fieldValue)) {
        formErrors.value.password = '密码必须包含大小写字母和数字，且长度至少为8个字符';
      } else {
        delete formErrors.value.password;
      }
      break;
    case 'email':
      if (!isValidEmail(fieldValue)) {
        formErrors.value.email = '请输入有效的邮箱地址';
      } else {
        delete formErrors.value.email;
      }
      break;
    case 'nickname':
      if (fieldValue.trim().length < 2) {
        formErrors.value.nickname = '昵称长度必须至少为2个字符';
      } else {
        delete formErrors.value.nickname;
      }
      break;
    case 'phone':
      // 中国大陆手机号验证规则
      const phoneRegex = /^1[3-9]\d{9}$/;
      if (!phoneRegex.test(fieldValue.trim())) {
        formErrors.value.phone = '请输入有效的手机号码';
      } else {
        delete formErrors.value.phone;
      }
      break;
  }
}

// 提交前的完整验证
function validateForm() {
  formErrors.value = {};
  
  if (!username.value.trim()) formErrors.value.username = '请输入用户名';
  if (!password.value) formErrors.value.password = '请输入密码'; // 密码为空的提示
  if (!email.value.trim()) formErrors.value.email = '请输入邮箱';
  if (!nickname.value.trim()) formErrors.value.nickname = '请输入昵称';
  if (!phone.value.trim()) formErrors.value.phone = '请输入手机号码';

  if (Object.keys(formErrors.value).length > 0) return false;

  validateField('username');
  validateField('password'); // 确保对密码进行强度验证
  validateField('email');
  validateField('nickname');
  validateField('phone');

  return Object.keys(formErrors.value).length === 0;
}

// 处理头像选择
function selectAvatar(avatar) {
  selectedAvatar.value = avatar;
}

// Function to handle form submission
async function submitRegister() {
  if (!validateForm()) {
    return;
  }
  
  // Emit an event with the registration data
  emit('register-attempt', {
    username: username.value,
    password: password.value,
    email: email.value,
    nickname: nickname.value,
    phone: phone.value,
    avatarUrl: selectedAvatar.value.url
  });
}

// Function to signal parent to show login view
function showLogin() {
  emit('show-login');
}
</script>

<template>
  <div class="register-page">
    <div class="register-container">
      <h2>注册</h2>
      <form @submit.prevent="submitRegister" class="register-form">
        <!-- 头像选择部分 -->
        <div class="avatar-selection">
          <h3>选择头像</h3>
          <div class="avatar-grid">
            <div 
              v-for="avatar in DEFAULT_AVATARS" 
              :key="avatar.id"
              class="avatar-option"
              :class="{ 'selected': selectedAvatar.id === avatar.id }"
              @click="selectAvatar(avatar)"
            >
              <img :src="avatar.url" :alt="avatar.alt">
            </div>
          </div>
        </div>

        <div class="form-group" :class="{ 'has-error': formErrors.username }">
          <label for="username">用户名:</label>
          <div class="input-wrapper">
            <input 
              type="text" 
              id="username" 
              v-model="username" 
              required 
              minlength="3" 
              maxlength="100"
              @blur="validateField('username')"
            >
            <small class="form-hint">用户名长度必须在3到100个字符之间</small>
          </div>
          <div v-if="formErrors.username" class="error-message">{{ formErrors.username }}</div>
        </div>
        
        <div class="form-group" :class="{ 'has-error': formErrors.password }">
          <label for="password">密码:</label>
          <div class="input-wrapper">
            <input 
              type="password" 
              id="password" 
              v-model="password" 
              required 
              minlength="8"
              @blur="validateField('password')"
              @input="validateField('password')"
            >
            <small class="form-hint">密码必须包含大小写字母和数字，且长度至少为8个字符</small>
          </div>
          <div v-if="formErrors.password" class="error-message">{{ formErrors.password }}</div>
        </div>
        
        <div class="form-group" :class="{ 'has-error': formErrors.email }">
          <label for="email">邮箱:</label>
          <input 
            type="email" 
            id="email" 
            v-model="email" 
            required
            @blur="validateField('email')"
          >
          <div v-if="formErrors.email" class="error-message">{{ formErrors.email }}</div>
        </div>
        
        <div class="form-group" :class="{ 'has-error': formErrors.nickname }">
          <label for="nickname">昵称:</label>
          <div class="input-wrapper">
            <input 
              type="text" 
              id="nickname" 
              v-model="nickname" 
              required
              minlength="2"
              @blur="validateField('nickname')"
            >
            <small class="form-hint">昵称长度必须至少为2个字符</small>
          </div>
          <div v-if="formErrors.nickname" class="error-message">{{ formErrors.nickname }}</div>
        </div>

        <div class="form-group" :class="{ 'has-error': formErrors.phone }">
          <label for="phone">手机号码:</label>
          <input 
            type="tel" 
            id="phone" 
            v-model="phone" 
            required
            @blur="validateField('phone')"
          >
          <div v-if="formErrors.phone" class="error-message">{{ formErrors.phone }}</div>
        </div>

        <div v-if="registerError" class="error-message global-error">
          {{ registerError }}
        </div>
        
        <div class="register-actions">
          <button type="submit" class="btn-register">注册</button>
          <button type="button" @click="showLogin" class="login-link">已有账户? 去登录</button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background-color: #f5f5f5;
}

.register-container {
  max-width: 450px;
  width: 90%;
  margin: 0 auto;
  padding: 1rem 2rem;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  background-color: #fff;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  max-height: 90vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  font-family: 'Nunito', sans-serif;
}

.input-wrapper {
  display: block;
  margin-bottom: 0.1rem;
}

/* Webkit 浏览器滚动条样式 */
.register-container::-webkit-scrollbar {
  width: 8px;
}
.register-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 10px;
}
.register-container::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 10px;
}
.register-container::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}

.register-container > h2 {
  text-align: center;
  margin-bottom: 0.7rem;
  font-size: 1.6rem; 
  color: #333;
  font-family: 'Nunito', sans-serif;
}

.avatar-selection {
  margin-bottom: 0.6rem;
  text-align: center;
}

.avatar-selection h3 {
  font-size: 0.95rem;
  color: #555;
  margin-bottom: 0.25rem;
  font-family: 'Nunito', sans-serif;
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(45px, 1fr));
  gap: 6px;
  justify-items: center;
  max-width: 100%;
  margin: 0 auto;
}

.avatar-option {
  display: inline-block;
}

.avatar-option img {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  object-fit: cover;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.2s ease-in-out, transform 0.2s ease-in-out;
}

.avatar-option img:hover {
  transform: scale(1.08);
}

.avatar-option.selected img {
  border-color: #4CAF50;
  box-shadow: 0 0 5px rgba(76, 175, 80, 0.4);
}

.form-group {
  margin-bottom: 0.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.15rem;
  font-size: 0.9rem; 
  color: #555;
  font-weight: 600;
  font-family: 'Nunito', sans-serif;
}

.form-group input[type="text"],
.form-group input[type="password"],
.form-group input[type="email"],
.form-group input[type="tel"] {
  width: 100%;
  padding: 0.45rem 0.9rem;
  font-size: 0.95rem;
  font-family: 'Nunito', sans-serif;
  border: 1px solid #dcdcdc;
  border-radius: 8px;
  box-sizing: border-box;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-hint {
  display: block;
  font-size: 0.75rem;
  color: #6c757d;
  font-family: 'Nunito', sans-serif;
  margin-top: 0.1rem;
}

.form-group input:focus {
  border-color: #4CAF50;
  outline: 0;
  box-shadow: 0 0 0 0.2rem rgba(76, 175, 80, 0.25);
}

.error-message {
  color: #d8000c;
  font-size: 0.75rem;
  margin-top: 0.1rem;
  font-family: 'Nunito', sans-serif;
}

.global-error {
  text-align: center;
  margin-bottom: 0.5rem;
  font-weight: bold;
  background-color: #ffcdd2;
  color: #d8000c;
  padding: 0.4rem;
  border-radius: 8px;
}

.register-actions {
  margin-top: 0.7rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  margin-bottom: 0.5rem;
}

.register-actions button {
  padding: 0.45rem;
  font-size: 0.95rem;
  font-family: 'Nunito', sans-serif;
  font-weight: 700;
  border-radius: 8px;
  cursor: pointer;
  border: none;
  transition: background-color 0.2s ease-in-out, transform 0.1s ease;
}

.register-actions button:active {
  transform: translateY(1px);
}

.register-actions .btn-register {
  background-color: #4CAF50;
  color: white;
}

.register-actions .btn-register:hover {
  background-color: #45a049;
}

.register-actions .login-link {
  background-color: transparent;
  color: #4CAF50;
  border: 1px solid #4CAF50;
  text-align: center;
}

.register-actions .login-link:hover {
  background-color: rgba(76, 175, 80, 0.05);
  color: #3e8e41;
}

/* 错误状态下的输入框样式 */
.form-group.has-error input {
  border-color: #d8000c;
}

.form-group.has-error input:focus {
  box-shadow: 0 0 0 0.2rem rgba(216, 0, 12, 0.2);
}
</style> 