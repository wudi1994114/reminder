<template>
  <view class="page-container">
    <!-- 顶部标题区域 -->
    <view class="header-section">
      <view class="title-container">
        <text class="page-title">我的提醒</text>
      </view>
      <view class="action-buttons">
        <button class="action-btn primary-btn" @click="handleCreateNew">
          <text class="btn-text">{{ createButtonText }}</text>
        </button>
      </view>
    </view>
    
    <!-- 提醒类型切换标签 -->
    <view class="tab-container">
      <view class="tab-buttons">
        <view 
          class="tab-button" 
          :class="{ active: activeTab === 'simple' }"
          @click="switchTab('simple')"
        >
          <text class="tab-text">简单提醒</text>
        </view>
        <view 
          class="tab-button" 
          :class="{ active: activeTab === 'complex' }"
          @click="switchTab('complex')"
        >
          <text class="tab-text">复杂提醒</text>
        </view>
      </view>
    </view>
    
    <!-- 内容区域 -->
    <scroll-view class="content-scroll" scroll-y>
      <view class="content-container">
        <!-- 加载状态 -->
        <view v-if="loading" class="loading-state">
          <view class="loading-content">
            <text class="loading-text">加载中...</text>
          </view>
        </view>
        
        <!-- 简单提醒列表 -->
        <view v-else-if="activeTab === 'simple'">
        <!-- 空状态 -->
          <view v-if="simpleReminders && simpleReminders.length === 0" class="empty-state">
          <view class="empty-content">
            <text class="empty-icon">📝</text>
              <text class="empty-title">暂无简单提醒</text>
              <text class="empty-desc">点击下方"新建简单提醒"开始添加你的提醒</text>
          </view>
         </view>
         
          <!-- 简单提醒列表 -->
        <view v-else class="reminder-list">
          <SimpleReminderCard
            v-for="item in simpleReminders" 
            :key="item.id" 
            :reminder="item"
            @click="goToDetail"
          />
        </view>
       </view>
        
        <!-- 复杂提醒列表 -->
        <view v-else-if="activeTab === 'complex'">
          <!-- 空状态 -->
          <view v-if="complexReminders && complexReminders.length === 0" class="empty-state">
            <view class="empty-content">
              <text class="empty-icon">⚙️</text>
              <text class="empty-title">暂无复杂提醒</text>
              <text class="empty-desc">点击下方"新建复杂提醒"开始添加你的复杂提醒</text>
            </view>
          </view>
          
          <!-- 复杂提醒列表 -->
          <view v-else class="reminder-list">
            <ComplexReminderCard
              v-for="item in complexReminders" 
              :key="item.id" 
              :reminder="item"
              @edit="editComplexReminder"
              @delete="deleteComplexReminder"
            />
          </view>
        </view>
      </view>
      
      <!-- 底部间距，为固定按钮留出空间 -->
      <view class="bottom-spacer"></view>
    </scroll-view>
    
    <!-- 登录弹窗 -->
    <view v-if="showLoginPopup" class="login-modal-overlay" @click="closeLoginModal">
      <view class="login-modal" @click.stop>
        <view class="login-modal-header">
          <text class="login-modal-title">登录</text>
          <view class="login-modal-close" @click="closeLoginModal">
            <text class="close-icon">×</text>
          </view>
        </view>
        <view class="login-modal-content">
          <text class="login-desc">使用微信账号快速登录</text>
          <button 
            class="wechat-login-button" 
            @click="handleWechatLogin"
          >
            <text class="wechat-login-text">微信一键登录</text>
          </button>
        </view>
      </view>
    </view>
    
    <!-- 全局登录弹窗 -->
    <GlobalLoginModal />
  </view>
</template>

<script>
import { ref, computed, onUnmounted } from 'vue';
import { getUpcomingReminders, getAllComplexReminders, deleteComplexReminder as deleteComplexReminderApi } from '@/services/cachedApi';
import { smartWechatLogin } from '@/api/wechat';
import ReminderCacheService, { globalDataVersion, updateDataVersion } from '@/services/reminderCache';
import { isAuthenticated, clearAllUserData, requireAuth } from '@/utils/auth';
import { usePageDataSync, checkDataSyncOnShow, createSmartDataLoader } from '@/utils/dataSync';
import GlobalLoginModal from '@/components/GlobalLoginModal.vue';
import SimpleReminderCard from '@/components/SimpleReminderCard.vue';
import ComplexReminderCard from '@/components/ComplexReminderCard.vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';

export default {
  name: 'IndexPage',
  components: {
    SimpleReminderCard,
    ComplexReminderCard,
    GlobalLoginModal,
    ConfirmDialog
  },
  onLoad() {
    console.log('🚀 Index页面: onLoad 触发，页面首次加载');
    // 页面首次加载时的初始化逻辑
    if (this.handlePageLoad) {
      this.handlePageLoad();
    }
  },
  
  onTabItemTap() {
    // 标签页切换时的逻辑，暂时移除直接调用
    console.log('标签页被点击');
  },
  
  onShow() {
    console.log('📱 Index页面: onShow 触发');
    if (this.handlePageShow) {
      this.handlePageShow();
    }
  },
  
  onShareAppMessage(res) {
    return {
      title: '快来使用提醒助手，再也不会忘记重要事情！',
      path: '/pages/index/index?from=share',
      // imageUrl: '自定义分享图片路径' 
    };
  },

  onShareTimeline() {
    return {
      title: '我发现一个超好用的提醒助手，推荐给你！',
      query: 'from=timeline',
      // imageUrl: '自定义分享图片路径'
    };
  },
  
  setup() {
    // 响应式数据
    const activeTab = ref('simple');
    const simpleReminders = ref([]);
    const complexReminders = ref([]);
    const isLoading = ref(false);
    const refreshing = ref(false);
    const showLoginPopup = ref(false);
    const newUserInfo = ref({});
    const localDataVersion = ref(0); // 本地数据版本

    // 页面数据清理函数
    const clearPageData = () => {
      console.log('🧹 Index页面：清理所有数据');
      simpleReminders.value = [];
      complexReminders.value = [];
      isLoading.value = false;
      refreshing.value = false;
      activeTab.value = 'simple';
    };

    // 注册页面数据同步
    const unregisterDataSync = usePageDataSync('IndexPage', clearPageData);
    
    // 计算属性
    const currentReminders = computed(() => {
      return activeTab.value === 'simple' ? simpleReminders.value : complexReminders.value;
    });
    
    const hasNoData = computed(() => {
      return !isLoading.value && currentReminders.value.length === 0;
    });
    
    const emptyStateText = computed(() => {
      return activeTab.value === 'simple' ? '暂无简单提醒' : '暂无复杂提醒';
    });
    
    const emptyStateDesc = computed(() => {
      if (activeTab.value === 'simple') {
        return '点击右上角"+"添加你的第一个简单提醒吧！';
      } else {
        return '点击右上角"+"创建你的第一个复杂提醒吧！';
      }
    });

    // 创建按钮文字
    const createButtonText = computed(() => {
      return activeTab.value === 'simple' ? '新建简单提醒' : '新建复杂提醒';
    });

    // loading别名，方便模板使用
    const loading = computed(() => isLoading.value);

    // 标签页切换
    const switchTab = (tab) => {
      console.log(`%c[Index页面] 标签切换触发`, 'color: #9C27B0; font-weight: bold;');
      console.log(`%c[Index页面] 当前标签: ${activeTab.value} -> 目标标签: ${tab}`, 'color: #9C27B0;');
      
      if (tab !== activeTab.value) {
        activeTab.value = tab;
        console.log(`%c[Index页面] 标签已切换到: ${tab}，开始加载对应数据`, 'color: #9C27B0;');
        loadCurrentTabData();
      } else {
        console.log(`%c[Index页面] 标签未变化，无需重新加载`, 'color: #9C27B0;');
      }
    };

    // 创建智能数据加载器
    const smartLoadCurrentTabData = createSmartDataLoader(
      async () => {
        console.log(`%c[Index页面] 智能数据加载器启动`, 'color: #607D8B; font-weight: bold;');
        console.log(`%c[Index页面] 当前标签: ${activeTab.value}`, 'color: #607D8B;');
        
        if (activeTab.value === 'simple') {
          console.log(`%c[Index页面] 开始加载简单提醒数据`, 'color: #607D8B;');
          await loadSimpleReminders();
        } else {
          console.log(`%c[Index页面] 开始加载复杂提醒数据`, 'color: #607D8B;');
          await loadComplexReminders();
        }
        
        // 数据加载成功后，同步版本号
        const oldLocalVersion = localDataVersion.value;
        localDataVersion.value = globalDataVersion.value;
        console.log(`%c[Index页面] 数据加载完成，版本同步`, 'color: #4CAF50; font-weight: bold;');
        console.log(`%c[Index页面] 本地版本更新: ${oldLocalVersion} -> ${localDataVersion.value}`, 'color: #4CAF50;');
      },
      isLoading
    );

    // 加载当前标签页数据
    const loadCurrentTabData = async () => {
      if (!isAuthenticated()) {
        console.log('用户未认证，跳过加载');
        return;
      }
      await smartLoadCurrentTabData();
    };

    // 加载简单提醒
    const loadSimpleReminders = async () => {
      if (isLoading.value) {
        console.log(`%c[Index页面] 简单提醒正在加载中，跳过重复请求`, 'color: #FF9800;');
        return;
      }
      
      try {
        isLoading.value = true;
        console.log('%c[Index页面] 开始加载简单提醒...', 'color: #2196F3; font-weight: bold;');
        
        const response = await getUpcomingReminders();
        console.log('%c[Index页面] 简单提醒API响应:', 'color: #2196F3;', response);
        
        if (response && Array.isArray(response)) {
          const oldCount = simpleReminders.value.length;
          simpleReminders.value = response;
          console.log(`%c[Index页面] 简单提醒加载成功`, 'color: #4CAF50; font-weight: bold;');
          console.log(`%c[Index页面] 提醒数量变化: ${oldCount} -> ${response.length}`, 'color: #4CAF50;');
        } else {
          console.warn('%c[Index页面] 简单提醒响应格式异常:', 'color: #FF9800;', response);
          simpleReminders.value = [];
        }
      } catch (error) {
        console.error('%c[Index页面] 加载简单提醒失败:', 'color: #f44336; font-weight: bold;', error);
        simpleReminders.value = [];
        
        // 如果是认证错误，清空数据
        if (error.message && error.message.includes('认证')) {
          console.log('%c[Index页面] 检测到认证错误，清空所有用户数据', 'color: #f44336;');
          clearAllUserData();
        }
      } finally {
        isLoading.value = false;
        console.log('%c[Index页面] 简单提醒加载流程结束', 'color: #2196F3;');
      }
    };

    // 加载复杂提醒
    const loadComplexReminders = async () => {
      if (isLoading.value) return;
      
      try {
        isLoading.value = true;
        console.log('开始加载复杂提醒...');
        
        const response = await getAllComplexReminders();
        console.log('复杂提醒加载响应:', response);
        
        if (response && Array.isArray(response)) {
          complexReminders.value = response;
          console.log(`成功加载 ${response.length} 个复杂提醒`);
        } else {
          console.warn('复杂提醒响应格式异常:', response);
          complexReminders.value = [];
        }
      } catch (error) {
        console.error('加载复杂提醒失败:', error);
        complexReminders.value = [];
        
        // 如果是认证错误，清空数据
        if (error.message && error.message.includes('认证')) {
          clearAllUserData();
        }
      } finally {
        isLoading.value = false;
      }
    };

    // 删除复杂提醒
    const deleteComplexReminder = async (reminderId) => {
      try {
        console.log('删除复杂提醒:', reminderId);
        await deleteComplexReminderApi(reminderId);
        
        // 从本地列表中移除
        complexReminders.value = complexReminders.value.filter(r => r.id !== reminderId);
        
        // 更新全局数据版本，通知其他页面数据已变更
        updateDataVersion();
        console.log('✅ 复杂提醒删除成功，全局数据版本已更新');
        
        uni.showToast({
          title: '删除成功',
          icon: 'success'
        });
      } catch (error) {
        console.error('删除复杂提醒失败:', error);
        uni.showToast({
          title: '删除失败',
          icon: 'none'
        });
      }
    };

    const editComplexReminder = async (reminderId) => {
      console.log('首页: 编辑复杂提醒, ID:', reminderId);
      
      // 检查登录状态，未登录会弹出登录框并等待
      const isLoggedIn = await requireAuth();
      if (!isLoggedIn) {
        console.log('用户取消登录，不跳转编辑页面');
        return;
      }
      
      uni.navigateTo({
        url: `/pages/create-complex/create-complex?id=${reminderId}`
      });
    };
    
    const handleCreateNew = async () => {
      // 检查登录状态，未登录会弹出登录框并等待
      const isLoggedIn = await requireAuth();
      if (!isLoggedIn) {
        console.log('用户取消登录，不跳转创建页面');
        return;
      }
      
      const url = activeTab.value === 'simple'
        ? '/pages/create/create'
        : '/pages/create-complex/create-complex';
      uni.navigateTo({ url });
    };
    
    const closeLoginModal = () => {
      showLoginPopup.value = false;
    };
    
    // 微信登录处理
    const handleWechatLogin = async () => {
      console.log('微信登录事件触发');
      
      try {
        uni.showLoading({ title: '登录中...' });
        
        // 登录只需要微信临时 code；头像和昵称在资料编辑页单独由用户选择。
        const response = await smartWechatLogin();
        console.log('后端登录响应:', response);
        
        if (response && response.accessToken) {
          // 使用ReminderCacheService处理登录成功
          await ReminderCacheService.onLoginSuccess(response, 'wechat');
          
          console.log('✅ 登录处理完成，用户状态已更新');
          
          // 关闭登录弹窗
          showLoginPopup.value = false;
          
          uni.hideLoading();
          uni.showToast({
            title: '登录成功',
            icon: 'success'
          });
          
          // 刷新页面数据
          setTimeout(() => {
            loadCurrentTabData();
          }, 1000);
        } else {
          throw new Error('登录响应格式错误');
        }
        
      } catch (error) {
        console.error('微信登录失败:', error);
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
    
    // 页面首次加载时的逻辑
    const handlePageLoad = async () => {
      console.log('%c[Index页面] onLoad处理开始', 'color: #4CAF50; font-weight: bold;');
      
      // 等待用户服务初始化完成
      let retryCount = 0;
      const maxRetries = 10; // 最多重试10次，总共约5秒
      
      while (retryCount < maxRetries) {
        if (ReminderCacheService.getUserState().isAuthenticated) {
          console.log('%c[Index页面] 用户已认证，开始加载数据', 'color: #4CAF50;');
          await loadCurrentTabData();
          break;
        } else {
          console.log(`%c[Index页面] 用户未认证，等待认证完成... (${retryCount + 1}/${maxRetries})`, 'color: #FF9800;');
          retryCount++;
          
          if (retryCount < maxRetries) {
            // 等待500ms后重试
            await new Promise(resolve => setTimeout(resolve, 500));
          } else {
            console.log('%c[Index页面] 等待认证超时，用户可能未登录', 'color: #f44336;');
            // 用户未登录，显示空状态
            clearPageData();
          }
        }
      }
      
      // 同步本地数据版本
      localDataVersion.value = globalDataVersion.value;
      console.log('%c[Index页面] onLoad处理完成', 'color: #4CAF50; font-weight: bold;');
    };

    // 页面显示时的逻辑
    const handlePageShow = async () => {
      console.log('%c[Index页面] onShow触发，强制刷新数据', 'color: #9C27B0; font-weight: bold;');
      
      // 强制刷新当前标签页的数据
      if (isAuthenticated()) {
        await loadCurrentTabData();
      }
      
      // 保留原有的登录状态检查，但不再重复加载数据
      checkDataSyncOnShow(
        'IndexPage',
        () => {}, 
        () => clearPageData()
      );
    };
    
    // 监听页面显示事件，刷新数据
    uni.$on('refreshIndexData', () => {
      console.log('收到页面显示刷新事件，重新加载数据');
      if (isAuthenticated()) {
        loadCurrentTabData();
      }
    });
    
    // 监听用户登录成功事件
    uni.$on('userLoginSuccess', () => {
      console.log('收到用户登录成功事件，刷新Index页面数据');
      if (isAuthenticated()) {
        loadCurrentTabData();
      }
    });

    // 监听用户登出事件，清理Index页面数据
    uni.$on('userLogout', () => {
      console.log('Index页面：收到用户登出事件，清理所有数据');

      // 清空提醒数据
      simpleReminders.value = [];
      complexReminders.value = [];

      // 重置加载状态
      isLoading.value = false;
      refreshing.value = false;

      // 重置到简单提醒标签
      activeTab.value = 'simple';

      console.log('✅ Index页面：数据清理完成');
    });

    // 组件销毁时清理事件监听器和数据同步
    onUnmounted(() => {
      uni.$off('refreshIndexData');
      uni.$off('userLoginSuccess');
      uni.$off('userLogout');

      // 注销数据同步
      if (unregisterDataSync) {
        unregisterDataSync();
      }
    });

    const goToDetail = async (reminder) => {
      // 检查登录状态，未登录会弹出登录框并等待
      const isLoggedIn = await requireAuth();
      if (!isLoggedIn) {
        console.log('用户取消登录，不跳转详情页面');
        return;
      }
      
      uni.navigateTo({
        url: `/pages/detail/detail?id=${reminder.id}`
      });
    };

    return {
      // 响应式数据
      activeTab,
      simpleReminders,
      complexReminders,
      isLoading,
      refreshing,
      showLoginPopup,
      newUserInfo,
      localDataVersion,
      
      // 计算属性  
      currentReminders,
      hasNoData,
      emptyStateText,
      emptyStateDesc,
      createButtonText,
      loading,
      
      // 方法
      switchTab,
      loadCurrentTabData,
      loadSimpleReminders,
      loadComplexReminders,
      deleteComplexReminder,
      editComplexReminder,
      handleCreateNew,
      closeLoginModal,
      handleWechatLogin,
      goToDetail,
      clearPageData,
      handlePageLoad,
      handlePageShow
    };
  }
};
</script>
<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #E6EFEC;
}

/* 顶部标题区域 */
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx;
  background-color: #fcfbf8;
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

.reminder-count {
  font-size: 26rpx;
  color: #9d8148;
  font-weight: 400;
  margin-top: 8rpx;
}

.action-buttons {
  display: flex;
  gap: 24rpx;
}

.action-btn {
  height: 72rpx;
  padding: 0 32rpx;
  border-radius: 16rpx;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  transition: all 0.2s ease;
}

.primary-btn {
  background-color: #f7bd4a;
  color: #1c170d;
}

.wechat-login-btn {
  background-color: #07c160;
  color: #ffffff;
}

.secondary-btn {
  background-color: #ffffff;
  color: #1c170d;
  border: 2rpx solid #e9e0ce;
}

.btn-text {
  font-size: 28rpx;
  font-weight: 600;
}

/* 内容区域 */
.content-scroll {
  flex: 1;
  background-color: #fcfbf8;
}

.content-container {
  padding: 0 32rpx;
}

.content-container {
  padding: 0 32rpx 32rpx;
  max-width: 960rpx;
  margin: 0 auto;
}

/* 加载状态 */
.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 120rpx 0;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24rpx;
}

.loading-text {
  font-size: 28rpx;
  color: #9d8148;
  font-weight: 500;
}

/* 空状态 */
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 120rpx 32rpx;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  max-width: 480rpx;
}

.empty-icon {
  font-size: 96rpx;
  margin-bottom: 32rpx;
  opacity: 0.6;
}

.empty-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #1c170d;
  margin-bottom: 16rpx;
}

.empty-desc {
  font-size: 28rpx;
  color: #9d8148;
  line-height: 1.4;
  margin-bottom: 48rpx;
}

.empty-action-btn {
  height: 88rpx;
  padding: 0 48rpx;
  background-color: #f7bd4a;
  color: #1c170d;
  border-radius: 16rpx;
  border: none;
  font-size: 32rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 提醒列表 */
.reminder-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

/* 响应式调整 */
@media (max-width: 750rpx) {
  .header-section {
    flex-direction: column;
    gap: 24rpx;
    align-items: stretch;
  }
  
  .title-container {
    text-align: center;
  }
  
  .action-buttons {
    justify-content: center;
  }
  
  .tab-container {
    padding: 0 24rpx 24rpx;
  }
  
  .content-container {
    padding: 0 24rpx 24rpx;
  }
  
  .bottom-actions {
    padding: 24rpx;
  }
}

/* 标签切换 */
.tab-container {
  padding: 0 32rpx 24rpx;
  background-color: #fcfbf8;
}

.tab-buttons {
  display: flex;
  background-color: #f0f0f0;
  border-radius: 16rpx;
  padding: 8rpx;
}

.tab-button {
  flex: 1;
  text-align: center;
  padding: 24rpx 32rpx;
  border-radius: 8rpx;
  transition: all 0.2s ease;
  cursor: pointer;
}

.tab-button.active {
  background-color: #f7bd4a;
}

.tab-text {
  font-size: 28rpx;
  color: #9d8148;
  font-weight: 500;
}

.tab-button.active .tab-text {
  color: #1c170d;
  font-weight: 600;
}

/* 底部按钮 */
.bottom-actions {
  padding: 32rpx;
  background-color: #fcfbf8;
  border-top: 1rpx solid #e9e0ce;
}

.bottom-btn {
  width: 100%;
  height: 88rpx;
  background-color: #f7bd4a;
  color: #1c170d;
  border-radius: 16rpx;
  border: none;
  font-size: 32rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 底部间距 */
.bottom-spacer {
  height: 120rpx;
}

/* 底部浮动按钮 */
.fab-container {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
  padding: 24rpx;
  background-color: #fcfbf8;
  border-top: 1rpx solid #e9e0ce;
}

.fab {
  width: 88rpx;
  height: 88rpx;
  background-color: #f7bd4a;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.fab-text {
  font-size: 48rpx;
  font-weight: 600;
  color: #1c170d;
}

/* 登录弹窗样式 */
.login-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(28, 23, 13, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.login-modal {
  background-color: #fcfbf8;
  border-radius: 24rpx;
  width: 600rpx;
  max-width: 90vw;
  overflow: hidden;
  border: 2rpx solid #e9e0ce;
}

.login-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx;
  border-bottom: 2rpx solid #e9e0ce;
  background-color: #fcfbf8;
}

.login-modal-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #1c170d;
}

.login-modal-close {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 8rpx;
  transition: all 0.2s ease;
}

.login-modal-close:hover {
  background-color: #e9e0ce;
}

.close-icon {
  font-size: 40rpx;
  color: #9d8148;
  line-height: 1;
  font-weight: 600;
}

.login-modal-content {
  padding: 48rpx 32rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32rpx;
  background-color: #fcfbf8;
}

.login-desc {
  font-size: 28rpx;
  color: #9d8148;
  text-align: center;
  font-weight: 500;
}

.wechat-login-button {
  width: 100%;
  height: 88rpx;
  background-color: #f7bd4a;
  color: #1c170d;
  border-radius: 16rpx;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  transition: all 0.2s ease;
  box-shadow: 0 4rpx 12rpx rgba(247, 189, 74, 0.3);
}

.wechat-login-button:active {
  transform: translateY(2rpx);
  box-shadow: 0 2rpx 8rpx rgba(247, 189, 74, 0.4);
}

.wechat-login-text {
  font-size: 32rpx;
  color: #1c170d;
  font-weight: 600;
}
</style>
