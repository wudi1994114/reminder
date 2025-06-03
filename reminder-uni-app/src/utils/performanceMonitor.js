/**
 * 性能监控工具
 * 用于统计页面加载、数据请求、计算等操作的耗时
 */

class PerformanceMonitor {
  constructor() {
    this.timers = new Map();
    this.logs = [];
    this.isEnabled = true; // 可以通过环境变量控制是否启用
  }

  /**
   * 开始计时
   * @param {string} name - 计时器名称
   * @param {string} description - 操作描述
   */
  start(name, description = '') {
    if (!this.isEnabled) return;
    
    const startTime = Date.now();
    this.timers.set(name, {
      startTime,
      description,
      endTime: null,
      duration: null
    });
    
    console.log(`🔄 [性能监控] 开始: ${name} ${description ? `- ${description}` : ''}`);
  }

  /**
   * 结束计时
   * @param {string} name - 计时器名称
   * @returns {number} 耗时（毫秒）
   */
  end(name) {
    if (!this.isEnabled) return 0;
    
    const timer = this.timers.get(name);
    if (!timer) {
      console.warn(`⚠️ [性能监控] 计时器 "${name}" 不存在`);
      return 0;
    }

    const endTime = Date.now();
    const duration = endTime - timer.startTime;
    
    timer.endTime = endTime;
    timer.duration = duration;

    // 记录日志
    const logEntry = {
      name,
      description: timer.description,
      startTime: timer.startTime,
      endTime,
      duration,
      timestamp: new Date().toLocaleString()
    };
    
    this.logs.push(logEntry);

    // 根据耗时显示不同级别的日志
    const icon = duration > 1000 ? '🐌' : duration > 500 ? '⚠️' : '✅';
    console.log(`${icon} [性能监控] 完成: ${name} - 耗时: ${duration}ms ${timer.description ? `(${timer.description})` : ''}`);

    this.timers.delete(name);
    return duration;
  }

  /**
   * 记录即时性能指标
   * @param {string} name - 指标名称
   * @param {number} value - 指标值
   * @param {string} unit - 单位
   */
  record(name, value, unit = 'ms') {
    if (!this.isEnabled) return;
    
    const logEntry = {
      name,
      value,
      unit,
      timestamp: new Date().toLocaleString(),
      type: 'metric'
    };
    
    this.logs.push(logEntry);
    console.log(`📊 [性能监控] 指标: ${name} = ${value}${unit}`);
  }

  /**
   * 测量函数执行时间
   * @param {string} name - 操作名称
   * @param {Function} fn - 要测量的函数
   * @param {string} description - 操作描述
   * @returns {Promise<any>} 函数执行结果
   */
  async measure(name, fn, description = '') {
    if (!this.isEnabled) {
      return await fn();
    }
    
    this.start(name, description);
    try {
      const result = await fn();
      this.end(name);
      return result;
    } catch (error) {
      this.end(name);
      console.error(`❌ [性能监控] ${name} 执行出错:`, error);
      throw error;
    }
  }

  /**
   * 获取性能统计报告
   * @returns {Object} 统计报告
   */
  getReport() {
    const completedOperations = this.logs.filter(log => log.duration !== undefined);
    
    if (completedOperations.length === 0) {
      return {
        total: 0,
        slowest: null,
        fastest: null,
        average: 0,
        operations: []
      };
    }

    const durations = completedOperations.map(log => log.duration);
    const total = durations.reduce((sum, duration) => sum + duration, 0);
    const average = total / durations.length;
    
    const slowest = completedOperations.reduce((prev, current) => 
      (prev.duration > current.duration) ? prev : current
    );
    
    const fastest = completedOperations.reduce((prev, current) => 
      (prev.duration < current.duration) ? prev : current
    );

    return {
      total,
      slowest,
      fastest,
      average: Math.round(average),
      operations: completedOperations.sort((a, b) => b.duration - a.duration)
    };
  }

  /**
   * 打印性能报告
   */
  printReport() {
    const report = this.getReport();
    
    console.log('\n📈 [性能监控] 统计报告');
    console.log('===============================');
    console.log(`总操作数: ${report.operations.length}`);
    console.log(`总耗时: ${report.total}ms`);
    console.log(`平均耗时: ${report.average}ms`);
    
    if (report.slowest) {
      console.log(`最慢操作: ${report.slowest.name} (${report.slowest.duration}ms)`);
    }
    
    if (report.fastest) {
      console.log(`最快操作: ${report.fastest.name} (${report.fastest.duration}ms)`);
    }
    
    console.log('\n🔍 详细操作耗时（按耗时降序）:');
    report.operations.forEach((op, index) => {
      const icon = op.duration > 1000 ? '🐌' : op.duration > 500 ? '⚠️' : '✅';
      console.log(`${index + 1}. ${icon} ${op.name}: ${op.duration}ms ${op.description ? `(${op.description})` : ''}`);
    });
    
    console.log('===============================\n');
  }

  /**
   * 清空日志
   */
  clear() {
    this.logs = [];
    this.timers.clear();
  }

  /**
   * 启用/禁用监控
   * @param {boolean} enabled 
   */
  setEnabled(enabled) {
    this.isEnabled = enabled;
  }
}

// 创建全局实例
const performanceMonitor = new PerformanceMonitor();

// 导出实例和类
export default performanceMonitor;
export { PerformanceMonitor };

// 页面性能监控辅助函数
export const pagePerformanceHelper = {
  /**
   * 监控页面初始化性能
   */
  startPageLoad() {
    performanceMonitor.start('page_load', '页面整体加载');
  },

  /**
   * 监控数据加载性能
   */
  startDataLoading(operation) {
    performanceMonitor.start(`data_${operation}`, `数据加载: ${operation}`);
  },

  /**
   * 监控计算性能
   */
  startComputation(operation) {
    performanceMonitor.start(`compute_${operation}`, `计算操作: ${operation}`);
  },

  /**
   * 监控UI渲染性能
   */
  startRendering(component) {
    performanceMonitor.start(`render_${component}`, `渲染组件: ${component}`);
  }
}; 