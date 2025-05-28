/**
 * 组件按需加载插件
 * 在页面加载时动态注册所需组件
 */

import { registerPageComponents } from '@/config/components.js';

// 存储已注册的组件，避免重复注册
const registeredComponents = new Set();

/**
 * 组件加载器插件
 */
export default {
  install(app, options = {}) {
    // 全局属性：注册页面组件
    app.config.globalProperties.$registerPageComponents = async function(pagePath) {
      const normalizedPath = pagePath.replace(/^\//, ''); // 移除开头的斜杠
      
      // 避免重复注册
      if (registeredComponents.has(normalizedPath)) {
        console.log(`📦 页面 ${normalizedPath} 的组件已注册，跳过重复注册`);
        return;
      }
      
      try {
        await registerPageComponents(app, normalizedPath);
        registeredComponents.add(normalizedPath);
        console.log(`✅ 页面 ${normalizedPath} 组件注册完成`);
      } catch (error) {
        console.error(`❌ 页面 ${normalizedPath} 组件注册失败:`, error);
      }
    };
    
    // 全局属性：清除注册记录（用于开发环境热重载）
    app.config.globalProperties.$clearComponentRegistry = function() {
      registeredComponents.clear();
      console.log('🧹 组件注册记录已清除');
    };
    
    // 全局属性：获取已注册组件列表
    app.config.globalProperties.$getRegisteredComponents = function() {
      return Array.from(registeredComponents);
    };
    
    console.log('🚀 组件按需加载插件已安装');
  }
};

/**
 * 页面组件注册混入
 * 在页面的 onLoad 生命周期中自动注册组件
 */
export const componentLoaderMixin = {
  async onLoad() {
    // 获取当前页面路径
    const pages = getCurrentPages();
    const currentPage = pages[pages.length - 1];
    const pagePath = currentPage.route;
    
    console.log(`📄 页面加载: ${pagePath}`);
    
    // 注册页面所需组件
    if (this.$registerPageComponents) {
      await this.$registerPageComponents(pagePath);
    }
  }
};

/**
 * 手动注册页面组件的工具函数
 * @param {string} pagePath 页面路径
 */
export async function loadPageComponents(pagePath) {
  const app = getApp();
  if (app && app.$registerPageComponents) {
    await app.$registerPageComponents(pagePath);
  }
}

/**
 * 预加载关键页面组件
 * 在应用启动时预加载一些关键页面的组件
 */
export async function preloadCriticalComponents(app) {
  const criticalPages = [
    'pages/index/index',
    'pages/create/create',
    'pages/login/login'
  ];
  
  console.log('🔄 开始预加载关键页面组件...');
  
  for (const pagePath of criticalPages) {
    try {
      await registerPageComponents(app, pagePath);
      console.log(`✅ 预加载页面 ${pagePath} 组件完成`);
    } catch (error) {
      console.error(`❌ 预加载页面 ${pagePath} 组件失败:`, error);
    }
  }
  
  console.log('�� 关键页面组件预加载完成');
} 