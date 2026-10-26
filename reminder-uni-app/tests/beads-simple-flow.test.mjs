import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import vm from 'node:vm';
const require = createRequire(new URL('../package.json', import.meta.url));
const { ref, shallowRef, computed, watch, nextTick } = require('vue');
const source = readFileSync(new URL('../src/pages/beads/index.vue', import.meta.url), 'utf8');
const script = source.match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import .*;\n/gm, '');
const brand = {id:'test',name:'测试品牌',credit:'credit',colors:[]};
const original = {width:1,height:1,cells:[0],palette:[{code:'R',hex:'#FF0000',rgb:[255,0,0]}]};
function page(t, draft, native = false) {
  const writes=[],calls=[],evaluations=[],dialogs=[],copies=[],removed=[];let unload;
  const values=new Map(draft?[['beads-draft-v1',draft]]:[]);
  const context=vm.createContext({setTimeout,clearTimeout,ref,shallowRef,computed,watch,nextTick,getCurrentInstance:()=>({proxy:{}}),onReady(){},onUnload(fn){unload=fn;},BEAD_PALETTES:[brand],assessImage:image=>{evaluations.push(image);return {columns:96,maxColors:24,cleanup:2,mode:'illustration',background:'keep',reason:'图片细节较多'};},countMaterials:p=>p.palette.map((c,i)=>({...c,index:i,count:p.cells.filter(j=>j===i).length})),getCanvasSurface:async()=>({canvas:{toDataURL:()=> 'data:image/png;base64,test'}}),decodePhoto:async()=>({width:1,height:1,data:new Uint8ClampedArray([255,0,0,255])}),convertToPattern:(_image,settings)=>{calls.push(settings);return structuredClone(original);},drawPattern(){},surfaceFile:async()=>'/tmp/preview.png',uni:{getStorageSync:key=>values.get(key)||'',setStorageSync:(key,value)=>{values.set(key,value);writes.push({key,value});},getSystemInfoSync:()=>({windowWidth:390}),showToast(){},showModal(options){dialogs.push(options);options.success({confirm:false});},previewImage(){}}});
  if (native) { context.uni.env={USER_DATA_PATH:'/data'};context.uni.getFileSystemManager=()=>({copyFile:options=>copies.push(options),unlink:options=>removed.push(options.filePath)}); }
  let executable=script;
  if (native) {
    let keep=true;const stack=[];
    executable=script.split('\n').filter(line=>{
      const directive=line.match(/^\s*\/\/ #(ifdef|ifndef) H5/);
      if(directive){stack.push(keep);keep=keep&&directive[1]==='ifndef';return false;}
      if(/^\s*\/\/ #endif/.test(line)){keep=stack.pop()??true;return false;}
      return keep;
    }).join('\n');
  }
  vm.runInContext(executable,context);t.after(()=>unload?.());
  return {run:code=>vm.runInContext(code,context),writes,calls,evaluations,dialogs,copies,removed};
}
const pause=ms=>new Promise(resolve=>setTimeout(resolve,ms));
test('selection assesses locally once, applies settings and generates without an extra action',async t=>{
  const ui=page(t);await ui.run("loadPhoto('cat.jpg')");
  assert.equal(ui.evaluations.length,1);assert.equal(ui.calls.length,1);
  assert.equal(ui.calls[0].columns,96);assert.equal(ui.calls[0].maxColors,24);assert.equal(ui.calls[0].mode,'illustration');
  assert.equal(ui.run('editing.value'),false);
});
test('preview clicks cannot mutate cells until editing is enabled',async t=>{
  const ui=page(t);ui.run(`pattern.value=${JSON.stringify(original)}`);
  await ui.run('paintAt({clientX:1,clientY:1})');assert.equal(ui.run('pattern.value.cells[0]'),0);assert.equal(ui.run('history.value.length'),0);
});
test('settings regenerate automatically without re-assessing or pressing generate',async t=>{
  const ui=page(t);await ui.run("loadPhoto('cat.jpg')");ui.run("cleanup.value=0");await pause(600);
  assert.equal(ui.calls.length,2);assert.equal(ui.calls[1].cleanup,0);assert.equal(ui.evaluations.length,1);
});
test('successful generation automatically saves a local draft',async t=>{
  const ui=page(t);await ui.run("loadPhoto('cat.jpg')");await pause(600);
  const saved=ui.writes.filter(x=>x.key==='beads-draft-v1').at(-1)?.value;
  assert.ok(saved);assert.equal(ui.calls.length,1);assert.equal(saved.columns,96);assert.equal(saved.maxColors,24);assert.deepEqual(saved.pattern.cells,[0]);
});
test('canceling automatic redraw restores settings and preserves hand edits',async t=>{
  const ui=page(t);await ui.run("loadPhoto('cat.jpg')");ui.run('hasManualEdits.value=true; pattern.value={...pattern.value,cells:[-1]}; columns.value=64');await pause(600);
  assert.equal(ui.dialogs.length,1);assert.equal(ui.calls.length,1);assert.equal(ui.run('columns.value'),96);assert.equal(ui.run('pattern.value.cells[0]'),-1);
});
test('restoring an old draft does not evaluate or silently redraw it',async t=>{
  const settings={columns:48,maxColors:16,brandId:'test',excludedCodes:[],mode:'photo',cleanup:0,background:'keep'};
  const ui=page(t,{version:1,pattern:original,source:'',...settings,generatedSettings:JSON.stringify(settings)});
  await ui.run('restoreDraft()');await pause(400);
  assert.equal(ui.calls.length,0);assert.equal(ui.evaluations.length,0);assert.equal(ui.run('mode.value'),'photo');assert.equal(ui.run('editing.value'),false);
});

test('native autosave copies the original independently of a hanging upload',async t=>{
  const ui=page(t,undefined,true);await ui.run("loadPhoto('cat.jpg')");ui.run('uploadPromise=new Promise(()=>{}); null');await pause(400);
  assert.equal(ui.copies.length,1);
  assert.ok(ui.writes.some(x=>x.key==='beads-draft-v1'));
  assert.equal(ui.run('sourcePath.value'),'cat.jpg');
  ui.copies[0].success();await pause(30);
  const saved=ui.writes.filter(x=>x.key==='beads-draft-v1').at(-1).value;
  assert.ok(saved.source.startsWith('/data/beads-original-'));assert.equal(ui.run('sourcePath.value'),'cat.jpg');
});
test('an edit during native source copying is saved after the copy without reverting cells',async t=>{
  const ui=page(t,undefined,true);await ui.run("loadPhoto('cat.jpg')");await pause(400);
  ui.run('hasManualEdits.value=true; pattern.value={...pattern.value,cells:[-1]}');
  ui.copies[0].success();await pause(400);
  const saved=ui.writes.filter(x=>x.key==='beads-draft-v1').at(-1).value;
  assert.equal(saved.pattern.cells[0],-1);assert.equal(saved.edited,true);assert.ok(saved.source.startsWith('/data/beads-original-'));
});
test('storage failure stays visible instead of claiming an automatic save',async t=>{
  const ui=page(t);ui.run('uni.setStorageSync=()=>{throw new Error("quota")}');await ui.run("loadPhoto('cat.jpg')");await pause(400);
  assert.equal(ui.run('draftFailed.value'),true);assert.match(ui.run('draftNotice.value'),/失败/);assert.equal(ui.writes.length,0);
});

test('saving during a pending settings change keeps metadata matched to rendered cells',async t=>{
  const ui=page(t);await ui.run("loadPhoto('cat.jpg')");ui.run('columns.value=64; flushDraft()');
  const saved=ui.writes.filter(x=>x.key==='beads-draft-v1').at(-1).value;
  assert.equal(saved.columns,96);assert.equal(saved.columns,JSON.parse(saved.generatedSettings).columns);
});

test('reverting pending settings cancels redraw without asking to discard edits',async t=>{
  const ui=page(t);await ui.run("loadPhoto('cat.jpg')");
  ui.run('hasManualEdits.value=true; columns.value=64');await nextTick();
  ui.run('columns.value=96');await pause(400);
  assert.equal(ui.calls.length,1);assert.equal(ui.dialogs.length,0);
});
test('a draft without its source rejects brand and exclusion changes while remaining editable',async t=>{
  const settings={columns:48,maxColors:16,brandId:'test',excludedCodes:[],mode:'photo',cleanup:0,background:'keep'};
  const ui=page(t,{version:1,pattern:original,source:'',...settings,generatedSettings:JSON.stringify(settings)});
  await ui.run('restoreDraft()');ui.run("changeBrand({detail:{value:1}}); toggleExcluded('R')");
  assert.equal(ui.run('settingsChanged.value'),false);assert.equal(ui.run('brandIndex.value'),0);
  assert.equal(ui.run('excludedCodes.value.length'),0);assert.equal(ui.run('locked.value'),false);
});
