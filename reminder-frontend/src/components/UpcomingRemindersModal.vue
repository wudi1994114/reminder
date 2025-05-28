<template>
  <div v-if="show" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content">
      <div class="modal-header">
        <h2>即将提醒</h2>
        <button class="close-btn" @click="closeModal">&times;</button>
      </div>
      <div class="modal-body">
        <div v-if="loading" class="loading-spinner">
          <div class="spinner"></div>
          <p>加载中...</p>
        </div>
        
        <div v-else>
          <div v-if="remindersByDate.length === 0" class="no-reminders">
            <p>暂无即将到来的提醒</p>
          </div>
          
          <div v-else class="reminder-list">
            <!-- 按日期分组展示 -->
            <div 
              v-for="(group, index) in remindersByDate" 
              :key="index" 
              class="date-group"
            >
              <div class="date-header">{{ group.date }}</div>
              <div 
                v-for="reminder in group.reminders" 
                :key="reminder.id" 
                class="reminder-item"
                @click="handleReminderClick(reminder)"
              >
                <div class="reminder-time">
                  {{ formatTimeOnly(reminder.eventTime) }}
                </div>
                <div class="reminder-content">
                  <h3 class="reminder-title">{{ reminder.title }}</h3>
                  <p v-if="reminder.description" class="reminder-description">{{ reminder.description }}</p>
                  <div class="reminder-type">
                    <span :class="getReminderTypeClass(reminder.reminderType)">
                      {{ getReminderTypeText(reminder.reminderType) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from '../utils/imports.js';
import { getUpcomingReminders } from '../services/api';
import { showNotification } from '../services/store';

// Props
const props = defineProps({
  show: {
    type: Boolean,
    required: true
  }
});

// Emits
const emit = defineEmits(['close', 'reminder-click']);

// 响应式数据
const reminders = ref([]);
const loading = ref(false);

// 按日期分组的提醒列表
const remindersByDate = computed(() => {
  const groups = [];
  const dateMap = new Map();
  
  // 按日期分组
  reminders.value.forEach(reminder => {
    const date = new Date(reminder.eventTime);
    const dateKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    const dateDisplay = formatDateForDisplay(date);
    
    if (!dateMap.has(dateKey)) {
      const group = {
        date: dateDisplay,
        dateKey: dateKey,
        reminders: []
      };
      groups.push(group);
      dateMap.set(dateKey, group);
    }
    
    dateMap.get(dateKey).reminders.push(reminder);
  });
  
  // 按日期排序
  groups.sort((a, b) => a.dateKey.localeCompare(b.dateKey));
  
  // 每组内部按时间排序
  groups.forEach(group => {
    group.reminders.sort((a, b) => new Date(a.eventTime) - new Date(b.eventTime));
  });
  
  return groups;
});

// 将日期格式化为显示用格式
const formatDateForDisplay = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const weekDay = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()];
  
  // 检查是否为今天、明天或后天
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  
  const tomorrow = new Date(today);
  tomorrow.setDate(tomorrow.getDate() + 1);
  
  const dayAfterTomorrow = new Date(today);
  dayAfterTomorrow.setDate(dayAfterTomorrow.getDate() + 2);
  
  const targetDate = new Date(date);
  targetDate.setHours(0, 0, 0, 0);
  
  let prefix = '';
  if (targetDate.getTime() === today.getTime()) {
    prefix = '今天';
  } else if (targetDate.getTime() === tomorrow.getTime()) {
    prefix = '明天';
  } else if (targetDate.getTime() === dayAfterTomorrow.getTime()) {
    prefix = '后天';
  }
  
  return prefix ? `${prefix} (${month}月${day}日 ${weekDay})` : `${month}月${day}日 ${weekDay}`;
};

// 加载即将到来的提醒
const loadUpcomingReminders = async () => {
  loading.value = true;
  try {
    const response = await getUpcomingReminders();
    reminders.value = response.data;
  } catch (error) {
    console.error('加载即将提醒失败:', error);
    showNotification('加载提醒数据失败', 'error');
  } finally {
    loading.value = false;
  }
};

// 监听show属性，当打开弹窗时加载数据
watch(() => props.show, (newVal) => {
  if (newVal) {
    loadUpcomingReminders();
  }
}, { immediate: true });

// 格式化日期时间（完整格式）
const formatDateTime = (dateTimeString) => {
  const date = new Date(dateTimeString);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  
  return `${year}-${month}-${day} ${hour}:${minute}`;
};

// 仅格式化时间部分为 HH:MM
const formatTimeOnly = (dateTimeString) => {
  const date = new Date(dateTimeString);
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  
  return `${hour}:${minute}`;
};

// 获取提醒类型的CSS类
const getReminderTypeClass = (type) => {
  return `type-${type.toLowerCase()}`;
};

// 获取提醒类型的显示文本
const getReminderTypeText = (type) => {
  const typeMap = {
    'EMAIL': '邮件',
    'SMS': '短信',
    'NOTIFICATION': '通知'
  };
  return typeMap[type] || type;
};

// 处理点击提醒项
const handleReminderClick = (reminder) => {
  // 格式化提醒数据
  const eventData = {
    id: reminder.id,
    title: reminder.title,
    description: reminder.description || '',
    reminderType: reminder.reminderType,
    // 将时间格式化为前端需要的格式
    selectedDate: formatDateOnly(reminder.eventTime),
    selectedHour: new Date(reminder.eventTime).getHours(),
    selectedMinute: new Date(reminder.eventTime).getMinutes(),
    selectedSecond: new Date(reminder.eventTime).getSeconds()
  };
  
  // 关闭当前模态窗口
  closeModal();
  
  // 触发提醒点击事件，传递格式化后的数据
  emit('reminder-click', eventData);
};

// 格式化日期（仅日期部分）
const formatDateOnly = (dateTimeString) => {
  const date = new Date(dateTimeString);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  
  return `${year}-${month}-${day}`;
};

// 关闭弹窗
const closeModal = () => {
  emit('close');
};
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal-content {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  width: 90%;
  max-width: 450px;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background-color: var(--theme-primary-color, #4a6cf7);
  color: white;
}

.modal-header h2 {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.3rem;
  color: white;
  cursor: pointer;
  padding: 0;
  margin: 0;
  line-height: 1;
}

.modal-body {
  padding: 12px;
  overflow-y: auto;
  max-height: calc(80vh - 50px);
}

.loading-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
}

.spinner {
  border: 3px solid rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  border-top: 3px solid var(--theme-primary-color, #4a6cf7);
  width: 30px;
  height: 30px;
  animation: spin 1s linear infinite;
  margin-bottom: 8px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.no-reminders {
  text-align: center;
  padding: 20px 0;
  color: #666;
}

.reminder-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.date-group {
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  overflow: hidden;
}

.date-header {
  background-color: #f5f7fa;
  padding: 6px 12px;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #e0e0e0;
  font-size: 0.9rem;
}

.reminder-item {
  display: flex;
  border-bottom: 1px solid #e0e0e0;
  overflow: hidden;
  transition: all 0.2s ease;
  cursor: pointer;
}

.reminder-item:last-child {
  border-bottom: none;
}

.reminder-item:hover {
  background-color: rgba(74, 108, 247, 0.05);
}

.reminder-time {
  padding: 8px 10px;
  min-width: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: #555;
  border-right: 1px solid #e0e0e0;
  font-size: 0.9rem;
}

.reminder-content {
  padding: 8px 10px;
  flex: 1;
}

.reminder-title {
  margin: 0 0 5px 0;
  font-size: 0.95rem;
  color: #333;
}

.reminder-description {
  margin: 0 0 5px 0;
  color: #666;
  font-size: 0.8rem;
}

.reminder-type {
  display: flex;
  justify-content: flex-end;
}

.reminder-type span {
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 0.7rem;
  font-weight: 500;
}

.type-email {
  background-color: #e3f2fd;
  color: #1976d2;
}

.type-sms {
  background-color: #e8f5e9;
  color: #388e3c;
}

.type-notification {
  background-color: #fff8e1;
  color: #ffa000;
}

@media (max-width: 768px) {
  .reminder-item {
    flex-direction: column;
  }
  
  .reminder-time {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #e0e0e0;
    padding: 6px;
  }
}
</style> 