# Library Management System

A web application for managing a library: books catalogue, members, loans/returns, search, and authentication.

## Repository layout

```
backend/    Spring Boot 3 REST API (Java 17, Maven wrapper)
frontend/   React + Vite + TypeScript SPA
.claude/    AI harness: project skills (testing, pr-review, raise-pr)
.github/    PR template, CI workflows
```

`backend/` and `frontend/` may not exist yet; skip steps for a side that isn't there.

## Tech stack

- **Backend:** Spring Boot 3, Spring Web, Spring Data JPA, Spring Security, Bean Validation, JUnit 5, Mockito, Spring Boot Test, JaCoCo
- **Frontend:** React, Vite, TypeScript, React Router, Vitest, React Testing Library, ESLint, Prettier

## Commands

Backend (run from `backend/`; on Windows use `mvnw.cmd`):

```bash
./mvnw verify                       # compile + all tests + coverage
./mvnw test -Dtest=BookServiceTest  # single test class
./mvnw spring-boot:run              # run API locally
```

Frontend (run from `frontend/`):

```bash
npm ci
npm run lint
npm test -- --run    # Vitest, single run
npm run build
npm run dev          # local dev server
```

## Conventions

### Backend
- Layered: `controller` → `service` → `repository`; packages by feature (`book`, `member`, `loan`, `auth`).
- Controllers accept/return DTOs only — never expose JPA entities.
- Validate input with `@Valid` + Bean Validation annotations.
- Errors handled centrally in a `@RestControllerAdvice`; return consistent error JSON.
- `@Transactional` on service methods that write; keep it out of controllers.
- Config via `application.yml` + environment variables; no secrets in the repo.

### Frontend
- Functional components + hooks only.
- All HTTP calls go through `frontend/src/api/`; components never call `fetch` directly.
- Every data-loading view handles loading, error, and empty states.
- Co-locate tests: `Component.tsx` + `Component.test.tsx`.

### Tests
- Every new behaviour ships with tests. Services: unit tests with Mockito. Controllers: `@WebMvcTest`. Repositories: `@DataJpaTest`. UI: Vitest + RTL, API layer mocked.
- Never delete, skip, or weaken a test to make a build pass.

## Git workflow

- Never commit directly to `main`.
- Branch names: `feature/<short-desc>`, `fix/<short-desc>`, `chore/<short-desc>`.
- Commit messages follow Conventional Commits (`feat:`, `fix:`, `chore:`, `test:`, `refactor:`, `docs:`).
- All changes land via pull request against `main`.

## Definition of done

Before finishing any task:
1. `/testing` — tests written and passing
2. `/pr-review` — self-review, fix Critical/Important findings
3. `/raise-pr` — push branch and open the PR
