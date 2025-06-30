# Nacos配置中心集成指南

## 概述

本项目已集成Nacos作为配置中心，支持动态配置管理和配置热更新。通过Nacos，可以集中管理不同环境的配置，提高配置的安全性和可维护性。

## 架构说明

### 配置层次结构
```
bootstrap.yaml (本地) 
    ↓
Nacos配置中心
    ├── reminder-common.yaml (公共配置)
    ├── reminder-dev.yaml (开发环境)
    ├── reminder-prod.yaml (生产环境)
    └── reminder-test.yaml (测试环境)
        ↓
application.yaml (本地默认配置)
```

### 配置优先级
1. Nacos配置中心 (最高优先级)
2. bootstrap.yaml
3. application.yaml
4. 环境变量
5. 系统属性

## 快速开始

### 1. 启动Nacos服务器

#### 使用Docker启动Nacos
```bash
docker run --name nacos-standalone -e MODE=standalone -e JVM_XMS=512m -e JVM_XMX=512m -e JVM_XMN=256m -p 8848:8848 -d nacos/nacos-server:latest
```

#### 访问Nacos控制台
- URL: http://localhost:8848/nacos
- 用户名: nacos
- 密码: nacos

### 2. 配置Nacos连接

在 `bootstrap.yaml` 中配置Nacos连接信息：

```yaml
spring:
  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        file-extension: yaml
        namespace: your-namespace-id
        group: DEFAULT_GROUP
        username: nacos
        password: nacos
```

### 3. 在Nacos中创建配置

#### 公共配置 (reminder-common.yaml)
- Data ID: `reminder-common.yaml`
- Group: `DEFAULT_GROUP`
- 配置格式: `YAML`
- 内容: 参考 `nacos-config-examples/reminder-common.yaml`

#### 环境特定配置
- 开发环境: `reminder-dev.yaml`
- 生产环境: `reminder-prod.yaml`
- 测试环境: `reminder-test.yaml`

## 配置管理

### 动态配置类
- `DynamicConfigProperties`: 管理可动态刷新的配置属性
- `NacosConfig`: Nacos连接配置管理
- `ConfigRefreshListener`: 配置变更监听器

### 配置刷新
配置支持自动刷新和手动刷新：

#### 自动刷新
当Nacos中的配置发生变更时，应用会自动接收变更通知并刷新配置。

#### 手动刷新
通过管理接口手动触发配置刷新：
```bash
POST /api/admin/config/refresh
```

### 配置管理接口

#### 获取配置状态
```bash
GET /api/admin/config/status
```

#### 验证配置有效性
```bash
GET /api/admin/config/validate
```

#### 获取Nacos连接信息
```bash
GET /api/admin/config/nacos-info
```

#### 健康检查
```bash
GET /api/admin/config/health
```

## 环境配置

### 开发环境
```bash
export SPRING_PROFILES_ACTIVE=dev
export NACOS_SERVER_ADDR=localhost:8848
export NACOS_NAMESPACE=dev
```

### 生产环境
```bash
export SPRING_PROFILES_ACTIVE=prod
export NACOS_SERVER_ADDR=prod-nacos-server:8848
export NACOS_NAMESPACE=prod
export NACOS_USERNAME=prod_user
export NACOS_PASSWORD=prod_password
```

## 安全配置

### 1. 启用Nacos认证
在生产环境中，建议启用Nacos的认证功能：

```properties
# Nacos服务器配置
nacos.core.auth.enabled=true
nacos.core.auth.server.identity.key=your-identity-key
nacos.core.auth.server.identity.value=your-identity-value
```

### 2. 配置加密
对于敏感配置，建议使用Nacos的配置加密功能或外部密钥管理系统。

### 3. 网络安全
- 使用VPC内网访问Nacos
- 配置防火墙规则
- 使用HTTPS访问

## 故障排除

### 常见问题

#### 1. 无法连接到Nacos服务器
- 检查网络连接
- 验证服务器地址和端口
- 检查防火墙设置

#### 2. 配置未生效
- 检查配置文件名和格式
- 验证命名空间和分组设置
- 查看应用日志

#### 3. 配置刷新失败
- 检查 `@RefreshScope` 注解
- 验证配置监听器
- 查看配置变更日志

### 日志配置
启用详细日志以便排查问题：

```yaml
logging:
  level:
    com.alibaba.nacos: DEBUG
    org.springframework.cloud.context: DEBUG
    com.core.reminder.config: DEBUG
```

## 最佳实践

### 1. 配置分层
- 公共配置放在 `reminder-common.yaml`
- 环境特定配置放在对应的环境文件中
- 敏感配置单独管理

### 2. 配置验证
- 使用 `@ConfigurationProperties` 进行配置绑定
- 实现配置验证逻辑
- 定期检查配置有效性

### 3. 版本管理
- 为重要配置创建备份
- 使用Nacos的配置历史功能
- 建立配置变更审批流程

### 4. 监控告警
- 监控配置刷新事件
- 设置配置验证失败告警
- 监控Nacos服务可用性

## 迁移指南

### 从本地配置迁移到Nacos

1. **备份现有配置**
   ```bash
   cp application.yaml application.yaml.backup
   ```

2. **创建Nacos配置**
   - 在Nacos控制台创建对应的配置文件
   - 复制配置内容到Nacos

3. **更新本地配置**
   - 移除敏感配置
   - 添加环境变量占位符

4. **测试验证**
   - 启动应用验证配置加载
   - 测试配置动态刷新功能

## 参考资料

- [Nacos官方文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)
- [Spring Cloud Alibaba文档](https://spring-cloud-alibaba-group.github.io/github-pages/hoxton/zh-cn/index.html)
- [配置中心最佳实践](https://nacos.io/zh-cn/docs/best-practice.html)
