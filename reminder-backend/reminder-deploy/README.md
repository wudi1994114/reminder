# Reminder 腾讯云部署目录

这是专门为腾讯云开发平台创建的部署目录，包含了运行 reminder-core 服务所需的所有模块。

## 📁 目录结构

```
reminder-deploy/
├── Dockerfile          # 腾讯云专用Dockerfile
├── pom.xml             # 部署项目的Maven配置
├── reminder-common/    # 公共模块
└── reminder-core/      # 核心服务模块
```

## 🚀 腾讯云部署步骤

### 1. 准备部署包
将整个 `reminder-deploy` 目录打包上传到腾讯云开发平台。

### 2. 配置构建参数
在腾讯云开发平台中设置：
- **Dockerfile路径**: `Dockerfile`
- **构建上下文**: 根目录

### 3. 构建镜像
腾讯云会自动执行以下构建流程：
1. 使用Maven构建 reminder-common 模块
2. 构建 reminder-core 模块（依赖common）
3. 创建运行时镜像
4. 启动Spring Boot应用

## 🔧 环境变量配置

在腾讯云平台中可以配置以下环境变量：

```bash
# Spring Boot配置
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080

# JVM参数（已在Dockerfile中预设）
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC

# 时区设置（已在Dockerfile中设置为Asia/Shanghai）
TZ=Asia/Shanghai
```

## 📊 健康检查

应用启动后，可以通过以下端点检查服务状态：
- 健康检查: `http://localhost:8080/actuator/health`
- 应用信息: `http://localhost:8080/actuator/info`

## 🔄 更新部署

当需要更新代码时：

1. 在 `reminder-backend` 目录下运行同步脚本：
   ```bash
   ./sync-deploy.sh
   ```

2. 重新打包 `reminder-deploy` 目录并上传到腾讯云

## 📝 注意事项

- 确保腾讯云平台有足够的内存（建议至少1GB）
- 首次构建可能需要较长时间（下载Maven依赖）
- 应用启动需要约60秒，请耐心等待健康检查通过

## 🐳 Docker镜像信息

- **基础镜像**: maven:3.8.4-openjdk-8 (构建阶段)
- **运行镜像**: openjdk:8-jdk-slim
- **暴露端口**: 8080
- **工作目录**: /app
- **应用用户**: appuser (非root用户，安全最佳实践)

## 🔍 故障排查

如果部署失败，请检查：
1. 所有模块的pom.xml文件是否正确
2. reminder-core是否正确依赖reminder-common
3. 腾讯云平台的构建日志中是否有错误信息

---

*此部署配置由自动化脚本生成，如需修改请联系开发团队*