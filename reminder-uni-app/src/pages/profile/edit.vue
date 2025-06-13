<template>
  <view class="container">
    <view class="header-section">
      <view class="nav-container">
        <view class="nav-back" @click="goBack">
          <text class="back-icon">‹</text>
        </view>
        <view class="title-container">
          <text class="page-title">编辑个人资料</text>
        </view>
        <view class="nav-spacer"></view>
      </view>
    </view>
    
    <view class="content-container">
      <view v-if="isLoading" class="loading-container">
        <view class="loading-spinner"></view>
        <text class="loading-text">加载用户信息中...</text>
      </view>
      <view v-else class="editor-wrapper">
        <UserInfoEditor
          :initialUserInfo="initialUserInfo"
          :promptMessage="promptMessage"
          :required="false"
          @success="onUpdateSuccess"
          @cancel="onUpdateCancel"
        />
      </view>
    </view>
  </view>
</template>

<script>
import { ref, onMounted, watch } from 'vue';
import { UserService, userState } from '../../services/userService';
import UserInfoEditor from '../../components/UserInfoEditor.vue';

export default {
  components: {
    UserInfoEditor
  },
  
  setup() {
    const initialUserInfo = ref({});
    const isLoading = ref(true);
    const promptMessage = ref('');

    // 计算初始用户信息
    const prepareInitialUserInfo = (userData) => {
      if (userData) {
        const isWechatVirtualEmail = (email) => {
          return email && email.includes('@wechat.local');
        };
        
        const userInfo = {
          nickname: userData.nickname || userData.username || '',
          avatarUrl: userData.avatarUrl || userData.avatar || '',
          email: isWechatVirtualEmail(userData.email) ? '' : (userData.email || ''),
          phone: userData.phone || userData.phoneNumber || ''
        };
        initialUserInfo.value = userInfo;
        
        // --- 修改：根据缺失信息生成更具体的提示 ---
        const missingInfo = [];
        if (!userInfo.email) {
          missingInfo.push('邮箱');
        }
        if (!userInfo.phone) {
          missingInfo.push('手机号');
        }

        if (missingInfo.length === 2) {
          // 两者都缺失
          promptMessage.value = '建议补充手机号和邮箱，确保能通过这两个重要渠道接收服务通知。';
        } else if (missingInfo.length === 1) {
          if (missingInfo[0] === '邮箱') {
            // 只缺失邮箱
            promptMessage.value = '补充邮箱后，您将能通过邮件接收订单回执和服务通知。';
          } else {
            // 只缺失手机号
            promptMessage.value = '补充手机号后，您将能通过短信接收紧急安全提醒或登录验证。';
          }
        } else {
          // 信息完整
          promptMessage.value = '';
        }
        // --- 结束修改 ---

        console.log('编辑资料页面: 生成的提示信息:', promptMessage.value);
        return userInfo;
      }
      return {};
    };

    // 获取用户信息 (此部分无变化)
    const loadUserInfo = async () => {
      try {
        isLoading.value = true;
        const userInfo = await UserService.getCurrentUser();
        if (userInfo) {
          console.log('编辑资料页面: 获取用户信息成功');
          prepareInitialUserInfo(userInfo);
        } else {
          uni.showModal({
            title: '提示',
            content: '请先登录',
            showCancel: false,
            success: () => {
              uni.reLaunch({ url: '/pages/login/login' });
            }
          });
          return;
        }
      } catch (error) {
        console.error('编辑资料页面: 获取用户信息失败:', error);
        uni.showToast({
          title: '获取用户信息失败',
          icon: 'none'
        });
      } finally {
        isLoading.value = false;
      }
    };

    // 监听用户状态变化 (此部分无变化)
    watch(() => userState.user, (newUser) => {
      if (newUser && newUser.id) {
        console.log('编辑资料页面: 检测到用户状态变化:', newUser);
        prepareInitialUserInfo(newUser);
      }
    }, { deep: true, immediate: true });

    onMounted(() => {
      loadUserInfo();
    });

    // 返回按钮处理 (此部分无变化)
    const goBack = () => {
      const pages = getCurrentPages();
      if (pages.length <= 1) {
        uni.reLaunch({ url: '/pages/mine/mine' });
      } else {
        uni.navigateBack();
      }
    };

    // 更新成功处理
    const onUpdateSuccess = async (data) => {
      console.log('编辑资料页面: 用户信息更新成功:', data);
      
      try {
        // 重新获取用户资料，确保本地状态与服务器同步
        console.log('🔄 重新获取用户资料...');
        const success = await UserService.refreshUserProfile();
        
        if (success) {
          console.log('✅ 用户资料已更新');
        } else {
          console.warn('⚠️ 重新获取用户资料失败，使用返回的数据更新本地状态');
          // 如果重新获取失败，使用返回的数据更新本地状态
          if (data.userInfo) {
            Object.assign(userState.user, data.userInfo);
            console.log('本地用户状态已更新');
          }
        }
      } catch (error) {
        console.error('❌ 重新获取用户资料出错:', error);
        // 如果重新获取失败，使用返回的数据更新本地状态
        if (data.userInfo) {
          Object.assign(userState.user, data.userInfo);
          console.log('本地用户状态已更新（降级处理）');
        }
      }
      
      setTimeout(() => {
        goBack();
      }, 1000);
    };

    // 取消处理
    const onUpdateCancel = () => {
      console.log('编辑资料页面: 用户取消编辑资料');
      goBack();
    };

    return {
      initialUserInfo,
      isLoading,
      promptMessage,
      goBack,
      onUpdateSuccess,
      onUpdateCancel
    };
  }
};
</script>

<style scoped>
.container {
  height: 100vh;
  background-color: #fcfbf8;
  display: flex;
  flex-direction: column;
  font-family: 'Manrope', 'Noto Sans', sans-serif;
}

.header-section {
  background-color: #fcfbf8;
  padding: 32rpx 32rpx 16rpx;
  flex-shrink: 0;
}

.nav-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 96rpx;
}

.nav-back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 96rpx;
  height: 96rpx;
  cursor: pointer;
}

.back-icon {
  font-size: 48rpx;
  color: #1c170d;
  font-weight: 600;
}

.title-container {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.page-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #1c170d;
  text-align: center;
  line-height: 1.2;
  letter-spacing: -0.015em;
}

.nav-spacer {
  width: 96rpx;
  height: 96rpx;
}

.content-container {
  flex: 1;
  overflow-y: auto;
  background-color: #fcfbf8;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 32rpx;
}

.loading-spinner {
  width: 60rpx;
  height: 60rpx;
  border: 4rpx solid #f4efe7;
  border-top: 4rpx solid #f7bd4a;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 24rpx;
}

.loading-text {
  font-size: 28rpx;
  color: #9d8148;
  text-align: center;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.editor-wrapper {
  padding: 16rpx 32rpx 32rpx;
}

@media (max-width: 750rpx) {
  .header-section {
    padding: 24rpx 24rpx 12rpx;
  }
  
  .nav-container {
    height: 80rpx;
  }
  
  .nav-back,
  .nav-spacer {
    width: 80rpx;
    height: 80rpx;
  }
  
  .back-icon {
    font-size: 40rpx;
  }
  
  .page-title {
    font-size: 32rpx;
  }
  
  .editor-wrapper {
    padding: 12rpx 24rpx 24rpx;
  }
}
</style>