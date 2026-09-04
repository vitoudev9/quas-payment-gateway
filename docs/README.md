# Mini Payment Gateway

A self-learning project that simulates a simplified payment gateway using **Java 25, Quarkus, CockroachDB, Apache Kafka, and Docker**.

The goal is not to build a production-ready payment system. The goal is to understand the architecture and engineering problems behind real-world payment and fintech systems.

The project deliberately evolves from a simple transactional application into a distributed, event-driven system so that the difficult problems are learned rather than hidden behind frameworks.

---

## 1. Project Vision

Build a small but realistic payment gateway where a merchant can create a payment and receive a final result from a simulated external bank.

The system should remain correct even when things go wrong.

The central engineering question is:

> **How can we process money-related operations safely when our system is distributed and failures are inevitable?**

We will progressively introduce:

- REST APIs
- Payment state machines
- CockroachDB transactions
- Event-Driven Architecture
- Apache Kafka
- Asynchronous processing
- Idempotency
- Retry and failure handling
- Distributed transaction concepts
- Transactional Outbox
- Audit trails
- Refunds
- Settlement and reconciliation
- ETL pipelines
- AI-assisted fraud detection
- Automated testing
- Observability

---

## 2. What We Are Actually Learning

This is not a CRUD project.

We are using a payment gateway as a practical laboratory for learning:

### Backend Engineering

- Modern Java
- Quarkus
- REST API design
- Domain modeling
- State machines
- Database transactions
- Testing

### Distributed Systems

- Event-Driven Architecture
- Kafka producers and consumers
- Asynchronous processing
- Eventual consistency
- Idempotency
- Retry
- Dead-letter topics
- Partial failures
- Distributed transaction problems
- Transactional Outbox

### Fintech Engineering

- Payment lifecycle
- Payment attempts
- Authorization
- Refunds
- Auditability
- Settlement
- Reconciliation
- Financial correctness

### AI / Fraud

- Fraud signals
- Risk scoring
- Rule-based fraud detection
- Asynchronous AI processing
- Explainable fraud analysis
- AI limitations

---

## 3. Technology Stack

| Technology | Purpose | Status |
|---|---|---|
| Java 25 | Application language | Locked |
| Quarkus | Backend framework | Locked |
| CockroachDB | Transactional source of truth | Locked |
| Apache Kafka | Event distribution | Locked |
| Docker / Docker Compose | Local infrastructure | Locked |
| JUnit | Testing | Locked |
| Testcontainers | Integration testing | Locked |
| Maven / Gradle | Build | TBD |
| AI technology | Fraud detection | Later |
| Analytics database | Analytics | TBD |

The exact Java distribution, Quarkus version, build tool, and Quarkus extensions are Sprint 0 decisions.

---

## 4. Architecture

The initial architecture is a **modular monolith**.

We are deliberately not starting with six independent microservices.

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
        │ Transactional    │       │ Domain Events  │
        │ State            │       └───────┬────────┘
        │ Audit            │               │
        │ Idempotency      │       ┌───────┼─────────────┐
        │ Outbox           │       │       │             │
        └──────────────────┘       ▼       ▼             ▼
                               Processor  Fraud     Notification
                                   │
                                   ▼
                              ┌──────────┐
                              │ Fake Bank│
                              └──────────┘
```

The architecture will evolve only when there is a technical or learning reason to do so.

---

## 5. Why Modular Monolith First?

Starting with microservices would introduce infrastructure complexity before we understand the domain.

The modular monolith lets us focus on:

- Domain boundaries
- Transactions
- Payment correctness
- Kafka
- Failure behavior
- Idempotency
- Eventual consistency

Later, components may be extracted into services such as:

- Payment Service
- Payment Processor
- Fraud Service
- Notification Service

But we will not create those services just to make the project look distributed.

---

## 6. Payment Flow

A simplified successful flow:

```text
Merchant
   │
   │ POST /api/v1/payments
   ▼
Quarkus
   │
   ▼
Create Payment
   │
   ▼
PENDING
   │
   │ PaymentCreated
   ▼
Kafka
   │
   ▼
Payment Processor
   │
   ▼
Fake Bank
   │
   │ APPROVED
   ▼
Payment Processor
   │
   │ PaymentSucceeded
   ▼
Kafka
   │
   ▼
Payment Domain
   │
   ▼
SUCCEEDED
```

The Fake Bank can return:

```text
APPROVED
DECLINED
TIMEOUT
SERVER_ERROR
```

---

## 7. Payment Lifecycle

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

Invalid transitions must be rejected.

The domain explicitly controls valid state transitions.

---

## 8. CockroachDB as Source of Truth

CockroachDB answers:

> **What is the authoritative state?**

Initial data:

- Merchant
- Customer
- Payment
- PaymentAttempt
- Refund
- AuditEvent
- IdempotencyRecord
- OutboxEvent

Future:

- Settlement
- ReconciliationRecord

Kafka answers a different question:

> **What happened that other components may need to know about?**

Events do not replace the transactional source of truth.

---

## 9. Kafka

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

### Event Envelope

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

- `eventId` — unique event identifier
- `eventType` — event type
- `aggregateId` — associated business entity
- `occurredAt` — event timestamp
- `version` — schema version
- `payload` — event-specific data

---

## 10. Idempotency

There are two different idempotency problems.

### API Idempotency

A merchant may retry:

```http
POST /api/v1/payments
Idempotency-Key: ABC-123
```

The result must be:

```text
One request
    ↓
One Payment
```

not:

```text
Retry
  ↓
Two Payments
```

### Kafka Idempotency

Kafka consumers must tolerate duplicate delivery:

```text
PaymentCreated
PaymentCreated
PaymentCreated
```

The financial operation must still happen once.

The two idempotency problems are related but should be treated as separate concerns.

---

## 11. Distributed Transaction Problem

The application interacts with:

```text
Quarkus
   │
   ├── CockroachDB
   │
   └── Kafka
```

We may want:

```text
Save Payment
     +
Publish PaymentCreated
```

to behave atomically.

But a normal database transaction does not automatically include Kafka.

For example:

```text
BEGIN
  │
  ▼
Save Payment
  │
  ▼
COMMIT
  │
  X
Kafka fails
```

Now:

```text
CockroachDB → Payment exists
Kafka       → Event does not exist
```

The opposite failure can also occur.

This is one of the major distributed-systems learning exercises in the project.

---

## 12. Transactional Outbox

We will investigate the Transactional Outbox pattern:

```text
                 Quarkus
                    │
             ┌──────┴──────┐
             ▼             ▼
         Payment        Outbox Event
           Row              Row
             │             │
             └──────┬──────┘
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

Importantly, we will reproduce the failure first and then implement the solution.

The objective is to understand the trade-offs, not just memorize a pattern.

---

## 13. Failure-First Engineering

Failures are part of the design.

We will explicitly simulate:

- Duplicate Kafka messages
- Network timeouts
- External bank failures
- Consumer crashes
- Database failures
- Kafka failures
- Retry exhaustion
- Partial completion
- Bank success followed by application crash

For each scenario we should document:

```text
Failure
   ↓
Expected behavior
   ↓
Recovery mechanism
   ↓
Consistency impact
```

---

## 14. Retry and DLQ

Transient failures should be retried.

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

We will investigate:

- Retry count
- Exponential backoff
- Jitter
- Retryable errors
- Non-retryable errors
- Dead-letter topics
- Recovery and reprocessing

---

## 15. Auditability

We should be able to answer:

> **What happened to this payment?**

not only:

> **What is the current status?**

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

Important payment history should be persisted and traceable.

---

## 16. Refunds

Later:

```http
POST /api/v1/payments/{paymentId}/refund
```

Conceptual flow:

```text
RefundRequested
      ↓
Kafka
      ↓
Processor
      ↓
Fake Bank
      ↓
RefundSucceeded
```

Refunds will introduce additional state transitions and idempotency requirements.

---

## 17. Settlement and Reconciliation

Later, simulate an external provider sending settlement data.

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
```

Possible outcomes:

```text
MATCH
MISMATCH
MISSING_INTERNAL
MISSING_EXTERNAL
```

This gives us practical exposure to financial operations beyond payment authorization.

---

## 18. Analytics

Analytics is intentionally delayed until the transactional system is stable.

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
Reporting
```

The analytics database has not been selected yet.

We will choose it based on actual analytical requirements.

---

## 19. AI Fraud Detection

AI is introduced only after the core payment system works.

The intended model is:

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

Potential signals:

- Transaction amount
- Transaction frequency
- Customer history
- Merchant history
- Currency
- Country
- Device
- Time
- Previous failed transactions

Initial deterministic rules may be:

```text
Amount > threshold          +30
New country                 +20
High transaction velocity   +25
New customer relationship   +15
```

Possible decisions:

```text
LOW       → APPROVE
MEDIUM    → REVIEW
HIGH      → DECLINE
```

AI remains an asynchronous analytical capability.

> **AI is not the source of truth.**

Deterministic business rules and the transactional system remain authoritative.

---

## 20. Learning Roadmap

```text
Sprint 0
Foundation
    ↓
Sprint 1
Payment MVP
    ↓
Sprint 2
Kafka + Reliability
    ↓
Sprint 3
Refund + Audit + Settlement + Reconciliation
    ↓
Sprint 4
AI Fraud Detection
    ↓
Later
Analytics / Architecture Evolution
```

The deeper learning progression is:

```text
CRUD
  ↓
Transactions
  ↓
Payment Domain
  ↓
Events
  ↓
Asynchronous Processing
  ↓
Failures
  ↓
Distributed Consistency
  ↓
Financial Operations
  ↓
AI
```

---

## 21. Engineering Principles

### 1. Start Simple

Do not introduce microservices simply because Kafka is present.

### 2. Database Is the Source of Truth

Financial state must have an authoritative transactional source.

### 3. Events Represent Facts

Kafka events communicate what happened.

### 4. Assume At-Least-Once Delivery

Consumers must safely handle duplicates.

### 5. Financial Operations Must Be Idempotent

Retries must not move money twice.

### 6. Design for Failure

Failures are normal in distributed systems.

### 7. Audit Important Operations

We should be able to reconstruct what happened.

### 8. AI Is Not Financial Authority

AI can analyze and recommend but should not blindly control authoritative financial state.

### 9. Learn Before Abstracting

Understand the problem before introducing a sophisticated pattern.

---

## 22. Current Project Status

```text
Sprint 0 — Foundation & Architecture
Status: 🟡 Planning
```

### Immediate next step

Before implementing payment functionality:

1. Decide Maven vs Gradle.
2. Select the Java 25 distribution.
3. Select the Quarkus version.
4. Select required Quarkus extensions.
5. Define Docker Compose topology.
6. Define the local CockroachDB topology.
7. Define the local Kafka topology.
8. Then create the initial Quarkus project.

---

## 23. Disclaimer

This is an educational simulation.

It does not process real money, connect to real banks, or implement the security, compliance, regulatory, operational, or reliability requirements necessary for a production financial system.
