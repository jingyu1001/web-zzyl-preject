# Web Sprint 5 · 护理项目模块 CRUD · 最终验收复核（lead）

> **验收对象**：服务管理-护理项目（`/nursing/project`）后端 6 接口 CRUD；约束：**前端页面代码零改动**、不改表结构、DB 种子保持 9 行。
> **验收人**：lead（总控）｜**日期**：2026-11-25 09:50｜**依据**：老师建表语句 + `docs/tasks/Web_Sprint5_护理项目CRUD_任务单.md`（A1–A16）+ `docs/test/Web_Sprint5_护理项目_测试报告.md`（v1.0）
> **结论：✅ 验收通过，可交付。**

---

## 1. 交付物（后端 6 项改动，前端 0 改动）

| # | 文件 | 类型 | 说明 |
|---|---|---|---|
| 1 | `zzyl-nursing-platform/.../mapper/NursingProjectMapper.java` | 新增 | MyBatis-Plus BaseMapper + list 查询 |
| 2 | `zzyl-nursing-platform/.../service/INursingProjectService.java` | 新增 | 服务接口（含名称唯一校验、all 下拉） |
| 3 | `zzyl-nursing-platform/.../service/impl/NursingProjectServiceImpl.java` | 新增 | 实现（唯一名预校验 + DuplicateKey 兜底、status 默认 1、部分更新） |
| 4 | `zzyl-nursing-platform/.../controller/NursingProjectController.java` | 新增 | 6 接口（list / {id} / POST / PUT / DELETE / all）+ 5 权限点 + `@Log` |
| 5 | `zzyl-nursing-platform/.../resources/mapper/nursing/NursingProjectMapper.xml` | 修改 | 复用老师既有 XML，仅补 `order by order_no asc, id asc`（+1 行） |
| 6 | `zzyl-admin/.../common/CommonController.java` | 修改 | `/common/upload` 保留 OSS 优先 + 未配置/失败时本地兜底（`/profile/upload/...`），响应四字段不变；`/common/uploads` 未动 |

- 前端 `code/zzyl/zznursing-vue3-master/`：`git status` **无任何改动**（A1 满足）。
- 未改库结构/配置/依赖；数据库最终 **COUNT=9、MAX(id)=9**（与测试前基线一致）。

## 2. lead 独立只读复核（09:49）

登录：`GET /captchaImage` → Redis 只读取验证码 → `POST /login`（admin，code=200，token 203 字符）。

| 检查项 | 结果 |
|---|---|
| `GET /nursing/project/list?pageNum=1&pageSize=10` | code=200，**total=9、rows=9**，顺序 id=1..9（order_no,id）✅ |
| `GET /nursing/project/list?name=洗` | **total=3**（洗头/洗脸/洗脚）✅ |
| `GET /nursing/project/list?status=0` | **total=1**（id=6 洗头，禁用）✅ |
| `GET /nursing/project/1` | data：id=1、价格 30.00、orderNo=1，字段完整 ✅ |
| `GET /nursing/project/all` | data=9 条 `{label,value}`，value 为字符串，首条「修剪指甲/1」、末条「心理咨询/9」✅ |
| 前端 88 / 后端 8080 | HTTP **200 / 200**，PID 35760 / 23192 全程未动 ✅ |
| 前端源码零改动 | `git status -- code/zzyl/zznursing-vue3-master` = **NONE** ✅ |
| DB 完整性 | `COUNT=9`、种子行未被修改（测试结束清理完成）✅ |

> 说明：复核为只读操作（1 次登录 + 5 个 GET），未产生测试数据、未改业务表。

## 3. web-tester 终版测试结果（v1.0）

| 层 | 用例 | PASS | FAIL |
|---|---|---|---|
| 静态核查 STA | 7 | 7 | 0 |
| API 功能 | 47 | 47 | 0 |
| UI 功能（浏览器实机） | 49 | 49 | 0 |
| 联动（plan 页 `/all` 下拉） | 3 | 3 | 0 |
| 回归（房型/用户/菜单/既有 API/健康） | 6 | 6 | 0 |
| 非功能 | 7 | 6 | 0（A13 负向权限 N/A，lead 决策） |
| **合计** | **119** | **118** | **0** |

- **P1/P2/P3 产品缺陷 = 0**；唯一 P3 观察 O-01（前端新增弹窗状态 radio 无默认选中，前端不改约束内；后端默认 status=1 正确）。
- 首轮 2 个 UI FAIL 经 web-arch triage 判定为**判定口径问题**（REG-06 渲染启发式阈值误判；UI-GUARD-02 误把 Vite 源码模块请求当 API），修正复测 49/49 PASS，非产品缺陷。
- 证据根目录：`artifacts/test_web_s5/`（static/api/ui/db/logs，60 张实机截图 + 47 条 API 结果 + 隔离证据）。

## 4. 遗留项处置

| 项 | 状态 | 结论 |
|---|---|---|
| A13 权限负向（受限账号） | N/A | lead 决策：本轮仅验 admin 正向（`*:*:*`）+ 未认证写接口 body code=401 且不落库；不建测试账号、不改库 |
| 批量删除 | 不做验收项 | 前端无批量入口；按任务单只验单 id，`DELETE /{ids}` 契约外仅记录 |
| O-01 前端状态 radio 默认值 | P3 观察 | 前端零改动约束下不修；后端默认 status=1 功能正确；Sprint 候选（前端优化） |
| O-02 测试脚本取值缺陷 | 已修复 | 验证码/种子哈希取值已单语句复核 + web-control 独立 MD5 交叉验证 |
| `/nursing/plan`、`/nursing/level` 接口 404 | 既有问题（非本 Sprint） | 页面壳正常，缺自身 Controller；记入 Sprint 候选池（C-01/C-02），本次仅验收 `/nursing/project/all` 恢复 |
| OSS 上传通道未配置 | 已接受差异 D-Web-S5-01 | 本机无 OSS 凭证；`/common/upload` 本地兜底实测可用（浏览器可直接显示），OSS 配置保留优先分支，后续配好凭证即自动走 OSS |
| `sys_logininfor` / `sys_oper_log` 新增 | 正常应用行为 | 登录/操作日志自动记录，属预期，不计违规 |

## 5. 调度与数据安全（web-control）

- DQ-20261125-0001（dev1 MySQL 自测）：09:04–09:05 提前释放；核验 COUNT=9、种子 MD5 一致、0 残留。
- DQ-20261125-0002（tester API W1）：09:08–09:13 提前释放，47/47 PASS，核验数据恢复基线。
- DQ-20261125-0003（tester UI W2）：09:14–09:25 提前释放，49/49 PASS，浏览器残留 0、8080/88 PID 未变。
- 违规记录 V-01（09:25:38 IDEA 增量编译未报备 → devtools 热重启 9s，发生在 tester 释放之后、窗口外无影响；来源未确认，已留档）。
- 全程未停止/重启 88（PID 35760）/8080（PID 23192）/MySQL/Redis。

## 6. 验收结论

**✅ 通过（可交付）**：
1. 老师要求的 6 个 CRUD 接口全部实现并与前端契约 1:1 匹配，前端零改动；
2. API 47/47 + UI 49/49 + 联动/回归全 PASS，P1/P2 缺陷 0；
3. 数据安全：测试数据全部清理，种子 9 行/哈希一致，服务未重启；
4. 已知限制（OSS 未配置走本地兜底、plan/level 缺 Controller、前端 radio 默认值）均为非阻塞项并已归档。

**后续建议**：Sprint 6 优先补 `NursingPlan` / `NursingLevel` Controller（候选池 C-01/C-02）；如需 OSS 外网图片或前端体验微调，另行立项。

---

**签署**：lead（总控）· 2026-11-25 09:50
**关联文档**：`docs/test/Web_Sprint5_护理项目_测试报告.md`（tester v1.0）、`docs/test/Web_Sprint5_护理项目_测试计划.md`（v1.2）、`docs/tasks/Web_Sprint5_护理项目CRUD_任务单.md`（§10 Review / §11 Triage）、`docs/test/web_调试调度日志.md`（v1.9）
