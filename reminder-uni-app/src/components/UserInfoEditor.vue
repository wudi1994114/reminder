<template>
  <view class="user-info-editor">    
    <view class="editor-content">
      <view class="avatar-section">
        <text class="label">头像</text>
        <button class="avatar-button" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
          <image 
            class="avatar-image" 
            :src="displayAvatarUrl"
            mode="aspectFill"
          ></image>
          <view class="avatar-overlay">
            <text class="avatar-text">点击更换</text>
          </view>
        </button>
      </view>
      
      <view v-if="promptMessage" class="info-prompt-under-avatar">
        <text>{{ promptMessage }}</text>
      </view>
      
      <view class="nickname-section">
        <text class="label">昵称 <text class="required">*</text></text>
        <input 
          class="nickname-input" 
          type="nickname" 
          placeholder="请输入您的昵称"
          placeholder-class="input-placeholder"
          v-model="nickname"
          maxlength="20"
          @input="onNicknameInput"
        />
        <text class="char-count">{{ nickname.length }}/20</text>
      </view>
      
      <view class="email-section">
        <text class="label">邮箱</text>
        <input 
          class="email-input" 
          type="text" 
          placeholder="请输入您的邮箱（选填）"
          placeholder-class="input-placeholder"
          v-model="email"
          @input="onEmailInput"
        />
        <text v-if="emailError" class="error-text">{{ emailError }}</text>
      </view>
      
      <view class="phone-section">
        <text class="label">手机号</text>
        <input 
          class="phone-input" 
          type="number" 
          placeholder="请输入您的手机号（选填）"
          placeholder-class="input-placeholder"
          v-model="phone"
          maxlength="11"
          @input="onPhoneInput"
        />
        <text v-if="phoneError" class="error-text">{{ phoneError }}</text>
      </view>
    </view>
    
    <view class="editor-actions">
      <button 
        class="save-button" 
        :disabled="!canSave || saving"
        @click="saveUserInfo"
      >
        {{ saving ? '保存中...' : '保存' }}
      </button>
      <button class="cancel-button" @click="cancel">稍后再说</button>
    </view>
  </view>
</template>

<script>
import { ref, watch, onMounted, computed } from 'vue';
import { updateUserInfoFromComponent, uploadAvatarWithFile } from '../services/api';
import { userState } from '../services/userService';

export default {
  name: 'UserInfoEditor',
  
  props: {
    // 初始用户信息
    initialUserInfo: {
      type: Object,
      default: () => ({})
    },
    // 是否必须完成
    required: {
      type: Boolean,
      default: false
    },
    // 新增：从父组件传入的提示信息
    promptMessage: {
      type: String,
      default: ''
    }
  },
  
  emits: ['success', 'cancel'],
  
  setup(props, { emit }) {
    const avatarUrl = ref('');
    const displayAvatarUrl = ref('/static/images/default-avatar.png');
    const nickname = ref('');
    const email = ref('');
    const phone = ref('');
    const saving = ref(false);
    const emailError = ref('');
    const phoneError = ref('');

    const defaultAvatar = '/static/images/default-avatar.png';

    const canSave = computed(() => {
      const hasValidNickname = nickname.value.trim().length >= 2;
      const hasValidEmail = !email.value || isValidEmail(email.value);
      const hasValidPhone = !phone.value || isValidPhone(phone.value);
      return hasValidNickname && hasValidEmail && hasValidPhone;
    });
    
    const resolveAvatarUrl = async (sourceUrl) => {
      if (!sourceUrl) {
        return defaultAvatar;
      }
      if (sourceUrl.startsWith('cloud://')) {
        try {
          const res = await wx.cloud.getTempFileURL({ fileList: [sourceUrl] });
          if (res.fileList && res.fileList.length > 0 && res.fileList[0].tempFileURL) {
            return res.fileList[0].tempFileURL;
          }
        } catch (error) {
          console.error('获取临时头像链接失败:', error);
          return defaultAvatar;
        }
      }
      return sourceUrl;
    };

    const initializeUserData = async () => {
      if (props.initialUserInfo && Object.keys(props.initialUserInfo).length > 0) {
        const info = props.initialUserInfo;
        avatarUrl.value = info.avatarUrl || '';
        displayAvatarUrl.value = await resolveAvatarUrl(info.avatarUrl) || defaultAvatar;
        nickname.value = info.nickname || '';
        email.value = info.email && !info.email.includes('@wechat.local') ? info.email : '';
        phone.value = info.phone || '';
      }
    };

    // 处理微信头像选择
    const onChooseAvatar = async (e) => {
      console.log('微信头像选择事件触发:', e);
      
      const wechatAvatarUrl = e.detail.avatarUrl;
      if (wechatAvatarUrl) {
        console.log('获取到微信头像URL:', wechatAvatarUrl);
        
        try {
          // 获取用户ID
          const userId = userState.user?.id;
          if (!userId) {
            console.error('无法获取用户ID，无法上传头像');
            return;
          }

          // 下载微信头像并上传到我们的云存储
          console.log('开始下载并上传微信头像...');
          
          // 1. 下载微信头像到本地临时文件
          const downloadRes = await new Promise((resolve, reject) => {
            uni.downloadFile({
              url: wechatAvatarUrl,
              success: resolve,
              fail: reject
            });
          });

          console.log('微信头像下载成功:', downloadRes.tempFilePath);

          // 2. 上传到我们的云存储
          const uploadResult = await uploadAvatarWithFile(userId, downloadRes.tempFilePath);
          
          if (uploadResult.success && uploadResult.avatarUrl) {
            // 使用上传后的URL
            avatarUrl.value = uploadResult.avatarUrl;
            
            // 解析并显示头像
            if (uploadResult.avatarUrl.startsWith('cloud://')) {
              displayAvatarUrl.value = await resolveAvatarUrl(uploadResult.avatarUrl);
            } else {
              displayAvatarUrl.value = uploadResult.avatarUrl;
            }
            
            console.log('✅ 微信头像上传完成，新URL:', uploadResult.avatarUrl);
          } else {
            console.error('头像上传失败:', uploadResult.error);
            // 失败时仍然尝试使用原始URL
            avatarUrl.value = wechatAvatarUrl;
            displayAvatarUrl.value = wechatAvatarUrl;
          }
          
        } catch (error) {
          console.error('处理微信头像失败:', error);
          // 出错时仍然尝试使用原始URL
          avatarUrl.value = wechatAvatarUrl;
          displayAvatarUrl.value = wechatAvatarUrl;
        }
      } else {
        console.warn('未获取到有效的头像URL');
      }
    };
    
    const onNicknameInput = (e) => {
      nickname.value = e.detail.value;
    };
    
    const onEmailInput = (e) => {
      email.value = e.detail.value;
      validateEmail();
    };

    const onPhoneInput = (e) => {
      phone.value = e.detail.value;
      validatePhone();
    };

    const isValidEmail = (val) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val);
    const isValidPhone = (val) => /^1[3-9]\d{9}$/.test(val);

    const validateEmail = () => {
      emailError.value = email.value && !isValidEmail(email.value) ? '请输入正确的邮箱格式' : '';
    };

    const validatePhone = () => {
      phoneError.value = phone.value && !isValidPhone(phone.value) ? '请输入正确的手机号格式' : '';
    };

    const saveUserInfo = async () => {
      if (!canSave.value) {
        uni.showToast({ title: '请检查输入项', icon: 'none' });
        return;
      }
      try {
        saving.value = true;
        const userInfo = {
          nickName: nickname.value.trim(),
          avatarUrl: avatarUrl.value
        };
        if (email.value && isValidEmail(email.value)) {
          userInfo.email = email.value.trim();
        }
        if (phone.value && isValidPhone(phone.value)) {
          userInfo.phone = phone.value.trim();
        }
        
        const result = await updateUserInfoFromComponent(userInfo);
        if (result.success) {
          uni.showToast({ title: '保存成功', icon: 'success' });
          emit('success', { userInfo: result.data, originalData: userInfo });
        } else {
          throw new Error(result.message || '保存失败');
        }
      } catch (error) {
        console.error('保存用户信息失败:', error);
        uni.showToast({ title: error.message || '保存失败，请重试', icon: 'none' });
      } finally {
        saving.value = false;
      }
    };

    const cancel = () => {
      if (props.required) {
        uni.showModal({
          title: '提示',
          content: '完善资料后可以获得更好的使用体验，确定要跳过吗？',
          success: (res) => {
            if (res.confirm) {
              emit('cancel');
            }
          }
        });
      } else {
        emit('cancel');
      }
    };

    watch(() => props.initialUserInfo, initializeUserData, { deep: true, immediate: true });
    
    onMounted(initializeUserData);

    return {
      avatarUrl,
      displayAvatarUrl,
      nickname,
      email,
      phone,
      saving,
      emailError,
      phoneError,
      canSave,
      onChooseAvatar,
      onNicknameInput,
      onEmailInput,
      onPhoneInput,
      saveUserInfo,
      cancel
    };
  }
};
</script>

<style scoped>
.user-info-editor {
  padding: 32rpx 24rpx;
  background-color: transparent;
  margin: 0;
}

.editor-header {
  text-align: center;
  margin-bottom: 40rpx;
  padding-bottom: 0;
}

.title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1c170d;
  display: block;
  margin-bottom: 6rpx;
  line-height: 1.2;
}

.subtitle {
  font-size: 24rpx;
  color: #9d8148;
  display: block;
  line-height: 1.4;
}

.editor-content {
  margin-bottom: 32rpx;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-bottom: 48rpx;
  padding: 0;
}

.avatar-section .label {
  text-align: center;
}

/* 新增：头像下方提示信息样式 */
.info-prompt-under-avatar {
  text-align: center;
  padding: 16rpx 24rpx;
  margin-top: -16rpx; /* 向上微调，减少与头像的间距 */
  margin-bottom: 32rpx; /* 与下方表单的间距 */
  font-size: 24rpx;
  color: #9d8148;
  background-color: #f4efe7;
  border-radius: 16rpx;
  line-height: 1.5;
}

.label {
  font-size: 28rpx;
  color: #1c170d;
  font-weight: 600;
  display: block;
  margin-bottom: 16rpx;
  text-align: left;
}

.avatar-button {
  position: relative;
  width: 160rpx;
  height: 160rpx;
  border-radius: 80rpx;
  overflow: hidden;
  cursor: pointer;
  border: none;
  padding: 0;
  background: none;
}

.avatar-button::after {
  border: none;
}

.avatar-image {
  width: 100%;
  height: 100%;
  border-radius: 80rpx;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(28, 23, 13, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 80rpx;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.avatar-button:active .avatar-overlay {
  opacity: 1;
}

.avatar-text {
  color: #ffffff;
  font-size: 22rpx;
  font-weight: 500;
}

.nickname-section,
.email-section,
.phone-section {
  position: relative;
  margin-bottom: 32rpx;
  padding: 0;
  background-color: transparent;
}

.required {
  color: #e74c3c;
  font-size: 24rpx;
  margin-left: 4rpx;
}

.nickname-input,
.email-input,
.phone-input {
  width: 100%;
  min-height: 76rpx;
  padding: 20rpx 28rpx;
  background-color: #f4efe7;
  border-radius: 24rpx;
  border: none;
  font-size: 26rpx;
  color: #1c170d;
  line-height: 1.4;
  box-sizing: border-box;
  transition: all 0.3s ease;
}

.nickname-input:focus,
.email-input:focus,
.phone-input:focus {
  background-color: #f0e8d8;
  outline: none;
  transform: translateY(-2rpx);
  box-shadow: 0 4rpx 16rpx rgba(247, 189, 74, 0.2);
}

.input-placeholder {
  color: #9d8148;
}

.error-text {
  position: absolute;
  left: 0;
  bottom: -28rpx;
  font-size: 22rpx;
  color: #e74c3c;
  font-weight: 500;
}

.char-count {
  position: absolute;
  right: 0;
  bottom: -28rpx;
  font-size: 22rpx;
  color: #9d8148;
}

.editor-actions {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  padding-top: 48rpx;
  margin-top: 24rpx;
}

.save-button {
  width: 100%;
  height: 88rpx;
  background: linear-gradient(135deg, #f7bd4a 0%, #f5b942 100%);
  color: #1c170d;
  border: none;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: 600;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 24rpx rgba(247, 189, 74, 0.3);
}

.save-button:active {
  transform: translateY(2rpx);
  box-shadow: 0 3rpx 12rpx rgba(247, 189, 74, 0.4);
}

.save-button:disabled {
  background: #e9e0ce;
  color: #9d8148;
  transform: none;
  box-shadow: none;
}

.cancel-button {
  width: 100%;
  height: 72rpx;
  background-color: transparent;
  color: #9d8148;
  border: none;
  border-radius: 36rpx;
  font-size: 28rpx;
  font-weight: 500;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cancel-button:active {
  background-color: rgba(157, 129, 72, 0.1);
  color: #1c170d;
  transform: scale(0.98);
}

@media (max-width: 750rpx) {
  .user-info-editor {
    padding: 24rpx 20rpx;
  }
  
  .editor-header {
    margin-bottom: 32rpx;
  }
  
  .title {
    font-size: 28rpx;
  }
  
  .subtitle {
    font-size: 22rpx;
  }
  
  .avatar-section {
    margin-bottom: 36rpx;
  }
  
  .avatar-button {
    width: 140rpx;
    height: 140rpx;
    border-radius: 70rpx;
  }
  
  .avatar-image {
    border-radius: 70rpx;
  }
  
  .avatar-overlay {
    border-radius: 70rpx;
  }
  
  .info-prompt-under-avatar {
    margin-bottom: 28rpx;
  }
  
  .nickname-section,
  .email-section,
  .phone-section {
    margin-bottom: 28rpx;
  }
  
  .nickname-input,
  .email-input,
  .phone-input {
    min-height: 76rpx;
    font-size: 26rpx;
    padding: 20rpx 28rpx;
  }
  
  .editor-actions {
    padding-top: 36rpx;
  }
  
  .save-button {
    height: 80rpx;
    font-size: 28rpx;
    border-radius: 40rpx;
  }
  
  .cancel-button {
    height: 64rpx;
    font-size: 26rpx;
    border-radius: 32rpx;
  }
}
</style>