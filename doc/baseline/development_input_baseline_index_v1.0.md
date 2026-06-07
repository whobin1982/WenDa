# 开发输入基线索引 v1.0

**项目：AI 原生本科专业建设与学生能力成长平台**  
**用途：作为自动开发、人工开发、代码生成、测试生成和验收执行的唯一输入基线索引。**  
**基线日期：2026-06-07**

---

## 1. 基线结论

本项目进入自动开发前，必须以 `/baseline` 目录中列明的文档为唯一权威输入。未经显式升级，不得混用旧版本文档、草稿文档、建议稿或 `/archive` 目录文件。

`/archive` 目录仅供人工追溯，不得作为自动开发、代码生成、测试生成、验收生成或 Claude Code 上下文输入。

---

## 2. 权威输入文档清单

| 序号 | 文档 | 版本 | 文件名 | 用途 | 优先级 |
|---|---|---|---|---|---|
| 1 | 产品需求说明书 | v0.3 | product_requirements_spec_v0.3.md | 需求范围、功能需求、非功能需求、非目标、开源治理上游约束 | P0 |
| 2 | 系统架构设计 | v0.3 | system_architecture_design_v0.3.md | 架构边界、模块、部署、存储、AI、权限、开源治理 ADR、Adapter 红线 | P0 |
| 3 | 接口文档 | v0.2 | api_contract_v0.2.md | API 契约、请求响应、全量错误码、权限、幂等、分页 | P0 |
| 4 | 详细技术方案 | v0.4 | detailed_technical_solution_v0.4.md | 模块实现、数据模型、前后端实现、DEV 任务、开源治理 | P0 |
| 5 | 权限判定矩阵 | v1.0 | permission_decision_matrix_v1.0.md | 权限判定、后端鉴权、前端菜单控制、权限测试依据 | P0 |
| 6 | 任务-接口精确映射表 | v1.0 | task_api_mapping_matrix_v1.0.md | DEV 任务与 162 个核心 API 的逐条映射 | P0 |
| 7 | 验收清单终版 | v1.2 | acceptance_checklist_final_v1.2.md | 最终完成标准、需求 / 接口 / 任务 / 开源治理验收 | P0 |
| 8 | 需求-验收精确追踪矩阵 | v1.0 | requirement_acceptance_trace_matrix_exact_v1.0.md | FR/NFR/ENG/DEV/API/RG 与验收项的机器可读精确追踪 | P0 |
| 9 | 需求-接口-任务精确追踪矩阵 | v1.0 | requirement_api_task_trace_matrix_exact_v1.0.md | 需求、API、DEV 任务的机器可读精确映射 | P0 |
| 10 | DEV-验收精确追踪矩阵 | v1.0 | dev_acceptance_trace_matrix_exact_v1.0.md | DEV 任务到验收项的机器可读精确追踪 | P0 |
| 11 | 权限测试精确追踪矩阵 | v1.0 | permission_test_trace_matrix_exact_v1.0.md | PERM 规则到 AC-RBAC / AC-API / API 的精确追踪 | P0 |
| 12 | 全量错误码字典 | v1.0 | api_error_code_dictionary_v1.0.md | 后端异常枚举、前端提示、测试断言的错误码定义 | P0 |
| 13 | API-错误码精确矩阵 | v1.0 | api_error_code_matrix_exact_v1.0.md | 错误码与 API 编号的机器可读精确绑定 | P0 |

---

## 3. 排除与归档规则

| 文件 / 类型 | 处理状态 | 规则 |
|---|---|---|
| 验收清单初版_v0.2.md | 已移入 `/archive` | 仅供人工追溯，不得作为自动开发输入。 |
| 旧版草稿、初版、历史版本 | 已移入 `/archive` | Claude Code 不得读取。 |
| 开源治理同步修订建议 v0.1 | 已并入主文档，不再作为独立输入 | 其内容已并入 PRD v0.3、架构 v0.3、技术方案 v0.4 和验收清单 v1.2。 |

---

## 4. 文档优先级规则

1. 需求范围冲突时，以《产品需求说明书 v0.3》为准。
2. 架构边界冲突时，以《系统架构设计 v0.3》为准。
3. 接口契约冲突时，以《接口文档 v0.2》为准。
4. 权限自然语言描述与权限矩阵冲突时，以《权限判定矩阵 v1.0》为准。
5. DEV 任务接口归属冲突时，以《任务-接口精确映射表 v1.0》为准。
6. 完成标准冲突时，以《验收清单终版 v1.2》为准。
7. 自动开发、测试生成、缺陷回溯和验收覆盖统计时，以机器可读精确追踪矩阵为准。
8. 技术实现细节冲突时，以《详细技术方案 v0.4》为准，但不得突破 PRD、架构、接口、权限和验收约束。

其他文档不得重复定义冲突优先级，只能引用本索引。

---

## 5. 已确认修复决策

| 决策编号 | 内容 |
|---|---|
| CONS-DEC-001 | 严格按本索引统一正文版本：PRD v0.3、架构 v0.3、技术方案 v0.4、验收终版 v1.2。 |
| CONS-DEC-002 | 正式新增 NFR-016，并进入需求、架构、技术方案、DEV 任务和验收追踪链。 |
| CONS-DEC-003 | 新增 ENG-OSG-001，承接开源优先、许可证、SBOM、SCA、漏洞扫描、供应商锁定和发布准入工程约束。 |
| CONS-DEC-004 | 统一 NFR 编号语义：部署归 NFR-013，端形态/兼容归 NFR-014，开源治理归 NFR-016 / ENG-OSG-001。 |
| CONS-DEC-005 | `PERM-*` 作为权限规则编号，不作为独立验收项；新增 `PERM → AC-RBAC / AC-API` 映射。 |
| CONS-DEC-006 | 新增全量错误码字典，统一后端异常、前端提示、接口自动化和验收断言。 |
| CONS-DEC-007 | 旧版文件移入 `/archive`，不得作为自动开发输入。 |
| CONS-DEC-008 | 《开源治理同步修订建议 v0.1》已并入主文档，不再作为独立输入。 |
| CONS-DEC-009 | 正文可保留人工可读范围表达，但自动开发必须使用机器可读精确追踪矩阵。 |
| CONS-DEC-010 | 所有已确认修复均改为正式基线要求，不再使用“建议 / 后续 / 可考虑 / 补丁”表述。 |

---

## 6. Claude Code 自动开发读取规则

1. 只读取 `/baseline` 目录。
2. 优先读取本索引，再读取 P0 文档。
3. 任务拆解以 `task_api_mapping_matrix_v1.0.md` 和精确追踪矩阵为准。
4. 权限实现以 `permission_decision_matrix_v1.0.md` 和 `permission_test_trace_matrix_exact_v1.0.md` 为准。
5. 错误处理以 `api_error_code_dictionary_v1.0.md` 和 `api_error_code_matrix_exact_v1.0.md` 为准。
6. 验收与测试生成以 `acceptance_checklist_final_v1.2.md` 和精确追踪矩阵为准。
7. 不得使用 `/archive` 文件覆盖或修正 `/baseline` 文件。
