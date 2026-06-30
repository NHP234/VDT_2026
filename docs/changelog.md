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

## 2026-06-30

### Tóm Tắt

- Planned: thêm Redis-backed deduplication cho Facebook inbound events trong
  Channel service trước khi publish Kafka. `InboundEventDispatchService` chỉ
  publish khi `InboundEventDeduplicator` accept event; Redis adapter dùng key
  ngắn hạn theo channel, provider account, source type và external message ID.
  Nếu Redis lỗi, Channel fail-open và dựa vào PostgreSQL idempotency ở Inbox để
  không mất event. Requirement: `FR-08`, `FR-10`, `NFR-03`. Verification:
  `backend/channel-service` tests pass 32/32, gồm duplicate simulator event,
  dispatch service và Redis dedup adapter tests; runtime smoke với Docker pass:
  gửi trùng `mid.dedup-smoke-20260630162145-84e1a7d2` trả `published=true` ở
  lần đầu, `published=false` ở lần hai, Kafka chỉ có 1 record.

### Rủi Ro Hoặc Follow-up

- Real Meta webhook POST hiện mới verify signature và chưa parse/publish payload
  thật, nên dedup path hiện áp dụng cho simulator/normalized Facebook inbound
  flow. Khi parse real webhook được thêm vào, dùng cùng dispatch service này.

## 2026-06-30

### Tóm Tắt

- Planned: thêm script smoke cross-service `scripts/smoke-cross-service.ps1`
  cho deterministic demo path: Channel Facebook simulator publish inbound
  event, Inbox consume/persist, agent login, reply queued, Channel consume reply
  request và Inbox nhận delivery result `SENT`. Requirement: `FR-12`, `NFR-05`.
  Verification: chạy `.\scripts\smoke-cross-service.ps1` pass với conversation
  `936dc6b8-aa20-4473-b267-9e13a39dbfbb`, outbound message
  `b51652c2-eeb2-42dd-b5be-cbece56ca60c` chuyển sang `SENT`.
- Planned: manual Kafka DLT smoke cho bounded retry vừa thêm. Publish malformed
  record vào `inbox.message-received.v1`, sau retry record xuất hiện trong
  `inbox.message-received.v1.dlq` với token `bad-smoke-20260630155836`.
  Requirement: `FR-09`, `NFR-03`. Verification: `kafka-console-consumer`
  đọc được record trong DLT topic.

### Rủi Ro Hoặc Follow-up

- Channel health endpoint hiện `UP` nhưng có thể mất khoảng 10 giây do
  `MailHealthIndicator`; nên tối ưu health/readiness trước demo nếu cần check
  nhanh.

## 2026-06-30

### Tóm Tắt

- Planned: cấu hình bounded retry và dead-letter handling cho Kafka consumers.
  Inbox service và Channel service dùng `DefaultErrorHandler` với số lần thử
  hữu hạn, fixed backoff cấu hình qua `.env.example`, rồi publish record lỗi
  sang topic nguồn cộng suffix `.dlq` cùng partition. Requirement: `FR-09`,
  `NFR-03`. Verification: thêm unit tests cho retry backoff và DLT topic
  resolver ở cả hai service.

### Rủi Ro Hoặc Follow-up

- Cần manual Kafka smoke khi Docker daemon sẵn sàng để xác nhận record lỗi thật
  đi vào topic `.dlq` sau khi retry hết lượt.

## 2026-06-29

### Tóm Tắt

- Planned: Thiết lập Frontend Workspace trong `frontend/`. `FR-01` đến `FR-06`, `NFR-01`, `NFR-04`, `NFR-05`.
  - Khởi tạo thành công React TypeScript Vite app bằng `create-vite`.
  - Thiết lập và định nghĩa đầy đủ các kiểu dữ liệu của domain tại `src/types/index.ts`.
  - Cài đặt cấu hình API Client (`httpClient.ts`, `authApi.ts`, `conversationsApi.ts`, `agentsApi.ts`) hỗ trợ Bearer Token và Problem Details.
  - Triển khai theme sáng cho workspace vận hành, dùng bố cục dày thông tin và font monospace cho các định danh kỹ thuật.
  - Triển khai màn hình Login và Unified Inbox với bố cục 3 cột chuyên dụng (Bộ lọc & danh sách, timeline trò chuyện & soạn thảo phản hồi, bảng thuộc tính chi tiết & lịch sử hoạt động).
  - Thiết lập môi trường kiểm thử Vitest và viết 12 test cases bao quát mọi hành vi (Login, phân trang danh sách, bộ lọc kênh/trạng thái, gán agent, gửi phản hồi, hiển thị tin nhắn lỗi và nút thử lại).
  - Verification: `npm run lint`, `npm test`, `npm run build` và `.\scripts\check.ps1` đều pass; frontend có 12/12 test pass.
- Unplanned: thêm cấu hình CORS cho Inbox service với `FRONTEND_ALLOWED_ORIGINS`
  để Vite dev server (`http://localhost:5173`) gọi được backend API.
  Reason: review frontend phát hiện browser preflight tới `POST /api/v1/auth/login`
  đang bị `403 Invalid CORS request`. Requirement hoặc maintenance link:
  `FR-01`, `NFR-04`. Verification: manual CORS preflight trên port `18080`
  trả `200` và `Access-Control-Allow-Origin: http://localhost:5173`.
- Planned: thêm Facebook simulator endpoint và checked-in fixtures cho Channel
  service. `POST /simulators/facebook/events` nhận Messenger message hoặc Page
  comment fixture, map sang normalized `inbox.message-received.v1` envelope,
  giữ Messenger conversation theo Page + sender và comment conversation theo
  Page + post + root comment. Requirement: `FR-08`, `FR-12`, `FR-09`.
  Verification: `backend/channel-service` tests passed với 5/5 tests, gồm
  normalizer fixture tests và controller API tests; manual HTTP smoke trên port
  `18081` trả normalized Messenger và comment payload đúng topic/source type/
  external conversation ID.
- Planned: triển khai Meta webhook verification cho Channel service.
  `GET /webhooks/facebook` kiểm tra `hub.mode=subscribe`, `hub.verify_token`
  khớp `FACEBOOK_VERIFY_TOKEN` và trả plain-text `hub.challenge`; token sai hoặc
  cấu hình thiếu trả Problem Details `403`. Requirement: `FR-08`, `NFR-04`.
  Verification: `backend/channel-service` tests passed với 11/11 tests, gồm
  service unit tests và controller contract tests; manual HTTP smoke trên port
  `18081` xác nhận valid token trả `challenge-42` và wrong token trả `403`.
- Planned: validate Meta webhook signatures trong real-provider mode bằng
  `X-Hub-Signature-256` (`sha256=<hmac>`), HMAC-SHA256 trên raw body với
  `FACEBOOK_APP_SECRET`. Simulator mode bỏ qua signature để local fixtures vẫn
  chạy nhẹ. Requirement: `FR-08`, `NFR-04`. Verification:
  `backend/channel-service` tests passed với 17/17 tests, gồm valid signature,
  invalid signature, missing app secret và unsigned real-mode webhook rejection;
  manual HTTP smoke trên port `18081` xác nhận signed real-mode POST trả `202`
  còn unsigned POST trả `403`.
- Planned: publish normalized inbound events từ Channel service lên Kafka topic
  `inbox.message-received.v1`. Facebook simulator sau khi normalize sẽ gọi
  application port `InboundEventPublisher`; Kafka adapter dùng external
  conversation ID làm record key để giữ ordering theo conversation; Kafka JSON
  serializer giữ timestamp dạng ISO 8601 UTC. Requirement: `FR-09`, `FR-08`,
  `FR-12`. Verification: `backend/channel-service` tests passed với 19/19 tests,
  gồm test Kafka publisher topic/key/value và controller test xác nhận simulator
  gọi publisher; manual Kafka smoke consume được event mới từ topic với
  `occurredAt` dạng `"2026-06-30T02:25:00Z"`.
- Planned: consume inbound events idempotently trong Inbox service. Thêm
  `InboundMessageIngestionService` để xử lý `inbox.message-received.v1`, tạo
  customer/channel identity/conversation/message khi cần, reopen conversation
  `PENDING` hoặc `RESOLVED`, ghi `MESSAGE_RECEIVED` activity, và lưu
  `processed_events` để bỏ qua redelivery. Kafka listener đọc JSON string từ
  topic rồi delegate vào application service; external message ID vẫn được
  kiểm tra trước khi ghi để chống duplicate từ provider. Requirement: `FR-09`,
  `NFR-03`, `FR-02`, `FR-03`, `FR-11`. Verification: `backend/inbox-service`
  tests passed với 16/16 tests, gồm test tạo mới inbound message, reopen
  conversation cũ, duplicate event và duplicate external message; manual Kafka
  smoke với consumer group mới publish event vào `inbox.message-received.v1` và
  xác nhận PostgreSQL có message `mid.local.inbox.consume.*`.
- Planned: publish outbound reply requests từ Inbox service. Thêm
  `OutboxPublishingService`, Kafka publisher adapter và scheduled publisher để
  lấy `outbox_events` trạng thái `PENDING`, publish JSON envelope ra topic đang
  lưu trong `event_type` như `inbox.reply-requested.v1`, rồi mark row
  `PUBLISHED`. Event key là outbound message ID để Channel service có thể xử lý
  theo message. Requirement: `FR-09`, `FR-06`, `NFR-03`. Verification:
  `backend/inbox-service` tests passed với 18/18 tests, gồm test envelope
  `reply-requested`, topic/key/value và status transition; manual Kafka smoke
  insert một outbox row pending, scheduler publish vào `inbox.reply-requested.v1`
  và PostgreSQL row chuyển sang `PUBLISHED`.
- Planned: publish outbound delivery results từ Channel service. Thêm consumer
  cho `inbox.reply-requested.v1` và `inbox.reply-retry-requested.v1`, mock
  provider delivery service, Kafka publisher adapter và delivery result events
  `channel.reply-delivery-succeeded.v1` / `channel.reply-delivery-failed.v1`.
  Nội dung reply bình thường publish success; nội dung chứa `[fail]` publish
  failed để demo failure/retry path không cần Meta credentials thật.
  Requirement: `FR-09`, `FR-06`, `FR-08`, `FR-12`. Verification:
  `backend/channel-service` tests passed với 22/22 tests, gồm success/failure
  delivery envelope và Kafka publisher topic/key/value; manual Kafka smoke
  produce một `reply-requested` event và consume được
  `channel.reply-delivery-succeeded.v1` chứa message ID tương ứng.
- Planned: consume outbound delivery results trong Inbox service. Thêm consumer
  cho `channel.reply-delivery-succeeded.v1` và
  `channel.reply-delivery-failed.v1`, application service cập nhật outbound
  message từ `QUEUED` sang `SENT` hoặc `FAILED`, lưu provider message ID, ghi
  `DELIVERY_STATUS_CHANGED` activity và lưu `processed_events` để bỏ qua
  duplicate result event. Requirement: `FR-06`, `FR-09`, `NFR-03`, `NFR-05`.
  Verification: `backend/inbox-service` tests passed với 22/22 tests, gồm
  success, failed, duplicate event và no-op khi status đã khớp.
- Planned: commit mốc `feat(inbox): add domain schema and authentication`
  (`1fb6b99`) để đóng phần Inbox service schema, seed data, domain policy,
  persistence foundation và backend authentication trước khi làm API kế tiếp.
  Requirement: `FR-01` đến `FR-06`, `FR-11`, `FR-12`, `NFR-04`, `NFR-05`.
  Verification: commit created.
- Planned: triển khai backend conversation list/detail API cho Inbox service.
  `GET /api/v1/conversations` hỗ trợ pagination, filter theo `channel`,
  `status`, `assignee=me|unassigned|{uuid}` và search theo customer display
  name hoặc exact channel identity. `GET /api/v1/conversations/{id}` trả
  conversation detail kèm messages và activities theo chronological order.
  Requirement: `FR-02`, `FR-03`, `NFR-05`. Verification: `backend/inbox-service`
  tests passed; manual HTTP smoke trên port `18080` passed với 2 seeded
  conversations, `channel=FACEBOOK` trả 1, `assignee=me` trả 1, exact identity
  search `tran.b@example.test` trả 1, detail trả 1 message và 1 activity;
  `.\scripts\check.ps1` passed.
- Planned: triển khai backend status và assignment mutation API cho Inbox
  service. `PATCH /api/v1/conversations/{id}/status` đổi status và ghi
  `STATUS_CHANGED`; `PATCH /api/v1/conversations/{id}/assignee` hỗ trợ
  peer-to-peer assign/reassign/unassign và ghi `ASSIGNMENT_CHANGED`.
  Requirement: `FR-04`, `FR-05`, `FR-11`, `NFR-05`. Verification:
  `backend/inbox-service` tests passed; manual HTTP smoke trên port `18080`
  passed với status đổi sang `PENDING`, assignment sang
  `minh.agent@example.test`, unassign về null và activities tăng;
  `.\scripts\check.ps1` passed.
- Planned: triển khai backend reply queue và retry API cho Inbox service.
  `POST /api/v1/conversations/{id}/replies` tạo outbound message trạng thái
  `QUEUED`, cập nhật last message, ghi `REPLY_QUEUED` activity và tạo outbox
  event. `POST /api/v1/messages/{id}/retry` chỉ cho outbound `FAILED` message,
  chuyển về `QUEUED`, ghi `DELIVERY_STATUS_CHANGED` và tạo retry outbox event.
  Requirement: `FR-06`, `FR-09`, `NFR-05`. Verification: `backend/inbox-service`
  tests passed; manual HTTP smoke trên port `18080` tạo reply `QUEUED`, giả lập
  `FAILED` trong PostgreSQL local, retry về `QUEUED` và thấy 2 outbox events
  cho message đó; `.\scripts\check.ps1` passed.
- Planned: chuẩn hóa validation/error response cho Inbox service theo
  `application/problem+json`. Security filter trả Problem Details cho lỗi
  `401/403`; global exception handler trả Problem Details cho validation,
  malformed body, missing parameter, invalid enum và not found/business rule
  errors. Requirement: `FR-01` đến `FR-06`, `NFR-04`. Verification:
  `backend/inbox-service` tests passed; manual HTTP smoke trên port `18080`
  xác nhận unauthenticated, failed login, invalid enum, blank reply và not found
  đều trả `application/problem+json` với status/title đúng.

### Rủi Ro Hoặc Follow-up

- Conversation list/detail API đã có backend, nhưng frontend Unified Inbox vẫn
  chưa scaffold nên `FR-02` và `FR-03` chưa complete ở cấp sản phẩm.

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
- Planned: thêm Inbox service Flyway migrations cho locked domain model, seed
  demo agents/baseline conversations, JPA persistence foundation cho agents,
  customers, channel identities, conversations, messages, activities,
  processed events và outbox events. Thêm domain policy tests cho status reopen
  và peer-to-peer assignment. Requirement: `FR-01` đến `FR-06`, `FR-11`,
  `FR-12`, `NFR-05`. Verification: PostgreSQL temp database migration check
  passed với 3 agents, 2 conversations, 2 messages; Inbox service non-web
  startup validated Flyway v2 và Hibernate mapping trên PostgreSQL;
  `.\scripts\check.ps1` passed.
- Planned: triển khai backend authentication foundation cho Inbox service:
  bcrypt password verification với seeded agents, stateless HMAC Bearer token,
  protected APIs, `POST /api/v1/auth/login`, `GET /api/v1/auth/me`,
  `POST /api/v1/auth/logout` và `GET /api/v1/agents`. Requirement: `FR-01`,
  `NFR-04`, `NFR-05`. Verification: `backend/inbox-service` tests passed;
  manual HTTP smoke trên port `18080` passed với health `UP`, unauth `/me`
  trả `401`, login trả Bearer token, `/me` trả `agent@example.test`,
  `/agents` trả 3 agents và logout trả `204`; `.\scripts\check.ps1` passed.

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
