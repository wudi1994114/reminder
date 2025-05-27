<template>
  <view class="cron-picker-overlay" v-if="show" @touchmove.stop.prevent @click.self="handleOverlayClick">
    <view class="cron-picker-container" @click.stop @touchmove.stop>
      <!-- 顶部标题栏 -->
      <view class="header-section">
        <view class="header-content">
          <view class="cancel-btn" @click="handleCancel">
            <text class="btn-text">取消</text>
          </view>
          <text class="title">重复设置</text>
          <view class="confirm-btn" @click="handleConfirm">
            <text class="btn-text">确定</text>
          </view>
        </view>
      </view>
      
      <!-- 时间类型切换 -->
      <view class="type-switch-section">
        <view class="switch-container">
          <view 
            class="switch-btn" 
            :class="{ active: currentType === 'year' }"
            @click="switchType('year')"
          >
            <text class="switch-text">年</text>
          </view>
          <view 
            class="switch-btn" 
            :class="{ active: currentType === 'month' }"
            @click="switchType('month')"
          >
            <text class="switch-text">月</text>
          </view>
          <view 
            class="switch-btn" 
            :class="{ active: currentType === 'week' }"
            @click="switchType('week')"
          >
            <text class="switch-text">周</text>
          </view>
        </view>
      </view>
      
      <!-- 时间选择区域 -->
      <view class="time-section">
        <view class="time-header">
          <text class="time-title">触发时间</text>
        </view>
        <view class="time-picker-container">
          <picker mode="time" :value="selectedTime" @change="onTimeChange">
            <view class="time-display">
              <text class="time-text">{{ selectedTime || '09:00' }}</text>
              <text class="time-arrow">▼</text>
            </view>
          </picker>
        </view>
      </view>
      
      <!-- 选择内容区域 -->
      <view class="content-section">
        <!-- 年选择 - 显示月份、周、日 -->
        <view v-if="currentType === 'year'" class="selection-area">
          <!-- 月份选择 -->
          <view class="level-section">
            <view class="level-header" @click="toggleMonthCollapse">
              <text class="section-title">选择月份</text>
              <text class="collapse-icon" :class="{ collapsed: isMonthCollapsed }">▼</text>
            </view>
            <view v-if="!isMonthCollapsed" class="options-grid">
              <view 
                v-for="month in monthOptions" 
                :key="month.value"
                class="option-btn"
                :class="{ selected: selectedMonths.includes(month.value) }"
                @click="toggleMonth(month.value)"
              >
                <text class="option-text">{{ month.label }}</text>
              </view>
            </view>
          </view>
          
          <!-- 周选择 -->
          <view class="level-section">
            <view class="level-header" @click="toggleWeekCollapse">
              <text class="section-title">选择周</text>
              <text class="collapse-icon" :class="{ collapsed: isWeekCollapsed }">▼</text>
            </view>
            <view v-if="!isWeekCollapsed" class="options-grid">
              <view 
                v-for="weekday in weekdayOptions" 
                :key="weekday.value"
                class="option-btn"
                :class="{ selected: selectedWeekdays.includes(weekday.value) }"
                @click="toggleWeekday(weekday.value)"
              >
                <text class="option-text">{{ weekday.label }}</text>
              </view>
            </view>
          </view>
          
          <!-- 日选择 -->
          <view class="level-section">
            <view class="level-header" @click="toggleDayCollapse">
              <text class="section-title">选择日</text>
              <text class="collapse-icon" :class="{ collapsed: isDayCollapsed }">▼</text>
            </view>
            <view v-if="!isDayCollapsed" class="options-grid day-grid">
              <view 
                v-for="day in dayOptions" 
                :key="day"
                class="option-btn"
                :class="{ selected: selectedDays.includes(day) }"
                @click="toggleDay(day)"
              >
                <text class="option-text">{{ day }}</text>
              </view>
            </view>
          </view>
        </view>
        
        <!-- 月选择 - 显示周和天 -->
        <view v-if="currentType === 'month'" class="selection-area">
          <!-- 周选择 -->
          <view class="level-section">
            <view class="level-header" @click="toggleWeekCollapse">
              <text class="section-title">选择周</text>
              <text class="collapse-icon" :class="{ collapsed: isWeekCollapsed }">▼</text>
            </view>
            <view v-if="!isWeekCollapsed" class="options-grid">
              <view 
                v-for="weekday in weekdayOptions" 
                :key="weekday.value"
                class="option-btn"
                :class="{ selected: selectedWeekdays.includes(weekday.value) }"
                @click="toggleWeekday(weekday.value)"
              >
                <text class="option-text">{{ weekday.label }}</text>
              </view>
            </view>
          </view>
          
          <!-- 日选择 -->
          <view class="level-section">
            <view class="level-header" @click="toggleDayCollapse">
              <text class="section-title">选择日</text>
              <text class="collapse-icon" :class="{ collapsed: isDayCollapsed }">▼</text>
            </view>
            <view v-if="!isDayCollapsed" class="options-grid day-grid">
              <view 
                v-for="day in dayOptions" 
                :key="day"
                class="option-btn"
                :class="{ selected: selectedDays.includes(day) }"
                @click="toggleDay(day)"
              >
                <text class="option-text">{{ day }}</text>
              </view>
            </view>
          </view>
        </view>
        
        <!-- 周选择 - 显示周几 -->
        <view v-if="currentType === 'week'" class="selection-area">
          <view class="level-section">
            <view class="level-header">
              <text class="section-title">选择星期</text>
            </view>
            <view class="options-grid">
              <view 
                v-for="weekday in weekdayOptions" 
                :key="weekday.value"
                class="option-btn"
                :class="{ selected: selectedWeekdays.includes(weekday.value) }"
                @click="toggleWeekday(weekday.value)"
              >
                <text class="option-text">{{ weekday.label }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { ref, reactive, watch } from 'vue';

export default {
  name: 'CronExpressionPicker',
  props: {
    show: {
      type: Boolean,
      default: false
    },
    initialValue: {
      type: String,
      default: ''
    }
  },
  emits: ['confirm', 'cancel', 'update:show'],
  setup(props, { emit }) {
    console.log('CronExpressionPicker setup, show:', props.show);
    
    const currentType = ref('month');
    const selectedMonths = ref([]);
    const selectedDays = ref([]);
    const selectedWeekdays = ref([]);
    const selectedTime = ref('09:00');  // 默认时间
    
    // 折叠状态 - 设置默认折叠状态
    const isMonthCollapsed = ref(true);  // 年模式下月份默认折叠
    const isWeekCollapsed = ref(true);   // 年和月模式下周默认折叠
    const isDayCollapsed = ref(true);    // 年和月模式下日默认折叠
    
    // 监听show属性变化
    watch(() => props.show, (newVal) => {
      console.log('CronExpressionPicker show changed:', newVal);
      if (newVal && props.initialValue) {
        // 当弹窗显示时，解析初始值
        parseInitialValue();
      }
    });
    
    // 解析初始cron表达式
    const parseInitialValue = () => {
      if (!props.initialValue) return;
      
      try {
        const parts = props.initialValue.trim().split(/\s+/);
        if (parts.length >= 3) {
          let hour, minute;
          
          if (parts.length === 5) {
            // 5位格式: 分 时 日 月 周
            minute = parts[0];
            hour = parts[1];
          } else if (parts.length >= 6) {
            // 6位或7位格式: 秒 分 时 日 月 周 [年]
            minute = parts[1];
            hour = parts[2];
          }
          
          if (hour && minute) {
            selectedTime.value = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
          }
        }
      } catch (error) {
        console.error('解析初始cron表达式失败:', error);
      }
    };
    
    // 月份选项
    const monthOptions = [
      { value: 1, label: '1月' },
      { value: 2, label: '2月' },
      { value: 3, label: '3月' },
      { value: 4, label: '4月' },
      { value: 5, label: '5月' },
      { value: 6, label: '6月' },
      { value: 7, label: '7月' },
      { value: 8, label: '8月' },
      { value: 9, label: '9月' },
      { value: 10, label: '10月' },
      { value: 11, label: '11月' },
      { value: 12, label: '12月' }
    ];
    
    // 日期选项 (1-31)
    const dayOptions = Array.from({ length: 31 }, (_, i) => i + 1);
    
    // 星期选项
    const weekdayOptions = [
      { value: 1, label: '周一' },
      { value: 2, label: '周二' },
      { value: 3, label: '周三' },
      { value: 4, label: '周四' },
      { value: 5, label: '周五' },
      { value: 6, label: '周六' },
      { value: 0, label: '周日' }
    ];
    
    // 切换类型
    const switchType = (type) => {
      console.log('切换类型到:', type);
      currentType.value = type;
      
      // 切换类型时清空其他选择
      if (type !== 'year') selectedMonths.value = [];
      if (type !== 'month') selectedDays.value = [];
      if (type !== 'week') selectedWeekdays.value = [];
      
      // 根据类型设置折叠状态
      if (type === 'year') {
        // 年模式：所有内容默认折叠
        isMonthCollapsed.value = true;
        isWeekCollapsed.value = true;
        isDayCollapsed.value = true;
      } else if (type === 'month') {
        // 月模式：周默认折叠，日可展开
        isMonthCollapsed.value = false; // 月模式下不显示月份选择
        isWeekCollapsed.value = true;   // 周默认折叠
        isDayCollapsed.value = false;   // 日可以展开
      } else if (type === 'week') {
        // 周模式：只显示周选择，其他不相关
        isMonthCollapsed.value = false;
        isWeekCollapsed.value = false;  // 周展开
        isDayCollapsed.value = false;
      }
    };
    
    // 切换月份选择
    const toggleMonth = (month) => {
      const index = selectedMonths.value.indexOf(month);
      if (index > -1) {
        selectedMonths.value.splice(index, 1);
      } else {
        selectedMonths.value.push(month);
      }
    };
    
    // 切换日期选择
    const toggleDay = (day) => {
      const index = selectedDays.value.indexOf(day);
      if (index > -1) {
        selectedDays.value.splice(index, 1);
      } else {
        selectedDays.value.push(day);
      }
    };
    
    // 切换星期选择
    const toggleWeekday = (weekday) => {
      const index = selectedWeekdays.value.indexOf(weekday);
      if (index > -1) {
        selectedWeekdays.value.splice(index, 1);
      } else {
        selectedWeekdays.value.push(weekday);
      }
    };
    
    // 时间变化处理
    const onTimeChange = (e) => {
      selectedTime.value = e.detail.value;
    };
    
    // 折叠切换方法
    const toggleMonthCollapse = () => {
      isMonthCollapsed.value = !isMonthCollapsed.value;
    };
    
    const toggleWeekCollapse = () => {
      isWeekCollapsed.value = !isWeekCollapsed.value;
    };
    
    const toggleDayCollapse = () => {
      isDayCollapsed.value = !isDayCollapsed.value;
    };
    
    // 处理遮罩点击
    const handleOverlayClick = () => {
      emit('update:show', false);
      emit('cancel');
    };
    
    // 处理取消
    const handleCancel = () => {
      console.log('点击取消按钮');
      emit('update:show', false);
      emit('cancel');
    };
    
    // 处理确认
    const handleConfirm = () => {
      console.log('点击确认按钮');
      const cronExpression = generateCronExpression();
      console.log('生成的cron表达式:', cronExpression);
      emit('confirm', cronExpression);
      emit('update:show', false);
    };
    
    // 生成cron表达式
    const generateCronExpression = () => {
      // 基础格式: 秒 分 时 日 月 周 年
      // 从选择的时间中提取小时和分钟
      const [hour, minute] = selectedTime.value.split(':');
      
      console.log('生成cron表达式，当前类型:', currentType.value);
      console.log('选择的时间:', selectedTime.value);
      console.log('选择的月份:', selectedMonths.value);
      console.log('选择的日期:', selectedDays.value);
      console.log('选择的星期:', selectedWeekdays.value);
      
      if (currentType.value === 'year') {
        // 年度重复：每年指定月份的1号
        const months = selectedMonths.value.length > 0 ? selectedMonths.value.join(',') : '1'; // 默认1月
        return `0 ${minute} ${hour} 1 ${months} ? *`;
      } else if (currentType.value === 'month') {
        // 月度重复：每月指定日期
        const days = selectedDays.value.length > 0 ? selectedDays.value.join(',') : '1'; // 默认1号
        return `0 ${minute} ${hour} ${days} * ? *`;
      } else if (currentType.value === 'week') {
        // 周度重复：每周指定星期
        const weekdays = selectedWeekdays.value.length > 0 ? selectedWeekdays.value.join(',') : '1'; // 默认周一
        return `0 ${minute} ${hour} ? * ${weekdays} *`;
      }
      
      return `0 ${minute} ${hour} * * ? *`; // 默认每天
    };

    return {
      currentType,
      selectedMonths,
      selectedDays,
      selectedWeekdays,
      selectedTime,
      isMonthCollapsed,
      isWeekCollapsed,
      isDayCollapsed,
      monthOptions,
      dayOptions,
      weekdayOptions,
      switchType,
      toggleMonth,
      toggleDay,
      toggleWeekday,
      onTimeChange,
      toggleMonthCollapse,
      toggleWeekCollapse,
      toggleDayCollapse,
      handleOverlayClick,
      handleCancel,
      handleConfirm
    };
  }
};
</script>

<style scoped>
/* 遮罩层 */
.cron-picker-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 0;
  
  /* 阻止滚动穿透 */
  overflow: hidden;
  touch-action: none;
  
  /* 防止在iOS上出现弹性滚动 */
  -webkit-overflow-scrolling: auto;
  
  /* 完全阻止背景操作 */
  pointer-events: auto;
  user-select: none;
  -webkit-user-select: none;
}

/* 弹窗容器 */
.cron-picker-container {
  width: 100%;
  height: 100%;
  background-color: #fcfbf8;
  border-radius: 32rpx 32rpx 0 0;
  display: flex;
  flex-direction: column;
  
  /* 允许弹窗内容滚动 */
  overflow: hidden;
  touch-action: pan-y;
  
  /* iOS滚动优化 */
  -webkit-overflow-scrolling: touch;
  
  /* 确保弹窗可以接收事件 */
  pointer-events: auto;
  
  /* 添加柔和阴影 */
  box-shadow: 0 -8rpx 32rpx rgba(0, 0, 0, 0.15);
}

/* 顶部标题栏 */
.header-section {
  padding: 32rpx;
  border-bottom: 1rpx solid #e9e0ce;
  border-radius: 32rpx 32rpx 0 0;
  background-color: #fcfbf8;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cancel-btn, .confirm-btn {
  background: none;
  border: none;
  padding: 16rpx 24rpx;
  font-size: 32rpx;
  color: #9d8148;
  pointer-events: auto;
  cursor: pointer;
  border-radius: 20rpx;
  transition: all 0.2s ease;
}

.cancel-btn:active, .confirm-btn:active {
  background-color: rgba(157, 129, 72, 0.1);
  transform: scale(0.95);
}

.confirm-btn {
  color: #f7bd4a;
  font-weight: 600;
}

.title {
  font-size: 36rpx;
  font-weight: 700;
  color: #1c170d;
}

.btn-text {
  font-size: 32rpx;
}

/* 类型切换区域 */
.type-switch-section {
  padding: 32rpx;
  border-bottom: 1rpx solid #e9e0ce;
  background: linear-gradient(135deg, #fcfbf8 0%, #f8f5ed 100%);
}

/* 时间选择区域 */
.time-section {
  padding: 32rpx;
  border-bottom: 1rpx solid #e9e0ce;
  background-color: #fcfbf8;
}

.time-header {
  margin-bottom: 24rpx;
}

.time-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
}

.time-picker-container {
  width: 100%;
}

.time-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 32rpx;
  background-color: #f4efe7;
  border-radius: 20rpx;
  border: 2rpx solid transparent;
  transition: all 0.2s ease;
}

.time-display:active {
  background-color: #f0ede4;
  border-color: #f7bd4a;
}

.time-text {
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
}

.time-arrow {
  font-size: 24rpx;
  color: #9d8148;
  font-weight: 600;
}

.switch-container {
  display: flex;
  background-color: #f0ede4;
  border-radius: 24rpx;
  padding: 12rpx;
  gap: 8rpx;
  box-shadow: inset 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.switch-btn {
  flex: 1;
  height: 72rpx;
  background: none;
  border: none;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  pointer-events: auto;
  cursor: pointer;
}

.switch-btn.active {
  background-color: #fcfbf8;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.12), 0 2rpx 8rpx rgba(0, 0, 0, 0.08);
  transform: translateY(-1rpx);
}

.switch-btn:active {
  transform: scale(0.96);
}

.switch-text {
  font-size: 30rpx;
  color: #9d8148;
  font-weight: 500;
}

.switch-btn.active .switch-text {
  color: #1c170d;
  font-weight: 600;
}

/* 内容区域 */
.content-section {
  flex: 1;
  padding: 32rpx 32rpx 64rpx 32rpx;
  overflow-y: auto;
  
  /* 允许内容区域滚动 */
  touch-action: pan-y;
  -webkit-overflow-scrolling: touch;
  
  /* 使用统一的背景色，避免渐变造成底部变淡 */
  background-color: #fcfbf8;
}

.selection-area {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  padding-bottom: 32rpx;
}

.level-section {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-bottom: 32rpx;
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 24rpx;
  padding: 8rpx;
  box-shadow: 0 2rpx 16rpx rgba(0, 0, 0, 0.04);
  border: 1rpx solid rgba(255, 255, 255, 0.8);
}

.level-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 24rpx;
  cursor: pointer;
  border-radius: 16rpx;
  transition: all 0.2s ease;
}

.level-header:active {
  background-color: rgba(157, 129, 72, 0.05);
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
}

.collapse-icon {
  font-size: 24rpx;
  color: #9d8148;
  transition: transform 0.3s ease;
}

.collapse-icon.collapsed {
  transform: rotate(-90deg);
}

/* 选项网格 */
.options-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
  padding: 16rpx;
}

/* 日期选项网格 - 每行6个 */
.options-grid.day-grid {
  grid-template-columns: repeat(6, 1fr);
  gap: 12rpx;
  padding: 16rpx;
}

.option-btn {
  height: 80rpx;
  background-color: #f0ede4;
  border: 2rpx solid transparent;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  pointer-events: auto;
  cursor: pointer;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.option-btn.selected {
  background-color: #f7bd4a;
  border-color: #f7bd4a;
  box-shadow: 0 4rpx 16rpx rgba(247, 189, 74, 0.3), 0 2rpx 8rpx rgba(0, 0, 0, 0.08);
  transform: translateY(-2rpx);
}

.option-text {
  font-size: 28rpx;
  color: #1c170d;
  font-weight: 500;
}

.option-btn.selected .option-text {
  color: #1c170d;
  font-weight: 600;
}

.option-btn:active {
  transform: scale(0.95);
}

.option-btn:hover {
  background-color: #ebe7dc;
  transform: translateY(-1rpx);
}

.option-btn.selected:hover {
  background-color: #f7bd4a;
  transform: translateY(-3rpx);
}

/* 响应式调整 */
@media (max-width: 750rpx) {
  .header-section, .type-switch-section {
    padding: 24rpx;
  }
  
  .content-section {
    padding: 24rpx 24rpx 48rpx 24rpx;
  }
  
  .title {
    font-size: 32rpx;
  }
  
  .cancel-btn, .confirm-btn, .btn-text {
    font-size: 28rpx;
  }
  
  .switch-btn {
    height: 64rpx;
  }
  
  .switch-text {
    font-size: 26rpx;
  }
  
  .section-title {
    font-size: 28rpx;
  }
  
  .options-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 12rpx;
  }
  
  .option-btn {
    height: 72rpx;
  }
  
  .option-text {
    font-size: 26rpx;
  }
}

/* 弹窗动画 */
.cron-picker-overlay {
  animation: modalFadeIn 0.3s ease-out;
}

.cron-picker-container {
  animation: modalSlideIn 0.3s ease-out;
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: translateY(50rpx) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
</style> 