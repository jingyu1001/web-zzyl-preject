# Web Sprint 5c · 护理等级模块 CRUD 后端 任务单

> 任务编号：**Task-S5c-01**｜模块：服务管理-护理等级（`/nursing/level`）
> 目标：补齐**后端** 6 接口，使现有前端页面（**禁止修改前端代码**）完成 增/删/改/查/启停/下拉，并带出关联**护理计划名称 planName**。
> 发布：web-arch｜日期：2026-11-25 晚场 22:30 前｜执行：web-dev1｜评审：web-arch｜验收：web-tester/lead
> 参考：Task-S5-01/S5b-01 同模块范式；**R2 口径沿用**（主表 XML 自定义 SQL、create_by/update_by 不写、createTime/updateTime 显式、不改表）。

---

## 1. 范围与硬约束

| 项 | 要求 |
|---|---|
| 允许修改 | **仅后端**：①`mapper/NursingLevelMapper.java` **补 5 个方法声明**（实测原文件仅 `extends BaseMapper`，不补无法调用 XML；dev1 已补：`selectNursingLevelVoList/selectNursingLevelById/insert/update/deleteById`）；②`resources/mapper/nursing/NursingLevelMapper.xml` **仅补 `order by nl.id asc`**（VoList；原 SQL 全保留，无需 id 过滤/新增 join）；③新增 `service/INursingLevelService.java`、`service/impl/NursingLevelServiceImpl.java`、`controller/NursingLevelController.java` |
| 复用不改 | `domain/NursingLevel.java`、`vo/NursingLevelVo.java`（无需改）；`NursingPlanMapper`（已实现，planId 存在性校验）；`CheckInConfigMapper`（删除引用校验） |
| 禁止修改 | **前端任何代码**；数据库结构；其它模块；`application.yml`；C++ 端不动 |
| 事务 | 单表操作，**不要求多表事务**（删除前引用校验与删除之间仅理论竞态，可接受并记录） |
| 环境 | 编译=向 web-control **轻量报备**（开始/结束各一句）；8080 主动重启/浏览器/MySQL 写=正式申请；**8080 当前 PID 31708 为主人 IDEA 实例，严禁停/重启**；前端 88 不动 |

---

## 2. 只读核查结论（web-arch，2026-11-25 22:20–22:30）

| # | 检查项 | 结论 |
|---|---|---|
| 1 | 表 `nursing_level` | ✅ 3 行（76/77/78，name=1/2/3号护理等级，plan_id=133/134/135，fee=1500/2000/3000，status 全 1，description 有值）；`name varchar(50) NOT NULL UNIQUE`；**AUTO_INCREMENT=79（不硬编码 id）** |
| 2 | 表字段 | `id int auto`、`name varchar(50) NOT NULL UNIQUE`、`plan_id int NOT NULL`、`fee decimal(10,2) NOT NULL`、`status tinyint NOT NULL default 1`、`description varchar(255) NULL`、`create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP`、`create_by bigint NULL`、`update_time datetime DEFAULT CURRENT_TIMESTAMP on update`、`update_by bigint NULL`、`remark varchar(255)` |
| 3 | 后端既有 | ✅ `domain/NursingLevel.java`（id/name/planId/fee/status/description，extends BaseEntity）、`vo/NursingLevelVo.java`（含 **planName**、createTime）、`NursingLevelMapper.xml` SQL 齐全（含 VoList join planName）；⚠ **Mapper 接口原文件仅 `extends BaseMapper`（lead 口头核查有误）**，dev1 已补 5 个方法声明与 XML 对齐（必需改动）；❌ 原缺 **Service/ServiceImpl/Controller**（dev1 已新增） |
| 4 | XML 现状 | `selectNursingLevelVoList` 已 `join nursing_plan np on nl.plan_id=np.id` 并 `select ... np.plan_name planName`（**planName 关联已有，无需新增 join**）；**仅补 `order by nl.id asc`**（当前工作区已补）；detail 用 `selectNursingLevelById`（entity，不含 planName，前端不依赖）；entity list 保留不使用 |
| 5 | 前端契约 | `listLevel`（pageNum/pageSize/name/status → rows/total）、`getLevel/{id}`（data 回填表单，用 planId/name/fee/status/description）、`addLevel`、`updateLevel`（整表单；**启停只传 {id,status}**）、`delLevel/{id}`（单 id）、`getAllLevels /all`（入住办理 ConfigurationForm 用 `item.name/item.id`，并据选中项取 fee） |
| 6 | 前端页面 | 表格 8 列：序号/护理等级名称/**执行护理计划(planName)**/护理费用/状态/等级说明/创建时间/操作；弹窗 500px、5 字段（名称/护理计划下拉(planId)/费用/状态/说明），rules 5 项必填；费用 `el-input-number step=100` 无 min；计划下拉来源 `/nursing/plan/all`（已实现，返回 List<NursingPlan>） |
| 7 | 菜单/权限 | ✅ `sys_menu` 2007 护理等级（`nursing:level:list`）+2008 query/2009 add/2010 edit/2011 remove/2012 export 均存在；无需改库 |
| 8 | 引用/删除风险 | 全库仅 **`check_in_config.nursing_level_id`** 引用本表（当前 level 77 被 1 条入住配置引用）→ 删除存在孤儿风险；建议**删除前校验引用**（见 §4，D1 待 lead 确认） |
| 9 | planId 关联 | `nursing_plan` 3 条；种子 level 77 → plan 134（**plan 134 status=0 禁用**）→ **不可要求“计划必须启用”**，否则现有数据不可编辑；仅校验 planId **存在**（D2） |
| 10 | 状态字典 | `nursing_level_status`：启用=1 / 禁用=0（与 DB 一致，无 5b 的 1/2 vs 0/1 问题） |
| 11 | 审计类型 | `create_by/update_by bigint`（同 5b）→ **沿用 R2：XML 自定义 SQL，不写这两列（NULL），createTime/updateTime 显式设置，不改表** |

---

## 3. 接口契约（6 个，全部 `/nursing/level`）

| # | 方法/路径 | 入参 | 出参 | 权限 | 要点 |
|---|---|---|---|---|---|
| 1 | `GET /nursing/level/list` | `pageNum,pageSize,name,status` | `TableDataInfo{total, rows[]}` | `nursing:level:list` | rows = **`NursingLevelVo`**（含 `planName`/fee/status/description/createTime）；`name like`、`status eq`；**按 `id asc` 排序**；`startPage()+getDataTable` |
| 2 | `GET /nursing/level/{id}` | path id | `AjaxResult.data=NursingLevel` | `nursing:level:query` | 用现有 `selectNursingLevelById`（实体：id/name/planId/fee/status/description/createTime；**planName 不强制**，前端用 planId，列表已带 planName）；不存在返回 success(null) |
| 3 | `POST /nursing/level` | JSON `NursingLevel` | `AjaxResult` | `nursing:level:add` | 校验后端（§4）；XML 自定义 insert；createTime 显式；createBy 不赋值 |
| 4 | `PUT /nursing/level` | JSON `NursingLevel` | `AjaxResult` | `nursing:level:edit` | **两种语义**：a) 整表单（name/planId/fee/status/description）全量更新；b) **启停只传 `{id,status}`** → 仅更新 status+updateTime，null 不覆盖（见 §4.3） |
| 5 | `DELETE /nursing/level/{id}` | path 单 id | `AjaxResult` | `nursing:level:remove` | 先做引用校验（§4.5）；XML delete；不存在返回“护理等级不存在” |
| 6 | `GET /nursing/level/all` | 无 | `AjaxResult.data=List<NursingLevel>` | `nursing:level:list` | 入住办理下拉用（消费 `item.name/id`，取 fee）；**lead 决策：返回 List<NursingLevel> 实体、全量 3 条（含禁用）、按 id asc**（不是 {label,value}、不是 Vo）；实现用 MP `list(orderByAsc(id))` 即可 |

---

## 4. 校验与业务规则（必须）

1. **名称**：`name` trim + 必填 + **≤50**（DB varchar(50)）；新增/修改（排除自身 id）唯一预校验 + `DuplicateKeyException` 兜底；中文提示“护理等级名称已存在”。
2. **planId**：必填；**存在性校验**（`NursingPlanMapper.selectNursingPlanById(planId)` 非空，否则“护理计划不存在”）；**不校验计划启用状态**（种子 level 77→plan 134 禁用，D2）。
3. **fee**：必填（非 null）；建议 `fee >= 0`（前端 step=100 无 min，后端不做上限）；DB decimal(10,2)。
4. **status**：必填（前端 radio 必选）；新增未传时默认 **1**；允许 0/1。
5. **description**：前端规则必填 → 后端按必填校验（trim 非空，≤255）；`remark` 可空。
6. **启停部分更新**：判定 `name == null && planId == null && fee == null && description == null && status != null` → 仅 `updateNursingLevel` 更新 id/status/updateTime；不做其他字段覆盖、不做引用拦截。
7. **整表单更新**：校验名称唯一、planId 存在、字段必填；`updateNursingLevel` XML 部分更新（null 不覆盖）；`updateTime` 显式设置；**createTime 不变**。
8. **删除引用校验（lead 已决策：拒绝）**：删除前统计 `check_in_config` 中 `nursing_level_id = id` 的条数（注入 `CheckInConfigMapper`）；>0 → `AjaxResult.error("该护理等级已被入住配置引用，无法删除")`；测试删除必须用**新增等级**（种子 77 预期被拒）。**整表单修改与启停放行**（不因引用拦截；`check_in_config.nursing_level_name` 历史值不跟随属既有设计，记为观察项）。
9. **R2 审计字段**：insert/update 走 Mapper XML 自定义 SQL，**不使用 MP `save/updateById`**（避免 MyMetaObjectHandler 写 bigint）；`createBy/updateBy` 不赋值（NULL）；`createTime` 新增显式、`updateTime` 更新/启停显式；**严禁 ALTER 表**。
10. **不新增依赖、不改表、不改前端、不改 plan 模块**；**本期不做导出**（前端无导出按钮、api 无 export 方法；sys_menu 2012 export 权限仅保留不实现）。

---

## 5. 后端文件清单与职责

| # | 文件 | 动作 | 职责 |
|---|---|---|---|
| 1 | `mapper/NursingLevelMapper.java` | **修改（补声明，已由工作区/lead 核查确认）** | `@Mapper extends BaseMapper<NursingLevel>`；声明 XML 已有方法：`List<NursingLevelVo> selectNursingLevelVoList(NursingLevel)`、`NursingLevel selectNursingLevelById(Long)`、`int insertNursingLevel(NursingLevel)`、`int updateNursingLevel(NursingLevel)`、`int deleteNursingLevelById(Long)` |
| 2 | `resources/mapper/nursing/NursingLevelMapper.xml` | **修改（仅 order by）** | 在 `selectNursingLevelVoList` 末尾补 `order by nl.id asc`；**原 join/字段/insert/update/delete SQL 全保留，无需 id 过滤或新增 join** |
| 3 | `service/INursingLevelService.java` | **新建** | `selectNursingLevelList`、`selectNursingLevelDetail(Long)`、`insertNursingLevel`、`updateNursingLevel`、`deleteNursingLevelById`、`selectNursingLevelAll()`、`checkNameUnique` |
| 4 | `service/impl/NursingLevelServiceImpl.java` | **新建** | `extends ServiceImpl<NursingLevelMapper, NursingLevel>`；list 走 `selectNursingLevelVoList`（含 planName）；detail 走 `selectNursingLevelById`；`/all` **返回 List<NursingLevel> 实体**（MP `list(orderByAsc(id))`，含禁用）；CRUD 走 XML；校验（§4）；planId 存在性校验（注入 `NursingPlanMapper`）；删除引用校验（注入 `CheckInConfigMapper`）；createTime/updateTime 显式；createBy/updateBy 不赋值；启停部分更新 |
| 5 | `controller/NursingLevelController.java` | **新建** | `@RestController @RequestMapping("/nursing/level")`、`extends BaseController`；6 接口；5 权限点；写操作 `@Log(title="护理等级", INSERT/UPDATE/DELETE)`；list `startPage()+getDataTable`；delete 单 id |

---

## 6. 验收 A 清单（web-tester；前置：dev1 完成+编译通过+轻量报备）

| # | 用例 | 预期 |
|---|---|---|
| A1 | 静态/变更范围 | 前端零改动（git status/diff 空）；后端仅：`NursingLevelMapper.java` 补声明 + `NursingLevelMapper.xml` 仅补 order by + 3 个新文件；未改表/配置/其它模块 |
| A2 | 列表 | `GET /nursing/level/list?pageNum=1&pageSize=10` → `total=3, rows=3`；按 id asc = 76/77/78；每行含 **planName**（1号/2号/3号护理计划）、fee（1500/2000/3000）、status=1、description、createTime |
| A3 | 搜索/分页 | `name=2号`→1 条（77）；`status=1`→3 条；`status=0`→0 条；`pageSize=2`→第1页 2 条、第2页 1 条 |
| A4 | 详情 | `GET /nursing/level/77` → data.planId=134、name=2号护理等级、fee=2000、status=1、description=我是2号哦（**planName 不要求**；列表 A2 已验 planName 关联） |
| A5 | 新增 | POST `{name:"ZZT5C等级",planId:133,fee:1234.5,status:1,description:"测试等级"}` → 200；列表 total=4；新 id=**实际自增（基线 79，不硬编码）**；DB 行存在、create_time 非空、**create_by 可为 NULL（R2）** |
| A6 | 整表单修改 | PUT 新等级改 name/fee/planId/status/description → 200；DB 全部生效；列表 planName 随 planId 联动变化；createTime 不变；**对种子/被引用等级修改也允许（不拦截，测试用可恢复字段）** |
| A7 | **启停部分更新（关键）** | PUT `{id:<A5新id>,status:0}` → 200；详情 status=0，**name/planId/fee/description 均未变**；再恢复 1 |
| A8 | 名称唯一 | POST 重名“1号护理等级” → 友好错误、无新增；PUT 新等级改名“1号护理等级” → 友好错误、原值不变 |
| A9 | planId 校验 | POST planId=999999 → 友好错误“护理计划不存在”、无新增；POST planId=134（**计划禁用**）→ 允许（D2，现有数据一致性要求） |
| A10 | 删除 | 新建等级后 DELETE → 200、DB 0 残留；DELETE 种子 77（被 check_in_config 引用）→ 友好错误“该护理等级已被入住配置引用，无法删除”且数据不变（lead 已决策）；DELETE 不存在 id → “护理等级不存在” |
| A11 | `/all` | GET `/nursing/level/all` → `data` 为 **List<NursingLevel> 实体数组、length=3（含禁用）**、按 id asc、含 id/name/planId/fee/status/description；入住办理表单等级下拉可显示 3 项并回填 fee |
| A12 | 前端页面 | 打开 `/nursing/level`：列表 3 行、**“执行护理计划”列显示 planName**、新增/编辑/删除(新)/启停按前端既有行为工作；**前端代码未改** |
| A13 | 权限 | admin 正向全通（负向 403 本轮 N/A，同 S5/S5b 口径） |
| A14 | 数据恢复 | `nursing_level=3`、`check_in_config` 不变、无 ZZT5C 残留、种子字段不变（自增会因测试推进，非缺陷）；MySQL 写后清理并回填实际行数 |

---

## 7. 风险 / 需 lead 决策

| # | 类别 | 内容 | 默认处置 |
|---|---|---|---|
| R1 | 类型（R2 沿用） | `create_by/update_by` DB=bigint，BaseEntity=String | 不改表；XML 自定义 SQL；两列不赋值（NULL）；createTime/updateTime 显式；验收不要求 create_by 有值 |
| R2 | 数据 | **level 77 被 `check_in_config` 引用 1 条** | 删除前校验并拒绝（D1）；测试删除用新增等级 |
| R3 | 数据 | level 77 → plan 134（**禁用**） | planId 仅校验存在、不校验启用（D2），否则现有数据无法编辑 |
| R4 | 自增 | `nursing_level` AUTO_INCREMENT=79 | 新 id 以实际返回为准，不硬编码 |
| R5 | XML | entity list 无 planName；VoList 已 join 但无 id/order | 列表/详情统一用 VoList；XML 只加 id 过滤 + `order by nl.id asc`，原 SQL 保留 |
| R6 | 观察项 | 前端表格无 count/灰置逻辑 | 删除引用拦截后，tester 点种子 77 会看到后端错误（预期，非前端缺陷） |
| — | **lead 已决策（2026-11-25 晚场）** | D1 删除引用校验：被 `check_in_config` 引用 → 友好拒绝（修改/启停放行）；D2 planId 仅校验存在、**允许禁用计划**（种子 77→134 禁用）；D3 `/all` 返回 `List<NursingLevel>` 实体、**全量 3 条含禁用**；D4 本期**不做导出** | 已确认，dev1 按此实现 |

---

## 8. 角色分工与时间盒

| 角色 | 动作 | 时间盒 |
|---|---|---|
| web-dev1 | 实现（Mapper 声明 + XML VoList + 3 文件）；自测；编译前后向 web-control 轻量报备 | 60~90 分钟 |
| web-arch | 完成后 **10 分钟** review（§9）给 PASS/打回并上报 lead | ≤10 分钟 |
| web-tester | PASS 后按 A1–A14 验收（DQ 申请 8080 稳定窗口+浏览器+MySQL 写；基线 level=3、check_in_config 不变；测试后清理） | 20~30 分钟 |
| web-control | 编译轻量报备、写库/浏览器窗口 FIFO 审批与核查 | 全程 |

---

## 9. web-arch Review 清单

- [ ] 仅约定文件变更；前端/DB/其它模块零改动
- [ ] Mapper 接口补齐 XML 全部使用的方法；namespace 一致
- [ ] XML VoList：join/planName 保留、**仅补 `order by nl.id asc`**；原 SQL 未删
- [ ] list 返回 Vo（planName 正确）、detail 返回实体、`/all` 返回 List<NursingLevel>（含禁用）；list `startPage()+getDataTable`
- [ ] Controller 6 接口/5 权限点/`@Log`；delete 单 id
- [ ] R2：insert/update 走 XML（非 MP save/updateById）；createBy/updateBy 未赋值；createTime/updateTime 显式；未 ALTER
- [ ] 校验：name trim/必填/≤50/唯一（排除自身 + DuplicateKeyException）；planId 必填+存在；fee 必填；status 默认 1；description 必填/≤255
- [ ] 启停仅 id+status 时只更新 status/updateTime，不覆盖其它字段
- [ ] 删除前 `check_in_config` 引用校验，>0 友好拒绝
- [ ] `/all` 返回 List<NursingLevel>（实体数组，全量含禁用，按 id asc）；未加缓存/未新增依赖
- [ ] 无全表更新/删除、无 SQL 拼接

---

## 10. 变更记录

| 版本 | 日期 | 变更 | 作者 |
|---|---|---|---|
| v0.1 | 2026-11-25 | 建立 Task-S5c-01 任务单：核查结论（表/索引/引用/权限/XML/planName 关联）、6 接口契约、校验与 D1–D3 决策、A1–A14 验收、review 清单 | web-arch |
| v0.2 | 2026-11-25 | 并入 lead 补充核查与决策：①NursingLevelMapper.java 接口已齐（XML 对应 SQL 齐全），**只缺 Service/ServiceImpl/Controller**；②XML **仅补 order by nl.id asc**（无需 id 过滤/join 改动）；③删除引用校验 lead 已决策：被 check_in_config 引用→友好拒绝，**修改/启停放行**；④/all 返回 **List<NursingLevel> 实体（含禁用）**；⑤detail 用 selectNursingLevelById（entity，planName 不要求）；⑥本期**不做导出**；A1–A14 同步对齐 | web-arch |
| v0.3 | 2026-11-25 | Review 通过（PASS）：实际改动=NursingLevelMapper.java 补 5 方法声明 + XML 仅补 order by + 3 个新文件；前端零改动；R2/校验/启停/删除引用拦截/all 实体均符合；编译 BUILD SUCCESS ×2、数据恢复（level=3/plan=3/sub=6、ZZT5C=0、CHECKSUM=586424728）；**事实修正**：原 Mapper 实际仅 extends BaseMapper（lead 口头核查有误），接口声明为必需改动 | web-arch |
| v0.4 | 2026-11-25 | 验收通过：A1–A14 全 PASS（API 45/45、UI 36/36、STA 7/7，合计 88/88，P1/P2=0）；数据基线恢复（level=3/plan=3/sub=6/cfg=1、ZZT5C=0、四项 CHECKSUM 与 web-control 基线一致）；可交付 lead；代码待统一提交 | web-arch |

---

## 11. Review 结论（web-arch，2026-11-25 22:32）

**结论：✅ PASS（通过，无阻塞项）** → 已通知 web-tester 按 A1–A14 验收。

| # | 审查项 | 结果（只读核验） |
|---|---|---|
| ① | 变更范围 | `git diff --stat`：仅 `NursingLevelMapper.java`(+42 方法声明) 与 `NursingLevelMapper.xml`(+1 `order by nl.id asc`)；另 3 个新文件；前端改动 **0**；未改表/配置/其它模块 |
| ② | Mapper/XML | 接口 5 方法声明与 XML 完全对齐；XML 仅补 order by，原 join/字段/CRUD SQL 全保留；list 用 VoList（planName join 生效） |
| ③ | R2 | insert/update/delete 全走 Mapper XML（未用 MP save/updateById）；createTime 新增显式、updateTime 更新/启停显式；createBy/updateBy 未赋值（NULL）；未 ALTER 表 |
| ④ | 校验 | name trim+必填+≤50+唯一（排除自身+DuplicateKeyException）；planId 必填+存在性（不存在→“护理计划不存在”；不校验启用，兼容种子 77→plan134 禁用）；fee 必填；description 必填+≤255；status 默认 1 |
| ⑤ | 启停部分更新 | `isEnableOrDisable` 判定（name/planId/fee/description 全 null 且 status 非 null）→ XML 部分更新仅 status/updateTime，不覆盖其它字段；A7 自测 name/planId/fee/description 均未变 |
| ⑥ | 删除引用拦截 | 删除前 `check_in_config.nursing_level_id` selectCount>0 → “该护理等级已被入住配置引用，无法删除”；修改/启停放行；自测 DELETE 77 被拒且数据不变、新增行删除成功 |
| ⑦ | 接口形状 | list 返回 Vo 行（含 planName、id asc）；detail 返回实体（planName 不要求）；`/all` 返回 **List<NursingLevel>** MP list orderByAsc(id)（含禁用）；delete 单 id；5 权限点+@Log；未实现导出 |
| ⑧ | 编译/自测/数据 | `mvn -DskipTests compile` BUILD SUCCESS ×2（10 模块）；web-control 2 轮轻量报备闭环，devtools 自愈，**PID 31708 未动**；自测 list/detail/all/新增(create_by=NULL)/重名/非法 planId/缺 description/整表单修改/启停/删除引用拦截/删除新行 全通过；DB 恢复 level=3/plan=3/sub=6、ZZT5C=0、CHECKSUM=586424728 与基线一致 |

**非阻塞观察（不要求本轮修改）**
- N1：整表单更新时前端会回传 createTime，XML update 会按原值写回（值不变）；若外部篡改 createTime 可被改，建议后续可将 createTime 从 update SQL 移除（仅记录）；
- N2：`check_in_config.nursing_level_name` 为历史快照，等级改名不跟随（lead 已确认属既有设计，不判缺陷）；
- N3：fee 未做 >=0 校验（前端无 min；DB 仅 NOT NULL），不阻塞；
- N4：导出未实现（前端无按钮/API，符合本期范围）；
- N5：代码工作区未 commit（等 lead 统一提交）。

**生效条件**：web-tester 按 A1–A14 实测通过（删除必须用新增等级；改/删引用 77 预期被拒；数据基线 level=3/plan=3/sub=6，测试后清理并回填）。

---

## 12. 验收结论（web-tester 最终结果，2026-11-25 22:36；web-arch 抽验）

**结论：✅ A1–A14 全部 PASS（STA 7/7、API 45/45、UI 36/36，合计 88/88；P1/P2=0，无需返修）→ 可交付 lead/主人验收。**

| 项 | 证据（web-arch 抽验） |
|---|---|
| 测试报告 | `docs/test/Web_Sprint5c_护理等级_测试报告.md`（A1–A14 矩阵/证据/轮次归因） |
| API 终轮 | `artifacts/test_web_s5c/api/level_api_results_20261125_223243.json` → **45/45 PASS**（抽验 JSON 无 non-pass） |
| UI 终轮 | `artifacts/test_web_s5c/ui/level_ui_results_level_ui_20261125143413.json` → **36/36 PASS**（counts total=36/pass=36/fail=0） |
| list | total=3、ids 76/77/78、planName=1/2/3号护理计划、fee 正确、id asc |
| detail | 77→planId=134、fee=2000、status/description 正确；实体无 planName（按口径不要求） |
| /all | List<NursingLevel> 实体 length=3、id asc、含 id/name/fee/status（符合口径） |
| A7 启停 | PUT {id,status} 只改 status，name/planId/fee/description 全保持 |
| A9 planId | 999999→“护理计划不存在”；planId=134（禁用）允许（D2） |
| A10 删除拦截 | DELETE 77→“该护理等级已被入住配置引用，无法删除”，77 与 check_in_config 不变；新等级删除 200、total 回 3、DB 0 残留 |
| A1 前端零改动 | git status/diff 空、三文件 hash=HEAD；UI 37 个 `/dev-api/nursing/level` 请求全 200 |
| 数据恢复 | level=3/plan=3/sub=6/cfg=1、ZZT5C=0；**CHECKSUM 586424728/2521898468/2554906448/680483255 与 web-control 基线完全一致**；种子 76/77/78 未变 |
| 观察项（不判缺陷） | fee 负值后端未拦截（任务单“建议 ≥0”，P3 记录）；XML VoList 无 id 过滤（详情用 selectById，无功能影响）；列表无 count 灰置/查看按钮属前端既有设计；首轮 W1 API 41/45 为测试脚本适配（decimal 比较/detail 口径），修正后 45/45 |

**交付状态**：代码工作区变更（`NursingLevelMapper.java` + XML + 3 新文件）尚未 commit（等 lead 统一提交）；前端零改动；无 DB 结构变更；C++ 端无契约影响。
