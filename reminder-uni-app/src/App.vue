<script>
import { onMounted } from 'vue';
import { UserService } from './services/userService';

export default {
  setup() {
    onMounted(() => {
      console.log('🚀 App: onMounted - 全局应用挂载');
    });

    return {};
  },
  onLaunch: function () {
    console.log('App Launch');
    this.initUserService();
  },
  onShow: function () {
    console.log('App Show');
  },
  onHide: function () {
    console.log('App Hide');
  },
  methods: {
    async initUserService() {
      try {
        console.log('🚀 App: 初始化用户服务');
        await UserService.init();

        if (UserService.getUserState().isAuthenticated) {
          console.log('✅ App: 用户服务初始化成功，用户已登录');
        } else {
          console.log('📝 App: 用户未登录，等待用户操作时弹出登录框');
        }
      } catch (error) {
        console.error('❌ App: 用户服务初始化失败:', error);
      }
    }
  }
}
</script>

<style>
/*每个页面公共css */
@import './styles/modal.scss';
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
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

/* 列表通用样式 */
.list-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.list-item:last-child {
  border-bottom: none;
}

/* 文本通用样式 */
.text-primary {
  color: #1c170d;
}

.text-secondary {
  color: #666666;
}

.text-muted {
  color: #999999;
}

.text-success {
  color: #52c41a;
}

.text-warning {
  color: #faad14;
}

.text-error {
  color: #ff4d4f;
}

/* 间距通用样式 */
.mb-small {
  margin-bottom: 16rpx;
}

.mb-medium {
  margin-bottom: 24rpx;
}

.mb-large {
  margin-bottom: 32rpx;
}

.mt-small {
  margin-top: 16rpx;
}

.mt-medium {
  margin-top: 24rpx;
}

.mt-large {
  margin-top: 32rpx;
}
</style>
