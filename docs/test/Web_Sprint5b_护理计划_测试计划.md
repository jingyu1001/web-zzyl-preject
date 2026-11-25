# Web Sprint 5b · 护理计划模块 CRUD · 测试计划 v1.2

> **编写**：web-tester（Web 测试）｜**日期**：2026-11-25｜**状态**：**任务单 v0.2 已发布并对齐（含 lead 决策：R2 create_by 不要求、/all 返回 List<NursingPlan>、count 强制校验、status 不限）**；等 web-dev1 完成 + arch review PASS 通知后开测
> **被测对象**：服务管理-护理计划（`/nursing/plan`）**后端 6 接口**（`nursing_plan` + `nursing_project_plan` 两张表）+ 现有前端页面（**前端源码零改动**）
> **权威依据**：`docs/tasks/Web_Sprint5b_护理计划CRUD_任务单.md`（**v0.2**，A1–A15）→ 本计划 §7.7 已映射
> **调度**：web-control（规则 v1.1）· 下午场从 `DQ-20261125-0004` 起；W1（API+MySQL 写）、W2（浏览器+MySQL 写）分段批准
> **交付**：报告 `docs/test/Web_Sprint5b_护理计划_测试报告.md`｜证据 `web-zzyl-preject/artifacts/test_web_s5b/`
> **纪律**：不 commit、不改前端/后端源码、不建表改表、不重启 88/8080/3306/6379；测试数据仅 `ZZT5B-*`，结束恢复基线 **3/6 行**（A8 临时改动按快照回滚）

---

## 1. 测试目标

1. 验证护理计划 6 接口与任务单契约一致：列表（含 `count` 引用次数）、详情（含子表）、新增（主表+子表）、修改（子表替换）、删除（级联子表）、启停部分更新、`/all`。
2. 验证前端 `/nursing/plan` 页面在真实后端下按**既有行为**可用：列表 3 行、搜索、新增（弹窗 1000px + 护理项目子表 4 列/可增删）、修改回填、查看只读、删除确认、启停；`count>0` 行修改/删除按钮禁用样式；**前端零改动**。
3. 验证 `/nursing/plan/all` 上线后，护理等级页（`/nursing/level`）的“护理计划”下拉恢复可用。
4. 回归：护理项目/房型/用户页与 `/nursing/project/*`、既有 API 不受新 Controller 影响。
5. 数据与事务：名称唯一（DB 唯一索引）、新增/修改**事务原子性**（子表失败不产生半截数据）、删除**级联清理子表**、启停不清空其它字段。

**通过口径**：任务单验收项全过（待 arch 发布后对齐编号）；无 P1/P2 缺陷；P3/P4 观察项与文档偏差记录不阻塞。

---

## 2. 范围

| 在范围 | 不在范围 |
|---|---|
| 6 接口：`GET /nursing/plan/list`、`GET /{id}`、`POST`、`PUT`、`DELETE /{id}`、`GET /all` | 护理等级/护理项目/任务等其它模块后端改造（护理等级页只验计划下拉） |
| 前端 `/nursing/plan` 全交互（含查看只读、子表增删行、count 禁用样式） | 前端任何代码修改（**禁止**；仅登记） |
| 联动：`/nursing/level` 计划下拉（`/all`） | 护理等级 CRUD、plan 提交链路（等级接口仍缺，属既有 R4） |
| 回归：护理项目/房型/用户页 + 既有 API + 8080 健康 | 性能/并发压测、导出 Excel（页面无导出按钮） |
| 权限（admin 正向）、审计日志、安全健壮性、数据清理 | 任务单明确不做项（以 arch 任务单为准） |

---

## 3. 依据与契约基线

### 3.1 依据优先级
1. **web-arch《Sprint 5b 护理计划 CRUD 任务单》**（**待发布**；发布后以它为准，本计划中标记 `[待任务单确认]` 的项随之对齐）；
2. 前端源码实际调用（`src/api/nursing/plan.js`、`views/nursing/plan/index.vue`、`views/nursing/level/index.vue`，只读核对）；
3. DB 实测（只读，见 §3.5）；
4. lead 下午场任务要求。

### 3.2 接口契约（前端反推，待任务单确认出参细节）

| # | 方法/路径 | 入参 | 前端期望出参 | 权限点（sys_menu 实测） |
|---|---|---|---|---|
| 1 | `GET /nursing/plan/list` | `pageNum,pageSize,planName,status` | `TableDataInfo{total,rows[]}`；rows=`NursingPlan`（含 **`count`** 引用数 + `createTime`）；**排序 `order by sort_no asc, id asc`** | `nursing:plan:list`(2013) |
| 2 | `GET /nursing/plan/{id}` | path id | `AjaxResult.data=NursingPlanVo`，**必带 `projectPlans[]`**（projectId/executeCycle 为字符串，executeFrequency 数字） | `nursing:plan:query`(2014) |
| 3 | `POST /nursing/plan` | JSON `NursingPlanDto`（含 `projectPlans`） | `AjaxResult{code:200,msg}`；主表+子表**同事务**；新 id 自增（实测 141 起，不硬编码） | `nursing:plan:add`(2015) |
| 4 | `PUT /nursing/plan` | JSON 整表单（含 projectPlans）**或仅 `{id,status}`** | `AjaxResult`；**两种语义**：整表单=更新主表+重建子表；启停=仅更 status，null 不覆盖、子表不动 | `nursing:plan:edit`(2016) |
| 5 | `DELETE /nursing/plan/{id}` | path 单 id（前端 `delPlan(row.id)`） | `AjaxResult`；**count>0 拒绝**；同事务删子表+主表 | `nursing:plan:remove`(2017) |
| 6 | `GET /nursing/plan/all` | 无 | `AjaxResult.data=**List<NursingPlan>（实体数组，非 Vo/label-value）**`；每项含 `id/planName`；按 sort_no,id；**全量 3 条（含禁用 134）** | `nursing:plan:list`（或 query） |

### 3.3 前端可观测行为基线（源码只读核对）

| 项 | 源码事实（预期） |
|---|---|
| 路由/菜单 | `/nursing/plan`，menu_id 2013“护理计划”（权限 `nursing:plan:list`）；页面组件 `views/nursing/plan/index.vue` |
| 搜索栏 | 名称（`planName`，可回车）/ 状态（字典 `nursing_plan_status`，可清空）+ 搜索/重置 |
| 表格 | **5 列**：序号(index,50)/名称/状态(tag 启用绿·禁用红)/创建时间(180，`yyyy-MM-dd HH:mm:ss`)/操作(280, fixed right)；**操作 4 个按钮**：修改、删除、查看、启用/禁用 |
| `count` 禁用逻辑 | `:class="scope.row.count ? 'disabled' : ''"` 仅加 CSS class（**非 disabled 属性**）→ 视觉禁用；点击是否真拦截为观察项 |
| 分页 | 默认 10 条/页；“共 N 条” |
| 新增/修改弹窗 | **1000px**；主表：护理计划名称*（input）/状态*（radio 字典，默认“启用”=1）/排序*（input-number min0 max999999，默认 1）；子表“护理项目”**4 列**：项目名称（下拉，数据源 `/nursing/project/all` label/value 字符串）、期望服务时间（time `HH:mm:ss`）、执行周期（下拉：天0/周1/月2）、执行频次（number 1~7）；子表默认 1 行，`+` 增行、`−` 删行（仅剩 1 行时不显示 `−`）；表头高 50/表体高 300 滚动 |
| 提交校验 | 前端 rules：`planName`/`status`/`sortNo` 必填；**子表重复项目校验**：`hasDuplicateIds` → toast“请勿选择重复的护理项目”且不发请求；子表无逐项必填规则 |
| 查看 | `handleLook` 打开弹窗置 `isLook=true`：主表输入/radio/排序、子表控件全部禁用，**无“确定/取消”按钮**（footer `v-if="!isLook"`）；标题复用“修改护理计划”（前端既有文案，观察） |
| 删除 | 确认框文案“**是否确认删除这条护理计划？**” → `DELETE` → toast“删除成功” |
| 启停 | 确认框文案“**是否确认禁用（启用）护理项目的数据项？**”（前端文案写“护理项目”，既有 quirk）→ `PUT {id,status}` → toast“禁用成功/启用成功” |
| 新增/修改 | 成功 toast“新增成功/修改成功” → 关闭弹窗 → 刷新列表 |
| 详情回填 | `formData=response.data`；`status=String(status)`；`projectList=response.data.projectPlans`；要求子表 `projectId` 为**字符串**（与 `/nursing/project/all` 的 value 字符串匹配）、`executeCycle` 字符串（与选项 '0'/'1'/'2' 匹配） |
| `/all` 消费方 | `views/nursing/level/index.vue`：`nursingPlanList=response.data`；下拉 `:label="item.planName" :value="item.id"`；列表列 `prop="planName"` |

### 3.4 状态值与字典口径（**任务单 v0.1 已确认 = 观察项 O-1**）

- DB 实测：`nursing_plan.status` tinyint NOT NULL DEFAULT 0（注释 0禁用/1启用）；种子 133=1、134=**0**、135=1。
- 字典 `nursing_plan_status`：**启用=1、禁用=2**（与 DB 0/1 不一致，属既有前端/字典 quirk）。
- 任务单 §6 O-1 口径：**前端零改动前提下，后端不限制 status 取值**（int 接受 0/1/2），不因 0 报错；tester **不得把“134 编辑 radio 未选中”判缺陷**，仅记录观察。
- 测试处理：① 列表/详情按 DB 实际 0/1 断言；② 状态搜索 `status=1`→2 条、`status=0`→1 条；③ 表单提交 status=1/2/0 均记录实际（后端不报错为预期，不判缺陷）；④ 启停按前端 0↔1 逻辑验证。

### 3.5 DB 基线（只读实测，2026-11-25 14:5x）

**nursing_plan（3 行，AUTO_INCREMENT=141，`plan_name` 有 UNIQUE 索引）**

| id | sort_no | plan_name | status | create_by | create_time |
|---|---|---|---|---|---|
| 133 | 1 | 1号护理计划 | 1 启用 | NULL | 2024-08-19 19:17:31 |
| 134 | 1 | 2号护理计划 | **0 禁用** | NULL | 2024-08-19 19:36:10 |
| 135 | 1 | 3号护理计划 | 1 启用 | NULL | 2024-08-29 16:56:39 |

**nursing_project_plan（6 行，AUTO_INCREMENT=1752，无唯一键/无外键）**

| id | plan_id | project_id | 项目名 | execute_time | execute_cycle | execute_frequency |
|---|---|---|---|---|---|---|
| 1736 | 133 | 1 | 修剪指甲 | 19:16:49 | 1 周 | 7 |
| 1737 | 133 | 2 | 衣物清洁 | 19:28:34 | 1 周 | 1 |
| 1739 | 135 | 1 | 修剪指甲 | 16:55:58 | 2 月 | 1 |
| 1740 | 135 | 5 | 助浴 | 16:56:09 | 0 天 | 1 |
| 1741 | 135 | 4 | 助餐 | 08:00:00 | 0 天 | 1 |
| 1743 | 134 | 1 | 修剪指甲 | 19:28:45 | 0 天 | 1 |

**其它口径**：
- `nursing_level`（护理等级）3 行，`plan_id` 分别引用 133/134/135 → **每个种子计划的 `count`=1** → 列表“修改/删除”应带 `disabled` 样式；因此 **UI 修改/删除用例需先新建 count=0 的测试计划**。
- `create_by/update_by` 为 **bigint**，而 RuoYi `BaseEntity` 为 String（`@TableField(fill=INSERT/UPDATE)`）→ MP 自动填充 String userId 写 bigint 列（MySQL 隐式转换）；`create_time` 为 **NOT NULL 且无 DB 默认值** → 必须由 MP fill 或 service 显式赋值，否则插入失败（**新增用例必须断言 create_time/create_by**）。
- `nursing_project_plan` 字段均 NOT NULL（plan_id/project_id/execute_time/execute_cycle/execute_frequency）→ 子表缺字段将直接 DB 异常（**新增事务/校验用例**）。
- `nursing_project` 9 行（子表项目下拉数据源，`/nursing/project/all` 已可用）。

### 3.6 任务单口径映射（**已确认，替换 v1.0 的 K1–K8 待确认项**）

| # | 口径 | 任务单依据 | 测试断言 |
|---|---|---|---|
| C1 | status 取值不做限制；字典 1/2 与 DB 0 差异属观察 O-1 | §4.4/§6 O-1 | 不判缺陷，记录实际 |
| C2 | list `count` = `select count(*) from nursing_level where plan_id=nursing_plan.id`；种子均 1 | §3.2-1/§4.4 | 每行 count=1；新建 count=0 |
| C3 | 排序 `sort_no asc, id asc` | §3.2-1/§4.5 | ids 133/134/135 |
| C4 | 整表单修改/删除时 count>0 → **后端强制拒绝**（友好错误）；**启停放行** | §4.4/A8 | API-A8 专项 |
| C5 | 子表校验：`projectPlans` 非空；projectId/executeTime(HH:mm:ss)/executeCycle(0/1/2)/executeFrequency(1~7) 必填；同计划 projectId 不重复；失败无脏主表 | §4.2/A11 | API-ADD-02/05/06、API-UPD 系列 |
| C6 | `/all` → **`List<NursingPlan>` 实体数组（非 Vo、非 `{label,value}`）**，全量 3 条（含禁用），按 sort_no,id | §3.2-6/§4.7/lead 决策 | API-ALL 系列 + 等级页下拉 |
| C7 | 新增/修改/删除主+子表 `@Transactional`，子表重建 delete+batchInsert 无残留 | §4.1/§4.9 | API-ADD-05、API-UPD-01、API-DEL-01 |
| C8 | 启停仅传 `{id,status}` → 只更新 status、不覆盖其它字段、不重建子表 | §3.3/A7 | API-UPD-02/03、UI-TOG-02 |
| C9 | **R2（lead 决策）**：主表 insert/update 走 **Mapper XML 自定义 SQL**（不走 MP 自动填充）；**create_by/update_by 不赋值（NULL 允许，不要求有值）**；`createTime` 新增时显式设置、`updateTime` 更新时显式设置；禁止 ALTER 表 | §4.8/R2 v0.2 | API-ADD-01/09、API-UPD-01 |
| C10 | 新增默认值：status 未传=1、sortNo 未传=1 | §4.6 | API-ADD-02/07 |
| C11 | 名称唯一：trim+必填+≤255、排除自身、DuplicateKeyException 兜底，友好中文 | §4.3/A10 | API-ADD-03/04、API-UPD-05/06 |
| C12 | A8 被引用校验：种子 133 整表单修改/删除→友好错误、数据不变；**133 启停 {id,status} → 200 允许**（验后按快照恢复 status/update_time） | §4.4/A8 | API-UPD-08/API-DEL-04/API-A8-01/02 |

---

## 4. 环境、工具与前置

| 项 | 值/策略 |
|---|---|
| 前端 88 | `http://localhost:88`，PID 23192 未动 → 88 PID **35760**（现状），只读访问 |
| 后端 8080 | `http://localhost:8080`，PID **23192**；devtools 开启；W1/W2 期间不重启（主人 IDEA 实例，未经 lead 批准不动） |
| MySQL 3306 | `zzyl` 库 `nursing_plan`/`nursing_project_plan`；**只读核验免申请；写（含经接口 CRUD）需窗口** |
| 浏览器自动化 | 无头 Edge（`playwright-core@1.65.1` 已装：`artifacts/tools/browser/`）；W2 独占，结束后无残留 |
| 验证码 | `captchaEnabled=true`；交接策略同 5a：Playwright/API 取图 → **Redis 只读 `GET captcha_codes:{uuid}`**（不写不删）→ 登录；失败 2 次即停 |
| 工具复用 | API：`artifacts/tools/api/api_suite.ps1` 的 HTTP/DB/断言框架（需改造为 plan 套件）；UI：`artifacts/tools/browser/ui_suite.mjs` 框架（需改造为 plan 套件） |
| 证据目录 | `artifacts/test_web_s5b/{static,api,ui,db,logs,tmp}`（自建） |

**前置条件（缺一不测）**：① arch 任务单发布；② dev1 完成 + `mvn compile` 通过（轻量报备）；③ arch review PASS；④ 8080 已加载新 Controller（接口探测）；⑤ W1/W2 窗口批准。

---

## 5. 测试数据设计（写库申报口径）

| 项 | 内容 |
|---|---|
| 命名规范 | 计划名统一 **`ZZT5B-`** 前缀 + 用途 + 时间戳（如 `ZZT5B-新增-1530a`），长度 ≤255；`plan_name` 唯一 |
| 新增条数 | W1：2–3 个测试计划（含 1 个 2 子表行、1 个 1 子表行）；W2：1–2 个（UI 新增/修改/删除用）；合计 ≤5 主表行、子表 ≤10 行 |
| 修改 | 仅测试计划：全字段更新（子表替换）、`{id,status}` 启停；种子只读 |
| 删除 | 测试计划经 `DELETE` 清理（验证级联子表）；**不动种子计划**（被 nursing_level 引用，不实际删除） |
| 预计影响行数 | `nursing_plan` INSERT ≤5 / UPDATE ≤6 / DELETE ≤5（净增 0，回 3 行）；`nursing_project_plan` INSERT ≤10 / DELETE ≤10（净增 0，回 6 行）；**A8 用例会尝试对种子 133 整表单改/删（预期被拒、无变更）并把其 status 临时改 0 后恢复 1、update_time 按快照回滚**（快照 SQL 落盘，异常时按快照恢复）；`AUTO_INCREMENT` 前移属正常（记录实际值） |
| 回滚/兜底 | 接口异常时按申报 `DELETE FROM nursing_plan WHERE plan_name LIKE 'ZZT5B-%';` 及其子表 `DELETE FROM nursing_project_plan WHERE plan_id IN (SELECT id FROM nursing_plan WHERE plan_name LIKE 'ZZT5B-%')`，回报实际行数 |
| 自动副作用 | `sys_logininfor`/`sys_oper_log` 少量新增（应用行为，不清理，记录备查） |
| 基线快照 | 开测前 `SELECT` 3/6 行 + 两表内容哈希（种子 md5），收测比对；AUTO_INCREMENT 记录（MySQL8 information_schema 统计缓存不可靠，以 `MAX(id)` 为准） |

---

## 6. 执行策略与窗口预案

```
开窗前（免申请）：STA 静态核查（前端零改动/后端改动范围/编译/代码评审/契约核对）
W1（API，10 分钟；资源：8080 稳定 + MySQL 写）
  S1 健康 + 登录 token → 只读 API（list/搜索/分页/详情/all/count/401）
  S2 写 API（add 主+子表/重名/事务/update 全字段+子表替换/启停部分/delete 级联）→ 只读 DB 校验 → 清理
  S3 回归 API + 审计日志 + 数据恢复核验 → 释放
W2（UI，15 分钟；资源：浏览器自动化 + 8080 只读 + MySQL 写）
  S4 登录 → 服务管理/护理计划 → 列表 3 行/状态/分页/count 禁用样式/搜索
  S5 新增（1000px 弹窗+子表增删行+重复项目校验+成功）→ 修改回填/保存 → 查看只读 → 启停 → 删除
  S6 联动 /nursing/level 计划下拉 /all → 回归（护理项目/房型/用户）→ 截图 → 清理 → 释放
W3（复测，按需 ≤10 分钟）：缺陷复测/补证
收尾：数据恢复 3/6、种子哈希比对、证据归档、报告、汇报 lead
```

**申请文案预案**（批准后发 web-control）：
```
W1【API】web-tester | 护理计划 CRUD API 验收 + 测试数据清理 | 后端8080稳定 + MySQL写（nursing_plan/nursing_project_plan，仅 ZZT5B-*，净增0回3/6行；A8 会快照并临时改种子133 status 后恢复，异常按快照回滚） | 10 分钟 | 不重启、不动种子 | 否
W2【UI】web-tester | 护理计划 UI 浏览器验收（列表/搜索/新增子表/修改/查看/删除/启停/联动/回归+截图） | 浏览器自动化（无头 Edge）+ 8080只读 + MySQL写（ZZT5B-*） | 15 分钟 | 同 W1 | 否
```

---

## 7. 用例清单

> 优先级：P0 冒烟/核心闭环｜P1 主要功能/契约｜P2 边界/异常｜P3 观察（不阻塞）
> 依据列：`TS`=任务单验收项（待发布后映射）｜`F`=前端源码｜`DB`=DB 实测｜`K`=§3.6 待确认口径

### 7.1 静态核查 STA（开窗前，免申请）

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| STA-01 | P0 | 前端目录 `git status/diff`；关键文件 hash vs HEAD | **前端零改动** | 任务要求/F |
| STA-02 | P0 | 后端改动范围审查（`git status --porcelain code/`） | 仅约定文件：疑似新增 `NursingPlanMapper.java`、`INursingPlanService.java`、`NursingPlanServiceImpl.java`、`NursingPlanController.java`、`NursingPlanMapper.xml`（补 order by/子表 SQL）；`NursingProjectPlanMapper` 相关如需调整仅在授权范围 | 任务单（待） |
| STA-03 | P1 | 代码评审：6 接口路径/方法/参数、`extends BaseController`、`@PreAuthorize` 4~5 权限点、`@Log`、`startPage/getDataTable`、事务注解 | 与任务单/契约一致 | TS |
| STA-04 | P1 | 编译状态核对（dev1 报备编译；测试方不主动触发 devtools 重启） | 编译通过、`target/classes` 含新类、8080 已加载 | TS |
| STA-05 | P1 | 无新增依赖/配置/表结构；`/nursing/project/*` 与 `/nursing/plan/*` 路径无冲突；Mapper XML namespace 与接口一致 | 符合 | TS |
| STA-06 | P1 | 前端 6 个 URL 与任务单契约一致 | 一致 | F |
| STA-07 | P1 | `/common/upload` 现状（5a 本地兜底）不回归（本模块不涉及上传，仅健康探测） | 仍可用（不影响本模块） | 5a 基线 |

### 7.2 API 层

**A. 列表与搜索**

| ID | 级别 | 步骤/请求 | 预期 | 依据 |
|---|---|---|---|---|
| API-LIST-01 | P0 | `GET /nursing/plan/list?pageNum=1&pageSize=10` | `code=200,total=3,rows=3`；行 id **133/134/135**；**每行含 `count=1`**（护理等级各引用一次）与 `createTime`；status 133=1、134=0、135=1 | A2/DB/C2 |
| API-LIST-02 | P1 | 排序校验 | 按 `sort_no asc, id asc`（种子 sort_no 均 1）→ 实际顺序 **133,134,135**；若任务单后续调整以任务单为准 | A2/C3 |
| API-LIST-03 | P1 | `GET list?planName=1号` | total=1，id=133（模糊 like 子串） | A3/F |
| API-LIST-04 | P1 | `GET list?planName=护理计划` | total=3（子串匹配） | A3/F |
| API-LIST-05 | P1 | `GET list?status=1`；`status=0` | status=1 → 2 条（133/135）；status=0 → 1 条（134） | A3/C1 |
| API-LIST-06 | P1 | `GET list?planName=不存在XYZ` | total=0，rows=[] | F |
| API-LIST-07 | P1 | 分页：`pageSize=2` page1/page2；`pageNum=3` 超界 | page1=2 条、page2=1 条、page3 reasonable=末页 1 条；total 均 3（框架行为，参照 5a） | A3 |
| API-LIST-08 | P2 | 组合搜索 planName=计划&status | 结果与单条件组合一致 | F |
| API-LIST-09 | P2 | 非法分页 `pageNum=0/pageSize=0/pageSize=1000` | 不 500；返回合法结构 | F |

**B. 详情（含子表）**

| ID | 级别 | 步骤/请求 | 预期 | 依据 |
|---|---|---|---|---|
| API-GET-01 | P0 | `GET /nursing/plan/133` | `data.id=133,planName=1号护理计划,status=1,sortNo=1,createTime`；`projectPlans.length=2`（1736 修剪指甲/1737 衣物清洁）；子项 `projectId` **字符串** "1"/"2"、executeTime "19:16:49"/"19:28:34"、executeCycle **字符串** "1"/"1"、executeFrequency 数字 7/1 | A4/DB |
| API-GET-02 | P1 | `GET /nursing/plan/134`；`/135` | 134：1 个子项（项目 id 1）；135：3 个子项（projectId "1"/"5"/"4"） | A4/DB |
| API-GET-03 | P1 | `GET /nursing/plan/999999` | `code=200,data=null`（前端可提示） | F |
| API-GET-04 | P2 | `GET /nursing/plan/abc` | JSON 错误（4xx/200+code），无堆栈 | 健壮 |
| API-GET-05 | P1 | 未带 token 访问详情/list | body `code=401`（RuoYi 约定 HTTP200+body401），不返回数据 | 安全 |

**C. 新增（主表 + 子表 + 事务）**

| ID | 级别 | 步骤/请求 | 预期 | 依据 |
|---|---|---|---|---|
| API-ADD-01 | P0 | POST `{planName:"ZZT5B-新增-<ts>",status:"1",sortNo:2,projectPlans:[{projectId:"1",executeTime:"08:00:00",executeCycle:"0",executeFrequency:2},{projectId:"5",executeTime:"09:30:00",executeCycle:"1",executeFrequency:1}]}` | code=200；列表 total=4；新 id=**实际自增**（当前 141，不硬编码）；DB 主表 1 行（**create_time 有值；create_by 可为 NULL，R2 决策不要求**）、子表该 plan_id **2 行**；详情回读 projectPlans 2 条且 projectId 为字符串 | A5/C2/C7/C9 |
| API-ADD-02 | P1 | POST 不带 projectPlans / 空数组 | **友好拒绝**（任务单 §4.2 必做：projectPlans 非空）；无落库 | A11/C5 |
| API-ADD-03 | P1 | POST 不带 planName / 空白名称 | 友好中文错误（“护理计划名称不能为空”类），非堆栈；无落库 | A10/C11 |
| API-ADD-04 | P1 | POST 重名（“1号护理计划”） | 友好错误“护理计划名称已存在”类；无落库；总数不变 | A10/C11 |
| API-ADD-05 | P0 | **事务/回滚**：POST 名称合法但子表含非法字段（`projectId:""` / `executeTime:""` / executeCycle 非法） | 友好错误；**DB 主表无新增（无脏数据）**；修复后重试成功 | A11/A15/C5/C7 |
| API-ADD-06 | P1 | POST 子表重复 projectId（同 1 重复 2 行） | **友好错误“请勿选择重复的护理项目”类**（任务单 §4.2 后端必做）；无落库 | A11/C5 |
| API-ADD-07 | P2 | POST planName 超长（256）/未传 status/未传 sortNo | 超长→友好错误；未传 status→默认 1；未传 sortNo→默认 1（任务单 §4.6） | C10 |
| API-ADD-08 | P2 | POST 非法 JSON | JSON 错误 4xx/200+code，无堆栈 | 健壮 |
| API-ADD-09 | P1 | DB 复核新增行审计字段（R2 v0.2） | `create_time` 有值（XML 显式设置）；**`create_by`/`update_by` 可为 NULL（不要求有值）**；`sort_no/status` 正确 | A5/C9 |

**D. 修改（整表单 + 子表重建 + 启停部分更新）**

| ID | 级别 | 步骤/请求 | 预期 | 依据 |
|---|---|---|---|---|
| API-UPD-01 | P0 | PUT 测试计划**整表单**：改名/改 sortNo + `projectPlans` 从 **2 行替换为 1 行**（A6 口径） | code=200；主表字段更新（**updateTime 有值**，R2/C9）；详情 projectPlans=1 条且为新组合；DB 旧子表 2 行删除、仅留 1 行（无残留）；projectId 字符串 | A6/C7/C9 |
| API-UPD-02 | P0 | **启停部分更新**：PUT **仅** `{id,status:0}` | code=200；详情 status=0，**planName/sortNo/子表条数内容全部保持**；DB 其它列未清空 | A7/C8（关键） |
| API-UPD-03 | P1 | 再 PUT `{id,status:1}` 恢复 | status=1，其它字段仍保持 | A7/C8 |
| API-UPD-04 | P1 | PUT 不存在 id（999999） | 友好错误“护理计划不存在”类，不误更新 | §3.3 |
| API-UPD-05 | P1 | PUT 改名与现有计划重名 | 友好错误“护理计划名称已存在”；原值未变 | A10/C11 |
| API-UPD-06 | P1 | PUT 自身当前名（排除自身） | code=200（不误报重名） | C11 |
| API-UPD-07 | P2 | PUT `{}` 无 id / 非法 JSON | JSON 错误，无堆栈，不误更新 | 健壮 |
| API-UPD-08 | P0 | **整表单修改种子 133**（count=1）：PUT `{id:133,planName:"1号护理计划",sortNo:1,status:1,projectPlans:[...]}` | 友好错误“该护理计划已被护理等级引用，无法修改或删除”；**数据不变**（快照校验 planName/sortNo/status/子表 2 行均不变） | A8/C4 |

**E. 删除（级联子表 + 被引用拦截）**

| ID | 级别 | 步骤/请求 | 预期 | 依据 |
|---|---|---|---|---|
| API-DEL-01 | P0 | 对含子表的测试计划 `DELETE /nursing/plan/{id}` | code=200；主表行删除；**子表 `plan_id=id` 行同步删除（级联）**；列表 total 减 1 | A9/C7 |
| API-DEL-02 | P1 | DELETE 不存在 id | 友好错误；无堆栈 | A9 |
| API-DEL-03 | P0 | 清理全部 ZZT5B-* 后核验 | `nursing_plan=3`、`nursing_project_plan=6`；种子内容哈希不变；ZZT5B 残留=0 | 数据纪律 |
| API-DEL-04 | P0 | **DELETE 种子 133**（count=1） | 友好错误“该护理计划已被护理等级引用，无法修改或删除”；**数据不变**（主表 133 与子表 2 行均在）。安全执行：先 `SELECT` 快照落盘；若实现缺失导致误删，立即按快照 SQL 恢复并记 **P1** | A8/C4 |
| API-DEL-05 | P3 | 批量删除 `{id1,id2}`（契约外观察，前端无入口） | 记录实际；不计缺陷 | 参考 5a |

**F. 启停放行专项（A8 后半）**

| ID | 级别 | 步骤/请求 | 预期 | 依据 |
|---|---|---|---|---|
| API-A8-01 | P0 | `PUT {id:133,status:0}`（种子 133 启停） | **code=200（允许，不做引用拦截）**；详情 status=0，planName/sortNo/子表 2 行不变 | A8/C12 |
| API-A8-02 | P1 | `PUT {id:133,status:1}` **恢复种子原状态**，并按开测前快照 SQL 恢复 133 的 `update_time`（A8 副作用回滚；窗口申报已含） | code=200；详情 status=1、planName/sortNo/子表不变；**收测种子哈希与基线一致**（含 update_time） | A8/数据纪律/C12 |

**G. `/all`**

| ID | 级别 | 步骤/请求 | 预期 | 依据 |
|---|---|---|---|---|
| API-ALL-01 | P0 | `GET /nursing/plan/all` | code=200；`data` 为 **List<NursingPlan> 实体数组（非 label/value）**、**length=3（含禁用 134）**；每项含 `id`（数字）与 `planName`（字符串）；排序 sort_no,id → 133/134/135 | A12/C6 |
| API-ALL-02 | P1 | 类型/字段断言 | id 为 number、planName 非空；无重复；不含子表（避免无谓查询为佳） | A12/C6 |
| API-ALL-03 | P1 | 新增测试计划后 `/all` | length=4 且含测试项；清理后回 3 | A12/C6 |
| API-ALL-04 | P1 | 未认证 `/all` | body code=401 | 安全 |

**H. 权限 / 安全 / 审计 / 状态**

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| API-AUTH-01 | P0 | admin token 调 6 接口 | 全通；`/getInfo` 权限（超管 `*:*:*` 或 5 权限点） | A14 |
| API-AUTH-02 | P1 | 未认证写接口（POST/PUT/DELETE） | body code=401，不产生数据（DB 行数不变） | 安全 |
| API-AUTH-03 | P2 | 代码评审 5 权限注解与 sys_menu 2013–2018 对齐 | 一致 | A14 |
| API-SEC-01 | P1 | planName 注入串 `' OR '1'='1` 搜索/新增 | 不改变语义、无 SQL 异常 | 安全 |
| API-SEC-02 | P1 | 错误响应审查 | 均 JSON `{code,msg}`，无 Java 堆栈/路径/SQL 泄露 | 安全 |
| API-SEC-03 | P2 | 写操作后只读查 `sys_oper_log` | 存在“护理计划”INSERT/UPDATE/DELETE 记录（@Log） | task §4 |
| API-STATUS-01 | P2 | `GET list?status=2`（字典值）与 POST/PUT status=2 | 后端不限制 status（记录实际：搜索 0 条/写入 2 成功均不判缺陷）；134 编辑 radio 未选中属 O-1 观察 | C1/O-1 |

### 7.3 UI 层（Playwright 无头 Edge）

**登录/导航**

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| UI-LOGIN-01 | P0 | 打开 88 → /login 渲染；空提交校验 | 标题/三输入框；空提交出现校验提示（账号密码预填需先清空） | F |
| UI-LOGIN-02 | P0 | admin+验证码登录 | 成功跳 /index | 环境 |
| UI-NAV-01 | P1 | 侧边栏 服务管理→护理计划 | 进入 `/nursing/plan`；页签“护理计划” | F/2013 |
| UI-NAV-02 | P2 | 直接访问 `/nursing/plan` | 正常加载 | F |

**列表**

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| UI-LIST-01 | P0 | 列表渲染 | **3 行**；行内容 1号/2号/3号护理计划；134 状态 tag“禁用”（红）、133/135“启用”（绿） | DB/F |
| UI-LIST-02 | P1 | 表头 | **5 列**：序号/名称/状态/创建时间/操作；操作列 4 按钮（修改/删除/查看/启用·禁用）；创建时间格式 `yyyy-MM-dd HH:mm:ss` | F |
| UI-LIST-03 | P1 | count 禁用样式 | 3 条种子 `count=1` → 修改/删除按钮带 `disabled` class（视觉置灰）；查看/启停可用 | F/K2 |
| UI-LIST-04 | P1 | 分页 | “共 3 条”、10条/页、单页无翻页 | F |
| UI-LIST-05 | P2 | 控制台 | 无未捕获 JS 异常（忽略既有 plan 接口已修复后应无错误） | F |

**搜索**

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| UI-SEARCH-01 | P1 | 名称“1号”→搜索 | 1 行（id133）；“护理计划”→3 行；重置回 3 行 | F |
| UI-SEARCH-02 | P1 | 状态下拉“启用”（字典值 1）→搜索；再选“禁用”（字典值 2）→搜索 | 启用→2 行（133/135）；**禁用=2 → 0 行**（DB 种子为 0，属 O-1 观察，不判缺陷） | A3/C1/O-1 |
| UI-SEARCH-03 | P2 | 名称不存在 → 空态“暂无数据” | 空表 + total 0 | F |
| UI-SEARCH-04 | P2 | 回车触发搜索 | 同点击搜索 | F |

**新增（核心：主表 + 子表）**

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| UI-ADD-01 | P0 | 点“新增” | 弹窗宽 1000、标题“添加护理计划”；主表 3 字段（名称/状态 radio 默认启用/排序 默认1）；子表表头 4 列 + 默认 1 行；底部 确定/取消 | F |
| UI-ADD-02 | P0 | 空表单点确定 | 前端必填校验：名称/状态/排序提示（弹窗不关闭、无 POST） | F |
| UI-ADD-03 | P1 | 子表行操作 | 默认 1 行时**无 − 图标**；点 + → 2 行；2 行时点 − → 回 1 行；每行 4 个控件 | F |
| UI-ADD-04 | P1 | 子表项目下拉 | 展开 **9 项**（/nursing/project/all）；选中项显示名称（value 为字符串 id） | F/A12 |
| UI-ADD-05 | P1 | 子表重复项目校验 | 两行选同一项目 → 点确定 → toast“请勿选择重复的护理项目”、**无 POST** | F |
| UI-ADD-06 | P0 | 填写合法：名称 `ZZT5B-UI新增-<ts>`、状态启用、排序 1、子表 2 行（项目/时间/周期/频次）→ 确定 | toast“新增成功”；弹窗关闭；列表 total=4；新行可搜索到（count=0 → 修改/删除无 disabled） | F/TS |
| UI-ADD-07 | P2 | 取消新增 | 弹窗关闭、无请求、列表不变 | F |

**修改/查看**

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| UI-EDIT-01 | P0 | 对 UI 新增的测试计划点“修改” | 标题“修改护理计划”；**主表回填**（名称/状态 radio 选中/排序）；**子表回填 2 行**：项目下拉显示项目名（projectId 字符串匹配）、时间 HH:mm:ss、周期/频次正确 | F/API-GET |
| UI-EDIT-02 | P0 | 修改名称 → 保存 | toast“修改成功”；列表新名称；详情回读一致 | F |
| UI-EDIT-03 | P1 | 修改子表：加 1 行/改项目/删 1 行 → 保存 | 子表替换成功（详情行数与内容为新组合）；旧行不残留 | F/API-UPD-01 |
| UI-EDIT-04 | P1 | 打开修改后取消 | 数据未变、无 PUT | F |
| UI-EDIT-05 | P1 | 查看：点“查看” | 弹窗打开且主表输入/radio/排序、子表控件全部禁用；**无确定/取消按钮**（仅关闭 X）；标题为“修改护理计划”（前端既有文案，记录观察） | F |
| UI-EDIT-06 | P2 | 修改为已存在名称 | 友好错误 toast；名称未变 | F |

**删除/启停**

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| UI-DEL-01 | P0 | 测试计划点删除 → 确认框 | 文案“是否确认删除这条护理计划？”；取消不删（无 DELETE） | F |
| UI-DEL-02 | P0 | 确认删除 | toast“删除成功”；列表 total 减 1；详情/DB 无残留（含子表级联） | F/API-DEL-01 |
| UI-DEL-03 | P1 | 种子行（count=1）**仅查验修改/删除按钮 disabled 样式**，不实际点击删除（避免误删种子） | 修改/删除按钮带 `disabled` class（视觉置灰）；查看/启停可用。**后端 count 强制拦截由 API-UPD-08/DEL-04 覆盖**；前端“仅 CSS 置灰、点击仍触发”属 O-2 观察 | A13/O-2/C4 |
| UI-TOG-01 | P0 | 测试计划点“禁用”→ 确认框 | 文案“是否确认禁用护理项目的数据项？”（前端既有文案）；取消→状态不变 | F |
| UI-TOG-02 | P0 | 确认禁用 | toast“禁用成功”；tag 变禁用；**抓包 PUT body 仅 `{id,status}`**；DB 其它字段/子表未被清空 | K8 关键 |
| UI-TOG-03 | P1 | 再启用 | toast“启用成功”；状态恢复；刷新后持久 | F |
| UI-TOG-04 | P2 | 启停后刷新页面 | 状态/名称/子表持久 | F |

**前端零改动护栏/网络**

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| UI-GUARD-01 | P0 | 测试前后 git 状态/文件 hash | 前端零改动 | 任务要求 |
| UI-GUARD-02 | P1 | 抓包：`/nursing/plan/*` API 均走 `/dev-api` 且 200；方法 GET/POST/PUT/DELETE | 与 `plan.js` 6 个 URL 一致；排除 `/src/*`、`@vite`、页面导航 | F |
| UI-GUARD-03 | P1 | 控制台无未捕获异常（排除已知 plan/level 缺接口，本模块应无 500） | 无 | F |

### 7.4 联动

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| LINK-01 | P0 | 打开 `/nursing/level`（护理等级页） | `/nursing/plan/all` 200；页面壳正常（其 CRUD 接口仍缺属既有 R4） | K6 |
| LINK-02 | P1 | 等级页点“新增”→ 弹窗“护理计划”下拉展开 | **3 项**（1号/2号/3号护理计划，含禁用 134）；选中后显示计划名 | F/K6 |
| LINK-03 | P2 | 护理计划页新增弹窗“护理项目”下拉 | 9 项（`/nursing/project/all` 回归，5a 已验） | 5a |

### 7.5 回归（只读）

| ID | 级别 | 步骤 | 预期 | 依据 |
|---|---|---|---|---|
| REG-01 | P0 | 8080/88 健康（端口/PID/首页 200） | 正常；记录 PID | 环境 |
| REG-02 | P0 | 护理项目页 `/nursing/project` | 列表 9 行、搜索/分页正常（5a 功能不回归） | 5a |
| REG-03 | P1 | 房型设置 `/liveIn/houseSet` | 列表 6 行、正常渲染 | 5a |
| REG-04 | P1 | 用户管理 `/system/user` | 列表 7 行 | 5a |
| REG-05 | P1 | 既有 API：`/elder/roomType/list`、`/system/user/list`、`/nursing/project/list` | 200 且数量正常（6/7/9）；无路径冲突 | 5a |
| REG-06 | P1 | 服务管理其它页（护理项目/护理等级/负责老人） | 可打开；护理等级其自身接口仍缺属既有状态，不计缺陷 | R4 |

### 7.6 非功能

| ID | 级别 | 步骤 | 预期 |
|---|---|---|---|
| NF-01 | P1 | 退出/未登录访问 `/nursing/plan` | 跳登录页；API 401 |
| NF-02 | N/A | 受限账号负向权限（A13 同类） | 按 lead 决策本轮 N/A（同 5a 口径，等任务单确认） |
| NF-03 | P1 | 审计/登录日志只读核对 | 有对应记录 |
| NF-04 | P2 | 错误响应无堆栈/路径泄露 | 通过 |
| NF-05 | P2 | 注入字符不改变语义 | 通过 |
| NF-06 | P2 | 页面控制台无严重异常 | 通过 |

### 7.7 任务单验收映射（A1–A15，报告矩阵用）

| 任务单 | 用例映射 | 关键断言 |
|---|---|---|
| A1 静态/变更范围 | STA-01..07 | 前端零改动；后端仅 4 新文件 + XML 修改；未改表/配置 |
| A2 列表 | API-LIST-01/02 + UI-LIST-01/02 | total=3 rows=3、ids 133/134/135、count=1、createTime、status 1/0/1 |
| A3 搜索/分页 | API-LIST-03..09 + UI-SEARCH-01..04 | `1号`→1、status=1→2、status=0→1、pageSize=2→2/1 |
| A4 详情 | API-GET-01/02 + UI-EDIT-01 | projectPlans 2/1/3 条；projectId/executeCycle 字符串、frequency 数字 |
| A5 新增主+子事务 | API-ADD-01/09 + UI-ADD-06 | 200、total=4、动态新 id、DB 主 1+子 2、**create_time 有值；create_by 可为 NULL（R2 不要求）** |
| A6 修改整表单 | API-UPD-01 + UI-EDIT-02/03 | 子表旧删新增无残留、projectId 字符串 |
| A7 启停部分更新（关键） | API-UPD-02/03 + UI-TOG-02/03/04 | 仅 status 变；planName/sortNo/子表保持；抓包 PUT 仅 id/status |
| A8 被引用校验 | API-UPD-08 / API-DEL-04 / API-A8-01/02 + UI-DEL-03 | 整表单改/删种子 133→友好错误且数据不变；启停放行；改后恢复 |
| A9 删除主+子事务 | API-DEL-01/02/03 + UI-DEL-01/02 | **必须用 A5 新增的 count=0 计划**；200、total 回 3、子表级联 0 残留、不存在→友好错误；**不用种子验证删除** |
| A10 名称唯一 | API-ADD-03/04、API-UPD-05/06 + UI-EDIT-06 | 友好错误、无 SQL 堆栈、排除自身 |
| A11 子表校验/回滚 | API-ADD-02/05/06 + UI-ADD-05 | 空/缺字段/重复→友好错误；主表无脏数据 |
| A12 `/all` | API-ALL-01..04 + LINK-01/02 | **List<NursingPlan> 实体数组**、length=3（含禁用）、id/planName、按 sort_no,id；等级页下拉 3 项 |
| A13 前端页面联动 | UI-LIST/SEARCH/ADD/EDIT/DEL/TOG 全部 + UI-GUARD-01/02 | 列表 3 条、count 行灰置、查看只读无底部按钮、启停刷新、前端零改动 |
| A14 权限 | API-AUTH-01..03 | admin 正向全通；负向 N/A（lead 决策） |
| A15 事务一致性（加强） | API-ADD-05（构造子表失败→主表无脏数据→修复重试成功） | 回滚原子性 |

**预计用例数**：STA 7 + API 52 + UI 29 + LINK 3 + REG 6 + NF 6 ≈ **103**（最终以执行结果为准）。

---

## 8. 缺陷分级与判定口径

| 等级 | 定义 | 处理 |
|---|---|---|
| **P1 阻塞** | 核心闭环不可用/服务异常/数据被破坏/契约关键项不符（如新增不写入、修改清空字段、删除不级联、前端被改） | 必修+复测；阻塞验收 |
| **P2 严重** | 主要功能缺陷或契约不符、有数据/安全风险（列表缺 count、事务半截数据、重名无友好提示、权限缺失、启停清空子表） | 验收前修复或 lead 书面豁免 |
| **P3 一般** | 次要功能/边界/提示不友好（超长名报 SQL 错、子表无校验、查看标题复用"修改"、删除被引用未拦截） | 记录不阻塞，下 Sprint 修 |
| **P4 观察** | 前端/字典文案与口径不一致、文档偏差、AUTO_INCREMENT 副作用 | 记录不判缺陷 |
| **ENV** | 环境阻塞（8080 热重启、浏览器不可用等） | 按 lead 决策记录 |

**判定注意**：① 以 arch 任务单为第一依据；② 前端若仅 CSS 置灰而未禁用点击，按任务单/契约判（后端 count 强制拦截为准，前端 quirk 记 O-2）；③ 不清空种子的破坏性验证不做（A8 用“先快照+尝试+必要时恢复”方式，且在窗口申报中声明回滚）；④ 字典/状态值差异（O-1）按任务单不判缺陷。

### 8.1 任务单已列观察项（记录，不判缺陷）

| 编号 | 观察 | 测试处理 |
|---|---|---|
| O-1 | 状态字典 启用=1/禁用=2，但种子 134 status=0；编辑时 radio 无匹配项 | 后端不限制 status 取值；134 radio 未选中不判缺陷，仅记录（UI-EDIT 观察 + API-STATUS-01） |
| O-2 | 前端“查看”复用 dialog 且标题写“修改护理计划”；count>0 灰置仅 CSS 点击仍触发 | 记录；后端 count 强制校验为准（API-UPD-08/DEL-04）；UI-DEL-03 不实际点击种子删除 |
| R2 | **lead 决策（v0.2）**：不改表；主表 insert/update 走 **XML 自定义 SQL**（绕开 MyMetaObjectHandler）；**`create_by/update_by` 不要求有值（NULL 允许）**；`createTime`（新增）/`updateTime`（更新）由 Service 显式设置 | 断言 create_time/update_time 有值、create_by 允许 NULL；**不判缺陷** |
| R3 | `nursing_project_plan.plan_id` 无索引、无外键 | 仅记录，不改表 |
| R7 | 子表 `batchInsert` 用 `sysdate()` 填 create_time | 验收检查子表 create_time 合理（API-ADD-01/09 DB 复核） |
| R1 | 3 个种子计划全部 count=1 → UI 无法对种子做修改/删除验收 | UI 先新建 count=0 测试计划再验（UI-ADD-06 → EDIT/DEL） |
| A8 副作用 | 种子 133 启停验证（status 1→0→1）会改其 `update_time` | 开测前快照 update_time，验后按快照 SQL 回滚，保证种子哈希与基线一致 |

---

## 9. 风险与应对

| 风险 | 应对 |
|---|---|
| 架构任务单/口径未发布（K1–K8） | 先按本计划准备；任务单发布后 10 分钟内对齐映射并更新计划版本；**开测前必须确认 K1/K4/K6/K8** |
| devtools 热重启扰动（下午场他人编译） | 进窗口先健康探测；发现未加载新代码→等/报 web-control；不擅自重启 8080 |
| 写库污染/误删种子 | 仅 `ZZT5B-*`；开测前 3/6 行快照+哈希；收测比对；不删种子 |
| 种子 count=1 导致 UI 修改/删除不可用 | UI 用例先经新增造 count=0 测试计划再测编辑/删除 |
| plan 与项目下拉类型不匹配（projectId 数字 vs 字符串） | 用例 UI-EDIT-01 专项断言回填显示；若失败记 P2 |
| 子表事务/级联实现不严 | API-ADD-05/API-UPD-01/API-DEL-01 重点断言 DB 无残留 |
| 状态字典 0/2 差异 | K1 专项用例 + UI-SEARCH-02/UI-ADD-06 记录实际，任务单为准 |
| 浏览器/MySQL 争用 | 按 web-control 分段批准+优先续批；结束立即释放 |

---

## 10. 报告结构与交付

`docs/test/Web_Sprint5b_护理计划_测试报告.md` 必含：
1. 摘要：用例统计（按层/优先级）+ 结论（通过/有条件通过/不通过）；
2. 环境/窗口（DQ 编号、实际时长、影响行数、种子哈希、前端零改动证据）；
3. 缺陷清单（BUG-5B-xx：严重级/复现步骤/实际 vs 预期/证据）；
4. 任务单验收矩阵（按 arch 发布编号逐项 PASS/FAIL/N/A）+ `[待确认 K1–K8]` 结论；
5. 观察项/文档偏差/前端 quirk 记录；
6. 数据清理与恢复核验（3/6 行、ZZT5B=0、AUTO_INCREMENT 记录）；
7. 结论与建议 + 证据索引（`artifacts/test_web_s5b/`）。

---

## 11. 变更记录

| 版本 | 日期 | 变更 | 作者 |
|---|---|---|---|
| v1.0 | 2026-11-25 | 首版：范围/前端契约/DB 基线（3/6 行+count 语义+create_by bigint）/待确认口径 K1–K8/92 用例/窗口预案/风险/报告结构 | web-tester |
| v1.1 | 2026-11-25 | **任务单 Task-S5b-01 v0.1 发布后对齐**：K1–K8 → 已确认口径 C1–C11（status 观察 O-1、count 强制拦截、事务/子表校验、/all 形态）；API 用例扩至 52（新增 A8 引用校验/启停放行、A11 子表校验、A5/A6/A9 映射）；新增 §7.7 A1–A15 验收映射与 §8.1 任务单观察项；窗口申请补充 A8 快照回滚方案；预计 103 用例 | web-tester |
| v1.2 | 2026-11-25 | **任务单 v0.2 / lead 决策对齐**：①`/all` 返回 `List<NursingPlan>` 实体数组（非 Vo/label-value，全量含禁用）；②R2：主表 XML 自定义 SQL，**create_by/update_by 不要求有值（NULL 允许）**，createTime/updateTime 显式设置；③A5/A9 不再要求 create_by、改删必须用新增 count=0 计划；④A8 种子 133 启停验后按快照回滚 update_time；⑤补充 O-2/R7/A8 副作用观察 | web-tester |
