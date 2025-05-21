<template>
  <view class="container">
    <view class="content-card">
      <view class="app-logo-section">
        <image class="app-logo" src="/static/images/logo.png"></image>
        <text class="app-name">Reminder Pro</text>
        <text class="app-version">Version 1.0.0 (Uni-App)</text>
      </view>

      <view class="info-section">
        <text class="section-title">应用简介</text>
        <text class="info-text">
          Reminder Pro 是一款帮助您记录和管理日常提醒事项的便捷工具。无论是工作任务、学习计划还是生活琐事，都可以轻松创建提醒，并通过日历视图清晰掌握。
        </text>
      </view>

      <view class="info-section">
        <text class="section-title">开发者</text>
        <text class="info-text">
          WUDI
        </text>
      </view>
      
      <view class="info-section">
        <text class="section-title">技术栈</text>
        <text class="info-text">
          Uni-App, Vue3, Spring Boot
        </text>
      </view>

      <view class="footer">
        <text class="copyright">© 2024 Reminder Pro. All rights reserved.</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      // 页面数据
    };
  },
  onLoad() {
    uni.setNavigationBarTitle({
      title: '关于应用'
    });
  }
};
</script>

<style scoped>
.container {
  padding: 30rpx;
  background-color: #f4f4f4;
  min-height: 100vh;
}

.content-card {
  background-color: #ffffff;
  border-radius: 15rpx;
  padding: 40rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
}

.app-logo-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 50rpx;
}

.app-logo {
  width: 150rpx;
  height: 150rpx;
  border-radius: 30rpx; /* Or 50% for circle */
  margin-bottom: 20rpx;
  background-color: #eee; /* Placeholder if logo is transparent or fails to load */
}

.app-name {
  font-size: 40rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 10rpx;
}

.app-version {
  font-size: 28rpx;
  color: #888;
}

.info-section {
  margin-bottom: 40rpx;
}

.section-title {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
  color: #444;
  margin-bottom: 15rpx;
  padding-bottom: 10rpx;
  border-bottom: 1rpx solid #eee;
}

.info-text {
  font-size: 28rpx;
  color: #666;
  line-height: 1.6;
}

.footer {
  text-align: center;
  margin-top: 60rpx;
  padding-top: 30rpx;
  border-top: 1rpx solid #eee;
}

.copyright {
  font-size: 24rpx;
  color: #999;
}
</style> 