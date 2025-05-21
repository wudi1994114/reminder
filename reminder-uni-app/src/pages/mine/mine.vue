<template>
  <view class="container">
    <view class="user-card">
      <view class="user-info" @click="goToUserProfile">
        <image class="avatar" :src="userState.user?.avatarUrl || '/static/images/avatar.png'"></image>
        <view class="user-details">
          <text class="username">{{ userState.user?.nickname || userState.user?.username || '未登录' }}</text>
          <text class="user-id" v-if="userState.user?.id">ID: {{ userState.user?.id }}</text>
        </view>
      </view>
      <button class="login-btn" v-if="!userState.isAuthenticated" @click="goToLogin">登录/注册</button>
    </view>
    
    <view class="stat-card" v-if="userState.isAuthenticated">
      <view class="stat-item">
        <text class="stat-num">{{ stats.totalReminders || 0 }}</text>
        <text class="stat-label">总提醒</text>
      </view>
      <view class="divider"></view>
      <view class="stat-item">
        <text class="stat-num">{{ stats.pendingReminders || 0 }}</text>
        <text class="stat-label">待处理</text>
      </view>
      <view class="divider"></view>
      <view class="stat-item">
        <text class="stat-num">{{ stats.completedReminders || 0 }}</text>
        <text class="stat-label">已完成</text>
      </view>
    </view>
    
    <view class="menu-list">
      <view class="menu-item" @click="navTo('/pages/profile/edit')">
        <text class="menu-icon">✏️</text>
        <text class="menu-label">编辑个人资料</text>
        <text class="menu-arrow">></text>
      </view>
      <view class="menu-item" @click="navTo('/pages/settings/notification')">
        <text class="menu-icon">🔔</text>
        <text class="menu-label">提醒设置</text>
        <text class="menu-arrow">></text>
      </view>
      <!-- <view class="menu-item" @click="navTo('/pages/settings/theme')">
        <text class="menu-icon">🎨</text>
        <text class="menu-label">主题设置</text>
        <text class="menu-arrow">></text>
      </view> -->
      <view class="menu-item" @click="navTo('/pages/settings/about')">
        <text class="menu-icon">📱</text>
        <text class="menu-label">关于应用</text>
        <text class="menu-arrow">></text>
      </view>
    </view>
    
    <view class="bottom-btn-area" v-if="userState.isAuthenticated">
      <button class="logout-btn" @click="confirmLogout">退出登录</button>
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

<style>
.container {
  padding: 30rpx;
  min-height: 100vh;
}

.user-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #ffffff;
  border-radius: 10rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}

.user-info {
  display: flex;
  align-items: center;
  flex: 1; /* 让用户信息区域占据更多空间 */
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  margin-right: 30rpx; /* 增大间距 */
  background-color: #f0f0f0;
  border: 1px solid #eee;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.username {
  font-size: 36rpx;
  font-weight: bold;
  margin-bottom: 10rpx;
  color: #333;
}

.user-id {
  font-size: 26rpx; /* 稍小一点 */
  color: #999999;
}

.login-btn {
  padding: 12rpx 30rpx;
  background-color: #3cc51f;
  color: #ffffff;
  font-size: 28rpx;
  border-radius: 8rpx;
  margin: 0; /* uni-app中button有默认margin */
  line-height: normal; /* 保证文字垂直居中 */
}

.stat-card {
  display: flex;
  justify-content: space-around;
  align-items: center;
  background-color: #ffffff;
  border-radius: 10rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-num {
  font-size: 40rpx;
  font-weight: bold;
  color: #3cc51f;
  margin-bottom: 10rpx;
}

.stat-label {
  font-size: 28rpx;
  color: #666666;
}

.divider {
  width: 1px;
  height: 80rpx;
  background-color: #eeeeee;
}

.menu-list {
  background-color: #ffffff;
  border-radius: 10rpx;
  padding: 10rpx 0;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
  margin-bottom: 60rpx;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 30rpx;
  border-bottom: 1px solid #f5f5f5;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-icon {
  margin-right: 20rpx;
  font-size: 40rpx;
}

.menu-label {
  flex: 1;
  font-size: 32rpx;
}

.menu-arrow {
  color: #cccccc;
  font-size: 32rpx;
}

.bottom-btn-area {
  /* margin-top: 60rpx; 由menu-list的margin-bottom控制 */
}

.logout-btn {
  width: 100%;
  height: 88rpx; /* 适配标准按钮高度 */
  line-height: 88rpx;
  background-color: #f56c6c;
  color: #ffffff;
  font-size: 32rpx;
  border-radius: 8rpx;
}
</style> 