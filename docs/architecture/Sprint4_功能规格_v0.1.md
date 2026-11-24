# Sprint 4 功能规格 v0.1（M5 护理计划 / M6 护理等级 / M7 床位预览 / M4 数据链增强）

> 项目：中州养老（Web 综合端 → C++ 转译端）
> 范围：M5 护理计划、M6 护理等级、M7 床位预览（可视化妆）、M4 数据链增强（floor/room/bed/room_type + 入住床位级联 + 图片/PDF 上传规格）；末尾列出“负责老人/来访管理”（本轮不做）
> 依据：前端 `src/views/nursing/{plan,level,floor}/**`、`src/api/nursing/*`、`src/components/ImageUpload`；运行态实测（`localhost:88` + CDP，admin 登录，1280×720 / 1024×768）；MySQL `zzyl` 只读查询
> 方式：**只读**（未改代码/数据库/服务/git）；截图见 `docs/architecture/assets_s4/`（10 张）
> 时间：2026-11-24 21:30–22:20｜配色/字号/通用组件沿用 v0.2；本文件只写增量

---

## 0. 状态总览

| 模块 | 页面/路由 | 状态 | 真实数据 | 后端 API | 结论 |
|---|---|---|---|---|---|
| **M5** 护理计划 | `/nursing/plan` | ⚠️ 部分可用 | DB 有 3 条，但接口 404 | ❌ `No static resource nursing/plan/list`（无 Controller） | 页面/弹窗 UI 完整可交互；列表空表、项目下拉为空；C++ 端用本地种子数据实现 |
| **M6** 护理等级 | `/nursing/level` | ⚠️ 部分可用 | DB 有 3 条，接口 404 | ❌ `No static resource nursing/level/list`（无 Controller） | 同上；计划下拉为空；C++ 端本地化 |
| **M7** 床位预览 | `/liveIn/floor` | ✅ 可用 | 楼层 11 / 房间 60 / 床位 89（实测渲染） | ✅ `floor/list`、`room/getRoomsByFloorId`、`roomType/listAll` 正常 | 可视化主页面可直接复刻（有 2 个字段级缺陷，见 §5.6） |
| **M4 增强** | 楼层/房间/床位 + 入住床位级联 | ✅ 数据链可用 | 同上 | ✅ 级联接口 `/elder/floor/getRoomAndBedByBedStatus/0` 实测 200 | 三表结构 + 种子 + 级联 JSON 见 §6 |
| 上传控件 | 图片 / PDF | ✅ 组件可用（未做真实写盘测试） | — | ✅ `/common/upload` 存在（RuoYi 默认） | 规格见 §7 |

> 一句话：**M7 和 M4 数据链可以真实跑；M5/M6 只能本地化（UI 复刻照做，数据用 SQLite 种子）。**

---

## 1. M5 服务管理-护理计划

### 1.1 页面（`/nursing/plan`）

- 路由：`/nursing/plan`，组件 `views/nursing/plan/index.vue`，权限 `nursing:plan:list`。
- 实测状态：页面可打开；表格空表“暂无数据”；顶部 toast `No static resource nursing/plan/list.`。
- 搜索栏（inline，label 68）：
  | 字段 | 控件 | 宽度基准 | 说明 |
  |---|---|---|---|
  | 名称 `planName` | 输入 | 192 | placeholder“请输入名称”，回车搜索 |
  | 状态 `status` | 下拉 | 200 | 字典 `nursing_plan_status`：启用=1 / 禁用=2 |
  | 按钮 | 搜索 80×32 主色 / 重置 80×32 白底 | — | — |
- 工具栏：新增按钮 **80×32**（plain primary 浅青底 `#E5F7F5`/字 `#00B8A0`，图标 Plus）。
- 表格列（实测宽度，空表时表头）：

| # | 列名 | 字段 | 宽度基准 | 说明 |
|---|---|---|---|---|
| 1 | 序号 | index | 50 | 居中 |
| 2 | 名称 | `planName` | auto（1280 下 265） | 居中 |
| 3 | 状态 | `status` | auto（265） | 标签：1=启用（绿）/否则禁用（红） |
| 4 | 创建时间 | `createTime` | 180 | `YYYY-MM-DD HH:mm:ss` |
| 5 | 操作 | — | **280**（fixed right） | 修改 / 删除 / 查看 / 启用·禁用 |

- 行操作规则（代码）：
  - 修改、删除：当 `row.count > 0`（被护理等级引用）时加 `disabled` 样式（不可点）；
  - 查看：只读弹窗（`isLook=true`，禁用全部输入、隐藏操作列与底部按钮）；
  - 启用/禁用：确认框“是否确认启用/禁用护理项目的数据项？”→ `PUT /nursing/plan`（只传 id+status）。
- 分页：`total>0` 才显示；请求参数 pageNum/pageSize（默认 1/10）。
- 缺陷：文案“护理项目”出现在确认框（复制粘贴），应为“护理计划”。

### 1.2 新增/修改/查看弹窗（实测 1000×614，居中，距顶 6vh）

- 标题：添加护理计划 / 修改护理计划；`label-width: 140px`；底部右对齐：确定（主色 80×32）/ 取消（白底 80×32）；查看模式无底部按钮。
- **顶部三字段**（一行 flex，每项宽 50% 换行排列）：
  | 字段 | 控件 | 必填 | 校验/默认 |
  |---|---|---|---|
  | 护理计划名称 `planName` | 输入（宽约 350） | ✅ | “护理计划名称为空，请输入护理计划名称” |
  | 状态 `status` | 单选（启用=1 / 禁用=2） | ✅ | 默认 1；必选 |
  | 排序 `sortNo` | 数字步进器（min 1，max 999999，整数） | ✅ | 默认 1；≤1 自动回 1 |
- **护理项目子表**（弹窗内，非独立表）：

| 列 | 控件 | 宽度比例 | 说明 |
|---|---|---|---|
| 护理项目名称 | 下拉（options = 所有护理项目） | flex 2 | 必选；数据源 `/nursing/project/all`（当前 404 → 空） |
| 期望服务时间 | 时间选择 `HH:mm:ss` | flex 1.8 | placeholder“请选择时间选择” |
| 执行周期 | 下拉（天=0 / 周=1 / 月=2） | flex 1.2 | — |
| 执行频次 | 数字（min1 / max7） | flex 1 | 默认 1 |
| 操作 | +（新增行，主色 16px）/ −（删除行，红 #C03639；仅剩 1 行时不显示） | flex 0.5 | — |

  - 表头：高 **50**、背景 **#F3F8F9**、14px、居中；表体：高 **300**、`overflow-y: scroll`、行高 50；每列内边距 10。
  - 校验：子表无逐项必填规则，但提交前做**项目重复校验**（projectId 去重），重复时提示“请勿选择重复的护理项目”。
- 提交 payload：
  ```json
  { "id": 133, "planName": "1号护理计划", "status": 1, "sortNo": 1,
    "projectPlans": [ { "projectId": 1, "executeTime": "19:16:49", "executeCycle": "1", "executeFrequency": 7 } ] }
  ```
  新增 `POST /nursing/plan`、修改 `PUT /nursing/plan`；成功 toast“新增成功/修改成功”并刷新列表。

### 1.3 数据关联与 API

```
nursing_plan (1) ──< nursing_project_plan (N) >── (1) nursing_project
                 └──< nursing_level.plan_id（护理等级引用计划，参考 §2）
```

| 方法/路径 | 用途 | 实测 |
|---|---|---|
| `GET /nursing/plan/list?pageNum&pageSize&planName&status` | 列表（TableDataInfo：`{total, rows[]}`） | ❌ 500 |
| `GET /nursing/plan/{id}` | 详情（含 `projectPlans[]`） | ❌ 500 |
| `POST /nursing/plan` | 新增 | ❌ 500 |
| `PUT /nursing/plan` | 修改/启停 | ❌ 500 |
| `DELETE /nursing/plan/{id}` | 删除 | ❌ 500 |
| `GET /nursing/plan/all` | 所有计划（护理等级下拉用） | ❌ 500 |
| `GET /nursing/project/all` | 所有护理项目（计划弹窗下拉用） | ❌ 500（Controller 缺失） |

> 期望返回结构（由前端代码反推）：`rows[] = {id, planName, status, sortNo, createTime, count, projectPlans:[{projectId,executeTime,executeCycle,executeFrequency}]}`；`count` 用于判断计划是否被护理等级引用。

### 1.4 种子数据（DB 实测）

**nursing_plan（3 条）**

| id | plan_name | sort_no | status | create_time |
|---|---|---|---|---|
| 133 | 1号护理计划 | 1 | 1 启用 | 2024-08-19 19:17:31 |
| 134 | 2号护理计划 | 1 | **0 禁用** | 2024-08-19 19:36:10 |
| 135 | 3号护理计划 | 1 | 1 启用 | 2024-08-29 16:56:39 |

**nursing_project_plan（6 条，plan→project 明细）**

| id | plan_id | project_id | 项目名 | execute_time | execute_cycle | execute_frequency |
|---|---|---|---|---|---|---|
| 1736 | 133 | 1 | 修剪指甲 | 19:16:49 | 1 周 | 7 |
| 1737 | 133 | 2 | 衣物清洁 | 19:28:34 | 1 周 | 1 |
| 1743 | 134 | 1 | 修剪指甲 | 19:28:45 | 0 天 | 1 |
| 1739 | 135 | 1 | 修剪指甲 | 16:55:58 | 2 月 | 1 |
| 1740 | 135 | 5 | 助浴 | 16:56:09 | 0 天 | 1 |
| 1741 | 135 | 4 | 助餐 | 08:00:00 | 0 天 | 1 |

**nursing_project（9 条，弹窗下拉数据源）**：修剪指甲30/次、衣物清洁5/件、整理床铺15/次、助餐15/餐、助浴40/次、洗头20/次（禁用）、洗脸15/次、洗脚20/次、心理咨询80/小时。

---

## 2. M6 服务管理-护理等级

### 2.1 页面（`/nursing/level`）

- 路由：`/nursing/level`，组件 `views/nursing/level/index.vue`，权限 `nursing:level:list`。
- 实测状态：页面可打开；空表“暂无数据”；toast `No static resource nursing/level/list.`。
- 搜索栏：等级名称（输入 192，placeholder“请输入等级名称”）｜状态（下拉 240，字典 `nursing_level_status`：启用=1/禁用=0）｜搜索/重置（80×32）。
- 工具栏：新增（80×32 plain primary）+ 右侧工具（刷新/搜索/列设置 3 个 32×32 圆形图标按钮）。
- 表格列（实测宽度）：

| # | 列名 | 字段 | 宽度 | 说明 |
|---|---|---|---|---|
| 1 | 序号 | index | 100 | 居中 |
| 2 | 护理等级名称 | `name` | 150 | — |
| 3 | 执行护理计划 | `planName` | auto（1280 下 124） | 关联计划名称（后端 join，当前无数据） |
| 4 | 护理费用(元/月) | `fee` | auto（122） | 金额 |
| 5 | 状态 | `status` | 120 | 标签 1=启用（绿）/否则禁用（红） |
| 6 | 等级说明 | `description` | auto（122） | 文本 |
| 7 | 创建时间 | `createTime` | 180 | `YYYY-MM-DD`（只到日） |
| 8 | 操作 | — | auto（122） | 修改 / 删除 / 启用·禁用（主色文字按钮，带图标） |

- 行操作：修改（弹窗回填）、删除（确认框“是否确认删除这条护理等级？”）、启用/禁用（确认框“是否确认启用/禁用该护理等级？”→ `PUT /nursing/level` 只传 id+status）。
- 分页同上；页面另有 `handleExport()` 导出方法（`nursing/level/export`），但模板未放导出按钮。

### 2.2 新增/修改弹窗（实测 500×432，`label-width: 80px`）

| # | 字段 | 控件 | 必填 | 校验提示/说明 |
|---|---|---|---|---|
| 1 | 等级名称 `name` | 输入 | ✅ | 请输入等级名称 / “等级名称不能为空” |
| 2 | 护理计划 `planId` | 下拉（options = `GET /nursing/plan/all`，label=planName，value=id） | ✅ | placeholder“请选择”；“护理计划ID不能为空”；当前接口 404 → 空 |
| 3 | 护理费用 `fee` | 数字（step=100，placeholder“请输入护理费用”） | ✅ | “护理费用不能为空” |
| 4 | 状态 `status` | 单选（禁用=0 / 启用=1） | ✅ | 必选 |
| 5 | 等级说明 `description` | 输入（placeholder“请输入等级说明”） | ✅ | “等级说明不能为空” |
| — | 底部 | 确定（主色）/ 取消 | — | — |

提交：`POST /nursing/level` 或 `PUT /nursing/level`；成功 toast“新增成功/修改成功”并刷新。

### 2.3 数据与 API

| 方法/路径 | 用途 | 实测 |
|---|---|---|
| `GET /nursing/level/list?pageNum&pageSize&name&status` | 列表 | ❌ 500 |
| `GET /nursing/level/{id}` | 详情 | ❌ 500 |
| `POST /nursing/level` | 新增 | ❌ 500 |
| `PUT /nursing/level` | 修改/启停 | ❌ 500 |
| `DELETE /nursing/level/{id}` | 删除 | ❌ 500 |
| `GET /nursing/level/all` | 所有等级（入住配置/下拉用） | ❌ 500 |
| `POST /nursing/level/export` | 导出（页面无按钮） | ❌（推断） |

**nursing_level（3 条实测）**

| id | name | plan_id | fee | status | description |
|---|---|---|---|---|---|
| 76 | 1号护理等级 | 133（1号护理计划） | 1500.00 | 1 启用 | 我是1号哦 |
| 77 | 2号护理等级 | 134（2号护理计划） | 2000.00 | 1 启用 | 我是2号哦 |
| 78 | 3号护理等级 | 135（3号护理计划） | 3000.00 | 1 启用 | 我是3号哦 |

> 关联：`nursing_level.plan_id → nursing_plan.id`；`check_in_config.nursing_level_id → nursing_level.id`（入住配置用），`nursing_level.fee` 即入住办理表单里“护理费用”的自动带出值。

---

## 3. M7 在住管理-床位预览（重点）

### 3.1 页面结构（自上而下）

```
内容区（x=200..1280，y=84 起；外层 .content-layout 背景 #FAFAFA，padding 24/15/52/24）
└── 白色内容块 .bg-wt（宽 1041，x=224）
    ├── ① 楼层 Tab 栏 .headBox（高 57，下边框 1px #E8E8E8）
    │    左：11 个楼层 Tab（1楼/2楼/…/10楼/12楼），每个 padding 12×20、14px；
    │        选中：#008D71 文字 + 底部 2px #008D71 圆角指示条；hover 出现编辑图标 16×16
    │    右：「新增楼层」（#008D71、14px、绝对定位 right:0 top:20）
    ├── ② 图例 + 新增房间行 .floorInfo（高 52，padding-top 20）
    │    左：空闲中/已入住/请假中（图标 26×26 + 文字 14px #262626，每项右距 30）
    │    右：新增房间按钮（88×32 主色）
    ├── ③ 房间卡片列表 .houseList（flex-wrap，顶部 padding 16）
    │    每张卡片（width = calc(50% - 20px)，margin-left 20、margin-bottom 20）
    │    ├── 房间标题行：房间号 18px/500 + 房型标签（浅青底）+ 编辑/删除图标 20×20 + 右侧「+」新增床位 24×24
    │    └── 床位网格（2 列）：床位卡（padding 14、圆角 2、底色 #F5F5F5、高 72）
    │          ├─ 状态图标 26×26（已入住 icon-yrz / 空闲 icon-kxz / 请假 icon-wcz）
    │          └─ 两行文字 14px #595959：「床位号：XXX」+ 老人姓名 / “空闲”
    │         hover：半透明黑遮罩 rgba(0,0,0,.3) + 居中 2 个 22×22 圆形按钮（编辑/删除）
    └── 滚动：页面整体纵向滚动（1280×720 下总高 1080，需滚动约 360px）
```

### 3.2 交互

| 交互 | 行为 |
|---|---|
| 点击楼层 Tab | 切换并拉取该楼层房间/床位（`getRoomsByFloorId/{floorId}`） |
| 楼层 Tab hover | 显示编辑小图标 → 打开“编辑楼层”弹窗（含删除按钮，楼层下有房间时删除置灰） |
| 新增楼层 | 弹窗 628×276：楼层名称*（≤5 字）、排序*（数字 1~99999999） |
| 新增房间 | 弹窗 628×336：房间号*（≤5 字）、房间类型*（下拉 = roomType/listAll 中 status=1 的 6 种）、排序* |
| 房间编辑 | 弹窗同上（回填）；房间删除：仅当房间无床位时可删（有床位时删除图标置灰） |
| 新增床位 | 房间标题右侧「+」→ 弹窗（床位名称* ≤10 字、排序*），自动带 roomId |
| 床位编辑/删除 | hover 床位出现；仅空床位可操作；已入住床位禁用（透明度 0.4） |
| 床位状态配色 | 床位底色**不区分状态**（统一 #F5F5F5），仅靠左侧图标区分：已入住=icon-yrz、空闲=icon-kxz、请假=icon-wcz（请假逻辑未实现，见 3.6） |
| 删除 | 共用 `components/DelectDialog` 删除确认弹窗（楼层/房间/床位共用） |
| 空态 | 楼层无房间 → “暂无数据”占位；房间无床位 → “当前房间没有安排床位” |

### 3.3 数据来源（实测 API）

| 方法/路径 | 返回结构（实测） | 状态 |
|---|---|---|
| `GET /elder/floor/list` | `R<List<Floor>>`：`data:[{id,name,code,...}]`（11 条） | ✅ 200 |
| `GET /elder/room/getRoomsByFloorId/{floorId}` | `R<List<RoomVo>>`：`data:[{id,floorName,floorId,code,price,roomId:null,bedVoList:[{id,bedNumber,bedStatus,roomId,ename,elderId}],deviceVos}]`（**无 typeName 字段**） | ✅ 200 |
| `GET /elder/roomType/listAll` | `{code,msg,data:[{id,name,bedCount,price,photo,status,...}]}`（6 条，全部 status=1） | ✅ 200 |
| `GET /elder/room/one/{roomId}` | 房间详情（入住配置回填用：floorName/floorId/roomId/code/price） | ✅ 200 |
| `GET /elder/floor/getRoomAndBedByBedStatus/{status}` | 3 级树（楼层→房间→床位），入住办理表单级联用 | ✅ 200 |
| `GET /elder/room/{id}` | 房间详情（编辑房间弹窗回填） | ✅（代码） |
| `POST/PUT/DELETE /elder/floor|room|bed` | 楼层/房间/床位增删改 | ✅（代码存在；本轮未做写操作） |

字段说明：
- `RoomVo`：`id, floorName, floorId, code, price(房型床位费), bedVoList[], deviceVos`；
- `BedVo`：`id, bedNumber, bedStatus(0 未入住/1 已入住/2 入住申请中), roomId, ename(入住老人姓名), elderId, userVos, deviceVos`；
- 注意：**`RoomVo` 没有 `typeName`**，所以页面房型标签为空、编辑房间时“房间类型”不回填（见 3.6 缺陷 D-41）。

### 3.4 布局规格（1280×720 与 1024×768，实测）

| 项 | 1280×720（侧栏展开） | 1024×768（侧栏展开） | 相对约束 |
|---|---|---|---|
| 内容块宽 | 1041（x=224..1265） | 785（x=224..1009） | `W − S − 24 − 15` |
| 楼层 Tab 行 | 1041×57 | 705×46（11 个 Tab 恰好放下） | 高固定 57；Tab 数多时需横向滚动（Web 未实现，见 D-42） |
| 图例行 | 1041×52 | 同 | padding-top 20 |
| 房间卡片 | **510.5×139**，2 列 | **382.5×139**，2 列 | `calc(50% - 20px)`，列间距 20，行距 20 |
| 床位卡 | **235.3×72**，2 列 | 约 174×72，2 列 | `calc(50% - 12px)`，列间距 12，行距 14 |
| 页面总高 | 1080（滚动 360） | 1080（滚动 312） | 内容高度由房间/床位数量决定 |
| 响应式断点 | 2 列 | 2 列 | `1605 ≤ W ≤ 2051` 时变 **3 列**（`calc(33.3% - 20px)`）；>2051 又回 2 列（Web 缺陷，见 D-43） |

### 3.5 截图

- `assets_s4/05_床位预览_1280x720.png`（首屏：楼层 Tab/图例/房间卡/床位卡）
- `assets_s4/06_床位预览_全页_1280.png`（整页 1280×1080）
- `assets_s4/07_床位预览_新增楼层弹窗_1280x720.png`
- `assets_s4/08_床位预览_新增房间弹窗_1280x720.png`
- `assets_s4/09_床位预览_编辑房间弹窗_1280x720.png`
- `assets_s4/10_床位预览_1024x768.png`（1024 两列布局）

### 3.6 M7 缺陷（C++ 端建议修正）

| 编号 | 缺陷 | 表现 | C++ 建议 |
|---|---|---|---|
| D-41 | `RoomVo` 缺 `typeName` | 房间卡片房型标签空白；编辑房间弹窗“房间类型”为空，保存会丢类型 | 本地 SQLite 关联 `room.type_name` 回填；卡片显示房型 |
| D-42 | 楼层 Tab 无横向滚动 | 楼层多/窗口窄时 Tab 会超出被裁（CSS 滚动条代码被注释） | Tab 区支持横向滚动或左右箭头 |
| D-43 | 3 列媒体查询区间异常 | 仅 1605–2051px 三列，2052px 以上回两列 | 改为 ≥1600 固定三列（或按内容宽自适应） |
| D-44 | “请假中”图例未实现 | 图例有“请假中”，但床位渲染只区分 ename 有/无；`icon-wcz` 未用于任何状态 | 明确 bed_status 与请假状态映射后实现，或去掉图例 |
| D-45 | 楼层 tab 编辑图标 hover 才出现 | 触摸/键盘不可达 | 常显或菜单化 |

---

## 4. M5/M6 后端缺口与 C++ 数据策略

- **后端缺失 Controller**：`NursingPlanController`、`NursingLevelController`、`NursingProjectController` 均不存在（`No static resource` 500），导致 M5/M6 列表/详情/CRUD/all 全部不可用；前端页面只能展示空表与空下拉。
- **C++ 端策略（建议）**：M5/M6 直接用 **SQLite 本地表 + 种子数据** 实现（结构见 §1.3/§2.3），交互与 UI 完全按 Web 复刻；不依赖后端。
- 依赖关系：M5 需要 `nursing_project`（9 条），M6 需要 `nursing_plan`（3 条）作为下拉 → 必须与 M5 一起本地化，否则表单下拉为空。

---

## 5. 数据模型：楼层/房间/床位/房型（M4 增强数据链）

### 5.1 表结构（实测字段）

| 表 | 字段 | 说明 |
|---|---|---|
| `floor` | `id, name, code, create_time, update_time, create_by, update_by, remark` | 楼层；`code` 为排序号（当前数据 1楼=11、9楼=8、10楼=9、12楼=1，序号混乱） |
| `room` | `id, code, sort, type_name, floor_id, is_deleted, create_time, update_time, create_by, update_by, remark` | 房间；`type_name` 冗余房型名称（→ room_type.name）；`floor_id → floor.id`；逻辑删除 is_deleted |
| `bed` | `id, bed_number, bed_status, sort, room_id, create_time, update_time, create_by, update_by, remark` | 床位；`bed_status` 0 未入住 / 1 已入住 / 2 入住申请中（另有 3 条 NULL）；`room_id → room.id` |
| `room_type` | `id, name, bed_count, price, introduction, photo, status, ...` | 房型；6 条（单人套房/双人套房/豪华单人间/豪华双人间/普通单人间/标准双人间） |

关联：
```
floor 1 ──< room (floor_id) ──< bed (room_id)
room.type_name ──► room_type.name（弱关联，非外键）
bed.id ◄── elder.bed_id；elder.id ◄── check_in.elder_id
```

### 5.2 种子数据（实测）

**楼层（11 条）**

| id | name | code | 房间数 |
|---|---|---|---|
| 1 | 1楼 | 11 | 9 |
| 2 | 2楼 | 2 | 7 |
| 3 | 3楼 | 3 | 7 |
| 4 | 4楼 | 4 | 7 |
| 5 | 5楼 | 5 | 7 |
| 6 | 6楼 | 6 | 7 |
| 7 | 7楼 | 7 | 7 |
| 8 | 8楼 | 8 | 7 |
| 391 | 9楼 | 8 | **0** |
| 401 | 10楼 | 9 | **0** |
| 406 | 12楼 | 1 | 2 |

**房间（60 条）**：1楼 101~109（9 间）；2~8 楼各 201~207 …801~807（每层 7 间，共 49 间）；12楼 1201、102-2（2 间）。房型分布：豪华单人间 10、普通单人间 9、豪华双人间 8、双人套房 9、单人套房 9（合计 45，均可匹配 room_type）；**特护房 7 + 普通双人间 8（合计 15）不在 room_type 表中**（房型下拉选不到，列表 join 为 NULL，见风险 R4）。

**床位（89 条）**：已入住 5（101-1 老李头儿、101-2 老李、104-1 张三、104-2 李天龙、403-1 魏忠语）；空闲 81；bed_status 为 NULL 3 条（108-2、109-2、1201-1，需清洗）。1楼现有 17 张床位（9 间房）。

### 5.3 与入住办理（M4）的对应关系

| check_in | elder_id | bed_id/bed_number | room_id / code | floor | 房型 |
|---|---|---|---|---|---|
| 1 张三 | 325 | 5 / 104-1 | 4 / 104 | 1楼 | 豪华双人间（4500） |
| 2 李天龙 | 326 | 6 / 104-2 | 4 / 104 | 1楼 | 豪华双人间（4500） |
| 3 老李 | 327 | 170 / 101-2 | 1 / 101 | 1楼 | 豪华单人间（3000） |
| 4 老李头儿 | 328 | 1 / 101-1 | 1 / 101 | 1楼 | 豪华单人间（3000） |
| 5 魏忠语 | 329 | 36 / 403-1 | 24 / 403 | 4楼 | 普通单人间（2000） |

### 5.4 入住表单「床位级联」规格（M4 增强）

- API：`GET /elder/floor/getRoomAndBedByBedStatus/0`（参数 0 = 只取未入住床位；入住办理页传 0）。
- 返回 3 级树（实测）：
  ```json
  { "code":200, "data":[
    { "value":"1", "label":"1楼", "children":[
      { "value":"2", "label":"102", "children":[
        { "value":"3", "label":"102-2", "children":null },
        { "value":"2", "label":"102-1", "children":null } ]} ]} ] }
  ```
- 控件：`el-cascader`（单值数组 `[floorId, roomId, bedId]`，宽度 100%、高 32）。
- 选择后取值：`bedId = val[2]`；再调 `GET /elder/room/one/{roomId}` 回填 `floorName/floorId/roomId/code/price → bedFee`。
- **只显示未入住床位**（status=0 过滤），这与床位预览页显示全部状态互补；C++ 端实现时：级联数据源 = room + bed where bed_status=0；床位预览 = 全状态。
- 提交时 `bedId` 落库并更新 `bed.bed_status=1`（后端 apply 逻辑）。

---

## 6. 上传控件规格（图片 / PDF）

### 6.1 图片上传（房型照片、护理项目图片）——通用 `ImageUpload` 组件

| 项 | 规格（源码默认） |
|---|---|
| 组件 | `src/components/ImageUpload/index.vue`（`el-upload`，`list-type="picture-card"`） |
| 上传框 | **148×148** 方块（picture-card），虚线边框 #CDD0D6、底色 #FAFAFA，中央 + 号；有图后显示缩略图 + 覆盖删除/预览 |
| 数量 | 默认 `limit=5`（房型/护理项目实际单图场景建议 limit=1，C++ 建议明确） |
| 格式 | 默认 `['png','jpg','jpeg']`（before-upload 校验） |
| 大小 | 默认 **5MB**（`fileSize=5`，超出提示并阻止） |
| 上传地址 | `/common/upload`（开发代理为 `/dev-api/common/upload`），携带 `Authorization: Bearer <token>` |
| 成功回填 | `res.url` 写入表单字段（photo/image） |
| 预览 | 点击缩略图打开 800px 宽预览弹窗（黑底遮罩，图片居中最大宽 100%） |
| 提示文案 | “请上传大小不超过 5MB 格式为 png/jpg/jpeg 的文件” |
| Web 实测 | 房型弹窗上传框 148×148、边框 1px dashed #CDD0D6、底色 #FAFAFA ✓ |

### 6.2 入住表单图片（一寸照片/身份证正反面）

| 项 | 规格 |
|---|---|
| 控件 | `el-upload list-type="picture"`（缩略图列表式，非 picture-card） |
| 上传按钮 | “点击上传”小按钮 **88×24**（主色，图标 upload） |
| 限制 | 仅大小 **<2MB**（before-upload 校验，超出 ElMessage 报“文件大小超过 2MB”）；未限制格式（建议 C++ 限定 jpg/png） |
| 地址 | `/dev-api/common/upload` + Bearer |
| 回填 | `res.url` → `image / idCardPortraitImg / idCardNationalEmblemImg` |
| 只读态 | 三张图用 `el-image` 显示 **100×100**；无图显示“加载失败” |

### 6.3 PDF 上传（入住合同）

| 项 | 规格 |
|---|---|
| 控件 | `el-upload` 普通列表式，`accept=".pdf"` |
| 按钮 | “点击上传”88×24 主色 + 提示“只能上传不超过 2MB 的文件”（12px 灰） |
| 限制 | ≤2MB（before-upload）+ 仅 PDF（accept） |
| 回填 | `res.url` → `agreementPath` |
| 只读态 | 显示“{老人姓名}的入住合同.pdf” + “查看”链接（`#409EFF`，新窗口打开 agreementPath） |
| 后端 | `POST /common/upload`（RuoYi CommonController；multipart 单文件 ≤10MB、总 ≤20MB，保存到 `C:/ruoyi/uploadPath`） |

### 6.4 C++ 端实现建议（WIC / 文件对话框）

- 图片：Win32 `IFileOpenDialog` 选文件 → 校验扩展名/大小 → 拷贝到本地 `assets/uploads/`（或 SQLite BLOB）→ WIC 解码生成纹理；148×148 表单预览用 contain（留白 #FAFAFA），列表缩略 cover。
- PDF：选文件校验 ≤2MB → 拷贝到本地 `assets/contracts/` → 记录路径；只读展示文件名 + “打开”调用 `ShellExecute`（默认 PDF 阅读器）。
- 上传进度/失败提示：复用消息提示组件（成功绿/失败红，顶部居中）。

---

## 7. 风险与待确认（逐项标注）

| # | 风险 | 影响页面 | 结论 |
|---|---|---|---|
| R1 | `NursingPlanController` 缺失（5 个接口全 404） | M5 护理计划 | Web 仅空壳；C++ 必须本地化（SQLite） |
| R2 | `NursingLevelController` 缺失（5 个接口全 404） | M6 护理等级 | 同上 |
| R3 | `NursingProjectController` 缺失（`/nursing/project/all` 404） | M5 计划弹窗项目下拉、护理项目页 | 项目数据只能本地种子（9 条） |
| R4 | 15 间房（特护房 7 + 普通双人间 8）的 type_name 不在 room_type | M7 房型显示/编辑 | C++ 种子可补 2 种房型或映射到现有 6 种（需 lead 定） |
| R5 | `RoomVo` 缺 typeName | M7 房型标签空、编辑房型丢失 | C++ 本地 join 修正 |
| R6 | bed_status 有 3 条 NULL；楼层 9/10 楼无房间；floor.code 重复混乱 | M7 | 种子数据清洗；C++ 不依赖 code 排序时按 id/名称 |
| R7 | “请假中”无实现、3 列媒体查询区间异常、Tab 无横向滚动 | M7 | 建议按 D-41~45 修正 |
| R8 | M5/M6 后端修复成本高（要新建 Controller+Service+Mapper+XML） | 排期 | 若 Sprint 4 只做 C++ 转译，建议维持“本地化”；若要 Web 可用需后端排期 |
| R9 | 上传未做真实写盘验证（本轮只读） | 上传功能 | `/common/upload` 代码存在（RuoYi 默认），C++ 端本地化上传更简单 |

---

## 8. 附录 A：本轮不做（Sprint 5 候选）

| 页面 | 现状（v0.2 实测） | 复杂度 | 备注 |
|---|---|---|---|
| 负责老人 `/nursing/oldPeople` | ✅ 可用（楼层 Tab + 房间/床位卡 + 批量设置护理员） | 中高 | 依赖 `/elder/floor/getAllFloorsWithNur`、`/system/user/listByDeptId`、`/elder/nursingElder/setNursing` |
| 来访管理 `/appointment/reservation` | ⚠️ UI 完整、列表空 | 低 | 后端 `ReservationController` 缺失 + `reservation` 表缺失 |
| 护理项目 `/nursing/project` | ⚠️ UI 完整、API 缺失 | 低 | 12 张服务项目图标素材落地页（Sprint2 已给规格） |

## 9. 附录 B：截图索引（`docs/architecture/assets_s4/`）

| 文件 | 说明 |
|---|---|
| `01_护理计划列表_1280x720.png` | M5 列表（空表 + toast） |
| `02_护理计划_新增弹窗_1280x720.png` | M5 新增弹窗（1000 宽，含护理项目子表） |
| `03_护理等级列表_1280x720.png` | M6 列表（空表 + toast） |
| `04_护理等级_新增弹窗_1280x720.png` | M6 新增弹窗（500 宽，5 字段） |
| `05_床位预览_1280x720.png` | M7 首屏（楼层 Tab/图例/房间卡/床位卡） |
| `06_床位预览_全页_1280.png` | M7 整页（1280×1080） |
| `07_床位预览_新增楼层弹窗_1280x720.png` | 楼层弹窗（628×276） |
| `08_床位预览_新增房间弹窗_1280x720.png` | 房间弹窗（628×336） |
| `09_床位预览_编辑房间弹窗_1280x720.png` | 房间编辑（回填：房间号/房型/排序） |
| `10_床位预览_1024x768.png` | 1024 布局（两列卡片，宽 382.5） |

## 10. 附录 C：C++ 端本地种子数据建议

- `nursing_plan`：3 条（§1.4）
- `nursing_project_plan`：6 条（§1.4）
- `nursing_project`：9 条（§1.4，含 1 条禁用“洗头”）
- `nursing_level`：3 条（§2.3：1500/2000/3000，plan_id 133/134/135）
- `floor`：11 条（§5.2，9/10 楼无房间可保留）
- `room`：60 条（§5.2；建议补 type_name 映射，特护房/普通双人间二选一处理）
- `bed`：89 条（§5.2；3 条 NULL 清洗为 0 空闲）
- `room_type`：6 条（单人套房/双人套房/豪华单人间/豪华双人间/普通单人间/标准双人间 + 照片素材，见 Sprint3 文档 §6）
