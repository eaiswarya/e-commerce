# Frontend Loans Implementation Plan (frontend pages, PR 2 of 2)

## Context
PR #9 shipped the Books and Members screens and the shared components. This PR finishes the design's frontend:
- lending and returning books
- loan lists on the book and member pages
- the dashboard

After it, a librarian can run the desk: lend a book to a member, take it back, see what's overdue, and get an overview on the home page.

## Reuse from `main`
- API layer:
  - `apiFetch`/`toQuery` (`api/client.ts`)
  - `errorMessage()` (`api/errors.ts`), which already has plain text for `NO_COPIES_AVAILABLE`, `LOAN_LIMIT_REACHED`, `MEMBER_HAS_OVERDUE`, `MEMBER_INACTIVE`, `ALREADY_RETURNED` and `CONCURRENT_UPDATE`
  - `queryKeys`
  - `searchBooks`/`searchMembers`
- Components: `QueryState`, `DataTable`, `Pagination`, `SearchInput`, `Modal`, `Badge`, `EmptyState`, `ErrorBanner`
- `useListParams` (URL-held filters and page), and the `renderWithProviders` test helper
- API:
  - `GET /api/loans?status=active|overdue|returned&memberId&bookId&page&sort` (default `borrowedAt,desc`; `active` includes overdue)
  - `POST /api/loans {bookId, memberId}`, `POST /api/loans/{id}/return`
  - `GET /api/members/{id}/loans?status=`
  - each loan's `status` is `ACTIVE | OVERDUE | RETURNED`

## Changes
1. **API and types:**
   - `Loan` and `LoanStatus` types
   - `api/loans.ts`: `searchLoans({status, memberId, bookId, page, size, sort})`, `memberLoans`, `borrowBook`, `returnLoan`
   - a `size` option on the book and member searches, so the dashboard can ask for counts cheaply
   - `queryKeys` for loans: everything under `['loans']`, so a borrow or return can invalidate all of it, plus `['books']` (copy counts change)
2. **`utils/dates.ts`:**
   - `formatDate('2026-10-13')` builds the date from its parts, so a due date never shifts by a day in time zones behind UTC
   - `formatDateTime(instant)`
3. **Shared loan UI: `components/loans/`:**
   - `LoanStatusBadge`: Active / Overdue / Returned
   - `useReturnLoan()`: mutation that invalidates loans and books
   - `LoansTable`: book and member links, borrowed and due dates, status, and a **Return** button on loans not yet returned. Columns can hide the book or the member, for use on their detail pages.
   - Return is a single click, because it's the most frequent desk action and doesn't destroy anything. A failure (e.g. `ALREADY_RETURNED`) shows above the table.
4. **`BorrowDialog`:**
   - two searchable pickers (radio lists): an **active** member, then a book **with a free copy**
   - a summary of what's selected, and **Lend book**
   - it can open with the member or the book already chosen (from their detail pages)
   - a refused borrow shows `errorMessage()` in plain language
   - on success it closes and invalidates
5. **`LoansPage` (`/loans`):**
   - Active / Overdue / Returned / All filter buttons (`aria-pressed`, kept in the URL with the page)
   - `LoansTable`, paging, and **Lend a book**
6. **Detail pages:**
   - Member: "On loan" (active) and "History" (returned, paged) tables, plus **Lend a book** while the member is active
   - Book: "On loan now" table, plus **Lend this book** while copies are available
7. **`DashboardPage` (`/`):**
   - four totals (books, active members, books on loan, overdue), each from an existing list endpoint with `size=1` → `totalElements`. Each tile handles its own loading and error.
   - the overdue list (`status=overdue`, `sort=dueDate,asc`) with Return
   - replaces `HomePage`; the nav gets **Loans**
8. **Docs:** the design doc's frontend notes.

## Tests (Vitest + RTL, API modules mocked)
- `api/loans.test.ts`, `utils/dates.test.ts`
- `LoansTable`: links, dates, badges, Return only for unreturned loans, a Return error shown
- `BorrowDialog`:
  - pickers search active members and available books
  - lending sends both ids
  - preselection
  - each 409 code shows its plain-language message
  - the button stays disabled until both are chosen
- `LoansPage`: filter in the URL → `status`, the default is active, empty, error with Retry, paging
- Detail pages: their loan sections and Lend buttons; Return refreshes
- `DashboardPage`: totals, the overdue list, empty overdue, a tile error that doesn't break the others
- `App`/`AppLayout`: `/loans` route and the Loans nav link

## Verification
- In `frontend/`: `npm run lint && npm test -- --run && npm run build`
- Manual check through the Vite proxy against the API:
  - borrow → book availability drops → `status=active`/`overdue` lists → return → availability restored
  - each refusal code (no copies, limit, inactive)
  - the dashboard counts
- Both CI jobs pass on the PR.
