/**
 * 版本控制配置
 * 用于控制不同版本下功能的显示与隐藏
 */

/**
 * 获取微信小程序版本信息
 * @returns {Object} 版本信息对象
 */
export const getVersionInfo = () => {
  try {
    // #ifdef MP-WEIXIN
    if (typeof wx !== 'undefined' && wx.getAccountInfoSync) {
      const accountInfo = wx.getAccountInfoSync();
      console.log('accountInfo', accountInfo);
      return {
        version: accountInfo.miniProgram.version,
        envVersion: accountInfo.miniProgram.envVersion // develop-开发版 trial-体验版 release-正式版
      };
      // return {
      //   version: '1.0.0',
      //   envVersion: 'trial' // 默认为正式版
      // };
      // return {
      //   version: '1.0.0',
      //   envVersion: 'release' // 默认为正式版
      // };
    }
    // #endif
    
    // 非微信小程序环境返回默认值
    return {
      version: '1.0.0',
      envVersion: 'release' // 默认为正式版
    };
  } catch (error) {
    console.error('获取版本信息失败:', error);
    return {
      version: '1.0.0',
      envVersion: 'release' // 默认为正式版
    };
  }
};

/**
 * 检查是否为开发版本
 * @returns {boolean} 是否为开发版本
 */
export const isDevelopmentVersion = () => {
  const versionInfo = getVersionInfo();
  return versionInfo.envVersion === 'develop';
};

/**
 * 检查是否为线上版本（正式版）
 * @returns {boolean} 是否为线上版本
 */
export const isProductionVersion = () => {
  const versionInfo = getVersionInfo();
  return versionInfo.envVersion === 'release';
};

/**
 * 检查是否为体验版
 * @returns {boolean} 是否为体验版
 */
export const isTrialVersion = () => {
  const versionInfo = getVersionInfo();
  return versionInfo.envVersion === 'trial';
};

/**
 * 功能控制配置
 */
export const FeatureControl = {
  /**
   * 是否显示手机号相关功能
   * 只有正式版显示
   */
  showPhoneFeatures: () => {
    return isProductionVersion();
  },

  /**
   * 是否显示邮箱相关功能
   * 只有正式版显示
   */
  showEmailFeatures: () => {
    return isProductionVersion();
  },

  /**
   * 是否可以编辑手机号
   * 只有正式版可编辑
   */
  canEditPhone: () => {
    return isProductionVersion();
  },

  /**
   * 是否可以编辑邮箱
   * 只有正式版可编辑
   */
  canEditEmail: () => {
    return isProductionVersion();
  },

  /**
   * 获取功能限制提示文本
   * @param {string} feature - 功能名称 'phone' | 'email'
   * @returns {string} 提示文本
   */
  getFeatureRestrictMessage: (feature) => {
    if (isDevelopmentVersion()) {
      const featureMap = {
        phone: '手机号',
        email: '邮箱'
      };
      return `尚不支持修改${featureMap[feature] || '此功能'}`;
    }
    if (isTrialVersion() && feature === 'email') {
      return '体验版不支持邮箱功能';
    }
    return '';
  }
};

/**
 * 版本标识
 */
export const VersionLabels = {
  develop: '开发版',
  trial: '体验版',
  release: '正式版'
};

/**
 * 获取当前版本标签
 * @returns {string} 版本标签
 */
export const getCurrentVersionLabel = () => {
  const versionInfo = getVersionInfo();
  return VersionLabels[versionInfo.envVersion] || '未知版本';
};

// 默认导出
export default {
  getVersionInfo,
  isDevelopmentVersion,
  isProductionVersion,
  isTrialVersion,
  FeatureControl,
  VersionLabels,
  getCurrentVersionLabel
}; 