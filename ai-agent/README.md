# AI Agent Service

Intermediate service between Scrapper and Bot. Consumes raw link updates from Kafka,
applies filtering and summarization, and publishes processed updates for the Bot.

## Architecture

Scrapper → link.raw-updates → AI Agent → link.processed-updates → Bot

## Requirements

- Java 25
- Docker (for Kafka + Schema Registry)

## Running locally

### 1. Start infrastructure

    docker compose up -d

### 2. Configure secrets

Create a `.env` file in the project root:

    TELEGRAM_TOKEN=your_token_here
    HUGGINGFACE_TOKEN=your_hf_token_here

The `.env` file is in `.gitignore` and must never be committed.

### 3. Run from IDE

Run `AiAgentApplication.main()` directly — no additional setup beyond Docker.

### 4. Run from Maven

    ./mvnw spring-boot:run -pl ai-agent

## Configuration

| Property | Description | Default |
|---|---|---|
| `ai-agent.filtering.stop-words` | Words that trigger filtering | `[spam, ads, promo]` |
| `ai-agent.filtering.excluded-authors` | Authors to ignore | `[bot-user]` |
| `ai-agent.filtering.min-length` | Minimum text length | `20` |
| `ai-agent.summarization.threshold` | Characters before summarization | `500` |
| `ai-agent.summarization.mode` | `stub` or `ai` | `ai` |
| `ai-agent.ai-api.token` | HuggingFace API token | `$HUGGINGFACE_TOKEN` |

## Summarization modes

- **`stub`** — truncates text to threshold + "..."
- **`ai`** (default) — calls HuggingFace Inference API (BART-large-CNN). Falls back to original text if token is missing or API fails.

To switch to stub mode, set in `application.yaml`:

    ai-agent:
      summarization:
        mode: stub

## Running tests

Unit tests only (no Docker needed):

    ./mvnw test -pl ai-agent -Dtest="UpdateFilterImplTest,StubSummarizerTest,UpdateProcessorImplTest"

All tests (Docker required for Testcontainers):

    ./mvnw test -pl ai-agent

## Test coverage

| Test class | Covers |
|---|---|
| `UpdateFilterImplTest` | TC-2.1 stop-words, TC-2.2 excluded author, TC-2.3 min-length, TC-2.4 valid passes |
| `StubSummarizerTest` | TC-3.1 long text truncated, TC-3.2 boundary |
| `UpdateProcessorImplTest` | TC-3.1 summarizer called, TC-3.2 summarizer skipped, filtered not published |
| `RawUpdateConsumerIntegrationTest` | TC-1.1 end-to-end valid message, TC-1.2 malformed message survives |
