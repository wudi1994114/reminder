<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="nav-header">
      <view class="nav-close" @click="cancel">
        <text class="close-icon">✕</text>
      </view>
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
      <datetime-picker 
        v-if="isDataReady"
        ref="dateTimePickerRef"
        label="时间设置"
        :initial-date="reminderDate"
        :initial-time="reminderTime"
        :auto-set-default="!isEdit"
        @change="onDateTimeChange"
      />
      <view v-else class="loading-placeholder">
        <text>加载中...</text>
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
    

  </view>
</template>

<script>
import { ref, computed, reactive, onMounted, getCurrentInstance, nextTick } from 'vue';
import { createEvent, updateEvent, getSimpleReminderById } from '../../services/api';

export default {
  onLoad(options) {
    console.log('创建页面: onLoad 接收到参数:', options);
    // 将参数存储到全局变量中供setup使用
    if (typeof window !== 'undefined') {
      window._createPageOptions = { ...options };
      console.log('创建页面: onLoad 存储到window._createPageOptions:', window._createPageOptions);
    }
  },
  setup() {
    console.log('创建页面: setup函数开始执行');
    
    // 1. 定义响应式数据
    const isEdit = ref(false);
    const isDataReady = ref(false); // 用于控制组件渲染时机
    const reminderForm = reactive({
      id: null,
      title: '',
      description: '',
      eventTime: '',
      reminderType: 'EMAIL',
      status: 'PENDING'
    });
    
    // 2. 初始化默认值（今天的日期和时间）
    const today = new Date();
    const defaultDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    const now = new Date();
    now.setHours(now.getHours() + 1);
    now.setMinutes(0);
    now.setSeconds(0);
    const defaultTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
    
    const reminderDate = ref(defaultDate);
    const reminderTime = ref(defaultTime);
    const isSubmitting = ref(false);
    const dateTimePickerRef = ref(null);
    
    // 3. 提醒方式相关
    const reminderTypeOptions = ['邮件', '短信', '微信'];
    const reminderTypeValues = ['EMAIL', 'SMS', 'WECHAT_MINI'];
    const reminderTypeIndex = ref(0);
    
    // 4. 统一的参数处理函数
    const processPageOptions = () => {
      let options = {};
      
      // 尝试获取页面参数
      if (typeof window !== 'undefined' && window._createPageOptions) {
        options = window._createPageOptions;
        console.log('创建页面: 获取到页面参数:', options);
      } else {
        try {
          const pages = getCurrentPages();
          const currentPage = pages[pages.length - 1];
          options = currentPage.options || {};
          console.log('创建页面: 从getCurrentPages获取到参数:', options);
        } catch (error) {
          console.log('创建页面: 获取页面参数失败:', error);
        }
      }
      
      const id = options.id || null;
      const mode = options.mode || '';
      const initialDate = options.date || '';
      
      console.log('创建页面: 解析参数:', { id, mode, initialDate });
      
      // 设置编辑模式
      isEdit.value = mode === 'edit' && id;
      
      // 如果有传入的日期，立即设置
      if (initialDate) {
        reminderDate.value = initialDate;
        console.log('创建页面: 设置传入的日期:', initialDate);
      }
      
      console.log('创建页面: 最终的初始值:', {
        reminderDate: reminderDate.value,
        reminderTime: reminderTime.value,
        isEdit: isEdit.value
      });
      
      return { id, mode, initialDate, options };
    };
    
    // 5. 在onMounted中统一处理
    onMounted(async () => {
      const { id, mode, initialDate, options } = processPageOptions();
      
      if (isEdit.value && id) {
        // 编辑模式：加载现有数据
        try {
          isSubmitting.value = true;
          const result = await getSimpleReminderById(id);
          if (result) {
            reminderForm.id = result.id;
            reminderForm.title = result.title;
            reminderForm.description = result.description;
            reminderForm.eventTime = result.eventTime;
            reminderForm.status = result.status;
            reminderForm.reminderType = result.reminderType || 'EMAIL';
            
            // 解析日期时间
            if (result.eventTime) {
              let eventTimeStr = result.eventTime;
              if (eventTimeStr.includes('T')) {
                const eventDate = new Date(eventTimeStr);
                if (!isNaN(eventDate.getTime())) {
                  const year = eventDate.getFullYear();
                  const month = String(eventDate.getMonth() + 1).padStart(2, '0');
                  const day = String(eventDate.getDate()).padStart(2, '0');
                  const hours = String(eventDate.getHours()).padStart(2, '0');
                  const minutes = String(eventDate.getMinutes()).padStart(2, '0');
                  
                  reminderDate.value = `${year}-${month}-${day}`;
                  reminderTime.value = `${hours}:${minutes}`;
                }
              } else {
                const [date, time] = eventTimeStr.split(' ');
                reminderDate.value = date;
                reminderTime.value = time ? time.substring(0, 5) : '09:00';
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
        // 创建模式：使用初始值
        console.log('创建页面: 创建模式，使用初始值');
      }
      
      // 更新事件时间
      reminderForm.eventTime = `${reminderDate.value} ${reminderTime.value}:00`;
      console.log('创建页面: 设置初始eventTime:', reminderForm.eventTime);
      
      // 标记数据已准备好
      isDataReady.value = true;
      console.log('创建页面: 数据准备完成，组件可以正常渲染');
    });
    
    // 日期时间组件变化处理
    const onDateTimeChange = (dateTimeData) => {
      reminderDate.value = dateTimeData.date;
      reminderTime.value = dateTimeData.time;
      reminderForm.eventTime = dateTimeData.eventTime;
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
            duration: 500
          });
          
          console.log('保存成功，1.5秒后返回');
          
          setTimeout(() => {
            console.log('创建页面: 准备返回日历页面');
            uni.navigateBack();
          }, 500);
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
      isDataReady,
      reminderForm,
      reminderDate,
      reminderTime,
      isSubmitting,
      reminderTypeOptions,
      reminderTypeIndex,
      dateTimePickerRef,
      onDateTimeChange,
      onReminderTypeChange,
      getReminderTypeIcon,
      getReminderTypeText: getReminderTypeTextUpdated,
      saveReminder,
      cancel,
      showReminderTypeSelector
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

/* 加载占位符样式 */
.loading-placeholder {
  padding: 32rpx;
  text-align: center;
  background-color: #f4efe7;
  border-radius: 24rpx;
  margin: 16rpx 0;
}

.loading-placeholder text {
  color: #9d8148;
  font-size: 28rpx;
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

/* 选择器显示样式 */
.picker-display {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
</style> 