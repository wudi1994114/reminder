# 备忘鸡拼豆 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development to implement task-by-task.

**Goal:** 奔跑鸭新增本地拼豆制作，原图归档到 saas-admin 的 beiwangji 图片库。

**Architecture:** 独立纯 JavaScript 像素处理、Canvas 适配和 Vue 页面。原图通过 Reminder 已有服务端 APP_CLIENT 身份上传并登记 biz_image，不把凭据发给小程序。保留用户现有未提交修改。

**Tech Stack:** uni-app/Vue 3、Canvas 2D、Node test、Spring Boot/RestTemplate、SaaS 图片接口。

## Task 1 本地算法与色板
- [x] 创建 `reminder-uni-app/src/utils/beads/engine.mjs` 和 `palettes.mjs`，测试位于 `reminder-uni-app/tests/beads-engine.test.mjs`。
- [x] 接口 `convertToPattern({width,height,data}, {columns,maxColors,palette,excludedCodes})` 返回 `{width,height,cells,palette}`；cells 为色板下标，透明格 -1。长边限制 128，短边保持比例。
- [x] 实现区域采样、感知色彩匹配、限色、排除、计数与编辑；先测试透明度、比例、限色和统计，再实现。
- [x] 色板只采用许可明确的数据，保留来源与声明，不复制 Xprite GPL 实现。
- [x] 运行 `node --test reminder-uni-app/tests/beads-engine.test.mjs`。

## Task 2 原图归档
- [x] 核对线上 beiwangji 应用、用户与图片菜单，幂等补齐，不轮换现有凭据。
- [x] 为 Reminder 新增 `POST /api/files/bead-original`，认证用户才能上传；固定归档目录，保存原字节，不覆盖头像上传。
- [x] 上传存储后调用 SaaS 图片登记 API，使原图在 beiwangji 图片列表可见；上传失败不声称成功。
- [x] 先扩展 StorageService 定向测试，验证身份作用域、图片入库、失败处理；运行本次相关 Maven 测试。

## Task 3 小程序页面与 Canvas
- [x] 新增 `pages/beads/index.vue`、`utils/beads/canvas.js`、`api/beads.js`；注册路由，在奔跑鸭增加入口。
- [x] 支持原图选取、尺寸/限色/品牌设置、本地转换、缩放、换色/擦除/整色替换、撤销、材料统计、色号排除、草稿与 PNG 导出。
- [x] Canvas 绘制网格，导出含坐标、色号和清单的图纸；避免逐格 Vue 节点。大图片先限幅解码，输出尺寸受限。
- [x] 原图单独上传、独立展示上传状态和重试；异步任务防止旧图片结果覆盖新图。
- [x] 运行算法测试、小程序构建、H5 构建；尽可能完成交互验收，明确真机未验证项。

## Task 4 复核与交付
- [x] 独立审查边界、故障状态、归档权限和图纸统计一致性，修复发现。
- [x] 更新部署说明，分别记录源码验证、构建、线上账号与原图实测、小程序发布状态。
