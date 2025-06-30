<template>
  <view class="datetime-picker">
    <!-- 分离的日期时间设置 -->
    <view class="datetime-container">
      <!-- 日期设置 -->
      <view class="date-time-section">
        <view class="section-header">
          <text class="section-label">{{ label }}</text>
        </view>
        
        <view class="datetime-inputs">
          <!-- 日期选择 -->
          <view v-if="displayMode === 'datetime'" class="input-item date-input" @click="showDatePicker">
            <text class="input-label">日期</text>
            <text class="input-value">{{ getFormattedDate() }}</text>
          </view>
          
          <!-- 时间选择 -->
          <view class="input-item time-input" @click="showTimePicker">
            <text class="input-label">时间</text>
            <text class="input-value">{{ getFormattedTime() }}</text>
          </view>
        </view>
        
        <!-- 温和的时间提示 -->
        <view v-if="showTimeWarning" class="time-warning">
          <text class="warning-text">💡 建议提前2分钟以上设置提醒，这样能确保及时通知到您</text>
        </view>
      </view>
    </view>
    
    <!-- 日期选择器弹窗 -->
    <view v-if="showDatePickerModal" class="picker-overlay" @click="hideDatePicker">
      <view class="picker-modal" @click.stop>
        <view class="picker-header">
          <text class="picker-cancel" @click="hideDatePicker">取消</text>
          <text class="picker-title">选择日期</text>
          <text class="picker-confirm" @click.stop="confirmDatePicker">确定</text>
        </view>
        
        <picker-view 
          class="picker-view" 
          :value="datePickerValue" 
          @change="onDatePickerChange"
        >
          <!-- 年份 -->
          <picker-view-column>
            <view v-for="(year, index) in years" :key="index" class="picker-item">
              {{ year }}年
            </view>
          </picker-view-column>
          
          <!-- 月份 -->
          <picker-view-column>
            <view v-for="(month, index) in months" :key="index" class="picker-item">
              {{ month }}月
            </view>
          </picker-view-column>
          
          <!-- 日期 -->
          <picker-view-column>
            <view v-for="(day, index) in days" :key="index" class="picker-item">
              {{ day }}日
            </view>
          </picker-view-column>
        </picker-view>
      </view>
    </view>
    
    <!-- 时间选择器弹窗 -->
    <view v-if="showTimePickerModal" class="picker-overlay" @click="hideTimePicker">
      <view class="picker-modal" @click.stop>
        <view class="picker-header">
          <text class="picker-cancel" @click="hideTimePicker">取消</text>
          <text class="picker-title">选择时间</text>
          <text class="picker-confirm" @click.stop="confirmTimePicker">确定</text>
        </view>
        
        <picker-view 
          class="picker-view" 
          :value="timePickerValue" 
          @change="onTimePickerChange"
        >
          <!-- 小时 -->
          <picker-view-column>
            <view v-for="(hour, index) in hours" :key="index" class="picker-item">
              {{ String(hour).padStart(2, '0') }}时
            </view>
          </picker-view-column>
          
          <!-- 分钟 -->
          <picker-view-column>
            <view v-for="(minute, index) in minutes" :key="index" class="picker-item">
              {{ String(minute).padStart(2, '0') }}分
            </view>
          </picker-view-column>
        </picker-view>
      </view>
    </view>
  </view>
</template>

<script>
import { ref, computed, watch, onMounted } from 'vue';

export default {
  name: 'DateTimePicker',
  props: {
    // 显示标签
    label: {
      type: String,
      default: '时间设置'
    },
    // 初始日期 YYYY-MM-DD
    initialDate: {
      type: String,
      default: ''
    },
    // 初始时间 HH:mm
    initialTime: {
      type: String,
      default: ''
    },
    // 是否自动设置默认时间（当前时间+1小时）
    autoSetDefault: {
      type: Boolean,
      default: true
    },
    // 选择器模式：'datetime' | 'date' | 'time'
    mode: {
      type: String,
      default: 'datetime',
      validator: (value) => ['datetime', 'date', 'time'].includes(value)
    },
    // 自定义显示列：['year', 'month', 'day', 'hour', 'minute']
    columns: {
      type: Array,
      default: () => ['year', 'month', 'day', 'hour', 'minute']
    },
    // 是否启用时间验证（提前2分钟）
    enableValidation: {
      type: Boolean,
      default: true
    },
    // 显示模式: 'datetime' | 'time'
    displayMode: {
      type: String,
      default: 'datetime'
    }
  },
  emits: ['change', 'dateChange', 'timeChange', 'weekdayChange', 'picker-show', 'picker-hide'],
  setup(props, { emit }) {
    const reminderDate = ref('');
    const reminderTime = ref('');
    const showDatePickerModal = ref(false);
    const showTimePickerModal = ref(false);
    const datePickerValue = ref([]);
    const timePickerValue = ref([]);
    const showTimeWarning = ref(false);
    
    // 生成选择器数据
    const currentYear = new Date().getFullYear();
    const years = ref(Array.from({ length: 10 }, (_, i) => currentYear + i));
    const months = ref(Array.from({ length: 12 }, (_, i) => i + 1));
    const days = ref([]);
    const hours = ref(Array.from({ length: 24 }, (_, i) => i));
    const minutes = ref(Array.from({ length: 60 }, (_, i) => i));
    
    // 获取默认时间（当前时间+2分钟）- 每次调用都实时获取
    const getDefaultDateTime = () => {
      const now = new Date(); // 每次调用都获取最新的当前时间
      now.setMinutes(now.getMinutes() + 2); // 加2分钟
      return {
        date: `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`,
        time: `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
      };
    };
    
    // 检查时间是否过短
    const checkTimeValidity = () => {
      // 如果禁用了验证，则直接返回true
      if (!props.enableValidation) {
        showTimeWarning.value = false;
        return true;
      }

      if (!reminderDate.value || !reminderTime.value) {
        showTimeWarning.value = false;
        return true;
      }
      
      const selectedDateTime = new Date(`${reminderDate.value}T${reminderTime.value}:00`);
      const currentTime = new Date(); // 实时获取当前时间
      const minTime = new Date(currentTime.getTime()); // 创建当前时间的副本
      minTime.setMinutes(minTime.getMinutes() + 2); // 最小提前2分钟
      
      const isValid = selectedDateTime >= minTime;
      showTimeWarning.value = !isValid;
      return isValid;
    };
    
    // 初始化日期时间
    const initializeDateTime = () => {
      console.log('datetime-picker: 开始初始化, props:', { 
        initialDate: props.initialDate, 
        initialTime: props.initialTime,
        autoSetDefault: props.autoSetDefault 
      });
      
      // 初始化日期
      if (props.initialDate) {
        reminderDate.value = props.initialDate;
        console.log('datetime-picker: 使用传入的初始日期:', props.initialDate);
      } else if (props.autoSetDefault && !reminderDate.value) {
        const defaultDateTime = getDefaultDateTime();
        reminderDate.value = defaultDateTime.date;
        console.log('datetime-picker: 使用默认日期:', reminderDate.value);
      }
      
      // 初始化时间
      if (props.initialTime) {
        reminderTime.value = props.initialTime;
        console.log('datetime-picker: 使用传入的初始时间:', props.initialTime);
      } else if (props.autoSetDefault && !reminderTime.value) {
        const defaultDateTime = getDefaultDateTime();
        reminderTime.value = defaultDateTime.time;
        console.log('datetime-picker: 使用默认时间（当前时间+2分钟）:', reminderTime.value);
      }
      
      console.log('datetime-picker: 初始化完成，最终值:', { date: reminderDate.value, time: reminderTime.value });
      
      // 检查时间有效性
      checkTimeValidity();
      
      // 初始化后触发一次change事件
      updateEventTime();
    };
    
    // 更新事件时间
    const updateEventTime = () => {
      if (reminderDate.value && reminderTime.value) {
        const eventTime = `${reminderDate.value} ${reminderTime.value}:00`;
        emit('change', {
          date: reminderDate.value,
          time: reminderTime.value,
          eventTime: eventTime
        });
        emit('dateChange', reminderDate.value);
        emit('timeChange', reminderTime.value);
      }
    };
    
    // 更新天数
    const updateDays = (year, month) => {
      const daysInMonth = new Date(year, month, 0).getDate();
      days.value = Array.from({ length: daysInMonth }, (_, i) => i + 1);
    };
    
    // 显示日期选择器
    const showDatePicker = () => {
      console.log('📅 显示日期选择器');
      initDatePickerValue();
      showDatePickerModal.value = true;
    };
    
    // 显示时间选择器
    const showTimePicker = () => {
      console.log('🕐 显示时间选择器');
      initTimePickerValue();
      showTimePickerModal.value = true;
    };
    
    // 初始化日期选择器值
    const initDatePickerValue = () => {
      let targetDate;
      if (reminderDate.value) {
        targetDate = new Date(`${reminderDate.value}T12:00:00`);
      } else {
        targetDate = new Date();
      }
      
      const year = targetDate.getFullYear();
      const month = targetDate.getMonth() + 1;
      const day = targetDate.getDate();
      
      // 更新days数组
      updateDays(year, month);
      
      // 设置picker值
      datePickerValue.value = [
        years.value.indexOf(year),
        months.value.indexOf(month),
        Math.min(day - 1, days.value.length - 1)
      ];
    };
    
    // 初始化时间选择器值
    const initTimePickerValue = () => {
      let targetTime;
      if (reminderTime.value) {
        const [hour, minute] = reminderTime.value.split(':');
        targetTime = { hour: parseInt(hour), minute: parseInt(minute) };
      } else {
        const defaultDateTime = getDefaultDateTime();
        const [hour, minute] = defaultDateTime.time.split(':');
        targetTime = { hour: parseInt(hour), minute: parseInt(minute) };
      }
      
      timePickerValue.value = [
        hours.value.indexOf(targetTime.hour),
        minutes.value.indexOf(targetTime.minute)
      ];
    };
    
    // 日期选择器变化
    const onDatePickerChange = (e) => {
      const newValues = e.detail.value;
      
      // 当年份或月份变化时，更新天数
      if (newValues[0] !== datePickerValue.value[0] || newValues[1] !== datePickerValue.value[1]) {
        const selectedYear = years.value[newValues[0]];
        const selectedMonth = months.value[newValues[1]];
        updateDays(selectedYear, selectedMonth);
        
        // 如果当前选中的日期超出了新月份的天数，调整到最后一天
        const adjustedDayIndex = Math.min(newValues[2], days.value.length - 1);
        newValues[2] = adjustedDayIndex;
      }
      
      datePickerValue.value = newValues;
    };
    
    // 时间选择器变化
    const onTimePickerChange = (e) => {
      timePickerValue.value = e.detail.value;
    };
    
    // 确认日期选择
    const confirmDatePicker = () => {
      const [yearIndex, monthIndex, dayIndex] = datePickerValue.value;
      const year = years.value[yearIndex];
      const month = months.value[monthIndex];
      const day = days.value[dayIndex];
      
      let selectedDate = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      console.log('📅 用户选择的日期:', selectedDate);
      
      // 如果有时间，检查完整的日期时间是否过短
      if (reminderTime.value) {
        const selectedDateTime = new Date(`${selectedDate}T${reminderTime.value}:00`);
        const currentTime = new Date(); // 实时获取当前时间
        const minTime = new Date(currentTime.getTime()); // 创建当前时间的副本
        minTime.setMinutes(minTime.getMinutes() + 2); // 加2分钟
        
        if (selectedDateTime < minTime) {
          // 如果选择的日期时间过短，自动调整为当前时间+2分钟
          console.log('⚠️ 选择的日期时间过短，自动调整为当前时间+2分钟');
          
          selectedDate = `${minTime.getFullYear()}-${String(minTime.getMonth() + 1).padStart(2, '0')}-${String(minTime.getDate()).padStart(2, '0')}`;
          const adjustedTime = `${String(minTime.getHours()).padStart(2, '0')}:${String(minTime.getMinutes()).padStart(2, '0')}`;
          
          reminderTime.value = adjustedTime;
          console.log('📅 调整后的日期:', selectedDate);
          console.log('🕐 调整后的时间:', adjustedTime);
          
          // 显示友好提示
          uni.showToast({
            title: '已自动调整为2分钟后',
            icon: 'none',
            duration: 2000
          });
        }
      }
      
      reminderDate.value = selectedDate;
      console.log('📅 日期确认:', reminderDate.value);
      
      showDatePickerModal.value = false;
      checkTimeValidity();
      updateEventTime();
    };
    
    // 确认时间选择
    const confirmTimePicker = () => {
      const [hourIndex, minuteIndex] = timePickerValue.value;
      const hour = hours.value[hourIndex];
      const minute = minutes.value[minuteIndex];
      
      let selectedTime = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
      console.log('🕐 用户选择的时间:', selectedTime);
      
      // 实时检查选择的时间是否过短
      const selectedDateTime = new Date(`${reminderDate.value}T${selectedTime}:00`);
      const currentTime = new Date(); // 实时获取当前时间
      const minTime = new Date(currentTime.getTime()); // 创建当前时间的副本
      minTime.setMinutes(minTime.getMinutes() + 2); // 加2分钟
      
      if (selectedDateTime < minTime) {
        // 如果选择的时间过短，自动调整为当前时间+2分钟
        console.log('⚠️ 选择时间过短，自动调整为当前时间+2分钟');
        
        // 检查是否需要跨天
        if (minTime.toDateString() !== currentTime.toDateString()) {
          // 如果跨天了，同时更新日期
          const newDate = `${minTime.getFullYear()}-${String(minTime.getMonth() + 1).padStart(2, '0')}-${String(minTime.getDate()).padStart(2, '0')}`;
          reminderDate.value = newDate;
          console.log('📅 时间跨天，同时更新日期为:', newDate);
        }
        
        selectedTime = `${String(minTime.getHours()).padStart(2, '0')}:${String(minTime.getMinutes()).padStart(2, '0')}`;
        console.log('🕐 调整后的时间:', selectedTime);
        
        // 显示友好提示
        uni.showToast({
          title: '已自动调整为2分钟后',
          icon: 'none',
          duration: 2000
        });
      }
      
      reminderTime.value = selectedTime;
      console.log('🕐 时间确认:', reminderTime.value);
      
      showTimePickerModal.value = false;
      checkTimeValidity();
      updateEventTime();
    };
    
    // 隐藏日期选择器
    const hideDatePicker = () => {
      showDatePickerModal.value = false;
    };
    
    // 隐藏时间选择器
    const hideTimePicker = () => {
      showTimePickerModal.value = false;
    };
    
    // 格式化显示日期
    const getFormattedDate = () => {
      if (!reminderDate.value) {
        return '选择日期';
      }
      
      const date = new Date(`${reminderDate.value}T12:00:00`);
      const now = new Date();
      const tomorrow = new Date(now);
      tomorrow.setDate(tomorrow.getDate() + 1);
      
      // 判断是否是今天、明天
      const isToday = date.toDateString() === now.toDateString();
      const isTomorrow = date.toDateString() === tomorrow.toDateString();
      
      if (isToday) {
        return '今天';
      } else if (isTomorrow) {
        return '明天';
      } else {
        // 格式化为中文日期格式
        const months = ['1月', '2月', '3月', '4月', '5月', '6月', 
                       '7月', '8月', '9月', '10月', '11月', '12月'];
        return `${months[date.getMonth()]}${date.getDate()}日`;
      }
    };
    
    // 格式化显示时间
    const getFormattedTime = () => {
      if (!reminderTime.value) {
        return '选择时间';
      }
      
      const [hour, minute] = reminderTime.value.split(':');
      const h = parseInt(hour);
      const m = parseInt(minute);
      
      if (h < 12) {
        const displayHour = h === 0 ? 12 : h;
        return `上午${displayHour}:${String(m).padStart(2, '0')}`;
      } else {
        const displayHour = h === 12 ? 12 : h - 12;
        return `下午${displayHour}:${String(m).padStart(2, '0')}`;
      }
    };
    
    // 监听props变化
    watch(() => props.initialDate, (newDate) => {
      if (newDate && newDate !== reminderDate.value) {
        reminderDate.value = newDate;
        checkTimeValidity();
        updateEventTime();
      }
    });
    
    watch(() => props.initialTime, (newTime) => {
      if (newTime && newTime !== reminderTime.value) {
        reminderTime.value = newTime;
        checkTimeValidity();
        updateEventTime();
      }
    });
    
    onMounted(() => {
      initializeDateTime();
    });
    
    // 暴露方法供父组件调用
    const setDateTime = (date, time) => {
      if (date) reminderDate.value = date;
      if (time) reminderTime.value = time;
      checkTimeValidity();
      updateEventTime();
    };
    
    const getDateTime = () => {
      return {
        date: reminderDate.value,
        time: reminderTime.value,
        eventTime: reminderDate.value && reminderTime.value ? `${reminderDate.value} ${reminderTime.value}:00` : ''
      };
    };

    return {
      reminderDate,
      reminderTime,
      showDatePickerModal,
      showTimePickerModal,
      datePickerValue,
      timePickerValue,
      showTimeWarning,
      years,
      months,
      days,
      hours,
      minutes,
      showDatePicker,
      showTimePicker,
      onDatePickerChange,
      onTimePickerChange,
      confirmDatePicker,
      confirmTimePicker,
      hideDatePicker,
      hideTimePicker,
      getFormattedDate,
      getFormattedTime,
      setDateTime,
      getDateTime
    };
  }
};
</script>

<style scoped>
.datetime-picker {
  width: 100%;
}

/* 日期时间容器 */
.datetime-container {
  width: 100%;
  background-color: #fcfbf8;
  border-radius: 16rpx;
  overflow: hidden;
}

.date-time-section {
  padding: 24rpx;
}

.section-header {
  margin-bottom: 20rpx;
}

.section-label {
  font-size: 28rpx;
  font-weight: 600;
  color: #1c170d;
  line-height: 1.4;
}

/* 日期时间输入区域 */
.datetime-inputs {
  display: flex;
  gap: 16rpx;
}

.input-item {
  flex: 1;
  background-color: #f4efe7;
  border-radius: 12rpx;
  padding: 20rpx 16rpx;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 120rpx;
}

.input-item:active {
  background-color: #f0e8d8;
  transform: translateY(-2rpx);
  box-shadow: 0 4rpx 16rpx rgba(247, 189, 74, 0.2);
}

.input-label {
  font-size: 24rpx;
  color: #9d8148;
  margin-bottom: 8rpx;
  font-weight: 500;
}

.input-value {
  font-size: 26rpx;
  color: #1c170d;
  font-weight: 600;
  text-align: center;
  line-height: 1.3;
}

/* 时间警告提示 */
.time-warning {
  margin-top: 16rpx;
  padding: 12rpx 16rpx;
  background-color: #fff3cd;
  border-radius: 8rpx;
  border-left: 4rpx solid #ffc107;
}

.warning-text {
  font-size: 24rpx;
  color: #856404;
  line-height: 1.5;
}

/* 选择器弹窗样式 */
.picker-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  z-index: 9999;
}

.picker-modal {
  width: 100%;
  background-color: #ffffff;
  border-radius: 24rpx 24rpx 0 0;
  overflow: hidden;
}

.picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.picker-cancel {
  font-size: 32rpx;
  color: #999999;
  cursor: pointer;
  padding: 8rpx 16rpx;
}

.picker-cancel:active {
  background-color: rgba(153, 153, 153, 0.1);
  border-radius: 8rpx;
}

.picker-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #1c170d;
}

.picker-confirm {
  font-size: 32rpx;
  color: #f7bd4a;
  font-weight: 600;
  padding: 8rpx 16rpx;
  cursor: pointer;
  user-select: none;
  -webkit-user-select: none;
  -webkit-touch-callout: none;
}

.picker-confirm:active {
  background-color: rgba(247, 189, 74, 0.1);
  border-radius: 8rpx;
}

.picker-view {
  height: 480rpx;
  padding: 0 32rpx;
}

.picker-item {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 80rpx;
  font-size: 32rpx;
  color: #1c170d;
}

/* 响应式适配 */
@media (max-width: 750rpx) {
  .date-time-section {
    padding: 20rpx;
  }
  
  .section-label {
    font-size: 26rpx;
  }
  
  .datetime-inputs {
    gap: 12rpx;
  }
  
  .input-item {
    padding: 16rpx 12rpx;
    min-height: 100rpx;
  }
  
  .input-label {
    font-size: 22rpx;
  }
  
  .input-value {
    font-size: 24rpx;
  }
  
  .time-warning {
    margin-top: 12rpx;
    padding: 10rpx 12rpx;
  }
  
  .warning-text {
    font-size: 22rpx;
  }
  
  .picker-view {
    height: 400rpx;
  }
  
  .picker-item {
    height: 64rpx;
    font-size: 28rpx;
  }
}
</style>