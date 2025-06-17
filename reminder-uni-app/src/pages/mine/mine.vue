<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="header-section">
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
            <text class="user-email" v-if="FeatureControl.showEmailFeatures() && userState.user?.email">{{ userState.user?.email }}</text>
            <text class="user-id" v-else-if="userState.user?.id">ID: {{ userState.user?.id }}</text>
          </view>
          <button 
            class="wechat-login-btn" 
            v-if="!userState.isAuthenticated" 
            open-type="getUserInfo"
            @getuserinfo="handleWechatLogin"
          >
            <text class="wechat-login-text">微信一键登录</text>
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
            <view class="menu-item" @click="navToAbout">
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
    
    <!-- 全局登录弹窗 -->
    <GlobalLoginModal />
  </view>
</template>

<script>
import { ref, reactive, computed, watch, onUnmounted } from 'vue';
import { UserService, userState } from '@/services/userService';
import { requireAuth, logout, checkAuthAndClearData, showOneClickLogin } from '@/utils/auth';
import { wechatLogin } from '@/services/api';
import { FeatureControl } from '@/config/version';
import GlobalLoginModal from '@/components/GlobalLoginModal.vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';

export default {
  components: {
    GlobalLoginModal,
    ConfirmDialog
  },
  onShow() {
    console.log('个人中心页面显示，检查登录状态');
    
    // 检查登录状态并清空数据
    if (!checkAuthAndClearData('个人中心页面-onShow')) {
      return;
    }
    
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
    const displayAvatarUrl = ref('https://thirdwx.qlogo.cn/mmopen/vi_32/POgEwh4mIHO4nibH0KlMECNjjGxQUq24ZEaGT4poC6icRiccVGKSyXwibcPq4BWmiaIGuG1icwxaQX6grC9VemZoJ8rg/132');

    // 解析头像URL（处理云文件ID）
    const resolveAvatarUrl = async (sourceUrl) => {
      if (!sourceUrl) {
        return 'https://thirdwx.qlogo.cn/mmopen/vi_32/POgEwh4mIHO4nibH0KlMECNjjGxQUq24ZEaGT4poC6icRiccVGKSyXwibcPq4BWmiaIGuG1icwxaQX6grC9VemZoJ8rg/132';
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
          return 'https://thirdwx.qlogo.cn/mmopen/vi_32/POgEwh4mIHO4nibH0KlMECNjjGxQUq24ZEaGT4poC6icRiccVGKSyXwibcPq4BWmiaIGuG1icwxaQX6grC9VemZoJ8rg/132';
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
        displayAvatarUrl.value = 'https://thirdwx.qlogo.cn/mmopen/vi_32/POgEwh4mIHO4nibH0KlMECNjjGxQUq24ZEaGT4poC6icRiccVGKSyXwibcPq4BWmiaIGuG1icwxaQX6grC9VemZoJ8rg/132';
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
      logout();
      showLogoutConfirmDialog.value = false;
    };

    const cancelLogout = () => {
      showLogoutConfirmDialog.value = false;
    };

    const handleWechatLogin = async (e) => {
      console.log('个人中心页面：微信一键登录触发', e);
      
      try {
        // 显示加载提示
        uni.showLoading({
          title: '登录中...',
          mask: true
        });
        
        // 获取用户信息
        const userInfo = e.detail.userInfo;
        if (!userInfo) {
          uni.hideLoading();
          uni.showToast({
            title: '登录已取消',
            icon: 'none'
          });
          return;
        }
        
        console.log('个人中心：获取到用户信息:', userInfo);
        
        // 调用微信登录获取code
        const loginRes = await new Promise((resolve, reject) => {
          uni.login({
            provider: 'weixin',
            success: resolve,
            fail: reject
          });
        });
        
        console.log('个人中心：微信登录成功:', loginRes);
        
        // 构建登录请求数据
        const loginData = {
          code: loginRes.code
        };
        
        console.log('🔐 个人中心：发送登录数据到后端:', loginData);
        
        // 调用真正的微信登录API
        const response = await wechatLogin(loginData);
        
        console.log('✅ 个人中心：微信登录API响应:', response);
        
        if (response && response.accessToken) {
          // 使用UserService处理登录成功
          await UserService.onLoginSuccess(response, 'wechat');
          
          console.log('✅ 个人中心：登录处理完成，用户状态已更新');
          
          uni.hideLoading();
          uni.showToast({
            title: '登录成功',
            icon: 'success'
          });
          
          // 刷新页面数据
          setTimeout(() => {
            checkUserSession();
            if (userState.isAuthenticated) {
              fetchUserStats();
            }
            
            // 发送全局事件，通知其他页面刷新数据
            uni.$emit('userLoginSuccess');
          }, 1000);
        } else {
          throw new Error('登录响应格式错误');
        }
        
      } catch (error) {
        console.error('个人中心：微信登录失败:', error);
        uni.hideLoading();
        
        let errorMessage = '登录失败';
        if (error.message) {
          if (error.message.includes('用户拒绝')) {
            errorMessage = '用户取消了授权';
          } else if (error.message.includes('网络')) {
            errorMessage = '网络连接失败';
          } else {
            errorMessage = error.message;
          }
        }
        
        uni.showToast({
          title: errorMessage,
          icon: 'none'
        });
      }
    };

    const goToUserProfile = async () => {
      const isAuthenticated = await requireAuth();
      
      if (isAuthenticated) {
        uni.navigateTo({
          url: '/pages/profile/edit'
        });
      }
    };

    const navTo = async (url) => {
      const isAuthenticated = await requireAuth();
      
      if (isAuthenticated) {
        uni.navigateTo({ url });
      }
    };

    const navToAbout = () => {
      // 关于应用不需要登录验证，直接跳转
      uni.navigateTo({ url: '/pages/settings/about' });
    };

    const onAvatarError = () => {
      console.log('我的页面: 头像加载失败，使用默认头像');
      displayAvatarUrl.value = 'https://thirdwx.qlogo.cn/mmopen/vi_32/POgEwh4mIHO4nibH0KlMECNjjGxQUq24ZEaGT4poC6icRiccVGKSyXwibcPq4BWmiaIGuG1icwxaQX6grC9VemZoJ8rg/132';
    };
    
    // 监听用户登出事件，清理Mine页面数据
    uni.$on('userLogout', () => {
      console.log('Mine页面：收到用户登出事件，清理所有数据');
      
      // 重置统计数据
      stats.totalReminders = 0;
      stats.pendingReminders = 0;
      stats.completedReminders = 0;
      
      // 重置头像为默认头像
      displayAvatarUrl.value = 'https://thirdwx.qlogo.cn/mmopen/vi_32/POgEwh4mIHO4nibH0KlMECNjjGxQUq24ZEaGT4poC6icRiccVGKSyXwibcPq4BWmiaIGuG1icwxaQX6grC9VemZoJ8rg/132';
      
      // 关闭任何打开的对话框
      showLogoutConfirmDialog.value = false;
      
      console.log('✅ Mine页面：数据清理完成');
    });
    
    // 组件销毁时清理事件监听器
    onUnmounted(() => {
      uni.$off('userLogout');
    });

    return {
      userState,
      stats,
      showLogoutConfirmDialog,
      displayAvatarUrl,
      FeatureControl,
      checkUserSession,
      fetchUserStats,
      confirmLogout,
      handleLogout,
      cancelLogout,
      handleWechatLogin,
      goToUserProfile,
      navTo,
      navToAbout,
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

.wechat-login-btn {
  background-color: #f7bd4a;
  border-radius: 40rpx;
  padding: 20rpx 40rpx;
  border: none;
  box-shadow: 0 3rpx 12rpx rgba(247, 189, 74, 0.3);
}

.wechat-login-text {
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