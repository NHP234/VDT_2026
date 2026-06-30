# Scripts

PowerShell scripts are provided because the current workspace is Windows-based.

| Script | Purpose |
| --- | --- |
| `dev-up.ps1` | Start local Docker Compose dependencies. |
| `dev-down.ps1` | Stop local Docker Compose dependencies. |
| `check.ps1` | Run available repository checks. Backend and frontend project checks are skipped until those projects exist. |
| `smoke-cross-service.ps1` | Verify the simulator-driven Channel -> Kafka -> Inbox -> reply -> Channel -> Kafka -> Inbox path against running services. |
| `smoke-email-flow.ps1` | Verify email simulator inbound, duplicate rejection, Inbox reply, SMTP delivery, and Mailpit receipt against running services. |

Run scripts from the repository root:

```powershell
.\scripts\check.ps1
.\scripts\dev-up.ps1
.\scripts\smoke-cross-service.ps1
.\scripts\smoke-email-flow.ps1
```
