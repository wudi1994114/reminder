<script setup>
import { ref } from 'vue';
import { isValidEmail, isStrongPassword } from '../utils/helpers';
import { DEFAULT_AVATARS } from '../constants/avatars';

// Define emits to communicate with the parent component
const emit = defineEmits(['register-attempt', 'show-login']);

// Local state for the form inputs
const username = ref('');
const password = ref('');
const confirmPassword = ref('');
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
    confirmPassword: confirmPassword.value,
    email: email.value,
    nickname: nickname.value,
    phone: phone.value
  }[fieldName];

  if (!fieldValue || !fieldValue.trim()) {
    return;
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
      // 如果确认密码已经输入，也要检查确认密码
      if (confirmPassword.value) {
        validateConfirmPassword();
      }
      break;
    case 'confirmPassword':
      validateConfirmPassword();
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

// 确认密码验证
function validateConfirmPassword() {
  if (!confirmPassword.value) {
    return;
  }
  if (password.value !== confirmPassword.value) {
    formErrors.value.confirmPassword = '两次输入的密码不一致';
  } else {
    delete formErrors.value.confirmPassword;
  }
}

// 提交前的完整验证
function validateForm() {
  formErrors.value = {};
  
  // 所有字段必填的验证
  if (!username.value.trim()) {
    formErrors.value.username = '请输入用户名';
  }
  if (!password.value) {
    formErrors.value.password = '请输入密码';
  }
  if (!confirmPassword.value) {
    formErrors.value.confirmPassword = '请确认密码';
  }
  if (!email.value.trim()) {
    formErrors.value.email = '请输入邮箱';
  }
  if (!nickname.value.trim()) {
    formErrors.value.nickname = '请输入昵称';
  }
  if (!phone.value.trim()) {
    formErrors.value.phone = '请输入手机号码';
  }

  // 如果有任何必填字段未填，直接返回false
  if (Object.keys(formErrors.value).length > 0) {
    return false;
  }

  // 所有字段都已填写，进行格式验证
  validateField('username');
  validateField('password');
  validateField('email');
  validateField('nickname');
  validateField('phone');
  validateConfirmPassword();

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
        <div v-if="formErrors.username" class="error-message">{{ formErrors.username }}</div>
      </div>
      
      <div class="form-group" :class="{ 'has-error': formErrors.password }">
        <label for="password">密码:</label>
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
        <div v-if="formErrors.password" class="error-message">{{ formErrors.password }}</div>
      </div>
      
      <div class="form-group" :class="{ 'has-error': formErrors.confirmPassword }">
        <label for="confirm-password">确认密码:</label>
        <input 
          type="password" 
          id="confirm-password" 
          v-model="confirmPassword" 
          required
          @blur="validateField('confirmPassword')"
          @input="validateField('confirmPassword')"
        >
        <div v-if="formErrors.confirmPassword" class="error-message">{{ formErrors.confirmPassword }}</div>
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
        <input 
          type="text" 
          id="nickname" 
          v-model="nickname" 
          required
          @blur="validateField('nickname')"
        >
        <div v-if="formErrors.nickname" class="error-message">{{ formErrors.nickname }}</div>
      </div>
      
      <div class="form-group" :class="{ 'has-error': formErrors.phone }">
        <label for="phone">手机号码:</label>
        <input 
          type="tel"
          id="phone" 
          v-model="phone" 
          required
          pattern="^1[3-9]\d{9}$"
          @blur="validateField('phone')"
          @input="validateField('phone')"
        >
        <small class="form-hint">请输入11位手机号码</small>
        <div v-if="formErrors.phone" class="error-message">{{ formErrors.phone }}</div>
      </div>
      
      <!-- Display error message passed via prop -->
      <div v-if="registerError" class="error-message">
        {{ registerError }}
      </div>
      
      <button type="submit" class="register-button">注册</button>
    </form>
    <!-- Back to login link -->
    <p class="toggle-link">
      已有账户？ <button @click="showLogin" class="link-button">返回登录</button>
    </p>
  </div>
</template>

<style scoped>
/* Styles specific to the register view - Similar to LoginView styles */
.register-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh; 
  padding: 2rem;
  box-sizing: border-box;
  background-color: #f8f9fa; 
}

.register-container h2 {
  margin-bottom: 1.5rem;
  color: #333;
  font-family: 'Nunito', sans-serif; 
}

.register-form {
  width: 100%;
  max-width: 400px; 
  padding: 2rem;
  background-color: #fff;
  border-radius: 12px; 
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08); 
}

.register-form .form-group {
  margin-bottom: 1.25rem; 
}

.register-form label {
  display: block;
  margin-bottom: 0.6rem;
  font-weight: 600;
  color: #555;
  font-family: 'Nunito', sans-serif;
}

.register-form input[type="email"],
.register-form input[type="password"],
.register-form input[type="text"],
.register-form input[type="tel"] {
  width: 100%;
  padding: 0.85rem 1rem; 
  border: 1px solid #dcdcdc;
  border-radius: 8px; 
  box-sizing: border-box;
  font-size: 1rem;
  font-family: 'Nunito', sans-serif;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.register-form input:focus {
  outline: none;
  border-color: var(--theme-primary-color, #4CAF50); 
  box-shadow: 0 0 0 3px rgba(76, 175, 80, 0.3); 
}

.error-message {
  color: #d8000c; 
  background-color: #ffcdd2;
  border: 1px solid #ef9a9a;
  padding: 0.8rem 1rem;
  border-radius: 8px;
  margin-bottom: 1rem;
  text-align: center;
  font-size: 0.9rem;
  font-family: 'Nunito', sans-serif;
}

.register-button {
  width: 100%;
  padding: 0.9rem;
  background-color: var(--theme-primary-color, #4CAF50); 
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 1.05rem;
  font-weight: 700; 
  font-family: 'Nunito', sans-serif;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.1s ease;
}

.register-button:hover {
  background-color: var(--theme-hover-color, #45a049); 
}

.register-button:active {
   transform: translateY(1px); 
}

.toggle-link {
    margin-top: 1.5rem;
    font-size: 0.9rem;
    color: #666;
    font-family: 'Nunito', sans-serif;
    text-align: center;
}
.link-button {
    background: none;
    border: none;
    padding: 0;
    font: inherit;
    color: var(--theme-primary-color, #4CAF50);
    text-decoration: underline;
    cursor: pointer;
    font-weight: 600;
}
.link-button:hover {
    color: var(--theme-hover-color, #45a049);
}

.form-hint {
  display: block;
  margin-top: 4px;
  font-size: 0.8rem;
  color: #666;
}

/* 新增样式 */
.has-error input {
  border-color: #d8000c;
}

.has-error input:focus {
  box-shadow: 0 0 0 3px rgba(216, 0, 12, 0.2);
}

.error-message {
  color: #d8000c;
  font-size: 0.85rem;
  margin-top: 0.4rem;
}

/* 密码强度指示器样式 */
.password-strength {
  margin-top: 0.5rem;
  font-size: 0.85rem;
}

.strength-indicator {
  height: 4px;
  border-radius: 2px;
  margin-top: 0.3rem;
  background-color: #ddd;
}

.strength-indicator div {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s ease;
}

.strength-weak {
  background-color: #ff4444;
  width: 33.33%;
}

.strength-medium {
  background-color: #ffbb33;
  width: 66.66%;
}

.strength-strong {
  background-color: #00C851;
  width: 100%;
}

/* 新增头像上传相关样式 */
.avatar-upload {
  margin-bottom: 1.5rem;
  text-align: center;
}

.avatar-preview {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  margin: 0 auto 1rem;
  border: 2px solid #dcdcdc;
  overflow: hidden;
  position: relative;
  background-color: #f8f9fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-preview .placeholder {
  color: #666;
  font-size: 2rem;
}

.avatar-upload-btn {
  background-color: var(--theme-primary-color, #4CAF50);
  color: white;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background-color 0.2s ease;
}

.avatar-upload-btn:hover {
  background-color: var(--theme-hover-color, #45a049);
}

.avatar-upload input[type="file"] {
  display: none;
}

/* 新增头像选择相关样式 */
.avatar-selection {
  margin-bottom: 1.5rem;
  text-align: center;
}

.avatar-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
}

.avatar-option {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  margin: 0.5rem;
  cursor: pointer;
  overflow: hidden;
  position: relative;
  border: 2px solid transparent;
}

.avatar-option img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-option.selected {
  border-color: var(--theme-primary-color, #4CAF50);
}
</style> 