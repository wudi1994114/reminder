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
import { ref, computed, onMounted, watch, shallowRef } from 'vue';
// 从后端服务 API 模块中导入获取简单提醒列表的函数
import { getAllSimpleReminders, getHolidaysByYearRange } from '../../services/api';
// 从工具函数模块中导入格式化时间的函数
import { formatTime } from '../../utils/helpers';
// 导入节气和农历工具
import { getLunarInfo, getSolarTermForDate } from '../../utils/lunarManager';
// 导入性能监控工具
import performanceMonitor, { pagePerformanceHelper } from '../../utils/performanceMonitor';
// 默认导出一个 Vue 组件对象
export default {
  // uni-app 页面的生命周期钩子，页面显示时触发
  onShow() { 
    // 开始监控页面显示性能
    pagePerformanceHelper.startPageLoad();
    
    // 只有在页面重新显示时才刷新数据（避免与 onMounted 重复）
    const isInitialized = typeof window !== 'undefined' && window._calendarInitialized;
    if (isInitialized) {
      // 页面显示时，刷新当前月份的提醒数据
      this.loadRemindersForMonth(this.currentCalendarDisplayTime.year, this.currentCalendarDisplayTime.month);
      // 如果之前已经有选中的日期，则重新加载该日期的提醒事项
      if (this.selectedDate) {
        this.loadRemindersForSelectedDate(this.selectedDate);
      }
    }
    
    // 使用 nextTick 确保 DOM 更新完成后再结束计时
    this.$nextTick(() => {
      performanceMonitor.end('page_load');
      if (isInitialized) {
        performanceMonitor.printReport();
      }
    });
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
    // 当前月份的所有提醒（使用 shallowRef 减少响应式开销）
    const allRemindersInCurrentMonth = shallowRef([]);
    // 当前年份的所有节日数据
    const holidaysInCurrentYear = shallowRef([]);

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
      // 开始监控日历计算性能
      const startTime = Date.now();
      
      const { year, month } = currentCalendarDisplayTime.value;
      
      // 添加缓存键
      const cacheKey = `${year}-${month}-${allRemindersInCurrentMonth.value.length}`;
      
      // 静态缓存（在组件实例间共享）
      if (typeof window !== 'undefined') {
        if (!window._calendarCache) window._calendarCache = new Map();
        if (window._calendarCache.has(cacheKey)) {
          const cached = window._calendarCache.get(cacheKey);
          performanceMonitor.record('calendar_dates_computation', Date.now() - startTime, 'ms');
          return cached;
        }
      }
      const firstDay = new Date(year, month - 1, 1);
      const startDate = new Date(firstDay);
      startDate.setDate(startDate.getDate() - firstDay.getDay());
      
      const dates = [];
      const today = new Date();
      const selectedDateStr = selectedDate.value ? 
        `${selectedDate.value.getFullYear()}-${selectedDate.value.getMonth() + 1}-${selectedDate.value.getDate()}` : '';
      
      // 缓存提醒数据以减少重复查找
      const reminders = allRemindersInCurrentMonth.value;
      const reminderDateMap = new Map();
      
      // 预处理提醒数据，建立日期映射
      reminders.forEach(reminder => {
        if (!reminder.eventTime) return;
        
        let reminderDateTime = reminder.eventTime;
        if (reminderDateTime.includes(' ') && !reminderDateTime.includes('T')) {
          reminderDateTime = reminderDateTime.replace(' ', 'T');
        }
        
        const reminderDate = new Date(reminderDateTime);
        if (!isNaN(reminderDate.getTime())) {
          const dateKey = `${reminderDate.getFullYear()}-${reminderDate.getMonth()}-${reminderDate.getDate()}`;
          if (!reminderDateMap.has(dateKey)) {
            reminderDateMap.set(dateKey, true);
          }
        }
      });
      
      // 生成日历日期
      for (let i = 0; i < 42; i++) {
        const currentDate = new Date(startDate);
        currentDate.setDate(startDate.getDate() + i);
        
        const dateStr = `${currentDate.getFullYear()}-${currentDate.getMonth() + 1}-${currentDate.getDate()}`;
        const isCurrentMonth = currentDate.getMonth() === month - 1;
        const isSelected = dateStr === selectedDateStr;
        
        // 使用预处理的映射检查是否有提醒
        const dateKey = `${currentDate.getFullYear()}-${currentDate.getMonth()}-${currentDate.getDate()}`;
        const hasReminder = reminderDateMap.has(dateKey);
        
        dates.push({
          key: `${currentDate.getFullYear()}-${currentDate.getMonth()}-${currentDate.getDate()}`,
          day: currentDate.getDate(),
          date: new Date(currentDate),
          isSelected,
          hasReminder,
          isOtherMonth: !isCurrentMonth
        });
      }
      
      // 缓存结果（限制缓存大小）
      if (typeof window !== 'undefined' && window._calendarCache) {
        if (window._calendarCache.size > 10) {
          const firstKey = window._calendarCache.keys().next().value;
          window._calendarCache.delete(firstKey);
        }
        window._calendarCache.set(cacheKey, dates);
      }
      
      // 记录日历计算性能
      const duration = Date.now() - startTime;
      performanceMonitor.record('calendar_dates_computation', duration, 'ms');
      
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
      
      // 开始监控月份数据加载
      performanceMonitor.start('load_month_reminders', `加载${year}-${month}月份提醒`);
      
      try {
        // 监控简单提醒加载
        performanceMonitor.start('load_simple_reminders', '获取简单提醒');
        const simpleReminders = await getAllSimpleReminders(year, month);
        
        // 结束数据加载监控
        performanceMonitor.end('load_simple_reminders');
        
        console.log('获取到的简单提醒数据:', simpleReminders);
        
        // 监控数据处理性能
        performanceMonitor.start('process_reminders_data', '处理提醒数据');
        
        // 确保数据是数组格式
        const rawReminders = Array.isArray(simpleReminders) ? simpleReminders : [];
        
        // 优化：预处理时间戳，避免重复计算
        performanceMonitor.start('preprocess_timestamps', '预处理时间戳');
        const processedReminders = rawReminders.map(reminder => {
          if (!reminder.eventTime) {
            return { ...reminder, _timestamp: 0, _formattedDateTime: null };
          }
          
          // 统一处理日期格式，只处理一次
          let dateTime = reminder.eventTime;
          if (dateTime.includes(' ') && !dateTime.includes('T')) {
            dateTime = dateTime.replace(' ', 'T');
          }
          
          const timestamp = new Date(dateTime).getTime();
          return {
            ...reminder,
            _timestamp: isNaN(timestamp) ? 0 : timestamp,
            _formattedDateTime: dateTime // 缓存格式化后的日期字符串
          };
        });
        performanceMonitor.end('preprocess_timestamps');
        
        // 优化：使用预计算的时间戳进行排序
        performanceMonitor.start('sort_reminders', '排序提醒数据');
        processedReminders.sort((a, b) => a._timestamp - b._timestamp);
        performanceMonitor.end('sort_reminders');
        
        // 冻结数据，减少响应式开销
        const allReminders = Object.freeze(processedReminders);
        
        // 更新当前月份的所有提醒数据
        allRemindersInCurrentMonth.value = allReminders;
        console.log(`${year}-${month} 月份提醒总数:`, allReminders.length);
        
        performanceMonitor.end('process_reminders_data');
        
        // 如果在加载完月份数据后，已经有一个日期被选中，
        // 则需要刷新该选中日期的提醒列表，以确保显示的是最新的数据。
        if (selectedDate.value) {
            loadRemindersForSelectedDate(selectedDate.value);
        }

        performanceMonitor.end('load_month_reminders');

      } catch (error) {
        performanceMonitor.end('load_month_reminders');
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
    
    // 获取选中日期标题（优化版本：使用缓存）
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
      
      // 优化：使用缓存避免重复计算农历信息
      const dateKey = `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
      
      if (typeof window !== 'undefined') {
        if (!window._lunarCache) window._lunarCache = new Map();
        
        if (window._lunarCache.has(dateKey)) {
          const cachedLunar = window._lunarCache.get(dateKey);
          return title + cachedLunar;
        }
      }
      
      try {
        // 监控农历信息获取性能
        performanceMonitor.start('get_lunar_info', '获取农历信息');
        const lunarInfo = getLunarInfo(d);
        performanceMonitor.end('get_lunar_info');
        
        console.log('=== 农历信息详细调试 ===');
        console.log('选中日期:', d.toISOString().split('T')[0]);
        console.log('完整农历信息:', lunarInfo);
        console.log('农历月份名:', lunarInfo.lunarMonthName);
        console.log('农历日期名:', lunarInfo.lunarDayName);
        console.log('节气信息:', lunarInfo.jieQi);
        console.log('农历节日:', lunarInfo.lunarFestival);
        console.log('节日是否为空:', !lunarInfo.lunarFestival);
        console.log('节日trim后:', lunarInfo.lunarFestival ? lunarInfo.lunarFestival.trim() : 'null');
        
        const lunarText = `${lunarInfo.lunarMonthName}${lunarInfo.lunarDayName}`;
        
        // 构建附加信息
        let additionalInfo = [];
        additionalInfo.push(lunarText);
        
        console.log('初始附加信息:', additionalInfo);
        
        // 添加节气信息（农历信息中已经包含了节气）
        if (lunarInfo.jieQi && lunarInfo.jieQi.trim()) {
          additionalInfo.push(lunarInfo.jieQi);
          console.log('添加节气后:', additionalInfo);
        }
        
        // 添加农历节日信息
        if (lunarInfo.lunarFestival && lunarInfo.lunarFestival.trim()) {
          additionalInfo.push(lunarInfo.lunarFestival);
          console.log('添加农历节日后:', additionalInfo);
        } else {
          console.log('农历节日信息为空，未添加');
        }
        
        // 添加法定节日信息
        const holidayInfo = getHolidayForDate(d);
        if (holidayInfo && holidayInfo.name) {
          additionalInfo.push(holidayInfo.name);
          console.log('添加法定节日后:', additionalInfo);
          console.log('法定节日信息:', holidayInfo);
        } else {
          console.log('法定节日信息为空，未添加');
        }
        
        console.log('最终附加信息:', additionalInfo);
        console.log('=== 调试结束 ===');
        
        // 缓存农历信息
        const lunarSuffix = additionalInfo.length > 0 ? ` (${additionalInfo.join(' ')})` : '';
        
        console.log('最终显示文本:', title + lunarSuffix);
        
        // 限制缓存大小
        if (typeof window !== 'undefined' && window._lunarCache) {
          if (window._lunarCache.size > 50) {
            const firstKey = window._lunarCache.keys().next().value;
            window._lunarCache.delete(firstKey);
          }
          window._lunarCache.set(dateKey, lunarSuffix);
        }
        
        return title + lunarSuffix;
        
      } catch (error) {
        console.warn('获取农历或节气信息失败:', error);
        return title;
      }
    };
    
    // 上一个月
    const previousMonth = () => {
      const { year, month } = currentCalendarDisplayTime.value;
      const newYear = month === 1 ? year - 1 : year;
      const newMonth = month === 1 ? 12 : month - 1;
      
      currentCalendarDisplayTime.value = { year: newYear, month: newMonth };
      loadRemindersForMonth(newYear, newMonth);
      
      // 如果年份变化，重新加载节日数据
      if (newYear !== year) {
        loadHolidaysForYear(newYear);
      }
    };
    
    // 下一个月
    const nextMonth = () => {
      const { year, month } = currentCalendarDisplayTime.value;
      const newYear = month === 12 ? year + 1 : year;
      const newMonth = month === 12 ? 1 : month + 1;
      
      currentCalendarDisplayTime.value = { year: newYear, month: newMonth };
      loadRemindersForMonth(newYear, newMonth);
      
      // 如果年份变化，重新加载节日数据
      if (newYear !== year) {
        loadHolidaysForYear(newYear);
      }
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

      // 优化：使用预处理的时间戳和缓存的日期字符串
      const targetDate = new Date(year, parseInt(month) - 1, parseInt(day));
      const targetDateStart = new Date(targetDate).setHours(0, 0, 0, 0);
      const targetDateEnd = new Date(targetDate).setHours(23, 59, 59, 999);

      const dayReminders = allRemindersInCurrentMonth.value.filter(reminder => {
        if (!reminder._timestamp || reminder._timestamp === 0) return false;
        
        // 使用时间戳范围比较，避免重复日期解析
        return reminder._timestamp >= targetDateStart && reminder._timestamp <= targetDateEnd;
      });
      
      // 优化：使用预处理的时间戳进行排序和比较
      selectedDateReminders.value = dayReminders
        .map(reminder => ({
          ...reminder,
          isPast: reminder._timestamp < now.getTime()
        }))
        .sort((a, b) => a._timestamp - b._timestamp);
      
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
    
    // 定义一个异步函数，用于加载指定年份的节日数据
    const loadHolidaysForYear = async (year) => {
      console.log(`正在加载 ${year} 年的节日数据`);
      
      // 开始监控节日数据加载
      performanceMonitor.start('load_holidays', `加载${year}年节日数据`);
      
      try {
        // 获取当前年份的节日数据
        const holidays = await getHolidaysByYearRange(year, year);
        
        console.log('获取到的节日数据:', holidays);
        
        // 确保数据是数组格式
        const rawHolidays = Array.isArray(holidays) ? holidays : [];
        
        // 预处理节日数据，建立日期映射
        const processedHolidays = rawHolidays.map(holiday => {
          const dateStr = `${holiday.year}-${String(holiday.month).padStart(2, '0')}-${String(holiday.day).padStart(2, '0')}`;
          return {
            ...holiday,
            dateStr: dateStr,
            isHoliday: holiday.holiday === true
          };
        });
        
        // 冻结数据，减少响应式开销
        const allHolidays = Object.freeze(processedHolidays);
        
        // 更新当前年份的节日数据
        holidaysInCurrentYear.value = allHolidays;
        console.log(`${year} 年节日总数:`, allHolidays.length);
        
        performanceMonitor.end('load_holidays');

      } catch (error) {
        performanceMonitor.end('load_holidays');
        console.error("获取节日数据失败:", error);
        // 清空相关数据，避免显示旧的或错误的数据
        holidaysInCurrentYear.value = [];
      }
    };

    // 获取指定日期的法定节日信息
    const getHolidayForDate = (date) => {
      if (!date || holidaysInCurrentYear.value.length === 0) {
        return null;
      }
      
      const year = date.getFullYear();
      const month = date.getMonth() + 1;
      const day = date.getDate();
      const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      
      return holidaysInCurrentYear.value.find(holiday => holiday.dateStr === dateStr);
    };
    
    // 组件挂载后执行的生命周期钩子
    onMounted(async () => {
      // 开始监控组件初始化性能
      performanceMonitor.start('component_mount', '组件挂载和初始化');
      
      // 标记组件已初始化
      if (typeof window !== 'undefined') {
        window._calendarInitialized = true;
      }
      
      try {
        // 异步加载数据，不阻塞 UI 渲染
        const loadDataPromise = loadRemindersForMonth(
          currentCalendarDisplayTime.value.year, 
          currentCalendarDisplayTime.value.month
        );
        
        // 加载当前年份的节日数据
        const loadHolidaysPromise = loadHolidaysForYear(
          currentCalendarDisplayTime.value.year
        );
        
        // 如果有选中日期，准备加载该日期的提醒（等数据加载完成后）
        const selectedDatePromise = selectedDate.value ? 
          loadDataPromise.then(() => loadRemindersForSelectedDate(selectedDate.value)) : 
          Promise.resolve();
        
        // 等待所有数据加载完成
        await Promise.all([loadDataPromise, loadHolidaysPromise, selectedDatePromise]);
        
        performanceMonitor.end('component_mount');
        
        // 输出初始化性能报告
        console.log('\n🚀 [日历页面] 初始化性能报告:');
        performanceMonitor.printReport();
        
      } catch (error) {
        performanceMonitor.end('component_mount');
        console.error('组件初始化失败:', error);
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
      loadHolidaysForYear,
      getHolidayForDate,
      viewReminderDetail,
      createReminderOnSelectedDate,
      formatDisplayTime,
      
      // 性能调试方法（开发环境使用）
      showPerformanceReport: () => {
        console.log('\n📊 [性能调试] 当前性能统计:');
        performanceMonitor.printReport();
      },
      clearPerformanceLog: () => {
        performanceMonitor.clear();
        console.log('✅ [性能调试] 性能日志已清空');
      }
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