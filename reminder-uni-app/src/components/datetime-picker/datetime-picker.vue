<template>
  <view class="datetime-picker">
    <!-- 时间设置 -->
    <view class="setting-item" @click="showDateTimePicker">
      <text class="setting-label">{{ label }}</text>
      <text class="setting-value">{{ getFormattedDateTime() }}</text>
    </view>
    
    <!-- 日期时间选择器弹窗 -->
    <view v-if="showPicker" class="picker-overlay" @click="hidePicker">
      <view class="picker-modal" @click.stop>
        <view class="picker-header">
          <text class="picker-cancel" @click="hidePicker">取消</text>
          <text class="picker-title">选择时间</text>
          <text class="picker-confirm" @click="confirmPicker">确定</text>
        </view>
        
        <picker-view 
          class="picker-view" 
          :value="pickerValue" 
          @change="onPickerChange"
        >
          <!-- 年份 -->
          <picker-view-column v-if="showColumn('year')">
            <view v-for="(year, index) in years" :key="index" class="picker-item">
              {{ year }}年
            </view>
          </picker-view-column>
          
          <!-- 月份 -->
          <picker-view-column v-if="showColumn('month')">
            <view v-for="(month, index) in months" :key="index" class="picker-item">
              {{ month }}月
            </view>
          </picker-view-column>
          
          <!-- 日期 -->
          <picker-view-column v-if="showColumn('day')">
            <view v-for="(day, index) in days" :key="index" class="picker-item">
              {{ day }}日
            </view>
          </picker-view-column>
          
          <!-- 周几 -->
          <picker-view-column v-if="showColumn('weekday')">
            <view v-for="(weekday, index) in weekdays" :key="index" class="picker-item">
              {{ weekday }}
            </view>
          </picker-view-column>
          
          <!-- 小时 -->
          <picker-view-column v-if="showColumn('hour')">
            <view v-for="(hour, index) in hours" :key="index" class="picker-item">
              {{ String(hour).padStart(2, '0') }}时
            </view>
          </picker-view-column>
          
          <!-- 分钟 -->
          <picker-view-column v-if="showColumn('minute')">
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
    }
  },
  emits: ['change', 'dateChange', 'timeChange', 'weekdayChange'],
  setup(props, { emit }) {
    const reminderDate = ref('');
    const reminderTime = ref('');
    const showPicker = ref(false);
    const pickerValue = ref([]); // 动态长度数组
    
    // 生成选择器数据
    const currentYear = new Date().getFullYear();
    const years = ref(Array.from({ length: 10 }, (_, i) => currentYear + i));
    const months = ref(Array.from({ length: 12 }, (_, i) => i + 1));
    const days = ref([]);
    const hours = ref(Array.from({ length: 24 }, (_, i) => i));
    const minutes = ref(Array.from({ length: 60 }, (_, i) => i));
    const weekdays = ref(['周日', '周一', '周二', '周三', '周四', '周五', '周六']);
    
    // 根据mode和columns确定显示的列
    const displayColumns = computed(() => {
      if (props.mode === 'date') {
        return ['year', 'month', 'day'];
      } else if (props.mode === 'time') {
        return ['hour', 'minute'];
      } else if (props.mode === 'datetime') {
        return props.columns;
      }
      return props.columns;
    });
    
    // 检查是否显示某列
    const showColumn = (column) => {
      return displayColumns.value.includes(column);
    };
    
    // 初始化日期时间
    const initializeDateTime = () => {
      console.log('datetime-picker: 开始初始化, props:', { 
        initialDate: props.initialDate, 
        initialTime: props.initialTime,
        autoSetDefault: props.autoSetDefault 
      });
      
      // 初始化日期 - 优先使用props，否则设置默认值
      if (props.initialDate) {
        reminderDate.value = props.initialDate;
        console.log('datetime-picker: 使用传入的初始日期:', props.initialDate);
      } else if (props.autoSetDefault && !reminderDate.value) {
        // 只有在autoSetDefault=true且当前没有值时才设置默认值
        const today = new Date();
        reminderDate.value = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
        console.log('datetime-picker: 使用默认日期:', reminderDate.value);
      } else {
        console.log('datetime-picker: 保持现有日期值:', reminderDate.value);
      }
      
      // 初始化时间 - 优先使用props，否则设置默认值
      if (props.initialTime) {
        reminderTime.value = props.initialTime;
        console.log('datetime-picker: 使用传入的初始时间:', props.initialTime);
      } else if (props.autoSetDefault && !reminderTime.value) {
        // 只有在autoSetDefault=true且当前没有值时才设置默认值
        const now = new Date();
        now.setHours(now.getHours() + 1);
        now.setMinutes(0);
        now.setSeconds(0);
        reminderTime.value = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
        console.log('datetime-picker: 使用默认时间:', reminderTime.value);
      } else {
        console.log('datetime-picker: 保持现有时间值:', reminderTime.value);
      }
      
      console.log('datetime-picker: 初始化完成，最终值:', { date: reminderDate.value, time: reminderTime.value });
      
      // 初始化后触发一次change事件
      updateEventTime();
    };
    
    onMounted(() => {
      initializeDateTime();
    });
    
    // 监听props变化 - 支持动态更新
    watch(() => props.initialDate, (newDate, oldDate) => {
      console.log('datetime-picker: initialDate watch 触发:', { newDate, oldDate, current: reminderDate.value });
      if (newDate && newDate !== reminderDate.value) {
        console.log('datetime-picker: 更新日期从', reminderDate.value, '到', newDate);
        reminderDate.value = newDate;
        updateEventTime();
      }
    });
    
    watch(() => props.initialTime, (newTime, oldTime) => {
      console.log('datetime-picker: initialTime watch 触发:', { newTime, oldTime, current: reminderTime.value });
      if (newTime && newTime !== reminderTime.value) {
        console.log('datetime-picker: 更新时间从', reminderTime.value, '到', newTime);
        reminderTime.value = newTime;
        updateEventTime();
      }
    });
    
    // 日期变化处理
    const onDateChange = (e) => {
      reminderDate.value = e.detail.value;
      updateEventTime();
      emit('dateChange', reminderDate.value);
    };
    
    // 时间变化处理
    const onTimeChange = (e) => {
      reminderTime.value = e.detail.value;
      updateEventTime();
      emit('timeChange', reminderTime.value);
    };
    
    // 更新完整的事件时间
    const updateEventTime = () => {
      if (reminderDate.value && reminderTime.value) {
        const eventTime = `${reminderDate.value} ${reminderTime.value}:00`;
        emit('change', {
          date: reminderDate.value,
          time: reminderTime.value,
          eventTime: eventTime
        });
      }
    };
    
    // 显示日期时间选择器
    const showDateTimePicker = () => {
      initPickerValue();
      showPicker.value = true;
    };
    
    // 初始化picker值
    const initPickerValue = () => {
      let targetDate;
      if (reminderDate.value && reminderTime.value) {
        targetDate = new Date(`${reminderDate.value}T${reminderTime.value}:00`);
      } else {
        targetDate = new Date();
        targetDate.setHours(targetDate.getHours() + 1);
        targetDate.setMinutes(0);
      }
      
      const year = targetDate.getFullYear();
      const month = targetDate.getMonth() + 1;
      const day = targetDate.getDate();
      const hour = targetDate.getHours();
      const minute = targetDate.getMinutes();
      
      // 更新days数组
      if (showColumn('day')) {
        updateDays(year, month);
      }
      
      // 根据显示的列构建picker值数组
      const values = [];
      displayColumns.value.forEach(column => {
        switch (column) {
          case 'year':
            values.push(years.value.indexOf(year));
            break;
          case 'month':
            values.push(months.value.indexOf(month));
            break;
          case 'day':
            values.push(Math.min(day - 1, days.value.length - 1));
            break;
          case 'weekday':
            values.push(targetDate.getDay()); // 0-6，对应周日到周六
            break;
          case 'hour':
            values.push(hours.value.indexOf(hour));
            break;
          case 'minute':
            values.push(minutes.value.indexOf(minute));
            break;
        }
      });
      
      pickerValue.value = values;
    };
    
    // 更新天数
    const updateDays = (year, month) => {
      const daysInMonth = new Date(year, month, 0).getDate();
      days.value = Array.from({ length: daysInMonth }, (_, i) => i + 1);
    };
    
    // picker值变化
    const onPickerChange = (e) => {
      const newValues = e.detail.value;
      
      // 解析当前选中的值
      const selectedValues = {};
      displayColumns.value.forEach((column, index) => {
        switch (column) {
          case 'year':
            selectedValues.year = years.value[newValues[index]];
            selectedValues.yearIndex = newValues[index];
            break;
          case 'month':
            selectedValues.month = months.value[newValues[index]];
            selectedValues.monthIndex = newValues[index];
            break;
          case 'day':
            selectedValues.dayIndex = newValues[index];
            break;
          case 'weekday':
            selectedValues.weekday = newValues[index]; // 0-6
            selectedValues.weekdayIndex = newValues[index];
            break;
          case 'hour':
            selectedValues.hourIndex = newValues[index];
            break;
          case 'minute':
            selectedValues.minuteIndex = newValues[index];
            break;
        }
      });
      
      // 当年份或月份变化时，更新天数
      if (showColumn('year') && showColumn('month') && showColumn('day')) {
        const yearIndex = displayColumns.value.indexOf('year');
        const monthIndex = displayColumns.value.indexOf('month');
        const dayIndex = displayColumns.value.indexOf('day');
        
        if (yearIndex !== -1 && monthIndex !== -1 && dayIndex !== -1) {
          if (newValues[yearIndex] !== pickerValue.value[yearIndex] || 
              newValues[monthIndex] !== pickerValue.value[monthIndex]) {
            updateDays(selectedValues.year, selectedValues.month);
            
            // 如果当前选中的日期超出了新月份的天数，调整到最后一天
            const adjustedDayIndex = Math.min(selectedValues.dayIndex, days.value.length - 1);
            newValues[dayIndex] = adjustedDayIndex;
          }
        }
      }
      
      pickerValue.value = newValues;
    };
    
    // 确认选择
    const confirmPicker = () => {
      // 解析选中的值
      const selectedValues = {};
      displayColumns.value.forEach((column, index) => {
        switch (column) {
          case 'year':
            selectedValues.year = years.value[pickerValue.value[index]];
            break;
          case 'month':
            selectedValues.month = months.value[pickerValue.value[index]];
            break;
          case 'day':
            selectedValues.day = days.value[pickerValue.value[index]];
            break;
          case 'weekday':
            selectedValues.weekday = pickerValue.value[index]; // 0-6
            break;
          case 'hour':
            selectedValues.hour = hours.value[pickerValue.value[index]];
            break;
          case 'minute':
            selectedValues.minute = minutes.value[pickerValue.value[index]];
            break;
        }
      });
      
      // 根据模式更新日期和时间
      if (props.mode === 'date' || (props.mode === 'datetime' && showColumn('year') && showColumn('month') && showColumn('day'))) {
        const year = selectedValues.year || new Date().getFullYear();
        const month = selectedValues.month || (new Date().getMonth() + 1);
        const day = selectedValues.day || new Date().getDate();
        reminderDate.value = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        emit('dateChange', reminderDate.value);
      }
      
      if (props.mode === 'time' || (props.mode === 'datetime' && showColumn('hour') && showColumn('minute'))) {
        const hour = selectedValues.hour !== undefined ? selectedValues.hour : new Date().getHours();
        const minute = selectedValues.minute !== undefined ? selectedValues.minute : 0;
        reminderTime.value = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
        emit('timeChange', reminderTime.value);
      }
      
      // 处理特殊情况：只有部分列的组合
      if (showColumn('month') && showColumn('day') && !showColumn('year')) {
        // 月日组合，使用当前年份
        const year = new Date().getFullYear();
        const month = selectedValues.month || (new Date().getMonth() + 1);
        const day = selectedValues.day || new Date().getDate();
        reminderDate.value = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        emit('dateChange', reminderDate.value);
      } else if (showColumn('day') && !showColumn('month') && !showColumn('year')) {
        // 只有日，使用当前年月
        const now = new Date();
        const year = now.getFullYear();
        const month = now.getMonth() + 1;
        const day = selectedValues.day || now.getDate();
        reminderDate.value = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        emit('dateChange', reminderDate.value);
      }
      
      // 发送weekday变化事件
      if (showColumn('weekday')) {
        emit('weekdayChange', selectedValues.weekday);
      }
      
      updateEventTime();
      
      showPicker.value = false;
    };
    
    // 隐藏选择器
    const hidePicker = () => {
      showPicker.value = false;
    };
    
    // 格式化显示日期时间
    const getFormattedDateTime = () => {
      if (!reminderDate.value || !reminderTime.value) {
        return '选择时间';
      }
      
      // 使用iOS兼容的日期格式创建Date对象
      const dateTimeStr = `${reminderDate.value}T${reminderTime.value}:00`;
      const date = new Date(dateTimeStr);
      
      // 检查日期是否有效
      if (isNaN(date.getTime())) {
        console.error('无效的日期格式:', dateTimeStr);
        return '选择时间';
      }
      
      const now = new Date();
      const tomorrow = new Date(now);
      tomorrow.setDate(tomorrow.getDate() + 1);
      
      // 判断是否是今天、明天
      const isToday = date.toDateString() === now.toDateString();
      const isTomorrow = date.toDateString() === tomorrow.toDateString();
      
      let dateStr = '';
      if (isToday) {
        dateStr = '今天';
      } else if (isTomorrow) {
        dateStr = '明天';
      } else {
        // 格式化为中文日期格式
        const months = ['1月', '2月', '3月', '4月', '5月', '6月', 
                       '7月', '8月', '9月', '10月', '11月', '12月'];
        dateStr = `${months[date.getMonth()]}${date.getDate()}日`;
      }
      
      // 格式化时间为中文格式
      let hours = date.getHours();
      const minutes = date.getMinutes();
      let timeStr = '';
      
      if (hours < 12) {
        const displayHour = hours === 0 ? 12 : hours;
        timeStr = `上午${displayHour}时${String(minutes).padStart(2, '0')}分`;
      } else {
        const displayHour = hours === 12 ? 12 : hours - 12;
        timeStr = `下午${displayHour}时${String(minutes).padStart(2, '0')}分`;
      }
      
      return `${dateStr} ${timeStr}`;
    };
    
    // 暴露方法供父组件调用
    const setDateTime = (date, time) => {
      console.log('datetime-picker: setDateTime 被调用:', { date, time });
      if (date) {
        reminderDate.value = date;
        console.log('datetime-picker: 设置日期为:', date);
      }
      if (time) {
        reminderTime.value = time;
        console.log('datetime-picker: 设置时间为:', time);
      }
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
      showPicker,
      pickerValue,
      years,
      months,
      days,
      hours,
      minutes,
      weekdays,
      displayColumns,
      showColumn,
      onDateChange,
      onTimeChange,
      showDateTimePicker,
      onPickerChange,
      confirmPicker,
      hidePicker,
      getFormattedDateTime,
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

/* 设置项样式 */
.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32rpx;
  background-color: #fcfbf8;
  padding: 32rpx;
  min-height: 112rpx;
  cursor: pointer;
}

.setting-item:active {
  background-color: #f4efe7;
}

.setting-label {
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
  line-height: 1.4;
  flex: 1;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}

.setting-value {
  font-size: 32rpx;
  font-weight: 400;
  color: #1c170d;
  line-height: 1.4;
  flex-shrink: 0;
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
  z-index: 1000;
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
  .setting-item {
    padding: 24rpx;
    min-height: 96rpx;
  }
  
  .setting-label,
  .setting-value {
    font-size: 28rpx;
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