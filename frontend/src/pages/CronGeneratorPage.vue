<template>
  <div class="cron-generator-page">
    <h1>CRON表达式生成器</h1>
    
    <div class="cron-container">
      <div class="cron-result">
        <h3>生成的CRON表达式:</h3>
        <div class="expression-box">
          <span class="cron-value">{{ cronExpression }}</span>
          <button class="copy-btn" @click="copyExpression">复制</button>
        </div>
        <p class="cron-description">{{ cronDescription }}</p>
      </div>
      
      <div class="generator-form">
        <div class="form-group">
          <h3>频率选择</h3>
          <div class="radio-group">
            <label>
              <input type="radio" v-model="frequency" value="daily" @change="updateExpression">
              每天
            </label>
            <label>
              <input type="radio" v-model="frequency" value="weekly" @change="updateExpression">
              每周
            </label>
            <label>
              <input type="radio" v-model="frequency" value="monthly" @change="updateExpression">
              每月
            </label>
            <label>
              <input type="radio" v-model="frequency" value="custom" @change="updateExpression">
              自定义
            </label>
          </div>
        </div>
        
        <!-- 每天选项 -->
        <div v-if="frequency === 'daily'" class="form-group">
          <h3>时间</h3>
          <div class="time-selector">
            <select v-model="hourValue" @change="updateExpression">
              <option v-for="h in 24" :key="`hour-${h-1}`" :value="h-1">{{ padZero(h-1) }}</option>
            </select>
            <span>:</span>
            <select v-model="minuteValue" @change="updateExpression">
              <option v-for="m in 60" :key="`min-${m-1}`" :value="m-1">{{ padZero(m-1) }}</option>
            </select>
          </div>
        </div>
        
        <!-- 每周选项 -->
        <div v-if="frequency === 'weekly'" class="form-group">
          <h3>星期</h3>
          <div class="weekday-selector">
            <label v-for="(day, index) in weekdays" :key="index">
              <input type="checkbox" v-model="selectedWeekdays" :value="index+1" @change="updateExpression">
              {{ day }}
            </label>
          </div>
          
          <h3>时间</h3>
          <div class="time-selector">
            <select v-model="hourValue" @change="updateExpression">
              <option v-for="h in 24" :key="`hour-${h-1}`" :value="h-1">{{ padZero(h-1) }}</option>
            </select>
            <span>:</span>
            <select v-model="minuteValue" @change="updateExpression">
              <option v-for="m in 60" :key="`min-${m-1}`" :value="m-1">{{ padZero(m-1) }}</option>
            </select>
          </div>
        </div>
        
        <!-- 每月选项 -->
        <div v-if="frequency === 'monthly'" class="form-group">
          <h3>日期</h3>
          <div class="monthday-selector">
            <div class="grid-container">
              <label v-for="day in 31" :key="`day-${day}`" class="grid-item">
                <input type="checkbox" v-model="selectedMonthDays" :value="day" @change="updateExpression">
                {{ day }}
              </label>
            </div>
          </div>
          
          <h3>时间</h3>
          <div class="time-selector">
            <select v-model="hourValue" @change="updateExpression">
              <option v-for="h in 24" :key="`hour-${h-1}`" :value="h-1">{{ padZero(h-1) }}</option>
            </select>
            <span>:</span>
            <select v-model="minuteValue" @change="updateExpression">
              <option v-for="m in 60" :key="`min-${m-1}`" :value="m-1">{{ padZero(m-1) }}</option>
            </select>
          </div>
        </div>
        
        <!-- 自定义选项 -->
        <div v-if="frequency === 'custom'" class="form-group">
          <h3>自定义CRON表达式</h3>
          <div class="custom-cron">
            <input type="text" v-model="customCron" placeholder="* * * * * *" @input="updateExpressionFromCustom">
            <p class="hint">格式: 秒 分 时 日 月 星期</p>
          </div>
        </div>
      </div>
      
      <div class="preview-section">
        <h3>执行预览 (未来10次执行时间)</h3>
        <div v-if="executionTimes.length" class="execution-list">
          <div v-for="(time, index) in executionTimes" :key="index" class="execution-time">
            {{ formatDateTime(time) }}
          </div>
        </div>
        <div v-else class="no-executions">
          <p>请配置有效的CRON表达式</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
// 引入cron解析库，如果不存在需要安装: npm install cron-parser
export default {
  name: 'CronGeneratorPage',
  data() {
    return {
      frequency: 'daily',
      hourValue: 9,
      minuteValue: 0,
      selectedWeekdays: [1], // 默认周一
      selectedMonthDays: [1], // 默认每月1号
      customCron: '0 0 9 * * *',
      cronExpression: '0 0 9 * * *',
      cronDescription: '每天上午9:00',
      executionTimes: [],
      weekdays: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    }
  },
  mounted() {
    this.updateExpression();
  },
  methods: {
    updateExpression() {
      switch(this.frequency) {
        case 'daily':
          this.cronExpression = `0 ${this.minuteValue} ${this.hourValue} * * *`;
          this.cronDescription = `每天 ${this.padZero(this.hourValue)}:${this.padZero(this.minuteValue)}`;
          break;
        case 'weekly':
          if (this.selectedWeekdays.length === 0) {
            this.selectedWeekdays = [1]; // 默认周一
          }
          const weekdaysStr = this.selectedWeekdays.sort().join(',');
          this.cronExpression = `0 ${this.minuteValue} ${this.hourValue} * * ${weekdaysStr}`;
          
          const weekdayNames = this.selectedWeekdays.map(d => this.weekdays[d-1]).join('、');
          this.cronDescription = `每周 ${weekdayNames} ${this.padZero(this.hourValue)}:${this.padZero(this.minuteValue)}`;
          break;
        case 'monthly':
          if (this.selectedMonthDays.length === 0) {
            this.selectedMonthDays = [1]; // 默认1号
          }
          const monthDaysStr = this.selectedMonthDays.sort((a, b) => a - b).join(',');
          this.cronExpression = `0 ${this.minuteValue} ${this.hourValue} ${monthDaysStr} * *`;
          this.cronDescription = `每月 ${monthDaysStr} 号 ${this.padZero(this.hourValue)}:${this.padZero(this.minuteValue)}`;
          break;
        case 'custom':
          this.cronExpression = this.customCron;
          this.cronDescription = '自定义CRON表达式';
          break;
      }
      
      this.calculateNextExecutions();
    },
    
    updateExpressionFromCustom() {
      this.cronExpression = this.customCron;
      this.calculateNextExecutions();
    },
    
    calculateNextExecutions() {
      // 这里需要一个CRON解析库来计算未来执行时间
      // 简化起见，这里生成模拟数据
      this.executionTimes = [];
      
      try {
        // 模拟计算CRON未来执行时间
        const now = new Date();
        let nextDate = new Date(now);
        
        for (let i = 0; i < 10; i++) {
          // 这是个模拟示例，真实实现需要依赖cron-parser或类似库
          nextDate = new Date(nextDate.getTime() + 24 * 60 * 60 * 1000);
          this.executionTimes.push(nextDate);
        }
      } catch (e) {
        console.error('Invalid cron expression', e);
      }
    },
    
    padZero(num) {
      return num < 10 ? `0${num}` : `${num}`;
    },
    
    formatDateTime(date) {
      return `${date.getFullYear()}-${this.padZero(date.getMonth() + 1)}-${this.padZero(date.getDate())} ${this.padZero(date.getHours())}:${this.padZero(date.getMinutes())}:${this.padZero(date.getSeconds())}`;
    },
    
    copyExpression() {
      navigator.clipboard.writeText(this.cronExpression)
        .then(() => {
          alert('CRON表达式已复制到剪贴板');
        })
        .catch(err => {
          console.error('复制失败:', err);
        });
    }
  }
}
</script>

<style scoped>
.cron-generator-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
  font-family: Arial, sans-serif;
}

h1 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.cron-container {
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.cron-result {
  background-color: #fff;
  padding: 15px;
  border-radius: 6px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.expression-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #f0f8ff;
  padding: 10px 15px;
  border-radius: 4px;
  margin: 10px 0;
}

.cron-value {
  font-family: monospace;
  font-size: 18px;
  font-weight: bold;
  color: #2c3e50;
}

.copy-btn {
  background-color: #4CAF50;
  color: white;
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
}

.copy-btn:hover {
  background-color: #45a049;
}

.cron-description {
  color: #6c757d;
  font-style: italic;
}

.generator-form {
  margin-bottom: 30px;
}

.form-group {
  margin-bottom: 20px;
}

.radio-group {
  display: flex;
  gap: 15px;
  margin-top: 10px;
}

.radio-group label {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.time-selector {
  display: flex;
  align-items: center;
  gap: 5px;
}

.time-selector select {
  padding: 8px;
  border-radius: 4px;
  border: 1px solid #ced4da;
}

.weekday-selector, .monthday-selector {
  margin-top: 10px;
  margin-bottom: 20px;
}

.weekday-selector label {
  display: block;
  margin-bottom: 8px;
}

.grid-container {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.grid-item {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 5px;
}

.custom-cron input {
  width: 100%;
  padding: 8px;
  border-radius: 4px;
  border: 1px solid #ced4da;
  font-family: monospace;
}

.hint {
  font-size: 12px;
  color: #6c757d;
  margin-top: 5px;
}

.preview-section {
  background-color: #fff;
  padding: 15px;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.execution-list {
  max-height: 300px;
  overflow-y: auto;
}

.execution-time {
  padding: 8px;
  border-bottom: 1px solid #eee;
  font-family: monospace;
}

.execution-time:last-child {
  border-bottom: none;
}

.no-executions {
  color: #6c757d;
  text-align: center;
  padding: 15px;
}
</style> 