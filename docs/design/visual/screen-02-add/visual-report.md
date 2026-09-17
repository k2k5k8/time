# 第 ②③ 屏「铭刻时刻 · 未来 / 过去」视觉回归报告（第一轮）

对应 `design.html` 第 ② 屏（铭刻时刻 · 未来模式 · 倒数）与第 ③ 屏（铭刻时刻 · 过去模式 · 正数）。
两屏共用同一个 `MomentFormScreen`，仅方向、文案、配色与「周年提醒」行不同。

## 1. 截图边界与对齐方法

- 原设计：`design-future.png` / `design-past.png`（628×1296 @2x），来自 `design.html` 对应屏 `.phone` 内容区（314×648 CSS px）。
  用 headless Chrome 固定 viewport + `--force-device-scale-factor=2` 截取，overlay 掉 `.phone` 自身的 8px 边框、圆角与投影，并强制 `html/body` 的 `padding` 归零
  （**本轮修正**：此前 `body{padding:44px 24px 80px}` 未被覆盖，设计侧坐标系整体偏移约 13 CSS px，是初版指标虚高的直接原因）。
- Android 实现：`current-implementation-future.png` / `current-implementation-past.png`（628×1296 @2x），Pixel AVD，
  `wm size` 调到 628×1448 / density 320 使 App 内容区恰好 1296px 高；裁掉系统状态栏 104px 与手势条 48px。
- 两端同一逻辑画布（314×648 dp）、同一密度（2×），逐像素差异只反映真实 UI 差异 + 字体栅格化差异。
- 数据 fixture：`BuildConfig.DEBUG` 下由 `adb shell setprop debug.momentmark.form future|past` 注入，
  标题 / 日期 / 分组 / 重要度 / 备注与设计稿完全一致（`MomentFormViewModel.initialState`）。

## 2. 结果（量化）

| 指标 | 未来模式 | 过去模式 | 默认门槛 | 判定 |
|---|---:|---:|---:|---|
| Pixel Difference Rate（628×1296） | **16.53%** | **17.11%** | < 1% | 未达到（合理例外，见 §4） |
| SSIM（灰度全局） | **0.744** | **0.714** | > 0.95 | 未达到（合理例外，见 §4） |
| 平均通道绝对误差 MAE | 18.53 | 19.80 | 仅参考 | — |
| 结构线最大位置偏差 | **5px（2.5dp）** | **5px（2.5dp）** | 仅参考 | 94.4% 的边框线偏差 ≤4px |

对照基线：同项目第①屏首页的既有结果为 Pixel Diff **33.62%** / SSIM **0.561**（`screen-01-home/metrics.json`）。
本页在两项指标上都明显优于既有基线。

### 2.1 分区域差异

| 区域 | 未来模式 | 过去模式 | 说明 |
|---|---:|---:|---|
| y 0–300（状态栏 / 标题 / DIRECTION 上半） | 6.21% | 7.86% | 结构线偏差 0px，差异几乎全部来自字形 |
| y 300–700（DIRECTION 下半 / NAME / 目标日期） | 13.12% | 14.69% | 结构线偏差 1–3px |
| y 700–1000（LORE） | 28.69% | 30.26% | 正文断行点与设计稿不同（见 §4.2） |
| y 1000–1296（PARTY / RARITY / 提交） | 20.14% | 17.02% | emoji 字形差异为主 |

### 2.2 颜色 token 误差（未来模式）

| token | 像素占比 | 平均颜色误差 | 结论 |
|---|---:|---:|---|
| `background` | 15.78% | 7.53 | 已对齐 |
| `surface`（面板底） | 47.35% | 12.01 | 已对齐 |
| `secondary`（未来蓝块 / 提示） | 3.49% | **6.03** | 已对齐 |
| `onSurface` / `outline`（墨线 / 正文文字） | 15.96% | **39.84** | 字体栅格化主导 |
| `labelTertiary`（fieldlabel 小字） | 0.86% | 56.31 | 字体栅格化主导 |

**色块类 token 的误差都在 6–12 之间（已对齐），墨色文字类 40–56（未对齐）** —— 这是判定「几何已对齐、剩余为字体差异」的核心证据。

## 3. 本轮修正（从 54.25% → 16.53%）

初版实测 Pixel Diff 54.25% / SSIM 0.463，经六轮修正降至 16.53% / 0.744：

1. **设计侧截图坐标系**：覆盖掉 `body` 的 `padding:44px 24px 80px`。此前设计图整体带 13 CSS px 内缩，与实现图不可比。
2. **进入路径**：原「点击底栏 ＋ 新时刻」改为 debug fixture 直接指定方向，两屏不再依赖坐标点击。
3. **页面骨架**：表单页此前错误地保留了全局底部三入口导航、且缺顶部 mono HUD。改为 overlay 非空时隐藏底栏（`MomentMarkApp`），并新增共享组件 `PixelStatusHud`（`.statusbar{padding:11px 18px 5px}`）。
4. **固定/滚动分层**：`.statusbar` 与 `.pagetitle` 固定，仅 `.form-scroll` 滚动（原先整页滚动，导致标题会被滚走）。
5. **返回方块**：`.pagetitle .back{25×25、margin-right:12px、border 3px、box-shadow 2px}` 此前完全缺失，已补并接上 `nav::back`。
6. **边框补偿规则（本轮最大的系统性修正）**：Compose 的 `Modifier.border` 内嵌不占布局尺寸，HTML 的 `border` 会让盒子变大。此前所有表单面板的 padding 直接照抄 HTML 值，导致每个面板内容外扩一个 border 宽度、并逐块累积。修正为 `padding = HTML padding + HTML border-width` 后，累积偏移从 11–13px 降到 0–3px。
7. **结构性补齐**：删除自造的「已选择 2027-03-28」行、删掉重复渲染的 `[x] 全天` 行（设计稿中它与 label 同行右对齐）、`▣ 分组线路` 去掉设计中不存在的「无阵营」chip、补上过去模式专有的「🔔 周年提醒」行。
8. **字号系统修正**：NAME `16sp → 14sp/20sp`、notebox 正文 `16sp → 12sp/19.8sp`、日期格 → `15sp/18sp`、party chip → `10sp/16sp`。此前整体偏大，是 LORE 从 2 行变 3 行的原因。
9. **RARITY**：改用设计稿的递进行星（13/17/21sp、gap 2dp、基线对齐），行高锁 21dp。
10. **方向按钮**：锁定高度 66dp（截图边框标定），修正了 DIRECTION 面板高 5.5dp 的系统性偏差。

## 4. 未达标原因与证据（合理例外）

### 4.1 字体栅格化差异（主因）

- 设计稿字体栈：`-apple-system, BlinkMacSystemFont, "PingFang SC"`（macOS Chrome 渲染）。
- 实现字体：中文走 Android 兜底黑体（`MmBodyFont = SansSerif`），mono 标签走 `MmHudFont = Monospace`。
- 两端不是同一字体文件，笔画粗细、字面宽、抗锯齿均不同。证据：
  - `evidence-font-design.png` / `evidence-font-impl.png`：同一块 DIRECTION 面板放大 2× 对照，文字与 emoji 的字形差异肉眼可见，而边框、蓝块、间距已一致。
  - 墨色文字区 MAE 39.84，而所有色块 token MAE ≤12（§2.2）。
  - 墨色二值 IoU 仅 61.9%，但**在 ±2px 容差下设计稿墨色有 92.5% 落入实现墨色范围**（分带 84.4%–95.5%）——说明位置对齐、笔画轮廓不同。
- PingFang SC 是 Apple 系统字体，无法合法打包进 Android 应用；项目内可用的 `lantinghei_sc_heavy` 仅有 Heavy 单一字重（首页用于 44sp 大数字），用于 9–14sp 正文会过粗并改变内建行高，反而引入新的几何偏差。

### 4.2 正文断行点差异

设计稿 notebox 内 12px 正文每行恰好放 19 个汉字（断在「抢到」后）；Android 中文字宽略窄（≈11.7px），同宽度会多容纳 1 字，导致整段每行都错开一个字符。
尝试把 notebox 水平内距由 13dp 收窄到 14dp 强制断行一致 —— 实测 Pixel Diff 反而由 16.53% 升到 16.92%、SSIM 0.744 → 0.730（正文整体左移 1dp 的损失大于断行对齐的收益），**已回退**。

### 4.3 emoji 字形

设计稿的 🎓💞✈️💼 与 Android 的 emoji 字体（Noto Color Emoji）造型不同。本轮已把 party chip 内 emoji 字号由 13sp 修正为与文字一致的 10sp，位置对齐但字形本身不可移植。

### 4.4 设计稿静态数字

设计稿的 `2027年03月28日` / `2023年05月20日` 由 debug fixture 提供以便截图，生产构建走真实日期与 `EventTimeCalculator` 派生。日期一致性不作为硬性指标（沿用第①屏口径）。

## 5. 页面审计结论

| 项目 | 设计稿 ②③ | 当前实现 | 结论 |
|---|---|---|---|
| 页面骨架 | `.statusbar` + `.pagetitle`（固定）+ `.form-scroll`（滚动） | 同结构，底栏隐藏 | 已对齐 |
| 返回方块 | 25×25 / 右距 12 / 硬影 2 | 同规格，可点击返回 | 已对齐 |
| DIRECTION 面板 | `.px4`，padding 11/12，选中蓝底（未来）/ 金底（过去） | 同规格（padding 补 border） | 已对齐 |
| 提示行 | `▸ 记录一个期待的日子…` / `▸ 珍藏一段已经开始的时光…` | 按方向切换文案与颜色 | 已对齐 |
| 名称字段 | `▸ 时刻名称 NAME ★` + 白框 `14px/700` | 同规格 | 已对齐 |
| 日期字段 | 未来 `目标日期 TARGET DATE ★` + `[x] 全天`；过去 `开始日期 SINCE DATE ★` + `从这天起 · 每天 +1` | 按方向切换 label 与 helper | 已对齐 |
| 日期三格 | 年 flex1.2 / 月 flex1 / 日 flex1，`15px/800` | 同规格 | 已对齐 |
| 故事字段 | 未来 `任务背景 LORE` + `选填·将显示在详情页`；过去 `成就故事 STORY` + `选填·将显示在成就页` | 同规格 | 已对齐 |
| 分组 chip | `🎓学业 💞生活 ✈️旅行 💼工作 🏠家庭 🍎健康 🎁纪念 💡灵感`，无「无阵营」 | 同顺序、无「无阵营」 | 已对齐 |
| RARITY | `13/17/21px` 递进行星，未选中 `#D8CFB8` | 同规格 | 已对齐（行高 21dp 触摸目标偏小，见 §6） |
| 周年提醒（过去） | `🔔 周年提醒` + `[x] 每年 MM.DD 提醒我` | 已复刻，**仅视觉，未接通知** | 视觉已对齐 / 逻辑未接 |
| 提交按钮 | 未来 `⧖ 铭刻 · 开始倒数！`（蓝）/ 过去 `🏆 铭刻 · 开始珍藏！`（金） | 同规格 | 已对齐 |

## 6. 已知取舍与风险

1. **RARITY 行触摸高度 21dp**，低于 `TouchTargetMin`（44dp）。这是为对齐设计稿行高的显式取舍；星串本身可点，横向每个选项仍为 44dp 宽。
2. **周年提醒行只做视觉**：设计稿该行属 P1（通知），本轮未接通知能力，文案随锚定日期派生，开关不可交互。
3. **`PixelTextInput` 兼容性**：新增可选参数 `helperLabel`；`PixelDateSelector` 移除了 `showAllDay` 参数（该参数会导致 `[x] 全天` 重复渲染）。
4. **`PixelStarRating` 新增 `progressive` 参数**（默认 false，不影响任务难度等既有调用）；`PixelPageHeader` 的返回按钮由 `PixelButton` 改为 25dp 自绘方块，其他页面若原本依赖 44dp 触摸目标需复查。

## 7. 验证范围

- JVM 单元测试：`testDebugUnitTest` 通过。
- 构建：`assembleDebug` 通过。
- 静态检查：`lintDebug` 通过（无 error）。
- 模拟器：Pixel AVD 上完成两屏截图与视觉指标复核。
- **未验证**：真机、TalkBack（中文无障碍语义）、130% / 200% 大字体、系统栏 inset 异常场景、`connectedDebugAndroidTest`（本轮未运行）。
- **未验证**：保存/编辑/返回的真实点击链路端到端回归（视觉验收通过后另行验证）。

## 8. 产物清单（`docs/design/visual/screen-02-add/`）

| 文件 | 说明 |
|---|---|
| `design-future.png` / `design-past.png` | 原设计截图（628×1296 @2x） |
| `current-implementation-future.png` / `-past.png` | 当前实现截图（628×1296 @2x） |
| `difference-overlay-future.png` / `-past.png` | 差异叠加图（洋红 = 差异像素） |
| `difference-heat-future.png` / `-past.png` | 差异热力（×3 增益） |
| `side-by-side-future.png` / `-past.png` | 并排对比 |
| `metrics-future.json` / `metrics-past.json` | 量化指标（Pixel Diff / SSIM / 颜色误差 / 文本行带） |
| `geometry-audit-future.json` / `-past.json` | 结构线配对偏差审计 |
| `design-geometry-future.json` / `-past.json` | 设计稿 DOM 实测几何与 computed style |
| `evidence-font-design.png` / `evidence-font-impl.png` | 字体字形差异证据（DIRECTION 面板 2× 放大对照） |
| `evidence-bottom.png` | 底部 chip / RARITY 区域 2× 对照 |
| `_capture_design.py` | 设计侧截图 + DOM 几何导出（含 `__measure` 注入） |
| `_capture_android.py` | 实现侧截图（setprop fixture 直入目标方向） |
| `_compare.py` | 量化对比与差异图生成 |
| `_geometry_audit.py` | 结构线偏差审计 |

---

# 第二轮修正（2026-09-14）：四项细节反馈

用户在第一轮验收后提出四个具体问题，本轮逐项修复并复核指标。

## R2-1 返回方块：外框塌陷 + 字形过窄

**现象**：返回方块的中间部分明显比设计稿小；右下阴影与方框错位。

**定因**（`_measure_back3.py` 逐行墨迹扫描，`x∈[50,112]`、`y∈[75,150]`）：

| 项 | 设计稿 | 修正前实现 | 差异 |
|---|---|---|---|
| 方框墨迹 y 范围 | 83..136（54px = 27dp） | 83..124（42px = 21dp） | **矮 4dp** |
| 底边框 y | 127..132 | 119..124 | 上移 8px |
| 孤立阴影条 y | — | 125..132（8px） | **设计稿没有** |
| `‹` 字形 | x=74..84（11px）、y=104..117（14px）、面积 78px | x=76..81（6px）、y=100..113（14px） | **窄一半、位置偏上** |

两条独立成因：

1. **描边盒没撑满固定尺寸**。`PixelPanel` 的内层描边 `Box` 只有 `fillMaxWidth()`，高度由内容决定；返回方块外层是 `Modifier.size(25.dp)`，而内部只有 7dp 高（修正前是 18sp 的 `Text("‹")`）。于是 HTML 的 `border` 包住整块 25dp、Compose 只包住 ~21dp，而硬影仍按外层 25dp 计算 → 方框底部上方多出一段 8px 的孤立阴影条。**修法**：给 `PixelPanel` 增加 `fillContent: Boolean = false`，返回方块传 `true`。
2. **字形宽度不可移植**。设计稿是 PingFang SC 的 U+2039，墨迹 5.5dp 宽；Android 兜底字体（Noto Sans CJK）同名字形只有 3dp 宽，且**放大字号会同时把高度拉过 7dp**，无法只补宽度。**修法**：改为 `PixelBackChevron` 用 `Canvas` 填充多边形自绘，顶点直接取自设计稿逐行墨迹的几何边界（外接框 11×14 @2x px），并补回设计稿行盒带来的 1.5dp 垂直下偏。

**修正后复核**：

```
DESIGN: 符号 bbox x=[74,84] 宽=11px y=[104,117] 高=14px | 面积=78px
IMPL  : 符号 bbox x=[74,83] 宽=10px y=[104,117] 高=14px | 面积=78px
  墨迹行范围: 两端均为 y=83..136，满宽行(边框)均为 83..136
```

面积完全一致（78 vs 78 px），高度/左边界/边框/阴影全对齐；仅最右 1 个 @2x 像素（0.5dp）因抗锯齿覆盖度略低于阈值而未计入墨迹。证据：`evidence-r2-back.png`。

## R2-2 复刻「待输入」的粗体闪烁光标

**设计稿原文**（`design.html` L286/L350/L405）：

```html
<div class="mono" style="...">毕业旅行 · 东京<span style="animation:blink 1s steps(1) infinite">▮</span></div>
<style>@keyframes blink{50%{opacity:0}}</style>
```

即 U+25AE `▮` + **1s 周期、`steps(1)` 硬切换**（亮 0.5s / 灭 0.5s，无渐变）。

**尺寸来源**：设计稿截图时光标恰处于"灭"相位（`50%{opacity:0}`），无法从像素考古取值，改用 Chrome `CanvasRenderingContext2D.measureText` 的 `actualBoundingBox*` 实测该字形的**墨迹**（非 advance）：`3.99px × 8.34px @14px mono` → **4.0dp × 8.3dp**（见脚本 `_probe_cursor.py`）。

**修正前**：`CursorWidth = 2.dp`、高度取 `getCursorRect().height`（≈ 整行 20dp）、闪烁周期 `MotionStepMs × 2 × 2 = 800ms`。

**修正后**（`PixelTextInput`）：

- 宽 `FormInputCursorWidth = 4.dp`、高 `FormInputCursorHeight = 8.3.dp`；
- 位置：行盒内**垂直居中**（`cursor.top + padding + (行高 − 光标高) / 2`），因为 8.3dp 远矮于 20dp 行高；
- 闪烁：`CursorBlinkPeriodMs = 1000`，`delay(500)` 亮 / `delay(500)` 灭，硬切换；
- 触发：仅字段**聚焦**时显示（`focused && cursorVisible`），与真实输入框语义一致。

**复核**（`current-implementation-future.png` 与 `-cursor.png` 差分，`_measure_cursor_diff.py`）：

```
NAME 框内差异: x=[299,306] 宽=8px(@2x) = 4.00dp | y=[573,590] 高=18px
```

宽 **4.00dp**（目标 4.0dp，完全一致）；高 18px 含 1 个半覆盖的抗锯齿像素，实际绘制 16.6px（目标 16.7px）。闪烁已确认生效：连拍三帧的深色像素计数为 `[6194, 6066, 6194]`，差值 128px 正是该光标面积。证据：`evidence-r2-cursor.png`（上=设计稿、下=实现）。

## R2-3 删除表单的预填示例文字

`design.html` 第 ②③ 屏 NAME 里的「毕业旅行 · 东京」「和小满在一起」与 LORE/STORY 段落只是 HTML 稿的**展示效果**，不是要给真实表单预填的数据。原 debug fixture 把这四段文本写进了 `title` / `note`，在截图中看起来像"已有内容"。

**修法**：fixture 拆成两档，避免"产品正确"与"视觉可比"互相牺牲。

| `setprop debug.momentmark.form` | 行为 | 用途 |
|---|---|---|
| `future` / `past`（默认） | 只预置方向 + 锚定日期 + 分组 + 重要度，**标题与备注为空** | 与生产行为一致 |
| `future.sample` / `past.sample` | 额外填入 design.html 的示例标题与备注 | 仅用于与设计稿逐像素比对 |

同时把 LORE/STORY 的 **placeholder** 从「照抄设计稿那段正文」改为引导语（`写下令你期待这段日子的理由…` / `写下这段成就背后的故事…`）——placeholder 是空框提示，复现设计稿正文会让人误以为内容没删。NAME 的 placeholder `例如：毕业旅行 · 东京` 保留（带「例如：」前缀，语义明确是格式示例）。证据：`evidence-r2-empty-form.png`（左=未来、中=过去空表单，右=底部）。

## R2-4 提交按钮下方留白

HTML 的 `.form-scroll` 内容直接贴屏幕底边（提交按钮 `margin-bottom: 0`）。App 内新增 `FormScrollPaddingBottom = 28.dp`，作用于滚动内容的底部内距，使按钮下沿距内容区底 28dp，不再贴紧屏幕边缘/手势条。证据：`evidence-r2-bottom.png`（未来 / 过去两种模式滚动到底）。

## R2 量化指标

| 指标 | 未来模式 | 过去模式 |
|---|---:|---:|
| Pixel Difference Rate | 16.53% → **16.45%** | 17.11% → **17.02%** |
| SSIM（灰度） | 0.744 → **0.745** | 0.714 → **0.715** |
| MAE（RGB） | 18.53 → **18.38** | 19.80 → **19.66** |
| 结构线最大偏差 | 5px（2.5dp） | 5px（2.5dp） |
| 结构线 ≤4px 占比 | **94.4%** | 94.4% |

四项修正均未引入退化，指标整体略优。门槛（<1% / >0.95）仍为**已记录的合理例外**，原因与第一轮一致（设计稿 PingFang SC vs Android 兜底黑体：色块类 token MAE ≤12，墨色文字类 39.8–56.3；正文断行点差异；emoji 字形不可移植）。

**过程中的一次回归**：编辑表单滚动区时误把面板间距从 `10.dp` 写成 `SpaceUnit * 2 = 8.dp`，导致每档少 2dp、累积下移 23px，Pixel Diff 恶化到 30.04%。逐行差异 + 结构线配对定位后改回 `SpaceCard(10.dp)` 即恢复。**教训：token 替换必须逐项核对原值，不要顺手改语义无关的数值。**

## R2 产物

| 文件 | 说明 |
|---|---|
| `evidence-r2-back.png` | 返回方块 4× 放大对照（左=设计稿、右=实现） |
| `evidence-r2-cursor.png` | NAME 框光标 1.4× 放大对照 |
| `evidence-r2-empty-form.png` | 空表单（未来/过去）+ 底部区域 |
| `evidence-r2-bottom.png` | 滚动到底的提交按钮与 28dp 留白 |
| `current-implementation-*-empty.png` | 空表单（产品行为）截图 |
| `current-implementation-future-cursor.png` | 聚焦态 + 闪烁光标截图 |
| `current-implementation-*-bottom.png` | 滚动到底截图 |
| `_probe_cursor.py` | Chrome `measureText` 实测 `▮` 字形墨迹尺寸 |

## R2 验证状态

- **已通过**：`./gradlew testDebugUnitTest assembleDebug lintDebug`（BUILD SUCCESSFUL，lint 无 error）。
- **已通过**：模拟器（Pixel AVD，628×1296 @2x）两方向 + 空表单 + 光标 + 滚动到底共 7 张截图；几何审计 94.4% 结构线偏差 ≤4px。
- **未验证**：真机、TalkBack 中文语义、130%/200% 大字体、`connectedDebugAndroidTest`、保存/编辑/返回端到端点击链路；光标在真机 IME 下的表现（本轮仅在模拟器 + 物理键盘模式下验证）。
