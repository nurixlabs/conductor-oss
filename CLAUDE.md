# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```shell
./gradlew build                                              # compile + test + spotless check
./gradlew spotlessApply                                      # fix formatting (always run before committing)
./gradlew :conductor-<module>:test                           # test a single module
./gradlew :conductor-<module>:test --tests "com.example.MyTest"  # run a single test class
./gradlew :conductor-test-harness:test                       # integration tests (Groovy/Spock, needs Docker)
./gradlew :conductor-server:bootRun                          # start server (requires Redis + ES7)
./gradlew :conductor-server-lite:bootRun                     # start server with SQLite only (zero external deps)
cd docker && docker-compose up                               # full stack (server + Redis + ES7)
```

A pre-commit hook exists at `hooks/pre-commit` that runs `spotlessApply` automatically. Install it with:
```shell
ln -s ../../hooks/pre-commit .git/hooks/pre-commit
```

## Architecture

### Module Dependency Layers

```
annotations / annotations-processor
         ↓
      common          (DTOs, model classes, protobuf types)
         ↓
       core           (DAO interfaces, WorkflowExecutor, DeciderService, system tasks)
         ↓
  rest / grpc-server  (API controllers)
         ↓
      server          (Spring Boot app — wires all persistence/indexing/event modules)
```

**Rule**: DAO interfaces are defined in `core` under `com.netflix.conductor.dao`. Implementations belong in persistence modules (`mysql-persistence`, `postgres-persistence`, `redis-persistence`, etc.).

### Core DAO Interfaces (`core/src/main/java/com/netflix/conductor/dao/`)

| Interface | Responsibility |
|---|---|
| `ExecutionDAO` | Workflow and task execution state (CRUD) |
| `MetadataDAO` | Workflow and task definitions |
| `IndexDAO` | Search indexing (ES7/OpenSearch/Postgres/SQLite) |
| `QueueDAO` | Task queue operations |
| `EventHandlerDAO` | Event handler CRUD |
| `PollDataDAO` | Worker poll tracking |
| `RateLimitingDAO` | Rate limit enforcement |
| `ConcurrentExecutionLimitDAO` | Concurrency constraints |

### Execution Flow

1. `WorkflowExecutor` — orchestrates all workflow state transitions
2. `DeciderService` — determines which tasks to schedule next given current workflow state
3. `AsyncSystemTaskExecutor` — drives async system task lifecycle (polls queues, calls `start()`/`execute()`)
4. `ExecutionDAOFacade` — unified facade over `ExecutionDAO` + `IndexDAO` + `QueueDAO`; all reads/writes go through here

### System Tasks

Extend `WorkflowSystemTask` + implement `TaskMapper`. Both must be Spring `@Component`s — Spring auto-discovers them into `SystemTaskRegistry` and `ConductorCoreConfiguration`. No explicit registration needed.

Key overrides in `WorkflowSystemTask`:
- `isAsync()` — `true` puts task on an internal queue polled by `SystemTaskWorkerCoordinator`; `false` runs inline in `decide()`
- `isAsyncComplete()` — `true` means task stays `IN_PROGRESS` until an external signal completes it (e.g., SUB_WORKFLOW)
- `start()` — called once on first execution (SCHEDULED → IN_PROGRESS)
- `execute()` — called on each poll cycle when still IN_PROGRESS (async tasks only); return `true` if you changed status

### Indexing Architecture

`IndexDAO` is a search-only index, not the source of truth. MySQL is the source of truth.

- `ExecutionDAOFacade` writes to both `ExecutionDAO` (MySQL) and `IndexDAO` (OpenSearch) inline on every workflow/task mutation
- `NoopIndexDAO` is wired when no indexing backend is configured — all search endpoints return empty results
- `conductor.app.asyncIndexingEnabled=true` makes index writes non-blocking (pushed to an internal queue)
- Only `WorkflowSummary` and `TaskSummary` (not full payloads) are indexed; raw JSON is only written on archival

### Persistence Backends

Selected at runtime via Spring properties:
- `conductor.db.type` — controls which `ExecutionDAO`/`MetadataDAO` bean is active
- `conductor.queue.type` — controls which `QueueDAO` bean is active
- `conductor.indexing.enabled` + indexing module on classpath — controls which `IndexDAO` is active

The `server` module selects the indexing persistence module at **build time** via `-PindexingBackend` Gradle property (default: `es7`).

### Testing Conventions

- Unit tests: Java/JUnit 5 or Groovy/Spock in `src/test/` of each module
- Integration tests: Groovy/Spock specs in `test-harness` module; run serially (`maxParallelForks=1`); use Testcontainers for external services
- Prefer real implementations over mocks; use Testcontainers for databases and caches
- Test class naming: `*Test.java` for unit, `*Spec.groovy` for Spock

## Code Style

- Spotless with Google Java Format (AOSP style) is enforced in CI
- Import order: `java` → `javax` → `org` → `com.netflix` → others → static
- License header (Apache 2.0) is required on all source files — Spotless enforces this
- No emojis in code, logs, or comments
- Comment algorithms and design decisions; do not add obvious inline comments
