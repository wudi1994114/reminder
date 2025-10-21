<template>
  <view class="page-container">
    <!-- 顶部标题区域 -->
    <view class="header-section">
      <view class="header-content">
        <text class="page-title">🦆 奔跑鸭</text>
        <text class="page-subtitle">你的跑步训练助手</text>
      </view>
    </view>
    
    <!-- 主要内容区域 -->
    <scroll-view class="content-scroll" scroll-y>
      <view class="content-container">
        <!-- 功能菜单 -->
        <view class="menu-section">
          <view class="menu-card">
            <!-- 配速计算器 -->
            <view class="menu-item" @click="navigateToPage('/pages/running-duck/pace-calculator')">
              <view class="menu-icon">
                <text class="icon-text">⏱️</text>
              </view>
              <view class="menu-content">
                <text class="menu-label">配速计算器</text>
                <text class="menu-desc">计算跑步配速和完赛时间</text>
              </view>
              <text class="menu-arrow">›</text>
            </view>
            
            <view class="menu-divider"></view>
            
            <!-- 实时配速 -->
            <view class="menu-item" @click="navigateToPage('/pages/running-duck/realtime-pace')">
              <view class="menu-icon">
                <text class="icon-text">🏃</text>
              </view>
              <view class="menu-content">
                <text class="menu-label">实时配速</text>
                <text class="menu-desc">实时追踪你的跑步配速</text>
              </view>
              <text class="menu-arrow">›</text>
            </view>
            
            <view class="menu-divider"></view>
            
            <!-- 力量训练小助手 -->
            <view class="menu-item" @click="navigateToPage('/pages/running-duck/strength-training')">
              <view class="menu-icon">
                <text class="icon-text">💪</text>
              </view>
              <view class="menu-content">
                <text class="menu-label">力量训练小助手</text>
                <text class="menu-desc">专业的力量训练指导</text>
              </view>
              <text class="menu-arrow">›</text>
            </view>
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
  name: 'RunningDuck',
  methods: {
    navigateToPage(url) {
      console.log('导航到:', url);
      
      // 已开发完成的功能可以直接跳转
      const completedPages = [
        '/pages/running-duck/pace-calculator',
        '/pages/running-duck/realtime-pace',
        '/pages/running-duck/strength-training'
      ];
      
      if (completedPages.includes(url)) {
        uni.navigateTo({ url });
        return;
      }
      
      // 其他功能开发中
      uni.showToast({
        title: '功能开发中...',
        icon: 'none',
        duration: 2000
      });
    }
  }
};
</script>

<style scoped>
.page-container {
  min-height: 100vh;
  background-color: #fcfbf8;
  display: flex;
  flex-direction: column;
  font-family: 'Manrope', 'Noto Sans', sans-serif;
}

/* 顶部区域 */
.header-section {
  background: linear-gradient(135deg, #f7bd4a 0%, #f9a826 100%);
  padding: calc(var(--status-bar-height, 44rpx) + 40rpx) 32rpx 60rpx;
  flex-shrink: 0;
}

.header-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.page-title {
  font-size: 48rpx;
  font-weight: 700;
  color: #1c170d;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.page-subtitle {
  font-size: 24rpx;
  color: rgba(28, 23, 13, 0.7);
  line-height: 1.4;
}

/* 内容区域 */
.content-scroll {
  flex: 1;
  overflow-y: auto;
}

.content-container {
  padding: 32rpx;
}

/* 功能菜单 */
.menu-section {
  margin-bottom: 32rpx;
}

.menu-card {
  background-color: #ffffff;
  border: 2rpx solid #f4efe7;
  border-radius: 24rpx;
  box-shadow: 0 6rpx 24rpx rgba(28, 23, 13, 0.08);
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 32rpx;
  transition: all 0.2s ease;
  cursor: pointer;
}

.menu-item:active {
  background-color: #fcfbf8;
}

.menu-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80rpx;
  height: 80rpx;
  background-color: #f4efe7;
  border-radius: 20rpx;
  margin-right: 24rpx;
  flex-shrink: 0;
}

.icon-text {
  font-size: 40rpx;
}

.menu-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.menu-label {
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
  line-height: 1.2;
}

.menu-desc {
  font-size: 24rpx;
  color: #9d8148;
  line-height: 1.4;
}

.menu-arrow {
  font-size: 48rpx;
  color: #cccccc;
  font-weight: 300;
  margin-left: 16rpx;
  flex-shrink: 0;
}

.menu-divider {
  height: 1rpx;
  background-color: #f4efe7;
  margin: 0 32rpx;
}

/* 底部间距 */
.bottom-spacer {
  height: 120rpx;
}

/* 响应式调整 */
@media (max-width: 750rpx) {
  .header-section {
    padding: calc(var(--status-bar-height, 44rpx) + 32rpx) 24rpx 48rpx;
  }

  .page-title {
    font-size: 42rpx;
  }

  .content-container {
    padding: 24rpx;
  }

  .menu-item {
    padding: 24rpx;
  }

  .menu-icon {
    width: 64rpx;
    height: 64rpx;
  }

  .icon-text {
    font-size: 32rpx;
  }

  .menu-label {
    font-size: 28rpx;
  }

  .menu-desc {
    font-size: 22rpx;
  }
}
</style>

