# Web Sprint 5 · 护理项目模块 CRUD 后端 任务单

> 任务编号：S5-WEB-01｜模块：服务管理-护理项目（`/nursing/project`）
> 目标：补齐**后端** 6 个接口，使现有前端页面（**禁止修改前端代码**）可正常完成 增/删/改/查/启停/下拉。
> 发布人：web-arch｜日期：2026-11-25 09:05｜执行人：web-dev1｜评审：web-arch｜验收：web-tester / lead
> 依据：lead 预检（表/实体/契约已确认）+ web-arch 只读核查（见 §2）
> 参考实现：同模块 `RoomTypeController / IRoomTypeService / RoomTypeServiceImpl / RoomTypeMapper / RoomTypeMapper.xml`

---

## 1. 范围与硬约束

| 项 | 要求 |
|---|---|
| 允许修改 | **仅后端**：①`zzyl-nursing-platform` 护理项目相关新增文件 + `resources/mapper/nursing/NursingProjectMapper.xml`（补排序）；②**`zzyl-admin/.../common/CommonController.java` 的 `/common/upload` 单文件方法（仅加 OSS 失败/未配置时的本地兜底，D-Web-S5-01）** |
| 禁止修改 | **前端任何代码**；`plan/arrange/房间/床位/入住` 等其它模块；global 配置；数据库结构；`/common/uploads`（多文件，已是本地，不动） |
| 不做 | C++ 端、页面改造、导出功能（菜单有 export 权限但页面无按钮，本期不实现）、Redis 缓存（`CacheConstants.NURSING_PROJECT_ALL_KEY` 已预留但无调用，本期不强制） |
| 数据库 | 表已存在，**不要建表/改表**；测试数据写入需向 web-control 正式申请（MySQL 写） |
| 环境 | 编译/IDEA Build → 向 web-control **轻量报备**（开始/结束各一句）；需要 8080 稳定窗口/主动重启 → **正式申请**；**不得停/重启主人 IDEA 实例 java PID 23192** |

---

## 2. 只读核查结论（web-arch，2026-11-25 09:00–09:10）

| # | 检查项 | 结论 |
|---|---|---|
| 1 | 表 `zzyl.nursing_project` | ✅ 存在，9 行（8 启用 / 1 禁用“洗头”）；`AUTO_INCREMENT` 以**测试前只读实测**为准（2026-11-25 实测值 = **10**，**不要硬编码 id**）；`name` 上有 **UNIQUE 索引**（`INDEX_NAME=name, NON_UNIQUE=0`）；字段见 §3.1 |
| 2 | 实体/VO | ✅ `domain/NursingProject.java` 字段齐全、`extends BaseEntity`；`vo/NursingProjectVo.java` 已有 `label/value`（均为 String）。无 `@TableName/@TableId` → MyBatis-Plus 默认表名 `nursing_project`、主键自增（全局 `id-type: auto`），可用 |
| 3 | 缺失文件 | ❌ 缺 `NursingProjectMapper.java`、`INursingProjectService.java`、`NursingProjectServiceImpl.java`、`NursingProjectController.java`；**`resources/mapper/nursing/NursingProjectMapper.xml` 已存在**（含 resultMap + list/byId/insert/update/delete SQL），仅**缺 `order by order_no, id`** |
| 4 | 菜单/权限 | ✅ `sys_menu` 已存在：2001 护理项目（`nursing:project:list`）+ 2002 query/2003 add/2004 edit/2005 remove/2006 export，均 visible=0/status=0；admin `*:*:*` 不受影响。**无需改库** |
| 5 | `/common/upload` | ⚠️ `CommonController` 存在 `POST /common/upload`（单文件，返回 `{url,fileName,newFileName,originalFilename}`），但底层 `AliOssUtil` 上传阿里云 OSS，AK/SK 读环境变量 `OSS_ACCESS_KEY_ID/OSS_ACCESS_KEY_SECRET`——**当前本机两个变量均未设置**，运行态预计上传报错（OSS 认证失败）；不影响已有 9 条 image URL 的读取/显示（历史值为 OSS 绝对地址，浏览器可直接加载）。**风险 R1，需 lead 决策**（配环境变量 or 验收降级为“URL 可保存/显示”） |
| 6 | Mapper 扫描 | ✅ `ApplicationConfig` 有 `@MapperScan("com.zzyl.**.mapper")`；`application.yml`：`mapperLocations: classpath*:mapper/**/*Mapper.xml`、`typeAliasesPackage: com.zzyl.**.domain` → 新 Mapper 接口与 XML 都会自动扫描 |
| 7 | 其它依赖 `/nursing/project/all` | `views/nursing/plan/index.vue`（护理计划弹窗项目下拉）与 `views/nursing/arrange/index.vue`（任务安排，孤儿页无路由）。**plan 页**会因本接口上线而恢复“项目下拉”可用（但 plan 列表仍受自身后端缺失影响）；无其它后端代码依赖该项目数据 |
| 8 | 自动填充/审计 | ✅ `MyMetaObjectHandler` 对 MyBatis-Plus insert/update 自动填 `createBy/createTime/updateBy/updateTime`；`@Log` 可记 `sys_oper_log` |

---

## 3. 数据与接口契约

### 3.1 表字段（`nursing_project`，实测）

| 字段 | 类型 | 空 | 默认 | 说明 |
|---|---|---|---|---|
| id | bigint PK | NO | auto_increment | 编号 |
| name | varchar(100) **UNIQUE** | YES | NULL | 名称（重名会触发唯一约束） |
| order_no | int | YES | NULL | 排序号 |
| unit | varchar(50) | YES | NULL | 单位 |
| price | decimal(10,2) | YES | NULL | 价格 |
| image | varchar(255) | YES | NULL | 图片 URL（OSS 绝对地址） |
| nursing_requirement | varchar(255) | YES | NULL | 护理要求 |
| status | int | NO | 1 | 0 禁用 / 1 启用 |
| create_by / update_by | varchar(50) | YES | NULL | 审计 |
| remark | varchar(255) | YES | NULL | 备注 |
| create_time | datetime | NO | CURRENT_TIMESTAMP | — |
| update_time | datetime | YES | CURRENT_TIMESTAMP on update | — |

### 3.2 接口（6 个，全部 `/nursing/project`）

| # | 方法/路径 | 入参 | 出参 | 权限点 | 说明 |
|---|---|---|---|---|---|
| 1 | `GET /nursing/project/list` | `pageNum,pageSize,name,status`（query） | RuoYi `TableDataInfo`：`{total, rows[], code, msg}` | `nursing:project:list` | `startPage()` + `getDataTable(list)`；**排序 `order by order_no, id`**；name 模糊 `like`、status 精确 |
| 2 | `GET /nursing/project/{id}` | path id | `AjaxResult{code,msg,data: NursingProject}` | `nursing:project:query` | 不存在时 data=null（前端可提示） |
| 3 | `POST /nursing/project` | JSON `NursingProject` | `AjaxResult{code:200,msg:"操作成功"}` | `nursing:project:add` | 成功返回 `toAjax(1)` |
| 4 | `PUT /nursing/project` | JSON `NursingProject`（**可能只含 id+status**） | `AjaxResult` | `nursing:project:edit` | **部分更新**：null 字段不覆盖（见 §3.3） |
| 5 | `DELETE /nursing/project/{id}` | path id（**单 id，不是数组**） | `AjaxResult` | `nursing:project:remove` | 前端 `delProject(id)` 传单值 |
| 6 | `GET /nursing/project/all` | 无 | `AjaxResult{code:200,data:[NursingProjectVo]}` | `nursing:project:list`（或 query） | 每项 `{label: name, value: String(id)}`；按 `order_no,id` 排序；**返回全量 9 条**（含禁用，便于计划页回填 label） |

**前端调用点**（只读核对，不得改）：
- `src/api/nursing/project.js`：`listProject/getProject/addProject/updateProject/delProject/getAllProjects` → 路径与上表一致；
- `views/nursing/project/index.vue`：列表列（编号/名称/排序号/单位/价格/图片/护理要求/状态/创建时间/操作）、表单校验（名称/价格/图片/护理要求必填；orderNo 1~20；price 5~100 step5）、启停按钮只传 `{id,status}`；
- `views/nursing/plan/index.vue` 依赖 `/all` 填充“护理项目”下拉。

**表格列口径（避免验收打架）**：前端表头共 **10 列** = **9 个数据列**（编号 / 名称 / 排序号 / 单位 / 价格 / 图片 / 护理要求 / 状态 / 创建时间）+ **1 个操作列**（修改/删除/启停）。
**删除口径**：UI 只验**单删**（`DELETE /nursing/project/{id}`）；批量删除 `DELETE /nursing/project/{ids}` 属 API 可选，**不作为本轮验收强制项**。

### 3.3 启停“部分更新”语义（必须满足）

- 前端启停：`updateProject({ id: row.id, status: row.status === 0 ? 1 : 0 })`，**不传其他字段**；
- 实现须保证：`updateById(entity)`（MyBatis-Plus 默认 NOT_NULL 策略，null 字段不进入 SET）或 XML `<if test="x != null">` 均可；
- **禁止**“先 select 再整对象 update”的写法覆盖其它字段（会把 unit/price/image 等置空）；
- 校验：`id` 必须存在；更新影响行数 0 时返回 `AjaxResult.error("护理项目不存在")`；
- 成功返回 200，前端 `res.code == 200` 后刷新列表。

### 3.4 唯一名冲突处理

- 新增：`name` 必填（trim 后非空）；先按 `name` 精确查重，存在 → `AjaxResult.error("护理项目名称已存在")`；
- 修改：名称变更时查重需 **排除自身 id**（`id <> #{id}`）；
- 兜底：捕获 `DuplicateKeyException`（并发/DB 唯一索引）返回同一友好错误，不抛 500 堆栈；
- `name` 长度 ≤100（前端未限 maxlength，后端可截断/报错，建议超过 100 返回“名称长度不能超过100”）。

### 3.5 后端文件清单与职责

| # | 文件（模块 `zzyl-nursing-platform`） | 动作 | 职责 |
|---|---|---|---|
| 1 | `src/main/java/com/zzyl/nursing/mapper/NursingProjectMapper.java` | **新建** | `@Mapper interface NursingProjectMapper extends BaseMapper<NursingProject>`；声明 `List<NursingProject> selectNursingProjectList(NursingProject q)`（XML 已有）；可声明 selectById（XML 已有）或直接用 MP `selectById`；声明 `List<NursingProjectVo> selectNursingProjectVoList()`（用于 /all，可选 XML 或 Java 转换） |
| 2 | `src/main/resources/mapper/nursing/NursingProjectMapper.xml` | **仅修改** | list 查询末尾补 `order by order_no, id`；如新增 `/all` XML，则加 `selectNursingProjectVoList`（`select id, name ... order by order_no,id`）；**不要删已有 SQL** |
| 3 | `src/main/java/com/zzyl/nursing/service/INursingProjectService.java` | **新建** | 方法：`selectNursingProjectList`、`selectNursingProjectById`、`insertNursingProject`、`updateNursingProject`、`deleteNursingProjectById`、`selectNursingProjectAll`、`checkNameUnique`（返回 boolean；建议 `boolean checkNameUnique(NursingProject)`） |
| 4 | `src/main/java/com/zzyl/nursing/service/impl/NursingProjectServiceImpl.java` | **新建** | `extends ServiceImpl<NursingProjectMapper, NursingProject> implements INursingProjectService`；list 走 mapper XML；getById/save/updateById/removeById 用 MP；唯一名校验（LambdaQueryWrapper，update 时 `ne(id)`）；all 转 VO（`label=name, value=String.valueOf(id)`）；插入前 `name=trim`、`status` 默认 1（若为 null） |
| 5 | `src/main/java/com/zzyl/nursing/controller/NursingProjectController.java` | **新建** | `@RestController @RequestMapping("/nursing/project")`；`extends BaseController`；list（`startPage()`+`getDataTable`）、getInfo、add、edit、remove、all；每个写操作 `@Log(title="护理项目", businessType=INSERT/UPDATE/DELETE)`；权限注解 `@PreAuthorize("@ss.hasPermi('nursing:project:xxx')")`；Swagger `@Api/@ApiOperation` 可加 |
| 6 | `zzyl-admin/src/main/java/com/zzyl/web/controller/common/CommonController.java` | **修改（仅 `/common/upload` 单文件方法）** | OSS 优先路径保留；`AliOssUtil` 的 key/secret 为空或 OSS 抛异常时**本地兜底**：`String fileName = FileUploadUtils.upload(RuoYiConfig.getUploadPath(), file); String url = serverConfig.getUrl() + fileName;`，返回字段语义不变（见 §3.6 / D-Web-S5-01）；`/common/uploads` 不动 |

> 注意：RoomTypeController **没有**权限注解，但本任务按 lead 要求必须加 5 个权限点；`@PreAuthorize` 使用 RuoYi 自定义 `@ss`（`com.zzyl.framework.web.service.PermissionService`）。

### 3.6 图片上传本地兜底（Lead R1 决策 = 选项 c，D-Web-S5-01）

**背景**：`/common/upload` 当前只走阿里云 OSS，而本机 `OSS_ACCESS_KEY_ID/OSS_ACCESS_KEY_SECRET` 环境变量为空 → 上传必失败；图片是护理项目表单必填项，不兜底则新增/修改无法验收。

**实现要求（仅改 `/common/upload` 单文件方法）**：
1. 保留 OSS 优先：key/secret 非空时先 `aliOssUtil.upload(originalFilename, inputStream)`；
2. key/secret 为空 **或** OSS 调用异常 → **本地兜底**：
   ```java
   String fileName = FileUploadUtils.upload(RuoYiConfig.getUploadPath(), file);
   String url = serverConfig.getUrl() + fileName;   // 例：http://localhost:8080/profile/upload/2026/11/25/xxx.png
   ```
3. 响应字段保持现有语义：`url`=完整可访问 URL、`fileName`=同 url（沿用当前单文件分支写法）、`newFileName`=文件名、`originalFilename`=原文件名；
4. 兜底也失败 → `AjaxResult.error(...)`（不要抛未捕获异常）；
5. 建议 `log.warn` 记录“OSS 不可用，已本地兜底”；
6. **未破坏 OSS 配置时的原行为**：key/secret 存在且 OSS 成功 → 仍返回 OSS URL；
7. `/common/uploads` 已是本地实现，**不要动**；`ResourcesConfig` 已有 `/profile/**` → `file:C:/ruoyi/uploadPath/` 映射（已核实），兜底 URL 可直接被浏览器/前端显示。

**已接受差异**：`D-Web-S5-01` —— 未配置 OSS 时，新上传图片落在本机 `C:/ruoyi/uploadPath/upload/...` 并通过 `http://localhost:8080/profile/...` 访问；已有 9 条历史记录仍为 OSS 绝对 URL，两者可共存显示。C++ 端只需按字符串 URL 处理，无需区分来源。

---

## 4. 实现要点（防坑清单）

1. **分页**：用 `BaseController.startPage()` + `getDataTable(list)`（PageHelper），不要用 MP `IPage`（项目现有风格）。
2. **排序**：`order by order_no asc, id asc` 必须加在 XML list 查询末尾（PageHelper 会拼 limit，排序必须进 SQL）。
3. **部分更新**：用 MP `updateById` 即可；不要写全量 update。
4. **唯一名**：service 预校验 + catch `DuplicateKeyException` 双保险；错误消息用中文友好提示。
5. **all 的 VO**：`value` 必须是 **String**（前端下拉 value 用字符串匹配；直接 `String.valueOf(id)`）。
6. **状态默认值**：新增若 JSON 未带 status，默认 1（与表默认一致）。
7. **删除**：前端传单 id；`removeById(id)`；不存在返回 error（不抛异常）。
8. **审计字段**：交给 MyMetaObjectHandler 自动填充，不要手工覆盖 createTime/createBy。
9. **图片字段**：只存字符串 URL，不做格式/可达性校验；空字符串按 null 处理（避免 `image=''`）。
10. **不新增依赖、不改 `application.yml`、不改前端、不建表**。
11. XML 注意：namespace 与 Mapper 接口全限定名一致；`resultMap type="NursingProject"` 依赖 `typeAliasesPackage`（已配置）。
12. 新增/修改后 `getDataTable` 返回 `code=200/msg=查询成功`（RuoYi 标准），前端按 `res.code==200` 判断。

---

## 5. 验收标准（web-tester / lead）

前置：dev1 完成 + 编译通过（轻量报备 web-control）；联调时按规则向 web-control 申请（8080 稳定窗口/浏览器/MySQL 写；浏览器自动化与 MySQL 写可并行申请）。

| # | 用例 | 预期 |
|---|---|---|
| A1 | 代码 Diff | **前端零改动**；后端仅新增 4 文件 + XML 排序修改（git diff 可核） |
| A2 | 编译 | `mvn -DskipTests compile` 通过（已向 web-control 轻量报备） |
| A3 | 列表 | `GET /nursing/project/list?pageNum=1&pageSize=10` → `code=200,total=9,rows.length=9`；顺序按 order_no,id（order_no 全为 1 → 实际按 id 升序）；第 1 行 id=1 修剪指甲、第 6 行 id=6 洗头（禁用）、第 9 行 id=9 心理咨询 |
| A4 | 搜索 | `name=洗` → **3 条**（洗头 id=6 / 洗脸 id=7 / 洗脚 id=8，按 order_no,id 排序）；`status=0` → 1 条（id=6 洗头）；组合条件正确 |
| A5 | 分页 | `pageSize=5` → total=9、rows=5；`pageNum=2` → rows=4。**注**：PageHelper 全局 `reasonable=true`，超界页（如 page3）会返回末页数据（实测 page3/page4 返回 ids 6-9），属框架默认行为、**非本模块缺陷**；tester 用例不要断言“超界页 rows=0” |
| A6 | 详情 | `GET /nursing/project/1` → data.id=1、name=修剪指甲、price=30.00、image 为 OSS URL |
| A7 | 新增 | POST `{name:"测试项目S5",orderNo:10,unit:"次",price:9.9,status:1,nursingRequirement:"无",image:"<url>"}` → code=200；列表 total=10；**新记录 id 以实际自增为准（示例值 10，不硬编码断言）**；DB 中 create_by/create_time 有值 |
| A8 | 重名 | 再用 `name:"测试项目S5"` 新增 → code=500/msg“护理项目名称已存在”，**非 500 堆栈**；列表仍 10 条 |
| A9 | 修改全字段 | PUT 用 A7 实际返回的新增 id 改 unit/price/nursingRequirement → 200；详情字段更新，其它未传字段保持不变 |
| A10 | **启停部分更新（关键）** | PUT `{"id":<A7新增id>,"status":0}` → 200；详情 status=0 且 **name/unit/price/image/orderNo 未被清空**；列表状态标签“禁用” |
| A11 | 删除 | DELETE `/nursing/project/{A7新增id}` → 200；列表 total 回 9；DB 确认已删（测试数据清理）。**UI 只验单删；批量删除 API 可选、不作强制** |
| A12 | all 下拉 | `GET /nursing/project/all` → `code=200,data.length=9`，每项 `{label:string,value:string}`，value 为 id 字符串，顺序 order_no,id；plan 页“护理项目”下拉可显示 9 项 |
| A13 | 权限（正向） | **本轮仅验 admin 正向全通**；负向权限（无 `nursing:project:*` 角色 403）**本轮 N/A**，不作为验收项 |
| A14 | 前端页面 | 打开 `/nursing/project`：列表 9 条、**表头 10 列（9 数据列 + 操作列）**、图片缩略图正常、新增/编辑/删除/启停按前端既有行为工作；**前端代码未改** |
| A15 | **图片上传本地兜底（D-Web-S5-01）** | `POST /common/upload`（带 Bearer，multipart 一张小图）→ `code=200` 且 `url` 非空、为 `http://localhost:8080/profile/upload/...`；浏览器直接打开该 URL 可见图片；前端新增/编辑弹窗 image-upload 回填成功、不报错 |
| A16 | 列表/弹窗图片显示 | 列表“图片”列缩略图正常；打开新增/编辑弹窗图片区正常；已有 9 条 OSS URL 与本地兜底 URL 均可显示（两种来源共存） |

**通过口径**：A1–A14 全过 + A15 按 lead 决策记录。

---

## 6. 风险与待决策

| # | 风险 | 影响 | 处理 |
|---|---|---|---|
| R1 | ~~OSS 环境变量未设置导致上传失败~~ **已决策（lead：选项 c 本地兜底，D-Web-S5-01）** | 图片上传验收 | 只改 `CommonController` `/common/upload`：OSS 优先 + key/secret 为空或异常时本地兜底；保留 OSS 正常时原行为；已写入 §3.6 与验收 A15/A16 |
| R2 | `name` 有 DB 唯一索引，重名插入会抛约束异常 | 不友好 500 | service 预校验 + catch DuplicateKeyException（已写入任务） |
| R3 | 前端启停只传 `{id,status}`，若用全量 update 会清空字段 | 数据被覆盖 | 强制 `updateById` / XML `<if>`（已写入验收 A10） |
| R4 | plan 页依赖 `/nursing/project/all`，本接口上线后 plan 页下拉恢复，但 plan 列表仍 404 | 护理计划页仍部分不可用 | 已知；本 Sprint 不修 plan 后端，验收仅看下拉 |
| R5 | 后端编译触发 devtools 热重启，扰动他人联调 | 环境抖动 | dev1 编译前后各向 web-control 报备一句（规则 §2.2） |
| R6 | 主人 IDEA 8080 实例 PID 23192 不可擅动 | 违规 | 需重启窗口时按规则正式申请；涉及主人实例先报 lead |

---

## 7. 角色分工与时间盒

| 角色 | 动作 | 时间盒 |
|---|---|---|
| web-dev1 | 按本任务单实现 4 个新文件 + XML 补排序；自测编译；完成后交 review | 60~90 分钟 |
| web-arch（本人） | 收到完成通知后 **10 分钟内** review（清单见 §8），给 PASS/打回结论并上报 lead | ≤10 分钟 |
| web-tester | review 通过后按 §5 验收（需 web-control 申请 8080 稳定窗口 + 浏览器 + MySQL 写） | 20~30 分钟 |
| web-control | 编译轻量报备登记；联调窗口/浏览器/写库按 FIFO 批准；结束后核查 | 全程 |

---

## 8. web-arch 代码评审清单（review 用｜2026-11-25 已全部核对 ✅）

- [x] 仅改动约定文件；前端 `git diff` 为空；未改配置/表/其它模块
- [x] **`CommonController` 仅改 `/common/upload` 单文件方法**：OSS 优先逻辑保留；key/secret 为空或 OSS 异常时本地兜底；`/common/uploads` 未动；兜底失败返回 error 不抛未捕获异常
- [x] 兜底 URL = `serverConfig.getUrl() + FileUploadUtils.upload(RuoYiConfig.getUploadPath(), file)`；响应 url/fileName/newFileName/originalFilename 语义不变（D-Web-S5-01）
- [x] 4 个新文件齐全，包名/路径正确；Mapper 接口与 XML namespace 一致
- [x] Controller 6 接口路径/方法/参数与 §3.2 一致；`extends BaseController`
- [x] 5 个权限注解正确（list/query/add/edit/remove）；写操作有 `@Log`
- [x] list：`startPage()+getDataTable`；XML 末尾 `order by order_no, id`；name like / status eq
- [x] 启停：`updateById`（null 不覆盖）；id 不存在返回 error
- [x] 唯一名：新增/修改（排除自身）校验；DuplicateKeyException 兜底；中文提示
- [x] all：返回 `List<NursingProjectVo>`，`value=String(id)`、`label=name`，排序一致
- [x] 删除单 id、不存在返回 error；新增 status 默认 1；image 空串归一 null
- [x] 无新增依赖、无 SQL 注入拼接、无全表 update/delete
- [x] 编译通过（dev1 已报备 web-control）

---

## 9. 交付物

1. 代码：后端 4 个新文件 + `NursingProjectMapper.xml` 排序修改 + `CommonController` 上传兜底（dev1）
2. 本任务单（web-arch）
3. review 结论 + 验收记录（web-arch / web-tester → lead）

---

## 10. Review 结论（web-arch，2026-11-25 09:20）

**结论：✅ PASS（通过，无阻塞项）** → 允许进入 web-tester 验收（A1–A16）。

| # | 审查项 | 结果（只读核验） |
|---|---|---|
| ① | 变更范围 | `git diff --stat`：仅 `CommonController.java`（+40/−8）与 `NursingProjectMapper.xml`（+1 排序行，另补 EOF 换行）；前端代码改动 **0**；未改 `application.yml`/库结构；4 个新文件为 untracked（待 lead 统一提交时 git add） |
| ② | XML 未破原 SQL | 仅补 `order by order_no asc, id asc`；原 resultMap / select / insert / update / delete 模板全部保留；namespace 一致 |
| ③ | 部分更新语义 | ServiceImpl 使用 `updateById`（MP 默认 null 不覆盖），**无 select 整对象再 update**；dev1 自测 A10：PUT `{id,status}` 后 DB+详情双验 name/unit/price/image/orderNo/requirement 均未变 |
| ④ | 唯一名 | trim + 必填 + ≤100；新增/修改（排除自身 id）预校验 + `DuplicateKeyException` 兜底 + 中文提示；dev1 边界自测通过 |
| ⑤ | `/all` | VO `label=name`、`value=String(id)`；`orderByAsc(orderNo, id)`；dev1 实测 9 条、顺序 1..9、value 全为 string |
| ⑥ | 权限与日志 | 5 权限点齐全（list/query/add/edit/remove）；写操作 `@Log(INSERT/UPDATE/DELETE)`；list `startPage()+getDataTable` |
| ⑦ | 上传兜底 | `CommonController` 仅 `/upload` + 私有 `uploadToOssOrLocal`；OSS key/secret 非空优先、失败/未配置本地兜底（`FileUploadUtils.upload` + `serverConfig.getUrl()`）；响应 4 字段不变；`/common/uploads` 未动 |
| ⑧ | 风格/安全 | 包结构、RuoYi 范式与 `RoomType*` 一致；无新增依赖；无 SQL 拼接/全表 update/delete；事务无必要（单表单语句） |
| ⑨ | 编译/自测 | dev1 `mvn -DskipTests compile` BUILD SUCCESS ×2（web-control 已登记轻量报备，devtools 热重启 2 次后 8080 恢复，主人 PID 23192 未动）；A3–A13、A15 自测证据完整；测试数据已清理（DB count=9、AUTO_INCREMENT=10） |

**非阻塞观察（不要求本轮修改）**
- N1：XML 中 insert/update/delete 模板保留但未使用（CRUD 走 MP），保留无害，后续可清理；
- N2：分页超界按 PageHelper `reasonable=true` 返回末页（已写入 A5 注，tester 已同步，**非缺陷**）；
- N3：`getInfo` 不存在 id 返回 `success(null)`（前端不依赖，可接受）；
- N4：未 commit（lead 统一提交）；4 个新文件目前 untracked，提交时需 `git add`。

**结论生效条件**：web-tester 按 A1–A16 实测通过（A13 仅 admin 正向；批量删除 N/A；A15/A16 图片上传必测）；tester 联调须按 web-control 规则申请 8080 稳定窗口 + 浏览器 + MySQL 写，测试数据用后清理并回填。若实测出现与本 review 证据冲突，回流 dev1 修复后再 review。

---

## 11. Triage 记录（REG-06 / UI-GUARD-02，web-arch 2026-11-25）

> 背景：tester UI 首轮出现 2 个 FAIL 截图（`48_FAIL_REG-06.png`、`49_FAIL_UI-GUARD-02.png`）。经只读核查 tester 产物（`artifacts/test_web_s5/ui/ui_results_*.json`、截图、console log）后裁定如下。

### 11.1 REG-06（服务管理其它页可打开：plan/level/oldPeople）→ **判定口径问题，非本轮缺陷；重判 PASS**

| 项 | 事实 |
|---|---|
| tester 判定依据 | “页面未渲染”：`{"plan":{"rendered":true},"level":{"rendered":true},"oldPeople":{"rendered":false}}`（最新 run 又显示 oldPeople `bodyTextLen=146`，疑似按文本长度阈值判渲染） |
| 截图证据 | `48/49_FAIL` 截图实为**同一张**：`负责老人` 页面已完整渲染——面包屑“首页/服务管理/负责老人”、楼层 Tab（4楼/1楼）、“403”房间卡、“床位号 403-1/老人姓名 魏忠语/护理员姓名 小青”、“批量设置护理员”，右侧菜单选中“负责老人” |
| 结论 | **oldPeople 页面正常渲染**；FAIL 系 tester 渲染判定启发式（文本长度阈值/等待时机）误判为 false negative |
| plan/level | 页面壳已渲染（rendered=true）；其接口 404 属**既有状态**（缺 `NursingPlanController/NursingLevelController`），在测试计划中已注明“与本次无关、记录”，**不得标 FAIL** |
| 处置 | ① REG-06 重判 **PASS**（plan/level 记录为 Known Issue；oldPeople 正常）；② 修正 tester 判定口径：渲染判定改为“关键 DOM 元素存在性”（如 `.headBox/.houseList/.bedList`、面包屑、Tab 或内容卡），并增加等待（networkidle/关键元素 visible），**禁止用 bodyTextLen 阈值单独判定** |

### 11.2 UI-GUARD-02（护理项目请求均走 /dev-api 真实后端）→ **口径问题，最新 run 已 PASS；无需改代码**

| 项 | 事实 |
|---|---|
| 旧 run 失败证据 | 被判定“非 /dev-api 请求”的实为：`http://localhost:88/src/views/nursing/project/index.vue`、`.../src/api/nursing/project.js`、`http://localhost:88/nursing/project` —— 均为 **Vite 源码模块请求 / 页面导航**，不是后端 API 请求 |
| 最新 run 结果 | `ui_results_latest.json`：**UI-GUARD-02 = PASS** —— `apiRequests=43`，方法覆盖 GET/POST/PUT/DELETE，状态全 200；并已正确排除“前端源码模块请求(非API)=29” |
| 真实链路 | 前端 axios `baseURL=/dev-api` → Vite proxy `/dev-api` → `http://localhost:8080`（`vite.config.js` rewrite 去掉前缀）；护理项目 6 接口实测均返回真实后端 JSON（API 47/47 PASS） |
| 处置 | 判定口径固定为：**只看 `resourceType ∈ {xhr, fetch}` 且 path 匹配 `^/dev-api/nursing/project/` 的请求**；排除 `/src/*`、`/@vite/*`、`.vue/.js` 模块与页面导航；UI-GUARD-02 以最新 run PASS 为准，旧截图归档为“脚本误报” |

### 11.3 结论

- **无真缺陷、无需 dev1 返修**；两项均为 tester 判定口径/脚本伪失败。
- 要求 tester：按 §11.1/§11.2 修正判定逻辑后**重生成最终结果**（REG-06→PASS；UI-GUARD-02 保持 PASS），并在报告中注明“首轮 2 FAIL 为判定口径问题，已修正”；最终 UI 结果应为 **49/49 PASS**（或 48/49→49/49，以重跑为准）。
