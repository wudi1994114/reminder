<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="header-section">
      <view class="nav-container">
        <view class="nav-back" @click="goBack">
          <text class="back-icon">‹</text>
        </view>
        <view class="title-container">
          <text class="page-title">配速计算器</text>
        </view>
        <view class="nav-spacer"></view>
      </view>
    </view>
    
    <!-- 主要内容区域 -->
    <scroll-view class="content-scroll" scroll-y>
      <view class="content-container">
        
        <!-- 距离类型选择 -->
        <view class="section-card">
          <view class="section-title">
            <text class="title-text">选择距离类型</text>
          </view>
          <view class="distance-selector">
            <view 
              class="distance-option" 
              :class="{ 'active': distanceType === 'full' }"
              @click="selectDistance('full')"
            >
              <text class="distance-emoji">🏃‍♂️</text>
              <text class="distance-name">全程马拉松</text>
              <text class="distance-value">42.195 km</text>
            </view>
            <view 
              class="distance-option" 
              :class="{ 'active': distanceType === 'half' }"
              @click="selectDistance('half')"
            >
              <text class="distance-emoji">🏃</text>
              <text class="distance-name">半程马拉松</text>
              <text class="distance-value">21.0975 km</text>
            </view>
          </view>
        </view>
        
        <!-- 目标完赛时间输入 -->
        <view class="section-card">
          <view class="section-title">
            <text class="title-text">目标完赛时间</text>
          </view>
          <view class="time-input-container">
            <view class="time-input-group">
              <input 
                class="time-input" 
                type="digit"
                v-model="targetHours"
                :placeholder="placeholders.targetHours"
                @input="onTimeInput"
                @focus="onFocus('targetHours')"
                @blur="onBlur('targetHours')"
                maxlength="2"
              />
              <text class="time-unit">小时</text>
            </view>
            <text class="time-separator">:</text>
            <view class="time-input-group">
              <input 
                class="time-input" 
                type="digit"
                v-model="targetMinutes"
                :placeholder="placeholders.targetMinutes"
                @input="onTimeInput"
                @focus="onFocus('targetMinutes')"
                @blur="onBlur('targetMinutes')"
                maxlength="2"
              />
              <text class="time-unit">分钟</text>
            </view>
          </view>
          <view class="time-tips">
            <text class="tip-text">💡 例如：3小时30分钟，输入 3:30</text>
          </view>
        </view>
        
        <!-- 计算按钮 -->
        <view class="action-section">
          <button class="calculate-btn" @click="calculatePace" :disabled="!canCalculate">
            <text class="btn-text">计算配速</text>
          </button>
        </view>
        
        <!-- 结果展示区域 -->
        <view class="result-section" v-if="showResult">
          <!-- 平均配速 -->
          <view class="result-card highlight-card">
            <view class="result-header">
              <text class="result-icon">⚡</text>
              <text class="result-title">平均配速</text>
            </view>
            <view class="result-value-large">
              <text class="value-number">{{ averagePace }}</text>
              <text class="value-unit">分钟/公里</text>
            </view>
          </view>
          
          <!-- 每5公里用时 -->
          <view class="section-card">
            <view class="section-title">
              <text class="title-text">分段用时（每5公里）</text>
            </view>
            <view class="split-table">
              <view class="split-header">
                <text class="header-cell distance-col">距离</text>
                <text class="header-cell time-col">用时</text>
                <text class="header-cell cumulative-col">累计时间</text>
              </view>
              <view 
                class="split-row" 
                v-for="(split, index) in splits" 
                :key="index"
                :class="{ 'last-row': index === splits.length - 1 }"
              >
                <text class="data-cell distance-col">{{ split.distance }} km</text>
                <text class="data-cell time-col">{{ split.time }}</text>
                <text class="data-cell cumulative-col">{{ split.cumulative }}</text>
              </view>
            </view>
          </view>
          
          <!-- 目标提示 -->
          <view class="tips-card">
            <text class="tips-icon">🎯</text>
            <view class="tips-content">
              <text class="tips-title">训练建议</text>
              <text class="tips-text">保持稳定配速，前半程略慢于平均配速，后半程逐渐加速</text>
            </view>
          </view>
        </view>
        
        <!-- 底部间距 -->
        <view class="bottom-spacer"></view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
export default {
  name: 'PaceCalculator',
  data() {
    return {
      distanceType: 'full', // 'full' 或 'half'
      targetHours: '',
      targetMinutes: '',
      showResult: false,
      averagePace: '',
      splits: [],
      // 动态 placeholder
      placeholders: {
        targetHours: '0',
        targetMinutes: '00'
      }
    };
  },
  computed: {
    canCalculate() {
      const hours = parseInt(this.targetHours) || 0;
      const minutes = parseInt(this.targetMinutes) || 0;
      return hours > 0 || minutes > 0;
    },
    totalDistance() {
      return this.distanceType === 'full' ? 42.195 : 21.0975;
    }
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    
    selectDistance(type) {
      this.distanceType = type;
      // 清空结果
      this.showResult = false;
    },
    
    onFocus(field) {
      // 获得焦点时清空 placeholder
      this.placeholders[field] = '';
    },
    
    onBlur(field) {
      // 失去焦点时，如果输入框为空，恢复 placeholder
      if (!this[field]) {
        const defaultPlaceholders = {
          targetHours: '0',
          targetMinutes: '00'
        };
        this.placeholders[field] = defaultPlaceholders[field];
      }
    },
    
    onTimeInput() {
      // 限制输入范围
      if (this.targetHours && parseInt(this.targetHours) > 99) {
        this.targetHours = '99';
      }
      if (this.targetMinutes && parseInt(this.targetMinutes) > 59) {
        this.targetMinutes = '59';
      }
    },
    
    calculatePace() {
      const hours = parseInt(this.targetHours) || 0;
      const minutes = parseInt(this.targetMinutes) || 0;
      
      if (hours === 0 && minutes === 0) {
        uni.showToast({
          title: '请输入目标时间',
          icon: 'none'
        });
        return;
      }
      
      // 计算总分钟数
      const totalMinutes = hours * 60 + minutes;
      
      // 计算每公里配速（分钟）
      const pacePerKm = totalMinutes / this.totalDistance;
      const paceMinutes = Math.floor(pacePerKm);
      const paceSeconds = Math.round((pacePerKm - paceMinutes) * 60);
      
      this.averagePace = `${paceMinutes}'${String(paceSeconds).padStart(2, '0')}"`;
      
      // 计算每5公里分段
      this.splits = [];
      let cumulativeMinutes = 0;
      
      for (let i = 5; i <= this.totalDistance; i += 5) {
        const segmentTime = pacePerKm * 5;
        cumulativeMinutes += segmentTime;
        
        const segmentHours = Math.floor(segmentTime / 60);
        const segmentMins = Math.floor(segmentTime % 60);
        const segmentSecs = Math.round((segmentTime % 1) * 60);
        
        const cumulativeHours = Math.floor(cumulativeMinutes / 60);
        const cumulativeMins = Math.floor(cumulativeMinutes % 60);
        const cumulativeSecs = Math.round((cumulativeMinutes % 1) * 60);
        
        this.splits.push({
          distance: i.toFixed(1),
          time: segmentHours > 0 
            ? `${segmentHours}:${String(segmentMins).padStart(2, '0')}:${String(segmentSecs).padStart(2, '0')}`
            : `${segmentMins}:${String(segmentSecs).padStart(2, '0')}`,
          cumulative: cumulativeHours > 0
            ? `${cumulativeHours}:${String(cumulativeMins).padStart(2, '0')}:${String(cumulativeSecs).padStart(2, '0')}`
            : `${cumulativeMins}:${String(cumulativeSecs).padStart(2, '0')}`
        });
      }
      
      // 处理最后不足5公里的部分
      const lastDistance = this.totalDistance % 5;
      if (lastDistance > 0) {
        const segmentTime = pacePerKm * lastDistance;
        cumulativeMinutes += segmentTime;
        
        const segmentHours = Math.floor(segmentTime / 60);
        const segmentMins = Math.floor(segmentTime % 60);
        const segmentSecs = Math.round((segmentTime % 1) * 60);
        
        const cumulativeHours = Math.floor(cumulativeMinutes / 60);
        const cumulativeMins = Math.floor(cumulativeMinutes % 60);
        const cumulativeSecs = Math.round((cumulativeMinutes % 1) * 60);
        
        this.splits.push({
          distance: this.totalDistance.toFixed(3),
          time: segmentHours > 0 
            ? `${segmentHours}:${String(segmentMins).padStart(2, '0')}:${String(segmentSecs).padStart(2, '0')}`
            : `${segmentMins}:${String(segmentSecs).padStart(2, '0')}`,
          cumulative: cumulativeHours > 0
            ? `${cumulativeHours}:${String(cumulativeMins).padStart(2, '0')}:${String(cumulativeSecs).padStart(2, '0')}`
            : `${cumulativeMins}:${String(cumulativeSecs).padStart(2, '0')}`
        });
      }
      
      this.showResult = true;
      
      // 滚动到结果区域
      setTimeout(() => {
        uni.pageScrollTo({
          scrollTop: 600,
          duration: 300
        });
      }, 100);
    }
  }
};
</script>

<style scoped>
.page-container {
  min-height: 100vh;
  background-color: #fcfbf8;
  display: flex;
  flex-direction: column;
  font-family: 'Manrope', 'Noto Sans', sans-serif;
}

/* 顶部导航栏 */
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
}

.content-container {
  padding: 16rpx 32rpx 32rpx;
}

/* 卡片样式 */
.section-card {
  background-color: #ffffff;
  border: 2rpx solid #f4efe7;
  border-radius: 24rpx;
  box-shadow: 0 6rpx 24rpx rgba(28, 23, 13, 0.08);
  padding: 32rpx;
  margin-bottom: 24rpx;
}

.section-title {
  margin-bottom: 24rpx;
}

.title-text {
  font-size: 28rpx;
  font-weight: 700;
  color: #1c170d;
}

/* 距离选择器 */
.distance-selector {
  display: flex;
  gap: 16rpx;
}

.distance-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  padding: 24rpx;
  background-color: #f4efe7;
  border: 3rpx solid #f4efe7;
  border-radius: 16rpx;
  transition: all 0.2s ease;
  cursor: pointer;
}

.distance-option.active {
  background-color: #fff8e8;
  border-color: #f7bd4a;
  box-shadow: 0 4rpx 16rpx rgba(247, 189, 74, 0.2);
}

.distance-emoji {
  font-size: 48rpx;
}

.distance-name {
  font-size: 26rpx;
  font-weight: 600;
  color: #1c170d;
}

.distance-value {
  font-size: 22rpx;
  color: #9d8148;
}

/* 时间输入 */
.time-input-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  padding: 24rpx 0;
}

.time-input-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}

.time-input {
  width: 160rpx;
  height: 120rpx;
  background-color: #f4efe7;
  border: 2rpx solid #f4efe7;
  border-radius: 16rpx;
  text-align: center;
  font-size: 48rpx;
  font-weight: 700;
  color: #1c170d;
}

.time-separator {
  font-size: 48rpx;
  font-weight: 700;
  color: #1c170d;
  padding-bottom: 32rpx;
}

.time-unit {
  font-size: 22rpx;
  color: #9d8148;
}

.time-tips {
  padding-top: 16rpx;
  border-top: 1rpx solid #f4efe7;
}

.tip-text {
  font-size: 24rpx;
  color: #9d8148;
  line-height: 1.5;
}

/* 计算按钮 */
.action-section {
  margin: 24rpx 0;
}

.calculate-btn {
  width: 100%;
  height: 96rpx;
  background-color: #f7bd4a;
  border-radius: 24rpx;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 24rpx rgba(247, 189, 74, 0.3);
  transition: all 0.2s ease;
}

.calculate-btn:active {
  background-color: #e6a63a;
  transform: translateY(2rpx);
}

.calculate-btn[disabled] {
  background-color: #e0ddd5;
  box-shadow: none;
}

.btn-text {
  font-size: 32rpx;
  font-weight: 700;
  color: #1c170d;
}

/* 结果区域 */
.result-section {
  margin-top: 32rpx;
}

.result-card {
  background-color: #ffffff;
  border: 2rpx solid #f4efe7;
  border-radius: 24rpx;
  box-shadow: 0 6rpx 24rpx rgba(28, 23, 13, 0.08);
  padding: 32rpx;
  margin-bottom: 24rpx;
}

.highlight-card {
  background: linear-gradient(135deg, #fff8e8 0%, #ffffff 100%);
  border-color: #f7bd4a;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 24rpx;
}

.result-icon {
  font-size: 32rpx;
}

.result-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1c170d;
}

.result-value-large {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 12rpx;
}

.value-number {
  font-size: 72rpx;
  font-weight: 700;
  color: #f7bd4a;
  line-height: 1;
}

.value-unit {
  font-size: 28rpx;
  color: #9d8148;
}

/* 分段表格 */
.split-table {
  border: 1rpx solid #f4efe7;
  border-radius: 12rpx;
  overflow: hidden;
}

.split-header {
  display: flex;
  background-color: #f4efe7;
  padding: 16rpx;
}

.header-cell {
  font-size: 24rpx;
  font-weight: 600;
  color: #1c170d;
  text-align: center;
}

.split-row {
  display: flex;
  padding: 16rpx;
  border-top: 1rpx solid #f4efe7;
}

.split-row.last-row {
  background-color: #fff8e8;
}

.data-cell {
  font-size: 26rpx;
  color: #1c170d;
  text-align: center;
}

.distance-col {
  flex: 1;
}

.time-col {
  flex: 1.2;
}

.cumulative-col {
  flex: 1.2;
}

/* 提示卡片 */
.tips-card {
  display: flex;
  gap: 16rpx;
  padding: 24rpx;
  background-color: #e8f5e9;
  border-radius: 16rpx;
}

.tips-icon {
  font-size: 32rpx;
  flex-shrink: 0;
}

.tips-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.tips-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #2e7d32;
}

.tips-text {
  font-size: 24rpx;
  color: #558b2f;
  line-height: 1.5;
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

  .content-container {
    padding: 12rpx 24rpx 24rpx;
  }

  .section-card {
    padding: 24rpx;
  }

  .time-input {
    width: 140rpx;
    height: 100rpx;
    font-size: 40rpx;
  }

  .value-number {
    font-size: 60rpx;
  }
}
</style>

