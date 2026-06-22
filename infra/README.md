# Infrastructure

Local infrastructure is defined in the root `docker-compose.yml`.

Services:

| Service | Purpose | Default host port |
| --- | --- | --- |
| PostgreSQL | Durable source of truth | `5432` |
| Kafka | Asynchronous integration events | `29092` |
| Redis | Short-lived deduplication and integration state | `6379` |
| Mailpit | Local SMTP and test email UI | SMTP `1025`, UI `8025` |

## Commands

From the repository root:

```powershell
Copy-Item .env.example .env
.\scripts\dev-up.ps1
.\scripts\dev-down.ps1
```

Use `.\scripts\dev-down.ps1 -Volumes` only when you intentionally want to
delete local named Docker volumes such as PostgreSQL data.

Note: PostgreSQL uses a named Docker volume. Kafka currently uses container
filesystem storage because the official Apache Kafka image could not write to
the mounted log directory in this Windows Docker setup. Kafka is not the source
of truth for business data in this MVP.
