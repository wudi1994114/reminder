<script setup>
import { ref, onMounted, nextTick, computed, onUnmounted } from 'vue';
import FullCalendar from '@fullcalendar/vue3';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import { reminderState, uiState, showNotification } from '../services/store';
import { getAllSimpleReminders, getAllComplexReminders, updateEvent, getHolidaysByYearRange } from '../services/api';
import { simpleReminderToEvent, complexReminderToEvent } from '../utils/helpers';
import { getSolarTermForDate, getLunarInfo } from '../utils/solarTermHelper';

// Props received from App.vue
const props = defineProps({
  options: {
    type: Object,
    required: true
  },
  events: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
});

// Emits sent to App.vue
const emit = defineEmits([
  'calendar-ready', 
  'event-click', 
  'date-select', 
  'event-drop', 
  'event-resize', 
  'upcoming-reminders-click',
  'toggle-month-selector',
  'complex-reminders-click'
]);

// Local ref for FullCalendar component
const calendarRef = ref(null);
let calendarApi = null;

// 节假日数据
const holidays = ref({});

// 加载节假日数据
const loadHolidays = async (year) => {
  try {
    console.log(`加载 ${year} 年的节假日数据`);
    
    // 检查是否已经有这个年份的数据
    const hasYearData = Object.keys(holidays.value).some(date => date.startsWith(`${year}-`));
    if (hasYearData) {
      console.log(`已有 ${year} 年的节假日数据，跳过加载`);
      return;
    }
    
    const response = await getHolidaysByYearRange(year, year);
    console.log(`收到 ${year} 年的节假日数据:`, response.data);
    
    if (!response.data || response.data.length === 0) {
      console.warn(`${year} 年没有节假日数据`);
      return;
    }
    
    const holidayData = {};
    
    response.data.forEach(holiday => {
      const month = String(holiday.month).padStart(2, '0');
      const day = String(holiday.day).padStart(2, '0');
      const date = `${holiday.year}-${month}-${day}`;
      
      holidayData[date] = {
        holiday: holiday.holiday,
        name: holiday.name
      };
      console.log(`处理 ${date} 的节假日数据:`, holidayData[date]);
    });
    
    // 合并到现有数据中
    holidays.value = { ...holidays.value, ...holidayData };
    console.log('节假日数据更新后:', holidays.value);
    
    // 重要：更新完数据后，重新应用到视图
    if (calendarApi) {
      console.log('重新渲染节假日数据到日历视图');
      updateHolidayDisplay();
    }
  } catch (error) {
    console.error('加载节假日数据失败:', error);
    showNotification('加载节假日数据失败', 'error');
  }
};

// 优化：更新当前视图中的节假日显示
const updateHolidayDisplay = () => {
  if (!calendarApi) {
    console.warn('无法更新节假日显示：日历API未初始化');
    return;
  }
  
  console.log('开始更新节假日显示');
  
  // 获取当前视图中的所有日期单元格
  const dayCells = document.querySelectorAll('.fc-daygrid-day');
  console.log(`找到 ${dayCells.length} 个日期单元格`);
  
  let holidayCount = 0;
  let workdayCount = 0;
  let weekendCount = 0;
  
  dayCells.forEach(cell => {
    // 获取单元格对应的日期
    const dateAttr = cell.getAttribute('data-date');
    if (!dateAttr) {
      console.warn('单元格没有data-date属性，跳过');
      return;
    }
    
    // 检查是否是节假日
    const dayInfo = holidays.value[dateAttr];
    const dateParts = dateAttr.split('-');
    const year = parseInt(dateParts[0]);
    const month = parseInt(dateParts[1]);
    const day = parseInt(dateParts[2]);
    const date = new Date(year, month - 1, day);
    const isWeekend = date.getDay() === 0 || date.getDay() === 6;
    
    // 清除可能存在的旧标签和名称
    const existingLabels = cell.querySelectorAll('.holiday-label, .workday-label, .holiday-name, .workday-name');
    existingLabels.forEach(label => label.remove());
    
    // 移除可能存在的旧类名
    cell.classList.remove('holiday-cell', 'weekend-cell', 'workday-cell');
    
    // 获取日期数字容器
    const dayTop = cell.querySelector('.fc-daygrid-day-top');
    
    if (dayInfo) {
      if (dayInfo.holiday) {
        // 法定节假日样式
        cell.classList.add('holiday-cell');
        
        // 添加日期前的节日名称
        if (dayTop) {
          // 确保dayTop是flex布局，方向是row
          dayTop.style.display = 'flex';
          dayTop.style.flexDirection = 'row';
          dayTop.style.alignItems = 'center';
          dayTop.style.justifyContent = 'flex-start';
          
          const nameElem = document.createElement('div');
          nameElem.className = 'holiday-name';
          nameElem.textContent = dayInfo.name;
          
          // 插入到dayTop的开始位置，但保留日期位置不变
          if (dayTop.childNodes.length > 0) {
            dayTop.insertBefore(nameElem, dayTop.firstChild);
          } else {
            dayTop.appendChild(nameElem);
          }
        }
        
        holidayCount++;
      } else {
        // 法定调休工作日
        cell.classList.add('workday-cell');
        
        // 添加角落的标签
        const label = document.createElement('div');
        label.className = 'workday-label';
        label.textContent = dayInfo.name || '调休工作日';
        cell.appendChild(label);
        
        // 添加日期前的工作日名称
        if (dayTop) {
          // 确保dayTop是flex布局，方向是row
          dayTop.style.display = 'flex';
          dayTop.style.flexDirection = 'row';
          dayTop.style.alignItems = 'center';
          dayTop.style.justifyContent = 'flex-start';
          
          const nameElem = document.createElement('div');
          nameElem.className = 'workday-name';
          nameElem.textContent = dayInfo.name || '调休工作日';
          
          // 插入到dayTop的开始位置，但保留日期位置不变
          if (dayTop.childNodes.length > 0) {
            dayTop.insertBefore(nameElem, dayTop.firstChild);
          } else {
            dayTop.appendChild(nameElem);
          }
        }
        
        workdayCount++;
      }
    } else if (isWeekend) {
      // 普通周末样式
      cell.classList.add('weekend-cell');
      weekendCount++;
    }
  });
  
  console.log(`节假日显示更新完成: ${holidayCount}个节假日, ${workdayCount}个调休工作日, ${weekendCount}个普通周末`);
};

// 将日历单元格渲染分为三个独立的方法
// 1. 渲染节气和农历信息
const renderSolarTermAndLunarInfo = (arg) => {
  const jsDate = arg.date; // JavaScript Date object
  const dateISO = jsDate.toISOString().split('T')[0]; // YYYY-MM-DD 格式
  const dayTop = arg.el.querySelector('.fc-daygrid-day-top');
  
  // 清理旧的节气和农历元素
  const lunarSelectors = ['.fc-day-lunar', '.fc-day-lunar-festival', '.fc-day-solar-term', '.fc-day-lunar-container'];
  lunarSelectors.forEach(selector => {
    const existingEl = arg.el.querySelector(selector);
    if (existingEl) existingEl.remove();
  });
  
  // 移除节气相关的类名
  arg.el.classList.remove('solar-term-cell');
  
  // 确保dayTop是flex布局，以便更好地控制子元素排列
  if (dayTop) {
    dayTop.style.display = 'flex';
    dayTop.style.flexDirection = 'column';
    dayTop.style.alignItems = 'flex-start';
  } else {
    console.warn(`dayTop not found for ${dateISO}`);
    return; // 如果没有找到dayTop，则无法添加节气和农历信息
  }
  
  // 创建农历和节气信息容器
  const lunarInfoContainer = document.createElement('div');
  lunarInfoContainer.className = 'fc-day-lunar-container';
  
  // 获取农历信息
  const lunarDateInfo = getLunarInfo(jsDate);
  if (lunarDateInfo) {
    if (lunarDateInfo.lunarFestival) {
      // 如果有农历节日，优先显示节日
      const lunarFestivalEl = document.createElement('div');
      lunarFestivalEl.className = 'fc-day-lunar-festival';
      lunarFestivalEl.textContent = lunarDateInfo.lunarFestival.split(',')[0]; // 只显示第一个节日
      lunarInfoContainer.appendChild(lunarFestivalEl);
    } else if (lunarDateInfo.lunarDayName) {
      // 否则显示农历日期
      const lunarEl = document.createElement('div');
      lunarEl.className = 'fc-day-lunar';
      lunarEl.textContent = lunarDateInfo.lunarDayName;
      lunarInfoContainer.appendChild(lunarEl);
    }
  }
  
  // 获取并显示节气信息
  const solarTermInfo = getSolarTermForDate(jsDate);
  if (solarTermInfo) {
    arg.el.classList.add('solar-term-cell');
    const solarTermEl = document.createElement('div');
    solarTermEl.className = 'fc-day-solar-term';
    solarTermEl.textContent = solarTermInfo.name;
    lunarInfoContainer.appendChild(solarTermEl);
  }
  
  // 如果有农历或节气信息，将容器添加到dayTop
  if (lunarInfoContainer.hasChildNodes()) {
    dayTop.appendChild(lunarInfoContainer);
  }
};

// 2. 渲染提醒事件红点
const renderReminderDots = (arg) => {
  const jsDate = arg.date;
  const dateISO = jsDate.toISOString().split('T')[0]; // YYYY-MM-DD 格式
  
  // 清理旧的提醒红点元素
  const existingDots = arg.el.querySelectorAll('.reminder-dot');
  existingDots.forEach(dot => dot.remove());
  
  // 检查这一天是否有提醒事项
  const hasSimpleReminders = reminderState.simpleReminders.some(reminder => {
    // 将提醒的事件时间转换为本地日期字符串，格式为YYYY-MM-DD
    const reminderDate = new Date(reminder.eventTime);
    const reminderDateISO = reminderDate.toISOString().split('T')[0];
    return reminderDateISO === dateISO;
  });
  
  const hasComplexReminders = reminderState.complexReminders.some(reminder => {
    // 复杂提醒可能没有具体日期，但可以通过其他方式判断是否在当天触发
    // 这里简化处理，实际可能需要根据cron表达式计算
    return false; // 暂不实现复杂提醒的判断逻辑
  });
  
  // 如果有提醒事项，添加红点标记
  if (hasSimpleReminders || hasComplexReminders) {
    const dotContainer = document.createElement('div');
    dotContainer.className = 'reminder-dot-container';
    
    const dot = document.createElement('div');
    dot.className = 'reminder-dot';
    dotContainer.appendChild(dot);
    
    // 将红点添加到单元格中
    arg.el.appendChild(dotContainer);
  }
};

// 3. 渲染节假日信息
const renderHolidayInfo = (arg) => {
  const jsDate = arg.date;
  const dateISO = jsDate.toISOString().split('T')[0]; // YYYY-MM-DD 格式
  const isWeekend = jsDate.getDay() === 0 || jsDate.getDay() === 6;
  const dayTop = arg.el.querySelector('.fc-daygrid-day-top');
  
  // 清理旧的节假日元素和样式
  const holidaySelectors = ['.holiday-label', '.workday-label', '.holiday-name', '.workday-name'];
  holidaySelectors.forEach(selector => {
    const existingEl = arg.el.querySelector(selector);
    if (existingEl) existingEl.remove();
  });
  
  // 移除节假日相关的类名
  arg.el.classList.remove('holiday-cell', 'workday-cell', 'weekend-cell');
  
  // 获取节假日信息
  const dayInfo = holidays.value[dateISO];
  
  // 处理节假日信息
  if (dayInfo) {
    if (dayInfo.holiday) {
      // 法定节假日
      arg.el.classList.add('holiday-cell');
      
      // 添加"休"标签
      const label = document.createElement('div');
      label.className = 'holiday-label';
      label.textContent = '休';
      arg.el.appendChild(label);
      
      // 添加节假日名称
      if (dayTop) {
        const nameElem = document.createElement('div');
        nameElem.className = 'holiday-name';
        nameElem.textContent = dayInfo.name;
        dayTop.appendChild(nameElem);
      }
    } else {
      // 调休工作日
      arg.el.classList.add('workday-cell');
      
      // 添加"班"标签
      const label = document.createElement('div');
      label.className = 'workday-label';
      label.textContent = '班';
      arg.el.appendChild(label);
    }
  } else if (isWeekend) {
    // 普通周末
    arg.el.classList.add('weekend-cell');
  }
};

// 日历配置
const calendarOptions = computed(() => ({
  ...props.options,
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  initialView: 'dayGridMonth',
  timeZone: 'local',
  headerToolbar: {
    left: 'prevYear,prev,next,nextYear',
    center: 'dayGridMonth,timeGridWeek,timeGridDay',
    right: 'upcomingReminders,complexReminders'
  },
  buttonText: {
    today: '今天',
    month: '月',
    week: '周',
    day: '日',
    upcomingReminders: '即将提醒',
    complexReminders: '复杂提醒'
  },
  customButtons: {
    upcomingReminders: {
      text: '即将提醒',
      click: function() {
        console.log('点击了即将提醒按钮');
        emit('upcoming-reminders-click');
      }
    },
    complexReminders: {
      text: '复杂提醒',
      click: function() {
        console.log('点击了复杂提醒按钮');
        emit('complex-reminders-click');
      }
    }
  },
  locale: 'zh-cn',
  editable: true,
  selectable: true,
  selectMirror: true,
  dayMaxEvents: true,
  events: props.events,
  // 配置不同视图的拖拽行为
  views: {
    dayGridMonth: {
      // 月视图：只允许修改日期，不允许修改时间
      eventStartEditable: true,
      eventDurationEditable: false
    },
    timeGridWeek: {
      // 周视图：允许修改日期和时间
      eventStartEditable: true,
      eventDurationEditable: true
    },
    timeGridDay: {
      // 日视图：只允许修改时间，日期固定为当天
      eventStartEditable: true,
      eventDurationEditable: true
    }
  },
  // 拖拽事件处理 - 直接在主配置中定义
  eventDrop: (info) => {
    console.log('事件被拖拽（CalendarDisplay）:', info);
    console.log('当前视图类型:', info.view?.type || '未知视图');
    console.log('原始事件ID:', info.event.id);
    console.log('原始事件标题:', info.event.title);
    
    // 确保使用本地时区的日期
    const dropWithLocalTime = {
      ...info,
      event: {
        ...info.event,
        id: info.event.id, // 确保ID被复制
        title: info.event.title, // 添加标题
        start: info.event.start ? new Date(info.event.start) : null,
        end: info.event.end ? new Date(info.event.end) : null,
        allDay: info.event.allDay,
        backgroundColor: info.event.backgroundColor,
        borderColor: info.event.borderColor,
        textColor: info.event.textColor,
        classNames: info.event.classNames,
        extendedProps: info.event.extendedProps ? {...info.event.extendedProps} : {}
      }
    };
    
    console.log('处理后的拖拽事件时间(本地):', dropWithLocalTime.event.start, dropWithLocalTime.event.end);
    console.log('处理后的事件ID:', dropWithLocalTime.event.id);
    console.log('处理后的事件标题:', dropWithLocalTime.event.title);
    console.log('事件extendedProps:', dropWithLocalTime.event.extendedProps);
    console.log('将触发event-drop事件');
    
    // 确保将事件传递给父组件
    emit('event-drop', dropWithLocalTime);
  },
  
  // 调整大小事件处理 - 直接在主配置中定义
  eventResize: (info) => {
    console.log('事件大小被调整（CalendarDisplay）:', info);
    console.log('当前视图类型:', info.view?.type || '未知视图');
    console.log('原始事件ID:', info.event.id);
    console.log('原始事件标题:', info.event.title);
    
    // 确保使用本地时区的日期
    const resizeWithLocalTime = {
      ...info,
      event: {
        ...info.event,
        id: info.event.id, // 确保ID被复制
        title: info.event.title, // 添加标题
        start: info.event.start ? new Date(info.event.start) : null,
        end: info.event.end ? new Date(info.event.end) : null,
        allDay: info.event.allDay,
        backgroundColor: info.event.backgroundColor,
        borderColor: info.event.borderColor,
        textColor: info.event.textColor,
        classNames: info.event.classNames,
        extendedProps: info.event.extendedProps ? {...info.event.extendedProps} : {}
      }
    };
    
    console.log('处理后的调整大小事件时间(本地):', resizeWithLocalTime.event.start, resizeWithLocalTime.event.end);
    console.log('处理后的事件ID:', resizeWithLocalTime.event.id);
    console.log('处理后的事件标题:', resizeWithLocalTime.event.title);
    console.log('事件extendedProps:', resizeWithLocalTime.event.extendedProps);
    console.log('将触发event-resize事件');
    
    // 确保将事件传递给父组件
    emit('event-resize', resizeWithLocalTime);
  },
  
  datesSet: async (arg) => {
    console.log('日历视图变化，触发datesSet事件:', arg);
    
    // 触发父组件的主题更新
    emit('toggle-month-selector', {
      view: arg.view,
      start: arg.start,
      end: arg.end
    });
    
    // 获取当前视图的年份范围
    const start = arg.start;
    const end = arg.end;
    const startYear = start.getFullYear();
    const endYear = end.getFullYear();
    
    console.log(`当前视图年份范围: ${startYear}-${endYear}`);
    
    try {
      // 加载视图范围内的所有年份节假日数据
      for (let year = startYear; year <= endYear; year++) {
        await loadHolidays(year);
      }
      
      // 确保节假日样式正确应用到视图中
      nextTick(() => {
        updateHolidayDisplay();
        console.log('完成节假日显示更新');
      });
    } catch (error) {
      console.error('处理节假日数据时出错:', error);
    }
  },
  dayCellDidMount: (arg) => {
    const dateISO = arg.date.toISOString().split('T')[0]; // YYYY-MM-DD 格式
    console.log(`dayCellDidMount for date: ${dateISO}`);
    
    // 清理所有旧样式和元素
    arg.el.classList.remove('holiday-cell', 'workday-cell', 'weekend-cell', 'solar-term-cell');
    const selectorsToRemove = ['.holiday-label', '.workday-label', '.holiday-name', '.workday-name', 
                              '.fc-day-lunar', '.fc-day-lunar-festival', '.fc-day-solar-term', 
                              '.fc-day-lunar-container', '.reminder-dot', '.reminder-dot-container'];
    selectorsToRemove.forEach(selector => {
      const existingEl = arg.el.querySelector(selector);
      if (existingEl) existingEl.remove();
    });
    
    // 1. 渲染节气和农历信息
    renderSolarTermAndLunarInfo(arg);
    
    // 2. 渲染提醒事件红点
    renderReminderDots(arg);
    
    // 3. 渲染节假日信息
    renderHolidayInfo(arg);
  },
  eventClick: (info) => {
    console.log('Event clicked in CalendarDisplay:', info.event);
    console.log('当前视图类型:', info.view?.type || '未知视图');
    console.log('Event original data:', {
      id: info.event.id,
      title: info.event.title,
      extendedProps: info.event.extendedProps || {},
      start: info.event.start,
      end: info.event.end,
      allDay: info.event.allDay
    });
    
    // 确保FullCalendar的事件对象正确转换
    // 这里必须直接使用原始的info对象，因为Event对象有自己的私有属性结构
    // 不能使用解构或复制，否则会丢失数据
    emit('event-click', info);
  },
  select: (info) => {
    console.log('Date selected in CalendarDisplay:', info);
    console.log('当前视图类型:', info.view?.type || '未知视图');
    // 确保使用本地时区的日期
    const selectionWithLocalTime = {
      ...info,
      start: new Date(info.start),
      end: new Date(info.end)
    };
    console.log('处理后的选择时间(本地):', selectionWithLocalTime.start, selectionWithLocalTime.end);
    emit('date-select', selectionWithLocalTime);
  },
  // 添加表头格式，月视图只显示星期
  dayHeaderFormat: { 
    weekday: 'short'
  },
  // 不同视图的表头格式配置
  views: {
    timeGridWeek: {
      dayHeaderFormat: { weekday: 'short', month: 'numeric', day: 'numeric', omitCommas: true }
    },
    timeGridDay: {
      dayHeaderFormat: { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }
    }
  }
}));

// 加载提醒事项
const loadReminders = async () => {
  reminderState.loading = true;
  try {
    // 获取当前日历显示的年月
    const currentDate = calendarApi ? calendarApi.getDate() : new Date();
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth() + 1; // 月份从0开始，需要+1
    
    console.log(`加载 ${year}年${month}月 的提醒事项`);
    
    const [simpleRes, complexRes] = await Promise.all([
      getAllSimpleReminders(year, month), // 传递年月参数
      getAllComplexReminders()
    ]);
    reminderState.simpleReminders = simpleRes.data;
    reminderState.complexReminders = complexRes.data;
  } catch (error) {
    showNotification('加载提醒事项失败', 'error');
    console.error('Failed to load reminders:', error);
  } finally {
    reminderState.loading = false;
  }
};

// 在组件挂载后获取 API 并触发 calendar-ready 事件
onMounted(() => {
  nextTick(() => {
    if (calendarRef.value) {
      calendarApi = calendarRef.value.getApi();
      if (calendarApi) {
        console.log("日历API初始化成功");
        
        // 向父组件发送API引用
        emit('calendar-ready', calendarApi);
        
        // 获取当前视图范围
        const currentView = calendarApi.view;
        const viewStart = currentView.currentStart || currentView.activeStart;
        const viewEnd = currentView.currentEnd || new Date(viewStart.getFullYear(), viewStart.getMonth() + 1, 0);
        
        console.log("当前视图范围:", {
          start: viewStart,
          end: viewEnd
        });
        
        // 发送主题更新事件
        emit('toggle-month-selector', {
          view: {
            currentStart: viewStart,
            activeStart: viewStart,
            start: viewStart,
            type: currentView.type
          },
          start: viewStart,
          end: viewEnd
        });
        
        // 监听日历视图变化事件，切换月份时重新加载提醒
        calendarApi.on('datesSet', (info) => {
          console.log('日历视图变化:', info);
          // 获取新视图的年月
          const newViewDate = info.view.currentStart || info.start;
          const year = newViewDate.getFullYear();
          const month = newViewDate.getMonth() + 1; // 月份从0开始，需要+1
          
          console.log(`视图切换到 ${year}年${month}月，重新加载提醒事项`);
          // 重新加载该月的提醒事项
          loadReminders();
        });
        
        // 加载并应用节假日数据
        const startYear = viewStart.getFullYear();
        const endYear = viewEnd.getFullYear();
        
        console.log(`初始化加载年份范围: ${startYear}-${endYear}`);
        
        // 立即加载当前年份的节假日
        loadHolidays(startYear).then(() => {
          console.log('初始化节假日加载完成');
          // 确保在下一个渲染周期应用节假日样式
          nextTick(() => {
            updateHolidayDisplay();
            console.log('初始化节假日显示更新完成');
          });
        });
        
        // 如果跨年，加载结束年份
        if (endYear > startYear) {
          loadHolidays(endYear);
        }
      } else {
        console.error("获取日历API失败");
      }
    } else {
      console.error("找不到日历组件引用");
    }
  });
  loadReminders();
});

// 确保在组件卸载时清理
onUnmounted(() => {
  calendarApi = null;
});

// Expose the calendar API if needed by the parent
function getApi() {
  return calendarApi;
}

// 添加刷新事件的方法
function refreshEvents() {
  console.log('CalendarDisplay: 刷新事件');
  if (calendarApi) {
    calendarApi.refetchEvents();
    console.log('日历事件已刷新');
  } else {
    console.error('无法刷新事件：日历API未初始化');
  }
}

defineExpose({ getApi, refreshEvents });

// --- Resize Handling (optional, can be managed by parent) ---
// let resizeTimeout = null;
// function debounce(func, wait) { /* ... */ }
// const handleResize = debounce(() => {
//   if (calendarApi) {
//     calendarApi.updateSize();
//   }
// }, 250);

// onMounted(() => {
//   // Get API instance earlier if possible, or wait for mount events
//   // if (calendarRef.value) calendarApi = calendarRef.value.getApi(); 
//   // window.addEventListener('resize', handleResize);
// });

// onUnmounted(() => {
//   // window.removeEventListener('resize', handleResize);
//   // clearTimeout(resizeTimeout);
// });

// 计算当前年份和月份
const currentYear = computed(() => {
  return calendarApi ? calendarApi.getDate().getFullYear() : new Date().getFullYear();
});

const currentMonth = computed(() => {
  const months = ['一月', '二月', '三月', '四月', '五月', '六月',
                 '七月', '八月', '九月', '十月', '十一月', '十二月'];
  const monthIndex = calendarApi ? calendarApi.getDate().getMonth() : new Date().getMonth();
  return months[monthIndex];
});

</script>

<template>
  <div class="calendar-container">
    <div v-if="reminderState.loading" class="loading-overlay">
      <div class="loading-spinner"></div>
    </div>
    
    <FullCalendar
      ref="calendarRef"
      :options="calendarOptions"
      class="calendar-view"
    />
  </div>
</template>

<style scoped>
.calendar-container {
  padding: 0;
  overflow: hidden;
  background-color: #fdfdfd;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 0 0 16px 16px;
}

:deep(.fc) {
  width: 100%;
  height: 100%;
  margin: 0;
  border: none;
  font-family: 'Nunito', sans-serif;
}

:deep(.fc-view-harness) {
  width: 100% !important;
  height: 100% !important;
  background: white;
}

:deep(.fc .fc-daygrid-day) {
  width: calc(100% / 7) !important;
  max-width: none !important;
  min-width: 0 !important;
}

/* 表头样式：所有表头（包括周末）都使用主题色 */
:deep(.fc-col-header-cell-cushion) {
  color: var(--theme-primary-color) !important;
  font-weight: 600 !important;
  padding: 8px 0 !important;
  font-size: 15px !important;
}

/* 法定假日单元格使用主题色的浅色版本 */
:deep(.fc .holiday-cell) {
  background-color: var(--theme-day-hover-bg) !important;
  position: relative;
}

/* 周末单元格使用浅色背景 */
:deep(.fc .weekend-cell),
:deep(.fc-day-sat), 
:deep(.fc-day-sun) {
  background-color: var(--theme-day-hover-bg) !important;
}

/* 法定假日标签使用主题色 */
:deep(.fc .holiday-label) {
  position: absolute;
  top: 2px;
  right: 2px;
  font-size: 10px;
  color: var(--theme-primary-color);
  padding: 1px 4px;
  border-radius: 4px;
  background-color: var(--theme-day-hover-bg);
  z-index: 1;
}

/* 法定假日日期数字使用主题色 */
:deep(.fc .holiday-cell .fc-daygrid-day-number) {
  color: var(--theme-primary-color) !important;
}

/* 日期数字样式 */
:deep(.fc .fc-daygrid-day-number) {
  font-size: 0.95em !important;
  font-weight: 600 !important;
  padding: 6px 8px !important;
  color: #495057 !important;
}

/* 今天日期样式 */
:deep(.fc .fc-day-today) {
  background-color: transparent !important;
}

:deep(.fc .fc-day-today .fc-daygrid-day-number) {
  background-color: var(--theme-today-bg) !important;
  color: white !important;
  border-radius: 50% !important;
  display: inline-block !important;
  width: 28px !important;
  height: 28px !important;
  line-height: 28px !important;
  text-align: center !important;
  padding: 0 !important;
}

:deep(.fc .fc-daygrid-day:hover) {
  background-color: var(--theme-day-hover-bg) !important;
}

/* 工具栏样式 */
:deep(.fc-toolbar-title) {
  display: none !important;
}

/* 减小工具栏内部间距，使按钮组更紧凑 */
:deep(.fc-toolbar.fc-header-toolbar) {
  justify-content: center !important; /* 整体居中 */
  margin-bottom: 0.5em !important;
  padding: 0 0.5rem !important;
  width: 100% !important;
  box-sizing: border-box !important;
  gap: 8px !important; /* 增加组间距 */
  display: flex !important;
  flex-wrap: nowrap !important;
}

/* 优化工具栏块之间的间距 */
:deep(.fc-toolbar-chunk) {
  display: flex !important;
  align-items: center !important;
  padding: 0 !important;
  margin: 0 8px !important; /* 增加外边距 */
  gap: 0.25rem !important; /* 减小按钮组内部间距 */
}

/* 确保工具栏区域不会占用太多空间 */
:deep(.fc-header-toolbar) {
  min-height: 40px !important;
}

/* 按钮样式 */
:deep(.fc .fc-button) {
  background-color: var(--theme-primary-color) !important;
  border: none !important;
  border-radius: 8px !important;
  padding: 6px 10px !important; /* 减小按钮内边距 */
  font-weight: 500 !important;
  font-size: 14px !important;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05) !important;
  transition: all 0.2s ease !important;
}

:deep(.fc-button-group) {
  display: inline-flex !important;
  gap: 2px !important;
  border-radius: 8px !important;
  overflow: hidden !important;
}

:deep(.fc-button-group .fc-button) {
  border-radius: 0 !important;
  margin: 0 !important;
  box-shadow: none !important;
}

:deep(.fc-button-group .fc-button:first-child) {
  border-top-left-radius: 8px !important;
  border-bottom-left-radius: 8px !important;
}

:deep(.fc-button-group .fc-button:last-child) {
  border-top-right-radius: 8px !important;
  border-bottom-right-radius: 8px !important;
}

:deep(.fc-button:not(.fc-button-group .fc-button)) {
  border-radius: 8px !important;
  margin: 0 4px !important;
}

:deep(.fc-button:hover) {
  background-color: var(--theme-hover-color) !important;
  transform: translateY(-1px) !important;
}

:deep(.fc-button:active) {
  background-color: var(--theme-active-color) !important;
  transform: translateY(0) !important;
}

:deep(.fc-button-primary:not(:disabled).fc-button-active),
:deep(.fc-button-primary:not(:disabled):active) {
  background-color: var(--theme-active-color) !important;
}

/* 加载中遮罩 */
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 响应式样式 */
@media (max-width: 768px) {
  :deep(.fc-header-toolbar) {
    flex-wrap: wrap;
    gap: 8px;
    padding: 8px;
  }

  :deep(.fc-button) {
    padding: 6px 10px;
    font-size: 12px;
  }

  :deep(.fc-header-toolbar .fc-toolbar-chunk) {
    flex: 1 1 auto;
    display: flex;
    justify-content: center;
  }

  :deep(.fc-header-toolbar .fc-toolbar-chunk:last-child) {
    margin-left: 0;
    margin-top: 8px;
    width: 100%;
  }
}

/* 工作日标签样式 */
:deep(.fc .workday-label) {
  position: absolute;
  top: 2px;
  right: 2px;
  font-size: 10px;
  color: #666;
  padding: 1px 4px;
  border-radius: 4px;
  background-color: rgba(200, 200, 200, 0.1);
  z-index: 1;
}

/* 表头背景样式 */
:deep(.fc-col-header-cell) {
  background-color: #ffffff !important;
}

/* 调休工作日单元格样式 - 显示为普通工作日 */
:deep(.fc .workday-cell) {
  background-color: #ffffff !important; /* 普通工作日背景色 */
}

/* 周末日期数字颜色与普通工作日保持一致 */
:deep(.fc .weekend-cell .fc-daygrid-day-number),
:deep(.fc-day-sat .fc-daygrid-day-number),
:deep(.fc-day-sun .fc-daygrid-day-number) {
  color: #495057 !important; /* 与普通工作日日期数字颜色一致 */
}

/* 修改日期单元格样式，将节日名称显示在日期数字前面 */
:deep(.fc .holiday-cell .fc-daygrid-day-top),
:deep(.fc .workday-cell .fc-daygrid-day-top) {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  padding-top: 5px;
}

:deep(.holiday-name),
:deep(.workday-name) {
  margin-right: 0;
  margin-left: 2px;
  flex-shrink: 1;
  min-width: 0;
  text-align: left;
}

:deep(.fc-daygrid-day-number) {
  margin-left: auto;
  flex-shrink: 0;
}

/* 节日名称标签样式 */
:deep(.holiday-name) {
  font-size: 10px;
  color: var(--theme-primary-color);
  margin-right: 4px;
  text-align: left;
  font-weight: 500;
  line-height: 1.2;
  max-width: 60%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  padding: 0 2px;
}

/* 调休工作日名称标签样式 */
:deep(.workday-name) {
  font-size: 10px;
  color: #666;
  margin-right: 4px;
  text-align: left;
  font-weight: 500;
  line-height: 1.2;
  max-width: 60%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  padding: 0 2px;
}

/* 调整日期数字位置和样式 */
:deep(.fc .fc-daygrid-day-number) {
  padding: 6px 8px !important;
}

/* 确保角落的标签仍然显示 */
:deep(.fc .holiday-label),
:deep(.fc .workday-label) {
  font-size: 9px;
  padding: 0px 4px;
  opacity: 0.8;
}

/* FullCalendar 更多事件弹窗样式定制 */
:deep(.fc-popover.fc-more-popover) {
  background-color: white;
  border-radius: 8px;
  border: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  padding: 0;
  min-width: 220px;
  animation: popoverFadeIn 0.2s ease-out;
}

/* 弹窗头部 */
:deep(.fc-popover-header) {
  background-color: var(--theme-primary-color);
  color: white;
  padding: 10px 12px;
  font-weight: 500;
  font-size: 14px;
  border-bottom: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 头部标题 */
:deep(.fc-popover-title) {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
}

/* 关闭按钮 */
:deep(.fc-popover-close) {
  font-size: 16px;
  color: white;
  opacity: 0.8;
  cursor: pointer;
  background: none;
  border: none;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}

:deep(.fc-popover-close:hover) {
  opacity: 1;
  background-color: rgba(255, 255, 255, 0.2);
}

/* 弹窗内容 */
:deep(.fc-popover-body) {
  padding: 10px;
  max-height: 300px;
  overflow-y: auto;
}

/* 内容中的事件项 */
:deep(.fc-popover-body .fc-event) {
  margin-bottom: 5px;
  border-radius: 4px;
  border: none;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all 0.2s;
}

:deep(.fc-popover-body .fc-event:hover) {
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.15);
  transform: translateY(-1px);
}

:deep(.fc-popover-body .fc-event:last-child) {
  margin-bottom: 0;
}

/* 事件内容 */
:deep(.fc-popover-body .fc-event-title) {
  font-size: 12px;
  padding: 3px 6px;
}

/* 弹窗入场动画 */
@keyframes popoverFadeIn {
  from {
    opacity: 0;
    transform: translateY(-5px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 移动端适配 */
@media (max-width: 480px) {
  :deep(.fc-popover.fc-more-popover) {
    min-width: 180px;
    max-width: 300px;
  }
  
  :deep(.fc-popover-header) {
    padding: 8px 10px;
  }
  
  :deep(.fc-popover-title) {
    font-size: 13px;
  }
  
  :deep(.fc-popover-body) {
    padding: 8px;
    max-height: 250px;
  }
}

/* 提醒事件红点样式 */
:deep(.reminder-dot-container) {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none; /* 确保不会阻止点击事件 */
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 2px;
  box-sizing: border-box;
}

:deep(.reminder-dot) {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #ff4d4f; /* 红色 */
  margin: 2px;
  box-shadow: 0 0 2px rgba(0, 0, 0, 0.2);
}

/* 节气和农历信息容器样式 */
:deep(.fc-day-lunar-container) {
  font-size: 0.75em; /* 较小的字体 */
  color: #6c757d;   /* 柔和的颜色 */
  margin-top: 2px;
  line-height: 1.2;
  width: 100%;
}

/* 农历日期样式 */
:deep(.fc-day-lunar) {
  font-size: 0.9em;
  color: #6c757d;
}

/* 农历节日样式 */
:deep(.fc-day-lunar-festival) {
  color: #e91e63; /* 节日使用醒目的颜色 */
  font-weight: bold;
}

/* 节气样式 */
:deep(.fc-day-solar-term) {
  color: #007bff; /* 节气使用另一种醒目的颜色 */
  font-weight: bold;
}

/* 标记节气日的单元格 (可选，用于特殊背景等) */
:deep(.solar-term-cell) {
  /* background-color: #e7f3ff; */ /* 例如，给节气日一个淡淡的背景色 */
}
</style> 