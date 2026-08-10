/**
 * File upload API. Files are sent to Reminder over standard HTTPS and are
 * persisted by the backend through the audited saas-admin storage gateway.
 */

import { buildApiUrl } from './http.js';
import { updateProfile } from './auth.js';

function authorizationHeader() {
    const token = uni.getStorageSync('accessToken');
    if (!token) {
        throw new Error('用户未登录，无法上传文件');
    }
    return token.startsWith('Bearer ') ? token : `Bearer ${token}`;
}

function parseResponseData(data) {
    if (typeof data !== 'string') {
        return data || {};
    }
    try {
        return JSON.parse(data);
    } catch (error) {
        throw new Error('文件服务返回了无法解析的数据');
    }
}

function uploadToBackend(filePath) {
    return new Promise((resolve, reject) => {
        uni.uploadFile({
            url: buildApiUrl('/files/upload'),
            filePath,
            name: 'file',
            header: {
                Authorization: authorizationHeader()
            },
            success: (res) => {
                let data;
                try {
                    data = parseResponseData(res.data);
                } catch (error) {
                    reject(error);
                    return;
                }

                if (res.statusCode >= 200 && res.statusCode < 300 && data.url) {
                    resolve(data);
                    return;
                }
                reject(new Error(data.error || data.message || `文件上传失败: ${res.statusCode}`));
            },
            fail: (err) => {
                reject(new Error(`网络请求失败: ${err.errMsg || 'unknown error'}`));
            }
        });
    });
}

/**
 * Upload an avatar and persist its HTTPS URL in the user profile.
 * The unused compatibility parameters are retained for existing callers.
 */
export async function uploadAvatarWithFile(userId, tempFilePath, oldAvatarUrl = null) {
    try {
        if (!userId) {
            throw new Error('无法获取用户ID');
        }
        if (!tempFilePath) {
            throw new Error('请选择要上传的头像');
        }

        const uploaded = await uploadToBackend(tempFilePath);
        await updateProfile({ avatarUrl: uploaded.url });
        return {
            success: true,
            avatarUrl: uploaded.url,
            objectName: uploaded.objectName
        };
    } catch (error) {
        console.error('头像上传处理失败:', error);
        return {
            success: false,
            error: error.message || error.errMsg || String(error)
        };
    }
}

/**
 * Upload a general file and return the stable HTTPS URL.
 */
export async function uploadFile(filePath) {
    try {
        const uploaded = await uploadToBackend(filePath);
        return {
            success: true,
            url: uploaded.url,
            objectName: uploaded.objectName
        };
    } catch (error) {
        console.error('文件上传失败:', error);
        return { success: false, error: error.message || String(error) };
    }
}

export default {
    uploadAvatarWithFile,
    uploadFile
};
