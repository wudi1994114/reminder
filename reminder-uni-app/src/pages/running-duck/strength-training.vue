<template>
  <view class="page-container">
    <!-- 渐变背景 -->
    <view class="bg-gradient"></view>
    
    <!-- 主要内容区域 -->
    <scroll-view class="content-scroll" scroll-y>
      <view class="content-container">
        <!-- 返回按钮 -->
        <view class="header-bar">
          <view class="back-btn" @click="goBack">
            <text class="back-icon">‹</text>
          </view>
          <text class="header-title">力量训练</text>
          <view class="placeholder"></view>
        </view>
        
        <!-- 音频封面 -->
        <view class="album-cover-section">
          <view class="album-cover" :class="{ 'rotating': isPlaying }">
            <view class="cover-inner">
              <text class="cover-icon">💪</text>
            </view>
            <!-- 播放状态指示器 -->
            <view v-if="isPlaying" class="play-indicator">
              <view class="sound-wave">
                <view class="wave-bar"></view>
                <view class="wave-bar"></view>
                <view class="wave-bar"></view>
              </view>
            </view>
          </view>
        </view>
        
        <!-- 音频信息 -->
        <view class="audio-info-section">
          <text class="audio-title">力量训练指导</text>
          <text class="audio-artist">专业训练音频</text>
        </view>
        
        <!-- 进度条区域 -->
        <view class="progress-section">
          <!-- 时间显示 -->
          <view class="time-display">
            <text class="time-text">{{ formatTime(currentTime) }}</text>
            <text class="time-text total-time">{{ formatTime(duration) }}</text>
          </view>
          
          <!-- 进度条 -->
          <view class="progress-bar-wrapper">
            <slider 
              class="progress-slider"
              :value="currentTime"
              :min="0"
              :max="duration || 100"
              :disabled="!duration || isLoading"
              activeColor="#ffffff"
              backgroundColor="rgba(255, 255, 255, 0.3)"
              block-size="16"
              block-color="#ffffff"
              @change="onProgressChange"
              @changing="onProgressChanging"
            />
            <!-- 缓冲进度背景 -->
            <view v-if="bufferedPercent > 0" class="buffer-bar" :style="{ width: bufferedPercent + '%' }"></view>
          </view>
        </view>
        
        <!-- 播放控制区域 -->
        <view class="controls-section">
          <!-- 停止按钮 -->
          <view 
            class="control-btn secondary-btn" 
            :class="{ disabled: !isPlaying && !isPaused }"
            @click="stopAudio"
          >
            <text class="control-icon">⏹</text>
          </view>
          
          <!-- 主播放/暂停按钮 -->
          <view 
            class="control-btn main-control-btn"
            :class="{ playing: isPlaying }"
            @click="togglePlayPause"
          >
            <text v-if="isPlaying" class="control-icon main-icon">⏸</text>
            <text v-else class="control-icon main-icon">▶</text>
          </view>
          
          <!-- 占位按钮（保持对称） -->
          <view class="control-btn secondary-btn placeholder-btn">
            <text class="control-icon">♫</text>
          </view>
        </view>
        
        <!-- 底部间距 -->
        <view class="bottom-spacer"></view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
export default {
  name: 'StrengthTraining',
  data() {
    return {
      // 云存储音频路径
      cloudAudioPath: 'cloud://prod-3gel427g5936cfa7.7072-prod-3gel427g5936cfa7-1362668225/train_voice/乔某某人 - 无限恐怖.078（利用资源）.mp3',
      // 音频实例
      audioContext: null,
      // 播放状态
      isPlaying: false,
      isPaused: false,
      isLoading: false,
      loadingText: '准备中...',
      // 播放时间
      currentTime: 0,
      duration: 0,
      // 缓冲进度
      bufferedPercent: 0,
      // 进度更新定时器
      progressTimer: null,
      // 是否正在拖动进度条
      isSeeking: false,
      // 播放重试次数
      retryCount: 0,
      maxRetries: 3
      ,
      // 通过云存储获取的临时 https 音频地址（缓存复用）
      tempAudioUrl: ''
    };
  },
  
  computed: {
    // 播放进度百分比
    progressPercent() {
      if (this.duration === 0) return 0;
      return (this.currentTime / this.duration) * 100;
    }
  },
  
  onLoad() {
    console.log('力量训练页面加载');
    this.initAudio();
  },
  
  onUnload() {
    console.log('力量训练页面卸载，清理音频资源');
    this.destroyAudio();
  },
  
  methods: {
    /**
     * 返回上一页
     */
    goBack() {
      uni.navigateBack();
    },
    
    /**
     * 切换播放/暂停
     */
    togglePlayPause() {
      if (this.isPlaying) {
        this.pauseAudio();
      } else {
        this.playAudio();
      }
    },
    
    /**
     * 初始化音频
     */
    initAudio() {
      // #ifdef MP-WEIXIN
      try {
        console.log('🎵 开始初始化音频实例...');
        
        this.audioContext = uni.createInnerAudioContext();
        
        if (!this.audioContext) {
          throw new Error('创建音频实例失败');
        }
        
        // 启用流式播放相关配置
        this.audioContext.autoplay = false;
        
        console.log('✅ 音频实例已创建成功');
        console.log('📊 音频实例信息:', {
          autoplay: this.audioContext.autoplay,
          src: this.audioContext.src || '(未设置)',
          paused: this.audioContext.paused
        });
      
      // 监听音频播放事件
      this.audioContext.onPlay(() => {
        console.log('🎵 音频开始播放');
        this.isPlaying = true;
        this.isPaused = false;
        this.isLoading = false;
        this.retryCount = 0;
        this.startProgressUpdate();
      });
      
      // 监听音频暂停事件
      this.audioContext.onPause(() => {
        console.log('⏸️ 音频暂停');
        this.isPlaying = false;
        this.isPaused = true;
        this.stopProgressUpdate();
      });
      
      // 监听音频停止事件
      this.audioContext.onStop(() => {
        console.log('⏹️ 音频停止');
        this.isPlaying = false;
        this.isPaused = false;
        this.currentTime = 0;
        this.bufferedPercent = 0;
        this.stopProgressUpdate();
      });
      
      // 监听音频播放完成事件
      this.audioContext.onEnded(() => {
        console.log('✅ 音频播放完成');
        this.isPlaying = false;
        this.isPaused = false;
        this.currentTime = 0;
        this.stopProgressUpdate();
        
        // 不提示完成，保持静默
      });
      
      // 监听音频错误事件
      this.audioContext.onError((err) => {
        console.error('❌ 音频播放错误:', err);
        
        const isTimeout = err.errMsg && err.errMsg.includes('timeout');
        
        if (isTimeout && this.retryCount < this.maxRetries) {
          this.retryCount++;
          console.log(`⚠️ 加载超时，正在重试 (${this.retryCount}/${this.maxRetries})...`);
          
          // 静默重试，不提示
          
          setTimeout(() => {
            this.playAudio();
          }, 1000);
        } else {
          this.isPlaying = false;
          this.isPaused = false;
          this.isLoading = false;
          this.retryCount = 0;
          
          let errorMsg = '音频加载失败';
          if (isTimeout) {
            errorMsg = '音频文件较大，加载超时，请检查网络';
          } else if (err.errMsg) {
            errorMsg = err.errMsg;
          }
          
          // 静默失败，不提示
        }
      });
      
      // 监听音频可以开始播放
      this.audioContext.onCanplay(() => {
        console.log('✅ 音频可以开始播放（流式加载中），时长:', this.audioContext.duration);
        this.duration = this.audioContext.duration || 0;
        
        if (this.isLoading && this.duration > 0) {
          this.isLoading = false;
          console.log('📡 开始流式播放，无需等待完整下载');
        }
      });
      
      // 监听音频等待数据
      this.audioContext.onWaiting(() => {
        console.log('⏳ 音频缓冲不足，等待数据...');
        this.loadingText = '缓冲中...';
        if (!this.isPlaying) {
          this.isLoading = true;
        }
      });
      
      // 监听时间更新
      this.audioContext.onTimeUpdate(() => {
        if (this.audioContext.buffered > 0 && this.duration > 0) {
          const buffered = this.audioContext.buffered;
          const percent = Math.floor((buffered / this.duration) * 100);
          this.bufferedPercent = Math.min(100, Math.max(0, percent));
          
          if (this.bufferedPercent < 100 && this.bufferedPercent % 10 === 0) {
            console.log(`📡 缓冲进度: ${this.bufferedPercent}%，当前播放: ${Math.floor(this.currentTime)}s / ${Math.floor(this.duration)}s`);
          }
        }
      });
      
      // 监听跳转完成
      this.audioContext.onSeeked(() => {
        console.log('⏩ 跳转完成');
        this.isSeeking = false;
        if (this.isLoading) {
          this.isLoading = false;
        }
      });
      
      // 监听跳转中
      this.audioContext.onSeeking(() => {
        console.log('⏩ 跳转中...');
        this.isSeeking = true;
        this.loadingText = '跳转中...';
        this.isLoading = true;
      });
      
      console.log('✅ 音频事件监听器已全部设置完成');
      
      } catch (error) {
        console.error('❌ 音频初始化失败:', error);
        // 静默失败，不提示
      }
      // #endif
    },
    
    /**
     * 播放音频
     */
    async playAudio() {
      // #ifdef MP-WEIXIN
      try {
        // 检查音频实例是否存在
        if (!this.audioContext) {
          console.warn('⚠️ 音频实例未初始化，重新初始化...');
          this.initAudio();
          // 等待一下确保初始化完成
          await new Promise(resolve => setTimeout(resolve, 100));
        }
        
        // 再次检查
        if (!this.audioContext) {
          throw new Error('音频实例初始化失败');
        }
        
        if (this.audioContext.src) {
          console.log('▶️ 恢复播放');
          this.audioContext.play();
          return;
        }
        
        this.isLoading = true;
        this.loadingText = '';
        console.log('📡 开始播放音频(获取临时URL):', this.cloudAudioPath);

        // 优先复用已获取的临时URL，避免重复请求
        let tempUrl = this.tempAudioUrl;
        if (!tempUrl) {
          const res = await wx.cloud.getTempFileURL({ fileList: [this.cloudAudioPath] });
          const fileItem = res && res.fileList && res.fileList[0];
          if (fileItem && fileItem.tempFileURL) {
            tempUrl = fileItem.tempFileURL;
            this.tempAudioUrl = tempUrl;
            console.log('✅ 获取到临时URL');
          } else {
            console.error('❌ 获取临时URL失败:', res);
            throw new Error('获取音频临时链接失败');
          }
        }

        // 设置可播放的 https 源并开始播放
        this.audioContext.src = tempUrl;
        console.log('▶️ 立即开始播放(https):', tempUrl);
        this.audioContext.play();
        
      } catch (error) {
        console.error('❌ 播放音频失败:', error);
        this.isLoading = false;
        this.isPaused = false;
        this.retryCount = 0;
        // 静默失败，不提示
      }
      // #endif
      
      // #ifndef MP-WEIXIN
      // 非小程序平台静默忽略
      // #endif
    },
    
    /**
     * 暂停音频
     */
    pauseAudio() {
      if (!this.audioContext) {
        console.warn('⚠️ 音频实例不存在');
        return;
      }
      
      if (this.isPlaying) {
        console.log('⏸️ 暂停播放');
        this.audioContext.pause();
      }
    },
    
    /**
     * 停止音频
     */
    stopAudio() {
      if (!this.audioContext) {
        console.warn('⚠️ 音频实例不存在');
        return;
      }
      
      console.log('⏹️ 停止播放');
      this.audioContext.stop();
      this.audioContext.src = '';
      this.retryCount = 0;
    },
    
    /**
     * 进度条拖动中
     */
    onProgressChanging(e) {
      this.isSeeking = true;
      this.currentTime = e.detail.value;
    },
    
    /**
     * 进度条拖动完成
     */
    onProgressChange(e) {
      const seekTime = e.detail.value;
      console.log('跳转到:', seekTime, '秒');
      
      if (!this.audioContext) {
        console.warn('⚠️ 音频实例不存在，无法跳转');
        this.isSeeking = false;
        return;
      }
      
      if (this.duration > 0) {
        this.audioContext.seek(seekTime);
        this.currentTime = seekTime;
        
        // 静默跳转
      }
      
      this.isSeeking = false;
    },
    
    /**
     * 开始更新播放进度
     */
    startProgressUpdate() {
      this.stopProgressUpdate();
      
      this.progressTimer = setInterval(() => {
        if (this.audioContext && this.isPlaying && !this.isSeeking) {
          this.currentTime = this.audioContext.currentTime;
          this.duration = this.audioContext.duration || this.duration;
        }
      }, 200);
    },
    
    /**
     * 停止更新播放进度
     */
    stopProgressUpdate() {
      if (this.progressTimer) {
        clearInterval(this.progressTimer);
        this.progressTimer = null;
      }
    },
    
    /**
     * 销毁音频资源
     */
    destroyAudio() {
      this.stopProgressUpdate();
      
      if (this.audioContext) {
        this.audioContext.stop();
        this.audioContext.destroy();
        this.audioContext = null;
      }
    },
    
    /**
     * 格式化时间（秒 -> mm:ss）
     */
    formatTime(seconds) {
      if (!seconds || isNaN(seconds)) return '00:00';
      
      const mins = Math.floor(seconds / 60);
      const secs = Math.floor(seconds % 60);
      
      return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
  }
};
</script>

<style scoped>
/* 系统主题变量 */
:root {
  --color-bg-start: #fffaf0;
  --color-bg-mid: #ffd489;
  --color-bg-end: #ffcd76;
  --color-text-primary: #1c170d;
  --color-text-muted: #b08f57;
  --color-text-secondary: #b08f57;
  --color-card-bg: #ffffff;
  --color-card-bg-2: #fdfbf6;
  --color-main: #ffcf7a;
  --color-surface: rgba(28, 23, 13, 0.04);
  --color-surface-strong: rgba(28, 23, 13, 0.08);
  --color-buffer: rgba(28, 23, 13, 0.18);
}

@media (prefers-color-scheme: dark) {
  :root {
    --color-bg-start: #141821;
    --color-bg-mid: #0f1220;
    --color-bg-end: #1d2033;
    --color-text-primary: #e6e6e6;
    --color-text-muted: #dfcfad;
    --color-text-secondary: #d2bb89;
    --color-card-bg: #1c2233;
    --color-card-bg-2: #242b3d;
    --color-main: #ffcf7a;
    --color-surface: rgba(255, 255, 255, 0.04);
    --color-surface-strong: rgba(255, 255, 255, 0.08);
    --color-buffer: rgba(230, 230, 230, 0.35);
  }
}

.page-container {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

/* 渐变背景 */
.bg-gradient {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(180deg, rgb(235, 251, 248) 0%, #ffffff 100%);
  z-index: 0;
}

/* 内容滚动区域 */
.content-scroll {
  position: relative;
  z-index: 1;
  height: 100vh;
}

.content-container {
  padding: 0;
  display: flex;
  flex-direction: column;
}

/* 顶部导航栏 */
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: calc(var(--status-bar-height, 44rpx) + 20rpx) 32rpx 20rpx;
  backdrop-filter: blur(20rpx);
  background: var(--color-surface);
}

.back-btn {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-surface-strong);
  border-radius: 50%;
  backdrop-filter: blur(10rpx);
}

.back-icon {
  font-size: 48rpx;
  color: var(--color-text-primary);
  font-weight: 300;
  line-height: 1;
}

.header-title {
  font-size: 32rpx;
  font-weight: 600;
  color: var(--color-text-primary);
  text-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
}

.placeholder {
  width: 72rpx;
}

/* 音频封面区域 */
.album-cover-section {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80rpx 0;
}

.album-cover {
  position: relative;
  width: 480rpx;
  height: 480rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-card-bg) 0%, var(--color-card-bg-2) 100%);
  box-shadow: 
    0 20rpx 60rpx rgba(0, 0, 0, 0.18),
    0 6rpx 24rpx rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

.cover-inner {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f7bd4a 0%, #f9a826 100%);
  position: relative;
}

.cover-inner::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 30% 30%, rgba(255,255,255,0.35), rgba(255,255,255,0) 60%);
}

.cover-icon {
  font-size: 200rpx;
}

/* 旋转动画 */
.album-cover.rotating {
  animation: rotate 20s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 播放状态指示器 */
.play-indicator {
  position: absolute;
  bottom: 40rpx;
  left: 50%;
  transform: translateX(-50%);
}

.sound-wave {
  display: flex;
  align-items: flex-end;
  gap: 8rpx;
  height: 40rpx;
}

.wave-bar {
  width: 6rpx;
  height: 100%;
  background: #ffffff;
  border-radius: 3rpx;
  animation: wave 1s ease-in-out infinite;
}

.wave-bar:nth-child(1) {
  animation-delay: 0s;
}

.wave-bar:nth-child(2) {
  animation-delay: 0.2s;
}

.wave-bar:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes wave {
  0%, 100% {
    transform: scaleY(0.3);
  }
  50% {
    transform: scaleY(1);
  }
}

/* 音频信息区域 */
.audio-info-section {
  padding: 0 64rpx;
  text-align: center;
  margin-bottom: 60rpx;
}

.audio-title {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 16rpx;
  text-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.2);
}

.audio-artist {
  display: block;
  font-size: 26rpx;
  color: var(--color-text-secondary);
  text-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
}

/* 进度条区域 */
.progress-section {
  padding: 0 64rpx;
  margin-bottom: 80rpx;
}

.time-display {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.time-text {
  font-size: 24rpx;
  color: var(--color-text-muted);
  font-weight: 500;
  text-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.1);
}

.progress-bar-wrapper {
  position: relative;
}

.progress-slider {
  width: 100%;
  position: relative;
  z-index: 2;
  height: 40rpx;
}

.buffer-bar {
  position: absolute;
  top: 50%;
  left: 0;
  height: 4rpx;
  background: var(--color-buffer);
  transform: translateY(-50%);
  border-radius: 2rpx;
  z-index: 1;
  pointer-events: none;
  transition: width 0.2s ease;
}

/* 控制按钮区域 */
.controls-section {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 48rpx;
  padding: 0 64rpx;
  margin-top: 32rpx;
  margin-bottom: 40rpx;
}

.control-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  backdrop-filter: blur(10rpx);
  transition: all 0.3s ease;
}

.secondary-btn {
  width: 96rpx;
  height: 96rpx;
  background: var(--color-surface-strong);
}

.secondary-btn:active:not(.disabled) {
  transform: scale(0.9);
  background: rgba(255, 255, 255, 0.3);
}

.secondary-btn.disabled {
  opacity: 0.4;
}

.main-control-btn {
  width: 144rpx;
  height: 144rpx;
  background: #fff7e8;
  box-shadow: 
    0 8rpx 28rpx rgba(0, 0, 0, 0.22),
    0 4rpx 12rpx rgba(0, 0, 0, 0.14);
}

.main-control-btn:active:not(.loading) {
  transform: scale(0.95);
}

.main-control-btn.playing {
  transform: scale(0.98);
  box-shadow: 0 10rpx 36rpx rgba(0,0,0,0.35), 0 6rpx 20rpx rgba(0,0,0,0.25);
}

.control-icon {
  font-size: 40rpx;
  color: var(--color-text-primary);
}

.main-icon {
  font-size: 56rpx;
  color: var(--color-main);
}

.loading-spinner {
  font-size: 48rpx;
  animation: rotate 2s linear infinite;
}

.placeholder-btn {
  opacity: 0.6;
}

/* 状态提示 */
.status-tip {
  text-align: center;
  margin-bottom: 40rpx;
}

.status-text {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
  padding: 12rpx 32rpx;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 24rpx;
  backdrop-filter: blur(10rpx);
}

/* 功能提示卡片 */
.feature-tips {
  display: flex;
  justify-content: center;
  gap: 32rpx;
  padding: 0 64rpx;
  margin-bottom: 40rpx;
}

.tip-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  padding: 24rpx 32rpx;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 20rpx;
  backdrop-filter: blur(10rpx);
  min-width: 140rpx;
}

.tip-icon-small {
  font-size: 32rpx;
}

.tip-text-small {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}

/* 底部间距 */
.bottom-spacer {
  height: 120rpx;
}

/* 响应式调整 */
@media (max-width: 750rpx) {
  .album-cover {
    width: 400rpx;
    height: 400rpx;
  }

  .cover-icon {
    font-size: 160rpx;
  }

  .audio-title {
    font-size: 36rpx;
  }

  .controls-section {
    gap: 40rpx;
  }

  .secondary-btn {
    width: 80rpx;
    height: 80rpx;
  }

  .main-control-btn {
    width: 120rpx;
    height: 120rpx;
  }

  .main-icon {
    font-size: 48rpx;
  }
}
</style>
