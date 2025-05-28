// 按需导入Vue核心函数
import { createApp } from './utils/imports.js'
import './style.css'
import App from './App.vue'

// 导入按需注入插件
import LazyLoader from './utils/lazy-loader.js'

const app = createApp(App);

// 安装按需注入插件，并配置预加载的关键组件
app.use(LazyLoader, {
  preload: [
    'CalendarDisplay',  // 主要日历组件
    'NotificationToast', // 通知组件
    'LoadingSpinner'    // 加载组件
  ]
});

app.mount('#app');
