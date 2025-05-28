/**
 * 页面基类混入
 * 提供组件按需加载和通用页面功能
 */

import { registerPageComponents } from '@/config/components.js';

export default {
  data() {
    return {
      // 组件加载状态
      componentsLoaded: false,
      componentsLoading: false
    };
  },
  
  async onLoad(options) {
    console.log('📄 页面 onLoad:', this.$mp?.page?.route || 'unknown');
    
    // 注册页面组件
    await this.loadPageComponents();
    
    // 调用子类的 onLoad 方法
    if (this.onPageLoad) {
      await this.onPageLoad(options);
    }
  },
  
  methods: {
    /**
     * 加载页面所需组件
     */
    async loadPageComponents() {
      if (this.componentsLoaded || this.componentsLoading) {
        return;
      }
      
      this.componentsLoading = true;
      
      try {
        // 获取当前页面路径
        const pages = getCurrentPages();
        const currentPage = pages[pages.length - 1];
        const pagePath = currentPage.route;
        
        console.log(`🔧 开始加载页面 ${pagePath} 的组件...`);
        
        // 注册组件
        await registerPageComponents(this.$app || getApp(), pagePath);
        
        this.componentsLoaded = true;
        console.log(`✅ 页面 ${pagePath} 组件加载完成`);
        
        // 触发组件加载完成事件
        this.onComponentsLoaded && this.onComponentsLoaded();
        
      } catch (error) {
        console.error('❌ 页面组件加载失败:', error);
        
        // 触发组件加载失败事件
        this.onComponentsLoadError && this.onComponentsLoadError(error);
        
      } finally {
        this.componentsLoading = false;
      }
    },
    
    /**
     * 手动注册单个组件
     * @param {string} componentName 组件名称
     */
    async registerComponent(componentName) {
      try {
        const { registerComponents } = await import('@/config/components.js');
        await registerComponents(this.$app || getApp(), [componentName]);
        console.log(`✅ 手动注册组件 ${componentName} 成功`);
      } catch (error) {
        console.error(`❌ 手动注册组件 ${componentName} 失败:`, error);
      }
    },
    
    /**
     * 检查组件是否已注册
     * @param {string} componentName 组件名称
     * @returns {boolean}
     */
    isComponentRegistered(componentName) {
      const app = this.$app || getApp();
      return !!(app && app.component && app.component(componentName));
    },
    
    /**
     * 等待组件加载完成
     * @returns {Promise}
     */
    waitForComponents() {
      return new Promise((resolve) => {
        if (this.componentsLoaded) {
          resolve();
        } else {
          const checkLoaded = () => {
            if (this.componentsLoaded) {
              resolve();
            } else {
              setTimeout(checkLoaded, 50);
            }
          };
          checkLoaded();
        }
      });
    }
  },
  
  // 生命周期钩子 - 可被子类重写
  onComponentsLoaded() {
    // 组件加载完成后的回调
    console.log('🎉 页面组件全部加载完成');
  },
  
  onComponentsLoadError(error) {
    // 组件加载失败后的回调
    console.error('💥 页面组件加载失败:', error);
    
    // 显示错误提示
    uni.showToast({
      title: '页面组件加载失败',
      icon: 'none',
      duration: 2000
    });
  }
}; 