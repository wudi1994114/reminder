import { handleApiError } from '../utils/helpers';
import cloudConfig from '../config/cloud.js';
import wechatConfig from '../config/wechat.js';

// 根据环境选择API地址
// let API_URL = 'http://127.0.0.1:8080/api';
let API_URL = 'http://192.168.100.174:8080/api';
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
        // 简化调试日志
        console.log('🔍 API请求:', options.method || 'GET', options.url);
        
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
        const fullUrl = options.url.startsWith('http') ? options.url : API_URL + options.url;
        console.log('📡 使用HTTP请求:', fullUrl);
        const token = uni.getStorageSync('accessToken');
        
        const requestOptions = {
            ...options,
            url: fullUrl,
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
        
        // 简化调试输出
        console.log('🚀 发起请求:', requestOptions.method || 'GET', requestOptions.url);
        uni.request(requestOptions);
    });
};

// --- 导出基础请求函数 ---
export { request };

// --- API方法 ---

// 认证相关
export const login = (credentials) => {
    console.log('🔐 login函数接收到的credentials:', JSON.stringify(credentials, null, 2));
    return request({
        url: '/auth/login',
        method: 'POST',
        data: credentials
    });
};

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
}).then(data => {
    if (!Array.isArray(data)) {
        console.warn('getUpcomingReminders API返回的数据不是数组:', data);
        return [];
    }
    return data;
}).catch(error => {
    console.error('获取即将到来的提醒出错:', error);
    return [];
});

// 复杂提醒事项相关
export const getAllComplexReminders = () => request({
    url: '/reminders/complex',
    method: 'GET'
}).then(data => {
    if (!Array.isArray(data)) {
        console.warn('getAllComplexReminders API返回的数据不是数组:', data);
        return [];
    }
    return data;
}).catch(error => {
    console.error('获取复杂提醒出错:', error);
    return [];
});

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

// 节假日相关
export const getHolidaysByYearRange = (startYear, endYear) => request({
    url: `/holidays`,
    method: 'GET',
    data: { startYear, endYear }
}).catch(handleApiError);

export const getCalendarData = (startYear, endYear, apiType = 'all') => {
    startYear = startYear || new Date().getFullYear();
    endYear = endYear || (startYear + 1);
    
    console.log(`请求日历数据: ${startYear}-${endYear}, 类型: ${apiType}`);
    
    let url = `/holidays?startYear=${startYear}&endYear=${endYear}`;
    
    if (apiType && apiType !== 'all') {
        url += `&type=${apiType}`;
    }
    
    return request({
        url,
        method: 'GET'
    })
    .then(data => {
        if (!Array.isArray(data)) {
            console.warn('日历API返回的数据不是数组:', data);
            return [];
        }
        console.log(`获取到 ${data.length} 条日历数据`);
        return data;
    })
    .catch(error => {
        console.error('获取日历数据出错:', error);
        return []; 
    });
};

// WebSocket 相关
export const connectWebSocket = () => {
    return new Promise((resolve, reject) => {
        if (!CLOUD_CONFIG.websocket.enabled) {
            console.warn('WebSocket功能已关闭');
            reject(new Error('WebSocket功能已关闭'));
            return;
        }
        if (!wx.cloud || !wx.cloud.connectContainer) {
            console.warn('当前环境不支持云托管 WebSocket');
            reject(new Error('不支持云托管 WebSocket'));
            return;
        }
        console.log('建立云托管 WebSocket 连接...');
        wx.cloud.connectContainer({
            config: { env: CLOUD_CONFIG.env },
            service: CLOUD_CONFIG.serviceName,
            path: CLOUD_CONFIG.websocket.path,
            success: (res) => {
                console.log('WebSocket 连接成功:', res);
                const { socketTask } = res;
                socketTask.onOpen((openRes) => console.log('WebSocket 连接已建立', openRes));
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
                socketTask.onError((error) => console.error('WebSocket 连接错误:', error));
                socketTask.onClose((closeRes) => console.log('WebSocket 连接已关闭:', closeRes));
                resolve(socketTask);
            },
            fail: (err) => {
                console.error('WebSocket 连接失败:', err);
                reject(err);
            }
        });
    });
};

let webSocketMessageHandlers = [];
export const onWebSocketMessage = (handler) => { webSocketMessageHandlers.push(handler); };
export const offWebSocketMessage = (handler) => {
    const index = webSocketMessageHandlers.indexOf(handler);
    if (index > -1) webSocketMessageHandlers.splice(index, 1);
};
const handleWebSocketMessage = (data) => {
    webSocketMessageHandlers.forEach(handler => {
        try { handler(data); } catch (e) { console.error('WebSocket 消息处理错误:', e); }
    });
};

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
                if (CLOUD_CONFIG.debug.verbose) console.log('WebSocket 消息发送成功:', data);
            },
            fail: (err) => console.error('WebSocket 消息发送失败:', err)
        });
        return true;
    } catch (e) {
        console.error('WebSocket 消息发送异常:', e);
        return false;
    }
};

// 云托管控制
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
        runtime: { hasWxCloud, hasCallContainer, hasConnectContainer, isReady: hasWxCloud && hasCallContainer }
    };
};

export const testCloudConnection = async () => {
    try {
        console.log('🧪 测试云托管连接...');
        const status = getCloudStatus();
        if (!status.runtime.isReady) {
            throw new Error('云托管服务未就绪');
        }
        const result = await request({ url: '/api/health', method: 'GET' });
        console.log('✅ 云托管连接测试成功');
        return { success: true, data: result };
    } catch (error) {
        console.error('❌ 云托管连接测试失败:', error);
        return { success: false, error: error.message };
    }
};

// 用户偏好设置
export const getUserPreferences = () => request({ url: '/user/preferences', method: 'GET' });
export const getUserPreference = (key) => request({ url: `/user/preferences/${key}`, method: 'GET' });
export const setUserPreference = (key, value, property = null) => request({ url: `/user/preferences/${key}`, method: 'PUT', data: { key, value, property } });
export const batchUpdateUserPreferences = (preferences, override = false) => request({ url: '/user/preferences/batch', method: 'PUT', data: { preferences, override } });
export const deleteUserPreference = (key) => request({ url: `/user/preferences/${key}`, method: 'DELETE' });
export const initializeUserPreferences = () => request({ url: '/user/preferences/initialize', method: 'POST' });
export const resetUserPreferences = () => request({ url: '/user/preferences/reset', method: 'POST' });

/**
 * 微信小程序工具类
 * 封装微信小程序常用API，提供统一的调用接口
 */
class WeChatUtils {
  
  static getUserProfile(options = {}) {
    return new Promise((resolve, reject) => {
      // #ifdef MP-WEIXIN
      console.log('📱 尝试获取微信用户信息...');
      
      // 检查是否支持 getUserProfile API
      if (typeof uni !== 'undefined' && uni.getUserProfile) {
        console.log('✅ 使用 uni.getUserProfile API');
        uni.getUserProfile({
          desc: options.desc || '用于完善用户资料和提供个性化服务',
          success: (res) => {
            console.log('✅ 获取用户信息成功:', {
              nickName: res.userInfo?.nickName,
              hasAvatar: !!res.userInfo?.avatarUrl,
              gender: res.userInfo?.gender
            });
            resolve(res.userInfo);
          },
          fail: (error) => {
            console.error('❌ getUserProfile 调用失败:', error);
            
            // 如果是用户拒绝授权，给出友好提示
            if (error.errMsg && error.errMsg.includes('auth deny')) {
              console.log('ℹ️ 用户拒绝授权，返回默认信息');
              resolve({
                nickName: '微信用户',
                avatarUrl: '',
                gender: 0,
                country: '',
                province: '',
                city: '',
                language: 'zh_CN'
              });
            } else {
              reject(error);
            }
          }
        });
      } else if (typeof wx !== 'undefined' && wx.getUserProfile) {
        console.log('✅ 使用 wx.getUserProfile API');
        wx.getUserProfile({
          desc: options.desc || '用于完善用户资料和提供个性化服务',
          success: (res) => {
            console.log('✅ 获取用户信息成功:', {
              nickName: res.userInfo?.nickName,
              hasAvatar: !!res.userInfo?.avatarUrl,
              gender: res.userInfo?.gender
            });
            resolve(res.userInfo);
          },
          fail: (error) => {
            console.error('❌ wx.getUserProfile 调用失败:', error);
            
            // 如果是用户拒绝授权，给出友好提示
            if (error.errMsg && error.errMsg.includes('auth deny')) {
              console.log('ℹ️ 用户拒绝授权，返回默认信息');
              resolve({
                nickName: '微信用户',
                avatarUrl: '',
                gender: 0,
                country: '',
                province: '',
                city: '',
                language: 'zh_CN'
              });
            } else {
              reject(error);
            }
          }
        });
      } else {
        // API不可用，返回默认信息
        console.warn('⚠️ getUserProfile API不可用，返回默认用户信息');
        console.log('💡 建议使用头像昵称填写组件获取用户信息');
        resolve({
          nickName: '微信用户',
          avatarUrl: '',
          gender: 0,
          country: '',
          province: '',
          city: '',
          language: 'zh_CN'
        });
      }
      // #endif
      // #ifndef MP-WEIXIN
      console.error('❌ 非微信小程序环境，无法获取用户信息');
      reject(new Error('当前环境不支持微信用户信息获取'));
      // #endif
    });
  }

  static login(options = {}) {
    console.log('🔐 微信登录开始...');
    
    return new Promise((resolve, reject) => {
      // #ifdef MP-WEIXIN
      const loginOptions = {
        timeout: options.timeout || 8000, // 缩短默认超时时间
        success: (res) => {
          console.log('🎉 微信登录成功');
          if (options.success) options.success(res);
          resolve({ code: res.code, errMsg: res.errMsg || 'login:ok' });
        },
        fail: (error) => {
          console.error('❌ 微信登录失败:', error.errMsg);
          if (options.fail) options.fail(error);
          reject({ errCode: error.errCode || -1, errMsg: error.errMsg || 'login:fail', detail: error });
        },
        complete: (res) => {
          if (options.complete) options.complete(res);
        }
      };
      
      // 快速检查并调用
      if (typeof wx !== 'undefined' && wx.login) {
        wx.login(loginOptions);
      } else {
        uni.login({ provider: 'weixin', ...loginOptions });
      }
      // #endif
      // #ifndef MP-WEIXIN
      const error = new Error('当前环境不支持微信登录');
      if (options.fail) options.fail(error);
      if (options.complete) options.complete({ errMsg: 'login:fail 当前环境不支持微信登录' });
      reject(error);
      // #endif
    });
  }

  static async getLoginCode() {
    const result = await this.login();
    return result.code;
  }

  /**
   * 新的用户信息获取方法 - 适配微信新的头像昵称填写组件
   * 注意：这个方法需要配合页面中的头像昵称填写组件使用
   * @param {Object} userInfo - 从头像昵称填写组件获取的用户信息
   * @param {boolean} showLoading - 是否显示加载提示，默认false
   * @returns {Promise} 处理结果
   */
  static async updateUserInfoFromComponent(userInfo, showLoading = false) {
    if (showLoading) {
      WeChatUtils.showLoading('更新中...');
    }
    
    try {
      console.log('📝 从头像昵称组件更新用户信息:', userInfo);
      
      if (!userInfo) {
        throw new Error('用户信息不能为空');
      }
      
      // 构造更新数据
      const updateData = {};
      
      if (userInfo.nickName) {
        updateData.nickname = userInfo.nickName;
      }
      
      if (userInfo.avatarUrl) {
        updateData.avatarUrl = userInfo.avatarUrl;
      }
      
      // 添加邮箱字段
      if (userInfo.email) {
        updateData.email = userInfo.email;
      }
      
      // 添加手机号字段（后端使用phoneNumber字段名）
      if (userInfo.phone) {
        updateData.phoneNumber = userInfo.phone;
      }
      
      if (Object.keys(updateData).length === 0) {
        console.warn('没有需要更新的用户信息');
        if (showLoading) {
          WeChatUtils.hideLoading();
        }
        return { success: false, message: '没有需要更新的信息' };
      }
      
      console.log('📤 发送到后端的更新数据:', updateData);
      
      // 调用后端更新接口
      const result = await updateProfile(updateData);
      console.log('✅ 用户信息更新成功:', result);
      
      if (showLoading) {
        WeChatUtils.hideLoading();
      }
      
      return { success: true, data: result };
    } catch (error) {
      console.error('❌ 更新用户信息失败:', error);
      
      if (showLoading) {
        WeChatUtils.hideLoading();
        WeChatUtils.showToast(error.message || '更新失败，请重试', 'none', 3000);
      }
      
      return { success: false, error: error.message };
    }
  }
  
  // ... 其他未修改的WeChatUtils方法保持不变 ...
  static shareToWeChat(shareData = {}) {
    return new Promise((resolve, reject) => {
      // #ifdef MP-WEIXIN
      uni.shareWithSystem({
        type: 'text',
        summary: shareData.title || '分享内容',
        success: resolve,
        fail: reject
      });
      // #endif
      // #ifndef MP-WEIXIN
      reject(new Error('当前环境不支持微信分享'));
      // #endif
    });
  }

  static shareToTimeline(shareData = {}) {
    return new Promise((resolve, reject) => {
      // #ifdef MP-WEIXIN
      uni.share({
        provider: 'weixin',
        scene: 'WXSceneTimeline',
        type: 0,
        title: shareData.title || '分享标题',
        summary: shareData.desc || '分享描述',
        imageUrl: shareData.imageUrl,
        success: resolve,
        fail: reject
      });
      // #endif
      // #ifndef MP-WEIXIN
      reject(new Error('当前环境不支持朋友圈分享'));
      // #endif
    });
  }

  static requestPayment(paymentData) {
    return new Promise((resolve, reject) => {
      // #ifdef MP-WEIXIN
      uni.requestPayment({
        ...paymentData,
        success: resolve,
        fail: reject
      });
      // #endif
      // #ifndef MP-WEIXIN
      reject(new Error('当前环境不支持微信支付'));
      // #endif
    });
  }

  static getWeRunData() {
    return new Promise((resolve, reject) => {
      // #ifdef MP-WEIXIN
      uni.getWeRunData({
        success: resolve,
        fail: reject
      });
      // #endif
      // #ifndef MP-WEIXIN
      reject(new Error('当前环境不支持微信运动数据'));
      // #endif
    });
  }

  static getLocation(options = {}) {
    return new Promise((resolve, reject) => {
      uni.getLocation({
        type: options.type || 'wgs84',
        success: resolve,
        fail: reject
      });
    });
  }

  static chooseAddress() {
    return new Promise((resolve, reject) => {
      // #ifdef MP-WEIXIN
      uni.chooseAddress({
        success: resolve,
        fail: reject
      });
      // #endif
      // #ifndef MP-WEIXIN
      reject(new Error('当前环境不支持选择地址'));
      // #endif
    });
  }

  static saveImageToPhotosAlbum(filePath) {
    return new Promise((resolve, reject) => {
      uni.saveImageToPhotosAlbum({
        filePath: filePath,
        success: resolve,
        fail: reject
      });
    });
  }

  static scanCode(options = {}) {
    return new Promise((resolve, reject) => {
      uni.scanCode({
        onlyFromCamera: options.onlyFromCamera || false,
        scanType: options.scanType || ['barCode', 'qrCode'],
        success: resolve,
        fail: reject
      });
    });
  }

  static vibrate(type = 'short') {
    if (type === 'long') {
      uni.vibrateLong();
    } else {
      uni.vibrateShort();
    }
  }

  static setNavigationBarTitle(title) {
    uni.setNavigationBarTitle({
      title: title
    });
  }

  static setNavigationBarColor(options = {}) {
    uni.setNavigationBarColor({
      frontColor: options.frontColor || '#000000',
      backgroundColor: options.backgroundColor || '#ffffff',
      animation: options.animation || {}
    });
  }

  static showLoading(title = '加载中...') {
    uni.showLoading({
      title: title,
      mask: true
    });
  }

  static hideLoading() {
    uni.hideLoading();
  }

  static showToast(title, icon = 'none', duration = 2000) {
    uni.showToast({
      title: title,
      icon: icon,
      duration: duration
    });
  }

  static showModal(options = {}) {
    return new Promise((resolve) => {
      uni.showModal({
        title: options.title || '提示',
        content: options.content || '',
        showCancel: options.showCancel !== false,
        cancelText: options.cancelText || '取消',
        confirmText: options.confirmText || '确定',
        success: (res) => {
          resolve(res.confirm);
        }
      });
    });
  }

  static getSystemInfo() {
    return new Promise((resolve, reject) => {
      uni.getSystemInfo({
        success: resolve,
        fail: reject
      });
    });
  }

  static isWeChatMiniProgram() {
    // #ifdef MP-WEIXIN
    return true;
    // #endif
    // #ifndef MP-WEIXIN
    return false;
    // #endif
  }

  static getVersionInfo() {
    return new Promise((resolve, reject) => {
      // #ifdef MP-WEIXIN
      if (typeof wx !== 'undefined' && wx.getAccountInfoSync) {
        try {
          const accountInfo = wx.getAccountInfoSync();
          resolve({
            version: accountInfo.miniProgram.version,
            envVersion: accountInfo.miniProgram.envVersion
          });
        } catch (error) {
          reject(error);
        }
      } else {
        reject(new Error('获取版本信息失败'));
      }
      // #endif
      // #ifndef MP-WEIXIN
      reject(new Error('当前环境不支持获取版本信息'));
      // #endif
    });
  }


  /**
   * 已重构: 更新用户信息到后台
   * @param {Object} userInfo - 从getUserProfile获取的用户信息
   * @returns {Promise} 更新结果
   */
  static async updateUserProfile(userInfo) {
    console.log('📝 调用用户信息更新接口，数据:', userInfo);
    
    // 构造发送到后端的数据
    const updateData = {
      nickname: userInfo.nickName,
      avatarUrl: userInfo.avatarUrl
    };
    if (userInfo.gender !== undefined) {
      updateData.gender = userInfo.gender;
    }
    
    // 使用全局的`request`函数，它会自动处理Token、云托管和URL
    // 注意：这里使用了`updateProfile`函数，它是`request`的封装，更加简洁
    return updateProfile(updateData);
  }

  /**
   * 已重构: 微信登录API调用 - 将js_code发送给后台
   * @param {Object} data - 登录数据
   * @param {string} data.code - 微信登录凭证
   * @param {Object} data.userInfo - 用户信息（可选）
   * @returns {Promise} 后台登录响应
   */
  static async wechatLogin(data) {
    console.log('🔐 调用后台微信登录接口，数据:', JSON.stringify(data, null, 2));

    // #ifdef MP-WEIXIN
    // 当云托管启用时，使用云托管专用登录接口
    if (CLOUD_CONFIG.enabled && typeof wx !== 'undefined' && wx.cloud && wx.cloud.callContainer) {
        console.log('🚀 使用云托管进行微信登录...');
        return new Promise((resolve, reject) => {
            wx.cloud.callContainer({
                config: { env: CLOUD_CONFIG.env },
                path: '/api/auth/wechat/cloud-login', // 新的云托管登录接口
                method: 'POST',
                header: {
                    'X-WX-SERVICE': CLOUD_CONFIG.serviceName,
                    'Content-Type': 'application/json'
                },
                // 在云托管模式下，我们不再发送code，只发送可选的userInfo
                data: { userInfo: data.userInfo }, 
                success: (res) => {
                    console.log('✅ 云托管微信登录成功:', res);
                    resolve(res.data);
                },
                fail: (err) => {
                    console.error('❌ 云托管微信登录失败:', err);
                    reject({ ...err, message: err.errMsg || '云托管登录请求失败' });
                }
            });
        });
    }
    // #endif

    // 在非云托管环境或非微信小程序环境，使用传统HTTP登录
    console.log('📡 发送传统HTTP登录请求到后端...');
    if (!data || !data.code) {
      console.error('❌ wechatLogin: 传统模式下缺少必要的code参数:', data);
      throw new Error('微信登录数据无效：缺少code参数');
    }

    try {
        const result = await request({
            url: '/auth/wechat/login', // 旧的基于code的登录接口
            method: 'POST',
            data: data
        });
        console.log('✅ 后端微信登录响应:', JSON.stringify(result, null, 2));
        return result;
    } catch (error) {
        console.error('❌ 后端微信登录失败:', error);
        throw error;
    }
  }

  /**
   * 智能微信登录流程 - 获取用户信息并完成登录
   * @param {Object} options - 登录选项
   * @returns {Promise} 登录结果
   */
  static async smartWechatLogin(options = {}) {
    // 显示加载弹窗
    WeChatUtils.showLoading('登录中...');
    
    try {
        console.log('🚀 开始智能微信登录流程...');
        const isCloudEnabled = CLOUD_CONFIG.enabled && WeChatUtils.isWeChatMiniProgram() && typeof wx !== 'undefined' && wx.cloud;
        console.log(`当前登录模式: ${isCloudEnabled ? '☁️ 云托管' : '🌐 HTTP'}`);

        // 1. 获取用户信息（可选，并行执行）
        const userInfoPromise = options.skipUserInfo ? Promise.resolve(null) :
            WeChatUtils.getUserProfile({ desc: '用于完善用户资料' })
            .catch(error => {
                console.warn('⚠️ 获取用户信息失败，将继续无用户信息登录:', error.message);
                return null; // 失败时返回null，不阻断流程
            });
            
        // 2. 获取登录凭证
        // 在云托管模式下，虽然后端不需要code，但前端调用wx.login()可以刷新session，是推荐做法
        const loginPromise = WeChatUtils.login({ timeout: 5000 });

        const [userInfo, loginResult] = await Promise.all([userInfoPromise, loginPromise]);

        if (!isCloudEnabled && !loginResult?.code) {
            throw new Error('获取微信登录凭证(code)失败');
        }

        // 3. 构建登录数据并调用后端
        const wechatLoginData = {};
        if (loginResult?.code) {
            wechatLoginData.code = loginResult.code; // 仅在HTTP模式下需要
        }
        if (userInfo) {
            console.log('✅ 用户信息已获取:', userInfo.nickName || '未知用户');
            wechatLoginData.userInfo = userInfo;
        }

        console.log('🔐 发送登录请求到后端...');
        const response = await WeChatUtils.wechatLogin(wechatLoginData);

        console.log('✅ 登录完成:', response.isNewUser ? '新用户' : '老用户');

        // 隐藏加载弹窗
        WeChatUtils.hideLoading();

        // 快速构建返回结果
        const result = { ...response };
        if (response.isNewUser) {
            result.needCompleteProfile = !userInfo;
        } else {
            result.userInfoUpdated = !!userInfo;
        }

        return result;
    } catch (error) {
      console.error('❌ 智能微信登录失败:', error.message);
      
      // 隐藏加载弹窗
      WeChatUtils.hideLoading();
      
      // 显示失败提示
      const errorMessage = error.message || '登录失败，请重试';
      WeChatUtils.showToast(errorMessage, 'none', 3000);
      
      throw error;
    }
  }

  /**
   * 完整的微信登录流程
   * @param {Object} options - 登录选项
   * @param {boolean} options.withUserInfo - 是否获取用户信息
   * @returns {Promise} 登录结果
   */
  static async loginWithBackend(options = {}) {
    // 显示加载弹窗
    WeChatUtils.showLoading('登录中...');
    
    try {
      const loginResult = await WeChatUtils.login();
      if (!loginResult || !loginResult.code) {
        throw new Error('获取微信登录凭证失败');
      }
      
      const loginData = { code: loginResult.code };
      
      if (options.withUserInfo) {
        try {
          const userInfo = await WeChatUtils.getUserProfile({ desc: '用于完善用户资料' });
          loginData.userInfo = userInfo;
        } catch (userInfoError) {
          console.warn('获取用户信息失败，继续登录流程:', userInfoError);
        }
      }
      
      // 调用重构后的登录接口
      const result = await WeChatUtils.wechatLogin(loginData);
      
      // 隐藏加载弹窗
      WeChatUtils.hideLoading();
      
      return result;
    } catch (error) {
      console.error('微信登录流程失败:', error);
      
      // 隐藏加载弹窗
      WeChatUtils.hideLoading();
      
      // 显示失败提示
      const errorMessage = error.message || '登录失败，请重试';
      WeChatUtils.showToast(errorMessage, 'none', 3000);
      
      throw error;
    }
  }

  /**
   * 请求微信一次性订阅权限
   * @param {string} templateId - 模板ID
   * @returns {Promise} 订阅结果
   */
  static requestSubscribeMessage(templateId) {
    return new Promise((resolve, reject) => {
      // #ifdef MP-WEIXIN
      if (typeof wx !== 'undefined' && wx.requestSubscribeMessage) {
        wx.requestSubscribeMessage({
          tmplIds: [templateId],
          success: (res) => {
            console.log('订阅消息权限请求成功:', res);
            // 检查用户是否同意订阅
            if (res[templateId] === 'accept') {
              resolve({ success: true, granted: true, templateId });
            } else if (res[templateId] === 'reject') {
              resolve({ success: true, granted: false, templateId, reason: '用户拒绝' });
            } else {
              resolve({ success: true, granted: false, templateId, reason: '未知状态' });
            }
          },
          fail: (error) => {
            console.error('订阅消息权限请求失败:', error);
            reject({ success: false, error: error.errMsg || '请求失败' });
          }
        });
      } else {
        console.warn('当前环境不支持微信订阅消息');
        reject({ success: false, error: '当前环境不支持微信订阅消息' });
      }
      // #endif
      // #ifndef MP-WEIXIN
      reject({ success: false, error: '当前环境不支持微信订阅消息' });
      // #endif
    });
  }

  /**
   * 智能请求微信订阅权限 - 带用户友好的提示
   * @param {Object} options - 配置选项
   * @param {string} options.templateId - 模板ID
   * @param {string} options.title - 提示标题
   * @param {string} options.content - 提示内容
   * @param {boolean} options.showToast - 是否显示提示消息，默认false
   * @returns {Promise} 订阅结果
   */
  static async smartRequestSubscribe(options = {}) {
    try {
      const {
        templateId = wechatConfig.subscribeTemplates.reminder,
        title = wechatConfig.subscribe.defaultTitle,
        content = wechatConfig.subscribe.defaultContent,
        showToast = false
      } = options;

      // 先询问用户是否同意
      const userConfirm = await WeChatUtils.showModal({
        title,
        content,
        showCancel: true,
        cancelText: '暂不开启',
        confirmText: '立即开启'
      });

      if (!userConfirm) {
        return { success: false, granted: false, reason: '用户取消' };
      }

      // 用户同意后请求订阅权限
      const result = await WeChatUtils.requestSubscribeMessage(templateId);
      
      if (result.success && result.granted) {
        if (showToast) {
          WeChatUtils.showToast(wechatConfig.subscribe.successMessage, 'success');
        }
        return { success: true, granted: true };
      } else {
        if (showToast) {
          WeChatUtils.showToast(wechatConfig.subscribe.failureMessage, 'none');
        }
        return { success: false, granted: false, reason: result.reason || '权限获取失败' };
      }
    } catch (error) {
      console.error('智能请求订阅权限失败:', error);
      if (showToast) {
        WeChatUtils.showToast(wechatConfig.subscribe.errorMessage, 'none');
      }
      return { success: false, granted: false, error: error.message };
    }
  }
}

// 导出工具类
export default WeChatUtils;

// 也可以按需导出具体方法
export const {
  // 注意：此处导出的 updateUserProfile 和 wechatLogin 是重构后的静态方法
  getUserProfile: wcGetUserProfile, // 避免命名冲突
  login: wcLogin, // 避免命名冲突
  getLoginCode,
  wechatLogin,
  loginWithBackend,
  smartWechatLogin,
  updateUserInfoFromComponent, // 新增：适配新的头像昵称填写组件
  shareToWeChat,
  shareToTimeline,
  requestPayment,
  getWeRunData,
  getLocation,
  chooseAddress,
  saveImageToPhotosAlbum,
  scanCode,
  vibrate,
  setNavigationBarTitle,
  setNavigationBarColor,
  showLoading,
  hideLoading,
  showToast,
  showModal,
  getSystemInfo,
  isWeChatMiniProgram,
  getVersionInfo,
  updateUserProfile,
  requestSubscribeMessage, // 新增：请求订阅权限
  smartRequestSubscribe // 新增：智能请求订阅权限
} = WeChatUtils;