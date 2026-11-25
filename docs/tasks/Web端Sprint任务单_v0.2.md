# Web 端 Sprint 任务单 / 协作文档 v0.2

> 生效日期：2026-11-25｜维护人：web-arch（Web 端架构师）
> 适用范围：中州养老 Web 综合端（`web-zzyl-preject/code/zzyl/zznursing-vue3-master` 前端 + `zznursing-master` 后端 + 本机 88/8080/3306/6379 共享环境）
> 关联文档：
> - 调度规则原文（web-control 发布）：`docs/tasks/Web调试调度规则.md` **v1.1** —— **以该文件为最终依据**；本文件 §2 为摘要，若冲突以规则原文为准
> - 调度日志（web-control 维护）：`docs/test/web_调试调度日志.md`
> - Sprint 5 详细任务单：`docs/tasks/Web_Sprint5_护理项目CRUD_任务单.md`
> - 架构/功能规格：`docs/architecture/`（见 §6 文档索引）
> 状态：**Sprint 5 已开工**——正式任务卡 Task-S5-01（护理项目 CRUD 后端）见 §5

---

## 1. 角色与协作链路

| 角色 | Agent | 职责 |
|---|---|---|
| 总控 | `lead` | 派单、拍板范围、最终验收 |
| **Web 架构师（本文件维护人）** | `web-arch` | 功能盘点、拆单、接口/数据契约、文档产出、跨端对齐 |
| **Web 调试子控制中心** | `web-control` | **共享环境仲裁**：8080 重启、88 启停、浏览器自动化/截图、MySQL 测试数据写入；FIFO 排队与释放登记 |
| Web 程序员 | `web-dev1/2/3` | 按任务单实现（每人独立写作用域） |
| Web 测试 | `web-tester` | 验收用例、回归、缺陷登记 |
| C++ 端 | `cpp-arch / cpp-dev1-3 / cpp-tester / cpp-control` | 转译端并行；接口/数据契约以架构文档为准 |

**汇报链路**：所有交付与问题 → `lead`；环境调度申请 → `web-control`（并行抄送 lead 仅限阻塞事项）。

---

## 2. 共享环境调度纪律（摘要，对齐 web-control 规则 v1.1，2026-11-25 起强制）

### 2.1 必须正式申请（FIFO，先批准后操作）

| 资源 | 申请场景 | 说明 |
|---|---|---|
| 后端 8080 | **主动重启 / 启停 / 长稳定窗口**（联调、自动化测试） | 带 `spring-boot-devtools`：编译会触发 3–15s 热重启；只有需要“长时间稳定窗口 / 主动重启 / 前后端联动验收”才正式申请 |
| 前端 88 | 启动 / 重启 / 停 / 改 `vite.config.*` / `npm install` | 今日 Sprint 5 **前端代码不改，88 不需要重启**；HMR 改前端源码免申请 |
| 浏览器自动化 | Edge/Chrome/Playwright/无头浏览器/批量截图脚本 | 同一时刻只允许 1 人，**整个自动化会话**都算一次占用 |
| MySQL 写 | INSERT/UPDATE/DELETE/建表/改表（仅测试数据） | 申请须写明：库表 / SQL / 预计影响行数 / 回滚方式；测试后清理并回填实际影响行数；演示/生产数据改动必须先报 lead |

- **申请格式**（send_message → web-control）：`申请者 | 目的 | 需要资源(前端88/后端8080/浏览器/MySQL写) | 预计时长(≤15分钟) | 影响范围 | 是否涉及重启`
- **回执**：`DQ-YYYYMMDD-NNNN` + 资源 + 到期时间；默认窗口 10 分钟，有人排队时 8 分钟，单次上限 15 分钟（超长分段申请）。
- **释放**：结束立即发 `释放 DQ-xxxx | 完成/部分完成/取消 | 产物路径 | 是否有残留进程`。

### 2.2 轻量报备（免申请、不占队列，但开始前 / 结束后各发一句话给 web-control）

1. **后端代码编译 / IDEA Build**（`mvn compile`、IDEA 增量编译）→ 会触发 8080 devtools 热重启，正在联调的人会看到 8080 抖动；
2. **前端 `npm install` / 改 vite 配置** → Vite 会自动重启 88；
3. 任何大量占用 CPU/磁盘的批量任务（大构建、全量测试）。

### 2.3 免申请（可直接执行）

- 只读代码分析 / grep / 静态查看
- 日志查看、端口探测、进程核查（只读自检）
- 只读 SQL（SELECT，不写库）
- `npm run build`、`mvn -DskipTests compile`、lint / 类型检查（**其中 mvn 编译按 §2.2 轻量报备**）
- 文档编写与更新（`docs/`）

### 2.4 禁止 / 受保护资源

- **主人常驻服务不得擅动**：88（node PID **35760**）、8080（java **IDEA 实例 PID 23192**）、3306（mysqld PID **16152**）、6379（redis-server PID **35808**）——默认不重启、不停止、不改配置。
- 主动重启默认**由申请人本人执行**；**涉及主人 IDEA 稳定实例（8080 PID 23192）的停止/重启，必须先报 lead 批准**，由 lead 指定执行人执行。
- 进程清理**只能按 PID + 路径/命令行确认归属**后精确停止；**严禁按进程名批量 kill**。
- 不写 web-control 维护的规则/日志文件（`docs/tasks/Web调试调度规则.md`、`docs/test/web_调试调度日志.md`）。
- 不做与批准目的无关的操作；不顺手改他人代码。

### 2.5 本人（web-arch）调度 SOP

1. 需要浏览器 / 8080 稳定窗口 / MySQL 写 → 先发申请，**未批准不动手**；
2. 编译 → §2.2 轻量报备（开始/结束各一句）；
3. 批准后严格按目的与时限执行；超时前未完成 → 续期申请；
4. 结束立即释放，并在本文件 §7 登记流水；
5. 只读分析 / 文档 / 端口自检等免申请事项直接做；
6. 不代他人申请、不批量杀进程、不重启主人服务。

---

## 3. Sprint 5 Web 端功能追加流程（收到功能清单后执行）

```
① 接收功能清单（lead 下发）
   ↓
② 功能盘点（web-arch，只读）：逐项核对 现有页面/路由/菜单/API/DB 表/当前状态 ✅⚠️❌
   ↓ 产出《功能盘点表》（模板见 docs/tasks/Web功能盘点与拆单模板_v0.1.md）
③ 差距分析：缺接口 / 缺表 / 缺页面 / 缺菜单 / 交互缺失 / 文档已有规格可复用
   ↓
④ 拆单：按“一个页面/一个弹窗=一个任务”拆分，标注依赖与写作用域
   ↓
⑤ 提交 lead 确认范围与优先级 → 分配给 web-dev1/2/3（写作用域互斥）
   ↓
⑥ 开发联调（涉及 88/8080/浏览器 的操作走 web-control）
   ↓
⑦ web-tester 验收 → 缺陷回流 → lead 验收
```

**盘点口径（必须逐项填）**：页面/路由 → 菜单来源（sys_menu） → 现有实现文件 → 当前状态（✅/⚠️/❌，引用 `Web功能清单_v0.2.md` 已有结论） → 依赖 API（实测状态） → 依赖 DB 表（存在/缺） → 与 C++ 端契约影响（是否有 UI 规格/数据契约需要同步）。

**拆单口径（任务单最小集）**：任务编号 / 标题 / 类型（新功能/修复/数据/UI）/ 涉及文件与写作用域 / 接口与字段 / 涉及表 / 依赖任务 / 验收标准（可测） / 需要哪个环境资源（是否需 web-control 申请） / owner。

---

## 4. 任务单模板（本 Sprint 使用）

```
## Task-<S5-xx> <标题>
- 类型：新功能 / 缺陷修复 / 数据补齐 / UI 对齐 / 文档
- 页面与路由：
- 现状（盘点结论）：
- 涉及文件（写作用域）：
- 接口（方法/路径/字段，实测状态）：
- 数据表（表名/字段/种子）：
- 依赖：Task-xxx / 环境（是否需要 web-control 申请）
- 验收标准：
  1. ...
  2. ...
- 风险与回滚：
- owner：
```

---

## 5. Sprint 5 正式任务（已开工）

### 5.1 任务卡 Task-S5-01

| 项 | 内容 |
|---|---|
| 任务编号 | **Task-S5-01** |
| 标题 | 护理项目模块 CRUD 后端（6 接口） |
| 类型 | 新功能（后端补齐，前端页面零改动） |
| 状态 | 🟡 已发布 web-dev1（并行开工）→ ⏳ 待实现完成 → ⏳ web-arch review → ⏳ web-tester 验收 |
| 页面/路由 | 服务管理-护理项目 `/nursing/project` |
| 写作用域（仅后端） | ①`zzyl-nursing-platform/src/main/java/com/zzyl/nursing/` 下新增：`mapper/NursingProjectMapper.java`、`service/INursingProjectService.java`、`service/impl/NursingProjectServiceImpl.java`、`controller/NursingProjectController.java`；②修改：`src/main/resources/mapper/nursing/NursingProjectMapper.xml`（**复用已有文件，仅补 `order by order_no, id`，不新建 XML**）；③修改：`zzyl-admin/.../common/CommonController.java` 的 **`/common/upload` 单文件方法**（OSS 优先 + 本地兜底，D-Web-S5-01；`/common/uploads` 不动） |
| 禁止改动 | **前端任何代码**、数据库结构、其它模块、全局配置；不做导出/缓存（本期） |
| 依赖 | 无后端前置；`plan` 页与 `arrange` 页仅消费 `/all`（不在本任务改） |
| owner / 评审 / 验收 | web-dev1 / web-arch（≤10 分钟） / web-tester + lead |
| 详细契约与验收 | `docs/tasks/Web_Sprint5_护理项目CRUD_任务单.md`（A1–A16 验收清单） |

**接口契约（6 条，前端既有调用，不得改前端）**

| # | 方法/路径 | 权限点 | 要点 |
|---|---|---|---|
| 1 | `GET /nursing/project/list` | `nursing:project:list` | `pageNum,pageSize,name(like),status(eq)` → `TableDataInfo{total,rows}`；`startPage()+getDataTable`；**`order by order_no,id`** |
| 2 | `GET /nursing/project/{id}` | `nursing:project:query` | `AjaxResult.data=NursingProject` |
| 3 | `POST /nursing/project` | `nursing:project:add` | 名称唯一（预校验+唯一索引兜底）；status 默认 1 |
| 4 | `PUT /nursing/project` | `nursing:project:edit` | **部分更新：null 字段不覆盖**（启停只传 `{id,status}`，必须用 `updateById`） |
| 5 | `DELETE /nursing/project/{id}` | `nursing:project:remove` | 单 id（不是数组） |
| 6 | `GET /nursing/project/all` | `nursing:project:list` | `[{label:name, value:String(id)}]`，9 条，按 `order_no,id` |

**只读核查结论（web-arch，09:00–09:12）**
- 表 `nursing_project`：9 行（8 启用/1 禁用）、`name` 有 **UNIQUE 索引**；`AUTO_INCREMENT` 以测试前只读实测为准（当前 **10**，**不硬编码 id**）；
- `NursingProjectMapper.xml` **已存在且与老师 DDL 一致**（13 字段 resultMap 全匹配、select/insert/update/delete 字段一致）——**唯一差异：list 查询缺 `order by order_no, id`**；dev1 复用/校对即可，不要新建 XML；
- 菜单/按钮权限 2001–2005（list/query/add/edit/remove）**已存在**，无需改库；export 权限存在但页面无按钮，本期不做；
- Mapper 扫描已就绪：`@MapperScan("com.zzyl.**.mapper")` + `mapperLocations: classpath*:mapper/**/*Mapper.xml` + `typeAliasesPackage: com.zzyl.**.domain`；
- `/common/upload` 代码存在（CommonController→AliOssUtil→阿里云 OSS），但 **`OSS_ACCESS_KEY_ID/OSS_ACCESS_KEY_SECRET` 环境变量当前未设置** → 运行态上传失败；**lead 已决策采用选项 c：后端本地兜底（D-Web-S5-01）**——仅改 `/common/upload`，OSS 优先、失败/未配置时 `FileUploadUtils.upload(RuoYiConfig.getUploadPath(), file)` + `serverConfig.getUrl()`，`/profile/**` 映射已确认可用；`/common/uploads` 不动；
- `NursingLevelMapper` 接口 + XML 已存在但缺 Service/Controller → 记入后续候选，**本轮不做**。
- **测试口径修正（2026-11-25，tester 反馈）**：A4 `name=洗` 预期 **3 条**（洗头 id=6 / 洗脸 id=7 / 洗脚 id=8，按 order_no,id）；`AUTO_INCREMENT` 以测试前只读实测为准（2026-11-25 实测 **10**，**不硬编码 id**，A7/A9/A10/A11 使用实际返回的新增 id）；表格口径 = **9 个数据列 + 操作列 = 表头 10 列**；A13 权限负向 **本轮 N/A**（仅验 admin 正向）；批量删除 API 可选、**UI 只验单删**。

### 5.2 Sprint 5 状态

| 项 | 状态 |
|---|---|
| Web 功能清单接收 | ✅ 已收到（护理项目 CRUD 后端；C++ 端今日不追加） |
| 功能盘点 | ✅ 完成（表/实体/VO/菜单权限/上传/Mapper 扫描/依赖方核查） |
| 拆单 | ✅ Task-S5-01（1 个后端任务；无前端任务） |
| 任务单发布 | ✅ 已发 web-dev1；已按 tester 反馈修正（A4/AUTO_INCREMENT/10 列/A13/批量删除口径） |
| 环境调度规则对齐 | ✅ 规则 v1.1 已落盘，本文件 §2 已对齐 |
| R1 上传验收口径 | ✅ lead 已决策：选项 c 后端本地兜底（D-Web-S5-01，已写入任务单 §3.6） |
| **Review 结论** | ✅ **PASS（web-arch，2026-11-25 09:20）**，无阻塞项；详见 `Web_Sprint5_护理项目CRUD_任务单.md` §10 |
| **Triage（首轮 2 FAIL）** | ✅ 均判定为**口径问题、非缺陷**：REG-06=oldPeople 实际已渲染（截图证据）+ plan/level 属既有缺失记录；UI-GUARD-02 最新 run 已 PASS（43 条 API 全 /dev-api、状态 200；旧 FAIL 是未排除 Vite 模块/页面导航的误报）。详见详细任务单 §11；**无需 dev1 返修**，tester 修正判定后重出结果 |
| 下一步 | **web-tester 重生成最终结果并交 lead**（REG-06 重判 PASS；A13 仅 admin 正向；批量删除 N/A；A15/A16 上传已 PASS）；联调按规则申请 8080 稳定窗口 + 浏览器 + MySQL 写 |

> 已知环境基线（沿用，盘点复用）：40 路由结论见 `docs/architecture/Web功能清单_v0.2.md`（✅21/⚠️11/❌8）；缺表/缺 Controller 清单不变；88/8080/3306/6379 保持运行、不得擅动（主人 8080 实例 PID 23192、88 PID 35760）。

### 5.3 候选池（S6 输入，本轮不纳入）

> 来源：dev3 只读盘点（2026-11-25）＋ Sprint 2–4 既有缺口结论。**仅登记，不在 Sprint 5 实施**；S6 由 lead 拍板优先级后再拆分任务单。

| # | 候选模块/缺口 | 类型 | 现状 | 依赖/前置 |
|---|---|---|---|---|
| C-01 | 护理等级（M6） | 缺 Service/Controller | `NursingLevelMapper` 接口 + XML + domain + VO 已存在；缺 Service/Controller（本轮只做护理项目） | 无；可仿护理项目实现 |
| C-02 | 护理计划（M5） | 缺 Controller/Service | 页面/弹窗完整；`NursingProjectPlanMapper` 与两个 XML 已存在；缺 `NursingPlanMapper` 接口/Service/Controller | 依赖护理项目 `/all`（已上线）；**下午场 Task-S5b-01 已 review PASS + tester 验收 PASS（A1–A15，API 48/48、UI 36/36，91/91），可交付 lead；见 `Web_Sprint5b_护理计划CRUD_任务单.md` §10/§11** |
| C-03 | 任务安排（照护安排） | 缺路由/菜单/后端 | 前端 `views/nursing/arrange/*` 代码存在但无菜单/路由；后端任务 Controller 缺失 | 护理计划/项目数据 |
| C-04 | 健康评估 | 缺 Controller/表/菜单 | 前端 `healthAssessment` 页无菜单/路由；后端无 Controller；库无对应表 | 需老师补表/接口 |
| C-05 | 预约管理端（来访管理） | 缺 Controller/表 | 前端页面完整；`ReservationController` 缺失；库缺 `reservation` 表 | 需补表 + 后端 |
| C-06 | 设备/报警（智能监测） | 缺表 | 前端页面完整；库缺 `device/device_data/alert_rule/alert_data` | 需老师补表 |
| C-07 | 合同管理 | 前端无入口 | 后端 `ContractController` 存在；前端有页面但无菜单/路由（404） | 补菜单/路由（需写库/确认） |
| C-08 | `/common/upload` OSS 凭证 | 配置项 | 本 Sprint 已用本地兜底（D-Web-S5-01）；后续如需恢复 OSS，需主人/老师提供凭证 | lead/主人 |

---

## 6. 文档索引

| 文档 | 说明 |
|---|---|
| `docs/architecture/Web架构提取_v0.1.md` | 架构/菜单/登录/看板/主题/DB 提取 |
| `docs/architecture/Web功能清单_v0.2.md` | 40 路由逐页状态、API/DB 映射、通用组件实测 |
| `docs/architecture/UI复刻规格_Cpp_v0.2.md` | C++ 端通用组件与模块规格 |
| `docs/architecture/Sprint3_M4入住办理规格_v0.1.md` | M4 入住办理 + 房型图片 |
| `docs/architecture/Sprint4_功能规格_v0.1.md` | M5/M6/M7 + 级联数据链 + 上传规格 |
| `docs/tasks/Web功能盘点与拆单模板_v0.1.md` | 本 Sprint 盘点/拆单模板 |
| `docs/tasks/Web_Sprint5_护理项目CRUD_任务单.md` | **Task-S5-01 详细契约/文件清单/验收 A1–A16** |
| `docs/tasks/Web端Sprint任务单_v0.2.md` | 本文件（协作/调度/任务卡） |
| `docs/tasks/Web调试调度规则.md`（web-control） | 调度规则原文 v1.1（权威） |
| `docs/test/web_调试调度日志.md`（web-control） | 调度日志（权威，DQ/V 记录） |

---

## 7. 本人在 Sprint 5 的调度申请流水（自查用；权威日志在 docs/test/）

| # | 日期 | 申请内容（资源/目的） | 批准 | 起止 | 释放 | 备注 |
|---|---|---|---|---|---|---|
| — | 2026-11-25 | 暂无正式申请（今日 web-arch 只做只读核查与文档，未占用端口/浏览器/写库） | — | — | — | dev1 的 mvn 编译由其本人向 web-control 轻量报备；我 review 通过后如需联调/截图再申请 |

---

## 8. 变更记录

| 版本 | 日期 | 变更 | 作者 |
|---|---|---|---|
| v0.1 | 2026-11-25 | 建立任务单/协作文档；纳入 web-control 共享环境调度纪律（申请/免申请/禁止/模板/SOP）；建立 Sprint 5 功能盘点→拆单流程与任务单模板 | web-arch |
| v0.2 | 2026-11-25 | ①对齐调度规则 v1.1（轻量报备 / 免申请 R5 / 主人实例 PID 35760/23192 保护 / DQ 回执与 15 分钟上限 / 释放格式）；②固化 Sprint 5 正式任务卡 **Task-S5-01 护理项目 CRUD 后端**（写作用域仅后端 + 复用既有 Mapper XML 补排序、6 接口契约、权限点、验收、风险 R1 OSS 凭证）；③记录只读核查结论与后续候选（NursingLevel 等） | web-arch |
| v0.2.1 | 2026-11-25 | 按 lead R1 决策（选项 c）更新 Task-S5-01：写作用域加入 `CommonController` `/common/upload` 本地兜底（D-Web-S5-01），验收 A15/A16 改为“上传返回非空 url→列表/弹窗正常显示→前端不报错”；新增 §5.3 候选池（S6 输入，C-01~C-08：护理等级/护理计划/任务安排/健康评估/预约/设备报警/合同/OSS 凭证） | web-arch |
| v0.2.2 | 2026-11-25 | 按 tester 反馈修正契约口径：A4 `name=洗` 预期 3 条（洗头/洗脸/洗脚）；`AUTO_INCREMENT` 以只读实测为准（当前 10，不硬编码 id，A7/A9/A10/A11 用实际新增 id）；明确表格口径 = 9 数据列 + 操作列 = 10 列；A13 权限负向本轮 N/A（仅 admin 正向）；批量删除 API 可选、UI 只验单删 | web-arch |
| v0.2.3 | 2026-11-25 | dev1 交付后完成正式 review：**PASS（无阻塞项）**；记录证据 9 项（变更范围/XML/部分更新/唯一名/all/权限@Log/上传兜底/风格/编译自测）；分页超界 easonable=true 记入 A5 非缺陷说明；已通知 web-tester 开测 A1–A16 | web-arch |

---

## 9. Sprint 5 最终验收记录（lead，2026-11-25 09:50）

- **tester 终版（v1.0）**：119 用例 = 118 PASS + 1 N/A（A13 负向权限），**FAIL 0、P1/P2 缺陷 0** → 通过。
- **lead 独立只读复核**（09:49）：list total=9/rows=9 排序 1..9、`name=洗`=3、`status=0`=1、详情 id=1 字段完整、`/all`=9 条 `{label,value}`、88/8080 HTTP 200、前端源码 `git status` 为空、DB `COUNT=9` 且种子未变 → **全部通过**。
- **结论：✅ 验收通过，可交付**。
- 遗留（非阻塞，已归档）：OSS 未配置→本地兜底（D-Web-S5-01）；plan/level 缺 Controller（Sprint 候选 C-01/C-02）；前端状态 radio 默认值（P3，前端零改动约束内不修）；批量删除契约外；A13 负向 N/A。
- 详细记录：`docs/test/Web_Sprint5_验收复核.md`；调度：`docs/test/web_调试调度日志.md` v1.9。
- 未 commit 的收尾由 lead 统一提交。

---

## 10. Sprint 5b 最终验收记录（lead，2026-11-25 15:22）

- **web-tester 终版（v1.0）**：91 用例 = 91 PASS（静态 7 + API 48 + UI 36），**FAIL 0、P1/P2 缺陷 0** → 通过。
- **lead 独立只读复核**（15:18）：list total=3/rows=3（133/134/135，count 各 1）、planName=1号→1、status=1→2/status=0→1、详情 133 projectPlans=2、/all=3 条实体（133/134/135）、前端源码 git 零改动、DB 3/6 行且 ZZT5B 残留 0 → **全部通过**。
- **结论：✅ 验收通过，可交付**。详细记录 `docs/test/Web_Sprint5b_验收复核.md`。
- 已归档差异/遗留：create_by 可 NULL（R2 lead 决策，不改表）；status 字典 1/2 vs 库 0/1（O-1）；查看标题复用与 CSS 灰置（O-2，前端零改动约束内不修）；子表 plan_id 无索引/外键（R3）；护理等级 Controller 缺失列 Sprint 6 候选 C-01。
- 提交：lead 统一 commit + push（后端 5 项 + 文档）。
