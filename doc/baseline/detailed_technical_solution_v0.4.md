# 详细技术方案 v0.4

**产品名称：AI 原生本科专业建设与学生能力成长平台**  
**文档视角：技术负责人 / Tech Lead**  
**生成依据：**《产品需求说明书 v0.3》《系统架构设计 v0.3》《接口文档 v0.2》《权限判定矩阵 v1.0》《任务-接口精确映射表 v1.0》《验收清单终版 v1.2》《开发输入基线索引 v1.0》  
**文档定位：**将需求、架构、接口和验收项转化为开发团队可执行的模块实现、数据模型、数据库设计、前后端实现、权限、安全、开源治理、许可证合规、测试、发布与回滚方案。  

**权威输入说明：本文档与其他输入文件存在冲突时，版本、冲突优先级和自动开发读取规则以《开发输入基线索引 v1.0》为准。**

---

## 1. 输入充分性检查

### 1.1 检查结论

当前输入足够生成第一版《详细技术方案》。已有文档已明确：

| 输入项 | 结论 | 对技术方案的作用 |
|---|---|---|
| 产品需求说明书 v0.3 | 充分 | 提供 FR-001～FR-032、NFR-001～NFR-016、MVP 范围、业务流程、开源治理上游约束和需求决策。 |
| 系统架构设计 v0.3 | 充分 | 提供技术栈、模块边界、部署、权限、存储、AI、异步、文件、报告、审计、可观测性和开源治理 ADR。 |
| 接口文档 v0.2 | 充分 | 提供 API 编号、请求响应、错误码、枚举、幂等、文件签名 URL、报告快照、授权范围等接口契约。 |
| 验收清单终版 v1.2 | 充分 | 提供 P0/P1/P2 验收项、接口验收、权限验收、开源治理验收、发布准入和红线。 |

### 1.2 本方案不重新定义的内容

本方案不重新定义产品需求，不调整 MVP 范围，不新增模块范围，不推翻系统架构。所有实现均以既有需求编号、接口编号、架构决策和验收项为依据。

### 1.3 本方案修订边界与实施前确认结论

本版本在 v0.2 基础上补充并固化 8 项开源治理实施前确认结果。以下事项已不再作为阻塞性待确认问题，而作为开发、测试、部署和发布准入的工程约束执行。

| 编号 | 事项 | v0.3 确认结论 | 是否阻塞开发 | 发布前要求 |
|---|---|---|---:|---|
| OSG-DEC-001 | 生产 / 试点是否启用外部 AI Provider | MVP-1 / MVP-2 可审批启用；学生相关 AI 外部调用默认关闭，需单独审批。 | 否 | 启用外部 Provider 前必须有审批记录和学校管理员开关。 |
| OSG-DEC-002 | 平台托管对象存储 | 可使用云对象存储，但必须通过 FileStorageAdapter，记录供应商、地域、备份、成本和迁移路径。 | 否 | 生产配置需有存储审批与迁移说明。 |
| OSG-DEC-003 | 文件安全扫描 | 平台托管优先 ClamAV 独立服务；云扫描 / 学校组件需审批切换；生产禁止 Mock。 | 否 | 生产必须启用真实扫描器，ClamAV 集成方式需确认。 |
| OSG-DEC-004 | 报告生成引擎 | 优先 Apache POI / EasyExcel + docx4j / poi-tl + LibreOffice Headless / PDFBox / OpenPDF；iText / 商业 SDK 需审批。 | 否 | 报告引擎定型前完成中文样例 Spike 和许可证记录。 |
| OSG-DEC-005 | 私有化对象存储 | 不默认强制 MinIO；优先对接学校已有 S3 兼容对象存储；无现成能力时再评估 MinIO / Ceph。 | 否 | MinIO 因 AGPLv3 需负责人 / 法务确认。 |
| OSG-DEC-006 | 监控看板 | MVP 不默认 Grafana；先实现健康检查与指标；Grafana 需负责人 / 法务确认。 | 否 | 如启用 Grafana，需许可证确认记录。 |
| OSG-DEC-007 | SBOM / SCA | 生产 / 试点发布前必须生成 SBOM、许可证扫描、漏洞扫描和镜像扫描结果。 | 否 | 无扫描产物不得发布。 |
| OSG-DEC-008 | 开源治理同步 | 开源治理要求进入验收清单、测试计划和发布准入，新增 AC-OSG 验收项。 | 否 | 未满足 AC-OSG 发布准入不得上线。 |

### 1.4 本版本新增工程约束

在满足功能、安全、性能、可维护性、交付周期和许可证合规的前提下，本项目优先选择成熟、活跃、文档完善、社区健康、可自托管、可替换的开源模块和开源解决方案。

如需采用闭源 SaaS、商业 SDK、专有云服务或强供应商绑定方案，必须记录：

1. 开源方案为何不适合；
2. 候选开源替代方案；
3. 成本、风险和交付影响；
4. 供应商锁定风险；
5. 未来替换路径；
6. 是否需要负责人审批。


---

## 2. 技术基线与总体实现策略

### 2.1 技术栈基线

| 层级 | 技术选择 | 开源优先处理 | 依据 |
|---|---|---|---|
| 前端 | Vue 3 + TypeScript | Vue 3 为 MIT，TypeScript 为 Apache-2.0；符合开源优先。 | ARCH-DEC-003。 |
| UI 组件库 | Element Plus / Ant Design Vue 候选 | 均需在开发前复核许可证、维护活跃度和组件覆盖；优先 MIT / Apache-2.0。 | TS-TQ-001。 |
| 后端 | Java 21 + Spring Boot 3.x | 推荐 OpenJDK 发行版；避免未确认的商业 JDK 授权。Spring Boot 为 Apache-2.0。 | ARCH-DEC-004。 |
| 主数据库 | PostgreSQL | PostgreSQL License，符合开源优先。 | ARCH-DEC-005。 |
| 文件存储 | S3 兼容对象存储抽象；平台托管可接云对象存储，私有化可接 MinIO / Ceph | 云对象存储属于专有云服务，需审批；MinIO 为 AGPLv3，需负责人确认；Ceph 运维较重，作为大型私有化候选。 | ARCH-DEC-006。 |
| 异步任务 | PostgreSQL 任务表 + Java Worker 轮询 | 自研 + PostgreSQL，符合开源优先；后续队列优先 RabbitMQ / Kafka / Valkey 等开源候选。 | ARCH-DEC-007。 |
| AI | AIProviderAdapter；MVP 可接 1 个默认外部 Provider，保留本地 / 私有 Provider 配置位 | 外部 AI 属闭源 SaaS / 商业 API，必须审批、默认关闭；本地开源模型作为替换路径。 | ARCH-DEC-008，API-DEC-003。 |
| 知识库检索 | PostgreSQL 结构化索引 + 基础中文关键词匹配，预留 KnowledgeSearchAdapter | MVP 符合开源优先；后续优先 OpenSearch、pgvector、Milvus、Qdrant 等需单独审查。 | ARCH-DEC-009，API-DEC-012。 |
| 文档解析 | 待定；建议优先 Apache Tika / Apache POI / docx4j 等 | 必须配合文件安全扫描和解析沙箱；不得直接引入许可证不明组件。 | TS-TQ-007。 |
| 文件安全扫描 | FileSecurityScannerAdapter；ClamAV / 云扫描 / 学校组件候选 | ClamAV 为 GPLv2，建议独立服务方式接入并需确认；云扫描属于专有服务需审批。 | ARCH-DEC-011。 |
| 报告生成 | 后端模板化生成 + ReportRendererAdapter | Excel 优先 Apache POI / EasyExcel；Word 优先 docx4j / poi-tl；PDF 优先 PDFBox / OpenPDF / LibreOffice Headless。iText 默认不采用。 | ARCH-DEC-012，API-DEC-010。 |
| 审计日志 | PostgreSQL 分区化、追加式业务审计表 | 符合开源优先。 | ARCH-DEC-014，API-DEC-007。 |
| 监控与指标 | Spring Boot Actuator / Micrometer 基础指标；Prometheus / OpenTelemetry 后续候选 | 优先开源；Grafana 为 AGPLv3，若采用需确认。 | ARCH-DEC-015。 |
| 部署形态 | 平台托管单学校独立租户，预留专有云 / 私有化迁移 | 云资源和专有服务需保留替换路径。 | ARCH-DEC-001。 |


### 2.2 实现总原则

1. **模块化单体优先。** 后端以领域模块划分包边界和依赖边界，避免第一版微服务化。
2. **学校空间隔离优先。** 所有核心表带 `school_id` / `tenant_id`，所有查询默认带租户过滤。
3. **状态机显式建模。** AI 草案、审核、课程体系版本、知识库资料、学习证据、能力地图、企业账号、异步任务均使用明确状态字段。
4. **正式数据不可被 AI 直接写入。** AI 只能生成 Draft、Suggestion 或 Analysis，发布必须走人工审核。
5. **文件不进数据库。** PostgreSQL 只保存文件元数据，文件实体进入对象存储。
6. **长耗时操作任务化。** AI、解析、导入、报告、能力地图生成均写入 `async_task`，由 Worker 执行。
7. **接口契约稳定。** 前端只依赖 API v0.2 已定义字段、枚举和错误码；新增字段向后兼容。
8. **审计与运行日志分离。** 业务审计进入 `audit_log`；运行日志用于排障，不存敏感明文。
9. **数据不可物理删除。** 已发布版本、审核、AI 运行、引用、授权、审计等关键记录只归档，不物理删除。

---


## 3. 开源优先技术选型原则

### 3.1 基本原则

1. **开源优先，但不唯开源。** 在满足功能、安全、性能、可维护性、交付周期和许可证合规的前提下，优先选择成熟、活跃、文档完善、社区健康、可自托管、可替换的开源组件。
2. **许可证合规优先于短期便利。** 引入任何 GPL、AGPL、SSPL、商业双许可或许可证不明确组件前，必须完成负责人确认。
3. **避免强供应商绑定。** AI、对象存储、文件扫描、报告渲染、知识检索、邮件、任务队列、认证等外部能力必须通过 Adapter / Provider 抽象接入。
4. **MVP 不引入过重依赖。** 不为追求“完整开源技术栈”而提前引入向量数据库、消息队列、日志平台、复杂搜索引擎、商业报表平台或低质量依赖。
5. **生产依赖可审计。** 生产 / 试点发布前必须形成第三方依赖清单、许可证清单、漏洞扫描结果和例外审批记录。
6. **Mock 不进生产。** MockAIProvider、MockFileSecurityScanner、SimpleReportRenderer 等测试替身只能用于开发 / 测试环境，不得在生产 / 试点环境启用。

### 3.2 开源治理分级

| 等级 | 说明 | 处理规则 |
|---|---|---|
| OS-1 低风险开源 | MIT、Apache-2.0、BSD、PostgreSQL License 等宽松许可证 | 可优先采用；保留 LICENSE / NOTICE；纳入 SBOM。 |
| OS-2 中风险开源 | MPL、LGPL、EPL 等弱 Copyleft | 可采用；需记录使用方式、动态链接 / 独立进程边界和许可证义务。 |
| OS-3 高风险开源 | GPL、AGPL、SSPL、强 Copyleft、网络服务义务许可证 | 必须负责人确认；必要时法务审查；不得默认引入生产。 |
| CS-1 商业 SDK | 闭源 SDK、商业授权库、商业报表 / PDF / 安全 SDK | 必须说明开源替代方案、成本、风险和替换路径，并审批。 |
| CS-2 闭源 SaaS | 外部 AI、商业邮件、云文件扫描、云监控等 | 必须审批、默认关闭或可配置，并明确数据出域策略。 |
| PC-1 专有云服务 | 云对象存储、云数据库、云安全、云日志等 | 可用于平台托管，但必须保留抽象层和迁移路径。 |
| SELF | 自研方案 | 需控制实现范围，避免重复造复杂基础设施；必须有测试覆盖。 |

### 3.3 引入新依赖的准入流程

```text
提出依赖
→ 填写用途、候选版本、许可证、替代方案
→ 技术负责人评估功能和维护性
→ 安全 / 许可证负责人评估许可证和漏洞风险
→ 如为 OS-3 / CS-1 / CS-2 / PC-1，负责人审批
→ 进入依赖清单和 SBOM
→ CI 扫描通过后合并
```

### 3.4 禁止规则

1. 业务模块不得直接调用闭源厂商 SDK，必须走 Adapter。
2. 未经审批不得引入 AGPL / GPL / SSPL 组件进入生产依赖。
3. 未经审批不得将学生个人数据发送到外部 AI Provider。
4. 未经审批不得使用商业 PDF / 报表 SDK 作为默认实现。
5. 未经审批不得在生产环境启用许可证不明确的 npm / Maven 包。
6. 不得将永久公开 URL、API Key、Provider Key、数据库密码写入代码、日志或配置仓库。

---

## 4. 关键技术组件选型表

| 组件 | 当前方案 / 候选 | 分类 | 开源优先判断 | 修订处理 | 关联模块 |
|---|---|---|---|---|---|
| 前端框架 | Vue 3 + TypeScript | 开源方案 | 符合 | 保留原方案 | 前端全局 |
| UI 组件库 | Element Plus / Ant Design Vue 候选 | 开源方案 | 基本符合 | 开发前最终选型并记录许可证 | 前端全局 |
| 后端框架 | Java 21 + Spring Boot 3.x | 开源方案 | 符合 | 保留原方案，JDK 采用 OpenJDK 发行版 | 后端全局 |
| ORM / SQL | Spring Data JPA + MyBatis 优先；jOOQ 候选 | 开源 / 商业双许可候选 | 基本符合 | jOOQ 需确认使用场景；复杂查询优先 MyBatis | 数据访问 |
| 主数据库 | PostgreSQL | 开源方案 | 符合 | 保留原方案 | 全部核心数据 |
| 数据库迁移 | Flyway Community / Liquibase 候选 | 开源 / 商业双线 | 符合 | 优先 Flyway Community；商业高级功能需审批 | 数据库 |
| 对象存储 | S3 兼容抽象 + 云对象存储 / MinIO / Ceph | 专有云 / 开源候选 | 需治理 | 保留抽象；云存储审批；MinIO AGPL 需确认 | 文件 |
| 异步任务 | PostgreSQL 任务表 + Java Worker | 自研 + 开源数据库 | 符合 | 保留原方案 | AI、文件、报告 |
| 后续队列 | RabbitMQ / Kafka / Valkey 候选 | 开源方案 | 符合 | 仅规模上升后引入；新版 Redis 不作为默认候选 | 异步任务 |
| AI Provider | 默认外部 AI Provider + 本地 Provider 配置位 | 闭源 SaaS + 自研抽象 | 需审批 | 保留外部 Provider，但默认关闭、审批启用 | AI |
| 本地模型服务 | vLLM / Ollama / llama.cpp 类候选 | 开源候选 | 方向符合 | 后续私有化 / 数据不出校场景评估 | AI |
| 知识库检索 | PostgreSQL 结构化索引 + 关键词匹配 | 开源方案 | 符合 | 保留 MVP 方案 | 知识库 |
| 后续搜索 | OpenSearch、pgvector、Milvus、Qdrant 候选 | 开源候选 | 需专项 | 后续按规模和效果评估 | 知识库 |
| 文档解析 | Apache Tika / POI / docx4j 候选 | 开源方案 | 基本符合 | 需安全沙箱和样例验证 | 文件解析 |
| 文件安全扫描 | FileSecurityScannerAdapter + ClamAV / 云扫描 | 开源高风险 / 专有服务 | 需确认 | ClamAV 独立服务方式优先；云扫描需审批 | 文件安全 |
| Excel 生成 | Apache POI / EasyExcel | 开源方案 | 符合 | 优先 Apache POI；大表格可评估 EasyExcel | 报告 |
| Word 生成 | docx4j / poi-tl | 开源候选 | 基本符合 | 需中文模板 Spike；poi-tl 需许可证复核 | 报告 |
| PDF 生成 | PDFBox / OpenPDF / LibreOffice Headless | 开源候选 | 基本符合 | 需中文排版 Spike；iText 不作为默认 | 报告 |
| iText | AGPL / 商业双许可 | 高风险 / 商业 | 不符合默认开源优先 | 仅在审批后使用 | 报告 |
| 邮件 | EmailNotificationAdapter + SMTP / 平台邮件 | 开源协议 / 商业服务 | 基本符合 | 优先 SMTP；商业邮件需审批 | 通知 |
| 监控指标 | Actuator / Micrometer | 开源方案 | 符合 | 建议明确采用 | 运维 |
| 监控平台 | Prometheus / OpenTelemetry；Grafana 候选 | 开源 / AGPL | 部分需确认 | Prometheus 优先；Grafana 需 AGPL 确认 | 运维 |
| 审计日志 | PostgreSQL 分区化追加式表 | 开源方案 | 符合 | 保留原方案 | 审计 |

---

## 5. 开源依赖清单

> 说明：本清单为技术方案级初始清单，不替代最终 Maven / npm / 容器镜像依赖清单。实际发布必须由构建工具生成 SBOM 与许可证扫描结果。

### 5.1 默认允许优先采用的开源依赖

| 依赖 | 用途 | 许可证风险级别 | 处理方式 |
|---|---|---|---|
| Vue 3 | 前端框架 | OS-1 | 允许采用。 |
| TypeScript | 类型系统 | OS-1 | 允许采用。 |
| Pinia | 前端状态管理候选 | OS-1 | 允许采用。 |
| Element Plus / Ant Design Vue | UI 组件库候选 | OS-1，需版本复核 | 最终选型前复核许可证和维护活跃度。 |
| OpenJDK 发行版 | Java 运行时 | OS-1/OS-2，按发行版 | 采用 Eclipse Temurin / Amazon Corretto / Azul Zulu 等开源发行版。 |
| Spring Boot / Spring Framework | 后端框架 | OS-1 | 允许采用。 |
| PostgreSQL | 主数据库 | OS-1 | 允许采用。 |
| MyBatis | 复杂 SQL | OS-1 | 允许采用。 |
| Flyway Community | 数据库迁移 | OS-1 | 允许采用。 |
| Apache POI | Excel / Office 处理 | OS-1 | Excel 首选。 |
| docx4j | Word DOCX 处理 | OS-1 | 需中文模板验证。 |
| Apache PDFBox | PDF 处理 | OS-1 | PDF 候选。 |
| Apache Tika | 文档解析候选 | OS-1 | 需配合安全扫描和沙箱。 |
| Micrometer / Spring Boot Actuator | 健康检查和指标 | OS-1 | 建议采用。 |
| Prometheus | 指标采集后续候选 | OS-1 | 后续运维增强候选。 |
| OpenTelemetry | 链路追踪后续候选 | OS-1 | 后续增强。 |
| OpenSearch | 后续全文检索 / 日志检索候选 | OS-1 | 后续专项评估。 |

### 5.2 可采用但需要负责人确认的开源依赖

| 依赖 | 用途 | 风险 | 处理方式 |
|---|---|---|---|
| MinIO | 私有化对象存储候选 | AGPLv3 | 需要负责人 / 法务确认；评估是否使用未修改独立服务；同时保留 Ceph 备选。 |
| ClamAV | 文件安全扫描候选 | GPLv2；libclamav 直接链接风险 | 推荐以独立进程 / 服务方式调用；需要负责人确认。 |
| Grafana | 监控可视化后续候选 | AGPLv3 | 若自托管或修改，需确认；MVP 不强制采用。 |
| OpenPDF | PDF 候选 | LGPL/MPL | 可候选；需确认集成方式和许可证义务。 |
| LibreOffice Headless | DOCX 转 PDF 候选 | MPLv2 + 多组件许可证 | 需部署包许可证清单、字体和进程隔离验证。 |
| jOOQ | SQL DSL 候选 | 开源 / 商业双许可 | 使用 PostgreSQL 可评估开源版；若引入商业数据库需重新审批。 |
| pgvector / Milvus / Qdrant | 向量检索后续候选 | 需版本核查 | 后续知识库增强专项评估。 |

### 5.3 默认不采用或需专项审批的依赖

| 依赖 / 服务 | 原因 | 处理方式 |
|---|---|---|
| iText | AGPL / 商业双许可；默认使用会带来许可证或商业授权风险 | 不作为默认 PDF 方案；只有在开源替代无法满足中文复杂排版且审批后使用。 |
| 新版 Redis | RSALv2 / SSPLv1 / AGPL 等许可变化，不作为默认 OSI 开源方案 | 后续缓存 / 队列优先评估 Valkey、RabbitMQ、Kafka。 |
| Elasticsearch / Elastic Stack | 许可路线不完全符合开源优先 | 后续优先评估 OpenSearch；如采用需审批。 |
| 商业报表平台 | 成本、锁定和私有化适配风险 | MVP 不采用；如后续采用需审批。 |
| 商业文件扫描 SaaS | 数据出域和供应商锁定 | 仅作为可选，生产启用前审批。 |
| 商业邮件服务 | 费用、数据出域、供应商绑定 | 可选；优先 SMTP。 |

---

## 6. 非开源方案例外说明

### 6.1 默认外部 AI Provider

| 项 | 说明 |
|---|---|
| 保留理由 | AI 是产品核心能力，MVP-1 需要真实生成专业画像、OBE 草案、课程体系草案和质量分析。完全本地开源模型首版落地会显著增加推理资源、模型效果验证和运维复杂度。 |
| 开源替代 | 本地开源模型 + vLLM / Ollama / llama.cpp 类服务；学校私有大模型服务。 |
| 成本影响 | 外部 Provider 有调用成本；本地模型有 GPU / 推理资源和运维成本。 |
| 安全风险 | 数据出域、模型输出不可控、Provider 不可用、配额和限流。 |
| 锁定风险 | 中到高。若 Prompt、Schema、调用参数绑定具体厂商，会形成锁定。 |
| 替换路径 | 保留 AIProviderAdapter；接口只暴露 providerId / modelId；Prompt 与 Schema 独立版本化。 |
| 审批 | 需要负责人审批；外部 Provider 默认关闭，由学校管理员显式启用。 |

### 6.2 平台托管云对象存储

| 项 | 说明 |
|---|---|
| 保留理由 | MVP 平台托管单学校租户需要快速、可靠、低运维成本的文件存储。 |
| 开源替代 | MinIO、Ceph。 |
| 成本影响 | 云存储按容量、请求和流量计费；自托管需运维成本。 |
| 安全风险 | 云账号权限、地域、访问策略、签名 URL 配置不当。 |
| 锁定风险 | 中。不同云厂商 API、生命周期规则和权限模型存在差异。 |
| 替换路径 | FileStorageAdapter + S3 兼容协议；对象 key 规则不绑定厂商；数据库保存元数据。 |
| 审批 | 专有云服务需负责人审批；私有化时重新评估 MinIO / Ceph。 |

### 6.3 云文件安全扫描 / 云监控 / 商业邮件

这些能力均不得作为业务模块硬依赖。生产使用前必须记录选择原因、开源替代、数据范围、成本和替换路径。默认替代方案如下：

| 能力 | 开源 / 开放替代 | 默认策略 |
|---|---|---|
| 文件安全扫描 | ClamAV 独立服务 | 优先自托管或学校指定组件；云扫描需审批。 |
| 邮件 | SMTP | 邮件为可选适配，未配置时企业邀请可复制链接。 |
| 监控 | Actuator / Micrometer / Prometheus / OpenTelemetry | MVP 先基础指标；云监控可选但不强绑定。 |

---

### 6.7 已确认非开源例外与审批口径

| 例外项 | v0.3 确认口径 | 必须保留的替代方案 | 审批要求 |
|---|---|---|---|
| 外部 AI Provider | MVP-1 / MVP-2 可审批启用；学生相关 AI 外部调用默认关闭。 | 本地 / 私有 AI Provider；开源模型服务；MockAIProvider 仅测试使用。 | 启用前需负责人审批、学校管理员显式启用、记录数据出域策略。 |
| 云对象存储 | 平台托管可使用云对象存储，但必须通过 FileStorageAdapter。 | 学校已有 S3 兼容对象存储、MinIO、Ceph。 | 需记录供应商、地域、加密、备份、成本、迁移路径和审批记录。 |
| 云文件安全扫描 | 默认优先 ClamAV 独立服务；云扫描作为可切换方案。 | ClamAV 独立服务、学校指定扫描组件。 | 使用云扫描需审批并记录文件出域范围。 |
| 商业 PDF / 报表 SDK | 不作为默认方案。 | Apache POI、EasyExcel、docx4j、poi-tl、LibreOffice Headless、PDFBox、OpenPDF。 | 仅在开源 Spike 失败且业务必要时审批。 |
| Grafana | MVP 不默认启用。 | Actuator + Micrometer 基础指标；Prometheus；云监控可选。 | 如自托管 Grafana，需 AGPLv3 负责人 / 法务确认。 |
| MinIO | 私有化不默认强制采用。 | 学校已有 S3 兼容服务、Ceph。 | 因 AGPLv3，作为交付组件前需负责人 / 法务确认。 |


## 7. 许可证与合规风险

| 风险项 | 等级 | 影响 | 处理要求 |
|---|---:|---|---|
| GPL / AGPL 组件进入生产依赖 | 高 | 可能触发源码披露、分发或网络服务义务 | 必须负责人确认；必要时法务审查。 |
| libclamav 直接链接 | 高 | GPLv2 对闭源商业代码影响较大 | 不直接链接；以独立进程 / 服务方式调用。 |
| MinIO AGPLv3 | 高 | 私有化交付和网络服务合规需要确认 | 使用前负责人确认；评估 Ceph。 |
| Grafana AGPLv3 | 中 | 自托管和修改场景需确认 | MVP 不强制；如采用需确认。 |
| iText AGPL / 商业双许可 | 高 | AGPL 或商业授权成本 | 默认不采用；审批后方可使用。 |
| Redis 新许可 | 中 | 后续缓存 / 队列误用可能不符合开源优先 | 后续优先 Valkey、RabbitMQ、Kafka。 |
| Elasticsearch 许可 | 中 | 后续搜索 / 日志平台锁定 | 优先 OpenSearch。 |
| npm / Maven 间接依赖 | 中 | 间接引入高风险许可证或漏洞 | 依赖扫描、SBOM、锁定版本。 |
| 字体包许可证 | 中 | PDF 中文渲染可能涉及字体授权 | 报告引擎 Spike 时同步确认字体许可证。 |

### 7.1 许可证合规准入

1. 所有生产依赖必须出现在 SBOM 中。
2. 所有 GPL / AGPL / SSPL / 商业双许可依赖必须有审批记录。
3. 所有第三方二进制、字体、模板、图标、模型权重均需纳入许可证清单。
4. CI 中应执行依赖漏洞扫描和许可证扫描。
5. 无法识别许可证的依赖默认不得进入生产。

---

## 8. 供应商锁定风险与替换路径

| 能力 | 供应商锁定风险 | 必须保留的替换路径 |
|---|---:|---|
| AI Provider | 高 | AIProviderAdapter；Provider / Model 抽象；Prompt 与 Schema 独立版本化；本地 / 私有 Provider 配置位。 |
| 对象存储 | 中 | FileStorageAdapter；S3 兼容接口；对象 key 标准化；文件元数据独立存储。 |
| 文件安全扫描 | 中 | FileSecurityScannerAdapter；扫描结果状态机独立；可替换云扫描 / ClamAV / 学校组件。 |
| 报告引擎 | 中 | ReportRendererAdapter；模板版本和数据快照独立；Word / Excel / PDF 引擎可替换。 |
| 知识库检索 | 中 | KnowledgeSearchAdapter；MVP PostgreSQL 实现；后续向量 / 搜索引擎可替换。 |
| 邮件 | 低到中 | EmailNotificationAdapter；邮件为可选，站内通知为基础。 |
| 认证 | 中 | AuthenticationProvider；MVP 内置账号；后续 CAS / OAuth2 / SAML / LDAP。 |
| 任务队列 | 低到中 | TaskQueueAdapter；MVP PostgreSQL 任务表；后续 RabbitMQ / Kafka / Valkey。 |
| 监控 | 低到中 | 结构化日志和指标标准；可接 Prometheus / OpenTelemetry / 云监控。 |

### 8.1 锁定风险控制红线

1. 不得在业务模块中直接引用具体 AI SDK。
2. 不得在业务模块中直接引用具体云对象存储 SDK。
3. 不得将厂商 URL、Bucket、Region、模型名写死在代码中。
4. 不得使用只有某厂商支持的报告模板特性作为业务必需能力。
5. 外部服务配置必须可按学校启用、停用和替换。

---

## 9. 第三方服务适配层设计

### 9.1 Adapter 清单

| Adapter | 职责 | 默认实现 | 替代实现 | 直接关联需求 |
|---|---|---|---|---|
| AIProviderAdapter | 调用 AI Provider、Schema 校验、返回 AI Run 结果 | DefaultExternalAIProviderAdapter | LocalPrivateAIProviderAdapter、MockAIProviderAdapter | FR-005、FR-011、FR-016、FR-030 |
| FileStorageAdapter | 上传、下载、签名 URL、对象元数据 | S3CompatibleStorageAdapter | CloudStorageAdapter、MinIOAdapter、CephAdapter、MockStorageAdapter | FR-015、FR-021、FR-026、FR-027 |
| FileSecurityScannerAdapter | 文件扫描、重扫、扫描结果写入 | ClamAVServiceAdapter / MockScanner | CloudScanAdapter、SchoolScannerAdapter | FR-015、FR-021、NFR-012 |
| ReportRendererAdapter | Word / Excel / PDF 渲染 | OpenSourceReportRenderer | LibreOfficeRenderer、CommercialRenderer（审批） | FR-013、FR-025、FR-026 |
| KnowledgeSearchAdapter | 知识库检索、AI 引用检索 | PostgreSQLKnowledgeSearchAdapter | OpenSearchAdapter、VectorSearchAdapter、HybridSearchAdapter | FR-015、FR-016、FR-030 |
| EmailNotificationAdapter | 邮件发送 | SMTPEmailAdapter / DisabledEmailAdapter | CommercialEmailAdapter（审批） | FR-028、NFR-009 |
| TaskQueueAdapter | 任务创建、拉取、状态更新 | PostgreSQLTaskQueueAdapter | RabbitMQAdapter、KafkaAdapter、ValkeyAdapter | NFR-009 |
| AuthenticationProvider | 登录认证 | LocalAuthProvider | CASProvider、OIDCProvider、SAMLProvider、LDAPProvider | FR-001、FR-002 |

### 9.2 Adapter 实现规则

1. Adapter 接口定义在对应领域模块或 common integration 层，业务服务只依赖接口。
2. 具体实现通过 Spring Bean Profile / 配置开关注入。
3. 每个 Adapter 必须有 Mock 实现，用于单元测试和集成测试。
4. Adapter 失败必须转换为统一错误码，不向上泄露厂商异常。
5. Adapter 调用必须记录 traceId；涉及业务操作的还需记录审计日志。
6. 外部服务调用不得在运行日志中输出密钥、Prompt 原文、学生隐私或永久 URL。

---

## 10. 依赖安全与版本管理策略

### 10.1 依赖锁定

| 层级 | 要求 |
|---|---|
| Maven | 使用固定版本；禁止 `LATEST`、`RELEASE`、动态范围版本；统一 dependencyManagement。 |
| npm | 使用 lockfile；禁止未锁定生产依赖；新增依赖需 PR 说明用途和许可证。 |
| 容器镜像 | 使用固定版本标签或 digest；禁止生产使用 `latest`。 |
| 系统工具 | LibreOffice、ClamAV、字体包等需记录版本和安装来源。 |

### 10.2 安全扫描

生产 / 试点发布前必须完成：

1. Maven 依赖漏洞扫描；
2. npm 依赖漏洞扫描；
3. 容器镜像漏洞扫描；
4. 许可证扫描；
5. SBOM 生成；
6. 高危漏洞处理或风险接受记录；
7. GPL / AGPL / SSPL / 商业双许可依赖审批检查。

可选工具候选：OWASP Dependency-Check、OSV-Scanner、Syft、Grype、Trivy、ScanCode Toolkit。最终工具由 DevSecOps 方案确定。

### 10.3 版本升级策略

1. 安全补丁优先升级。
2. 大版本升级必须经过兼容性测试。
3. AI Provider SDK 如可避免，则优先使用 HTTP Adapter，减少 SDK 锁定。
4. 报告渲染库升级必须重新验证中文、分页、表格、字体、支撑矩阵和能力地图 PDF。
5. 文件解析库升级必须重新验证恶意文件拦截、解析失败降级和大文件异步处理。

### 10.4 生产禁用项

1. MockAIProvider；
2. MockFileSecurityScanner；
3. MockStorageAdapter；
4. SimpleReportRenderer，仅限测试；
5. 未审批的商业 SDK；
6. 未审批的 GPL / AGPL / SSPL 组件；
7. 未加密的 Provider Key；
8. 永久公开文件 URL。


### 10.5 生产 / 试点发布准入门禁

生产 / 试点发布必须满足以下门禁条件：

| 门禁编号 | 门禁项 | 阻断规则 |
|---|---|---|
| RG-OSG-001 | SBOM | 无 SBOM 文件，不允许发布。 |
| RG-OSG-002 | 第三方依赖清单 | 前端、后端、容器镜像和关键运维组件依赖清单不完整，不允许发布。 |
| RG-OSG-003 | 许可证扫描 | 无许可证扫描结果，不允许发布。 |
| RG-OSG-004 | 高风险许可证审批 | 存在未审批 GPL / AGPL / SSPL / 商业 SDK / 闭源 SaaS / 专有云服务，不允许发布。 |
| RG-OSG-005 | 漏洞扫描 | 存在未处理 Critical / High 漏洞且无风险接受记录，不允许发布。 |
| RG-OSG-006 | 容器镜像扫描 | 生产镜像未扫描，不允许发布。 |
| RG-OSG-007 | Mock 禁用 | 生产配置启用 MockAIProvider、MockFileSecurityScanner、MockStorageAdapter 或 SimpleReportRenderer，不允许发布。 |
| RG-OSG-008 | Adapter 边界 | 业务模块绕过 Adapter 直接绑定厂商 SDK，必须整改后发布。 |
| RG-OSG-009 | 外部 AI Provider | 外部 AI Provider 启用但无学校管理员配置和负责人审批，不允许发布。 |
| RG-OSG-010 | 文件安全扫描 | 生产环境无真实扫描器，或扫描失败文件可进入解析 / 预览 / 下载，不允许发布。 |

推荐工具组合：Syft 生成 SBOM；Grype、OSV-Scanner、Trivy 执行漏洞和镜像扫描；ScanCode Toolkit、Maven license plugin、npm / pnpm license 工具执行许可证扫描。具体工具可在 DevSecOps 实施阶段微调，但不得取消上述发布准入产物。


---

## 11. 对实施任务拆解的影响

以下任务是在 v0.1 的 DEV-001～DEV-060 基础上新增或修订的实施任务，不改变产品需求，只补充工程治理、开源合规和可替换能力。

| 任务ID | 阶段 | 模块 | 实现任务 | 关联需求 | 关联接口 | 数据表 / 模块 | 主要测试点 | 主要风险 |
|---|---|---|---|---|---|---|---|---|
| DEV-061 | 基础 | OpenSource Governance | 建立第三方依赖准入流程和许可证分级规则 | NFR-012、NFR-014 | 无直接业务 API | dependency_policy、工程规范 | 新增依赖需填写用途、许可证、替代方案；OS-3/CS/PC 项需审批 | 团队绕过流程直接引包 |
| DEV-062 | 基础 | Build / CI | 接入 SBOM 生成、许可证扫描和漏洞扫描 | NFR-012、NFR-014 | 无直接业务 API | CI/CD、SBOM 文件 | 生产发布前生成 SBOM；高风险许可证阻断或审批 | 工具误报 / 漏报影响交付 |
| DEV-063 | 基础 | Adapter Governance | 检查 AI、文件、扫描、报告、检索、邮件、任务、认证 Adapter 边界 | FR-001、FR-030、NFR-012 | API-AI、API-FILE、API-RPT 等 | Adapter 模块 | 业务模块不得直接依赖厂商 SDK；Mock 只在测试启用 | 厂商 SDK 穿透业务层 |
| DEV-064 | MVP-1 | AI Governance | 外部 AI Provider 例外审批、默认关闭、学校启用和数据出域策略校验 | FR-030、NFR-010 | API-CFG-003～004、API-AI | school_ai_settings、ai_runs | 外部 Provider 默认 disabled；学生数据禁出域；审批记录存在 | 未审批启用外部 AI |
| DEV-065 | MVP-2 | File Security | ClamAV / 云扫描候选 Spike 与生产接入决策 | FR-015、FR-021、NFR-012 | API-FILE-006 | FileSecurityScannerAdapter | GPL 集成方式确认；扫描失败阻止解析下载 | 文件扫描组件许可证或部署不可接受 |
| DEV-066 | MVP-1/MVP-4 | Report Renderer | 开源报告引擎 Spike：Word、Excel、PDF 中文样例验证 | FR-013、FR-025、FR-026 | API-RPT | ReportRendererAdapter | 支撑矩阵、课程地图、能力地图 PDF、中文字体、分页通过 | PDF 中文渲染失败导致商业 SDK 依赖 |
| DEV-067 | 横向 | Object Storage | 云对象存储与 MinIO/Ceph 替换路径验证 | FR-015、FR-021、NFR-014 | API-FILE | FileStorageAdapter | S3 兼容、签名 URL、迁移导出、权限校验 | 云服务锁定或 MinIO AGPL 风险 |
| DEV-068 | 横向 | Observability | Actuator / Micrometer 基础指标接入，Prometheus / Grafana 风险评估 | NFR-008、NFR-009 | 健康检查 / 运维接口 | ops 模块 | 健康检查、Worker 心跳、AI 失败指标 | Grafana AGPL 未审批 |
| DEV-069 | 横向 | Release Governance | 生产发布准入检查：依赖清单、许可证、漏洞、例外审批、Mock 禁用 | NFR-012、NFR-014 | 无直接业务 API | release checklist | 发布前检查项全部通过 | 发布流程被跳过 |
| DEV-070 | 横向 | Documentation | 更新上游 / 下游文档中的开源优先、例外审批和测试验收项 | 全部 NFR | 无直接业务 API | PRD、架构、接口、验收、测试计划 | 文档一致性检查 | 文档未同步导致验收口径不一致 |

### 11.1 对原任务的定点修订

| 原任务 | 修订点 |
|---|---|
| DEV-013 AIProviderAdapter | 增加“外部 Provider 审批记录检查”和“学生数据禁出域策略单元测试”。 |
| DEV-023 / DEV-056 Report | 报告引擎不得默认采用 iText；需先完成开源候选 Spike。 |
| DEV-028 File | 签名 URL 实现不得依赖厂商专有特性；保留 S3 兼容。 |
| DEV-029 File Security | 扫描组件具体实现需经过许可证确认；Mock 不得进入生产。 |
| DEV-033 Knowledge Search | 不引入 Elasticsearch / 向量库作为 MVP 依赖；后续引入需审查。 |
| DEV-057 Observability | Grafana 不作为默认强依赖；先实现 Actuator / Micrometer。 |
| DEV-058 Backup | 云备份能力如使用专有云服务，需记录服务依赖和替换路径。 |
| DEV-060 Test Automation | 增加依赖许可证、SBOM、漏洞扫描和例外审批测试 / 检查。 |

---

## 12. 对测试计划和验收清单的影响

### 12.1 新增验收项建议

| 验收项ID | 类别 | 验收项 | 前置条件 | 操作步骤 | 预期结果 | 优先级 |
|---|---|---|---|---|---|---|
| AC-OSG-001 | 开源治理 | 第三方依赖清单完整 | 构建完成 | 查看 Maven、npm、容器镜像依赖清单 | 所有生产依赖均有名称、版本、用途、许可证 | P0 |
| AC-OSG-002 | 许可证合规 | 高风险许可证项审批 | 存在 GPL / AGPL / SSPL / 商业组件 | 查看审批记录 | 每个高风险项均有审批结论和使用边界 | P0 |
| AC-OSG-003 | 闭源 / SaaS 例外 | 外部 AI Provider 审批与默认关闭 | 配置外部 AI Provider | 查看学校 AI 配置 | 外部 Provider 默认关闭，启用需学校管理员和审批记录 | P0 |
| AC-OSG-004 | 供应商锁定 | Adapter 边界检查 | 代码评审 / 架构检查 | 检查业务模块依赖 | 业务模块不直接依赖具体 AI、对象存储、扫描、报告、邮件厂商 SDK | P0 |
| AC-OSG-005 | SBOM | 生产发布 SBOM | 生产 / 试点发布前 | 执行构建流水线 | 生成 SBOM 文件并归档 | P1 |
| AC-OSG-006 | 漏洞扫描 | 高危漏洞处理 | 构建完成 | 查看漏洞扫描报告 | 无未处理 Critical / High 漏洞；例外项有风险接受记录 | P0 |
| AC-OSG-007 | Mock 禁用 | 生产环境无 Mock 实现 | 生产配置准备完成 | 检查配置和启动日志 | MockAIProvider、MockFileSecurityScanner、SimpleReportRenderer 未启用 | P0 |
| AC-OSG-008 | 报告开源引擎验证 | 报告引擎定型前 | 使用支撑矩阵、课程地图、能力地图样例生成报告 | 中文、表格、分页、字体显示正常；库许可证已记录 | P1 |
| AC-OSG-009 | 高风险组件审批 | 使用 MinIO、Grafana、iText、ClamAV、Redis、Elasticsearch / ELK 等组件前 | 查看审批记录和许可证审查记录 | 每个高风险组件均有负责人确认、使用方式和替代方案 | P0 |
| AC-OSG-010 | 非开源方案例外记录 | 使用闭源 SaaS、商业 SDK、专有云服务前 | 查看例外说明 | 存在保留理由、开源替代方案、供应商锁定风险和未来替换路径 | P0 |

### 12.2 测试计划新增内容

1. **SCA 测试：** 检查 Maven / npm / 容器镜像依赖许可证和漏洞。
2. **Adapter 替身测试：** AI、对象存储、扫描、报告、邮件均必须可用 Mock 实现跑通测试。
3. **外部服务关闭测试：** 外部 AI Provider 关闭时，AI 任务应返回 `AI_PROVIDER_DISABLED` 或明确提示，不应崩溃。
4. **供应商替换冒烟测试：** FileStorageAdapter 至少用 Mock 和 S3 兼容实现跑通上传、下载、签名 URL。
5. **报告引擎 Spike 测试：** 用真实中文样例验证 Word / PDF / Excel。
6. **许可证阻断测试：** 构建流水线对未审批高风险许可证给出阻断或告警。
7. **生产配置检查：** 生产 profile 不允许启用 Mock 和未审批服务。

---
## 13. 工程结构建议

### 13.1 后端工程结构

建议采用单仓库模块化单体结构：

```text
backend/
  app/                         Spring Boot 启动模块
  common/                      通用响应、错误码、分页、幂等、审计、租户上下文
  auth/                        认证、本地账号、Token、Authentication Provider 抽象
  organization/                学校、学院、用户、角色、Scope
  settings/                    学校级配置中心
  dashboard/                   学校/学院/专业看板指标
  major/                       专业、培养方案版本、专业工作台
  obe/                         培养目标、毕业要求、能力指标点
  curriculum/                  课程体系、课程地图、支撑矩阵、质量分析
  course/                      平台课程库、专业课程版本、课程内容
  knowledge/                   知识库资料、片段、检索、引用
  student/                     学生档案、画像、申诉、培养路径
  evidence/                    学习证据、证据审核、能力映射
  ability/                     能力等级、成长任务、徽章、预警
  abilitymap/                  毕业能力地图、授权、外部访问
  employer/                    用人单位、企业邀请、岗位、企业评价、就业反馈
  ai/                          AI Provider、Prompt 版本、AI Run、Schema 校验
  draftreview/                 草案、审核、发布、版本状态
  file/                        文件元数据、上传会话、安全扫描、签名 URL
  importdata/                  导入模板、预校验、提交导入、导入日志
  report/                      报告任务、模板版本、数据快照、文件输出
  notification/                站内通知、待办
  async/                       async_task、Worker、重试、心跳
  archive/                     归档与恢复
  audit/                       业务审计日志
  ops/                         健康检查、指标、运行监控
```

### 13.2 前端工程结构

```text
frontend/
  src/
    app/                       应用初始化、路由、权限菜单
    api/                       API Client，按领域拆分
    stores/                    Pinia 状态：auth、settings、task、notification
    modules/
      auth/
      organization/
      settings/
      dashboard/
      major/
      obe/
      curriculum/
      course/
      knowledge/
      student/
      evidence/
      ability-map/
      employer/
      report/
      audit/
      notification/
    components/
      data-table/
      support-matrix/
      course-map/
      upload/
      async-task-status/
      review-flow/
      file-preview/
      report-download/
    mobile/
      student-h5/
      public-ability-map/
```

前端必须统一使用接口文档中的英文枚举值，中文展示通过本地枚举字典实现。MVP 阶段采用 PRD 标准术语，不提供学校级术语配置。

---

## 14. 后端通用实现方案

### 14.1 通用响应与错误处理

关联接口规范：接口文档 2.3、2.6。  
关联需求：FR-001～FR-032、NFR-012。

实现要点：

1. 所有 API 返回统一响应结构：`success`、`data`、`error`、`requestId`、`timestamp`。
2. 所有业务异常映射为接口文档通用错误码或领域错误码。
3. 所有参数校验错误返回 `VALIDATION_ERROR`，并提供字段级错误详情。
4. 所有权限错误返回 `FORBIDDEN`，不得泄露资源是否存在的敏感信息。
5. 所有版本冲突返回 `VERSION_CONFLICT`。
6. 所有状态机错误返回 `BUSINESS_STATE_INVALID`。
7. 对外 API 不返回 Java 异常栈；运行日志记录 traceId 和异常详情。

建议错误分层：

| 层级 | 示例 | 处理方式 |
|---|---|---|
| 通用错误 | UNAUTHORIZED、FORBIDDEN、NOT_FOUND、VALIDATION_ERROR | 全局异常处理器统一处理。 |
| 并发错误 | VERSION_CONFLICT、IDEMPOTENCY_CONFLICT | 在服务层校验并返回明确错误。 |
| 领域错误 | AI_PROVIDER_DISABLED、FILE_SECURITY_SCAN_FAILED、ABILITY_MAP_AUTH_REVOKED | 领域模块定义错误枚举，统一映射。 |
| 依赖错误 | OBJECT_STORAGE_UNAVAILABLE、DEPENDENCY_UNAVAILABLE | 记录运行日志，可触发告警。 |

### 14.2 幂等与并发控制

关联接口规范：2.5、8.1、8.2。  
关联需求：FR-012、FR-013、FR-015、FR-025、FR-026、FR-028、FR-032。

实现要点：

| 机制 | 适用接口 | 实现说明 |
|---|---|---|
| Idempotency-Key | 创建学校、用户、专业、版本、AI 任务、报告任务、上传会话、导入提交、授权、归档恢复等 POST 接口 | 建表 `idempotency_record`，按 `school_id + user_id + idempotency_key + request_hash` 唯一。 |
| If-Match | PATCH / PUT 更新类接口 | 每个可编辑资源维护 `version` 整数或 ETag；更新时校验版本。 |
| 唯一约束 | 学号、课程代码、临时代码、专业代码等 | 数据库唯一约束 + 服务层友好错误。 |
| 状态机校验 | 草案、审核、发布、证据、授权、企业账号、任务 | 服务层集中校验可执行动作。 |

### 14.3 租户上下文

关联需求：NFR-014、FR-001、FR-002。  
实现要点：

1. 登录后生成 `TenantContext`，包含 `schoolId`、`userId`、`roles`、`scopes`。
2. 所有业务查询必须带 `school_id` 条件。
3. 后端禁止从前端请求体信任 `schoolId`；以认证上下文和资源归属为准。
4. 异步任务创建时写入 `school_id`，Worker 执行时恢复租户上下文。
5. 文件对象、报告、审计、AI Run 均必须记录 `school_id`。

---

## 15. 权限实现方案

### 15.1 权限模型

采用 `RBAC + Scope + ABAC`：

| 层级 | 作用 | 典型示例 |
|---|---|---|
| RBAC | 角色决定可执行动作 | SCHOOL_ADMIN 可创建学院；MAJOR_OWNER 可编辑本专业课程体系。 |
| Scope | 组织范围决定数据边界 | COLLEGE_MANAGER 只能管理授权学院；MENTOR 只能查看所指导学生。 |
| ABAC | 资源属性和业务状态决定是否允许操作 | 企业导师必须有学生授权且授权未过期；未审核证据不能计入能力。 |

### 15.2 权限拦截实现

1. API 层使用注解或拦截器声明所需权限动作，例如 `major:edit`、`evidence:review`、`ability_map:external_view`。
2. 服务层再次校验资源归属和业务状态，避免仅靠前端隐藏入口。
3. 查询层统一增加 `school_id` 过滤。
4. 学生、导师、教师、企业导师访问学生数据时必须走资源级授权判断。
5. 外部公开能力地图访问 `API-PUB-001` 不能直接暴露学生 ID；必须通过授权 token / accessToken 查询授权记录。

### 15.3 学生数据可见边界

| 数据 | 默认可见 | 对外展示 |
|---|---|---|
| 学生画像 | 学生本人、授权导师、授权教师 | 禁止默认对外展示。 |
| 成长预警 | 学生本人、导师、授权教师 | 禁止进入对外能力地图。 |
| 学习证据 | 学生本人、相关教师、导师 | 仅学生授权范围内展示。 |
| 能力地图 | 学生本人、导师、专业负责人 | 必须授权、有效期内、范围内展示。 |
| 申诉记录 | 学生本人、导师、分派处理人 | 禁止对外展示。 |

---

## 16. 数据库设计

### 16.1 通用字段规范

所有核心业务表建议包含：

| 字段 | 说明 |
|---|---|
| id | 主键，建议使用字符串 ID 或 UUID。 |
| school_id / tenant_id | 学校空间隔离字段，核心表必填。 |
| created_by / created_at | 创建人和创建时间。 |
| updated_by / updated_at | 更新人和更新时间。 |
| version | 乐观锁版本。 |
| status | 业务状态。 |
| archived_flag | 是否归档。 |
| deleted_flag | 逻辑删除标记，仅用于草案或非关键对象；关键对象不物理删除。 |

### 16.2 核心表设计总览

| 领域 | 表名 | 主要职责 | 关键字段 | 关联需求 |
|---|---|---|---|---|
| 组织权限 | schools | 学校空间 | school_id、school_name、school_type、status | FR-001 |
| 组织权限 | colleges | 学院 | college_id、school_id、college_name、status | FR-001, FR-003 |
| 组织权限 | users | 用户账号 | user_id、school_id、username、auth_source、status | FR-001 |
| 组织权限 | user_role_scopes | 用户角色与范围 | user_id、role_code、scope_type、scope_id | FR-001, FR-002 |
| 配置 | school_quality_rules | 质量阈值 | total_credits_min/max、practice_ratio_min、version | FR-011, FR-031 |
| 配置 | school_ai_settings | AI Provider 和数据策略 | providers_json、external_enabled、data_policy_json | FR-030, FR-031 |
| 配置 | school_ability_level_settings | 能力等级配置 | levels_json、version | FR-023, FR-031 |
| 配置 | course_code_policy | 临时代码策略 | temp_code_format、allow_publish_with_temp_code | FR-014, FR-031 |
| 专业 | majors | 专业档案 | major_id、college_id、major_code、degree_type、owner_user_id | FR-003 |
| 专业 | major_versions | 培养方案版本 | major_version_id、major_id、effective_grade、status | FR-003, FR-032 |
| 专业 | major_ai_contexts | 专业 AI 上下文 | major_id、context_json、context_version | FR-005, FR-030 |
| OBE | program_objectives | 培养目标 | objective_id、major_version_id、code、description | FR-007 |
| OBE | graduation_requirements | 毕业要求 | requirement_id、objective_id、code、description | FR-007 |
| OBE | ability_indicators | 能力指标点 | indicator_id、requirement_id、code、description | FR-007 |
| 课程 | courses | 平台课程 | course_id、course_code、course_name、course_module、course_nature、course_type | FR-014 |
| 课程 | course_versions | 专业课程版本 | course_version_id、course_id、major_id、course_objectives_json、status | FR-014, FR-017 |
| 课程体系 | curriculum_versions | 课程体系版本 | curriculum_version_id、major_version_id、status、version_name | FR-008, FR-032 |
| 课程体系 | curriculum_courses | 课程体系内课程 | curriculum_course_id、curriculum_version_id、course_version_id、semester、credits | FR-008, FR-009 |
| 课程体系 | course_relations | 课程关系 | from_course_id、to_course_id、relation_type | FR-008 |
| 课程体系 | support_matrix_items | 支撑矩阵 | curriculum_version_id、course_id、indicator_id、support_level、weight | FR-010 |
| 质量分析 | quality_analyses | 质量分析结果 | analysis_id、business_object_id、risk_json、suggestion_draft_id | FR-011, FR-030 |
| 草案审核 | drafts | AI / 人工草案 | draft_id、draft_type、content_json、ai_run_id、status | FR-012 |
| 草案审核 | human_reviews | 审核记录 | review_id、object_type、object_id、reviewer_id、decision | FR-012 |
| AI | ai_providers | Provider 配置元数据 | provider_id、provider_type、enabled、config_ref | FR-030, FR-031 |
| AI | ai_runs | AI 运行记录 | ai_run_id、task_type、provider_id、model_id、prompt_version、status | FR-030 |
| 异步 | async_tasks | 异步任务 | task_id、task_type、business_object_id、status、progress_percent | NFR-009 |
| 文件 | files | 文件元数据 | file_id、storage_key、file_hash、security_scan_status、business_object | FR-015, FR-021 |
| 知识库 | knowledge_documents | 知识库资料 | document_id、course_id、source_type、review_status、license_status | FR-015, FR-016 |
| 知识库 | knowledge_chunks | 知识片段 | chunk_id、document_id、content_text、tags、review_status | FR-016 |
| 知识库 | citation_records | 引用记录 | citation_id、document_id、chunk_id、license_snapshot_json | FR-016, FR-017 |
| 课程内容 | course_contents | 课程内容草案 / 版本 | content_id、course_version_id、content_type、status | FR-017 |
| 导入 | import_templates | 导入模板元数据 | template_type、template_version、file_id | FR-025 |
| 导入 | import_jobs | 导入任务 | import_id、import_type、template_version、status、result_json | FR-025 |
| 学生 | students | 学生档案 | student_id、student_no、major_id、grade、status | FR-018, FR-025 |
| 学生 | student_course_records | 课程修读记录 | student_id、course_code、semester、score、curriculum_version_id | FR-025 |
| 学生画像 | student_profiles | 学生画像 | student_id、profile_json、basis_json、status | FR-018 |
| 学生画像 | profile_appeals | 画像申诉 | appeal_id、student_id、appeal_type、status、handler_id | FR-019 |
| 培养路径 | development_paths | 个性化路径 | student_id、path_json、mentor_confirm_status | FR-020 |
| 证据 | student_evidences | 学习证据 | evidence_id、student_id、evidence_type、status、file_ids | FR-021 |
| 证据 | evidence_indicator_mappings | 证据能力点映射 | evidence_id、indicator_id、mapping_status | FR-021 |
| 证据 | evidence_reviews | 证据审核 | review_id、evidence_id、decision、reviewer_id | FR-022 |
| 能力 | ability_level_records | 能力等级记录 | student_id、indicator_id、level_code、evidence_ids | FR-022, FR-023 |
| 能力 | growth_tasks | 成长任务 | student_id、indicator_id、task_status | FR-023 |
| 能力 | growth_badges | 徽章 | student_id、indicator_id、level_code、evidence_id | FR-023 |
| 预警 | growth_warnings | 成长预警 | warning_id、student_id、warning_type、trigger_basis_json、status | FR-024 |
| 能力地图 | graduate_ability_maps | 能力地图 | ability_map_id、student_id、map_json、status、version | FR-026 |
| 能力地图 | ability_map_authorizations | 授权记录 | authorization_id、ability_map_id、display_scopes、valid_until、status | FR-027 |
| 能力地图 | ability_map_access_logs | 对外访问日志 | authorization_id、viewer_id/ip、accessed_scopes、accessed_at | FR-027 |
| 企业 | employers | 用人单位 | employer_id、name、credit_code、review_status | FR-028 |
| 企业 | employer_invitations | 企业邀请 | invitation_id、token_hash、status、expires_at | FR-028 |
| 企业 | job_roles | 岗位能力模型 | job_role_id、employer_id、job_name、ability_json | FR-028 |
| 企业 | job_ability_mappings | 岗位能力映射 | job_role_id、indicator_id、mapping_level | FR-028, FR-029 |
| 企业 | employer_evaluations | 实习 / 项目评价 | employer_id、student_id、evaluation_json、status | FR-028, FR-029 |
| 反馈 | employment_feedback | 就业反馈 | feedback_id、employer_id、major_id、feedback_json | FR-029 |
| 报告 | reports | 报告元数据 | report_id、report_type、template_version、data_snapshot_json、file_id | FR-013, FR-026, FR-030 |
| 通知 | notifications | 站内通知 | notification_id、recipient_user_id、type、read_status | FR-012 |
| 通知 | todos | 待办 | todo_id、assignee_user_id、business_object、status | FR-004, FR-012 |
| 归档 | archive_records | 归档记录 | archive_record_id、object_type、object_id、reason、restored_at | FR-032 |
| 审计 | audit_logs | 业务审计日志 | audit_log_id、actor_user_id、action、before_summary、after_summary | FR-002, NFR-012 |

### 16.3 关键约束与索引

| 表 | 约束 / 索引建议 |
|---|---|
| users | `school_id + username` 唯一；`auth_source + external_identity_id` 可选唯一。 |
| user_role_scopes | `user_id + role_code + scope_type + scope_id` 唯一。 |
| majors | `school_id + major_code` 可选唯一；`college_id + major_name` 建议唯一或提示冲突。 |
| courses | `school_id + course_code` 唯一；临时代码也纳入唯一约束。 |
| students | `school_id + student_no` 唯一。 |
| student_course_records | `student_id + course_code + semester + curriculum_version_id` 唯一或冲突检测。 |
| support_matrix_items | `curriculum_version_id + curriculum_course_id + indicator_id` 唯一。 |
| ability_level_records | `student_id + indicator_id + level_code + status` 建索引，保留变更历史。 |
| ability_map_authorizations | `authorization_id` 主索引；按 `student_id + status + valid_until` 建索引。 |
| knowledge_documents | 按 `school_id + course_id + review_status + trust_level` 建组合索引。 |
| knowledge_chunks | 按 `document_id`、`tags`、关键词字段建索引。 |
| async_tasks | 按 `status + priority + created_at` 建 Worker 拉取索引；按 `school_id + created_by` 建查询索引。 |
| audit_logs | 按时间分区；按 `school_id + actor_user_id`、`business_object_type + business_object_id`、`action + operation_time` 建索引。 |

---

## 17. 模块级实现方案

### 17.1 M-01 认证、组织、租户与权限

关联需求：FR-001、FR-002、FR-028、NFR-014。  
关联接口：API-AUTH-001～004、API-ORG-001～006、API-USER-001～006、API-ROLE-001。

实现内容：

1. 实现本地账号登录、刷新 Token、登出、当前用户信息。
2. 实现学校、学院、用户、角色、Scope 管理。
3. Token 中只保存必要身份信息；权限和 Scope 从数据库加载或短期缓存。
4. 用户停用后 Token 刷新失败，现有短期 access token 可通过版本号或状态校验强制失效。
5. 企业账号仍走邀请制，不进入学校 SSO。
6. 预留 `auth_source`、`external_identity_id` 以便后续接 SSO。

前端实现：

- 登录页、用户管理页、角色范围绑定页、权限菜单渲染。
- 根据 `/auth/me` 的 `permissions` 和 `scopes` 动态生成菜单与按钮权限。

测试点：

- 不同角色登录菜单不同。
- 越权 API 返回 FORBIDDEN。
- 停用账号无法继续登录。
- 企业导师无学生结论发布权。

风险与控制：

| 风险 | 控制措施 |
|---|---|
| 只做前端权限导致越权 | 后端服务层强校验资源 Scope。 |
| Token 中权限过期 | 增加用户权限版本或短 TTL。 |
| 企业账号误纳入校内权限 | 企业角色单独 Scope，不绑定学校管理权限。 |

### 17.2 M-02 学校级配置中心

关联需求：FR-011、FR-014、FR-023、FR-024、FR-030、FR-031、NFR-007、NFR-010。  
关联接口：API-CFG-001～012。

实现内容：

1. 质量阈值配置：学分、实践比例、支撑要求。
2. AI Provider / AI 策略配置：外部模型启用、学生数据出域、知识库资料出域、脱敏策略。
3. 能力等级配置：平台预置五级，学校可配置名称、描述、证据要求模板。
4. 成长预警规则配置：启用、等级、可见范围、通知、导出允许。
5. 课程临时代码策略配置。

前端实现：

- 学校设置页面分 Tab：质量规则、AI 策略、能力等级、成长预警、课程代码策略。

测试点：

- 配置更新需 If-Match。
- 新质量分析使用新阈值。
- 外部 Provider 默认关闭。
- 学生数据默认不得进入外部模型上下文。

风险：配置变更影响历史报告。控制：报告记录 `dataVersionSnapshot` 和配置版本，不重算历史报告。

### 17.3 M-03～M-05 多专业、看板与专业工作台

关联需求：FR-003、FR-004、FR-005。  
关联接口：API-DSH-001～003、API-MAJ-001～008。

实现内容：

1. 多学院、多专业建档。
2. 专业列表展示建设阶段、成熟度、课程体系完整度、风险、待审核数量。
3. MVP-1 至少支持 2 学院 3 专业，其中 1 个完整闭环。
4. 成熟度采用规则计算，AI 只解释风险和建议。
5. 专业工作台聚合 OBE、课程体系、课程地图、支撑矩阵、AI 质量分析、待办和版本。

成熟度计算建议：

| 指标 | 权重建议 | 数据来源 |
|---|---:|---|
| 专业基础信息完整 | 10% | majors |
| 专业画像已发布 | 10% | drafts / published profile |
| OBE 已发布 | 15% | program_objectives / requirements / indicators |
| 课程体系已发布 | 20% | curriculum_versions |
| 支撑矩阵已确认 | 15% | support_matrix_items |
| 四年课程地图完整 | 10% | curriculum_courses |
| 质量分析完成 | 10% | quality_analyses |
| 报告可导出 | 10% | reports |

测试点：看板数据与底层表一致；学院管理者仅看本学院。

### 17.4 M-06 AI 专业画像与 OBE

关联需求：FR-006、FR-007、FR-012、FR-030。  
关联接口：API-AI-001～002、API-OBE-001～003、API-DRF-001～003、API-REV-001～005。

实现内容：

1. AI 专业画像生成任务创建 `async_task` 和 `ai_run`。
2. Worker 调用 AIProviderAdapter，输出按 Schema 校验。
3. AI 输出写入 `drafts`，不直接写入正式 OBE 或专业画像。
4. OBE 使用内置结构：培养目标、毕业要求、能力指标点。
5. OBE 草案人工保存，发布后写入正式版本表或标记正式版本状态。
6. 已发布版本不可被 AI 新草案覆盖。

AI 输出 Schema 校验失败时：

- 任务状态 failed。
- `ai_runs.status = schema_invalid` 或 failed。
- 返回 `AI_OUTPUT_SCHEMA_INVALID`。
- 允许用户重新生成。

### 17.5 M-07 课程体系、课程地图、支撑矩阵与质量分析

关联需求：FR-008、FR-009、FR-010、FR-011、FR-012、FR-014、FR-032。  
关联接口：API-CUR-001～009、API-SUP-001～003、API-QA-001～002。

实现内容：

1. 创建课程体系版本草案，基于 major_version。
2. 课程体系内课程引用 course_version；支持学分、学时、理论 / 实践学时、建议学期。
3. 支持课程关系：prerequisite、recommended_before、not_same_semester。
4. 支撑矩阵支持 high / medium / low / none，权重固定为 1.0 / 0.6 / 0.3 / 0。
5. 课程地图按 8 学期聚合。
6. 学分学时统计按学校阈值计算风险。
7. 质量分析先规则后 AI：规则负责触发风险，AI 解释和建议进入草案。
8. 发布课程体系时，旧版本不覆盖，新版本替代。

异常处理：

- 课程关系循环或冲突返回 `VALIDATION_ERROR` 或风险提示。
- 带临时代码发布时按学校策略决定阻止或提示。
- 已发布课程体系不允许直接编辑，只能创建新版本。

### 17.6 M-08 课程库与专业课程版本

关联需求：FR-014、FR-017。  
关联接口：API-CRS-001～006。

实现内容：

1. 平台课程 `courses` 学校范围内课程代码唯一。
2. 未填写正式代码时，按临时代码规则生成 `TMP-{majorCode}-{sequence}` 或等价实现。
3. 专业课程版本 `course_versions` 可覆盖课程目标、案例、实践项目、考核方式。
4. 同一平台课程可被多个专业复用。
5. 替换临时代码时，保持课程体系、支撑矩阵、课程地图关联不变。

### 17.7 M-09 课程知识库

关联需求：FR-015、FR-016、FR-017、NFR-011。  
关联接口：API-FILE-001～006、API-KB-001～008。

实现内容：

1. 文件上传使用上传会话 + 确认上传 + 安全扫描状态。
2. 知识库资料支持 docx、pdf、xlsx、md、txt、手动 URL。
3. URL 仅用户手动提交，不做开放互联网搜索。
4. `knowledge_documents` 记录来源、作者、机构、发布时间、可信度、版权许可、使用范围、审核状态。
5. `knowledge_chunks` 默认继承文档版权许可。
6. AI 引用时写入 `citation_records`，包含文档、片段、审核和许可状态快照。
7. 检索采用结构化筛选 + 基础关键词匹配。
8. 未审核、禁用、禁止使用、扫描未通过资料不得用于正式课程内容生成。

### 17.8 M-10 课程内容生成

关联需求：FR-017、FR-012、FR-016。  
关联接口：API-CC-001～004、API-KB-007～008、API-DRF / API-REV。

实现内容：

1. 课程内容生成任务基于课程目标、能力指标点、可引用知识库资料。
2. AI 引用检索必须过滤审核状态和许可状态。
3. 生成结果作为课程内容草案，教师可编辑、拒绝、重生成。
4. 发布课程内容前必须人工审核。
5. 生成内容展示引用来源。

### 17.9 M-11 AI 编排与运行记录

关联需求：FR-006、FR-007、FR-011、FR-017、FR-018、FR-020、FR-022、FR-030、FR-031。  
关联接口：API-AI-001～004、API-CFG-003～004、API-CFG-011～012、API-MAJ-007～008。

实现内容：

1. AIProviderAdapter 抽象外部云模型和本地 / 私有模型配置位。
2. 外部 Provider 默认关闭，学校管理员启用。
3. Prompt 模板不对学校用户开放完整编辑；专业负责人可配置专业上下文。
4. 每次 AI Run 保存 Provider、Model、PromptVersion、SchemaVersion、InputSummary、Output、Citations、Status。
5. 学生数据默认不得进入外部模型上下文；如任务需要学生数据，按学校策略校验。
6. AI 输出必须 Schema 校验。

### 17.10 M-12 审核、发布与版本

关联需求：FR-012、FR-032。  
关联接口：API-DRF-001～003、API-REV-001～005、API-ARC-001～003。

实现内容：

1. 统一 Draft 模型承载 AI 草案和人工草案。
2. 统一 Review 模型承载提交审核、通过、驳回、需修改。
3. 发布时通过领域服务写入正式表或创建正式版本，并记录 `human_reviews`。
4. 已发布内容不可直接覆盖，只能新版本替代。
5. 归档 / 恢复通过 Archive 模块处理。

### 17.11 M-13 数据导入

关联需求：FR-025。  
关联接口：API-IMP-001～005、API-FILE-001～003。

实现内容：

1. 后端提供学生名单、课程修读记录 Excel / CSV 模板下载。
2. 模板带 templateVersion。
3. 导入前预校验，识别新增、重复、冲突、错误。
4. 用户选择跳过、更新或终止导入。
5. 高风险冲突不得静默覆盖：同学号不同姓名、培养方案版本变化等。
6. 导入日志和错误明细可查询 / 下载。

### 17.12 M-14 学生画像、申诉与培养路径

关联需求：FR-018、FR-019、FR-020、FR-025、NFR-010。  
关联接口：API-STU-001～004、API-PRF-001～002、API-APL-001～004、API-PATH-001～003。

实现内容：

1. 学生画像仅使用白名单数据。
2. 敏感黑名单数据不得导入画像、AI 推荐和成长预警。
3. 学生画像生成进入草案或画像版本，不直接对外展示。
4. 申诉采用导师初审 + 类型分派 + 最小状态流。
5. 培养路径推荐需要说明依据，导师可确认或调整。
6. 系统不得输出绝对化结论。

### 17.13 M-15 学习证据、能力升级、成长任务与预警

关联需求：FR-021、FR-022、FR-023、FR-024。  
关联接口：API-EVD-001～007、API-ABL-001～003、API-GRW-001～003、API-WRN-001～002。

实现内容：

1. 学习证据支持文件附件、课程、项目、实验、竞赛、实习、作品等类型。
2. 证据状态：submitted → parsed → pending_review → verified → counted / rejected / need_more_evidence。
3. 证据解析任务可生成能力点映射草案。
4. 未审核证据不得计入能力等级。
5. 能力升级由 AI 建议、教师 / 导师确认后生效。
6. 徽章必须绑定能力点、等级、已审核证据和审核记录。
7. 成长预警使用平台默认规则 + 学校配置 + 专业补充阈值；不得公开排名。

### 17.14 M-17 毕业能力地图与授权

关联需求：FR-026、FR-027。  
关联接口：API-MAP-001～006、API-AUTHZ-001～003、API-PUB-001～002。

实现内容：

1. 能力地图由异步任务生成草案。
2. 发布前导师或专业负责人审核。
3. 内部版可包含完整已审核能力、证据、评价和建议。
4. 对外版使用章节级 `displayScopes` + 固定禁止项。
5. 授权支持有效期、撤销、访问日志。
6. 对外 PDF 导出需要记录授权范围快照。
7. 撤销后不得生成新的对外导出文件；已生成历史报告按审计记录保留但访问应受当前授权策略控制。

### 17.15 M-18 用人单位与就业反馈

关联需求：FR-028、FR-029。  
关联接口：API-EMP-001～006、API-JOB-001～003、API-EVL-001、API-FDB-001～002。

实现内容：

1. 企业账号采用邀请制 + 学校 / 学院审核。
2. 企业基础信息、联系人信息、合作关系说明必填。
3. 证明材料支持 PDF、DOCX、JPG、JPEG、PNG，安全扫描通过后供人工审核。
4. 企业导师仅在授权范围内维护岗位能力模型、提交评价和反馈。
5. 就业反馈可触发改进建议任务，建议进入草案，不直接修改课程体系。

### 17.16 M-19 报告中心

关联需求：FR-013、FR-026、FR-030。  
关联接口：API-RPT-001～005、API-MAP-006。

实现内容：

1. 前端只选择报告类型和格式，不选择模板版本。
2. 后端使用当前平台模板版本。
3. 报告生成异步执行。
4. 保存 `templateVersion`、`dataVersionSnapshot`、`authorizationSnapshot`、`fileId`。
5. 历史报告下载默认下载已生成文件，不自动重算。
6. 专业建设报告、培养方案支持 Word / PDF；支撑矩阵、统计支持 Excel；能力地图支持 PDF。

### 17.17 M-20 文件服务与安全扫描

关联需求：FR-015、FR-021、FR-026、FR-028、NFR-009、NFR-012。  
关联接口：API-FILE-001～006。

实现内容：

1. `POST /files/upload-sessions` 创建上传会话。
2. 对象存储生成短期上传 URL。
3. 完成上传后触发安全扫描任务。
4. 扫描通过前文件不得解析、预览、下载、进入 AI 上下文或报告生成。
5. 下载 / 预览先经后端鉴权，再返回 5 分钟签名 URL。
6. 敏感场景可走后端代理。
7. 文件访问行为写审计日志。

### 17.18 M-21 异步任务

关联需求：NFR-009、FR-012、FR-015、FR-017、FR-025、FR-026、FR-030。  
关联接口：API-AI-003～004、API-NOT / API-TODO。

实现内容：

1. `async_tasks` 作为任务事实表。
2. Worker 按状态、优先级、创建时间拉取任务。
3. 状态：pending、queued、running、succeeded、failed、cancelled、retrying、archived。
4. 默认最多重试 3 次。
5. 不可重试错误直接 failed。
6. Worker 心跳进入监控。

### 17.19 M-22 通知与待办

关联需求：FR-004、FR-012、FR-028、NFR-009。  
关联接口：API-NOT-001～003、API-TODO-001。

实现内容：

1. 站内通知和待办中心为 MVP 基础能力。
2. 通知和任务状态采用轮询。
3. AI 完成、文件解析、报告生成、审核待办、企业邀请审核均生成通知或待办。
4. 邮件通知作为可选适配，不阻塞核心流程。

### 17.20 M-23 审计、归档与合规

关联需求：FR-002、FR-032、NFR-004、NFR-010、NFR-012。  
关联接口：API-AUD-001、API-ARC-001～003。

实现内容：

1. 审计日志追加式写入 PostgreSQL 分区表。
2. before_summary / after_summary 只存脱敏摘要和业务对象引用。
3. 关键操作必须审计：登录、授权、发布、审核、文件访问、报告导出、AI 运行、能力地图外部访问等。
4. 归档不物理删除，恢复需权限和审计。

### 17.21 M-24 可观测性与运维

关联需求：NFR-008、NFR-009、NFR-012、NFR-014。  
实现内容：

1. Spring Boot 健康检查和基础指标。
2. 结构化运行日志，带 requestId / traceId。
3. Worker 心跳、任务积压、AI Provider 失败率、文件扫描失败、报告失败监控。
4. 数据库每日备份、对象存储版本化 / 备份，30 天保留，上线前恢复演练。
5. 运行日志不得记录学生隐私、API Key、Provider Key、Prompt 原文、永久 URL。

---

## 18. 前端实现方案

### 18.1 页面清单

| MVP | 页面 / 模块 | 关联需求 | 关键接口 |
|---|---|---|---|
| MVP-1 | 登录、当前用户、权限菜单 | FR-001, FR-002 | API-AUTH-001～004 |
| MVP-1 | 学校、学院、用户、角色 | FR-001～FR-003 | API-ORG、API-USER、API-ROLE |
| MVP-1 | 学校 / 学院 / 专业看板 | FR-004, FR-005 | API-DSH-001～003 |
| MVP-1 | 专业档案、培养方案版本 | FR-003 | API-MAJ-001～006 |
| MVP-1 | 专业 AI 上下文、AI 专业画像 | FR-005, FR-006 | API-MAJ-007～008、API-AI-001 |
| MVP-1 | 草案区、审核发布 | FR-012 | API-DRF、API-REV |
| MVP-1 | OBE 编辑 | FR-007 | API-OBE |
| MVP-1 | 课程体系编辑、课程地图、支撑矩阵 | FR-008～FR-010 | API-CUR、API-SUP |
| MVP-1 | 质量分析、报告中心 | FR-011, FR-013, FR-030 | API-QA、API-RPT |
| MVP-2 | 课程库、课程版本 | FR-014 | API-CRS |
| MVP-2 | 知识库资料、文件上传、审核、检索 | FR-015, FR-016 | API-FILE、API-KB |
| MVP-2 | 课程内容生成与发布 | FR-017 | API-CC |
| MVP-3 | 学生列表、画像、课程修读记录 | FR-018, FR-025 | API-STU、API-PRF、API-IMP |
| MVP-3 | 学生申诉、培养路径 | FR-019, FR-020 | API-APL、API-PATH |
| MVP-3 | 学习证据、能力等级、成长任务、预警 | FR-021～FR-024 | API-EVD、API-ABL、API-GRW、API-WRN |
| MVP-4 | 能力地图、授权、外部展示 | FR-026, FR-027 | API-MAP、API-AUTHZ、API-PUB |
| MVP-4 | 企业邀请、岗位、评价、就业反馈 | FR-028, FR-029 | API-EMP、API-JOB、API-EVL、API-FDB |
| 横向 | 通知、待办、审计、归档 | FR-002, FR-012, FR-032 | API-NOT、API-TODO、API-AUD、API-ARC |

### 18.2 前端关键组件

| 组件 | 用途 | 实现要点 |
|---|---|---|
| PermissionGate | 按权限控制按钮和区域 | 仅做前端显示控制，后端仍强校验。 |
| AsyncTaskStatus | 轮询展示任务状态 | running 时 3～5 秒轮询，完成后停止。 |
| FileUploader | 上传会话、上传、确认、状态展示 | 支持大小、类型、进度、错误提示。 |
| SupportMatrix | 支撑矩阵编辑 | 支持高/中/低/无、批量保存、横向滚动。 |
| CourseMap | 8 学期课程地图 | 按学期分组，显示负载风险。 |
| ReviewFlowPanel | 草案、审核、发布状态 | 统一展示审核操作。 |
| DataImportWizard | 下载模板、上传、预校验、选择策略、提交导入 | 支持错误明细下载。 |
| AbilityMapScopeSelector | 授权范围选择 | 使用 displayScopes 枚举，固定禁止项不可选。 |
| ReportTaskPanel | 报告生成和下载 | 轮询任务状态，获取下载 URL。 |

### 18.3 前端错误处理

1. `VALIDATION_ERROR`：表单字段级展示。
2. `FORBIDDEN`：提示无权限并隐藏后续操作。
3. `VERSION_CONFLICT`：提示数据已更新，要求刷新。
4. `BUSINESS_STATE_INVALID`：提示当前状态不可操作。
5. `TASK_RESULT_NOT_READY`：继续轮询或提示等待。
6. `FILE_SECURITY_SCAN_PENDING / FAILED`：文件卡片展示状态，不允许预览 / 下载。
7. `AI_PROVIDER_DISABLED`：提示联系学校管理员启用 AI Provider。

---

## 19. AI 与异步任务实现细节

### 19.1 AI 任务处理流程

```text
用户触发 AI 任务
→ 后端校验权限、学校 AI 策略、数据出域策略
→ 创建 async_task
→ 创建 ai_run 记录，status=pending
→ Worker 拉取任务
→ 组装输入摘要和上下文
→ 调用 AIProviderAdapter
→ 校验输出 Schema
→ 写入 drafts / suggestions / analyses
→ 更新 ai_run 和 async_task
→ 生成通知 / 待办
```

### 19.2 AI 数据策略

| 场景 | 外部模型默认策略 |
|---|---|
| 专业建设资料 | 学校管理员允许后可进入外部模型上下文。 |
| 课程知识库资料 | 学校策略允许且资料许可允许时可进入外部模型上下文。 |
| 学生个人数据 | 默认禁止进入外部模型上下文。 |
| 敏感黑名单数据 | 禁止进入 AI 上下文。 |
| 未审核知识库资料 | 不得用于正式课程内容生成。 |

### 19.3 Worker 并发与重试

| 参数 | MVP 建议 |
|---|---|
| AI Worker 并发 | 默认 5，可配置。 |
| 文件解析 Worker 并发 | 默认 5，可配置。 |
| 任务轮询间隔 | 2～5 秒。 |
| 最大重试 | 默认 3 次。 |
| 可重试错误 | AI Provider 超时、临时网络错误、对象存储临时失败。 |
| 不可重试错误 | 权限不足、数据校验失败、文件类型不支持、扫描失败。 |

---

## 20. 文件、安全与隐私实现

### 20.1 文件上传状态流

```text
pending_upload
→ uploaded
→ pending_security_scan
→ scan_passed / scan_failed / scan_error
→ pending_parse
→ parsed / parse_failed
→ archived
```

### 20.2 文件限制

| 类型 | 限制 |
|---|---|
| 课程知识库 | 单文件 50 MB；单批 20 个；总大小 300 MB。 |
| 学习证据 | 单文件 50 MB；单条证据最多 10 个附件；总大小 200 MB。 |
| 企业证明材料 | PDF、DOCX、JPG、JPEG、PNG。 |
| 异步触发 | >10 MB、PDF >50 页、Excel >10,000 行、单批 >5 个、预计 >5 秒。 |

### 20.3 安全扫描

1. 开发 / 测试环境可用 Mock 扫描器。
2. 生产 / 试点环境必须接入实际扫描组件。
3. 扫描未通过不得预览、下载、解析、引用、进入 AI 上下文或报告生成。
4. 扫描结果记录扫描组件、时间、状态、错误信息。

### 20.4 隐私脱敏

1. 审计日志只保存字段级脱敏摘要。
2. 运行日志不得记录学生隐私、Prompt 原文、Provider Key、数据库密码。
3. AI 输入摘要要脱敏。
4. 对外能力地图只返回授权 displayScopes。

---

## 21. 性能实现方案

### 21.1 后端性能措施

| 场景 | 措施 |
|---|---|
| 列表查询 | 强制分页，默认 pageSize 20，最大 100。 |
| 支撑矩阵 | 按课程体系版本批量查询，必要时按课程 / 指标分页或虚拟滚动。 |
| 学生列表 | 按学院、专业、年级索引过滤。 |
| 知识库检索 | 结构化索引 + 基础关键词字段索引。 |
| 报告生成 | 异步任务，不阻塞请求。 |
| 文件解析 | 异步任务，状态轮询。 |
| AI 调用 | 异步任务，Provider 超时和重试。 |
| 看板 | 按需计算，后续可引入缓存或物化统计；MVP 先规则聚合。 |

### 21.2 前端性能措施

1. 大表格使用分页、横向滚动或虚拟滚动。
2. 支撑矩阵避免一次性渲染过大 DOM。
3. 看板数据聚合由后端返回，前端不做大规模本地计算。
4. 文件上传显示进度，不阻塞页面。
5. 任务状态只对当前页面相关任务短轮询。

### 21.3 验收性能基线

按验收清单 v0.2：

- 普通页面主要内容加载 ≤ 3 秒。
- 中型数据页面响应 ≤ 5 秒。
- 20 个并发在线用户无 5xx。
- 5 个并发 AI 任务和 5 个并发文件解析 / 报告任务可查询状态。

---

## 22. 测试方案

### 22.1 测试分层

| 测试类型 | 覆盖内容 |
|---|---|
| 单元测试 | 状态机、权限判断、枚举校验、质量规则、支撑矩阵权重、导入校验。 |
| 集成测试 | API + DB + 文件服务 Mock + AI Provider Mock + Worker。 |
| 接口契约测试 | 接口响应结构、错误码、分页、排序、过滤、幂等、If-Match。 |
| 权限测试 | RBAC + Scope + ABAC；学生授权；企业访问。 |
| 流程测试 | AI 草案审核发布、课程体系发布、证据审核、能力地图授权。 |
| 异常测试 | 文件超限、扫描失败、Provider 禁用、重复导入、授权过期、版本冲突。 |
| 性能测试 | 验收数据基线下页面和接口响应。 |
| 兼容性测试 | Chrome / Edge；iOS Safari / Android Chrome。 |

### 22.2 必测红线

1. AI 不得直接发布正式数据。
2. 未授权企业不得访问学生隐私。
3. 未审核证据不得计入能力等级。
4. 未审核 / 禁用 / 禁止使用知识库资料不得用于正式课程内容发布。
5. 已发布版本不得被覆盖或物理删除。
6. 对外能力地图必须校验授权范围和有效期。
7. 关键操作必须生成审计日志。

### 22.3 Mock 与测试替身

| 组件 | 测试替身 |
|---|---|
| AI Provider | MockAIProvider：成功、失败、超时、Schema 错误。 |
| 文件安全扫描 | MockFileSecurityScanner：通过、失败、异常。 |
| 对象存储 | 本地 S3 Mock 或 MinIO。 |
| 邮件服务 | MockEmailNotificationAdapter。 |
| 报告渲染 | SimpleReportRenderer：生成可打开的测试文件。 |

---

## 23. 发布与回滚方案

### 23.1 环境

| 环境 | 用途 |
|---|---|
| dev | 开发联调，使用 Mock AI / Mock 扫描。 |
| test | QA 测试，使用验收数据基线。 |
| staging | 试点预发布，接近生产配置。 |
| production | 平台托管单学校独立租户。 |

### 23.2 发布步骤

1. 数据库迁移脚本先在 test / staging 执行。
2. 部署后端主服务和 Worker。
3. 部署前端静态资源。
4. 配置对象存储、AI Provider、文件扫描、邮件可选项。
5. 初始化学校空间、角色、默认枚举、学校设置、报告模板和导入模板。
6. 执行冒烟测试：登录、创建专业、创建 AI 任务、上传文件、生成报告、审计日志。
7. 正式切换生产访问。

### 23.3 回滚策略

| 变更类型 | 回滚方式 |
|---|---|
| 前端发布 | 保留上一个构建版本，静态资源回滚。 |
| 后端服务 | 镜像版本回滚；注意兼容数据库迁移。 |
| 数据库迁移 | 只允许向前兼容迁移；破坏性迁移必须拆分为新增字段、数据回填、切换、清理四步。 |
| 配置变更 | 学校设置保留版本，支持恢复上一个版本。 |
| 报告模板 | 模板版本化；历史报告不重算。 |
| AI Provider 配置 | 学校管理员可禁用 Provider；失败任务保留可重试。 |
| 对象存储文件 | 启用版本化 / 备份，文件元数据归档不物理删除。 |

### 23.4 数据迁移原则

1. 不做物理删除。
2. 新字段先允许为空，再逐步回填，再加约束。
3. 枚举扩展向后兼容。
4. 重要状态变更需要审计。
5. 迁移脚本必须可在 staging 验证。

---

## 24. 主要风险与应对

| 风险 | 影响 | 应对 |
|---|---|---|
| 权限边界复杂 | 学生隐私泄露、越权访问 | RBAC + Scope + ABAC；后端强校验；权限测试覆盖。 |
| AI 输出不稳定 | 草案质量差、Schema 不一致 | AI 输出 Schema 校验；失败可重试；人工审核发布。 |
| 支撑矩阵数据量大 | 前端卡顿、接口慢 | 分页 / 虚拟滚动；必要索引；只按版本加载。 |
| 报告 PDF 中文渲染不稳定 | 导出失败或格式错乱 | 报告引擎预研；中文字体包；样例覆盖支撑矩阵和能力地图。 |
| 文件安全风险 | 恶意文件进入解析或下载 | 文件扫描适配层；扫描前禁止后续流程。 |
| 导入数据质量差 | 学生档案和课程记录污染 | 预校验 + 用户选择策略 + 导入日志。 |
| 审计日志膨胀 | 查询变慢、存储增长 | 分区表、索引、后续冷归档。 |
| AI Provider 不可用 | AI 任务失败 | Provider 禁用提示、任务失败重试、Mock 测试。 |
| 后续私有化要求 | 云依赖迁移成本 | S3 兼容、Adapter 抽象、不绑定云厂商。 |

---

## 25. 开发任务拆解表

> 说明：接口编号按《接口文档 v0.2》；测试点可映射到验收清单具体项或验收类型；风险为任务级主要风险。

| 任务ID | 阶段 | 模块 | 实现任务 | 关联需求 | 关联接口 | 数据表 / 模块 | 主要测试点 | 主要风险 |
|---|---|---|---|---|---|---|---|---|
| DEV-001 | 基础 | Common | 通用响应、错误码、分页、排序、过滤、请求 ID | 全部 FR, NFR-012 | 全部 API 通用规范 | common | 统一响应、错误码、分页边界、非法排序字段 | 错误码不统一导致前端难处理 |
| DEV-002 | 基础 | Common | 幂等键与 If-Match 乐观锁框架 | FR-012, FR-013, FR-025, FR-032 | 所有需 Idempotency-Key / If-Match 的 API | idempotency_record、version 字段 | 重复提交返回同一结果；版本冲突返回 VERSION_CONFLICT | 幂等记录粒度错误导致误复用 |
| DEV-003 | 基础 | Auth | 本地登录、Token、刷新、退出、当前用户 | FR-001, FR-002 | API-AUTH-001～004 | users、user_role_scopes | 登录、刷新、停用后不可用、菜单权限 | Token 失效策略不严 |
| DEV-004 | MVP-1 | Organization | 学校、学院管理 | FR-001, FR-003 | API-ORG-001～006 | schools、colleges | 创建学校/学院、学校隔离、学院列表权限 | school_id 漏过滤 |
| DEV-005 | MVP-1 | User | 用户、角色、Scope 管理 | FR-001, FR-002 | API-USER-001～006、API-ROLE-001 | users、user_role_scopes | 角色绑定、范围绑定、停用、重置密码 | 角色范围模型不清导致越权 |
| DEV-006 | MVP-1 | Permission | 后端权限拦截器和资源级 Scope 校验 | FR-002, NFR-010 | 全部受保护 API | auth/permission 模块 | 学院/专业/学生/企业越权拦截 | 仅前端控权限造成漏洞 |
| DEV-007 | MVP-1 | Settings | 学校级质量规则、课程代码策略 | FR-011, FR-014, FR-031 | API-CFG-001～002、API-CFG-009～010 | school_quality_rules、course_code_policy | 阈值生效、临时代码发布策略 | 配置影响历史报告 |
| DEV-008 | MVP-1 | Settings | AI 策略、能力等级、成长预警配置 | FR-023, FR-024, FR-030, FR-031 | API-CFG-003～008、API-CFG-011～012 | school_ai_settings、school_ability_level_settings | 外部 Provider 默认关闭、学生数据禁出域 | AI 策略绕过风险 |
| DEV-009 | MVP-1 | Dashboard | 学校、学院、专业看板指标接口 | FR-004, FR-005 | API-DSH-001～003 | dashboard 服务、majors、curriculum_versions | 指标一致性、权限范围、性能 | 指标实时计算慢 |
| DEV-010 | MVP-1 | Major | 专业档案、专业列表、培养方案版本 | FR-003, FR-005, FR-032 | API-MAJ-001～006 | majors、major_versions | 多专业、版本不覆盖、归档 | 版本链断裂 |
| DEV-011 | MVP-1 | Major AI Context | 专业 AI 上下文维护 | FR-005, FR-030 | API-MAJ-007～008 | major_ai_contexts | 上下文版本、If-Match、权限 | 专业上下文绕过学校策略 |
| DEV-012 | MVP-1 | Async | async_task 表和 Java Worker 轮询 | NFR-009, FR-012, FR-030 | API-AI-003～004 | async_tasks、Worker | 状态流、取消、重试、并发 5 任务 | Worker 卡死无告警 |
| DEV-013 | MVP-1 | AI | AIProviderAdapter、Provider 配置、AI Run | FR-006, FR-030, FR-031 | API-CFG-003～004、API-AI-001～004 | ai_providers、ai_runs | Provider 禁用、模型记录、Schema 错误 | 模型返回不稳定 |
| DEV-014 | MVP-1 | Draft Review | 草案区、编辑、提交审核、审核发布 | FR-012, FR-032 | API-DRF-001～003、API-REV-001～005 | drafts、human_reviews | 状态流、不能跳过审核、发布审计 | 不同领域发布逻辑分散 |
| DEV-015 | MVP-1 | AI Profile | AI 专业画像任务与草案 | FR-006, FR-012, FR-030 | API-AI-001、API-DRF、API-REV | ai_runs、drafts、major_versions | 结构化输出、依据说明、已发布不覆盖 | AI 结果字段不稳定 |
| DEV-016 | MVP-1 | OBE | OBE 获取、保存草案、AI 生成任务 | FR-007, FR-012 | API-OBE-001～003 | program_objectives、graduation_requirements、ability_indicators | 层级关系、草案发布、版本影响 | OBE 改动影响课程体系提示不足 |
| DEV-017 | MVP-1 | Course | 平台课程库、专业课程版本、临时代码 | FR-014 | API-CRS-001～006 | courses、course_versions | 课程代码唯一、临时代码替换关联不丢 | 课程代码策略不一致 |
| DEV-018 | MVP-1 | Curriculum | 课程体系草案、元数据、课程清单 | FR-008, FR-032 | API-CUR-001～006 | curriculum_versions、curriculum_courses | 草案编辑、新版本、归档课程 | 发布后误编辑 |
| DEV-019 | MVP-1 | Curriculum Relation | 课程关系维护和冲突检测 | FR-008 | API-CUR-007 | course_relations | 先修关系、不可同学期、冲突提示 | 循环检测遗漏 |
| DEV-020 | MVP-1 | Course Map | 四年课程地图和统计 | FR-009, FR-011 | API-CUR-008～009 | curriculum_courses、school_quality_rules | 8 学期展示、统计准确、负载风险 | 大课程量页面慢 |
| DEV-021 | MVP-1 | Support Matrix | 支撑矩阵获取、保存、AI 草案 | FR-010, FR-012 | API-SUP-001～003 | support_matrix_items、drafts | 高/中/低/无权重、人工确认 | 矩阵批量保存冲突 |
| DEV-022 | MVP-1 | Quality Analysis | 课程体系规则质量分析 + AI 建议 | FR-011, FR-030 | API-QA-001～002 | quality_analyses、drafts | 风险依据、建议草案、阈值版本 | AI 建议误成正式调整 |
| DEV-023 | MVP-1 | Report | 专业报告、培养方案、Excel 导出任务 | FR-013, FR-030 | API-RPT-001～005 | reports、files、async_tasks | 模板版本、数据快照、文件可打开 | 中文 PDF 渲染失败 |
| DEV-024 | MVP-1 | Archive | 归档与恢复通用能力 | FR-032 | API-ARC-001～003 | archive_records、各业务表 archived_flag | 归档不物理删除、恢复审计 | 恢复破坏当前版本 |
| DEV-025 | MVP-1 | Audit | 业务审计日志写入与查询 | FR-002, FR-032, NFR-012 | API-AUD-001 | audit_logs | 关键操作日志、脱敏摘要、查询权限 | 日志包含敏感原文 |
| DEV-026 | MVP-1 | Frontend Shell | Vue 3 应用框架、路由、权限菜单 | FR-001～FR-005 | API-AUTH、API-DSH、API-MAJ | frontend app | 权限菜单、Chrome/Edge 兼容 | 前端权限与后端不一致 |
| DEV-027 | MVP-1 | Frontend Curriculum | 课程体系、课程地图、支撑矩阵组件 | FR-008～FR-010 | API-CUR、API-SUP | frontend curriculum | 大表格渲染、矩阵保存、风险显示 | UI 性能不足 |
| DEV-028 | MVP-2 | File | 上传会话、确认上传、签名 URL、文件元数据 | FR-015, FR-021, FR-027 | API-FILE-001～005 | files、FileStorageAdapter | 50MB 限制、5 分钟 URL、权限下载 | 签名 URL 泄漏 |
| DEV-029 | MVP-2 | File Security | 文件安全扫描适配层与重扫 | FR-015, NFR-012 | API-FILE-006 | files、FileSecurityScannerAdapter | 扫描 pending/failed 阻止解析下载 | 扫描器不可用阻塞生产 |
| DEV-030 | MVP-2 | Knowledge | 知识库文档入库、URL 手动提交 | FR-015 | API-KB-001～003 | knowledge_documents、files | 支持格式、URL 待审核、不开放搜索 | URL 抓取安全边界 |
| DEV-031 | MVP-2 | Knowledge Parse | 文档解析任务、摘要、标签、片段 | FR-015, NFR-009 | API-KB-006、API-AI-003 | knowledge_chunks、async_tasks | 大文件异步、解析失败可重试 | 中文解析质量不稳定 |
| DEV-032 | MVP-2 | Knowledge Governance | 可信度、版权许可、审核、禁用 | FR-016, NFR-011 | API-KB-004～005 | knowledge_documents、human_reviews | 未审核不得正式引用、禁用生效 | 许可状态判断复杂 |
| DEV-033 | MVP-2 | Knowledge Search | 结构化筛选 + 基础中文关键词检索 | FR-016 | API-KB-002、API-KB-007 | knowledge_documents、knowledge_chunks | q 检索、过滤、排序、权限 | 检索结果相关性有限 |
| DEV-034 | MVP-2 | Citation | CitationRecord 生成与查询 | FR-016, FR-017 | API-KB-008 | citation_records | 引用快照、许可快照、追溯 | 引用粒度不一致 |
| DEV-035 | MVP-2 | Course Content | 课程内容生成、编辑、发布 | FR-017, FR-012 | API-CC-001～004 | course_contents、drafts、citation_records | 引用来源、教师审核、发布版本 | AI 引用未审核资料 |
| DEV-036 | MVP-2 | Frontend Knowledge | 知识库列表、上传、审核、检索、引用展示 | FR-015～FR-017 | API-FILE、API-KB、API-CC | frontend knowledge | 上传状态、扫描状态、检索过滤 | 文件状态展示不清 |
| DEV-037 | MVP-3 | Import Template | 后端导入模板下载 | FR-025 | API-IMP-001～002 | import_templates、files | 模板版本、字段说明、敏感字段不出现 | 模板与校验规则不同步 |
| DEV-038 | MVP-3 | Import Flow | 学生名单和课程修读记录预校验与提交 | FR-025 | API-IMP-003～005 | import_jobs、students、student_course_records | 重复/冲突/错误明细、导入日志 | 高风险冲突被覆盖 |
| DEV-039 | MVP-3 | Student | 学生列表、详情、基础信息、课程记录 | FR-018, FR-025 | API-STU-001～004 | students、student_course_records | 学号唯一、课程记录关联版本 | 成绩被误当正式成绩系统 |
| DEV-040 | MVP-3 | Student Profile | 学生画像查看、生成任务 | FR-018, FR-020 | API-PRF-001～002 | student_profiles、ai_runs | 白名单数据、依据说明、不可对外 | 敏感标签进入画像 |
| DEV-041 | MVP-3 | Appeal | 画像 / 建议 / 预警申诉与分派处理 | FR-019 | API-APL-001～004 | profile_appeals、todos | 导师初审、转教师/负责人、状态流 | 处理责任不清 |
| DEV-042 | MVP-3 | Development Path | 个性化培养路径生成与导师确认 | FR-020 | API-PATH-001～003 | development_paths、ai_runs | 推荐依据、导师调整、无绝对化结论 | AI 输出歧视性表达 |
| DEV-043 | MVP-3 | Evidence | 学习证据提交、列表、详情、文件绑定 | FR-021 | API-EVD-001～003、API-FILE | student_evidences、files | 附件限制、证据状态、权限 | 文件权限泄露 |
| DEV-044 | MVP-3 | Evidence Parse | 证据解析任务和能力点映射草案 | FR-021 | API-EVD-004～005 | evidence_indicator_mappings、ai_runs | 映射可编辑、未审核不计入 | 错误映射影响能力 |
| DEV-045 | MVP-3 | Evidence Review | 证据提交审核与审核决定 | FR-022 | API-EVD-006～007 | evidence_reviews、student_evidences | verified / rejected / need_more_evidence | 审核人权限不准 |
| DEV-046 | MVP-3 | Ability | 能力等级、升级建议、确认升级 | FR-022, FR-023 | API-ABL-001～003 | ability_level_records、ability_upgrade_suggestions | 已审核证据才升级、人工确认 | 无证据升级 |
| DEV-047 | MVP-3 | Growth | 成长任务、徽章、成长预警 | FR-023, FR-024 | API-GRW-001～003、API-WRN-001～002 | growth_tasks、growth_badges、growth_warnings | 徽章证据追溯、预警可见范围 | 预警被公开展示 |
| DEV-048 | MVP-3 | Frontend Student H5 | 学生端响应式页面 | FR-018～FR-024 | API-STU、API-PRF、API-EVD、API-GRW | frontend mobile/student-h5 | iOS Safari/Android Chrome、证据提交 | 移动端上传体验差 |
| DEV-049 | MVP-4 | Ability Map | 能力地图生成、详情、审核、PDF 导出 | FR-026 | API-MAP-001～006 | graduate_ability_maps、reports、files | 审核前不对外、PDF 导出 | 草案能力结论外泄 |
| DEV-050 | MVP-4 | Ability Auth | 能力地图授权、撤销、公开访问、访问日志 | FR-027 | API-AUTHZ-001～003、API-PUB-001～002 | ability_map_authorizations、ability_map_access_logs | displayScopes、过期/撤销、访问审计 | 授权范围裁剪不完整 |
| DEV-051 | MVP-4 | Employer Invite | 企业邀请、信息提交、审核 | FR-028 | API-EMP-001～004 | employers、employer_invitations、files | 必填信息、材料白名单、审核状态 | 邀请 token 泄漏 |
| DEV-052 | MVP-4 | Employer Profile | 用人单位列表和档案维护 | FR-028 | API-EMP-005～006 | employers | 企业权限、档案更新、停用 | 企业范围越权 |
| DEV-053 | MVP-4 | Job Role | 岗位能力模型与专业能力映射 | FR-028, FR-029 | API-JOB-001～003 | job_roles、job_ability_mappings | 岗位能力映射到指标点 | 映射口径不统一 |
| DEV-054 | MVP-4 | Evaluation Feedback | 实习 / 项目评价、就业反馈、改进建议任务 | FR-028, FR-029, FR-030 | API-EVL-001、API-FDB-001～002 | employer_evaluations、employment_feedback、quality_analyses | 反馈生成建议草案，不直接改课程体系 | 企业评价进入未授权能力地图 |
| DEV-055 | 横向 | Notification | 站内通知、未读数量、待办、标记已读 | FR-004, FR-012, NFR-009 | API-NOT-001～003、API-TODO-001 | notifications、todos | 轮询、未读数、任务完成通知 | 通知积压或重复 |
| DEV-056 | 横向 | Report Renderer | ReportRendererAdapter、模板版本、数据快照 | FR-013, FR-026, FR-030 | API-RPT-001～005 | reports、files、async_tasks | 历史报告不重算、中文可显示 | PDF 引擎兼容性 |
| DEV-057 | 横向 | Observability | 健康检查、结构化日志、指标、告警 | NFR-008, NFR-009, NFR-012 | 运维接口 / 健康检查 | ops 模块、async_tasks | Worker 心跳、5xx、Provider 失败 | 日志记录敏感明文 |
| DEV-058 | 横向 | Backup | 数据库每日备份、对象存储版本化、恢复演练 | NFR-014, NFR-012 | 无对外业务 API | PostgreSQL、对象存储 | 30 天保留、恢复演练、文件引用一致 | 只恢复 DB 不恢复文件 |
| DEV-059 | 横向 | Frontend Report & Notification | 报告中心、通知待办、任务轮询组件 | FR-013, FR-030, FR-012 | API-RPT、API-NOT、API-TODO、API-AI-003 | frontend report/notification | 任务轮询、下载 URL、未读角标 | 过度轮询造成压力 |
| DEV-060 | 横向 | Test Automation | 接口契约、权限、状态流、验收红线自动化测试 | 全部 FR/NFR | 全部核心 API | test suites | P0 红线、幂等、越权、状态机 | 测试数据维护成本 |

---

## 26. 需求、接口、数据表覆盖摘要

| 需求范围 | 覆盖模块 | 关键任务 |
|---|---|---|
| FR-001～FR-005 | Auth、Organization、Dashboard、Major | DEV-003～011、DEV-026 |
| FR-006～FR-013 | AI、OBE、Curriculum、SupportMatrix、Report | DEV-013～023、DEV-027、DEV-056 |
| FR-014～FR-017 | Course、File、Knowledge、CourseContent | DEV-017、DEV-028～036 |
| FR-018～FR-025 | Import、Student、Profile、Evidence、Ability、Growth | DEV-037～048 |
| FR-026～FR-029 | AbilityMap、Employer、Feedback | DEV-049～054 |
| FR-030～FR-032 | AI Quality、Settings、Archive、Audit、Report | DEV-007～008、DEV-013、DEV-022～025、DEV-056 |
| NFR-001～NFR-015 | Common、Security、Performance、Observability、Backup | DEV-001～002、DEV-006、DEV-025、DEV-057～060 |

---

## 27. 版本结论

本《详细技术方案 v0.3》将需求、架构、接口和验收基线转化为可执行实现方案。当前方案不新增产品范围，技术实现遵循已确认的架构决策：Vue 3 + TypeScript、Java 21 + Spring Boot 3.x、PostgreSQL、S3 兼容对象存储、PostgreSQL 任务表 + Java Worker、AI Provider Adapter、后端模板化报告、追加式审计日志和基础可观测性。

建议下一步进入：

1. 数据库详细 DDL 与迁移脚本设计。
2. 后端模块包结构与接口实现排期。
3. 前端页面原型到组件拆解。
4. AI Prompt 模板与输出 Schema 技术细化。
5. 报告渲染引擎技术预研。
6. Sprint 计划与测试用例实现。


---

## 28. v0.4 主要变更、确认结论与正式同步修订

### 28.1 与 v0.2 相比的主要变更清单

| 编号 | 变更 | 影响 |
|---|---|---|
| CHG-001 | 新增“开源优先技术选型原则”章节 | 明确第三方组件和闭源服务准入原则。 |
| CHG-002 | 新增“关键技术组件选型表” | 将所有关键组件按开源、商业、闭源 SaaS、专有云、自研、待确认分类。 |
| CHG-003 | 新增“开源依赖清单” | 区分默认允许、需确认和不建议默认采用的依赖。 |
| CHG-004 | 新增“非开源方案例外说明” | 对外部 AI、云对象存储、云扫描、商业邮件等补充保留理由和替换路径。 |
| CHG-005 | 新增“许可证与合规风险” | 对 MinIO、ClamAV、Grafana、iText、Redis、Elastic 等标记风险。 |
| CHG-006 | 新增“供应商锁定风险与替换路径” | 强化 AIProviderAdapter、FileStorageAdapter、ReportRendererAdapter 等抽象层。 |
| CHG-007 | 新增“依赖安全与版本管理策略” | 加入 SBOM、许可证扫描、漏洞扫描、Mock 禁用和生产发布准入。 |
| CHG-008 | 新增 DEV-061～DEV-070 | 补充开源治理、CI 扫描、Adapter 检查、报告引擎 Spike、发布准入等任务。 |
| CHG-009 | 新增 AC-OSG-001～AC-OSG-014 正式验收要求 | 已纳入验收清单终版 v1.2 和自动开发基线。 |
| CHG-010 | 原有方案主体保持不变 | 不改变 FR、NFR、MVP、接口和架构决策。 |
| CHG-011 | 回填 OSG-DEC-001～OSG-DEC-008 | 将开源治理确认项固化为技术方案约束。 |
| CHG-012 | 新增生产 / 试点发布准入门禁 RG-OSG-001～RG-OSG-010 | 明确 SBOM、许可证、漏洞、镜像扫描、审批、Mock 禁用和 Adapter 边界阻断规则。 |
| CHG-013 | 明确外部 AI Provider 生产启用边界 | MVP-1 / MVP-2 可审批启用；学生相关 AI 外部调用默认关闭。 |
| CHG-014 | 明确对象存储和私有化对象存储策略 | 平台托管可用云对象存储；私有化优先学校已有 S3 兼容服务，不默认强制 MinIO。 |
| CHG-015 | 明确报告、扫描、监控工具的开源优先默认方案 | iText / 商业 PDF SDK、云扫描、Grafana 等均需审批。 |

### 28.2 实施前技术确认项的处理结果

以下 v0.2 中的实施前确认项已全部确认，并转化为 OSG-DEC-001～OSG-DEC-008：

| 原编号 | v0.3 决策编号 | 确认结果 |
|---|---|---|
| OSG-CONF-001 | OSG-DEC-001 | MVP-1 / MVP-2 可审批启用外部 AI Provider；学生相关 AI 外部调用默认关闭。 |
| OSG-CONF-002 | OSG-DEC-002 | 平台托管可使用云对象存储，但必须通过 FileStorageAdapter 并记录审批和迁移路径。 |
| OSG-CONF-003 | OSG-DEC-005 | 私有化不默认强制 MinIO，优先学校已有 S3 兼容对象存储。 |
| OSG-CONF-004 | OSG-DEC-003 | 文件安全扫描优先 ClamAV 独立服务，云扫描 / 学校组件需审批，生产禁止 Mock。 |
| OSG-CONF-005 | OSG-DEC-004 | 报告生成优先开源组合方案，iText / 商业 PDF SDK 需审批。 |
| OSG-CONF-006 | OSG-DEC-006 | MVP 不默认 Grafana；如需 Prometheus + Grafana 看板，Grafana 需负责人 / 法务确认。 |
| OSG-CONF-007 | OSG-DEC-007 | 生产 / 试点发布前必须生成 SBOM、许可证扫描、漏洞扫描和容器镜像扫描结果。 |
| OSG-CONF-008 | OSG-DEC-008 | 开源治理要求同步进入验收清单、测试计划和发布准入。 |

当前无阻塞性待确认问题。后续仍需在 DevSecOps、部署方案和测试计划中细化具体工具、审批责任人和执行流程。

### 28.3 建议同步修改的上游或下游文档

| 文档 | 修改建议 |
|---|---|
| 产品需求说明书 | 在非功能需求中新增“开源优先与供应商锁定治理”原则，不改变业务范围。 |
| 系统架构设计 | 新增 ADR：开源优先原则；补充第三方依赖分类、Adapter 红线、非开源例外审批。 |
| 接口文档 | 不需大规模修改；可在 AI、文件、报告、通知接口说明中强调不暴露具体厂商实现。 |
| 验收清单 | 新增 AC-OSG-001～AC-OSG-010，并将其作为发布前 P0 / P1 验收项。 |
| 测试计划 | 增加许可证扫描、SBOM、漏洞扫描、容器镜像扫描、Mock 禁用、Adapter 边界检查、外部服务关闭测试、报告引擎 Spike。 |
| 发布计划 | 增加生产发布准入：依赖清单、许可证审查、漏洞处理、例外审批、备份和回滚确认。 |

### 28.4 v0.3 版本结论

本版本在不改变产品需求、不推翻系统架构、不重新定义接口契约的前提下，将“开源优先原则”进一步固化为开发、测试、验收和发布准入的可执行约束。

进入自动开发前，必须完成以下基线准备工作：

1. 报告渲染引擎中文样例 Spike；
2. ClamAV 独立服务集成方式和许可证确认；
3. 外部 AI Provider 启用审批表和数据出域说明；
4. FileStorageAdapter 云对象存储配置与迁移说明；
5. SBOM / SCA / 镜像扫描工具链落地；
6. 验收清单终版 v1.2 已纳入 AC-OSG-001～AC-OSG-014；
7. 发布准入清单新增 RG-OSG-001～RG-OSG-010。


---

# 附录 D：v0.4 一致性修订基线

## D.1 修订目的

本补丁基于一致性审查 6 项必须修复决策形成，用于将《详细技术方案 v0.3》修订为可作为自动开发输入的技术基线。该补丁不改变产品需求，不推翻原架构。

## D.2 新增上游追溯约束

| 编号 | 名称 | 说明 |
|---|---|---|
| NFR-016 | 开源优先与供应商锁定治理 | PRD 层非功能需求，用于承接开源优先、许可证合规、供应商锁定治理等项目级约束。 |
| ENG-OSG-001 | 开源治理工程约束 | 技术方案、验收清单、发布准入和开发任务共同追溯的工程约束。 |

统一追溯链：

```text
NFR-016 → ENG-OSG-001 → OSG-DEC-001～008 → DEV-061～070 → AC-OSG-001～014 → RG-OSG-001～010
```

## D.3 非功能需求编号语义修复

| 编号 | 修复后语义 |
|---|---|
| NFR-013 | 部署形态、单学校独立租户、数据隔离、对象存储、备份恢复、专有云 / 私有化迁移。 |
| NFR-014 | PC Web、学生端 H5、移动端浏览器和浏览器兼容性。 |
| NFR-016 / ENG-OSG-001 | 开源优先、许可证、供应商锁定、SBOM、SCA、漏洞扫描、发布准入。 |

开发任务中原误追溯到 NFR-014 的对象存储、备份恢复、开源治理类任务，应改追溯到 NFR-013 或 NFR-016 / ENG-OSG-001。

## D.4 模块边界修复：M-15 / M-16

| 模块 | 修复后职责 | 关联 DEV 任务 | 关联接口 |
|---|---|---|---|
| M-15 学习证据与能力升级模块 | 学习证据、证据解析、证据审核、能力指标点映射、能力等级、成长任务、徽章、里程碑 | DEV-043～DEV-047 中证据、能力、成长任务、徽章部分 | API-EVD-*、API-ABL-*、API-GRW-* |
| M-16 成长预警模块 | 预警规则、预警阈值、预警生成、预警处理、预警可见范围、学生申诉、导师待办、预警隐私控制 | DEV-047 中成长预警部分、DEV-041 申诉流相关部分 | API-WRN-*、API-APL-*、API-CFG-007～008 |

技术实现时，成长预警不得混入能力升级状态机；预警不得影响能力等级正式结论，不得进入对外能力地图。

## D.5 接口映射修复要求

新增《任务-接口精确映射表 v1.0》作为自动开发输入。详细技术方案任务表中的接口范围表达仅作为人工阅读摘要，自动开发应以精确映射表为准。

规则：

1. 每个核心 API 必须至少关联一个 DEV 任务；
2. 每个涉及接口实现的 DEV 任务必须列出具体 API 编号；
3. 禁止将“相关接口”“全部接口”“等”作为自动开发任务边界；
4. 接口验收失败时，必须能回溯到具体 DEV 任务、模块、数据表和验收项。

## D.6 权限实现修复要求

新增《权限判定矩阵 v1.0》作为权限实现的最高执行依据。若接口文档自然语言权限描述与权限判定矩阵不一致，以权限判定矩阵为准，并同步修订接口文档。

后端必须根据权限矩阵实现：

1. RBAC 角色判断；
2. Scope 组织范围判断；
3. ABAC 资源关系判断；
4. 学生授权范围判断；
5. 企业授权访问范围判断；
6. AI 任务数据出域策略判断；
7. 文件下载与报告导出权限判断。

## D.7 AC-OSG 范围修复

所有技术方案中提到 `AC-OSG-001～AC-OSG-010` 的地方，统一修订为：

```text
AC-OSG-001～AC-OSG-014
```

并统一追溯到：

```text
NFR-016 + ENG-OSG-001
```

## D.8 开发输入优先级规则

自动开发时文档优先级如下：

1. 开发输入基线索引 v1.0；
2. 产品需求说明书 v0.3；
3. 系统架构设计 v0.3；
4. 接口文档 v0.2；
5. 详细技术方案 v0.4；
6. 权限判定矩阵 v1.0；
7. 任务-接口精确映射表 v1.0；
8. 验收清单终版 v1.2。

如技术方案与接口文档冲突，以接口文档为接口契约源；如权限自然语言描述与权限矩阵冲突，以权限判定矩阵为准；如验收项与需求范围冲突，以 PRD 非目标和需求范围为准。
