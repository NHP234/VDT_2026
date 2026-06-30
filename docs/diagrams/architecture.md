# Architecture Diagrams

Diagrams use Mermaid so they can be reviewed in Git and reused in the report or
slide deck. They describe the current implemented modular-monolith-style demo:
two Spring Boot services plus local infrastructure, not a fully decomposed
microservice platform.

## C4 Context

```mermaid
flowchart LR
    agent["Customer-care agent"]
    customer["Facebook or email customer"]
    meta["Meta Graph API and Webhooks"]
    mail["Email mailbox / SMTP provider"]
    system["Omnichannel Customer Care System"]

    agent -->|"view, reply, assign, update status"| system
    customer -->|"Messenger, Page comments, email"| meta
    customer -->|"email"| mail
    meta -->|"webhooks / Graph API replies"| system
    mail -->|"IMAP inbound / SMTP outbound"| system
```

## C4 Container

```mermaid
flowchart TB
    agent["Customer-care agent"]
    frontend["React frontend\nVite + TypeScript"]
    inbox["Inbox service\nSpring Boot"]
    channel["Channel service\nSpring Boot"]
    postgres[("PostgreSQL\nsource of truth")]
    kafka[("Kafka\nintegration events")]
    redis[("Redis\nshort-lived dedup keys")]
    mailpit["Mailpit\nlocal SMTP demo"]
    meta["Meta Graph API\nreal or simulated"]

    agent -->|"browser"| frontend
    frontend -->|"REST /api/v1"| inbox
    channel -->|"inbox.message-received.v1"| kafka
    kafka -->|"consume inbound events"| inbox
    inbox -->|"outbox reply-request.v1"| kafka
    kafka -->|"consume reply requests"| channel
    channel -->|"delivery-result.v1"| kafka
    kafka -->|"consume delivery results"| inbox
    inbox -->|"JPA + Flyway"| postgres
    channel -->|"dedup before publish"| redis
    channel -->|"webhook simulator / real webhooks"| meta
    channel -->|"SMTP demo"| mailpit
```

## Inbox Service Component View

```mermaid
flowchart TB
    rest["REST controllers\nAuth, conversations, messages"]
    consumers["Kafka consumers\ninbound messages, delivery results"]
    app["Application services\nqueries, commands, ingestion, outbox"]
    domain["Domain rules\nstatus, assignment, message state"]
    persistence["Persistence adapters\nJPA repositories, Flyway schema"]
    outbox["Outbox publisher\nscheduled Kafka publication"]
    security["Security and API concerns\nBearer token, Problem Details, correlation ID"]
    postgres[("PostgreSQL")]
    kafka[("Kafka")]

    rest --> app
    consumers --> app
    app --> domain
    app --> persistence
    app --> outbox
    rest --> security
    consumers --> security
    persistence --> postgres
    outbox --> kafka
    kafka --> consumers
```

## Channel Service Component View

```mermaid
flowchart TB
    webhooks["Webhook and simulator controllers\nFacebook, email fixtures"]
    normalizers["Inbound normalizers\nFacebook, email"]
    dispatch["Inbound dispatch service\ncorrelation + dedup + publish"]
    delivery["Reply delivery service\nprovider selection + result publication"]
    providers["Provider adapters\nMeta Graph API, SMTP, simulated senders"]
    kafkaAdapters["Kafka producers/consumers"]
    redisAdapter["Redis deduplicator"]
    redis[("Redis")]
    kafka[("Kafka")]
    meta["Meta Graph API"]
    smtp["SMTP provider / Mailpit"]

    webhooks --> normalizers
    normalizers --> dispatch
    dispatch --> redisAdapter
    dispatch --> kafkaAdapters
    kafkaAdapters --> delivery
    delivery --> providers
    delivery --> kafkaAdapters
    redisAdapter --> redis
    kafkaAdapters --> kafka
    providers --> meta
    providers --> smtp
```

