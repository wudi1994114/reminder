<template>
  <view class="user-info-editor">    
    <view class="editor-content">
      <view class="avatar-section">
        <text class="label">头像</text>
        <button 
          class="avatar-button" 
          open-type="chooseAvatar" 
          @chooseavatar="onChooseAvatar"
        >
          <image 
            class="avatar-image" 
            :src="avatarUrl || '/static/images/default-avatar.png'"
            mode="aspectFill"
          ></image>
          <view class="avatar-overlay">
            <text class="avatar-text">点击选择</text>
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
import { updateUserInfoFromComponent } from '../services/api';

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
  
  data() {
    return {
      avatarUrl: '',
      nickname: '',
      email: '',
      phone: '',
      saving: false,
      emailError: '',
      phoneError: ''
    };
  },
  
  computed: {
    canSave() {
      const hasValidNickname = this.nickname.trim().length >= 2;
      const hasValidEmail = !this.email || this.isValidEmail(this.email);
      const hasValidPhone = !this.phone || this.isValidPhone(this.phone);
      return hasValidNickname && hasValidEmail && hasValidPhone;
    }
  },
  
  mounted() {
    this.initializeUserData();
  },
  
  watch: {
    initialUserInfo: {
      handler(newVal) {
        if (newVal && Object.keys(newVal).length > 0) {
          console.log('UserInfoEditor: 检测到初始用户信息变化:', newVal);
          this.initializeUserData();
        }
      },
      deep: true,
      immediate: true
    }
  },
  
  methods: {
    initializeUserData() {
      console.log('UserInfoEditor: 初始化用户数据:', this.initialUserInfo);
      if (this.initialUserInfo && Object.keys(this.initialUserInfo).length > 0) {
        if (this.initialUserInfo.avatarUrl) {
          this.avatarUrl = this.initialUserInfo.avatarUrl;
          console.log('UserInfoEditor: 设置头像:', this.initialUserInfo.avatarUrl);
        }
        if (this.initialUserInfo.nickname) {
          this.nickname = this.initialUserInfo.nickname;
          console.log('UserInfoEditor: 设置昵称:', this.initialUserInfo.nickname);
        }
        console.log('UserInfoEditor: 邮箱字段检查 - 原始值:', this.initialUserInfo.email, '类型:', typeof this.initialUserInfo.email);
        if (this.initialUserInfo.email && this.initialUserInfo.email.trim() !== '') {
          this.email = this.initialUserInfo.email;
          console.log('UserInfoEditor: 设置邮箱:', this.initialUserInfo.email);
        } else {
          console.log('UserInfoEditor: 邮箱为空，不设置');
        }
        console.log('UserInfoEditor: 手机号字段检查 - 原始值:', this.initialUserInfo.phone, '类型:', typeof this.initialUserInfo.phone);
        if (this.initialUserInfo.phone && this.initialUserInfo.phone.trim() !== '') {
          this.phone = this.initialUserInfo.phone;
          console.log('UserInfoEditor: 设置手机号:', this.initialUserInfo.phone);
        } else {
          console.log('UserInfoEditor: 手机号为空，不设置');
        }
      } else {
        console.log('UserInfoEditor: 初始用户信息为空或无效');
      }
    },
    onChooseAvatar(e) {
      const { avatarUrl } = e.detail;
      this.avatarUrl = avatarUrl;
      console.log('选择的头像:', avatarUrl);
    },
    onNicknameInput(e) {
      this.nickname = e.detail.value;
    },
    onEmailInput(e) {
      this.email = e.detail.value;
      this.validateEmail();
    },
    onPhoneInput(e) {
      this.phone = e.detail.value;
      this.validatePhone();
    },
    validateEmail() {
      if (this.email && !this.isValidEmail(this.email)) {
        this.emailError = '请输入正确的邮箱格式';
      } else {
        this.emailError = '';
      }
    },
    validatePhone() {
      if (this.phone && !this.isValidPhone(this.phone)) {
        this.phoneError = '请输入正确的手机号格式';
      } else {
        this.phoneError = '';
      }
    },
    isValidEmail(email) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      return emailRegex.test(email);
    },
    isValidPhone(phone) {
      const phoneRegex = /^1[3-9]\d{9}$/;
      return phoneRegex.test(phone);
    },
    async saveUserInfo() {
      if (!this.canSave) {
        uni.showToast({
          title: '昵称至少需要2个字符',
          icon: 'none'
        });
        return;
      }
      try {
        this.saving = true;
        const userInfo = {
          nickName: this.nickname.trim(),
          avatarUrl: this.avatarUrl
        };
        if (this.email && this.isValidEmail(this.email)) {
          userInfo.email = this.email.trim();
        }
        if (this.phone && this.isValidPhone(this.phone)) {
          userInfo.phone = this.phone.trim();
        }
        console.log('保存用户信息:', userInfo);
        const result = await updateUserInfoFromComponent(userInfo);
        if (result.success) {
          uni.showToast({
            title: '保存成功',
            icon: 'success'
          });
          this.$emit('success', {
            userInfo: result.data,
            originalData: userInfo
          });
        } else {
          throw new Error(result.message || '保存失败');
        }
      } catch (error) {
        console.error('保存用户信息失败:', error);
        uni.showToast({
          title: error.message || '保存失败，请重试',
          icon: 'none'
        });
      } finally {
        this.saving = false;
      }
    },
    cancel() {
      if (this.required) {
        uni.showModal({
          title: '提示',
          content: '完善资料后可以获得更好的使用体验，确定要跳过吗？',
          success: (res) => {
            if (res.confirm) {
              this.$emit('cancel');
            }
          }
        });
      } else {
        this.$emit('cancel');
      }
    }
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
  border: none;
  padding: 0;
  background: #f0f0f0;
  transition: all 0.3s ease;
  margin: 0 auto;
  box-shadow: 0 4rpx 20rpx rgba(28, 23, 13, 0.1);
}

.avatar-button:active {
  transform: scale(0.96);
  box-shadow: 0 2rpx 12rpx rgba(28, 23, 13, 0.15);
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