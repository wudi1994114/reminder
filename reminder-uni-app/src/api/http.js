/**
 * HTTP请求封装层
 * 负责：HTTP请求、云托管调用、Token管理、错误处理
 */

import cloudConfig from '../config/cloud.js';
import { getApiBaseUrl, TIMEOUT } from '../config/api.js';

// 获取API基础URL
const API_URL = getApiBaseUrl();

/**
 * 云托管请求方法
 */
export const callContainer = (options) => {
    return new Promise((resolve, reject) => {
        const token = uni.getStorageSync('accessToken');

        // 智能处理路径，防止重复添加 '/api'
        let requestPath = options.url.replace(API_URL, '');
        if (!requestPath.startsWith('/api/')) {
            requestPath = '/api' + (requestPath.startsWith('/') ? '' : '/') + requestPath;
        }

        const callOptions = {
            config: {
                env: cloudConfig.env
            },
            path: requestPath,
            method: options.method || 'GET',
            header: {
                'X-WX-SERVICE': cloudConfig.serviceName,
                'Content-Type': 'application/json',
                ...options.header
            },
            success: (res) => {
                console.log('云托管请求成功:', options.url, res);
                resolve(res.data || res);
            },
            fail: (err) => {
                console.error('云托管请求失败:', err);

                // 对于特定接口，失败时返回默认值
                if (options.url.includes('/reminders/simple')) {
                    console.warn('获取提醒数据失败，返回空数组');
                    resolve([]);
                    return;
                }

                reject({
                    ...err,
                    message: err.errMsg || '云托管请求失败'
                });
            }
        };

        // 添加请求数据
        if (options.data) {
            callOptions.data = options.data;
            console.log('云托管请求数据:', options.data);
        }

        // 添加认证Token
        if (token) {
            callOptions.header['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
        }

        // 调试日志
        console.log('云托管最终请求配置:', {
            path: callOptions.path,
            method: callOptions.method,
            hasData: !!callOptions.data,
            dataKeys: callOptions.data ? Object.keys(callOptions.data) : [],
            hasAuth: !!callOptions.header['Authorization']
        });

        wx.cloud.callContainer(callOptions);
    });
};

/**
 * 封装uni.request为Promise风格
 */
export const request = (options) => {
    return new Promise((resolve, reject) => {
        console.log('🔍 API请求:', options.method || 'GET', options.url);
        
        // 优先使用云托管
        if (cloudConfig.enabled) {
            // #ifdef MP-WEIXIN
            if (typeof wx !== 'undefined' && wx.cloud && wx.cloud.callContainer) {
                console.log('✅ 使用云托管请求:', options.url);
                callContainer(options).then(resolve).catch(reject);
                return;
            } else {
                console.warn('⚠️ 微信云服务未初始化，降级使用HTTP请求');
            }
            // #endif
            // #ifndef MP-WEIXIN
            console.log('🌐 非微信环境，使用HTTP请求');
            // #endif
        } else {
            console.log('🔄 云托管已禁用，使用HTTP请求');
        }
        
        // 降级使用传统HTTP请求
        const fullUrl = options.url.startsWith('http') ? options.url : API_URL + options.url;
        console.log('📡 使用HTTP请求:', fullUrl);
        const token = uni.getStorageSync('accessToken');
        
        const requestOptions = {
            ...options,
            url: fullUrl,
            timeout: options.timeout || TIMEOUT,
            header: {
                'Content-Type': 'application/json',
                ...options.header
            },
            success: (res) => {
                console.log('API请求成功:', options.url, res.statusCode);
                
                // HTTP 请求状态码检查
                if (res.statusCode >= 200 && res.statusCode < 300) {
                    resolve(res.data);
                } else {
                    console.error('请求错误:', res.statusCode, res);
                    
                    // 处理401/403认证错误
                    if (res.statusCode === 401 || res.statusCode === 403) {
                        console.log('认证失败，但保留token以便重试');

                        // 对于特定接口，返回空数组
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
                console.error('请求失败:', err);
                
                // 网络错误处理
                let errorMessage = '网络连接失败';
                if (err.errMsg) {
                    if (err.errMsg.includes('ERR_CONNECTION_REFUSED')) {
                        errorMessage = '服务器连接被拒绝，请检查网络或联系管理员';
                    } else if (err.errMsg.includes('timeout')) {
                        errorMessage = '请求超时，请检查网络连接';
                    } else if (err.errMsg.includes('fail')) {
                        errorMessage = '网络请求失败，请稍后重试';
                    }
                }
                
                // 对于特定接口，返回空数组
                if (options.url.includes('/reminders/simple')) {
                    console.warn('获取提醒数据失败，返回空数组');
                    resolve([]);
                    return;
                }
                
                // 显示错误提示
                uni.showToast({
                    title: errorMessage,
                    icon: 'none',
                    duration: 3000
                });
                
                reject({
                    ...err,
                    message: errorMessage
                });
            }
        };
        
        // 添加认证Token
        if (token) {
            requestOptions.header['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
        }
        
        console.log('🚀 发起请求:', requestOptions.method || 'GET', requestOptions.url);
        uni.request(requestOptions);
    });
};

/**
 * 导出请求配置
 */
export const HTTP_CONFIG = {
    baseURL: API_URL,
    timeout: TIMEOUT,
    headers: {
        'Content-Type': 'application/json'
    }
};

export default {
    request,
    callContainer,
    HTTP_CONFIG
};

