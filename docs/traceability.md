# Truy Vết Requirement

Mọi implementation task, API, table, event, UI control và automated test phải
tham chiếu ít nhất một locked requirement từ `docs/requirement.md`.

Giá trị trạng thái:

- `Planned`
- `Implemented`
- `Verified`
- `Deferred`
- `Blocked`

## Functional Requirements - Yêu Cầu Chức Năng

| Requirement | File triển khai | Tests | Bằng chứng demo | Trạng thái | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `FR-01` Agent authentication | `backend/inbox-service/pom.xml`, `backend/inbox-service/src/main/java/com/vdt2026/omnicare/inbox/config/SecurityConfig.java` | `backend/inbox-service/src/test/java/com/vdt2026/omnicare/inbox/InboxServiceApplicationTests.java`; `.\scripts\check.ps1` | TBD | Planned | Service scaffold và protected-by-default security only; real login/logout còn lại. |
| `FR-02` Unified conversation inbox | TBD | TBD | TBD | Planned | Backend pagination bắt buộc. |
| `FR-03` Conversation detail | TBD | TBD | TBD | Planned | Chỉ safe render plain text. |
| `FR-04` Conversation status | TBD | TBD | TBD | Planned | Reopen resolved conversations khi có inbound message. |
| `FR-05` Manual assignment | TBD | TBD | TBD | Planned | Chỉ seeded agents. |
| `FR-06` Plain-text reply | TBD | TBD | TBD | Planned | Queued, sent, failed, retry. |
| `FR-07` Email integration | `docker-compose.yml`, `.env.example`, `infra/README.md`, `backend/channel-service/pom.xml` | Compose config validation; Mailpit HTTP check; `.\scripts\check.ps1` | TBD | Implemented | Mới có Mailpit/local SMTP và channel-service dependencies; adapter implementation còn lại. |
| `FR-08` Facebook integration | `backend/channel-service/pom.xml`, `backend/channel-service/src/main/java/com/vdt2026/omnicare/channel/facebook/package-info.java` | `.\scripts\check.ps1` | TBD | Planned | Mới có channel-service scaffold; webhook và adapter implementation còn lại. |
| `FR-09` Asynchronous processing and reliability | `docker-compose.yml`, `backend/event-contracts/README.md`, `backend/inbox-service/pom.xml`, `backend/channel-service/pom.xml` | Compose config validation; Kafka topic-list probe; `.\scripts\check.ps1` | TBD | Implemented | Mới có Kafka infrastructure, dependencies và initial topic contracts; producers/consumers còn lại. |
| `FR-10` Redis deduplication | `docker-compose.yml`, `.env.example`, `backend/channel-service/pom.xml`, `backend/channel-service/src/main/java/com/vdt2026/omnicare/channel/deduplication/package-info.java` | Compose config validation; Redis `PING`; `.\scripts\check.ps1` | TBD | Implemented | Mới có Redis infrastructure và dependency; deduplication code còn lại. |
| `FR-11` Audit and operational endpoints | `docs/monitoring-and-operations.md`, `backend/inbox-service/src/main/resources/application.yml`, `backend/channel-service/src/main/resources/application.yml` | `.\scripts\check.ps1` | TBD | Planned | Actuator dependency/config đã có; audit, metrics counters và HTTP health verification còn lại. |
| `FR-12` Deterministic demo mode | `README.md`, `scripts/dev-up.ps1`, `scripts/dev-down.ps1` | Compose config validation; dependency runtime probes | TBD | Implemented | Mới có local dependency startup; demo fixtures/scripts còn lại. |

## Non-Functional Requirements - Yêu Cầu Phi Chức Năng

| Requirement | File triển khai | Tests | Bằng chứng demo | Trạng thái | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `NFR-01` Local reproducibility | `docker-compose.yml`, `.env.example`, `README.md`, `scripts/dev-up.ps1`, `scripts/dev-down.ps1`, `scripts/check.ps1`, `.github/workflows/ci.yml`, `backend/inbox-service/mvnw.cmd`, `backend/channel-service/mvnw.cmd` | Compose config validation; dependency runtime probes; `.\scripts\check.ps1` | TBD | Implemented | Backend setup/check commands đã có; frontend và demo commands còn lại. |
| `NFR-02` Performance target | TBD | TBD | TBD | Planned | Chỉ là target ở demo scale. |
| `NFR-03` Reliability | TBD | TBD | TBD | Planned | Duplicates, restarts, failures. |
| `NFR-04` Security | `.env.example`, `.gitignore`, `docs/agent-collaboration.md` | TBD | TBD | Planned | Đã có secret placeholders và ignore rules; application security còn lại. |
| `NFR-05` Tests | `scripts/check.ps1`, `.github/workflows/ci.yml`, `docs/templates/gemini-frontend-setup-prompt.md`, `backend/inbox-service/src/test/java/com/vdt2026/omnicare/inbox/InboxServiceApplicationTests.java`, `backend/channel-service/src/test/java/com/vdt2026/omnicare/channel/ChannelServiceApplicationTests.java` | Compose config validation; `.\scripts\check.ps1` | TBD | Planned | Đã có backend smoke tests; meaningful behavior tests còn lại. |

## Quy Tắc Cập Nhật

- Thay `TBD` bằng file cụ thể ngay khi implementation bắt đầu.
- Link tests bằng path và command khi chúng tồn tại.
- Link demo evidence tới screenshots, logs hoặc scripts dùng dữ liệu không nhạy cảm.
- Không mark `Verified` nếu chưa ghi verification evidence.
