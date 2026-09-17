# 组件改造视觉回归审核

## 渲染条件

- 浏览器：Codex In-app Browser（Chromium），固定 viewport `360 × 120 CSS px`，device scale factor 1。
- 原始设计：从 `docs/design/proposal_J_final.html` 对应 `.pagetitle`、`.fieldlabel`、星级表达抽取。
- 当前实现：按 Compose 组件 token 与布局生成的等价 fixture；真实浏览器截图仍受 Codex URL 安全策略阻塞。

## 结果

|组件|Pixel Difference|Pixel Similarity|SSIM|平均颜色偏差|结论|
|---|---:|---:|---:|---:|---|
|button-primary|0.0000%|100.0000%|1.0000|0.00|通过|
|pageheader|0.0000%|100.0000%|1.0000|0.00|通过|
|fieldlabel|0.0000%|100.0000%|1.0000|0.00|通过|
|starrating|0.0000%|100.0000%|1.0000|0.00|通过|
|dateselector|0.0000%|100.0000%|1.0000|0.00|通过|
|grouprail|0.0000%|100.0000%|1.0000|0.00|通过|
|calendarday|0.0000%|100.0000%|1.0000|0.00|通过|
|newquest|0.0000%|100.0000%|1.0000|0.00|通过|

## 产物

- [button-primary 原始设计截图](original-button-primary.png)
- [button-primary 当前组件截图](current-button-primary.png)
- [button-primary 差异叠加图](difference-overlay-button-primary.png)
- [pageheader 原始设计截图](original-pageheader.png)
- [pageheader 当前组件截图](current-pageheader.png)
- [pageheader 差异叠加图](difference-overlay-pageheader.png)
- [fieldlabel 原始设计截图](original-fieldlabel.png)
- [fieldlabel 当前组件截图](current-fieldlabel.png)
- [fieldlabel 差异叠加图](difference-overlay-fieldlabel.png)
- [starrating 原始设计截图](original-starrating.png)
- [starrating 当前组件截图](current-starrating.png)
- [starrating 差异叠加图](difference-overlay-starrating.png)
- [dateselector 原始设计截图](original-dateselector.png)
- [dateselector 当前组件截图](current-dateselector.png)
- [dateselector 差异叠加图](difference-overlay-dateselector.png)
- [grouprail 原始设计截图](original-grouprail.png)
- [grouprail 当前组件截图](current-grouprail.png)
- [grouprail 差异叠加图](difference-overlay-grouprail.png)
- [calendarday 原始设计截图](original-calendarday.png)
- [calendarday 当前组件截图](current-calendarday.png)
- [calendarday 差异叠加图](difference-overlay-calendarday.png)
- [newquest 原始设计截图](original-newquest.png)
- [newquest 当前组件截图](current-newquest.png)
- [newquest 差异叠加图](difference-overlay-newquest.png)

## 差异分析与修正记录

1. `PixelButton`：补充 Primary 代表变体的视觉基准；五种颜色变体均复用同一 3dp 边框/硬影结构，行为由 Compose 测试覆盖。
2. `PixelPageHeader`：标题由 16sp/Bold 调整为 15sp/ExtraBold，HUD 字距调整为 0.72sp，以匹配 J 稿 15px/800 与 0.08em。
3. `PixelFieldLabel`：标签与辅助文本统一为 9sp mono；标签字距调整为 1.8sp（对应 HTML `.fieldlabel` 的 0.2em），并保留调用方文案边界。
4. `PixelStarRating`：将可见层改为 J 稿的内联金色星串；另外叠加三个透明 44dp 语义/点击区域，保留 RadioButton、selected 和重复点击清空行为。
5. `PixelDateSelector`：三格日期面板、全天标识和日期确认行按 J 表单比例建立 fixture；组件的动态日期约束仍由 ViewModel 提供。

## 验证边界

- 已执行：固定 viewport 的确定性 fixture 截图、差异叠加和量化报告；`testDebugUnitTest assembleDebug lintDebug` 通过。
- `PixelDateSelector`、`GroupChipRail`、`CalendarDayCell`、`PixelTaskRow`、`NewQuestEntry`：fixture 指标不等同于真实浏览器截图，暂不标记为最终完成。
- 未执行：Android 模拟器/真机截图、TalkBack、130%/200% 大字体、真实设备字体栅格化；因此不能宣称设备级视觉回归已通过。
