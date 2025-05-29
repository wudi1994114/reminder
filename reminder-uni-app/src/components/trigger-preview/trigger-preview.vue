<template>
  <view class="trigger-preview">
    <!-- 标题和操作按钮 -->
    <view class="preview-header">
      <text class="preview-title">{{ title }}</text>
      <view class="preview-actions" @click="showActions" v-if="showActionButton">
        <text class="action-icon">⋯</text>
      </view>
    </view>

    
    <!-- 触发时间列表 -->
    <view class="trigger-times">
      <view v-if="previewTimes.length === 0" class="no-preview">
        <text class="no-preview-text">{{ emptyText }}</text>
      </view>
      <view v-else>
        <view 
          v-for="(time, index) in displayTimes" 
          :key="index" 
          class="time-item"
          :class="{ 'time-item-highlight': highlightFirst && index === 0 }"
        >
          <view class="time-content">
            <text class="time-text">{{ time }}</text>
            <text class="time-index" v-if="showIndex">第{{ index + 1 }}次</text>
          </view>
        </view>
        
        <!-- 显示更多按钮 -->
        <view v-if="previewTimes.length > maxDisplay && !showAll" class="show-more" @click="toggleShowAll">
          <text class="show-more-text">显示更多 ({{ previewTimes.length - maxDisplay }})</text>
        </view>
        
        <!-- 收起按钮 -->
        <view v-if="showAll && previewTimes.length > maxDisplay" class="show-less" @click="toggleShowAll">
          <text class="show-less-text">收起</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';

export default {
  name: 'TriggerPreview',
  props: {
    // 标题
    title: {
      type: String,
      default: '下次触发时间'
    },
    // 预览时间数组
    previewTimes: {
      type: Array,
      default: () => []
    },
    // 人类可读描述
    description: {
      type: String,
      default: ''
    },
    // 空状态文本
    emptyText: {
      type: String,
      default: '暂无预览时间'
    },
    // 最大显示数量
    maxDisplay: {
      type: Number,
      default: 5
    },
    // 是否显示描述
    showDescription: {
      type: Boolean,
      default: true
    },
    // 是否显示操作按钮
    showActionButton: {
      type: Boolean,
      default: true
    },
    // 是否显示序号
    showIndex: {
      type: Boolean,
      default: false
    },
    // 是否高亮第一项
    highlightFirst: {
      type: Boolean,
      default: true
    },
    // 自动刷新间隔（秒）
    autoRefreshInterval: {
      type: Number,
      default: 0
    }
  },
  emits: ['refresh', 'copy-description', 'copy-times', 'action'],
  setup(props, { emit }) {
    const showAll = ref(false);
    const refreshTimer = ref(null);
    
    // 计算显示的时间列表
    const displayTimes = computed(() => {
      if (showAll.value || props.previewTimes.length <= props.maxDisplay) {
        return props.previewTimes;
      }
      return props.previewTimes.slice(0, props.maxDisplay);
    });
    
    // 切换显示全部/收起
    const toggleShowAll = () => {
      showAll.value = !showAll.value;
    };
    
    // 显示操作菜单
    const showActions = () => {
      const actions = ['刷新预览'];
      
      if (props.description) {
        actions.push('复制描述');
      }
      
      if (props.previewTimes.length > 0) {
        actions.push('复制时间表');
      }
      
      uni.showActionSheet({
        itemList: actions,
        success: (res) => {
          handleAction(res.tapIndex, actions);
        }
      });
    };
    
    // 处理操作
    const handleAction = (index, actions) => {
      const action = actions[index];
      
      switch (action) {
        case '刷新预览':
          emit('refresh');
          uni.showToast({
            title: '预览已刷新',
            icon: 'success',
            duration: 1500
          });
          break;
          
        case '复制描述':
          if (props.description) {
            uni.setClipboardData({
              data: props.description,
              success: () => {
                uni.showToast({
                  title: '描述已复制',
                  icon: 'success',
                  duration: 1500
                });
                emit('copy-description', props.description);
              }
            });
          }
          break;
          
        case '复制时间表':
          if (props.previewTimes.length > 0) {
            const timeList = props.previewTimes.join('\n');
            uni.setClipboardData({
              data: timeList,
              success: () => {
                uni.showToast({
                  title: '时间表已复制',
                  icon: 'success',
                  duration: 1500
                });
                emit('copy-times', timeList);
              }
            });
          }
          break;
      }
      
      emit('action', { action, index });
    };
    
    // 设置自动刷新
    const setupAutoRefresh = () => {
      if (props.autoRefreshInterval > 0) {
        refreshTimer.value = setInterval(() => {
          emit('refresh');
        }, props.autoRefreshInterval * 1000);
      }
    };
    
    // 清除自动刷新
    const clearAutoRefresh = () => {
      if (refreshTimer.value) {
        clearInterval(refreshTimer.value);
        refreshTimer.value = null;
      }
    };
    
    // 监听自动刷新间隔变化
    watch(() => props.autoRefreshInterval, (newInterval) => {
      clearAutoRefresh();
      if (newInterval > 0) {
        setupAutoRefresh();
      }
    });
    
    // 组件挂载时设置自动刷新
    onMounted(() => {
      setupAutoRefresh();
    });
    
    // 组件卸载时清除定时器
    onUnmounted(() => {
      clearAutoRefresh();
    });
    
    // 手动刷新方法
    const refresh = () => {
      emit('refresh');
    };
    
    // 获取预览数据
    const getPreviewData = () => {
      return {
        times: props.previewTimes,
        description: props.description,
        count: props.previewTimes.length
      };
    };
    
    return {
      showAll,
      displayTimes,
      toggleShowAll,
      showActions,
      handleAction,
      refresh,
      getPreviewData
    };
  }
};
</script>

<style scoped>
.trigger-preview {
  background-color: #ffffff;
  border-radius: 24rpx;
  border: 2rpx solid #e9e0ce;
  overflow: hidden;
}

/* 预览头部 */
.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
  background-color: #fcfbf8;
}

.preview-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
}

.preview-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48rpx;
  height: 48rpx;
  border-radius: 24rpx;
  background-color: #f4efe7;
  cursor: pointer;
  transition: all 0.2s ease;
}

.preview-actions:active {
  background-color: #e6d9c8;
  transform: scale(0.95);
}

.action-icon {
  font-size: 32rpx;
  color: #1c170d;
  font-weight: 600;
}

/* 描述部分 */
.description-section {
  padding: 24rpx 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
  background-color: #f8f9fa;
}

.description-container {
  padding: 20rpx 24rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  border-left: 6rpx solid #f7bd4a;
}

.description-text {
  font-size: 28rpx;
  color: #1c170d;
  font-weight: 500;
  line-height: 1.4;
}

/* 空状态 */
.no-preview {
  text-align: center;
  padding: 64rpx 32rpx;
}

.no-preview-text {
  font-size: 28rpx;
  color: #9d8148;
  font-weight: 400;
}

/* 时间项 */
.time-item {
  border-bottom: 1rpx solid #f0f0f0;
  transition: background-color 0.2s ease;
}

.time-item:last-child {
  border-bottom: none;
}

.time-item:active {
  background-color: #f8f9fa;
}

.time-item-highlight {
  background-color: #fff8e1;
  border-left: 6rpx solid #f7bd4a;
}

.time-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx;
}

.time-text {
  font-size: 32rpx;
  color: #1c170d;
  font-weight: 400;
  flex: 1;
}

.time-index {
  font-size: 24rpx;
  color: #9d8148;
  font-weight: 400;
  background-color: #f4efe7;
  padding: 8rpx 16rpx;
  border-radius: 12rpx;
}

/* 显示更多/收起按钮 */
.show-more,
.show-less {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24rpx 32rpx;
  background-color: #f8f9fa;
  border-top: 1rpx solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.show-more:active,
.show-less:active {
  background-color: #f0f0f0;
}

.show-more-text,
.show-less-text {
  font-size: 28rpx;
  color: #f7bd4a;
  font-weight: 500;
}

/* 响应式适配 */
@media (max-width: 750rpx) {
  .preview-header {
    padding: 24rpx;
  }
  
  .preview-title {
    font-size: 28rpx;
  }
  
  .description-section {
    padding: 20rpx 24rpx;
  }
  
  .description-text {
    font-size: 26rpx;
  }
  
  .time-content {
    padding: 24rpx;
  }
  
  .time-text {
    font-size: 28rpx;
  }
  
  .time-index {
    font-size: 22rpx;
    padding: 6rpx 12rpx;
  }
  
  .no-preview {
    padding: 48rpx 24rpx;
  }
  
  .no-preview-text {
    font-size: 26rpx;
  }
}

/* 动画效果 */
.time-item {
  animation: fadeInUp 0.3s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 加载状态 */
.trigger-preview.loading {
  opacity: 0.7;
  pointer-events: none;
}

.trigger-preview.loading::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 40rpx;
  height: 40rpx;
  margin: -20rpx 0 0 -20rpx;
  border: 4rpx solid #f0f0f0;
  border-top: 4rpx solid #f7bd4a;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>