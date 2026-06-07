# 权限判定矩阵 v1.0

**适用项目：AI 原生本科专业建设与学生能力成长平台**  
**生成目的：作为接口鉴权、后端权限实现、前端菜单控制、权限自动化测试和验收的共同依据。**

---

## 1. 使用原则

1. 权限模型采用 `RBAC + Scope + ABAC`。
2. RBAC 判断用户角色是否具备某类操作资格。
3. Scope 判断用户是否处于正确学校、学院、专业、课程、学生或企业范围。
4. ABAC 判断用户与具体资源之间是否存在授权关系，例如授课关系、导师关系、学生本人关系、企业授权关系、能力地图授权关系。
5. 若本文档与接口文档中的自然语言权限描述不一致，以本文档为准，并同步修订接口文档。
6. 权限校验必须在后端执行，前端菜单控制仅用于体验优化，不作为安全边界。
7. 所有 P0 权限规则均必须具备正向和反向自动化测试。
8. `PERM-*` 是权限规则编号，不作为独立验收项编号；权限验收以 `AC-RBAC-*` 角色级验收和 `AC-API-*` 接口级验收为准。
9. 每条 `PERM-*` 规则必须通过第 5 章 `PERM → AC-RBAC / AC-API` 映射表追溯到角色级和接口级验收。

---

## 2. 角色代码

| 角色代码 | 角色名称 | 权限定位 |
|---|---|---|
| SYSTEM_ADMIN | 系统管理员 | 平台级初始化、系统级配置、跨学校运维，但无教育内容发布权。 |
| SCHOOL_ADMIN | 学校管理员 | 学校空间内用户、学院、配置、AI 策略、审计与发布准入管理。 |
| COLLEGE_MANAGER | 学院管理者 | 授权学院范围内专业建设状态、专业管理和报告查看。 |
| MAJOR_OWNER | 专业负责人 | 本专业专业建设、OBE、课程体系、支撑矩阵、质量分析和正式版本发布。 |
| ACADEMIC_ADMIN | 教务人员 | 培养方案、课程计划、课程体系统计、报告导出和版本辅助管理。 |
| TEACHER | 任课教师 | 所授课程、课程内容、课程知识库、关联学生证据审核。 |
| MENTOR | 导师 / 班主任 | 所指导学生画像、成长建议、预警、能力地图审核意见。 |
| STUDENT | 学生 | 本人画像、学习证据、成长任务、能力地图、对外授权。 |
| EMPLOYER_MENTOR | 企业导师 / 用人单位 | 授权企业范围内岗位能力、实习评价、就业反馈和授权能力地图访问。 |
| KNOWLEDGE_ADMIN | 知识库管理员 | 课程知识库资料审核、标注、可信度、版权许可和禁用。 |

---

## 3. 权限判定矩阵

| 权限规则编号 | 角色 | 资源类型 | 动作 | 允许条件 | 禁止条件 | 接口范围 | 关联需求 | 关联验收项 | 优先级 |
|---|---|---|---|---|---|---|---|---|---|
| PERM-SYS-001 | SYSTEM_ADMIN | 学校空间 | 创建 / 初始化 | 用户为平台系统管理员；操作对象为新学校空间。 | 管理教育内容发布；访问学生隐私详情；绕过学校授权边界。 | API-ORG-001 | FR-001, NFR-013 | 见第5章映射表 | P0 |
| PERM-SYS-002 | SYSTEM_ADMIN | 系统配置 / 开源治理 | 查看 / 管理 | 用户为系统管理员；操作为平台级配置、依赖治理、系统健康检查。 | 直接查看学生证据正文、画像详情、能力地图未授权内容。 | 运维接口、发布准入记录 | NFR-016, ENG-OSG-001 | AC-OSG-001～AC-OSG-014 | P0 |
| PERM-SCHOOL-001 | SCHOOL_ADMIN | 学校内组织与用户 | 创建 / 更新 / 停用 | 用户属于当前学校；具备 SCHOOL_ADMIN；资源 school_id 与当前学校一致。 | 操作其他学校空间；删除审计日志；修改已发布教育内容绕过审核。 | API-ORG-*、API-USER-*、API-ROLE-001 | FR-001, FR-002 | 见第5章映射表 | P0 |
| PERM-SCHOOL-002 | SCHOOL_ADMIN | 学校配置 | 查看 / 更新 | 用户属于当前学校；具备 SCHOOL_ADMIN；配置项属于学校级设置。 | 启用外部 AI Provider 无审批；允许学生数据出域无审批；绕过 NFR-016。 | API-CFG-* | FR-031, NFR-010, NFR-016 | 见第5章映射表 | P0 |
| PERM-SCHOOL-003 | SCHOOL_ADMIN | 审计日志 | 查询 | 仅查询本学校空间内审计日志；返回脱敏摘要。 | 物理删除审计日志；通过审计日志查看敏感原文；查询其他学校日志。 | API-AUD-001 | FR-002, FR-032, NFR-012 | 见第5章映射表 | P0 |
| PERM-COLLEGE-001 | COLLEGE_MANAGER | 学院与专业 | 查看 / 管理 | 用户被授权到 collegeId；资源属于该学院；操作在学院管理范围内。 | 查看或管理其他学院专业；修改学校级配置；查看学生隐私详情。 | API-DSH-002、API-MAJ-* | FR-003, FR-004 | 见第5章映射表 | P0 |
| PERM-COLLEGE-002 | COLLEGE_MANAGER | 用人单位邀请 | 创建 / 审核 | 企业合作关系绑定本学院；用户具备学院管理者角色。 | 审核其他学院企业；访问未授权学生能力地图。 | API-EMP-001、API-EMP-004 | FR-028 | 见第5章映射表 | P0 |
| PERM-MAJOR-001 | MAJOR_OWNER | 专业建设数据 | 查看 / 编辑 / AI 草案 / 发布 | 用户被绑定为该专业负责人；资源属于该专业；发布必须经过人工审核状态。 | 管理其他专业；绕过审核发布；覆盖历史版本；访问非授权学生隐私。 | API-MAJ-*、API-OBE-*、API-CUR-*、API-SUP-*、API-QA-* | FR-003～FR-012, FR-030 | 见第5章映射表 | P0 |
| PERM-MAJOR-002 | MAJOR_OWNER | 课程体系 / 支撑矩阵 | 编辑 / 发布 | 用户为该专业负责人；课程体系属于本专业；If-Match 版本校验通过。 | 直接修改已发布版本；删除历史版本；发布含临时代码且学校策略禁止。 | API-CUR-*、API-SUP-* | FR-008～FR-011 | 见第5章映射表 | P0 |
| PERM-MAJOR-003 | MAJOR_OWNER | 学生汇总数据 | 查看汇总 | 学生属于本专业；仅查看汇总或授权范围内详情。 | 查看学生画像敏感详情、成长预警详情、申诉原文，除非具备明确授权。 | API-STU-*、API-PRF-*、API-WRN-* | FR-018～FR-024, NFR-010 | 见第5章映射表 | P0 |
| PERM-ACAD-001 | ACADEMIC_ADMIN | 培养方案 / 课程计划 | 查看 / 辅助维护 / 导出 | 用户属于当前学校 / 授权学院；资源属于授权范围。 | 绕过专业负责人发布正式教育版本；访问学生隐私详情；修改 AI 安全策略。 | API-MAJ-*、API-CUR-*、API-RPT-* | FR-003, FR-008, FR-013 | 见第5章映射表 | P1 |
| PERM-TEACHER-001 | TEACHER | 所授课程 | 查看 / 编辑课程内容 | 用户为该课程任课教师或课程负责人；课程属于当前学校授权范围。 | 编辑非所授课程；发布未经审核内容；引用未审核知识库资料。 | API-CRS-*、API-CC-* | FR-014, FR-017 | 见第5章映射表 | P0 |
| PERM-TEACHER-002 | TEACHER | 学习证据 | 查看 / 审核 / 驳回 / 要求补充 | 学习证据关联该教师所授课程、作业、实验或项目；学生属于当前学校。 | 查看非所授课程证据；查看学生画像详情；直接修改能力等级最终结论。 | API-EVD-* | FR-021, FR-022 | 见第5章映射表 | P0 |
| PERM-TEACHER-003 | TEACHER | 课程知识库 | 上传 / 查看 / 发起生成 | 资料关联该教师课程或授权课程；资料通过文件安全扫描后才可解析。 | 使用未审核或禁用资料发布正式课程内容；访问其他课程未授权资料。 | API-FILE-*、API-KB-*、API-CC-* | FR-015～FR-017 | 见第5章映射表 | P0 |
| PERM-MENTOR-001 | MENTOR | 指导学生画像 | 查看 / 辅导 / 处理申诉 | 用户为该学生导师 / 班主任；学生属于当前学校。 | 查看非指导学生画像；向企业展示画像；访问敏感黑名单字段。 | API-PRF-*、API-APL-*、API-PATH-* | FR-018～FR-020 | 见第5章映射表 | P0 |
| PERM-MENTOR-002 | MENTOR | 成长预警 | 查看 / 处理 | 用户为该学生导师；预警可见范围包含导师；仅用于辅导。 | 公开展示、排名、惩罚使用；向用人单位展示；查看非指导学生预警。 | API-WRN-*、API-TODO-001 | FR-024, NFR-010 | 见第5章映射表 | P0 |
| PERM-MENTOR-003 | MENTOR | 能力地图 | 审核 / 形成意见 | 用户为该学生导师；能力地图处于导师审核状态。 | 代替学生进行对外授权；导出对外版本绕过学生授权。 | API-MAP-* | FR-026, FR-027 | 见第5章映射表 | P0 |
| PERM-STUDENT-001 | STUDENT | 本人数据 | 查看 / 补充 / 申诉 | 用户为学生本人；资源 student_id 与当前用户绑定。 | 查看他人学生数据；修改教师评价；修改正式能力等级。 | API-STU-*、API-PRF-*、API-APL-* | FR-018～FR-020 | 见第5章映射表 | P0 |
| PERM-STUDENT-002 | STUDENT | 学习证据 | 上传 / 查看 / 补充 | 学生本人提交；附件符合文件限制并通过安全扫描；证据未归档。 | 审核自己的证据；将未审核证据计入能力；访问他人证据。 | API-EVD-*、API-FILE-* | FR-021, FR-022 | 见第5章映射表 | P0 |
| PERM-STUDENT-003 | STUDENT | 能力地图授权 | 创建 / 撤销 / 查看访问日志 | 学生本人；能力地图已审核或符合授权条件；displayScopes 合法。 | 授权成长预警、学生画像详情、未审核证据、申诉记录、敏感数据。 | API-AUTHZ-*、API-PUB-002 | FR-026, FR-027 | 见第5章映射表 | P0 |
| PERM-EMP-001 | EMPLOYER_MENTOR | 企业账号 | 补充 / 查看 / 有限更新 | 企业账号由邀请制创建；已通过学校 / 学院审核；资源属于绑定企业。 | 自主注册绕过审核；查看其他企业数据；查看学生隐私。 | API-EMP-* | FR-028 | 见第5章映射表 | P0 |
| PERM-EMP-002 | EMPLOYER_MENTOR | 授权能力地图 | 查看 / 下载授权 PDF | 企业已审核；存在学生授权；授权未过期 / 未撤销；请求内容在 displayScopes 内。 | 查看学生画像详情、成长预警、申诉记录、未审核证据、授权范围外章节。 | API-PUB-001、API-RPT-005 | FR-027, FR-028 | 见第5章映射表 | P0 |
| PERM-EMP-003 | EMPLOYER_MENTOR | 岗位能力 / 反馈 | 创建 / 提交 | 企业账号已审核；岗位或反馈属于该企业或授权专业合作范围。 | 发布学生能力结论；修改课程体系；查看未授权学生数据。 | API-JOB-*、API-EVL-001、API-FDB-* | FR-028, FR-029 | 见第5章映射表 | P0 |
| PERM-KB-001 | KNOWLEDGE_ADMIN | 课程知识库资料 | 审核 / 标注 / 禁用 / 归档 | 用户具备知识库管理员角色；资料属于当前学校授权范围。 | 访问其他学校资料；绕过文件安全扫描；物理删除资料和引用记录。 | API-KB-*、API-FILE-* | FR-015, FR-016, NFR-011 | 见第5章映射表 | P0 |
| PERM-KB-002 | KNOWLEDGE_ADMIN | 版权许可状态 | 维护 | 资料属于当前学校；更新版权、许可、使用范围、可信度需审计。 | 将禁止使用资料标记为可正式引用而无审核记录；删除历史审核。 | API-KB-004～005 | FR-016, NFR-011 | 见第5章映射表 | P0 |
| PERM-AI-001 | SCHOOL_ADMIN / MAJOR_OWNER / TEACHER | AI 任务 | 创建 / 查看 | 学校 AI 策略允许；外部 Provider 已启用且审批存在；任务资源在用户授权范围内。 | 学生数据默认进入外部模型；Provider 未启用；绕过草案区；发布 AI 结果为正式数据。 | API-AI-*、API-CFG-* | FR-005～FR-017, FR-030, NFR-010 | 见第5章映射表 | P0 |
| PERM-FILE-001 | 授权用户 | 文件 | 上传 / 预览 / 下载 | 文件业务对象在用户授权范围内；安全扫描通过；签名 URL 未过期；学生授权有效。 | 扫描未通过文件访问；永久公开 URL；越权下载；授权过期或撤销后访问。 | API-FILE-*、API-RPT-005 | FR-015, FR-021, FR-027, NFR-012 | 见第5章映射表 | P0 |
| PERM-RPT-001 | 授权用户 | 报告 | 生成 / 下载 | 用户对业务对象有导出权限；数据版本已发布或授权；报告任务成功。 | 导出未授权学生数据；历史报告自动重算；下载授权撤销后的对外报告。 | API-RPT-*、API-MAP-006 | FR-013, FR-026, FR-027, FR-030 | 见第5章映射表 | P0 |
| PERM-ARC-001 | 资源管理员 | 归档与恢复 | 归档 / 恢复 | 用户对资源有归档 / 恢复权限；资源支持归档；操作有理由和审计。 | 物理删除已发布版本、审核记录、AI 运行记录、审计日志。 | API-ARC-* | FR-032 | 见第5章映射表 | P0 |
| PERM-NOT-001 | 当前用户 | 通知 / 待办 | 查看 / 标记已读 / 处理 | 通知接收人为当前用户；待办分配给当前用户或角色范围。 | 查看他人通知；通过待办访问无权限业务对象。 | API-NOT-*、API-TODO-001 | FR-004, FR-012, NFR-009 | 见第5章映射表 | P1 |
| PERM-OSG-001 | 安全 / 开源治理负责人 | 开源治理记录 | 查看 / 审批 / 阻断发布 | 用户具备开源治理职责；操作对象为依赖、SBOM、许可证、漏洞、例外审批。 | 业务模块绕过 Adapter；未审批高风险许可证进入生产；生产启用 Mock。 | 发布准入 / AC-OSG | NFR-016, ENG-OSG-001 | AC-OSG-001～AC-OSG-014 | P0 |

---

## 4. 权限测试规则

1. 每条 P0 权限规则必须至少包含 1 条正向测试和 1 条反向测试。
2. 反向测试必须覆盖 IDOR 场景，即修改 path / query 中的 schoolId、collegeId、majorId、studentId、employerId、abilityMapId 等资源 ID。
3. 学生数据、能力地图、学习证据、成长预警、审计日志、文件下载、AI 外部调用为重点越权测试对象。
4. 权限失败应返回 `FORBIDDEN` 或对应业务错误码，不得返回敏感资源存在性细节。
5. 所有越权访问均应记录安全审计日志或安全事件日志。


---

## 5. PERM → AC-RBAC / AC-API 映射表

本表为权限矩阵与验收清单之间的正式追溯桥梁。`PERM-*` 只表示权限规则，不再作为独立验收项编号。自动开发、后端鉴权实现、前端菜单控制和权限自动化测试必须以本表为准。机器可读精确版本见 `permission_test_trace_matrix_exact_v1.0.md`。

| 权限规则编号 | 角色级验收项 | 接口级 / 发布准入验收项 |
|---|---|---|
| PERM-SYS-001 | AC-RBAC-001-P, AC-RBAC-001-N | AC-API-ORG-001 |
| PERM-SYS-002 | AC-RBAC-001-P, AC-RBAC-001-N | AC-OSG-001, AC-OSG-002, AC-OSG-003, AC-OSG-004, AC-OSG-005, AC-OSG-006, AC-OSG-007, AC-OSG-008, AC-OSG-009, AC-OSG-010, AC-OSG-011, AC-OSG-012, AC-OSG-013, AC-OSG-014 |
| PERM-SCHOOL-001 | AC-RBAC-002-P, AC-RBAC-002-N | AC-API-ORG-001, AC-API-ORG-002, AC-API-ORG-003, AC-API-ORG-004, AC-API-ORG-005, AC-API-ORG-006, AC-API-ROLE-001, AC-API-USER-001, AC-API-USER-002, AC-API-USER-003, AC-API-USER-004, AC-API-USER-005, AC-API-USER-006 |
| PERM-SCHOOL-002 | AC-RBAC-002-P, AC-RBAC-002-N | AC-API-CFG-001, AC-API-CFG-002, AC-API-CFG-003, AC-API-CFG-004, AC-API-CFG-005, AC-API-CFG-006, AC-API-CFG-007, AC-API-CFG-008, AC-API-CFG-009, AC-API-CFG-010, AC-API-CFG-011, AC-API-CFG-012 |
| PERM-SCHOOL-003 | AC-RBAC-002-P, AC-RBAC-002-N | AC-API-AUD-001 |
| PERM-COLLEGE-001 | AC-RBAC-003-P, AC-RBAC-003-N | AC-API-DSH-002, AC-API-MAJ-001, AC-API-MAJ-002, AC-API-MAJ-003, AC-API-MAJ-004, AC-API-MAJ-005, AC-API-MAJ-006, AC-API-MAJ-007, AC-API-MAJ-008 |
| PERM-COLLEGE-002 | AC-RBAC-003-P, AC-RBAC-003-N | AC-API-EMP-001, AC-API-EMP-004 |
| PERM-MAJOR-001 | AC-RBAC-004-P, AC-RBAC-004-N | AC-API-CUR-001, AC-API-CUR-002, AC-API-CUR-003, AC-API-CUR-004, AC-API-CUR-005, AC-API-CUR-006, AC-API-CUR-007, AC-API-CUR-008, AC-API-CUR-009, AC-API-MAJ-001, AC-API-MAJ-002, AC-API-MAJ-003, AC-API-MAJ-004, AC-API-MAJ-005, AC-API-MAJ-006, AC-API-MAJ-007, AC-API-MAJ-008, AC-API-OBE-001, AC-API-OBE-002, AC-API-OBE-003, AC-API-QA-001, AC-API-QA-002, AC-API-SUP-001, AC-API-SUP-002, AC-API-SUP-003 |
| PERM-MAJOR-002 | AC-RBAC-004-P, AC-RBAC-004-N | AC-API-CUR-001, AC-API-CUR-002, AC-API-CUR-003, AC-API-CUR-004, AC-API-CUR-005, AC-API-CUR-006, AC-API-CUR-007, AC-API-CUR-008, AC-API-CUR-009, AC-API-SUP-001, AC-API-SUP-002, AC-API-SUP-003 |
| PERM-MAJOR-003 | AC-RBAC-004-P, AC-RBAC-004-N | AC-API-PRF-001, AC-API-PRF-002, AC-API-STU-001, AC-API-STU-002, AC-API-STU-003, AC-API-STU-004, AC-API-WRN-001, AC-API-WRN-002 |
| PERM-ACAD-001 | AC-RBAC-005-P, AC-RBAC-005-N | AC-API-CUR-001, AC-API-CUR-002, AC-API-CUR-003, AC-API-CUR-004, AC-API-CUR-005, AC-API-CUR-006, AC-API-CUR-007, AC-API-CUR-008, AC-API-CUR-009, AC-API-MAJ-001, AC-API-MAJ-002, AC-API-MAJ-003, AC-API-MAJ-004, AC-API-MAJ-005, AC-API-MAJ-006, AC-API-MAJ-007, AC-API-MAJ-008, AC-API-RPT-001, AC-API-RPT-002, AC-API-RPT-003, AC-API-RPT-004, AC-API-RPT-005 |
| PERM-TEACHER-001 | AC-RBAC-006-P, AC-RBAC-006-N | AC-API-CC-001, AC-API-CC-002, AC-API-CC-003, AC-API-CC-004, AC-API-CRS-001, AC-API-CRS-002, AC-API-CRS-003, AC-API-CRS-004, AC-API-CRS-005, AC-API-CRS-006 |
| PERM-TEACHER-002 | AC-RBAC-006-P, AC-RBAC-006-N | AC-API-EVD-001, AC-API-EVD-002, AC-API-EVD-003, AC-API-EVD-004, AC-API-EVD-005, AC-API-EVD-006, AC-API-EVD-007 |
| PERM-TEACHER-003 | AC-RBAC-006-P, AC-RBAC-006-N | AC-API-CC-001, AC-API-CC-002, AC-API-CC-003, AC-API-CC-004, AC-API-FILE-001, AC-API-FILE-002, AC-API-FILE-003, AC-API-FILE-004, AC-API-FILE-005, AC-API-FILE-006, AC-API-KB-001, AC-API-KB-002, AC-API-KB-003, AC-API-KB-004, AC-API-KB-005, AC-API-KB-006, AC-API-KB-007, AC-API-KB-008 |
| PERM-MENTOR-001 | AC-RBAC-007-P, AC-RBAC-007-N | AC-API-APL-001, AC-API-APL-002, AC-API-APL-003, AC-API-APL-004, AC-API-PATH-001, AC-API-PATH-002, AC-API-PATH-003, AC-API-PRF-001, AC-API-PRF-002 |
| PERM-MENTOR-002 | AC-RBAC-007-P, AC-RBAC-007-N | AC-API-TODO-001, AC-API-WRN-001, AC-API-WRN-002 |
| PERM-MENTOR-003 | AC-RBAC-007-P, AC-RBAC-007-N | AC-API-MAP-001, AC-API-MAP-002, AC-API-MAP-003, AC-API-MAP-004, AC-API-MAP-005, AC-API-MAP-006 |
| PERM-STUDENT-001 | AC-RBAC-008-P, AC-RBAC-008-N | AC-API-APL-001, AC-API-APL-002, AC-API-APL-003, AC-API-APL-004, AC-API-PRF-001, AC-API-PRF-002, AC-API-STU-001, AC-API-STU-002, AC-API-STU-003, AC-API-STU-004 |
| PERM-STUDENT-002 | AC-RBAC-008-P, AC-RBAC-008-N | AC-API-EVD-001, AC-API-EVD-002, AC-API-EVD-003, AC-API-EVD-004, AC-API-EVD-005, AC-API-EVD-006, AC-API-EVD-007, AC-API-FILE-001, AC-API-FILE-002, AC-API-FILE-003, AC-API-FILE-004, AC-API-FILE-005, AC-API-FILE-006 |
| PERM-STUDENT-003 | AC-RBAC-008-P, AC-RBAC-008-N | AC-API-AUTHZ-001, AC-API-AUTHZ-002, AC-API-AUTHZ-003, AC-API-PUB-002 |
| PERM-EMP-001 | AC-RBAC-007-P, AC-RBAC-007-N, AC-RBAC-009-P, AC-RBAC-009-N | AC-API-EMP-001, AC-API-EMP-002, AC-API-EMP-003, AC-API-EMP-004, AC-API-EMP-005, AC-API-EMP-006 |
| PERM-EMP-002 | AC-RBAC-007-P, AC-RBAC-007-N, AC-RBAC-009-P, AC-RBAC-009-N | AC-API-PUB-001, AC-API-RPT-005 |
| PERM-EMP-003 | AC-RBAC-007-P, AC-RBAC-007-N, AC-RBAC-009-P, AC-RBAC-009-N | AC-API-EVL-001, AC-API-FDB-001, AC-API-FDB-002, AC-API-JOB-001, AC-API-JOB-002, AC-API-JOB-003 |
| PERM-KB-001 | AC-RBAC-010-P, AC-RBAC-010-N | AC-API-FILE-001, AC-API-FILE-002, AC-API-FILE-003, AC-API-FILE-004, AC-API-FILE-005, AC-API-FILE-006, AC-API-KB-001, AC-API-KB-002, AC-API-KB-003, AC-API-KB-004, AC-API-KB-005, AC-API-KB-006, AC-API-KB-007, AC-API-KB-008 |
| PERM-KB-002 | AC-RBAC-010-P, AC-RBAC-010-N | AC-API-KB-004, AC-API-KB-005 |
| PERM-AI-001 | AC-RBAC-002-P, AC-RBAC-002-N, AC-RBAC-004-P, AC-RBAC-004-N, AC-RBAC-006-P, AC-RBAC-006-N | AC-API-AI-001, AC-API-AI-002, AC-API-AI-003, AC-API-AI-004, AC-API-CFG-001, AC-API-CFG-002, AC-API-CFG-003, AC-API-CFG-004, AC-API-CFG-005, AC-API-CFG-006, AC-API-CFG-007, AC-API-CFG-008, AC-API-CFG-009, AC-API-CFG-010, AC-API-CFG-011, AC-API-CFG-012 |
| PERM-FILE-001 | AC-RBAC-001-P, AC-RBAC-001-N, AC-RBAC-002-P, AC-RBAC-002-N, AC-RBAC-004-P, AC-RBAC-004-N, AC-RBAC-006-P, AC-RBAC-006-N, AC-RBAC-008-P, AC-RBAC-008-N, AC-RBAC-009-P, AC-RBAC-009-N, AC-RBAC-010-P, AC-RBAC-010-N | AC-API-FILE-001, AC-API-FILE-002, AC-API-FILE-003, AC-API-FILE-004, AC-API-FILE-005, AC-API-FILE-006, AC-API-RPT-005 |
| PERM-RPT-001 | AC-RBAC-001-P, AC-RBAC-001-N, AC-RBAC-002-P, AC-RBAC-002-N, AC-RBAC-004-P, AC-RBAC-004-N, AC-RBAC-006-P, AC-RBAC-006-N, AC-RBAC-008-P, AC-RBAC-008-N, AC-RBAC-009-P, AC-RBAC-009-N, AC-RBAC-010-P, AC-RBAC-010-N | AC-API-MAP-006, AC-API-RPT-001, AC-API-RPT-002, AC-API-RPT-003, AC-API-RPT-004, AC-API-RPT-005 |
| PERM-ARC-001 | AC-RBAC-001-P, AC-RBAC-001-N, AC-RBAC-002-P, AC-RBAC-002-N, AC-RBAC-004-P, AC-RBAC-004-N, AC-RBAC-006-P, AC-RBAC-006-N, AC-RBAC-008-P, AC-RBAC-008-N, AC-RBAC-009-P, AC-RBAC-009-N, AC-RBAC-010-P, AC-RBAC-010-N | AC-API-ARC-001, AC-API-ARC-002, AC-API-ARC-003 |
| PERM-NOT-001 | AC-RBAC-001-P, AC-RBAC-001-N, AC-RBAC-002-P, AC-RBAC-002-N, AC-RBAC-004-P, AC-RBAC-004-N, AC-RBAC-006-P, AC-RBAC-006-N, AC-RBAC-008-P, AC-RBAC-008-N, AC-RBAC-009-P, AC-RBAC-009-N, AC-RBAC-010-P, AC-RBAC-010-N | AC-API-NOT-001, AC-API-NOT-002, AC-API-NOT-003, AC-API-TODO-001 |
| PERM-OSG-001 | AC-OSG-001, AC-OSG-002, AC-OSG-003, AC-OSG-004, AC-OSG-005, AC-OSG-006, AC-OSG-007, AC-OSG-008, AC-OSG-009, AC-OSG-010, AC-OSG-011, AC-OSG-012, AC-OSG-013, AC-OSG-014 | AC-OSG-001, AC-OSG-002, AC-OSG-003, AC-OSG-004, AC-OSG-005, AC-OSG-006, AC-OSG-007, AC-OSG-008, AC-OSG-009, AC-OSG-010, AC-OSG-011, AC-OSG-012, AC-OSG-013, AC-OSG-014 |
