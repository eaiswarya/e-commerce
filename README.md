# Library Management System

Staff-facing web app for managing a library's books, members, and loans.

- `backend/` — Spring Boot 4 REST API (Java 17)
- `frontend/` — React + Vite + TypeScript SPA

Design: [`docs/plans/2026-09-28-library-management-design.md`](docs/plans/2026-09-28-library-management-design.md)

## Prerequisites

- Java 17+
- Node 22+ (frontend)

Maven is not required; the backend ships a Maven wrapper.

CI (`.github/workflows/ci.yml`) runs on every pull request to `main` and on every push to `main`: the `backend` job
runs `./mvnw verify` (the JaCoCo report is attached as the `jacoco-report` artifact) and the `frontend` job runs
lint, tests and the production build.

## Backend

```bash
cd backend
./mvnw spring-boot:run   # API on http://localhost:8080 (Windows PowerShell: .\mvnw.cmd spring-boot:run)
./mvnw verify            # tests + coverage report at target/site/jacoco/index.html
```

The default `dev` profile uses an in-memory H2 database (console at http://localhost:8080/h2-console).
The `prod` profile uses PostgreSQL and reads its settings from the environment:

| Variable | Purpose |
|----------|---------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection |
| `JWT_SECRET` | Token signing key, at least 32 characters (required) |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | First admin, created on startup only when no librarians exist (username defaults to `admin`) |

```bash
SPRING_PROFILES_ACTIVE=prod DB_URL=jdbc:postgresql://localhost:5432/library DB_USERNAME=library DB_PASSWORD=secret \
  JWT_SECRET=change-me-to-a-long-random-string-32+ ADMIN_PASSWORD=choose-one ./mvnw spring-boot:run
```

### Authentication

Every `/api/**` endpoint except login needs a Bearer token. In dev the seeded login is `admin` / `admin123`.

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | python -c "import json,sys;print(json.load(sys.stdin)['token'])")
curl -s localhost:8080/api/auth/me -H "Authorization: Bearer $TOKEN"
# {"username":"admin","fullName":"Administrator"}
```

Tokens expire after 8 hours (`library.jwt.expiry`).

## Frontend

Start the API first (see above), then:

```bash
cd frontend
npm ci
npm run dev              # app on http://localhost:5173; /api is proxied to the API on :8080
npm run lint             # ESLint + Prettier check (npm run format fixes formatting)
npm test -- --run        # Vitest, single run
npm run build            # type-check and production build into dist/
```

Sign in with the dev login `admin` / `admin123`. The session is kept in `sessionStorage`, so a reload stays signed in and
closing the tab signs out. When the API rejects the token (for example after it expires), the app returns to the login page.
