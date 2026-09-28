---
name: testing
description: Use when asked to run tests, write tests, or verify a change in this Spring Boot + React project, and before reviewing or raising a PR. Detects affected backend/frontend code, adds missing tests, runs the full check suite, and reports real results.
---

# Testing

Verify the current change with evidence. Never claim tests pass without having run them in this session.

## 1. Find what changed

```bash
git diff --name-only main...HEAD
git status --porcelain
```

Classify changed files:
- `backend/**` → backend affected
- `frontend/**` → frontend affected
- If a side's directory doesn't exist, report "no <side> yet — skipped" and continue.

## 2. Add missing tests

For each changed source file, check that its behaviour is covered. If not, write tests first:

| Code | Test style |
|------|-----------|
| Service (`*Service.java`) | JUnit 5 + Mockito unit test, mock repositories |
| Controller (`*Controller.java`) | `@WebMvcTest` + `MockMvc`, mock the service; cover 2xx, validation 400, 404, auth 401/403 |
| Repository (custom queries) | `@DataJpaTest` |
| React component / hook | Vitest + React Testing Library; mock `src/api/`; cover loading, error, empty, success |
| API client (`src/api/*`) | Vitest with mocked `fetch` |

Test names describe behaviour (`returnsNotFoundWhenBookMissing`, `shows error when loans fail to load`).

## 3. Run the checks

Backend (from `backend/`; Windows: `mvnw.cmd`):

```bash
./mvnw verify
```

Frontend (from `frontend/`):

```bash
npm ci          # only if node_modules missing or package-lock changed
npm run lint
npm test -- --run
npm run build
```

Run only the affected side(s) unless asked for everything.

## 4. On failure

1. Read the actual failure output; find the root cause.
2. Fix the code (or the test if the test is genuinely wrong — explain why).
3. Re-run the failing command, then the full suite for that side.

Never `@Disabled`, `.skip`, delete assertions, or loosen checks to get green.

## 5. Report

```
Testing summary
- Backend:  ./mvnw verify → PASS (Tests: 42, Failures: 0, Errors: 0, Skipped: 0)
- Frontend: lint PASS · vitest PASS (18 passed) · build PASS
- Tests added: BookServiceTest (3), BookList.test.tsx (4)
```

If anything fails or was skipped, say so plainly with the relevant output.
