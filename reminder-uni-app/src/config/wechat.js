/**
 * 微信小程序配置
 */
export default {
  // 订阅消息模板ID配置
  subscribeTemplates: {
    // 提醒通知模板ID - 需要在微信公众平台申请
    reminder: 'VXTd9P8DUPpM0aM-nLSHSkDC9KTUolBNhkAHV7UkqxQ',
    
    // 其他可能的模板ID
    // taskComplete: 'your-task-complete-template-id',
    // systemNotice: 'your-system-notice-template-id'
  },
  
  // 订阅消息相关配置
  subscribe: {
    // 默认提示文案
    defaultTitle: '开启微信提醒',
    defaultContent: '为了及时提醒您，需要获取微信消息推送权限，是否允许？',
    
    // 权限获取失败时的提示
    failureMessage: '未获取到微信提醒权限，请选择其他提醒方式',
    errorMessage: '微信提醒权限请求失败，请选择其他提醒方式',
    
    // 成功提示
    successMessage: '微信提醒权限开启成功'
  },
  
  // 开发环境配置
  development: {
    // 开发环境下是否启用订阅消息
    enableSubscribe: true,
    // 开发环境下的模拟模板ID
    mockTemplateId: 'mock-template-id-for-dev'
  }
}; 