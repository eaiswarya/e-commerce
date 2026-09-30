# Frontend Scaffold Implementation Plan

## Context
The backend is complete: auth, books, members and loans (PRs #3–#7). `main` is at `fa620d0`. The next item in `docs/plans/2026-09-28-library-management-design.md` is the frontend foundation, which doesn't exist yet.

This PR delivers:
- a React SPA that librarians can log into
- one API client that every later page uses
- redirects to `/login` when logged out or when the API returns 401
- the frontend CI job

The real pages (dashboard, books, members, loans, borrow dialog) are the next PR, `feature/frontend-pages`.

Outcome: `npm run dev` serves the app. You log in as `admin`/`admin123` against the running API, land on a protected home screen showing your name, and can log out. Lint, tests and build pass locally and in CI.

## Backend contract this relies on (already on `main`)
- `POST /api/auth/login` `{username, password}` returns `{token, expiresAt, fullName}`. Bad credentials give 401 `UNAUTHORIZED` with a message.
- `GET /api/auth/me` returns `{username, fullName}`.
- Errors: `{status, error, message, fieldErrors?, timestamp}` (`dto/ErrorResponse`). Codes are in the design's error table.
- Lists: `{content, page, size, totalElements, totalPages}` (`dto/PageResponse`).
- There's no CORS config, so the Vite dev server proxies `/api` to `http://localhost:8080`, as the design says.
- Local Node is v22.14 (the README requires Node 22+).

## Stack (per the design and your task list)
- React **18**, TypeScript, Vite, React Router (`react-router` v7, library mode) and TanStack Query v5
- CSS modules
- Vitest with jsdom, React Testing Library, `@testing-library/user-event` and `jest-dom`
- ESLint (flat config, `typescript-eslint`, `react-hooks`, `react-refresh`) and Prettier (via `eslint-config-prettier`)

## Changes

### 1. Project setup: `frontend/`
- Written by hand (not `create-vite`, whose template now targets React 19), with `react`/`react-dom`/`@types/react*` pinned to 18.
- `vite.config.ts`: `server.proxy['/api'] = 'http://localhost:8080'`, plus a Vitest `test` block (`environment: 'jsdom'`, `setupFiles: src/test/setup.ts`, `restoreMocks: true`).
- `package.json` scripts:
  - `dev`, `build` (`tsc -b && vite build`)
  - `lint` (`eslint . && prettier --check .`)
  - `format`, `test` (`vitest`)
- Commit `package-lock.json`. Add `.prettierrc`, and ignore `dist/`, `coverage/` and `node_modules/`.

### 2. Shared types: `src/types.ts`
`ErrorResponse`, `PageResponse<T>`, `LoginResponse`, `CurrentUser`. The book, member and loan types come with the pages PR.

### 3. API client: `src/api/client.ts`
Every HTTP call goes through here, per CLAUDE.md.
- `apiFetch<T>(path, { method, body, signal })`: sends JSON and adds `Authorization: Bearer <token>` when a token is set. A 204 returns `undefined`.
- An error response throws `ApiError { status, code, message, fieldErrors }`, parsed from `ErrorResponse`. A non-JSON error body falls back to the HTTP status text. A network failure gives `ApiError(0, 'NETWORK_ERROR', 'Cannot reach the server…')`.
- `setAuthToken(token | null)` and `onUnauthorized(handler)`. A 401 calls the handler **only if the request carried a token**. So a wrong password on login shows its message instead of redirecting, while an expired session sends the user to `/login`.
- `src/api/errors.ts`: `errorMessage(error)` turns each design error code into a plain-language sentence (`NO_COPIES_AVAILABLE` → "No copies of this book are available right now.", `CONCURRENT_UPDATE`, and so on). Unknown codes fall back to the server message. The borrow dialog and edit forms reuse it in the next PR.
- `src/api/auth.ts`: `login(username, password)`, `fetchCurrentUser()`.

### 4. Auth state: `src/auth/`
- `session.ts`: saves `{token, expiresAt, fullName, username}` to `sessionStorage` (design: memory + sessionStorage). It drops an expired or corrupt entry on load, and wraps every storage access in try/catch.
- `authContext.ts` (context + `useAuth()`) and `AuthProvider.tsx`, kept apart so React Fast Refresh works. `useAuth()` returns `{user, isAuthenticated, login, logout}`.
  - `login` calls the API, stores the session and sets the client token.
  - `logout` clears everything.
  - It registers `onUnauthorized` to call `logout()` and clear the TanStack Query cache.
- `ProtectedRoute.tsx`: a logged-out user goes to `<Navigate to="/login" replace state={{ from: location }} />`. After login they return to `from`.

### 5. UI shell and login page
- `src/main.tsx`: `QueryClientProvider` (`retry: 1` for queries, no retry on 4xx), `BrowserRouter`, `AuthProvider`.
- `src/App.tsx` routes:
  - `/login` → `LoginPage`
  - protected `AppLayout` with `/` → `HomePage`
  - `*` → a not-found message
- `src/components/`: `FormField` (label and input with `aria-invalid`/`aria-describedby` for errors) and `ErrorBanner` (`role="alert"`, optional Retry button). Both are reused by the pages PR.
- `src/pages/login/LoginPage.tsx`:
  - username and password fields, validated on the client (required fields)
  - the submit button is disabled with "Signing in…" while pending
  - errors show through `ErrorBanner` + `errorMessage`, and `fieldErrors` from a 400 map onto the fields
  - visiting `/login` while already logged in redirects to `/`
- `src/pages/home/HomePage.tsx`: a placeholder welcome with the librarian's name. `AppLayout` has a header with the app name, the user's name and a Log out button. The pages PR replaces this with the dashboard and nav links.
- `src/styles/global.css` holds a few CSS variables and a base layout. Components use CSS modules.

### 6. CI: `.github/workflows/ci.yml`
Add a `frontend` job next to `backend`:
- `working-directory: frontend`
- `actions/setup-node@v4` (Node 22, `cache: npm`, `cache-dependency-path: frontend/package-lock.json`)
- `npm ci`, `npm run lint`, `npm test -- --run`, `npm run build`

### 7. Docs
- README: a Frontend section with `npm ci` / `npm run dev`, a note that it runs on http://localhost:5173, and that the API must be running.
- Design doc: note the router and React versions, and the rule that a 401 redirects only for requests that carried a token.

## Tests (TDD, co-located `*.test.ts(x)`, API layer mocked for UI)
- `api/client.test.ts` (mocked `fetch`):
  - sends JSON and the bearer token; omits the header when there's no token
  - parses success, 204, `ErrorResponse` with `fieldErrors`, and non-JSON errors
  - a network error gives `NETWORK_ERROR`
  - a 401 with a token calls the handler; a 401 without a token doesn't
- `api/errors.test.ts`: known codes map to sentences, and unknown codes fall back to the server message
- `auth/session.test.ts`: saves and loads; an expired or corrupt entry is cleared
- `auth/ProtectedRoute.test.tsx`: logged out redirects to `/login`; logged in renders the child
- `auth/AuthContext.test.tsx`: login stores the session and token; an API 401 during a session logs out
- `pages/login/LoginPage.test.tsx` (mock `api/auth`):
  - empty submit shows field errors and makes no call
  - pending state disables the button
  - success navigates to the original page
  - a 401 shows the server message
  - a network error shows a friendly message
  - an already-logged-in user is redirected

## Delivery
- Branch `feature/frontend-scaffold` from `main`.
- One commit per step (Conventional Commits). Then run `/testing`, `/pr-review`, and ask before `/raise-pr`. Watch both CI jobs on the PR.

## Verification
- In `frontend/`: `npm ci && npm run lint && npm test -- --run && npm run build` all pass.
- Manual check with `mvnw.cmd spring-boot:run` in `backend/` and `npm run dev` in `frontend/`:
  - http://localhost:5173 redirects to `/login`
  - a wrong password shows the error
  - `admin`/`admin123` lands on the home screen with "Administrator"
  - a reload stays logged in (sessionStorage)
  - Log out returns to `/login`
  - corrupting the stored token in DevTools (tokens are stateless JWTs and can't be revoked server-side) sends the user to `/login` on the next API call
- CI: `backend` and `frontend` both pass on the PR.
