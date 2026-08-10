# TTS 文本转语音 API 使用说明

## 📌 配置说明

在 `application-wechat.yml` 中配置腾讯云密钥：

```yaml
tencent:
  cloud:
    secret-id: "你的SecretId"
    secret-key: "你的SecretKey"
    tts:
      region: ap-beijing
      default-voice-type: 0
      default-speed: 0
      default-volume: 5
```

## 🔑 接口列表

所有接口都在 `/api/tts/` 路径下，**无需鉴权**，可以直接访问。

---

### 1. 简单测试接口

**POST** `/api/tts/test`

最简单的测试接口，输入文本返回Base64编码的音频。

**请求示例：**
```bash
curl -X POST http://localhost:8080/api/tts/test \
  -H "Content-Type: application/json" \
  -d '{
    "text": "你好，这是一个测试"
  }'
```

**响应示例：**
```json
{
  "success": true,
  "message": "转换成功",
  "requestId": "xxx-xxx-xxx",
  "audioSize": 12345,
  "codec": "mp3",
  "audioData": "base64编码的音频数据..."
}
```

---

### 2. 高级转换接口

**POST** `/api/tts/convert`

支持自定义音色、语速、音量等参数。

**请求示例：**
```bash
curl -X POST http://localhost:8080/api/tts/convert \
  -H "Content-Type: application/json" \
  -d '{
    "text": "春眠不觉晓，处处闻啼鸟",
    "voiceType": 1,
    "speed": 0,
    "volume": 7,
    "codec": "mp3"
  }'
```

**参数说明：**
- `text` (必填): 要转换的文本，最大150字符
- `voiceType` (可选): 音色ID，默认0
  - 0: 亲和女声
  - 1: 亲和男声
  - 2: 成熟男声
  - 4: 温暖女声
  - 5: 情感女声
  - 6: 情感男声
- `speed` (可选): 语速，范围[-2, 2]，默认0
  - -2: 0.6倍速
  - -1: 0.8倍速
  - 0: 1.0倍速
  - 1: 1.2倍速
  - 2: 1.5倍速
- `volume` (可选): 音量，范围[0, 10]，默认5
- `codec` (可选): 音频格式，可选 mp3/wav/pcm，默认mp3

---

### 3. 下载音频文件

**POST** `/api/tts/download`

直接下载音频文件，不返回JSON。

**请求示例：**
```bash
curl -X POST http://localhost:8080/api/tts/download \
  -H "Content-Type: application/json" \
  -d '{
    "text": "这是下载测试",
    "voiceType": 0
  }' \
  --output audio.mp3
```

**特点：**
- 直接返回音频文件流
- 响应头包含 `Content-Disposition: attachment`
- 可以直接下载保存

---

### 4. 音频流播放

**GET** `/api/tts/stream`

用于浏览器直接播放，返回音频流。

**请求示例：**
```bash
# 浏览器访问
http://localhost:8080/api/tts/stream?text=测试播放&voiceType=0&speed=0

# 或使用curl
curl "http://localhost:8080/api/tts/stream?text=测试播放&voiceType=0" --output test.mp3
```

**参数说明：**
- `text` (必填): 要转换的文本
- `voiceType` (可选): 音色ID，默认0
- `speed` (可选): 语速，默认0

**特点：**
- GET请求，方便浏览器直接访问
- 返回音频流，可以在 `<audio>` 标签中直接播放
- 设置了 `Content-Type: audio/mpeg`

---

### 5. 文本验证

**GET** `/api/tts/validate`

验证文本长度是否符合要求。

**请求示例：**
```bash
curl "http://localhost:8080/api/tts/validate?text=这是测试文本"
```

**响应示例：**
```json
{
  "valid": true,
  "length": 6,
  "maxLength": 150,
  "message": "文本长度合法"
}
```

---

### 6. 文本分段

**POST** `/api/tts/split`

将长文本按指定长度分段。

**请求示例：**
```bash
curl -X POST http://localhost:8080/api/tts/split \
  -H "Content-Type: application/json" \
  -d '{
    "text": "很长很长的文本...",
    "maxLength": 150
  }'
```

**响应示例：**
```json
{
  "success": true,
  "totalLength": 300,
  "segmentCount": 2,
  "segments": [
    "第一段文本...",
    "第二段文本..."
  ]
}
```

---

### 7. 健康检查

**GET** `/api/tts/health`

检查TTS服务是否正常。

**请求示例：**
```bash
curl http://localhost:8080/api/tts/health
```

**响应示例：**
```json
{
  "status": "ok",
  "service": "TTS文本转语音服务",
  "timestamp": 1705478400000
}
```

---

## 🌐 前端使用示例

### HTML + JavaScript

```html
<!DOCTYPE html>
<html>
<head>
    <title>TTS测试</title>
</head>
<body>
    <h1>文本转语音测试</h1>
    
    <textarea id="text" rows="4" cols="50" placeholder="输入要转换的文本..."></textarea><br>
    
    <select id="voiceType">
        <option value="0">亲和女声</option>
        <option value="1">亲和男声</option>
        <option value="2">成熟男声</option>
    </select>
    
    <button onclick="playAudio()">播放</button>
    <button onclick="downloadAudio()">下载</button>
    
    <audio id="audioPlayer" controls style="display:block; margin-top:20px;"></audio>
    
    <script>
        const API_BASE = 'http://localhost:8080/api/tts';
        
        // 播放音频
        function playAudio() {
            const text = document.getElementById('text').value;
            const voiceType = document.getElementById('voiceType').value;
            
            if (!text) {
                alert('请输入文本');
                return;
            }
            
            // 直接使用stream接口
            const url = `${API_BASE}/stream?text=${encodeURIComponent(text)}&voiceType=${voiceType}`;
            
            const player = document.getElementById('audioPlayer');
            player.src = url;
            player.play();
        }
        
        // 下载音频
        async function downloadAudio() {
            const text = document.getElementById('text').value;
            const voiceType = document.getElementById('voiceType').value;
            
            if (!text) {
                alert('请输入文本');
                return;
            }
            
            const response = await fetch(`${API_BASE}/download`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    text: text,
                    voiceType: parseInt(voiceType)
                })
            });
            
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'tts-audio.mp3';
            a.click();
        }
    </script>
</body>
</html>
```

---

## 📝 注意事项

1. **文本长度限制**: 单次请求最多150字符
2. **Base64解码**: `/test` 和 `/convert` 接口返回的 `audioData` 是Base64编码，需要解码后才能播放
3. **音频格式**: 默认返回MP3格式，也支持WAV和PCM
4. **无需鉴权**: 所有 `/api/tts/**` 接口都无需JWT token
5. **日志输出**: 所有请求都会在后端输出详细日志，方便调试

---

## 🧪 Postman测试集合

可以导入以下JSON到Postman进行测试：

```json
{
  "info": {
    "name": "TTS API测试",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "简单测试",
      "request": {
        "method": "POST",
        "url": "http://localhost:8080/api/tts/test",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\"text\": \"你好，这是测试\"}"
        }
      }
    },
    {
      "name": "高级转换",
      "request": {
        "method": "POST",
        "url": "http://localhost:8080/api/tts/convert",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\"text\": \"测试文本\", \"voiceType\": 1, \"speed\": 0}"
        }
      }
    },
    {
      "name": "音频流播放",
      "request": {
        "method": "GET",
        "url": "http://localhost:8080/api/tts/stream?text=测试&voiceType=0"
      }
    },
    {
      "name": "健康检查",
      "request": {
        "method": "GET",
        "url": "http://localhost:8080/api/tts/health"
      }
    }
  ]
}
```

---

## 🚀 快速开始

1. 配置腾讯云密钥
2. 启动应用
3. 访问健康检查接口确认服务正常
4. 使用简单测试接口进行第一次转换
5. 根据需要使用其他高级接口

**就这么简单！** 🎉

