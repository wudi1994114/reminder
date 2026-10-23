<template>
  <view class="beads-page">
    <view class="intro"><text class="title">我的拼豆工坊</text><text class="subtitle">选一张图片，自动做成拼豆图纸。</text></view>
    <view class="workspace">
      <view class="photo-row">
        <image v-if="sourcePath" class="photo" :src="sourcePath" mode="aspectFit" @click="previewOriginal" />
        <view v-else class="photo-placeholder"><text>▦</text></view>
        <view class="photo-copy"><text class="section-title">{{ sourcePath ? '图片已选好' : pattern ? '上次的图纸' : '从喜欢的图片开始' }}</text><text class="hint">{{ sourcePath ? '参数已按图片自动匹配' : '人物、宠物、插画都可以试试' }}</text><button class="button primary choose" :disabled="locked" @click="choosePhoto">{{ sourcePath || pattern ? '换张图片' : '选择图片' }}</button></view>
      </view>
      <view v-if="hasDraft && !pattern && !sourcePath" class="draft-row"><text class="hint">上次的制作已保存在本机</text><button class="button secondary" :disabled="locked" @click="restoreDraft">继续上次制作</button></view>
      <view class="setting-line"><text class="label">豆子品牌</text><picker :range="brands" range-key="name" :value="brandIndex" :disabled="locked || (!!pattern && !sourcePath)" @change="changeBrand"><view class="picker-value">{{ selectedBrand.name }} <text>⌄</text></view></picker></view>
      <view v-if="sourcePath" class="background-row"><view><text class="label">去除背景</text><text class="hint block">适合居中、轮廓清楚的单个主体</text></view><switch color="#bf861c" :checked="background === 'remove'" :disabled="locked" @change="background = $event.detail.value ? 'remove' : 'keep'" /></view>
    </view>
    <view v-if="busy" class="progress-note"><text>{{ phase }}</text><text class="hint block">在本机处理，请稍候</text></view>
    <view v-if="error" class="error-panel"><text>{{ error }}</text><button v-if="sourcePath" class="button secondary" :disabled="locked" @click="requestConvert">重试制作</button></view>
    <view v-if="pattern" class="pattern-section">
      <view class="section-heading"><view><text class="section-title">你的拼豆图纸</text><text class="hint block">{{ totalBeads }} 颗 · {{ materials.length }} 色 · {{ pattern.width }} × {{ pattern.height }} 格</text></view><button class="button text-button" :disabled="locked || settingsChanged" @click="editing = !editing">{{ editing ? '完成修图' : '修图' }}</button></view>
      <text v-if="assessmentReason" class="hint block recommendation">{{ assessmentReason }}</text>
      <view v-if="sourcePath" class="detail-actions"><button class="button secondary" :disabled="locked || columns <= 32" @click="adjustDetail(-1)">更省豆</button><button class="button secondary" :disabled="locked || columns >= 96" @click="adjustDetail(1)">更清楚</button></view>
      <view v-if="editing" class="zoom-row"><text class="hint">{{ Math.round(zoom * 100) }}%</text><view class="zoom-actions"><button class="button secondary square" :disabled="zoom <= 1 || locked" @click="zoom = Math.max(1, zoom - .5)">−</button><button class="button secondary square" :disabled="zoom >= 4 || locked" @click="zoom = Math.min(4, zoom + .5)">＋</button><button class="button text-button" :disabled="!history.length || locked" @click="undo">撤销</button></view></view>
      <scroll-view class="canvas-scroll" scroll-x scroll-y :scroll-with-animation="false" :style="{height: viewportHeight + 'px'}"><view class="canvas-wrap" :style="{width: canvasWidth + 'px', height: canvasHeight + 'px'}"><canvas id="bead-preview" type="2d" canvas-id="bead-preview" class="pattern-canvas" :style="{width: canvasWidth + 'px', height: canvasHeight + 'px'}" @touchstart="touchStart" @touchend="touchEnd" @click="clickCanvas" /></view></scroll-view>
      <text class="hint block preview-hint">{{ editing ? toolHint : '滑动查看图纸；进入“修图”后才能修改。' }}</text>
      <view v-if="editing" class="editor-panel">
        <view class="tools"><button v-for="item in tools" :key="item.id" class="button tool" :class="{active: tool === item.id}" :disabled="locked" @click="tool = item.id">{{ item.name }}</button></view>
        <picker :range="brandPaintColors" range-key="label" :value="brandBrushIndex" :disabled="locked" @change="chooseBrandColor"><view class="picker-value add-color">从 {{ patternBrand.name }} 添加色号 <text>＋</text></view></picker>
        <scroll-view class="paint-palette" scroll-x><view class="paint-colors"><button v-for="color in paintColors" :key="color.index" class="paint-color" :class="{selected: !pendingBrushColor && selectedColor === color.index}" :disabled="locked" @click="selectExistingColor(color.index)"><view class="swatch" :style="{backgroundColor: color.hex}"/><text>{{ color.code }}</text></button></view></scroll-view>
        <view class="selected-description"><text class="hint">当前画笔：{{ selectedColorName }}</text><text class="hint">{{ editNotice }}</text></view>
      </view>
      <button class="button primary save-main" :disabled="locked || (!!sourcePath && settingsChanged)" :loading="exporting" @click="exportPattern">{{ exporting ? exportProgress : saveLabel }}</button>
      <view class="draft-status"><text class="hint">{{ savingDraft ? '正在自动保存…' : draftNotice }}</text><button v-if="draftFailed" class="button text-button" :disabled="savingDraft || locked" @click="saveDraft">重试保存</button></view>
      <text v-if="saveNotice" class="hint block">{{ saveNotice }}</text>
      <view class="materials-section"><button class="disclosure" @click="showMaterials = !showMaterials"><text>用豆清单 · {{ materials.length }} 色</text><text>{{ showMaterials ? '收起' : '展开' }}</text></button><view v-if="showMaterials"><view v-for="color in materials" :key="color.code" class="material-row"><view class="swatch" :style="{backgroundColor: color.hex}"/><view class="material-name"><text>{{ color.code }} · {{ color.name }}</text><text class="hint">{{ color.count }} 颗</text></view><button v-if="showMissing && sourcePath" class="button text-button" :disabled="locked" @click="toggleExcluded(color.code)">{{ excludedCodes.includes(color.code) ? '恢复' : '缺少此色' }}</button></view><button v-if="sourcePath" class="button text-button" @click="showMissing = !showMissing">{{ showMissing ? '完成' : '缺少某个颜色？' }}</button></view></view>
    </view>
    <view class="more-section"><button class="disclosure" @click="showMore = !showMore"><text>更多设置</text><text>{{ showMore ? '收起' : '展开' }}</text></button><view v-if="showMore">
      <text class="hint block">已按图片匹配参数，也可以在这里调整。修改后会自动更新。</text>
      <view class="setting-line"><text class="label">图纸宽度</text><view class="choices"><button v-for="size in sizes" :key="size" class="chip" :class="{active: columns === size}" :disabled="locked || (!!pattern && !sourcePath)" @click="columns = size">{{ size }}</button></view></view>
      <view class="setting-line"><text class="label">最多颜色</text><view class="choices"><button v-for="limit in limits" :key="limit" class="chip" :class="{active: maxColors === limit}" :disabled="locked || (!!pattern && !sourcePath)" @click="maxColors = limit">{{ limit }} 色</button></view></view>
      <view class="setting-line"><text class="label">杂点清理</text><view class="choices"><button v-for="item in cleanupLevels" :key="item.id" class="chip" :class="{active: cleanup === item.id}" :disabled="locked || (!!pattern && !sourcePath)" @click="cleanup = item.id">{{ item.name }}</button></view></view>
      <view v-if="excludedCodes.length" class="excluded"><text class="hint">已排除这些颜色，轻点可恢复</text><view class="excluded-list"><button v-for="code in excludedCodes" :key="code" class="chip" :disabled="locked" @click="toggleExcluded(code)">{{ code }} ×</button></view></view>
      <view v-if="sourcePath" class="archive-details"><text class="label">原图备份</text><text class="hint block">{{ archiveLabel }}</text><text v-if="archiveError" class="error">{{ archiveError }}</text><button v-if="!sourceIsPreview && archiveStatus !== 'uploading' && archiveStatus !== 'success'" class="button text-button" @click="archiveOriginal">{{ authenticated ? '重试备份' : '登录并备份' }}</button><text class="hint block">仅备份原图，上限5MB；不影响本机制作。</text></view>
    </view></view>
    <text class="credit">色号与屏幕颜色仅供参考，请核对实物色卡。{{ pattern ? patternBrand.credit : selectedBrand.credit }}</text>
    <canvas id="bead-source" type="2d" canvas-id="bead-source" class="offscreen" /><canvas id="bead-export" type="2d" canvas-id="bead-export" class="offscreen" />
    <OneClickLogin v-if="showLogin" :visible="showLogin" @close="showLogin = false" @login-success="loginSuccess" />
  </view>
</template>

<script setup>
import { ref, shallowRef, computed, nextTick, getCurrentInstance, watch } from 'vue';
import { onReady, onUnload } from '@dcloudio/uni-app';
import OneClickLogin from '@/components/OneClickLogin.vue';
import { uploadBeadOriginal } from '@/api/beads.js';
import { convertToPattern, countMaterials, paintCell, replaceColor } from '@/utils/beads/engine.mjs';
import { assessImage } from '@/utils/beads/assessment.mjs';
import { BEAD_PALETTES } from '@/utils/beads/palettes.mjs';
import { getCanvasSurface, decodePhoto, drawPattern, exportTiles, drawExport, surfaceFile, saveExport } from '@/utils/beads/canvas.js';

const scope = getCurrentInstance().proxy;
const DRAFT_KEY = 'beads-draft-v1';
const sizes = [32, 48, 64, 96, 128], limits = [8, 16, 24, 32];
const brands = BEAD_PALETTES;
const cleanupLevels = [{id:0,name:'关闭'}, {id:1,name:'轻度'}, {id:2,name:'标准'}];
const backgrounds = [{id:'keep',name:'保留背景'}, {id:'remove',name:'去除背景'}];
const mode = ref('illustration'), cleanup = ref(1), background = ref('keep');
const tools = [{id:'paint',name:'点格换色'}, {id:'erase',name:'擦除'}, {id:'replace',name:'整色替换'}];
const patternBrandId = ref(brands[0].id);
const columns = ref(64), maxColors = ref(16), brandIndex = ref(0), excludedCodes = ref([]);
const sourceIsPreview = ref(false);
const sourcePath = ref(''), pattern = shallowRef(null), history = shallowRef([]), zoom = ref(1);
const savingDraft = ref(false);
const busy = ref(false), exporting = ref(false), error = ref(''), archiveError = ref('');
const archiveNeedsLogin = ref(false);
const archiveStatus = ref('idle'), authenticated = ref(!!uni.getStorageSync('accessToken')), showLogin = ref(false);
const pendingBrushColor = shallowRef(null), generatedSettings = ref('');
const selectedColor = ref(-1), tool = ref('paint'), editNotice = ref(''), saveNotice = ref(''), exportProgress = ref('正在保存…'), hasDraft = ref(false);
let alive = true, generation = 0, rgba = null, sourceSurface = null, exportSurface = null, startTouch = null;
let uploadPromise = null, persistentPath = '', pendingRender = 0, lastTouchEdit = 0;
const editing = ref(false), hasManualEdits = ref(false), showMore = ref(false), showMaterials = ref(false), showMissing = ref(false);
const assessmentReason = ref(''), phase = ref('正在制作图纸…'), confirming = ref(false);
const draftNotice = ref(''), draftFailed = ref(false), saveLabel = ref('保存到相册');
// #ifdef H5
saveLabel.value = '下载图纸';
// #endif
const locked = computed(() => busy.value || exporting.value || confirming.value);
let convertTimer = null, draftTimer = null, restoring = false, draftQueued = false, sourceCopyPromise = null;
const BRAND_KEY = 'beads-brand-v1';
try { const remembered = uni.getStorageSync(BRAND_KEY); const index = brands.findIndex(b => b.id === remembered); if (index >= 0) brandIndex.value = index; } catch (_) {}
function rememberBrand() { try { uni.setStorageSync(BRAND_KEY, selectedBrand.value.id); } catch (_) {} }
function scheduleDraft(delay = 320) {
  clearTimeout(draftTimer);
  if (!alive || !pattern.value) return;
  draftTimer = setTimeout(() => { draftTimer = null; saveDraft(); }, delay);
}
function scheduleConvert() {
  clearTimeout(convertTimer);
  if (!alive || !sourcePath.value || busy.value || restoring || confirming.value) return;
  convertTimer = setTimeout(() => { convertTimer = null; requestConvert(); }, 280);
}
watch(pattern, () => { if (!restoring) scheduleDraft(); });
function adjustDetail(direction) {
  const steps = [32, 64, 96];
  const next = direction < 0 ? [...steps].reverse().find(size => size < columns.value) : steps.find(size => size > columns.value);
  if (!next) return;
  columns.value = next; maxColors.value = next >= 96 ? 24 : 16;
  assessmentReason.value = direction < 0 ? '已调整为更省豆的尺寸' : '已调整为更清楚的尺寸';
}

const patternBrand = computed(() => brands.find(brand => brand.id === patternBrandId.value) || brands[0]);
const selectedBrand = computed(() => brands[brandIndex.value] || brands[0]);
const materials = computed(() => pattern.value ? countMaterials(pattern.value) : []);
const totalBeads = computed(() => materials.value.reduce((sum, color) => sum + color.count, 0));
const paintColors = computed(() => pattern.value ? pattern.value.palette.map((color, index) => ({...color,index})) : []);
const selectedColorName = computed(() => pendingBrushColor.value?.code || pattern.value?.palette[selectedColor.value]?.code || '请选择颜色');
const brandPaintColors = computed(() => [{code:'',label:'请选择要添加的色号'}, ...patternBrand.value.colors.map(color => ({...color,label: `${color.code} · ${color.name}`}))]);
const brandBrushIndex = computed(() => Math.max(0,brandPaintColors.value.findIndex(color => color.code === selectedColorName.value)));
function settingsKey() { return JSON.stringify({columns:columns.value,maxColors:maxColors.value,brandId:selectedBrand.value.id,excludedCodes:[...excludedCodes.value].sort(),mode:mode.value,cleanup:cleanup.value,background:background.value}); }
function restoreGeneratedSettings(value) {
  if (typeof value !== 'string') return settingsKey();
  try {
    const saved = JSON.parse(value);
    return JSON.stringify({columns:saved.columns,maxColors:saved.maxColors,brandId:saved.brandId,excludedCodes:[...saved.excludedCodes].sort(),mode:saved.mode === 'illustration' ? 'illustration' : 'photo',cleanup:[0,1,2].includes(saved.cleanup) ? saved.cleanup : 0,background:saved.background === 'remove' ? 'remove' : 'keep'});
  } catch (_) { return settingsKey(); }
}
const settingsChanged = computed(() => !!pattern.value && generatedSettings.value !== settingsKey());
watch(() => settingsKey(), () => {
  if (restoring || busy.value) return;
  if (pattern.value && !settingsChanged.value) { clearTimeout(convertTimer); convertTimer = null; return; }
  scheduleConvert();
});

function selectExistingColor(index) { selectedColor.value = index; pendingBrushColor.value = null; }
function chooseBrandColor(event) {
  const color = patternBrand.value.colors[Number(event.detail.value) - 1]; if (!color || !pattern.value) return;
  const index = pattern.value.palette.findIndex(item => item.code === color.code && item.hex === color.hex);
  if (index >= 0) selectExistingColor(index);
  else { pendingBrushColor.value = color; selectedColor.value = -1; }
}
const screenWidth = Math.max(280, uni.getSystemInfoSync().windowWidth - 40);
const cellSize = computed(() => pattern.value ? Math.min(2048 / Math.max(pattern.value.width, pattern.value.height), Math.max(3, Math.min(10, screenWidth / pattern.value.width)) * zoom.value) : 5);
const canvasWidth = computed(() => pattern.value ? pattern.value.width * cellSize.value : 1);
const canvasHeight = computed(() => pattern.value ? pattern.value.height * cellSize.value : 1);
const viewportHeight = computed(() => Math.min(420, Math.max(180, canvasHeight.value)));
const archiveLabel = computed(() => sourceIsPreview.value ? '草稿保留参考图；归档原图请重新选图。' : ({idle:'原图未归档，图纸仍可正常制作',guest:'登录后可归档原图，本地制作不受影响',uploading:'正在归档原图…',success:'原图已归档',failed:'原图归档失败，本地制作不受影响'}[archiveStatus.value]));
const toolHint = computed(() => ({paint:'先选下方色号，再轻点图纸中的一格。',erase:'轻点一格将它留空；透明格不计入材料。',replace:'先选目标色号，再轻点图纸，将同色豆子一起替换。'}[tool.value]));
function message(title) { uni.showToast({title,icon:'none'}); }
function current(ticket) { return alive && ticket === generation; }
function changeBrand(event) { if (pattern.value && !sourcePath.value) return; brandIndex.value = Number(event.detail.value); excludedCodes.value = []; rememberBrand(); }
function previewOriginal() { if (sourcePath.value) uni.previewImage({urls:[sourcePath.value],current:sourcePath.value}); }
function choosePhoto() {
  uni.chooseImage({count:1,sizeType:['original'],sourceType:['album'], success: result => {
    const path = result.tempFilePaths?.[0]; if (path) loadPhoto(path);
  },fail: result => { if (!String(result.errMsg).includes('cancel')) message('未能打开相册，请重试'); }});
}
async function loadPhoto(path) {
  if (!alive) return;
  clearTimeout(convertTimer); clearTimeout(draftTimer);
  if (pattern.value) flushDraft();
  const ticket = ++generation;
  sourcePath.value = path; sourceIsPreview.value = false; persistentPath = ''; sourceCopyPromise = null; rgba = null;
  pattern.value = null; history.value = []; hasManualEdits.value = false; editing.value = false; excludedCodes.value = [];
  error.value = ''; archiveError.value = ''; archiveStatus.value = 'idle'; saveNotice.value = ''; draftNotice.value = ''; assessmentReason.value = ''; zoom.value = 1;
  archiveOriginal(false);
  await convert(ticket, true);
}
async function archiveOriginal(promptLogin = true) {
  if (!sourcePath.value || sourceIsPreview.value || archiveStatus.value === 'uploading') return;
  authenticated.value = !archiveNeedsLogin.value && !!uni.getStorageSync('accessToken');
  if (!authenticated.value) { archiveStatus.value = 'guest'; if (promptLogin) showLogin.value = true; return; }
  const ticket = generation, path = sourcePath.value;
  archiveStatus.value = 'uploading'; archiveError.value = '';
  const work = uploadBeadOriginal(path); uploadPromise = work;
  try {
    const result = await work;
    if (!current(ticket)) return;
    if (!result?.success) throw new Error(result?.error || '请检查网络后重试');
    archiveStatus.value = 'success';
  } catch (failure) {
    if (current(ticket)) { archiveStatus.value = 'failed'; archiveError.value = failure.message || '请检查网络后重试'; if (/登录已过期|重新登录/.test(archiveError.value)) { archiveNeedsLogin.value = true; authenticated.value = false; } }
  } finally { if (uploadPromise === work) uploadPromise = null; }
}
function loginSuccess() { showLogin.value = false; archiveNeedsLogin.value = false; authenticated.value = true; archiveOriginal(false); }
async function requestConvert() {
  if (!sourcePath.value || locked.value || (pattern.value && !settingsChanged.value && !error.value)) return;
  const ticket = generation;
  if (hasManualEdits.value || history.value.length) {
    confirming.value = true;
    try {
      const result = await new Promise(resolve => uni.showModal({title:'更新图纸？',content:'更新会替换当前手动修改。取消可继续保留这份图纸。',confirmText:'更新图纸',cancelText:'取消',success:resolve,fail:()=>resolve({confirm:false})}));
      if (!current(ticket)) return;
      if (!result.confirm) {
        restoring = true;
        try { applySettings(JSON.parse(generatedSettings.value)); rememberBrand(); await nextTick(); } finally { restoring = false; }
        return;
      }
    } finally { if (current(ticket)) confirming.value = false; }
  }
  await convert(ticket);
}
function applySettings(settings) {
  columns.value = sizes.includes(settings.columns) ? settings.columns : 64;
  maxColors.value = limits.includes(settings.maxColors) ? settings.maxColors : 16;
  if (settings.brandId) brandIndex.value = Math.max(0, brands.findIndex(b => b.id === settings.brandId));
  mode.value = settings.mode === 'photo' ? 'photo' : 'illustration';
  cleanup.value = [0,1,2].includes(settings.cleanup) ? settings.cleanup : 1;
  background.value = settings.background === 'remove' ? 'remove' : 'keep';
  if (Array.isArray(settings.excludedCodes)) excludedCodes.value = [...settings.excludedCodes];
}
async function convert(ticket, evaluate = false) {
  if (!sourcePath.value || busy.value || exporting.value) return;
  busy.value = true; error.value = ''; phase.value = evaluate ? '正在评估图片…' : '正在更新图纸…';
  try {
    await nextTick(); await new Promise(resolve => setTimeout(resolve,20));
    if (!current(ticket)) return;
    if (!sourceSurface) sourceSurface = await getCanvasSurface('bead-source',scope);
    if (!rgba) rgba = await decodePhoto(sourceSurface,sourcePath.value,()=>current(ticket));
    if (!current(ticket) || !rgba) return;
    if (evaluate) { const assessment = assessImage(rgba); applySettings(assessment); assessmentReason.value = assessment.reason; }
    mode.value = 'illustration'; phase.value = '正在制作图纸…';
    const settings = {columns:columns.value,maxColors:maxColors.value,palette:selectedBrand.value.colors,excludedCodes:[...excludedCodes.value],mode:mode.value,cleanup:cleanup.value,background:background.value};
    const result = convertToPattern(rgba,settings);
    if (!current(ticket)) return;
    pattern.value = result; patternBrandId.value = selectedBrand.value.id; generatedSettings.value = settingsKey(); history.value = []; hasManualEdits.value = false; editing.value = false;
    selectedColor.value = materials.value[0]?.index ?? -1; pendingBrushColor.value = null; editNotice.value = ''; saveNotice.value = ''; zoom.value = 1; rememberBrand(); scheduleDraft();
  } catch (failure) { if (current(ticket)) error.value = failure.message || '制作失败，请重试或换张图片'; }
  finally { if (current(ticket)) busy.value = false; }
}
async function render() {
  const renderTicket = ++pendingRender;
  await nextTick(); if (!pattern.value || !alive || renderTicket !== pendingRender) return;
  try { const surface = await getCanvasSurface('bead-preview', scope); if (alive && renderTicket === pendingRender && pattern.value) drawPattern(surface, pattern.value, cellSize.value); }
  catch (failure) { if (alive) error.value = failure.message; }
}
watch([pattern, zoom], render);
function touchStart(event) { const point = event.touches?.[0]; startTouch = point ? {x:point.clientX,y:point.clientY} : null; }
async function touchEnd(event) {
  if (busy.value || exporting.value || savingDraft.value || !pattern.value) return;
  const point = event.changedTouches?.[0], start = startTouch; startTouch = null;
  if (!point || !start || Math.hypot(point.clientX - start.x, point.clientY - start.y) > 8) return;
  lastTouchEdit = Date.now();
  await paintAt(point);
}
async function clickCanvas(event) {
  // #ifdef H5
  if (Date.now() - lastTouchEdit < 600) return;
  const point = {clientX:event.clientX ?? event.detail?.x,clientY:event.clientY ?? event.detail?.y};
  if (Number.isFinite(point.clientX) && Number.isFinite(point.clientY)) await paintAt(point);
  // #endif
}
async function paintAt(point) {
  if (!editing.value || !pattern.value || locked.value || settingsChanged.value) return;
  const rect = await new Promise(resolve => uni.createSelectorQuery().in(scope).select('#bead-preview').boundingClientRect(resolve).exec());
  if (!rect || !pattern.value || !alive) return;
  const x = Math.floor((point.clientX - rect.left) / rect.width * pattern.value.width);
  const y = Math.floor((point.clientY - rect.top) / rect.height * pattern.value.height);
  if (x < 0 || y < 0 || x >= pattern.value.width || y >= pattern.value.height) return;
  const before = pattern.value, from = before.cells[y * before.width + x];
  let working = before, to = tool.value === 'erase' ? -1 : selectedColor.value;
  if (tool.value !== 'erase' && pendingBrushColor.value) {
    const color = pendingBrushColor.value;
    to = before.palette.findIndex(item => item.code === color.code && item.hex === color.hex);
    if (to < 0) { to = before.palette.length; working = {...before,palette:[...before.palette,{...color,rgb:[...color.rgb]}]}; }
  }
  if (tool.value !== 'erase' && (to < 0 || !working.palette[to])) { editNotice.value = '请先从品牌色板选择一个画笔色号。'; return; }
  if (from === to || (tool.value === 'replace' && from < 0)) return;
  hasManualEdits.value = true;
  history.value = [...history.value.slice(-19), before];
  pattern.value = tool.value === 'replace' ? replaceColor(working, from, to) : paintCell(working, x, y, to);
  if (tool.value !== 'erase') selectExistingColor(to);
  editNotice.value = tool.value === 'replace' ? '同色豆子已一起替换' : `已修改第 ${y + 1} 行、第 ${x + 1} 列`;
  saveNotice.value = '';
}
function undo() { if (!history.value.length) return; pattern.value = history.value[history.value.length - 1]; history.value = history.value.slice(0,-1); if (!pattern.value.palette[selectedColor.value]) selectedColor.value = materials.value[0]?.index ?? -1; pendingBrushColor.value = null; editNotice.value = '已撤销上一步'; saveNotice.value = ''; }
function toggleExcluded(code) { if (pattern.value && !sourcePath.value) return; if (selectedBrand.value.id !== patternBrandId.value) { brandIndex.value = Math.max(0,brands.findIndex(brand => brand.id === patternBrandId.value)); excludedCodes.value = []; } excludedCodes.value = excludedCodes.value.includes(code) ? excludedCodes.value.filter(item => item !== code) : [...excludedCodes.value,code]; }
function draftSnapshot() {
  let settings;
  try { settings = JSON.parse(generatedSettings.value); } catch (_) { settings = JSON.parse(settingsKey()); }
  return {version:1,pattern:pattern.value,mode:settings.mode,cleanup:settings.cleanup,background:settings.background,columns:settings.columns,maxColors:settings.maxColors,brandId:settings.brandId,patternBrandId:patternBrandId.value,generatedSettings:generatedSettings.value,excludedCodes:[...(settings.excludedCodes || [])],edited:hasManualEdits.value,assessmentReason:assessmentReason.value,updatedAt:Date.now()};
}
function discardOwnedSource(path) {
  // #ifndef H5
  try {
    const root = uni.env?.USER_DATA_PATH || (typeof wx !== 'undefined' && wx.env?.USER_DATA_PATH);
    if (root && typeof path === 'string' && path.startsWith(`${root}/beads-original-`)) uni.getFileSystemManager().unlink({filePath:path,fail:()=>{}});
  } catch (_) {}
  // #endif
}
function writeDraft(snapshot, source) {
  let previous;
  try { previous = uni.getStorageSync(DRAFT_KEY)?.source; } catch (_) {}
  try {
    try { uni.setStorageSync(DRAFT_KEY,{...snapshot,source}); }
    catch (_) { uni.setStorageSync(DRAFT_KEY,{...snapshot,source:''}); source = ''; }
    if (previous && previous !== source) discardOwnedSource(previous);
    hasDraft.value = true; draftFailed.value = false;
    draftNotice.value = source ? '已自动保存在本机' : '图纸已自动保存；下次重新生成需重新选图。';
    return true;
  } catch (_) {
    draftFailed.value = true; draftNotice.value = '自动保存失败，请重试或先保存图纸。'; return false;
  }
}
function cachedSource() {
  // #ifdef H5
  if (rgba && sourceSurface?.canvas?.toDataURL) return sourceSurface.canvas.toDataURL('image/png');
  // #endif
  return persistentPath || (sourceIsPreview.value ? sourcePath.value : '');
}
async function persistSource(path, ticket) {
  if (persistentPath) return persistentPath;
  if (sourceCopyPromise) return sourceCopyPromise;
  // #ifndef H5
  const root = uni.env?.USER_DATA_PATH || (typeof wx !== 'undefined' && wx.env?.USER_DATA_PATH);
  if (!root || !uni.getFileSystemManager) throw new Error('原图无法保存在本机');
  const extension = /\.(png|jpe?g|webp)$/i.exec(path)?.[1] || 'jpg';
  const target = `${root}/beads-original-${Date.now()}-${ticket}.${extension}`;
  const work = new Promise((resolve,reject) => uni.getFileSystemManager().copyFile({srcPath:path,destPath:target,success:()=>resolve(target),fail:reject}));
  sourceCopyPromise = work;
  try {
    const result = await work;
    if (!current(ticket)) { discardOwnedSource(result); return ''; }
    persistentPath = result; return result;
  } finally { if (sourceCopyPromise === work) sourceCopyPromise = null; }
  // #endif
  return '';
}
function flushDraft() {
  if (!pattern.value) return;
  try { writeDraft(draftSnapshot(),cachedSource()); }
  catch (_) { writeDraft(draftSnapshot(),''); }
}
async function saveDraft() {
  if (!pattern.value || !alive) return;
  if (savingDraft.value) { draftQueued = true; return; }
  savingDraft.value = true; draftQueued = false;
  const ticket = generation, snapshot = draftSnapshot(), path = sourcePath.value, capturedSettings = settingsKey();
  try {
    let source = '';
    try { source = cachedSource(); } catch (_) {}
    if (!writeDraft(snapshot,source)) return;
    // Save cells synchronously first. Copying the original never waits for its network archive.
    if (!source && path && !sourceIsPreview.value) {
      try { source = await persistSource(path,ticket); } catch (_) { source = ''; }
      if (!current(ticket)) return;
      if (snapshot.pattern === pattern.value && snapshot.generatedSettings === generatedSettings.value && capturedSettings === settingsKey()) {
        if (source) writeDraft(snapshot,source);
      } else draftQueued = true;
    }
  } finally {
    if (alive) { savingDraft.value = false; if (draftQueued) scheduleDraft(0); }
  }
}
async function restoreDraft() {
  clearTimeout(convertTimer); clearTimeout(draftTimer); restoring = true;
  try {
    const draft = uni.getStorageSync(DRAFT_KEY), saved = draft?.pattern;
    if (draft?.version !== 1 || !saved || !Number.isInteger(saved.width) || !Number.isInteger(saved.height) || saved.width < 1 || saved.height < 1 || saved.width > 128 || saved.height > 128 || !Array.isArray(saved.cells) || saved.cells.length !== saved.width * saved.height || !Array.isArray(saved.palette) || saved.palette.length > 512 || saved.palette.some(color => !/^#[0-9a-f]{6}$/i.test(color.hex) || typeof color.code !== 'string') || saved.cells.some(index => !Number.isInteger(index) || index < -1 || index >= saved.palette.length)) throw new Error('这份草稿无法读取，请重新选图');
    const ticket = ++generation; rgba = null; editing.value = false; hasManualEdits.value = draft.edited !== false; assessmentReason.value = draft.assessmentReason || ''; sourceCopyPromise = null; patternBrandId.value = draft.patternBrandId || draft.brandId || brands[0].id; pattern.value = saved; history.value = []; zoom.value = 1;
    columns.value = sizes.includes(draft.columns) ? draft.columns : 48; maxColors.value = limits.includes(draft.maxColors) ? draft.maxColors : 16;
    brandIndex.value = Math.max(0,brands.findIndex(brand => brand.id === draft.brandId)); excludedCodes.value = Array.isArray(draft.excludedCodes) ? draft.excludedCodes : [];
    mode.value = draft.mode === 'illustration' ? 'illustration' : 'photo'; cleanup.value = [0,1,2].includes(draft.cleanup) ? draft.cleanup : 0; background.value = draft.background === 'remove' ? 'remove' : 'keep';
    generatedSettings.value = restoreGeneratedSettings(draft.generatedSettings);
    sourcePath.value = draft.source || ''; sourceIsPreview.value = sourcePath.value.startsWith('data:'); persistentPath = sourcePath.value; archiveStatus.value = 'idle';
    // Recover the settings that produced these cells, not legacy options pending a redraw.
    try { applySettings(JSON.parse(generatedSettings.value)); } catch (_) {}
    selectedColor.value = materials.value[0]?.index ?? -1; pendingBrushColor.value = null;
    saveNotice.value = '已恢复本机草稿。';
    if (sourcePath.value) {
      try { await new Promise((resolve,reject) => uni.getImageInfo({src:sourcePath.value,success:resolve,fail:reject})); if (!current(ticket)) return; }
      catch (_) { if (!current(ticket)) return; sourcePath.value = ''; saveNotice.value = '图纸已恢复；原图已失效，仍可编辑和导出。重新选图可制作新图纸。'; }
    }
  } catch (failure) { message(failure.message || '草稿无法读取'); }
  finally { await nextTick(); restoring = false; }
}
async function exportPattern() {
  if (!pattern.value || exporting.value) return;
  exporting.value = true; saveNotice.value = ''; const snapshot = pattern.value;
  const list = countMaterials(snapshot), tiles = exportTiles(snapshot), ticket = generation;
  let completed = 0;
  try {
    if (!exportSurface) exportSurface = await getCanvasSurface('bead-export',scope);
    for (let index = 0; index < tiles.length; index++) {
      if (!current(ticket)) return;
      exportProgress.value = `保存 ${index + 1}/${tiles.length}`;
      drawExport(exportSurface,snapshot,list,tiles[index],index + 1,tiles.length,patternBrand.value.name,patternBrand.value.credit);
      const path = await surfaceFile(exportSurface,scope);
      if (!current(ticket)) return;
      await saveExport(path,`拼豆图纸-${index + 1}.png`); completed++;
    }
    // #ifdef H5
    saveNotice.value = `已下载 ${completed} 张图纸。若浏览器拦截多图下载，请允许此网站下载多个文件。`;
    // #endif
    // #ifndef H5
    saveNotice.value = `已保存 ${completed} 张图纸到相册，每张均含行列坐标、色号和完整材料表。`;
    // #endif
  } catch (failure) {
    if (!alive) return;
    const detail = String(failure.errMsg || failure.message || '');
    if (/auth|deny|permission/i.test(detail)) {
      saveNotice.value = `已保存 ${completed} 张。请允许访问相册后重试。`;
      uni.showModal({title:'需要相册权限',content:'请在设置中允许保存到相册，再回来保存图纸。',confirmText:'打开设置',success: result => { if (result.confirm) uni.openSetting({}); }});
    } else saveNotice.value = `已保存 ${completed} 张，剩余图纸保存失败，请重试。`;
  } finally { if (alive) exporting.value = false; }
}
onReady(() => { try { hasDraft.value = !!uni.getStorageSync(DRAFT_KEY); } catch (_) { hasDraft.value = false; } });
onUnload(() => { clearTimeout(convertTimer); clearTimeout(draftTimer); if (pattern.value) flushDraft(); alive = false; generation++; pendingRender++; rgba = null; sourceSurface = null; exportSurface = null; });
</script>

<style scoped>
.beads-page{min-height:100vh;background:#fcfbf8;color:#272117;padding:32rpx 32rpx calc(60rpx + env(safe-area-inset-bottom));font-family:-apple-system,BlinkMacSystemFont,'Noto Sans',sans-serif;box-sizing:border-box}.intro{padding:8rpx 0 24rpx}.eyebrow{display:block;font-size:24rpx;color:#92723a;letter-spacing:2rpx}.title{display:block;margin:16rpx 0;font-size:54rpx;font-weight:750;letter-spacing:-2rpx}.subtitle{font-size:27rpx;color:#746b5b;line-height:1.7}.workspace{background:#fff;border:1px solid #eee8db;border-radius:24rpx;padding:28rpx}.photo-row{display:flex;gap:24rpx;align-items:center}.photo,.photo-placeholder{width:156rpx;height:170rpx;background:#f8f3e8;border-radius:16rpx;flex-shrink:0}.photo-placeholder{display:flex;align-items:center;justify-content:center;color:#d0ad65;font-size:72rpx}.photo-copy{flex:1;min-width:0;display:flex;flex-direction:column;gap:10rpx}.section-title{font-size:32rpx;font-weight:650;line-height:1.5}.hint{font-size:24rpx;color:#796f60;line-height:1.65}.block{display:block}.button{margin:0;padding:0 24rpx;min-height:88rpx;line-height:1.4;display:flex;align-items:center;justify-content:center;box-sizing:border-box;border-radius:16rpx;font-size:27rpx;font-weight:600;white-space:normal}.button::after,.chip::after,.paint-color::after{border:0}.primary{background:#f7bd4a;color:#31230a}.secondary{background:#f5f1e9;color:#514430;border:1px solid #e5dccb}.button[disabled],.chip[disabled],.paint-color[disabled]{opacity:.45}.choose{align-self:flex-start;margin-top:4rpx}.archive-row,.draft-row{display:flex;align-items:center;gap:12rpx;margin-top:22rpx}.archive-text{flex:1}.text-button{background:transparent;color:#8b641d;font-size:24rpx;padding:0 12rpx;flex-shrink:0}.settings{margin-top:26rpx;border-top:1px solid #f0eadd;padding-top:12rpx}.setting-line{display:flex;align-items:center;justify-content:space-between;gap:12rpx;padding:16rpx 0;flex-wrap:wrap}.label{font-size:26rpx;color:#514632}.choices{display:flex;gap:6rpx;flex-wrap:wrap}.chip{min-width:70rpx;min-height:88rpx;line-height:1.4;padding:12rpx;font-size:24rpx;display:flex;align-items:center;justify-content:center;background:#f6f3ed;color:#5b5140;border-radius:12rpx;margin:0;box-sizing:border-box}.chip.active,.tool.active{background:#fff0c8;color:#6c4a0e;box-shadow:inset 0 0 0 1px #e8bb5c}.picker-value{min-height:88rpx;min-width:220rpx;display:flex;align-items:center;justify-content:space-between;gap:32rpx;font-size:26rpx;background:#f6f3ed;padding:0 24rpx;border-radius:12rpx}.generate{width:100%;margin-top:24rpx}.error-panel{padding:20rpx;background:#fff3ed;margin-top:20rpx;border-radius:12rpx;display:flex;flex-direction:column;gap:20rpx;font-size:26rpx;color:#9a4127}.error{display:block;color:#9a4127;font-size:24rpx;line-height:1.6}.pattern-section,.materials-section{padding-top:40rpx}.section-heading{display:flex;align-items:center;justify-content:space-between;gap:10rpx}.zoom-row{display:flex;justify-content:space-between;align-items:center;padding:12rpx 0}.zoom-actions{display:flex;gap:12rpx}.square{min-width:88rpx;padding:0;font-size:34rpx}.canvas-scroll{width:100%;background:#eee9df;border:1px solid #dfd7c7;border-radius:12rpx;box-sizing:border-box}.canvas-wrap{position:relative}.pattern-canvas{display:block}.tools{display:flex;gap:12rpx;margin:20rpx 0 12rpx}.tool{flex:1;background:#eee9df;color:#675c48;padding:0 8rpx;font-size:24rpx}.paint-palette{width:100%;margin-top:16rpx;white-space:nowrap}.paint-colors{display:flex;gap:10rpx;width:max-content;padding:8rpx 4rpx 12rpx}.paint-color{display:flex;flex-direction:column;gap:8rpx;align-items:center;justify-content:center;min-width:100rpx;min-height:116rpx;padding:10rpx;border-radius:12rpx;background:#fff;border:1px solid #e6dfd2;margin:0;font-size:22rpx;color:#514632;line-height:1.4}.paint-color.selected{border:2px solid #9d6e16;background:#fff4d8}.swatch{display:block;width:44rpx;height:44rpx;border-radius:10rpx;border:1px solid rgba(0,0,0,.14);flex-shrink:0}.selected-description{display:flex;flex-direction:column;min-height:70rpx;margin-top:8rpx}.save-actions{display:flex;gap:16rpx;margin:24rpx 0 14rpx}.save-actions .button{flex:1}.material-row{display:flex;align-items:center;gap:18rpx;border-bottom:1px solid #eee7da;padding:14rpx 0}.material-name{display:flex;flex:1;min-width:0;flex-direction:column;gap:6rpx;font-size:26rpx}.excluded{padding:20rpx 0}.excluded-list{display:flex;gap:10rpx;flex-wrap:wrap;padding:12rpx 0 20rpx}.add-color{margin-top:16rpx;font-size:24rpx}.settings-notice{display:block;margin-top:16rpx;padding:16rpx;background:#fff3d4;border-radius:12rpx;color:#825b12;font-size:24rpx;line-height:1.6}.credit{display:block;margin-top:24rpx;font-size:21rpx;color:#8c8170;line-height:1.7;word-break:break-word}.empty-note{display:flex;flex-direction:column;gap:16rpx;align-items:center;text-align:center;color:#88765b;font-size:25rpx;padding:60rpx 8rpx}.empty-symbol{color:#dfb355;font-size:38rpx;letter-spacing:12rpx}.offscreen{position:fixed;left:-10000px;top:-10000px;width:1px;height:1px;pointer-events:none}@media (min-width:700px){.beads-page{max-width:960px;margin:auto;padding:32px}.title{font-size:36px}.choices{gap:8px}.canvas-scroll{max-width:100%}}
.background-row{display:flex;justify-content:space-between;align-items:center;gap:16rpx;padding-top:12rpx}.recommendation{padding:12rpx 0}.detail-actions{display:flex;gap:16rpx;margin:10rpx 0 20rpx}.detail-actions .button{flex:1;min-height:76rpx}.save-main{width:100%;margin-top:22rpx}.draft-status{display:flex;align-items:center;justify-content:space-between;min-height:52rpx}.preview-hint{margin-top:12rpx}.disclosure{display:flex;justify-content:space-between;align-items:center;width:100%;min-height:88rpx;background:transparent;color:#514430;font-size:27rpx;font-weight:600;margin:0;padding:16rpx 0;border-radius:0}.disclosure::after{border:0}.more-section{margin-top:18rpx;border-top:1px solid #e8e0d2}.materials-section{padding-top:16rpx;border-top:1px solid #e8e0d2;margin-top:16rpx}.progress-note{padding:24rpx 0;color:#514430;font-size:28rpx}.archive-details{border-top:1px solid #eee7da;padding-top:20rpx;margin-top:12rpx}.beads-page .hint{color:#695f50}.beads-page .credit{color:#746958}
</style>
