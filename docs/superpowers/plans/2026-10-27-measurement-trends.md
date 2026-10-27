# Measurement Trends Implementation Plan

> **For agentic workers:** Use subagent-driven-development for the isolated backend endpoint, test-driven-development for trend processing, and requesting-code-review for independent review.

**Goal:** Keep the calendar compact, inherit the previous height without a date label, and show weight/height history on demand.

**Architecture:** Add a scoped projection-only measurement history endpoint; implement pure trend geometry/loading logic and a reusable canvas component. DailyRecordPanel only routes metric clicks, carries forward the existing display height, and keeps the edit action separate.

**Tech Stack:** Existing Spring Boot/JPA, Vue3/uni-app canvas, JUnit/H2, Node test.

- [x] Backend: add trend DTO and two repository projections, service range validation and static /trend controller route. First add failing controller/service tests for identity, date window, null measurements and bounds; implement and run focused tests.
- [x] Frontend data: add tests/daily-record-trend.test.mjs and utils/daily-record-trend.mjs for real measurements, proportional date coordinates, summary/selection and stale-response guards. Run new tests red then green.
- [x] UI: add MetricTrend.vue, add dailyRecordsApi.getTrend, route metric clicks to a responsive chart dialog with a close button, remove height date text, keep the editor action. Support30/90/365-day windows and loading/empty/error/single-point states.
- [x] Verify: run frontend regressions, backend tests, H5/WeChat builds; use a local fixture for canvas and narrow-screen checks. Request independent review, fix findings, document actual results and screenshots without deploying.
