import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
// 尝试使用不同的导入方式
import * as uniModule from '@dcloudio/vite-plugin-uni'
const uni = uniModule.default || uniModule

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  // 加载 .env 文件中的环境变量
  const env = loadEnv(mode, process.cwd(), '');

  // 设置明确的代理目标，改为后端运行的端口，一般是8080
  const proxyTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8080';

  // 打印检查 uni 的类型
  console.log('uni type:', typeof uni);

  return {
    plugins: [
      vue(),
      typeof uni === 'function' ? uni() : uni
    ],
    // 优化构建配置，支持按需导入和Tree Shaking
    build: {
      // 启用Tree Shaking
      minify: 'terser',
      terserOptions: {
        compress: {
          // 移除console.log
          drop_console: mode === 'production',
          // 移除debugger
          drop_debugger: mode === 'production',
          // 移除未使用的代码
          pure_funcs: mode === 'production' ? ['console.log'] : []
        }
      },
      // 分包策略，优化按需加载
      rollupOptions: {
        output: {
          // 手动分包
          manualChunks: {
            // Vue核心
            'vue-vendor': ['vue'],
            // FullCalendar相关
            'calendar-vendor': [
              '@fullcalendar/vue3',
              '@fullcalendar/daygrid',
              '@fullcalendar/timegrid',
              '@fullcalendar/interaction'
            ],
            // HTTP请求库
            'http-vendor': ['axios'],
            // 工具库
            'utils-vendor': ['cron-parser', 'cronstrue', 'lunar-typescript']
          }
        }
      },
      // 设置chunk大小警告阈值
      chunkSizeWarningLimit: 1000
    },
    // 优化依赖预构建
    optimizeDeps: {
      // 明确包含需要预构建的依赖
      include: [
        'vue',
        'axios',
        '@fullcalendar/vue3',
        '@fullcalendar/daygrid',
        '@fullcalendar/timegrid',
        '@fullcalendar/interaction',
        'cron-parser',
        'cronstrue',
        'lunar-typescript'
      ],
      // 排除不需要预构建的依赖
      exclude: []
    },
    server: {
      host: '0.0.0.0', // 允许通过 IP 地址访问
      port: 5173,      // 前端开发服务器端口
      allowedHosts: ['wwmty.cn', 'localhost'], // 建议也允许 localhost
      proxy: {
        '/api': {
          target: proxyTarget, // 使用环境变量或默认值
          changeOrigin: true
          // rewrite: (path) => path.replace(/^\/api/, '') // 移除/api前缀
        }
      }
    }
  }
})
