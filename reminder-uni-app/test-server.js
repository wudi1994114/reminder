/**
 * 本地测试服务器
 * 用于开发环境测试语音识别功能
 * 
 * 使用方法：
 * 1. npm install express multer cors
 * 2. node test-server.js
 * 3. 在微信开发者工具中测试语音功能
 */

const express = require('express');
const multer = require('multer');
const cors = require('cors');
const fs = require('fs');
const path = require('path');

const app = express();
const port = 3000;

// 配置CORS，允许微信开发者工具访问
app.use(cors({
  origin: '*',
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));

// 配置文件上传
const upload = multer({ 
  dest: 'uploads/',
  limits: {
    fileSize: 10 * 1024 * 1024 // 10MB限制
  }
});

// 确保uploads目录存在
if (!fs.existsSync('uploads')) {
  fs.mkdirSync('uploads');
}

// 模拟识别结果数据
const mockResults = [
  '今天天气真不错',
  '提醒我明天开会',
  '买菜做饭洗衣服',
  '这是语音识别测试',
  '语音功能正常工作',
  '测试录音识别功能',
  '微信小程序语音输入',
  '语音转文字成功',
  '开发环境测试完成',
  '录音上传功能正常',
  '后端服务连接成功',
  '语音识别API调用成功'
];

// 健康检查接口
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    message: '语音识别测试服务器运行正常',
    timestamp: new Date().toISOString()
  });
});

// 语音识别接口
app.post('/api/speech/recognize', upload.single('audio'), (req, res) => {
  console.log('\n🎤 收到语音识别请求');
  console.log('📅 时间:', new Date().toLocaleString());
  
  try {
    // 检查文件是否上传成功
    if (!req.file) {
      console.log('❌ 没有收到音频文件');
      return res.status(400).json({
        success: false,
        message: '音频文件为空'
      });
    }

    // 打印文件信息
    console.log('📁 文件信息:');
    console.log('  - 原始文件名:', req.file.originalname);
    console.log('  - 文件大小:', req.file.size, 'bytes');
    console.log('  - 文件类型:', req.file.mimetype);
    console.log('  - 临时路径:', req.file.path);

    // 打印表单数据
    if (req.body) {
      console.log('📋 表单数据:', req.body);
    }

    // 模拟处理时间（1-3秒）
    const processingTime = Math.random() * 2000 + 1000;
    console.log(`⏳ 模拟处理时间: ${Math.round(processingTime)}ms`);

    setTimeout(() => {
      // 随机选择一个识别结果
      const randomResult = mockResults[Math.floor(Math.random() * mockResults.length)];
      
      console.log('✅ 模拟识别结果:', randomResult);
      
      // 返回识别结果
      res.json({
        success: true,
        text: randomResult,
        confidence: Math.random() * 0.3 + 0.7, // 0.7-1.0之间的置信度
        duration: req.file.size / 1000, // 模拟音频时长
        message: '识别成功',
        debug: {
          fileSize: req.file.size,
          fileName: req.file.originalname,
          processingTime: Math.round(processingTime)
        }
      });

      // 清理临时文件
      setTimeout(() => {
        try {
          fs.unlinkSync(req.file.path);
          console.log('🗑️ 临时文件已清理:', req.file.path);
        } catch (error) {
          console.log('⚠️ 清理临时文件失败:', error.message);
        }
      }, 1000);

    }, processingTime);

  } catch (error) {
    console.error('❌ 处理请求失败:', error);
    res.status(500).json({
      success: false,
      message: '服务器内部错误: ' + error.message
    });
  }
});

// 错误处理中间件
app.use((error, req, res, next) => {
  console.error('❌ 服务器错误:', error);
  
  if (error instanceof multer.MulterError) {
    if (error.code === 'LIMIT_FILE_SIZE') {
      return res.status(400).json({
        success: false,
        message: '文件大小超出限制（最大10MB）'
      });
    }
  }
  
  res.status(500).json({
    success: false,
    message: '服务器内部错误'
  });
});

// 404处理
app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: '接口不存在'
  });
});

// 启动服务器
app.listen(port, () => {
  console.log('\n🚀 语音识别测试服务器启动成功！');
  console.log(`📍 服务地址: http://localhost:${port}`);
  console.log(`🔍 健康检查: http://localhost:${port}/health`);
  console.log(`🎤 语音识别: POST http://localhost:${port}/api/speech/recognize`);
  console.log('\n📋 使用说明:');
  console.log('1. 确保微信开发者工具的网络设置允许访问本地服务器');
  console.log('2. 在小程序中测试语音功能');
  console.log('3. 查看控制台输出的详细日志');
  console.log('\n⚠️  注意: 这只是开发测试服务器，不要在生产环境使用！');
  console.log('─'.repeat(60));
});

// 优雅关闭
process.on('SIGINT', () => {
  console.log('\n👋 正在关闭服务器...');
  
  // 清理uploads目录
  try {
    const files = fs.readdirSync('uploads');
    files.forEach(file => {
      fs.unlinkSync(path.join('uploads', file));
    });
    console.log('🗑️ 临时文件已清理');
  } catch (error) {
    console.log('⚠️ 清理临时文件失败:', error.message);
  }
  
  process.exit(0);
});
