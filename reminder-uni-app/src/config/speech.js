/**
 * 腾讯云语音服务配置
 * 注意：在生产环境中，这些敏感信息应该通过后端接口获取
 */

// 语音识别配置（简化版本，主要配置由后端STS服务提供）
export const SPEECH_CONFIG = {
  // 录音配置
  sampleRate: 16000, // 采样率
  frameSize: 1280, // 每帧音频长度
  maxRecordingTime: 30000, // 最大录音时长（毫秒）
};

// 腾讯云ASR配置
export const TENCENT_ASR_CONFIG = {
  // 应用ID - 需要从腾讯云控制台获取
  appId: '1362364412', // 腾讯云应用ID
  
  // 地域配置
  region: 'ap-beijing',
  
  // ASR引擎类型
  engine_model_type: '16k_zh', // 中文普通话
  
  // 音频格式
  voiceFormat: 1, // 1: wav, 4: speex, 6: silk, 8: mp3, 12: opus, 14: m4a
  
  // 其他配置
  filter_dirty: 1, // 过滤脏词
  filter_modal: 1, // 过滤语气词
  filter_punc: 1, // 智能添加标点
  convert_num_mode: 1, // 阿拉伯数字智能转换
  need_vad: 1, // 语音活动检测，开启后服务器会在检测到静音末点时主动断开连接
};

// 语音识别状态枚举
export const SPEECH_STATUS = {
  IDLE: 'idle',           // 空闲状态
  CONNECTING: 'connecting', // 连接中
  CONNECTED: 'connected', // 连接成功，等待录音
  RECORDING: 'recording',   // 录音中
  PROCESSING: 'processing', // 处理中
  COMPLETED: 'completed',   // 完成
  ERROR: 'error'           // 错误
};

// 错误类型枚举
export const SPEECH_ERROR_TYPES = {
  PERMISSION_DENIED: 'permission_denied',     // 权限被拒绝
  NETWORK_ERROR: 'network_error',            // 网络错误
  RECOGNITION_ERROR: 'recognition_error',     // 识别错误
  TIMEOUT_ERROR: 'timeout_error',            // 超时错误
  UNKNOWN_ERROR: 'unknown_error'             // 未知错误
};
