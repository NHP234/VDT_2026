# Cấu Trúc Backend

System boundary đã khóa gồm hai Spring Boot service:

- `backend/inbox-service`: authentication, agents, conversations, messages,
  assignments, statuses, audit activities và agent-facing REST APIs.
- `backend/channel-service`: Facebook adapter, email adapter, simulator inputs,
  outbound provider delivery, Redis deduplication và Kafka integration.

Không thêm deployable service mới trừ khi `docs/requirement.md` thay đổi và có
ADR ghi lại quyết định.

## Layout Dự Kiến

```text
backend/
|-- inbox-service/
|   |-- mvnw
|   |-- pom.xml
|   |-- src/main/java/.../inbox/
|   |   |-- identity/
|   |   |-- inbox/
|   |   |-- customer/
|   |   |-- shared/
|   |   `-- config/
|   |-- src/main/resources/
|   |   |-- application.yml
|   |   `-- db/migration/
|   `-- src/test/java/
|-- channel-service/
|   |-- mvnw
|   |-- pom.xml
|   |-- src/main/java/.../channel/
|   |   |-- facebook/
|   |   |-- email/
|   |   |-- delivery/
|   |   |-- events/
|   |   |-- deduplication/
|   |   |-- simulator/
|   |   `-- config/
|   |-- src/main/resources/
|   `-- src/test/java/
`-- event-contracts/
    `-- README.md
```

## Mẫu Package

Trong mỗi business module, ưu tiên cấu trúc:

```text
module/
|-- domain/
|-- application/
|   |-- command/
|   |-- query/
|   `-- port/
|-- infrastructure/
`-- interfaces/
```

Quy tắc:

- `domain` chứa business concepts và rules, hạn chế phụ thuộc framework.
- `application` chứa use cases và ports.
- `infrastructure` chứa JPA, Kafka, Redis, Meta, email và adapter khác.
- `interfaces` chứa REST controllers, webhook endpoints, event consumers và request/response DTOs.
- External provider DTOs không được trở thành domain objects hoặc public API responses.
- Không trả JPA entities trực tiếp từ controllers.

## Trách Nhiệm Của Inbox Service

Capability bắt buộc:

- Agent authentication và protected REST APIs. `FR-01`
- Conversation list, detail, filtering, search và pagination. `FR-02`, `FR-03`
- Status/assignment changes kèm audit activity. `FR-04`, `FR-05`, `FR-11`
- Tạo queued outbound message và retry endpoint. `FR-06`
- Idempotent inbound event consumer. `FR-09`
- Delivery result consumer. `FR-09`
- Health và metrics. `FR-11`

## Trách Nhiệm Của Channel Service

Capability bắt buộc:

- Facebook webhook verification, signature validation, inbound normalization,
  simulator fixtures và outbound replies. `FR-08`, `FR-12`
- Email IMAP polling, fixture ingestion, SMTP outbound delivery và giữ threading metadata. `FR-07`, `FR-12`
- Redis duplicate fast rejection, nhưng độ bền đúng vẫn được PostgreSQL enforce trong Inbox service. `FR-10`
- Kafka publication và consumption cho channel events. `FR-09`
- Health và metrics. `FR-11`

## Quy Tắc Database

- PostgreSQL là durable source of truth.
- Mọi thay đổi schema dùng Flyway migration mới.
- Không sửa migration đã apply.
- Internal aggregate IDs dùng UUID trừ khi ADR chọn khác.
- Provider IDs được lưu riêng và dùng cho deduplication.
- Unique constraints phải enforce inbound message idempotency ngay cả khi Redis rỗng hoặc key hết hạn.

## Quy Tắc API

- Agent-facing APIs bắt đầu bằng `/api/v1`.
- Webhooks bắt đầu bằng `/webhooks/{provider}`.
- Simulator endpoints phải tách khỏi real-provider endpoints và chỉ bật trong local `demo` profile.
- REST collections có thể tăng trưởng phải có pagination.
- Error responses nên dùng Problem Details khi thực tế.
- OpenAPI documentation phải khớp implemented behavior.

## Vị Trí Test

Dùng loại test nhỏ nhất chứng minh được behavior:

- Unit tests cho domain state transitions và application services.
- Repository/migration tests cho persistence constraints.
- API tests cho validation, authentication, authorization và error responses.
- Adapter contract tests cho Meta và email provider behavior.
- Cross-service hoặc end-to-end smoke tests cho deterministic demo path.
