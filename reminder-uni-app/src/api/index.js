// API统一导出
export { request, API_CONFIG } from './request.js';
export { authApi } from './auth.js';
export { reminderApi, createEvent, updateEvent, deleteEvent } from './reminder.js';
export { calendarApi } from './calendar.js';

// 兼容旧的导入方式，从原services/api.js导出的方法
export const {
    login,
    register,
    getUserProfile,
    updateProfile
} = authApi;

export const {
    getAllSimpleReminders,
    getSimpleReminderById,
    createSimpleReminder,
    updateSimpleReminder,
    deleteSimpleReminder,
    getUpcomingReminders,
    getAllComplexReminders,
    getComplexReminderById,
    createComplexReminder,
    updateComplexReminder,
    deleteComplexReminder
} = reminderApi;

export const {
    getHolidaysByYearRange,
    getCalendarData
} = calendarApi;

// 默认导出所有API
export default {
    auth: authApi,
    reminder: reminderApi,
    calendar: calendarApi
}; 