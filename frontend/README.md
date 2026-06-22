# Frontend Workspace

The React TypeScript app should be created here by the frontend agent.

Before editing this directory, read:

- [`../AGENTS.md`](../AGENTS.md)
- [`../docs/requirement.md`](../docs/requirement.md)
- [`../docs/frontend-structure.md`](../docs/frontend-structure.md)
- [`../docs/agent-collaboration.md`](../docs/agent-collaboration.md)

The frontend scope is limited to:

1. Login.
2. Unified inbox.

Do not add dashboards, settings, analytics, tags, notes, rich text, attachments,
or other out-of-scope UI.

## Expected Initial Setup

Gemini should create a Vite React TypeScript app in this directory and add:

- React Router.
- TanStack Query.
- ESLint and Prettier.
- Vitest and React Testing Library.
- API client modules matching `docs/requirement.md`.

See [`../docs/templates/gemini-frontend-setup-prompt.md`](../docs/templates/gemini-frontend-setup-prompt.md)
for a ready-to-use frontend prompt.
