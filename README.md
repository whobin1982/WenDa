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
- 更多规则与可用 skills，请查看 [CLAUDE.md](CLAUDE.md)

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

## 参考文件

- [CLAUDE.md](CLAUDE.md)
- [.claude/settings.json](.claude/settings.json)
- [.claude/hooks/check-gstack.sh](.claude/hooks/check-gstack.sh)
