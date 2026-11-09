# Architecture notes

This document explains the decisions behind SupportIQ, the alternatives considered and what would
change at production scale. It is intentionally opinionated: a portfolio project should show
judgement, not only features.

## 1. Shape: modular monolith

SupportIQ is a single deployable unit split into feature packages that behave like modules:

| Module | Responsibility | Owns |
| --- | --- | --- |
| `customer` | customer records | `customers` |
| `ticket` | ticket lifecycle, messages | `tickets`, `ticket_messages` |
| `knowledge` | ingestion, retrieval | `knowledge_documents` + vectors |
| `assistant` | RAG orchestration, tools, triage | — (orchestrates) |
| `conversation` | transcripts and citations | `ai_conversations` |
| `analytics` | usage read model | — (aggregates MongoDB) |
| `shared` | errors, events, audit, security | `audit_events` |

Why not microservices? The workflows are tightly coupled (a ticket answer needs the ticket, the
knowledge base and a transcript). A monolith keeps the transaction and failure boundaries visible,
which is exactly what an interviewer will ask about. The ports (`ChatPort`, `EventPublisher`,
`ChunkingStrategy`) are the seams along which it could be split later.

**Enforced by ArchUnit:** controllers may not touch repositories, services may not depend on
controllers, and feature packages must not form cycles.

## 2. Why three datastores?

### MySQL — system of record

Tickets need uniqueness (`reference`), referential integrity, optimistic locking (`@Version`) and
transactional status changes. These are exactly the guarantees a relational database provides.

### MongoDB — transcripts and audit

A conversation is append-heavy, unbounded and read as one aggregate. Citations are nested value
objects that vary per turn. A relational schema would need `conversations`, `turns` and
`turn_citations` tables joined on every read, for no integrity benefit. MongoDB stores the aggregate
once, and `$push` appends atomically. Audit payloads are heterogeneous by nature — a document store
fits them better than a widening table.

### PostgreSQL + pgvector — embeddings

pgvector provides ANN indexes (HNSW) with cosine distance and JSON metadata filters. Spring AI's
`PgVectorStore` is mature and speaks the portable `VectorStore` API, so the assistant is not coupled
to it.

**Key decision:** the application uses **two relational datasources**. Spring Boot auto-configures
MySQL for JPA; `VectorStoreConfig` builds a separate, small Hikari pool and `JdbcTemplate` for
pgvector. Benefits: independent scaling, backups and credentials; a vector query cannot exhaust the
MySQL connection pool. Cost: one more pool and explicit configuration. The pgvector auto-configuration
is excluded so there is exactly one `VectorStore` bean.

## 3. RAG pipeline

```
POST /assistant/chat
  → load/create conversation (MongoDB)
  → append USER turn
  → embed question + similarity search (pgvector, top-k, threshold)
  → build numbered context block (deterministic formatting)
  → ChatClient: system prompt + last N turns + augmented question + tools
  → parse answer + token usage
  → append ASSISTANT turn with citations (MongoDB)
  → publish AiAnswerGeneratedEvent
  → return answer + citations + usage
```

Design choices:

- **Explicit retrieval instead of an opaque advisor.** The orchestrator sees the chunks, so it can
  return citations, log retrieval quality and apply per-customer filters later. The framework still
  does the hard part: embedding, storage and similarity search.
- **Prompt in a resource file** (`prompts/support-system.st`), not a Java string. Prompts are
  versioned artefacts that reviewers can diff.
- **Grounding rule in the prompt:** if context is missing, say so; never invent policy.
- **Citations are stored with the answer.** An audit reviewer can reproduce which passage supported
  which claim even after the knowledge base changes.

## 4. Ingestion consistency (two stores, no distributed transaction)

1. Normalise and hash the content (SHA-256). A `READY` or `PROCESSING` document with that hash is
   returned as-is; a `FAILED` one is re-armed for retry.
2. Commit metadata with status `PROCESSING`.
3. Chunk and embed, then write vectors.
4. Mark `READY` with the chunk count, or `FAILED` with a truncated reason. Vector ids are
   `documentId:chunkIndex`, so a retry overwrites rather than duplicates embeddings.
5. Publish `DocumentIngested`.

**Why not one transaction?** MySQL and PostgreSQL cannot share an ACID transaction without XA, and
holding a database transaction open during a remote embedding call is an anti-pattern (connection
starvation, lock timeouts). The explicit state machine makes the failure window observable and
retryable. The production upgrade is a transactional outbox plus a compensating cleanup job.

Idempotency is enforced by the unique content hash — important because AI clients retry.

## 5. Resilience

`ResilientChatPort` decorates `SpringAiChatPort`:

- circuit breaker: 50% failure rate over 10 calls, 5-call minimum, 30s open, 2 half-open probes;
- retry: bounded attempts with backoff for transport and 5xx errors;
- open breaker → `503` with `AiServiceUnavailableException` instead of tying up request threads;
- breaker state and a gauge are exposed to Micrometer and `/actuator/health`.

Streaming is intentionally not breaker-wrapped at the token level: failures arrive after
subscription, so mapping them to a retry would replay partial output. The stream maps errors to the
same 503 problem contract.

## 6. Events and messaging

`EventPublisher` is a port with two adapters:

| Profile | Adapter | Delivery |
| --- | --- | --- |
| default | Spring `ApplicationEventPublisher` | in-process, synchronous publish, async consumer |
| `gcp` | Google Cloud Pub/Sub | durable, at-least-once |

The audit listener is `@Async` on virtual threads, so audit writes never add latency to the request
and cannot roll back business work. Pub/Sub push deliveries are verified with a shared-secret header
and fail closed when the secret is unset.

**Known limitation:** the default adapter is at-most-once. Durable delivery needs the outbox pattern.

## 7. Security model

- JWT (HS256) resource server, stateless, no sessions.
- `roles` claim → `ROLE_*` authorities; actuator admin endpoints require `ROLE_ADMIN`.
- Development token endpoint is conditional on a property and must be disabled in production.
- MCP is a separate unauthenticated-by-default transport; the application adds `X-MCP-API-Key`
  checking because the protocol itself does not authenticate.
- Secrets come from the environment only. Nothing sensitive is logged; the AI health indicator
  reports only whether a key is configured.

**Production hardening:** asymmetric keys (RS256) with JWKS rotation, OAuth2 scopes per operation,
rate limiting, per-tenant authorization, PII redaction before embedding, prompt-injection defences
(treat retrieved text as untrusted), and egress allow-lists for the model provider.

## 8. Observability

- Correlation id (`X-Request-Id`) in MDC on every request, propagated into logs.
- Custom metrics: request counts, token consumption, latency histograms, stream completions.
- Health: MySQL/Mongo/pgvector indicators from Spring Boot plus a custom `ai` component.
- Actuator exposure is explicit rather than `*`; `env` and `beans` stay closed.
- Audit trail in MongoDB gives a business-level record that metrics cannot.

## 9. Alternatives considered

| Decision | Alternative | Why not |
| --- | --- | --- |
| PostgreSQL for vectors | MySQL vector extension, Pinecone | pgvector is open, local, and Spring AI supports metadata filters; no extra vendor |
| MongoDB for transcripts | MySQL JSON column | aggregation and atomic array appends are cleaner in MongoDB |
| Explicit RAG | `QuestionAnswerAdvisor` | less control over citations, filters and retrieval metrics |
| Spring events default | Kafka | operating Kafka is unnecessary for a local demo; Pub/Sub covers the cloud story |
| OpenAI-compatible client | provider-specific SDKs | any gateway or local model works by changing `OPENAI_BASE_URL` |
| Manual Docker Compose | Testcontainers by default | `./mvnw verify` must work without a Docker daemon; ITs are the next step |

## 10. What changes at 10x traffic

1. **Outbox + idempotent consumers** for events and ingestion compensation.
2. **Read replicas** for ticket queries; CQRS read model if filtering grows.
3. **pgvector tuning**: IVFFlat vs HNSW trade-offs, `maintenance_work_mem`, partition by tenant.
4. **Model cost controls**: per-tenant budgets, caching of embeddings, smaller models for triage.
5. **Async ingestion API** with a job resource instead of a blocking request.
6. **Rate limiting and queueing** in front of the model, with backpressure on streaming.
7. **OpenTelemetry** traces spanning HTTP → retrieval → model → datastores.
8. **Multi-tenancy** with mandatory metadata filters and row-level authorization.
