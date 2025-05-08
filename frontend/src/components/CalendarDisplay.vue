<script setup>
import { ref, onMounted, nextTick, computed, onUnmounted } from 'vue';
import FullCalendar from '@fullcalendar/vue3';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import { reminderState, uiState, showNotification } from '../services/store';
import { getAllSimpleReminders, getAllComplexReminders, updateEvent, getHolidaysByYearRange } from '../services/api';
import { simpleReminderToEvent, complexReminderToEvent } from '../utils/helpers';

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
  'toggle-month-selector'
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

// 日历配置
const calendarOptions = computed(() => ({
  ...props.options,
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  initialView: 'dayGridMonth',
  timeZone: 'local',
  headerToolbar: {
    left: 'prevYear,prev,next,nextYear',
    center: '',
    right: 'dayGridMonth,timeGridWeek,timeGridDay'
  },
  buttonText: {
    today: '今天',
    month: '月',
    week: '周',
    day: '日'
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
    
    // 确保使用本地时区的日期
    const dropWithLocalTime = {
      ...info,
      event: {
        ...info.event,
        id: info.event.id, // 确保ID被复制
        start: info.event.start ? new Date(info.event.start) : null,
        end: info.event.end ? new Date(info.event.end) : null,
        extendedProps: info.event.extendedProps ? {...info.event.extendedProps} : {}
      }
    };
    
    console.log('处理后的拖拽事件时间(本地):', dropWithLocalTime.event.start, dropWithLocalTime.event.end);
    console.log('处理后的事件ID:', dropWithLocalTime.event.id);
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
    
    // 确保使用本地时区的日期
    const resizeWithLocalTime = {
      ...info,
      event: {
        ...info.event,
        id: info.event.id, // 确保ID被复制
        start: info.event.start ? new Date(info.event.start) : null,
        end: info.event.end ? new Date(info.event.end) : null,
        extendedProps: info.event.extendedProps ? {...info.event.extendedProps} : {}
      }
    };
    
    console.log('处理后的调整大小事件时间(本地):', resizeWithLocalTime.event.start, resizeWithLocalTime.event.end);
    console.log('处理后的事件ID:', resizeWithLocalTime.event.id);
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
    const year = arg.date.getFullYear();
    const month = String(arg.date.getMonth() + 1).padStart(2, '0');
    const day = String(arg.date.getDate()).padStart(2, '0');
    const date = `${year}-${month}-${day}`;
    
    const isWeekend = arg.date.getDay() === 0 || arg.date.getDay() === 6;
    const dayInfo = holidays.value[date];
    
    console.log(`渲染日期单元格 ${date}`, dayInfo ? `节假日信息: ${JSON.stringify(dayInfo)}` : '无节假日信息');
    
    // 清除可能存在的旧标签和名称
    const existingLabels = arg.el.querySelectorAll('.holiday-label, .workday-label, .holiday-name, .workday-name');
    existingLabels.forEach(label => label.remove());
    
    // 移除可能存在的旧类名
    arg.el.classList.remove('holiday-cell', 'weekend-cell', 'workday-cell');
    
    // 设置日期属性，便于后续更新
    arg.el.setAttribute('data-date', date);
    
    if (dayInfo) {
      // 获取日期数字容器
      const dayTop = arg.el.querySelector('.fc-daygrid-day-top');
      
      // 确保dayTop是flex布局，方向是row
      if (dayTop) {
        dayTop.style.display = 'flex';
        dayTop.style.flexDirection = 'row';
        dayTop.style.alignItems = 'center';
        dayTop.style.justifyContent = 'flex-start';
      }
      
      if (dayInfo.holiday) {
        // 法定节假日样式 - 使用主题色
        arg.el.classList.add('holiday-cell');
        
        // 添加角落的标签
        const label = document.createElement('div');
        label.className = 'holiday-label';
        label.textContent = dayInfo.name;
        arg.el.appendChild(label);
        
        // 添加日期前的节日名称
        if (dayTop) {
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
        
        console.log(`应用节假日样式到 ${date}: ${dayInfo.name}`);
      } else {
        // 法定调休工作日 - 标记为普通工作日
        arg.el.classList.add('workday-cell');
        
        // 添加角落的标签
        const label = document.createElement('div');
        label.className = 'workday-label';
        label.textContent = dayInfo.name || '调休工作日';
        arg.el.appendChild(label);
        
        // 添加日期前的工作日名称
        if (dayTop) {
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
        
        console.log(`应用调休工作日样式到 ${date}: ${dayInfo.name || '调休工作日'}`);
        
        // 如果这一天原本是周末，移除周末样式
        arg.el.classList.remove('weekend-cell');
      }
    } else if (isWeekend) {
      // 普通周末样式 - 使用主题浅色
      arg.el.classList.add('weekend-cell');
    }
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
    const [simpleRes, complexRes] = await Promise.all([
      getAllSimpleReminders(),
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

:deep(.fc-toolbar.fc-header-toolbar) {
  justify-content: space-between !important;
  padding: 0 1rem !important;
  width: 100% !important;
  box-sizing: border-box !important;
  margin: 0 !important;
}

:deep(.fc-toolbar-chunk) {
  display: flex !important;
  align-items: center !important;
  gap: 0.5rem !important;
}

/* 按钮样式 */
:deep(.fc .fc-button) {
  background-color: var(--theme-primary-color) !important;
  border: none !important;
  border-radius: 8px !important;
  padding: 8px 12px !important;
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
</style> 