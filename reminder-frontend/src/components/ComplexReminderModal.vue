<template>
  <div v-if="show" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content">
      <h2>复杂提醒设置</h2>
      
      <!-- 顶部：提醒基本信息 -->
      <div class="section reminder-basic-info">
        <h3>提醒基本信息</h3>
        <div class="form-group">
          <label for="eventTitle">标题:<span class="required">*</span></label>
          <input type="text" id="eventTitle" v-model="reminderData.title" class="form-control">
        </div>
        <div class="form-group">
          <label for="eventDesc">描述:</label>
          <textarea id="eventDesc" v-model="reminderData.description" class="form-control"></textarea>
        </div>
        <div class="form-group">
          <label for="reminderType">提醒方式:</label>
          <select id="reminderType" v-model="reminderData.reminderType" class="form-control">
            <option value="EMAIL">邮件</option>
            <option value="SMS" disabled>短信 (暂不可用)</option>
          </select>
        </div>
      </div>
      
      <!-- 中间：时间模式选择器 -->
      <div class="section time-mode-selector">
        <h3>时间设置</h3>
        <div class="tabs">
          <div class="tab-buttons">
            <button 
              :class="['tab-button', { active: activeTab === 'simple' }]" 
              @click="activeTab = 'simple'"
            >
              简易模式
            </button>
            <button 
              :class="['tab-button', { active: activeTab === 'advanced' }]" 
              @click="activeTab = 'advanced'"
            >
              高级模式
            </button>
          </div>
          
          <div class="tab-content">
            <!-- 简易模式 -->
            <div v-if="activeTab === 'simple'" class="tab-pane">
              <div class="form-row">
                <div class="form-group recurrence-type">
                  <label for="recurrenceType">重复类型:</label>
                  <select id="recurrenceType" v-model="reminderData.recurrenceType" class="form-control">
                    <option value="DAILY">每天</option>
                    <option value="WEEKLY">每周</option>
                    <option value="MONTHLY">每月</option>
                    <option value="YEARLY">每年</option>
                  </select>
                </div>
                
                <div class="form-group time-selector">
                  <label>时间:</label>
                  <div class="time-inputs">
                    <select v-model="reminderData.hour" class="form-control time-input">
                      <option v-for="hour in 24" :key="`hour-${hour-1}`" :value="hour-1">
                        {{ String(hour-1).padStart(2, '0') }}
                      </option>
                    </select>
                    <span class="time-separator">:</span>
                    <select v-model="reminderData.minute" class="form-control time-input">
                      <option v-for="minute in 60" :key="`minute-${minute-1}`" :value="minute-1">
                        {{ String(minute-1).padStart(2, '0') }}
                      </option>
                    </select>
                  </div>
                </div>
              </div>
              
              <!-- 重复类型为每周时，选择星期几 -->
              <div v-if="reminderData.recurrenceType === 'WEEKLY'" class="form-group">
                <label>重复的星期:</label>
                <div class="weekday-selector">
                  <label v-for="(day, index) in weekDays" :key="index" class="weekday-option">
                    <input 
                      type="radio" 
                      :value="index" 
                      v-model="reminderData.weekday" 
                      name="weekday"
                    >
                    {{ day }}
                  </label>
                </div>
              </div>
              
              <!-- 重复类型为每月时，选择日期 -->
              <div v-if="reminderData.recurrenceType === 'MONTHLY'" class="form-group">
                <label>每月的哪一天:</label>
                <select v-model="reminderData.dayOfMonth" class="form-control">
                  <option v-for="day in 31" :key="day" :value="day">{{ day }}</option>
                </select>
              </div>
              
              <!-- 重复类型为每年时，选择月份和日期 -->
              <div v-if="reminderData.recurrenceType === 'YEARLY'" class="form-group">
                <label>每年的哪一天:</label>
                <div class="year-date-selector">
                  <select v-model="reminderData.month" class="form-control">
                    <option v-for="(month, index) in months" :key="index" :value="index + 1">
                      {{ month }}
                    </option>
                  </select>
                  <select v-model="reminderData.dayOfMonth" class="form-control">
                    <option v-for="day in getDaysInMonth(reminderData.month)" :key="day" :value="day">
                      {{ day }}
                    </option>
                  </select>
                </div>
              </div>
            </div>
            
            <!-- 高级模式 -->
            <div v-if="activeTab === 'advanced'" class="tab-pane">
              <div class="cron-editor">
                <div class="cron-fields">
                  <div class="form-group">
                    <label>分钟 (0-59):</label>
                    <input 
                      type="text" 
                      v-model="cronExpression.minute" 
                      class="form-control"
                      @input="validateCronField($event, 'minute', 0, 59)"
                      placeholder="例如: 0,15,30,45 或 */15"
                    >
                    <div v-if="cronErrors.minute" class="field-error">{{ cronErrors.minute }}</div>
                  </div>
                  <div class="form-group">
                    <label>小时 (0-23):</label>
                    <input 
                      type="text" 
                      v-model="cronExpression.hour" 
                      class="form-control"
                      @input="validateCronField($event, 'hour', 0, 23)"
                      placeholder="例如: 8,12,18 或 */2"
                    >
                    <div v-if="cronErrors.hour" class="field-error">{{ cronErrors.hour }}</div>
                  </div>
                  <div class="form-group">
                    <label>日期 (1-31):
                      <span v-if="isDayDisabled" class="field-note">(与星期冲突)</span>
                    </label>
                    <input 
                      type="text" 
                      v-model="cronExpression.day" 
                      class="form-control"
                      :class="{ 'field-disabled': isDayDisabled }"
                      @focus="handleDayFocus"
                      @input="validateCronField($event, 'day', 1, 31)"
                      placeholder="例如: 1,15 或 */5"
                    >
                    <div v-if="cronErrors.day" class="field-error">{{ cronErrors.day }}</div>
                  </div>
                  <div class="form-group">
                    <label>月份 (1-12):</label>
                    <input 
                      type="text" 
                      v-model="cronExpression.month" 
                      class="form-control"
                      @input="validateCronField($event, 'month', 1, 12)"
                      placeholder="例如: 1,6,12 或 */3"
                    >
                    <div v-if="cronErrors.month" class="field-error">{{ cronErrors.month }}</div>
                  </div>
                  <div class="form-group">
                    <label>星期 (0-6):
                      <span v-if="isWeekdayDisabled" class="field-note">(与日期冲突)</span>
                    </label>
                    <input 
                      type="text" 
                      v-model="cronExpression.weekday" 
                      class="form-control"
                      :class="{ 'field-disabled': isWeekdayDisabled }"
                      @focus="handleWeekdayFocus"
                      @input="validateCronField($event, 'weekday', 0, 6)"
                      placeholder="例如: 1,3,5 或 1-5"
                    >
                    <div v-if="cronErrors.weekday" class="field-error">{{ cronErrors.weekday }}</div>
                  </div>
                </div>
                <div class="cron-help">
                  <p>支持特殊符号: * (任意), , (列表), - (范围), / (间隔)</p>
                  <p>说明: 日期和星期通常不应同时指定具体值</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 时间段设置 -->
      <div class="time-range-settings">
        <h4>时间段设置</h4>
        <div class="time-range-row">
          <div class="form-group">
            <label>生效日期:</label>
            <input type="date" v-model="reminderData.validFrom" class="form-control">
          </div>
          <div class="form-group">
            <label>失效日期 (可选):</label>
            <input type="date" v-model="reminderData.validUntil" class="form-control">
          </div>
          <div class="form-group">
            <label>最多执行次数:</label>
            <input type="number" v-model.number="reminderData.maxExecutions" min="1" class="form-control">
          </div>
        </div>
      </div>
      
      <!-- 底部：预览区域 -->
      <div class="section preview-area">
        <h3>触发时间预览</h3>
        <div class="next-executions">
          <p>下次将在这些时间触发:</p>
          <ul class="execution-times">
            <template v-if="previewTimes && previewTimes.length > 0">
              <li v-for="(time, index) in previewTimes" :key="'preview-time-' + index">
              {{ time }}
            </li>
            </template>
            <li v-else class="no-preview">暂无预览时间</li>
          </ul>
        </div>
        
        <div class="human-readable">
          <p>描述: {{ humanReadableDescription }}</p>
        </div>
      </div>
      
      <!-- 按钮区域 -->
      <div class="modal-actions">
        <button type="button" class="button primary" @click="saveReminder">保存</button>
        <button type="button" class="button" @click="closeModal">取消</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';

// Props
const props = defineProps({
  show: {
    type: Boolean,
    required: true
  },
  initialData: {
    type: Object,
    default: () => ({})
  }
});

// Emits
const emit = defineEmits(['close', 'save']);

// 本地状态
const activeTab = ref('simple');
const previewTimes = ref([]); // 确保初始化为空数组
const humanReadableDescription = ref('每天上午8:00'); // 设置默认值
const reminderData = ref({
  title: '',
  description: '',
  reminderType: 'EMAIL',
  recurrenceType: 'DAILY',
  validFrom: new Date().toISOString().split('T')[0],
  validUntil: '',
  maxExecutions: null,
  weekday: 1, // 默认周一
  dayOfMonth: 1, // 默认每月1号
  month: 1,  // 默认1月
  hour: 8,   // 默认上午8点
  minute: 0  // 默认0分
});

// 星期几数组
const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

// 月份数组
const months = [
  '一月', '二月', '三月', '四月', '五月', '六月',
  '七月', '八月', '九月', '十月', '十一月', '十二月'
];

// 获取指定月份的天数
const getDaysInMonth = (month) => {
  // 默认31天
  if (!month) return 31;
  
  // 2月特殊处理
  if (month === 2) {
    const year = new Date().getFullYear();
    // 闰年判断
    if ((year % 4 === 0 && year % 100 !== 0) || year % 400 === 0) {
      return 29;
    } else {
      return 28;
    }
  }
  
  // 30天的月份
  if ([4, 6, 9, 11].includes(month)) {
    return 30;
  }
  
  // 其他31天
  return 31;
};

// Cron 表达式
const cronExpression = ref({
  minute: '0',
  hour: '0',
  day: '*',
  month: '*',
  weekday: '*'
});

// 存储Cron字段验证错误信息
const cronErrors = ref({
  minute: '',
  hour: '',
  day: '',
  month: '',
  weekday: ''
});

// 计算完整的 Cron 表达式
const displayCronExpression = computed(() => {
  return `${cronExpression.value.minute} ${cronExpression.value.hour} ${cronExpression.value.day} ${cronExpression.value.month} ${cronExpression.value.weekday}`;
});

// 计算日期和星期字段的状态
const isDayDisabled = computed(() => {
  return cronExpression.value.weekday !== '*' && cronExpression.value.weekday !== '?';
});

const isWeekdayDisabled = computed(() => {
  return cronExpression.value.day !== '*' && cronExpression.value.day !== '?';
});

// 处理字段焦点事件
const handleDayFocus = () => {
  if (isDayDisabled.value) {
    // 如果日期字段置灰，则清除星期字段的值
    cronExpression.value.weekday = '*';
  }
};

const handleWeekdayFocus = () => {
  if (isWeekdayDisabled.value) {
    // 如果星期字段置灰，则清除日期字段的值
    cronExpression.value.day = '*';
  }
};

// 监听字段变化，实现互斥关系
watch(() => cronExpression.value.day, (newVal) => {
  // 如果日期字段设置了具体值，星期字段自动变为'?'
  if (newVal !== '*' && newVal !== '?') {
    if (cronExpression.value.weekday !== '*' && cronExpression.value.weekday !== '?') {
      console.log('日期字段已设置，重置星期字段');
      cronExpression.value.weekday = '*';
    }
  }
});

watch(() => cronExpression.value.weekday, (newVal) => {
  // 如果星期字段设置了具体值，日期字段自动变为'?'
  if (newVal !== '*' && newVal !== '?') {
    if (cronExpression.value.day !== '*' && cronExpression.value.day !== '?') {
      console.log('星期字段已设置，重置日期字段');
      cronExpression.value.day = '*';
    }
  }
});



// 更新预览和描述的函数
const updatePreviewAndDescription = () => {
  // 添加调试日志
  console.log('====== 开始更新预览 ======');
  console.log('当前激活的标签:', activeTab.value);
  console.log('时间段设置:', {
    validFrom: reminderData.value.validFrom,
    validUntil: reminderData.value.validUntil,
    maxExecutions: reminderData.value.maxExecutions
  });
  
  // 生成基础预览数据
  let hasGeneratedPreview = false;
  
  try {
    // 如果是简易模式，先确保Cron表达式已同步
    if (activeTab.value === 'simple') {
      // 同步简易模式设置到Cron表达式
      updateCronFromSimpleMode();
    }
    
    // 直接使用Cron表达式生成预览，同时考虑时间段限制
    updatePreviewBasedOnCronExpression();
    hasGeneratedPreview = true;
    
    // 更新人类可读描述
    updateHumanReadableDescription();
    
    // 检查是否生成了预览
    console.log('生成预览结果:', {
      hasGeneratedPreview,
      previewCount: previewTimes.value?.length || 0,
      previewSample: previewTimes.value?.slice(0, 2) || []
    });
  } catch (error) {
    console.error('更新预览时出错:', error);
    previewTimes.value = ['更新预览时出错，请检查设置'];
    humanReadableDescription.value = '无法生成预览，请检查设置';
  }
  
  console.log('====== 预览更新完成 ======');
};

// 监听show变化，方便调试
watch(() => props.show, (newVal) => {
  console.log('ComplexReminderModal: 显示状态变化 =>', newVal);
}, { immediate: true });

// 监听Cron表达式变化
watch(cronExpression, () => {
  console.log('Cron表达式变化为:', displayCronExpression.value);
  if (activeTab.value === 'advanced') {
    updatePreviewAndDescription();
  }
}, { deep: true });

// 监听模式切换
watch(activeTab, (newTab) => {
  console.log('切换到模式:', newTab);
  updatePreviewAndDescription();
});



// 监听时间段设置变化
watch(() => [
  reminderData.value.validFrom, 
  reminderData.value.validUntil, 
  reminderData.value.maxExecutions
], () => {
  // 直接重新基于Cron表达式生成预览，而不是只过滤已有预览
  console.log('时间条件变更，重新生成预览...');
  // 清空预览时间缓存，强制重新生成
  previewTimes.value = [];
  // 重新生成完整预览
  updatePreviewAndDescription();
}, { deep: true });

// 监听简易模式变化
watch(() => reminderData.value.recurrenceType, (newVal) => {
  console.log('重复类型变化为:', newVal);
  updatePreviewAndDescription();
}, { immediate: true });

// 应用时间段过滤函数
const applyTimeRangeFilters = () => {
  // 先保存原始预览时间，以防所有时间都被过滤掉
  const originalPreviewTimes = [...previewTimes.value];
  
  // 调试信息
  console.log('应用时间段过滤前的预览时间:', originalPreviewTimes);
  console.log('当前日期设置:', {
    startDate: reminderData.value.validFrom,
    endDate: reminderData.value.validUntil,
    maxExecutions: reminderData.value.maxExecutions
  });
  
  // 检查是否有预览时间
  if (originalPreviewTimes.length === 0 || 
      (originalPreviewTimes.length === 1 && (
        originalPreviewTimes[0].includes('无效') || 
        originalPreviewTimes[0].includes('无法') || 
        originalPreviewTimes[0].includes('请检查')
      ))) {
    console.log('没有有效的预览时间可过滤');
    return; // 如果没有有效预览时间或只有错误信息，直接返回
  }
  
  // 将日期格式转换为Date对象进行比较
  let previewDates = [];
  
  // 尝试解析每个日期
  for (const timeStr of originalPreviewTimes) {
    try {
      // 首先检查是否为错误消息而非日期
      if (!timeStr.includes('-') || !timeStr.includes(':')) {
        continue;
      }
      
      // 假设timeStr格式为: YYYY-MM-DD HH:MM:SS
      const date = new Date(timeStr);
      if (!isNaN(date.getTime())) {
        previewDates.push(date);
        continue;
      }
      
      // 尝试其他格式
      const parts = timeStr.split(' ');
      if (parts.length >= 2) {
        const datePart = parts[0];
        const timePart = parts[1];
        
        // 尝试解析 YYYY/MM/DD 或 YYYY-MM-DD 格式
        const dateComponents = datePart.split(/[-\/]/);
        if (dateComponents.length === 3) {
          const year = parseInt(dateComponents[0]);
          const month = parseInt(dateComponents[1]) - 1;
          const day = parseInt(dateComponents[2]);
          
          const timeComponents = timePart.split(':');
          const hour = timeComponents.length > 0 ? parseInt(timeComponents[0]) : 0;
          const minute = timeComponents.length > 1 ? parseInt(timeComponents[1]) : 0;
          const second = timeComponents.length > 2 ? parseInt(timeComponents[2]) : 0;
          
          const parsedDate = new Date(year, month, day, hour, minute, second);
          if (!isNaN(parsedDate.getTime())) {
            previewDates.push(parsedDate);
          }
        }
      }
    } catch (e) {
      console.error('解析日期失败:', timeStr, e);
    }
  }
  
  console.log('解析后的预览日期:', previewDates);
  
  // 如果无法解析任何日期，保留原始预览
  if (previewDates.length === 0) {
    console.warn('无法解析任何预览日期，保留原始预览');
    return;
  }
  
  // 应用开始日期过滤
  if (reminderData.value.validFrom) {
    const startDate = new Date(reminderData.value.validFrom);
    startDate.setHours(0, 0, 0, 0);
    console.log('过滤开始日期:', startDate);
    previewDates = previewDates.filter(date => date >= startDate);
  }
  
  // 应用结束日期过滤
  if (reminderData.value.validUntil) {
    const endDate = new Date(reminderData.value.validUntil);
    endDate.setHours(23, 59, 59, 999);
    console.log('过滤结束日期:', endDate);
    previewDates = previewDates.filter(date => date <= endDate);
  }
  
  // 应用最大执行次数限制
  if (reminderData.value.maxExecutions && reminderData.value.maxExecutions > 0) {
    console.log('限制最大执行次数:', reminderData.value.maxExecutions);
    previewDates = previewDates.slice(0, reminderData.value.maxExecutions);
  }
  
  console.log('过滤后的预览日期:', previewDates);
  
  // 如果过滤后没有预览时间，添加提示消息
  if (previewDates.length === 0) {
    previewTimes.value = ['没有符合时间段条件的预览时间'];
    return;
  }
  
  // 转换回格式化的字符串并更新预览
  previewTimes.value = previewDates.map(date => formatDate(date));
  console.log('最终预览时间:', previewTimes.value);
};

// 格式化日期为可读字符串
const formatDate = (date) => {
  try {
    // 确保输入是有效的Date对象
    if (!(date instanceof Date) || isNaN(date.getTime())) {
      console.error('formatDate: 无效的日期对象', date);
      return '无效日期';
    }
    
    // 生成统一格式的日期字符串: YYYY-MM-DD HH:MM:SS
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  } catch (error) {
    console.error('formatDate: 格式化日期时出错', error);
    return '日期格式化错误';
  }
};

// 验证Cron表达式字段
const validateCronField = (event, field, min, max) => {
  const value = event.target.value;
  
  // 如果是空字符串，设置为默认值
  if (value === '') {
    if (field === 'minute' || field === 'hour') {
      cronExpression.value[field] = '0';
    } else {
      cronExpression.value[field] = '*';
    }
    cronErrors.value[field] = '';
    return;
  }
  
  // 允许的特殊字符：*,/-
  const specialChars = ['*', ',', '/', '-'];
  
  // 首先检查是否为单个特殊字符（如*）
  if (value.length === 1 && specialChars.includes(value)) {
    cronErrors.value[field] = '';
    return;
  }
  
  // 检查表达式中的每个数字值是否在范围内
  const numberPattern = /\d+/g;
  const numbers = value.match(numberPattern);
  
  if (numbers) {
    for (const num of numbers) {
      const numValue = parseInt(num);
      if (isNaN(numValue) || numValue < min || numValue > max) {
        cronErrors.value[field] = `数值 ${num} 超出有效范围 (${min}-${max})`;
        
        // 阻止非法输入，恢复之前的值
        event.preventDefault();
        // 获取输入前的值（通过cronExpression）
        event.target.value = cronExpression.value[field];
        return;
      }
    }
  }
  
  // 检查语法是否符合cron规则
  try {
    // 检查是否包含无效字符
    const validPattern = new RegExp(`^[0-9${specialChars.map(c => '\\' + c).join('')}]+$`);
    if (!validPattern.test(value)) {
      cronErrors.value[field] = '包含无效字符';
      event.preventDefault();
      event.target.value = cronExpression.value[field];
      return;
    }
    
    // 检查连续特殊字符
    if (/[,/-]{2,}/.test(value)) {
      cronErrors.value[field] = '不允许连续特殊字符';
      event.preventDefault();
      event.target.value = cronExpression.value[field];
      return;
    }
    
    // 检查不适当的*/
    if (value.includes('*/')) {
      const parts = value.split('/');
      if (parts[0] !== '*' || parts.length > 2) {
        cronErrors.value[field] = '间隔表达式格式无效';
        event.preventDefault();
        event.target.value = cronExpression.value[field];
        return;
      }
    }
    
    // 检查范围表达式
    if (value.includes('-')) {
      const ranges = value.split(',');
      for (const range of ranges) {
        if (range.includes('-')) {
          const [start, end] = range.split('-').map(Number);
          if (isNaN(start) || isNaN(end) || start > end || start < min || end > max) {
            cronErrors.value[field] = '范围表达式无效';
            event.preventDefault();
            event.target.value = cronExpression.value[field];
            return;
          }
        }
      }
    }
    
    // 如果验证通过，清除错误
    cronErrors.value[field] = '';
  } catch (error) {
    console.error('验证cron表达式时出错:', error);
    cronErrors.value[field] = '表达式格式错误';
    event.preventDefault();
    event.target.value = cronExpression.value[field];
    return;
  }
};

// 监听简易模式设置变化并更新Cron表达式
watch(() => [
  reminderData.value.recurrenceType, 
  reminderData.value.weekday,
  reminderData.value.dayOfMonth,
  reminderData.value.month,
  reminderData.value.hour,
  reminderData.value.minute
], () => {
  if (activeTab.value === 'simple') {
    // 更新Cron表达式
    updateCronFromSimpleMode();
    // 更新预览
    updatePreviewAndDescription();
    console.log('简易模式设置已同步到Cron表达式:', displayCronExpression.value);
  }
}, { deep: true });

// 监听模式切换
watch(activeTab, (newTab) => {
  console.log('切换到模式:', newTab);
  
  if (newTab === 'simple') {
    // 从高级模式切换到简易模式，尝试解析Cron表达式到简易模式
    parseCronToSimpleMode();
  } else {
    // 从简易模式切换到高级模式，更新Cron表达式
    updateCronFromSimpleMode();
  }
  
  updatePreviewAndDescription();
});

// 将简易模式设置转换为Cron表达式
const updateCronFromSimpleMode = () => {
  // 设置分钟和小时
  cronExpression.value.minute = String(reminderData.value.minute || 0);
  cronExpression.value.hour = String(reminderData.value.hour || 0);
  
  // 根据重复类型设置日期、月份和星期
  switch (reminderData.value.recurrenceType) {
    case 'DAILY':
      // 每天：日期和月份任意，星期任意
      cronExpression.value.day = '*';
      cronExpression.value.month = '*';
      cronExpression.value.weekday = '*';
      break;
      
    case 'WEEKLY':
      // 每周特定日：日期任意，月份任意，星期特定值
      cronExpression.value.day = '*';
      cronExpression.value.month = '*';
      cronExpression.value.weekday = String(reminderData.value.weekday || 1); // 默认周一
      break;
      
    case 'MONTHLY':
      // 每月特定日：日期特定值，月份任意，星期任意
      cronExpression.value.day = String(reminderData.value.dayOfMonth || 1); // 默认1号
      cronExpression.value.month = '*';
      cronExpression.value.weekday = '*';
      break;
      
    case 'YEARLY':
      // 每年特定日：日期特定值，月份特定值，星期任意
      cronExpression.value.day = String(reminderData.value.dayOfMonth || 1); // 默认1号
      cronExpression.value.month = String(reminderData.value.month || 1); // 默认1月
      cronExpression.value.weekday = '*';
      break;
      
    default:
      // 默认每天
      cronExpression.value.day = '*';
      cronExpression.value.month = '*';
      cronExpression.value.weekday = '*';
  }
};

// 尝试将Cron表达式解析为简易模式设置
const parseCronToSimpleMode = () => {
  try {
    const minute = parseInt(cronExpression.value.minute);
    const hour = parseInt(cronExpression.value.hour);
    
    // 如果分钟和小时是有效数字，则更新简易模式的时间设置
    if (!isNaN(minute) && minute >= 0 && minute < 60) {
      reminderData.value.minute = minute;
    }
    
    if (!isNaN(hour) && hour >= 0 && hour < 24) {
      reminderData.value.hour = hour;
    }
    
    // 判断Cron表达式对应的简易模式类型
    if (cronExpression.value.day === '*' && cronExpression.value.month === '*' && cronExpression.value.weekday !== '*') {
      // 每周某天
      reminderData.value.recurrenceType = 'WEEKLY';
      const weekday = parseInt(cronExpression.value.weekday);
      if (!isNaN(weekday) && weekday >= 0 && weekday <= 6) {
        reminderData.value.weekday = weekday;
      }
    } else if (cronExpression.value.day !== '*' && cronExpression.value.month === '*') {
      // 每月某天
      reminderData.value.recurrenceType = 'MONTHLY';
      const day = parseInt(cronExpression.value.day);
      if (!isNaN(day) && day >= 1 && day <= 31) {
        reminderData.value.dayOfMonth = day;
      }
    } else if (cronExpression.value.day !== '*' && cronExpression.value.month !== '*') {
      // 每年某月某天
      reminderData.value.recurrenceType = 'YEARLY';
      const day = parseInt(cronExpression.value.day);
      const month = parseInt(cronExpression.value.month);
      
      if (!isNaN(day) && day >= 1 && day <= 31) {
        reminderData.value.dayOfMonth = day;
      }
      
      if (!isNaN(month) && month >= 1 && month <= 12) {
        reminderData.value.month = month;
      }
    } else {
      // 默认每天
      reminderData.value.recurrenceType = 'DAILY';
    }
    
    console.log('已将Cron表达式解析为简易模式设置:', reminderData.value);
  } catch (error) {
    console.error('无法解析Cron表达式为简易模式:', error);
    // 出错时保持默认的简易模式设置
  }
};

// 基于Cron表达式更新预览
const updatePreviewBasedOnCronExpression = () => {
  // 记录Cron表达式到控制台
  console.log('当前Cron表达式:', displayCronExpression.value);
  
  // 解析当前cron表达式，确保所有值都有默认值
  const minute = cronExpression.value.minute || '0';
  const hour = cronExpression.value.hour || '0';
  const day = cronExpression.value.day || '*';
  const month = cronExpression.value.month || '*';
  const weekday = cronExpression.value.weekday || '*';
  
  // 进行基本验证
  try {
    // 基本验证各字段...
    
    // 获取时间段设置
    const validFromDate = reminderData.value.validFrom ? new Date(reminderData.value.validFrom) : null;
    if (validFromDate) {
      validFromDate.setHours(0, 0, 0, 0); // 设置为当天开始时间
    }
    
    const validUntilDate = reminderData.value.validUntil ? new Date(reminderData.value.validUntil) : null;
    if (validUntilDate) {
      validUntilDate.setHours(23, 59, 59, 999); // 设置为当天结束时间
    }
    
    const maxExecutions = reminderData.value.maxExecutions && reminderData.value.maxExecutions > 0 
      ? reminderData.value.maxExecutions 
      : 5; // 默认显示5个预览
    
    // 确定起始日期（现在或有效期开始日期）
    let startDate = new Date();
    if (validFromDate && validFromDate > startDate) {
      startDate = new Date(validFromDate);
    }
    
    console.log('预览生成起始日期:', startDate);
    
    // 根据Cron表达式和时间段设置，生成预览时间
    const generatedTimes = [];
    
    // 检查是否指定了月份（不是*和?）
    const hasSpecificMonth = month !== '*' && month !== '?';
    // 检查是否指定了日期（不是*和?）
    const hasSpecificDay = day !== '*' && day !== '?';
    // 检查是否指定了星期（不是*和?）
    const hasSpecificWeekday = weekday !== '*' && weekday !== '?';
    
    // 检查是否是间隔表达式（形如 */n 或 /n）
    const hasIntervalMinute = minute.includes('/');
    const hasIntervalHour = hour.includes('/');
    const hasIntervalDay = day.includes('/');
    const hasIntervalMonth = month.includes('/');
    const hasIntervalWeekday = weekday.includes('/');
    
    // 解析间隔值（如果有）
    let minuteInterval = 1;
    let hourInterval = 1;
    
    if (hasIntervalMinute) {
      const parts = minute.split('/');
      minuteInterval = parseInt(parts[1]) || 1;
    }
    
    if (hasIntervalHour) {
      const parts = hour.split('/');
      hourInterval = parseInt(parts[1]) || 1;
    }
    
    // 处理分钟间隔的情况（如：/3 * * * *，每3分钟触发一次）
    if (hasIntervalMinute && hour === '*') {
      let currentDate = new Date(startDate);
      let count = 0;
      
      // 向前调整时间到下一个符合间隔的分钟
      const currentMinute = currentDate.getMinutes();
      const nextValidMinute = Math.ceil(currentMinute / minuteInterval) * minuteInterval;
      if (nextValidMinute > currentMinute) {
        currentDate.setMinutes(nextValidMinute);
        currentDate.setSeconds(0);
      } else {
        // 如果当前分钟已经符合，则加一个间隔
        currentDate.setMinutes(currentMinute + minuteInterval);
        currentDate.setSeconds(0);
      }
      
      // 生成预览
      while (count < maxExecutions) {
        const targetDate = new Date(currentDate);
        
        // 检查是否在有效期范围内
        if (!validUntilDate || targetDate <= validUntilDate) {
          generatedTimes.push(targetDate);
          count++;
        } else {
          break;
        }
        
        // 前进到下一个间隔时间
        currentDate.setMinutes(currentDate.getMinutes() + minuteInterval);
      }
    }
    // 处理小时间隔的情况（如：0 /2 * * *，每2小时整点触发一次）
    else if (hasIntervalHour && !hasIntervalMinute) {
      let currentDate = new Date(startDate);
      let count = 0;
      
      // 向前调整时间到下一个符合间隔的小时
      const currentHour = currentDate.getHours();
      const nextValidHour = Math.ceil(currentHour / hourInterval) * hourInterval;
      
      if (nextValidHour > currentHour || (nextValidHour === currentHour && currentDate.getMinutes() > 0)) {
        currentDate.setHours(nextValidHour);
        currentDate.setMinutes(parseInt(minute) || 0);
        currentDate.setSeconds(0);
      } else {
        // 如果当前小时已经符合且分钟为0，则加一个间隔
        currentDate.setHours(currentHour + hourInterval);
        currentDate.setMinutes(parseInt(minute) || 0);
        currentDate.setSeconds(0);
      }
      
      // 生成预览
      while (count < maxExecutions) {
        const targetDate = new Date(currentDate);
        
        // 检查是否在有效期范围内
        if (!validUntilDate || targetDate <= validUntilDate) {
          generatedTimes.push(targetDate);
          count++;
        } else {
          break;
        }
        
        // 前进到下一个间隔时间
        currentDate.setHours(currentDate.getHours() + hourInterval);
      }
    }
    // 按照现有逻辑处理每周特定日期
    else if (hasSpecificWeekday) {
      // 每周特定日期
      const specificWeekdayValue = parseInt(weekday);
      let currentDate = new Date(startDate);
      let count = 0;
      
      // 最多查找30天来找出符合条件的日期
      for (let i = 0; i < 30 && count < maxExecutions; i++) {
        if (currentDate.getDay() === specificWeekdayValue) {
          // 是目标星期几
          const targetDate = new Date(currentDate);
          targetDate.setHours(parseInt(hour) || 0);
          targetDate.setMinutes(parseInt(minute) || 0);
          targetDate.setSeconds(0);
          
          // 检查是否在有效期范围内
          if ((!validUntilDate || targetDate <= validUntilDate)) {
            generatedTimes.push(targetDate);
            count++;
          }
        }
        
        // 前进一天
        currentDate.setDate(currentDate.getDate() + 1);
      }
    } 
    // 按照现有逻辑处理每月特定日期
    else if (hasSpecificDay) {
      // 每月特定日期
      const specificDayValue = parseInt(day);
      let currentDate = new Date(startDate);
      let count = 0;
      
      // 最多查找12个月来找出符合条件的日期
      for (let i = 0; i < 12 && count < maxExecutions; i++) {
        // 调整到当月的指定日期
        currentDate.setDate(1); // 先设置到月初
        currentDate.setDate(specificDayValue); // 再设置到指定日期
        
        // 如果当月没有这个日期（例如2月没有30日），会自动调整到下月初
        // 需要检查是否越界到下月
        if (currentDate.getDate() !== specificDayValue) {
          // 回到上月最后一天
          currentDate.setDate(0);
        }
        
        // 设置时间
        currentDate.setHours(parseInt(hour) || 0);
        currentDate.setMinutes(parseInt(minute) || 0);
        currentDate.setSeconds(0);
        
        // 检查是否在有效期范围内且在起始日期之后
        if (currentDate >= startDate && (!validUntilDate || currentDate <= validUntilDate)) {
          generatedTimes.push(new Date(currentDate));
          count++;
        }
        
        // 前进到下月1日
        currentDate.setDate(1);
        currentDate.setMonth(currentDate.getMonth() + 1);
      }
    } 
    // 处理特定月份的情况
    else if (hasSpecificMonth) {
      // 特定月份
      const specificMonthValue = parseInt(month);
      let currentDate = new Date(startDate);
      let count = 0;
      
      // 最多查找5年来找出符合条件的日期
      for (let i = 0; i < 60 && count < maxExecutions; i++) {
        // 如果当前不是目标月份，调整到目标月份
        if (currentDate.getMonth() + 1 !== specificMonthValue) {
          // 如果当前月小于目标月，调整到当年的目标月
          if (currentDate.getMonth() + 1 < specificMonthValue) {
            currentDate.setMonth(specificMonthValue - 1);
          } else {
            // 如果当前月大于目标月，调整到下一年的目标月
            currentDate.setFullYear(currentDate.getFullYear() + 1);
            currentDate.setMonth(specificMonthValue - 1);
          }
          currentDate.setDate(1); // 设置为月初
        }
        
        // 设置时间
        const targetDate = new Date(currentDate);
        targetDate.setHours(parseInt(hour) || 0);
        targetDate.setMinutes(parseInt(minute) || 0);
        targetDate.setSeconds(0);
        
        // 检查是否在有效期范围内
        if (targetDate >= startDate && (!validUntilDate || targetDate <= validUntilDate)) {
          generatedTimes.push(new Date(targetDate));
          count++;
        }
        
        // 前进一天
        currentDate.setDate(currentDate.getDate() + 1);
        
        // 如果月份变了且不是目标月份，直接跳到下一年的目标月
        if (currentDate.getMonth() + 1 !== specificMonthValue) {
          currentDate.setFullYear(currentDate.getFullYear() + (currentDate.getMonth() + 1 > specificMonthValue ? 1 : 0));
          currentDate.setMonth(specificMonthValue - 1);
          currentDate.setDate(1);
        }
      }
    }
    // 默认情况：每天或其他模式
    else {
      // 默认情况：每天或复杂表达式
      let currentDate = new Date(startDate);
      
      for (let i = 0; i < maxExecutions; i++) {
        const targetDate = new Date(currentDate);
        targetDate.setHours(parseInt(hour) || 0);
        targetDate.setMinutes(parseInt(minute) || 0);
        targetDate.setSeconds(0);
        
        // 检查是否在有效期范围内
        if (!validUntilDate || targetDate <= validUntilDate) {
          generatedTimes.push(targetDate);
        } else {
          break; // 超出有效期范围，停止生成
        }
        
        // 前进一天
        currentDate.setDate(currentDate.getDate() + 1);
      }
    }
    
    // 格式化并更新预览时间
    if (generatedTimes.length > 0) {
      previewTimes.value = generatedTimes.map(date => formatDate(date));
    } else {
      previewTimes.value = ['没有符合条件的预览时间'];
    }
    
    console.log('生成的预览时间:', previewTimes.value);
    
  } catch (error) {
    console.error('生成预览时出错:', error);
    previewTimes.value = ['生成预览时出错，请检查表达式'];
  }
};

// 更新人类可读描述
const updateHumanReadableDescription = () => {
  // 如果是简易模式，根据设置生成描述
  if (activeTab.value === 'simple') {
    const hour = reminderData.value.hour || 0;
    const minute = reminderData.value.minute || 0;
    const timeStr = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
    
    switch (reminderData.value.recurrenceType) {
      case 'DAILY':
        humanReadableDescription.value = `每天 ${timeStr}`;
        break;
      case 'WEEKLY':
        const weekday = reminderData.value.weekday || 1;
        humanReadableDescription.value = `每${weekDays[weekday]} ${timeStr}`;
        break;
      case 'MONTHLY':
        const dayOfMonth = reminderData.value.dayOfMonth || 1;
        humanReadableDescription.value = `每月${dayOfMonth}日 ${timeStr}`;
        break;
      case 'YEARLY':
        const month = reminderData.value.month || 1;
        const yearDay = reminderData.value.dayOfMonth || 1;
        humanReadableDescription.value = `每年${months[month-1]}${yearDay}日 ${timeStr}`;
        break;
      default:
        humanReadableDescription.value = `每天 ${timeStr}`;
    }
  } else {
    // 高级模式下，根据Cron表达式生成描述
    try {
      const minute = cronExpression.value.minute;
      const hour = cronExpression.value.hour;
      const day = cronExpression.value.day;
      const month = cronExpression.value.month;
      const weekday = cronExpression.value.weekday;
      
      let desc = '';
      
      // 检查是否是间隔表达式
      const hasIntervalMinute = minute.includes('/');
      const hasIntervalHour = hour.includes('/');
      const hasIntervalDay = day.includes('/');
      const hasIntervalMonth = month.includes('/');
      const hasIntervalWeekday = weekday.includes('/');
      
      // 解析间隔值（如果有）
      let minuteInterval = 1;
      let hourInterval = 1;
      
      if (hasIntervalMinute) {
        const parts = minute.split('/');
        minuteInterval = parseInt(parts[1]) || 1;
      }
      
      if (hasIntervalHour) {
        const parts = hour.split('/');
        hourInterval = parseInt(parts[1]) || 1;
      }
      
      // 处理分钟间隔的情况
      if (hasIntervalMinute && hour === '*') {
        desc = `每隔${minuteInterval}分钟`;
      } 
      // 处理小时间隔的情况
      else if (hasIntervalHour && minute !== '*' && !hasIntervalMinute) {
        desc = `每隔${hourInterval}小时的 ${minute} 分`;
      }
      // 描述日常情况
      else {
        desc = '在';
        
        // 描述时间
        if (hour !== '*' && minute !== '*') {
          desc += `每天 ${hour}:${String(minute).padStart(2, '0')} `;
        } else if (hour !== '*') {
          desc += `每天 ${hour}点的每分钟 `;
        } else if (minute !== '*') {
          desc += `每小时的 ${minute} 分 `;
        } else {
          desc += '每分钟 ';
        }
        
        // 描述日期
        if (day !== '*' && month !== '*' && weekday === '*') {
          // 每年特定日期
          let monthName = '每月';
          if (month !== '*') {
            if (!isNaN(parseInt(month)) && parseInt(month) >= 1 && parseInt(month) <= 12) {
              monthName = months[parseInt(month) - 1];
            } else {
              monthName = `${month}月`;
            }
          }
          desc += `每年${monthName}${day}日`;
        } else if (day === '*' && month === '*' && weekday !== '*') {
          // 每周特定日
          let weekdayName = '某天';
          if (!isNaN(parseInt(weekday)) && parseInt(weekday) >= 0 && parseInt(weekday) <= 6) {
            weekdayName = weekDays[parseInt(weekday)];
          } else {
            weekdayName = `星期${weekday}`;
          }
          desc += `每${weekdayName}`;
        } else if (day !== '*' && month === '*' && weekday === '*') {
          // 每月特定日
          desc += `每月${day}日`;
        } else if (day === '*' && month === '*' && weekday === '*') {
          // 每天
          desc += '每天';
        } else {
          // 复杂表达式
          desc += `满足Cron表达式: "${displayCronExpression.value}"`;
        }
      }
      
      humanReadableDescription.value = desc;
    } catch (error) {
      console.error('生成人类可读描述时出错:', error);
      humanReadableDescription.value = `Cron表达式: ${displayCronExpression.value}`;
    }
  }
};

// 保存提醒
const saveReminder = () => {
  // 检查必填字段
  if (!reminderData.value.title) {
    alert('请填写提醒标题');
    return;
  }
  
  // 检查Cron表达式是否有错误
  const hasErrors = Object.values(cronErrors.value).some(error => error !== '');
  if (activeTab.value === 'advanced' && hasErrors) {
    alert('Cron表达式存在错误，请修正后再保存');
    return;
  }
  
  // 如果是简易模式，确保Cron表达式已更新
  if (activeTab.value === 'simple') {
    updateCronFromSimpleMode();
  }
  
  // 创建一个包含所有数据的对象
  const reminderDataToSave = {
    ...reminderData.value,
    timeMode: activeTab.value, // 记录使用的是哪种模式
    cronExpression: displayCronExpression.value // 无论哪种模式，都使用当前的Cron表达式
  };
  
  // 验证日期范围
  if (reminderDataToSave.validUntil && new Date(reminderDataToSave.validUntil) < new Date(reminderDataToSave.validFrom)) {
    alert('结束日期不能早于开始日期');
    return;
  }
  
  console.log('ComplexReminderModal: 保存数据', reminderDataToSave);
  emit('save', reminderDataToSave);
  closeModal();
};

// 关闭模态框
const closeModal = () => {
  console.log('ComplexReminderModal: 关闭弹窗');
  emit('close');
};

// 在组件挂载完成后调用一次更新预览
onMounted(() => {
  console.log('ComplexReminderModal: 组件已挂载');
  console.log('ComplexReminderModal: 显示状态 =>', props.show);
  console.log('ComplexReminderModal: 初始数据 =>', props.initialData);
  
  // 初始化时，根据默认的简易模式设置更新Cron表达式
  updateCronFromSimpleMode();
  
  // 组件挂载后手动触发一次预览更新
  setTimeout(() => {
    updatePreviewAndDescription();
  }, 0);
  
  // 组件挂载后手动处理初始数据
  if (props.initialData && Object.keys(props.initialData).length > 0) {
    const newVal = props.initialData;
    console.log('ComplexReminderModal: 收到初始数据', newVal);
    
    // 更新本地数据，保留原有值，避免undefined
    reminderData.value = {
      ...reminderData.value,
      id: newVal.id,
      title: newVal.title || reminderData.value.title,
      description: newVal.description || reminderData.value.description,
      reminderType: newVal.reminderType || reminderData.value.reminderType,
      cronExpression: newVal.cronExpression,
      validFrom: newVal.validFrom || reminderData.value.validFrom,
      validUntil: newVal.validUntil || reminderData.value.validUntil,
      maxExecutions: newVal.maxExecutions || reminderData.value.maxExecutions
    };
    
    // 解析cron表达式，设置模式
    if (newVal.cronExpression) {
      const parts = newVal.cronExpression.split(' ');
      if (parts.length === 5) {
        cronExpression.value = {
          minute: parts[0],
          hour: parts[1],
          day: parts[2],
          month: parts[3],
          weekday: parts[4]
        };
        
        // 尝试判断是简单模式还是高级模式
        const isSimplePattern = 
          (parts[2] === '*' && parts[3] === '*' && parts[4] === '*') || // 每天
          (parts[2] === '*' && parts[3] === '*' && /^[0-6]$/.test(parts[4])) || // 每周某天
          (parts[2] !== '*' && parts[3] !== '*' && parts[4] === '*'); // 每年某天
        
        activeTab.value = isSimplePattern ? 'simple' : 'advanced';
        
        // 如果是简单模式，尝试设置hour和minute
        if (isSimplePattern) {
          reminderData.value.hour = parseInt(parts[1]);
          reminderData.value.minute = parseInt(parts[0]);
          
          // 如果是每周某天
          if (/^[0-6]$/.test(parts[4])) {
            reminderData.value.recurrenceType = 'WEEKLY';
            reminderData.value.weekday = parseInt(parts[4]);
          }
          // 如果是每月某天
          else if (/^\d+$/.test(parts[2]) && parts[3] === '*') {
            reminderData.value.recurrenceType = 'MONTHLY';
            reminderData.value.dayOfMonth = parseInt(parts[2]);
          }
          // 如果是每年某天
          else if (/^\d+$/.test(parts[2]) && /^\d+$/.test(parts[3])) {
            reminderData.value.recurrenceType = 'YEARLY';
            reminderData.value.dayOfMonth = parseInt(parts[2]);
            reminderData.value.month = parseInt(parts[3]);
          }
          // 否则默认每天
          else {
            reminderData.value.recurrenceType = 'DAILY';
          }
        }
      }
    }
    
    // 更新预览 - 这里不立即调用，由挂载后的setTimeout触发
  }
});
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
  z-index: 3000;
}

.modal-content {
  position: relative;
  background-color: white;
  padding: 30px 40px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.2);
  width: 600px;
  max-width: 90%;
  max-height: 90vh;
  overflow-y: auto;
  z-index: 3001;
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

h2, h3, h4 {
  margin-top: 0;
  color: #555;
  font-weight: 700;
}

h2 {
  text-align: center;
  margin-bottom: 25px;
}

h3 {
  margin-bottom: 15px;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.section {
  margin-bottom: 25px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 600;
}

.form-control {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
}

.required {
  color: red;
  margin-left: 3px;
}

.tabs {
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
}

.tab-buttons {
  display: flex;
  border-bottom: 1px solid #eee;
}

.tab-button {
  flex: 1;
  padding: 12px;
  background: #f5f5f5;
  border: none;
  cursor: pointer;
  font-weight: 600;
  transition: background-color 0.2s;
}

.tab-button.active {
  background: #fff;
  border-bottom: 2px solid #4a90e2;
}

.tab-content {
  padding: 20px;
  background: #fff;
}

.cron-fields {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-gap: 15px;
}

.cron-help {
  margin-top: 15px;
  padding: 10px;
  background: #f9f9f9;
  border-radius: 8px;
  font-size: 13px;
}

.next-executions {
  background: #f9f9f9;
  padding: 15px;
  border-radius: 8px;
}

.execution-times {
  list-style: none;
  padding: 0;
  margin: 10px 0 0;
}

.execution-times li {
  padding: 8px 0;
  border-bottom: 1px dashed #eee;
}

.execution-times li:last-child {
  border-bottom: none;
}

.cron-expression {
  margin: 15px 0;
  padding: 15px;
  background: #f0f8ff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cron-expression code {
  font-family: monospace;
  background: #e6f0ff;
  padding: 5px 10px;
  border-radius: 4px;
}

.human-readable {
  margin-top: 15px;
  font-style: italic;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.button {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: background-color 0.2s;
}

.button.primary {
  background-color: #4a90e2;
  color: white;
}

.button.primary:hover {
  background-color: #3a7bc8;
}

.button.secondary {
  background-color: #e2e2e2;
  color: #333;
}

.button.secondary:hover {
  background-color: #d4d4d4;
}

.time-range-settings {
  margin-top: 25px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.time-range-row {
  display: flex;
  gap: 15px;
  align-items: flex-start;
}

.time-range-row .form-group {
  flex: 1;
  margin-bottom: 0;
  min-width: 0; /* 防止flex子项溢出 */
}

.time-range-row input[type="number"] {
  width: 100%;
}

.time-range-row .form-group input {
  height: 40px;
  box-sizing: border-box;
}

@media (max-width: 768px) {
  .time-range-row {
    flex-direction: column;
    gap: 10px;
  }
  
  .time-range-row .form-group {
    width: 100%;
    margin-bottom: 10px;
  }
}

.field-disabled {
  background-color: #f5f5f5;
  border-color: #e0e0e0;
  color: #999;
  cursor: pointer;
}

.field-note {
  font-size: 12px;
  color: #ff6b6b;
  font-weight: normal;
  margin-left: 5px;
}

.field-error {
  color: red;
  font-size: 12px;
  margin-top: 5px;
}

.form-row {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
}

.form-row .form-group {
  flex: 1;
  margin-bottom: 0;
}

.time-inputs {
  display: flex;
  align-items: center;
}

.time-input {
  width: 70px;
}

.time-separator {
  margin: 0 5px;
  font-weight: bold;
}

.weekday-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 5px;
}

.weekday-option {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
}

.year-date-selector {
  display: flex;
  gap: 10px;
}

.year-date-selector select {
  flex: 1;
}

.no-preview {
  color: #666;
  font-style: italic;
  text-align: center;
  padding: 10px;
}
</style>
