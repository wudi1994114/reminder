<script setup>
import { ref, watch, computed } from 'vue';
import { createEvent, updateEvent, deleteEvent } from '../services/api';
import { showNotification } from '../services/store';
import ConfirmDialog from './ConfirmDialog.vue';

// Props
const props = defineProps({
  show: {
    type: Boolean,
    required: true
  },
  eventData: {
    type: Object,
    default: () => {
      const now = new Date();
      const year = now.getFullYear();
      const month = String(now.getMonth() + 1).padStart(2, '0');
      const day = String(now.getDate()).padStart(2, '0');
      const formattedDate = `${year}-${month}-${day}`;
      
      return {
        title: '',
        description: '',
        reminderType: 'EMAIL',
        selectedDate: formattedDate,
        selectedHour: now.getHours(),
        selectedMinute: now.getMinutes(),
        selectedSecond: 0
      };
    }
  },
  isEditing: {
    type: Boolean,
    default: false
  }
});

// Emits
const emit = defineEmits(['close', 'save', 'delete']);

// Local reactive state for the form
const localEventData = ref({...props.eventData});
const loading = ref(false);

// 日期选择器数据
const selectedYear = ref(new Date().getFullYear());
const selectedMonth = ref(new Date().getMonth() + 1);
const selectedDay = ref(new Date().getDate());

// 生成年份选项：当前年份前后10年
const yearOptions = computed(() => {
  const currentYear = new Date().getFullYear();
  const years = [];
  for (let i = currentYear - 10; i <= currentYear + 10; i++) {
    years.push(i);
  }
  return years;
});

// 生成月份选项：1-12月
const monthOptions = computed(() => {
  return Array.from({ length: 12 }, (_, i) => i + 1);
});

// 生成日期选项：根据年月计算当月天数
const dayOptions = computed(() => {
  const daysInMonth = new Date(selectedYear.value, selectedMonth.value, 0).getDate();
  return Array.from({ length: daysInMonth }, (_, i) => i + 1);
});

// 监听年月日变化，更新selectedDate
watch([selectedYear, selectedMonth, selectedDay], ([year, month, day]) => {
  const formattedMonth = String(month).padStart(2, '0');
  const formattedDay = String(day).padStart(2, '0');
  localEventData.value.selectedDate = `${year}-${formattedMonth}-${formattedDay}`;
}, { immediate: true });

// 从selectedDate解析年月日
const parseSelectedDate = () => {
  if (localEventData.value.selectedDate) {
    const [year, month, day] = localEventData.value.selectedDate.split('-');
    selectedYear.value = parseInt(year);
    selectedMonth.value = parseInt(month);
    selectedDay.value = parseInt(day);
  }
};

// Watch the eventData prop to reset the form
watch(() => props.eventData, (newVal) => {
  console.log('EventModal: eventData变化', newVal);
  
  // 为null或undefined时给默认值
  if (!newVal) {
    console.log('EventModal: 收到空的事件数据，使用默认值');
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const formattedDate = `${year}-${month}-${day}`;
    
    localEventData.value = {
      title: '',
      description: '',
      reminderType: 'EMAIL',
      selectedDate: formattedDate,
      selectedHour: now.getHours(),
      selectedMinute: now.getMinutes(),
      selectedSecond: 0,
      recurrenceType: 'NONE',
      intervalValue: 1,
      intervalUnit: 'DAYS'
    };
    
    // 设置年月日选择器
    selectedYear.value = now.getFullYear();
    selectedMonth.value = now.getMonth() + 1;
    selectedDay.value = now.getDate();
    
    return;
  }
  
  // 打印详细的属性值，帮助调试
  console.log('EventModal: 收到新的事件数据, 属性列表:');
  console.log('- id:', newVal.id);
  console.log('- title:', newVal.title);
  console.log('- description:', newVal.description);
  console.log('- reminderType:', newVal.reminderType);
  console.log('- selectedDate:', newVal.selectedDate);
  console.log('- selectedHour:', newVal.selectedHour);
  console.log('- selectedMinute:', newVal.selectedMinute);
  
  try {
    // 确保对象是干净的副本，使用JSON解析确保没有引用关系
    const cleanData = JSON.parse(JSON.stringify(newVal));
    
    // 获取当前时间作为默认值
    const now = new Date();
    
    // 确保所有必要的字段都有值
    const dataWithDefaults = {
      id: cleanData.id,
      title: cleanData.title || '',
      description: cleanData.description || '',
      reminderType: cleanData.reminderType || 'EMAIL',
      selectedDate: cleanData.selectedDate || (() => {
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
      })(),
      // 如果是编辑模式且有原始时间，则保留原始时间，否则使用当前时间
      selectedHour: props.isEditing && typeof cleanData.selectedHour === 'number' ? 
        cleanData.selectedHour : now.getHours(),
      selectedMinute: props.isEditing && typeof cleanData.selectedMinute === 'number' ? 
        cleanData.selectedMinute : now.getMinutes(),
      selectedSecond: props.isEditing && typeof cleanData.selectedSecond === 'number' ? 
        cleanData.selectedSecond : 0,
      recurrenceType: cleanData.recurrenceType || 'NONE',
      intervalValue: cleanData.intervalValue || 1,
      intervalUnit: cleanData.intervalUnit || 'DAYS'
    };
    
    // 更新本地数据
    localEventData.value = dataWithDefaults;
    console.log('EventModal: 更新后的本地数据:', localEventData.value);
    
    // 解析selectedDate到年月日选择器
    parseSelectedDate();
  } catch (error) {
    console.error('EventModal: 处理事件数据时出错:', error);
    // 出错时使用默认值
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const formattedDate = `${year}-${month}-${day}`;
    
    localEventData.value = {
      title: newVal.title || '',
      description: newVal.description || '',
      reminderType: newVal.reminderType || 'EMAIL',
      selectedDate: newVal.selectedDate || formattedDate,
      // 总是使用当前时间作为默认值
      selectedHour: now.getHours(),
      selectedMinute: now.getMinutes(),
      selectedSecond: 0,
      recurrenceType: 'NONE',
      intervalValue: 1,
      intervalUnit: 'DAYS'
    };
    
    // 设置年月日选择器
    selectedYear.value = now.getFullYear();
    selectedMonth.value = now.getMonth() + 1;
    selectedDay.value = now.getDate();
  }
}, { deep: true, immediate: true });

// 表单验证状态
const errors = ref({
  title: ''
});

// 添加确认弹窗状态
const showDeleteConfirm = ref(false);

// Submit handler
const handleSubmit = async (e) => {
  e.preventDefault();
  console.log('EventModal: 提交表单，数据:', localEventData.value);
  
  // 重置错误信息
  errors.value = {
    title: ''
  };
  
  // 验证标题不能为空
  if (!localEventData.value.title || localEventData.value.title.trim() === '') {
    errors.value.title = '标题不能为空';
    showNotification('请输入事件标题！', 'error');
    return;
  }

  loading.value = true;
  try {
    // 不再直接调用API，而是通过emit把数据传给父组件
    console.log('EventModal: 通过emit将数据传给父组件处理');
    emit('save', localEventData.value);
    
    // 父组件会负责关闭模态框，但我们仍然可以设置loading状态
    loading.value = false;
  } catch (error) {
    console.error('EventModal: 提交表单时出错', error);
    showNotification('提交表单时发生错误', 'error');
    loading.value = false;
  }
};

// Delete handler
const handleDelete = async () => {
  if (!localEventData.value.id) {
    console.error('EventModal: 没有ID，无法删除');
    showNotification('无法删除：缺少ID', 'error');
    return;
  }
  
  // 显示确认弹窗，而不是使用confirm
  showDeleteConfirm.value = true;
};

// 确认删除事件处理函数
const confirmDelete = async () => {
  console.log('EventModal: 确认删除事件，ID:', localEventData.value.id);
  loading.value = true;
  
  try {
    // 通过emit将删除请求传给父组件
    emit('delete', localEventData.value.id);
    loading.value = false;
    // 关闭确认弹窗
    showDeleteConfirm.value = false;
  } catch (error) {
    console.error('EventModal: 删除事件时出错', error);
    showNotification('删除失败', 'error');
    loading.value = false;
    // 关闭确认弹窗
    showDeleteConfirm.value = false;
  }
};

// 取消删除事件处理函数
const cancelDelete = () => {
  showDeleteConfirm.value = false;
};

// Close handler
const closeModal = () => {
  console.log('EventModal: Closing modal');
  emit('close');
};

</script>

<template>
  <div v-if="show" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content">
      <h2>{{ isEditing ? '编辑事件' : '创建新事件' }}</h2>
      <form @submit.prevent="handleSubmit">
        <div class="form-group" :class="{ 'has-error': errors.title }">
          <label for="eventTitle">标题:<span class="required">*</span></label>
          <input type="text" id="eventTitle" v-model="localEventData.title" :class="{ 'input-error': errors.title }">
          <div v-if="errors.title" class="error-message">{{ errors.title }}</div>
        </div>
        <div class="form-group">
          <label for="eventDesc">描述:</label>
          <textarea id="eventDesc" v-model="localEventData.description"></textarea>
        </div>
        <div class="form-group">
          <label for="reminderType">提醒方式:</label>
          <select id="reminderType" v-model="localEventData.reminderType">
            <option value="EMAIL">邮件</option>
            <option value="SMS" disabled>短信 (暂不可用)</option>
          </select>
        </div>
        <div class="form-group date-selector">
          <label>日期:</label>
          <div class="date-pickers">
            <select v-model.number="selectedYear" class="date-picker">
              <option v-for="year in yearOptions" :key="`year-${year}`" :value="year">{{ year }}</option>
            </select>
            <span>年</span>
            <select v-model.number="selectedMonth" class="date-picker">
              <option v-for="month in monthOptions" :key="`month-${month}`" :value="month">{{ month }}</option>
            </select>
            <span>月</span>
            <select v-model.number="selectedDay" class="date-picker">
              <option v-for="day in dayOptions" :key="`day-${day}`" :value="day">{{ day }}</option>
            </select>
            <span>日</span>
          </div>
        </div>
        <div class="form-group time-selector">
          <label>时间:</label>
          <div class="time-pickers">
            <select v-model.number="localEventData.selectedHour" class="time-picker">
              <option v-for="h in 24" :key="'h'+h" :value="h-1">{{ (h-1).toString().padStart(2, '0') }}</option>
            </select>
            <span>时</span>
            <select v-model.number="localEventData.selectedMinute" class="time-picker">
              <option v-for="m in 60" :key="'m'+m" :value="m-1">{{ (m-1).toString().padStart(2, '0') }}</option>
            </select>
            <span>分</span>
            <select v-model.number="localEventData.selectedSecond" class="time-picker">
              <option v-for="s in 60" :key="'s'+s" :value="s-1">{{ (s-1).toString().padStart(2, '0') }}</option>
            </select>
            <span>秒</span>
          </div>
        </div>
        <div class="form-group">
          <label for="recurrenceType">重复:</label>
          <select id="recurrenceType" v-model="localEventData.recurrenceType">
            <option value="NONE">无</option>
            <option value="DAILY">每天</option>
            <option value="WEEKLY">每周</option>
            <option value="MONTHLY">每月</option>
            <option value="YEARLY">每年</option>
            <option value="INTERVAL">按间隔</option>
          </select>
        </div>
        <div v-if="localEventData.recurrenceType === 'INTERVAL'" class="form-group interval-settings">
          <label>间隔:</label>
          <input type="number" v-model.number="localEventData.intervalValue" min="1" style="width: 60px;">
          <select v-model="localEventData.intervalUnit">
            <option value="HOURS">小时</option>
            <option value="DAYS">天</option>
            <option value="WEEKS">周</option>
            <option value="MONTHS">月</option>
          </select>
        </div>
        
        <div class="modal-actions">
          <button type="submit" class="button primary">{{ isEditing ? '保存' : '创建' }}</button>
          <button v-if="isEditing" type="button" class="button danger" @click="handleDelete">删除</button>
          <button type="button" class="button" @click="closeModal">取消</button>
        </div>
      </form>
    </div>
  </div>
  
  <!-- 添加删除确认弹窗 -->
  <ConfirmDialog
    :show="showDeleteConfirm"
    title="删除提醒"
    message="确定要删除这个提醒事项吗？此操作无法撤销。"
    confirm-text="删除"
    cancel-text="取消"
    @confirm="confirmDelete"
    @cancel="cancelDelete"
  />
</template>

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
  min-width: 450px;
  max-height: 90vh;
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
  margin-bottom: 25px;
  color: #555; 
  font-weight: 700; 
  text-align: center;
  font-family: 'Nunito', sans-serif;
}

.modal-content .form-group {
  margin-bottom: 18px;
}

.modal-content label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600; 
  color: #666;
  font-family: 'Nunito', sans-serif;
}

.modal-content input[type="text"],
.modal-content input[type="number"],
.modal-content textarea,
.modal-content select {
  width: 100%; 
  padding: 12px 15px; 
  border-radius: 8px; 
  box-sizing: border-box; 
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  background-color: #fff; 
  border: 1px solid #e0e6ed; 
  color: #212529; 
  font-family: 'Nunito', sans-serif;
}

.modal-content input[type="text"]:focus,
.modal-content input[type="number"]:focus,
.modal-content textarea:focus,
.modal-content select:focus {
    border-color: var(--theme-primary-color, #a0c4ff); 
    box-shadow: 0 0 0 2px color-mix(in srgb, var(--theme-primary-color, #81c7f5) 20%, transparent);
    outline: none;
}

.modal-content textarea {
  min-height: 90px;
  resize: vertical;
}

.modal-actions {
  margin-top: 30px;
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

.time-selector {
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 5px;
  border: 1px solid var(--theme-primary-color);
  box-shadow: 0 0 5px rgba(0, 0, 0, 0.05);
}

.time-selector label {
  margin-right: 5px;
  flex-shrink: 0;
}
.time-selector select {
  flex: 1 1 auto;
  min-width: 60px;
  text-align: center;
  font-size: 1em;
  padding: 8px 5px;
  width: auto;
}
.time-selector span { 
  font-weight: bold;
  color: #b0b0b0; 
  margin: 0 2px;
}

.interval-settings {
    display: flex;
    align-items: center;
    gap: 10px;
}
.interval-settings label {
    flex-shrink: 0;
}
.interval-settings input[type="number"] {
    width: 70px; 
    flex-shrink: 0;
    padding: 8px 5px;
}
.interval-settings select {
    flex: 1 1 auto;
    padding: 8px 5px;
    width: auto;
}

.modal-actions button.danger {
  background-color: #f44336;
  color: white;
}
.modal-actions button.danger:hover {
  background-color: #d32f2f;
}

/* 自定义下拉选择器样式 */
.date-selector .date-pickers {
  display: flex;
  align-items: center;
  gap: 5px;
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  padding: 8px 12px;
  border: 1px solid var(--theme-primary-color);
  box-shadow: 0 0 5px rgba(0, 0, 0, 0.05);
}

.date-selector select.date-picker {
  -webkit-appearance: none;
  -moz-appearance: none;
  appearance: none;
  background: transparent;
  border: none;
  font-size: 1.1em;
  font-weight: 500;
  text-align: center;
  padding: 10px 5px;
  cursor: pointer;
  color: #333;
  outline: none;
  min-width: 60px;
  flex: 1;
  scrollbar-width: none; /* Firefox */
}

.date-selector select.date-picker::-webkit-scrollbar {
  display: none; /* Chrome, Safari */
}

.date-selector select.date-picker:hover {
  background-color: var(--theme-day-hover-bg, rgba(0, 0, 0, 0.05));
  border-radius: 4px;
}

.date-selector select.date-picker:focus {
  background-color: var(--theme-hover-color, rgba(0, 0, 0, 0.1));
  border-radius: 4px;
  color: white;
}

.date-selector span {
  color: var(--theme-primary-color, #666);
  font-weight: 500;
}

/* 使时间选择器与日期选择器样式一致 */
.time-selector {
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 5px;
  border: 1px solid var(--theme-primary-color);
  box-shadow: 0 0 5px rgba(0, 0, 0, 0.05);
}

.time-selector .time-pickers {
  display: flex;
  align-items: center;
  gap: 5px;
  flex: 1;
}

.time-selector select.time-picker {
  -webkit-appearance: none;
  -moz-appearance: none;
  appearance: none;
  background: transparent;
  border: none;
  font-size: 1.1em;
  font-weight: 500;
  text-align: center;
  padding: 10px 5px;
  cursor: pointer;
  color: #333;
  outline: none;
  min-width: 60px;
  flex: 1;
  scrollbar-width: none; /* Firefox */
}

.time-selector select.time-picker::-webkit-scrollbar {
  display: none; /* Chrome, Safari */
}

.time-selector select.time-picker:hover {
  background-color: var(--theme-day-hover-bg, rgba(0, 0, 0, 0.05));
  border-radius: 4px;
}

.time-selector select.time-picker:focus {
  background-color: var(--theme-hover-color, rgba(0, 0, 0, 0.1));
  border-radius: 4px;
  color: white;
}

.time-selector span {
  color: var(--theme-primary-color, #666);
  font-weight: 500;
}

/* iOS风格滑动选择器效果 */
@media (max-width: 768px) {
  .date-selector .date-pickers,
  .time-selector {
    border-radius: 16px;
    padding: 10px;
    background: rgba(255, 255, 255, 0.9);
    overflow: hidden;
    box-shadow: inset 0 0 8px rgba(0, 0, 0, 0.1), 0 0 0 1px var(--theme-primary-color);
    border-color: var(--theme-primary-color);
    position: relative;
  }
  
  /* 添加主题色渐变背景 */
  .date-selector .date-pickers::after,
  .time-selector::after {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    top: 0;
    bottom: 0;
    background: linear-gradient(to bottom, 
      var(--theme-primary-color) 0%, 
      transparent 10%, 
      transparent 90%, 
      var(--theme-primary-color) 100%);
    opacity: 0.05;
    pointer-events: none;
    z-index: 0;
    border-radius: 16px;
  }
  
  .date-selector select.date-picker,
  .time-selector select.time-picker {
    padding: 15px 5px;
    font-size: 1.2em;
    position: relative;
    z-index: 2;
  }
  
  /* 添加高亮选中效果 */
  .date-selector,
  .time-selector {
    position: relative;
  }
  
  .date-selector::before,
  .time-selector::before {
    content: '';
    position: absolute;
    left: 10%;
    right: 10%;
    top: 50%;
    height: 38px;
    transform: translateY(-50%);
    border-top: 1px solid var(--theme-primary-color);
    border-bottom: 1px solid var(--theme-primary-color);
    opacity: 0.3;
    pointer-events: none;
    z-index: 1;
    border-radius: 4px;
  }
}

.form-group.has-error label {
  color: #f44336;
}

.input-error {
  border-color: #f44336 !important;
  box-shadow: 0 0 0 1px rgba(244, 67, 54, 0.25) !important;
}

.error-message {
  color: #f44336;
  font-size: 12px;
  margin-top: 4px;
}

.required {
  color: #f44336;
  margin-left: 2px;
}

.complex-reminder-button {
  margin-top: 20px;
}

.button.full-width {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #f0f4f9;
  color: #4a90e2;
  border: 1px dashed #4a90e2;
  transition: all 0.2s;
}

.button.full-width:hover {
  background-color: #e6effc;
}

.button-icon {
  font-weight: bold;
}
</style> 