# Library Management System

A web application for managing a library: books catalogue, members, loans/returns, search, and authentication.

## Repository layout

```
backend/    Spring Boot 4 REST API (Java 17, Maven wrapper)
frontend/   React + Vite + TypeScript SPA
.claude/    AI harness: project skills (testing, pr-review, raise-pr)
.github/    PR template, CI workflows
```

`backend/` and `frontend/` may not exist yet; skip steps for a side that isn't there.

## Tech stack

- **Backend:** Spring Boot 4.1, Spring Web MVC, Spring Data JPA, Spring Security, Bean Validation, Lombok, JUnit 5, Mockito, Spring Boot Test, JaCoCo
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
- Layered: `controller` → `service` → `repository`, with packages by layer under `com.library`: `controller`, `service`, `repository`, `entity` (JPA entities), `dto` (request/response records), `security`, `exception` (custom exceptions + `GlobalExceptionHandler`), `config` (`@ConfigurationProperties`, beans). Tests mirror the same packages.
- Controllers accept/return DTOs only — never expose JPA entities.
- Use Lombok instead of boilerplate: `@RequiredArgsConstructor` for constructor injection, `@Slf4j` for loggers, `@Getter` / `@NoArgsConstructor(access = PROTECTED)` on entities. DTOs are Java records.
- Validate input with `@Valid` + Bean Validation annotations.
- Errors handled centrally in a `@RestControllerAdvice`; return consistent error JSON.
- `@Transactional` on service methods that write; keep it out of controllers.
- Config via `application.yml` + environment variables; no secrets in the repo.
- Security: every `/api/**` route requires a JWT except `POST /api/auth/login`. Security errors (401/403) go through `GlobalExceptionHandler`. Get the current librarian with `@AuthenticationPrincipal Jwt jwt` (`jwt.getSubject()` is the username).
- Nested `@ConfigurationProperties` records need `@Valid` for their constraints to apply.
- Inject `java.time.Clock` (bean in `TimeConfig`) instead of calling `Instant.now()`/`LocalDate.now()`, so time-based logic is testable.

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
