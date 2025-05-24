import { request } from './request.js';
import { handleApiError } from '@/utils/helpers.js';

// 提醒事项相关API
export const reminderApi = {
    // ========== 简单提醒相关 ==========
    
    // 获取所有简单提醒
    getAllSimpleReminders: (year, month) => {
        let url = '/reminders/simple';
        if (year && month) {
            url += `?year=${year}&month=${month}`;
        }
        console.log('getAllSimpleReminders url:', url);
        return request({
            url,
            method: 'GET'
        })
        .then(data => {
            if (!Array.isArray(data)) {
                console.warn('API返回的数据不是数组:', data);
                return [];
            }
            return data;
        })
        .catch(error => {
            console.error('获取提醒列表出错:', error);
            return [];
        });
    },

    // 根据ID获取简单提醒
    getSimpleReminderById: (id) => request({
        url: `/reminders/simple/${id}`,
        method: 'GET'
    }).catch(handleApiError),

    // 创建简单提醒
    createSimpleReminder: (reminder) => request({
        url: '/reminders/simple',
        method: 'POST',
        data: reminder
    }).catch(handleApiError),

    // 更新简单提醒
    updateSimpleReminder: (id, reminder) => request({
        url: `/reminders/simple/${id}`,
        method: 'PUT',
        data: reminder
    }).catch(handleApiError),

    // 删除简单提醒
    deleteSimpleReminder: (id) => request({
        url: `/reminders/simple/${id}`,
        method: 'DELETE'
    }).catch(handleApiError),

    // 获取即将到来的提醒
    getUpcomingReminders: () => request({
        url: '/reminders/upcoming',
        method: 'GET'
    }).catch(handleApiError),

    // ========== 复杂提醒相关 ==========
    
    // 获取所有复杂提醒
    getAllComplexReminders: () => request({
        url: '/reminders/complex',
        method: 'GET'
    }).catch(handleApiError),

    // 根据ID获取复杂提醒
    getComplexReminderById: (id) => request({
        url: `/reminders/complex/${id}`,
        method: 'GET'
    }).catch(handleApiError),

    // 创建复杂提醒
    createComplexReminder: (reminder) => request({
        url: '/reminders/complex',
        method: 'POST',
        data: reminder
    }).catch(handleApiError),

    // 更新复杂提醒
    updateComplexReminder: (id, reminder) => request({
        url: `/reminders/complex/${id}`,
        method: 'PUT',
        data: reminder
    }).catch(handleApiError),

    // 删除复杂提醒
    deleteComplexReminder: (id) => request({
        url: `/reminders/complex/${id}`,
        method: 'DELETE'
    }).catch(handleApiError)
};

// 兼容旧API名称
export const createEvent = reminderApi.createSimpleReminder;
export const updateEvent = reminderApi.updateSimpleReminder;
export const deleteEvent = reminderApi.deleteSimpleReminder; 