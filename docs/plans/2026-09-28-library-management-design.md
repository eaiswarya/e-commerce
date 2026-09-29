# Library Management System — Design

**Date:** 2026-09-28
**Status:** Approved

## Goal

A staff-facing web app for a library: manage books and members, record borrowing and returns, and search the catalogue.

## Decisions

| Topic | Decision |
|-------|----------|
| Users | Librarians/admins only log in. Members are records, no login. |
| Database | PostgreSQL (prod), H2 in-memory (dev + tests). Schema via Flyway. |
| Copies | Count per title: `totalCopies` / `availableCopies`. |
| Loan rules | 14-day loan period, max 5 active loans per member, blocked while any loan is overdue. No fines. Values configurable. |
| Auth | Stateless JWT (Bearer), BCrypt passwords, 8h expiry, admin seeded on first start. |
| Runtime | Java 17, Spring Boot 4.1.1, Maven wrapper; React 18 + Vite + TypeScript. |

## Architecture

```
backend/   Spring Boot REST API on :8080, profiles dev (H2) and prod (PostgreSQL)
frontend/  React SPA; Vite dev server proxies /api → :8080
```

Hibernate runs with `ddl-auto=validate`; Flyway owns the schema.

### Backend packages (by layer)

```
com.library
├── controller   REST controllers (AuthController, BookController, ...)
├── service      business logic and rules (AuthService, TokenService, BookService, ...)
├── repository   Spring Data repositories and Specifications
├── entity       JPA entities (Librarian, Book, Member, Loan)
├── dto          request/response records, ErrorResponse, PageResponse
├── security     SecurityConfig, AdminSeeder
├── exception    NotFoundException, BusinessRuleException, GlobalExceptionHandler
└── config       LibraryProperties, TimeConfig
```

Each feature adds one class per layer it needs (e.g. books: `BookController`, `BookService`, `BookRepository` + `BookSpecifications`, `Book`, `BookRequest`/`BookResponse`). Lombok removes boilerplate (`@RequiredArgsConstructor`, `@Slf4j`, `@Getter` on entities); DTOs are records.

## Data model

| Table | Columns |
|-------|---------|
| `librarian` | id, username (unique), password_hash, full_name |
| `book` | id, isbn (unique), title, author, category, published_year, total_copies, available_copies, version |
| `member` | id, member_code (unique, `M0001`), full_name, email (unique), phone, active, joined_at, version |
| `loan` | id, book_id → book, member_id → member, borrowed_at, due_date, returned_at (null = active) |

Constraints and invariants:
- `CHECK (0 <= available_copies AND available_copies <= total_copies)`
- `version` columns give optimistic locking; concurrent borrows of the last copy cannot oversell.
- Books and members with active loans cannot be deleted. Members are deactivated, never hard-deleted, to keep history.

## API

All routes under `/api`, JWT required except login. List endpoints are paged (`?page=0&size=20&sort=title,asc`) and return `{ content, page, size, totalElements, totalPages }`. `size` is capped at 100; an unknown `sort` field is a 400 `BAD_REQUEST`.

### Auth
- `POST /api/auth/login` `{username, password}` → `{token, expiresAt, fullName}`
- `GET /api/auth/me`

### Books
- `GET /api/books?q=&category=&available=` — `q` matches title or author (contains, case-insensitive) or the ISBN (exact, hyphens/spaces ignored); `category` is case-insensitive; `available=true` keeps only books with a free copy (omitted or `false` = no filter). Default sort `title`.
- `GET /api/books/{id}`, `POST /api/books` (201 + `Location`), `PUT /api/books/{id}`, `DELETE /api/books/{id}` (204)
- Request: `{isbn, title, author, category?, publishedYear?, totalCopies, version}`. ISBN-10 or ISBN-13, stored without hyphens/spaces. `version` is ignored on `POST` and **required on `PUT`**.
- Response: `{id, isbn, title, author, category, publishedYear, totalCopies, availableCopies, version}`. Send `version` back unchanged on the next `PUT`; if the book changed in the meantime the update is rejected with 409 `CONCURRENT_UPDATE` (reload and retry). A successful `PUT` returns the new `version`.
- Changing `totalCopies` shifts `availableCopies` by the same amount; rejected (409 `COPIES_ON_LOAN`) if total would fall below copies currently on loan.

### Members
- `GET /api/members?q=&active=`: `q` matches name or email (contains, case-insensitive) or the whole member code (case-insensitive). `active=true` keeps active members, `active=false` keeps only inactive ones, and omitting it applies no filter. Default sort is `fullName`.
- `GET /api/members/{id}`, `POST /api/members` (201 + `Location`), `PUT /api/members/{id}`, `PATCH /api/members/{id}/deactivate` (200, idempotent). There is no DELETE.
- Request: `{fullName, email, phone?, version}`.
  - Email is stored lowercased and must be unique regardless of case (409 `DUPLICATE`).
  - Phone is an optional leading `+` followed by 3–29 digits, spaces, hyphens or parentheses; an empty phone is stored as none.
  - `version` is ignored on `POST` and **required on `PUT`**; a stale one gives 409 `CONCURRENT_UPDATE`, as for books.
- Response: `{id, memberCode, fullName, email, phone, active, joinedAt, version}`.
  - `memberCode` is assigned on create from a DB sequence (`M0001`, `M0002`, ...) and never changes.
  - `joinedAt` is the creation instant.
- `GET /api/members/{id}/loans?status=active|returned|all`: added with the loans PR, because it needs the `loan` table.

### Loans
- `POST /api/loans` `{bookId, memberId}` — requires active member, fewer than max active loans, no overdue loans, and an available copy. Decrements `availableCopies`, sets `dueDate = today + periodDays`.
- `POST /api/loans/{id}/return` — sets `returnedAt`, increments `availableCopies`; rejected if already returned.
- `GET /api/loans?status=active|overdue|returned&memberId=&bookId=`

Borrow and return each run in one transaction.

### Errors

`GlobalExceptionHandler` returns `{ status, error, message, fieldErrors?, timestamp }`.

| Case | Status | `error` |
|------|--------|---------|
| Validation failure | 400 | `VALIDATION_FAILED` + `fieldErrors` |
| Bad credentials / missing or expired token | 401 | `UNAUTHORIZED` |
| Resource missing | 404 | `NOT_FOUND` |
| No copies available | 409 | `NO_COPIES_AVAILABLE` |
| Loan limit reached | 409 | `LOAN_LIMIT_REACHED` |
| Member has overdue loans | 409 | `MEMBER_HAS_OVERDUE` |
| Member inactive | 409 | `MEMBER_INACTIVE` |
| Loan already returned | 409 | `ALREADY_RETURNED` |
| Delete with active loans | 409 | `HAS_ACTIVE_LOANS` |
| Total copies below copies on loan | 409 | `COPIES_ON_LOAN` |
| Duplicate ISBN / email (including a race caught by the DB unique index) | 409 | `DUPLICATE` |
| Optimistic lock conflict / stale `version` on update | 409 | `CONCURRENT_UPDATE` |
| Unknown `sort` field, malformed parameter | 400 | `BAD_REQUEST` |

### Configuration

```yaml
library:
  loan:
    period-days: 14
    max-active: 5
  jwt:
    secret: ${JWT_SECRET}
    expiry: 8h
```

## Frontend

React 18, TypeScript, Vite, React Router, TanStack Query, CSS modules with a small shared component set.

### Pages

| Route | Content |
|-------|---------|
| `/login` | Username/password form |
| `/` | Dashboard: totals (books, active members, active loans, overdue) and overdue list |
| `/books` | Search, category and "available only" filters, paged table, add/edit/delete |
| `/books/:id` | Details and current loans |
| `/members` | Search, paged table, add/edit, deactivate |
| `/members/:id` | Details, active and past loans, Return button per active loan |
| `/loans` | Active / Overdue / Returned tabs, Borrow dialog |

**Borrow flow:** pick member (searchable) → pick book (searchable, available only) → confirm. A 409 is shown as a plain-language message.

### Layout

```
src/
├── api/         client.ts (fetch wrapper: token, error mapping), books.ts, members.ts, loans.ts, auth.ts
├── auth/        AuthContext, ProtectedRoute
├── components/  Table, Pagination, SearchInput, Modal, FormField, ErrorBanner, EmptyState
├── pages/       one folder per page
└── types.ts     shared API types
```

- Token held in memory and `sessionStorage`.
- A 401 clears the token and redirects to `/login`.
- Every page handles loading, error (with retry) and empty states.
- Forms validate on the client; API `fieldErrors` map onto fields.

## Testing

**Backend** (`./mvnw verify`, JaCoCo):
- Service unit tests (JUnit 5 + Mockito) covering every loan rule and the book copy-count edit rule
- `@WebMvcTest` controller tests: 400 / 401 / 404 / 409
- `@DataJpaTest` repository tests: search, overdue queries, constraints
- One `@SpringBootTest` flow: login → add book → add member → borrow → return

**Frontend** (Vitest + React Testing Library, API mocked):
- API client: token, 401 handling, error mapping
- Books / Members / Loans pages: loading, error, empty, success
- Borrow dialog, including 409 display
- Login and protected-route redirect

## Delivery

Each PR is built test-first and shipped with `/testing` → `/pr-review` → `/raise-pr`.

1. `feature/backend-scaffold` — Spring Boot project, profiles, Flyway, error handling, health check
2. `feature/auth` — Librarian, JWT, security config, admin seed
3. `feature/books` — CRUD and search
4. `chore/ci` — GitHub Actions running the backend checks on every PR (moved ahead so later PRs are checked automatically)
5. `feature/members` — CRUD, search, deactivate
6. `feature/loans` — borrow, return, rules
7. `feature/frontend-scaffold` — Vite, routing, API client, auth, login page; adds the frontend CI job
8. `feature/frontend-pages` — dashboard, books, members, loans

## Out of scope (v1)

Fines, member logins, reservations, email reminders, barcodes / per-copy tracking, multiple branches.
