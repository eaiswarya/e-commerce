# Frontend Catalogue Implementation Plan (frontend pages, PR 1 of 2)

## Context
The backend (PRs #3–#7) and the frontend foundation (PR #8: API client, auth, login, CI) are merged; `main` is at `2e21d29`. What remains is the design's last item: the screens librarians use every day. It ships in two PRs, as agreed:

1. **`feature/frontend-catalogue`** (this plan, in detail): shared UI components, then the Books and Members pages with search, paging, add/edit (conflict-safe) and delete or deactivate.
2. **`feature/frontend-loans`** (outlined at the end, detailed after PR 1 merges): the Loans page, the borrow dialog, loans on book and member detail with Return, and the dashboard.

Outcome of PR 1: a signed-in librarian can find, add, edit and remove books, and find, add, edit and deactivate members. Every view handles loading, error (with retry) and empty states. An edit made on stale data is caught and can be reloaded.

## Reuse from `main`
- `src/api/client.ts`: `apiFetch`, `ApiError` (`code`, `fieldErrors`). `src/api/errors.ts`: `errorMessage()` already covers `DUPLICATE`, `CONCURRENT_UPDATE`, `COPIES_ON_LOAN`, `HAS_ACTIVE_LOANS` and `HAS_LOAN_HISTORY`.
- `src/components/FormField.tsx` (forwards its ref for `register()`) and `ErrorBanner.tsx` (with `onRetry`)
- react-hook-form + zod + `zodResolver`, following the pattern in `src/pages/login/LoginPage.tsx`
- `src/test/render.tsx` (`renderWithProviders`, route with state); `src/components/layout/AppLayout.tsx` (add the nav here)
- `src/types.ts` `PageResponse<T>`, and TanStack Query from `createQueryClient()` (no retry on 4xx)
- Backend contract (design doc):
  - `GET /api/books?q&category&available&page&size&sort`
  - `POST`/`PUT` (with `version`)/`DELETE /api/books/{id}`
  - `GET /api/members?q&active`, `POST`/`PUT` (with `version`), `PATCH /api/members/{id}/deactivate`

## Libraries
- **Add `@radix-ui/react-dialog`**: an accessible modal (focus trap, Esc, `aria-modal`) for the add/edit forms and confirm prompts. It works in jsdom, unlike the native `<dialog>.showModal()`.
- **Keep react-hook-form + zod** for the book and member forms. The schemas mirror the backend's Bean Validation limits.
- **No TanStack Table.** Server-side paging and sorting leave a table that only renders rows, and a ~40-line `DataTable` is simpler than a headless table library.

## PR 1 changes (`feature/frontend-catalogue`)

### 1. Types and API modules
- `types.ts`: `Book`, `BookInput`, `Member`, `MemberInput`
- `api/client.ts`: add `toQuery(params)`, which skips empty, `undefined` and `false`-means-no-filter values
- `api/books.ts`: `searchBooks({q, category, available, page})`, `getBook`, `createBook`, `updateBook(id, input)` (includes `version`), `deleteBook`
- `api/members.ts`: `searchMembers({q, active, page})`, `getMember`, `createMember`, `updateMember`, `deactivateMember`
- `api/queryKeys.ts`: one place for keys (`['books', params]`, `['book', id]`, …), so mutations invalidate the right queries

### 2. Shared components: `src/components/`
- `QueryState`: renders loading (`role="status"`), an error (`ErrorBanner` + Retry → `refetch`), or an empty state, and the data otherwise. Every data view uses it, which meets CLAUDE.md's loading/error/empty rule in one place.
- `DataTable<T>` (column config: header plus a cell render function, and a stable `rowKey`) and `Pagination` (Previous/Next, "Page X of Y", disabled at the ends)
- `SearchInput`: a labelled input, debounced 300 ms through a small `useDebouncedValue` hook
- `Modal` (Radix Dialog wrapper with a title) and `ConfirmDialog` (a message, Confirm/Cancel, a pending state, and the error message on failure)
- `EmptyState`
- `AppLayout`: nav links Dashboard / Books / Members (Loans arrives in PR 2), with `aria-current` on the active link

### 3. Books: `src/pages/books/`
- `BooksPage` (`/books`):
  - search (title, author, ISBN), a category filter, and an "Available only" checkbox
  - filters and page live in the URL (`useSearchParams`), so reload and the Back button keep them
  - the table shows Title (links to detail), Author, ISBN, Category, and Available/Total
  - "Add book" opens `BookFormDialog`
- `BookFormDialog` (create and edit):
  - the zod schema matches the backend: ISBN-10/13 with optional hyphens or spaces, title ≤200, author ≤150, category ≤50, year 1450–2100, copies 0–1000; blank optional fields are sent as `null`
  - API `fieldErrors` map to fields through `setError`
  - Edit sends the `version` the book was loaded with
  - **On 409 `CONCURRENT_UPDATE`:** show `errorMessage()` with a "Reload latest" button. It refetches the book and resets the form to the new values and `version`, telling the user their unsaved changes were replaced.
  - Other errors show in the banner: `COPIES_ON_LOAN` and `DUPLICATE`
- `BookDetailPage` (`/books/:id`):
  - details, Edit (the same dialog), and Delete through `ConfirmDialog`
  - a 409 `HAS_ACTIVE_LOANS` or `HAS_LOAN_HISTORY` shows its message; the latter suggests setting copies to 0
  - a 404 shows "Book not found" with a link back
  - after delete, navigate to `/books` and invalidate the list

### 4. Members: `src/pages/members/`
- `MembersPage` (`/members`):
  - search (name, email, code) and an Active / Inactive / All filter (`active=true|false|omitted`), kept in the URL
  - the table shows Code, Name (links to detail), Email, Phone, and a Status badge
  - "Add member" opens `MemberFormDialog`
- `MemberFormDialog`:
  - zod: name ≤150, email trimmed and ≤254 (the API rejects padded emails), phone optional and must contain a digit
  - the same `version`/conflict handling as books
  - `DUPLICATE` shows "A member with this email already exists"
- `MemberDetailPage` (`/members/:id`):
  - details (code, joined date), Edit, and Deactivate through `ConfirmDialog` (hidden once inactive)
  - an "Inactive" badge
  - Loans section: placeholder until PR 2

### 5. Routes and docs
- `App.tsx`: add `/books`, `/books/:id`, `/members`, `/members/:id` under `AppLayout`. Home stays the placeholder until the PR 2 dashboard.
- Design doc: note the page behaviours (URL-held filters, conflict reload).

## Tests (Vitest + RTL, API modules mocked, co-located)
- `api/books.test.ts`, `api/members.test.ts`: URLs, query strings (empty filters dropped), methods, bodies. `toQuery` gets its own cases.
- Components:
  - `QueryState`: loading, error with Retry, empty, data
  - `DataTable`, `Pagination`: bounds
  - `SearchInput`: debounce with fake timers
  - `ConfirmDialog`: confirm, cancel, error shown
  - `AppLayout`: active nav link
- `BooksPage`:
  - loading, error with Retry, empty, rows
  - typing searches after the debounce and resets to page 1
  - filters are read from and written to the URL
  - paging
- `BookFormDialog`:
  - client validation blocks submit
  - create sends normalised values
  - edit sends `version`
  - `fieldErrors` are shown
  - **409 `CONCURRENT_UPDATE` → "Reload latest" refetches and resets the form**
- `BookDetailPage`: 404 state; delete confirm → navigates; 409 `HAS_LOAN_HISTORY` message shown
- `MembersPage`, `MemberFormDialog`, `MemberDetailPage`:
  - the same patterns
  - the status filter maps to `active`
  - deactivate hides the button and shows "Inactive"

## Delivery
- Branch `feature/frontend-catalogue` from `main`, with one Conventional Commit per step: libs + API modules, shared components, books, members, routes/docs.
- Then `/testing` (plus a manual run against the API), `/pr-review`, ask before `/raise-pr`, and watch both CI jobs.

## Verification
- `cd frontend && npm ci && npm run lint && npm test -- --run && npm run build` all pass. The backend is untouched; CI runs `mvnw verify`.
- Manual run with the API (`mvnw.cmd spring-boot:run`) and `npm run dev`:
  - add a book and see it in search (q, category, available) and on page 2
  - edit a book in two tabs: the second save shows the conflict, and Reload loads the new version
  - delete a book that was never borrowed
  - add a member with an email in mixed case; a duplicate email is refused; deactivate the member, then filter Inactive
- I have no browser tool, so this manual pass is API-level (curl through the Vite proxy) plus the component tests. Please click through once before merging.

---

## PR 2 outline (`feature/frontend-loans`, detailed after PR 1 merges)
- `api/loans.ts`: `searchLoans({status, memberId, bookId, page})`, `borrow`, `returnLoan`, `memberLoans`
- `LoansPage` (`/loans`): Active / Overdue / Returned tabs (`status` in the URL), a table with book, member, due date, a status badge, and a Return action
- `BorrowDialog`:
  1. pick a member (searchable, active only)
  2. pick a book (searchable, `available=true`)
  3. confirm
  - Each 409 (`NO_COPIES_AVAILABLE`, `LOAN_LIMIT_REACHED`, `MEMBER_HAS_OVERDUE`, `MEMBER_INACTIVE`, `CONCURRENT_UPDATE`) shows as plain language through `errorMessage()`.
- `MemberDetailPage`: active and past loans with a Return button. `BookDetailPage`: current loans.
- `DashboardPage` (`/`, replacing Home):
  - totals come from existing list endpoints with `size=1` → `totalElements`: books, active members, active loans, overdue
  - the overdue list is `status=overdue`, sorted by `dueDate` ascending
- Nav: add Loans. Tests follow the same patterns, including the borrow dialog's 409 messages.
