# DateTimePicker 日期时间选择器

一个灵活的日期时间选择器组件，支持多种显示模式和自定义列配置。

## 功能特性

- 🎯 **多种模式**：支持日期时间、仅日期、仅时间三种模式
- 🔧 **自定义列**：可自由配置显示年、月、日、时、分任意组合
- 📱 **原生体验**：使用picker-view提供原生滑动选择体验
- 🎨 **中文友好**：中文格式显示，支持今天/明天智能识别
- ⚡ **响应式**：自适应不同屏幕尺寸

## 使用方法

### 1. 导入组件
```javascript
import DateTimePicker from '../../components/datetime-picker/datetime-picker.vue';

export default {
  components: {
    DateTimePicker
  }
}
```

### 2. 基本使用
```vue
<template>
  <datetime-picker 
    @change="onDateTimeChange"
  />
</template>

<script>
export default {
  methods: {
    onDateTimeChange(dateTimeData) {
      console.log('选择的日期时间:', dateTimeData);
      // dateTimeData 包含: { date, time, eventTime }
    }
  }
}
</script>
```

### 3. 完整配置
```vue
<template>
  <datetime-picker 
    ref="dateTimePickerRef"
    label="自定义标签"
    :initial-date="initialDate"
    :initial-time="initialTime"
    :auto-set-default="false"
    @change="onDateTimeChange"
    @dateChange="onDateChange"
    @timeChange="onTimeChange"
  />
</template>

<script>
export default {
  data() {
    return {
      initialDate: '2024-01-15',
      initialTime: '14:30'
    }
  },
  methods: {
    onDateTimeChange(data) {
      // 完整的日期时间数据
      console.log('日期:', data.date);        // '2024-01-15'
      console.log('时间:', data.time);        // '14:30'
      console.log('完整时间:', data.eventTime); // '2024-01-15 14:30:00'
    },
    onDateChange(date) {
      console.log('日期变化:', date);
    },
    onTimeChange(time) {
      console.log('时间变化:', time);
    }
  }
}
</script>
```

## Props 属性

| 属性名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| label | String | '时间设置' | 显示的标签文本 |
| initialDate | String | '' | 初始日期，格式：YYYY-MM-DD |
| initialTime | String | '' | 初始时间，格式：HH:mm |
| autoSetDefault | Boolean | true | 是否自动设置默认时间（当前时间+1小时） |
| mode | String | 'datetime' | 选择器模式：'datetime' \| 'date' \| 'time' |
| columns | Array | ['year', 'month', 'day', 'hour', 'minute'] | 自定义显示列 |

### mode 模式说明

- **datetime**: 完整的日期时间选择器（默认）
- **date**: 仅日期选择器（年月日）
- **time**: 仅时间选择器（时分）

### columns 列配置

可选值：`['year', 'month', 'day', 'hour', 'minute']`

- **year**: 年份列
- **month**: 月份列  
- **day**: 日期列
- **hour**: 小时列
- **minute**: 分钟列

## Events 事件

| 事件名 | 参数 | 说明 |
|--------|------|------|
| change | { date, time, eventTime } | 日期或时间变化时触发 |
| dateChange | date (String) | 仅日期变化时触发 |
| timeChange | time (String) | 仅时间变化时触发 |

## Methods 方法

通过 ref 可以调用以下方法：

### setDateTime(date, time)
设置日期和时间
```javascript
this.$refs.dateTimePickerRef.setDateTime('2024-01-15', '14:30');
```

### getDateTime()
获取当前日期时间数据
```javascript
const dateTime = this.$refs.dateTimePickerRef.getDateTime();
console.log(dateTime); // { date: '2024-01-15', time: '14:30', eventTime: '2024-01-15 14:30:00' }
```

## 时间显示格式

组件会智能显示时间格式：
- **今天**: "今天 上午9:00"
- **明天**: "明天 下午2:30"
- **其他日期**: "3月15日 上午8:00"

## 使用示例

### 示例1：简单提醒创建
```vue
<template>
  <view class="reminder-form">
    <input v-model="title" placeholder="提醒标题" />
    <datetime-picker @change="onTimeChange" />
    <button @click="saveReminder">保存</button>
  </view>
</template>

<script>
import DateTimePicker from '../../components/datetime-picker/datetime-picker.vue';

export default {
  components: { DateTimePicker },
  data() {
    return {
      title: '',
      eventTime: ''
    }
  },
  methods: {
    onTimeChange(data) {
      this.eventTime = data.eventTime;
    },
    saveReminder() {
      // 保存提醒逻辑
      console.log('保存提醒:', { title: this.title, eventTime: this.eventTime });
    }
  }
}
</script>
```

### 示例2：编辑模式
```vue
<template>
  <datetime-picker 
    ref="editTimePicker"
    :initial-date="reminderData.date"
    :initial-time="reminderData.time"
    :auto-set-default="false"
    @change="onTimeChange"
  />
</template>

<script>
export default {
  data() {
    return {
      reminderData: {
        date: '2024-01-15',
        time: '09:00'
      }
    }
  },
  methods: {
    onTimeChange(data) {
      this.reminderData.date = data.date;
      this.reminderData.time = data.time;
    }
  }
}
</script>
```

## 样式定制

组件使用了 scoped 样式，如需定制样式，可以通过以下方式：

### 1. 全局样式覆盖
```css
/* 在全局样式中 */
.datetime-picker .setting-item {
  background-color: #f0f0f0;
}
```

### 2. 深度选择器
```vue
<style scoped>
.my-form ::v-deep .datetime-picker .setting-label {
  color: #333;
  font-weight: bold;
}
</style>
```

## 注意事项

1. **日期格式**: 组件内部使用 YYYY-MM-DD 格式处理日期
2. **时间格式**: 组件内部使用 HH:mm 格式处理时间
3. **iOS兼容性**: 已处理iOS日期解析兼容性问题
4. **事件触发**: 初始化时会自动触发一次 change 事件
5. **响应式**: 组件会监听 props 变化并自动更新

## 更新日志

### v1.0.0
- 初始版本发布
- 支持基本的日期时间选择功能
- 提供中文友好的显示格式
- 支持系统原生选择器

### v1.1.0
- ✨ 新增 `mode` 参数支持不同选择模式
- ✨ 新增 `columns` 参数支持自定义显示列
- 🐛 修复月份切换时天数不正确的问题
- 💄 优化选择器UI和交互体验