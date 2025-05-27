<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <view class="nav-icon">
          <text class="icon-arrow">←</text>
        </view>
      </view>
      <view class="nav-title">{{ isEdit ? '编辑高级提醒' : '创建高级提醒' }}</view>
      <view class="nav-right"></view>
    </view>
    
    <!-- 主要内容区域 -->
    <scroll-view class="content-scroll" scroll-y>
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

        <!-- 时间设置模式选择 -->
        <view class="form-section">
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
          <view v-if="activeTab === 'simple'" class="tab-content">
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
          <view v-if="activeTab === 'advanced'" class="tab-content">
            <!-- 时间设置按钮 -->
            <view class="option-item" @click="showTimeSettings">
              <view class="option-header">
                <text class="option-title">时间设置</text>
                <text class="option-arrow">›</text>
              </view>
              <view class="cron-display-readonly">
                <text class="cron-description">{{ cronDescription }}</text>
              </view>
            </view>

            <!-- 下次触发时间预览 -->
            <trigger-preview
              title="下次触发时间"
              :preview-times="previewTimes"
              :description="humanReadableDescription"
              :max-display="5"
              :show-description="true"
              :show-action-button="true"
              :highlight-first="true"
              @refresh="updatePreview"
              @copy-description="onCopyDescription"
              @copy-times="onCopyTimes"
            />
          </view>
        </view>
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
    <CronExpressionPicker 
      :show="showCronPicker"
      :initialValue="reminderData.cronExpression"
      @confirm="onCronConfirm"
      @cancel="onCronCancel"
      @update:show="showCronPicker = $event"
    />
  </view>
</template>

<script>
import CronExpressionPicker from '../../components/CronExpressionPicker.vue';
import DateTimePicker from '../../components/datetime-picker/datetime-picker.vue';
import TriggerPreview from '../../components/trigger-preview/trigger-preview.vue';

export default {
  components: {
    CronExpressionPicker,
    DateTimePicker,
    TriggerPreview
  },
  data() {
    return {
      isEdit: false,
      isSubmitting: false,
      activeTab: 'simple', // 'simple' 或 'advanced'
      showCronExpression: false, // 控制Cron表达式展开
      showCronPicker: false, // 控制Cron选择器显示
      
      // 提醒数据
      reminderData: {
        id: null,
        title: '',
        description: '',
        reminderType: 'EMAIL',
        cronExpression: '0 8 * * *', // 默认每天8点
        validFrom: '',
        validUntil: '',
        maxExecutions: null
      },
      
      // 提醒方式选项
      reminderTypeOptions: ['邮件提醒', '短信提醒'],
      reminderTypeValues: ['EMAIL', 'SMS'],
      reminderTypeIndex: 0,
      
      // 自定义时间选择器显示状态
      showCustomPickers: false,
      
      // 简单模式的日期时间设置
      reminderDate: '',
      reminderTime: '',
      simpleDate: '',
      simpleTime: '',
      
      // 重复选项
      repeatOptions: ['不重复', '每天', '每周', '每月'],
      repeatIndex: 0,
      
      // 时间选择器选项
      hourOptions: Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0')),
      minuteOptions: Array.from({ length: 60 }, (_, i) => String(i).padStart(2, '0')),
      selectedHour: 8, // 默认8点
      selectedMinute: 0, // 默认0分
      
      // 简易模式数据
      simpleData: {
        recurrenceType: 'DAILY',
        hour: 8,
        minute: 0,
        weekday: 1, // 周一
        dayOfMonth: 1,
        month: 1
      },
      simpleTime: '08:00',
      
      // 选项数据
      recurrenceOptions: ['每天', '每周', '每月', '每年'],
      recurrenceValues: ['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY'],
      recurrenceIndex: 0,
      
      weekDays: ['周日', '周一', '周二', '周三', '周四', '周五', '周六'],
      weekdayIndex: 1,
      
      months: ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月'],
      monthIndex: 0,
      
      monthDays: [],
      monthDayIndex: 0,
      
      dayIndex: 0,
      
      // 预览数据
      previewTimes: [],
      humanReadableDescription: '每天上午8:00'
    }
  },
  
  computed: {
    // 控制Cron输入框显示
    showCronInput() {
      return this.repeatIndex === 4; // 自定义时显示
    },
    
    // Cron表达式预览
    cronPreview() {
      if (this.reminderData.cronExpression && this.repeatIndex === 4) {
        try {
          // 这里可以添加cronstrue库来解析Cron表达式
          return '自定义Cron表达式';
        } catch (e) {
          return '无效的Cron表达式';
        }
      }
      return '';
    },
    
    // Cron表达式文字描述
    cronDescription() {
      return this.parseCronToDescription(this.reminderData.cronExpression);
    },
    
    // 根据重复类型动态确定时间选择器显示的列
    timePickerColumns() {
      switch (this.repeatIndex) {
        case 0: // 不重复 - 显示月日时分
          return ['month', 'day', 'hour', 'minute'];
        case 1: // 每天 - 只显示时分
          return ['hour', 'minute'];
        case 2: // 每周 - 显示周几和时分
          return ['weekday', 'hour', 'minute'];
        case 3: // 每月 - 显示日时分
          return ['day', 'hour', 'minute'];
        default:
          return ['month', 'day', 'hour', 'minute'];
      }
    }
  },
  
  watch: {
    // 监听重复选项变化，自动更新Cron表达式
    repeatIndex(newIndex) {
      this.updateCronFromRepeat();
    },
    
    // 监听时间变化，更新Cron表达式中的时间部分
    reminderTime(newTime) {
      if (newTime && this.repeatIndex > 0 && this.repeatIndex < 4) {
        const [hour, minute] = newTime.split(':');
        const cronParts = this.reminderData.cronExpression.split(' ');
        if (cronParts.length >= 2) {
          cronParts[0] = minute || '0';
          cronParts[1] = hour || '8';
          this.reminderData.cronExpression = cronParts.join(' ');
        }
      }
    }
  },
  
  onLoad(options) {
    console.log('复杂提醒页面加载参数:', options);
    if (options.id) {
      this.isEdit = true;
      this.loadReminderData(options.id);
    }
    this.initializeData();
    this.generateMonthDays();
    this.updatePreview();
  },
  
  methods: {
    // 初始化数据
    initializeData() {
      const today = new Date();
      this.reminderData.validFrom = today.toISOString().split('T')[0];
      
      // 初始化简单模式的日期时间
      this.reminderDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
      this.simpleDate = this.reminderDate;
      
      // 设置默认时间为当前时间的后一小时整点
      const now = new Date();
      now.setHours(now.getHours() + 1);
      now.setMinutes(0);
      now.setSeconds(0);
      this.reminderTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
      this.simpleTime = this.reminderTime;
      
      // 同步简单模式数据
      this.simpleData.hour = now.getHours();
      this.simpleData.minute = 0;
      // 使用默认的重复选项（repeatIndex = 0 对应 "不重复"）
      this.updateCronFromRepeat();
      
      // 更新eventTime
      this.updateEventTime();
      
      console.log('初始化数据完成:');
      console.log('今天日期:', this.reminderDate);
      console.log('设置时间:', this.reminderTime);
      console.log('简单模式数据:', this.simpleData);
    },
    
    // 切换标签
    switchTab(tab) {
      this.activeTab = tab;
      console.log('切换到模式:', tab);
      
      if (tab === 'simple') {
        // 确保简单模式数据完整
        if (!this.simpleData.hour && !this.simpleData.minute) {
          this.simpleData.hour = 8;
          this.simpleData.minute = 0;
          this.simpleTime = '08:00';
        }
        this.updateCronFromSimple();
      }
      
      this.updatePreview();
    },
    
    // 提醒方式改变
    onReminderTypeChange(e) {
      this.reminderTypeIndex = e.detail.value;
      this.reminderData.reminderType = this.reminderTypeValues[this.reminderTypeIndex];
    },
    
    // 简单模式 - 日期改变
    onDateChange(e) {
      this.reminderDate = e.detail.value;
      this.updateEventTime();
    },
    
    // 简单模式 - 时间改变
    onTimeChange(e) {
      this.reminderTime = e.detail.value;
      this.updateEventTime();
    },
    
    // 简单模式 - 重复选项改变
    onRepeatChange(e) {
      this.repeatIndex = e.detail.value;
      this.updateCronFromRepeat();
    },
    
    // 小时选择改变
    onHourChange(e) {
      this.selectedHour = e.detail.value;
      this.updateCronFromTime();
    },
    
    // 分钟选择改变
    onMinuteChange(e) {
      this.selectedMinute = e.detail.value;
      this.updateCronFromTime();
    },
    
    // 简单模式时间变化处理
    onSimpleTimeChange(dateTimeData) {
      this.simpleDate = dateTimeData.date;
      this.simpleTime = dateTimeData.time;
      
      // 解析时间
      const [hour, minute] = dateTimeData.time.split(':');
      
      // 更新简易模式数据
      this.simpleData.hour = parseInt(hour);
      this.simpleData.minute = parseInt(minute);
      
      // 解析日期（如果有的话）
      if (dateTimeData.date) {
        const [year, month, day] = dateTimeData.date.split('-');
        this.simpleData.month = parseInt(month);
        this.simpleData.dayOfMonth = parseInt(day);
      }
      
      // 根据当前重复类型更新Cron表达式
      this.updateCronFromSimple();
      
      // 更新预览
      this.updatePreview();
    },
    
    // 周几变化处理
    onWeekdayChange(weekday) {
      // weekday: 0-6 (周日到周六)
      this.simpleData.weekday = weekday;
      
      // 根据当前重复类型更新Cron表达式
      this.updateCronFromSimple();
      
      // 更新预览
      this.updatePreview();
    },
    
    // 根据时间选择更新Cron表达式
    updateCronFromTime() {
      const hour = parseInt(this.hourOptions[this.selectedHour]);
      const minute = parseInt(this.minuteOptions[this.selectedMinute]);
      
      // 更新简易模式数据
      this.simpleData.hour = hour;
      this.simpleData.minute = minute;
      
      // 根据当前重复类型更新Cron表达式
      switch (Number(this.repeatIndex)) {
        case 1: // 每天
          this.reminderData.cronExpression = `0 ${minute} ${hour} * * ?`;
          break;
        case 2: // 每周
          this.reminderData.cronExpression = `0 ${minute} ${hour} ? * MON`;
          break;
        case 3: // 每月
          this.reminderData.cronExpression = `0 ${minute} ${hour} 1 * ?`;
          break;
        default:
          // 不重复或自定义不处理
          break;
      }
    },
    
    // 更新eventTime
    updateEventTime() {
      if (this.reminderDate && this.reminderTime) {
        this.reminderData.eventTime = `${this.reminderDate} ${this.reminderTime}:00`;
      } else {
        this.reminderData.eventTime = '';
      }
    },
    
    // 根据重复选项更新Cron表达式
    updateCronFromRepeat() {
      // 使用简单模式的时间数据
      const hour = this.simpleData.hour;
      const minute = this.simpleData.minute;
      
      switch (Number(this.repeatIndex)) {
        case 0: // 不重复
          this.reminderData.cronExpression = '';
          this.simpleData.recurrenceType = 'NONE';
          break;
        case 1: // 每天
          this.reminderData.cronExpression = `${minute} ${hour} * * *`;
          this.simpleData.recurrenceType = 'DAILY';
          break;
        case 2: // 每周
          this.reminderData.cronExpression = `${minute} ${hour} * * 1`;
          this.simpleData.recurrenceType = 'WEEKLY';
          this.simpleData.weekday = 1; // 默认周一
          break;
        case 3: // 每月
          const dayOfMonth = this.simpleData.dayOfMonth || 1; // 使用已选择的日期，默认1号
          this.reminderData.cronExpression = `${minute} ${hour} ${dayOfMonth} * *`;
          this.simpleData.recurrenceType = 'MONTHLY';
          if (!this.simpleData.dayOfMonth) {
            this.simpleData.dayOfMonth = 1; // 只有在没有设置时才默认为1号
          }
          break;
      }
      
      // 更新预览
      this.updatePreview();
    },
    
    // 解析Cron表达式为文字描述
    parseCronToDescription(cronExpression) {
      if (!cronExpression || cronExpression.trim() === '') {
        return '请设置重复规则';
      }
      
      try {
        // 解析cron表达式 (格式: 秒 分 时 日 月 周 年)
        const parts = cronExpression.trim().split(/\s+/);
        if (parts.length < 6) {
          return '无效的Cron表达式';
        }
        
        const [second, minute, hour, day, month, weekday, year] = parts;
        
        let description = '';
        
        // 解析时间
        const timeStr = this.formatTime(hour, minute);
        
        // 解析重复模式
        if (weekday !== '?' && weekday !== '*') {
          // 按周重复
          const weekdays = this.parseWeekdays(weekday);
          description = `每${weekdays}${timeStr}`;
        } else if (day !== '?' && day !== '*') {
          // 按月重复
          if (day.includes(',')) {
            const days = day.split(',').join('日、');
            description = `每月${days}日${timeStr}`;
          } else {
            description = `每月${day}日${timeStr}`;
          }
        } else if (month !== '*') {
          // 按年重复
          const months = this.parseMonths(month);
          description = `每年${months}${timeStr}`;
        } else {
          // 每天重复
          description = `每天${timeStr}`;
        }
        
        return description;
      } catch (error) {
        console.error('解析Cron表达式失败:', error);
        return '无效的Cron表达式';
      }
    },
    
    // 格式化时间
    formatTime(hour, minute) {
      const h = hour === '*' ? '0' : hour;
      const m = minute === '*' ? '0' : minute;
      const hourNum = parseInt(h);
      const minuteNum = parseInt(m);
      
      if (hourNum < 12) {
        return `上午${hourNum}:${String(minuteNum).padStart(2, '0')}`;
      } else if (hourNum === 12) {
        return `中午${hourNum}:${String(minuteNum).padStart(2, '0')}`;
      } else {
        return `下午${hourNum - 12}:${String(minuteNum).padStart(2, '0')}`;
      }
    },
    
    // 解析星期
    parseWeekdays(weekday) {
      const weekMap = {
        '0': '周日', '7': '周日',
        '1': '周一', '2': '周二', '3': '周三', 
        '4': '周四', '5': '周五', '6': '周六',
        'SUN': '周日', 'MON': '周一', 'TUE': '周二', 
        'WED': '周三', 'THU': '周四', 'FRI': '周五', 'SAT': '周六'
      };
      
      if (weekday.includes(',')) {
        return weekday.split(',').map(w => weekMap[w.trim()] || w).join('、');
      } else {
        return weekMap[weekday] || weekday;
      }
    },
    
    // 解析月份
    parseMonths(month) {
      const monthMap = {
        '1': '1月', '2': '2月', '3': '3月', '4': '4月',
        '5': '5月', '6': '6月', '7': '7月', '8': '8月',
        '9': '9月', '10': '10月', '11': '11月', '12': '12月'
      };
      
      if (month.includes(',')) {
        return month.split(',').map(m => monthMap[m.trim()] || m).join('、');
      } else {
        return monthMap[month] || month;
      }
    },
    
    // 加载提醒数据（编辑模式）
    async loadReminderData(id) {
      try {
        const { getComplexReminderById } = require('../../services/api');
        const data = await getComplexReminderById(id);
        
        // 更新表单数据
        this.reminderData = {
          ...this.reminderData,
          ...data
        };
        
        // 解析Cron表达式到简易模式
        if (data.cronExpression) {
          this.parseCronToSimple(data.cronExpression);
        }
        
        console.log('加载复杂提醒数据成功:', data);
      } catch (error) {
        console.error('加载提醒数据失败:', error);
        uni.showToast({
          title: '加载数据失败',
          icon: 'error'
        });
      }
    },
    
    // 保存提醒
    saveReminder() {
      if (!this.validateForm()) {
        return;
      }
      
      // 显示确认弹窗
      uni.showModal({
        title: '确认保存',
        content: `确定要${this.isEdit ? '修改' : '创建'}这个复杂提醒吗？`,
        confirmText: '确定',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            this.handleConfirmSave();
          }
        }
      });
    },
    
    // 确认保存
    async handleConfirmSave() {
      this.isSubmitting = true;
      
      try {
        const { createComplexReminder, updateComplexReminder } = require('../../services/api');
        
        // 准备保存数据
        const saveData = {
          ...this.reminderData,
          timeMode: this.activeTab
        };
        
        console.log('保存复杂提醒:', saveData);
        
        let result;
        if (this.isEdit && this.reminderData.id) {
          result = await updateComplexReminder(this.reminderData.id, saveData);
        } else {
          result = await createComplexReminder(saveData);
        }
        
        console.log('保存成功:', result);
        
        // 显示成功提示
        uni.showToast({
          title: this.isEdit ? '修改成功' : '创建成功',
          icon: 'success',
          duration: 2000
        });
        
        setTimeout(() => {
          this.goBack();
        }, 1500);
        
      } catch (error) {
        console.error('保存失败:', error);
        
        // 显示错误弹窗
        uni.showModal({
          title: '保存失败',
          content: error.message || '未知错误，请重试',
          showCancel: false,
          confirmText: '知道了'
        });
      } finally {
        this.isSubmitting = false;
      }
    },
    
    // 表单验证
    validateForm() {
      if (!this.reminderData.title.trim()) {
        uni.showToast({
          title: '请输入提醒标题',
          icon: 'none',
          duration: 2000
        });
        return false;
      }
      
      if (!this.reminderData.cronExpression.trim()) {
        uni.showToast({
          title: 'Cron表达式不能为空',
          icon: 'none',
          duration: 2000
        });
        return false;
      }
      
      // 验证日期范围
      if (this.reminderData.validFrom && this.reminderData.validUntil) {
        const startDate = new Date(this.reminderData.validFrom);
        const endDate = new Date(this.reminderData.validUntil);
        if (endDate <= startDate) {
          // 使用更详细的错误弹窗
          uni.showModal({
            title: '日期范围错误',
            content: '结束日期必须晚于开始日期，请重新选择。',
            showCancel: false,
            confirmText: '知道了'
          });
          return false;
        }
      }
      
      return true;
    },
    
    // 显示Cron帮助信息
    showCronHelp() {
      uni.showModal({
        title: 'Cron表达式说明',
        content: '格式：分钟 小时 日期 月份 星期\n\n示例：\n0 8 * * * - 每天上匈8点\n30 14 * * 1 - 每周一下午2点30分\n0 9 1 * * - 每月第一天上匈9点\n0 10 25 12 * - 每年12月25日上午10点',
        showCancel: false,
        confirmText: '知道了'
      });
    },
    
    // 复制描述事件处理
    onCopyDescription(description) {
      console.log('描述已复制:', description);
    },
    
    // 复制时间表事件处理
    onCopyTimes(timeList) {
      console.log('时间表已复制:', timeList);
    },
    
    // 显示预览操作菜单（保留兼容性）
    showPreviewActions() {
      uni.showActionSheet({
        itemList: ['刷新预览', '复制描述', '导出时间表'],
        success: (res) => {
          switch (res.tapIndex) {
            case 0:
              this.updatePreview();
              uni.showToast({
                title: '预览已刷新',
                icon: 'success'
              });
              break;
            case 1:
              uni.setClipboardData({
                data: this.humanReadableDescription,
                success: () => {
                  uni.showToast({
                    title: '描述已复制',
                    icon: 'success'
                  });
                }
              });
              break;
            case 2:
              const timeList = this.previewTimes.join('\n');
              uni.setClipboardData({
                data: timeList,
                success: () => {
                  uni.showToast({
                    title: '时间表已复制',
                    icon: 'success'
                  });
                }
              });
              break;
          }
        }
      });
    },
    
    // 返回上一页
    goBack() {
      // 如果有未保存的数据，显示确认弹窗
      if (this.hasUnsavedChanges()) {
        uni.showModal({
          title: '提示',
          content: '您有未保存的修改，确定要离开吗？',
          confirmText: '离开',
          cancelText: '留下',
          confirmColor: '#ff4757',
          success: (res) => {
            if (res.confirm) {
              uni.navigateBack();
            }
          }
        });
      } else {
        uni.navigateBack();
      }
    },
    
    // 检查是否有未保存的修改
    hasUnsavedChanges() {
      // 简单检查标题是否为空或者有内容
      return this.reminderData.title.trim() !== '' || this.reminderData.description.trim() !== '';
    },
    
    // 重复类型改变
    onRecurrenceChange(e) {
      this.recurrenceIndex = e.detail.value;
      this.simpleData.recurrenceType = this.recurrenceValues[this.recurrenceIndex];
      this.updateCronFromSimple();
      this.updatePreview();
    },
    

    

    
    // 每月第几天改变
    onMonthDayChange(e) {
      this.monthDayIndex = e.detail.value;
      this.simpleData.dayOfMonth = this.monthDayIndex + 1;
      this.updateCronFromSimple();
      this.updatePreview();
    },
    
    // 月份改变
    onMonthChange(e) {
      this.monthIndex = e.detail.value;
      this.simpleData.month = this.monthIndex + 1;
      this.updateCronFromSimple();
      this.updatePreview();
    },
    
    // 日期改变
    onDayChange(e) {
      this.dayIndex = e.detail.value;
      this.simpleData.dayOfMonth = this.dayIndex + 1;
      this.updateCronFromSimple();
      this.updatePreview();
    },
    
    // 生效日期改变
    onValidFromChange(e) {
      this.reminderData.validFrom = e.detail.value;
      this.updatePreview();
    },
    
    // 失效日期改变
    onValidUntilChange(e) {
      this.reminderData.validUntil = e.detail.value;
      this.updatePreview();
    },
    
    // 生成月份天数选项
    generateMonthDays() {
      this.monthDays = [];
      for (let i = 1; i <= 31; i++) {
        this.monthDays.push(i + '日');
      }
    },
    
    // 获取指定月份的天数
    getDaysInMonth() {
      const month = this.simpleData.month || 1;
      const year = new Date().getFullYear();
      const daysInMonth = new Date(year, month, 0).getDate();
      const days = [];
      for (let i = 1; i <= daysInMonth; i++) {
        days.push(i + '日');
      }
      return days;
    },
    
    // 从简易模式更新Cron表达式
    updateCronFromSimple() {
      const { recurrenceType, hour, minute, weekday, dayOfMonth, month } = this.simpleData;
      
      switch (recurrenceType) {
        case 'DAILY':
          this.reminderData.cronExpression = `${minute} ${hour} * * *`;
          break;
        case 'WEEKLY':
          this.reminderData.cronExpression = `${minute} ${hour} * * ${weekday}`;
          break;
        case 'MONTHLY':
          this.reminderData.cronExpression = `${minute} ${hour} ${dayOfMonth} * *`;
          break;
        case 'YEARLY':
          this.reminderData.cronExpression = `${minute} ${hour} ${dayOfMonth} ${month} *`;
          break;
      }
      
      console.log('更新Cron表达式:', this.reminderData.cronExpression);
    },
    
    // 更新预览
    updatePreview() {
      this.updateHumanReadableDescription();
      this.generatePreviewTimes();
    },
    
    // 更新人类可读描述
    updateHumanReadableDescription() {
      if (this.activeTab === 'simple') {
        const { recurrenceType, hour, minute, weekday, dayOfMonth, month } = this.simpleData;
        const timeStr = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
        
        switch (recurrenceType) {
          case 'NONE':
            this.humanReadableDescription = `单次提醒 ${timeStr}`;
            break;
          case 'DAILY':
            this.humanReadableDescription = `每天 ${timeStr}`;
            break;
          case 'WEEKLY':
            this.humanReadableDescription = `每${this.weekDays[weekday]} ${timeStr}`;
            break;
          case 'MONTHLY':
            // 检查是否有月份不存在该日期
            const hasInvalidMonths = this.checkInvalidMonthsForDay(dayOfMonth);
            if (hasInvalidMonths.length > 0) {
              this.humanReadableDescription = `每月${dayOfMonth}日 ${timeStr} (${hasInvalidMonths.join('、')}月将使用月末)`;
            } else {
              this.humanReadableDescription = `每月${dayOfMonth}日 ${timeStr}`;
            }
            break;
          case 'YEARLY':
            // 检查是否是2月29日（闰年问题）
            if (month === 2 && dayOfMonth === 29) {
              this.humanReadableDescription = `每年${this.months[month-1]}${dayOfMonth}日 ${timeStr} (非闰年将使用2月28日)`;
            } else {
              this.humanReadableDescription = `每年${this.months[month-1]}${dayOfMonth}日 ${timeStr}`;
            }
            break;
          default:
            this.humanReadableDescription = `单次提醒 ${timeStr}`;
            break;
        }
      } else {
        // 高级模式，解析Cron表达式
        try {
          // 这里可以使用cronstrue库来解析
          this.humanReadableDescription = `Cron表达式: ${this.reminderData.cronExpression}`;
        } catch (error) {
          this.humanReadableDescription = '无效的Cron表达式';
        }
      }
    },
    
    // 生成预览时间
    generatePreviewTimes() {
      this.previewTimes = [];
      
      try {
        const now = new Date();
        const startDate = this.reminderData.validFrom ? new Date(this.reminderData.validFrom) : now;
        const endDate = this.reminderData.validUntil ? new Date(this.reminderData.validUntil) : null;
        const maxExecutions = this.reminderData.maxExecutions || 10;
        
        // 从当前时间开始查找
        let currentDate = new Date(now.getTime());
        const generatedTimes = [];
        
        console.log('开始生成预览时间，当前时间:', this.formatDateTime(now));
        console.log('简单模式数据:', this.simpleData);
        
        // 根据重复类型生成时间
        for (let i = 0; i < Math.min(maxExecutions, 10); i++) {
          const targetDate = this.getNextTriggerTime(currentDate);
          
          console.log(`第${i+1}次查找，从时间:`, this.formatDateTime(currentDate), '找到:', targetDate ? this.formatDateTime(targetDate) : 'null');
          
          if (!targetDate) break;
          
          if (endDate && targetDate > endDate) break;
          
          generatedTimes.push(this.formatDateTime(targetDate));
          
          // 移动到下一个周期的起始点
          if (this.simpleData.recurrenceType === 'DAILY') {
            // 每天重复：移动到下一天的0点
            currentDate = new Date(targetDate.getTime());
            currentDate.setDate(currentDate.getDate() + 1);
            currentDate.setHours(0, 0, 0, 0);
          } else {
            // 其他重复类型：移动到目标时间后1分钟
            currentDate = new Date(targetDate.getTime() + 60 * 1000);
          }
        }
        
        this.previewTimes = generatedTimes;
        console.log('生成的预览时间:', generatedTimes);
      } catch (error) {
        console.error('生成预览时间出错:', error);
        this.previewTimes = ['生成预览时出错'];
      }
    },
    
    // 获取下次触发时间
    getNextTriggerTime(fromDate) {
      const { recurrenceType, hour, minute, weekday, dayOfMonth, month } = this.simpleData;
      
      // 创建目标时间，从fromDate的日期开始，设置为指定的小时和分钟
      let targetDate = new Date(fromDate);
      targetDate.setHours(hour || 0, minute || 0, 0, 0);
      
      switch (recurrenceType) {
        case 'NONE':
          // 单次提醒，如果时间已过，则使用设定的日期时间
          if (this.simpleDate && this.simpleTime) {
            const specificDate = new Date(`${this.simpleDate}T${this.simpleTime}:00`);
            if (specificDate > fromDate) {
              return specificDate;
            }
          }
          // 如果没有设定具体日期或时间已过，返回null
          return null;
          
        case 'DAILY':
          // 如果目标时间小于等于当前时间，移动到下一天
          if (targetDate <= fromDate) {
            targetDate.setDate(targetDate.getDate() + 1);
            targetDate.setHours(hour || 0, minute || 0, 0, 0);
          }
          console.log('DAILY计算结果:', this.formatDateTime(targetDate), '从时间:', this.formatDateTime(fromDate));
          break;
          
        case 'WEEKLY':
          const currentWeekday = targetDate.getDay();
          let daysToAdd = weekday - currentWeekday;
          if (daysToAdd <= 0 || (daysToAdd === 0 && targetDate <= fromDate)) {
            daysToAdd += 7;
          }
          targetDate.setDate(targetDate.getDate() + daysToAdd);
          break;
          
        case 'MONTHLY':
          // 处理月份日期不存在的情况（如2月30日、6月31日等）
          const setValidMonthlyDate = (date, targetDay) => {
            const year = date.getFullYear();
            const month = date.getMonth();
            const daysInMonth = new Date(year, month + 1, 0).getDate();
            
            // 如果目标日期超过当月最大天数，使用当月最后一天
            const validDay = Math.min(targetDay, daysInMonth);
            date.setDate(validDay);
            return validDay;
          };
          
          let actualDay = setValidMonthlyDate(targetDate, dayOfMonth);
          
          if (targetDate <= fromDate) {
            // 移动到下个月
            targetDate.setMonth(targetDate.getMonth() + 1);
            actualDay = setValidMonthlyDate(targetDate, dayOfMonth);
          }
          
          console.log(`MONTHLY计算: 目标日期${dayOfMonth}日, 实际使用${actualDay}日`);
          break;
          
        case 'YEARLY':
          // 处理年度重复中的日期不存在情况（如闰年2月29日）
          const setValidYearlyDate = (date, targetMonth, targetDay) => {
            const year = date.getFullYear();
            const daysInMonth = new Date(year, targetMonth, 0).getDate();
            
            // 如果目标日期超过当月最大天数，使用当月最后一天
            const validDay = Math.min(targetDay, daysInMonth);
            date.setMonth(targetMonth - 1, validDay);
            return validDay;
          };
          
          let actualYearDay = setValidYearlyDate(targetDate, month, dayOfMonth);
          
          if (targetDate <= fromDate) {
            // 移动到下一年
            targetDate.setFullYear(targetDate.getFullYear() + 1);
            actualYearDay = setValidYearlyDate(targetDate, month, dayOfMonth);
          }
          
          console.log(`YEARLY计算: 目标${month}月${dayOfMonth}日, 实际使用${month}月${actualYearDay}日`);
          break;
          
        default:
          return null;
      }
      
      return targetDate;
    },
    
    // 格式化日期时间
    formatDateTime(date) {
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
      });
    },
    
    // 检查哪些月份不存在指定的日期
    checkInvalidMonthsForDay(day) {
      const invalidMonths = [];
      const currentYear = new Date().getFullYear();
      
      for (let month = 1; month <= 12; month++) {
        const daysInMonth = new Date(currentYear, month, 0).getDate();
        if (day > daysInMonth) {
          invalidMonths.push(month);
        }
      }
      
      return invalidMonths;
    },
    
    // 解析Cron表达式到简易模式
    parseCronToSimple(cronExpression) {
      try {
        const parts = cronExpression.split(' ');
        if (parts.length !== 5) return;
        
        const [minute, hour, day, month, weekday] = parts;
        
        this.simpleData.minute = parseInt(minute) || 0;
        this.simpleData.hour = parseInt(hour) || 0;
        this.simpleTime = `${String(this.simpleData.hour).padStart(2, '0')}:${String(this.simpleData.minute).padStart(2, '0')}`;
        
        // 设置默认日期为今天
        if (!this.simpleDate) {
          const today = new Date();
          this.simpleDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
        }
        
        // 判断重复类型
        if (day === '*' && month === '*' && weekday === '*') {
          // 每天
          this.simpleData.recurrenceType = 'DAILY';
          this.repeatIndex = 1; // 对应repeatOptions中的"每天"
        } else if (day === '*' && month === '*' && weekday !== '*') {
          // 每周
          this.simpleData.recurrenceType = 'WEEKLY';
          this.repeatIndex = 2; // 对应repeatOptions中的"每周"
          this.simpleData.weekday = parseInt(weekday) || 1;
        } else if (day !== '*' && month === '*' && weekday === '*') {
          // 每月
          this.simpleData.recurrenceType = 'MONTHLY';
          this.repeatIndex = 3; // 对应repeatOptions中的"每月"
          this.simpleData.dayOfMonth = parseInt(day) || 1;
        }
        console.log('解析Cron表达式成功:', this.simpleData);
      } catch (error) {
        console.error('解析Cron表达式失败:', error);
        this.activeTab = 'advanced';
      }
    },
    
    // 切换Cron表达式展开状态
    toggleCronExpression() {
      this.showCronExpression = !this.showCronExpression;
    },
    
    // 显示时间设置
    showTimeSettings() {
      console.log('点击了时间设置按钮');
      this.showCronPicker = true;
      console.log('showCronPicker设置为:', this.showCronPicker);
    },
    
    // Cron选择器确认
    onCronConfirm(cronExpression) {
      this.reminderData.cronExpression = cronExpression;
      this.showCronPicker = false;
      this.updatePreview();
      
      uni.showToast({
        title: '时间设置已更新',
        icon: 'success',
        duration: 1500
      });
    },
    
    // Cron选择器取消
    onCronCancel() {
      this.showCronPicker = false;
    },
    
    // 选择重复类型
    selectRecurrenceType(type, index) {
      this.simpleData.recurrenceType = type;
      this.recurrenceIndex = index;
      this.updateCronFromSimple();
      this.updatePreview();
    },
    
    // 显示提醒方式选择器
    showReminderTypeSelector() {
      uni.showActionSheet({
        itemList: this.reminderTypeOptions,
        success: (res) => {
          this.reminderTypeIndex = res.tapIndex;
          this.reminderData.reminderType = this.reminderTypeValues[res.tapIndex];
        }
      });
    },
    
    // 显示重复选择器
    showRepeatSelector() {
      uni.showActionSheet({
        itemList: this.repeatOptions,
        success: (res) => {
          this.repeatIndex = res.tapIndex;
          // 确保有时间数据后再更新Cron表达式
          if (this.simpleData.hour !== undefined && this.simpleData.minute !== undefined) {
            this.updateCronFromRepeat();
          } else {
            // 如果没有时间数据，使用默认时间
            this.simpleData.hour = 8;
            this.simpleData.minute = 0;
            this.updateCronFromRepeat();
          }
        }
      });
    },
    
    // 显示时间选择器
    showTimeSelector() {
      // 显示自定义日期时间选择器
      this.showCustomDateTime();
    },
    
    // 显示自定义日期时间选择
    showCustomDateTime() {
      this.showCustomPickers = true;
    },
    
    // 隐藏自定义选择器
    hideCustomPickers() {
      this.showCustomPickers = false;
    },
    
    // 确认自定义日期时间
    confirmCustomDateTime() {
      if (this.reminderDate && this.reminderTime) {
        this.updateEventTime();
        this.showCustomPickers = false;
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
    },
    
    // 格式化显示日期时间
    getFormattedDateTime() {
      if (!this.reminderDate || !this.reminderTime) {
        return '选择时间';
      }
      
      // 使用iOS兼容的日期格式创建Date对象
      const dateTimeStr = `${this.reminderDate}T${this.reminderTime}:00`;
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
  font-size: 48rpx;
  color: #1c170d;
  font-weight: 400;
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
}

.form-container {
  padding: 32rpx;
  max-width: 960rpx;
  margin: 0 auto;
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
  color: #1c170d;
  font-weight: 600;
  line-height: 1.4;
  flex: 1;
}

.setting-value {
  flex-shrink: 0;
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
</style>