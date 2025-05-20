import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import uni from '@dcloudio/vite-plugin-uni'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  // 加载 .env 文件中的环境变量
  const env = loadEnv(mode, process.cwd(), '');

  // 设置明确的代理目标，改为后端运行的端口，一般是8080
  const proxyTarget = env.VITE_API_PROXY_TARGET || 'http://localhost';

  return {
    plugins: [
      vue(),
      uni()
    ],
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
