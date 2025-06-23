<template>
  <view class="voice-input-container">
    <!-- 语音按钮 -->
    <view 
      class="voice-input-btn" 
      :class="{ 
        'recording': isRecording, 
        'processing': isProcessing,
        'disabled': isDisabled 
      }"
      @click="handleVoiceClick"
    >
      <image 
        v-if="!isRecording && !isProcessing" 
        class="voice-icon" 
        src="/static/images/voice.png" 
        mode="aspectFit"
      />
      <view v-else-if="isRecording" class="recording-animation">
        <view class="wave wave1"></view>
        <view class="wave wave2"></view>
        <view class="wave wave3"></view>
      </view>
      <view v-else class="processing-spinner"></view>
    </view>

    <!-- 录音状态提示 -->
    <view v-if="showStatusText" class="status-text">
      {{ statusText }}
    </view>

    <!-- 识别结果预览 -->
    <view v-if="showResult && currentResult" class="result-preview">
      <text class="result-text">{{ currentResult }}</text>
    </view>
  </view>
</template>

<script>
import { speechService, SPEECH_STATUS, SPEECH_ERROR_TYPES } from '@/services/speechService.js';
import { isDevelopmentVersion } from '@/config/version.js';

export default {
  name: 'VoiceInput',
  props: {
    // 是否显示状态文本
    showStatusText: {
      type: Boolean,
      default: true
    },
    // 是否显示识别结果预览
    showResult: {
      type: Boolean,
      default: true
    },
    // 最大录音时长（秒）
    maxDuration: {
      type: Number,
      default: 30 // 降低到30秒以减少文件大小
    },
    // 是否禁用
    disabled: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      isRecording: false,
      isProcessing: false,
      currentResult: '',
      statusText: '',
      recordingTimer: null,
      recordingDuration: 0
    };
  },
  computed: {
    isDisabled() {
      return this.disabled || this.isProcessing;
    }
  },
  mounted() {
    this.initSpeechService();
  },
  beforeDestroy() {
    this.cleanup();
  },
  methods: {
    /**
     * 初始化语音服务
     */
    initSpeechService() {
      speechService.setCallbacks({
        onStatusChange: this.handleStatusChange,
        onResult: this.handleResult,
        onError: this.handleError,
        onComplete: this.handleComplete
      });
    },

    /**
     * 处理语音按钮点击
     */
    async handleVoiceClick() {
      if (this.isDisabled) {
        return;
      }

      try {
        if (!this.isRecording) {
          await this.startRecording();
        } else {
          this.stopRecording();
        }
      } catch (error) {
        console.error('语音操作失败:', error);
        this.showErrorToast(error.message);
      }
    },

    /**
     * 开始录音
     */
    async startRecording() {
      try {
        console.log('🎤 开始录音...');
        
        // 检查权限提示
        this.statusText = '正在请求麦克风权限...';
        
        await speechService.startRecognition();
        
        // 开始录音计时
        this.startRecordingTimer();
        
        // 提供触觉反馈
        this.vibrate();
        
      } catch (error) {
        console.error('开始录音失败:', error);
        throw error;
      }
    },

    /**
     * 停止录音
     */
    stopRecording() {
      console.log('⏹️ 停止录音...');
      
      speechService.stopRecognition();
      this.stopRecordingTimer();
      
      // 提供触觉反馈
      this.vibrate();
    },

    /**
     * 开始录音计时
     */
    startRecordingTimer() {
      this.recordingDuration = 0;
      this.recordingTimer = setInterval(() => {
        this.recordingDuration++;
        this.statusText = `录音中... ${this.recordingDuration}s`;
        
        // 检查是否超过最大时长
        if (this.recordingDuration >= this.maxDuration) {
          this.stopRecording();
          this.showToast('录音时间已达上限');
        }
      }, 1000);
    },

    /**
     * 停止录音计时
     */
    stopRecordingTimer() {
      if (this.recordingTimer) {
        clearInterval(this.recordingTimer);
        this.recordingTimer = null;
      }
    },

    /**
     * 处理状态变化
     */
    handleStatusChange(status) {
      console.log('语音状态变化:', status);
      
      switch (status) {
        case SPEECH_STATUS.CONNECTING:
          this.isRecording = false;
          this.isProcessing = true;
          this.statusText = '连接中...';
          break;
          
        case SPEECH_STATUS.RECORDING:
          this.isRecording = true;
          this.isProcessing = false;
          this.statusText = '录音中...';
          break;
          
        case SPEECH_STATUS.PROCESSING:
          this.isRecording = false;
          this.isProcessing = true;
          this.statusText = '识别中...';
          this.stopRecordingTimer();
          break;
          
        case SPEECH_STATUS.COMPLETED:
          this.isRecording = false;
          this.isProcessing = false;
          this.statusText = '识别完成';
          this.stopRecordingTimer();
          break;
          
        case SPEECH_STATUS.ERROR:
          this.isRecording = false;
          this.isProcessing = false;
          this.statusText = '识别失败';
          this.stopRecordingTimer();
          break;
          
        default:
          this.isRecording = false;
          this.isProcessing = false;
          this.statusText = '';
          break;
      }
    },

    /**
     * 处理识别结果
     */
    handleResult(result, isFinal) {
      console.log('识别结果:', result, '是否最终结果:', isFinal);
      
      this.currentResult = result;
      
      // 触发结果事件
      this.$emit('result', {
        text: result,
        isFinal: isFinal
      });
    },

    /**
     * 处理识别完成
     */
    handleComplete(finalResult) {
      console.log('识别完成:', finalResult);
      
      this.statusText = '';
      
      // 触发完成事件
      this.$emit('complete', finalResult);
      
      // 清空结果预览
      setTimeout(() => {
        this.currentResult = '';
      }, 2000);
    },

    /**
     * 处理错误
     */
    handleError(error) {
      console.error('语音识别错误:', error);
      
      this.stopRecordingTimer();
      
      let errorMessage = '语音识别失败';
      
      switch (error.type) {
        case SPEECH_ERROR_TYPES.PERMISSION_DENIED:
          errorMessage = '请允许访问麦克风权限';
          break;
        case SPEECH_ERROR_TYPES.NETWORK_ERROR:
          if (error.message.includes('后端服务未配置')) {
            errorMessage = '语音服务暂未配置，请使用文字输入';
          } else if (error.message.includes('存储空间不足')) {
            errorMessage = '设备存储空间不足，请清理后重试';
          } else {
            errorMessage = '网络连接失败，请检查网络';
          }
          break;
        case SPEECH_ERROR_TYPES.TIMEOUT_ERROR:
          errorMessage = '识别超时，请重试';
          break;
        default:
          errorMessage = error.message || '语音识别失败';
          break;
      }
      
      this.showErrorToast(errorMessage);
      
      // 触发错误事件
      this.$emit('error', error);
    },

    /**
     * 显示提示
     */
    showToast(message) {
      uni.showToast({
        title: message,
        icon: 'none',
        duration: 2000
      });
    },

    /**
     * 显示错误提示
     */
    showErrorToast(message) {
      uni.showToast({
        title: message,
        icon: 'error',
        duration: 3000
      });
    },

    /**
     * 触觉反馈
     */
    vibrate() {
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

    /**
     * 清理资源
     */
    cleanup() {
      this.stopRecordingTimer();
      if (this.isRecording) {
        speechService.stopRecognition();
      }
    }
  }
};
</script>

<style lang="scss" scoped>
.voice-input-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
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
  position: relative;
  overflow: hidden;

  &:active {
    background-color: #e6a73d;
    transform: scale(0.95) translateY(1rpx);
    box-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.1);
  }

  &.recording {
    background-color: #ff4757;
    animation: pulse 1.5s infinite;
  }

  &.processing {
    background-color: #5352ed;
  }

  &.disabled {
    background-color: #cccccc;
    cursor: not-allowed;
    
    &:active {
      transform: none;
      background-color: #cccccc;
    }
  }
}

.voice-icon {
  width: 100%;
  height: 100%;
}

/* 录音动画 */
.recording-animation {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2rpx;
}

.wave {
  width: 4rpx;
  height: 16rpx;
  background-color: white;
  border-radius: 2rpx;
  animation: wave-animation 1.2s infinite ease-in-out;
}

.wave1 {
  animation-delay: 0s;
}

.wave2 {
  animation-delay: 0.2s;
}

.wave3 {
  animation-delay: 0.4s;
}

/* 处理中动画 */
.processing-spinner {
  width: 20rpx;
  height: 20rpx;
  border: 2rpx solid rgba(255, 255, 255, 0.3);
  border-top: 2rpx solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 状态文本 */
.status-text {
  font-size: 20rpx;
  color: #666666;
  text-align: center;
  min-height: 28rpx;
}

/* 结果预览 */
.result-preview {
  max-width: 300rpx;
  padding: 8rpx 12rpx;
  background-color: #f8f9fa;
  border-radius: 8rpx;
  border: 1rpx solid #e9ecef;
}

.result-text {
  font-size: 24rpx;
  color: #333333;
  line-height: 1.4;
  word-break: break-all;
}

/* 动画定义 */
@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

@keyframes wave-animation {
  0%, 40%, 100% {
    transform: scaleY(0.4);
  }
  20% {
    transform: scaleY(1);
  }
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>
