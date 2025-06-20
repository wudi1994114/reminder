<template>
  <view v-if="show" class="picker-overlay" @click="cancel">
    <view class="picker-modal" @click.stop>
      <view class="picker-header">
        <text class="picker-title">{{ title }}</text>
        <view class="picker-close" @click="cancel">
          <text class="close-icon">✕</text>
        </view>
      </view>
      <scroll-view class="picker-content" scroll-y>
        <view 
          v-for="(item, index) in items" 
          :key="index" 
          class="picker-item"
          :class="{ 'selected': isSelected(item) }"
          @click="selectItem(item)"
        >
          <text class="item-text">{{ item.label || item }}</text>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'CustomPickerModal',
  props: {
    show: {
      type: Boolean,
      default: false,
    },
    title: {
      type: String,
      default: '请选择',
    },
    items: {
      type: Array,
      default: () => [],
    },
    selectedValue: {
      type: [String, Number, Object],
      default: null,
    },
  },
  watch: {
    show(newVal, oldVal) {
      console.log('CustomPickerModal show 属性变化:', oldVal, '->', newVal);
      console.log('当前 props:', {
        title: this.title,
        items: this.items,
        selectedValue: this.selectedValue
      });
    },
    items(newVal) {
      console.log('CustomPickerModal items 属性变化:', newVal);
    }
  },
  mounted() {
    console.log('CustomPickerModal 组件已挂载');
    console.log('初始 props:', {
      show: this.show,
      title: this.title,
      items: this.items,
      selectedValue: this.selectedValue
    });
  },
  methods: {
    isSelected(item) {
        console.log('CustomPickerModal isSelected 检查:', item, 'vs', this.selectedValue);
        if (item && typeof item === 'object') {
            const result = item.value === this.selectedValue;
            console.log('对象比较结果:', result);
            return result;
        }
        const result = item === this.selectedValue;
        console.log('直接比较结果:', result);
        return result;
    },
    cancel() {
      console.log('CustomPickerModal cancel 被调用');
      this.$emit('update:show', false);
      this.$emit('cancel');
    },
    selectItem(item) {
      console.log('CustomPickerModal selectItem 被调用:', item);
      this.$emit('confirm', item);
      this.$emit('update:show', false);
    },
  },
};
</script>

<style scoped>
.picker-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1001;
}

.picker-modal {
  background-color: #fcfbf8;
  border-radius: 24rpx;
  margin: 32rpx;
  width: 80%;
  max-width: 600rpx;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
  border-bottom: 1rpx solid #f4efe7;
}

.picker-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1c170d;
}

.picker-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48rpx;
  height: 48rpx;
  cursor: pointer;
}

.close-icon {
  font-size: 40rpx;
  color: #1c170d;
}

.picker-content {
  padding: 16rpx 0;
  flex: 1;
}

.picker-item {
  padding: 32rpx;
  text-align: center;
  cursor: pointer;
  transition: background-color 0.2s;
}

.picker-item:active {
  background-color: #f4efe7;
}

.item-text {
  font-size: 36rpx;
  color: #1c170d;
}

.picker-item.selected .item-text {
  color: #f7bd4a;
  font-weight: 600;
}
</style> 