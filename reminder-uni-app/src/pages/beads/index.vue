<template>
  <view class="beads-page" :class="{'has-pattern': !!pattern}">
    <view class="intro" :class="{compact: !!pattern}">
      <view class="intro-brand"><view class="bead-mark" aria-hidden="true"><view/><view/><view/><view/></view><view><text class="title">拼豆小工坊</text><text v-if="!pattern" class="subtitle">把喜欢的图片，拼成小小的作品。</text></view></view>
    </view>
    <view class="workspace" :class="{'workspace-compact': !!pattern}">
      <view v-if="!pattern && !sourcePath" class="welcome">
        <view class="welcome-art" aria-hidden="true"><view class="welcome-board"><view v-for="(bead,index) in welcomeBeads" :key="index" class="welcome-bead" :class="{'is-colored': bead === '1', 'is-light': index < 27}" /></view></view>
        <text class="welcome-title">先挑一张喜欢的图片</text><text class="hint welcome-description">人物、宠物、小插画，都可以试试。</text>
        <button class="button primary choose" :disabled="locked" @click="choosePhoto">选择图片</button>
        <text class="hint welcome-footnote">选好图片，自动配色生成图纸</text>
      </view>
      <view v-else class="photo-row">
        <image v-if="sourcePath" class="photo" :src="sourcePath" mode="aspectFit" @click="previewOriginal" />
        <view v-else class="photo-placeholder"><view class="bead-mark" aria-hidden="true"><view/><view/><view/><view/></view></view>
        <view class="photo-copy"><text class="section-title">{{ sourcePath ? '我的原图' : '上次的作品' }}</text><text class="hint">{{ sourcePath ? '已自动匹配图片参数' : '本机草稿，继续慢慢拼' }}</text></view>
        <button class="button secondary choose" :disabled="locked" @click="choosePhoto">换张图片</button>
      </view>
      <view v-if="hasDraft && !pattern && !sourcePath" class="draft-row"><view><text class="label">上次的作品还在</text><text class="hint block">已保存在这台设备上</text></view><button class="button secondary" :disabled="locked" @click="restoreDraft">继续上次制作</button></view>
      <view class="source-options">
        <view class="setting-line brand-setting"><text class="label">豆子品牌</text><picker aria-label="豆子品牌" :range="brands" range-key="name" :value="brandIndex" :disabled="locked || (!!pattern && !sourcePath)" @change="changeBrand"><view class="picker-value"><text>{{ selectedBrand.name }}</text><text class="picker-chevron">⌄</text></view></picker></view>
        <view v-if="sourcePath" class="background-row"><text class="label">去除背景</text><switch color="#dc8b2b" :checked="background === 'remove'" :disabled="locked" @change="background = $event.detail.value ? 'remove' : 'keep'" /></view>
      </view>
    </view>
    <view v-if="busy" class="progress-note"><view class="status-dot"/><view><text>{{ phase }}</text><text class="hint block">小豆子正在就位，请稍候</text></view></view>
    <view v-if="error" class="error-panel"><text>{{ error }}</text><button v-if="sourcePath" class="button secondary" :disabled="locked" @click="requestConvert">重试制作</button></view>
    <view v-if="pattern" class="pattern-section">
      <view class="section-heading"><view><text class="section-title">图纸预览</text><text class="hint block pattern-meta">{{ totalBeads }} 颗 · {{ materials.length }} 色 · {{ pattern.width }} × {{ pattern.height }} 格</text></view><button class="button edit-toggle" :class="{'is-editing': editing}" :disabled="locked || settingsChanged" @click="editing = !editing">{{ editing ? '完成修图' : '修图' }}</button></view>
      <scroll-view class="canvas-scroll" :scroll-x="!pinching" :scroll-y="!pinching" :scroll-left="viewScrollLeft" :scroll-top="viewScrollTop" :scroll-with-animation="false" :style="{height: viewportHeight + 'px'}" @scroll="trackScroll"><view class="canvas-wrap" :style="{width: canvasWidth + 'px', height: canvasHeight + 'px'}">
        <!-- #ifdef H5 -->
        <view id="bead-preview" class="pattern-canvas" :style="{width: canvasWidth + 'px', height: canvasHeight + 'px'}" @touchstart="touchStart" @touchmove="touchMove" @touchend="touchEnd" @touchcancel="touchCancel" @click="clickCanvas" />
        <!-- #endif -->
        <!-- #ifndef H5 -->
        <view id="bead-preview" class="pattern-canvas" :style="{width: canvasWidth + 'px', height: canvasHeight + 'px'}" @touchstart="touchStart" @touchmove="touchMove" @touchend="touchEnd" @touchcancel="touchCancel"><image v-if="previewImagePath" class="preview-bitmap" :src="previewImagePath" mode="scaleToFill" /></view>
        <!-- #endif -->
      </view></scroll-view>
      <view v-if="previewError" class="error-panel"><text>{{ previewError }}</text><button class="button secondary" :disabled="locked" @click="render">重新显示图纸</button></view>
      <view class="zoom-row"><view class="code-toggle"><text class="label">色号</text><switch aria-label="显示色号" color="#dc8b2b" :checked="showCodes" :disabled="locked" @change="showCodes = $event.detail.value" /></view><view class="zoom-actions"><button class="button text-button fit-view" :disabled="locked" @click="fitView">全图</button><button class="button square" aria-label="缩小图纸" :disabled="zoom <= 1 || locked" @click="changeZoom(.5)">−</button><text class="hint zoom-value">{{ Math.round(zoom * 100) }}%</text><button class="button square" aria-label="放大图纸" :disabled="zoom >= MAX_ZOOM || locked" @click="changeZoom(2)">＋</button></view></view>
      <view v-if="sourcePath && !editing" class="detail-actions"><text class="label">图案细节</text><view class="choices"><button class="button secondary" :disabled="locked || columns <= 32" @click="adjustDetail(-1)">更省豆</button><button class="button secondary" :disabled="locked || columns >= 96" @click="adjustDetail(1)">更清楚</button></view></view>
      <view v-if="editing" class="editor-panel">
        <view class="editor-heading"><text class="section-title">修图工具</text><button class="button text-button" :disabled="!history.length || locked" @click="undo">撤销</button></view>
        <view class="tools"><button v-for="item in tools" :key="item.id" class="button tool" :class="{active: tool === item.id}" :aria-pressed="tool === item.id" :disabled="locked" @click="tool = item.id">{{ item.name }}</button></view>
        <picker :range="brandPaintColors" range-key="label" :value="brandBrushIndex" :disabled="locked" @change="chooseBrandColor"><view class="picker-value add-color">从 {{ patternBrand.name }} 添加色号 <text>＋</text></view></picker>
        <scroll-view class="paint-palette" scroll-x><view class="paint-colors"><button v-for="color in paintColors" :key="color.index" class="paint-color" :class="{selected: !pendingBrushColor && selectedColor === color.index}" :disabled="locked" @click="selectExistingColor(color.index)"><view class="swatch" :style="{backgroundColor: color.hex}"/><text>{{ color.code }}</text></button></view></scroll-view>
        <view class="selected-description"><text class="hint">当前画笔：{{ selectedColorName }}</text><text class="hint">{{ editNotice }}</text></view>
      </view>
      <view class="export-panel">
        <view class="export-settings"><text class="label">导出方式</text><view class="choices"><button class="chip" :class="{active: exportMode === 'whole'}" :aria-pressed="exportMode === 'whole'" :disabled="locked" @click="exportMode = 'whole'">完整一张</button><button class="chip" :class="{active: exportMode === 'tiles'}" :aria-pressed="exportMode === 'tiles'" :disabled="locked" @click="exportMode = 'tiles'">分块导出</button></view></view>
        <view class="export-settings quality-settings"><text class="label">清晰度</text><view class="choices"><button class="chip" :class="{active: exportQuality === 'standard'}" :aria-pressed="exportQuality === 'standard'" :disabled="locked" @click="exportQuality = 'standard'">标准</button><button class="chip" :class="{active: exportQuality === 'high'}" :aria-pressed="exportQuality === 'high'" :disabled="locked" @click="exportQuality = 'high'">高清</button></view></view>
        <view v-if="exportFailed && exportPaths.length" class="export-preview"><button class="button secondary" :disabled="locked" @click="previewExport">预览图纸 · {{ exportPaths.length }} 张</button></view>
        <view v-if="draftFailed" class="draft-status"><text class="hint">{{ draftNotice }}</text><button class="button text-button" :disabled="savingDraft || locked" @click="saveDraft">重试保存</button></view>
      </view>
      <view class="materials-section"><button class="disclosure" :aria-expanded="showMaterials" @click="showMaterials = !showMaterials"><view class="disclosure-copy"><text class="section-title">用豆清单</text><text class="hint">{{ materials.length }} 色，共 {{ totalBeads }} 颗</text></view><text class="disclosure-action">{{ showMaterials ? '收起' : '展开' }} <text class="disclosure-chevron">{{ showMaterials ? '⌃' : '⌄' }}</text></text></button><view v-if="showMaterials" class="materials-list"><view v-for="color in materials" :key="color.code" class="material-row"><view class="swatch" :style="{backgroundColor: color.hex}"/><view class="material-name"><text>{{ color.code }} · {{ color.name }}</text><text class="hint">{{ color.count }} 颗</text></view><button v-if="showMissing && sourcePath" class="button text-button" :disabled="locked" @click="toggleExcluded(color.code)">{{ excludedCodes.includes(color.code) ? '恢复' : '缺少此色' }}</button></view><button v-if="sourcePath" class="button text-button missing-action" @click="showMissing = !showMissing">{{ showMissing ? '完成' : '缺少某个颜色？' }}</button></view></view>
    </view>
    <view class="more-section"><button class="disclosure" :aria-expanded="showMore" @click="showMore = !showMore"><view class="disclosure-copy"><text class="section-title">更多设置</text><text class="hint">图纸尺寸、配色与原图备份</text></view><text class="disclosure-action">{{ showMore ? '收起' : '展开' }} <text class="disclosure-chevron">{{ showMore ? '⌃' : '⌄' }}</text></text></button><view v-if="showMore" class="more-content">
      <text class="hint block settings-description">{{ assessmentReason || '选好图片后会自动匹配参数，也可以手动调整。' }}</text>
      <view class="setting-line"><text class="label">图纸宽度</text><view class="choices"><button v-for="size in sizes" :key="size" class="chip" :class="{active: columns === size}" :disabled="locked || (!!pattern && !sourcePath)" @click="columns = size">{{ size }}</button></view></view>
      <view class="setting-line"><text class="label">最多颜色</text><view class="choices"><button v-for="limit in limits" :key="limit" class="chip" :class="{active: maxColors === limit}" :disabled="locked || (!!pattern && !sourcePath)" @click="maxColors = limit">{{ limit }} 色</button></view></view>
      <view class="setting-line"><text class="label">杂点清理</text><view class="choices"><button v-for="item in cleanupLevels" :key="item.id" class="chip" :class="{active: cleanup === item.id}" :disabled="locked || (!!pattern && !sourcePath)" @click="cleanup = item.id">{{ item.name }}</button></view></view>
      <view v-if="excludedCodes.length" class="excluded"><text class="hint">已排除这些颜色，轻点可恢复</text><view class="excluded-list"><button v-for="code in excludedCodes" :key="code" class="chip" :disabled="locked" @click="toggleExcluded(code)">{{ code }} ×</button></view></view>
      <view v-if="sourcePath" class="archive-details"><text class="label">原图备份</text><text class="hint block">{{ archiveLabel }}</text><text v-if="archiveError" class="error">{{ archiveError }}</text><button v-if="!sourceIsPreview && archiveStatus !== 'uploading' && archiveStatus !== 'success'" class="button text-button" @click="archiveOriginal">{{ authenticated ? '重试备份' : '登录并备份' }}</button><text class="hint block">仅备份原图，上限5MB；不影响本机制作。</text></view>
      <text class="palette-credit">{{ pattern ? patternBrand.credit : selectedBrand.credit }}</text>
    </view></view>
    <!-- #ifdef H5 -->
    <view id="bead-source" class="offscreen" /><view id="bead-export" class="offscreen" />
    <!-- #endif -->
    <!-- #ifndef H5 -->
    <canvas id="bead-source" type="2d" canvas-id="bead-source" class="offscreen" /><canvas id="bead-export" type="2d" canvas-id="bead-export" class="offscreen" /><canvas id="bead-preview-render" type="2d" canvas-id="bead-preview-render" class="offscreen" />
    <!-- #endif -->
    <view v-if="pattern" class="save-dock"><view class="save-dock-inner"><view class="save-summary"><text class="save-summary-title">{{ exportMode === 'whole' ? '完整一张' : '分块导出' }} · {{ exportQuality === 'high' ? '高清' : '标准' }}</text></view><button class="button primary save-main" :disabled="locked || (!!sourcePath && settingsChanged)" :loading="exporting" @click="exportPattern">{{ exporting ? exportProgress : saveLabel }}</button></view></view>
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
const welcomeBeads = ['001101100','011111110','111111111','111111111','011111110','001111100','000111000','000010000'].join('').split('');
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
const exportPaths = shallowRef([]), savedPages = ref(0), exportMode = ref('whole'), exportQuality = ref('high');
const exportFailed = ref(false);
const pinching = ref(false), viewScrollLeft = ref(0), viewScrollTop = ref(0);
const previewMaxHeight = ref(420);
let pinchState = null, scrollPosition = {left:0,top:0}, previewLayoutReady = false;
const previewImagePath = ref(''), previewError = ref('');
let previewSurface = null, previewCache = null;
const archiveNeedsLogin = ref(false);
const archiveStatus = ref('idle'), authenticated = ref(!!uni.getStorageSync('accessToken')), showLogin = ref(false);
const pendingBrushColor = shallowRef(null), generatedSettings = ref('');
const selectedColor = ref(-1), tool = ref('paint'), editNotice = ref(''), saveNotice = ref(''), exportProgress = ref('正在保存…'), hasDraft = ref(false);
let alive = true, generation = 0, rgba = null, sourceSurface = null, exportSurface = null, startTouch = null;
let uploadPromise = null, persistentPath = '', pendingRender = 0, lastTouchEdit = 0;
const editing = ref(false), hasManualEdits = ref(false), showMore = ref(false), showMaterials = ref(false), showMissing = ref(false), showCodes = ref(true);
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
watch(pattern, () => { previewLayoutReady = false; if (!pattern.value) previewImagePath.value = ''; exportPaths.value = []; savedPages.value = 0; exportFailed.value = false; if (!restoring) scheduleDraft(); });
watch([exportMode, exportQuality], () => { exportPaths.value = []; savedPages.value = 0; exportFailed.value = false; saveNotice.value = ''; });
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
const screenWidth = ref(Math.max(280, uni.getSystemInfoSync().windowWidth - 40));
const MAX_ZOOM = 16;
const canvasBorder = computed(() => showCodes.value ? 1 : 0);
const fitCellSize = computed(() => pattern.value ? Math.min(screenWidth.value / (pattern.value.width + canvasBorder.value * 2), previewMaxHeight.value / (pattern.value.height + canvasBorder.value * 2)) : 5);
const cellSize = computed(() => fitCellSize.value * zoom.value);
const canvasWidth = computed(() => pattern.value ? (pattern.value.width + canvasBorder.value * 2) * cellSize.value : 1);
const canvasHeight = computed(() => pattern.value ? (pattern.value.height + canvasBorder.value * 2) * cellSize.value : 1);
const viewportHeight = computed(() => Math.min(previewMaxHeight.value, Math.max(180, canvasHeight.value)));
function measurePreview() {
  if (!uni.createSelectorQuery) return Promise.resolve();
  const query = uni.createSelectorQuery().in(scope);
  if (!query.selectViewport) return Promise.resolve();
  return new Promise(resolve => {
    for (const selector of ['.canvas-scroll','.zoom-row','.save-dock']) query.select(selector).boundingClientRect();
    query.selectViewport().scrollOffset();
    query.exec(([canvas,controls,dock,scroll]) => {
      if (alive && canvas && controls && dock) {
        const available = dock.top - canvas.top - (scroll?.scrollTop || 0) - controls.height - 20;
        if (Number.isFinite(available)) previewMaxHeight.value = Math.max(180,Math.min(420,Math.floor(available)));
      }
      resolve();
    });
  });
}
function resizePreview(event) {
  const width = event.size?.windowWidth || uni.getSystemInfoSync().windowWidth;
  screenWidth.value = Math.max(280,width - 40); previewLayoutReady = false; fitView(); render();
}
if (uni.onWindowResize) uni.onWindowResize(resizePreview);
function trackScroll(event) { scrollPosition = {left:event.detail.scrollLeft || 0,top:event.detail.scrollTop || 0}; }
function positionView(left, top) {
  viewScrollLeft.value = Math.max(0, Math.min(left, canvasWidth.value - screenWidth.value));
  viewScrollTop.value = Math.max(0, Math.min(top, canvasHeight.value - viewportHeight.value));
  scrollPosition = {left:viewScrollLeft.value,top:viewScrollTop.value};
}
function fitView() { zoom.value = 1; positionView(0,0); }
function changeZoom(factor) {
  const previous = zoom.value, next = Math.max(1,Math.min(MAX_ZOOM,previous * factor));
  const anchorX = screenWidth.value / 2, anchorY = viewportHeight.value / 2;
  const left = (scrollPosition.left + anchorX) * next / previous - anchorX;
  const top = (scrollPosition.top + anchorY) * next / previous - anchorY;
  zoom.value = next; positionView(left,top);
}
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
  error.value = ''; archiveError.value = ''; archiveStatus.value = 'idle'; saveNotice.value = ''; draftNotice.value = ''; assessmentReason.value = ''; fitView();
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
    selectedColor.value = materials.value[0]?.index ?? -1; pendingBrushColor.value = null; editNotice.value = ''; saveNotice.value = ''; fitView(); rememberBrand(); scheduleDraft();
  } catch (failure) { if (current(ticket)) error.value = failure.message || '制作失败，请重试或换张图片'; }
  finally { if (current(ticket)) busy.value = false; }
}
async function render() {
  const renderTicket = ++pendingRender;
  await nextTick(); if (!pattern.value || !alive || renderTicket !== pendingRender) return;
  try {
    previewError.value = '';
    if (!previewLayoutReady) { previewLayoutReady = true; await measurePreview(); await nextTick(); }
    if (!alive || renderTicket !== pendingRender || !pattern.value) return;
    // #ifdef H5
    const surface = await getCanvasSurface('bead-preview', scope);
    if (alive && renderTicket === pendingRender && pattern.value) drawPattern(surface, pattern.value, cellSize.value, showCodes.value);
    return;
    // #endif
    // #ifndef H5
    // A normal image clips inside scroll-view; native Canvas 2D does not on all devices.
    if (pinching.value && previewImagePath.value) return;
    const snapshot = pattern.value, codes = showCodes.value, labels = codes && cellSize.value >= 12;
    if (previewCache?.pattern === snapshot && previewCache.codes === codes && previewCache.labels === labels) return;
    if (!previewSurface) previewSurface = await getCanvasSurface('bead-preview-render',scope);
    if (!alive || renderTicket !== pendingRender) return;
    const border = codes ? 2 : 0, rasterCell = Math.min(32,2048 / Math.max(snapshot.width + border,snapshot.height + border));
    drawPattern(previewSurface,snapshot,rasterCell,codes,labels);
    const path = await surfaceFile(previewSurface,scope);
    if (alive && renderTicket === pendingRender && pattern.value === snapshot) {
      previewImagePath.value = path; previewCache = {pattern:snapshot,codes,labels};
    }
    // #endif
  }
  catch (failure) { if (alive && renderTicket === pendingRender) previewError.value = failure.message || failure.errMsg || '图纸暂时未能显示，请重试'; }
}
watch([pattern, zoom, showCodes, pinching, previewMaxHeight, screenWidth], render);
function touchStart(event) {
  const touches = event.touches || [];
  if (touches.length >= 2) {
    startTouch = null; pinching.value = true;
    const state = {distance:Math.max(1,Math.hypot(touches[1].clientX-touches[0].clientX,touches[1].clientY-touches[0].clientY)),zoom:zoom.value,left:scrollPosition.left,top:scrollPosition.top,anchorX:screenWidth.value/2,anchorY:viewportHeight.value/2};
    pinchState = state;
    const centerX = (touches[0].clientX + touches[1].clientX) / 2, centerY = (touches[0].clientY + touches[1].clientY) / 2;
    uni.createSelectorQuery().in(scope).select('.canvas-scroll').boundingClientRect(rect => {
      if (pinchState === state && rect) { state.anchorX = centerX - rect.left; state.anchorY = centerY - rect.top; }
    }).exec();
    return;
  }
  const point = touches[0]; startTouch = point && !pinching.value ? {x:point.clientX,y:point.clientY} : null;
}
function touchMove(event) {
  const touches = event.touches || [];
  if (touches.length < 2 || locked.value) return;
  event.preventDefault?.();
  if (!pinchState) { touchStart(event); return; }
  const state = pinchState, distance = Math.hypot(touches[1].clientX-touches[0].clientX,touches[1].clientY-touches[0].clientY);
  zoom.value = Math.max(1,Math.min(MAX_ZOOM,state.zoom * distance / state.distance));
  const ratio = zoom.value / state.zoom;
  positionView((state.left + state.anchorX) * ratio - state.anchorX,(state.top + state.anchorY) * ratio - state.anchorY);
}
function touchCancel() { pinching.value = false; pinchState = null; startTouch = null; lastTouchEdit = Date.now(); }
async function touchEnd(event) {
  if (pinching.value) { if (!event.touches?.length) touchCancel(); return; }
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
  const border = canvasBorder.value;
  const x = Math.floor((point.clientX - rect.left) / rect.width * (pattern.value.width + border * 2)) - border;
  const y = Math.floor((point.clientY - rect.top) / rect.height * (pattern.value.height + border * 2)) - border;
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
    const ticket = ++generation; rgba = null; editing.value = false; hasManualEdits.value = draft.edited !== false; assessmentReason.value = draft.assessmentReason || ''; sourceCopyPromise = null; patternBrandId.value = draft.patternBrandId || draft.brandId || brands[0].id; pattern.value = saved; history.value = []; fitView();
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
function previewExport() {
  if (exportPaths.value.length) uni.previewImage({urls:[...exportPaths.value],current:exportPaths.value[0]});
}
async function exportPattern() {
  if (!pattern.value || exporting.value) return;
  exporting.value = true; exportFailed.value = false; saveNotice.value = ''; const snapshot = pattern.value;
  const list = countMaterials(snapshot), tiles = exportTiles(snapshot, exportMode.value === 'tiles' ? 48 : Math.max(snapshot.width,snapshot.height)), ticket = generation;
  let stage = 'render';
  try {
    if (exportPaths.value.length !== tiles.length) {
      // Finish every image before requesting album access, retaining previews on failure.
      exportPaths.value = []; savedPages.value = 0;
      if (!exportSurface) exportSurface = await getCanvasSurface('bead-export',scope);
      for (let index = 0; index < tiles.length; index++) {
        if (!current(ticket)) return;
        exportProgress.value = `生成图纸 ${index + 1}/${tiles.length}`;
        drawExport(exportSurface,snapshot,list,tiles[index],index + 1,tiles.length,patternBrand.value.name,patternBrand.value.credit,exportQuality.value);
        const path = await surfaceFile(exportSurface,scope);
        if (!current(ticket)) return;
        exportPaths.value = [...exportPaths.value,path];
      }
    }
    stage = 'album';
    if (savedPages.value === exportPaths.value.length) savedPages.value = 0;
    for (let index = savedPages.value; index < exportPaths.value.length; index++) {
      if (!current(ticket)) return;
      exportProgress.value = `保存 ${index + 1}/${exportPaths.value.length}`;
      await saveExport(exportPaths.value[index],`拼豆图纸-${index + 1}.png`);
      if (!current(ticket)) return;
      savedPages.value = index + 1;
    }
    uni.showToast({title:'导出成功',icon:'success',duration:1500});
  } catch (failure) {
    if (!alive) return;
    exportFailed.value = true;
    const detail = String(failure.errMsg || failure.message || '未知错误').slice(0,240);
    if (stage === 'album' && /auth|deny|denied|permission/i.test(detail)) {
      saveNotice.value = `已保存 ${savedPages.value} 张。相册权限未开启，可预览图纸或开启权限后重试。错误：${detail}`;
      uni.showModal({title:'需要相册权限',content:'请开启相册权限后重试。',confirmText:'打开设置',cancelText:'取消',success: result => { if (result.confirm) uni.openSetting({success: settings => { if (alive && settings.authSetting?.['scope.writePhotosAlbum']) exportPattern(); }}); }});
      return;
    } else if (stage === 'album') saveNotice.value = `已保存 ${savedPages.value} 张，写入相册失败。可预览图纸或重试。错误：${detail}`;
    else saveNotice.value = `图纸图片生成失败，${exportMode.value === 'whole' ? '可切换分块导出或重试' : '请重试'}。错误：${detail}`;
    uni.showModal({title:'导出失败',content:detail,showCancel:false});
  } finally { if (alive) exporting.value = false; }
}
onReady(() => { try { hasDraft.value = !!uni.getStorageSync(DRAFT_KEY); } catch (_) { hasDraft.value = false; } });
onUnload(() => { clearTimeout(convertTimer); clearTimeout(draftTimer); if (pattern.value) flushDraft(); uni.offWindowResize?.(resizePreview); alive = false; generation++; pendingRender++; rgba = null; sourceSurface = null; exportSurface = null; previewSurface = null; previewCache = null; });
</script>

<style scoped>
.preview-bitmap{display:block;width:100%;height:100%;pointer-events:none}
.beads-page{--page:#fff9ec;--surface:#fff;--ink:#3b3028;--muted:#776858;--line:#eddfca;--yellow:#ffca55;--yellow-soft:#fff1c5;--coral:#a94f3d;--coral-soft:#fff0e8;--mint:#4c8070;min-height:100vh;box-sizing:border-box;padding:24rpx 28rpx 36rpx;background:var(--page);color:var(--ink);font-family:-apple-system,BlinkMacSystemFont,'PingFang SC','Noto Sans',sans-serif}
.beads-page.has-pattern{padding-bottom:calc(140rpx + env(safe-area-inset-bottom))}
.intro{padding:12rpx 4rpx 30rpx}.intro.compact{padding:4rpx 4rpx 20rpx}.intro-brand{display:flex;align-items:center;gap:20rpx}.title{display:block;font-size:44rpx;line-height:1.4;font-weight:750;letter-spacing:0}.subtitle{display:block;margin-top:8rpx;font-size:24rpx;line-height:1.6;color:var(--muted)}.compact .title{font-size:36rpx}
.bead-mark{width:64rpx;height:64rpx;display:flex;flex-wrap:wrap;gap:8rpx;flex-shrink:0}.bead-mark>view{position:relative;width:28rpx;height:28rpx;border-radius:9rpx;background:#f8bd47}.bead-mark>view:nth-child(2){background:#ee9f8c}.bead-mark>view:nth-child(3){background:#93c4b3}.bead-mark>view:nth-child(4){background:#edc576}.bead-mark>view::after{content:'';position:absolute;width:8rpx;height:8rpx;border-radius:50%;background:#fff9ec;top:10rpx;left:10rpx}.compact .bead-mark{transform:scale(.82);transform-origin:center}
.workspace{background:var(--surface);border:1px solid var(--line);border-radius:28rpx;padding:28rpx}.welcome{text-align:center;padding:12rpx 4rpx 28rpx}.welcome-art{display:flex;justify-content:center;margin:8rpx 0 26rpx}.welcome-board{display:flex;flex-wrap:wrap;width:228rpx;gap:6rpx;padding:16rpx;background:#fff8e8;border-radius:20rpx;box-sizing:content-box}.welcome-bead{position:relative;width:20rpx;height:20rpx;flex-shrink:0;border-radius:6rpx;background:#f0e5d0}.welcome-bead::after{content:'';position:absolute;width:6rpx;height:6rpx;top:7rpx;left:7rpx;border-radius:50%;background:#fff8e8}.welcome-bead.is-colored{background:#e99a87}.welcome-bead.is-colored.is-light{background:#f5b39e}.welcome-title{display:block;font-size:36rpx;font-weight:700;line-height:1.5}.welcome-description{display:block;margin-top:8rpx}.welcome .choose{width:100%;margin-top:26rpx;min-height:96rpx}.welcome-footnote{display:block;margin-top:14rpx;font-size:22rpx}
.button{display:flex;align-items:center;justify-content:center;box-sizing:border-box;min-height:88rpx;margin:0;padding:0 24rpx;font-size:26rpx;font-weight:650;line-height:1.45;white-space:normal;border-radius:22rpx;transition:background-color .15s ease-out,transform .15s ease-out}.button::after,.chip::after,.paint-color::after,.disclosure::after{border:0}.primary{background:var(--yellow);color:#493219}.secondary{background:#fff9ee;color:#715330;border:1px solid #ebd7b4}.text-button{background:transparent;color:#8e592a;padding:0 14rpx;flex-shrink:0}.button[disabled],.chip[disabled],.paint-color[disabled]{opacity:.43}.button:not([disabled]):active,.chip:not([disabled]):active{transform:translateY(1px)}.button:focus-visible,.chip:focus-visible,.disclosure:focus-visible,.paint-color:focus-visible{outline:2px solid #a65c26;outline-offset:3px}
.section-title{font-size:30rpx;font-weight:700;line-height:1.5}.hint{font-size:23rpx;color:var(--muted);line-height:1.65}.block{display:block}.label{font-size:25rpx;color:#65513c;font-weight:550}.photo-row{display:flex;align-items:center;gap:18rpx}.photo,.photo-placeholder{width:100rpx;height:108rpx;flex-shrink:0;border-radius:18rpx;background:#fff6e6}.photo-placeholder{display:flex;align-items:center;justify-content:center}.photo-placeholder .bead-mark{transform:scale(.65)}.photo-copy{flex:1;min-width:0;display:flex;flex-direction:column;gap:6rpx}.photo-copy .section-title{font-size:28rpx}.photo-copy .hint{font-size:21rpx}.photo-row .choose{min-height:76rpx;padding:0 18rpx;font-size:23rpx;flex-shrink:0}.workspace-compact{padding:22rpx}.source-options{border-top:1px solid #f3eadb;margin-top:8rpx;padding-top:12rpx}.setting-line{display:flex;align-items:center;justify-content:space-between;gap:12rpx;padding:16rpx 0;flex-wrap:wrap}.brand-setting{flex-wrap:nowrap}.picker-value{display:flex;align-items:center;justify-content:space-between;gap:20rpx;min-height:80rpx;box-sizing:border-box;padding:0 20rpx;border-radius:16rpx;background:#f9f5ed;font-size:24rpx;color:#554634}.picker-chevron{color:#8c7250;font-size:28rpx}.background-row{display:flex;align-items:center;justify-content:space-between;gap:8rpx}.workspace-compact .source-options{display:flex;align-items:center;gap:16rpx;margin-top:18rpx;padding-top:14rpx}.workspace-compact .brand-setting{flex:1;min-width:0;padding:0}.workspace-compact .brand-setting>.label{display:none}.workspace-compact .brand-setting picker{width:100%}.workspace-compact .picker-value{min-height:72rpx;padding:0 14rpx;font-size:24rpx;gap:8rpx}.workspace-compact .background-row{flex-shrink:0}.workspace-compact .background-row .label{font-size:24rpx}.background-row switch{transform:scale(.82);transform-origin:right center;margin-left:-8rpx}.draft-row{display:flex;align-items:center;justify-content:space-between;gap:16rpx;padding:22rpx 0;border-top:1px dashed var(--line)}.draft-row .button{font-size:23rpx;padding:0 18rpx}
.pattern-section{padding-top:28rpx}.section-heading{display:flex;align-items:center;justify-content:space-between;gap:16rpx;margin-bottom:18rpx;padding:0 2rpx}.section-heading .section-title{font-size:32rpx}.pattern-meta{font-size:22rpx;margin-top:3rpx;font-variant-numeric:tabular-nums}.edit-toggle{min-height:76rpx;min-width:116rpx;padding:0 22rpx;border:1px solid #ecc7b7;background:var(--coral-soft);color:var(--coral);font-size:24rpx}.edit-toggle.is-editing{background:#f8b49b;border-color:#f8b49b;color:#592f24}.canvas-scroll{width:100%;box-sizing:border-box;background-color:#f8f2e7;background-image:radial-gradient(#deceb5 1px,transparent 1px);background-size:12px 12px;border:1px solid #dfcfb3;border-radius:24rpx;overflow:hidden}.canvas-wrap{position:relative;margin:0 auto}.pattern-canvas{display:block;touch-action:pan-x pan-y}
.zoom-row{display:flex;align-items:center;justify-content:space-between;gap:8rpx;margin-top:18rpx}.code-toggle{display:flex;align-items:center;gap:0;flex-shrink:0}.code-toggle .label{font-size:23rpx}.code-toggle switch{transform:scale(.72);transform-origin:left center;width:90rpx;margin-right:-16rpx;margin-left:8rpx}.zoom-actions{display:flex;align-items:center;gap:0;background:var(--surface);border:1px solid var(--line);border-radius:20rpx;padding:0 4rpx}.zoom-actions .button{min-height:80rpx;border-radius:16rpx}.fit-view{font-size:23rpx;padding:0 16rpx}.square{min-width:80rpx;padding:0;font-size:32rpx;background:transparent;color:#65513c}.zoom-value{min-width:66rpx;font-size:21rpx;text-align:center;font-variant-numeric:tabular-nums}.detail-actions{display:flex;align-items:center;justify-content:space-between;gap:12rpx;padding:20rpx 2rpx 0}.detail-actions .button{min-height:76rpx;padding:0 22rpx;font-size:23rpx}.choices{display:flex;gap:8rpx;flex-wrap:wrap}.chip{display:flex;align-items:center;justify-content:center;min-width:76rpx;min-height:80rpx;box-sizing:border-box;padding:12rpx 18rpx;background:#f8f4ed;color:#78634b;border:1px solid transparent;border-radius:16rpx;font-size:23rpx;line-height:1.4;font-weight:550;margin:0}.chip.active{background:var(--yellow-soft);border-color:#dfb860;color:#6d4a18}
.editor-panel{margin-top:24rpx;padding:22rpx;background:#fff3eb;border:1px solid #efd7c8;border-radius:24rpx}.editor-heading{display:flex;justify-content:space-between;align-items:center}.editor-heading .section-title{font-size:28rpx}.editor-heading .button{min-height:64rpx;font-size:23rpx}.tools{display:flex;gap:8rpx;margin:12rpx 0 16rpx}.tool{flex:1;min-height:80rpx;background:#fff;color:#795945;padding:0 8rpx;font-size:22rpx;border:1px solid #ead8cb;border-radius:16rpx}.tool.active{background:#f7d7c4;border-color:#d69772;color:#6a3d25}.add-color{width:100%;background:#fff;border:1px solid #ead8cb;font-size:22rpx;min-height:80rpx}.paint-palette{width:100%;margin-top:16rpx;white-space:nowrap}.paint-colors{display:flex;gap:10rpx;width:max-content;padding:4rpx 0 8rpx}.paint-color{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:10rpx;min-width:86rpx;min-height:112rpx;box-sizing:border-box;padding:8rpx;border-radius:16rpx;border:1px solid #e7d7cb;background:#fff;color:#6e4f39;font-size:21rpx;line-height:1.4;margin:0}.paint-color.selected{border:2px solid #b86f40;background:#fff2de}.swatch{display:block;position:relative;width:42rpx;height:42rpx;flex-shrink:0;border-radius:12rpx;border:1px solid rgba(55,40,22,.15)}.swatch::after{content:'';position:absolute;width:12rpx;height:12rpx;top:15rpx;left:15rpx;border-radius:50%;background:rgba(255,255,255,.8)}.selected-description{display:flex;flex-direction:column;gap:2rpx;min-height:50rpx;margin-top:10rpx}.selected-description .hint{font-size:21rpx}
.export-panel{margin-top:16rpx;background:var(--surface);border:1px solid var(--line);border-radius:24rpx;padding:12rpx 20rpx}.export-settings{display:flex;align-items:center;justify-content:space-between;gap:12rpx;padding:4rpx 0}.export-settings .choices{gap:6rpx}.export-settings .chip{padding:10rpx 16rpx}.export-preview{display:flex;flex-direction:column;gap:10rpx;margin-top:20rpx;padding-top:18rpx;border-top:1px dashed var(--line)}.export-preview .button{min-height:80rpx}.draft-status{display:flex;align-items:center;justify-content:space-between;gap:8rpx;min-height:44rpx;margin-top:14rpx}.status-dot{width:10rpx;height:10rpx;flex-shrink:0;border-radius:50%;background:#77a993}.status-dot.is-error{background:#c76d4d}.draft-status .button{min-height:60rpx;font-size:22rpx;padding:0 8rpx}
.materials-section,.more-section{margin-top:18rpx;border:1px solid var(--line);border-radius:24rpx;background:var(--surface);padding:0 24rpx}.disclosure{display:flex;align-items:center;justify-content:space-between;gap:12rpx;width:100%;min-height:116rpx;box-sizing:border-box;padding:18rpx 0;margin:0;background:transparent;color:var(--ink);text-align:left;border-radius:0}.disclosure-copy{display:flex;flex-direction:column;gap:2rpx}.disclosure-copy .section-title{font-size:28rpx}.disclosure-copy .hint{font-size:21rpx}.disclosure-action{font-size:21rpx;color:#84613c;white-space:nowrap}.disclosure-chevron{display:inline-block;margin-left:8rpx;font-size:26rpx}.material-row{display:flex;align-items:center;gap:16rpx;padding:18rpx 0;border-top:1px solid #f1e7da}.material-name{display:flex;flex:1;min-width:0;flex-direction:column;gap:4rpx;font-size:23rpx;word-break:break-word}.material-name .hint{font-size:21rpx}.material-row .button{font-size:21rpx;padding:0 8rpx}.missing-action{width:100%;font-size:23rpx}.more-content{padding-bottom:20rpx;border-top:1px solid #f1e7da}.settings-description{font-size:21rpx;padding:18rpx 0 4rpx}.more-content>.setting-line{display:block;padding:14rpx 0}.more-content>.setting-line>.label{display:block;margin-bottom:12rpx}.more-content .choices{gap:8rpx}.more-content .chip{min-width:78rpx;min-height:80rpx;padding:10rpx 16rpx}.excluded{padding:16rpx 0}.excluded-list{display:flex;gap:8rpx;flex-wrap:wrap;padding-top:12rpx}.archive-details{margin-top:16rpx;padding-top:20rpx;border-top:1px dashed var(--line)}.archive-details .hint{font-size:21rpx}.archive-details .button{font-size:23rpx}
.palette-credit{display:block;margin-top:16rpx;padding-top:14rpx;border-top:1px solid var(--line);font-size:19rpx;color:#7c6c59;line-height:1.6;word-break:break-word}.progress-note{display:flex;align-items:center;gap:14rpx;margin-top:20rpx;padding:20rpx 24rpx;background:#fff0c8;border-radius:18rpx;color:#76531f;font-size:25rpx}.progress-note .status-dot{background:#dc9938}.progress-note .hint{font-size:21rpx}.error-panel{display:flex;flex-direction:column;gap:16rpx;margin-top:20rpx;padding:22rpx;border:1px solid #ecc7b9;background:#fff1e9;border-radius:20rpx;font-size:25rpx;color:#9a4531}.error{display:block;font-size:22rpx;color:#9a4531;line-height:1.6}
.save-dock{position:fixed;z-index:10;left:0;right:0;bottom:0;padding:18rpx 28rpx calc(18rpx + env(safe-area-inset-bottom));box-sizing:border-box;border-top:1px solid #edd8af;background:#fffdf7}.save-dock-inner{display:flex;align-items:center;gap:24rpx;width:100%;max-width:900px;margin:0 auto}.save-summary{display:flex;flex-direction:column;gap:3rpx;min-width:0;flex-shrink:0}.save-summary-title{font-size:25rpx;color:#69502f;font-weight:650}.save-main{flex:1;min-width:0;min-height:92rpx;font-size:28rpx;padding:0 18rpx;border-radius:24rpx}.offscreen{position:fixed;left:-10000px;top:-10000px;width:1px;height:1px;pointer-events:none}
@media(min-width:700px){.beads-page{max-width:960px;margin:0 auto;padding:24px 28px 40px}.beads-page.has-pattern{padding-bottom:calc(80px + env(safe-area-inset-bottom))}.intro{padding-top:4px}.workspace-compact{padding:16px}.workspace-compact .source-options{justify-content:flex-start}.workspace-compact .brand-setting{max-width:260px}.workspace-compact .background-row{margin-left:20px}.welcome{max-width:380px;margin:0 auto}.welcome .choose{max-width:300px;margin-left:auto;margin-right:auto}.source-options{max-width:520px;margin-left:auto;margin-right:auto}.workspace-compact .source-options{max-width:none}.save-dock{padding-left:28px;padding-right:28px}.save-dock-inner{gap:32px}.save-main{max-width:360px;margin-left:auto}.save-summary{min-width:200px}}
@media(min-width:700px){.title{font-size:26px}.compact .title{font-size:22px}.subtitle{font-size:14px}.section-title,.section-heading .section-title{font-size:18px}.hint,.label{font-size:13px}.photo-copy .section-title{font-size:16px}.photo-copy .hint{font-size:12px}.photo,.photo-placeholder{width:56px;height:60px}.button,.chip{font-size:14px;min-height:44px;padding:0 16px}.workspace-compact .picker-value{font-size:13px;min-height:40px}.workspace-compact .background-row .label{font-size:13px}.edit-toggle{min-height:40px;min-width:64px}.zoom-actions .button{min-height:42px}.square{min-width:42px}.zoom-value,.code-toggle .label{font-size:13px}.bead-mark{width:32px;height:32px;gap:4px}.bead-mark>view{width:14px;height:14px;border-radius:4px}.bead-mark>view::after{width:4px;height:4px;left:5px;top:5px}.disclosure-copy .section-title{font-size:16px}.disclosure-copy .hint,.disclosure-action,.material-name,.save-summary-title{font-size:13px}.swatch{width:24px;height:24px;border-radius:6px}.swatch::after{width:6px;height:6px;left:9px;top:9px}.save-dock{padding-top:12px;padding-bottom:calc(12px + env(safe-area-inset-bottom))}.save-main{min-height:48px;font-size:16px;border-radius:12px}}
@media(min-width:700px){.workspace,.canvas-scroll,.editor-panel,.export-panel,.materials-section,.more-section{border-radius:14px}.button,.chip,.zoom-actions,.welcome-board{border-radius:12px}}
@media(prefers-reduced-motion:reduce){.button{transition:none}.button:not([disabled]):active,.chip:not([disabled]):active{transform:none}}
</style>
