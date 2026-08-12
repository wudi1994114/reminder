# Gmail 邮件提供商退役设计

## 目标

从 `reminder-backend` 中完整移除 Gmail 专用发送能力，避免后端继续携带 Google OAuth 与 Gmail API 的代码、依赖和配置。

## 范围

- 删除 `GmailSender` Spring Bean。
- 删除仅被该发送器使用的 Google Auth、Google API Client、Gmail API 与 OAuth Jetty 依赖。
- 删除 `application.yaml` 中的 Gmail 凭据占位配置。
- 将邮件工厂的缺省提供商改为 `none`，不再隐式选择已退役的提供商。
- 清理测试代码里误导性的 Gmail 注释。

## 保留项

- 保留 `EmailSender` 通用接口与 `EmailSenderFactory`。
- 保留腾讯云邮件发送实现及其所需的通用邮件依赖。
- 保留 `EMAIL_PROVIDER` 部署配置；无提供商时继续安全地返回发送失败，不影响应用启动。

## 验证

- 增加结构回归测试，确保 Gmail 发送器、Google 邮件 API 依赖和 Gmail 配置不会重新出现。
- 运行定向回归测试、完整 Maven 测试及打包。
- 全仓搜索确认运行时代码和配置中无 Gmail/Google 邮件 API 残留。
