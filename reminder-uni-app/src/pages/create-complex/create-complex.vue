<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <view class="nav-icon">
          <text class="icon-arrow">✕</text>
        </view>
      </view>
      <view class="nav-title"></view>
      <view class="nav-right"></view>
    </view>
    
    <!-- 主要内容区域 -->
    <scroll-view 
      class="content-scroll" 
      scroll-y 
      :scroll-with-animation="true"
      :enable-back-to-top="true"
      :scroll-top="scrollTop"
      :enhanced="true"
      :bounces="true"
      :show-scrollbar="false"
    >
      <view class="form-container">
        <!-- 基本信息 -->
        <view class="input-section">
          <input 
            class="title-input" 
            v-model="reminderData.title" 
            placeholder="标题"
            placeholder-class="input-placeholder"
            maxlength="50"
          />
        </view>
        
        <view class="input-section">
          <textarea 
            class="content-textarea" 
            v-model="reminderData.description" 
            placeholder="内容"
            placeholder-class="input-placeholder"
            maxlength="200"
            auto-height
          />
        </view>

        <!-- 提醒方式 -->
        <view class="setting-item" @click="showReminderTypeSelector">
          <text class="setting-label">提醒方式</text>
          <text class="setting-value">{{ reminderTypeOptions[reminderTypeIndex] }}</text>
        </view>

        <!-- 模式切换标签 -->
        <view class="tab-container">
          <view class="tab-buttons">
            <view 
              class="tab-button" 
              :class="{ active: activeTab === 'simple' }"
              @click="switchTab('simple')"
            >
              <text class="tab-text">简易模式</text>
            </view>
            <view 
              class="tab-button" 
              :class="{ active: activeTab === 'advanced' }"
              @click="switchTab('advanced')"
            >
              <text class="tab-text">高级模式</text>
            </view>
          </view>
        </view>
        
        <!-- 简易模式内容 -->
        <view v-if="activeTab === 'simple'">
          <!-- 重复设置 -->
          <view class="setting-item" @click="showRepeatSelector">
            <text class="setting-label">重复</text>
            <text class="setting-value">{{ repeatOptions[repeatIndex] }}</text>
          </view>

          <!-- 时间设置 -->
          <datetime-picker 
            ref="simpleTimePickerRef"
            label="提醒时间"
            :initial-date="simpleDate"
            :initial-time="simpleTime"
            :auto-set-default="!isEdit"
            :columns="timePickerColumns"
            @change="onSimpleTimeChange"
            @weekdayChange="onWeekdayChange"
          />

          <!-- Cron表达式输入（自定义重复时显示） -->
          <view v-if="showCronInput" class="input-group">
            <view class="input-label">
              <text class="label-text">Cron表达式</text>
            </view>
            <view class="input-wrapper">
              <input 
                class="form-input" 
                v-model="reminderData.cronExpression" 
                placeholder="Cron表达式 (例如: 0 0 8 * * ?)"
                placeholder-class="input-placeholder"
              />
            </view>
            <view v-if="cronPreview" class="cron-preview">
              <text class="preview-text">{{ cronPreview }}</text>
            </view>
          </view>

          <!-- 简单模式下的触发时间预览 -->
          <trigger-preview
            title="触发时间预览"
            :preview-times="previewTimes"
            :description="humanReadableDescription"
            :max-display="3"
            :show-description="true"
            :show-action-button="false"
            :highlight-first="true"
            :show-index="true"
            @refresh="updatePreview"
          />
        </view>
        
        <!-- 高级模式内容 -->
        <view v-if="activeTab === 'advanced'">
          <!-- 时间设置按钮 -->
          <view class="setting-item time-setting-item" @click="showTimeSettings">
            <text class="setting-label">时间设置</text>
            <view 
              class="setting-value-container" 
              :class="{ 'overflow': isTextOverflow }"
              :style="{ '--scroll-distance': scrollDistance }"
              ref="timeValueContainer"
            >
              <text class="setting-value scrollable-text" ref="timeValueText">{{ cronDescription }}</text>
            </view>
          </view>
          
          <!-- 触发时间预览 -->
          <trigger-preview
            title="触发时间预览"
            :preview-times="previewTimes"
            :description="humanReadableDescription"
            :max-display="3"
            :show-description="true"
            :show-action-button="false"
            :highlight-first="true"
            :show-index="true"
            @refresh="updatePreview"
          />
        </view>
        
        <!-- 额外的底部间距，确保页面可以向上滚动 -->
        <view class="extra-bottom-space"></view>
      </view>
    </scroll-view>
    
    <!-- 自定义日期时间选择器 -->
    <view class="custom-datetime" v-if="showCustomPickers" @touchmove.stop.prevent @click.self="hideCustomPickers">
      <view class="custom-modal" @click.stop>
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
              <view class="picker-display-modal">
                <text class="picker-text">{{ reminderDate || '选择日期' }}</text>
              </view>
            </picker>
          </view>
          
          <view class="picker-item">
            <text class="picker-label">时间</text>
            <picker mode="time" :value="reminderTime" @change="onTimeChange">
              <view class="picker-display-modal">
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
    
    <!-- 底部操作按钮 -->
    <view class="bottom-actions">
      <button 
        class="action-btn submit-btn" 
        @click="saveReminder" 
        :disabled="isSubmitting"
        :class="{ 'btn-loading': isSubmitting }"
      >
        <text class="btn-text" v-if="!isSubmitting">{{ isEdit ? '保存修改' : '创建提醒' }}</text>
        <text class="btn-text" v-else>保存中...</text>
      </button>
      <button class="action-btn cancel-btn" @click="goBack">
        <text class="btn-text">取消</text>
      </button>
    </view>
    
    <!-- Cron表达式选择器 -->
    <cron-expression-picker
      :show="showCronPicker"
      :initialValue="reminderData.cronExpression"
      @confirm="onCronConfirm"
      @cancel="onCronCancel"
      @update:show="showCronPicker = $event"
    />
  </view>
</template>

<script>
// 移除静态组件导入，改为按需加载
import { 
  createComplexReminder, 
  updateComplexReminder, 
  getComplexReminderById,
  smartRequestSubscribe
} from '@/services/api';
import { reminderState } from '@/services/store';
import { DateFormatter } from '@/utils/dateFormat';
import { isProductionVersion } from '@/config/version';
import { requireAuth } from '@/utils/auth';
import { cronstrue } from 'cronstrue/i18n';

export default {
  data() {
    return {
      isEdit: false,
      isSubmitting: false,
      activeTab: 'simple', 
      showCronPicker: false, 
      isTextOverflow: false, 
      scrollDistance: '-50%',
      scrollTop: 0,
      
      reminderTypeOptions: [],
      reminderTypeValues: [],
      reminderTypeIndex: 0,
      originalReminderType: null,

      repeatOptions: ['每日', '每周', '每月', '每年', '自定义'],
      repeatValues: ['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY', 'CUSTOM'],
      repeatIndex: 0,
      
      timePickerColumns: ['date', 'time'],
      simpleDate: '',
      simpleTime: '',
      selectedWeekday: 1, 

      showCustomPickers: false,
      reminderDate: '',
      reminderTime: '',
      
      previewTimes: [],
      cronPreview: '',
      humanReadableDescription: '',
    };
  },
  
  computed: {
    reminderData() {
      return reminderState.form || {};
    },
    showCronInput() {
      return this.repeatOptions[this.repeatIndex] === '自定义';
    },
    cronDescription() {
      return this.humanReadableDescription || '点击设置';
    }
  },

  async onLoad(options) {
    await requireAuth();
    this.isEdit = !!options.id;
    if (this.isEdit) {
      this.activeTab = 'advanced'; // 编辑模式默认进入高级模式
      await this.loadReminderData(options.id);
    } else {
      this.resetAndSetupNewReminder();
    }
    this.initReminderTypes(); // 在这里初始化提醒方式
  },
  
  methods: {
    initReminderTypes() {
      let options = [];
      let values = [];

      if (isProductionVersion()) {
        // 正式环境: 只有邮件和手机
        options = ['邮件', '手机'];
        values = ['EMAIL', 'SMS'];
        // 创建新提醒时，默认邮件
        if (!this.isEdit) {
          this.reminderData.reminderType = 'EMAIL';
        }
      } else {
        // 开发和测试环境: 只有微信
        options = ['微信'];
        values = ['WECHAT_MINI'];
        // 创建新提醒时，默认微信
        if (!this.isEdit) {
          this.reminderData.reminderType = 'WECHAT_MINI';
        }
      }

      this.reminderTypeOptions = options;
      this.reminderTypeValues = values;

      const defaultIndex = this.reminderTypeValues.indexOf(this.reminderData.reminderType);
      this.reminderTypeIndex = defaultIndex !== -1 ? defaultIndex : 0;
    },

    showReminderTypeSelector() {
      uni.showActionSheet({
        itemList: this.reminderTypeOptions,
        success: (res) => {
          const selectedIndex = res.tapIndex;
          this.reminderTypeIndex = selectedIndex;
          this.reminderData.reminderType = this.reminderTypeValues[selectedIndex];
        },
        fail: (err) => {
          console.log(err.errMsg);
        }
      });
    },

    resetAndSetupNewReminder() {
      const now = new Date();
      now.setMinutes(now.getMinutes() + 2);

      const defaultTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
      
      reminderState.form = {
        title: '',
        description: '',
        cronExpression: `0 ${String(now.getMinutes()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')} * * ?`,
        reminderType: '', // 让 initReminderTypes 来设置
        status: 'PENDING',
      };

      this.simpleTime = defaultTime;
      this.simpleDate = new Date().toISOString().split('T')[0];
      this.selectedWeekday = new Date().getDay() || 7; 
      this.originalReminderType = this.reminderData.reminderType;
      this.updateCronFromSimple();
    },

    async loadReminderData(id) {
      try {
        const data = await getComplexReminderById(id);
        if (data) {
          reminderState.form = data;
          this.originalReminderType = data.reminderType;
          // ... 其他数据恢复逻辑
          this.updateSimpleInputsFromCron(data.cronExpression);
          this.updatePreview();
        }
      } catch (error) {
        console.error('加载复杂提醒失败:', error);
      }
    },

    goBack() {
      const pages = getCurrentPages();
      if (pages.length <= 1) {
        // 如果没有上一页，则跳转到首页
        uni.switchTab({
          url: '/pages/index/index'
        });
      } else {
        uni.navigateBack();
      }
    },
    
    switchTab(tab) {
      console.log('switchTab called with:', tab);
      this.activeTab = tab;
    },
    
    showRepeatSelector() {
      console.log('showRepeatSelector called');
       uni.showActionSheet({
        itemList: this.repeatOptions,
        success: (res) => {
          this.repeatIndex = res.tapIndex;
          this.updateCronFromSimple();
        }
      });
    },

    onSimpleTimeChange(e) {
      console.log('onSimpleTimeChange called with:', e);
      this.simpleDate = e.date;
      this.simpleTime = e.time;
      this.updateCronFromSimple();
    },

    onWeekdayChange(e) {
      console.log('onWeekdayChange called with:', e);
      this.selectedWeekday = e.weekday;
      this.updateCronFromSimple();
    },

    updateCronFromSimple() {
      const repeatType = this.repeatValues[this.repeatIndex];
      const timeParts = this.simpleTime.split(':');
      const hours = timeParts[0];
      const minutes = timeParts[1];
      let cron = '';

      switch (repeatType) {
        case 'DAILY':
          cron = `0 ${minutes} ${hours} * * ?`;
          break;
        case 'WEEKLY':
          // cron星期几：1=周日, 2=周一, ... 7=周六
          // selectedWeekday: 1=周一, ... 7=周日
          const cronWeekday = this.selectedWeekday === 7 ? 1 : this.selectedWeekday + 1;
          cron = `0 ${minutes} ${hours} ? * ${cronWeekday}`;
          break;
        case 'MONTHLY':
          const dayOfMonth = this.simpleDate.split('-')[2];
          cron = `0 ${minutes} ${hours} ${dayOfMonth} * ?`;
          break;
        case 'YEARLY':
          const month = this.simpleDate.split('-')[1];
          const day = this.simpleDate.split('-')[2];
          cron = `0 ${minutes} ${hours} ${day} ${month} ?`;
          break;
        case 'CUSTOM':
          // 自定义模式下，不主动更新cron表达式，由用户手动输入
          return; 
      }
      this.reminderData.cronExpression = cron;
      this.updatePreview();
    },

    updateSimpleInputsFromCron(cron) {
      if (!cron) return;

      const parts = cron.split(' ');
      const minutes = parts[1];
      const hours = parts[2];
      const dayOfMonth = parts[3];
      const month = parts[4];
      const dayOfWeek = parts[5];

      this.simpleTime = `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
      
      if (dayOfWeek !== '?' && dayOfWeek !== '*') { // 每周
        this.repeatIndex = this.repeatValues.indexOf('WEEKLY');
        const cronWeekday = parseInt(dayOfWeek, 10);
        this.selectedWeekday = cronWeekday === 1 ? 7 : cronWeekday - 1;
      } else if (month !== '?' && month !== '*') { // 每年
        this.repeatIndex = this.repeatValues.indexOf('YEARLY');
        const today = new Date();
        this.simpleDate = `${today.getFullYear()}-${String(month).padStart(2, '0')}-${String(dayOfMonth).padStart(2, '0')}`;
      } else if (dayOfMonth !== '?' && dayOfMonth !== '*') { // 每月
        this.repeatIndex = this.repeatValues.indexOf('MONTHLY');
        const today = new Date();
        this.simpleDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(dayOfMonth).padStart(2, '0')}`;
      } else { // 每日
        this.repeatIndex = this.repeatValues.indexOf('DAILY');
      }
    },

    updatePreview() {
      if (!this.reminderData.cronExpression) {
        this.previewTimes = [];
        this.humanReadableDescription = '无效的表达式';
        return;
      }
      try {
        this.humanReadableDescription = cronstrue.toString(this.reminderData.cronExpression, { locale: 'zh_CN' });
        //... 更多预览逻辑
      } catch (e) {
        this.humanReadableDescription = 'Cron表达式解析失败';
        this.previewTimes = [];
      }
    },

    showTimeSettings() {
      console.log('showTimeSettings called');
      this.showCronPicker = true;
    },

    onCronConfirm(cron) {
      console.log('onCronConfirm called with:', cron);
      this.reminderData.cronExpression = cron;
      this.showCronPicker = false;
      this.updatePreview();
    },

    onCronCancel() {
      console.log('onCronCancel called');
      this.showCronPicker = false;
    },

    async saveReminder() {
      if (!this.reminderData.title) {
        uni.showToast({ title: '请输入提醒标题', icon: 'none' });
        return;
      }
      if (!this.reminderData.cronExpression) {
        uni.showToast({ title: '请设置提醒时间', icon: 'none' });
        return;
      }

      // 检查微信订阅权限
      if (this.needWechatSubscribe()) {
        const subResult = await smartRequestSubscribe({ showToast: true });
        if (!subResult.success || !subResult.granted) {
          uni.showModal({
            title: '授权失败',
            content: '微信提醒需要订阅消息授权，是否前往设置？',
            success: (res) => {
              if (res.confirm) uni.openSetting();
            }
          });
          return;
        }
      }

      this.isSubmitting = true;
      try {
        const dataToSave = { ...this.reminderData };
        let result;
        if (this.isEdit) {
          result = await updateComplexReminder(dataToSave.id, dataToSave);
        } else {
          result = await createComplexReminder(dataToSave);
        }

        if (result) {
          uni.showToast({
            title: this.isEdit ? '更新成功' : '创建成功',
            icon: 'success',
            duration: 1500,
          });
          setTimeout(() => uni.navigateBack(), 1500);
        }
      } catch (error) {
        console.error('保存复杂提醒失败:', error);
        // api.js中已有统一的错误提示
      } finally {
        this.isSubmitting = false;
      }
    },

    needWechatSubscribe() {
      // #ifdef MP-WEIXIN
      if (this.reminderData.reminderType !== 'WECHAT_MINI') {
        return false;
      }
      if (!this.isEdit) {
        return true; // 创建时，选择微信就需要
      }
      if (this.isEdit && this.originalReminderType !== 'WECHAT_MINI') {
        return true; // 编辑时，从其他方式改成微信需要
      }
      return false;
      // #endif
      return false;
    },
    
    onDateChange(e) {
      this.reminderDate = e.detail.value;
    },
    onTimeChange(e) {
      this.reminderTime = e.detail.value;
    },
    showCustomPickers() {
      // no-op
    },
    hideCustomPickers() {
      // no-op
    },
    confirmCustomDateTime() {
       // no-op
    }
  }
}
</script>

<style scoped>
.page-container {
  height: 100vh;
  background-color: #fcfbf8;
  display: flex;
  flex-direction: column;
  font-family: 'PingFang SC', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 导航栏样式 */
.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 88rpx;
  padding: 0 32rpx;
  background-color: #fcfbf8;
  border-bottom: none;
}

.nav-left {
  display: flex;
  align-items: center;
  width: 96rpx;
}

.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48rpx;
  height: 48rpx;
}

.icon-arrow {
  font-size: 40rpx;
  color: #1c170d;
  font-weight: 400;
  line-height: 1;
}

.nav-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #1c170d;
  text-align: center;
  flex: 1;
  margin-right: 96rpx;
}

.nav-right {
  width: 96rpx;
}

/* 内容区域 */
.content-scroll {
  flex: 1;
  padding: 0;
  padding-bottom: 160rpx; /* 为固定底部按钮留出空间 */
  /* 确保内容可以滚动 */
  min-height: 100%;
  overflow-y: auto;
}

.form-container {
  padding: 32rpx;
  max-width: 960rpx;
  margin: 0 auto;
  /* 确保容器有足够的最小高度 */
  min-height: calc(100vh - 160rpx);
}

/* 表单区块 */
.form-section {
  margin-bottom: 24rpx;
}

.section-header {
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #1c170d;
  line-height: 1.2;
}

/* 输入组样式 */
.input-group {
  margin-bottom: 32rpx;
}

.input-wrapper, .textarea-wrapper {
  background-color: #f4efe7;
  border-radius: 24rpx;
  border: none;
  overflow: hidden;
}

.form-input, .form-textarea {
  width: 100%;
  padding: 28rpx 32rpx;
  font-size: 32rpx;
  color: #1c170d;
  background-color: transparent;
  border: none;
  line-height: 1.4;
}

.form-textarea {
  min-height: 144rpx;
  resize: none;
}

.input-placeholder {
  color: #9d8148;
}

/* 单选按钮组 */
.radio-group {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.radio-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx;
  background-color: #ffffff;
  border: 2rpx solid #e9e0ce;
  border-radius: 24rpx;
  transition: all 0.2s ease;
}

.radio-item.active {
  border-color: #f7bd4a;
  background-color: #ffffff;
}

.radio-content {
  flex: 1;
}

.radio-text {
  font-size: 28rpx;
  font-weight: 500;
  color: #1c170d;
}

.radio-button {
  width: 40rpx;
  height: 40rpx;
  border: 4rpx solid #e9e0ce;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.radio-item.active .radio-button {
  border-color: #f7bd4a;
}

.radio-dot {
  width: 24rpx;
  height: 24rpx;
  background-color: #f7bd4a;
  border-radius: 50%;
}

/* 底部按钮 */
.bottom-actions {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 32rpx;
  padding: 24rpx 32rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom)); /* 适配安全区域 */
  background-color: #fcfbf8;
  border-top: 1rpx solid #e9e0ce;
  z-index: 100;
}

.action-btn {
  flex: 1;
  height: 96rpx;
  border-radius: 16rpx;
  font-size: 32rpx;
  font-weight: 600;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cancel-btn {
  background-color: #f8f9fa;
  color: #666666;
}

.submit-btn {
  background-color: #f7bd4a;
  color: #1c170d;
}

.submit-btn:disabled,
.btn-loading {
  background-color: #cccccc;
  color: #999999;
}

.btn-text {
  font-size: 32rpx;
  font-weight: 600;
}

/* 标签切换 */
.tab-container {
  padding: 0 0 0rpx;
}

.tab-buttons {
  display: flex;
  background-color: #f0f0f0;
  border-radius: 16rpx;
  padding: 8rpx;
}

.tab-button {
  flex: 1;
  text-align: center;
  padding: 24rpx;
  border-radius: 12rpx;
  transition: all 0.3s ease;
}

.tab-button.active {
  background-color: #f7bd4a;
}

.tab-text {
  font-size: 28rpx;
  color: #666666;
  font-weight: 500;
}

.tab-button.active .tab-text {
  color: #1c170d;
  font-weight: 600;
}

/* 标签内容 */
.tab-content {
  padding: 0;
}

/* 标签内容中的section-header */
.tab-content .section-header {
  margin: 48rpx 0 24rpx 0;
}

.tab-content .section-header:first-of-type {
  margin-top: 24rpx;
}

/* 输入标签和选择器 */
.input-label {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.label-text {
  font-size: 28rpx;
  color: #1c170d;
  font-weight: 500;
}

.required-mark {
  color: #ff4757;
  font-size: 28rpx;
}

/* 选择器样式 */
.picker-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx;
  background-color: #f4efe7;
  border-radius: 24rpx;
  border: none;
}

.picker-icon {
  font-size: 32rpx;
  margin-right: 16rpx;
}

.picker-text {
  flex: 1;
  font-size: 28rpx;
  color: #1c170d;
}

.picker-arrow {
  font-size: 32rpx;
  color: #9d8148;
}

/* 时间选择器 */
.time-picker-container {
  width: 100%;
}

/* 时间选择器行 */
.time-picker-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.time-picker-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.time-label {
  font-size: 24rpx;
  color: #9d8148;
  font-weight: 500;
  text-align: center;
}

.time-separator {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: 32rpx;
}

.separator-text {
  font-size: 36rpx;
  font-weight: 600;
  color: #1c170d;
}

/* 日期时间选择器布局 */
.date-time-container {
  display: flex;
  gap: 24rpx;
}

.date-time-container .picker-display {
  flex: 1;
}

/* Cron表达式预览 */
.cron-preview {
  margin-top: 16rpx;
  padding: 24rpx;
  background-color: #f0f8ff;
  border-radius: 16rpx;
  border-left: 6rpx solid #007aff;
}

.preview-text {
  font-size: 28rpx;
  color: #007aff;
  line-height: 1.4;
}

/* 年度日期选择容器 */
.year-date-container {
  display: flex;
  gap: 24rpx;
}

.year-date-container .picker-display {
  flex: 1;
}

/* 设置项样式 */
.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 0;
  border-bottom: 1rpx solid #e9e0ce;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-label {
  font-size: 32rpx;
  font-weight: 400;
  color: #1c170d;
  line-height: 1.4;
  flex-shrink: 0;
  min-width: 160rpx;
  max-width: 40%;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}

.setting-value {
  font-size: 32rpx;
  font-weight: 400;
  color: #1c170d;
  line-height: 1.4;
  flex: 1;
  max-width: 60%;
  text-align: right;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.value-text {
  font-size: 32rpx;
  color: #1c170d;
  font-weight: 400;
}

/* 高级选项 */
.advanced-options {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.option-item {
  background-color: #ffffff;
  border: 2rpx solid #e9e0ce;
  border-radius: 24rpx;
  overflow: hidden;
}

.option-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 32rpx;
  cursor: pointer;
}

.option-title {
  font-size: 28rpx;
  color: #1c170d;
  font-weight: 500;
}

.option-toggle {
  transition: transform 0.3s ease;
}

.option-toggle.expanded {
  transform: rotate(180deg);
}

.toggle-icon {
  font-size: 32rpx;
  color: #1c170d;
}

.option-content {
  padding: 0 32rpx 30rpx;
  border-top: 1rpx solid #f0f0f0;
}

.option-value {
  font-size: 24rpx;
  color: #9d8148;
  font-weight: 400;
  padding-top: 16rpx;
}

.option-arrow {
  font-size: 32rpx;
  color: #9d8148;
  font-weight: 400;
}

.option-description {
  padding: 0 32rpx 16rpx;
}

.option-desc-text {
  font-size: 24rpx;
  color: #9d8148;
  font-weight: 400;
}

/* Cron表达式显示 */
.cron-display-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  padding: 24rpx 32rpx;
  background-color: #f4efe7;
  border-radius: 16rpx;
  border: 2rpx solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.cron-display-wrapper:active {
  background-color: #f0ede4;
  border-color: #f7bd4a;
}

/* 只读Cron表达式显示 */
.cron-display-readonly {
  padding: 24rpx 32rpx;
  background-color: #f8f9fa;
  border-radius: 16rpx;
  border: 1rpx solid #e9ecef;
}

.cron-description {
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
  line-height: 1.4;
}

.cron-expression {
  font-size: 24rpx;
  color: #9d8148;
  font-family: 'Courier New', monospace;
  line-height: 1.2;
}

.picker-arrow {
  position: absolute;
  right: 24rpx;
  top: 50%;
  transform: translateY(-50%);
  font-size: 32rpx;
  color: #9d8148;
  font-weight: 600;
}

/* Cron帮助信息 */
.cron-help {
  margin-top: 16rpx;
  padding: 24rpx;
  background-color: #f0f8ff;
  border-radius: 16rpx;
  border-left: 6rpx solid #f7bd4a;
}

.help-text {
  font-size: 24rpx;
  color: #666666;
  line-height: 1.4;
}

/* 响应式调整 */
@media (max-width: 750rpx) {
  .year-date-container {
    flex-direction: column;
    gap: 24rpx;
  }
  
  .half-width {
    flex: none;
    width: 100%;
  }
  
  .form-container {
    padding: 24rpx;
  }
  
  .tab-button {
    font-size: 28rpx;
  }
  
  .picker-display {
    padding: 20rpx 24rpx;
  }
  
  .section-title {
    font-size: 32rpx;
  }
}

/* 简单页面样式 */
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
  flex-shrink: 0;
  min-width: 160rpx;
  max-width: 40%;
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

.close-icon {
  font-size: 48rpx;
  color: #1c170d;
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

.picker-display-modal {
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

/* 时间设置项特殊样式 */
.setting-item.time-setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  background-color: #fcfbf8;
  padding: 32rpx;
  min-height: 112rpx;
  cursor: pointer;
}

.setting-item.time-setting-item:active {
  background-color: #f4efe7;
}

.setting-value-container {
  flex: 1;
  min-width: 0;
  text-align: right;
  overflow: hidden;
  position: relative;
  height: 40rpx;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.scrollable-text {
  font-size: 28rpx;
  color: #1c170d;
  font-weight: 400;
  line-height: 1.4;
  white-space: nowrap;
  display: inline-block;
  max-width: 100%;
  text-align: right;
}

/* 当文本超出容器时启用跑马灯效果 */
.setting-value-container.overflow {
  /* border: 2rpx solid red; */ /* 调试用：显示overflow状态 */
}

.setting-value-container.overflow .scrollable-text {
  animation: marquee 6s linear infinite;
  animation-delay: 0.5s;
  /* 移除text-overflow以显示完整文本 */
  text-overflow: unset;
  overflow: visible;
}

.setting-value-container:hover.overflow .scrollable-text,
.setting-item.time-setting-item:active .setting-value-container.overflow .scrollable-text {
  animation-duration: 3s;
}

@keyframes marquee {
  0% {
    transform: translateX(0);
  }
  25% {
    transform: translateX(0);
  }
  75% {
    transform: translateX(var(--scroll-distance, -150px));
  }
  100% {
    transform: translateX(var(--scroll-distance, -150px));
  }
}

/* 当文本不超出时，正常显示 */
.setting-value-container:not(.overflow) .scrollable-text {
  text-overflow: ellipsis;
  overflow: hidden;
}

/* 额外的底部间距，确保页面可以向上滚动 */
.extra-bottom-space {
  height: 200rpx; /* 额外的底部空间 */
  width: 100%;
}
</style>