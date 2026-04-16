# Link Tracker

A Spring Boot microservices application that tracks changes on GitHub repositories and StackOverflow questions and notifies users via Telegram.

## Architecture

- **bot** (port 8080) — Telegram bot, handles user commands and sends notifications
- **scrapper** (port 8081) — Scheduler, polls GitHub/SO APIs for changes and notifies the bot

## Prerequisites

- Docker Desktop (must be running)
- JDK 25

## Quick Start

### 1. Start the database

```bash
docker compose up -d postgresql
```

### 2. Configure environment

```bash
cp scrapper/.env.example scrapper/.env
```

Edit `scrapper/.env` and fill in your tokens. The app starts without tokens too — GitHub and SO will be called unauthenticated (lower rate limits but functional).

### 3. Run scrapper

```bash
cd scrapper
..\mvnw spring-boot:run -DskipTests      # Windows
../mvnw spring-boot:run -DskipTests      # Linux/macOS
```

Wait for `Started ScrapperApplication in X seconds`.

### 4. Run bot (separate terminal)

Add `TELEGRAM_TOKEN` to `bot/.env`, then:

```bash
cd bot
..\mvnw spring-boot:run -DskipTests
```

## Running Tests

Docker Desktop must be running.

```bash
.\mvnw test -pl scrapper -am        # Windows
./mvnw test -pl scrapper -am        # Linux/macOS
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

|           Property           | Default |        Description         |
|------------------------------|---------|----------------------------|
| `app.database.access-type`   | `SQL`   | `SQL` or `ORM`             |
| `app.scheduler.interval`     | `60000` | Polling interval ms        |
| `app.scheduler.batch-size`   | `100`   | Links per tick (50–500)    |
| `app.scheduler.thread-count` | `4`     | Parallel threads per batch |

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
docker compose up -d postgresql
```


## Kafka Topic Configuration

Topics are created programmatically via Spring Kafka `NewTopic` beans on application startup.

### `link-updates`
| Setting | Value | Why |
|---------|-------|-----|
| Partitions | 3 | Allows up to 3 bot instances to consume in parallel |
| Replication factor | 3 | Every message stored on all 3 brokers — survives 2 broker failures |
| `retention.ms` | 604800000 (7 days) | Enough time for manual inspection if bot is down for a while |
| `min.insync.replicas` | 2 | A write is only confirmed when 2 out of 3 brokers have it — prevents data loss |

### `link-updates.DLT`
| Setting | Value | Why |
|---------|-------|-----|
| Partitions | 3 | Matches main topic |
| Replication factor | 3 | Dead letters are important for debugging — keep them safe |
| `retention.ms` | 2592000000 (30 days) | Longer retention — these need manual review and shouldn't expire quickly |
