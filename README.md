# Pulse

A CI/CD monitoring dashboard for GitHub Actions. Add any GitHub repository and Pulse tracks its workflow runs: the latest status, the commit and author, run duration, a 10-run history strip and the recent success rate. Data refreshes automatically.

**Stack:** Spring Boot 3 (Java 17) · PostgreSQL + Flyway · Angular 17 + Tailwind CSS · Docker Compose

## How it works

- You add repositories from the dashboard (`owner/repo` or a GitHub URL).
- The backend polls the GitHub Actions API every 5 minutes (configurable) and upserts workflow runs into Postgres. It keys them by run ID, so queued and running builds update when they finish.
- The Angular dashboard reads `/api/repositories/summary` and refreshes every 30 seconds.

## Quick start (Docker)

```bash
cp .env.example .env      # optional: set GITHUB_TOKEN
docker compose up --build
```

- Dashboard: http://localhost:4200
- API: http://localhost:8080/api
- Health: http://localhost:8080/actuator/health

## Local development

**Prerequisites:** Java 17+, Maven, Node 18+, PostgreSQL. The database must exist; Flyway creates the tables.

```bash
createdb cicd_dashboard

# Terminal 1: backend
mvn spring-boot:run

# Terminal 2: frontend (proxies /api to localhost:8080)
cd frontend
npm install
npm start
```

Then open http://localhost:4200.

### Tests

```bash
mvn test                    # runs against in-memory H2, no Postgres needed
cd frontend && npm run build
```

## Configuration

All settings come from environment variables (see `.env.example`):

| Variable | Default | Purpose |
|---|---|---|
| `GITHUB_TOKEN` | *(empty)* | Optional for public repos (60 req/hr without it, 5000 with it). Required for private repos. |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `cicd_dashboard` | Postgres location |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `postgres` | Postgres credentials |
| `SCHEDULER_ENABLED` | `true` | Periodic GitHub sync |
| `SCHEDULER_GITHUB_SYNC_RATE` | `300000` | Milliseconds between syncs |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Only needed if the frontend is served from another origin |

A token needs no scopes for public repositories; for private repositories it needs `repo` (classic token) or Actions read access (fine-grained token).

## API

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/repositories` | List monitored repositories |
| GET | `/api/repositories/summary` | Repositories with latest and recent builds (dashboard view) |
| POST | `/api/repositories` | Add a repository: `{"githubUrl": "owner/repo"}` |
| POST | `/api/repositories/{id}/sync` | Sync one repository now |
| DELETE | `/api/repositories/{id}` | Stop monitoring and delete its builds |
| POST | `/api/scheduler/trigger-sync` | Sync all repositories now |
| GET | `/api/builds`, `/api/builds/{id}`, `/api/builds/repository/{id}`, `/api/builds/status/{status}` | Query stored builds |
| GET | `/api/github/status/{owner}/{repo}`, `/api/github/runs/{owner}/{repo}` | Live passthrough to the GitHub Actions API |

See [API_DOCUMENTATION.md](API_DOCUMENTATION.md) for request and response details.

## Project layout

```
src/main/java/com/peraton/cicd/
  controller/   REST endpoints
  service/      GitHub client, sync logic, scheduler
  model/        JPA entities
  dto/          API payloads
src/main/resources/db/migration/   Flyway schema migrations
frontend/                          Angular dashboard (+ nginx Dockerfile)
```

## License

MIT
