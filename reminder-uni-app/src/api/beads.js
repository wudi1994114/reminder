import { buildApiUrl } from './http.js';

/** Upload only the selected original. All pattern generation stays on device. */
export function uploadBeadOriginal(filePath) {
  const token = uni.getStorageSync('accessToken');
  if (!token) return Promise.resolve({ success: false, error: '登录后可备份原图，本地制作不受影响' });
  return new Promise((resolve) => {
    uni.uploadFile({
      url: buildApiUrl('/files/bead-original'), filePath, name: 'file', timeout: 60000,
      header: { Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}` },
      success(res) {
        try {
          const body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
          if (res.statusCode >= 200 && res.statusCode < 300 && body.imageId && body.objectName && body.url) {
            resolve({ success: true, ...body });
          } else {
            resolve({ success: false, error: res.statusCode === 401 ? '登录已过期，请重新登录后备份' :
              (body?.error || body?.message || '原图备份失败，请重试') });
          }
        } catch (_) { resolve({ success: false, error: '原图备份响应异常，请重试' }); }
      },
      fail() { resolve({ success: false, error: '网络不可用，原图尚未备份' }); }
    });
  });
}
