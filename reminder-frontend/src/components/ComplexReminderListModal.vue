<template>
  <div v-if="show" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content">
      <h2>已设置的复杂提醒</h2>
      
      <div class="toolbar">
        <div class="search-box">
          <input 
            type="text" 
            v-model="searchText" 
            placeholder="搜索标题..." 
            class="search-input"
          />
        </div>
        <div class="filter-options">
          <select v-model="filterStatus" class="filter-select">
            <option value="all">全部状态</option>
            <option value="active">活跃</option>
            <option value="expired">已过期</option>
          </select>
        </div>
      </div>
      
      <div class="reminder-list">
        <!-- 没有提醒时显示 -->
        <div v-if="filteredReminders.length === 0" class="empty-state">
          <div v-if="complexReminders.length === 0">
            暂无复杂提醒，点击"新建复杂提醒"按钮创建
          </div>
          <div v-else>
            没有符合条件的复杂提醒
          </div>
        </div>
        
        <!-- 提醒列表 -->
        <div v-else class="reminder-items">
          <div 
            v-for="reminder in filteredReminders" 
            :key="reminder.id" 
            class="reminder-item"
            :class="{ 'expired': isExpired(reminder) }"
          >
            <div class="reminder-content" @click="editReminder(reminder)">
              <div class="reminder-header">
                <h3>{{ reminder.title }}</h3>
                <span 
                  class="reminder-status" 
                  :class="{ 'status-active': !isExpired(reminder), 'status-expired': isExpired(reminder) }"
                >
                  {{ isExpired(reminder) ? '已过期' : '活跃' }}
                </span>
              </div>
              <div class="reminder-details">
                <p class="description">{{ getHumanReadableDescription(reminder) }}</p>
                <p class="date-range">
                  <span class="label">有效期: </span>
                  <span>{{ formatDateRange(reminder) }}</span>
                </p>
                <p class="next-trigger">
                  <span class="label">下次触发: </span>
                  <span>{{ getNextTriggerTime(reminder) }}</span>
                </p>
              </div>
            </div>
            <div class="reminder-actions">
              <button @click="editReminder(reminder)" class="edit-btn">
                <span class="icon">✏️</span> 编辑
              </button>
              <button @click="deleteReminder(reminder.id)" class="delete-btn">
                <span class="icon">🗑️</span> 删除
              </button>
            </div>
          </div>
        </div>
      </div>
      
      <div class="modal-actions">
        <button @click="createNewReminder" class="primary-btn">新建复杂提醒</button>
        <button @click="closeModal" class="cancel-btn">关闭</button>
      </div>
    </div>
  </div>
  
  <!-- 添加删除确认弹窗 -->
  <ConfirmDialog
    :show="showDeleteConfirm"
    title="删除复杂提醒"
    message="确定要删除这个复杂提醒吗？此操作无法撤销。"
    confirm-text="删除"
    cancel-text="取消"
    @confirm="confirmDelete"
    @cancel="cancelDelete"
  />
</template>

<script setup>
import { ref, computed, onMounted } from '../utils/imports.js';
import { reminderState } from '../services/store';
// 使用按需导入的工具函数
import { cronToString, parseCronExpression } from '../utils/imports.js';
import ConfirmDialog from './ConfirmDialog.vue';

// 定义props
const props = defineProps({
  show: {
    type: Boolean,
    required: true
  }
});

// 定义emit
const emit = defineEmits(['close', 'edit', 'delete', 'create']);

// 本地状态
const searchText = ref('');
const filterStatus = ref('all');
const complexReminders = computed(() => reminderState.complexReminders || []);

// 过滤后的提醒列表
const filteredReminders = computed(() => {
  return complexReminders.value.filter(reminder => {
    // 标题搜索过滤
    const titleMatch = reminder.title.toLowerCase().includes(searchText.value.toLowerCase());
    
    // 状态过滤
    let statusMatch = true;
    if (filterStatus.value === 'active') {
      statusMatch = !isExpired(reminder);
    } else if (filterStatus.value === 'expired') {
      statusMatch = isExpired(reminder);
    }
    
    return titleMatch && statusMatch;
  });
});

// 判断提醒是否已过期
function isExpired(reminder) {
  if (!reminder.validUntil) return false;
  
  const now = new Date();
  const expiryDate = new Date(reminder.validUntil);
  
  return now > expiryDate;
}

// 获取提醒的可读描述
function getHumanReadableDescription(reminder) {
  try {
    // 使用按需导入的cronToString函数解析cron表达式
    return cronToString(reminder.cronExpression, { locale: 'zh_CN' });
  } catch (error) {
    console.error('解析cron表达式出错:', error);
    return reminder.cronExpression || '未知触发条件';
  }
}

// 格式化日期范围
function formatDateRange(reminder) {
  const startDate = reminder.validFrom ? new Date(reminder.validFrom).toLocaleDateString('zh-CN') : '无起始日期';
  const endDate = reminder.validUntil ? new Date(reminder.validUntil).toLocaleDateString('zh-CN') : '无结束日期';
  
  return `${startDate} 至 ${endDate}`;
}

// 获取下次触发时间
function getNextTriggerTime(reminder) {
  if (isExpired(reminder)) {
    return '已过期，不会再触发';
  }
  
  try {
    const now = new Date();
    const cronExpression = reminder.cronExpression;
    
    // 简单解析cron表达式 (格式: 分钟 小时 日期 月份 星期)
    const parts = cronExpression.split(' ');
    if (parts.length !== 5) {
      return '无效的cron表达式';
    }
    
    const minute = parts[0];
    const hour = parts[1]; 
    
    // 创建一个表示下一次可能的触发时间的日期对象
    const nextDate = new Date(now);
    
    // 设置小时和分钟
    nextDate.setHours(parseInt(hour, 10));
    nextDate.setMinutes(parseInt(minute, 10));
    nextDate.setSeconds(0);
    nextDate.setMilliseconds(0);
    
    // 如果下一次触发时间已经过去，则加一天
    if (nextDate <= now) {
      nextDate.setDate(nextDate.getDate() + 1);
    }
    
    // 格式化时间输出
    return nextDate.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });
  } catch (error) {
    console.error('计算下次触发时间出错:', error);
    return '无法计算';
  }
}

// 添加删除确认弹窗的状态
const showDeleteConfirm = ref(false);
const reminderToDelete = ref(null);

// 编辑提醒
function editReminder(reminder) {
  emit('edit', reminder);
  closeModal();
}

// 删除提醒 - 修改为显示确认弹窗
function deleteReminder(id) {
  // 设置要删除的提醒ID
  reminderToDelete.value = id;
  // 显示确认弹窗
  showDeleteConfirm.value = true;
}

// 确认删除提醒
function confirmDelete() {
  if (reminderToDelete.value) {
    emit('delete', reminderToDelete.value);
    showDeleteConfirm.value = false;
    reminderToDelete.value = null;
  }
}

// 取消删除
function cancelDelete() {
  showDeleteConfirm.value = false;
  reminderToDelete.value = null;
}

// 创建新提醒
function createNewReminder() {
  emit('create');
  closeModal();
}

// 关闭模态框
function closeModal() {
  emit('close');
}

// 组件挂载时执行的逻辑
onMounted(() => {
  console.log('ComplexReminderListModal mounted, showing reminders:', complexReminders.value.length);
});
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 3000;
}

.modal-content {
  width: 700px;
  max-width: 90%;
  max-height: 80vh;
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  padding: 24px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

h2 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #333;
  text-align: center;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 12px;
}

.search-box {
  flex: 1;
}

.search-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.filter-select {
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background-color: white;
  font-size: 14px;
  min-width: 120px;
}

.reminder-list {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 16px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  color: #888;
  font-size: 16px;
  text-align: center;
  border: 1px dashed #ddd;
  border-radius: 8px;
}

.reminder-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reminder-item {
  display: flex;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.2s ease;
}

.reminder-item:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.reminder-item.expired {
  opacity: 0.7;
}

.reminder-content {
  flex: 1;
  padding: 16px;
  cursor: pointer;
}

.reminder-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.reminder-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.reminder-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 500;
}

.status-active {
  background-color: #e3f2fd;
  color: #1976d2;
}

.status-expired {
  background-color: #fbe9e7;
  color: #d32f2f;
}

.reminder-details {
  color: #666;
  font-size: 14px;
}

.reminder-details p {
  margin: 6px 0;
}

.reminder-details .label {
  color: #888;
}

.reminder-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-left: 1px solid #eee;
  background-color: #fafafa;
}

.reminder-actions button {
  border: none;
  background: none;
  padding: 12px 16px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  transition: background-color 0.2s;
}

.reminder-actions button:hover {
  background-color: #f0f0f0;
}

.edit-btn {
  color: #1976d2;
}

.delete-btn {
  color: #d32f2f;
}

.icon {
  margin-right: 6px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.primary-btn, .cancel-btn {
  padding: 10px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.primary-btn {
  background-color: var(--theme-primary-color, #4CAF50);
  color: white;
}

.primary-btn:hover {
  background-color: var(--theme-hover-color, #45a049);
}

.cancel-btn {
  background-color: #f5f5f5;
  color: #333;
}

.cancel-btn:hover {
  background-color: #e0e0e0;
}

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
  }
  
  .reminder-item {
    flex-direction: column;
  }
  
  .reminder-actions {
    flex-direction: row;
    border-left: none;
    border-top: 1px solid #eee;
  }
  
  .reminder-actions button {
    flex: 1;
    justify-content: center;
  }
}
</style> 