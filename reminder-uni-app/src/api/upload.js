/**
 * 文件上传API
 * 负责：头像上传、文件上传等
 */

import cloudConfig from '../config/cloud.js';
import { getApiBaseUrl } from '../config/api.js';
import { request } from './http.js';
import { updateProfile } from './auth.js';

const API_URL = getApiBaseUrl();

/**
 * 上传头像到云存储并更新到后端
 * @param {string} userId - 用户ID
 * @param {string} tempFilePath - 已选择的临时文件路径
 * @param {string} oldAvatarUrl - 旧头像URL（用于删除）
 * @returns {Promise<{success: boolean, avatarUrl?: string, error?: string}>}
 */
export async function uploadAvatarWithFile(userId, tempFilePath, oldAvatarUrl = null) {
    // #ifdef MP-WEIXIN
    console.log('🔄 开始处理头像上传，文件路径:', tempFilePath);
    try {
        const isCloudEnabled = cloudConfig.enabled && typeof wx !== 'undefined' && wx.cloud;
        let newAvatarUrl;

        if (isCloudEnabled) {
            // 云托管上传逻辑
            console.log('☁️ 使用云托管上传...');
            const extension = tempFilePath.substring(tempFilePath.lastIndexOf('.'));
            const cloudPath = `mp_avatar/${userId}_${Date.now()}${extension}`;
            
            console.log('☁️ 上传到云路径:', cloudPath);

            const uploadRes = await wx.cloud.uploadFile({
                cloudPath: cloudPath,
                filePath: tempFilePath,
            });

            newAvatarUrl = uploadRes.fileID;
            console.log('✅ 云托管上传成功，FileID:', newAvatarUrl);

        } else {
            // 传统后端上传逻辑
            console.log('🌐 使用传统HTTP上传...');
            const uploadUrl = `${API_URL}/files/upload`;
            
            const token = uni.getStorageSync('accessToken');
            if (!token) {
                throw new Error("用户未登录，无法上传文件");
            }

            const uploadRes = await new Promise((resolve, reject) => {
                uni.uploadFile({
                    url: uploadUrl,
                    filePath: tempFilePath,
                    name: 'file',
                    header: {
                        'Authorization': `Bearer ${token}`
                    },
                    success: (res) => {
                        if (res.statusCode === 200) {
                            resolve(JSON.parse(res.data));
                        } else {
                            reject(new Error(`文件上传失败: ${res.statusCode}`));
                        }
                    },
                    fail: (err) => {
                        reject(new Error(`网络请求失败: ${err.errMsg}`));
                    }
                });
            });
            
            newAvatarUrl = uploadRes.url;
            console.log('✅ 后端上传成功，URL:', newAvatarUrl);
        }

        // 将新的 URL/FileID 更新到后端
        console.log('🔄 更新用户资料...');
        await updateProfile({ avatarUrl: newAvatarUrl });
        console.log('✅ 后端用户资料更新成功');

        // 后台静默删除旧头像
        deleteOldAvatarAsync(oldAvatarUrl);

        return { success: true, avatarUrl: newAvatarUrl };

    } catch (error) {
        console.error('❌ 头像上传处理失败:', error);
        
        let errorMessage = '未知错误';
        if (error.message) {
            errorMessage = error.message;
        } else if (error.errMsg) {
            errorMessage = error.errMsg;
        } else if (typeof error === 'string') {
            errorMessage = error;
        }
        
        return { success: false, error: errorMessage };
    }
    // #endif

    // #ifndef MP-WEIXIN
    console.warn('⚠️ uploadAvatarWithFile 功能仅在微信小程序中受支持');
    return Promise.resolve({ success: false, error: '当前环境不支持上传头像' });
    // #endif
}

/**
 * 删除云存储文件
 * @param {string} fileID - 云文件ID
 * @returns {Promise<boolean>} 删除是否成功
 */
export async function deleteCloudFile(fileID) {
    // #ifdef MP-WEIXIN
    if (!fileID || !fileID.startsWith('cloud://')) {
        console.log('⚠️ 无效的云文件ID，跳过删除:', fileID);
        return false;
    }

    try {
        console.log('🗑️ 开始删除云文件:', fileID);
        const res = await wx.cloud.deleteFile({
            fileList: [fileID]
        });
        
        if (res.fileList && res.fileList.length > 0) {
            const deleteResult = res.fileList[0];
            if (deleteResult.status === 0) {
                console.log('✅ 云文件删除成功:', fileID);
                return true;
            } else {
                console.error('❌ 云文件删除失败:', deleteResult.errMsg);
                return false;
            }
        }
        return false;
    } catch (error) {
        console.error('❌ 删除云文件异常:', error);
        return false;
    }
    // #endif
    
    // #ifndef MP-WEIXIN
    console.log('⚠️ 非微信环境，无法删除云文件');
    return false;
    // #endif
}

/**
 * 异步删除旧头像（后台静默执行，不影响主流程）
 * @param {string} oldAvatarUrl - 旧头像URL
 */
export function deleteOldAvatarAsync(oldAvatarUrl) {
    if (!oldAvatarUrl) return;
    
    setTimeout(() => {
        try {
            if (oldAvatarUrl.startsWith('cloud://') && 
                !oldAvatarUrl.includes('thirdwx.qlogo.cn')) {
                console.log('🗑️ 后台静默删除旧头像:', oldAvatarUrl);
                deleteCloudFile(oldAvatarUrl).catch(err => {
                    console.warn('⚠️ 后台删除旧头像失败（不影响任何流程）:', err);
                });
            } else {
                console.log('ℹ️ 旧头像不是云文件或为默认头像，跳过删除:', oldAvatarUrl);
            }
        } catch (error) {
            console.warn('⚠️ 删除旧头像过程中发生错误（不影响任何流程）:', error);
        }
    }, 100);
}

/**
 * 通用文件上传
 * @param {string} filePath - 文件路径
 * @param {string} fileName - 文件名
 * @returns {Promise<{success: boolean, url?: string, error?: string}>}
 */
export async function uploadFile(filePath, fileName = 'file') {
    try {
        const token = uni.getStorageSync('accessToken');
        if (!token) {
            throw new Error("用户未登录，无法上传文件");
        }

        const uploadUrl = `${API_URL}/files/upload`;
        
        const result = await new Promise((resolve, reject) => {
            uni.uploadFile({
                url: uploadUrl,
                filePath: filePath,
                name: fileName,
                header: {
                    'Authorization': `Bearer ${token}`
                },
                success: (res) => {
                    if (res.statusCode === 200) {
                        const data = JSON.parse(res.data);
                        resolve({ success: true, url: data.url });
                    } else {
                        reject(new Error(`文件上传失败: ${res.statusCode}`));
                    }
                },
                fail: (err) => {
                    reject(new Error(`网络请求失败: ${err.errMsg}`));
                }
            });
        });
        
        return result;
    } catch (error) {
        console.error('文件上传失败:', error);
        return { success: false, error: error.message };
    }
}

export default {
    uploadAvatarWithFile,
    uploadFile,
    deleteCloudFile,
    deleteOldAvatarAsync
};

