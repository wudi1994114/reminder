<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="nav-header">
      <view class="nav-close" @click="cancel">
        <text class="close-icon">✕</text>
      </view>
      <text class="nav-title">{{ isEdit ? '编辑提醒' : '新建提醒' }}</text>
    </view>
    
    <!-- 主要内容区域 -->
    <scroll-view class="content-scroll" scroll-y>
      <!-- 标题输入 -->
      <view class="input-section">
        <input 
          class="title-input" 
          v-model="reminderForm.title" 
          placeholder="标题"
          placeholder-class="input-placeholder"
          maxlength="50"
        />
      </view>
      
      <!-- 内容输入 -->
      <view class="input-section">
        <textarea 
          class="content-textarea" 
          v-model="reminderForm.description" 
          placeholder="内容"
          placeholder-class="input-placeholder"
          maxlength="200"
          auto-height
        />
      </view>
      
      <!-- 提醒方式 -->
      <view class="setting-item" @click="showReminderTypeSelector">
        <text class="setting-label">提醒方式</text>
        <text class="setting-value">{{ getReminderTypeText(reminderForm.reminderType) }}</text>
      </view>
      
      <!-- 时间设置 -->
      <view class="setting-item" @click="showTimeSelector">
        <text class="setting-label">时间设置</text>
        <text class="setting-value">{{ getFormattedDateTime() }}</text>
      </view>
    </scroll-view>
    
    <!-- 底部保存按钮 -->
    <view class="bottom-container">
      <button 
        class="save-button" 
        @click="saveReminder" 
        :disabled="isSubmitting"
        :class="{ 'button-loading': isSubmitting }"
      >
        <text class="button-text" v-if="!isSubmitting">{{ isEdit ? '更新提醒' : '保存提醒' }}</text>
        <text class="button-text" v-else>保存中...</text>
      </button>
      <view class="bottom-spacer"></view>
    </view>
    
    <!-- 自定义日期时间选择（当用户选择自定义时显示） -->
    <view class="custom-datetime" v-if="showCustomPickers">
      <view class="custom-modal">
        <view class="custom-header">
          <text class="custom-title">选择自定义日期和时间</text>
          <view class="custom-close" @click="hideCustomPickers">
            <text class="close-icon">✕</text>
          </view>
        </view>
        
        <view class="picker-container">
          <view class="picker-item">
            <text class="picker-label">日期</text>
            <picker mode="date" :value="reminderDate" @change="onDateChange">
              <view class="picker-display">
                <text class="picker-text">{{ reminderDate || '选择日期' }}</text>
              </view>
            </picker>
          </view>
          
          <view class="picker-item">
            <text class="picker-label">时间</text>
            <picker mode="time" :value="reminderTime" @change="onTimeChange">
              <view class="picker-display">
                <text class="picker-text">{{ reminderTime || '选择时间' }}</text>
              </view>
            </picker>
          </view>
        </view>
        
        <view class="custom-actions">
          <button class="custom-btn confirm-btn" @click="confirmCustomDateTime">
            <text class="btn-text">确认</text>
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { ref, computed, reactive, onMounted, getCurrentInstance } from 'vue';
import { createEvent, updateEvent, getSimpleReminderById } from '../../services/api';

export default {
  onLoad(options) {
    console.log('onLoad 接收到的参数:', options);
    this.pageOptions = options || {};
  },
  
  setup() {
    const isEdit = ref(false);
    const pageOptions = ref({}); // 用于存储页面参数
    const reminderForm = reactive({
      id: null,
      title: '',
      description: '',
      eventTime: '',
      reminderType: 'EMAIL', // 添加默认提醒方式
      status: 'PENDING' // 默认为PENDING
    });
    
    const reminderDate = ref('');
    const reminderTime = ref('');
    const isSubmitting = ref(false);
    const showCustomPickers = ref(false);
    
    // 提醒方式相关
    const reminderTypeOptions = ['邮件', '短信', '微信'];
    const reminderTypeValues = ['EMAIL', 'SMS', 'WECHAT_MINI'];
    const reminderTypeIndex = ref(0); // 默认选择邮件提醒
    
    onMounted(async () => {
      // 获取页面参数 - 使用getCurrentPages方式
      const pages = getCurrentPages();
      const currentPage = pages[pages.length - 1];
      const options = currentPage.options || {};
      
      const id = options.id || null;
      const mode = options.mode || '';
      const initialDate = options.date || ''; // 从日历页传来的日期
      
      console.log('页面参数:', { id, mode, initialDate }); // 添加调试日志
      
      isEdit.value = mode === 'edit' && id;
      
      if (isEdit.value) {
        try {
          isSubmitting.value = true; // 开始加载时也设为true，防止重复点击
          const result = await getSimpleReminderById(id);
          if (result) {
            reminderForm.id = result.id;
            reminderForm.title = result.title;
            reminderForm.description = result.description;
            reminderForm.eventTime = result.eventTime;
            reminderForm.status = result.status;
            reminderForm.reminderType = result.reminderType || 'EMAIL'; // 设置提醒方式
            
            if (result.eventTime) {
              // 处理不同格式的日期时间字符串
              let eventTimeStr = result.eventTime;
              
              // 如果是ISO格式，先转换为本地时间字符串
              if (eventTimeStr.includes('T')) {
                // 确保使用iOS兼容的日期格式
                const eventDate = new Date(eventTimeStr);
                
                // 检查日期是否有效
                if (isNaN(eventDate.getTime())) {
                  console.error('无效的日期格式:', eventTimeStr);
                  // 使用当前时间作为默认值
                  const now = new Date();
                  reminderDate.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
                  reminderTime.value = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
                } else {
                  const year = eventDate.getFullYear();
                  const month = String(eventDate.getMonth() + 1).padStart(2, '0');
                  const day = String(eventDate.getDate()).padStart(2, '0');
                  const hours = String(eventDate.getHours()).padStart(2, '0');
                  const minutes = String(eventDate.getMinutes()).padStart(2, '0');
                  const seconds = String(eventDate.getSeconds()).padStart(2, '0');
                  eventTimeStr = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
                  
                  const [date, time] = eventTimeStr.split(' ');
                  reminderDate.value = date;
                  reminderTime.value = time.substring(0, 5); // HH:mm
                }
              } else {
                // 非ISO格式，直接分割
                const [date, time] = eventTimeStr.split(' ');
                reminderDate.value = date;
                reminderTime.value = time ? time.substring(0, 5) : '09:00'; // HH:mm，如果没有时间则默认09:00
              }
            }
            
            // 设置提醒方式索引
            const typeIndex = reminderTypeValues.indexOf(reminderForm.reminderType);
            reminderTypeIndex.value = typeIndex >= 0 ? typeIndex : 0;
          }
        } catch (error) {
          console.error('获取提醒详情失败:', error);
          uni.showToast({title: '加载提醒数据失败', icon: 'none'});
        } finally {
          isSubmitting.value = false;
        }
      } else {
        // 创建模式，设置默认值
        // 1. 设置日期：优先使用传入的日期，否则使用今天
        if (initialDate) {
          reminderDate.value = initialDate;
        } else {
          const today = new Date();
          reminderDate.value = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
        }
        
        // 2. 设置默认时间为当前时间的后一小时整点
        const now = new Date();
        now.setHours(now.getHours() + 1);
        now.setMinutes(0);
        now.setSeconds(0);
        reminderTime.value = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
        
        // 3. 更新eventTime
        updateEventTime();
      }
    });
    
    const onDateChange = (e) => {
      reminderDate.value = e.detail.value;
      updateEventTime();
    };
    
    const onTimeChange = (e) => {
      reminderTime.value = e.detail.value;
      updateEventTime();
    };
    
    // 提醒方式相关方法
    const onReminderTypeChange = (e) => {
      reminderTypeIndex.value = e.detail.value;
      reminderForm.reminderType = reminderTypeValues[e.detail.value];
    };
    
    const getReminderTypeIcon = (type) => {
      switch (type) {
        case 'EMAIL': return '📧';
        case 'SMS': return '📱';
        case 'WECHAT_MINI': return '💬';
        default: return '📧';
      }
    };
    
    const getReminderTypeText = (type) => {
      switch (type) {
        case 'EMAIL': return '邮件提醒';
        case 'SMS': return '短信提醒';
        case 'WECHAT_MINI': return '微信小程序提醒';
        default: return '邮件提醒';
      }
    };
    
    const updateEventTime = () => {
      if (reminderDate.value && reminderTime.value) {
        reminderForm.eventTime = `${reminderDate.value} ${reminderTime.value}:00`; // 补全秒
      } else {
        reminderForm.eventTime = '';
      }
    };
    
    const saveReminder = async () => {
      if (!reminderForm.title) {
        uni.showToast({ title: '请输入提醒标题', icon: 'none' });
        return;
      }
      if (!reminderForm.eventTime) {
        uni.showToast({ title: '请选择提醒时间', icon: 'none' });
        return;
      }
      
      isSubmitting.value = true;
      try {
        let result;
        const dataToSave = { 
          ...reminderForm
        };
        
        // 将eventTime转换为ISO 8601格式
        if (dataToSave.eventTime) {
          // 将 "YYYY-MM-DD HH:mm:ss" 格式转换为 iOS 兼容的格式，然后转为 ISO 8601
          const eventTimeStr = dataToSave.eventTime.replace(' ', 'T'); // 转换为 "YYYY-MM-DDTHH:mm:ss" 格式
          const eventDate = new Date(eventTimeStr);
          dataToSave.eventTime = eventDate.toISOString();
        }
        
        // 移除不需要的字段
        delete dataToSave.toUserId; // 让后端自动设置
        delete dataToSave.status; // 后端会设置默认状态

        if (isEdit.value) {
          result = await updateEvent(reminderForm.id, dataToSave);
        } else {
          result = await createEvent(dataToSave);
        }
        
        if (result) {
          uni.showToast({
            title: isEdit.value ? '修改成功' : '创建成功',
            icon: 'success',
            duration: 1500
          });
          setTimeout(() => {
            uni.navigateBack();
          }, 1500);
        } else {
           // API已在内部处理错误提示，这里可以不重复提示
        }
      } catch (error) {
        console.error('保存失败:', error);
        // API已在内部处理错误提示
      } finally {
        isSubmitting.value = false;
      }
    };
    
    const cancel = () => {
      uni.navigateBack();
    };
    
    // 新增方法：显示提醒方式选择器
    const showReminderTypeSelector = () => {
      uni.showActionSheet({
        itemList: reminderTypeOptions,
        success: (res) => {
          reminderTypeIndex.value = res.tapIndex;
          reminderForm.reminderType = reminderTypeValues[res.tapIndex];
        }
      });
    };
    
    // 新增方法：显示时间选择器
    const showTimeSelector = () => {
      // 直接显示自定义日期时间选择器
      showCustomDateTime();
    };
    

    
    // 显示自定义日期时间选择
    const showCustomDateTime = () => {
      showCustomPickers.value = true;
    };
    
    // 隐藏自定义选择器
    const hideCustomPickers = () => {
      showCustomPickers.value = false;
    };
    
    // 确认自定义日期时间
    const confirmCustomDateTime = () => {
      if (reminderDate.value && reminderTime.value) {
        updateEventTime();
        showCustomPickers.value = false;
        uni.showToast({
          title: '自定义时间设置成功',
          icon: 'success'
        });
      } else {
        uni.showToast({
          title: '请选择日期和时间',
          icon: 'none'
        });
      }
    };
    
    // 新增方法：格式化显示日期时间
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
        timeStr = `上午${displayHour}:${String(minutes).padStart(2, '0')}`;
      } else {
        const displayHour = hours === 12 ? 12 : hours - 12;
        timeStr = `下午${displayHour}:${String(minutes).padStart(2, '0')}`;
      }
      
      return `${dateStr} ${timeStr}`;
    };
    
    // 更新getReminderTypeText方法以支持中文
    const getReminderTypeTextUpdated = (type) => {
      switch (type) {
        case 'EMAIL': return '邮件';
        case 'SMS': return '短信';
        case 'WECHAT_MINI': return '微信';
        default: return '邮件';
      }
    };

    return {
      isEdit,
      reminderForm,
      reminderDate,
      reminderTime,
      isSubmitting,
      reminderTypeOptions,
      reminderTypeIndex,
      onDateChange,
      onTimeChange,
      onReminderTypeChange,
      getReminderTypeIcon,
      getReminderTypeText: getReminderTypeTextUpdated,
      saveReminder,
      cancel,
      showReminderTypeSelector,
      showTimeSelector,
      getFormattedDateTime,
      showCustomPickers,
      showCustomDateTime,
      hideCustomPickers,
      confirmCustomDateTime
    };
  }
};
</script>

<style scoped>
/* 页面容器 */
.page-container {
  height: 100vh;
  background-color: #fcfbf8;
  display: flex;
  flex-direction: column;
  font-family: 'Manrope', 'Noto Sans', sans-serif;
}

/* 导航栏样式 */
.nav-header {
  display: flex;
  align-items: center;
  background-color: #fcfbf8;
  padding: 32rpx 32rpx 16rpx;
  justify-content: space-between;
}

.nav-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 96rpx;
  height: 96rpx;
  color: #1c170d;
  cursor: pointer;
}

.close-icon {
  font-size: 48rpx;
  color: #1c170d;
}

.nav-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #1c170d;
  text-align: center;
  flex: 1;
  padding-right: 96rpx;
  line-height: 1.2;
  letter-spacing: -0.015em;
}

/* 内容滚动区域 */
.content-scroll {
  flex: 1;
  padding: 0;
}

/* 输入区域样式 */
.input-section {
  padding: 24rpx 32rpx;
}

.title-input {
  width: 100%;
  min-height: 112rpx;
  padding: 32rpx;
  background-color: #f4efe7;
  border-radius: 24rpx;
  border: none;
  font-size: 32rpx;
  color: #1c170d;
  line-height: 1.4;
}

.content-textarea {
  width: 100%;
  min-height: 288rpx;
  padding: 32rpx;
  background-color: #f4efe7;
  border-radius: 24rpx;
  border: none;
  font-size: 32rpx;
  color: #1c170d;
  line-height: 1.4;
  resize: none;
}

.input-placeholder {
  color: #9d8148;
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
  font-weight: 400;
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

/* 底部容器 */
.bottom-container {
  background-color: #fcfbf8;
}

.save-button {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 168rpx;
  max-width: 960rpx;
  height: 96rpx;
  margin: 0 32rpx;
  padding: 0 40rpx;
  background-color: #f7bd4a;
  border-radius: 48rpx;
  border: none;
  cursor: pointer;
  overflow: hidden;
}

.save-button:active {
  background-color: #e6a73d;
}

.save-button:disabled,
.button-loading {
  background-color: #d3d4d6;
  cursor: not-allowed;
}

.button-text {
  font-size: 32rpx;
  font-weight: 700;
  color: #1c170d;
  line-height: 1.4;
  letter-spacing: 0.015em;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}

.bottom-spacer {
  height: 40rpx;
  background-color: #fcfbf8;
}

/* 加载状态动画 */
.button-loading::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  animation: loading 1.5s infinite;
}

@keyframes loading {
  0% {
    left: -100%;
  }
  100% {
    left: 100%;
  }
}

/* 响应式适配 */
@media (max-width: 750rpx) {
  .nav-header {
    padding: 24rpx 24rpx 12rpx;
  }
  
  .input-section {
    padding: 20rpx 24rpx;
  }
  
  .setting-item {
    padding: 24rpx;
    min-height: 96rpx;
  }
  
  .save-button {
    margin: 0 24rpx;
    height: 88rpx;
  }
  
  .nav-title {
    font-size: 32rpx;
  }
  
  .title-input,
  .content-textarea {
    font-size: 28rpx;
    padding: 24rpx;
  }
  
  .setting-label,
  .setting-value {
    font-size: 28rpx;
  }
  
  .button-text {
    font-size: 28rpx;
  }
}

/* 自定义日期时间选择器 */
.custom-datetime {
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

.custom-modal {
  background-color: #fcfbf8;
  border-radius: 24rpx;
  margin: 32rpx;
  max-width: 600rpx;
  width: 100%;
  overflow: hidden;
}

.custom-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
  border-bottom: 1rpx solid #f4efe7;
}

.custom-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1c170d;
}

.custom-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64rpx;
  height: 64rpx;
  cursor: pointer;
}

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

.custom-actions {
  padding: 32rpx;
  border-top: 1rpx solid #f4efe7;
}

.custom-btn {
  width: 100%;
  height: 88rpx;
  background-color: #f7bd4a;
  border-radius: 44rpx;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.custom-btn:active {
  background-color: #e6a73d;
}

.btn-text {
  font-size: 28rpx;
  font-weight: 700;
  color: #1c170d;
}
</style> 