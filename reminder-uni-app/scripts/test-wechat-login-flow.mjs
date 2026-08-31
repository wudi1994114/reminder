import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const loginEntrypoints = [
  'src/pages/mine/mine.vue',
  'src/components/GlobalLoginModal.vue',
  'src/pages/index/index.vue'
];

test('WeChat login entry points request a login code without requiring deprecated getUserInfo data', async () => {
  for (const relativePath of loginEntrypoints) {
    const source = await readFile(path.join(projectRoot, relativePath), 'utf8');

    assert.match(source, /@click="handleWechatLogin"/, `${relativePath} must use a click-driven login action`);
    assert.doesNotMatch(source, /open-type="getUserInfo"/, `${relativePath} must not use the deprecated getUserInfo button flow`);
    assert.doesNotMatch(source, /e\.detail\.userInfo/, `${relativePath} must not cancel login when userInfo is unavailable`);
    assert.match(source, /smartWechatLogin/, `${relativePath} must delegate code exchange to the shared WeChat login flow`);
  }
});
