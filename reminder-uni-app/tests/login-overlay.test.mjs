import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';

const require = createRequire(new URL('../package.json', import.meta.url));
const vueRequire = createRequire(require.resolve('vue/package.json'));
const { createSSRApp } = require('vue');
const { renderToString } = vueRequire('@vue/server-renderer');
const { parse } = vueRequire('@vue/compiler-sfc');
const { compile } = vueRequire('@vue/compiler-ssr');
const source = readFileSync(new URL('../src/components/OneClickLogin.vue', import.meta.url), 'utf8');
const template = parse(source).descriptor.template.content;
const ssrRender = new Function('require', compile(template).code)(vueRequire);

function render(visible, wechatLoading = false) {
  return renderToString(createSSRApp({
    props: ['visible'],
    setup: () => ({ wechatLoading, errorMsg: '', isWeChatMiniProgram: true,
      close() {}, handleMaskClick() {}, handleWechatLogin() {} }),
    ssrRender,
  }, { visible }));
}

test('closed login dialog removes the overlay instead of depending on hidden/display styles', async () => {
  const html = await render(false);
  assert.doesNotMatch(html, /login-modal|微信一键登录|登录提醒应用/);
});

test('explicitly opened dialog renders login action and its pending state', async () => {
  assert.match(await render(true), /微信一键登录/);
  assert.match(await render(true, true), /登录中/);
});
