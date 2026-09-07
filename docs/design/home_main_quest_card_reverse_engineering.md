# 首页主线任务横幅卡逆向还原记录

目标：`proposal_J_final.html` 第 ① 屏中标题为“春节 · 回家的日子”的主线任务横幅卡（`.px4`）。本记录先于组件实现，作为像素级验收基线。

## 1. 原始结构

```text
px4 外层卡片
├── HUD 行（display:flex; justify-content:space-between; mono 9px）
│   ├── 左侧：◆ MAIN QUEST 主线 · 倒数（mana 紫 #9B6DFF）
│   └── 右侧：EXP +148（ink-3 #A79B85）
└── 主信息行（display:flex; align-items:baseline; justify-content:space-between; margin-top:4px）
    ├── 左列
    │   ├── 标题：春节 · 回家的日子（14px / 800）
    │   └── 数字：148 + 单位“ 天后”（44px / 800 / line-height 1.05；单位 12px / ink-2）
    └── 右列：UNLOCK + 2027.02.06（mono 9px / ink-3）
```

外层 `.px4` 同时负责：3px 墨色实线边框 + 4px 右下硬偏移阴影；不引入额外 DOM 层级。

## 2. 原始 CSS/尺寸关系

| 层级 | 原始属性 | 值 |
|---|---|---|
| 外层 `.px4` | background | `var(--panel)` `#F6F1E3` |
| 外层 `.px4` | border | `3px solid var(--panel-line)` |
| 外层 `.px4` | box-shadow | `4px 4px 0 var(--panel-line)` |
| 外层 `.px4` | padding | `11px 13px`（top/bottom 11，left/right 13） |
| 外层 `.px4` | 实测宽度 | `290px`（手机 330px 减去 8px×2 边框与 12px×2 内边距） |
| HUD 行 | display / justify-content | `flex` / `space-between` |
| HUD 行 | font / weight | `.mono` 9px / 700 |
| 主线徽章 | color | `var(--mana)` `#9B6DFF` |
| EXP 角标 | color | `var(--ink-3)` `#A79B85` |
| 主信息行 | margin-top | `4px` |
| 主信息行 | align-items | `baseline` |
| 标题 | font-size / weight | `14px` / `800`，默认 sans |
| 大数字 | font-size / weight / line-height | `44px` / `800` / `1.05`（46.2px） |
| 单位 | font-size / color | `12px` / `var(--ink-2)` `#6B6152`，继承 weight 800 |
| UNLOCK 日期 | font / size / color | `.mono` 9px / 700 / `var(--ink-3)` `#A79B85` |

页面根色变量来自 HTML：`--panel:#F6F1E3`、`--panel-line:#2B2620`、`--ink:#2B2620`、`--ink-2:#6B6152`、`--ink-3:#A79B85`、`--mana:#9B6DFF`。

Chrome headless 实测行高：标题 14px 默认 sans 为 `20px`；9px mono HUD 与 UNLOCK 为 `13px`；44px 数字由显式 `line-height:1.05` 得 `46.2px`。

## 3. Compose Props

`HomeMainQuestCard` 保留原 DOM 顺序并参数化：

- `title: String`：标题，默认 `春节 · 回家的日子`。
- `days: Long`：倒数天数，默认 `148`。
- `unitLabel: String`：单位文案，默认 ` 天后`。
- `anchorDate: LocalDate`：UNLOCK 日期，默认 `2027-02-06`。
- `questLabel: String`：主线徽章文案，默认 `◆ MAIN QUEST 主线 · 倒数`。
- `expLabel: String?`：右上角 EXP 角标，默认 `EXP +148`；传 `null` 时不渲染。
- `onClick: () -> Unit`：卡片点击回调。
- `modifier`、`semanticDescription`：同既有首页卡片。

## 4. 逐项对比清单

- [x] 外框 3dp 墨线、4dp 硬影（PixelPanel `Emphasized`）。
- [x] `11/13/11dp` 内边距、HUD→主信息行 `4dp` 间距。
- [x] 标题 14sp/800、数字 44sp/800/1.05、单位 12sp/800（ink-2）。
- [x] HUD 与 UNLOCK 使用等宽字体 9sp/700；行高取 Chrome 实测 13sp。
- [x] 主线徽章 mana 紫 #9B6DFF；其余弱注释 ink-3 #A79B85。
- [x] 主信息行左右分布，UNLOCK 日期左对齐（同原 span）。
- [x] 不新增图标、按钮、圆角、渐变或装饰元素；仅忠实还原已有 DOM 层级。

视觉验证产物见 `docs/design/visual/home-main-quest/`。
