/**
 * 微信小程序语音识别服务
 * 基于 wx.getRecorderManager() 和腾讯云语音识别API
 * 采用纯JS WebSocket实现，不依赖官方SDK
 */
import { SPEECH_STATUS, SPEECH_ERROR_TYPES, TENCENT_ASR_CONFIG } from '@/config/speech.js';
import { request } from '@/services/api.js';
import CryptoJS from 'crypto-js';

// --- Helper Functions ---
/**
 * 生成UUID
 */
function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

export class WechatSpeechRecognitionService {
  constructor() {
    this.recorderManager = null;
    this.socketTask = null; // WebSocket 任务实例
    this.status = SPEECH_STATUS.IDLE;
    this.callbacks = {
      onStatusChange: null,
      onResult: null,
      onError: null,
      onComplete: null,
    };
    this.recognitionResult = '';
    this.isInitialized = false;
    this.recordingStartTime = 0;
    this.maxRecordingTime = 60000; // 60秒
    this.stsCredentials = null; // 存储临时凭证
    this.useBackupMode = false; // 备用模式：录音后上传
  }

  /**
   * 初始化服务
   */
  async init() {
    if (this.isInitialized) {
      return;
    }
    try {
      console.log('🚀 初始化微信语音识别服务 (纯JS实现)...');
      if (!this.checkWechatSupport()) {
        throw new Error('当前环境不支持微信语音功能');
      }
      this.initRecorderManager();
      this.isInitialized = true;
      console.log('✅ 微信语音识别服务初始化完成');
    } catch (error) {
      console.error('❌ 微信语音识别服务初始化失败:', error);
      this.handleError(SPEECH_ERROR_TYPES.UNKNOWN_ERROR, error.message);
      throw error;
    }
  }

  checkWechatSupport() {
    // #ifdef MP-WEIXIN
    return typeof wx !== 'undefined' && wx.getRecorderManager && uni.connectSocket;
    // #endif
    console.warn('⚠️ 当前环境不支持微信录音API或WebSocket');
    return false;
  }

  initRecorderManager() {
    // #ifdef MP-WEIXIN
    this.recorderManager = wx.getRecorderManager();

    this.recorderManager.onStart(() => {
      console.log('🎤 录音开始');
      this.recordingStartTime = Date.now();
      this.updateStatus(SPEECH_STATUS.RECORDING);
    });

    this.recorderManager.onStop(async (res) => {
      console.log('⏹️ 录音结束:', res);
      // 在实时模式下，由stopAsrConnection触发结束
      // 在备用模式下，需要处理录音文件
      if (this.useBackupMode) {
          this.updateStatus(SPEECH_STATUS.PROCESSING);
          await this.handleBackupModeRecognition(res);
      }
    });

    this.recorderManager.onError((error) => {
      console.error('❌ 录音错误:', error);
      this.handleError(SPEECH_ERROR_TYPES.RECOGNITION_ERROR, `录音失败: ${error.errMsg}`);
    });

    this.recorderManager.onFrameRecorded((res) => {
      if (!this.useBackupMode && this.socketTask && this.status === SPEECH_STATUS.RECORDING) {
        this.sendAudioDataToAsr(res.frameBuffer);
      }
    });
    // #endif
  }

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

      // 重置模式
      this.useBackupMode = false;

      // 检查录音权限
      await this.checkRecordPermission();
      
      try {
        // 尝试获取临时密钥并连接
        await this.checkAndRefreshCredentials();
        await this.startAsrConnection();
        // 连接成功后，开始录音，状态在onStart中变为RECORDING
        this.startRecording();
      } catch (error) {
        console.warn('⚠️ 实时识别模式启动失败，将自动切换到备用模式:', error);
        this.useBackupMode = true;
        this.updateStatus(SPEECH_STATUS.IDLE); // 重置状态
        this.handleError(SPEECH_ERROR_TYPES.CONNECTION_ERROR, '连接实时服务失败，已切换到录音后识别模式，请重新开始。');
      }

    } catch (error) {
      console.error('❌ 开始录音失败:', error);
      this.handleError(SPEECH_ERROR_TYPES.PERMISSION_DENIED, error.message);
    }
  }

  stopRecognition() {
    if (this.status !== SPEECH_STATUS.RECORDING) {
      console.warn('⚠️ 当前没有在录音');
      return;
    }
    console.log('⏹️ 停止录音识别...');
    this.updateStatus(SPEECH_STATUS.PROCESSING);
    
    // #ifdef MP-WEIXIN
    if (this.recorderManager) {
      this.recorderManager.stop();
    }
    // #endif

    // 如果是实时模式，则发送结束帧
    if (!this.useBackupMode) {
      this.stopAsrConnection();
    }
  }

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

  async getStsCredentials() {
    try {
      console.log('☁️ 获取STS临时凭证...');
      // 此处替换为您项目中获取STS凭证的API调用
      const credentials = await request({ url: '/sts/speech-credentials', method: 'GET' });

      if (!credentials || !credentials.tmpSecretId || !credentials.tmpSecretKey || !credentials.sessionToken) {
        throw new Error('获取到的临时凭证无效');
      }
      
      this.stsCredentials = {
        ...credentials,
        expiredTime: Date.now() + (credentials.expiredTime - 60) * 1000, // 提前60秒过期
      };
      
      console.log('✅ STS临时凭证获取成功');
    } catch (error) {
      console.error('❌ 获取STS临时凭证失败:', error);
      this.stsCredentials = null;
      throw error;
    }
  }

  _createSignature() {
    const { tmpSecretId, tmpSecretKey } = this.stsCredentials;
    const host = 'asr.cloud.tencent.com';
    // 从配置中获取AppID
    const appId = TENCENT_ASR_CONFIG.appId;
    const path = '/asr/v2/' + appId;
    
    const params = {
      engine_model_type: TENCENT_ASR_CONFIG.engineModelType,
      secretid: tmpSecretId,
      timestamp: Math.floor(Date.now() / 1000),
      expired: Math.floor(Date.now() / 1000) + 3600, // 1小时有效期
      nonce: Math.floor(Math.random() * 100000),
      voice_id: generateUUID(),
      // 如果使用临时密钥，必须传递 token
      token: this.stsCredentials.sessionToken,
    };

    // 1. 对参数进行升序排序
    const sortedKeys = Object.keys(params).sort();
    
    // 2. 拼接请求字符串
    let paramStr = sortedKeys.map(key => `${key}=${params[key]}`).join('&');
    let signUrl = `${host}${path}?${paramStr}`;
    
    // 3. HMAC-SHA1加密
    let signature = CryptoJS.HmacSHA1(signUrl, tmpSecretKey);
    
    // 4. Base64编码
    signature = CryptoJS.enc.Base64.stringify(signature);

    // 5. URL编码并拼接最终URL
    const finalUrl = `wss://${host}${path}?${paramStr}&signature=${encodeURIComponent(signature)}`;
    
    return finalUrl;
  }
  
  async checkAndRefreshCredentials() {
    if (!this.stsCredentials || Date.now() >= this.stsCredentials.expiredTime) {
      console.log('ℹ️ 临时凭证不存在或已过期，正在刷新...');
      await this.getStsCredentials();
    } else {
      console.log('✅ 临时凭证有效');
    }
  }

  async startAsrConnection() {
    return new Promise((resolve, reject) => {
      if (this.socketTask) {
        this.socketTask.close();
      }
      
      const signedUrl = this._createSignature();
      console.log('✍️ 生成签名URL:', signedUrl);

      this.socketTask = uni.connectSocket({
        url: signedUrl,
        success: () => {}, // success回调不代表连接成功
        fail: (err) => {
          console.error('❌ uni.connectSocket 调用失败:', err);
          reject(new Error('WebSocket创建失败'));
        }
      });

      this.socketTask.onOpen(() => {
        console.log('🔗 WebSocket 连接已打开');
        this.updateStatus(SPEECH_STATUS.CONNECTED);
        resolve();
      });

      this.socketTask.onMessage((res) => {
        const data = JSON.parse(res.data);
        if (data.code !== 0) {
          console.error('❌ WebSocket收到错误消息:', data);
          this.handleError(SPEECH_ERROR_TYPES.RECOGNITION_ERROR, `实时识别错误: ${data.message}`);
          this.stopRecognition();
          return;
        }

        if (data.result) {
            this.recognitionResult = data.result.voice_text_str;
            this.triggerCallback('onResult', this.recognitionResult);
        }

        if (data.final === 1) {
            console.log('🏁 识别流程结束');
            this.updateStatus(SPEECH_STATUS.IDLE);
            this.triggerCallback('onComplete', this.recognitionResult);
            this.closeWebSocket();
        }
      });

      this.socketTask.onError((err) => {
        console.error('❌ WebSocket 连接发生错误:', err);
        this.handleError(SPEECH_ERROR_TYPES.CONNECTION_ERROR, 'WebSocket连接错误');
        reject(new Error('WebSocket连接错误'));
      });

      this.socketTask.onClose(() => {
        console.log('🔌 WebSocket 连接已关闭');
        this.socketTask = null;
        if (this.status !== SPEECH_STATUS.IDLE) {
          this.updateStatus(SPEECH_STATUS.IDLE);
        }
      });
    });
  }

  startRecording() {
    // #ifdef MP-WEIXIN
    this.recorderManager.start(TENCENT_ASR_CONFIG.recorder);
    // #endif
  }

  sendAudioDataToAsr(frameBuffer) {
    if (this.socketTask && this.status === SPEECH_STATUS.RECORDING) {
      this.socketTask.send({
        data: frameBuffer,
        fail: (err) => {
            console.error('❌ 发送音频帧失败:', err);
        }
      });
    }
  }

  async handleBackupModeRecognition(recordResult) {
    if (!recordResult || !recordResult.tempFilePath) {
      this.handleError(SPEECH_ERROR_TYPES.UNKNOWN_ERROR, '录音文件未找到');
      return;
    }
    try {
      const result = await this.uploadAndRecognize(recordResult.tempFilePath);
      this.recognitionResult = result;
      this.updateStatus(SPEECH_STATUS.IDLE);
      this.triggerCallback('onComplete', this.recognitionResult);
    } catch (error) {
      this.handleError(SPEECH_ERROR_TYPES.RECOGNITION_ERROR, error.message);
    }
  }

  async uploadAndRecognize(filePath) {
    console.log(`☁️ 备用模式：上传文件 ${filePath} 进行识别...`);
    // 这里应该是您项目中将文件上传到后端，再由后端调用腾讯云API的逻辑
    // 为了演示，我们直接返回一个模拟结果
    return new Promise((resolve, reject) => {
        // ... 原有的上传和识别逻辑 ...
        // ... 我们假设它调用一个 /api/speech/recognize 接口 ...
        uni.uploadFile({
            url: request.API_URL + '/speech/recognize',
            filePath: filePath,
            name: 'file',
            success: (uploadRes) => {
                const data = JSON.parse(uploadRes.data);
                if (data.success) {
                    resolve(data.text);
                } else {
                    reject(new Error(data.message || '备用模式识别失败'));
                }
            },
            fail: (err) => {
                reject(new Error('文件上传失败'));
            }
        });
    });
  }

  stopAsrConnection() {
    if (this.socketTask && this.status === SPEECH_STATUS.PROCESSING) {
        console.log('🏁 发送结束帧...');
        this.socketTask.send({
            data: JSON.stringify({ type: 'end' }),
            fail: (err) => {
                console.error('❌ 发送结束帧失败:', err);
                // 即使失败，也尝试关闭
                this.closeWebSocket();
            }
        });
    } else {
        this.closeWebSocket();
    }
  }

  closeWebSocket() {
    if (this.socketTask) {
      this.socketTask.close();
      this.socketTask = null;
    }
  }
  
  updateStatus(newStatus) {
    if (this.status !== newStatus) {
      this.status = newStatus;
      this.triggerCallback('onStatusChange', newStatus);
    }
  }

  handleError(errorType, message) {
    this.updateStatus(SPEECH_STATUS.IDLE);
    this.triggerCallback('onError', { type: errorType, message });
    this.closeWebSocket();
  }

  triggerCallback(callbackName, ...args) {
    if (this.callbacks[callbackName] && typeof this.callbacks[callbackName] === 'function') {
      this.callbacks[callbackName](...args);
    }
  }

  setCallbacks(callbacks) {
    this.callbacks = { ...this.callbacks, ...callbacks };
  }

  cleanup() {
    console.log('🧹 清理语音服务...');
    if (this.recorderManager) {
      this.recorderManager.stop();
    }
    this.closeWebSocket();
    this.stsCredentials = null;
    this.status = SPEECH_STATUS.IDLE;
  }

  destroy() {
    this.cleanup();
    this.isInitialized = false;
    this.recorderManager = null; // 释放引用
  }

  getStatus() {
    return this.status;
  }
  
  getResult() {
    return this.recognitionResult;
  }
}

// 创建单例实例
export const wechatSpeechService = new WechatSpeechRecognitionService();

// 导出状态和错误类型常量
export { SPEECH_STATUS, SPEECH_ERROR_TYPES };
