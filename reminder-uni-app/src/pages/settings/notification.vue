<template>
  <view class="container">
    <view class="header-section">
      <view class="nav-container">
        <view class="nav-back" @click="goBack">
          <text class="back-icon">‹</text>
        </view>
        <view class="title-container">
          <text class="page-title">提醒设置</text>
        </view>
        <view class="nav-spacer"></view>
      </view>
    </view>

    <!-- 主要内容区域 -->
    <scroll-view
      class="content-scroll"
      scroll-y
      :scroll-with-animation="true"
      :enable-back-to-top="true"
    >
      <view class="content-container">
        <!-- 通用设置分类 -->
        <view class="category-section">
          <view class="category-header">
            <view class="category-icon">
              <text class="icon-text">⚙️</text>
            </view>
            <text class="category-title">通用</text>
          </view>
          
          <view class="category-card">
            <!-- 标签管理开关 -->
            <view class="setting-item">
              <view class="item-content">
                <view class="item-icon">
                  <text class="icon-text">🏷️</text>
                </view>
                <view class="item-info">
                  <text class="item-label">标签管理</text>
                  <text class="item-description">启用自定义标签功能</text>
                </view>
              </view>
              <view class="item-switch">
                <switch
                  :checked="tagManagementEnabled"
                  @change="onTagManagementToggle"
                  color="#f7bd4a"
                />
              </view>
            </view>

            <!-- 标签列表编辑 -->
            <view v-if="tagManagementEnabled" class="tag-management-section">
              <view class="divider"></view>
              <view class="tag-editor">
                <view class="tag-editor-header">
                  <text class="tag-editor-title">我的标签</text>
                  <text class="tag-count">{{ getTotalLength() }}/100</text>
                </view>

                <!-- 标签列表 -->
                <view class="tag-list">
                  <view
                    v-for="(tag, index) in userTags"
                    :key="index"
                    class="tag-item"
                  >
                    <view class="tag-content">
                      <text class="tag-text">{{ getTagTitle(tag) }}</text>
                    </view>
                    <view class="tag-remove" @click="removeTag(index)">
                      <text class="remove-icon">×</text>
                    </view>
                  </view>

                  <!-- 添加标签按钮 -->
                  <view
                    v-if="getTotalLength() < 100"
                    class="tag-add"
                    @click="showAddTagDialog"
                  >
                    <text class="add-icon">+</text>
                  </view>
                </view>

                <!-- 标签说明 -->
                <view class="tag-tips">
                  <text class="tip-text">• 标签总长度不超过100个字符</text>
                  <text class="tip-text">• 标签用于快速输入常用提醒内容</text>
                </view>
              </view>
            </view>
          </view>
        </view>
        
        <!-- 提醒设置分类 -->
        <view class="category-section">
          <view class="category-header">
            <view class="category-icon">
              <text class="icon-text">🔔</text>
            </view>
            <text class="category-title">提醒</text>
          </view>
          
          <view class="category-card">
            <view class="setting-item" @click="navigateToReminderMethod">
              <view class="item-content">
                <view class="item-icon">
                  <text class="icon-text">📱</text>
                </view>
                <view class="item-info">
                  <text class="item-label">提醒方式</text>
                  <text class="item-description">设置提醒通知方式</text>
                </view>
              </view>
              <view class="item-arrow">
                <text class="arrow-icon">›</text>
              </view>
            </view>
          </view>
        </view>
        
        <!-- 底部间距 -->
        <view class="bottom-spacer"></view>
      </view>
    </scroll-view>

    <!-- 添加标签对话框 -->
    <view v-if="showAddDialog" class="dialog-overlay" @click="hideAddTagDialog">
      <view class="dialog-container" @click.stop>
        <view class="dialog-header">
          <text class="dialog-title">添加标签</text>
        </view>
        <view class="dialog-content">
          <view class="input-wrapper">
            <view class="input-label">
              <text class="label-text">标题</text>
              <text class="required-mark">*</text>
            </view>
            <input
              v-model="newTagTitle"
              class="tag-input"
              placeholder="请输入标签标题"
              maxlength="20"
              :focus="showAddDialog"
            />
          </view>
          
          <view class="input-wrapper">
            <view class="input-label">
              <text class="label-text">内容</text>
            </view>
            <input
              v-model="newTagContent"
              class="tag-input"
              placeholder="请输入标签内容（可选）"
              maxlength="50"
              @confirm="addTag"
            />
          </view>
          
          <view class="input-tips">
            <text class="tip-text">标题用于显示，内容用于快速填入提醒</text>
          </view>
        </view>
        <view class="dialog-actions">
          <button class="dialog-btn cancel-btn" @click="hideAddTagDialog">取消</button>
          <button class="dialog-btn confirm-btn" @click="addTag">确定</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { ref, onMounted } from 'vue';
import {
  getUserTagManagementEnabled,
  setUserTagManagementEnabled,
  getUserTagList,
  setUserTagList
} from '@/services/api.js';

export default {
  name: 'NotificationSettings',
  setup() {
    // 响应式数据
    const tagManagementEnabled = ref(false);
    const userTags = ref([]);
    const showAddDialog = ref(false);
    const newTagTitle = ref('');
    const newTagContent = ref('');
    const loading = ref(false);

    // 页面加载时获取用户设置
    onMounted(async () => {
      await loadUserSettings();
    });

    // 加载用户设置
    const loadUserSettings = async () => {
      try {
        loading.value = true;

        // 获取标签管理开关状态
        try {
          const enabledResponse = await getUserTagManagementEnabled();
          tagManagementEnabled.value = enabledResponse.value === '1';
        } catch (error) {
          console.log('标签管理功能未启用或首次使用');
          tagManagementEnabled.value = false;
        }

        // 如果启用了标签管理，获取标签列表
        if (tagManagementEnabled.value) {
          try {
            const tagsResponse = await getUserTagList();
            const tagListString = tagsResponse.value || '';
            userTags.value = tagListString ? tagListString.split('|-|').filter(tag => tag.trim()) : [];
          } catch (error) {
            console.log('获取标签列表失败，使用空列表');
            userTags.value = [];
          }
        }

      } catch (error) {
        console.error('加载用户设置失败:', error);
        uni.showToast({
          title: '加载设置失败',
          icon: 'none'
        });
      } finally {
        loading.value = false;
      }
    };

    // 标签管理开关切换
    const onTagManagementToggle = async (event) => {
      const enabled = event.detail.value;

      try {
        await setUserTagManagementEnabled(enabled);
        tagManagementEnabled.value = enabled;

        if (enabled) {
          // 启用时加载标签列表
          await loadUserSettings();
        } else {
          // 禁用时清空标签列表
          userTags.value = [];
        }

        uni.showToast({
          title: enabled ? '标签管理已启用' : '标签管理已禁用',
          icon: 'success'
        });

      } catch (error) {
        console.error('切换标签管理状态失败:', error);
        uni.showToast({
          title: '设置失败',
          icon: 'none'
        });
      }
    };

    // 计算标签列表总长度
    const getTotalLength = () => {
      return userTags.value.join('|-|').length;
    };

    // 获取标签的标题部分用于显示
    const getTagTitle = (tag) => {
      return tag.includes('|') ? tag.split('|')[0].trim() : tag;
    };

    // 验证标签格式 - 根据后端验证规则
    const validateTag = (title, content) => {
      if (!title || !title.trim()) {
        return { valid: false, message: '标题不能为空' };
      }

      const trimmedTitle = title.trim();
      const trimmedContent = content ? content.trim() : '';

      // 标题是必须的
      if (!trimmedTitle) {
        return { valid: false, message: '标题不能为空' };
      }

      return { valid: true, message: '格式正确' };
    };

    // 显示添加标签对话框
    const showAddTagDialog = () => {
      if (getTotalLength() >= 100) {
        uni.showToast({
          title: '标签总长度已达到100字符限制',
          icon: 'none'
        });
        return;
      }
      newTagTitle.value = '';
      newTagContent.value = '';
      showAddDialog.value = true;
    };

    // 隐藏添加标签对话框
    const hideAddTagDialog = () => {
      showAddDialog.value = false;
      newTagTitle.value = '';
      newTagContent.value = '';
    };

    // 添加标签
    const addTag = async () => {
      const title = newTagTitle.value.trim();
      const content = newTagContent.value.trim();

      // 基本检查
      if (!title) {
        uni.showToast({
          title: '请输入标签标题',
          icon: 'none',
          duration: 2000
        });
        return;
      }

      // 构建完整标签（后台自动拼接）
      const fullTag = content ? `${title}|${content}` : title;

              // 检查长度限制
        const currentLength = getTotalLength();
        const newTotalLength = currentLength + (currentLength > 0 ? 3 : 0) + fullTag.length; // +3是|-|分隔符
      
      if (newTotalLength > 100) {
        uni.showToast({
          title: `添加此标签将超过100字符限制（当前${currentLength}，添加后${newTotalLength}）`,
          icon: 'none',
          duration: 3000
        });
        return;
      }

      // 检查是否重复（只检查标题部分）
      const existingTitles = userTags.value.map(tag => {
        return tag.includes('|') ? tag.split('|')[0].trim() : tag;
      });
      
      if (existingTitles.includes(title)) {
        uni.showToast({
          title: '相同标题的标签已存在',
          icon: 'none',
          duration: 2000
        });
        return;
      }

      // 验证标签格式
      const validation = validateTag(title, content);
      if (!validation.valid) {
        uni.showToast({
          title: validation.message,
          icon: 'none',
          duration: 3000
        });
        return;
      }

      try {
        // 添加到本地列表
        const newTags = [...userTags.value, fullTag];

        // 保存到服务器
        await setUserTagList(newTags.join('|-|'));

        // 更新本地状态
        userTags.value = newTags;

        // 关闭对话框
        hideAddTagDialog();

        uni.showToast({
          title: '标签添加成功',
          icon: 'success',
          duration: 1500
        });

      } catch (error) {
        console.error('添加标签失败:', error);

        // 根据错误类型显示不同的提示
        let errorMessage = '添加失败';
        if (error.message && error.message.includes('验证失败')) {
          errorMessage = '标签格式不正确';
        } else if (error.message && error.message.includes('网络')) {
          errorMessage = '网络错误，请重试';
        }

        uni.showToast({
          title: errorMessage,
          icon: 'none',
          duration: 2000
        });
      }
    };

    // 删除标签
    const removeTag = async (index) => {
      try {
        // 从本地列表中删除
        const newTags = userTags.value.filter((_, i) => i !== index);

        // 保存到服务器
        await setUserTagList(newTags.join('|-|'));

        // 更新本地状态
        userTags.value = newTags;

        uni.showToast({
          title: '标签删除成功',
          icon: 'success'
        });

      } catch (error) {
        console.error('删除标签失败:', error);
        uni.showToast({
          title: '删除失败',
          icon: 'none'
        });
      }
    };

    const goBack = () => {
      uni.navigateBack({
        delta: 1
      });
    };

    const navigateToReminderMethod = () => {
      uni.showToast({
        title: '提醒方式功能开发中',
        icon: 'none',
        duration: 2000
      });
    };

    return {
      // 响应式数据
      tagManagementEnabled,
      userTags,
      showAddDialog,
      newTagTitle,
      newTagContent,
      loading,

      // 方法
      goBack,
      navigateToReminderMethod,
      onTagManagementToggle,
      getTotalLength,
      getTagTitle,
      showAddTagDialog,
      hideAddTagDialog,
      addTag,
      removeTag
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
  padding: calc(var(--status-bar-height, 44rpx) + 80rpx) 32rpx 48rpx;
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

/* 内容区域 */
.content-scroll {
  flex: 1;
  overflow-y: auto;
  background-color: #fcfbf8;
}

.content-container {
  padding: 16rpx 32rpx 32rpx;
}

/* 分类区域 */
.category-section {
  margin-bottom: 40rpx;
}

.category-header {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 16rpx;
  padding: 0 8rpx;
}

.category-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56rpx;
  height: 56rpx;
  background-color: #f4efe7;
  border-radius: 16rpx;
  flex-shrink: 0;
}

.category-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1c170d;
  line-height: 1.2;
}

.category-card {
  background-color: #ffffff;
  border: 2rpx solid #f4efe7;
  border-radius: 24rpx;
  box-shadow: 0 6rpx 24rpx rgba(28, 23, 13, 0.08);
  overflow: hidden;
}

/* 设置项 */
.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
  transition: all 0.2s ease;
  cursor: pointer;
}

.setting-item:active {
  background-color: #fcfbf8;
}

.item-content {
  display: flex;
  align-items: center;
  gap: 24rpx;
  flex: 1;
}

.item-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72rpx;
  height: 72rpx;
  background-color: #f4efe7;
  border-radius: 20rpx;
  flex-shrink: 0;
}

.icon-text {
  font-size: 36rpx;
}

.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.item-label {
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
  line-height: 1.2;
}

.item-description {
  font-size: 24rpx;
  color: #9d8148;
  line-height: 1.4;
}

.item-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48rpx;
  height: 48rpx;
  flex-shrink: 0;
}

.arrow-icon {
  font-size: 48rpx;
  color: #cccccc;
  font-weight: 300;
}

.item-switch {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 标签管理区域 */
.tag-management-section {
  padding: 0;
}

.divider {
  height: 1rpx;
  background-color: #f4efe7;
  margin: 0 32rpx;
}

.tag-editor {
  padding: 32rpx;
}

.tag-editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.tag-editor-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1c170d;
}

.tag-count {
  font-size: 24rpx;
  color: #9d8148;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 24rpx;
  min-height: 80rpx;
  align-items: flex-start;
  align-content: flex-start;
}

.tag-item {
  display: flex;
  align-items: center;
  background-color: #f7bd4a;
  border-radius: 20rpx;
  padding: 12rpx 16rpx;
  gap: 8rpx;
  max-width: 300rpx;
}

.tag-content {
  flex: 1;
  overflow: hidden;
}

.tag-text {
  font-size: 24rpx;
  color: #1c170d;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}



.tag-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32rpx;
  height: 32rpx;
  background-color: rgba(28, 23, 13, 0.1);
  border-radius: 50%;
  flex-shrink: 0;
}

.remove-icon {
  font-size: 24rpx;
  color: #1c170d;
  font-weight: 600;
  line-height: 1;
}

.tag-add {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80rpx;
  height: 56rpx;
  background-color: #f4efe7;
  border: 2rpx dashed #cccccc;
  border-radius: 20rpx;
  transition: all 0.2s ease;
}

.tag-add:active {
  background-color: #f7bd4a;
  border-color: #f7bd4a;
}

.add-icon {
  font-size: 32rpx;
  color: #9d8148;
  font-weight: 300;
}

.tag-tips {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.tip-text {
  font-size: 22rpx;
  color: #9d8148;
  line-height: 1.4;
}

/* 对话框样式 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog-container {
  background-color: #ffffff;
  border-radius: 24rpx;
  margin: 32rpx;
  max-width: 600rpx;
  width: 100%;
  box-shadow: 0 12rpx 48rpx rgba(0, 0, 0, 0.2);
}

.dialog-header {
  padding: 32rpx 32rpx 16rpx;
  border-bottom: 1rpx solid #f4efe7;
}

.dialog-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1c170d;
  text-align: center;
}

.dialog-content {
  padding: 32rpx;
}

.input-wrapper {
  margin-bottom: 24rpx;
}

.input-wrapper:last-of-type {
  margin-bottom: 16rpx;
}

.input-label {
  display: flex;
  align-items: center;
  margin-bottom: 8rpx;
  gap: 4rpx;
}

.label-text {
  font-size: 28rpx;
  color: #1c170d;
  font-weight: 500;
}

.required-mark {
  font-size: 28rpx;
  color: #ff4757;
  font-weight: 600;
}

.tag-input {
  width: 100%;
  height: 80rpx;
  background-color: #f4efe7;
  border: 2rpx solid #f4efe7;
  border-radius: 16rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #1c170d;
  box-sizing: border-box;
}

.tag-input:focus {
  border-color: #f7bd4a;
  background-color: #ffffff;
}

.input-tips {
  margin-top: 8rpx;
}

.dialog-actions {
  display: flex;
  border-top: 1rpx solid #f4efe7;
}

.dialog-btn {
  flex: 1;
  height: 88rpx;
  background-color: transparent;
  border: none;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 0;
  margin: 0;
  padding: 0;
}

.cancel-btn {
  color: #9d8148;
  border-right: 1rpx solid #f4efe7;
}

.confirm-btn {
  color: #f7bd4a;
}

.dialog-btn:active {
  background-color: #f4efe7;
}

/* 底部间距 */
.bottom-spacer {
  height: 120rpx;
}

/* 响应式调整 */
@media (max-width: 750rpx) {
  .header-section {
    padding: calc(var(--status-bar-height, 44rpx) + 64rpx) 24rpx 32rpx;
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

  .content-container {
    padding: 12rpx 24rpx 24rpx;
  }

  .setting-item {
    padding: 24rpx;
  }

  .item-icon {
    width: 64rpx;
    height: 64rpx;
  }

  .icon-text {
    font-size: 32rpx;
  }

  .item-label {
    font-size: 28rpx;
  }

  .item-description {
    font-size: 22rpx;
  }
}
</style>