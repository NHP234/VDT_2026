# Nhật Ký Thay Đổi

File này ghi lại các thay đổi đã hoàn thành bằng ngôn ngữ dễ đọc. Nó không thay
thế Git history; nó giải thích tiến độ dự án, bằng chứng implementation và các
việc phát sinh.

## Format Entry - Mẫu Ghi Chép

```text
## YYYY-MM-DD

### Tóm Tắt

- Planned: mô tả ngắn. Requirement: FR-xx/NFR-xx. Verification: command hoặc manual check.
- Unplanned: mô tả ngắn. Reason: vì sao cần làm. Requirement hoặc maintenance link.

### Rủi Ro Hoặc Follow-up

- Việc vẫn cần chú ý.
```

## 2026-06-22

### Tóm Tắt

- Unplanned: chuyển toàn bộ Markdown trong `docs/` sang tiếng Việt để người
  phụ trách dự án đọc và theo dõi dễ hơn, đồng thời giữ nguyên các technical
  anchors như requirement IDs, endpoint paths, enum values, Kafka topics,
  commands và file paths. Reason: cải thiện khả năng quản lý dự án và bàn giao
  giữa các agent. Requirement hoặc maintenance link: documentation governance,
  `docs/agent-collaboration.md`.
- Unplanned: làm rõ requirement sau khi review scope: `Message` bao gồm
  Facebook Messenger inbox, Facebook Page comment và email; assignment là
  peer-to-peer giữa các agent; và inbound message mới trên `PENDING` hoặc
  `RESOLVED` tự động chuyển conversation về `OPEN`. Reason: tránh
  backend/frontend agents diễn giải khác nhau trước khi implement domain và UI.
  Requirement hoặc maintenance link: `FR-03`, `FR-04`, `FR-05`, `FR-08`.

### Rủi Ro Hoặc Follow-up

- `README.md` và `AGENTS.md` chưa được dịch vì yêu cầu hiện tại chỉ áp dụng cho
  thư mục `docs/`.

## 2026-06-18

### Tóm Tắt

- Planned: thêm documentation scaffold cho project management, progress
  tracking, backend/frontend structure, monitoring, traceability, ADRs và
  cross-agent collaboration. Requirement: project governance cho `NFR-01`,
  `NFR-05` và documentation deliverables. Verification: file review.
- Planned: thêm repository setup skeleton với `backend/`, `frontend/`,
  `infra/`, `scripts/`, `.github/workflows/`, `.env.example`,
  `docker-compose.yml`, root check scripts, Git ignore/editor config, backend
  event-contract documentation và Gemini frontend setup prompt.
  Requirement: `FR-07`, `FR-09`, `FR-10`, `FR-12`, `NFR-01`, `NFR-04`,
  `NFR-05`. Verification: `docker compose --env-file .env.example config` và
  `.\scripts\check.ps1` passed.
- Planned: validate local Docker dependency stack. Đổi Kafka từ tag
  `bitnami/kafka:3.7` không tồn tại sang `apache/kafka:3.7.0`, chỉnh Kafka
  environment variables và bỏ Kafka log volume sau khi official image không ghi
  được vào mounted directory. Requirement: `FR-09`, `NFR-01`. Verification:
  tất cả Compose services healthy; PostgreSQL `pg_isready`, Redis `PING`,
  Kafka topic listing và Mailpit HTTP check passed.
- Planned: scaffold `backend/inbox-service` và `backend/channel-service` từ
  Spring Initializr với Java 21, Spring Boot 3.5.15, Maven Wrapper, Actuator,
  Security, Validation, Web, Kafka và service-specific data dependencies. Thêm
  package placeholders, base `application.yml`, health endpoint security
  allowance và lightweight smoke tests. Requirement: `FR-01` đến `FR-11`,
  `NFR-01`, `NFR-05`. Verification: `.\scripts\check.ps1` passed và chạy cả
  hai backend Maven Wrapper test suites.
- Unplanned: regenerate Maven Wrapper ở binary mode vì Windows wrapper
  `only-script` từ Initializr bị lỗi trong PowerShell runner này. Reason: đảm
  bảo local test execution ổn định. Requirement: `NFR-01`, `NFR-05`.
- Planned: cập nhật CI để install Java 21 trước backend checks và chỉnh
  `.gitignore` của backend services để binary Maven Wrapper jars không bị ẩn
  khỏi Git tracking sau này. Requirement: `NFR-01`, `NFR-05`. Verification:
  `.\scripts\check.ps1` passed locally.

### Rủi Ro Hoặc Follow-up

- Backend service scaffolds đã tồn tại, nhưng business domain, migrations, APIs,
  adapters và real integration tests vẫn còn pending.
- Root check scripts hiện skip frontend checks cho đến khi React project được
  scaffold.
- Full app run commands và deterministic demo commands vẫn pending.
- Kafka local data hiện nằm trong container filesystem. Điều này chấp nhận được
  cho MVP vì PostgreSQL là durable source of truth và Kafka chỉ là local async
  transport.
- Các agent sau phải giữ changelog này đồng bộ với progress checklist và
  traceability table.
