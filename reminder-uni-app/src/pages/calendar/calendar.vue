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
import { ref, computed, onMounted, watch } from 'vue';
import { getAllSimpleReminders } from '../../services/api';
import { formatTime } from '../../utils/helpers';
import { reminderState } from '../../services/store';
import VCalendar from '../../components/v-calendar/v-calendar.vue'; // 引入新日历组件

export default {
  components: {
    VCalendar // 注册组件
  },
  onShow() { 
    // 页面显示时，使用 v-calendar 内部的当前年月重新加载数据
    // 或者，如果需要强制刷新到今天的月份，可以重新设置 defaultTime
    // this.currentCalendarDisplayTime.value = { year: new Date().getFullYear(), month: new Date().getMonth() + 1 };
    this.loadRemindersForMonth(this.currentCalendarDisplayTime.year, this.currentCalendarDisplayTime.month);
    if (this.selectedDate) {
      this.loadRemindersForSelectedDate(this.selectedDate);
    }
  },
  setup() {
    const calendarComponentRef = ref(null); // ref for v-calendar component if needed
    const calendarExtraData = ref([]); // For v-calendar markings
    const currentCalendarDisplayTime = ref({
      year: new Date().getFullYear(),
      month: new Date().getMonth() + 1, // 1-12
    });

    const selectedDate = ref(null); // Date object for the selected date
    const selectedDateReminders = ref([]);
    const loadingRemindersForDate = ref(false);
    const allRemindersInCurrentMonth = ref([]); // Store all reminders for the current month

    const weekdays = ['日', '一', '二', '三', '四', '五', '六'];

    const formatSelectedDateForDisplay = computed(() => {
      if (!selectedDate.value) return '';
      const d = new Date(selectedDate.value); // Ensure it's a Date object
      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const date = String(d.getDate()).padStart(2, '0');
      const weekday = weekdays[d.getDay()];
      return `${year}年${month}月${date}日 星期${weekday}`;
    });

    const loadRemindersForMonth = async (year, month) => {
      // month is 1-12
      console.log(`Loading reminders for ${year}-${month}`);
      try {
        const response = await getAllSimpleReminders(year, month);
        console.log('获取到的原始提醒数据:', JSON.stringify(response));
        
        // 检查返回值是否是数组
        if (!Array.isArray(response)) {
          console.warn('API 返回了非数组数据:', response);
          allRemindersInCurrentMonth.value = [];
          calendarExtraData.value = [];
          return; // 提前返回，不再处理
        }
        
        allRemindersInCurrentMonth.value = response || [];
        
        // 创建一个辅助对象，用于去重（同一天可能有多个提醒）
        const dateMap = {};
        const processedData = [];
        
        // 遍历获取的提醒事件数据
        response.forEach(reminder => {
          if (reminder && reminder.eventTime) {
            // 解析 ISO 8601 格式的日期，提取年、月、日
            const eventDate = new Date(reminder.eventTime);
            const year = eventDate.getFullYear();
            const month = eventDate.getMonth() + 1; // 月份是 1-12
            const day = eventDate.getDate();
            
            // 格式化为 "YYYY-M-D" 格式，v-calendar 组件要求这种格式
            // 注意：不要使用零填充的月份和日期 (2025-05-07 改为 2025-5-7)
            const formattedDate = `${year}-${month}-${day}`;
            console.log('格式化的日期:', formattedDate, '原始日期:', reminder.eventTime);
            
            // 如果这一天还没有添加到映射中，就添加它（确保每一天只有一个 dot）
            if (!dateMap[formattedDate]) {
              dateMap[formattedDate] = true;
              
              // 添加到 processedData 数组，包含 date 和 dot 属性
              processedData.push({
                date: formattedDate,
                dot: true,
                // 明确设置 dotColor 确保点是可见的，即使 v-calendar 组件不直接使用它，
                // 下面的代码也有助于调试
                dotColor: '#ff4500', // 鲜红色
                // value: '', // 可选：如果您想在日期下显示一些信息
              });
            }
          }
        });
        
        // 更新 calendarExtraData
        calendarExtraData.value = processedData;
        console.log("处理后的日历数据:", JSON.stringify(calendarExtraData.value));

        // If a date is already selected, refresh its reminders
        if (selectedDate.value) {
            loadRemindersForSelectedDate(selectedDate.value);
        }

      } catch (error) {
        console.error("获取月份提醒失败:", error);
        allRemindersInCurrentMonth.value = [];
        calendarExtraData.value = [];
      }
    };
    
    const handleMonthTap = (time) => {
      // time is { year, month }
      console.log('Month changed to:', time);
      currentCalendarDisplayTime.value = { year: time.year, month: time.month };
      selectedDate.value = null; // Clear selected date when month changes
      selectedDateReminders.value = [];
      loadRemindersForMonth(time.year, time.month);
    };

    const handleDateTap = (dateString) => {
      // dateString is YYYY-M-D from v-calendar
      console.log('Date tapped:', dateString);
      // Convert YYYY-M-D to a Date object, ensuring local timezone.
      // Parts are 1-indexed for month, 0-indexed for Date constructor's month
      const parts = dateString.split('-').map(Number);
      const newSelectedDate = new Date(parts[0], parts[1] - 1, parts[2]);
      selectedDate.value = newSelectedDate;
      loadRemindersForSelectedDate(newSelectedDate);
    };

    const loadRemindersForSelectedDate = (dateObj) => {
      if (!dateObj) return;
      loadingRemindersForDate.value = true;
      const year = dateObj.getFullYear();
      const month = String(dateObj.getMonth() + 1).padStart(2, '0');
      const day = String(dateObj.getDate()).padStart(2, '0');
      const dateStringPrefix = `${year}-${month}-${day}`;

      selectedDateReminders.value = allRemindersInCurrentMonth.value.filter(r => 
        r.eventTime.startsWith(dateStringPrefix)
      );
      console.log(`Reminders for ${dateStringPrefix}:`, selectedDateReminders.value);
      loadingRemindersForDate.value = false;
    };
    
    const viewReminderDetail = (id) => {
      uni.navigateTo({
        url: `/pages/detail/detail?id=${id}`
      });
    };
    
    const createReminderOnSelectedDate = () => {
      if (selectedDate.value) {
        const d = new Date(selectedDate.value);
        const year = d.getFullYear();
        const monthStr = String(d.getMonth() + 1).padStart(2, '0');
        const dateStr = String(d.getDate()).padStart(2, '0');
        const dateString = `${year}-${monthStr}-${dateStr}`;
        uni.navigateTo({
          url: `/pages/create/create?date=${dateString}`
        });
      } else {
        // This case should ideally not happen if button is only shown when selectedDate is true
        uni.showToast({ title: '请先选择一个日期', icon: 'none'}); 
      }
    };

    const formatDisplayTime = (dateTimeStr) => {
        if(!dateTimeStr) return '';
        return formatTime(new Date(dateTimeStr));
    };
    
    onMounted(() => {
      // Load reminders for the initial month
      loadRemindersForMonth(currentCalendarDisplayTime.value.year, currentCalendarDisplayTime.value.month);
      
      // 添加一些测试数据，确保即使在未登录状态下也能看到圆点
      setTimeout(() => {
        if (calendarExtraData.value.length === 0) {
          console.log('未获取到真实数据，添加测试数据');
          const testYear = currentCalendarDisplayTime.value.year;
          const testMonth = currentCalendarDisplayTime.value.month;
          
          // 在本月添加3个测试点
          const day1 = 10;
          const day2 = 15;
          const day3 = 20;
          
          calendarExtraData.value = [
            { date: `${testYear}-${testMonth}-${day1}`, dot: true },
            { date: `${testYear}-${testMonth}-${day2}`, dot: true },
            { date: `${testYear}-${testMonth}-${day3}`, dot: true }
          ];
          
          console.log('添加测试数据后:', JSON.stringify(calendarExtraData.value));
        }
      }, 2000); // 2秒后检查
    });
    
    return {
      calendarComponentRef,
      calendarExtraData,
      currentCalendarDisplayTime,
      selectedDate,
      formatSelectedDateForDisplay,
      selectedDateReminders,
      loadingRemindersForDate,
      handleMonthTap,
      handleDateTap,
      loadRemindersForSelectedDate, // for onShow
      loadRemindersForMonth, // for onShow
      viewReminderDetail,
      createReminderOnSelectedDate,
      formatDisplayTime
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