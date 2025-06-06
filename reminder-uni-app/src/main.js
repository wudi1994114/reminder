import {
	createSSRApp
} from "vue";
import App from "./App.vue";
import cloudConfig from "./config/cloud.js";

// 初始化微信云服务
function initCloudService() {
	// #ifdef MP-WEIXIN
	if (wx.cloud) {
		wx.cloud.init({
			env: cloudConfig.env,
			traceUser: true
		});
		console.log('微信云服务初始化成功，环境ID:', cloudConfig.env);
	} else {
		console.warn('当前环境不支持微信云服务');
	}
	// #endif
}

export function createApp() {
	const app = createSSRApp(App);
	
	// 初始化云服务
	initCloudService();
	
	return {
		app,
	};
}
