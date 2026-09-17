# 第 ⑧ 屏「分类抽屉 · 搜索」视觉复刻报告（MAP FILTER · 昼）

- 日期：2026-09-15
- 画布：628×1296（314×648 dp @2x），与 design.html `.phone` 内容区一致
- fixture：`setprop debug.momentmark.drawer filter`（DEBUG 专用，生产不触发）
- 产物：`design.png` / `current-implementation.png` / `diff.png` / `metrics.json` / `geometry.json`

## 1. 屏幕结构与数据 fixture

设计稿第 ⑧ 屏 = 首页展开「分类抽屉」状态：

| 区域 | 设计稿内容 | 实现来源 |
|---|---|---|
| 状态栏 | `LV.07` / `MAP FILTER` | 抽屉展开时 HUD 右槽切为 MAP FILTER（关闭时恢复 `GOLD ×1,280`） |
| 搜索栏 | 白底 `.px`：⌕（14px 蓝）+「搜索任务与成就…」(13px/700 ink-3) + ESC 徽章（mono 8px、2px 墨线、1px 6px 内距） | `MapFilterSearchBar`；搜索行为属 P1 未设计，输入区仅占位展示，ESC 承担关闭 |
| 背景列表 | `opacity:.35`、padding 14/12、gap 9；两行 `.px` 卡（考研初试 `96 DAYS · DUE 12.19`、演唱会 `32 DAYS · DUE 09.20`） | fixture 注入同数据；真实模式投影首页 Moment（置顶→过去→未来，`N DAYS · DUE/SINCE MM.dd`） |
| scrim | `rgba(43,38,32,.45)` 覆盖内容区（不盖 HUD/搜索栏/导航） | `MapFilterScrim`（outline 45%），点击关闭 |
| 抽屉 | `.px4` left10/top6/bottom6/宽222；`▸ 地图筛选 FILTER`（mono 11/800/.15em）→ 状态 STATUS（全部 ×6 选中=墨底金字、倒数中、正数中；chip mono 9/800、2.5px 边、4px 8px 内距）→ 分组 PARTY LINES（🎓 学业 ×3、💞 生活 ×2（danger 选中）、✈️ 旅行 ×1、💼 工作 ×0；mono 10/800、2.5px 边）→ `⚙ 管理分组 →`（ghost，mono 10/800、7px 0 内距、margin-top auto） | `MapFilterDrawerPanel`；fixture 还原稿面（生活 danger 选中态、学业墨色首行）；真实模式分组行全部未选中样式 |
| 底部导航 | `◉ 大事件`（选中）+`＋ 新时刻`+`▤ 日子簿` | 既有 `PixelBottomNav`，未改动 |

真实数据接入（已设计部分）：抽屉分组行 = Room `groups`（按 sortOrder）+ 首页 ViewModel 按分组名聚合的活动 Moment 计数（`Moment.groupId` 当前存组名）；不在 Room 但被 Moment 引用的组名追加在后。「全部 ×N」= 活动 Moment 总数。「⚙ 管理分组 →」→ 系统设置·队伍编成。搜索/筛选为 P1（PRD §7），chips 与分组行纯展示不可点（DESIGN_SYSTEM §6.6）。

## 2. 方法

- 设计稿：`_capture_design.py` 抽取 design.html 第 ⑧ 屏片段生成 standalone，Chrome headless `--force-device-scale-factor=2 --window-size=314,648`。
- 实现：`_capture_android.py` 把模拟器（emulator-5554）调至内容区 628×1296（wm size 628×1448 / density 320，避开系统栏），安装 APK、清数据、`setprop debug.momentmark.drawer filter` 直启冻结帧，screencap 后按状态栏 inset 裁剪。
- 对比：`_compare.py`（阈值 8/255）输出 PDR/SSIM/MAE/diff.png；`_geometry.py` 输出关键元素几何。

## 3. 视觉指标

| 指标 | 结果 | 门槛 |
|---|---:|---|
| Pixel Difference Rate | **8.10%** | <1%（未达 → 合理例外，见 §5） |
| SSIM | **0.839** | >0.95（未达 → 合理例外，见 §5） |
| MAE RGB | R=10.8 / G=12.3 / B=11.5 | — |

本屏 PDR/SSIM 均优于此前已验收各屏（screen-04-day 10.77%/0.776、screen-05 13.43%/0.760、screen-06 22.58%/0.623）。

### 3.1 分区归因（差异全部集中在文字）

| 分区 | PDR | MAE | 说明 |
|---|---:|---:|---|
| HUD 状态栏 | 5.01% | 3.4 | 仅 mono 文字 |
| 搜索栏 | 8.71% | 8.4 | ⌕ + placeholder + ESC 文字 |
| 抽屉面板 | 12.22% | 19.1 | 文字最密（标题/chips/分组行/emoji/按钮） |
| scrim/背景区 | **0.96%** | 0.6 | 压暗卡边缘抗锯齿 |
| 底部导航 | 4.11% | 3.7 | mono 文字 |

### 3.2 关键几何（`geometry.json`，@2x px）

| 元素 | design | impl | 偏差 |
|---|---:|---:|---:|
| 搜索白框 top | 63 | 63 | 0 |
| 搜索白框高度 | 73 | 74 | +1 |
| 抽屉 left | 20 | 20 | 0 |
| 抽屉 width | 452 | 452 | 0 |
| 抽屉 top | 176 | 177 | +1 |
| 底部导航 top | 1105 | 1106 | +1 |
| 抽屉底/管理按钮边框行 | 1062–1067 | 1061–1066 | −1 |

抽屉底部区域逐行 ASCII 墨迹比对：管理按钮顶边、文字墨迹行（y≈1088 起）两图同行同位。

### 3.3 颜色

| 色块 | design | impl | 偏差 |
|---|---|---|---|
| scrim 叠加色 | [146,140,126] | [146,140,126] | 0 |
| 抽屉边框 | [43,38,32] | [43,38,32] | 0 |
| 搜索白框底 | [255,255,255] | [255,255,255] | 0 |
| 搜索面板上/下空白带 MAE | — | — | 0.0 |

### 3.4 字体规格（实现 = 设计规格）

| 文本 | 规格（design.html） | 实现 token |
|---|---|---|
| 搜索 placeholder | 13px/700 sans，ink-3 | 13sp/700 SansSerif labelTertiary，lh 18sp |
| ⌕ | 14px xp 蓝 | 14sp secondary |
| ESC | mono 8px/800，2px 边 | Monospace 8sp/800 lh 10sp，border 2dp |
| 抽屉标题 | mono 11px/800/.15em | Monospace 11sp/800 ls 1.65sp lh 13sp |
| 区段 label | mono 8px/800/.2em ink-3 | Monospace 8sp/800 ls 1.6sp lh 10sp |
| 状态 chip | mono 9px/800，边 2.5px，内距 4px 8px | Monospace 9sp/800 lh 11sp，2.5dp 边，6.5/10.5dp |
| 分组行 | mono 10px/800，边 2.5px，内距 5px 8px | Monospace 10sp/800 lh 12sp，2.5dp 边，7.5/10.5dp |
| 管理按钮 | mono 10px/800 ghost，内距 7px 0 | Monospace 10sp/800 lh 12sp，10dp（7+3 边框补偿） |
| 背景卡标题/meta | 13px/800；mono 9px ink-3 | 13sp/800 lh 18sp；Monospace 9sp/800 lh 11sp |

## 4. 迭代记录（顺序：尺寸→间距→字体→颜色→阴影→细节）

1. 初版即达到几何全对齐（§3.2 全部 ±1px、颜色 0 误差、空白区 MAE 0.0），无需尺寸/间距/颜色/阴影迭代。
2. 字体：规格（family/size/weight/lineHeight/letterSpacing）逐项与 HTML 对应（§3.4）；渲染差异来自平台栅格化（§5），无布局空间可优化（screen-04/05/06 的行高实验已证明同根因不可通过布局参数消除）。

## 5. 合理例外论证（未达 <1% / SSIM>0.95 的证据）

1. **字体栈平台差异**：设计稿 macOS `-apple-system/PingFang SC + ui-monospace(SF Mono/Menlo)`；Android 模拟器 Noto Sans CJK + 默认 Monospace。同字号/字重的 advance width、抗锯齿栅格化不同（§3.1 分区表：差异集中区全部是文字区，非文字区 PDR 0.96%/MAE 0.6）。
2. **mono 字重回退**：Android Monospace 无 ExtraBold(800)，回退 Bold(700)，笔画比设计稿细（抽屉底 ASCII 对比可见墨迹同行但略细）。
3. **emoji 渲染差异**：🎓💞✈️💼/⚙/⌕ 由平台 emoji 字体渲染（Noto Color Emoji vs macOS），颜色与形状不同。
4. 已验收各屏（10.77%–22.58%）同根因按例外登记；本屏 8.10% 为全仓库最优，且结构/尺寸/间距/颜色/硬影逐项对齐。

## 6. 后续验证范围

- 视觉：本报告（fixture 冻结帧）。
- 逻辑：`MomentHomeViewModelTest`（计数聚合/抽屉开关/分组行装配）、`MomentHomeScreenTest`（chips/分组行/关闭/管理分组回调/左缘右滑/fixture 帧，10/10 通过）。
- 大字体：模拟器 `font_scale 1.3 / 2.0` 截图（`font-scale-130.png` / `font-scale-200.png`）像素核验——搜索框、scrim、抽屉面板、底部导航结构完整，无裁剪、无崩溃。
- 未验证：真机、TalkBack 中文语义朗读、搜索与筛选真实逻辑（P1 未设计）。
