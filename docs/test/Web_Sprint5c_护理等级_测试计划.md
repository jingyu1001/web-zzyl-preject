# Web Sprint 5c · 护理等级模块 CRUD · 测试计划 v1.1

> **编写**：web-tester（Web 测试）｜**日期**：2026-11-25 晚场｜**状态**：**已对齐 Task-S5c-01 v0.1（D1 删除引用拒绝 / D2 禁用计划可关联 / D3 /all 全量）**；等 web-dev1 完成 + web-arch review PASS 通知后开测
> **被测对象**：服务管理-护理等级（`/nursing/level`）后端 6 接口 + 现有前端页面（**前端源码零改动**）
> **依据**：`src/api/nursing/level.js`、`views/nursing/level/index.vue`（只读核对）+ DB 实测 + lead 晚场任务要求；**arch 任务单发布后以任务单为准**
> **调度**：web-control（规则 v1.1）· 晚场从 `DQ-20261125-0007` 起；W1（API+MySQL 写）、W2（浏览器+MySQL 写）分段批准
> **交付**：报告 `docs/test/Web_Sprint5c_护理等级_测试报告.md`｜证据 `web-zzyl-preject/artifacts/test_web_s5c/`
> **纪律**：不 commit、不改前端/后端源码、不建表改表、不重启服务（**8080 现为用户 IDEA 实例 PID 31708**、88 PID 35760）；测试数据仅 `ZZT5C-*`，结束恢复 **level=3 / plan=3 / project_plan=6**

---

## 1. 测试目标

1. 验证护理等级 6 接口与契约一致：列表（含**执行护理计划 planName**）、搜索（name/status）、分页、详情、新增、修改、**启停部分更新**、删除、`/all`。
2. 验证前端 `/nursing/level` 在真实后端下按既有行为可用：列表 **3 行 8 列**、搜索/重置、**500px 弹窗 5 字段**（等级名称/护理计划下拉/费用/状态/等级说明）、修改回填、启停、删除确认；**前端零改动**。
3. 联动：护理等级页的“护理计划”下拉（`/nursing/plan/all` 3 项）可用；护理计划页 `count` 在测试等级创建/删除前后仍恢复为 1。
4. 回归：护理项目/护理计划/房型/用户页正常，`/nursing/project/*`、`/nursing/plan/*` 无路径冲突。
5. 数据/事务：名称唯一（DB UNIQUE）、费用/必填字段校验、**启停不清空其它字段**、删除（含被引用场景，最终口径以任务单为准）、`/all` 供入住配置页下拉（`item.name/id/fee`）。

**通过口径**：任务单验收项全过；无 P1/P2 缺陷；P3/P4 观察项与文档偏差记录不阻塞。

---

## 2. 范围

| 在范围 | 不在范围 |
|---|---|
| 6 接口：`GET /nursing/level/list`、`GET /{id}`、`POST`、`PUT`、`DELETE /{id}`、`GET /all` | 护理计划/护理项目后端（已 5b/5a 验收；仅回归） |
| 前端 `/nursing/level` 全交互（搜索/新增弹窗/修改回填/启停/删除） | 前端任何代码修改（**禁止**；仅登记） |
| 联动：`/nursing/plan/all` 下拉 3 项；护理计划页 count 联动；入住配置页 `/nursing/level/all` 下拉（若可打开） | 入住办理/入住配置 CRUD 本身（其接口既有状态，不在本轮） |
| 回归：护理项目/护理计划/房型/用户页 + 既有 API + 服务健康 | 导出 Excel（页面无按钮，按任务单不做）、C++ 端 |
| admin 正向权限、审计日志、安全健壮性、数据清理 | 负向权限（同 5a/5b 口径，lead 决策 N/A） |

---

## 3. 依据与契约基线

### 3.1 依据优先级
1. **web-arch《Sprint 5c 护理等级 CRUD 任务单》**（**待发布**；发布后以它为准，本计划 `[待任务单确认]` 项随之对齐）；
2. 前端源码实际调用（`src/api/nursing/level.js`、`views/nursing/level/index.vue`、`views/nursing/plan/index.vue`、`views/nursing/checkIn/components/ConfigurationForm.vue`，只读核对）；
3. DB 实测（只读，见 §3.5）；
4. lead 晚场任务要求。

### 3.2 接口契约（前端反推，待任务单确认出参细节）

| # | 方法/路径 | 入参 | 前端期望出参 | 权限点（sys_menu 实测） |
|---|---|---|---|---|
| 1 | `GET /nursing/level/list` | `pageNum,pageSize,name,status` | `TableDataInfo{total,rows[]}`；rows = **`NursingLevelVo`**（含 **`planName`**/fee/status/description/createTime）；`name like`、`status eq`；**`order by nl.id asc`**；`startPage()+getDataTable` | `nursing:level:list`(2007) |
| 2 | `GET /nursing/level/{id}` | path id | `AjaxResult{data:NursingLevelVo}`（**含 planName**，VoList + id 过滤）；不存在可 success(null) | `nursing:level:query`(2008) |
| 3 | `POST /nursing/level` | JSON `{name,planId,fee,status,description}` | `AjaxResult{code:200,msg}` | `nursing:level:add`(2009) |
| 4 | `PUT /nursing/level` | JSON 全字段 **或仅 `{id,status}` 启停** | `AjaxResult`；**部分更新**：null 不覆盖 | `nursing:level:edit`(2010) |
| 5 | `DELETE /nursing/level/{id}` | path 单 id（前端 `delLevel(row.id)`） | `AjaxResult`；**D1 默认拒绝**：被 `check_in_config.nursing_level_id` 引用时 → 友好错误“该护理等级已被入住配置引用，无法删除”；不存在 → “护理等级不存在” | `nursing:level:remove`(2011) |
| 6 | `GET /nursing/level/all` | 无 | `AjaxResult.data=**List<NursingLevelVo>**`、**全量 3 条（含禁用，D3）**、按 id asc、含 id/name/planName/fee/status；入住配置页消费 `item.name/id` 并取 `fee`（不能是 label/value） | `nursing:level:list`（或 query） |

### 3.3 前端可观测行为基线（源码只读核对）

| 项 | 源码事实（预期） |
|---|---|
| 路由/菜单 | `/nursing/level`，menu_id **2007**“护理等级”（`nursing:level:list`）；组件 `views/nursing/level/index.vue` |
| 搜索栏 | 等级名称（`name`，placeholder“请输入等级名称”，可回车）/ 状态（字典 `nursing_level_status`，可清空）+ 搜索/重置 |
| 表格 | **8 列**：序号(index,100)/护理等级名称(150)/**执行护理计划(planName)**/护理费用(元/月)/状态(120，tag：status===1 启用绿，否则禁用红)/等级说明/创建时间(180，**只显示 `yyyy-MM-dd`**)/操作（修改/删除/启用·禁用） |
| 分页 | 默认 10 条/页；“共 N 条” |
| 新增/修改弹窗 | **500px**；5 字段：等级名称*（input）/护理计划*（select，数据源 `/nursing/plan/all`，label=planName、value=id）/护理费用*（input-number **step=100**）/状态*（radio 字典，无默认选中）/等级说明*（input）；底部 确定/取消 |
| 前端校验 rules | `name`（等级名称不能为空）、`planId`（护理计划ID不能为空）、`fee`（护理费用不能为空）、`status`（状态不能为空）、`description`（等级说明不能为空）——**5 项全必填** |
| 修改回填 | `getLevel(id)` → `form=data`，`status=String(status)`；planId 用于下拉回显 |
| 启停 | `PUT {id, status: status==0 ? 1 : 0}`；确认框“**是否确认启用/禁用该护理等级？**”；toast“启用成功/禁用成功” |
| 删除 | 确认框“**是否确认删除这条护理等级？**”→ DELETE → toast“删除成功” |
| 提交 | 成功 toast“新增成功/修改成功”→ 关闭弹窗 → 刷新列表 |
| 无 | 无“查看”按钮；无 count 禁用逻辑（修改/删除始终可点）；无导出按钮 |
| `/all` 消费方 | `views/nursing/checkIn/components/ConfigurationForm.vue`：`nursingLevelIdOptions=res.data`；`:label="item.name" :value="item.id"`；选中后 `obj.fee` 回填护理费用 → **/all 至少需 id/name/fee** |

### 3.5 DB 基线（只读实测，2026-11-25 22:2x）

**nursing_level（3 行，AUTO_INCREMENT=79，`name` 有 UNIQUE 索引）**

| id | name | plan_id | fee | status | description | create_time | update_time |
|---|---|---|---|---|---|---|---|
| 76 | 1号护理等级 | 133 | 1500.00 | 1 启用 | 我是1号哦 | 2024-08-14 16:33:16 | 2026-04-13 22:26:12 |
| 77 | 2号护理等级 | 134 | 2000.00 | 1 启用 | 我是2号哦 | 2024-08-20 11:18:21 | 2026-03-04 15:08:57 |
| 78 | 3号护理等级 | 135 | 3000.00 | 1 启用 | 我是3号哦 | 2024-08-29 16:58:00 | 2025-06-09 21:03:09 |

- 表结构：`id int AI`、`name varchar(50) NOT NULL UNIQUE`、`plan_id int NOT NULL`、`fee decimal(10,2) NOT NULL`、`status tinyint(1) NOT NULL DEFAULT 1`、`description varchar(255) NULL`、`create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP`、`create_by/update_by bigint`、`update_time ON UPDATE CURRENT_TIMESTAMP`。
- 字典 `nursing_level_status`：**启用=1、禁用=0**（与 DB 一致，无 5b 的 O-1 问题）。
- **引用关系**：`check_in_config.nursing_level_id`（1 行 → **level 77**）为真实引用；`check_in.nursing_level_name` 仅字符串名称（非外键）。**该引用是否阻止删除 `[待任务单确认]`**。
- 关联：`nursing_plan=3`（133/134/135，均 count=1）、`nursing_project_plan=6`。
- 其它：`/nursing/level/all` 同时服务入住配置页下拉；列表页“执行护理计划”列依赖 `planName`（XML `selectNursingLevelVoList` 已含 `join nursing_plan ... plan_name planName` 查询，可直接复用）。

### 3.6 已确认口径映射（Task-S5c-01 v0.1）

| # | 待确认项 | 本计划默认处理 |
|---|---|---|
| C1 | list `order by nl.id asc`（76/77/78）；`startPage()+getDataTable` | §3.2-1/§4.7 |
| C2 | list/detail/all 均返回 **NursingLevelVo**（含 planName）；列表/详情统一 VoList | §3.2/§4.7 |
| C3 | 详情 = VoList + **id 过滤**（含 planName）；不存在 `success(null)` | §3.2-2/§4 |
| C4 | 后端校验：name trim/必填/**≤50**/唯一；planId 必填+存在；fee 必填 **≥0**；status 默认 1；description trim 必填/≤255 | §4.1–§4.5 |
| C5 | planId **存在性校验**（NursingPlanMapper），不存在→“护理计划不存在”；**不校验计划启用状态**（D2：134 禁用也可关联） | §4.2/A9 |
| C6 | **D1 删除引用校验**：`check_in_config.nursing_level_id` 引用 >0 → 友好错误“该护理等级已被入住配置引用，无法删除”；测试删除用新增等级，种子 77 预期被拒 | §4.8/A10 |
| C7 | 判定 name/planId/fee/description 均 null 且 status!=null → 仅更新 status+updateTime，不覆盖其它、不做引用拦截 | §4.6/A7 |
| C8 | `/all` = **List<NursingLevelVo> 全量 3 条（含禁用，D3）**、按 id asc、含 id/name/planName/fee/status | §3.2-6/§4 |
| C9 | 导出不做（页面无按钮，任务单未要求） | §1 |

---

## 4. 环境、工具与前置

| 项 | 值/策略 |
|---|---|
| 前端 88 | `http://localhost:88`，PID **35760**（未变） |
| 后端 8080 | `http://localhost:8080`，**用户 IDEA 实例 PID 31708**（16:49 重启，devtools）；**不得停/重启**；窗口内需稳定 |
| MySQL 3306 | `zzyl`：`nursing_level`/`nursing_plan`/`nursing_project_plan`；只读免申请，写需窗口 |
| 浏览器自动化 | 无头 Edge（playwright-core@1.65.1 已装 `artifacts/tools/browser/`）；W2 独占，结束无残留 |
| 验证码 | `captchaEnabled=true`；沿用 **Redis 只读 `GET captcha_codes:{uuid}`** 交接（只读不写不删，已预报备） |
| 工具 | 复用 5b 框架：`artifacts/tools/api/level_api_suite.ps1`（由 plan 版改造）、`artifacts/tools/browser/level_ui_suite.mjs`（由 plan UI 版改造） |
| 证据目录 | `artifacts/test_web_s5c/{static,api,ui,db,logs,tmp}`（自建） |

**前置条件（缺一不测）**：① arch 任务单发布；② dev1 完成 + 编译通过（轻量报备）；③ arch review PASS；④ 8080 已加载新 Controller（接口探测）；⑤ W1/W2 窗口批准。

---

## 5. 测试数据设计（写库申报口径）

| 项 | 内容 |
|---|---|
| 命名规范 | 等级名称统一 **`ZZT5C-`** 前缀 + 用途 + 时间戳（长度 ≤50，`name` UNIQUE） |
| 新增条数 | W1：2–3 条测试等级（关联不同 plan_id）；W2：1 条（UI 新增/修改/启停/删除用）；合计 ≤4 |
| 修改 | 仅测试等级：全字段/启停 `{id,status}`；种子只读（除任务单要求的引用删除验证，按快照+恢复执行） |
| 删除 | 测试等级经 DELETE 清理；**不实际删除种子 76/77/78**（77 被 check_in_config 引用；若任务单要求验证被引用删除，先快照、失败回滚） |
| 预计影响行数 | `nursing_level` INSERT ≤4 / UPDATE ≤6 / DELETE ≤4（净增 0，回 3 行）；**`nursing_plan`/`nursing_project_plan`/`check_in_config` 只读（0 变更）**；A10 会尝试 DELETE 种子 77（预期被拒、数据不变；先快照，若误删按快照 SQL 恢复） |
| 回滚/兜底 | 接口异常时按申报 `DELETE FROM nursing_level WHERE name LIKE 'ZZT5C-%';`；若涉及种子 77/78 异常，按开测前快照 SQL 恢复 |
| 自动副作用 | `sys_logininfor`/`sys_oper_log` 少量（应用行为，不清理，记录备查） |
| 基线快照 | 开测前 `SELECT` 3 行 + 三表 CHECKSUM/哈希；收测比对（含 `nursing_plan=3`、`nursing_project_plan=6`） |

---

## 6. 执行策略与窗口预案

```
开窗前（免申请）：STA 静态核查（前端零改动/后端改动范围/编译/代码评审/契约核对）
W1（API，10 分钟；资源：8080 稳定 + MySQL 写）
  S1 健康 + 登录 token → 只读 API（list/搜索/分页/详情/all/401）
  S2 写 API（add/唯一名冲突/必填校验/update 全字段+启停部分/delete）→ 只读 DB 校验 → 清理
  S3 回归 API + 审计日志 + 数据恢复核验（3/3/6）→ 释放
W2（UI，15 分钟；资源：浏览器自动化 + 8080 只读 + MySQL 写）
  S4 登录 → 服务管理/护理等级 → 列表 3 行 8 列/状态/创建时间/分页/搜索
  S5 新增（500px 弹窗 5 字段+计划下拉 3 项+校验+成功）→ 修改回填/保存 → 启停 → 删除
  S6 联动（护理计划页 count 恢复 1；`/nursing/plan/all` 可开）→ 回归（护理项目/护理计划/房型/用户）→ 清理 → 释放
W3（复测，按需 ≤10 分钟）：缺陷复测/补证
收尾：数据恢复 3/3/6、哈希比对、证据归档、报告、汇报 lead
```

**申请文案预案**（批准后发 web-control）：
```
W1【API】web-tester | 护理等级 CRUD API 验收 + 测试数据清理 | 后端8080稳定（用户实例 PID 31708，不重启）+ MySQL写（nursing_level 仅 ZZT5C-*，净增0回3行；plan/project_plan/check_in_config 只读） | 10 分钟 | 不重启 | 否
W2【UI】web-tester | 护理等级 UI 浏览器验收（列表/搜索/新增弹窗/修改回填/启停/删除/联动/回归+截图） | 浏览器自动化（无头 Edge）+ 8080只读 + MySQL写（ZZT5C-*） | 15 分钟 | 同 W1 | 否
```

---

## 7. 用例清单

> 优先级：P0 冒烟/核心｜P1 主要功能/契约｜P2 边界/异常｜P3 观察（不阻塞）
> 依据列：`TS`=任务单（待发布映射）｜`F`=前端源码｜`DB`=DB 实测｜`K`=§3.6 待确认口径

### 7.1 静态核查 STA（开窗前，免申请）

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| STA-01 | P0 | 前端目录 `git status/diff`；关键文件 hash vs HEAD | **前端零改动**（`level.js`/`level/index.vue`/`plan.js` 等） | 任务要求/F |
| STA-02 | P0 | 后端改动范围（`git status --porcelain code/`） | 仅约定文件：疑似新增 `INursingLevelService`/`NursingLevelServiceImpl`/`NursingLevelController`；`NursingLevelMapper.java` 补方法（task 单为准）；`NursingLevelMapper.xml` 如需补 order by | 任务单（待） |
| STA-03 | P1 | 代码评审：6 接口路径/方法/参数、`extends BaseController`、`@PreAuthorize` 4~5 权限点、`@Log`、`startPage/getDataTable` | 与任务单/契约一致 | TS |
| STA-04 | P1 | 编译状态（dev1 报备；测试方不主动触发 devtools 重启） + 8080 已加载（接口探测） | 编译通过、新类存在、服务可用 | TS |
| STA-05 | P1 | 未改配置/表结构/其它模块；`/nursing/level/*` 与 project/plan 路径无冲突；Mapper namespace 一致 | 符合 | TS |
| STA-06 | P1 | 前端 6 URL 与契约一致 | 一致 | F |
| STA-07 | P1 | 既有 `/nursing/plan/all`、`/nursing/project/all` 健康（本模块联动依赖） | 200 | 5b 基线 |

### 7.2 API 层

**A. 列表与搜索**

| ID | 级别 | 步骤/请求 | 预期 | 依据 |
|---|---|---|---|---|
| API-LIST-01 | P0 | `GET /nursing/level/list?pageNum=1&pageSize=10` | `code=200,total=3,rows=3`；行 id **76/77/78**；**每行含 `planName`**（执行护理计划：1号/2号/3号护理计划）、name/fee/status/description/createTime；status 均 1 | A/DB/K2 |
| API-LIST-02 | P1 | 排序校验 | 默认 id asc（76/77/78）；任务单另有排序以任务单 | K1 |
| API-LIST-03 | P1 | `GET list?name=1号` | total=1，id=76（模糊 like） | F |
| API-LIST-04 | P1 | `GET list?name=等级` | total=3（子串匹配） | F |
| API-LIST-05 | P1 | `GET list?status=1`；`status=0` | status=1 → 3 条；status=0 → 0 条（基线全启用） | DB |
| API-LIST-06 | P1 | `GET list?name=不存在XYZ` | total=0，rows=[] | F |
| API-LIST-07 | P1 | 分页：`pageSize=2` page1/page2；`pageNum=3` 超界 | page1=2、page2=1、page3 reasonable=末页 1 条；total 均 3（框架行为，参照 5a/5b） | K1 |
| API-LIST-08 | P2 | 组合：name=等级&status=1 | total=3 | F |
| API-LIST-09 | P2 | 非法分页 `pageNum=0/pageSize=0/pageSize=1000` | 不 500，合法结构 | F |

**B. 详情**

| ID | 级别 | 步骤/请求 | 预期 | 依据 |
|---|---|---|---|---|
| API-GET-01 | P0 | `GET /nursing/level/76` | `data.id=76,name=1号护理等级,planId=133,**planName=1号护理计划**,fee=1500.00,status=1,description=我是1号哦,createTime` | A4/C3 |
| API-GET-02 | P1 | 详情 77/78 | plan_id=134/135、fee=2000/3000、description 对应 | DB |
| API-GET-03 | P1 | `GET /nursing/level/999999` | `code=200,data=null`（前端可提示） | F |
| API-GET-04 | P2 | `GET /nursing/level/abc` | JSON 错误，无堆栈 | 健壮 |
| API-GET-05 | P1 | 未认证详情/list | body `code=401`（RuoYi 约定 HTTP200+body401），不返回数据 | 安全 |

**C. 新增**

| ID | 级别 | 步骤/请求 | 预期 | 依据 |
|---|---|---|---|---|
| API-ADD-01 | P0 | POST `{name:"ZZT5C-新增-<ts>",planId:133,fee:2500,status:1,description:"ZZT5C测试说明"}` | code=200；列表 total=4；新 id=**实际自增（当前 79 起，不硬编码）**；DB 1 行、字段正确、create_time 有值（create_by 允许 NULL 或按任务单） | A5/DB/K4 |
| API-ADD-02 | P1 | 缺 name / 缺 planId / 缺 fee / 缺 description（分 4 次） | 按任务单：友好错误（前端 5 项必填）；**记录实际**；无脏数据（失败场景） | K4/F |
| API-ADD-03 | P1 | 重名（“1号护理等级”） | 友好错误“护理等级名称已存在”类（非堆栈）；无新增 | DB UNIQUE |
| API-ADD-04 | P2 | name 超长（51+，varchar(50)）/fee 负数或 0 /status 非法值 | 记录实际（DB 约束/友好错误/接受）；按任务单判定 | K4 |
| API-ADD-05 | P1 | planId 不存在（999999） | **友好错误“护理计划不存在”、无新增**（C5/任务单 §4.2） | A9/C5 |
| API-ADD-06 | P2 | fee 精度（1234.56）/小数超 2 位 | 按 decimal(10,2) 记录实际 | DB |
| API-ADD-07 | P2 | 非法 JSON | JSON 错误 4xx/200+code，无堆栈 | 健壮 |
| API-ADD-09 | P1 | POST planId=134（该计划 **status=0 禁用**） | **允许新增**（D2：仅校验存在、不校验启用；种子 level 77 即关联 134）→ 200，列表 planName=2号护理计划 | A9/D2 |
| API-ADD-08 | P3 | 新增未传 status | 按任务单默认（DB 默认 1 / 前端默认无）→ 记录实际 | K4 |

**D. 修改（全字段 + 启停部分更新）**

| ID | 级别 | 步骤/请求 | 预期 | 依据 |
|---|---|---|---|---|
| API-UPD-01 | P0 | PUT 测试等级全字段：改 name/fee/planId/description | code=200；列表/详情字段更新（**planName 随 planId 联动**）；**create_time 不变**、update_time 有值 | A6/C7 |
| API-UPD-02 | P0 | **启停部分更新**：PUT **仅** `{id:测试id,status:0}` | code=200；详情 status=0，且 **name/planId/fee/description 全部保持** | A7/K7（关键） |
| API-UPD-03 | P1 | 再 PUT `{id,status:1}` 恢复 | status=1，其它字段仍保持 | K7 |
| API-UPD-04 | P1 | PUT 不存在 id | 友好错误，不误更新 | F |
| API-UPD-05 | P1 | PUT 改名与种子重名 | 友好错误；原值不变 | DB UNIQUE |
| API-UPD-06 | P1 | PUT 自身当前名（排除自身） | code=200 | K |
| API-UPD-07 | P2 | PUT 缺 id / 非法 JSON | JSON 错误，无堆栈 | 健壮 |
| API-UPD-08 | P2 | 修改 planId 到另一有效计划 | 200；list 的 planName 列变化正确（联动 join） | K2/K5 |

**E. 删除**

| ID | 级别 | 步骤/请求 | 预期 | 依据 |
|---|---|---|---|---|
| API-DEL-01 | P0 | 删除新增测试等级 | code=200；列表 total 减 1；DB 行不存在 | A8/DB |
| API-DEL-02 | P1 | DELETE 不存在 id | 友好错误；无堆栈 | F |
| API-DEL-03 | P0 | 清理全部 ZZT5C-* 后核验 | `nursing_level=3`；`nursing_plan=3`、`nursing_project_plan=6`；种子哈希不变；ZZT5C 残留=0 | 数据纪律 |
| API-DEL-04 | P0 | **DELETE 种子 77**（被 check_in_config 引用 1 条） | **友好错误“该护理等级已被入住配置引用，无法删除”**且数据不变（77 与其字段不变、check_in_config 不变）。安全执行：先快照；若实现缺失导致误删，立即按快照 SQL 恢复并记 **P1** | A10/C6 |
| API-DEL-05 | P3 | 批量删除 `{id1,id2}`（契约外观察，前端无入口） | 记录实际；不计缺陷 | 参考 5a |

**F. `/all`**

| ID | 级别 | 步骤/请求 | 预期 | 依据 |
|---|---|---|---|---|
| API-ALL-01 | P0 | `GET /nursing/level/all` | code=200；`data` 数组 length=3；每项含 **id/name/fee**（入住配置页选等级回填费用所需）；状态/排序按任务单 | K8/F |
| API-ALL-02 | P1 | 类型断言 | id number、name string、fee number；是否含 status 记录 | K8 |
| API-ALL-03 | P1 | 新增测试等级后 `/all` | length=4 含测试项；清理后回 3 | K8 |
| API-ALL-04 | P1 | 未认证 `/all` | body code=401 | 安全 |

**G. 权限 / 安全 / 审计**

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| API-AUTH-01 | P0 | admin token 调 6 接口 | 全通；`/getInfo` 权限（超管 `*:*:*` 或 5 权限点） | A14 |
| API-AUTH-02 | P1 | 未认证写（POST/PUT/DELETE） | body code=401，不产生数据 | 安全 |
| API-AUTH-03 | P2 | 代码评审 5 权限注解与 sys_menu 2007–2012 对齐 | 一致 | TS |
| API-SEC-01 | P1 | name 注入串 `' OR '1'='1` 搜索 | 不改变语义、无 SQL 异常 | 安全 |
| API-SEC-02 | P1 | 错误响应审查 | JSON `{code,msg}`，无 Java 堆栈/路径/SQL 泄露 | 安全 |
| API-SEC-03 | P2 | 写操作后只读查 `sys_oper_log` | 存在“护理等级”INSERT/UPDATE/DELETE 记录（@Log） | TS |

### 7.3 UI 层（Playwright 无头 Edge）

**登录/导航**

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| UI-LOGIN-01 | P0 | 打开 88 → /login 渲染 | 标题/三输入框 | F |
| UI-LOGIN-02 | P1 | 空提交校验（清空预填后） | 校验提示（账号/密码/验证码） | F |
| UI-LOGIN-03 | P0 | admin+验证码登录 | 成功跳 /index | 环境 |
| UI-NAV-01 | P1 | 服务管理→护理等级 | 进入 `/nursing/level`；页签“护理等级” | F/2007 |
| UI-NAV-02 | P2 | 直接访问 `/nursing/level` | 正常加载 | F |

**列表**

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| UI-LIST-01 | P0 | 列表渲染 | **3 行**；1/2/3号护理等级；状态 tag 全部“启用” | DB/F |
| UI-LIST-02 | P1 | 表头 | **8 列**：序号/护理等级名称/**执行护理计划**/护理费用(元/月)/状态/等级说明/创建时间/操作；**planName 列有值**（1号/2号/3号护理计划）；创建时间格式 `yyyy-MM-dd` | F |
| UI-LIST-03 | P1 | 分页 | “共 3 条”、10条/页、单页无翻页 | F |
| UI-LIST-04 | P2 | 控制台 | 无未捕获 JS 异常 | F |

**搜索**

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| UI-SEARCH-01 | P1 | 名称“1号”→搜索；重置 | 1 行（76）；重置回 3 行 | F |
| UI-SEARCH-02 | P2 | 名称不存在 → 空态 | “暂无数据”、total 0 | F |
| UI-SEARCH-03 | P2 | 回车触发搜索 | 同点击搜索 | F |

**新增（核心）**

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| UI-ADD-01 | P0 | 点“新增” | 弹窗宽 500、标题“添加护理等级”；5 字段（等级名称/护理计划/护理费用/状态/等级说明）+ 确定/取消 | F |
| UI-ADD-02 | P0 | 空表单点确定 | 前端 5 项必填校验（等级名称/护理计划ID/护理费用/状态/等级说明），弹窗不关、无 POST | F |
| UI-ADD-03 | P1 | 护理计划下拉 | 展开 **3 项**（1号/2号/3号护理计划，来自 `/nursing/plan/all`）；选中显示名称 | F/5b |
| UI-ADD-04 | P2 | 费用控件 | input-number step=100；输入非数字/负数记录控件行为 | F |
| UI-ADD-05 | P0 | 填合法：名称 `ZZT5C-UI-<ts>`、计划1号、费用 2500、状态启用、说明 → 确定 | toast“新增成功”；弹窗关；列表 **total=4**；新行 planName=1号护理计划 | F/TS |
| UI-ADD-06 | P1 | 新增重名（“1号护理等级”）→ 确定 | 友好错误 toast；无新增行 | DB UNIQUE |
| UI-ADD-07 | P2 | 取消新增 | 弹窗关、无请求、列表不变 | F |

**修改/启停/删除（用新增的测试等级）**

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| UI-EDIT-01 | P0 | 对新测试等级点“修改” | 标题“修改护理等级”；回填 name/护理计划下拉选中/费用/状态 radio/说明 | F |
| UI-EDIT-02 | P0 | 改名/改费用/改说明 → 确定 | toast“修改成功”；列表新值 | F/TS |
| UI-EDIT-03 | P1 | 修改取消 | 数据未变、无 PUT | F |
| UI-EDIT-04 | P1 | 修改为已存在名称 | 友好错误 toast；原名不变 | DB UNIQUE |
| UI-TOG-01 | P0 | 点“禁用”→确认框 | 文案“是否确认禁用该护理等级？”；取消不生效 | F |
| UI-TOG-02 | P0 | 确认禁用 | toast“禁用成功”；tag 变禁用；**抓包 PUT 仅 `{id,status}`**；其它字段不变 | K7（关键） |
| UI-TOG-03 | P1 | 再启用 | toast“启用成功”；状态恢复；刷新后持久 | F |
| UI-DEL-01 | P0 | 点“删除”→确认框 | 文案“是否确认删除这条护理等级？”；取消不删 | F |
| UI-DEL-02 | P0 | 确认删除 | toast“删除成功”；列表 total 回 3；刷新后一致 | F/A8 |

**前端零改动护栏**

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| UI-GUARD-01 | P0 | 测试前后 git 状态/hash | 前端零改动 | 任务要求 |
| UI-GUARD-02 | P1 | 抓包 `/nursing/level/*` 均走 `/dev-api`、方法 GET/POST/PUT/DELETE、状态 200 | 与 `level.js` 6 URL 一致；排除 `/src/*`、`@vite`、导航 | F |
| UI-GUARD-03 | P2 | 关键页控制台无严重异常 | 无 | F |

### 7.4 联动

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| LINK-01 | P0 | 护理等级新增弹窗“护理计划”下拉 | 3 项（`/nursing/plan/all`，5b 已恢复），选中 1号护理计划 | F/5b |
| LINK-02 | P1 | 护理计划页 `/nursing/plan`：测试等级创建期间/删除后 | 测试等级关联 1号计划时，计划页 count 由 1→2；删除后回 1（最终恢复基线） | lead 要求/DB |
| LINK-03 | P2 | 入住配置页（`/enterQuit/checkIn`→详情/入住配置）`/nursing/level/all` 下拉 | 若可打开：下拉 3 项（item.name），选中回填 fee；该页自身其它接口缺失属既有状态 | F/K8 |

### 7.5 回归（只读）

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| REG-01 | P0 | 8080/88 健康（端口/PID/首页 200） | 正常；8080 PID 31708 记录 | 环境 |
| REG-02 | P0 | 护理项目页 `/nursing/project` | 列表 9 行正常 | 5a |
| REG-03 | P1 | 护理计划页 `/nursing/plan` | 列表 3 行、count=1 恢复基线 | 5b |
| REG-04 | P1 | 房型设置 `/liveIn/houseSet` | 列表 6 行 | 5a |
| REG-05 | P1 | 用户管理 `/system/user` | 列表 7 行 | 5a |
| REG-06 | P1 | 既有 API：`/nursing/project/list`、`/nursing/plan/list`、`/nursing/plan/all`、`/elder/roomType/list`、`/system/user/list` | 200 且数量正常（9/3/3/6/7）；无路径冲突 | 5a/5b |

### 7.6 非功能

| ID | 级别 | 步骤 | 预期 |
|---|---|---|---|
| NF-01 | P1 | 未登录访问 `/nursing/level` | 跳登录页；API 401 |
| NF-02 | N/A | 受限账号负向权限（同 5a/5b 口径） | lead 决策本轮 N/A（待任务单确认） |
| NF-03 | P1 | 审计/登录日志只读核对 | 有对应记录 |
| NF-04 | P2 | 错误响应无堆栈/路径泄露 | 通过 |
| NF-05 | P2 | 注入字符不改变语义 | 通过 |
| NF-06 | P2 | 页面控制台无严重异常 | 通过 |

### 7.7 任务单验收映射（A1–A14，报告矩阵用）

| 任务单 | 用例映射 | 关键断言 |
|---|---|---|
| A1 静态/变更范围 | STA-01..07 | 前端零改动；后端仅 Mapper 声明 + XML VoList 改动 + 3 新文件 |
| A2 列表 | API-LIST-01/02 + UI-LIST-01/02 | total=3 rows=3；ids 76/77/78；**planName**/fee/status/description/createTime |
| A3 搜索/分页 | API-LIST-03..09 + UI-SEARCH-01..03 | name=2号→1；status=1→3、status=0→0；pageSize=2→2/1 |
| A4 详情 | API-GET-01/02 | 77 → planId=134、planName=2号护理计划、fee=2000、description |
| A5 新增 | API-ADD-01..04 + UI-ADD-05 | 200、total=4、动态新 id、DB 行存在、create_time 有值、create_by 允许 NULL |
| A6 整表单修改 | API-UPD-01 + UI-EDIT-01/02 | 字段全生效、planName 随 planId 联动、createTime 不变 |
| A7 启停部分更新（关键） | API-UPD-02/03 + UI-TOG-02/03 | 仅 status 变；name/planId/fee/description 不变；抓包 PUT 仅 id/status |
| A8 名称唯一 | API-ADD-03、API-UPD-05/06 + UI-ADD-06/EDIT-04 | 友好错误、无堆栈、排除自身 |
| A9 planId 校验 | API-ADD-05/09 | 999999→“护理计划不存在”；**134 禁用计划允许**（D2） |
| A10 删除 | API-DEL-01/02/04 + UI-DEL-01/02 | 新等级删除 200；种子 77 被引用→友好拒绝且数据不变；不存在→“护理等级不存在” |
| A11 `/all` | API-ALL-01..04 + LINK-03 | List<NursingLevelVo> 3 条、id asc、含 id/name/planName/fee/status |
| A12 前端页面 | UI 全部 + UI-GUARD-01/02 | 3 行 8 列、“执行护理计划”列 planName、增改删启停、前端零改动 |
| A13 权限 | API-AUTH-01..03 | admin 正向全通；负向 N/A |
| A14 数据恢复 | API-DEL-03 + 收尾核验 | level=3、plan=3、project_plan=6、check_in_config 不变、ZZT5C=0 |

**预计用例数**：STA 7 + API 42 + UI 24 + LINK 3 + REG 6 + NF 6 ≈ **88**（最终以执行结果为准）。

---

## 8. 缺陷分级与判定口径

| 等级 | 定义 | 处理 |
|---|---|---|
| **P1 阻塞** | 核心闭环不可用/服务异常/数据被破坏/契约关键项不符（如列表无 planName、启停清空字段、删除不生效、前端被改） | 必修+复测，阻塞验收 |
| **P2 严重** | 主要功能缺陷或契约不符、数据/安全风险（名称唯一无友好提示、部分更新覆盖字段、`/all` 缺 fee 导致入住配置回填失败、权限缺失） | 验收前修复或 lead 书面豁免 |
| **P3 一般** | 次要功能/边界/提示不友好（超长名报 SQL 错、缺字段无后端校验、非法 planId 产生隐藏数据） | 记录不阻塞，下 Sprint 修 |
| **P4 观察** | 文档偏差、前端既有 quirk（无查看按钮、状态无默认选中）、AUTO_INCREMENT 副作用 | 记录不判缺陷 |
| **ENV** | 环境阻塞（8080 热重启/用户实例不可用、浏览器不可用） | 按 lead 决策记录 |

**首轮失败处理**：同 5a/5b 经验——**先判脚本适配问题（选择器/断言/取值/超时），修正脚本后复测；仍失败再判产品缺陷**并交 arch triage。

---

## 9. 风险与应对

| 风险 | 应对 |
|---|---|
| arch 任务单未发布 | 按本计划准备；发布后 10 分钟内对齐 `[待任务单确认] K1–K9` 并升版 |
| 8080 为用户 IDEA 实例（**PID 31708**） | 窗口内不重启/不编译；devtools 热重启由他人报备；发现异常报 web-control，不擅自处理（W1/W2 申请中已写明“不重启”） |
| 删除被引用等级（77 被 check_in_config 引用） | 默认不删种子；若任务单要求，先快照+失败回滚，窗口申报声明 |
| planId 无外键导致隐藏数据 | K5 专项记录；若任务单要求校验则断言友好错误 |
| 写库污染/误删种子 | 仅 `ZZT5C-*`；三表基线快照+哈希；收测比对 3/3/6 |
| 浏览器/MySQL 争用 | 按 web-control 分段批准+优先续批；结束立即释放 |
| 首轮脚本失败误判产品缺陷 | 先按 5b 经验排查脚本（选择器/断言/序列化），修正后复测 |

---

## 10. 报告结构与交付

`docs/test/Web_Sprint5c_护理等级_测试报告.md` 必含：
1. 摘要：用例统计（按层/优先级）+ 结论（通过/有条件通过/不通过）；
2. 环境/窗口（DQ 编号、实际时长、影响行数、三表 CHECKSUM/哈希、前端零改动证据）；
3. 缺陷清单（BUG-5C-xx：严重级/复现步骤/实际 vs 预期/证据）；
4. 任务单验收矩阵（按 arch 发布编号逐项 PASS/FAIL/N/A）+ `[待确认]` 结论；
5. 观察项/文档偏差/前端 quirk；
6. 数据清理与恢复核验（level=3/plan=3/project_plan=6、ZZT5C=0、AUTO_INCREMENT 记录）；
7. 结论与建议 + 证据索引（`artifacts/test_web_s5c/`）。

---

## 11. 变更记录

| 版本 | 日期 | 变更 | 作者 |
|---|---|---|---|
| v1.0 | 2026-11-25 | 首版：范围/前端契约/DB 基线（3 行+引用关系+字典 1/0）/待确认 K1–K9/86 用例/窗口预案/风险/报告结构 | web-tester |
| v1.1 | 2026-11-25 | **Task-S5c-01 v0.1 发布后对齐**：K1–K9 → 已确认口径 C1–C9（Vo+planName、id asc、planId 存在性+D2 禁用可关联、D1 删除引用拒绝、启停部分更新、/all 全量 Vo、R2 审计）；新增 §7.7 A1–A14 映射；API 用例补 A9/A10/A6 联动；窗口申请强调 8080 PID 31708 不重启 | web-tester |
