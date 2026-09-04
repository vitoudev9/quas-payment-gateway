# Mini Payment Gateway — Project Plan

**Version:** 0.3  
**Status:** Aligned with Architecture v0.3  
**Last Updated:** 2026-09-04

## 1. Project Methodology

This project follows a lightweight Agile approach.

Each sprint has:

- Sprint Goal
- User Stories
- Tasks
- Acceptance Criteria
- Definition of Done
- Retrospective

The plan is intentionally iterative. Architectural decisions may change as we learn, but changes should be reflected in `ARCHITECTURE.md`, `README.md`, and this file.

---

## 2. Product Goal

Build a simplified payment gateway that demonstrates realistic fintech engineering concepts.

The system should allow:

- A merchant to create a payment.
- The system to process the payment asynchronously.
- A simulated bank to approve or reject the payment.
- The system to safely handle retries and duplicate messages.
- The merchant to query payment status.
- The system to maintain an auditable history.
- The system to support refunds.
- The system to reconcile internal transactions with external settlement data.
- An AI component to assist with fraud detection.

The system is educational and will not process real money.

---

## 3. Architecture Alignment

The project plan follows these locked architectural decisions:

| Area | Decision |
|---|---|
| Language | Java 25 |
| Framework | Quarkus |
| Database | CockroachDB |
| Messaging | Apache Kafka |
| Infrastructure | Docker / Docker Compose |
| Architecture | Modular Monolith initially |
| Testing | JUnit + Testcontainers |
| AI | Later |
| Analytics DB | TBD |
| Build Tool | TBD |

The earlier Spring Boot / PostgreSQL foundation tasks are intentionally replaced by Quarkus / CockroachDB tasks so the project plan matches the current architecture.

---

## 4. Agile Roadmap

| Sprint | Goal | Priority |
|---|---|---|
| Sprint 0 | Foundation & Architecture | Must Have |
| Sprint 1 | Payment MVP | Must Have |
| Sprint 2 | Event-Driven Architecture & Reliability | Must Have |
| Sprint 3 | Fintech Features | Should Have |
| Sprint 4 | AI Fraud Detection | Should Have |
| Later | Analytics / Architecture Evolution | Optional |

---

# Sprint 0 — Foundation & Architecture

## Sprint Goal

Create a runnable Quarkus development environment and establish the technical foundation for the project.

The goal is **not** to build payment functionality yet.

The goal is to prove:

```text
Developer
   ↓
Quarkus
   ↓
CockroachDB
   ↓
Kafka
   ↓
Automated Test
```

works locally.

## Tasks

### Project Setup

- [ ] Create Git repository
- [ ] Decide Maven vs Gradle
- [ ] Select Java 25 distribution
- [ ] Select Quarkus version
- [ ] Generate Quarkus project
- [ ] Define project metadata
- [ ] Define package/module structure
- [ ] Create `.gitignore`
- [ ] Create initial documentation

### Quarkus

- [ ] Add REST capability
- [ ] Add validation
- [ ] Add database integration
- [ ] Add Kafka integration
- [ ] Add health checks
- [ ] Add testing dependencies

Exact extensions should be confirmed after the Maven/Gradle and Quarkus version decisions.

### CockroachDB

- [ ] Create Docker Compose service
- [ ] Start single-node CockroachDB
- [ ] Configure application connection
- [ ] Verify connectivity
- [ ] Create initial database/schema strategy
- [ ] Decide migration approach

### Kafka

- [ ] Create Docker Compose service
- [ ] Start Kafka locally
- [ ] Configure Quarkus Kafka connection
- [ ] Verify producer connectivity
- [ ] Verify consumer connectivity
- [ ] Decide initial topic configuration

### Testing

- [ ] Configure JUnit
- [ ] Configure Testcontainers
- [ ] Create basic application test
- [ ] Create database integration test
- [ ] Create Kafka integration test where appropriate

### Documentation

- [ ] Document local prerequisites
- [ ] Document startup commands
- [ ] Document shutdown commands
- [ ] Document test commands
- [ ] Document local architecture
- [ ] Record Sprint 0 decisions

## Acceptance Criteria

- Application starts successfully.
- CockroachDB starts successfully.
- Kafka starts successfully.
- Quarkus can connect to CockroachDB.
- Quarkus can connect to Kafka.
- A basic automated test executes successfully.
- The local environment can be started from documented commands.
- No payment business logic is required yet.

## Definition of Done

- [ ] Implementation completed
- [ ] Tests pass
- [ ] Docker environment starts
- [ ] Documentation updated
- [ ] Architecture decisions recorded
- [ ] Changes committed to Git

---

# Sprint 1 — Payment MVP

## Sprint Goal

A merchant can create a payment and observe it progress through the basic payment lifecycle with a simulated bank.

The implementation remains inside the modular monolith.

## Story S1-01 — Create Payment

### User Story

> As a merchant, I want to create a payment so that I can charge a customer.

### Example

```http
POST /api/v1/payments
Idempotency-Key: abc-123
Content-Type: application/json
```

```json
{
  "amount": 10000,
  "currency": "USD",
  "merchantId": "merchant-001",
  "customerId": "customer-001"
}
```

### Acceptance Criteria

- Payment request is validated.
- Payment receives a unique identifier.
- Payment is persisted in CockroachDB.
- Initial status is `PENDING`.
- API returns the payment identifier.
- Duplicate idempotency keys do not create duplicate payments.
- The behavior is covered by tests.

---

## Story S1-02 — Retrieve Payment

```http
GET /api/v1/payments/{paymentId}
```

### Acceptance Criteria

- Existing payment can be retrieved.
- Unknown payment returns an appropriate error.
- Response contains current payment status.
- Response contains enough information to identify the payment.
- The behavior is covered by tests.

---

## Story S1-03 — Payment State Machine

Implement:

```text
CREATED
   ↓
PENDING
   ↓
PROCESSING
   ├──→ SUCCEEDED
   │
   └──→ FAILED

SUCCEEDED
   ↓
REFUNDED
```

### Acceptance Criteria

- Valid transitions succeed.
- Invalid transitions are rejected.
- State changes are persisted.
- State-transition rules are tested.
- The domain, rather than the API/controller, owns transition rules.

---

## Story S1-04 — Fake Bank

Create a simulated external bank/payment provider.

The Fake Bank supports:

- Successful authorization
- Declined authorization
- Timeout
- Simulated server error

The behavior must be configurable for testing.

The Fake Bank must not be treated as a real external banking integration.

---

## Story S1-05 — End-to-End Payment

Connect the payment domain to the Fake Bank.

Expected outcomes:

```text
PENDING
   ↓
PROCESSING
   ↓
SUCCEEDED
```

or:

```text
PENDING
   ↓
PROCESSING
   ↓
FAILED
```

### Sprint 1 Boundary

The payment flow should work end-to-end, but the sophisticated distributed reliability patterns are intentionally reserved for Sprint 2.

---

# Sprint 2 — Event-Driven Architecture & Reliability

## Sprint Goal

Make payment processing asynchronous and resilient to distributed-system failures.

## Tasks

- [ ] Configure Kafka producer
- [ ] Configure Kafka consumers
- [ ] Define common event envelope
- [ ] Create `payments.created`
- [ ] Create `payments.processing`
- [ ] Create `payments.succeeded`
- [ ] Create `payments.failed`
- [ ] Implement asynchronous processor
- [ ] Implement event identifiers
- [ ] Implement idempotent consumers
- [ ] Add processed-event tracking
- [ ] Implement retries
- [ ] Configure backoff
- [ ] Add dead-letter topic
- [ ] Simulate consumer failure
- [ ] Simulate bank timeout
- [ ] Simulate duplicate events
- [ ] Simulate Kafka failure
- [ ] Simulate database failure
- [ ] Document transaction boundaries
- [ ] Reproduce the DB/Kafka consistency problem
- [ ] Investigate Transactional Outbox
- [ ] Implement the selected reliability solution

---

## Story S2-01 — PaymentCreated Event

When a payment is created, publish:

```text
PaymentCreated
```

Conceptual event:

```json
{
  "eventId": "evt-123",
  "eventType": "PaymentCreated",
  "aggregateId": "pay-123",
  "occurredAt": "2026-09-04T10:00:00Z",
  "version": 1,
  "payload": {}
}
```

---

## Story S2-02 — Idempotent Consumer

If:

```text
PaymentCreated
PaymentCreated
```

is delivered twice, the financial operation must still happen once.

### Acceptance Criteria

Given the same `eventId` is processed twice:

```text
Bank charges = 1
```

not:

```text
Bank charges = 2
```

---

## Story S2-03 — Retry

Transient bank failures should trigger retries.

Example:

```text
Attempt 1 → timeout
Attempt 2 → timeout
Attempt 3 → success
```

Retry policy must define:

- Maximum attempts
- Backoff
- Jitter
- Retryable errors
- Non-retryable errors
- Final failure behavior

---

## Story S2-04 — Dead-Letter Topic

Messages that cannot be processed successfully after the retry policy should move to a dead-letter topic.

Acceptance criteria:

- Failed message is identifiable.
- Original event information is retained.
- Failure reason is available.
- DLQ behavior is tested.
- Recovery/reprocessing approach is documented.

---

## Story S2-05 — Failure Recovery

Test and document:

- Kafka consumer crash
- Database failure
- Bank timeout
- Bank success followed by application crash
- Duplicate Kafka message
- Kafka unavailable
- Partial completion

For every scenario document:

```text
Failure
   ↓
Observed behavior
   ↓
Expected behavior
   ↓
Recovery mechanism
   ↓
Data consistency
```

---

## Story S2-06 — Transaction Boundary Experiment

Deliberately reproduce:

```text
DB commit succeeds
        +
Kafka publish fails
```

and:

```text
Kafka publish succeeds
        +
DB transaction fails
```

Then investigate the Transactional Outbox pattern.

The objective is understanding, not simply copying a pattern.

---

# Sprint 3 — Fintech Features

## Sprint Goal

Introduce concepts found in real financial systems.

## Story S3-01 — Refunds

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

Acceptance criteria should include:

- Valid refund state transitions
- Duplicate refund protection
- Refund audit trail
- Bank failure handling
- Idempotency

---

## Story S3-02 — Audit Trail

Record important payment state transitions.

Example:

```text
Payment Created
      ↓
Payment Pending
      ↓
Payment Processing
      ↓
Payment Succeeded
```

The audit history must allow reconstruction of the payment lifecycle.

---

## Story S3-03 — Settlement

Simulate an external provider sending settlement data.

Input may be:

```text
CSV / API
```

The settlement flow should remain separate from the normal payment transaction flow.

---

## Story S3-04 — Reconciliation

Compare:

```text
Internal Transactions
        VS
External Settlement
```

Classify:

```text
MATCH
MISMATCH
MISSING_INTERNAL
MISSING_EXTERNAL
```

---

## Story S3-05 — ETL Pipeline

Conceptual flow:

```text
External CSV/API
      ↓
Extract
      ↓
Transform
      ↓
Validate
      ↓
Kafka
      ↓
Reconciliation
      ↓
CockroachDB
```

The implementation should emphasize data correctness and traceability.

---

# Sprint 4 — AI Fraud Detection

## Sprint Goal

Add AI-assisted fraud analysis without making AI authoritative over money movement.

## Initial Fraud Signals

- Transaction amount
- Transaction frequency
- Customer history
- Merchant history
- Currency
- Country
- Device
- Time of transaction
- Previous failed transactions

## Initial Rule-Based Model

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

## AI Extension

The AI component may:

- Detect anomalies
- Generate risk explanations
- Classify suspicious patterns
- Assist investigation
- Produce a risk score

The payment processor remains deterministic.

AI should not directly become the source of truth for financial state.

---

# 5. Engineering Quality Backlog

Progressively introduce:

- [ ] Unit tests
- [ ] Integration tests
- [ ] API tests
- [ ] Kafka integration tests
- [ ] Testcontainers
- [ ] Failure tests
- [ ] Structured logging
- [ ] Metrics
- [ ] Health checks
- [ ] Correlation IDs
- [ ] Dockerized application
- [ ] Architecture documentation
- [ ] API documentation
- [ ] Event documentation

---

# 6. Definition of Done

A story is Done when:

- [ ] Implementation completed
- [ ] Unit tests added
- [ ] Integration tests added where appropriate
- [ ] Error handling implemented
- [ ] Logging implemented
- [ ] API/event contract documented
- [ ] Failure scenarios considered
- [ ] Code reviewed
- [ ] Documentation updated
- [ ] Changes committed to Git

---

# 7. Current Status

```text
Sprint: 0 — Foundation & Architecture
Status: 🟡 Planning

Current next action:
Resolve Sprint 0 foundation decisions.
```

Before writing payment code, settle:

1. Maven vs Gradle
2. Java 25 distribution
3. Quarkus version
4. Required Quarkus extensions
5. Docker Compose topology
6. CockroachDB local topology
7. Kafka local topology

Then begin the first implementation story.

---

# 8. Sprint Retrospective

At the end of every sprint, record:

- What went well?
- What went wrong?
- What did I learn?
- What architectural assumption changed?
- What failure surprised me?
- What should I do differently next sprint?
- What should be documented for future reference?
