import {
	createSSRApp
} from "vue";
import App from "./App.vue";
import ComponentLoader from "@/plugins/component-loader.js";

export function createApp() {
	const app = createSSRApp(App);
	
	// 安装组件按需加载插件
	app.use(ComponentLoader);
	
	return {
		app,
	};
}
