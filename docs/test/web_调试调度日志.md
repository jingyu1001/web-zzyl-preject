# Web 端 · 调试调度日志（web-control）

> **调度对象**：Web 团队共享环境 —— 前端 88 / 后端 8080 / 浏览器自动化 / MySQL 写。
> 只对上述排他资源排队；构建、静态检查、只读分析免申请（例外：后端编译会触发 devtools 热重启，需轻量报备）。
> **调度员**：web-control（Web 调试子控制中心）｜**日志建立**：2026-11-25 08:47｜**最后更新**：2026-11-25 15:18（**第 2 时段测试已收尾**：Sprint 5b 护理计划；DQ-0004/0005/0006 全部释放；W1 API **48/48** + W2 UI **36/36** PASS；违规 0）
> **配套规则**：`docs/tasks/Web调试调度规则.md`（v1.1，完整协议）｜与 C++ 端 cpp-control 协议对齐。

---

## 一、调度规则摘要

1. **申请**：`申请者 | 目的 | 需要资源(前端88/后端8080/浏览器/MySQL写) | 预计时长(≤15分钟) | 影响范围 | 是否涉及重启`（send_message 给 web-control）。
2. **FIFO**：按提交时间排队；同一资源同一时刻只批 1 人；不插队。
3. **时长**：默认 10 分钟；有人排队时 8 分钟；上限 15 分钟。回执含编号 `DQ-YYYYMMDD-NNNN` 与到期时间。
4. **释放**：结束立即发「释放」（完成/部分完成/取消 + 产物 + 残留进程）。
5. **超时**：到期前 2 分钟提醒；超时 2 分钟记录并上报 lead、资源交下一位。
6. **违规**：未申请占端口/起服务、越权重启主人服务、按名批量杀进程 → 记 `V-NN` 并上报 lead。
7. **免申请**：`npm run build`、`mvn -DskipTests compile`、lint、只读代码/日志分析。但**后端编译会触发 8080 devtools 热重启（约 3–15 秒）**，编译前后各报备一句。
8. **主人服务**：88/8080/3306/6379 默认不重启不停止；重启默认由**申请人本人**执行，涉及主人 IDEA 实例（PID 35760/23192）须先报 **lead** 批准。web-control 只仲裁与核查，不写代码、不 commit、不 triage。

---

## 二、当前状态（2026-11-25 09:05 · **Sprint 5 调度进行中**）

- **调度状态**：值守中 —— **第 2 时段（下午场）已恢复调度**；任务：**「护理计划」模块 CRUD 后端**（**前端零改动**），web-arch 出任务单 → web-dev1 实现 → web-tester 验收
- **公告状态**：✅ 14:55 已向 web-arch / web-dev1 / web-dev2 / web-dev3 / web-tester 发下午场调度公告（规则 v1.1 + 本时段资源口径 + 基线）
- **上午场尾账**：编译 #1/#2 已闭环；DQ-0001/0002/0003 全部释放；V-01 已记录（09:25:38 IDEA 增量编译未报备 → devtools 重启 9s，窗口外无影响）
- **下午场资源预期**：① dev1 编译 = 轻量报备；② dev1 自测 MySQL 写（`nursing_plan` / `nursing_project_plan` 测试数据，**基线 3/6 行**，结束清理恢复）；③ tester 浏览器自动化（**分段批准**，连续测试优先续批）+ 可能 MySQL 写；DQ 编号续 **DQ-20261125-0004** 起
- **当前占用（DQ）**：**无** —— DQ-20261125-0006（tester W2）已于 **15:18 释放闭环**（UI 36/36 PASS，校验和与基线一致）；**第 2 时段 DQ-0004/0005/0006 全部关闭**
- **排队队列**：**0 人**（tester 连续测试结束，已恢复普通 FIFO）
- **前端 88**：运行中 —— `node.exe`（vite.js）PID **35760**（与上午一致）；**本时段不需重启**（前端零改动）；默认不动
- **后端 8080**：运行中 —— `java.exe`（`com.zzyl.RuoYiApplication`，IDEA 启动）PID **23192**（W2 期间 PID 未变；**09:25:38 IDEA 增量编译触发 devtools 自动重启，09:26:01 已恢复**，见 V-01）；**主动重启须申请，且不得停/重启此主人 IDEA 实例除非 lead 批准**
- **MySQL 3306**：运行中 —— `mysqld.exe` PID **16152**；写窗口已释放（W2 核验 `nursing_plan=3` / `nursing_project_plan=6`、`ZZT5B` 残留 0、CHECKSUM 与基线一致）
- **Redis 6379**：运行中 —— `redis-server.exe` PID **35808**；默认不动
- **浏览器自动化**：**空闲** —— W2 结束核验：msedgedriver 0 / headless Edge 0 / 自动化 node 0（msedge 常规 16 = 基线，主人/DSH 窗口）
- **下午场基线核查（14:53，只读）**：88→**35760** / 8080→**23192** / 3306→**16152** / 6379→**35808**，**四端口 PID 与上午基线完全一致**；浏览器自动化 **0 进程**（msedge 18 个常规窗口，主人/DSH 使用，非自动化）；MySQL `nursing_plan=**3**` / `nursing_project_plan=**6**` / `nursing_project=**9**`（与 lead 交底一致）
- **下一动作**：第 2 时段测试已收尾（摘要见 §七.2）；等 lead / web-arch 后续指令与新申请；空闲不高频汇报

---

## 三、第 0 时段就位记录（2026-11-25 08:45–08:49）

### 3.1 就位动作

| 时间 | 动作 |
|---|---|
| 08:45 | web-control 由 lead 创建启动；开始只读核查 Web 端共享环境 |
| 08:45–08:47 | 只读核查 88/8080/3306/6379 监听状态、占用进程名/PID/启动时间/命令行；核查浏览器自动化进程与全机 LISTEN 快照 |
| 08:47 | 建立 `docs/test`、`docs/tasks` 目录（原工程仅有 `docs/architecture`） |
| 08:47 | 收到 lead 补充指令：今早只做就位、**暂不向团队发公告**、发完简报后静默待命 |
| 08:48 | 写入 `docs/tasks/Web调试调度规则.md`（v1.0） |
| 08:48 | 写入本日志（第 0 时段就位记录） |
| 08:49 | 向 lead 发就位简报；lead 回复**口径全部认可**（详见 §3.6），转入静默待命 |

### 3.2 端口与进程核查表（只读，核查时间 2026-11-25 08:45–08:47）

| 端口 | 监听地址 | PID | 进程名 | 启动时间 | 命令行摘要 | 归属判断 |
|---|---|---|---|---|---|---|
| **88** | `::` | 35760 | node.exe | 2026-11-24 08:35:48 | `node ...\zznursing-vue3-master\node_modules\.bin\..\vite\bin\vite.js` | 前端 Vite dev server（R1）；主人/上一轮常驻 |
| **8080** | `::` | 23192 | java.exe | 2026-11-24 14:33:30 | `...\.jdks\ms-21.0.9\bin\java.exe ... -javaagent:...idea_rt.jar=60926 ... com.zzyl.RuoYiApplication`（IDEA 启动，另监听 35729/60930） | 后端 Spring Boot（R2）；主人/上一轮常驻，带 devtools |
| **3306** | `127.0.0.1` | 16152 | mysqld.exe | 2026-11-21 02:35:46 | `C:\mysql\mysql-8.0.44\bin\mysqld.exe --defaults-file=C:\mysql\my.ini` | 本机 MySQL 便携版（R4）；主人环境 |
| **6379** | `0.0.0.0` / `::` | 35808 | redis-server.exe | 2026-11-23 16:32:08 | `C:\Users\ASUS\Desktop\soft\Redis-x64-3.2.100\redis-server.exe` | 本机 Redis；主人环境 |

**关联核查**：
- 目标端口均**只有一个实例**（88 唯一 35760；8080 唯一 23192），未发现多开。
- `java.exe` PID 19544（08:42:56）/ PID 22776（08:55:38）为 IntelliJ IDEA 内部 JVM（`-Xmx700m -Djava.awt.headless=true` + jna/idea 参数），**不监听目标端口**，不属调度资源。
- 其余 `node.exe`（PID 21216 / 30416 / 24892 / 38668）均未监听 88，非 Vite 实例。
- 其它监听端口（60058 = DeepSeek Harness Web GUI、63342 = IDEA、35729/60930 = 8080 的 devtools/JMX、9877 等本地工具端口）**不属于 Web 调试资源**，不纳入调度。

### 3.3 浏览器自动化核查

| 项 | 结果 |
|---|---|
| Edge / Chrome 进程 | `msedge.exe` ×18、`msedgewebview2.exe` ×30（含 DeepSeek Harness Web GUI 的 WebView）——**常规窗口，非自动化** |
| 自动化关键字进程（headless / playwright / puppeteer / webdriver / --remote-debugging-port） | **无**（仅命中 IDEA 内部 JVM 的 `headless` 参数与 DSH/powershell 自身，非浏览器自动化） |
| 结论 | **浏览器自动化资源空闲，当前无人占用** |

### 3.4 核查结论

1. **四个端口全部有服务在跑**，启动时间均早于本调度中心建立（08:45），属**主人常驻服务 / 上一轮遗留**，不是今日有人未申请抢资源。
2. **未发现"未申请就占端口/起服务"迹象**：88/8080 各仅一个实例且命令行指向本工程；无多余 mvn/java 后端实例。
3. 就位时环境状态：**四端口健康、浏览器自动化空闲、MySQL 写空闲、队列 0、无违规**。
4. **口径确认**：今早（lead 08:47 指令）只做就位，暂不打扰团队；Web Sprint 5 任务下发后再统一公告（规则 §八）并进入正式调度。

### 3.5 给团队的注意事项（已随 08:55 公告发出）

- 88/8080/3306/6379 是主人常驻服务，**任何人不得私自重启/停止**；重启默认由申请人本人执行，涉及主人 IDEA 实例（PID 35760/23192）须先报 lead 批准。
- 后端 8080 带 `spring-boot-devtools`：**后端编译会触发自动热重启**（8080 约 3–15 秒不可用），编译前后请报备；要长时间稳定测试请正式申请窗口。
- 前端改源码有 HMR，**免申请**；但 `npm install`/改 vite 配置会重启 88，需申请。
- 浏览器自动化/截图、MySQL 写必须申请，**FIFO**，默认 10 分钟、有人排队 8 分钟。
- 结束立即发「释放」；进程清理只按 **PID+路径** 确认，严禁按进程名批量杀。

### 3.6 lead 口径确认（2026-11-25 08:49）

lead 已确认「就位简报」并**全部认可**，按规则 v1.0/v1.1 执行：

1. **8080 devtools 热重启**：后端编译/IDEA Build 采用「轻量报备（不占队列）」；只有「需要长时间稳定窗口 / 主动重启 / 前后端联动验收」才正式申请排他窗口。
2. **88/8080 重启执行人**：默认由**申请人本人**执行（如 dev1 改完后端代码自己重启并验证），web-control 登记窗口 + 核查结果；若涉及主人 IDEA 稳定实例（**88 PID 35760 / 8080 PID 23192**）的重启/停止，**必须先报 lead 批准**。
3. **默认时长**：维持 10 分钟（有人排队 8 分钟、上限 15 分钟）。
4. **值守节奏**：今天上午待命（等老师下发 Web 功能），C++ 端 9:00–12:00 不开发；期间无需主动汇报，收到任务/申请再启动调度。规则与日志随 Web 文档一起提交仓库。

### 3.7 第 1 时段开工记录（Sprint 5 Web · 2026-11-25 08:54 起）

- **08:54** 收到 lead 总控通知：**Web Sprint 5 正式开工（09:00）**。
  - 任务：实现「护理项目」模块 CRUD **后端**（**不改前端页面**）；web-dev1 实现 → web-arch review → web-tester 测试。
  - 资源预期：① 后端编译（`mvn compile`，devtools 自动热重启 8080）= **轻量报备**；② 主动重启后端 = **正式申请窗口**，**不得停/重启主人 IDEA 稳定实例（PID 23192）除非 lead 批准**；③ web-tester 需**浏览器自动化窗口**（UI 测试）+ 可能的 **MySQL 写测试数据窗口**（测试后清理/恢复并记录影响）；④ 前端 88 **不需要重启**（前端代码不改，Vite HMR 无关）。
- **08:54 开工基线核查（只读）**：四端口与 PID 均未变化（88→35760、8080→23192、3306→16152、6379→35808），浏览器自动化空闲，队列 0，无违规。
- **08:55** 向 **web-arch / web-dev1 / web-dev2 / web-dev3 / web-tester** 发出调度公告（规则 v1.1 + 申请模板 + 日志路径 + 今日资源口径）。
- **调度口径**：FIFO 与「同资源同刻仅 1 人」；空闲不高频汇报，仅 lead 询问 / 申请处理完 / 发现违规时汇报。
- **边界**：C++ 端今日不开发、cpp-control 不活跃；本项目只调度 Web 侧资源，不互相越权。
- 本时段 DQ 记录见 §四（首条编号 `DQ-20261125-0001`）。

### 3.8 轻量报备登记（不占队列、不占编号；连续时段）

| 时间 | 申请者 | 内容 | 影响资源 | 预计/实际影响 | 状态 |
|---|---|---|---|---|---|
| 08:58–08:59 | web-dev1 | **编译 #1**：护理项目 CRUD 后端代码写完（4 个新 Java 文件 + `NursingProjectMapper.xml` 补 order by）→ `mvn -DskipTests compile` | 后端 8080（devtools 热重启） | **实测 BUILD SUCCESS，总耗时 1:08，10 个模块全 SUCCESS**（zzyl-nursing-platform 131 源文件无错误）；devtools 重启 08:59:29 起、08:59:44 Started，**8080 实际中断 ≈15s**；PID 23192 未变；8080/35729/60930 已恢复 LISTEN；`sys-error.log` 无本次新增错误 | ✅ 完成（09:00 核查通过） |
| 09:00–09:02 | web-dev1 | **编译 #2**：`CommonController.java` 的 `/common/upload` 加「OSS 优先 + 本地兜底」（`/common/uploads` 未动，lead/web-arch 决策）→ `mvn -DskipTests compile` | 后端 8080（devtools 热重启） | **实测 BUILD SUCCESS 1:08，10 模块全 SUCCESS**（zzyl-admin 重编 24 源文件，含 CommonController）；devtools 重启至 **09:02:00**（`Started in 11.389s`），**8080 中断 ≈11s** 后恢复；PID 23192 未变；35729 恢复；`sys-error.log` 无新增错误 | ✅ 完成（09:02 核查通过） |
| 14:58–14:59 | web-dev1 | **编译 #3（下午场 Sprint 5b）**：护理计划 CRUD 后端代码写完（4 个新 Java 文件 + `NursingPlanMapper.xml` 补 count 映射 / list 子查询与排序）→ `mvn -DskipTests compile` | 后端 8080（devtools 热重启） | **实测 BUILD SUCCESS 53.7s，10 模块全 SUCCESS**（zzyl-nursing-platform 含 4 个新护理计划类，无错误）。devtools 触发**双重重启**：第 1 次 **14:59:06 启动失败**（编译中间态 class 缺失 → `NursingProjectMapper` bean 不可用，`Application run failed`）、第 2 次 **14:59:22 成功**（`Started in 14.32s`）；8080 中断约 **16s**（14:59:06–14:59:22，因双重启略超 3–15s 常态，**自愈、无人工干预**）；PID **23192 未变**；14:59:45 复测 `GET /captchaImage` **HTTP 200**、14:59 后无新增 error | ✅ 完成（备注：编译期 devtools 双重启竞态自愈，属已知风险、非违规） |

**核查记录**：
- 编译 #1 前基线（08:58）：8080 LISTEN，PID 23192，启动时间 2026-11-24 14:33:30 未变；无 DQ 占用、无排队 → 不打断任何窗口。结束后（09:00）：8080/35729/60930 恢复，`Started RuoYiApplication in 16.062 seconds`，`sys-error.log` mtime 仍 08:44:05（无新增）。
- 编译 #2 前基线（09:00:49）：8080 LISTEN 正常、PID 23192 未变、唯一 RuoYi 实例、mvn 进程 0。结束后（09:02）：8080/35729 恢复，`Started in 11.389s`，`sys-error.log` 仍无新增错误。
- 说明：两次 devtools 热重启均**未改变 JVM PID（23192）**，属同进程内 ApplicationContext 重启，符合轻量报备预期，无异常。

### 3.9 第 2 时段（下午场 · Sprint 5b「护理计划」CRUD）开记（2026-11-25 14:53 起）

- **14:53** lead 通知：**下午场任务开工** —— 实现「护理计划」模块 CRUD **后端**（**前端零改动**）；web-arch 出任务单 → web-dev1 实现 → web-tester 验收；web-control 恢复值守。预计资源：① dev1 编译（轻量报备）；② dev1 自测 MySQL 写（`nursing_plan` / `nursing_project_plan` 测试数据，**基线 3/6 行**，结束清理恢复）；③ tester 浏览器自动化窗口（**分段批准**）+ 可能 MySQL 写；DQ 编号续 **DQ-20261125-0004** 起。
- **14:53 第 2 时段基线核查（只读）**：88→**35760**、8080→**23192**、3306→**16152**、6379→**35808**，**四端口 PID 与上午基线完全一致**；浏览器自动化 **0 进程**（msedge 18 个常规窗口为主人/DSH，非自动化；较上午 +1 属用户正常使用）；MySQL 基线 `nursing_plan=**3**`、`nursing_project_plan=**6**`、`nursing_project=**9**`（与 lead 交底一致）。
- **14:55** 向 web-arch / web-dev1 / web-dev2 / web-dev3 / web-tester 发**第 2 时段调度公告**（规则 v1.1 + 资源口径 + 基线 + DQ 续号 + 上午 V-01 提醒）。
- **14:55** tester 回执（登记）：① 上午 `/nursing/plan/all` 404 结论为**预期 Known Issue** —— `NursingPlanController` 缺失，属 Sprint 5b 待实现 R4，**非 Sprint 5a 缺陷**；09:24:47 那条是 REG-06 访问 plan/level 页时页面自身请求的应用行为；已写入 `docs/test/Web_Sprint5_护理项目_测试报告.md` §6.3；② V-01 与 tester 无关（09:25 已释放，窗口外事件，5a 报告未提及、无冲突）；③ 5a 报告/证据已归档 `docs/test/Web_Sprint5_护理项目_测试报告.md`、`artifacts/test_web_s5/`；④ **下午 Sprint 5b 计划**：等 arch 任务单 + dev1 完成 + review PASS 后按模板申请 **W1（API+MySQL 写）** 与 **W2（浏览器+MySQL 写）**，会提前告知；预计仍需 **Redis 只读 `GET captcha_codes`**（同上午口径，已预报备）。
- **调度口径**：维持 FIFO +「同资源同刻仅 1 人」；tester **分段批准（每段 ≤15 分钟）+ 段间优先续批**（lead 上午授权延续）；MySQL 写须基线/影响行数/回滚 + 结束核对；**后端编译须轻量报备**（吸取 V-01 教训）。

---

## 四、调度记录（DQ）

| 编号 | 申请者 | 资源 | 目的 | 申请时间 | 批准时间 | 到期时间 | 释放时间 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|---|---|
| **DQ-20261125-0001** | web-dev1 | MySQL 写（`zzyl.nursing_project`） | Sprint5 护理项目 CRUD 自测：① INSERT 测试行 1 条（name=测试项目S5-dev1）② 同名 INSERT 1 次（预期唯一索引报错、0 行）③ UPDATE 部分更新（启停只传 {id,status}）+ 全字段更新 ④ DELETE 1 次 | 2026-11-25 09:02 | 2026-11-25 09:03 | 2026-11-25 09:13 | **2026-11-25 09:05** | **已释放（完成，提前释放，未超时）** | 队列 0 → 按默认 10 分钟批准；实际窗口 09:04–09:05。**web-control 独立核验（09:05）**：`COUNT=9` ✅；`id>=10` 残留 0 ✅；种子行 `SEED_MD5=7e9f6e46dd5b425800f307bf4469f982` 与 dev1 声明**完全一致** ✅；上传测试文件已删除 ✅；8080 PID 23192 未动、RuoYi 实例 1、mvn 进程 0、无残留进程 ✅。**实际影响行数**：INSERT 1（id=10）/ 同名 INSERT 0（唯一索引拦截，中文错误「护理项目名称已存在」，未落库）/ UPDATE 2 次 2 行（① `{id,status}` 部分更新 1→0，其余字段未被覆盖 ② 全字段更新）/ DELETE 1（id=10）。**申报副作用**：登录 1 次写 `sys_logininfor`、`@Log` 写 `sys_oper_log`（应用自动行为，未清理，按规则申报）→ 属正常行为，记录备查、不计违规。上传自测：`POST /common/upload` 200、URL `http://127.0.0.1:8080/profile/upload/2026/11/25/s5_upload_test_20261125090418A001.png`、GET 200 image/png、本地文件已删。 |
| **DQ-20261125-0002** | web-tester | 后端 8080 稳定访问 + MySQL 写（`zzyl.nursing_project`） | Sprint5 护理项目验收 **W1（API 层 A1–A16）**：列表/搜索/分页/详情/新增/重名/修改/启停部分更新/删除/all/上传本地兜底/admin 权限/既有 API 回归 + 测试数据清理验证 | 2026-11-25 09:07 | 2026-11-25 09:08 | 2026-11-25 09:18 | **2026-11-25 09:14** | **已释放（完成，提前释放，API 47/47 PASS）** | 实际窗口 09:08–09:13。**申报实际影响**：nursing_project INSERT 5 / UPDATE 4 次 / DELETE 5，净增 0；收测 COUNT=9、`ZZT5` 残留 0、MAX(id)=9；9 条种子逐行比对无变化（tester 收测种子哈希 `4a6dfdb56c4edc1040b6ce30ccdb8f66`，查询式见测试报告）。**web-control 独立核验（09:14，只读）**：`COUNT=9` ✅、`ZZT5` 残留 **0** ✅、`id>=10` **0** ✅、`MAX(id)=9` ✅、种子 `SEED_MD5=7e9f6e46dd5b425800f307bf4469f982` **与 W1 前基线完全一致** ✅；产物 `artifacts/test_web_s5/api/`（`api_results_20261125_091303.json` 等 25 个文件）✅；8080 PID 23192 未动、RuoYi 实例 1、mvn 0、自动化浏览器 0（msedgedriver 0）✅。自动日志 `sys_logininfor` +5 / `sys_oper_log` 149（含 dev1 自测，应用行为，记录备查、不计违规）；无残留进程。 |
| **DQ-20261125-0003** | web-tester | 浏览器自动化（无头 Edge 系统 channel）+ 8080 只读 + MySQL 写（`zzyl.nursing_project`，仅 `ZZT5-*` ≤4 行） | Sprint5 护理项目 **W2 UI 浏览器验收**（A14/A16、前端零改动核对、plan 页 /all 下拉联动、房型·用户页回归 + 截图取证） | 2026-11-25 09:13 | 2026-11-25 09:14 | 2026-11-25 09:29 | **2026-11-25 09:26** | **已释放（完成，提前释放，UI 49/49 PASS）** | 实际窗口 09:14–09:25（提前 4 分钟）。**申报实际影响**：nursing_project INSERT 3 / UPDATE 3 次 / DELETE 3，净增 0；收测 COUNT=9、MAX(id)=9、id>9 行=0、`ZZT5` 残留 0；种子哈希与 W1 一致（`4a6dfdb56c4edc1040b6ce30ccdb8f66`，同查询式）；说明：lead 09:22 前后观察到的 `COUNT=12/MAX(id)=50` 属 UI 测试中间态（先建 3 行再清理，自增因多轮测试前移，正常副作用），最终已恢复。**web-control 独立核验（09:26，只读）**：`COUNT=9` ✅、`ZZT5` 残留 **0** ✅、`id>9` **0** ✅、`MAX(id)=9` ✅、`AUTO_INCREMENT=10`、种子 `SEED_MD5=7e9f6e46dd5b425800f307bf4469f982` **与基线完全一致** ✅；UI 产物 `artifacts/test_web_s5/ui/`（`ui_results_ui_20261125012234.json` + 60 张截图 + 日志，65 文件）✅；浏览器残留 **msedgedriver 0 / headless Edge 0 / 自动化 node 0**，msedge 常规 17 = 基线 ✅；8080 PID 23192 / 88 PID 35760 均未变 ✅。**Redis 只读报备**：`redis-cli GET captcha_codes:{uuid}` 共 3 次（09:15:25 / 09:19:42 / 09:22:40，只读不写不删，免申请）；W1 前 09:08 另有 1 次视觉识别验证码登录；未改后端/配置/验证码策略。**期间观察到** 09:24:47 后端 `sys-error` 出现 `NoResourceFoundException: /nursing/plan/all` 404（属应用行为/测试过程，web-control 不 triage，已提示由 tester/arch 报告结论）。**窗口外事件**：09:25:38 IDEA 增量编译触发 devtools 重启（09:26:01 恢复，8080 中断约 9s），发生在 tester 释放之后、未影响批准窗口 → 记 V-01。 |
| **DQ-20261125-0004** | web-dev1 | MySQL 写（`zzyl.nursing_plan` + `zzyl.nursing_project_plan`） | Task-S5b-01 护理计划 CRUD 自测：新增计划+子表 → 整表单修改（子表替换）→ 启停部分更新 → 删除清理 → 名称唯一/子表校验事务回滚验证 | 2026-11-25 14:59 | 2026-11-25 15:00 | 2026-11-25 15:10 | **2026-11-25 15:01** | **已释放（完成，提前释放，未超时）** | 实际窗口 15:00:00–15:00:35（约 35s，提前释放）。**申报实际影响**：`nursing_plan` INSERT 1（id=141）/ UPDATE 3 次 3 行（整表单 1、启停 1→0 1、恢复 1）/ DELETE 1（id=141）；`nursing_project_plan` INSERT 3（1752/1753 + 重建 1）/ DELETE 3；重名 / 子表非法 / 被引用拦截均 **0 行落库**；A8「启停种子 133」**按口径未执行**（种子只做只读拦截验证）。**web-control 独立核验（15:01，只读）**：`nursing_plan=3`、`nursing_project_plan=6` ✅；`plan_name LIKE 'ZZT5b%'` 残留 **0** ✅；主表 ids **133/134/135**、子表 ids 与基线完全一致（**1736/1737/1739/1740/1741/1743**）、id=141 不存在 ✅；**`CHECKSUM TABLE` = 2521898468 / 2554906448 与基线完全一致** ✅；8080 PID 23192 未动、HTTP 200、RuoYi 1、mvn 0 ✅。副作用 `sys_logininfor` 1 / `sys_oper_log` 若干（应用行为，记录备查、不计违规）；无残留进程。 |
| **DQ-20261125-0005** | web-tester | 后端 8080 稳定访问 + MySQL 写（`zzyl.nursing_plan` + `zzyl.nursing_project_plan`） | Sprint 5b 护理计划验收 **W1（API 层 A1–A15）**：列表/count/排序、搜索/分页、详情含 projectPlans、新增主+子事务、名称唯一、子表校验回滚、整表单修改（子表替换）、启停部分更新、删除级联、`/all`、**A8 被引用强制校验**、admin 权限、审计、安全 | 2026-11-25 15:02 | 2026-11-25 15:03 | **2026-11-25 15:13** | **2026-11-25 15:06** | **已释放（完成，提前释放，API 48/48 PASS）** | arch review PASS（15:15 前通报）后按 FIFO 优先批（队列 0 → 默认 10 分钟）。**基线（web-control 15:02 复核）**：`nursing_plan=3`（133/134/135）、`nursing_project_plan=6`；`CHECKSUM TABLE` = **2521898468 / 2554906448**；**种子 133 快照：status=1、update_time=NULL、create_time=2024-08-19 19:17:31**（用于 A8 恢复核验）。要求：仅 `ZZT5B-*` 测试行（plan INSERT ≤5 / UPDATE ≤6 / DELETE ≤5；project_plan INSERT ≤10 / DELETE ≤10，均净增 0）；种子 133/134/135 只读，**唯一例外 A8**：种子 133 status 临时 1→0→1、整表单改/删应被拒，验后按开测前快照 SQL 恢复 `status/update_time`（须在释放中回报快照与恢复证据）；不触碰表结构/配置/其它表；**不重启、不动 8080 PID 23192 / 88 PID 35760**；测试前先探测 8080 已加载新 Controller；产物 `artifacts/test_web_s5b/api/`；结束核对 counts **3/6**、`ZZT5B` 残留 0、**校验和与基线一致**、种子 133 status/update_time 已恢复；`sys_logininfor`/`sys_oper_log` 记录备查。W1 释放后 tester 立即申请 W2（浏览器+8080 只读+MySQL 写 15 分钟），队列空闲按段间优先授权**立即续批**；Redis 只读 `GET captcha_codes` 预报备沿用。**申报实际影响**：`nursing_plan` INSERT 2 / UPDATE 6（含 A8 种子 133 启停 2 次）/ DELETE 2，净增 0；`nursing_project_plan` INSERT 5 / DELETE 5，净增 0。**web-control 独立核验（15:06，只读）**：`nursing_plan=3` / `nursing_project_plan=6` ✅；`ZZT5B` 残留 **0** ✅；**种子 133 `status=1、update_time=NULL`（与开测快照一致）** ✅；`ORPHAN_PP=0`、子表 ids 与基线一致 ✅；**`CHECKSUM TABLE` 2521898468 / 2554906448 与基线完全一致** ✅；产物 `artifacts/test_web_s5b/api/` 18 文件 ✅；8080 PID 23192 未动 + HTTP 200、msedgedriver 0、mvn 0 ✅；副作用 `sys_logininfor`/`sys_oper_log` 记录备查、不计违规。 |
| **DQ-20261125-0006** | web-tester | 浏览器自动化（无头 Edge）+ 8080 只读 + MySQL 写（`nursing_plan` + `nursing_project_plan`，仅 `ZZT5B-*` ≤3 行） | Sprint 5b 护理计划 **W2 UI 浏览器验收**：A13 列表 3 行/5 列/count 灰置、搜索、1000px 弹窗+子表增删行/重复校验、新增 count=0 计划、修改回填+子表替换、查看只读、启停、删除、护理等级页 /all 下拉、护理项目/房型/用户回归 + 截图 | 2026-11-25 15:05 | 2026-11-25 15:06 | **2026-11-25 15:21** | **2026-11-25 15:18** | **已释放（完成，提前释放，UI 36/36 PASS）** | 按 lead 授权 W1 释放后队列 0 → **立即续批（段间优先，不打断 tester）**。资源基线：浏览器自动化 **0**（msedgedriver 0；msedge 常规 16 个属主人/DSH，非自动化）、8080 PID 23192 **只读**、MySQL 写空闲；MySQL 仅 `ZZT5B-*` ≤3 行、结束清理回 **3/6**、种子只读（本段不做 A8 类修改）；**不重启、不动 23192/35760**；结束须清理无头 Edge/驱动进程并回填影响行数 + 截图产物；Redis 只读 `GET captcha_codes` 沿用同意。**申报实际影响**：`nursing_plan` INSERT 1 / UPDATE 3 / DELETE 1，净增 0；`nursing_project_plan` INSERT 3 / DELETE 3，净增 0；**本段未修改种子**（种子 133 保持 status=1、update_time=NULL）。**web-control 独立核验（15:18，只读）**：`nursing_plan=3` / `nursing_project_plan=6` ✅；`ZZT5B` 残留 **0** ✅；孤儿子表 **0** ✅；**种子 133 未变（status=1/update_time=NULL）** ✅；**`CHECKSUM TABLE` 2521898468 / 2554906448 与基线完全一致** ✅；UI 产物 `artifacts/test_web_s5b/ui/` **55 文件含 51 张截图** ✅；浏览器残留 **msedgedriver 0 / headless Edge 0 / 自动化 node 0**、msedge 常规 16 = 基线 ✅；8080 PID **23192**（HTTP 200）/ 88 PID **35760** 未变、mvn 0 ✅。Redis 只读 `GET captcha_codes` **1 次**（15:15:28，只读不写不删，免申请）；副作用 sys_* 记录备查、不计违规。**W1+W2 合计 A1–A15 全过（API 48/48 + UI 36/36）**。 |

---

## 五、违规与异常记录（V）

| 编号 | 时间 | 主体 | 类别 | 事实 | 处理 |
|---|---|---|---|---|---|
| V-01 | 2026-11-25 09:25:38–09:26:01 | 未确认（IDEA 增量编译，**非 mvn**；无 agent 报备） | 未报备的后端编译触发 devtools 热重启（R2 轻量报备项缺失） | 09:25:38–09:25:41，IDEA 编译进程（java PID 38700，启动后已退出）增量更新 5 个 class：`zzyl-admin/.../CommonController.class`、`zzyl-nursing-platform/.../NursingProject{Mapper,Controller,ServiceImpl,Service}.class`；随后 devtools 约 **09:25:52** 自动重启、**09:26:01 `Started in 8.659s`**，8080 中断约 9 秒；**发生在 tester 释放（09:25）之后，未影响任何批准窗口**；当时无 mvn 进程、无 agent 发起报备，来源未确认（疑 IDEA 自动/手工 Build，可能来自主人或 arch/dev1） | 已记录并上报 lead；8080 已恢复 LISTEN（PID 23192 未变、无崩溃）；按规则「查不清留档不追责」，并提醒团队：**后端编译一律轻量报备（含 IDEA Build）**，需 IDEA 侧操作者配合；若再次出现未报备编译将升级处理；tester 14:55 回执确认该事件与其 5a 测试无关、报告未提及、无冲突。 |

---

## 六、事件时间线

| 时间 | 事件 |
|---|---|
| 2026-11-25 08:45 | web-control 就位，开始只读核查 Web 端共享环境（88/8080/3306/6379 + 浏览器自动化 + 进程归属）。 |
| 2026-11-25 08:45–08:47 | 核查完成：四端口均在跑、各仅一个实例、启动时间早于调度中心；无自动化浏览器进程；无未申请新起服务迹象。 |
| 2026-11-25 08:47 | 建立 `docs/test`、`docs/tasks` 目录。 |
| 2026-11-25 08:47 | lead 补充指令：今早只做就位、**暂不向 web-arch/dev1/2/3/tester 发公告**、发完简报静默待命。 |
| 2026-11-25 08:48 | 写入规则文档（v1.0）与本日志（第 0 时段就位记录 + 端口核查表 + 结论）。 |
| 2026-11-25 08:49 | 向 lead 发就位简报；lead 回复**全部认可**，规则升 v1.1；转入静默待命。 |
| 2026-11-25 08:54 | lead 通知：**Web Sprint 5 正式开工（09:00）**，任务=护理项目 CRUD 后端（不改前端）→ dev1 实现 / arch review / tester 测试；恢复值守。开工基线：四端口同 PID、自动化空闲、队列 0。 |
| 2026-11-25 08:55 | 向 web-arch / web-dev1 / web-dev2 / web-dev3 / web-tester 发出调度公告（规则 v1.1、申请模板、今日资源口径）。 |
| 2026-11-25 08:58 | dev1 **轻量报备（开始·编译 #1）**：护理项目 CRUD 后端 `mvn -DskipTests compile`；预告稍后可能申请 MySQL 写窗口。核查编译前 8080 基线正常、无冲突 → 登记 §3.8 并回执。 |
| 2026-11-25 08:59–09:00 | dev1 **轻量报备（结束·编译 #1）**：BUILD SUCCESS 1:08、10 模块全 SUCCESS。核查：devtools 重启 08:59:29→08:59:44（16.062s），8080 中断 ≈15s 后恢复、PID 23192 未变、无新增错误 → 闭环。 |
| 2026-11-25 09:00 | dev1 **轻量报备（开始·编译 #2）**：`CommonController.java` `/common/upload` 加 OSS 本地兜底（`/common/uploads` 未动）。核查前基线（09:00:49）8080 正常、无 DQ 占用、队列 0 → 无冲突，登记并回执。 |
| 2026-11-25 09:02 | dev1 **轻量报备（结束·编译 #2）**：BUILD SUCCESS 1:08、10 模块全 SUCCESS（zzyl-admin 重编 24 源文件）。核查：devtools 重启至 09:02:00（`Started in 11.389s`），8080 中断 ≈11s 后恢复、PID 23192 未变、无新增错误 → 闭环。 |
| 2026-11-25 09:02–09:03 | dev1 **正式申请 MySQL 写窗口**（`zzyl.nursing_project` CRUD 自测 8 分钟，含 SQL 摘要与回滚方案，不涉及重启）。只读核查 `COUNT=9`（与声明一致）、MySQL 写空闲、队列 0 → **批准 DQ-20261125-0001**，窗口 **09:03–09:13**。 |
| 2026-11-25 09:04–09:05 | dev1 **释放 DQ-20261125-0001**（完成，提前释放，实际窗口 09:04–09:05）。web-control 独立核验：`nursing_project` `COUNT=9`、`id>=10` 残留 0、**种子行 MD5 `7e9f6e46dd5b425800f307bf4469f982` 与 dev1 声明一致**、上传测试文件已删、8080 PID 23192 未动、无残留进程 → **DQ-0001 关闭（提前释放、未超时）**。申报副作用：`sys_logininfor` 1 条登录、`sys_oper_log` 若干操作日志（应用自动行为，未清理，记录备查、不计违规）。 |
| 2026-11-25 09:06–09:07 | **开测预告与 lead 授权**：lead 通知 web-arch 对护理项目 CRUD **Review PASS**，web-tester 将申请浏览器自动化窗口（预计 20–30 分钟，可能同期申请 MySQL 写）。web-control 记录**测试前基线（09:06:30，只读）**：88→35760 / 8080→23192 / 3306→16152 / 6379→35808 均 LISTEN 正常；浏览器自动化 **0 进程残留**（msedge 17 个常规窗口为主人/DSH，非自动化）；`nursing_project` **COUNT=9**、`SEED_MD5=7e9f6e46dd5b425800f307bf4469f982`。**lead 授权执行口径**：① tester 浏览器自动化**分段批准（每段 ≤15 分钟）**，段间队列 0 时立即续批；② tester 连续测试期间**段间续批优先于其他申请**（保证不被打断），tester 全部结束后恢复 FIFO；③ MySQL 写若申请，按基线/影响行数/回滚 + 结束核对执行；④ 8080/88 保持运行不动（PID 23192/35760）。**结束核对项**：`COUNT=9`、`SEED_MD5` 一致、浏览器自动化 0 残留、8080/88 PID 未变，并在日志记 Sprint 5 测试时段摘要。 |
| 2026-11-25 09:07–09:08 | web-tester **正式申请 W1**（Sprint5 护理项目验收 · API 层 A1–A16，10 分钟）：资源=**后端 8080 稳定访问 + MySQL 写**。MySQL 仅 `ZZT5-*` 测试行（INSERT ≤6 / UPDATE ≤5 / DELETE ≤6，净增 0 回 9 条；种子只读 + md5 比对）；`sys_logininfor`/`sys_oper_log` 自动新增少量记录（应用行为申报）；不重启、不动产 23192/35760；兜底 `DELETE FROM nursing_project WHERE name LIKE 'ZZT5-%'`；产物 `artifacts/test_web_s5/api/`。web-control 核查（09:08）：MySQL 写空闲、8080 LISTEN PID 23192 正常、队列 0、浏览器自动化空闲 → **批准 DQ-20261125-0002**，窗口 **09:08–09:18**（8080 稳定窗口内他人不得编译/重启后端）；已告知释放格式与 W2 续批安排。 |
| 2026-11-25 09:13–09:14 | web-tester **释放 DQ-20261125-0002**（完成，API **47/47 PASS**，提前释放，实际窗口 09:08–09:13；产物 `artifacts/test_web_s5/api/`）。web-control 独立核验（09:14）：`COUNT=9`、`ZZT5` 残留 **0**、`id>=10` 残留 **0**、`MAX(id)=9`、**种子 `SEED_MD5=7e9f6e46dd5b425800f307bf4469f982` 与 W1 前基线完全一致**、8080 PID 23192 未动、自动化浏览器 0 残留 → **W1 关闭（未超时）**。同时 tester **申请 W2**（浏览器自动化无头 Edge + 8080 只读 + MySQL 写 ≤4 行 `ZZT5-*`，15 分钟，UI 验收+截图；报备可能用 Redis 只读 GET 取验证码）→ 队列 0、浏览器自动化空闲 → **按 lead 优先授权立即批准 DQ-20261125-0003**，窗口 **09:14–09:29**。 |
| 2026-11-25 09:25–09:26 | web-tester **释放 DQ-20261125-0003**（完成，UI **49/49 PASS**，提前释放，实际窗口 09:14–09:25；UI 产物 `artifacts/test_web_s5/ui/` 65 文件含 60 张截图）。web-control 独立核验（09:26）：`COUNT=9`、`ZZT5` 残留 0、`id>9` 0、`MAX(id)=9`、种子 MD5 与基线一致、`AUTO_INCREMENT=10`；浏览器残留 msedgedriver / 无头 Edge / 自动化 node 均 **0**（msedge 常规 17 = 基线）；8080 PID 23192 / 88 PID 35760 未变 → **W2 关闭（提前 4 分钟、未超时）**；Redis 只读 3 次（09:15:25 / 09:19:42 / 09:22:40）记录备查。 |
| 2026-11-25 09:25:38–09:26:01 | **V-01 事件**：IDEA 增量编译（java PID 38700）更新 5 个后端 class（含 `CommonController.class`），**未报备** → 8080 devtools 自动重启、**09:26:01 恢复**（8.659s，中断约 9s）；发生在 tester 释放之后、未影响批准窗口；来源未确认（无 mvn 进程 / 无 agent 报备），已记录并上报 lead，提醒团队后端编译（含 IDEA Build）须轻量报备。 |
| 2026-11-25 14:53–14:55 | **第 2 时段（下午场）开工**：lead 宣布实现「护理计划」CRUD 后端（前端零改动），arch 出任务单 → dev1 实现 → tester 验收。web-control 基线核查（14:53）：四端口 PID **与上午完全一致**（88→35760 / 8080→23192 / 3306→16152 / 6379→35808）、浏览器自动化 0、MySQL `nursing_plan=3` / `nursing_project_plan=6` / `nursing_project=9`；14:55 发下午场调度公告（含 V-01 教训：编译须报备）。tester 回执：上午 `/nursing/plan/all` 404 属 **5b 已知 R4**（非 5a 缺陷）、V-01 与 tester 无关、5a 报告已归档、下午将申请 W1/W2 并预报备 Redis 只读。DQ 编号续 **DQ-20261125-0004** 起。 |
| 2026-11-25 14:58–14:59 | dev1 **轻量报备（开始/结束·编译 #3）**：Sprint 5b 护理计划 CRUD 后端 `mvn -DskipTests compile` **BUILD SUCCESS 53.7s**（10 模块全 SUCCESS，含 4 个新护理计划类）。web-control 核查：devtools 触发**双重重启**（第 1 次 14:59:06 因编译中间态 class 缺失启动失败、第 2 次 14:59:22 成功 `Started in 14.32s`；8080 中断约 16s，略超常态但自愈），**PID 23192 未变**；14:59:45 `GET /captchaImage` **HTTP 200**、无新增 error → 编译报备闭环（异常自愈已记录）。 |
| 2026-11-25 14:59–15:00 | dev1 **正式申请 MySQL 写窗口**（`nursing_plan` + `nursing_project_plan` 自测 8 分钟，含 SQL / 回滚 / 种子只读拦截验证）。web-control 重核基线：`nursing_plan=3`（ids 133/134/135）、`nursing_project_plan=6`（ids 1736/1737/1739/1740/1741/1743）、`CHECKSUM TABLE` **2521898468 / 2554906448**；MySQL 写空闲、队列 0 → **批准 DQ-20261125-0004**，窗口 **15:00–15:10**（默认 10 分钟；申请人预计 8 分钟）。 |
| 2026-11-25 15:00–15:01 | dev1 **释放 DQ-20261125-0004**（完成，提前释放，实际窗口 15:00:00–15:00:35）。web-control 独立核验：`nursing_plan=3` / `nursing_project_plan=6`、`ZZT5b` 残留 **0**、主子表 ids 与基线完全一致、**`CHECKSUM TABLE` 2521898468 / 2554906448 与基线完全一致**、8080 PID 23192 未动且 `GET /captchaImage` HTTP 200 → **DQ-0004 关闭（未超时）**；副作用 `sys_logininfor` 1 / `sys_oper_log` 若干记录备查。 |
| 2026-11-25 15:02–15:03 | web-tester **正式申请 W1**（Sprint 5b 护理计划验收 · API 层 A1–A15，10 分钟）：资源=**8080 稳定访问 + MySQL 写**（`ZZT5B-*` 行净增 0；**A8 例外**：种子 133 status 临时 1→0→1，整表单改/删应被拒，验后按开测前快照恢复 `status/update_time`）；不重启、不动 23192/35760；产物 `artifacts/test_web_s5b/api/`；Redis 只读预报备沿用。lead 同步发开测预告确认 arch review PASS。web-control 基线复核（15:02：8080 LISTEN PID 23192 + HTTP 200、MySQL 写空闲、队列 0；`nursing_plan=3` / `nursing_project_plan=6`；`CHECKSUM TABLE` 2521898468/2554906448；种子 133 快照 status=1/update_time=NULL）→ **批准 DQ-20261125-0005**，窗口 **15:03–15:13**（默认 10 分钟）；并告知 W2 释放后立即续批安排。 |
| 2026-11-25 15:05–15:06 | web-tester **释放 DQ-20261125-0005**（完成，**API 48/48 PASS**，提前释放，实际窗口 15:03–15:05；产物 `artifacts/test_web_s5b/api/` 18 文件）。web-control 独立核验（15:06）：`nursing_plan=3` / `nursing_project_plan=6`、`ZZT5B` 残留 **0**、**种子 133 `status=1、update_time=NULL` 已恢复（与开测快照一致）**、`ORPHAN_PP=0`、**`CHECKSUM TABLE` 2521898468/2554906448 与基线完全一致**、8080 PID 23192 未动 + HTTP 200 → **W1 关闭（未超时）**。同时 tester **申请 W2**（浏览器无头 Edge + 8080 只读 + MySQL 写 ≤3 行，15 分钟，UI 验收+截图）→ 队列 0、浏览器自动化空闲 → **按 lead 段间优先授权立即批准 DQ-20261125-0006**，窗口 **15:06–15:21**。 |
| 2026-11-25 15:17–15:18 | web-tester **释放 DQ-20261125-0006**（完成，**UI 36/36 PASS**，提前释放，实际窗口 15:06–15:17；UI 产物 `artifacts/test_web_s5b/ui/` **55 文件含 51 张截图**）。web-control 独立核验（15:18）：`nursing_plan=3` / `nursing_project_plan=6`、`ZZT5B` 残留 **0**、孤儿子表 **0**、**种子 133 未变（status=1/update_time=NULL）**、**`CHECKSUM TABLE` 2521898468/2554906448 与基线完全一致**、浏览器残留 **msedgedriver 0 / headless Edge 0 / 自动化 node 0**、8080 PID 23192 + HTTP 200 / 88 PID 35760 未变 → **W2 关闭（提前释放、未超时）**；**W1+W2 合计 A1–A15 全过（API 48/48 + UI 36/36）**。 |

---

## 七、收工摘要

### 第 0 时段（2026-11-25 08:45–08:49 · 就位待命）

- **时段性质**：调度中心建立与就位自检，**无申请、无调度**（lead 指令今早不开发、等老师任务）。
- **就位核查**：88/8080/3306/6379 全部运行中，各唯一实例；浏览器自动化与 MySQL 写空闲；队列 0；无违规。
- **交付**：本日志（`docs/test/web_调试调度日志.md`）+ 规则文档（`docs/tasks/Web调试调度规则.md`）。
- **收工核对**：未启动/停止/重启任何进程；未改动任何业务代码；未向团队成员发公告（按 lead 指令）。
- **口径确认**：lead 08:49 全部认可就位简报（devtools 轻量报备、重启执行人、默认时长），已写入规则 v1.1 与 §3.6。
- **下一时段**：Sprint 5 开工后恢复调度（已于 08:54 恢复，见 §3.7）。

### 第 1 时段（2026-11-25 09:00 起 · Sprint 5 Web · 进行中）

- 任务：护理项目模块 CRUD 后端（不改前端页面）；dev1 实现 → arch review → tester 测试。
- 公告已发（08:55）；**轻量报备：编译 #1/#2 均完成闭环**（8080 均自动恢复、PID 未变、无新增错误）；**DQ-0001 已释放闭环**；**DQ-0002 W1 已释放闭环**（tester，API 47/47 PASS，09:08–09:13，核验 COUNT=9 / ZZT5 残留 0 / 种子 MD5 与基线一致 / 8080 未动）；**DQ-0003 W2 已释放闭环**（UI 49/49 PASS，09:14–09:25）；队列 0；无违规。
- **Sprint 5 测试时段摘要（2026-11-25 09:07–09:26）**：
  - **窗口统计**：DQ-20261125-0002（W1，批准 09:08–09:18，实际 09:08–09:13，提前释放 5 分钟）、DQ-20261125-0003（W2，批准 09:14–09:29，实际 09:14–09:25，提前释放 4 分钟）；分段批准 + 优先续批，tester 全程未被其他申请打断；**无超时**。
  - **成果**：W1 API 层 **47/47 PASS**（`artifacts/test_web_s5/api/` 25 文件）；W2 UI 层 **49/49 PASS**（`artifacts/test_web_s5/ui/` 65 文件含 60 张截图、`logs/ui_stdout.log`）。
  - **数据核验（web-control 独立）**：`nursing_project` 每段结束均恢复 **COUNT=9**、`ZZT5` 残留 0、`id>9` 0；种子 `SEED_MD5=7e9f6e46dd5b425800f307bf4469f982` 前后一致；`AUTO_INCREMENT=10`（多轮测试前移，正常）。
  - **资源核验**：8080 PID 23192 / 88 PID 35760 全程未变（W2 期间）；浏览器残留 0（msedgedriver / 无头 Edge / 自动化 node）；RuoYi 实例 1、mvn 0。
  - **报备/副作用**：Redis 只读 `GET captcha_codes` 3 次（免申请、已记录，未写未删）；`sys_logininfor` / `sys_oper_log` 自动新增（应用行为、记录备查）；09:24:47 后端 `sys-error` 出现 `/nursing/plan/all` 404（应用/测试过程，不 triage，结论由 tester/arch 报告）。
  - **违规**：**V-01 1 条**（09:25:38 IDEA 增量编译未报备 → devtools 热重启，发生在 tester 释放之后、窗口外无影响；已上报 lead、来源未确认、留档）。
  - **结论**：Sprint 5 Web 测试调度闭环；tester 测试报告与最终结论将同步 lead / web-arch。
- 收工摘要待 lead 宣布收工时填写。

### 第 2 时段（2026-11-25 14:53–15:18 · Sprint 5b「护理计划」CRUD）

- **任务**：护理计划 CRUD **后端**（前端零改动）；arch 任务单 → dev1 实现（编译 #3）→ arch review PASS（15:15 前）→ tester W1+W2 验收
- **窗口统计**：DQ-20261125-0004（dev1 MySQL 写自测，批准 15:00–15:10，实际 **15:00:00–15:00:35**）、DQ-0005（tester W1 API，批准 15:03–15:13，实际 **15:03–15:05**）、DQ-0006（tester W2 UI，批准 15:06–15:21，实际 **15:06–15:17**）；**三段全部提前释放、无超时**；tester 段间优先续批未被打断
- **成果**：dev1 自测（CRUD / 名称唯一 / 子表校验 / 被引用拦截全过）；tester **API 48/48 PASS + UI 36/36 PASS（A1–A15 全过）**；产物 `artifacts/test_web_s5b/api/`（18 文件）、`artifacts/test_web_s5b/ui/`（**55 文件含 51 张截图**）
- **数据核验（web-control 独立，每段结束）**：`nursing_plan=3`（133/134/135）、`nursing_project_plan=6`、`ZZT5B` 残留 **0**、孤儿子表 **0**、**种子 133 于 A8 临时改动后已恢复 status=1/update_time=NULL**、**`CHECKSUM TABLE` 2521898468 / 2554906448 全程与基线一致**
- **资源核验**：8080 PID **23192** / 88 PID **35760** 全程未变（8080 HTTP 200）；浏览器自动化结束 **0 残留**（msedgedriver / headless Edge / 自动化 node 均 0）；RuoYi 1、mvn 0
- **报备**：编译 #3 轻量报备（BUILD SUCCESS 53.7s；devtools 双重重启竞态 14:59:06 失败 → 14:59:22 成功，8080 中断约 16s 自愈，PID 未变）；Redis 只读 `GET captcha_codes` 1 次（15:15:28，W2）；`sys_logininfor`/`sys_oper_log` 自动新增（应用行为，记录备查）
- **违规**：**0**（V-01 系第 1 时段事件，本时段无新增）
- **结论**：Sprint 5b Web 测试调度闭环；tester 报告与结论将同步 lead / web-arch。

---

## 八、文档变更记录

| 版本 | 时间 | 变更 |
|---|---|---|
| v1.0 | 2026-11-25 08:49 | 建立日志（第 0 时段就位记录、端口核查表、规则摘要、DQ/V 表、事件时间线）；规则文档 v1.0 同日建立。 |
| v1.1 | 2026-11-25 08:49 | 记录 lead 口径确认（§3.6）；规则升 v1.1（devtools 轻量报备 + 重启执行人）；时间线对齐实际时间。 |
| v1.2 | 2026-11-25 08:55 | Sprint 5 开工：新增 §3.7 第 1 时段开工记录；当前状态切换为调度进行中；公告已发；时间线/收工摘要同步。 |
| v1.3 | 2026-11-25 08:58 | 新增 §3.8 轻量报备登记表；登记 dev1 编译报备（开始）并回执。 |
| v1.4 | 2026-11-25 09:00 | 编译 #1 结束闭环（BUILD SUCCESS 1:08；8080 重启 ≈15s 后恢复、PID 未变、无新增错误）；登记编译 #2 预告。 |
| v1.5 | 2026-11-25 09:03 | 编译 #2 闭环（devtools 重启 ≈11s 后恢复）；**批准 DQ-20261125-0001（web-dev1 MySQL 写，09:03–09:13）**，只读基线 COUNT=9。 |
| v1.6 | 2026-11-25 09:05 | **DQ-20261125-0001 释放闭环**（提前释放、未超时）；独立核验 COUNT=9 / 残留 0 / 种子 MD5 一致 / 上传文件已删 / 8080 未动；状态回到无占用、队列 0。 |
| v1.7 | 2026-11-25 09:08 | **Sprint 5 测试时段开记**：记录开测预告/lead 分段优先续批授权与测试前基线（§六 09:06 行）；**批准 DQ-20261125-0002（web-tester W1：8080 稳定访问 + MySQL 写，09:08–09:18）**；状态/时间线同步。 |
| v1.8 | 2026-11-25 09:14 | **DQ-0002 W1 释放闭环**（API 47/47 PASS，独立核验 COUNT=9 / ZZT5 残留 0 / 种子 MD5 一致 / 8080 未动 / 产物齐全）；**批准 DQ-20261125-0003（tester W2：浏览器自动化 + 8080 只读 + MySQL 写，09:14–09:29）**；状态/时间线/收工摘要同步。 |
| v1.9 | 2026-11-25 09:27 | **DQ-0003 W2 释放闭环**（UI 49/49 PASS，独立核验 COUNT=9 / ZZT5 残留 0 / 种子 MD5 一致 / 浏览器 0 残留 / 8080、88 PID 未变）；**记录 V-01**（09:25:38 IDEA 增量编译未报备 → devtools 热重启 9s，窗口外无影响，来源未确认、已上报 lead）；新增 **Sprint 5 测试时段摘要**；状态回到无占用、队列 0。 |
| v1.10 | 2026-11-25 14:56 | **第 2 时段（下午场 · Sprint 5b 护理计划 CRUD）开记**：新增 §3.9（任务/资源预期/基线核查/公告/tester 回执）；当前状态切换为下午场值守；`/nursing/plan/all` 404 结论登记为 5b 已知 R4；DQ 编号续 DQ-20261125-0004 起。 |
| v1.11 | 2026-11-25 15:00 | 编译 #3 闭环（BUILD SUCCESS 53.7s；devtools 双重启 14:59:06 失败→14:59:22 成功，8080 中断约 16s 自愈，PID 未变）；**批准 DQ-20261125-0004（web-dev1 MySQL 写，15:00–15:10）**，基线 3/6 行 + ids + 表校验和；状态/时间线同步。 |
| v1.12 | 2026-11-25 15:01 | **DQ-0004 释放闭环**（完成，实际窗口 15:00:00–15:00:35）；独立核验 counts 3/6、`ZZT5b` 残留 0、ids 一致、**表校验和与基线完全一致**、8080 未动 HTTP 200；状态回到无占用、队列 0。 |
| v1.13 | 2026-11-25 15:03 | arch review PASS；**批准 DQ-20261125-0005（tester W1：8080 稳定 + MySQL 写，15:03–15:13）**，基线 3/6 + CHECKSUM + **种子 133 快照**（A8 恢复核验用）；W2 续批安排已约定。 |
| v1.14 | 2026-11-25 15:06 | **DQ-0005 W1 释放闭环**（API 48/48 PASS；独立核验 3/6、ZZT5B 残留 0、**种子 133 已恢复**、ORPHAN 0、CHECKSUM 与基线完全一致、8080 未动）；**批准 DQ-20261125-0006（tester W2：浏览器 + 8080 只读 + MySQL 写，15:06–15:21）**；状态/时间线同步。 |
| v1.15 | 2026-11-25 15:18 | **DQ-0006 W2 释放闭环**（UI 36/36 PASS；独立核验 3/6、ZZT5B 0、ORPHAN 0、种子未变、CHECKSUM 与基线一致、浏览器 0 残留、8080/88 PID 未变）；新增 **第 2 时段测试摘要**（Sprint 5b：API 48/48 + UI 36/36，DQ-0004/0005/0006 全闭环，违规 0）；状态回到无占用、队列 0。 |

---

## 十三、lead 最终验收记录（2026-11-25 09:50）

- web-tester 终版测试报告 v1.0：119 用例 = 118 PASS + 1 N/A，FAIL 0、P1/P2 = 0；首轮 2 个 UI FAIL 经 arch triage 判为口径问题（已修正复测 49/49 PASS）。
- lead 独立只读复核全部通过（list/get/all/搜索/分页/前端零改动/DB 9 行/服务 PID 未变）；详见 `docs/test/Web_Sprint5_验收复核.md`。
- **Sprint 5 验收结论：✅ 通过，可交付**。
- 调度收工：DQ-20261125-0001/0002/0003 均提前释放闭环；V-01（IDEA 增量编译未报备，窗口外无影响）留档；资源最终 0 占用、队列 0、无残留进程。
- 注：原 web-control / web-arch / web-tester 会话在收尾阶段中断，本条为 lead 代记收工。

---

## 十四、第 2 时段 lead 最终验收记录（2026-11-25 15:22）

- web-tester 终版报告 v1.0：91/91 PASS（静态 7 + API 48 + UI 36，0 FAIL、P1/P2=0），A1–A15 全满足。
- lead 独立只读复核（15:18）全部通过：list total=3/count=1、搜索/分页/详情子表//all 实体数组、前端源码零改动、DB 3/6 行恢复且 CHECKSUM 与基线一致、8080/88 PID 未变。
- **Sprint 5b 验收结论：✅ 通过，可交付**。详见 `docs/test/Web_Sprint5b_验收复核.md`。
- 调度收工：DQ-20261125-0004（dev1 自测）/DQ-0005（tester W1 API 48/48）/DQ-0006（tester W2 UI 36/36）全部提前释放；违规 0；浏览器/驱动/MySQL 写残留 0。
- 注：web-control 会话已在上报后转入待命；本条为 lead 收尾代记。
