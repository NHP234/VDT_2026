# Backend Workspace

Backend implementation belongs under this directory.

Expected services:

- `inbox-service`: authentication, conversations, messages, assignment, status,
  audit, metrics, and agent-facing REST APIs.
- `channel-service`: Facebook/email adapters, simulators, outbound delivery,
  Redis deduplication, and Kafka integration.
- `event-contracts`: shared Kafka topic and event envelope documentation.

Follow [`../docs/backend-structure.md`](../docs/backend-structure.md) before
creating packages, migrations, events, or APIs.

## Commands

Run backend tests from the repository root:

```powershell
.\scripts\check.ps1
```

Run a specific service test suite:

```powershell
cd backend/inbox-service
.\mvnw.cmd test

cd ..\channel-service
.\mvnw.cmd test
```

If `JAVA_HOME` is not set, point it at JDK 21 before running a service directly:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
```

The root `scripts/check.ps1` script sets this automatically for the current
machine when that JDK path exists.
