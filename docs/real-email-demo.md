# Real Email Demo Setup

Tài liệu này dùng cho demo email thật trước khi làm Facebook thật. Simulator
vẫn là đường demo chính ổn định; real email là lớp tăng độ thuyết phục.

## Mục Tiêu

- Người dùng gửi email thật vào một mailbox demo.
- Channel service poll mailbox đó bằng IMAP.
- Inbox service hiển thị conversation `EMAIL` trong unified inbox.
- Agent reply trong UI.
- Channel service gửi email trả lời qua SMTP của mailbox thật.

## Khuyến Nghị Tài Khoản

Dùng một Gmail demo riêng, không dùng email cá nhân chính. Không commit
credential. `.env` đã bị ignore, nên chỉ điền credential trong file đó.

Theo tài liệu chính thức của Google, Gmail hỗ trợ IMAP tại
`imap.gmail.com:993` với SSL và SMTP tại `smtp.gmail.com` với TLS qua port
`587` hoặc SSL qua port `465`. Gmail cũng nêu app password cần tài khoản bật
2-Step Verification và không phải tài khoản nào cũng có mục App Password.

Nguồn:

- https://developers.google.com/workspace/gmail/imap/imap-smtp
- https://support.google.com/mail/answer/185833

## Cấu Hình Gmail Bằng App Password

1. Tạo hoặc chọn một Gmail demo.
2. Bật 2-Step Verification.
3. Tạo App Password tại Google Account.
4. Bật IMAP trong Gmail settings nếu tài khoản đang tắt IMAP.
5. Copy `.env.example` thành `.env` nếu chưa có.
6. Sửa các dòng email trong `.env`:

```properties
EMAIL_MODE=real
EMAIL_MAILBOX=your-demo-mailbox@gmail.com
EMAIL_IMAP_ENABLED=true
EMAIL_SMTP_ENABLED=true

IMAP_HOST=imap.gmail.com
IMAP_PORT=993
IMAP_USERNAME=your-demo-mailbox@gmail.com
IMAP_PASSWORD=your-16-digit-google-app-password
IMAP_FOLDER=INBOX
IMAP_PROTOCOL=imaps
IMAP_POLL_FIXED_DELAY_MS=10000
IMAP_MAX_MESSAGES_PER_POLL=10

SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
CHANNEL_SMTP_HOST=smtp.gmail.com
CHANNEL_SMTP_PORT=587
SMTP_USERNAME=your-demo-mailbox@gmail.com
SMTP_PASSWORD=your-16-digit-google-app-password
SMTP_AUTH=true
SMTP_STARTTLS_ENABLE=true
SMTP_STARTTLS_REQUIRED=true
SMTP_SSL_ENABLE=false
MAIL_FROM=your-demo-mailbox@gmail.com
MAIL_REPLY_TO=your-demo-mailbox+omnicare@gmail.com
```

Nếu dùng SMTP SSL port `465`, đổi:

```properties
SMTP_PORT=465
CHANNEL_SMTP_PORT=465
SMTP_STARTTLS_ENABLE=false
SMTP_STARTTLS_REQUIRED=false
SMTP_SSL_ENABLE=true
```

## Chạy Stack

Sau khi sửa `.env`, restart channel service để nhận credential mới:

```powershell
.\scripts\dev-down.ps1
.\scripts\dev-up.ps1 -Build
```

Mở UI:

- Frontend: http://localhost:5173
- Login: `agent@example.test` / `change-me-local-only`

## Kịch Bản Demo

1. Mở Gmail cá nhân hoặc một email khác.
2. Gửi email tới `EMAIL_MAILBOX`.
3. Chạy watcher:

```powershell
.\scripts\watch-real-email-inbox.ps1 -Search your-sender-email@example.com
```

4. Khi script báo thấy conversation, mở unified inbox và search sender đó.
5. Agent reply trong UI.
6. Kiểm tra email người gửi nhận được reply thật.

## Lưu Ý Khi Demo

- IMAP poll mặc định mỗi 10 giây, nên email có thể mất vài giây mới xuất hiện.
- `MAIL_REPLY_TO` nên trỏ về địa chỉ được filter/label, ví dụ
  `your-demo-mailbox+omnicare@gmail.com`, để khách reply vào email do OmniCare
  gửi vẫn quay lại đúng folder IMAP demo.
- Gmail App Password có thể bị revoke khi đổi password Google.
- Nếu không thấy App Password, tài khoản có thể thuộc organization, dùng security
  key-only 2FA hoặc Advanced Protection.
- Không gửi nội dung nhạy cảm khi demo vì message được lưu vào database local.
- Nếu real provider lỗi, quay lại simulator/Mailpit để demo chính không bị gãy.
