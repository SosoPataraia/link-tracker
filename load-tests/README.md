# Load Tests

## Setup

1. Start the full stack: `docker compose up -d`
2. Seed the database: `Get-Content load-tests/seed.sql | docker exec -i postgresql psql -U postgres -d scrapper`
3. Start scrapper: `cd scrapper && ..\mvnw.cmd spring-boot:run "-Dapp.scheduler.interval=999999999"`
4. Run JMeter: `C:\apache-jmeter-5.6.3\bin\jmeter.bat -n -t load-tests\link-tracker.jmx -l load-tests\results.csv`

## Test Configuration

- 1000 chats, 100 links each = 100,000 links total
- 32 threads (2 × 16 cores)
- Ramp-up: 60 seconds
- Duration: 5 minutes
- GET /links only

## Results

| Scenario | RPS | Avg ms | Min ms | Max ms | Errors |
|---|---|---|---|---|---|
| No cache | 864.3 | 33 | 2 | 1273 | 0% |
| Valkey cache | 864.3 | 38 | 2 | 476 | 0% |
| Client-side cache | 922.1 | 31 | 2 | 852 | 0% |

## Analysis

Client-side caching achieved the highest throughput (922 RPS) by serving responses directly from JVM memory, avoiding both DB and Redis round-trips for repeated requests. Valkey cache showed lower max latency (476ms vs 1273ms no-cache) demonstrating better tail latency under load. No-cache mode performed surprisingly well due to PostgreSQL query caching and indexed lookups on a warm dataset.
