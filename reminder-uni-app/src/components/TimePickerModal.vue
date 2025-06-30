<template>
  <view v-if="show" class="time-picker-overlay" @click="cancel">
    <view class="time-picker-modal" @click.stop>
      <view class="time-picker-header">
        <text class="picker-cancel" @click="cancel">取消</text>
        <text class="picker-title">选择时间</text>
        <text class="picker-confirm" @click="confirm">确定</text>
      </view>
      
      <picker-view 
        class="picker-view" 
        :value="pickerValue" 
        @change="onPickerChange"
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
</template>

<script>
export default {
  name: 'TimePickerModal',
  props: {
    show: {
      type: Boolean,
      default: false,
    },
    initialTime: {
      type: String,
      default: '09:00',
    },
  },
  data() {
    return {
      hours: Array.from({ length: 24 }, (_, i) => i),
      minutes: Array.from({ length: 60 }, (_, i) => i),
      pickerValue: [9, 0], // [小时索引, 分钟索引]
      selectedHour: 9,
      selectedMinute: 0,
    };
  },
  watch: {
    show(newVal) {
      console.log('TimePickerModal show 属性变化:', newVal);
      if (newVal) {
        this.initializeTime();
      }
    },
    initialTime(newVal) {
      console.log('TimePickerModal initialTime 属性变化:', newVal);
      if (this.show) {
        this.initializeTime();
      }
    }
  },
  mounted() {
    console.log('TimePickerModal 组件已挂载');
    this.initializeTime();
  },
  methods: {
    initializeTime() {
      console.log('TimePickerModal 初始化时间:', this.initialTime);
      if (this.initialTime) {
        const [hour, minute] = this.initialTime.split(':').map(Number);
        this.selectedHour = hour;
        this.selectedMinute = minute;
        this.pickerValue = [hour, minute];
        console.log('TimePickerModal 设置初始值:', { hour, minute, pickerValue: this.pickerValue });
      }
    },
    onPickerChange(e) {
      console.log('TimePickerModal picker 变化:', e.detail.value);
      const [hourIndex, minuteIndex] = e.detail.value;
      this.selectedHour = this.hours[hourIndex];
      this.selectedMinute = this.minutes[minuteIndex];
      this.pickerValue = e.detail.value;
      console.log('TimePickerModal 更新选中时间:', { hour: this.selectedHour, minute: this.selectedMinute });
    },
    cancel() {
      console.log('TimePickerModal cancel 被调用');
      this.$emit('update:show', false);
      this.$emit('cancel');
    },
    confirm() {
      console.log('TimePickerModal confirm 被调用');
      const timeString = `${String(this.selectedHour).padStart(2, '0')}:${String(this.selectedMinute).padStart(2, '0')}`;
      console.log('TimePickerModal 确认选择的时间:', timeString);
      this.$emit('confirm', timeString);
      this.$emit('update:show', false);
    },
  },
};
</script>

<style scoped>
.time-picker-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 9999;
}

.time-picker-modal {
  background-color: #fcfbf8;
  border-radius: 24rpx 24rpx 0 0;
  width: 100%;
  max-height: 60vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.time-picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
  border-bottom: 1rpx solid #f4efe7;
}

.picker-cancel,
.picker-confirm {
  background: none;
  border: none;
  padding: 16rpx 24rpx;
  font-size: 32rpx;
  color: #9d8148;
  cursor: pointer;
  border-radius: 20rpx;
  transition: all 0.2s ease;
}

.picker-cancel:active,
.picker-confirm:active {
  background-color: rgba(157, 129, 72, 0.1);
  transform: scale(0.95);
}

.picker-confirm {
  color: #f7bd4a;
  font-weight: 600;
}

.picker-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1c170d;
}

.picker-view {
  height: 400rpx;
  width: 100%;
}

.picker-item {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 80rpx;
  font-size: 36rpx;
  color: #1c170d;
}
</style> 