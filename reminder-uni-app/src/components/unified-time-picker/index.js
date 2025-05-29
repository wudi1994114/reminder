/**
 * 统一时间组件导出
 */

import UnifiedTimePicker from './unified-time-picker.vue';
import TimePreview from './time-preview.vue';
import timeManager from '../../utils/timeManager';

// 导出组件
export { UnifiedTimePicker, TimePreview };

// 导出时间管理器
export { timeManager };

// 导出时间管理器的具体方法
export const {
  TimeType,
  UnifiedTimeData,
  generateCronExpression,
  generateDescription,
  generatePreviewTimes,
  detectTimeType,
  convertFromBackend,
  convertToBackend
} = timeManager;

// 默认导出
export default {
  UnifiedTimePicker,
  TimePreview,
  timeManager,
  TimeType,
  UnifiedTimeData
}; 