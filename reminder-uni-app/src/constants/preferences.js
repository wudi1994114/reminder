/**
 * 用户偏好设置常量定义
 * 与后端 UserPreferenceDto.PreferenceKeys 保持一致
 */

// 偏好设置键名常量
export const PREFERENCE_KEYS = {
  // 通知相关
  DEFAULT_REMINDER_TYPE: 'defaultReminderType',
  EMAIL_NOTIFICATION_ENABLED: 'emailNotificationEnabled',
  SMS_NOTIFICATION_ENABLED: 'smsNotificationEnabled',
  WECHAT_NOTIFICATION_ENABLED: 'wechatNotificationEnabled',
  
  // 提醒设置
  DEFAULT_ADVANCE_MINUTES: 'defaultAdvanceMinutes',
  SOUND_ENABLED: 'soundEnabled',
  VIBRATION_ENABLED: 'vibrationEnabled',
  
  // 界面设置
  THEME: 'theme',
  LANGUAGE: 'language',
  TIMEZONE: 'timezone',
  
  // 时间设置
  DAILY_SUMMARY_TIME: 'dailySummaryTime',
  DAILY_SUMMARY_ENABLED: 'dailySummaryEnabled',
  WEEKEND_NOTIFICATION_ENABLED: 'weekendNotificationEnabled',
  
  // 免打扰设置
  QUIET_HOURS_START: 'quietHoursStart',
  QUIET_HOURS_END: 'quietHoursEnd',
  QUIET_HOURS_ENABLED: 'quietHoursEnabled'
};

// 提醒类型选项
export const REMINDER_TYPE_OPTIONS = [
  { label: '邮件通知', value: 'EMAIL' },
  { label: '短信通知', value: 'SMS' },
  { label: '微信通知', value: 'WECHAT_MINI' }
];

// 主题选项
export const THEME_OPTIONS = [
  { label: '浅色主题', value: 'light' },
  { label: '深色主题', value: 'dark' }
];

// 语言选项
export const LANGUAGE_OPTIONS = [
  { label: '简体中文', value: 'zh-CN' },
  { label: 'English', value: 'en-US' }
];

// 时区选项
export const TIMEZONE_OPTIONS = [
  { label: '北京时间 (UTC+8)', value: 'Asia/Shanghai' },
  { label: '东京时间 (UTC+9)', value: 'Asia/Tokyo' },
  { label: '纽约时间 (UTC-5)', value: 'America/New_York' },
  { label: '伦敦时间 (UTC+0)', value: 'Europe/London' }
];

// 提前提醒时间选项（分钟）
export const ADVANCE_MINUTES_OPTIONS = [
  { label: '不提前', value: '0' },
  { label: '5分钟前', value: '5' },
  { label: '15分钟前', value: '15' },
  { label: '30分钟前', value: '30' },
  { label: '1小时前', value: '60' },
  { label: '2小时前', value: '120' },
  { label: '1天前', value: '1440' }
];

// 每日汇总时间选项
export const DAILY_SUMMARY_TIME_OPTIONS = [
  { label: '早上 6:00', value: '06:00' },
  { label: '早上 7:00', value: '07:00' },
  { label: '早上 8:00', value: '08:00' },
  { label: '早上 9:00', value: '09:00' },
  { label: '晚上 18:00', value: '18:00' },
  { label: '晚上 19:00', value: '19:00' },
  { label: '晚上 20:00', value: '20:00' },
  { label: '晚上 21:00', value: '21:00' }
];

// 免打扰时间选项
export const QUIET_HOURS_OPTIONS = [
  { label: '22:00', value: '22:00' },
  { label: '23:00', value: '23:00' },
  { label: '00:00', value: '00:00' },
  { label: '01:00', value: '01:00' },
  { label: '06:00', value: '06:00' },
  { label: '07:00', value: '07:00' },
  { label: '08:00', value: '08:00' },
  { label: '09:00', value: '09:00' }
];

// 默认偏好设置值
export const DEFAULT_PREFERENCES = {
  [PREFERENCE_KEYS.DEFAULT_REMINDER_TYPE]: 'EMAIL',
  [PREFERENCE_KEYS.EMAIL_NOTIFICATION_ENABLED]: 'true',
  [PREFERENCE_KEYS.SMS_NOTIFICATION_ENABLED]: 'false',
  [PREFERENCE_KEYS.WECHAT_NOTIFICATION_ENABLED]: 'false',
  [PREFERENCE_KEYS.DEFAULT_ADVANCE_MINUTES]: '15',
  [PREFERENCE_KEYS.SOUND_ENABLED]: 'true',
  [PREFERENCE_KEYS.VIBRATION_ENABLED]: 'true',
  [PREFERENCE_KEYS.THEME]: 'light',
  [PREFERENCE_KEYS.LANGUAGE]: 'zh-CN',
  [PREFERENCE_KEYS.TIMEZONE]: 'Asia/Shanghai',
  [PREFERENCE_KEYS.DAILY_SUMMARY_TIME]: '08:00',
  [PREFERENCE_KEYS.DAILY_SUMMARY_ENABLED]: 'false',
  [PREFERENCE_KEYS.WEEKEND_NOTIFICATION_ENABLED]: 'true',
  [PREFERENCE_KEYS.QUIET_HOURS_START]: '22:00',
  [PREFERENCE_KEYS.QUIET_HOURS_END]: '07:00',
  [PREFERENCE_KEYS.QUIET_HOURS_ENABLED]: 'false'
};

// 偏好设置分组配置
export const PREFERENCE_GROUPS = [
  {
    title: '通知设置',
    icon: '🔔',
    items: [
      {
        key: PREFERENCE_KEYS.DEFAULT_REMINDER_TYPE,
        label: '默认通知方式',
        type: 'select',
        options: REMINDER_TYPE_OPTIONS,
        description: '创建新提醒时的默认通知方式'
      },
      {
        key: PREFERENCE_KEYS.EMAIL_NOTIFICATION_ENABLED,
        label: '邮件通知',
        type: 'switch',
        description: '是否启用邮件通知'
      },
      {
        key: PREFERENCE_KEYS.SMS_NOTIFICATION_ENABLED,
        label: '短信通知',
        type: 'switch',
        description: '是否启用短信通知'
      },
      {
        key: PREFERENCE_KEYS.WECHAT_NOTIFICATION_ENABLED,
        label: '微信通知',
        type: 'switch',
        description: '是否启用微信小程序通知'
      }
    ]
  },
  {
    title: '提醒设置',
    icon: '⏰',
    items: [
      {
        key: PREFERENCE_KEYS.DEFAULT_ADVANCE_MINUTES,
        label: '默认提前提醒',
        type: 'select',
        options: ADVANCE_MINUTES_OPTIONS,
        description: '创建新提醒时的默认提前时间'
      },
      {
        key: PREFERENCE_KEYS.SOUND_ENABLED,
        label: '声音提醒',
        type: 'switch',
        description: '是否启用声音提醒'
      },
      {
        key: PREFERENCE_KEYS.VIBRATION_ENABLED,
        label: '震动提醒',
        type: 'switch',
        description: '是否启用震动提醒'
      },
      {
        key: PREFERENCE_KEYS.WEEKEND_NOTIFICATION_ENABLED,
        label: '周末提醒',
        type: 'switch',
        description: '周末是否接收提醒'
      }
    ]
  },
  {
    title: '界面设置',
    icon: '🎨',
    items: [
      {
        key: PREFERENCE_KEYS.THEME,
        label: '主题',
        type: 'select',
        options: THEME_OPTIONS,
        description: '选择应用主题'
      },
      {
        key: PREFERENCE_KEYS.LANGUAGE,
        label: '语言',
        type: 'select',
        options: LANGUAGE_OPTIONS,
        description: '选择应用语言'
      },
      {
        key: PREFERENCE_KEYS.TIMEZONE,
        label: '时区',
        type: 'select',
        options: TIMEZONE_OPTIONS,
        description: '选择时区设置'
      }
    ]
  },
  {
    title: '每日汇总',
    icon: '📊',
    items: [
      {
        key: PREFERENCE_KEYS.DAILY_SUMMARY_ENABLED,
        label: '启用每日汇总',
        type: 'switch',
        description: '是否启用每日提醒汇总邮件'
      },
      {
        key: PREFERENCE_KEYS.DAILY_SUMMARY_TIME,
        label: '汇总时间',
        type: 'select',
        options: DAILY_SUMMARY_TIME_OPTIONS,
        description: '每日汇总邮件发送时间',
        dependsOn: PREFERENCE_KEYS.DAILY_SUMMARY_ENABLED
      }
    ]
  },
  {
    title: '免打扰模式',
    icon: '🌙',
    items: [
      {
        key: PREFERENCE_KEYS.QUIET_HOURS_ENABLED,
        label: '启用免打扰',
        type: 'switch',
        description: '是否启用免打扰模式'
      },
      {
        key: PREFERENCE_KEYS.QUIET_HOURS_START,
        label: '开始时间',
        type: 'select',
        options: QUIET_HOURS_OPTIONS,
        description: '免打扰开始时间',
        dependsOn: PREFERENCE_KEYS.QUIET_HOURS_ENABLED
      },
      {
        key: PREFERENCE_KEYS.QUIET_HOURS_END,
        label: '结束时间',
        type: 'select',
        options: QUIET_HOURS_OPTIONS,
        description: '免打扰结束时间',
        dependsOn: PREFERENCE_KEYS.QUIET_HOURS_ENABLED
      }
    ]
  }
];

/**
 * 工具函数：将字符串值转换为布尔值
 * @param {string} value 字符串值
 * @returns {boolean} 布尔值
 */
export const stringToBoolean = (value) => {
  if (typeof value === 'boolean') return value;
  if (typeof value === 'string') {
    return value.toLowerCase() === 'true';
  }
  return false;
};

/**
 * 工具函数：将布尔值转换为字符串
 * @param {boolean} value 布尔值
 * @returns {string} 字符串值
 */
export const booleanToString = (value) => {
  return String(Boolean(value));
};

/**
 * 工具函数：获取选项的标签
 * @param {Array} options 选项数组
 * @param {string} value 值
 * @returns {string} 标签
 */
export const getOptionLabel = (options, value) => {
  const option = options.find(opt => opt.value === value);
  return option ? option.label : value;
};