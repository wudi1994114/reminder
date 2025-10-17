<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="header-section">
      <view class="nav-container">
        <view class="nav-back" @click="goBack">
          <text class="back-icon">‹</text>
        </view>
        <view class="title-container">
          <text class="page-title">实时配速</text>
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
        
        <!-- 当前跑步状态 -->
        <view class="section-card">
          <view class="section-title">
            <text class="title-text">当前跑步状态</text>
          </view>
          
          <!-- 当前公里数 -->
          <view class="input-row">
            <view class="input-label">
              <text class="label-icon">📍</text>
              <text class="label-text">当前公里数</text>
            </view>
            <view class="input-field-group">
              <input 
                class="input-field" 
                type="digit"
                v-model="currentDistance"
                :placeholder="placeholders.currentDistance"
                @input="onCurrentDistanceInput"
                @focus="onFocus('currentDistance')"
                @blur="onBlur('currentDistance')"
              />
              <text class="input-unit">km</text>
            </view>
          </view>
          
          <!-- 已用时间 -->
          <view class="input-row">
            <view class="input-label">
              <text class="label-icon">⏱️</text>
              <text class="label-text">已用时间</text>
            </view>
            <view class="time-inputs">
              <view class="time-field-group">
                <input 
                  class="time-field" 
                  type="digit"
                  v-model="currentHours"
                  :placeholder="placeholders.currentHours"
                  @focus="onFocus('currentHours')"
                  @blur="onBlur('currentHours')"
                  maxlength="2"
                />
                <text class="field-unit">时</text>
              </view>
              <text class="time-colon">:</text>
              <view class="time-field-group">
                <input 
                  class="time-field" 
                  type="digit"
                  v-model="currentMinutes"
                  :placeholder="placeholders.currentMinutes"
                  @focus="onFocus('currentMinutes')"
                  @blur="onBlur('currentMinutes')"
                  maxlength="2"
                />
                <text class="field-unit">分</text>
              </view>
            </view>
          </view>
        </view>
        
        <!-- 目标完赛时间 -->
        <view class="section-card">
          <view class="section-title">
            <text class="title-text">目标完赛时间</text>
          </view>
          <view class="input-row">
            <view class="input-label">
              <text class="label-icon">🎯</text>
              <text class="label-text">目标时间</text>
            </view>
            <view class="time-inputs">
              <view class="time-field-group">
                <input 
                  class="time-field" 
                  type="digit"
                  v-model="targetHours"
                  :placeholder="placeholders.targetHours"
                  @focus="onFocus('targetHours')"
                  @blur="onBlur('targetHours')"
                  maxlength="2"
                />
                <text class="field-unit">时</text>
              </view>
              <text class="time-colon">:</text>
              <view class="time-field-group">
                <input 
                  class="time-field" 
                  type="digit"
                  v-model="targetMinutes"
                  :placeholder="placeholders.targetMinutes"
                  @focus="onFocus('targetMinutes')"
                  @blur="onBlur('targetMinutes')"
                  maxlength="2"
                />
                <text class="field-unit">分</text>
              </view>
            </view>
          </view>
        </view>
        
        <!-- 计算按钮 -->
        <view class="action-section">
          <button class="calculate-btn" @click="calculateRealTimePace" :disabled="!canCalculate">
            <text class="btn-text">计算实时配速</text>
          </button>
        </view>
        
        <!-- 结果展示区域 -->
        <view class="result-section" v-if="showResult">
          
          <!-- 当前配速状态 -->
          <view class="result-card status-card">
            <view class="status-header">
              <text class="status-title">当前配速状态</text>
            </view>
            <view class="status-grid">
              <view class="status-item">
                <text class="status-label">已跑距离</text>
                <text class="status-value">{{ currentDistance }} km</text>
              </view>
              <view class="status-item">
                <text class="status-label">当前配速</text>
                <text class="status-value">{{ currentPace }}</text>
              </view>
              <view class="status-item">
                <text class="status-label">剩余距离</text>
                <text class="status-value">{{ remainingDistance }} km</text>
              </view>
              <view class="status-item">
                <text class="status-label">剩余时间</text>
                <text class="status-value">{{ remainingTime }}</text>
              </view>
            </view>
          </view>
          
          <!-- 需要配速 - 高亮显示 -->
          <view class="result-card highlight-card">
            <view class="result-header">
              <text class="result-icon">🎯</text>
              <text class="result-title">接下来需要配速</text>
            </view>
            <view class="result-value-large">
              <text class="value-number">{{ requiredPace }}</text>
              <text class="value-unit">分钟/公里</text>
            </view>
            <view class="pace-change" :class="paceChangeClass">
              <text class="change-icon">{{ paceChangeIcon }}</text>
              <text class="change-text">{{ paceChangeText }}</text>
            </view>
          </view>
          
          <!-- 配速对比 -->
          <view class="section-card">
            <view class="section-title">
              <text class="title-text">配速对比分析</text>
            </view>
            <view class="comparison-table">
              <view class="comparison-row">
                <text class="comparison-label">当前配速</text>
                <text class="comparison-value">{{ currentPace }}</text>
              </view>
              <view class="comparison-row">
                <text class="comparison-label">需要配速</text>
                <text class="comparison-value highlight">{{ requiredPace }}</text>
              </view>
              <view class="comparison-row">
                <text class="comparison-label">配速差异</text>
                <text class="comparison-value" :class="paceChangeClass">{{ paceDifference }}</text>
              </view>
            </view>
          </view>
          
          <!-- 预测结果 -->
          <view class="section-card">
            <view class="section-title">
              <text class="title-text">预测完赛时间</text>
            </view>
            <view class="prediction-item">
              <text class="prediction-label">若保持当前配速</text>
              <text class="prediction-value">{{ predictedFinishTime }}</text>
            </view>
            <view class="prediction-item">
              <text class="prediction-label">目标完赛时间</text>
              <text class="prediction-value target">{{ targetFinishTime }}</text>
            </view>
            <view class="prediction-item">
              <text class="prediction-label">时间差异</text>
              <text class="prediction-value" :class="timeDifferenceClass">{{ timeDifference }}</text>
            </view>
          </view>
          
          <!-- 建议卡片 -->
          <view class="tips-card" :class="adviceCardClass">
            <text class="tips-icon">{{ adviceIcon }}</text>
            <view class="tips-content">
              <text class="tips-title">{{ adviceTitle }}</text>
              <text class="tips-text">{{ adviceText }}</text>
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
  name: 'RealtimePace',
  data() {
    return {
      distanceType: 'full',
      currentDistance: '',
      currentHours: '',
      currentMinutes: '',
      targetHours: '',
      targetMinutes: '',
      showResult: false,
      
      // 计算结果
      remainingDistance: '',
      remainingTime: '',
      currentPace: '',
      requiredPace: '',
      predictedFinishTime: '',
      targetFinishTime: '',
      timeDifference: '',
      paceDifference: '',
      
      // 状态指示
      paceChangeClass: '',
      paceChangeIcon: '',
      paceChangeText: '',
      timeDifferenceClass: '',
      adviceCardClass: '',
      adviceIcon: '',
      adviceTitle: '',
      adviceText: '',
      
      // 动态 placeholder
      placeholders: {
        currentDistance: '0',
        currentHours: '0',
        currentMinutes: '00',
        targetHours: '0',
        targetMinutes: '00'
      }
    };
  },
  computed: {
    canCalculate() {
      const distance = parseFloat(this.currentDistance) || 0;
      const currentH = parseInt(this.currentHours) || 0;
      const currentM = parseInt(this.currentMinutes) || 0;
      const targetH = parseInt(this.targetHours) || 0;
      const targetM = parseInt(this.targetMinutes) || 0;
      
      return distance > 0 && 
             distance < this.totalDistance &&
             (currentH > 0 || currentM > 0) &&
             (targetH > 0 || targetM > 0);
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
          currentDistance: '0',
          currentHours: '0',
          currentMinutes: '00',
          targetHours: '0',
          targetMinutes: '00'
        };
        this.placeholders[field] = defaultPlaceholders[field];
      }
    },
    
    onCurrentDistanceInput() {
      const distance = parseFloat(this.currentDistance);
      if (distance && distance > this.totalDistance) {
        this.currentDistance = this.totalDistance.toString();
        uni.showToast({
          title: `距离不能超过${this.totalDistance}km`,
          icon: 'none'
        });
      }
    },
    
    formatTime(totalMinutes) {
      const hours = Math.floor(totalMinutes / 60);
      const minutes = Math.floor(totalMinutes % 60);
      const seconds = Math.round((totalMinutes % 1) * 60);
      
      if (hours > 0) {
        return `${hours}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
      }
      return `${minutes}:${String(seconds).padStart(2, '0')}`;
    },
    
    formatPace(paceMinutes) {
      const minutes = Math.floor(paceMinutes);
      const seconds = Math.round((paceMinutes - minutes) * 60);
      return `${minutes}'${String(seconds).padStart(2, '0')}"`;
    },
    
    calculateRealTimePace() {
      // 验证输入
      const distance = parseFloat(this.currentDistance);
      if (!distance || distance <= 0 || distance >= this.totalDistance) {
        uni.showToast({
          title: '请输入有效的当前公里数',
          icon: 'none'
        });
        return;
      }
      
      const currentH = parseInt(this.currentHours) || 0;
      const currentM = parseInt(this.currentMinutes) || 0;
      const currentTotalMinutes = currentH * 60 + currentM;
      
      if (currentTotalMinutes <= 0) {
        uni.showToast({
          title: '请输入已用时间',
          icon: 'none'
        });
        return;
      }
      
      const targetH = parseInt(this.targetHours) || 0;
      const targetM = parseInt(this.targetMinutes) || 0;
      const targetTotalMinutes = targetH * 60 + targetM;
      
      if (targetTotalMinutes <= 0) {
        uni.showToast({
          title: '请输入目标时间',
          icon: 'none'
        });
        return;
      }
      
      if (currentTotalMinutes >= targetTotalMinutes) {
        uni.showToast({
          title: '已用时间不能大于或等于目标时间',
          icon: 'none'
        });
        return;
      }
      
      // 计算剩余距离和时间
      const remaining = this.totalDistance - distance;
      const remainingMinutes = targetTotalMinutes - currentTotalMinutes;
      
      this.remainingDistance = remaining.toFixed(2);
      this.remainingTime = this.formatTime(remainingMinutes);
      
      // 计算当前配速
      const currentPaceValue = currentTotalMinutes / distance;
      this.currentPace = this.formatPace(currentPaceValue);
      
      // 计算需要配速
      const requiredPaceValue = remainingMinutes / remaining;
      this.requiredPace = this.formatPace(requiredPaceValue);
      
      // 计算配速差异
      const paceDiff = requiredPaceValue - currentPaceValue;
      const paceDiffSeconds = Math.abs(Math.round(paceDiff * 60));
      
      if (Math.abs(paceDiff) < 0.05) {
        // 配速基本一致
        this.paceChangeClass = 'neutral';
        this.paceChangeIcon = '✓';
        this.paceChangeText = '保持当前配速即可';
        this.paceDifference = '几乎相同';
      } else if (paceDiff > 0) {
        // 需要减速
        this.paceChangeClass = 'slower';
        this.paceChangeIcon = '↓';
        this.paceChangeText = `可以略微减速 ${paceDiffSeconds} 秒/公里`;
        this.paceDifference = `慢 ${paceDiffSeconds} 秒/公里`;
      } else {
        // 需要加速
        this.paceChangeClass = 'faster';
        this.paceChangeIcon = '↑';
        this.paceChangeText = `需要加速 ${paceDiffSeconds} 秒/公里`;
        this.paceDifference = `快 ${paceDiffSeconds} 秒/公里`;
      }
      
      // 预测完赛时间（保持当前配速）
      const predictedTotalMinutes = currentPaceValue * this.totalDistance;
      this.predictedFinishTime = this.formatTime(predictedTotalMinutes);
      this.targetFinishTime = this.formatTime(targetTotalMinutes);
      
      // 计算时间差异
      const timeDiff = predictedTotalMinutes - targetTotalMinutes;
      const timeDiffMinutes = Math.abs(Math.round(timeDiff));
      
      if (Math.abs(timeDiff) < 1) {
        this.timeDifference = '几乎相同';
        this.timeDifferenceClass = 'neutral';
      } else if (timeDiff > 0) {
        this.timeDifference = `慢 ${timeDiffMinutes} 分钟`;
        this.timeDifferenceClass = 'slower';
      } else {
        this.timeDifference = `快 ${timeDiffMinutes} 分钟`;
        this.timeDifferenceClass = 'faster';
      }
      
      // 生成建议
      this.generateAdvice(paceDiff, timeDiff, remaining);
      
      this.showResult = true;
      
      // 滚动到结果区域
      setTimeout(() => {
        uni.pageScrollTo({
          scrollTop: 700,
          duration: 300
        });
      }, 100);
    },
    
    generateAdvice(paceDiff, timeDiff, remaining) {
      if (Math.abs(timeDiff) < 2) {
        // 配速完美
        this.adviceCardClass = 'success';
        this.adviceIcon = '🎉';
        this.adviceTitle = '配速完美';
        this.adviceText = '当前配速非常理想，继续保持这个节奏，稳定完赛！';
      } else if (timeDiff > 0 && timeDiff < 10) {
        // 略微落后
        this.adviceCardClass = 'warning';
        this.adviceIcon = '💪';
        this.adviceTitle = '需要略微加速';
        this.adviceText = `剩余${this.remainingDistance}km，需要提升配速约${Math.round(Math.abs(paceDiff) * 60)}秒/公里。注意保持呼吸节奏，逐步提速。`;
      } else if (timeDiff > 10) {
        // 明显落后
        this.adviceCardClass = 'danger';
        this.adviceIcon = '⚠️';
        this.adviceTitle = '需要明显加速';
        this.adviceText = `目前落后较多，建议评估体能状况。如果感觉良好，可以尝试提速；如果疲劳，建议调整目标时间。`;
      } else if (timeDiff < -10) {
        // 超前太多
        this.adviceCardClass = 'info';
        this.adviceIcon = '🐢';
        this.adviceTitle = '可以适当放松';
        this.adviceText = `当前进度超前，可以适当放慢节奏，为后半程保留体能。注意不要过度放松。`;
      } else {
        // 略微超前
        this.adviceCardClass = 'success';
        this.adviceIcon = '👍';
        this.adviceTitle = '节奏很好';
        this.adviceText = `当前节奏良好，可以略微放松但要保持稳定。注意补充能量和水分。`;
      }
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

/* 顶部导航栏 - 与配速计算器保持一致 */
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

/* 距离选择器 - 与配速计算器保持一致 */
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

/* 输入行 */
.input-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f4efe7;
}

.input-row:last-child {
  border-bottom: none;
}

.input-label {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.label-icon {
  font-size: 28rpx;
}

.label-text {
  font-size: 28rpx;
  font-weight: 600;
  color: #1c170d;
}

.input-field-group {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.input-field {
  width: 160rpx;
  height: 72rpx;
  background-color: #f4efe7;
  border: 2rpx solid #f4efe7;
  border-radius: 12rpx;
  text-align: center;
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
}

.input-unit {
  font-size: 24rpx;
  color: #9d8148;
  min-width: 40rpx;
}

/* 时间输入 */
.time-inputs {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.time-field-group {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.time-field {
  width: 100rpx;
  height: 72rpx;
  background-color: #f4efe7;
  border: 2rpx solid #f4efe7;
  border-radius: 12rpx;
  text-align: center;
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
}

.time-colon {
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
}

.field-unit {
  font-size: 22rpx;
  color: #9d8148;
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

/* 状态卡片 */
.status-card {
  background-color: #f8f9fa;
}

.status-header {
  margin-bottom: 24rpx;
}

.status-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #1c170d;
}

.status-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24rpx;
}

.status-item {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.status-label {
  font-size: 24rpx;
  color: #9d8148;
}

.status-value {
  font-size: 32rpx;
  font-weight: 700;
  color: #1c170d;
}

/* 高亮卡片 */
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
  margin-bottom: 16rpx;
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

.pace-change {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 12rpx 24rpx;
  border-radius: 12rpx;
  background-color: #f4efe7;
}

.pace-change.faster {
  background-color: #ffebee;
}

.pace-change.slower {
  background-color: #e3f2fd;
}

.pace-change.neutral {
  background-color: #e8f5e9;
}

.change-icon {
  font-size: 24rpx;
  font-weight: 700;
}

.change-text {
  font-size: 24rpx;
  font-weight: 600;
  color: #1c170d;
}

/* 对比表格 */
.comparison-table {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.comparison-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx;
  background-color: #f8f9fa;
  border-radius: 12rpx;
}

.comparison-label {
  font-size: 26rpx;
  color: #1c170d;
}

.comparison-value {
  font-size: 28rpx;
  font-weight: 700;
  color: #1c170d;
}

.comparison-value.highlight {
  color: #f7bd4a;
}

.comparison-value.faster {
  color: #f44336;
}

.comparison-value.slower {
  color: #2196f3;
}

.comparison-value.neutral {
  color: #4caf50;
}

/* 预测结果 */
.prediction-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f4efe7;
}

.prediction-item:last-child {
  border-bottom: none;
}

.prediction-label {
  font-size: 26rpx;
  color: #1c170d;
}

.prediction-value {
  font-size: 28rpx;
  font-weight: 700;
  color: #1c170d;
}

.prediction-value.target {
  color: #f7bd4a;
}

/* 建议卡片 */
.tips-card {
  display: flex;
  gap: 16rpx;
  padding: 24rpx;
  border-radius: 16rpx;
  margin-top: 16rpx;
}

.tips-card.success {
  background-color: #e8f5e9;
}

.tips-card.warning {
  background-color: #fff3e0;
}

.tips-card.danger {
  background-color: #ffebee;
}

.tips-card.info {
  background-color: #e3f2fd;
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
  color: #1c170d;
}

.tips-text {
  font-size: 24rpx;
  color: #1c170d;
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

  .input-field,
  .time-field {
    width: 120rpx;
    height: 64rpx;
    font-size: 28rpx;
  }

  .value-number {
    font-size: 60rpx;
  }
}
</style>

