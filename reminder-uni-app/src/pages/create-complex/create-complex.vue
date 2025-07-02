<template>
  <view class="page-container">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <view class="nav-icon">
          <text class="icon-arrow">✕</text>
        </view>
      </view>
      <view class="nav-title"></view>
      <view class="nav-right"></view>
    </view>

    <scroll-view
        class="content-scroll"
        scroll-y
        :scroll-with-animation="true"
        :enable-back-to-top="true"
        :scroll-top="scrollTop"
        :enhanced="true"
        :bounces="true"
        :show-scrollbar="true"
    >
      <view class="form-container">
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
          <view class="textarea-container">
            <textarea
                class="content-textarea"
                v-model="reminderData.description"
                placeholder="内容"
                placeholder-class="input-placeholder"
                maxlength="200"
                :style="{ height: textareaHeight + 'px' }"
                @linechange="handleLineChange"
            />
            <view class="quick-tags-section">
              <view class="quick-tags">
                <view
                    v-for="tag in userTags"
                    :key="tag"
                    class="tag-item"
                    @click="addQuickTag(tag)"
                >
                  <text class="tag-text">{{ getTagTitle(tag) }}</text>
                </view>

                <view v-if="userTags.length === 0" class="tag-settings-hint" @click="goToTagSettings">
                  <text class="hint-text">设置标签</text>
                </view>
              </view>
              <VoiceInput
                @result="handleVoiceResult"
                @complete="handleVoiceComplete"
                @error="handleVoiceError"
                :showStatusText="false"
                :showResult="false"
              />
            </view>
          </view>
        </view>

        <view class="setting-item" @click="showReminderTypeSelector">
          <text class="setting-label">提醒方式</text>
          <text class="setting-value">{{ reminderTypeOptions[reminderTypeIndex] }}</text>
        </view>

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

        <view v-if="activeTab === 'simple'">
          <view class="setting-item" @click="showRepeatSelector">
            <text class="setting-label">重复</text>
            <text class="setting-value">{{ repeatOptions[repeatIndex] }}</text>
          </view>

          <view class="setting-item" @click="showSimpleTimeModal = true">
            <text class="setting-label">提醒时间</text>
            <text class="setting-value">{{ formatSimpleTime() }}</text>
          </view>

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

          <trigger-preview
              title="触发时间预览"
              :preview-times="formattedPreviewTimes"
              :description="humanReadableDescription"
              :max-display="3"
              :show-description="true"
              :show-action-button="false"
              :highlight-first="false"
              :show-index="true"
              @refresh="updatePreview"
          />
        </view>

        <view v-if="activeTab === 'advanced'">
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

          <trigger-preview
              title="触发时间预览"
              :preview-times="formattedPreviewTimes"
              :description="humanReadableDescription"
              :max-display="3"
              :show-description="true"
              :show-action-button="false"
              :highlight-first="false"
              :show-index="true"
              @refresh="updatePreview"
          />
        </view>

        <view class="extra-bottom-space"></view>
      </view>
    </scroll-view>

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

    <view class="bottom-actions" v-show="!showCronPicker && !showSimpleTimeModal && !showCustomPickers">
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

    <cron-expression-picker
        :show="showCronPicker"
        :initialValue="reminderData.cronExpression"
        @confirm="onCronConfirm"
        @cancel="onCronCancel"
        @update:show="showCronPicker = $event"
    />

    <time-picker-modal
        :show="showSimpleTimeModal"
        :initialTime="simpleTime"
        @confirm="onSimpleTimeConfirm"
        @cancel="showSimpleTimeModal = false"
        @update:show="showSimpleTimeModal = $event"
    />

  </view>
</template>

<script>
// 移除静态组件导入，改为按需加载
import {
  createComplexReminder,
  updateComplexReminder
} from '@/services/cachedApi';
import {
  getComplexReminderById,
  smartRequestSubscribe,
  getUserTagManagementEnabled,
  getUserTagList
} from '@/services/api';
import {
  generateComplexReminderIdempotencyKey,
  cacheIdempotencyKey,
  isIdempotencyKeyCached
} from '@/utils/idempotency';
import { reminderState } from '@/services/store';
import { DateFormatter } from '@/utils/dateFormat';
import { isProductionVersion, isDevelopmentVersion } from '@/config/version';
import { requireAuth } from '@/utils/auth';
import ReminderCacheService from '@/services/reminderCache';
import {
  generateDescription,
  detectTimeType,
  parseCronExpression,
  generatePreviewTimes,
  TimeType,
  UnifiedTimeData
} from '@/utils/timeManager';
import cronstrue from 'cronstrue/i18n';
import CronExpressionPicker from '@/components/CronExpressionPicker.vue';
import TriggerPreview from '@/components/trigger-preview/trigger-preview.vue';
import TimePickerModal from '@/components/TimePickerModal.vue';
import VoiceInput from '@/components/VoiceInput.vue';

export default {
  components: {
    CronExpressionPicker,
    TriggerPreview,
    TimePickerModal,
    VoiceInput
  },
  data() {
    return {
      isEdit: false,
      isSubmitting: false,
      activeTab: 'simple',
      showCronPicker: false,
      showSimpleTimeModal: false,
      isTextOverflow: false,
      scrollDistance: '-50%',
      scrollTop: 0,
      isInitialized: false, // 标记是否已初始化
      reminderId: null, // 保存提醒ID

      reminderTypeOptions: [],
      reminderTypeValues: [],
      reminderTypeIndex: 0,
      originalReminderType: null,

      repeatOptions: ['每日', '每周', '每月', '每年'],
      repeatValues: ['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY'],
      repeatIndex: 0,

      simpleDate: '',
      simpleTime: '',

      showCustomPickers: false,
      reminderDate: '',
      reminderTime: '',

      previewTimes: [],
      cronPreview: '',
      humanReadableDescription: '',

      // 用户标签
      userTags: [],
      tagManagementEnabled: false,

      // [ADDED] 新增用于手动控制 textarea 高度的 data 属性
      initialTextareaHeight: 0,
      textareaHeight: 0,
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
      if (!this.reminderData.cronExpression) {
        return '请选择';
      }
      return this.humanReadableDescription || '请选择';
    },
    formattedPreviewTimes() {
      if (!this.previewTimes || this.previewTimes.length === 0) {
        return [];
      }
      return this.previewTimes.map(p => p.formatted);
    }
  },

  async onLoad(options) {
    await requireAuth();

    // [ADDED] 在页面加载时，将 rpx 转换为 px，并设置为初始高度
    // 288 是你在 CSS 中原本设置的 min-height 值
    const initialHeightInPx = uni.upx2px(288);
    this.initialTextareaHeight = initialHeightInPx;
    this.textareaHeight = initialHeightInPx;

    this.isEdit = !!options.id;
    this.reminderId = options.id; // 保存ID供onShow使用

    // 先初始化提醒方式选项
    this.initReminderTypes();

    if (this.isEdit) {
      this.activeTab = 'advanced'; // 编辑模式默认进入高级模式
      await this.loadReminderData(options.id);
      // 加载数据后重新初始化提醒类型索引
      this.initReminderTypes();
    } else {
      this.resetAndSetupNewReminder();
    }
    
    await this.loadUserTags(); // 加载用户标签
    this.isInitialized = true; // 标记已初始化
  },

  onShow() {
    // 页面显示时刷新数据（从编辑页面返回时会触发）
    console.log('复杂任务页面: onShow 触发，准备刷新数据');
    if (this.isInitialized && this.isEdit && this.reminderId) {
      console.log('复杂任务页面: 开始刷新数据，ID:', this.reminderId);
      this.loadReminderData(this.reminderId);
    }
  },

  methods: {
    // [ADDED] 新增 linechange 事件处理函数
    handleLineChange(event) {
      // event.detail = { height, heightRpx, lineCount }
      const contentHeight = event.detail.height;

      // 核心逻辑：取 "初始高度" 和 "内容实际高度" 中的最大值
      this.textareaHeight = Math.max(this.initialTextareaHeight, contentHeight);
    },

    initReminderTypes() {
      let options = [];
      let values = [];

      if (isProductionVersion()) {
        // 正式环境: 只有邮件和手机
        options = ['邮件', '手机'];
        values = ['EMAIL', 'SMS'];
      } else {
        // 开发和测试环境: 只有微信
        options = ['微信'];
        values = ['WECHAT_MINI'];
      }

      this.reminderTypeOptions = options;
      this.reminderTypeValues = values;

      // 设置当前选中的索引
      if (this.reminderData.reminderType) {
        // 如果已经有提醒类型，使用现有的
        const existingIndex = this.reminderTypeValues.indexOf(this.reminderData.reminderType);
        this.reminderTypeIndex = existingIndex !== -1 ? existingIndex : 0;
        console.log('保持现有提醒类型索引:', this.reminderTypeIndex, '类型:', this.reminderData.reminderType);
      } else {
        // 如果没有提醒类型，设置为第一个选项
        this.reminderTypeIndex = 0;
        this.reminderData.reminderType = this.reminderTypeValues[0];
        console.log('设置默认提醒类型索引:', this.reminderTypeIndex, '类型:', this.reminderData.reminderType);
      }
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

      // 根据环境确定默认的提醒方式
      let defaultReminderType = '';
      if (isProductionVersion()) {
        // 正式环境默认邮件
        defaultReminderType = 'EMAIL';
      } else {
        // 开发和测试环境默认微信
        defaultReminderType = 'WECHAT_MINI';
      }

      reminderState.form = {
        title: '',
        description: '',
        cronExpression: '', // 高级模式默认为空
        reminderType: defaultReminderType, // 设置默认提醒方式，不再使用空字符串
        status: 'PENDING',
      };

      // 简单模式：设置默认时间和重复类型
      this.simpleTime = defaultTime;
      this.simpleDate = new Date().toISOString().split('T')[0];
      this.repeatIndex = 0; // 默认每日

      console.log('设置新提醒默认值:', {
        time: defaultTime,
        repeatType: this.repeatValues[this.repeatIndex],
        reminderType: defaultReminderType,
        simpleTime: this.simpleTime
      });

      // 简单模式自动计算Cron表达式
      this.$nextTick(() => {
        this.updateCronFromSimple();
      });
    },

    async loadReminderData(id) {
      try {
        const data = await getComplexReminderById(id);
        if (data) {
          console.log('加载复杂提醒数据:', data);
          
          // 设置表单数据
          reminderState.form = data;
          this.originalReminderType = data.reminderType;
          
          // 设置提醒类型索引
          const typeIndex = this.reminderTypeValues.indexOf(data.reminderType);
          this.reminderTypeIndex = typeIndex !== -1 ? typeIndex : 0;
          console.log('设置提醒类型索引:', this.reminderTypeIndex, '对应类型:', data.reminderType);
          
          // 从Cron表达式恢复简单模式的输入
          if (data.cronExpression) {
            this.updateSimpleInputsFromCron(data.cronExpression);
            this.updatePreview();
          }
          
          console.log('复杂提醒数据加载完成:', {
            title: data.title,
            description: data.description,
            cronExpression: data.cronExpression,
            reminderType: data.reminderType,
            reminderTypeIndex: this.reminderTypeIndex,
            simpleTime: this.simpleTime,
            repeatIndex: this.repeatIndex
          });
        }
      } catch (error) {
        console.error('加载复杂提醒失败:', error);
        uni.showToast({
          title: '加载数据失败',
          icon: 'none'
        });
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

      // 切换到简单模式时，如果没有Cron表达式，自动生成
      if (tab === 'simple' && !this.reminderData.cronExpression) {
        this.$nextTick(() => {
          this.updateCronFromSimple();
        });
      }
    },

    showRepeatSelector() {
      console.log('=== showRepeatSelector 开始 ===');
      console.log('当前 repeatIndex:', this.repeatIndex);
      console.log('当前 repeatValue:', this.repeatValues[this.repeatIndex]);

      uni.showActionSheet({
        itemList: this.repeatOptions,
        success: (res) => {
          console.log('=== ActionSheet 选择成功 ===');
          console.log('选择的索引:', res.tapIndex);
          console.log('更新 repeatIndex 从', this.repeatIndex, '到', res.tapIndex);
          this.repeatIndex = res.tapIndex;
          console.log('调用 updateCronFromSimple');
          this.updateCronFromSimple();
        },
        fail: (err) => {
          console.log('ActionSheet 选择失败:', err.errMsg);
        }
      });
      console.log('=== showRepeatSelector 结束 ===');
    },

    onSimpleTimeChange(timeData) {
      console.log('=== onSimpleTimeChange 开始 ===');
      console.log('时间数据:', timeData);
      console.log('原 simpleTime:', this.simpleTime);

      this.simpleTime = timeData;
      console.log('新 simpleTime:', this.simpleTime);
      console.log('调用 updateCronFromSimple');
      this.updateCronFromSimple();
      console.log('=== onSimpleTimeChange 结束 ===');
    },

    onSimpleTimeConfirm(timeString) {
      console.log('简单模式时间确认:', timeString);
      this.simpleTime = timeString;
      this.showSimpleTimeModal = false;
      this.updateCronFromSimple();
    },

    formatSimpleTime() {
      if (!this.simpleTime) {
        return '选择时间';
      }

      const [hour, minute] = this.simpleTime.split(':');
      const h = parseInt(hour);
      const m = parseInt(minute);

      // 转换为中文时间格式
      if (h < 12) {
        const displayHour = h === 0 ? 12 : h;
        return `上午${displayHour}:${String(m).padStart(2, '0')}`;
      } else {
        const displayHour = h === 12 ? 12 : h - 12;
        return `下午${displayHour}:${String(m).padStart(2, '0')}`;
      }
    },

    optimizeCronDescription(cronDesc) {
      if (!cronDesc) return cronDesc;

      try {
        // 解析原始 Cron 表达式来生成更简洁的中文描述
        const normalizedCron = this.normalizeCronExpression(this.reminderData.cronExpression);
        const parts = normalizedCron.split(/\s+/);

        if (parts.length >= 6) {
          const [seconds, minutes, hours, day, month, weekday] = parts;

          // 格式化时间
          const h = parseInt(hours);
          const m = parseInt(minutes);
          let timeStr = '';
          if (h < 12) {
            const displayHour = h === 0 ? 12 : h;
            timeStr = `上午${displayHour}:${String(m).padStart(2, '0')}`;
          } else {
            const displayHour = h === 12 ? 12 : h - 12;
            timeStr = `下午${displayHour}:${String(m).padStart(2, '0')}`;
          }

          // 检测模式并生成简洁描述
          if (month !== '*' && month !== '?' && day !== '*' && day !== '?') {
            // 每年特定月份特定日期：每m月d,d,d号HH:mm
            const months = month.split(',').map(m => parseInt(m)).join('、');
            const days = day.split(',').map(d => parseInt(d)).join('、');
            return `每${months}月${days}号${timeStr}`;
          } else if (month !== '*' && month !== '?' && weekday !== '*' && weekday !== '?') {
            // 每年特定月份特定星期：每m月周w,w,wHH:mm
            const months = month.split(',').map(m => parseInt(m)).join('、');
            const weekNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
            const weeks = weekday.split(',').map(w => {
              const cronDay = parseInt(w);
              // Cron: 1=周日, 2=周一, 3=周二, 4=周三, 5=周四, 6=周五, 7=周六
              // 正确转换：处理所有情况
              let dayIndex;
              if (cronDay === 1) {
                dayIndex = 0; // 周日
              } else if (cronDay === 7) {
                dayIndex = 6; // 周六
              } else {
                dayIndex = cronDay - 1; // 周一到周五
              }
              return weekNames[dayIndex];
            }).join('、');
            return `每${months}月${weeks}${timeStr}`;
          } else if (day !== '*' && day !== '?') {
            // 每月特定日期：每月d,d,d号HH:mm
            const days = day.split(',').map(d => parseInt(d)).join('、');
            return `每月${days}号${timeStr}`;
          } else if (weekday !== '*' && weekday !== '?') {
            // 每周特定星期：每周w,w,wHH:mm
            const weekNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
            const weeks = weekday.split(',').map(w => {
              const cronDay = parseInt(w);
              // Cron: 1=周日, 2=周一, 3=周二, 4=周三, 5=周四, 6=周五, 7=周六
              // 正确转换：处理所有情况
              let dayIndex;
              if (cronDay === 1) {
                dayIndex = 0; // 周日
              } else if (cronDay === 7) {
                dayIndex = 6; // 周六
              } else {
                dayIndex = cronDay - 1; // 周一到周五
              }
              return weekNames[dayIndex];
            }).join('、');
            return `每${weeks}${timeStr}`;
          } else if (month !== '*' && month !== '?') {
            // 每年特定月份：每m月HH:mm
            const months = month.split(',').map(m => parseInt(m)).join('、');
            return `每${months}月${timeStr}`;
          } else {
            // 每天：每日HH:mm
            return `每日${timeStr}`;
          }
        }
      } catch (e) {
        console.log('简洁描述生成失败，使用优化后的原描述:', e);
      }

      // 如果解析失败，使用优化后的原描述
      let optimized = cronDesc
          .replace(/在(\d{1,2}):(\d{2})/g, (match, hour, minute) => {
            const h = parseInt(hour);
            const m = parseInt(minute);
            if (h < 12) {
              const displayHour = h === 0 ? 12 : h;
              return `上午${displayHour}:${minute}`;
            } else {
              const displayHour = h === 12 ? 12 : h - 12;
              return `下午${displayHour}:${minute}`;
            }
          })
          .replace(/星期天/g, '周日')
          .replace(/星期一/g, '周一')
          .replace(/星期二/g, '周二')
          .replace(/星期三/g, '周三')
          .replace(/星期四/g, '周四')
          .replace(/星期五/g, '周五')
          .replace(/星期六/g, '周六')
          .replace(/仅星期/g, '每')
          .replace(/仅于/g, '在')
          .replace(/每天/g, '每日')
          .replace(/，/g, '、')
          .replace(/和/g, '、')
          .replace(/\s+/g, '')
          .replace(/、+/g, '、');

      return optimized;
    },

    updateCronFromSimple() {
      console.log('=== updateCronFromSimple 开始 ===');
      console.log('当前状态:');
      console.log('  repeatIndex:', this.repeatIndex);
      console.log('  repeatType:', this.repeatValues[this.repeatIndex]);
      console.log('  simpleTime:', this.simpleTime);

      // 验证 simpleTime 格式
      if (!this.simpleTime || typeof this.simpleTime !== 'string') {
        console.error('simpleTime 无效:', this.simpleTime);
        return;
      }

      const repeatType = this.repeatValues[this.repeatIndex];
      const timeParts = this.simpleTime.split(':');

      // 确保时间部分都存在且有效
      if (timeParts.length !== 2) {
        console.error('时间格式无效，应为 HH:MM 格式:', this.simpleTime);
        return;
      }

      const hours = timeParts[0];
      const minutes = timeParts[1];

      // 验证时间值
      if (!hours || !minutes || isNaN(hours) || isNaN(minutes)) {
        console.error('时间值无效:', { hours, minutes });
        return;
      }

      console.log('时间解析:');
      console.log('  hours:', hours);
      console.log('  minutes:', minutes);

      // 获取今天的日期信息
      const today = new Date();
      const todayWeekday = today.getDay(); // 0=周日, 1=周一, ..., 6=周六
      const todayDate = today.getDate(); // 1-31
      const todayMonth = today.getMonth() + 1; // 1-12

      console.log('今天的日期信息:');
      console.log('  星期:', todayWeekday, '(0=周日)');
      console.log('  日期:', todayDate);
      console.log('  月份:', todayMonth);

      let cron = '';

      switch (repeatType) {
        case 'DAILY':
          cron = `${minutes} ${hours} * * ?`;
          console.log('生成每日 cron:', cron);
          break;
        case 'WEEKLY':
          // cron星期几：1=周日, 2=周一, ... 7=周六
          // JavaScript星期几：0=周日, 1=周一, ... 6=周六
          const cronWeekday = todayWeekday === 0 ? 1 : todayWeekday + 1;
          cron = `${minutes} ${hours} ? * ${cronWeekday}`;
          console.log('生成每周 cron:', cron, '(今天是星期', todayWeekday, '-> cron星期', cronWeekday, ')');
          break;
        case 'MONTHLY':
          cron = `${minutes} ${hours} ${todayDate} * ?`;
          console.log('生成每月 cron:', cron, '(今天是', todayDate, '日)');
          break;
        case 'YEARLY':
          cron = `${minutes} ${hours} ${todayDate} ${todayMonth} ?`;
          console.log('生成每年 cron:', cron, '(今天是', todayMonth, '月', todayDate, '日)');
          break;
      }

      console.log('原 cronExpression:', this.reminderData.cronExpression);
      this.reminderData.cronExpression = cron;
      console.log('新 cronExpression:', this.reminderData.cronExpression);
      console.log('调用 updatePreview');
      this.updatePreview();
      console.log('=== updateCronFromSimple 结束 ===');
    },

    updateSimpleInputsFromCron(cron) {
      if (!cron) return;

      try {
        const parts = cron.trim().split(/\s+/);
        console.log('解析Cron表达式到简易模式，分割结果:', parts);

        let seconds, minutes, hours, dayOfMonth, month, dayOfWeek;

        if (parts.length === 5) {
          // 5位格式: 分 时 日 月 周 (主要支持格式)
          [minutes, hours, dayOfMonth, month, dayOfWeek] = parts;
        } else if (parts.length === 6) {
          // 6位格式: 秒 分 时 日 月 周 (向后兼容)
          [seconds, minutes, hours, dayOfMonth, month, dayOfWeek] = parts;
        } else {
          console.warn('不支持的Cron表达式格式，应为5位或6位，实际为', parts.length, '位:', cron);
          return;
        }

        // 设置时间，确保数值有效
        const hourNum = parseInt(hours) || 0;
        const minuteNum = parseInt(minutes) || 0;
        this.simpleTime = `${String(hourNum).padStart(2, '0')}:${String(minuteNum).padStart(2, '0')}`;

        // 判断重复类型
        if (dayOfWeek !== '?' && dayOfWeek !== '*') {
          // 每周模式
          this.repeatIndex = this.repeatValues.indexOf('WEEKLY');
          const cronWeekday = parseInt(dayOfWeek, 10);
          // 转换cron星期格式到我们的格式
        } else if (month !== '?' && month !== '*' && dayOfMonth !== '?' && dayOfMonth !== '*') {
          // 每年模式（同时指定了月份和日期）
          this.repeatIndex = this.repeatValues.indexOf('YEARLY');
        } else if (dayOfMonth !== '?' && dayOfMonth !== '*') {
          // 每月模式（指定了日期但月份为*）
          this.repeatIndex = this.repeatValues.indexOf('MONTHLY');
        } else {
          // 每日模式（默认）
          this.repeatIndex = this.repeatValues.indexOf('DAILY');
        }

        console.log('Cron表达式解析完成:', {
          time: this.simpleTime,
          repeatType: this.repeatValues[this.repeatIndex],
        });

      } catch (e) {
        console.error('解析Cron表达式到简易模式失败:', e);
        // 解析失败时使用默认值
        this.repeatIndex = this.repeatValues.indexOf('DAILY');
      }
    },

    updatePreview() {
      if (!this.reminderData.cronExpression) {
        this.previewTimes = [];
        this.humanReadableDescription = '';
        return;
      }

      try {
        // 规范化Cron表达式：5位自动补秒，6位直接使用
        const normalizedCron = this.normalizeCronExpression(this.reminderData.cronExpression);

        console.log('正在解析Cron表达式:', normalizedCron);

        // 使用cronstrue生成人性化描述，并优化为中文表达
        const cronDesc = cronstrue.toString(normalizedCron, { locale: 'zh_CN' });
        this.humanReadableDescription = this.optimizeCronDescription(cronDesc);

        // 使用cron-parser生成预览时间
        this.generatePreviewTimes(normalizedCron);

        console.log('Cron表达式解析成功:', this.humanReadableDescription);

      } catch (e) {
        console.error('Cron表达式解析失败:', e);
        console.error('当前表达式:', this.reminderData.cronExpression);

        this.humanReadableDescription = 'Cron表达式格式错误';
        this.previewTimes = [];
      }
    },

    normalizeCronExpression(cronExpression) {
      const cronParts = cronExpression.trim().split(/\s+/);

      if (cronParts.length === 5) {
        // 5位格式：分 时 日 月 周 -> 秒 分 时 日 月 周
        const normalized = '0 ' + cronExpression;
        console.log('5位格式转换为6位:', cronExpression, '=>', normalized);
        return normalized;
      } else if (cronParts.length === 6) {
        // 6位格式直接使用
        return cronExpression;
      } else {
        throw new Error(`不支持的Cron表达式格式，应为5位或6位，实际为${cronParts.length}位`);
      }
    },

    generatePreviewTimes(cronExpression) {
      // 统一使用cron-parser生成预览时间
      this.generateSimplePreviewTimes(cronExpression);
    },

    generateSimplePreviewTimes(cronExpression) {
      try {
        console.log('使用内部timeManager生成预览时间');
        this.previewTimes = [];

        // 创建timeData对象
        const timeType = detectTimeType(cronExpression);
        const timeData = UnifiedTimeData.fromCronExpression(cronExpression, timeType);

        console.log('timeData创建完成:', timeData);

        // 使用内部的generatePreviewTimes函数
        const previewList = generatePreviewTimes(timeData, 5);

        // 转换格式
        const previewTimes = previewList.map(formatted => ({
          time: null, // 内部函数已返回格式化的字符串
          formatted: formatted
        }));

        this.previewTimes = previewTimes;
        console.log('内部预览时间生成成功:', this.previewTimes);

      } catch (e) {
        console.error('内部预览时间生成失败:', e);
        this.previewTimes = [];
        this.humanReadableDescription = 'Cron表达式格式错误';
      }
    },


    formatPreviewTime(date) {
      try {
        // 使用项目内部的统一时间格式化工具，显示完整日期、星期和时间
        return DateFormatter.formatDetail(date);
      } catch (e) {
        console.error('格式化预览时间失败:', e);
        return DateFormatter.formatDateTime(date) || date.toLocaleString('zh-CN');
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
      if (!this.reminderData.reminderType || this.reminderData.reminderType === '') {
        // 如果提醒方式为空，设置默认值
        const defaultType = isProductionVersion() ? 'EMAIL' : 'WECHAT_MINI';
        this.reminderData.reminderType = defaultType;
        console.warn('提醒方式为空，已设置为默认值:', defaultType);
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
        
        // 添加调试日志，记录发送到后端的完整数据
        console.log('📤 准备保存复杂提醒，发送到后端的数据:', {
          title: dataToSave.title,
          description: dataToSave.description,
          cronExpression: dataToSave.cronExpression,
          reminderType: dataToSave.reminderType,
          reminderTypeType: typeof dataToSave.reminderType,
          reminderTypeLength: dataToSave.reminderType ? dataToSave.reminderType.length : 0,
          isReminderTypeEmpty: dataToSave.reminderType === '',
          fullData: JSON.stringify(dataToSave, null, 2)
        });
        
        let result;

        if (this.isEdit) {
          result = await updateComplexReminder(dataToSave.id, dataToSave);
        } else {
          // 创建新任务时生成幂等键
          // 首先尝试从ReminderCacheService获取用户信息
          let userId = 'anonymous';

          try {
            // 方法1: 从ReminderCacheService获取当前用户
            const currentUser = ReminderCacheService.getCurrentUser();
            if (currentUser && currentUser.id) {
              userId = currentUser.id;
              console.log('从ReminderCacheService获取用户ID:', userId);
            } else {
              // 方法2: 从缓存中获取用户信息
              const cachedData = uni.getStorageSync('user_profile_cache');
              if (cachedData) {
                const parsed = JSON.parse(cachedData);
                if (parsed.user && parsed.user.id) {
                  userId = parsed.user.id;
                  console.log('从缓存获取用户ID:', userId);
                }
              }
            }
          } catch (error) {
            console.warn('获取用户ID失败，使用anonymous:', error);
          }

          // 生成基于内容的幂等键
          const idempotencyKey = generateComplexReminderIdempotencyKey(userId, dataToSave);
          console.log('生成幂等键:', idempotencyKey, '用户ID:', userId);

          // 检查是否已经在本地缓存中（防止快速重复点击）
          if (isIdempotencyKeyCached(idempotencyKey)) {
            uni.showToast({
              title: '请勿重复提交',
              icon: 'none',
              duration: 2000
            });
            return;
          }

          // 缓存幂等键
          cacheIdempotencyKey(idempotencyKey);

          // 调用API创建任务
          result = await createComplexReminder(dataToSave, idempotencyKey);
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
    showCustomPickersModal() {
      // no-op
    },
    hideCustomPickers() {
      // no-op
    },
    confirmCustomDateTime() {
      // no-op
    },

    // 添加快捷标签到内容
    addQuickTag(tag) {
      // 解析标签：检查是否包含|分隔符
      let titlePart = tag;
      let contentPart = tag;
      
      if (tag.includes('|')) {
        const parts = tag.split('|', 2);
        titlePart = parts[0].trim();
        contentPart = parts[1].trim();
      }

      // 检测title是否为空，为空则将标签的标题部分给title
      if (!this.reminderData.title || this.reminderData.title.trim() === '') {
        this.reminderData.title = titlePart;
      }

      if (!this.reminderData.description) {
        this.reminderData.description = contentPart;
      } else {
        // 如果内容不为空，在末尾添加标签的内容部分
        const currentText = this.reminderData.description.trim();
        // 添加空格分隔符，允许多次添加相同标签
        this.reminderData.description = currentText + (currentText ? ' ' : '') + contentPart;
      }

      // 限制长度不超过200字符
      if (this.reminderData.description.length > 200) {
        this.reminderData.description = this.reminderData.description.substring(0, 200);
      }

      // 提供触觉反馈（仅在真机上，开发环境中禁用以避免开发者工具的屏幕放大效果）
      // #ifdef MP-WEIXIN
      if (!isDevelopmentVersion()) {
        uni.vibrateShort({
          fail: () => {
            // 忽略失败，不是所有设备都支持震动
          }
        });
      }
      // #endif
    },

    // 处理语音识别结果
    handleVoiceResult(resultData) {
      console.log('@@ ComplexCreate - VoiceLifecycle: 接收到语音识别中间结果', resultData);

      // 实时更新内容（中间结果）
      if (!resultData.isFinal && resultData.text) {
        // 可以选择是否显示中间结果
        console.log('@@ ComplexCreate - VoiceLifecycle: 中间文本:', resultData.text);
      }
    },

    // 处理语音识别完成
    handleVoiceComplete(finalText) {
      console.log('@@ ComplexCreate - VoiceLifecycle: 语音识别流程完成', finalText);

      if (finalText && finalText.trim()) {
        // 将识别结果添加到内容中
        if (!this.reminderData.description) {
          this.reminderData.description = finalText.trim();
        } else {
          // 如果内容不为空，在末尾添加识别结果
          const currentText = this.reminderData.description.trim();
          this.reminderData.description = currentText + (currentText ? ' ' : '') + finalText.trim();
        }

        // 限制长度不超过200字符
        if (this.reminderData.description.length > 200) {
          this.reminderData.description = this.reminderData.description.substring(0, 200);
        }

        // 显示成功提示
        uni.showToast({
          title: '语音识别成功',
          icon: 'success',
          duration: 1500
        });
      }
    },

    // 处理语音识别错误
    handleVoiceError(error) {
      console.error('@@ ComplexCreate - VoiceLifecycle: 语音识别流程出错', error);

      // 错误已经在VoiceInput组件中处理了，这里可以做额外的处理
      // 比如记录错误日志等
    },

    // 加载用户标签
    async loadUserTags() {
      try {
        // 检查标签管理是否启用
        try {
          const enabledResponse = await getUserTagManagementEnabled();
          this.tagManagementEnabled = enabledResponse.value === '1';
        } catch (error) {
          console.log('标签管理功能未启用');
          this.tagManagementEnabled = false;
          this.userTags = [];
          return;
        }

        // 如果启用了标签管理，获取标签列表
        if (this.tagManagementEnabled) {
          try {
            const tagsResponse = await getUserTagList();
            const tagListString = tagsResponse.value || '';
            console.log('🏷️ 复杂提醒页面 - 获取到的标签字符串:', tagListString);
            
            this.userTags = tagListString ? tagListString.split('|-|').filter(tag => tag.trim()) : [];
            console.log('🏷️ 复杂提醒页面 - 最终标签数组:', this.userTags);
          } catch (error) {
            console.log('获取标签列表失败，使用空列表');
            this.userTags = [];
          }
        } else {
          this.userTags = [];
        }

      } catch (error) {
        console.error('加载用户标签失败:', error);
        this.userTags = [];
      }
    },

    // 获取标签的标题部分用于显示
    getTagTitle(tag) {
      return tag.includes('|') ? tag.split('|')[0].trim() : tag;
    },

    // 跳转到标签设置页面
    goToTagSettings() {
      uni.navigateTo({
        url: '/pages/settings/notification'
      });
    },



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
  z-index: 1;
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
  font-size: 36rpx;
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
  font-size: 36rpx;
  color: #1c170d;
  font-weight: 400;
  line-height: 1.4;
  white-space: nowrap;
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

/* [MODIFIED] 从 .content-textarea 移除了 min-height */
/* [MODIFIED] 添加了 transition 使高度变化更平滑 */
.content-textarea {
  width: 100%;
  /* min-height: 288rpx; */ /* <-- 已移除，由 JS 控制 */
  padding: 32rpx;
  padding-bottom: 80rpx; /* 为底部标签区域留出空间 */
  background-color: transparent;
  border-radius: 24rpx;
  border: none;
  font-size: 32rpx;
  color: #1c170d;
  line-height: 1.4;
  resize: none;
  transition: height 0.1s ease-in-out;
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
  font-size: 36rpx;
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
  z-index: 9999;
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

/* 文本框容器 */
.textarea-container {
  position: relative;
  background-color: #f4efe7;
  border-radius: 24rpx;
  min-height: 120rpx;
  overflow: hidden;
}

/* 快捷标签和语音输入区域 */
.quick-tags-section {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: transparent;
  padding: 16rpx 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.quick-tags {
  display: flex;
  align-items: center;
  gap: 16rpx;
  flex: 1;
}

.tag-item {
  background-color: #f7bd4a;
  border-radius: 16rpx;
  padding: 6rpx 12rpx;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4rpx 8rpx rgba(0, 0, 0, 0.15);
}

.tag-item:active {
  background-color: #e6a73d;
  transform: scale(0.95) translateY(1rpx);
  box-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.1);
}

.tag-text {
  font-size: 22rpx;
  color: #1c170d;
  font-weight: 500;
}

.tag-settings-hint {
  background-color: #e9e0ce;
  border: 2rpx dashed #cccccc;
  border-radius: 16rpx;
  padding: 6rpx 12rpx;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tag-settings-hint:active {
  background-color: #f7bd4a;
  border-color: #f7bd4a;
}

.hint-text {
  font-size: 22rpx;
  color: #9d8148;
  font-weight: 500;
}

.voice-input-btn {
  width: 48rpx;
  height: 48rpx;
  background-color: #f7bd4a;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4rpx 8rpx rgba(0, 0, 0, 0.15);
}

.voice-input-btn:active {
  background-color: #e6a73d;
  transform: scale(0.95) translateY(1rpx);
  box-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.1);
}

.voice-icon {
  width: 100%;
  height: 100%;
}

/* 额外的底部间距，确保页面可以向上滚动 */
.extra-bottom-space {
  height: 200rpx; /* 额外的底部空间 */
  width: 100%;
}
</style>