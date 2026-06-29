# Omnichannel Customer Care System

Mini project for Viettel Digital Talent 2026. The product scope is locked in
[`docs/requirement.md`](docs/requirement.md).

## Current Status

The repository has base documentation, local infrastructure, backend Spring Boot
services, and a React TypeScript frontend scaffold. The current vertical slice
covers backend authentication, conversation APIs, assignment/status updates,
reply queueing, retry, and a frontend login/unified inbox workspace.

## Quick Start

From PowerShell:

```powershell
Copy-Item .env.example .env
.\scripts\dev-up.ps1
.\scripts\check.ps1
.\scripts\dev-down.ps1
```

Local infrastructure defaults:

| Service | URL or port |
| --- | --- |
| PostgreSQL | `localhost:5432` |
| Redis | `localhost:6379` |
| Kafka | `localhost:29092` |
| Mailpit SMTP | `localhost:1025` |
| Mailpit UI | `http://localhost:8025` |

`scripts/check.ps1` validates Docker Compose, runs both backend Maven Wrapper
test suites, and runs frontend install/lint/test/build checks.

Backend run commands:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'

cd backend\inbox-service
.\mvnw.cmd spring-boot:run

cd ..\channel-service
.\mvnw.cmd spring-boot:run
```

Frontend dev commands:

```powershell
cd frontend
npm ci
npm run dev
```

Set `VITE_API_BASE_URL` when the Inbox service is not running at
`http://localhost:8080`. Set `FRONTEND_ALLOWED_ORIGINS` for backend CORS when
the frontend dev server uses a non-default origin.

## Verified Setup Checks

Current verified checks:

- `docker compose --env-file .env.example config`
- `.\scripts\check.ps1`
- `backend/inbox-service`: Maven Wrapper test suite.
- `backend/channel-service`: Maven Wrapper test suite.
- `frontend`: Vitest unit tests, ESLint checker, and build compilation.
- PostgreSQL readiness probe inside the container.
- Redis `PING`.
- Kafka topic-list probe inside the container.
- Mailpit HTTP response at `http://localhost:8025`.

## Documentation Map

| File | Purpose |
| --- | --- |
| [`AGENTS.md`](AGENTS.md) | Global engineering, architecture, security, and collaboration rules for human and AI agents. |
| [`docs/requirement.md`](docs/requirement.md) | Product source of truth. Requirements here override all other documents. |
| [`docs/project-management.md`](docs/project-management.md) | Working cadence, tracking rules, and how to keep project documents current. |
| [`docs/progress-checklist.md`](docs/progress-checklist.md) | Delivery checklist mapped to requirement IDs. Update this for planned progress. |
| [`docs/changelog.md`](docs/changelog.md) | Human-readable summary of completed work, including unplanned or out-of-checklist changes. |
| [`docs/backend-structure.md`](docs/backend-structure.md) | Backend service, package, module, API, persistence, and test structure. |
| [`docs/frontend-structure.md`](docs/frontend-structure.md) | Frontend structure and rules for the Gemini frontend agent. |
| [`docs/agent-collaboration.md`](docs/agent-collaboration.md) | Handoff and ownership rules between Codex, Gemini, and the human owner. |
| [`docs/monitoring-and-operations.md`](docs/monitoring-and-operations.md) | Runtime health, logs, metrics, demo checks, and operational tracking. |
| [`docs/traceability.md`](docs/traceability.md) | Requirement-to-implementation-to-test traceability table. |
| [`docs/templates/gemini-frontend-setup-prompt.md`](docs/templates/gemini-frontend-setup-prompt.md) | Ready-to-use prompt for the frontend agent's initial React setup. |
| [`docs/adr/`](docs/adr/) | Architecture Decision Records. |
| [`docs/diagrams/`](docs/diagrams/) | Architecture and sequence diagrams. |
| [`docs/report/`](docs/report/) | Report source files and evidence. |
| [`docs/slides/`](docs/slides/) | Presentation source files and evidence. |

## Working Rule

Before adding implementation work, identify the requirement ID from
`docs/requirement.md`, update the progress checklist, and keep the changelog and
traceability table synchronized.
