import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    uni(),
  ],
  // 路径别名配置
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '@api': resolve(__dirname, 'src/api'),
      '@store': resolve(__dirname, 'src/store'),
      '@utils': resolve(__dirname, 'src/utils'),
      '@components': resolve(__dirname, 'src/components'),
      '@pages': resolve(__dirname, 'src/pages'),
      '@static': resolve(__dirname, 'src/static'),
      '@styles': resolve(__dirname, 'src/styles'),
      '@types': resolve(__dirname, 'src/types'),
      '@config': resolve(__dirname, 'src/config'),
      '@plugins': resolve(__dirname, 'src/plugins'),
      '@mixins': resolve(__dirname, 'src/mixins')
    }
  },
  // 开发服务器配置
  server: {
    port: 3000,
    open: true,
    cors: true
  },
  // 构建配置
  build: {
    target: 'es2015',
    minify: 'terser',
    // 代码分割优化
    rollupOptions: {
      output: {
        // 更细粒度的代码分割
        manualChunks: {
          // Vue核心
          'vue-vendor': ['vue'],
          
          // uni-app核心
          'uni-vendor': ['@dcloudio/uni-app'],
          
          // 第三方库
          'vendor-utils': ['axios', 'cron-parser', 'cronstrue'],
          
          // 日期相关库
          'vendor-date': ['lunar-typescript'],
          
          // 组件按类型分割
          'components-picker': [
            './src/components/datetime-picker/datetime-picker.vue',
            './src/components/unified-time-picker/unified-time-picker.vue'
          ],
          
          'components-cron': [
            './src/components/CronExpressionPicker.vue',
            './src/components/trigger-preview/trigger-preview.vue'
          ],
          
          'components-calendar': [
            './src/components/v-calendar/v-calendar.vue'
          ],
          
          'components-dialog': [
            './src/components/ConfirmDialog.vue',
            './src/components/ConfirmModal.vue'
          ],
          
          'components-form': [
            './src/components/LoginForm.vue'
          ]
        },
        
        // 动态导入的chunk命名
        chunkFileNames: (chunkInfo) => {
          const facadeModuleId = chunkInfo.facadeModuleId;
          
          if (facadeModuleId) {
            // 组件chunk
            if (facadeModuleId.includes('/components/')) {
              const componentName = facadeModuleId.split('/').pop().replace('.vue', '');
              return `components/[name]-${componentName}-[hash].js`;
            }
            
            // 页面chunk
            if (facadeModuleId.includes('/pages/')) {
              const pageName = facadeModuleId.split('/pages/')[1].split('/')[0];
              return `pages/[name]-${pageName}-[hash].js`;
            }
          }
          
          return 'chunks/[name]-[hash].js';
        }
      },
      
      // 压缩配置
      terserOptions: {
        compress: {
          // 移除console.log (生产环境)
          drop_console: process.env.NODE_ENV === 'production',
          drop_debugger: true,
          pure_funcs: process.env.NODE_ENV === 'production' ? ['console.log'] : []
        }
      }
    },
    
    // CSS 配置
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `@import "@/styles/variables.scss";`
        }
      }
    },
    
    // 优化配置
    optimizeDeps: {
      include: [
        'vue',
        '@dcloudio/uni-app',
        'axios'
      ],
      exclude: [
        // 排除大型组件，让它们按需加载
        '@/components/CronExpressionPicker.vue',
        '@/components/v-calendar/v-calendar.vue'
      ]
    },
    
    // 定义全局常量
    define: {
      __COMPONENT_LAZY_LOADING__: true,
      __DEV__: process.env.NODE_ENV === 'development'
    }
  }
})
