<template>
  <view class="page">
    <view class="header">
      <view class="title">{{ sessionTitle }}</view>
      <view class="sub">{{ subTitle }}</view>
    </view>

    <scroll-view scroll-y class="content">
      <view class="panel">
        <view class="exercise">{{ currentExercise?.name || '准备开始' }}</view>
        <view class="row">
          <input type="number" v-model.number="current.reps" class="num" />
          <text>次 ×</text>
          <input type="number" v-model.number="current.sets" class="num" />
          <text>组 · 休</text>
          <input type="number" v-model.number="current.restSec" class="num" />
          <text>s</text>
        </view>
      </view>

      <view class="controls">
        <button @click="playAudio">播放音频</button>
        <button type="primary" @click="nextSet">完成一组</button>
      </view>

      <view v-if="resting" class="rest">
        <text>休息中：{{ restLeft }}s</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { strengthApi } from '@/api/strength.js';

export default {
  name: 'StartSession',
  data() {
    return {
      mode: 'single', // single | plan
      plan: null,
      items: [],
      index: 0,
      setIndex: 1,
      current: { reps: 11, sets: 3, restSec: 30 },
      currentExercise: null,
      resting: false,
      restLeft: 0,
      audioCtx: null
    };
  },
  computed: {
    sessionTitle() {
      return this.mode === 'single' ? (this.currentExercise?.name || '单项训练') : (this.plan?.name || '组合训练');
    },
    subTitle() {
      if (this.mode === 'single') return `第 ${this.setIndex}/${this.current.sets} 组`;
      const it = this.items[this.index];
      return it ? `第 ${this.index + 1}/${this.items.length} 项 · 第 ${this.setIndex}/${this.current.sets} 组` : '';
    }
  },
  onLoad(query) {
    try {
      if (query && query.payload) {
        const payload = JSON.parse(decodeURIComponent(query.payload));
        if (payload.type === 'single') {
          this.mode = 'single';
          this.currentExercise = payload.exercise;
          this.current.reps = payload.exercise?.defaultReps || 11;
          this.current.sets = payload.exercise?.defaultSets || 3;
          this.current.restSec = payload.exercise?.defaultRestSec || 30;
        } else if (payload.type === 'plan') {
          this.mode = 'plan';
          this.loadPlan(payload.id);
        }
      }
    } catch (e) {
      console.error(e);
    }
  },
  onUnload() {
    if (this.audioCtx) {
      try { this.audioCtx.stop(); this.audioCtx.destroy(); } catch(_) {}
      this.audioCtx = null;
    }
  },
  methods: {
    async loadPlan(id) {
      const res = await strengthApi.getPlan(Number(id));
      this.plan = res.plan || res;
      this.items = (res.items || []).map(x => ({
        exerciseId: x.exerciseId,
        reps: x.reps || 11,
        sets: x.sets || 3,
        restSec: x.restSec || 30
      }));
      await this.loadExerciseDetail(0);
    },
    async loadExerciseDetail(i) {
      const it = this.items[i];
      if (!it) return;
      const ex = await strengthApi.getExercise(it.exerciseId);
      this.currentExercise = ex;
      this.current.reps = it.reps || ex.defaultReps || 11;
      this.current.sets = it.sets || ex.defaultSets || 3;
      this.current.restSec = it.restSec || ex.defaultRestSec || 30;
      this.index = i;
      this.setIndex = 1;
    },
    playAudio() {
      // 兼容已有的音频逻辑：小程序使用 InnerAudioContext
      // 仅在有 audioUrl 时播放
      const url = this.currentExercise?.audioUrl;
      if (!url) {
        uni.showToast({ title: '无音频', icon: 'none' });
        return;
      }
      // #ifdef MP-WEIXIN
      if (!this.audioCtx) this.audioCtx = uni.createInnerAudioContext();
      this.audioCtx.autoplay = true;
      this.audioCtx.src = url;
      this.audioCtx.play();
      // #endif
    },
    nextSet() {
      if (this.setIndex < this.current.sets) {
        this.startRest(() => {
          this.setIndex += 1;
        });
      } else {
        // 下一项
        if (this.mode === 'plan' && this.index < this.items.length - 1) {
          this.startRest(async () => {
            await this.loadExerciseDetail(this.index + 1);
          });
        } else {
          uni.showToast({ title: '训练完成', icon: 'success' });
        }
      }
    },
    startRest(cb) {
      this.resting = true;
      this.restLeft = Number(this.current.restSec) || 30;
      const timer = setInterval(() => {
        if (this.restLeft <= 1) {
          clearInterval(timer);
          this.resting = false;
          cb && cb();
        } else {
          this.restLeft -= 1;
        }
      }, 1000);
    }
  }
};
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100vh; }
.header { padding: 20rpx; background: #f8f8f8; }
.title { font-size: 36rpx; font-weight: 700; }
.sub { margin-top: 6rpx; color: #888; }
.content { flex: 1; padding: 16rpx; }
.panel { background: #fff; border-radius: 12rpx; padding: 16rpx; }
.exercise { font-size: 32rpx; font-weight: 600; margin-bottom: 12rpx; }
.row { display: flex; align-items: center; gap: 8rpx; }
.num { width: 100rpx; background: #f5f5f5; padding: 8rpx; border-radius: 8rpx; text-align: center; }
.controls { display: flex; gap: 16rpx; margin: 16rpx 0; }
.rest { text-align: center; color: #ff6b00; font-size: 28rpx; }
</style>


