# gstack

## gstack

- 所有网页浏览一律使用 gstack 提供的 `/browse` skill。
- 永远不要使用 `mcp__claude-in-chrome__*` 工具。
- 可用 skills：
  - `/office-hours`
  - `/plan-ceo-review`
  - `/plan-eng-review`
  - `/plan-design-review`
  - `/design-consultation`
  - `/design-shotgun`
  - `/design-html`
  - `/review`
  - `/ship`
  - `/land-and-deploy`
  - `/canary`
  - `/benchmark`
  - `/browse`
  - `/connect-chrome`
  - `/qa`
  - `/qa-only`
  - `/design-review`
  - `/setup-browser-cookies`
  - `/setup-deploy`
  - `/setup-gbrain`
  - `/retro`
  - `/investigate`
  - `/document-release`
  - `/document-generate`
  - `/codex`
  - `/cso`
  - `/autoplan`
  - `/plan-devex-review`
  - `/devex-review`
  - `/careful`
  - `/freeze`
  - `/guard`
  - `/unfreeze`
  - `/gstack-upgrade`
  - `/learn`

For engineering work in this repository, use the gstack workflow.

Default workflow:
1. For vague product or feature requests, start with /office-hours or /spec.
2. For GitHub issue-first work, use /spec. Prefer /spec --no-execute when I only want a ticket; use /spec --execute only when I explicitly ask you to create the issue and start implementation.
3. Before coding non-trivial features, run /autoplan or the relevant plan reviews.
4. Implement only after the plan is approved or after /autoplan reaches its final approval gate.
5. After implementation, run /review.
6. For web or UI changes, run /qa with the local/staging URL, or use diff-aware /qa if no URL is available.
7. When the branch is ready, run /ship to test, review, commit, push, and create or update the GitHub PR.
8. Do not merge to main unless I explicitly ask for /land-and-deploy.
9. Do not force push.
10. Stop and ask me before destructive data changes, security-sensitive changes, irreversible operations, unclear product tradeoffs, major scope changes, or any failing tests you cannot fix safely.

GitHub behavior:
- Use GitHub Issues for tracked specs.
- Use feature branches, not direct commits to the base branch.
- Use PRs for review.
- Include test evidence and known limitations in the PR body.