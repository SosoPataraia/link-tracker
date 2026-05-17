# Link Tracker

A Spring Boot microservices application that tracks changes on GitHub repositories and StackOverflow questions and notifies users via Telegram.

## Architecture

- **bot** (port 8080) — Telegram bot, handles user commands and sends notifications
- **scrapper** (port 8081) — Scheduler, polls GitHub/SO APIs for changes and notifies the bot
- Communication: **Apache Kafka** (default) or HTTP (configurable)

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

Wait ~10 seconds for Kafka to be ready.

### 2. Configure environment

```bash
cp scrapper/.env.example scrapper/.env
```

Edit `scrapper/.env` and fill in your tokens. The app starts without tokens too — GitHub and SO will be called unauthenticated (lower rate limits but functional).
GITHUB_TOKEN=your_github_token_here
KAFKA_BOOTSTRAP_SERVERS=localhost:29092

Create `bot/.env`:
TELEGRAM_TOKEN=your_telegram_token_here
KAFKA_BOOTSTRAP_SERVERS=localhost:29092

### 3. Run scrapper

```bash
cd scrapper
..\mvnw spring-boot:run -DskipTests      # Windows
../mvnw spring-boot:run -DskipTests      # Linux/macOS
```

Wait for `Started ScrapperApplication in X seconds`.

### 4. Run bot (separate terminal)

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
cd bot
..\mvnw test
```

### Integration test — Scrapper → Kafka → Bot:

```bash
cd scrapper
..\mvnw test -Dtest=ScrapperToBotIntegrationTest
```

This test verifies the full message flow:
1. `LinkCheckerService` detects a new GitHub issue
2. Sends a `LinkUpdate` message to the `link-updates` Kafka topic
3. A raw Kafka consumer verifies the message arrived with correct content

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

By default, scrapper sends notifications to bot via **Kafka**. To switch to HTTP:

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

Topics are created programmatically via Spring Kafka `NewTopic` beans on application startup.

### `link-updates`

|        Setting        |       Value        |                                      Why                                       |
|-----------------------|--------------------|--------------------------------------------------------------------------------|
| Partitions            | 3                  | Allows up to 3 bot instances to consume in parallel                            |
| Replication factor    | 3                  | Every message stored on all 3 brokers — survives 2 broker failures             |
| `retention.ms`        | 604800000 (7 days) | Enough time for manual inspection if bot is down for a while                   |
| `min.insync.replicas` | 2                  | A write is only confirmed when 2 out of 3 brokers have it — prevents data loss |

### `link-updates.DLT`

|      Setting       |        Value         |                                   Why                                    |
|--------------------|----------------------|--------------------------------------------------------------------------|
| Partitions         | 3                    | Matches main topic                                                       |
| Replication factor | 3                    | Dead letters are important for debugging — keep them safe                |
| `retention.ms`     | 2592000000 (30 days) | Longer retention — these need manual review and shouldn't expire quickly |

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

Circuit breaker parameters (`sliding-window-size`, `failure-rate-threshold`, `wait-duration-in-open-state`, etc.) are configured per client under `resilience4j.circuitbreaker.instances` in `application.yaml`.

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

