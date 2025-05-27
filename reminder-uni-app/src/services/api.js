import { handleApiError } from '../utils/helpers';

// API基础URL
const API_URL = 'http://192.168.100.154:8080/api';

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
                        // 对于日历数据，如果遇到授权错误，返回空数组而不是拒绝 Promise
                        if (options.url.includes('/reminders/simple')) {
                            console.warn('获取提醒数据需要登录，返回空数组');
                            resolve([]);
                            return;
                        }
                        // 可以在这里重定向到登录页面
                    }
                    
                    reject(res); // 直接 reject 整个响应对象
                }
            },
            fail: (err) => {
                console.error('请求失败:', err);
                
                // 对于日历数据，如果遇到网络错误，也返回空数组
                if (options.url.includes('/reminders/simple')) {
                    console.warn('获取提醒数据失败，返回空数组');
                    resolve([]);
                    return;
                }
                
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
    console.log('getAllSimpleReminders url:', url);
    return request({
        url,
        method: 'GET'
    })
    .then(data => {
        // 确保返回的数据是数组
        if (!Array.isArray(data)) {
            console.warn('API返回的数据不是数组:', data);
            return []; // 返回空数组
        }
        return data;
    })
    .catch(error => {
        console.error('获取提醒列表出错:', error);
        return []; // 出错时返回空数组
    });
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

/**
 * 获取日历数据（包括节假日、调休日等）
 * @param {number} startYear - 开始年份
 * @param {number} endYear - 结束年份
 * @param {string} apiType - 数据类型: 'holidays'(节假日), 'events'(普通事件), 'all'(全部)
 * @returns {Promise} - 返回日历数据
 */
export const getCalendarData = (startYear, endYear, apiType = 'all') => {
    // 参数校验和处理
    startYear = startYear || new Date().getFullYear();
    endYear = endYear || (startYear + 1);
    
    console.log(`请求日历数据: ${startYear}-${endYear}, 类型: ${apiType}`);
    
    let url = `/holidays?startYear=${startYear}&endYear=${endYear}`;
    
    // 如果提供了apiType参数且不为空，添加到URL中
    if (apiType && apiType !== 'all') {
        url += `&type=${apiType}`;
    }
    
    return request({
        url,
        method: 'GET'
    })
    .then(data => {
        // 确保返回的数据是数组
        if (!Array.isArray(data)) {
            console.warn('日历API返回的数据不是数组:', data);
            return []; // 返回空数组
        }
        
        console.log(`获取到 ${data.length} 条日历数据`);
        return data;
    })
    .catch(error => {
        console.error('获取日历数据出错:', error);
        // 对于日历数据，错误时返回空数组，避免阻断UI显示
        return []; 
    });
}; 