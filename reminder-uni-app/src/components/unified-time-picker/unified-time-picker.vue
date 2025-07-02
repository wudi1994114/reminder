<template>
  <view class="unified-time-picker">
    <!-- 时间设置按钮 -->
    <view class="setting-item" @click="showPicker">
      <text class="setting-label">{{ label }}</text>
      <text class="setting-value">{{ displayText }}</text>
    </view>
    
    <!-- 弹窗内容 -->
    <view class="picker-overlay" v-if="show" @touchmove.stop.prevent @click.self="handleCancel">
      <view class="picker-container" @click.stop>
        <!-- 顶部标题栏 -->
        <view class="header-section">
          <view class="header-content">
            <view class="cancel-btn" @click="handleCancel">
              <text class="btn-text">取消</text>
            </view>
            <text class="title">{{ pickerTitle }}</text>
            <view class="confirm-btn" @click="handleConfirm">
              <text class="btn-text">确定</text>
            </view>
          </view>
        </view>
        
        <!-- 模式切换标签 -->
        <view class="mode-tabs" v-if="showModeSwitch">
          <view 
            class="mode-tab" 
            :class="{ active: currentMode === 'simple' }"
            @click="switchMode('simple')"
          >
            <text class="tab-text">一次性</text>
          </view>
          <view 
            class="mode-tab" 
            :class="{ active: currentMode === 'repeat' }"
            @click="switchMode('repeat')"
          >
            <text class="tab-text">重复</text>
          </view>
        </view>
        
        <!-- 内容区域 -->
        <scroll-view class="content-section" scroll-y>
          <!-- 简单模式内容 -->
          <view v-if="currentMode === 'simple'" class="simple-mode-content">
            <!-- 日期选择 -->
            <view class="input-group">
              <view class="input-label">
                <text class="label-text">日期</text>
              </view>
              <picker mode="date" :value="tempData.date" @change="onDateChange">
                <view class="picker-display">
                  <text class="picker-text">{{ formatDate(tempData.date) || '选择日期' }}</text>
                  <text class="picker-arrow">▼</text>
                </view>
              </picker>
            </view>
            
            <!-- 时间选择 -->
            <view class="input-group">
              <view class="input-label">
                <text class="label-text">时间</text>
              </view>
              <picker mode="time" :value="tempData.time" @change="onTimeChange">
                <view class="picker-display">
                  <text class="picker-text">{{ tempData.time || '选择时间' }}</text>
                  <text class="picker-arrow">▼</text>
                </view>
              </picker>
            </view>
          </view>
          
          <!-- 重复模式内容 -->
          <view v-if="currentMode === 'repeat'" class="repeat-mode-content">
            <!-- 重复类型选择 -->
            <view class="type-selection">
              <view 
                v-for="(type, index) in repeatTypes" 
                :key="type.value"
                class="type-btn"
                :class="{ active: tempData.type === type.value }"
                @click="selectRepeatType(type.value)"
              >
                <text class="type-text">{{ type.label }}</text>
              </view>
            </view>
            
            <!-- 时间设置 -->
            <view class="input-group">
              <view class="input-label">
                <text class="label-text">触发时间</text>
              </view>
              <picker mode="time" :value="formatTime(tempData.hour, tempData.minute)" @change="onRepeatTimeChange">
                <view class="picker-display">
                  <text class="picker-text">{{ formatTime(tempData.hour, tempData.minute) }}</text>
                  <text class="picker-arrow">▼</text>
                </view>
              </picker>
            </view>
            
            <!-- 根据重复类型显示不同的选项 -->
            <view v-if="tempData.type === 'WEEKLY'" class="weekday-selection">
              <view class="input-label">
                <text class="label-text">选择星期</text>
              </view>
              <view class="weekday-grid">
                <view 
                  v-for="(day, index) in weekDays" 
                  :key="index"
                  class="weekday-btn"
                  :class="{ selected: tempData.weekdays.includes(index) }"
                  @click="toggleWeekday(index)"
                >
                  <text class="weekday-text">{{ day }}</text>
                </view>
              </view>
            </view>
            
            <!-- 月度选择 -->
            <view v-if="tempData.type === 'MONTHLY'" class="day-selection">
              <view class="input-label">
                <text class="label-text">选择日期</text>
              </view>
              <scroll-view class="day-grid-scroll" scroll-x>
                <view class="day-grid">
                  <view 
                    v-for="day in 31" 
                    :key="day"
                    class="day-btn"
                    :class="{ selected: tempData.monthDays.includes(day) }"
                    @click="toggleMonthDay(day)"
                  >
                    <text class="day-text">{{ day }}</text>
                  </view>
                </view>
              </scroll-view>
            </view>
          </view>
          
          <!-- 预览区域 -->
          <view class="preview-section" v-if="previewTimes.length > 0">
            <view class="preview-header">
              <text class="preview-title">预览</text>
            </view>
            <view class="preview-list">
              <view v-for="(time, index) in previewTimes.slice(0, 3)" :key="index" class="preview-item">
                <text class="preview-text">{{ time }}</text>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script>
import { ref, computed, watch, reactive } from 'vue';
import timeManager from '../../utils/timeManager';

const { TimeType, UnifiedTimeData, generateDescription, generatePreviewTimes, generateCronExpression } = timeManager;

export default {
  name: 'UnifiedTimePicker',
  props: {
    // 显示标签
    label: {
      type: String,
      default: '时间设置'
    },
    // 弹窗标题
    pickerTitle: {
      type: String,
      default: '选择时间'
    },
    // 是否显示模式切换
    showModeSwitch: {
      type: Boolean,
      default: true
    },
    // 初始模式
    initialMode: {
      type: String,
      default: 'simple',
      validator: (value) => ['simple', 'repeat'].includes(value)
    },
    // 初始值
    modelValue: {
      type: Object,
      default: () => new UnifiedTimeData()
    }
  },
  emits: ['update:modelValue', 'change', 'confirm', 'cancel', 'picker-show', 'picker-hide'],
  
  setup(props, { emit }) {
    // 显示状态
    const show = ref(false);
    
    // 当前模式
    const currentMode = ref(props.initialMode);
    
    // 临时数据（编辑中的数据）
    const tempData = reactive(new UnifiedTimeData());
    
    // 原始数据（用于取消时恢复）
    const originalData = ref(null);
    
    // 重复类型选项
    const repeatTypes = [
      { value: TimeType.DAILY, label: '每天' },
      { value: TimeType.WEEKLY, label: '每周' },
      { value: TimeType.MONTHLY, label: '每月' },
      { value: TimeType.YEARLY, label: '每年' }
    ];
    
    // 星期选项
    const weekDays = ['日', '一', '二', '三', '四', '五', '六'];
    
    // 显示文本
    const displayText = computed(() => {
      return generateDescription(props.modelValue) || '选择时间';
    });
    
    // 预览时间
    const previewTimes = computed(() => {
      return generatePreviewTimes(tempData, 5);
    });
    
    // 显示选择器
    const showPicker = () => {
      // 保存原始数据
      originalData.value = JSON.parse(JSON.stringify(props.modelValue));
      
      // 复制数据到临时编辑对象
      Object.assign(tempData, props.modelValue);
      
      // 根据数据类型设置模式
      if (tempData.type === TimeType.ONCE) {
        currentMode.value = 'simple';
      } else {
        currentMode.value = 'repeat';
      }
      
      // 确保有默认值
      if (!tempData.date) {
        const today = new Date();
        tempData.date = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
      }
      if (!tempData.time) {
        tempData.time = '09:00';
      }
      
      show.value = true;
      emit('picker-show');
    };
    
    // 切换模式
    const switchMode = (mode) => {
      currentMode.value = mode;
      
      if (mode === 'simple') {
        tempData.type = TimeType.ONCE;
        // 清空重复相关数据
        tempData.weekdays = [];
        tempData.monthDays = [];
        tempData.months = [];
        tempData.cronExpression = '';
      } else {
        // 默认设置为每天
        if (tempData.type === TimeType.ONCE) {
          tempData.type = TimeType.DAILY;
        }
      }
      
      // 更新预览
      updateTempData();
    };
    
    // 格式化日期显示
    const formatDate = (date) => {
      if (!date) return '';
      const [year, month, day] = date.split('-');
      return `${year}年${parseInt(month)}月${parseInt(day)}日`;
    };
    
    // 格式化时间
    const formatTime = (hour, minute) => {
      return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
    };
    
    // 日期改变
    const onDateChange = (e) => {
      tempData.date = e.detail.value;
      updateTempData();
    };
    
    // 时间改变
    const onTimeChange = (e) => {
      tempData.time = e.detail.value;
      const [hour, minute] = e.detail.value.split(':');
      tempData.hour = parseInt(hour);
      tempData.minute = parseInt(minute);
      updateTempData();
    };
    
    // 重复模式时间改变
    const onRepeatTimeChange = (e) => {
      const [hour, minute] = e.detail.value.split(':');
      tempData.hour = parseInt(hour);
      tempData.minute = parseInt(minute);
      tempData.time = e.detail.value;
      updateTempData();
    };
    
    // 选择重复类型
    const selectRepeatType = (type) => {
      tempData.type = type;
      
      // 重置选择
      if (type === TimeType.DAILY) {
        tempData.weekdays = [];
        tempData.monthDays = [];
        tempData.months = [];
      } else if (type === TimeType.WEEKLY && tempData.weekdays.length === 0) {
        tempData.weekdays = [1]; // 默认周一
      } else if (type === TimeType.MONTHLY && tempData.monthDays.length === 0) {
        tempData.monthDays = [1]; // 默认1号
      } else if (type === TimeType.YEARLY) {
        if (tempData.months.length === 0) tempData.months = [1]; // 默认1月
        if (tempData.monthDays.length === 0) tempData.monthDays = [1]; // 默认1号
      }
      
      updateTempData();
    };
    
    // 切换星期选择
    const toggleWeekday = (weekday) => {
      const index = tempData.weekdays.indexOf(weekday);
      if (index > -1) {
        tempData.weekdays.splice(index, 1);
      } else {
        tempData.weekdays.push(weekday);
      }
      tempData.weekdays.sort((a, b) => a - b);
      updateTempData();
    };
    
    // 切换月份日期选择
    const toggleMonthDay = (day) => {
      const index = tempData.monthDays.indexOf(day);
      if (index > -1) {
        tempData.monthDays.splice(index, 1);
      } else {
        tempData.monthDays.push(day);
      }
      tempData.monthDays.sort((a, b) => a - b);
      updateTempData();
    };
    
    // 更新临时数据
    const updateTempData = () => {
      // 更新eventTime或cronExpression
      if (tempData.type === TimeType.ONCE) {
        tempData.eventTime = `${tempData.date} ${tempData.time}:00`;
        tempData.cronExpression = '';
      } else {
        tempData.eventTime = '';
        tempData.cronExpression = generateCronExpression(tempData);
      }
      
      // 更新描述
      tempData.description = generateDescription(tempData);
      
      // 触发change事件
      emit('change', tempData);
    };
    
    // 确认选择
    const handleConfirm = () => {
      // 更新数据
      const newData = JSON.parse(JSON.stringify(tempData));
      
      // 生成最终的预览时间
      newData.previewTimes = generatePreviewTimes(newData, 10);
      
      emit('update:modelValue', newData);
      emit('confirm', newData);
      show.value = false;
      emit('picker-hide');
    };
    
    // 取消选择
    const handleCancel = () => {
      // 恢复原始数据
      if (originalData.value) {
        Object.assign(tempData, originalData.value);
      }
      
      emit('cancel');
      show.value = false;
      emit('picker-hide');
    };
    
    // 监听modelValue变化
    watch(() => props.modelValue, (newValue) => {
      if (!show.value && newValue) {
        Object.assign(tempData, newValue);
      }
    }, { deep: true });
    
    return {
      show,
      currentMode,
      tempData,
      originalData,
      repeatTypes,
      weekDays,
      displayText,
      previewTimes,
      // 方法
      showPicker,
      switchMode,
      formatDate,
      formatTime,
      onDateChange,
      onTimeChange,
      onRepeatTimeChange,
      selectRepeatType,
      toggleWeekday,
      toggleMonthDay,
      handleConfirm,
      handleCancel
    };
  }
};
</script>

<style scoped>
.unified-time-picker {
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
  transition: background-color 0.2s ease;
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
}

.setting-value {
  font-size: 32rpx;
  font-weight: 400;
  color: #9d8148;
  line-height: 1.4;
  flex-shrink: 0;
}

/* 弹窗遮罩 */
.picker-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  z-index: 10000;
  touch-action: none;
}

/* 弹窗容器 */
.picker-container {
  width: 100%;
  max-height: 80vh;
  background-color: #fcfbf8;
  border-radius: 32rpx 32rpx 0 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 -8rpx 32rpx rgba(0, 0, 0, 0.15);
}

/* 顶部标题栏 */
.header-section {
  padding: 32rpx;
  border-bottom: 1rpx solid #e9e0ce;
  background-color: #fcfbf8;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cancel-btn, .confirm-btn {
  padding: 16rpx 24rpx;
  border-radius: 20rpx;
  transition: all 0.2s ease;
}

.cancel-btn:active, .confirm-btn:active {
  background-color: rgba(157, 129, 72, 0.1);
  transform: scale(0.95);
}

.btn-text {
  font-size: 32rpx;
  color: #9d8148;
}

.confirm-btn .btn-text {
  color: #f7bd4a;
  font-weight: 600;
}

.title {
  font-size: 36rpx;
  font-weight: 700;
  color: #1c170d;
}

/* 模式切换标签 */
.mode-tabs {
  display: flex;
  background-color: #f0ede4;
  margin: 0 32rpx;
  border-radius: 24rpx;
  padding: 8rpx;
  gap: 8rpx;
}

.mode-tab {
  flex: 1;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 18rpx;
  transition: all 0.3s ease;
  cursor: pointer;
}

.mode-tab.active {
  background-color: #fcfbf8;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.12);
  transform: translateY(-1rpx);
}

.mode-tab:active {
  transform: scale(0.96);
}

.tab-text {
  font-size: 30rpx;
  color: #9d8148;
  font-weight: 500;
}

.mode-tab.active .tab-text {
  color: #1c170d;
  font-weight: 600;
}

/* 内容区域 */
.content-section {
  flex: 1;
  padding: 32rpx;
  overflow-y: auto;
  background-color: #fcfbf8;
}

/* 输入组 */
.input-group {
  margin-bottom: 32rpx;
}

.input-label {
  margin-bottom: 16rpx;
}

.label-text {
  font-size: 28rpx;
  color: #1c170d;
  font-weight: 600;
}

/* 选择器显示 */
.picker-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx;
  background-color: #f4efe7;
  border-radius: 24rpx;
  border: 2rpx solid transparent;
  transition: all 0.2s ease;
}

.picker-display:active {
  background-color: #f0ede4;
  border-color: #f7bd4a;
}

.picker-text {
  font-size: 28rpx;
  color: #1c170d;
  font-weight: 500;
}

.picker-arrow {
  font-size: 24rpx;
  color: #9d8148;
  font-weight: 600;
}

/* 重复类型选择 */
.type-selection {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
  margin-bottom: 32rpx;
}

.type-btn {
  height: 80rpx;
  background-color: #f0ede4;
  border: 2rpx solid transparent;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  cursor: pointer;
}

.type-btn.active {
  background-color: #f7bd4a;
  border-color: #f7bd4a;
  box-shadow: 0 4rpx 16rpx rgba(247, 189, 74, 0.3);
  transform: translateY(-2rpx);
}

.type-btn:active {
  transform: scale(0.95);
}

.type-text {
  font-size: 28rpx;
  color: #1c170d;
  font-weight: 500;
}

.type-btn.active .type-text {
  font-weight: 600;
}

/* 星期选择 */
.weekday-selection {
  margin-bottom: 32rpx;
}

.weekday-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 12rpx;
}

.weekday-btn {
  height: 72rpx;
  background-color: #f0ede4;
  border: 2rpx solid transparent;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  cursor: pointer;
}

.weekday-btn.selected {
  background-color: #f7bd4a;
  border-color: #f7bd4a;
  box-shadow: 0 2rpx 8rpx rgba(247, 189, 74, 0.3);
}

.weekday-btn:active {
  transform: scale(0.95);
}

.weekday-text {
  font-size: 26rpx;
  color: #1c170d;
  font-weight: 500;
}

.weekday-btn.selected .weekday-text {
  font-weight: 600;
}

/* 日期选择 */
.day-selection {
  margin-bottom: 32rpx;
}

.day-grid-scroll {
  width: 100%;
  white-space: nowrap;
}

.day-grid {
  display: inline-flex;
  gap: 12rpx;
  padding: 8rpx 0;
}

.day-btn {
  width: 64rpx;
  height: 64rpx;
  background-color: #f0ede4;
  border: 2rpx solid transparent;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  cursor: pointer;
  flex-shrink: 0;
}

.day-btn.selected {
  background-color: #f7bd4a;
  border-color: #f7bd4a;
  box-shadow: 0 2rpx 8rpx rgba(247, 189, 74, 0.3);
}

.day-btn:active {
  transform: scale(0.95);
}

.day-text {
  font-size: 24rpx;
  color: #1c170d;
  font-weight: 500;
}

.day-btn.selected .day-text {
  font-weight: 600;
}

/* 预览区域 */
.preview-section {
  margin-top: 32rpx;
  padding: 24rpx;
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 24rpx;
  border: 1rpx solid #e9e0ce;
}

.preview-header {
  margin-bottom: 16rpx;
}

.preview-title {
  font-size: 28rpx;
  color: #1c170d;
  font-weight: 600;
}

.preview-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.preview-item {
  padding: 16rpx 20rpx;
  background-color: #f8f5ed;
  border-radius: 16rpx;
  border-left: 4rpx solid #f7bd4a;
}

.preview-text {
  font-size: 26rpx;
  color: #1c170d;
  font-weight: 500;
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
  
  .content-section {
    padding: 24rpx;
  }
  
  .type-selection {
    grid-template-columns: 1fr;
  }
  
  .weekday-grid {
    gap: 8rpx;
  }
  
  .weekday-btn {
    height: 64rpx;
  }
  
  .day-btn {
    width: 56rpx;
    height: 56rpx;
  }
}

/* 弹窗动画 */
.picker-overlay {
  animation: modalFadeIn 0.3s ease-out;
}

.picker-container {
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