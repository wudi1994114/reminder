<template>
  <view class="test-page">
    <view class="header">
      <text class="title">年模式互斥测试</text>
    </view>
    
    <view class="content">
      <view class="test-section">
        <button class="test-btn" @click="showCronPicker">打开Cron选择器</button>
      </view>
      
      <view class="result-section">
        <view class="result-item">
          <text class="label">当前Cron表达式：</text>
          <text class="value">{{ cronExpression || '未设置' }}</text>
        </view>
        
        <view class="result-item">
          <text class="label">描述：</text>
          <text class="value">{{ cronDescription || '未设置' }}</text>
        </view>
      </view>
    </view>
    
    <!-- Cron表达式选择器 -->
    <CronExpressionPicker 
      :show="showPicker"
      :initialValue="cronExpression"
      @confirm="onCronConfirm"
      @cancel="onCronCancel"
      @update:show="showPicker = $event"
    />
  </view>
</template>

<script>
import CronExpressionPicker from '../../components/CronExpressionPicker.vue';

export default {
  components: {
    CronExpressionPicker
  },
  data() {
    return {
      showPicker: false,
      cronExpression: '0 9 1 1 1 *', // 默认年模式：每年1月1日周一上午9点
      cronDescription: ''
    }
  },
  
  mounted() {
    this.updateDescription();
  },
  
  methods: {
    showCronPicker() {
      this.showPicker = true;
    },
    
    onCronConfirm(cron) {
      this.cronExpression = cron;
      this.showPicker = false;
      this.updateDescription();
      
      uni.showToast({
        title: 'Cron表达式已更新',
        icon: 'success'
      });
    },
    
    onCronCancel() {
      this.showPicker = false;
    },
    
    updateDescription() {
      // 简单的描述生成
      if (this.cronExpression) {
        try {
          const parts = this.cronExpression.split(' ');
          if (parts.length >= 6) {
            const [second, minute, hour, day, month, weekday] = parts;
            
            if (month !== '*' && month !== '?') {
              if (weekday !== '?' && weekday !== '*') {
                this.cronDescription = `年重复（星期模式）：每年${month}月的星期${weekday}，${hour}:${minute}`;
              } else if (day !== '?' && day !== '*') {
                this.cronDescription = `年重复（日期模式）：每年${month}月${day}日，${hour}:${minute}`;
              } else {
                this.cronDescription = `年重复：每年${month}月，${hour}:${minute}`;
              }
            } else {
              this.cronDescription = '其他模式';
            }
          }
        } catch (e) {
          this.cronDescription = '解析失败';
        }
      }
    }
  }
}
</script>

<style scoped>
.test-page {
  padding: 32rpx;
  background-color: #fcfbf8;
  min-height: 100vh;
}

.header {
  text-align: center;
  margin-bottom: 48rpx;
}

.title {
  font-size: 36rpx;
  font-weight: 700;
  color: #1c170d;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 48rpx;
}

.test-section {
  text-align: center;
}

.test-btn {
  background-color: #f7bd4a;
  color: #1c170d;
  border: none;
  border-radius: 16rpx;
  padding: 24rpx 48rpx;
  font-size: 32rpx;
  font-weight: 600;
}

.result-section {
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.08);
}

.result-item {
  margin-bottom: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.result-item:last-child {
  margin-bottom: 0;
}

.label {
  font-size: 28rpx;
  color: #9d8148;
  font-weight: 500;
}

.value {
  font-size: 32rpx;
  color: #1c170d;
  font-weight: 400;
  word-break: break-all;
}
</style> 