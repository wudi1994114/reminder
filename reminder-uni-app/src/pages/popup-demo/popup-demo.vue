<template>
  <view class="demo-container">
    <view class="header">
      <text class="title">iOS日期兼容性测试</text>
    </view>
    
    <scroll-view class="content" scroll-y>
      <!-- 日期格式测试 -->
      <view class="section">
        <text class="section-title">日期格式测试</text>
        
        <view class="test-item">
          <text class="test-label">测试日期: 2025-05-26 19:00</text>
          <button class="test-btn" @click="testDateFormat1">测试旧格式 (可能失败)</button>
          <text class="test-result">{{ result1 }}</text>
        </view>
        
        <view class="test-item">
          <text class="test-label">测试日期: 2025-05-26T19:00:00</text>
          <button class="test-btn" @click="testDateFormat2">测试ISO格式 (应该成功)</button>
          <text class="test-result">{{ result2 }}</text>
        </view>
        
        <view class="test-item">
          <text class="test-label">当前时间格式化测试</text>
          <button class="test-btn" @click="testCurrentTime">测试当前时间</button>
          <text class="test-result">{{ result3 }}</text>
        </view>
      </view>
      
      <!-- 提醒时间显示测试 -->
      <view class="section">
        <text class="section-title">提醒时间显示测试</text>
        
        <view class="test-item">
          <text class="test-label">模拟提醒时间显示</text>
          <button class="test-btn" @click="testReminderDisplay">测试提醒显示</button>
          <text class="test-result">{{ reminderDisplayResult }}</text>
        </view>
      </view>
      
      <!-- 时间选择功能测试 -->
      <view class="section">
        <text class="section-title">时间选择功能测试</text>
        
        <view class="test-item">
          <text class="test-label">测试直接打开自定义时间选择器</text>
          <button class="test-btn" @click="testTimeSelector">测试时间选择</button>
          <text class="test-result">{{ timeSelectResult }}</text>
        </view>
      </view>
      
      <!-- 页面导航测试 -->
      <view class="section">
        <text class="section-title">页面导航测试</text>
        
        <view class="test-item">
          <text class="test-label">测试新设计的日历页面</text>
          <button class="test-btn" @click="goToCalendar">打开日历页面</button>
          <text class="test-result">查看现代化设计风格的日历界面</text>
        </view>
        
        <view class="test-item">
          <text class="test-label">测试新设计的创建提醒页面</text>
          <button class="test-btn" @click="goToCreate">打开创建页面</button>
          <text class="test-result">查看简化的时间选择流程</text>
        </view>
      </view>
      
      <!-- 弹窗功能测试 -->
      <view class="section">
        <text class="section-title">弹窗功能测试</text>
        
        <view class="button-grid">
          <button class="demo-btn" @click="showSuccessToast">成功提示</button>
          <button class="demo-btn" @click="showErrorToast">错误提示</button>
          <button class="demo-btn" @click="showConfirmModal">确认对话框</button>
          <button class="demo-btn" @click="showActionSheet">操作菜单</button>
          <button class="demo-btn" @click="showLoading">加载提示</button>
          <button class="demo-btn" @click="showCustomModal">自定义弹窗</button>
        </view>
      </view>
    </scroll-view>
    
    <!-- 自定义弹窗 -->
    <view class="custom-modal" v-if="showCustom">
      <view class="modal-content">
        <view class="modal-header">
          <text class="modal-title">自定义弹窗</text>
          <view class="close-btn" @click="hideCustomModal">
            <text class="close-icon">✕</text>
          </view>
        </view>
        <view class="modal-body">
          <text class="modal-text">这是一个自定义弹窗示例，展示了现代化的设计风格。</text>
          <text class="modal-text">所有日期处理都已经过iOS兼容性优化。</text>
        </view>
        <view class="modal-footer">
          <button class="modal-btn confirm" @click="hideCustomModal">确定</button>
        </view>
      </view>
    </view>
    
    <!-- 测试用自定义时间选择器 -->
    <view class="custom-modal" v-if="showTimeCustom">
      <view class="modal-content">
        <view class="modal-header">
          <text class="modal-title">选择日期和时间</text>
          <view class="close-btn" @click="hideTimeCustom">
            <text class="close-icon">✕</text>
          </view>
        </view>
        
        <view class="picker-container">
          <view class="picker-item">
            <text class="picker-label">日期</text>
            <picker mode="date" :value="testDate" @change="onTestDateChange">
              <view class="picker-display">
                <text class="picker-text">{{ testDate || '选择日期' }}</text>
              </view>
            </picker>
          </view>
          
          <view class="picker-item">
            <text class="picker-label">时间</text>
            <picker mode="time" :value="testTime" @change="onTestTimeChange">
              <view class="picker-display">
                <text class="picker-text">{{ testTime || '选择时间' }}</text>
              </view>
            </picker>
          </view>
        </view>
        
        <view class="modal-footer">
          <button class="modal-btn confirm" @click="confirmTimeSelection">确认</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { ref } from 'vue';

export default {
  setup() {
    const result1 = ref('');
    const result2 = ref('');
    const result3 = ref('');
    const reminderDisplayResult = ref('');
    const timeSelectResult = ref('');
    const showCustom = ref(false);
    const showTimeCustom = ref(false);
    const testDate = ref('');
    const testTime = ref('');
    
    // 测试旧的日期格式（可能在iOS上失败）
    const testDateFormat1 = () => {
      try {
        const date = new Date("2025-05-26 19:00");
        if (isNaN(date.getTime())) {
          result1.value = '❌ 失败: 无效日期格式';
        } else {
          result1.value = `✅ 成功: ${date.toLocaleString()}`;
        }
      } catch (error) {
        result1.value = `❌ 错误: ${error.message}`;
      }
    };
    
    // 测试ISO日期格式（应该在iOS上成功）
    const testDateFormat2 = () => {
      try {
        const date = new Date("2025-05-26T19:00:00");
        if (isNaN(date.getTime())) {
          result2.value = '❌ 失败: 无效日期格式';
        } else {
          result2.value = `✅ 成功: ${date.toLocaleString()}`;
        }
      } catch (error) {
        result2.value = `❌ 错误: ${error.message}`;
      }
    };
    
    // 测试当前时间格式化
    const testCurrentTime = () => {
      try {
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        const hours = String(now.getHours()).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        
        const isoFormat = `${year}-${month}-${day}T${hours}:${minutes}:00`;
        const testDate = new Date(isoFormat);
        
        if (isNaN(testDate.getTime())) {
          result3.value = '❌ 失败: 格式化后的日期无效';
        } else {
          result3.value = `✅ 成功: ${testDate.toLocaleString()}`;
        }
      } catch (error) {
        result3.value = `❌ 错误: ${error.message}`;
      }
    };
    
    // 测试提醒时间显示（模拟getFormattedDateTime函数）
    const testReminderDisplay = () => {
      try {
        const reminderDate = '2025-05-26';
        const reminderTime = '19:00';
        
        // 使用iOS兼容的日期格式
        const dateTimeStr = `${reminderDate}T${reminderTime}:00`;
        const date = new Date(dateTimeStr);
        
        if (isNaN(date.getTime())) {
          reminderDisplayResult.value = '❌ 失败: 无效的日期格式';
          return;
        }
        
        const now = new Date();
        const tomorrow = new Date(now);
        tomorrow.setDate(tomorrow.getDate() + 1);
        
        const isToday = date.toDateString() === now.toDateString();
        const isTomorrow = date.toDateString() === tomorrow.toDateString();
        
        let dateStr = '';
        if (isToday) {
          dateStr = '今天';
        } else if (isTomorrow) {
          dateStr = '明天';
        } else {
          const months = ['1月', '2月', '3月', '4月', '5月', '6月', 
                         '7月', '8月', '9月', '10月', '11月', '12月'];
          dateStr = `${months[date.getMonth()]}${date.getDate()}日`;
        }
        
        let hours = date.getHours();
        const minutes = date.getMinutes();
        let timeStr = '';
        
        if (hours < 12) {
          const displayHour = hours === 0 ? 12 : hours;
          timeStr = `上午${displayHour}:${String(minutes).padStart(2, '0')}`;
        } else {
          const displayHour = hours === 12 ? 12 : hours - 12;
          timeStr = `下午${displayHour}:${String(minutes).padStart(2, '0')}`;
        }
        
        reminderDisplayResult.value = `✅ 成功: ${dateStr} ${timeStr}`;
      } catch (error) {
        reminderDisplayResult.value = `❌ 错误: ${error.message}`;
      }
    };
    
    // 弹窗功能测试
    const showSuccessToast = () => {
      uni.showToast({
        title: '操作成功',
        icon: 'success',
        duration: 2000
      });
    };
    
    const showErrorToast = () => {
      uni.showToast({
        title: '操作失败，请重试',
        icon: 'error',
        duration: 2000
      });
    };
    
    const showConfirmModal = () => {
      uni.showModal({
        title: '确认操作',
        content: '您确定要执行此操作吗？',
        confirmText: '确定',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            uni.showToast({
              title: '已确认',
              icon: 'success'
            });
          } else {
            uni.showToast({
              title: '已取消',
              icon: 'none'
            });
          }
        }
      });
    };
    
    const showActionSheet = () => {
      uni.showActionSheet({
        itemList: ['选项一', '选项二', '选项三'],
        success: (res) => {
          uni.showToast({
            title: `选择了选项${res.tapIndex + 1}`,
            icon: 'none'
          });
        }
      });
    };
    
    const showLoading = () => {
      uni.showLoading({
        title: '加载中...'
      });
      
      setTimeout(() => {
        uni.hideLoading();
        uni.showToast({
          title: '加载完成',
          icon: 'success'
        });
      }, 2000);
    };
    
    const showCustomModal = () => {
      showCustom.value = true;
    };
    
    const hideCustomModal = () => {
      showCustom.value = false;
    };
    
    // 页面导航函数
    const goToCalendar = () => {
      uni.navigateTo({
        url: '/pages/calendar/calendar'
      });
    };
    
    const goToCreate = () => {
      uni.navigateTo({
        url: '/pages/create/create'
      });
    };
    
    // 测试时间选择器
    const testTimeSelector = () => {
      // 设置默认值为当前时间
      const now = new Date();
      testDate.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
      testTime.value = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
      
      showTimeCustom.value = true;
      timeSelectResult.value = '已打开自定义时间选择器';
    };
    
    const hideTimeCustom = () => {
      showTimeCustom.value = false;
    };
    
    const confirmTimeSelection = () => {
      if (testDate.value && testTime.value) {
        timeSelectResult.value = `✅ 选择成功: ${testDate.value} ${testTime.value}`;
        showTimeCustom.value = false;
      } else {
        timeSelectResult.value = '❌ 请选择日期和时间';
      }
    };
    
    const onTestDateChange = (e) => {
      testDate.value = e.detail.value;
    };
    
    const onTestTimeChange = (e) => {
      testTime.value = e.detail.value;
    };
    
    return {
      result1,
      result2,
      result3,
      reminderDisplayResult,
      timeSelectResult,
      showCustom,
      showTimeCustom,
      testDate,
      testTime,
      testDateFormat1,
      testDateFormat2,
      testCurrentTime,
      testReminderDisplay,
      testTimeSelector,
      hideTimeCustom,
      confirmTimeSelection,
      onTestDateChange,
      onTestTimeChange,
      showSuccessToast,
      showErrorToast,
      showConfirmModal,
      showActionSheet,
      showLoading,
      showCustomModal,
      hideCustomModal,
      goToCalendar,
      goToCreate
    };
  }
};
</script>

<style scoped>
.demo-container {
  min-height: 100vh;
  background-color: #fcfbf8;
  font-family: 'Manrope', 'Noto Sans', sans-serif;
}

.header {
  padding: 32rpx;
  background-color: #fcfbf8;
  border-bottom: 1rpx solid #f4efe7;
}

.title {
  font-size: 40rpx;
  font-weight: 700;
  color: #1c170d;
  text-align: center;
}

.content {
  flex: 1;
  padding: 32rpx;
}

.section {
  margin-bottom: 48rpx;
  background-color: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
  margin-bottom: 24rpx;
  display: block;
}

.test-item {
  margin-bottom: 32rpx;
  padding: 24rpx;
  background-color: #f4efe7;
  border-radius: 16rpx;
}

.test-item:last-child {
  margin-bottom: 0;
}

.test-label {
  display: block;
  font-size: 28rpx;
  color: #1c170d;
  margin-bottom: 16rpx;
}

.test-btn {
  width: 100%;
  height: 80rpx;
  background-color: #f7bd4a;
  border-radius: 40rpx;
  border: none;
  font-size: 28rpx;
  font-weight: 600;
  color: #1c170d;
  margin-bottom: 16rpx;
}

.test-btn:active {
  background-color: #e6a73d;
}

.test-result {
  display: block;
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
  word-break: break-all;
}

.button-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24rpx;
}

.demo-btn {
  height: 88rpx;
  background-color: #f7bd4a;
  border-radius: 44rpx;
  border: none;
  font-size: 28rpx;
  font-weight: 600;
  color: #1c170d;
}

.demo-btn:active {
  background-color: #e6a73d;
}

/* 自定义弹窗样式 */
.custom-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background-color: #fcfbf8;
  border-radius: 24rpx;
  margin: 32rpx;
  max-width: 600rpx;
  width: 100%;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
  border-bottom: 1rpx solid #f4efe7;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1c170d;
}

.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64rpx;
  height: 64rpx;
  cursor: pointer;
}

.close-icon {
  font-size: 32rpx;
  color: #1c170d;
}

.modal-body {
  padding: 32rpx;
}

.modal-text {
  display: block;
  font-size: 28rpx;
  color: #1c170d;
  line-height: 1.6;
  margin-bottom: 16rpx;
}

.modal-text:last-child {
  margin-bottom: 0;
}

.modal-footer {
  padding: 32rpx;
  border-top: 1rpx solid #f4efe7;
}

.modal-btn {
  width: 100%;
  height: 88rpx;
  border-radius: 44rpx;
  border: none;
  font-size: 28rpx;
  font-weight: 700;
  cursor: pointer;
}

.modal-btn.confirm {
  background-color: #f7bd4a;
  color: #1c170d;
}

.modal-btn.confirm:active {
  background-color: #e6a73d;
}

/* Picker样式 */
.picker-container {
  padding: 32rpx;
}

.picker-item {
  margin-bottom: 32rpx;
}

.picker-item:last-child {
  margin-bottom: 0;
}

.picker-label {
  display: block;
  font-size: 28rpx;
  font-weight: 500;
  color: #1c170d;
  margin-bottom: 16rpx;
}

.picker-display {
  padding: 24rpx 32rpx;
  background-color: #f4efe7;
  border-radius: 16rpx;
  border: none;
}

.picker-text {
  font-size: 28rpx;
  color: #1c170d;
}
</style>