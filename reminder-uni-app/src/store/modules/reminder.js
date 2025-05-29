import { reactive } from 'vue';

// 提醒事项状态
export const reminderState = reactive({
    simpleReminders: [],
    complexReminders: [],
    upcomingReminders: [],
    loading: false,
    error: null,
    lastFetchTime: null
});

// 提醒相关操作
export const reminderActions = {
    // 设置简单提醒列表
    setSimpleReminders(reminders) {
        reminderState.simpleReminders = reminders || [];
        reminderState.lastFetchTime = new Date();
    },

    // 设置复杂提醒列表
    setComplexReminders(reminders) {
        reminderState.complexReminders = reminders || [];
    },

    // 设置即将到来的提醒
    setUpcomingReminders(reminders) {
        reminderState.upcomingReminders = reminders || [];
    },

    // 添加简单提醒
    addSimpleReminder(reminder) {
        reminderState.simpleReminders.push(reminder);
    },

    // 更新简单提醒
    updateSimpleReminder(id, updatedReminder) {
        const index = reminderState.simpleReminders.findIndex(r => r.id === id);
        if (index !== -1) {
            reminderState.simpleReminders[index] = { ...reminderState.simpleReminders[index], ...updatedReminder };
        }
    },

    // 删除简单提醒
    deleteSimpleReminder(id) {
        const index = reminderState.simpleReminders.findIndex(r => r.id === id);
        if (index !== -1) {
            reminderState.simpleReminders.splice(index, 1);
        }
    },

    // 添加复杂提醒
    addComplexReminder(reminder) {
        reminderState.complexReminders.push(reminder);
    },

    // 更新复杂提醒
    updateComplexReminder(id, updatedReminder) {
        const index = reminderState.complexReminders.findIndex(r => r.id === id);
        if (index !== -1) {
            reminderState.complexReminders[index] = { ...reminderState.complexReminders[index], ...updatedReminder };
        }
    },

    // 删除复杂提醒
    deleteComplexReminder(id) {
        const index = reminderState.complexReminders.findIndex(r => r.id === id);
        if (index !== -1) {
            reminderState.complexReminders.splice(index, 1);
        }
    },

    // 设置加载状态
    setLoading(loading) {
        reminderState.loading = loading;
    },

    // 设置错误信息
    setError(error) {
        reminderState.error = error;
    },

    // 清空所有提醒数据
    clearAll() {
        reminderState.simpleReminders = [];
        reminderState.complexReminders = [];
        reminderState.upcomingReminders = [];
        reminderState.error = null;
        reminderState.lastFetchTime = null;
    }
};

// 计算属性
export const reminderGetters = {
    // 获取今日提醒
    getTodayReminders() {
        const today = new Date();
        const todayStr = today.toDateString();
        
        return reminderState.simpleReminders.filter(reminder => {
            const reminderDate = new Date(reminder.eventTime);
            return reminderDate.toDateString() === todayStr;
        });
    },

    // 获取本周提醒
    getWeekReminders() {
        const now = new Date();
        const weekStart = new Date(now.setDate(now.getDate() - now.getDay()));
        const weekEnd = new Date(now.setDate(now.getDate() - now.getDay() + 6));
        
        return reminderState.simpleReminders.filter(reminder => {
            const reminderDate = new Date(reminder.eventTime);
            return reminderDate >= weekStart && reminderDate <= weekEnd;
        });
    },

    // 根据ID获取提醒
    getReminderById: (id) => {
        return reminderState.simpleReminders.find(r => r.id === id) ||
               reminderState.complexReminders.find(r => r.id === id);
    }
};

export default {
    state: reminderState,
    actions: reminderActions,
    getters: reminderGetters
}; 