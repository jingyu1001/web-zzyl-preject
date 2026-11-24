# Sprint 3 · M4 入住办理 + 房型图片 规格 v0.1

> 项目：中州养老（Web 综合端 → C++ 转译端）
> 范围：**M4 入退管理-入住办理（列表 / 办理入住表单 / 详情）** + **在住管理-房型设置图片正式接入**
> 来源：前端 `src/views/nursing/checkIn/**`、`src/api/nursing/*`；后端 `CheckInController/CheckInServiceImpl/CheckInMapper.xml`、`CheckIn*.java`、`Floor/Room/Bed/RoomType Controller`；MySQL `zzyl` 库只读查询；运行态实测（`localhost:88` + CDP，admin 登录）
> 方式：**只读**（未改代码/数据库/服务/git）；截图见 `docs/architecture/assets_s3/`（6 张）
> 时间：2026-11-24｜基线：v0.2 通用组件规格（颜色/字号/表格/弹窗沿用，本文件只写增量）
> 结论先说：**列表页可用；办理表单 UI 完整但护理等级接口缺失（无法完成提交）；详情页 read 模式后端接口缺失（当前不可用）** —— C++ 端需按本文“本地数据化”方案实现。

---

## 1. 列表页（`/enterQuit/checkIn`）

### 1.1 页面结构（1280×720，侧栏展开）

| 区域 | 基准尺寸/位置 | 说明 |
|---|---|---|
| 内容容器 | x=200..1280，y=84 起；padding 20 → 内容 x=220..1260（宽 1040） | 页面纵向滚动 |
| 搜索栏 | 表单项 2 个 + 按钮；y=104，高 60 | inline 表单，label 68 + 输入 192 |
| 工具栏 | y≈184，高 32 | 仅“发起入住申请”（136×32，浅青底主色字，图标 Plus） |
| 数据表格 | 表头 40；数据行 **63**（本页因状态标签/两行内容撑高） | 9 列，无固定列宽（除 2 列 180） |
| 分页 | y=612，高 32，右对齐 | “共 5 条　10条/页　1　前往 页” |
| 页面总高 | ≈660（720 内可见，无需滚动） | — |

### 1.2 搜索项

| 字段 | 控件 | 宽度基准 | 说明 |
|---|---|---|---|
| 老人姓名 `elderName` | 输入框 | label 68 + 输入 192（控件高 32） | placeholder“请输入老人姓名”；后端 `like` 模糊 |
| 身份证号 `idCardNo` | 输入框 | 同上 | placeholder“请输入身份证号”；后端 `=` 精确 |
| 按钮 | 搜索（80×32 主色）/ 重置（80×32 白底） | — | 回车触发搜索 |

### 1.3 工具栏

| 按钮 | 尺寸/样式 | 行为 | 权限 |
|---|---|---|---|
| 发起入住申请 | **136×32**，圆角 4，底 #E5F7F5、字 #00B8A0、图标 + | 跳转 `/enterQuit/checkInInfo`（新建模式） | `nursing:checkIn:add` |

> Web 未提供“修改/删除/导出”入口（虽然后端有 export/update/delete 接口与权限，但页面未挂按钮）。

### 1.4 表格列（字段名 + 含义 + 宽度基准）

| # | 列名 | 字段 | 含义 | 宽度基准（实测，自动分配） | 对齐 |
|---|---|---|---|---|---|
| 1 | 序号 | （index） | 行号 | 48 | 居中 |
| 2 | 老人姓名 | `elderName` | 老人姓名 | 107 | 居中 |
| 3 | 身份证号 | `idCardNo` | 身份证号 | 105 | 居中 |
| 4 | 入住床位 | `bedNumber` | 床位编号（如 104-1） | 105 | 居中 |
| 5 | 护理等级名称 | `nursingLevelName` | 护理等级名称（冗余字段） | 105 | 居中 |
| 6 | 入住期限 | `startDate` ~ `endDate` | 起止日期，格式 `YYYY-MM-DD ~ YYYY-MM-DD` | **180** | 居中 |
| 7 | 状态 | `status` | 0=已入住（绿标签）/ 1=已退住（红标签） | 105 | 居中 |
| 8 | 创建时间 | `createTime` | `YYYY-MM-DD HH:mm:ss` | **180** | 居中 |
| 9 | 操作 | — | “查看”（文字按钮 34×20，主色） | 105 | 居中 |

**状态枚举**：`0 已入住`（标签底 #F0F9EB、字 #67C23A、高 24、圆角 4）；`1 已退住`（标签 `type=danger`：底 #FEF0F0、字 #F56C6C）。
**行高说明**：本页实测 63px（普通列表 49px），因为状态标签 24 + 单元格内边距；C++ 端可按 56~64 自适应行高，或固定 60。

### 1.5 操作列动作

| 动作 | 触发 | 逻辑 | 现状 |
|---|---|---|---|
| 查看 | 行内“查看”按钮 | `router.push('/enterQuit/checkInInfo?id={id}&type=read')` | ⚠️ 详情页 read 模式后端缺接口（见 §3.4） |

> 列表页无批量勾选、无删除/修改/导出按钮。

### 1.6 分页

- 高 32，右对齐；文案“共 5 条　10条/页　页码　前往 1 页”；
- 请求参数 `pageNum`、`pageSize`（默认 1/10）；
- 排序：无显式排序，按后端默认（主键升序）。

### 1.7 5 条真实数据（2026-11-24 实测，来自 `check_in` 表）

| id | 老人姓名 | 身份证号 | 入住床位 | 护理等级名称 | 入住期限 | 状态 | 创建时间 |
|---|---|---|---|---|---|---|---|
| 1 | 张三 | 132123196712131234 | 104-1 | 测试护理等级 | 2024-08-27 ~ 2024-09-30 | 已入住 | 2024-08-27 16:43:20 |
| 2 | 李天龙 | 132123196712131239 | 104-2 | 1号护理计划 | 2024-08-27 ~ 2024-09-30 | 已入住 | 2024-08-27 16:50:09 |
| 3 | 老李 | 132123195612132345 | 101-2 | 2号护理等级 | 2024-09-12 ~ 2024-10-31 | 已入住 | 2024-09-12 18:51:36 |
| 4 | 老李头儿 | 410725196904056698 | 101-1 | 2号护理等级 | 2024-09-12 ~ 2024-10-31 | 已入住 | 2024-09-12 19:10:23 |
| 5 | 魏忠语 | 210102196703292842 | 403-1 | 2号护理等级 | 2026-03-05 ~ 2026-04-30 | 已入住 | 2026-03-05 20:49:06 |

- 统计：总数 5，已入住 5，已退住 0。
- 数据质量提示：①`check_in.nursing_level_name` 为历史冗余值（“测试护理等级/1号护理计划/2号护理等级”），与 `nursing_level` 表（1/2/3号护理等级）名称不完全一致；②`elder` 表 id=325（张三）的 `name` 字段实际存的是电话号“13211223322”（脏数据）。

---

## 2. 办理入住表单（`/enterQuit/checkInInfo`，新建模式）

### 2.1 页面结构（单页长表单，非弹窗）

```
AppMain 内容区（x=200..1280，宽 1080）
└── <el-form label-width=100px>
    ├── 基本信息   （section 高 431）
    ├── 家属信息   （section 高 158）
    ├── 入住配置   （section 高 358）
    ├── 签约办理   （section 高 249）
    └── 提交按钮   （section 高 72，居中）
总高 1352（1280×720 下需纵向滚动约 632）
```

- 每个区块：`padding: 20px`；区块标题 `h3`：字号 **16.38px**、500、颜色 #000、margin 0；标题下直接跟表单行（无卡片边框/分隔线）。
- 表单行：`el-row > el-col :span=12` → **两列 50%/50%**（Web 无响应式堆叠，窄窗口仍 50%）。
- 每项：label 右对齐 **宽 100px**；控件行高 **32**；控件宽度铺满列宽（侧栏展开时约 412px，实测侧栏收起时 485.5px）。
- 控件间距：行与行垂直间隔约 28~34（由表单项 margin 决定，实测紧凑）。

### 2.2 字段分组与校验

#### A. 基本信息（10 项）

| # | 字段 | 控件 | 必填 | 校验提示 | 备注 |
|---|---|---|---|---|---|
| 1 | 老人姓名 `name` | 输入 | ✅ | 请输入老人姓名 | — |
| 2 | 身份证号 `idCardNo` | 输入 | ✅ | 请输入身份证号 | — |
| 3 | 出生日期 `birthday` | 日期选择（YYYY-MM-DD） | ✅ | 请选择出生日期 | — |
| 4 | 年龄 `age` | 输入（数字文本） | ✅ | 请输入年龄 | — |
| 5 | 性别 `sex` | 单选 男(1)/女(0) | ✅ | 性别不能为空 | 默认 0 |
| 6 | 联系方式 `phone` | 输入 | ✅ | 请输入联系方式 | — |
| 7 | 家庭住址 `address` | 文本域（固定 4 行） | ✅ | 请输入家庭住址 | 高约 94 |
| 8 | 一寸照片 `image` | 上传按钮（“点击上传”，88×24 主色小按钮） | — | — | 单张图片，≤2MB |
| 9 | 身份证人像面 `idCardPortraitImg` | 上传按钮 | — | — | 同上 |
| 10 | 身份证国徽面 `idCardNationalEmblemImg` | 上传按钮 | — | — | 同上 |

#### B. 家属信息（可增删行，无独立表）

| 列 | 字段 | 控件 | 说明 |
|---|---|---|---|
| 家属姓名 | `name` | 输入（宽 192） | placeholder“请输入家属姓名” |
| 家属联系方式 | `phone` | 输入（宽 192） | placeholder“请输入家属联系方式” |
| 与老人关系 | `kinship` | 下拉（宽 200） | 选项：0 子女 / 1 配偶 / 2 亲属 / 3 朋友 / 4 社工 / 5 其他 |
| 操作 | — | “+”（新增行）、“−”（删除行，至少保留 1 行） | 按钮 32×32 白底 |

- 表头：高 **50**、背景 **#F3F8F9**、14px，四列横排（左右 padding 20，列间 justify-between）；行高 50。
- 校验：家属信息**无必填校验**（前端未配置）。
- 存储：后端把家属列表 JSON 序列化后写入 `check_in.remark`（无独立家属表）。

#### C. 入住配置（10 项）

| # | 字段 | 控件 | 必填 | 说明/取值来源 |
|---|---|---|---|---|
| 1 | 入住期限 `checkInTime`→`startDate/endDate` | 日期时间范围 | ✅ | 开始/结束（YYYY-MM-DD HH:mm:ss），区间选择器 |
| 2 | 费用期限 `feeTime`→`feeStartDate/feeEndDate` | 日期时间范围 | ✅ | 同上 |
| 3 | 护理等级 `nursingLevelId` | 下拉 | ✅ | 来源 `GET /nursing/level/all`（**当前 404**）；选中后自动回填护理费用 `nursingFee` 与名称 `nursingLevelName` |
| 4 | 入住床位 `bed` | 级联选择（楼层→房间→床位） | ✅ | 来源 `GET /elder/floor/getRoomAndBedByBedStatus/0`（✅ 可用）；选中后取 `GET /elder/room/one/{roomId}` 回填楼层名/楼层ID/房间ID/床位费用/房号 |
| 5 | 押金 `deposit` | 输入 | ✅ | 默认 **3000**（提交时硬编码 3000） |
| 6 | 护理费用 `nursingFee` | 数字输入（禁用） | ✅ | 由护理等级自动带出（元/月） |
| 7 | 床位费用 `bedFee` | 数字输入（禁用） | ✅ | 由房型价格自动带出（元/月） |
| 8 | 其他费用 `otherFees` | 数字输入 | ✅ | 默认 0 |
| 9 | 医保支付 `insurancePayment` | 数字输入 | ✅ | 默认 0 |
| 10 | 政府补贴 `governmentSubsidy` | 数字输入 | ✅ | 默认 0 |

#### D. 签约办理（5 项）

| # | 字段 | 控件 | 必填 | 说明 |
|---|---|---|---|---|
| 1 | 合同名称 `contractName` | 输入 | ✅ | 请输入合同名称 |
| 2 | 签约日期 `signDate` | 日期时间 | ✅ | 请选择签约日期 |
| 3 | 丙方姓名 `thirdPartyName` | 输入 | — | — |
| 4 | 丙方联系方式 `thirdPartyPhone` | 输入 | — | — |
| 5 | 入住合同 `agreementPath` | 上传（PDF，≤2MB） | — | 按钮“点击上传”+ 提示“只能上传不超过 2MB 的文件” |

### 2.3 提交行为

1. 前端合并三个子表单 `rules`（基本信息 + 入住配置 + 签约办理）做统一校验；不通过则终止。
2. 组装 `CheckInApplyDto`：
   - `checkInElderDto`（10 字段）
   - `elderFamilyDtoList`（家属数组）
   - `checkInConfigDto`（含 startDate/endDate/feeStartDate/feeEndDate/nursingLevelId/nursingLevelName/bedId/bedFee/code/floorId/floorName/governmentSubsidy/insurancePayment/nursingFee/otherFees/roomId/**deposit 固定 3000**）
   - `checkInContractDto`（contractName/signDate/thirdPartyName/thirdPartyPhone/agreementPath）
3. `POST /nursing/checkIn/apply`（无权限注解，登录即可）。
4. 成功：`ElMessage.success('提交成功')` → 跳回列表 `/enterQuit/checkIn`；失败：`ElMessage.error('提交失败')`（接口层 500 时还会由请求拦截器弹顶部红色错误提示）。
5. 后端事务（`@Transactional`）顺序：
   ① 按身份证查老人，若 `elder.status ∈ (1,4)` → 抛异常“该老人已入住”；
   ② 更新 `bed.bed_status = 1`（已入住）；
   ③ 新增/更新 `elder`（`status=1`、回填 `bed_id/bed_number`）；
   ④ 新增 `contract`（编号 `HT` + 生成码，`status` 由开始日期是否未来决定 0/1，写入 elderId/elderName/起止日期）；
   ⑤ 新增 `check_in`（status=0，`remark` 存家属 JSON）；
   ⑥ 新增 `check_in_config`。
6. **当前实测问题**：护理等级下拉接口 404 → 无法选择等级，表单无法通过校验/提交；床位级联接口正常。

---

## 3. 详情页（`/enterQuit/checkInInfo?id={id}&type=read`）

### 3.1 分区结构（与办理表单同一套组件，只读模式）

| 区块 | 内容 | 只读态表现 |
|---|---|---|
| 基本信息 | 老人姓名、身份证号、出生日期、年龄、性别、联系方式、家庭住址、一寸照片、身份证人像面、身份证国徽面 | 输入框 `readonly`；三张图片改为 `el-image` 100×100 展示 |
| 家属信息 | 家属姓名 / 联系方式 / 与老人关系 / 操作 | 输入框/下拉禁用；+/− 按钮仅展示不可用 |
| 入住配置 | 入住期限、费用期限、护理等级、入住床位、押金、护理费用、床位费用、其他费用、医保支付、政府补贴 | 日期框只读；护理等级下拉禁用；**入住床位显示文本**（床号，不用级联）；费用输入禁用 |
| 签约办理 | 合同名称、签约日期、丙方姓名、丙方联系方式、入住合同 | 输入只读；入住合同显示“{老人姓名}的入住合同.pdf [查看]”（链接） |
| 提交 | 底部居中“提交”按钮 | Web 详情模式**仍显示提交按钮**（设计缺陷，只读模式应隐藏） |

### 3.2 布局比例（1280×720）

- 与办理表单完全一致：内容宽 = `W − S − 40`（S=200 侧栏展开；折叠 54）；两列各 50%，label 100，控件 32 高；区块 padding 20；标题 16.38px。
- 自适应：Web 未见响应式断点（el-col 固定 span=12，窄窗口仍两列）；建议 C++ 端在内容宽 < 900 时切换单列（两列 → 一列，控件整行）。
- 只读/可编辑切换由 `type` 参数控制：`type=read` → 只读；无 `type` → 新建可编辑。

### 3.3 详情数据来源（前端期望）

```js
GET /nursing/checkIn/detail/{id}
→ { data: {
      checkInElderVo,        // 基本信息（老人）
      elderFamilyVoList,     // 家属数组
      checkInConfigVo,       // 入住配置
      contract               // 合同
   } }
```

### 3.4 实测结论（重要）

- **后端不存在** `GET /nursing/checkIn/detail/{id}` 映射（`CheckInController` 只有 list/export/{id}/add/edit/delete/apply）。
- 实测调用返回 `{"code":500,"msg":"No static resource nursing/checkIn/detail/1."}`；
- 页面 fallback 显示空表 + 图片“加载失败”+ 顶部两条错误提示（`nursing/level/all` 与 `nursing/checkIn/detail/1`）。
- ⇒ **Web 详情页 read 模式当前不可用**；C++ 端应基于本地 SQLite 数据自行组装上述 4 块视图模型（结构见 §4.3）。

---

## 4. 数据模型

### 4.1 `check_in` 表（入住主表，实测 5 行）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK | 主键 |
| elder_name | varchar(50) | 老人姓名（冗余） |
| elder_id | bigint | 老人ID（→ elder.id） |
| id_card_no | char(18) | 身份证号（冗余） |
| start_date | date | 入住开始 |
| end_date | date | 入住结束 |
| nursing_level_name | varchar(50) | 护理等级名称（冗余） |
| bed_number | varchar(50) | 入住床位号（冗余） |
| status | tinyint | 0 已入住 / 1 已退住 |
| sort_order | int | 排序 |
| create_time / update_time | timestamp | 创建/修改时间 |
| create_by / update_by | varchar(50) | 创建/修改人 |
| remark | varchar(255) | **家属信息 JSON**：`[{"kinship":"0","name":"…","phone":"…"}]` |

### 4.2 `check_in_config` 表（入住配置，实测仅 1 行，属 check_in_id=5）

| 字段 | 说明 | 实测样例（id=1） |
|---|---|---|
| id | 主键 | 1 |
| check_in_id | → check_in.id | 5 |
| nursing_level_id | → nursing_level.id | 77 |
| nursing_level_name | 等级名称 | 2号护理等级 |
| fee_start_date / fee_end_date | 费用起止 | 2026-03-05 / 2026-04-30 |
| deposit | 押金（元） | 3000.00 |
| nursing_fee | 护理费（元/月） | 2000.00 |
| bed_fee | 床位费（元/月） | 2000.00 |
| insurance_payment | 医保支付 | 0.00 |
| government_subsidy | 政府补贴 | 0.00 |
| other_fees | 其他费用 | 0.00 |
| sort_order / 审计字段 | — | — |

### 4.3 关联关系与查询方式

```
check_in.elder_id ──► elder.id ──► elder.bed_id ──► bed.id ──► bed.room_id ──► room.id ──► room.floor_id ──► floor.id
                                                          └► room.type_name ──► room_type.name
check_in_config.nursing_level_id ──► nursing_level.id（fee 为护理费）
check_in_config.check_in_id ──► check_in.id
contract.elder_id ──► elder.id（合同名称/编号/签约日期/PDF路径/双方信息/起止）
check_in.remark ──► 家属 JSON 数组（无独立表）
```

实测关联（5 条入住 → 老人 → 床位 → 房间 → 楼层/房型）：

| check_in.id | elder.id | bed.id / bed_number | room.code | floor.name | room_type.name / price |
|---|---|---|---|---|---|
| 1 | 325 | 5 / 104-1 | 104 | 1楼 | 豪华双人间 / 4500 |
| 2 | 326 | 6 / 104-2 | 104 | 1楼 | 豪华双人间 / 4500 |
| 3 | 327 | 170 / 101-2 | 101 | 1楼 | 豪华单人间 / 3000 |
| 4 | 328 | 1 / 101-1 | 101 | 1楼 | 豪华单人间 / 3000 |
| 5 | 329 | 36 / 403-1 | 403 | 4楼 | 普通单人间 / 2000 |

`nursing_level`（3 条）：76 = 1号护理等级 / 1500 元；77 = 2号护理等级 / 2000 元；78 = 3号护理等级 / 3000 元（均为启用）。
`contract`（1 条）：elder_id=329 魏忠语，合同号 `HT202603052049060001`，状态 1（已生效），签约 2026-03-05，起止 2026-03-05~2026-04-30。
`bed`：`bed_status` 0 未入住 / 1 已入住 / 2 入住申请中；`room`：code/type_name/floor_id；`floor`：name/code。

### 4.4 接口清单（实测标注）

| # | 方法/路径 | 用途 | 状态 | 返回结构（来源） |
|---|---|---|---|---|
| 1 | `GET /nursing/checkIn/list?pageNum&pageSize&elderName&idCardNo` | 列表 | ✅ 200 | `{total, rows[], code, msg}`；rows 字段同 §4.1（实测） |
| 2 | `GET /nursing/checkIn/{id}` | 单条 | ✅（代码存在，未被前端使用） | `R<CheckIn>` |
| 3 | `POST /nursing/checkIn` | 新增 | ✅（代码） | `AjaxResult` |
| 4 | `PUT /nursing/checkIn` | 修改 | ✅（代码） | `AjaxResult` |
| 5 | `DELETE /nursing/checkIn/{ids}` | 删除 | ✅（代码） | `AjaxResult` |
| 6 | `POST /nursing/checkIn/export` | 导出 | ✅（代码，页面无入口） | Excel |
| 7 | `POST /nursing/checkIn/apply` | 办理入住（事务） | ✅ 200（代码） | `{code:200,msg:"操作成功"}` |
| 8 | `GET /nursing/checkIn/detail/{id}` | 详情聚合 | ❌ 500 `No static resource` | 前端期望 4 块（§3.3） |
| 9 | `GET /nursing/level/all` | 护理等级下拉 | ❌ 500 `No static resource`（缺 Controller） | 前端期望 `{data:[NursingLevel]}` |
| 10 | `GET /elder/floor/getRoomAndBedByBedStatus/{status}` | 床位级联 | ✅ 200 | `{data:[{value,label,children:[{value,label,children:[bed]}]}]}`（实测） |
| 11 | `GET /elder/room/one/{roomId}` | 房间信息回填 | ✅ 200 | `{data:{floorName,floorId,roomId,code,price,bedVoList,deviceVos}}`（实测） |
| 12 | `POST /common/upload` | 图片/PDF 上传 | ✅（代码） | `{url,...}`，前端用 `res.url`；大小限制 2MB |
| 13 | `GET /elder/floor/list` | 楼层列表 | ✅ 200 | `R<List<Floor>>`（实测） |
| 14 | `GET /elder/roomType/list` | 房型列表 | ✅ 200 | `{total, rows[]}`（实测，6 条） |

> 权限：列表 `nursing:checkIn:list`、查看 `elder:checkIn:edit`（页面按钮 v-hasPermi）、申请 `nursing:checkIn:add`；apply 接口本身无注解。

---

## 5. C++ 复刻 UI 规格（M4）

> 颜色/字号/通用组件沿用 v0.2 §2；本节约 1280×720 基准，布局以相对约束表达。

### 5.1 列表页

| 组件 | 规格 |
|---|---|
| 页面容器 | 内容区 x=S..W，y=84..H；padding 20；可用宽 `A = W − S − 40` |
| 搜索栏 | 2 项：老人姓名 / 身份证号；label 68 右对齐 + 输入 192×32；搜索 80×32 主色、重置 80×32 白底；行高 60 |
| 工具栏 | “发起入住申请” 136×32，浅青底 #E5F7F5、主色字 #00B8A0、圆角 4、+ 图标 |
| 表格 | 表头 40（#F8F8F9/13px/#515A6E/600）；行高建议 **60**（Web 实测 63）；9 列：序号48 / 老人姓名107 / 身份证号105 / 入住床位105 / 护理等级105 / 入住期限180 / 状态105 / 创建时间180 / 操作105（按 A=1040 基准，总宽 > A 时横向滚动或按比例压缩） |
| 状态标签 | 已入住：24 高、圆角 4、底 #F0F9EB、字 #67C23A；已退住：底 #FEF0F0、字 #F56C6C |
| 操作列 | “查看”文字按钮（14px 主色 #00B8A0，34×20） |
| 分页 | 高 32、右对齐、“共 N 条 / 10 条/页 / 页码 / 前往 N 页”；当前页主色 32×32 圆角 2 |
| 空态/加载 | “暂无数据”居中；loading 遮罩 |
| 响应式 | 窗口变窄：搜索项换行、表格横向滚动；`W < 1200` 时列宽按比例压缩，可选隐藏“护理等级名称/创建时间”两列（C++ 建议提供横向滚动而非隐藏） |

### 5.2 办理入住表单页

| 组件 | 规格 |
|---|---|
| 容器 | 单页纵向滚动长表单，非弹窗；内容宽 = A；四个分区，每区 padding 20 |
| 分区标题 | 16.38px（取 16px）、weight 500、颜色 #000000；标题与内容无分隔线（C++ 可加 1px #EBEEF5 分隔线提升可读性，列为差异 D-30） |
| 两列栅格 | 每行 2 列各 50%；item 高 32；label 100 右对齐；控件铺满列宽；行间距建议 28 |
| 基本信息 | 10 字段（§2.2A），两列排布（老人姓名/身份证号、出生日期/年龄、性别/联系方式、家庭住址/一寸照片、身份证人像面/国徽面）；图片上传按钮 88×24 主色小按钮（“点击上传”），上传后显示缩略图（建议 100×100） |
| 家属信息 | 自绘表格：表头高 50、背景 #F3F8F9、14px；4 列（姓名输入 192、电话输入 192、关系下拉 200、操作 +/- 32×32）；行高 50；最少 1 行 |
| 入住配置 | 10 字段两列；日期时间范围控件（双输入 + “至” + 日历图标，高 32）；护理等级下拉 32；入住床位级联下拉 32（楼层→房间→床位三层，选择后回填房费）；押金输入默认 3000；护理费/床位费为**禁用只读数字框**（自动带出）；其余费用数字框 |
| 签约办理 | 5 字段：合同名称、签约日期（时间）、丙方姓名、丙方电话、PDF 上传（按钮 88×24 + 提示 12px 灰）；只允许 PDF、≤2MB |
| 提交 | 页面底部居中，主色按钮 60×32；提交前校验三组规则；成功 toast“提交成功”并返回列表，失败 toast 错误信息 |
| 响应式 | 建议：内容宽 < 900 时两列改单列（Web 不做，列为差异 D-31）；1280 基准保持两列 |

**校验清单（C++ 必做）**：基本信息 10 项中 7 项必填（见 §2.2A）；入住配置 10 项全部必填（日期范围、护理等级、床位、押金、三个费用、其他费用、医保、补贴）；签约办理 2 项必填（合同名称、签约日期）；家属无必填；非法提交逐项红字提示 + 控件边框红。

### 5.3 详情页（只读）

| 组件 | 规格 |
|---|---|
| 布局 | 与表单同一套组件；`readonly` 模式：输入只读（灰底/无边框可选）、下拉禁用、日期只读 |
| 图片 | 一寸照片/身份证正反面：100×100 展示；点击可预览大图（建议） |
| 入住床位 | 只读文本显示床号（如“104-1”），不显示级联控件 |
| 合同 | 只读文本“{老人姓名}的入住合同.pdf + 查看链接”（无 PDF 时显示“--”） |
| 按钮 | **只读时隐藏“提交”按钮**（Web 未隐藏，列为差异 D-32） |
| 数据 | 从本地 SQLite 组装 4 块：老人信息 / 家属列表（remark JSON 解析）/ 入住配置（join check_in_config）/ 合同（按 elder_id） |
| 响应式 | 同 §5.2；窄屏单列 |

### 5.4 数据事务（C++ 本地 SQLite 建议）

办理入住一次提交需写 4 张表（建议 SQLite 事务包裹）：
`elder`（新增或更新，置 status=1、bed_id/bed_number）→ `bed`（bed_status=1）→ `contract`（生成合同号 HT+时间戳）→ `check_in`（status=0，remark 存家属 JSON）→ `check_in_config`。退住场景（本期可选）：check_in.status=1、bed.bed_status=0、elder.status=5。

---

## 6. 房型图片接入规格（在住管理-房型设置）

### 6.1 素材现状（2026-11-24）

`C:\Users\ASUS\Desktop\zzyl\web-zzyl-preject\素材\服务项目图片\` 新增 5 张房型图：

| 文件 | 尺寸 | 大小 | 比例 |
|---|---|---|---|
| 单人套房.png | 700×280 | 373,550 B | 2.5:1 |
| 双人套房.png | 700×280 | 343,389 B | 2.5:1 |
| 豪华单人间.png | 700×280 | 370,799 B | 2.5:1 |
| 豪华双人间.png | 700×280 | 349,065 B | 2.5:1 |
| 普通单人间.png | 700×280 | 341,035 B | 2.5:1 |

> 与原 12 张服务项目图标（340×300 @2x）不同：房型图为 700×280 横图，**不要混用**。实测这 5 张与 DB `room_type.photo` 指向的远端 OSS 图为同一内容（Web 列表已能正常显示）。

### 6.2 房型种子 ↔ 图片映射（6 条房型）

| room_type.id | 房型名称 | 床位费 | 本地图片 | 映射结论 |
|---|---|---|---|---|
| 1 | 单人套房 | 4000 | `单人套房.png` | ✅ 有图 |
| 2 | 双人套房 | 6000 | `双人套房.png` | ✅ 有图 |
| 3 | 豪华单人间 | 3000 | `豪华单人间.png` | ✅ 有图 |
| 4 | 豪华双人间 | 4500 | `豪华双人间.png` | ✅ 有图 |
| 5 | 普通单人间 | 2000 | `普通单人间.png` | ✅ 有图 |
| 116 | 标准双人间 | 1500 | **无本地图**；其 OSS 远程图实测加载失败（列表显示占位图标） | ⚠️ **需占位图**（灰底 #F5F7FA + 山形图片图标 + “暂无图片”），待老师补图 |

### 6.3 C++ 端加载方式（WIC）

- 素材建议拷入工程：`assets/roomtypes/单人套房.png`、`assets/roomtypes/双人套房.png`、`assets/roomtypes/豪华单人间.png`、`assets/roomtypes/豪华双人间.png`、`assets/roomtypes/普通单人间.png`（文件名与房型名一致，便于按名称查找；也可用 id 命名并维护映射表）。
- 用 **WIC**（`IWICImagingFactory::CreateDecoderFromFilename`）解码 PNG → `IWICFormatConverter` 转 `GUID_WICPixelFormat32bppPBGRA` → 生成 D3D11/OpenGL 纹理；700×280 图片建议生成 mipmap（列表缩略显示清晰）。
- 内存建议：5 张原图解码后约 5×700×280×4 ≈ 3.9MB，可常驻纹理缓存；首帧异步加载避免卡顿。
- 缺图占位：加载失败/文件不存在 → 绘制 148×148 或 50×50 灰底面板 + 图片图标 + 文字“暂无图片”。

### 6.4 显示规格

| 使用位置 | Web 实测 | C++ 规格 |
|---|---|---|
| 房型列表「房间图片」列 | 列宽 100；图片 **50×50**、圆角 0、居中；点击可预览（Web 用 `image-preview` 组件） | 缩略图 50×50（按 cover 居中裁切，2.5:1 原图会裁掉左右；或 50×20 contain + 上下留白）；鼠标悬浮显示 700×280 大图/放大镜预览 |
| 房型新增/编辑弹窗「照片」 | 上传框 **148×148**；边框 1px dashed #CDD0D6；底色 #FAFAFA；内含 + 与“点击上传” | 148×148 方框；有图时按 contain 显示（留白 #FAFAFA），另加“更换/删除”小按钮；无图显示 + 号 |
| 详情/大图预览 | 700×280 原图 | 700×280 或按窗口等比缩放（最大 700 宽），背景遮罩，ESC 关闭 |
| 上传大小限制 | ≤2MB（前端 beforeUpload） | 本地选图时校验 ≤2MB 与 png/jpg 格式 |

### 6.5 房型列表/弹窗规格（与 v0.2 一致，补充）

- 列表：无搜索栏；工具栏“新增”（80×32 plain primary）；列：序号 50 / 房间图片 100 / 房型类型 118 / 床位费用 116 / 房型介绍 180 / 创建时间 160 / 状态 116 / 操作 200；行高 49；状态标签“启用”绿、“禁用”红；操作“修改/删除/禁用”（主色文字按钮）。
- 弹窗：500×667，标题“添加房型/修改房型”，单列 label 宽 80，字段：房型名称*/床位数量*/床位费用*/介绍(textarea)/照片(148 上传)/备注；底部确定/取消。
- 数据：`GET /elder/roomType/list`（6 条，✅ 实测）；新增/编辑 `POST/PUT /elder/roomType`；图片字段 `photo`（上传后存 URL/本地路径）。

---

## 7. 本期 Web 端新增/变更（代码核对）

- **git**：仓库当前 3 个提交（`183dd4b` 初始、`282d822` 初始工程+架构文档、`2e390ae` Sprint2 文档）；工作区干净，仅 5 张新增房型图为 **未跟踪文件**（`?? 素材/服务项目图片/*.png`）。
- **功能变更**：对比 Sprint2，**前端业务代码无新增/变更**（`checkIn`、`roomType`、路由、菜单与 Sprint2 一致）；后端 `CheckInController` 等亦未新增 detail 接口。
- **配置变更**：`application.yml` 中 AI 相关 key 已改为环境变量注入（`${OPENAI_API_KEY:sk-local-placeholder}`、`${DEEPSEEK_API_KEY:...}`）；但**数据库口令、Druid 口令、微信 appSecret、华为云 AK/SK 仍为明文硬编码**（安全风险，建议 Sprint 3 内整改）。
- **素材变更**：老师新增 5 张房型图；素材目录已迁移至仓库根 `素材/服务项目图片/`。

---

## 8. 风险与待确认

| # | 风险/问题 | 影响 | 建议 |
|---|---|---|---|
| R1 | 详情接口 `GET /nursing/checkIn/detail/{id}` 后端不存在（实测 500） | Web 详情页不可用；C++ 若照抄会缺数据 | C++ 用本地 SQLite 组装详情；或请后端补该聚合接口 |
| R2 | 护理等级接口 `GET /nursing/level/all` 缺失 | Web 办理表单无法选等级→无法提交；C++ 表单同样缺下拉数据 | C++ 内置 3 条等级字典（76/77/78：1500/2000/3000） |
| R3 | 数据不完整/脏数据：`check_in_config` 仅 1/5、`contract` 仅 1/5；`elder` id=325 姓名存成电话；`check_in.nursing_level_name` 与等级表不一致 | 5 条列表数据可展示，但详情配置多处缺失 | C++ 种子数据可补全/清洗；文档标注示例值以 DB 为准 |
| R4 | 家属信息无独立表，存 `check_in.remark` JSON（varchar 255） | 家属多行易超长；无法按家属查询 | C++ SQLite 可沿用 JSON 字段或建 `elder_family` 表（需 lead 确认边界） |
| R5 | 房型“标准双人间”无本地图且远端 OSS 图失效 | 列表第 6 行显示破图 | 用占位图；请老师补 `标准双人间.png` |
| R6 | 房型图 700×280（2.5:1）与 50×50 缩略/148×148 方框比例不匹配 | 直接拉伸会变形 | 缩略图 cover 裁切、上传框 contain 留白（§6.4） |
| R7 | 办理入住一次写 5 张表（elder/bed/contract/check_in/check_in_config） | C++ 本地事务一致性要求高 | SQLite 事务包裹；失败回滚；建议加“该老人已入住”前置校验（同后端逻辑） |
| R8 | 安全：DB/Druid/微信/华为云密钥明文 | 演示/提交风险 | 迁移环境变量（README 已部分整改 AI key） |
| R9 | Web 表单两列无响应式、详情只读仍显示提交按钮 | C++ 复刻会继承缺陷 | 按差异 D-30~D-32 修正 |

**待 lead 确认**：①C++ 端 M4 数据是继续调 Web 接口还是 SQLite 本地化（本文按本地化给规格）；②详情页是否本期完整做（需本地组装数据）；③是否允许补“标准双人间”占位图/扩充演示数据。

---

## 9. 截图索引（`docs/architecture/assets_s3/`）

| 文件 | 说明 |
|---|---|
| `01_入住办理列表_1280x720.png` | 列表页（搜索/工具栏/表格/状态标签/分页） |
| `02_办理入住表单_首屏_1280x720.png` | 表单首屏（基本信息 + 家属信息） |
| `03_办理入住表单_全页.png` | 表单全页（1280×1352，含入住配置/签约办理/提交） |
| `04_入住详情_read模式_1280x720.png` | 详情 read 模式（实测缺接口，顶部错误提示） |
| `05_房型设置列表_1280x720.png` | 房型列表（前 5 条真实图 + 第 6 条占位） |
| `06_房型设置新增弹窗_1280x720.png` | 房型新增弹窗（148×148 上传框） |

---

## 10. 附录：C++ 端建议种子数据

- `room_type`：6 条（见 §6.2，photo 用 `assets/roomtypes/<名称>.png`，标准双人间用占位）
- `nursing_level`：3 条（1号/2号/3号护理等级，1500/2000/3000 元/月）
- `check_in`：可直接用 §1.7 的 5 条（status=0 已入住）
- `elder` / `bed` / `room` / `floor`：按 §4.3 关联表准备 5 条最小数据
- `contract`：1 条（魏忠语，HT202603052049060001）或按 5 条补齐
- `check_in_config`：补 5 条（当前 DB 只有 1 条；缺的可按护理费/床位费规则补）
