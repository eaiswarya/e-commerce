# Library Management System

Staff-facing web app for managing a library's books, members, and loans.

- `backend/` — Spring Boot 4 REST API (Java 17)
- `frontend/` — React + Vite + TypeScript (coming soon)

Design: [`docs/plans/2026-09-28-library-management-design.md`](docs/plans/2026-09-28-library-management-design.md)

## Prerequisites

- Java 17+
- Node 22+ (frontend)

Maven is not required; the backend ships a Maven wrapper.

## Backend

```bash
cd backend
./mvnw spring-boot:run   # API on http://localhost:8080 (Windows PowerShell: .\mvnw.cmd spring-boot:run)
./mvnw verify            # tests + coverage report at target/site/jacoco/index.html
```

The default `dev` profile uses an in-memory H2 database (console at http://localhost:8080/h2-console).
The `prod` profile uses PostgreSQL and reads `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` from the environment:

```bash
SPRING_PROFILES_ACTIVE=prod DB_URL=jdbc:postgresql://localhost:5432/library DB_USERNAME=library DB_PASSWORD=secret ./mvnw spring-boot:run
```
