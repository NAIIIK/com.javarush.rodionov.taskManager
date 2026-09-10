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
- **AOP-based logging** - every `@Service` method call is logged (arguments, return
  value, execution time, thrown exceptions). Sensitive data (passwords, tokens) is
  masked via custom `@Sensitive` / `@SensitiveResult` annotations, never printed in
  plain text.
- **Global exception handling** via `@RestControllerAdvice` - consistent JSON error
  responses with status, message, timestamp, and request path.

## Tech stack

| Layer      | Technology                                                           |
|------------|----------------------------------------------------------------------|
| Language   | Java 21                                                              |
| Framework  | Spring Boot (Web MVC, Security, Data JPA, AOP, Validation, Actuator) |
| Database   | PostgreSQL, migrations via Liquibase                                 |
| Auth       | JWT (`jjwt`), access + refresh tokens                                |
| Build      | Maven                                                                |
| Logging    | SLF4J + Logback (`logback-spring.xml`), custom AOP logging aspect    |
| Tests      | JUnit 5, Mockito (unit), Testcontainers + PostgreSQL (integration)   |
| Containers | Docker, Docker Compose (app + Postgres + pgAdmin)                    |

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

```bash
docker compose up --build
```

This starts PostgreSQL, pgAdmin (`http://localhost:5050`, `admin@taskmanager.local` /
`admin`), and the application (`http://localhost:8080`).

### Run locally

Requires Java 21 and a running PostgreSQL instance matching
`src/main/resources/application.yaml` (or override via environment variables).

```bash
./mvnw spring-boot:run
```

### Environment variables

| Variable            | Description                           | Default (dev only)       |
|---------------------|---------------------------------------|--------------------------|
| `JWT_SECRET`        | HMAC signing secret for access tokens | insecure dev placeholder |
| `POSTGRES_USER`     | Database user                         | `taskmanager`            |
| `POSTGRES_PASSWORD` | Database password                     | `taskmanager`            |
| `LOG_PATH`          | Directory for the log file            | `logs`                   |

> Never rely on the default `JWT_SECRET` outside local development.

## API overview

All endpoints except `/api/auth/**` require a valid access token
(`Authorization: Bearer <token>`).

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

## Logging

Logging is configured in `logback-spring.xml`:

- Console output with colorized pattern (Spring Boot defaults).
- A rolling text file at `${LOG_PATH}/task-manager.log`, rotated daily and when it
  exceeds 50 MB, keeping 14 days of history (capped at 1 GB total).

On top of that, `logging.LoggingAspect` wraps every `@Service` method and logs:

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