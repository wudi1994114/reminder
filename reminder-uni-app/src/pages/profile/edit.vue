<template>
  <view class="container">
    <view class="form-card">
      <view class="form-item avatar-section">
        <text class="label">头像</text>
        <view class="avatar-preview" @click="chooseAvatar">
          <image class="avatar-image" :src="avatarPreview || userState.user?.avatarUrl || '/static/images/avatar.png'" mode="aspectFill"></image>
          <text class="change-avatar-text">点击更换头像</text>
        </view>
      </view>

      <view class="form-item">
        <text class="label">昵称</text>
        <input class="input" v-model="form.nickname" placeholder="请输入昵称" />
      </view>

      <view class="form-item">
        <text class="label">邮箱</text>
        <input class="input disabled-input" :value="userState.user?.email || ''" placeholder="邮箱不可修改" disabled />
      </view>
      
      <view class="form-item" v-if="userState.user?.phone">
        <text class="label">手机号</text>
        <input class="input disabled-input" :value="userState.user?.phone || ''" placeholder="手机号不可修改" disabled />
      </view>

      <button class="btn btn-primary save-btn" @click="saveProfile" :disabled="isSubmitting" :loading="isSubmitting">
        {{ isSubmitting ? '保存中...' : '保存更改' }}
      </button>
    </view>
  </view>
</template>

<script>
import { ref, reactive, onMounted } from 'vue';
import { userState, saveUserInfo } from '../../services/store';
import { updateProfile } from '../../services/api';

export default {
  setup() {
    const form = reactive({
      nickname: ''
    });
    const avatarPreview = ref('');
    let avatarFile = null; // Store the chosen file object
    const isSubmitting = ref(false);

    onMounted(() => {
      if (userState.user) {
        form.nickname = userState.user.nickname || userState.user.username || '';
      }
    });

    const chooseAvatar = () => {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          avatarPreview.value = res.tempFilePaths[0];
          avatarFile = { path: res.tempFilePaths[0], name: res.tempFiles[0].name }; // 存储路径和文件名
        },
        fail: (err) => {
          console.log('选择图片失败:', err);
          uni.showToast({ title: '选择图片失败', icon: 'none' });
        }
      });
    };

    const uploadAvatar = async (filePath, fileName) => {
      try {
        return new Promise((resolve, reject) => {
          uni.uploadFile({
            url: 'YOUR_BACKEND_UPLOAD_URL', // <--- 再次确保这是你的后端上传接口地址
            filePath: filePath,
            name: 'file', // 后端接收文件的 key
            fileName: fileName, // uni-app 中建议指定 fileName，有些平台可能需要
            header: {
              // 'Authorization': 'Bearer ' + uni.getStorageSync('token'), // 如果需要认证
              // 'Content-Type': 'multipart/form-data' // 通常会自动设置，但可以显式指定
            },
            // formData: {
            // 'customParam': 'value'
            // },
            success: (uploadFileRes) => {
              console.log('uni.uploadFile success response:', uploadFileRes);
              if (uploadFileRes.statusCode === 200) {
                try {
                  const responseData = JSON.parse(uploadFileRes.data);
                  if (responseData && responseData.avatarUrl) { // 直接检查 avatarUrl
                    resolve(responseData.avatarUrl);
                  } else if (responseData && responseData.data && responseData.data.avatarUrl) { // 检查嵌套的 avatarUrl
                     resolve(responseData.data.avatarUrl);
                  } else {
                    console.error('Avatar upload success, but avatarUrl not found in response:', responseData);
                    uni.showToast({ title: '头像上传成功，但未找到URL', icon: 'none' });
                    reject(new Error('Avatar URL not found in response'));
                  }
                } catch (e) {
                  console.error('Error parsing upload response:', e, uploadFileRes.data);
                  uni.showToast({ title: '解析上传响应失败', icon: 'none' });
                  reject(new Error('Error parsing upload response'));
                }
              } else {
                console.error('Avatar upload failed with status code:', uploadFileRes.statusCode, uploadFileRes.data);
                uni.showToast({ title: `头像上传失败: ${uploadFileRes.statusCode}`, icon: 'none' });
                reject(new Error(`Upload failed with status ${uploadFileRes.statusCode}`));
              }
            },
            fail: (err) => {
              console.error('uni.uploadFile fail:', err);
              uni.showToast({ title: '头像上传请求失败', icon: 'none' });
              reject(err);
            }
          });
        });

      } catch (error) {
        console.error('uploadAvatar 函数出错:', error);
        uni.showToast({ title: '头像上传功能异常', icon: 'none' });
        return null; // 或 reject(error) 如果上层调用 await
      }
    };

    const saveProfile = async () => {
      if (!form.nickname.trim()) {
        uni.showToast({ title: '昵称不能为空', icon: 'none' });
        return;
      }

      isSubmitting.value = true;
      try {
        let avatarUrlToUpdate = userState.user?.avatarUrl;

        if (avatarFile && avatarFile.path) {
          uni.showLoading({ title: '头像上传中...' });
          try {
            const uploadedAvatarUrl = await uploadAvatar(avatarFile.path, avatarFile.name);
            uni.hideLoading();
            if (uploadedAvatarUrl) {
              avatarUrlToUpdate = uploadedAvatarUrl;
            } else {
              uni.showToast({ title: '头像上传失败，请重试', icon: 'none' });
              isSubmitting.value = false;
              return; // 终止保存
            }
          } catch (uploadError) {
            uni.hideLoading();
            console.error("头像上传时发生错误: ", uploadError);
            uni.showToast({ title: '头像上传出错，请重试', icon: 'none' });
            isSubmitting.value = false;
            return; // 终止保存
          }
        }

        const profileData = {
          nickname: form.nickname,
          avatarUrl: avatarUrlToUpdate,
          email: userState.user?.email 
        };

        const updatedUserResponse = await updateProfile(profileData);
        
        if (updatedUserResponse) {
            const userToSave = {
                ...userState.user, // 保留旧信息如 id, email 等
                nickname: updatedUserResponse.nickname || profileData.nickname, // 优先使用后端返回的
                avatarUrl: updatedUserResponse.avatarUrl || profileData.avatarUrl, // 优先使用后端返回的
             };
            saveUserInfo(userToSave);

            uni.showToast({ title: '资料更新成功', icon: 'success' });
            setTimeout(() => {
            uni.navigateBack();
          }, 1500);
        } else {
          // uni.showToast({ title: '更新失败，请重试', icon: 'none' }); // handleApiError 应该会处理
        }
      } catch (error) {
        console.error('保存资料失败:', error);
        uni.showToast({ title: '保存失败，请稍后重试', icon: 'none' });
      } finally {
        isSubmitting.value = false;
      }
    };

    return {
      userState,
      form,
      avatarPreview,
      isSubmitting,
      chooseAvatar,
      saveProfile
    };
  }
};
</script>

<style scoped>
.container {
  padding: 30rpx;
}

.form-card {
  background-color: #ffffff;
  border-radius: 10rpx;
  padding: 30rpx;
  box-shadow: 0 2rpx 20rpx rgba(0, 0, 0, 0.1);
}

.form-item {
  margin-bottom: 40rpx;
}

.label {
  display: block;
  font-size: 28rpx;
  color: #666666;
  margin-bottom: 15rpx;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center; /* Center avatar content */
}

.avatar-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
}

.avatar-image {
  width: 180rpx;
  height: 180rpx;
  border-radius: 50%;
  background-color: #f0f0f0;
  margin-bottom: 15rpx;
  border: 2rpx solid #eee;
}

.change-avatar-text {
  font-size: 26rpx;
  color: #3cc51f;
}

.input {
  width: 100%;
  height: 88rpx; /* Standard height */
  background-color: #f8f8f8;
  border-radius: 8rpx;
  padding: 0 25rpx;
  font-size: 30rpx;
  color: #333;
}

.disabled-input {
  background-color: #f0f0f0;
  color: #999999;
}

.save-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  margin-top: 20rpx; /* Add some space before button */
}
</style>
