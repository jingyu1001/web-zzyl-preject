# Web Sprint 5b · 护理计划模块 CRUD · 最终验收复核（lead）

> **验收对象**：服务管理-护理计划（`/nursing/plan`）后端 6 接口（`nursing_plan` + `nursing_project_plan` 主从表 CRUD）；约束：**前端页面代码零改动**、不改表结构、种子数据保持 `nursing_plan=3` / `nursing_project_plan=6`。
> **验收人**：lead（总控）｜**日期**：2026-11-25 15:20｜**依据**：老师两次建表语句 + `docs/tasks/Web_Sprint5b_护理计划CRUD_任务单.md`（v0.2，A1–A15）+ `docs/test/Web_Sprint5b_护理计划_测试报告.md`（v1.0）
> **结论：✅ 验收通过，可交付。**

---

## 1. 交付物（后端 5 项改动，前端 0 改动）

| # | 文件 | 类型 | 说明 |
|---|---|---|---|
| 1 | `zzyl-nursing-platform/.../mapper/NursingPlanMapper.java` | 新增 | 对齐既有 XML 的 Mapper 接口 |
| 2 | `zzyl-nursing-platform/.../service/INursingPlanService.java` | 新增 | 服务接口 |
| 3 | `zzyl-nursing-platform/.../service/impl/NursingPlanServiceImpl.java` | 新增 | 实现：主从同事务、唯一名、count 引用校验、启停部分更新、`/all` |
| 4 | `zzyl-nursing-platform/.../controller/NursingPlanController.java` | 新增 | 6 接口 + 5 权限点 + 写操作日志注解 |
| 5 | `zzyl-nursing-platform/.../resources/mapper/nursing/NursingPlanMapper.xml` | 修改 | resultMap 补 `count`；list 加 count 子查询（`nursing_level.plan_id`）+ `order by sort_no asc, id asc`；原 SQL 全保留 |

- 前端 `code/zzyl/zznursing-vue3-master/`：`git status` **无任何改动**（A1 满足）。
- 未改库结构/配置/依赖；数据库最终 **3 / 6 行**，`ZZT5B*` 残留 0。

## 2. lead 独立只读复核（15:18）

登录：`GET /captchaImage` → Redis 只读取验证码 → `POST /login`（admin，code=200）。

| 检查项 | 结果 |
|---|---|
| `GET /nursing/plan/list?pageNum=1&pageSize=10` | code=200，**total=3、rows=3**，id 133/134/135，status 1/0/1，sort_no=1，**每行 count=1** ✅ |
| `?planName=1号` | **total=1** ✅ |
| `?status=1` / `?status=0` | **2 条 / 1 条** ✅ |
| `GET /nursing/plan/133` | data 含 `projectPlans` **2 条**；projectId=1（字符串）、executeCycle=1、executeFrequency=7 ✅ |
| `GET /nursing/plan/all` | data=**3 条 NursingPlan 实体**（ids 133/134/135，含禁用 134），非 label/value ✅ |
| 前端源码零改动 | `git status -- code/zzyl/zznursing-vue3-master` = **NONE** ✅ |
| DB 完整性 | `nursing_plan=3`、`nursing_project_plan=6`、`ZZT5B%`=0 ✅ |
| 服务 | 8080 / 88 全程运行，PID 23192 / 35760 未动 ✅ |

> 复核为只读（1 次登录 + 6 个 GET），未产生业务测试数据。

## 3. web-tester 终版测试结果（v1.0）

| 层 | 用例 | PASS | FAIL |
|---|---|---|---|
| 静态核查 STA | 7 | 7 | 0 |
| API 功能 | 48 | 48 | 0 |
| UI 功能（浏览器实机，含联动/回归/前端零改动护栏） | 36 | 36 | 0 |
| **合计** | **91** | **91** | **0** |

- **P1/P2 缺陷 = 0**，无需返修；首轮 W1 35/48、W2 22/36 的失败均判定为**测试脚本适配问题**（PS 类型断言、时间选择器/弹窗级联/只读选择器定位），修正脚本后终轮全过（报告附录 A 留痕）。
- 核心闭环：列表/搜索/分页、详情子表、**新增主+子事务**、名称唯一、**子表校验回滚无脏主表**、整表单修改子表 2→1 替换无残留、**启停仅 `{id,status}` 其它字段/子表不变**、删除级联子表、**A8 被引用强制校验**、`/all` 实体数组。
- 数据保护：种子 133 整表单改/删被友好拒绝；133 启停 1→0→1 放行后按快照恢复 `status=1、update_time=NULL`；两表 **CHECKSUM TABLE 全程与基线一致**（2521898468 / 2554906448）。
- 证据根目录：`artifacts/test_web_s5b/`（api 18 文件 + ui 51 张截图 + static/db/logs）。

## 4. 遗留项处置（非阻塞）

| 项 | 状态 | 结论 |
|---|---|---|
| O-1 状态字典（1/2）与库（0/1）不一致 | 已归档 | 后端不限制取值；tester 不判缺陷；后续前端/字典对齐项 |
| O-2 查看标题复用「修改」、count 灰置仅 CSS | 已归档 | 前端既有行为（前端零改动约束内不修）；后端已强制 count 校验兜底 |
| R3 子表 `plan_id` 无索引/外键 | 已归档 | 教师建表原始设计；本轮不改表；Sprint 6 可选优化 |
| create_by/update_by 允许 NULL | 已接受差异（lead 决策） | bigint 与 BaseEntity String 类型差；主表走 XML 自定义 SQL、不写这两列、不改表 |
| `/nursing/level` 后端 Controller 仍缺 | 既有问题 | 不在本轮范围；已列 Sprint 6 候选 C-01 |

## 5. 验收结论

**✅ 通过（可交付）**：
1. 老师要求的护理计划主从表 CRUD 6 接口全部实现，与前端契约 1:1 匹配，前端零改动；
2. API 48/48 + UI 36/36 + 静态 7/7 全 PASS，P1/P2 缺陷 0；
3. 数据安全：测试数据全部清理、种子校验和与基线一致、8080/88 服务未重启；
4. 已知限制均为非阻塞项并已归档。

**后续建议**：Sprint 6 优先补 `NursingLevel` / `NursingTask` 等缺失 Controller（候选 C-01/C-03）；前端状态字典对齐、子表索引优化可随 Sprint 6 一并处理。

---

**签署**：lead（总控）· 2026-11-25 15:20
**关联文档**：`docs/test/Web_Sprint5b_护理计划_测试报告.md`（tester v1.0）、`docs/test/Web_Sprint5b_护理计划_测试计划.md`（v1.2）、`docs/tasks/Web_Sprint5b_护理计划CRUD_任务单.md`（§10 Review）、`docs/test/web_调试调度日志.md`（v1.15）
