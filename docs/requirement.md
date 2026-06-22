# Tài Liệu Yêu Cầu Sản Phẩm

## 1. Kiểm Soát Tài Liệu

| Field | Value |
| --- | --- |
| Project | Omnichannel Customer Care System for Facebook and Email |
| Program | Viettel Digital Talent 2026 - Mini Project |
| Version | 1.0 |
| Status | Scope locked for implementation |
| Date | 2026-06-11 |
| Delivery window | 3 weeks |

## 2. Quyền Ưu Tiên Về Phạm Vi

Tài liệu này là nguồn sự thật cho phạm vi sản phẩm.

- Chỉ các requirement được đánh dấu **MUST** mới bắt buộc để hoàn thành dự án.
- Các mục **STRETCH** không thuộc completion criteria. Chỉ bắt đầu chúng sau khi
  mọi requirement MUST đã pass acceptance criteria.
- Bất cứ điều gì nằm trong **Out of Scope** không được triển khai trong delivery
  window 3 tuần.
- Feature mới cần được edit rõ trong tài liệu này, tăng version, và loại bỏ
  hoặc giảm một phần việc có effort tương đương.
- Khi tài liệu này mâu thuẫn với `AGENTS.md`, product scope trong tài liệu này
  thắng. Engineering rules trong `AGENTS.md` vẫn áp dụng.
- Refactoring, infrastructure và abstractions phải hỗ trợ một requirement đã
  liệt kê. Chúng không phải deliverable độc lập.

## 3. Định Nghĩa Sản Phẩm

### 3.1 Vấn Đề

Customer-care agents hiện phải chuyển qua lại giữa Facebook và email để nhận và
trả lời yêu cầu khách hàng. Điều này làm conversation khó theo dõi, khó assign
và khó resolve nhất quán.

### 3.2 Mục Tiêu Sản Phẩm

Cung cấp một web-based inbox nơi agent có thể nhận, xem, assign, trả lời và
resolve customer conversations đến từ Facebook hoặc email.

### 3.3 Tuyên Bố Demo Chính

> Một Facebook message/comment hoặc email inbound đi vào hệ thống, xuất hiện
> đúng một lần trong unified inbox, được agent xử lý, và reply được gửi qua đúng
> kênh ban đầu.

Nếu một feature không làm statement này mạnh hơn, feature đó không thuộc MVP.

### 3.4 Tiêu Chí Thành Công

MVP thành công khi:

1. Cả Facebook và email inbound flows có thể demo deterministically bằng simulators.
2. Ít nhất một real provider adapter cho mỗi channel được triển khai và test với
   test account có sẵn hoặc protocol-compatible test server.
3. Agent hoàn thành core demo flow từ web application.
4. Duplicate inbound events không tạo duplicate messages.
5. Failed outbound messages hiển thị được và retry được.
6. Toàn bộ local system start bằng documented commands và Docker Compose.

## 4. Actor Và Môi Trường

### 4.1 Actor

MVP có đúng một application role:

- **Agent**: sign in, xem conversations, assign conversations, đổi status và
  reply khách hàng.

Không có administrator workflow trong MVP. Agent accounts và integration
configuration được tạo bằng seed data và environment variables.

### 4.2 Setup Được Hỗ Trợ

- Một organization/workspace.
- Một Facebook Page.
- Một email mailbox.
- Một danh sách nhỏ seeded agents.
- Desktop web browser, viewport width tối thiểu 1280 px.
- Local hoặc demo-machine deployment qua Docker Compose.

Multi-tenant và production-cloud deployment rõ ràng nằm ngoài phạm vi.

## 5. System Boundary Đã Khóa

Implementation chỉ gồm các application components này:

1. **Web app**: React và TypeScript agent interface.
2. **Inbox service**: Spring Boot service sở hữu authentication, conversations,
   messages, assignments, statuses, audit activities và agent-facing REST APIs.
3. **Channel service**: Spring Boot service sở hữu Facebook/email adapters,
   webhook hoặc polling input, outbound delivery và provider payload mapping.
4. **PostgreSQL**: durable source of truth.
5. **Kafka**: asynchronous communication cho inbound messages và outbound reply
   requests/results.
6. **Redis**: inbound event deduplication và short-lived integration state.

Không giới thiệu API gateway, service discovery server, centralized
configuration server, Kubernetes cluster hoặc business microservice bổ sung.

## 6. Functional Requirements - Yêu Cầu Chức Năng

### FR-01 - Xác Thực Agent [MUST]

Hệ thống phải:

- Cung cấp login screen dùng username/email và password.
- Authenticate seeded agent accounts qua Inbox service.
- Bảo vệ mọi agent-facing pages và APIs.
- Cho phép current agent log out.

Acceptance criteria:

- Valid credentials mở unified inbox.
- Invalid credentials hiển thị generic error, không tiết lộ field nào sai.
- Unauthenticated request tới protected API bị reject.
- Không có user registration, password reset, account management hoặc SSO.

### FR-02 - Unified Conversation Inbox [MUST]

Hệ thống phải hiển thị paginated conversation list gồm:

- Customer display name hoặc channel identity.
- Channel: `FACEBOOK` hoặc `EMAIL`.
- Conversation source type: `MESSAGE`, `COMMENT` hoặc `EMAIL`.
- Last-message preview.
- Last-activity time.
- Current status.
- Assigned agent hoặc `Unassigned`.

Agent có thể filter theo:

- Channel.
- Status.
- Assigned agent, gồm `Unassigned` và `Assigned to me`.

Agent có thể search theo customer display name hoặc exact channel identity.

Acceptance criteria:

- List được order theo latest activity trước.
- Filters có thể combine.
- Pagination do backend thực hiện.
- Empty, loading và error states hiển thị được.
- Không có full-message content search.

### FR-03 - Chi Tiết Conversation [MUST]

Agent có thể mở conversation và xem:

- Read-only customer/channel identity.
- Channel và source type.
- Current assignee và status.
- Tất cả normalized messages theo chronological order.
- Message direction: `INBOUND` hoặc `OUTBOUND`.
- Message timestamp.
- Outbound delivery status: `QUEUED`, `SENT` hoặc `FAILED`.
- Status và assignment activity history.

Message trong MVP là bản ghi normalized cho một nội dung chăm sóc khách hàng,
không phải chỉ riêng email hoặc chỉ riêng Messenger. Các nguồn hợp lệ:

- Facebook Page private Messenger inbox message.
- Facebook Page post comment.
- Email message.

`MessageDirection` mô tả hướng đi so với hệ thống:

- `INBOUND`: khách hàng hoặc provider gửi vào hệ thống.
- `OUTBOUND`: agent gửi từ hệ thống ra lại original channel.

Acceptance criteria:

- Refresh page vẫn giữ selected conversation qua URL.
- Provider-specific payload structures không lộ ra UI.
- Plain text được render an toàn; external HTML không được render.

### FR-04 - Trạng Thái Conversation [MUST]

Conversation status chỉ gồm:

- `OPEN`: cần agent xử lý.
- `PENDING`: đang chờ khách hàng hoặc external action.
- `RESOLVED`: được xem là hoàn tất.

Rules:

- Agent có thể manually chọn bất kỳ status nào trong 3 status.
- Conversation mới bắt đầu ở `OPEN`.
- Inbound message mới trên `PENDING` hoặc `RESOLVED` conversation tự động
  chuyển lại thành `OPEN`.
- Agent gửi outbound reply không tự động resolve conversation. Agent phải tự đổi
  sang `PENDING` hoặc `RESOLVED` khi phù hợp.
- Mỗi status change ghi actor, old status, new status và timestamp.

Không có `CLOSED`, archived, deleted, snoozed hoặc SLA state trong MVP.

### FR-05 - Gán Conversation Thủ Công [MUST]

Agent có thể:

- Assign conversation cho bất kỳ seeded agent nào.
- Assign conversation cho chính mình.
- Reassign conversation đang thuộc agent khác sang một seeded agent khác.
- Đưa conversation về `Unassigned`.

Assignment trong MVP là peer-to-peer manual assignment: mọi authenticated agent
có quyền ngang nhau cho thao tác assign, self-assign, reassign và unassign. MVP
không có Lead/Supervisor role, approval step hoặc phân quyền riêng cho
assignment.

Acceptance criteria:

- Assignment changes hiển thị trong conversation list và detail.
- Mỗi thay đổi ghi actor, old assignee, new assignee và timestamp.
- Không có teams, queues, skills, routing rules hoặc automatic assignment.

### FR-06 - Reply Plain Text [MUST]

Agent có thể gửi non-empty plain-text reply từ một conversation.

Rules:

- Reply luôn dùng original channel và source type của conversation.
- Inbox service lưu outbound message là `QUEUED` trước khi delivery.
- Channel service cập nhật thành `SENT` hoặc `FAILED` qua event.
- Failed message có thể manual retry.
- Repeated clicks hoặc redelivered events không được tạo nhiều provider sends
  cho cùng một reply ID.

Acceptance criteria:

- Outbound message xuất hiện ngay với delivery status hiện tại.
- Provider success đổi status thành `SENT`.
- Simulated provider failure đổi status thành `FAILED`.
- Retry failed message tạo một delivery attempt mới nhưng không duplicate
  conversation message.

Rich text, attachments, templates, signatures, drafts, scheduled replies và bulk
replies nằm ngoài phạm vi.

### FR-07 - Tích Hợp Email [MUST]

Inbound:

- Channel service poll một configurable mailbox bằng IMAP.
- Normalize sender, recipients, subject, plain-text body, message ID, reference
  headers và received timestamp.
- Publish normalized inbound event tới Kafka.
- Reprocess cùng email message ID không duplicate message.

Outbound:

- Channel service gửi plain-text email qua SMTP.
- Giữ `In-Reply-To` và `References` headers khi reply existing email thread.
- Report success/failure cho Inbox service qua Kafka.

Threading rules:

- Email join existing conversation khi `In-Reply-To` hoặc `References` match
  một known email message.
- Nếu không match thì tạo conversation mới.
- Không dùng subject-only matching vì có thể merge unrelated messages.

Acceptance criteria:

- Inbound và outbound flows pass integration test bằng protocol-compatible test
  mail server hoặc configured test mailbox.
- Demo có thể inject deterministic inbound email fixture nếu external mailbox
  không available.
- HTML parts được convert sang safe plain text hoặc ignored.
- Attachments bị ignored và không persist.

### FR-08 - Tích Hợp Facebook [MUST]

Facebook adapter hỗ trợ một configured Facebook Page.

Inbound event types:

- Private Page/Messenger text message từ customer.
- Customer text comment trên Page post.

Outbound actions:

- Reply bằng text message tới Messenger conversation.
- Reply bằng text comment tới originating Facebook comment.

Adapter phải:

- Support Meta webhook verification.
- Require và validate provider webhook signature trong real-provider mode.
  Unsigned simulator input chỉ được accept bởi endpoint riêng bật trong local
  `demo` profile.
- Normalize provider IDs, sender identity, text, timestamps và source type.
- Deduplicate redelivered webhook events.
- Publish normalized events tới Kafka.
- Giữ access tokens ngoài source control.

Conversation rules:

- Messenger messages được group theo Facebook Page và sender identity.
- Page-post comment tạo conversation gắn với root comment.
- Messenger và comment conversations không merge.
Acceptance criteria:

- Checked-in simulator fixture demo được cả Messenger và comment flows.
- Duplicate fixtures/webhook deliveries chỉ tạo một message.
- Real adapter pass HTTP contract test với mocked Meta endpoints cho webhook
  verification, signature rejection, Messenger reply và comment reply.
- Live Meta demo chỉ bắt buộc khi valid Page, App, token và test-user access có
  sẵn trước cuối Week 2. Nếu không, dùng simulator và document external
  limitation trong report.

Reactions, likes, edits, deletes, media, post publishing, moderation, Instagram
và non-Page Facebook features nằm ngoài phạm vi.

### FR-09 - Xử Lý Bất Đồng Bộ Và Reliability [MUST]

Kafka được dùng cho các flows:

1. Channel service publishes inbound channel event.
2. Inbox service consumes và persists normalized message.
3. Inbox service publishes outbound reply request.
4. Channel service sends reply và publishes delivery result.
5. Inbox service updates outbound delivery status.

Rules:

- Delivery semantics là at least once.
- Consumers idempotent bằng stable event IDs và external message IDs.
- Processing failures dùng bounded retry rồi dead-letter topic.
- Failed outbound delivery vẫn visible trong Inbox UI.
- Kafka không dùng cho synchronous conversation list/detail queries.

### FR-10 - Deduplication Bằng Redis [MUST]

Redis phải:

- Lưu short-lived deduplication keys cho inbound provider event IDs.
- Dùng configurable expiration time.
- Cải thiện fast duplicate rejection nhưng không trở thành durable source of truth.

PostgreSQL cũng phải enforce unique durable external-message constraint.
Correctness phải sống sót khi Redis data mất hoặc key hết hạn.

Không cần general-purpose caching layer, distributed session platform hoặc
Redis-backed business entity.

### FR-11 - Audit Và Operational Endpoints [MUST]

Hệ thống phải:

- Persist assignment và status activities.
- Include correlation IDs trong logs xuyên service/event boundaries.
- Expose Spring Boot health endpoints.
- Expose counters cho inbound events processed, duplicate events rejected,
  outbound deliveries sent và outbound deliveries failed qua Actuator/Micrometer.

Không có user-facing analytics dashboard hoặc report builder.

### FR-12 - Chế Độ Demo Xác Định [MUST]

Repository phải có demo mode không phụ thuộc approval từ external providers.

Demo mode phải có:

- Seeded agent accounts.
- Seeded baseline conversations hoặc reset script.
- Facebook Messenger và comment webhook fixtures.
- Inbound email fixture hoặc test-server command.
- Controllable outbound success/failure simulation.
- Documented steps để reset và chạy main demo.

Simulated messages phải đi qua cùng normalization, Kafka, persistence và UI paths
như real provider messages. Direct database insertion không phải inbound demo
flow hợp lệ.

## 7. API Surface Tối Thiểu Cho Agent

Inbox service API giới hạn ở:

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/auth/login` | Authenticate agent |
| `POST` | `/api/v1/auth/logout` | End current session/token |
| `GET` | `/api/v1/auth/me` | Get current agent |
| `GET` | `/api/v1/agents` | List seeded agents for assignment |
| `GET` | `/api/v1/conversations` | Paginated list, filters và identity search |
| `GET` | `/api/v1/conversations/{id}` | Conversation, messages và activity |
| `PATCH` | `/api/v1/conversations/{id}/status` | Change status |
| `PATCH` | `/api/v1/conversations/{id}/assignee` | Change assignment |
| `POST` | `/api/v1/conversations/{id}/replies` | Send plain-text reply |
| `POST` | `/api/v1/messages/{id}/retry` | Retry failed outbound message |

Additional internal webhook, simulator, health và service-to-service endpoints
chỉ được thêm khi cần cho locked requirements.

## 8. Domain Model Đã Khóa

MVP chỉ persist các business concepts sau:

- `Agent`
- `Customer`
- `ChannelIdentity`
- `Conversation`
- `Message`
- `ConversationActivity`
- `ProcessedEvent`
- `OutboxEvent` khi cần reliable publication

Key enums:

- `Channel`: `FACEBOOK`, `EMAIL`
- `ConversationSourceType`: `MESSAGE`, `COMMENT`, `EMAIL`
- `ConversationStatus`: `OPEN`, `PENDING`, `RESOLVED`
- `MessageDirection`: `INBOUND`, `OUTBOUND`
- `DeliveryStatus`: `RECEIVED`, `QUEUED`, `SENT`, `FAILED`

Không thêm `Team`, `Tag`, `Note`, `SLA`, `Campaign`, `Attachment`, `Bot`,
`Ticket`, `KnowledgeBase` hoặc custom-field model.

## 9. Business Rules - Quy Tắc Nghiệp Vụ

1. Internal IDs dùng UUIDs; provider IDs được lưu riêng.
2. Timestamps lưu ở UTC và serialize dạng ISO 8601.
3. Inbound message unique theo channel, provider account/Page và external
   message ID.
4. Customer không tự động merge giữa Facebook và email.
5. Inbound message mới cập nhật last-activity time của conversation.
6. Inbound message mới trên `PENDING` hoặc `RESOLVED` conversation tự động
   chuyển conversation về `OPEN`.
7. Chỉ authenticated agents được read hoặc change conversations.
8. Provider payload DTOs không bao giờ thành domain entities hoặc public API
   responses.
9. Outbound reply input bị reject nếu trên 10,000 characters. Inbound content
   dài hơn được normalize và truncate xuống 10,000 characters trước persistence.
10. Không hỗ trợ delete conversations hoặc messages.
11. Assignment là peer-to-peer trong MVP: mọi authenticated agent có thể
    assign, self-assign, reassign hoặc unassign conversation.

## 10. Phạm Vi User Interface

Web app có đúng hai functional screens:

### 10.1 Login

- Username/email field.
- Password field.
- Submit action.
- Loading và generic error state.

### 10.2 Unified Inbox

Inbox là desktop layout 3 vùng:

1. Filter và conversation list.
2. Conversation timeline và reply composer.
3. Customer identity, assignment, status và activity summary.

Allowed supporting UI:

- Empty/loading/error states.
- Confirmation hoặc feedback cho failed operations.
- Responsive handling xuống đến desktop width 1280 px.

Không có dashboard, settings screen, agent-management screen, separate
customer-management screen hoặc mobile-specific interface.

Client có thể dùng polling sau khi send/receive events. WebSocket và browser
push notifications nằm ngoài phạm vi. SSE chỉ được phép là Stretch item 2.

## 11. Non-Functional Requirements - Yêu Cầu Phi Chức Năng

### NFR-01 - Khả Năng Chạy Lại Local [MUST]

- Hạ tầng bắt buộc start qua Docker Compose.
- Application setup và demo commands được document trong root `README.md`.
- Secrets được supply qua environment variables với `.env.example`.

### NFR-02 - Mục Tiêu Performance [MUST]

Trên demo machine với tối đa 1,000 conversations và 10,000 messages:

- Paginated inbox request nên hoàn tất trong 2 giây.
- Conversation-detail request nên hoàn tất trong 2 giây.
- Successfully consumed inbound event nên xuất hiện qua API polling trong 5 giây.

Đây là demo targets, không phải production SLAs hoặc load-test commitments.

### NFR-03 - Reliability [MUST]

- Duplicate supported inbound events không tạo duplicate messages.
- Restart application service không làm mất committed business data.
- Provider và Kafka failures được log với correlation IDs.
- Outbound failures vẫn observable và retryable.

### NFR-04 - Security [MUST]

- Passwords được lưu bằng modern one-way password hash.
- Access tokens, mailbox credentials và passwords không bao giờ commit.
- Protected APIs enforce authentication server-side.
- Webhook inputs và reply bodies được validate.
- External HTML không bao giờ inject vào browser.
- Sensitive values được mask trong logs và report screenshots.

### NFR-05 - Tests [MUST]

Minimum automated suite cover:

- Conversation status và reopening rules.
- Email và Facebook deduplication.
- Email threading bằng reference headers.
- Assignment audit recording.
- Reply status transition từ `QUEUED` sang `SENT` hoặc `FAILED`.
- Authorization cho protected endpoints.
- Persistence migrations và key repository constraints.
- Một end-to-end hoặc cross-service smoke path dùng simulators.

CI và tests không được require live Meta hoặc external email credentials.

## 12. Ngoài Phạm Vi

Các mục sau bị cấm rõ ràng trong locked delivery window:

- User registration, password reset, profile editing, SSO và role-management UI.
- Multiple organizations, tenants, Facebook Pages hoặc email mailboxes.
- Integration configuration qua web UI.
- Instagram, Zalo, WhatsApp, Telegram, live chat, SMS hoặc voice channels.
- Facebook media, reactions, likes, post publishing, edit/delete synchronization
  và moderation workflows.
- Email/Facebook attachments, inline images, rich-text composition và HTML rendering.
- Customer/contact editing, cross-channel identity merge, company accounts và CRM fields.
- Tags, internal notes, mentions, teams, queues, skills, SLA, priorities và automatic routing.
- Canned responses, signatures, drafts, scheduled messages và bulk actions.
- Chatbot, generative AI, sentiment analysis, translation hoặc recommendations.
- WebSocket updates, browser push, desktop notifications và mobile apps. SSE chỉ
  được phép trong locked Stretch scope.
- Full-text message search và external search engines.
- Analytics dashboard, business reports, exports và BI integration.
- Kubernetes, service mesh, API gateway, service discovery, cloud autoscaling và
  multi-region deployment.
- Exactly-once distributed delivery và graphical dead-letter management UI.
- Production-grade high availability, disaster recovery và formal load testing.

## 13. Phạm Vi Stretch

Dự án chỉ có hai stretch items được phép:

1. Một inbox header nhỏ hiển thị counts theo `OPEN`, `PENDING`, `RESOLVED`.
2. Server-sent events cho automatic inbox refresh.

Các mục này:

- Không bắt buộc cho final demo hoặc Definition of Done.
- Không được bắt đầu trước khi mọi MUST requirements và tests pass.
- Phải bỏ ngay nếu đe dọa documentation hoặc demo readiness.

Không có stretch feature nào khác được duyệt trong version này.

## 14. Kế Hoạch Triển Khai 3 Tuần

### Tuần 1 - Core Có Thể Dùng Được

- Repository và Docker Compose skeleton.
- Inbox service schema, migrations, authentication và seed data.
- Conversation list/detail, assignment, status và reply API foundations.
- Login và unified inbox UI.
- Kafka contracts và deterministic inbound simulator path.

Exit condition:

- Một simulated inbound message xuất hiện trong UI và có thể assign, đổi status
  và queue reply.

### Tuần 2 - Tích Hợp Channel

- Facebook Messenger/comment webhook adapter và simulator fixtures.
- IMAP inbound và SMTP outbound email adapter.
- Outbound Facebook/email delivery path.
- Kafka result events, Redis deduplication, durable uniqueness, retry và
  dead-letter handling.

Exit condition:

- Cả hai channel hoàn thành core flow qua simulators/test servers, và real test
  accounts nếu có đã được thử và document.

### Tuần 3 - Hardening Và Deliverables

- Security review, validation, logs, health và basic metrics.
- Unit, integration, API và smoke tests bắt buộc.
- Demo reset/run scripts và rehearsal.
- Architecture diagrams và screenshots.
- Report khoảng 15 trang.
- Presentation khoảng 15 slide.

Exit condition:

- Clean-machine setup chạy được documented demo mà không phụ thuộc live provider availability.

## 15. Kịch Bản Nghiệm Thu Cuối

Final demo phải show:

1. Agent logs in.
2. Facebook Messenger message hoặc Page comment được injected/received.
3. Event xuất hiện đúng một lần trong unified inbox.
4. Agent mở event, assign và đổi status.
5. Agent gửi reply và thấy `SENT`.
6. Duplicate Facebook event được submit và không có duplicate xuất hiện.
7. Email được injected/received và threaded vào đúng conversation.
8. Agent reply bằng email.
9. Simulated delivery failure tạo `FAILED`.
10. Agent retry và thấy delivery thành `SENT`.
11. Agent resolves conversation.
12. Inbound message mới tự động reopen conversation.

Pass scenario này, required automated tests và documented clean-machine startup
được xem là product completion.

## 16. Quy Tắc Truy Vết Requirement

Mọi implementation task, API, table, event, UI control và automated test phải
tham chiếu ít nhất một `FR-*` hoặc `NFR-*` requirement.

Trước khi thêm việc, hỏi:

1. Locked requirement nào yêu cầu việc này?
2. Acceptance criterion nào sẽ verify nó?
3. Nó có đưa vào khái niệm ngoài phạm vi không?

Nếu câu 1 không có đáp án, việc đó không được đưa vào dự án 3 tuần.
