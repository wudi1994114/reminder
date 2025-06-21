import config from '@/config/env.js';

// API基础URL
const API_URL = config.API_BASE_URL;

// 封装uni.request为Promise风格
export const request = (options) => {
    return new Promise((resolve, reject) => {
        const token = uni.getStorageSync('accessToken');
        
        const requestOptions = {
            ...options,
            url: options.url.startsWith('http') ? options.url : API_URL + options.url,
            timeout: config.REQUEST_TIMEOUT,
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
                    if (config.DEBUG) {
                        console.error('请求错误:', res.statusCode, res);
                    }
                    
                    // 处理401/403认证错误
                    if (res.statusCode === 401 || res.statusCode === 403) {
                        // 不再自动清除token，保留以便重试
                        console.log('认证失败，但保留token以便重试');
                        // 对于日历数据，如果遇到授权错误，返回空数组而不是拒绝 Promise
                        if (options.url.includes('/reminders/simple')) {
                            console.warn('获取提醒数据需要登录，返回空数组');
                            resolve([]);
                            return;
                        }
                    }
                    
                    reject(res);
                }
            },
            fail: (err) => {
                if (config.DEBUG) {
                    console.error('请求失败:', err);
                }
                
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

// 请求拦截器配置
export const API_CONFIG = {
    baseURL: API_URL,
    timeout: config.REQUEST_TIMEOUT,
    headers: {
        'Content-Type': 'application/json'
    }
}; 