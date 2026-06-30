# Candidate Profile System — Backend

Part of the [Multi-Source Candidate Data Transformer](../README.md). Spring Boot 3.4 / Java 21 module for multi-source candidate profile ingestion, parsing, merge, and API.

## Infrastructure

| Service | How to run |
|---------|------------|
| **MySQL** | Local MySQL 8 (`MySQL80` service) — `root` / `root` |
| **Redis** | Optional — `docker compose up -d` (rate limiting / future cache) |

Database `candidate_db` is created automatically on first run (`createDatabaseIfNotExist=true`).

## Run

```bash
# Optional Redis
docker compose up -d

# Configure env (see .env.example)
cp .env.example .env

# Start app
mvn spring-boot:run
```

Health check: http://localhost:8080/actuator/health

## Configuration

Key env vars in `backend/.env`:

- `GEMINI_API_KEY` — structured resume extraction (recommended)
- `GITHUB_TOKEN` — optional, higher GitHub API rate limits
- `DB_PASSWORD` — MySQL password

## Source parsers

| Parser | Input |
|--------|-------|
| `ResumeParserService` | PDF/DOCX via Tika → Gemini (Java REST) + heuristic fallback |
| `GitHubService` | GitHub REST API (profile metadata, languages, avatar) |
| `CsvImportService` | Recruiter CSV |
| `AtsJsonService` | ATS JSON blob |

Resume flow: **Tika text → Gemini JSON → merge with `ResumeTextExtractor` when sparse.**

Field-level confidence scores are written to `candidate_confidence` during merge for the Confidence tab.
