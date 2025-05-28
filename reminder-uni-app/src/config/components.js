/**
 * 组件按需注册配置
 * 定义各个页面需要使用的组件，实现按需加载
 */

// 组件映射表
export const componentMap = {
  // 日期时间选择器
  'DateTimePicker': () => import('@/components/datetime-picker/datetime-picker.vue'),
  
  // Cron表达式选择器
  'CronExpressionPicker': () => import('@/components/CronExpressionPicker.vue'),
  
  // 触发预览组件
  'TriggerPreview': () => import('@/components/trigger-preview/trigger-preview.vue'),
  
  // 统一时间选择器
  'UnifiedTimePicker': () => import('@/components/unified-time-picker/unified-time-picker.vue'),
  
  // 日历组件
  'VCalendar': () => import('@/components/v-calendar/v-calendar.vue'),
  
  // 确认对话框
  'ConfirmDialog': () => import('@/components/ConfirmDialog.vue'),
  
  // 确认模态框
  'ConfirmModal': () => import('@/components/ConfirmModal.vue'),
  
  // 登录表单
  'LoginForm': () => import('@/components/LoginForm.vue')
};

// 页面组件配置
export const pageComponentsConfig = {
  // 首页
  'pages/index/index': [],
  
  // 创建简单提醒页面
  'pages/create/create': [
    'DateTimePicker',
    'ConfirmDialog'
  ],
  
  // 创建复杂提醒页面
  'pages/create-complex/create-complex': [
    'CronExpressionPicker',
    'DateTimePicker', 
    'TriggerPreview'
  ],
  
  // 日历页面
  'pages/calendar/calendar': [
    'VCalendar',
    'DateTimePicker',
    'ConfirmDialog'
  ],
  
  // 详情页面
  'pages/detail/detail': [
    'DateTimePicker',
    'ConfirmDialog'
  ],
  
  // 登录页面
  'pages/login/login': [
    'LoginForm'
  ],
  
  // 注册页面
  'pages/register/register': [
    'LoginForm'
  ],
  
  // 我的页面
  'pages/mine/mine': [
    'ConfirmDialog'
  ],
  
  // 个人资料编辑页面
  'pages/profile/edit': [
    'ConfirmDialog'
  ],
  
  // Cron测试页面
  'pages/test-cron/test-cron': [
    'CronExpressionPicker',
    'TriggerPreview'
  ]
};

/**
 * 获取页面需要的组件
 * @param {string} pagePath 页面路径
 * @returns {Array} 组件名称数组
 */
export function getPageComponents(pagePath) {
  return pageComponentsConfig[pagePath] || [];
}

/**
 * 动态注册组件
 * @param {Object} app Vue应用实例
 * @param {Array} componentNames 组件名称数组
 */
export async function registerComponents(app, componentNames) {
  const registrationPromises = componentNames.map(async (componentName) => {
    if (componentMap[componentName]) {
      try {
        const component = await componentMap[componentName]();
        app.component(componentName, component.default || component);
        console.log(`✅ 组件 ${componentName} 注册成功`);
      } catch (error) {
        console.error(`❌ 组件 ${componentName} 注册失败:`, error);
      }
    } else {
      console.warn(`⚠️ 未找到组件 ${componentName} 的配置`);
    }
  });
  
  await Promise.all(registrationPromises);
}

/**
 * 为页面注册所需组件
 * @param {Object} app Vue应用实例
 * @param {string} pagePath 页面路径
 */
export async function registerPageComponents(app, pagePath) {
  const componentNames = getPageComponents(pagePath);
  if (componentNames.length > 0) {
    console.log(`🔧 为页面 ${pagePath} 注册组件:`, componentNames);
    await registerComponents(app, componentNames);
  }
}

export default {
  componentMap,
  pageComponentsConfig,
  getPageComponents,
  registerComponents,
  registerPageComponents
}; 