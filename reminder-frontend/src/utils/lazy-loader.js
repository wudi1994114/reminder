/**
 * 按需注入加载器
 * 基于微信小程序按需注入理念实现的组件懒加载系统
 */

// 组件注册状态管理
const componentRegistry = new Map();
const loadingComponents = new Set();
const componentPromises = new Map();

/**
 * 组件配置映射表
 * 定义各个组件的懒加载配置
 */
export const componentMap = {
  // 日历相关组件
  'CalendarDisplay': () => import('../components/CalendarDisplay.vue'),
  'EventModal': () => import('../components/EventModal.vue'),
  'ReminderModal': () => import('../components/ReminderModal.vue'),
  'ComplexReminderModal': () => import('../components/ComplexReminderModal.vue'),
  
  // 表单组件
  'DateTimePicker': () => import('../components/DateTimePicker.vue'),
  'CronExpressionPicker': () => import('../components/CronExpressionPicker.vue'),
  
  // 通用组件
  'ConfirmDialog': () => import('../components/ConfirmDialog.vue'),
  'NotificationToast': () => import('../components/NotificationToast.vue'),
  'LoadingSpinner': () => import('../components/LoadingSpinner.vue'),
  
  // 第三方库组件
  'FullCalendar': () => import('@fullcalendar/vue3'),
  
  // 工具库
  'dayGridPlugin': () => import('@fullcalendar/daygrid'),
  'timeGridPlugin': () => import('@fullcalendar/timegrid'),
  'interactionPlugin': () => import('@fullcalendar/interaction'),
  'cronParser': () => import('cron-parser'),
  'cronstrue': () => import('cronstrue'),
  'lunarTypescript': () => import('lunar-typescript')
};

/**
 * 页面组件依赖配置
 * 定义每个页面需要的组件
 */
export const pageComponentsConfig = {
  // 主页面
  'App': [
    'CalendarDisplay',
    'EventModal', 
    'ReminderModal',
    'ComplexReminderModal',
    'ConfirmDialog',
    'NotificationToast',
    'LoadingSpinner'
  ],
  
  // 日历页面
  'Calendar': [
    'CalendarDisplay',
    'FullCalendar',
    'dayGridPlugin',
    'timeGridPlugin', 
    'interactionPlugin',
    'EventModal',
    'ReminderModal'
  ],
  
  // 创建提醒页面
  'CreateReminder': [
    'ReminderModal',
    'DateTimePicker',
    'ConfirmDialog'
  ],
  
  // 创建复杂提醒页面
  'CreateComplexReminder': [
    'ComplexReminderModal',
    'CronExpressionPicker',
    'DateTimePicker',
    'cronParser',
    'cronstrue',
    'ConfirmDialog'
  ]
};

/**
 * 创建占位组件
 * 在组件加载期间显示的占位内容
 */
export function createPlaceholderComponent(componentName, options = {}) {
  return {
    name: `${componentName}Placeholder`,
    template: `
      <div class="component-placeholder" :class="placeholderClass">
        <div class="placeholder-content">
          <div class="placeholder-spinner" v-if="showSpinner"></div>
          <div class="placeholder-text">{{ placeholderText }}</div>
        </div>
      </div>
    `,
    props: {
      showSpinner: {
        type: Boolean,
        default: true
      },
      placeholderText: {
        type: String,
        default: options.text || `正在加载 ${componentName}...`
      },
      placeholderClass: {
        type: String,
        default: options.className || 'default-placeholder'
      }
    }
  };
}

/**
 * 异步组件工厂函数
 * 创建支持按需注入的异步组件
 */
export function createAsyncComponent(componentName, options = {}) {
  const loader = componentMap[componentName];
  
  if (!loader) {
    console.warn(`组件 ${componentName} 未在 componentMap 中配置`);
    return null;
  }

  return {
    // 异步组件加载函数
    loader: async () => {
      // 避免重复加载
      if (componentPromises.has(componentName)) {
        return componentPromises.get(componentName);
      }

      // 标记为加载中
      loadingComponents.add(componentName);
      
      console.log(`🔄 开始按需注入组件: ${componentName}`);
      
      try {
        const componentPromise = loader();
        componentPromises.set(componentName, componentPromise);
        
        const module = await componentPromise;
        const component = module.default || module;
        
        // 标记为已注册
        componentRegistry.set(componentName, component);
        loadingComponents.delete(componentName);
        
        console.log(`✅ 组件 ${componentName} 注入成功`);
        
        return component;
      } catch (error) {
        loadingComponents.delete(componentName);
        componentPromises.delete(componentName);
        console.error(`❌ 组件 ${componentName} 注入失败:`, error);
        throw error;
      }
    },
    
    // 加载中显示的组件
    loadingComponent: createPlaceholderComponent(componentName, {
      text: options.loadingText || `正在加载 ${componentName}...`,
      className: options.loadingClass || 'loading-placeholder'
    }),
    
    // 加载失败显示的组件
    errorComponent: {
      name: `${componentName}Error`,
      template: `
        <div class="component-error">
          <div class="error-content">
            <div class="error-icon">⚠️</div>
            <div class="error-text">组件 ${componentName} 加载失败</div>
            <button @click="retry" class="retry-button">重试</button>
          </div>
        </div>
      `,
      methods: {
        retry() {
          // 清除缓存，重新加载
          componentPromises.delete(componentName);
          componentRegistry.delete(componentName);
          this.$forceUpdate();
        }
      }
    },
    
    // 延迟时间（毫秒）
    delay: options.delay || 200,
    
    // 超时时间（毫秒）
    timeout: options.timeout || 10000
  };
}

/**
 * 批量注册页面组件
 * 为指定页面注册所需的所有组件
 */
export async function registerPageComponents(app, pageName) {
  const componentNames = pageComponentsConfig[pageName];
  
  if (!componentNames || componentNames.length === 0) {
    console.log(`页面 ${pageName} 无需注册组件`);
    return;
  }
  
  console.log(`🔧 开始为页面 ${pageName} 注册组件:`, componentNames);
  
  const registrationPromises = componentNames.map(async (componentName) => {
    // 跳过已注册的组件
    if (componentRegistry.has(componentName)) {
      console.log(`组件 ${componentName} 已注册，跳过`);
      return;
    }
    
    try {
      const asyncComponent = createAsyncComponent(componentName);
      if (asyncComponent) {
        app.component(componentName, asyncComponent);
        console.log(`✅ 异步组件 ${componentName} 注册成功`);
      }
    } catch (error) {
      console.error(`❌ 组件 ${componentName} 注册失败:`, error);
    }
  });
  
  await Promise.all(registrationPromises);
  console.log(`✅ 页面 ${pageName} 组件注册完成`);
}

/**
 * 预加载关键组件
 * 在应用启动时预加载一些关键组件
 */
export async function preloadCriticalComponents(componentNames = []) {
  console.log('🚀 开始预加载关键组件:', componentNames);
  
  const preloadPromises = componentNames.map(async (componentName) => {
    const loader = componentMap[componentName];
    if (loader && !componentRegistry.has(componentName)) {
      try {
        const module = await loader();
        const component = module.default || module;
        componentRegistry.set(componentName, component);
        console.log(`✅ 预加载组件 ${componentName} 成功`);
      } catch (error) {
        console.error(`❌ 预加载组件 ${componentName} 失败:`, error);
      }
    }
  });
  
  await Promise.all(preloadPromises);
  console.log('✅ 关键组件预加载完成');
}

/**
 * 检查组件是否已加载
 */
export function isComponentLoaded(componentName) {
  return componentRegistry.has(componentName);
}

/**
 * 检查组件是否正在加载
 */
export function isComponentLoading(componentName) {
  return loadingComponents.has(componentName);
}

/**
 * 获取已加载的组件列表
 */
export function getLoadedComponents() {
  return Array.from(componentRegistry.keys());
}

/**
 * 清除组件缓存（开发环境使用）
 */
export function clearComponentCache() {
  componentRegistry.clear();
  loadingComponents.clear();
  componentPromises.clear();
  console.log('🧹 组件缓存已清除');
}

/**
 * 按需注入插件
 */
export default {
  install(app, options = {}) {
    // 全局属性
    app.config.globalProperties.$registerPageComponents = (pageName) => 
      registerPageComponents(app, pageName);
    
    app.config.globalProperties.$isComponentLoaded = isComponentLoaded;
    app.config.globalProperties.$isComponentLoading = isComponentLoading;
    app.config.globalProperties.$getLoadedComponents = getLoadedComponents;
    
    // 开发环境功能
    if (process.env.NODE_ENV === 'development') {
      app.config.globalProperties.$clearComponentCache = clearComponentCache;
    }
    
    // 预加载关键组件
    if (options.preload && options.preload.length > 0) {
      preloadCriticalComponents(options.preload);
    }
    
    console.log('🚀 按需注入插件已安装');
  }
}; 