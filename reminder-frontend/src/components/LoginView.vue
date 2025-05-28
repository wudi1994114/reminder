<script setup>
import { ref } from '../utils/imports.js';

// Define emits to communicate with the parent component
const emit = defineEmits(['login-attempt', 'show-register']);

// Local state for the form inputs
const loginUsername = ref('');
const loginPassword = ref('');

// Prop to receive error messages from the parent
defineProps({
  loginError: {
    type: String,
    default: null
  }
});

// Function to handle form submission
function submitLogin() {
  // Emit an event with the login credentials
  emit('login-attempt', {
    username: loginUsername.value,
    password: loginPassword.value
  });
}

// Function to signal parent to show registration view
function showRegister() {
    emit('show-register');
}
</script>

<template>
  <div class="login-container">
    <h2>登录</h2>
    <form @submit.prevent="submitLogin" class="login-form">
      <div class="form-group">
        <label for="login-username">用户名:</label>
        <input type="text" id="login-username" v-model="loginUsername" required>
      </div>
      <div class="form-group">
        <label for="login-password">密码:</label>
        <input type="password" id="login-password" v-model="loginPassword" required>
      </div>
      <!-- Display error message passed via prop -->
      <div v-if="loginError" class="error-message">
        {{ loginError }}
      </div>
      <button type="submit" class="login-button">登录</button>
    </form>
    <!-- Added registration link/button -->
    <p class="toggle-link">
      还没有账户？ <button @click="showRegister" class="link-button">立即注册</button>
    </p>
  </div>
</template>

<style scoped>
/* Styles specific to the login view - Copied and adapted from App.vue */
.login-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh; 
  padding: 2rem;
  box-sizing: border-box;
  background-color: #f8f9fa; 
}

.login-container h2 {
  margin-bottom: 1.5rem;
  color: #333;
  font-family: 'Nunito', sans-serif; 
}

.login-form {
  width: 100%;
  max-width: 400px; 
  padding: 2rem;
  background-color: #fff;
  border-radius: 12px; 
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08); 
}

.login-form .form-group {
  margin-bottom: 1.25rem; 
}

.login-form label {
  display: block;
  margin-bottom: 0.6rem;
  font-weight: 600;
  color: #555;
  font-family: 'Nunito', sans-serif;
}

.login-form input[type="email"],
.login-form input[type="password"],
.login-form input[type="text"]
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

.login-form input:focus {
  outline: none;
  border-color: var(--theme-primary-color, #a0c4ff); 
  box-shadow: 0 0 0 3px rgba(160, 196, 255, 0.3); 
}

.error-message {
  color: #d8000c; 
  background-color: #ffcdd2;
  border: 1px solid #ef9a9a;
  padding: 0.8rem 1rem;
  border-radius: 8px;
  margin-bottom: 1rem;
  text-align: center;
  font-size: 0.95rem;
  font-weight: bold;
  font-family: 'Nunito', sans-serif;
  width: 100%;
  box-shadow: 0 2px 10px rgba(239, 83, 80, 0.2);
  animation: errorPulse 2s infinite;
}

@keyframes errorPulse {
  0% { opacity: 1; }
  50% { opacity: 0.8; }
  100% { opacity: 1; }
}

.login-button {
  width: 100%;
  padding: 0.9rem;
  background-color: var(--theme-primary-color, #a0c4ff); 
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 1.05rem;
  font-weight: 700; 
  font-family: 'Nunito', sans-serif;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.1s ease;
}

.login-button:hover {
  background-color: var(--theme-hover-color, #8ab8ff); 
}

.login-button:active {
   transform: translateY(1px); 
}

/* Added styles for the toggle link/button */
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
    color: var(--theme-primary-color, #a0c4ff);
    text-decoration: underline;
    cursor: pointer;
    font-weight: 600;
}
.link-button:hover {
    color: var(--theme-hover-color, #8ab8ff);
}
</style> 