/**
 * 微信云托管配置
 */
export default {
  // 云托管功能开关 - 强制启用
  enabled: true,
  
  // 云环境ID - 从您当前的API URL提取
  env: 'prod-3gel427g5936cfa7',
  
  // 服务名称 - 您需要在云托管控制台确认实际的服务名
  serviceName: 'bewangji',
  
  // WebSocket配置
  websocket: {
    enabled: true,
    path: '/ws' // WebSocket路径
  },
  
  // 性能优化配置
  performance: {
    // 是否排除用户身份信息（提升性能）
    excludeCredentials: false,
    // 请求超时时间（毫秒）
    timeout: 15000
  },
  
  // 调试配置
  debug: {
    // 是否开启详细日志
    verbose: true,
    // 是否在控制台显示请求详情
    showRequestDetails: true
  }
}; 