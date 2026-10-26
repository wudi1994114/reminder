import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import vm from 'node:vm';

const require = createRequire(new URL('../package.json', import.meta.url));
const vue = require('vue');
function nativeSource(source) {
  let keep = true; const stack = [];
  return source.split('\n').filter(line => {
    const directive = line.match(/^\s*\/\/ #(ifdef|ifndef) H5/);
    if (directive) { stack.push(keep); keep = keep && directive[1] === 'ifndef'; return false; }
    if (/^\s*\/\/ #endif/.test(line)) { keep = stack.pop() ?? true; return false; }
    return keep;
  }).join('\n');
}
const canvasCode = nativeSource(readFileSync(new URL('../src/utils/beads/canvas.js', import.meta.url), 'utf8')).replace(/export /g, '');
const canvasContext = vm.createContext({setTimeout, clearTimeout});
vm.runInContext(canvasCode, canvasContext);
test('native PNG export explicitly preserves the bitmap dimensions', async () => {
  let options;
  const canvas = { width:1652, height:1850 };
  const context = vm.createContext({ setTimeout, clearTimeout, uni:{canvasToTempFilePath(input) { options = input; input.success({tempFilePath:'/tmp/chart.png'}); }} });
  vm.runInContext(canvasCode, context);
  context.surface = {canvas};
  assert.equal(await vm.runInContext('surfaceFile(surface, {})', context), '/tmp/chart.png');
  assert.equal(options.width, canvas.width); assert.equal(options.height, canvas.height);
  assert.equal(options.destWidth, canvas.width); assert.equal(options.destHeight, canvas.height);
});

const source = readFileSync(new URL('../src/pages/beads/index.vue', import.meta.url), 'utf8');
const script = nativeSource(source.match(/<script setup>([\s\S]*?)<\/script>/)[1]).replace(/^import .*;\n/gm, '');
const pattern = {width:96,height:64,cells:Array(96 * 64).fill(0),palette:[{code:'A1',name:'黄',hex:'#FAF4C8',rgb:[250,244,200]}]};
function page(t, failAlbum, mode) {
  const events = [], previews = [], toasts = [], dialogs = [], drawn = [], qualities = [], previewRenders = []; let unload, saves = 0;
  const context = vm.createContext({
    ...vue, setTimeout, clearTimeout, getCurrentInstance:()=>({proxy:{}}), onReady(){}, onUnload(fn){unload=fn;},
    BEAD_PALETTES:[{id:'test',name:'测试',credit:'credit',colors:pattern.palette}],
    countMaterials:()=>[{...pattern.palette[0],count:96 * 64}],
    getCanvasSurface:async id=>({id}), drawPattern(){}, exportTiles:canvasContext.exportTiles, drawExport(_surface,_pattern,_materials,tile,_part,_total,_brand,_credit,quality){drawn.push(tile);qualities.push(quality);},
    surfaceFile:async surface=>{ if(surface.id==='bead-preview-render'){const path=`/tmp/preview-${previewRenders.length+1}.png`;previewRenders.push(path);return path;} const path=`/tmp/chart-${events.filter(e=>e.startsWith('render')).length + 1}.png`; events.push(`render:${path}`); return path; },
    saveExport:async path=>{ events.push(`album:${path}`); if (failAlbum && saves++ === 1) throw {errMsg:failAlbum}; },
    uni:{getStorageSync:()=>'',setStorageSync(){},getSystemInfoSync:()=>({windowWidth:390}),showToast:options=>toasts.push(options.title),showModal:options=>dialogs.push(options),previewImage:options=>previews.push(options),createSelectorQuery:()=>({in(){return this;},select(){return this;},boundingClientRect(callback){callback({left:0,top:0,width:350,height:420});return this;},exec(){}})},
  });
  vm.runInContext(script, context); context.sample = structuredClone(pattern);
  vm.runInContext('pattern.value = sample', context);
  if (mode) vm.runInContext(`exportMode.value = '${mode}'`, context);
  t.after(()=>unload?.());
  return {run:code=>vm.runInContext(code,context),events,previews,toasts,dialogs,drawn,qualities,previewRenders};
}
test('all chart pages remain available when saving a later page fails', async t => {
  const ui = page(t, 'saveImageToPhotosAlbum:fail system permission denied', 'tiles');
  await ui.run('exportPattern()');
  assert.equal(ui.events.filter(event=>event.startsWith('render')).length, 4);
  assert.equal(ui.run('exportPaths.value.length'), 4);
  assert.match(ui.run('saveNotice.value'), /已保存 1/);
  ui.run('previewExport()'); assert.equal(ui.previews[0].urls.length, 4);
  assert.equal(ui.run('exporting.value'), false);
});
test('retry continues after the last saved page and does not duplicate earlier pages', async t => {
  const ui = page(t, 'saveImageToPhotosAlbum:fail disk full', 'tiles');
  await ui.run('exportPattern()'); await ui.run('exportPattern()');
  assert.equal(ui.events.filter(event=>event.startsWith('render')).length, 4);
  assert.equal(ui.events.filter(event=>event==='album:/tmp/chart-1.png').length, 1);
  assert.equal(ui.run('savedPages.value'), 4);
  assert.equal(ui.toasts.at(-1), '导出成功');
});
test('album failure exposes its reason and never reports a completed save', async t => {
  const ui = page(t, 'saveImageToPhotosAlbum:fail disk full', 'tiles');
  await ui.run('exportPattern()');
  assert.match(ui.run('saveNotice.value'), /disk full/);
  assert.equal(ui.toasts.includes('导出成功'), false);
  assert.equal(ui.dialogs.length, 1);
  assert.match(ui.dialogs[0].content, /disk full/);
});
test('editing invalidates prepared images so preview cannot show an older chart', async t => {
  const ui = page(t); await ui.run('exportPattern()');
  ui.run('pattern.value={...pattern.value,cells:pattern.value.cells.map(()=>-1)}'); await vue.nextTick();
  assert.equal(ui.run('exportPaths.value.length'), 0);
});
test('default export saves the entire pattern as exactly one image', async t => {
  const ui = page(t); await ui.run('exportPattern()');
  assert.equal(ui.events.filter(event=>event.startsWith('album')).length, 1);
  assert.deepEqual(JSON.parse(JSON.stringify(ui.drawn)), [{x:0,y:0,width:96,height:64}]);
  assert.equal(ui.run('exportMode.value'), 'whole');
  assert.deepEqual(ui.toasts, ['导出成功']);
  assert.equal(ui.run('saveNotice.value'), '');
  assert.equal(ui.run('exportFailed.value'), false);
});
test('switching export modes discards old previews without changing pattern or settings', async t => {
  const ui = page(t), settings = ui.run('settingsKey()'); await ui.run('exportPattern()');
  ui.run("exportMode.value='tiles'"); await vue.nextTick();
  assert.equal(ui.run('exportPaths.value.length'), 0);
  await ui.run('exportPattern()'); assert.equal(ui.run('exportPaths.value.length'), 4);
  ui.run("exportMode.value='whole'"); await vue.nextTick();
  await ui.run('exportPattern()'); assert.equal(ui.run('exportPaths.value.length'), 1);
  assert.equal(ui.run('settingsKey()'), settings);
  assert.equal(ui.run('pattern.value.cells.every(cell=>cell===0)'), true);
});
test('a complete 128 by 128 chart fits the export canvas bounds', () => {
  const ctx = {measureText:text=>({width:text.length*7})};
  for (const name of ['fillRect','fillText','beginPath','moveTo','lineTo','stroke','setLineDash','strokeRect']) ctx[name]=()=>{};
  const surface = {canvas:{},ctx};
  const large = {...pattern,width:128,height:128,cells:Array(128*128).fill(0)};
  const materials = Array.from({length:32},()=>({...pattern.palette[0],count:512}));
  canvasContext.drawExport(surface,large,materials,{x:0,y:0,width:128,height:128},1,1,'测试色板','Color data credit');
  assert.ok(surface.canvas.width<=4096, `width ${surface.canvas.width}`);
  assert.ok(surface.canvas.height<=4096, `height ${surface.canvas.height}`);
});
test('the initial preview fits the entire grid with codes enabled', t => {
  const ui = page(t);
  for (const dimensions of [[64,85],[128,128],[32,128]]) {
    ui.run(`pattern.value={...sample,width:${dimensions[0]},height:${dimensions[1]}}`);
    assert.ok(ui.run('canvasWidth.value <= screenWidth.value + .01'));
    assert.ok(ui.run('canvasHeight.value <= 420.01'));
  }
});
test('zoom buttons enlarge and reduce the preview and full view restores its bounds', t => {
  const ui = page(t), width = ui.run('canvasWidth.value');
  ui.run('changeZoom(2)'); assert.equal(ui.run('canvasWidth.value'), width * 2);
  ui.run('changeZoom(.5)'); assert.equal(ui.run('canvasWidth.value'), width);
  ui.run('changeZoom(16); fitView()'); assert.equal(ui.run('canvasWidth.value'), width);
  assert.equal(ui.run('viewScrollLeft.value'), 0); assert.equal(ui.run('viewScrollTop.value'), 0);
});
test('pinching changes scale without painting and releases scrolling on touch end', async t => {
  const ui = page(t); ui.run('editing.value=true');
  ui.run('touchStart({touches:[{clientX:50,clientY:100},{clientX:150,clientY:100}]})');
  ui.run('touchMove({touches:[{clientX:0,clientY:100},{clientX:300,clientY:100}]})');
  assert.equal(ui.run('zoom.value'), 3);
  await ui.run('touchEnd({touches:[],changedTouches:[{clientX:300,clientY:100}]})');
  assert.equal(ui.run('pinching.value'), false); assert.equal(ui.run('history.value.length'), 0);
  assert.equal(ui.run('pattern.value.cells.every(cell=>cell===0)'), true);
});
test('high quality export is the default and changing it invalidates prepared images', async t => {
  const ui = page(t); await ui.run('exportPattern()'); assert.equal(ui.qualities[0], 'high');
  ui.run("exportQuality.value='standard'"); await vue.nextTick();
  assert.equal(ui.run('exportPaths.value.length'), 0);
  await ui.run('exportPattern()'); assert.equal(ui.qualities.at(-1), 'standard');
});
test('high quality draws more pixels and larger code lettering within canvas bounds', () => {
  function draw(quality) {
    let codeFont;
    const ctx={measureText:text=>({width:text.length*7})};
    for (const name of ['fillRect','beginPath','moveTo','lineTo','stroke','setLineDash','strokeRect']) ctx[name]=()=>{};
    ctx.fillText=text=>{if(text==='A1')codeFont=parseFloat(ctx.font);};
    const surface={canvas:{},ctx}, sample={...pattern,width:64,height:85,cells:Array(64*85).fill(0)};
    canvasContext.drawExport(surface,sample,Array.from({length:16},()=>({...pattern.palette[0],count:340})),{x:0,y:0,width:64,height:85},1,1,'测试','Color data credit',quality);
    return {...surface.canvas,codeFont};
  }
  const standard=draw('standard'), high=draw('high');
  assert.ok(high.width>standard.width); assert.ok(high.height>standard.height);
  assert.ok(high.codeFont>standard.codeFont); assert.ok(high.width<=4096&&high.height<=4096);
});
test('preview leaves room for zoom controls and the fixed save bar', async t => {
  const ui = page(t);
  ui.run(`pattern.value={...sample,width:64,height:85}; uni.createSelectorQuery=()=>({in(){return this;},select(){return this;},boundingClientRect(){return this;},selectViewport(){return this;},scrollOffset(){return this;},exec(callback){callback([{top:280},{height:44},{top:680},{scrollTop:0}]);}})`);
  await ui.run('measurePreview()');
  assert.ok(ui.run('canvasHeight.value + 280 + 44 + 20 <= 680'));
  ui.run('resizePreview({size:{windowWidth:320}})');
  assert.ok(ui.run('canvasWidth.value <= 280.01'));
});
test('native preview reuses its bitmap during zoom and refreshes code visibility', async t => {
  const ui=page(t);await vue.nextTick();await ui.run('render()');
  assert.match(ui.run('previewImagePath.value'), /^\/tmp\/preview-/);
  const initial=ui.previewRenders.length;
  ui.run('changeZoom(2)');await vue.nextTick();await ui.run('render()');
  assert.equal(ui.previewRenders.length,initial);
  ui.run('changeZoom(2)');await vue.nextTick();await ui.run('render()');
  assert.equal(ui.previewRenders.length,initial+1);
});
