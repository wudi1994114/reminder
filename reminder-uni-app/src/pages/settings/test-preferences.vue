<template>
  <view class="test-container">
    <text class="title">偏好设置API测试</text>
    
    <button @click="testGetPreferences" class="test-btn">获取所有偏好设置</button>
    <button @click="testSetPreference" class="test-btn">设置单个偏好</button>
    <button @click="testBatchUpdate" class="test-btn">批量更新偏好</button>
    <button @click="testReset" class="test-btn">重置偏好设置</button>
    
    <view class="result-container">
      <text class="result-title">测试结果：</text>
      <text class="result-text">{{ testResult }}</text>
    </view>
  </view>
</template>

<script>
import { ref } from 'vue';
import { 
  getUserPreferences, 
  setUserPreference, 
  batchUpdateUserPreferences, 
  resetUserPreferences 
} from '../../services/api';
import { PREFERENCE_KEYS } from '../../constants/preferences';

export default {
  setup() {
    const testResult = ref('等待测试...');

    const testGetPreferences = async () => {
      try {
        testResult.value = '正在获取偏好设置...';
        const result = await getUserPreferences();
        testResult.value = JSON.stringify(result, null, 2);
        console.log('获取偏好设置成功:', result);
      } catch (error) {
        testResult.value = `获取失败: ${error.message}`;
        console.error('获取偏好设置失败:', error);
      }
    };

    const testSetPreference = async () => {
      try {
        testResult.value = '正在设置偏好...';
        const result = await setUserPreference(PREFERENCE_KEYS.THEME, 'dark', '测试设置主题');
        testResult.value = JSON.stringify(result, null, 2);
        console.log('设置偏好成功:', result);
      } catch (error) {
        testResult.value = `设置失败: ${error.message}`;
        console.error('设置偏好失败:', error);
      }
    };

    const testBatchUpdate = async () => {
      try {
        testResult.value = '正在批量更新...';
        const preferences = {
          [PREFERENCE_KEYS.THEME]: 'light',
          [PREFERENCE_KEYS.SOUND_ENABLED]: 'true',
          [PREFERENCE_KEYS.VIBRATION_ENABLED]: 'false'
        };
        const result = await batchUpdateUserPreferences(preferences, false);
        testResult.value = JSON.stringify(result, null, 2);
        console.log('批量更新成功:', result);
      } catch (error) {
        testResult.value = `批量更新失败: ${error.message}`;
        console.error('批量更新失败:', error);
      }
    };

    const testReset = async () => {
      try {
        testResult.value = '正在重置偏好设置...';
        const result = await resetUserPreferences();
        testResult.value = JSON.stringify(result, null, 2);
        console.log('重置成功:', result);
      } catch (error) {
        testResult.value = `重置失败: ${error.message}`;
        console.error('重置失败:', error);
      }
    };

    return {
      testResult,
      testGetPreferences,
      testSetPreference,
      testBatchUpdate,
      testReset
    };
  }
};
</script>

<style scoped>
.test-container {
  padding: 20px;
}

.title {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 20px;
  text-align: center;
}

.test-btn {
  width: 100%;
  margin-bottom: 10px;
  padding: 12px;
  background-color: #007AFF;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
}

.result-container {
  margin-top: 20px;
  padding: 15px;
  background-color: #f5f5f5;
  border-radius: 8px;
}

.result-title {
  font-weight: bold;
  margin-bottom: 10px;
  display: block;
}

.result-text {
  font-family: monospace;
  font-size: 12px;
  line-height: 1.4;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>