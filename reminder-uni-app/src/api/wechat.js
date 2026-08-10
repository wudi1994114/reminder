/**
 * 微信小程序专用API
 * 负责：微信登录、用户信息、支付、分享等微信特有功能
 */

import { request } from './http.js';
import { updateProfile } from './auth.js';
import wechatConfig from '../config/wechat.js';

/**
 * 检查是否为微信小程序环境
 */
export function isWeChatMiniProgram() {
    // #ifdef MP-WEIXIN
    return true;
    // #endif
    // #ifndef MP-WEIXIN
    return false;
    // #endif
}

/**
 * 微信登录
 * @param {Object} options - 登录选项
 * @returns {Promise<{code: string, errMsg: string}>}
 */
export function wechatLogin(options = {}) {
    console.log('🔐 微信登录开始...');
    
    return new Promise((resolve, reject) => {
        // #ifdef MP-WEIXIN
        const loginOptions = {
            timeout: options.timeout || 8000,
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

/**
 * 获取微信登录code
 * @returns {Promise<string>}
 */
export async function getLoginCode() {
    const result = await wechatLogin();
    return result.code;
}

/**
 * 智能微信登录 - 完整流程
 * @param {Object} options - 登录选项
 * @returns {Promise<{accessToken: string, user: Object}>}
 */
export async function smartWechatLogin(options = {}) {
    try {
        console.log('🔐 开始智能微信登录流程...');
        
        // 1. 获取code
        const loginResult = await wechatLogin(options);
        console.log('✅ 获取微信登录凭证成功');
        
        // 2. 调用后端登录接口
        const loginData = { code: loginResult.code };
        const response = await loginWithBackend(loginData);
        
        console.log('✅ 后端登录成功');
        return response;
    } catch (error) {
        console.error('❌ 智能微信登录失败:', error);
        throw error;
    }
}

/**
 * 使用后端登录接口
 * @param {Object} data - 登录数据 {code: string}
 * @returns {Promise<{accessToken: string, user: Object}>}
 */
export async function loginWithBackend(data) {
    console.log('🔐 调用后台微信登录接口');
    return request({
        url: '/auth/wechat/login',
        method: 'POST',
        data: data
    });
}

/**
 * 从组件更新用户信息（适配微信新的头像昵称填写组件）
 * @param {Object} userInfo - 用户信息
 * @param {boolean} showLoading - 是否显示加载提示
 * @returns {Promise<{success: boolean, data?: Object, error?: string}>}
 */
export async function updateUserInfoFromComponent(userInfo, showLoading = false) {
    if (showLoading) {
        uni.showLoading({ title: '更新中...', mask: true });
    }
    
    try {
        console.log('📝 从头像昵称组件更新用户信息:', userInfo);
        
        if (!userInfo) {
            throw new Error('用户信息不能为空');
        }
        
        const updateData = {};
        
        if (userInfo.nickName) {
            updateData.nickname = userInfo.nickName;
        }
        
        if (userInfo.avatarUrl) {
            updateData.avatarUrl = userInfo.avatarUrl;
        }
        
        if (userInfo.email) {
            updateData.email = userInfo.email;
        }
        
        if (userInfo.phone) {
            updateData.phoneNumber = userInfo.phone;
        }
        
        if (Object.keys(updateData).length === 0) {
            console.warn('没有需要更新的用户信息');
            if (showLoading) uni.hideLoading();
            return { success: false, message: '没有需要更新的信息' };
        }
        
        console.log('📤 发送到后端的更新数据:', updateData);
        
        const result = await updateProfile(updateData);
        console.log('✅ 用户信息更新成功:', result);
        
        if (showLoading) uni.hideLoading();
        
        return { success: true, data: result };
    } catch (error) {
        console.error('❌ 更新用户信息失败:', error);
        
        if (showLoading) {
            uni.hideLoading();
            uni.showToast({
                title: error.message || '更新失败，请重试',
                icon: 'none',
                duration: 3000
            });
        }
        
        return { success: false, error: error.message };
    }
}

/**
 * 获取系统信息（使用新API）
 * @returns {Promise<Object>}
 */
export async function getSystemInfo() {
    try {
        // #ifdef MP-WEIXIN
        if (typeof wx !== 'undefined') {
            const [systemSetting, deviceInfo, windowInfo, appBaseInfo] = await Promise.all([
                new Promise((resolve) => {
                    try {
                        const setting = wx.getSystemSetting ? wx.getSystemSetting() : {};
                        resolve(setting);
                    } catch (error) {
                        console.warn('获取系统设置失败:', error);
                        resolve({});
                    }
                }),
                new Promise((resolve) => {
                    try {
                        const device = wx.getDeviceInfo ? wx.getDeviceInfo() : {};
                        resolve(device);
                    } catch (error) {
                        console.warn('获取设备信息失败:', error);
                        resolve({});
                    }
                }),
                new Promise((resolve) => {
                    try {
                        const window = wx.getWindowInfo ? wx.getWindowInfo() : {};
                        resolve(window);
                    } catch (error) {
                        console.warn('获取窗口信息失败:', error);
                        resolve({});
                    }
                }),
                new Promise((resolve) => {
                    try {
                        const app = wx.getAppBaseInfo ? wx.getAppBaseInfo() : {};
                        resolve(app);
                    } catch (error) {
                        console.warn('获取应用信息失败:', error);
                        resolve({});
                    }
                })
            ]);

            const combinedInfo = {
                ...systemSetting,
                ...deviceInfo,
                ...windowInfo,
                ...appBaseInfo,
                platform: deviceInfo.platform || systemSetting.platform || 'unknown',
                system: deviceInfo.system || 'unknown',
                model: deviceInfo.model || 'unknown',
                brand: deviceInfo.brand || 'unknown',
                screenWidth: windowInfo.screenWidth || 0,
                screenHeight: windowInfo.screenHeight || 0,
                windowWidth: windowInfo.windowWidth || 0,
                windowHeight: windowInfo.windowHeight || 0,
                pixelRatio: windowInfo.pixelRatio || 1,
                language: appBaseInfo.language || systemSetting.language || 'zh_CN',
                version: appBaseInfo.version || 'unknown',
                _apiVersion: 'new'
            };

            console.log('✅ 使用新API获取系统信息成功');
            return combinedInfo;
        }
        // #endif

        // 降级
        return new Promise((resolve, reject) => {
            uni.getSystemInfo({
                success: (info) => {
                    console.log('⚠️ 降级使用 uni.getSystemInfo');
                    resolve({ ...info, _apiVersion: 'fallback' });
                },
                fail: reject
            });
        });

    } catch (error) {
        console.error('获取系统信息失败:', error);
        return new Promise((resolve, reject) => {
            uni.getSystemInfo({
                success: (info) => resolve({ ...info, _apiVersion: 'error_fallback' }),
                fail: reject
            });
        });
    }
}

/**
 * 请求订阅消息
 * @param {string} templateId - 模板ID
 * @returns {Promise<{success: boolean, granted: boolean}>}
 */
export function requestSubscribeMessage(templateId) {
    return new Promise((resolve) => {
        // #ifdef MP-WEIXIN
        if (typeof wx !== 'undefined' && wx.requestSubscribeMessage) {
            wx.requestSubscribeMessage({
                tmplIds: [templateId],
                success: (res) => {
                    const granted = res[templateId] === 'accept';
                    console.log(granted ? '✅ 用户同意订阅' : '❌ 用户拒绝订阅');
                    resolve({ success: true, granted });
                },
                fail: (error) => {
                    console.error('❌ 请求订阅失败:', error);
                    resolve({ success: false, granted: false, error: error.errMsg });
                }
            });
        } else {
            console.warn('⚠️ requestSubscribeMessage API不可用');
            resolve({ success: false, granted: false, error: 'API不可用' });
        }
        // #endif
        // #ifndef MP-WEIXIN
        console.warn('⚠️ 非微信环境，无法请求订阅');
        resolve({ success: false, granted: false, error: '非微信环境' });
        // #endif
    });
}

/**
 * 智能请求订阅消息（带用户确认）
 * @param {Object} options - 选项
 * @param {string} options.templateId - 模板ID
 * @param {string} options.title - 提示标题
 * @param {string} options.content - 提示内容
 * @param {boolean} options.showToast - 是否显示成功提示
 * @returns {Promise<{success: boolean, granted: boolean, reason?: string}>}
 */
export async function smartRequestSubscribe(options = {}) {
    try {
        const {
            templateId = wechatConfig.subscribeTemplates.reminder, // 使用配置的模板ID
            title = wechatConfig.subscribe.defaultTitle,
            content = wechatConfig.subscribe.defaultContent,
            showToast = false
        } = options;

        // 先询问用户是否同意
        const userConfirm = await new Promise((resolve) => {
            uni.showModal({
                title,
                content,
                showCancel: true,
                cancelText: '暂不开启',
                confirmText: '立即开启',
                success: (res) => {
                    resolve(res.confirm);
                },
                fail: () => {
                    resolve(false);
                }
            });
        });

        if (!userConfirm) {
            return { success: false, granted: false, reason: '用户取消' };
        }

        // 用户同意后请求订阅权限
        const result = await requestSubscribeMessage(templateId);
        
        if (result.success && result.granted) {
            if (showToast) {
                uni.showToast({
                    title: '订阅成功',
                    icon: 'success'
                });
            }
            return { success: true, granted: true };
        } else {
            return { success: false, granted: false, reason: result.error || '订阅失败' };
        }
    } catch (error) {
        console.error('❌ 智能请求订阅失败:', error);
        return { success: false, granted: false, reason: error.message };
    }
}

/**
 * 分享到微信
 */
export function shareToWeChat(shareData = {}) {
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

/**
 * 扫码
 */
export function scanCode(options = {}) {
    return new Promise((resolve, reject) => {
        uni.scanCode({
            onlyFromCamera: options.onlyFromCamera || false,
            scanType: options.scanType || ['barCode', 'qrCode'],
            success: resolve,
            fail: reject
        });
    });
}

// 导出所有API
export default {
    isWeChatMiniProgram,
    wechatLogin,
    getLoginCode,
    smartWechatLogin,
    loginWithBackend,
    updateUserInfoFromComponent,
    getSystemInfo,
    requestSubscribeMessage,
    smartRequestSubscribe,
    shareToWeChat,
    scanCode
};
