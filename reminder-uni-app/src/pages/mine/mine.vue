<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="header-section">
      <view class="nav-container">
        <view class="title-container">
          <text class="page-title">我的</text>
        </view>
      </view>
    </view>
    
    <!-- 内容区域 -->
    <scroll-view class="content-scroll" scroll-y>
      <view class="content-container">
        <!-- 用户信息区域 -->
        <view class="user-section">
          <view class="user-info" @click="goToUserProfile">
            <image class="avatar" :src="userState.user?.avatarUrl || '/static/images/avatar.png'"></image>
            <view class="user-details">
              <text class="username">{{ userState.user?.nickname || userState.user?.username || '未登录' }}</text>
              <text class="user-id" v-if="userState.user?.id">ID: {{ userState.user?.id }}</text>
            </view>
          </view>
          <button class="login-btn" v-if="!userState.isAuthenticated" @click="goToLogin">
            <text class="btn-text">登录/注册</text>
          </button>
        </view>
        
        <!-- 统计信息 -->
        <view class="stats-section" v-if="userState.isAuthenticated">
          <text class="section-title">数据统计</text>
          <view class="stats-details">
            <view class="stat-item">
              <text class="stat-label">总提醒</text>
              <text class="stat-value">{{ stats.totalReminders || 0 }}</text>
            </view>
            <view class="stat-item">
              <text class="stat-label">待处理</text>
              <text class="stat-value">{{ stats.pendingReminders || 0 }}</text>
            </view>
            <view class="stat-item">
              <text class="stat-label">已完成</text>
              <text class="stat-value">{{ stats.completedReminders || 0 }}</text>
            </view>
          </view>
        </view>
        
        <!-- 功能菜单 -->
        <view class="menu-section">
          <text class="section-title">功能设置</text>
          <view class="menu-details">
            <view class="menu-item" @click="navTo('/pages/profile/edit')">
              <text class="menu-label">编辑个人资料</text>
            </view>
            <view class="menu-item" @click="navTo('/pages/settings/notification')">
              <text class="menu-label">提醒设置</text>
            </view>
            <view class="menu-item" @click="navTo('/pages/settings/about')">
              <text class="menu-label">关于应用</text>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>
    
    <!-- 底部按钮 -->
    <view class="bottom-actions" v-if="userState.isAuthenticated">
      <button class="action-button logout-btn" @click="confirmLogout">
        <text class="button-text">退出登录</text>
      </button>
    </view>

    <ConfirmDialog
      :show="showLogoutConfirmDialog"
      title="确认退出"
      message="确定要退出当前账号吗？"
      @confirm="handleLogout"
      @cancel="cancelLogout"
    />
  </view>
</template>

<script>
import { ref, reactive, onMounted } from 'vue';
import { userState, clearUserInfo, saveUserInfo } from '../../services/store';
import { getUserProfile } from '../../services/api'; // 假设有获取用户统计的API
import ConfirmDialog from '../../components/ConfirmDialog.vue';

export default {
  components: {
    ConfirmDialog
  },
  onShow() { // 每次进入页面时检查登录状态和用户信息
    this.checkUserSession();
    if (userState.isAuthenticated) {
      this.fetchUserStats(); // 如果已登录，同时获取统计信息
    }
  },
  setup() {
    const stats = reactive({
      totalReminders: 0,
      pendingReminders: 0,
      completedReminders: 0
    });
    const showLogoutConfirmDialog = ref(false);

    const checkUserSession = async () => {
      const token = uni.getStorageSync('accessToken');
      if (token && !userState.user) { // 有token但store中无用户信息，尝试获取
        try {
          const profile = await getUserProfile();
          if (profile) {
            saveUserInfo(profile);
            fetchUserStats(); // 获取用户信息后获取统计
          }
        } catch (error) {
          console.log('获取用户信息失败，可能token已过期', error);
          clearUserInfo(); // 清除无效的登录状态
        }
      } else if (!token) {
        clearUserInfo();
      }
    };

    const fetchUserStats = async () => {
      // TODO: 实现API调用获取用户提醒统计
      // 示例：const userStats = await getUserReminderStats();
      // if(userStats) { Object.assign(stats, userStats); }
      // 暂时使用模拟数据
      stats.totalReminders = userState.user?.totalReminders || 12;
      stats.pendingReminders = userState.user?.pendingReminders || 5;
      stats.completedReminders = userState.user?.completedReminders || 7;
    };
    
    const confirmLogout = () => {
      showLogoutConfirmDialog.value = true;
    };

    const handleLogout = () => {
      clearUserInfo();
      showLogoutConfirmDialog.value = false;
      uni.showToast({
        title: '已退出登录',
        icon: 'success',
        duration: 1500
      });
      // 可以选择跳转到登录页或首页
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
        // TODO: 跳转到用户资料编辑页
        // uni.showToast({ title: '资料编辑页开发中', icon: 'none' });
        uni.navigateTo({ url: '/pages/profile/edit' });
      } else {
        goToLogin();
      }
    };
    
    const navTo = (url) => {
      if (!userState.isAuthenticated) { // 对所有navTo的目标都进行登录检查
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
      // 实际项目中应有真实的页面路径
      // uni.showToast({
      //   title: '功能开发中',
      //   icon: 'none',
      //   duration: 1500
      // });
      uni.navigateTo({ url: url });
    };
    
    return {
      userState, // 将整个userState暴露给模板
      stats,
      showLogoutConfirmDialog,
      confirmLogout,
      handleLogout,
      cancelLogout,
      goToLogin,
      goToUserProfile,
      navTo,
      checkUserSession, // 暴露给onShow
      fetchUserStats // 暴露给onShow
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

/* 顶部导航区域 */
.header-section {
  padding: 32rpx;
  background-color: #fcfbf8;
  border-bottom: none;
}

.nav-container {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.title-container {
  flex: 1;
}

.page-title {
  font-size: 48rpx;
  font-weight: 700;
  color: #1c170d;
  line-height: 1.2;
}

/* 内容区域 */
.content-scroll {
  flex: 1;
  background-color: #fcfbf8;
  padding-bottom: 120rpx; /* 为底部按钮留出空间 */
}

.content-container {
  padding: 0 32rpx 32rpx;
  max-width: 960rpx;
  margin: 0 auto;
}

/* 用户信息区域 */
.user-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 8rpx;
  margin-bottom: 24rpx;
}

.user-info {
  display: flex;
  align-items: center;
  flex: 1;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  margin-right: 24rpx;
  background-color: #f0f0f0;
  border: 2rpx solid #e9e0ce;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.username {
  font-size: 36rpx;
  font-weight: 700;
  color: #1c170d;
  line-height: 1.3;
}

.user-id {
  font-size: 26rpx;
  color: #9d8148;
  font-weight: 400;
}

.login-btn {
  height: 72rpx;
  padding: 0 24rpx;
  background-color: #f7bd4a;
  color: #1c170d;
  border-radius: 16rpx;
  border: none;
  font-size: 28rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-text {
  font-size: 28rpx;
  font-weight: 600;
}

/* 统计信息区域 */
.stats-section {
  padding: 32rpx 8rpx;
  margin-top: 24rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
  margin-bottom: 16rpx;
}

.stats-details {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
}

.stat-label {
  font-size: 30rpx;
  color: #1c170d;
  font-weight: 500;
}

.stat-value {
  font-size: 30rpx;
  color: #f7bd4a;
  font-weight: 600;
}

/* 功能菜单区域 */
.menu-section {
  padding: 32rpx 8rpx;
  margin-top: 24rpx;
}

.menu-details {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  cursor: pointer;
}

.menu-item:active {
  background-color: #f4efe7;
  margin: 0 -8rpx;
  padding: 16rpx 8rpx;
  border-radius: 8rpx;
}

.menu-label {
  font-size: 30rpx;
  color: #1c170d;
  font-weight: 500;
}

/* 底部按钮区域 */
.bottom-actions {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: #fcfbf8;
  padding: 24rpx 32rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  display: flex;
  gap: 24rpx;
}

.action-button {
  flex: 1;
  height: 88rpx;
  border-radius: 16rpx;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.logout-btn {
  background-color: #f56c6c;
  color: #ffffff;
}

.button-text {
  font-size: 32rpx;
  font-weight: 600;
}

/* 响应式调整 */
@media (max-width: 750rpx) {
  .content-container {
    padding: 0 24rpx 24rpx;
  }
  
  .user-section {
    padding: 16rpx 8rpx;
  }
  
  .stats-section, .menu-section {
    padding: 24rpx 8rpx;
  }
  
  .stat-item, .menu-item {
    padding: 12rpx 0;
  }
  
  .stat-label, .stat-value, .menu-label {
    font-size: 28rpx;
  }
  
  .section-title {
    font-size: 30rpx;
  }
  
  .username {
    font-size: 32rpx;
  }
  
  .user-id {
    font-size: 24rpx;
  }
  
  .bottom-actions {
    padding: 20rpx 24rpx;
    padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
    gap: 20rpx;
  }
  
  .action-button {
    height: 80rpx;
  }
  
  .button-text {
    font-size: 30rpx;
  }
}
</style> 