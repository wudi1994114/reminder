/**
 * Standard HTTP request layer shared by H5 and mini-program builds.
 */

import { getApiBaseUrl, TIMEOUT } from '../config/api.js';

const API_URL = getApiBaseUrl();

export function buildApiUrl(url = '') {
    if (/^https?:\/\//i.test(url)) {
        return url;
    }

    let path = String(url).replace(/^\/+/, '');
    if (/\/api$/i.test(API_URL) && /^api(?:\/|$)/i.test(path)) {
        path = path.replace(/^api\/?/i, '');
    }
    return path ? `${API_URL}/${path}` : API_URL;
}

function authorizationHeader() {
    const token = uni.getStorageSync('accessToken');
    if (!token) {
        return {};
    }
    return {
        Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}`
    };
}

function isReminderListRequest(url = '') {
    return String(url).includes('/reminders/simple');
}

/**
 * Promise wrapper around uni.request. All traffic uses the configured API URL.
 */
export const request = (options = {}) => new Promise((resolve, reject) => {
    const requestUrl = buildApiUrl(options.url);
    const requestOptions = {
        ...options,
        url: requestUrl,
        timeout: options.timeout || TIMEOUT,
        header: {
            'Content-Type': 'application/json',
            ...authorizationHeader(),
            ...options.header
        },
        success: (res) => {
            if (res.statusCode >= 200 && res.statusCode < 300) {
                resolve(res.data);
                return;
            }

            if ((res.statusCode === 401 || res.statusCode === 403)
                    && isReminderListRequest(options.url)) {
                resolve([]);
                return;
            }
            reject(res);
        },
        fail: (err) => {
            if (isReminderListRequest(options.url)) {
                resolve([]);
                return;
            }

            let errorMessage = '网络连接失败';
            if (err.errMsg?.includes('ERR_CONNECTION_REFUSED')) {
                errorMessage = '服务器连接被拒绝，请检查网络或联系管理员';
            } else if (err.errMsg?.includes('timeout')) {
                errorMessage = '请求超时，请检查网络连接';
            } else if (err.errMsg?.includes('fail')) {
                errorMessage = '网络请求失败，请稍后重试';
            }

            uni.showToast({
                title: errorMessage,
                icon: 'none',
                duration: 3000
            });
            reject({ ...err, message: errorMessage });
        }
    };

    uni.request(requestOptions);
});

export const HTTP_CONFIG = {
    baseURL: API_URL,
    timeout: TIMEOUT,
    headers: {
        'Content-Type': 'application/json'
    }
};

export default {
    request,
    buildApiUrl,
    HTTP_CONFIG
};
