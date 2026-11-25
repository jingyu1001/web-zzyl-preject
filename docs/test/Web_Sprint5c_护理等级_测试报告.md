# Web Sprint 5c · 护理等级模块 CRUD · 测试报告（终版 v1.0）

> **被测对象**：服务管理-护理等级（`/nursing/level`）后端 6 接口 + 现有前端页面（**前端源码零改动**）
> **测试执行**：web-tester｜**日期**：2026-11-25 晚场｜**依据**：`docs/tasks/Web_Sprint5c_护理等级CRUD_任务单.md`（**v0.1**，A1–A14 / D1–D3 / R2）、`docs/test/Web_Sprint5c_护理等级_测试计划.md`（v1.1）
> **调度**：web-control · W1 `DQ-20261125-0008`（22:32–22:33，提前释放）｜W2 `DQ-20261125-0009`（22:34–22:35，提前释放）
> **证据根目录**：`web-zzyl-preject/artifacts/test_web_s5c/`（static / api / ui / db / logs / tmp）
> **纪律**：未 commit、未改前端/后端源码、未建表改表、未重启服务（**8080 用户 IDEA 实例 PID 31708 未动**、88 PID 35760 未动）

---

## 1. 结论摘要

**结论：✅ 通过，可交付 lead / 主人验收（A1–A14 全部满足）。**

| 层 | 用例数 | PASS | FAIL | 说明 |
|---|---|---|---|---|
| 静态核查 STA | 7 | **7** | 0 | 前端零改动、后端改动范围合规（Mapper 声明 + XML 排序 + 3 新文件）、编译产物、契约核对 |
| API 功能 API | 45 | **45** | 0 | 列表 Vo+planName、搜索/分页、详情、新增/校验、修改、启停部分更新、删除（引用拒绝）、`/all`、权限、审计、安全 |
| UI 功能 UI | 36 | **36** | 0 | 登录/列表 8 列/搜索/新增弹窗 5 字段+计划下拉/修改回填/启停/删除 + 联动 + 回归 |
| **合计** | **88** | **88** | **0** | **P1/P2 缺陷 = 0，无需 dev1 返修** |

- **核心闭环全部通过**：列表 `total=3` + **planName**（1/2/3号护理计划）+ id asc；搜索/分页；详情；新增（5 项校验/唯一名/planId 存在性/**D2 禁用计划 134 允许**/create_time 有值/create_by 可 NULL）；整表单修改（planName 随 planId 联动、createTime 不变）；**启停仅 `{id,status}` 且其它字段零变化**；删除（新等级 200；**A10 种子 77 被 check_in_config 引用 → 友好拒绝且数据不变**）；`/all` 实体 3 条（含 id/name/fee/status）。
- **联动**：护理计划页 1号计划 `count` 1→2（测试等级关联）→删除后 **恢复 1**；`/nursing/plan/all` 正常 3 项。
- **前端零改动**：前端目录 `git status/diff` 空，`level.js`/`level/index.vue` hash 与 HEAD 一致；UI 抓包 37 个 `/dev-api/nursing/level` 请求（GET/POST/PUT/DELETE）全 200。
- 说明：W1 首轮 41/45 失败均为**测试脚本适配**（decimal 字符串比较、arch 最新口径“详情返回实体无 planName”的断言偏差），修正后 **45/45**；W2 首轮即 **36/36**。

---

## 2. 环境与调度记录

| 项 | 值 |
|---|---|
| 前端 88 | `http://localhost:88`，PID **35760**（未重启） |
| 后端 8080 | `http://localhost:8080`，**用户 IDEA 实例 PID 31708**（16:49 重启），全程未重启；devtools 已加载新 Controller |
| MySQL 3306 | `zzyl`：`nursing_level`（3 行）、`nursing_plan`（3 行）、`nursing_project_plan`（6 行）、`check_in_config`（1 行，引用 level 77） |
| Redis 6379 | **仅只读** `GET captcha_codes:{uuid}` 1 次（22:34:21），未写未删；未改配置 |
| 浏览器自动化 | 无头 Edge（playwright-core@1.65.1，系统 channel），W2 独占；结束 msedgedriver **0**、我的无头 Edge **0**、自动化 node **0** |
| 窗口 | W1 22:32–22:33（API+MySQL 写，提前 9 分钟释放）｜W2 22:34–22:35（浏览器+MySQL 写，提前 14 分钟释放） |
| 备注 | 22:31 出现的 headless Edge 经查为 Edge 自带 `PlatformExperiencesHelper` 遥测进程（`--edge-upload-metrics`，about:blank），**非任何 agent 的自动化会话**，不影响资源判定 |

---

## 3. 执行概况

| 轮次 | 产物/结果 |
|---|---|
| API 套件 `api/level_api_results_20261125_223243.json` | **45/45 PASS** |
| UI 套件 `ui/level_ui_results_level_ui_20261125143413.json` | **36/36 PASS**，**28 张截图** |
| 静态核查 `static/` | 前端 `git status` 空、diff 空、hash=HEAD；后端仅 `NursingLevelMapper.java` 补声明 + `NursingLevelMapper.xml` 加排序 + 3 个新文件；无配置/表/其它模块改动 |
| 数据核验 `db/` | `baseline.json`/`final.json`/`seed77_snapshot.json`；四表 CHECKSUM 与 web-control 基线一致 |

---

## 4. A1–A14 验收矩阵

| # | 验收项 | 结论 | 关键证据 |
|---|---|---|---|
| A1 | 静态/变更范围：前端零改动；后端仅 Mapper 声明 + XML 排序 + 3 新文件 | **PASS** | `static/frontend_zero_change.txt`（空）、`static/frontend_hashes.txt`（三文件 SAME）、`static/git_status_code.txt`、`static/backend_diff_stat.txt` |
| A2 | 列表：total=3、rows=3、id asc、**planName**、fee/status/description/createTime | **PASS** | `API-LIST-01`：「total=3 ids=76,77,78 planName=1号/2号/3号护理计划 fees=1500.00/2000.00/3000.00」；UI `LIST-01/02`（8 列含“执行护理计划”=1号护理计划） |
| A3 | 搜索/分页：`name=2号`→1、`status=1`→3、`status=0`→0、pageSize=2→2/1 | **PASS** | `API-LIST-03/05/07`；UI `SEARCH-01/02/04` |
| A4 | 详情：77 → planId=134、fee=2000、status/description（返回实体；planName 不要求） | **PASS** | `API-GET-01`：「id=77 planId=134 fee=2000; planName=缺席（实体，按 arch 口径不要求）」 |
| A5 | 新增：200、total=4、动态 id、DB create_time 有值、create_by 可 NULL | **PASS** | `API-ADD-01`：「id=85 total=4 create_time=2026-11-25 22:32:55 create_by=[NULL] list planName=1号护理计划」；UI `ADD-04` id=90、total=4 |
| A6 | 整表单修改：字段全生效、planName 联动、createTime 不变 | **PASS** | `API-UPD-01`：「planName=3号护理计划 fee=2600; createTime unchanged」；UI `EDIT-01/02` |
| A7 | **启停部分更新（关键）**：仅 `{id,status}`、其它字段不变 | **PASS** | `API-UPD-02`：「status 1->0; name/planId/fee/description 保持」；UI `TOG-02`：抓包 `PUT={"id":90,"status":0}` |
| A8 | 名称唯一：重名新增/改名→友好错误、排除自身 | **PASS** | `API-ADD-03`、`API-UPD-05/06`；UI `ADD-05`/`EDIT-04` |
| A9 | planId 校验：999999→“护理计划不存在”；**134 禁用计划允许（D2）** | **PASS** | `API-ADD-05`：「code=500 msg=护理计划不存在」；`API-ADD-09`：「id=89 planId=134（D2 允许）list planName=2号护理计划」 |
| A10 | 删除：新等级 200；**种子 77 被引用→友好拒绝且数据不变**；不存在→“护理等级不存在” | **PASS** | `API-DEL-04`：「code=500 msg=该护理等级已被入住配置引用，无法删除; 77 与 check_in_config 均不变」；`API-DEL-01`：deleted id=85、total 回 3；UI `DEL-01/02` |
| A11 | `/all`：length=3、id asc、含 id/name/fee/status（实体） | **PASS** | `API-ALL-01`：「length=3 ids=76,77,78（实体数组，含 id/name/fee/status；planName 不存在=False，按 arch 口径 /all 用实体）」 |
| A12 | 前端页面：3 行、**8 列含执行护理计划**、新增/编辑/启停/删除按既有行为、前端零改动 | **PASS** | UI `LIST-01/02`、`ADD/EDIT/TOG/DEL` 全通过；`UI-GUARD-01` 37 请求全 200 |
| A13 | 权限：admin 正向全通（负向 N/A） | **PASS** | `API-AUTH-01`（admin `*:*:*`）、`API-AUTH-02`（未认证写 body 401 且不产生数据） |
| A14 | 数据恢复：level=3、plan=3、project_plan=6、check_in_config 不变、ZZT5C=0 | **PASS** | `API-DEL-03`：「level=3 plan=3 project_plan=6 cfg=1；各表哈希与基线一致」；收尾 CHECKSUM 四表与基线一致 |

---

## 5. 关键业务断言（证据摘录）

1. **A7 启停不丢字段**：`API-UPD-02` → `status 1->0; name/planId/fee/description 保持`；UI 抓包 `PUT={"id":90,"status":0}`、toast“禁用成功”。
2. **A10 引用删除拦截**：`API-DEL-04` → `code=500 msg=该护理等级已被入住配置引用，无法删除`，level 77 与 check_in_config 均不变；种子 77 快照前后一致。
3. **A9 planId 校验**：不存在→`护理计划不存在`；**禁用计划 134 允许关联**（D2，与既有数据 level 77→plan 134 一致）。
4. **A2 planName 关联**：列表 Vo join 正常（1/2/3号护理计划）；**详情/`/all` 按 arch 口径为实体**（无 planName），无断言冲突。
5. **UI 新增校验**：空表单出现 **5 项**前端必填提示（等级名称/护理计划ID/护理费用/状态/等级说明）；计划下拉 3 项。
6. **联动**：测试等级关联 1号计划时 `plan133.count=2`，删除后 **恢复 1**（LINK-01/04）。

---

## 6. 缺陷与观察项

### 6.1 产品缺陷
- **P1/P2/P3 产品缺陷：0 项**（无需 dev1 返修）。

### 6.2 观察项（P3/P4，不阻塞）
| 编号 | 观察 | 处理 |
|---|---|---|
| O-1 | `fee` 后端仅校验非空，**负值未拦截**（任务单写“建议 fee>=0”，非硬性要求）；测试用 `fee=-10` 建档成功并已清理 | 记录；建议下迭代补 `fee>=0` 校验（P3，不判缺陷） |
| O-2 | 详情返回**实体**（无 planName）、`/all` 返回**实体数组**；与任务单 §3.2 初稿“Vo”不同，但 **arch 正式开测口径已明确“实体、planName 不要求”** | 按 arch 口径验收通过；报告留痕 |
| O-3 | XML `selectNursingLevelVoList` 未加 `id` 过滤（任务单建议加），但详情改用 `selectNursingLevelById`，功能无影响 | 记录（非阻塞） |
| O-4 | 前端列表无 count/灰置逻辑、无“查看”按钮 | 前端既有设计，任务单无要求；按前端现状验收 |
| R2 | `create_by/update_by` bigint vs BaseEntity String：按 lead 决策 XML 自定义 SQL、不赋值（NULL） | 实测新增 `create_by=[NULL]`、create_time 有值，符合口径 |
| R5 | AUTO_INCREMENT 前移（测试新增） | 正常副作用；`MAX(id)`/`CHECKSUM` 已核验 |

### 6.3 首轮失败说明（测试脚本适配，非产品缺陷）
- W1 首轮 41/45：① `fee` 用字符串比较（`2000` vs `2000.00`）；② 按任务单初稿断言“详情含 planName”，而 arch 开测口径为“详情返回实体、planName 不要求”。修正脚本后 **45/45**。
- W2 首轮即 **36/36**（沿用 5a/5b 脚本经验：选择器/弹窗兜底/等待策略）。

---

## 7. 测试数据与清理（R4 写库纪律）

| 项 | W1（API） | W2（UI） |
|---|---|---|
| 测试数据 | `ZZT5C-*` 等级 5 条（含 1 条负费用观察行） | `ZZT5C-UI-*` 1 条 |
| 变更类型 | level INSERT **5** / UPDATE **4** / DELETE **5** | level INSERT **1** / UPDATE **3** / DELETE **1** |
| 收测状态 | **level=3、plan=3、project_plan=6、cfg=1、ZZT5C 残留=0** | 同左 |
| 种子 76/77/78 | 字段与 update_time 全部不变（A10 DELETE 77 被拒，数据不变） | 未触碰 |
| CHECKSUM TABLE | **level 586424728 / plan 2521898468 / project_plan 2554906448 / cfg 680483255**，与 web-control 基线完全一致 | 同左（第二次核验一致） |
| 种子哈希 | level `6b8c4530278b39172564e178d9ed978e`、plan `2c87473b…`、detail `5326dd71…`、cfg `ad1c3250…`，与开测基线一致 | level 哈希一致 |
| 自动日志 | `sys_logininfor`/`sys_oper_log` 少量（应用行为，未清理，记录备查） | 同左 |
| 备注 | W1 INSERT 实际 5 行较申报 ≤4 多 1（负费用观察用例，均已清理）；web-control 记录备查、不计违规 | — |

---

## 8. 证据索引（`artifacts/test_web_s5c/`）

| 路径 | 内容 |
|---|---|
| `static/` | `git_status_code.txt`、`frontend_zero_change.txt`（空）、`frontend_hashes.txt`、`backend_diff_stat.txt`、`diff_NursingLevelMapper_xml.txt` |
| `api/level_api_results_20261125_223243.json`（+ `_latest.json`） | API 45 用例结果（PASS/证据/四表基线） |
| `db/baseline.json`、`db/final.json`、`db/seed77_snapshot.json` | 四表基线/收测、种子 77 快照 |
| `ui/level_ui_results_level_ui_20261125143413.json`（+ `_latest.json`） | UI 36 用例结果（证据 + 截图路径） |
| `ui/*.png` | **28 张**过程截图（登录/列表/搜索/新增弹窗/新增成功/修改回填/启停/删除/联动/回归） |
| `logs/level_ui_stdout.log` | UI 套件控制台日志（含验证码交接时间） |
| `../tools/api/level_api_suite.ps1`、`../tools/browser/level_ui_suite.mjs` | 可复现测试脚本 |

---

## 9. 结论与建议

1. **验收结论：通过**。A1–A14 全部满足，P1/P2 缺陷 0；关键项（planName 关联、启停部分更新 A7、planId 存在性/D2、**D1 删除引用拦截 A10**、`/all` 实体、数据恢复）均有可复现证据，**可交付 lead / 主人验收**。
2. **建议（不阻塞）**：
   - `fee>=0` 后端校验建议补齐（O-1，任务单“建议”项）；
   - XML VoList 可加 `id` 过滤以贴齐任务单 §5（O-3，当前详情走 `selectById`，无功能影响）；
   - 后续回归可直接复用两套脚本（API 免浏览器、UI 需 web-control 浏览器窗口）。
3. **流程留痕**：W1 首轮 41/45 均为测试脚本适配（decimal 比较/detail 口径），修正后 45/45；W2 首轮 36/36。**未将脚本问题误判为产品缺陷**。

---

## 变更记录

| 版本 | 日期 | 变更 | 作者 |
|---|---|---|---|
| v1.0 | 2026-11-25 | 终版：A1–A14 验收矩阵、API 45/45 + UI 36/36（88/88 PASS）、P1/P2 缺陷 0、观察项 O-1/O-2/O-3/O-4/R2/R5、数据清理与四表 CHECKSUM 核验、脚本适配说明 | web-tester |
