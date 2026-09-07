# 首页双列小卡逆向还原记录

目标是 `proposal_J_final.html` 第①屏中两张整行卡片之后的两列 `.px` 小卡（“考研初试”和“演唱会”）。两张卡的 DOM/CSS 骨架完全相同，只有数据和徽章/进度颜色不同；Compose 以 `HomeSmallCard` 作为唯一复用组件，`MomentQuestCard` 与 `HomeTaskCard` 仅做领域投影适配。

## 1. 结构

```text
px（普通像素卡）
└── Column（padding: 10px 11px 9px）
    ├── Header（flex / space-between / center）
    │   ├── Title（12px / 800）
    │   └── badge-tag（符号，mono 8px，2px 描边，2px 6px 内距）
    ├── Value（margin-top: 2px）
    │   ├── 数字（26px / 800）
    │   └── 单位（10px）
    ├── hpbar（margin-top: 5px；动态时可省略）
    └── Metadata（mono 8px / 800；margin-top: 4px）
```

外层 `.px`：`background: #F6F1E3`、`border: 3px solid #2B2620`、`box-shadow: 3px 3px 0 #2B2620`。父级网格是 `display:grid; grid-template-columns:1fr 1fr; gap:10px; margin-top:10px`。

## 2. CSS 与颜色

| 元素 | 原始属性 |
|---|---|
| 标题 | `font-size:12px; font-weight:800` |
| 数字 | `font-size:26px; font-weight:800; margin-top:2px` |
| 单位 | `font-size:10px` |
| 进度条 | `height:10px; border:2px solid #2B2620; background:#DFD5BB; margin-top:5px` |
| 进度填充 | `left:0; top:0; bottom:0; width:0..100%`；Compose 按剩余比例实时计算颜色（>60% 绿、30–60% 橙、<30% 红） |
| 元数据 | `font-family:ui-monospace; font-size:8px; font-weight:800; color:#A79B85; margin-top:4px` |
| 左卡徽章 | `◇`，`#3E9BFF`（支线） |
| 右卡徽章 | `⧗`，`#F5A623`（限时） |

## 3. Props

`HomeSmallCard`：`title`、`value`、`unitLabel`、`badgeLabel`、`badgeColor`、`metadata`、`progressFraction?`、`progressColor?`、`onClick`、`modifier`、`semanticDescription`。默认值不改变骨架；`progressFraction` 为 null 时不渲染不存在的进度条。

`MomentQuestCard` 将 `MomentCardState` 的 `days`、`anchorDate`、`groupId`、`hpFraction` 投影进该组件；`HomeTaskCard` 将 `TaskCardState` 投影进同一组件，但不把 Task 伪装成 Moment。

## 4. 对比结论

- [x] DOM 顺序、3px 墨线、3px 硬影、内边距和间距保持原值。
- [x] 标题/数字/单位/mono 元数据字号逐项保留。
- [x] 进度长度由真实 `hpFraction` 驱动，颜色按时间远近动态切换。
- [x] 两个首页小卡入口复用同一 Compose 组件；大卡继续使用各自的 `.px4`/`.ach` 组件。
- [x] 浏览器固定 viewport 截图、差异叠加、Pixel Difference、SSIM、颜色误差与 bbox 报告见 `docs/design/visual/home-small-card/`。
