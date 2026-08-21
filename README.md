# 刻间 · Moment Mark

这是根据 `时间管理App_Vibe_Coding设计方案.md` 建立的 Android 项目雏形。

阶段 2 的“原型债务收口与可信时间内核”、阶段 3“本地持久化基础”、阶段 4“持久化接入与可恢复编辑”、阶段 5“事件操作与最小 CRUD”、阶段 6“真实新增事件”和阶段 7“删除撤销”已完成；当前进行阶段 7B“质量加固”：

- Kotlin + Jetpack Compose + Material 3
- 单 Activity
- 应用显示名为 `Moment Mark`，安装包标识为 `com.cch.momentmark.app`；启动器使用圆角自适应图标，支持常见圆形、圆角矩形和方形蒙版。
- 主题与三态模式（跟随系统 / 浅色 / 深色；由 DataStore 恢复与保存）
- 首页卡片列表、分类抽屉筛选和设置入口
- 左侧暖白分类与分组抽屉：状态筛选、组合分组筛选、动态数量和分组管理
- 三段式蓝白/橙白事件卡片
- 主页默认展示的“奶油极简 · 日系编辑式”倒计时模板
- 底部时间轴风格悬浮导航：默认“大事件”，保留“日子簿”视觉占位
- 首次启动播种 Room 样例并读取未来事件和过去事件
- 旅行模板的内存实时编辑
- 可见的内存标题搜索：支持聚焦、清空、无结果和返回关闭
- 模板页三卡空间堆叠浏览：实时手势跟随、轻量透视、随机入位动画和 Small/Wide 横向切换
- 首页卡片长按编辑：拖动避让重排，并以 DataStore 本地保存卡片顺序与槽位规格
- `EventTimeCalculator` 统一处理 `ALL_DAY` / `TIMED`，支持注入 `Clock` 与 `ZoneId`
- 真实 JUnit 时间边界测试（今天、昨日、明日、闰日、月末、now、精确时刻、DST、跨时区）
- Room v2 schema、v1→v2 migration、DAO 和 `TimeEventRepository` 已建立并接入首页读取
- DataStore 三态主题设置已建立并接入设置页
- 首页首次启动播种 4 个真实样例；模板画廊仍为内存预览
- 事件设置页已有模板/旅行字段编辑会写回 Repository
- 事件设置页支持已有事件的标题编辑、置顶、归档和删除确认
- 首页底部时间轴导航中的加号进入真实新建表单，默认全天，支持精确时刻、标题、日期/时间校验和保存
- 删除确认后显示 Snackbar 撤销，撤销动作避开底部导航的加号触控区域
- 事件卡、搜索、设置、新建和时间轴导航节点提供明确的中文无障碍语义

当前 Room/DataStore 已接入首页读取、主题恢复、事件设置编辑、真实新增和删除撤销；分组筛选与分组名称已接入 DataStore，分组改名/删除会同步事件归属；归档/软删除已从活动首页隐藏。刻意未实现回收站、Widget、通知、网络、真实分类和用户配色保存。旅行模板已接入轻量日期计算；新旧模板均使用同一套可配置字段。

底部导航加号现在连接真实表单；回收站、Widget、通知、网络、账号和完整 CRUD 仍明确留到后续阶段。

## 打开与运行

1. 使用 Android Studio 打开本目录。
2. 等待 Gradle Sync 完成。
3. 选择 `app` 配置，在 Android 模拟器或真机上运行。

命令行构建：

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew testDebugUnitTest assembleDebug lintDebug
```

如果当前目录还没有 Gradle Wrapper，可使用 Android Studio 自带的 Gradle 或在本机执行一次 `gradle wrapper` 生成它。

## 验证边界

2026-08-19 使用 Android Studio 内置 JDK 执行 `testDebugUnitTest assembleDebug lintDebug`：14 个 JUnit 测试真实执行并通过，Debug APK 生成，lint 为 0 error、15 个非阻断的版本/工具提示（含 Kapt→KSP 建议）。另在 Pixel 9 Pro API 35 模拟器执行 `connectedDebugAndroidTest`：6 个 instrumentation tests 通过，并完成首页、搜索、设置、事件操作、新建事件、删除 Snackbar、立即撤销和返回键冒烟；130%/200% 字体缩放与 2856×1280 宽屏尺寸覆盖下的关键语义也已检查。

2026-08-21 已执行 `testDebugUnitTest`（29 个 JVM 测试通过）、`assembleDebug`（Debug APK 已生成）、`lintDebug`（成功）和 `assembleRelease`（R8/资源压缩后的未签名 Release APK 已生成）。首页滚动已改为平台默认 fling，并避免逐卡入场动画和全屏模糊离屏渲染；模板手势、随机入位、拖拽避让和真实滚动帧率仍需在模拟器/真机目测与性能验收。仍未验证：真实设备、真实 TalkBack 朗读、完整错误/加载状态、物理旋转矩阵，以及删除撤销的跨重启行为；桌面组件刷新、通知或设备重启行为也尚未实现。
