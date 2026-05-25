# AI Agent Service

Intermediate service in the Link Tracker pipeline. Consumes raw link updates from Scrapper,
applies filtering, summarization, prioritization and grouping, then publishes processed
events for the Bot.

## Architecture

```
Scrapper → link.raw-updates → AI Agent → link.processed-updates → Bot → Telegram
```

## Requirements

- Java 25
- Docker (for Kafka + Schema Registry)

## Running locally

### 1. Start infrastructure

```bash
docker compose up -d
```

Wait ~20 seconds for Kafka to be ready and topics to be created.

### 2. Configure secrets

Create a `.env` file in the project root:

```
HUGGINGFACE_TOKEN=your_hf_token_here
```

The `.env` file is in `.gitignore` and must never be committed.
The app works without a token — summarization falls back to the original text.

### 3. Run from IDE

Run `AiAgentApplication.main()` directly — no additional setup beyond Docker.

### 4. Run from Maven

```bash
./mvnw spring-boot:run -pl ai-agent -am      # Linux/macOS
..\mvnw spring-boot:run -pl ai-agent -am     # Windows
```

## Configuration

| Property | Default | Description |
|---|---|---|
| `ai-agent.filtering.stop-words` | spam, ads, promo | Words that trigger filtering |
| `ai-agent.filtering.excluded-authors` | bot-user | Authors to ignore |
| `ai-agent.filtering.min-length` | 20 | Minimum text length |
| `ai-agent.summarization.threshold` | 500 | Characters before summarization |
| `ai-agent.summarization.mode` | ai | `stub` or `ai` |
| `ai-agent.ai-api.token` | `$HUGGINGFACE_TOKEN` | HuggingFace API token |
| `ai-agent.prioritization.high-keywords` | critical, urgent, breaking, security | Keywords that trigger HIGH priority |
| `ai-agent.prioritization.low-keywords` | minor, typo, chore, docs | Keywords that trigger LOW priority |
| `ai-agent.grouping.window-ms` | 30000 | Grouping window in milliseconds |

## Summarization modes

- **`stub`** — truncates text to threshold + "..."
- **`ai`** (default) — calls HuggingFace Inference API (BART-large-CNN). Falls back to original text if token is missing or API fails.

To switch to stub mode:

```yaml
ai-agent:
  summarization:
    mode: stub
```

## Prioritization

Updates are assigned priority based on keywords in the description:

- **HIGH** — description contains any high-keyword (e.g. `critical`, `security`)
- **LOW** — description contains any low-keyword (e.g. `typo`, `docs`) and no high-keyword
- **MEDIUM** — no matching keywords found

HIGH always wins over LOW if both are present.

## Grouping

Updates addressed to the same `tgChatId` that arrive within `window-ms` are merged into
one message with a numbered list. The priority of the merged message is the maximum
of all buffered priorities (HIGH > MEDIUM > LOW).

If only one update arrives for a chat within the window, it is passed through unchanged.

## Running tests

Unit tests only (no Docker needed):

```bash
./mvnw test -pl ai-agent -am -Dtest="UpdateFilterImplTest,StubSummarizerTest,UpdateProcessorImplTest,PrioritizationServiceImplTest,GroupingServiceImplTest"
```

All tests (Docker required for Testcontainers):

```bash
./mvnw test -pl ai-agent -am
```

## Test coverage

| Test class | Covers |
|---|---|
| `UpdateFilterImplTest` | Stop-words, excluded author, min-length, valid passes |
| `StubSummarizerTest` | Long text truncated, boundary |
| `UpdateProcessorImplTest` | Summarizer called/skipped, filtered not published |
| `PrioritizationServiceImplTest` | TC-1.1 high keyword → HIGH, TC-1.2 no keywords → MEDIUM, TC-1.3 low keyword → LOW |
| `GroupingServiceImplTest` | TC-2.1 multiple updates grouped into numbered list, TC-2.2 single update passes through |
| `ProcessedUpdateIntegrationTest` | TC-3.1 successful publish with correct priority, TC-3.2 filtered message not published |
