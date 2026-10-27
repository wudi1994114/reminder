# 每日记录与趋势发布证据（2026-10-27）

## 已完成

- 功能提交 `b2bee71` 已推送至 `origin/codex/cloudbase-exit-monolith`。仅提交日历、每日记录、趋势和对应文档/测试；发布前后的原有本地修改补丁逐字节一致。
- 小程序来自隔离的 Git index 导出，沿用稳定 package/pages/vite 配置，不包含未提交的音乐实验与训练页面修改。
- 前端隔离候选 112 项测试通过，0失败；生产环境微信构建成功，确认 API 指向 `https://reminder-api.wwmty.com/api`。
- 后端以实际生产源码 `017654f-beads-20261023` 为基线，叠加16个本功能文件，190个其他生产文件哈希不变，保留 `beiwangji` 存储配置、偏好回退与部署修复。
- 后端30个测试套件共107项，0失败、0错误、11跳过；Java17打包成功。JAR SHA256：`3348879aeed3bd561f9a378d1aab8ca0f6202272cadc8594965eb96313dd9e1a`。

## 生产后端

- 发布目录 `/opt/saas-app/reminder/releases/20261027-daily-records-1`，保留 JAR、源码归档、源码哈希清单、测试结果及组合基线 provenance。
- 镜像 `127.0.0.1:3000/admin/reminder-backend:20261027.daily.1`；镜像ID `sha256:405bd76f31e4ad2ed4360219e2be47e85997d18bedc6e83b879d98ea86930ab2`。
- 仓库摘要 `sha256:2237f83859ee52a110f3eaefaa340ca8945d45d505df7221c235627b7c8342f1`；revision标签 `b2bee71-on-017654f-beads-20261023`。
- 升级前完成 Reminder schema 全量 custom-format 备份并校验可列出归档索引；远端文件 `schema-backup.dump`，105764 bytes，权限600，所在目录权限700。没有导出其他业务 schema。
- 仅执行增量 `20261027-daily-records.sql`，新表初始0行；沿用现网运行环境、Compose与持久回滚目录，只重建 Reminder。
- 新审计状态 `/opt/saas-app/reminder/deploy-state/releases/20261027144846-1880548`；先前状态仍保留，可回滚旧镜像，新表为兼容性增量无需删除。
- 容器健康 `healthy`；公网 `/actuator/health` 返回 `UP`。其余15个容器的ID与启动时间不变。
- 专用探针账号经公网HTTPS登录成功；未登录每日记录请求401；创建/读取两日记录成功；空身高沿用上一测量且不复制；体重与身高的30/90/365天趋势全部通过；过期写入409。
- 按探针记录ID与版本精确清理两条测试记录，最终每日记录表0行，原有账号10个。日志中无探针备注内容。

## 微信小程序

- AppID `wx597f8aaca581205f`，版本 `1.0.20261027.1`。
- 2026-10-27 开发者工具官方CLI `upload` 返回成功，退出码0；上传包1041584 bytes。
- 更新说明：日历新增体重、身高与备注记录；点击数值弹窗查看变化趋势；优化日历空间布局。
- 上传回执与日志位于 `/tmp/reminder-daily-records-release-20261027/wechat-upload-info.json`、`wechat-upload.log`。本轮未进行微信真机操作验收。
- **尚未提交审核/公开发布。** 微信公众平台现有Chrome标签页无法获取访问授权，工具明确返回 site-safety policy block，并禁止使用间接执行、其他浏览器或原始浏览器命令绕过。没有进行绕过尝试。
- 用户可在微信公众平台「版本管理 → 开发版本」选择 `1.0.20261027.1` 点击「提交审核」。CLI上传成功不能代替审核提交状态。
