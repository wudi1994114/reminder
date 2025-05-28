<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <view class="nav-icon">
          <text class="icon-arrow">✕</text>
        </view>
      </view>
      <view class="nav-title">{{ isEdit ? '编辑高级提醒' : '创建高级提醒' }}</view>
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
// 移除静态组件导入，改为按需加载
import { 
  createComplexReminder, 
  updateComplexReminder, 
  getComplexReminderById 
} from '../../services/api';
import { reminderState } from '../../services/store';
import pageBaseMixin from '@/mixins/page-base.js';

export default {
  // 使用页面基类混入，提供组件按需加载功能
  mixins: [pageBaseMixin],
  data() {
    return {
      isEdit: false,
      isSubmitting: false,
      activeTab: 'simple', // 'simple' 或 'advanced'
      showCronExpression: false, // 控制Cron表达式展开
      showCronPicker: false, // 控制Cron选择器显示
      isTextOverflow: false, // 控制跑马灯效果
      scrollDistance: '-50%', // 滚动距离
      scrollTop: 0, // 滚动位置
      
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
      const result = this.parseCronToDescription(this.reminderData.cronExpression);
      console.log('计算cronDescription:', this.reminderData.cronExpression, '->', result);
      return result;
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
    },
    
    // 监听cronDescription变化，触发文本溢出检测
    cronDescription(newDescription) {
      console.log('cronDescription变化:', newDescription);
      this.$nextTick(() => {
        this.checkTextOverflow();
      });
    }
  },
  
  // 页面加载完成后的回调（由混入的onLoad调用）
  async onPageLoad(options) {
    console.log('复杂提醒页面加载参数:', options);
    
    // 等待组件加载完成
    await this.waitForComponents();
    
    // 检查登录状态
    const token = uni.getStorageSync('accessToken');
    if (!token) {
      uni.showModal({
        title: '提示',
        content: '请先登录',
        showCancel: false,
        confirmText: '去登录',
        success: () => {
          uni.reLaunch({
            url: '/pages/login/login'
          });
        }
      });
      return;
    }
    
    if (options.id) {
      this.isEdit = true;
      this.loadReminderData(options.id);
    }
    this.initializeData();
    this.generateMonthDays();
    this.updatePreview();
    
    // 添加调试日志
    console.log('页面加载完成，初始cronExpression:', this.reminderData.cronExpression);
    console.log('初始cronDescription:', this.cronDescription);
    
    // 确保页面可以滚动
    this.$nextTick(() => {
      setTimeout(() => {
        this.scrollTop = 1; // 设置一个很小的滚动位置
      }, 200);
    });
  },
  
  mounted() {
    // 页面挂载后检测文本溢出
    this.checkTextOverflow();
    
    // 确保页面可以滚动，设置一个小的初始滚动位置
    this.$nextTick(() => {
      // 延迟一点时间确保DOM完全渲染
      setTimeout(() => {
        this.scrollTop = 1; // 设置一个很小的滚动位置，确保可以向上滚动
      }, 100);
    });
  },
  
  updated() {
    // 页面更新后检测文本溢出
    this.checkTextOverflow();
  },
  
  methods: {
    // 初始化数据
    initializeData() {
      const today = new Date();
      // 移除强制设置validFrom的默认值，让它保持为空字符串（对应后端的null）
      // this.reminderData.validFrom = today.toISOString().split('T')[0];
      
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
      
      // 确保切换后页面可以滚动
      this.$nextTick(() => {
        this.ensureScrollable();
      });
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
      console.log('解析Cron表达式:', cronExpression);
      
      if (!cronExpression || cronExpression.trim() === '') {
        console.log('Cron表达式为空');
        return '请设置重复规则';
      }
      
      try {
        // 解析cron表达式 (格式: 秒 分 时 日 月 周 年)
        const parts = cronExpression.trim().split(/\s+/);
        console.log('Cron表达式分割结果:', parts);
        
        if (parts.length < 5) {
          console.log('Cron表达式位数不足');
          return '无效的Cron表达式';
        }
        
        let second, minute, hour, day, month, weekday, year;
        
        if (parts.length === 5) {
          // 5位格式: 分 时 日 月 周
          [minute, hour, day, month, weekday] = parts;
          second = '0';
          year = '*';
          console.log('解析为5位格式');
        } else if (parts.length === 6) {
          // 6位格式: 秒 分 时 日 月 周
          [second, minute, hour, day, month, weekday] = parts;
          year = '*';
          console.log('解析为6位格式');
        } else {
          // 7位格式: 秒 分 时 日 月 周 年
          [second, minute, hour, day, month, weekday, year] = parts;
          console.log('解析为7位格式');
        }
        
        console.log('解析结果:', { second, minute, hour, day, month, weekday, year });
        
        let description = '';
        
        // 解析时间
        const timeStr = this.formatTime(hour, minute);
        console.log('格式化时间结果:', timeStr);
        
        // 解析重复模式 - 修复逻辑，正确处理"?"符号，优先检查年重复
        if (month !== '*' && month !== '?' && month.trim() !== '') {
          // 按年重复 - 优先检查年重复
          const months = this.parseMonths(month);
          
          if (weekday !== '?' && weekday !== '*' && weekday.trim() !== '') {
            // 年重复 + 星期模式
            const weekdays = this.parseWeekdays(weekday);
            description = `每年${months}的${weekdays}${timeStr}`;
          } else if (day !== '?' && day !== '*' && day.trim() !== '') {
            // 年重复 + 日期模式
            description = `每年${months}${day}日${timeStr}`;
          } else {
            // 年重复但日期和星期都为通配符
            description = `每年${months}${timeStr}`;
          }
          console.log('识别为年重复:', description);
        } else if (weekday !== '?' && weekday !== '*' && weekday.trim() !== '') {
          // 按周重复
          const weekdays = this.parseWeekdays(weekday);
          description = `每${weekdays}${timeStr}`;
          console.log('识别为周重复:', description);
        } else if (day !== '?' && day !== '*' && day.trim() !== '') {
          // 按月重复
          if (day.includes(',')) {
            const days = day.split(',').join('日、');
            description = `每月${days}日${timeStr}`;
          } else {
            description = `每月${day}日${timeStr}`;
          }
          console.log('识别为月重复:', description);
        } else {
          // 每天重复
          description = `每天${timeStr}`;
          console.log('识别为每天重复:', description);
        }
        
        console.log('最终描述:', description);
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
        return weekday.split(',').map(w => weekMap[w.trim()] || w).join(',');
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
        return month.split(',').map(m => monthMap[m.trim()] || m).join(',');
      } else {
        return monthMap[month] || month;
      }
    },
    
    // 加载提醒数据（编辑模式）
    async loadReminderData(id) {
      try {
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
        // 准备保存数据，清理空字符串为null
        const saveData = {
          ...this.reminderData,
          timeMode: this.activeTab,
          // 将空字符串转换为null，确保后端正确处理
          validFrom: this.reminderData.validFrom || null,
          validUntil: this.reminderData.validUntil || null,
          maxExecutions: this.reminderData.maxExecutions || null
        };
        
        console.log('保存复杂提醒:', saveData);
        
        let result;
        if (this.isEdit && this.reminderData.id) {
          result = await updateComplexReminder(this.reminderData.id, saveData);
          console.log('修改成功:', result);
          
          // 更新全局状态中的复杂提醒列表
          const index = reminderState.complexReminders.findIndex(item => item.id === this.reminderData.id);
          if (index !== -1) {
            reminderState.complexReminders[index] = result;
          }
          
          // 编辑成功后直接关闭页面
          uni.navigateBack();
        } else {
          result = await createComplexReminder(saveData);
          console.log('创建成功:', result);
          
          // 将新创建的复杂提醒添加到全局状态列表中
          if (result) {
            reminderState.complexReminders.push(result);
          }
          
          // 创建成功后直接关闭页面
          uni.navigateBack();
        }
        
      } catch (error) {
        console.error('保存失败:', error);
        
        // 更详细的错误处理
        let errorMessage = '未知错误，请重试';
        
        if (error && error.statusCode) {
          // HTTP错误
          if (error.statusCode === 401) {
            errorMessage = '请先登录';
          } else if (error.statusCode === 403) {
            errorMessage = '权限不足';
          } else if (error.statusCode === 400) {
            errorMessage = error.data?.message || '请求参数错误';
          } else if (error.statusCode === 500) {
            errorMessage = error.data?.message || '服务器内部错误';
          } else {
            errorMessage = `请求失败 (${error.statusCode})`;
          }
        } else if (error && error.message) {
          errorMessage = error.message;
        }
        
        // 显示错误弹窗
        uni.showModal({
          title: '保存失败',
          content: errorMessage,
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
        // 高级模式，使用cronDescription
        this.humanReadableDescription = this.cronDescription;
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
        console.log('当前模式:', this.activeTab);
        console.log('Cron表达式:', this.reminderData.cronExpression);
        
        // 根据模式选择不同的生成方式
        if (this.activeTab === 'advanced') {
          // 高级模式：直接从Cron表达式生成
          const cronData = this.parseCronExpressionForPreview(this.reminderData.cronExpression);
          if (cronData) {
            for (let i = 0; i < Math.min(maxExecutions, 10); i++) {
              const targetDate = this.getNextTriggerTimeFromCron(currentDate, cronData);
              if (!targetDate) break;
              
              if (endDate && targetDate > endDate) break;
              
              generatedTimes.push(this.formatDateTime(targetDate));
              
              // 移动到目标时间后1分钟
              currentDate = new Date(targetDate.getTime() + 60 * 1000);
            }
          }
        } else {
          // 简单模式：使用simpleData生成
          console.log('简单模式数据:', this.simpleData);
          for (let i = 0; i < Math.min(maxExecutions, 10); i++) {
            const targetDate = this.getNextTriggerTime(currentDate);
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
      console.log('接收到的cron表达式:', cronExpression);
      console.log('更新前的cronExpression:', this.reminderData.cronExpression);
      
      // 使用Vue.set确保响应式更新
      this.$set(this.reminderData, 'cronExpression', cronExpression);
      
      console.log('更新后的cronExpression:', this.reminderData.cronExpression);
      
      // 强制更新视图
      this.$forceUpdate();
      
      // 延迟计算以确保数据已更新
      this.$nextTick(() => {
        console.log('nextTick中计算的cronDescription:', this.cronDescription);
        // 再次强制更新确保显示正确
        this.$forceUpdate();
        // 检测文本溢出
        this.checkTextOverflow();
      });
      
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
    },
    
    // 检测文本是否溢出
    checkTextOverflow() {
      console.log('开始检测文本溢出，cronDescription:', this.cronDescription);
      this.$nextTick(() => {
        try {
          // 获取容器宽度
          uni.createSelectorQuery().in(this).select('.setting-value-container').boundingClientRect((containerRect) => {
            if (containerRect) {
              console.log('容器宽度:', containerRect.width);
              
              // 使用字符长度估算文本宽度（中文字符按1.2倍计算）
              const text = this.cronDescription;
              let estimatedWidth = 0;
              for (let i = 0; i < text.length; i++) {
                const char = text.charAt(i);
                // 中文字符宽度约16px，英文字符约8px（基于28rpx字体）
                if (/[\u4e00-\u9fa5]/.test(char)) {
                  estimatedWidth += 16; // 中文字符
                } else {
                  estimatedWidth += 8; // 英文字符和数字
                }
              }
              
              console.log('估算文本宽度:', estimatedWidth, 'px，容器宽度:', containerRect.width, 'px');
              
              // 如果估算宽度超过容器宽度，启用跑马灯
              if (estimatedWidth > containerRect.width - 20) { // 20px的误差范围
                // 计算滚动距离
                const overflowWidth = estimatedWidth - containerRect.width + 40; // 额外40px缓冲
                this.scrollDistance = `-${overflowWidth}px`;
                this.isTextOverflow = true;
                console.log('文本溢出，启用跑马灯，滚动距离:', this.scrollDistance);
              } else {
                this.isTextOverflow = false;
                console.log('文本未溢出，禁用跑马灯');
              }
            } else {
              console.log('无法获取容器尺寸信息');
              // 如果无法获取容器信息，基于文本长度简单判断
              if (this.cronDescription.length > 15) {
                this.isTextOverflow = true;
                this.scrollDistance = '-150px';
                console.log('基于文本长度判断溢出，启用跑马灯');
              }
            }
          }).exec();
        } catch (error) {
          console.error('检测文本溢出失败:', error);
          // 出错时，基于文本长度简单判断
          if (this.cronDescription.length > 15) {
            this.isTextOverflow = true;
            this.scrollDistance = '-150px';
            console.log('检测出错，基于文本长度判断溢出');
          }
        }
      });
    },
    
    // 解析Cron表达式用于预览生成
    parseCronExpressionForPreview(cronExpression) {
      if (!cronExpression || cronExpression.trim() === '') {
        return null;
      }
      
      try {
        const parts = cronExpression.trim().split(/\s+/);
        
        if (parts.length < 5) {
          return null;
        }
        
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
        } else {
          // 7位格式: 秒 分 时 日 月 周 年
          [second, minute, hour, day, month, weekday, year] = parts;
        }
        
        return {
          second: second,
          minute: minute,
          hour: hour,
          day: day,
          month: month,
          weekday: weekday,
          year: year
        };
      } catch (error) {
        console.error('解析Cron表达式失败:', error);
        return null;
      }
    },
    
    // 根据Cron表达式获取下次触发时间
    getNextTriggerTimeFromCron(fromDate, cronData) {
      if (!cronData) return null;
      
      const { minute, hour, day, month, weekday } = cronData;
      
      // 解析时间
      const targetMinute = minute === '*' ? fromDate.getMinutes() : parseInt(minute);
      const targetHour = hour === '*' ? fromDate.getHours() : parseInt(hour);
      
      // 创建候选时间
      let targetDate = new Date(fromDate);
      targetDate.setHours(targetHour, targetMinute, 0, 0);
      
      // 如果时间已过，移动到下一天
      if (targetDate <= fromDate) {
        targetDate.setDate(targetDate.getDate() + 1);
        targetDate.setHours(targetHour, targetMinute, 0, 0);
      }
      
      // 检查日期和星期约束
      const maxAttempts = 366; // 最多尝试一年
      let attempts = 0;
      
      while (attempts < maxAttempts) {
        const currentDay = targetDate.getDate();
        const currentMonth = targetDate.getMonth() + 1;
        const currentWeekday = targetDate.getDay();
        
        let dayMatch = true;
        let monthMatch = true;
        let weekdayMatch = true;
        
        // 检查日期约束
        if (day !== '*' && day !== '?') {
          if (day.includes(',')) {
            dayMatch = day.split(',').map(d => parseInt(d.trim())).includes(currentDay);
          } else {
            dayMatch = currentDay === parseInt(day);
          }
        }
        
        // 检查月份约束
        if (month !== '*' && month !== '?') {
          if (month.includes(',')) {
            monthMatch = month.split(',').map(m => parseInt(m.trim())).includes(currentMonth);
          } else {
            monthMatch = currentMonth === parseInt(month);
          }
        }
        
        // 检查星期约束
        if (weekday !== '*' && weekday !== '?') {
          let targetWeekdays = [];
          if (weekday.includes(',')) {
            targetWeekdays = weekday.split(',').map(w => {
              const wd = w.trim();
              return isNaN(wd) ? this.parseWeekdayString(wd) : parseInt(wd);
            });
          } else {
            const wd = weekday.trim();
            targetWeekdays = [isNaN(wd) ? this.parseWeekdayString(wd) : parseInt(wd)];
          }
          weekdayMatch = targetWeekdays.includes(currentWeekday);
        }
        
        // 如果所有约束都满足，返回这个时间
        if (dayMatch && monthMatch && weekdayMatch) {
          return targetDate;
        }
        
        // 移动到下一天
        targetDate.setDate(targetDate.getDate() + 1);
        targetDate.setHours(targetHour, targetMinute, 0, 0);
        attempts++;
      }
      
      return null; // 找不到合适的时间
    },
    
    // 解析星期字符串
    parseWeekdayString(weekdayStr) {
      const weekdayMap = {
        'SUN': 0, 'MON': 1, 'TUE': 2, 'WED': 3, 'THU': 4, 'FRI': 5, 'SAT': 6
      };
      return weekdayMap[weekdayStr.toUpperCase()] || 0;
    },
    
    // 确保页面可以滚动
    ensureScrollable() {
      // 设置一个很小的滚动位置，然后立即重置，这样可以激活滚动
      this.scrollTop = 1;
      this.$nextTick(() => {
        setTimeout(() => {
          this.scrollTop = 0;
        }, 50);
      });
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