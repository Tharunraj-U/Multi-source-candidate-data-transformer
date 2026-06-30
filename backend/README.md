# Candidate Profile System — Backend

Part of the [Multi-Source Candidate Data Transformer](../README.md).

## Runtime

| Service | Required | Notes |
|---------|----------|-------|
| MySQL 8 | Yes | Local dev defaults to `candidate_db` with `root` / `root` |
| Ollama desktop app | Optional | Required only when `OLLAMA_ENABLED=true` |

## Run

```bash
cp .env.example .env
mvn spring-boot:run
```

Default endpoints:

- API: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- Ollama: `http://localhost:11434`

## Processing design

The backend does not use Redis or an external queue.

1. `POST /api/v1/candidate/upload` saves the candidate first with status `DRAFT`.
2. Each uploaded file or URL becomes a `raw_source` row with status `PENDING`.
3. `POST /api/v1/candidate/process` creates a `processing_job` row with status `QUEUED`.
4. A Spring `@Async` worker marks the job `RUNNING` and processes each source.
5. Unexpected worker failures are retried up to `APP_PROCESSING_MAX_ATTEMPTS`.
6. Per-source parse failures are stored on the `raw_source` record.
7. Final candidate status becomes `COMPLETED`, `PARTIAL`, or `FAILED`.

User-visible failure reporting is available through `GET /api/v1/candidate/{id}/job/{jobId}`. That response includes the current `status`, `attemptCount`, `maxAttempts`, and `errorMessage`.

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `OLLAMA_BASE_URL` | `http://localhost:11434/v1` | Local Ollama API |
| `OLLAMA_MODEL` | `qwen2.5-coder:3b` | Installed Ollama model |
| `OLLAMA_ENABLED` | `true` | `false` switches to non-LLM extraction paths |
| `RESUME_TEXT_PROVIDER` | `tika` | `tika` or `docling` |
| `APP_PROCESSING_MAX_ATTEMPTS` | `3` | Max async worker attempts for unexpected failures |

## Resume pipeline

`Docling or Tika -> resume text -> Ollama extraction -> parsed JSON -> candidate merge`

## Validation

```bash
mvn test
mvn test -Dtest=OllamaIntegrationTest
```
