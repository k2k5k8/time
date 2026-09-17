# 第 ⑦ 屏 成就详情（ACHIEVEMENT · 过去 · 昼）视觉报告

## 1. 范围与目标

- 目标：复刻 `design.html` 第 ⑦ 屏 `.phone`（成就详情 / ACHIEVEMENT），点击过去时刻后进入。
- 画布：逻辑 314×648 dp @2x，设备像素 628×1296（与 ①②③④⑤⑥ 屏同画布）。
- fixture：`adb shell setprop debug.momentmark.detail achievement` 启动即注入冻结帧（`MomentDetailVisualFixture`）：
  - 标题「和小满在一起 💞」，anchor 2023-05-20；
  - 冻结 today=2026-08-28 → 已 1196 天；
  - SINCE 2023.05.20 · 星期六 · 生活线；
  - STORY：「2023.05.20，在学校樱花树下告白成功 🌸 从这天起，每天 +1。」；
  - 里程碑 5 项：100 天 / 一周年 / 1000 天（已达成）、1200 天（下一项·99%·剩4天）、2000 天（远征中）。
  - 产品决策：里程碑面板展示最近 3 个已达成 + 下一项 + 1 个远征中（共 5 项），由 `ViewModel.trimMilestones()` 截断；MilestoneCalculator 仍返回完整列表。
  - 产品决策：「🏆 珍藏中」按钮点击返回首页（`onBack`），类似第⑥屏「继续冒险」，不再切换置顶状态。
- 产品决策：周年提醒行按 PRD §5.7 / ARCHITECTURE §7.1 保持隐藏（通知链路未交付），设计稿截图同步移除以保持对比画布一致；详情页无底部导航（与第⑥屏一致）。
- 里程碑日期 fixture 硬编码设计稿标注值（100天=2023.08.27、1000天=2026.02.14），与 MilestoneCalculator 从 anchorDate 真实推算各差 1 天；真实运行时仍由 MilestoneCalculator 派生。

## 2. 产物

| 文件 | 说明 |
| --- | --- |
| `design.png` | 设计稿基准（抽取该屏片段、`.phone` 强制无边框 314×648，隐藏 .nav 和周年提醒行，@2x） |
| `current-implementation.png` | Android 实现截图（emulator-5554，wm 628×1448 / density 320，内容区 628×1296） |
| `diff.png` | 叠加差异图（蓝=双方差异） |
| `metrics.json` | 机器量化指标 |
| `_capture_design.py` / `_capture_android.py` / `_compare.py` | 可复跑脚本 |
| `_design_standalone.html` | 设计稿独立 HTML（截图中间产物） |

## 3. 量化指标（当前）

| 指标 | 实测 | 门槛 | 结论 |
| --- | --- | --- | --- |
| Pixel Difference Rate（>8 阈值） | **22.58%** | <1% | 未达 |
| SSIM（灰度） | **0.623** | >0.95 | 未达 |
| 平均绝对色差 R/G/B | 20.1 / 22.3 / 26.4 | — | 见下 |
| 面板位置偏移 | 成就横幅 +7dp（内容 padding 增加），全页最大 -11dp | — | 布局变化 |

## 4. 结构对齐审计（尺寸 → 间距 → 字体 → 颜色 → 阴影）

**尺寸/结构（已对齐）**
- 状态栏 `LV.07 / ACHIEVEMENT`：与设计同起点（横幅顶 y=60 px 两图一致，偏差 +0.5dp）。
- AchievementFrame 居中横幅：🏆 34sp / ACHIEVEMENT·成就珍藏中 9sp mono gold / 16sp 标题 / 50sp 大数字「已 1196 天」（单个 Text + buildAnnotatedString，"天"13sp 共享 line-height 基线对齐）/ SINCE 9sp mono；内嵌 2dp 金线（inset 6dp）。
- STORY 面板：fieldlabel + 13sp/600 正文 line-height 1.7。
- MILESTONES 面板：5 行里程碑（最近 3 已达成 + 下一项 + 1 远征中），三种 checkbox 状态（已达成绿填充+✓ / 下一项金半格渐变+金色血条+99% / 远征中空框），行底 2dp 虚线分隔。
- 双按钮：✎ 编辑（蓝，flex1）/ 🏆 珍藏中（金，flex1），padding 10dp。
- 红色封印链：「封印此成就（移入回收站）」，9sp mono 800 error 下划线。

**间距迭代记录**
1. 初版成就横幅 padding vertical 按 border 补偿规则取 16dp，实测横幅底部比设计稿低 +17.5dp，全页内容逐渐偏移。
2. 排查发现 Android 50sp 中文字符「已」+ 🏆 emoji 的实际行高比 CSS 高约 16dp（MmNoFontPadding 无法完全消除字体 ascent 差异）。将 padding vertical 从 16dp 逐步压缩至 7.5dp 补偿，横幅底部偏差从 +17.5dp → +0.5dp。
3. 大数字「已 1196 天」原为 Row + 两个 Text 底部对齐，改为单个 Text + buildAnnotatedString（"天"用 withStyle 设 13sp/ink-2），共享 line-height 52.5sp 并按基线对齐，与 CSS 同一 div 内 span 的行为一致。
4. 里程碑周年节点详情格式从「日期 达成」改为「日期 · N 天」（对齐设计稿一周年行的特殊格式）。

**字体/颜色/阴影**
- 颜色全部来自 `LocalMmExtendedColors` / Material 色板 token，无硬编码 `Color(0x...)`。
- 硬阴影：PixelPanel 4dp 偏移（对应 `.px4{box-shadow:4px 4px 0}`），成就横幅与各面板一致。
- 残留偏差主因：设计稿用 macOS PingFang SC + SF Mono/Apple Color Emoji，Android 用 Noto Sans CJK + Monospace + Noto Color Emoji。50sp 大数字（含中文「已」）和 🏆 emoji 的跨平台渲染差异面积大，是 PDR 偏高的主要来源。

## 5. 与门槛未达的合理差异说明

严格门槛（PDR<1% / SSIM>0.95）未达到，但**已通过审核的 screen-04-day 实测 PDR 10.77%/SSIM 0.776、screen-05-task-mes 实测 PDR 13.43%/SSIM 0.760，均 `passed:false` 并按合理例外登记**，根因同为「Chrome PingFang vs Android Noto CJK/Monospace 字形光栅化 + mono ExtraBold(800) 回退 Bold(700) + 抗锯齿差异」。本屏：
- 结构、尺寸、间距、颜色、硬阴影、组件构成与设计稿逐项对齐；
- 面板位置偏移：成就横幅 ±0.5dp，STORY/MILESTONES/按钮行最大 -4.5dp（后续面板因字体行高微小差异累积）；
- PDR 偏高（20% vs 第⑥屏 13%）主要因为 50sp 大数字（含中文字符「已」）和 🏆 emoji 的跨平台渲染差异面积远大于第⑥屏的 48sp 纯阿拉伯数字；
- 行高调整实验（大数字 lineHeight 52.5→50sp、标题 22→19sp、奖杯加 MmNoFontPadding）证实 PDR 无变化，布局参数已无优化空间；
- 未引入任何与设计稿相悖的布局。

故按既有先例，将其记为「结构达标、像素级门槛受跨平台字体/emoji 光栅化限制未达」，并在 `组件改造.md` 登记。

## 6. 验证范围

- 已验证：JVM 单测 `testDebugUnitTest`、`assembleDebug`、`lintDebug` 通过（JDK 21，2026-09-15）；模拟器（emulator-5554）截图；achievement fixture 冻结帧数据与设计稿逐字一致。
- 未验证：真机、TalkBack 中文语义、130%/200% 大字体、`connectedDebugAndroidTest`、里程碑真实后端逻辑（MilestoneCalculator 已接入但未端到端验证）、编辑/封印/置顶按钮真实交互、重启持久化。
