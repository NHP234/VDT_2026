# Prompt Setup Frontend Cho Gemini

Dùng prompt này khi yêu cầu Gemini bắt đầu setup frontend.

```text
Bạn là frontend agent cho D:\VDT_2026.

Đọc các file này trước khi sửa:
- AGENTS.md
- docs/requirement.md
- docs/frontend-structure.md
- docs/agent-collaboration.md
- docs/progress-checklist.md

Task:
Tạo frontend project ban đầu trong frontend/ cho Omnichannel Customer Care
System. Dùng React + TypeScript + Vite. Giữ phạm vi trong MVP đã khóa:
Login và Unified Inbox only. Không thêm dashboard, settings, analytics, tags,
notes, attachments, rich text, chatbot UI hoặc mobile-specific features.

Setup requirements:
- Tạo Vite React TypeScript app trong frontend/.
- Thêm React Router cho Login và Unified Inbox routes.
- Thêm TanStack Query cho server state.
- Thêm ESLint và Prettier.
- Thêm Vitest và React Testing Library.
- Thêm API client structure khớp docs/frontend-structure.md và API paths trong
  docs/requirement.md.
- Chỉ thêm placeholder pages/components khi cần để chứng minh routing và setup;
  không tự nghĩ thêm UI features chưa được duyệt.
- Thêm npm scripts cho dev, build, lint, format/check-format và test.
- Cập nhật docs/progress-checklist.md cho frontend setup checklist items đã xong.
- Cập nhật docs/changelog.md với thay đổi và verification đã chạy.
- Cập nhật docs/traceability.md cho FR/NFR bị ảnh hưởng bởi setup.

Verification:
- Chạy npm install nếu dependencies còn thiếu.
- Chạy npm run lint.
- Chạy npm run test -- --run.
- Chạy npm run build.

Report back:
- Files changed.
- Requirement IDs covered.
- Commands run and results.
- Assumptions hoặc blockers.
```
