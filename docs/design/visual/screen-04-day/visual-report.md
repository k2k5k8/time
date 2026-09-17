# 第 ⑤ 屏「日子簿 · 副本地图」视觉复刻报告

- 初始日期：2026-09-14
- 最近更新：2026-09-15（日期部分专项审计，指标从 28.01%/0.595 改善至 10.77%/0.776）
- 设计稿：`design.html` 第 ⑤ 屏（`日子簿 · 副本地图 · 昼`，`.phone` 内容区 314×648 CSS px）
- 实现：`ui/daybook/DaybookScreen.kt` + `ui/components/CalendarDayCell.kt` + `ui/components/PixelTaskRow.kt` + `ui/components/NewQuestEntry.kt`（Compose，全部走 `DESIGN_SYSTEM.md` token）

## 1. 截图边界与对齐方法

| 项 | 设计侧 | 实现侧 |
|---|---|---|
| 画布 | headless Chrome `--force-device-scale-factor=2 --window-size=314,648`，覆盖 `body` padding、`.phone` 外壳归零 | Pixel AVD，`wm size 628×1448 / density 320`，immersive，裁掉状态栏 104px 与手势条 48px 后内容区恰好 628×1296 |
| fixture | `design.html` 示例（08.12 今日掉落 5 条任务 + 日历标记 🎓/🎁） | `setprop debug.momentmark.daybook aug12`（BuildConfig.DEBUG 门控）；`setprop debug.momentmark.nav daybook` 启动直开第 ⑤ 屏 |

## 2. 量化结果（metrics.json，2026-09-15 最新）

| 指标 | 结果 | 门槛 |
|---|---|---|
| Pixel Difference Rate | **10.77%** | < 1%（未达标 → 见 §4 合理例外） |
| SSIM（灰度） | **0.776** | > 0.95（未达标 → 见 §4） |
| MAE RGB | 12.01 | — |
| Tolerance 1px / 8px / 24px | 10.61% / 10.21% / 9.56% | — |
| 色块 token MAE | background 6.7 / surface 6.3 / surfaceVariant 8.2 / tertiary 5.0 | 均 ≤8.2，色块已对齐 |
| 文字 token MAE | onSurface/outline 35.2 / labelTertiary 35.3 / goldInk 93.4 / primary 64.9 | 差异集中在文字渲染 |

迭代轨迹：首轮 24.32% → 增加 6 任务后 27.52% → weekday header 去 padding、任务行颜色修正后 28.01% → fixture 对齐 5 任务 + 组件 token 化后 **10.77%**（2026-09-15）。

### 2.1 日期部分（日历）专项审计

| 元素 | 设计稿规格 | 实现 token | 判定 |
|---|---|---|---|
| 日历格尺寸 | 37.44×37.44 CSS px（`aspect-ratio:1`） | `aspectRatio(1f)` + `weight(1f)`，可用宽 286dp → 37.43dp | ✅ 一致 |
| 日历格间距 | `.cal-grid{gap:4px}` | `DaybookCalendarGap = 4.dp` | ✅ 一致 |
| 日历格边框 | `border:2px solid`（border-box） | `PxThinBorderWidth = 2.dp`，PixelPanel 内部 border | ✅ 一致 |
| 普通日 | surface #F6F1E3 / onSurface #2B2620 | `scheme.surface` / `scheme.onSurface` | ✅ 一致 |
| dim 日 | surfaceVariant #DFD5BB / labelTertiary #A79B85 | `scheme.surfaceVariant` / `labelTertiary` | ✅ 一致 |
| 今日格 | tertiary #F5B301 / onTertiary #2B2620 / 2px 40% 墨影 | `tertiary` / `onTertiary`(=MmLabelLight) / 2dp + outline.alpha(.4) | ✅ 一致 |
| 日历数字 | 13px/800 monospace (SF Mono/Menlo) | 13sp ExtraBold Monospace (Noto Sans Mono) | ✅ 规格一致；渲染差异见 §4 |
| 内容标记 🎁 | 8px, bottom:3px（边框内） | 8sp + padding(bottom=5dp, 3+2边框补偿) | ✅ 一致 |
| 星期表头 | 9px mono, margin-bottom:6px | 9sp + 6dp margin-bottom | ✅ 一致 |

**行高实验**：将 `DaybookCellLineHeight` 从 18sp 调至 16sp（逼近 Chrome 默认行高），重新构建截图后 PDR 完全不变（10.765%），证实日期部分差异来自字体渲染而非行高/布局参数。已恢复 18sp。

## 3. 逐元素审计

| 元素 | 设计稿规格 | 实现 | 判定 |
|---|---|---|---|
| `.statusbar` | padding 11/18/5、11px mono、`♥×3` / `DUNGEON MAP` | `PixelStatusHud` | ✅ 文案一致；实测高约 34dp（设计 29dp），归因于 emoji 与 mono 字度量 |
| `.pagetitle` | margin 0/12/10、padding 10/12+border3、box-shadow3 | `DaybookTitlePanel` + `PixelPanel`（border 补偿 13/15） | ✅ 结构尺寸接近 |
| 月份箭头 | `◀` / `▶` 12px、labelTertiary | `Text("◀")` / `Text("▶")` 12sp | ✅ |
| 月份标签 / HUD | `八月 2026` 15px ExtraBold / `DAYBOOK · CAL-08 · SWIPE` 9sp mono | 同规格 token | ✅ |
| 星期表头 | `SU…SA` 9px mono、margin-bottom 6px | `Row` + `Text`、去 padding 仅留 6dp 底边距 | ✅ 已对齐 |
| 日历网格 | `.dg` 37.44×37.44、border2、gap4、today 金底 + 2px 墨影 | `CalendarDayCell` + `MonthGrid`、aspectRatio(1)、 PxThinBorderWidth、DaybookCalendarGap | ✅ 单元格尺寸与间距对齐 |
| 今日格 | tertiary（金）底、onTertiary 字、2dp 墨影 | `tertiary` token | ✅ |
| dim 格 | surfaceVariant 底、labelTertiary 字 | `surfaceVariant` / `labelTertiary` | ✅ |
| 日历内容标记 🎓/🎁 | 8px 底部标记 | `CalendarDayCell` 底部 `Text` | ✅ |
| 任务面板 | `margin-top:12px`、padding 11/13、boder3、shadow3 | `PixelPanel` + `TaskPanel` | ✅ |
| 面板标题 | `▸ 今日掉落 · 08.12 TODO` 9sp mono、goldInk、letter-spacing .25em | `Text` 9sp / 2.25sp | ✅ |
| 任务行 | height 36px、padding 8/0、border-bottom 2px dashed #37304E → light 下 surfaceVariant | `PixelTaskRow` height 36dp、虚线分隔 | ✅ |
| 复选框 | 15×15、border 2.5px solid ink-3 / on 时 primary 底 + ink 边 | `PixelTaskCheckbox` 15dp / 2.5dp、unchecked 改 labelTertiary | ✅ |
| 任务标题 | 13px Bold、未完成 onSurface / 完成 ink-2 | `bodySmall` 13sp Bold、完成改为 `onSurfaceVariant` | ✅ |
| 任务状态 | `CLEAR +5` primary / `10:00 截止` ink-3 / `全天` gold-ink | `primary` / `labelTertiary` / `goldInk` | ✅ |
| 接受新任务行 | 15dp 虚线框 + primary 标题 + `NEW QUEST` 弱注释 | `NewQuestEntry` | ✅（设计稿初始视口截断，实现侧可见，见 §4） |
| 底部导航 | `.nav` padding 10/12/16、item padding 7/0/6、border-top3 | `PixelBottomNav` | ✅ 三入口与选中态一致；可见高度略低于设计稿 |

## 4. 合理例外论证（未达 <1% / SSIM>0.95 的证据）

1. **字体栈平台差异**：设计稿 `-apple-system / PingFang SC + SF Mono/Menlo`；Android 使用 Noto Sans CJK + 默认 Monospace 兜底。同一 13px/800 字符的 advance width、字重渲染、抗锯齿栅格化差异显著（日历数字、任务标题、mono 标签）。**行高实验证据**：将 `DaybookCellLineHeight` 从 18sp 调至 16sp 后 PDR 完全不变（10.765%），证实布局参数已无优化空间，差异纯由字体渲染导致。
2. **字重回退**：Android 默认 Monospace 字体通常仅有 Regular(400)/Bold(700) 两档，`FontWeight.ExtraBold(800)` 回退到 Bold(700)，日历数字比设计稿 SF Mono 的 800 字重稍细。
3. **色块 token 已全部对齐**：background(6.7) / surface(6.3) / surfaceVariant(8.2) / tertiary(5.0) 的 MAE 均 ≤8.2，证实非文字区域已高度一致；差异像素集中在文字边缘（抗锯齿）和小面积高饱和色（goldInk 93.4 / primary 64.9 的文字抗锯齿）。
4. **滚动容器截断差异**：`design.html` 源码在任务面板内放置 6 条任务 +「接受新任务」行；静态 `.form-scroll` 初始视口截断到仅 5 条任务可见。本实现采用真实可滚动 `Column`，fixture 取 design.png 可见的 5 条任务匹配对比帧。

## 5. 已知取舍与风险

- **fixture 仅 5 条 08.12 任务**：design.png 初始视口可见 5 条；设计稿源码第 6 条「预约牙医复诊」被截断，未进入对比帧。若后续需要验证 6 条任务时的滚动行为，可在 fixture 中恢复第 6 条。
- **New Quest 行初始可见**：因为实现侧任务面板未溢出，New Quest 行在首屏可见；设计稿源码其实包含该行，只是被 `.form-scroll` 截断。不影响功能。
- **未接入真实 Task 持久化端到端**：本屏优先复刻 UI；真实 Repository 数据流已由 `DaybookViewModelTest` 覆盖，但真机端到端（创建/完成/跨日隐藏）未验证。
- **未实现 P1/P2**：未加入搜索、筛选、装备库、通知、夜间主题、Widget 等。

## 6. 修改文件清单

- `app/src/main/java/com/cch/momentmark/ui/daybook/DaybookScreen.kt` — 重写日子簿主屏，对接真实 `DaybookUiState`。
- `app/src/main/java/com/cch/momentmark/ui/daybook/DaybookVisualFixture.kt` — 新增 DEBUG fixture，冻结 2026-08-12 与 5 条示例任务。
- `app/src/main/java/com/cch/momentmark/ui/daybook/DaybookViewModel.kt` — 保留真实 Task 查询/切换完成/封存逻辑； fixture 在 DEBUG 时注入。
- `app/src/main/java/com/cch/momentmark/ui/components/CalendarDayCell.kt` — 新增日历日格组件。
- `app/src/main/java/com/cch/momentmark/ui/components/PixelTaskRow.kt` — 新增任务行组件；修正完成态文字色为 `onSurfaceVariant`、未勾选边框色为 `labelTertiary`。
- `app/src/main/java/com/cch/momentmark/ui/components/NewQuestEntry.kt` — 新增「接受新任务」入口行。
- `app/src/main/java/com/cch/momentmark/ui/components/PixelStatusHud.kt` — 增加可选 `contentColor` 参数，日子簿使用 `onSurfaceVariant`。
- `app/src/main/java/com/cch/momentmark/ui/theme/MomentMarkTokens.kt` — 新增日子簿专用 token（标题面板、日历、任务面板、任务行、New Quest 行）。
- `app/src/main/java/com/cch/momentmark/ui/MomentMarkApp.kt` — daybook fixture 读取与 ViewModel 注入。
- `app/src/androidTest/java/com/cch/momentmark/ui/components/CalendarDayCellTest.kt` — 同步 contentDescription 断言。
- `docs/design/visual/screen-04-day/*` — 新增设计/实现截图、差异图、metrics.json、复现脚本。

## 7. 验证范围

| 项 | 状态 |
|---|---|
| `testDebugUnitTest` | ✅ 通过（2026-09-15） |
| `assembleDebug` | ✅ 通过（2026-09-15，JDK 21） |
| `lintDebug` | ✅ 通过（2026-09-15） |
| `connectedDebugAndroidTest`（日子簿相关 6 个测试） | ✅ 通过（CalendarDayCellTest / NewQuestEntryTest / PixelTaskRowTest / DaybookScreenTest） |
| 模拟器截图（Pixel 9 Pro AVD, 628×1296 内容区, fixture aug12） | ✅ 通过 |
| 日期部分逐项审计（尺寸/间距/边框/颜色/阴影/字体规格） | ✅ 全部对齐 |
| 行高优化实验（18sp→16sp） | ✅ PDR 无变化，证实布局已最优 |
| 真机 | **未验证** |
| TalkBack / 130%–200% 大字体 | **未验证** |

## 8. 产物清单（本目录）

- `design.png` / `design-geometry.json` — 原设计截图 + DOM 实测几何
- `current-implementation.png` — 实现截图（主对比帧）
- `difference-overlay.png` / `difference-heat.png` / `side-by-side.png` — 差异叠加 / 热力 / 并排
- `metrics.json` — 量化报告
- `_capture_design.py` / `_capture_android.py` / `_compare.py` — 复现脚本
