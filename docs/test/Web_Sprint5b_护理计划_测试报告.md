# Web Sprint 5b · 护理计划模块 CRUD · 测试报告（终版 v1.0）

> **被测对象**：服务管理-护理计划（`/nursing/plan`）后端 6 接口（`nursing_plan` + `nursing_project_plan`）+ 现有前端页面（**前端源码零改动**）
> **测试执行**：web-tester｜**日期**：2026-11-25 下午场
> **依据**：`docs/tasks/Web_Sprint5b_护理计划CRUD_任务单.md`（**v0.2**，A1–A15）、`docs/test/Web_Sprint5b_护理计划_测试计划.md`（v1.2）
> **调度**：web-control · W1 `DQ-20261125-0005`（15:03–15:05，提前释放）｜W2 `DQ-20261125-0006`（15:06–15:17，提前释放）
> **证据根目录**：`web-zzyl-preject/artifacts/test_web_s5b/`（static / api / ui / db / logs / tmp）
> **纪律**：未 commit、未改前端/后端源码、未建表改表、未重启 88/8080/3306/6379（8080 PID 23192、88 PID 35760 全程未动）

---

## 1. 结论摘要

**结论：✅ 通过，可交付 lead / 主人验收（A1–A15 全部满足）。**

| 层 | 用例数 | PASS | FAIL | 说明 |
|---|---|---|---|---|
| 静态核查 STA | 7 | **7** | 0 | 前端零改动、后端改动范围合规、编译产物、契约核对 |
| API 功能 API | 48 | **48** | 0 | 6 接口 + 事务/引用强制/子表校验/启停部分更新/`/all` |
| UI 功能 UI | 36 | **36** | 0 | 登录/列表/搜索/新增子表/修改回填/查看只读/启停/删除 + 联动 + 回归 |
| **合计** | **91** | **91** | **0** | **P1/P2 缺陷 = 0，无需 dev1 返修** |

- **核心闭环全部通过**：列表 `total=3` + `count=1` + 排序、`planName/status` 搜索、分页、详情 `projectPlans`（projectId/executeCycle 字符串、frequency 数字）、**新增主表+子表事务**、名称唯一、**子表校验/事务回滚无脏主表**、整表单修改（子表 2→1 替换无残留）、**启停仅 `{id,status}` 且其它字段/子表不变**、删除级联子表、**A8 被引用强制校验**、`/all` 返回 `List<NursingPlan>` 3 条实体。
- **A8 种子保护**：种子 133 整表单改/删均被友好拒绝（数据不变）；133 启停 `{id,status}` 放行（1→0→1），验后按快照恢复 `status=1、update_time=NULL`；两表 **CHECKSUM 与基线完全一致**。
- **前端零改动**：前端目录 `git status/diff` 为空，`plan.js` / `plan/index.vue` / `level/index.vue` hash 与 HEAD 一致。
- **说明**：首轮 W1（35/48）与 W2（22/36）的失败均为**测试脚本适配问题**（PS 类型断言、DB 字段拆分、`update_time=NULL` 恢复、Element Plus 时间选择器/弹窗级联/只读选择器），**非产品缺陷**；修正脚本后 W1 **48/48**、W2 **36/36**（详见 §6.2 与附录 A）。

---

## 2. 环境与调度记录

| 项 | 值 |
|---|---|
| 前端 88 | `http://localhost:88`，PID **35760**（未重启） |
| 后端 8080 | `http://localhost:8080`，Spring Boot（IDEA 实例）PID **23192**（未重启；dev1 编译后 devtools 已加载新 Controller） |
| MySQL 3306 | `zzyl`：`nursing_plan`（基线 3 行）、`nursing_project_plan`（基线 6 行）、`nursing_level`（3 行，各计划引用 1 次 → count=1） |
| Redis 6379 | **仅只读** `GET captcha_codes:{uuid}`：W1 前 1 次（15:04）、W2 1 次（15:15），未写未删；未改配置 |
| 浏览器自动化 | 无头 Edge（playwright-core@1.65.1，系统 channel），W2 独占；结束 msedgedriver **0**、无头 Edge **0**、自动化 node **0** |
| 窗口 | W1 15:03–15:05（API+MySQL 写，提前 8 分钟释放）｜W2 15:06–15:17（浏览器+MySQL 写，提前 4 分钟释放） |

---

## 3. 执行概况

| 轮次 | 产物/结果 |
|---|---|
| API 套件 `api/plan_api_results_20261125_150507.json` | **48/48 PASS**（首轮 35/48 为脚本适配，修正后终轮 48/48） |
| UI 套件 `ui/plan_ui_results_plan_ui_20261125071520.json` | **36/36 PASS**，**51 张截图**（首轮 22/36、次轮 35/36 均为脚本适配） |
| 静态核查 `static/` | 前端 `git status` 空、diff 空、hash=HEAD；后端仅 4 新文件 + `NursingPlanMapper.xml`；无配置/表/其它模块改动；`target/classes` 含新类 |
| 数据核验 `db/` | 基线快照 + `seed133_snapshot.json` + 收测核验；两表 CHECKSUM 与 web-control 基线一致 |

---

## 4. A1–A15 验收矩阵

| # | 验收项 | 结论 | 关键证据 |
|---|---|---|---|
| A1 | 静态/变更范围：前端零改动；后端仅 4 新文件 + XML | **PASS** | `static/frontend_zero_change.txt`（空）、`static/frontend_hashes.txt`（三文件 HEAD=WORK）、`static/git_status_code.txt`（仅 4 新增 + XML 修改）、`static/backend_diff_stat.txt` |
| A2 | 列表：total=3、rows=3、sort_no,id、count=1、createTime、status | **PASS** | `API-LIST-01/02`：ids **133,134,135**，每行 `count=1`、createTime 有值、status 1/0/1 |
| A3 | 搜索/分页：`1号`→1；`status=1`→2、`status=0`→1；pageSize=2→2/1 | **PASS** | `API-LIST-03/04/05/07`；UI `SEARCH-01/02` |
| A4 | 详情：projectPlans 2 条；projectId/executeCycle 字符串、frequency 数字；134/135=1/3 | **PASS** | `API-GET-01/02`：「projectPlans=2; projectId/executeCycle 为字符串; freq=7」；UI `EDIT-01` 回填项目名 |
| A5 | 新增主+子事务：200、total=4、动态 id、DB 主 1+子 2、create_time 有值、create_by 可 NULL | **PASS** | `API-ADD-01`：「new id=148; list total=4; create_time=2026-11-25 15:05:15; create_by=[NULL]」；UI `ADD-06` id=151、total=4、count=0 |
| A6 | 整表单修改：子表 2→1 替换无残留；projectId 字符串 | **PASS** | `API-UPD-01`：「sortNo=7 kids=1（旧 2 行已删）update_time=…」；UI `EDIT-02` toast 修改成功、kids=1 |
| A7 | **启停部分更新（关键）**：仅 `{id,status}`、其它字段/子表不变 | **PASS** | `API-UPD-02/03`；UI `TOG-02/03`：抓包 `PUT={"id":151,"status":0}`、字段保持 |
| A8 | 被引用校验：种子 133 整表单改/删→友好拒绝且数据不变；133 启停放行 | **PASS** | `API-UPD-08`/`API-DEL-04`：code=500 msg「该护理计划已被护理等级引用，无法修改或删除」、133 与子表 2 行仍在；`API-A8-01/02`：status 1→0→1 放行，验后 `update_time` 回滚、planHash 恢复 |
| A9 | 删除新计划：200、total 回 3、子表级联 0 残留；不存在→友好错误 | **PASS** | `API-DEL-01`：「deleted id=148; 级联子表 0 残留; total 回 3」；UI `DEL-02` toast 删除成功、total=3 |
| A10 | 名称唯一：重名新增/改名→友好错误、无落库/原值不变；排除自身 | **PASS** | `API-ADD-03/04`、`API-UPD-05/06`；UI `EDIT-05` 重名 toast |
| A11 | 子表校验/回滚：空/缺字段/重复→友好错误；主表无脏数据 | **PASS** | `API-ADD-02`（护理项目不能为空）、`API-ADD-05`（事务回滚「主表无新增」）、`API-ADD-06`（请勿选择重复的护理项目）；UI `ADD-05` 前端拦截无 POST |
| A12 | `/all`：`List<NursingPlan>` 实体数组 3 条含禁用、id/planName、排序；等级页下拉 3 项 | **PASS** | `API-ALL-01`：「length=3 ids=133,134,135（实体数组，非 label/value）」；UI `LINK-01/02`：all 200 length=3、下拉 3 项 |
| A13 | 前端页面联动：列表 3 条、count=1 行灰置、新增/修改/查看只读/启停/删除、前端零改动 | **PASS** | UI `LIST-01..04`（5 列、count 行 修改/删除带 disabled class）、`ADD/EDIT/TOG/DEL` 全通过；`GUARD-01` 37 个 `/dev-api/nursing/plan` 请求 GET/POST/PUT/DELETE 全 200 |
| A14 | 权限：admin 正向全通（负向 N/A） | **PASS** | `API-AUTH-01`（admin `*:*:*`）、`API-AUTH-02`（未认证写 body 401 且不产生数据）；负向按 lead 决策 N/A |
| A15 | 事务一致性（加强）：构造子表失败→主表无脏数据 | **PASS** | `API-ADD-05`：子表非法 → 友好错误「护理项目不能为空」且主表无新增（随后修复重试成功用例链） |

---

## 5. 关键业务断言（证据摘录）

1. `API-UPD-02`：`status 1->0; planName/sortNo/子表保持`；UI 抓包 `PUT={"id":151,"status":0}`（**启停部分更新成立，不覆盖其它字段/子表**）。
2. `API-UPD-08` / `API-DEL-04`：种子 133 被拒，`msg=该护理计划已被护理等级引用，无法修改或删除`，133 主表与子表 2 行不变。
3. `API-A8-01/02`：133 启停放行后按快照恢复：`status=1; update_time=NULL; planHash=71555f48…`（与基线一致）。
4. `API-ADD-05`：子表非法 `code=500 msg=护理项目不能为空`，**主表无新增**（事务/校验前置），符合 A11/A15。
5. `API-GET-01`：`projectPlans=2; projectId/executeCycle 为字符串; freq=7`（前端下拉回填类型正确）。
6. UI：表头 `["序号","名称","状态","创建时间","操作"]`（5 列）；3 行 count=1 的修改/删除均带 `disabled` class；新增弹窗 1000px + 子表 4 列默认 1 行；查看只读无底部按钮；启停/删除 toast 与列表刷新正常。

---

## 6. 缺陷与观察项

### 6.1 产品缺陷
- **P1/P2/P3 产品缺陷：0 项**（无需 dev1 返修）。

### 6.2 测试脚本适配问题（非产品缺陷；首轮失败原因）
| 现象 | 原因 | 修正 |
|---|---|---|
| W1 首轮 35/48 FAIL（类型转换/DB 取值/哈希未恢复） | `Assert-True` 强类型 `[bool]` 不接受字符串/对象；`CONCAT_WS` 单行取值被脚本拆分错误；`update_time=NULL` 快照恢复函数跳过 NULL | 修正断言签名、拆分独立 DB 查询、NULL 时恢复 `update_time=NULL`；终轮 **48/48** |
| W2 首轮 22/36 FAIL | Element Plus 时间选择器选择器不符、校验期望数错误、失败后弹窗未关导致后续点击级联超时 | 改用 `.el-date-editor input`/input 索引、期望 1 条名称校验、增加 `ensureNoDialog` 兜底；次轮 35/36 |
| W2 次轮唯一 FAIL（查看只读下拉禁用判定） | 对 `.el-select` 根节点查 `is-disabled`，实际 disabled 类在 `.el-select__wrapper` | 放宽为 wrapper/input 多重判定；终轮 **36/36** |

> 以上均为**测试脚本问题**，不涉及后端/前端产品代码；已在报告留痕，避免误判。

### 6.3 任务单已列观察项（不判缺陷）
| 编号 | 观察 | 处理 |
|---|---|---|
| O-1 | 状态字典 启用=1/禁用=2，而种子 134 status=0；134 编辑时 radio 无选中 | 后端不限制 status（接受 0/1/2）；UI 记录实际（禁用搜索 0 行），**不判缺陷** |
| O-2 | 前端“查看”复用 dialog 且标题写“修改护理计划”；count>0 灰置仅 CSS 点击仍触发 | 记录；后端 count 强制校验为准（A8 已验证）；UI 未实际操作种子删改 |
| R1 | 3 个种子计划均 count=1，无法对种子做修改/删除验收 | 已按 A5 新增 count=0 计划验证 A6/A7/A9 |
| R2 | `create_by/update_by` bigint vs BaseEntity String | lead 决策不改表、主表走 XML 自定义 SQL、`create_by` 允许 NULL；实测接口/DB 正常（API-ADD-01 create_by=[NULL]） |
| R3 | 子表 `plan_id` 无索引/无外键 | 仅记录；测试中未发现孤儿数据 |
| R7 | 子表 `batchInsert` 用 `sysdate()` 填 create_time | 子表 create_time 正常有值 |
| — | 前端新增弹窗子表默认 1 行、无逐项必填规则（前端） | 后端已兜底校验（A11 通过）；前端不改 |

---

## 7. 测试数据与清理（R4 写库纪律）

| 项 | W1（API） | W2（UI） |
|---|---|---|
| 测试数据 | `ZZT5B-*` 计划 2 个（含子表）；A8 使用种子 133（快照） | `ZZT5B-UI-*` 计划 1 个（含子表 2 行） |
| 变更类型 | plan INSERT **2** / UPDATE **6**（含 133 启停 2 次）/ DELETE **2**；project_plan INSERT **5** / DELETE **5** | plan INSERT **1** / UPDATE **3** / DELETE **1**；project_plan INSERT **3** / DELETE **3** |
| 收测状态 | **plan=3、detail=6、ZZT5B=0、孤儿=0** | 同左 |
| 种子 133 | status **1→0→1**（A8 启停放行），`update_time` 按快照恢复 **NULL** | 未触碰 |
| 校验和 | **CHECKSUM TABLE = 2521898468 / 2554906448**（与 web-control 基线完全一致） | 同左（第二次核验一致） |
| 种子哈希 | plan `71555f48b59b6ff68b822c739770b008`、detail `5326dd71d92a21217172a5de380aca4a`（与开测基线一致） | plan 哈希一致 |
| 自动日志 | `sys_logininfor`/`sys_oper_log` 少量（应用行为，未清理，记录备查） | 同左 |
| 副作用说明 | `AUTO_INCREMENT` 前移（测试新增）属正常；不硬编码 id | 同左 |

---

## 8. 证据索引（`artifacts/test_web_s5b/`）

| 路径 | 内容 |
|---|---|
| `static/` | `git_status_code.txt`、`frontend_zero_change.txt`（空）、`frontend_hashes.txt`、`backend_diff_stat.txt`、`diff_NursingPlanMapper_xml.txt`、`compiled_classes.txt`、`env_health.txt` |
| `api/plan_api_results_20261125_150507.json`（+ `_latest.json`） | API 48 用例结果（PASS/证据/基线） |
| `db/baseline.json`、`db/final.json`、`db/seed133_snapshot.json` | 两表基线/收测、种子 133 快照 |
| `ui/plan_ui_results_plan_ui_20261125071520.json`（+ `_latest.json`） | UI 36 用例结果（证据+截图路径） |
| `ui/*.png` | **51 张**过程截图（登录/列表/搜索/新增弹窗/子表/新增成功/修改回填/查看只读/启停/删除/等级页下拉/回归） |
| `logs/plan_ui_stdout.log` | UI 套件控制台日志（含验证码交接时间） |
| `../tools/api/plan_api_suite.ps1`、`../tools/browser/plan_ui_suite.mjs` | 可复现测试脚本 |

---

## 9. 结论与建议

1. **验收结论：通过**。A1–A15 全部满足，P1/P2 缺陷 0；主+子表事务、被引用强制校验（A8）、启停部分更新（A7）、`/all` 实体数组（A12）等关键项均有可复现证据，**可交付 lead / 主人验收**。
2. **建议（不阻塞）**：
   - 前端观察项 O-1（状态字典 1/2 vs 库 0/1）与 O-2（查看标题/灰置仅 CSS）建议下个迭代统一（前端改动需 lead 决策，本轮未动）；
   - 子表 `plan_id` 索引/外键（R3）可在大数据量前评估；
   - 后续回归可直接复用 `artifacts/tools/api/plan_api_suite.ps1` 与 `artifacts/tools/browser/plan_ui_suite.mjs`。
3. **流程留痕**：首轮 W1/W2 失败均为测试脚本适配问题，已在 §6.2/附录 A 透明记录，**未作为产品缺陷**；最终交付轮 API 48/48、UI 36/36。

---

## 附录 A · 测试执行轮次与失败归因

| 轮次 | 结果 | 失败归因（均为测试脚本/口径适配） |
|---|---|---|
| W1 #1 | 35/48 | PS 强类型断言、DB 单行取值拆分、`update_time=NULL` 恢复 |
| W1 #2 | 47/48 | 删除用例计数断言（未计其他测试行） |
| W1 #3 | 47/48 | 清理用例断言误改 |
| **W1 终轮** | **48/48 PASS** | — |
| W2 #1 | 22/36 | 时间选择器选择器、弹窗未关导致级联超时、校验期望数 |
| W2 #2 | 35/36 | 查看只读下拉 disabled 类判定位置 |
| **W2 终轮** | **36/36 PASS** | — |

> 补充：A8 首次执行后种子 133 `update_time` 因快照恢复函数跳过 NULL 未还原；已立即按快照恢复为 NULL 并在终轮及收尾两次核验（CHECKSUM 与基线一致）。

---

## 变更记录

| 版本 | 日期 | 变更 | 作者 |
|---|---|---|---|
| v1.0 | 2026-11-25 | 终版：A1–A15 验收矩阵、API 48/48 + UI 36/36（91/91 PASS）、P1/P2 缺陷 0、观察项 O-1/O-2/R1/R2/R3/R7、数据清理与 CHECKSUM 核验、脚本适配说明 | web-tester |
