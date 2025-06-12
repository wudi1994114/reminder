<template>
  <view v-if="show" class="picker-modal-overlay" @click="handleOverlayClick">
    <view class="picker-modal-container" @click.stop>
      <!-- 标题栏 -->
      <view class="picker-header">
        <view class="cancel-btn" @click="handleCancel">
          <text class="cancel-text">取消</text>
        </view>
        <view class="title-container">
          <text class="picker-title">{{ title }}</text>
        </view>
        <view class="confirm-btn" @click="handleConfirm">
          <text class="confirm-text">确定</text>
        </view>
      </view>
      
      <!-- 选择器内容 -->
      <view class="picker-content">
        <picker-view 
          :value="[selectedIndex]" 
          @change="handlePickerChange"
          class="picker-view"
        >
          <picker-view-column>
            <view 
              v-for="(option, index) in options" 
              :key="option.value"
              class="picker-item"
            >
              <text class="picker-item-text">{{ option.label }}</text>
            </view>
          </picker-view-column>
        </picker-view>
      </view>
    </view>
  </view>
</template>

<script>
import { ref, computed, watch } from 'vue';

export default {
  name: 'PickerModal',
  props: {
    show: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: '请选择'
    },
    options: {
      type: Array,
      default: () => []
    },
    value: {
      type: String,
      default: ''
    }
  },
  emits: ['confirm', 'cancel'],
  setup(props, { emit }) {
    const selectedIndex = ref(0);
    const selectedValue = ref('');

    // 计算当前选中的索引
    const currentIndex = computed(() => {
      const index = props.options.findIndex(option => option.value === props.value);
      return index >= 0 ? index : 0;
    });

    // 监听value变化，更新选中索引
    watch(() => props.value, (newValue) => {
      const index = props.options.findIndex(option => option.value === newValue);
      selectedIndex.value = index >= 0 ? index : 0;
      selectedValue.value = newValue;
    }, { immediate: true });

    // 监听show变化，重置选中状态
    watch(() => props.show, (newShow) => {
      if (newShow) {
        selectedIndex.value = currentIndex.value;
        selectedValue.value = props.value;
      }
    });

    const handlePickerChange = (event) => {
      const index = event.detail.value[0];
      selectedIndex.value = index;
      if (props.options[index]) {
        selectedValue.value = props.options[index].value;
      }
    };

    const handleConfirm = () => {
      emit('confirm', selectedValue.value);
    };

    const handleCancel = () => {
      emit('cancel');
    };

    const handleOverlayClick = () => {
      emit('cancel');
    };

    return {
      selectedIndex,
      selectedValue,
      handlePickerChange,
      handleConfirm,
      handleCancel,
      handleOverlayClick
    };
  }
};
</script>

<style scoped>
.picker-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  z-index: 1000;
}

.picker-modal-container {
  width: 100%;
  background-color: #ffffff;
  border-radius: 16px 16px 0 0;
  overflow: hidden;
}

.picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 16px;
  border-bottom: 1px solid #f0f0f0;
}

.cancel-btn,
.confirm-btn {
  padding: 8px 12px;
  border-radius: 6px;
}

.cancel-text {
  font-size: 16px;
  color: #666666;
}

.confirm-text {
  font-size: 16px;
  color: #007AFF;
  font-weight: 500;
}

.title-container {
  flex: 1;
  display: flex;
  justify-content: center;
}

.picker-title {
  font-size: 18px;
  font-weight: 600;
  color: #333333;
}

.picker-content {
  height: 240px;
}

.picker-view {
  width: 100%;
  height: 100%;
}

.picker-item {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 48px;
}

.picker-item-text {
  font-size: 16px;
  color: #333333;
  text-align: center;
}
</style>