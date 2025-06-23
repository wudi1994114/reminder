/**
 * 微信小程序语音识别服务
 * 基于 wx.getRecorderManager() 和腾讯云语音识别API
 */

import { SPEECH_STATUS, SPEECH_ERROR_TYPES, TENCENT_ASR_CONFIG } from '@/config/speech.js';
import { request } from '@/services/api.js';

// 腾讯云语音识别SDK导入
let AsrRealTime = null;
let sdkLoadError = null;

// 尝试加载腾讯云SDK
try {
  // #ifdef MP-WEIXIN
  // 在小程序环境中，直接require可能会有问题，改用动态加载
  try {
    AsrRealTime = require('tencentcloud-speech-sdk-js/dist/AsrRealTime');
  } catch (err) {
    try {
      // 尝试其他路径
      AsrRealTime = require('tencentcloud-speech-sdk-js/app/AsrRealTime');
    } catch (err2) {
      console.warn('⚠️ 腾讯云语音SDK在小程序环境下加载失败，将使用备用方案');
      sdkLoadError = err2;
    }
  }
  // #endif
  
  // #ifndef MP-WEIXIN
  // 在非小程序环境中使用动态import
  import('tencentcloud-speech-sdk-js/dist/AsrRealTime').then(module => {
    AsrRealTime = module.default || module;
  }).catch(error => {
    console.warn('⚠️ 腾讯云语音SDK加载失败:', error);
    sdkLoadError = error;
  });
  // #endif
} catch (error) {
  console.warn('⚠️ 腾讯云语音SDK初始化失败:', error);
  sdkLoadError = error;
}

/**
 * 微信小程序语音识别服务类
 */
export class WechatSpeechRecognitionService {
  constructor() {
    this.recorderManager = null;
    this.websocket = null;
    this.status = SPEECH_STATUS.IDLE;
    this.callbacks = {
      onStatusChange: null,
      onResult: null,
      onError: null,
      onComplete: null
    };
    this.recognitionResult = '';
    this.isInitialized = false;
    this.recordingStartTime = 0;
    this.maxRecordingTime = 60000; // 60秒
    this.stsCredentials = null; // 存储临时凭证
    
    // 腾讯云ASR相关属性
    this.asrClient = null; // 腾讯云实时语音识别客户端
    this.asrConnected = false; // ASR连接状态
    this.audioBuffer = []; // 音频数据缓冲区
    this.useBackupMode = false; // 是否使用备用模式（录音后上传）
  }

  /**
   * 初始化服务
   */
  async init() {
    if (this.isInitialized) {
      return;
    }

    try {
      console.log('🚀 初始化微信语音识别服务...');
      
      // 检查微信小程序环境
      if (!this.checkWechatSupport()) {
        throw new Error('当前环境不支持微信语音功能');
      }

      // 检查腾讯云SDK是否加载成功
      if (!AsrRealTime || sdkLoadError) {
        console.warn('⚠️ 腾讯云语音SDK未正确加载，将使用备用模式（录音后上传识别）');
        this.useBackupMode = true;
      }

      // 初始化录音管理器
      this.initRecorderManager();

      // 如果不是备用模式，尝试获取STS凭证和初始化ASR客户端
      if (!this.useBackupMode) {
        try {
          // 获取STS临时凭证
          await this.getStsCredentials();
          // 初始化腾讯云ASR客户端
          await this.initAsrClient();
        } catch (error) {
          console.warn('⚠️ 实时语音识别初始化失败，将使用备用模式:', error);
          this.useBackupMode = true;
        }
      }

      this.isInitialized = true;
      console.log('✅ 微信语音识别服务初始化完成', this.useBackupMode ? '(备用模式)' : '(实时模式)');
      
    } catch (error) {
      console.error('❌ 微信语音识别服务初始化失败:', error);
      this.handleError(SPEECH_ERROR_TYPES.UNKNOWN_ERROR, error.message);
      throw error;
    }
  }

  /**
   * 检查微信小程序支持
   */
  checkWechatSupport() {
    // #ifdef MP-WEIXIN
    if (typeof wx !== 'undefined' && wx.getRecorderManager) {
      return true;
    }
    // #endif
    
    console.warn('⚠️ 当前环境不支持微信录音API');
    return false;
  }

  /**
   * 初始化录音管理器
   */
  initRecorderManager() {
    // #ifdef MP-WEIXIN
    this.recorderManager = wx.getRecorderManager();
    
    // 录音开始事件
    this.recorderManager.onStart(() => {
      console.log('🎤 录音开始');
      this.recordingStartTime = Date.now();
      this.updateStatus(SPEECH_STATUS.RECORDING);
    });

    // 录音结束事件
    this.recorderManager.onStop((res) => {
      console.log('⏹️ 录音结束:', res);
      this.updateStatus(SPEECH_STATUS.PROCESSING);
      
      // 处理录音文件
      this.handleRecordingComplete(res);
    });

    // 录音错误事件
    this.recorderManager.onError((error) => {
      console.error('❌ 录音错误:', error);
      let errorMessage = '录音失败';

      if (error.errMsg.includes('storage limit')) {
        errorMessage = '存储空间不足，请清理后重试';
      } else if (error.errMsg.includes('permission')) {
        errorMessage = '录音权限被拒绝';
      } else if (error.errMsg) {
        errorMessage = '录音失败: ' + error.errMsg;
      }

      this.handleError(SPEECH_ERROR_TYPES.RECOGNITION_ERROR, errorMessage);
    });

    // 录音帧数据事件（实时音频数据）
    this.recorderManager.onFrameRecorded((res) => {
      // 只在实时模式下发送音频数据到腾讯云ASR
      if (!this.useBackupMode && this.asrConnected) {
        this.sendAudioDataToAsr(res.frameBuffer);
      }
    });
    // #endif
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
      this.updateStatus(SPEECH_STATUS.CONNECTING);
      this.recognitionResult = '';
      this.audioBuffer = [];

      // 检查录音权限
      await this.checkRecordPermission();

      // 如果不是备用模式，进行实时识别准备
      if (!this.useBackupMode) {
        try {
          // 检查并刷新STS凭证
          await this.checkAndRefreshCredentials();

          // 启动腾讯云ASR连接
          await this.startAsrConnection();
        } catch (error) {
          console.warn('⚠️ 实时识别模式启动失败，切换到备用模式:', error);
          this.useBackupMode = true;
        }
      }

      // 开始录音
      this.startRecording();
      
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
      this.updateStatus(SPEECH_STATUS.PROCESSING);
      
      // 停止录音
      // #ifdef MP-WEIXIN
      if (this.recorderManager) {
        this.recorderManager.stop();
      }
      // #endif

      // 停止腾讯云ASR连接
      this.stopAsrConnection();
      
    } catch (error) {
      console.error('❌ 停止录音失败:', error);
      this.handleError(SPEECH_ERROR_TYPES.UNKNOWN_ERROR, error.message);
    }
  }

  /**
   * 检查录音权限
   */
  async checkRecordPermission() {
    return new Promise((resolve, reject) => {
      // #ifdef MP-WEIXIN
      wx.getSetting({
        success: (res) => {
          if (res.authSetting['scope.record'] === false) {
            // 用户拒绝了录音权限，引导用户开启
            wx.showModal({
              title: '需要录音权限',
              content: '请在设置中开启录音权限以使用语音功能',
              confirmText: '去设置',
              success: (modalRes) => {
                if (modalRes.confirm) {
                  wx.openSetting({
                    success: (settingRes) => {
                      if (settingRes.authSetting['scope.record']) {
                        resolve();
                      } else {
                        reject(new Error('用户未开启录音权限'));
                      }
                    },
                    fail: () => {
                      reject(new Error('打开设置失败'));
                    }
                  });
                } else {
                  reject(new Error('用户拒绝开启录音权限'));
                }
              }
            });
          } else {
            // 权限已授予或未询问过
            resolve();
          }
        },
        fail: () => {
          reject(new Error('获取权限设置失败'));
        }
      });
      // #endif
      
      // #ifndef MP-WEIXIN
      reject(new Error('非微信小程序环境'));
      // #endif
    });
  }

  /**
   * 获取STS临时凭证
   */
  async getStsCredentials() {
    try {
      console.log('🔑 获取STS临时凭证...');

      const response = await request({
        url: '/api/sts/speech-credentials',
        method: 'GET'
      });

      this.stsCredentials = response;
      console.log('✅ STS临时凭证获取成功，过期时间:', new Date(response.expiredTime * 1000).toLocaleString());

      return response;
    } catch (error) {
      console.error('❌ 获取STS临时凭证失败:', error);
      throw new Error('获取语音服务凭证失败');
    }
  }

  /**
   * 初始化腾讯云ASR客户端
   */
  async initAsrClient() {
    try {
      console.log('🔧 初始化腾讯云ASR客户端...');
      
      // 检查AppId配置
      if (!TENCENT_ASR_CONFIG.appId) {
        console.warn('⚠️ 未配置腾讯云AppId，请在speech.js中配置');
      }

      // 创建ASR客户端配置
      const asrConfig = {
        appid: TENCENT_ASR_CONFIG.appId,
        secretid: this.stsCredentials.tmpSecretId,
        secretkey: this.stsCredentials.tmpSecretKey,
        token: this.stsCredentials.sessionToken,
        
        // 识别配置
        engine_model_type: TENCENT_ASR_CONFIG.engineModelType,
        voice_format: TENCENT_ASR_CONFIG.voiceFormat,
        filter_dirty: TENCENT_ASR_CONFIG.filterDirty,
        filter_modal: TENCENT_ASR_CONFIG.filterModal,
        filter_punc: TENCENT_ASR_CONFIG.filterPunc,
        convert_num_mode: TENCENT_ASR_CONFIG.convertNumMode,
      };

      // 创建ASR客户端实例
      this.asrClient = new AsrRealTime(asrConfig);
      
      console.log('✅ 腾讯云ASR客户端初始化完成');
      
    } catch (error) {
      console.error('❌ 腾讯云ASR客户端初始化失败:', error);
      throw error;
    }
  }

  /**
   * 检查并刷新STS凭证
   */
  async checkAndRefreshCredentials() {
    if (!this.stsCredentials) {
      await this.getStsCredentials();
      return;
    }

    // 检查凭证是否即将过期（提前5分钟刷新）
    const currentTime = Math.floor(Date.now() / 1000);
    const expirationBuffer = 5 * 60; // 5分钟

    if (currentTime + expirationBuffer >= this.stsCredentials.expiredTime) {
      console.log('🔄 STS凭证即将过期，正在刷新...');
      await this.getStsCredentials();
      
      // 重新初始化ASR客户端
      await this.initAsrClient();
    }
  }

  /**
   * 启动腾讯云ASR连接
   */
  async startAsrConnection() {
    try {
      console.log('🔗 启动腾讯云实时语音识别连接...');
      
      if (!this.asrClient) {
        throw new Error('ASR客户端未初始化');
      }

      // 设置ASR事件回调
      this.asrClient.OnRecognitionStart = () => {
        console.log('🎯 ASR识别开始');
        this.asrConnected = true;
        this.updateStatus(SPEECH_STATUS.RECORDING);
      };

      this.asrClient.OnRecognitionResultChange = (result) => {
        console.log('📝 ASR实时结果:', result);
        if (result.voice_text_str) {
          this.recognitionResult = result.voice_text_str;
          this.triggerCallback('onResult', this.recognitionResult, false);
        }
      };

      this.asrClient.OnRecognitionComplete = (result) => {
        console.log('✅ ASR识别完成:', result);
        if (result.voice_text_str) {
          this.recognitionResult = result.voice_text_str;
          this.triggerCallback('onResult', this.recognitionResult, true);
          this.triggerCallback('onComplete', this.recognitionResult);
        }
        this.updateStatus(SPEECH_STATUS.COMPLETED);
      };

      this.asrClient.OnError = (error) => {
        console.error('❌ ASR识别错误:', error);
        this.handleError(SPEECH_ERROR_TYPES.RECOGNITION_ERROR, error.message || '语音识别失败');
      };

      // 启动ASR连接
      await this.asrClient.start();
      console.log('✅ 腾讯云ASR连接启动成功');
      
    } catch (error) {
      console.error('❌ 启动ASR连接失败:', error);
      throw error;
    }
  }

  /**
   * 开始录音
   */
  startRecording() {
    // #ifdef MP-WEIXIN
    let options;
    
    if (this.useBackupMode) {
      // 备用模式：使用wav格式便于后端处理
      options = {
        duration: Math.min(this.maxRecordingTime, 30000), // 限制最大30秒
        sampleRate: 16000,
        numberOfChannels: 1,
        encodeBitRate: 24000,
        format: 'wav', // 备用模式使用wav格式
        frameSize: 1280
      };
    } else {
      // 实时模式：使用mp3格式，支持帧数据回调
      options = {
        duration: Math.min(this.maxRecordingTime, 30000),
        sampleRate: 16000,
        numberOfChannels: 1,
        encodeBitRate: 24000,
        format: 'mp3',
        frameSize: 1280
      };
    }

    console.log('🎤 开始录音，配置:', options, this.useBackupMode ? '(备用模式)' : '(实时模式)');
    this.recorderManager.start(options);
    // #endif
  }

  /**
   * 发送音频数据到腾讯云ASR服务
   */
  sendAudioDataToAsr(frameBuffer) {
    try {
      if (!this.asrConnected || !this.asrClient) {
        // ASR未连接，缓存音频数据
        this.audioBuffer.push(frameBuffer);
        return;
      }

      // 发送缓存的音频数据
      if (this.audioBuffer.length > 0) {
        console.log('📡 发送缓存的音频数据，帧数:', this.audioBuffer.length);
        this.audioBuffer.forEach(buffer => {
          this.asrClient.write(buffer);
        });
        this.audioBuffer = [];
      }

      // 发送当前音频帧
      this.asrClient.write(frameBuffer);
      
    } catch (error) {
      console.error('❌ 发送音频数据失败:', error);
      // 不抛出错误，继续录音
    }
  }

  /**
   * 发送音频数据到语音识别服务（兼容旧接口）
   */
  sendAudioData(audioData) {
    this.sendAudioDataToAsr(audioData);
  }

  /**
   * 处理录音完成
   */
  handleRecordingComplete(recordResult) {
    console.log('🎤 录音完成，处理结果:', recordResult);

    if (this.useBackupMode) {
      // 备用模式：录音后上传识别
      this.handleBackupModeRecognition(recordResult);
    } else {
      // 实时模式：等待WebSocket返回最终结果
      // 结果会通过WebSocket回调处理
      console.log('📡 等待实时识别最终结果...');
    }
  }

  /**
   * 备用模式语音识别处理
   */
  async handleBackupModeRecognition(recordResult) {
    try {
      console.log('🔄 使用备用模式进行语音识别...');
      
      // 上传音频文件并获取识别结果
      const recognitionResult = await this.uploadAndRecognize(recordResult.tempFilePath);
      
      if (recognitionResult && recognitionResult.text) {
        this.recognitionResult = recognitionResult.text;
        this.triggerCallback('onResult', recognitionResult.text, true);
        this.triggerCallback('onComplete', recognitionResult.text);
        this.updateStatus(SPEECH_STATUS.COMPLETED);
      } else {
        throw new Error('识别结果为空');
      }
      
    } catch (error) {
      console.error('❌ 备用模式语音识别失败:', error);
      this.handleError(SPEECH_ERROR_TYPES.RECOGNITION_ERROR, error.message);
    }
  }

  /**
   * 上传音频文件并进行识别
   */
  async uploadAndRecognize(filePath) {
    try {
      console.log('📤 上传音频文件进行识别:', filePath);
      
      // 如果没有STS凭证，获取一下
      if (!this.stsCredentials) {
        await this.getStsCredentials();
      }

      // 上传音频文件到后端进行识别
      const response = await request({
        url: '/api/speech/recognize',
        method: 'POST',
        filePath: filePath,
        name: 'audio',
        formData: {
          format: 'wav',
          sampleRate: '16000',
          appId: TENCENT_ASR_CONFIG.appId,
          engineModelType: TENCENT_ASR_CONFIG.engineModelType
        }
      });

      return response;
      
    } catch (error) {
      console.error('❌ 上传识别失败:', error);
      throw new Error('语音识别服务暂时不可用');
    }
  }

  /**
   * 停止腾讯云ASR连接
   */
  stopAsrConnection() {
    try {
      if (this.asrClient && this.asrConnected) {
        console.log('⏹️ 停止腾讯云ASR连接...');
        
        // 发送剩余的音频缓冲数据
        if (this.audioBuffer.length > 0) {
          console.log('📡 发送剩余音频数据，帧数:', this.audioBuffer.length);
          this.audioBuffer.forEach(buffer => {
            this.asrClient.write(buffer);
          });
          this.audioBuffer = [];
        }

        // 停止ASR连接
        this.asrClient.stop();
        this.asrConnected = false;
        
        console.log('✅ 腾讯云ASR连接已停止');
      }
    } catch (error) {
      console.error('❌ 停止ASR连接失败:', error);
    }
  }

  /**
   * 关闭WebSocket连接（兼容旧接口）
   */
  closeWebSocket() {
    this.stopAsrConnection();
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
    
    // 清理资源
    this.cleanup();
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
  }

  /**
   * 清理资源
   */
  cleanup() {
    // 停止录音，仅当处于录音状态时
    // #ifdef MP-WEIXIN
    if (this.recorderManager && this.status === SPEECH_STATUS.RECORDING) {
      try {
        console.log('🧹 cleanup: 正在停止录音...');
        this.recorderManager.stop();
      } catch (error) {
        console.warn('🧹 cleanup: 停止录音失败（可能已被停止）:', error);
      }
    }
    // #endif
    
    // 停止ASR连接
    this.stopAsrConnection();
    
    // 清理状态
    console.log('🧹 cleanup: 重置服务状态...');
    this.status = SPEECH_STATUS.IDLE;
    this.recognitionResult = '';
    this.audioBuffer = [];
    this.asrConnected = false;
  }

  /**
   * 销毁服务
   */
  destroy() {
    this.cleanup();
    this.recorderManager = null;
    this.isInitialized = false;
    console.log('🗑️ 微信语音识别服务已销毁');
  }

  /**
   * 获取当前状态
   */
  getStatus() {
    return this.status;
  }

  /**
   * 获取识别结果
   */
  getResult() {
    return this.recognitionResult;
  }
}

// 创建单例实例
export const wechatSpeechService = new WechatSpeechRecognitionService();

// 导出状态和错误类型常量
export { SPEECH_STATUS, SPEECH_ERROR_TYPES };
