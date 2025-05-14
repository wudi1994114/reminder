<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick, computed } from 'vue'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import LoginView from './components/LoginView.vue';
import AppHeader from './components/AppHeader.vue';
import CalendarDisplay from './components/CalendarDisplay.vue';
import EventModal from './components/EventModal.vue';
import UserProfileModal from './components/UserProfileModal.vue';
import UpcomingRemindersModal from './components/UpcomingRemindersModal.vue';
import ComplexReminderModal from './components/ComplexReminderModal.vue';
import ComplexReminderListModal from './components/ComplexReminderListModal.vue';
import RegisterView from './components/RegisterView.vue';
import { 
  login,
  register,
  fetchUserProfile, 
  getAllSimpleReminders,
  createEvent,
  updateEvent,
  deleteEvent,
  getUserProfile,
  createComplexReminder,
  getAllComplexReminders,
  updateComplexReminder,
  deleteComplexReminder as deleteComplexReminderApi
} from './services/api.js';
import { userState, reminderState, uiState, showNotification } from './services/store'

// 移除所有显式的 CSS 导入，依赖 Vite 和插件自动处理
// import '@fullcalendar/common/main.css' 

// --- 新增：登录状态管理 ---
const isLoggedIn = ref(false);
const loginError = ref(null);
const currentUserProfile = ref(null); // 存储登录用户信息 { id, email, nickname }

// Add registration state
const showRegisterForm = ref(false);
const registerUsername = ref('');
const registerPassword = ref('');
const registerNickname = ref(''); // Need nickname for registration
const registerEmail = ref('');    // Need email for registration
const registerError = ref(null);

const allEvents = ref([]);
// 新增：用于事件编辑模态框的数据
const currentEventForModal = ref(null);
// 新增：标记当前是否处于编辑状态
const isEditingEvent = ref(false);

// 新增：用于控制 CalendarDisplay 渲染的状态
const isCalendarOptionsReady = ref(false);

// --- 保留：每月主题颜色定义 ---
const monthThemes = [
  { primary: '#a8dadc', hover: '#92cdd0', active: '#7cbcc0', today: '#a8dadc', dayHoverBg: '#e1f5f6' }, // 1月: 浅蓝绿
  { primary: '#fec89a', hover: '#fdbb84', active: '#fcad6e', today: '#fec89a', dayHoverBg: '#fff0e1' }, // 2月: 柔和橙
  { primary: '#a0c4ff', hover: '#8ab8ff', active: '#74aaff', today: '#a0c4ff', dayHoverBg: '#e6f0ff' }, // 3月: 淡蓝
  { primary: '#b0f2c2', hover: '#9cefcb', active: '#88ebba', today: '#b0f2c2', dayHoverBg: '#e1fbee' }, // 4月: 薄荷绿
  { primary: '#ffcad4', hover: '#ffbbca', active: '#ffaabf', today: '#ffcad4', dayHoverBg: '#ffeef2' }, // 5月: 粉红
  { primary: '#fff3b0', hover: '#ffef9b', active: '#ffeb85', today: '#fff3b0', dayHoverBg: '#fffcee' }, // 6月: 淡黄
  { primary: '#ffafcc', hover: '#ffa0c0', active: '#ff90b4', today: '#ffafcc', dayHoverBg: '#ffebf2' }, // 7月: 桃粉
  { primary: '#dda15e', hover: '#d19550', active: '#c48a42', today: '#dda15e', dayHoverBg: '#f5eade' }, // 8月: 赭石
  { primary: '#adc178', hover: '#a1b76a', active: '#95ac5c', today: '#adc178', dayHoverBg: '#eaf0d9' }, // 9月: 橄榄绿
  { primary: '#f7d6e0', hover: '#f4c7d4', active: '#f1b8c8', today: '#f7d6e0', dayHoverBg: '#fdeff5' }, // 10月: 浅粉紫
  { primary: '#bccbe0', hover: '#acbfd6', active: '#9cb3cc', today: '#bccbe0', dayHoverBg: '#eef2f7' }, // 11月: 灰蓝
  { primary: '#e0fbfc', hover: '#d0f7fa', active: '#c0f2f8', today: '#e0fbfc', dayHoverBg: '#f0feff' }  // 12月: 冰蓝
];


// 新增：用于年月下拉选择的状态
const currentCalendarDate = ref(new Date()); // 存储当前日历视图的日期
const selectedYear = ref(new Date().getFullYear());
const selectedMonth = ref(new Date().getMonth() + 1); // 月份用 1-12

// 恢复：控制自定义月份选择器显隐的状态
const showMonthSelectorPopup = ref(false);

// 恢复：月份名称映射和计算属性
const monthNames = [
  '一月', '二月', '三月', '四月', '五月', '六月',
  '七月', '八月', '九月', '十月', '十一月', '十二月'
];
const currentMonthName = computed(() => {
    if (selectedMonth.value >= 1 && selectedMonth.value <= 12) {
        return monthNames[selectedMonth.value - 1];
    }
    return '';
});

// --- 新增：跳转到上/下一年函数 ---
function prevYear() {
  if (calendarApi.value) { 
    calendarApi.value.prevYear(); 
     console.log("Navigated to previous year.");
  } else {
      console.error("Cannot navigate prev year: Calendar API not available.");
  }
}
function nextYear() {
  if (calendarApi.value) { 
    calendarApi.value.nextYear(); 
    console.log("Navigated to next year.");
  } else {
      console.error("Cannot navigate next year: Calendar API not available.");
  }
}

// --- 修改：状态管理 (更新时间字段) ---
const showModal = ref(false);
const newEvent = ref({
  title: '',
  description: '',
  reminderType: 'EMAIL',
  selectedDate: '', // YYYY-MM-DD
  selectedHour: 0,   // 0-23
  selectedMinute: 0, // 0-59
  selectedSecond: 0, // 0-59
  recurrenceType: 'NONE', 
  intervalValue: 1,     
  intervalUnit: 'DAYS'   
});

// --- 新增：用户个人资料模态框状态和数据 ---
const showUserProfileModal = ref(false); // 控制模态框显示
const userProfileFormData = ref({}); // 用于编辑的表单数据副本

// 新增：控制即将提醒模态框显示
const showUpcomingRemindersModal = ref(false);

// 新增：控制复杂提醒模态框显示
const showComplexReminderModal = ref(false);
const complexReminderData = ref({});
// 新增：用于存储当前编辑的复杂提醒数据
const currentComplexReminderForModal = ref({});

// 新增：控制复杂提醒列表模态框显示
const showComplexReminderListModal = ref(false);

// --- 日历 API 引用 ---
const calendarRef = ref(null);
// let calendarApi = null; // 改为 ref 以便在 setup 中直接访问
const calendarApi = ref(null); // <--- 修改：使用 ref
// 添加对CalendarDisplay组件的引用
const calendarDisplayRef = ref(null);

// Debounce 函数
function debounce(func, wait) {
  return function(...args) {
    clearTimeout(resizeTimeout);
    resizeTimeout = setTimeout(() => func.apply(this, args), wait);
  }
}

// --- 优化：主题更新函数，增强兼容性 ---
function updateThemeAndSelector(view) {
  console.log('Updating theme and selector with view:', view);
  
  // 处理不同格式的视图对象
  let date;
  if (view.currentStart) {
    // FullCalendar 提供的标准视图对象
    date = view.currentStart;
  } else if (view.start) {
    // 有些情况下通过 start 属性提供日期
    date = view.start;
  } else if (view.activeStart) {
    // 有些情况下通过 activeStart 属性提供日期
    date = view.activeStart;
  } else {
    console.error('无法从视图对象中获取有效日期:', view);
    // 尝试回退到当前日历日期
    if (calendarApi.value) {
      date = calendarApi.value.getDate();
      console.log('使用日历API当前日期作为回退:', date);
    } else {
      // 最后的回退方案: 使用当前日期
      date = new Date();
      console.log('使用系统当前日期作为回退:', date);
    }
  }
  
  currentCalendarDate.value = date;
  const year = date.getFullYear();
  const month = date.getMonth(); // 0-11
  
  console.log(`应用主题 - 年份: ${year}, 月份: ${month + 1}`);
  
  // 更新年月选择器的值
  selectedYear.value = year;
  selectedMonth.value = month + 1; // 更新为 1-12
  
  // 更新主题颜色
  const currentTheme = monthThemes[month];
  if (currentTheme) {
    console.log('应用主题颜色:', currentTheme);
    const rootStyle = document.documentElement.style;
    rootStyle.setProperty('--theme-primary-color', currentTheme.primary);
    rootStyle.setProperty('--theme-hover-color', currentTheme.hover);
    rootStyle.setProperty('--theme-active-color', currentTheme.active);
    rootStyle.setProperty('--theme-today-bg', currentTheme.today);
    rootStyle.setProperty('--theme-day-hover-bg', currentTheme.dayHoverBg);
  } else {
    console.error(`找不到月份 ${month + 1} 对应的主题颜色`);
  }
}

// 处理日历视图变化
function handleCalendarDatesSet(info) {
  console.log("Calendar dates set event received with info:", info);
  updateThemeAndSelector(info.view);
}

// --- 新增：用户个人资料模态框函数 ---
function openUserProfileModal() {
  if (!currentUserProfile.value) {
    console.error("Cannot open profile modal: user data unavailable.");
    handleLogout();
    return;
  }
  userProfileFormData.value = {
    username: currentUserProfile.value.nickname || '',
    avatarUrl: currentUserProfile.value.avatarUrl || 'https://via.placeholder.com/40', 
    email: currentUserProfile.value.email || '',
    phone: currentUserProfile.value.phone || '' 
  };
  showUserProfileModal.value = true;
}

function closeUserProfileModal() {
  showUserProfileModal.value = false;
  // 清空表单数据（可选）
  userProfileFormData.value = {}; 
}

async function saveUserProfile() {
  if (!isLoggedIn.value || !currentUserProfile.value) {
    alert("请先登录！");
    handleLogout();
    return;
  }
  console.log("Attempting to save user profile for user ID:", currentUserProfile.value.id);

  // TODO: Implement backend API call for saving profile in api.js and call it here
  // Example:
  // try {
  //   const dataToUpdate = {
  //      nickname: userProfileFormData.value.username,
  //      avatarUrl: userProfileFormData.value.avatarUrl,
  //      phone: userProfileFormData.value.phone
  //      // email/username usually not editable or handled differently
  //   };
  //   const response = await updateUserProfile(dataToUpdate); // Function to add in api.js
  //   currentUserProfile.value = response.data; // Update local state with response
  //   closeUserProfileModal();
  //   alert('用户信息更新成功！');
  // } catch (error) { ... handle error, maybe logout ... }

  // --- Simulation ---
  const updatedUserData = {
    ...currentUserProfile.value,
    nickname: userProfileFormData.value.username,
    avatarUrl: userProfileFormData.value.avatarUrl,
    phone: userProfileFormData.value.phone // Assuming phone exists in profile DTO later
  };
  currentUserProfile.value = updatedUserData; // Update local state
  console.log("User profile updated locally (simulation). Needs backend integration for saving.");
  closeUserProfileModal();
   alert('用户信息已在本地更新（模拟）。');
  // --- End Simulation ---
}

onMounted(() => {
  // if (calendarRef.value) { // CalendarDisplay 可能还未挂载
  //   calendarApi = calendarRef.value.getApi();
  // }
  // 打印初始屏幕尺寸
  console.log(`Screen Width: ${window.innerWidth}, Screen Height: ${window.innerHeight}`);
  checkAuthStatus(); // 调用检查函数

  // 初始化 selectedYear 和 selectedMonth 为当前日期
  const now = new Date();
  selectedYear.value = now.getFullYear();
  selectedMonth.value = now.getMonth() + 1;
  currentCalendarDate.value = now; // 初始化日历日期状态

  // 恢复：添加 click outside 监听器
  document.addEventListener('click', handleClickOutside);
  
  // 监听弹窗状态变化
  watch(() => uiState.isEventModalOpen, (newVal) => {
    console.log('弹窗状态变化:', newVal);
    if (newVal) {
      console.log('弹窗打开，数据:', currentEventForModal.value);
    }
  });
});

onUnmounted(() => {
  // 组件卸载时移除监听器
  clearTimeout(resizeTimeout); // 清理可能存在的 timeout
  // 恢复：移除 click outside 监听器
  document.removeEventListener('click', handleClickOutside);
});

// --- 实现：认证相关函数 --- 
async function checkAuthStatus() {
  console.log("App.vue: Checking auth status...");
  // 如果已经在检查中，避免重复检查
  if (userState.loading) {
    console.log("Auth check already in progress, skipping...");
    return;
  }

  registerError.value = null;
  loginError.value = null;
  userState.loading = true;

  try {
    const token = localStorage.getItem('accessToken');
    console.log("Token found:", token ? "Yes" : "No");
    
    if (token) {
      console.log("Token found in localStorage, validating format...");
      // 确保 token 格式正确
      if (!token.startsWith('Bearer ')) {
        const formattedToken = `Bearer ${token}`;
        localStorage.setItem('accessToken', formattedToken);
        console.log("Token format corrected with Bearer prefix");
      }
      
      try {
        const response = await fetchUserProfile();
        if(response.data) {
          currentUserProfile.value = response.data;
          console.log("User profile loaded:", currentUserProfile.value);
          isLoggedIn.value = true;
          // 只有在成功获取用户信息后才加载事件
          await loadEvents();
        } else {
          console.warn("Auth token found, but no user info in response.");
          handleLogout(false); // 传入 false 表示不重新加载
        }
      } catch (error) {
        console.error("Error during initial auth check:", error);
        handleLogout(false); // 传入 false 表示不重新加载
      }
    } else {
      console.log("No token found, setting logged out state.");
      handleLogout(false); // 传入 false 表示不重新加载
    }
  } finally {
    userState.loading = false;
    // 确保日历选项在认证检查完成后准备就绪
    isCalendarOptionsReady.value = true;
  }
}

// --- NEW: Function to fetch and set user profile ---
async function fetchAndSetUserProfile() {
  const token = localStorage.getItem('accessToken');
  if (!token) {
    console.log("No token found, cannot fetch profile.");
    handleLogout(); // Ensure clean state if no token
    return;
  }
  try {
    console.log("Attempting to fetch user profile...");
    const response = await fetchUserProfile(); // Call the service function
    currentUserProfile.value = response.data; // Store the fetched profile
    isLoggedIn.value = true; // 确保设置登录状态为true
    console.log("User profile fetched:", currentUserProfile.value);

    // After successfully fetching profile, load user-specific events
    await loadEvents(); // 加载用户事件

  } catch (error) {
    console.error("Failed to fetch user profile:", error.response ? error.response.data : error.message);
    loginError.value = "无法加载用户信息，请重新登录。";
    // If profile fetch fails (e.g., token expired), log the user out
    handleLogout();
  }
}

// Modified login handler to use username
async function handleLogin(credentials) {
  console.log("App.vue: Login attempt received with username:", credentials.username);
  loginError.value = null;
  registerError.value = null;
  try {
    const response = await login(credentials);
    console.log("Login response:", response);
    
    const { accessToken } = response.data;
    if (!accessToken) {
      throw new Error('登录响应中没有找到 token');
    }

    // 确保 token 格式正确并存储
    const tokenToStore = accessToken.startsWith('Bearer ') ? accessToken : `Bearer ${accessToken}`;
    localStorage.setItem('accessToken', tokenToStore);
    console.log("Token stored in localStorage");

    // 更新认证状态
    userState.isAuthenticated = true;
    isLoggedIn.value = true;

    // 立即获取用户信息
    await fetchAndSetUserProfile();

    if (currentUserProfile.value) {
      console.log("Login successful, profile fetched.");
      showNotification('登录成功', 'success');
      await loadEvents();
    } else {
      console.warn("Login seemed successful, but failed to fetch profile.");
      loginError.value = "登录成功，但加载用户信息失败。";
      handleLogout(false);
    }

  } catch (error) {
    console.error("Login failed:", error);
    console.error("Error response:", error.response);
    loginError.value = error.response?.data?.message || "登录失败，请检查用户名和密码。";
    handleLogout(false);
  }
}

function handleLogout(shouldReload = true) {
  console.log("App.vue: Logging out...");
  // 清除存储的认证信息
  localStorage.removeItem('accessToken');
  localStorage.removeItem('currentUser');
  
  // 重置所有状态
  currentUserProfile.value = null;
  isLoggedIn.value = false;
  userState.isAuthenticated = false;
  userState.user = null;
  allEvents.value = []; 
  
  // 清除错误信息
  loginError.value = null;
  showRegisterForm.value = false;
  registerError.value = null;
  registerUsername.value = '';
  registerPassword.value = '';
  registerNickname.value = '';
  registerEmail.value = '';
  
  // 关闭所有模态框
  uiState.isEventModalOpen = false;
  uiState.isProfileModalOpen = false;
  
  // 只在需要时重新加载页面
  if (shouldReload) {
    window.location.reload();
  }
}

// --- 修改：API 调用函数，使用 apiClient 并处理认证错误 ---
async function loadEvents() {
  if (!isLoggedIn.value) {
    console.log("未登录，跳过加载事项");
    allEvents.value = [];
    return;
  }
  
  console.log("开始加载事项...");
  try {
    const token = localStorage.getItem('accessToken');
    console.log("当前 token:", token);
    
    // 获取当前日历的年月
    const currentDate = calendarApi.value 
      ? calendarApi.value.getDate() 
      : new Date();
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth() + 1; // 月份从0开始，需要+1
    
    console.log(`加载 ${year}年${month}月 的提醒事项`);
    
    // 同时加载简单提醒和复杂提醒
    const [simpleResponse, complexResponse] = await Promise.all([
      getAllSimpleReminders(year, month), // 传递年月参数
      getAllComplexReminders()
    ]);
    
    console.log("简单提醒加载响应:", simpleResponse);
    console.log("复杂提醒加载响应:", complexResponse);
    
    // 处理简单提醒
    const simpleEvents = simpleResponse.data
      .map(event => {
        // 确保正确处理日期，避免时区问题
        const eventTime = new Date(event.eventTime);
        
        // 创建FullCalendar格式的事件对象
        return {
          id: event.id,
          title: event.title,
          start: eventTime,
          // 所有非标准属性都应放入extendedProps
          extendedProps: {
            description: event.description || '',
            reminderType: event.reminderType || 'EMAIL',
            fromUserId: event.fromUserId,
            toUserId: event.toUserId,
            // 格式化后的显示时间
            displayTime: eventTime.toLocaleTimeString('zh-CN', {
              hour: '2-digit',
              minute: '2-digit',
              hour12: false
            }),
            // 默认的重复设置
            recurrenceType: event.recurrenceType || 'NONE',
            intervalValue: event.intervalValue || 1,
            intervalUnit: event.intervalUnit || 'DAYS',
            type: 'simple'
          }
        };
      });

    // 处理复杂提醒
    // 注意：复杂提醒不会直接显示在日历上，因为它们是基于cron表达式的
    // 我们在store中保存它们供需要时使用
    const complexEvents = complexResponse.data.map(reminder => {
      return {
        id: reminder.id,
        title: reminder.title,
        // 复杂提醒使用特殊标记，不会直接显示在日历上
        display: 'background',
        extendedProps: {
          description: reminder.description || '',
          reminderType: reminder.reminderType || 'EMAIL',
          cronExpression: reminder.cronExpression,
          validFrom: reminder.validFrom,
          validUntil: reminder.validUntil,
          maxExecutions: reminder.maxExecutions,
          type: 'complex'
        }
      };
    });
    
    // 将所有事件合并并排序
    allEvents.value = [...simpleEvents, ...complexEvents]
      .sort((a, b) => {
        // 对于有开始时间的事件按时间排序
        if (a.start && b.start) {
          return new Date(a.start) - new Date(b.start);
        }
        // 复杂提醒（没有start属性）放在后面
        if (!a.start) return 1;
        if (!b.start) return -1;
        return 0;
      });

    // 更新store中的提醒事项状态
    reminderState.simpleReminders = simpleResponse.data;
    reminderState.complexReminders = complexResponse.data;

    console.log("事项加载成功，转换后的事件数据:", allEvents.value);
    
    // 立即更新日历显示
    if (calendarApi.value) {
      console.log('通过calendarApi更新日历显示');
      calendarApi.value.refetchEvents();
    }
    
    // 使用CalendarDisplay组件的方法刷新
    if (calendarDisplayRef.value) {
      console.log('通过CalendarDisplay组件刷新事件');
      calendarDisplayRef.value.refreshEvents();
    }
  } catch (error) {
    console.error("加载事项失败:", error);
    console.error("错误响应:", error.response);
    if (error.response?.status === 401 || error.response?.status === 403) {
      console.log("认证失败，执行登出操作");
      handleLogout();
    } else {
      showNotification("加载事项失败，请重试", "error");
    }
  }
}

// 修改：事件处理函数
const handleEventClick = (info) => {
  console.log('Event clicked in App.vue:', info.event);
  
  if (!isLoggedIn.value) {
    showNotification('请先登录', 'warning');
    return;
  }
  
  try {
    // 直接获取事件对象中的数据，不要尝试解构或复制，因为FullCalendar的Event是特殊对象
    // 直接使用原始属性
    const id = info.event.id;
    const title = info.event.title;
    const start = info.event.start;
    
    // 直接获取extendedProps中的数据
    let description = '';
    let reminderType = 'EMAIL';
    let recurrenceType = 'NONE';
    let intervalValue = 1;
    let intervalUnit = 'DAYS';
    let eventType = 'simple'; // 默认为简单提醒
    
    // 安全获取扩展属性
    if (info.event.extendedProps) {
      description = info.event.extendedProps.description || '';
      reminderType = info.event.extendedProps.reminderType || 'EMAIL';
      recurrenceType = info.event.extendedProps.recurrenceType || 'NONE';
      intervalValue = info.event.extendedProps.intervalValue || 1;
      intervalUnit = info.event.extendedProps.intervalUnit || 'DAYS';
      eventType = info.event.extendedProps.type || 'simple';
    }
    
    console.log('事件基本信息:', { id, title, start });
    console.log('事件扩展信息:', { description, reminderType, recurrenceType, eventType });
    
    // 根据事件类型打开不同的模态框
    if (eventType === 'complex') {
      // 如果是复杂提醒，从store中获取完整数据
      const complexReminder = reminderState.complexReminders.find(r => r.id === id);
      if (complexReminder) {
        // 设置当前编辑的复杂提醒
        currentComplexReminderForModal.value = complexReminder;
        // 打开复杂提醒编辑模态框
        showComplexReminderModal.value = true;
      } else {
        console.error('找不到对应的复杂提醒数据:', id);
        showNotification('无法加载提醒数据', 'error');
      }
      return; // 不再继续处理简单提醒的逻辑
    }
    
    // 以下是简单提醒的处理逻辑
    // 确保日期正确处理
    const eventDate = new Date(start);
    
    // 格式化日期为 YYYY-MM-DD 格式，避免时区转换问题
    const year = eventDate.getFullYear();
    const month = String(eventDate.getMonth() + 1).padStart(2, '0');
    const day = String(eventDate.getDate()).padStart(2, '0');
    const hour = String(eventDate.getHours()).padStart(2, '0');
    const minute = String(eventDate.getMinutes()).padStart(2, '0');
    
    console.log('格式化后的日期时间:', `${year}-${month}-${day} ${hour}:${minute}`);
    
    // 创建一个包含所有必要数据的对象
    // 这个对象将用于EventModal
    currentEventForModal.value = {
      id, // 事件ID
      title, // 事件标题
      description, // 事件描述
      date: `${year}-${month}-${day}`, // 日期部分
      time: `${hour}:${minute}`, // 时间部分
      reminderType, // 提醒类型
      recurrenceType, // 重复类型
      intervalValue, // 重复间隔值
      intervalUnit // 重复间隔单位
    };
    
    console.log('准备编辑的事件数据:', currentEventForModal.value);
    
    // 标记为编辑模式
    isEditingEvent.value = true;
    
    // 打开事件编辑模态框
    uiState.isEventModalOpen = true;
    showModal.value = true;
  } catch (error) {
    console.error('处理点击事件时发生错误:', error);
    showNotification('处理提醒数据时出错', 'error');
  }
};

const handleDateSelect = (selectInfo) => {
  console.log('Date selected:', selectInfo);
  if (!isLoggedIn.value) {
    showNotification('请先登录', 'warning');
    return;
  }

  try {
    // 获取正确的日期，确保修正时区问题
    const selectedDate = new Date(selectInfo.start);
    console.log('选择的日期（原始）:', selectedDate);
    
    // 格式化日期为 YYYY-MM-DD 格式，避免时区转换问题
    const year = selectedDate.getFullYear();
    const month = String(selectedDate.getMonth() + 1).padStart(2, '0');
    const day = String(selectedDate.getDate()).padStart(2, '0');
    const formattedDate = `${year}-${month}-${day}`;
    
    console.log('格式化后的日期:', formattedDate);
    
    // 重置当前编辑的事件
    currentEventForModal.value = {
      title: '',
      description: '',
      reminderType: 'EMAIL',
      selectedDate: formattedDate, // 使用格式化的日期而不是toISOString()
      selectedHour: selectedDate.getHours(),
      selectedMinute: selectedDate.getMinutes(),
      selectedSecond: 0,
      start: selectedDate,
      end: selectInfo.end,
      allDay: selectInfo.allDay
    };
    
    isEditingEvent.value = false;
    
    // 确保uiState和本地状态同步
    console.log('设置弹窗状态为打开 (日期选择)', formattedDate);
    uiState.isEventModalOpen = true;
    showModal.value = true; // 同时设置本地状态以确保弹窗显示
    
    console.log('Event modal should open for new event:', currentEventForModal.value);
    console.log('Modal state:', { uiState: uiState.isEventModalOpen, showModal: showModal.value });
    selectInfo.view.calendar.unselect();
  } catch (error) {
    console.error('处理日期选择时出错:', error);
    showNotification('创建新事件失败', 'error');
  }
};

// 修改：关闭模态框函数
const closeEventModal = () => {
  console.log('Closing event modal');
  uiState.isEventModalOpen = false;
  showModal.value = false; // 同时设置本地状态以确保弹窗关闭
  currentEventForModal.value = null;
  isEditingEvent.value = false;
  console.log('Modal state after closing:', { uiState: uiState.isEventModalOpen, showModal: showModal.value });
};

// 处理事件拖拽
const handleEventDrop = async (dropInfo) => {
  if (!isLoggedIn.value) return;
  
  const event = dropInfo.event;
  const viewType = dropInfo.view?.type || '未知视图';
  console.log('处理事件拖拽，当前视图：', viewType);
  console.log('事件详情：', {
    id: event.id,
    title: event.title,
    start: event.start,
    end: event.end,
    extendedProps: event.extendedProps
  });
  
  // 检查extendedProps是否存在
  if (!event.extendedProps) {
    console.log('警告：事件缺少extendedProps属性');
    return; // 如果缺少extendedProps，则无法继续处理
  }
  
  if (event.extendedProps.type === 'simple' || !event.extendedProps.type) {
    try {
      // 准备完整的事件数据
      let updatedTime;
      
      if (viewType === 'dayGridMonth') {
        // 月视图：只修改日期，保留原来的时间
        const originalTime = new Date(event.extendedProps.eventTime || event.start);
        const newDate = new Date(event.start);
        
        // 合并日期和原始时间
        newDate.setHours(
          originalTime.getHours(),
          originalTime.getMinutes(),
          originalTime.getSeconds()
        );
        
        updatedTime = newDate.toISOString();
        console.log('月视图拖拽：只修改日期，保留原时间', newDate);
      } 
      else if (viewType === 'timeGridWeek') {
        // 周视图：可以修改日期和时间
        updatedTime = event.start.toISOString();
        console.log('周视图拖拽：修改日期和时间', event.start);
      } 
      else if (viewType === 'timeGridDay') {
        // 日视图：只修改时间，保留原来的日期
        const originalDate = new Date(event.extendedProps.eventTime || event.start);
        const newTime = new Date(event.start);
        
        // 合并原始日期和新时间
        originalDate.setHours(
          newTime.getHours(),
          newTime.getMinutes(),
          newTime.getSeconds()
        );
        
        updatedTime = originalDate.toISOString();
        console.log('日视图拖拽：只修改时间，保留原日期', originalDate);
      }
      else {
        // 默认：直接使用新的时间
        updatedTime = event.start.toISOString();
        console.log('默认处理：使用完整的新时间', event.start);
      }
      
      // 构造完整的事件数据
      const reminderData = {
        title: event.title,
        description: event.extendedProps?.description || '',
        eventTime: updatedTime,
        reminderType: event.extendedProps?.reminderType || 'EMAIL',
        toUserId: event.extendedProps?.toUserId || currentUserProfile.value?.id,
        fromUserId: event.extendedProps?.fromUserId || currentUserProfile.value?.id
      };
      
      console.log('发送更新请求，完整数据：', reminderData);
      await updateEvent(event.id, reminderData);
      showNotification('更新提醒时间成功', 'success');
      await loadEvents(); // 重新加载事件
    } catch (error) {
      console.error('更新事件失败：', error);
      console.error('错误详情：', error.response?.data);
      dropInfo.revert();
      showNotification('更新提醒时间失败：' + (error.response?.data?.message || error.message), 'error');
    }
  }
};

// 处理事件调整大小
const handleEventResize = async (resizeInfo) => {
  if (!isLoggedIn.value) return;
  
  const event = resizeInfo.event;
  const viewType = resizeInfo.view?.type || '未知视图';
  console.log('处理事件调整大小，当前视图：', viewType);
  console.log('事件详情：', {
    id: event.id,
    title: event.title,
    start: event.start,
    end: event.end,
    extendedProps: event.extendedProps
  });
  
  // 检查extendedProps是否存在
  if (!event.extendedProps) {
    console.log('警告：事件缺少extendedProps属性');
    return; // 如果缺少extendedProps，则无法继续处理
  }
  
  if (event.extendedProps.type === 'simple' || !event.extendedProps.type) {
    try {
      // 对于调整大小，我们使用更新后的开始时间
      const updatedTime = event.start.toISOString();
      
      // 构造完整的事件数据
      const reminderData = {
        title: event.title,
        description: event.extendedProps?.description || '',
        eventTime: updatedTime,
        reminderType: event.extendedProps?.reminderType || 'EMAIL',
        toUserId: event.extendedProps?.toUserId || currentUserProfile.value?.id,
        fromUserId: event.extendedProps?.fromUserId || currentUserProfile.value?.id
      };
      
      console.log('发送更新请求，完整数据：', reminderData);
      await updateEvent(event.id, reminderData);
      showNotification('更新提醒时间成功', 'success');
      await loadEvents(); // 重新加载事件
    } catch (error) {
      console.error('更新事件失败：', error);
      console.error('错误详情：', error.response?.data);
      resizeInfo.revert();
      showNotification('更新提醒时间失败：' + (error.response?.data?.message || error.message), 'error');
    }
  }
};

// (可选) FullCalendar 自身的 windowResize 回调，主要用于切换工具栏等
// function handleWindowResize(arg) {
//   console.log('FC windowResize event:', arg.view);
//   // 可以在这里根据窗口大小做更复杂的操作，比如切换 initialView 或 headerToolbar
//   // if (window.innerWidth < 768) { ... } else { ... }
// }

// --- Add Registration Logic ---
function showRegisterView() {
  showRegisterForm.value = true;
  loginError.value = null; // Clear login error when switching
}

function showLoginView() {
  showRegisterForm.value = false;
  registerError.value = null; // Clear register error when switching
}

async function handleRegister(registerData) {
  console.log("App.vue: Registration attempt...", registerData);
  registerError.value = null; // Clear previous errors
  loginError.value = null;

  // Basic validation using the passed registerData parameter
  if (!registerData.username || !registerData.password || !registerData.nickname || !registerData.email) {
    registerError.value = "所有字段均为必填项。";
    return;
  }
  
  // Password confirmation check if needed 
  if (registerData.password !== registerData.confirmPassword && registerData.confirmPassword) {
    registerError.value = "两次输入的密码不一致";
    return;
  }

  try {
    const userData = {
      username: registerData.username,
      email: registerData.email,
      password: registerData.password,
      nickname: registerData.nickname,
      avatarUrl: registerData.avatarUrl || null // Include avatarUrl if provided
    };
    
    await register(userData); // Use the new service function
    console.log("App.vue: Registration successful."); 
    alert("注册成功！请使用您的新账户登录。"); 
    showLoginView(); // Switch back to login view
    // Clear registration form fields
    registerUsername.value = '';
    registerPassword.value = '';
    registerNickname.value = '';
    registerEmail.value = '';
  } catch (error) {
    console.error('App.vue: Registration failed:', error);
    
    // 直接使用错误对象中的message，这已经在api.js的拦截器中处理过了
    if (error.message) {
      registerError.value = error.message;
    } 
    // 处理字段特定的错误
    else if (error.fieldErrors && Object.keys(error.fieldErrors).length > 0) {
      // 展示第一个字段错误
      const firstErrorField = Object.keys(error.fieldErrors)[0];
      registerError.value = error.fieldErrors[firstErrorField];
    }
    // 兜底的错误消息
    else if (error.response) {
      if (error.response.status === 409) {
        registerError.value = '用户名或邮箱已被占用';
      } else {
        registerError.value = `注册失败 (${error.response.status})`;
      }
    } else {
      registerError.value = '注册失败，请稍后重试';
    }
  }
}

// --- 定义基本的 Calendar 选项 (不含随屏幕变化的 headerToolbar) ---
const baseCalendarOptions = {
  plugins: [ dayGridPlugin, timeGridPlugin, interactionPlugin ],
  customButtons: {
    prevYear: { text: '<<', click: prevYear },
    nextYear: { text: '>>', click: nextYear },
  },
  events: allEvents,
  editable: true,
  selectable: true,
  selectMirror: true,
  datesSet: handleCalendarDatesSet,
  eventClick: handleEventClick,
  select: handleDateSelect,
  eventDrop: handleEventDrop,
  eventResize: handleEventResize,
  eventTimeFormat: {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  },
  eventContent: function(arg) {
    return {
      html: `<div class="fc-event-main-inner">
               <div class="fc-event-time">${arg.event.extendedProps.displayTime || ''}</div>
               <div class="fc-event-title">${arg.event.title}</div>
             </div>`
    }
  },
  headerToolbar: {
    left: 'prevYear,prev,next,nextYear',
    center: '', // 移除中间的标题
    right: 'dayGridMonth,timeGridWeek,timeGridDay'
  },
  initialView: 'dayGridMonth', 
};

// --- 使用 computed 来动态生成完整的 calendarOptions ---
const windowWidth = ref(window.innerWidth); // 跟踪窗口宽度
let resizeTimeout = null; // 定义 resizeTimeout (保留这里的声明)
const handleResize = debounce(() => { // 定义 handleResize (保留这里的定义)
  windowWidth.value = window.innerWidth;
}, 200);

// onMounted 和 onUnmounted 移到这里，因为 handleResize 在这里定义
onMounted(() => {
  if (calendarRef.value) { // 仍然尝试获取 ref，虽然可能被 v-if 延迟
      // calendarApi = calendarRef.value.getApi(); // 获取 API 应由 handleCalendarReady 处理
  }
  // 打印初始屏幕尺寸
  console.log(`Screen Width: ${window.innerWidth}, Screen Height: ${window.innerHeight}`);
  checkAuthStatus();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  clearTimeout(resizeTimeout);
});

const calendarOptions = computed(() => {
  const isSmallScreen = windowWidth.value < 768; 
  const headerToolbarOptions = isSmallScreen
    ? { 
        left: 'prev,next',
        center: '',
        right: 'dayGridMonth,timeGridWeek,timeGridDay'
      }
    : { 
        left: 'prevYear,prev,next,nextYear',
        center: '',
        right: 'dayGridMonth,timeGridWeek,timeGridDay'
      };

  return {
    ...baseCalendarOptions,
    headerToolbar: headerToolbarOptions,
    initialView: 'dayGridMonth',
    buttonText: {
      today: '今天',
      month: '月',
      week: '周',
      day: '日'
    }
  };
});

// 在 calendarOptions 定义之后，标记为就绪
nextTick(() => {
  isCalendarOptionsReady.value = true;
  console.log("Calendar options marked ready immediately after definition (using nextTick).");
});

// --- 修改：用于 CalendarDisplay 的选项 ---
const calendarDisplayOptions = computed(() => {
  return {
    ...calendarOptions.value,
    events: allEvents.value, // 将获取到的事件传递给子组件
    eventClick: handleEventClick,
    select: handleDateSelect,
    eventDrop: handleEventDrop,
    eventResize: handleEventResize
  };
});

// --- 优化：处理日历就绪事件，确保立即应用正确主题 ---
const handleCalendarReady = (apiInstance) => {
  console.log("App.vue: 接收到日历就绪事件，获取API实例");
  calendarApi.value = apiInstance;
  
  if (calendarApi.value) {
    // API 准备好后，立即获取当前视图并更新主题
    const currentView = calendarApi.value.view;
    console.log("当前日历视图:", currentView);
    
    // 调用主题更新函数
    updateThemeAndSelector(currentView);
    
    // 标记日历选项已就绪
    isCalendarOptionsReady.value = true;
    
    // 确保事件已加载并显示在日历上
    if (allEvents.value.length > 0) {
      console.log("日历就绪，刷新已加载的事件");
      calendarApi.value.refetchEvents();
    } else if (isLoggedIn.value) {
      console.log("日历就绪，但需要加载事件");
      loadEvents().then(() => {
        console.log("事件加载完成，更新日历显示");
        calendarApi.value.refetchEvents();
      });
    }
    
    // 设置日历视图变化事件处理
    calendarApi.value.on('datesSet', (info) => {
      console.log('App.vue: 日历视图变化:', info);
      // 获取新视图的年月
      const newViewDate = info.view.currentStart || info.start;
      const year = newViewDate.getFullYear();
      const month = newViewDate.getMonth() + 1; // 月份从0开始，需要+1
      
      console.log(`App.vue: 视图切换到 ${year}年${month}月，重新加载提醒事项`);
      // 重新加载该月的提醒事项
      loadEvents();
      
      // 调用主题更新函数
      updateThemeAndSelector(info.view);
    });
    
    // 可以在这里触发额外的初始化操作
    console.log("日历组件完全初始化完成");
  } else {
    console.error("App.vue: 无法获取有效的日历API实例");
  }
};

// --- 月份选择器相关逻辑 ---
const monthSelectorRef = ref(null);
const headerRef = ref(null);
const appContainerRef = ref(null);

// 处理点击外部关闭
const handleClickOutside = (event) => {
  if (!showMonthSelectorPopup.value || !monthSelectorRef.value) {
    return;
  }
  if (!monthSelectorRef.value.contains(event.target)) {
    showMonthSelectorPopup.value = false;
  }
};

// 修改 toggleMonthSelector 函数
function toggleMonthSelector(info) {
  console.log('Toggle month selector called with info:', info);
  
  // 处理从日历组件传来的视图更新事件
  if (info && info.view) {
    console.log('Updating theme from calendar view change');
    updateThemeAndSelector(info.view);
    return;
  }
  
  // 处理用户点击切换选择器的情况
  console.log('Toggling month selector popup');
  showMonthSelectorPopup.value = !showMonthSelectorPopup.value;
  
  if (showMonthSelectorPopup.value) {
    nextTick(() => {
      const dateDisplay = document.querySelector('.date-display');
      const monthSelector = document.querySelector('.month-selector-popup');
      if (dateDisplay && monthSelector) {
        const rect = dateDisplay.getBoundingClientRect();
        const selectorWidth = monthSelector.offsetWidth;
        
        // 计算水平居中位置
        const left = rect.left + (rect.width - selectorWidth) / 2;
        
        // 设置位置
        monthSelector.style.top = `${rect.bottom + 5}px`; // 在日期显示区域下方5px
        monthSelector.style.left = `${left}px`;
        
        // 处理边界情况
        const viewportWidth = window.innerWidth;
        const selectorRect = monthSelector.getBoundingClientRect();
        
        if (selectorRect.left < 0) {
          monthSelector.style.left = '5px';
        } else if (selectorRect.right > viewportWidth) {
          monthSelector.style.left = `${viewportWidth - selectorWidth - 5}px`;
        }
      }
    });
  }
}

// 添加窗口大小变化监听
window.addEventListener('resize', () => {
  if (showMonthSelectorPopup.value) {
    toggleMonthSelector();
    toggleMonthSelector();
  }
});

// 处理月份选择
function selectMonth(monthNumber) {
  if (selectedMonth.value !== monthNumber) {
    selectedMonth.value = monthNumber;
    if (calendarApi.value && selectedYear.value) {
      const monthString = monthNumber.toString().padStart(2, '0');
      const dateStr = `${selectedYear.value}-${monthString}-01`;
      calendarApi.value.gotoDate(dateStr);
    }
  }
  showMonthSelectorPopup.value = false;
}

// --- 新增：事件处理 (简化版) ---
// 用于替代旧的 saveEvent 和 deleteEvent，处理来自 EventModal 的事件
async function handleSaveEvent(formData) {
  console.log('App.vue: 开始保存事件，收到表单数据:', formData);
  try {
    if (!currentUserProfile.value?.id) {
      throw new Error('用户未登录或用户信息不完整');
    }

    // 使用传入的formData(来自EventModal)或currentEventForModal构造事件数据
    const eventData = formData || currentEventForModal.value;
    
    if (!eventData) {
      throw new Error('事件数据丢失');
    }
    
    // 构造符合后端格式的数据
    const reminderData = {
      title: eventData.title,
      description: eventData.description || '',
      // 格式化时间为ISO 8601格式 (YYYY-MM-DDTHH:MM:SS+TIMEZONE)
      eventTime: `${eventData.selectedDate}T${String(eventData.selectedHour || 0).padStart(2, '0')}:${String(eventData.selectedMinute || 0).padStart(2, '0')}:${String(eventData.selectedSecond || 0).padStart(2, '0')}+08:00`,
      reminderType: eventData.reminderType || 'EMAIL',
      toUserId: currentUserProfile.value.id,
      fromUserId: currentUserProfile.value.id // 确保包含发送者ID
    };
    
    console.log('App.vue: 发送到后端的数据格式:', reminderData);

    let savedEvent;
    if (isEditingEvent.value && eventData.id) {
      // 更新时需要保留ID
      const response = await updateEvent(eventData.id, reminderData);
      savedEvent = response.data;
      showNotification('更新成功', 'success');
    } else {
      // 创建新事项
      const response = await createEvent(reminderData);
      savedEvent = response.data;
      showNotification('创建成功', 'success');
    }

    // 重新加载事项列表
    await loadEvents();
    
    // 立即更新日历显示
    // 使用两种方式确保更新显示
    if (calendarApi.value) {
      console.log('通过calendarApi更新日历显示');
      calendarApi.value.refetchEvents();
    }
    
    // 使用CalendarDisplay组件的方法刷新
    if (calendarDisplayRef.value) {
      console.log('通过CalendarDisplay组件刷新事件');
      calendarDisplayRef.value.refreshEvents();
    }
    
    closeEventModal();
  } catch (error) {
    console.error("保存事项失败:", error);
    if (error.response) {
      console.error("错误状态:", error.response.status);
      console.error("错误详情:", error.response.data);
    }
    showNotification(error.response?.data?.message || error.message || "保存事项失败，请重试", 'error');
  }
}

async function handleDeleteEvent(eventId) {
  if (!confirm('确定要删除这个提醒事项吗？')) {
    return;
  }
  
  try {
    await deleteEvent(eventId);
    await loadEvents(); // Reload events after deletion
  } catch (error) {
    console.error("Failed to delete event:", error);
    // Handle error appropriately
  }
}

// 添加处理即将提醒按钮点击的方法
const handleUpcomingRemindersClick = () => {
  console.log('处理即将提醒按钮点击');
  // 显示即将提醒模态框
  showUpcomingRemindersModal.value = true;
};

// 添加处理复杂提醒按钮点击的方法
const handleComplexRemindersClick = () => {
  console.log('处理复杂提醒按钮点击');
  // 显示复杂提醒列表模态框
  showComplexReminderListModal.value = true;
};

// 处理复杂提醒模态框关闭
const handleComplexReminderClose = () => {
  showComplexReminderModal.value = false;
  // 清空当前编辑的复杂提醒数据
  currentComplexReminderForModal.value = {};
};

// 处理复杂提醒模态框保存
const handleComplexReminderSave = async (data) => {
  console.log('收到复杂提醒设置数据:', data);
  
  try {
    if (!currentUserProfile.value || !currentUserProfile.value.id) {
      throw new Error('用户未登录或用户信息不完整');
    }
    
    // 处理日期格式
    const reminderToSave = {
      ...data,
      // 确保日期格式正确
      validFrom: data.validFrom || new Date().toISOString().split('T')[0],
      validUntil: data.validUntil || null,
      // 添加用户ID信息
      fromUserId: currentUserProfile.value.id,
      toUserId: currentUserProfile.value.id // 默认发送给自己
    };
    
    console.log('发送到后端的复杂提醒数据:', reminderToSave);

    let response;
    // 判断是创建还是更新
    if (data.id) {
      // 如果有ID，说明是更新现有提醒
      console.log('更新复杂提醒:', data.id);
      response = await updateComplexReminder(data.id, reminderToSave);
      showNotification('复杂提醒已更新', 'success');
    } else {
      // 否则是创建新提醒
      console.log('创建新的复杂提醒');
      response = await createComplexReminder(reminderToSave);
      showNotification('复杂提醒已创建', 'success');
    }
    
    console.log('复杂提醒保存成功:', response.data);
    
    // 清空当前编辑的复杂提醒
    currentComplexReminderForModal.value = {};
    
    // 刷新日历显示
    if (calendarApi.value) {
      calendarApi.value.refetchEvents();
    }
    
    if (calendarDisplayRef.value) {
      calendarDisplayRef.value.refreshEvents();
    }
    
    // 重新加载事件列表
    await loadEvents();
    
    // 关闭模态框
    showComplexReminderModal.value = false;
  } catch (error) {
    console.error('保存复杂提醒失败:', error);
    
    // 显示错误信息
    const errorMessage = error.response?.data?.message || error.message || '保存复杂提醒失败，请重试';
    showNotification(errorMessage, 'error');
  }
};

// 处理点击即将提醒列表中的提醒项
const handleUpcomingReminderClick = (reminderData) => {
  console.log('处理点击即将提醒列表中的提醒项:', reminderData);
  
  // 设置当前编辑的提醒
  currentEventForModal.value = reminderData;
  isEditingEvent.value = true;
  
  // 打开事件编辑模态框
  uiState.isEventModalOpen = true;
  showModal.value = true;
};

// 处理弹出菜单操作
function handleHeaderAction(action) {
  console.log('Header action clicked:', action);
  switch (action) {
    case 'showUpcomingReminders':
      showUpcomingRemindersModal.value = true;
      break;
    case 'showUserProfile':
      showUserProfileModal.value = true;
      break;
    case 'createComplexReminder':
      // 重置当前编辑的复杂提醒数据
      currentComplexReminderForModal.value = {};
      // 打开复杂提醒编辑模态框
      showComplexReminderModal.value = true;
      break;
    case 'showComplexReminderList':
      // 打开复杂提醒列表模态框
      showComplexReminderListModal.value = true;
      break;
    default:
      console.warn('未处理的操作:', action);
  }
}

// ... existing code ...

// 创建一个新的复杂提醒
const createNewComplexReminder = () => {
  // 重置当前编辑的复杂提醒数据
  currentComplexReminderForModal.value = {};
  // 打开复杂提醒编辑模态框
  showComplexReminderModal.value = true;
};

// 编辑现有复杂提醒
const editComplexReminder = (reminder) => {
  // 设置当前编辑的复杂提醒
  currentComplexReminderForModal.value = reminder;
  // 打开复杂提醒编辑模态框
  showComplexReminderModal.value = true;
};

// 删除复杂提醒
const deleteComplexReminder = async (id) => {
  try {
    // 使用API模块中的方法删除复杂提醒
    await deleteComplexReminderApi(id);
    
    // 从状态中移除已删除的提醒
    reminderState.complexReminders = reminderState.complexReminders.filter(
      reminder => reminder.id !== id
    );
    
    // 提示成功
    showNotification('复杂提醒已删除', 'success');
    
    // 重新加载日历事件
    await loadEvents();
  } catch (error) {
    console.error('删除复杂提醒失败:', error);
    showNotification('删除失败: ' + error.message, 'error');
  }
};

</script>

<template>
  <div class="app-root">
    <div class="app-container" ref="appContainerRef">
      <div v-if="!isLoggedIn" class="auth-container">
        <LoginView 
          v-if="!showRegisterForm"
          :login-error="loginError"
          @login-attempt="handleLogin"
          @show-register="showRegisterView"
        />
        <RegisterView
          v-else
          :register-error="registerError"
          @register-attempt="handleRegister"
          @show-login="showLoginView"
        />
      </div>

      <template v-else>
        <AppHeader 
          ref="headerRef"
          :selected-year="selectedYear"
          :selected-month="selectedMonth"
          :current-month-name="currentMonthName"
          :is-logged-in="isLoggedIn"
          :current-user="currentUserProfile"
          @toggle-month-selector="toggleMonthSelector"
          @select-month="selectMonth"
          @logout="handleLogout"
          @open-profile="openUserProfileModal"
          @header-action="handleHeaderAction"
        />
        
        <main class="main-content">
          <div class="calendar-wrapper">
            <CalendarDisplay
              v-if="isCalendarOptionsReady"
              ref="calendarDisplayRef"
              :options="calendarOptions"
              :events="allEvents"
              :loading="reminderState.loading"
              @calendar-ready="handleCalendarReady"
              @toggle-month-selector="toggleMonthSelector"
              @event-click="handleEventClick"
              @date-select="handleDateSelect"
              @event-drop="(info) => {
                console.log('App.vue接收到event-drop事件:', info);
                handleEventDrop(info);
              }"
              @event-resize="(info) => {
                console.log('App.vue接收到event-resize事件:', info);
                handleEventResize(info);
              }"
              @upcoming-reminders-click="handleUpcomingRemindersClick"
              @complex-reminders-click="handleComplexRemindersClick"
            />
          </div>
        </main>

        <!-- 事件编辑模态框 -->
        <Teleport to="body">
          <EventModal
            v-if="uiState.isEventModalOpen || showModal"
            :show="uiState.isEventModalOpen || showModal"
            :event-data="currentEventForModal"
            :is-editing="isEditingEvent"
            @close="closeEventModal"
            @save="handleSaveEvent"
            @delete="handleDeleteEvent"
          />
        </Teleport>

        <!-- 月份选择器弹出框 -->
        <Teleport to="body">
          <div 
            v-if="showMonthSelectorPopup" 
            class="month-selector-popup" 
            ref="monthSelectorRef"
          >
            <div class="month-grid">
              <div
                v-for="(month, index) in monthNames"
                :key="index"
                class="month-item"
                :class="{ active: selectedMonth === index + 1 }"
                @click="selectMonth(index + 1)"
              >
                {{ month }}
              </div>
            </div>
          </div>
        </Teleport>

        <!-- 添加调试信息 -->
        <div v-if="showMonthSelectorPopup" style="display: none;">
          Debug: Month selector should be visible ({{ showMonthSelectorPopup }})
        </div>

        <!-- 用户资料模态框 -->
        <UserProfileModal
          v-if="uiState.isProfileModalOpen"
          :show="uiState.isProfileModalOpen"
          :user-profile="currentUserProfile"
          @close="uiState.isProfileModalOpen = false"
          @save="saveUserProfile"
        />

        <!-- 即将提醒模态框 -->
        <Teleport to="body">
          <UpcomingRemindersModal
            v-if="showUpcomingRemindersModal"
            :show="showUpcomingRemindersModal"
            @close="showUpcomingRemindersModal = false"
            @reminder-click="handleUpcomingReminderClick"
          />
        </Teleport>

        <!-- 复杂提醒设置模态框 -->
        <ComplexReminderModal
          v-if="showComplexReminderModal"
          :show="showComplexReminderModal"
          :initial-data="currentComplexReminderForModal"
          @close="handleComplexReminderClose"
          @save="handleComplexReminderSave"
        />

        <!-- 新增：复杂提醒列表模态框 -->
        <ComplexReminderListModal
          v-if="showComplexReminderListModal"
          :show="showComplexReminderListModal"
          @close="showComplexReminderListModal = false"
          @edit="editComplexReminder"
          @delete="deleteComplexReminder"
          @create="createNewComplexReminder"
        />
      </template>
    </div>
  </div>
</template>

<style>
:root {
  --theme-primary-color: #4CAF50;
  --theme-hover-color: #45a049;
  --theme-active-color: #3d8b40;
  --theme-today-bg: #4CAF50;
  --theme-day-hover-bg: #f8f9fa;

  --mobile-min-width: 320px;  /* iPhone SE 等小屏设备 */
  --mobile-max-width: 767px;
  --tablet-min-width: 768px;
  --tablet-max-width: 1023px;
  --desktop-min-width: 1024px;
  --desktop-large-width: 1440px;

  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;

  /* 添加字体相关变量 */
  --font-family-main: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  --font-size-title: 1.5em;
  --font-weight-title: 600;
  --font-color-primary: #2c3e50;
  --font-color-secondary: #666;
}

.app-root {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f7fa;
  padding: 0;
  margin: 0;
  box-sizing: border-box;
}

.app-container {
  display: flex;
  flex-direction: column;
  height: 90vh; /* 页面高度的90% */
  width: calc(90vh * 1.5); /* 保持3:2比例 */
  max-width: 95vw;
  background-color: white;
  box-sizing: border-box;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  overflow: hidden;
  position: relative;
}

/* 修改 app-header 样式 */
:deep(.app-header) {
  flex: 1; /* 占据剩余空间的1份 */
  width: 100%;
  max-width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  border-radius: 12px 12px 0 0;
  border-bottom: 1px solid #eee;
  padding: 0 15px;
  z-index: 10;
}

.main-content {
  flex: 10; /* 占据剩余空间的10份，实现1:10的比例 */
  width: 100%;
  max-width: 100%;
  background: white;
  border-radius: 0 0 12px 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.calendar-wrapper {
  flex: 1;
  min-height: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

/* 确保日历组件填满容器 */
:deep(.fc) {
  width: 100%;
  height: 100%;
  background: white;
  display: flex;
  flex-direction: column;
}

/* 优化响应式布局 */
@media (max-width: 768px) {
  .app-root {
    padding: 10px;
  }
  .app-container {
    width: 100%;
    height: 100%;
    max-width: 100%;
    border-radius: 0;
  }
}

/* 日历组件样式 */
:deep(.fc) {
  width: 100%;
  height: 100%;
}

/* 控制日历单元格的宽度 */
:deep(.fc .fc-daygrid-day) {
  width: calc(100% / 7); /* 确保7列等宽 */
}

/* 优化日历头部的响应式布局 */
:deep(.fc .fc-toolbar.fc-header-toolbar) {
  margin-bottom: 1.5em;
  padding: 0 1.5em;
  flex-wrap: wrap;
  gap: 8px;
  width: 100% !important;
  box-sizing: border-box !important;
}

/* 优化按钮组的布局 */
:deep(.fc .fc-button-group) {
  gap: 4px;
}

/* 调整日历内容区域的滚动行为 */
:deep(.fc-scroller) {
  overflow: hidden !important; /* 隐藏所有滚动条 */
}

/* 确保日历视图在容器中正确显示并自适应大小 */
:deep(.fc-view-harness) {
  width: 100% !important;
  height: 100% !important; /* 确保高度100% */
}

/* 确保日历容器能够完全适应空间 */
:deep(.fc-view-harness-active) {
  height: 100% !important;
  flex: 1; /* 使其填充剩余空间 */
}

/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Nunito', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 
               'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  line-height: 1.5;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f7fa;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 添加登录容器样式 */
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f7fa;
}

/* 月份选择器样式 */
.month-selector-popup {
  position: fixed;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  padding: var(--spacing-lg) var(--spacing-xl);
  z-index: 1100;
  width: 420px;
  font-family: var(--font-family-main);
  animation: popupFadeIn 0.2s ease-out;
  box-sizing: border-box;
  transform-origin: top center;
}

.month-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-md);
  width: 100%;
  margin: 0 auto;
}

.month-item {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: var(--spacing-md) 0;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
  font-size: 1.4rem;
  font-weight: 500;
  color: var(--font-color-primary);
  white-space: nowrap;
  text-align: center;
  width: 100%;
  min-width: 90px;
  background-color: #f8f9fa;
}

.month-item:hover {
  background-color: var(--theme-hover-color);
  color: white;
}

.month-item.active {
  background-color: var(--theme-primary-color);
  color: white;
}

@keyframes popupFadeIn {
  from {
    opacity: 0;
    transform: translateY(-8px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 响应式布局调整 */
@media (max-width: 768px) {
  .month-selector-popup {
    width: 380px;
    padding: var(--spacing-md) var(--spacing-lg);
  }

  .month-grid {
    gap: var(--spacing-sm);
  }

  .month-item {
    padding: var(--spacing-sm) 0;
    font-size: 1.3rem;
    min-width: 80px;
  }
}

@media (max-width: 480px) {
  .month-selector-popup {
    width: 360px;
    padding: var(--spacing-sm) var(--spacing-md);
  }

  .month-grid {
    gap: var(--spacing-xs);
  }

  .month-item {
    padding: 8px 0;
    font-size: 1.2rem;
    min-width: 70px;
  }
}

@media (max-width: 320px) {
  .month-selector-popup {
    width: 300px;
    padding: var(--spacing-xs) var(--spacing-sm);
    left: 50% !important;
    transform: translateX(-50%) !important;
  }

  .month-grid {
    gap: 4px;
  }

  .month-item {
    padding: 6px 0;
    font-size: 1.1rem;
    min-width: 60px;
  }
}

/* 确保模态框显示在最上层 */
.event-modal {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
  z-index: 1100;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

/* 响应式布局 */
@media (max-width: 767px) {
  .app-container {
    width: 100%;
    height: 100%;
    max-width: 100%;
    border-radius: 0;
    margin: 0;
  }

  .month-selector-popup {
    width: 90%;
    max-width: 320px;
    padding: var(--spacing-sm);
  }

  .month-item {
    padding: var(--spacing-xs);
    font-size: calc(var(--font-size-title) * 0.9);
  }

  .event-modal {
    width: 95%;
    max-height: 95vh;
    border-radius: 8px;
  }

  :deep(.fc) {
    font-size: 12px;
  }

  :deep(.fc-header-toolbar) {
    flex-wrap: wrap;
    gap: var(--spacing-xs);
    padding: var(--spacing-xs);
  }

  :deep(.fc-toolbar-title) {
    font-size: 1.2em;
  }

  :deep(.fc-button) {
    padding: 4px 8px;
    font-size: 12px;
  }
}

/* 平板布局 */
@media (min-width: 768px) and (max-width: 1023px) {
  .app-container {
    width: calc(85vh * 1.5);
    height: 85vh;
  }

  .month-selector-popup {
    width: 320px;
  }

  :deep(.fc) {
    font-size: 14px;
  }
}

/* 确保最小宽度 */
@media (max-width: 320px) {
  .app-container {
    min-width: var(--mobile-min-width);
  }

  .month-selector-popup {
    width: calc(100% - var(--spacing-md) * 2);
    left: var(--spacing-md);
    transform: none;
  }

  .month-item {
    padding: 4px;
    font-size: calc(var(--font-size-title) * 0.8);
  }

  :deep(.fc-toolbar-title) {
    font-size: 1em;
  }

  :deep(.fc-button) {
    padding: 2px 6px;
    font-size: 11px;
  }
}

/* 大屏幕优化 */
@media (min-width: 1440px) {
  .app-container {
    max-width: 1400px;
  }

  .month-selector-popup {
    width: 360px;
  }

  .month-item {
    padding: var(--spacing-md);
    font-size: 16px;
  }
}

/* 暗色模式支持 */
@media (prefers-color-scheme: dark) {
  :root {
    --theme-primary-color: #66bb6a;
    --theme-hover-color: #81c784;
    --theme-active-color: #4caf50;
  }

  .app-root {
    background-color: #121212;
  }

  .app-container {
    background-color: #1e1e1e;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.3);
  }

  .month-selector-popup,
  .event-modal {
    background-color: #2d2d2d;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.4);
  }

  .month-item {
    color: #e0e0e0;
  }

  .month-item:hover {
    background-color: #3d3d3d;
  }
}

/* 更新日期显示区域样式 */
.date-display {
  flex-grow: 1;
  justify-content: center;
  font-family: var(--font-family-main);
  font-size: var(--font-size-title);
  font-weight: var(--font-weight-title);
  color: var(--font-color-primary);
  gap: 0.5em;
  text-align: center;
}

.year-display {
  color: var(--font-color-secondary);
}

.month-display.clickable {
  cursor: pointer;
  text-decoration: none;
  position: relative;
}

.month-display.clickable::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: var(--theme-primary-color);
  transform: scaleX(0);
  transition: transform 0.2s ease;
}

.month-display.clickable:hover::after {
  transform: scaleX(1);
}

/* 更新月份选择器样式 */
.month-selector-popup {
  position: fixed;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  padding: var(--spacing-lg) var(--spacing-xl);
  z-index: 1100;
  width: 420px;
  font-family: var(--font-family-main);
  animation: popupFadeIn 0.2s ease-out;
  box-sizing: border-box;
}

.month-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-md);
  width: 100%;
  margin: 0 auto;
}

.month-item {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: var(--spacing-md) 0;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
  font-size: 1.4rem;
  font-weight: 500;
  color: var(--font-color-primary);
  white-space: nowrap;
  text-align: center;
  width: 100%;
  min-width: 90px;
  background-color: #f8f9fa;
}

.month-item:hover {
  background-color: var(--theme-hover-color);
  color: white;
}

.month-item.active {
  background-color: var(--theme-primary-color);
  color: white;
}

/* 移除之前的 fc-toolbar 相关样式 */
:deep(.fc-toolbar-title) {
  display: none !important;
}

/* 隐藏原有的日期显示 */
:deep(.fc-toolbar-chunk:nth-child(2)) {
  visibility: hidden;
}

/* 调整工具栏布局 */
:deep(.fc-toolbar.fc-header-toolbar) {
  justify-content: space-between;
  padding: 0 1rem;
  width: 100% !important;
  box-sizing: border-box !important;
  margin: 0 !important;
}

:deep(.fc-toolbar-chunk) {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .date-display {
    font-size: calc(var(--font-size-title) * 0.9);
  }
  
  .month-item {
    font-size: calc(var(--font-size-title) * 0.9);
    padding: var(--spacing-xs) var(--spacing-sm);
  }
}

@media (max-width: 480px) {
  .date-display {
    font-size: calc(var(--font-size-title) * 0.8);
  }
  
  .month-item {
    font-size: calc(var(--font-size-title) * 0.8);
    padding: var(--spacing-xs);
  }
}
</style>
