<template>
  <view class="page-container">
    
    <!-- 日历区域 -->
    <view class="calendar-wrapper">
      <view class="calendar-container">
        <!-- 月份导航 -->
        <view class="month-nav">
          <button class="nav-btn" @click="previousMonth">
            <text class="nav-arrow">‹</text>
          </button>
          <text class="month-title">{{ getMonthTitle() }}</text>
          <button class="nav-btn" @click="nextMonth">
            <text class="nav-arrow">›</text>
          </button>
        </view>
        
        <!-- 星期标题 -->
        <view class="weekdays">
          <text class="weekday-label" v-for="day in weekdayLabels" :key="day">{{ day }}</text>
        </view>
        
        <!-- 日期网格 -->
        <view class="dates-grid">
          <button 
            v-for="date in calendarDates" 
            :key="date.key"
            class="date-btn"
            :class="{
              'date-selected': date.isSelected,
              'date-has-reminder': date.hasReminder,
              'date-other-month': date.isOtherMonth
            }"
            @click="selectDate(date)"
          >
            <view class="date-content">{{ date.day }}</view>
          </button>
        </view>
      </view>
    </view>
    
    <!-- 选中日期的提醒列表 -->
    <view class="reminders-section" v-if="selectedDate">
      <text class="section-title">{{ getSelectedDateTitle() }}</text>
      
      <view v-if="loadingRemindersForDate" class="loading-state">
        <text class="loading-text">加载提醒...</text>
      </view>
      <view v-else-if="selectedDateReminders.length === 0" class="empty-state">
        <text class="empty-text">当日无即将到来的提醒</text>
      </view>
      <view v-else class="reminders-list">
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
          <view class="reminder-checkbox">
            <checkbox 
              :checked="reminder.status === 'COMPLETED'"
              @change="toggleReminderStatus(reminder)"
              class="custom-checkbox"
            />
          </view>
        </view>
      </view>
    </view>
    
    <!-- 底部添加按钮 -->
    <view class="bottom-section">
      <button class="add-btn" @click="createReminderOnSelectedDate" :disabled="!selectedDate">
        <text class="add-icon">+</text>
        <text class="add-text">添加提醒</text>
      </button>
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
// 默认导出一个 Vue 组件对象
export default {
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
    // 当前显示的年月
    const currentCalendarDisplayTime = ref({
      year: new Date().getFullYear(),
      month: new Date().getMonth() + 1,
    });

    // 选中的日期
    const selectedDate = ref(new Date());
    // 选中日期的提醒列表
    const selectedDateReminders = ref([]);
    // 是否正在加载提醒
    const loadingRemindersForDate = ref(false);
    // 当前月份的所有提醒
    const allRemindersInCurrentMonth = ref([]);

    // 星期标签
    const weekdayLabels = ['日', '一', '二', '三', '四', '五', '六'];
    const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
    
    // 月份名称
    const monthNames = [
      '一月', '二月', '三月', '四月', '五月', '六月',
      '七月', '八月', '九月', '十月', '十一月', '十二月'
    ];

    // 计算日历日期数据
    const calendarDates = computed(() => {
      const { year, month } = currentCalendarDisplayTime.value;
      const firstDay = new Date(year, month - 1, 1);
      const lastDay = new Date(year, month, 0);
      const startDate = new Date(firstDay);
      startDate.setDate(startDate.getDate() - firstDay.getDay());
      
      const dates = [];
      const today = new Date();
      const now = new Date(); // 当前时间，用于过滤未来提醒
      const selectedDateStr = selectedDate.value ? 
        `${selectedDate.value.getFullYear()}-${selectedDate.value.getMonth() + 1}-${selectedDate.value.getDate()}` : '';
      
      for (let i = 0; i < 42; i++) {
        const currentDate = new Date(startDate);
        currentDate.setDate(startDate.getDate() + i);
        
        const dateStr = `${currentDate.getFullYear()}-${currentDate.getMonth() + 1}-${currentDate.getDate()}`;
        const isCurrentMonth = currentDate.getMonth() === month - 1;
        const isSelected = dateStr === selectedDateStr;
        
        // 检查该日期是否有未来的提醒
        const hasReminder = allRemindersInCurrentMonth.value.some(reminder => {
          if (!reminder.eventTime) return false;
          const reminderDate = new Date(reminder.eventTime);
          const isSameDate = reminderDate.getFullYear() === currentDate.getFullYear() &&
                 reminderDate.getMonth() === currentDate.getMonth() &&
                 reminderDate.getDate() === currentDate.getDate();
          // 只有未来的提醒才标记
          return isSameDate && reminderDate >= now;
        });
        
        dates.push({
          key: `${currentDate.getFullYear()}-${currentDate.getMonth()}-${currentDate.getDate()}`,
          day: currentDate.getDate(),
          date: new Date(currentDate),
          isSelected,
          hasReminder,
          isOtherMonth: !isCurrentMonth
        });
      }
      
      return dates;
    });

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
        allRemindersInCurrentMonth.value = response || [];

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
      }
    };
    
    // 获取月份标题
    const getMonthTitle = () => {
      const { year, month } = currentCalendarDisplayTime.value;
      return `${monthNames[month - 1]} ${year}`;
    };
    
    // 获取选中日期标题
    const getSelectedDateTitle = () => {
      if (!selectedDate.value) return '';
      const d = selectedDate.value;
      const today = new Date();
      const isToday = d.toDateString() === today.toDateString();
      
      if (isToday) {
        return '今天';
      } else {
        const month = d.getMonth() + 1;
        const date = d.getDate();
        const weekday = weekdays[d.getDay()];
        return `${month}月${date}日 星期${weekday}`;
      }
    };
    
    // 上一个月
    const previousMonth = () => {
      const { year, month } = currentCalendarDisplayTime.value;
      if (month === 1) {
        currentCalendarDisplayTime.value = { year: year - 1, month: 12 };
      } else {
        currentCalendarDisplayTime.value = { year, month: month - 1 };
      }
      loadRemindersForMonth(currentCalendarDisplayTime.value.year, currentCalendarDisplayTime.value.month);
    };
    
    // 下一个月
    const nextMonth = () => {
      const { year, month } = currentCalendarDisplayTime.value;
      if (month === 12) {
        currentCalendarDisplayTime.value = { year: year + 1, month: 1 };
      } else {
        currentCalendarDisplayTime.value = { year, month: month + 1 };
      }
      loadRemindersForMonth(currentCalendarDisplayTime.value.year, currentCalendarDisplayTime.value.month);
    };
    
    // 选择日期
    const selectDate = (dateObj) => {
      selectedDate.value = dateObj.date;
      loadRemindersForSelectedDate(dateObj.date);
    };
    
    // 返回上一页
    const goBack = () => {
      uni.navigateBack();
    };
    
    // 切换提醒状态
    const toggleReminderStatus = (reminder) => {
      // 这里可以添加更新提醒状态的API调用
      console.log('切换提醒状态:', reminder);
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

      // 获取当前时间，用于过滤
      const now = new Date();

      //从当前月份已加载的所有提醒中筛选出属于选中日期的提醒
      // eventTime 通常是 "YYYY-MM-DDTHH:mm:ss" 格式
      const dayReminders = allRemindersInCurrentMonth.value.filter(r => 
        r.eventTime.startsWith(dateStringPrefix)
      );
      
      // 进一步过滤，只显示当前时间往后的提醒
      selectedDateReminders.value = dayReminders.filter(reminder => {
        if (!reminder.eventTime) return false;
        const reminderTime = new Date(reminder.eventTime);
        return reminderTime >= now;
      }).sort((a, b) => {
        // 按时间升序排列，最近的提醒在前面
        const timeA = new Date(a.eventTime);
        const timeB = new Date(b.eventTime);
        return timeA - timeB;
      });
      
      console.log(`日期 ${dateStringPrefix} 的未来提醒事项:`, selectedDateReminders.value);
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
        
        console.log('选中的日期:', selectedDate.value);
        console.log('格式化后的日期:', dateString);
        console.log('即将跳转的URL:', `/pages/create/create?date=${dateString}`);
        
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
        
        // 确保日期字符串格式兼容iOS
        let isoDateStr = dateTimeStr;
        // 如果是 "YYYY-MM-DD HH:mm:ss" 格式，转换为 ISO 格式
        if (dateTimeStr.includes(' ') && !dateTimeStr.includes('T')) {
            isoDateStr = dateTimeStr.replace(' ', 'T');
        }
        
        // 使用导入的 formatTime 工具函数将 Date 对象格式化为 "HH:mm"
        const date = new Date(isoDateStr);
        if (isNaN(date.getTime())) {
            console.error('无效的日期格式:', dateTimeStr);
            return '';
        }
        return formatTime(date);
    };
    
    // 组件挂载后执行的生命周期钩子
    onMounted(() => {
      // 组件初次加载时，加载当前默认月份（通常是当前系统月份）的提醒数据
      loadRemindersForMonth(currentCalendarDisplayTime.value.year, currentCalendarDisplayTime.value.month);
      // 由于默认选中了当前日期，也需要加载当前日期的提醒事项
      if (selectedDate.value) {
        loadRemindersForSelectedDate(selectedDate.value);
      }
    });
    
    // 从 setup 函数返回所有需要在模板中使用或在组件选项中访问的响应式数据和方法
    return {
      // 数据
      currentCalendarDisplayTime,
      selectedDate,
      selectedDateReminders,
      loadingRemindersForDate,
      weekdayLabels,
      calendarDates,
      
      // 计算属性和方法
      getMonthTitle,
      getSelectedDateTitle,
      previousMonth,
      nextMonth,
      selectDate,
      goBack,
      toggleReminderStatus,
      loadRemindersForSelectedDate,
      loadRemindersForMonth,
      viewReminderDetail,
      createReminderOnSelectedDate,
      formatDisplayTime
    };
  }
};
</script>

<style scoped>
/* 页面容器 */
.page-container {
  min-height: 100vh;
  background-color: #fcfbf8;
  display: flex;
  flex-direction: column;
  font-family: 'Manrope', 'Noto Sans', sans-serif;
}

/* 顶部导航栏 */
.nav-header {
  display: flex;
  align-items: center;
  background-color: #fcfbf8;
  padding: 32rpx 32rpx 16rpx;
  justify-content: space-between;
}

.nav-back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 96rpx;
  height: 96rpx;
  color: #1c170d;
  cursor: pointer;
}

.back-icon {
  font-size: 48rpx;
  font-weight: 400;
}

.nav-title {
  color: #1c170d;
  font-size: 36rpx;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -0.015em;
  flex: 1;
  text-align: center;
}

/* 日历区域 */
.calendar-wrapper {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 48rpx;
  padding: 32rpx;
}

.calendar-container {
  display: flex;
  min-width: 576rpx;
  max-width: 672rpx;
  flex: 1;
  flex-direction: column;
  gap: 8rpx;
}

/* 月份导航 */
.month-nav {
  display: flex;
  align-items: center;
  padding: 8rpx;
  justify-content: space-between;
}

.nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80rpx;
  height: 80rpx;
  color: #1c170d;
  background: none;
  border: none;
  cursor: pointer;
}

.nav-arrow {
  font-size: 36rpx;
  font-weight: 400;
}

.month-title {
  color: #1c170d;
  font-size: 32rpx;
  font-weight: 700;
  line-height: 1.2;
  flex: 1;
  text-align: center;
}

/* 星期标题 */
.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}

.weekday-label {
  color: #1c170d;
  font-size: 26rpx;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: 0.015em;
  display: flex;
  height: 96rpx;
  width: 100%;
  align-items: center;
  justify-content: center;
  padding-bottom: 8rpx;
}

/* 提醒列表区域 */
.reminders-section {
  background-color: #fcfbf8;
  padding: 32rpx;
}

/* 日期网格 */
.dates-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}

.date-btn {
  height: 96rpx;
  width: 100%;
  color: #1c170d;
  font-size: 28rpx;
  font-weight: 500;
  line-height: 1.4;
  background: none;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.date-content {
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.date-btn.date-selected .date-content {
  background-color: #f7bd4a;
}

.date-btn.date-has-reminder::after {
  content: '';
  position: absolute;
  top: 70rpx;
  left: 50%;
  transform: translateX(-50%);
  width: 12rpx;
  height: 12rpx;
  background-color: #ff4757;
  border-radius: 50%;
  z-index: 1;
}

.date-btn.date-selected.date-has-reminder::after {
  background-color: #1c170d;
}

.date-btn.date-other-month {
  color: #9d8148;
  opacity: 0.5;
}

.date-btn:active .date-content {
  background-color: #e6a73d;
}

.section-title {
  color: #1c170d;
  font-size: 36rpx;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -0.015em;
  padding: 32rpx 0 16rpx;
}

/* 加载状态 */
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200rpx;
}

.loading-text {
  font-size: 28rpx;
  color: #9d8148;
  line-height: 1.4;
}

/* 空状态 */
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200rpx;
  text-align: center;
}

.empty-text {
  font-size: 28rpx;
  color: #9d8148;
  line-height: 1.4;
}

/* 提醒列表 */
.reminders-list {
  display: flex;
  flex-direction: column;
}

.reminder-item {
  display: flex;
  align-items: center;
  gap: 32rpx;
  background-color: #fcfbf8;
  padding: 32rpx;
  min-height: 144rpx;
  padding-top: 16rpx;
  padding-bottom: 16rpx;
  justify-content: space-between;
  cursor: pointer;
}

.reminder-item:active {
  background-color: #f4efe7;
}

.reminder-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  flex: 1;
}

.reminder-title {
  color: #1c170d;
  font-size: 32rpx;
  font-weight: 500;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 8rpx;
}

.reminder-time {
  color: #9d8148;
  font-size: 28rpx;
  font-weight: 400;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.reminder-checkbox {
  flex-shrink: 0;
  display: flex;
  width: 56rpx;
  height: 56rpx;
  align-items: center;
  justify-content: center;
}

.custom-checkbox {
  width: 40rpx;
  height: 40rpx;
  border-radius: 8rpx;
  border: 4rpx solid #e9e0ce;
  background-color: transparent;
  color: #f7bd4a;
}

.custom-checkbox:checked {
  background-color: #f7bd4a;
  border-color: #f7bd4a;
}

/* 底部按钮区域 */
.bottom-section {
  display: flex;
  padding: 32rpx;
  padding-top: 24rpx;
}

.add-btn {
  display: flex;
  min-width: 168rpx;
  max-width: 960rpx;
  cursor: pointer;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 48rpx;
  height: 96rpx;
  padding: 0 40rpx;
  flex: 1;
  background-color: #f7bd4a;
  color: #1c170d;
  gap: 16rpx;
  padding-left: 40rpx;
  font-size: 32rpx;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: 0.015em;
  border: none;
}

.add-btn:active {
  background-color: #e6a73d;
}

.add-btn:disabled {
  background-color: #e9e0ce;
  color: #9d8148;
  cursor: not-allowed;
}

.add-icon {
  color: #1c170d;
  font-size: 48rpx;
  font-weight: 400;
}

.add-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 响应式适配 */
@media (max-width: 750rpx) {
  .nav-header {
    padding: 24rpx 24rpx 12rpx;
  }
  
  .calendar-wrapper {
    padding: 24rpx;
    gap: 32rpx;
  }
  
  .calendar-container {
    min-width: 480rpx;
  }
  
  .month-title {
    font-size: 28rpx;
  }
  
  .weekday-label {
    font-size: 24rpx;
    height: 80rpx;
  }
  
  .date-btn {
    height: 80rpx;
    font-size: 26rpx;
  }
  
  .date-btn.date-has-reminder::after {
    top: 52rpx;
    width: 10rpx;
    height: 10rpx;
  }
  
  .reminders-section {
    padding: 24rpx;
  }
  
  .section-title {
    font-size: 32rpx;
    padding: 24rpx 0 12rpx;
  }
  
  .reminder-item {
    padding: 24rpx;
    min-height: 120rpx;
  }
  
  .reminder-title {
    font-size: 28rpx;
  }
  
  .reminder-time {
    font-size: 26rpx;
  }
  
  .bottom-section {
    padding: 24rpx;
  }
  
  .add-btn {
    height: 80rpx;
    font-size: 28rpx;
  }
  
  .add-icon {
    font-size: 40rpx;
  }
}
</style> 