/**
 * 智能语音识别服务
 * 根据环境自动选择合适的语音服务实现
 */

import { SPEECH_CONFIG, SPEECH_STATUS, SPEECH_ERROR_TYPES } from '@/config/speech.js';
import { wechatSpeechService } from './wechatSpeechService.js';



/**
 * 智能语音识别服务类
 * 根据环境自动选择最佳的语音服务实现
 */
export class SpeechRecognitionService {
  constructor() {
    this.activeService = null;
    this.status = SPEECH_STATUS.IDLE;
    this.callbacks = {
      onStatusChange: null,
      onResult: null,
      onError: null,
      onComplete: null
    };
    this.recognitionResult = '';
    this.isInitialized = false;
  }

  /**
   * 初始化服务
   */
  async init() {
    if (this.isInitialized) {
      return;
    }

    try {
      console.log('🚀 初始化智能语音识别服务...');

      // 选择合适的语音服务实现
      this.activeService = this.selectSpeechService();

      // 设置回调转发
      this.activeService.setCallbacks({
        onStatusChange: (status) => {
          this.status = status;
          this.triggerCallback('onStatusChange', status);
        },
        onResult: (result, isFinal) => {
          this.recognitionResult = result;
          this.triggerCallback('onResult', result, isFinal);
        },
        onError: (error) => {
          this.triggerCallback('onError', error);
        },
        onComplete: (result) => {
          this.recognitionResult = result;
          this.triggerCallback('onComplete', result);
        }
      });

      // 初始化选中的服务
      await this.activeService.init();

      this.isInitialized = true;
      console.log('✅ 智能语音识别服务初始化完成');

    } catch (error) {
      console.error('❌ 智能语音识别服务初始化失败:', error);
      this.handleError(SPEECH_ERROR_TYPES.UNKNOWN_ERROR, error.message);
      throw error;
    }
  }

  /**
   * 选择合适的语音服务实现
   */
  selectSpeechService() {
    // 检查是否为微信小程序环境
    // #ifdef MP-WEIXIN
    if (typeof wx !== 'undefined' && wx.getRecorderManager) {
      console.log('🎯 选择微信小程序语音服务');
      return wechatSpeechService;
    }
    // #endif

    // 检查是否支持浏览器语音API
    // #ifndef MP-WEIXIN
    if (this.checkBrowserSupport()) {
      console.log('🎯 选择浏览器语音服务');
      return this.createBrowserSpeechService();
    }
    // #endif

    throw new Error('当前环境不支持语音识别功能');
  }

  /**
   * 检查浏览器支持
   */
  checkBrowserSupport() {
    // 检查是否支持 MediaDevices API
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      console.warn('⚠️ 浏览器不支持 MediaDevices API');
      return false;
    }

    // 检查是否支持 WebSocket
    if (!window.WebSocket) {
      console.warn('⚠️ 浏览器不支持 WebSocket');
      return false;
    }

    return true;
  }

  /**
   * 创建浏览器语音服务（使用原有的SDK实现）
   */
  createBrowserSpeechService() {
    // 这里可以返回原有的基于SDK的实现
    // 为了简化，暂时返回一个简单的实现
    return {
      init: async () => {
        console.log('🌐 浏览器语音服务初始化');
      },
      setCallbacks: (callbacks) => {
        this.browserCallbacks = callbacks;
      },
      startRecognition: async () => {
        console.log('🎤 浏览器开始录音');
        // 这里可以实现基于SDK的录音逻辑
        this.browserCallbacks?.onError?.({
          type: SPEECH_ERROR_TYPES.UNKNOWN_ERROR,
          message: '浏览器语音服务暂未实现'
        });
      },
      stopRecognition: () => {
        console.log('⏹️ 浏览器停止录音');
      },
      destroy: () => {
        console.log('🗑️ 浏览器语音服务销毁');
      },
      getStatus: () => SPEECH_STATUS.IDLE,
      getResult: () => ''
    };
  }



  /**
   * 开始录音识别
   */
  async startRecognition() {
    try {
      if (!this.isInitialized) {
        await this.init();
      }

      if (this.status === SPEECH_STATUS.RECORDING) {
        console.warn('⚠️ 正在录音中，请先停止当前录音');
        return;
      }

      console.log('🚀 开始录音识别...');
      this.recognitionResult = '';

      // 委托给具体的服务实现
      await this.activeService.startRecognition();

    } catch (error) {
      console.error('❌ 开始录音失败:', error);
      this.handleError(SPEECH_ERROR_TYPES.PERMISSION_DENIED, error.message);
    }
  }

  /**
   * 停止录音识别
   */
  stopRecognition() {
    try {
      if (this.status !== SPEECH_STATUS.RECORDING) {
        console.warn('⚠️ 当前没有在录音');
        return;
      }

      console.log('⏹️ 停止录音识别...');

      // 委托给具体的服务实现
      if (this.activeService) {
        this.activeService.stopRecognition();
      }

    } catch (error) {
      console.error('❌ 停止录音失败:', error);
      this.handleError(SPEECH_ERROR_TYPES.UNKNOWN_ERROR, error.message);
    }
  }

  /**
   * 更新状态
   */
  updateStatus(newStatus) {
    if (this.status !== newStatus) {
      this.status = newStatus;
      this.triggerCallback('onStatusChange', newStatus);
    }
  }

  /**
   * 处理错误
   */
  handleError(errorType, message) {
    this.updateStatus(SPEECH_STATUS.ERROR);
    this.triggerCallback('onError', { type: errorType, message });
  }

  /**
   * 触发回调
   */
  triggerCallback(callbackName, ...args) {
    if (this.callbacks[callbackName] && typeof this.callbacks[callbackName] === 'function') {
      this.callbacks[callbackName](...args);
    }
  }

  /**
   * 设置回调函数
   */
  setCallbacks(callbacks) {
    this.callbacks = { ...this.callbacks, ...callbacks };

    // 如果已经有活跃服务，也设置其回调
    if (this.activeService) {
      this.activeService.setCallbacks({
        onStatusChange: (status) => {
          this.status = status;
          this.triggerCallback('onStatusChange', status);
        },
        onResult: (result, isFinal) => {
          this.recognitionResult = result;
          this.triggerCallback('onResult', result, isFinal);
        },
        onError: (error) => {
          this.triggerCallback('onError', error);
        },
        onComplete: (result) => {
          this.recognitionResult = result;
          this.triggerCallback('onComplete', result);
        }
      });
    }
  }

  /**
   * 销毁服务
   */
  destroy() {
    try {
      if (this.activeService) {
        this.activeService.destroy();
        this.activeService = null;
      }

      this.status = SPEECH_STATUS.IDLE;
      this.recognitionResult = '';
      this.isInitialized = false;

      console.log('🗑️ 智能语音识别服务已销毁');
    } catch (error) {
      console.error('❌ 销毁智能语音识别服务失败:', error);
    }
  }

  /**
   * 获取当前状态
   */
  getStatus() {
    return this.activeService ? this.activeService.getStatus() : this.status;
  }

  /**
   * 获取识别结果
   */
  getResult() {
    return this.activeService ? this.activeService.getResult() : this.recognitionResult;
  }
}

// 创建单例实例
export const speechService = new SpeechRecognitionService();

// 导出状态和错误类型常量
export { SPEECH_STATUS, SPEECH_ERROR_TYPES };
