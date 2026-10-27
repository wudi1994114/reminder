# Daily Records Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development for the backend unit and requesting-code-review for independent review; integrate and verify in this session. Steps use checkbox syntax for tracking.

**Goal:** 在日历中提供按账号保存的个人身高、体重和备注记录。

**Architecture:** 独立每日记录实体、仓库、服务和控制器；前端拆分 API、可独立测试的记录会话及 DailyRecordPanel。日历仅负责传入日期/月和显示有记录标记。当前工作区有未提交的音乐、提醒及训练改动，保留它们并仅编辑本任务文件。

**Tech Stack:** Java 17 / Spring Boot 2.7 / JPA / PostgreSQL，Vue 3 / uni-app，JUnit / H2 / Node test。

## Task 1: Backend and migration

- [x] 新增 DailyRecordServiceIntegrationTest 和 DailyRecordControllerTest，先运行 `mvn -q -Dtest='DailyRecord*Test' test` 观察新增功能缺失失败。
- [x] 新增 common/model/DailyRecord.java、core/dto/DailyRecordRequest.java、core/repository/DailyRecordRepository.java、core/service/DailyRecordService.java、core/controller/DailyRecordController.java，实现设计中的四个接口。
- [x] 唯一约束、记录 ID 和版本保护阻止重复记录/静默覆盖（含删除再建）；每次查询都限定认证用户；最新身高不读取未来数据；通用请求日志隐藏本接口正文。
- [x] 更新首次 schema.sql 并增加 deploy/migrations/20261027-daily-records.sql，使用显式 reminder schema、事务、无破坏性的建表 SQL。
- [x] 重新执行新增测试，然后完整 `mvn -q test`。

## Task 2: Frontend session and API

- [x] 新增 tests/daily-records.test.mjs，执行 `node --test tests/daily-records.test.mjs` 验证缺失模块失败。
- [x] 新增 utils/daily-records.mjs：本地日期键、数值验证、表单转换、草稿读写、异步记录会话；通过依赖注入测试真实逻辑。
- [x] 新增 api/dailyRecords.js，沿用 api/http.js，请求路径为 `/daily-records`。
- [x] 验证日期切换、账号切换、保存期间修改、请求乱序、保存/删除失败、409 冲突以及草稿保留。

## Task 3: Calendar interface

- [x] 新增 components/DailyRecordPanel.vue：摘要、可选数字输入、备注编辑、草稿恢复提示、错误重试、保存和确认删除；以原生日历日期为上下文。
- [x] 修改 pages/calendar/calendar.vue，插入面板、传入日期和月份、有记录日期标记；修正这一路径上已有的只读 computed 赋值，使切换日期和退出登录可靠。
- [x] 不修改 pages.json 或其他已脏页面；新面板就地编辑，不需要新路由。
- [x] 运行前端相关测试和既有回归；`npm run build:h5`、`npm run build:mp-weixin`。

## Task 4: Review and delivery

- [x] 独立审查需求一致性和代码质量，修复重要问题后重验。
- [x] 本地浏览器检查空白、编辑、保存、历史身高和窄屏长备注；后端没有部署时用仅本地的测试夹具，不冒称生产结果。
- [x] 在 evidence 文档记录命令、结果和限制；部署手册增加每日记录增量迁移说明。
- [x] 检查 diff 仅包含任务变更，报告本地完成情况及未执行的发布/真机环节。
