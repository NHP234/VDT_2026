# Cấu Trúc Frontend

Frontend dự kiến được triển khai bởi một frontend agent riêng (Gemini). File này
là handoff contract cho phần việc đó.

Phạm vi web app chỉ gồm hai màn hình trong `docs/requirement.md`:

1. Login.
2. Unified inbox.

Không thêm dashboard, settings, agent management, customer management, mobile
app, analytics, tags, notes, SLA controls, chatbot UI, rich text, attachments
hoặc tính năng ngoài phạm vi khác.

## Layout Dự Kiến

```text
frontend/
|-- package.json
|-- vite.config.ts
|-- tsconfig.json
|-- src/
|   |-- app/
|   |   |-- App.tsx
|   |   |-- router.tsx
|   |   `-- providers.tsx
|   |-- api/
|   |   |-- httpClient.ts
|   |   |-- authApi.ts
|   |   |-- conversationsApi.ts
|   |   `-- agentsApi.ts
|   |-- features/
|   |   |-- auth/
|   |   |-- inbox/
|   |   |-- conversation-detail/
|   |   |-- assignment/
|   |   `-- reply/
|   |-- components/
|   |-- types/
|   |-- styles/
|   `-- test/
```

## Màn Hình Bắt Buộc

### Login

Requirements: `FR-01`

- Trường username hoặc email.
- Trường password.
- Submit action.
- Loading state.
- Generic invalid-credentials error.
- Redirect agent đã authenticated sang inbox.

### Unified Inbox

Requirements: `FR-02`, `FR-03`, `FR-04`, `FR-05`, `FR-06`

Layout desktop tối thiểu 1280 px gồm:

1. Filter và conversation list.
2. Conversation timeline và reply composer.
3. Customer identity, assignment, status và activity summary.

Behavior bắt buộc:

- Backend-driven pagination.
- Filter theo channel, status và assigned agent.
- Search theo customer display name hoặc exact channel identity.
- Giữ selected conversation trong URL.
- Hiển thị loading, empty và error states.
- Hiển thị outbound delivery status: `QUEUED`, `SENT`, `FAILED`.
- Cho phép manual retry với failed outbound messages.
- Render plain text an toàn và không inject provider HTML.

## Kỳ Vọng API Contract

Chỉ dùng agent-facing API surface được định nghĩa trong `docs/requirement.md`:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`
- `GET /api/v1/agents`
- `GET /api/v1/conversations`
- `GET /api/v1/conversations/{id}`
- `PATCH /api/v1/conversations/{id}/status`
- `PATCH /api/v1/conversations/{id}/assignee`
- `POST /api/v1/conversations/{id}/replies`
- `POST /api/v1/messages/{id}/retry`

Nếu backend contract còn thiếu hoặc mơ hồ, ghi assumption trong frontend PR
hoặc changelog và không tự nghĩ thêm product feature.

## Quy Tắc Cho Frontend Agent

Gemini hoặc frontend-focused agent phải:

- Đọc `AGENTS.md`, `docs/requirement.md`, file này và
  `docs/agent-collaboration.md` trước khi sửa `frontend/`.
- Map UI work với requirement IDs trong `docs/progress-checklist.md`.
- Cập nhật `docs/changelog.md` sau mỗi thay đổi frontend hoàn chỉnh.
- Cập nhật `docs/traceability.md` khi requirement có thêm UI implementation
  hoặc test evidence.
- Tránh đổi backend contract nếu chưa cập nhật backend docs liên quan và chưa
  xác nhận với human khi thay đổi ảnh hưởng phạm vi.
- Chỉ dùng generated/mock data khi nó được cô lập rõ khỏi production paths và
  được document.

## Kỳ Vọng Test

Frontend test tối thiểu:

- Login success và generic failure state. `FR-01`
- Conversation list loading, empty, error, filtering và pagination behavior.
  `FR-02`
- Conversation detail rendering và URL persistence. `FR-03`
- Assignment và status action behavior. `FR-04`, `FR-05`
- Reply send, failed status display và retry action. `FR-06`
