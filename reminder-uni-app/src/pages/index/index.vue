<template>
  <view class="page-container">
    <!-- 顶部标题区域 -->
    <view class="header-section">
      <view class="title-container">
        <text class="page-title">我的提醒</text>
      </view>
      <view class="action-buttons">
        <button class="action-btn primary-btn" @click="handleCreateNew">
          <text class="btn-text">新建提醒</text>
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
              @click="goToComplexDetail"
              @edit="editComplexReminder"
              @delete="deleteComplexReminder"
            />
          </view>
        </view>
      </view>
      
      <!-- 底部间距，为固定按钮留出空间 -->
      <view class="bottom-spacer"></view>
    </scroll-view>
  </view>
</template>

<script>
import { ref, onMounted, computed } from 'vue';
import { getUpcomingReminders, getAllComplexReminders, deleteComplexReminder as deleteComplexReminderApi } from '../../services/api';
import { reminderState } from '../../services/store';
import SimpleReminderCard from '../../components/SimpleReminderCard.vue';
import ComplexReminderCard from '../../components/ComplexReminderCard.vue';

export default {
  name: 'IndexPage',
  components: {
    SimpleReminderCard,
    ComplexReminderCard
  },
  onTabItemTap() {
    this.loadCurrentTabData();
  },
  
  // 添加onShow生命周期方法，页面显示时刷新数据
  onShow() {
    console.log('Index页面显示，刷新当前标签页数据');
    // 调用setup中返回的方法来刷新数据
    if (this.loadCurrentTabData) {
      this.loadCurrentTabData();
    }
  },
  
  setup() {
    const loading = ref(false);
    const activeTab = ref('simple'); // 'simple' 或 'complex'
    
    // 使用共享状态管理的即将到来的提醒数据
    const simpleReminders = computed(() => {
      return reminderState.upcomingReminders || [];
    });
    
    // 复杂提醒数据 - 改为使用全局状态管理
    const complexReminders = computed(() => {
      return reminderState.complexReminders || [];
    });
    
    // 加载当前标签页数据
    const loadCurrentTabData = () => {
      console.log('加载当前标签页数据，当前标签:', activeTab.value);
      if (activeTab.value === 'simple') {
        loadSimpleReminders();
      } else {
        loadComplexReminders();
      }
    };
    
    // 加载即将到来的简单提醒列表
    const loadSimpleReminders = async () => {
      try {
        loading.value = true;
        reminderState.loading = true;
        
        const result = await getUpcomingReminders();
        
        // 确保result是数组才设置状态，否则设置为空数组
        if (Array.isArray(result)) {
          reminderState.upcomingReminders = result;
        } else {
          console.warn('API返回的数据不是数组:', result);
          reminderState.upcomingReminders = [];
        }
      } catch (error) {
        console.error('获取简单提醒列表失败:', error);
        
        // 确保发生错误时也赋值为空数组
        reminderState.upcomingReminders = [];
        
        uni.showToast({
          title: '获取简单提醒列表失败',
          icon: 'none',
          duration: 2000
        });
      } finally {
        loading.value = false;
        reminderState.loading = false;
      }
    };
    
    // 加载复杂提醒列表 - 更新全局状态
    const loadComplexReminders = async () => {
      try {
        loading.value = true;
        
        const result = await getAllComplexReminders();
        
        // 确保result是数组才设置状态，否则设置为空数组
        if (Array.isArray(result)) {
          // 更新全局状态
          reminderState.complexReminders = result;
        } else {
          console.warn('API返回的数据不是数组:', result);
          reminderState.complexReminders = [];
        }
      } catch (error) {
        console.error('获取复杂提醒列表失败:', error);
        
        reminderState.complexReminders = [];
        
        uni.showToast({
          title: '获取复杂提醒列表失败',
          icon: 'none',
          duration: 2000
        });
      } finally {
        loading.value = false;
      }
    };
    
    // 切换标签页
    const switchTab = (tab) => {
      activeTab.value = tab;
      loadCurrentTabData();
    };
    
    const navigateToCreate = () => {
      uni.navigateTo({
        url: '/pages/create/create'
      });
    };
    
    const navigateToComplexCreate = () => {
      uni.navigateTo({
        url: '/pages/create-complex/create-complex'
      });
    };
    
    const goToDetail = (id) => {
      console.log('=== Index页面跳转简单提醒详情 ===');
      console.log('点击的提醒ID:', id);
      console.log('ID类型:', typeof id);
      console.log('跳转URL:', `/pages/detail/detail?id=${id}`);
      
      uni.navigateTo({
        url: `/pages/detail/detail?id=${id}`
      });
    };
    
    const goToComplexDetail = (id) => {
      console.log('=== Index页面跳转复杂提醒详情 ===');
      console.log('点击的复杂提醒ID:', id);
      
      // 暂时跳转到编辑页面，后续可以创建专门的详情页
      uni.navigateTo({
        url: `/pages/create-complex/create-complex?id=${id}`
      });
    };
    
    const editComplexReminder = (id) => {
      uni.navigateTo({
        url: `/pages/create-complex/create-complex?id=${id}`
      });
    };
    
    const deleteComplexReminder = (id) => {
      console.log('=== 删除复杂提醒 ===');
      console.log('要删除的复杂提醒ID:', id);
      
      // 显示确认弹窗
      uni.showModal({
        title: '确认删除',
        content: '确定要删除这个复杂提醒吗？删除后将无法恢复，同时会删除所有相关的简单提醒。',
        confirmText: '删除',
        cancelText: '取消',
        confirmColor: '#ff4757',
        success: async (res) => {
          if (res.confirm) {
            try {
              // 显示加载提示
              uni.showLoading({
                title: '删除中...',
                mask: true
              });
              
              // 调用删除API
              await deleteComplexReminderApi(id);
              
              // 从全局状态中移除该复杂提醒
              const index = reminderState.complexReminders.findIndex(item => item.id === id);
              if (index !== -1) {
                reminderState.complexReminders.splice(index, 1);
              }
              
              // 隐藏加载提示
              uni.hideLoading();
              
              // 显示成功提示
              uni.showToast({
                title: '删除成功',
                icon: 'success',
                duration: 2000
              });
              
              console.log('复杂提醒删除成功，ID:', id);
              
            } catch (error) {
              console.error('删除复杂提醒失败:', error);
              
              // 隐藏加载提示
              uni.hideLoading();
              
              // 处理错误信息
              let errorMessage = '删除失败，请重试';
              
              if (error && error.statusCode) {
                if (error.statusCode === 401) {
                  errorMessage = '请先登录';
                } else if (error.statusCode === 403) {
                  errorMessage = '权限不足';
                } else if (error.statusCode === 404) {
                  errorMessage = '提醒不存在或已被删除';
                } else if (error.statusCode === 500) {
                  errorMessage = error.data?.message || '服务器内部错误';
                } else {
                  errorMessage = `删除失败 (${error.statusCode})`;
                }
              } else if (error && error.message) {
                errorMessage = error.message;
              }
              
              // 显示错误弹窗
              uni.showModal({
                title: '删除失败',
                content: errorMessage,
                showCancel: false,
                confirmText: '知道了'
              });
            }
          }
        }
      });
    };
    

    
    // 页面加载时获取数据
    onMounted(() => {
      loadCurrentTabData();
    });
    
    const handleCreateNew = () => {
      if (activeTab.value === 'simple') {
        navigateToCreate();
      } else {
        navigateToComplexCreate();
      }
    };
    
    return {
      activeTab,
      simpleReminders,
      complexReminders,
      loading,
      switchTab,
      navigateToCreate,
      navigateToComplexCreate,
      goToDetail,
      goToComplexDetail,
      editComplexReminder,
      deleteComplexReminder,
      loadCurrentTabData,
      handleCreateNew
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

.secondary-btn {
  background-color: #ffffff;
  color: #1c170d;
  border: 2rpx solid #e9e0ce;
}

.test-btn {
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
</style>
