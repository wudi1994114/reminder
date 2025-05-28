<template>
  <div v-if="show" class="modal-overlay" @click.self="onCancel">
    <div class="modal-content">
      <h2>{{ title }}</h2>
      <p class="message">{{ message }}</p>
      <div class="modal-actions">
        <button @click="onConfirm" class="button primary confirm-btn">{{ confirmText }}</button>
        <button @click="onCancel" class="button cancel-btn">{{ cancelText }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from '../utils/imports.js';

const props = defineProps({
  show: {
    type: Boolean,
    required: true
  },
  title: {
    type: String,
    default: '确认'
  },
  message: {
    type: String,
    default: '确定要执行此操作吗？'
  },
  confirmText: {
    type: String,
    default: '确定'
  },
  cancelText: {
    type: String,
    default: '取消'
  }
});

const emit = defineEmits(['confirm', 'cancel']);

const onConfirm = () => {
  emit('confirm');
};

const onCancel = () => {
  emit('cancel');
};
</script>

<style scoped>
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
  z-index: 2000;
}

.modal-content {
  position: relative;
  background-color: white;
  padding: 30px 40px; 
  border-radius: 16px; 
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.2);
  min-width: 360px;
  max-width: 90vw;
  overflow-y: auto;
  z-index: 2001;
  animation: modalFadeIn 0.2s ease-out;
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-content h2 {
  margin-top: 0;
  margin-bottom: 16px;
  color: #555; 
  font-weight: 700; 
  text-align: center;
  font-family: 'Nunito', sans-serif;
}

.message {
  margin-bottom: 24px;
  text-align: center;
  font-size: 16px;
  color: #666;
  line-height: 1.5;
}

.modal-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 8px;
}

.button {
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: 'Nunito', sans-serif;
}

.primary {
  background-color: var(--theme-primary-color, #4CAF50);
  color: white;
}

.primary:hover {
  background-color: var(--theme-hover-color, #45a049);
}

.cancel-btn {
  background-color: #f8f9fa;
  color: #666;
}

.cancel-btn:hover {
  background-color: #e9ecef;
}

/* 响应式调整 */
@media (max-width: 480px) {
  .modal-content {
    padding: 20px 25px;
    min-width: 280px;
  }
  
  .button {
    padding: 8px 16px;
    font-size: 14px;
  }
}

/* 暗色模式支持 */
@media (prefers-color-scheme: dark) {
  .modal-content {
    background-color: #2d2d2d;
  }
  
  .modal-content h2 {
    color: #e0e0e0;
  }
  
  .message {
    color: #ccc;
  }
  
  .cancel-btn {
    background-color: #3d3d3d;
    color: #e0e0e0;
  }
  
  .cancel-btn:hover {
    background-color: #4d4d4d;
  }
}
</style> 