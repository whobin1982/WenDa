# Wenda

## 团队接入说明

本仓库已启用 **required gstack team mode**。

如果你要在这个仓库里进行 AI 辅助协作，请先完成 gstack 安装与 team 模式初始化；否则项目级 hook 会阻止相关 Skill 调用。

## 5 分钟接入流程

1. 安装 gstack

```bash
git clone --depth 1 https://github.com/garrytan/gstack.git ~/.claude/skills/gstack
cd ~/.claude/skills/gstack && ./setup --team
```

2. 重启你的 Claude / 终端会话

3. 回到本仓库开始工作

## 接入验证

完成安装后，你可以用以下方式快速自检：

- 确认目录存在：`~/.claude/skills/gstack/bin`
- 进入本仓库后，不应再被项目 hook 拒绝
- 需要网页浏览时，使用 `/browse`

## 必须知道的规则

- 所有网页浏览统一使用 gstack 的 `/browse`
- 不要使用 `mcp__claude-in-chrome__*` 工具
- 更详细的仓库级工作流规则、常用 skills 与 GitHub 协作方式，请查看 [CLAUDE.md](CLAUDE.md)

## 历史说明

本仓库早期有少量英文提交信息，例如：

- `docs: add gstack workflow and GitHub collaboration rules to CLAUDE.md`

该提交的中文含义是：**在 `CLAUDE.md` 中补充 gstack 工作流与 GitHub 协作规则**。

后续仓库已逐步统一为中文表达，包括：

- 提交信息尽量使用中文
- PR 标题与描述尽量使用中文
- 仓库内文档与协作说明优先使用中文

说明：这里是对历史提交含义的中文整理，**不会改写已经推送的 Git 历史**。

## 故障排查

如果你在仓库内使用 Skill 时被阻止，请优先检查：

1. `~/.claude/skills/gstack/bin` 是否存在
2. 是否已经执行过：

```bash
cd ~/.claude/skills/gstack && ./setup --team
```

3. 是否已经重启 Claude / 终端会话

项目级强制检查位于：

- [.claude/settings.json](.claude/settings.json)
- [.claude/hooks/check-gstack.sh](.claude/hooks/check-gstack.sh)

## 下一步如何进入规范流程

根据当前仓库规则，后续新任务建议这样开始：

1. **需求还不清楚**
   - 先从 `/office-hours` 或 `/spec` 开始
2. **想先形成可追踪的任务单 / GitHub Issue**
   - 优先使用 `/spec --no-execute`
3. **规格已经比较清楚，准备进入实现前评审**
   - 使用 `/autoplan`
4. **实现完成后**
   - 使用 `/review`
5. **分支准备好后**
   - 使用 `/ship`

如果你现在就有一个明确的新需求，通常最合适的入口是：

- **需求模糊**：先 `/spec`
- **方案已清楚、准备实现**：先 `/autoplan`

## 参考文件

- [CLAUDE.md](CLAUDE.md)
- [.claude/settings.json](.claude/settings.json)
- [.claude/hooks/check-gstack.sh](.claude/hooks/check-gstack.sh)
