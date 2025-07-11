import axios from 'axios';

// 之前的实现可能被覆盖，强制使用相对路径
// 注意：这里不再支持环境变量配置，直接使用相对路径
const API_URL = '/api';

// 创建 Axios 实例，并设置baseURL
const apiClient = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json',
    }
});

// 添加请求拦截器以包含token
apiClient.interceptors.request.use(config => {
    // 每次请求前检查baseURL，确保使用我们设置的路径
    if (config.baseURL !== API_URL) {
        console.warn('baseURL不匹配，强制使用配置的API_URL');
        config.baseURL = API_URL;
    }
    
    const token = localStorage.getItem('accessToken');
    console.log('Interceptor: Checking token for:', config.url);
    
    if (token) {
        // 确保 token 格式正确
        const formattedToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
        config.headers['Authorization'] = formattedToken;
        console.log('Interceptor: Token attached to request');
    } else {
        console.warn('Interceptor: No token found in localStorage');
    }
    return config;
}, error => {
    console.error('Request interceptor error:', error);
    return Promise.reject(error);
});

// 响应拦截器 - 处理错误
apiClient.interceptors.response.use(
    (response) => {
        console.log(`Response from ${response.config.url}:`, response.status);
        return response;
    },
    (error) => {
        console.error('Response error:', error);
        if (error.response) {
            console.error('Error status:', error.response.status);
            console.error('Error data:', error.response.data);
            console.error('Request URL:', error.config.url);
            console.error('Request method:', error.config.method);
            
            // 扩展错误对象，添加更多信息以便前端处理
            error.statusCode = error.response.status;
            
            // 处理不同类型的错误响应格式
            if (error.response.data) {
                // 如果是对象类型，并且包含message字段
                if (typeof error.response.data === 'object' && error.response.data.message) {
                    error.message = error.response.data.message;
                    
                    // 如果有详细错误字段，也添加到error对象
                    if (error.response.data.errors) {
                        error.fieldErrors = error.response.data.errors;
                    }
                } 
                // 如果是字符串类型
                else if (typeof error.response.data === 'string') {
                    error.message = error.response.data;
                }
            }
            
            // 对于403和401错误，提供更详细的调试信息
            if (error.response.status === 403) {
                console.error('403 Forbidden: 权限不足或认证问题');
                console.error('请求头:', error.config.headers);
            }
        }
        
        // 只有在非登录请求时才处理认证错误
        if ((error.response?.status === 401 || error.response?.status === 403) && 
            !error.config.url.includes('/auth/login')) {
            console.log('Authentication error detected');
            
            // 尝试刷新令牌或清除无效令牌
            const token = localStorage.getItem('accessToken');
            if (token) {
                console.log('Token exists but request failed with auth error. Consider refreshing token.');
                // 可以在这里添加令牌刷新逻辑
            }
            
            localStorage.removeItem('accessToken');
            // 不再直接重定向，而是通过状态管理处理
            if (window.vueApp) {
                window.vueApp.$emit('auth-error');
            }
        }
        return Promise.reject(error);
    }
);

// --- 认证相关的API调用 ---
export const login = (credentials) => apiClient.post('/auth/login', credentials);
export const register = (userData) => apiClient.post('/auth/register', userData);
export const getUserProfile = () => apiClient.get('/auth/profile');
export const updateProfile = (profileData) => apiClient.put('/auth/profile', profileData);

// --- 用户信息相关的API调用 ---
export const fetchUserProfile = () => {
    return apiClient.get('/auth/profile');
};

// --- 提醒事项相关的API调用 ---
export const getAllSimpleReminders = (year, month) => {
    // 如果提供了年月参数，则添加到请求URL中
    if (year && month) {
        return apiClient.get(`/reminders/simple?year=${year}&month=${month}`);
    }
    // 否则获取所有提醒
    return apiClient.get('/reminders/simple');
};
export const getSimpleReminderById = (id) => apiClient.get(`/reminders/simple/${id}`);
export const createEvent = (reminder) => apiClient.post('/reminders/simple', reminder);
export const updateEvent = (id, reminder) => apiClient.put(`/reminders/simple/${id}`, reminder);
export const deleteEvent = (id) => apiClient.delete(`/reminders/simple/${id}`);
export const getUpcomingReminders = () => apiClient.get('/reminders/upcoming');

// --- 复杂提醒事项相关的API调用 ---
export const getAllComplexReminders = () => apiClient.get('/reminders/complex');
export const getComplexReminderById = (id) => apiClient.get(`/reminders/complex/${id}`);
export const createComplexReminder = (reminder) => apiClient.post('/reminders/complex', reminder);
export const updateComplexReminder = (id, reminder) => apiClient.put(`/reminders/complex/${id}`, reminder);
export const deleteComplexReminder = (id) => {
    return apiClient.delete(`/complex-reminders/${id}`);
};

/**
 * 获取指定年份范围的法定节假日
 * @param {number} startYear - 开始年份
 * @param {number} endYear - 结束年份
 * @returns {Promise} - 返回节假日数据
 */
export const getHolidaysByYearRange = async (startYear, endYear) => {
    return await apiClient.get(`/holidays`, {
        params: {
            startYear,
            endYear
        }
    });
};

/**
 * 获取指定年份的所有节气信息
 * @param {number} year - 年份
 * @returns {Promise} - 返回节气数据
 */
export const getSolarTermsByYear = async (year) => {
    return await apiClient.get(`/solarterms`, {
        params: { year }
    });
};

/**
 * 获取指定年月的节气信息
 * @param {number} year - 年份
 * @param {number} month - 月份(1-12)
 * @returns {Promise} - 返回指定月份的节气数据
 */
export const getSolarTermsByMonth = async (year, month) => {
    return await apiClient.get(`/solarterms`, {
        params: { year, month }
    });
};

// --- 用户偏好设置相关的API调用 ---
export const getUserPreferences = () => {
    return apiClient.get('/user/preferences');
};

export const getUserPreference = (key) => {
    return apiClient.get(`/user/preferences/${key}`);
};

export const setUserPreference = (key, value, property = null) => {
    return apiClient.put(`/user/preferences/${key}`, { key, value, property });
};

export const deleteUserPreference = (key) => {
    return apiClient.delete(`/user/preferences/${key}`);
};

export const batchUpdateUserPreferences = (preferences, override = false) => {
    return apiClient.put('/user/preferences/batch', { preferences, override });
};

export const updateUserPreferences = (preferences) => {
    return apiClient.put('/user/preferences', preferences);
};

// --- 标签管理相关的API调用 ---
export const getUserTagManagementEnabled = () => {
    return apiClient.get('/user/preferences/userTagManagementEnabled');
};

export const setUserTagManagementEnabled = (enabled) => {
    const key = 'userTagManagementEnabled';
    const value = enabled ? '1' : '0';
    const property = '用户标签管理功能开关，0关闭，1开启';
    return apiClient.put('/user/preferences/userTagManagementEnabled', { key, value, property });
};

export const getUserTagList = () => {
    return apiClient.get('/user/preferences/userTagList');
};

export const setUserTagList = (tagList) => {
    const key = 'userTagList';
    const property = '用户标签列表，|||分隔不同标签，|分隔标题和内容，总长度不超过100个字符';
    return apiClient.put('/user/preferences/userTagList', { key, value: tagList, property });
};

export const deleteUserTagList = () => {
    return apiClient.delete('/user/preferences/userTagList');
};

export default apiClient; 