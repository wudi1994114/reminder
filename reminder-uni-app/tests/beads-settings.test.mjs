import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import vm from 'node:vm';

const require = createRequire(new URL('../package.json', import.meta.url));
const { ref, shallowRef, computed } = require('vue');
const source = readFileSync(new URL('../src/pages/beads/index.vue', import.meta.url), 'utf8');
const script = source.match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import .*;\n/gm, '');
const brand = { id:'test', name:'测试', colors:[] };
const pattern = { width:1, height:1, cells:[-1], palette:[] };
function page(draft) {
  const writes = [], conversions = [];
  const context = vm.createContext({ ref, shallowRef, computed, watch() {}, nextTick:async () => {}, getCurrentInstance:() => ({proxy:{}}), onReady() {}, onUnload() {}, BEAD_PALETTES:[brand], countMaterials:() => [], getCanvasSurface:async () => ({}), decodePhoto:async () => ({width:1,height:1,data:new Uint8ClampedArray(4)}), convertToPattern:(_rgba,settings) => { conversions.push(settings); return pattern; }, uni:{getStorageSync: key => key === 'beads-draft-v1' ? draft : '', getSystemInfoSync:() => ({windowWidth:375}), setStorageSync:(_key,value) => writes.push(value), showToast() {}} });
  vm.runInContext(script, context);
  return { run: code => vm.runInContext(code, context), writes, conversions };
}
const oldSettings = {columns:48,maxColors:16,brandId:'test',excludedCodes:[]};
const oldDraft = {version:1,pattern,source:'',...oldSettings,generatedSettings:JSON.stringify(oldSettings)};

test('new page defaults to illustration with light cleanup in generation settings', () => {
  const ui = page();
  const settings = JSON.parse(ui.run('settingsKey()'));
  assert.equal(settings.mode, 'illustration');
  assert.equal(settings.cleanup, 1);
});

test('legacy draft keeps photo rendering and no cleanup without a spurious regeneration notice', async () => {
  const ui = page(oldDraft);
  await ui.run('restoreDraft()');
  const settings = JSON.parse(ui.run('settingsKey()'));
  assert.equal(settings.mode, 'photo');
  assert.equal(settings.cleanup, 0);
  assert.equal(ui.run('settingsChanged.value'), false);
});

test('legacy draft preserves pending generation changes during key migration', async () => {
  const ui = page({...oldDraft,columns:64});
  await ui.run('restoreDraft()');
  assert.equal(ui.run('settingsChanged.value'), true);
});

test('mode and cleanup survive draft save and restore', async () => {
  const ui = page({...oldDraft,mode:'illustration',cleanup:2,generatedSettings:JSON.stringify({...oldSettings,mode:'illustration',cleanup:2})});
  await ui.run('restoreDraft()');
  assert.equal(ui.run('settingsChanged.value'), false);
  await ui.run('saveDraft()');
  assert.equal(ui.writes[0].mode, 'illustration');
  assert.equal(ui.writes[0].cleanup, 2);
  ui.run("mode.value = 'photo'");
  assert.equal(ui.run('settingsChanged.value'), true);
});

test('source-persistence fallback also saves conversion settings', async () => {
  const ui = page({...oldDraft,mode:'illustration',cleanup:2});
  await ui.run('restoreDraft()');
  ui.run("sourcePath.value = 'unavailable.png'");
  await ui.run('saveDraft()');
  assert.equal(ui.writes[0].source, '');
  assert.equal(ui.writes[0].mode, 'illustration');
  assert.equal(ui.writes[0].cleanup, 2);
});


test('conversion receives selected mode and cleanup and clears the changed-settings notice', async () => {
  const ui = page();
  ui.run("sourcePath.value = 'photo.png'; mode.value = 'photo'; cleanup.value = 2");
  await ui.run('convert(0)');
  assert.equal(ui.conversions[0].mode, 'photo');
  assert.equal(ui.conversions[0].cleanup, 2);
  assert.equal(ui.run('settingsChanged.value'), false);
  ui.run('cleanup.value = 0');
  assert.equal(ui.run('settingsChanged.value'), true);
});
