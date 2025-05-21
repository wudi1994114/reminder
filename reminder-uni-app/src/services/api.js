import { handleApiError } from '../utils/helpers';

// API基础URL
const API_URL = 'http://localhost:8080/api';

// 封装uni.request为Promise风格
const request = (options) => {
    return new Promise((resolve, reject) => {
        const token = uni.getStorageSync('accessToken');
        
        const requestOptions = {
            ...options,
            url: options.url.startsWith('http') ? options.url : API_URL + options.url,
            header: {
                'Content-Type': 'application/json',
                ...options.header
            },
            success: (res) => {
                // HTTP 请求状态码检查
                if (res.statusCode >= 200 && res.statusCode < 300) {
                    resolve(res.data);
                } else {
                    // 处理HTTP错误
                    console.error('请求错误:', res.statusCode, res);
                    
                    // 处理401/403认证错误
                    if (res.statusCode === 401 || res.statusCode === 403) {
                        uni.removeStorageSync('accessToken');
                        // 可以在这里重定向到登录页面
                    }
                    
                    reject(res); // 直接 reject 整个响应对象
                }
            },
            fail: (err) => {
                console.error('请求失败:', err);
                reject(err);
            }
        };
        
        // 添加认证Token
        if (token) {
            requestOptions.header['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
        }
        
        uni.request(requestOptions);
    });
};

// API方法

// 认证相关
export const login = (credentials) => request({
    url: '/auth/login',
    method: 'POST',
    data: credentials
});

export const register = (userData) => request({
    url: '/auth/register',
    method: 'POST',
    data: userData
});

export const getUserProfile = () => request({
    url: '/auth/profile',
    method: 'GET'
}).catch(handleApiError);

export const updateProfile = (profileData) => request({
    url: '/auth/profile',
    method: 'PUT',
    data: profileData
}).catch(handleApiError);

// 提醒事项相关
export const getAllSimpleReminders = (year, month) => {
    // 如果提供了年月参数，则添加到请求URL中
    let url = '/reminders/simple';
    if (year && month) {
        url += `?year=${year}&month=${month}`;
    }
    
    return request({
        url,
        method: 'GET'
    }).catch(handleApiError);
};

export const getSimpleReminderById = (id) => request({
    url: `/reminders/simple/${id}`,
    method: 'GET'
}).catch(handleApiError);

export const createEvent = (reminder) => request({
    url: '/reminders/simple',
    method: 'POST',
    data: reminder
}).catch(handleApiError);

export const updateEvent = (id, reminder) => request({
    url: `/reminders/simple/${id}`,
    method: 'PUT',
    data: reminder
}).catch(handleApiError);

export const deleteEvent = (id) => request({
    url: `/reminders/simple/${id}`,
    method: 'DELETE'
}).catch(handleApiError);

export const getUpcomingReminders = () => request({
    url: '/reminders/upcoming',
    method: 'GET'
}).catch(handleApiError);

// 复杂提醒事项相关
export const getAllComplexReminders = () => request({
    url: '/reminders/complex',
    method: 'GET'
}).catch(handleApiError);

export const getComplexReminderById = (id) => request({
    url: `/reminders/complex/${id}`,
    method: 'GET'
}).catch(handleApiError);

export const createComplexReminder = (reminder) => request({
    url: '/reminders/complex',
    method: 'POST',
    data: reminder
}).catch(handleApiError);

export const updateComplexReminder = (id, reminder) => request({
    url: `/reminders/complex/${id}`,
    method: 'PUT',
    data: reminder
}).catch(handleApiError);

export const deleteComplexReminder = (id) => request({
    url: `/reminders/complex/${id}`,
    method: 'DELETE'
}).catch(handleApiError);

/**
 * 获取指定年份范围的法定节假日
 * @param {number} startYear - 开始年份
 * @param {number} endYear - 结束年份
 * @returns {Promise} - 返回节假日数据
 */
export const getHolidaysByYearRange = (startYear, endYear) => request({
    url: `/holidays`,
    method: 'GET',
    data: { startYear, endYear }
}).catch(handleApiError); 