import {
	createSSRApp
} from "vue";
import App from "./App.vue";

// #ifdef MP-WEIXIN
// 兼容旧基础库：部分版本缺少 getStaticSystemInfoSync
if (typeof wx !== 'undefined' && !wx.getStaticSystemInfoSync && wx.getSystemInfoSync) {
  wx.getStaticSystemInfoSync = function () {
    return wx.getSystemInfoSync();
  };
}
// #endif

export function createApp() {
	const app = createSSRApp(App);
	
	return {
		app,
	};
}
