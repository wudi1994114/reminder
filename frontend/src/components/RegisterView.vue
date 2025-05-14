<script setup>
import { ref } from 'vue';

// Define emits to communicate with the parent component
const emit = defineEmits(['register-attempt', 'show-login']);

// Local state for the form inputs
const username = ref('');
const password = ref('');
const confirmPassword = ref('');
const email = ref('');
const nickname = ref('');

// Prop to receive error messages from the parent
defineProps({
  registerError: {
    type: String,
    default: null
  }
});

// Function to handle form submission
function submitRegister() {
  // 添加更详细的前端验证
  if (username.value.length < 3) {
    alert('用户名长度必须至少为3个字符');
    return;
  }
  
  if (password.value !== confirmPassword.value) {
    alert('两次输入的密码不一致');
    return;
  }
  
  // Emit an event with the registration data
  emit('register-attempt', {
    username: username.value,
    password: password.value,
    email: email.value,
    nickname: nickname.value
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
      <div class="form-group">
        <label for="username">用户名:</label>
        <input type="text" id="username" v-model="username" required minlength="3" maxlength="100">
        <small class="form-hint">用户名长度必须在3到100个字符之间</small>
      </div>
      <div class="form-group">
        <label for="password">密码:</label>
        <input type="password" id="password" v-model="password" required minlength="6">
        <small class="form-hint">密码长度至少为6个字符</small>
      </div>
      <div class="form-group">
        <label for="confirm-password">确认密码:</label>
        <input type="password" id="confirm-password" v-model="confirmPassword" required>
      </div>
      <div class="form-group">
        <label for="email">邮箱:</label>
        <input type="email" id="email" v-model="email" required>
      </div>
      <div class="form-group">
        <label for="nickname">昵称:</label>
        <input type="text" id="nickname" v-model="nickname" required>
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
.register-form input[type="text"]
{
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
</style> 