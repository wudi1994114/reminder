<script setup>
// 删除：未使用的导入
// import { ref, watch } from 'vue';

// Props received from parent (App.vue)
const props = defineProps({
  currentUser: {
    type: Object, // Should contain { id, email, nickname, avatarUrl?, phone? }
    default: null
  },
  selectedYear: {
    type: Number,
    required: true
  },
  selectedMonth: {
    type: Number,
    required: true
  },
  currentMonthName: {
    type: String,
    required: true
  },
  isLoggedIn: {
    type: Boolean,
    required: true
  }
});

// Emits sent to parent (App.vue)
const emit = defineEmits(['logout', 'open-profile', 'toggle-month-selector', 'select-month']);

// 删除：不再需要本地 ref 和相关逻辑
/*
const localSelectedMonthYear = ref('');
watch(() => props.selectedMonthYear, (newValue) => {
  localSelectedMonthYear.value = newValue;
});
function handleMonthInputChange() {
    emit('update:selectedMonthYear', localSelectedMonthYear.value);
}
localSelectedMonthYear.value = props.selectedMonthYear;
*/

</script>

<template>
  <header class="app-header">
    <!-- 新增：左侧占位符，用于居中 -->
    <div class="header-spacer"></div>

    <!-- 修改：日期显示区域 -->
    <div class="date-display header-section">
        <span class="month-display clickable" @click.stop="emit('toggle-month-selector')">
          {{ selectedYear }}年 {{ currentMonthName }}
        </span>
    </div>

    <!-- 修改：用户区域 -->
    <div v-if="isLoggedIn" class="user-profile-area header-section">
      <img 
        :src="currentUser?.avatarUrl || 'https://via.placeholder.com/40'" 
        alt="User Avatar" 
        class="user-avatar"
        @click="emit('open-profile')"
      >
      <span class="user-name" @click="emit('open-profile')">
        {{ currentUser?.nickname || '用户' }}
      </span>
      <button @click.stop="emit('logout')" class="logout-button">登出</button>
    </div>
  </header>
</template>

<style scoped>
/* Styles for the header */
.app-header {
  display: flex;
  justify-content: space-between; 
  align-items: center;
  padding: 10px 15px; 
  z-index: 10; 
  position: relative; 
  flex-shrink: 0; 
  border-top-left-radius: 16px;
  border-top-right-radius: 16px;
}

/* Style for sections within the header */
.header-section {
    display: flex;
    align-items: center;
}

/* 新增：左侧占位符样式 */
.header-spacer {
    /* 尝试让它和右侧用户区域宽度相似 */
    /* visibility: hidden; 使其不显示但占位 */
    /* 或者可以计算右侧宽度，但 flexbox 更灵活 */
    flex-basis: 150px; /* 估算值，可能需要根据用户区实际宽度调整 */
    flex-shrink: 0;
}

/* 修改：日期显示区域样式 */
.date-display {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-family: var(--font-family-main);
  padding: 8px 16px;
  border-radius: 8px;
  transition: background-color 0.2s ease;
}

.month-display {
  font-size: 1.6rem;
  font-weight: 600;
  color: var(--font-color-primary);
  position: relative;
  padding: 4px 12px;
  border-radius: 6px;
  transition: all 0.2s ease;
  display: inline-block;
  text-align: center;
}

.month-display.clickable {
  cursor: pointer;
}

.month-display.clickable:hover {
  background-color: var(--theme-day-hover-bg);
}

.month-display.clickable::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 2px;
  background-color: var(--theme-primary-color);
  transition: all 0.2s ease;
  transform: none;
}

.month-display.clickable:hover::after {
  width: 100%;
}

/* 修改：用户区域样式 */
.user-profile-area {
    /* 确保它不被压缩 */
    flex-shrink: 0;
    flex-basis: 150px; /* 尝试与 spacer 保持一致 */
    justify-content: flex-end; /* 用户信息内部靠右 */
    cursor: pointer;
    gap: 10px; 
}

.user-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    /* margin-right: 10px; Removed margin, using gap */
    object-fit: cover;
    border: 2px solid var(--theme-primary-color, #e0e0e0);
}

.user-name {
    font-weight: 600;
    color: #333;
    white-space: nowrap;
}

.logout-button {
  /* margin-left: 15px; Removed margin, using gap */
  padding: 6px 12px;
  font-size: 0.85rem;
  background-color: #f1f3f5;
  color: #495057;
  border: 1px solid #dee2e6;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  font-family: 'Nunito', sans-serif;
  transition: background-color 0.2s, border-color 0.2s;
  white-space: nowrap;
}
.logout-button:hover {
    background-color: #e9ecef;
    border-color: #ced4da;
}

/* Responsive adjustments */
@media (max-width: 768px) { /* 调整断点 */
    .header-spacer {
        flex-basis: 80px; /* 减小占位符 */
    }
    .user-profile-area {
        flex-basis: 80px; /* 减小用户区 */
        gap: 5px;
    }
    .user-name {
        display: none; /* 在平板尺寸也隐藏用户名 */
    }
    .logout-button {
        padding: 4px 8px;
        font-size: 0.8rem;
    }
}

@media (max-width: 600px) {
  .app-header {
    padding: 8px 10px; 
  }
  .header-spacer {
      flex-basis: 50px; /* 进一步减小 */
  }
  .date-display {
      font-size: 1.1em; /* 相应调整小屏幕字号 */
  }
  .month-display {
      display: none; 
  }
  .user-profile-area {
      flex-basis: 50px;
      gap: 5px; 
  }
  .user-name {
      display: none; 
  }
  .logout-button {
      padding: 4px 8px;
      font-size: 0.8rem;
  }
}
</style> 