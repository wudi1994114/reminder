import { handleApiError } from '../utils/helpers';
import cloudConfig from '../config/cloud.js';

// 根据环境选择API地址
let API_URL = 'http://127.0.0.1:8080/api';
// let API_URL = 'http://123.57.175.66/task/api';
// let API_URL = 'https://bewangji-166224-6-1362668225.sh.run.tcloudbase.com/api';

// 使用外部配置文件
const CLOUD_CONFIG = cloudConfig;

// 云托管请求方法
const callContainer = (options) => {
    return new Promise((resolve, reject) => {
        const token = uni.getStorageSync('accessToken');
        
        const callOptions = {
            config: {
                env: CLOUD_CONFIG.env
            },
            path: '/api' + options.url.replace(API_URL, ''), // 在路径前追加/api
            method: options.method || 'GET',
            header: {
                'X-WX-SERVICE': CLOUD_CONFIG.serviceName,
                'Content-Type': 'application/json',
                ...options.header
            },
            data: options.data,
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
        
        // 添加认证Token
        if (token) {
            callOptions.header['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
        }
        
        wx.cloud.callContainer(callOptions);
    });
};

// 封装uni.request为Promise风格
const request = (options) => {
    return new Promise((resolve, reject) => {
        // 优先使用云托管
        if (CLOUD_CONFIG.enabled) {
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
        console.log('📡 使用HTTP请求:', options.url);
        const token = uni.getStorageSync('accessToken');
        
        const requestOptions = {
            ...options,
            url: options.url.startsWith('http') ? options.url : API_URL + options.url,
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
                    // 处理HTTP错误
                    console.error('请求错误:', res.statusCode, res);
                    
                    // 处理401/403认证错误
                    if (res.statusCode === 401 || res.statusCode === 403) {
                        console.log('认证失败，清除token');
                        uni.removeStorageSync('accessToken');
                        
                        // 对于日历数据，如果遇到授权错误，返回空数组而不是拒绝 Promise
                        if (options.url.includes('/reminders/simple')) {
                            console.warn('获取提醒数据需要登录，返回空数组');
                            resolve([]);
                            return;
                        }
                        
                        // 显示登录提示
                        uni.showModal({
                            title: '登录已过期',
                            content: '请重新登录',
                            showCancel: false,
                            success: () => {
                                uni.reLaunch({
                                    url: '/pages/login/login'
                                });
                            }
                        });
                    }
                    
                    reject(res); // 直接 reject 整个响应对象
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
                
                // 对于日历数据，如果遇到网络错误，也返回空数组
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
        
        console.log('发起API请求:', requestOptions.url);
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

export const register = (userData) => {
    console.log('注册API调用，数据:', userData);
    return request({
        url: '/auth/register',
        method: 'POST',
        data: userData
    });
};

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

/**
 * WebSocket 云托管连接
 */
export const connectWebSocket = () => {
    return new Promise((resolve, reject) => {
        // 检查WebSocket功能是否启用
        if (!CLOUD_CONFIG.websocket.enabled) {
            console.warn('WebSocket功能已关闭');
            reject(new Error('WebSocket功能已关闭'));
            return;
        }
        
        // 检查是否支持云托管 WebSocket
        if (!wx.cloud || !wx.cloud.connectContainer) {
            console.warn('当前环境不支持云托管 WebSocket');
            reject(new Error('不支持云托管 WebSocket'));
            return;
        }

        console.log('建立云托管 WebSocket 连接...');
        
        wx.cloud.connectContainer({
            config: {
                env: CLOUD_CONFIG.env
            },
            service: CLOUD_CONFIG.serviceName,
            path: CLOUD_CONFIG.websocket.path,
            success: (res) => {
                console.log('WebSocket 连接成功:', res);
                const { socketTask } = res;
                
                // 设置事件监听
                socketTask.onOpen((openRes) => {
                    console.log('WebSocket 连接已建立', openRes);
                });
                
                socketTask.onMessage((message) => {
                    if (CLOUD_CONFIG.debug.verbose) {
                        console.log('收到 WebSocket 消息:', message.data);
                    }
                    
                    try {
                        const data = JSON.parse(message.data);
                        handleWebSocketMessage(data);
                    } catch (e) {
                        if (CLOUD_CONFIG.debug.verbose) {
                            console.log('收到文本消息:', message.data);
                        }
                    }
                });
                
                socketTask.onError((error) => {
                    console.error('WebSocket 连接错误:', error);
                });
                
                socketTask.onClose((closeRes) => {
                    console.log('WebSocket 连接已关闭:', closeRes);
                });
                
                resolve(socketTask);
            },
            fail: (err) => {
                console.error('WebSocket 连接失败:', err);
                reject(err);
            }
        });
    });
};

// WebSocket 消息处理
let webSocketMessageHandlers = [];

export const onWebSocketMessage = (handler) => {
    webSocketMessageHandlers.push(handler);
};

export const offWebSocketMessage = (handler) => {
    const index = webSocketMessageHandlers.indexOf(handler);
    if (index > -1) {
        webSocketMessageHandlers.splice(index, 1);
    }
};

const handleWebSocketMessage = (data) => {
    webSocketMessageHandlers.forEach(handler => {
        try {
            handler(data);
        } catch (e) {
            console.error('WebSocket 消息处理错误:', e);
        }
    });
};

// WebSocket 消息发送
export const sendWebSocketMessage = (socketTask, message) => {
    if (!socketTask) {
        console.error('WebSocket 连接不存在');
        return false;
    }
    
    try {
        const data = typeof message === 'object' ? JSON.stringify(message) : message;
        socketTask.send({
            data: data,
            success: () => {
                if (CLOUD_CONFIG.debug.verbose) {
                    console.log('WebSocket 消息发送成功:', data);
                }
            },
            fail: (err) => {
                console.error('WebSocket 消息发送失败:', err);
            }
        });
        return true;
    } catch (e) {
        console.error('WebSocket 消息发送异常:', e);
        return false;
    }
};

/**
 * 云托管开关控制
 */
export const setCloudEnabled = (enabled) => {
    CLOUD_CONFIG.enabled = enabled;
    console.log(`云托管已${enabled ? '启用' : '关闭'}`);
};

export const getCloudStatus = () => {
    const hasWxCloud = typeof wx !== 'undefined' && wx.cloud;
    const hasCallContainer = hasWxCloud && wx.cloud.callContainer;
    const hasConnectContainer = hasWxCloud && wx.cloud.connectContainer;
    
    return {
        enabled: CLOUD_CONFIG.enabled,
        env: CLOUD_CONFIG.env,
        serviceName: CLOUD_CONFIG.serviceName,
        websocketEnabled: CLOUD_CONFIG.websocket.enabled,
        // 运行时状态
        runtime: {
            hasWxCloud,
            hasCallContainer,
            hasConnectContainer,
            isReady: hasWxCloud && hasCallContainer
        }
    };
};

// 测试云托管连接
export const testCloudConnection = async () => {
    try {
        console.log('🧪 测试云托管连接...');
        const status = getCloudStatus();
        
        if (!status.runtime.isReady) {
            throw new Error('云托管服务未就绪');
        }
        
        // 发送测试请求
        const result = await request({
            url: '/api/health', // 假设有健康检查接口
            method: 'GET'
        });
        
        console.log('✅ 云托管连接测试成功');
        return { success: true, data: result };
    } catch (error) {
        console.error('❌ 云托管连接测试失败:', error);
        return { success: false, error: error.message };
    }
}; 