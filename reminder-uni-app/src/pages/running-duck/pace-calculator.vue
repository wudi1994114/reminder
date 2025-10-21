<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="header-section">
      <view class="nav-container">
        <view class="nav-back" @click="goBack">
          <text class="back-icon">‹</text>
        </view>
        <view class="title-contain er">
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
            <view 
              class="distance-option" 
              :class="{ 'active': distanceType === 'custom' }"
              @click="selectDistance('custom')"
            >
              <text class="distance-emoji">📏</text>
              <text class="distance-name">自定义距离</text>
              <text class="distance-value custom-hint">点击设置</text>
            </view>
          </view>
          
          <!-- 自定义距离输入 -->
          <view v-if="distanceType === 'custom'" class="custom-distance-input">
            <view class="input-row-inline">
              <text class="input-label-inline">比赛距离</text>
              <input 
                class="inline-input" 
                type="digit"
                v-model="customDistance"
                :placeholder="placeholders.customDistance"
                @input="onCustomDistanceInput"
                @focus="onFocus('customDistance')"
                @blur="onBlur('customDistance')"
              />
              <text class="input-label-inline">公里</text>
            </view>
            <view class="time-tips">
              <text class="tip-text">💡 例如：10公里、5公里等任意距离</text>
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
        
        <!-- 前N公里配速设置（可选） -->
        <view class="section-card">
          <view class="section-header">
            <view class="section-title">
              <text class="title-text">前N公里配速</text>
              <text class="optional-tag">可选</text>
            </view>
            <switch 
              :checked="enableFirstNKm" 
              @change="onToggleFirstNKm"
              color="#f7bd4a"
            />
          </view>
          
          <view v-if="enableFirstNKm" class="first-n-km-inputs">
            <!-- 前N公里距离 -->
            <view class="input-row-inline">
              <text class="input-label-inline">前</text>
              <input 
                class="inline-input" 
                type="digit"
                v-model="firstNKmDistance"
                :placeholder="placeholders.firstNKmDistance"
                @input="onFirstNKmDistanceInput"
                @focus="onFocus('firstNKmDistance')"
                @blur="onBlur('firstNKmDistance')"
              />
              <text class="input-label-inline">公里的配速为</text>
            </view>
            
            <!-- 前N公里配速 -->
            <view class="pace-input-row">
              <input 
                class="pace-input" 
                type="digit"
                v-model="firstNKmPaceMinutes"
                :placeholder="placeholders.firstNKmPaceMinutes"
                @input="onPaceInput"
                @focus="onFocus('firstNKmPaceMinutes')"
                @blur="onBlur('firstNKmPaceMinutes')"
                maxlength="2"
              />
              <text class="pace-separator">'</text>
              <input 
                class="pace-input" 
                type="digit"
                v-model="firstNKmPaceSeconds"
                :placeholder="placeholders.firstNKmPaceSeconds"
                @input="onPaceInput"
                @focus="onFocus('firstNKmPaceSeconds')"
                @blur="onBlur('firstNKmPaceSeconds')"
                maxlength="2"
              />
              <text class="pace-separator">"</text>
              <text class="pace-unit">/公里</text>
            </view>
            
            <view class="time-tips">
              <text class="tip-text">💡 例如：前10公里配速为 5'30"/km</text>
            </view>
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
          <view class="result-card highlight-card" v-if="!enableFirstNKm">
            <view class="result-header">
              <text class="result-icon">⚡</text>
              <text class="result-title">平均配速</text>
            </view>
            <view class="result-value-large">
              <text class="value-number">{{ averagePace }}</text>
              <text class="value-unit">分钟/公里</text>
            </view>
          </view>
          
          <!-- 前N公里后续配速 -->
          <view v-if="enableFirstNKm && requiredPaceAfterN" class="pace-breakdown-section">
            <view class="result-card info-card">
              <view class="result-header">
                <text class="result-icon">📊</text>
                <text class="result-title">配速分析</text>
              </view>
              <view class="pace-breakdown-grid">
                <view class="breakdown-item">
                  <text class="breakdown-label">前 {{ firstNKmDistance }} km</text>
                  <text class="breakdown-value">{{ firstNKmPaceMinutes }}'{{ firstNKmPaceSeconds }}"</text>
                </view>
                <view class="breakdown-item">
                  <text class="breakdown-label">剩余 {{ remainingKmAfterN }} km</text>
                  <text class="breakdown-value highlight">{{ requiredPaceAfterN }}</text>
                </view>
              </view>
            </view>
            
            <view class="result-card highlight-card">
              <view class="result-header">
                <text class="result-icon">🎯</text>
                <text class="result-title">后续需要配速</text>
              </view>
              <view class="result-value-large">
                <text class="value-number">{{ requiredPaceAfterN }}</text>
                <text class="value-unit">分钟/公里</text>
              </view>
              <view class="pace-change" :class="paceChangeClass">
                <text class="change-icon">{{ paceChangeIcon }}</text>
                <text class="change-text">{{ paceChangeText }}</text>
              </view>
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
      distanceType: 'full', // 'full', 'half', 或 'custom'
      customDistance: '',
      targetHours: '',
      targetMinutes: '',
      showResult: false,
      averagePace: '',
      splits: [],
      
      // 前N公里配速功能
      enableFirstNKm: false,
      firstNKmDistance: '',
      firstNKmPaceMinutes: '',
      firstNKmPaceSeconds: '',
      requiredPaceAfterN: '',
      remainingKmAfterN: '',
      paceChangeClass: '',
      paceChangeIcon: '',
      paceChangeText: '',
      
      // 动态 placeholder
      placeholders: {
        targetHours: '0',
        targetMinutes: '00',
        customDistance: '10',
        firstNKmDistance: '5',
        firstNKmPaceMinutes: '5',
        firstNKmPaceSeconds: '00'
      }
    };
  },
  computed: {
    canCalculate() {
      const hours = parseInt(this.targetHours) || 0;
      const minutes = parseInt(this.targetMinutes) || 0;
      const hasTime = hours > 0 || minutes > 0;
      
      // 如果是自定义距离，需要检查距离是否有效
      if (this.distanceType === 'custom') {
        const distance = parseFloat(this.customDistance) || 0;
        return hasTime && distance > 0;
      }
      
      return hasTime;
    },
    totalDistance() {
      if (this.distanceType === 'custom') {
        return parseFloat(this.customDistance) || 0;
      }
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
          targetMinutes: '00',
          customDistance: '10',
          firstNKmDistance: '5',
          firstNKmPaceMinutes: '5',
          firstNKmPaceSeconds: '00'
        };
        this.placeholders[field] = defaultPlaceholders[field];
      }
    },
    
    onCustomDistanceInput() {
      const distance = parseFloat(this.customDistance);
      if (distance && distance > 999) {
        this.customDistance = '999';
        uni.showToast({
          title: '距离不能超过999公里',
          icon: 'none'
        });
      }
      if (distance && distance < 0.1) {
        this.customDistance = '0.1';
        uni.showToast({
          title: '距离不能小于0.1公里',
          icon: 'none'
        });
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
    
    onToggleFirstNKm(e) {
      this.enableFirstNKm = e.detail.value;
      // 清空结果
      this.showResult = false;
    },
    
    onFirstNKmDistanceInput() {
      const distance = parseFloat(this.firstNKmDistance);
      if (distance && distance >= this.totalDistance) {
        this.firstNKmDistance = (this.totalDistance - 1).toString();
        uni.showToast({
          title: '距离必须小于总距离',
          icon: 'none'
        });
      }
    },
    
    onPaceInput() {
      // 限制配速输入范围
      if (this.firstNKmPaceMinutes && parseInt(this.firstNKmPaceMinutes) > 99) {
        this.firstNKmPaceMinutes = '99';
      }
      if (this.firstNKmPaceSeconds && parseInt(this.firstNKmPaceSeconds) > 59) {
        this.firstNKmPaceSeconds = '59';
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
      
      // 如果启用了前N公里配速功能
      if (this.enableFirstNKm) {
        this.calculatePaceWithFirstNKm(totalMinutes);
        return;
      }
      
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
    },
    
    calculatePaceWithFirstNKm(totalMinutes) {
      // 验证前N公里输入
      const firstNKm = parseFloat(this.firstNKmDistance);
      if (!firstNKm || firstNKm <= 0 || firstNKm >= this.totalDistance) {
        uni.showToast({
          title: '请输入有效的前N公里距离',
          icon: 'none'
        });
        return;
      }
      
      const paceMin = parseInt(this.firstNKmPaceMinutes) || 0;
      const paceSec = parseInt(this.firstNKmPaceSeconds) || 0;
      if (paceMin === 0 && paceSec === 0) {
        uni.showToast({
          title: '请输入前N公里的配速',
          icon: 'none'
        });
        return;
      }
      
      // 计算前N公里用时（分钟）
      const firstNKmPacePerKm = paceMin + paceSec / 60;
      const firstNKmTotalMinutes = firstNKm * firstNKmPacePerKm;
      
      // 检查前N公里用时是否已经超过目标时间
      if (firstNKmTotalMinutes >= totalMinutes) {
        uni.showToast({
          title: '前N公里用时已超过目标时间，无法达成目标',
          icon: 'none',
          duration: 2000
        });
        return;
      }
      
      // 计算剩余距离和时间
      const remainingDistance = this.totalDistance - firstNKm;
      const remainingMinutes = totalMinutes - firstNKmTotalMinutes;
      
      // 计算后续需要的配速
      const requiredPacePerKm = remainingMinutes / remainingDistance;
      const reqPaceMinutes = Math.floor(requiredPacePerKm);
      const reqPaceSeconds = Math.round((requiredPacePerKm - reqPaceMinutes) * 60);
      
      this.requiredPaceAfterN = `${reqPaceMinutes}'${String(reqPaceSeconds).padStart(2, '0')}"`;
      this.remainingKmAfterN = remainingDistance.toFixed(2);
      
      // 计算配速差异，用于提示
      const paceDiff = requiredPacePerKm - firstNKmPacePerKm;
      const paceDiffSeconds = Math.abs(Math.round(paceDiff * 60));
      
      if (Math.abs(paceDiff) < 0.05) {
        // 配速基本一致
        this.paceChangeClass = 'neutral';
        this.paceChangeIcon = '✓';
        this.paceChangeText = '保持配速即可完成目标';
      } else if (paceDiff > 0) {
        // 需要减速
        this.paceChangeClass = 'slower';
        this.paceChangeIcon = '↓';
        this.paceChangeText = `需要放慢 ${paceDiffSeconds} 秒/公里`;
      } else {
        // 需要加速
        this.paceChangeClass = 'faster';
        this.paceChangeIcon = '↑';
        this.paceChangeText = `需要加快 ${paceDiffSeconds} 秒/公里`;
      }
      
      // 计算分段用时（基于后续配速）
      this.splits = [];
      let cumulativeMinutes = firstNKmTotalMinutes;
      
      // 从第一个完整的5km开始
      const firstSegmentEnd = Math.ceil(firstNKm / 5) * 5;
      
      for (let i = firstSegmentEnd; i <= this.totalDistance; i += 5) {
        const segmentStart = i - 5;
        let segmentTime;
        
        if (segmentStart < firstNKm) {
          // 跨越前N公里边界的分段
          const beforeN = firstNKm - segmentStart;
          const afterN = 5 - beforeN;
          segmentTime = beforeN * firstNKmPacePerKm + afterN * requiredPacePerKm;
        } else {
          // 完全在后续配速范围内
          segmentTime = 5 * requiredPacePerKm;
        }
        
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
        const segmentTime = lastDistance * requiredPacePerKm;
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
          scrollTop: 800,
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
  gap: 12rpx;
  flex-wrap: nowrap;
}

.distance-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  padding: 20rpx 12rpx;
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

.distance-value.custom-hint {
  color: #f7bd4a;
  font-weight: 600;
}

/* 自定义距离输入 */
.custom-distance-input {
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid #f4efe7;
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

/* 前N公里配速输入 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.optional-tag {
  margin-left: 12rpx;
  padding: 4rpx 12rpx;
  background-color: #f7bd4a;
  color: #1c170d;
  font-size: 20rpx;
  font-weight: 600;
  border-radius: 8rpx;
}

.first-n-km-inputs {
  padding-top: 24rpx;
  border-top: 1rpx solid #f4efe7;
}

.input-row-inline {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  margin-bottom: 24rpx;
}

.input-label-inline {
  font-size: 26rpx;
  color: #1c170d;
}

.inline-input {
  width: 120rpx;
  height: 64rpx;
  background-color: #f4efe7;
  border: 2rpx solid #f4efe7;
  border-radius: 12rpx;
  text-align: center;
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
}

.pace-input-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  margin-bottom: 16rpx;
}

.pace-input {
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

.pace-separator {
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
}

.pace-unit {
  font-size: 22rpx;
  color: #9d8148;
  margin-left: 8rpx;
}

/* 配速分析 */
.pace-breakdown-section {
  margin-bottom: 24rpx;
}

.info-card {
  background-color: #f8f9fa;
  margin-bottom: 24rpx;
}

.pace-breakdown-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24rpx;
}

.breakdown-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  padding: 16rpx;
  background-color: #ffffff;
  border-radius: 12rpx;
}

.breakdown-label {
  font-size: 24rpx;
  color: #9d8148;
}

.breakdown-value {
  font-size: 32rpx;
  font-weight: 700;
  color: #1c170d;
}

.breakdown-value.highlight {
  color: #f7bd4a;
}

.pace-change {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 12rpx 24rpx;
  border-radius: 12rpx;
  background-color: #f4efe7;
  margin-top: 16rpx;
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
  
  .distance-selector {
    gap: 8rpx;
  }
  
  .distance-option {
    padding: 16rpx 8rpx;
    gap: 6rpx;
  }
  
  .distance-emoji {
    font-size: 40rpx;
  }
  
  .distance-name {
    font-size: 22rpx;
  }
  
  .distance-value {
    font-size: 20rpx;
  }

  .time-input {
    width: 140rpx;
    height: 100rpx;
    font-size: 40rpx;
  }
  
  .inline-input {
    width: 100rpx;
    height: 56rpx;
    font-size: 28rpx;
  }
  
  .pace-input {
    width: 90rpx;
    height: 64rpx;
    font-size: 28rpx;
  }

  .value-number {
    font-size: 60rpx;
  }
}
</style>

