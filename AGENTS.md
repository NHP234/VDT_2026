# AGENTS.md

## 1. Project Context

### Project name

Omnichannel Customer Care System for Facebook and Email.

### Program

Viettel Digital Talent 2026 - Mini Project.

### Problem statement

Build a small but realistic customer-care platform that collects Facebook Page
messages/comments and email into one workspace. Customer-care agents can view,
reply to, assign, and track conversations without switching between channels.

### Learning objectives

- Understand the architecture of an omnichannel customer-care system.
- Integrate with Facebook through the Meta Graph API and Webhooks.
- Understand email protocols and integration approaches: SMTP, IMAP, POP3, and
  provider APIs where appropriate.
- Apply architecture and design patterns deliberately.
- Use Redis and Kafka where they solve concrete performance, reliability, or
  integration problems.
- Produce a working demo, an approximately 15-page report, and an approximately
  15-slide presentation.

### Expected MVP

1. Receive Facebook Page messages and comments through webhooks.
2. Receive email messages through IMAP or a provider adapter.
3. Normalize all inbound events into a common conversation model.
4. Show a unified inbox with filtering by channel and status.
5. View conversation history and customer information.
6. Reply through the original channel.
7. Assign conversations to agents and update their status.
8. Preserve an audit trail and basic operational metrics.
9. Provide mocks/simulators when real Facebook or email credentials are not
   available.

### Non-goals for the MVP

- Full CRM, chatbot, call-center, or marketing automation functionality.
- Production-scale multi-tenancy, billing, or advanced analytics.
- Premature decomposition into many independently deployed services.
- Exactly-once delivery across external systems. We target at-least-once
  processing with idempotent consumers.

## 2. Working Principles

- Prefer a small, demonstrable vertical slice over a broad unfinished system.
- Every technology must have a documented reason to exist.
- Keep business logic independent from Facebook, email, databases, and message
  brokers through ports/interfaces and adapters.
- Optimize first for correctness, clarity, observability, and demo reliability.
- Make incremental changes that remain runnable and testable.
- `docs/requirement.md` is the source of truth for product scope. When this file
  and another document conflict about features, `docs/requirement.md` wins.
- Use `docs/project-management.md`, `docs/progress-checklist.md`,
  `docs/changelog.md`, and `docs/traceability.md` to track planned work,
  completed work, unplanned changes, and requirement coverage.
- If a change is outside the current progress checklist, record it in
  `docs/changelog.md` as unplanned work and explain the requirement or
  maintenance reason.
- Do not silently change requirements or architecture decisions. Record
  important decisions in `docs/adr/`.
- When requirements are ambiguous, inspect the repository and existing ADRs
  first. Ask only when a wrong assumption would cause meaningful rework.
- Code, identifiers, commits, API fields, and technical documents use English.
  User-facing copy may use Vietnamese.

## 3. Default Technology Stack

These are defaults, not excuses to force unnecessary infrastructure. A change
to a major choice should be captured in an Architecture Decision Record (ADR).

### Backend

- Java 21 LTS.
- Spring Boot 3.x.
- Maven Wrapper.
- Spring Web, Validation, Data JPA, Security, Actuator.
- Flyway for database migrations.
- MapStruct only when mapping becomes repetitive; otherwise use explicit
  mapping code.
- springdoc-openapi for REST API documentation.

### Frontend

- React with TypeScript.
- Vite.
- React Router.
- TanStack Query for server state.
- A lightweight component library may be selected once and used consistently.
- Vitest and React Testing Library.

### Data and infrastructure

- PostgreSQL as the source of truth.
- Redis for short-lived cache, rate limiting, distributed coordination, or
  ephemeral state. Redis must not become the only store for business data.
- Apache Kafka for asynchronous integration events and decoupled processing.
  Do not use Kafka for simple synchronous CRUD.
- Docker Compose for the local environment.
- Mailpit for local SMTP testing.
- Meta webhook simulator and email fixtures for deterministic demos.

### Testing and quality

- JUnit 5, AssertJ, Mockito, and Spring Boot Test.
- Testcontainers for integration tests that need PostgreSQL, Redis, or Kafka.
- ESLint and Prettier for frontend code.
- JaCoCo may report coverage, but meaningful behavior assertions matter more
  than a coverage percentage.

### Observability

- Structured logs with correlation/trace IDs.
- Spring Boot Actuator health and metrics endpoints.
- Micrometer with Prometheus-compatible metrics when metrics are introduced.
- Never log access tokens, passwords, email bodies containing sensitive data,
  or unnecessary personally identifiable information.

## 4. Architecture Direction

Start as a modular system with independently owned business modules. Deployable
services may be extracted only when the boundary and operational benefit are
clear. For the mini project, a small number of services is preferable to a
diagram full of empty services.

Suggested logical boundaries:

- `identity`: users, roles, authentication, and authorization.
- `inbox`: conversations, messages, assignment, status, and agent workflows.
- `facebook-adapter`: Meta webhook verification, inbound events, and replies.
- `email-adapter`: inbound email polling/webhooks and outbound SMTP/provider API.
- `customer`: normalized customer identity and channel identities.
- `notification`: optional asynchronous agent notifications.
- `reporting`: operational metrics and read models when needed.

Architecture rules:

- Apply Clean/Hexagonal Architecture pragmatically:
  - `domain`: business concepts and rules; no framework dependencies where
    practical.
  - `application`: use cases, commands/queries, and ports.
  - `infrastructure`: JPA, Kafka, Redis, Meta, email, and other adapters.
  - `interfaces`: REST controllers, webhook endpoints, and event consumers.
- Dependencies point inward toward domain/application logic.
- Controllers validate transport concerns and delegate to use cases; they do not
  contain business workflows.
- Repositories expose domain-oriented operations, not arbitrary database access.
- External payload models must not leak into the domain model.
- Events are versioned and contain stable IDs, timestamps, and correlation IDs.
- Use the transactional outbox pattern when database state and Kafka publication
  must be coordinated.
- Consumers must be idempotent because Facebook, email, and Kafka can redeliver.
- Use retries with bounded exponential backoff. Non-recoverable events go to a
  dead-letter topic or an auditable failure store.
- Prefer REST for immediate request/response behavior and Kafka for asynchronous
  facts that have already happened.

## 5. Domain Conventions

Core concepts should use consistent names:

- `Channel`: `FACEBOOK` or `EMAIL`.
- `Conversation`: an agent-facing thread associated with a customer and channel.
- `Message`: inbound or outbound content within a conversation.
- `ExternalMessageId`: provider-specific ID used for deduplication.
- `Customer`: normalized person or organization.
- `ChannelIdentity`: Facebook PSID/profile or email address linked to a customer.
- `Assignment`: current agent/team ownership.
- `ConversationStatus`: `OPEN`, `PENDING`, or `RESOLVED` for the locked MVP.

Rules:

- Store timestamps in UTC and serialize them as ISO 8601. Convert to local time
  only in the UI.
- Use UUIDs for internal aggregate identifiers unless an ADR chooses otherwise.
- Preserve provider IDs separately; never use them as universal internal IDs.
- Make inbound event handling idempotent using provider event/message IDs.
- Message direction is explicit: `INBOUND` or `OUTBOUND`.
- Raw provider payloads may be retained for debugging with a retention policy
  and sensitive-field handling.

## 6. API and Event Rules

### REST

- APIs are rooted at `/api/v1`.
- Webhook endpoints are rooted at `/webhooks/{provider}`.
- Use nouns for resources and HTTP methods for actions.
- Return consistent error responses using RFC 9457 Problem Details where
  practical.
- Validate all external input at the boundary.
- Use pagination for collections that can grow.
- Do not expose JPA entities directly.
- Keep OpenAPI definitions synchronized with implemented behavior.

### Kafka

- Topic names follow `<domain>.<event>.v<version>`, for example
  `inbox.message-received.v1`.
- Event names use past tense because events describe facts.
- Event schemas are backward compatible within a version.
- Include `eventId`, `eventType`, `occurredAt`, `correlationId`, `source`, and
  payload.
- Do not place secrets or unnecessary personal data in events.

## 7. Clean Code Rules

- Follow SOLID where it improves changeability; do not create interfaces or
  abstractions without a real boundary.
- Prefer clear, domain-specific names over comments.
- Keep functions focused and keep classes cohesive.
- Avoid boolean parameters that obscure intent; use enums or separate methods.
- Avoid deep nesting with guard clauses and extracted behavior.
- Do not use magic strings or numbers for business rules.
- Prefer immutable data and explicit state transitions.
- Handle errors intentionally. Never swallow exceptions or catch generic
  exceptions without adding value.
- Do not return `null` collections. Use empty collections.
- Use `Optional` for potentially absent return values, not for entity fields or
  method parameters.
- Avoid static mutable state and hidden global dependencies.
- Do not duplicate business rules across controllers, consumers, and services.
- Comments explain why a non-obvious decision exists, not what obvious code does.
- Remove dead code instead of commenting it out.
- Keep pull requests and commits focused on one coherent change.

## 8. Security and Privacy

- All secrets come from environment variables or an ignored local secret file.
- Commit `.env.example`, never `.env` with real values.
- Verify Meta webhook signatures and verification tokens.
- Validate sender, recipient, MIME type, attachment size, and content boundaries
  for email.
- Sanitize rendered HTML email and never inject provider HTML directly into the
  UI.
- Enforce authorization server-side for every protected operation.
- Apply least privilege to database users, Meta permissions, and email accounts.
- Encrypt traffic in production-like environments.
- Mask tokens and personal data in logs and screenshots used in reports.
- Document demo data retention and deletion behavior.
- Dependency vulnerabilities rated critical or high must be addressed or
  explicitly documented before the final demo.

## 9. Testing Strategy

Use the smallest test that proves the behavior:

- Unit tests for domain rules, state transitions, and application services.
- Integration tests for repositories, migrations, Kafka consumers/producers,
  Redis behavior, and external adapter contracts.
- API tests for validation, authorization, error responses, and key workflows.
- Frontend component tests for important states and interactions.
- A small end-to-end smoke test for the main demo flow.

Every bug fix should include a regression test when reasonably possible.

Critical scenarios:

- Duplicate Facebook webhook delivery does not duplicate a message.
- Duplicate email retrieval does not duplicate a message.
- A reply is associated with the correct conversation and channel identity.
- Failed outbound delivery is visible and retryable.
- Unauthorized agents cannot access or mutate protected data.
- Conversation assignment/status transitions follow domain rules.
- Service restart does not lose accepted business data.

Tests must not depend on real Meta or email accounts in CI.

## 10. Repository Conventions

Proposed top-level structure:

```text
.
|-- backend/
|-- frontend/
|-- infra/
|-- docs/
|   |-- requirement.md
|   |-- adr/
|   |-- diagrams/
|   |-- report/
|   `-- slides/
|-- scripts/
|-- .env.example
|-- docker-compose.yml
|-- README.md
`-- AGENTS.md
```

- Keep commands reproducible through wrappers and repository scripts.
- Database changes require a new Flyway migration; never edit an applied
  migration.
- Generated files must be clearly identified and excluded from manual edits.
- Do not commit build output, IDE metadata, local databases, logs, or secrets.
- Keep the root `README.md` current with setup, run, test, and demo instructions.
- Keep `docs/backend-structure.md` and `docs/frontend-structure.md` synchronized
  with the actual repository layout as implementation evolves.

## 11. Git Workflow

- Branch naming: `feat/<name>`, `fix/<name>`, `docs/<name>`, or
  `chore/<name>`.
- Use Conventional Commits, for example:
  - `feat(inbox): add conversation assignment`
  - `fix(facebook): deduplicate webhook events`
  - `docs(adr): record Kafka usage decision`
- A commit should build and its relevant tests should pass.
- Do not mix broad refactoring with a feature unless required for that feature.
- Never rewrite or discard another contributor's uncommitted work.

## 12. Definition of Done

A task is done when:

- Acceptance behavior is implemented.
- Relevant automated tests pass.
- Formatting, linting, and static checks pass.
- Error handling, security, and observability were considered.
- API/event documentation is updated when contracts change.
- Migrations and configuration examples are included when needed.
- The feature works using local Docker-based dependencies or documented mocks.
- The main flow has been manually verified when it affects the demo.
- Important architectural decisions and trade-offs are recorded.

## 13. Documentation and Deliverables

Maintain documentation as the system evolves, not only at the end:

- ADRs for major choices such as modular monolith vs microservices, Kafka,
  Redis, authentication, Meta integration, and email ingestion.
- C4-style context/container/component diagrams where useful.
- Sequence diagrams for Facebook inbound, email inbound, and outbound reply.
- A traceability table connecting requirements, implementation, tests, and demo.
- Screenshots must use non-sensitive demo data.
- Record limitations honestly, especially those caused by Meta app review,
  permissions, or provider sandbox restrictions.

The report should explain trade-offs and evidence, not merely list technology.
The slide deck should emphasize the problem, architecture, key flows, demo,
results, limitations, and lessons learned.

## 14. Collaboration Rules for Human and AI

- The human owns product decisions, credentials, external accounts, and final
  submission.
- The AI should inspect existing code and documentation before editing.
- The AI should implement, test, and document requested changes end to end when
  feasible, rather than stopping at suggestions.
- The AI must preserve unrelated local changes and must not run destructive Git
  or filesystem commands without explicit instruction.
- The AI should state assumptions and surface material risks early.
- The AI should keep changes scoped and explain meaningful trade-offs.
- The AI must not fabricate successful API calls, tests, metrics, screenshots,
  or integration results.
- For external APIs, verify behavior against current official documentation
  before implementation because Meta and email-provider APIs can change.
- New dependencies require a concrete benefit and compatibility/license check.
- When a shortcut is taken for the mini project, document the production-grade
  alternative and why the shortcut is acceptable for the demo.
- All agents must follow `docs/agent-collaboration.md` for ownership, handoff,
  and cross-agent coordination.
- Frontend work is expected to be performed by Gemini or another frontend agent.
  That agent must read `docs/frontend-structure.md` before editing `frontend/`
  and must keep frontend work within the locked two-screen UI scope.
- Backend agents must read `docs/backend-structure.md` before creating or
  changing backend services, packages, migrations, events, or APIs.
- API contract changes must be reflected in backend code or documentation,
  frontend API expectations, `docs/traceability.md`, and `docs/changelog.md`.

## 15. Initial Delivery Order

Unless requirements change, implement in this order:

1. Repository skeleton, Docker Compose, CI checks, and base documentation.
2. Inbox domain model and persistence.
3. Unified inbox REST API and minimal frontend.
4. Facebook webhook simulator, then real Meta adapter.
5. Email simulator/Mailpit, then real IMAP and SMTP/provider adapter.
6. Idempotency, Kafka event flow, retries, and failure visibility.
7. Redis-backed optimization with measured or clearly demonstrated value.
8. Authentication, authorization, audit, and operational metrics.
9. End-to-end demo hardening.
10. Report, diagrams, screenshots, and slides.
