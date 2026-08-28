# Moment Mark 架构优化执行计划

本计划按“每次只改一个边界、每步可回滚、通过验收才进入下一步”设计。

## 总规则

- 不引入 Navigation-Compose 或 Hilt，直到阶段 2 完成并验收。
- 不改卡片模板、视觉、文案、时间计算和现有交互。
- 每阶段结束必须给出：改动文件、测试命令及结果、未验证项。
- 构建成功不等于真实设备验证；涉及界面行为的阶段额外跑 emulator instrumentation。

## 阶段 0：建立基线

执行：

```bash
git status --short
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew testDebugUnitTest assembleDebug lintDebug
```

验收：

- 工作区状态已记录。
- JVM 测试、debug 构建、lint 均通过。
- 若基线失败，先修基线；不要开始重构。

## 阶段 1：只拆 UI 文件，不改状态和导航

目标：让 `MomentMarkApp.kt` 从 3257 行降至 **750 行以内**，但行为完全不变。

按以下顺序移动：

1. 将 `HomeScreen` 移至 `ui/home/HomeScreen.kt`。
2. 将 `EventCard` 及各模板卡片渲染函数移至 `ui/home/EventCard.kt`。
3. 将 `TouchBar`、时间轴节点、空状态等首页组件移至 `ui/home/HomeTimeline.kt`。
4. 将 `EventSettingsScreen` 移至 `ui/eventsettings/EventSettingsScreen.kt`。
5. 将 `SettingsScreen` 移至 `ui/settings/SettingsScreen.kt`。
6. 保留 `MomentMarkApp.kt` 中的根 Compose、抽屉、`AppScreen` 路由和回调连接。

约束：

- 保持函数签名、`rememberSaveable` key、Modifier 链和可访问性描述不变。
- 现有 `HomeFeature`、`SettingsFeature` 等 wrapper 可以保留，避免一次同时改调用关系。
- 不删除任何旧卡片或模板兼容映射。

验收：

```bash
./gradlew testDebugUnitTest lintDebug
./gradlew connectedDebugAndroidTest
wc -l app/src/main/java/com/cch/momentmark/ui/MomentMarkApp.kt
```

重点观察：首页筛选、抽屉、创建/编辑/删除/撤销、日子簿跳转、卡片点击与 TalkBack 描述。

## 阶段 2：提取应用状态持有者，不上 Hilt

目标：根 Composable 不再负责创建数据依赖、收集 Flow 和执行 CRUD。

新增：

- `data/AppContainer.kt`：集中创建 `MomentMarkDatabase`、`TimeEventRepository`、各 DataStore。
- `ui/app/MomentMarkAppViewModel.kt`
- `ui/app/MomentMarkAppUiState.kt`
- `ui/app/MomentMarkAppViewModelFactory.kt`

状态归属：

| 放入 ViewModel | 继续留在 Compose |
| --- | --- |
| Room 事件流、主题、分组、初始化 seed、保存/置顶/归档/软删除/恢复 | Drawer 展开状态、动画、4 秒撤销倒计时、页面局部输入状态 |
| 异步加载状态与错误状态 | 单次视觉过渡状态 |

实施要求：

- 给 Repository、Settings Store、Group Store 抽出最小接口，供 fake 实现测试。
- `MomentMarkApp.kt` 只获取 ViewModel、收集 `uiState`、保留自定义 `AppScreen` 状态机。
- 不引入 Hilt；使用手写 Factory 即可。
- 增加 ViewModel 单测：首次加载、保存、软删除、撤销、分组变更、主题变更。

验收标准：

- `MomentMarkApp.kt` 内不存在 `MomentMarkDatabase.create()`、`TimeEventRepository(...)`，也不直接收集 repository/settings/group Flow。
- 新增 ViewModel 单测通过。
- 不发生旋转或重组后重复 seed 的问题。

## 阶段 3：把 Room 迁移测试升级为标准验证

现状已有 v1→v4 迁移测试，因此不是从零开始；本阶段提升其覆盖强度。

修改：

1. 在 `gradle/libs.versions.toml` 增加 `androidx.room:room-testing`。
2. 在 `app/build.gradle.kts` 增加对应 `androidTestImplementation`。
3. 重写 `MomentMarkDatabaseMigrationTest.kt`：
   - 使用 `MigrationTestHelper`；
   - 保留现有手工 v1 fixture（当前仓库没有 v1 schema JSON）；
   - 覆盖 v2→v4、v3→v4；
   - 验证迁移后不仅有列，还能读取旧记录、保留关键值、通过目标 schema 校验。
4. 规定以后任何 `@Database(version = N)` 增长必须同时提交：migration、schema JSON、从上一版本到新版本的测试。

明确禁止：为绕过开发错误而添加全局 `fallbackToDestructiveMigration()`。

验收：

```bash
./gradlew connectedDebugAndroidTest
```

## 阶段 4：明确软删除策略并实现回收站

采用安全默认策略：**不自动永久删除；用户在回收站中手动清空。**

修改：

- DAO/Repository 增加 `observeDeleted()`。
- 新增“回收站”入口和页面：恢复单条、永久删除单条、清空回收站（二次确认）。
- `purgeDeleted()` 仅由“清空回收站”调用，不能在后台静默调用。
- 为恢复、永久删除、空列表、重启后仍可见补测试。

验收：

- 删除后首页不可见。
- 撤销窗口结束后，记录在回收站仍可恢复。
- 只有用户确认“清空”才永久删除。

## 阶段 5：处理日子簿数据源，不再把 mock 当正式数据

先做无争议的结构调整：

- 将 `MockDaybookDataSource` 改名为 `PrototypeDaybookDataSource`，避免语义误导。
- `DaybookFeature` 改为接收 `DaybookDataSource`，不在 Composable 内直接创建数据源。
- 保留用户 `TimeEvent` 映射逻辑，并为其补测试。

接入真实数据前必须由产品确定以下范围：

- 只展示公历节日，或支持中国农历、节气、调休；
- 是否必须离线；
- 数据源是否可商用、可追溯。

没有以上决定时，Agent 不应凭空继续维护硬编码农历日期；那会把原型数据伪装成准确日历。

## Agent 固定执行提示词

```text
执行“Moment Mark 架构优化执行计划”的第 N 阶段，且只执行该阶段。
不得修改卡片模板、视觉、文案、时间计算规则或未涉及的业务行为。
开始前检查 git diff；保留已有用户改动。
完成后运行本阶段规定的测试，并报告：
1. 修改文件；
2. 测试命令和实际结果；
3. 未覆盖的 emulator、真机、TalkBack 或数据准确性验证；
4. 是否达到本阶段验收标准。
未通过验收时继续修复当前阶段，不进入下一阶段。
```
