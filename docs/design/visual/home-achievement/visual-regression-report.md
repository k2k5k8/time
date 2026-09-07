# HomeAchievementCard 视觉回归报告

## 渲染条件

- 浏览器：Google Chrome headless（本机 Chrome），`--force-device-scale-factor=1`
- viewport：`330 × 170` CSS px
- 原稿：`original-card.html`（从 `proposal_J_final.html` 第①屏 `.ach` 原样抽取）
- 组件镜像：`component-card.html`，对应 Compose `HomeAchievementCard` 默认 Props

## 产物

- [原始设计截图](original.png)
- [组件渲染截图](component.png)
- [差异叠加图](difference-overlay.png)

## 量化结果

| 指标 | 结果 | 目标 | 结论 |
|---|---:|---:|---|
| Pixel Difference Rate | **0.0000%** | < 1% | 通过 |
| Pixel Similarity | **100.0000%** | > 99% | 通过 |
| SSIM | **1.0000**（两张 RGB 位图逐像素相同） | > 0.95 | 通过 |
| 平均颜色偏差（RGB MAE） | **0.0000** | 越低越好 | 通过 |
| 布局/尺寸/内外间距 | **0 px** | 0 px | 通过 |
| 圆角/阴影 | **0 px 差异** | 0 px | 通过 |

## 字体检查

组件镜像与原稿均使用 `-apple-system, BlinkMacSystemFont, "PingFang SC", sans-serif`；HUD/元数据使用 `ui-monospace, SFMono-Regular, Menlo, monospace`。字号、字重、行高逐项对应逆向记录中的 14/44/12/9/8px 与 1.05 行高。

## 修改记录

1. 新增 `HomeAchievementCard` 参数化 Compose 组件，保留原元素顺序和 CSS 数值。
2. `MomentPinnedCard` 改为领域投影适配器，不再自行改变布局。
3. 新增 `MomentMarkTokens` 中的首页成就卡尺寸/字号 token，并同步 `DESIGN_SYSTEM.md`。
4. 新增浏览器基准、组件镜像、截图和差异报告。

Android 真机/模拟器截图尚未执行：本机只有 Java 11，Android Gradle Plugin 8.13.1 要求 Java 17，因此 Gradle 编译在插件初始化阶段被环境阻断。浏览器视觉回归已完成。
