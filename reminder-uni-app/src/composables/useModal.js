import { ref, watch } from 'vue';

/**
 * 弹窗组合式函数
 * 用于处理弹窗的显示/隐藏和防止背景滚动
 */
export function useModal() {
  const isVisible = ref(false);
  
  // 显示弹窗
  const showModal = () => {
    isVisible.value = true;
    // 阻止页面滚动
    preventBodyScroll();
  };
  
  // 隐藏弹窗
  const hideModal = () => {
    isVisible.value = false;
    // 恢复页面滚动
    restoreBodyScroll();
  };
  
  // 切换弹窗显示状态
  const toggleModal = () => {
    if (isVisible.value) {
      hideModal();
    } else {
      showModal();
    }
  };
  
  // 阻止页面滚动
  const preventBodyScroll = () => {
    // 在uni-app中，我们通过CSS来处理
    // 这里可以添加一些额外的逻辑
    console.log('阻止背景滚动');
  };
  
  // 恢复页面滚动
  const restoreBodyScroll = () => {
    // 恢复页面滚动
    console.log('恢复背景滚动');
  };
  
  // 处理遮罩层点击
  const handleOverlayClick = (closeOnOverlay = true) => {
    if (closeOnOverlay) {
      hideModal();
    }
  };
  
  // 处理触摸移动事件（阻止背景滚动）
  const handleTouchMove = (e) => {
    e.preventDefault();
    e.stopPropagation();
  };
  
  // 监听弹窗状态变化
  watch(isVisible, (newValue) => {
    if (newValue) {
      preventBodyScroll();
    } else {
      restoreBodyScroll();
    }
  });
  
  return {
    isVisible,
    showModal,
    hideModal,
    toggleModal,
    handleOverlayClick,
    handleTouchMove
  };
}

/**
 * 弹窗事件处理器
 * 返回常用的事件处理属性
 */
export function useModalEvents(closeOnOverlay = true) {
  const { handleOverlayClick, handleTouchMove } = useModal();
  
  return {
    // 遮罩层事件
    overlayEvents: {
      '@touchmove.stop.prevent': handleTouchMove,
      '@click.self': () => handleOverlayClick(closeOnOverlay)
    },
    
    // 弹窗内容事件
    contentEvents: {
      '@click.stop': () => {} // 阻止事件冒泡
    }
  };
} 