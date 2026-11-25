# Web Sprint 5 · 护理项目模块 CRUD · 测试报告（终版 v1.0）

> **被测对象**：服务管理-护理项目（`/nursing/project`）后端 6 接口 + 现有前端页面（前端源码零改动）
> **测试执行**：web-tester｜**日期**：2026-11-25｜**依据**：`docs/tasks/Web_Sprint5_护理项目CRUD_任务单.md`（A1–A16）、`docs/test/Web_Sprint5_护理项目_测试计划.md`（v1.2）
> **调度**：web-control · W1 `DQ-20261125-0002`（09:08–09:13）· W2 `DQ-20261125-0003`（09:14–09:25，提前释放）
> **证据根目录**：`web-zzyl-preject/artifacts/test_web_s5/`（static / api / ui / db / logs）
> **纪律**：未 commit、未改前端/后端源码、未建表改表、未重启 88/8080/3306/6379（8080 PID 23192、88 PID 35760 全程未动）

---

## 1. 结论摘要

**结论：✅ 通过，可交付 lead / 主人验收（A1–A16 全部满足；A13 负向权限按 lead 决策 N/A）。**

| 层 | 用例数 | PASS | FAIL | 说明 |
|---|---|---|---|---|
| 静态核查 STA | 7 | **7** | 0 | 前端零改动、后端改动范围合规、编译产物/代码评审 |
| API 功能 API | 47 | **47** | 0 | 6 接口全契约 + 边界 + 上传 + 回归 + 安全 |
| UI 功能 UI | 49 | **49** | 0 | 登录/列表/搜索/新增/修改/删除/启停/分页/图片/回归 |
| 联动 LINK | 3 | **3** | 0 | plan 页 `/nursing/project/all` 下拉 9 项 |
| 回归 REG | 6 | **6** | 0 | 房型/用户/菜单/既有 API/服务健康 |
| 非功能 NF | 7 | **6** | 0 | 1 项 N/A（A13 负向权限，lead 决策） |
| **合计** | **119** | **118** | **0** | 另 1 项 N/A；**P1/P2 缺陷 = 0，无需 dev1 返修** |

- **键契约全部通过**：列表 9 条 + 排序（order_no,id）、`name=洗`→3 条、`status=0`→1 条、分页（含 reasonable 超界末页）、详情、新增（必填/重名/默认 status/动态 id）、修改（全字段 + **启停仅 `{id,status}` 部分更新不清空其它字段**）、删除、`/all`（9 条 `{label,value}` 字符串结构）。
- **A15/A16**：`/common/upload` 本地兜底真实验证通过（url 非空、浏览器可直接打开、前端弹窗回填），历史 OSS URL 与本地兜底 URL 共存显示正常。
- **P1/P2 缺陷：无。** 唯一 P3 观察项为前端新增弹窗“状态”radio 无默认选中（前端不改，功能正确，见 §6）。
- 首轮执行中出现的 2 个 UI FAIL 经 web-arch triage 确认为**判定口径问题**（REG-06 渲染判定启发式、UI-GUARD-02 误把 Vite 源码模块请求当 API），已修正后复测 **49/49 PASS**；详见附录 A。

---

## 2. 环境与调度记录

| 项 | 值 |
|---|---|
| 前端 88 | `http://localhost:88`，Vite dev，PID **35760**（未重启） |
| 后端 8080 | `http://localhost:8080`，Spring Boot（IDEA 实例）PID **23192**（未重启；dev1 编译触发 devtools 自动加载新代码，测试期间 8080 稳定） |
| MySQL 3306 | `zzyl` 库 `nursing_project` 表（只读核验 + 经接口的测试数据写） |
| Redis 6379 | **仅只读** `GET captcha_codes:{uuid}` 3 次（09:15:25 / 09:19:42 / 09:22:40，用于验证码交接，未写未删）；W1 前 1 次人工视觉识别验证码登录 |
| 浏览器自动化 | 无头 Edge（`playwright-core@1.65.1`，系统 channel），W2 独占窗口，结束后无残留进程（`msedgedriver=0`、无头 Edge=0） |
| 窗口 | W1 09:08–09:13（API+MySQL 写）｜W2 09:14–09:25（浏览器+8080 只读+MySQL 写），均提前释放 |

---

## 3. 执行概况

| 轮次/产物 | 结果 |
|---|---|
| API 套件 `api/api_results_20261125_091303.json` | **47/47 PASS**（另首轮 4 类失败均为**测试脚本适配**：curl `-X` 方法名大小写、PS5.1 mysql stderr、价格字符串比较、RuoYi 未认证 HTTP200+body401 约定；修正后全过，**非后端缺陷**） |
| UI 套件 `ui/ui_results_ui_20261125012234.json` | **49/49 PASS**，60 张过程截图 |
| 静态核查 `static/` | 前端 `git status --porcelain` 为空、`git diff` 为空、关键文件 hash 与 HEAD 一致；后端仅约定文件变更 |
| 数据库核验 `db/seed_verify_final.json` | COUNT=9、MAX(id)=9、ZZT5 残留=0、种子哈希 `4a6dfdb56c4edc1040b6ce30ccdb8f66`（W1/W2 前后一致；web-control 独立 SEED_MD5 亦一致） |

---

## 4. A1–A16 验收矩阵

| # | 验收项 | 结论 | 关键证据 |
|---|---|---|---|
| A1 | 代码 Diff：前端零改动；后端仅 4 新文件 + XML + 批准的 `CommonController` 修改 | **PASS** | `static/frontend_zero_change.txt`（空）、`static/frontend_hashes.txt`（HEAD=WORK）、`static/git_status_code.txt`、`static/backend_diff_stat.txt`；前端请求/页面行为由 UI 套件全量覆盖 |
| A2 | 编译通过（dev1 报备 web-control） | **PASS** | dev1 编译 ×2 SUCCESS；`target/classes` 含 4 个新类；8080 接口全 200；测试方零重启 |
| A3 | 列表：total=9、rows=9、排序 order_no,id、第1/6/9 行正确 | **PASS** | `API-LIST-01`：ids=1..9、row1 修剪指甲/30.00、row6 洗头(禁用)、row9 心理咨询 |
| A4 | 搜索：`name=洗`→3 条；`status=0`→1 条；组合正确 | **PASS** | `API-LIST-03/04/05`（3 条 ids 6,7,8；1 条 id6；组合 2 条）；UI `SEARCH-01/03/04` |
| A5 | 分页：pageSize=5→5/4；超界 reasonable 返回末页 | **PASS** | `API-LIST-08`（page1=5、page2=4、page3 reasonable=末页 ids6–9，框架行为）；UI `PAGE-01/02/03`（12 行时第 2 页 2 行、切换 20/页） |
| A6 | 详情 id=1 字段与 OSS 图片 | **PASS** | `API-GET-01`：name/orderNo/unit/price=30.00/image(OSS)/nursingRequirement=无/status=1 |
| A7 | 新增成功、动态 id、total=10、DB create_by/create_time 有值 | **PASS（附工具说明）** | `API-ADD-01`：新 id=41（动态，>maxBefore=9）、列表 total=10、DB `create_by=1`、`create_time` 非空（证据串中时间被测试脚本取值缺陷截断，见 `db/NOTE_seedhash.md`，不影响判定）；UI `ADD-05` toast“新增成功”、total=10 |
| A8 | 重名 → code=500/msg“护理项目名称已存在”、非堆栈、列表不变 | **PASS** | `API-ADD-05`（code=500 msg=护理项目名称已存在，无堆栈）；UI `ADD-07`（toast 友好错误） |
| A9 | 修改全字段（createTime 不变） | **PASS** | `API-UPD-01`；UI `EDIT-01/02`（回填+保存+列表新值） |
| A10 | **启停部分更新**：`{id,status}` 成功且其它字段不被清空 | **PASS** | `API-UPD-02/03`（status 1→0→1，name/orderNo/unit/price/image/requirement 全保持）；UI `TOG-02/03/04`（抓包 PUT body=`{"id":51,"status":0}`，刷新后状态/字段持久） |
| A11 | 删除：单删成功、不存在友好错误、清理验证 | **PASS** | `API-DEL-01/02/03`（列表减1、DB 行不存在、清理后 total=9、种子哈希不变）；UI `DEL-01/02/03`（确认框文案/取消/确定/刷新一致） |
| A12 | `/all`：9 条 label/value(string)、排序、含禁用洗头；plan 页下拉可用 | **PASS** | `API-ALL-01/02/03`；UI `LINK-01/02/03`（`/all` 200 length=9；plan 新增弹窗下拉 9 项，首项修剪指甲，含洗头） |
| A13 | 权限：admin 正向全通；负向 N/A | **PASS（正向）/ N/A（负向）** | `API-AUTH-01`（admin `*:*:*` + 菜单/按钮权限）、`API-AUTH-03`（未认证写 body code=401 且不产生数据）；负向权限按 lead 决策本轮 N/A；`@PreAuthorize` 5 权限点经代码评审 |
| A14 | 前端页面：列表 10 列/9 行、增删改查启停按既有行为、前端零改动 | **PASS** | UI `LIST-01/02/03/04/05`、`ADD/EDIT/DEL/TOG/PAGE` 全通过；`UI-GUARD-02`（43 个 `/dev-api/nursing/project` API 请求 GET/POST/PUT/DELETE 全 200，正确区分 Vite 源码请求） |
| A15 | 图片上传本地兜底：url 非空、浏览器可打开、前端回填 | **PASS** | `API-UP-01`：`http://localhost:8080/profile/upload/2026/11/25/upload_sample_20261125091312A014.png` → GET 200 `image/png` 37984B；UI `ADD-04`（弹窗上传成功+预览）、`A16-01`（浏览器直接打开 url 200）；`api/upload_result.json` |
| A16 | 列表/弹窗图片显示（OSS 与本地 URL 共存） | **PASS** | UI `LIST-04`（9 条 OSS 缩略图，img src 为 OSS 绝对地址）、`ADD-04`（本地兜底 URL 缩略图）；`API-IMG-01`（OSS 图片 URL → 200 image/jpg 37984B） |

---

## 5. 关键业务断言（证据摘录）

1. **启停不丢字段（最关键）**：`API-UPD-02` → `status 1->0；其它字段保持: name=ZZT5-UPD-… price=50.00`；UI 抓包 `PUT body={"id":51,"status":0}`，刷新后单位/护理要求等原值不变。
2. **重名友好错误**：`API-ADD-05` → `code=500 msg=护理项目名称已存在`，响应无 Java 堆栈/SQL 泄露；UI toast 同文案。
3. **未认证约定**：RuoYi 返回 `HTTP 200 + body code=401`（如 `API-GET-04/ALL-04/AUTH-03`），非缺陷，已在测试计划 v1.2 固化口径。
4. **前端零改动**：`code/zzyl/zznursing-vue3-master` 目录 `git status/diff` 全空；`project.js`/`project/index.vue` hash 与 HEAD 完全一致；所有页面请求走 `/dev-api` 真实后端（非 mock）。
5. **UI 关键交互**：表头 `["编号","名称","排序号","单位","价格","图片","护理要求","状态","创建时间","操作"]`（10 列）；搜索“洗”3 条；“新增成功/修改成功/删除成功/禁用成功/启用成功” toast 均出现；删除确认框文案与前端一致。

---

## 6. 缺陷与观察项

### 6.1 产品缺陷
- **P1/P2/P3 产品缺陷：0 项**（无需 dev1 返修；web-arch 已 triage 确认首轮 2 FAIL 为口径问题）。

### 6.2 观察项（P3/不阻塞，仅记录）
| 编号 | 观察 | 影响 | 建议 |
|---|---|---|---|
| O-01 | 前端“新增护理项目”弹窗**状态 radio 无默认选中**（C++ UI 规格写“默认启用”）；提交后后端默认 `status=1`，功能正确 | 体验差异，不阻塞 | 前端不改（A1 约束）；若要一致可后续 Sprint 调前端；C++ 端按“默认启用”实现即可 |
| O-02 | 测试脚本 `Sql-One` 取值缺陷曾导致 evidence 中 create_time/seedHash 被截断（已修复，见 `db/NOTE_seedhash.md`） | 仅测试产物可读性 | 已修复；种子一致性由单语句复核 + web-control 独立 MD5 交叉验证 |

### 6.3 既有问题（非本 Sprint 范围，已知 R4）
- `/nursing/plan`、`/nursing/level` 页面壳正常，但**其自身后端 Controller 缺失**（`/nursing/plan/list`、`/nursing/level/list` 等仍 404/500）→ 属既有状态，不计本次回归缺陷；本次仅验收 `/nursing/project/all` 下拉恢复。
- 批量删除接口未实现：前端无批量入口、任务单明确单 id，**不作验收项**（`API-DEL-04` 仅记录契约外行为）。

### 6.4 文档偏差（已由 lead 转 web-arch 修正，归档）
- D1 `name=洗` 应为 **3 条**（原任务单写 1 条）；
- D2 新增 id **不硬编码**（以实际自增为准，本轮实测最大 id=51，测试后 MAX(id) 回 9）；
- D3 表头 **10 列 = 9 数据列 + 操作列**；
- D4 批量删除契约外（见 6.3）。

---

## 7. 测试数据与清理（R4 写库纪律）

| 项 | W1（API） | W2（UI） |
|---|---|---|
| 测试数据 | `ZZT5-API*` 等 ≤5 行（曾因脚本适配多轮重跑，均即时清理） | `ZZT5-UI*` 3 行（1 新增 + 2 分页前置） |
| 变更类型 | INSERT 5 / UPDATE 4 / DELETE 5（末轮） | INSERT 3 / UPDATE 3 / DELETE 3 |
| 收测状态 | **COUNT=9、MAX(id)=9、ZZT5 残留=0** | 同左 |
| 种子数据 | 9 条只读；内容哈希 `4a6dfdb56c4edc1040b6ce30ccdb8f66` 前后一致；web-control 独立 `SEED_MD5=7e9f6e46…` 与其基线一致 | 同左（`db/seed_verify_final.json`） |
| 自动日志 | `sys_logininfor`/`sys_oper_log` 少量新增（应用行为，未清理，web-control 记录备查） | 同左 |
| 副作用说明 | `AUTO_INCREMENT` 因多轮测试前移属正常；MySQL 8 `information_schema` 统计缓存值不可靠，以 `MAX(id)=9` 为准 | 同左 |

> lead 在 09:22 前后观察到的 `COUNT=12/MAX(id)=50` 为 **W2 测试过程中间态**（测试行尚未清理），最终已恢复 9 行，见 `db/seed_verify_final.json`。

---

## 8. 证据索引（`artifacts/test_web_s5/`）

| 目录/文件 | 内容 |
|---|---|
| `static/` | `git_status_code.txt`、`frontend_zero_change.txt`、`frontend_hashes.txt`、`backend_diff_stat.txt`、`diff_CommonController.txt`、`diff_NursingProjectMapper_xml.txt` |
| `api/api_results_20261125_091303.json` / `_latest.json` | API 47 用例结果（PASS/FAIL + 证据 + 基线/收测） |
| `api/upload_result.json` | A15 上传 url/HTTP/Content-Type/大小 |
| `ui/ui_results_ui_20261125012234.json` / `_latest.json` | UI 49 用例结果（证据 + 截图路径） |
| `ui/*.png` | **60 张**过程截图（登录/列表/搜索/新增/修改/启停/分页/联动/回归/失败现场） |
| `db/baseline.json`、`db/final.json`、`db/seed_verify_final.json`、`db/NOTE_seedhash.md` | 数据核验（含 test 脚本缺陷说明与权威哈希） |
| `logs/ui_stdout.log` | UI 套件控制台日志（含验证码交接时间点） |
| `../tools/` | 可复现测试工具：`api/api_suite.ps1`、`api/get_captcha.ps1`、`browser/ui_suite.mjs`、`README.md` |

---

## 9. 结论与建议

1. **验收结论：通过**。A1–A16 全部满足（A13 负向按 lead 决策 N/A）；核心契约（尤其 A10 启停部分更新、A15/A16 图片本地兜底、前端零改动）均有可复现证据，**可交付 lead / 主人验收**。
2. **建议（不阻塞）**：
   - 护理计划/护理等级页后端 Controller 缺失为下一 Sprint 优先项（本次已确认 `/nursing/project/all` 已恢复，plan 页下拉可用）；
   - 前端“状态”radio 默认选中与 C++ UI 规格对齐（前端修改需 lead 决策，本次未动）；
   - 批量删除如需支持，按 RuoYi 惯例用 `DELETE /{ids}` 另行立项；
   - 后续回归可直接复用 `artifacts/tools/` 两套脚本（API 免浏览器、UI 需 web-control 浏览器窗口）。
3. **流程留痕**：首轮 2 UI FAIL 经 arch triage 判为口径问题（附录 A），已修正复测；首轮 API 4 类失败为测试脚本适配问题，均为**测试工具问题，非产品缺陷**。

---

## 附录 A · 首轮误判与修正（web-arch triage 确认）

| 首轮 FAIL | 原因 | 修正 |
|---|---|---|
| `REG-06` 服务管理其它页 | 渲染启发式阈值误判（负责老人页实为完整渲染；plan/level 页面壳正常、接口 404 属既有 R4） | 改为 URL 校验 + 页面内容/关键元素存在性判定；**复测 PASS**，Known Issue 备注 |
| `UI-GUARD-02` 请求均走 /dev-api | 误把 Vite 源码模块请求（`/src/...vue/js`）与页面导航当 API | 只统计 `/dev-api/nursing/project` 的 xhr/fetch；**复测 PASS**（43 请求全 200，方法含 GET/POST/PUT/DELETE） |
| API 首轮 4 类失败 | curl `-X` 方法大小写、PS5.1 mysql stderr、价格字符串比较、RuoYi 未认证 HTTP200+body401 约定 | 测试脚本修正后 **47/47 PASS**；非后端缺陷 |

---

## 变更记录

| 版本 | 日期 | 变更 | 作者 |
|---|---|---|---|
| v1.0 | 2026-11-25 | 终版测试报告：A1–A16 验收矩阵、118/119 PASS + 1 N/A、P1/P2 缺陷 0、P3 观察 2 项、数据清理与证据索引、首轮误判说明 | web-tester |
