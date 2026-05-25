# Link Tracker

A Spring Boot microservices application that tracks changes on GitHub repositories and StackOverflow questions and notifies users via Telegram.

## Architecture

- **bot** (port 8080) — Telegram bot, handles user commands and sends notifications
- **scrapper** (port 8081) — Scheduler, polls GitHub/SO APIs for changes and notifies the bot
- **ai-agent** (port 8082) — AI Agent, reads raw updates from Scrapper, applies prioritization and grouping, publishes processed events for the Bot
- Communication: **Apache Kafka** (default) or HTTP (configurable)

```
Scrapper → link.raw-updates → AI Agent → link.processed-updates → Bot → Telegram
```

## Prerequisites

- Docker Desktop (must be running)
- JDK 25

## Quick Start

### 1. Start the infrastructure

```bash
docker compose up -d
```

This starts:
- PostgreSQL (port 5432)
- Kafka cluster — 3 brokers in KRaft mode (ports 29092, 29093, 29094)
- Kafka UI (port 8090) — browse topics and messages at http://localhost:8090
- Valkey — 1 primary + 2 replicas (primary on port 6379)

Wait ~20 seconds for Kafka to be ready and all topics to be created.

### 2. Configure environment

```bash
cp scrapper/.env.example scrapper/.env
```

Edit `scrapper/.env` and fill in your tokens. The app starts without tokens too — GitHub and SO will be called unauthenticated (lower rate limits but functional).

```
GITHUB_TOKEN=your_github_token_here
KAFKA_BOOTSTRAP_SERVERS=localhost:29092
```

Create `bot/.env`:

```
TELEGRAM_TOKEN=your_telegram_token_here
KAFKA_BOOTSTRAP_SERVERS=localhost:29092
```

### 3. Run scrapper

```bash
cd scrapper
..\mvnw spring-boot:run -DskipTests      # Windows
../mvnw spring-boot:run -DskipTests      # Linux/macOS
```

Wait for `Started ScrapperApplication in X seconds`.

### 4. Run AI Agent (separate terminal)

```bash
cd ai-agent
..\mvnw spring-boot:run -DskipTests      # Windows
../mvnw spring-boot:run -DskipTests      # Linux/macOS
```

### 5. Run bot (separate terminal)

```bash
cd bot
..\mvnw spring-boot:run -DskipTests      # Windows
../mvnw spring-boot:run -DskipTests      # Linux/macOS
```

## Running Tests

Docker Desktop must be running.

### All scrapper tests:

```bash
.\mvnw test -pl scrapper -am        # Windows
./mvnw test -pl scrapper -am        # Linux/macOS
```

### All bot tests:

```bash
.\mvnw test -pl bot -am        # Windows
./mvnw test -pl bot -am        # Linux/macOS
```

### All ai-agent tests:

```bash
.\mvnw test -pl ai-agent -am        # Windows
./mvnw test -pl ai-agent -am        # Linux/macOS
```

### Integration test — Scrapper → Kafka → Bot:

```bash
cd scrapper
..\mvnw test -Dtest=ScrapperToBotIntegrationTest
```

This test verifies the full message flow:
1. `LinkCheckerService` detects a new GitHub issue
2. Sends a `LinkUpdate` message to the `link.raw-updates` Kafka topic
3. A raw Kafka consumer verifies the message arrived with correct content

## AI Agent Service

The AI Agent sits between Scrapper and Bot in the Kafka pipeline and handles three responsibilities:

**Filtering** — drops updates containing stop-words, from excluded authors, or below minimum length.

**Prioritization** — assigns HIGH, MEDIUM, or LOW priority based on keywords in the description. HIGH keywords take precedence over LOW. If neither is found, priority is MEDIUM.

**Grouping** — buffers updates for the same `tgChatId` within a configurable time window. Multiple updates for the same chat are merged into one numbered list. Priority of the merged message is the maximum of all buffered priorities.

### AI Agent configuration

| Property | Default | Description |
|---|---|---|
| `ai-agent.filtering.stop-words` | spam, ads, promo | Updates containing these words are dropped |
| `ai-agent.filtering.excluded-authors` | bot-user | Updates from these authors are dropped |
| `ai-agent.filtering.min-length` | 20 | Minimum description length to pass filter |
| `ai-agent.summarization.threshold` | 500 | Descriptions longer than this are summarized |
| `ai-agent.summarization.mode` | ai | `ai` uses HuggingFace API, `stub` truncates |
| `ai-agent.prioritization.high-keywords` | critical, urgent, breaking, security | Keywords that trigger HIGH priority |
| `ai-agent.prioritization.low-keywords` | minor, typo, chore, docs | Keywords that trigger LOW priority |
| `ai-agent.grouping.window-ms` | 30000 | Grouping window in milliseconds |

## Caching (Valkey)

The scrapper caches `GET /links` responses in Valkey (Redis-compatible) to reduce database load.

### How it works

- `GET /links` responses are cached in Valkey keyed by `chatId` with a configurable TTL (default 60s)
- `POST /links` and `DELETE /links` evict the cache entry for the affected chat
- Optionally, a JVM-level L1 cache can be enabled via Lettuce CLIENT TRACKING for zero-latency reads

### Cache configuration

|            Property             | Default |                      Description                      |
|---------------------------------|---------|-------------------------------------------------------|
| `app.cache.ttl`                 | `60s`   | Cache entry TTL in Valkey                             |
| `app.cache.client-side-enabled` | `false` | Enable JVM-level L1 cache via Lettuce CLIENT TRACKING |

### Infrastructure

The docker-compose includes a 3-node Valkey setup:
- `valkey` — primary node (port 6379)
- `valkey-replica-1` — replica
- `valkey-replica-2` — replica

### Load test results

Tests run with 32 threads, 60s ramp-up, 5 minute duration, 100k links (1000 chats × 100 links):

|     Scenario      |  RPS  | Avg ms | Min ms | Max ms | Errors |
|-------------------|-------|--------|--------|--------|--------|
| No cache          | 864.3 | 33     | 2      | 1273   | 0%     |
| Valkey cache      | 743.7 | 38     | 2      | 476    | 0%     |
| Client-side cache | 922.1 | 31     | 2      | 852    | 0%     |

Client-side caching achieves the highest throughput by serving from JVM memory. Valkey cache shows better tail latency (Max 476ms vs 1273ms) compared to no-cache.

## Notification Transport

By default, scrapper sends notifications to AI Agent via **Kafka**. To switch to HTTP (bypasses AI Agent):

```yaml
# scrapper/src/main/resources/application.yaml
app:
  notification:
    transport: http  # or 'kafka' (default)
```

## API

Swagger UI: `http://localhost:8081/swagger-ui/index.html`

Key endpoints (all require `Tg-Chat-Id` header except chat registration):

| Method |      Path       |  Description  |
|--------|-----------------|---------------|
| POST   | `/tg-chat/{id}` | Register chat |
| DELETE | `/tg-chat/{id}` | Delete chat   |
| POST   | `/links`        | Add link      |
| DELETE | `/links`        | Remove link   |
| GET    | `/links`        | List links    |

Supported links: `github.com/{owner}/{repo}` and `stackoverflow.com/questions/{id}/...`

## Configuration

`scrapper/src/main/resources/application.yaml`:

|              Property               | Default |        Description         |
|-------------------------------------|---------|----------------------------|
| `app.database.access-type`          | `SQL`   | `SQL` or `ORM`             |
| `app.notification.transport`        | `kafka` | `kafka` or `http`          |
| `app.kafka.consumer.retry-attempts` | `3`     | Retry attempts before DLQ  |
| `app.scheduler.interval`            | `60000` | Polling interval ms        |
| `app.scheduler.batch-size`          | `100`   | Links per tick (50–500)    |
| `app.scheduler.thread-count`        | `4`     | Parallel threads per batch |
| `app.cache.ttl`                     | `60s`   | Valkey cache TTL           |
| `app.cache.client-side-enabled`     | `false` | Enable L1 JVM cache        |

## Error Handling (DLQ)

The bot consumer handles errors in three categories:

- **Deserialization error** — message sent immediately to `link-updates.DLT`, no retries
- **Validation error** — message sent immediately to `link-updates.DLT`, no retries
- **Processing error** — retried N times (configurable via `app.kafka.consumer.retry-attempts`, default 3), then sent to `link-updates.DLT`

## Kafka Topic Configuration

Topics are created via `kafka-init` in docker-compose on startup.

### `link.raw-updates`

|        Setting        |       Value        |                         Why                          |
|-----------------------|--------------------|------------------------------------------------------|
| Partitions            | 3                  | Allows parallel consumption by AI Agent              |
| Replication factor    | 3                  | Survives 2 broker failures                           |
| `retention.ms`        | 604800000 (7 days) | Time for inspection if AI Agent is down              |
| `min.insync.replicas` | 2                  | Write confirmed on 2/3 brokers — prevents data loss  |

### `link.processed-updates`

|        Setting        |       Value        |                         Why                          |
|-----------------------|--------------------|------------------------------------------------------|
| Partitions            | 3                  | Allows up to 3 bot instances to consume in parallel  |
| Replication factor    | 3                  | Survives 2 broker failures                           |
| `retention.ms`        | 604800000 (7 days) | Time for inspection if bot is down                   |
| `min.insync.replicas` | 2                  | Write confirmed on 2/3 brokers — prevents data loss  |

## Resilience (HW-7)

The scrapper protects all outgoing HTTP calls (GitHub, StackOverflow, bot) with timeout, retry, circuit breaker, and a Kafka fallback. Public endpoints are rate-limited per IP.

### How it works

- **Timeout** — every `RestClient` uses `HttpComponentsClientHttpRequestFactory` with configurable connect/read timeouts. A slow service fails fast instead of blocking a thread.
- **Retry** — failed calls are retried with constant backoff (default) or exponential backoff (opt-in). Whether a failure is retryable is decided by the configured HTTP status code list, not hardcoded.
- **Circuit Breaker** — a COUNT_BASED sliding window tracks failures per client. After the failure threshold the breaker opens and calls fail immediately; after `wait-duration-in-open-state` it allows trial calls (HALF-OPEN) and closes again if they succeed.
- **Fallback** — when `app.notification.transport=http`, if the bot's HTTP endpoint is unavailable and the breaker opens, notifications are automatically rerouted to Kafka so updates are never silently lost.
- **Rate Limiting** — a Bucket4j token-bucket filter limits requests per client IP. Exceeding the limit returns HTTP 429.

### Resilience configuration

|                       Property                        |      Default      |                  Description                   |
|-------------------------------------------------------|-------------------|------------------------------------------------|
| `app.http-client.connect-timeout`                     | `3s`              | TCP connect timeout                            |
| `app.http-client.read-timeout`                        | `5s`              | Response read timeout                          |
| `app.resilience.retry-max-attempts`                   | `3`               | Total retry attempts                           |
| `app.resilience.retry-wait-duration`                  | `500ms`           | Delay between retries (constant backoff)       |
| `app.resilience.retryable-status-codes`               | `500,502,503,504` | HTTP statuses that trigger a retry             |
| `app.resilience.retry-exponential-backoff-enabled`    | `false`           | Use exponential instead of constant backoff    |
| `app.resilience.retry-exponential-backoff-multiplier` | `2`               | Multiplier when exponential backoff is enabled |
| `app.rate-limit.capacity`                             | `50`              | Max requests per IP in a window                |
| `app.rate-limit.refill-tokens`                        | `50`              | Tokens refilled per period                     |
| `app.rate-limit.refill-period`                        | `1m`              | Refill period                                  |

Circuit breaker parameters are configured per client under `resilience4j.circuitbreaker.instances` in `application.yaml`.

### Switching retry strategy

Constant backoff is the default. To use exponential backoff instead:

```yaml
app:
  resilience:
    retry-exponential-backoff-enabled: true
    retry-exponential-backoff-multiplier: 2
```

## Troubleshooting

### Port 5432 conflict (Windows)

If a local PostgreSQL installation conflicts with Docker on port 5432, run PowerShell as Administrator:

```powershell
Stop-Service -Name postgresql* -Force
docker restart postgresql
```

### Liquibase error: "relation already exists"

The `scrapper` database has tables but no Liquibase changelog. Clean solution:

```bash
docker compose down -v
docker compose up -d
```
