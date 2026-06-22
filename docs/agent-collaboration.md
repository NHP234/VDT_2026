# Quy Tắc Cộng Tác Giữa Các Agent

Repository này có thể được chỉnh sửa bởi human owner, Codex, Gemini hoặc agent
khác. Mục tiêu là triển khai phối hợp, không để các nhánh suy nghĩ trôi lệch.

## Quy Tắc Chung

- Đọc `AGENTS.md` và `docs/requirement.md` trước khi sửa.
- Gắn mọi việc với một requirement ID đã khóa.
- Không thêm tính năng nằm trong phần `Out of Scope` của `docs/requirement.md`.
- Giữ nguyên các thay đổi local không liên quan.
- Không rewrite hoặc discard uncommitted work của người/agent khác.
- Cập nhật `docs/progress-checklist.md`, `docs/changelog.md` và
  `docs/traceability.md` khi thay đổi ảnh hưởng tiến độ.
- Ghi quyết định kiến trúc quan trọng vào `docs/adr/`.
- Không commit secrets, provider tokens, mailbox credentials, screenshot chứa
  dữ liệu nhạy cảm hoặc file `.env` local.

## Ownership

| Khu vực | Owner chính | Ghi chú |
| --- | --- | --- |
| `backend/` | Codex hoặc backend agent | Theo `docs/backend-structure.md`. |
| `frontend/` | Gemini hoặc frontend agent | Theo `docs/frontend-structure.md`. |
| `infra/` | Codex hoặc human owner | Docker Compose và local dependencies. |
| `docs/requirement.md` | Human owner | Thay đổi phạm vi cần approval rõ ràng. |
| `docs/adr/` | Agent bất kỳ, có human review cho quyết định lớn | Bắt buộc khi đổi kiến trúc hoặc stack đáng kể. |
| `docs/report/`, `docs/slides/` | Human owner với agent hỗ trợ | Chỉ dùng bằng chứng không nhạy cảm. |

## Checklist Handoff

Trước khi kết thúc task, agent nên để lại:

- Đã thay đổi gì.
- Requirement IDs được bao phủ.
- Đã verify bằng gì, hoặc vì sao chưa chạy verification.
- File agent tiếp theo nên đọc.
- Rủi ro hoặc blocker còn mở.

Dùng `docs/changelog.md` cho summary bền vững. Chat message thôi là chưa đủ vì
agent khác có thể không thấy cùng context.

## Thay Đổi Contract Backend Và Frontend

Đổi API contract cần:

1. Xác nhận thay đổi hỗ trợ `docs/requirement.md`.
2. Cập nhật backend implementation hoặc API documentation.
3. Cập nhật kỳ vọng trong frontend API client.
4. Cập nhật `docs/traceability.md`.
5. Ghi vào `docs/changelog.md`.

Không thêm frontend-only mock endpoints hoặc backend-only response fields nếu
chưa document rõ chúng là demo support tạm thời hay final contract.

## Nhắc Riêng Cho Gemini Frontend

Gemini dự kiến chỉ build application trong `frontend/` trừ khi human owner mở
rộng task rõ ràng. Gemini phải theo `docs/frontend-structure.md` và giữ frontend
work trong phạm vi UI hai màn hình đã khóa.
