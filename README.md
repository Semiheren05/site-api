# site-api

[![CI](https://github.com/Semiheren05/site-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Semiheren05/site-api/actions/workflows/ci.yml)

Backend API for my personal website. It handles accounts and authentication today and will serve the site's content (blog posts, portfolio items, contact form) as it grows. It runs on my own Ubuntu home server behind Nginx and Cloudflare Tunnel.

## Tech stack

| Area | Choice |
|---|---|
| Language / framework | Java 21, Spring Boot 3.5 |
| Security | Spring Security, stateless JWT (jjwt), BCrypt, role-based access |
| Persistence | PostgreSQL, Spring Data JPA / Hibernate, Flyway migrations |
| API docs | OpenAPI 3 / Swagger UI (springdoc) |
| Testing | JUnit 5, Mockito, AssertJ, Testcontainers |
| Delivery | Docker (multi-stage), Docker Compose, GitHub Actions |

## Features

- **Register / login** returning a signed JWT access token
- **Role-based authorization** (`USER`, `ADMIN`); admin routes under `/api/admin/**`
- **First admin bootstrap** from environment variables, idempotent
- **Consistent errors**: every error is an [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem-detail JSON, including 401/403 from the security layer
- **Request validation** with per-field error messages
- **Schema owned by Flyway**; Hibernate only validates it
- **CORS** restricted to the website's own origins
- **Health endpoint** for uptime checks (`/actuator/health`)

## Getting started

### With Docker Compose (API + PostgreSQL)

```bash
cp .env.example .env
# set JWT_SECRET in .env, e.g. with: openssl rand -base64 48
docker compose up --build
```

The API is then at `http://localhost:8080` and Swagger UI at `http://localhost:8080/swagger-ui.html`.

### Running locally from your IDE

```bash
docker compose up -d db     # only the database
mvn spring-boot:run
```

Without `JWT_SECRET` set, a development-only secret from `application.yml` is used. Never rely on it outside local development.

### Configuration

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/siteapi` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `siteapi` / `siteapi` | Database credentials |
| `JWT_SECRET` | dev-only value | Base64 HMAC key, at least 32 bytes |
| `JWT_EXPIRATION` | `1h` | Access token lifetime |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5500,http://localhost:3000` | Comma-separated browser origins |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | empty | Creates the first admin on startup if both are set |

## API

| Method | Path | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | public | Create an account, returns a token (201) |
| `POST` | `/api/auth/login` | public | Email + password → token |
| `GET` | `/api/users/me` | authenticated | The current user |
| `GET` | `/api/admin/users?page=0&size=20` | `ADMIN` | Paginated user list, newest first |
| `GET` | `/actuator/health` | public | Health check |

Example:

```bash
curl -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"semih@example.com","password":"password123"}'
# {"accessToken":"eyJhbGciOi...","tokenType":"Bearer","expiresIn":3600}

curl localhost:8080/api/users/me -H 'Authorization: Bearer eyJhbGciOi...'
```

Error example (validation):

```json
{
  "type": "about:blank",
  "title": "Validation error",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/auth/register",
  "errors": {
    "email": "must be a well-formed email address",
    "password": "size must be between 8 and 72"
  }
}
```

## Project structure

```
src/main/java/com/bearsemih/siteapi
├── auth/        register & login (controller, service, DTOs)
├── user/        user entity, repository, /me and admin endpoints, admin bootstrap
├── security/    JWT service & filter, security config, CORS, 401/403 handling
├── common/      global exception handler and domain exceptions
└── config/      OpenAPI and web configuration
src/main/resources/db/migration   Flyway SQL migrations
```

## Tests

```bash
mvn verify
```

Unit tests cover the JWT logic and auth rules with Mockito. Integration tests start a real PostgreSQL with Testcontainers (Docker required; they are skipped when Docker is unavailable) and exercise the full HTTP flow: registration, login, validation errors, 401/403/409 responses and pagination. GitHub Actions runs the whole suite and builds the Docker image on every push.

## Roadmap

- [x] Authentication, roles, error handling, tests, CI
- [ ] Blog posts & portfolio items (admin CRUD, public read API)
- [ ] Contact form with rate limiting and e-mail notification
- [ ] Refresh tokens
- [ ] Privacy-friendly page view statistics
- [ ] Automatic deployment to the home server from GitHub Actions
