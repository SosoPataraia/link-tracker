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

## HW-4 Notes

**FR-2.1** — Reacts to new Issues, PRs (GitHub) and new answers, comments (StackOverflow).

**FR-2.2** — Messages include: title, username, creation time, preview up to 200 chars.

**NFR-1.1** — Batch processing ordered by `last_checked ASC NULLS FIRST`, batch size configurable.

**NFR-1.2 (bonus)** — Each batch split into `thread-count` sublists processed in parallel. Per-link errors are isolated.
