---
name: pr-review
description: Use when asked to review a pull request, review changes, or self-review before raising a PR in this Spring Boot + React project. Accepts an optional PR number or branch (default is current branch vs main) and an optional --comment flag to post the review on GitHub.
---

# PR Review

Review a diff for correctness, security, and adherence to `CLAUDE.md`. Report only real issues, each with a location and a concrete fix.

## 1. Get the diff

| Input | Command |
|-------|---------|
| none | `git diff main...HEAD` (plus `git diff` for uncommitted work) |
| PR number `N` | `gh pr view N` and `gh pr diff N` |
| branch `B` | `git diff main...B` |

Read the full changed files where context is needed — don't judge from hunks alone.

## 2. Checklist

**Correctness**
- Logic errors, off-by-one, null handling, edge cases (empty lists, missing IDs, duplicates)
- Library rules: can't loan an unavailable copy, can't exceed member loan limit, return updates availability, due dates/fines computed correctly

**Spring Boot**
- Entities never returned from controllers — DTOs only
- `@Valid` on request bodies; constraints on DTO fields
- Exceptions mapped via `@RestControllerAdvice`; correct HTTP status codes
- `@Transactional` on writing service methods; no lazy-loading outside transactions
- N+1 queries (use fetch joins / `@EntityGraph` where lists load relations)
- Endpoints protected appropriately (roles for admin actions); no auth bypass
- No string-concatenated JPQL/SQL; no secrets or hardcoded URLs/credentials

**React**
- Hook dependency arrays correct; no state updates after unmount
- Stable `key`s on lists (not array index for mutable lists)
- Loading / error / empty states handled
- HTTP only via `src/api/`
- No `dangerouslySetInnerHTML` with user data; tokens not stored insecurely
- Accessibility: labels on inputs, buttons are `<button>`, alt text

**Tests & hygiene**
- New behaviour has tests; tests assert behaviour, not implementation
- No dead code, debug logs, commented-out blocks, or unrelated changes
- Naming and structure consistent with `CLAUDE.md`

## 3. Output

```
## PR Review: <branch or #N>

### Critical (must fix)
- `backend/src/.../LoanService.java:48` — Loan created without checking copy availability; two members can borrow the same copy. Fix: check `copy.isAvailable()` and throw `CopyUnavailableException`.

### Important (should fix)
- ...

### Minor (optional)
- ...

**Verdict:** Request changes | Approve
```

Omit empty sections. If nothing survives scrutiny, say "No issues found" and approve.

## 4. Posting (only with `--comment` and a PR number)

Ask the user before posting. Then:

```bash
gh pr review N --request-changes --body-file review.md   # if Critical/Important exist
gh pr review N --comment --body-file review.md           # otherwise
```

Write `review.md` to the scratchpad, not the repo.
