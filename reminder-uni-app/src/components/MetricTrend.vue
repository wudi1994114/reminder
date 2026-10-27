<template>
  <view class="trend-overlay" @touchmove.stop.prevent @click="emit('close')">
  <view :id="dialogId" class="trend-dialog" role="dialog" aria-modal="true" :aria-label="`${measurement.label}变化`" tabindex="-1" @click.stop>
    <view class="trend-toolbar">
      <text class="trend-title">{{ measurement.label }}变化</text>
      <view class="trend-ranges" role="group" aria-label="查看时间范围">
        <button v-for="range in ranges" :key="range.days" class="range-button" :class="{ selected: days === range.days }" :aria-pressed="days === range.days" tabindex="0" @click="days = range.days" @keydown.enter.stop.prevent="days = range.days" @keydown.space.stop.prevent="days = range.days">{{ range.label }}</button>
      </view>
      <button :id="closeId" class="close-trend" aria-label="关闭变化图" tabindex="0" @click="emit('close')" @keydown.enter.stop.prevent="emit('close')" @keydown.space.stop.prevent="emit('close')">×</button>
    </view>
    <view v-if="state.loading" class="trend-message" role="status">读取记录中…</view>
    <view v-else-if="state.error" class="trend-message error-message" role="alert"><text>{{ state.error }}</text><button class="retry-trend" tabindex="0" @click="load" @keydown.enter.stop.prevent="load" @keydown.space.stop.prevent="load">重试</button></view>
    <view v-else-if="!state.points.length" class="trend-message">这段时间还没有{{ measurement.label }}记录</view>
    <template v-else>
      <view class="trend-summary" aria-live="polite">
        <text class="point-date">{{ selectedPoint.recordDate.replace(/-/g, '.') }}</text>
        <text class="point-value">{{ selectedPoint.value }}<text class="point-unit"> {{ measurement.unit }}</text></text>
        <text class="trend-change">{{ changeLabel }}</text>
      </view>
      <view role="img" :aria-label="chartDescription">
        <canvas :id="canvasId" :canvas-id="canvasId" class="trend-canvas" :style="{height: chartHeight + 'px'}" aria-hidden="true" @touchend="selectPoint" @click="selectPoint" />
      </view>
      <text class="trend-caption">{{ state.points.length === 1 ? '仅1次记录，继续记录可查看变化' : `共${state.points.length}次记录 · 点击曲线查看数值` }}</text>
    </template>
  </view>
  </view>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { dailyRecordsApi } from '@/api/dailyRecords.js';
import { MEASUREMENTS, createTrendState, createTrendLoader, drawTrend, nearestTrendPoint, trendLayout, trendPointerX, trendSummary } from '@/utils/daily-record-trend.mjs';

const props = defineProps({ metric: { type: String, required: true }, end: { type: String, required: true }, userId: { type: [Number, String], default: null } });
const emit = defineEmits(['close']);
const instance = getCurrentInstance();
const canvasId = `measurement-trend-${Math.random().toString(36).slice(2)}`;
const dialogId = `${canvasId}-dialog`, closeId = `${canvasId}-close`;
const chartHeight = 176;
const ranges = [{ days: 30, label: '30天' }, { days: 90, label: '90天' }, { days: 365, label: '一年' }];
const days = ref(30), state = reactive(createTrendState());
const loader = createTrendLoader({ state, getTrend: args => dailyRecordsApi.getTrend(args) });
const measurement = computed(() => MEASUREMENTS[props.metric]);
const summary = computed(() => trendSummary(state.points, state.selectedIndex));
const selectedPoint = computed(() => summary.value?.selected);
const changeLabel = computed(() => {
  if (!summary.value || summary.value.count < 2) return '';
  const change = summary.value.selectedChange;
  return change ? `较首条 ${change > 0 ? '+' : ''}${change.toFixed(1)} ${measurement.value.unit}` : '与首条相同';
});
const chartDescription = computed(() => `${measurement.value.label}变化图，${state.points.length}次记录，选中${selectedPoint.value?.recordDate ?? ''}，${selectedPoint.value?.value ?? ''}${measurement.value.unit}，${changeLabel.value}`);
let alive = true, drawSequence = 0, lastLayout = null, canvasRect = null;
let releaseDialog = () => {};

onMounted(async () => {
  // #ifdef H5
  await nextTick();
  if (!alive) return;
  const dialog = document.getElementById(dialogId), close = document.getElementById(closeId);
  if (!dialog || !close) return;
  const previousFocus = document.activeElement === document.body ? document.querySelector('.metric-active') : document.activeElement;
  const previousOverflow = document.body.style.overflow, background = [];
  let branch = dialog.parentElement;
  while (branch?.parentElement) {
    for (const sibling of branch.parentElement.children) {
      if (sibling !== branch) { background.push([sibling, sibling.inert]); sibling.inert = true; }
    }
    if (branch.parentElement === document.body) break;
    branch = branch.parentElement;
  }
  document.body.style.overflow = 'hidden';
  const handleKey = event => {
    if (event.key === 'Escape') { event.preventDefault(); event.stopPropagation(); emit('close'); return; }
    if (event.key !== 'Tab') return;
    const items = [...dialog.querySelectorAll('[tabindex="0"]')].filter(item => !item.disabled && item.getClientRects().length);
    if (!items.length) return;
    const index = items.indexOf(document.activeElement);
    if ((event.shiftKey && index <= 0) || (!event.shiftKey && (index === -1 || index === items.length - 1))) {
      event.preventDefault(); items[event.shiftKey ? items.length - 1 : 0].focus();
    }
  };
  document.addEventListener('keydown', handleKey, true);
  close.focus();
  releaseDialog = () => {
    document.removeEventListener('keydown', handleKey, true);
    document.body.style.overflow = previousOverflow;
    for (const [element, inert] of background) element.inert = inert;
    if (previousFocus?.isConnected && previousFocus.getClientRects().length) previousFocus.focus();
  };
  // #endif
});

function load() { return loader.load({ userId: props.userId, metric: props.metric, end: props.end, days: days.value }); }
async function drawChart() {
  const sequence = ++drawSequence;
  await nextTick();
  if (!alive || sequence !== drawSequence || state.loading || !state.points.length) return;
  uni.createSelectorQuery().in(instance.proxy).select(`#${canvasId}`).boundingClientRect(rect => {
    if (!alive || sequence !== drawSequence || !rect?.width || !state.range || state.loading || !state.points.length) return;
    canvasRect = rect;
    lastLayout = trendLayout(state.points, state.range, rect.width, chartHeight);
    drawTrend(uni.createCanvasContext(canvasId, instance.proxy), lastLayout, state.selectedIndex);
  }).exec();
}
function selectPoint(event) {
  if (!lastLayout || !canvasRect || state.loading) return;
  let detailIsLocal = true;
  // #ifdef H5
  detailIsLocal = false;
  // #endif
  const x = trendPointerX(event, canvasRect, detailIsLocal);
  const index = nearestTrendPoint(lastLayout, x);
  if (index >= 0) state.selectedIndex = index;
}
watch([() => props.metric, () => props.end, () => props.userId, days], load, { immediate: true });
watch([() => state.points, () => state.selectedIndex], drawChart, { flush: 'post' });
if (typeof uni.onWindowResize === 'function') uni.onWindowResize(drawChart);
onBeforeUnmount(() => {
  alive = false; ++drawSequence; loader.dispose();
  releaseDialog();
  if (typeof uni.offWindowResize === 'function') uni.offWindowResize(drawChart);
});
</script>

<style scoped>
.trend-overlay { position: fixed; top: 0; right: 0; bottom: 0; left: 0; z-index: var(--layer-modal, 1000); display: flex; align-items: center; justify-content: center; box-sizing: border-box; padding: 16px; background: rgba(28, 23, 13, .4); overflow: hidden; touch-action: none; }
.trend-dialog { box-sizing: border-box; width: 100%; max-width: 420px; max-height: calc(100vh - 32px); padding: 4px 16px 12px; border: 1px solid #dfd4c0; border-radius: 12px; background: #fffdf8; overflow-y: auto; color: #1c170d; }
.trend-toolbar, .trend-ranges, .trend-summary { display: flex; align-items: center; }
.trend-toolbar { gap: 4px; min-height: 44px; }
.trend-title { flex: 1; min-width: 0; font-size: 15px; font-weight: 600; white-space: nowrap; }
.range-button, .close-trend, .retry-trend { min-width: 44px; min-height: 44px; margin: 0; padding: 0 6px; color: #6d604c; background: transparent; font-size: 12px; line-height: 44px; border-radius: 4px; }
.range-button.selected { background: #fff0cc; color: #78551c; font-weight: 600; }
.close-trend, .retry-trend { color: #78551c; }
.close-trend { padding: 0; font-size: 28px; font-weight: 400; }
.trend-summary { gap: 8px; min-height: 30px; font-variant-numeric: tabular-nums; }
.point-date { font-size: 12px; color: #6d604c; }
.point-value { font-size: 16px; font-weight: 600; }
.point-unit, .trend-change { font-size: 12px; color: #6d604c; font-weight: 400; }
.trend-change { margin-left: auto; }
.trend-canvas { display: block; width: 100%; }
.trend-caption { display: block; font-size: 11px; line-height: 18px; color: #6d604c; }
.trend-message { display: flex; align-items: center; justify-content: space-between; gap: 8px; min-height: 72px; font-size: 13px; line-height: 20px; color: #6d604c; }
.error-message { color: #a23d32; }
button::after { border: 0; }
button:focus-visible { outline: 2px solid #8b6018; outline-offset: 2px; }
button:active { background: #f4ead4; }
@media (hover: hover) { button:hover { background: #f5ecd9; } }
</style>
