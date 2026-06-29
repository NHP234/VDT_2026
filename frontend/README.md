# Frontend Workspace

React TypeScript Vite app for the agent-facing Omnichannel Customer Care workspace.

## Scope

The frontend is intentionally limited to:

1. Login.
2. Unified inbox.

Do not add dashboards, settings, analytics, tags, notes, rich text, attachments,
or other out-of-scope UI without updating `docs/requirement.md` first.

## Commands

```powershell
npm ci
npm run dev
npm run lint
npm test
npm run build
```

Use `VITE_API_BASE_URL` to point at a backend other than `http://localhost:8080`.

## Structure

```text
src/
|-- api/       # HTTP client and backend API modules
|-- app/       # providers and routing
|-- features/  # auth and inbox UI
|-- styles/    # global app CSS
|-- test/      # Vitest setup and component tests
`-- types/     # shared API/domain types
```

Before changing this app, read `../AGENTS.md`, `../docs/requirement.md`,
`../docs/frontend-structure.md`, and `../docs/agent-collaboration.md`.
