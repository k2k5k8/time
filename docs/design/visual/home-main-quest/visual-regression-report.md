# HomeMainQuestCard 视觉回归报告

## 渲染条件

- 浏览器：Google Chrome headless（本机 Chrome），`--force-device-scale-factor=1`
- viewport：`320 × 145` CSS px
- 原稿：`original-card.html`（从 `proposal_J_final.html` 第①屏 `.px4` 主线横幅卡原样抽取，line 134-145）
- 组件镜像：`component-card.html`，对应 Compose `HomeMainQuestCard` 默认 Props

## 产物

- [原始设计截图](original.png)
- [组件渲染截图](component.png)
- [差异叠加图](difference-overlay.png)
- [差异热力图](difference-heat.png)
- [量化指标 JSON](metrics.json)

## 量化结果

| 指标 | 结果 | 目标 | 结论 |
|---|---:|---:|---|
| Pixel Difference Rate | **0.0000%** | < 1% | 通过 |
| Pixel Similarity | **100.0000%** | > 99% | 通过 |
| SSIM | **1.0000**（两张 RGB 位图逐像素相同） | > 0.95 | 通过 |
| 平均颜色偏差（RGB MAE） | **0.0000** | 越低越好 | 通过 |
| 背景颜色偏差 | **0.0000** | 0 | 通过 |
| 内容区域 / bbox | 原始 `(12,12,305,126)`，组件 `(12,12,305,126)` | 一致 | 通过 |
| 布局/尺寸/内外间距/阴影偏移 | **0 px 差异** | 0 px | 通过 |

## 字体检查

组件镜像与原稿均使用 `-apple-system, BlinkMacSystemFont, "PingFang SC", sans-serif`；HUD/UNLOCK 使用 `ui-monospace, SFMono-Regular, Menlo, monospace`。字号、字重逐项对应：标题 14sp/800、数字 44sp/800/1.05、单位 12sp/800、HUD 与 UNLOCK 9sp/700。行高以 Chrome 实测值为准：14sp sans 标题 20sp，9sp mono HUD 与 UNLOCK 13sp。

## 修改记录

1. 新增 `HomeMainQuestCard` 参数化 Compose 组件，保留原元素顺序和 CSS 数值。
2. 在 `MomentMarkTokens` 中新增首页主线横幅卡专用 token（`HomeMainQuest*`）。
3. 同步 `DESIGN_SYSTEM.md` §3.3 token 表与 §4 组件规格，区分未来主线横幅卡与过去成就金框卡。
4. 新增浏览器基准、组件镜像、截图、差异图和量化指标。

## 验证范围说明

- 浏览器视觉回归已通过：原稿与组件镜像在 320×145 viewport 下逐像素一致。
- Android 真机/模拟器截图尚未执行：本机只有 Java 11，Android Gradle Plugin 8.13.1 要求 Java 17，因此 Gradle 编译在插件初始化阶段被环境阻断。Compose 组件已按 token 与浏览器实测行高忠实转写，真实设备像素表现需待具备 Java 17 / 真机环境后补测。
