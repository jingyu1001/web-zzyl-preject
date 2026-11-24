# 中州养老 Web 端 功能清单 v0.2

> 测试对象：正在运行的 Web 端（前端 `http://localhost:88`，Vite dev；后端 `http://localhost:8080`，Spring Boot/JDK21）
> 测试时间：2026-11-24 12:50–13:15｜测试账号：admin / admin123（无头 Edge + CDP 真实登录，逐路由访问）
> 测试方式：**只读测试**——不修改前后端代码、不重启服务、不写数据库、不动 git；验证码答案从 Redis 读取后完成登录
> 测试范围：32 个可见菜单路由 + 3 个“有代码无菜单”路由 + 5 个隐藏详情路由 = **40 条路由**
> 截图：`docs/architecture/assets_v0.2/`（40 张页面图 + 6 张通用组件/弹窗图，1280×720）

---

## 0. 结论速览

| 统计 | 数量 | 说明 |
|---|---|---|
| ✅ 可用（页面加载真实数据，具备查询/增删改等完整操作） | **21** | 含首页看板（数据为前端 mock）、入住办理列表、房型设置、床位预览、负责老人、系统管理全家桶、系统监控、系统工具 |
| ⚠️ 部分可用（页面/UI 可正常打开，但数据为空或操作链路断） | **11** | 预约信息、护理项目、护理计划、护理等级、设备管理、报警规则、报警数据、入住办理（详情链路断）、小智（未验证问答）、数据监控、系统接口 |
| ❌ 不可用/不可达（404、缺表、缺接口导致页面无数据或报错） | **8** | 合同管理、健康评估列表、任务安排列表（3 个无菜单页 404）+ 5 个隐藏详情页 |

**根因三句话**：
1. **后端缺 Controller**：`NursingProjectController / NursingPlanController / NursingLevelController / ReservationController / 任务(task) / 健康评估(healthAssessment)` 均不存在，对应前端 API（如 `/nursing/project/list`）返回 `No static resource ...`，页面只能显示空表。
2. **数据库缺表**：`device / device_data / alert_rule / alert_data / reservation / family_member` 未导入，设备、报警相关页面接口直接抛 SQL 异常（`Table 'zzyl.xxx' doesn't exist`）。
3. **前端有页面但无菜单/无路由**：`views/nursing/contract`、`healthAssessment`、`arrange` 等页面代码存在，但 `sys_menu` 没有对应菜单，路由未注册 → 直接访问 404。

---

## 1. 菜单树（可见 + 隐藏 + 无菜单页面）

```
中州养老
├── 首页 /index                                   ✅ 看板（mock 数据）
├── 中州智询 /aiconsult
│   └── 小智 /aiconsult/aiconsult                 ⚠️ AI 问答页（页面正常，问答依赖 AI 服务）
├── 来访管理 /appointment
│   └── 预约信息 /appointment/reservation         ⚠️ UI 完整，后端无 Controller
├── 入退管理 /enterQuit
│   ├── 入住办理 /enterQuit/checkIn               ⚠️ 列表可用；查看/发起申请 → 详情页缺 /nursing/level/all
│   ├── 入住详情 /enterQuit/checkInInfo           ❌ 隐藏页（缺 level 接口，表单骨架可渲染）
│   └── 评估详情 /enterQuit/healthDetails         ❌ 隐藏页（缺 healthAssessment 接口）
├── 在住管理 /liveIn
│   ├── 房型设置 /liveIn/houseSet                 ✅ 6 条数据，增删改/禁用/图片上传
│   └── 床位预览 /liveIn/floor                    ✅ 楼层/房间/床位可视化，可新增楼层/房间
├── 服务管理 /nursing
│   ├── 护理项目 /nursing/project                 ⚠️ UI+弹窗完整，后端无 Controller（素材图标落地页）
│   ├── 护理计划 /nursing/plan                    ⚠️ UI 完整，后端无 Controller
│   ├── 护理等级 /nursing/level                   ⚠️ UI 完整，后端无 Controller
│   └── 负责老人 /nursing/oldPeople               ✅ 楼层/床位/护理员分配可视化
├── 智能监测 /intelligence
│   ├── 设备管理 /intelligence/device             ⚠️ 页面可开，device 表缺失
│   ├── 报警规则 /intelligence/alertRule          ⚠️ 页面可开，alert_rule 表缺失
│   ├── 报警数据 /intelligence/alertData          ⚠️ 页面可开，alert_data 表缺失
│   ├── 设备详情 /intelligence/details            ❌ 隐藏页（无数据来源）
│   └── 新增报警规则 /intelligence/ruleDetails    ❌ 隐藏页（alert_rule 表缺失）
├── 系统管理 /system
│   ├── 用户管理 /system/user                     ✅ 7 条，CRUD/导入导出/状态开关/重置密码
│   ├── 角色管理 /system/role                     ✅ 6 条，CRUD/导出
│   ├── 菜单管理 /system/menu                     ✅ 128 条树表，CRUD
│   ├── 部门管理 /system/dept                     ✅ 8 条树表，CRUD
│   ├── 岗位管理 /system/post                     ✅ 4 条，CRUD/导出
│   ├── 字典管理 /system/dict                     ✅ 10 条，CRUD/刷新缓存
│   ├── 参数设置 /system/config                   ✅ 6 条，CRUD/刷新缓存
│   ├── 通知公告 /system/notice                   ✅ 2 条，CRUD
│   └── 日志管理 /system/log
│       ├── 操作日志 /system/log/operlog          ✅ 10 条，查询/删除/清空/导出/详情
│       └── 登录日志 /system/log/logininfor       ✅ 10 条，查询/删除/清空/解锁/导出
├── 系统监控 /monitor
│   ├── 在线用户 /monitor/online                  ✅ 1 条，强退
│   ├── 定时任务 /monitor/job                     ✅ 3 条，CRUD/日志/导出
│   ├── 数据监控 /monitor/druid                   ⚠️ Druid iframe 需单独登录，页面基本空白
│   ├── 服务监控 /monitor/server                  ✅ CPU/内存/磁盘/JVM 信息
│   ├── 缓存监控 /monitor/cache                   ✅ Redis 信息/图表
│   └── 缓存列表 /monitor/cacheList               ✅ 7 条缓存，可清理
└── 系统工具 /tool
    ├── 表单构建 /tool/build                      ✅ 纯前端拖拽构建器
    ├── 代码生成 /tool/gen                        ✅ 4 条，导入/预览/生成
    └── 系统接口 /tool/swagger                    ⚠️ Swagger UI 渲染失败（springfox 与 Spring Boot 3 兼容问题）

【有代码但无菜单/路由（直接访问 404）】
├── 合同管理 views/nursing/contract/index.vue      ❌ 无菜单；后端 ContractController 存在但前端无入口
├── 健康评估 views/nursing/healthAssessment/       ❌ 无菜单；后端也无 Controller
└── 任务安排 views/nursing/arrange/                ❌ 无菜单；后端也无 Controller
```

---

## 2. 逐页状态总表（40 条路由）

> 状态：✅ 数据+操作正常｜⚠️ 页面可用但数据/操作受影响｜❌ 不可用
> “数据量”为该页列表首屏行数；“API”为本次实测捕获到的 XHR（省略 `/getInfo`、`/getRouters`）；“DB 表”为对应的库表。

| # | 页面 | 路由 | 状态 | 数据量 | 主要 API | DB 表 | 说明 |
|---|---|---|---|---|---|---|---|
| 01 | 首页看板 | `/index` | ✅ | — | `/system/user/profile` | `sys_user` 等 | 除个人卡外全部为前端 mock 数据（见 v0.1） |
| 02 | 小智 | `/aiconsult/aiconsult` | ⚠️ | 0 | —（未触发） | — | 聊天页壳正常，问答依赖后端 AI Key，未验证收发 |
| 03 | 预约信息 | `/appointment/reservation` | ⚠️ | 0 | `/nursing/reservation/list` | `reservation`（缺） | API `No static resource`，空表 |
| 04 | 入住办理 | `/enterQuit/checkIn` | ⚠️ | 5 | `/nursing/checkIn/list` | `check_in` | 列表正常；“查看/发起申请”跳详情页后缺 `/nursing/level/all` |
| 05 | 房型设置 | `/liveIn/houseSet` | ✅ | 6 | `/elder/roomType/list` | `room_type` | 增删改/禁用/图片上传完整 |
| 06 | 床位预览 | `/liveIn/floor` | ✅ | 11 层/60 房 | `/elder/floor/list`、`/elder/room/getRoomsByFloorId/{id}` | `floor`、`room`、`bed` | 楼层 Tab + 房间卡片 + 床位状态，增楼层/房间 |
| 07 | 护理项目 | `/nursing/project` | ⚠️ | 0 | `/nursing/project/list` | `nursing_project` | UI/弹窗完整，后端无 Controller；12 张素材图标对应此页 |
| 08 | 护理计划 | `/nursing/plan` | ⚠️ | 0 | `/nursing/plan/list`、`/nursing/project/all` | `nursing_plan`、`nursing_project_plan` | 后端无 Controller |
| 09 | 护理等级 | `/nursing/level` | ⚠️ | 0 | `/nursing/level/list`、`/nursing/plan/all` | `nursing_level` | 后端无 Controller |
| 10 | 负责老人 | `/nursing/oldPeople` | ✅ | 2 层 | `/elder/floor/getAllFloorsWithNur`、`/system/user/listByDeptId`、`/elder/room/getRoomsWithNurByFloorId/{id}` | `floor`、`room`、`bed`、`nursing_elder` | 楼层/房间/床位/护理员分配可视化 |
| 11 | 设备管理 | `/intelligence/device` | ⚠️ | 0 | `/nursing/device/list`、`/nursing/device/syncProductList` | `device`（缺） | SQL 异常：表不存在 |
| 12 | 报警规则 | `/intelligence/alertRule` | ⚠️ | 0 | `/nursing/alertRule/list`、`/nursing/device/syncProductList` | `alert_rule`（缺） | SQL 异常：表不存在 |
| 13 | 报警数据 | `/intelligence/alertData` | ⚠️ | 0 | `/nursing/alertData/list` | `alert_data`（缺） | SQL 异常：表不存在 |
| 14 | 用户管理 | `/system/user` | ✅ | 7 | `/system/user/list`、`/system/user/deptTree` | `sys_user`、`sys_dept`、`sys_post`、`sys_role` | CRUD/导入/导出/状态开关/重置密码/分配角色 |
| 15 | 角色管理 | `/system/role` | ✅ | 6 | `/system/role/list` | `sys_role`、`sys_role_menu`、`sys_role_dept` | CRUD/导出/分配用户/数据权限 |
| 16 | 菜单管理 | `/system/menu` | ✅ | 128 | `/system/menu/list` | `sys_menu` | 树表，CRUD |
| 17 | 部门管理 | `/system/dept` | ✅ | 8 | `/system/dept/list` | `sys_dept` | 树表，CRUD |
| 18 | 岗位管理 | `/system/post` | ✅ | 4 | `/system/post/list` | `sys_post` | CRUD/导出 |
| 19 | 字典管理 | `/system/dict` | ✅ | 10 | `/system/dict/type/list` | `sys_dict_type`、`sys_dict_data` | CRUD/刷新缓存 |
| 20 | 参数设置 | `/system/config` | ✅ | 6 | `/system/config/list` | `sys_config` | CRUD/刷新缓存 |
| 21 | 通知公告 | `/system/notice` | ✅ | 2 | `/system/notice/list` | `sys_notice` | CRUD |
| 22 | 操作日志 | `/system/log/operlog` | ✅ | 10 | `/monitor/operlog/list` | `sys_oper_log` | 查询/删除/清空/导出/详情 |
| 23 | 登录日志 | `/system/log/logininfor` | ✅ | 10 | `/monitor/logininfor/list` | `sys_logininfor` | 查询/删除/清空/解锁/导出 |
| 24 | 在线用户 | `/monitor/online` | ✅ | 1 | `/monitor/online/list` | Redis `login_tokens:*` | 强退 |
| 25 | 定时任务 | `/monitor/job` | ✅ | 3 | `/monitor/job/list` | `sys_job`、`sys_job_log` | CRUD/启停/日志/导出 |
| 26 | 数据监控 | `/monitor/druid` | ⚠️ | — | — | — | Druid 控制台 iframe，需 /druid 登录，页面基本空白 |
| 27 | 服务监控 | `/monitor/server` | ✅ | — | `/monitor/server` | —（OSHI 实时） | CPU/内存/服务器/JVM 面板 |
| 28 | 缓存监控 | `/monitor/cache` | ✅ | — | `/monitor/cache` | Redis | Redis 信息表 + 命令统计图 |
| 29 | 缓存列表 | `/monitor/cacheList` | ✅ | 7 | `/monitor/cache/getNames` 等 | Redis | 查看/清理缓存 |
| 30 | 表单构建 | `/tool/build` | ✅ | — | — | — | 纯前端拖拽生成器，导出 vue/复制代码 |
| 31 | 代码生成 | `/tool/gen` | ✅ | 4 | `/tool/gen/list` | `gen_table`、`gen_table_column` | 导入表/编辑/预览/生成 |
| 32 | 系统接口 | `/tool/swagger` | ⚠️ | — | `/swagger-resources` | — | 前端报 `n.forEach is not a function`，Swagger UI 未渲染 |
| 33 | 合同管理（无菜单） | `/nursing/contract` | ❌ | — | — | `contract` | 路由未注册 → 404；后端 ContractController 存在但前端无入口 |
| 34 | 健康评估（无菜单） | `/nursing/healthAssessment` | ❌ | — | — | 无表无接口 | 404 |
| 35 | 任务安排（无菜单） | `/nursing/arrange` | ❌ | — | — | — | 404 |
| 36 | 入住详情（隐藏） | `/enterQuit/checkInInfo` | ❌ | — | `/nursing/level/all` | `nursing_level`（接口 404） | 表单骨架可渲染，护理等级下拉加载失败 |
| 37 | 评估详情（隐藏） | `/enterQuit/healthDetails` | ❌ | 0 | `/nursing/healthAssessment/1` | — | 接口 404，空表 |
| 38 | 设备详情（隐藏） | `/intelligence/details` | ❌ | 0 | — | `device`（缺） | 无数据来源 |
| 39 | 报警规则详情（隐藏） | `/intelligence/ruleDetails` | ❌ | — | `/nursing/alertRule/1` | `alert_rule`（缺） | SQL 异常 |
| 40 | 任务安排详情（隐藏） | `/nursing/arrangeDetails` | ❌ | — | `/nursing/task/1` | — | 接口 404 |

---

## 3. 模块明细（表格列 / 搜索项 / 操作 / API / DB）

### 3.1 系统管理-用户管理（✅ 推荐转译样板）

- **路由**：`/system/user`｜数据：7 条（分页 10 条/页）
- **搜索栏**：部门树下拉（占整行，placeholder“请输入部门名称”）、用户名称（输入 240）、手机号码（240）、状态（下拉 240）、创建时间（日期范围）
- **工具栏**：新增、修改、删除、导入、导出（右侧还有刷新/列设置/搜索图标按钮）
- **表格列**：勾选列(50)｜用户编号｜用户名称｜用户昵称｜部门｜手机号码(120)｜状态(开关)｜创建时间(160)｜操作(150：修改/删除/重置密码/分配角色)
- **新增/编辑弹窗**（600×512，圆角 8，标题“添加用户/修改用户”，双列）：
  - 用户昵称*（maxlength 30）｜归属部门（树选择）
  - 手机号码（maxlength 11）｜邮箱（maxlength 50）
  - 用户名称*（仅新增，2–20 字符）｜用户密码*（仅新增，5–20，非法字符校验）
  - 用户性别（下拉）｜状态（单选：正常/停用）
  - 岗位（多选）｜角色（多选）
  - 备注（textarea 整行）
  - 底部：确定（主色）/取消
- **行内操作**：修改弹窗、删除（确认框）、重置密码（弹窗输入新密码）、分配角色（跳转页）
- **API**：`GET /system/user/list`、`GET /system/user/{id}`、`POST /system/user`、`PUT /system/user`、`DELETE /system/user/{id}`、`PUT /system/user/resetPwd`、`PUT /system/user/changeStatus`、`GET /system/user/deptTree`、`POST /system/user/export`
- **DB**：`sys_user`、`sys_dept`、`sys_post`、`sys_user_post`、`sys_role`、`sys_user_role`

### 3.2 在住管理-房型设置（✅ 推荐转译样板，含图片上传）

- **路由**：`/liveIn/houseSet`｜数据：6 条
- **搜索栏**：无（页面直接进入工具栏+列表）
- **工具栏**：新增（浅青底主色字）
- **表格列**：序号(50)｜房间图片(100，缩略图)｜房型类型｜床位费用｜房型介绍(180)｜创建时间(160)｜状态(开关/标签)｜操作(200：修改/删除/禁用·启用)
- **新增/编辑弹窗**（500×667，标题“添加房型/修改房型”，单列 label-width 80）：
  - 房型名称*｜床位数量*｜床位费用*｜介绍（textarea）｜照片（上传：148×148 方块 + 点击上传）｜备注
  - 底部：确定/取消
- **API**：`GET /elder/roomType/list`、`GET /elder/roomType/{id}`、`POST /elder/roomType`、`PUT /elder/roomType`、`DELETE /elder/roomType/{id}`（含图片上传接口）
- **DB**：`room_type(id, name, bed_count, price, introduction, photo, status, ...)`

### 3.3 服务管理-护理项目（⚠️ UI 完整、后端缺 Controller；12 张素材图标落地页）

- **路由**：`/nursing/project`｜数据：0（DB 有 9 条，接口 404）
- **搜索栏**：名称（输入）、状态（字典下拉：`nursing_project_status`）+ 搜索/重置
- **工具栏**：新增（带 + 图标，浅青底主色字）
- **表格列**：编号(120)｜名称(150)｜排序号(100)｜单位(100)｜价格(150)｜图片(150，缩略图)｜护理要求｜状态(120)｜创建时间(180)｜操作(200：修改/删除/启用禁用)
- **新增/编辑弹窗**（500×727，标题“添加护理项目”）：
  - 名称*｜排序号（number）｜单位｜价格*（number）｜状态（单选，字典）｜图片（上传 148×148）｜护理要求（textarea）
  - 底部：确定/取消
- **API（当前均 404）**：`GET /nursing/project/list`、`GET /nursing/project/{id}`、`POST /nursing/project`、`PUT /nursing/project`、`DELETE /nursing/project/{id}`、`GET /nursing/project/all`
- **DB**：`nursing_project(id, name, order_no, unit, price, image, nursing_requirement, status)`（已有 9 条数据）
- **素材**：`素材/服务项目图片/*.png`（12 张 340×300）→ 见 `UI复刻规格_Cpp_v0.2.md` §6

### 3.4 入退管理-入住办理（⚠️ 列表可用、详情链路断）

- **路由**：`/enterQuit/checkIn`｜数据：5 条
- **搜索栏**：老人姓名、身份证号 + 搜索/重置
- **工具栏**：发起入住申请（主色按钮）
- **表格列**：序号｜老人姓名｜身份证号｜入住床位｜护理等级名称｜入住期限｜状态｜创建时间｜操作（查看）
- **详情页**（`/enterQuit/checkInInfo`，❌ 依赖缺接口）：基本信息（老人姓名/身份证/出生日期/年龄/性别/联系方式/家庭住址/一寸照片/身份证正反面）、家属信息、入住配置（入住期限、费用期限、护理等级、入住床位、押金、护理费用、床位费用、其他费用、医保支付、政府补贴）、签约办理（合同名称/签约日期/丙方姓名/丙方联系方式/入住合同上传）
- **API**：`GET /nursing/checkIn/list`（✅）、`GET /nursing/checkIn/{id}`、`POST /nursing/checkIn`、`GET /nursing/level/all`（❌ 404）、`GET /elder/floor/getRoomAndBedByBedStatus/0`（✅）
- **DB**：`check_in`、`check_in_config`、`contract`、`elder`、`nursing_level`

### 3.5 在住管理-床位预览 / 负责老人（✅ 可视化页面）

- 床位预览：楼层 Tab（1/2/3…12 楼）+ 状态图例（空闲中/已入住/请假中）+ 房间卡片（房间号、编辑、删除、+ 新增床位）+ 床位卡片（床号、老人姓名/空闲）；API `/elder/floor/list`、`/elder/roomType/listAll`、`/elder/room/getRoomsByFloorId/{id}`；DB `floor/room/bed/elder`。
- 负责老人：楼层 Tab + 房间/床位卡片 + “批量设置护理员”；API `/elder/floor/getAllFloorsWithNur`、`/system/user/listByDeptId`、`/elder/room/getRoomsWithNurByFloorId/{id}`；DB `floor/room/bed/nursing_elder/sys_user`。

### 3.6 其他系统页面（略，均为 RuoYi 标准 CRUD，详见第 2 节表格与截图）

---

## 4. 通用组件规格（实测，1280×720）

| 组件 | 尺寸/样式（基准 px） | 备注 |
|---|---|---|
| 内容容器 `.app-container` | padding 20；顶部 y=84（顶栏 50+标签 34），内容宽 1080 | 页面整体纵向滚动（长页面需滚到分页） |
| 搜索表单（inline） | 表单项宽 308（label 68 + 控件 240×32）；行高 32；行间距 28（下一行 y+60）；末尾按钮区 172×32 | 标签右对齐；输入框 placeholder 灰 #A8ABB2；输入框高 32（内层 30） |
| 搜索按钮 | 80×32，主色 #00B8A0 底色白字，圆角 4，14px，带放大镜图标 | Element primary 按钮 |
| 重置按钮 | 80×32，白底、#606266 字、边框 #DCDFE6，圆角 4 | 带刷新图标 |
| 工具栏按钮（新增/修改/删除/导入/导出） | 80×32，圆角 4，14px，细边框 + 浅底：新增 `#E5F7F5/#00B8A0`、修改 `#F0F9EB/#B3E19D`、删除 `#FEF0F0/#FAB6B6`、导入 `#F4F4F5/#909399`、导出 `#FDF6EC/#E6A23C` | RuoYi 浅色按钮风格 |
| 行内操作 | 文字按钮 14px 主色 #00B8A0（修改/删除/查看/详细），间距 8 | 部分页为图标按钮 |
| 数据表格表头 | 高 40；背景 #F8F8F9；文字 #515A6E、13px、600；下边框 #EBEEF5 | 全局覆盖样式 |
| 数据表格行 | 高 49（含单元格 padding 8 0）；正文 14px #606266；hover 高亮 | 斑马纹默认关闭 |
| 表格空态 | 文本“暂无数据”，垂直居中 | — |
| 分页 | 高 32，右对齐；文案“共 N 条　10条/页　前往 N 页”；按钮 32×32、圆角 2 方形 | 长页面在视口下方，需滚动 |
| 弹窗 | 宽 600（用户）/500（房型、护理项目）；圆角 8；距顶 6vh（720 下 43.2）；标题 18px；底部按钮高 32（确定主色 64 宽、取消白底） | 双列表单（用户）/单列（房型、项目） |
| 表单 | label 右对齐，宽约 80–90；控件高 32；表单项垂直间距 28 | 必填项标签前红色 * |
| 状态开关 | Element switch，40×20，开启 #00B8A0 | 用户/角色/房型等状态列 |
| 图片上传 | 148×148 虚线/实线方框，中央 + 号；上传后显示缩略图 | 房型照片、护理项目图片 |
| 消息提示（错误） | 顶部居中，top 16；高 38；左右内边距；背景 #FEF0F0；文字 #F56C6C 深色/黑；圆角 4；14px | 如“No static resource…”、SQL 异常 |
| 删除确认框 | Element `ElMessageBox.confirm`：宽 420，圆角 4，标题 16px，内容 14px；底部“取消/确定”（确定主色） | 各页删除/清空/强退 |
| 标签页（TagsView） | 高 34；标签 26 高；选中 #00B8A0 白字 | 打开菜单后追加页签 |

**页面加载提示**：接口等待时表格区域有 loading 遮罩；失败时弹顶部错误消息（约 3 秒后消失），表格回落到“暂无数据”。

---

## 5. 关键结论（给排期/修复用）

### 5.1 后端缺失 Controller（导致 7 个页面空表）

| 前端 API 前缀 | 缺失的 Controller | 影响页面 |
|---|---|---|
| `/nursing/project/**` | `NursingProjectController` | 护理项目 |
| `/nursing/plan/**` | `NursingPlanController` | 护理计划 |
| `/nursing/level/**` | `NursingLevelController` | 护理等级、入住详情（护理等级下拉） |
| `/nursing/reservation/**` | `ReservationController` | 预约信息 |
| `/nursing/task/**` | 任务相关 Controller | 任务安排详情 |
| `/nursing/healthAssessment/**` | 健康评估 Controller | 评估详情 |

> 注：`ContractController` 后端存在，但前端无菜单/路由，合同页面不可达。

### 5.2 数据库缺表（导致 5 个页面 SQL 异常）

`device`、`device_data`、`alert_rule`、`alert_data`、`reservation`、`family_member` —— 需老师提供完整建表/数据脚本。

### 5.3 前端“有页面无入口”

`views/nursing/contract/index.vue`、`views/nursing/healthAssessment/{index,details}.vue`、`views/nursing/arrange/*` 均未注册路由（`sys_menu` 无对应菜单），只能 404。

### 5.4 其他问题

- `/tool/swagger` 页面报 `n.forEach is not a function`（springfox 3.0 与 Spring Boot 3.5 不兼容）。
- `/monitor/druid` 页面为 iframe 指向 `/druid`，需先登录 Druid 控制台（ruoyi/123456），否则空白。
- 首页看板数据为前端 mock（见 v0.1 文档）。

---

## 6. 截图索引（`docs/architecture/assets_v0.2/`）

- 页面截图：`01_首页看板.png` ～ `40_任务安排详情（隐藏）.png`（共 40 张，1280×720）
- 通用组件截图：
  - `50_通用_用户管理列表_1280x720.png`（搜索栏+工具栏+表格+分页）
  - `51_通用_用户管理新增弹窗_1280x720.png`（双列表单弹窗）
  - `52_通用_删除确认框_1280x720.png`（确认框）
  - `53_在住_房型设置新增弹窗_1280x720.png`（单列表单+图片上传）
  - `54_服务_护理项目新增弹窗_1280x720.png`（护理项目表单）
  - `55_通用_错误消息提示_1280x720.png`（错误 toast）

---

## 7. 本次测试边界

- 只读：未修改代码/配置/数据库/git；未重启用户服务；使用独立无头 Edge 实例（调试端口 9333）与用户前台浏览器隔离。
- 登录会由后端正常写入 `sys_logininfor`/`sys_user.login_date`（系统自身行为，非人工改库）。
- 未做压力/安全/兼容性测试；未验证 AI 问答实际收发、文件上传真实落盘、导入导出文件内容。
