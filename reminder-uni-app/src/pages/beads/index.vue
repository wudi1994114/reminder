<template>
  <view class="beads-page">
    <view class="intro">
      <text class="eyebrow">把喜欢，拼成一颗颗小豆子</text>
      <text class="title">我的拼豆工坊</text>
      <text class="subtitle">选一张照片，做一份属于你的拼豆图纸。</text>
    </view>

    <view class="workspace">
      <view class="photo-row">
        <image v-if="sourcePath" class="photo" :src="sourcePath" mode="aspectFit" @click="previewOriginal" />
        <view v-else class="photo-placeholder"><text>▦</text></view>
        <view class="photo-copy">
          <text class="section-title">{{ sourcePath ? (sourceIsPreview ? '草稿参考图' : '原图已选好') : pattern ? '已恢复本地图纸' : '从一张图片开始' }}</text>
          <text class="hint">{{ sourcePath ? (sourceIsPreview ? '轻点查看草稿参考图' : '轻点缩略图查看原图') : '人物、宠物或简单插画都可以' }}</text>
          <button class="button primary choose" :disabled="busy || exporting || savingDraft" @click="choosePhoto">{{ sourcePath || pattern ? '重新选图' : '从相册选图' }}</button>
        </view>
      </view>
      <view v-if="sourcePath" class="archive-row">
        <text class="hint archive-text">{{ archiveLabel }}</text>
        <button v-if="!sourceIsPreview && archiveStatus !== 'uploading' && archiveStatus !== 'success'" class="button text-button" @click="archiveOriginal">{{ authenticated ? '重试归档' : '登录并归档' }}</button>
      </view>
      <text v-if="sourcePath && !sourceIsPreview" class="hint block">原图归档上限 5MB；大图仍可本地制作。</text>
      <text v-if="archiveError" class="error">{{ archiveError }}</text>
      <view v-if="hasDraft && !pattern && !sourcePath" class="draft-row">
        <text class="hint">还有一份上次保存的图纸</text>
        <button class="button secondary" @click="restoreDraft">继续上次制作</button>
      </view>

      <view class="settings">
        <view class="setting-line"><text class="label">图片类型</text><view class="choices"><button v-for="item in modes" :key="item.id" class="chip" :class="{ active: mode === item.id }" :disabled="busy || exporting || savingDraft" @click="mode = item.id">{{ item.name }}</button></view></view>
        <text class="hint block">插画保留色块和轮廓；照片保留自然的明暗过渡。</text>
        <view class="setting-line"><text class="label">杂点清理</text><view class="choices"><button v-for="item in cleanupLevels" :key="item.id" class="chip" :class="{ active: cleanup === item.id }" :disabled="busy || exporting || savingDraft" @click="cleanup = item.id">{{ item.name }}</button></view></view>
        <text class="hint block">轻度减少零散杂色；标准清理更强，精细图案可关闭。</text>
        <view class="setting-line"><text class="label">图片背景</text><view class="choices"><button v-for="item in backgrounds" :key="item.id" class="chip" :class="{ active: background === item.id }" :disabled="busy || exporting || savingDraft" @click="background = item.id">{{ item.name }}</button></view></view>
        <text class="hint block">去除背景适合居中的单一主体；背景复杂或边缘不完整时，可改选保留背景，或用画笔修补透明格。</text>
        <view class="setting-line"><text class="label">图纸宽度</text><view class="choices"><button v-for="size in sizes" :key="size" class="chip" :class="{ active: columns === size }" :disabled="busy || exporting || savingDraft" @click="columns = size">{{ size }}</button></view></view>
        <view class="setting-line"><text class="label">最多颜色</text><view class="choices"><button v-for="limit in limits" :key="limit" class="chip" :class="{ active: maxColors === limit }" :disabled="busy || exporting || savingDraft" @click="maxColors = limit">{{ limit }} 色</button></view></view>
        <view class="setting-line"><text class="label">拼豆色板</text><picker :range="brands" range-key="name" :value="brandIndex" :disabled="busy || exporting || savingDraft" @change="changeBrand"><view class="picker-value">{{ selectedBrand.name }} <text>⌄</text></view></picker></view>
        <text class="hint">细节较多的图片建议 64 格以上、16 色起。保留图片比例，最长边不超过 128 格。色号和屏幕颜色仅供参考，请以实物色卡为准。</text>
        <text v-if="settingsChanged" class="settings-notice">设置已改变，重新生成后生效。</text>
        <text class="credit">{{ selectedBrand.credit }}</text>
        <button class="button primary generate" :disabled="!sourcePath || busy || exporting || savingDraft" :loading="busy" @click="requestConvert">{{ busy ? '正在制作图纸…' : pattern ? '按当前设置重新生成' : '生成拼豆图纸' }}</button>
      </view>
      <view v-if="error" class="error-panel"><text>{{ error }}</text><button v-if="sourcePath" class="button secondary" :disabled="busy" @click="requestConvert">重试生成</button></view>
    </view>

    <view v-if="pattern" class="pattern-section">
      <view class="section-heading"><view><text class="section-title">你的拼豆图纸</text><text class="hint block">{{ pattern.width }} 列 × {{ pattern.height }} 行 · {{ totalBeads }} 颗 · {{ materials.length }} 色 · {{ patternBrand.name }}</text></view><button class="button text-button" :disabled="!history.length || busy || exporting || savingDraft" @click="undo">撤销{{ history.length ? '（' + history.length + '）' : '' }}</button></view>
      <view class="zoom-row"><text class="hint">双向滑动查看 · {{ Math.round(zoom * 100) }}%</text><view class="zoom-actions"><button class="button secondary square" :disabled="zoom <= 1 || exporting" @click="zoom = Math.max(1, zoom - .5)">−</button><button class="button secondary square" :disabled="zoom >= 4 || exporting" @click="zoom = Math.min(4, zoom + .5)">＋</button></view></view>
      <scroll-view class="canvas-scroll" scroll-x scroll-y :scroll-with-animation="false" :style="{height: viewportHeight + 'px'}">
        <view class="canvas-wrap" :style="{ width: canvasWidth + 'px', height: canvasHeight + 'px' }">
          <canvas id="bead-preview" type="2d" canvas-id="bead-preview" class="pattern-canvas" :style="{width: canvasWidth + 'px', height: canvasHeight + 'px'}" @touchstart="touchStart" @touchend="touchEnd" @click="clickCanvas" />
        </view>
      </scroll-view>
      <view class="tools"><button v-for="item in tools" :key="item.id" class="button tool" :class="{active: tool === item.id}" :disabled="busy || exporting || savingDraft" @click="tool = item.id">{{ item.name }}</button></view>
      <text class="hint">{{ toolHint }}</text>
      <picker :range="brandPaintColors" range-key="label" :value="brandBrushIndex" :disabled="busy || exporting || savingDraft" @change="chooseBrandColor"><view class="picker-value add-color">从 {{ patternBrand.name }} 添加色号 <text>＋</text></view></picker>
      <scroll-view class="paint-palette" scroll-x><view class="paint-colors"><button v-for="color in paintColors" :key="color.index" class="paint-color" :class="{selected: !pendingBrushColor && selectedColor === color.index}" :disabled="busy || exporting || savingDraft" @click="selectExistingColor(color.index)"><view class="swatch" :style="{backgroundColor: color.hex}"/><text>{{ color.code }}</text></button></view></scroll-view>
      <view class="selected-description"><text class="hint">当前画笔：{{ selectedColorName }}</text><text class="hint">{{ editNotice }}</text></view>
      <view class="save-actions"><button class="button secondary" :disabled="busy || exporting || savingDraft" :loading="savingDraft" @click="saveDraft">{{ savingDraft ? '正在保存…' : '保存草稿' }}</button><button class="button primary" :disabled="busy || exporting || savingDraft" :loading="exporting" @click="exportPattern">{{ exporting ? exportProgress : '保存 PNG 图纸' }}</button></view>
      <text v-if="saveNotice" class="hint block">{{ saveNotice }}</text>
    </view>

    <view v-if="pattern" class="materials-section">
      <view class="section-heading"><text class="section-title">准备这些豆子</text><text class="hint">共 {{ totalBeads }} 颗</text></view>
      <text class="hint block">不想使用某个颜色？将它禁用，再重新生成图纸。</text>
      <view v-for="color in materials" :key="color.code" class="material-row"><view class="swatch" :style="{backgroundColor: color.hex}"/><view class="material-name"><text>{{ color.code }} · {{ color.name }}</text><text class="hint">{{ color.count }} 颗</text></view><button class="button text-button" :disabled="busy || exporting || savingDraft" @click="toggleExcluded(color.code)">{{ excludedCodes.includes(color.code) ? '恢复使用' : '禁用此色' }}</button></view>
      <view v-if="excludedCodes.length" class="excluded"><text class="hint">已禁用 {{ excludedCodes.length }} 色</text><view class="excluded-list"><button v-for="code in excludedCodes" :key="code" class="chip" :disabled="busy || exporting || savingDraft" @click="toggleExcluded(code)">{{ code }} ×</button></view><button class="button secondary" :disabled="!sourcePath || busy || exporting || savingDraft" @click="requestConvert">使用剩余颜色重新生成</button></view>
      <text class="credit">{{ patternBrand.credit }}</text>
    </view>
    <view v-if="!pattern" class="empty-note"><text class="empty-symbol">● ● ●</text><text>一张照片，一点耐心，一份小小的快乐。</text><text class="hint">无需登录也能制作、编辑和保存图纸。</text></view>
    <canvas id="bead-source" type="2d" canvas-id="bead-source" class="offscreen" />
    <canvas id="bead-export" type="2d" canvas-id="bead-export" class="offscreen" />
    <OneClickLogin v-if="showLogin" :visible="showLogin" @close="showLogin = false" @login-success="loginSuccess" />
  </view>
</template>

<script setup>
import { ref, shallowRef, computed, nextTick, getCurrentInstance, watch } from 'vue';
import { onReady, onUnload } from '@dcloudio/uni-app';
import OneClickLogin from '@/components/OneClickLogin.vue';
import { uploadBeadOriginal } from '@/api/beads.js';
import { convertToPattern, countMaterials, paintCell, replaceColor } from '@/utils/beads/engine.mjs';
import { BEAD_PALETTES } from '@/utils/beads/palettes.mjs';
import { getCanvasSurface, decodePhoto, drawPattern, exportTiles, drawExport, surfaceFile, saveExport } from '@/utils/beads/canvas.js';

const scope = getCurrentInstance().proxy;
const DRAFT_KEY = 'beads-draft-v1';
const sizes = [32, 48, 64, 96, 128], limits = [8, 16, 24, 32];
const brands = BEAD_PALETTES;
const modes = [{id:'illustration',name:'插画'}, {id:'photo',name:'照片'}];
const cleanupLevels = [{id:0,name:'关闭'}, {id:1,name:'轻度'}, {id:2,name:'标准'}];
const backgrounds = [{id:'keep',name:'保留背景'}, {id:'remove',name:'去除背景'}];
const mode = ref('illustration'), cleanup = ref(1), background = ref('keep');
const tools = [{id:'paint',name:'点格换色'}, {id:'erase',name:'擦除'}, {id:'replace',name:'整色替换'}];
const patternBrandId = ref(brands[0].id);
const columns = ref(48), maxColors = ref(16), brandIndex = ref(0), excludedCodes = ref([]);
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
function changeBrand(event) { brandIndex.value = Number(event.detail.value); excludedCodes.value = []; }
function previewOriginal() { if (sourcePath.value) uni.previewImage({urls:[sourcePath.value],current:sourcePath.value}); }
function choosePhoto() {
  uni.chooseImage({count:1,sizeType:['original'],sourceType:['album'], success: async result => {
    const path = result.tempFilePaths?.[0]; if (!path || !alive) return;
    const ticket = ++generation; sourcePath.value = path; sourceIsPreview.value = false; persistentPath = ''; rgba = null; pattern.value = null; history.value = []; excludedCodes.value = [];
    error.value = ''; archiveError.value = ''; archiveStatus.value = 'idle'; saveNotice.value = ''; editNotice.value = ''; zoom.value = 1;
    archiveOriginal(false);
    await convert(ticket);
  }, fail: result => { if (!String(result.errMsg).includes('cancel')) message('未能打开相册，请重试'); }});
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
  if (history.value.length) {
    const result = await new Promise(resolve => uni.showModal({title:'重新生成图纸？',content:'将按当前设置生成新图纸，手动修改和撤销记录会被清除。保存草稿后可继续保留当前版本。',confirmText:'重新生成',success:resolve}));
    if (!result.confirm || !alive) return;
  }
  await convert(generation);
}
async function convert(ticket) {
  if (!sourcePath.value || busy.value || exporting.value) return;
  busy.value = true; error.value = '';
  try {
    await nextTick();
    // Give the native view time to display the busy state before local image analysis.
    await new Promise(resolve => setTimeout(resolve, 20));
    if (!current(ticket)) return;
    if (!sourceSurface) sourceSurface = await getCanvasSurface('bead-source', scope);
    if (!rgba) rgba = await decodePhoto(sourceSurface, sourcePath.value, () => current(ticket));
    if (!current(ticket) || !rgba) return;
    const result = convertToPattern(rgba, {columns:columns.value,maxColors:maxColors.value,palette:selectedBrand.value.colors,excludedCodes:excludedCodes.value,mode:mode.value,cleanup:cleanup.value,background:background.value});
    if (!current(ticket)) return;
    pattern.value = result; patternBrandId.value = selectedBrand.value.id; generatedSettings.value = settingsKey(); history.value = []; selectedColor.value = materials.value[0]?.index ?? -1; pendingBrushColor.value = null; editNotice.value = ''; saveNotice.value = ''; zoom.value = 1;
  } catch (failure) { if (current(ticket)) error.value = failure.message || '图纸生成失败，请换一张图片重试'; }
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
  if (!pattern.value || busy.value || exporting.value || savingDraft.value) return;
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
  history.value = [...history.value.slice(-19), before];
  pattern.value = tool.value === 'replace' ? replaceColor(working, from, to) : paintCell(working, x, y, to);
  if (tool.value !== 'erase') selectExistingColor(to);
  editNotice.value = tool.value === 'replace' ? '同色豆子已一起替换' : `已修改第 ${y + 1} 行、第 ${x + 1} 列`;
  saveNotice.value = '';
}
function undo() { if (!history.value.length) return; pattern.value = history.value[history.value.length - 1]; history.value = history.value.slice(0,-1); if (!pattern.value.palette[selectedColor.value]) selectedColor.value = materials.value[0]?.index ?? -1; pendingBrushColor.value = null; editNotice.value = '已撤销上一步'; saveNotice.value = ''; }
function toggleExcluded(code) { if (selectedBrand.value.id !== patternBrandId.value) { brandIndex.value = Math.max(0,brands.findIndex(brand => brand.id === patternBrandId.value)); excludedCodes.value = []; } excludedCodes.value = excludedCodes.value.includes(code) ? excludedCodes.value.filter(item => item !== code) : [...excludedCodes.value,code]; }
async function saveDraft() {
  if (!pattern.value || savingDraft.value) return;
  savingDraft.value = true;
  const ticket = generation;
  try {
    let savedSource = sourcePath.value;
    // #ifndef H5
    if (savedSource && savedSource !== persistentPath) {
      if (uploadPromise) { try { await uploadPromise; } catch (_) { /* Archiving never blocks local drafts. */ } }
      if (!current(ticket)) return;
      const result = await new Promise((resolve,reject) => uni.saveFile({tempFilePath:savedSource,success:resolve,fail:reject}));
      if (!current(ticket)) return;
      savedSource = result.savedFilePath; persistentPath = savedSource; sourcePath.value = savedSource;
    }
    // #endif
    // #ifdef H5
    if (rgba && sourceSurface) savedSource = sourceSurface.canvas.toDataURL('image/png');
    // #endif
    if (!current(ticket)) return;
    const draft = {version:1,pattern:pattern.value,source:savedSource,mode:mode.value,cleanup:cleanup.value,background:background.value,columns:columns.value,maxColors:maxColors.value,brandId:selectedBrand.value.id,patternBrandId:patternBrandId.value,generatedSettings:generatedSettings.value,excludedCodes:excludedCodes.value,updatedAt:Date.now()};
    try { uni.setStorageSync(DRAFT_KEY,draft); }
    catch (_) { uni.setStorageSync(DRAFT_KEY,{...draft,source:''}); saveNotice.value = '图纸草稿已保存，原图未能保存在本机；恢复后仍可编辑和导出。'; hasDraft.value = true; return; }
    hasDraft.value = true; saveNotice.value = '草稿已保存在本机，可下次继续制作。';
  } catch (_) {
    if (!current(ticket)) return;
    try { uni.setStorageSync(DRAFT_KEY,{version:1,pattern:pattern.value,source:'',mode:mode.value,cleanup:cleanup.value,background:background.value,columns:columns.value,maxColors:maxColors.value,brandId:selectedBrand.value.id,patternBrandId:patternBrandId.value,generatedSettings:generatedSettings.value,excludedCodes:excludedCodes.value}); hasDraft.value = true; saveNotice.value = '图纸草稿已保存，原图未能保存在本机；恢复后仍可编辑和导出。'; }
    catch (_) { message('本机空间不足，草稿未保存，请先导出图纸'); }
  } finally { if (alive) savingDraft.value = false; }
}
async function restoreDraft() {
  try {
    const draft = uni.getStorageSync(DRAFT_KEY), saved = draft?.pattern;
    if (draft?.version !== 1 || !saved || !Number.isInteger(saved.width) || !Number.isInteger(saved.height) || saved.width < 1 || saved.height < 1 || saved.width > 128 || saved.height > 128 || !Array.isArray(saved.cells) || saved.cells.length !== saved.width * saved.height || !Array.isArray(saved.palette) || saved.palette.length > 512 || saved.palette.some(color => !/^#[0-9a-f]{6}$/i.test(color.hex) || typeof color.code !== 'string') || saved.cells.some(index => !Number.isInteger(index) || index < -1 || index >= saved.palette.length)) throw new Error('这份草稿无法读取，请重新选图');
    const ticket = ++generation; rgba = null; patternBrandId.value = draft.patternBrandId || draft.brandId || brands[0].id; pattern.value = saved; history.value = []; zoom.value = 1;
    columns.value = sizes.includes(draft.columns) ? draft.columns : 48; maxColors.value = limits.includes(draft.maxColors) ? draft.maxColors : 16;
    brandIndex.value = Math.max(0,brands.findIndex(brand => brand.id === draft.brandId)); excludedCodes.value = Array.isArray(draft.excludedCodes) ? draft.excludedCodes : [];
    mode.value = draft.mode === 'illustration' ? 'illustration' : 'photo'; cleanup.value = [0,1,2].includes(draft.cleanup) ? draft.cleanup : 0; background.value = draft.background === 'remove' ? 'remove' : 'keep';
    generatedSettings.value = restoreGeneratedSettings(draft.generatedSettings);
    sourcePath.value = draft.source || ''; sourceIsPreview.value = sourcePath.value.startsWith('data:'); persistentPath = sourcePath.value; archiveStatus.value = 'idle';
    selectedColor.value = materials.value[0]?.index ?? -1; pendingBrushColor.value = null;
    saveNotice.value = '已恢复本机草稿。';
    if (sourcePath.value) {
      try { await new Promise((resolve,reject) => uni.getImageInfo({src:sourcePath.value,success:resolve,fail:reject})); if (!current(ticket)) return; }
      catch (_) { if (!current(ticket)) return; sourcePath.value = ''; saveNotice.value = '图纸已恢复；原图已失效，仍可编辑和导出。重新选图可制作新图纸。'; }
    }
  } catch (failure) { message(failure.message || '草稿无法读取'); }
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
onUnload(() => { alive = false; generation++; pendingRender++; rgba = null; sourceSurface = null; exportSurface = null; });
</script>

<style scoped>
.beads-page{min-height:100vh;background:#fcfbf8;color:#272117;padding:32rpx 32rpx calc(60rpx + env(safe-area-inset-bottom));font-family:-apple-system,BlinkMacSystemFont,'Noto Sans',sans-serif;box-sizing:border-box}.intro{padding:16rpx 0 40rpx}.eyebrow{display:block;font-size:24rpx;color:#92723a;letter-spacing:2rpx}.title{display:block;margin:16rpx 0;font-size:54rpx;font-weight:750;letter-spacing:-2rpx}.subtitle{font-size:27rpx;color:#746b5b;line-height:1.7}.workspace{background:#fff;border:1px solid #eee8db;border-radius:24rpx;padding:28rpx}.photo-row{display:flex;gap:24rpx;align-items:center}.photo,.photo-placeholder{width:156rpx;height:170rpx;background:#f8f3e8;border-radius:16rpx;flex-shrink:0}.photo-placeholder{display:flex;align-items:center;justify-content:center;color:#d0ad65;font-size:72rpx}.photo-copy{flex:1;min-width:0;display:flex;flex-direction:column;gap:10rpx}.section-title{font-size:32rpx;font-weight:650;line-height:1.5}.hint{font-size:24rpx;color:#796f60;line-height:1.65}.block{display:block}.button{margin:0;padding:0 24rpx;min-height:88rpx;line-height:1.4;display:flex;align-items:center;justify-content:center;box-sizing:border-box;border-radius:16rpx;font-size:27rpx;font-weight:600;white-space:normal}.button::after,.chip::after,.paint-color::after{border:0}.primary{background:#f7bd4a;color:#31230a}.secondary{background:#f5f1e9;color:#514430;border:1px solid #e5dccb}.button[disabled],.chip[disabled],.paint-color[disabled]{opacity:.45}.choose{align-self:flex-start;margin-top:4rpx}.archive-row,.draft-row{display:flex;align-items:center;gap:12rpx;margin-top:22rpx}.archive-text{flex:1}.text-button{background:transparent;color:#8b641d;font-size:24rpx;padding:0 12rpx;flex-shrink:0}.settings{margin-top:26rpx;border-top:1px solid #f0eadd;padding-top:12rpx}.setting-line{display:flex;align-items:center;justify-content:space-between;gap:12rpx;padding:16rpx 0;flex-wrap:wrap}.label{font-size:26rpx;color:#514632}.choices{display:flex;gap:6rpx;flex-wrap:wrap}.chip{min-width:70rpx;min-height:88rpx;line-height:1.4;padding:12rpx;font-size:24rpx;display:flex;align-items:center;justify-content:center;background:#f6f3ed;color:#5b5140;border-radius:12rpx;margin:0;box-sizing:border-box}.chip.active,.tool.active{background:#fff0c8;color:#6c4a0e;box-shadow:inset 0 0 0 1px #e8bb5c}.picker-value{min-height:88rpx;min-width:220rpx;display:flex;align-items:center;justify-content:space-between;gap:32rpx;font-size:26rpx;background:#f6f3ed;padding:0 24rpx;border-radius:12rpx}.generate{width:100%;margin-top:24rpx}.error-panel{padding:20rpx;background:#fff3ed;margin-top:20rpx;border-radius:12rpx;display:flex;flex-direction:column;gap:20rpx;font-size:26rpx;color:#9a4127}.error{display:block;color:#9a4127;font-size:24rpx;line-height:1.6}.pattern-section,.materials-section{padding-top:40rpx}.section-heading{display:flex;align-items:center;justify-content:space-between;gap:10rpx}.zoom-row{display:flex;justify-content:space-between;align-items:center;padding:12rpx 0}.zoom-actions{display:flex;gap:12rpx}.square{min-width:88rpx;padding:0;font-size:34rpx}.canvas-scroll{width:100%;background:#eee9df;border:1px solid #dfd7c7;border-radius:12rpx;box-sizing:border-box}.canvas-wrap{position:relative}.pattern-canvas{display:block}.tools{display:flex;gap:12rpx;margin:20rpx 0 12rpx}.tool{flex:1;background:#eee9df;color:#675c48;padding:0 8rpx;font-size:24rpx}.paint-palette{width:100%;margin-top:16rpx;white-space:nowrap}.paint-colors{display:flex;gap:10rpx;width:max-content;padding:8rpx 4rpx 12rpx}.paint-color{display:flex;flex-direction:column;gap:8rpx;align-items:center;justify-content:center;min-width:100rpx;min-height:116rpx;padding:10rpx;border-radius:12rpx;background:#fff;border:1px solid #e6dfd2;margin:0;font-size:22rpx;color:#514632;line-height:1.4}.paint-color.selected{border:2px solid #9d6e16;background:#fff4d8}.swatch{display:block;width:44rpx;height:44rpx;border-radius:10rpx;border:1px solid rgba(0,0,0,.14);flex-shrink:0}.selected-description{display:flex;flex-direction:column;min-height:70rpx;margin-top:8rpx}.save-actions{display:flex;gap:16rpx;margin:24rpx 0 14rpx}.save-actions .button{flex:1}.material-row{display:flex;align-items:center;gap:18rpx;border-bottom:1px solid #eee7da;padding:14rpx 0}.material-name{display:flex;flex:1;min-width:0;flex-direction:column;gap:6rpx;font-size:26rpx}.excluded{padding:20rpx 0}.excluded-list{display:flex;gap:10rpx;flex-wrap:wrap;padding:12rpx 0 20rpx}.add-color{margin-top:16rpx;font-size:24rpx}.settings-notice{display:block;margin-top:16rpx;padding:16rpx;background:#fff3d4;border-radius:12rpx;color:#825b12;font-size:24rpx;line-height:1.6}.credit{display:block;margin-top:24rpx;font-size:21rpx;color:#8c8170;line-height:1.7;word-break:break-word}.empty-note{display:flex;flex-direction:column;gap:16rpx;align-items:center;text-align:center;color:#88765b;font-size:25rpx;padding:60rpx 8rpx}.empty-symbol{color:#dfb355;font-size:38rpx;letter-spacing:12rpx}.offscreen{position:fixed;left:-10000px;top:-10000px;width:1px;height:1px;pointer-events:none}@media (min-width:700px){.beads-page{max-width:960px;margin:auto;padding:32px}.title{font-size:36px}.choices{gap:8px}.canvas-scroll{max-width:100%}}
</style>
