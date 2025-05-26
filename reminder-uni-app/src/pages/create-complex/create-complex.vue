<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <text class="nav-icon">←</text>
        <text class="nav-text">返回</text>
      </view>
      <view class="nav-title">{{ isEdit ? '编辑复杂提醒' : '创建复杂提醒' }}</view>
      <view class="nav-right"></view>
    </view>
    
    <!-- 主要内容区域 -->
    <scroll-view class="content-scroll" scroll-y>
      <view class="form-container">
        <!-- 基本信息 -->
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
                v-model="reminderData.title" 
                placeholder="请输入提醒标题"
                placeholder-class="input-placeholder"
                maxlength="50"
              />
            </view>
          </view>
          
          <view class="input-group">
            <view class="input-label">
              <text class="label-text">描述</text>
            </view>
            <view class="textarea-wrapper">
              <textarea 
                class="form-textarea" 
                v-model="reminderData.description" 
                placeholder="请输入提醒描述（可选）"
                placeholder-class="input-placeholder"
                maxlength="200"
                auto-height
              />
            </view>
          </view>
          
          <view class="input-group">
            <view class="input-label">
              <text class="label-text">提醒方式</text>
            </view>
            <picker :range="reminderTypeOptions" :value="reminderTypeIndex" @change="onReminderTypeChange">
              <view class="picker-display">
                <text class="picker-icon">📧</text>
                <text class="picker-text">{{ reminderTypeOptions[reminderTypeIndex] }}</text>
                <text class="picker-arrow">›</text>
              </view>
            </picker>
          </view>
        </view>
        
        <!-- 时间设置模式选择 -->
        <view class="form-section">
          <view class="section-header">
            <text class="section-icon">⏰</text>
            <text class="section-title">时间设置</text>
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
          <view v-if="activeTab === 'simple'" class="tab-content">
            <view class="input-group">
              <view class="input-label">
                <text class="label-text">重复类型</text>
              </view>
              <picker :range="recurrenceOptions" :value="recurrenceIndex" @change="onRecurrenceChange">
                <view class="picker-display">
                  <text class="picker-icon">🔄</text>
                  <text class="picker-text">{{ recurrenceOptions[recurrenceIndex] }}</text>
                  <text class="picker-arrow">›</text>
                </view>
              </picker>
            </view>
            
            <view class="input-group">
              <view class="input-label">
                <text class="label-text">提醒时间</text>
              </view>
              <view class="time-picker-container">
                <picker mode="time" :value="simpleTime" @change="onSimpleTimeChange">
                  <view class="picker-display">
                    <text class="picker-icon">🕐</text>
                    <text class="picker-text">{{ simpleTime || '选择时间' }}</text>
                    <text class="picker-arrow">›</text>
                  </view>
                </picker>
              </view>
            </view>
            
            <!-- 每周特定日选择 -->
            <view v-if="simpleData.recurrenceType === 'WEEKLY'" class="input-group">
              <view class="input-label">
                <text class="label-text">星期几</text>
              </view>
              <picker :range="weekDays" :value="weekdayIndex" @change="onWeekdayChange">
                <view class="picker-display">
                  <text class="picker-icon">📅</text>
                  <text class="picker-text">{{ weekDays[weekdayIndex] }}</text>
                  <text class="picker-arrow">›</text>
                </view>
              </picker>
            </view>
            
            <!-- 每月特定日选择 -->
            <view v-if="simpleData.recurrenceType === 'MONTHLY'" class="input-group">
              <view class="input-label">
                <text class="label-text">每月第几天</text>
              </view>
              <picker :range="monthDays" :value="monthDayIndex" @change="onMonthDayChange">
                <view class="picker-display">
                  <text class="picker-icon">📅</text>
                  <text class="picker-text">{{ monthDays[monthDayIndex] }}</text>
                  <text class="picker-arrow">›</text>
                </view>
              </picker>
            </view>
            
            <!-- 每年特定日选择 -->
            <view v-if="simpleData.recurrenceType === 'YEARLY'" class="input-group">
              <view class="input-label">
                <text class="label-text">月份和日期</text>
              </view>
              <view class="year-date-container">
                <picker :range="months" :value="monthIndex" @change="onMonthChange">
                  <view class="picker-display half-width">
                    <text class="picker-icon">📅</text>
                    <text class="picker-text">{{ months[monthIndex] }}</text>
                    <text class="picker-arrow">›</text>
                  </view>
                </picker>
                <picker :range="getDaysInMonth()" :value="dayIndex" @change="onDayChange">
                  <view class="picker-display half-width">
                    <text class="picker-icon">📅</text>
                    <text class="picker-text">{{ getDaysInMonth()[dayIndex] }}</text>
                    <text class="picker-arrow">›</text>
                  </view>
                </picker>
              </view>
            </view>
          </view>
          
          <!-- 高级模式内容 -->
          <view v-if="activeTab === 'advanced'" class="tab-content">
            <view class="input-group">
              <view class="input-label">
                <text class="label-text">Cron表达式</text>
              </view>
              <view class="input-wrapper">
                <input 
                  class="form-input" 
                  v-model="reminderData.cronExpression" 
                  placeholder="请输入Cron表达式，如：0 8 * * *"
                  placeholder-class="input-placeholder"
                />
              </view>
              <view class="cron-help" @click="showCronHelp">
                <text class="help-text">格式：分钟 小时 日期 月份 星期 📝点击查看详细说明</text>
              </view>
            </view>
          </view>
        </view>
        
        <!-- 时间段设置 -->
        <view class="form-section">
          <view class="section-header">
            <text class="section-icon">📅</text>
            <text class="section-title">时间段设置</text>
          </view>
          
          <view class="input-group">
            <view class="input-label">
              <text class="label-text">生效日期</text>
            </view>
            <picker mode="date" :value="reminderData.validFrom" @change="onValidFromChange">
              <view class="picker-display">
                <text class="picker-icon">📅</text>
                <text class="picker-text">{{ reminderData.validFrom || '选择开始日期' }}</text>
                <text class="picker-arrow">›</text>
              </view>
            </picker>
          </view>
          
          <view class="input-group">
            <view class="input-label">
              <text class="label-text">失效日期（可选）</text>
            </view>
            <picker mode="date" :value="reminderData.validUntil" @change="onValidUntilChange">
              <view class="picker-display">
                <text class="picker-icon">📅</text>
                <text class="picker-text">{{ reminderData.validUntil || '选择结束日期' }}</text>
                <text class="picker-arrow">›</text>
              </view>
            </picker>
          </view>
          
          <view class="input-group">
            <view class="input-label">
              <text class="label-text">最多执行次数（可选）</text>
            </view>
            <view class="input-wrapper">
              <input 
                class="form-input" 
                v-model.number="reminderData.maxExecutions" 
                type="number"
                placeholder="不限制"
                placeholder-class="input-placeholder"
              />
            </view>
          </view>
        </view>
        
        <!-- 预览区域 -->
        <view class="form-section">
          <view class="section-header">
            <text class="section-icon">🔍</text>
            <text class="section-title">触发时间预览</text>
            <view class="preview-actions" @click="showPreviewActions">
              <text class="action-text">更多</text>
              <text class="action-icon">⋮</text>
            </view>
          </view>
          
          <view class="preview-content">
            <view class="preview-description">
              <text class="description-label">描述：</text>
              <text class="description-text">{{ humanReadableDescription }}</text>
            </view>
            
            <view class="preview-times">
              <text class="times-label">下次将在这些时间触发：</text>
              <view class="times-list">
                <view v-if="previewTimes.length === 0" class="no-preview">
                  <text class="no-preview-text">暂无预览时间</text>
                </view>
                <view v-else>
                  <view v-for="(time, index) in previewTimes.slice(0, 5)" :key="index" class="time-item">
                    <text class="time-text">{{ time }}</text>
                  </view>
                  <view v-if="previewTimes.length > 5" class="more-times">
                    <text class="more-text">…还有 {{ previewTimes.length - 5 }} 个时间</text>
                  </view>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>
    
    <!-- 底部操作按钮 -->
    <view class="bottom-actions">
      <button class="action-btn cancel-btn" @click="goBack">
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
export default {
  data() {
    return {
      isEdit: false,
      isSubmitting: false,
      activeTab: 'simple', // 'simple' 或 'advanced'
      
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
    },
    
    // 切换标签
    switchTab(tab) {
      this.activeTab = tab;
      console.log('切换到模式:', tab);
      
      if (tab === 'simple') {
        this.updateCronFromSimple();
      }
      
      this.updatePreview();
    },
    
    // 提醒方式改变
    onReminderTypeChange(e) {
      this.reminderTypeIndex = e.detail.value;
      this.reminderData.reminderType = this.reminderTypeValues[this.reminderTypeIndex];
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
    
    // 显示预览操作菜单
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
    
    // 简易时间改变
    onSimpleTimeChange(e) {
      this.simpleTime = e.detail.value;
      const [hour, minute] = e.detail.value.split(':');
      this.simpleData.hour = parseInt(hour);
      this.simpleData.minute = parseInt(minute);
      this.updateCronFromSimple();
      this.updatePreview();
    },
    
    // 星期几改变
    onWeekdayChange(e) {
      this.weekdayIndex = e.detail.value;
      this.simpleData.weekday = this.weekdayIndex;
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
          case 'DAILY':
            this.humanReadableDescription = `每天 ${timeStr}`;
            break;
          case 'WEEKLY':
            this.humanReadableDescription = `每${this.weekDays[weekday]} ${timeStr}`;
            break;
          case 'MONTHLY':
            this.humanReadableDescription = `每月${dayOfMonth}日 ${timeStr}`;
            break;
          case 'YEARLY':
            this.humanReadableDescription = `每年${this.months[month-1]}${dayOfMonth}日 ${timeStr}`;
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
        
        let currentDate = new Date(Math.max(startDate.getTime(), now.getTime()));
        const generatedTimes = [];
        
        // 根据重复类型生成时间
        for (let i = 0; i < Math.min(maxExecutions, 10); i++) {
          const targetDate = this.getNextTriggerTime(currentDate);
          
          if (!targetDate) break;
          
          if (endDate && targetDate > endDate) break;
          
          generatedTimes.push(this.formatDateTime(targetDate));
          
          // 移动到下一个周期
          currentDate = new Date(targetDate.getTime() + 24 * 60 * 60 * 1000);
        }
        
        this.previewTimes = generatedTimes;
      } catch (error) {
        console.error('生成预览时间出错:', error);
        this.previewTimes = ['生成预览时出错'];
      }
    },
    
    // 获取下次触发时间
    getNextTriggerTime(fromDate) {
      const { recurrenceType, hour, minute, weekday, dayOfMonth, month } = this.simpleData;
      
      let targetDate = new Date(fromDate);
      targetDate.setHours(hour, minute, 0, 0);
      
      switch (recurrenceType) {
        case 'DAILY':
          if (targetDate <= fromDate) {
            targetDate.setDate(targetDate.getDate() + 1);
          }
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
          targetDate.setDate(dayOfMonth);
          if (targetDate <= fromDate) {
            targetDate.setMonth(targetDate.getMonth() + 1);
            targetDate.setDate(dayOfMonth);
          }
          break;
          
        case 'YEARLY':
          targetDate.setMonth(month - 1, dayOfMonth);
          if (targetDate <= fromDate) {
            targetDate.setFullYear(targetDate.getFullYear() + 1);
          }
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
    
    // 解析Cron表达式到简易模式
    parseCronToSimple(cronExpression) {
      try {
        const parts = cronExpression.split(' ');
        if (parts.length !== 5) return;
        
        const [minute, hour, day, month, weekday] = parts;
        
        this.simpleData.minute = parseInt(minute) || 0;
        this.simpleData.hour = parseInt(hour) || 0;
        this.simpleTime = `${String(this.simpleData.hour).padStart(2, '0')}:${String(this.simpleData.minute).padStart(2, '0')}`;
        
        // 判断重复类型
        if (day === '*' && month === '*' && weekday === '*') {
          // 每天
          this.simpleData.recurrenceType = 'DAILY';
          this.recurrenceIndex = 0;
        } else if (day === '*' && month === '*' && weekday !== '*') {
          // 每周
          this.simpleData.recurrenceType = 'WEEKLY';
          this.recurrenceIndex = 1;
          this.simpleData.weekday = parseInt(weekday) || 0;
          this.weekdayIndex = this.simpleData.weekday;
        } else if (day !== '*' && month === '*' && weekday === '*') {
          // 每月
          this.simpleData.recurrenceType = 'MONTHLY';
          this.recurrenceIndex = 2;
          this.simpleData.dayOfMonth = parseInt(day) || 1;
          this.monthDayIndex = this.simpleData.dayOfMonth - 1;
        } else if (day !== '*' && month !== '*' && weekday === '*') {
          // 每年
          this.simpleData.recurrenceType = 'YEARLY';
          this.recurrenceIndex = 3;
          this.simpleData.dayOfMonth = parseInt(day) || 1;
          this.simpleData.month = parseInt(month) || 1;
          this.monthIndex = this.simpleData.month - 1;
          this.dayIndex = this.simpleData.dayOfMonth - 1;
        } else {
          // 复杂表达式，切换到高级模式
          this.activeTab = 'advanced';
        }
        
        console.log('解析Cron表达式成功:', this.simpleData);
      } catch (error) {
        console.error('解析Cron表达式失败:', error);
        this.activeTab = 'advanced';
      }
    }
  }
}
</script>

<style scoped>
.page-container {
  height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
}

/* 导航栏样式 */
.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 80rpx;
  padding: 0 24rpx;
  background-color: #ffffff;
  border-bottom: 1rpx solid #e5e5e5;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.nav-icon {
  font-size: 32rpx;
  color: #007aff;
}

.nav-text {
  font-size: 28rpx;
  color: #007aff;
}

.nav-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333333;
}

.nav-right {
  width: 120rpx;
}

/* 内容区域 */
.content-scroll {
  flex: 1;
  padding: 0 24rpx;
}

.form-container {
  padding: 24rpx 0;
}

/* 表单区块 */
.form-section {
  background-color: #ffffff;
  border-radius: 12rpx;
  margin-bottom: 24rpx;
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.section-icon {
  font-size: 32rpx;
}

.section-title {
  flex: 1;
  font-size: 28rpx;
  font-weight: 600;
  color: #333333;
}

.preview-actions {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 8rpx 12rpx;
  background-color: #f0f0f0;
  border-radius: 6rpx;
  cursor: pointer;
}

.preview-actions:active {
  background-color: #e0e0e0;
}

.action-text {
  font-size: 24rpx;
  color: #666666;
}

.action-icon {
  font-size: 28rpx;
  color: #666666;
}

/* 输入组 */
.input-group {
  padding: 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.input-group:last-child {
  border-bottom: none;
}

.input-label {
  display: flex;
  align-items: center;
  gap: 6rpx;
  margin-bottom: 12rpx;
}

.label-text {
  font-size: 26rpx;
  color: #333333;
  font-weight: 500;
}

.required-mark {
  color: #ff4757;
  font-size: 26rpx;
}

/* 输入框样式 */
.input-wrapper, .textarea-wrapper {
  background-color: #f8f9fa;
  border-radius: 8rpx;
  border: 1rpx solid #e9ecef;
}

.form-input, .form-textarea {
  width: 100%;
  padding: 20rpx;
  font-size: 26rpx;
  color: #333333;
  background-color: transparent;
  border: none;
}

.input-placeholder {
  color: #999999;
}

/* 选择器样式 */
.picker-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx;
  background-color: #f8f9fa;
  border-radius: 8rpx;
  border: 1rpx solid #e9ecef;
}

.picker-icon {
  font-size: 28rpx;
  margin-right: 12rpx;
}

.picker-text {
  flex: 1;
  font-size: 26rpx;
  color: #333333;
}

.picker-arrow {
  font-size: 28rpx;
  color: #999999;
}

/* 标签切换 */
.tab-container {
  padding: 0 24rpx 24rpx;
}

.tab-buttons {
  display: flex;
  background-color: #f0f0f0;
  border-radius: 8rpx;
  padding: 6rpx;
}

.tab-button {
  flex: 1;
  text-align: center;
  padding: 16rpx;
  border-radius: 6rpx;
  transition: all 0.3s ease;
}

.tab-button.active {
  background-color: #007aff;
}

.tab-text {
  font-size: 26rpx;
  color: #666666;
}

.tab-button.active .tab-text {
  color: #ffffff;
  font-weight: 600;
}

/* 底部按钮 */
.bottom-actions {
  display: flex;
  gap: 16rpx;
  padding: 24rpx;
  background-color: #ffffff;
  border-top: 1rpx solid #e5e5e5;
}

.action-btn {
  flex: 1;
  height: 76rpx;
  border-radius: 8rpx;
  font-size: 28rpx;
  font-weight: 600;
  border: none;
}

.cancel-btn {
  background-color: #f8f9fa;
  color: #666666;
}

.submit-btn {
  background-color: #007aff;
  color: #ffffff;
}

.submit-btn:disabled,
.btn-loading {
  background-color: #cccccc;
  color: #999999;
}

.btn-text {
  font-size: 28rpx;
}

/* 标签内容 */
.tab-content {
  padding: 0 24rpx 24rpx;
}

/* 时间选择器 */
.time-picker-container {
  width: 100%;
}

/* 年份日期选择器 */
.year-date-container {
  display: flex;
  gap: 12rpx;
}

.half-width {
  flex: 1;
}

/* Cron帮助信息 */
.cron-help {
  margin-top: 12rpx;
  padding: 12rpx;
  background-color: #f0f8ff;
  border-radius: 6rpx;
  border-left: 3rpx solid #007aff;
}

.help-text {
  font-size: 22rpx;
  color: #666666;
  line-height: 1.3;
}

/* 预览区域 */
.preview-content {
  padding: 24rpx;
}

.preview-description {
  margin-bottom: 20rpx;
  padding: 16rpx;
  background-color: #f8f9fa;
  border-radius: 8rpx;
  border: 1rpx solid #e9ecef;
}

.description-label {
  font-size: 24rpx;
  color: #666666;
  font-weight: 500;
}

.description-text {
  font-size: 24rpx;
  color: #333333;
  margin-left: 6rpx;
}

.preview-times {
  background-color: #f8f9fa;
  border-radius: 8rpx;
  border: 1rpx solid #e9ecef;
  padding: 16rpx;
}

.times-label {
  font-size: 24rpx;
  color: #666666;
  font-weight: 500;
  margin-bottom: 12rpx;
  display: block;
}

.times-list {
  margin-top: 12rpx;
}

.no-preview {
  text-align: center;
  padding: 24rpx;
}

.no-preview-text {
  font-size: 24rpx;
  color: #999999;
}

.time-item {
  padding: 12rpx 16rpx;
  margin-bottom: 6rpx;
  background-color: #ffffff;
  border-radius: 6rpx;
  border: 1rpx solid #e5e5e5;
}

.time-item:last-child {
  margin-bottom: 0;
}

.time-text {
  font-size: 24rpx;
  color: #333333;
}

.more-times {
  text-align: center;
  padding: 12rpx;
  margin-top: 6rpx;
}

.more-text {
  font-size: 22rpx;
  color: #999999;
  font-style: italic;
}

/* 响应式调整 */
@media (max-width: 750rpx) {
  .year-date-container {
    flex-direction: column;
    gap: 16rpx;
  }
  
  .half-width {
    flex: none;
    width: 100%;
  }
}
</style>