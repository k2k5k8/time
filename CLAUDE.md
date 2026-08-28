# CLAUDE.md — 刻间 MomentMark

## 如何给任何 AI/Agent 交接设计上下文

不需要喂截图或让 AI 先"看图生成提示词"——设计已固化为文本，直接按层读取：

1. **自动层**：Agent 在本项目开工时会自动加载本文件（CLAUDE.md），从而知道设计规则的存在。
2. **规范层**：需要具体取值时读 `DESIGN_SYSTEM.md`（token 表 + 组件规格 + 词表）。
3. **视觉层**：需要看效果基准时，让 Agent 直接读 `docs/design/proposal_J.html` 与 `docs/design/proposal_J_app.html` 的**源码**（HTML 是文本，解析比图片识别精确；浏览器打开仅供人类查看）。

新会话开场一句话即可：**「按 CLAUDE.md 的设计规则实现 XX 页面，视觉基准 docs/design/proposal_J_app.html 第 N 屏」**。

## 项目概况

Android 原生应用（时间/倒计时记录），Kotlin 2.2 + Jetpack Compose + Material 3，单 Activity。
存储：Room（事件表，schema 版本化迁移）+ DataStore Preferences（主题/分组/模板/卡片布局）。
无网络、无 DI、无导航库（自定义枚举路由 `AppScreen`）。

## 构建与测试

```bash
./gradlew assembleDebug        # 编译验证（改完必须跑）
./gradlew test                 # JVM 单测
./gradlew connectedAndroidTest # 真机测试（涉及 Room 迁移时必须）
```

- Room schema 升级必须先写 Migration + 迁移测试，否则不许升版本。

## 设计体系强制规则（像素冒险 Pixel Quest）

`DESIGN_SYSTEM.md` 是设计风格的**唯一权威来源**，视觉基准见 `docs/design/proposal_J.html` 与 `docs/design/proposal_J_app.html`。

1. **新 UI 禁止硬编码视觉值**：不允许新增 `Color(0x…)`、裸 `.sp`、裸 `.dp`。颜色用 `MaterialTheme.colorScheme.*`；字号用 `MaterialTheme.typography.*`；间距/描边/阴影/血条等像素元素用 `MomentMarkTokens.*`（均在 `ui/theme/`）。缺 token 就先在 `ui/theme/` 里成对补（昼/夜），并同步 DESIGN_SYSTEM.md §3。
2. **全局直角**：不要给任何组件设圆角（Shapes 已全局 0dp）。
3. **层级表达**：只用「3dp 墨线描边 + 3–4dp 硬偏移阴影」，禁用柔和阴影/渐变/模糊/半透明玻璃。
4. **血条三段变色**是数据语义（>60% 绿 / 30–60% 橙 / <30% 红），不许当装饰乱用。
5. **游戏化词表强制**：事件=任务、置顶=主线、删除=封印、回收站=封印之地、恢复=复活、永久删除=净化、模板=装备、日子簿=副本地图。文案命名见 DESIGN_SYSTEM.md §2。
6. **数字直接跳变**（不做滚动补间），按钮按压用「位移 2dp + 阴影缩短」而非透明度。
7. 改风格必须「token + DESIGN_SYSTEM.md」同步改，禁止只改一边。

## 存量迁移规则

旧「暖纸风格」文件（`ui/home/EventCard.kt`、`HomeHero.kt`、`AdaptiveBackground.kt`、`CategoryDrawer.kt`、`ui/eventdetail/`、`ui/eventsettings/`、`ui/daybook/` 等）仍有约 186 处硬编码颜色。**修改这些文件时，把本次触碰到的硬编码值顺手替换为 token**；不要求一次性全量迁移，但迁移完成后在 DESIGN_SYSTEM.md §7 登记。

## 行为红线（沿用既有契约，不因风格改变）

- 长按 500ms 进入卡片布局编辑；拖拽避让逻辑（governor）不要绕过。
- 删除必须二次确认 + Snackbar 撤销（避开底部加号触控区）；撤销只恢复最近一次软删除。
- 相对时间显示一律由 `domain/time/EventTimeCalculator` 派生，不落库、不写死。
- 无障碍：中文 content description；卡片合并语义；装饰 icon 隐藏。
- 字体缩放 200% 不得裁剪主信息；Small/Wide 两列网格规则不变。
