<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="nav-bar">
      <view class="nav-left" @click="cancel">
        <text class="nav-icon">←</text>
        <text class="nav-text">返回</text>
      </view>
      <view class="nav-title">{{ isEdit ? '编辑提醒' : '创建提醒' }}</view>
      <view class="nav-right"></view>
    </view>
    
    <!-- 主要内容区域 -->
    <scroll-view class="content-scroll" scroll-y>
      <view class="form-container">
        <!-- 标题输入 -->
        <view class="form-section">
          <view class="section-header">
            <text class="section-icon">📝</text>
            <text class="section-title">基本信息</text>
          </view>
          
          <view class="input-group">
            <view class="input-label">
              <text class="label-text">标题</text>
              <text class="required-mark">*</text>
            </view>
            <view class="input-wrapper">
              <input 
                class="form-input" 
                v-model="reminderForm.title" 
                placeholder="请输入提醒标题"
                placeholder-class="input-placeholder"
                maxlength="50"
              />
            </view>
          </view>
          
          <view class="input-group">
            <view class="input-label">
              <text class="label-text">内容</text>
            </view>
            <view class="textarea-wrapper">
              <textarea 
                class="form-textarea" 
                v-model="reminderForm.description" 
                placeholder="请输入提醒内容（可选）"
                placeholder-class="input-placeholder"
                maxlength="200"
                auto-height
              />
            </view>
          </view>
        </view>
        
        <!-- 时间设置 -->
        <view class="form-section">
          <view class="section-header">
            <text class="section-icon">⏰</text>
            <text class="section-title">时间设置</text>
          </view>
          
          <view class="input-group">
            <view class="input-label">
              <text class="label-text">提醒时间</text>
              <text class="required-mark">*</text>
            </view>
            <view class="datetime-container">
              <picker mode="date" :value="reminderDate" @change="onDateChange" class="datetime-picker">
                <view class="picker-display date-display">
                  <text class="picker-icon">📅</text>
                  <text class="picker-text">{{ reminderDate || '选择日期' }}</text>
                </view>
              </picker>
              <picker mode="time" :value="reminderTime" @change="onTimeChange" class="datetime-picker">
                <view class="picker-display time-display">
                  <text class="picker-icon">🕐</text>
                  <text class="picker-text">{{ reminderTime || '选择时间' }}</text>
                </view>
              </picker>
            </view>
          </view>
        </view>
        
        <!-- 重复设置 -->
        <view class="form-section">
          <view class="section-header">
            <text class="section-icon">🔄</text>
            <text class="section-title">重复设置</text>
          </view>
          
          <view class="input-group">
            <view class="input-label">
              <text class="label-text">重复频率</text>
            </view>
            <picker :range="repeatOptions" :value="repeatIndex" @change="onRepeatChange">
              <view class="picker-display repeat-display">
                <text class="picker-icon">🔁</text>
                <text class="picker-text">{{ repeatOptions[repeatIndex] }}</text>
                <text class="picker-arrow">›</text>
              </view>
            </picker>
          </view>
          
          <!-- Cron表达式输入 -->
          <view class="input-group" v-if="showCronInput">
            <view class="input-label">
              <text class="label-text">Cron表达式</text>
            </view>
            <view class="input-wrapper">
              <input 
                class="form-input cron-input" 
                v-model="reminderForm.cronExpression" 
                placeholder="请输入Cron表达式，如：0 0 8 * * ?"
                placeholder-class="input-placeholder"
              />
            </view>
            <view class="cron-preview" v-if="cronPreview">
              <text class="preview-icon">💡</text>
              <text class="preview-text">{{ cronPreview }}</text>
            </view>
          </view>
        </view>
        
        <!-- 提醒方式设置 -->
        <view class="form-section">
          <view class="section-header">
            <text class="section-icon">📢</text>
            <text class="section-title">提醒方式</text>
          </view>
          
          <view class="input-group">
            <view class="input-label">
              <text class="label-text">提醒方式</text>
            </view>
            <picker :range="reminderTypeOptions" :value="reminderTypeIndex" @change="onReminderTypeChange">
              <view class="picker-display reminder-type-display">
                <text class="picker-icon">{{ getReminderTypeIcon(reminderForm.reminderType) }}</text>
                <text class="picker-text">{{ getReminderTypeText(reminderForm.reminderType) }}</text>
                <text class="picker-arrow">›</text>
              </view>
            </picker>
          </view>
        </view>
      </view>
    </scroll-view>
    
    <!-- 底部操作按钮 -->
    <view class="bottom-actions">
      <button class="action-btn cancel-btn" @click="cancel">
        <text class="btn-text">取消</text>
      </button>
      <button 
        class="action-btn submit-btn" 
        @click="saveReminder" 
        :disabled="isSubmitting"
        :class="{ 'btn-loading': isSubmitting }"
      >
        <text class="btn-text" v-if="!isSubmitting">{{ isEdit ? '保存修改' : '创建提醒' }}</text>
        <text class="btn-text" v-else>保存中...</text>
      </button>
    </view>
  </view>
</template>

<script>
import { ref, computed, reactive, onMounted, watch, getCurrentInstance } from 'vue';
import { createEvent, updateEvent, getSimpleReminderById } from '../../services/api';
import cronstrue from 'cronstrue/i18n';

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
      cronExpression: '',
      status: 'PENDING' // 默认为PENDING
    });
    
    const reminderDate = ref('');
    const reminderTime = ref('');
    const isSubmitting = ref(false);
    
    const repeatOptions = ['不重复', '每天', '每周', '每月', '自定义'];
    const repeatIndex = ref(0); 
    
    // 提醒方式相关
    const reminderTypeOptions = ['邮件提醒', '短信提醒', '微信小程序提醒'];
    const reminderTypeValues = ['EMAIL', 'SMS', 'WECHAT_MINI'];
    const reminderTypeIndex = ref(0); // 默认选择邮件提醒
    
    const showCronInput = computed(() => repeatIndex.value === 4); // 自定义时显示Cron输入框

    const cronPreview = computed(() => {
      if (reminderForm.cronExpression && repeatIndex.value === 4) {
        try {
          return cronstrue.toString(reminderForm.cronExpression, { locale: "zh_CN" });
        } catch (e) {
          return '无效的Cron表达式';
        }
      }
      return '';
    });
    
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
            reminderForm.cronExpression = result.cronExpression;
            reminderForm.status = result.status;
            reminderForm.reminderType = result.reminderType || 'EMAIL'; // 设置提醒方式
            
            if (result.eventTime) {
              // 处理不同格式的日期时间字符串
              let eventTimeStr = result.eventTime;
              
              // 如果是ISO格式，先转换为本地时间字符串
              if (eventTimeStr.includes('T')) {
                const eventDate = new Date(eventTimeStr);
                const year = eventDate.getFullYear();
                const month = String(eventDate.getMonth() + 1).padStart(2, '0');
                const day = String(eventDate.getDate()).padStart(2, '0');
                const hours = String(eventDate.getHours()).padStart(2, '0');
                const minutes = String(eventDate.getMinutes()).padStart(2, '0');
                const seconds = String(eventDate.getSeconds()).padStart(2, '0');
                eventTimeStr = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
              }
              
              const [date, time] = eventTimeStr.split(' ');
              reminderDate.value = date;
              reminderTime.value = time.substring(0, 5); // HH:mm
            }
            
            if (result.cronExpression) {
              if (result.cronExpression === '0 0 8 * * ?') repeatIndex.value = 1;
              else if (result.cronExpression === '0 0 8 ? * MON') repeatIndex.value = 2;
              else if (result.cronExpression === '0 0 8 1 * ?') repeatIndex.value = 3;
              else repeatIndex.value = 4; // 自定义
            } else {
              repeatIndex.value = 0; // 不重复
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
    
    const onRepeatChange = (e) => {
      repeatIndex.value = e.detail.value;
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
    
    watch(repeatIndex, (newIndex) => {
      switch (Number(newIndex)) {
        case 0: reminderForm.cronExpression = ''; break;
        case 1: reminderForm.cronExpression = '0 0 8 * * ?'; break; // 每天早上8点
        case 2: reminderForm.cronExpression = '0 0 8 ? * MON'; break; // 每周一早上8点
        case 3: reminderForm.cronExpression = '0 0 8 1 * ?'; break; // 每月1号早上8点
        // case 4 (自定义) 不做处理，用户自行输入
      }
    });
    
    const saveReminder = async () => {
      if (!reminderForm.title) {
        uni.showToast({ title: '请输入提醒标题', icon: 'none' });
        return;
      }
      if (!reminderForm.eventTime) {
        uni.showToast({ title: '请选择提醒时间', icon: 'none' });
        return;
      }
      if (repeatIndex.value === 4 && !reminderForm.cronExpression) {
        uni.showToast({ title: '自定义重复需要填写Cron表达式', icon: 'none' });
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
        
        // 如果不是自定义重复，且cronExpression为空（例如不重复），则确保不传递cronExpression
        if (repeatIndex.value !== 4 && !dataToSave.cronExpression) {
            delete dataToSave.cronExpression;
        }

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
    
    return {
      isEdit,
      reminderForm,
      reminderDate,
      reminderTime,
      isSubmitting,
      repeatOptions,
      repeatIndex,
      showCronInput,
      cronPreview,
      reminderTypeOptions,
      reminderTypeIndex,
      onDateChange,
      onTimeChange,
      onRepeatChange,
      onReminderTypeChange,
      getReminderTypeIcon,
      getReminderTypeText,
      saveReminder,
      cancel
    };
  }
};
</script>

<style>
.page-container {
  height: 100vh;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
}

/* 导航栏样式 */
.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15rpx 25rpx;
  background-color: #ffffff;
  border-bottom: 1px solid #ebeef5;
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-left {
  display: flex;
  align-items: center;
  flex: 1;
  cursor: pointer;
}

.nav-icon {
  font-size: 36rpx;
  color: #409eff;
  margin-right: 6rpx;
}

.nav-text {
  font-size: 26rpx;
  color: #409eff;
}

.nav-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #303133;
  text-align: center;
  flex: 2;
}

.nav-right {
  flex: 1;
}

/* 内容滚动区域 */
.content-scroll {
  flex: 1;
  overflow-y: auto;
}

.form-container {
  padding: 20rpx;
}

/* 表单区块样式 */
.form-section {
  background-color: #ffffff;
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.section-header {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
  padding-bottom: 12rpx;
  border-bottom: 1px solid #f0f2f5;
}

.section-icon {
  font-size: 32rpx;
  margin-right: 8rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #303133;
}

/* 输入组样式 */
.input-group {
  margin-bottom: 20rpx;
}

.input-group:last-child {
  margin-bottom: 0;
}

.input-label {
  display: flex;
  align-items: center;
  margin-bottom: 10rpx;
}

.label-text {
  font-size: 26rpx;
  color: #606266;
  font-weight: 500;
}

.required-mark {
  color: #f56c6c;
  margin-left: 4rpx;
  font-size: 26rpx;
}

/* 输入框样式 */
.input-wrapper, .textarea-wrapper {
  background-color: #f8f9fa;
  border-radius: 8rpx;
  border: 2rpx solid transparent;
  transition: all 0.3s ease;
}

.input-wrapper:focus-within, .textarea-wrapper:focus-within {
  border-color: #409eff;
  background-color: #ffffff;
  box-shadow: 0 0 0 3rpx rgba(64, 158, 255, 0.1);
}

.form-input {
  width: 100%;
  padding: 18rpx;
  font-size: 26rpx;
  color: #303133;
  background-color: transparent;
  border: none;
  outline: none;
}

.form-textarea {
  width: 100%;
  padding: 18rpx;
  font-size: 26rpx;
  color: #303133;
  background-color: transparent;
  border: none;
  outline: none;
  min-height: 80rpx;
  resize: none;
}

.input-placeholder {
  color: #c0c4cc;
}

/* 日期时间选择器样式 */
.datetime-container {
  display: flex;
  gap: 12rpx;
}

.datetime-picker {
  flex: 1;
}

.picker-display {
  display: flex;
  align-items: center;
  padding: 18rpx;
  background-color: #f8f9fa;
  border-radius: 8rpx;
  border: 2rpx solid transparent;
  transition: all 0.3s ease;
}

.picker-display:active {
  background-color: #e9ecef;
  transform: scale(0.98);
}

.date-display {
  flex: 1.5;
}

.time-display {
  flex: 1;
}

.repeat-display, .reminder-type-display {
  justify-content: space-between;
}

.picker-icon {
  font-size: 28rpx;
  margin-right: 8rpx;
}

.picker-text {
  font-size: 26rpx;
  color: #303133;
  flex: 1;
}

.picker-arrow {
  font-size: 28rpx;
  color: #c0c4cc;
  margin-left: 8rpx;
}

/* Cron表达式预览 */
.cron-preview {
  margin-top: 12rpx;
  padding: 15rpx;
  background-color: #f0f9ff;
  border-radius: 8rpx;
  border-left: 4rpx solid #409eff;
  display: flex;
  align-items: flex-start;
}

.preview-icon {
  font-size: 28rpx;
  margin-right: 8rpx;
  margin-top: 2rpx;
}

.preview-text {
  font-size: 24rpx;
  color: #409eff;
  line-height: 1.4;
  flex: 1;
}

/* 底部操作按钮 */
.bottom-actions {
  display: flex;
  gap: 16rpx;
  padding: 20rpx;
  background-color: #ffffff;
  border-top: 1px solid #ebeef5;
  box-shadow: 0 -2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.action-btn {
  flex: 1;
  height: 76rpx;
  border-radius: 38rpx;
  border: none;
  font-size: 28rpx;
  font-weight: 600;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.action-btn:active {
  transform: scale(0.98);
}

.cancel-btn {
  background-color: #f5f7fa;
  color: #909399;
}

.cancel-btn:active {
  background-color: #e4e7ed;
}

.submit-btn {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: #ffffff;
  box-shadow: 0 3rpx 10rpx rgba(103, 194, 58, 0.3);
}

.submit-btn:active {
  background: linear-gradient(135deg, #5daf34 0%, #7bc143 100%);
}

.submit-btn:disabled,
.btn-loading {
  background: linear-gradient(135deg, #c0c4cc 0%, #d3d4d6 100%);
  box-shadow: none;
  transform: none;
}

.btn-text {
  font-size: 28rpx;
  font-weight: 600;
}

/* 加载状态动画 */
.btn-loading::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 25rpx;
  width: 24rpx;
  height: 24rpx;
  margin-top: -12rpx;
  border: 3rpx solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 响应式适配 */
@media (max-width: 750rpx) {
  .form-container {
    padding: 16rpx;
  }
  
  .form-section {
    padding: 18rpx;
    margin-bottom: 12rpx;
  }
  
  .datetime-container {
    flex-direction: column;
    gap: 10rpx;
  }
  
  .bottom-actions {
    padding: 16rpx;
    gap: 12rpx;
  }
}
</style> 