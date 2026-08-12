# Gmail 邮件提供商退役实施计划

> 按测试先行方式执行；只修改后端和本计划文档，不触碰现有前端工作区改动。

## 任务一：建立退役回归约束

**文件：**

- 新增：`reminder-backend/src/test/java/com/task/reminder/sender/LegacyEmailProviderRemovalTest.java`

1. 测试断言旧发送器源码不存在。
2. 测试断言 POM 不再声明 Google Auth/API/Gmail/OAuth 依赖。
3. 测试断言主配置不再声明旧提供商配置，工厂缺省值为 `none`。
4. 运行 `mvn -q -Dtest=LegacyEmailProviderRemovalTest test`，确认修改前失败。

## 任务二：删除 Gmail 实现和配置

**文件：**

- 删除：`reminder-backend/src/main/java/com/task/reminder/sender/GmailSender.java`
- 修改：`reminder-backend/pom.xml`
- 修改：`reminder-backend/src/main/resources/application.yaml`
- 修改：`reminder-backend/src/main/java/com/task/reminder/sender/EmailSenderFactory.java`
- 修改：`reminder-backend/src/test/java/EmailSenderTest.java`

1. 删除旧发送器类。
2. 删除仅供旧发送器使用的 Maven 属性和依赖。
3. 删除旧凭据占位配置。
4. 把工厂缺省提供商设置为 `none`，清理误导性测试注释。

## 任务三：验证和审查

1. 运行 `mvn -q -Dtest=LegacyEmailProviderRemovalTest test`。
2. 运行 `mvn -q test`。
3. 运行 `mvn -q -DskipTests package`。
4. 搜索源码、配置和 POM，确认没有旧实现或 Google 邮件 API 残留。
5. 检查 Git 差异，确保没有混入用户的前端改动。
