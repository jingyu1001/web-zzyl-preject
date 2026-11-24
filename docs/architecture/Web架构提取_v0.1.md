# 中州养老 Web 综合端 架构提取 v0.1

> 项目：中州养老（Web 综合端，后续将逐阶段转译为 C++ 桌面端）
> 提取对象：`web-zzyl-preject/code/zzyl/zznursing-vue3-master`（前端）、`web-zzyl-preject/code/zzyl/zznursing-master`（后端）、本机 MySQL `zzyl` 库
> 分析方式：**只读**源码 + MySQL 只读查询 + 本地临时启动前后端做运行态测量（未修改任何源码/配置/数据库）
> 分析时间：2026-11-23
> 本迭代范围：①应用整体框架 ②登录页 ③首页看板（其他业务模块仅在“工程结构/菜单”层面登记，不做细节展开）

---

## 0. 结论速览（给非技术读者的 6 句话）

1. 前端是 **RuoYi-Vue3**（Vue3 + Element Plus + Pinia + Vite），后端是 **RuoYi 3.8.9（Spring Boot 3.5.3 + Java 21）多模块单体**，数据库 MySQL 8，缓存/验证码/会话依赖 Redis。
2. 顶部菜单、按钮权限、路由**全部来自数据库表 `sys_menu`**，登录后由后端 `GET /getRouters` 下发，前端动态 `addRoute` 注册，不是写死在前端路由表里的。
3. 登录流程 = 图形验证码（后端 kaptcha 算术题，答案存 Redis）→ `POST /login` 拿 JWT → Cookie 存 token → `GET /getInfo` 拿用户/角色/权限 → `GET /getRouters` 拿菜单 → 进首页。
4. **首页看板没有后端数据接口**：除“个人卡片”调用 `GET /system/user/profile` 外，其余 5 个统计环形图、3 类折线/柱状图、老人等级/年龄分布、预约总览全部是前端 `views/dashboard/constants.js` 里的 **静态假数据**，按“日期”在 A/B/C 三套里轮换。
5. 视觉基调：主色 **#00B8A0（青绿）**、卡片白底无边框/4px 圆角/极浅阴影、侧边栏 #304156 深蓝灰、正文 14px、标题 16px 加粗。
6. 当前 `zzyl` 库只导入了 **35 张表**，而代码里至少还有 `device / device_data / alert_rule / alert_data / reservation / family_member` 等表缺失，相关页面点了会报错；本迭代范围（框架/登录/看板）**不依赖**这些缺失表。

---

## 1. 技术栈与运行环境

### 1.1 前端

| 项 | 取值 | 说明 |
|---|---|---|
| 框架 | Vue **3.4.31** | Composition API + `<script setup>` |
| 路由 | vue-router **4.4.0** | `createWebHistory`，history 模式 |
| 状态 | Pinia **2.1.7** | modules：app / settings / user / permission / tagsView / dict / setColor |
| UI 库 | Element Plus **2.7.6** + @element-plus/icons-vue 2.3.1 | 中文包 zh-cn，默认尺寸 default |
| 图表 | ECharts **5.5.1** | 按需引入 core/components/charts |
| 构建 | Vite **5.3.2** + sass 1.77.5 | 开发端口 **88**，`host: true` |
| 请求 | axios **0.28.1** | 统一封装 `src/utils/request.js` |
| 其它 | js-cookie 3.0.5、jsencrypt 3.3.2、nprogress、@vueuse/core 10.11.0、dayjs（经 `utils/date.js`）、splitpanes、vue-quill | — |
| 页面标题 | `VITE_APP_TITLE = 中州养老` | 见 `.env.development` |
| 接口前缀 | 开发 `VITE_APP_BASE_API = /dev-api`，Vite 代理到 `http://localhost:8080`；生产 `/prod-api` | `vite.config.js` |
| 扫码地址 | `VITE_APP_SOCKET_URL = ws://localhost:8080` | 只写在 `.env` 与 `.env.test`，`.env.development` 里没有（靠基础 `.env` 兜底） |

工程根目录：`zzyl / bin / html / public / src / vite/`；`package.json` 中 `name` 仍是 `ruoyi`（未改项目名）。

### 1.2 后端

| 项 | 取值 |
|---|---|
| 语言/运行时 | **Java 21**（`maven.compiler.source/target=21`） |
| 框架 | **Spring Boot 3.5.3**（RuoYi 3.8.9 定制，`groupId=com.zzyl`） |
| 安全 | Spring Security + JWT（jjwt 0.9.1，HS512）+ BCrypt |
| 持久层 | MyBatis 3.5.19 + MyBatis-Plus 3.5.7（`mybatis-plus-spring-boot3-starter`）+ PageHelper 2.1.0 |
| 连接池 | Druid 1.2.23（控制台 `/druid`，用户名 ruoyi / 口令 123456） |
| 数据库 | MySQL 8.0.33 驱动，库 `zzyl`，账号 root / 口令 123456（明文写在 `application.yml`） |
| 缓存/会话 | Redis `localhost:6379` db0，无密码（Lettuce 连接池） |
| 验证码 | kaptcha 2.3.3，类型 `math`（算术题，答案存 Redis `captcha_codes:{uuid}`，默认 2 分钟） |
| 定时任务 | Quartz（RAMJobStore） |
| 接口文档 | springfox 3.0.0（Swagger），`swagger.pathMapping=/dev-api` |
| 其它 | fastjson2 2.0.53、hutool 5.8.10、POI 4.1.2、oshi 6.8.1、spring-ai 1.0.0、阿里云 OSS SDK 3.17.4、华为云 IoTDA SDK 3.1.70、Kotlin BOM 1.9.23 |
| 端口 | **8080**，context-path `/` |
| Token | 请求头 `Authorization`，前缀 Bearer；JWT 密钥 `abcdefghijklmnopqrstuvwxyz`；有效期 **30 分钟**（Redis 会话滑动续期） |
| 上传目录 | `C:/ruoyi/uploadPath`（Windows 绝对路径） |

### 1.3 本机运行环境探测（2026-11-23）

| 组件 | 状态 | 备注 |
|---|---|---|
| MySQL 8.0.44 @127.0.0.1:3306 | ✅ 运行中 | `zzyl` 库已导入（35 张表，见 §11） |
| Redis 3.2.100 @127.0.0.1:6379 | ✅ 运行中 | 无密码，`PONG` 正常 |
| 后端 8080 | ⏸ 未常驻 | 已验证可启动：`%JAVA_HOME%\bin\java -jar zzyl-admin\target\zzyl-admin.jar`（约 20 s 起） |
| 前端 88 | ⏸ 未常驻 | `npm run dev`（Vite 5）可启动 |
| JDK | ⚠️ 系统 PATH 默认 **1.8**，`JAVA_HOME` 指向 **ms-21.0.9** | 跑后端必须显式用 JDK21，否则启动失败 |
| 浏览器 | Edge 158 / Chrome 可用 | 截图验证用 |

> 本次架构分析曾临时启动前后端（仅运行、未改代码）并在 1280×720/1024×768/1600×900/1920×1080 四个视口做了真实渲染测量，截图见 `docs/architecture/assets/`。分析结束后已可随时停掉，不影响源码。

---

## 2. 后端工程结构（Maven 多模块）

```
zznursing-master
├── pom.xml                     # 父 POM：Java 21 / Spring Boot 3.5.3 / 依赖版本统一管理
├── zzyl-admin                  # 启动模块 + Web 层（24 个类）★ 打包产物 zzyl-admin.jar
│   └── com/zzyl/RuoYiApplication.java
│   └── com/zzyl/web/controller/{system,monitor,common,tool}
│   └── com/zzyl/web/core/config/SwaggerConfig.java
│   └── resources/application.yml（唯一主配置）
├── zzyl-framework              # 安全/框架（50 个类）
│   ├── config/（SecurityConfig、RedisConfig、DruidConfig、MyBatisConfig、MybatisPlusConfig、CaptchaConfig、IotClientConfig…）
│   ├── security/（JwtAuthenticationTokenFilter、AuthenticationEntryPointImpl、LogoutSuccessHandlerImpl）
│   ├── web/service/（TokenService、SysLoginService、SysPermissionService、SysPasswordService、SysRegisterService、PermissionService）
│   ├── aspectj/（DataScopeAspect、LogAspect、RateLimiterAspect、DataSourceAspect）
│   └── interceptor/（RepeatSubmitInterceptor、MemberInterceptor）
├── zzyl-system                 # 系统管理（52 个类）：用户/角色/菜单/部门/岗位/字典/参数/通知/日志
│   └── service/impl/SysMenuServiceImpl（含 buildMenus 路由树装配）
├── zzyl-common                 # 通用（114 个类）：AjaxResult、BaseController、RedisCache、常量、工具、异常、XSS、@RepeatSubmit 等
├── zzyl-quartz                 # 定时任务（18 个类，sys_job / sys_job_log）
├── zzyl-generator              # 代码生成（13 个类，gen_table / gen_table_column）
├── zzyl-nursing-platform       # 养老业务（127 个类）★ 业务主战场
│   ├── controller/（Elder、Bed、Room、RoomType、Floor、CheckIn、Contract、NursingPlan/Project/Level、
│   │                Reservation、Device、DeviceData、AlertRule、AlertData、NursingElder…）
│   ├── controller/member/（小程序端：FamilyMember、MemberReservation、MemberRoomType）
│   ├── domain/ service/ mapper/ dto/ vo/
│   ├── config/WebSocketServer.java（`@ServerEndpoint("/ws/{sid}")` 报警推送）
│   └── task/（护理任务定时生成等）
├── zzyl-oss                    # 阿里云 OSS（1 个类）
└── zzyl-ai-agent               # Spring AI 智能体（1 个类，对应“中州智询/小智”）
```

**模块职能一句话**：`admin` 是入口与 Web 层，`framework` 是安全与横切能力，`system` 是权限与基础数据，`common` 是工具与基类，`nursing-platform` 是养老业务，其余 4 个是可插拔能力（定时、代码生成、OSS、AI）。

### 2.1 关键横切机制

| 机制 | 位置 | 作用 |
|---|---|---|
| 认证过滤器 | `JwtAuthenticationTokenFilter` | 解析 Bearer token → Redis 取 LoginUser → 塞 SecurityContext；token 剩余 <20 min 自动续期 |
| 授权 | `SecurityConfig` + `@PreAuthorize("@ss.hasPermi('xxx')")` | 接口级权限；白名单：`/login /register /captchaImage /member/** /ws/**`、静态资源、`/aiconsult/**`（AI 咨询整段匿名） |
| 权限服务 | `SysPermissionService` | admin 角色直接给 `*:*:*`；其他角色查 `sys_role_menu` |
| 统一返回 | `AjaxResult` | `{code, msg, data?}`，成功 200、失败 500/601、会话失效 401 |
| 统一异常 | `GlobalExceptionHandler` | 业务异常/权限异常/参数校验统一转 JSON |
| 防重复提交 | `@RepeatSubmit` + Redis | 默认 5 秒内同 URL+参数拦截（`@RepeatSubmit.interval` 默认 5000ms；前端另有 1 秒 sessionStorage 级预拦截） |
| 数据权限 | `DataScopeAspect` | 按角色的 `data_scope` 拼 SQL 条件 |
| 多数据源 | `DynamicDataSource` | 主库/从库（从库默认关闭） |
| 日志 | `@Log` + `LogAspect` → `sys_oper_log`；登录日志 → `sys_logininfor` | 异步写入 |

### 2.2 后端 API 覆盖面（摘要，非本次范围）

- 登录/认证：`POST /login`、`GET /getInfo`、`GET /getRouters`、`POST /logout`、`GET /captchaImage`、`/register`（默认关闭）
- 系统管理：`/system/user|role|menu|dept|post|dict|config|notice`、`/system/user/profile`
- 监控：`/monitor/online|job|druid|server|cache`
- 养老业务：`/nursing/*`、`/member/*`（小程序）、`/system/...` 若干
- AI：`/aiconsult/**`（匿名）

接口总量：**40 个 Controller、262 处 `@*Mapping` 注解**；前端 `src/api` 共 **33 个 js 文件**（nursing 13、system 9、monitor 7、tool 1、login+menu 2、aiconsult 1）。

---

## 3. 前端工程结构与分层

```
src/
├── main.js                 # 应用入口：ElementPlus、Pinia、路由、权限、全局组件/方法
├── App.vue                 # <router-view/> + 主题色注入（handleThemeStyle）
├── permission.js           # 全局路由守卫（token / 白名单 / 动态路由）
├── settings.js             # 布局默认配置（sideTheme=theme-dark、tagsView=true、fixedHeader=false、sidebarLogo=true）
├── api/                    # 33 个接口文件（按后端模块分组）
│   ├── login.js            # login / getInfo / logout / getCodeImg / register
│   ├── menu.js             # getRouters
│   ├── system/*.js、monitor/*.js、nursing/*.js、tool/gen.js、aiconsult/knowledge.js
├── assets/
│   ├── images/（登录背景图、logo、常用功能图标、默认头像等）
│   ├── icons/svg/（RuoYi 全套 svg 图标，菜单图标按名字引用）
│   ├── logo/logo.png（64×64）
│   └── styles/（index.scss、variables.module.scss、sidebar.scss、element-ui.scss、ruoyi.scss、btn.scss、mixin.scss、transition.scss）
├── components/             # 通用组件：Breadcrumb、Hamburger、Screenfull、SizeSelect、HeaderSearch、
│                           # TopNav、DictTag、Pagination、RightToolbar、Editor、FileUpload、ImageUpload、
│                           # ImagePreview、SvgIcon、TopNav、warn（报警弹层）、switchBar 等
├── directive/              # v-hasPermi / v-hasRole / v-copyText
├── layout/                 # 应用整体框架（详见 §5）
├── plugins/                # $tab（页签）、$auth（权限判断）、$cache（本地缓存）、$modal、$download
├── router/index.js         # 静态路由 + 动态路由（详见 §4）
├── store/                  # Pinia：app（侧栏/设备/字号）、settings（主题/布局）、user（token/角色/权限）、
│                           # permission（菜单→路由）、tagsView（页签）、dict（字典缓存）、setColor（图表主题）
├── utils/                  # request（axios 封装）、auth（Cookie token）、theme（主色派生）、date（日期工具）、
│                           # jsencrypt（RSA）、validate、ruoyi（通用方法）、color/charts（图表调色）等
└── views/                  # 页面（dashboard、login、nursing/*、system/*、monitor/*、tool/*、aiconsult/*、error/*）
```

**分层约定**：`views` 只做页面编排；`api` 只做接口函数；`store` 管全局状态；`layout` 管壳；`utils` 管横切。组件命名与目录基本沿用 RuoYi 规范（便于后续 C++ 端按“页面—面板—组件”对齐）。

---

## 4. 路由与菜单结构

### 4.1 路由生成机制（重要）

```
constantRoutes（前端静态，src/router/index.js）
        +
dynamicRoutes（前端静态但带 permissions，登录后按权限 addRoute）
        +
GET /getRouters 返回的菜单树（后端 sys_menu 组装，按角色过滤）
        ↓ permission.js 路由守卫
        ↓ filterAsyncRouter（把 component 字符串映射成 @/views 下的 .vue）
        ↓ router.addRoute(...)
```

- 路由守卫（`permission.js`）逻辑：
  1. 有 token 且目标 `/login` → 跳 `/`；
  2. 有 token 且 `userStore.roles` 为空 → 先 `GET /getInfo`，再 `GET /getRouters` 生成路由，最后 `next({...to, replace:true})`；
  3. 无 token 且不在白名单（`/login`、`/register`）→ `next('/login?redirect=' + to.fullPath)`。
- 动态组件映射：`import.meta.glob('./../../views/**/*.vue')`，按后端 `component` 字段（如 `nursing/plan/index`）拼路径；`Layout / ParentView / InnerLink` 特殊处理。
- 菜单组件路径与前端 `src/views` 目录一一对应，**新增页面 = 建 .vue + 在 sys_menu 插一条记录**（或用代码生成器）。
- 页面缓存：`<keep-alive :include="cachedViews">`，按路由 `name` 缓存，`meta.noCache=true` 不缓存。
- 特殊兼容：`permission.js` 中 `mergeStaticChildren` 会把前端 `/aiconsult` 静态路由的“知识库”子项合并到后端“中州智询”菜单下（历史 hack，后续重构要清掉）。

### 4.2 静态路由清单（constantRoutes）

| 路径 | 组件 | 说明 |
|---|---|---|
| `/redirect/:path(.*)` | `views/redirect/index.vue` | 页面刷新中转（hidden） |
| `/login` | `views/login.vue` | 登录页（hidden） |
| `/register` | `views/register.vue` | 注册页（hidden，默认关闭） |
| `/:pathMatch(.*)*` | `views/error/404.vue` | 404 |
| `/401` | `views/error/401.vue` | 无权限 |
| `` → `/index` | `layout` + `views/dashboard/index.vue` | **首页看板**，`meta.title=首页, icon=dashboard, affix=true`（不可关闭标签） |
| `/aiconsult` → `/aiconsult/knowledge` | `layout` + `views/aiconsult/knowledge/index` | 知识库（hidden，由菜单合并展示） |
| `/user/profile` | `layout` + `views/system/user/profile/index` | 个人中心（hidden） |

`dynamicRoutes`（5 条，按 permissions）：`/system/user-auth/role/:userId`、`/system/role-auth/user/:roleId`、`/system/dict-data/index/:dictId`、`/monitor/job-log/index/:jobId`、`/tool/gen-edit/index/:tableId`。

### 4.3 菜单来源与渲染规则

- 数据源：`sys_menu`（128 行，含按钮权限 F 型、目录 M 型、菜单 C 型）。
- 登录用户 → `GET /getRouters` → `SysMenuServiceImpl.buildMenus()`：`M` 型且有子项 → 目录 + `alwaysShow=true` + `redirect=noRedirect`；`C` 型 → Layout 下单页（菜单外框包装）；按钮不返回。
- `RouterVo` 字段：`name / path / hidden / redirect / component / query / alwaysShow / meta{title,icon,noCache,link} / children`。
- 侧边栏渲染（`SidebarItem.vue`）：
  - 父级只有 1 个可见子项且未设 `alwaysShow` → **只显示子项**（一级菜单扁平化）；
  - `hidden=true` 的路由不出现在侧栏；
  - `el-menu unique-opened` → 同级只展开一个子菜单；
  - 选中项：文字 + 图标变为 **#00B8A0**，背景不改变（深色侧栏上不加色块）；
  - 折叠态（54px）：只显图标，悬浮出弹层。

### 4.4 当前一级菜单清单（admin 账号，按 order_num）

| 序 | 菜单 | 路由 | 图标字段（src/assets/icons/svg 名称） | 可见子项 |
|---|---|---|---|---|
| 0 | 首页（静态路由） | `/index` | dashboard | — |
| 1 | 中州智询 | `/aiconsult` | component | 小智（`/aiconsult/aiconsult`→`aiconsult/virtualagent`）+ 合并的“知识库” |
| 2 | 来访管理 | `/appointment` | button | 预约信息 |
| 3 | 入退管理 | `/enterQuit` | component | 入住办理（入住详情/评估详情隐藏） |
| 4 | 在住管理 | `/liveIn` | date-range | 房型设置、床位预览 |
| 5 | 服务管理 | `/nursing` | example | 护理项目、护理计划、护理等级、负责老人（任务安排详情隐藏） |
| 6 | 智能监测 | `/intelligence` | exit-fullscreen | 设备管理、报警规则、报警数据（设备详情/新增报警规则隐藏） |
| 7 | 系统管理 | `/system` | system | 用户/角色/菜单/部门/岗位/字典/参数/通知/日志管理（操作日志、登录日志） |
| 8 | 系统监控 | `/monitor` | monitor | 在线用户、定时任务、数据监控、服务监控、缓存监控、缓存列表 |
| 9 | 系统工具 | `/tool` | tool | 表单构建、代码生成、系统接口 |
| — | 若依官网（hidden，外链） | `http://ruoyi.vip` | guide | — |

> ⚠️ 菜单 `icon` 字段明显被改错过（出现 `component`、`button`、`date-range`、`exit-fullscreen` 等“表单控件/功能图标”名，不是目录语义图标），导致侧栏图标与菜单含义不匹配。前端对找不到的 svg 名不报错，只是图标错/缺。C++ 复刻时建议按语义自行映射图标，不要照抄字段值。

### 4.5 权限模型

- 角色 6 个：超级管理员（admin）、普通角色（common）、护理员（nursingUser）、维修工（maintainer）、客服专员（customerService）、院长（SuperAdministrator）。
- 菜单权限：`sys_role_menu` 共 630 条；admin 走代码特判（全量 `*:*:*`）。
- 按钮权限：`sys_menu.menu_type='F'` + `perms`（如 `system:user:add`），前端 `v-hasPermi`、后端 `@PreAuthorize`。
- 数据权限：角色 `data_scope` + `sys_role_dept`（后端 SQL 拼接）。
- 部门树：`智慧养老院(100)` → 高层办公室/财务部/行政部/护理部/后勤部/销售部。

---

## 5. 布局组件树（应用整体框架）

```
layout/index.vue（.app-wrapper，:style="--current-color: theme"）
├── Sidebar/（.sidebar-container，fixed，宽 200px；折叠 54px；暗色 #304156）
│   ├── Logo.vue            （高 50px，logo 32×32 + 标题“中州养老”14px/白色/600）
│   └── el-scrollbar
│       └── el-menu（theme-dark，unique-opened，collapse，active-text-color=#00B8A0）
│           └── SidebarItem.vue（递归）→ AppLink + el-menu-item / el-sub-menu
├── .main-container（margin-left: 200px，高度自适应）
│   ├── div（固定头容器；settings.fixedHeader=false → 当前**不固定**）
│   │   ├── Navbar.vue（高 50px，白底，阴影 0 1px 4px rgba(0,21,41,.08)）
│   │   │   ├── Hamburger（50×50 点击区，图标 20×20，展开时旋转 180°）
│   │   │   ├── Breadcrumb（默认；`/首页` 起头）/ TopNav（settings.topNav=true 时启用的顶部菜单）
│   │   │   └── .right-menu（右对齐；8 个元素）：
│   │   │       搜索、源码、文档、全屏、主题（明/暗）、字号选择、头像下拉（个人中心/退出登录）、布局设置
│   │   └── TagsView（高 34px，白底，下边框 #D8DCE5，选中标签 #00B8A0 白字 + 8px 白点）
│   │        └── ScrollPane（横向滚动）+ 右键菜单（刷新/关闭当前/关闭其他/关闭左侧/关闭右侧/全部关闭）
│   ├── AppMain.vue（.app-main，min-height: calc(100vh - 84px)）
│   │   └── <router-view> + <keep-alive> + transition + iframe-toggle
│   └── Settings/index.vue（右滑抽屉 300px）：主题风格（深/浅）、主色选择、顶部菜单开关、
│       标签栏开关、固定头部开关、Logo 开关、动态标题开关（写入 localStorage `layout-setting`）
└── Warn 组件（`components/warn/index.vue`）：WebSocket 报警弹层
```

**布局行为要点**

| 行为 | 规格 |
|---|---|
| 侧栏宽度 | 展开 200px，折叠 54px（cookie `sidebarStatus` 记忆），<992px 变移动端抽屉（遮罩 + 关闭） |
| 顶栏 | 高 50px；菜单折叠按钮在最左；面包屑从 `/首页` 起拼；右功能区图标 18×18、每项 padding 0 8px（宽约 34px） |
| 标签页 | 高 34px；标签 26px 高、12px 字；首个/末个外边距 15px，标签间距 5px；首页为 affix 不可关闭 |
| 内容区 | 页面整体纵向滚动；**顶栏与标签栏随页面一起滚出**（`fixedHeader=false`）；侧栏固定不动 |
| 滚动条 | 全局 6px，轨道 #F1F1F1，滑块 #C0C0C0 圆角 3px |
| 报警推送 | 登录后建立 `ws://localhost:8080/ws/{userId}`；`notifyType=1` 弹层 + 楼层/床位高亮，否则解除 |
| 移动端 | `<992px`：`device=mobile`，侧栏变 drawer（fixed 200px + 0.3s 位移），顶栏右上只留头像 |

---

## 6. 登录流程（验证码 / token / 跳转）

### 6.1 时序

```
浏览器打开 /login（白名单）
  │
  ├─① GET /captchaImage（isToken:false，超时 20s）
  │     ← {code:200, captchaEnabled:true, uuid, img:"<base64 jpg>"}
  │     前端：img 拼成 data:image/gif;base64,xxx；uuid 暂存
  │
  ├─② 填写 admin/admin123/验证码（可勾选“记住密码”→ Cookies：username、RSA 加密后的 password、rememberMe，30 天）
  │
  ├─③ POST /login {username, password, code, uuid}
  │     后端：Redis 取 captcha_codes:{uuid} → 忽略大小写比对 → 删除 key
  │           → 用户名校验/密码长度/黑名单 IP → Spring Security BCrypt 认证
  │           → 生成 LoginUser → tokenService.createToken()
  │           → JWT(HS512){sub=username, login_user_key=<uuid>}；Redis login_tokens:<uuid> 存 LoginUser（30 min，滑动续期）
  │     ← {code:200, msg:"操作成功", token:"eyJhbGciOiJIUzUxMiJ9...."}
  │     前端：Cookie 写入 Admin-Token；Pinia user.token 赋值
  │
  ├─④ router.push(redirect || '/')
  │     路由守卫：roles 为空 → GET /getInfo（带 Bearer）
  │       ← {code, user{...}, roles:["admin"], permissions:["*:*:*"]}
  │     → GET /getRouters
  │       ← {code, data:[RouterVo...]}
  │     → filterAsyncRouter + addRoute → next({...to, replace:true})
  │
  └─⑤ 进入首页 /index（dashboard）
```

### 6.2 接口契约（前后端字段级）

| 接口 | 方法/路径 | 请求 | 响应（关键字段） |
|---|---|---|---|
| 验证码 | `GET /captchaImage` | header `isToken:false` | `{code, msg, captchaEnabled, uuid, img(base64)}`；关闭验证码时只有 `captchaEnabled:false` |
| 登录 | `POST /login` | `{username, password, code, uuid}` | `{code, msg, token}`（**token 在顶层**，不是 `data.token`） |
| 用户信息 | `GET /getInfo` | Bearer | `{code, msg, user:{userId,deptId,userName,nickName,email,phonenumber,sex,avatar,status,loginIp,loginDate,dept:{deptName}}}, roles:[], permissions:[]}` |
| 路由 | `GET /getRouters` | Bearer | `{code, msg, data:[{name,path,hidden,redirect,component,alwaysShow,query,meta:{title,icon,noCache,link},children}]}` |
| 退出 | `POST /logout` | Bearer | `{code,msg}`，成功后 Redis 会话删除、前端清 cookie |
| 个人信息（看板用） | `GET /system/user/profile` | Bearer | `{code, msg, data:<SysUser>, roleGroup:"超级管理员", postGroup:"董事长"}` |

Cookie：`Admin-Token`（无过期时间 = 会话级）。请求拦截器自动加 `Authorization: Bearer <token>`。
响应拦截：`code=401` 弹“登录状态已过期 → 重新登录”；`500` 红色提示；`601` 警告；`200` 正常。

### 6.3 登录相关实现细节

- 验证码开关：`sys_config['sys.account.captchaEnabled']='true'`；类型 math（如“8-1=?”），图片 jpg base64；点击图片刷新。
- 验证码错误/密码错误后自动重新拉验证码；按钮 loading 文案“登 录 中...”。
- 默认账号：`admin / admin123`（页面 `loginForm` 也预填了这两个值，仅演示环境）。
- 记住密码：密码用 `jsencrypt` 公钥加密后存 Cookie（非明文）。
- 登录页无注册入口（`sys.account.registerUser=false`）。
- 登录成功日志异步写 `sys_logininfor`。

---

## 7. 首页看板数据流（重点）

### 7.1 组件树

```
views/dashboard/index.vue（.home-wrapper，页面 padding 24px 15px 52px 24px）
├── TopPanel.vue
│   ├── 左：数据概览卡（5 个环形图：老人数量 / 床位数量 / 服务单数量 / 员工数量 / 收入金额）
│   └── 右：个人卡（头像、Hello、邮箱/账号/部门·岗位/角色、下属员工头像组）
├── MiddleChart.vue
│   ├── 左：图表卡（tabs：收益情况 / 入退情况 / 服务情况；时间：今日 / 本周 / 本月；日期区间框；图表本体）
│   └── 右：常用功能卡（8 宫格：来访管理、入住申请、退住申请、床位预览、用户管理、订单管理、退款管理、报警数据）
└── RankList.vue
    ├── 老人等级分布（环形图）
    ├── 老人年龄分布（堆叠柱状图）
    └── 预约总览（周选择 + 当日预约列表）
```

### 7.2 数据来源结论（与实际代码核对）

| 面板 | 数据来源 | 是否真实接口 |
|---|---|---|
| 数据概览 5 个统计（数值 + 2 个分段） | `dashboard/constants.js` 的 `OLDMAN_NUM_* / BED_NUM_* / SERVE_NUM_* / STAFF_NUM_* / MONEY_NUM_*` | ❌ 静态 |
| 个人卡 | `GET /system/user/profile`（邮箱、账号、部门/岗位、角色，头像为静态 `grzxhz.jpg`） | ✅ 真实 |
| 收益/入退/服务图表 | `constants.js` 的 `TODAY_* / WEEK_* / MONTH_*` | ❌ 静态 |
| 老人等级分布 | `ELDER_RANK_DATA_*` | ❌ 静态 |
| 老人年龄分布 | `ELDER_AGE_DATA_*` | ❌ 静态 |
| 预约总览 | `SUBSCRIBE_DATA`（20 组 × 3 条，按“星期几”取索引） | ❌ 静态 |
| 常用功能 | 路由跳转配置 | — |
| 图表 option | `dashboard/index1.js`（5 个饼图 + 等级 + 年龄）；`EarningsEchart/EnterEchart/ServeEchart` 内各自硬编码 | — |

> 全看板 **只有 1 个真实 API 调用**：`views/dashboard/index.vue` 的 `getUserProfile()`（`/system/user/profile`）。
> 也就是说：**C++ 转译端第一阶段如果只复刻框架/登录/看板，可以完全不依赖养老业务接口，只依赖登录与 profile 两个接口（甚至 profile 也可用假数据）。**

### 7.3 假数据轮换规则（已在运行态验证）

- `TopPanel` / `MiddleChart` / `RankList` 的 `onMounted` 都执行同一段逻辑：
  ```js
  const num = (getMonthInfo(new Date()).surplusDay + 1) % 3;
  // surplusDay = 当月天数 - 今日日
  // num=1 → A 套；num=2 → B 套；num=0 → C 套
  ```
- 实测 2026-11-23（11 月 30 天）：`surplusDay=7` → `num=2` → **B 套**；页面显示 `655 人 / 812 个 / 100.35w 笔 / 204 人 / 410.81w 元`，与 `*_B` 常量完全一致。
- 代码注释写“3 套数据 3 天出现一次”，但按公式实际是**每天变换**（surplusDay 每天 -1，mod 3 循环）。注释与实现不一致，C++ 端复刻时采用公式行为即可。

### 7.4 个人卡字段映射（唯一真实接口）

| 卡片位置 | 代码 | 后端字段 | 实测值（admin） |
|---|---|---|---|
| 邮箱行 | `baseData.user.email` | `SysUser.email` | `ry@163.com` |
| 账号行 | `baseData.user.userName` | `SysUser.userName` | `admin` |
| 部门/岗位行 | `baseData.user.dept.deptName` + `postGroup` | `SysUser.dept.deptName` + `selectUserPostGroup()` | `院长办公室 / 董事长` |
| 角色行 | `baseData.roleGroup` | `selectUserRoleGroup()` | `超级管理员` |
| 问候语 | `baseData.nickName` | ——（**取错层级，永远 undefined**） | 显示为 `Hello！`（无名字）⚠️ |
| 头像 | 静态 `assets/images/grzxhz.jpg` | ——（原动态绑定被注释） | 猫头像 |
| 下属员工 | 6 张静态 `img1~img6.png` + `+3` | —— | 静态 |

### 7.5 看板已发现的缺陷（实测确认，C++ 端不要照抄）

| 编号 | 缺陷 | 表现/原因 | 建议 |
|---|---|---|---|
| BUG-01 | 个人卡问候昵称为空 | 模板用 `baseData.nickName`，但 `baseData = {user, roleGroup, postGroup}` → undefined | 改为 `baseData.user.nickName`，或接口补 `nickName` |
| BUG-02 | 时间筛选“本周”但区间显示今天 | `active` 默认 `ref(1)`（本周），但初始化只调 `getDate()`（今日→今日），未调 `getDataInfo()` | 初始化按 active 计算区间 |
| BUG-03 | 中间图表被卡片裁切 | 卡片固定高 278px，内容高约 325px；图表 canvas 234px，底部约 27px 被 `overflow:hidden` 裁掉（x 轴刻度看不到） | 卡片高改 auto 或图表高按 200px 内收 |
| BUG-04 | 五个环形图在 1280 宽下被压扁 | 容器 CSS 固定 158×158，但 flex 收缩成 138×158（非正方形，环形变扁的风险） | 用 `flex:1 1 0; aspect-ratio:1/1; max-width:158px` |
| BUG-05 | 图表不随窗口变化重排 | 只有 RankList 监听 `window.resize` 且只 resize 了最后一个 `myChart`；其他图表 canvas 尺寸固定 | 统一用 ResizeObserver 或封装 chart 组件 |
| BUG-06 | 图表 DOM id 冲突 | `EarningsEchart.vue` 和 `ServeEchart.vue` 都写 `id="main"`（`v-if` 互斥才没炸） | 用 ref 容器，不用全局 id |
| BUG-07 | 前端 lodash 未声明依赖 | `store/modules/setColor.js` `import keys from 'lodash/keys'`，`package.json` 无 lodash（靠 Element Plus 传递依赖，构建不保证） | 显式加依赖或去掉 |
| BUG-08 | 报警 WebSocket 地址缺失 | `layout/index.vue` 用 `import.meta.env.VITE_APP_SOCKET_URL`，但 `.env.development` 未定义（只在 `.env`/`.env.test`），若基础 env 被覆盖会连 `undefined/ws/...` | 统一在各环境显式声明 |
| BUG-09 | 菜单图标字段错乱 | `sys_menu.icon` 填了 `component/button/date-range` 等非图标名 | 修正 `sys_menu` 或用语义映射表 |

---

## 8. 主题样式要点（C++ 复刻依据）

### 8.1 颜色 Token

| 用途 | 色值 | 出处 |
|---|---|---|
| **品牌主色** | **#00B8A0** | `settings.theme` 默认值；`handleThemeStyle()` 运行时写入 `--el-color-primary` 及 1~9 级派生色 |
| 链接/次要品牌 | #008D71 | `$--color-link` |
| 图表蓝（收入/入住） | #4D8DFF | 折线/柱状 series color |
| 图表主青（退款/退住/服务） | #00B8A0 | 同上 |
| 环形浅色轨道 | #D7F1ED | 各饼图首色 |
| 文字-主 | #303133 | Element `--el-text-color-primary` |
| 文字-标题深 | #262626 | `$base--color-bk2` |
| 文字-正文 | #595959 | `$--color-bk3`（图表 axis 文字） |
| 文字-次要 | #999999 | `$base-color-bk4`（数据更新时间/个人卡信息/预约行） |
| 文字-图表副标题 | #909399 | 环形图中心 name |
| 文字-占位 | #BFBFBF | `$--color-bk6` |
| 分隔线/坐标轴 | #DCDCDC、#E8E8E8 | 图表线 / 普通边框 |
| 浅灰填充块 | #F3F8F9 | 日期区间框、预约行背景 |
| 页面/卡片底 | #FFFFFF | body 未设背景（白）；卡片白 |
| 侧栏底 | #304156 | `$menuBg` |
| 二级菜单底 | #1F2D3D | `$base-sub-menu-background` |
| 菜单 hover | #263445 / rgba(0,0,0,.06) | — |
| 菜单文字 | #BFCBD9 | `$menuText` |
| 菜单选中 | #00B8A0（文字+图标，无底色块） | `el-menu active-text-color` |
| 成功/警告/危险/信息 | #67C23A / #E6A23C / #F56C6C / #909399 | Element 语义色 |
| 参观标签 | #27BA9B | `.icon3` |
| 预约标签 | #FE6E6E | `.icon1` |
| 老人等级 4 色 | #47CBBA / #9891DF / #8EBEF3 / #FFB057 | `getElderRankDistribution` |
| 老人年龄男/女 | #27C2AE / #FE6E6E | `getElderAgeDistribution` |
| 图表通用色板 | #D7F1ED, #46CDBC, #00A870, #EBB105, #ED7B2F, #E34D59, #ED49B4, #834EC2 | `getChartListColor()` |

### 8.2 字号 / 行高 / 圆角 / 阴影 / 间距

| 项 | 规格 |
|---|---|
| 基础字号 | 14px（body）；中文字体栈 `Helvetica Neue, Helvetica, PingFang SC, Hiragino Sans GB, Microsoft YaHei, Arial` |
| 字号梯度 | 12 / 14 / 16 / 18（环形图中心数字 18）/ 16.38（登录页标题，UA h3 1.17em） |
| 卡片标题 | 16px / 700 |
| 行高 | 常规 24、个人信息 26、导航 50、图表头 41 |
| 卡片圆角 | 4px（Element `--el-card-border-radius`） |
| 登录卡圆角 | 6px；弹窗 8px；日期框/预约行 2px；图标底 4px；标签圆角 0（无圆角）；头像 50% |
| 卡片阴影 | `rgba(0,0,0,0.03) 0 2px 4px 0`（即 #00000008）；**无边框** |
| 导航阴影 | `rgba(0,21,41,0.08) 0 1px 4px 0` |
| 标签栏阴影 | `rgba(0,0,0,.12) 0 1px 3px 0, rgba(0,0,0,.04) 0 0 3px 0`，下边框 1px #D8DCE5 |
| 首页内容 padding | 上 24 / 右 15 / 下 52 / 左 24 |
| 卡片间距 | 行间 16px；两列 gutter 24px；三列 gutter 12px |
| 卡片头 | padding 20 20 0；min-height 41px；卡体 padding 0 20 20（首页特例，顶部 0） |
| 侧栏 | 200px（折叠 54）；logo 50px；一级项 56px；二级项 50px；图标 14px + 右距 16；左缩进 20/40/60 |
| 顶栏 | 50px；图标 18px；右侧项 padding 0 8；头像 30px 圆；昵称 14px/700 |
| 标签栏 | 34px；标签高 26 / 12px 字 / padding 0 8；首尾外边距 15；间距 5 |

### 8.3 Element Plus 覆盖规则（复刻时注意）

```scss
.el-card { box-shadow:none; border:0 }                       // 全局去边框/阴影
.home-wrapper .el-card { box-shadow:0 2px 4px 0 #00000008 }  // 首页卡片重新加极浅阴影
.el-card__header { padding:14px 15px 7px !important; min-height:40px }   // 全局
.el-card__body   { padding:15px 20px 20px 20px !important }               // 全局
.home-wrapper .el-card__body { padding-top:0 !important }                 // 首页
.el-form-item.el-form-item--default { margin-bottom:28px }                // 登录表单行距
.el-dialog { border-radius:8px }
```

- 主色注入：`App.vue mounted → handleThemeStyle(theme)` 动态生成 `--el-color-primary` + light-1..9 / dark-1..9；**没有改 SCSS 变量**，所以换主色只影响 Element 组件（按钮、输入 focus、tabs、checkbox 等）与 `var(--el-color-primary)` 引用处；`$--color-main` 仍固定 #00B8A0。
- 暗色模式：`html.dark` 覆盖 `--sidebar-bg/--navbar-bg/--tags-*` 等变量；顶栏月亮图标切换（`useDark`）。
- 布局配置持久化：`localStorage['layout-setting']`（theme、sideTheme、topNav、tagsView、fixedHeader、sidebarLogo、dynamicTitle）。

---

## 9. 数据库现状（zzyl 库）

### 9.1 表清单（共 35 张，括号为行数）

**系统表（17）**：`sys_user(8)`、`sys_role(6)`、`sys_menu(128)`、`sys_dept(14)`、`sys_post(4)`、`sys_config(6)`、`sys_dict_type(14)`、`sys_dict_data(37)`、`sys_notice(2)`、`sys_job(3)`、`sys_job_log`、`sys_logininfor`、`sys_oper_log`、`sys_role_menu(630)`、`sys_role_dept(3)`、`sys_user_role(7)`、`sys_user_post(7)`
**业务表（14）**：`elder(5)`、`bed(89)`、`room(60)`、`room_type(6)`、`floor(11)`、`check_in(5)`、`check_in_config(1)`、`contract(1)`、`nursing_elder(7)`、`nursing_level(3)`、`nursing_plan(3)`、`nursing_project(9)`、`nursing_project_plan(6)`、`nursing_task(0)`
**其它（4）**：`gen_table(4)`、`gen_table_column(72)`、`general_log`、`slow_log`
> 合计 17 + 14 + 4 = 35 张。注意：代码 `zzyl-nursing-platform` 里有 **19 个业务实体类**，但库里只有 14 张业务表，其中 6 个实体没有对应表（见 §9.3），另有 `nursing_task` 表存在但无同名实体（任务逻辑在 arrange/定时任务中处理）。

### 9.2 核心表字段契约（与本次范围相关）

- `sys_user`：`user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex(char 0男1女2未知), avatar, password(BCrypt), status, del_flag, login_ip, login_date, create_by/time, update_by/time, remark`
- `sys_menu`：`menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type(M/C/F), visible, status, perms, icon, ...`
- `sys_role`：`role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, ...`
- `sys_dept`：`dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag`
- `sys_config`：6 条参数（皮肤 skin-blue、初始密码 123456、侧栏主题 theme-dark、验证码开关 true、注册开关 false、登录黑名单空）
- 业务表典型字段：`elder(name,image,id_card_no,sex,status(0禁用/1启用/2请假/3退住中/4入住中/5已退住),phone,birthday,address,bed_number,bed_id)`；`bed(bed_number,bed_status(0未入住/1已入住),sort,room_id)`；`room(code,sort,type_name,floor_id)`；`room_type(name,bed_count,price,introduction,photo,status)`；`check_in(elder_name,elder_id,id_card_no,start_date,end_date,nursing_level_name,bed_number,status(0入住/1退住))`；`nursing_task(nursing_id,project_id,elder_id,estimated_server_time,real_server_time,status(1待执行/2已执行/3已关闭))`。
- 统一字段习惯：`sort_order / create_time / update_time / create_by / update_by / remark`；逻辑删除见 `room.is_deleted`。

### 9.3 数据库与代码不一致（重要风险）

代码 `zzyl-nursing-platform` 里有 19 个业务实体，但 `zzyl` 库只有 14 张业务表，以下 6 个实体**没有对应表**（对应页面/接口会 500）：

| 缺失表 | 对应业务/实体 | 影响 |
|---|---|---|
| `device` | Device 设备管理 | 智能监测-设备管理页 |
| `device_data` | DeviceData 设备数据 | 设备详情页 |
| `alert_rule` | AlertRule 报警规则 | 智能监测-报警规则页 |
| `alert_data` | AlertData 报警数据 | 智能监测-报警数据页 |
| `reservation` | Reservation 预约信息 | 来访管理页 |
| `family_member` | FamilyMember 家属成员（小程序） | `/member/**` 接口 |

另有前端页面 `views/nursing/healthAssessment/*`（健康评估）在后端没有对应 Controller，属于“页面有、接口无”的另一类不一致（列表页可能查到空数据/404）。

> 本次三个目标（框架/登录/看板）均不依赖这些表：登录只读 `sys_*`，看板只调 `sys_user/profile`。

---

## 10. 主要风险与待确认（给 lead 决策用）

| # | 风险 | 影响 | 建议 |
|---|---|---|---|
| R1 | 库缺 6+ 张业务表 | 后续业务页面/接口必报错 | 找老师要完整 SQL，或在第一天范围内明确“只做框架+登录+看板” |
| R2 | 看板全是假数据 | 验收若要求“真实统计”会返工 | 向老师确认：看板是否需要接真实统计接口；若需要，需新增统计 API + SQL 聚合 |
| R3 | 后端强依赖 Redis | 换机/断 Redis 登录直接不可用 | 部署清单必须包含 Redis；或后续改用 JWT 无状态会话 |
| R4 | JDK 版本 | `java -version` 默认 1.8，项目需 21 | 启动脚本/IDE 显式指定 JDK21（`JAVA_HOME=...ms-21.0.9`） |
| R5 | 敏感配置硬编码 | `application.yml` 含数据库口令、Druid 口令、华为云 AK/SK、百度千帆 key、微信 appSecret | 提交前改为环境变量；对外演示前轮换密钥 |
| R6 | `/aiconsult/**` 匿名开放 | AI 咨询接口无鉴权 | 演示可接受，正式版应收紧 |
| R7 | 前端未声明 lodash、WebSocket env 缺失、图表裁切/不 resize 等 9 个已知缺陷（§7.5） | 不影响“能跑”，影响“好看/稳定” | 架构 v0.2 统一列成技术债清单，分配修复 |
| R8 | 菜单图标字段错乱 | 侧栏图标与语义不符 | 修正 `sys_menu.icon` 或前端做映射兜底 |
| R9 | 仓库 git 状态 | `web-zzyl-preject` 是 git 仓库但 `main` 无任何提交，源码均为未跟踪文件 | 第一天结束前请 lead 决定是否首次提交（本次分析未动 git） |
| R10 | 项目名/版权 | 前端仍叫 ruoyi、页脚版权 ruoyi.vip | 演示前是否要改“中州养老”品牌化，需老师/lead 确认 |

---

## 11. 下一步（架构侧）

1. **等 lead 确认**：①本迭代验收标准（是否要真实数据）②技术栈是否会补充（前端是否允许换框架/是否加 Redis 之外中间件）③C++ 端第一期复刻边界（只做框架+登录+看板，还是含业务页）。
2. 在此基础上出 **架构 v0.2**：ECS 分层落地（实体=数据表/领域对象、组件=可复用 UI 面板、系统=登录鉴权/菜单路由/看板数据装配）、目录与接口契约冻结、任务单拆分给 web-dev1/2/3。
3. C++ 端配套产出：`UI复刻规格_Cpp_1280x720_v0.1.md`（本期已交付）。
4. 建议第一天三件套优先级：**布局框架（壳）→ 登录闭环 → 首页看板视觉对齐**；业务页面在第二天起按菜单顺序推进。

---

## 附录 A：截图证据（本次运行态生成）

| 文件 | 说明 |
|---|---|
| `assets/01_登录页_1280x720.png` | 登录页真实渲染（1280×720） |
| `assets/02_首页看板_1280x720.png` | 首页看板首屏（1280×720，含图表裁切现象） |
| `assets/03_首页看板_1280全页.png` | 首页看板整页（1280×985，完整三行卡片） |
| `assets/04_首页看板_1920x1080.png` | 1920×1080（xl 断点，底部三列等宽） |
| `assets/05_首页看板_1024x768.png` | 1024×768（单列堆叠） |
| `assets/06_侧边栏展开_1280.png` | 展开“服务管理”子菜单后的侧栏 |
| `assets/07_首页看板_滚动后_1280.png` | 页面滚到底部（顶栏/标签栏已滚出，验证非固定头部） |

## 附录 B：本次未做的事（防止误解）

- 未修改任何源码/配置/数据库；未做 git 操作。
- 未分析除“框架/登录/看板”外的业务页面实现（只登记目录与菜单）。
- 未做性能压测、安全审计（仅标注明显风险）。
- 未验证原生 Windows 手机端/小程序端。
