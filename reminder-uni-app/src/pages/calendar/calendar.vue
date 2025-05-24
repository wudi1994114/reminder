<template>
  <view class="container">
    <v-calendar 
      ref="calendarComponentRef" 
      :extraData="calendarExtraData"
      :defaultTime="currentCalendarDisplayTime"
      @calendarTap="handleDateTap"
      @monthTap="handleMonthTap"
      :selColor="'#3cc51f'" 
      :showDot="true"
      :showText="false" 
    />
    
    <view class="reminders-list-section" v-if="selectedDate">
      <view class="list-header">
        <text class="date-title">{{ formatSelectedDateForDisplay }}</text>
        <button class="add-btn-inline" @click="createReminderOnSelectedDate">新增提醒</button>
      </view>
      
      <view v-if="loadingRemindersForDate" class="loading-reminders">
          <text>加载提醒...</text>
      </view>
      <view v-else-if="selectedDateReminders.length === 0" class="empty-tip-inline">
        <text>当日无提醒</text>
      </view>
      <view v-else class="list-content">
        <view 
          v-for="(reminder, index) in selectedDateReminders" 
          :key="reminder.id || index" 
          class="reminder-item"
          @click="viewReminderDetail(reminder.id)"
        >
          <view class="reminder-info">
            <text class="reminder-title">{{ reminder.title }}</text>
            <text class="reminder-time">{{ formatDisplayTime(reminder.eventTime) }}</text>
          </view>
          <view class="reminder-status" :class="reminder.status === 'PENDING' ? 'pending' : 'completed'">
            <text>{{ reminder.status === 'PENDING' ? '待提醒' : '已完成' }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
// 导入 Vue Composition API 中的核心函数
// ref: 用于创建响应式数据引用
// computed: 用于创建计算属性
// onMounted: 生命周期钩子，在组件挂载后执行
// watch: 用于侦听响应式数据的变化
import { ref, computed, onMounted, watch } from 'vue';
// 从后端服务 API 模块中导入获取简单提醒列表的函数
import { getAllSimpleReminders } from '../../services/api';
// 从工具函数模块中导入格式化时间的函数
import { formatTime } from '../../utils/helpers';
// 导入自定义的 v-calendar 日历组件
import VCalendar from '../../components/v-calendar/v-calendar.vue';

// 默认导出一个 Vue 组件对象
export default {
  // 注册在本组件中使用的子组件
  components: {
    VCalendar // 注册 v-calendar 组件，使其可以在模板中使用
  },
  // uni-app 页面的生命周期钩子，页面显示时触发
  onShow() { 
    // 页面显示时，通常需要刷新当前月份的提醒数据
    // 使用 v-calendar 内部维护的当前年份和月份来加载数据
    // 注意：这里直接调用了 setup 函数中返回的方法，
    // 在 Vue 3 Options API 或者 Vue 2 中，setup 返回的内容会暴露给 this
    this.loadRemindersForMonth(this.currentCalendarDisplayTime.year, this.currentCalendarDisplayTime.month);
    // 如果之前已经有选中的日期，则重新加载该日期的提醒事项
    if (this.selectedDate) {
      this.loadRemindersForSelectedDate(this.selectedDate);
    }
  },
  // Vue 3 Composition API 的入口点
  setup() {
    // 创建一个 ref 引用，用于将来可能需要直接操作 v-calendar 组件实例
    const calendarComponentRef = ref(null); 
    // 创建一个响应式数组，用于存放传递给 v-calendar 组件的额外数据（如日期标记）
    const calendarExtraData = ref([]); 
    // 创建一个响应式对象，存储当前日历显示的年份和月份
    // 默认值为当前系统的年份和月份
    const currentCalendarDisplayTime = ref({
      year: new Date().getFullYear(), // 获取当前年份
      month: new Date().getMonth() + 1, // 获取当前月份 (0-11)，所以 +1 变成 (1-12)
    });

    // 创建一个响应式引用，存储用户当前选中的日期对象，默认为 null (未选中)
    const selectedDate = ref(null); 
    // 创建一个响应式数组，用于存储选中日期的提醒事项列表
    const selectedDateReminders = ref([]);
    // 创建一个响应式布尔值，标记是否正在加载选中日期的提醒事项
    const loadingRemindersForDate = ref(false);
    // 创建一个响应式数组，用于存储当前月份获取到的所有提醒事项
    const allRemindersInCurrentMonth = ref([]); 

    // 定义一个包含星期名称的数组，用于日期显示
    const weekdays = ['日', '一', '二', '三', '四', '五', '六'];

    // 创建一个计算属性，用于格式化显示选中的日期
    const formatSelectedDateForDisplay = computed(() => {
      // 如果没有选中日期，则返回空字符串
      if (!selectedDate.value) return '';
      // 将选中的日期字符串或时间戳转换为 Date 对象，确保操作的是 Date 实例
      const d = new Date(selectedDate.value); 
      const year = d.getFullYear(); // 获取年份
      // 获取月份 (0-11)，+1 变为 (1-12)，并用 padStart 补零确保两位数
      const month = String(d.getMonth() + 1).padStart(2, '0'); 
      // 获取日期，并用 padStart 补零确保两位数
      const date = String(d.getDate()).padStart(2, '0'); 
      const weekday = weekdays[d.getDay()]; // 根据 getDay() 返回的星期索引 (0-6) 获取星期名称
      // 返回格式化后的日期字符串，例如："2023年10月26日 星期四"
      return `${year}年${month}月${date}日 星期${weekday}`;
    });

    // 定义一个异步函数，用于加载指定年份和月份的提醒事项
    const loadRemindersForMonth = async (year, month) => {
      // month 参数是 1-12 范围
      console.log(`正在加载 ${year}-${month} 的提醒事项`);
      try {
        // 调用后端 API 获取简单提醒数据
        const response = await getAllSimpleReminders(year, month);
        console.log('获取到的原始提醒数据:', JSON.stringify(response));
        
        // 校验 API 返回的是否为数组，如果不是，则进行错误处理
        if (!Array.isArray(response)) {
          console.warn('API 返回了非数组数据:', response);
          allRemindersInCurrentMonth.value = []; // 清空当前月份的提醒
          calendarExtraData.value = []; // 清空日历标记数据
          return; // 提前退出函数执行
        }
        
        // 如果 API 调用成功且返回的是数组，则更新当前月份的所有提醒数据
        allRemindersInCurrentMonth.value = response || []; // 使用获取到的数据，如果为 null 或 undefined 则设置为空数组
        
        // 创建一个辅助对象，用于标记日期是否已处理，以避免在日历上对同一天重复打点
        const dateMap = {};
        // 创建一个数组，用于存放处理后传递给 v-calendar 的标记数据
        const processedData = [];
        
        // 遍历从 API 获取的提醒事件数据
        response.forEach(reminder => {
          // 确保 reminder 对象存在且包含 eventTime 属性
          if (reminder && reminder.eventTime) {
            // 将 ISO 8601 格式的 eventTime 字符串转换为 Date 对象
            const eventDate = new Date(reminder.eventTime);
            const reminderYear = eventDate.getFullYear(); // 提取年份
            const reminderMonth = eventDate.getMonth() + 1; // 提取月份 (1-12)
            const reminderDay = eventDate.getDate(); // 提取日期
            
            // 格式化日期为 "YYYY-M-D" 的字符串格式，这是 v-calendar 组件要求的格式
            // 注意：月份和日期不进行零填充 (例如：2025-5-7 而不是 2025-05-07)
            const formattedDate = `${reminderYear}-${reminderMonth}-${reminderDay}`;
            
            // 检查该日期是否已在 dateMap 中处理过
            // 如果没有，则表示这是该日期第一次遇到有提醒的事件
            if (!dateMap[formattedDate]) {
              dateMap[formattedDate] = true; // 标记该日期已处理
              
              // 将格式化后的日期和打点信息添加到 processedData 数组中
              processedData.push({
                date: formattedDate, // 日期字符串
                dot: true, // 显示红点标记
                // 可以显式设置 dotColor，即使 v-calendar 组件当前不直接使用此属性，
                // 也有助于调试或未来的扩展。
                dotColor: '#ff4500', // 设置一个鲜艳的红色
                // value: '', // 可选：如果想在日期下方显示一些文本信息，可以在这里设置
              });
            }
          }
        });
        
        // 更新传递给 v-calendar 组件的 extraData，使其在日历上显示红点
        calendarExtraData.value = processedData;
        console.log("处理后的日历标记数据:", JSON.stringify(calendarExtraData.value));

        // 如果在加载完月份数据后，已经有一个日期被选中，
        // 则需要刷新该选中日期的提醒列表，以确保显示的是最新的数据。
        if (selectedDate.value) {
            loadRemindersForSelectedDate(selectedDate.value);
        }

      } catch (error) {
        // 如果 API 调用或数据处理过程中发生错误，则打印错误信息
        console.error("获取月份提醒失败:", error);
        // 清空相关数据，避免显示旧的或错误的数据
        allRemindersInCurrentMonth.value = [];
        calendarExtraData.value = [];
      }
    };
    
    // 定义处理 v-calendar 组件月份切换事件 (monthTap) 的回调函数
    const handleMonthTap = (time) => {
      // time 对象包含切换后的 year 和 month
      console.log('月份已切换至:', time);
      // 更新当前日历显示的年月
      currentCalendarDisplayTime.value = { year: time.year, month: time.month };
      // 月份切换时，清除之前选中的日期
      selectedDate.value = null; 
      // 清空选中日期的提醒列表
      selectedDateReminders.value = [];
      // 加载新月份的提醒数据
      loadRemindersForMonth(time.year, time.month);
    };

    // 定义处理 v-calendar 组件日期点击事件 (calendarTap) 的回调函数
    const handleDateTap = (dateString) => {
      // dateString 是从 v-calendar 传来的 "YYYY-M-D" 格式的日期字符串
      console.log('日期被点击:', dateString);
      // 将 "YYYY-M-D" 格式的字符串转换为 Date 对象
      // 注意：JavaScript Date 构造函数的月份参数是 0-11，所以需要 parts[1] - 1
      const parts = dateString.split('-').map(Number); // 将字符串按 '-' 分割并转换为数字数组
      const newSelectedDate = new Date(parts[0], parts[1] - 1, parts[2]);
      // 更新当前选中的日期
      selectedDate.value = newSelectedDate;
      // 加载新选中日期的提醒事项列表
      loadRemindersForSelectedDate(newSelectedDate);
    };

    // 定义加载特定选中日期提醒事项的函数
    const loadRemindersForSelectedDate = (dateObj) => {
      // 如果 dateObj 为空 (未选中日期)，则不执行任何操作
      if (!dateObj) return;
      // 开始加载数据，设置 loading 状态为 true
      loadingRemindersForDate.value = true;
      const year = dateObj.getFullYear(); // 获取年份
      // 获取月份 (0-11)，+1 并补零
      const month = String(dateObj.getMonth() + 1).padStart(2, '0'); 
      // 获取日期，并补零
      const day = String(dateObj.getDate()).padStart(2, '0'); 
      // 构建日期字符串前缀，格式为 "YYYY-MM-DD"，用于过滤
      const dateStringPrefix = `${year}-${month}-${day}`;

      //从当前月份已加载的所有提醒中筛选出属于选中日期的提醒
      // eventTime 通常是 "YYYY-MM-DDTHH:mm:ss" 格式
      selectedDateReminders.value = allRemindersInCurrentMonth.value.filter(r => 
        r.eventTime.startsWith(dateStringPrefix)
      );
      console.log(`日期 ${dateStringPrefix} 的提醒事项:`, selectedDateReminders.value);
      // 数据加载完成，设置 loading 状态为 false
      loadingRemindersForDate.value = false;
    };
    
    // 定义查看提醒详情的函数
    const viewReminderDetail = (id) => {
      // 使用 uni-app 的导航 API 跳转到详情页面，并传递提醒的 id
      uni.navigateTo({
        url: `/pages/detail/detail?id=${id}`
      });
    };
    
    // 定义在选中日期上创建新提醒的函数
    const createReminderOnSelectedDate = () => {
      // 确保有日期被选中
      if (selectedDate.value) {
        // 将选中的 Date 对象转换为 "YYYY-MM-DD" 格式的字符串
        const d = new Date(selectedDate.value);
        const year = d.getFullYear();
        const monthStr = String(d.getMonth() + 1).padStart(2, '0');
        const dateStr = String(d.getDate()).padStart(2, '0');
        const dateString = `${year}-${monthStr}-${dateStr}`;
        // 跳转到创建提醒页面，并预填选中日期
        uni.navigateTo({
          url: `/pages/create/create?date=${dateString}`
        });
      } else {
        // 如果没有选中日期（理论上按钮不应显示），提示用户
        uni.showToast({ title: '请先选择一个日期', icon: 'none'}); 
      }
    };

    // 定义格式化显示提醒时间的函数
    const formatDisplayTime = (dateTimeStr) => {
        // 如果日期时间字符串为空，则返回空字符串
        if(!dateTimeStr) return '';
        // 使用导入的 formatTime 工具函数将 Date 对象格式化为 "HH:mm"
        return formatTime(new Date(dateTimeStr));
    };
    
    // 组件挂载后执行的生命周期钩子
    onMounted(() => {
      // 组件初次加载时，加载当前默认月份（通常是当前系统月份）的提醒数据
      loadRemindersForMonth(currentCalendarDisplayTime.value.year, currentCalendarDisplayTime.value.month);
    });
    
    // 从 setup 函数返回所有需要在模板中使用或在组件选项中访问的响应式数据和方法
    return {
      calendarComponentRef,       // v-calendar 组件的引用
      calendarExtraData,          // 传递给 v-calendar 的标记数据
      currentCalendarDisplayTime, // 当前日历显示的年月
      selectedDate,               // 用户选中的日期
      formatSelectedDateForDisplay, // 格式化选中日期的计算属性
      selectedDateReminders,      // 选中日期的提醒列表
      loadingRemindersForDate,    // 是否正在加载选中日期的提醒
      handleMonthTap,             // 处理月份切换的方法
      handleDateTap,              // 处理日期点击的方法
      loadRemindersForSelectedDate, // 加载选中日期提醒的方法 (也用于 onShow)
      loadRemindersForMonth,      // 加载月份提醒的方法 (也用于 onShow)
      viewReminderDetail,         // 查看提醒详情的方法
      createReminderOnSelectedDate, // 创建新提醒的方法
      formatDisplayTime           // 格式化提醒时间的方法
    };
  }
};
</script>

<style scoped>
.container {
  padding: 20rpx;
  display: flex;
  flex-direction: column;
  min-height: 100vh; /* Ensure container takes full height */
}

/* Optional: Add some margin below the calendar */
.v-calendar-component { /* Assuming you might wrap v-calendar or it has a root class */
  margin-bottom: 20rpx;
}

.reminders-list-section {
  margin-top: 30rpx;
  background-color: #fff;
  border-radius: 10rpx;
  padding: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0,0,0,0.05);
  flex-grow: 1; /* Allow this section to take remaining space */
  display: flex; /* Use flex to manage inner content if it might overflow */
  flex-direction: column;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
  padding-bottom: 15rpx;
  border-bottom: 1rpx solid #eee;
}

.date-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.add-btn-inline {
  background-color: #3cc51f;
  color: white;
  padding: 10rpx 25rpx;
  font-size: 26rpx;
  border-radius: 8rpx;
  line-height: normal; /* Override default button line-height if necessary */
  border: none;
}

.loading-reminders,
.loading-calendar /* If you add a loading state for the calendar itself */
.empty-tip-inline {
  text-align: center;
  color: #999;
  padding: 40rpx 0;
  font-size: 28rpx;
}

.list-content {
  /* max-height: 500rpx; /* Example: set max height if list can be very long */
  /* overflow-y: auto; */ /* And allow scrolling */
}

.reminder-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 25rpx 10rpx;
  border-bottom: 1rpx solid #f5f5f5;
  background-color: #fff;
  /* transition: background-color 0.2s; removed hover for touch devices */
}

.reminder-item:last-child {
  border-bottom: none;
}

.reminder-info {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.reminder-title {
  font-size: 30rpx;
  color: #333;
  margin-bottom: 8rpx;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.reminder-time {
  font-size: 26rpx;
  color: #888;
}

.reminder-status {
  padding: 8rpx 15rpx;
  border-radius: 6rpx;
  font-size: 24rpx;
}

.reminder-status.pending {
  background-color: #fff5e6; /* Light orange */
  color: #fa8c16;
}

.reminder-status.completed {
  background-color: #f6ffed; /* Light green */
  color: #52c41a;
}
</style> 