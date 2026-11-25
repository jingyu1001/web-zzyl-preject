# Web Sprint 5b · 护理计划模块 CRUD 后端 任务单

> 任务编号：**Task-S5b-01**｜模块：服务管理-护理计划（`/nursing/plan`）
> 目标：补齐**后端** 6 个接口，使现有前端页面（**禁止修改前端代码**）完成 增/删/改/查/启停/下拉、主表+子表事务一致性。
> 发布：web-arch｜日期：2026-11-25 下午场｜执行：web-dev1｜评审：web-arch｜验收：web-tester/lead
> 参考实现：上午 Task-S5-01（护理项目 CRUD）与 `RoomType*` 同模块范式；子表 Mapper 已存在（复用，不新建）。

---

## 1. 范围与硬约束

| 项 | 要求 |
|---|---|
| 允许修改 | **仅后端**：①`zzyl-nursing-platform/.../nursing/` 新增 `mapper/NursingPlanMapper.java`、`service/INursingPlanService.java`、`service/impl/NursingPlanServiceImpl.java`、`controller/NursingPlanController.java`；②修改 `resources/mapper/nursing/NursingPlanMapper.xml`（补 `count` 映射 + list 子查询/排序，保留原 SQL） |
| 复用不改 | `NursingProjectPlanMapper.java` / `NursingProjectPlanMapper.xml`（已有 `batchInsert/selectByNursingPlanId/deleteByNursingPlanId` 等，直接复用） |
| 禁止修改 | **前端任何代码**；数据库结构；其它模块；`application.yml`；C++ 端本轮不动 |
| 事务 | 新增/修改/删除的**主表+子表**必须在同一 `@Transactional` 内（见 §4） |
| **R2 专项（lead 决策，2026-11-25）** | `nursing_plan` 的 **insert/update 走 Mapper XML 自定义 SQL**（`insertNursingPlan/updateNursingPlan`），**不触发 MyBatis-Plus 全局 `MyMetaObjectHandler` 的字符串自动填充**；`create_by/update_by` 不赋值（存 NULL，与 3 条种子一致）或按需显式写数字；**严禁 ALTER 表改类型**；若严格模式报错，优先 SQL 层不写这两列；验收不要求 create_by 有值 |
| 环境 | 编译=向 web-control **轻量报备**（开始/结束各一句；上午有未报备违规 V-01，务必先报）；8080 主动重启/浏览器/MySQL 写=**正式申请**；不得停/重启主人 IDEA 实例（java PID 23192）；前端 88 本时段不动 |

---

## 2. 只读核查结论（web-arch，2026-11-25 14:55–15:10）

| # | 检查项 | 结论 |
|---|---|---|
| 1 | 表 `nursing_plan` | ✅ 3 行（133 1号/启用、134 2号/**status=0 禁用**、135 3号/启用；sort_no 均 1）；**`plan_name` 有 UNIQUE 索引**；`AUTO_INCREMENT=141`（**不硬编码 id**，以实测自增为准） |
| 2 | 表 `nursing_project_plan` | ✅ 6 行（133→project 1,2；134→1；135→1,5,4）；**`plan_id` 无索引**（仅主键；数据量小可接受，性能观察项）；`plan_id/project_id/execute_time/execute_cycle/execute_frequency` 均 **NOT NULL** |
| 3 | 后端既有 | ✅ `domain/NursingPlan.java`（含 `@TableField(exist=false) Long count`）、`domain/NursingProjectPlan.java`、`dto/NursingPlanDto.java`（含 `List<NursingProjectPlan> projectPlans`）、`vo/NursingPlanVo.java`（含 `List<NursingProjectPlanVo> projectPlans`）、`vo/NursingProjectPlanVo.java`、`mapper/NursingProjectPlanMapper.java` + 两个 XML 均存在；❌ **缺** `NursingPlanMapper.java` 接口、`INursingPlanService`、`NursingPlanServiceImpl`、`NursingPlanController` |
| 4 | 既有 XML 现状 | `NursingPlanMapper.xml`：resultMap **无 count 映射**、list **无 order by**、无 count 子查询；`NursingProjectPlanMapper.xml`：`batchInsert(planId,list)` 已按 NOT NULL 列写入 + `sysdate()`、`selectByNursingPlanId` 返回 `NursingProjectPlanVo`、`deleteByNursingPlanId` 注解 SQL 已存在 |
| 5 | 菜单/权限 | ✅ `sys_menu` 2013 护理计划（`nursing:plan:list`）+ 2014 query/2015 add/2016 edit/2017 remove/2018 export 均存在；无需改库 |
| 6 | 引用依赖 | `nursing_level.plan_id` → 3 个计划**各被 1 条护理等级引用**（count=1）；`nursing_project_plan.plan_id` 子表。**无其它表引用 plan_id**（information_schema 全库核查） |
| 7 | `/nursing/plan/all` 使用方 | 仅前端 `views/nursing/level/index.vue`（护理等级下拉）。护理等级页自身列表接口仍缺 Controller，本任务只保证该下拉接口可用 |
| 8 | 表结构 vs 老师 DDL | 当前库实测 **`create_by/update_by` 为 `bigint`**（与老师 DDL 一致），而 RuoYi `BaseEntity` 是 `String` → **不是建表差异，是框架类型差异**；映射见 §3.4。老师建表语句未在仓库落盘（仅有 lead 转述） |
| 9 | 自增/种子 | `nursing_plan` 自增 **141**、`nursing_project_plan` 现有最大 id 1743 → 测试新增用**实际返回 id**，禁止硬编码 |
| 10 | 状态字典 | `nursing_plan_status`：启用=1 / 禁用=2（前端标签按 `status===1 ? 启用 : 禁用`）；DB 种子 134 为 **0** → 见观察项 O-1 |

---

## 3. 数据与接口契约

### 3.1 表字段（实测）

`nursing_plan`：`id int PK auto`、`sort_no int NULL`、`plan_name varchar(255) NOT NULL UNIQUE`、`status tinyint NOT NULL default 0`、`create_time datetime NOT NULL`、`update_time datetime NULL`、`create_by bigint NULL`、`update_by bigint NULL`、`remark varchar(255) NULL`

`nursing_project_plan`：`id int PK auto`、`plan_id int NOT NULL`、`project_id int NOT NULL`、`execute_time varchar(50) NOT NULL`（HH:mm:ss）、`execute_cycle int NOT NULL`（0 天/1 周/2 月）、`execute_frequency int NOT NULL`、`create_time datetime NOT NULL`、`update_time datetime NULL`、`create_by/update_by bigint NULL`、`remark varchar(255) NULL`

### 3.2 接口（6 个，全部 `/nursing/plan`，前端既有调用，不得改前端）

| # | 方法/路径 | 入参 | 出参 | 权限 | 要点 |
|---|---|---|---|---|---|
| 1 | `GET /nursing/plan/list` | `pageNum,pageSize,planName,status` | `TableDataInfo{total, rows[]}` | `nursing:plan:list` | 行对象 = `NursingPlan`（含 `count` 引用数 + `createTime`）；`planName like`、`status eq`；**按 `sort_no asc, id asc` 排序** |
| 2 | `GET /nursing/plan/{id}` | path id | `AjaxResult.data=NursingPlanVo` | `nursing:plan:query` | **必须带 `projectPlans[]`**（前端 `formData.projectPlans` 直接回填子表）；projectId 为字符串、executeCycle 为字符串（见 §3.4） |
| 3 | `POST /nursing/plan` | JSON `NursingPlanDto` | `AjaxResult` | `nursing:plan:add` | 校验子表后，主表+子表同事务；新 id 自增（实测当前 141，不硬编码） |
| 4 | `PUT /nursing/plan` | JSON `NursingPlanDto` | `AjaxResult` | `nursing:plan:edit` | **两种语义**：a) 整表单（含 planName/projectPlans）→ 全量更新主表 + 重建子表；b) **启停只传 `{id,status}`** → 仅更新 status，**null 字段不覆盖、子表不动**（见 §3.3） |
| 5 | `DELETE /nursing/plan/{id}` | path id（**单 id**，前端 `delPlan(row.id)`） | `AjaxResult` | `nursing:plan:remove` | 先校验引用数；同事务删子表 + 主表 |
| 6 | `GET /nursing/plan/all` | 无 | `AjaxResult.data=List<NursingPlan>`（**lead 决策：返回实体列表，不是 {label,value}**） | `nursing:plan:list`（或 query） | 护理等级下拉用（前端读 `item.id/item.planName`）；**返回全量 3 条（含禁用）**，按 `sort_no,id` |

### 3.3 启停“部分更新”语义（前端 `handleEnable` 已确认）

- 前端：`updatePlan({ id: row.id, status: status === 0 ? 1 : 0 })`，**只有 id+status**；
- 后端判定：`planName == null && projectPlans == null && status != null` → **仅 status 更新**（`updateById`，null 不覆盖），**不做引用数拦截**（启停不影响引用关系）；
- 全量更新判定：`planName != null || projectPlans != null` → 走“整表单”分支（校验名称唯一、引用数、重建子表）；
- 成功返回 200；id 不存在返回 `AjaxResult.error("护理计划不存在")`。

### 3.4 字段映射与类型（重点：create_by bigint / VO 字符串）

| 场景 | DB | Java | 处理 |
|---|---|---|---|
| 主表审计字段 | `create_by/update_by bigint` | `BaseEntity.createBy/updateBy String` | **R2 lead 决策：不改表**；主表 insert/update 走 XML 自定义 SQL，**不赋值 create_by/update_by（NULL，与种子一致）**，不触发 MyMetaObjectHandler；**验收不要求 create_by 有值**（A5 按此口径）；若严格模式报错优先 SQL 不写这两列 |
| 主表 id | int | Long（`IdType.AUTO`） | 正常映射 |
| 子表 projectId | int | domain `Long` / VO **`String`** | 前端下拉 value 为字符串，`selectByNursingPlanId` 返回 Vo 的 projectId 必须是字符串（MyBatis 自动 Long→String 转换，验收 A6 核对） |
| 子表 executeCycle | int | domain `Integer` / VO **`String`** | 前端下拉 value '0'/'1'/'2' 字符串；详情返回必须是字符串 |
| 子表 executeFrequency | int | domain `Integer` / VO **`Long`** | 数字，前端 input-number 可接受 |
| 请求 JSON 字符串数字 | — | `NursingPlanDto`/`NursingProjectPlan` 数字类型 | Jackson 自动把 `"1"` 转数字，无需额外处理；空串会转 null → 由 §4 校验拦截 |

### 3.5 后端文件清单与职责

| # | 文件 | 动作 | 职责 |
|---|---|---|---|
| 1 | `mapper/NursingPlanMapper.java` | **新建** | `@Mapper interface NursingPlanMapper extends BaseMapper<NursingPlan>`；声明 XML 全部方法：`selectNursingPlanList(NursingPlan)`、`selectNursingPlanById(Long)`、`insertNursingPlan(NursingPlan)`、`updateNursingPlan(NursingPlan)`、`deleteNursingPlanById(Long)`；namespace 必须与 XML 一致 |
| 2 | `resources/mapper/nursing/NursingPlanMapper.xml` | **修改（resultMap + list）** | ①resultMap 增加 `<result property="count" column="count"/>`；②list SQL 增加 `(select count(*) from nursing_level l where l.plan_id = nursing_plan.id) as count` 并补 `order by sort_no asc, id asc`；③原 insert/update/delete SQL 全保留并**作为主表写操作入口**（R2 决策）；④`/all` 用 MP wrapper 实现，不改 XML |
| 3 | `service/INursingPlanService.java` | **新建** | `selectNursingPlanList`、`selectNursingPlanDetail(Long)`（返回 Vo 含子表）、`insertNursingPlan(NursingPlanDto)`、`updateNursingPlan(NursingPlanDto)`、`deleteNursingPlanById(Long)`、`selectNursingPlanAll()`（返回 `List<NursingPlan>`）、`checkPlanNameUnique(NursingPlan)` |
| 4 | `service/impl/NursingPlanServiceImpl.java` | **新建** | `extends ServiceImpl<NursingPlanMapper, NursingPlan>`；列表走 XML；详情=主表+`nursingProjectPlanMapper.selectByNursingPlanId`；**主表 CRUD 一律走 Mapper XML 自定义 SQL**（`insertNursingPlan/updateNursingPlan/deleteNursingPlanById`，绕开 MP 全局自动填充，R2）；Service 内显式设置 `createTime`（新增）/`updateTime`（更新），createBy/updateBy 不赋值；新增/修改/删除 `@Transactional(rollbackFor=Exception.class)`；子表校验；名称唯一；引用数校验；子表重建：`deleteByNursingPlanId` + `batchInsert(list, planId)`；启停部分更新走 XML `updateNursingPlan`（仅 id/status/updateTime）；all=MP wrapper 排序后返回实体列表 |
| 5 | `controller/NursingPlanController.java` | **新建** | `@RestController @RequestMapping("/nursing/plan")`、`extends BaseController`；6 接口（list `startPage()+getDataTable`、getInfo、add、edit、remove、all）；5 权限点；写操作 `@Log(title="护理计划", INSERT/UPDATE/DELETE)`；`@Api/@ApiOperation` 可选 |

---

## 4. 事务、校验与业务规则（必须）

1. **主表+子表同事务**：新增 = `mapper.insertNursingPlan(主表)`（XML 自定义 SQL）→ 回填 `planId` → `batchInsert(子表, planId)`；修改（整表单）= `mapper.updateNursingPlan(主表)` → `deleteByNursingPlanId(id)` → `batchInsert`；删除 = `deleteByNursingPlanId` → `mapper.deleteNursingPlanById(id)`。任一步失败整体回滚（`@Transactional(rollbackFor=Exception.class)`）。
2. **子表校验（后端必做，前端无子表 rules）**：`projectPlans` 非空；每行 `projectId`、`executeTime`（HH:mm:ss）、`executeCycle`（0/1/2）、`executeFrequency`（1~7）必填；同一计划内 `projectId` 不得重复（前端已去重，后端兜底）；失败返回友好错误（如“护理项目不能为空/期望服务时间不能为空/请勿选择重复的护理项目”），**不得留脏主表**。
3. **名称唯一**：`planName` trim + 必填 + ≤255；新增/修改（排除自身 id）预校验；`DuplicateKeyException` 兜底（DB plan_name UNIQUE）；中文提示“护理计划名称已存在”。
4. **count 引用校验（`nursing_level.plan_id`）**：
   - list 返回 `count`（该计划被护理等级引用数，当前种子均=1）；前端 `count>0` 时“修改/删除”按钮灰置（仅 CSS，**点击仍会触发**）；
   - **后端强制**：整表单修改 / 删除时若 count>0 → `AjaxResult.error("该护理计划已被护理等级引用，无法修改或删除")`；**启停（只传 id+status）不受限制**。
5. **排序/分页**：list `order by sort_no asc, id asc`（PageHelper 会拼 limit，排序必须进 SQL）；`pageNum/pageSize` 默认 1/10。
6. **默认值**：新增未传 `status` 时默认 1（前端默认传 '1'）；未传 `sortNo` 默认 1；`remark` 可空。
7. **`/all`（lead 决策）**：返回 **`List<NursingPlan>` 实体列表（不是 `{label,value}`）**，前端读 `item.id/item.planName`；**返回全量 3 条（含禁用）**，按 `sort_no,id`；不返回子表。
8. **审计字段（R2 lead 决策）**：主表 **insert/update 必须走 Mapper XML 自定义 SQL**（`insertNursingPlan/updateNursingPlan`），**不使用 MyBatis-Plus 全局自动填充**（避免把字符串写进 bigint）；`createBy/updateBy` 不赋值（NULL，与种子一致）或按需显式写数字；**`createTime` 在 Service 显式设置**（DB `create_time NOT NULL` 且无默认值，XML 不写会插入失败）、更新时显式设置 `updateTime`；**禁止 ALTER 表**；若严格模式仍报错，优先 SQL 层不写 create_by/update_by。
9. **status 取值（lead 口径）**：**本模块 status 不限制取值**（字典 1/2 与库 0/1 不一致）；后端接受 0/1/2，不因非 1 报错；**tester 不得判为缺陷**。
10. **不新增依赖、不改表、不改 `NursingProjectPlanMapper`、不改前端**。

---

## 5. 验收 A 清单（web-tester；前置：dev1 完成 + 编译通过 + web-control 轻量报备）

| # | 用例 | 预期 |
|---|---|---|
| A1 | 静态/变更范围 | 前端零改动（git status/diff 空）；后端仅新增 4 文件 + `NursingPlanMapper.xml`（resultMap/list 子查询+排序）；未改表/配置/其它模块 |
| A2 | 列表 | `GET /nursing/plan/list?pageNum=1&pageSize=10` → `total=3, rows=3`；顺序 sort_no,id → ids 133/134/135；每行含 `count=1`、`createTime`；status 133=1、134=0、135=1 |
| A3 | 搜索/分页 | `planName=1号` → 1 条（133）；`status=1` → 2 条（133/135）；`status=0` → 1 条（134）；`pageSize=2` → 第 1 页 2 条、第 2 页 1 条 |
| A4 | 详情 | `GET /nursing/plan/133` → data.planName=1号护理计划、sortNo=1、status=1、`projectPlans` **2 条**（projectId 为 **字符串** "1","2"，executeTime "19:16:49"/"19:28:34"，executeCycle "1"/"1"，executeFrequency 7/1）；134/135 分别 1/3 条 |
| A5 | 新增（主+子事务） | POST `{planName:"ZZT5b计划",status:"1",sortNo:2,projectPlans:[{projectId:"1",executeTime:"08:00:00",executeCycle:"0",executeFrequency:2},{projectId:"5",executeTime:"09:30:00",executeCycle:"1",executeFrequency:1}]}` → 200；列表 total=4；新 id=**实际自增（当前 141，不硬编码）**；DB `nursing_plan` 1 条 + `nursing_project_plan` 该 plan_id **2 条**；`create_time` 有值；**create_by 可为 NULL（R2 决策，不要求有值）** |
| A6 | 修改（整表单，必须用新增计划） | **先按 A5 新增（count=0）** → GET 新计划得到 id → PUT 改名/改 sortNo/子表改成 1 条 → 200；主表更新；子表**旧 2 条删除、新 1 条插入**（无残留）；详情 projectPlans 1 条且 projectId 为字符串 |
| A7 | **启停部分更新（关键）** | PUT `{id:<A5新id>,status:0}` → 200；详情 status=0，**planName/sortNo 未变、子表 projectPlans 条数未变**；再 PUT `{id,status:1}` 恢复 |
| A8 | 被引用校验（种子） | 对种子计划 133（count=1）PUT 整表单或 DELETE → 返回友好错误“该护理计划已被护理等级引用，无法修改或删除”，**数据不变**；对 133 PUT `{id:133,status:0}` → 200（启停允许）。**注意：133/134/135 全部 count=1，不能只点种子行验修改/删除** |
| A9 | 删除（主+子事务，必须用新增计划） | **先按 A5 新增一条 count=0 的计划**，DELETE 该新 id → 200；列表 total 回 3；DB 主表 0 残留、子表该 plan_id 0 残留；再 DELETE 不存在 id → 友好错误。**禁止用种子行验证删除（会被引用校验拒绝）** |
| A10 | 名称唯一 | POST 重名“1号护理计划” → 友好错误（非 500 堆栈）、无新增；PUT 新计划改名为已存在名 → 友好错误、原值不变 |
| A11 | 子表校验/回滚 | POST 子表 `projectId:""` 或 `executeTime:""` → 友好错误；**DB 主表无新增**（事务/校验前置）；同一计划重复 projectId → 友好错误 |
| A12 | `/all` | GET `/nursing/plan/all` → `code=200, data` 为 **List<NursingPlan> 实体数组、length=3（含禁用）**，每项含 `id/planName`，按 sort_no,id；护理等级页（如可打开）下拉可显示 3 项（**不是 {label,value} 结构**） |
| A13 | 前端页面联动 | 打开 `/nursing/plan`：列表 3 条、count=1 行“修改/删除”灰置；新增弹窗可建新计划；查看模式只读（输入/子表禁用、无底部按钮）；启停后列表刷新；**前端代码未改** |
| A14 | 权限 | admin 正向全通（负向 403 本轮 N/A，同 Task-S5-01 口径） |
| A15 | 事务一致性（可选加强） | 构造子表插入失败（如 executeTime 超长/为空绕过前端）→ 主表无脏数据；修复输入后重试成功 |

---

## 6. 风险 / 观察项 / 需决策

| # | 类别 | 内容 | 处置 |
|---|---|---|---|
| R1 | 数据 | 3 个现有计划**全部 count=1**（被护理等级引用）→ 前端列表“修改/删除”全部灰置；**本 Sprint 无法对种子计划做修改/删除验收**，必须“新增 count=0 计划→改→删”验证（A5→A6/A9） | 验收按 A5–A9；**tester 不得只点种子行** |
| R2 | 类型（**lead 已决策**） | `create_by/update_by` DB=**bigint**，BaseEntity=String | **不改表**；主表 insert/update 走 Mapper XML 自定义 SQL，绕开 MyMetaObjectHandler；create_by/update_by 不赋值（NULL）或显式数字；createTime/updateTime 在 Service 显式设置；严格模式报错时优先 SQL 不写这两列；验收不要求 create_by 有值 |
| R3 | 一致性 | 子表 `plan_id` 无索引；`nursing_level.plan_id` 亦无外键 | 数据量小可接受；**不改表**，仅记录 |
| R4 | 观察项 O-1（lead 已确认） | 状态字典 启用=1/禁用=2，但种子 134 status=0（前端标签仍显示禁用；编辑时 radio 无匹配项） | **本模块 status 不限制取值**（后端接受 0/1/2，不因非 1 报错）；**tester 不得判为缺陷** |
| R5 | 观察项 O-2 | 前端“查看”复用同一个 dialog 且标题写成“修改护理计划”（既有文案 bug）；`count>0` 的灰置只靠 CSS，点击仍触发请求 | 后端必须做 count 强制校验（§4.4）；前端 bug 不在本轮修，记录 |
| R6 | 缓存常量 | `CacheConstants.NURSING_PLAN_ALL_KEY` 已预留但全库无使用 | 本期不实现缓存，不强制 |
| R7 | 审计字段 | 主表 XML insert 需显式写 `create_time`（DB NOT NULL 无默认），update 显式写 `update_time`；子表 `batchInsert` 用 `sysdate()` | A5/A6 检查时间字段有值 |
| — | **lead 决策记录（2026-11-25）** | ①R2：不允许改表，XML 自定义 SQL + 不写 create_by/update_by（详见 §4.8）；②`/all` 返回全量 3 条 `List<NursingPlan>`（§3.2 #6）；③count 强制校验 + 测试必须用新增计划（§4.4/§5 A8–A9）；④status 不限制取值（§4.9） | **已确认，无需再决策** |

---

## 7. 角色分工与时间盒

| 角色 | 动作 | 时间盒 |
|---|---|---|
| web-dev1 | 实现 4 个新文件 + XML 校对（resultMap count/list 子查询+排序）；自测；编译前/后向 web-control 轻量报备 | 60~90 分钟 |
| web-arch | 收到完成通知后 **10 分钟内** review（§8）给 PASS/打回并上报 lead | ≤10 分钟 |
| web-tester | PASS 后按 A1–A15 验收（需 DQ 申请 8080 稳定窗口+浏览器+MySQL 写；基线 plan=3/子表=6，测试后恢复并回填实际影响行数） | 20~30 分钟 |
| web-control | 编译轻量报备、联调/写库窗口 FIFO 审批与核查 | 全程 |

---

## 8. web-arch Review 清单

- [ ] 仅约定文件变更；前端/DB/其它模块零改动
- [ ] 4 个新文件包名/路径正确；Mapper namespace 与 XML 一致；XML 声明的方法（含 insert/update/delete）接口齐全
- [ ] Controller 6 接口路径/方法/参数与 §3.2 一致；5 权限点；写操作 `@Log`；list `startPage()+getDataTable`
- [ ] XML：resultMap 含 count；list 含 count 子查询 + `order by sort_no asc, id asc`；原 SQL 保留
- [ ] **R2：主表 insert/update 走 Mapper XML 自定义 SQL**（未用 MP `save/updateById` 触发自动填充）；createBy/updateBy 未赋值；**createTime 显式设置**（新增）、updateTime 显式设置（更新）；未 ALTER 表
- [ ] 详情返回 Vo + projectPlans（projectId/executeCycle 为字符串）
- [ ] 新增/修改/删除 `@Transactional`；主+子表原子性；子表重建无残留
- [ ] 启停仅 id+status 时只更新 status、不覆盖其它字段、不重建子表（XML update 仅传 id/status/updateTime）
- [ ] 名称唯一（排除自身）+ DuplicateKeyException 兜底；count>0 时整表单修改/删除被拒、启停放行
- [ ] 子表必填校验 + projectId 去重 + 友好错误；失败无脏主表
- [ ] **`/all` 返回 `List<NursingPlan>` 全量 3 条（含禁用）按 sort_no,id**（不是 Vo、不是 label/value）
- [ ] **status 不限制取值**（接受 0/1/2；不得因非 1 拒绝）
- [ ] 无新增依赖、无全表更新/删除、无 SQL 拼接

---

## 9. 变更记录

| 版本 | 日期 | 变更 | 作者 |
|---|---|---|---|
| v0.1 | 2026-11-25 | 建立 Task-S5b-01 任务单：核查结论（表/索引/引用/权限/XML/类型）、6 接口契约、事务与校验规则、A1–A15 验收、风险与 review 清单 | web-arch |
| v0.2 | 2026-11-25 | 按 lead 决策更新：①R2 不改表——主表 insert/update 走 Mapper XML 自定义 SQL、不写 create_by/update_by、createTime 显式设置；②`/all` 返回全量 3 条 `List<NursingPlan>`（非 Vo/label-value）；③count 强制校验维持，验收明确“必须新增 count=0 计划再验改/删”；④status 不限制取值（字典 1/2 与库 0/1 不一致不得判缺陷） | web-arch |

---

## 10. Review 结论（web-arch，2026-11-25 15:15）

**结论：✅ PASS（通过，无阻塞项）** → 允许通知 web-tester 按 A1–A15 验收。

| # | 审查项 | 结果（只读核验） |
|---|---|---|
| ① | 变更范围 | `git status/diff`：仅 `NursingPlanMapper.xml` 修改（+5/−1：resultMap 补 count、list 加 count 子查询+order by）+ 4 个新文件；前端改动 **0**；未改表/配置/其它模块；上午 Task-S5-01 已由 lead 提交（0837e45），本轮工作区只含本任务变更 |
| ② | XML 未破原 SQL | 原 `selectNursingPlanVo/selectById/insert/update/delete` 全保留；list 仅替换为带 count 子查询的 select + 补 `order by sort_no asc, id asc`；namespace 与接口一致 |
| ③ | R2 实现（lead 决策） | 主表 CRUD **全部走 Mapper XML**（`insertNursingPlan/updateNursingPlan/deleteNursingPlanById`），未使用 MP `save/updateById`（不触发 MyMetaObjectHandler）；`createTime` 新增时显式设置、`updateTime` 更新/启停时显式设置；`createBy/updateBy` 未赋值（NULL，符合 R2）；未 ALTER 表 |
| ④ | 事务原子性 | 新增/整表单修改/删除均 `@Transactional(rollbackFor=Exception.class)`；修改顺序 update 主表→删旧子表→批量插入；删除先删子表再删主表；子表校验在主表落库前（A11 无脏主表） |
| ⑤ | 启停部分更新 | `planName==null && projectPlans==null && status!=null` → `updateStatusOnly`（XML 部分更新仅 id/status/updateTime）；不做引用拦截、不重建子表；实测 A7 name/sortNo/子表条数不变 |
| ⑥ | count 引用校验 | list 子查询 `nursing_level.plan_id` 得出 count；整表单修改/删除 count>0 抛“该护理计划已被护理等级引用，无法修改或删除”；启停放行；A8 对种子 133 验证被拒、数据不变 |
| ⑦ | 唯一名/子表校验 | 名称 trim+必填+≤255+排除自身；`DuplicateKeyException` 兜底；子表非空/必填/周期 0-2/频次 1-7/计划内 projectId 去重；`ServiceException` 中文提示，A10/A11 实测通过 |
| ⑧ | 接口/权限/详情/all | 6 接口齐全；5 权限点；写操作 `@Log`；list `startPage()+getDataTable`；详情 Vo 含 projectPlans 且 projectId/executeCycle 为**字符串**（A4 实测）；delete 单 id；**/all 返回 `List<NursingPlan>` 全量 3 条（含禁用）**（A12 实测）；status 未限制取值（符合 lead 口径） |
| ⑨ | 编译/自测/数据恢复 | `mvn -DskipTests compile` BUILD SUCCESS（10 模块）；devtools 热重启自愈 1 次（14:59 竞态，16s 恢复，**PID 23192 未动**，web-control 已闭环）；自测 A2–A12 证据完整；DB 恢复 `plan=3/sub=6`、ZZT5b 残留 0、种子 checksum 与基线一致（`nursing_plan` 自增仍=141） |

**非阻塞观察（不要求本轮修改）**
- N1：整表单修改的 `mapper.updateNursingPlan` 未单独判断更新行数（进入前已 `selectById` 判存；仅并发删除竞态下影响，可接受）；
- N2：`create_by/update_by` 为 NULL 属 lead 决策的预期状态，测试不得判缺陷；
- N3：`nursing_project_plan.plan_id` 无索引（数据量小，仅记录，不改表）；
- N4：运行态当前 AUTO_INCREMENT 仍=141（自测新增/删除后基线恢复），**tester 仍以实际返回 id 为准，不硬编码**；
- N5：前端“查看”标题文案与 count 灰置仅 CSS 属既有前端行为，后端已强制 count 校验兜底。

**生效条件**：web-tester 按 A1–A15 实测通过（改/删必须用新增 count=0 计划；A13 仅 admin 正向；MySQL 写基线 3/6、测试后清理并回填）。若实测与 review 证据冲突，回流 dev1 修复后再 review。

---

## 11. 验收结论（web-tester 最终结果，2026-11-25 15:18；web-arch 抽验）

**结论：✅ A1–A15 全部 PASS（API 48/48、UI 36/36、STA 7/7，合计 91/91；P1/P2=0，无需返修）→ 可交付 lead/主人验收。**

| 项 | 证据（web-arch 抽验） |
|---|---|
| 测试报告 | `docs/test/Web_Sprint5b_护理计划_测试报告.md`（A1–A15 矩阵/证据/轮次归因） |
| API 终轮 | `artifacts/test_web_s5b/api/plan_api_results_20261125_150507.json` → **48/48 PASS**（已抽验 JSON 计数） |
| UI 终轮 | `artifacts/test_web_s5b/ui/plan_ui_results_plan_ui_20261125071520.json` → **36/36 PASS**（counts total=36/pass=36/fail=0） |
| A7 启停 | 抓包 `PUT={"id":151,"status":0}`，status 1→0 且 planName/sortNo/子表不变；恢复成功 |
| A8 被引用 | `API-UPD-08`/`API-DEL-04` → code=500 “该护理计划已被护理等级引用，无法修改或删除”，133 与子表 2 行不变；133 启停放行 1→0→1 后已恢复快照（status=1/update_time=NULL） |
| A6/A11 | 子表旧 2 删新 1 无残留；非法子表 → 友好错误且主表无新增 |
| A12 `/all` | `List<NursingPlan>` 实体数组 length=3（含禁用 134），护理等级页下拉 3 项 |
| A1 前端零改动 | git status/diff 空；`plan.js`/`plan/index.vue`/`level/index.vue` hash=HEAD；UI 抓包 37 个 `/dev-api/nursing/plan` 请求（GET/POST/PUT/DELETE）全 200 |
| 数据恢复 | `nursing_plan=3`、`nursing_project_plan=6`、ZZT5B 残留=0、孤儿=0；**CHECKSUM TABLE = 2521898468 / 2554906448，与基线及 web-control 记录一致**；种子 133 已恢复 |
| 观察项（不判缺陷） | O-1 字典 1/2 vs 库 0/1（134 radio 无选中仅记录）；O-2 查看标题/仅 CSS 灰置（后端强制校验已验证）；R2 create_by 允许 NULL；R3 子表 plan_id 无索引；R7 子表 sysdate 正常 |

**交付状态**：代码工作区变更（4 新文件 + XML）尚未 commit（等 lead 统一提交）；前端零改动；无 DB 结构变更；C++ 端无契约影响（本次为 Web 后端接口补齐，`/all` 形状已按 lead 决策固定）。
