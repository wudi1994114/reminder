<template>
  <view class="daily-record-panel" :aria-label="`${date} 每日记录`">
    <view v-if="state.editing" class="record-heading">
      <text class="record-title">编辑记录</text>
      <button class="record-action" :disabled="state.saving" @click="session.closeEditor()">收起</button>
    </view>

    <view v-if="!userId" class="record-sign-in">
      <text>登录后，记下体重和今天的小事</text>
      <button class="record-action" @click="signIn">登录记录</button>
    </view>
    <view v-else-if="state.loading" class="record-loading" aria-live="polite">
      <view class="skeleton-metrics"></view><view class="skeleton-diary"></view>
      <text class="record-note">正在读取这天的记录…</text>
    </view>
    <view v-else-if="!state.loaded" class="record-load-error" role="alert">
      <text>{{ state.error || '记录暂时无法读取' }}</text>
      <button class="record-action" @click="reload">重新加载</button>
    </view>
    <template v-else>
      <template v-if="!state.editing">
        <view class="record-overview">
          <view class="record-metrics">
            <button class="metric-summary" :class="{ 'metric-active': activeTrend === 'weightKg' }" :aria-label="weightLabel" :aria-expanded="activeTrend === 'weightKg'" aria-haspopup="dialog" tabindex="0" @click="showTrend('weightKg')">
              <text class="metric-label">体重</text>
              <view class="metric-reading"><text class="metric-value" :class="{ 'metric-empty': state.record?.weightKg == null }">{{ state.record?.weightKg ?? '未记录' }}</text><text v-if="state.record?.weightKg != null" class="metric-unit">kg</text></view>
            </button>
            <button class="metric-summary" :class="{ 'metric-active': activeTrend === 'heightCm' }" :aria-label="heightLabel" :aria-expanded="activeTrend === 'heightCm'" aria-haspopup="dialog" tabindex="0" @click="showTrend('heightCm')">
              <text class="metric-label">身高</text>
              <view class="metric-reading"><text class="metric-value" :class="{ 'metric-empty': heightValue == null }">{{ heightValue ?? '未记录' }}</text><text v-if="heightValue != null" class="metric-unit">cm</text></view>
            </button>
          </view>
          <button class="record-action compact-edit" :disabled="state.saving" aria-label="编辑当天记录" @click="startEditing('weightKg')">{{ state.record ? '编辑' : '记录' }}</button>
        </view>
        <button class="diary-summary" @click="startEditing('diary')">
          <text class="diary-label">备注</text>
          <text v-if="state.record?.diary" class="diary-preview">{{ state.record.diary }}</text>
          <text v-else class="diary-placeholder">添加备注…</text>
          <text class="diary-open" aria-hidden="true">›</text>
        </button>
        <text v-if="hasDraft" class="record-note draft-note">有草稿，点编辑继续</text>
      </template>

      <view v-else class="record-editor">
        <text v-if="state.draftRestored" class="draft-note" aria-live="polite">已恢复这天的未保存草稿</text>
        <view class="measurement-fields">
          <view class="measurement-field">
            <view class="number-input-wrap">
              <label class="input-label" for="daily-weight">体重</label>
              <input id="daily-weight" v-model="state.form.weightKg" class="number-input" type="digit" inputmode="decimal" :maxlength="6" :disabled="state.saving" :focus="focusField === 'weightKg'" placeholder="选填" placeholder-class="record-input-placeholder" aria-label="体重，单位千克" />
              <text class="input-unit">kg</text>
            </view>
          </view>
          <view class="measurement-field">
            <view class="number-input-wrap">
              <label class="input-label" for="daily-height">身高</label>
              <input id="daily-height" v-model="state.form.heightCm" class="number-input" type="digit" inputmode="decimal" :maxlength="6" :disabled="state.saving" :focus="focusField === 'heightCm'" :placeholder="heightValue == null ? '选填' : '沿用'" placeholder-class="record-input-placeholder" aria-label="本次测量身高，留空沿用上次记录，单位厘米" />
              <text class="input-unit">cm</text>
            </view>
          </view>
        </view>
        <text v-if="state.latestHeight" class="record-note">未填写时沿用上次身高</text>
        <view class="note-field-heading"><label class="input-label" for="daily-diary">备注 <text class="optional-label">选填</text></label><text class="diary-count">{{ state.form.diary.length }} / 10000</text></view>
        <textarea id="daily-diary" v-model="state.form.diary" class="diary-input" :maxlength="10000" :disabled="state.saving" :focus="focusField === 'diary'" :cursor-spacing="24" :show-confirm-bar="false" placeholder="填写备注，记录这天的情况…" placeholder-class="record-input-placeholder" aria-label="备注，最多10000字" />
        <view v-if="state.error" class="record-error" role="alert">
          <text>{{ state.error }}</text>
          <button v-if="state.conflict" class="record-action" @click="reloadLatest">载入最新记录</button>
        </view>
        <view class="editor-actions">
          <button v-if="state.record" class="delete-record" :disabled="state.saving" @click="confirmDelete">删除记录</button>
          <button class="save-record" :disabled="state.saving || state.conflict" :loading="state.saving" @click="save">{{ state.saving ? '正在保存' : '保存记录' }}</button>
        </view>
      </view>
      <text v-if="state.storageError" class="record-warning" role="status">{{ state.storageError }}</text>
      <view v-if="state.heightError" class="record-warning"><text>{{ state.heightError }}</text><button class="record-action" @click="reload">重试</button></view>
    </template>
    <view v-if="state.monthError" class="record-warning"><text>{{ state.monthError }}</text><button class="record-action" @click="refreshMonth">重试</button></view>
    <MetricTrend v-if="activeTrend" :metric="activeTrend" :end="date" :user-id="userId" @close="activeTrend = null" />
  </view>
</template>

<script setup>
import { computed, reactive, ref, watch, onBeforeUnmount, nextTick } from 'vue';
import { dailyRecordsApi } from '@/api/dailyRecords.js';
import { userState } from '@/services/userService.js';
import { requireAuth } from '@/utils/auth.js';
import { createDailyRecordState, createDailyRecordSession } from '@/utils/daily-records.mjs';
import MetricTrend from '@/components/MetricTrend.vue';

const props = defineProps({ date: { type: String, required: true }, month: { type: String, required: true } });
const emit = defineEmits(['record-dates']);
const userId = computed(() => userState.isAuthenticated && uni.getStorageSync('accessToken') ? userState.user?.id || null : null);
const state = reactive(createDailyRecordState());
const focusField = ref('');
const activeTrend = ref(null);
const session = createDailyRecordSession({ state, api: dailyRecordsApi, storage: {
  get: key => uni.getStorageSync(key), set: (key, value) => uni.setStorageSync(key, value), remove: key => uni.removeStorageSync(key),
} });
const heightValue = computed(() => state.record?.heightCm ?? state.latestHeight?.heightCm ?? null);
const hasDraft = computed(() => state.form.diary !== (state.record?.diary || '') || state.form.weightKg !== String(state.record?.weightKg ?? '') || state.form.heightCm !== String(state.record?.heightCm ?? ''));
const weightLabel = computed(() => `查看体重变化，${state.record?.weightKg == null ? '当天未记录' : `${state.record.weightKg}千克`}`);
const heightLabel = computed(() => `查看身高变化，${heightValue.value == null ? '尚未记录' : `${heightValue.value}厘米`}`);
watch([userId, () => props.date], ([id, date]) => { focusField.value = ''; activeTrend.value = null; session.loadDay(id, date); }, { immediate: true });
watch([userId, () => props.month], ([id, month]) => session.loadMonth(id, month), { immediate: true });
watch(() => state.recordDates, dates => emit('record-dates', dates), { immediate: true });
watch(() => state.form, () => session.persistDraft(), { deep: true, flush: 'sync' });

async function signIn() { await requireAuth(); }
async function startEditing(field) {
  if (!await requireAuth()) return;
  if (!state.loaded) { await session.loadDay(userId.value, props.date); if (!state.loaded) return; }
  activeTrend.value = null;
  session.beginEdit();
  focusField.value = '';
  await nextTick(); focusField.value = field;
}
async function showTrend(metric) {
  if (await requireAuth()) activeTrend.value = activeTrend.value === metric ? null : metric;
}
function reload() { return session.loadDay(userId.value, props.date); }
function refreshMonth() { return session.loadMonth(userId.value, props.month); }
async function save() {
  if (await session.save()) { uni.showToast({ title: '记录已保存', icon: 'success' }); refreshMonth(); }
}
function confirmDelete() {
  const date = props.date, id = userId.value, recordId = state.record?.id, version = state.record?.version;
  uni.showModal({ title: '删除这天的记录？', content: `${date} 的体重、身高和备注将一起删除。`, confirmText: '删除记录', confirmColor: '#a23d32',
    success: async result => { if (result.confirm && date === props.date && id === userId.value && recordId === state.record?.id && version === state.record?.version && await session.remove()) { uni.showToast({ title: '记录已删除', icon: 'success' }); refreshMonth(); } },
  });
}
function reloadLatest() {
  const date = props.date, id = userId.value;
  uni.showModal({ title: '载入最新记录？', content: '将用账号里最新保存的记录替换当前输入。你可以先复制要保留的备注。', confirmText: '载入最新',
    success: result => { if (result.confirm && date === props.date && id === userId.value) session.loadDay(id, date, { discardDraft: true }); },
  });
}
function refresh() { reload(); refreshMonth(); }
defineExpose({ refresh, persistDraft: session.persistDraft, onPageHide() { activeTrend.value = null; session.persistDraft(); } });
onBeforeUnmount(() => session.dispose());
</script>

<style scoped>
.daily-record-panel { margin: 0 16px 12px; padding: 0 10px; border: 1px solid #dfd4c0; border-radius: 8px; background: #fffdf8; color: #1c170d; }
.record-heading, .record-sign-in, .record-overview, .record-metrics, .measurement-fields, .note-field-heading, .editor-actions { display: flex; align-items: center; }
.record-heading { min-height: 44px; justify-content: space-between; }
.record-title { font-size: 14px; font-weight: 600; }
.record-action { flex-shrink: 0; min-width: 44px; min-height: 44px; padding: 0 6px; color: #78551c; font-size: 13px; line-height: 44px; font-weight: 600; }
.record-sign-in { min-height: 52px; justify-content: space-between; gap: 8px; }
.record-sign-in, .record-load-error { color: #6d604c; font-size: 13px; line-height: 1.5; }
.record-overview { gap: 8px; padding: 4px 0; }
.record-metrics { flex: 1; min-width: 0; gap: 12px; }
.metric-summary { flex: 1; min-width: 0; display: flex; align-items: center; gap: 6px; min-height: 44px; padding: 0; text-align: left; line-height: 1.35; border-radius: 4px; }
.metric-label { flex-shrink: 0; font-size: 13px; color: #6d604c; }
.metric-active { background: #fff0cc; }
.metric-reading { display: flex; align-items: baseline; gap: 3px; white-space: nowrap; }
.metric-value { font-size: 18px; font-weight: 600; font-variant-numeric: tabular-nums; }
.metric-empty { font-size: 13px; font-weight: 400; color: #6d604c; }
.metric-unit, .input-unit { font-size: 12px; color: #6d604c; }
.compact-edit { padding: 0; }
.diary-summary { display: flex; align-items: center; gap: 10px; width: 100%; min-height: 44px; padding: 8px 0; border-top: 1px solid #e9dfce; text-align: left; }
.diary-label { flex-shrink: 0; font-size: 13px; color: #6d604c; font-weight: 400; }
.diary-open { flex-shrink: 0; font-size: 20px; color: #78551c; line-height: 20px; }
.diary-preview, .diary-placeholder { flex: 1; min-width: 0; color: #403729; font-size: 14px; line-height: 20px; }
.diary-preview { white-space: pre-line; overflow: hidden; overflow-wrap: anywhere; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.diary-placeholder { color: #6d604c; }
.record-note, .draft-note, .record-warning { display: block; margin: 4px 0; font-size: 12px; color: #6d604c; line-height: 18px; }
.record-warning { color: #924531; }
.record-warning .record-action { display: inline-block; }
.draft-note { color: #78551c; }
.record-editor { padding-bottom: 8px; }
.measurement-fields { gap: 8px; }
.measurement-field { flex: 1; min-width: 0; }
.input-label { flex-shrink: 0; font-size: 13px; font-weight: 500; }
.optional-label { margin-left: 4px; font-size: 12px; color: #6d604c; font-weight: 400; }
.number-input-wrap { display: flex; align-items: center; gap: 6px; min-height: 44px; padding: 0 8px; border: 1px solid #baab91; border-radius: 6px; background: #fff; }
.number-input { flex: 1; min-width: 0; height: 42px; font-size: 16px; color: #1c170d; }
.note-field-heading { min-height: 32px; justify-content: space-between; gap: 8px; }
.diary-count { font-size: 11px; color: #6d604c; }
.diary-input { width: 100%; height: 88px; padding: 8px; border: 1px solid #baab91; border-radius: 6px; background: #fff; color: #1c170d; font-size: 14px; line-height: 20px; }
:deep(.record-input-placeholder) { color: #756751; }
.record-error { margin-top: 8px; color: #a23d32; font-size: 13px; line-height: 1.5; }
.editor-actions { justify-content: space-between; gap: 8px; margin-top: 8px; }
.save-record { min-height: 44px; margin-left: auto; padding: 0 20px; border-radius: 6px; background: #f7bd4a; color: #1c170d; font-size: 14px; font-weight: 600; line-height: 44px; }
.delete-record { min-height: 44px; padding: 0 6px; color: #a23d32; font-size: 13px; line-height: 44px; }
button { margin: 0; }
button::after { border: 0; }
button:focus-visible, input:focus-visible, textarea:focus-visible { outline: 2px solid #8b6018; outline-offset: 2px; }
button:active { background-color: #f4ead4; }
.save-record:active { background-color: #e6a73d; }
button[disabled] { opacity: .55; }
.record-loading { padding: 10px 0; }
.skeleton-metrics, .skeleton-diary { background: #eee6d8; border-radius: 4px; }
.skeleton-metrics { height: 28px; width: 70%; }
.skeleton-diary { height: 20px; margin-top: 8px; width: 90%; }
@media (hover: hover) { button:hover { background-color: #f5ecd9; } .save-record:hover { background-color: #edb13d; } }
@media (max-width: 350px) { .daily-record-panel { margin-left: 12px; margin-right: 12px; padding-left: 8px; padding-right: 8px; } .record-metrics { gap: 8px; } .record-overview { gap: 4px; } .number-input-wrap { padding: 0 6px; gap: 4px; } }
</style>
