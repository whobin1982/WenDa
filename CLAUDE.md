# Wenda Agent Instructions

## gstack 工作流

- 本仓库要求使用 gstack team mode；若本地未完成接入，项目 hook 会阻止相关 Skill 调用。
- 所有网页浏览一律使用 gstack 提供的 `/browse` skill。
- 永远不要使用 `mcp__claude-in-chrome__*` 工具。
- 常用 skills：
  - `/office-hours`
  - `/spec`
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

如需面向团队成员的接入说明、安装步骤与故障排查，请先阅读 [README.md](README.md)。

在这个仓库中进行工程工作时，请使用 gstack 工作流。

默认工作流：
1. 如果需求比较模糊，先从 `/office-hours` 或 `/spec` 开始。
2. 如果是先建 GitHub Issue 的工作方式，使用 `/spec`。当我只想要 ticket 时，优先使用 `/spec --no-execute`；只有在我明确要求你创建 issue 并开始实现时，才使用 `/spec --execute`。
3. 在编写非 trivial 功能之前，先运行 `/autoplan` 或相应的 plan review。
4. 只有在计划获批之后，或 `/autoplan` 走到最终批准节点之后，才开始实现。
5. 实现完成后，运行 `/review`。
6. 如果是 web 或 UI 变更，使用本地 / 预发地址运行 `/qa`；如果没有可用 URL，则使用可感知 diff 的 `/qa`。
7. 当分支准备就绪后，运行 `/ship` 来完成测试、review、commit、push，以及创建或更新 GitHub PR。
8. 除非我明确要求 `/land-and-deploy`，否则不要合并到 `main`。
9. 不要 force push。
10. 如果涉及破坏性数据变更、安全敏感变更、不可逆操作、不明确的产品取舍、重大范围变化，或你无法安全修复的失败测试，请先停下来问我。

GitHub 协作规则：
- 对需要追踪的规格说明，使用 GitHub Issues。
- 使用 feature branch，不要直接提交到 base branch。
- 通过 PR 进行评审。
- 在 PR 描述中包含测试证据和已知限制。
