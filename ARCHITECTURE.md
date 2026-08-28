# Moment Mark 技术架构（v0.2：J 最终设计迁移版）

> 用途：指导以 `PRD.md` 和 `docs/design/proposal_J_final.html` 为目标的 UI 重做与功能扩展。  
> 事实来源：技术栈与现有数据风险来自当前代码；产品行为、页面和视觉以 PRD/J 最终设计稿为准。  
> 迁移立场：旧页面、旧卡片模板、旧首页 Hero、自由拖拽排版和原型日子簿均可废弃或重写；它们只可作为数据迁移或可复用底层代码的参考，绝不是必须保留的交互约束。

## 1. 文档与决策优先级

遇到冲突时，按以下顺序决策：

1. `PRD.md`：产品行为、页面入口、数据归属、P0/P1/P2 和验收标准。
2. `docs/design/proposal_J_final.html`：最终页面编排、布局和交互表达；`proposal_J.html`、`proposal_J_app.html` 仅作补充。
3. `DESIGN_SYSTEM.md`：视觉 token、组件规格与词表。若它仍引用旧方案，必须随本次 J 最终设计迁移同步更新，不能反向覆盖 J 最终设计。
4. 本文：目标数据模型、实施边界、遗留代码的退出路径和技术债。
5. 现有代码：仅是技术栈、可迁移数据和已验证底层能力的参考。与前四项冲突时，应修改或移除旧代码，而不是迁就旧行为。

`CLAUDE.md` 是给 AI 的快速交接入口，不能替代上述文档。

## 2. 当前技术栈与版本

| 层级 | 技术 | 当前版本/约束 | 用途 |
| --- | --- | --- | --- |
| 平台 | Android 原生 | `minSdk 26`、`compileSdk 36`、`targetSdk 36` | 单 Android App。 |
| 语言 | Kotlin | 2.2.20，JVM target 11 | 主语言。 |
| 构建 | Android Gradle Plugin | 8.13.1 | Gradle 构建。 |
| UI | Jetpack Compose + Material 3 | Compose BOM 2025.08.00 | 单 Activity 声明式 UI。 |
| 生命周期 | AndroidX Lifecycle ViewModel | 2.9.0 | 持有应用级 UI 状态和协程。 |
| 关系数据 | Room | 2.7.2，KAPT，schema 导出 | 时刻的可靠本地持久化与迁移。 |
| 轻量配置 | DataStore Preferences | 1.1.7 | 主题、分组、模板收藏、详情附属数据、卡片布局。 |
| 时间 | `java.time` | Android API 26+ | 使用 `LocalDate`、`Instant`、`ZoneId`。 |
| 异步 | Kotlin Coroutines / Flow | Coroutines 1.8.1 | 数据流与 UI 状态。 |
| 测试 | JUnit 4、Compose UI Test、Espresso、Room Testing | Espresso 3.7.0 | JVM、界面及 Room 迁移测试。 |

目标架构仍可保持**单模块 `:app`、单 Activity、Compose、MVVM + Repository、本地优先、无网络**。当前手工 `AppScreen` 路由和 `AppContainer` 可短期沿用；若重做 J 最终设计的页面流时它们妨碍返回栈或参数传递，可局部替换为清晰的导航层。不要为保留旧路由而保留旧页面行为。

构建命令：

```bash
./gradlew testDebugUnitTest assembleDebug lintDebug
./gradlew connectedDebugAndroidTest
```

涉及 Room schema 变更时，第二条必须在已连接设备或模拟器上执行；构建成功不等于日期、手势、真实页面或无障碍流程已验证。

## 3. 系统总览

```text
MainActivity
  └─ MomentMarkApp（Compose 根；只协调 J 最终设计的页面流）
       ├─ AppViewModel / Navigator（页面状态、一次性事件）
       ├─ AppContainer（手工装配即可）
       │    ├─ MomentRepository ─ Room / moments（或迁移期 time_events）
       │    ├─ TaskRepository ─ Room / tasks
       │    ├─ RecycleBinRepository ─ Room 查询两个实体
       │    └─ SettingsStore ─ DataStore（主题等纯偏好）
       ├─ ui/moment/（大事件首页、铭刻时刻表单、统一时刻详情）
       ├─ ui/daybook/ + ui/task/（副本地图、任务表单/详情）
       ├─ ui/recyclebin/ + ui/settings/
       └─ ui/theme + ui/components（J 最终设计的共享 token/组件）

Domain：Moment、Task、HomeCardItem、EventTimeCalculator
```

现有 `HomeScreen`、`EventCreateFeature`、`EventSettingsFeature`、`PrototypeDaybookDataSource`、`CardBoard` 等代码属于迁移来源，不是上述目标树的命名或职责约束。

数据流应始终是：**持久化事实 → Repository/Store → `Flow` → ViewModel UI state → Compose Feature → 共享展示组件**。  
禁止把计算后的“还有 X 天”、日期文案、卡片排序结果写回数据库；这些是随当前时间变化的展示派生值。

## 4. 目录结构与职责

```text
.
├── PRD.md                         产品行为与验收标准
├── ARCHITECTURE.md                本文：架构、数据边界、技术债
├── DESIGN_SYSTEM.md               唯一视觉 token/组件规范
├── CLAUDE.md                      AI 快速交接规则
├── docs/
│   ├── design/                    HTML 设计稿；J_final 是最终页面结构基准
│   ├── card_picture/              卡片参考素材
│   ├── page_picture/              页面参考素材
│   └── background_picture/        文档参考背景素材
├── background_picture/            运行时详情背景资源；Gradle 作为 assets 打包
├── gradle/libs.versions.toml      版本目录；新增依赖统一在此声明
└── app/
    ├── build.gradle.kts           Android 配置、依赖、Room schema/asset source set
    ├── schemas/                   已导出的 Room schema；不得手工伪造
    └── src/
        ├── main/java/com/cch/momentmark/
        │   ├── MainActivity.kt    Activity 入口，负责 edge-to-edge 和 Compose 根
        │   ├── data/
        │   │   ├── local/         Room Entity、DAO、Database、Migration
        │   │   ├── repository/    Repository 与 Entity/Domain 映射
        │   │   ├── settings/      DataStore 边界，不能散落到 Composable
        │   │   ├── templates/     旧模板元数据；仅 P2 装备库恢复时再使用
        │   │   ├── SampleEvents.kt 原型/预览种子数据，不是业务真相
        │   │   └── AppContainer.kt 手工依赖装配根
        │   ├── domain/
        │   │   ├── model/         不依赖 Android UI 的实体与共享内容契约
        │   │   └── time/          唯一时间计算内核与趣味文案生成
        │   ├── ui/
        │   │   ├── app/           App ViewModel、UI state、factory
        │   │   ├── home/          旧首页（Hero、卡片墙、搜索、长按排版）；迁移后可删除
        │   │   ├── daybook/       副本地图、月历、日列表
        │   │   ├── eventdetail/   旧时刻详情；迁移期参考
        │   │   ├── eventsettings/ 旧时刻表单/模板选择；迁移期参考
        │   │   ├── recyclebin/    回收站页面
        │   │   ├── settings/      设置页
        │   │   ├── components/    可复用卡片/导航/对话框/时间展示
        │   │   └── theme/         token、Theme、排版、Shape；新 UI 只从此取视觉值
        │   └── utils/             无业务归属的工具，例如背景资源读取
        ├── test/                  JVM：时间、映射、筛选、排版等纯逻辑
        └── androidTest/           Room migration、Compose 交互与无障碍测试
        └── main/res/              Android 静态资源、字体、图标、主题
```

目录约束：

- J 最终设计的新页面优先落入 `ui/moment/`、`ui/task/`、`ui/daybook/` 等明确 Feature；不要继续把大型业务 UI 堆进 `MomentMarkApp.kt`，也不必被旧 `eventsettings/` 的页面结构绑住。
- 新持久化事实先定义 `domain/model`，再加 `data/local` 与 `data/repository`；Composable 不可直接读写 Room 或 DataStore。
- 新视觉 token 只能进入 `ui/theme/`，并同步 `DESIGN_SYSTEM.md`。
- 设计稿 HTML 是可读规范，不是运行时 UI；不要把 HTML/WebView 引入 App。

## 5. 核心数据模型

### 5.1 目标时刻模型：`Moment`

J 最终设计中的“时刻”不是旧首页卡片的别名，而是独立的产品实体。创建表单显式让用户选择“未来·倒数”或“过去·正数”；该选择记录创建语义，而实际展示状态由创建语义和本地日期统一推导。

建议新的 `MomentEntity` / `Moment` 使用以下事实字段：

| 分类 | 目标字段 | 规则 |
| --- | --- | --- |
| 身份 | `id` | 稳定 ID；仅用于领域关系，不再绑旧卡片布局。 |
| 内容 | `title`、`note` | `note` 根据方向显示为 LORE 或 STORY。 |
| 创建方向 | `creationDirection: FUTURE_COUNTDOWN / PAST_ACHIEVEMENT` | 必须持久化，表示创建时选择的语义。 |
| 日期 | `anchorDate: LocalDate` | 未来=目标日期，过去=开始日期；首版只支持全日。 |
| 组织 | `groupId?`、`rarity?`、`isPinned`、`pinnedOrder?` | 置顶项全宽优先；多个置顶项的 `pinnedOrder` 由用户手动调整并持久化。 |
| 生命周期 | `deletedAt`、`createdAt`、`updatedAt` | 供回收站的 30 天规则使用。 |

**已确认的到期转换规则：** 对 `creationDirection=FUTURE_COUNTDOWN` 的时刻，目标日当天的派生状态为 `TODAY`；从次日起自动转为 `PAST_ACHIEVEMENT`。这只是同一条 Moment 的展示状态变化，不改 ID、日期、备注、分组、重要度或置顶，也不生成第二条记录。`creationDirection` 与 `displayState` 必须分开：前者持久化，后者由日期计算派生。

当前 `TimeEvent` / `time_events`、`TimeEventMapper`、`advancedConfigJson` 与模板字段均为遗留实现：

- 若其中已有需要保留的真实用户时刻，写一次**显式迁移**到 `Moment`；不得由 UI 悄悄猜测方向。
- **已确认（2026-08-28，用户拍板）：当前库中只有样例/开发数据，无真实用户数据。** 采用清空开发库 + 全新 `moments`/`tasks` schema，**不编写 `time_events` 数据搬运迁移**。落地仍须遵守 §6.7：提升数据库版本 + 显式建表 Migration（或开发设备卸载重装后以新版本全新建库）；禁止 `fallbackToDestructiveMigration()`。
- 若仅有 `SampleEvents` 或开发期原型数据，可在新结构上线时清空开发数据库；不能把原型字段强行带进新模型。
- 不要为了兼容旧卡片而把 `templateKey`、`TravelCardConfig`、`relativeLabel` 等塞进新 `Moment`。

### 5.2 时间模型：不可破坏的规则

| 类型 | 存储 | 显示/计算 |
| --- | --- | --- |
| P0 时刻/任务日期 | `LocalDate` | 使用本地自然日计算，不以毫秒除以 86,400,000 计算。 |
| P1 精确截止时间 | `Instant` + `ZoneId` | 只在 PRD 启用精确任务时间后引入。 |
| 倒数/正数/今天/自动转过去 | 不落库 | 根据 `creationDirection + anchorDate + Clock` 只由 `EventTimeCalculator` 派生。 |
| 重复规则 | P1 后再落库 | 由统一时间内核推导下一次，不能散落在页面。 |

所有时刻卡片、详情页、未来 Widget/通知都必须调用 `domain/time/EventTimeCalculator`。严禁模板、页面或数据库自己实现一套天数计算。

### 5.3 新首页的展示投影：`HomeCardItem`

新 J 首页不是旧模板池或自由卡片墙。它应先从 `Moment` 和已开启 `showOnHome` 的 `Task` 建立只读展示投影，再按 PRD 排列：

1. 置顶 Moment：全宽、按用户调整的 `pinnedOrder`；
2. 普通过去 Moment：左列、日期由近到远；
3. 普通未来 Moment：右列、日期由近到远；
4. 仅用户主动显示的 Task：位于右列、按所属/截止日期由近到远；到期日显示“就是今天”，日期过去后从首页投影隐藏，不改变它的 Task 身份。

可保留“内容与样式分离”的思想，但旧 `TimeCardFields` / `rememberTimeCardPresentation` 只是可选适配器。新页面应定义面向 J 卡片的单一 `MomentCardPresentation` / `TaskCardPresentation`，不能被旧旅行模板字段、示例标题或旧网格尺寸限制。

### 5.4 P0 待新增：日常待办（`Task`）

当前日子簿只用 `PrototypeDaybookDataSource` 将系统节日、已有时刻和示例记录映射为 `DaybookEvent`；它**不是**可完成、可持久化的待办系统。实现 PRD 的“新任务”时，应新增独立 Task 聚合，不能将任务伪装为 `TimeEvent`。

建议新增 `TaskEntity` / `Task` / `TaskRepository`（名称可按项目惯例调整）：

| 字段 | 类型/示例 | 规则 |
| --- | --- | --- |
| `id` | String UUID | 稳定主键。 |
| `title` | String | 必填。 |
| `dueLocalDate` | ISO String / `LocalDate` | 必填；决定它在哪天的日子簿出现。 |
| `dueInstant`、`zoneId` | nullable | 可选精确截止时间；不影响日历归属日。 |
| `note` | String | 可选任务描述 LORE。 |
| `taskType`、`difficulty`、`groupId` | nullable/枚举字符串 | 可选，匹配最终设计稿。 |
| `isCompleted`、`completedAt` | Boolean、nullable epoch millis | 勾选状态和完成时间。 |
| `showOnHome` | Boolean，默认 `false` | 只有用户主动开启才进入首页投影。 |
| `deletedAt`、`createdAt`、`updatedAt` | epoch millis | 与时刻同一软删除语义。 |

关系与投影：

```text
Moment ── 独立 ── Task
   │                  │
   └── P1 可用 `relatedMomentId` 建立可选关联 ──┘

HomeCardItem = Moment 投影 + showOnHome = true 的 Task 投影
Daybook = Task.dueLocalDate 的主视图 + 系统节日/时刻的只读标记
```

- Task 与 Moment 在 P0 没有必需外键；不得为了“前置目标”展示强行绑定二者。
- 待办跨日只是在今天的查询中自然不可见，**不**迁移、删除或改写原 `dueLocalDate`。
- `showOnHome=true` 的任务仍是 Task；它只在日期不早于今天时进入首页右列，到期后隐藏，不能因此被时间内核当成过去/未来 Moment。
- P0 不继承旧 `CardBoard` 的长按自由布局和 `gridWidth/gridHeight`。若 P2 重新引入可自由排版，再为新 `HomeCardItem` 设计独立布局存储；不能复用旧卡片墙语义。

### 5.5 其他持久化边界

| 存储 | 当前内容 | 约束 |
| --- | --- | --- |
| Room `moment_mark.db` | 迁移期 `time_events`；目标 `moments`、`tasks` | 可靠的用户业务事实、需要查询/排序/迁移的数据。 |
| DataStore `moment_mark_settings` | 主题模式、`autoPurgeEnabled` | 轻量全局设置；自动净化默认关闭。 |
| DataStore `moment_mark_groups` | 旧分组列表 | 迁移期兼容；新 P0 可先直接保存 `groupId`，P1 再决定是否建 groups 表。 |
| DataStore `moment_mark_event_details` | 旧背景/关联倒计时 | 不作为 J 最终详情页的必需数据源；保留或迁移须逐项确认。 |
| DataStore `moment_mark_templates` | 旧模板收藏 | P2 装备库前不参与 P0。 |
| DataStore `moment_mark_card_board` | 旧自由卡片墙位置 | P0 不读取；待确认后再删除或只做遗留数据清理。 |

选择原则：需要筛选、按日期查询、软删除恢复或与其他实体一致更新的数据进 Room；单个用户偏好或纯 UI 元数据可留 DataStore。

## 6. 关键设计决策与原因

### 6.1 保留轻量技术栈，但允许替换旧页面协调代码

可继续使用单 Activity、Compose、ViewModel、Repository 和手工 `AppContainer`，因为它们不与新设计冲突。`MomentMarkApp.kt` 的旧 `AppScreen` 状态机不是产品约束：

- 重做“大事件 / 新时刻 / 日子簿 / 回收站”流时，可以重写 route state 或引入轻量导航层。
- 不为旧页面的返回逻辑、抽屉、Hero 或设置入口保留路由分支。
- 迁移后的导航只需满足 PRD 的三入口、表单返回页、详情返回页与回收站入口；不要额外继承旧信息架构。

### 6.2 Moment 与 Task 分表，而不是一个万能 Event

时刻需要“过去/未来”的日期语义和卡片模板；待办需要完成状态、所属日、跨日隐藏、首页显示开关。将二者塞进一张表会产生大量无意义的 nullable 字段，并让“新时刻 / 新任务”入口混乱。

因此：目标结构为 `moments` 与 `tasks` 两张表；首页通过投影组合展示，而不是让两种实体互相伪装。是否迁移旧 `time_events` 由真实用户数据是否存在决定，不能为了兼容旧代码而把它当作永恒表结构。

### 6.3 事实落库、展示派生

`localDate`、`Instant`、标题、备注、置顶、完成状态是事实；“还有多少天”“是不是今天”“血条百分比”“下一里程碑”等都是展示派生。这样跨午夜、切换时区、恢复进程和未来 Widget 都能得到同一结果。

### 6.4 内容与 J 最终卡片样式分离

J 最终设计的卡片需要一个集中展示投影，保证标题、日期、倒数/正数、分组、重要度都来自 Moment/Task 事实。旧 `TemplateCatalog`、旅行卡、Small/Wide 选择器、旧渲染分发器并非 P0 依赖；可删除、下线或留待 P2 装备库重构后再接入。

### 6.5 首页顺序属于产品规则，不继承自由排版

首页顺序由 PRD 的置顶、方向和日期规则决定，不由旧 `CardLayoutStorage` 的用户拖拽坐标决定。`pinnedOrder` 只服务置顶之间的人工排序；其他卡片由查询/投影稳定排序。不要把 `gridWidth/gridHeight`、拖拽 governor 或旧卡片墙持久化迁入新 P0。

### 6.6 视觉变更只走 token

新 UI 只能使用 `ui/theme/` 中的 `MaterialTheme` 与 `MomentMarkTokens`。禁止新增硬编码 `Color(0x...)`、裸 `dp/sp`、圆角或柔和阴影。变更 token 时同步更新 `DESIGN_SYSTEM.md`；修改设计稿时同步 HTML 基准。

### 6.7 Room 迁移是不可跳过的交付物

任何 Room Entity/schema 变更必须同时完成：

1. 提升 `MomentMarkDatabase` 版本；
2. 写从上一个版本到新版本的 `Migration`；
3. 导出新的 `app/schemas/.../<version>.json`；
4. 增加或更新 `MomentMarkDatabaseMigrationTest`；
5. 真正跑迁移测试。

不得用 `fallbackToDestructiveMigration()` 绕过，也不得仅因本机数据少而删除数据库。

## 7. 后续功能的推荐落点

| 需求 | 应修改/新增的位置 | 不应做的事 |
| --- | --- | --- |
| 首页时刻排序、置顶全宽、过去左/未来右 | 新 `ui/moment/home/` 的 `HomeCardItem` 投影与 J 网格 | 不接回旧 Hero、自由卡片墙或模板选择器。 |
| 铭刻时刻的未来/过去模式 | 新 `ui/moment/form/` + `Moment.creationDirection` | 不把“已/还有 X 天”保存进 Entity；未来时刻仅在目标日次日由时间内核自动转为过去成就。 |
| 统一时刻详情 | 新 `ui/moment/detail/`，一套组件按派生 `displayState` 切换内容 | 不为过去/未来各复制一整套数据模型。 |
| 新任务、勾选、跨日隐藏 | 新 `domain/model/task`、`data/local/task`、`data/repository/task`、`ui/task`，并替换 `ui/daybook/` 数据源 | 不继续扩张 `PrototypeDaybookDataSource` 充当任务数据库。 |
| 待办显示在首页 | 新 `HomeCardItem` 投影、明确 `showOnHome` | 不将 Task 保存为 TimeEvent。 |
| 回收站 30 天 | Room DAO 增加 `purgeDeletedBefore(cutoff)`；仅在 `autoPurgeEnabled=true` 的 App 启动时清理，Task 同步实现 | 不使用当前“清空所有 deleted”的方法作为自动到期策略，也不引入后台定时删除。 |
| 分组 | P0 将 `groupId` 作为 Moment/Task 可选字段；P1 再决定 Room `groups` 表与管理页 | 不让旧 DataStore、模板 JSON 或样例数据成为分组真相。 |
| 通知/Widget | 复用 `EventTimeCalculator`，在 P1 再引入 WorkManager/Glance 等 | 不在 P0 引入后台定时链路。 |

## 8. 已知技术债与演进顺序

| 优先级 | 已知问题（代码事实） | 风险 | 建议处理方式 |
| --- | --- | --- |
| 高 | 日子簿是 `PrototypeDaybookDataSource`：含硬编码节日、原型用户记录和示例内容，没有 Task 持久化。 | 无法实现 PRD 的真实待办、完成状态、跨日规则。 | 以新 Task Room 模型/Repository 和 J 副本地图替换它，而不是继续修补原型。 |
| 高 | 现有首页 Hero、旧模板池、自由卡片墙、抽屉筛选和长按拖拽来自旧方案。 | 继续复用会违背 J 最终设计的固定三入口、置顶全宽和左右分列。 | 将这些视为可删除遗留 UI；只在明确映射到 J 设计的部分保留。 |
| 高 | 回收站当前可 `purgeDeleted()` 一次清空所有软删除，尚未按 `deletedAt`、用户设置实现 30 天到期。 | 与设计稿/PRD 的恢复期不一致，可能误删。 | 加按 cutoff 删除的 DAO；仅在用户开启自动净化后、App 启动时执行；手动清空仍要二次确认。 |
| 高 | 时刻删除不会原子清理 `EventDetailStore`、卡片布局等 DataStore 附属项。 | 永久删除后遗留孤儿数据；恢复/重建同 ID 时可能读到旧背景或关联倒计时。 | 在永久删除路径增加按 event ID 的清理；多存储操作需设计失败补偿。 |
| 高 | `advancedConfigJson`、`templateConfigJson` 使用 JSON 字符串承载多个字段。 | 字段演进、查询、迁移和类型安全较弱。 | 不要立即大爆炸重构；新 Task 使用显式列。未来只在确有查询/关系需求时渐进拆列。 |
| 中 | `MomentMarkApp.kt`（约 600 行）承载旧手工导航、跨页面状态和事件处理。 | 重做 J 页面时旧分支会干扰新路由。 | 以 J 三入口为边界重建协调层；不要迁移无对应页面的旧 screen。 |
| 中 | `HomeScreen.kt` 仍较大，且混合旧 Hero、搜索、拖拽、卡片墙职责。 | 局部改动容易把旧交互带回新首页。 | 新首页新建 Feature/组件；旧 HomeScreen 迁移完成后删除，不保留拖拽 governor。 |
| 中 | 当前多个 DataStore 之间无事务；分组重命名同时写 Room 与 DataStore。 | 中途失败可能留下不一致状态。 | 在操作层记录顺序并补偿；分组关系变复杂时迁移到 Room。 |
| 中 | 空数据库会写入 `SampleEvents.all`。 | 生产用户可能看到样例数据，且“空状态”无法区分。 | 发布前改为仅 debug/预览 seed，或首次启动明确询问是否载入示例。 |
| 中 | 旧 UI 存在大量旧暖纸风格硬编码色值。 | 新 J 页面会与旧页面视觉冲突。 | 新 J 页面只用 token；迁移完成的旧页面直接移除，不要求为保留它而 token 化。 |
| 中 | 日子簿的农历节日是按某年公历日期的原型表。 | 跨年后日期错误。 | P1 接入可验证的农历/节气数据源；在此之前不要把原型数据宣称为准确日历。 |
| 低 | `applicationId` 为 `com.cch.momentmark.app`，Activity package 仍为 `com.cch.momentmark`。 | 改 applicationId 会成为新 App 身份，旧本地数据不会迁移。 | 除非明确做数据迁移，不改 applicationId。 |
| 低 | 内置字体的再发布许可尚未核验。 | 正式上架存在许可风险。 | 发布前核实字体许可，必要时替换为可再分发字体。 |

推荐实施顺序：**J 三入口/导航骨架 → Moment 新模型与首页/表单/详情 → Task 数据层与副本地图 → 任务表单/勾选/跨日 → 回收站 30 天生命周期 → 清除旧首页/模板/拖拽遗留 → P1 提醒/重复/里程碑**。

## 9. 修改与验证清单

每次修改 UI 或功能前后，AI/开发者都应执行相应检查：

### 修改 UI 时

- [ ] 读取本文件、`PRD.md`、`DESIGN_SYSTEM.md` 和对应的 `proposal_J*.html` 屏幕。
- [ ] 以 J 最终设计的三入口、固定首页规则和页面层级为准；旧模板、Hero、拖拽排版只在设计稿明确保留时才迁移。
- [ ] 新视觉值来自 token；若新增 token，同步 `DESIGN_SYSTEM.md`。
- [ ] 检查 130%/200% 字体缩放、中文语义描述、触摸目标、边到边系统栏。
- [ ] 跑 `assembleDebug`；涉及交互再跑对应 Compose 测试或真机检查。

### 修改数据或时间逻辑时

- [ ] 全日日期仍是 `LocalDate`；精确时刻仍是 `Instant + ZoneId`。
- [ ] 相对时间只经 `EventTimeCalculator` 派生。
- [ ] 使用固定 `Clock` 测试未来时刻在目标日前、目标日当天、目标日次日分别为倒数、今天、过去成就，且 ID/用户字段不变。
- [ ] 时刻表单拒绝“未来·倒数”的过去日期和“过去·正数”的未来日期。
- [ ] 自动净化默认关闭；只有用户开启后，App 启动时才按 `deletedAt < now - 30 days` 清理，且不得启动后台定时删除。
- [ ] Room 版本、Migration、schema JSON、迁移测试同步更新。
- [ ] 编辑不会意外修改 `pinnedOrder`、`deletedAt`、Task 完成状态或已确认需要迁移的详情数据。
- [ ] 清楚报告 JVM、模拟器、真机和未验证范围，不能只报告“构建成功”。

### P2 重新引入模板/装备库时

- [ ] 定义新的 Moment/Task 展示投影，不使用静态示例内容。
- [ ] 只有 P2 已确认时才接入模板目录、选择器、分发器和预览页。
- [ ] 不因旧 `TemplateCatalog` 存在而恢复旧模板或 Small/Wide 自由网格。
- [ ] 验证与 J 最终首页布局兼容的 span、滚动性能和无障碍语义。
