<template>
  <view class="page-container">
    <view class="content-wrapper">
      <view class="calendar-wrapper" :key="forceRefreshKey">
        <view class="calendar-container">
          <view class="month-nav">
            <button class="nav-btn" @click="previousMonth">
              <text class="nav-arrow">‹</text>
            </button>
            <text class="month-title">{{ getMonthTitle() }}</text>
            <button class="nav-btn" @click="nextMonth">
              <text class="nav-arrow">›</text>
            </button>
          </view>
          
          <view class="weekdays">
            <text class="weekday-label" v-for="day in weekdayLabels" :key="day">{{ day }}</text>
          </view>
          
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
    
    <view class="bottom-actions">
      <button class="add-btn" @click="createReminderOnSelectedDate" :disabled="!selectedDate">
        <text class="add-icon">+</text>
        <text class="add-text">添加提醒</text>
      </button>
    </view>
    
    <!-- 全局登录弹窗 -->
    <GlobalLoginModal />
  </view>
</template>

<script>
import { ref, computed, onMounted, watch, shallowRef, onUnmounted } from 'vue';
import { getAllSimpleReminders, getHolidaysByYearRange } from '@/services/api';
import { formatTime } from '@/utils/dateFormat';
import { getLunarInfo } from '@/utils/lunarManager';
import { requireAuth, isAuthenticated, checkAuthAndClearData, clearAllUserData } from '@/utils/auth';
import { globalDataVersion } from '@/services/reminderCache';
import GlobalLoginModal from '@/components/GlobalLoginModal.vue';

export default {
  components: {
    GlobalLoginModal
  },
  data() {
    return {
      isFirstShow: true,
    };
  },
  
  onShow() {
    console.log('日历页面显示，检查登录状态');
    
    // 检查登录状态并清空数据
    if (!checkAuthAndClearData('日历页面-onShow')) {
      return;
    }
    
    if (this.isFirstShow) {
      this.isFirstShow = false;
      return;
    }
    
    this.refreshCalendarData();
  },

  setup() {
    const currentCalendarDisplayTime = ref({
      year: new Date().getFullYear(),
      month: new Date().getMonth() + 1,
    });
    const selectedDate = ref(new Date());
    const loadingRemindersForDate = ref(false);
    const allRemindersInCurrentMonth = shallowRef([]);
    const holidaysInCurrentYear = shallowRef([]);
    const forceRefreshKey = ref(0);
    const localDataVersion = ref(0);

    const weekdayLabels = ['日', '一', '二', '三', '四', '五', '六'];
    const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
    
    const monthNames = [
      '一月', '二月', '三月', '四月', '五月', '六月',
      '七月', '八月', '九月', '十月', '十一月', '十二月'
    ];

    const calendarCache = new Map();

    const calendarDates = computed(() => {
      const { year, month } = currentCalendarDisplayTime.value;
      const cacheKey = `${year}-${month}-${allRemindersInCurrentMonth.value.length}`;
      if (calendarCache.has(cacheKey)) {
          return calendarCache.get(cacheKey);
      }
      
      const firstDay = new Date(year, month - 1, 1);
      const startDate = new Date(firstDay);
      startDate.setDate(startDate.getDate() - firstDay.getDay());
      
      const dates = [];
      const selectedDateStr = selectedDate.value ? 
        `${selectedDate.value.getFullYear()}-${selectedDate.value.getMonth() + 1}-${selectedDate.value.getDate()}` : '';
      
      const reminderDateMap = new Map();
      allRemindersInCurrentMonth.value.forEach(reminder => {
        if (!reminder.eventTime) return;
        let reminderDateTime = reminder.eventTime.replace(' ', 'T');
        const reminderDate = new Date(reminderDateTime);
        if (!isNaN(reminderDate.getTime())) {
          const dateKey = `${reminderDate.getFullYear()}-${reminderDate.getMonth()}-${reminderDate.getDate()}`;
          reminderDateMap.set(dateKey, true);
        }
      });
      
      for (let i = 0; i < 42; i++) {
        const currentDate = new Date(startDate);
        currentDate.setDate(startDate.getDate() + i);
        const isCurrentMonth = currentDate.getMonth() === month - 1;
        const dateKey = `${currentDate.getFullYear()}-${currentDate.getMonth()}-${currentDate.getDate()}`;
        
        dates.push({
          key: dateKey,
          day: currentDate.getDate(),
          date: new Date(currentDate),
          isSelected: `${currentDate.getFullYear()}-${currentDate.getMonth() + 1}-${currentDate.getDate()}` === selectedDateStr,
          hasReminder: reminderDateMap.has(dateKey),
          isOtherMonth: !isCurrentMonth
        });
      }
      
      if (calendarCache.size > 10) {
        calendarCache.delete(calendarCache.keys().next().value);
      }
      calendarCache.set(cacheKey, dates);
      
      return dates;
    });

    const selectedDateReminders = computed(() => {
        if (!selectedDate.value) return [];
        const selected = selectedDate.value;
        return allRemindersInCurrentMonth.value
            .filter(r => {
                if (!r.eventTime) return false;
                const d = new Date(r.eventTime.replace(' ', 'T'));
                return d.getFullYear() === selected.getFullYear() &&
                       d.getMonth() === selected.getMonth() &&
                       d.getDate() === selected.getDate();
            })
            .sort((a, b) => a._timestamp - b._timestamp)
            .map(r => ({...r, isPast: r._timestamp < Date.now()}));
    });

    const loadRemindersForMonth = async (year, month) => {
      if (!isAuthenticated()) {
        allRemindersInCurrentMonth.value = [];
        return;
      }
      
      try {
        const simpleReminders = await getAllSimpleReminders(year, month);
        const rawReminders = Array.isArray(simpleReminders) ? simpleReminders : [];
        allRemindersInCurrentMonth.value = rawReminders.map(reminder => {
          if (!reminder.eventTime) return { ...reminder, _timestamp: 0 };
          let dateTime = reminder.eventTime.replace(' ', 'T');
          const timestamp = new Date(dateTime).getTime();
          return { ...reminder, _timestamp: isNaN(timestamp) ? 0 : timestamp };
        });
      } catch (error) {
        console.error('加载提醒事项失败:', error);
        allRemindersInCurrentMonth.value = [];
      }
    };
    
    const getMonthTitle = () => {
      const { year, month } = currentCalendarDisplayTime.value;
      return `${monthNames[month - 1]} ${year}`;
    };

    const getHolidayForDate = (date) => {
      if (!date || !holidaysInCurrentYear.value.length) return null;
      const dateStr = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
      return holidaysInCurrentYear.value.find(holiday => holiday.dateStr === dateStr);
    };

    const getSelectedDateTitle = () => {
      if (!selectedDate.value) return '';
      const d = selectedDate.value;
      const today = new Date();
      const isToday = d.toDateString() === today.toDateString();
      const title = isToday ? '今天' : `${d.getMonth() + 1}月${d.getDate()}日 星期${weekdays[d.getDay()]}`;
      
      try {
        const lunarInfo = getLunarInfo(d);
        const holidayInfo = getHolidayForDate(d);
        let additionalInfo = [`${lunarInfo.lunarMonthName}${lunarInfo.lunarDayName}`];
        if (lunarInfo.jieQi) additionalInfo.push(lunarInfo.jieQi);
        if (lunarInfo.lunarFestival) additionalInfo.push(lunarInfo.lunarFestival);
        if (holidayInfo) additionalInfo.push(holidayInfo.name);
        return `${title} (${additionalInfo.join(' ')})`;
      } catch {
        return title;
      }
    };
    
    const previousMonth = async () => {
      const { year, month } = currentCalendarDisplayTime.value;
      const newDate = new Date(year, month - 2, 1);
      currentCalendarDisplayTime.value = { year: newDate.getFullYear(), month: newDate.getMonth() + 1 };
      await loadRemindersForMonth(currentCalendarDisplayTime.value.year, currentCalendarDisplayTime.value.month);
      if (currentCalendarDisplayTime.value.year !== year) {
        await loadHolidaysForYear(currentCalendarDisplayTime.value.year);
      }
    };
    
    const nextMonth = async () => {
      const { year, month } = currentCalendarDisplayTime.value;
      const newDate = new Date(year, month, 1);
      currentCalendarDisplayTime.value = { year: newDate.getFullYear(), month: newDate.getMonth() + 1 };
      await loadRemindersForMonth(currentCalendarDisplayTime.value.year, currentCalendarDisplayTime.value.month);
      if (currentCalendarDisplayTime.value.year !== year) {
        await loadHolidaysForYear(currentCalendarDisplayTime.value.year);
      }
    };
    
    const loadRemindersForSelectedDate = (dateObj) => {
      if (!dateObj) return;
      loadingRemindersForDate.value = true;
      const targetStart = new Date(dateObj).setHours(0, 0, 0, 0);
      const targetEnd = new Date(dateObj).setHours(23, 59, 59, 999);
      const now = Date.now();
      
      selectedDateReminders.value = allRemindersInCurrentMonth.value
        .filter(r => r._timestamp >= targetStart && r._timestamp <= targetEnd)
        .map(r => ({ ...r, isPast: r._timestamp < now }));
        
      loadingRemindersForDate.value = false;
    };

    const selectDate = (dateObj) => {
      selectedDate.value = dateObj.date;
      loadRemindersForSelectedDate(dateObj.date);
    };
    
    const toggleReminderStatus = (reminder) => {
      console.log('切换提醒状态:', reminder);
    };

    const viewReminderDetail = async (reminderId) => {
      const authenticated = await requireAuth();
      
      if (authenticated) {
        uni.navigateTo({
          url: `/pages/detail/detail?id=${reminderId}`
        });
      }
    };
    
    const createReminderOnSelectedDate = async () => {
      const authenticated = await requireAuth();
      
      if (authenticated && selectedDate.value) {
        const year = selectedDate.value.getFullYear();
        const month = String(selectedDate.value.getMonth() + 1).padStart(2, '0');
        const day = String(selectedDate.value.getDate()).padStart(2, '0');
        const dateStr = `${year}-${month}-${day}`;
        
        uni.navigateTo({
          url: `/pages/create/create?date=${dateStr}`
        });
      }
    };

    const formatDisplayTime = (dateTimeStr) => {
        if (!dateTimeStr) return '';
        return formatTime(dateTimeStr);
    };
    
    const loadHolidaysForYear = async (year) => {
      console.log(`正在加载 ${year} 年的节日数据`);
      try {
        const holidays = await getHolidaysByYearRange(year, year);
        holidaysInCurrentYear.value = Object.freeze(
          (Array.isArray(holidays) ? holidays : []).map(h => ({
            ...h,
            dateStr: `${h.year}-${String(h.month).padStart(2, '0')}-${String(h.day).padStart(2, '0')}`
          }))
        );
      } catch (error) {
        console.error("获取节日数据失败:", error);
        holidaysInCurrentYear.value = [];
      }
    };
    
    const refreshCalendarData = async () => {
      console.log('日历页面: [onShow] 触发数据刷新');
      try {
        if (typeof window !== 'undefined' && window._calendarCache) {
          window._calendarCache.clear();
        }
        await loadRemindersForMonth(currentCalendarDisplayTime.value.year, currentCalendarDisplayTime.value.month);
        if (selectedDate.value) {
          loadRemindersForSelectedDate(selectedDate.value);
        }
        forceRefreshKey.value++;
        console.log('日历页面: 数据刷新完成');
      } catch (error) {
        console.error('日历页面: 刷新数据失败:', error);
      }
    };

    onMounted(async () => {
      console.log('日历页面挂载，开始加载数据');
      
      // 使用统一的登录检查和数据清理
      if (!checkAuthAndClearData('日历页面-onMounted')) {
        return;
      }
      
      await loadRemindersForMonth(currentCalendarDisplayTime.value.year, currentCalendarDisplayTime.value.month);
      await loadHolidaysForYear(currentCalendarDisplayTime.value.year);
      loadRemindersForSelectedDate(selectedDate.value);
    });
    
    // 监听用户登出事件，清理日历数据
    uni.$on('userLogout', () => {
      console.log('日历页面：收到用户登出事件，清理所有数据');
      
      // 清空日历相关数据
      allRemindersInCurrentMonth.value = [];
      selectedDateReminders.value = [];
      holidaysInCurrentYear.value = [];
      loadingRemindersForDate.value = false;
      
      // 清空缓存
      if (typeof window !== 'undefined' && window._calendarCache) {
        window._calendarCache.clear();
      }
      
      // 强制刷新日历显示
      forceRefreshKey.value++;
      
      console.log('✅ 日历页面：数据清理完成');
    });
    
    // 组件销毁时清理事件监听器
    onUnmounted(() => {
      uni.$off('userLogout');
    });
    
    return {
      currentCalendarDisplayTime,
      selectedDate,
      selectedDateReminders,
      loadingRemindersForDate,
      weekdayLabels,
      calendarDates,
      forceRefreshKey,
      getMonthTitle,
      getSelectedDateTitle,
      previousMonth,
      nextMonth,
      selectDate,
      toggleReminderStatus,
      viewReminderDetail,
      createReminderOnSelectedDate,
      formatDisplayTime,
      refreshCalendarData,
    };
  }
};
</script>

<style scoped>
.page-container {
  min-height: 100vh;
  background-color: #fcfbf8;
  display: flex;
  flex-direction: column;
  font-family: 'Manrope', 'Noto Sans', sans-serif;
  position: relative;
}

.content-wrapper {
  flex: 1;
  padding-bottom: 110rpx;
}

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

.reminders-section {
  background-color: #fcfbf8;
  padding: 16rpx 32rpx;
}

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

@media (max-width: 750rpx) {
  .content-wrapper {
    padding-bottom: 90rpx;
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

.reminder-meta {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 8rpx;
}

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

.past-indicator {
  color: #ff4757;
  font-size: 24rpx;
  font-weight: 500;
  background-color: rgba(255, 71, 87, 0.1);
  padding: 4rpx 12rpx;
  border-radius: 16rpx;
  white-space: nowrap;
}

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