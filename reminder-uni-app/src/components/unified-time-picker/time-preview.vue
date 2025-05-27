<template>
  <view class="time-preview" v-if="timeData && (timeData.previewTimes?.length > 0 || timeData.description)">
    <!-- 标题和描述 -->
    <view class="preview-header">
      <text class="preview-title">{{ title }}</text>
      <view class="preview-actions" v-if="showActions" @click="showActionMenu">
        <text class="action-icon">⋯</text>
      </view>
    </view>
    
    <!-- 人类可读描述 -->
    <view class="description-section" v-if="showDescription && timeData.description">
      <text class="description-text">{{ timeData.description }}</text>
    </view>
    
    <!-- 预览时间列表 -->
    <view class="times-section" v-if="timeData.previewTimes?.length > 0">
      <view class="times-list">
        <view 
          v-for="(time, index) in displayTimes" 
          :key="index"
          class="time-item"
          :class="{ 
            'time-item-highlight': highlightFirst && index === 0,
            'time-item-past': isPastTime(time)
          }"
        >
          <view class="time-content">
            <text class="time-text">{{ time }}</text>
            <text class="time-index" v-if="showIndex">第{{ index + 1 }}次</text>
          </view>
        </view>
      </view>
      
      <!-- 展开/收起按钮 -->
      <view v-if="timeData.previewTimes.length > maxDisplay" class="expand-section">
        <view class="expand-btn" @click="toggleExpand">
          <text class="expand-text">
            {{ isExpanded ? '收起' : `显示更多 (${timeData.previewTimes.length - maxDisplay})` }}
          </text>
          <text class="expand-icon" :class="{ 'expand-icon-up': isExpanded }">▼</text>
        </view>
      </view>
    </view>
    
    <!-- 空状态 -->
    <view v-else class="empty-state">
      <text class="empty-text">{{ emptyText }}</text>
    </view>
  </view>
</template>

<script>
import { ref, computed } from 'vue';

export default {
  name: 'TimePreview',
  props: {
    // 时间数据对象
    timeData: {
      type: Object,
      default: null
    },
    // 标题
    title: {
      type: String,
      default: '触发时间预览'
    },
    // 最大显示数量
    maxDisplay: {
      type: Number,
      default: 3
    },
    // 是否显示描述
    showDescription: {
      type: Boolean,
      default: true
    },
    // 是否显示操作按钮
    showActions: {
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
    // 空状态文本
    emptyText: {
      type: String,
      default: '暂无预览时间'
    }
  },
  emits: ['refresh', 'copy-description', 'copy-times', 'action'],
  
  setup(props, { emit }) {
    const isExpanded = ref(false);
    
    // 显示的时间列表
    const displayTimes = computed(() => {
      if (!props.timeData?.previewTimes) return [];
      
      if (isExpanded.value || props.timeData.previewTimes.length <= props.maxDisplay) {
        return props.timeData.previewTimes;
      }
      return props.timeData.previewTimes.slice(0, props.maxDisplay);
    });
    
    // 切换展开状态
    const toggleExpand = () => {
      isExpanded.value = !isExpanded.value;
    };
    
    // 检查是否是过去的时间
    const isPastTime = (timeStr) => {
      // 这里可以根据需要实现时间比较逻辑
      return false;
    };
    
    // 显示操作菜单
    const showActionMenu = () => {
      const actions = ['刷新预览'];
      
      if (props.timeData?.description) {
        actions.push('复制描述');
      }
      
      if (props.timeData?.previewTimes?.length > 0) {
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
          if (props.timeData?.description) {
            uni.setClipboardData({
              data: props.timeData.description,
              success: () => {
                uni.showToast({
                  title: '描述已复制',
                  icon: 'success',
                  duration: 1500
                });
                emit('copy-description', props.timeData.description);
              }
            });
          }
          break;
          
        case '复制时间表':
          if (props.timeData?.previewTimes?.length > 0) {
            const timeList = props.timeData.previewTimes.join('\n');
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
    
    return {
      isExpanded,
      displayTimes,
      toggleExpand,
      isPastTime,
      showActionMenu
    };
  }
};
</script>

<style scoped>
.time-preview {
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 24rpx;
  border: 1rpx solid #e9e0ce;
  overflow: hidden;
}

/* 预览头部 */
.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 32rpx 16rpx;
}

.preview-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
}

.preview-actions {
  padding: 8rpx 16rpx;
  border-radius: 16rpx;
  transition: background-color 0.2s ease;
}

.preview-actions:active {
  background-color: rgba(157, 129, 72, 0.1);
}

.action-icon {
  font-size: 32rpx;
  color: #9d8148;
  font-weight: 600;
}

/* 描述区域 */
.description-section {
  padding: 0 32rpx 16rpx;
}

.description-text {
  font-size: 28rpx;
  color: #9d8148;
  font-weight: 500;
  line-height: 1.4;
}

/* 时间列表区域 */
.times-section {
  padding: 0 32rpx 24rpx;
}

.times-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.time-item {
  padding: 20rpx 24rpx;
  background-color: #f8f5ed;
  border-radius: 16rpx;
  border-left: 4rpx solid transparent;
  transition: all 0.2s ease;
}

.time-item-highlight {
  border-left-color: #f7bd4a;
  background-color: #fef9f0;
  box-shadow: 0 2rpx 8rpx rgba(247, 189, 74, 0.15);
}

.time-item-past {
  opacity: 0.6;
  background-color: #f0f0f0;
}

.time-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.time-text {
  font-size: 28rpx;
  color: #1c170d;
  font-weight: 500;
  line-height: 1.3;
}

.time-index {
  font-size: 24rpx;
  color: #9d8148;
  font-weight: 400;
}

/* 展开按钮 */
.expand-section {
  margin-top: 16rpx;
  display: flex;
  justify-content: center;
}

.expand-btn {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 12rpx 24rpx;
  background-color: #f0ede4;
  border-radius: 20rpx;
  transition: all 0.2s ease;
}

.expand-btn:active {
  background-color: #ebe7dc;
  transform: scale(0.95);
}

.expand-text {
  font-size: 26rpx;
  color: #9d8148;
  font-weight: 500;
}

.expand-icon {
  font-size: 20rpx;
  color: #9d8148;
  transition: transform 0.3s ease;
}

.expand-icon-up {
  transform: rotate(180deg);
}

/* 空状态 */
.empty-state {
  padding: 48rpx 32rpx;
  text-align: center;
}

.empty-text {
  font-size: 28rpx;
  color: #9d8148;
  font-weight: 400;
}

/* 响应式适配 */
@media (max-width: 750rpx) {
  .preview-header {
    padding: 20rpx 24rpx 12rpx;
  }
  
  .preview-title {
    font-size: 28rpx;
  }
  
  .description-section {
    padding: 0 24rpx 12rpx;
  }
  
  .description-text {
    font-size: 26rpx;
  }
  
  .times-section {
    padding: 0 24rpx 20rpx;
  }
  
  .time-item {
    padding: 16rpx 20rpx;
  }
  
  .time-text {
    font-size: 26rpx;
  }
  
  .time-index {
    font-size: 22rpx;
  }
}
</style> 