# Feature Flag & A/B Testing Service

A production-ready, self-hosted feature flag and A/B testing backend service built with **Spring Boot 3.4** and **Java 21**. Provides real-time flag evaluation, percentage-based rollouts, attribute-based user targeting, and consistent A/B variant assignment — all without any external SaaS dependency.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Local Setup](#local-setup)
- [Running the Application](#running-the-application)
- [Database Migrations](#database-migrations)
- [API Reference](#api-reference)
- [Authentication](#authentication)
- [Flag Evaluation Logic](#flag-evaluation-logic)
- [A/B Testing](#ab-testing)
- [Targeting Rules](#targeting-rules)
- [Real-Time Updates via SSE](#real-time-updates-via-sse)
- [Impression Analytics](#impression-analytics)
- [Observability](#observability)
- [Configuration Reference](#configuration-reference)
- [Make Targets](#make-targets)
- [Running Tests](#running-tests)
- [Postman Collection](#postman-collection)
- [Example Workflows](#example-workflows)

---

## Overview

Modern software delivery teams need to decouple feature releases from code deployments. This service solves that problem by providing a central, self-hosted system for managing feature visibility at runtime — with no restarts, no redeployments, and no third-party SaaS required.

**Key capabilities:**

- Create and manage feature flags with a simple REST API
- Enable flags for a percentage of users using deterministic consistent hashing
- Target specific users or segments based on arbitrary attributes
- Run A/B experiments with weighted variant assignment that stays stable per user
- Propagate flag changes to all connected application instances in real time via Server-Sent Events
- Record every flag evaluation as an impression for analytics and experiment analysis
- Expose Prometheus metrics for latency, cache performance, and active SSE connections

---

## Architecture

The service is divided into two logical planes:

```
┌──────────────────────────────────────────────────────────────┐
│                        Control Plane                         │
│                                                              │
│   Admin REST API  ──►  Flag Evaluation Engine                │
│         │                      │                             │
│         ▼                      ▼                             │
│    PostgreSQL            Redis Cache                         │
│  (source of truth)    (flag:* key prefix)                    │
│         │                      │                             │
│         └──── sync on write ───┘                             │
│                         │                                    │
│                  Redis Pub/Sub                               │
│               (flags:changes channel)                        │
└─────────────────────────┬────────────────────────────────────┘
                          │
┌─────────────────────────▼────────────────────────────────────┐
│                         Data Plane                           │
│                                                              │
│   POST /evaluate       GET /stream        Impression Logger  │
│  (reads Redis only)  (SSE broadcaster)   (@Async to Postgres)│
│         │                  │                     │           │
│   FlagEvaluator    SseFlagBroadcaster    ImpressionRepository│
│   PercentageRoller FlagChangeListener    MetricsService      │
│   RuleMatcherSvc   FlagChangePublisher                       │
│   VariantSelector                                            │
└──────────────────────────────────────────────────────────────┘
```

**Flag change propagation flow:**

```
Admin PUT /admin/flags/{key}
  → Save to PostgreSQL
  → Write to Redis cache (flag:{key})
  → Publish to Redis channel (flags:changes)
  → FlagChangeListener receives message
  → Refreshes Redis cache
  → SseFlagBroadcaster pushes to all SSE clients
  → Connected applications receive update in < 200ms
```

**Flag evaluation flow:**

```
POST /evaluate
  → Check Redis cache (flag:{key})
  → Cache miss? → Read from PostgreSQL → warm cache
  → FlagEvaluator.evaluate(flag, context)
      1. Killswitch check    → DISABLED
      2. Targeting rules     → TARGETED
      3. Percentage rollout  → ROLLOUT
      4. Default             → DEFAULT
  → ImpressionLogger.logAsync() [non-blocking]
  → Return EvaluateResponse
```

---

## Tech Stack

| Technology | Version | Role |
|---|---------|---|
| Java | 21      | Primary language |
| Spring Boot | 3.4.5   | Application framework |
| Spring Data JPA | 3.4.5   | ORM and repository layer |
| Spring Data Redis | 3.4.5   | Cache and pub/sub |
| Spring Security | 3.4.5   | API key authentication |
| Spring Boot Actuator | 3.4.5   | Health checks and metrics |
| PostgreSQL | 15      | Primary data store |
| Redis | 7       | Evaluation cache and pub/sub broker |
| Flyway | 10.20.1 | Database schema migrations |
| Hibernate | 6.6.13  | JPA implementation |
| HikariCP | 5.1.0   | JDBC connection pooling |
| Lettuce | 6.4.2   | Redis client |
| Jackson | 2.18.3  | JSON serialization |
| Google Guava | 33.5    | MurmurHash3 for consistent hashing |
| Micrometer | 1.14.6  | Metrics instrumentation |
| Springdoc OpenAPI | 2.8.0   | Swagger UI and API docs |
| JUnit 5 | 5.11.4  | Unit testing |
| AssertJ | 3.26.3  | Fluent test assertions |

---

## Project Structure

```
feature-flag-service/
├── src/
│   ├── main/
│   │   ├── java/com/debankar/featureflags/
│   │   │   ├── FeatureFlagServiceApplication.java
│   │   │   │
│   │   │   ├── api/                                # HTTP layer
│   │   │   │   ├── AdminFlagController.java        # Flag CRUD endpoints
│   │   │   │   ├── EvaluationController.java       # /evaluate endpoint
│   │   │   │   ├── SseController.java              # /stream SSE endpoint
│   │   │   │   ├── GlobalExceptionsHandler.java    # catch all exceptions
│   │   │   │   └── dto/
│   │   │   │       ├── FlagRequest.java
│   │   │   │       ├── FlagResponse.java
│   │   │   │       ├── EvaluateRequest.java
│   │   │   │       ├── EvaluateResponseDto.java
│   │   │   │       └── ErrorResponse.java
│   │   │   │
│   │   │   ├── domain/                       # Pure Java domain model
│   │   │   │   ├── Flag.java
│   │   │   │   ├── TargetingRule.java
│   │   │   │   ├── TargetingOperator.java
│   │   │   │   ├── Variant.java
│   │   │   │   ├── EvaluationContext.java
│   │   │   │   └── EvaluateResponse.java
│   │   │   │
│   │   │   ├── engine/                       # Evaluation logic (no I/O)
│   │   │   │   ├── FlagEvaluator.java
│   │   │   │   ├── PercentageRoller.java
│   │   │   │   ├── RuleMatcherService.java
│   │   │   │   └── VariantSelector.java
│   │   │   │
│   │   │   ├── cache/                        # Redis abstraction
│   │   │   │   ├── FlagCacheService.java
│   │   │   │   ├── FlagCacheWarmup.java
│   │   │   │   └── FlagMapper.java
│   │   │   │
│   │   │   ├── pubsub/                       # Redis pub/sub
│   │   │   │   ├── FlagChangePublisher.java
│   │   │   │   └── FlagChangeListener.java
│   │   │   │
│   │   │   ├── sse/                          # SSE broadcasting
│   │   │   │   └── SseFlagBroadcaster.java
│   │   │   │
│   │   │   ├── persistence/                  # JPA entities + repos
│   │   │   │   ├── FlagEntity.java
│   │   │   │   ├── FlagRepository.java
│   │   │   │   ├── ImpressionEntity.java
│   │   │   │   └── ImpressionRepository.java
│   │   │   │
│   │   │   ├── analytics/                    # Async logging + metrics
│   │   │   │   ├── ImpressionLogger.java
│   │   │   │   └── MetricsService.java
│   │   │   │
│   │   │   └── config/                       # Spring configuration
│   │   │       ├── RedisConfig.java
│   │   │       ├── SecurityConfig.java
│   │   │       ├── AsyncConfig.java
│   │   │       └── TraceIdFilter.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │           ├── V1__create_flags.sql
│   │           └── V2__create_impressions.sql
│   │
│   └── test/
│       └── java/com/debankar/featureflags/
│           └── engine/
│           │   ├── FlagEvaluatorTest.java
│           │   ├── PercentageRollerTest.java
│           │   └── RuleMatcherServiceTest.java
│           └── FeatureFlagServiceApplicationTests.java
├── docker-compose.yml
├── Makefile
├── .env.example
├── .gitignore
├── pom.xml
└── README.md
```

---

## Prerequisites

Make sure the following are installed on your machine before getting started:

| Tool | Version | Purpose | Download |
|---|---|---|---|
| Java JDK | 21 (LTS) | Runtime and compiler | [Amazon Corretto 21](https://aws.amazon.com/corretto/) |
| Docker Desktop | 4.x+ | Runs Postgres and Redis | [docker.com](https://www.docker.com/products/docker-desktop/) |
| Git | Any recent | Version control | [git-scm.com](https://git-scm.com/) |
| Maven wrapper | Included | Build tool (no install needed) | Bundled in project |

**Optional but recommended:**

- **IntelliJ IDEA** (Community Edition is free) — IDE for Spring Boot
- **Postman** — for testing API endpoints interactively
- **DBeaver** — free GUI for inspecting the PostgreSQL database

**Verify prerequisites:**

```bash
java -version          # should print openjdk 21
docker --version       # should print Docker version 24+
docker compose version # should print v2.x
git --version          # any recent version
./mvnw --version       # run from project root
```

---

## Local Setup

**Step 1 — Clone the repository**

```bash
git clone https://github.com/deedeecee/feature-flag-service.git
cd feature-flag-service
```

**Step 2 — Set up environment variables**

```bash
cp .env.example .env
```

Open `.env` and review the values. For local development, the defaults work without any changes:

```bash
DB_USERNAME=postgres
DB_PASSWORD=postgres
REDIS_HOST=localhost
REDIS_PORT=6379
ADMIN_API_KEY=dev-admin-key
CLIENT_API_KEY=dev-client-key
```

> **Important:** Never commit `.env` to Git. It is already listed in `.gitignore`.

**Step 3 — Start infrastructure**

```bash
make dev
```

This starts PostgreSQL on port `5432` and Redis on port `6379` as Docker containers. Verify they are healthy:

```bash
make ps
```

Both containers should show `healthy` in the STATUS column.

**Step 4 — Run database migrations**

```bash
make migrate
```

Flyway will create the `flags` and `impressions` tables and the `flyway_schema_history` tracking table. Verify:

```bash
docker exec -it featureflags-postgres psql -U postgres -d featureflags -c "\dt"
```

You should see three tables listed.

**Step 5 — Start the application**

```bash
make run
```

The application starts on port `8080`. A successful startup looks like this in the logs:

```
Started FeatureFlagServiceApplication in 5.x seconds
Cache warmup complete — loaded 0 flag(s) into Redis.
```

**Step 6 — Verify everything is running**

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{
  "status": "UP",
  "components": {
    "db":    { "status": "UP" },
    "redis": { "status": "UP" },
    "ping":  { "status": "UP" }
  }
}
```

All three components must show `UP` before using the API.

---

## Running the Application

**Development mode (recommended):**

Always start infrastructure first, then the app:

```bash
make dev   # Terminal 1 — start Postgres + Redis (keep running)
make run   # Terminal 2 — start the Spring Boot app
```

**Full containerised stack:**

```bash
make up    # builds Docker image and runs all three containers
make down  # stops everything
```

> Note: `make up` requires a `Dockerfile` to be present. The app service in `docker-compose.yml` is gated behind the `full` profile so it does not start with `make dev`.

**Stopping the application:**

Press `Ctrl + C` in the terminal running `make run`. To stop Docker containers:

```bash
make stop
```

---

## Database Migrations

Migrations are managed by **Flyway** and run automatically when the Spring Boot application starts. They also run manually via `make migrate`.

| Migration | File | Description |
|---|---|---|
| V1 | `V1__create_flags.sql` | Creates the `flags` table with JSONB columns for targeting rules and variants |
| V2 | `V2__create_impressions.sql` | Creates the `impressions` table for analytics |

**Migration commands:**

```bash
make migrate        # apply pending migrations
make migrate-info   # show migration status
make migrate-clean  # wipe all tables (destructive — dev only)
```

> **Never modify an already-applied migration file.** Always create a new versioned file (V3, V4, etc.) for schema changes.

---

## API Reference

### Base URL

```
http://localhost:8080
```

### Authentication

All endpoints (except `/actuator/health`) require an `X-API-Key` header.

| Key type | Header value | Access |
|---|---|---|
| Admin key | `dev-admin-key` | All `/admin/**` endpoints |
| Client key | `dev-client-key` | `/evaluate` and `/stream` endpoints |

Requests missing the header receive `401 Unauthorized`. Requests with the wrong key type for an endpoint receive `403 Forbidden`.

---

### Admin Endpoints — Flag Management

All admin endpoints require: `X-API-Key: dev-admin-key`

---

#### `POST /admin/flags` — Create a flag

Creates a new feature flag.

**Request body:**

```json
{
  "key": "checkout-redesign",
  "name": "New Checkout Redesign",
  "enabled": true,
  "rolloutPercentage": 50,
  "targetingRules": [],
  "variants": []
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `key` | string | Yes | Unique identifier. Lowercase alphanumeric with hyphens. Cannot be changed after creation. |
| `name` | string | Yes | Human-readable display name |
| `enabled` | boolean | No | Master killswitch. Default: `false` |
| `rolloutPercentage` | integer (0–100) | No | Fraction of users who receive the flag. Default: `0` |
| `targetingRules` | array | No | Ordered list of attribute-based targeting rules |
| `variants` | array | No | A/B test variants with weights summing to 100 |

**Response:** `201 Created`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "key": "checkout-redesign",
  "name": "New Checkout Redesign",
  "enabled": true,
  "rolloutPercentage": 50,
  "targetingRules": null,
  "variants": null,
  "createdAt": "2026-04-14T10:00:00+05:30",
  "updatedAt": "2026-04-14T10:00:00+05:30"
}
```

---

#### `GET /admin/flags` — List all flags

Returns all flags in the system.

**Response:** `200 OK` — array of flag objects

---

#### `GET /admin/flags/{key}` — Get a flag

Returns a single flag by its key.

**Response:** `200 OK` — flag object, or `404 Not Found`

---

#### `PUT /admin/flags/{key}` — Update a flag

Updates any mutable field on an existing flag. Automatically invalidates the Redis cache and broadcasts the change to all SSE clients.

**Request body:** Same shape as `POST /admin/flags`

**Response:** `200 OK` — updated flag object

---

#### `DELETE /admin/flags/{key}` — Delete a flag

Deletes a flag from PostgreSQL and removes it from the Redis cache. All subsequent evaluations for this key return `404`.

**Response:** `204 No Content`

---

#### `GET /admin/flags/{key}/stats` — Get impression stats

Returns the total impression count and a variant breakdown for the last 7 days.

**Response:** `200 OK`

```json
{
  "flagKey": "checkout-redesign",
  "totalImpressions": 1482,
  "last7Days": [
    { "variant": "control",   "count": 743 },
    { "variant": "treatment", "count": 739 }
  ]
}
```

---

### Evaluation Endpoints

All evaluation endpoints require: `X-API-Key: dev-client-key`

---

#### `POST /evaluate` — Evaluate a flag

Evaluates a single flag for a given user context. Reads exclusively from Redis — never hits PostgreSQL at evaluation time. Falls back to PostgreSQL only on a cache miss, then warms the cache.

**Request body:**

```json
{
  "flagKey": "checkout-redesign",
  "userId": "user-abc-123",
  "attributes": {
    "plan": "enterprise",
    "region": "us-east"
  }
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `flagKey` | string | Yes | The flag key to evaluate |
| `userId` | string | Yes | Stable user identifier used for consistent hashing |
| `attributes` | object | No | Arbitrary key-value pairs matched against targeting rules |

**Response:** `200 OK`

```json
{
  "enabled": true,
  "variant": null,
  "reason": "ROLLOUT"
}
```

| Field | Description |
|---|---|
| `enabled` | Whether the flag is on for this user |
| `variant` | Variant name for A/B tests, `null` for boolean flags |
| `reason` | Why this evaluation was reached (see below) |

**Reason codes:**

| Reason | Meaning |
|---|---|
| `DISABLED` | Flag is turned off (`enabled: false`) — killswitch active |
| `TARGETED` | User matched a targeting rule |
| `ROLLOUT` | User's hash fell within the rollout percentage |
| `DEFAULT` | User's hash was outside the rollout — flag is off for this user |

---

#### `POST /evaluate/batch` — Batch evaluate

Evaluates multiple flags in a single request.

**Request body:** Array of evaluate requests

```json
[
  { "flagKey": "checkout-redesign", "userId": "user-123", "attributes": {} },
  { "flagKey": "homepage-ab-test",  "userId": "user-123", "attributes": {} }
]
```

**Response:** `200 OK` — array of evaluate responses in the same order

---

### Streaming Endpoint

#### `GET /stream` — Open SSE stream

Opens a persistent Server-Sent Events connection. The server pushes a `flag-change` event every time any flag is created, updated, or deleted. The connection stays open indefinitely until the client disconnects.

**Headers required:** `X-API-Key: dev-client-key`

**Response:** `text/event-stream`

```
event: flag-change
data: {"id":"...","key":"checkout-redesign","enabled":true,"rolloutPercentage":75,...}

event: flag-change
data: {"id":"...","key":"homepage-ab-test","enabled":false,...}
```

---

### System Endpoints (public — no auth required)

| Endpoint | Description |
|---|---|
| `GET /actuator/health` | Service health including Postgres and Redis status |
| `GET /actuator/prometheus` | Prometheus metrics in text format |

---

## Flag Evaluation Logic

The evaluator applies rules in strict priority order. The first matching rule wins and no further rules are checked.

```
1. KILLSWITCH     Is flag.enabled == false?
                  → Return { enabled: false, reason: DISABLED }

2. TARGETING      Does the user match any targeting rule?
                  → Return { enabled: true, reason: TARGETED }

3. ROLLOUT        Does MurmurHash3(flagKey + ":" + userId) % 100
                  fall within rolloutPercentage?
                  → Return { enabled: true, variant: <selected>, reason: ROLLOUT }

4. DEFAULT        None of the above matched.
                  → Return { enabled: false, reason: DEFAULT }
```

**Consistent hashing** ensures the same `userId` always produces the same bucket for the same `flagKey`. This means a user in a 20% rollout gets the same `true` or `false` result on every evaluation, across every app instance, across restarts — without any stored session state.

---

## A/B Testing

A flag becomes an A/B test when it has a `variants` array. Variant weights must sum to exactly 100.

**Create an A/B test flag:**

```json
{
  "key": "homepage-ab-test",
  "name": "Homepage A/B Test",
  "enabled": true,
  "rolloutPercentage": 100,
  "variants": [
    { "name": "control",   "weight": 50 },
    { "name": "treatment", "weight": 50 }
  ]
}
```

**Evaluate:**

```json
{ "enabled": true, "variant": "control", "reason": "ROLLOUT" }
```

The variant is selected using the same consistent hash as the rollout check — so a user always receives the same variant for the same flag, making experiment results reliable across sessions.

**Multi-variant example:**

```json
"variants": [
  { "name": "control",     "weight": 34 },
  { "name": "treatment-a", "weight": 33 },
  { "name": "treatment-b", "weight": 33 }
]
```

---

## Targeting Rules

Targeting rules allow flags to be enabled for specific users or segments regardless of the rollout percentage. Rules are evaluated in order — the first match wins.

**Supported operators:**

| Operator | Behaviour |
|---|---|
| `EQUALS` | Attribute equals the value (case-insensitive) |
| `NOT_EQUALS` | Attribute does not equal the value |
| `CONTAINS` | Attribute contains the value as a substring |
| `IN` | Attribute is one of a comma-separated list of values |
| `GREATER_THAN` | Attribute (numeric) is greater than the value |
| `LESS_THAN` | Attribute (numeric) is less than the value |

**Example — enterprise-only feature:**

```json
{
  "key": "enterprise-feature",
  "name": "Enterprise Only Feature",
  "enabled": true,
  "rolloutPercentage": 0,
  "targetingRules": [
    {
      "attribute": "plan",
      "operator": "EQUALS",
      "value": "enterprise"
    }
  ]
}
```

With `rolloutPercentage: 0`, the flag is off for everyone except users whose `attributes.plan` equals `"enterprise"`. Those users receive `{ "enabled": true, "reason": "TARGETED" }`.

**Multiple rules example:**

```json
"targetingRules": [
  { "attribute": "plan",   "operator": "IN",      "value": "enterprise,team" },
  { "attribute": "region", "operator": "EQUALS",  "value": "us-east"         },
  { "attribute": "age",    "operator": "GREATER_THAN", "value": "18"         }
]
```

---

## Real-Time Updates via SSE

When you open a `GET /stream` connection, the service maintains it indefinitely and pushes events whenever any flag changes. This eliminates polling entirely.

**How to test real-time updates:**

1. Open the SSE stream in Postman (or `curl -H "X-API-Key: dev-client-key" http://localhost:8080/stream`) and keep it open.
2. In a separate tab, update any flag via `PUT /admin/flags/{key}`.
3. Watch the event appear in the SSE stream within milliseconds.

**Under the hood:**

Every `SseEmitter` connection is registered in a `ConcurrentHashMap` inside `SseFlagBroadcaster`. When `FlagChangeListener` receives a message from the Redis `flags:changes` channel, it calls `SseFlagBroadcaster.broadcast(flag)`, which iterates all active emitters and sends the event. Disconnected or timed-out emitters are automatically removed from the registry.

---

## Impression Analytics

Every call to `POST /evaluate` records an impression asynchronously — meaning it never adds latency to the evaluation response. The impression is written to the `impressions` table by a dedicated thread pool (`impressionExecutor`) with a queue capacity of 50,000.

**View stats for a flag:**

```bash
curl http://localhost:8080/admin/flags/checkout-redesign/stats \
  -H "X-API-Key: dev-admin-key"
```

**Response:**

```json
{
  "flagKey": "checkout-redesign",
  "totalImpressions": 3200,
  "last7Days": [
    { "variant": null, "count": 3200 }
  ]
}
```

If the async executor queue fills up, impressions are dropped with a warning log rather than failing the evaluation request.

---

## Observability

### Health check

```bash
curl http://localhost:8080/actuator/health
```

Reports the status of PostgreSQL, Redis, disk space, and SSL.

### Prometheus metrics

```bash
curl http://localhost:8080/actuator/prometheus
```

**Custom metrics exposed by this service:**

| Metric | Type | Description |
|---|---|---|
| `feature_flag_evaluation_latency_seconds` | Timer | End-to-end evaluation latency |
| `feature_flag_cache_hit_total` | Counter | Redis cache hits |
| `feature_flag_cache_miss_total` | Counter | Redis cache misses (fallback to Postgres) |
| `feature_flag_sse_connections_active` | Gauge | Current number of open SSE connections |

### Trace IDs

Every request gets a unique trace ID. If your client sends an `X-Trace-Id` header, that value is used. Otherwise the service generates a UUID. The trace ID is:

- Visible in every log line: `02:31:53 [abc-123] INFO ...`
- Returned in the `X-Trace-Id` response header

This lets you correlate a specific request across all log lines it touches.

---

## Configuration Reference

All configuration is in `src/main/resources/application.yml`. Sensitive values are injected via environment variables with development defaults.

| Property | Environment variable | Default | Description |
|---|---|---|---|
| `spring.datasource.url` | — | `jdbc:postgresql://localhost:5432/featureflags` | Postgres JDBC URL |
| `spring.datasource.username` | `DB_USERNAME` | `postgres` | Postgres username |
| `spring.datasource.password` | `DB_PASSWORD` | `postgres` | Postgres password |
| `spring.data.redis.host` | `REDIS_HOST` | `localhost` | Redis hostname |
| `spring.data.redis.port` | `REDIS_PORT` | `6379` | Redis port |
| `feature-flags.admin-api-key` | `ADMIN_API_KEY` | `dev-admin-key` | Key for admin endpoints |
| `feature-flags.client-api-key` | `CLIENT_API_KEY` | `dev-client-key` | Key for client endpoints |

**Change a value for local dev** — edit `.env`:

```bash
ADMIN_API_KEY=my-custom-admin-key
CLIENT_API_KEY=my-custom-client-key
```

**Production** — always inject secrets via environment variables. Never hardcode them in `application.yml` or commit them to Git.

---

## Make Targets

| Target | Command | Description |
|---|---|---|
| Start infrastructure | `make dev` | Start Postgres and Redis containers |
| Stop infrastructure | `make stop` | Stop containers (data is preserved in volumes) |
| Restart infrastructure | `make restart` | Stop then start containers |
| Start app | `make run` | Start the Spring Boot app on port 8080 |
| Build JAR | `make build` | Compile and package (skips tests) |
| Run unit tests | `make test` | Run all unit tests |
| Run migrations | `make migrate` | Apply pending Flyway migrations |
| Migration status | `make migrate-info` | Show which migrations have been applied |
| Wipe database | `make migrate-clean` | Drop all tables — dev only, destructive |
| View logs | `make logs` | Tail Docker container logs |
| Container status | `make ps` | Show container health status |
| Clean everything | `make clean` | Remove build artifacts and volumes |

---

## Running Tests

The test suite is divided into two categories.

### Unit tests

Pure Java tests with no external dependencies. They test the evaluation engine in isolation and run in under 5 seconds.

```bash
make test
```

**Test classes:**

| Class | What it tests |
|---|---|
| `FlagEvaluatorTest` | All evaluation branches — killswitch, targeting, rollout, default, A/B |
| `PercentageRollerTest` | Consistent hashing — boundary conditions, stability, distribution |
| `RuleMatcherServiceTest` | All six targeting operators with matching and non-matching inputs |

**Coverage targets:** 95%+ line coverage on the `engine` package.

### Unit test output

A successful run looks like:

```
[INFO] Tests run: 5, Failures: 0, Errors: 0  ← FlagEvaluatorTest
[INFO] Tests run: 4, Failures: 0, Errors: 0  ← PercentageRollerTest
[INFO] Tests run: 7, Failures: 0, Errors: 0  ← RuleMatcherServiceTest
[INFO] BUILD SUCCESS
```

---

## Postman Collection

A Postman collection is included at [postman/feature-flag-service.postman_collection.json](postman/feature-flag-service.postman_collection.json). It contains all API requests pre-configured with collection variables for the base URL and API keys.

**Import it:**

1. Open Postman → click **Import**
2. Select `postman/feature-flag-service.postman_collection.json`
3. The collection appears with all requests grouped into nine folders

**Collection variables (pre-set):**

| Variable | Value |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `adminKey` | `dev-admin-key` |
| `clientKey` | `dev-client-key` |
| `flagKey` | `checkout-redesign` |

---

## Example Workflows

### Basic feature flag rollout

```bash
# 1. Create the flag at 0% — code is deployed but hidden
curl -X POST http://localhost:8080/admin/flags \
  -H "X-API-Key: dev-admin-key" \
  -H "Content-Type: application/json" \
  -d '{"key":"new-dashboard","name":"New Dashboard","enabled":true,"rolloutPercentage":0}'

# 2. Enable for 5% to validate
curl -X PUT http://localhost:8080/admin/flags/new-dashboard \
  -H "X-API-Key: dev-admin-key" \
  -H "Content-Type: application/json" \
  -d '{"key":"new-dashboard","name":"New Dashboard","enabled":true,"rolloutPercentage":5}'

# 3. Evaluate from your application
curl -X POST http://localhost:8080/evaluate \
  -H "X-API-Key: dev-client-key" \
  -H "Content-Type: application/json" \
  -d '{"flagKey":"new-dashboard","userId":"user-xyz","attributes":{}}'

# 4. If healthy, ramp to 100%
curl -X PUT http://localhost:8080/admin/flags/new-dashboard \
  -H "X-API-Key: dev-admin-key" \
  -H "Content-Type: application/json" \
  -d '{"key":"new-dashboard","name":"New Dashboard","enabled":true,"rolloutPercentage":100}'

# 5. Emergency killswitch — instant off for all users
curl -X PUT http://localhost:8080/admin/flags/new-dashboard \
  -H "X-API-Key: dev-admin-key" \
  -H "Content-Type: application/json" \
  -d '{"key":"new-dashboard","name":"New Dashboard","enabled":false,"rolloutPercentage":100}'
```

### A/B experiment

```bash
# 1. Create the experiment
curl -X POST http://localhost:8080/admin/flags \
  -H "X-API-Key: dev-admin-key" \
  -H "Content-Type: application/json" \
  -d '{
    "key":"checkout-button-color",
    "name":"Checkout Button Colour Test",
    "enabled":true,
    "rolloutPercentage":100,
    "variants":[{"name":"control","weight":50},{"name":"green-button","weight":50}]
  }'

# 2. Evaluate for different users — each always gets the same variant
curl -X POST http://localhost:8080/evaluate \
  -H "X-API-Key: dev-client-key" \
  -H "Content-Type: application/json" \
  -d '{"flagKey":"checkout-button-color","userId":"user-alice","attributes":{}}'
# → {"enabled":true,"variant":"control","reason":"ROLLOUT"}

curl -X POST http://localhost:8080/evaluate \
  -H "X-API-Key: dev-client-key" \
  -H "Content-Type: application/json" \
  -d '{"flagKey":"checkout-button-color","userId":"user-bob","attributes":{}}'
# → {"enabled":true,"variant":"green-button","reason":"ROLLOUT"}

# 3. Check experiment stats after running for a week
curl http://localhost:8080/admin/flags/checkout-button-color/stats \
  -H "X-API-Key: dev-admin-key"
```

### Targeted enterprise feature

```bash
# Enable only for enterprise plan users
curl -X POST http://localhost:8080/admin/flags \
  -H "X-API-Key: dev-admin-key" \
  -H "Content-Type: application/json" \
  -d '{
    "key":"advanced-analytics",
    "name":"Advanced Analytics",
    "enabled":true,
    "rolloutPercentage":0,
    "targetingRules":[{"attribute":"plan","operator":"EQUALS","value":"enterprise"}]
  }'

# Enterprise user — gets it
curl -X POST http://localhost:8080/evaluate \
  -H "X-API-Key: dev-client-key" \
  -H "Content-Type: application/json" \
  -d '{"flagKey":"advanced-analytics","userId":"user-1","attributes":{"plan":"enterprise"}}'
# → {"enabled":true,"variant":null,"reason":"TARGETED"}

# Free user — does not get it
curl -X POST http://localhost:8080/evaluate \
  -H "X-API-Key: dev-client-key" \
  -H "Content-Type: application/json" \
  -d '{"flagKey":"advanced-analytics","userId":"user-2","attributes":{"plan":"free"}}'
# → {"enabled":false,"variant":null,"reason":"DEFAULT"}
```

---