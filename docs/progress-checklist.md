# Checklist Tiến Độ

Cập nhật checklist này khi công việc dự kiến bắt đầu hoặc hoàn thành. Dùng
`docs/changelog.md` để ghi tóm tắt thay đổi đã xong và các việc phát sinh.

Ký hiệu trạng thái: `[ ]` chưa bắt đầu, `[~]` đang làm, `[x]` đã kiểm chứng,
`[!]` đang bị chặn.

## 0. Thiết Lập Dự Án

- [x] Tạo bộ tài liệu quản lý dự án và quy tắc handoff giữa các agent. `NFR-01`, `NFR-05`
- [x] Tạo skeleton repository: `backend/`, `frontend/`, `infra/`, `scripts/`, và tài liệu cần thiết. `NFR-01`
- [x] Thêm `.env.example` cho hạ tầng local và credential dịch vụ. `NFR-01`, `NFR-04`
- [x] Thêm Docker Compose cho PostgreSQL, Kafka, Redis và Mailpit. `FR-07`, `FR-09`, `FR-10`, `NFR-01`
- [x] Thêm lệnh format, lint và test cho backend/frontend. `NFR-05`
- [~] Ghi lệnh setup, run, test và demo trong `README.md`. `NFR-01`, `FR-12`

## 1. Nền Tảng Backend

- [x] Tạo Spring Boot project cho Inbox service với Java 21 và Maven Wrapper. `FR-01` đến `FR-06`, `FR-11`
- [x] Tạo Spring Boot project cho Channel service với Java 21 và Maven Wrapper. `FR-07`, `FR-08`, `FR-09`, `FR-10`
- [x] Thêm tài liệu event contract dùng chung cho Kafka topic. `FR-09`
- [x] Thêm Flyway migration cho domain model đã khóa. `FR-02` đến `FR-06`, `FR-11`
- [x] Seed demo agents và dữ liệu baseline. `FR-01`, `FR-12`
- [x] Thêm authentication và protected API behavior. `FR-01`, `NFR-04`

## 2. Inbox Domain Và REST API

- [x] Triển khai danh sách conversation có filter, search và pagination. `FR-02`
- [x] Triển khai conversation detail với messages và activities. `FR-03`
- [x] Triển khai đổi status và ghi audit activity. `FR-04`, `FR-11`
- [x] Triển khai manual assignment và ghi audit activity. `FR-05`, `FR-11`
- [x] Triển khai tạo reply dưới dạng outbound message trạng thái `QUEUED`. `FR-06`, `FR-09`
- [x] Triển khai endpoint retry cho outbound message bị failed. `FR-06`
- [x] Trả validation/error response thống nhất. `FR-01` đến `FR-06`, `NFR-04`

## 3. Frontend Agent Workspace

- [x] Tạo React TypeScript Vite app. `FR-01` đến `FR-06`
- [x] Triển khai login screen và session handling. `FR-01`
- [x] Triển khai layout desktop 3 vùng cho unified inbox. `FR-02`, `FR-03`
- [x] Triển khai filter, exact identity search và pagination. `FR-02`
- [x] Triển khai thao tác assignment, status, reply và retry. `FR-04`, `FR-05`, `FR-06`
- [x] Triển khai loading, empty, error và failed-delivery states. `FR-02`, `FR-03`, `FR-06`
- [x] Đảm bảo không render trực tiếp external HTML. `FR-03`, `NFR-04`

## 4. Facebook Adapter

- [x] Thêm simulator endpoints và fixtures cho Messenger/comment events. `FR-08`, `FR-12`
- [x] Triển khai Meta webhook verification. `FR-08`
- [x] Validate Meta webhook signatures trong real-provider mode. `FR-08`, `NFR-04`
- [~] Normalize Facebook inbound payload, không để provider DTO lọt vào domain. `FR-08`
- [ ] Deduplicate Facebook inbound events bằng Redis và PostgreSQL. `FR-08`, `FR-10`, `NFR-03`
- [ ] Triển khai outbound Messenger/comment reply adapter với mocked contract tests. `FR-08`

## 5. Email Adapter

- [ ] Thêm đường đi inbound email fixture xác định. `FR-07`, `FR-12`
- [ ] Triển khai IMAP polling cho một mailbox. `FR-07`
- [ ] Normalize plain-text email fields và bỏ qua attachments. `FR-07`, `NFR-04`
- [ ] Triển khai email threading bằng `In-Reply-To` và `References`. `FR-07`
- [ ] Deduplicate email messages bằng Redis và PostgreSQL. `FR-07`, `FR-10`, `NFR-03`
- [ ] Triển khai SMTP outbound delivery với reply headers. `FR-07`

## 6. Kafka, Redis Và Reliability

- [x] Định nghĩa Kafka topics theo format `<domain>.<event>.v<version>`. `FR-09`
- [x] Publish inbound normalized events từ Channel service. `FR-09`
- [x] Consume inbound events idempotently trong Inbox service. `FR-09`, `NFR-03`
- [x] Publish outbound reply requests từ Inbox service. `FR-09`
- [x] Publish outbound delivery results từ Channel service. `FR-09`
- [x] Triển khai bounded retry và dead-letter handling. `FR-09`, `NFR-03`
- [ ] Chỉ dùng Redis cho short-lived deduplication và integration state. `FR-10`

## 7. Observability Và Operations

- [ ] Thêm correlation IDs xuyên suốt HTTP, logs và Kafka events. `FR-11`, `NFR-03`
- [~] Expose health endpoints cho backend services. `FR-11`
- [ ] Expose counters cho inbound, duplicate, sent và failed events. `FR-11`
- [ ] Thêm demo reset và run scripts. `FR-12`, `NFR-01`
- [ ] Ghi operational checks trong `docs/monitoring-and-operations.md`. `FR-11`, `FR-12`

## 8. Tests Và Quality

- [x] Unit test status transitions và reopening rules. `FR-04`, `NFR-05`
- [ ] Unit/integration test assignment audit recording. `FR-05`, `NFR-05`
- [ ] Test Facebook deduplication. `FR-08`, `NFR-05`
- [ ] Test email deduplication và threading. `FR-07`, `NFR-05`
- [x] Test reply delivery status transitions và retry behavior. `FR-06`, `NFR-05`
- [ ] Test authorization cho protected endpoints. `FR-01`, `NFR-05`
- [ ] Test migrations và durable uniqueness constraints. `FR-07`, `FR-08`, `NFR-05`
- [x] Thêm một end-to-end hoặc cross-service smoke test dùng simulators. `FR-12`, `NFR-05`

## 9. Deliverables Cuối

- [ ] Thêm C4-style context/container/component diagrams khi hữu ích. `FR-12`
- [ ] Thêm sequence diagrams cho Facebook inbound, email inbound và outbound reply. `FR-07`, `FR-08`, `FR-09`
- [ ] Hoàn thiện traceability table. `NFR-05`
- [ ] Chụp screenshot demo không chứa dữ liệu nhạy cảm. `FR-12`, `NFR-04`
- [ ] Viết report khoảng 15 trang. `FR-12`
- [ ] Tạo slide deck khoảng 15 slide. `FR-12`
- [ ] Rehearse final acceptance scenario trong `docs/requirement.md`. `FR-01` đến `FR-12`

## Mục Stretch

Không bắt đầu các mục này cho đến khi mọi MUST item ở trên đã verified.

- [ ] Inbox header counts theo `OPEN`, `PENDING`, `RESOLVED`.
- [ ] Server-sent events để tự động refresh inbox.
