/**
 * 系统信息工具函数
 * 使用新的微信小程序API替代已废弃的 wx.getSystemInfoSync
 * 
 * 废弃提示：wx.getSystemInfoSync is deprecated. 
 * Please use wx.getSystemSetting/wx.getAppAuthorizeSetting/wx.getDeviceInfo/wx.getWindowInfo/wx.getAppBaseInfo instead.
 */

/**
 * 获取完整的系统信息
 * 使用新的推荐API组合，替代 wx.getSystemInfoSync
 * @returns {Promise<Object>} 完整的系统信息
 */
export async function getSystemInfo() {
  try {
    // #ifdef MP-WEIXIN
    if (typeof wx !== 'undefined') {
      console.log('🔍 使用新API获取系统信息...');
      
      // 并行获取各种信息
      const [systemSetting, deviceInfo, windowInfo, appBaseInfo, appAuthSetting] = await Promise.allSettled([
        getSystemSetting(),
        getDeviceInfo(), 
        getWindowInfo(),
        getAppBaseInfo(),
        getAppAuthorizeSetting()
      ]);

      // 提取成功的结果
      const extractValue = (result) => result.status === 'fulfilled' ? result.value : {};
      
      const combinedInfo = {
        // 系统设置
        ...extractValue(systemSetting),
        
        // 设备信息
        ...extractValue(deviceInfo),
        
        // 窗口信息  
        ...extractValue(windowInfo),
        
        // 应用信息
        ...extractValue(appBaseInfo),
        
        // 授权信息
        authSetting: extractValue(appAuthSetting),
        
        // 兼容性字段
        platform: extractValue(deviceInfo).platform || extractValue(systemSetting).platform || 'unknown',
        system: extractValue(deviceInfo).system || 'unknown',
        model: extractValue(deviceInfo).model || 'unknown',
        brand: extractValue(deviceInfo).brand || 'unknown',
        screenWidth: extractValue(windowInfo).screenWidth || 0,
        screenHeight: extractValue(windowInfo).screenHeight || 0,
        windowWidth: extractValue(windowInfo).windowWidth || 0,
        windowHeight: extractValue(windowInfo).windowHeight || 0,
        pixelRatio: extractValue(windowInfo).pixelRatio || 1,
        language: extractValue(appBaseInfo).language || extractValue(systemSetting).language || 'zh_CN',
        version: extractValue(appBaseInfo).version || 'unknown',
        
        // API版本标记
        _apiVersion: 'new',
        _timestamp: Date.now()
      };

      console.log('✅ 新API获取系统信息成功');
      return combinedInfo;
    }
    // #endif
    
    // 非微信环境降级
    console.warn('⚠️ 非微信环境，降级使用 uni.getSystemInfo');
    return await fallbackGetSystemInfo();
    
  } catch (error) {
    console.error('❌ 获取系统信息失败，降级处理:', error);
    return await fallbackGetSystemInfo();
  }
}

/**
 * 获取系统设置信息
 * 替代 wx.getSystemInfoSync 中的系统设置部分
 */
export function getSystemSetting() {
  return new Promise((resolve) => {
    try {
      // #ifdef MP-WEIXIN
      if (typeof wx !== 'undefined' && wx.getSystemSetting) {
        const setting = wx.getSystemSetting();
        console.log('✅ 获取系统设置成功');
        resolve(setting);
      } else {
        console.warn('⚠️ getSystemSetting API不可用');
        resolve({});
      }
      // #endif
      // #ifndef MP-WEIXIN
      resolve({});
      // #endif
    } catch (error) {
      console.error('❌ 获取系统设置失败:', error);
      resolve({});
    }
  });
}

/**
 * 获取设备信息
 * 替代 wx.getSystemInfoSync 中的设备信息部分
 */
export function getDeviceInfo() {
  return new Promise((resolve) => {
    try {
      // #ifdef MP-WEIXIN
      if (typeof wx !== 'undefined' && wx.getDeviceInfo) {
        const device = wx.getDeviceInfo();
        console.log('✅ 获取设备信息成功');
        resolve(device);
      } else {
        console.warn('⚠️ getDeviceInfo API不可用');
        resolve({});
      }
      // #endif
      // #ifndef MP-WEIXIN
      resolve({});
      // #endif
    } catch (error) {
      console.error('❌ 获取设备信息失败:', error);
      resolve({});
    }
  });
}

/**
 * 获取窗口信息
 * 替代 wx.getSystemInfoSync 中的窗口信息部分
 */
export function getWindowInfo() {
  return new Promise((resolve) => {
    try {
      // #ifdef MP-WEIXIN
      if (typeof wx !== 'undefined' && wx.getWindowInfo) {
        const window = wx.getWindowInfo();
        console.log('✅ 获取窗口信息成功');
        resolve(window);
      } else {
        console.warn('⚠️ getWindowInfo API不可用');
        resolve({});
      }
      // #endif
      // #ifndef MP-WEIXIN
      resolve({});
      // #endif
    } catch (error) {
      console.error('❌ 获取窗口信息失败:', error);
      resolve({});
    }
  });
}

/**
 * 获取应用基础信息
 * 替代 wx.getSystemInfoSync 中的应用信息部分
 */
export function getAppBaseInfo() {
  return new Promise((resolve) => {
    try {
      // #ifdef MP-WEIXIN
      if (typeof wx !== 'undefined' && wx.getAppBaseInfo) {
        const app = wx.getAppBaseInfo();
        console.log('✅ 获取应用信息成功');
        resolve(app);
      } else {
        console.warn('⚠️ getAppBaseInfo API不可用');
        resolve({});
      }
      // #endif
      // #ifndef MP-WEIXIN
      resolve({});
      // #endif
    } catch (error) {
      console.error('❌ 获取应用信息失败:', error);
      resolve({});
    }
  });
}

/**
 * 获取应用授权设置
 * 新增的授权信息获取
 */
export function getAppAuthorizeSetting() {
  return new Promise((resolve) => {
    try {
      // #ifdef MP-WEIXIN
      if (typeof wx !== 'undefined' && wx.getAppAuthorizeSetting) {
        const auth = wx.getAppAuthorizeSetting();
        console.log('✅ 获取授权设置成功');
        resolve(auth);
      } else {
        console.warn('⚠️ getAppAuthorizeSetting API不可用');
        resolve({});
      }
      // #endif
      // #ifndef MP-WEIXIN
      resolve({});
      // #endif
    } catch (error) {
      console.error('❌ 获取授权设置失败:', error);
      resolve({});
    }
  });
}

/**
 * 降级方案：使用旧的 uni.getSystemInfo
 */
function fallbackGetSystemInfo() {
  return new Promise((resolve, reject) => {
    uni.getSystemInfo({
      success: (info) => {
        console.log('⚠️ 使用降级方案 uni.getSystemInfo');
        resolve({
          ...info,
          _apiVersion: 'fallback',
          _timestamp: Date.now()
        });
      },
      fail: (error) => {
        console.error('❌ 降级方案也失败了:', error);
        reject(error);
      }
    });
  });
}

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
 * 获取环境信息摘要（常用字段）
 * @returns {Promise<Object>} 包含常用字段的环境信息
 */
export async function getEnvironmentSummary() {
  try {
    const systemInfo = await getSystemInfo();
    
    return {
      platform: systemInfo.platform || 'unknown',
      system: systemInfo.system || 'unknown', 
      model: systemInfo.model || 'unknown',
      brand: systemInfo.brand || 'unknown',
      language: systemInfo.language || 'zh_CN',
      version: systemInfo.version || 'unknown',
      screenWidth: systemInfo.screenWidth || 0,
      screenHeight: systemInfo.screenHeight || 0,
      pixelRatio: systemInfo.pixelRatio || 1,
      isWeChatMP: isWeChatMiniProgram(),
      apiVersion: systemInfo._apiVersion || 'unknown'
    };
  } catch (error) {
    console.error('获取环境摘要失败:', error);
    return {
      platform: 'unknown',
      system: 'unknown',
      model: 'unknown', 
      brand: 'unknown',
      language: 'zh_CN',
      version: 'unknown',
      screenWidth: 0,
      screenHeight: 0,
      pixelRatio: 1,
      isWeChatMP: isWeChatMiniProgram(),
      apiVersion: 'error'
    };
  }
}
