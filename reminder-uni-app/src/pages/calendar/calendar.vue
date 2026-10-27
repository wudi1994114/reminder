<template>
  <view class="page-container">
    <view class="content-wrapper">
      <view class="calendar-wrapper" :key="forceRefreshKey">
        <view class="calendar-container">
          <view class="month-nav">
            <button class="nav-btn" aria-label="上一个月" @click="previousMonth">
              <text class="nav-arrow">‹</text>
            </button>
            <text class="month-title">{{ getMonthTitle() }}</text>
            <button class="nav-btn" aria-label="下一个月" @click="nextMonth">
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
                'date-has-record': recordDates.includes(date.localDateKey),
                'date-other-month': date.isOtherMonth
              }"
              :aria-label="`${date.localDateKey}${recordDates.includes(date.localDateKey) ? '，有每日记录' : ''}`"
              @click="selectDate(date)"
            >
              <view class="date-content">{{ date.day }}</view>
            </button>
          </view>
        </view>
      </view>

      <view v-if="selectedDate" class="selected-day-heading">
        <text class="section-title">{{ getSelectedDateTitle() }}</text>
        <view v-if="recordDates.length" class="calendar-record-legend"><view class="record-legend-mark"></view><text>有记录</text></view>
      </view>
      <DailyRecordPanel v-if="selectedDate" ref="dailyRecordPanel" :date="selectedDateKey" :month="displayMonthKey" @record-dates="updateRecordDates" />
      
      <view class="reminders-section" v-if="selectedDate">
        <view class="reminders-header">
          <text class="section-title reminder-section-title">提醒<text v-if="selectedDateReminders.length" class="reminder-count"> {{ selectedDateReminders.length }}</text></text>
          <text v-if="!loadingRemindersForDate && !selectedDateReminders.length" class="inline-empty">暂无安排</text>
          <button class="add-btn" @click="createReminderOnSelectedDate" :disabled="!selectedDate"><text class="add-icon" aria-hidden="true">+</text><text>添加提醒</text></button>
        </view>
        
        <view v-if="loadingRemindersForDate" class="loading-state">
          <text class="loading-text">加载提醒...</text>
        </view>
        <view v-else-if="selectedDateReminders.length" class="reminders-list">
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
    
    <!-- 全局登录弹窗 -->
    <GlobalLoginModal />
  </view>
</template>

<script>
import { ref, computed, onMounted, watch, shallowRef, onUnmounted } from 'vue';
import { getAllSimpleReminders, getHolidaysByYearRange } from '@/services/api';
import { formatTime } from '@/utils/date/format';
import { getLunarInfo } from '@/utils/lunarManager';
import { requireAuth, isAuthenticated, checkAuthAndClearData, clearAllUserData } from '@/utils/auth';
import { globalDataVersion } from '@/services/reminderCache';
import GlobalLoginModal from '@/components/GlobalLoginModal.vue';
import DailyRecordPanel from '@/components/DailyRecordPanel.vue';
import { dateKey } from '@/utils/daily-records.mjs';
import { monthGridDates } from '@/utils/calendar-grid.mjs';

export default {
  components: {
    GlobalLoginModal,
    DailyRecordPanel
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

    this.$refs.dailyRecordPanel?.refresh();
    
    if (this.isFirstShow) {
      this.isFirstShow = false;
      return;
    }
    
    this.refreshCalendarData();
  },

  onHide() {
    this.$refs.dailyRecordPanel?.onPageHide();
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
    const recordDates = ref([]);
    const selectedDateKey = computed(() => dateKey(selectedDate.value));
    const displayMonthKey = computed(() => `${currentCalendarDisplayTime.value.year}-${String(currentCalendarDisplayTime.value.month).padStart(2, '0')}`);
    const updateRecordDates = dates => { recordDates.value = dates; };

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
      
      // 生成选中日期的字符串（用于比较）
      const selectedDateStr = selectedDate.value ? 
        `${selectedDate.value.getFullYear()}-${selectedDate.value.getMonth() + 1}-${selectedDate.value.getDate()}` : '';
      
      // 从缓存获取或生成日期数组
      let dates;
      if (calendarCache.has(cacheKey)) {
        // 从缓存获取，但需要重新计算 isSelected 状态
        dates = calendarCache.get(cacheKey).map(date => ({
          ...date,
          isSelected: `${date.date.getFullYear()}-${date.date.getMonth() + 1}-${date.date.getDate()}` === selectedDateStr
        }));
      } else {
        // 生成新的日期数组
        dates = [];
        
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
        
        for (const currentDate of monthGridDates(year, month)) {
          const isCurrentMonth = currentDate.getMonth() === month - 1;
          const dateKey = `${currentDate.getFullYear()}-${currentDate.getMonth()}-${currentDate.getDate()}`;
          
          dates.push({
            key: dateKey,
            localDateKey: `${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, '0')}-${String(currentDate.getDate()).padStart(2, '0')}`,
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
      }
      
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
    
    const selectDate = async (dateObj) => {
      selectedDate.value = dateObj.date;
      const year = dateObj.date.getFullYear(), month = dateObj.date.getMonth() + 1;
      const previousYear = currentCalendarDisplayTime.value.year;
      if (year !== previousYear || month !== currentCalendarDisplayTime.value.month) {
        currentCalendarDisplayTime.value = { year, month };
        await loadRemindersForMonth(year, month);
        if (year !== previousYear) await loadHolidaysForYear(year);
      }
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
    });
    
    // 监听用户登出事件，清理日历数据
    uni.$on('userLogout', () => {
      console.log('日历页面：收到用户登出事件，清理所有数据');
      
      // 清空日历相关数据
      allRemindersInCurrentMonth.value = [];
      recordDates.value = [];
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
      recordDates,
      selectedDateKey,
      displayMonthKey,
      updateRecordDates,
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
.page-container { min-height: calc(100vh - var(--window-top, 0px) - var(--window-bottom, 0px)); display: flex; flex-direction: column; background: #fcfbf8; color: #1c170d; }
.content-wrapper { width: 100%; max-width: 640px; margin: 0 auto; padding-bottom: 12px; }
.calendar-wrapper { padding: 4px 12px 8px; }
.calendar-container { min-width: 0; }
.month-nav { display: flex; align-items: center; justify-content: space-between; min-height: 44px; }
.nav-btn { display: flex; align-items: center; justify-content: center; width: 44px; min-height: 44px; padding: 0; color: #1c170d; background: transparent; }
.nav-arrow { font-size: 26px; line-height: 32px; }
.month-title { flex: 1; font-size: 17px; font-weight: 600; line-height: 24px; text-align: center; }
.weekdays, .dates-grid { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); }
.weekday-label { display: flex; align-items: center; justify-content: center; height: 28px; font-size: 12px; color: #6d604c; font-weight: 500; }
.date-btn { display: flex; align-items: center; justify-content: center; position: relative; width: 100%; height: 44px; min-height: 44px; padding: 0; color: #1c170d; font-size: 14px; line-height: 20px; background: transparent; }
.date-content { display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; border-radius: 50%; transition: background-color .15s ease-out; }
.date-selected .date-content { background: #f7bd4a; font-weight: 600; }
.date-other-month { color: #9b8b72; }
.date-has-reminder::after { content: ''; position: absolute; bottom: 3px; left: calc(50% - 3px); width: 6px; height: 6px; border-radius: 50%; background: #c64d3d; }
.date-has-record::before { content: ''; position: absolute; bottom: 4px; left: calc(50% - 6px); width: 12px; height: 3px; border-radius: 2px; background: #78551c; }
.date-has-record.date-has-reminder::before { left: calc(50% + 2px); }
.date-has-record.date-has-reminder::after { left: calc(50% - 8px); }
.date-btn:active .date-content { background: #e6a73d; }
.selected-day-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; min-height: 36px; padding: 0 16px; }
.section-title { min-width: 0; color: #1c170d; font-size: 14px; font-weight: 600; line-height: 20px; }
.selected-day-heading > .section-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.calendar-record-legend { flex-shrink: 0; display: flex; align-items: center; gap: 4px; color: #6d604c; font-size: 11px; line-height: 16px; }
.record-legend-mark { width: 12px; height: 3px; border-radius: 2px; background: #78551c; }
.reminders-section { margin: 0 16px; }
.reminders-header { display: flex; align-items: center; gap: 8px; min-height: 44px; }
.reminder-count { color: #6d604c; font-size: 12px; font-weight: 400; }
.inline-empty { color: #6d604c; font-size: 13px; line-height: 20px; }
.add-btn { display: flex; align-items: center; justify-content: center; gap: 4px; min-height: 44px; margin-left: auto; padding: 0 10px; border-radius: 6px; color: #78551c; background: transparent; font-size: 13px; line-height: 20px; font-weight: 600; }
.add-icon { font-size: 20px; font-weight: 400; }
.loading-state { display: flex; align-items: center; min-height: 40px; }
.loading-text { font-size: 13px; color: #6d604c; }
.reminders-list { display: flex; flex-direction: column; }
.reminder-item { display: flex; align-items: center; gap: 12px; min-height: 56px; padding: 8px 0; border-bottom: 1px solid #e9dfce; }
.reminder-info { display: flex; flex: 1; min-width: 0; flex-direction: column; gap: 4px; }
.reminder-title { font-size: 14px; font-weight: 500; line-height: 20px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.reminder-meta { display: flex; align-items: center; gap: 8px; }
.reminder-time { font-size: 12px; line-height: 18px; color: #6d604c; }
.reminder-description { font-size: 13px; line-height: 18px; color: #6d604c; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow-wrap: anywhere; }
.reminder-checkbox { display: flex; align-items: center; justify-content: center; flex-shrink: 0; width: 44px; height: 44px; }
.custom-checkbox { color: #b68424; }
.past-indicator { padding: 1px 4px; border-radius: 4px; background: #fff0ec; color: #9e4536; font-size: 11px; line-height: 16px; }
.title-past { color: #6d604c; text-decoration: line-through; }
.time-past, .desc-past { color: #756751; }
button { margin: 0; cursor: pointer; }
.add-btn { margin-left: auto; }
button::after { border: 0; }
button:focus-visible { outline: 2px solid #8b6018; outline-offset: 2px; }
button[disabled] { opacity: .55; }
.nav-btn:active, .add-btn:active, .reminder-item:active { background-color: #f4ead4; }
@media (hover: hover) { .nav-btn:hover, .add-btn:hover { background-color: #f5ecd9; } }
@media (max-width: 350px) { .calendar-wrapper { padding-left: 8px; padding-right: 8px; } .selected-day-heading { padding: 0 12px; gap: 8px; } .reminders-section { margin: 0 12px; } }
@media (prefers-reduced-motion: reduce) { .date-content { transition: none; } }
</style>
