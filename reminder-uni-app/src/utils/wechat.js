/**
 * 微信小程序工具类
 * 封装微信小程序常用API，提供统一的调用接口
 */

class WeChatUtils {
  
  /**
   * 获取用户信息
   * @param {Object} options - 配置选项
   * @returns {Promise} 用户信息
   */
  static getUserProfile(options = {}) {
    return new Promise((resolve, reject) => {
      // #ifdef MP-WEIXIN
      uni.getUserProfile({
        desc: options.desc || '用于完善用户资料',
        success: (res) => {
          console.log('获取用户信息成功:', res);
          resolve(res.userInfo);
        },
        fail: (error) => {
          console.error('获取用户信息失败:', error);
          reject(error);
        }
      });
      // #endif
      
      // #ifndef MP-WEIXIN
      // 非微信小程序环境的处理
      reject(new Error('当前环境不支持微信用户信息获取'));
      // #endif
    });
  }

  /**
   * 微信登录 - wx.login(Object object)
   * @param {Object} options - 登录配置选项
   * @param {function} options.success - 成功回调
   * @param {function} options.fail - 失败回调
   * @param {function} options.complete - 完成回调
   * @returns {Promise} 登录凭证
   */
  static login(options = {}) {
    return new Promise((resolve, reject) => {
      // #ifdef MP-WEIXIN
      const loginOptions = {
        timeout: options.timeout || 10000, // 默认超时时间10秒
        success: (res) => {
          console.log('微信登录成功:', res);
          
          // 调用用户提供的成功回调
          if (options.success && typeof options.success === 'function') {
            options.success(res);
          }
          
          // 返回完整的登录结果
          resolve({
            code: res.code,
            errMsg: res.errMsg || 'login:ok'
          });
        },
        fail: (error) => {
          console.error('微信登录失败:', error);
          
          // 调用用户提供的失败回调
          if (options.fail && typeof options.fail === 'function') {
            options.fail(error);
          }
          
          // 格式化错误信息
          const formattedError = {
            errCode: error.errCode || -1,
            errMsg: error.errMsg || 'login:fail',
            detail: error
          };
          
          reject(formattedError);
        },
        complete: (res) => {
          // 调用用户提供的完成回调
          if (options.complete && typeof options.complete === 'function') {
            options.complete(res);
          }
        }
      };
      
      // 直接调用wx.login
      if (typeof wx !== 'undefined' && wx.login) {
        wx.login(loginOptions);
      } else {
        // 备用：使用uni.login
        uni.login({
          provider: 'weixin',
          ...loginOptions
        });
      }
      // #endif
      
      // #ifndef MP-WEIXIN
      const error = new Error('当前环境不支持微信登录');
      if (options.fail && typeof options.fail === 'function') {
        options.fail(error);
      }
      if (options.complete && typeof options.complete === 'function') {
        options.complete({ errMsg: 'login:fail 当前环境不支持微信登录' });
      }
      reject(error);
      // #endif
    });
  }

  /**
   * 简化的微信登录方法 - 只返回code
   * @returns {Promise<string>} 登录凭证code
   */
  static async getLoginCode() {
    try {
      const result = await this.login();
      return result.code;
    } catch (error) {
      throw error;
    }
  }

  /**
   * 分享到微信好友或群聊
   * @param {Object} shareData - 分享数据
   */
  static shareToWeChat(shareData = {}) {
    const defaultData = {
      title: '我的提醒',
      path: '/pages/index/index',
      imageUrl: '/static/images/share-logo.png'
    };
    
    const finalData = { ...defaultData, ...shareData };
    
    // #ifdef MP-WEIXIN
    return new Promise((resolve, reject) => {
      uni.showShareMenu({
        withShareTicket: true,
        success: () => {
          // 设置分享内容
          uni.onShareAppMessage(() => {
            return finalData;
          });
          resolve(finalData);
        },
        fail: reject
      });
    });
    // #endif
    
    // #ifndef MP-WEIXIN
    console.log('当前环境不支持微信分享');
    return Promise.resolve(finalData);
    // #endif
  }

  /**
   * 分享到朋友圈
   * @param {Object} shareData - 分享数据
   */
  static shareToTimeline(shareData = {}) {
    const defaultData = {
      title: '我的提醒 - 让生活更有序',
      imageUrl: '/static/images/share-logo.png'
    };
    
    const finalData = { ...defaultData, ...shareData };
    
    // #ifdef MP-WEIXIN
    return new Promise((resolve, reject) => {
      uni.showShareMenu({
        withShareTicket: true,
        success: () => {
          // 设置朋友圈分享内容
          uni.onShareTimeline(() => {
            return finalData;
          });
          resolve(finalData);
        },
        fail: reject
      });
    });
    // #endif
    
    // #ifndef MP-WEIXIN
    console.log('当前环境不支持朋友圈分享');
    return Promise.resolve(finalData);
    // #endif
  }

  /**
   * 发起支付
   * @param {Object} paymentData - 支付数据
   */
  static requestPayment(paymentData) {
    return new Promise((resolve, reject) => {
      // #ifdef MP-WEIXIN
      uni.requestPayment({
        ...paymentData,
        success: (res) => {
          console.log('支付成功:', res);
          resolve(res);
        },
        fail: (error) => {
          console.error('支付失败:', error);
          reject(error);
        }
      });
      // #endif
      
      // #ifndef MP-WEIXIN
      reject(new Error('当前环境不支持微信支付'));
      // #endif
    });
  }

  /**
   * 获取微信运动步数
   * @returns {Promise} 步数信息
   */
  static getWeRunData() {
    return new Promise((resolve, reject) => {
      // #ifdef MP-WEIXIN
      uni.getWeRunData({
        success: (res) => {
          console.log('获取微信运动数据成功:', res);
          resolve(res);
        },
        fail: (error) => {
          console.error('获取微信运动数据失败:', error);
          reject(error);
        }
      });
      // #endif
      
      // #ifndef MP-WEIXIN
      reject(new Error('当前环境不支持微信运动数据获取'));
      // #endif
    });
  }

  /**
   * 获取地理位置
   * @param {Object} options - 位置选项
   */
  static getLocation(options = {}) {
    const defaultOptions = {
      type: 'gcj02',
      altitude: false
    };
    
    const finalOptions = { ...defaultOptions, ...options };
    
    return new Promise((resolve, reject) => {
      uni.getLocation({
        ...finalOptions,
        success: (res) => {
          console.log('获取位置成功:', res);
          resolve(res);
        },
        fail: (error) => {
          console.error('获取位置失败:', error);
          reject(error);
        }
      });
    });
  }

  /**
   * 选择地址
   * @returns {Promise} 地址信息
   */
  static chooseAddress() {
    return new Promise((resolve, reject) => {
      // #ifdef MP-WEIXIN
      uni.chooseAddress({
        success: (res) => {
          console.log('选择地址成功:', res);
          resolve(res);
        },
        fail: (error) => {
          console.error('选择地址失败:', error);
          reject(error);
        }
      });
      // #endif
      
      // #ifndef MP-WEIXIN
      reject(new Error('当前环境不支持微信地址选择'));
      // #endif
    });
  }

  /**
   * 保存图片到相册
   * @param {string} filePath - 图片路径
   */
  static saveImageToPhotosAlbum(filePath) {
    return new Promise((resolve, reject) => {
      uni.saveImageToPhotosAlbum({
        filePath,
        success: (res) => {
          console.log('保存图片成功:', res);
          uni.showToast({
            title: '已保存到相册',
            icon: 'success'
          });
          resolve(res);
        },
        fail: (error) => {
          console.error('保存图片失败:', error);
          uni.showToast({
            title: '保存失败',
            icon: 'none'
          });
          reject(error);
        }
      });
    });
  }

  /**
   * 扫码功能
   * @param {Object} options - 扫码选项
   */
  static scanCode(options = {}) {
    const defaultOptions = {
      scanType: ['qrCode', 'barCode']
    };
    
    const finalOptions = { ...defaultOptions, ...options };
    
    return new Promise((resolve, reject) => {
      uni.scanCode({
        ...finalOptions,
        success: (res) => {
          console.log('扫码成功:', res);
          resolve(res);
        },
        fail: (error) => {
          console.error('扫码失败:', error);
          reject(error);
        }
      });
    });
  }

  /**
   * 震动反馈
   * @param {string} type - 震动类型 ('short' | 'long')
   */
  static vibrate(type = 'short') {
    // #ifdef MP-WEIXIN
    if (type === 'short') {
      uni.vibrateShort({
        type: 'medium'
      });
    } else {
      uni.vibrateLong();
    }
    // #endif
  }

  /**
   * 设置导航栏标题
   * @param {string} title - 标题
   */
  static setNavigationBarTitle(title) {
    uni.setNavigationBarTitle({
      title
    });
  }

  /**
   * 设置导航栏颜色
   * @param {Object} options - 颜色配置
   */
  static setNavigationBarColor(options = {}) {
    const defaultOptions = {
      frontColor: '#000000',
      backgroundColor: '#ffffff'
    };
    
    const finalOptions = { ...defaultOptions, ...options };
    
    // #ifdef MP-WEIXIN
    uni.setNavigationBarColor(finalOptions);
    // #endif
  }

  /**
   * 显示加载提示
   * @param {string} title - 提示文字
   */
  static showLoading(title = '加载中...') {
    uni.showLoading({
      title,
      mask: true
    });
  }

  /**
   * 隐藏加载提示
   */
  static hideLoading() {
    uni.hideLoading();
  }

  /**
   * 显示消息提示
   * @param {string} title - 提示文字
   * @param {string} icon - 图标类型
   * @param {number} duration - 显示时长
   */
  static showToast(title, icon = 'none', duration = 2000) {
    uni.showToast({
      title,
      icon,
      duration
    });
  }

  /**
   * 显示模态对话框
   * @param {Object} options - 对话框配置
   */
  static showModal(options = {}) {
    const defaultOptions = {
      title: '提示',
      content: '',
      showCancel: true,
      confirmText: '确定',
      cancelText: '取消'
    };
    
    const finalOptions = { ...defaultOptions, ...options };
    
    return new Promise((resolve, reject) => {
      uni.showModal({
        ...finalOptions,
        success: (res) => {
          resolve(res);
        },
        fail: (error) => {
          reject(error);
        }
      });
    });
  }

  /**
   * 获取系统信息
   * @returns {Promise} 系统信息
   */
  static getSystemInfo() {
    return new Promise((resolve, reject) => {
      uni.getSystemInfo({
        success: (res) => {
          console.log('系统信息:', res);
          resolve(res);
        },
        fail: (error) => {
          console.error('获取系统信息失败:', error);
          reject(error);
        }
      });
    });
  }

  /**
   * 检查是否是微信小程序环境
   * @returns {boolean} 是否为微信小程序
   */
  static isWeChatMiniProgram() {
    // #ifdef MP-WEIXIN
    return true;
    // #endif
    
    // #ifndef MP-WEIXIN
    return false;
    // #endif
  }

  /**
   * 获取小程序版本信息
   * @returns {Object} 版本信息
   */
  static getVersionInfo() {
    // #ifdef MP-WEIXIN
    const accountInfo = uni.getAccountInfoSync();
    return {
      miniProgram: accountInfo.miniProgram,
      plugin: accountInfo.plugin
    };
    // #endif
    
    // #ifndef MP-WEIXIN
    return {
      miniProgram: null,
      plugin: null
    };
    // #endif
  }

  /**
   * 更新用户信息到后台
   * @param {Object} userInfo - 用户信息
   * @returns {Promise} 更新结果
   */
  static async updateUserProfile(userInfo) {
    const API_URL = 'http://127.0.0.1:8080/api';
    
    try {
      console.log('📝 调用用户信息更新接口，数据:', userInfo);
      
      const token = uni.getStorageSync('accessToken');
      if (!token) {
        throw new Error('未找到访问令牌，请重新登录');
      }
      
      const updateData = {
        nickname: userInfo.nickName,
        avatarUrl: userInfo.avatarUrl
      };
      
      // 只有当性别存在时才添加
      if (userInfo.gender !== undefined) {
        updateData.gender = userInfo.gender;
      }
      
      console.log('📤 发送更新请求:', updateData);
      
      const response = await new Promise((resolve, reject) => {
        uni.request({
          url: `${API_URL}/auth/profile`,
          method: 'PUT',
          header: {
            'Content-Type': 'application/json',
            'Authorization': token
          },
          data: updateData,
          success: (res) => {
            console.log('📥 用户信息更新API响应:', res);
            
            if (res.statusCode >= 200 && res.statusCode < 300) {
              resolve(res.data);
            } else {
              reject({
                statusCode: res.statusCode,
                data: res.data,
                message: res.data?.message || `更新失败 (${res.statusCode})`
              });
            }
          },
          fail: (err) => {
            console.error('❌ 用户信息更新API请求失败:', err);
            reject({
              ...err,
              message: '网络连接失败，请检查网络'
            });
          }
        });
      });
      
      console.log('✅ 用户信息更新成功:', response);
      return response;
      
    } catch (error) {
      console.error('❌ 用户信息更新失败:', error);
      throw error;
    }
  }

  /**
   * 微信登录API调用 - 将js_code发送给后台
   * @param {Object} data - 登录数据
   * @param {string} data.code - 微信登录凭证
   * @param {Object} data.userInfo - 用户信息（可选）
   * @returns {Promise} 后台登录响应
   */
  static async wechatLogin(data) {
    const API_URL = 'http://127.0.0.1:8080/api';
    
    try {
      console.log('调用后台微信登录接口，数据:', data);
      
      const response = await new Promise((resolve, reject) => {
        uni.request({
          url: `${API_URL}/auth/wechat/login`,
          method: 'POST',
          header: {
            'Content-Type': 'application/json'
          },
          data: data,
          success: (res) => {
            console.log('微信登录API响应:', res);
            
            if (res.statusCode >= 200 && res.statusCode < 300) {
              resolve(res.data);
            } else {
              reject({
                statusCode: res.statusCode,
                data: res.data,
                message: res.data?.message || `请求失败 (${res.statusCode})`
              });
            }
          },
          fail: (err) => {
            console.error('微信登录API请求失败:', err);
            
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
            
            reject({
              ...err,
              message: errorMessage
            });
          }
        });
      });
      
      return response;
    } catch (error) {
      console.error('微信登录API调用失败:', error);
      throw error;
    }
  }

  /**
   * 智能微信登录流程 - 只有新用户才获取用户信息
   * @param {Object} options - 登录选项
   * @returns {Promise} 登录结果
   */
  static async smartWechatLogin(options = {}) {
    try {
      console.log('🚀 开始智能微信登录流程...');
      
      // 1. 先进行基础登录（不获取用户信息）
      const loginResult = await this.login();
      
      if (!loginResult || !loginResult.code) {
        throw new Error('未获取到微信登录凭证');
      }
      
      console.log('📱 获取到微信登录码，进行基础登录...');
      
      // 2. 发送基础登录请求
      const basicLoginData = {
        code: loginResult.code
        // 不包含 userInfo
      };
      
      const basicResponse = await this.wechatLogin(basicLoginData);
      
      console.log('🔍 基础登录完成，检查用户类型:', {
        isNewUser: basicResponse.isNewUser,
        userId: basicResponse.userId
      });
      
      // 3. 如果是新用户，获取用户信息并更新
      if (basicResponse.isNewUser) {
        console.log('🆕 检测到新用户，弹窗获取用户信息...');
        
        // 先保存token，以便后续API调用
        if (basicResponse.accessToken) {
          uni.setStorageSync('accessToken', `Bearer ${basicResponse.accessToken}`);
          console.log('🔑 已保存访问令牌，可以调用后续API');
        }
        
        try {
          // 显示提示
          const confirmResult = await new Promise((resolve) => {
            uni.showModal({
              title: '完善资料',
              content: '检测到您是首次登录，是否授权获取您的微信头像和昵称？',
              confirmText: '授权',
              cancelText: '跳过',
              success: (res) => {
                resolve(res.confirm);
              }
            });
          });
          
          if (confirmResult) {
            // 用户同意授权，获取用户信息
            console.log('👤 用户同意授权，获取用户信息...');
            
            const userInfo = await this.getUserProfile({
              desc: '用于完善您的用户资料'
            });
            
            console.log('✅ 获取用户信息成功:', userInfo);
            
            // 调用专门的更新接口，而不是重复登录
            try {
              const updateResult = await this.updateUserProfile(userInfo);
              console.log('🔄 用户信息更新完成:', updateResult);
              
              return {
                ...basicResponse,
                userInfoUpdated: true,
                message: '注册成功，资料已完善',
                // 更新本地响应数据
                nickname: updateResult.nickname || userInfo.nickName,
                avatarUrl: updateResult.avatarUrl || userInfo.avatarUrl
              };
            } catch (updateError) {
              console.warn('⚠️ 用户信息更新失败，但登录成功:', updateError);
              
              return {
                ...basicResponse,
                userInfoUpdated: false,
                message: '注册成功，但资料更新失败，可稍后在个人中心完善'
              };
            }
          } else {
            console.log('⏭️ 用户跳过用户信息获取');
            
            return {
              ...basicResponse,
              userInfoUpdated: false,
              message: '注册成功，可稍后在个人中心完善资料'
            };
          }
        } catch (userInfoError) {
          console.warn('⚠️ 获取用户信息失败，继续完成登录:', userInfoError);
          
          return {
            ...basicResponse,
            userInfoUpdated: false,
            message: '注册成功，获取用户信息失败，可稍后在个人中心完善资料'
          };
        }
      } else {
        // 老用户，直接返回登录结果
        console.log('👋 欢迎老用户回来！');
        
        return {
          ...basicResponse,
          userInfoUpdated: false,
          message: '登录成功，欢迎回来'
        };
      }
      
    } catch (error) {
      console.error('❌ 智能微信登录失败:', error);
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
    try {
      // 1. 获取微信登录code
      const loginResult = await this.login();
      
      if (!loginResult || !loginResult.code) {
        throw new Error('未获取到微信登录凭证');
      }
      
      // 2. 构建登录数据
      const loginData = {
        code: loginResult.code
      };
      
      // 3. 可选：获取用户信息
      if (options.withUserInfo) {
        try {
          const userInfo = await this.getUserProfile({
            desc: '用于完善用户资料'
          });
          loginData.userInfo = userInfo;
        } catch (userInfoError) {
          console.warn('获取用户信息失败，继续登录流程:', userInfoError);
          // 不阻断登录流程
        }
      }
      
      // 4. 调用后台登录接口
      const response = await this.wechatLogin(loginData);
      
      return response;
    } catch (error) {
      console.error('微信登录流程失败:', error);
      throw error;
    }
  }
}

// 导出工具类
export default WeChatUtils;

// 也可以按需导出具体方法
export const {
  getUserProfile,
  login,
  getLoginCode,
  wechatLogin,
  loginWithBackend,
  smartWechatLogin,
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
  updateUserProfile
} = WeChatUtils; 