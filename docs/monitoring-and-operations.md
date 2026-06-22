# Monitoring Và Operations

Tài liệu này theo dõi runtime observability, health checks và demo operations.
Nó bổ sung cho progress tracking trong `docs/progress-checklist.md`.

## Runtime Signals Bắt Buộc

Requirements: `FR-11`, `NFR-03`, `NFR-04`

Mỗi backend service nên expose:

- Spring Boot Actuator health endpoint.
- Structured logs có correlation IDs.
- Metrics tương thích Micrometer và Prometheus scraping khi metrics được đưa vào.
- Failure logs rõ ràng cho provider, Kafka, database và validation failures.

Không bao giờ log:

- Access tokens.
- Passwords.
- Mailbox credentials.
- Nội dung email nhạy cảm đầy đủ.
- PII không cần thiết.

## Counters Bắt Buộc

Tối thiểu:

| Metric | Service | Requirement |
| --- | --- | --- |
| `inbound_events_processed_total` | Channel và Inbox | `FR-11` |
| `duplicate_events_rejected_total` | Channel và Inbox | `FR-10`, `FR-11` |
| `outbound_deliveries_sent_total` | Channel | `FR-11` |
| `outbound_deliveries_failed_total` | Channel | `FR-11` |
| `outbound_retries_requested_total` | Inbox | `FR-06`, `FR-11` |

Tên metric có thể chỉnh theo convention của Spring/Micrometer, nhưng ý nghĩa
phải giữ traceable.

## Health Checks

Theo dõi các check này khi service tồn tại:

| Check | Hành vi kỳ vọng |
| --- | --- |
| Inbox service liveness | Process đang chạy và phục vụ được request cơ bản. |
| Inbox service readiness | Database và Kafka dependencies cần cho accepted work reachable. |
| Channel service liveness | Process đang chạy và phục vụ được request cơ bản. |
| Channel service readiness | Kafka, Redis, provider configuration và mail dependencies reachable theo profile. |
| PostgreSQL | Accepts connections và migrations đã chạy. |
| Kafka | Các topic bắt buộc available hoặc được local setup auto-create. |
| Redis | Accepts connections cho deduplication keys. |
| Mailpit/test mail server | Accepts SMTP và expose test inbox khi dùng. |

## Local Dependency Endpoints

| Dependency | Local endpoint |
| --- | --- |
| PostgreSQL | `localhost:5432` |
| Kafka | `localhost:29092` |
| Redis | `localhost:6379` |
| Mailpit SMTP | `localhost:1025` |
| Mailpit UI | `http://localhost:8025` |

## Checklist Demo Operations

Trước demo:

- [ ] Start local infrastructure bằng Docker Compose command đã document.
- [ ] Start backend services bằng command đã document.
- [ ] Start frontend app bằng command đã document.
- [ ] Xác nhận seeded agent login được.
- [ ] Reset demo data hoặc xác nhận baseline đã biết.
- [ ] Inject Facebook Messenger fixture.
- [ ] Inject Facebook comment fixture.
- [ ] Inject/receive email fixture.
- [ ] Xác nhận duplicate event submissions không tạo duplicate messages.
- [ ] Xác nhận outbound success/failure simulation.
- [ ] Xác nhận failed outbound retry path.
- [ ] Xác nhận health endpoints available.
- [ ] Xác nhận logs có correlation IDs và không chứa secrets.

## Operational Notes Log

Dùng section này cho operational observations bền vững trong quá trình dev.

| Date | Note | Follow-up |
| --- | --- | --- |
| 2026-06-18 | Monitoring plan được tạo trước khi services tồn tại. | Thêm URL endpoint và metric name cụ thể sau implementation. |
| 2026-06-18 | Local dependency endpoints đã document cho PostgreSQL, Kafka, Redis và Mailpit. | Thêm Actuator URLs sau khi scaffold backend. |
| 2026-06-18 | Docker dependency stack verified: PostgreSQL `pg_isready`, Redis `PING`, Kafka topic listing và Mailpit HTTP `200`. | Giữ service-level health checks cập nhật khi backend app tồn tại. |
| 2026-06-18 | Backend service scaffolds đã có Actuator và cấu hình expose health endpoint. | Verify `/actuator/health` qua HTTP sau khi migrations và service startup ổn định. |
