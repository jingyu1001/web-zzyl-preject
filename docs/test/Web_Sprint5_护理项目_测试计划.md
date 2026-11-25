# Web Sprint 5 · 护理项目模块 CRUD · 测试计划 v1.2

> **编写**：web-tester（Web 测试）｜**日期**：2026-11-25｜**状态**：待执行（等 web-dev1 完成 + web-arch review 通过；测试工具已就绪）
> **被测对象**：服务管理-护理项目（`/nursing/project`）**后端 6 接口** + 现有前端页面（**前端源码零改动**）
> **权威依据**：`docs/tasks/Web_Sprint5_护理项目CRUD_任务单.md`（web-arch，契约/验收 A1–A16）
> **环境纪律**：`docs/tasks/Web调试调度规则.md` v1.1（web-control）｜调度日志：`docs/test/web_调试调度日志.md`
> **交付**：测试报告 `docs/test/Web_Sprint5_护理项目_测试报告.md`｜证据 `web-zzyl-preject/artifacts/test_web_s5/`｜脚本 `artifacts/tools/browser/`
> **不 commit、不改前端/后端源码、不建表/改表、不重启主人服务（88/3306/6379，8080 主人 IDEA 实例未经 lead 批准不动）**
>
> **决策更新（v1.1，lead 2026-11-25 确认）**
> 1. **R1 上传口径 = 后端本地兜底**：web-dev1 修改 `CommonController./common/upload`——OSS 上传失败时落本地 `C:/ruoyi/uploadPath` 并返回 `http://localhost:8080/profile/upload/...`；A15 验收改为「**能上传 → url 非空 → 图片显示**」，不需要 OSS 凭证；
> 2. **A13 负向权限本轮 N/A**：不建测试账号、不改库；仅验 admin 账号按钮可见 + 6 接口可用；
> 3. **批量删除**：UI 只验单删；API 层可选验证（RuoYi 惯例 `DELETE /{ids}` 数组），**契约外、非阻塞**；
> 4. 文档偏差 D1（`name=洗`→3 条）/D2（id 动态不硬编码）/D3（表头 10 列）已由 lead 转 arch 修正；
> 5. **A16 列表/弹窗图片显示**（9 条历史 OSS URL 与本地兜底 URL 共存）纳入验收；
> 6. 执行期补充：RuoYi 未认证响应为 **HTTP 200 + body code=401**；PageHelper `reasonable=true` 超界页返回末页数据（均不判缺陷）。

---

## 1. 测试目标

1. 验证 web-dev1 新增的 6 个后端接口与任务单 §3 契约一致，且能支撑现有前端页面（**零改动**）完成：列表/搜索/分页/详情/新增/修改/**启停部分更新**/删除/下拉。
2. 验证前端既有行为（列表 9 行、10 列表头、表单校验、图片上传、确认框、状态标签）在真实后端下可用。
3. 验证 `/nursing/project/all` 上线后，护理计划页（`/nursing/plan`）的“护理项目”下拉恢复可用（该页其它自身接口仍缺，**只验下拉**）。
4. 回归：房型设置、用户管理、既有菜单与关键 API 不受新 Controller 影响（无路径/Bean 冲突、服务健康）。
5. 产出：用例执行统计、**缺陷清单（含复现步骤/严重级）**、结论与建议。

**通过口径（对齐任务单 §5）**：A1–A16 全过（A13 负向 N/A；A15/A16 按本地兜底口径）；**无 P1/P2 未修复缺陷**；P3/P4 与文档偏差记录不阻塞。

---

## 2. 范围

| 在范围 | 不在范围（Out of Scope） |
|---|---|
| `GET /nursing/project/list`、`GET /{id}`、`POST`、`PUT`、`DELETE /{id}`、`GET /all` | 护理计划/护理等级/预约/任务等其它模块后端（仍 404/500，属既有已知状态） |
| 前端 `/nursing/project` 全交互（搜索/新增/修改/删除/启停/分页/图片列） | 前端任何代码修改（**禁止**；发现 UI 问题只登记不修改） |
| 联动：`/nursing/plan` 新增弹窗“护理项目”下拉 | plan 页列表/提交（其接口缺失，已知 R4） |
| 回归：房型/用户/菜单/关键既有 API、8080 服务健康 | 导出 Excel（页面无按钮，任务单明确不做）、C++ 端 |
| 权限（admin 全通 / 无权限角色 403 / 未登录 401）、审计日志、安全健壮性 | 性能压测、并发、OSS 真实上传功能本体（环境 R1，见 §4.3） |

---

## 3. 测试依据、契约摘要与口径

### 3.1 依据优先级（冲突时）

1. **任务单 `Web_Sprint5_护理项目CRUD_任务单.md` §3/§5**（架构契约与验收标准，权威）；
2. **前端源码实际调用**（`src/api/nursing/project.js`、`views/nursing/project/index.vue`、`views/nursing/plan/index.vue`，只读核对）——决定“页面可观测行为”的预期；
3. **DB 实测数据**（只读查询，2026-11-25 09:0x 基线）——决定列表/搜索/下拉的具体预期值；
4. lead 本次补充要求（API/UI/联动/回归四层）——若与任务单冲突（如“批量删除”），以任务单为准并单列“契约外观察项”。

### 3.2 接口契约摘要（任务单 §3.2，6 个接口）

| # | 方法/路径 | 入参 | 出参 | 权限点 |
|---|---|---|---|---|
| 1 | `GET /nursing/project/list` | query `pageNum,pageSize,name,status` | `TableDataInfo{code,msg,total,rows[]}`；排序 `order by order_no,id`；name 模糊、status 精确 | `nursing:project:list` |
| 2 | `GET /nursing/project/{id}` | path id | `AjaxResult{code,msg,data}`；不存在 data=null | `nursing:project:query` |
| 3 | `POST /nursing/project` | JSON `NursingProject` | `AjaxResult{code:200,msg}` | `nursing:project:add` |
| 4 | `PUT /nursing/project` | JSON（**可能只含 `{id,status}`**） | `AjaxResult`；部分更新，null 不覆盖；id 不存在返回 error | `nursing:project:edit` |
| 5 | `DELETE /nursing/project/{id}` | path **单 id**（非数组） | `AjaxResult`；不存在返回 error | `nursing:project:remove` |
| 6 | `GET /nursing/project/all` | 无 | `AjaxResult{code:200,data:[{label:String,value:String}]}`，排序 `order_no,id`，**全量 9 条含禁用** | `nursing:project:list`（或 query） |

**业务规则（任务单 §3.3/§3.4）**：
- 启停：前端只传 `{id,status}` → **null 字段不得覆盖**（unit/price/image 等必须保留）；
- 唯一名：新增/修改（修改需排除自身 id）→ 重名返回 `code=500,msg="护理项目名称已存在"`（**JSON 友好错误，不得抛 SQL 堆栈**）；`DuplicateKeyException` 兜底；
- `name` trim 后非空；建议 >100 字符返回“名称长度不能超过100”；
- 新增未带 `status` → 默认 1；`image=""` 归一 null；审计字段由 `MyMetaObjectHandler` 填充。

### 3.3 前端可观测行为基线（源码只读核对结论）

| 项 | 源码事实（预期） |
|---|---|
| 搜索栏 | 名称输入（可回车）、状态下拉（字典 `nursing_project_status`：1=启用/0=禁用，可清空）、搜索/重置 |
| 表头 | **9 个数据列**：编号(index,120)/名称(150)/排序号(100)/单位(100)/价格(150)/图片(150,缩略图50×50)/护理要求(auto)/状态(120, tag 启用绿/禁用红)/创建时间(180, `yyyy-MM-dd HH:mm:ss`) + **操作列(200)** = **共 10 列** |
| 工具栏 | 仅“新增”（无批量删除/修改/删除按钮 → 前端无批量入口） |
| 新增/修改弹窗 | 500px；7 字段：名称*/排序号 number(1~20)/单位/价格* number(5~100,step5)/状态 radio(字典)/图片* 上传(148×148)/护理要求* textarea；按钮 确定/取消 |
| 前端表单校验（rules） | **name、price、image、nursingRequirement 必填**；orderNo/unit/status 非必填（status 未选 → 提交 null） |
| 修改回填 | `getProject(id)` → `response.data` 全字段回填；status 转字符串供 radio 回填 |
| 删除 | 确认框 `是否确认删除护理项目编号为"<id>"的数据项？` → DELETE → 刷新 |
| 启停 | `updateProject({id, status: row.status===0?1:0})`；确认框 `是否确认启用/禁用这条护理项目？` |
| 分页 | 默认 pageNum=1/pageSize=10；`pagination` 组件“共 N 条” |
| 图片上传 | `image-upload` → `POST {VITE_APP_BASE_API}/common/upload`（= `/dev-api/common/upload`→8080）；成功取 `response.url` 回填 |
| 护理计划联动 | `plan/index.vue` `onMounted → getAllProjects() → projectOptions = res.data`；`el-option :label=item.label :value=item.value`（value 为字符串 id） |

### 3.4 准备期发现的文档/口径偏差（已报 lead，测试按实测执行并记录）

| # | 文档写法 | 实测/源码事实 | 测试处理 |
|---|---|---|---|
| D1 | 任务单 A4：`name=洗` → 1 条（洗头） | 种子数据 **洗头/洗脸/洗脚 3 条**含“洗”，预期 **3 条**（id 6,7,8） | 按 3 条验收；在报告“文档偏差”登记，建议 arch 修正 |
| D2 | 任务单 §2/§5 示例“新记录 id=11” | 09:0x 只读实测 `AUTO_INCREMENT=10`（9 行，下一条 id=10）；测试写库后还会前移 | **不硬编码 id**：测试前只读确认，动态断言 |
| D3 | lead 口述“表格 9 列” | 前端 9 个数据列 + 操作列 = 表头 10 列 | 按 9 数据列/10 列表头记录（§3.3） |
| D4 | lead 要求 API“delete 单删/**批量**” | 任务单 §3.2 明确“**单 id，不是数组**”；前端无批量入口 | 单删为验收项；批量删除列 **契约外观察项**（API-DEL-04，不判缺陷，仅记录实际行为） |

### 3.5 种子数据基线（DB 只读实测，2026-11-25 09:0x）

| id | name | unit | price | status | order_no |
|---|---|---|---|---|---|
| 1 | 修剪指甲 | 次 | 30.00 | 1 启用 | 1 |
| 2 | 衣物清洁 | 件 | 5.00 | 1 | 1 |
| 3 | 整理床铺 | 次 | 15.00 | 1 | 1 |
| 4 | 助餐 | 餐 | 15.00 | 1 | 1 |
| 5 | 助浴 | 次 | 40.00 | 1 | 1 |
| 6 | 洗头 | 次 | 20.00 | **0 禁用** | 1 |
| 7 | 洗脸 | 次 | 15.00 | 1 | 1 |
| 8 | 洗脚 | 次 | 20.00 | 1 | 1 |
| 9 | 心理咨询 | 小时 | 80.00 | 1 | 1 |

- 全部 `nursing_requirement=无`；`image` 均为 `https://zzyl-nursingproject-bucket.oss-cn-beijing.aliyuncs.com/images/2026/11/21/*.png`（OSS 绝对地址）；`create_time` 2024-08-29。
- 表结构要点：`name varchar(100) UNIQUE`（重名触发唯一约束）、`status int NOT NULL DEFAULT 1`、`create_time NOT NULL DEFAULT CURRENT_TIMESTAMP`、其余字段可空。
- 基线快照（`SELECT` 结果/行数）在开测前保存到 `artifacts/test_web_s5/db/baseline.json`，收测后比对，确保 **9 条种子数据零改动**。

---

## 4. 环境、工具与前置

### 4.1 共享环境基线（2026-11-25 08:45 核查，默认不动）

| 资源 | 地址/进程 | 纪律 |
|---|---|---|
| 前端 88 | Vite dev `node.exe` PID 35760 | 常态运行；前端不改，**无需重启**；只读访问页面 |
| 后端 8080 | Spring Boot（IDEA 启动）`java.exe` PID 23192，devtools 开启 | 默认不动；**不得停/重启该主人 IDEA 实例（除非 lead 批准）**；联调需稳定窗口正式申请 |
| MySQL 3306 | `mysqld.exe` PID 16152，库 `zzyl` | **写操作（含经接口产生的增删改）必须申请**；只读查询免申请 |
| Redis 6379 | `redis-server.exe` PID 35808 | 默认不动（本计划不读写；仅 §4.2 备选方案如启用需 lead 认可） |
| 浏览器自动化 | 无头 Edge/Chrome（本机 Edge/Chrome 已装） | **正式申请窗口，同刻仅 1 人，≤15 分钟** |
| 其他 | 数据库账号 `root/123456`（本地配置只读使用）；管理员 `admin`；验证码开关 `sys.account.captchaEnabled=true`（DB 只读实测） | 不改配置、不改库 |

### 4.2 账号与验证码策略（关键前置）

- **管理员**：`admin`（DB 中 `sys_user.status=0` 正常）；密码按 RuoYi 默认 `admin123` 假设尝试，**失败即报 lead 请提供测试账号，不猜测/不改库**。
- **无权限账号（A13 负向）**：**本轮 N/A**（lead 决策：不建测试账号、不改库）；仅验 admin 正向（按钮可见 + 6 接口可用 + `@PreAuthorize` 代码评审）。
- **验证码（captchaEnabled=true，不关闭、不改库）**：
  1. **首选（无侵入）**：`GET /captchaImage`（或 Playwright 拦截登录页该响应）拿到 `uuid + base64 图片` → 落盘 PNG → 我用视觉读取算术题 → 计算答案 → 登录脚本读取答案文件后提交 `POST /login {username,password,code,uuid}` 或 UI 填写登录；
  2. 备选：只读查询 Redis `captcha_codes:{uuid}`（不写不删，须 lead 认可只读边界）；
  3. 禁止：改 `sys_config` 关闭验证码（写库+配置变更，需正式申请且超出本期范围）。
- 登录会写 `sys_logininfor`/`sys_oper_log`（应用自动行为，随窗口申报说明，不清理）。

### 4.3 环境风险（预先登记，不擅自处置）

| # | 风险 | 影响 | 计划内处理 |
|---|---|---|---|
| R1 | ~~OSS 上传环境变量未配置~~ → **lead 决策：后端本地兜底**（dev1 改 `CommonController`：OSS 失败落 `C:/ruoyi/uploadPath`，返回 `http://localhost:8080/profile/upload/...`；框架 `ResourcesConfig` 已映射 `/profile/**`，`RuoYiConfig.profile=C:/ruoyi/uploadPath`） | UI“图片上传”验收项 | A15 按「**能上传 → url 非空 → 图片显示**」验收；`GET url` 应 200 且为图片；若仍失败记 **ENV-R1**（非代码缺陷）并报 lead；**不再需要 OSS 凭证**（OSS 真实外网通道不验） |
| R2 | devtools：任何后端编译触发 8080 热重启（3–15s） | 测试中 8080 抖动 | 窗口内我**不编译**；进窗口先做健康检查；发现 8080 为新代码未加载 → 报 lead（主人实例重启需批准）后再重试 |
| R3 | 验证码 | 登录 | §4.2 视觉解码；两次失败即停，报 lead |
| R4 | plan 页自身接口仍 404/500 | 联动页有错误 toast | 只验下拉，其它错误记录为“已知/范围外” |
| R5 | 测试写库污染 | 数据/自增 | 只写 `ZZT5-*` 测试行，**结束时 API 删除回 9 条**；种子行只读校验 md5 不变；实际影响行数回填释放消息 |
| R6 | 浏览器/端口争用 | 被他人占用 | 只走 web-control 正式申请，FIFO；不硬抢、不按名杀进程 |

### 4.4 工具与证据

| 类别 | 工具 | 说明 |
|---|---|---|
| API 测试 | **PowerShell 7 `Invoke-RestMethod`/`Invoke-WebRequest`** | 免安装；请求/响应 JSON 落盘 `artifacts/.../api/`；断言用 PowerShell 脚本 |
| UI 自动化 | **Playwright（playwright-core，已向 lead 提交安装需求）** | 用系统 Edge/Chrome（`channel: 'msedge'`/`executablePath`），不下载浏览器内核；截图/控制台/网络证据 |
| UI 备选 | Node 24 内置 `fetch`+`WebSocket` 直连 CDP | 若 lead 不批安装则自研（无第三方依赖） |
| DB 校验 | `mysql.exe --host=127.0.0.1 --user=root -e "SELECT ..."` | **只读**（基线/比对/清理确认）；写库只经接口或申报后的清理 SQL |
| 证据 | `artifacts/test_web_s5/{static,api,ui,db,logs}/` | 截图 PNG、响应 JSON、git diff、控制台日志、用例结果表 |

---

## 5. 测试数据设计（写库申报口径）

| 项 | 内容 |
|---|---|
| 命名规范 | 测试行名称统一前缀 **`ZZT5-`** + 用途 + 时间戳（如 `ZZT5-新增-1105a`），保证唯一、可检索清理；长度 ≤100 |
| 新增条数 | W1 约 2–3 行（排序用 orderNo=0 的 1 行 + 普通 1 行 + UI 分页用 1 行）；**总数 ≤4** |
| 修改 | 仅对测试行（全字段 1 次、`{id,status}` 部分更新 2 次）；种子行只读 |
| 删除 | 全部测试行经 `DELETE /nursing/project/{id}` 清理；若接口异常，用申报过的清理 SQL（`DELETE FROM nursing_project WHERE name LIKE 'ZZT5-%'`）并在释放消息回报 |
| 预计影响行数 | `nursing_project`：INSERT 2–4、UPDATE 3–5、DELETE 2–4（净增 0，结束回 9 行；`AUTO_INCREMENT` 前移属正常副作用，记录实际值） |
| 自动副作用 | `sys_logininfor`（登录）、`sys_oper_log`（@Log 写操作）少量新增，应用行为，不清理 |
| 不触碰 | 9 条种子行、`sys_*` 配置、表结构、其它业务表；**不删被 `nursing_project_plan` 引用的种子项目**（数据一致性建议仅写报告，不执行破坏性验证） |

---

## 6. 执行策略与顺序（窗口内）

```
开窗前（免申请，随时）
  S0 静态核查：前端零改动/后端文件清单/权限注解/编译结果/契约一致性（STA 用例）
  S1 只读核对：8080/88 端口健康、种子基线快照、captcha/账号可用性探测（不写库）
窗口 W1（API，10 分钟；资源：后端 8080 稳定窗口 + MySQL 写）
  S2 环境健康 + 登录 token（admin/ry）→ 只读 API（list/search/page/detail/all/401/403）
  S3 写 API（add/默认 status/重名/update 全字段+部分/delete）→ 只读 DB 校验 → 清理
  S4 回归 API（房型/用户/菜单）+ 审计日志只读核对 + 上传（A15）探测 → 释放
窗口 W2（UI，15 分钟；资源：浏览器自动化 + 8080 只读 + MySQL 写）
  S5 登录（验证码视觉解码）→ 菜单/列表/搜索/分页（只读）
  S6 新增（含上传/桩）→ 修改 → 启停 → 删除 → 分页（写）
  S7 联动 plan 下拉 → 回归页面（房型/用户）→ 截图/控制台污染检查 → 释放
窗口 W3（复测，按需 ≤10 分钟）：缺陷复测/补证
收尾（免申请）：测试数据清理确认、种子 md5 比对、证据归档、报告输出、汇报 lead
```

**时间盒**：W1 10min、W2 15min、W3 按需 10min；超时前 2 分钟收到 web-control 提醒即收尾释放。

---

## 7. 用例清单

> 优先级：**P0** 冒烟/核心闭环｜**P1** 主要功能/契约｜**P2** 边界/异常｜**P3** 观察/体验（不阻塞）
> 执行方式：API=PowerShell 脚本；UI=Playwright；DB=只读 SQL；STA=git/静态检查
> “依据”列：A*=任务单验收项；L=lead 本次要求；前端=源码核对

### 7.1 静态核查（开窗前，免申请）

| 用例ID | 级别 | 步骤 | 预期结果 | 依据 |
|---|---|---|---|---|
| STA-01 | P0 | `git -C web-zzyl-preject status --porcelain -- code/zzyl/zznursing-vue3-master`；`git diff -- code/zzyl/zznursing-vue3-master`；记录 `project.js`/`index.vue` 等关键文件 SHA256 | **前端零改动**：无任何 modified/untracked/删除；哈希与 HEAD 一致 | A1 |
| STA-02 | P0 | `git status --porcelain`；`git diff --stat` 审查后端改动范围 | 仅 `zzyl-nursing-platform` 下：新增 4 个 Java 文件（Mapper/IService/ServiceImpl/Controller）+ `NursingProjectMapper.xml` 修改（含 order by）；**另：`zzyl-admin/.../CommonController.java` 修改（lead 批准的 R1 本地兜底，属本次授权范围）**；**无**其它模块/配置/前端/表结构改动 | A1/A2、任务单 §3.5、lead R1 决策 |
| STA-03 | P1 | `git diff -- .../pom.xml`；检查是否新增依赖 | 无依赖变更、无 SQL 拼接、无全表 update/delete | 任务单 §4 |
| STA-04 | P0 | 只读代码评审：Controller 6 接口路径/方法/参数；`extends BaseController`；`@PreAuthorize` 5 权限点；`@Log` 写操作；`startPage()+getDataTable`；XML `order by order_no, id`；Mapper namespace 对齐 | 与任务单 §3.2/§3.5 一致 | 任务单 §8 review 清单 |
| STA-05 | P1 | 只读核前端 `src/api/nursing/project.js` 6 个 URL 与任务单 §3.2 一致 | 一致（list/get/{id}/POST/PUT/DELETE/all） | A1 |
| STA-06 | P0 | 编译/构建状态核对（dev1 已报备 `mvn -DskipTests compile`；我**不主动触发** devtools 重启） | 编译通过；`target/classes` 含 4 个新类；8080 已加载新代码（用接口 200 佐证） | A2 |
| STA-07 | P1 | 后端启动日志/健康：检查 8080 无 Ambiguous mapping、无 Bean 冲突、应用正常 | 无启动异常 | L/REG |

### 7.2 API 层用例（W1）

**A. 认证与列表**

| 用例ID | 级别 | 步骤/请求 | 预期结果 | 依据 |
|---|---|---|---|---|
| API-LIST-01 | P0 | admin 登录后 `GET /nursing/project/list?pageNum=1&pageSize=10` | `code=200,msg=查询成功,total=9,rows.length=9`；rows 顺序 id=1..9（order_no 均为 1）；第 1 行 id=1 修剪指甲/price=30.00/status=1；第 6 行 id=6 洗头/status=0；第 9 行 id=9 心理咨询 | A3 |
| API-LIST-02 | P0 | 同上，校验排序规则（先建 1 条 `orderNo=0` 测试行后 `GET list`） | 测试行（order_no=0）排在全部 order_no=1 种子之前；其余仍 id 升序；清理后恢复 | L（排序） |
| API-LIST-03 | P1 | `GET list?name=洗` | total=3，ids=6,7,8（洗头/洗脸/洗脚）；**注意文档 D1 写“1 条”为误差** | A4/D1 |
| API-LIST-04 | P1 | `GET list?status=0` | total=1，id=6 洗头 | A4 |
| API-LIST-05 | P1 | `GET list?name=洗&status=1` | total=2（id=7 洗脸、id=8 洗脚） | L |
| API-LIST-06 | P1 | `GET list?name=不存在的名称XYZ` | total=0、rows=[]、code=200 | L |
| API-LIST-07 | P1 | `GET list?name=指`（子串匹配） | total=1（修剪指甲，验证 `%like%` 非前缀） | L |
| API-LIST-08 | P1 | `GET list?pageNum=1&pageSize=5`；`pageNum=2&pageSize=5`；`pageNum=3&pageSize=5` | 分别 rows=5（id1–5）/rows=4（id6–9）/ **page3 reasonable=true 返回末页 4 条（id6–9，框架行为非缺陷）**；total 均=9 | A5 |
| API-LIST-09 | P2 | `GET list?pageSize=20`；`pageSize=1000`；`pageNum=0` | 不 500；pageSize=20 返回 9 行；异常分页参数返回合法结构（total=9，行数由 PageHelper 决定，记录实际） | L（边界） |

**B. 详情**

| 用例ID | 级别 | 步骤/请求 | 预期结果 | 依据 |
|---|---|---|---|---|
| API-GET-01 | P0 | `GET /nursing/project/1` | code=200；data.id=1/name=修剪指甲/orderNo=1/unit=次/price=30.00/image 为 OSS URL/nursingRequirement=无/status=1/createTime 非空 | A6 |
| API-GET-02 | P1 | `GET /nursing/project/999999` | code=200，`data=null`（前端可提示） | 契约 §3.2-2 |
| API-GET-03 | P2 | `GET /nursing/project/abc`（非数字） | 返回 JSON 错误（4xx/500 且 code+msg，无 HTML 堆栈） | L（健壮） |
| API-GET-04 | P1 | 未带 token `GET /nursing/project/1` | **body code=401**（RuoYi `AuthenticationEntryPointImpl` 约定 HTTP 200 + body 401），不返回数据 | A13/安全 |

**C. 新增**

| 用例ID | 级别 | 步骤/请求 | 预期结果 | 依据 |
|---|---|---|---|---|
| API-ADD-01 | P0 | POST 合法测试行 `{name:"ZZT5-新增-<ts>",orderNo:10,unit:"次",price:9.9,status:1,nursingRequirement:"无",image:"<OSS URL>"}` | code=200/msg=操作成功；list total=10；新记录 id=**测试前只读确认的 AUTO_INCREMENT**（当前 10，不硬编码）；详情字段与入参一致 | A7/D2 |
| API-ADD-02 | P1 | POST **不带 status** 的测试行 | code=200；详情 status=1（默认值与表默认一致） | A7/契约 §4.6 |
| API-ADD-03 | P1 | POST 不带 `name`（其余字段合法） | code=500，msg 为**中文友好提示**（如“护理项目名称不能为空”）；**无 SQL/Java 堆栈**；list total 不变 | L（必填）/契约 §3.4 |
| API-ADD-04 | P1 | POST `name="   "`（纯空格） | 同 API-ADD-03（trim 后空视为缺失） | 契约 §3.4 |
| API-ADD-05 | P1 | POST name 与种子重复（如“修剪指甲”）/ 与已存在测试行重复 | code=500，msg=“护理项目名称已存在”；非堆栈；list total 不变 | A8 |
| API-ADD-06 | P2 | POST name 长度 101+（>100） | 优选 code=500 msg“名称长度不能超过100”；若发生 DB 截断/异常，记录实际（P3，建议项） | 契约 §3.4 建议 |
| API-ADD-07 | P2 | POST 不带 price/image/nursingRequirement | **契约未要求后端必填** → 预期 code=200（前端已拦截）；记录为“契约内行为”，不判缺陷 | L/契约 |
| API-ADD-08 | P3 | POST `image:""`；POST status=2；POST price 字符串 `"abc"` | 分别记录实际：image 归一 null（契约 §4.9）/status=2 是否入库/非法 JSON 类型是否 400；作为观察项 | 契约 §4.9 |
| API-ADD-09 | P2 | POST 非法 JSON（语法错误） | 返回 JSON 400（`code=400,msg`），无堆栈 | L（健壮） |

**D. 修改**

| 用例ID | 级别 | 步骤/请求 | 预期结果 | 依据 |
|---|---|---|---|---|
| API-UPD-01 | P0 | PUT 测试行全字段 `{id,name,orderNo,unit,price,status,image,nursingRequirement}` | code=200；详情全部字段更新；`createTime` 不变、`updateTime` 变化 | A9 |
| API-UPD-02 | P0 | PUT **仅** `{"id":测试行id,"status":0}`（模拟前端启停） | code=200；详情 **status=0** 且 **name/orderNo/unit/price/image/nursingRequirement 全部保持**（未被 null 覆盖） | **A10（关键）** |
| API-UPD-03 | P1 | 再 PUT `{"id":测试行id,"status":1}` 启用回来 | code=200；status=1；其它字段仍保持 | A10 |
| API-UPD-04 | P1 | PUT `{"id":999999,"status":0}` | code=500，msg“护理项目不存在”（友好错误）；不更新任何行 | 契约 §3.3 |
| API-UPD-05 | P1 | PUT 测试行 name 改为种子名（如“修剪指甲”） | code=500，msg“护理项目名称已存在”；测试行名称未变 | A8/§3.4 |
| API-UPD-06 | P1 | PUT 测试行 `{id, name:<自身当前名称>}`（排除自身） | code=200（`id<>#{id}` 逻辑正确，不误报重名） | 契约 §3.4 |
| API-UPD-07 | P2 | PUT `{}`（无 id）/ PUT 非法 JSON | 返回 JSON 错误（code+msg，无堆栈），不误更新 | L（健壮） |

**E. 删除**

| 用例ID | 级别 | 步骤/请求 | 预期结果 | 依据 |
|---|---|---|---|---|
| API-DEL-01 | P0 | `DELETE /nursing/project/{测试行id}` | code=200；list total 减 1；DB 该行不存在 | A11 |
| API-DEL-02 | P1 | `DELETE /nursing/project/999999` | code=500，msg 友好（“护理项目不存在”之类）；无堆栈 | 契约 §4.7 |
| API-DEL-03 | P0 | 清理全部 `ZZT5-*` 测试行后 `GET list` + DB 只读 | total=9；仅 9 条种子；种子行内容与基线快照一致（md5 比对） | A11/§5 写库纪律 |
| API-DEL-04 | P3 | `DELETE /nursing/project/{id1},{id2}`（批量，**契约外观察**，可选执行） | RuoYi 惯例 `Long[] ids` 可能支持；任务单只要求单 id：**无论支持与否都不判缺陷**，仅记录实际行为｜前端无批量入口 | D4/lead 决策 |

**F. 下拉 all**

| 用例ID | 级别 | 步骤/请求 | 预期结果 | 依据 |
|---|---|---|---|---|
| API-ALL-01 | P0 | `GET /nursing/project/all` | code=200；`data` 为数组，**length=9**；每项仅/含 `{label:String,value:String}`；`value` 为 id 字符串（"1".."9"）；`label` 为名称；顺序 order_no,id（1..9）；**含禁用“洗头”** | A12 |
| API-ALL-02 | P1 | 校验每项类型与顺序（脚本断言） | 9 项 label/value 全部 string；无 null/重复 value | A12 |
| API-ALL-03 | P1 | 测试期间新增测试行后再 `GET /all` | length=10 且含测试项（证明实时数据源）；清理后回 9 | A12/L |
| API-ALL-04 | P1 | 未带 token `GET /all` | body code=401，无数据（ry 负向本轮 N/A） | A13 |

**H. 图片上传（A15，R1 本地兜底口径）**

| 用例ID | 级别 | 步骤/请求 | 预期结果 | 依据 |
|---|---|---|---|---|
| API-UP-01 | P0 | admin token `POST /common/upload`（multipart，文件=`素材/服务项目图片/修剪指甲@2x.png`，≤5MB） | code=200；`url` 非空且形如 `http://localhost:8080/profile/upload/...`；`GET url` → 200 且 `Content-Type: image/*`、内容长度>0 | A15/lead R1 决策 |
| API-UP-02 | P2 | 上传非图片（`.txt`）/空文件 | 记录实际行为（返回 url 或友好错误均可；不影响 A15 结论）；无 500 堆栈 | A15 补充 |

**G. 权限与安全健壮**

| 用例ID | 级别 | 步骤/请求 | 预期结果 | 依据 |
|---|---|---|---|---|
| API-AUTH-01 | P0 | admin token 调用 6 接口 | 全部按契约返回（200/业务提示） | A13 |
| API-AUTH-02 | N/A | `ry`（无角色）token 调 list/all/get | **本轮 N/A**（lead 决策：不建测试账号、不改库）；仅在报告记录“负向权限未实测，`@PreAuthorize` 由代码评审确认” | A13/lead 决策 |
| API-AUTH-03 | P1 | 无 token 调 list/POST/PUT/DELETE | 401 JSON；写接口不产生数据（DB 校验行数不变） | 安全 |
| API-AUTH-04 | P2 | 只读核对 5 个 `@PreAuthorize` 权限点与菜单 `sys_menu` 一致（list/query/add/edit/remove） | 一致（admin 不受影响） | A13/STA-04 |
| API-SEC-01 | P1 | `GET list?name=' OR '1'='1`；`name=%` 等 | 返回 0/符合 like 语义，**不返回全部数据**；无 SQL 异常 | 安全 |
| API-SEC-02 | P1 | POST name 含 `<script>alert(1)</script>` / `' OR 1=1 --` | 正常入库（参数化）或友好校验；list 查询不执行注入、不报 SQL 错误；响应 JSON | 安全 |
| API-SEC-03 | P2 | 错误路径（非法 id/不存在/重名）响应体审查 | 均为 JSON `{code,msg}`；**无 Java 堆栈/绝对路径/SQL 语句** | L/安全 |
| API-SEC-04 | P2 | 审计：新增/修改/删除/启停后只读查 `sys_oper_log` | 有 `title='护理项目'` 且 business_type 对应的记录（@Log 生效） | 任务单 §3.5/§4.8 |

### 7.3 UI 层用例（W2，Playwright）

> 验证码：Playwright 拦截 `/dev-api/captchaImage` 响应存图 → 我视觉解码 → 脚本读取答案文件继续（§4.2）。
> 证据：每用例截图/关键网络响应存 `artifacts/test_web_s5/ui/`。

**登录与导航**

| 用例ID | 级别 | 步骤 | 预期结果 | 依据 |
|---|---|---|---|---|
| UI-LOGIN-01 | P0 | 打开 `http://localhost:88/` | 重定向 `/login`；标题“中州养老”；用户名/密码/验证码输入框 + 登录按钮渲染 | A14 |
| UI-LOGIN-02 | P1 | 登录页空提交 | 出现校验提示（账号/密码/验证码必填类），不发起登录请求 | L |
| UI-LOGIN-03 | P0 | admin + 正确密码 + 正确验证码 → 登录 | 登录成功；跳转 `/index`；右上角显示 admin；localStorage/cookie 有 token | L |
| UI-NAV-01 | P1 | 侧边栏“服务管理 → 护理项目”（menu 2001）可见并点击 | 进入 `/nursing/project`；TagsView 新增“护理项目”页签 | A14 |
| UI-NAV-02 | P2 | 浏览器直接访问 `http://localhost:88/nursing/project` | 页面正常加载（无 404/空白/报错页） | A14 |

**列表**

| 用例ID | 级别 | 步骤 | 预期结果 | 依据 |
|---|---|---|---|---|
| UI-LIST-01 | P0 | 打开护理项目页 | 表格渲染 **9 行**；第 1 行“修剪指甲”、第 6 行“洗头”、第 9 行“心理咨询”；无“暂无数据” | A14 |
| UI-LIST-02 | P1 | 校验表头 | **10 列**：编号/名称/排序号/单位/价格/图片/护理要求/状态/创建时间/操作（编号为 1..9 序号） | D3/前端 |
| UI-LIST-03 | P1 | 校验状态列/操作按钮 | 洗头行状态 tag “禁用”（红），其余“启用”（绿）；启用行按钮“禁用”、禁用行按钮“启用” | 前端 |
| UI-LIST-04 | P1 | 校验图片列与创建时间 | 图片缩略图 50×50（`<img>` 有 src；OSS 加载成功/失败均记录，失败记 ENV-网络，不判功能失败）；创建时间格式 `yyyy-MM-dd HH:mm:ss` | 前端 |
| UI-LIST-05 | P1 | 校验分页条 | 显示“共 9 条”、默认“10条/页”、无分页按钮（单页） | A14 |
| UI-LIST-06 | P2 | 检查浏览器控制台 | 页面无 JS 报错（警告除外）；无 401/403 循环 | L |

**搜索/重置**

| 用例ID | 级别 | 步骤 | 预期结果 | 依据 |
|---|---|---|---|---|
| UI-SEARCH-01 | P0 | 名称输入“洗”→ 点搜索 | 列表 **3 行**（洗头/洗脸/洗脚）；“共 3 条”；网络 `name=洗` 请求 200 | A4/D1 |
| UI-SEARCH-02 | P1 | 点“重置” | 条件清空、列表恢复 9 行 | 前端 |
| UI-SEARCH-03 | P1 | 状态下拉选“禁用”→ 搜索 | 1 行（洗头）；total=1 | A4 |
| UI-SEARCH-04 | P1 | 名称“洗”+ 状态“启用”组合搜索 | 2 行（洗脸/洗脚） | L |
| UI-SEARCH-05 | P2 | 名称输入不存在值 → 搜索 | 列表空态“暂无数据”；分页 total=0 | L |
| UI-SEARCH-06 | P2 | 名称输入框内回车 | 触发搜索（同 UI-SEARCH-01） | 前端 |

**新增（含图片上传）**

| 用例ID | 级别 | 步骤 | 预期结果 | 依据 |
|---|---|---|---|---|
| UI-ADD-01 | P0 | 点“新增” | 弹窗标题“添加护理项目”；7 个字段（名称/排序号/单位/价格/状态/图片/护理要求）+ 确定/取消；表单为空 | 前端 |
| UI-ADD-02 | P0 | 空表单直接点“确定” | 4 条校验提示：名称/价格/图片/护理要求“不能为空”（对应 rules）；弹窗不关闭；**无 POST 请求** | 前端/A14 |
| UI-ADD-03 | P2 | 排序号/价格控件边界（输入 0/21/999、价格 1/101/7） | 按控件 min/max/step 纠正或限制（orderNo 1–20、price 5–100 step5）；记录实际行为 | 前端 |
| UI-ADD-04 | P0 | 图片上传：选择 `素材/服务项目图片/修剪指甲@2x.png`，等待上传完成 | **A15（本地兜底口径）**：`/dev-api/common/upload` 返回 code=200 且 `url` 非空（`http://localhost:8080/profile/upload/...`）；上传框显示图片缩略图（预览可打开）；`GET url` 200 图片资源。失败时记录错误并改用 **route.fulfill 桩**继续 UI-ADD-05/06（报告注明“桩替代”，A15 记 FAIL/ENV） | A15/lead R1 决策 |
| UI-ADD-05 | P0 | 填合法数据（name=`ZZT5-UI新增-<ts>`、单位“次”、价格 50、护理要求“测试”、图片就绪）→ 确定 | toast“新增成功”；弹窗关闭；列表刷新 **total=10**；新行按排序号显示（orderNo=1 时在末行/第 2 页，记录实际） | A14 |
| UI-ADD-06 | P1 | 新增时不选状态（radio 无默认选中）→ 提交 | 提交成功；DB/详情 status=1（后端默认）；**记录观察项**：前端 radio 无默认选中（与 C++ UI 规格“默认启用”不一致，前端不改，P3 观察） | 前端/契约 |
| UI-ADD-07 | P1 | 新增重名（名称与“修剪指甲”相同）→ 确定 | 后端返回友好错误 toast（“护理项目名称已存在”，非 SQL 堆栈）；**无新增行**；弹窗保持（前端 catch 不关闭） | A8 |
| UI-ADD-08 | P2 | 点“取消” | 弹窗关闭、表单重置、无请求、列表不变 | 前端 |

**修改**

| 用例ID | 级别 | 步骤 | 预期结果 | 依据 |
|---|---|---|---|---|
| UI-EDIT-01 | P0 | 对测试行点“修改” | 弹窗标题“修改护理项目”；**全字段回填**（名称/排序号/单位/价格/护理要求/图片预览/状态单选，含 status→String 回填）；网络 `GET /{id}` 200 | A14/前端 |
| UI-EDIT-02 | P0 | 改名称/单位/价格/护理要求 → 确定 | toast“修改成功”；列表显示新值（名称变化可见）；网络 PUT 200 | A9/A14 |
| UI-EDIT-03 | P1 | 打开修改后**取消** | 数据未变；无 PUT 请求 | 前端 |
| UI-EDIT-04 | P1 | 修改为已存在名称 → 确定 | 友好错误 toast；值未变（对应 API-UPD-05） | A8 |

**删除**

| 用例ID | 级别 | 步骤 | 预期结果 | 依据 |
|---|---|---|---|---|
| UI-DEL-01 | P0 | 测试行点“删除” → 确认框出现，点“取消” | 文案 `是否确认删除护理项目编号为"<id>"的数据项？`；取消后行仍在、无 DELETE 请求 | 前端 |
| UI-DEL-02 | P0 | 再次删除 → 点“确定” | toast“删除成功”；列表 total 减 1、行消失；网络 DELETE 200 | A11/A14 |
| UI-DEL-03 | P1 | 删除后刷新页面（F5） | 数据一致（持久化生效） | A14 |

**启停（关键 A10 的 UI 侧）**

| 用例ID | 级别 | 步骤 | 预期结果 | 依据 |
|---|---|---|---|---|
| UI-TOG-01 | P0 | 测试行点“禁用” → 确认框 | 文案“是否确认禁用这条护理项目？”；取消→状态不变 | 前端 |
| UI-TOG-02 | P0 | 确认禁用 | toast“禁用成功”；状态 tag 变“禁用”（红）、按钮变“启用”；网络 PUT 请求体**仅 `{id,status:0}`**（抓包断言） | A10/A14 |
| UI-TOG-03 | P1 | 再点“启用”→ 确认 | toast“启用成功”；状态恢复“启用”；其它字段仍正常显示（未被清空） | A10 |
| UI-TOG-04 | P1 | 启停后刷新页面 | 状态持久；该行 name/unit/price/image 等无丢失（部分更新未覆盖） | A10 |

**分页**

| 用例ID | 级别 | 步骤 | 预期结果 | 依据 |
|---|---|---|---|---|
| UI-PAGE-01 | P1 | 制造 11 行（+2 测试行）后查看列表 | “共 11 条”；第 1 页 10 行；出现第 2 页按钮 | A5/A14 |
| UI-PAGE-02 | P1 | 点第 2 页 | 显示第 11 行（测试行，按排序）；数据正确；请求 `pageNum=2` | A5 |
| UI-PAGE-03 | P2 | 切换“每页条数”10→20（若组件支持） | 单页显示 11 行；页码回到 1 | 前端 |

**前端零改动护栏**

| 用例ID | 级别 | 步骤 | 预期结果 | 依据 |
|---|---|---|---|---|
| UI-GUARD-01 | P0 | 测试结束再次执行 STA-01 的 git/哈希核对 | 前端目录零改动（无 modified/untracked） | A1 |
| UI-GUARD-02 | P1 | 抓包核对：列表/详情/新增/修改/删除/启停/all 请求均发往 `/dev-api/nursing/project/*` 且返回真实后端 JSON（非本地 mock） | 路径/方法/参数与 `src/api/nursing/project.js` 完全一致 | A1/A14 |

### 7.4 联动用例

| 用例ID | 级别 | 步骤 | 预期结果 | 依据 |
|---|---|---|---|---|
| LINK-01 | P0 | 登录后打开 `/nursing/plan` | 页面结构正常渲染（计划列表自身接口仍失败 toast 属已知 R4，不判缺陷）；关键：`/nursing/project/all` 请求 200 | A12/R4 |
| LINK-02 | P1 | plan 页点“新增”→ 弹窗内“护理项目名称”下拉展开 | 下拉显示 **9 项**，label=名称（含禁用“洗头”）、value=字符串 id；顺序 1..9 | A12 |
| LINK-03 | P1 | 选择某一项 | 选中值正确写入（DOM 显示所选 label）；`el-option` value 为字符串（与后端 `value:String` 一致）；**不提交**（plan 提交接口缺失，避免无关报错） | A12 |
| LINK-04 | P3 | 检查菜单/路由：`/nursing/arrange`（任务安排孤儿页无菜单） | 不在本次范围，记录现状即可 | R4 |

### 7.5 回归用例（窗口内，只读）

| 用例ID | 级别 | 步骤 | 预期结果 | 依据 |
|---|---|---|---|---|
| REG-01 | P0 | 8080 健康：端口 LISTEN、`GET /`、无需重启 | 服务正常（PID 未变或为 devtools 正常重启后的新进程，记录实际） | L |
| REG-02 | P0 | 房型设置 `/liveIn/houseSet` | 列表 **6 行**（room_type）、图片/状态/操作正常；打开“新增房型”弹窗渲染正常后取消（不提交） | L |
| REG-03 | P1 | 用户管理 `/system/user` | 列表 **7 行**、分页/状态正常 | L |
| REG-04 | P1 | 护理项目页自身（原 404 页面） | 已恢复为本次验收对象（无回归问题） | A14 |
| REG-05 | P1 | 关键既有 API：`/elder/roomType/list`、`/system/user/list`、`/system/menu/getRouters` | 200 且结构/数量与测试前一致（6/7/正常路由）；**无路径冲突** | L |
| REG-06 | P1 | 其它服务管理页面：`/nursing/plan`、`/nursing/level`、`/nursing/oldPeople` | 可打开；plan/level 接口失败为既有状态（与本次无关，记录）；oldPeople 正常 | L/R4 |

### 7.6 非功能/权限/安全（汇总）

| 用例ID | 级别 | 步骤 | 预期结果 | 依据 |
|---|---|---|---|---|
| NF-01 | P0 | 未登录直接访问 `/nursing/project`（UI） | 跳转登录页（不渲染业务数据） | A13 |
| NF-02 | N/A | 无权限账号 `ry` 登录后访问护理项目页/接口 | **本轮 N/A**（lead 决策；不建账号、不改库）；报告记录“负向权限未实测，已由代码评审 + 菜单权限点核对替代” | A13/lead 决策 |
| NF-03 | P1 | 审计：UI 新增/修改/删除/启停后查 `sys_oper_log` | 有对应“护理项目”操作日志；登录日志有记录 | §4.8 |
| NF-04 | P1 | 异常响应审查 | 所有错误为 JSON code/msg，无堆栈/路径/SQL 暴露 | 安全 |
| NF-05 | P2 | 注入字符 name 搜索/新增 | 不改变语义、不报 SQL 异常（参数化验证） | 安全 |
| NF-06 | P2 | 浏览器控制台/网络面板 | 关键页面无未捕获 JS 异常；无 4xx/5xx 除已知范围外 | L |
| NF-07 | P2 | 大/非法分页参数（API） | 不返回 500 堆栈（同 API-LIST-09） | L |

---

## 8. 缺陷分级与判定口径

| 等级 | 定义 | 示例 | 处理 |
|---|---|---|---|
| **P1 阻塞** | 核心功能不可用/服务不可用/数据被破坏/契约关键项不符 | 列表 500、新增/修改/删除失败、**启停清空其它字段（A10）**、前端代码被改、8080 启动失败 | 必须修复后复测；阻塞验收 |
| **P2 严重** | 主要功能缺陷或契约不符，有数据/安全风险 | 排序缺失、重名返回 SQL 堆栈、权限注解缺失、部分更新部分字段丢失、分页错乱 | 验收前修复或经 lead 书面豁免 |
| **P3 一般** | 次要功能/边界/提示不友好 | name>100 报 SQL 错误、非法分页 500、错误提示非中文、审计日志缺失 | 记录，不阻塞（可下 Sprint 修） |
| **P4 轻微/观察** | 体验/文案/文档偏差/环境限制 | 状态 radio 无默认选中、文档 D1/D2/D3、图片 OSS 偶发加载失败 | 记录，不判缺陷 |
| **ENV** | 环境阻塞（非代码缺陷） | OSS 上传失败（R1）、浏览器自动化不可用 | 按 lead 决策记录；用桩/降级验证并注明 |

**判定注意**：① 以任务单契约优先；② 文档偏差（D1/D2/D3/D4）不计缺陷；③ 桩替代的上传链路不判“上传功能通过”；④ 只判“本次新增代码引入的问题”，既有 out-of-scope 模块缺陷只登记不追责。

---

## 9. 窗口申请预案（批准后执行，向 web-control）

```
W1【API】web-tester | 护理项目 API 功能/权限/回归 + 测试数据清理 | 后端8080稳定窗口 + MySQL写 | 10 分钟 | 仅 zzyl.nursing_project 的 ZZT5-* 测试行（≤4 行，结束后删除回 9 条）；sys_logininfor/sys_oper_log 自动日志；无重启、不动种子数据 | 否
W2【UI】web-tester | 护理项目 UI 浏览器验收（列表/搜索/新增/修改/删除/启停/分页/联动/回归）+ 截图 | 浏览器自动化（无头 Edge）+ 后端8080只读访问 + MySQL写 | 15 分钟 | 同上（测试行+自动日志）；无重启 | 否
W3【复测】web-tester | 缺陷复测/补证（按需） | 按实际（浏览器或API） | ≤10 分钟 | 同上 | 否
释放模板：释放 DQ-xxx ｜结果：完成/部分完成 ｜产物：artifacts/test_web_s5/ + 测试报告 ｜实际影响行数：nursing_project +N/-N（回 9）、sys_logininfor +N、sys_oper_log +N ｜残留进程：无
```

**开测前置条件（缺一不测）**：① dev1 完成通知 + arch review PASS（lead 通知）；② 8080 已加载新代码（先只读探测接口）；③ 前端 88 正常；④ 浏览器/写入窗口批准；⑤ 工具就绪（playwright-core 或 CDP 备选）。

---

## 10. 风险与应对（测试侧）

| 风险 | 应对 |
|---|---|
| 验证码识别失败 | 2 次失败即停，改用 Redis 只读方案（先报 lead）或请 admin 协助；不重试刷屏 |
| 8080 热重启/未加载新代码 | 进窗口先健康探测；不稳定等 30s；仍失败报 lead/web-control，不擅自重启 |
| OSS 上传失败（R1） | 记 ENV，用 route.fulfill 桩验证表单链路，上传功能由 lead 决策；不擅自改代码/配置 |
| 窗口内时间不够 | 优先 P0/P1；P2/P3 记录“未执行”并在报告说明；申请 W3 续测 |
| 写库污染/误删种子 | 只用 ZZT5-* 测试行；基线快照 + 收测 md5 比对；不删被引用种子行 |
| 与 dev1/arch 联调撞车 | 测试期间若后端再次编译（devtools 重启），暂停操作 30s 再探测；严重则顺延窗口 |
| 浏览器进程残留 | 脚本 finally 关闭浏览器；释放前核对 headless 进程为空（PID+路径确认，不按名杀） |

---

## 11. 报告结构与交付

`docs/test/Web_Sprint5_护理项目_测试报告.md` 必含：
1. **执行摘要**：用例总数/**通过数**/失败数/阻塞数/未执行数（按层/优先级拆分）；结论（通过/有条件通过/不通过）；
2. **环境与窗口记录**：DQ 编号、起止、实际影响行数、种子 md5 对比、前端零改动证据；
3. **缺陷清单**（编号 BUG-S5-xx：标题/严重级/所在层/复现步骤/实际 vs 预期/证据路径/建议）；
4. **A1–A15 验收矩阵**（逐项 PASS/FAIL/条件-PASS/NA + 证据链接；A13 负向=N/A 需注明 lead 决策，A15 按本地兜底口径）；
5. **文档偏差与观察项**（D1–D4、UI 观察、ENV 项）；
6. **结论与建议**（是否可交付 lead/主人验收、遗留项优先级）；
7. **附件索引**（artifacts 目录树 + 关键截图/JSON）。

---

## 12. 变更记录

| 版本 | 日期 | 变更 | 作者 |
|---|---|---|---|
| v1.0 | 2026-11-25 | 首版：范围/契约基线/环境工具/数据申报/109 用例（STA 7 + API 44 + UI 41 + LINK 4 + REG 6 + NF 7）/缺陷分级/窗口预案/报告结构 | web-tester |
| v1.1 | 2026-11-25 | 按 lead 决策更新：R1 改后端本地兜底（A15 新口径 + 新增 API-UP-01/02）；A13 负向权限 N/A；批量删除降为契约外可选；STA-02 纳入 lead 批准的 `CommonController` 修改；用例总数 **111**（STA 7 + API 46 + UI 41 + LINK 4 + REG 6 + NF 7，其中 2 项 N/A） | web-tester |
| v1.2 | 2026-11-25 | 执行期对齐：验收范围 A1–A16；证据路径改为 `artifacts/test_web_s5/`；分页超界 reasonable=true 口径；未认证响应 HTTP200+body401 口径；A16 图片共存显示纳入；API 层实际执行 **47/47 PASS**（另 A7 create_by 未填充记 P3 观察，见测试报告） | web-tester |
