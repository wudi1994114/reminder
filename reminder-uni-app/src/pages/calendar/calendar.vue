<template>
  <view class="page-container">
    <!-- 内容区域 -->
    <view class="content-wrapper">
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
          <text class="empty-text">当日无提醒安排</text>
        </view>
        <view v-else class="reminders-list">
          <view 
            v-for="(reminder, index) in selectedDateReminders" 
            :key="reminder.id || index" 
            class="reminder-item"
            :class="{ 'reminder-past': reminder.isPast }"
            @click="viewReminderDetail(reminder.id)"
          >
            <view class="reminder-info">
              <text class="reminder-title" :class="{ 'title-past': reminder.isPast }">{{ reminder.title }}</text>
              <view class="reminder-meta">
                <text class="reminder-time" :class="{ 'time-past': reminder.isPast }">{{ formatDisplayTime(reminder.eventTime) }}</text>
                <text v-if="reminder.isPast" class="past-indicator">已过期</text>
              </view>
              <text v-if="reminder.description" class="reminder-description" :class="{ 'desc-past': reminder.isPast }">{{ reminder.description }}</text>
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
    </view>
    
    <!-- 底部添加按钮 -->
    <view class="bottom-actions">
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
import { getAllSimpleReminders, getAllComplexReminders } from '../../services/api';
// 从工具函数模块中导入格式化时间的函数
import { formatTime } from '../../utils/helpers';
// 导入节气和农历工具
import { getLunarInfo, getSolarTermForDate } from '../../utils/solarTermHelper';
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
      const selectedDateStr = selectedDate.value ? 
        `${selectedDate.value.getFullYear()}-${selectedDate.value.getMonth() + 1}-${selectedDate.value.getDate()}` : '';
      
      for (let i = 0; i < 42; i++) {
        const currentDate = new Date(startDate);
        currentDate.setDate(startDate.getDate() + i);
        
        const dateStr = `${currentDate.getFullYear()}-${currentDate.getMonth() + 1}-${currentDate.getDate()}`;
        const isCurrentMonth = currentDate.getMonth() === month - 1;
        const isSelected = dateStr === selectedDateStr;
        
        // 检查该日期是否有提醒（包括当天和未来的提醒）
        const hasReminder = allRemindersInCurrentMonth.value.some(reminder => {
          if (!reminder.eventTime) return false;
          
          // 处理不同格式的日期时间字符串
          let reminderDateTime = reminder.eventTime;
          if (reminderDateTime.includes(' ') && !reminderDateTime.includes('T')) {
            reminderDateTime = reminderDateTime.replace(' ', 'T');
          }
          
          const reminderDate = new Date(reminderDateTime);
          if (isNaN(reminderDate.getTime())) {
            console.warn('无效的提醒时间格式:', reminder.eventTime);
            return false;
          }
          
          // 检查是否是同一天
          const isSameDate = reminderDate.getFullYear() === currentDate.getFullYear() &&
                 reminderDate.getMonth() === currentDate.getMonth() &&
                 reminderDate.getDate() === currentDate.getDate();
          
          // 显示当天及未来的提醒（不过滤历史提醒，让用户看到所有安排）
          return isSameDate;
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
        // 并行获取简单提醒和复杂提醒
        const [simpleReminders, complexReminders] = await Promise.all([
          getAllSimpleReminders(year, month),
          getAllComplexReminders().then(data => {
            // 过滤出当月的复杂提醒
            if (!Array.isArray(data)) return [];
            return data.filter(reminder => {
              if (!reminder.eventTime && !reminder.cronExpression) return false;
              
              // 对于有明确时间的复杂提醒
              if (reminder.eventTime) {
                let dateTime = reminder.eventTime;
                if (dateTime.includes(' ') && !dateTime.includes('T')) {
                  dateTime = dateTime.replace(' ', 'T');
                }
                const reminderDate = new Date(dateTime);
                if (!isNaN(reminderDate.getTime())) {
                  return reminderDate.getFullYear() === year && 
                         (reminderDate.getMonth() + 1) === month;
                }
              }
              
              // 对于基于Cron表达式的复杂提醒，这里可以扩展处理逻辑
              // 暂时返回false，后续可以根据需要添加Cron解析
              return false;
            });
          }).catch(error => {
            console.warn('获取复杂提醒失败:', error);
            return [];
          })
        ]);
        
        console.log('获取到的简单提醒数据:', simpleReminders);
        console.log('获取到的复杂提醒数据:', complexReminders);
        
        // 合并两种类型的提醒
        const allReminders = [
          ...(Array.isArray(simpleReminders) ? simpleReminders : []),
          ...(Array.isArray(complexReminders) ? complexReminders : [])
        ];
        
        // 按时间排序
        allReminders.sort((a, b) => {
          const getDateTime = (reminder) => {
            if (!reminder.eventTime) return new Date(0);
            let dateTime = reminder.eventTime;
            if (dateTime.includes(' ') && !dateTime.includes('T')) {
              dateTime = dateTime.replace(' ', 'T');
            }
            return new Date(dateTime);
          };
          
          return getDateTime(a) - getDateTime(b);
        });
        
        // 更新当前月份的所有提醒数据
        allRemindersInCurrentMonth.value = allReminders;
        console.log(`${year}-${month} 月份提醒总数:`, allReminders.length);

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
      
      // 获取基本日期信息
      const month = d.getMonth() + 1;
      const date = d.getDate();
      const weekday = weekdays[d.getDay()];
      
      let title = '';
      if (isToday) {
        title = '今天';
      } else {
        title = `${month}月${date}日 星期${weekday}`;
      }
      
      try {
        // 获取农历信息
        const lunarInfo = getLunarInfo(d);
        const lunarText = `${lunarInfo.lunarMonthName}月${lunarInfo.lunarDayName}`;
        
        // 获取节气信息
        const solarTerm = getSolarTermForDate(d);
        
        // 构建附加信息
        let additionalInfo = [];
        
        // 添加农历信息
        additionalInfo.push(lunarText);
        
        // 添加节气信息
        if (solarTerm) {
          additionalInfo.push(solarTerm.name);
        }
        
        // 添加农历节日信息
        if (lunarInfo.lunarFestival && lunarInfo.lunarFestival.trim()) {
          additionalInfo.push(lunarInfo.lunarFestival);
        }
        
        // 组合标题
        if (additionalInfo.length > 0) {
          title += ` (${additionalInfo.join(' ')})`;
        }
        
      } catch (error) {
        console.warn('获取农历或节气信息失败:', error);
        // 如果获取农历信息失败，仍然显示基本的日期信息
      }
      
      return title;
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

      // 获取当前时间，用于标识未来的提醒
      const now = new Date();

      //从当前月份已加载的所有提醒中筛选出属于选中日期的提醒
      const dayReminders = allRemindersInCurrentMonth.value.filter(reminder => {
        if (!reminder.eventTime) return false;
        
        // 处理不同格式的日期时间字符串
        let dateTime = reminder.eventTime;
        if (dateTime.includes(' ') && !dateTime.includes('T')) {
          dateTime = dateTime.replace(' ', 'T');
        }
        
        const reminderDate = new Date(dateTime);
        if (isNaN(reminderDate.getTime())) {
          console.warn('无效的提醒时间格式:', reminder.eventTime);
          return false;
        }
        
        // 检查是否是同一天
        return reminderDate.getFullYear() === year &&
               reminderDate.getMonth() === (parseInt(month) - 1) &&
               reminderDate.getDate() === parseInt(day);
      });
      
      // 按时间排序，并标记是否为未来提醒
      selectedDateReminders.value = dayReminders
        .map(reminder => ({
          ...reminder,
          isPast: new Date(reminder.eventTime.includes(' ') && !reminder.eventTime.includes('T') 
            ? reminder.eventTime.replace(' ', 'T') 
            : reminder.eventTime) < now
        }))
        .sort((a, b) => {
          // 按时间升序排列，最近的提醒在前面
          const timeA = new Date(a.eventTime.includes(' ') && !a.eventTime.includes('T') 
            ? a.eventTime.replace(' ', 'T') 
            : a.eventTime);
          const timeB = new Date(b.eventTime.includes(' ') && !b.eventTime.includes('T') 
            ? b.eventTime.replace(' ', 'T') 
            : b.eventTime);
          return timeA - timeB;
        });
      
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
  position: relative;
}

/* 内容区域 */
.content-wrapper {
  flex: 1;
  padding-bottom: 110rpx; /* 为底部按钮留出空间 */
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
  gap: 24rpx;
  padding: 16rpx 32rpx;
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
  padding: 16rpx 32rpx;
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
  font-size: 32rpx;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: -0.015em;
  padding: 16rpx 0 8rpx;
  word-wrap: break-word;
  white-space: normal;
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
  gap: 24rpx;
  background-color: #fcfbf8;
  padding: 16rpx 0;
  min-height: 120rpx;
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
.bottom-actions {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: #fcfbf8;
  padding: 8rpx 32rpx 8rpx 32rpx;
  z-index: 100;
}

.add-btn {
  display: flex;
  min-width: 168rpx;
  max-width: 960rpx;
  cursor: pointer;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 40rpx;
  height: 80rpx;
  padding: 0 32rpx;
  flex: 1;
  background-color: #f7bd4a;
  color: #1c170d;
  gap: 12rpx;
  font-size: 30rpx;
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
  font-size: 40rpx;
  font-weight: 400;
}

.add-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 响应式适配 */
@media (max-width: 750rpx) {
  .content-wrapper {
    padding-bottom: 90rpx; /* 小屏幕适配 */
  }
  
  .nav-header {
    padding: 24rpx 24rpx 12rpx;
  }
  
  .calendar-wrapper {
    padding: 12rpx 24rpx;
    gap: 16rpx;
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
    padding: 12rpx 24rpx;
  }
  
  .section-title {
    font-size: 32rpx;
    padding: 12rpx 0 8rpx;
  }
  
  .reminder-item {
    padding: 12rpx 0;
    min-height: 100rpx;
  }
  
  .reminder-title {
    font-size: 28rpx;
  }
  
  .reminder-time {
    font-size: 26rpx;
  }
  
  .bottom-actions {
    padding: 6rpx 24rpx 6rpx 24rpx;
  }
  
  .add-btn {
    height: 70rpx;
    font-size: 26rpx;
    border-radius: 35rpx;
  }
  
  .add-icon {
    font-size: 32rpx;
  }
}

/* 提醒元数据容器 */
.reminder-meta {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 8rpx;
}

/* 提醒描述 */
.reminder-description {
  color: #9d8148;
  font-size: 26rpx;
  font-weight: 400;
  line-height: 1.3;
  margin-top: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 过期标识 */
.past-indicator {
  color: #ff4757;
  font-size: 24rpx;
  font-weight: 500;
  background-color: rgba(255, 71, 87, 0.1);
  padding: 4rpx 12rpx;
  border-radius: 16rpx;
  white-space: nowrap;
}

/* 过去提醒的样式 */
.reminder-item.reminder-past {
  opacity: 0.7;
  background-color: #f8f9fa;
}

.reminder-item.reminder-past:active {
  background-color: #e9ecef;
}

.reminder-title.title-past {
  color: #6c757d;
  text-decoration: line-through;
}

.reminder-time.time-past {
  color: #adb5bd;
}

.reminder-description.desc-past {
  color: #adb5bd;
}
</style> 