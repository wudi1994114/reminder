// Vue 组合式 API 按需导入
export {
  ref,
  reactive,
  computed,
  watch,
  watchEffect,
  onMounted,
  onUnmounted,
  onBeforeMount,
  onBeforeUnmount,
  onUpdated,
  onBeforeUpdate,
  nextTick,
  defineComponent,
  defineProps,
  defineEmits,
  defineExpose,
  createApp,
  readonly
} from 'vue'

// FullCalendar 按需注入函数
export const useFullCalendar = () => import('@fullcalendar/vue3').then(module => module.FullCalendar);
export const useDayGridPlugin = () => import('@fullcalendar/daygrid').then(module => module.default);
export const useTimeGridPlugin = () => import('@fullcalendar/timegrid').then(module => module.default);
export const useInteractionPlugin = () => import('@fullcalendar/interaction').then(module => module.default);

// 按需注入的FullCalendar组合
export const useFullCalendarPlugins = async () => {
  const [FullCalendar, dayGridPlugin, timeGridPlugin, interactionPlugin] = await Promise.all([
    useFullCalendar(),
    useDayGridPlugin(),
    useTimeGridPlugin(),
    useInteractionPlugin()
  ]);
  
  return {
    FullCalendar,
    dayGridPlugin,
    timeGridPlugin,
    interactionPlugin
  };
};

// 工具库按需注入
export const useCronParser = () => import('cron-parser').then(module => ({ parseExpression: module.parseExpression }));
export const useCronstrue = () => import('cronstrue').then(module => module.default);

// 农历相关按需注入
export const useLunarLibrary = () => import('lunar-typescript').then(module => ({
  Solar: module.Solar,
  Lunar: module.Lunar,
  HolidayUtil: module.HolidayUtil,
  SolarUtil: module.SolarUtil,
  SolarTerm: module.SolarTerm
}));

// 创建一个延迟加载函数，用于动态导入大型库
export const lazyImport = (importFn) => {
  let modulePromise = null
  
  return (...args) => {
    if (!modulePromise) {
      modulePromise = importFn()
    }
    return modulePromise.then(module => {
      const defaultExport = module.default || module
      if (typeof defaultExport === 'function') {
        return defaultExport(...args)
      }
      return defaultExport
    })
  }
}

// 延迟加载的工具函数示例
export const lazyLoadCronParser = lazyImport(() => import('cron-parser'))
export const lazyLoadCronstrue = lazyImport(() => import('cronstrue'))
export const lazyLoadLunar = lazyImport(() => import('lunar-typescript'))

// 按需注入的组合式函数
export const useAsyncComponent = (componentName, loader) => {
  const component = ref(null);
  const loading = ref(false);
  const error = ref(null);
  
  const loadComponent = async () => {
    if (component.value) return component.value;
    
    loading.value = true;
    error.value = null;
    
    try {
      console.log(`🔄 按需加载组件: ${componentName}`);
      const module = await loader();
      component.value = module.default || module;
      console.log(`✅ 组件 ${componentName} 加载成功`);
      return component.value;
    } catch (err) {
      error.value = err;
      console.error(`❌ 组件 ${componentName} 加载失败:`, err);
      throw err;
    } finally {
      loading.value = false;
    }
  };
  
  return {
    component: readonly(component),
    loading: readonly(loading),
    error: readonly(error),
    loadComponent
  };
};

// 按需注入的资源管理器
export class LazyResourceManager {
  constructor() {
    this.resources = new Map();
    this.loadingPromises = new Map();
  }
  
  // 注册资源
  register(name, loader) {
    this.resources.set(name, loader);
  }
  
  // 加载资源
  async load(name) {
    if (this.loadingPromises.has(name)) {
      return this.loadingPromises.get(name);
    }
    
    const loader = this.resources.get(name);
    if (!loader) {
      throw new Error(`资源 ${name} 未注册`);
    }
    
    console.log(`🔄 按需加载资源: ${name}`);
    const promise = loader().then(module => {
      console.log(`✅ 资源 ${name} 加载成功`);
      return module.default || module;
    }).catch(error => {
      console.error(`❌ 资源 ${name} 加载失败:`, error);
      this.loadingPromises.delete(name);
      throw error;
    });
    
    this.loadingPromises.set(name, promise);
    return promise;
  }
  
  // 批量加载资源
  async loadMultiple(names) {
    const promises = names.map(name => this.load(name));
    return Promise.all(promises);
  }
  
  // 预加载资源
  async preload(names) {
    console.log('🚀 开始预加载资源:', names);
    try {
      await this.loadMultiple(names);
      console.log('✅ 资源预加载完成');
    } catch (error) {
      console.error('❌ 资源预加载失败:', error);
    }
  }
  
  // 清除缓存
  clearCache() {
    this.loadingPromises.clear();
    console.log('🧹 资源缓存已清除');
  }
}

// 全局资源管理器实例
export const resourceManager = new LazyResourceManager();

// 注册常用资源
resourceManager.register('fullcalendar', () => import('@fullcalendar/vue3'));
resourceManager.register('daygrid', () => import('@fullcalendar/daygrid'));
resourceManager.register('timegrid', () => import('@fullcalendar/timegrid'));
resourceManager.register('interaction', () => import('@fullcalendar/interaction'));
resourceManager.register('cronParser', () => import('cron-parser'));
resourceManager.register('cronstrue', () => import('cronstrue'));
resourceManager.register('lunar', () => import('lunar-typescript')); 