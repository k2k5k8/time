# 首页成就卡逆向还原记录

目标：`proposal_J_final.html` 第 ① 屏中标题为“和小满在一起 💞”的置顶成就卡（`.ach`）。本记录先于组件实现，作为像素级验收基线。

## 1. 原始结构

```text
ach（外层卡片）
├── HUD 行（display:flex; justify-content:space-between; align-items:center; mono）
│   ├── 徽章：🏆 ACHIEVEMENT 成就 · 正数
│   └── 置顶：★ 置顶
├── 主信息行（display:flex; align-items:baseline; justify-content:space-between）
│   ├── 左列
│   │   ├── 标题：和小满在一起 💞
│   │   └── 数字：1196 + 单位“天”
│   └── 右列：SINCE + 2023.05.20
├── hpbar（血条）
│   └── i（99% 金色填充）
└── 里程碑行（display:flex; justify-content:space-between; mono）
    ├── 下一里程碑 1200 天 · 还有 4 天
    └── 99%
```

`.ach::before` 在外框内缩 3px 绘制 2px 金色内框；它不参与文档流。

## 2. 原始 CSS/尺寸关系

| 层级 | 原始属性 | 值 |
|---|---|---|
| `.ach` | background | `var(--panel)` |
| `.ach` | border | `3px solid var(--panel-line)` |
| `.ach` | box-shadow | `4px 4px 0 var(--panel-line)` |
| `.ach` | position | `relative` |
| `.ach::before` | inset / border | `inset:3px; border:2px solid var(--gold)` |
| 卡片内边距 | padding | `11px 13px 10px` |
| HUD 行 | font | `.mono`（等宽、`font-weight:700`），字号 `9px`，徽章额外 `font-weight:800` |
| 主信息行 | margin-top | `4px` |
| 标题 | font-size / weight | `14px / 800` |
| 数字 | font-size / weight / line-height | `44px / 800 / 1.05` |
| 单位 | font-size / color | `12px / var(--ink-2)` |
| SINCE | mono、字号、颜色 | `9px / var(--ink-3)`，右对齐 |
| HP 条 | height / border / fill | `10px / 2px / 99%`，填充 `var(--gold)` |
| HP 条 | margin-top | `8px` |
| 里程碑行 | margin-top / font | `4px / mono 8px 800` |
| 里程碑颜色 | color | `var(--ink-3)` |

页面根色变量来自 HTML：`--panel:#F6F1E3`、`--panel-line:#2B2620`、`--gold:#F5B301`、`--gold-ink:#B98A00`、`--ink-2:#6B6152`、`--ink-3:#A79B85`。

## 3. Compose Props

`HomeAchievementCard` 保留原 DOM 顺序并参数化：

- `title: String`：标题，默认 `和小满在一起 💞`。
- `days: Long`：已过去天数，默认 `1196`。
- `anchorDate: LocalDate`：SINCE 日期，默认 `2023-05-20`。
- `milestoneLabel: String?`、`milestoneDaysRemaining: Long?`、`milestoneProgress: Float?`：里程碑行与金色血条；均为空时不渲染附加行。
- `isPinned: Boolean`：控制“★ 置顶”文案。
- `onClick: () -> Unit`：卡片点击回调。

`MomentPinnedCard` 仅负责把领域投影 `MomentCardState` 映射到该组件，不改变事实字段或时间计算。

## 4. 逐项对比清单

- [x] 外框 3px 墨线、4px 硬影、3px 内缩金线。
- [x] `11/13/10px` 内边距、4/8/4px 垂直间距。
- [x] 标题 14px/800、数字 44px/800/1.05、单位 12px。
- [x] HUD 与元数据使用等宽字体及 9/8px 字号。
- [x] 主信息左右基线对齐，SINCE 右对齐。
- [x] HP 条 10px 高、2px 描边，填充比例来自参数。
- [x] 不新增图标、按钮、圆角、渐变或装饰元素。

浏览器视觉验证产物见 `docs/design/visual/home-achievement/`。
