# Architecture Decision Records - Nhật Ký Quyết Định Kiến Trúc

Dùng thư mục này để ghi các quyết định kiến trúc và công nghệ quan trọng.

Ví dụ cần ADR:

- Giữ dự án ở dạng hai Spring Boot services thay vì nhiều microservices hơn.
- Chọn authentication strategy.
- Giới thiệu Kafka topics và event contracts.
- Dùng Redis chỉ cho deduplication.
- Chọn frontend component library.
- Thay đổi default technology trong `AGENTS.md`.

## Cách Đặt Tên

Dùng số thứ tự:

```text
0001-short-decision-title.md
0002-another-decision.md
```

## Template ADR

```text
# NNNN - Tiêu Đề Quyết Định

## Trạng Thái

Proposed | Accepted | Superseded

## Bối Cảnh

Vấn đề hoặc constraint nào khiến ta phải ra quyết định?

## Quyết Định

Ta đã quyết định gì?

## Hệ Quả

Trade-offs, limitations và follow-up work là gì?

## Requirement Liên Quan

Quyết định này hỗ trợ FR/NFR ID nào?
```
