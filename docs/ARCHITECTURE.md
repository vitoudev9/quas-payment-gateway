# Mini Payment Gateway — Architecture

**Version:** 0.3  
**Status:** Architecture Aligned / Sprint 0 Ready  
**Last Updated:** 2026-09-04

## 1. Architecture Goal

The goal of this project is to build a small but realistic payment gateway while using it as a laboratory for learning modern backend and fintech architecture.

We are **not** trying to build a production-ready payment system. We are deliberately building a system that exposes us to the problems real financial systems must solve.

The central engineering question is:

> **How can we safely process money-related operations when our system is distributed and failures are inevitable?**

We want to progressively explore:

- Modern Java
- Quarkus
- Distributed SQL
- CockroachDB
- Event-Driven Architecture
- Apache Kafka
- Database transactions
- Distributed transaction problems
- Idempotency
- Eventual consistency
- Retry and failure recovery
- Auditability
- Refunds
- Settlement and reconciliation
- ETL
- AI-assisted fraud detection
- Automated testing
- Observability

The architecture must remain small enough to understand and evolve incrementally.

---

## 2. Technology Decisions

| Area | Decision | Status |
|---|---|---|
| Language | Java 25 | Locked |
| Framework | Quarkus | Locked |
| Primary Database | CockroachDB | Locked |
| Messaging | Apache Kafka | Locked |
| Infrastructure | Docker / Docker Compose | Locked |
| Testing | JUnit + Testcontainers | Locked |
| Application Architecture | Modular Monolith initially | Locked |
| Build Tool | Maven or Gradle | TBD |
| Java Distribution | TBD | TBD |
| Quarkus Version | TBD | TBD |
| Analytics Database | TBD | Later |
| AI Technology | TBD | Later |

The build tool, Java distribution, Quarkus version, exact Quarkus extensions, and local infrastructure topology will be settled during Sprint 0.

---

## 3. Architecture Philosophy

We are **not** building microservices simply because Kafka is being used.

The first implementation is a **modular monolith**.

This gives us a clean domain structure while avoiding premature complexity around:

- Service discovery
- Network communication
- Independent deployment
- Distributed configuration
- Multiple application runtimes

The architecture will evolve only when there is a clear technical or learning reason to introduce another service.

### Learning progression

```text
REST API
    ↓
Database Transactions
    ↓
Payment Domain
    ↓
Kafka / Event-Driven Architecture
    ↓
Asynchronous Processing
    ↓
Failure Handling
    ↓
Distributed Transaction Problems
    ↓
Reliability Patterns
    ↓
Refund / Audit / Settlement / Reconciliation
    ↓
Analytics
    ↓
AI Fraud Detection
```

The important principle is:

> **Learn the problem before abstracting the solution.**

---

## 4. High-Level Architecture

```text
                         ┌─────────────┐
                         │   Merchant  │
                         └──────┬──────┘
                                │
                               REST
                                │
                                ▼
                    ┌──────────────────────┐
                    │       Quarkus        │
                    │                      │
                    │ Payment API          │
                    │ Payment Domain       │
                    │ Merchant Domain      │
                    │ Banking              │
                    │ Messaging            │
                    │ Fraud                 │
                    └──────────┬───────────┘
                               │
                  ┌────────────┴────────────┐
                  │                         │
                  ▼                         ▼
        ┌──────────────────┐       ┌────────────────┐
        │   CockroachDB    │       │     Kafka      │
        │                  │       │                │
        │ Payments         │       │ Domain Events  │
        │ Merchants        │       └───────┬────────┘
        │ Attempts         │               │
        │ Audit            │       ┌───────┼───────────────┐
        │ Idempotency      │       │       │               │
        │ Outbox           │       ▼       ▼               ▼
        └──────────────────┘  Processor  Fraud      Notification
                                   │
                                   ▼
                              ┌──────────┐
                              │ Fake Bank│
                              └──────────┘
```

The initial system runs as one Quarkus application. Kafka and CockroachDB are external infrastructure components.

---

## 5. Modular Monolith Structure

Conceptually:

```text
payment-gateway/
│
├── payment/
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── api/
│
├── merchant/
│
├── banking/
│
├── messaging/
│
├── fraud/
│
└── common/
```

The exact Java package structure may evolve as the domain becomes clearer.

The important rule is that modules should have clear responsibilities and boundaries even though they initially run in one application.

---

## 6. CockroachDB — Transactional Source of Truth

CockroachDB is the authoritative transactional store.

Initial domain data includes:

- Merchant
- Customer
- Payment
- PaymentAttempt
- Refund
- AuditEvent
- IdempotencyRecord
- OutboxEvent

Future data may include:

- Settlement
- ReconciliationRecord

The key distinction is:

```text
CockroachDB
    ↓
"What is the authoritative state?"
```

while:

```text
Kafka
    ↓
"What happened that other components may need to know about?"
```

Kafka events do not replace the transactional source of truth.

### CockroachDB learning objectives

We will deliberately use CockroachDB to explore:

- Distributed SQL
- Replication
- Strong consistency
- Transaction isolation
- Serializable transactions
- Concurrent transactions
- Transaction retries
- Node failures
- Distributed data
- Horizontal scaling

---

## 7. Local Database Topology

### Initial

```text
Docker
  │
  └── CockroachDB
        └── Node 1
```

### Later experiment

```text
Docker
  │
  ├── cockroach-node-1
  ├── cockroach-node-2
  └── cockroach-node-3
```

The multi-node setup is a learning experiment. We will observe what happens when a node fails and returns.

Questions:

- Can payments continue?
- What happens to existing transactions?
- How does replication behave?
- What happens when a node returns?
- How does the application experience the failure?

---

## 8. Payment Domain

The payment lifecycle is explicitly modeled.

```text
CREATED
   │
   ▼
PENDING
   │
   ▼
PROCESSING
   │
   ├───────────────┐
   ▼               ▼
SUCCEEDED        FAILED
   │
   ▼
REFUNDED
```

The domain controls valid transitions.

Invalid transitions must be rejected.

The project should also preserve payment history so we can answer:

> What happened to this payment?

and not only:

> What is the payment's current status?

---

## 9. Initial Payment Flow

```text
Merchant
   │
   │ POST /api/v1/payments
   ▼
Quarkus
   │
   ├── Validate request
   │
   ├── Check idempotency
   │
   └── Persist Payment = PENDING
           │
           ▼
       PaymentCreated
           │
           ▼
         Kafka
           │
           ▼
   Payment Processor
           │
           ▼
       Fake Bank
       │    │    │
       │    │    └── SERVER_ERROR
       │    └─────── TIMEOUT
       └──────────── APPROVED / DECLINED
```

The final result is represented as a domain event and eventually becomes authoritative payment state in CockroachDB.

---

## 10. Kafka Architecture

Initial topics:

```text
payments.created
payments.processing
payments.succeeded
payments.failed
```

Future topics:

```text
payments.refund.requested
payments.refunded

fraud.analysis.requested
fraud.analysis.completed

settlement.received
reconciliation.completed
```

### Event envelope

All domain events should use a common envelope:

```json
{
  "eventId": "evt_123",
  "eventType": "PaymentCreated",
  "aggregateId": "pay_123",
  "occurredAt": "2026-09-04T14:00:00Z",
  "version": 1,
  "payload": {}
}
```

Important fields:

| Field | Purpose |
|---|---|
| `eventId` | Unique event identifier; useful for idempotency and tracing |
| `eventType` | Identifies the event |
| `aggregateId` | Business entity associated with the event |
| `occurredAt` | Time the event occurred |
| `version` | Supports event schema evolution |
| `payload` | Event-specific data |

Kafka should be treated as an **at-least-once delivery environment** for our learning model. Consumers must therefore be safe against duplicate delivery.

---

## 11. API Idempotency

A merchant may retry a request:

```http
POST /api/v1/payments
Idempotency-Key: ABC-123
```

If the same request is received again with the same key:

```text
One API operation
        ↓
One Payment
```

not:

```text
Retry
  ↓
Two Payments
```

The API idempotency problem is separate from Kafka consumer idempotency.

---

## 12. Kafka Consumer Idempotency

Kafka consumers must assume duplicate delivery can happen.

Example:

```text
PaymentCreated
PaymentCreated
PaymentCreated
```

The financial operation must still happen once.

A possible mechanism:

```text
processed_events
----------------
event_id
evt-123
evt-456
```

Conceptually:

```text
Receive event
     │
     ▼
Does eventId already exist?
     │
   ┌─┴─┐
  YES  NO
   │    │
 ignore process
        │
        ▼
   record eventId
```

The exact implementation will be learned and designed during the reliability sprint.

---

## 13. Distributed Transaction Problem

Our application interacts with at least two independent systems:

```text
Quarkus
   │
   ├──────────────► CockroachDB
   │
   └──────────────► Kafka
```

We may want:

```text
Save Payment
     +
Publish PaymentCreated
```

to behave atomically.

A normal database transaction does not automatically cover Kafka.

### Failure example

```text
BEGIN DB TRANSACTION
        │
        ▼
INSERT Payment
        │
        ▼
COMMIT
        │
        X
     Kafka fails
```

Result:

```text
CockroachDB → Payment exists
Kafka       → PaymentCreated was never published
```

The opposite failure is also possible:

```text
Kafka       → Event published
CockroachDB → Transaction rolled back
```

This creates a distributed consistency problem.

---

## 14. Transactional Outbox

One solution we will investigate is the Transactional Outbox pattern.

```text
                    Quarkus
                       │
               ┌───────┴────────┐
               ▼                ▼
           Payment           Outbox Event
             Row                 Row
               │                │
               └───────┬────────┘
                       ▼
                One DB Transaction
                       │
                       ▼
                 CockroachDB
                       │
                       ▼
                 Outbox Worker
                       │
                       ▼
                     Kafka
```

We will deliberately reproduce the failure first.

The purpose is to understand:

1. Why the problem exists.
2. Why the pattern works.
3. What new failure modes the pattern introduces.
4. How retries and duplicate publication must be handled.

---

## 15. Payment Processor

The processor consumes payment events and communicates with the Fake Bank.

```text
Kafka
  │
  ▼
PaymentCreated
  │
  ▼
Payment Processor
  │
  ▼
Fake Bank
```

Possible bank results:

```text
APPROVED
    ↓
PaymentSucceeded

DECLINED
    ↓
PaymentFailed

TIMEOUT
    ↓
Retry

SERVER_ERROR
    ↓
Retry
```

The exact retry policy will be designed during implementation.

---

## 16. Failure-First Engineering

Failures are part of the architecture, not edge cases added at the end.

We will explicitly test:

- Bank timeout
- Bank server error
- Duplicate Kafka event
- Consumer crash
- Bank success followed by application crash
- Database failure
- Kafka failure
- Partial completion
- Retry exhaustion

Each failure scenario should have a documented expected behavior.

---

## 17. Retry Strategy

Transient failures should be retried.

Example:

```text
Attempt 1 → timeout
      ↓
   backoff
      ↓
Attempt 2 → timeout
      ↓
   backoff
      ↓
Attempt 3 → success
```

Topics to investigate:

- Retry count
- Backoff
- Exponential backoff
- Jitter
- Retryable vs non-retryable errors
- Dead-letter topics
- Recovery procedures

The final policy remains an implementation decision.

---

## 18. Dead-Letter Topic

Messages that cannot be processed successfully after the retry policy should eventually move to a dead-letter topic.

```text
PaymentCreated
      │
      ▼
  Processor
      │
      ├── Retry
      ├── Retry
      ├── Retry
      │
      ▼
     DLQ
```

The DLQ is not simply a place to hide failures. We will learn how to inspect, recover, and safely reprocess failed messages.

---

## 19. Refunds

Refunds are introduced after the payment flow and reliability foundations are understood.

```http
POST /api/v1/payments/{paymentId}/refund
```

Conceptual flow:

```text
RefundRequested
      │
      ▼
    Kafka
      │
      ▼
  Processor
      │
      ▼
  Fake Bank
      │
      ▼
RefundSucceeded
```

Refunds will require their own idempotency and state-transition rules.

---

## 20. Audit Trail

We should persist important state transitions.

Example:

```text
Payment Created
      ↓
Payment Pending
      ↓
Payment Processing
      ↓
Payment Succeeded
      ↓
Refund Requested
      ↓
Refunded
```

The audit trail allows us to reconstruct what happened instead of relying only on the current status.

---

## 21. Settlement and Reconciliation

Later, an external provider will send simulated settlement data.

```text
External Provider
       │
       │ CSV / API
       ▼
      ETL
       │
       ▼
     Kafka
       │
       ▼
 Reconciliation
       │
   ┌───┴────┐
   ▼        ▼
 MATCH   MISMATCH
```

Possible classifications:

```text
MATCH
MISMATCH
MISSING_INTERNAL
MISSING_EXTERNAL
```

The reconciliation workflow should be designed as a separate financial capability rather than mixed into the basic payment transaction.

---

## 22. Analytics

Analytics comes after the transactional system is stable.

Conceptually:

```text
Kafka
  │
  ▼
ETL Pipeline
  │
  ▼
Analytics Database
  │
  ▼
Reporting / Analysis
```

The analytics database is intentionally **not selected yet**.

The choice should be based on actual analytical requirements rather than introducing a technology prematurely.

---

## 23. AI Fraud Detection

AI is introduced only after the core payment processing system works.

AI should operate as an asynchronous capability:

```text
                         Payment
                            │
                 ┌──────────┴──────────┐
                 │                     │
                 ▼                     ▼
        Payment Processing        Fraud Event
                 │                     │
                 ▼                     ▼
             Fake Bank            AI Analysis
                                       │
                                       ▼
                                   Risk Score
```

The AI component may:

- Detect anomalies
- Produce risk explanations
- Classify suspicious patterns
- Assist investigation
- Generate a risk score

AI is **not** the authoritative source of financial state.

Deterministic business rules and the transactional system remain authoritative.

---

## 24. Initial Fraud Model

Potential signals:

- Transaction amount
- Transaction frequency
- Customer history
- Merchant history
- Currency
- Country
- Device
- Time of transaction
- Previous failed transactions

A first implementation may use deterministic rules:

```text
Amount > threshold          +30
New country                 +20
High transaction velocity   +25
New customer relationship   +15
```

Then:

```text
Risk Score
    │
    ├── LOW    → APPROVE
    ├── MEDIUM → REVIEW
    └── HIGH   → DECLINE
```

Later, rules may be replaced or supplemented by ML/AI.

---

## 25. Testing Strategy

Testing is part of the architecture.

We will use:

- JUnit
- Mockito where appropriate
- Testcontainers
- Integration tests
- API tests
- Kafka integration tests
- Failure tests

Important principle:

> We should test not only the happy path, but also the failure behavior of the system.

Examples:

```text
Duplicate request
Duplicate event
Bank timeout
Consumer crash
Database unavailable
Kafka unavailable
Retry exhaustion
Partial completion
```

---

## 26. Observability

The project will progressively introduce:

- Structured logging
- Correlation IDs
- Health checks
- Metrics
- Payment traceability
- Event traceability

We should eventually be able to follow one payment across:

```text
API
 ↓
Database
 ↓
Kafka
 ↓
Processor
 ↓
Fake Bank
 ↓
Kafka
 ↓
Payment State
```

---

## 27. Architecture Evolution

### Phase 1 — Basic Payment

```text
Quarkus
   │
   ▼
CockroachDB
   │
   ▼
Basic Payment
```

### Phase 2 — Event Driven

```text
Quarkus
   │
   ├── CockroachDB
   │
   └── Kafka
          │
          ▼
    Payment Processor
          │
          ▼
       Fake Bank
```

### Phase 3 — Reliability

```text
Kafka
  │
  ├── Idempotency
  ├── Retry
  ├── Backoff
  ├── DLQ
  └── Failure Recovery
```

### Phase 4 — Financial Operations

```text
Payment
  │
  ├── Refund
  ├── Audit
  ├── Settlement
  └── Reconciliation
```

### Phase 5 — Analytics

```text
Kafka
  │
  ▼
ETL
  │
  ▼
Analytics DB
```

### Phase 6 — AI

```text
Payment Events
      │
      ▼
Fraud Analysis
      │
      ▼
AI / ML
      │
      ▼
Risk Score
```

---

## 28. When Should We Extract Microservices?

The modular monolith is intentional.

Possible future services include:

- Payment Service
- Payment Processor
- Fraud Service
- Notification Service

Extraction should happen only when there is a meaningful reason such as:

- Independent scaling requirement
- Clear ownership boundary
- Independent deployment need
- Failure isolation
- Technology isolation
- A deliberate distributed-systems learning experiment

We should not extract services merely to make the architecture diagram look more sophisticated.

---

## 29. Key Architectural Principles

1. **Start Simple**  
   Do not introduce microservices simply because the project uses Kafka.

2. **Database Is the Source of Truth**  
   Financial state must have an authoritative transactional source.

3. **Events Represent Facts**  
   Kafka communicates what happened to interested components.

4. **Assume At-Least-Once Delivery**  
   Consumers must safely handle duplicate events.

5. **Make Financial Operations Idempotent**  
   Retries must not accidentally perform a financial operation twice.

6. **Design for Failure**  
   External systems, databases, Kafka, and application instances can fail.

7. **Audit Important Operations**  
   We should be able to reconstruct what happened.

8. **Separate AI From Financial Authority**  
   AI can analyze and recommend; it should not blindly determine authoritative financial state.

9. **Learn Before Abstracting**  
   Understand the failure/problem before introducing a sophisticated pattern.

---

## 30. Architecture Questions We Will Investigate

### Database

- How does CockroachDB handle distributed transactions?
- What isolation behavior should we use?
- How do transaction retries work?
- What happens when a CockroachDB node fails?
- How should connection pooling be configured?

### Kafka

- How should topics be partitioned?
- What should the payment event key be?
- How do we guarantee ordering where required?
- How should retries work?
- When should an event go to the DLQ?
- How do we evolve event schemas?

### Transactions

- Where should transaction boundaries exist?
- When is Transactional Outbox appropriate?
- When should Kafka transactions be considered?
- How do we handle partial failures?
- Where is eventual consistency acceptable?

### Application

- When should modules become separate services?
- How should APIs be versioned?
- How should correlation IDs be propagated?
- How do we observe one payment across components?

### Financial Domain

- How should payment attempts be modeled?
- How should refunds work?
- How should settlement work?
- How should reconciliation detect discrepancies?
- Should we introduce a double-entry ledger?

### AI

- Should fraud analysis be synchronous or asynchronous?
- Which signals should be used?
- Should we start with rules, ML, or an LLM?
- How should AI decisions be explained?
- What happens when the AI service is unavailable?

---

## 31. Current Locked Decisions

```text
Language        → Java 25
Framework       → Quarkus
Database        → CockroachDB
Messaging       → Apache Kafka
Infrastructure  → Docker / Docker Compose
Testing         → JUnit + Testcontainers
Architecture    → Modular Monolith initially
AI              → Fraud Detection, later
Analytics DB    → TBD
Build Tool      → TBD
```

---

## 32. Sprint 0 Decisions

Before implementation starts, Sprint 0 must settle:

1. Maven vs Gradle
2. Exact Java 25 distribution
3. Quarkus version
4. Required Quarkus extensions
5. Docker Compose topology
6. CockroachDB local topology
7. Kafka local topology
8. Initial database schema
9. Initial API contract
10. Initial Kafka event contract
11. Local development commands
12. Test strategy and Testcontainers setup

---

## 33. Guiding Question

Throughout the project, keep returning to:

> **How can we safely process money-related operations when our system is distributed and failures are inevitable?**

Every architectural decision, experiment, test, and feature should help answer that question.
