<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="header-section">
      <view class="nav-container">
        <view class="nav-back" @click="goBack">
          <text class="back-icon">‹</text>
        </view>
        <view class="title-container">
          <text class="page-title">提醒设置</text>
        </view>
        <view class="nav-actions">
          <view class="reset-btn" @click="confirmReset">
            <text class="reset-text">重置</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 主要内容区域 -->
    <scroll-view class="content-scroll" scroll-y>
      <view class="content-container">
        <!-- 加载状态 -->
        <view v-if="loading" class="loading-container">
          <text class="loading-text">加载中...</text>
        </view>

        <!-- 设置分组 -->
        <view v-else class="settings-container">
          <view 
            v-for="group in PREFERENCE_GROUPS" 
            :key="group.title" 
            class="setting-group"
          >
            <view class="group-header">
              <text class="group-icon">{{ group.icon }}</text>
              <text class="group-title">{{ group.title }}</text>
            </view>
            
            <view class="group-content">
              <view 
                v-for="item in group.items" 
                :key="item.key"
                class="setting-item"
                :class="{ 'disabled': isItemDisabled(item) }"
              >
                <!-- 开关类型 -->
                <view v-if="item.type === 'switch'" class="switch-item">
                  <view class="item-info">
                    <text class="item-label">{{ item.label }}</text>
                    <text class="item-description">{{ item.description }}</text>
                  </view>
                  <switch 
                    :checked="getSwitchValue(item.key)"
                    @change="handleSwitchChange(item.key, $event)"
                    :disabled="isItemDisabled(item)"
                    color="#007AFF"
                  />
                </view>

                <!-- 选择器类型 -->
                <view v-else-if="item.type === 'select'" class="select-item" @click="openPicker(item)">
                  <view class="item-info">
                    <text class="item-label">{{ item.label }}</text>
                    <text class="item-description">{{ item.description }}</text>
                  </view>
                  <view class="select-value">
                    <text class="value-text">{{ getSelectLabel(item) }}</text>
                    <text class="arrow-icon">›</text>
                  </view>
                </view>
              </view>
            </view>
          </view>
        </view>

        <!-- 底部间距 -->
        <view class="bottom-spacer"></view>
      </view>
    </scroll-view>

    <!-- 选择器弹窗 -->
    <picker-modal
      :show="pickerVisible"
      :title="currentPickerItem?.label"
      :options="currentPickerItem?.options || []"
      :value="getPreferenceValue(currentPickerItem?.key)"
      @confirm="handlePickerConfirm"
      @cancel="closePicker"
    />

    <!-- 确认重置对话框 -->
    <confirm-dialog
      :show="showResetDialog"
      title="重置设置"
      message="确定要重置所有偏好设置为默认值吗？此操作不可撤销。"
      @confirm="handleReset"
      @cancel="cancelReset"
    />
  </view>
</template>

<script>
import { ref, reactive, onMounted, computed, watch } from 'vue';
import { 
  getUserPreferences, 
  setUserPreference, 
  resetUserPreferences 
} from '../../services/api';
import { 
  PREFERENCE_GROUPS, 
  PREFERENCE_KEYS, 
  DEFAULT_PREFERENCES,
  stringToBoolean,
  booleanToString,
  getOptionLabel
} from '../../constants/preferences';
import { FeatureControl, VersionLabels, getCurrentVersionLabel, isProductionVersion } from '@/config/version';
import { reminderActions } from '@/store/modules/reminder';
import { uiActions } from '@/store/modules/ui';
import { logout } from '@/utils/auth';
import PickerModal from '@/components/PickerModal.vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';

export default {
  name: 'NotificationSettings',
  components: {
    PickerModal,
    ConfirmDialog
  },
  setup() {
    // 响应式数据
    const loading = ref(true);
    const preferences = reactive({});
    const pickerVisible = ref(false);
    const currentPickerItem = ref(null);
    const showResetDialog = ref(false);

    // 计算属性 - 根据版本控制过滤设置组
    const PREFERENCE_GROUPS_COMPUTED = computed(() => {
      return PREFERENCE_GROUPS.map(group => {
        const filteredItems = group.items.filter(item => {
          // 根据版本控制隐藏邮件和短信相关设置
          if (item.key === PREFERENCE_KEYS.EMAIL_NOTIFICATION_ENABLED) {
            return FeatureControl.showEmailFeatures();
          }
          if (item.key === PREFERENCE_KEYS.SMS_NOTIFICATION_ENABLED) {
            return FeatureControl.showPhoneFeatures();
          }
          if (item.key === PREFERENCE_KEYS.DAILY_SUMMARY_ENABLED) {
            // 每日汇总需要邮箱功能
            return FeatureControl.showEmailFeatures();
          }
          if (item.key === PREFERENCE_KEYS.DAILY_SUMMARY_TIME) {
            // 汇总时间依赖于邮箱功能
            return FeatureControl.showEmailFeatures();
          }
          return true; // 其他项目正常显示
        }).map(item => {
          // 对DEFAULT_REMINDER_TYPE项目过滤选项
          if (item.key === PREFERENCE_KEYS.DEFAULT_REMINDER_TYPE) {
            const filteredOptions = item.options.filter(option => {
              if (option.value === 'EMAIL') {
                return FeatureControl.showEmailFeatures();
              }
              if (option.value === 'SMS') {
                return FeatureControl.showPhoneFeatures();
              }
              return true; // 其他选项（如微信）正常显示
            });
            return {
              ...item,
              options: filteredOptions
            };
          }
          return item; // 其他项目保持原样
        });
        
        return {
          ...group,
          items: filteredItems
        };
      }).filter(group => group.items.length > 0); // 过滤掉没有项目的组
    });

    // 生命周期
    onMounted(() => {
      loadPreferences();
    });

    // 方法
    const loadPreferences = async () => {
      try {
        loading.value = true;
        const response = await getUserPreferences();
        
        // 将后端返回的偏好设置合并到本地
        if (response && response.preferences) {
          Object.assign(preferences, response.preferences);
        }
        
        // 确保所有必需的偏好设置都有默认值
        Object.keys(DEFAULT_PREFERENCES).forEach(key => {
          if (!(key in preferences)) {
            preferences[key] = DEFAULT_PREFERENCES[key];
          }
        });
        
        console.log('加载偏好设置成功:', preferences);
      } catch (error) {
        console.error('加载偏好设置失败:', error);
        
        // 加载失败时使用默认值
        Object.assign(preferences, DEFAULT_PREFERENCES);
        
        uni.showToast({
          title: '加载设置失败，使用默认值',
          icon: 'none',
          duration: 2000
        });
      } finally {
        loading.value = false;
      }
    };

    const getPreferenceValue = (key) => {
      return preferences[key] || DEFAULT_PREFERENCES[key] || '';
    };

    const getSwitchValue = (key) => {
      return stringToBoolean(getPreferenceValue(key));
    };

    const getSelectLabel = (item) => {
      const value = getPreferenceValue(item.key);
      return getOptionLabel(item.options, value);
    };

    const isItemDisabled = (item) => {
      if (!item.dependsOn) return false;
      return !getSwitchValue(item.dependsOn);
    };

    const handleSwitchChange = async (key, event) => {
      try {
        const value = booleanToString(event.detail.value);
        preferences[key] = value;
        
        await setUserPreference(key, value);
        console.log(`设置 ${key} = ${value} 成功`);
        
        uni.showToast({
          title: '设置已保存',
          icon: 'success',
          duration: 1000
        });
      } catch (error) {
        console.error(`设置 ${key} 失败:`, error);
        
        // 恢复原值
        preferences[key] = booleanToString(!event.detail.value);
        
        uni.showToast({
          title: '设置保存失败',
          icon: 'error',
          duration: 2000
        });
      }
    };

    const openPicker = (item) => {
      if (isItemDisabled(item)) return;
      
      currentPickerItem.value = item;
      pickerVisible.value = true;
    };

    const closePicker = () => {
      pickerVisible.value = false;
      currentPickerItem.value = null;
    };

    const handlePickerConfirm = async (value) => {
      try {
        const key = currentPickerItem.value.key;
        preferences[key] = value;
        
        await setUserPreference(key, value);
        console.log(`设置 ${key} = ${value} 成功`);
        
        uni.showToast({
          title: '设置已保存',
          icon: 'success',
          duration: 1000
        });
      } catch (error) {
        console.error(`设置失败:`, error);
        
        uni.showToast({
          title: '设置保存失败',
          icon: 'error',
          duration: 2000
        });
      } finally {
        closePicker();
      }
    };

    const confirmReset = () => {
      showResetDialog.value = true;
    };

    const cancelReset = () => {
      showResetDialog.value = false;
    };

    const handleReset = async () => {
      try {
        showResetDialog.value = false;
        loading.value = true;
        
        const response = await resetUserPreferences();
        
        // 更新本地偏好设置
        if (response && response.preferences) {
          Object.assign(preferences, response.preferences);
        } else {
          Object.assign(preferences, DEFAULT_PREFERENCES);
        }
        
        uni.showToast({
          title: '设置已重置',
          icon: 'success',
          duration: 2000
        });
        
        console.log('重置偏好设置成功');
      } catch (error) {
        console.error('重置偏好设置失败:', error);
        
        uni.showToast({
          title: '重置失败',
          icon: 'error',
          duration: 2000
        });
      } finally {
        loading.value = false;
      }
    };

    const goBack = () => {
      uni.navigateBack();
    };

    return {
      // 数据
      loading,
      preferences,
      pickerVisible,
      currentPickerItem,
      showResetDialog,
      
      // 常量
      PREFERENCE_GROUPS: PREFERENCE_GROUPS_COMPUTED,
      
      // 方法
      loadPreferences,
      getPreferenceValue,
      getSwitchValue,
      getSelectLabel,
      isItemDisabled,
      handleSwitchChange,
      openPicker,
      closePicker,
      handlePickerConfirm,
      confirmReset,
      cancelReset,
      handleReset,
      goBack
    };
  }
};
</script>

<style scoped>
.page-container {
  height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
}

/* 顶部导航栏 */
.header-section {
  background-color: #ffffff;
  border-bottom: 1px solid #e5e5e5;
  padding-top: var(--status-bar-height, 44px);
}

.nav-container {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}

.nav-back {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-icon {
  font-size: 24px;
  color: #007AFF;
  font-weight: bold;
}

.title-container {
  flex: 1;
  display: flex;
  justify-content: center;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #333333;
}

.nav-actions {
  width: 44px;
  display: flex;
  justify-content: center;
}

.reset-btn {
  padding: 6px 12px;
  border-radius: 6px;
  background-color: #f0f0f0;
}

.reset-text {
  font-size: 14px;
  color: #666666;
}

/* 内容区域 */
.content-scroll {
  flex: 1;
}

.content-container {
  padding: 16px;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.loading-text {
  font-size: 16px;
  color: #999999;
}

/* 设置分组 */
.setting-group {
  margin-bottom: 24px;
}

.group-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.group-icon {
  font-size: 20px;
  margin-right: 8px;
}

.group-title {
  font-size: 16px;
  font-weight: 600;
  color: #333333;
}

.group-content {
  background-color: #ffffff;
  border-radius: 12px;
  overflow: hidden;
}

/* 设置项 */
.setting-item {
  border-bottom: 1px solid #f0f0f0;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-item.disabled {
  opacity: 0.5;
}

.switch-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
}

.select-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
}

.item-info {
  flex: 1;
  margin-right: 16px;
}

.item-label {
  font-size: 16px;
  color: #333333;
  display: block;
  margin-bottom: 4px;
}

.item-description {
  font-size: 14px;
  color: #999999;
  line-height: 1.4;
}

.select-value {
  display: flex;
  align-items: center;
}

.value-text {
  font-size: 16px;
  color: #666666;
  margin-right: 8px;
}

.arrow-icon {
  font-size: 18px;
  color: #cccccc;
}

.bottom-spacer {
  height: 40px;
}
</style>