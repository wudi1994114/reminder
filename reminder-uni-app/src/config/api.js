/**
 * API相关配置
 */

// API基础URL配置
export const API_CONFIG = {
    // 开发环境
    development: {
        BASE_URL: 'http://127.0.0.1:8080/api',
        // BASE_URL: 'http://192.168.100.174:8080/api',
    },
    // 生产环境  
    production: {
        BASE_URL: 'https://reminder-api.wwmty.com/api',
    },
    // 测试环境
    test: {
        BASE_URL: 'http://127.0.0.1:8080/api',
    }
};

// 获取当前环境的API URL
export function getApiBaseUrl() {
    const env = import.meta.env.MODE || 'development';
    const configuredUrl = import.meta.env.VITE_API_BASE_URL;
    const baseUrl = configuredUrl || API_CONFIG[env]?.BASE_URL || API_CONFIG.development.BASE_URL;
    return baseUrl.replace(/\/+$/, '');
}

// 请求超时配置
export const TIMEOUT = 10000;

// 请求重试配置
export const RETRY_CONFIG = {
    maxRetries: 3,
    retryDelay: 1000,
};

export default {
    API_CONFIG,
    getApiBaseUrl,
    TIMEOUT,
    RETRY_CONFIG
};
