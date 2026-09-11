# Task Manager

A Jira/Trello-style task management REST API built with Spring Boot. Users can create
projects, invite members with roles, manage tasks through a fixed workflow, and leave
comments - all secured with JWT authentication.

## Features

- **JWT authentication** - register/login, access + refresh tokens, refresh token
  rotation and revocation, logout.
- **Projects** - any registered user can create a project and automatically becomes
  its owner.
- **Role-based access per project** (not global):
  - `OWNER` - full control, including deleting the project and managing member roles.
  - `MANAGER` - manages tasks and members, cannot delete the project or change roles.
  - `MEMBER` - works on assigned tasks, can self-assign to a task.
- **Tasks** - fixed status workflow (`TO_DO` → `IN_PROGRESS` → `IN_CODE_REVIEW` →
  `DONE`), priority (`LOW`/`MEDIUM`/`HIGH`), due date, assignee. Status can be changed
  by the assignee or by a role above the assignee.
- **Comments** on tasks.
- **Interactive API docs** - OpenAPI 3 spec and Swagger UI generated automatically
  from the code (springdoc).
- **AOP-based logging** - every `@Service` method call is logged (arguments, return
  value, execution time, thrown exceptions). Sensitive data (passwords, tokens) is
  masked via custom `@Sensitive` / `@SensitiveResult` annotations, never printed in
  plain text.
- **Centralized log aggregation** - in Docker, application logs are shipped through
  an ELK stack (Elasticsearch, Logstash, Kibana) for searching and filtering.
- **Global exception handling** via `@RestControllerAdvice` - consistent JSON error
  responses with status, message, timestamp, and request path.

## Tech stack

| Layer        | Technology                                                                                       |
|--------------|--------------------------------------------------------------------------------------------------|
| Language     | Java 21                                                                                          |
| Framework    | Spring Boot (Web MVC, Security, Data JPA, AOP, Validation, Actuator)                             |
| Database     | PostgreSQL, migrations via Liquibase                                                             |
| Auth         | JWT (`jjwt`), access + refresh tokens                                                            |
| API docs     | OpenAPI 3 / Swagger UI (`springdoc-openapi`)                                                     |
| Build        | Maven                                                                                            |
| Logging      | SLF4J + Logback (`logback-spring.xml`), custom AOP logging aspect                                |
| Log shipping | ELK stack (Elasticsearch, Logstash, Kibana) + Filebeat, JSON logs via `logstash-logback-encoder` |
| Tests        | JUnit 5, Mockito (unit), Testcontainers + PostgreSQL (integration)                               |
| Containers   | Docker, Docker Compose (app, Postgres, pgAdmin, ELK stack)                                       |

## Project structure

The codebase is organized **feature-first** (package per feature - entity, repository,
service, controller, DTOs live together), not layer-first:

```
com.javarush.taskmanager
├── auth/               registration, login, refresh, logout
├── user/               user entity, global role
├── project/            projects
│   └── member/         project membership & roles, access guard
├── task/               tasks
├── comment/            task comments
├── security/           JWT service, filters, current-user resolution
├── logging/            AOP logging aspect + @Sensitive/@SensitiveResult
├── exception/          custom exceptions + global exception handler
└── config/             security & web configuration
```

## Getting started

### Run with Docker Compose (recommended)

Copy the environment template and adjust values if needed:

```bash
cp .env.example .env
```

Then start everything:

```bash
docker compose up --build
```

This starts:

| Service       | URL                   | Notes                                                   |
|---------------|-----------------------|---------------------------------------------------------|
| app           | http://localhost:8080 | the API itself                                          |
| pgAdmin       | http://localhost:5050 | login `admin@taskmanager.local` / `admin`               |
| Kibana        | http://localhost:5601 | log search UI, see [Logging](#logging--log-aggregation) |
| Elasticsearch | http://localhost:9200 | log storage, no auth in dev setup                       |
| Logstash      | tcp/5044 (internal)   | receives logs from Filebeat                             |
| PostgreSQL    | localhost:5432        | –                                                       |

### Run locally

Requires Java 21 and a running PostgreSQL instance matching
`src/main/resources/application.yaml` (or override via environment variables).

```bash
./mvnw spring-boot:run
```

Running locally (no `docker` Spring profile active) skips the ELK/JSON logging setup
entirely - you get plain-text console + file logs, see
[Logging](#logging--log-aggregation).

### Environment variables

Defined in `.env` (copy from `.env.example`), consumed by `docker-compose.yaml`:

| Variable            | Description                            | Default (dev only)       |
|---------------------|----------------------------------------|--------------------------|
| `JWT_SECRET`        | HMAC signing secret for access tokens  | insecure dev placeholder |
| `POSTGRES_USER`     | Database user                          | `taskmanager`            |
| `POSTGRES_PASSWORD` | Database password                      | `taskmanager`            |
| `LOG_PATH`          | Directory for the log file (local run) | `logs`                   |

## API overview

All endpoints except `/api/auth/**` require a valid access token
(`Authorization: Bearer <token>`).

### Interactive docs (Swagger)

Once the app is running, the full interactive API reference is available at:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Raw OpenAPI 3 spec: `http://localhost:8080/v3/api-docs`

This is the source of truth for request/response shapes, validation rules, and
status codes - the tables below are a quick map, not the full contract.

### Auth - `/api/auth`

| Method | Path        | Description                             |
|--------|-------------|-----------------------------------------|
| POST   | `/register` | Create a user account                   |
| POST   | `/login`    | Obtain access + refresh tokens          |
| POST   | `/refresh`  | Exchange a refresh token for a new pair |
| POST   | `/logout`   | Revoke a refresh token                  |

### Projects - `/api/projects`

| Method | Path           | Description                             |
|--------|----------------|-----------------------------------------|
| POST   | `/`            | Create a project (caller becomes OWNER) |
| GET    | `/`            | List projects the caller is a member of |
| GET    | `/{projectId}` | Get a single project                    |

### Tasks - `/api/projects/{projectId}/tasks`, `/api/tasks/{taskId}`

| Method | Path                              | Description              |
|--------|-----------------------------------|--------------------------|
| POST   | `/api/projects/{projectId}/tasks` | Create a task (MANAGER+) |
| GET    | `/api/projects/{projectId}/tasks` | List tasks in a project  |
| PATCH  | `/api/tasks/{taskId}/assign-self` | Self-assign to a task    |
| PATCH  | `/api/tasks/{taskId}/status`      | Update task status       |

### Comments - `/api/tasks/{taskId}/comments`

| Method | Path | Description             |
|--------|------|-------------------------|
| POST   | `/`  | Add a comment to a task |
| GET    | `/`  | List comments on a task |

## Logging & log aggregation

Logging is configured in `logback-spring.xml` and behaves differently depending on
the active Spring profile:

- **Local / IDE run** (`docker` profile *not* active) - plain-text console output
  plus a rolling file at `${LOG_PATH}/task-manager.log`, rotated daily and when it
  exceeds 50 MB, keeping 14 days of history (capped at 1 GB total).
- **`docker` profile** - logs are written as structured **JSON** (via
  `logstash-logback-encoder`) to stdout only; no file is written inside the
  container, since container filesystems are ephemeral.

### Quick access to console logs

For a fast look without opening Kibana, just tail the container's stdout directly:

```bash
docker compose logs -f app
```

- `-f` (follow) streams new lines as they're written, similar to `tail -f`.
- Drop `-f` to print everything captured so far and exit.
- Add `--tail=100` to only show the last 100 lines before following:
  ```bash
  docker compose logs -f --tail=100 app
  ```
- When running locally (`./mvnw spring-boot:run`), logs go straight to your IDE/terminal
  console as usual - no extra command needed.

This is the quickest way to check whether the app started correctly or to watch a
single request go through. Use Kibana (below) instead when you need to search across
a longer time window or filter by fields like log level or logger name.

### ELK stack (Docker only)

When running via `docker compose`, container stdout is picked up and shipped
through an ELK pipeline so logs can be searched instead of grepped:

```
app (stdout, JSON) → Filebeat → Logstash → Elasticsearch → Kibana
```

- **Filebeat** autodiscovers the `taskmanager-app` container and tails its Docker
  log file.
- **Logstash** (`elk/logstash/pipeline/logstash.conf`) parses the JSON payload and
  writes it to Elasticsearch under a daily index, `taskmanager-app-YYYY.MM.dd`.
- **Kibana** (`http://localhost:5601`) is where you actually browse logs: create a
  data view with index pattern `taskmanager-app-*` and time field `@timestamp`
  under *Stack Management → Data Views*, then use *Discover* to search/filter.

This dev setup runs Elasticsearch/Kibana with security disabled
(`xpack.security.enabled: false`) - no login required.

On top of all this, `logging.LoggingAspect` wraps every `@Service` method and logs:

```
--> AuthService.login(***)
<-- AuthService.login [12 ms] returned ***
```

Arguments annotated with `@Sensitive` and results of methods annotated with
`@SensitiveResult` are masked (`***`) instead of being printed - this covers
passwords, access/refresh tokens, and any DTO or object that carries them
(`LoginRequest`, `RegisterRequest`, `RefreshRequest`, `AuthResponse`, raw JWT
strings, `CustomUserDetails`).

## Testing

```bash
./mvnw test
```

Unit tests (Mockito) cover service logic in isolation; integration tests
(Testcontainers + a real PostgreSQL container) exercise full request flows for auth,
projects, tasks, and comments.