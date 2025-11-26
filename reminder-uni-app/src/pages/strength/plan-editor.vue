<template>
  <view class="page">
    <view class="header">
      <input class="name" v-model="plan.name" placeholder="计划名称" />
      <input class="desc" v-model="plan.description" placeholder="描述（可选）" />
    </view>
    <scroll-view scroll-y class="list">
      <view v-for="(it,idx) in items" :key="idx" class="item">
        <view class="title">{{ it.exercise?.name || '选择动作' }}</view>
        <view class="row">
          <input type="number" v-model.number="it.reps" class="num" />
          <text>次 ×</text>
          <input type="number" v-model.number="it.sets" class="num" />
          <text>组 · 休</text>
          <input type="number" v-model.number="it.restSec" class="num" />
          <text>s</text>
        </view>
        <view class="ops">
          <button size="mini" @click="pickExercise(idx)">选择动作</button>
          <button size="mini" type="warn" @click="remove(idx)">删除</button>
        </view>
      </view>
      <view class="add">
        <button type="primary" @click="addItem">添加一项</button>
      </view>
    </scroll-view>
    <view class="footer">
      <button type="primary" @click="save">保存计划</button>
    </view>
  </view>
</template>

<script>
import { strengthApi } from '@/api/strength.js';

export default {
  name: 'PlanEditor',
  data() {
    return {
      planId: null,
      plan: { name: '', description: '', isPublic: false },
      items: []
    };
  },
  onLoad(query) {
    if (query && query.id) {
      this.planId = Number(query.id);
      this.loadPlan();
    } else {
      this.addItem();
    }
  },
  methods: {
    async loadPlan() {
      const res = await strengthApi.getPlan(this.planId);
      const plan = res.plan || res?.data?.plan || res;
      const items = res.items || res?.data?.items || [];
      this.plan = plan;
      // 回显时仅保留必要字段
      this.items = items.map(x => ({
        exerciseId: x.exerciseId,
        reps: x.reps || 11,
        sets: x.sets || 3,
        restSec: x.restSec || 30
      }));
    },
    addItem() {
      this.items.push({ exerciseId: null, reps: 11, sets: 3, restSec: 30 });
    },
    remove(idx) {
      this.items.splice(idx,1);
    },
    async pickExercise(idx) {
      // 简化：跳到市场页并选择后返回（可后续用事件总线/选择器组件优化）
      uni.navigateTo({ url: '/pages/strength/exercise-market' });
      uni.$once('exercise:selected', (ex) => {
        const it = this.items[idx];
        it.exerciseId = ex.id;
        it.reps = ex.defaultReps || 11;
        it.sets = ex.defaultSets || 3;
        it.restSec = ex.defaultRestSec || 30;
        it.exercise = ex;
      });
    },
    async save() {
      const payload = { plan: this.plan, items: this.items.map((x, i) => ({
        orderIndex: i + 1,
        exerciseId: x.exerciseId,
        reps: x.reps || 11,
        sets: x.sets || 3,
        restSec: x.restSec || 30
      })) };
      if (this.planId) {
        await strengthApi.updatePlan(this.planId, payload);
      } else {
        const res = await strengthApi.createPlan(payload);
        this.planId = res?.plan?.id || res?.id || this.planId;
      }
      uni.showToast({ title: '已保存', icon: 'success' });
      setTimeout(() => uni.navigateBack(), 500);
    }
  }
};
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100vh; }
.header { padding: 16rpx; background: #f8f8f8; }
.name, .desc { background: #fff; padding: 12rpx; border-radius: 8rpx; margin-bottom: 12rpx; }
.list { flex: 1; padding: 16rpx; }
.item { background: #fff; border-radius: 12rpx; padding: 16rpx; margin-bottom: 16rpx; }
.title { font-size: 28rpx; font-weight: 600; margin-bottom: 8rpx; }
.row { display: flex; align-items: center; gap: 8rpx; }
.num { width: 100rpx; background: #f5f5f5; padding: 8rpx; border-radius: 8rpx; text-align: center; }
.ops { margin-top: 12rpx; display: flex; gap: 12rpx; }
.add { padding: 12rpx 0; }
.footer { padding: 16rpx; background: #f7f7f7; }
</style>


