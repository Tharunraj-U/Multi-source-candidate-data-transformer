# Candidate Profile System — Backend (partial)

Spring Boot 3.4 / Java 21 module with **entities** and **parser services** only.

## Infrastructure

| Service | How to run |
|---------|------------|
| **MySQL** | Local MySQL 8 (`MySQL80` service) — `root` / `root` |
| **Redis** | Docker only: `docker compose up -d` |

Database `candidate_db` is created automatically on first run (`createDatabaseIfNotExist=true`).

## Database initialization

Flyway runs `V1__init.sql` on startup:
- 13 application tables + `flyway_schema_history`
- 7 seed rows in `skill_alias`

Hibernate validates entities against the schema (`ddl-auto: validate`).

## Run

```bash
# Start Redis
docker compose up -d

# Start app
mvn spring-boot:run
```

Health check: http://localhost:8080/actuator/health

## Configuration (`application.properties`)

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/candidate_db
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD:root}
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.maximum-pool-size=10
```

Optional env vars: `GITHUB_TOKEN`, `LINKEDIN_EMAIL`, `LINKEDIN_PASSWORD`

## Included

### JPA Entities (`domain/entity/`)
`Candidate`, `RawSource`, `CandidateEmail`, `CandidatePhone`, `CandidateSkill`, `CandidateExperience`, `CandidateEducation`, `CandidateLink`, `CandidateProvenance`, `CandidateConfidence`, `RuntimeConfig`, `ProcessingJob`, `SkillAlias`

### Parser Services (`service/parser/`)
`ResumeParserService`, `LinkedInScraperService`, `GitHubService`

## Not included (yet)

Controllers, repositories, merge/confidence services, orchestrator.
