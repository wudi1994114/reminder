import { SPEECH_STATUS } from '@/config/speech.js';

/**
 * A safe, inert version of the WechatSpeechRecognitionService for production release.
 * All methods are stubbed to prevent crashes while the feature is on hold.
 */
export class WechatSpeechRecognitionService {
  constructor() {
    this.status = SPEECH_STATUS.IDLE;
    this.callbacks = {};
    console.log('@@ WechatSpeech - Service is in safe mode for production release.');
  }

  // All methods are now safe stubs.
  async initAudioApi() { console.warn('Voice feature is temporarily disabled.'); }
  async startRecognition() { 
    console.warn('Voice feature is temporarily disabled.');
    this.handleError('disabled', '语音功能暂不可用');
  }
  stopRecognition() {}
  async startMicrophone() {}
  stopMicrophone() {}
  async getStsCredentials() { return {}; }
  _createSignature() { return ''; }
  async checkAndRefreshCredentials() {}
  async startAsrConnection() {}
  sendAudioDataToAsr(frameBuffer) {}
  stopAsrConnection() {}
  closeWebSocket() {}
  
  updateStatus(newStatus) {
    this.status = newStatus;
    this.triggerCallback('onStatusChange', this.status);
  }

  handleError(errorType, message) {
    this.updateStatus(SPEECH_STATUS.IDLE);
    this.triggerCallback('onError', { type: errorType, message });
  }

  triggerCallback(callbackName, ...args) {
    if (this.callbacks[callbackName]) {
      this.callbacks[callbackName](...args);
    }
  }

  setCallbacks(callbacks) {
    this.callbacks = { ...this.callbacks, ...callbacks };
  }
  
  cleanup() {
    this.status = SPEECH_STATUS.IDLE;
  }
  destroy() {
    this.cleanup();
  }
  cancelRecognition() {
    this.updateStatus(SPEECH_STATUS.IDLE);
  }
  getStatus() {
    return this.status;
  }
  getResult() {
    return '';
  }
}

export const wechatSpeechService = new WechatSpeechRecognitionService();
