<template>
  <view class="page">
    <view class="search-bar">
      <input class="search-input" v-model="keyword" placeholder="搜索动作名称" @confirm="fetchList" />
      <picker :range="muscleOptions" range-key="label" @change="onMuscleChange">
        <view class="picker">{{ muscleLabel }}</view>
      </picker>
    </view>

    <scroll-view scroll-y class="list">
      <view v-for="item in list" :key="item.id" class="card" @click="goDetail(item)">
        <view class="title">{{ item.name }}</view>
        <view class="meta">{{ item.muscleGroup || '通用' }} · 默认 {{ item.defaultReps }} 次 × {{ item.defaultSets }} 组 · 休息 {{ item.defaultRestSec }}s</view>
      </view>
    </scroll-view>

    <view class="footer">
      <button type="primary" @click="createExercise">新建动作</button>
    </view>
  </view>
  
</template>

<script>
import { strengthApi } from '@/api/strength.js';

export default {
  name: 'ExerciseMarket',
  data() {
    return {
      keyword: '',
      muscleGroup: '',
      list: [],
      muscleOptions: [
        { value: '', label: '全部肌群' },
        { value: '胸', label: '胸' },
        { value: '背', label: '背' },
        { value: '腿', label: '腿' },
        { value: '肩', label: '肩' },
        { value: '手臂', label: '手臂' },
        { value: '核心', label: '核心' }
      ]
    };
  },
  computed: {
    muscleLabel() {
      const f = this.muscleOptions.find(x => x.value === this.muscleGroup);
      return f ? f.label : '全部肌群';
    }
  },
  onLoad() {
    this.fetchList();
  },
  methods: {
    async fetchList() {
      const res = await strengthApi.listExercises({ keyword: this.keyword, muscleGroup: this.muscleGroup });
      this.list = Array.isArray(res) ? res : (res?.data || []);
    },
    onMuscleChange(e) {
      const idx = Number(e.detail.value);
      this.muscleGroup = this.muscleOptions[idx].value;
      this.fetchList();
    },
    goDetail(item) {
      // 跳到执行页，传单项
      const payload = encodeURIComponent(JSON.stringify({ type: 'single', exercise: item }));
      uni.navigateTo({ url: `/pages/strength/start-session?payload=${payload}` });
    },
    async createExercise() {
      // 简化：直接创建一个空动作，后续可做编辑页
      const created = await strengthApi.createExercise({ name: '新建动作', muscleGroup: '', defaultReps: 11, defaultSets: 3, defaultRestSec: 30, isPublic: false });
      uni.showToast({ title: '已新建', icon: 'success' });
      this.fetchList();
    }
  }
};
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100vh; }
.search-bar { display: flex; gap: 12rpx; padding: 16rpx; }
.search-input { flex: 1; background: #fff; padding: 12rpx; border-radius: 8rpx; }
.picker { padding: 12rpx 16rpx; background: #fff; border-radius: 8rpx; }
.list { flex: 1; padding: 12rpx 16rpx; }
.card { background: #fff; border-radius: 12rpx; padding: 20rpx; margin-bottom: 16rpx; }
.title { font-size: 32rpx; font-weight: 600; }
.meta { color: #888; font-size: 24rpx; margin-top: 8rpx; }
.footer { padding: 16rpx; background: #f7f7f7; }
</style>


