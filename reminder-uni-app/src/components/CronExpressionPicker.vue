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
      
      <!-- 选择内容区域 -->
      <view class="content-section">
        <!-- 时间选择区域 -->
        <view class="time-section">
          <view class="setting-item" @click="showTimePicker">
            <text class="setting-label">触发时间</text>
            <text class="setting-value">{{ getFormattedTime() }}</text>
          </view>
        </view>
        
        <!-- 时间选择器弹窗 -->
        <view v-if="showTimePickerModal" class="time-picker-overlay" @click="hideTimePicker">
          <view class="time-picker-modal" @click.stop>
            <view class="time-picker-header">
              <text class="time-picker-cancel" @click="hideTimePicker">取消</text>
              <text class="time-picker-title">选择时间</text>
              <text class="time-picker-confirm" @click="confirmTimePicker">确定</text>
            </view>
            
            <picker-view 
              class="time-picker-view" 
              :value="timePickerValue" 
              @change="onTimePickerChange"
            >
              <!-- 小时 -->
              <picker-view-column>
                <view v-for="(hour, index) in timeHours" :key="index" class="time-picker-item">
                  {{ String(hour).padStart(2, '0') }}时
                </view>
              </picker-view-column>
              
              <!-- 分钟 -->
              <picker-view-column>
                <view v-for="(minute, index) in timeMinutes" :key="index" class="time-picker-item">
                  {{ String(minute).padStart(2, '0') }}分
                </view>
              </picker-view-column>
            </picker-view>
          </view>
        </view>
        
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
          <view class="level-section" :class="{ disabled: selectedDays.length > 0 }">
            <view class="level-header" @click="toggleWeekCollapse">
              <view class="header-left">
                <text class="section-title">按星期重复</text>
                <text v-if="selectedDays.length > 0" class="disabled-hint">（已选择日期）</text>
              </view>
              <view class="header-actions">
                <text v-if="selectedWeekdays.length > 0" class="clear-btn" @click.stop="clearWeekdays">清空</text>
                <text class="collapse-icon" :class="{ collapsed: isWeekCollapsed }">▼</text>
              </view>
            </view>
            <view v-if="!isWeekCollapsed" class="options-grid">
              <view 
                v-for="weekday in weekdayOptions" 
                :key="weekday.value"
                class="option-btn"
                :class="{ 
                  selected: selectedWeekdays.includes(weekday.value),
                  disabled: selectedDays.length > 0
                }"
                @click="toggleWeekday(weekday.value)"
              >
                <text class="option-text">{{ weekday.label }}</text>
              </view>
            </view>
          </view>
          
          <!-- 日选择 -->
          <view class="level-section" :class="{ disabled: selectedWeekdays.length > 0 }">
            <view class="level-header" @click="toggleDayCollapse">
              <view class="header-left">
                <text class="section-title">按日期重复</text>
                <text v-if="selectedWeekdays.length > 0" class="disabled-hint">（已选择星期）</text>
              </view>
              <view class="header-actions">
                <text v-if="selectedDays.length > 0" class="clear-btn" @click.stop="clearDays">清空</text>
                <text class="collapse-icon" :class="{ collapsed: isDayCollapsed }">▼</text>
              </view>
            </view>
            <view v-if="!isDayCollapsed" class="options-grid day-grid">
              <view 
                v-for="day in dayOptions" 
                :key="day"
                class="option-btn"
                :class="{ 
                  selected: selectedDays.includes(day),
                  disabled: selectedWeekdays.length > 0
                }"
                @click="toggleDay(day)"
              >
                <text class="option-text">{{ day }}</text>
              </view>
            </view>
          </view>
        </view>
        
        <!-- 月选择 - 只显示日期 -->
        <view v-if="currentType === 'month'" class="selection-area">
          <!-- 日选择 -->
          <view class="level-section">
            <view class="level-header">
              <text class="section-title">选择日期</text>
            </view>
            <view class="options-grid day-grid">
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
    
    // 时间选择器相关
    const showTimePickerModal = ref(false);
    const timePickerValue = ref([9, 0]); // [小时索引, 分钟索引]
    const timeHours = ref(Array.from({ length: 24 }, (_, i) => i));
    const timeMinutes = ref(Array.from({ length: 60 }, (_, i) => i));
    
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
        console.log('解析初始Cron表达式:', props.initialValue, '分割结果:', parts);
        
        if (parts.length >= 3) {
          let second, minute, hour, day, month, weekday, year;
          
          if (parts.length === 5) {
            // 5位格式: 分 时 日 月 周
            [minute, hour, day, month, weekday] = parts;
            second = '0';
            year = '*';
          } else if (parts.length === 6) {
            // 6位格式: 秒 分 时 日 月 周
            [second, minute, hour, day, month, weekday] = parts;
            year = '*';
          } else if (parts.length === 7) {
            // 7位格式: 秒 分 时 日 月 周 年
            [second, minute, hour, day, month, weekday, year] = parts;
          }
          
          console.log('解析结果:', { second, minute, hour, day, month, weekday, year });
          
          // 设置时间
          if (hour && minute) {
            selectedTime.value = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
          }
          
          // 根据Cron表达式判断类型并设置选择状态
          if (month !== '*' && month !== '?' && month.trim() !== '') {
            // 年重复 - 优先检查年重复
            currentType.value = 'year';
            selectedMonths.value = month.split(',').map(m => parseInt(m.trim())).filter(m => !isNaN(m));
            
            // 检查是使用星期还是日期
            if (weekday !== '?' && weekday !== '*' && weekday.trim() !== '') {
              // 年重复 + 星期模式
              // 将Cron星期值(1-7)转换为JavaScript星期值(0-6)
              selectedWeekdays.value = weekday.split(',')
                .map(w => parseInt(w.trim()))
                .filter(w => !isNaN(w))
                .map(cronWeekday => {
                  // Cron: 1=周日, 2=周一, 3=周二, 4=周三, 5=周四, 6=周五, 7=周六
                  // JS:   0=周日, 1=周一, 2=周二, 3=周三, 4=周四, 5=周五, 6=周六
                  return cronWeekday === 1 ? 0 : cronWeekday === 7 ? 6 : cronWeekday - 1;
                });
              selectedDays.value = []; // 清空日期选择
              console.log('识别为年重复（星期模式），选择的月份:', selectedMonths.value, '选择的星期:', selectedWeekdays.value);
            } else if (day !== '?' && day !== '*' && day.trim() !== '') {
              // 年重复 + 日期模式
              selectedDays.value = day.split(',').map(d => parseInt(d.trim())).filter(d => !isNaN(d));
              selectedWeekdays.value = []; // 清空星期选择
              console.log('识别为年重复（日期模式），选择的月份:', selectedMonths.value, '选择的日期:', selectedDays.value);
            } else {
              // 年重复，默认使用日期模式
              selectedDays.value = [1];
              selectedWeekdays.value = [];
              console.log('识别为年重复（默认日期模式），选择的月份:', selectedMonths.value);
            }
          } else if (weekday !== '?' && weekday !== '*' && weekday.trim() !== '') {
            // 周重复
            currentType.value = 'week';
            // 将Cron星期值(1-7)转换为JavaScript星期值(0-6)
            selectedWeekdays.value = weekday.split(',')
              .map(w => parseInt(w.trim()))
              .filter(w => !isNaN(w))
              .map(cronWeekday => {
                // Cron: 1=周日, 2=周一, 3=周二, 4=周三, 5=周四, 6=周五, 7=周六
                // JS:   0=周日, 1=周一, 2=周二, 3=周三, 4=周四, 5=周五, 6=周六
                return cronWeekday === 1 ? 0 : cronWeekday === 7 ? 6 : cronWeekday - 1;
              });
            console.log('识别为周重复，选择的星期:', selectedWeekdays.value);
          } else if (day !== '?' && day !== '*' && day.trim() !== '') {
            // 月重复
            currentType.value = 'month';
            selectedDays.value = day.split(',').map(d => parseInt(d.trim())).filter(d => !isNaN(d));
            console.log('识别为月重复，选择的日期:', selectedDays.value);
          } else {
            // 默认为月重复
            currentType.value = 'month';
            if (selectedDays.value.length === 0) {
              selectedDays.value = [1]; // 默认每月1号
            }
            console.log('默认设置为月重复');
          }
          
          // 根据类型设置折叠状态
          if (currentType.value === 'year') {
            isMonthCollapsed.value = false; // 年模式展开月份选择
            isWeekCollapsed.value = true;
            isDayCollapsed.value = true;
          } else if (currentType.value === 'month') {
            isMonthCollapsed.value = true;
            isWeekCollapsed.value = true;
            isDayCollapsed.value = false; // 月模式展开日期选择
          } else if (currentType.value === 'week') {
            isMonthCollapsed.value = true;
            isWeekCollapsed.value = false; // 周模式展开星期选择
            isDayCollapsed.value = true;
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
      if (type !== 'month' && type !== 'year') selectedDays.value = [];
      if (type !== 'week' && type !== 'year') selectedWeekdays.value = [];
      
      // 根据类型设置折叠状态
      if (type === 'year') {
        // 年模式：月份展开，其他折叠
        isMonthCollapsed.value = false;
        isWeekCollapsed.value = true;
        isDayCollapsed.value = true;
      } else if (type === 'month') {
        // 月模式：只显示日期，不需要折叠状态
        isMonthCollapsed.value = true;
        isWeekCollapsed.value = true;
        isDayCollapsed.value = false;
        // 确保月模式下没有星期选择
        selectedWeekdays.value = [];
      } else if (type === 'week') {
        // 周模式：只显示星期选择
        isMonthCollapsed.value = true;
        isWeekCollapsed.value = false;
        isDayCollapsed.value = true;
        // 确保周模式下没有日期选择
        selectedDays.value = [];
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
      // 在年模式中，如果已经选择了星期，阻止选择日期
      if (currentType.value === 'year' && selectedWeekdays.value.length > 0) {
        uni.showToast({
          title: '请先清空星期选择',
          icon: 'none',
          duration: 2000
        });
        return;
      }
      
      const index = selectedDays.value.indexOf(day);
      if (index > -1) {
        selectedDays.value.splice(index, 1);
      } else {
        selectedDays.value.push(day);
        // 在年模式中，如果选择了日期，清空星期选择（因为它们在Cron中是互斥的）
        if (currentType.value === 'year') {
          selectedWeekdays.value = [];
        }
      }
    };
    
    // 切换星期选择
    const toggleWeekday = (weekday) => {
      // 在年模式中，如果已经选择了日期，阻止选择星期
      if (currentType.value === 'year' && selectedDays.value.length > 0) {
        uni.showToast({
          title: '请先清空日期选择',
          icon: 'none',
          duration: 2000
        });
        return;
      }
      
      const index = selectedWeekdays.value.indexOf(weekday);
      if (index > -1) {
        selectedWeekdays.value.splice(index, 1);
      } else {
        selectedWeekdays.value.push(weekday);
        // 在年模式中，如果选择了星期，清空日期选择（因为它们在Cron中是互斥的）
        if (currentType.value === 'year') {
          selectedDays.value = [];
        }
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
      // 5位格式: 分 时 日 月 周 (保持兼容性)
      // 从选择的时间中提取小时和分钟
      const [hour, minute] = selectedTime.value.split(':');
      
      console.log('生成cron表达式，当前类型:', currentType.value);
      console.log('选择的时间:', selectedTime.value);
      console.log('选择的月份:', selectedMonths.value);
      console.log('选择的日期:', selectedDays.value);
      console.log('选择的星期:', selectedWeekdays.value);
      
      if (currentType.value === 'year') {
        // 年度重复：根据选择的内容生成不同的表达式
        const months = selectedMonths.value.length > 0 ? selectedMonths.value.join(',') : '1'; // 默认1月
        
        if (selectedWeekdays.value.length > 0) {
          // 如果选择了星期，则使用星期模式（日期用?）
          // 将JavaScript星期值(0-6)转换为Cron星期值(1-7)
          const cronWeekdays = selectedWeekdays.value.map(jsWeekday => {
            return jsWeekday === 6 ? 7 : jsWeekday + 1;
          });
          const weekdays = cronWeekdays.join(',');
          return `${minute} ${hour} ? ${months} ${weekdays}`;
        } else {
          // 如果没有选择星期，则使用日期模式（星期用?）
          const days = selectedDays.value.length > 0 ? selectedDays.value.join(',') : '1'; // 默认1号
          return `${minute} ${hour} ${days} ${months} ?`;
        }
      } else if (currentType.value === 'month') {
        // 月度重复：每月指定日期
        const days = selectedDays.value.length > 0 ? selectedDays.value.join(',') : '1'; // 默认1号
        return `${minute} ${hour} ${days} * ?`;
      } else if (currentType.value === 'week') {
        // 周度重复：每周指定星期
        // 将JavaScript星期值(0-6)转换为Cron星期值(1-7)
        if (selectedWeekdays.value.length > 0) {
          const cronWeekdays = selectedWeekdays.value.map(jsWeekday => {
            return jsWeekday === 6 ? 7 : jsWeekday + 1;
          });
          const weekdays = cronWeekdays.join(',');
          return `${minute} ${hour} ? * ${weekdays}`;
        } else {
          return `${minute} ${hour} ? * 2`; // 默认周一(Cron值2)
        }
      }
      
      return `${minute} ${hour} * * ?`; // 默认每天
    };

    // 清空星期选择
    const clearWeekdays = () => {
      selectedWeekdays.value = [];
      selectedDays.value = [];
    };

    // 清空日期选择
    const clearDays = () => {
      selectedDays.value = [];
    };

    // 时间选择器方法
    const showTimePicker = () => {
      // 解析当前时间到picker值
      const [hour, minute] = selectedTime.value.split(':');
      timePickerValue.value = [parseInt(hour), parseInt(minute)];
      showTimePickerModal.value = true;
    };

    const hideTimePicker = () => {
      showTimePickerModal.value = false;
    };

    const onTimePickerChange = (e) => {
      timePickerValue.value = e.detail.value;
    };

    const confirmTimePicker = () => {
      const [hourIndex, minuteIndex] = timePickerValue.value;
      const hour = timeHours.value[hourIndex];
      const minute = timeMinutes.value[minuteIndex];
      selectedTime.value = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
      showTimePickerModal.value = false;
    };

    const getFormattedTime = () => {
      if (!selectedTime.value) return '选择时间';
      
      const [hour, minute] = selectedTime.value.split(':');
      const hourNum = parseInt(hour);
      const minuteNum = parseInt(minute);
      
      if (hourNum < 12) {
        const displayHour = hourNum === 0 ? 12 : hourNum;
        return `上午${displayHour}:${String(minuteNum).padStart(2, '0')}`;
      } else {
        const displayHour = hourNum === 12 ? 12 : hourNum - 12;
        return `下午${displayHour}:${String(minuteNum).padStart(2, '0')}`;
      }
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
      handleConfirm,
      clearWeekdays,
      clearDays,
      showTimePickerModal,
      timePickerValue,
      timeHours,
      timeMinutes,
      showTimePicker,
      hideTimePicker,
      onTimePickerChange,
      confirmTimePicker,
      getFormattedTime
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
  padding: 0 0 32rpx 0;
  margin-bottom: 24rpx;
  background-color: #fcfbf8;
  border-bottom: 1rpx solid #e9e0ce;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 32rpx;
  background-color: #f4efe7;
  border-radius: 20rpx;
  cursor: pointer;
  transition: all 0.2s ease;
}

.setting-item:active {
  background-color: #f0ede4;
}

.setting-label {
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
}

.setting-value {
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
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

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  flex: 1;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.clear-btn {
  font-size: 24rpx;
  color: #f7bd4a;
  font-weight: 500;
  padding: 8rpx 16rpx;
  background-color: rgba(247, 189, 74, 0.1);
  border-radius: 12rpx;
  transition: all 0.2s ease;
}

.clear-btn:active {
  background-color: rgba(247, 189, 74, 0.2);
  transform: scale(0.95);
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

/* 禁用状态样式 */
.level-section.disabled {
  opacity: 0.5;
  pointer-events: none;
}

.level-section.disabled .section-title {
  color: #9d8148;
}

.disabled-hint {
  font-size: 24rpx;
  color: #9d8148;
  font-style: italic;
  margin-left: 16rpx;
}

.option-btn.disabled {
  background-color: #f5f5f5;
  border-color: #e0e0e0;
  opacity: 0.6;
  pointer-events: none;
  cursor: not-allowed;
}

.option-btn.disabled .option-text {
  color: #999999;
}

.option-btn.disabled:hover {
  background-color: #f5f5f5;
  transform: none;
}

/* 时间选择器弹窗样式 */
.time-picker-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  z-index: 10000;
}

.time-picker-modal {
  width: 100%;
  background-color: #ffffff;
  border-radius: 24rpx 24rpx 0 0;
  overflow: hidden;
}

.time-picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.time-picker-cancel {
  font-size: 32rpx;
  color: #999999;
}

.time-picker-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #1c170d;
}

.time-picker-confirm {
  font-size: 32rpx;
  color: #f7bd4a;
  font-weight: 600;
}

.time-picker-view {
  height: 480rpx;
  padding: 0 32rpx;
}

.time-picker-item {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 80rpx;
  font-size: 32rpx;
  color: #1c170d;
}
</style> 