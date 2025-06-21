<template>
  <view class="container">
    <view class="header-section">
      <view class="nav-container">
        <view class="nav-back" @click="goBack">
          <text class="back-icon">‹</text>
        </view>
        <view class="title-container">
          <text class="page-title">联系作者</text>
        </view>
        <view class="nav-spacer"></view>
      </view>
    </view>
    
    <view class="content-container">
      <view class="author-info-section">
        <view class="author-card">
          <view class="author-avatar">
            <text class="avatar-emoji">👨‍💻</text>
          </view>
          <view class="author-details">
            <text class="author-name">开发者</text>
            <text class="author-email">📧 609679329@qq.com</text>
            <text class="author-description">感谢您使用我们的应用！如果您有任何建议、问题或想法，欢迎随时联系我们。</text>
          </view>
        </view>
      </view>
      
      <view class="feedback-section">
        <view class="section-header">
          <text class="section-title">您的反馈</text>
        </view>
        
        <view class="feedback-form">
          <view class="email-section">
            <view class="input-label">邮箱地址（可选）</view>
            <input
              class="email-input"
              type="text"
              placeholder="请输入您的邮箱地址，方便我们回复您"
              placeholder-class="input-placeholder"
              v-model="email"
              maxlength="255"
              @input="onEmailInput"
            />
            <text v-if="emailError" class="error-text">{{ emailError }}</text>
          </view>

          <view class="textarea-container">
            <view class="input-label">反馈内容</view>
            <textarea
              class="feedback-textarea"
              placeholder="请输入您的意见、建议或问题..."
              placeholder-class="textarea-placeholder"
              v-model="feedbackMessage"
              maxlength="2000"
              :auto-height="true"
              @input="onTextareaInput"
            ></textarea>
            <view class="char-count">{{ feedbackMessage.length }}/2000</view>
          </view>
          
          <button 
            class="submit-button" 
            :disabled="!canSubmit || submitting"
            @click="submitFeedback"
          >
            {{ submitting ? '提交中...' : '提交反馈' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { ref, computed } from 'vue';
import { submitUserFeedback } from '@/services/api';

export default {
  setup() {
    const email = ref('');
    const feedbackMessage = ref('');
    const submitting = ref(false);
    const emailError = ref('');

    // 邮箱验证正则
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    // 计算是否可以提交
    const canSubmit = computed(() => {
      return feedbackMessage.value.trim().length > 0 && !submitting.value && !emailError.value;
    });

    // 处理邮箱输入
    const onEmailInput = (e) => {
      email.value = e.detail.value;
      // 验证邮箱格式
      if (email.value && !emailRegex.test(email.value)) {
        emailError.value = '邮箱格式不正确';
      } else {
        emailError.value = '';
      }
    };

    // 处理文本输入
    const onTextareaInput = (e) => {
      feedbackMessage.value = e.detail.value;
    };

    // 提交反馈
    const submitFeedback = async () => {
      if (!canSubmit.value) {
        return;
      }

      try {
        submitting.value = true;
        
        const response = await submitUserFeedback({
          email: email.value.trim() || null,
          message: feedbackMessage.value.trim()
        });

        if (response.success) {
          uni.showToast({
            title: '提交成功',
            icon: 'success'
          });
          
          // 清空输入框
          email.value = '';
          feedbackMessage.value = '';
          emailError.value = '';
          
          // 延迟返回上一页
          setTimeout(() => {
            goBack();
          }, 1500);
        } else {
          throw new Error(response.message || '提交失败');
        }
      } catch (error) {
        console.error('提交反馈失败:', error);
        uni.showToast({
          title: error.message || '提交失败，请重试',
          icon: 'none'
        });
      } finally {
        submitting.value = false;
      }
    };

    // 返回按钮处理
    const goBack = () => {
      const pages = getCurrentPages();
      if (pages.length <= 1) {
        uni.reLaunch({ url: '/pages/mine/mine' });
      } else {
        uni.navigateBack();
      }
    };

    return {
      email,
      feedbackMessage,
      submitting,
      emailError,
      canSubmit,
      onEmailInput,
      onTextareaInput,
      submitFeedback,
      goBack
    };
  }
};
</script>

<style scoped>
.container {
  height: 100vh;
  background-color: #fcfbf8;
  display: flex;
  flex-direction: column;
  font-family: 'Manrope', 'Noto Sans', sans-serif;
}

.header-section {
  background-color: #fcfbf8;
  padding: calc(var(--status-bar-height, 44rpx) + 80rpx) 32rpx 48rpx;
  flex-shrink: 0;
}

.nav-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 96rpx;
}

.nav-back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 96rpx;
  height: 96rpx;
  cursor: pointer;
}

.back-icon {
  font-size: 48rpx;
  color: #1c170d;
  font-weight: 600;
}

.title-container {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.page-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #1c170d;
  text-align: center;
  line-height: 1.2;
  letter-spacing: -0.015em;
}

.nav-spacer {
  width: 96rpx;
  height: 96rpx;
}

.content-container {
  flex: 1;
  overflow-y: auto;
  background-color: #fcfbf8;
  padding: 16rpx 32rpx 32rpx;
}

.author-info-section {
  margin-bottom: 40rpx;
}

.author-card {
  background-color: #ffffff;
  border: 2rpx solid #f4efe7;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 3rpx 12rpx rgba(28, 23, 13, 0.05);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.author-avatar {
  margin-bottom: 20rpx;
}

.avatar-emoji {
  font-size: 120rpx;
  line-height: 1;
}

.author-details {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.author-name {
  font-size: 32rpx;
  font-weight: 700;
  color: #1c170d;
  line-height: 1.2;
}

.author-email {
  font-size: 28rpx;
  color: #9d8148;
  line-height: 1.4;
}

.author-description {
  font-size: 26rpx;
  color: #666666;
  line-height: 1.5;
  margin-top: 8rpx;
  max-width: 500rpx;
}

.feedback-section {
  margin-bottom: 40rpx;
}

.section-header {
  margin-bottom: 20rpx;
  padding: 0 8rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
  line-height: 1.2;
}

.feedback-form {
  background-color: #ffffff;
  border: 2rpx solid #f4efe7;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 3rpx 12rpx rgba(28, 23, 13, 0.05);
}

.email-section {
  margin-bottom: 32rpx;
}

.input-label {
  font-size: 28rpx;
  font-weight: 600;
  color: #1c170d;
  margin-bottom: 12rpx;
  line-height: 1.2;
}

.email-input {
  width: 100%;
  height: 80rpx;
  padding: 0 24rpx;
  border: 2rpx solid #f4efe7;
  border-radius: 16rpx;
  background-color: #fcfbf8;
  font-size: 28rpx;
  color: #1c170d;
  line-height: 1.5;
  box-sizing: border-box;
}

.input-placeholder {
  color: #9d8148;
}

.error-text {
  font-size: 22rpx;
  color: #e74c3c;
  margin-top: 8rpx;
  line-height: 1.4;
}

.textarea-container {
  position: relative;
  margin-bottom: 32rpx;
}

.feedback-textarea {
  width: 100%;
  min-height: 200rpx;
  padding: 24rpx;
  border: 2rpx solid #f4efe7;
  border-radius: 16rpx;
  background-color: #fcfbf8;
  font-size: 28rpx;
  color: #1c170d;
  line-height: 1.5;
  resize: none;
  box-sizing: border-box;
}

.textarea-placeholder {
  color: #9d8148;
}

.char-count {
  position: absolute;
  bottom: 12rpx;
  right: 16rpx;
  font-size: 22rpx;
  color: #9d8148;
}

.submit-button {
  width: 100%;
  height: 88rpx;
  background-color: #f7bd4a;
  border: none;
  border-radius: 44rpx;
  font-size: 32rpx;
  font-weight: 600;
  color: #1c170d;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 3rpx 12rpx rgba(247, 189, 74, 0.3);
  transition: all 0.2s ease;
}

.submit-button:disabled {
  background-color: #e5e5e5;
  color: #999999;
  box-shadow: none;
}

.submit-button:not(:disabled):active {
  background-color: #f5b235;
  transform: translateY(1rpx);
}

@media (max-width: 750rpx) {
  .header-section {
    padding: calc(var(--status-bar-height, 44rpx) + 32rpx) 24rpx 16rpx;
  }
  
  .nav-container {
    height: 80rpx;
  }
  
  .nav-back,
  .nav-spacer {
    width: 80rpx;
    height: 80rpx;
  }
  
  .back-icon {
    font-size: 40rpx;
  }
  
  .page-title {
    font-size: 32rpx;
  }
  
  .content-container {
    padding: 12rpx 24rpx 24rpx;
  }
  
  .author-card {
    padding: 24rpx;
  }
  
  .avatar-emoji {
    font-size: 100rpx;
  }
  
  .feedback-form {
    padding: 24rpx;
  }

  .email-input {
    height: 72rpx;
    padding: 0 20rpx;
    font-size: 26rpx;
  }

  .feedback-textarea {
    min-height: 160rpx;
    padding: 20rpx;
    font-size: 26rpx;
  }
  
  .submit-button {
    height: 80rpx;
    font-size: 28rpx;
    border-radius: 40rpx;
  }
}
</style>
