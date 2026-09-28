---
name: raise-pr
description: Use when asked to raise, open, or create a pull request, or when a task is finished and ready to ship in this project. Ensures a feature branch, runs tests, commits with Conventional Commits, pushes, and opens a PR against main with the standard template.
---

# Raise PR

## 1. Preconditions

```bash
git branch --show-current
git status --porcelain
gh auth status
```

- On `main`? Create a branch from the change: `feature/…`, `fix/…`, or `chore/…` (`git checkout -b <name>`). Never commit to `main`.
- `gh` not authenticated? Stop and ask the user to run `! gh auth login`.
- Check for an existing PR: `gh pr view --json url` — if one exists, push updates and return its URL instead of creating another.

## 2. Verify

Run the `testing` skill. If anything fails, stop and report — do not open a PR on a red build.

Optionally run `pr-review` and fix Critical/Important findings first.

## 3. Commit

Review what will be committed (`git status`, `git diff`). Stage specific files — never commit `.env`, credentials, `target/`, `node_modules/`, or `dist/`.

Commit message: Conventional Commits, imperative, ≤72-char subject:

```
feat(loans): prevent borrowing unavailable copies

<optional body explaining why>
```

Add the co-author attribution line if the session provides one.

## 4. Push and open PR

Confirm with the user before pushing (outward-facing). Then:

```bash
git push -u origin <branch>
gh pr create --base main --title "<conventional title>" --body-file <scratchpad>/pr-body.md
```

PR body (mirrors `.github/pull_request_template.md`):

```markdown
## Summary
<1–3 sentences: what and why>

## Changes
- <bullet per meaningful change>

## Testing
- Backend: `./mvnw verify` → <result>
- Frontend: `npm run lint && npm test -- --run && npm run build` → <result>

## Screenshots
<UI changes only; otherwise "N/A">

## Checklist
- [ ] Tests added/updated
- [ ] No secrets or credentials committed
- [ ] Follows CLAUDE.md conventions
```

Append the PR attribution line if the session provides one. Testing results must be real output from step 2.

## 5. Report

Return the PR URL and a one-line summary.
