# TASKS

## 日子簿页面雏形（2026-08-21）

- [x] 接通底部“日子簿”入口与独立 Compose 页面。
- [x] 完成月视图、今天/选中态、月切换、周标题和日期事件横杠。
- [x] 完成系统事件 / 用户事件分层、当日详情和可点击空状态；移除重复底部新增胶囊。
- [x] 从日历日期统一携带 `initialDate` 进入新增日期页；返回/保存回到来源页，保留 `showInMilestone` 同步接口。
- [x] 首页与日子簿改用同一套轻量胶囊导航；压缩日子簿首屏间距并移除冗余页头。
- [x] 日子簿顶层避让状态栏/摄像头区域；已有事件日期提供独立的轻量新增按钮，空状态继续整块点击新增。
- [x] 增加 `DaybookDataSourceTest`，验证月份查询、事件来源和大事件同步候选字段。
- [ ] 后续接入独立 Daybook Room 表、真实节假日/节气数据和用户可编辑的同步开关。

## 卡片空间浏览与主页排版雏形（2026-08-21）

- [x] 模板页改为有界三卡空间堆叠：手势跟随、轻量 `rotationX`、连续缩放/透明度和柔和停靠。
- [x] “随机看看”沿模板浏览的连续位置入位；模板入口有轻量淡入/上移过渡；两处搜索输入统一为柔和半透明圆角样式。
- [x] 模板页顶部改为参考图的左右暖白悬浮圆钮；移除标题和独立背景色，按钮上移，搜索激活以轻暖强调态和按钮下输入面反馈。
- [x] 统一模板行的 Small/Wide 全局槽位；上下预览的左右尺寸卡按接近中心的进度渐显，消除纵向换选时左侧预览跳变。
- [x] 首页长按进入编辑布局，支持拖动、跨 Small/Wide 卡片的动态避让/重排、完成/返回退出和弹性落位。
- [x] 新增 `CardLayoutStorage`，保存 `cardId / order / gridWidth / gridHeight`；新增纯 JVM 重排测试。
- [x] 执行 `testDebugUnitTest`（28 个 JVM 测试）、`assembleDebug` 与 `lintDebug`。

## 首页创建入口与新增日期页紧凑化（2026-08-21）

- [x] 移除主页独立 FAB，将原有新建逻辑移入主页与日子簿共用的底部时间轴导航加号；日子簿加号预填当前选中日期。
- [x] 新增日期页返回按钮固定在状态栏安全区下方；首次新增默认选择全天，编辑已有事件保留原时间类型。
- [x] 移除时间、置顶和重复项的辅助介绍；日期/时间选择格改为等宽自适应布局，窄屏纵向排列并避免文本换行。
- [ ] 在真实模拟器和设备目测模板手势、随机跨多模板、卡片拖拽、字体缩放和 TalkBack；当前未实现可见的 2×2 高度跨度卡片。

## 当前阶段：阶段 7B · 质量加固（模拟器子集完成）

### 阶段背景

阶段 1（含原阶段 1.5）已经完成 Compose 雏形、首页视觉、分类抽屉和卡片模板探索；阶段 2 已收口原型债务；阶段 3/4/5/6 已完成本地持久化、已有事件操作和真实新增；阶段 7 补齐删除后的 Snackbar 撤销路径；本轮补齐分类抽屉的双维筛选与分组管理。回收站 UI 继续后置。

### 阶段 2 验收快照（2026-08-19）

- 13 个 JUnit 测试真实执行并通过（`testDebugUnitTest`）。
- `assembleDebug` 成功并生成 `app/build/outputs/apk/debug/app-debug.apk`。
- `lintDebug`：0 errors、15 warnings；为 Gradle/AGP/依赖版本提示及 Kapt→KSP 工具建议，未新增屏蔽规则。
- Pixel 9 Pro API 35 模拟器已完成首页、抽屉筛选、搜索有/无结果、清空、返回、设置三态主题、事件设置和硬件返回键冒烟。
- 字体缩放、旋转、TalkBack、真实设备和 Room 冷启动迁移尚未验证。

### 阶段 3 自动验收快照（2026-08-19）

- Room v2 schema、1→2 migration、DAO、Repository 和 DataStore settings store 已建立。
- 3 个 Android instrumentation tests 在 Pixel 9 Pro API 35 模拟器真实执行并通过：迁移列校验、Repository 时间形状校验/读写、主题持久化往返。
- 阶段 3 的持久化层仍未接入 Compose 首页；当前 UI 继续使用内存样例，避免越级实现 CRUD。

### 阶段 4 自动验收快照（2026-08-19）

- 首页首次启动会将现有 4 个真实样例播种到 Room，之后从 Room Flow 读取；模板画廊仍为不可持久化的展示预览。
- 设置页三态主题变更写入 DataStore，重新启动后继续读取保存值。
- 事件模板和旅行配置的现有编辑会写回 Repository；Mapper 往返测试已加入。
- Pixel 9 Pro API 35 模拟器启动后可见 Room 播种的“东京旅行”“开始学习 Kotlin”“研究生考试”等事件。

### 阶段 5 自动验收快照（2026-08-19）

- 事件设置页提供已有事件的标题/模板/旅行字段编辑、置顶、归档和删除确认。
- `observeActive()` 排除已归档和软删除事件；删除不直接物理清除数据库记录。
- 5 个 Android instrumentation tests 在 Pixel 9 Pro API 35 模拟器通过，新增归档/置顶/软删除活动流边界测试。

### 阶段 6 自动验收快照（2026-08-19）

- 首页新增真实创建入口，进入“新建事件”表单，不再是占位入口。
- 表单支持标题、全天/精确时刻切换、日期/时间输入和保存校验；保存通过 Repository 写入 Room。
- Pixel 9 Pro API 35 模拟器完成一次真实创建，临时事件保存后回到首页并可见。

### 阶段 7 自动验收快照（2026-08-19）

- 删除软删除后显示“事件已删除 / 撤销”Snackbar；Snackbar 避开首页创建入口的触控区域。
- 点击撤销会调用 `restoreDeleted`，恢复事件的活动流可见性。
- 5 个 Android instrumentation tests 在 Pixel 9 Pro API 35 模拟器通过，覆盖恢复后的活动流边界。

### 阶段 7B 自动与模拟器快照（2026-08-19）

- 事件卡、搜索、设置、新建和时间轴导航节点补齐 TalkBack 可发现的中文语义；事件卡的完整描述与点击动作覆盖同一卡片边界。
- 130% 和 200% 字体缩放下，关键入口与事件卡语义仍存在；2856×1280 宽屏尺寸覆盖下，首页关键语义与创建入口仍存在。
- `testDebugUnitTest` 14 个 JUnit、`connectedDebugAndroidTest` 6 个 instrumentation tests、`assembleDebug` 和 `lintDebug` 均通过；新增 Compose UI 语义/200% 字体测试。
- Espresso 已升级到 3.7.0，API 35 的 `InputManager.getInstance()` 兼容性问题已消除；真实 TalkBack 朗读仍待设备验证。

### 首页背景融合实现（2026-08-21）

- 新增 `AdaptiveBackgroundPaletteAnalyzer`：对 Hero 图片进行低分辨率采样，生成环境色、动态 UI 基底、卡片色调和环境阴影色。
- 新增 `AdaptiveTransitionLayer`：用模糊环境雾、动态渐变和复杂背景 Quiet Zone 消除图片与卡片区域的硬边界。
- 新增 `AdaptiveCardSurface`：作为现有卡片渲染器的半透明纸张外框，保留所有既有模板、卡片内容和 Small/Wide 两列/跨列规则。
- 背景进入使用约 850ms 淡入，卡片使用低位移淡入；顶层模板硬阴影已移除，改由环境色阴影承接。
- 当前只完成源码接入和自动构建验证；真实背景切换视觉、不同屏幕密度、性能和 TalkBack 仍需模拟器/真机复核。

### 阶段 2 目标（已完成）

1. 消除看起来可用但没有真实结果的交互。
2. 将巨型 UI 文件拆成可维护的 feature/component 边界，保持现有视觉不回归。
3. 建立统一、可注入时钟、可测试的全天与精确时刻计算。
4. 建立首批真实单元测试和可重复的构建/lint 门禁。

阶段 2 的原型债务、时间内核、测试与文档目标已完成；其禁止扩展项不因进入阶段 3 而追溯开放。

### 阶段 3 目标（已完成）

1. 建立稳定的 `TimeEventEntity`、Room DAO、数据库版本与可执行迁移。
2. 建立只负责本地数据边界的 `TimeEventRepository`，在写入前校验 `ALL_DAY` / `TIMED` 时间形状。
3. 使用 DataStore 保存三态主题设置，并为默认值、往返读写提供 Android 测试。
4. 保留当前 UI 的内存样例和卡片网格，等下一阶段再接入读取、编辑和正式 CRUD。

### 阶段 4 目标（已完成）

1. 首页事件源切换为 Room Flow，首次无数据时只播种既有样例，不改变卡片视觉和 Small/Wide 网格。
2. 主题设置从 DataStore 恢复，用户选择即时写入，未知值仍回退 `SYSTEM`。
3. 事件设置页已有模板/旅行字段编辑写回 Repository，重启后可恢复。
4. 增加 Domain↔Entity mapper 测试，明确数据库只保存事实字段。

### 阶段 5 目标（已完成）

1. 在事件设置页提供真实保存中的标题、模板/旅行字段编辑。
2. 提供置顶切换，并让首页“置顶”筛选读取持久化状态。
3. 提供归档和删除确认；归档/软删除后从活动首页消失，但数据库保留可恢复事实。
4. 保持当前无真实新增入口的边界，创建流程和删除撤销单独设计后再开放。

### 阶段 6 目标（已完成）

1. 提供真实新建入口和独立表单，标题为空或日期格式错误时不可保存。
2. 支持 `ALL_DAY` 与 `TIMED` 两种输入，精确时刻保留设备 ZoneId。
3. 保存后通过 Room Flow 回到首页，保持搜索、筛选和 Small/Wide 网格行为。
4. 不让表单直接操作 DAO；所有写入经过 Repository 的时间形状校验。

### 阶段 7 目标

1. 删除确认后立即从首页移除，并提供明确的撤销动作。
2. 撤销只恢复最近一次软删除，不物理清除或重建事件事实。
3. Snackbar 不遮挡创建入口，也不把撤销点击误传给底层首页控件。
4. 增加 Repository 恢复测试并保留当前删除确认行为。

### 阶段 7 必须完成

- 首页不得再把 Room 数据与同 id 的内存样例重复展示；模板画廊预览不得被播种为真实事件。
- 新建表单必须有真实保存结果和明确的取消行为，不能只显示提示。
- 编辑、置顶、归档、删除、新增和撤销写回必须经过 `TimeEventRepository`。
- 删除必须二次确认，归档和软删除不能物理清除记录。
- 任何相对时间、星期和状态仍由 `EventTimeCalculator` 派生。
- 保持搜索、抽屉筛选、四类卡片和 Small/Wide 两列行为不回归。
- 增加真实 Android instrumentation tests 覆盖 mapper 往返和持久化接入后的启动数据。
- 更新 `PRODUCT_SPEC.md`、`DESIGN_SYSTEM.md` 和 `README.md`，明确当前为可恢复读取/编辑而非完整 CRUD。

### 阶段 7 允许修改

- `data/local`、`data/repository`、`data/settings`、现有 Compose feature 边界
- `app/src/androidTest`、必要的 UI/mapper 测试
- `README.md`、`PRODUCT_SPEC.md`、`DESIGN_SYSTEM.md`、本文件

### 阶段 7 禁止扩展

- 不实现回收站 UI 和批量恢复；分组 CRUD 已限定为现有事件分组的本地管理，不扩展到独立的复杂分组模型。
- 不创建 Widget、通知、AlarmManager、WorkManager。
- 不引入网络、登录、同步、Firebase、分析或广告 SDK。
- 不新增卡片模板或扩大视觉探索范围，不改变 Small/Wide 两列网格。
- 不把派生倒计时、星期或相对文案冗余写入数据库。

### 自动验收

使用 Android Studio 内置 JDK：

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew testDebugUnitTest assembleDebug lintDebug

JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew connectedDebugAndroidTest
```

必须满足：

- `testDebugUnitTest` 不再是 `NO-SOURCE`，报告中能看到真实执行的 14 个 JUnit 测试。
- 所有时间边界测试通过，测试不依赖运行当下的系统日期或默认时区。
- `assembleDebug` 成功，APK 正常生成。
- `lintDebug` 0 error；warning 逐条分类，不以关闭规则掩盖新增问题。
- `connectedDebugAndroidTest` 在可用模拟器上真实执行 5 个 Stage 5 instrumentation tests 并通过。
- 源码中不再存在面向用户控件的空点击回调或“点击后只提示未来阶段”的假交互。

### 模拟器 / 真机验收

- 首页、抽屉、搜索、设置和事件设置页可以完整往返，返回键行为正确。
- Small 每排两张、Wide 跨两列，滚动位置与模板切换不回归。
- 系统浅色/深色切换，以及手动三态主题切换后主要文字和卡片可读。
- 搜索覆盖有结果、无结果、超长关键字、清空、旋转/配置变化。
- 字体缩放 100%、130%、200% 下，顶部栏、经典卡和至少一张旅行卡无关键操作被遮挡。
- TalkBack 能朗读顶部按钮、卡片整体语义和搜索状态。

### 阶段 7 完成定义

只有自动验收通过、Android instrumentation tests 通过，并完成至少一次模拟器或真机检查后，阶段 7 才能标记完成。回收站、Widget、通知和账号功能必须留到后续阶段。
