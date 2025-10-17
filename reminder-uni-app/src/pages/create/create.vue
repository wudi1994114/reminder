<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="nav-header">
      <view class="nav-close" @click="cancel">
        <text class="close-icon">×</text>
      </view>
    </view>
    
    <!-- 主要内容区域 -->
    <scroll-view class="content-scroll" scroll-y>
      <!-- 标题输入 -->
      <view class="input-section">
        <input 
          class="title-input" 
          v-model="reminderForm.title" 
          placeholder="标题"
          placeholder-class="input-placeholder"
          maxlength="50"
        />
      </view>
      
      <!-- 内容输入 -->
      <view class="input-section">
        <view class="textarea-container">
          <textarea
            class="content-textarea"
            v-model="reminderForm.description"
            placeholder="内容"
            placeholder-class="input-placeholder"
            maxlength="200"
            :style="{ height: textareaHeight + 'px' }"
            @linechange="handleLineChange"
          />
          <view class="quick-tags-section">
            <view class="quick-tags">
              <view
                v-for="tag in userTags"
                :key="tag"
                class="tag-item"
                @click="addQuickTag(tag)"
              >
                <text class="tag-text">{{ getTagTitle(tag) }}</text>
              </view>

              <view v-if="userTags.length === 0" class="tag-settings-hint" @click="goToTagSettings">
                <text class="hint-text">设置标签</text>
              </view>
            </view>

          </view>
        </view>
      </view>
      
      <!-- 提醒方式 -->
      <view class="setting-item" @click="showReminderTypeSelector">
        <text class="setting-label">提醒方式</text>
        <text class="setting-value">{{ getReminderTypeText(reminderForm.reminderType) }}</text>
      </view>


          <!-- 底部保存按钮 -->
      <view class="bottom-container">
        <button 
          class="save-button" 
          @click="saveReminder" 
          :disabled="isSubmitting"
          :class="{ 'button-loading': isSubmitting }"
        >
          <text class="button-text" v-if="!isSubmitting">{{ isEdit ? '更新提醒' : '保存提醒' }}</text>
          <text class="button-text" v-else>保存中...</text>
        </button>
        <view class="bottom-spacer"></view>
      </view>
        
      <!-- 时间设置 -->
      <datetime-picker 
        v-if="isDataReady"
        ref="dateTimePickerRef"
        label="时间设置"
        displayMode="datetime"
        :initial-date="reminderDate"
        :initial-time="reminderTime"
        :auto-set-default="!isEdit"
        @change="onDateTimeChange"
      />
      <view v-else class="loading-placeholder">
        <text>加载中...</text>
      </view>
    </scroll-view>
    

    

  </view>
</template>

<script>
import { ref, computed, reactive, onMounted, getCurrentInstance, nextTick, watch } from 'vue';
import {
  createEvent,
  updateEvent
} from '@/services/cachedApi';
import {
  getSimpleReminderById,
  smartRequestSubscribe,
  getUserTagManagementEnabled,
  getUserTagList,
  increaseWechatAuthCount
} from '@/services/api';
import { updateDataVersion } from '@/services/reminderCache';
import { requireAuth } from '@/utils/auth';
import { FeatureControl, isProductionVersion, isDevelopmentVersion } from '@/config/version';
import UnifiedTimePicker from '@/components/unified-time-picker/unified-time-picker.vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';


export default {
  onLoad(options) {
    console.log('创建页面: onLoad 接收到参数:', options);
    // 将参数存储到全局变量中供setup使用
    if (typeof window !== 'undefined') {
      window._createPageOptions = { ...options };
      console.log('创建页面: onLoad 存储到window._createPageOptions:', window._createPageOptions);
    }
  },
  setup() {
    console.log('创建页面: setup函数开始执行');
    
    // 1. 定义响应式数据
    const isEdit = ref(false);
    const isDataReady = ref(false); // 用于控制组件渲染时机
    const originalReminderType = ref(null); // 用于追踪原始的提醒方式
    
    const reminderForm = reactive({
      id: null,
      title: '',
      description: '',
      eventTime: '',
      reminderType: isProductionVersion() ? 'WECHAT_MINI' : 'WECHAT_MINI', // 设置默认提醒方式，不再使用空字符串
      status: 'PENDING'
    });
    
    // 2. 初始化默认值（今天的日期和当前时间+2分钟）
    const today = new Date();
    const defaultDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    const now = new Date();
    now.setMinutes(now.getMinutes() + 2); // 当前时间+2分钟
    const defaultTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
    
    const reminderDate = ref(defaultDate);
    const reminderTime = ref(defaultTime);
    const isSubmitting = ref(false);
    const dateTimePickerRef = ref(null);
    
    // 3. 提醒方式相关
    const reminderTypeOptions = ref([]);
    const reminderTypeValues = ref([]);
    const reminderTypeIndex = ref(0);

    // 4. 用户标签相关
    const userTags = ref([]);
    const tagManagementEnabled = ref(false);
    
    // 5. 新增用于手动控制 textarea 高度的响应式数据
    const initialTextareaHeight = ref(0);
    const textareaHeight = ref(0);
    
    // 6. 时间选择器显示状态
    const isTimePickerVisible = ref(false);
    
    // 4. 统一的参数处理函数
    const processPageOptions = () => {
      let options = {};
      
      // 尝试获取页面参数
      if (typeof window !== 'undefined' && window._createPageOptions) {
        options = window._createPageOptions;
        console.log('创建页面: 获取到页面参数:', options);
      } else {
        try {
          const pages = getCurrentPages();
          const currentPage = pages[pages.length - 1];
          options = currentPage.options || {};
          console.log('创建页面: 从getCurrentPages获取到参数:', options);
        } catch (error) {
          console.log('创建页面: 获取页面参数失败:', error);
        }
      }
      
      const id = options.id || null;
      const mode = options.mode || '';
      const initialDate = options.date || '';
      
      console.log('创建页面: 解析参数:', { id, mode, initialDate });
      
      // 设置编辑模式
      isEdit.value = mode === 'edit' && id;
      
      // 如果有传入的日期，立即设置
      if (initialDate) {
        reminderDate.value = initialDate;
        console.log('创建页面: 设置传入的日期:', initialDate);
      }
      
      return { id }; // 2. 精简返回值
    };
    
    // 5. 在onMounted中统一处理
    onMounted(async () => {
      // 首先检查登录状态
      const isAuthenticated = await requireAuth();
      
      if (!isAuthenticated) {
        // 用户未登录且拒绝登录，返回上一页
        console.log('用户未登录，返回上一页');
        uni.navigateBack({
          fail: () => {
            // 如果没有上一页，跳转到首页
            uni.switchTab({
              url: '/pages/index/index'
            });
          }
        });
        return;
      }

      // 在页面加载时，将 rpx 转换为 px，并设置为初始高度
      // 288 是原本设置的 min-height 值
      const initialHeightInPx = uni.upx2px(288);
      initialTextareaHeight.value = initialHeightInPx;
      textareaHeight.value = initialHeightInPx;

      initReminderTypes(); // 初始化提醒选项
      await loadUserTags(); // 加载用户标签

      const { id } = processPageOptions();
      
      if (isEdit.value && id) {
        // 编辑模式：加载现有数据
        try {
          isSubmitting.value = true;
          const result = await getSimpleReminderById(id);
          if (result) {
            reminderForm.id = result.id;
            reminderForm.title = result.title;
            reminderForm.description = result.description;
            reminderForm.eventTime = result.eventTime;
            reminderForm.status = result.status;
            reminderForm.reminderType = result.reminderType || 'EMAIL';
            originalReminderType.value = reminderForm.reminderType; // 3. 记录原始类型
            
            // 解析日期时间
            if (result.eventTime) {
              let eventTimeStr = result.eventTime;
              if (eventTimeStr.includes('T')) {
                const eventDate = new Date(eventTimeStr);
                if (!isNaN(eventDate.getTime())) {
                  const year = eventDate.getFullYear();
                  const month = String(eventDate.getMonth() + 1).padStart(2, '0');
                  const day = String(eventDate.getDate()).padStart(2, '0');
                  const hours = String(eventDate.getHours()).padStart(2, '0');
                  const minutes = String(eventDate.getMinutes()).padStart(2, '0');
                  
                  reminderDate.value = `${year}-${month}-${day}`;
                  reminderTime.value = `${hours}:${minutes}`;
                }
              } else {
                const [date, time] = eventTimeStr.split(' ');
                reminderDate.value = date;
                reminderTime.value = time ? time.substring(0, 5) : '09:00';
              }
            }
            
            // 设置提醒方式索引
            const typeIndex = reminderTypeValues.value.indexOf(reminderForm.reminderType);
            reminderTypeIndex.value = typeIndex >= 0 ? typeIndex : 0;
          }
        } catch (error) {
          console.error('获取提醒详情失败:', error);
          uni.showToast({title: '加载提醒数据失败', icon: 'none'});
        } finally {
          isSubmitting.value = false;
        }
      } else {
        // 创建模式：使用初始值
        console.log('创建页面: 创建模式，使用初始值');
        originalReminderType.value = reminderForm.reminderType; // 4. 创建模式也记录初始值
        
        // 设置提醒方式索引以匹配默认的提醒类型
        const typeIndex = reminderTypeValues.value.indexOf(reminderForm.reminderType);
        reminderTypeIndex.value = typeIndex >= 0 ? typeIndex : 0;
      }
      
      // 更新事件时间
      reminderForm.eventTime = `${reminderDate.value} ${reminderTime.value}:00`;
      console.log('创建页面: 设置初始eventTime:', reminderForm.eventTime);
      
      // 标记数据已准备好
      isDataReady.value = true;
      console.log('创建页面: 数据准备完成，组件可以正常渲染');
    });
    
    // 日期时间组件变化处理
    const onDateTimeChange = (dateTimeData) => {
      reminderDate.value = dateTimeData.date;
      reminderTime.value = dateTimeData.time;
      reminderForm.eventTime = dateTimeData.eventTime;
    };
    
    // 提醒方式相关方法
    const onReminderTypeChange = (e) => {
      reminderTypeIndex.value = e.detail.value;
      reminderForm.reminderType = reminderTypeValues[e.detail.value];
    };
    
    const getReminderTypeIcon = (type) => {
      switch (type) {
        case 'EMAIL': return '📧';
        case 'SMS': return '📱';
        case 'WECHAT_MINI': return '💬';
        default: return '📧';
      }
    };
    
    const getReminderTypeText = (type) => {
      const typeMap = {
        'EMAIL': '邮件',
        'SMS': '短信',
        'WECHAT_MINI': '微信'
      };
      return typeMap[type] || '邮件';
    };

    // 统一的微信授权处理方法
    const handleWechatAuthorization = async () => {
      if (!needWechatSubscribe()) {
        return true; // 不需要授权，直接通过
      }

      try {
        console.log('📱 需要请求微信订阅权限');
        const subscribeResult = await smartRequestSubscribe({
          showToast: false  // 不显示自动提示，由我们控制
        });
        
        if (!subscribeResult.success || !subscribeResult.granted) {
          console.log('⚠️ 微信订阅权限获取失败，引导用户去设置');
          return new Promise((resolve) => {
            uni.showModal({
              title: '微信提醒需要授权',
              content: '检测到您未开启微信订阅消息权限，是否前往设置页面进行授权？',
              confirmText: '去设置',
              cancelText: '取消',
              success: (res) => {
                if (res.confirm) {
                  // 用户选择去设置
                  uni.openSetting({
                    success: (settingRes) => {
                      if (settingRes.authSetting['scope.subscribeMessage']) {
                        uni.showToast({
                          title: '授权成功，请重新保存',
                          icon: 'success'
                        });
                      } else {
                        uni.showToast({
                          title: '您未授权微信提醒',
                          icon: 'none'
                        });
                      }
                    }
                  });
                } else {
                  // 用户选择取消，停留在当前页面
                  uni.showToast({
                    title: '已取消保存，可选择其他提醒方式',
                    icon: 'none'
                  });
                }
                resolve(false); // 授权失败或用户取消
              }
            });
          });
        }
        
        console.log('✅ 微信订阅权限获取成功');
        
        // 授权成功后，增加服务器端的微信授权次数
        try {
          await increaseWechatAuthCount(1);
          console.log('✅ 微信授权次数已增加1次');
        } catch (error) {
          console.error('❌ 增加微信授权次数失败:', error);
          // 即使增加次数失败，也继续创建提醒
        }
        
        return true; // 授权成功
        
      } catch (error) {
        console.error('❌ 请求微信订阅权限失败:', error);
        uni.showToast({
          title: '无法获取微信权限，请重试',
          icon: 'none',
          duration: 3000
        });
        return false; // 授权失败
      }
    };

    const saveReminder = async () => {
      if (!reminderForm.title) {
        uni.showToast({ title: '请输入提醒标题', icon: 'none' });
        return;
      }
      if (!reminderForm.eventTime) {
        uni.showToast({ title: '请选择提醒时间', icon: 'none' });
        return;
      }
      
      // 统一的微信授权处理
      const authResult = await handleWechatAuthorization();
      if (!authResult) {
        return; // 授权失败或用户取消，停止保存
      }
      
      // 执行保存
      await performSave();
    };
    
    // 抽取保存逻辑为独立函数
    const performSave = async () => {
      isSubmitting.value = true;
      try {
        let result;
        const dataToSave = { 
          ...reminderForm
        };
        
        // 确保提醒方式不为空
        if (!dataToSave.reminderType || dataToSave.reminderType === '') {
          const defaultType = reminderTypeValues.value[0] || 'WECHAT_MINI';
          dataToSave.reminderType = defaultType;
          console.warn('简单提醒：提醒方式为空，已设置为默认值:', defaultType);
        }
        
        // 将eventTime转换为ISO 8601格式
        if (dataToSave.eventTime) {
          // 将 "YYYY-MM-DD HH:mm:ss" 格式转换为 iOS 兼容的格式，然后转为 ISO 8601
          const eventTimeStr = dataToSave.eventTime.replace(' ', 'T'); // 转换为 "YYYY-MM-DDTHH:mm:ss" 格式
          const eventDate = new Date(eventTimeStr);
          dataToSave.eventTime = eventDate.toISOString();
        }
        
        // 移除不需要的字段
        delete dataToSave.toUserId; // 让后端自动设置
        delete dataToSave.status; // 后端会设置默认状态

        if (isEdit.value) {
          result = await updateEvent(reminderForm.id, dataToSave);
        } else {
          result = await createEvent(dataToSave);
        }
        
        if (result) {
          // 更新全局数据版本，通知其他页面数据已变更
          updateDataVersion();
          console.log('✅ 简单提醒保存成功，全局数据版本已更新');
          
          uni.showToast({
            title: isEdit.value ? '修改成功' : '创建成功',
            icon: 'success',
            duration: 500
          });
          
          console.log('保存成功，0.5秒后返回');
          
          setTimeout(() => {
            console.log('创建页面: 准备返回日历页面');
            // 检查页面栈，如果只有一个页面则跳转到首页，否则返回上一页
            const pages = getCurrentPages();
            if (pages.length <= 1) {
              console.log('当前是第一个页面，跳转到首页');
              uni.reLaunch({
                url: '/pages/index/index'
              });
            } else {
              console.log('返回上一页');
              uni.navigateBack();
            }
          }, 1500);
        } else {
           // API已在内部处理错误提示，这里可以不重复提示
        }
      } catch (error) {
        console.error('保存失败:', error);
        // API已在内部处理错误提示
              } finally {
        isSubmitting.value = false;
      }
    };
    
    const cancel = () => {
      // 检查页面栈，如果只有一个页面则跳转到首页，否则返回上一页
      const pages = getCurrentPages();
      if (pages.length <= 1) {
        console.log('取消操作: 当前是第一个页面，跳转到首页');
        uni.reLaunch({
          url: '/pages/index/index'
        });
      } else {
        console.log('取消操作: 返回上一页');
        uni.navigateBack();
      }
    };
    
    // 检查是否需要请求微信订阅权限
    const needWechatSubscribe = () => {
      // 只有微信小程序环境才需要检查
      // #ifdef MP-WEIXIN
      // 如果提醒方式不是微信，则不需要
      if (reminderForm.reminderType !== 'WECHAT_MINI') {
        return false;
      }
      
      // 如果是创建新提醒，且选择了微信提醒，则需要授权
      if (!isEdit.value) {
        console.log('🔍 [创建模式] 用户选择微信提醒，需要请求订阅权限');
        return true;
      }
      
      // 如果是编辑模式，只有当提醒方式从非微信改为微信时，才需要授权
      if (isEdit.value && originalReminderType.value !== 'WECHAT_MINI') {
        console.log('🔍 [编辑模式] 用户从其他方式改为微信提醒，需要请求订阅权限');
        return true;
      }
      
      console.log('🤔 [编辑模式] 提醒方式未更改或已是微信提醒，无需重复请求授权');
      return false;
      // #endif
      
      // #ifndef MP-WEIXIN
      return false;
      // #endif
    };

    // 新增方法：显示提醒方式选择器
    const showReminderTypeSelector = () => {
      uni.showActionSheet({
        itemList: reminderTypeOptions.value,
        success: (res) => {
          const selectedType = reminderTypeValues.value[res.tapIndex];
          
          reminderTypeIndex.value = res.tapIndex;
          reminderForm.reminderType = selectedType;
          
          console.log('提醒方式已设置为:', selectedType);
        }
      });
    };
    
    const initReminderTypes = () => {
      let options = [];
      let values = [];

      if (isProductionVersion()) {
        // 正式环境
        options = ['微信', '邮件', '手机'];
        values = ['WECHAT_MINI', 'EMAIL', 'SMS'];
      } else {
        // 开发和测试环境
        options = ['微信'];
        values = ['WECHAT_MINI'];
      }

      reminderTypeOptions.value = options;
      reminderTypeValues.value = values;
      
      // 如果提醒类型为空或无效，设置为第一个可用选项
      if (!reminderForm.reminderType || !values.includes(reminderForm.reminderType)) {
        reminderForm.reminderType = values[0];
        console.log('设置默认提醒方式为:', values[0]);
      }
      
      // 设置对应的索引
      const defaultIndex = values.indexOf(reminderForm.reminderType);
      reminderTypeIndex.value = defaultIndex !== -1 ? defaultIndex : 0;
    };

    // 加载用户标签
    const loadUserTags = async () => {
      try {
        // 检查标签管理是否启用
        try {
          const enabledResponse = await getUserTagManagementEnabled();
          tagManagementEnabled.value = enabledResponse.value === '1';
        } catch (error) {
          console.log('标签管理功能未启用');
          tagManagementEnabled.value = false;
          userTags.value = [];
          return;
        }

        // 如果启用了标签管理，获取标签列表
        if (tagManagementEnabled.value) {
          try {
            const tagsResponse = await getUserTagList();
            const tagListString = tagsResponse.value || '';
            console.log('🏷️ 简单提醒页面 - 获取到的标签字符串:', tagListString);
            
            userTags.value = tagListString ? tagListString.split('|-|').filter(tag => tag.trim()) : [];
            console.log('🏷️ 简单提醒页面 - 最终标签数组:', userTags.value);
          } catch (error) {
            console.log('获取标签列表失败，使用空列表');
            userTags.value = [];
          }
        } else {
          userTags.value = [];
        }

      } catch (error) {
        console.error('加载用户标签失败:', error);
        userTags.value = [];
      }
    };

    // 新增 linechange 事件处理函数
    const handleLineChange = (event) => {
      // event.detail = { height, heightRpx, lineCount }
      const contentHeight = event.detail.height;

      // 核心逻辑：取 "初始高度" 和 "内容实际高度" 中的最大值
      textareaHeight.value = Math.max(initialTextareaHeight.value, contentHeight);
    };

    // 添加快捷标签到内容
    const addQuickTag = (tag) => {
      // 解析标签：检查是否包含|分隔符
      let titlePart = tag;
      let contentPart = tag;
      
      if (tag.includes('|')) {
        const parts = tag.split('|', 2);
        titlePart = parts[0].trim();
        contentPart = parts[1].trim();
      }

      // 检测title是否为空，为空则将标签的标题部分给title
      if (!reminderForm.title || reminderForm.title.trim() === '') {
        reminderForm.title = titlePart;
      }

      if (!reminderForm.description) {
        reminderForm.description = contentPart;
      } else {
        // 如果内容不为空，在末尾添加标签的内容部分
        const currentText = reminderForm.description.trim();
        // 添加空格分隔符，允许多次添加相同标签
        reminderForm.description = currentText + (currentText ? ' ' : '') + contentPart;
      }

      // 限制长度不超过200字符
      if (reminderForm.description.length > 200) {
        reminderForm.description = reminderForm.description.substring(0, 200);
      }

      // 提供触觉反馈（仅在真机上，开发环境中禁用以避免开发者工具的屏幕放大效果）
      // #ifdef MP-WEIXIN
      if (!isDevelopmentVersion()) {
        uni.vibrateShort({
          fail: () => {
            // 忽略失败，不是所有设备都支持震动
          }
        });
      }
      // #endif
    };



    // 获取标签的标题部分用于显示
    const getTagTitle = (tag) => {
      return tag.includes('|') ? tag.split('|')[0].trim() : tag;
    };

    // 跳转到标签设置页面
    const goToTagSettings = async () => {
      // 检查登录状态，未登录会弹出登录框并等待
      const isLoggedIn = await requireAuth();
      if (!isLoggedIn) {
        console.log('用户取消登录，不跳转标签设置页面');
        return;
      }
      
      uni.navigateTo({
        url: '/pages/settings/notification'
      });
    };

    return {
      isEdit,
      isDataReady,
      reminderForm,
      reminderDate,
      reminderTime,
      isSubmitting,
      reminderTypeOptions,
      reminderTypeIndex,
      dateTimePickerRef,
      userTags,
      tagManagementEnabled,
      initialTextareaHeight,
      textareaHeight,
      onDateTimeChange,
      onReminderTypeChange,
      getReminderTypeIcon,
      getReminderTypeText,
      saveReminder,
      performSave,
      handleWechatAuthorization,
      needWechatSubscribe,
      loadUserTags,
      handleLineChange,
      addQuickTag,
      getTagTitle,
      goToTagSettings
    };
  }
};
</script>

<style scoped>
/* 页面容器 */
.page-container {
  height: 100vh;
  background-color: #fcfbf8;
  display: flex;
  flex-direction: column;
  font-family: 'Manrope', 'Noto Sans', sans-serif;
}

/* 导航栏样式 */
.nav-header {
  display: flex;
  align-items: center;
  background-color: #fcfbf8;
  padding: 32rpx 32rpx 16rpx;
  justify-content: space-between;
}

.nav-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 96rpx;
  height: 96rpx;
  color: #1c170d;
  cursor: pointer;
}

.close-icon {
  font-size: 48rpx;
  color: #1c170d;
}

.nav-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #1c170d;
  text-align: center;
  flex: 1;
  padding-right: 96rpx;
  line-height: 1.2;
  letter-spacing: -0.015em;
}

/* 内容滚动区域 */
.content-scroll {
  flex: 1;
  padding: 0;
}

/* 输入区域样式 */
.input-section {
  padding: 24rpx 32rpx;
}

.title-input {
  width: 100%;
  min-height: 112rpx;
  padding: 32rpx;
  background-color: #f4efe7;
  border-radius: 24rpx;
  border: none;
  font-size: 32rpx;
  color: #1c170d;
  line-height: 1.4;
}

/* 文本区域容器 */
.textarea-container {
  position: relative;
  background-color: #f4efe7;
  border-radius: 24rpx;
  overflow: hidden;
}

.content-textarea {
  width: 100%;
  /* min-height 已移除，由 JS 控制 */
  padding: 32rpx 32rpx 120rpx 32rpx; /* 底部留出空间给标签 */
  background-color: transparent;
  border: none;
  font-size: 32rpx;
  color: #1c170d;
  line-height: 1.4;
  resize: none;
  transition: height 0.1s ease-in-out;
}

.input-placeholder {
  color: #9d8148;
}

/* 快捷标签区域 */
.quick-tags-section {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: transparent;
  padding: 16rpx 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.quick-tags {
  display: flex;
  align-items: center;
  gap: 16rpx;
  flex: 1;
}

.tag-item {
  background-color: #f7bd4a;
  border-radius: 16rpx;
  padding: 6rpx 12rpx;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4rpx 8rpx rgba(0, 0, 0, 0.15);
}

.tag-item:active {
  background-color: #e6a73d;
  transform: scale(0.95) translateY(1rpx);
  box-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.1);
}

.tag-text {
  font-size: 22rpx;
  color: #1c170d;
  font-weight: 500;
}

.tag-settings-hint {
  background-color: #e9e0ce;
  border: 2rpx dashed #cccccc;
  border-radius: 16rpx;
  padding: 6rpx 12rpx;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tag-settings-hint:active {
  background-color: #f7bd4a;
  border-color: #f7bd4a;
}

.hint-text {
  font-size: 22rpx;
  color: #9d8148;
  font-weight: 500;
}



/* 设置项样式 */
.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32rpx;
  background-color: #fcfbf8;
  padding: 32rpx;
  min-height: 112rpx;
  cursor: pointer;
}

.setting-item:active {
  background-color: #f4efe7;
}

.setting-label {
  font-size: 32rpx;
  font-weight: 400;
  color: #1c170d;
  line-height: 1.4;
  flex: 1;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}

.setting-value {
  font-size: 32rpx;
  font-weight: 400;
  color: #1c170d;
  line-height: 1.4;
  flex-shrink: 0;
}

/* 底部容器 */
.bottom-container {
  background-color: #fcfbf8;
  position: fixed;
  width: 100%;
  z-index: 1;
  bottom: 0;
}

.save-button {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 168rpx;
  max-width: 960rpx;
  height: 96rpx;
  margin: 0 32rpx;
  padding: 0 40rpx;
  background-color: #f7bd4a;
  border-radius: 48rpx;
  border: none;
  cursor: pointer;
  overflow: hidden;
}

.save-button:active {
  background-color: #e6a73d;
}

.save-button:disabled,
.button-loading {
  background-color: #d3d4d6;
  cursor: not-allowed;
}

.button-text {
  font-size: 32rpx;
  font-weight: 700;
  color: #1c170d;
  line-height: 1.4;
  letter-spacing: 0.015em;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}

.bottom-spacer {
  height: 40rpx;
  background-color: #fcfbf8;
}

/* 加载状态动画 */
.button-loading::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  animation: loading 1.5s infinite;
}

@keyframes loading {
  0% {
    left: -100%;
  }
  100% {
    left: 100%;
  }
}

/* 加载占位符样式 */
.loading-placeholder {
  padding: 32rpx;
  text-align: center;
  background-color: #f4efe7;
  border-radius: 24rpx;
  margin: 16rpx 0;
}

.loading-placeholder text {
  color: #9d8148;
  font-size: 28rpx;
}

/* 响应式适配 */
@media (max-width: 750rpx) {
  .nav-header {
    padding: 24rpx 24rpx 12rpx;
  }
  
  .input-section {
    padding: 20rpx 24rpx;
  }
  
  .setting-item {
    padding: 24rpx;
    min-height: 96rpx;
  }
  
  .save-button {
    margin: 0 24rpx;
    height: 88rpx;
  }
  
  .nav-title {
    font-size: 32rpx;
  }
  
  .title-input {
    font-size: 28rpx;
    padding: 24rpx;
  }

  .content-textarea {
    font-size: 28rpx;
    padding: 24rpx 24rpx 100rpx 24rpx; /* 小屏幕上减少底部间距 */
  }

  .quick-tags-section {
    padding: 12rpx 20rpx;
    gap: 12rpx;
  }

  .quick-tags {
    gap: 12rpx;
  }

  .tag-item,
  .tag-settings-hint {
    padding: 4rpx 10rpx;
  }

  .tag-text,
  .hint-text {
    font-size: 20rpx;
  }


  
  .setting-label,
  .setting-value {
    font-size: 28rpx;
  }
  
  .button-text {
    font-size: 28rpx;
  }
}

/* 选择器显示样式 */
.picker-display {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
</style> 