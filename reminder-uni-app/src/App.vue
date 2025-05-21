<script>
import { getUserProfile } from './services/api';
import { userState, saveUserInfo } from './services/store';

export default {
  onLaunch: function () {
    console.log('App Launch');
    this.checkLogin();
  },
  onShow: function () {
    console.log('App Show');
  },
  onHide: function () {
    console.log('App Hide');
  },
  methods: {
    async checkLogin() {
      const token = uni.getStorageSync('accessToken');
      
      if (token) {
        try {
          // 获取用户信息
          const userInfo = await getUserProfile();
          if (userInfo) {
            saveUserInfo(userInfo);
          }
        } catch (error) {
          console.error('获取用户信息失败:', error);
          // 清除无效的token
          uni.removeStorageSync('accessToken');
          userState.isAuthenticated = false;
          userState.user = null;
          
          // 跳转到登录页面
          uni.reLaunch({
            url: '/pages/login/login'
          });
        }
      } else {
        // 无token时跳转到登录页
        uni.reLaunch({
          url: '/pages/login/login'
        });
      }
    }
  }
}
</script>

<style>
/*每个页面公共css */
page {
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica, 
               Segoe UI, Arial, Roboto, 'PingFang SC', 'miui', 'Hiragino Sans GB', 
               'Microsoft Yahei', sans-serif;
  font-size: 28rpx;
  color: #333333;
  background-color: #f8f8f8;
}

/* 清除默认样式 */
view, text, input, textarea, button {
  box-sizing: border-box;
}

/* 按钮通用样式 */
button {
  margin: 0;
  padding: 0;
  background-color: transparent;
  border: none;
}

button::after {
  border: none;
}

/* 内容区域通用样式 */
.container {
  padding: 30rpx;
}

/* 卡片通用样式 */
.card {
  background-color: #ffffff;
  border-radius: 10rpx;
  padding: 30rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}

/* 表单通用样式 */
.form-group {
  margin-bottom: 30rpx;
}

.label {
  display: block;
  font-size: 28rpx;
  color: #666666;
  margin-bottom: 10rpx;
}

.input {
  width: 100%;
  height: 80rpx;
  background-color: #f5f5f5;
  border-radius: 8rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
}

/* 按钮通用样式 */
.btn {
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border-radius: 8rpx;
  font-size: 28rpx;
}

.btn-primary {
  background-color: #3cc51f;
  color: #ffffff;
}

.btn-default {
  background-color: #f0f0f0;
  color: #666666;
}

/* 错误提示样式 */
.error-msg {
  color: #ff4d4f;
  font-size: 24rpx;
  margin-top: 10rpx;
}
</style>
