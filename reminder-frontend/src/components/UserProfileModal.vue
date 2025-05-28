<script setup>
import { ref, watch } from '../utils/imports.js';

// Props
const props = defineProps({
  show: {
    type: Boolean,
    required: true
  },
  currentUserData: { // Receive the current user data to pre-fill the form
    type: Object,
    required: true
  }
});

// Emits
const emit = defineEmits(['close', 'save-profile']);

// Local state for the form, initialized/updated via watcher
const formData = ref({});

// Watch the currentUserData prop to reset the form when the modal opens/data changes
watch(() => props.currentUserData, (newData) => {
    // Deep copy to avoid modifying the prop directly
    formData.value = JSON.parse(JSON.stringify(newData));
}, { deep: true, immediate: true });

// Submit handler
function submitForm() {
  // Emit the current form data for saving
  emit('save-profile', formData.value);
}

// Close handler
function closeModal() {
  emit('close');
}
</script>

<template>
  <div v-if="show" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content user-profile-modal">
      <h2>编辑个人资料</h2>
      <form @submit.prevent="submitForm">
        <div class="form-group">
          <label for="profileUsername">用户名:</label>
          <!-- Use nickname from currentUserData as username -->
          <input type="text" id="profileUsername" v-model="formData.nickname"> 
        </div>
        <div class="form-group">
          <label for="profileAvatar">头像 URL:</label>
          <input type="url" id="profileAvatar" v-model="formData.avatarUrl">
        </div>
        <div class="form-group">
          <label for="profileEmail">邮箱:</label>
          <input type="email" id="profileEmail" :value="formData.email" disabled>
        </div>
        <div class="form-group">
          <label for="profilePhone">手机号:</label>
          <input type="tel" id="profilePhone" v-model="formData.phone">
        </div>
        <!-- Add other editable profile fields from currentUserData if needed -->
        <div class="modal-actions">
          <button type="submit" class="button primary">保存</button>
          <button type="button" class="button" @click="closeModal">取消</button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
/* Styles specific to the user profile modal - Copied and adapted */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background-color: white;
  padding: 30px 40px;
  border-radius: 16px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  min-width: 400px; /* Slightly smaller min-width potentially */
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-content h2 {
  margin-top: 0;
  margin-bottom: 25px;
  color: #555;
  font-weight: 700;
  text-align: center;
  font-family: 'Nunito', sans-serif;
}

.user-profile-modal .form-group {
  margin-bottom: 1.25rem;
}
.user-profile-modal label {
  display: block;
  margin-bottom: 0.6rem;
  font-weight: 600;
  color: #555;
  font-family: 'Nunito', sans-serif;
}
.user-profile-modal input[type="text"],
.user-profile-modal input[type="email"],
.user-profile-modal input[type="tel"],
.user-profile-modal input[type="url"] {
  width: 100%;
  padding: 0.85rem 1rem;
  border: 1px solid #dcdcdc;
  border-radius: 8px;
  box-sizing: border-box;
  font-family: 'Nunito', sans-serif;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.user-profile-modal input:focus {
   outline: none;
   border-color: var(--theme-primary-color, #a0c4ff); 
   box-shadow: 0 0 0 3px rgba(160, 196, 255, 0.3); 
}

.user-profile-modal input:disabled {
    background-color: #f1f3f5;
    cursor: not-allowed;
    color: #adb5bd;
}

.modal-actions {
  margin-top: 1.5rem;
  display: flex;
  justify-content: flex-end;
}
.modal-actions button {
   padding: 12px 20px;
   margin-left: 12px;
   border: none;
   border-radius: 20px;
   cursor: pointer;
   font-weight: 600;
   font-family: 'Nunito', sans-serif;
   transition: background-color 0.2s ease, box-shadow 0.2s ease, transform 0.1s ease;
}
.modal-actions button:hover {
    transform: translateY(-1px);
}
.modal-actions button:active {
    transform: translateY(0px);
}
.modal-actions button.primary {
  background-color: var(--theme-primary-color, #a0c4ff);
  color: white;
}
.modal-actions button.primary:hover {
  background-color: var(--theme-hover-color, #8ab8ff);
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
}
.modal-actions button.button:not(.primary) {
  background-color: #f1f3f5; 
  color: #555;
  border: 1px solid #e9ecef;
}
.modal-actions button.button:not(.primary):hover {
  background-color: #e9ecef;
  box-shadow: 0 2px 5px rgba(0,0,0,0.05);
}
</style> 