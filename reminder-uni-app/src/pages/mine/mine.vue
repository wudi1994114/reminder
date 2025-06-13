<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="header-section">
      <view class="nav-container">
        <view class="nav-spacer"></view>
        <view class="title-container">
          <text class="page-title">我的</text>
        </view>
        <view class="nav-actions">
          <view class="settings-btn" @click="navTo('/pages/settings/about')">
            <text class="settings-icon">⚙️</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 主要内容区域 -->
    <scroll-view class="content-scroll" scroll-y>
      <view class="content-container">
        <!-- 用户信息区域 -->
        <view class="user-section">
          <view class="user-avatar" @click="goToUserProfile">
            <image 
              class="avatar-image" 
              :src="displayAvatarUrl" 
              mode="aspectFill"
              @error="onAvatarError"
            ></image>
          </view>
          <view class="user-info">
            <text class="username">{{ userState.user?.nickname || userState.user?.username || '未登录' }}</text>
            <text class="user-email" v-if="userState.user?.email">{{ userState.user?.email }}</text>
            <text class="user-id" v-else-if="userState.user?.id">ID: {{ userState.user?.id }}</text>
          </view>
          <button class="login-btn" v-if="!userState.isAuthenticated" @click="goToLogin">
            <text class="btn-text">登录/注册</text>
          </button>
        </view>
        
        <!-- 统计信息 -->
        <view class="stats-section" v-if="userState.isAuthenticated">
          <view class="section-header">
            <text class="section-title">统计数据</text>
          </view>
          <view class="stats-grid">
            <view class="stat-card">
              <text class="stat-number">{{ stats.totalReminders || 0 }}</text>
              <text class="stat-label">总提醒数</text>
            </view>
            <view class="stat-card">
              <text class="stat-number">{{ stats.pendingReminders || 0 }}</text>
              <text class="stat-label">待提醒</text>
            </view>
            <view class="stat-card">
              <text class="stat-number">{{ stats.completedReminders || 0 }}</text>
              <text class="stat-label">已完成</text>
            </view>
          </view>
        </view>
        
        <!-- 功能菜单 -->
        <view class="menu-section">
          <view class="section-header">
            <text class="section-title">功能设置</text>
          </view>
          <view class="menu-card">
            <view class="menu-item" @click="navTo('/pages/profile/edit')">
              <view class="menu-icon">
                <text class="icon-text">👤</text>
              </view>
              <text class="menu-label">编辑个人资料</text>
              <text class="menu-arrow">›</text>
            </view>
            <view class="menu-divider"></view>
            <view class="menu-item" @click="navTo('/pages/settings/notification')">
              <view class="menu-icon">
                <text class="icon-text">🔔</text>
              </view>
              <text class="menu-label">提醒设置</text>
              <text class="menu-arrow">›</text>
            </view>
            <view class="menu-divider"></view>
            <view class="menu-item" @click="navTo('/pages/settings/about')">
              <view class="menu-icon">
                <text class="icon-text">ℹ️</text>
              </view>
              <text class="menu-label">关于应用</text>
              <text class="menu-arrow">›</text>
            </view>
          </view>
        </view>
        
        <!-- 退出登录按钮 -->
        <view class="logout-section" v-if="userState.isAuthenticated">
          <button class="logout-button" @click="confirmLogout">
            <text class="logout-text">退出登录</text>
          </button>
        </view>
        
        <!-- 底部间距 -->
        <view class="bottom-spacer"></view>
      </view>
    </scroll-view>

    <!-- 确认对话框 -->
    <confirm-dialog
      :show="showLogoutConfirmDialog"
      title="确认退出"
      message="确定要退出当前账号吗？"
      @confirm="handleLogout"
      @cancel="cancelLogout"
    />
  </view>
</template>

<script>
import { ref, reactive, computed, watch } from 'vue';
import { UserService, userState } from '../../services/userService';

export default {
  onShow() {
    this.checkUserSession();
    if (userState.isAuthenticated) {
      this.fetchUserStats();
    }
  },
  setup() {
    const stats = reactive({
      totalReminders: 0,
      pendingReminders: 0,
      completedReminders: 0
    });
    const showLogoutConfirmDialog = ref(false);
    const displayAvatarUrl = ref('/static/images/avatar.png');

    // 解析头像URL（处理云文件ID）
    const resolveAvatarUrl = async (sourceUrl) => {
      if (!sourceUrl) {
        return '/static/images/avatar.png';
      }
      
      if (sourceUrl.startsWith('cloud://')) {
        try {
          // #ifdef MP-WEIXIN
          const res = await wx.cloud.getTempFileURL({ fileList: [sourceUrl] });
          if (res.fileList && res.fileList.length > 0 && res.fileList[0].tempFileURL) {
            console.log('我的页面: 云文件ID转换为临时URL成功');
            return res.fileList[0].tempFileURL;
          }
          // #endif
        } catch (error) {
          console.error('我的页面: 获取临时头像链接失败:', error);
          return '/static/images/avatar.png';
        }
      }
      
      return sourceUrl;
    };

    // 监听用户头像变化并解析URL
    watch(() => userState.user?.avatarUrl, async (newAvatarUrl) => {
      if (userState.isAuthenticated && newAvatarUrl) {
        console.log('我的页面: 检测到头像变化:', newAvatarUrl);
        displayAvatarUrl.value = await resolveAvatarUrl(newAvatarUrl);
      } else {
        console.log('我的页面: 使用默认头像');
        displayAvatarUrl.value = '/static/images/avatar.png';
      }
    }, { immediate: true });

    const checkUserSession = async () => {
      try {
        const userInfo = await UserService.getCurrentUser();
        if (userInfo) {
          fetchUserStats();
        } else {
          console.log('用户未登录或token已过期');
        }
      } catch (error) {
        console.log('检查用户会话失败:', error);
      }
    };

    const fetchUserStats = async () => {
      // TODO: 实现API调用获取用户提醒统计
      stats.totalReminders = userState.user?.totalReminders || 12;
      stats.pendingReminders = userState.user?.pendingReminders || 5;
      stats.completedReminders = userState.user?.completedReminders || 7;
    };
    
    const confirmLogout = () => {
      showLogoutConfirmDialog.value = true;
    };

    const handleLogout = () => {
      UserService.logout();
      showLogoutConfirmDialog.value = false;
      uni.showToast({
        title: '已退出登录',
        icon: 'success',
        duration: 1500
      });
      uni.reLaunch({ url: '/pages/login/login' });
    };

    const cancelLogout = () => {
      showLogoutConfirmDialog.value = false;
    };

    const goToLogin = () => {
      uni.navigateTo({ url: '/pages/login/login' });
    };

    const goToUserProfile = () => {
      if(userState.isAuthenticated){
        uni.navigateTo({ url: '/pages/profile/edit' });
      } else {
        goToLogin();
      }
    };
    
    const navTo = (url) => {
      if (!userState.isAuthenticated) {
          uni.showModal({
              title: '请先登录',
              content: '该功能需要登录后才能使用',
              success: (res) => {
                  if (res.confirm) {
                      goToLogin();
                  }
              }
          });
          return;
      }
      uni.navigateTo({ url: url });
    };

    // 头像加载失败处理
    const onAvatarError = (e) => {
      console.log('我的页面: 头像加载失败，使用默认头像');
      // 头像加载失败时，可以设置一个默认头像
      e.target.src = '/static/images/avatar.png';
    };
    
    return {
      userState,
      stats,
      showLogoutConfirmDialog,
      confirmLogout,
      handleLogout,
      cancelLogout,
      goToLogin,
      goToUserProfile,
      navTo,
      checkUserSession,
      fetchUserStats,
      displayAvatarUrl,
      onAvatarError
    };
  }
};
</script>

<style scoped>
.page-container {
  height: 100vh;
  background-color: #fcfbf8;
  display: flex;
  flex-direction: column;
  font-family: 'PingFang SC', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 顶部导航栏 */
.header-section {
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: rgba(252, 251, 248, 0.8);
  backdrop-filter: blur(20rpx);
  border-bottom: none;
}

.nav-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
  padding-bottom: 24rpx;
}

.nav-spacer {
  flex: 1;
}

.title-container {
  flex: 1;
  text-align: center;
}

.page-title {
  font-size: 40rpx;
  font-weight: 600;
  color: #1c170d;
  line-height: 1.2;
}

.nav-actions {
  flex: 1;
  display: flex;
  justify-content: flex-end;
}

.settings-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background-color: transparent;
  transition: background-color 0.2s ease;
}

.settings-btn:active {
  background-color: #f4efe7;
}

.settings-icon {
  font-size: 48rpx;
}

/* 内容区域 */
.content-scroll {
  flex: 1;
  padding: 0;
}

.content-container {
  padding: 24rpx;
  max-width: 960rpx;
  margin: 0 auto;
}

/* 用户信息区域 */
.user-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  margin-bottom: 32rpx;
}

.user-avatar {
  margin-bottom: 20rpx;
}

.avatar-image {
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  border: 3rpx solid #f4efe7;
  box-shadow: 0 6rpx 24rpx rgba(28, 23, 13, 0.1);
  object-fit: cover;
  display: block;
}

.user-info {
  margin-bottom: 24rpx;
}

.username {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #1c170d;
  line-height: 1.2;
  margin-bottom: 12rpx;
}

.user-email,
.user-id {
  display: block;
  font-size: 28rpx;
  color: #9d8148;
  line-height: 1.4;
}

.login-btn {
  background-color: #f7bd4a;
  border-radius: 40rpx;
  padding: 20rpx 40rpx;
  border: none;
  box-shadow: 0 3rpx 12rpx rgba(247, 189, 74, 0.3);
}

.btn-text {
  font-size: 28rpx;
  font-weight: 600;
  color: #1c170d;
}

/* 统计信息区域 */
.stats-section {
  margin-bottom: 32rpx;
}

.section-header {
  margin-bottom: 16rpx;
  padding: 0 8rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
  line-height: 1.2;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 16rpx;
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  padding: 20rpx;
  background-color: #ffffff;
  border: 2rpx solid #f4efe7;
  border-radius: 20rpx;
  box-shadow: 0 3rpx 12rpx rgba(28, 23, 13, 0.05);
}

.stat-number {
  font-size: 40rpx;
  font-weight: 700;
  color: #1c170d;
  line-height: 1.2;
}

.stat-label {
  font-size: 22rpx;
  font-weight: 500;
  color: #666666;
  line-height: 1.4;
}

/* 功能菜单区域 */
.menu-section {
  margin-bottom: 40rpx;
}

.menu-card {
  background-color: #ffffff;
  border: 2rpx solid #f4efe7;
  border-radius: 24rpx;
  box-shadow: 0 3rpx 12rpx rgba(28, 23, 13, 0.05);
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 20rpx 24rpx;
  transition: background-color 0.2s ease;
}

.menu-item:active {
  background-color: #fcfbf8;
}

.menu-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64rpx;
  height: 64rpx;
  background-color: #f4efe7;
  border-radius: 16rpx;
  flex-shrink: 0;
}

.icon-text {
  font-size: 32rpx;
}

.menu-label {
  flex: 1;
  font-size: 28rpx;
  font-weight: 500;
  color: #1c170d;
  line-height: 1.4;
}

.menu-arrow {
  font-size: 40rpx;
  color: #cccccc;
  font-weight: 300;
}

.menu-divider {
  height: 1rpx;
  background-color: #f4efe7;
  margin: 0 24rpx;
}

/* 退出登录区域 */
.logout-section {
  padding: 24rpx 0;
}

.logout-button {
  width: 100%;
  background-color: #1c170d;
  border-radius: 24rpx;
  padding: 20rpx 40rpx;
  border: none;
  box-shadow: 0 3rpx 12rpx rgba(28, 23, 13, 0.2);
  transition: all 0.2s ease;
}

.logout-button:active {
  background-color: #2d2419;
  transform: translateY(2rpx);
}

.logout-text {
  font-size: 28rpx;
  font-weight: 600;
  color: #ffffff;
  text-align: center;
}

/* 底部间距 */
.bottom-spacer {
  height: 24rpx;
}

/* 响应式调整 */
@media (max-width: 750rpx) {
  .content-container {
    padding: 24rpx;
  }
  
  .stats-grid {
    gap: 16rpx;
  }
  
  .stat-card {
    padding: 20rpx;
  }
  
  .stat-number {
    font-size: 40rpx;
  }
  
  .stat-label {
    font-size: 22rpx;
  }
}
</style> 