# Hướng Dẫn Quản Lý Dự Án

## Nguồn Quyết Định Phạm Vi

`docs/requirement.md` là nguồn sự thật cho phạm vi sản phẩm. Nếu tài liệu này,
checklist, ADR, code sinh tự động hoặc gợi ý của agent mâu thuẫn với
`docs/requirement.md`, tài liệu yêu cầu sẽ được ưu tiên.

## File Theo Dõi

| File | Cập nhật khi |
| --- | --- |
| `docs/progress-checklist.md` | Công việc dự kiến bắt đầu, thay đổi trạng thái, hoặc hoàn thành. |
| `docs/changelog.md` | Một thay đổi hoàn chỉnh được làm xong, đặc biệt khi thay đổi đó chưa có trong checklist. |
| `docs/traceability.md` | Một requirement có thêm phần triển khai, test, tài liệu hoặc bằng chứng demo. |
| `docs/adr/` | Có quyết định kiến trúc hoặc công nghệ đáng kể. |
| `README.md` | Lệnh setup, chạy, test hoặc demo thay đổi. |
| `.env.example` | Cần thêm biến môi trường mới. |

## Nhịp Triển Khai

Dùng kế hoạch 3 tuần trong `docs/requirement.md`:

1. Tuần 1: lõi dùng được và đường đi simulator xác định.
2. Tuần 2: tích hợp kênh, Kafka, Redis, deduplication và retry.
3. Tuần 3: hardening, test, demo, report, diagram và slide.

Mỗi task nên ghi rõ:

- Requirement ID như `FR-02` hoặc `NFR-03`.
- Hành vi nghiệm thu.
- File hoặc module bị ảnh hưởng.
- Test hoặc kiểm chứng thủ công đã dùng.
- Công việc có nằm trong kế hoạch hay là phát sinh.

## Trạng Thái Tiến Độ

Dùng các trạng thái này thống nhất trong checklist và summary:

- `Not started`: chưa có triển khai.
- `In progress`: đã có việc làm nhưng chưa đủ acceptance behavior.
- `Blocked`: chưa thể tiếp tục nếu thiếu quyết định, credential, dependency hoặc tài khoản ngoài.
- `Implemented`: đã có hành vi, nhưng chưa kiểm chứng đầy đủ.
- `Verified`: test tự động hoặc kiểm chứng thủ công liên quan đã pass.
- `Deferred`: tạm hoãn rõ ràng vì là stretch, ngoài phạm vi, hoặc được thay bằng task khác.

## Công Việc Ngoài Checklist

Nếu có việc phát sinh ngoài `docs/progress-checklist.md`:

1. Xác nhận việc đó vẫn hỗ trợ một requirement đã khóa.
2. Thêm entry ngắn vào `docs/changelog.md` dưới ngày hiện tại.
3. Đánh dấu là `Unplanned`.
4. Ghi requirement liên quan hoặc giải thích đó là maintenance của dự án.
5. Nếu thay đổi kiến trúc hoặc phạm vi, thêm/cập nhật ADR.

## Tiêu Chí Hoàn Thành

Một mục dự án chỉ được xem là xong khi:

- Acceptance behavior đã được triển khai.
- Test liên quan hoặc kiểm chứng thủ công đã được ghi lại.
- Documentation và traceability đã được cập nhật.
- Security, privacy, error handling và observability đã được cân nhắc.
- Thay đổi không đưa vào khái niệm ngoài phạm vi của `docs/requirement.md`.
