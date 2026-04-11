# Link Tracker

## Prerequisites

- Docker Desktop
- JDK 25
- Maven 3.9.12+

## Running the project

### 1. Set up environment variables

Copy `.env.example` to `.env` and fill in your tokens:

```bash
cp .env.example .env
```

### 2. Start the database and run migrations

```bash
docker compose up -d postgresql
docker compose up liquibase-migrations
```

### 3. Run the applications

**Scrapper** (port 8081):

```bash
./mvnw spring-boot:run -pl scrapper -am
```

**Bot** (port 8080):

```bash
./mvnw spring-boot:run -pl bot -am
```

Or run both `ScrapperApplication` and `BotApplication` directly from IDE after `docker compose up`.

## Running tests

```bash
./mvnw test -pl scrapper -am
```

Tests use Testcontainers — Docker must be running.

## Configuration

- Database access type: `app.database.access-type=SQL` or `ORM` in `application.yaml`
- Default is SQL

