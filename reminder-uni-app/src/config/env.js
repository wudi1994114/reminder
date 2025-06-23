// 环境配置
const env = process.env.NODE_ENV || 'development';

// 不同环境的配置
const configs = {
    development: {
        API_BASE_URL: 'http://localhost:8080/api',
        DEBUG: true,
        LOG_LEVEL: 'debug'
    },
    production: {
        API_BASE_URL: 'https://api.reminder.com/api',
        DEBUG: false,
        LOG_LEVEL: 'error'
    },
    test: {
        API_BASE_URL: 'http://test-api.reminder.com/api',
        DEBUG: true,
        LOG_LEVEL: 'warn'
    }
};

// 导出当前环境配置
export default {
    env,
    ...configs[env],
    // 通用配置
    APP_NAME: '备忘鸡',
    VERSION: '1.0.0',
    STORAGE_PREFIX: 'reminder_',
    REQUEST_TIMEOUT: 10000,
    MAX_FILE_SIZE: 5 * 1024 * 1024, // 5MB
    SUPPORTED_IMAGE_TYPES: ['jpg', 'jpeg', 'png', 'gif'],
    DATE_FORMAT: 'YYYY-MM-DD HH:mm:ss',
    PAGINATION_SIZE: 20
}; 