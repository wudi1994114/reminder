<template>
  <view class="container">
    <view class="form-card">
      <view class="form-header">
        <text class="form-title">{{ isEdit ? '编辑提醒' : '创建提醒' }}</text>
      </view>
      
      <view class="form-content">
        <view class="form-item">
          <text class="label">标题 <text class="required">*</text></text>
          <input class="input" v-model="reminderForm.title" placeholder="请输入提醒标题" />
        </view>
        
        <view class="form-item">
          <text class="label">内容</text>
          <textarea class="textarea" v-model="reminderForm.description" placeholder="请输入提醒内容" />
        </view>
        
        <view class="form-item">
          <text class="label">提醒时间 <text class="required">*</text></text>
          <view class="picker-container">
            <picker mode="date" :value="reminderDate" @change="onDateChange">
              <view class="picker date-picker">{{ reminderDate || '选择日期' }}</view>
            </picker>
            <picker mode="time" :value="reminderTime" @change="onTimeChange">
              <view class="picker time-picker">{{ reminderTime || '选择时间' }}</view>
            </picker>
          </view>
        </view>
        
        <view class="form-item">
          <text class="label">重复</text>
          <picker :range="repeatOptions" :value="repeatIndex" @change="onRepeatChange">
            <view class="picker">{{ repeatOptions[repeatIndex] }}</view>
          </picker>
        </view>
        
        <view class="form-item" v-if="showCronInput">
          <text class="label">Cron表达式</text>
          <input class="input" v-model="reminderForm.cronExpression" placeholder="请输入Cron表达式" />
          <text class="cron-tip">{{ cronPreview }}</text>
        </view>
      </view>
      
      <view class="form-actions">
        <button class="btn cancel" @click="cancel">取消</button>
        <button 
          class="btn submit" 
          @click="saveReminder" 
          :disabled="isSubmitting"
          :loading="isSubmitting"
        >
          {{ isSubmitting ? '保存中...' : '保存' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import { ref, computed, reactive, onMounted, watch } from 'vue';
import { createEvent, updateEvent, getSimpleReminderById } from '../../services/api';
import cronstrue from 'cronstrue/i18n';

export default {
  setup() {
    const isEdit = ref(false);
    const reminderForm = reactive({
      id: null,
      title: '',
      description: '', // 后端需要的是description
      eventTime: '', // 后端需要的是eventTime
      cronExpression: '',
      status: 'PENDING' // 默认为PENDING
    });
    
    const reminderDate = ref('');
    const reminderTime = ref('');
    const isSubmitting = ref(false);
    
    const repeatOptions = ['不重复', '每天', '每周', '每月', '自定义'];
    const repeatIndex = ref(0); 
    
    const showCronInput = computed(() => repeatIndex.value === 4); // 自定义时显示Cron输入框

    const cronPreview = computed(() => {
      if (reminderForm.cronExpression && repeatIndex.value === 4) {
        try {
          return cronstrue.toString(reminderForm.cronExpression, { locale: "zh_CN" });
        } catch (e) {
          return '无效的Cron表达式';
        }
      }
      return '';
    });
    
    onMounted(async () => {
      const pages = getCurrentPages();
      const page = pages[pages.length - 1];
      const id = page.$page?.options?.id || null;
      const mode = page.$page?.options?.mode || '';
      const initialDate = page.$page?.options?.date || ''; // 从日历页传来的日期
      
      isEdit.value = mode === 'edit' && id;
      
      if (isEdit.value) {
        try {
          isSubmitting.value = true; // 开始加载时也设为true，防止重复点击
          const result = await getSimpleReminderById(id);
          if (result) {
            reminderForm.id = result.id;
            reminderForm.title = result.title;
            reminderForm.description = result.description;
            reminderForm.eventTime = result.eventTime;
            reminderForm.cronExpression = result.cronExpression;
            reminderForm.status = result.status;
            
            if (result.eventTime) {
              const [date, time] = result.eventTime.split(' ');
              reminderDate.value = date;
              reminderTime.value = time.substring(0, 5); // HH:mm
            }
            
            if (result.cronExpression) {
              if (result.cronExpression === '0 0 8 * * ?') repeatIndex.value = 1;
              else if (result.cronExpression === '0 0 8 ? * MON') repeatIndex.value = 2;
              else if (result.cronExpression === '0 0 8 1 * ?') repeatIndex.value = 3;
              else repeatIndex.value = 4; // 自定义
            } else {
              repeatIndex.value = 0; // 不重复
            }
          }
        } catch (error) {
          console.error('获取提醒详情失败:', error);
          uni.showToast({title: '加载提醒数据失败', icon: 'none'});
        } finally {
          isSubmitting.value = false;
        }
      } else {
        // 创建模式，尝试填充从日历页传递的日期
        if(initialDate) {
          reminderDate.value = initialDate;
        }
        // 设置默认时间为当前时间的后一小时整点
        const now = new Date();
        now.setHours(now.getHours() + 1);
        now.setMinutes(0);
        now.setSeconds(0);
        if (!reminderTime.value) { // 仅当未设置时间时
          reminderTime.value = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
        }

        updateEventTime();
      }
    });
    
    const onDateChange = (e) => {
      reminderDate.value = e.detail.value;
      updateEventTime();
    };
    
    const onTimeChange = (e) => {
      reminderTime.value = e.detail.value;
      updateEventTime();
    };
    
    const updateEventTime = () => {
      if (reminderDate.value && reminderTime.value) {
        reminderForm.eventTime = `${reminderDate.value} ${reminderTime.value}:00`; // 补全秒
      } else {
        reminderForm.eventTime = '';
      }
    };
    
    watch(repeatIndex, (newIndex) => {
      switch (Number(newIndex)) {
        case 0: reminderForm.cronExpression = ''; break;
        case 1: reminderForm.cronExpression = '0 0 8 * * ?'; break; // 每天早上8点
        case 2: reminderForm.cronExpression = '0 0 8 ? * MON'; break; // 每周一早上8点
        case 3: reminderForm.cronExpression = '0 0 8 1 * ?'; break; // 每月1号早上8点
        // case 4 (自定义) 不做处理，用户自行输入
      }
    });
    
    const saveReminder = async () => {
      if (!reminderForm.title) {
        uni.showToast({ title: '请输入提醒标题', icon: 'none' });
        return;
      }
      if (!reminderForm.eventTime) {
        uni.showToast({ title: '请选择提醒时间', icon: 'none' });
        return;
      }
      if (repeatIndex.value === 4 && !reminderForm.cronExpression) {
        uni.showToast({ title: '自定义重复需要填写Cron表达式', icon: 'none' });
        return;
      }
      
      isSubmitting.value = true;
      try {
        let result;
        const dataToSave = { ...reminderForm };
        // 如果不是自定义重复，且cronExpression为空（例如不重复），则确保不传递cronExpression
        if (repeatIndex.value !== 4 && !dataToSave.cronExpression) {
            delete dataToSave.cronExpression;
        }

        if (isEdit.value) {
          result = await updateEvent(reminderForm.id, dataToSave);
        } else {
          result = await createEvent(dataToSave);
        }
        
        if (result) {
          uni.showToast({
            title: isEdit.value ? '修改成功' : '创建成功',
            icon: 'success',
            duration: 1500
          });
          setTimeout(() => {
            uni.navigateBack();
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
      uni.navigateBack();
    };
    
    return {
      isEdit,
      reminderForm,
      reminderDate,
      reminderTime,
      isSubmitting,
      repeatOptions,
      repeatIndex,
      showCronInput,
      cronPreview,
      onDateChange,
      onTimeChange,
      saveReminder,
      cancel
    };
  }
};
</script>

<style>
.container {
  padding: 30rpx;
}

.form-card {
  background-color: #ffffff;
  border-radius: 10rpx;
  padding: 30rpx;
  box-shadow: 0 2rpx 20rpx rgba(0, 0, 0, 0.1);
}

.form-header {
  margin-bottom: 30rpx;
  padding-bottom: 20rpx;
  border-bottom: 1px solid #f0f0f0;
}

.form-title {
  font-size: 36rpx;
  font-weight: bold;
}

.form-content {
  margin-bottom: 30rpx;
}

.form-item {
  margin-bottom: 30rpx;
}

.label {
  display: block;
  font-size: 28rpx;
  color: #666666;
  margin-bottom: 10rpx;
}

.required {
  color: #f56c6c; /* 红色星号 */
  margin-left: 4rpx;
}

.input, .textarea, .picker {
  width: 100%;
  padding: 20rpx;
  background-color: #f8f8f8;
  border-radius: 8rpx;
  font-size: 28rpx;
  color: #333;
}

.textarea {
  height: 200rpx;
}

.picker-container {
  display: flex;
  justify-content: space-between;
}

.picker.date-picker {
  width: calc(60% - 10rpx); /* 日期选择器宽一些 */
}

.picker.time-picker {
  width: calc(40% - 10rpx); /* 时间选择器窄一些 */
}

.cron-tip {
  font-size: 24rpx;
  color: #999999;
  margin-top: 10rpx;
}

.form-actions {
  display: flex;
  justify-content: space-between;
}

.btn {
  width: 45%;
  padding: 20rpx 0;
  font-size: 30rpx; /* 统一按钮字体大小 */
  text-align: center;
  border-radius: 8rpx;
}

.submit {
  background-color: #3cc51f;
  color: #ffffff;
}

.submit:disabled {
  background-color: #a0cfff;
}

.cancel {
  background-color: #f0f0f0;
  color: #666666;
}
</style> 