# HomeSmallCard 视觉回归报告

## 基准与产物

- 原稿：`original-card.html`，逐项抽取自 `proposal_J_final.html` 第①屏两列 `.px` 卡。
- 组件镜像：`component-card.html`，对应 `HomeSmallCard` 的默认 DOM 顺序与默认样例数据。
- 固定条件：viewport `306 × 140 CSS px`、device scale factor 1、Chromium headless。
- 预期产物：`original.png`、`component.png`、`difference-overlay.png`、`difference-heat.png`、`metrics.json`。

## 本次运行边界

Kotlin/JVM 编译验证已执行；浏览器截图未能在当前受限 macOS 沙箱完成。系统 Chrome 与 Playwright Chromium 在启动阶段因 Mach port 权限直接退出，Codex 内置浏览器又禁止 `file://` 和本机临时服务 URL。因此本目录保留了可复现的 HTML 和 `_diff.py`，但没有伪造 PNG 或量化数字；Pixel Difference、SSIM、RGB 颜色误差和 bbox 均为 **未测量**，不是通过。

在具备可启动的 Chromium 后运行：

```bash
node <playwright-script>                 # 生成 original.png / component.png
python3 _diff.py                         # 生成差异图与 metrics.json
```

指标目标仍为 Pixel Difference <1%、Pixel Similarity >99%、SSIM >0.95；真实 Android 设备、TalkBack、大字体和系统 inset 也尚未在本次执行中验证。
